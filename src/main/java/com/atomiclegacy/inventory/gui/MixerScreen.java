package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.MixerBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.MixerMenu;
import com.atomiclegacy.network.MixerControlPayload;
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

public class MixerScreen extends AbstractContainerScreen<MixerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_mixer.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public MixerScreen(MixerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 + 40 / 2 - this.font.width(this.title) / 2; // [parity Q1: GUIMixer]
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderHoverTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        // [parity Q1: GUIMixer#drawGuiContainerBackgroundLayer coordinates]
        int energy = this.menu.energyScaled(52);
        if (energy > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 12, this.topPos + 70 - energy, 176, 52 - energy, 16, energy, TEX_W, TEX_H);
        }

        if (this.menu.processTime() > 0 && this.menu.progress() > 0) {
            int progress = this.menu.progress() * 52 / this.menu.processTime();
            guiGraphics.blit(TEXTURE, this.leftPos + 71, this.topPos + 31, 192, 0, progress, 44, TEX_W, TEX_H);
        }

        // [parity e2] FluidTank#renderTank instead of flat fills
        E2TankGui.renderTank(guiGraphics, this.leftPos + 52, this.topPos + 70, 7, 52, this.menu.inputFluid1(), this.menu.inputAmount1(), this.menu.inputCapacity1());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 61, this.topPos + 70, 7, 52, this.menu.inputFluid2(), this.menu.inputAmount2(), this.menu.inputCapacity2());
        E2TankGui.renderTank(guiGraphics, this.leftPos + 126, this.topPos + 70, 16, 52, this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity());
        K3BGui.infoPanel(guiGraphics, this.leftPos + 152, this.topPos + 55, 8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.leftPos + 71 <= mouseX && this.leftPos + 71 + 12 > mouseX
                && this.topPos + 17 < mouseY && this.topPos + 17 + 12 >= mouseY) {
            PacketDistributor.sendToServer(new MixerControlPayload(this.menu.blockPos()));
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** [parity Q1: GUIMixer#drawScreen tooltips] */
    private void renderHoverTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        K3BGui.electricityInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 12, this.topPos + 18, 16, 52,
                this.menu.energy(), MixerBlockEntity.MAX_POWER);
        K3BGui.customInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 152, this.topPos + 55, 8, 8, mouseX, mouseY, List.of(
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"),
                Component.translatable("desc.gui.upgrade.overdrive")));
        if (this.minecraft != null && this.minecraft.level != null) {
            List<net.minecraft.world.item.crafting.RecipeHolder<com.atomiclegacy.inventory.recipes.MixerRecipe>> recipes =
                    MixerBlockEntity.recipesFor(this.minecraft.level, this.menu.outputFluid());
            if (recipes.size() > 1) {
                int index = Math.floorMod(this.menu.recipeIndex(), recipes.size());
                com.atomiclegacy.inventory.recipes.MixerRecipe recipe = recipes.get(index).value();
                List<Component> label = new java.util.ArrayList<>();
                label.add(Component.literal("Current recipe (" + (this.menu.recipeIndex() + 1) + "/" + recipes.size() + "):")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
                recipe.input1().ifPresent(f -> label.add(Component.literal("-").append(Component.translatable(f.fluid().translationKey()))));
                recipe.input2().ifPresent(f -> label.add(Component.literal("-").append(Component.translatable(f.fluid().translationKey()))));
                recipe.solidInput().ifPresent(s -> {
                    net.minecraft.world.item.ItemStack[] items = s.ingredient().getItems();
                    if (items.length > 0) {
                        int shown = (int) (System.currentTimeMillis() % (1000L * items.length) / 1000L); // extractForCyclingDisplay(20)
                        label.add(Component.literal("-").append(items[shown].getHoverName()));
                    }
                });
                label.add(Component.literal("Click to change!").withStyle(net.minecraft.ChatFormatting.RED));
                K3BGui.customInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 71, this.topPos + 17, 12, 12, mouseX, mouseY, label);
            }
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 52, this.topPos + 18, 7, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.inputFluid1(), this.menu.inputAmount1(), this.menu.inputCapacity1()), this.menu.inputFluid1(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 61, this.topPos + 18, 7, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.inputFluid2(), this.menu.inputAmount2(), this.menu.inputCapacity2()), this.menu.inputFluid2(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 126, this.topPos + 18, 16, 52)) {
            HbmFluidTooltipRenderer.render(guiGraphics, this.font, HbmFluidTooltip.forTank(this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity()), this.menu.outputFluid(), mouseX, mouseY);
        }
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
