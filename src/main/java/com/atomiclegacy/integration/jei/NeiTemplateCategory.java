package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * [parity T4: JEI] Base for categories of 1.7.10 handlers that extend TemplateRecipeHandler directly: the background is
 * TemplateRecipeHandler#drawBackground (getGuiTexture() at 0,0 from 5,11, 166x65); PositionedStack coordinates are used
 * as JEI slot positions unchanged.
 */
public abstract class NeiTemplateCategory<T> implements IRecipeCategory<T> {
    protected final ResourceLocation texture;
    private final RecipeType<T> type;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    protected NeiTemplateCategory(IGuiHelper helper, RecipeType<T> type, Component title, ItemStack icon, String texture) {
        this.texture = AtomicLegacy.id("textures/gui/nei/" + texture + ".png");
        this.type = type;
        this.title = title;
        this.background = helper.createDrawable(this.texture, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(icon);
    }

    /**
     * TemplateRecipeHandler#drawProgressBar(x, y, tx, ty, w, h, ticks, direction): 0 right, 1 down, 3 up, +4 drains.
     * Returns the drawable; callers draw it at x, y.
     */
    protected IDrawableAnimated progress(IGuiHelper helper, int tx, int ty, int w, int h, int ticks, int direction) {
        boolean drain = direction > 3;
        IDrawableAnimated.StartDirection start = switch (direction % 4) {
            case 0 -> IDrawableAnimated.StartDirection.LEFT;
            case 1 -> IDrawableAnimated.StartDirection.TOP;
            case 2 -> IDrawableAnimated.StartDirection.RIGHT;
            default -> IDrawableAnimated.StartDirection.BOTTOM;
        };
        return helper.createAnimatedDrawable(helper.createDrawable(this.texture, tx, ty, w, h), ticks, start, drain);
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
}
