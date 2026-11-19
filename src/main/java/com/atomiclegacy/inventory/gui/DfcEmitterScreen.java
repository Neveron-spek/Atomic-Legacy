package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.DfcEmitterMenu;
import com.atomiclegacy.network.DfcControlPayload;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class DfcEmitterScreen extends AbstractContainerScreen<DfcEmitterMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/dfc/gui_emitter.png");
    private EditBox wattsField;

    public DfcEmitterScreen(DfcEmitterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.wattsField = new EditBox(this.font, this.leftPos + 57, this.topPos + 57, 29, 12, Component.empty());
        this.wattsField.setTextColor(0xFFFFFF);
        this.wattsField.setTextColorUneditable(0xFFFFFF);
        this.wattsField.setBordered(false);
        this.wattsField.setMaxLength(3);
        this.wattsField.setValue(Integer.toString(this.menu.watts()));
        addRenderableWidget(this.wattsField);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.wattsField != null && !this.wattsField.isFocused()) {
            this.wattsField.setValue(Integer.toString(this.menu.watts()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [parity g1: GUICoreEmitter.mouseClicked - field focus follows the click, buttons react to any mouse button]
        boolean inField = this.wattsField.isMouseOver(mouseX, mouseY);
        this.wattsField.setFocused(inField);
        if (!inField && getFocused() == this.wattsField) {
            setFocused(null);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 97, this.topPos + 52, 18, 18)) {
            Integer watts = parseNumber(this.wattsField.getValue());
            if (watts != null) {
                int clamped = Math.max(1, Math.min(100, watts));
                this.wattsField.setValue(Integer.toString(clamped));
                playClick();
                PacketDistributor.sendToServer(new DfcControlPayload(this.menu.blockPos(), DfcControlPayload.TARGET_EMITTER_WATTS, clamped));
            }
            return true;
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 133, this.topPos + 52, 18, 18)) {
            playClick();
            PacketDistributor.sendToServer(new DfcControlPayload(this.menu.blockPos(), DfcControlPayload.TARGET_EMITTER_TOGGLE, 0));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [parity g1: GUICoreEmitter.keyTyped - a focused field consumes typed keys, only Esc falls through]
        if (this.wattsField.isFocused() && this.wattsField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.wattsField.isFocused() && keyCode != 256) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** NumberUtils.isNumber + (int) Double.parseDouble of the reference. */
    static Integer parseNumber(String text) {
        try {
            return (int) Double.parseDouble(text.trim().isEmpty() ? "x" : text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 17, 16, 52,
                this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity());
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 26, this.topPos + 17, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, Component.translatable("gui.atomiclegacy.dfc.output", DfcScreenUtil.shortNumber(this.menu.prev()) + "Spk"), 50, 30, 0xFF7F7F, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.wattsField != null && this.wattsField.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 53, 210, 4, 34, 16);
        }
        if (this.menu.on()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 52, 192, 0, 18, 18);
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 45, 210, 0, this.menu.watts() * 34 / 100, 4);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 69 - power, 176, 52 - power, 16, power);
        }
        E2TankGui.renderTank(guiGraphics, this.leftPos + 8, this.topPos + 69, 16, 52, this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity());
    }

    private void playClick() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
