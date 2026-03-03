package com.espmod.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class Freecam {
    public static boolean active = false;
    public static double x, y, z;
    public static float yaw, pitch;
    private static final float SPEED = 1.0f;

    // Remember what perspective was set before freecam
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    public static void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        active = !active;

        if (active && client.player != null) {
            x = client.player.getX();
            y = client.player.getY() + client.player.getStandingEyeHeight();
            z = client.player.getZ();
            yaw = client.player.getYaw();
            pitch = client.player.getPitch();

            // Switch to third-person so the player body is rendered
            previousPerspective = client.options.getPerspective();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (!active) {
            // Restore original perspective
            if (client.options != null) {
                client.options.setPerspective(previousPerspective);
            }
        }
    }

    public static void updateMovement(float forward, float strafe, boolean jumping, boolean sneaking) {
        if (!active)
            return;

        float radPitch = (float) Math.toRadians(pitch);

        double moveX = 0;
        double moveY = 0;
        double moveZ = 0;

        if (forward != 0 || strafe != 0) {
            float angle = yaw;
            if (forward < 0)
                angle += 180;
            if (strafe > 0)
                angle += (forward > 0 ? -45 : forward < 0 ? 45 : -90);
            if (strafe < 0)
                angle += (forward > 0 ? 45 : forward < 0 ? -45 : 90);

            float radAngle = (float) Math.toRadians(angle);
            moveX = -MathHelper.sin(radAngle) * SPEED;
            moveZ = MathHelper.cos(radAngle) * SPEED;
            moveY = -MathHelper.sin(radPitch) * forward * SPEED;
        }

        if (jumping)
            moveY += SPEED;
        if (sneaking)
            moveY -= SPEED;

        x += moveX;
        y += moveY;
        z += moveZ;
    }
}
