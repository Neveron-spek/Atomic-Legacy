package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.AssemblyMachineBlockEntity;
import com.atomiclegacy.inventory.container.AssemblyMachineMenu;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
import com.atomiclegacy.registry.HbmItems;
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

public class AssemblyMachineScreen extends AbstractContainerScreen<AssemblyMachineMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_assembler.png");

    public AssemblyMachineScreen(AssemblyMachineMenu menu, Inventory playerInventory, Component title) {
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }

        if (isHovering(8, 99, 52, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(0), mouseX, mouseY);
            return;
        }

        if (isHovering(80, 99, 52, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(1), mouseX, mouseY);
            return;
        }

        // [parity K3] GUIMachineAssemblyMachine has no progress tooltip

        if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = this.menu.selectedRecipe();
            if (selectedRecipe.isPresent()) {
                LegacyRecipeTooltip.render(guiGraphics, this.font, AssemblyRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(net.minecraft.ChatFormatting.YELLOW)), mouseX, mouseY);
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
        if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            if (this.minecraft != null) {
                K3GuiText.click(); // [parity e2] GuiInfoContainerProcessor#mouseClicked: this.click()
                this.minecraft.setScreen(new AssemblyRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(AssemblyRecipeSelectorScreen::displayIcon)
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 8, this.topPos + 126);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics) {
        Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = this.menu.selectedRecipe();
        if (selectedRecipe.isEmpty()) {
            return;
        }

        List<AssemblyMachineRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().ingredients();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = AssemblyMachineBlockEntity.INPUT_START + ingredientIndex;
            if (slotIndex >= AssemblyMachineBlockEntity.INPUT_END) {
                return;
            }
            Slot slot = this.menu.getSlot(slotIndex);
            if (slot.hasItem()) {
                continue;
            }

            ItemStack ghost = AssemblyRecipeSelectorScreen.displayIngredient(ingredients.get(ingredientIndex));
            if (!ghost.isEmpty()) {
                renderGhostItem(guiGraphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, slot.x, slot.y);
            }
        }
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack ghost, int x, int y, int slotU, int slotV) {
        // [parity e2] renderRecipeIcons: the item itself (no count), then the slot background at 50% above it (zLevel 300)
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

    private void renderFluidTanks(GuiGraphics guiGraphics) {
        // [parity e2] FluidTank#renderTank(guiLeft + x, guiTop + 115, z, 52, 16, 1)
        E2TankGui.renderTank(guiGraphics, this.leftPos + 8, this.topPos + 115, 52, 16, 1,
                this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0));
        E2TankGui.renderTank(guiGraphics, this.leftPos + 80, this.topPos + 115, 52, 16, 1,
                this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1));
    }

    private List<Component> fluidTooltip(int tank) {
        return HbmFluidTooltip.forTank(
                this.menu.tankFluid(tank),
                this.menu.tankAmount(tank),
                this.menu.tankCapacity(tank),
                this.menu.tankPressure(tank)
        );
    }
}
