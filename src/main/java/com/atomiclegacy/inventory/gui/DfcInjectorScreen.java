package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.DfcInjectorMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DfcInjectorScreen extends AbstractContainerScreen<DfcInjectorMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/dfc/gui_injector.png");

    public DfcInjectorScreen(DfcInjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 44, this.topPos + 17, 16, 52,
                this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0));
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 116, this.topPos + 17, 16, 52,
                this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 44, this.topPos + 69, 16, 52, this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0));
        E2TankGui.renderTank(guiGraphics, this.leftPos + 116, this.topPos + 69, 16, 52, this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1));
    }
}
