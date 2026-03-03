package com.espmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.espmod.gui.EspConfigScreen;
import com.espmod.render.ChunkScannerManager;
import com.espmod.render.ChunkAnalysisManager;
import com.espmod.utils.Freelook;
import com.espmod.utils.Freecam;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

public class EspModClient implements ClientModInitializer {
	private static KeyBinding configKey;
	private static KeyBinding freelookKey;
	private static KeyBinding freecamKey;
	private static KeyBinding toggleBotKey;
	private static KeyBinding cycleRoleKey;

	@Override
	public void onInitializeClient() {
		EspMod.LOGGER.info("[EspMod] Initializing Client...");
		try {
			com.espmod.config.Config.load();
		} catch (Exception e) {
			EspMod.LOGGER.error("[EspMod] Failed to load config!", e);
		}

		try {
			com.espmod.render.EspRenderer.register();
			com.espmod.render.BlockEspManager.register();
			ChunkScannerManager.register();
			ChunkAnalysisManager.register();
			com.espmod.render.ChunkAnalysisAiManager.register();
			com.espmod.litematica.ChestAreaManager.register();
			com.espmod.litematica.automation.AutomationEngine.register();
			com.espmod.litematica.MiningAreaManager.loadConfig();
			EspMod.LOGGER.info("[EspMod] All managers registered.");
		} catch (Throwable t) {
			EspMod.LOGGER.error("[EspMod] CRITICAL: Failed to register managers!", t);
		}

		configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.espmod.config",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_Z,
				"category.espmod.main"));

		freelookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.espmod.freelook",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_ALT,
				"category.espmod.main"));

		freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.espmod.freecam",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_C,
				"category.espmod.main"));

		toggleBotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Toggle Smart Bot",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"category.espmod.main"));

		cycleRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Cycle Smart Bot Role",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				"category.espmod.main"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (configKey.wasPressed()) {
				client.setScreen(new EspConfigScreen(client.currentScreen));
			}

			while (freecamKey.wasPressed()) {
				Freecam.toggle();
			}

			while (toggleBotKey.wasPressed()) {
				com.espmod.litematica.automation.AutomationEngine.toggle();
			}

			while (cycleRoleKey.wasPressed()) {
				com.espmod.litematica.automation.Role[] roles = com.espmod.litematica.automation.Role.values();
				int nextOrdinal = (com.espmod.litematica.automation.AutomationEngine.currentRole.ordinal() + 1)
						% roles.length;
				com.espmod.litematica.automation.Role nextRole = roles[nextOrdinal];
				com.espmod.litematica.automation.AutomationEngine.setRole(nextRole);
				if (client.player != null) {
					client.player.sendMessage(
							net.minecraft.text.Text.literal("§b[Smart Auto] Role set to: " + nextRole), true);
				}
			}

			if (freelookKey.isPressed() && !Freelook.active) {
				Freelook.active = true;
				if (client.player != null) {
					Freelook.cameraYaw = client.player.getYaw();
					Freelook.cameraPitch = client.player.getPitch();
				}
				client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
			} else if (!freelookKey.isPressed() && Freelook.active) {
				Freelook.active = false;
				client.options.setPerspective(Perspective.FIRST_PERSON);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("at")
					.then(ClientCommandManager.literal("set")
							.then(ClientCommandManager.literal("A").executes(context -> {
								MinecraftClient client = MinecraftClient.getInstance();
								if (client.player == null)
									return 0;
								net.minecraft.util.hit.HitResult hit = client.crosshairTarget;
								if (hit != null && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
									net.minecraft.util.math.BlockPos pos = ((net.minecraft.util.hit.BlockHitResult) hit)
											.getBlockPos();
									com.espmod.litematica.MiningAreaManager.setPosA(pos, client);
								} else {
									client.player.sendMessage(net.minecraft.text.Text
											.literal("§cYou must look at a block to set Corner A."), false);
								}
								return 1;
							}))
							.then(ClientCommandManager.literal("B").executes(context -> {
								MinecraftClient client = MinecraftClient.getInstance();
								if (client.player == null)
									return 0;
								net.minecraft.util.hit.HitResult hit = client.crosshairTarget;
								if (hit != null && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
									net.minecraft.util.math.BlockPos pos = ((net.minecraft.util.hit.BlockHitResult) hit)
											.getBlockPos();
									com.espmod.litematica.MiningAreaManager.setPosB(pos, client);
								} else {
									client.player.sendMessage(net.minecraft.text.Text
											.literal("§cYou must look at a block to set Corner B."), false);
								}
								return 1;
							})))
					.then(ClientCommandManager.literal("help").executes(context -> {
						MinecraftClient client = MinecraftClient.getInstance();
						if (client.player != null) {
							client.player.sendMessage(net.minecraft.text.Text.literal("§e--- Smart Auto Commands ---"),
									false);
							client.player.sendMessage(
									net.minecraft.text.Text.literal(
											"§a.at set A §foppure §a/at set A §f- Set mining corner A (look at block)"),
									false);
							client.player.sendMessage(
									net.minecraft.text.Text.literal(
											"§a.at set B §foppure §a/at set B §f- Set mining corner B (look at block)"),
									false);
						}
						return 1;
					})));
		});
	}
}
