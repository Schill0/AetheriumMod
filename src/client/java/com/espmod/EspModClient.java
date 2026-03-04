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

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (configKey.wasPressed()) {
				client.setScreen(new EspConfigScreen(client.currentScreen));
			}

			while (freecamKey.wasPressed()) {
				Freecam.toggle();
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
	}
}
