package com.atomiclegacy.worldgen;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmParityV3Config;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * [parity b2: crater biomes] BiomeGenCraterBase (craterBiome "Crater", craterInnerBiome "Inner Crater",
 * craterOuterBiome "Outer Crater") and the biome change of EntityFalloutRain (getBiomeChange, WorldUtil.setBiome,
 * WorldUtil.syncBiomeChange). The biomes are data pack biomes (data/atomiclegacy/worldgen/biome) that no
 * dimension generates. 1.21 stores biomes per 4x4x4 cell instead of per column, so the original per-column rule is
 * evaluated once per 4x4 cell (at its centre column) and written to the whole height of the chunk.
 */
public final class CraterBiomes {
    public static final ResourceKey<Biome> CRATER = key("crater");
    public static final ResourceKey<Biome> CRATER_INNER = key("crater_inner");
    public static final ResourceKey<Biome> CRATER_OUTER = key("crater_outer");

    private CraterBiomes() {
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, AtomicLegacy.id(path));
    }

    /** biome instanceof BiomeGenCraterBase. */
    public static boolean isCrater(Holder<Biome> biome) {
        return biome.is(CRATER) || biome.is(CRATER_INNER) || biome.is(CRATER_OUTER);
    }

    /** EntityFalloutRain.getBiomeChange(dist, scale, original): the key of the new biome or null for no change. */
    @Nullable
    public static ResourceKey<Biome> getBiomeChange(double dist, int scale, Holder<Biome> original) {
        if (!HbmParityV3Config.enableCraterBiomes()) return null;
        if (scale >= 150 && dist < 15)
            return CRATER_INNER;
        if (scale >= 100 && dist < 55 && !original.is(CRATER_INNER))
            return CRATER;
        if (scale >= 25 && !original.is(CRATER_INNER) && !original.is(CRATER))
            return CRATER_OUTER;
        return null;
    }

    /**
     * The biome part of one EntityFalloutRain chunk step. {@code outer} chunks only change columns within the
     * fallout radius, exactly like the stomp loop they share with.
     *
     * @return whether the chunk was modified (and re-sent, WorldUtil.syncBiomeChange)
     */
    public static boolean apply(ServerLevel level, LevelChunk chunk, double centerX, double centerZ, int scale, boolean outer) {
        if (!HbmParityV3Config.enableCraterBiomes()) return false;
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int quartY = QuartPos.fromBlock(level.getSeaLevel());
        @SuppressWarnings("unchecked")
        Holder<Biome>[] change = new Holder[16];
        boolean biomeModified = false;
        for (int qx = 0; qx < 4; qx++) {
            for (int qz = 0; qz < 4; qz++) {
                int x = minX + qx * 4 + 2;
                int z = minZ + qz * 4 + 2;
                double distance = Math.hypot(x - centerX, z - centerZ);
                if (outer && distance > scale) continue;
                double percent = distance * 100 / scale;
                Holder<Biome> original = chunk.getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z));
                ResourceKey<Biome> biome = getBiomeChange(percent, scale, original);
                if (biome == null || original.is(biome)) continue;
                Optional<Holder.Reference<Biome>> holder = registry.getHolder(biome);
                if (holder.isEmpty()) continue;
                change[qx * 4 + qz] = holder.get();
                biomeModified = true;
            }
        }
        if (!biomeModified) return false;

        int quartMinX = QuartPos.fromBlock(minX);
        int quartMinZ = QuartPos.fromBlock(minZ);
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
            Holder<Biome> replacement = change[((qx - quartMinX) & 3) * 4 + ((qz - quartMinZ) & 3)];
            return replacement != null ? replacement : chunk.getNoiseBiome(qx, qy, qz);
        }, level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.<net.minecraft.world.level.chunk.ChunkAccess>of(chunk));
        return true;
    }
}
