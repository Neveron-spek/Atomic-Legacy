package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.HeaterBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.HeaterMenu;
import com.atomiclegacy.network.HeatExchangerControlPayload;
import com.atomiclegacy.network.ToggleHeaterPayload;
import com.atomiclegacy.network.OilburnerBurnRatePayload;
import net.minecraft.client.gui.components.EditBox;
import com.atomiclegacy.util.HbmFluidTooltip;
import com.atomiclegacy.util.LegacyBurnModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class HeaterScreen extends AbstractContainerScreen<HeaterMenu> {
    private static final ResourceLocation FIREBOX = AtomicLegacy.id("textures/gui/machine/gui_firebox.png");
    private static final ResourceLocation OVEN = AtomicLegacy.id("textures/gui/machine/gui_heating_oven.png");
    private static final ResourceLocation OILBURNER = AtomicLegacy.id("textures/gui/machine/gui_oilburner.png");
    private static final ResourceLocation HEATEX = AtomicLegacy.id("textures/gui/machine/gui_heatex.png");
    private static final int COMMON_HEIGHT = 168;
    private static final int OILBURNER_HEIGHT = 203;
    private static final int HEATEX_HEIGHT = 204;
    private static final int OILBURNER_TOGGLE_X = 80;
    private static final int OILBURNER_TOGGLE_Y = 54;
    private static final int OILBURNER_TOGGLE_WIDTH = 16;
    private static final int OILBURNER_TOGGLE_HEIGHT = 14;
    /** [parity g1] TileEntityHeaterFirebox.burnModule == TileEntityHeaterOven.burnModule (description only). */
    private static final LegacyBurnModule FIREBOX_BURN_MODULE = new LegacyBurnModule()
            .time(LegacyBurnModule.LIGNITE, 1.25).time(LegacyBurnModule.COAL, 1.25).time(LegacyBurnModule.COKE, 1.25)
            .time(LegacyBurnModule.SOLID, 1.5).time(LegacyBurnModule.ROCKET, 1.5).time(LegacyBurnModule.BALEFIRE, 0.5)
            .heat(LegacyBurnModule.LIGNITE, 2).heat(LegacyBurnModule.COAL, 2).heat(LegacyBurnModule.COKE, 2)
            .heat(LegacyBurnModule.SOLID, 3).heat(LegacyBurnModule.ROCKET, 5).heat(LegacyBurnModule.BALEFIRE, 15);
    private EditBox heatExchangerAmountField;
    private EditBox heatExchangerDelayField;
    // [parity L3: oilburner] GUIOilburner burn rate slider state
    private boolean usingSlider;
    private int burnRate = -1;

    public HeaterScreen(HeaterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = menu.isOilburnerLayout() ? OILBURNER_HEIGHT : menu.isHeatExchangerLayout() ? HEATEX_HEIGHT : COMMON_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        if (this.menu.isHeatExchangerLayout()) {
            this.heatExchangerAmountField = new EditBox(this.font, this.leftPos + 73, this.topPos + 31, 30, 10, Component.empty());
            initHeatExchangerField(this.heatExchangerAmountField);
            this.heatExchangerAmountField.setValue(Integer.toString(this.menu.heatExchangerAmountToCool()));
            // [parity g1: GUIHeaterHeatex.keyTyped - each field sends only its own value, Math.max(NumberUtils.toInt(text), 1)]
            this.heatExchangerAmountField.setResponder(value -> PacketDistributor.sendToServer(new HeatExchangerControlPayload(
                    this.menu.blockPos(), legacyToInt(value), this.menu.heatExchangerTickDelay())));
            this.addRenderableWidget(this.heatExchangerAmountField);

            this.heatExchangerDelayField = new EditBox(this.font, this.leftPos + 73, this.topPos + 49, 30, 10, Component.empty());
            initHeatExchangerField(this.heatExchangerDelayField);
            this.heatExchangerDelayField.setValue(Integer.toString(this.menu.heatExchangerTickDelay()));
            this.heatExchangerDelayField.setResponder(value -> PacketDistributor.sendToServer(new HeatExchangerControlPayload(
                    this.menu.blockPos(), this.menu.heatExchangerAmountToCool(), legacyToInt(value))));
            this.addRenderableWidget(this.heatExchangerDelayField);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.menu.isOilburnerLayout()) {
            renderOilburnerTooltips(guiGraphics, mouseX, mouseY);
            return;
        }
        if (this.menu.isHeatExchangerLayout()) {
            renderHeatExchangerTooltips(guiGraphics, mouseX, mouseY);
            return;
        }

        // [parity g1: GUIFirebox.drawScreen]
        if (this.menu.kind() <= HeaterBlockEntity.Kind.OVEN.ordinal() && this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null && this.hoveredSlot.index < 2 && !this.hoveredSlot.hasItem()) {
            List<Component> bonuses = new java.util.ArrayList<>(FIREBOX_BURN_MODULE.timeDesc());
            bonuses.addAll(FIREBOX_BURN_MODULE.heatDesc());
            if (!bonuses.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, bonuses, mouseX, mouseY);
            }
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 80, this.topPos + 27, 71, 7)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format(java.util.Locale.US, "%,d", this.menu.heat()) + " / " + String.format(java.util.Locale.US, "%,d", this.menu.maxHeat()) + "TU"
            )), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 80, this.topPos + 36, 71, 7)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(this.menu.burnHeat() + "TU/t"),
                    Component.literal((this.menu.burnTime() / 20) + "s")
            ), mouseX, mouseY);
        }
        if (isHovering(80, 47, 71, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("Setting: " + this.menu.setting()),
                    Component.literal("Power: " + this.menu.power() + " HE"),
                    Component.literal("Input: " + this.menu.lastInput() + " HE/t")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture();
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.isOilburnerLayout()) {
            renderOilburnerBg(guiGraphics, texture);
            return;
        }
        if (this.menu.isHeatExchangerLayout()) {
            renderHeatExchangerBg(guiGraphics);
            return;
        }

        int heat = this.menu.heatScaled(69);
        if (heat > 0) {
            guiGraphics.blit(texture, this.leftPos + 81, this.topPos + 28, 176, 0, heat, 5);
        }

        int burn = this.menu.burnScaled(70);
        if (burn > 0) {
            guiGraphics.blit(texture, this.leftPos + 81, this.topPos + 37, 176, 5, burn, 5);
        }

        if (this.menu.enabled() && (this.menu.burnTime() > 0 || this.menu.kind() >= 2)) {
            guiGraphics.blit(texture, this.leftPos + 25, this.topPos + 26, 176, 10, 18, 18);
        }
    }

    private ResourceLocation texture() {
        return switch (this.menu.kind()) {
            case 1 -> OVEN;
            case 2 -> OILBURNER;
            case 4 -> HEATEX;
            default -> FIREBOX;
        };
    }

    private void renderOilburnerBg(GuiGraphics guiGraphics, ResourceLocation texture) {
        if (this.burnRate < 0) {
            this.burnRate = this.menu.setting();
        }
        int maxBurnRate = this.menu.maxBurnRate();
        int j = maxBurnRate > 1 ? (this.burnRate - 1) * 32 / (maxBurnRate - 1) : 0;
        if (j > 32) { // an upgrade was removed, the cached burn rate exceeds the new max
            j = 32;
            this.burnRate = this.menu.setting();
        }
        guiGraphics.blit(texture, this.leftPos + 71 + j, this.topPos + 23, 210, 14, 4, 8);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 44, this.topPos + 69, 16, 52, this.menu.oilFluid(), this.menu.oilAmount(), this.menu.oilCapacity()); // [parity g2]

        int heat = this.menu.heatScaled(52);
        if (heat > 0) {
            guiGraphics.blit(texture, this.leftPos + 116, this.topPos + 69 - heat, 194, 52 - heat, 16, heat);
        }

        if (this.menu.enabled()) {
            guiGraphics.blit(texture, this.leftPos + 70, this.topPos + 54, 210, 0, 35, 14);
            if (this.menu.oilAmount() > 0 && HeaterBlockEntity.flammableHeatPerMillibucket(this.menu.oilFluid()) > 0) {
                guiGraphics.blit(texture, this.leftPos + 79, this.topPos + 34, 176, 0, 18, 18);
            }
        }
    }

    private void renderHeatExchangerBg(GuiGraphics guiGraphics) {
        E2TankGui.renderTank(guiGraphics, this.leftPos + 44, this.topPos + 88, 16, 52, this.menu.heatExchangerInputFluid(),
                this.menu.heatExchangerInputAmount(), this.menu.heatExchangerInputCapacity());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 116, this.topPos + 88, 16, 52, this.menu.heatExchangerOutputFluid(),
                this.menu.heatExchangerOutputAmount(), this.menu.heatExchangerOutputCapacity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [parity g1: GUIHeaterHeatex.mouseClicked - GuiTextField focus follows the click]
        for (EditBox field : new EditBox[] {this.heatExchangerAmountField, this.heatExchangerDelayField}) {
            if (field != null && !field.isMouseOver(mouseX, mouseY)) {
                field.setFocused(false);
                if (getFocused() == field) {
                    setFocused(null);
                }
            }
        }
        if (this.menu.isOilburnerLayout() // [parity g2] GUIOilburner: any mouse button
                && this.leftPos + OILBURNER_TOGGLE_X <= mouseX
                && this.leftPos + OILBURNER_TOGGLE_X + OILBURNER_TOGGLE_WIDTH > mouseX
                && this.topPos + OILBURNER_TOGGLE_Y < mouseY
                && this.topPos + OILBURNER_TOGGLE_Y + OILBURNER_TOGGLE_HEIGHT >= mouseY) {
            PacketDistributor.sendToServer(new ToggleHeaterPayload(this.menu.blockPos()));
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        if (this.menu.isOilburnerLayout()
                && this.leftPos + 70 <= mouseX && this.leftPos + 70 + 36 > mouseX
                && this.topPos + 23 < mouseY && this.topPos + 23 + 8 >= mouseY) {
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            this.usingSlider = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderOilburnerTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 116, this.topPos + 17, 16, 52)) { // [parity g2] drawCustomInfoStat
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format(java.util.Locale.US, "%,d", this.menu.heat()) + " / " + String.format(java.util.Locale.US, "%,d", this.menu.maxHeat()) + " TU"
            )), mouseX, mouseY);
        }
        int rate = this.burnRate < 0 ? this.menu.setting() : this.burnRate;
        if (this.usingSlider || (this.leftPos + 70 <= mouseX && this.leftPos + 70 + 36 > mouseX && this.topPos + 23 < mouseY && this.topPos + 23 + 8 >= mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(rate + " mB/t")),
                    Math.max(this.leftPos + 70, Math.min(mouseX, this.leftPos + 106)), Math.max(this.topPos + 23, Math.min(mouseY, this.topPos + 31)));
        }
        long heatEnergy = this.menu.oilFluid().flammableHeatEnergy();
        if (heatEnergy > 0 && K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 34, 18, 18)) {
            int heat = (int) (heatEnergy * rate / 1000);
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("\u00a7e" + String.format(java.util.Locale.US, "%,d", heat) + " TU/t"),
                    Component.literal("\u00a7e" + String.format(java.util.Locale.US, "%,d", heat * 20) + " TU/s")
            ), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 70, this.topPos + 54, 35, 15)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Ignition")), mouseX, mouseY);
        }
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 44, this.topPos + 17, 16, 52,
                this.menu.oilFluid(), this.menu.oilAmount(), this.menu.oilCapacity()); // [parity g2] FluidTank#renderTankInfo
        if (this.usingSlider) {
            int maxBurnRate = this.menu.maxBurnRate();
            int next = Math.max(1, Math.min((mouseX - this.leftPos - 70) * maxBurnRate / 32, maxBurnRate));
            if (next != this.burnRate) {
                this.burnRate = next;
                PacketDistributor.sendToServer(new OilburnerBurnRatePayload(this.menu.blockPos(), next));
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.usingSlider && (button == 0 || button == 1)) {
            this.usingSlider = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderHeatExchangerTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 44, this.topPos + 36, 16, 52,
                this.menu.heatExchangerInputFluid(), this.menu.heatExchangerInputAmount(), this.menu.heatExchangerInputCapacity());
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 116, this.topPos + 36, 16, 52,
                this.menu.heatExchangerOutputFluid(), this.menu.heatExchangerOutputAmount(), this.menu.heatExchangerOutputCapacity());
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 70, this.topPos + 26, 36, 18)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.atomiclegacy.heatex.amount_per_cycle")), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 70, this.topPos + 44, 36, 18)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.atomiclegacy.heatex.cycle_tick_delay")), mouseX, mouseY);
        }
    }

    /** GUIHeaterHeatex.initText: green text, no background, 5 characters. */
    private static void initHeatExchangerField(EditBox field) {
        field.setTextColor(0x00FF00);
        field.setTextColorUneditable(0x00FF00);
        field.setBordered(false);
        field.setMaxLength(5);
    }

    private static int legacyToInt(String value) {
        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [parity g1: GUIHeaterHeatex.keyTyped - a focused field consumes typed keys, only Esc falls through]
        for (EditBox field : new EditBox[] {this.heatExchangerAmountField, this.heatExchangerDelayField}) {
            if (field != null && field.isFocused() && (field.keyPressed(keyCode, scanCode, modifiers) || keyCode != 256)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // [parity g1: GUIFirebox - the heating oven title is white]
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                this.menu.kind() == HeaterBlockEntity.Kind.OVEN.ordinal() ? 0xFFFFFF : 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);

        int top = bottomY - height;
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            for (int tileY = 0; tileY < height; tileY += 16) {
                int tileHeight = Math.min(16, height - tileY);
                graphics.blit(
                        texture,
                        this.leftPos + x + tileX,
                        this.topPos + top + tileY,
                        0,
                        16 - tileHeight,
                        tileWidth,
                        tileHeight,
                        16,
                        16
                );
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}
