package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidTraitInfo;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * [parity T4: JEI] Base for categories whose 1.7.10 handler extends NEIUniversalHandler: the recipe is converted to the
 * RecipeSet the NEI handler builds from its getRecipes() map, then laid out by {@link UniversalJeiCategory}
 * (gui_nei.png, getInputCoords/getOutputCoords, slot frames, operation frame, machine at 75,31).
 * Subclasses add the handler's drawExtras().
 */
public abstract class NeiUniversalCategory<T> implements IRecipeCategory<T> {
    private final RecipeType<T> type;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final Map<T, UniversalJeiRecipe> layouts = Collections.synchronizedMap(new WeakHashMap<>());

    protected NeiUniversalCategory(IGuiHelper helper, RecipeType<T> type, Component title, ItemStack icon) {
        this.type = type;
        this.title = title;
        this.background = helper.createDrawable(UniversalJeiCategory.TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(icon);
    }

    /** The RecipeSet of the NEI handler (InventoryUtil.extractObject of the getRecipes() key/value). */
    protected abstract UniversalJeiRecipe layout(T recipe);

    /** NEI drawExtras(). */
    protected void drawExtras(T recipe, GuiGraphics graphics, Font font) {
    }

    public final UniversalJeiRecipe cached(T recipe) {
        return this.layouts.computeIfAbsent(recipe, this::layout);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        UniversalJeiCategory.build(builder, cached(recipe));
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        UniversalJeiRecipe layout = cached(recipe);
        UniversalJeiCategory.drawFrames(graphics, layout.inputs().size(), layout.outputs().size());
        drawExtras(recipe, graphics, Minecraft.getInstance().font);
    }

    // ---- helpers shared by the T4 categories ----

    /** Block#getLocalizedName() titles. */
    static Component blockName(ItemLike block) {
        return Component.translatable(block.asItem().getDescriptionId());
    }

    static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    /** AStack#extractForNEI with the stack size of the recipe. */
    static List<ItemStack> items(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(count)).toList();
    }

    static UniversalJeiRecipe.Slot slot(Ingredient ingredient, int count) {
        return UniversalJeiRecipe.Slot.of(items(ingredient, count));
    }

    static UniversalJeiRecipe.Slot slot(ItemStack stack) {
        return UniversalJeiRecipe.Slot.of(stack.copy());
    }

    static UniversalJeiRecipe recipe(List<UniversalJeiRecipe.Slot> in, List<UniversalJeiRecipe.Slot> out, ItemStack... machines) {
        return new UniversalJeiRecipe(in, out, List.of(machines));
    }

    /** fontRenderer.drawString(text, side - getStringWidth(text), y, color). */
    static void right(GuiGraphics graphics, Font font, String text, int side, int y, int color) {
        graphics.drawString(font, text, side - font.width(text), y, color, false);
    }

    static void left(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    /** BobMathUtil.getShortNumber. */
    static String shortNumber(long value) {
        return HbmFluidTraitInfo.shortNumber(value);
    }
}
