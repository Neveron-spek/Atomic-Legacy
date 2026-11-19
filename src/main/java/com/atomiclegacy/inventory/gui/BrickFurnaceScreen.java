package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.BrickFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity e2] GUIFurnaceBrick of the 1.7.10 reference (gui/processing/gui_furnace_brick.png, default 176x166, no hover info). */
public final class BrickFurnaceScreen extends AbstractContainerScreen<BrickFurnaceMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_furnace_brick.png");

    public BrickFurnaceScreen(BrickFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // [parity e2] both labels are white in GUIFurnaceBrick
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0xFFFFFF, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // [parity e2] GUIFurnaceBrick: drawn while burnTime > 0, flame height b + 1 and arrow width p + 1
        if (menu.burnTime() > 0) {
            int b = menu.burnTime() * 13 / menu.maxBurnTime();
            graphics.blit(TEXTURE, leftPos + 62, topPos + 54 + 12 - b, 176, 12 - b, 14, b + 1);
            int p = menu.progress() * 24 / 200;
            graphics.blit(TEXTURE, leftPos + 85, topPos + 34, 176, 14, p + 1, 16);
        }
    }
}
