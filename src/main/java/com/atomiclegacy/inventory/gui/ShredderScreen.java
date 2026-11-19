package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ShredderBlockEntity;
import com.atomiclegacy.inventory.container.ShredderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** [parity K3: shredder] Mirrors GUIMachineShredder (1.7.10): textures/gui/processing/gui_shredder.png, xSize 176, ySize 233. */
public class ShredderScreen extends AbstractContainerScreen<ShredderMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_shredder.png");

    public ShredderScreen(ShredderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 106 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        K3BGui.electricityInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 106 - 88, 16, 88,
                this.menu.energy(), ShredderBlockEntity.ENERGY_CAPACITY);
        if (bladeError()) {
            K3BGui.customInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos - 16, this.topPos + 36, 16, 16,
                    this.leftPos - 8, this.topPos + 36 + 16, List.of(Component.literal("Error: Shredder blades are broken or missing!")));
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.energy() > 0) {
            int energyPixels = this.menu.energyScaled(88);
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 106 - energyPixels, 176, 160 - energyPixels, 16, energyPixels);
        }
        int progressPixels = this.menu.progressScaled(34);
        guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 89, 176, 54, progressPixels + 1, 18);
        renderGearIcon(guiGraphics, true, this.menu.leftGear());
        renderGearIcon(guiGraphics, false, this.menu.rightGear());
        if (bladeError()) {
            K3BGui.infoPanel(guiGraphics, this.leftPos - 16, this.topPos + 36, 6);
        }
    }

    private boolean bladeError() {
        int left = this.menu.leftGear();
        int right = this.menu.rightGear();
        return left == 0 || left == 3 || right == 0 || right == 3;
    }

    private void renderGearIcon(GuiGraphics guiGraphics, boolean left, int state) {
        if (state <= 0) {
            return;
        }
        int u = left ? 176 : 194;
        int v = Math.min(2, state - 1) * 18;
        int x = left ? 43 : 79;
        guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + 71, u, v, 18, 18);
    }
}
