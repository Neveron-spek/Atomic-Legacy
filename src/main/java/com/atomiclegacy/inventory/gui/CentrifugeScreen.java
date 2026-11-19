package com.atomiclegacy.inventory.gui;

import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.CentrifugeMenu;
import com.atomiclegacy.tileentity.CentrifugeBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity K3: centrifuge] Mirrors GUIMachineCentrifuge (1.7.10): processing/gui_centrifuge.png, xSize 182, ySize 189. */
public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_centrifuge.png");

    public CentrifugeScreen(CentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 182;
        this.imageHeight = 189;
        this.inventoryLabelX = 11;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 + 36 / 2 - this.font.width(this.title) / 2;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        K3BGui.electricityInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 18, 16, 37,
                this.menu.energy(), CentrifugeBlockEntity.MAX_POWER);
        K3BGui.customInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 160, this.topPos + 16, 8, 8, mouseX, mouseY, List.of(
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"),
                Component.translatable("desc.gui.upgrade.overdrive")));
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.energy() > 0) {
            int i1 = (int) (this.menu.energy() * 37L / CentrifugeBlockEntity.MAX_POWER);
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 55 - i1, 182, 37 - i1, 16, i1);
        }
        if (this.menu.progress() > 0) {
            int p = this.menu.progress() * 145 / CentrifugeBlockEntity.BASE_PROCESSING_SPEED;
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                guiGraphics.blit(TEXTURE, this.leftPos + 72 + i * 20, this.topPos + 57 - h, 182, 73 - h, 12, h);
                p -= h;
                if (p <= 0) {
                    break;
                }
            }
        }
        guiGraphics.blit(K3BGui.UTILITY, this.leftPos + 160, this.topPos + 16, 0, 32, 8, 8);
    }
}
