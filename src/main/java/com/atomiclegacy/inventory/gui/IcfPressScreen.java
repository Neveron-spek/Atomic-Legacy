package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.IcfPressBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.IcfPressMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class IcfPressScreen extends AbstractContainerScreen<IcfPressMenu> {
    private static final ResourceLocation TEXTURE =
            AtomicLegacy.id("textures/gui/processing/gui_icf_press.png");

    public IcfPressScreen(IcfPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 179;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // [parity g1: GUIICFPress.drawScreen]
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 44, this.topPos + 18, 16, 52,
                this.menu.fluid(0), this.menu.fluidAmount(0), IcfPressBlockEntity.TANK_CAPACITY, this.menu.fluidPressure(0));
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52,
                this.menu.fluid(1), this.menu.fluidAmount(1), IcfPressBlockEntity.TANK_CAPACITY, this.menu.fluidPressure(1));
        slotHint(graphics, 4, "tooltip.atomiclegacy.machine_icf_press.input_top_bottom", mouseX, mouseY);
        slotHint(graphics, 5, "tooltip.atomiclegacy.machine_icf_press.input_sides", mouseX, mouseY);
    }

    private void slotHint(GuiGraphics graphics, int slotIndex, String key, int mouseX, int mouseY) {
        Slot slot = this.menu.getSlot(slotIndex);
        if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(key).withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int muonPixels = Math.min(52, this.menu.muon() * 52 / IcfPressBlockEntity.MAX_MUON);
        if (muonPixels > 0) {
            graphics.blit(TEXTURE, this.leftPos + 28, this.topPos + 70 - muonPixels,
                    176, 52 - muonPixels, 4, muonPixels);
        }
        E2TankGui.renderTank(graphics, this.leftPos + 44, this.topPos + 70, 16, 52, this.menu.fluid(0), this.menu.fluidAmount(0), IcfPressBlockEntity.TANK_CAPACITY);
        E2TankGui.renderTank(graphics, this.leftPos + 152, this.topPos + 70, 16, 52, this.menu.fluid(1), this.menu.fluidAmount(1), IcfPressBlockEntity.TANK_CAPACITY);
    }
}
