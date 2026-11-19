package com.atomiclegacy.train;

import com.atomiclegacy.AtomicLegacy;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** [parity S1: trains] ModEventHandler.worldTick: EntityRailCarBase.updateMotion at Phase.END of every server world tick. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class TrainEvents {
    private TrainEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            RailCarEntity.updateMotion(level);
        }
    }
}
