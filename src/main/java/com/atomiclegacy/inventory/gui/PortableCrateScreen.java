package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import com.atomiclegacy.inventory.container.PortableCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PortableCrateScreen extends AbstractContainerScreen<PortableCrateMenu> {
    public PortableCrateScreen(PortableCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = menu.kind().guiWidth();
        this.imageHeight = menu.kind().guiHeight();
        this.inventoryLabelX = menu.kind().inventoryLabelX();
        // [parity e2] GUICrateDesh: ySize - 96 + 3, other crates ySize - 96 + 2
        this.inventoryLabelY = this.imageHeight - 96 + (menu.kind() == StorageCrateBlockEntity.Kind.DESH ? 3 : 2);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.menu.kind().texture(false), this.leftPos, this.topPos,
                0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        StorageCrateBlockEntity.Kind kind = this.menu.kind();
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, kind.titleColor(false), false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, kind.inventoryLabelColor(false), false);
    }
}
