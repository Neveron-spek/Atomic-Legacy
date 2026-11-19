package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.CrucibleBlockEntity;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.inventory.container.CrucibleMenu;
import com.atomiclegacy.inventory.recipes.CrucibleRecipe;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CrucibleScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<CrucibleMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_crucible.png");

    public CrucibleScreen(CrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity g1: GUICrucible.drawScreen - "%,d / %,dTU" without a space]
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 125, this.topPos + 81, 34, 7)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format(Locale.US, "%,d", this.menu.progress()) + " / " + String.format(Locale.US, "%,d", CrucibleBlockEntity.PROCESS_TIME) + "TU"
            )), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 125, this.topPos + 90, 34, 7)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format(Locale.US, "%,d", this.menu.heat()) + " / " + String.format(Locale.US, "%,d", CrucibleBlockEntity.MAX_HEAT) + "TU"
            )), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 106, this.topPos + 80, 18, 18)) {
            Optional<RecipeHolder<CrucibleRecipe>> recipe = this.menu.selectedRecipe();
            if (recipe.isPresent()) LegacyRecipeTooltip.render(guiGraphics, this.font, CrucibleRecipeSelectorScreen.describeRecipe(recipe.get()), mouseX, mouseY);
            else guiGraphics.renderComponentTooltip(this.font, LegacyRecipeTooltip.setRecipe(), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 16, this.topPos + 17, 36, 81)) {
            guiGraphics.renderComponentTooltip(this.font, materialStackTooltip(this.menu.wasteStack()), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 61, this.topPos + 17, 36, 81)) {
            guiGraphics.renderComponentTooltip(this.font, materialStackTooltip(this.menu.recipeStack()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // [parity g1: GUICrucible - white title]
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.progressScaled(33);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 82, 176, 0, progress, 5);
        }

        int heat = this.menu.heatScaled(33);
        if (heat > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 91, 176, 5, heat, 5);
        }

        drawStack(guiGraphics, this.menu.recipeStack(), CrucibleBlockEntity.RECIPE_CAPACITY, 62, 97);
        drawStack(guiGraphics, this.menu.wasteStack(), CrucibleBlockEntity.WASTE_CAPACITY, 17, 97);
        renderRecipeIcon(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 106, this.topPos + 80, 18, 18)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CrucibleRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** [parity g1] GUICrucible.drawStack: tinted opaque layer, then a white 30 % additive (SRC_ALPHA, ONE) layer. */
    private void drawStack(GuiGraphics guiGraphics, List<FoundryMaterialStack> stacks, int capacity, int x, int y) {
        if (stacks.isEmpty() || capacity <= 0) {
            return;
        }
        int lastHeight = 0;
        int lastAmount = 0;
        for (FoundryMaterialStack stack : stacks) {
            int targetHeight = (lastAmount + stack.amount()) * 79 / capacity;
            if (lastHeight == targetHeight) {
                continue; // the reference skips without adding the amount
            }
            int offset = stack.material().behavior() == com.atomiclegacy.foundry.FoundryMaterial.SmeltingBehavior.ADDITIVE ? 34 : 0;
            int color = stack.material().moltenColor();
            int height = targetHeight - lastHeight;
            guiGraphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
            guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y - targetHeight, 176 + offset, 89 - targetHeight, 34, height);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.3F);
            guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y - targetHeight, 176 + offset, 89 - targetHeight, 34, height);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            lastAmount += stack.amount();
            lastHeight = targetHeight;
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderRecipeIcon(GuiGraphics guiGraphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().icon().copy())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        if (icon.isEmpty()) {
            return;
        }
        guiGraphics.renderItem(icon, this.leftPos + 107, this.topPos + 81);
    }

    private List<Component> materialStackTooltip(List<FoundryMaterialStack> stacks) {
        if (stacks.isEmpty()) {
            return List.of(Component.translatable("gui.atomiclegacy.empty").withStyle(ChatFormatting.RED));
        }
        return stacks.stream()
                .map(stack -> Component.translatable(stack.material().translationKey())
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                                ? Component.literal((stack.amount() * 2) + "mB")
                                : ScrapsItem.formatAmountComponent(stack.amount())))
                .map(Component.class::cast)
                .toList();
    }
}
