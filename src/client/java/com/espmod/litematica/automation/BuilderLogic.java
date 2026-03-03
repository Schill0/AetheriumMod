package com.espmod.litematica.automation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Hand;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import java.util.Queue;
import java.util.LinkedList;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import com.espmod.litematica.ChestAreaManager;

public class BuilderLogic {

    private enum BuilderState {
        CHECK_BLOCKS,
        FIND_CHESTS,
        MOVE_TO_CHEST,
        LOOT_CHEST,
        FIND_SCHEMATIC,
        MOVE_TO_SCHEMATIC,
        FIND_WORK,
        MOVE_TO_BUILD
    }

    private static BuilderState state = BuilderState.CHECK_BLOCKS;
    private static BlockPos targetBlock = null;
    private static BlockPos targetChest = null;
    private static java.util.List<BlockPos> chestList = new java.util.ArrayList<>();
    private static int chestIndex = 0;
    private static BlockPos targetSchematic = null;
    private static Item requiredItem = null;

    private static int actionTimer = 0;

    private static Queue<BlockPos> currentPath = new LinkedList<>();
    private static BlockPos currentTarget = null;
    private static int pathRecalcTimer = 0;

    public static void reset() {
        state = BuilderState.CHECK_BLOCKS;
        targetBlock = null;
        targetChest = null;
        chestList.clear();
        chestIndex = 0;
        targetSchematic = null;
        requiredItem = null;
        actionTimer = 0;
        currentPath.clear();
        currentTarget = null;
        pathRecalcTimer = 0;
    }

    public static boolean tick(MinecraftClient client) {
        if (client.player == null || client.world == null)
            return false;

        WorldSchematic schWorld = SchematicWorldHandler.getSchematicWorld();
        if (schWorld == null)
            return false;

        switch (state) {
            case CHECK_BLOCKS:
                if (hasAnyBlock(client)) {
                    state = BuilderState.FIND_SCHEMATIC;
                } else {
                    state = BuilderState.FIND_CHESTS;
                }
                return false;

            case FIND_CHESTS:
                if (ChestAreaManager.chestZones.isEmpty()) {
                    client.player.sendMessage(Text.literal("§c[Builder] No Chest Zones defined!"), true);
                    return false;
                }
                ChestAreaManager.ChestZone zone = ChestAreaManager.chestZones.values().iterator().next();
                chestList = findAllChestsInZone(client, zone);
                if (!chestList.isEmpty()) {
                    chestIndex = 0;
                    targetChest = chestList.get(0);
                    state = BuilderState.MOVE_TO_CHEST;
                } else {
                    client.player.sendMessage(Text.literal("§c[Builder] No chests found in zone!"), true);
                    return true;
                }
                return false;

            case MOVE_TO_CHEST:
                if (targetChest == null) {
                    state = BuilderState.FIND_CHESTS;
                    return false;
                }

                if (client.player.getBlockPos().getSquaredDistance(targetChest) > 6.25) { // 2.5 blocks away
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
                        state = BuilderState.LOOT_CHEST;
                    }
                }
                return false;

            case LOOT_CHEST:
                if (client.currentScreen instanceof GenericContainerScreen screen) {
                    if (actionTimer-- <= 0) {
                        try {
                            boolean foundItem = false;
                            for (int i = 0; i < screen.getScreenHandler().getInventory().size(); i++) {
                                ItemStack stack = screen.getScreenHandler().getInventory().getStack(i);
                                if (stack != null && stack.isOf(requiredItem)) {
                                    client.interactionManager.clickSlot(screen.getScreenHandler().syncId, i, 0,
                                            SlotActionType.QUICK_MOVE, client.player);
                                    foundItem = true;
                                    break;
                                }
                            }

                            client.player.closeHandledScreen();
                            AutomationEngine.resetInputs(client);

                            if (foundItem || hasItemInInventory(client, requiredItem)) {
                                state = BuilderState.FIND_SCHEMATIC;
                            } else {
                                chestIndex++;
                                if (chestIndex < chestList.size()) {
                                    targetChest = chestList.get(chestIndex);
                                    state = BuilderState.MOVE_TO_CHEST;
                                } else {
                                    client.player.sendMessage(
                                            Text.literal("§c[Builder] Required item "
                                                    + requiredItem.getName().getString() + " not found in any chests!"),
                                            true);
                                    return true; // Stop automation
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            client.player.closeHandledScreen();
                        }
                    }
                } else {
                    if (client.player.getBlockPos().getSquaredDistance(targetChest) > 16.0) {
                        state = BuilderState.MOVE_TO_CHEST;
                    } else {
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                                new net.minecraft.util.hit.BlockHitResult(
                                        new net.minecraft.util.math.Vec3d(targetChest.getX(), targetChest.getY(),
                                                targetChest.getZ()),
                                        net.minecraft.util.math.Direction.UP, targetChest, false));
                        actionTimer = 20; // Wait 1 second for GUI
                    }
                }
                return false;

            case FIND_SCHEMATIC:
                for (SchematicPlacement placement : DataManager.getSchematicPlacementManager()
                        .getAllSchematicsPlacements()) {
                    if (placement.isEnabled()) {
                        targetSchematic = placement.getOrigin();
                        state = BuilderState.MOVE_TO_SCHEMATIC;
                        return false;
                    }
                }
                client.player.sendMessage(net.minecraft.text.Text.literal("§c[Builder] No Schematic found!"), true);
                return true; // Finished because we can't do anything

            case MOVE_TO_SCHEMATIC:
                if (targetSchematic == null) {
                    state = BuilderState.FIND_SCHEMATIC;
                    return false;
                }
                if (client.player.getBlockPos().getSquaredDistance(targetSchematic) > 100.0) {
                    moveTo(client, targetSchematic, 8.0);
                } else {
                    AutomationEngine.resetInputs(client);
                    state = BuilderState.FIND_WORK;
                }
                return false;

            case FIND_WORK:
                targetBlock = findMissingBlock(client, schWorld);
                if (targetBlock != null) {
                    BlockState expected = schWorld.getBlockState(targetBlock);
                    requiredItem = expected.getBlock().asItem();

                    if (hasItemInInventory(client, requiredItem)) {
                        state = BuilderState.MOVE_TO_BUILD;
                    } else {
                        state = BuilderState.MOVE_TO_CHEST;
                    }
                    return false;
                }
                // Completely finished building!
                return true;

            case MOVE_TO_BUILD:
                BlockState current = client.world.getBlockState(targetBlock);
                BlockState expected = schWorld.getBlockState(targetBlock);
                if (current.getBlock() == expected.getBlock()) {
                    targetBlock = null;
                    state = BuilderState.FIND_WORK;
                    AutomationEngine.resetInputs(client);
                    return false;
                }

                if (!hasItemInInventory(client, requiredItem)) {
                    state = BuilderState.MOVE_TO_CHEST;
                    return false;
                }

                double dX = targetBlock.getX() + 0.5 - client.player.getX();
                double dZ = targetBlock.getZ() + 0.5 - client.player.getZ();
                double distXZSq = dX * dX + dZ * dZ;

                // Scaffolding Logic: if target is more than 2 blocks above feet, and we are
                // directly under/near it
                if (targetBlock.getY() > client.player.getBlockY() + 1 && distXZSq < 4.0) {
                    if (actionTimer-- <= 0) {
                        // Look down, jump, and place a scaffold
                        client.player.setPitch(90f);
                        client.options.jumpKey.setPressed(true);

                        // Try to find a junk block to scaffold
                        Item junk = findScaffoldBlock(client);
                        if (junk != null) {
                            equipItem(client, junk);
                            BlockPos belowPlayer = client.player.getBlockPos().down();
                            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                                    new net.minecraft.util.hit.BlockHitResult(
                                            new net.minecraft.util.math.Vec3d(belowPlayer.getX() + 0.5,
                                                    belowPlayer.getY() + 1, belowPlayer.getZ() + 0.5),
                                            net.minecraft.util.math.Direction.UP, belowPlayer, false));
                        }
                        actionTimer = 10;
                    } else {
                        client.options.jumpKey.setPressed(false);
                    }
                    return false;
                } else {
                    client.options.jumpKey.setPressed(false);
                }

                if (distXZSq > 16.0) { // Further than 4 blocks horizontally
                    moveTo(client, targetBlock, 3.5);
                } else {
                    AutomationEngine.resetInputs(client);
                    equipItem(client, requiredItem);
                    lookAt(client, targetBlock);

                    if (actionTimer-- <= 0) {
                        // Place block by interacting with an adjacent block
                        BlockPos adjacent = findAdjacentSolidBlock(client, targetBlock);
                        if (adjacent != null) {
                            net.minecraft.util.math.Direction face = getDirectionTo(adjacent, targetBlock);
                            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                                    new net.minecraft.util.hit.BlockHitResult(
                                            new net.minecraft.util.math.Vec3d(adjacent.getX() + 0.5,
                                                    adjacent.getY() + 0.5, adjacent.getZ() + 0.5),
                                            face, adjacent, false));
                        } else {
                            // Fallback, click the target directly (might work for floating blocks with some
                            // mods)
                            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                                    new net.minecraft.util.hit.BlockHitResult(
                                            new net.minecraft.util.math.Vec3d(targetBlock.getX(), targetBlock.getY(),
                                                    targetBlock.getZ()),
                                            net.minecraft.util.math.Direction.UP, targetBlock, false));
                        }
                        actionTimer = 5; // wait a bit before retrying
                    }
                }
                return false;
        }
        return false;
    }

    private static boolean hasAnyBlock(MinecraftClient client) {
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            if (client.player.getInventory().getStack(i).getItem() instanceof net.minecraft.item.BlockItem) {
                return true;
            }
        }
        return false;
    }

    private static java.util.List<BlockPos> findAllChestsInZone(MinecraftClient client,
            ChestAreaManager.ChestZone zone) {
        int minX = Math.min(zone.x1, zone.x2);
        int minY = Math.min(zone.y1, zone.y2);
        int minZ = Math.min(zone.z1, zone.z2);
        int maxX = Math.max(zone.x1, zone.x2);
        int maxY = Math.max(zone.y1, zone.y2);
        int maxZ = Math.max(zone.z1, zone.z2);

        java.util.List<BlockPos> result = new java.util.ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    net.minecraft.block.Block b = client.world.getBlockState(p).getBlock();
                    if (b == net.minecraft.block.Blocks.CHEST || b == net.minecraft.block.Blocks.TRAPPED_CHEST
                            || b == net.minecraft.block.Blocks.BARREL) {
                        result.add(p);
                    }
                }
            }
        }
        return result;
    }

    private static BlockPos findMissingBlock(MinecraftClient client, WorldSchematic schWorld) {
        int r = 10;
        BlockPos playerPos = client.player.getBlockPos();

        fi.dy.masa.litematica.selection.Box schematicBox = null;
        try {
            for (fi.dy.masa.litematica.schematic.placement.SchematicPlacement p : fi.dy.masa.litematica.data.DataManager
                    .getSchematicPlacementManager().getAllSchematicsPlacements()) {
                if (p.getEclosingBox() != null) {
                    schematicBox = p.getEclosingBox();
                    break;
                }
            }
        } catch (Exception e) {
        }

        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = playerPos.add(x, y, z);

                    if (schematicBox != null) {
                        BlockPos min = schematicBox.getPos1();
                        BlockPos max = schematicBox.getPos2();
                        int minX = Math.min(min.getX(), max.getX());
                        int minY = Math.min(min.getY(), max.getY());
                        int minZ = Math.min(min.getZ(), max.getZ());
                        int maxX = Math.max(min.getX(), max.getX());
                        int maxY = Math.max(min.getY(), max.getY());
                        int maxZ = Math.max(min.getZ(), max.getZ());

                        if (p.getX() < minX || p.getX() > maxX || p.getY() < minY || p.getY() > maxY || p.getZ() < minZ
                                || p.getZ() > maxZ) {
                            continue; // Skip blocks outside the schematic bounds
                        }
                    }

                    BlockState expected = schWorld.getBlockState(p);
                    if (expected == null || expected.isAir())
                        continue;

                    BlockState actual = client.world.getBlockState(p);

                    if (expected.getBlock() != actual.getBlock()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasItemInInventory(MinecraftClient client, Item item) {
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) {
                return true;
            }
        }
        return false;
    }

    private static void equipItem(MinecraftClient client, Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) {
                client.player.getInventory().selectedSlot = i;
                return;
            }
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

    private static BlockPos findAdjacentSolidBlock(MinecraftClient client, BlockPos target) {
        net.minecraft.util.math.Direction[] dirs = net.minecraft.util.math.Direction.values();
        for (net.minecraft.util.math.Direction dir : dirs) {
            BlockPos adj = target.offset(dir);
            if (!client.world.getBlockState(adj).isAir() && client.world.getBlockState(adj).getFluidState().isEmpty()) {
                return adj;
            }
        }
        return null; // Floating block scenario
    }

    private static net.minecraft.util.math.Direction getDirectionTo(BlockPos from, BlockPos to) {
        if (to.getY() > from.getY())
            return net.minecraft.util.math.Direction.UP;
        if (to.getY() < from.getY())
            return net.minecraft.util.math.Direction.DOWN;
        if (to.getX() > from.getX())
            return net.minecraft.util.math.Direction.EAST;
        if (to.getX() < from.getX())
            return net.minecraft.util.math.Direction.WEST;
        if (to.getZ() > from.getZ())
            return net.minecraft.util.math.Direction.SOUTH;
        if (to.getZ() < from.getZ())
            return net.minecraft.util.math.Direction.NORTH;
        return net.minecraft.util.math.Direction.UP;
    }

    private static Item findScaffoldBlock(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            String name = stack.getItem().toString();
            if (name.contains("scaffold") || name.contains("dirt") || name.contains("cobblestone")
                    || name.contains("stone")) {
                return stack.getItem();
            }
        }
        // Fallback: use whatever block we have that isn't the required item
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof net.minecraft.item.BlockItem && stack.getItem() != requiredItem) {
                return stack.getItem();
            }
        }
        return null;
    }
}
