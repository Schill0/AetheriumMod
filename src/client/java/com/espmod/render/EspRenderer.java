package com.espmod.render;

import com.espmod.config.Config;
import com.espmod.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class EspRenderer {

    public static void register() {
        WorldRenderEvents.LAST.register(EspRenderer::onRender);
    }

    private static void onRender(WorldRenderContext context) {
        if (!Config.enableEsp)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        Vec3d cameraPos = context.camera().getPos();
        Vec3d traceStart = Vec3d.fromPolar(context.camera().getPitch(), context.camera().getYaw()).multiply(1.0);

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player)
                continue;

            boolean shouldRender = false;
            int color = 0;

            if (entity instanceof PlayerEntity player) {
                if (Config.espPlayers) {
                    shouldRender = true;
                    String name = player.getName().getString();
                    if (Config.isFriend(name)) {
                        color = Config.getColorFriend();
                    } else if (Config.isEnemy(name)) {
                        color = Config.getColorEnemy();
                    } else {
                        color = Config.getColorNeutral();
                    }
                }
            } else if (entity instanceof HostileEntity) {
                if (Config.espMobs && Config.espHostiles) {
                    shouldRender = true;
                    color = Config.getColorEnemy();
                }
            } else if (entity instanceof MobEntity) {
                if (Config.espMobs) {
                    shouldRender = true;
                    color = 0xFFFFFF00; // Yellow
                }
            }

            if (shouldRender) {
                float tickDelta = context.tickCounter().getTickDelta(true);
                double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - cameraPos.x;
                double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - cameraPos.y;
                double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - cameraPos.z;

                Vec3d targetPos = new Vec3d(x, y + entity.getHeight() / 2, z);

                RenderUtils.drawLine(context.matrixStack(), traceStart, targetPos, color);

                Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ()).offset(x, y,
                        z);
                RenderUtils.drawBoxOutline(context.matrixStack(), box, color);
            }
        }
    }
}
