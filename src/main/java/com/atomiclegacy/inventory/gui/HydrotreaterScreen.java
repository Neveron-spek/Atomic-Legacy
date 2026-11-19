package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.tileentity.HydrotreaterBlockEntity;
import com.atomiclegacy.inventory.container.HydrotreaterMenu;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class HydrotreaterScreen extends AbstractContainerScreen<HydrotreaterMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_hydrotreater.png");
    private static final int TANK_BOTTOM_Y = 70;
    private static final int TANK_TOP_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    public HydrotreaterScreen(HydrotreaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 238;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        titleLabelY = 5;
        inventoryLabelX = 8;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!renderMachineTooltip(graphics, mouseX, mouseY)) {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private boolean renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 17, this.topPos + TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT)) {
            graphics.renderComponentTooltip(font, List.of(K3GuiText.electricity(menu.power(), menu.maxPower())), mouseX, mouseY);
            return true;
        }
        int[] tankX = {35, 53, 125, 143};
        for (int index = 0; index < tankX.length; index++) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + tankX[index], this.topPos + TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT)) {
                HbmFluidTooltipRenderer.render(graphics, font,
                        HbmFluidTooltip.forTank(menu.tankFluid(index), menu.tankAmount(index), menu.tankCapacity(index)),
                        menu.tankFluid(index), mouseX, mouseY);
                return true;
            }
        }
        // [parity e2] drawStackText hint over the empty catalytic converter slot (container slot 10)
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot == menu.getSlot(10) && !hoveredSlot.hasItem()) {
            LegacyTurretScreenCompat.renderStackHint(graphics, font, mouseX, mouseY,
                    List.of(new net.minecraft.world.item.ItemStack(HbmItems.CATALYTIC_CONVERTER.get())));
            return true;
        }
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // [parity e2] GUIMachineHydrotreater: power * 54 / maxPower over a 52 px gauge (literal)
        int powerPixels = (int) ((long) menu.power() * 54 / menu.maxPower());
        graphics.blit(TEXTURE, leftPos + 17, topPos + TANK_BOTTOM_Y - powerPixels, 176, TANK_HEIGHT - powerPixels, TANK_WIDTH, powerPixels);

        drawFluid(graphics, 35, menu.tankScaled(0, TANK_HEIGHT), menu.tankFluid(0));
        drawFluid(graphics, 53, menu.tankScaled(1, TANK_HEIGHT), menu.tankFluid(1));
        drawFluid(graphics, 125, menu.tankScaled(2, TANK_HEIGHT), menu.tankFluid(2));
        drawFluid(graphics, 143, menu.tankScaled(3, TANK_HEIGHT), menu.tankFluid(3));
    }

    // [parity e2] title in 0xffffff
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    private void drawFluid(GuiGraphics graphics, int x, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (minecraft == null || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(leftPos + x, topPos + TANK_BOTTOM_Y - height,
                    leftPos + x + TANK_WIDTH, topPos + TANK_BOTTOM_Y, color);
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        // [parity e2] FluidTank#renderTank: one quad, texture anchored to the bottom edge and repeated
        graphics.blit(texture, leftPos + x, topPos + TANK_BOTTOM_Y - height, 0.0F, 16.0F - height, TANK_WIDTH, height, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
