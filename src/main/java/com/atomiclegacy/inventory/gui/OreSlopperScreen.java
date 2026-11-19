package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.OreSlopperMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class OreSlopperScreen extends AbstractContainerScreen<OreSlopperMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_ore_slopper.png");

    public OreSlopperScreen(OreSlopperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 9;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity g2] FluidTank#renderTankInfo, drawElectricityInfo
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 26, this.topPos + 18, 16, 52,
                this.menu.waterFluid(), this.menu.waterAmount(), this.menu.tankCapacity());
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 116, this.topPos + 18, 16, 52,
                this.menu.slopFluid(), this.menu.slopAmount(), this.menu.tankCapacity());
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.progressScaled(35);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 52 - progress, 176, 34 - progress, 34, progress);
        }

        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 70 - power, 176, 86 - power, 16, power);
        }

        if (this.menu.power() >= this.menu.consumption()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 12, this.topPos + 4, 202, 34, 9, 12);
        }

        E2TankGui.renderTank(guiGraphics, this.leftPos + 26, this.topPos + 70, 16, 52, this.menu.waterFluid(), this.menu.waterAmount(), this.menu.tankCapacity());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 116, this.topPos + 70, 16, 52, this.menu.slopFluid(), this.menu.slopAmount(), this.menu.tankCapacity());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
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
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + tileY, 0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
