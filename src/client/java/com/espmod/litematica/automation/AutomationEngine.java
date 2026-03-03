package com.espmod.litematica.automation;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;

public class AutomationEngine {
    public static Role currentRole = Role.NONE;
    public static boolean isRunning = false;
    public static BlockPos chestsPos = null;

    private static int waitTimer = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutomationEngine::onTick);
    }

    public static void toggle() {
        isRunning = !isRunning;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        if (isRunning) {
            MinerLogic.reset();
            BuilderLogic.reset();
            client.player.sendMessage(Text.literal("§a[Smart Auto] Started Role: " + currentRole), false);
            waitTimer = com.espmod.config.Config.botStartWaitTicks;
            if (com.espmod.config.Config.botStartCommand != null
                    && !com.espmod.config.Config.botStartCommand.trim().isEmpty()) {
                client.player.networkHandler.sendCommand(com.espmod.config.Config.botStartCommand.replace("/", ""));
            }
        } else {
            if (client.player != null) {
                // Debug out
                try {
                    for (fi.dy.masa.litematica.schematic.placement.SchematicPlacement p : fi.dy.masa.litematica.data.DataManager
                            .getSchematicPlacementManager().getAllSchematicsPlacements()) {
                        fi.dy.masa.litematica.selection.Box box = p.getEclosingBox();
                        if (box != null) {
                            client.player.sendMessage(Text.literal("§d[DEBUG] Placement: " + p.getName() + " Origin: "
                                    + p.getOrigin().toShortString() + " Box: " + box.getPos1().toShortString() + " to "
                                    + box.getPos2().toShortString()), false);
                        }
                    }
                } catch (Exception e) {
                }
            }

            client.player.sendMessage(Text.literal("§c[Smart Auto] Stopped."), false);
            resetInputs(client);
        }
    }

    public static void setRole(Role role) {
        currentRole = role;
    }

    private static void onTick(MinecraftClient client) {
        if (!isRunning || client.player == null || client.world == null)
            return;

        if (waitTimer > 0) {
            waitTimer--;
            return;
        }

        switch (currentRole) {
            case MINER:
                if (MinerLogic.tick(client)) {
                    // Miner finished, transition to Farmer
                    client.player.sendMessage(Text.literal("§e[Smart Auto] Miner completed. Switching to Farmer..."),
                            false);
                    currentRole = Role.FARMER;
                    waitTimer = 20;
                }
                break;
            case FARMER:
                if (FarmerLogic.tick(client)) {
                    client.player.sendMessage(Text.literal("§e[Smart Auto] Farmer completed. Switching to Builder..."),
                            false);
                    currentRole = Role.BUILDER;
                    waitTimer = 20;
                }
                break;
            case BUILDER:
                if (BuilderLogic.tick(client)) {
                    client.player.sendMessage(Text.literal("§a[Smart Auto] Builder completed. Tasks Finished!"), false);
                    currentRole = Role.NONE;
                    isRunning = false;
                    resetInputs(client);
                }
                break;
            case NONE:
            default:
                break;
        }
    }

    public static void resetInputs(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
    }
}
