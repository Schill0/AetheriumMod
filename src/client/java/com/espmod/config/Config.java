package com.espmod.config;

import java.util.HashSet;
import java.util.Set;
import com.espmod.EspMod;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("espmod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean enableEsp = true;
    public static boolean espPlayers = true;
    public static boolean espMobs = true;
    public static boolean espHostiles = true;
    public static boolean espItems = false;
    public static boolean espBlocks = true;
    public static boolean espPistons = false; // special check for extended pistons
    public static boolean enableChunkScanner = true;
    public static boolean enableChunkAnalysis = false;
    public static boolean enableChunkAnalysisAI = false;
    public static int chunkScannerSize = 1; // 1 = 1x1 chunk, 2 = 2x2, etc.
    public static int chunkAnalysisSize = 1;
    public static int chunkAnalysisAISize = 1;
    public static int espBlockRadius = 110; // normal scanner radius

    public static String botStartCommand = "/home casse";
    public static int botStartWaitTicks = 60; // 3 seconds

    public static String colorFriendHex = "#00FF00"; // Green
    public static String colorEnemyHex = "#FF0000"; // Red
    public static String colorNeutralHex = "#FFFFFF"; // White
    public static String colorBlockHex = "#00FFFF"; // Cyan

    public static Set<String> friends = new HashSet<>();
    public static Set<String> enemies = new HashSet<>();
    public static Set<String> blockList = new HashSet<>();

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enableEsp", enableEsp);
        json.addProperty("espPlayers", espPlayers);
        json.addProperty("espMobs", espMobs);
        json.addProperty("espHostiles", espHostiles);
        json.addProperty("espItems", espItems);
        json.addProperty("espBlocks", espBlocks);
        json.addProperty("enableChunkScanner", enableChunkScanner);
        json.addProperty("enableChunkAnalysis", enableChunkAnalysis);
        json.addProperty("enableChunkAnalysisAI", enableChunkAnalysisAI);
        json.addProperty("chunkScannerSize", chunkScannerSize);
        json.addProperty("chunkAnalysisSize", chunkAnalysisSize);
        json.addProperty("chunkAnalysisAISize", chunkAnalysisAISize);
        json.addProperty("espBlockRadius", espBlockRadius);

        json.addProperty("botStartCommand", botStartCommand);
        json.addProperty("botStartWaitTicks", botStartWaitTicks);

        json.addProperty("colorFriendHex", colorFriendHex);
        json.addProperty("colorEnemyHex", colorEnemyHex);
        json.addProperty("colorNeutralHex", colorNeutralHex);
        json.addProperty("colorBlockHex", colorBlockHex);

        JsonArray friendsArray = new JsonArray();
        for (String f : friends)
            friendsArray.add(f);
        json.add("friends", friendsArray);

        JsonArray enemiesArray = new JsonArray();
        for (String e : enemies)
            enemiesArray.add(e);
        json.add("enemies", enemiesArray);

        JsonArray blocksArray = new JsonArray();
        for (String b : blockList)
            blocksArray.add(b);
        json.add("blockList", blocksArray);

        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            blockList.add("minecraft:spawner");
            blockList.add("minecraft:kelp");
            blockList.add("minecraft:kelp_plant");
            blockList.add("minecraft:dispenser");
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("enableEsp"))
                enableEsp = json.get("enableEsp").getAsBoolean();
            if (json.has("espPlayers"))
                espPlayers = json.get("espPlayers").getAsBoolean();
            if (json.has("espMobs"))
                espMobs = json.get("espMobs").getAsBoolean();
            if (json.has("espHostiles"))
                espHostiles = json.get("espHostiles").getAsBoolean();
            if (json.has("espItems"))
                espItems = json.get("espItems").getAsBoolean();
            if (json.has("espBlocks"))
                espBlocks = json.get("espBlocks").getAsBoolean();
            if (json.has("enableChunkScanner"))
                enableChunkScanner = json.get("enableChunkScanner").getAsBoolean();
            if (json.has("enableChunkAnalysis"))
                enableChunkAnalysis = json.get("enableChunkAnalysis").getAsBoolean();
            if (json.has("enableChunkAnalysisAI"))
                enableChunkAnalysisAI = json.get("enableChunkAnalysisAI").getAsBoolean();
            if (json.has("chunkScanSize")) {
                int oldSize = json.get("chunkScanSize").getAsInt();
                chunkScannerSize = oldSize;
                chunkAnalysisSize = oldSize;
                chunkAnalysisAISize = oldSize;
            }
            if (json.has("chunkScannerSize"))
                chunkScannerSize = json.get("chunkScannerSize").getAsInt();
            if (json.has("chunkAnalysisSize"))
                chunkAnalysisSize = json.get("chunkAnalysisSize").getAsInt();
            if (json.has("chunkAnalysisAISize"))
                chunkAnalysisAISize = json.get("chunkAnalysisAISize").getAsInt();
            if (json.has("espBlockRadius"))
                espBlockRadius = json.get("espBlockRadius").getAsInt();

            if (json.has("botStartCommand"))
                botStartCommand = json.get("botStartCommand").getAsString();
            if (json.has("botStartWaitTicks"))
                botStartWaitTicks = json.get("botStartWaitTicks").getAsInt();

            if (json.has("colorFriendHex"))
                colorFriendHex = json.get("colorFriendHex").getAsString();
            if (json.has("colorEnemyHex"))
                colorEnemyHex = json.get("colorEnemyHex").getAsString();
            if (json.has("colorNeutralHex"))
                colorNeutralHex = json.get("colorNeutralHex").getAsString();
            if (json.has("colorBlockHex"))
                colorBlockHex = json.get("colorBlockHex").getAsString();

            if (json.has("friends")) {
                friends.clear();
                for (JsonElement e : json.getAsJsonArray("friends"))
                    friends.add(e.getAsString());
            }
            if (json.has("enemies")) {
                enemies.clear();
                for (JsonElement e : json.getAsJsonArray("enemies"))
                    enemies.add(e.getAsString());
            }
            if (json.has("blockList")) {
                blockList.clear();
                for (JsonElement e : json.getAsJsonArray("blockList"))
                    blockList.add(e.getAsString());
            }
        } catch (Exception e) {
            EspMod.LOGGER.error("[EspMod] Exception loading config, file might be corrupted. Resetting to defaults.",
                    e);
            // Optional: delete corrupted file
            try {
                Files.deleteIfExists(CONFIG_FILE);
            } catch (Exception ignored) {
            }
        }
    }

    public static int parseHex(String hex, int defaultColor) {
        if (hex == null || hex.isEmpty())
            return defaultColor;
        if (hex.startsWith("#"))
            hex = hex.substring(1);
        try {
            int color = (int) Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                return color | 0xFF000000; // Add full alpha if missing
            }
            return color;
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }

    public static int getColorFriend() {
        return parseHex(colorFriendHex, 0xFF00FF00);
    }

    public static int getColorEnemy() {
        return parseHex(colorEnemyHex, 0xFFFF0000);
    }

    public static int getColorNeutral() {
        return parseHex(colorNeutralHex, 0xFFFFFFFF);
    }

    public static int getColorBlock() {
        return parseHex(colorBlockHex, 0xFF00FFFF);
    }

    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public static boolean isEnemy(String name) {
        return enemies.contains(name.toLowerCase());
    }

    public static void addFriend(String name) {
        String lower = name.toLowerCase();
        friends.add(lower);
        enemies.remove(lower);
        save();
    }

    public static void removeFriend(String name) {
        friends.remove(name.toLowerCase());
        save();
    }

    public static void addBlock(String id) {
        if (!id.contains(":"))
            id = "minecraft:" + id;
        blockList.add(id.toLowerCase());
        save();
    }

    public static void removeBlock(String id) {
        if (!id.contains(":"))
            id = "minecraft:" + id;
        blockList.remove(id.toLowerCase());
        save();
    }
}
