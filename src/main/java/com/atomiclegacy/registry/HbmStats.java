package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 1.7.10 MainRegistry custom statistics used by ported content. */
public final class HbmStats {
    private static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, AtomicLegacy.MOD_ID);

    /** MainRegistry.statBullets ("stat.ntmBullets"), incremented once per fired gun cycle. */
    public static final DeferredHolder<ResourceLocation, ResourceLocation> NTM_BULLETS =
            STATS.register("ntm_bullets", () -> AtomicLegacy.id("ntm_bullets"));

    private HbmStats() {
    }

    public static void register(IEventBus modEventBus) {
        STATS.register(modEventBus);
    }

    /** Creates the Stat entries with the default formatter (StatBasic). */
    public static void createStats() {
        Stats.CUSTOM.get(NTM_BULLETS.get(), StatFormatter.DEFAULT);
    }
}
