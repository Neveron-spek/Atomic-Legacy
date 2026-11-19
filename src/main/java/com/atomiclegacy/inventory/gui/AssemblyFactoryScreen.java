package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.AssemblyFactoryBlockEntity;
import com.atomiclegacy.inventory.container.AssemblyFactoryMenu;
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

public class AssemblyFactoryScreen extends AbstractContainerScreen<AssemblyFactoryMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_assembly_factory.png");

    public AssemblyFactoryScreen(AssemblyFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 240;
        this.inventoryLabelX = 33;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 113 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            if (isHovering(105 + left, 20 + top, 5, 32, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(module), mouseX, mouseY);
                return;
            }
            if (isHovering(105 + left, 54 + top, 5, 16, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(module + AssemblyFactoryBlockEntity.MODULE_COUNT), mouseX, mouseY);
                return;
            }
            if (isHovering(6 + left, 53 + top, 18, 18, mouseX, mouseY)) {
                Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
                if (selectedRecipe.isPresent()) {
                    LegacyRecipeTooltip.render(guiGraphics, this.font, AssemblyRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
                } else {
                    guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(net.minecraft.ChatFormatting.YELLOW)), mouseX, mouseY);
                }
                return;
            }
        }

        if (isHovering(232, 149, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(8), mouseX, mouseY);
            return;
        }
        if (isHovering(241, 149, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(9), mouseX, mouseY);
            return;
        }
        if (isHovering(234, 18, 16, 92, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 256, 140);
        guiGraphics.blit(TEXTURE, this.leftPos + 25, this.topPos + 140, 25, 140, 231, 100);

        int energyPixels = this.menu.energyScaled(92);
        if (energyPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 234, this.topPos + 110 - energyPixels, 0, 232 - energyPixels, 16, energyPixels);
        }

        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            // [parity e2] (int) Math.ceil(37 * progress)
            int progressPixels = Math.min(37, (int) Math.ceil(37 * (double) this.menu.progress(module) / this.menu.workTime(module)));
            if (progressPixels > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 45 + left, this.topPos + 63 + top, 0, 240, progressPixels, 6);
            }

            if (this.menu.isWorking(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 45 + left, this.topPos + 55 + top, 4, 236, 4, 4);
                guiGraphics.blit(TEXTURE, this.leftPos + 53 + left, this.topPos + 55 + top, 4, 236, 4, 4);
            } else if (this.menu.hasRecipe(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 45 + left, this.topPos + 55 + top, 0, 236, 4, 4);
                // [parity e2] right LED also needs canCool(): water >= 100 && lps <= max - 100
                if (this.menu.energy() >= this.menu.currentDemand(module)
                        && this.menu.tankAmount(8) >= 100 && this.menu.tankAmount(9) <= this.menu.tankCapacity(9) - 100) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 53 + left, this.topPos + 55 + top, 0, 236, 4, 4);
                }
            }

            renderTemplateIcon(guiGraphics, module, left, top);
            renderRecipeGhosts(guiGraphics, module);
            // [parity e2] FluidTank#renderTank
            tank(guiGraphics, 105 + left, 52 + top, 5, 32, module);
            tank(guiGraphics, 105 + left, 70 + top, 5, 16, module + AssemblyFactoryBlockEntity.MODULE_COUNT);
        }
        tank(guiGraphics, 232, 201, 7, 52, 8);
        tank(guiGraphics, 241, 201, 7, 52, 9);
    }

    private void tank(GuiGraphics guiGraphics, int x, int bottomY, int width, int height, int tank) {
        E2TankGui.renderTank(guiGraphics, this.leftPos + x, this.topPos + bottomY, width, height,
                this.menu.tankFluid(tank), this.menu.tankAmount(tank), this.menu.tankCapacity(tank));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            if (isHovering(6 + left, 53 + top, 18, 18, mouseX, mouseY)) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new AssemblyFactoryRecipeSelectorScreen(this, this.menu, module));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics, int module, int left, int top) {
        ItemStack icon = this.menu.selectedRecipe(module)
                .map(AssemblyRecipeSelectorScreen::displayIcon)
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 7 + left, this.topPos + 54 + top);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics, int module) {
        Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
        if (selectedRecipe.isEmpty()) {
            return;
        }

        List<AssemblyMachineRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().ingredients();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = AssemblyFactoryBlockEntity.inputSlotStart(module) + ingredientIndex;
            if (slotIndex >= AssemblyFactoryBlockEntity.inputSlotStart(module) + AssemblyFactoryBlockEntity.MODULE_INPUT_COUNT) {
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

    private List<Component> fluidTooltip(int tank) {
        return HbmFluidTooltip.forTank(
                this.menu.tankFluid(tank),
                this.menu.tankAmount(tank),
                this.menu.tankCapacity(tank),
                this.menu.tankPressure(tank)
        );
    }
}
