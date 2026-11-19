package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ChemicalFactoryBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.ChemicalFactoryMenu;
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

import java.util.List;
import java.util.Optional;

public class ChemicalFactoryScreen extends AbstractContainerScreen<ChemicalFactoryMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_chemical_factory.png");

    public ChemicalFactoryScreen(ChemicalFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 216;
        this.inventoryLabelX = 26;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 106 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity g1: GUIMachineChemicalFactory.drawScreen]
        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            for (int index = 0; index < ChemicalFactoryBlockEntity.TANKS_PER_MODULE; index++) {
                int tank = ChemicalFactoryBlockEntity.tankIndex(module, index);
                int outputTank = ChemicalFactoryBlockEntity.MODULE_COUNT * ChemicalFactoryBlockEntity.TANKS_PER_MODULE + tank;
                fluidTooltip(guiGraphics, tank, 60 + index * 5, 20 + module * 22, 3, 16, mouseX, mouseY);
                fluidTooltip(guiGraphics, outputTank, 189 + index * 5, 20 + module * 22, 3, 16, mouseX, mouseY);
            }
        }
        fluidTooltip(guiGraphics, 24, 224, 125, 7, 52, mouseX, mouseY);
        fluidTooltip(guiGraphics, 25, 233, 125, 7, 52, mouseX, mouseY);
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 224, this.topPos + 18, 16, 68)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }
        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 74, this.topPos + 19 + module * 22, 18, 18)) {
                Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
                if (selectedRecipe.isPresent()) {
                    LegacyRecipeTooltip.render(guiGraphics, this.font, ChemicalPlantRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
                } else {
                    guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 248, 116);
        guiGraphics.blit(TEXTURE, this.leftPos + 18, this.topPos + 116, 18, 116, 230, 100);

        int energyPixels = this.menu.energyScaled(68);
        if (energyPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 224, this.topPos + 86 - energyPixels, 0, 184 - energyPixels, 16, energyPixels);
        }

        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            int progressPixels = this.menu.progressScaled(module, 22);
            if (progressPixels > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 29 + module * 22, 0, 216, progressPixels, 6);
            }

            if (this.menu.isWorking(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 21 + module * 22, 4, 222, 4, 4);
                guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 21 + module * 22, 4, 222, 4, 4);
            } else if (this.menu.hasRecipe(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 21 + module * 22, 0, 222, 4, 4);
                // [parity g1: right LED - power >= recipe.power && canCool()]
                int recipePower = this.menu.selectedRecipe(module).map(holder -> holder.value().power()).orElse(Integer.MAX_VALUE);
                if (this.menu.energy() >= recipePower && canCool()) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 21 + module * 22, 0, 222, 4, 4);
                }
            }

            renderTemplateIcon(guiGraphics, module);
            renderRecipeGhosts(guiGraphics, module);
            for (int index = 0; index < ChemicalFactoryBlockEntity.TANKS_PER_MODULE; index++) {
                int tank = ChemicalFactoryBlockEntity.tankIndex(module, index);
                E2TankGui.renderTank(guiGraphics, this.leftPos + 60 + index * 5, this.topPos + 36 + module * 22, 3, 16,
                    this.menu.tankFluid(tank), this.menu.tankAmount(tank), this.menu.tankCapacity(tank));
                int outputTank = ChemicalFactoryBlockEntity.MODULE_COUNT * ChemicalFactoryBlockEntity.TANKS_PER_MODULE + tank;
                E2TankGui.renderTank(guiGraphics, this.leftPos + 189 + index * 5, this.topPos + 36 + module * 22, 3, 16,
                    this.menu.tankFluid(outputTank), this.menu.tankAmount(outputTank), this.menu.tankCapacity(outputTank));
            }
        }

        E2TankGui.renderTank(guiGraphics, this.leftPos + 224, this.topPos + 177, 7, 52, this.menu.tankFluid(24), this.menu.tankAmount(24), this.menu.tankCapacity(24));
        E2TankGui.renderTank(guiGraphics, this.leftPos + 233, this.topPos + 177, 7, 52, this.menu.tankFluid(25), this.menu.tankAmount(25), this.menu.tankCapacity(25));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 74, this.topPos + 19 + module * 22, 18, 18)) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ChemicalFactoryRecipeSelectorScreen(this, this.menu, module));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics, int module) {
        ItemStack icon = this.menu.selectedRecipe(module)
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 75, this.topPos + 20 + module * 22);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics, int module) {
        Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
        if (selectedRecipe.isEmpty()) {
            return;
        }
        List<ChemicalPlantRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().inputItems();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = ChemicalFactoryBlockEntity.inputSlotStart(module) + ingredientIndex;
            if (slotIndex >= ChemicalFactoryBlockEntity.inputSlotStart(module) + ChemicalFactoryBlockEntity.MODULE_ITEM_COUNT) {
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

    private void fluidTooltip(GuiGraphics guiGraphics, int tank, int x, int y, int width, int height, int mouseX, int mouseY) {
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height,
                this.menu.tankFluid(tank), this.menu.tankAmount(tank), this.menu.tankCapacity(tank), this.menu.tankPressure(tank));
    }

    /** TileEntityMachineChemicalFactory.canCool: water >= 100 and room for 100 mB of spent steam. */
    private boolean canCool() {
        return this.menu.tankAmount(24) >= 100 && this.menu.tankAmount(25) <= this.menu.tankCapacity(25) - 100;
    }
}
