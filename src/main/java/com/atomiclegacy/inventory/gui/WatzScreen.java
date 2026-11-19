package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.WatzMenu;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class WatzScreen extends AbstractContainerScreen<WatzMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/reactors/gui_watz.png");
    private static final int LOCK_X = 142;
    private static final int LOCK_Y = 70;
    private static final int LOCK_SIZE = 18;

    public WatzScreen(WatzMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 229;
        this.inventoryLabelY = this.imageHeight - 93;
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

        if (isHovering(13, 100, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(String.format(Locale.US, "%,d TU", this.menu.heat()))), mouseX, mouseY);
        }
        if (isHovering(143, 71, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    this.menu.locked() ? "tooltip.atomiclegacy.watz.unlock" : "tooltip.atomiclegacy.watz.lock"
            )), mouseX, mouseY);
        }
        for (int tank = 0; tank < 3; tank++) {
            if (isHovering(142 + tank * 6, 23, 6, 45, mouseX, mouseY)) {
                HbmFluidDefinition fluid = tankFluid(tank);
                guiGraphics.renderComponentTooltip(
                        this.font,
                        HbmFluidTooltip.forTank(fluid, this.menu.tankAmount(tank), this.menu.tankCapacity(tank)),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        float heatColor = Mth.clamp(1.0F - (float) Math.log(this.menu.heat() / 100_000.0D + 1.0D) * 0.4F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, heatColor, heatColor, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 131, 122);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos + 131, this.topPos, 131, 0, 36, 122);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos + 130, 0, 130, this.imageWidth, 99);
        guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 31, 176, 31, 9, 60);
        guiGraphics.blit(TEXTURE, this.leftPos + 105, this.topPos + 96, 185, 26, 30, 26);
        guiGraphics.blit(TEXTURE, this.leftPos + 9, this.topPos + 96, 184, 0, 26, 26);

        if (this.menu.on()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 147, this.topPos + 8, 176, 0, 8, 8);
        }
        if (this.menu.locked()) {
            guiGraphics.blit(TEXTURE, this.leftPos + LOCK_X, this.topPos + LOCK_Y, 210, 0, LOCK_SIZE, LOCK_SIZE);
        }

        // [parity M3: watz] GUIElements.drawSmoothGauge(guiLeft + 22, guiTop + 109, z, 1 - col, 5, 2, 1, 0x7F0000)
        K3LegacyGauge.draw(guiGraphics, this.leftPos + 22, this.topPos + 109, 1.0D - heatColor, 5, 2, 1, 0x7F0000);
        for (int tank = 0; tank < 3; tank++) {
            drawFluid(guiGraphics, 143 + tank * 6, 69, 4, this.menu.tankScaled(tank, 43), tankFluid(tank));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 93, 4210752, false);
        String flux = String.format(Locale.US, "%,.1f", this.menu.flux() / 10.0D);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8F, 0.8F, 1.0F);
        guiGraphics.drawString(this.font, flux, (int) (161 * 1.25D - this.font.width(flux)), (int) (107 * 1.25D), 0x00FF00, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && this.leftPos + LOCK_X <= mouseX
                && this.leftPos + LOCK_X + LOCK_SIZE > mouseX
                && this.topPos + LOCK_Y < mouseY
                && this.topPos + LOCK_Y + LOCK_SIZE >= mouseY) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private HbmFluidDefinition tankFluid(int tank) {
        return HbmFluids.byOldId(this.menu.tankFluidOldId(tank)).orElse(HbmFluids.none());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        graphics.blit(texture, this.leftPos + x, this.topPos + bottomY - height, 0, 16 - height, width, height, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
