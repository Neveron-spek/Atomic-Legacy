package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.RadarSlotsMenu;
import com.atomiclegacy.network.RadarControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direct layout port of GUIMachineRadarNTSlots. */
public final class RadarSlotsScreen extends AbstractContainerScreen<RadarSlotsMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_radar_link.png");

    public RadarSlotsScreen(RadarSlotsMenu menu, Inventory inventory, Component title) {
        // [parity J1] GUIMachineRadarNTSlots: polaroidID == 11 renames the GUI title to "Reda"
        super(menu, inventory, com.atomiclegacy.AtomicLegacy.polaroidID == 11 ? Component.literal("Reda") : title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = 90;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (K3GuiText.hover(mouseX, mouseY, leftPos + 5, topPos + 5, 8, 8)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.atomiclegacy.radar.configure")), mouseX, mouseY); // [parity g2] radar.toggleGui
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (K3GuiText.hover(mouseX, mouseY, leftPos + 5, topPos + 5, 8, 8)) {
            K3GuiText.click();
            PacketDistributor.sendToServer(new RadarControlPayload(menu.blockPos(), 7)); // [parity g2] reopen GUI 0
            return true;
        }
        return handled;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.power() > 0) {
            int bar = (int) (menu.power() * 160L / Math.max(1L, menu.powerCapacity()));
            graphics.blit(TEXTURE, leftPos + 8, topPos + 64, 0, 185, bar, 16, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
