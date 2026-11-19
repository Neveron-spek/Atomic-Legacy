package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.ResearchReactorMenu;
import com.atomiclegacy.network.ResearchReactorControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ResearchReactorScreen extends AbstractContainerScreen<ResearchReactorMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/reactors/gui_research_reactor.png");
    private static final int FIELD_X = 8;
    private static final int FIELD_Y = 99;
    private static final int FIELD_WIDTH = 33;
    private static final int FIELD_HEIGHT = 16;
    private static final int CONFIRM_X = 44;
    private static final int CONFIRM_Y = 97;
    private static final int CONFIRM_WIDTH = 11;
    private static final int CONFIRM_HEIGHT = 20;

    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");
    private EditBox controlField;
    private int confirmFlashTicks;
    // [parity M2: research reactor] GUIReactorResearch NumberDisplay instances
    private final M2NumberDisplay[] displays = {
            new M2NumberDisplay(14, 25, 0x08FF00).setDigitLength(4),
            new M2NumberDisplay(12, 63, 0x08FF00).setDigitLength(3),
            new M2NumberDisplay(5, 101, 0x08FF00).setDigitLength(3)
    };

    public ResearchReactorScreen(ResearchReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 121 - this.font.width(this.title) / 2;
        this.controlField = new EditBox(this.font, this.leftPos + FIELD_X, this.topPos + FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        this.controlField.setBordered(false);
        this.controlField.setMaxLength(3);
        this.controlField.setValue(String.valueOf((int) (this.menu.controlLevel() * 100)));
        // the original never draws the text field; only the third number display shows its value
        this.addWidget(this.controlField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(-14, 23, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.atomiclegacy.research_reactor.cooling.0"),
                    Component.translatable("tooltip.atomiclegacy.research_reactor.cooling.1"),
                    Component.translatable("tooltip.atomiclegacy.research_reactor.cooling.2"),
                    Component.translatable("tooltip.atomiclegacy.research_reactor.cooling.3")
            ), this.leftPos - 6, this.topPos + 23 + 16);
        }
        if (isHovering(-14, 61, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.atomiclegacy.research_reactor.fuel.0"),
                    Component.translatable("tooltip.atomiclegacy.research_reactor.fuel.1")
            ), this.leftPos - 6, this.topPos + 61 + 16);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.controlLevel() <= 0.5D) {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 81 + 36 * x, this.topPos + 26 + 36 * y, 176, 0, 8, 8);
                }
            }
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 99 + 36 * x, this.topPos + 44 + 36 * y, 176, 0, 8, 8);
                }
            }
        }

        if (this.confirmFlashTicks > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + CONFIRM_X, this.topPos + CONFIRM_Y, 176, 8, CONFIRM_WIDTH, CONFIRM_HEIGHT);
            this.confirmFlashTicks--;
        }

        this.displays[0].drawNumber(guiGraphics, this.leftPos, this.topPos, this.menu.flux());
        this.displays[1].drawNumber(guiGraphics, this.leftPos, this.topPos, this.menu.temperature());
        String text = this.controlField.getValue();
        if (!text.isEmpty() && text.chars().allMatch(Character::isDigit)) {
            int level = (int) Mth.clamp(Double.parseDouble(text), 0, 100);
            this.controlField.setValue(level + "");
            this.displays[2].drawNumber(guiGraphics, this.leftPos, this.topPos, level);
        } else {
            this.controlField.setValue(0 + "");
            this.displays[2].drawNumber(guiGraphics, this.leftPos, this.topPos, 0);
        }
        // GuiInfoContainer#drawInfoPanel types 3 and 2 from gui_utility.png
        guiGraphics.blit(UTILITY, this.leftPos - 14, this.topPos + 23, 24, 0, 16, 16, 256, 256);
        guiGraphics.blit(UTILITY, this.leftPos - 14, this.topPos + 61, 8, 0, 16, 16, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 15066597, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        // [parity M2] hard-coded labels of GUIReactorResearch
        guiGraphics.drawString(this.font, "Flux", 6, 13, 15066597, false);
        guiGraphics.drawString(this.font, "Heat", 6, 51, 15066597, false);
        guiGraphics.drawString(this.font, "Control", 6, 89, 15066597, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.displays[2].setBlinks(this.leftPos + FIELD_X <= mouseX && this.leftPos + FIELD_X + FIELD_WIDTH > mouseX
                && this.topPos + FIELD_Y < mouseY && this.topPos + FIELD_Y + FIELD_HEIGHT >= mouseY);
        if (this.leftPos + CONFIRM_X <= mouseX
                && this.leftPos + CONFIRM_X + CONFIRM_WIDTH > mouseX
                && this.topPos + CONFIRM_Y < mouseY
                && this.topPos + CONFIRM_Y + CONFIRM_HEIGHT >= mouseY) {
            double parsed;
            try {
                parsed = Double.parseDouble(this.controlField.getValue());
            } catch (NumberFormatException ignored) {
                return handled;
            }
            int level = (int) Mth.clamp(parsed, 0, 100);
            this.controlField.setValue(Integer.toString(level));
            PacketDistributor.sendToServer(new ResearchReactorControlPayload(this.menu.blockPos(), level));
            this.confirmFlashTicks = 15;
            playClick();
            return true;
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.controlField.isFocused() && keyCode != 256 && this.controlField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.controlField.isFocused() && keyCode != 256 && this.minecraft != null
                && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void playClick() {
        if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
            // [parity M2] hbm:block.rbmk_az5_cover at pitch 0.5
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(com.atomiclegacy.registry.HbmSoundEvents.RBMK_AZ5_COVER.get(), 0.5F));
        }
    }
}
