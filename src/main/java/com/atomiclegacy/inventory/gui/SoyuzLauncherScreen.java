package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.SoyuzLauncherMenu;
import com.atomiclegacy.network.SoyuzLauncherControlPayload;
import com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** [parity P2: soyuz_launcher] Layout port of GUISoyuzLauncher (194x244, gui_soyuz.png). */
public final class SoyuzLauncherScreen extends AbstractContainerScreen<SoyuzLauncherMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_soyuz.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public SoyuzLauncherScreen(SoyuzLauncherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 194;
        this.imageHeight = 244;
        this.inventoryLabelX = 17;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private SoyuzLauncherBlockEntity launcher() {
        return this.menu.launcher();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        SoyuzLauncherBlockEntity launcher = launcher();
        if (isHovering(152, 44, 16, 52, mouseX, mouseY)) {
            HbmFluidTank tank = launcher.keroseneTank();
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(tank.type(), tank.amount(), tank.capacity()), mouseX, mouseY);
        }
        if (isHovering(170, 44, 16, 52, mouseX, mouseY)) {
            HbmFluidTank tank = launcher.oxygenTank();
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(tank.type(), tank.amount(), tank.capacity()), mouseX, mouseY);
        }
        if (isHovering(134, 44, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(launcher.power(), SoyuzLauncherBlockEntity.MAX_POWER)) /* [parity e2] drawElectricityInfo */, mouseX, mouseY);
        }
        if (isHovering(-16, 53, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.soyuz.desc"),
                    this.leftPos - 8, this.topPos + 53 + 16);
        }
        if (isHovering(79, 52, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.soyuz.cargo"), mouseX, mouseY);
        }
        if (isHovering(97, 52, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, LegacyInfoText.lines("desc.gui.soyuz.satellite"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (inButton(mouseX, mouseY, 97, 52)) send(0, 0);
        if (inButton(mouseX, mouseY, 79, 52)) send(1, 0);
        if (inButton(mouseX, mouseY, 88, 97)) send(0, 1);
        return handled;
    }

    private boolean inButton(double x, double y, int left, int top) {
        return this.leftPos + left <= x && this.leftPos + left + 18 > x && this.topPos + top < y && this.topPos + top + 18 >= y;
    }

    private void send(int value, int id) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        PacketDistributor.sendToServer(new SoyuzLauncherControlPayload(this.menu.blockPos(), value, id));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 4, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 17, this.inventoryLabelY, 4210752, false);
        int countdown = launcher().renderCountdown();
        String secs = Integer.toString(countdown / 20);
        String cents = Integer.toString((countdown % 20) * 5);
        if (secs.length() == 1) secs = "0" + secs;
        if (cents.length() == 1) cents += "0";
        graphics.drawString(this.font, secs + ":" + cents, 85, 121, 0xFF0000, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        SoyuzLauncherBlockEntity launcher = launcher();
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int i = (int) (launcher.power() * 52 / SoyuzLauncherBlockEntity.MAX_POWER);
        graphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 96 - i, 194, 52 - i, 16, i, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 97, this.topPos + 79, 210 + (launcher.hasRocket() ? 18 : 0), 8, 18, 18, 256, 256);
        int j = launcher.designator();
        if (j > 0) graphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 79, 210 + (j - 1) * 18, 8, 18, 18, 256, 256);
        int k = this.menu.mode();
        graphics.blit(TEXTURE, this.leftPos + 97 - k * 18, this.topPos + 52, 228 - k * 18, 26, 18, 18, 256, 256);
        int l = launcher.orbital();
        if (l > 0) graphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 25, 210 + (l - 1) * 18, 8, 18, 18, 256, 256);
        int m = launcher.satellite();
        if (m > 0) graphics.blit(TEXTURE, this.leftPos + 97, this.topPos + 25, 210 + (m - 1) * 18, 8, 18, 18, 256, 256);
        if (launcher.renderStarting()) graphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 97, 210, 44, 18, 18, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 157, this.topPos + 31, launcher.hasFuel() ? 210 : 216, 0, 6, 8, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 175, this.topPos + 31, launcher.hasOxy() ? 210 : 216, 0, 6, 8, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 139, this.topPos + 31, launcher.hasPower() ? 210 : 216, 0, 6, 8, 256, 256);
        drawFluid(graphics, 152, 96, launcher.keroseneTank());
        drawFluid(graphics, 170, 96, launcher.oxygenTank());
        graphics.blit(UTILITY, this.leftPos - 16, this.topPos + 53, 8, 0, 16, 16, 256, 256);
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, HbmFluidTank tank) {
        HbmFluidDefinition fluid = tank.type();
        int height = tank.capacity() <= 0 ? 0 : tank.amount() * 52 / tank.capacity();
        if (height <= 0 || fluid.isNone()) return;
        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + 16, this.topPos + bottomY,
                    0xFF000000 | fluid.color());
            return;
        }
        RenderSystem.setShaderColor(((fluid.color() >> 16) & 0xFF) / 255.0F, ((fluid.color() >> 8) & 0xFF) / 255.0F,
                (fluid.color() & 0xFF) / 255.0F, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + tileY, 0, 16 - tileHeight, 16, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
