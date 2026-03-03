package com.espmod.mixin.litematica;

import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import com.espmod.litematica.ChestAreaManager;

@Mixin(GuiPlacementConfiguration.class)
public abstract class MixinGuiPlacementConfiguration {

    @Shadow(remap = false)
    public SchematicPlacement placement;

    // Removed incorrect addWidget Shadow

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    public void onInitGui(CallbackInfo ci) {
        ChestAreaManager.currentPlacement = this.placement;
        GuiPlacementConfiguration sys = (GuiPlacementConfiguration) (Object) this;

        int x = 12; // Far Left
        int y = sys.height - 40; // Bottom left corner above the taskbar

        ButtonGeneric chestBtn = new ButtonGeneric(x, y, 120, 20, "Select Chest Area");

        // Cast to Litematica's GuiBase where addButton is actually declared as public
        // and hooks the interact system
        ((fi.dy.masa.malilib.gui.GuiBase) (Object) this).addButton(chestBtn, new IButtonActionListener() {
            @Override
            public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
                if (mouseButton == 0) {
                    // Left click
                    ChestAreaManager.startChestSelection(ChestAreaManager.currentPlacement.getName());
                    MinecraftClient.getInstance().setScreen(null);
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal(
                                "§e[Litematica] §fLeft/Right click with a §6Stick§f to set the 2 corners of the Chest Zone for schematic: §a"
                                        + ChestAreaManager.currentPlacement.getName()),
                                false);
                    }
                }
            }
        });
    }
}
