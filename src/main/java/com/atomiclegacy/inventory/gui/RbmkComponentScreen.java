package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.RbmkFuelRodItem;
import com.atomiclegacy.inventory.container.RbmkComponentMenu;
import com.atomiclegacy.network.RbmkAutoControlPayload;
import com.atomiclegacy.network.RbmkConsoleControlPayload;
import com.atomiclegacy.network.RbmkControlRodPayload;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RbmkComponentScreen extends AbstractContainerScreen<RbmkComponentMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 186;
    private static final int AUTOLOADER_HEIGHT = 182;
    private static final int CONSOLE_WIDTH = 244;
    private static final int CONSOLE_HEIGHT = 172;
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int[] CONTROL_GROUP_COLORS = {
            0xFFFF3030,
            0xFFFFFF30,
            0xFF30FF30,
            0xFF3090FF,
            0xFFFF70FF
    };
    private final boolean[] consoleSelection = new boolean[RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT];
    private EditBox consoleLevelField;
    private EditBox[] autoControlFields;
    private boolean az5CoverClosed = true;
    private long lastAz5Press;

    public RbmkComponentScreen(RbmkComponentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = menu.kind() == RbmkComponentBlock.Kind.CONSOLE ? CONSOLE_WIDTH : WIDTH;
        this.imageHeight = menu.kind() == RbmkComponentBlock.Kind.CONSOLE ? CONSOLE_HEIGHT : menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER ? AUTOLOADER_HEIGHT : HEIGHT;
        this.inventoryLabelY = menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER ? this.imageHeight - 96 + 2 : 92;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            this.consoleLevelField = new EditBox(this.font, this.leftPos + 9, this.topPos + 84, 35, 9, Component.empty());
            this.consoleLevelField.setTextColor(0x00FF00);
            this.consoleLevelField.setTextColorUneditable(0x008000);
            this.consoleLevelField.setBordered(false);
            this.consoleLevelField.setMaxLength(3);
            // [parity e2] GUIRBMKConsole never pre-filled the field (an empty field makes "save" a no-op).
            this.addRenderableWidget(this.consoleLevelField);
            return;
        }
        if (this.menu.kind().isControl()) {
            if (this.menu.kind().isAutomaticControl()) {
                initAutoControlWidgets();
            } else {
                initManualControlWidgets();
            }
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.BOILER) {
            this.addRenderableWidget(new ClickArea(this.leftPos + 33, this.topPos + 21, 20, 64, () -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                }
            }));
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            addAutoloaderButton(4, 74, 36);
            addAutoloaderButton(5, 90, 36);
        }
    }

    private void initManualControlWidgets() {
        for (int index = 0; index < 5; index++) {
            final int row = index;
            this.addRenderableWidget(new ClickArea(this.leftPos + 118, this.topPos + 26 + row * 11, 30, 10,
                    () -> sendManualControl(RbmkControlRodPayload.ACTION_SET_LEVEL, 100 - row * 25)));
            this.addRenderableWidget(new ClickArea(this.leftPos + 28, this.topPos + 26 + row * 11, 12, 10,
                    () -> sendManualControl(RbmkControlRodPayload.ACTION_ASSIGN_COLOR, row)));
        }
    }

    private void sendManualControl(int action, int value) {
        PacketDistributor.sendToServer(new RbmkControlRodPayload(this.menu.pos(), action, value));
    }

    private void initAutoControlWidgets() {
        this.autoControlFields = new EditBox[4];
        int[] values = {
                this.menu.autoLevelUpper(),
                this.menu.autoLevelLower(),
                this.menu.autoHeatUpper(),
                this.menu.autoHeatLower()
        };
        for (int index = 0; index < this.autoControlFields.length; index++) {
            EditBox field = new EditBox(this.font, this.leftPos + 30, this.topPos + 27 + 11 * index, 26, 6, Component.empty());
            field.setTextColor(0xFFFFFF);
            field.setTextColorUneditable(0xFFFFFF);
            field.setBordered(false);
            field.setMaxLength(index < 2 ? 3 : 4);
            field.setValue(String.valueOf(values[index]));
            this.autoControlFields[index] = field;
            this.addRenderableWidget(field);
        }
        for (int function = 0; function < 3; function++) {
            final int selectedFunction = function;
            this.addRenderableWidget(new ClickArea(this.leftPos + 61, this.topPos + 48 + function * 11, 22, 10,
                    () -> sendAutoControl(selectedFunction, false)));
        }
        this.addRenderableWidget(new ClickArea(this.leftPos + 28, this.topPos + 70, 30, 10,
                () -> sendAutoControl(this.menu.autoFunction(), true)));
    }

    private void addAutoloaderButton(int id, int x, int y) {
        this.addRenderableWidget(new ClickArea(this.leftPos + x, this.topPos + y, 12, 12, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }));
    }

    private void sendAutoControl(int function, boolean readFields) {
        int levelUpper = this.menu.autoLevelUpper();
        int levelLower = this.menu.autoLevelLower();
        int heatUpper = this.menu.autoHeatUpper();
        int heatLower = this.menu.autoHeatLower();
        if (readFields && this.autoControlFields != null && this.autoControlFields.length == 4) {
            levelUpper = parseAutoField(0, 100);
            levelLower = parseAutoField(1, 100);
            heatUpper = parseAutoField(2, 9999);
            heatLower = parseAutoField(3, 9999);
        }
        PacketDistributor.sendToServer(new RbmkAutoControlPayload(
                this.menu.pos(),
                function,
                levelUpper,
                levelLower,
                heatUpper,
                heatLower,
                readFields
        ));
    }

    private int parseAutoField(int index, int max) {
        if (this.autoControlFields == null || index < 0 || index >= this.autoControlFields.length) {
            return 0;
        }
        // GUIRBMKControlAuto: NumberUtils.isNumber(text) ? (int) clamp(parseDouble, 0, max) : "0"
        String value = this.autoControlFields[index].getValue();
        if (!org.apache.commons.lang3.math.NumberUtils.isCreatable(value)) {
            this.autoControlFields[index].setValue("0");
            return 0;
        }
        int parsed;
        try {
            parsed = (int) Math.max(0.0D, Math.min(max, Double.parseDouble(value)));
        } catch (NumberFormatException ignored) {
            this.autoControlFields[index].setValue("0");
            return 0;
        }
        this.autoControlFields[index].setValue(String.valueOf(parsed));
        return parsed;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            renderConsoleGrid(guiGraphics, mouseX, mouseY);
            renderConsoleControls(guiGraphics);
        } else {
            renderRbmkOverlays(guiGraphics, texture);
            if (this.menu.kind().acceptsFuel()) {
                renderRodWarnings(guiGraphics);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            renderConsoleLabels(guiGraphics);
            return;
        }
        int titleColor = this.menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER ? 0xFFFFFF : 4210752;
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, titleColor, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        if (this.menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            String cycle = this.menu.autoloaderCycle() + "%";
            guiGraphics.drawString(this.font, cycle, this.imageWidth / 2 - this.font.width(cycle) / 2, 23, 0x00FF00, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            List<Component> tooltip = consoleTooltip(x, y);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
                return;
            }
        } else {
            List<Component> tooltip = rbmkTooltip(x, y);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
                return;
            }
        }
        super.renderTooltip(guiGraphics, x, y);
    }

    private void renderRbmkOverlays(GuiGraphics graphics, ResourceLocation texture) {
        if (this.menu.kind().acceptsFuel() && !this.menu.getSlot(0).getItem().isEmpty()) {
            renderFuelRodOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.BOILER) {
            renderBoilerOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.HEATER) {
            renderHeaterOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            renderOutgasserOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.CONTROL
                || this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_MOD
                || this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_REASIM) {
            renderManualControlOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_AUTO
                || this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO) {
            renderAutoControlOverlay(graphics, texture);
        }
    }

    private void renderFuelRodOverlay(GuiGraphics graphics, ResourceLocation texture) {
        ItemStack rod = this.menu.getSlot(0).getItem();
        if (!(rod.getItem() instanceof RbmkFuelRodItem)) {
            return;
        }
        blit(graphics, texture, this.leftPos + 34, this.topPos + 21, 176, 0, 18, 67);
        // [parity e2] GUIRBMKRod truncates: (int) (depletion * 67), (int) (xenon * 58)
        int depletion = clampPixels((int) (RbmkFuelRodItem.depletion(rod) * 67.0D), 67);
        if (depletion > 0) {
            blit(graphics, texture, this.leftPos + 34, this.topPos + 21, 194, 0, 18, depletion);
        }
        int xenon = clampPixels((int) (RbmkFuelRodItem.xenon(rod) * 58.0D), 58);
        if (xenon > 0) {
            blit(graphics, texture, this.leftPos + 126, this.topPos + 82 - xenon, 212, 58 - xenon, 14, xenon);
        }
    }

    private void renderBoilerOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int water = clampPixels(this.menu.water() * 58 / RbmkComponentBlockEntity.WATER_CAPACITY, 58);
        if (water > 0) {
            blit(graphics, texture, this.leftPos + 126, this.topPos + 82 - water, 176, 58 - water, 14, water);
        }
        int steam = clampPixels(this.menu.steam() * 22 / RbmkComponentBlockEntity.STEAM_CAPACITY, 24);
        if (steam > 0) {
            if (steam > 0) steam++;
            if (steam > 22) steam++;
            blit(graphics, texture, this.leftPos + 91, this.topPos + 65 - steam, 190, 24 - steam, 4, steam);
        }
        int steamU = switch (this.menu.steamCompression()) {
            case 1 -> 208;
            case 2 -> 222;
            case 3 -> 236;
            default -> 194;
        };
        blit(graphics, texture, this.leftPos + 36, this.topPos + 24, steamU, 0, 14, 58);
    }

    private void renderHeaterOverlay(GuiGraphics graphics, ResourceLocation texture) {
        // [parity e2] feed/steam.renderTank(guiLeft + 68 / 126, guiTop + 82, z, 14, 58) with the real tank types
        RbmkComponentBlockEntity heater = console();
        E2TankGui.renderTank(graphics, this.leftPos + 68, this.topPos + 82, 14, 58,
                heater != null ? heater.heaterInputTank().type() : fluid("coolant"),
                this.menu.heaterInput(), RbmkComponentBlockEntity.HEATER_TANK_CAPACITY);
        E2TankGui.renderTank(graphics, this.leftPos + 126, this.topPos + 82, 14, 58,
                heater != null ? heater.heaterOutputTank().type() : fluid("coolant_hot"),
                this.menu.heaterOutput(), RbmkComponentBlockEntity.HEATER_TANK_CAPACITY);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(graphics, texture, this.leftPos + 72, this.topPos + 72, 176, 0, 10, 10);
        blit(graphics, texture, this.leftPos + 130, this.topPos + 72, 186, 0, 10, 10);
    }

    private void renderOutgasserOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int progress = clampPixels(this.menu.outgasserProgress() * 13 / RbmkComponentBlockEntity.OUTGASSER_DURATION, 13);
        if (progress > 0) {
            blit(graphics, texture, this.leftPos + 82, this.topPos + 50, 176, 0, progress, 6);
        }
        int gas = clampPixels(this.menu.outgasserGas() * 42 / RbmkComponentBlockEntity.OUTGASSER_GAS_CAPACITY, 42);
        if (gas > 0) {
            blit(graphics, texture, this.leftPos + 115, this.topPos + 66 - gas, 188, 42 - gas, 10, gas);
        }
    }

    private void renderManualControlOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int height = clampPixels((int) (56 * (1D - controlLevelValue())), 56);
        if (height > 0) {
            blit(graphics, texture, this.leftPos + 75, this.topPos + 29, 176, 56 - height, 8, height);
        }
        // [parity e2] GUIRBMKControl: power lamp of the powered (ReaSim) rod
        RbmkComponentBlockEntity rod = console();
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_REASIM && rod != null) {
            blit(graphics, texture, this.leftPos + 87, this.topPos + 21, 196, rod.controlHasPower() ? 16 : 0, 16, 16);
        }
        int color = this.menu.colorGroup();
        if (color >= 0 && color < 5) {
            blit(graphics, texture, this.leftPos + 28, this.topPos + 26 + color * 11, 184, color * 10, 12, 10);
        }
    }

    private void renderAutoControlOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int height = clampPixels((int) (56 * (1D - controlLevelValue())), 56);
        if (height > 0) {
            blit(graphics, texture, this.leftPos + 124, this.topPos + 29, 176, 56 - height, 8, height);
        }
        // [parity e2] GUIRBMKControlAuto: power lamp of the powered (ReaSim) rod
        RbmkComponentBlockEntity rod = console();
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO && rod != null) {
            blit(graphics, texture, this.leftPos + 136, this.topPos + 21, 210, rod.controlHasPower() ? 16 : 0, 16, 16);
        }
        int function = Math.floorMod(this.menu.autoFunction(), 3);
        blit(graphics, texture, this.leftPos + 59, this.topPos + 27, 184, function * 19, 26, 19);
    }

    /** [parity e2] rod.level as the reference GUIs read it ((int) (56 * (1D - rod.level)) truncates). */
    private double controlLevelValue() {
        RbmkComponentBlockEntity rod = console();
        return rod != null ? rod.controlLevel() : this.menu.controlLevel() / 1000.0D;
    }

    private void renderConsoleGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return;
        }
        ResourceLocation texture = texture();
        int baseX = this.leftPos + 86;
        int baseY = this.topPos + 11;
        for (int index = 0; index < RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT; index++) {
            int kindOrdinal = console.consoleKind(index);
            if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
                continue;
            }
            RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
            int x = baseX + index % RbmkComponentBlockEntity.CONSOLE_GRID_SIZE * 10;
            int y = baseY + index / RbmkComponentBlockEntity.CONSOLE_GRID_SIZE * 10;
            blit(guiGraphics, texture, x, y, consoleColumnOffset(kind), 172, 10, 10);
            int maxHeat = Math.max(1, console.consoleMaxHeat(index));
            int heatHeight = clampPixels((int) Math.ceil((console.consoleHeat(index) - 20) * 10.0D / maxHeat), 10);
            if (heatHeight > 0) {
                blit(guiGraphics, texture, x, y + 10 - heatHeight, 0, 192 - heatHeight, 10, heatHeight);
            }
            if (kind.isControl()) {
                int colorGroup = console.consoleColorGroup(index);
                if (colorGroup >= 0 && colorGroup < CONTROL_GROUP_COLORS.length && isManualControlKind(kind)) {
                    blit(guiGraphics, texture, x, y, colorGroup * 10, 202, 10, 10);
                }
                int rodHeight = 8 - clampPixels((int) Math.ceil(console.consoleControl(index) * 8.0D / 100.0D), 8);
                if (rodHeight > 0) {
                    blit(guiGraphics, texture, x + 4, y + 1, 24, 183, 2, rodHeight);
                }
            } else if (kind.acceptsFuel() && console.consoleFuelMaxHeat(index) > 0) {
                // GUIRBMKConsole used the rod core's excess heat above the
                // 20°C baseline, exactly like the column heat strip.  Using
                // the raw core temperature makes a cold rod appear loaded.
                int coreHeight = clampPixels((int) Math.ceil(
                        (console.consoleFuelCoreHeat(index) - 20) * 8.0D
                                / Math.max(1, console.consoleFuelMaxHeat(index))), 8);
                // Legacy GUIRBMKConsole drew this strip from the rod's
                // enrichment (remaining fuel), not from spent fraction.
                int enrichment = 1000 - console.consoleFuelDepletion(index);
                int fuelHeight = clampPixels((int) Math.ceil(enrichment * 8.0D / 1000.0D), 8);
                // consoleFuelXenon is normalized xenon * 1000; old GUI used
                // the legacy percentage (xenon * 8 / 100).
                int xenon = clampPixels((int) Math.ceil(console.consoleFuelXenon(index) * 8.0D / 1000.0D), 8);
                if (coreHeight > 0) blit(guiGraphics, texture, x + 1, y + 9 - coreHeight, 11, 191 - coreHeight, 2, coreHeight);
                if (fuelHeight > 0) blit(guiGraphics, texture, x + 4, y + 9 - fuelHeight, 14, 191 - fuelHeight, 2, fuelHeight);
                if (xenon > 0) blit(guiGraphics, texture, x + 7, y + 9 - xenon, 17, 191 - xenon, 2, xenon);
            } else if (kind == RbmkComponentBlock.Kind.BOILER) {
                int water = clampPixels((int) Math.ceil(console.consoleWater(index) * 8.0D / Math.max(1, console.consoleMaxWater(index))), 8);
                int steam = clampPixels((int) Math.ceil(console.consoleSteam(index) * 8.0D / Math.max(1, console.consoleMaxSteam(index))), 8);
                if (water > 0) blit(guiGraphics, texture, x + 1, y + 9 - water, 41, 191 - water, 3, water);
                if (steam > 0) blit(guiGraphics, texture, x + 6, y + 9 - steam, 46, 191 - steam, 3, steam);
                int markerY = 1 + clampPixels(console.consoleSteamType(index), 3) * 2;
                blit(guiGraphics, texture, x + 4, y + markerY, 44, 183 + markerY - 1, 2, 2);
            } else if (kind == RbmkComponentBlock.Kind.HEATER) {
                int cold = clampPixels((int) Math.ceil(console.consoleHeaterInput(index) * 8.0D / Math.max(1, console.consoleHeaterMax(index))), 8);
                int hot = clampPixels((int) Math.ceil(console.consoleHeaterOutput(index) * 8.0D / Math.max(1, console.consoleHeaterMax(index))), 8);
                if (cold > 0) blit(guiGraphics, texture, x + 1, y + 9 - cold, 131, 191 - cold, 3, cold);
                if (hot > 0) blit(guiGraphics, texture, x + 6, y + 9 - hot, 136, 191 - hot, 3, hot);
            }
            if (this.consoleSelection[index]) {
                blit(guiGraphics, texture, x, y, 0, 192, 10, 10);
            }
            // [parity e2] GUIRBMKConsole has no hover highlight on the column grid.
        }
    }

    private void renderConsoleControls(GuiGraphics guiGraphics) {
        ResourceLocation texture = texture();
        RbmkComponentBlockEntity console = console();
        if (console != null) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 2; column++) {
                    int slot = row * 2 + column;
                    blit(guiGraphics, texture, this.leftPos + 6 + 40 * column, this.topPos + 8 + 21 * row,
                            console.consoleScreenType(slot) * 18, 238, 18, 18);
                }
            }
            renderFluxGraph(guiGraphics, console);
            // [parity e2] drawn in the background layer with absolute coordinates like GUIRBMKConsole
            // (renderLabels is already translated by leftPos/topPos and misplaced the scale).
            renderFluxScale(guiGraphics, console);
        }
        if (az5CoverClosed) {
            blit(guiGraphics, texture, this.leftPos + 30, this.topPos + 138, 228, 172, 28, 28);
        }
        // [parity e2] the colour group buttons are part of gui_rbmk_console.png; the reference draws nothing over them.
    }

    private void renderConsoleLabels(GuiGraphics guiGraphics) {
        // [parity e2] GUIRBMKConsole has no foreground layer; the flux scale is drawn in renderConsoleControls.
    }

    private void renderFluxScale(GuiGraphics graphics, RbmkComponentBlockEntity console) {
        int highest = Integer.MIN_VALUE;
        int lowest = Integer.MAX_VALUE;
        for (int index = 0; index < console.consoleFluxHistorySize(); index++) {
            int value = console.consoleFluxHistory(index);
            highest = Math.max(highest, value);
            lowest = Math.min(lowest, value);
        }
        if (highest == Integer.MIN_VALUE) {
            highest = console.consoleTotalFlux();
            lowest = 0;
        }
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        // [parity e2] (int) ((guiLeft + 80 - width * 0.5) / 0.5) and (int) ((guiTop + 133 - FONT_HEIGHT * 0.5) / 0.5)
        String high = Integer.toString(highest);
        String low = Integer.toString(lowest);
        int lowY = (this.topPos + 133) * 2 - this.font.lineHeight;
        graphics.drawString(this.font, high, (this.leftPos + 8) * 2, (this.topPos + 98) * 2, 0x00FF00, false);
        graphics.drawString(this.font, high, (this.leftPos + 80) * 2 - this.font.width(high), (this.topPos + 98) * 2, 0x00FF00, false);
        graphics.drawString(this.font, low, (this.leftPos + 8) * 2, lowY, 0x00FF00, false);
        graphics.drawString(this.font, low, (this.leftPos + 80) * 2 - this.font.width(low), lowY, 0x00FF00, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            // [parity e2] GuiTextField.mouseClicked: any click outside the field drops its focus.
            if (this.consoleLevelField != null && !this.consoleLevelField.isMouseOver(mouseX, mouseY)) {
                this.consoleLevelField.setFocused(false);
                if (this.getFocused() == this.consoleLevelField) {
                    this.setFocused(null);
                }
            }
            if (handleConsoleClick((int) mouseX, (int) mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** [parity e2] keyTyped: a focused text field swallows the key (the inventory key must not close the GUI). */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 && this.getFocused() instanceof EditBox box && box.isFocused()) {
            box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean handleConsoleClick(int mouseX, int mouseY, int button) {
        int gridX = this.leftPos + 86;
        int gridY = this.topPos + 11;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) {
                int slot = row * 2 + column;
                if (inside(mouseX, mouseY, 6 + 40 * column, 8 + 21 * row, 18, 18)) {
                    PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), RbmkConsoleControlPayload.ACTION_TOGGLE_SCREEN, slot, new int[0]));
                    press(0.5F);
                    return true;
                }
                if (inside(mouseX, mouseY, 24 + 40 * column, 8 + 21 * row, 18, 18)) {
                    PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), RbmkConsoleControlPayload.ACTION_ASSIGN_SCREEN, slot, selectedIndices()));
                    press(0.75F);
                    return true;
                }
            }
        }
        if (mouseX >= gridX && mouseX < gridX + 150 && mouseY > gridY && mouseY <= gridY + 150) {
            int index = (mouseX - gridX) / 10 + (mouseY - gridY) / 10 * RbmkComponentBlockEntity.CONSOLE_GRID_SIZE;
            RbmkComponentBlockEntity console = console();
            if (console != null && index >= 0 && index < this.consoleSelection.length && console.consoleKind(index) >= 0) {
                this.consoleSelection[index] = !this.consoleSelection[index];
                press(0.75F + (this.consoleSelection[index] ? 0.25F : 0.0F));
                return true;
            }
        }
        if (inside(mouseX, mouseY, 72, 70, 10, 10)) {
            java.util.Arrays.fill(this.consoleSelection, false);
            press(0.5F);
            return true;
        }
        if (inside(mouseX, mouseY, 61, 70, 10, 10)) {
            selectAllControls();
            press(1.5F);
            return true;
        }
        if (inside(mouseX, mouseY, 70, 82, 12, 12)) {
            sendConsoleControl(RbmkConsoleControlPayload.ACTION_CYCLE_COMPRESSOR, 0);
            press(1.0F);
        }
        for (int color = 0; color < CONTROL_GROUP_COLORS.length; color++) {
            if (inside(mouseX, mouseY, 6 + color * 11, 70, 10, 10)) {
                if (button == 0) {
                    selectColorGroup(color);
                }
                if (button == 1) {
                    sendConsoleControl(RbmkConsoleControlPayload.ACTION_ASSIGN_COLOR, color);
                }
                press(0.8F + color * 0.1F);
                return true;
            }
        }
        if (inside(mouseX, mouseY, 30, 138, 28, 28)) {
            if (az5CoverClosed) {
                az5CoverClosed = false;
                uiSound(com.atomiclegacy.registry.HbmSoundEvents.RBMK_AZ5_COVER.get(), 0.5F);
            } else if (lastAz5Press + 3000L < System.currentTimeMillis()) {
                uiSound(com.atomiclegacy.registry.HbmSoundEvents.RBMK_SHUTDOWN.get(), 1.0F);
                lastAz5Press = System.currentTimeMillis();
                PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), RbmkConsoleControlPayload.ACTION_AZ5, 0, new int[0]));
                press(1.0F);
            }
            return true;
        }
        if (inside(mouseX, mouseY, 48, 82, 12, 12)) {
            // NumberUtils.isNumber(text) ? clamp(parse, 0, 100) : return (nothing is sent)
            int level = parseConsoleLevel();
            if (level < 0) {
                return true;
            }
            sendConsoleControl(RbmkConsoleControlPayload.ACTION_SET_CONTROL, level);
            press(1.0F);
        }
        return false;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        int left = this.leftPos + x;
        int top = this.topPos + y;
        // 1.7.10 GUIs (drawCustomInfoStat / mouseClicked): x in [x, x + w), y in (y, y + h].
        return mouseX >= left && mouseX < left + width && mouseY > top && mouseY <= top + height;
    }

    private void selectAllControls() {
        java.util.Arrays.fill(this.consoleSelection, false);
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return;
        }
        for (int i = 0; i < this.consoleSelection.length; i++) {
            int kindOrdinal = console.consoleKind(i);
            if (kindOrdinal >= 0 && kindOrdinal < RbmkComponentBlock.Kind.values().length
                    && isManualControlKind(RbmkComponentBlock.Kind.values()[kindOrdinal])) {
                this.consoleSelection[i] = true;
            }
        }
    }

    private void selectColorGroup(int colorGroup) {
        java.util.Arrays.fill(this.consoleSelection, false);
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return;
        }
        for (int i = 0; i < this.consoleSelection.length; i++) {
            int kindOrdinal = console.consoleKind(i);
            if (kindOrdinal >= 0 && kindOrdinal < RbmkComponentBlock.Kind.values().length
                    && isManualControlKind(RbmkComponentBlock.Kind.values()[kindOrdinal])
                    && console.consoleColorGroup(i) == colorGroup) {
                this.consoleSelection[i] = true;
            }
        }
    }

    private void sendConsoleControl(int action, int value) {
        PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), action, value, selectedIndices()));
    }

    private static boolean isManualControlKind(RbmkComponentBlock.Kind kind) {
        return kind == RbmkComponentBlock.Kind.CONTROL
                || kind == RbmkComponentBlock.Kind.CONTROL_MOD
                || kind == RbmkComponentBlock.Kind.CONTROL_REASIM;
    }

    private int[] selectedIndices() {
        int count = selectedCount();
        int[] indices = new int[count];
        int cursor = 0;
        for (int i = 0; i < this.consoleSelection.length; i++) {
            if (this.consoleSelection[i]) {
                indices[cursor++] = i;
            }
        }
        return indices;
    }

    private int selectedCount() {
        int count = 0;
        for (boolean selected : this.consoleSelection) {
            if (selected) {
                count++;
            }
        }
        return count;
    }

    private int parseConsoleLevel() {
        if (this.consoleLevelField == null) {
            return -1;
        }
        String text = this.consoleLevelField.getValue();
        if (!org.apache.commons.lang3.math.NumberUtils.isCreatable(text)) {
            return -1;
        }
        double parsed;
        try {
            parsed = Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return -1;
        }
        int level = (int) Math.max(0.0D, Math.min(100.0D, parsed));
        this.consoleLevelField.setValue(level + "");
        return level;
    }

    private static void press(float pitch) {
        uiSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), pitch);
    }

    private static void uiSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, pitch));
    }

    /** TileEntityRBMKConsole.ColumnType for a column kind. */
    private static String consoleColumnType(RbmkComponentBlock.Kind kind) {
        return switch (kind) {
            case FUEL_ROD, FUEL_ROD_MOD -> "FUEL";
            case FUEL_ROD_REASIM, FUEL_ROD_REASIM_MOD -> "FUEL_SIM";
            case CONTROL, CONTROL_MOD, CONTROL_REASIM -> "CONTROL";
            case CONTROL_AUTO, CONTROL_REASIM_AUTO -> "CONTROL_AUTO";
            case BOILER -> "BOILER";
            case MODERATOR -> "MODERATOR";
            case ABSORBER -> "ABSORBER";
            case REFLECTOR -> "REFLECTOR";
            case OUTGASSER -> "OUTGASSER";
            case STORAGE -> "STORAGE";
            case COOLER -> "COOLER";
            case HEATER -> "HEATEX";
            default -> "BLANK";
        };
    }

    private static Component fluidName(int oldId) {
        return HbmFluids.byOldId(oldId).map(f -> (Component) Component.translatable(f.translationKey()))
                .orElse(Component.literal("NONE"));
    }

    private static final String[] GROUP_NAMES = {"red", "yellow", "green", "blue", "purple"};
    private static final net.minecraft.ChatFormatting[] GROUP_FORMATS = {
            net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.GREEN,
            net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.LIGHT_PURPLE};

    /** GUIRBMKConsole.drawScreen tooltips and RBMKColumn.getFancyStats, line for line. */
    private List<Component> consoleTooltip(int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return tooltip;
        }
        int baseX = this.leftPos + 86;
        int baseY = this.topPos + 11;
        if (baseX <= mouseX && baseX + 150 > mouseX && baseY < mouseY) {
            int index = (mouseX - baseX) / 10 + (mouseY - baseY) / 10 * RbmkComponentBlockEntity.CONSOLE_GRID_SIZE;
            // 1.7.10 used index > 0 here, so the top-left cell never showed a tooltip.
            if (index > 0 && index < RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT) {
                int kindOrdinal = console.consoleKind(index);
                if (kindOrdinal >= 0 && kindOrdinal < RbmkComponentBlock.Kind.values().length) {
                    columnStats(tooltip, console, index, RbmkComponentBlock.Kind.values()[kindOrdinal]);
                    return tooltip;
                }
            }
        }
        if (inside(mouseX, mouseY, 61, 70, 10, 10)) {
            tooltip.add(Component.literal("Select all control rods"));
        }
        if (inside(mouseX, mouseY, 72, 70, 10, 10)) {
            tooltip.add(Component.literal("Deselect all"));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) {
                int id = row * 2 + column + 1;
                if (inside(mouseX, mouseY, 6 + 40 * column, 8 + 21 * row, 18, 18)) {
                    tooltip.add(Component.translatable("rbmk.console." + consoleScreenKey(console.consoleScreenType(id - 1)), id)
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));
                }
                if (inside(mouseX, mouseY, 24 + 40 * column, 8 + 21 * row, 18, 18)) {
                    tooltip.add(Component.translatable("rbmk.console.assign", id));
                }
            }
        }
        String[] groups = {"red", "yellow", "green", "blue", "purple"};
        for (int k = 0; k < 5; k++) {
            if (inside(mouseX, mouseY, 6 + 11 * k, 70, 10, 10)) {
                tooltip.add(Component.literal("Left click: Select " + groups[k] + " group").withStyle(GROUP_FORMATS[k]));
                tooltip.add(Component.literal("Right click: Assign " + groups[k] + " group").withStyle(GROUP_FORMATS[k]));
            }
        }
        if (inside(mouseX, mouseY, 70, 82, 12, 12)) {
            tooltip.add(Component.literal("Cycle steam channel compressor setting"));
        }
        return tooltip;
    }

    private static void columnStats(List<Component> tooltip, RbmkComponentBlockEntity console, int index, RbmkComponentBlock.Kind kind) {
        String type = consoleColumnType(kind);
        tooltip.add(Component.literal(type));
        tooltip.add(Component.translatable("rbmk.heat", (console.consoleTip(index, 0) / 10D) + "\u00b0C")
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        switch (type) {
            case "FUEL", "FUEL_SIM" -> {
                tooltip.add(Component.translatable("rbmk.rod.depletion", (console.consoleTip(index, 1) / 1000D) + "%")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
                tooltip.add(Component.translatable("rbmk.rod.xenon", (console.consoleTip(index, 2) / 1000D) + "%")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("rbmk.rod.coreTemp", (console.consoleTip(index, 3) / 10D) + "\u00b0C")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
                tooltip.add(Component.translatable("rbmk.rod.skinTemp", (console.consoleTip(index, 4) / 10D) + "\u00b0C",
                        (console.consoleTip(index, 5) / 10D) + "\u00b0C").withStyle(net.minecraft.ChatFormatting.RED));
            }
            case "BOILER" -> {
                tooltip.add(Component.translatable("rbmk.boiler.water", console.consoleWater(index), console.consoleMaxWater(index))
                        .withStyle(net.minecraft.ChatFormatting.BLUE));
                tooltip.add(Component.translatable("rbmk.boiler.steam", console.consoleSteam(index), console.consoleMaxSteam(index))
                        .withStyle(net.minecraft.ChatFormatting.WHITE));
                tooltip.add(Component.translatable("rbmk.boiler.type", fluidName(console.consoleTip(index, 7)))
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            case "CONTROL", "CONTROL_AUTO" -> {
                if (type.equals("CONTROL")) {
                    int col = console.consoleColorGroup(index);
                    if (col >= 0 && col < GROUP_NAMES.length) {
                        tooltip.add(Component.translatable("rbmk.control." + GROUP_NAMES[col]).withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                }
                tooltip.add(Component.translatable("rbmk.control.level", console.consoleTip(index, 6) + "%")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            case "HEATEX" -> {
                tooltip.add(Component.empty().append(fluidName(console.consoleTip(index, 7)))
                        .append(" " + console.consoleHeaterInput(index) + "/" + console.consoleHeaterMax(index) + "mB")
                        .withStyle(net.minecraft.ChatFormatting.BLUE));
                tooltip.add(Component.empty().append(fluidName(console.consoleTip(index, 8)))
                        .append(" " + console.consoleHeaterOutput(index) + "/" + console.consoleHeaterMax(index) + "mB")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
            default -> {
            }
        }
        if (kind == RbmkComponentBlock.Kind.FUEL_ROD_MOD || kind == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD
                || kind == RbmkComponentBlock.Kind.CONTROL_MOD) {
            tooltip.add(Component.translatable("rbmk.moderated").withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
    }

    /** drawScreen tooltips of GUIRBMKRod/Boiler/Heater/Outgasser/Control/ControlAuto, line for line. */
    private List<Component> rbmkTooltip(int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        RbmkComponentBlockEntity rbmk = console();
        RbmkComponentBlock.Kind kind = this.menu.kind();
        if (kind.acceptsFuel()) {
            ItemStack fuel = this.menu.getSlot(0).getItem();
            if (fuel.getItem() instanceof RbmkFuelRodItem && RbmkFuelRodItem.hullHeat(fuel) > 1_000) {
                if (inside(mouseX, mouseY, -16, 20, 16, 16)) {
                    tooltip.add(Component.literal("Fuel skin temperature has exceeded 1,000\u00b0C,"));
                    tooltip.add(Component.literal("autoloaders can no longer cycle fuel!"));
                }
            }
            if (fuel.getItem() instanceof RbmkFuelRodItem && RbmkFuelRodItem.hullHeat(fuel) > 200) {
                if (inside(mouseX, mouseY, -16, 36, 16, 16)) {
                    tooltip.add(Component.literal("Fuel skin temperature has exceeded 200\u00b0C,"));
                    tooltip.add(Component.literal("fuel can no longer be removed by hand!"));
                }
            }
        } else if (kind == RbmkComponentBlock.Kind.BOILER) {
            if (inside(mouseX, mouseY, 126, 24, 16, 56)) {
                tooltip.addAll(HbmFluidTooltip.forTank(rbmk != null ? rbmk.waterTank().type() : fluid("water"), this.menu.water(), RbmkComponentBlockEntity.WATER_CAPACITY));
            }
            if (inside(mouseX, mouseY, 89, 39, 8, 28)) {
                tooltip.addAll(HbmFluidTooltip.forTank(steamFluid(), this.menu.steam(), RbmkComponentBlockEntity.STEAM_CAPACITY));
            }
        } else if (kind == RbmkComponentBlock.Kind.HEATER) {
            if (inside(mouseX, mouseY, 68, 24, 16, 58)) {
                tooltip.addAll(HbmFluidTooltip.forTank(rbmk != null ? rbmk.heaterInputTank().type() : fluid("coolant"), this.menu.heaterInput(), RbmkComponentBlockEntity.HEATER_TANK_CAPACITY));
            }
            if (inside(mouseX, mouseY, 126, 24, 16, 58)) {
                tooltip.addAll(HbmFluidTooltip.forTank(rbmk != null ? rbmk.heaterOutputTank().type() : fluid("coolant_hot"), this.menu.heaterOutput(), RbmkComponentBlockEntity.HEATER_TANK_CAPACITY));
            }
        } else if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
            if (inside(mouseX, mouseY, 112, 21, 16, 48)) {
                tooltip.addAll(HbmFluidTooltip.forTank(rbmk != null ? rbmk.outgasserGasTank().type() : fluid("tritium"), this.menu.outgasserGas(), RbmkComponentBlockEntity.OUTGASSER_GAS_CAPACITY));
            }
        } else if (kind.isAutomaticControl()) {
            if (inside(mouseX, mouseY, 124, 29, 16, 56)) {
                tooltip.add(Component.literal((int) ((rbmk != null ? rbmk.controlLevel() : this.menu.controlLevel() / 1000.0D) * 100) + "%"));
            }
            if (kind == RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO && rbmk != null && inside(mouseX, mouseY, 136, 21, 16, 16)) {
                tooltip.add(Component.literal(com.atomiclegacy.fluid.HbmFluidTraitInfo.shortNumber(rbmk.controlPowerValue()) + " / "
                        + com.atomiclegacy.fluid.HbmFluidTraitInfo.shortNumber(50_000L) + "HE"));
            }
            String func = "Function: " + switch (this.menu.autoFunction()) {
                case 1 -> "Quadratic";
                case 2 -> "Inverse Quadratic";
                default -> "Linear";
            };
            if (inside(mouseX, mouseY, 58, 26, 28, 19)) tooltip.add(Component.literal(func));
            if (inside(mouseX, mouseY, 61, 48, 22, 10)) tooltip.add(Component.literal("Select linear interpolation"));
            if (inside(mouseX, mouseY, 61, 59, 22, 10)) tooltip.add(Component.literal("Select quadratic interpolation"));
            if (inside(mouseX, mouseY, 61, 70, 22, 10)) tooltip.add(Component.literal("Select inverse quadratic interpolation"));
            if (inside(mouseX, mouseY, 28, 26, 30, 10)) {
                tooltip.add(Component.literal("Level at max heat"));
                tooltip.add(Component.literal("Should be smaller than level at min heat"));
            }
            if (inside(mouseX, mouseY, 28, 37, 30, 10)) {
                tooltip.add(Component.literal("Level at min heat"));
                tooltip.add(Component.literal("Should be larger than level at max heat"));
            }
            if (inside(mouseX, mouseY, 28, 48, 30, 10)) {
                tooltip.add(Component.literal("Max heat"));
                tooltip.add(Component.literal("Must be larger than min heat"));
            }
            if (inside(mouseX, mouseY, 28, 59, 30, 10)) {
                tooltip.add(Component.literal("Min heat"));
                tooltip.add(Component.literal("Must be smaller than max heat"));
            }
            if (inside(mouseX, mouseY, 28, 70, 30, 10)) tooltip.add(Component.literal("Save parameters"));
        } else if (kind.isControl()) {
            if (inside(mouseX, mouseY, 71, 29, 16, 56)) {
                tooltip.add(Component.literal((int) ((rbmk != null ? rbmk.controlLevel() : this.menu.controlLevel() / 1000.0D) * 100) + "%"));
            }
            if (kind == RbmkComponentBlock.Kind.CONTROL_REASIM && rbmk != null && inside(mouseX, mouseY, 87, 21, 16, 16)) {
                tooltip.add(Component.literal(com.atomiclegacy.fluid.HbmFluidTraitInfo.shortNumber(rbmk.controlPowerValue()) + " / "
                        + com.atomiclegacy.fluid.HbmFluidTraitInfo.shortNumber(50_000L) + "HE"));
            }
        }
        return tooltip;
    }

    /** GUIRBMKRod: GuiInfoContainer.drawInfoPanel types 6 (large red !) and 7 (large yellow !). */
    private void renderRodWarnings(GuiGraphics graphics) {
        ItemStack fuel = this.menu.getSlot(0).getItem();
        if (!(fuel.getItem() instanceof RbmkFuelRodItem)) {
            return;
        }
        ResourceLocation utility = AtomicLegacy.id("textures/gui/gui_utility.png");
        if (RbmkFuelRodItem.hullHeat(fuel) > 1_000) {
            graphics.blit(utility, this.leftPos - 16, this.topPos + 20, 8, 16, 16, 16, 256, 256);
        }
        if (RbmkFuelRodItem.hullHeat(fuel) > 200) {
            graphics.blit(utility, this.leftPos - 16, this.topPos + 36, 24, 16, 16, 16, 256, 256);
        }
    }

    private static Component autoFunctionName(int function) {
        return switch (Math.floorMod(function, 3)) {
            case 1 -> Component.translatable("rbmk.auto.function_quad");
            case 2 -> Component.translatable("rbmk.auto.function_inverse_quad");
            default -> Component.translatable("rbmk.auto.function_linear");
        };
    }

    private RbmkComponentBlockEntity console() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity blockEntity = this.minecraft.level.getBlockEntity(this.menu.pos());
        return blockEntity instanceof RbmkComponentBlockEntity rbmk ? rbmk : null;
    }

    private void renderFluxGraph(GuiGraphics graphics, RbmkComponentBlockEntity console) {
        int highest = Integer.MIN_VALUE;
        int lowest = Integer.MAX_VALUE;
        int size = console.consoleFluxHistorySize();
        for (int index = 0; index < size; index++) {
            int value = console.consoleFluxHistory(index);
            highest = Math.max(highest, value);
            lowest = Math.min(lowest, value);
        }
        if (highest == Integer.MIN_VALUE || size < 2) {
            return;
        }
        int range = Math.max(1, highest - lowest);
        for (int index = 0; index < size - 1; index++) {
            int x1 = this.leftPos + 7 + (int) Math.round(index * 74.0D / size);
            int x2 = this.leftPos + 7 + (int) Math.round((index + 1) * 74.0D / size);
            int y1 = this.topPos + 127 - (int) Math.round((console.consoleFluxHistory(index) - lowest) * 24.0D / range);
            int y2 = this.topPos + 127 - (int) Math.round((console.consoleFluxHistory(index + 1) - lowest) * 24.0D / range);
            drawLine(graphics, x1, y1, x2, y2, 0xFF00FF00);
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps <= 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / steps;
            int y = y1 + (y2 - y1) * step / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static int consoleColumnOffset(RbmkComponentBlock.Kind kind) {
        return switch (kind) {
            case FUEL_ROD, FUEL_ROD_MOD -> 10;
            // [parity e2] ColumnType.FUEL_SIM(90)
            case FUEL_ROD_REASIM, FUEL_ROD_REASIM_MOD -> 90;
            case CONTROL, CONTROL_MOD, CONTROL_REASIM -> 20;
            case CONTROL_AUTO, CONTROL_REASIM_AUTO -> 30;
            case BOILER -> 40;
            case MODERATOR -> 50;
            case ABSORBER -> 60;
            case REFLECTOR -> 70;
            case OUTGASSER -> 80;
            case STORAGE -> 110;
            case COOLER -> 120;
            case HEATER -> 130;
            default -> 0;
        };
    }

    private static String consoleScreenKey(int type) {
        return switch (Math.floorMod(type, 6)) {
            case 1 -> "col_temp";
            case 2 -> "rod_extraction";
            case 3 -> "fuel_depletion";
            case 4 -> "fuel_poison";
            case 5 -> "fuel_temp";
            default -> "none";
        };
    }

    private static int consoleColor(RbmkComponentBlock.Kind kind, int heat) {
        if (kind.isControl()) {
            return 0xFF4CA3FF;
        }
        if (kind.acceptsFuel()) {
            int red = Math.min(255, 80 + heat / 5);
            return 0xFF000000 | red << 16 | 0x004020;
        }
        return switch (kind) {
            case BOILER -> 0xFF87D7FF;
            case HEATER -> 0xFFFFA13A;
            case COOLER -> 0xFF40D0D0;
            case MODERATOR -> 0xFFCFCFCF;
            case ABSORBER -> 0xFF343434;
            case REFLECTOR -> 0xFFE8E8A0;
            case OUTGASSER -> 0xFFB060FF;
            case STORAGE -> 0xFF8A6A40;
            default -> 0xFF606060;
        };
    }

    private ResourceLocation texture() {
        return switch (this.menu.kind()) {
            case AUTOLOADER -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_autoloader.png");
            case BOILER -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_boiler.png");
            case HEATER -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_heater.png");
            case OUTGASSER -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_outgasser.png");
            case STORAGE -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_storage.png");
            case CONSOLE -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_console.png");
            case CONTROL, CONTROL_MOD, CONTROL_REASIM -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_control.png");
            case CONTROL_AUTO, CONTROL_REASIM_AUTO -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_control_auto.png");
            default -> AtomicLegacy.id("textures/gui/reactors/gui_rbmk_element.png");
        };
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        height = clampPixels(height, bottomY);
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation fluidTexture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(fluidTexture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(fluidTexture, this.leftPos + x, this.topPos + top + tileY, 0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }

    private void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture, x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private int clampPixels(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private HbmFluidDefinition steamFluid() {
        return fluid(switch (this.menu.steamCompression()) {
            case 1 -> "hotsteam";
            case 2 -> "superhotsteam";
            case 3 -> "ultrahotsteam";
            default -> "steam";
        });
    }

    private String percent(int milli) {
        return Integer.toString(Math.round(milli / 10.0F));
    }

    private static final class ClickArea extends AbstractWidget {
        private final Runnable action;

        private ClickArea(int x, int y, int width, int height, Runnable action) {
            // [parity e2] 1.7.10 click areas are x in [x, x + w), y in (y, y + h]
            super(x, y + 1, width, height, Component.empty());
            this.action = action;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
