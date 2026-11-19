package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.inventory.container.StrandCasterMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import com.atomiclegacy.items.ScrapsItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class StrandCasterScreen extends AbstractContainerScreen<StrandCasterMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_strand_caster.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public StrandCasterScreen(StrandCasterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 4;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 16, this.topPos + 17, 36, 81)) {
            guiGraphics.renderComponentTooltip(this.font, materialTooltip(), mouseX, mouseY);
        }
        // [parity g2] FluidTank#renderTankInfo
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 82, this.topPos + 14, 16, 24,
                this.menu.waterFluid(), this.menu.waterAmount(), this.menu.waterCapacity());
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 82, this.topPos + 65, 16, 24,
                this.menu.steamFluid(), this.menu.steamAmount(), this.menu.steamCapacity());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false); // [parity g2] white title
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        FoundryMaterial material = this.menu.material();
        if (this.menu.amount() != 0 && material != null) {
            // [parity g2] texture strip tinted with moltenColor, then the same strip additively (SRC_ALPHA, ONE) at alpha 0.3
            int targetHeight = Math.min(this.menu.amount() * 79 / this.menu.capacity(), 92);
            int hex = material.moltenColor();
            RenderSystem.setShaderColor(((hex >> 16) & 0xFF) / 255.0F, ((hex >> 8) & 0xFF) / 255.0F, (hex & 0xFF) / 255.0F, 1.0F);
            blit(guiGraphics, this.leftPos + 17, this.topPos + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.3F);
            blit(guiGraphics, this.leftPos + 17, this.topPos + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // [parity g2] FluidTank#renderTank: bottom edges 38 and 89
        E2TankGui.renderTank(guiGraphics, this.leftPos + 82, this.topPos + 38, 16, 24,
                this.menu.waterFluid(), this.menu.waterAmount(), this.menu.waterCapacity());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 82, this.topPos + 89, 16, 24,
                this.menu.steamFluid(), this.menu.steamAmount(), this.menu.steamCapacity());
    }

    private List<Component> materialTooltip() {
        FoundryMaterial material = this.menu.material();
        if (material == null) {
            return List.of(Component.literal("Empty").withStyle(ChatFormatting.RED)); // [parity g2] literal "Empty"
        }
        int amount = this.menu.amount();
        boolean shift = com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT);
        // [parity g2] Mats.formatAmount(amount, KEY_LSHIFT)
        return List.of(Component.translatable(material.translationKey()).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": "))
                .append(shift ? Component.literal((amount * 2) + "mB") : ScrapsItem.formatAmountComponent(amount)));
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, com.atomiclegacy.fluid.HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
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

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
    }
}
