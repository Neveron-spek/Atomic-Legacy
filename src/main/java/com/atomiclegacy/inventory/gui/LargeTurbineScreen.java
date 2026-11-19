package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.LargeTurbineBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.LargeTurbineMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class LargeTurbineScreen extends AbstractContainerScreen<LargeTurbineMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/generators/gui_turbine_large.png");

    public LargeTurbineScreen(LargeTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
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

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + 17, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity()), this.menu.inputFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 134, this.topPos + 17, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity()), this.menu.outputFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 123, this.topPos + 35, 7, 34)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), LargeTurbineBlockEntity.ENERGY_CAPACITY)), mouseX, mouseY); // [parity Q3: drawElectricityInfo]
        }
        if (this.menu.outputFluid().isNone() && K3GuiText.hover(mouseX, mouseY, this.leftPos + -16, this.topPos + 68, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Error: Invalid fluid!")), this.leftPos - 8, this.topPos + 36 + 16 + 32); // [parity g1: drawCustomInfoStat tPosX/tPosY] // [parity Q3: GUIMachineLargeTurbine text]
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawSteamIcon(guiGraphics);

        int powerPixels = this.menu.powerScaled(34);
        if (powerPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 123, this.topPos + 69 - powerPixels, 176, 34 - powerPixels, 7, powerPixels);
        }
        if (this.menu.outputFluid().isNone()) {
            guiGraphics.blit(K3GuiText.UTILITY, this.leftPos - 16, this.topPos + 68, 8, 16, 16, 16, 256, 256); // [parity Q3: drawInfoPanel type 6]
        }
        // [parity Q3: FluidTank.renderTank with the fluid texture, drawn last as in the original]
        E2TankGui.renderTank(guiGraphics, this.leftPos + 62, this.topPos + 69, 16, 52, this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 134, this.topPos + 69, 16, 52, this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity());
    }

    private void drawSteamIcon(GuiGraphics graphics) {
        int v = switch (this.menu.inputFluid().name()) {
            case "steam" -> 0;
            case "hotsteam" -> 14;
            case "superhotsteam" -> 28;
            case "ultrahotsteam" -> 42;
            default -> -1;
        };
        if (v >= 0) {
            graphics.blit(TEXTURE, this.leftPos + 99, this.topPos + 18, 183, v, 14, 14);
        }
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}
