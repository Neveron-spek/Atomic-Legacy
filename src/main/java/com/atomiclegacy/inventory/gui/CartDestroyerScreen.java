package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.CartDestroyerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity S1: carts] GUICartDestroyer (textures/gui/cart/gui_destroyer.png) with the 8-frame shredder animation. */
public class CartDestroyerScreen extends AbstractContainerScreen<CartDestroyerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/cart/gui_destroyer.png");

    public CartDestroyerScreen(CartDestroyerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 4, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int time = (int) (System.currentTimeMillis() % 1000);
        int index = time / 128;

        if (index == 1 || index == 7) graphics.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 166, 44, 16);
        if (index == 2 || index == 6) graphics.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 182, 44, 16);
        if (index == 3 || index == 5) graphics.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 198, 44, 16);
        if (index == 4) graphics.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 214, 44, 16);
    }
}
