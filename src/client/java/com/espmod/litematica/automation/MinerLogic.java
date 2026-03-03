package com.espmod.litematica.automation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import java.util.Queue;
import java.util.LinkedList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import com.espmod.litematica.ChestAreaManager;

public class MinerLogic {

    private enum MinerState {
        CHECK_TOOLS,
        FIND_CHESTS,
        MOVE_TO_CHEST,
        LOOT_CHEST,
        FIND_SCHEMATIC,
        MOVE_TO_SCHEMATIC,
        FIND_WORK,
        MOVE_TO_MINE
    }

    private static MinerState state = MinerState.CHECK_TOOLS;
    private static BlockPos targetBlock = null;
    private static BlockPos targetChest = null;
    private static BlockPos targetSchematic = null;
    private static int actionTimer = 0;

    private static Queue<BlockPos> currentPath = new LinkedList<>();
    private static BlockPos currentTarget = null;
    private static int pathRecalcTimer = 0;

    public static void reset() {
        state = MinerState.CHECK_TOOLS;
        targetBlock = null;
        targetChest = null;
        targetSchematic = null;
        actionTimer = 0;
        currentPath.clear();
        currentTarget = null;
        pathRecalcTimer = 0;
    }

    public static boolean tick(MinecraftClient client) {
        if (client.player == null || client.world == null)
            return false;

        WorldSchematic schWorld = null;
        try {
            schWorld = SchematicWorldHandler.getSchematicWorld();
        } catch (Exception e) {
        }

        switch (state) {
            case CHECK_TOOLS:
                // Move on regardless if we have tools or not, since user wants manual mining
                // possible
                if (!hasMiningTool(client) && !ChestAreaManager.chestZones.isEmpty()) {
                    state = MinerState.FIND_CHESTS;
                } else {
                    state = MinerState.FIND_SCHEMATIC;
                }
                return false;
            case FIND_CHESTS:
                if (ChestAreaManager.chestZones.isEmpty()) {
                    client.player.sendMessage(net.minecraft.text.Text.literal("§c[Miner] No Chest Zones defined!"),
                            true);
                    return false;
                }
                // Pick the first available chest zone for simplicity
                ChestAreaManager.ChestZone zone = ChestAreaManager.chestZones.values().iterator().next();
                targetChest = findChestInZone(client, zone);
                if (targetChest != null) {
                    state = MinerState.MOVE_TO_CHEST;
                } else {
                    client.player.sendMessage(net.minecraft.text.Text.literal("§c[Miner] No chests found in zone!"),
                            true);
                    return true;
                }
                return false;

            case MOVE_TO_CHEST:
                if (targetChest == null) {
                    state = MinerState.FIND_CHESTS;
                    return false;
                }

                if (client.player.getBlockPos().getSquaredDistance(targetChest) > 6.25) {
                    moveTo(client, targetChest, 2.5);
                } else {
                    AutomationEngine.resetInputs(client);
                    if (actionTimer-- <= 0) {
                        lookAt(client, targetChest);
                        client.options.sneakKey.setPressed(true);
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                                new net.minecraft.util.hit.BlockHitResult(
                                        new net.minecraft.util.math.Vec3d(targetChest.getX(), targetChest.getY(),
                                                targetChest.getZ()),
                                        net.minecraft.util.math.Direction.UP, targetChest, false));
                        client.options.sneakKey.setPressed(false);
                        actionTimer = 10;
                        state = MinerState.LOOT_CHEST;
                    }
                }
                return false;

            case LOOT_CHEST:
                if (actionTimer > 0)
                    actionTimer--;

                if (client.currentScreen instanceof GenericContainerScreen chestScreen && actionTimer <= 0) {
                    boolean found = false;
                    for (int i = 0; i < chestScreen.getScreenHandler().getInventory().size(); i++) {
                        ItemStack stack = chestScreen.getScreenHandler().getInventory().getStack(i);
                        if (stack.getItem().toString().contains("pickaxe") ||
                                stack.getItem().toString().contains("axe") ||
                                stack.getItem().toString().contains("shovel")) {
                            client.interactionManager.clickSlot(chestScreen.getScreenHandler().syncId, i, 0,
                                    SlotActionType.QUICK_MOVE, client.player);
                            found = true;
                            actionTimer = 10;
                            break;
                        }
                    }
                    if (actionTimer <= 0) {
                        client.player.closeHandledScreen();
                        state = MinerState.CHECK_TOOLS;
                    }
                } else if (!(client.currentScreen instanceof GenericContainerScreen) && actionTimer <= 0) {
                    state = MinerState.MOVE_TO_CHEST; // Retry
                }
                return false;

            case FIND_SCHEMATIC:
                // Try grabbing position from manual area first
                if (com.espmod.litematica.MiningAreaManager.activeZone != null) {
                    com.espmod.litematica.MiningAreaManager.MiningZone mz = com.espmod.litematica.MiningAreaManager.activeZone;
                    int midX = (mz.x1 + mz.x2) / 2;
                    int midY = (mz.y1 + mz.y2) / 2;
                    int midZ = (mz.z1 + mz.z2) / 2;
                    targetSchematic = new BlockPos(midX, midY, midZ);
                    state = MinerState.MOVE_TO_SCHEMATIC;
                    return false;
                }

                // Fallback to Litematica schematic (if any)
                for (SchematicPlacement placement : DataManager.getSchematicPlacementManager()
                        .getAllSchematicsPlacements()) {
                    if (placement.isEnabled()) {
                        targetSchematic = placement.getOrigin();
                        state = MinerState.MOVE_TO_SCHEMATIC;
                        return false;
                    }
                }
                client.player.sendMessage(net.minecraft.text.Text
                        .literal("§c[Miner] No Mining Area or Schematic found! Use .at set A / .at set B"), true);
                return true; // We are completely done if no schematic.

            case MOVE_TO_SCHEMATIC:
                if (targetSchematic == null) {
                    state = MinerState.FIND_SCHEMATIC;
                    return false;
                }
                if (client.player.getBlockPos().getSquaredDistance(targetSchematic) > 100.0) { // Keep moving if > 10
                                                                                               // blocks away
                    moveTo(client, targetSchematic, 8.0);
                    return false;
                } else {
                    AutomationEngine.resetInputs(client);
                    state = MinerState.FIND_WORK;
                    return false;
                }

            case FIND_WORK:
                targetBlock = findMismatchBlock(client, schWorld);
                if (targetBlock != null) {
                    state = MinerState.MOVE_TO_MINE;
                    return false;
                }
                // Completely finished mining
                return true;

            case MOVE_TO_MINE:
                if (targetBlock == null) {
                    state = MinerState.FIND_WORK;
                    return false;
                }
                BlockState actual = client.world.getBlockState(targetBlock);
                BlockState expected = null;
                if (schWorld != null) {
                    expected = schWorld.getBlockState(targetBlock);
                }

                // If it's air, Or if we have a schematic and actual matches expected, we are
                // done with this block
                if (actual.isAir() || (expected != null && actual.getBlock() == expected.getBlock())) {
                    targetBlock = null;
                    state = MinerState.FIND_WORK;
                    AutomationEngine.resetInputs(client);
                    return false;
                }

                equipMiningTool(client); // Will return false if no tool, but we can still punch (slower)
                mineTarget(client, targetBlock);
                return false;
        }

        return false; // No work found
    }

    private static BlockPos findMismatchBlock(MinecraftClient client, WorldSchematic schWorld) {
        int r = 10;
        BlockPos playerPos = client.player.getBlockPos();

        com.espmod.litematica.MiningAreaManager.MiningZone zone = com.espmod.litematica.MiningAreaManager.activeZone;

        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = playerPos.add(x, y, z);

                    if (zone != null) {
                        int minX = Math.min(zone.x1, zone.x2);
                        int minY = Math.min(zone.y1, zone.y2);
                        int minZ = Math.min(zone.z1, zone.z2);
                        int maxX = Math.max(zone.x1, zone.x2);
                        int maxY = Math.max(zone.y1, zone.y2);
                        int maxZ = Math.max(zone.z1, zone.z2);

                        // STRICT BOUNDARY CHECK - if outside, skip
                        if (p.getX() < minX || p.getX() > maxX || p.getY() < minY || p.getY() > maxY || p.getZ() < minZ
                                || p.getZ() > maxZ) {
                            continue;
                        }
                    }

                    BlockState actual = client.world.getBlockState(p);

                    if (actual.isAir() || actual.getBlock() == Blocks.BEDROCK || actual.getFluidState().isStill())
                        continue;

                    // IF MANUALLY SCANNED AN AREA AND NO LITEMATICA SCHEMATIC IS GIVEN NO PROBLEM
                    // WE MINE EVERYTHING
                    if (schWorld == null) {
                        return p;
                    }

                    BlockState expected = schWorld.getBlockState(p);
                    if (expected == null || expected.isAir()) {
                        // If schematic expects air but actual is a block, this is a mismatch, MINE IT!
                        return p;
                    }

                    // If different block type and actual is not air, MINE IT!
                    if (expected.getBlock() != actual.getBlock()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasMiningTool(MinecraftClient client) {
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            String itemName = client.player.getInventory().getStack(i).getItem().toString();
            if (itemName.contains("pickaxe") || itemName.contains("axe") || itemName.contains("shovel")) {
                return true;
            }
        }
        return false;
    }

    private static boolean equipMiningTool(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            String itemName = client.player.getInventory().getStack(i).getItem().toString();
            if (itemName.contains("pickaxe") || itemName.contains("axe") || itemName.contains("shovel")) {
                client.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    private static void mineTarget(MinecraftClient client, BlockPos target) {
        double dist = client.player.getBlockPos().getSquaredDistance(target);
        if (dist > 16) {
            moveTo(client, target, 3.5);
            client.options.attackKey.setPressed(false);
        } else {
            AutomationEngine.resetInputs(client);
            lookAt(client, target);
            client.options.attackKey.setPressed(true);
        }
    }

    private static void moveTo(MinecraftClient client, BlockPos target, double reach) {
        if (currentTarget == null || !currentTarget.equals(target) || (currentPath.isEmpty() && pathRecalcTimer <= 0)) {
            currentPath = com.espmod.litematica.automation.pathfinding.Pathfinder.findPath(client,
                    client.player.getBlockPos(), target);
            currentTarget = target;
            pathRecalcTimer = 20; // Try again in a bit if no path
        }

        if (pathRecalcTimer > 0)
            pathRecalcTimer--;

        // If no path was found, fallback to direct moveTo
        if (currentPath.isEmpty()) {
            double dX = target.getX() - client.player.getX();
            double dZ = target.getZ() - client.player.getZ();
            if (dX * dX + dZ * dZ > reach * reach) {
                lookAt(client, target);
                client.options.forwardKey.setPressed(true);
                client.options.jumpKey.setPressed(client.player.horizontalCollision);
            } else {
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
            }
            return;
        }

        BlockPos nextNode = currentPath.peek();
        double dX = nextNode.getX() + 0.5 - client.player.getX();
        double dZ = nextNode.getZ() + 0.5 - client.player.getZ();
        double distSq = dX * dX + dZ * dZ;

        if (distSq < 0.3) {
            currentPath.poll(); // Reached this node
            client.options.jumpKey.setPressed(false);
            if (currentPath.isEmpty()) {
                client.options.forwardKey.setPressed(false);
                return;
            }
            nextNode = currentPath.peek();
        }

        lookAt(client, nextNode);
        client.options.forwardKey.setPressed(true);

        // Jump if needed
        if (nextNode.getY() > client.player.getBlockY()) {
            client.options.jumpKey.setPressed(true);
        } else {
            client.options.jumpKey.setPressed(false);
        }
    }

    private static void lookAt(MinecraftClient client, BlockPos target) {
        double dX = target.getX() + 0.5 - client.player.getX();
        double dY = target.getY() + 0.5 - client.player.getEyeY();
        double dZ = target.getZ() + 0.5 - client.player.getZ();
        double distXZ = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float) Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dY, distXZ));
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }

    private static BlockPos findChestInZone(MinecraftClient client, ChestAreaManager.ChestZone zone) {
        int minX = Math.min(zone.x1, zone.x2);
        int minY = Math.min(zone.y1, zone.y2);
        int minZ = Math.min(zone.z1, zone.z2);
        int maxX = Math.max(zone.x1, zone.x2);
        int maxY = Math.max(zone.y1, zone.y2);
        int maxZ = Math.max(zone.z1, zone.z2);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    net.minecraft.block.Block b = client.world.getBlockState(p).getBlock();
                    if (b == net.minecraft.block.Blocks.CHEST || b == net.minecraft.block.Blocks.TRAPPED_CHEST
                            || b == net.minecraft.block.Blocks.BARREL) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
}
