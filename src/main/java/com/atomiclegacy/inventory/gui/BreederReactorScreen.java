package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.BreederReactorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class BreederReactorScreen extends AbstractContainerScreen<BreederReactorMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_breeder.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public BreederReactorScreen(BreederReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity M2: breeder] drawCustomInfoStat draws the panel text at (guiLeft - 8, guiTop + 32); no other tooltip
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.atomiclegacy.breeder_reactor.flux.0"),
                    Component.translatable("tooltip.atomiclegacy.breeder_reactor.flux.1"),
                    Component.translatable("tooltip.atomiclegacy.breeder_reactor.flux.2")
            ), this.leftPos - 8, this.topPos + 32);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 32, 176, 0, progress, 20);
        }
        // [parity M2: breeder] GuiInfoContainer#drawInfoPanel type 3 from gui_utility.png
        guiGraphics.blit(UTILITY, this.leftPos - 16, this.topPos + 16, 24, 0, 16, 16, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        String flux = Integer.toString(this.menu.flux());
        guiGraphics.drawString(this.font, flux, 88 - this.font.width(flux) / 2, 21, 0x08FF00, false);
    }
}
