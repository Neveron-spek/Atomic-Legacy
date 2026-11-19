package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.RockMillMenu;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public final class RockMillScreen extends AbstractContainerScreen<RockMillMenu> {
    private static final ResourceLocation TEXTURE =
            AtomicLegacy.id("textures/gui/processing/gui_rockmill.png");

    public RockMillScreen(RockMillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // [parity g2] FluidTank#renderTankInfo, drawElectricityInfo
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 63, 52, 16,
                this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity());
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 80, this.topPos + 63, 52, 16,
                this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity());
        if (hoverLegacy(152, 18, 16, 71, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
        if (hoverLegacy(7, 89, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<RockMillRecipe>> selected = this.menu.selectedRecipe();
            if (selected.isPresent()) LegacyRecipeTooltip.render(graphics, this.font, RockMillRecipeSelectorScreen.describeRecipe(selected.get()), mouseX, mouseY);
            else graphics.renderComponentTooltip(this.font, LegacyRecipeTooltip.setRecipe(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = this.menu.powerScaled(71);
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 89 - power,
                    176, 71 - power, 16, power);
        }

        if (this.menu.progress() > 0) {
            int progress = (int) Math.ceil(70.0D * this.menu.progress() / this.menu.duration()); // [parity g2] Math.ceil
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 90,
                    176, 71, progress, 16);
        }

        if (this.menu.processing()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 85, 195, 0, 3, 6);
            graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 85, 195, 0, 3, 6);
        } else if (this.menu.selectedRecipe().isPresent()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 85, 192, 0, 3, 6);
            if (this.menu.power() >= this.menu.consumption()) {
                graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 85, 192, 0, 3, 6);
            }
        }

        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        graphics.renderItem(icon, this.leftPos + 8, this.topPos + 90); // [parity g2] renderItem: no stack size overlay

        renderInputGhost(graphics);
        E2TankGui.renderTank(graphics, this.leftPos + 8, this.topPos + 79, 52, 16, 1,
                this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity());
        E2TankGui.renderTank(graphics, this.leftPos + 80, this.topPos + 79, 52, 16, 1,
                this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoverLegacy(7, 89, 18, 18, mouseX, mouseY)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new RockMillRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** [parity g2] empty input slot: cycling first input (extractForCyclingDisplay(20)), then the slot texture at 50 % alpha. */
    private void renderInputGhost(GuiGraphics graphics) {
        Optional<RecipeHolder<RockMillRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty() || selected.get().value().input().isNone() || this.menu.getSlot(2).hasItem()) {
            return;
        }
        ItemStack[] items = selected.get().value().input().ingredient().getItems();
        if (items.length == 0) {
            return;
        }
        ItemStack ghost = items[(int) (System.currentTimeMillis() % (1000L * items.length) / 1000L)].copy();
        ghost.setCount(selected.get().value().input().count());
        graphics.renderItem(ghost, this.leftPos + 8, this.topPos + 27);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
        graphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 27, 8, 27, 16, 16);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    /** [parity g2] GuiInfoContainer hover test: x <= mouse < x + w, y < mouse <= y + h. */
    private boolean hoverLegacy(int x, int y, int width, int height, double mouseX, double mouseY) {
        return K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height);
    }
}
