package com.espmod.render;

import com.espmod.config.Config;
import com.espmod.utils.RenderUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkScannerManager {
    private static final Set<BlockPos> foundBlocks = ConcurrentHashMap.newKeySet();
    private static Set<BlockPos> scanningBlocks = new HashSet<>();

    private static ChunkPos lastChunkPos = null;
    private static boolean isScanning = false;

    // Scan progress state
    private static int scanCurrentY = 0;
    private static int scanChunkStartX = 0;
    private static int scanChunkStartZ = 0;
    private static int scanChunkEndX = 0;
    private static int scanChunkEndZ = 0;

    private static final int BLOCKS_PER_TICK = 50000;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick(client));
        WorldRenderEvents.LAST.register(ChunkScannerManager::onRender);
    }

    private static void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null || !Config.enableEsp || !Config.enableChunkScanner) {
            foundBlocks.clear();
            isScanning = false;
            lastChunkPos = null;
            return;
        }

        ChunkPos currentChunkPos = client.player.getChunkPos();

        // Trigger new scan if chunk changed
        if (!currentChunkPos.equals(lastChunkPos) && !isScanning) {
            lastChunkPos = currentChunkPos;
            isScanning = true;
            scanningBlocks = new HashSet<>();
            scanCurrentY = client.world.getBottomY();

            // Calculate grid bounds based on chunkScannerSize (1 = 1x1 chunk, 2 = 2x2
            // chunks)
            int size = Config.chunkScannerSize;
            // Center the grid on the player's current chunk
            int halfSize = size / 2;
            scanChunkStartX = (currentChunkPos.x - halfSize) * 16;
            scanChunkStartZ = (currentChunkPos.z - halfSize) * 16;
            scanChunkEndX = (currentChunkPos.x - halfSize + size) * 16 - 1;
            scanChunkEndZ = (currentChunkPos.z - halfSize + size) * 16 - 1;
        }

        if (isScanning) {
            int blocksScannedThisTick = 0;
            int topY = client.world.getTopY();

            while (blocksScannedThisTick < BLOCKS_PER_TICK && scanCurrentY < topY) {
                // Skip scanning this Y-layer if EVERY chunk column has an empty section here
                // A chunk section covers 16 Y-levels starting at sectionY * 16 + bottomY
                boolean sectionLoaded = false;
                int sectionY = client.world.getSectionIndex(scanCurrentY);

                // Check if at least one chunk in the grid has a non-empty section at this Y
                outer: for (int cx = scanChunkStartX >> 4; cx <= scanChunkEndX >> 4; cx++) {
                    for (int cz = scanChunkStartZ >> 4; cz <= scanChunkEndZ >> 4; cz++) {
                        net.minecraft.world.chunk.WorldChunk wc = client.world.getChunk(cx, cz);
                        if (wc != null) {
                            ChunkSection[] sections = wc.getSectionArray();
                            if (sectionY >= 0 && sectionY < sections.length) {
                                if (!sections[sectionY].isEmpty()) {
                                    sectionLoaded = true;
                                    break outer;
                                }
                            }
                        }
                    }
                }

                if (sectionLoaded) {
                    for (int x = scanChunkStartX; x <= scanChunkEndX; x++) {
                        for (int z = scanChunkStartZ; z <= scanChunkEndZ; z++) {
                            BlockPos pos = new BlockPos(x, scanCurrentY, z);
                            BlockState state = client.world.getBlockState(pos);

                            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
                            if (Config.blockList.contains(blockId)) {
                                scanningBlocks.add(pos);
                            }
                            blocksScannedThisTick++;
                        }
                    }
                } else {
                    // Skip entire Y-layer but count it as scanned to advance
                    blocksScannedThisTick += (scanChunkEndX - scanChunkStartX + 1)
                            * (scanChunkEndZ - scanChunkStartZ + 1);
                }
                scanCurrentY++;
            }

            if (scanCurrentY >= topY) {
                foundBlocks.clear();
                foundBlocks.addAll(scanningBlocks);
                isScanning = false;

                if (client.player != null && !foundBlocks.isEmpty()) {
                    java.util.Map<String, Integer> counts = new java.util.HashMap<>();
                    for (BlockPos p : foundBlocks) {
                        String id = Registries.BLOCK.getId(client.world.getBlockState(p).getBlock()).toString();
                        counts.put(id, counts.getOrDefault(id, 0) + 1);
                    }
                    for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
                        client.player.sendMessage(net.minecraft.text.Text.literal(
                                "§8[§bEspMod§8] §f[§b" + entry.getKey() + "§f]: §e" + entry.getValue()), false);
                    }
                }
            }
        }
    }

    private static void onRender(WorldRenderContext context) {
        if (!Config.enableEsp || !Config.enableChunkScanner)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        Vec3d cameraPos = context.camera().getPos();
        int color = Config.getColorBlock(); // Use same color as block ESP

        for (BlockPos pos : foundBlocks) {
            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;

            // Only draw box outline, NO tracer line
            Box box = new Box(x, y, z, x + 1, y + 1, z + 1);
            RenderUtils.drawBoxOutline(context.matrixStack(), box, color);
        }
    }
}
