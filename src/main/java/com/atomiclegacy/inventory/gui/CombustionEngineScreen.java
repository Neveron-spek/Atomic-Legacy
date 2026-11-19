package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.CombustionEngineBlockEntity;
import com.atomiclegacy.inventory.container.CombustionEngineMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.CombustionPistonSetItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class CombustionEngineScreen extends AbstractContainerScreen<CombustionEngineMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/generators/gui_combustion.png");
    private boolean draggingThrottle;

    public CombustionEngineScreen(CombustionEngineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity Q3: GUICombustionEngine.drawScreen]
        if (!this.draggingThrottle) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 143, this.topPos + 17, 16, 52)) {
                guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.powerCap())), mouseX, mouseY);
            }
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 35, this.topPos + 17, 16, 52)) {
                HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.fuelFluid(), this.menu.fuelAmount(), this.menu.fuelCapacity()), this.menu.fuelFluid(), mouseX, mouseY);
            }
        }
        int setting = this.menu.throttle();
        if (this.draggingThrottle || (this.leftPos + 80 <= mouseX && this.leftPos + 80 + 34 > mouseX && this.topPos + 38 < mouseY && this.topPos + 38 + 8 >= mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(((setting * 2) / 10D) + "mB/t")),
                    Math.max(this.leftPos + 80, Math.min(this.leftPos + 114, mouseX)), Math.max(this.topPos + 38, Math.min(this.topPos + 46, mouseY)));
        }
        ItemStack piston = this.menu.getSlot(CombustionEngineBlockEntity.SLOT_PISTON).getItem();
        if (piston.getItem() instanceof CombustionPistonSetItem pistonSet) {
            double power = 0;
            HbmFluidDefinition fluid = this.menu.fuelFluid();
            if (fluid.combustibleFuelGrade() != null) {
                power = setting * 0.2 * fluid.combustibleHeatEnergy() / 1_000D * pistonSet.pistonType(piston).efficiency(fluid.combustibleFuelGrade());
            }
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 50, 35, 14)) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal(String.format(Locale.US, "%,d", (int) power) + " HE/t").withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.format(Locale.US, "%,d", (int) (power * 20)) + " HE/s").withStyle(ChatFormatting.YELLOW)
                ), mouseX, mouseY);
            }
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 13, 35, 15)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Ignition")), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && K3GuiText.hover(mouseX, mouseY, this.leftPos + 89, this.topPos + 13, 16, 14)) {
            sendButton(0);
            playClick();
            return true;
        }
        if (button == 0 && K3GuiText.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 38, 36, 8)) {
            this.draggingThrottle = true;
            sendThrottle(mouseX);
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingThrottle) {
            sendThrottle(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingThrottle = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        ItemStack piston = this.menu.getSlot(CombustionEngineBlockEntity.SLOT_PISTON).getItem();
        if (piston.getItem() instanceof CombustionPistonSetItem pistonSet) {
            int i = pistonSet.pistonType(piston).ordinal();
            guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 51, 176, 52 + i * 12, 25, 12);
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 79 + this.menu.throttleSliderX(), this.topPos + 38, 192, 15, 4, 8);
        if (this.menu.enabled()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 13, 192, 0, 35, 15);
        }
        int power = (int) (this.menu.power() * 53L / this.menu.powerCap()); // [parity Q3: 53 px as in the original]
        guiGraphics.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - power, 176, 52 - power, 16, power);
        // [parity e2] engine.tank.renderTank(guiLeft + 35, guiTop + 69, zLevel, 16, 52)
        E2TankGui.renderTank(guiGraphics, this.leftPos + 35, this.topPos + 69, 16, 52, this.menu.fuelFluid(), this.menu.fuelAmount(), this.menu.fuelCapacity());
    }

    private void sendThrottle(double mouseX) {
        int throttle = (int) ((mouseX - this.leftPos - 81) * CombustionEngineBlockEntity.MAX_THROTTLE / 32.0D);
        throttle = Math.max(0, Math.min(CombustionEngineBlockEntity.MAX_THROTTLE, throttle));
        sendButton(throttle + 1);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
