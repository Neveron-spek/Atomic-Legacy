package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.RefineryMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class RefineryScreen extends AbstractContainerScreen<RefineryMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_refinery.png");
    // [parity L3: refinery GUI] GUIMachineRefinery: 182x240, 256x256 texture (processing/gui_refinery.png)
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public RefineryScreen(RefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 182;
        this.imageHeight = 240;
        this.inventoryLabelX = 11;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - 36 / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity e2] FluidTank#renderTankInfo / drawElectricityInfo of GUIMachineRefinery
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 12, this.topPos + 17, 16, 70,
                this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity());
        for (int index = 0; index < 4; index++) {
            E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 64 + index * 18, this.topPos + 35, 16, 52,
                    this.menu.outputFluid(index), this.menu.outputAmount(index), this.menu.outputCapacity(index));
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 158, this.topPos + 18, 16, 88)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerPixels = (int) ((long) this.menu.power() * 88L / this.menu.maxPower());
        if (powerPixels > 0) {
            blit(guiGraphics, this.leftPos + 158, this.topPos + 106 - powerPixels, 182, 88 - powerPixels, 16, powerPixels);
        }

        if (this.menu.inputAmount() != 0) {
            E2TankGui.renderTank(guiGraphics, this.leftPos + 12, this.topPos + 88, 16, 70,
                    this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity());
        }
        drawRecipePipes(guiGraphics);
        for (int index = 0; index < 4; index++) {
            E2TankGui.renderTank(guiGraphics, this.leftPos + 64 + index * 18, this.topPos + 88, 16, 52,
                    this.menu.outputFluid(index), this.menu.outputAmount(index), this.menu.outputCapacity(index));
        }
    }

    private void drawRecipePipes(GuiGraphics guiGraphics) {
        // Untinted when the input has no refinery recipe; the outputs are NONE in that case.
        HbmFluidDefinition heavy = this.menu.outputFluid(0);
        HbmFluidDefinition naphtha = this.menu.outputFluid(1);
        HbmFluidDefinition light = this.menu.outputFluid(2);
        HbmFluidDefinition gas = this.menu.outputFluid(3);

        drawTinted(guiGraphics, heavy, 30, 30, 0, 248, 43, 4);
        drawTinted(guiGraphics, naphtha, 30, 26, 0, 240, 61, 8);
        drawTinted(guiGraphics, light, 30, 22, 61, 240, 79, 12);
        drawTinted(guiGraphics, gas, 30, 18, 140, 240, 97, 16);
    }

    private void drawTinted(GuiGraphics guiGraphics, HbmFluidDefinition fluid, int x, int y, int u, int v, int width, int height) {
        if (fluid.isNone()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
            float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
            float blue = (fluid.color() & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, 1.0F);
        }
        blit(guiGraphics, this.leftPos + x, this.topPos + y, u, v, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
    }
}
