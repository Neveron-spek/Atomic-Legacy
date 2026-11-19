package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.MachineKeyForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** [parity Q2: GUIMachineKeyForge] 176x186, white centred title, two info panels with desc.gui.keyforge.* */
public class MachineKeyForgeScreen extends AbstractContainerScreen<MachineKeyForgeMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_keyforge.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public MachineKeyForgeScreen(MachineKeyForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // drawCustomInfoStat(mouseX, mouseY, guiLeft + 12, guiTop + 28 (+16), 16, 16, guiLeft + 20, guiTop + 44 (+16), text)
        if (inside(mouseX, mouseY, this.leftPos + 12, this.topPos + 28)) {
            guiGraphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.keyforge.key"), this.leftPos + 20, this.topPos + 28 + 16);
        }
        if (inside(mouseX, mouseY, this.leftPos + 12, this.topPos + 28 + 16)) {
            guiGraphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.keyforge.random"), this.leftPos + 20, this.topPos + 28 + 32);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return K3GuiText.hover(mouseX, mouseY, x, y, 16, 16); // drawCustomInfoStat bounds
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // GuiInfoContainer#drawInfoPanel types 2 (large blue I) and 3 (large green I)
        guiGraphics.blit(UTILITY, this.leftPos + 12, this.topPos + 28, 8, 0, 16, 16, 256, 256);
        guiGraphics.blit(UTILITY, this.leftPos + 12, this.topPos + 28 + 16, 24, 0, 16, 16, 256, 256);
    }
}
