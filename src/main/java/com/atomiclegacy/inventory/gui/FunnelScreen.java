package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.FunnelMenu;
import com.atomiclegacy.network.FunnelControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class FunnelScreen extends AbstractContainerScreen<FunnelMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_funnel.png");

    public FunnelScreen(FunnelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
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
        // [parity g1: GUIFunnel.drawScreen]
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 159, this.topPos + 73, 10, 10)) {
            int mode = this.menu.mode();
            graphics.renderComponentTooltip(this.font, java.util.List.of(Component.literal(
                    "Mode: " + (mode == 1 ? "3x3 only" : mode == 2 ? "2x2 only" : "3x3 then 2x2"))), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 159, this.topPos + 73, 176, this.menu.mode() * 10, 10, 10, 256, 256);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 159, this.topPos + 73, 10, 10)) {
            PacketDistributor.sendToServer(new FunnelControlPayload(this.menu.blockPos()));
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
