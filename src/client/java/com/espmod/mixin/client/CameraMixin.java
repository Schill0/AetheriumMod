package com.espmod.mixin.client;

import com.espmod.utils.Freecam;
import com.espmod.utils.Freelook;
import net.minecraft.client.render.Camera;
import net.minecraft.world.BlockView;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void adjustCameraRotation(Args args) {
        if (Freecam.active) {
            args.set(0, Freecam.yaw);
            args.set(1, Freecam.pitch);
        } else if (Freelook.active) {
            args.set(0, Freelook.cameraYaw);
            args.set(1, Freelook.cameraPitch);
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
            float tickDelta, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (Freecam.active) {
            this.setPos(Freecam.x, Freecam.y, Freecam.z);
        }
    }
}
