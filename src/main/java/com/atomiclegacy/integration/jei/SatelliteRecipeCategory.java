package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** [parity S3: JEI] SatelliteHandler: gui_nei_anvil.png, input 12,24, sat_dock 30,31, 6x3 output grid at 48,6. */
public final class SatelliteRecipeCategory implements IRecipeCategory<ParityS3JeiRecipes.SatelliteDisplay> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/jei/gui_nei_anvil.png");
    private final IDrawable background;
    private final IDrawable icon;

    public SatelliteRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.SAT_DOCK.get()));
    }

    @Override public RecipeType<ParityS3JeiRecipes.SatelliteDisplay> getRecipeType() { return ParityS3Jei.SATELLITE; }
    @Override public Component getTitle() { return Component.literal("Satellite"); }
    @Override public IDrawable getBackground() { return this.background; }
    @Override public IDrawable getIcon() { return this.icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ParityS3JeiRecipes.SatelliteDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 24).addItemStack(recipe.satellite());
        List<ItemStack> out = recipe.outputs();
        int overflowCount = out.size() / 18;
        for (int i = 0; i < Math.min(out.size(), 18); i++) {
            List<ItemStack> stacks = new ArrayList<>();
            List<Component> lines = new ArrayList<>();
            for (int j = 0; j < overflowCount + 1 && j * 18 + i < out.size(); j++) {
                stacks.add(out.get(j * 18 + i));
                lines.add(recipe.chances().get(j * 18 + i));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 48 + 18 * (i % 6), 6 + 18 * (i / 6))
                    .addItemStacks(stacks)
                    .addRichTooltipCallback((view, tooltip) -> {
                        ItemStack shown = view.getDisplayedIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
                        for (int k = 0; k < stacks.size(); k++) {
                            if (ItemStack.isSameItemSameComponents(stacks.get(k), shown)) {
                                tooltip.add(lines.get(k));
                                return;
                            }
                        }
                    });
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 30, 31).addItemStack(new ItemStack(HbmBlocks.SAT_DOCK.get()));
    }

    @Override
    public void draw(ParityS3JeiRecipes.SatelliteDisplay recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 11, 23, 113, 105, 18, 18);
        graphics.blit(TEXTURE, 47, 5, 5, 87, 108, 54);
        graphics.blit(TEXTURE, 29, 14, 131, 96, 18, 36);
    }
}
