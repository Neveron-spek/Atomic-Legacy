package com.atomiclegacy.radiation;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * [parity K2: chunk radiation] Port of 1.7.10 com.hbm.handler.radiation.ChunkRadiationHandlerSimple (commit 936c60c),
 * the handler ChunkRadiationManager uses by default (RadiationConfig.enablePRISM = false, enableChunkRads = true).
 *
 * <p>One float per X/Z chunk, clamped to 0..100 000 on read/write; writes into unloaded chunks are dropped
 * (world.blockExists); updateSystem() (every 20 server ticks) spreads 60 % / 7.5 % / 2.5 % into the 3x3
 * neighbourhood, decaying {@code max(0, (old + v * p) * 0.99 - 0.05)} for chunks that were already tracked and
 * overwriting chunks that were not; handleWorldDestruction() runs every server tick. The value is stored in the
 * chunk NBT under the original key {@code hfr_simple_radiation}. Keys use the 1.7.10 ChunkCoordIntPair hash so
 * the HashMap iteration order matches the original. No resistance/shielding exists in this handler.</p>
 */
public final class ChunkRadiationData {
    /** ChunkRadiationHandlerSimple.maxRad. */
    public static final float MAX_RAD = 100_000F;
    /** ChunkRadiationHandlerSimple.NBT_KEY_CHUNK_RADIATION. */
    public static final String NBT_KEY = "hfr_simple_radiation";

    private static final Map<ServerLevel, ChunkRadiationData> PER_WORLD = new IdentityHashMap<>();

    /** SimpleRadiationPerWorld.radiation. */
    final HashMap<Coord, Float> radiation = new HashMap<>();
    private final ServerLevel level;

    private ChunkRadiationData(ServerLevel level) {
        this.level = level;
    }

    /** 1.7.10 ChunkCoordIntPair: same equals and hashCode. */
    record Coord(int x, int z) {
        @Override
        public int hashCode() {
            int i = 1664525 * x + 1013904223;
            int j = 1664525 * (z ^ -559038737) + 1013904223;
            return i ^ j;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Coord c && c.x == x && c.z == z;
        }
    }

    public static synchronized ChunkRadiationData get(ServerLevel level) {
        return PER_WORLD.computeIfAbsent(level, ChunkRadiationData::new);
    }

    /** Diagnostics: a map that is not registered for the world (used to run one world-destruction pass in isolation). */
    static ChunkRadiationData detached(ServerLevel level) {
        return new ChunkRadiationData(level);
    }

    /** receiveWorldUnload. */
    static synchronized void unload(ServerLevel level) {
        PER_WORLD.remove(level);
    }

    static synchronized ChunkRadiationData[] all() {
        return PER_WORLD.values().toArray(new ChunkRadiationData[0]);
    }

    static synchronized void clearAll() {
        PER_WORLD.clear();
    }

    private static float clamp(float v) {
        return v < 0F ? 0F : Math.min(v, MAX_RAD);
    }

    // ------------------------------------------------------------------ ChunkRadiationHandler API

    public double getRadiation(BlockPos pos) {
        return getRadiation(pos.getX() >> 4, pos.getZ() >> 4);
    }

    float getRadiation(int chunkX, int chunkZ) {
        // [parity V3: RadiationConfig.enableChunkRads] without receiveWorldLoad the proxy has no per-world map: 0 RAD
        if (!com.atomiclegacy.config.HbmParityV3Config.enableChunkRads()) {
            return 0F;
        }
        Float rad = radiation.get(new Coord(chunkX, chunkZ));
        return rad == null ? 0F : clamp(rad);
    }

    public double getChunkRadiation(long chunkKey) {
        return getRadiation(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
    }

    public void setRadiation(BlockPos pos, double rad) {
        setRadiation(pos.getX() >> 4, pos.getZ() >> 4, (float) rad);
    }

    void setRadiation(int chunkX, int chunkZ, float rad) {
        if (!com.atomiclegacy.config.HbmParityV3Config.enableChunkRads()) { // [parity V3: RadiationConfig.enableChunkRads] writes are dropped
            return;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ); // world.blockExists(x, 0, z)
        if (chunk != null) {
            radiation.put(new Coord(chunkX, chunkZ), clamp(rad));
            chunk.setUnsaved(true); // isModified = true
        }
    }

    /** incrementRad. */
    public void incrementRadiation(BlockPos pos, double amount) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        setRadiation(cx, cz, getRadiation(cx, cz) + (float) amount);
    }

    /**
     * Source compatibility only: 1.7.10 incrementRad has no per-source cap, so {@code max} (invented by the port
     * base) is ignored. [parity K2]
     */
    @Deprecated
    public void incrementRadiation(BlockPos pos, double amount, double max) {
        incrementRadiation(pos, amount);
    }

    /** decrementRad. */
    public void decrementRadiation(BlockPos pos, double amount) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        setRadiation(cx, cz, Math.max(getRadiation(cx, cz) - (float) amount, 0F));
    }

    public void clearRadiation(BlockPos pos) {
        setRadiation(pos, 0.0D);
    }

    /** clearSystem. */
    public void clearSystem() {
        radiation.clear();
    }

    boolean isEmpty() {
        return radiation.isEmpty();
    }

    // ------------------------------------------------------------------ chunk NBT

    /** receiveChunkLoad: the stored value (0 when absent) is always put, like the original. */
    void receiveChunkLoad(int chunkX, int chunkZ, float stored) {
        radiation.put(new Coord(chunkX, chunkZ), stored);
    }

    /** receiveChunkSave. */
    float valueForSave(int chunkX, int chunkZ) {
        Float val = radiation.get(new Coord(chunkX, chunkZ));
        return val == null ? 0F : val;
    }

    // ------------------------------------------------------------------ updateSystem

    /** ChunkRadiationHandlerSimple.updateSystem for this world. */
    void updateSystem() {
        HashMap<Coord, Float> buff = new HashMap<>(radiation);
        radiation.clear();
        RandomSource rand = level.random;

        for (Map.Entry<Coord, Float> chunk : buff.entrySet()) {
            if (chunk.getValue() == 0) {
                continue;
            }
            Coord coord = chunk.getKey();

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int type = Math.abs(i) + Math.abs(j);
                    float percent = type == 0 ? 0.6F : type == 1 ? 0.075F : 0.025F;
                    Coord newCoord = new Coord(coord.x + i, coord.z + j);

                    if (buff.containsKey(newCoord)) {
                        Float val = radiation.get(newCoord);
                        float rad = val == null ? 0 : val;
                        float newRad = rad + chunk.getValue() * percent;
                        // MathHelper.clamp_float(0F, newRad * 0.99F - 0.05F, maxRad) with the original argument
                        // order: max(0, x), the upper cap is never applied here.
                        float decayed = newRad * 0.99F - 0.05F;
                        radiation.put(newCoord, 0F < decayed ? decayed : 0F);
                    } else {
                        radiation.put(newCoord, chunk.getValue() * percent);
                    }

                    float rad = radiation.get(newCoord);
                    if (rad > com.atomiclegacy.config.HbmParityV3Config.fogRad() && rand.nextInt(com.atomiclegacy.config.HbmParityV3Config.fogCh()) == 0 // [parity V3: RadiationConfig.fogRad/fogCh]
                            && level.getChunkSource().getChunkNow(coord.x, coord.z) != null) {
                        int x = coord.x * 16 + rand.nextInt(16);
                        int z = coord.z * 16 + rand.nextInt(16);
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + rand.nextInt(5);
                        // effectNT "radFog"
                        level.sendParticles(HbmParticleTypes.RADIATION_FOG.get(), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }

        // 1.7.10 always saved a chunk on unload; 1.21 saves only dirty chunks, so a changed value marks the chunk.
        for (Map.Entry<Coord, Float> e : radiation.entrySet()) {
            Float before = buff.get(e.getKey());
            if (before == null || Float.compare(before, e.getValue()) != 0) {
                LevelChunk loaded = level.getChunkSource().getChunkNow(e.getKey().x, e.getKey().z);
                if (loaded != null) {
                    loaded.setUnsaved(true);
                }
            }
        }
    }

    // ------------------------------------------------------------------ handleWorldDestruction

    /** ChunkRadiationHandlerSimple.handleWorldDestruction for this world (count 10, threshold 10, chunks 5). */
    @SuppressWarnings("unchecked")
    void handleWorldDestruction() {
        int count = 10;
        int threshold = 10;
        int chunks = 5;

        Object[] entries = radiation.entrySet().toArray();
        if (entries.length == 0) {
            return;
        }
        RandomSource rand = level.random;
        Block wasteEarth = block("waste_earth");
        Block wasteLeaves = block("waste_leaves");
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int c = 0; c < chunks; c++) {
            Map.Entry<Coord, Float> randEnt = (Map.Entry<Coord, Float>) entries[rand.nextInt(entries.length)];
            Coord coords = randEnt.getKey();

            for (int i = 0; i < count; i++) {
                if (randEnt.getValue() < threshold) {
                    continue;
                }
                if (level.getChunkSource().getChunkNow(coords.x, coords.z) == null) {
                    continue;
                }
                for (int a = 0; a < 16; a++) {
                    for (int b = 0; b < 16; b++) {
                        if (rand.nextInt(3) != 0) {
                            continue;
                        }
                        // coords.getCenterXPos() - 8 + a == chunkX * 16 + a
                        int x = coords.x * 16 + a;
                        int z = coords.z * 16 + b;
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - rand.nextInt(2);
                        pos.set(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        if (state.is(Blocks.GRASS_BLOCK)) {
                            level.setBlock(pos, wasteEarth.defaultBlockState(), 3);
                        } else if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN)) {
                            // Blocks.tallgrass (grass/fern metadata); double plants were a separate block
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        } else if (isLeaves(state) && !state.is(wasteLeaves)) {
                            if (rand.nextInt(7) <= 5) {
                                level.setBlock(pos, wasteLeaves.defaultBlockState(), 3);
                            } else {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
    }
}
