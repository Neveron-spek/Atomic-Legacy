package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.BombMultiMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direct layout port of GUIBombMulti. */
public final class BombMultiScreen extends AbstractContainerScreen<BombMultiMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/weapon/bomb_generic.png");

    public BombMultiScreen(BombMultiMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int upper = this.menu.typeForSlot(2);
        int lower = this.menu.typeForSlot(5);
        // GUIBombMulti: matching types 1..6 draw their icon, two empty slots draw nothing, a mismatch draws icon 7.
        if (upper != lower) {
            graphics.blit(TEXTURE, this.leftPos + 124, this.topPos + 34, 176, 7 * 18, 18, 18);
        } else if (upper >= 1 && upper <= 6) {
            graphics.blit(TEXTURE, this.leftPos + 124, this.topPos + 34, 176, (upper - 1) * 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    /** [parity e2] item tooltips, as in GuiContainer.drawScreen */
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
