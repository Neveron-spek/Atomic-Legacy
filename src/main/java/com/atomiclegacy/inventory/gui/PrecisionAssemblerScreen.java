package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.PrecisionAssemblerMenu;
import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.ChatFormatting;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

/** Exact 176x256 precision assembler GUI, based on GUIMachinePrecAss. */
public final class PrecisionAssemblerScreen extends AbstractContainerScreen<PrecisionAssemblerMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_precass.png");

    public PrecisionAssemblerScreen(PrecisionAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = 162;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // [parity g1: GUIMachinePrecAss.drawScreen]
        super.renderTooltip(graphics, mouseX, mouseY);
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 99, 52, 16,
                this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0));
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 80, this.topPos + 99, 52, 16,
                this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1));
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 61)) {
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), this.menu.capacity())), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 7, this.topPos + 125, 18, 18)) {
            Optional<RecipeHolder<PrecisionAssemblerRecipe>> selected = this.menu.selectedRecipe();
            if (selected.isPresent()) LegacyRecipeTooltip.render(graphics, this.font, PrecisionAssemblerRecipeSelectorScreen.describeRecipe(selected.get()), mouseX, mouseY);
            else graphics.renderComponentTooltip(this.font, LegacyRecipeTooltip.setRecipe(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int energy = this.menu.energyScaled(61);
        if (energy > 0) graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energy, 176, 61 - energy, 16, energy);
        int progress = this.menu.progressScaled(70);
        if (progress > 0) graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progress, 16);
        if (this.menu.isWorking()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (this.menu.selectedRecipe().isPresent()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            // [parity g1: right LED - power >= recipe.power]
            if (this.menu.energy() >= this.menu.selectedRecipe().get().value().power()) {
                graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }
        renderRecipeIcon(graphics);
        renderGhostIngredients(graphics);
        E2TankGui.renderTank(graphics, this.leftPos + 8, this.topPos + 115, 52, 16, 1,
                this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0));
        E2TankGui.renderTank(graphics, this.leftPos + 80, this.topPos + 115, 52, 16, 1,
                this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 7, this.topPos + 125, 18, 18) && this.minecraft != null) {
            this.minecraft.setScreen(new PrecisionAssemblerRecipeSelectorScreen(this, this.menu));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderRecipeIcon(GuiGraphics graphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(PrecisionAssemblerRecipeSelectorScreen::displayIcon)
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, this.leftPos + 8, this.topPos + 126);
        }
    }

    private void renderGhostIngredients(GuiGraphics graphics) {
        Optional<RecipeHolder<PrecisionAssemblerRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) return;
        List<PrecisionAssemblerRecipe.CountedIngredient> ingredients = selected.get().value().ingredients();
        for (int index = 0; index < ingredients.size(); index++) {
            Slot slot = this.menu.getSlot(PrecisionAssemblerMenu.INPUT_START + index);
            if (slot.hasItem()) continue;
            ItemStack ghost = cyclingIngredient(ingredients.get(index));
            if (ghost.isEmpty()) continue;
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            // [parity g1] renderItem, then the slot texture at alpha 0.5 above it (zLevel 300)
            graphics.renderItem(ghost, x, y);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
            graphics.blit(TEXTURE, x, y, slot.x, slot.y, 16, 16);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    /** AStack.extractForCyclingDisplay(20): one candidate per second. */
    private static ItemStack cyclingIngredient(PrecisionAssemblerRecipe.CountedIngredient ingredient) {
        ItemStack[] stacks = ingredient.ingredient().getItems();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack display = stacks[(int) (System.currentTimeMillis() % (1000L * stacks.length) / 1000L)].copy();
        display.setCount(ingredient.count());
        return display;
    }
}
