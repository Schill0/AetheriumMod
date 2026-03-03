package com.espmod.render;

import ai.onnxruntime.*;
import com.espmod.EspMod;
import com.espmod.config.Config;
import com.espmod.utils.RenderUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.*;

public class ChunkAnalysisAiManager {

    private static OrtEnvironment env;
    private static OrtSession session;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Mapping statico per performance O(1)
    private static final Map<Block, Float> blockMapping = new HashMap<>();

    // Results: ChunkPos -> Probability (0.0 to 1.0)
    private static final Map<ChunkPos, Float> chunkResults = new ConcurrentHashMap<>();
    private static final Set<ChunkPos> pendingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static ChunkPos lastChunkPos = null;
    private static boolean isModelLoaded = false;

    public static void register() {
        initializeBlockMapping();
        loadModel();
        ClientTickEvents.END_CLIENT_TICK.register(ChunkAnalysisAiManager::onTick);
        WorldRenderEvents.LAST.register(ChunkAnalysisAiManager::onRender);
    }

    private static void initializeBlockMapping() {
        // Questa mappa riflette la logica di training: 0=Aria/Dec, 1=NatSol, 2=Liquid,
        // 3=Artif
        Registries.BLOCK.forEach(block -> {
            String id = Registries.BLOCK.getId(block).toString();
            if (isArtificial(id))
                blockMapping.put(block, 3.0f);
            else if (id.equals("minecraft:water") || id.equals("minecraft:lava"))
                blockMapping.put(block, 2.0f);
            else if (isAirOrDecorative(id))
                blockMapping.put(block, 0.0f);
            else
                blockMapping.put(block, 1.0f); // Default
        });
        EspMod.LOGGER.info("[EspMod AI] Block mapping initialized for {} blocks.", blockMapping.size());
    }

    private static void loadModel() {
        executor.submit(() -> {
            try {
                EspMod.LOGGER.info("[EspMod AI] Starting model load from resources...");
                env = OrtEnvironment.getEnvironment();

                // Dobbiamo estrarre `radar_donut_v1.onnx` E `radar_donut_v1.onnx.data` in una
                // cartella temporanea
                // perchè `createSession(byte[])` non supporta il caricamento di pesi da file
                // esterni `.data`.
                java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("espmod_ai");
                tempDir.toFile().deleteOnExit();

                String[] filesToExtract = { "radar_donut_v1.onnx", "radar_donut_v1.onnx.data" };
                java.nio.file.Path mainModelPath = null;

                for (String fileName : filesToExtract) {
                    try (InputStream stream = ChunkAnalysisAiManager.class
                            .getResourceAsStream("/assets/espmod/ai/" + fileName)) {
                        if (stream != null) {
                            java.nio.file.Path outPath = tempDir.resolve(fileName);
                            java.nio.file.Files.copy(stream, outPath,
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            outPath.toFile().deleteOnExit();
                            if (fileName.equals("radar_donut_v1.onnx")) {
                                mainModelPath = outPath;
                            }
                        } else {
                            EspMod.LOGGER.error("[EspMod AI] Model file NOT FOUND in resources: " + fileName);
                        }
                    }
                }

                if (mainModelPath != null) {
                    session = env.createSession(mainModelPath.toString(), new OrtSession.SessionOptions());
                    isModelLoaded = true;
                    EspMod.LOGGER.info("[EspMod AI] Model loaded successfully! Inputs: {}", session.getInputNames());
                } else {
                    EspMod.LOGGER.error("[EspMod AI] Fallito il caricamento del file principale ONNX.");
                }
            } catch (Throwable t) {
                EspMod.LOGGER.error("[EspMod AI] CRITICAL ERROR during model load!", t);
            }
        });
    }

    private static void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null || !Config.enableEsp || !Config.enableChunkAnalysisAI
                || !isModelLoaded) {
            return;
        }

        ChunkPos currentChunkPos = client.player.getChunkPos();
        if (!currentChunkPos.equals(lastChunkPos)) {
            lastChunkPos = currentChunkPos;
            triggerInitialScan(client, currentChunkPos);
        }
    }

    private static void triggerInitialScan(MinecraftClient client, ChunkPos center) {
        int radius = Config.chunkAnalysisAISize / 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos target = new ChunkPos(center.x + x, center.z + z);
                // Debounce: non ri-analizzare se già fatto o in attesa
                if (!chunkResults.containsKey(target) && !pendingChunks.contains(target)) {
                    runInference(client, target);
                }
            }
        }
    }

    private static void runInference(MinecraftClient client, ChunkPos cp) {
        if (client.world == null)
            return;

        WorldChunk chunk = client.world.getChunk(cp.x, cp.z);
        if (chunk == null)
            return;

        // 1. RACCOLTA DATI SUL CLIENT THREAD (Thread Safety)
        // Tensore in formato One-Hot [1, 4, 16, 51, 16] appiattito
        float[] inputData = new float[4 * 16 * 51 * 16];
        float sum = 0;
        int startX = cp.getStartX();
        int startZ = cp.getStartZ();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y <= 50; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = client.world.getBlockState(pos);
                    int classIdx = Math.round(blockMapping.getOrDefault(state.getBlock(), 1.0f));

                    // index = c * (16 * 51 * 16) + x * (51 * 16) + y * 16 + z
                    int index = classIdx * (16 * 51 * 16) + x * (51 * 16) + y * 16 + z;
                    inputData[index] = 1.0f;
                    sum += classIdx;
                }
            }
        }

        final float finalSum = sum;
        pendingChunks.add(cp);

        // 2. INFERENZA IN BACKGROUND
        executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                EspMod.LOGGER.info("[EspMod AI] Sessione AI avviata per chunk [{}, {}]. Sum: {}", cp.x, cp.z, finalSum);

                if (finalSum == 0) {
                    EspMod.LOGGER.warn("[EspMod AI] Buffer vuoto (tutti 0) per chunk [{}, {}]. Salto.", cp.x, cp.z);
                    pendingChunks.remove(cp);
                    return;
                }

                long[] shape = new long[] { 1, 4, 16, 51, 16 };
                try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)) {
                    String inputName = session.getInputNames().iterator().next();
                    try (OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor))) {
                        float[][] output = (float[][]) result.get(0).getValue();
                        float probability = output[0][0];
                        chunkResults.put(cp, probability);

                        long duration = System.currentTimeMillis() - startTime;
                        EspMod.LOGGER.info("[EspMod AI] Sessione AI completata in {}ms. Probabilità: {}%",
                                duration, String.format("%.1f", probability * 100));

                        client.execute(() -> {
                            if (client.player != null) {
                                String type = probability > 0.80f ? "§cBASE"
                                        : (probability > 0.40f ? "§eANOMALIA" : "§aNATURALE");
                                client.player.sendMessage(
                                        Text.literal("§7[§dAI-Debug§7] " + type + " §fnel chunk [" + cp.x
                                                + ", " + cp.z + "] - Prob: "
                                                + String.format("%.2f%%", probability * 100) + " (" + duration + "ms)"),
                                        false);
                            }
                        });
                    }
                }
            } catch (Throwable t) {
                EspMod.LOGGER.error("[EspMod AI] Sessione AI fallita per chunk [{}, {}]", cp.x, cp.z, t);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(
                                "§c[EspMod AI] Errore ML per chunk [" + cp.x + ", " + cp.z + "]: " + t.toString()),
                                false);
                    }
                });
            } finally {
                pendingChunks.remove(cp);
            }
        });
    }

    public static void clearResults() {
        chunkResults.clear();
        pendingChunks.clear();
        lastChunkPos = null;
    }

    private static boolean isArtificial(String id) {
        return id.contains("planks") || id.contains("slab") || id.contains("stairs") ||
                id.contains("fence") || id.contains("chest") || id.contains("furnace") ||
                id.contains("torch") || id.contains("rail") || id.contains("bed") ||
                id.contains("cobblestone") || id.contains("stone_bricks");
    }

    private static boolean isAirOrDecorative(String id) {
        if (id.equals("minecraft:grass_block"))
            return false;
        return id.contains("air") || id.contains("grass") || id.contains("flower") ||
                id.contains("fern") || id.equals("minecraft:snow");
    }

    private static void onRender(WorldRenderContext context) {
        if (!Config.enableEsp || !Config.enableChunkAnalysisAI)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
            return;

        Vec3d cameraPos = context.camera().getPos();

        for (Map.Entry<ChunkPos, Float> entry : chunkResults.entrySet()) {
            float prob = entry.getValue();

            ChunkPos cp = entry.getKey();
            int color = prob > 0.80f ? 0xFFFF0000 : (prob > 0.40f ? 0xFFFFFF00 : 0xFF00FF00); // RED vs YELLOW vs GREEN

            double x = cp.getStartX() - cameraPos.x;
            double z = cp.getStartZ() - cameraPos.z;
            double yMin = 0 - cameraPos.y;
            double yMax = 50 - cameraPos.y;

            Box box = new Box(x, yMin, z, x + 16, yMax, z + 16);
            RenderUtils.drawBoxOutline(context.matrixStack(), box, color);
        }
    }
}
