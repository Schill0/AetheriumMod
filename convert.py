import os
import re

replacements = {
    r'\bMatrixStack\b': 'PoseStack',
    r'net\.minecraft\.client\.util\.math\.PoseStack': 'com.mojang.blaze3d.vertex.PoseStack',
    
    r'\bVec3d\b': 'Vec3',
    r'net\.minecraft\.util\.math\.Vec3': 'net.minecraft.world.phys.Vec3',
    
    r'\bBox\b': 'AABB',
    r'net\.minecraft\.util\.math\.AABB': 'net.minecraft.world.phys.AABB',
    
    r'\bMathHelper\b': 'Mth',
    r'net\.minecraft\.util\.math\.Mth': 'net.minecraft.util.Mth',
    
    r'\bTessellator\b': 'Tesselator',
    r'net\.minecraft\.client\.render\.Tesselator': 'com.mojang.blaze3d.vertex.Tesselator',
    
    r'net\.minecraft\.client\.render\.BufferBuilder': 'com.mojang.blaze3d.vertex.BufferBuilder',
    r'net\.minecraft\.client\.render\.VertexFormat': 'com.mojang.blaze3d.vertex.VertexFormat',
    r'net\.minecraft\.client\.render\.VertexFormats': 'com.mojang.blaze3d.vertex.DefaultVertexFormat',
    r'\bVertexFormats\b': 'DefaultVertexFormat',
    r'VertexFormat\.DrawMode\.DEBUG_LINES': 'VertexFormat.Mode.DEBUG_LINES',
    
    r'net\.minecraft\.client\.render\.GameRenderer': 'net.minecraft.client.renderer.GameRenderer',
    r'GameRenderer::getPositionColorProgram': 'GameRenderer::getPositionColorShader',
    
    r'BufferRenderer\.drawWithGlobalProgram': 'BufferUploader.drawWithShader',
    
    r'\bMinecraftClient\b': 'Minecraft',
    r'net\.minecraft\.client\.Minecraft': 'net.minecraft.client.Minecraft',
    
    r'net\.minecraft\.entity\.Entity': 'net.minecraft.world.entity.Entity',
    r'\bPlayerEntity\b': 'Player',
    r'net\.minecraft\.entity\.player\.Player': 'net.minecraft.world.entity.player.Player',
    r'\bHostileEntity\b': 'Enemy',
    r'net\.minecraft\.entity\.mob\.Enemy': 'net.minecraft.world.entity.monster.Enemy',
    r'\bMobEntity\b': 'Mob',
    r'net\.minecraft\.entity\.mob\.Mob': 'net.minecraft.world.entity.Mob',
    
    r'\bclient\.world\b': 'client.level',
    r'getEntities\(\)': 'entitiesForRendering()',
    r'\bgetPos\(\)': 'position()',
    r'lastRenderX': 'xo',
    r'lastRenderY': 'yo',
    r'lastRenderZ': 'zo',
    r'\bgetHeight\(\)': 'getBbHeight()',
    r'\bgetStandingEyeHeight\(\)': 'getEyeHeight()',
    
    r'net\.minecraft\.util\.math\.BlockPos': 'net.minecraft.core.BlockPos',
    r'net\.minecraft\.block\.BlockState': 'net.minecraft.world.level.block.state.BlockState',
    r'net\.minecraft\.block\.Blocks': 'net.minecraft.world.level.block.Blocks',
    r'\.isOf\(': '.is(',
    r'\bgetBottomY\(\)': 'getMinBuildHeight()',
    r'\bgetTopY\(\)': 'getMaxBuildHeight()',
    
    r'\bDrawContext\b': 'GuiGraphics',
    r'net\.minecraft\.client\.gui\.GuiGraphics': 'net.minecraft.client.gui.GuiGraphics',
    r'net\.minecraft\.client\.gui\.screen\.Screen': 'net.minecraft.client.gui.screens.Screen',
    r'\bButtonWidget\b': 'Button',
    r'net\.minecraft\.client\.gui\.widget\.Button': 'net.minecraft.client.gui.components.Button',
    r'\bTextFieldWidget\b': 'EditBox',
    r'net\.minecraft\.client\.gui\.widget\.EditBox': 'net.minecraft.client.gui.components.EditBox',
    r'\bText\b': 'Component',
    r'net\.minecraft\.text\.Component': 'net.minecraft.network.chat.Component',
    r'Component\.literal': 'Component.literal',
    r'\baddDrawableChild\b': 'addRenderableWidget',
    r'\btextRenderer\b': 'font',
    r'\bdrawCenteredTextWithShadow\b': 'drawCenteredString',
    r'\bdimensions\b': 'bounds',
    r'\bsetMessage\b': 'setMessage',
    r'tickCounter\(\)\.getTickDelta\(true\)': 'getFrameTime()',
    r'getRenderTickCounter\(\)\.getTickDelta\(true\)': 'getTimer().getGameTimeDeltaTicks()'
}

for root, _, files in os.walk(r'C:\Users\UTN\Desktop\EspMod\src\client\java'):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            for k, v in replacements.items():
                content = re.sub(k, v, content)
            
            # Extra manual fixes
            content = content.replace('import net.minecraft.client.render.*;', 
                'import com.mojang.blaze3d.vertex.Tesselator;\nimport com.mojang.blaze3d.vertex.BufferBuilder;\nimport com.mojang.blaze3d.vertex.VertexFormat;\nimport com.mojang.blaze3d.vertex.DefaultVertexFormat;\nimport net.minecraft.client.renderer.GameRenderer;\nimport com.mojang.blaze3d.vertex.BufferUploader;')
            
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
                
print('Done!')
