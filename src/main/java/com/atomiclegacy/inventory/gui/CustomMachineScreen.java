package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.CustomMachineMenu;
import com.atomiclegacy.items.CustomMachineItem;
import com.atomiclegacy.tileentity.CustomMachineBlockEntity;
import com.atomiclegacy.tileentity.ModulePatternMatcher;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Locale;

/** [parity X1: custom machines] GUIMachineCustom: gui/processing/gui_custom.png, 176 x 256. */
public final class CustomMachineScreen extends AbstractContainerScreen<CustomMachineMenu> {
    private static final ResourceLocation texture = AtomicLegacy.id("textures/gui/processing/gui_custom.png");
    private final CustomMachineBlockEntity custom;

    public CustomMachineScreen(CustomMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.custom = menu.custom;
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float interp) {
        super.render(graphics, x, y, interp);
        this.renderTooltip(graphics, x, y);

        K3BGui.electricityInfo(graphics, this.font, x, y, leftPos + 150, topPos + 18, 16, 52, custom.power, custom.config.maxPower);
        if (custom.config.maxHeat > 0) K3BGui.customInfo(graphics, this.font, x, y, leftPos + 61, topPos + 53, 18, 18, x, y,
                List.of(Component.literal("Heat:" + String.format(Locale.US, "%,d", custom.heat) + " / " + String.format(Locale.US, "%,d", custom.config.maxHeat))));
        if (this.menu.getCarried().isEmpty()) {
            for (Slot slot : this.menu.slots) {
                int tileIndex = slot.getContainerSlot();

                if (this.isHovering(slot.x, slot.y, 16, 16, x, y) && slot instanceof CustomMachineMenu.PatternSlot
                        && tileIndex - 10 < custom.matcher.modes.length && custom.matcher.modes[tileIndex - 10] != null) {
                    graphics.renderComponentTooltip(this.font, List.of(Component.literal(ChatFormatting.RED + "Right click to change"),
                            Component.literal(ModulePatternMatcher.getLabel(custom.matcher.modes[tileIndex - 10]))), x, y - 30);
                }
            }
        }

        for (int i = 0; i < custom.inputTanks.length; i++) {
            renderTankInfo(graphics, custom.inputTanks[i], x, y, leftPos + 8 + 18 * i, topPos + 18, 16, 34);
        }

        for (int i = 0; i < custom.outputTanks.length; i++) {
            renderTankInfo(graphics, custom.outputTanks[i], x, y, leftPos + 78 + 18 * i, topPos + 18, 16, 34);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String name = this.custom.getName();
        String localizedName = CustomMachineConfigJSON.localized(this.custom.config, CustomMachineItem.languageCode());
        if (localizedName != null) name = localizedName;
        graphics.drawString(this.font, name, 68 - this.font.width(name) / 2, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        if (custom.config.fluxMode) graphics.drawString(this.font, "Flux:" + custom.flux, 83, 57, 0x08FF00, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float interp, int x, int y) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (custom.config.fluxMode) {
            graphics.blit(texture, leftPos + 78, topPos + 54, 192, 122, 51, 15);
        }
        if (custom.maxHeat > 0) {
            graphics.blit(texture, leftPos + 61, topPos + 53, 236, 0, 18, 18);
            K3LegacyGauge.draw(graphics, leftPos + 70, topPos + 62, (double) custom.heat / (double) custom.config.maxHeat, 5, 2, 1, 0x7F0000);
        }
        int p = custom.progress * 90 / Math.max(custom.maxProgress, 1);
        graphics.blit(texture, leftPos + 78, topPos + 119, 192, 0, Math.min(p, 44), 16);
        if (p > 44) {
            p -= 44;
            graphics.blit(texture, leftPos + 78 + 44, topPos + 119, 192, 16, p, 16);
        }

        int e = (int) (custom.power * 52 / custom.config.maxPower);
        graphics.blit(texture, leftPos + 150, topPos + 70 - e, 176, 52 - e, 16, e);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                if (custom.config.itemInCount <= index) {
                    graphics.blit(texture, leftPos + 7 + j * 18, topPos + 71 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                    graphics.blit(texture, leftPos + 7 + j * 18, topPos + 107 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                }
                if (custom.config.itemOutCount <= index) {
                    graphics.blit(texture, leftPos + 77 + j * 18, topPos + 71 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            if (custom.config.fluidInCount <= i) {
                graphics.blit(texture, leftPos + 7 + i * 18, topPos + 17, 192 + i * 18, 32, 18, 54);
            }
            if (custom.config.fluidOutCount <= i) {
                graphics.blit(texture, leftPos + 77 + i * 18, topPos + 17, 192 + i * 18, 32, 18, 36);
            }
        }

        for (int i = 0; i < custom.inputTanks.length; i++) {
            renderTank(graphics, custom.inputTanks[i], leftPos + 8 + 18 * i, topPos + 52, 16, 34);
        }

        for (int i = 0; i < custom.outputTanks.length; i++) {
            renderTank(graphics, custom.outputTanks[i], leftPos + 78 + 18 * i, topPos + 52, 16, 34);
        }
    }

    /**
     * FluidTank#renderTank(x, y, z, width, height), orientation 0: the 16 x 16 fluid sheet repeated upwards from the
     * bottom edge (V from 1 - i / 16 to 1), tinted by getTint (0xFFFFFF for the built-in fluids).
     */
    /** [parity g1] FluidTank#renderTank: tinted with the fluid colour (shared E2TankGui). */
    private void renderTank(GuiGraphics graphics, HbmFluidTank tank, int x, int y, int width, int height) {
        E2TankGui.renderTank(graphics, x, y, width, height, tank.type(), tank.amount(), tank.capacity());
    }

    private void renderTankInfo(GuiGraphics graphics, HbmFluidTank tank, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY) {
            HbmFluidTooltipRenderer.render(graphics, this.font, HbmFluidTooltip.forTank(tank.type(), tank.amount(), tank.capacity(), tank.pressure()),
                    tank.type(), mouseX, mouseY);
        }
    }
}
