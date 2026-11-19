package com.atomiclegacy.explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atomiclegacy.AtomicLegacy;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * [parity H3] Next-tick detonation queue. 1.7.10 explosives hit by another blast spawned an
 * {@code EntityTNTPrimedBase} with fuse 0, which exploded on its first update; blocks that have no primed
 * entity kind in the port (wall charges, plastic explosive blocks) use this queue for the same one-tick delay,
 * which also keeps long chains from recursing inside one explosion.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class LegacyDeferredDetonations {

    private static final Map<ResourceKey<Level>, List<Runnable>> QUEUE = new HashMap<>();

    private LegacyDeferredDetonations() {
    }

    public static void defer(ServerLevel level, Runnable detonation) {
        QUEUE.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(detonation);
    }

    /** Runs everything queued so far; detonations queued while draining wait for the next call. */
    public static int drain(ServerLevel level) {
        List<Runnable> pending = QUEUE.remove(level.dimension());
        if (pending == null) {
            return 0;
        }
        for (Runnable detonation : pending) {
            detonation.run();
        }
        return pending.size();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            drain(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();
    }
}
