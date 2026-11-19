package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.items.LegacyHolotapeImageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Terminal-style direct port of GUIScreenHolotape. */
public final class HolotapeImageScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private final LegacyHolotapeImageItem.Tape tape;
    private int left;
    private int top;

    private HolotapeImageScreen(ItemStack stack) {
        super(Component.literal("Holotape"));
        this.tape = LegacyHolotapeImageItem.tape(stack);
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new HolotapeImageScreen(stack));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        // [parity g2] initGui plays hbm:block.bobble
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                com.atomiclegacy.registry.HbmSoundEvents.BOBBLE.get(), 1.0F));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xCC003300);
        int y = top + 30;
        double exactLeft = (width - (double) WIDTH) / 2.0D;
        for (String line : LegacyTextWrap.autoBreak(font, tape.text(), 275)) { // [parity g2] I18nUtil.autoBreak
            graphics.drawString(font, line, (int) (exactLeft + WIDTH / 2.0D - font.width(line) / 2), y, 0xFF009900, true);
            y += 10;
        }
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
