package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.SatelliteLinkerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity P2: GUI] Layout port of GUIMachineSatLinker (176x186, gui_sat_linker.png). */
public final class SatelliteLinkerScreen extends AbstractContainerScreen<SatelliteLinkerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_linker.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public SatelliteLinkerScreen(SatelliteLinkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (hovering(mouseX, mouseY, 12, 28)) {
            graphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.satlinker.chip"),
                    this.leftPos + 20, this.topPos + 28 + 16);
        }
        if (hovering(mouseX, mouseY, 12, 28 + 16)) {
            graphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.satlinker.random"),
                    this.leftPos + 20, this.topPos + 28 + 32);
        }
    }

    private boolean hovering(int mouseX, int mouseY, int x, int y) {
        return K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, 16, 16); // [parity g2] drawCustomInfoStat
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        graphics.blit(UTILITY, this.leftPos + 12, this.topPos + 28, 8, 0, 16, 16, 256, 256);
        graphics.blit(UTILITY, this.leftPos + 12, this.topPos + 28 + 16, 24, 0, 16, 16, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }
}
