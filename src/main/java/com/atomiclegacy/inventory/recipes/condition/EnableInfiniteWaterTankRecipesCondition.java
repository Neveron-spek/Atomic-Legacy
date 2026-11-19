package com.atomiclegacy.inventory.recipes.condition;

import com.mojang.serialization.MapCodec;
import com.atomiclegacy.config.HbmConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class EnableInfiniteWaterTankRecipesCondition implements ICondition {
    public static final EnableInfiniteWaterTankRecipesCondition INSTANCE = new EnableInfiniteWaterTankRecipesCondition();
    public static final MapCodec<EnableInfiniteWaterTankRecipesCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private EnableInfiniteWaterTankRecipesCondition() {
    }

    @Override
    public boolean test(IContext context) {
        // [parity b1: inf_water] 1.7.10 CraftingManager: the two recipes exist unless GeneralConfig.enable528.
        return !HbmConfig.ENABLE_528_MODE.get(); // no config switch in 1.7.10 (stale port key false hid the recipes)
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "enableInfiniteWaterTankRecipes";
    }
}
