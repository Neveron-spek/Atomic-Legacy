package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.PyroOvenMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Pixel-for-pixel slot layout of GUIPyroOven from the 1.7.10 client. */
public final class PyroOvenScreen extends AbstractContainerScreen<PyroOvenMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_pyrooven.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public PyroOvenScreen(PyroOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 18; // [parity e2]
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        // [parity e2] GUIPyroOven#drawScreen
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 18, 16, 52,
                this.menu.inputFluid(), this.menu.inputAmount(), 24_000);
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 116, this.topPos + 18, 16, 52,
                this.menu.outputFluid(), this.menu.outputAmount(), 24_000);
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52)) {
            graphics.renderComponentTooltip(this.font, java.util.List.of(K3GuiText.electricity(this.menu.energy(), 10_000_000L)), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 108, this.topPos + 76, 8, 8)) {
            // getUpgradeInfo: SPEED 3, POWER 3, OVERDRIVE 3; drawn at the panel, not at the cursor
            graphics.renderComponentTooltip(this.font, K3GuiText.upgradeInfo(3, 3, 0, 0, 3), this.leftPos + 108, this.topPos + 76);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(graphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energy = this.menu.energyScaled(52);
        if (energy > 0) {
            blit(graphics, this.leftPos + 152, this.topPos + 70 - energy, 176, 64 - energy, 16, energy);
        }
        int progress = this.menu.progressScaled(27);
        if (progress > 0) {
            blit(graphics, this.leftPos + 57, this.topPos + 47, 176, 0, progress, 12);
        }
        E2TankGui.renderTank(graphics, this.leftPos + 8, this.topPos + 70, 16, 52, this.menu.inputFluid(), this.menu.inputAmount(), 24_000);
        E2TankGui.renderTank(graphics, this.leftPos + 116, this.topPos + 70, 16, 52, this.menu.outputFluid(), this.menu.outputAmount(), 24_000);
        K3GuiText.drawSmallBlueInfo(graphics, this.leftPos + 108, this.topPos + 76);
    }

    private static void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
