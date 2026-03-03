package com.espmod.gui;

import com.espmod.config.Config;
import com.espmod.render.BlockEspManager;
import com.espmod.render.ChunkAnalysisAiManager;
import com.espmod.render.ChunkScannerManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class EspConfigScreen extends Screen {
    private final Screen parent;

    private int p1x, p2x, p3x, startY;

    // Scroll offsets for lists
    private int blockListScroll = 0;
    private int friendListScroll = 0;

    // Max visible items in each list
    private static final int BLOCK_LIST_MAX = 7;
    private static final int FRIEND_LIST_MAX = 11;

    private TextFieldWidget friendField;
    private TextFieldWidget cFriendField;
    private TextFieldWidget cEnemyField;
    private TextFieldWidget cNeutralField;
    private TextFieldWidget cBlockField;
    private TextFieldWidget cBotCommandField;

    public EspConfigScreen(Screen parent) {
        super(Text.literal("ESP Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int totalWidth = 470; // 170 + 10 + 140 + 10 + 140
        int startX = this.width / 2 - (totalWidth / 2);
        this.startY = this.height / 2 - 120;

        this.p1x = startX;
        this.p2x = startX + 180;
        this.p3x = startX + 330;

        // Panel 1: Colors (2x2 grid) - Positioned below toggles
        int cfY = startY + 195;
        cFriendField = new TextFieldWidget(this.textRenderer, p1x + 10, cfY, 70, 14, Text.literal(""));
        cFriendField.setMaxLength(9);
        cFriendField.setText(Config.colorFriendHex);
        this.addDrawableChild(cFriendField);

        cEnemyField = new TextFieldWidget(this.textRenderer, p1x + 90, cfY, 70, 14, Text.literal(""));
        cEnemyField.setMaxLength(9);
        cEnemyField.setText(Config.colorEnemyHex);
        this.addDrawableChild(cEnemyField);

        int cfY2 = startY + 225;
        cNeutralField = new TextFieldWidget(this.textRenderer, p1x + 10, cfY2, 70, 14, Text.literal(""));
        cNeutralField.setMaxLength(9);
        cNeutralField.setText(Config.colorNeutralHex);
        this.addDrawableChild(cNeutralField);

        cBlockField = new TextFieldWidget(this.textRenderer, p1x + 90, cfY2, 70, 14, Text.literal(""));
        cBlockField.setMaxLength(9);
        cBlockField.setText(Config.colorBlockHex);
        this.addDrawableChild(cBlockField);

        // Bot Start Command Field (Panel 3)
        int botCmdY = startY + 160;
        cBotCommandField = new TextFieldWidget(this.textRenderer, p3x + 10, botCmdY, 120, 14, Text.literal(""));
        cBotCommandField.setMaxLength(100);
        cBotCommandField.setText(Config.botStartCommand);
        this.addDrawableChild(cBotCommandField);

        // Panel 2: Friends
        friendField = new TextFieldWidget(this.textRenderer, p2x + 10, startY + 190, 120, 16, Text.literal(""));
        friendField.setMaxLength(30);
        this.addDrawableChild(friendField);

        // Panel 3: Blocks button
        // No longer a text field, now handled in `EspModConfigYacl`
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Override to remove vanilla background blur and darkness
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw custom clear background overlay instead of blur
        context.fill(0, 0, this.width, this.height, 0x44000000);

        // Draw 3 Panels
        int pHeight = 300;
        drawPanel(context, p1x, startY, 170, pHeight, "Modules & Colors");
        drawPanel(context, p2x, startY, 140, pHeight, "Friends List");
        drawPanel(context, p3x, startY, 140, pHeight, "Blocks & Bots");

        // === PANEL 1: TOGGLES ===
        int ty = startY + 30;
        drawToggle(context, "Master ESP", Config.enableEsp, p1x + 10, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "Player ESP", Config.espPlayers, p1x + 10, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "Mob ESP", Config.espMobs, p1x + 10, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "Hostiles Only", Config.espHostiles, p1x + 10, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "Block ESP", Config.espBlocks, p1x + 10, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "Chunk Scanner", Config.enableChunkScanner, p1x + 10, ty, mouseX, mouseY);
        drawSizeControl(context, Config.chunkScannerSize, p1x + 110, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "Chunk Analysis", Config.enableChunkAnalysis, p1x + 10, ty, mouseX, mouseY);
        drawSizeControl(context, Config.chunkAnalysisSize, p1x + 110, ty, mouseX, mouseY);
        ty += 20;
        drawToggle(context, "AI Analysis", Config.enableChunkAnalysisAI, p1x + 10, ty, mouseX, mouseY);
        drawSizeControl(context, Config.chunkAnalysisAISize, p1x + 110, ty, mouseX, mouseY);

        // Color Labels
        ty += 15;
        context.drawTextWithShadow(this.textRenderer, "Frnd", p1x + 12, ty, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Enmy", p1x + 92, ty, 0xFFAAAAAA);
        ty += 30;
        context.drawTextWithShadow(this.textRenderer, "Neut", p1x + 12, ty, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Blck", p1x + 92, ty, 0xFFAAAAAA);

        int saveY = startY + 250;
        drawButton(context, "Save Colors", p1x + 10, saveY, 150, 18, mouseX, mouseY, 0xFF333333);

        // === PANEL 2: FRIENDS ===
        List<String> friendsList = new ArrayList<>(Config.friends);
        int friendsVisible = Math.min(FRIEND_LIST_MAX, friendsList.size() - friendListScroll);
        for (int i = 0; i < friendsVisible; i++) {
            int idx = i + friendListScroll;
            if (idx >= friendsList.size())
                break;
            String name = friendsList.get(idx);
            if (name.length() > 16)
                name = name.substring(0, 16) + "..";
            int itemY = startY + 30 + (i * 13);
            context.drawTextWithShadow(this.textRenderer, name, p2x + 10, itemY, 0xFF55FF55);
            drawButton(context, "x", p2x + 115, itemY - 2, 14, 11, mouseX, mouseY, 0xFFFF4444);
        }
        // Scroll arrows for friends list
        if (friendListScroll > 0)
            drawButton(context, "^", p2x + 60, startY + 25, 20, 10, mouseX, mouseY, 0xFF333355);
        if (friendListScroll + FRIEND_LIST_MAX < friendsList.size())
            drawButton(context, "v", p2x + 60, startY + 170, 20, 10, mouseX, mouseY, 0xFF333355);
        drawButton(context, "Add Friend", p2x + 10, startY + 210, 120, 16, mouseX, mouseY, 0xFF333333);
        drawButton(context, "Clear Friends", p2x + 10, startY + 230, 120, 16, mouseX, mouseY, 0xFF882222);

        // === PANEL 3: BLOCKS & BOTS ===
        context.drawTextWithShadow(this.textRenderer, "Using YACL for blocks.", p3x + 10, startY + 30, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "Click below to search", p3x + 10, startY + 45, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, "and filter tracking.", p3x + 10, startY + 60, 0xFFAAAAAA);

        drawButton(context, "Open Block Selector", p3x + 10, startY + 90, 120, 20, mouseX, mouseY, 0xFF5500AA);

        // Bot Settings
        int bty = startY + 145;
        context.drawTextWithShadow(this.textRenderer, "Bot Start Command:", p3x + 10, bty, 0xFFAAAAAA);
        bty += 35; // Space for textfield (160 + 14)
        context.drawTextWithShadow(this.textRenderer, "Wait Ticks:", p3x + 10, bty + 2, 0xFFAAAAAA);
        drawSizeControl(context, Config.botStartWaitTicks, p3x + 70, bty, mouseX, mouseY);

        bty += 35;
        // Litematica button
        drawButton(context, "Smart Litematica", p3x + 10, bty, 120, 16, mouseX, mouseY, 0xFF0055AA);

        drawButton(context, "Clear Blocks", p3x + 10, startY + 280, 120, 16, mouseX, mouseY, 0xFF882222);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSizeControl(DrawContext context, int value, int x, int y, int mx, int my) {
        drawButton(context, "-", x, y, 12, 12, mx, my, 0xFF444444);
        context.drawCenteredTextWithShadow(this.textRenderer, value + "", x + 20, y + 2, 0xFFFFFF);
        drawButton(context, "+", x + 28, y, 12, 12, mx, my, 0xFF444444);
    }

    private void drawPanel(DrawContext context, int x, int y, int width, int height, String title) {
        context.fill(x, y, x + width, y + height, 0xD0101010); // Dark sleek background
        context.fill(x, y, x + width, y + 20, 0xFF2A2B2E); // Header bar
        context.drawBorder(x - 1, y - 1, width + 2, height + 2, 0xFF444444); // Subtle border
        context.drawCenteredTextWithShadow(this.textRenderer, title, x + width / 2, y + 6, 0xFF00E5FF); // Cyan header
                                                                                                        // text
    }

    private void drawToggle(DrawContext context, String text, boolean state, int x, int y, int mx, int my) {
        int color = state ? 0xFF55FF55 : 0xFFFF5555;
        context.fill(x, y, x + 12, y + 12, color);
        context.drawBorder(x, y, 12, 12, 0xFF000000);
        context.drawTextWithShadow(this.textRenderer, text, x + 20, y + 2, 0xFFDDDDDD);
    }

    private void drawButton(DrawContext context, String text, int x, int y, int w, int h, int mx, int my,
            int baseColor) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        context.fill(x, y, x + w, y + h, hovered ? 0xFF888888 : baseColor);
        context.drawBorder(x, y, w, h, 0xFF000000);
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Toggles
            int ty = startY + 30;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.enableEsp = !Config.enableEsp;
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.espPlayers = !Config.espPlayers;
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.espMobs = !Config.espMobs;
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.espHostiles = !Config.espHostiles;
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.espBlocks = !Config.espBlocks;
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.enableChunkScanner = !Config.enableChunkScanner;
                Config.save();
                return true;
            }
            if (isHovered(p1x + 110, ty, 12, 12, mouseX, mouseY)) {
                Config.chunkScannerSize = Math.max(1, Config.chunkScannerSize - 1);
                Config.save();
                return true;
            }
            if (isHovered(p1x + 138, ty, 12, 12, mouseX, mouseY)) {
                Config.chunkScannerSize = Math.min(10, Config.chunkScannerSize + 1);
                Config.save();
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.enableChunkAnalysis = !Config.enableChunkAnalysis;
                Config.save();
                return true;
            }
            if (isHovered(p1x + 110, ty, 12, 12, mouseX, mouseY)) {
                Config.chunkAnalysisSize = Math.max(1, Config.chunkAnalysisSize - 1);
                Config.save();
                return true;
            }
            if (isHovered(p1x + 138, ty, 12, 12, mouseX, mouseY)) {
                Config.chunkAnalysisSize = Math.min(10, Config.chunkAnalysisSize + 1);
                Config.save();
                return true;
            }
            ty += 20;
            if (handleToggleClick(p1x + 10, ty, mouseX, mouseY)) {
                Config.enableChunkAnalysisAI = !Config.enableChunkAnalysisAI;
                if (!Config.enableChunkAnalysisAI) {
                    ChunkAnalysisAiManager.clearResults();
                }
                Config.save();
                return true;
            }
            if (isHovered(p1x + 110, ty, 12, 12, mouseX, mouseY)) {
                Config.chunkAnalysisAISize = Math.max(1, Config.chunkAnalysisAISize - 1);
                Config.save();
                return true;
            }
            if (isHovered(p1x + 138, ty, 12, 12, mouseX, mouseY)) {
                Config.chunkAnalysisAISize = Math.min(10, Config.chunkAnalysisAISize + 1);
                Config.save();
                return true;
            }

            // Bot Wait Ticks Buttons
            int waitY = startY + 180;
            if (isHovered(p3x + 70, waitY, 12, 12, mouseX, mouseY)) {
                Config.botStartWaitTicks = Math.max(0, Config.botStartWaitTicks - 20);
                Config.save();
                return true;
            }
            if (isHovered(p3x + 98, waitY, 12, 12, mouseX, mouseY)) {
                Config.botStartWaitTicks = Math.min(400, Config.botStartWaitTicks + 20);
                Config.save();
                return true;
            }

            // Smart Litematica click
            int smY = startY + 215;
            if (isHovered(p3x + 10, smY, 120, 16, mouseX, mouseY)) {
                // Smart Litematica Logic (Moved to Menu)
                if (this.client != null && this.client.player != null) {
                    this.client.player.sendMessage(Text.literal(
                            "§eSmart Litematica features are now injected into the native Litematica GUI! Press M -> Configuration -> Placement."),
                            false);
                }
                return true;
            }

            // Save Colors
            int saveY = startY + 250;
            if (isHovered(p1x + 10, saveY, 150, 18, mouseX, mouseY)) {
                Config.colorFriendHex = cFriendField.getText();
                Config.colorEnemyHex = cEnemyField.getText();
                Config.colorNeutralHex = cNeutralField.getText();
                Config.colorBlockHex = cBlockField.getText();
                Config.botStartCommand = cBotCommandField.getText();
                Config.save();
                return true;
            }

            // Friend Add
            if (isHovered(p2x + 10, startY + 210, 120, 16, mouseX, mouseY)) {
                String f = friendField.getText().trim();
                if (!f.isEmpty() && !Config.friends.contains(f)) {
                    Config.addFriend(f);
                    Config.save();
                    friendField.setText("");
                }
                return true;
            }
            // Clear Friends
            if (isHovered(p2x + 10, startY + 230, 120, 16, mouseX, mouseY)) {
                Config.friends.clear();
                Config.save();
                return true;
            }

            // Friend Remove (scrolled)
            List<String> friendsList = new ArrayList<>(Config.friends);
            int friendsVisible2 = Math.min(FRIEND_LIST_MAX, friendsList.size() - friendListScroll);
            // Scroll arrows for friends
            if (isHovered(p2x + 60, startY + 25, 20, 10, mouseX, mouseY) && friendListScroll > 0) {
                friendListScroll--;
                return true;
            }
            if (isHovered(p2x + 60, startY + 170, 20, 10, mouseX, mouseY)
                    && friendListScroll + FRIEND_LIST_MAX < friendsList.size()) {
                friendListScroll++;
                return true;
            }
            for (int i = 0; i < friendsVisible2; i++) {
                int idx = i + friendListScroll;
                if (idx >= friendsList.size())
                    break;
                int itemY = startY + 30 + (i * 13);
                if (isHovered(p2x + 115, itemY - 2, 14, 11, mouseX, mouseY)) {
                    Config.removeFriend(friendsList.get(idx));
                    friendListScroll = Math.min(friendListScroll, Math.max(0, Config.friends.size() - FRIEND_LIST_MAX));
                    return true;
                }
            }

            // Panel 3 (Visual Block Selector)
            if (isHovered(p3x + 10, startY + 90, 120, 20, mouseX, mouseY)) {
                if (this.client != null) {
                    this.client.setScreen(new BlockSelectorScreen(this));
                }
                return true;
            }
            if (isHovered(p3x + 10, startY + 280, 120, 16, mouseX, mouseY)) {
                Config.blockList.clear();
                Config.save();
                return true;
            }

            // Radius Modifiers
            int ry = startY + 125;
            if (isHovered(p3x + 10, ry + 12, 20, 14, mouseX, mouseY)) {
                Config.espBlockRadius = Math.max(16, Config.espBlockRadius - 10);
                Config.save();
                return true;
            }
            if (isHovered(p3x + 110, ry + 12, 20, 14, mouseX, mouseY)) {
                Config.espBlockRadius = Math.min(300, Config.espBlockRadius + 10);
                Config.save();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleToggleClick(int x, int y, double mx, double my) {
        return mx >= x && mx <= x + 100 && my >= y && my <= y + 12;
    }

    private boolean isHovered(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scrollDir = verticalAmount > 0 ? -1 : 1; // scroll up = negative delta = go up in list

        // Scroll blocks list if hovering over panel 3
        boolean overBlocksPanel = mouseX >= p3x && mouseX <= p3x + 140;
        if (overBlocksPanel) {
            int newScroll = blockListScroll + scrollDir;
            int maxScroll = Math.max(0, Config.blockList.size() - BLOCK_LIST_MAX);
            blockListScroll = Math.max(0, Math.min(newScroll, maxScroll));
            return true;
        }

        // Scroll friends list if hovering over panel 2
        boolean overFriendsPanel = mouseX >= p2x && mouseX <= p2x + 140;
        if (overFriendsPanel) {
            int newScroll = friendListScroll + scrollDir;
            int maxScroll = Math.max(0, Config.friends.size() - FRIEND_LIST_MAX);
            friendListScroll = Math.max(0, Math.min(newScroll, maxScroll));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Config.save();
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }
}
