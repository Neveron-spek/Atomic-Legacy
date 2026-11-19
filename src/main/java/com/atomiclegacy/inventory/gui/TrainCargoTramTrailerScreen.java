package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.train.TrainMenus;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity S1: trains] TrainCargoTramTrailer.GUITrainCargoTramTrailer (gui_cargo_tram_trailer.png, 176 x 222). */
public class TrainCargoTramTrailerScreen extends AbstractContainerScreen<TrainMenus.CargoTramTrailer> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/vehicles/gui_cargo_tram_trailer.png");

    public TrainCargoTramTrailerScreen(TrainMenus.CargoTramTrailer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
