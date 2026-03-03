package com.espmod.gui;

import com.espmod.config.Config;
import com.espmod.render.BlockEspManager;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.OptionDescription;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;

public class EspModConfigYacl {

        public static Screen createConfigScreen(Screen parent) {
                return YetAnotherConfigLib.createBuilder()
                                .title(Text.literal("ESP Mod & Smart Automation Settings"))

                                // Category: General Display
                                .category(ConfigCategory.createBuilder()
                                                .name(Text.literal("General"))
                                                .tooltip(Text.literal("Main module toggles"))
                                                .group(OptionGroup.createBuilder()
                                                                .name(Text.literal("Modules"))
                                                                .option(Option.<Boolean>createBuilder()
                                                                                .name(Text.literal("Master ESP"))
                                                                                .binding(false, () -> Config.enableEsp,
                                                                                                newVal -> Config.enableEsp = newVal)
                                                                                .controller(TickBoxControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Boolean>createBuilder()
                                                                                .name(Text.literal("Player ESP"))
                                                                                .binding(false, () -> Config.espPlayers,
                                                                                                newVal -> Config.espPlayers = newVal)
                                                                                .controller(TickBoxControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Boolean>createBuilder()
                                                                                .name(Text.literal("Mob ESP"))
                                                                                .binding(false, () -> Config.espMobs,
                                                                                                newVal -> Config.espMobs = newVal)
                                                                                .controller(TickBoxControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Boolean>createBuilder()
                                                                                .name(Text.literal("Hostiles Only"))
                                                                                .binding(false, () -> Config.espHostiles,
                                                                                                newVal -> Config.espHostiles = newVal)
                                                                                .controller(TickBoxControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Boolean>createBuilder()
                                                                                .name(Text.literal(
                                                                                                "Block ESP (Farmer)"))
                                                                                .binding(false, () -> Config.espBlocks,
                                                                                                newVal -> {
                                                                                                        Config.espBlocks = newVal;
                                                                                                })
                                                                                .controller(TickBoxControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Integer>createBuilder()
                                                                                .name(Text.literal(
                                                                                                "ESP Block Radius"))
                                                                                .binding(60, () -> Config.espBlockRadius,
                                                                                                newVal -> {
                                                                                                        Config.espBlockRadius = newVal;
                                                                                                })
                                                                                .controller(opt -> IntegerSliderControllerBuilder
                                                                                                .create(opt)
                                                                                                .range(16, 319)
                                                                                                .step(1))
                                                                                .build())
                                                                .build())
                                                .group(OptionGroup.createBuilder()
                                                                .name(Text.literal("Colors"))
                                                                .option(Option.<Color>createBuilder()
                                                                                .name(Text.literal("Friend Color"))
                                                                                .binding(Color.decode("#00FF00"),
                                                                                                () -> Color.decode(
                                                                                                                Config.colorFriendHex),
                                                                                                col -> {
                                                                                                        Config.colorFriendHex = String
                                                                                                                        .format("#%02x%02x%02x",
                                                                                                                                        col.getRed(),
                                                                                                                                        col.getGreen(),
                                                                                                                                        col.getBlue());
                                                                                                })
                                                                                .controller(ColorControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Color>createBuilder()
                                                                                .name(Text.literal("Enemy Color"))
                                                                                .binding(Color.decode("#FF0000"),
                                                                                                () -> Color.decode(
                                                                                                                Config.colorEnemyHex),
                                                                                                col -> {
                                                                                                        Config.colorEnemyHex = String
                                                                                                                        .format("#%02x%02x%02x",
                                                                                                                                        col.getRed(),
                                                                                                                                        col.getGreen(),
                                                                                                                                        col.getBlue());
                                                                                                })
                                                                                .controller(ColorControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Color>createBuilder()
                                                                                .name(Text.literal("Neutral Color"))
                                                                                .binding(Color.decode("#FFFFFF"),
                                                                                                () -> Color.decode(
                                                                                                                Config.colorNeutralHex),
                                                                                                col -> {
                                                                                                        Config.colorNeutralHex = String
                                                                                                                        .format("#%02x%02x%02x",
                                                                                                                                        col.getRed(),
                                                                                                                                        col.getGreen(),
                                                                                                                                        col.getBlue());
                                                                                                })
                                                                                .controller(ColorControllerBuilder::create)
                                                                                .build())
                                                                .build())
                                                .group(OptionGroup.createBuilder()
                                                                .name(Text.literal("Smart Automation Bots"))
                                                                .option(Option.<String>createBuilder()
                                                                                .name(Text.literal(
                                                                                                "Bot Startup Command"))
                                                                                .description(OptionDescription.of(Text
                                                                                                .literal("Command to run when bots start. Leave empty to disable.")))
                                                                                .binding("/home casse",
                                                                                                () -> Config.botStartCommand,
                                                                                                newVal -> Config.botStartCommand = newVal)
                                                                                .controller(StringControllerBuilder::create)
                                                                                .build())
                                                                .option(Option.<Integer>createBuilder()
                                                                                .name(Text.literal(
                                                                                                "Bot Startup Wait (Ticks)"))
                                                                                .description(OptionDescription.of(Text
                                                                                                .literal("How many ticks to wait after running the command before starting tasks (20 ticks = 1 second).")))
                                                                                .binding(60, () -> Config.botStartWaitTicks,
                                                                                                newVal -> Config.botStartWaitTicks = newVal)
                                                                                .controller(opt -> IntegerSliderControllerBuilder
                                                                                                .create(opt)
                                                                                                .range(0, 400).step(20))
                                                                                .build())
                                                                .build())
                                                .build())

                                // Category: Block ESP / Farmer Settings
                                .category(ConfigCategory.createBuilder()
                                                .name(Text.literal("Block ESP Filter"))
                                                .tooltip(Text.literal(
                                                                "Search and select blocks for the ESP and Farmer bot"))
                                                .group(ListOption.<String>createBuilder()
                                                                .name(Text.literal("Tracked Blocks"))
                                                                .description(OptionDescription
                                                                                .of(Text.literal(
                                                                                                "Blocks to highlight (e.g. minecraft:diamond_ore)")))
                                                                .binding(new ArrayList<String>(),
                                                                                () -> new ArrayList<>(Config.blockList),
                                                                                newVal -> {
                                                                                        Config.blockList = new java.util.HashSet<>(
                                                                                                        newVal);
                                                                                        Config.save();
                                                                                })
                                                                .controller(StringControllerBuilder::create)
                                                                .initial("")
                                                                .build())
                                                .build())

                                .save(Config::save)
                                .build()
                                .generateScreen(parent);
        }
}
