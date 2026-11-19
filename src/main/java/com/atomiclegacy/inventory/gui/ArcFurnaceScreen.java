package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.inventory.container.ArcFurnaceMenu;
import com.atomiclegacy.items.ScrapsItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class ArcFurnaceScreen extends AbstractContainerScreen<ArcFurnaceMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_arc_furnace.png");

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && K3GuiText.hover(mouseX, mouseY, leftPos + 151, topPos + 17, 18, 18)
                && this.minecraft != null && this.minecraft.gameMode != null) {
            K3GuiText.click(); // [parity K3: gui.button.press]
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        // [parity K3: GUIMachineArcFurnaceLarge.drawScreen - only the energy and melt tooltips exist]
        if (K3GuiText.hover(mouseX, mouseY, leftPos + 8, topPos + 36, 7, 70)) {
            graphics.renderComponentTooltip(font, List.of(K3GuiText.electricity(menu.power(), ArcFurnaceMenuPower.MAX)), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, leftPos + 152, topPos + 36, 16, 70)) {
            graphics.renderComponentTooltip(font, liquidTooltip(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // [parity K3: title drawn in 0xffffff, inventory label in 4210752]
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.liquidMode()) graphics.blit(TEXTURE, leftPos + 151, topPos + 17, 190, 18, 18, 18);
        if (menu.working()) graphics.blit(TEXTURE, leftPos + 7, topPos + 17, 190, 0, 18, 18);

        int power = (int) ((long) menu.power() * 70 / ArcFurnaceMenuPower.MAX);
        if (power > 0) graphics.blit(TEXTURE, leftPos + 8, topPos + 106 - power, 176, 70 - power, 7, power);
        int progress = menu.progressScaled(70);
        if (progress > 0) graphics.blit(TEXTURE, leftPos + 17, topPos + 106 - progress, 183, 70 - progress, 7, progress);
        drawLiquid(graphics);
    }

    private void drawLiquid(GuiGraphics graphics) {
        int lastHeight = 0;
        int lastAmount = 0;
        for (int index = 0; index < 4; index++) {
            FoundryMaterial material = FoundryMaterial.byId(menu.liquidMaterialId(index)).orElse(null);
            int materialAmount = menu.liquidMaterialAmount(index);
            if (material == null || materialAmount <= 0) continue;
            int targetHeight = Math.min(70, (lastAmount + materialAmount) * 70 / menu.liquidCapacity());
            int height = targetHeight - lastHeight;
            lastAmount += materialAmount;
            if (height <= 0) continue;
            int color = material.moltenColor();
            graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
            graphics.blit(TEXTURE, leftPos + 152, topPos + 106 - targetHeight, 208, 70 - targetHeight, 16, height);
            // [parity e2] drawStack: second pass keeps the tint and is additive (GL_SRC_ALPHA, GL_ONE)
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            graphics.blit(TEXTURE, leftPos + 152, topPos + 106 - targetHeight, 208, 70 - targetHeight, 16, height);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            lastHeight = targetHeight;
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private List<Component> liquidTooltip() {
        List<Component> tooltip = new ArrayList<>();
        if (menu.liquidAmount() <= 0) {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
            return tooltip;
        }
        for (int index = 0; index < 4; index++) {
            FoundryMaterial material = FoundryMaterial.byId(menu.liquidMaterialId(index)).orElse(null);
            int amount = menu.liquidMaterialAmount(index);
            if (material != null && amount > 0) {
                tooltip.add(Component.translatable(material.translationKey()).withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(leftShiftDown() // [parity K3: Mats.formatAmount(amount, KEY_LSHIFT) shows mB]
                                ? Component.literal((amount * 2) + "mB")
                                : ScrapsItem.formatAmountComponent(amount)));
            }
        }
        return tooltip;
    }

    private static boolean leftShiftDown() {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    private static final class ArcFurnaceMenuPower {
        private static final int MAX = (int) com.atomiclegacy.tileentity.ArcFurnaceBlockEntity.MAX_POWER;
    }
}
