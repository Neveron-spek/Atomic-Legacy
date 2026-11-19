package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ReactorControlBlockEntity;
import com.atomiclegacy.inventory.container.ReactorControlMenu;
import com.atomiclegacy.network.ReactorControlPayload;
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

/** Exact GUIReactorControl layout and curve definitions, using NeoForge widgets and payloads. */
public final class ReactorControlScreen extends AbstractContainerScreen<ReactorControlMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_reactor_control.png");
    private final EditBox[] fields = new EditBox[4];

    public ReactorControlScreen(ReactorControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        addField(0, 35, 38, 3, Integer.toString(this.menu.levelUpper()));
        addField(1, 65, 38, 3, Integer.toString(this.menu.levelLower()));
        addField(2, 35, 49, 4, Integer.toString(this.menu.heatUpper() / 50));
        addField(3, 65, 49, 4, Integer.toString(this.menu.heatLower() / 50));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawCurve(graphics);
        // [parity e2] GUIReactorControl NumberDisplay(6 / 66 / 126, 20, 0x08FF00) with 3 / 4 / 3 digits
        this.displays[0].drawNumber(graphics, this.leftPos, this.topPos, this.menu.levelPercent());
        this.displays[1].drawNumber(graphics, this.leftPos, this.topPos, this.menu.flux());
        this.displays[2].drawNumber(graphics, this.leftPos, this.topPos, this.menu.temperature());
    }

    private final M2NumberDisplay[] displays = {
            new M2NumberDisplay(6, 20, 0x08FF00).setDigitLength(3),
            new M2NumberDisplay(66, 20, 0x08FF00).setDigitLength(4),
            new M2NumberDisplay(126, 20, 0x08FF00).setDigitLength(3)
    };

    /** [parity e2] keyTyped: a focused text field swallows the key (the inventory key must not close the GUI). */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 && this.getFocused() instanceof EditBox box && box.isFocused()) {
            box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [parity e2] save button: guiLeft + 33 .. +58, guiTop + 59 < y <= guiTop + 69, any mouse button
        if (inside(mouseX, mouseY, 33, 59, 58, 10)) {
            sendParameters();
            return true;
        }
        {
            for (int function = 0; function < 3; function++) {
                if (inside(mouseX, mouseY, 7, 37 + function * 11, 22, 10)) {
                    PacketDistributor.sendToServer(ReactorControlPayload.function(this.menu.blockPos(), function));
                    click();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void addField(int index, int x, int y, int maxLength, String value) {
        // [parity e2] GuiTextField(x, y, 26, 7), no input filter (non-digits are reset to 0 on save)
        EditBox field = new EditBox(this.font, this.leftPos + x, this.topPos + y, 26, 7, Component.empty());
        field.setBordered(false);
        field.setTextColor(0x08FF00);
        field.setTextColorUneditable(-1);
        field.setMaxLength(maxLength);
        field.setValue(value);
        this.fields[index] = field;
        addRenderableWidget(field);
    }

    private void sendParameters() {
        int levelUpper = parse(0, 100, 1);
        int levelLower = parse(1, 100, 1);
        int heatUpper = parse(2, 1000, 50);
        int heatLower = parse(3, 1000, 50);
        this.fields[0].setValue(Integer.toString(levelUpper));
        this.fields[1].setValue(Integer.toString(levelLower));
        this.fields[2].setValue(Integer.toString(heatUpper / 50));
        this.fields[3].setValue(Integer.toString(heatLower / 50));
        PacketDistributor.sendToServer(ReactorControlPayload.parameters(this.menu.blockPos(), levelUpper, levelLower, heatUpper, heatLower));
        click();
    }

    private int parse(int index, int max, int multiplier) {
        // [parity e2] NumberUtils.isDigits(text) ? (int) clamp(parseDouble(text), 0, max) * mod : 0
        String text = this.fields[index].getValue();
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) {
            return 0;
        }
        try {
            return (int) Mth.clamp(Double.parseDouble(text), 0, max) * multiplier;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void drawCurve(GuiGraphics graphics) {
        int previousX = this.leftPos + 128;
        int previousY = curveY(0);
        for (int i = 1; i < 40; i++) {
            int currentX = this.leftPos + 128 + i;
            int currentY = curveY(i * 1250);
            drawLine(graphics, previousX, previousY, currentX, currentY, 0xFF08FF00);
            previousX = currentX;
            previousY = currentY;
        }
    }

    private int curveY(int heat) {
        double target = switch (this.menu.function()) {
            case 1 -> quadratic(heat);
            case 2 -> logarithmic(heat);
            default -> linear(heat);
        };
        if (!Double.isFinite(target)) {
            target = 0.0D;
        }
        return this.topPos + 39 + Mth.clamp((int) Math.round(target / 100.0D * 28.0D), 0, 28);
    }

    private double linear(int heat) {
        int low = this.menu.heatLower();
        int high = this.menu.heatUpper();
        if (high == low) return 0.0D;
        return (heat - low) * ((this.menu.levelUpper() - this.menu.levelLower()) / (double) (high - low)) + this.menu.levelLower();
    }

    private double quadratic(int heat) {
        int low = this.menu.heatLower();
        int high = this.menu.heatUpper();
        if (high == low) return 0.0D;
        return Math.pow((heat - low) / (double) (high - low), 2.0D) * (this.menu.levelUpper() - this.menu.levelLower()) + this.menu.levelLower();
    }

    private double logarithmic(int heat) {
        int low = this.menu.heatLower();
        int high = this.menu.heatUpper();
        if (high == low) return 0.0D;
        return Math.pow((heat - high) / (double) (low - high), 2.0D) * (this.menu.levelLower() - this.menu.levelUpper()) + this.menu.levelUpper();
    }



    private void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int stepX = x0 < x1 ? 1 : -1;
        int stepY = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 2, y0 + 2, color);
            if (x0 == x1 && y0 == y1) break;
            int twice = error * 2;
            if (twice >= dy) {
                error += dy;
                x0 += stepX;
            }
            if (twice <= dx) {
                error += dx;
                y0 += stepY;
            }
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY && this.topPos + y + height >= mouseY;
    }

    private void click() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
