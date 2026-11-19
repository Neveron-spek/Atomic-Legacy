package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.inventory.container.RotaryFurnaceMenu;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.tileentity.RotaryFurnaceBlockEntity;
import com.atomiclegacy.util.HbmFluidTooltip;
import com.atomiclegacy.util.LegacyBurnModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

public class RotaryFurnaceScreen extends AbstractContainerScreen<RotaryFurnaceMenu> {
    /** [parity e2] TileEntityMachineRotaryFurnace.burnModule (description only). */
    private static final LegacyBurnModule BURN_MODULE = new LegacyBurnModule()
            .time(LegacyBurnModule.COKE, 1.25).time(LegacyBurnModule.ROCKET, 1.5)
            .time(LegacyBurnModule.SOLID, 1.5).time(LegacyBurnModule.BALEFIRE, 1.5)
            .heat(LegacyBurnModule.SOLID, 1.5).heat(LegacyBurnModule.ROCKET, 3).heat(LegacyBurnModule.BALEFIRE, 10);
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_rotary_furnace.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public RotaryFurnaceScreen(RotaryFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        // [parity e2] GUIMachineRotaryFurnace: title centred on (xSize - 54) / 2
        this.titleLabelX = (this.imageWidth - 54) / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(8, 36, 52, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.additiveFluid(), this.menu.additiveAmount(), this.menu.additiveCapacity()), mouseX, mouseY);
        }
        if (isHovering(134, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(HbmFluids.byName("steam").orElse(HbmFluids.none()), this.menu.steamAmount(), this.menu.steamCapacity()), mouseX, mouseY);
        }
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), this.menu.spentSteamAmount(), this.menu.spentSteamCapacity()), mouseX, mouseY);
        }
        // [parity e2] GUIMachineRotaryFurnace: RED "Empty" or YELLOW "<material>: " + Mats.formatAmount(amount, LSHIFT)
        if (isHovering(98, 18, 16, 52, mouseX, mouseY)) {
            FoundryMaterial material = this.menu.outputMaterial();
            Component line;
            if (material == null) {
                line = Component.literal("Empty").withStyle(ChatFormatting.RED);
            } else {
                int amount = this.menu.outputAmount();
                boolean shift = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
                line = Component.translatable(material.translationKey()).withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(shift ? Component.literal((amount * 2) + "mB") : ScrapsItem.formatAmountComponent(amount));
            }
            guiGraphics.renderComponentTooltip(this.font, List.of(line), mouseX, mouseY);
        }
        // [parity e2] burnModule.getDesc() over the empty fuel slot (time bonuses, then heat bonuses)
        if (this.hoveredSlot != null && this.hoveredSlot.index == 4 && !this.hoveredSlot.hasItem()) {
            List<Component> bonuses = new ArrayList<>(BURN_MODULE.timeDesc());
            bonuses.addAll(BURN_MODULE.heatDesc());
            if (!bonuses.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, bonuses, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        // [parity e2] (int) Math.ceil(progress * 33); progress is synced as 0..10000
        int progress = Math.min(33, (this.menu.progress() * 33 + 9_999) / 10_000);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 30, 176, 0, progress, 10, TEX_W, TEX_H);
        }

        int burn = this.menu.burnScaled(14);
        if (burn > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 69 - burn, 176, 24 - burn, 14, burn, TEX_W, TEX_H);
        }

        // [parity e2] molten output: gauge texture (176, 76 - amount) tinted with moltenColor, drawn twice (second pass additive)
        FoundryMaterial material = this.menu.outputMaterial();
        if (material != null) {
            int hex = material.moltenColor();
            int amount = this.menu.outputAmount() * 52 / RotaryFurnaceBlockEntity.MAX_OUTPUT;
            if (amount > 0) {
                guiGraphics.setColor(((hex >> 16) & 0xFF) / 255.0F, ((hex >> 8) & 0xFF) / 255.0F, (hex & 0xFF) / 255.0F, 1.0F);
                guiGraphics.blit(TEXTURE, this.leftPos + 98, this.topPos + 70 - amount, 176, 76 - amount, 16, amount, TEX_W, TEX_H);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                guiGraphics.blit(TEXTURE, this.leftPos + 98, this.topPos + 70 - amount, 176, 76 - amount, 16, amount, TEX_W, TEX_H);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        // [parity e2] FluidTank#renderTank (fluid texture); tank 0 is horizontal (orientation 1)
        E2TankGui.renderTank(guiGraphics, this.leftPos + 8, this.topPos + 52, 52, 16, 1,
                this.menu.additiveFluid(), this.menu.additiveAmount(), this.menu.additiveCapacity());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 134, this.topPos + 70, 16, 52,
                HbmFluids.byName("steam").orElse(HbmFluids.none()), this.menu.steamAmount(), this.menu.steamCapacity());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 152, this.topPos + 70, 16, 52,
                HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), this.menu.spentSteamAmount(), this.menu.spentSteamCapacity());
    }

    private static List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }

    private static String format(int value) {
        return String.format(Locale.US, "%,d", value);
    }
}
