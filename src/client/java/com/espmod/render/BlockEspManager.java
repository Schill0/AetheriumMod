package com.espmod.render;

import com.espmod.config.Config;
import com.espmod.utils.RenderUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.state.property.Properties;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class BlockEspManager {
    private static final Set<BlockPos> foundBlocks = ConcurrentHashMap.newKeySet();
    private static Set<BlockPos> scanningBlocks = new HashSet<>();
    private static int currentY = 0;
    private static boolean isScanning = false;
    private static BlockPos scanCenter = null;
    private static final int BLOCKS_PER_TICK = 50000;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick(client));
        WorldRenderEvents.LAST.register(BlockEspManager::onRender);
    }

    private static void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null || !Config.enableEsp || !Config.espBlocks) {
            foundBlocks.clear();
            isScanning = false;
            return;
        }

        if (!isScanning) {
            isScanning = true;
            scanCenter = client.player.getBlockPos();
            currentY = client.world.getBottomY();
            scanningBlocks = new HashSet<>();
        }

        int blocksScannedThisTick = 0;
        int topY = client.world.getTopY();

        while (blocksScannedThisTick < BLOCKS_PER_TICK && currentY < topY) {
            for (int x = -Config.espBlockRadius; x <= Config.espBlockRadius; x++) {
                for (int z = -Config.espBlockRadius; z <= Config.espBlockRadius; z++) {
                    BlockPos pos = scanCenter.add(x, currentY - scanCenter.getY(), z);

                    if (pos.getY() < client.world.getBottomY() || pos.getY() >= client.world.getTopY())
                        continue;

                    if (pos.getSquaredDistance(scanCenter) > Config.espBlockRadius * Config.espBlockRadius)
                        continue;

                    BlockState state = client.world.getBlockState(pos);
                    boolean match = false;

                    String blockId = Registries.BLOCK.getId(state.getBlock()).toString();

                    if (Config.blockList.contains(blockId)) {
                        match = true;
                    } else if (Config.espPistons) {
                        if (state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON)
                                || state.isOf(Blocks.PISTON_HEAD) || state.isOf(Blocks.MOVING_PISTON)) {
                            if (state.contains(Properties.EXTENDED) && state.get(Properties.EXTENDED)) {
                                match = true;
                            } else if (state.isOf(Blocks.MOVING_PISTON)) {
                                match = true;
                            }
                        }
                    }

                    if (match) {
                        scanningBlocks.add(pos);
                    }
                    blocksScannedThisTick++;
                }
            }
            currentY++;
        }

        if (currentY >= topY) {
            foundBlocks.clear();
            foundBlocks.addAll(scanningBlocks);
            isScanning = false;
        }
    }

    private static void onRender(WorldRenderContext context) {
        if (!Config.enableEsp || !Config.espBlocks)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        Vec3d cameraPos = context.camera().getPos();
        Vec3d traceStart = Vec3d.fromPolar(context.camera().getPitch(), context.camera().getYaw()).multiply(1.0);

        List<BlockPos> validBlocks = new ArrayList<>();
        double radiusSq = Config.espBlockRadius * Config.espBlockRadius;
        for (BlockPos pos : foundBlocks) {
            if (pos.getSquaredDistance(client.player.getPos()) <= radiusSq) {
                validBlocks.add(pos);
            }
        }

        if (validBlocks.isEmpty()) {
            return;
        }

        int color = Config.getColorBlock();
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;
        if (a == 0)
            a = 1.0f;

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        for (BlockPos pos : validBlocks) {
            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;

            Vec3d targetPos = new Vec3d(x + 0.5, y + 0.5, z + 0.5);

            RenderUtils.addLineToBuffer(buffer, matrix, traceStart, targetPos, r, g, b, a);

            Box box = new Box(x, y, z, x + 1, y + 1, z + 1);
            RenderUtils.addBoxOutlineToBuffer(buffer, matrix, box, r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
