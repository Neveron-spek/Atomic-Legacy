package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.SatelliteDockMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


/** Direct GUI layout port of GUISatDock. */
/** [parity P2: GUI] Layout port of GUISatDock (176x186, gui_sat_dock.png). */
public final class SatelliteDockScreen extends AbstractContainerScreen<SatelliteDockMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_dock.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public SatelliteDockScreen(SatelliteDockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (K3GuiText.hover(mouseX, mouseY, leftPos - 7, topPos + 36, 16, 16)) { // [parity g2] drawCustomInfoStat
            graphics.renderComponentTooltip(font, LegacyInfoText.lines("desc.gui.satdock.desc"), leftPos + 1, topPos + 36 + 16);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(UTILITY, leftPos - 7, topPos + 36, 8, 0, 16, 16, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 115 - font.width(title) / 2, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
