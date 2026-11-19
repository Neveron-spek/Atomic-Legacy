package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.VacuumDistillMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class VacuumDistillScreen extends AbstractContainerScreen<VacuumDistillMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_vacuum_distill.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public VacuumDistillScreen(VacuumDistillMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
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

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 44, this.topPos + 18, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity()), this.menu.inputFluid(), mouseX, mouseY);
        }
        for (int index = 0; index < 4; index++) {
            int x = 80 + index * 18;
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + 18, 16, 52)) {
                HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.outputFluid(index), this.menu.outputAmount(index), this.menu.outputCapacity(index)), this.menu.outputFluid(index), mouseX, mouseY);
            }
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 26, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // [parity e2] GUIMachineVacuumDistill: power * 54 / maxPower over a 52 px gauge (literal)
        int powerPixels = (int) ((long) this.menu.power() * 54 / this.menu.maxPower());
        blit(guiGraphics, this.leftPos + 26, this.topPos + 70 - powerPixels, 176, 52 - powerPixels, 16, powerPixels);

        drawFluid(guiGraphics, 44, 70, 16, this.menu.inputScaled(52), this.menu.inputFluid());
        for (int index = 0; index < 4; index++) {
            drawFluid(guiGraphics, 80 + index * 18, 70, 16, this.menu.outputScaled(index, 52), this.menu.outputFluid(index));
        }
    }

    // [parity e2] title at y 5 in 0xffffff, inventory label 4210752
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, 5, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);

        // [parity e2] FluidTank#renderTank: one quad, texture anchored to the bottom edge and repeated
        graphics.blit(texture, this.leftPos + x, this.topPos + bottomY - height, 0.0F, 16.0F - height, width, height, 16, 16);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}

