package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * [parity e2] FluidTank#renderTank / FluidTank#renderTankInfo of the 1.7.10 reference: one tinted quad over the
 * repeating 16x16 fluid texture (orientation 0 fills bottom-up, orientation 1 fills left-to-right with mirrored U).
 */
final class E2TankGui {
    private E2TankGui() {
    }

    /** renderTank(x, y, z, width, height): absolute x, y is the BOTTOM edge of the gauge. */
    static void renderTank(GuiGraphics graphics, int x, int y, int width, int height,
                           HbmFluidDefinition fluid, int amount, int capacity) {
        renderTank(graphics, x, y, width, height, 0, fluid, amount, capacity);
    }

    static void renderTank(GuiGraphics graphics, int x, int y, int width, int height, int orientation,
                           HbmFluidDefinition fluid, int amount, int capacity) {
        if (fluid == null || capacity <= 0 || amount <= 0) {
            return;
        }
        y -= height;
        int i = orientation == 1
                ? (int) ((long) amount * width / capacity)
                : (int) ((long) amount * height / capacity);
        if (i <= 0) {
            return;
        }
        int color = fluid.color();
        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
            // same missing-texture fallback as the other tank gauges of the port
            if (orientation == 1) {
                graphics.fill(x, y, x + i, y + height, 0xFF000000 | color);
            } else {
                graphics.fill(x, y + height - i, x + width, y + height, 0xFF000000 | color);
            }
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        if (orientation == 1) {
            // minU = 1, maxU = 1 - i / 16, minV = 0, maxV = height / 16
            graphics.blit(texture, x, y, i, height, 16.0F, 0.0F, -i, height, 16, 16);
        } else {
            // minU = 0, maxU = width / 16, minV = 1 - i / 16, maxV = 1
            graphics.blit(texture, x, y + height - i, width, i, 0.0F, 16.0F - i, width, i, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** renderTankInfo(gui, mouseX, mouseY, x, y, width, height): absolute x/y of the TOP-left corner. */
    static void renderTankInfo(GuiGraphics graphics, Font font, int mouseX, int mouseY, int x, int y, int width, int height,
                               HbmFluidDefinition fluid, int amount, int capacity) {
        renderTankInfo(graphics, font, mouseX, mouseY, x, y, width, height, fluid, amount, capacity, 0);
    }

    static void renderTankInfo(GuiGraphics graphics, Font font, int mouseX, int mouseY, int x, int y, int width, int height,
                               HbmFluidDefinition fluid, int amount, int capacity, int pressure) {
        if (fluid != null && K3GuiText.hover(mouseX, mouseY, x, y, width, height)) {
            HbmFluidTooltipRenderer.render(graphics, font, HbmFluidTooltip.forTank(fluid, amount, capacity, pressure), fluid, mouseX, mouseY);
        }
    }
}
