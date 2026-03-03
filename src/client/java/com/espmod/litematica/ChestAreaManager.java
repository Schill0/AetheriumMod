package com.espmod.litematica;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;

public class ChestAreaManager {
    public static String activePlacementForSelection = null;
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;
    public static SchematicPlacement currentPlacement = null;

    // Map describing PlacementName -> ChestArea configuration
    public static Map<String, ChestZone> chestZones = new HashMap<>();

    public static class ChestZone {
        public int x1, y1, z1;
        public int x2, y2, z2;

        public ChestZone(BlockPos p1, BlockPos p2) {
            x1 = p1.getX();
            y1 = p1.getY();
            z1 = p1.getZ();
            x2 = p2.getX();
            y2 = p2.getY();
            z2 = p2.getZ();
        }
    }

    public static void startChestSelection(String placementName) {
        activePlacementForSelection = placementName;
        pos1 = null;
        pos2 = null;
    }

    public static void saveConfig() {
        try {
            File f = new File("config/espmod_chest_areas.json");
            f.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(f);
            new GsonBuilder().setPrettyPrinting().create().toJson(chestZones, fw);
            fw.close();
        } catch (Exception e) {
        }
    }

    public static void loadConfig() {
        try {
            File f = new File("config/espmod_chest_areas.json");
            if (f.exists()) {
                FileReader fr = new FileReader(f);
                chestZones = new Gson().fromJson(fr, new TypeToken<Map<String, ChestZone>>() {
                }.getType());
                fr.close();
            }
        } catch (Exception e) {
        }
    }

    public static void register() {
        loadConfig();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen.getClass().getName().contains("GuiPlacementConfiguration")) {
                ScreenEvents.afterRender(screen).register((screen1, drawContext, mouseX, mouseY, tickDelta) -> {
                    if (currentPlacement == null)
                        return;
                    ChestZone zone = chestZones
                            .get(currentPlacement.getName());

                    int x = 140;
                    int y = screen1.height - 34;

                    if (zone != null) {
                        String coords = "Chest Zone: " + zone.x1 + "," + zone.y1 + "," + zone.z1 + " to " + zone.x2
                                + "," + zone.y2 + "," + zone.z2;
                        drawContext.drawText(MinecraftClient.getInstance().textRenderer, coords, x, y, 0xFFAA00, true);
                    } else {
                        drawContext.drawText(MinecraftClient.getInstance().textRenderer, "No Chest Zone selected.", x,
                                y, 0xAAAAAA, true);
                    }
                });
            }
        });
    }

    public static void handleRawClick(BlockPos pos, boolean isLeftClick) {
        if (activePlacementForSelection == null)
            return;

        if (isLeftClick) {
            pos1 = pos;
            net.minecraft.client.MinecraftClient.getInstance().player
                    .sendMessage(Text.literal("§aChest Area Point 1 set to: " + pos.toShortString()), true);
        } else {
            pos2 = pos;
            net.minecraft.client.MinecraftClient.getInstance().player
                    .sendMessage(Text.literal("§aChest Area Point 2 set to: " + pos.toShortString()), true);
        }

        if (pos1 != null && pos2 != null) {
            finishSelection();
        }
    }

    private static void finishSelection() {
        chestZones.put(activePlacementForSelection, new ChestZone(pos1, pos2));
        saveConfig();
        net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                Text.literal(
                        "§6[Litematica] §aChest Zone for '" + activePlacementForSelection + "' saved successfully!"),
                false);
        activePlacementForSelection = null;
    }
}
