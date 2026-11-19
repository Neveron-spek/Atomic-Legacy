package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Machine recipe types have no vanilla recipe-book tab (1.7.10 had no recipe book at all). Without a category
 * finder ClientRecipeBook logs "Unknown recipe category" for every machine recipe on each world join
 * (thousands of WARN lines). They are mapped to UNKNOWN, which is exactly what vanilla falls back to.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HbmRecipeBookCategories {
    private HbmRecipeBookCategories() {
    }

    @SubscribeEvent
    public static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        for (DeferredHolder<RecipeType<?>, ? extends RecipeType<?>> type : HbmRecipeTypes.RECIPE_TYPES.getEntries()) {
            event.registerRecipeCategoryFinder(type.get(), recipe -> RecipeBookCategories.UNKNOWN);
        }
    }
}
