package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.CatalyticReformerMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CatalyticReformerScreen extends AbstractContainerScreen<CatalyticReformerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_catalytic_reformer.png");
    private static final int TANK_BOTTOM_Y = 70;
    private static final int TANK_TOP_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    public CatalyticReformerScreen(CatalyticReformerMenu menu, Inventory playerInventory, Component title) {
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

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 17, this.topPos + TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }

        int[] tankX = {35, 107, 125, 143};
        for (int index = 0; index < tankX.length; index++) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + tankX[index], this.topPos + TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT)) {
                HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.tankFluid(index), this.menu.tankAmount(index), this.menu.tankCapacity(index)), this.menu.tankFluid(index), mouseX, mouseY);
            }
        }

        // [parity e2] drawStackText hint over the empty catalytic converter slot
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot == this.menu.getSlot(10) && !this.hoveredSlot.hasItem()) {
            LegacyTurretScreenCompat.renderStackHint(guiGraphics, this.font, mouseX, mouseY,
                    List.of(new net.minecraft.world.item.ItemStack(com.atomiclegacy.registry.HbmItems.CATALYTIC_CONVERTER.get())));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // [parity e2] GUIMachineCatalyticReformer: power * 54 / maxPower over a 52 px gauge (literal)
        int powerPixels = (int) ((long) this.menu.power() * 54 / this.menu.maxPower());
        guiGraphics.blit(TEXTURE, this.leftPos + 17, this.topPos + TANK_BOTTOM_Y - powerPixels, 176, TANK_HEIGHT - powerPixels, TANK_WIDTH, powerPixels);

        drawFluid(guiGraphics, 35, TANK_BOTTOM_Y, TANK_WIDTH, this.menu.tankScaled(0, TANK_HEIGHT), this.menu.tankFluid(0));
        drawFluid(guiGraphics, 107, TANK_BOTTOM_Y, TANK_WIDTH, this.menu.tankScaled(1, TANK_HEIGHT), this.menu.tankFluid(1));
        drawFluid(guiGraphics, 125, TANK_BOTTOM_Y, TANK_WIDTH, this.menu.tankScaled(2, TANK_HEIGHT), this.menu.tankFluid(2));
        drawFluid(guiGraphics, 143, TANK_BOTTOM_Y, TANK_WIDTH, this.menu.tankScaled(3, TANK_HEIGHT), this.menu.tankFluid(3));
    }

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
}
