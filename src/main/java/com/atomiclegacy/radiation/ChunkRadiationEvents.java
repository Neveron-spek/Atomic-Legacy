package com.atomiclegacy.radiation;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * [parity K2: chunk radiation] ChunkRadiationManager event hooks of 1.7.10: ServerTickEvent END drives
 * updateSystem/handleWorldDestruction, ChunkDataEvent Load/Save read and write {@code hfr_simple_radiation}.
 * World load/unload and ChunkEvent.Load are routed through RadiationEvents.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class ChunkRadiationEvents {
    private ChunkRadiationEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        HbmRadiationWorlds.serverTickEnd(event.getServer());
    }

    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (com.atomiclegacy.config.HbmParityV3Config.enableChunkRads() // [parity V3: RadiationConfig.enableChunkRads]
                && event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.chunkDataLoaded(level, chunk.getPos(), event.getData().getFloat(ChunkRadiationData.NBT_KEY));
        }
    }

    @SubscribeEvent
    public static void onChunkDataSave(ChunkDataEvent.Save event) {
        if (com.atomiclegacy.config.HbmParityV3Config.enableChunkRads() // [parity V3: RadiationConfig.enableChunkRads]
                && event.getChunk() instanceof LevelChunk chunk && event.getLevel() instanceof ServerLevel level) {
            event.getData().putFloat(ChunkRadiationData.NBT_KEY, HbmRadiationWorlds.chunkDataSaved(level, chunk.getPos()));
        }
    }
}
