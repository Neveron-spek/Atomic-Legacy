package com.atomiclegacy.inventory.recipes;

import com.atomiclegacy.AtomicLegacy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Global reload generation used by event-driven machine recipe caches.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class MachineRecipeCache {
    private static final AtomicLong GENERATION = new AtomicLong();

    private MachineRecipeCache() {
    }

    public static long generation() {
        return GENERATION.get();
    }

    public static void invalidateAll() {
        GENERATION.incrementAndGet();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            invalidateAll();
        }
    }
}
