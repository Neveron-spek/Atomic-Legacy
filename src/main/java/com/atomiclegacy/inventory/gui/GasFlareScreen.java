package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.GasFlareMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class GasFlareScreen extends AbstractContainerScreen<GasFlareMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/generators/gui_flare_stack.png");

    public GasFlareScreen(GasFlareMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 16, 35, 10)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(Component.translatable("gui.atomiclegacy.gas_flare.valve")),
                    mouseX,
                    mouseY
            );
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 50, 35, 14)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(Component.translatable("gui.atomiclegacy.gas_flare.ignition")),
                    mouseX,
                    mouseY
            );
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 35, this.topPos + 17, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity()), this.menu.fluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 143, this.topPos + 17, 16, 52)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(K3GuiText.electricity(this.menu.power(), this.menu.powerCapacity())),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && K3GuiText.hover(mouseX, mouseY, this.leftPos + 89, this.topPos + 16, 16, 10)) {
            sendButton(0);
            playClick();
            return true;
        }
        if (button == 0 && K3GuiText.hover(mouseX, mouseY, this.leftPos + 89, this.topPos + 50, 16, 14)) {
            sendButton(1);
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - power, 176, 94 - power, 16, power);
        }
        if (this.menu.valveOpen()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 15, 176, 0, 35, 10);
        }
        if (this.menu.ignitionEnabled()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 49, 176, 10, 35, 14);
        }
        if (this.menu.valveOpen()
                && this.menu.ignitionEnabled()
                && this.menu.fluidAmount() > 0
                && this.menu.fluid().flammableHeatPerMillibucket() > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 29, 176, 24, 18, 18);
        }

        // [parity e2] flare.tank.renderTank(guiLeft + 35, guiTop + 69, zLevel, 16, 52)
        E2TankGui.renderTank(guiGraphics, this.leftPos + 35, this.topPos + 69, 16, 52,
                this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity());
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
