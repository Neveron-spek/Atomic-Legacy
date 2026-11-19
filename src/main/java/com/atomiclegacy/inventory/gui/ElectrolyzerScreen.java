package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ElectrolyzerBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryShape;
import com.atomiclegacy.inventory.container.ElectrolyzerMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {
    private static final ResourceLocation FLUID_TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_electrolyser_fluid.png");
    private static final ResourceLocation METAL_TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_electrolyser_metal.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 210;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, (this.imageWidth / 2 - this.font.width(this.title) / 2) - 16, 7, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (this.menu.isFluidMode()) {
            tankTooltip(guiGraphics, mouseX, mouseY, 42, 18, this.menu.inputFluid(), this.menu.inputAmount());
            tankTooltip(guiGraphics, mouseX, mouseY, 96, 18, this.menu.outputFluid1(), this.menu.outputAmount1());
            tankTooltip(guiGraphics, mouseX, mouseY, 116, 18, this.menu.outputFluid2(), this.menu.outputAmount2());
        } else {
            tankTooltip(guiGraphics, mouseX, mouseY, 36, 18, this.menu.acidFluid(), this.menu.acidAmount());
            materialTooltip(guiGraphics, mouseX, mouseY, 58, 18, this.menu.leftMaterial(), this.menu.leftAmount());
            materialTooltip(guiGraphics, mouseX, mouseY, 96, 18, this.menu.rightMaterial(), this.menu.rightAmount());
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 186, this.topPos + 18, 16, 89)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(Integer.toUnsignedLong(this.menu.energy()), ElectrolyzerBlockEntity.MAX_POWER)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isFluidMode()) {
            E2TankGui.renderTank(guiGraphics, this.leftPos + 42, this.topPos + 70, 16, 52, this.menu.inputFluid(), this.menu.inputAmount(), ElectrolyzerBlockEntity.TANK_CAPACITY);
            E2TankGui.renderTank(guiGraphics, this.leftPos + 96, this.topPos + 70, 16, 52, this.menu.outputFluid1(), this.menu.outputAmount1(), ElectrolyzerBlockEntity.TANK_CAPACITY);
            E2TankGui.renderTank(guiGraphics, this.leftPos + 116, this.topPos + 70, 16, 52, this.menu.outputFluid2(), this.menu.outputAmount2(), ElectrolyzerBlockEntity.TANK_CAPACITY);

            // [parity g1: GUIElectrolyserFluid - power indicator when power >= usageFluid]
            if (Integer.toUnsignedLong(this.menu.energy()) >= this.menu.usageFluid()) {
                guiGraphics.blit(texture(), this.leftPos + 190, this.topPos + 4, 226, 40, 9, 12, TEX_W, TEX_H);
            }
            int fluidProgress = this.menu.fluidProgressScaled(41);
            if (fluidProgress > 0) {
                guiGraphics.blit(texture(), this.leftPos + 62, this.topPos + 26, 226, 0, 12, fluidProgress, TEX_W, TEX_H);
            }
        } else {
            E2TankGui.renderTank(guiGraphics, this.leftPos + 36, this.topPos + 70, 16, 52, this.menu.acidFluid(), this.menu.acidAmount(), ElectrolyzerBlockEntity.TANK_CAPACITY);
            drawMaterial(guiGraphics, 58, 60, 34, this.menu.leftMaterial(), this.menu.leftAmount());
            drawMaterial(guiGraphics, 96, 60, 34, this.menu.rightMaterial(), this.menu.rightAmount());
            // [parity g1: GUIElectrolyserMetal - power indicator when power >= usageOre]
            if (Integer.toUnsignedLong(this.menu.energy()) >= this.menu.usageOre()) {
                guiGraphics.blit(texture(), this.leftPos + 190, this.topPos + 4, 226, 25, 9, 12, TEX_W, TEX_H);
            }
            int oreProgress = this.menu.oreProgressScaled(26);
            if (oreProgress > 0) {
                guiGraphics.blit(texture(), this.leftPos + 7, this.topPos + 71 - oreProgress, 226, 25 - oreProgress, 22, oreProgress, TEX_W, TEX_H);
            }
        }
        int energy = this.menu.energyScaled(89);
        if (energy > 0) {
            guiGraphics.blit(texture(), this.leftPos + 186, this.topPos + 107 - energy, 210, 89 - energy, 16, energy, TEX_W, TEX_H);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 82, 54, 12)) {
            K3GuiText.click();
            int next = this.menu.isFluidMode() ? ElectrolyzerBlockEntity.GUI_METAL : ElectrolyzerBlockEntity.GUI_FLUID;
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, next);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void tankTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, HbmFluidDefinition fluid, int amount) {
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + x, this.topPos + y, 16, 52, fluid, amount, ElectrolyzerBlockEntity.TANK_CAPACITY);
    }

    /** [parity g1] GUIElectrolyserMetal: texture strip (210, 131 - p) tinted with the molten colour. */
    private void drawMaterial(GuiGraphics graphics, int x, int bottomY, int width, FoundryMaterial material, int amount) {
        if (material == null) {
            return;
        }
        int height = amount * 42 / ElectrolyzerBlockEntity.MAX_MATERIAL;
        int color = material.moltenColor();
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        graphics.blit(texture(), this.leftPos + x, this.topPos + bottomY - height, 210, 131 - height, width, height, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void materialTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, FoundryMaterial material, int amount) {
        if (!K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, 34, 42)) {
            return;
        }
        if (material == null || amount <= 0) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.atomiclegacy.empty").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            return;
        }
        graphics.renderComponentTooltip(
                this.font,
                List.of(Component.translatable(material.translationKey()).withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                                ? Component.literal((amount * 2) + "mB")
                                : formatMaterialAmount(amount))),
                mouseX,
                mouseY
        );
    }

    private static Component formatMaterialAmount(int amount) {
        int remaining = amount;
        int blocks = remaining / FoundryShape.BLOCK.quanta();
        remaining -= blocks * FoundryShape.BLOCK.quanta();
        int ingots = remaining / FoundryShape.INGOT.quanta();
        remaining -= ingots * FoundryShape.INGOT.quanta();
        int nuggets = remaining / FoundryShape.NUGGET.quanta();
        remaining -= nuggets * FoundryShape.NUGGET.quanta();

        java.util.ArrayList<Component> parts = new java.util.ArrayList<>();
        if (blocks > 0) parts.add(Component.translatable(blocks == 1 ? "matshape.atomiclegacy.block" : "matshape.atomiclegacy.blocks", blocks));
        if (ingots > 0) parts.add(Component.translatable(ingots == 1 ? "matshape.atomiclegacy.ingot" : "matshape.atomiclegacy.ingots", ingots));
        if (nuggets > 0) parts.add(Component.translatable(nuggets == 1 ? "matshape.atomiclegacy.nugget" : "matshape.atomiclegacy.nuggets", nuggets));
        if (remaining > 0) parts.add(Component.translatable(remaining == 1 ? "matshape.atomiclegacy.quantum" : "matshape.atomiclegacy.quanta", remaining));
        if (parts.isEmpty()) {
            return Component.literal("0");
        }
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(" "));
            }
            result.append(parts.get(i));
        }
        return result;
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture(), x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private ResourceLocation texture() {
        return this.menu.isFluidMode() ? FLUID_TEXTURE : METAL_TEXTURE;
    }
}
