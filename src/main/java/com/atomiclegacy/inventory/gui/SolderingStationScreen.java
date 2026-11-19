package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.SolderingStationMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SolderingStationScreen extends AbstractContainerScreen<SolderingStationMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_soldering_station.png");
    private static final int TANK_X = 35;
    private static final int TANK_Y = 63;
    private static final int TANK_WIDTH = 34;
    private static final int TANK_HEIGHT = 16;
    private static final int TANK_BOTTOM_Y = 79;

    public SolderingStationScreen(SolderingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        // [parity K3: GUIMachineSolderingStation draws the title 18 px left of centre]
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 18;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity K3: GUIMachineSolderingStation.drawScreen - tank, energy, upgrade info and collision button]
        if (isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.tankAmount(), this.menu.tankCapacity()),
                    mouseX,
                    mouseY
            );
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(K3GuiText.electricity(this.menu.energyLong(), this.menu.maxPower())), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 78, this.topPos + 67, 8, 8)) {
            guiGraphics.renderComponentTooltip(this.font, K3GuiText.upgradeInfo(3, 3, 0, 0, 3),
                    this.leftPos + 78, this.topPos + 67);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 5, this.topPos + 66, 10, 10)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("Recipe Collision Prevention: ").append(this.menu.collisionPrevention()
                            ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                            : Component.literal("OFF").withStyle(ChatFormatting.RED)),
                    Component.literal("Prevents no-fluid recipes from being processed"),
                    Component.literal("when fluid is present.")
            ), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (this.leftPos + 5 <= mouseX && this.leftPos + 5 + 10 > mouseX
                && this.topPos + 66 < mouseY && this.topPos + 66 + 10 >= mouseY
                && this.minecraft != null && this.minecraft.gameMode != null) {
            K3GuiText.click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        return handled;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.collisionPrevention()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 5, this.topPos + 66, 192, 14, 10, 10);
        }

        int powerPixels = (int) (this.menu.energyLong() * 52 / Math.max(this.menu.maxPower(), 1));
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
            guiGraphics.blit(TEXTURE, this.leftPos + 72, this.topPos + 28, 192, 0, progressPixels, 14);
        }

        if (this.menu.energy() >= this.menu.consumption()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 156, this.topPos + 4, 176, 52, 9, 12);
        }

        K3GuiText.drawSmallBlueInfo(guiGraphics, this.leftPos + 78, this.topPos + 67);
        // [parity e2] tank.renderTank(guiLeft + 35, guiTop + 79, z, 34, 16, 1): horizontal gauge
        E2TankGui.renderTank(guiGraphics, this.leftPos + TANK_X, this.topPos + TANK_BOTTOM_Y, TANK_WIDTH, TANK_HEIGHT, 1,
                this.menu.fluid(), this.menu.tankAmount(), this.menu.tankCapacity());
    }

}
