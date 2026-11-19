package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.SirenBlockEntity;
import com.atomiclegacy.inventory.container.SirenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class SirenScreen extends AbstractContainerScreen<SirenMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_siren.png");

    public SirenScreen(SirenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2; // [parity g2] centred title
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.blockPos()) instanceof SirenBlockEntity siren
                && siren.selectedTrack() != null) {
            var track = siren.selectedTrack();
            graphics.drawString(this.font, track.title(), 46, 28, track.color(), false);
            graphics.drawString(this.font, Component.translatable("gui.atomiclegacy.siren.type", track.playback().name()), 46, 40, track.color(), false);
            graphics.drawString(this.font, Component.translatable("gui.atomiclegacy.siren.volume", track.volume()), 46, 52, track.color(), false);
        }
    }
}
