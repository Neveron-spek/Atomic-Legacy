package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ChemicalPlantBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.ChemicalPlantMenu;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChemicalPlantScreen extends AbstractContainerScreen<ChemicalPlantMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_chemplant.png");

    public ChemicalPlantScreen(ChemicalPlantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity g1: GUIMachineChemicalPlant.drawScreen]
        for (int index = 0; index < 3; index++) {
            fluidTooltip(guiGraphics, index, 8 + index * 18, mouseX, mouseY);
            fluidTooltip(guiGraphics, index + 3, 80 + index * 18, mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 61)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 7, this.topPos + 125, 18, 18)) {
            Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe();
            if (selectedRecipe.isPresent()) {
                LegacyRecipeTooltip.render(guiGraphics, this.font, ChemicalPlantRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energyPixels = this.menu.energyScaled(61);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 152,
                    this.topPos + 79 - energyPixels,
                    176,
                    61 - energyPixels,
                    16,
                    energyPixels
            );
        }

        int progressPixels = this.menu.progressScaled(70);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61 + (this.menu.restrictedMode() ? 16 : 0), progressPixels, 16); // [parity U2: ROR]
        }

        if (this.menu.isWorking()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (this.menu.hasRecipe()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (this.menu.energy() >= this.menu.recipePower()) { // [parity K3] renderStandardLEDs: power >= recipe.power
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        renderTemplateIcon(guiGraphics);
        renderRecipeGhosts(guiGraphics);
        renderFluidTanks(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 7, this.topPos + 125, 18, 18)) {
            K3GuiText.click(); // GuiInfoContainerProcessor.mouseClicked
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ChemicalPlantRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 8, this.topPos + 126);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics) {
        Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe();
        if (selectedRecipe.isEmpty()) {
            return;
        }

        List<ChemicalPlantRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().inputItems();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = ChemicalPlantBlockEntity.SOLID_INPUT_START + ingredientIndex;
            if (slotIndex >= ChemicalPlantBlockEntity.SOLID_INPUT_END) {
                return;
            }
            Slot slot = this.menu.getSlot(slotIndex);
            if (slot.hasItem()) {
                continue;
            }

            ItemStack ghost = cyclingIngredient(ingredients.get(ingredientIndex));
            if (!ghost.isEmpty()) {
                renderGhostItem(guiGraphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, slot.x, slot.y);
            }
        }
    }

    /** [parity g1] GuiInfoContainerProcessor.renderRecipeIcons: the item, then the slot texture at alpha 0.5 above it. */
    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack ghost, int x, int y, int slotU, int slotV) {
        guiGraphics.renderItem(ghost, x, y);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
        guiGraphics.blit(TEXTURE, x, y, slotU, slotV, 16, 16);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    /** AStack.extractForCyclingDisplay(20): one candidate per second. */
    private static ItemStack cyclingIngredient(ChemicalPlantRecipe.CountedIngredient ingredient) {
        ItemStack[] stacks = ingredient.ingredient().getItems();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack display = stacks[(int) (System.currentTimeMillis() % (1000L * stacks.length) / 1000L)].copy();
        display.setCount(ingredient.count());
        return display;
    }

    private void renderFluidTanks(GuiGraphics guiGraphics) {
        for (int index = 0; index < 3; index++) {
            E2TankGui.renderTank(guiGraphics, this.leftPos + 8 + index * 18, this.topPos + 52, 16, 34,
                    this.menu.tankFluid(index), this.menu.tankAmount(index), this.menu.tankCapacity());
            E2TankGui.renderTank(guiGraphics, this.leftPos + 80 + index * 18, this.topPos + 52, 16, 34,
                    this.menu.tankFluid(index + 3), this.menu.tankAmount(index + 3), this.menu.tankCapacity());
        }
    }

    private void fluidTooltip(GuiGraphics guiGraphics, int tank, int x, int mouseX, int mouseY) {
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + x, this.topPos + 18, 16, 34,
                this.menu.tankFluid(tank), this.menu.tankAmount(tank), this.menu.tankCapacity(), this.menu.tankPressure(tank));
    }
}
