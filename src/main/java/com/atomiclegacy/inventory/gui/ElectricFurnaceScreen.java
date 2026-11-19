package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ElectricFurnaceBlockEntity;
import com.atomiclegacy.inventory.container.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** [parity K3] GUIMachineElectricFurnace of the 1.7.10 reference (gui/processing/gui_electric_furnace.png, 176x186). */
public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final ResourceLocation TEXTURE =
            AtomicLegacy.id("textures/gui/processing/gui_electric_furnace.png");

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
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
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(K3GuiText.electricity(this.menu.energy(), ElectricFurnaceBlockEntity.ENERGY_CAPACITY)), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 115, this.topPos + 19, 8, 8)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("desc.gui.upgrade"),
                    Component.translatable("desc.gui.upgrade.speed"),
                    Component.translatable("desc.gui.upgrade.power")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.hasPower()) {
            int p = (int) ((long) this.menu.energy() * 34 / ElectricFurnaceBlockEntity.ENERGY_CAPACITY);
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 52 - p, 176, 64 - p, 16, p);
        }

        if (this.menu.isWorking()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 45, this.topPos + 20, 192, 12, 18, 16);
            guiGraphics.blit(TEXTURE, this.leftPos + 46, this.topPos + 47, 192, 28, 18, 16);
        }

        int p = this.menu.progress() * 28 / this.menu.workTime();
        guiGraphics.blit(TEXTURE, this.leftPos + 43, this.topPos + 36, 176, 0, p, 12);

        K3GuiText.drawSmallBlueInfo(guiGraphics, this.leftPos + 115, this.topPos + 19);
    }
}
