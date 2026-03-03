package com.espmod.gui;

import com.espmod.config.Config;
import com.espmod.render.BlockEspManager;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockSelectorScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchField;
    private List<Block> allBlocks = new ArrayList<>();
    private List<Block> filteredBlocks = new ArrayList<>();

    private int currentPage = 0;
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS;

    private ButtonWidget nextBtn;
    private ButtonWidget prevBtn;

    public BlockSelectorScreen(Screen parent) {
        super(Text.literal("Select ESP Blocks"));
        this.parent = parent;

        // Cache all blocks
        Registries.BLOCK.forEach(allBlocks::add);
        filterBlocks("");
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.searchField = new TextFieldWidget(this.textRenderer, cx - 100, 30, 200, 20, Text.literal("Search..."));
        this.searchField.setMaxLength(50);
        this.searchField.setChangedListener(this::filterBlocks);
        this.addDrawableChild(this.searchField);

        this.prevBtn = ButtonWidget.builder(Text.literal("< Prev"), button -> {
            if (currentPage > 0)
                currentPage--;
        }).dimensions(cx - 100, this.height - 40, 60, 20).build();
        this.addDrawableChild(this.prevBtn);

        this.nextBtn = ButtonWidget.builder(Text.literal("Next >"), button -> {
            if ((currentPage + 1) * ITEMS_PER_PAGE < filteredBlocks.size())
                currentPage++;
        }).dimensions(cx + 40, this.height - 40, 60, 20).build();
        this.addDrawableChild(this.nextBtn);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(cx - 20, this.height - 40, 40, 20).build());
    }

    private void filterBlocks(String query) {
        String q = query.toLowerCase();
        this.filteredBlocks = allBlocks.stream()
                .filter(b -> Registries.BLOCK.getId(b).getPath().toLowerCase().contains(q))
                .collect(Collectors.toList());
        this.currentPage = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        int totalPages = (int) Math.ceil((double) filteredBlocks.size() / ITEMS_PER_PAGE);
        context.drawCenteredTextWithShadow(this.textRenderer,
                "Page " + (currentPage + 1) + " / " + Math.max(1, totalPages), this.width / 2, 55, 0xAAAAAA);

        int startX = (this.width - (COLUMNS * 24)) / 2;
        int startY = 70;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredBlocks.size());

        for (int i = startIndex; i < endIndex; i++) {
            Block block = filteredBlocks.get(i);
            Identifier id = Registries.BLOCK.getId(block);
            String blockIdStr = id.toString();

            int relIdx = i - startIndex;
            int col = relIdx % COLUMNS;
            int row = relIdx / COLUMNS;

            int bx = startX + (col * 24);
            int by = startY + (row * 24);

            boolean isSelected = Config.blockList.contains(blockIdStr);

            // Draw highlight if selected
            if (isSelected) {
                context.fill(bx - 2, by - 2, bx + 18, by + 18, 0xAA00FF00); // Green rect
            } else if (mouseX >= bx && mouseX <= bx + 16 && mouseY >= by && mouseY <= by + 16) {
                context.fill(bx - 2, by - 2, bx + 18, by + 18, 0x55FFFFFF); // Hover rect
            }

            ItemStack stack = new ItemStack(block);
            context.drawItem(stack, bx, by);

            // Draw tooltip if hovered
            if (mouseX >= bx && mouseX <= bx + 16 && mouseY >= by && mouseY <= by + 16) {
                context.drawTooltip(this.textRenderer, Text.literal(id.getPath()), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int startX = (this.width - (COLUMNS * 24)) / 2;
        int startY = 70;
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredBlocks.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relIdx = i - startIndex;
            int col = relIdx % COLUMNS;
            int row = relIdx / COLUMNS;

            int bx = startX + (col * 24);
            int by = startY + (row * 24);

            if (mouseX >= bx && mouseX <= bx + 16 && mouseY >= by && mouseY <= by + 16) {
                Block block = filteredBlocks.get(i);
                String id = Registries.BLOCK.getId(block).toString();

                if (Config.blockList.contains(id)) {
                    Config.blockList.remove(id);
                } else {
                    Config.blockList.add(id);
                }

                Config.save();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.client != null) {
                this.client.setScreen(parent);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
