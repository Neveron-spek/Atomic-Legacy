package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacySatelliteToolItem;
import com.atomiclegacy.network.SatelliteCoordPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** [parity P2: sat_coord GUI] Layout port of GUIScreenSatCoord (176x126, gui_sat_coord.png). */
public final class SatelliteCoordScreen extends Screen {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/satellites/gui_sat_coord.png");
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 126;

    private final Player player;
    private int guiLeft;
    private int guiTop;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    private SatelliteCoordScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    public static void open(Player player) {
        Minecraft.getInstance().setScreen(new SatelliteCoordScreen(player));
    }

    private ItemStack device() {
        return this.player.getMainHandItem();
    }

    private boolean connected() {
        ItemStack device = device();
        return device.getItem() instanceof LegacySatelliteToolItem && LegacySatelliteToolItem.isConnected(device);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - X_SIZE) / 2;
        this.guiTop = (this.height - Y_SIZE) / 2;
        this.xField = field(this.guiLeft + 66, this.guiTop + 21);
        this.yField = field(this.guiLeft + 66, this.guiTop + 56);
        this.zField = field(this.guiLeft + 66, this.guiTop + 92);
    }

    private EditBox field(int x, int y) {
        EditBox box = new EditBox(this.font, x, y, 48, 12, Component.empty());
        box.setTextColor(-1);
        box.setTextColorUneditable(-1);
        box.setBordered(false);
        box.setMaxLength(7);
        return box;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!connected()) return false;
        boolean handled = false;
        for (EditBox box : new EditBox[] {this.xField, this.yField, this.zField}) {
            boolean inside = box.isMouseOver(mouseX, mouseY);
            box.setFocused(inside);
            if (inside) {
                box.mouseClicked(mouseX, mouseY, button);
                handled = true;
            }
        }
        if (mouseX >= this.guiLeft + 133 && mouseX < this.guiLeft + 133 + 18
                && mouseY >= this.guiTop + 52 && mouseY < this.guiTop + 52 + 18) {
            if (isNumber(this.xField.getValue()) && isNumber(this.zField.getValue())) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                boolean hasY = isNumber(this.yField.getValue());
                PacketDistributor.sendToServer(new SatelliteCoordPayload(
                        (int) Double.parseDouble(this.xField.getValue()),
                        hasY,
                        hasY ? (int) Double.parseDouble(this.yField.getValue()) : -1,
                        (int) Double.parseDouble(this.zField.getValue())));
                this.onClose();
                return true;
            }
        }
        return handled;
    }

    /** Close enough to NumberUtils.isNumber for decimal input; values that would not parse are rejected. */
    private static boolean isNumber(String text) {
        if (text == null || text.isEmpty() || text.contains("N") || text.contains("I")
                || Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(text.length() - 1))) {
            return false;
        }
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : new EditBox[] {this.xField, this.yField, this.zField}) {
            if (box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : new EditBox[] {this.xField, this.yField, this.zField}) {
            if (box.isFocused() && box.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, X_SIZE, Y_SIZE);
        if (this.xField.isFocused()) graphics.blit(TEXTURE, this.guiLeft + 61, this.guiTop + 16, 0, 126, 54, 18);
        if (this.yField.isFocused()) graphics.blit(TEXTURE, this.guiLeft + 61, this.guiTop + 52, 0, 126, 54, 18);
        if (this.zField.isFocused()) graphics.blit(TEXTURE, this.guiLeft + 61, this.guiTop + 88, 0, 126, 54, 18);
        if (connected()) {
            graphics.blit(TEXTURE, this.guiLeft + 120, this.guiTop + 17, 194, 0, 7, 7);
            graphics.blit(TEXTURE, this.guiLeft + 120, this.guiTop + 25, 194, 0, 7, 7);
        }
        this.xField.render(graphics, mouseX, mouseY, partialTick);
        this.yField.render(graphics, mouseX, mouseY, partialTick);
        this.zField.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
