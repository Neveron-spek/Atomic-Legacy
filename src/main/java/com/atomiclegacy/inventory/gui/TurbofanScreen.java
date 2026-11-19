package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.TurbofanMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Direct layout port of GUIMachineTurbofan from HBM 1.7.10. */
public final class TurbofanScreen extends AbstractContainerScreen<TurbofanMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/generators/gui_turbofan.png");
    private static final ResourceLocation GAUGE_SMALL_ROUND = AtomicLegacy.id("textures/gui/gauges/small_round.png");

    public TurbofanScreen(TurbofanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 43 - this.font.width(this.title) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int energy = Math.min(52, this.menu.energy() * 52 / Math.max(1, this.menu.energyCapacity()));
        if (energy > 0) {
            graphics.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - energy, 192, 52 - energy, 16, energy, 256, 256);
        }
        int afterburner = this.menu.afterburner();
        if (afterburner > 0) {
            graphics.blit(TEXTURE, this.leftPos + 98, this.topPos + 44, 176, (Math.min(afterburner, 6) - 1) * 16, 16, 16, 256, 256);
        }
        // [parity Q3: GUIElements.renderGauge(ROUND_SMALL) for blood, FluidTank.renderTank for fuel]
        if (this.menu.showBlood()) {
            int frame = (int) Math.round((13 - 1) * ((double) this.menu.bloodAmount() / 24_000D));
            graphics.blit(GAUGE_SMALL_ROUND, this.leftPos + 97, this.topPos + 16, 0, frame * 18, 18, 18, 18, 234);
        }
        // [parity e2] turbofan.tank.renderTank(guiLeft + 35, guiTop + 69, zLevel, 34, 52)
        E2TankGui.renderTank(graphics, this.leftPos + 35, this.topPos + 69, 34, 52, this.menu.fuel(), this.menu.fuelAmount(), 24_000);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        // [parity e2] GUIMachineTurbofan#drawScreen
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 35, this.topPos + 17, 34, 52,
                this.menu.fuel(), this.menu.fuelAmount(), 24_000);
        if (this.menu.showBlood()) {
            E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 98, this.topPos + 17, 16, 16,
                    this.menu.blood(), this.menu.bloodAmount(), 24_000);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 143, this.topPos + 17, 16, 52)) {
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottom, int width, int maxHeight,
                           HbmFluidDefinition fluid, int amount, int capacity) {
        if (fluid.isNone() || amount <= 0) return;
        int height = Math.min(maxHeight, amount * maxHeight / Math.max(1, capacity));
        graphics.fill(this.leftPos + x, this.topPos + bottom - height,
                this.leftPos + x + width, this.topPos + bottom, 0xFF000000 | fluid.color());
    }
}
