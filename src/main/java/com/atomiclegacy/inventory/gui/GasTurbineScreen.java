package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.GasTurbineBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.GasTurbineMenu;
import com.atomiclegacy.network.GasTurbineControlPayload;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GasTurbineScreen extends AbstractContainerScreen<GasTurbineMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/generators/gui_turbinegas.png");
    private int yStart;
    private int slidStart;
    private int numberToDisplay;
    private int digitNumber;
    private int exponent;

    public GasTurbineScreen(GasTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 223;
        this.inventoryLabelY = this.imageHeight - 94;
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
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos + 74, this.topPos + 86, 194, this.menu.autoMode() ? 11 : 24, 29, 13);

        switch (this.menu.state()) {
            case 0 -> guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 32, 178, 38, 16, 16);
            case -1 -> {
                guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 32, 194, 38, 16, 16);
                displayStartup(guiGraphics);
            }
            case 1 -> {
                guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 32, 210, 38, 16, 16);
                drawPowerMeterDisplay(guiGraphics, 20 * this.menu.instantPowerOutput());
            }
            default -> {
            }
        }

        guiGraphics.blit(TEXTURE, this.leftPos + 36, this.topPos + 97 - this.menu.powerSliderPos(), 178, 0, 16, 6);
        int powerPixels = this.menu.powerScaled(142);
        if (powerPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 109, 0, 223, powerPixels, 16);
        }

        drawRPMGauge(guiGraphics, this.menu.rpm());
        drawThermometer(guiGraphics, this.menu.temp()); // [parity g2] raw temp
        drawInfoPanels(guiGraphics);

        // [parity g2] FluidTank#renderTank
        E2TankGui.renderTank(guiGraphics, this.leftPos + 8, this.topPos + 65, 16, 48, this.menu.fuelFluid(), this.menu.fuelAmount(), GasTurbineBlockEntity.FUEL_CAPACITY);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 8, this.topPos + 103, 16, 32, this.menu.lubricantFluid(), this.menu.lubricantAmount(), GasTurbineBlockEntity.LUBRICANT_CAPACITY);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 147, this.topPos + 98, 16, 36, this.menu.waterFluid(), this.menu.waterAmount(), GasTurbineBlockEntity.WATER_CAPACITY);
        E2TankGui.renderTank(guiGraphics, this.leftPos + 147, this.topPos + 58, 16, 36, this.menu.steamFluid(), this.menu.steamAmount(), GasTurbineBlockEntity.STEAM_CAPACITY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [parity g2] GUIMachineTurbineGas#mouseClicked: any button, slider anchor stored first
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.slidStart = this.menu.powerSliderPos();
        this.yStart = (int) mouseY;
        if (Math.sqrt(Math.pow(mouseX - (this.leftPos + 88), 2) + Math.pow(mouseY - (this.topPos + 40), 2)) <= 8.0D) {
            if (this.menu.counterOrOutput() == 0 || this.menu.counterOrOutput() == 579) {
                playClick();
                sendState(this.menu.state() - 1);
                handled = true;
            } else {
                return true;
            }
        }
        if (this.menu.state() == 1
                && mouseX > this.leftPos + 74
                && mouseX <= this.leftPos + 74 + 29
                && mouseY >= this.topPos + 86
                && mouseY < this.topPos + 86 + 13) {
            playClick();
            sendAuto(!this.menu.autoMode());
            handled = true;
        }
        if (this.menu.state() == 1
                && this.topPos + 97 - this.slidStart <= this.yStart
                && this.topPos + 103 - this.slidStart > this.yStart
                && this.leftPos + 36 < mouseX
                && this.leftPos + 52 >= mouseX) {
            sendAuto(false);
            playClick();
            handled = true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.menu.autoMode()
                && this.menu.state() == 1
                && this.leftPos + 36 < mouseX
                && this.leftPos + 52 >= mouseX
                && this.topPos + 37 < mouseY
                && this.topPos + 103 >= mouseY
                && this.topPos + 97 - this.slidStart <= this.yStart
                && this.topPos + 103 - this.slidStart > this.yStart) {
            int slidPos = this.topPos + 100 - (int) mouseY;
            if (slidPos > 60) {
                slidPos = 60;
            } else if (slidPos < 0) {
                slidPos = 0;
            }
            sendSlider(slidPos);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 26, this.topPos + 108, 142, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), GasTurbineBlockEntity.ENERGY_CAPACITY)), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 36, this.topPos + 36, 16, 66)) {
            // [parity g2] 20 * (consumption * 0.05D + consumption * throttle / 100), Java double formatting
            double consumption = GasTurbineBlockEntity.fuelMaxConsumption(this.menu.fuelFluid());
            String line = this.menu.state() == 1
                    ? "Fuel consumption: " + 20 * (consumption * 0.05D + consumption * this.menu.throttle() / 100) + " mb/s"
                    : "Generator offline";
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(line)), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 133, this.topPos + 23, 8, 72)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Temperature: " + Math.max(20, this.menu.temp()) + "\u00B0C")), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 16, 16, 48)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.fuelFluid(), this.menu.fuelAmount(), GasTurbineBlockEntity.FUEL_CAPACITY), this.menu.fuelFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 70, 16, 32)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.lubricantFluid(), this.menu.lubricantAmount(), GasTurbineBlockEntity.LUBRICANT_CAPACITY), this.menu.lubricantFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 147, this.topPos + 61, 16, 36)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.waterFluid(), this.menu.waterAmount(), GasTurbineBlockEntity.WATER_CAPACITY), this.menu.waterFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 147, this.topPos + 21, 16, 36)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.steamFluid(), this.menu.steamAmount(), GasTurbineBlockEntity.STEAM_CAPACITY), this.menu.steamFluid(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + -16, this.topPos + 34, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.turbinegas.automode"), this.leftPos - 8, this.topPos + 60);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + -16, this.topPos + 50, 16, 16)) {
            ArrayList<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("desc.gui.turbinegas.fuels"));
            for (HbmFluidDefinition definition : HbmFluids.niceOrder()) {
                if (GasTurbineBlockEntity.isGasFuel(definition)) {
                    lines.add(Component.literal("  ").append(Component.translatable(definition.translationKey())));
                }
            }
            guiGraphics.renderComponentTooltip(this.font, lines, this.leftPos - 8, this.topPos + 60);
        }
        if ((this.menu.fuelAmount() < 5000 || this.menu.lubricantAmount() < 1000) && K3GuiText.hover(mouseX, mouseY, this.leftPos + -16, this.topPos + 66, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.turbinegas.warning"), this.leftPos - 8, this.topPos + 60);
        }
    }

    private void displayStartup(GuiGraphics guiGraphics) {
        if (this.numberToDisplay < 8_888_888 && this.menu.counterOrOutput() < 60) {
            this.digitNumber++;
            if (this.digitNumber == 9) {
                this.digitNumber = 1;
                this.exponent++;
            }
            this.numberToDisplay += (int) Math.pow(10, this.exponent);
        }
        if (this.menu.counterOrOutput() > 50) {
            this.numberToDisplay = 0;
        }
        drawPowerMeterDisplay(guiGraphics, this.numberToDisplay);
    }

    private void drawPowerMeterDisplay(GuiGraphics guiGraphics, int number) {
        int firstDigitX = 65;
        int firstDigitY = 71;
        int[] digit = new int[7];
        for (int i = 6; i >= 0; i--) {
            digit[i] = number % 10;
            number /= 10;
            guiGraphics.blit(TEXTURE, this.leftPos + firstDigitX + i * 7, this.topPos + firstDigitY, 194 + digit[i] * 5, 0, 5, 11);
        }

        int uselessZeros = 0;
        for (int i = 0; i < 6; i++) {
            if (digit[i] == 0) {
                uselessZeros++;
            } else {
                break;
            }
        }
        for (int i = 0; i < uselessZeros; i++) {
            guiGraphics.blit(TEXTURE, this.leftPos + firstDigitX + i * 7, this.topPos + firstDigitY, 244, 0, 5, 11);
        }
    }

    private void drawRPMGauge(GuiGraphics guiGraphics, int position) {
        // [parity L1: gas turbine] GUIElements.drawSmoothTextureModalCircle(guiLeft + 64, guiTop + 16, 176, 64, 48, 48, rpm / 100)
        LegacyGuiElements.drawSmoothTextureModalCircle(guiGraphics, TEXTURE, this.leftPos + 64, this.topPos + 16, 176, 64, 48, 48, (double) position / 100);
    }

    private void drawThermometer(GuiGraphics guiGraphics, int temp) {
        int height = 64 * temp / 800;
        if (height > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 136, this.topPos + 28 + 64 - height, 176, 64 - height, 2, height);
        }
    }

    private void drawInfoPanels(GuiGraphics guiGraphics) {
        // [parity L1: gas turbine] drawInfoPanel types 3, 2, 7 and 6 from gui_utility.png
        guiGraphics.blit(K3GuiText.UTILITY, this.leftPos - 16, this.topPos + 34, 24, 0, 16, 16, 256, 256);
        guiGraphics.blit(K3GuiText.UTILITY, this.leftPos - 16, this.topPos + 50, 8, 0, 16, 16, 256, 256);
        if (this.menu.fuelAmount() < 5000 || this.menu.lubricantAmount() < 1000) {
            guiGraphics.blit(K3GuiText.UTILITY, this.leftPos - 16, this.topPos + 66, 24, 16, 16, 16, 256, 256);
        }
        if (this.menu.fuelAmount() == 0 || this.menu.lubricantAmount() == 0) {
            guiGraphics.blit(K3GuiText.UTILITY, this.leftPos - 16, this.topPos + 66, 8, 16, 16, 16, 256, 256);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int height, HbmFluidDefinition fluid, int width) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }

    private List<Component> splitTooltip(String key) {
        String translated = Component.translatable(key).getString();
        String[] parts = translated.split("\\$");
        ArrayList<Component> lines = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                lines.add(Component.literal(part));
            }
        }
        return lines;
    }

    private void sendSlider(int slider) {
        PacketDistributor.sendToServer(new GasTurbineControlPayload(this.menu.blockPos(), slider, true, false, false, 0, false));
    }

    private void sendAuto(boolean autoMode) {
        PacketDistributor.sendToServer(new GasTurbineControlPayload(this.menu.blockPos(), 0, false, autoMode, true, 0, false));
    }

    private void sendState(int state) {
        PacketDistributor.sendToServer(new GasTurbineControlPayload(this.menu.blockPos(), 0, false, false, false, state, true));
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
