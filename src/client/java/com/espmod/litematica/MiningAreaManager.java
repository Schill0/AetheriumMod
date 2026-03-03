package com.espmod.litematica;

import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MiningAreaManager {
    public static BlockPos posA = null;
    public static BlockPos posB = null;

    public static class MiningZone {
        public int x1, y1, z1;
        public int x2, y2, z2;

        public MiningZone(BlockPos p1, BlockPos p2) {
            x1 = p1.getX();
            y1 = p1.getY();
            z1 = p1.getZ();
            x2 = p2.getX();
            y2 = p2.getY();
            z2 = p2.getZ();
        }
    }

    public static MiningZone activeZone = null;

    public static void setPosA(BlockPos pos, MinecraftClient client) {
        posA = pos;
        client.player.sendMessage(Text.literal("§a[Mining Area] Corner A set to: " + pos.toShortString()), false);
        checkAndCreateZone(client);
    }

    public static void setPosB(BlockPos pos, MinecraftClient client) {
        posB = pos;
        client.player.sendMessage(Text.literal("§a[Mining Area] Corner B set to: " + pos.toShortString()), false);
        checkAndCreateZone(client);
    }

    private static void checkAndCreateZone(MinecraftClient client) {
        if (posA != null && posB != null) {
            activeZone = new MiningZone(posA, posB);
            client.player.sendMessage(
                    Text.literal("§a[Mining Area] Zone created! Miner will only mine blocks within these coordinates."),
                    false);
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            File f = new File("config/espmod_mining_area.json");
            f.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(f);
            new GsonBuilder().setPrettyPrinting().create().toJson(activeZone, fw);
            fw.close();
        } catch (Exception e) {
        }
    }

    public static void loadConfig() {
        try {
            File f = new File("config/espmod_mining_area.json");
            if (f.exists()) {
                FileReader fr = new FileReader(f);
                activeZone = new Gson().fromJson(fr, MiningZone.class);
                fr.close();
            }
        } catch (Exception e) {
        }
    }
}
