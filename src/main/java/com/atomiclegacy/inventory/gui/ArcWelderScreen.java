package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.ArcWelderMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ArcWelderScreen extends AbstractContainerScreen<ArcWelderMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_arc_welder.png");
    private static final int TANK_X = 35;
    private static final int TANK_Y = 63;
    private static final int TANK_WIDTH = 34;
    private static final int TANK_HEIGHT = 16;
    private static final int TANK_BOTTOM_Y = 79;

    public ArcWelderScreen(ArcWelderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        // [parity e2] GUIMachineArcWelder draws the title 18 px left of centre
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 18;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity e2] GUIMachineArcWelder#drawScreen: tank, energy and upgrade info; no progress tooltip
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + TANK_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.tankAmount(), this.menu.tankCapacity()),
                    mouseX,
                    mouseY
            );
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(K3GuiText.electricity(this.menu.energy(), this.menu.maxPower())), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 78, this.topPos + 67, 8, 8)) {
            guiGraphics.renderComponentTooltip(this.font, K3GuiText.upgradeInfo(3, 3, 0, 0, 3),
                    this.leftPos + 78, this.topPos + 67);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerPixels = (int) ((long) this.menu.energy() * 52 / Math.max(this.menu.maxPower(), 1));
        if (powerPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 152,
                    this.topPos + 70 - powerPixels,
                    176,
                    52 - powerPixels,
                    16,
                    powerPixels
            );
        }

        int progressPixels = this.menu.progressScaled(33);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 72, this.topPos + 37, 192, 0, progressPixels, 14);
        }

        if (this.menu.energy() >= this.menu.consumption()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 156, this.topPos + 4, 176, 52, 9, 12);
        }

        K3GuiText.drawSmallBlueInfo(guiGraphics, this.leftPos + 78, this.topPos + 67); // [parity e2] drawInfoPanel(.., 8)
        // [parity e2] tank.renderTank(guiLeft + 35, guiTop + 79, z, 34, 16, 1): horizontal gauge
        E2TankGui.renderTank(guiGraphics, this.leftPos + TANK_X, this.topPos + TANK_BOTTOM_Y, TANK_WIDTH, TANK_HEIGHT, 1,
                this.menu.fluid(), this.menu.tankAmount(), this.menu.tankCapacity());
    }

}
