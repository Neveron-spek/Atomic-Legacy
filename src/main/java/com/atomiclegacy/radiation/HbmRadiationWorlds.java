package com.atomiclegacy.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [parity K2: chunk radiation] Facade of 1.7.10 ChunkRadiationManager over {@link ChunkRadiationData}
 * (ChunkRadiationHandlerSimple). The former asynchronous solver, exposure cache and loaded-chunk filter were
 * port inventions and are gone: the system now runs synchronously on the server thread like the original.
 */
public final class HbmRadiationWorlds {
    /** ChunkRadiationManager.eggTimer. */
    private static int eggTimer = 0;
    /** RadiationConfig.worldRadEffects default. */

    /** Chunk NBT values read by the asynchronous ChunkDataEvent.Load, applied on the server thread at ChunkEvent.Load. */
    private static final Map<WorldKey, Map<Long, Float>> PENDING_LOADS = new ConcurrentHashMap<>();

    private HbmRadiationWorlds() {
    }

    /** Per-level tick: the original system ticks once per server tick (see {@link #serverTickEnd}). */
    public static void tick(ServerLevel level) {
    }

    /** ChunkRadiationManager.updateSystem (ServerTickEvent END). */
    public static void serverTickEnd(MinecraftServer server) {
        if (!com.atomiclegacy.config.HbmParityV3Config.enableChunkRads()) { // [parity V3: RadiationConfig.enableChunkRads] ChunkRadiationManager.updateSystem
            return;
        }
        eggTimer++;
        if (eggTimer >= 20) {
            for (ChunkRadiationData data : ChunkRadiationData.all()) {
                data.updateSystem();
            }
            eggTimer = 0;
        }
        if (com.atomiclegacy.config.HbmParityV3Config.worldRadEffects()) { // [parity V3: RadiationConfig.worldRadEffects]
            for (ChunkRadiationData data : ChunkRadiationData.all()) {
                data.handleWorldDestruction();
            }
        }
    }

    public static double getRadiation(ServerLevel level, BlockPos pos) {
        return ChunkRadiationData.get(level).getRadiation(pos);
    }

    public static boolean hasRadiation(ServerLevel level) {
        return !ChunkRadiationData.get(level).isEmpty();
    }

    /** Called from the asynchronous ChunkDataEvent.Load for full chunks. */
    public static void chunkDataLoaded(ServerLevel level, ChunkPos pos, float stored) {
        PENDING_LOADS.computeIfAbsent(worldKey(level), k -> new ConcurrentHashMap<>()).put(pos.toLong(), stored);
    }

    /** receiveChunkSave. */
    public static float chunkDataSaved(ServerLevel level, ChunkPos pos) {
        return ChunkRadiationData.get(level).valueForSave(pos.x, pos.z);
    }

    /** receiveChunkLoad (server thread). */
    static void markChunkLoaded(ServerLevel level, ChunkPos chunkPos) {
        Map<Long, Float> pending = PENDING_LOADS.get(worldKey(level));
        Float stored = pending == null ? null : pending.remove(chunkPos.toLong());
        if (stored != null) {
            ChunkRadiationData.get(level).receiveChunkLoad(chunkPos.x, chunkPos.z, stored);
        }
    }

    /**
     * receiveChunkUnload: the original calls {@code radiation.remove(event.getChunk())} with the Chunk object
     * instead of its ChunkCoordIntPair, so nothing is removed and unloaded chunks keep spreading and decaying in
     * memory until they are loaded again from NBT. Kept as is.
     */
    static void markChunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
    }

    /** The original reads chunk radiation directly every tick; nothing is queued. */
    static void queueExposure(ServerLevel level, LivingEntity entity) {
    }

    static double getExposureRadiation(ServerLevel level, LivingEntity entity) {
        return ChunkRadiationData.get(level).getRadiation(entity.blockPosition());
    }

    /** receiveWorldUnload. */
    static void unload(ServerLevel level) {
        ChunkRadiationData.unload(level);
        PENDING_LOADS.remove(worldKey(level));
    }

    /** The default handler has no resistance; kept for callers. */
    public static void invalidateResistance(ServerLevel level, BlockPos pos) {
        RadiationShielding.invalidate(level, pos);
    }

    public static void invalidateResistanceSection(ServerLevel level, long sectionKey) {
        RadiationShielding.invalidateAll(level);
    }

    public static void clear() {
        ChunkRadiationData.clearAll();
        PENDING_LOADS.clear();
    }

    // ------------------------------------------------------------------ [parity K2: diagnostics] hooks

    /** One ChunkRadiationHandlerSimple.updateSystem pass of this world only. */
    public static void parityK2UpdateSystem(ServerLevel level) {
        ChunkRadiationData.get(level).updateSystem();
    }

    /** One handleWorldDestruction pass over a detached map holding only the given chunk. */
    public static void parityK2WorldDestruction(ServerLevel level, int chunkX, int chunkZ, float rad) {
        ChunkRadiationData data = ChunkRadiationData.detached(level);
        data.radiation.put(new ChunkRadiationData.Coord(chunkX, chunkZ), rad);
        data.handleWorldDestruction();
    }

    /** The ChunkDataEvent.Load + ChunkEvent.Load path. */
    public static void parityK2ChunkLoad(ServerLevel level, ChunkPos pos, float stored) {
        chunkDataLoaded(level, pos, stored);
        markChunkLoaded(level, pos);
    }

    /** Whether the chunk has an entry in the radiation map (also 0 entries). */
    public static boolean parityK2Tracked(ServerLevel level, int chunkX, int chunkZ) {
        return ChunkRadiationData.get(level).radiation.containsKey(new ChunkRadiationData.Coord(chunkX, chunkZ));
    }

    /** Raw stored value (no read clamp). */
    public static float parityK2Raw(ServerLevel level, int chunkX, int chunkZ) {
        Float v = ChunkRadiationData.get(level).radiation.get(new ChunkRadiationData.Coord(chunkX, chunkZ));
        return v == null ? -1F : v;
    }

    /** Removes the entry (the diagnostics start from untracked chunks). */
    public static void parityK2Forget(ServerLevel level, int chunkX, int chunkZ) {
        ChunkRadiationData.get(level).radiation.remove(new ChunkRadiationData.Coord(chunkX, chunkZ));
    }

    private static WorldKey worldKey(ServerLevel level) {
        return new WorldKey(level.getServer(), level.dimension());
    }

    private record WorldKey(MinecraftServer server, ResourceKey<Level> dimension) {
    }
}
