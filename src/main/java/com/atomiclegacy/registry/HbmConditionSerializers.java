package com.atomiclegacy.registry;

import com.mojang.serialization.MapCodec;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.condition.Enable528ModeCondition;
import com.atomiclegacy.inventory.recipes.condition.EnableExpensiveModeCondition;
import com.atomiclegacy.inventory.recipes.condition.EnableInfiniteWaterTankRecipesCondition;
import com.atomiclegacy.inventory.recipes.condition.LessBullshitModeCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class HbmConditionSerializers {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, AtomicLegacy.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<Enable528ModeCondition>> ENABLE_528_MODE =
            CONDITION_SERIALIZERS.register("enable_528_mode", () -> Enable528ModeCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<EnableExpensiveModeCondition>> ENABLE_EXPENSIVE_MODE =
            CONDITION_SERIALIZERS.register("enable_expensive_mode", () -> EnableExpensiveModeCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<EnableInfiniteWaterTankRecipesCondition>> ENABLE_INFINITE_WATER_TANK_RECIPES =
            CONDITION_SERIALIZERS.register("enable_infinite_water_tank_recipes", () -> EnableInfiniteWaterTankRecipesCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<LessBullshitModeCondition>> LESS_BULLSHIT_MODE =
            CONDITION_SERIALIZERS.register("less_bullshit_mode", () -> LessBullshitModeCondition.CODEC);

    private HbmConditionSerializers() {
    }

    public static void register(IEventBus eventBus) {
        CONDITION_SERIALIZERS.register(eventBus);
    }
}
