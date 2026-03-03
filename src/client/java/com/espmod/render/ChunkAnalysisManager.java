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
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkAnalysisManager {

    // Exact block IDs that are unnatural (non-colored, specific blocks)
    private static final Set<String> UNNATURAL_EXACT = new HashSet<>(Arrays.asList(
            // Hard materials / crafted stone
            "minecraft:cobblestone", "minecraft:cobblestone_wall", "minecraft:cobblestone_slab",
            "minecraft:cobblestone_stairs",
            "minecraft:mossy_cobblestone", "minecraft:mossy_cobblestone_wall", "minecraft:mossy_cobblestone_slab",
            "minecraft:stone_bricks", "minecraft:cracked_stone_bricks",
            "minecraft:mossy_stone_bricks", "minecraft:chiseled_stone_bricks",
            "minecraft:stone_brick_slab", "minecraft:stone_brick_stairs", "minecraft:stone_brick_wall",
            "minecraft:bricks", "minecraft:brick_wall", "minecraft:brick_slab", "minecraft:brick_stairs",
            "minecraft:obsidian", "minecraft:crying_obsidian",
            // Glass (non-stained)
            "minecraft:glass", "minecraft:glass_pane", "minecraft:tinted_glass",
            // Wood planks (all types)
            "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
            "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
            "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:bamboo_planks",
            "minecraft:crimson_planks", "minecraft:warped_planks",
            // Crafted slabs from wood
            "minecraft:oak_slab", "minecraft:spruce_slab", "minecraft:birch_slab",
            "minecraft:jungle_slab", "minecraft:acacia_slab", "minecraft:dark_oak_slab",
            // Utilities
            "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest", "minecraft:barrel",
            "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
            "minecraft:crafting_table", "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil",
            "minecraft:enchanting_table", "minecraft:brewing_stand", "minecraft:beacon",
            "minecraft:hopper", "minecraft:dropper", "minecraft:dispenser",
            "minecraft:composter", "minecraft:grindstone", "minecraft:smithing_table",
            "minecraft:cartography_table", "minecraft:fletching_table", "minecraft:loom",
            "minecraft:stonecutter", "minecraft:jukebox", "minecraft:respawn_anchor",
            // Torches
            "minecraft:torch", "minecraft:wall_torch",
            "minecraft:soul_torch", "minecraft:soul_wall_torch",
            "minecraft:lantern", "minecraft:soul_lantern",
            // Redstone
            "minecraft:piston", "minecraft:sticky_piston", "minecraft:observer",
            "minecraft:redstone_lamp", "minecraft:note_block", "minecraft:target",
            "minecraft:lever", "minecraft:stone_button", "minecraft:oak_button",
            "minecraft:comparator", "minecraft:repeater", "minecraft:daylight_detector",
            // Rails / transport
            "minecraft:rail", "minecraft:powered_rail", "minecraft:detector_rail", "minecraft:activator_rail",
            // Doors / trapdoors
            "minecraft:oak_door", "minecraft:iron_door", "minecraft:iron_trapdoor",
            // Signs
            "minecraft:oak_sign", "minecraft:oak_wall_sign", "minecraft:oak_hanging_sign",
            // Item frame
            "minecraft:item_frame", "minecraft:glow_item_frame",
            // Specific naturally-absent
            "minecraft:hay_block", "minecraft:ladder", "minecraft:iron_bars",
            "minecraft:nether_brick", "minecraft:nether_brick_fence", "minecraft:nether_brick_stairs",
            "minecraft:quartz_block", "minecraft:chiseled_quartz_block", "minecraft:quartz_pillar",
            "minecraft:quartz_slab", "minecraft:quartz_stairs",
            "minecraft:purpur_block", "minecraft:purpur_pillar", "minecraft:purpur_slab", "minecraft:purpur_stairs",
            "minecraft:end_stone_bricks", "minecraft:end_stone_brick_slab"));

    // Suffixes: if a blockId (after removing "minecraft:") ENDS WITH one of these,
    // it's unnatural — covers ALL color variants automatically.
    private static final List<String> UNNATURAL_SUFFIXES = Arrays.asList(
            "_wool",
            "_carpet",
            "_concrete",
            "_concrete_powder",
            "_stained_glass",
            "_stained_glass_pane",
            "_glazed_terracotta",
            "_terracotta", // catches dyed terracotta like "red_terracotta"
            "_bed",
            "_banner",
            "_wall_banner",
            "_shulker_box",
            "_candle",
            "_planks", // catches any future wood types too
            "_door", // catches all wood doors + iron door
            "_trapdoor", // catches all wood trapdoors
            "_stairs", // catches all crafted stair variants
            "_fence_gate",
            "_button");

    /**
     * Returns true if the given block ID is considered unnatural / player-placed.
     */
    private static boolean isUnnatural(String blockId) {
        if (UNNATURAL_EXACT.contains(blockId))
            return true;
        String id = blockId.startsWith("minecraft:") ? blockId.substring(10) : blockId;
        for (String suffix : UNNATURAL_SUFFIXES) {
            if (id.endsWith(suffix))
                return true;
        }
        return false;
    }

    private static final Set<BlockPos> foundBlocks = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> patternBlocks = ConcurrentHashMap.newKeySet();
    private static Set<BlockPos> scanningBlocks = new HashSet<>();

    private static ChunkPos lastChunkPos = null;
    private static boolean isScanning = false;

    private static int scanCurrentY = 0;
    private static int scanChunkStartX = 0;
    private static int scanChunkStartZ = 0;
    private static int scanChunkEndX = 0;
    private static int scanChunkEndZ = 0;

    private static final int BLOCKS_PER_TICK = 50000;

    // Magenta = crafted blocks, Orange = dig patterns
    private static final int COLOR_MODIFIED = 0xFFFF00FF;
    private static final int COLOR_PATTERN = 0xFFFF8800;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ChunkAnalysisManager::onTick);
        WorldRenderEvents.LAST.register(ChunkAnalysisManager::onRender);
    }

    private static void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null || !Config.enableEsp || !Config.enableChunkAnalysis) {
            foundBlocks.clear();
            patternBlocks.clear();
            isScanning = false;
            lastChunkPos = null;
            return;
        }

        ChunkPos currentChunkPos = client.player.getChunkPos();

        if (!currentChunkPos.equals(lastChunkPos) && !isScanning) {
            lastChunkPos = currentChunkPos;
            isScanning = true;
            scanningBlocks = new HashSet<>();
            scanCurrentY = client.world.getBottomY();

            int size = Config.chunkAnalysisSize;
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
                boolean sectionLoaded = false;
                int sectionY = client.world.getSectionIndex(scanCurrentY);

                outer: for (int cx = scanChunkStartX >> 4; cx <= scanChunkEndX >> 4; cx++) {
                    for (int cz = scanChunkStartZ >> 4; cz <= scanChunkEndZ >> 4; cz++) {
                        WorldChunk wc = client.world.getChunk(cx, cz);
                        if (wc != null) {
                            ChunkSection[] sections = wc.getSectionArray();
                            if (sectionY >= 0 && sectionY < sections.length && !sections[sectionY].isEmpty()) {
                                sectionLoaded = true;
                                break outer;
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
                            if (isUnnatural(blockId)) {
                                scanningBlocks.add(pos);
                            }
                            blocksScannedThisTick++;
                        }
                    }
                } else {
                    blocksScannedThisTick += (scanChunkEndX - scanChunkStartX + 1)
                            * (scanChunkEndZ - scanChunkStartZ + 1);
                }
                scanCurrentY++;
            }

            if (scanCurrentY >= topY) {
                foundBlocks.clear();
                foundBlocks.addAll(scanningBlocks);
                isScanning = false;

                // Run pattern detection on the finished scan area
                detectPatterns(client);

                if (client.player != null) {
                    String patternMsg = patternBlocks.isEmpty() ? ""
                            : " §8| §6" + patternBlocks.size() + " pattern scavi";
                    if (foundBlocks.isEmpty() && patternBlocks.isEmpty()) {
                        client.player.sendMessage(net.minecraft.text.Text.literal(
                                "§8[§dAnalysis§8] §fNessun blocco modificato trovato nel "
                                        + Config.chunkAnalysisSize + "x" + Config.chunkAnalysisSize + " chunk grid."),
                                false);
                    } else {
                        Map<String, Integer> counts = new HashMap<>();
                        for (BlockPos p : foundBlocks) {
                            String id = Registries.BLOCK.getId(client.world.getBlockState(p).getBlock()).toString();
                            counts.put(id, counts.getOrDefault(id, 0) + 1);
                        }

                        client.player.sendMessage(net.minecraft.text.Text.literal(
                                "§8[§dAnalysis§8] §fTrovati §e" + foundBlocks.size()
                                        + " §fblocchi modificati" + patternMsg + "!"),
                                false);

                        // Show top 5 block types
                        counts.entrySet().stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .limit(5)
                                .forEach(e -> client.player.sendMessage(
                                        net.minecraft.text.Text.literal(
                                                "  §8» §b" + e.getKey().replace("minecraft:", "") + " §f× §e"
                                                        + e.getValue()),
                                        false));

                        if (!patternBlocks.isEmpty()) {
                            client.player.sendMessage(net.minecraft.text.Text.literal(
                                    "  §8» §6" + patternBlocks.size() + " potenziali scavi rilevati §7(arancione)"),
                                    false);
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stone-family blocks considered "uniform walls" of a player-dug tunnel
    private static final Set<String> STONE_FAMILY = new HashSet<>(Arrays.asList(
            "minecraft:stone", "minecraft:deepslate", "minecraft:cobbled_deepslate",
            "minecraft:granite", "minecraft:diorite", "minecraft:andesite",
            "minecraft:tuff", "minecraft:calcite", "minecraft:smooth_stone",
            "minecraft:netherrack", "minecraft:end_stone"));

    /**
     * Returns true if the block is part of the "stone family" (natural solid
     * walls).
     */
    private static boolean isStoneFamily(MinecraftClient client, BlockPos pos) {
        String id = Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()).toString();
        return STONE_FAMILY.contains(id);
    }

    /**
     * Suspicion score for a run — higher = more likely player-dug.
     *
     * @param client      MC client
     * @param midPos      representative midpoint of the run
     * @param perpW1      perpendicular width at 25% of run length
     * @param perpW2      perpendicular width at 75% of run length
     * @param wallSamples list of BlockPos on the walls of the run
     * @return integer score (flag if >= SCORE_THRESHOLD)
     */
    private static final int SCORE_THRESHOLD = 4;

    private static int scoreRun(MinecraftClient client, BlockPos midPos,
            int perpW1, int perpW2, List<BlockPos> wallSamples) {
        int score = 0;

        // ── Idea 1: Width consistency ──────────────────────────────────────
        int widthVar = Math.abs(perpW1 - perpW2);
        if (widthVar == 0)
            score += 3; // perfectly uniform
        else if (widthVar == 1)
            score += 1; // mostly uniform
        else
            score -= 2; // irregular = natural cave

        // ── Idea 2: Wall uniformity (stone-family ratio) ───────────────────
        if (!wallSamples.isEmpty()) {
            long stoneCount = wallSamples.stream()
                    .filter(wp -> isStoneFamily(client, wp))
                    .count();
            double ratio = (double) stoneCount / wallSamples.size();
            if (ratio >= 0.85)
                score += 2; // nearly all stone = player tunnel
            else if (ratio < 0.50)
                score -= 1; // lots of non-stone = natural
        }

        // ── Idea 3: Proximity to any crafted/unnatural block ───────────────
        // foundBlocks holds every block matched by isUnnatural() in the scan
        int bestDist = Integer.MAX_VALUE;
        for (BlockPos fp : foundBlocks) {
            int dist = (int) Math.sqrt(fp.getSquaredDistance(midPos));
            if (dist < bestDist)
                bestDist = dist;
        }
        if (bestDist <= 10)
            score += 5; // crafted block very close
        else if (bestDist <= 20)
            score += 3; // crafted block nearby
        else if (bestDist <= 40)
            score += 1; // crafted block somewhat near

        return score;
    }

    /** Scan the completed area for dig patterns (shafts, tunnels). */
    private static void detectPatterns(MinecraftClient client) {
        patternBlocks.clear();
        if (client.world == null)
            return;

        int bottomY = client.world.getBottomY();
        int topY = client.world.getTopY();

        // ── 1×1 Vertical Shafts ──────────────────────────────────────────────
        // Requirements: ≥8 consecutive air, all 4 walls solid at every sampled Y,
        // score ≥ SCORE_THRESHOLD
        for (int x = scanChunkStartX; x <= scanChunkEndX; x++) {
            for (int z = scanChunkStartZ; z <= scanChunkEndZ; z++) {
                int airRun = 0;
                int runStartY = bottomY;
                for (int y = bottomY; y <= Math.min(topY - 1, 100); y++) {
                    boolean isAir = client.world.getBlockState(new BlockPos(x, y, z)).isAir();
                    if (isAir) {
                        if (airRun == 0)
                            runStartY = y;
                        airRun++;
                    } else {
                        if (airRun >= 8) {
                            int midY = runStartY + airRun / 2;
                            // Idea 1 for shafts: check all 4 walls solid at 3 sample points
                            boolean walled = isShaftWalled(client, x, runStartY + 2, z)
                                    && isShaftWalled(client, x, midY, z)
                                    && isShaftWalled(client, x, runStartY + airRun - 3, z);
                            if (walled) {
                                // Collect wall samples (Idea 2)
                                List<BlockPos> walls = new ArrayList<>(Arrays.asList(
                                        new BlockPos(x + 1, midY, z), new BlockPos(x - 1, midY, z),
                                        new BlockPos(x, midY, z + 1), new BlockPos(x, midY, z - 1)));
                                BlockPos midPos = new BlockPos(x, midY, z);
                                int score = scoreRun(client, midPos, 1, 1, walls); // width=1 guaranteed
                                if (score >= SCORE_THRESHOLD) {
                                    for (int fy = runStartY; fy < runStartY + airRun; fy++)
                                        patternBlocks.add(new BlockPos(x, fy, z));
                                }
                            }
                        }
                        airRun = 0;
                    }
                }
            }
        }

        // ── Horizontal Tunnels (X axis) ───────────────────────────────────────
        // Requirements: ≥8 air in a row, solid floor & ceiling, score ≥ threshold
        for (int y = bottomY; y < topY; y++) {
            for (int z = scanChunkStartZ; z <= scanChunkEndZ; z++) {
                int airRun = 0;
                int runStartX = scanChunkStartX;
                for (int x = scanChunkStartX; x <= scanChunkEndX + 1; x++) {
                    boolean isAir = x <= scanChunkEndX &&
                            client.world.getBlockState(new BlockPos(x, y, z)).isAir();
                    if (isAir) {
                        if (airRun == 0)
                            runStartX = x;
                        airRun++;
                    } else {
                        if (airRun >= 8) {
                            int q1x = runStartX + airRun / 4;
                            int q3x = runStartX + (3 * airRun) / 4;
                            int midX = runStartX + airRun / 2;
                            // Check solid floor everywhere
                            boolean solidFloor = !client.world.getBlockState(new BlockPos(q1x, y - 1, z)).isAir()
                                    && !client.world.getBlockState(new BlockPos(midX, y - 1, z)).isAir()
                                    && !client.world.getBlockState(new BlockPos(q3x, y - 1, z)).isAir();
                            if (solidFloor) {
                                // Idea 1: measure perpendicular width (Z) at q1 and q3
                                int w1 = perpWidth(client, q1x, y, z, false);
                                int w3 = perpWidth(client, q3x, y, z, false);
                                // Idea 2: sample wall blocks
                                List<BlockPos> walls = new ArrayList<>(Arrays.asList(
                                        new BlockPos(q1x, y - 1, z), new BlockPos(q1x, y + 1, z),
                                        new BlockPos(midX, y - 1, z), new BlockPos(midX, y + 1, z),
                                        new BlockPos(q3x, y - 1, z), new BlockPos(q3x, y + 1, z)));
                                int score = scoreRun(client, new BlockPos(midX, y, z), w1, w3, walls);
                                if (score >= SCORE_THRESHOLD) {
                                    for (int fx = runStartX; fx < runStartX + airRun; fx++)
                                        patternBlocks.add(new BlockPos(fx, y, z));
                                }
                            }
                        }
                        airRun = 0;
                    }
                }
            }
        }

        // ── Horizontal Tunnels (Z axis) ───────────────────────────────────────
        for (int y = bottomY; y < topY; y++) {
            for (int x = scanChunkStartX; x <= scanChunkEndX; x++) {
                int airRun = 0;
                int runStartZ = scanChunkStartZ;
                for (int z = scanChunkStartZ; z <= scanChunkEndZ + 1; z++) {
                    boolean isAir = z <= scanChunkEndZ &&
                            client.world.getBlockState(new BlockPos(x, y, z)).isAir();
                    if (isAir) {
                        if (airRun == 0)
                            runStartZ = z;
                        airRun++;
                    } else {
                        if (airRun >= 8) {
                            int q1z = runStartZ + airRun / 4;
                            int q3z = runStartZ + (3 * airRun) / 4;
                            int midZ = runStartZ + airRun / 2;
                            boolean solidFloor = !client.world.getBlockState(new BlockPos(x, y - 1, q1z)).isAir()
                                    && !client.world.getBlockState(new BlockPos(x, y - 1, midZ)).isAir()
                                    && !client.world.getBlockState(new BlockPos(x, y - 1, q3z)).isAir();
                            if (solidFloor) {
                                int w1 = perpWidth(client, x, y, q1z, true);
                                int w3 = perpWidth(client, x, y, q3z, true);
                                List<BlockPos> walls = new ArrayList<>(Arrays.asList(
                                        new BlockPos(x, y - 1, q1z), new BlockPos(x, y + 1, q1z),
                                        new BlockPos(x, y - 1, midZ), new BlockPos(x, y + 1, midZ),
                                        new BlockPos(x, y - 1, q3z), new BlockPos(x, y + 1, q3z)));
                                int score = scoreRun(client, new BlockPos(x, y, midZ), w1, w3, walls);
                                if (score >= SCORE_THRESHOLD) {
                                    for (int fz = runStartZ; fz < runStartZ + airRun; fz++)
                                        patternBlocks.add(new BlockPos(x, y, fz));
                                }
                            }
                        }
                        airRun = 0;
                    }
                }
            }
        }
    }

    /**
     * Measures the perpendicular width of an air void at a given position.
     * 
     * @param alongZ true = tunnel runs along Z (measure in X), false = tunnel along
     *               X (measure in Z)
     */
    private static int perpWidth(MinecraftClient client, int x, int y, int z, boolean alongZ) {
        int count = 0;
        if (alongZ) {
            // tunnel is Z-axis, measure X spread
            for (int dx = -4; dx <= 4; dx++)
                if (client.world.getBlockState(new BlockPos(x + dx, y, z)).isAir())
                    count++;
        } else {
            // tunnel is X-axis, measure Z spread
            for (int dz = -4; dz <= 4; dz++)
                if (client.world.getBlockState(new BlockPos(x, y, z + dz)).isAir())
                    count++;
        }
        return count;
    }

    /** Returns true if all 4 horizontal neighbours are non-air. */
    private static boolean isShaftWalled(MinecraftClient client, int x, int y, int z) {
        return !client.world.getBlockState(new BlockPos(x + 1, y, z)).isAir()
                && !client.world.getBlockState(new BlockPos(x - 1, y, z)).isAir()
                && !client.world.getBlockState(new BlockPos(x, y, z + 1)).isAir()
                && !client.world.getBlockState(new BlockPos(x, y, z - 1)).isAir();
    }

    private static void onRender(WorldRenderContext context) {
        if (!Config.enableEsp || !Config.enableChunkAnalysis)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        Vec3d cameraPos = context.camera().getPos();

        // Draw crafted blocks in magenta
        for (BlockPos pos : foundBlocks) {
            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;
            RenderUtils.drawBoxOutline(context.matrixStack(), new Box(x, y, z, x + 1, y + 1, z + 1), COLOR_MODIFIED);
        }

        // Draw pattern blocks in orange
        for (BlockPos pos : patternBlocks) {
            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;
            RenderUtils.drawBoxOutline(context.matrixStack(), new Box(x, y, z, x + 1, y + 1, z + 1), COLOR_PATTERN);
        }
    }

    /** Force re-scan (for manual trigger) */
    public static void rescan() {
        lastChunkPos = null;
    }
}
