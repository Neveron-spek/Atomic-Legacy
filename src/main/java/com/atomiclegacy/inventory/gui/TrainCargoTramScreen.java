package com.atomiclegacy.inventory.gui;

import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.train.TrainMenus;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity S1: trains] TrainCargoTram.GUITrainCargoTram (textures/gui/vehicles/gui_cargo_tram.png, 176 x 204). */
public class TrainCargoTramScreen extends AbstractContainerScreen<TrainMenus.CargoTram> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/vehicles/gui_cargo_tram.png");

    public TrainCargoTramScreen(TrainMenus.CargoTram menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (K3GuiText.hover(mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52)) {
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(menu.power(), menu.maxPower())), mouseX, mouseY);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 140 / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int i = menu.power() * 53 / menu.maxPower();
        graphics.blit(TEXTURE, leftPos + 152, topPos + 70 - i, 176, 52 - i, 16, i);

        if (menu.power() > menu.powerConsumption()) {
            graphics.blit(TEXTURE, leftPos + 156, topPos + 4, 176, 52, 9, 12);
        }
    }
}
