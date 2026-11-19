package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.DieselGeneratorMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** [parity L1: diesel] GUIMachineDiesel (176x203, on/off button, info panels from gui_utility). */
public class DieselGeneratorScreen extends AbstractContainerScreen<DieselGeneratorMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/generators/gui_diesel.png");

    public DieselGeneratorScreen(DieselGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;
        if (K3GuiText.hover(mouseX, mouseY, x + 35, y + 69 - 52, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.fuelFluid(), this.menu.fuelAmount(), this.menu.fuelCapacity()), this.menu.fuelFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, x + 141, y + 69 - 52, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.powerCap())), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, x - 8, y + 36, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("Fuel consumption rate:"),
                    Component.literal("  1 mB/t"),
                    Component.literal("  20 mB/s"),
                    Component.literal("(Consumption rate is constant)")
            ), x, y + 36 + 16);
        }
        if (this.menu.hePerTick() <= 0 && K3GuiText.hover(mouseX, mouseY, x - 8, y + 36 + 32, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("Error: The currently set fuel type"),
                    Component.literal("is not supported by this engine!")
            ), x, y + 36 + 16 + 32);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // GUIMachineDiesel.mouseClicked: 89 <= x < 105, 61 < y <= 75
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 89, this.topPos + 61, 16, 14)) {
            K3GuiText.click();
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.power() > 0) {
            int i = this.menu.powerScaled(52);
            guiGraphics.blit(TEXTURE, x + 141, y + 69 - i, 176, 52 - i, 16, i);
        }
        if (this.menu.isOn()) {
            guiGraphics.blit(TEXTURE, x + 79, y + 61, 192, 16, 35, 14);
        }
        if (this.menu.running()) {
            guiGraphics.blit(TEXTURE, x + 89, y + 42, 192, 0, 16, 16);
        }
        guiGraphics.blit(K3GuiText.UTILITY, x - 8, y + 36, 8, 0, 16, 16, 256, 256); // drawInfoPanel type 2
        if (this.menu.hePerTick() <= 0) {
            guiGraphics.blit(K3GuiText.UTILITY, x - 8, y + 36 + 32, 8, 16, 16, 16, 256, 256); // type 6
        }
        // [parity e2] diesel.tank.renderTank(guiLeft + 35, guiTop + 69, zLevel, 16, 52)
        E2TankGui.renderTank(guiGraphics, x + 35, y + 69, 16, 52, this.menu.fuelFluid(), this.menu.fuelAmount(), this.menu.fuelCapacity());
    }
}
