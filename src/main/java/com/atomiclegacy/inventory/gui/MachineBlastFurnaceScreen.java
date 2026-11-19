package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.tileentity.MachineBlastFurnaceBlockEntity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.MachineBlastFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachineBlastFurnaceScreen extends AbstractContainerScreen<MachineBlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_blast_furnace_1710.png");

    public MachineBlastFurnaceScreen(MachineBlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // [parity g1: GUIBlastFurnace.drawScreen]
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.index == MachineBlastFurnaceBlockEntity.FUEL_SLOT
                && !this.hoveredSlot.hasItem()) {
            List<Component> bonuses = MachineBlastFurnaceBlockEntity.BURN_MODULE.heatDesc();
            if (!bonuses.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, bonuses, mouseX, mouseY);
            }
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 62, 18, 18)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Speed: " + this.menu.speedPercent() + "%")), mouseX, mouseY);
        }
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 25, this.topPos + 71, 18, 18,
                this.menu.airblastFluid(), this.menu.airblastAmount(), this.menu.airblastCapacity());
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 25, this.topPos + 17, 18, 18,
                this.menu.flueFluid(), this.menu.flueAmount(), this.menu.flueCapacity());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Keep the legacy order: progress is behind the fuel column, then the fuel column is drawn over it.
        int fuelPixels = this.menu.fuelScaled(26);
        int progressPixels = this.menu.progressScaled(Math.max(0, 88 - fuelPixels));
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 106 - progressPixels - fuelPixels, 176, 102 - progressPixels - fuelPixels, 56, progressPixels);
        }
        if (fuelPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 106 - fuelPixels, 176, 128 - fuelPixels, 56, fuelPixels);
        }

        if (this.menu.isProcessing()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 81, this.topPos + 64, 176, 0, 14, 14);
        }

        // [parity K3] GUIElements.drawSmoothGauge (Vec3#rotateAroundZ direction; the old helper turned the needle mirrored)
        K3LegacyGauge.draw(guiGraphics, this.leftPos + 34, this.topPos + 80,
                (double) this.menu.airblastAmount() / Math.max(1, this.menu.airblastCapacity()), 5, 2, 1, 0x800000);
        K3LegacyGauge.draw(guiGraphics, this.leftPos + 34, this.topPos + 26,
                (double) this.menu.flueAmount() / Math.max(1, this.menu.flueCapacity()), 5, 2, 1, 0x800000);
    }
}
