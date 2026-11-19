package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.blocks.SnowglobeType;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public final class SnowglobeScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private final SnowglobeType type;
    private int left;
    private int top;

    private SnowglobeScreen(SnowglobeType type) {
        super(Component.literal("Nuclear Tech Commemorative Snowglobe"));
        this.type = type;
    }

    public static void open(SnowglobeType type) {
        Minecraft.getInstance().setScreen(new SnowglobeScreen(type));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(HbmSoundEvents.BOBBLE.get(), 1.0F));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xCC003300);
        int y = top + 10;
        drawCentered(graphics, "Nuclear Tech Commemorative Snowglobe", y, 0xFF00FF00);
        y += 10;
        drawCentered(graphics, type.label(), y, 0xFF009900);
        y += 20;
        if (type.inscription() != null) {
            drawCentered(graphics, "On the bottom is the following inscription:", y, 0xFF00FF00);
            y += 10;
            for (String line : LegacyTextWrap.autoBreakWithParagraphs(font, type.inscription(), 280)) { // [parity g2]
                drawCentered(graphics, line, y, 0xFF009900);
                y += 10;
            }
        }
    }

    private void drawCentered(GuiGraphics graphics, String text, int y, int color) {
        FormattedCharSequence sequence = Component.literal(text).getVisualOrderText();
        // [parity g2] (int) (left + sizeX / 2 - width / 2) with left = (this.width - sizeX) / 2 as double
        double exactLeft = (this.width - (double) WIDTH) / 2.0D;
        graphics.drawString(font, sequence, (int) (exactLeft + WIDTH / 2.0D - font.width(sequence) / 2), y, color, true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
