package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.FurnaceCombinationMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class FurnaceCombinationScreen extends AbstractContainerScreen<FurnaceCombinationMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_furnace_combination.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public FurnaceCombinationScreen(FurnaceCombinationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
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

        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 118, this.topPos + 18, 16, 52,
                this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity());
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 44, this.topPos + 36, 39, 7)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    format(this.menu.progress()) + " / " + format(this.menu.processTime()) + "TU"
            )), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 44, this.topPos + 45, 39, 7)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    format(this.menu.heat()) + " / " + format(this.menu.maxHeat()) + "TU"
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progressPixels = this.menu.progressScaled(38);
        if (progressPixels > 0) {
            blit(guiGraphics, this.leftPos + 45, this.topPos + 37, 176, 0, progressPixels, 5);
        }

        int heatPixels = this.menu.heatScaled(37);
        if (heatPixels > 0) {
            blit(guiGraphics, this.leftPos + 45, this.topPos + 46, 176, 5, heatPixels, 5);
        }

        E2TankGui.renderTank(guiGraphics, this.leftPos + 118, this.topPos + 70, 16, 52, this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity());
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }

    private static String format(int value) {
        return String.format(Locale.US, "%,d", value);
    }
}
