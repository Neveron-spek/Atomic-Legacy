package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.LegacyAmmoBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity b1: ammo bag] 1.7.10 GUIAmmoBag (176x168) and GUICasingBag (176x186). */
public final class LegacyAmmoBagScreen extends AbstractContainerScreen<LegacyAmmoBagMenu> {
    private static final ResourceLocation AMMO_BAG = AtomicLegacy.id("textures/gui/gui_ammo_bag.png");
    private static final ResourceLocation CASING_BAG = AtomicLegacy.id("textures/gui/gui_casing_bag.png");

    public LegacyAmmoBagScreen(LegacyAmmoBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = menu.kind().ammo() ? 168 : 186;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(menu.kind().ammo() ? AMMO_BAG : CASING_BAG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 98, 4210752, false);
    }
}
