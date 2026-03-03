package com.espmod.mixin.client;

import com.espmod.utils.Freecam;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (Freecam.active) {
            KeyboardInput input = (KeyboardInput) (Object) this;
            Freecam.updateMovement(
                    input.movementForward,
                    input.movementSideways,
                    input.jumping,
                    input.sneaking);

            input.movementForward = 0;
            input.movementSideways = 0;
            input.jumping = false;
            input.sneaking = false;
        }
    }
}
