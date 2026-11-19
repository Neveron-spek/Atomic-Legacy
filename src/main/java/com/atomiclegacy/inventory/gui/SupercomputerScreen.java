package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.SupercomputerMenu;
import com.atomiclegacy.inventory.recipes.SupercomputerRecipe;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Exact 176x211 GUIMachineSuperComputer layout. */
public final class SupercomputerScreen extends AbstractContainerScreen<SupercomputerMenu> {
    private static final ResourceLocation TEXTURE =
            AtomicLegacy.id("textures/gui/processing/gui_supercomputer.png");

    public SupercomputerScreen(SupercomputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 211;
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
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 54, 52, 16,
                this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity(), this.menu.inputPressure());
        E2TankGui.renderTankInfo(graphics, this.font, mouseX, mouseY, this.leftPos + 80, this.topPos + 54, 52, 16,
                this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity(), this.menu.outputPressure());
        if (hoverLegacy(152, 18, 16, 61, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
        if (hoverLegacy(7, 80, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<SupercomputerRecipe>> selected = this.menu.selectedRecipe();
            if (selected.isPresent()) LegacyRecipeTooltip.render(graphics, this.font, SupercomputerRecipeSelectorScreen.describeRecipe(selected.get()), mouseX, mouseY);
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
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);

        int power = this.menu.powerScaled(61);
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - power,
                    176, 61 - power, 16, power, 256, 256);
        }

        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 81,
                    176, 61, progress, 16, 256, 256);
        }

        if (this.menu.processing()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 76,
                    195, 0, 3, 6, 256, 256);
            graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 76,
                    195, 0, 3, 6, 256, 256);
        } else if (this.menu.selectedRecipe().isPresent()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 76,
                    192, 0, 3, 6, 256, 256);
            if (this.menu.power() >= this.menu.consumption()) {
                graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 76,
                        192, 0, 3, 6, 256, 256);
            }
        }

        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        graphics.renderItem(icon, this.leftPos + 8, this.topPos + 81); // [parity g2] renderItem: no stack size overlay

        renderInputGhosts(graphics);
        // [parity g2] renderTank(x, bottom 70, 52, 16, orientation 1)
        E2TankGui.renderTank(graphics, this.leftPos + 8, this.topPos + 70, 52, 16, 1,
                this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity());
        E2TankGui.renderTank(graphics, this.leftPos + 80, this.topPos + 70, 52, 16, 1,
                this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoverLegacy(7, 80, 18, 18, mouseX, mouseY)) {
            K3GuiText.click(); // [parity g2] GuiInfoContainerProcessor#mouseClicked
            if (this.minecraft != null) {
                this.minecraft.setScreen(new SupercomputerRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderInputGhosts(GuiGraphics graphics) {
        Optional<RecipeHolder<SupercomputerRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) {
            return;
        }
        List<SupercomputerRecipe.CountedIngredient> ingredients = selected.get().value().ingredients();
        for (int index = 0; index < ingredients.size(); index++) {
            Slot slot = this.menu.getSlot(2 + index);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack[] candidates = ingredients.get(index).ingredient().getItems();
            if (candidates.length == 0) {
                continue;
            }
            // [parity g2] extractForCyclingDisplay(20), full item, then slot texture at alpha 0.5 on top
            ItemStack ghost = candidates[(int) (System.currentTimeMillis() % (1000L * candidates.length) / 1000L)].copy();
            ghost.setCount(ingredients.get(index).count());
            graphics.renderItem(ghost, this.leftPos + slot.x, this.topPos + slot.y);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
            graphics.blit(TEXTURE, this.leftPos + slot.x, this.topPos + slot.y,
                    slot.x, slot.y, 16, 16, 256, 256);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    private void drawHorizontalFluid(
            GuiGraphics graphics, int x, int y, int width, int height, HbmFluidDefinition fluid) {
        if (width <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = AtomicLegacy.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + y,
                    this.leftPos + x + width, this.topPos + y + height,
                    0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            graphics.blit(texture, this.leftPos + x + tileX, this.topPos + y,
                    0, 0, tileWidth, height, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** [parity g2] GuiInfoContainer hover test: x <= mouse < x + w, y < mouse <= y + h. */
    private boolean hoverLegacy(int x, int y, int width, int height, double mouseX, double mouseY) {
        return K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height);
    }
}
