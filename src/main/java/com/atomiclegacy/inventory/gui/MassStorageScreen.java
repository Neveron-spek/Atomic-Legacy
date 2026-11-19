package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.MassStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class MassStorageScreen extends AbstractContainerScreen<MassStorageMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/storage/gui_mass_storage.png");

    public MassStorageScreen(MassStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 221;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft != null && minecraft.gameMode != null) { // [parity g2] any mouse button
            if (hoverLegacy(62, 72, 14, 14, mouseX, mouseY)) {
                click();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, hasShiftDown() ? 1 : 0);
                return true;
            }
            if (hoverLegacy(80, 72, 14, 14, mouseX, mouseY)) {
                click();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int gauge = menu.capacity() <= 0 ? 0 : Math.min(88, menu.stockpile() * 88 / menu.capacity());
        if (gauge > 0) graphics.blit(TEXTURE, leftPos + 97, topPos + 105 - gauge, 176, 88 - gauge, 16, gauge);
        if (menu.output()) graphics.blit(TEXTURE, leftPos + 80, topPos + 72, 192, 0, 14, 14);
    }

    // [parity R2] GUIMassStorage draws no status text; the fill is a drawCustomInfoStat tooltip.
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int capacity = Math.max(1, menu.capacity());
        String percent = (((int) (menu.stockpile() * 1000D / (double) capacity)) / 10D) + "%";
        info(graphics, mouseX, mouseY, 96, 16, 18, 90, String.format(Locale.US, "%,d", menu.stockpile()) + " / " + String.format(Locale.US, "%,d", menu.capacity()), percent);
        info(graphics, mouseX, mouseY, 62, 72, 14, 14, "Click: Provide one", "Shift-click: Provide stack");
        info(graphics, mouseX, mouseY, 80, 72, 14, 14, "Toggle output");
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void info(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, String... lines) {
        int ax = leftPos + x;
        int ay = topPos + y;
        if (ax <= mouseX && ax + width > mouseX && ay < mouseY && ay + height >= mouseY) {
            graphics.renderComponentTooltip(font, java.util.Arrays.stream(lines).map(Component::literal).map(c -> (Component) c).toList(), mouseX, mouseY);
        }
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** [parity g2] GuiInfoContainer hover test: x <= mouse < x + w, y < mouse <= y + h. */
    private boolean hoverLegacy(int x, int y, int width, int height, double mouseX, double mouseY) {
        return K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height);
    }
}
