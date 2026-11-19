package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.CrystallizerMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_crystallizer_alt.png");

    public CrystallizerScreen(CrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2; // [parity e2] GUICrystallizer: 70 - width / 2
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity e2] GUICrystallizer#drawScreen: energy, tank and the upgrade info (drawn at a fixed position); no progress tooltip
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(),
                    com.atomiclegacy.tileentity.CrystallizerBlockEntity.MAX_POWER)), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 35, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.tankFluid(),
                    this.menu.tankAmount(),
                    this.menu.tankCapacity(),
                    0
            ), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 117, this.topPos + 22, 8, 8)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("desc.gui.upgrade"),
                    Component.translatable("desc.gui.upgrade.speed"),
                    Component.translatable("desc.gui.upgrade.effectiveness"),
                    Component.translatable("desc.gui.upgrade.overdrive")
            ), this.leftPos + 200, this.topPos + 45);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energyPixels = this.menu.energyScaled(52);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 152,
                    this.topPos + 70 - energyPixels,
                    176,
                    64 - energyPixels,
                    16,
                    energyPixels
            );
        }

        int progressPixels = this.menu.progressScaled(28);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, progressPixels, 12);
        }

        K3GuiText.drawSmallBlueInfo(guiGraphics, this.leftPos + 117, this.topPos + 22); // [parity e2] drawInfoPanel(.., 8)
        // [parity e2] FluidTank#renderTank
        E2TankGui.renderTank(guiGraphics, this.leftPos + 35, this.topPos + 70, 16, 52,
                this.menu.tankFluid(), this.menu.tankAmount(), this.menu.tankCapacity());
    }
}
