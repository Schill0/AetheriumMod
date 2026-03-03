package com.espmod.mixin.client;

import com.espmod.litematica.ChestAreaManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouseInput {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButtonHead(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) { // 1 = GLFW_PRESS
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null && client.currentScreen == null) {
                if (ChestAreaManager.activePlacementForSelection != null
                        && client.player.getMainHandStack().isOf(Items.STICK)) {
                    if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
                        if (button == 0) { // Left Click
                            ChestAreaManager.handleRawClick(hit.getBlockPos(), true);
                            ci.cancel();
                        } else if (button == 1) { // Right Click
                            ChestAreaManager.handleRawClick(hit.getBlockPos(), false);
                            ci.cancel();
                        }
                    }
                }
            }
        }
    }
}
