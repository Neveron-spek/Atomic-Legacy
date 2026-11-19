package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.FrackingTowerMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** [parity e2] 1.7.10 GUIMachineOilWell (gui_well.png, 184x190), three-tank variant. */
public class FrackingTowerScreen extends AbstractContainerScreen<FrackingTowerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/machine/gui_well.png");

    public FrackingTowerScreen(FrackingTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 184;
        this.imageHeight = 190;
        this.titleLabelY = 10;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 126 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        tankInfo(guiGraphics, mouseX, mouseY, 76, 74 - 52, 16, 52, this.menu.oilFluid(), this.menu.oilAmount(), this.menu.oilCapacity());
        tankInfo(guiGraphics, mouseX, mouseY, 112, 74 - 52, 16, 52, this.menu.gasFluid(), this.menu.gasAmount(), this.menu.gasCapacity());
        tankInfo(guiGraphics, mouseX, mouseY, 54, 45, 6, 32, this.menu.fracksolFluid(), this.menu.fracksolAmount(), this.menu.fracksolCapacity());
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 160, this.topPos + 21, 8, 8)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("desc.gui.upgrade"),
                    Component.translatable("desc.gui.upgrade.speed"),
                    Component.translatable("desc.gui.upgrade.power"),
                    Component.translatable("desc.gui.upgrade.afterburner"),
                    Component.translatable("desc.gui.upgrade.overdrive")
            ), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 22, 16, 34)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int i = (int) ((long) this.menu.power() * 34 / this.menu.maxPower());
        guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 56 - i, 184, 34 - i, 16, i);

        int k = this.menu.indicator();
        if (k != 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 50, this.topPos + 19, 184 + (k - 1) * 14, 34, 14, 14);
        }

        drawFluid(guiGraphics, 76, 74, 16, 52, this.menu.oilFluid(), this.menu.oilAmount(), this.menu.oilCapacity());
        drawFluid(guiGraphics, 112, 74, 16, 52, this.menu.gasFluid(), this.menu.gasAmount(), this.menu.gasCapacity());
        drawFluid(guiGraphics, 54, 77, 6, 32, this.menu.fracksolFluid(), this.menu.fracksolAmount(), this.menu.fracksolCapacity());
        K3GuiText.drawSmallBlueInfo(guiGraphics, this.leftPos + 160, this.topPos + 21);
    }

    /** FluidTank.renderTankInfo */
    private void tankInfo(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height,
                          HbmFluidDefinition fluid, int amount, int capacity) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height)) {
            HbmFluidTooltipRenderer.render(graphics, this.font, HbmFluidTooltip.forTank(fluid, amount, capacity), fluid, mouseX, mouseY);
        }
    }

    /** FluidTank.renderTank, orientation 0: one quad, texture anchored to the bottom edge and repeated. */
    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int fullHeight, HbmFluidDefinition fluid, int amount, int capacity) {
        int height = (int) ((long) amount * fullHeight / capacity);
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        graphics.blit(texture, this.leftPos + x, this.topPos + bottomY - height, 0.0F, 16.0F - height, width, height, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
