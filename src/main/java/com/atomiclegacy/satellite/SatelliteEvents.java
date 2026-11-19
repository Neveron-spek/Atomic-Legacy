package com.atomiclegacy.satellite;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Advances the small persisted satellite registry independently of loaded machines. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class SatelliteEvents {
    private SatelliteEvents() {
    }

    @SubscribeEvent
    // [parity P2: satellites] ModEventHandler world tick, Phase.START: satellites tick,
    // then SatelliteRayScan.updateSystem once per second (time % 20 == 10).
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SatelliteSavedData.get(level).tick(level);
            if (level.getGameTime() % 20L == 10L) {
                SatelliteEventTracker.updateRays(level);
            }
        }
    }
}
