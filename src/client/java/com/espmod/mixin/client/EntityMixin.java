package com.espmod.mixin.client;

import com.espmod.utils.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (com.espmod.utils.Freecam.active && (Object) this == MinecraftClient.getInstance().player) {
            float f = (float) cursorDeltaY * 0.15F;
            float g = (float) cursorDeltaX * 0.15F;
            com.espmod.utils.Freecam.pitch += f;
            com.espmod.utils.Freecam.yaw += g;

            if (com.espmod.utils.Freecam.pitch < -90.0F)
                com.espmod.utils.Freecam.pitch = -90.0F;
            if (com.espmod.utils.Freecam.pitch > 90.0F)
                com.espmod.utils.Freecam.pitch = 90.0F;

            ci.cancel();
        } else if (Freelook.active && (Object) this == MinecraftClient.getInstance().player) {
            float f = (float) cursorDeltaY * 0.15F;
            float g = (float) cursorDeltaX * 0.15F;
            Freelook.cameraPitch += f;
            Freelook.cameraYaw += g;

            if (Freelook.cameraPitch < -90.0F)
                Freelook.cameraPitch = -90.0F;
            if (Freelook.cameraPitch > 90.0F)
                Freelook.cameraPitch = 90.0F;

            ci.cancel();
        }
    }
}
