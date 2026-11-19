package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.PressBlockEntity;
import com.atomiclegacy.inventory.container.MachinePressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** [parity K3] GUIMachinePress of the 1.7.10 reference (gui/processing/gui_press.png, 176x214). */
public class MachinePressScreen extends AbstractContainerScreen<MachinePressMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_press.png");

    public MachinePressScreen(MachinePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 25, this.topPos + 16, 18, 18)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(this.menu.speedPercent() + "%")), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 25, this.topPos + 34, 18, 18)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(this.menu.operationsLeft() + " operations left")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 5, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.burnTime() >= 20) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 36, 0, 214, 14, 14);
        }

        int k = this.menu.progressScaled(16);
        if (k > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 35, 15, 214, 18, k);
        }

        double speed = (double) this.menu.speed() / (double) PressBlockEntity.MAX_SPEED;
        K3LegacyGauge.draw(guiGraphics, this.leftPos + 34, this.topPos + 25, speed, 5, 2, 1, 0x7F0000);
    }
}
