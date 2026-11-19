package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.PressBlockEntity;
import com.atomiclegacy.inventory.container.MachineEPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** [parity K3] GUIMachineEPress of the 1.7.10 reference (gui/processing/gui_electric_press.png, 176x186). */
public class MachineEPressScreen extends AbstractContainerScreen<MachineEPressMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_electric_press.png");

    public MachineEPressScreen(MachineEPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 52 - 34, 16, 34)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), PressBlockEntity.ELECTRIC_ENERGY_CAPACITY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 89 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int i = (int) ((long) this.menu.energy() * 34 / PressBlockEntity.ELECTRIC_ENERGY_CAPACITY);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 52 - i, 176, 34 - i, 16, i);
        }

        int k = this.menu.progressScaled(16);
        if (k > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 18, this.topPos + 33, 192, 0, 18, k);
        }
    }
}
