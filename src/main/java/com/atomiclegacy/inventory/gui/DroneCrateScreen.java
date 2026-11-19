package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.DroneCrateMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** Original 176x185 transport drone crate GUI, including its two mode buttons. */
public final class DroneCrateScreen extends AbstractContainerScreen<DroneCrateMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/storage/gui_crate_drone.png");
    private static final int TANK_X = 125;
    private static final int TANK_Y = 17;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 34;

    public DroneCrateScreen(DroneCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 185;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        E2TankGui.renderTankInfo(graphics, font, mouseX, mouseY, leftPos + TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT,
                menu.tankFluid(), menu.tankAmount(), menu.tankCapacity());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEXTURE, leftPos + 151, topPos + 16, 194, menu.itemType() ? 0 : 18, 18, 18);
        graphics.blit(TEXTURE, leftPos + 151, topPos + 52, 176, menu.sendingMode() ? 18 : 0, 18, 18);
        E2TankGui.renderTank(graphics, leftPos + TANK_X, topPos + TANK_Y + TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT, menu.tankFluid(), menu.tankAmount(), menu.tankCapacity());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickMode(mouseX, mouseY, 151, 16, 0) || clickMode(mouseX, mouseY, 151, 52, 1)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickMode(double mouseX, double mouseY, int x, int y, int menuButton) {
        if (!K3GuiText.hover(mouseX, mouseY, leftPos + x, topPos + y, 18, 18)) {
            return false;
        }
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menuButton);
        }
        return true;
    }
}
