package com.atomiclegacy.integration.jei;

import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.helpers.IGuiHelper;

import java.util.List;

/** [parity T4: JEI] WatzRecipeHandler (NEIUniversalHandler): watz_pellet -> watz_pellet_depleted per EnumWatzType. */
public final class WatzRecipeCategory extends NeiUniversalCategory<WatzJeiRecipe> {
    public WatzRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.WATZ, blockName(HbmBlocks.WATZ.get()), stack(HbmBlocks.WATZ.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(WatzJeiRecipe recipe) {
        return recipe(List.of(slot(recipe.input())), List.of(slot(recipe.output())), stack(HbmBlocks.WATZ.get()));
    }
}
