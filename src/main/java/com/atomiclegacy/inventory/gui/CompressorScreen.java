package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.CompressorBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.CompressorMenu;
import com.atomiclegacy.network.CompressorControlPayload;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class CompressorScreen extends AbstractContainerScreen<CompressorMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_compressor.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public CompressorScreen(CompressorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!renderHoverTooltips(guiGraphics, mouseX, mouseY)) {
            super.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        // [parity e2] GUICompressor#drawGuiContainerBackgroundLayer, order and sizes kept literally
        if (this.menu.energy() >= this.menu.usage()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 156, this.topPos + 4, 176, 52, 9, 12, TEX_W, TEX_H);
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 43 + this.menu.inputPressure() * 11, this.topPos + 46, 193, 18, 8, 124, TEX_W, TEX_H);
        int progress = this.menu.progress() * 55 / Math.max(1, this.menu.processTime());
        guiGraphics.blit(TEXTURE, this.leftPos + 42, this.topPos + 26, 192, 0, progress, 17, TEX_W, TEX_H);
        int energy = (int) ((long) this.menu.energy() * 52 / CompressorBlockEntity.MAX_POWER);
        guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - energy, 176, 52 - energy, 16, energy, TEX_W, TEX_H);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 17, this.topPos + 70, 16, 52,
                this.menu.inputFluid(), this.menu.inputAmount(), CompressorBlockEntity.TANK_CAPACITY);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 107, this.topPos + 70, 16, 52,
                this.menu.outputFluid(), this.menu.outputAmount(), CompressorBlockEntity.TANK_CAPACITY);
    }

    // [parity e2] title at 70 - width / 2 in 0xC7C1A3
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 0xC7C1A3, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int pressure = 0; pressure < 5; pressure++) {
                if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 43 + pressure * 11, this.topPos + 46, 8, 14)) {
                    PacketDistributor.sendToServer(new CompressorControlPayload(this.menu.blockPos(), pressure));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean renderHoverTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 17, this.topPos + 18, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.inputFluid(),
                    this.menu.inputAmount(),
                    CompressorBlockEntity.TANK_CAPACITY,
                    this.menu.inputPressure()), this.menu.inputFluid(), mouseX, mouseY);
            return true;
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 107, this.topPos + 18, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.outputFluid(),
                    this.menu.outputAmount(),
                    CompressorBlockEntity.TANK_CAPACITY,
                    this.menu.outputPressure()), this.menu.outputFluid(), mouseX, mouseY);
            return true;
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), CompressorBlockEntity.MAX_POWER)), mouseX, mouseY);
            return true;
        }
        for (int pressure = 0; pressure < 5; pressure++) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 43 + pressure * 11, this.topPos + 46, 8, 14)) {
                guiGraphics.renderComponentTooltip(this.font,
                        List.of(Component.literal(pressure + " PU -> " + (pressure + 1) + " PU")), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
