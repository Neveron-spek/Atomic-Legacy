package com.atomiclegacy.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

import java.util.HashMap;
import java.util.Map;

/**
 * [parity E1] 1.7.10 biome traits used by NTMWorldGenerator / HbmWorldGen spawn conditions:
 * BiomeGenBase.rootHeight / heightVariation and the Forge BiomeDictionary types of the
 * 1.7.10 biome each modern vanilla biome descends from. Modern biomes expose neither value,
 * so the table carries the 1.7.10 numbers verbatim; biomes without a 1.7.10 ancestor
 * (and modded biomes) fall back to tags.
 */
public final class LegacyBiomeTraits {
    public static final int OCEAN = 1, RIVER = 2, BEACH = 4, SANDY = 8, PLAINS = 16, SPARSE = 32, SNOWY = 64, SWAMP = 128, FOREST = 256;

    public record Traits(float rootHeight, float heightVariation, int types) {
        public boolean is(int type) {
            return (types & type) != 0;
        }
    }

    private static final Map<ResourceKey<Biome>, Traits> TABLE = new HashMap<>();

    private static void put(ResourceKey<Biome> key, float root, float variation, int types) {
        TABLE.put(key, new Traits(root, variation, types));
    }

    static {
        // 1.7.10 ancestors (BiomeGenBase heights, BiomeDictionary.registerVanillaBiomes; mutated ids +128
        // are not registered there and get BiomeDictionary.makeBestGuess). Verified against MCP 9.08 / Forge 1.7.10 [parity K2].
        put(Biomes.OCEAN, -1.0F, 0.1F, OCEAN);
        put(Biomes.DEEP_OCEAN, -1.8F, 0.1F, OCEAN);
        put(Biomes.FROZEN_OCEAN, -1.0F, 0.1F, OCEAN | SNOWY);
        put(Biomes.RIVER, -0.5F, 0.0F, RIVER);
        put(Biomes.FROZEN_RIVER, -0.5F, 0.0F, RIVER | SNOWY);
        put(Biomes.PLAINS, 0.125F, 0.05F, PLAINS);
        put(Biomes.SUNFLOWER_PLAINS, 0.125F, 0.05F, PLAINS);
        put(Biomes.DESERT, 0.125F, 0.05F, SANDY);
        put(Biomes.WINDSWEPT_HILLS, 1.0F, 0.5F, 0);
        put(Biomes.WINDSWEPT_GRAVELLY_HILLS, 1.0F, 0.5F, 0);
        put(Biomes.WINDSWEPT_FOREST, 1.0F, 0.5F, FOREST | SPARSE);
        put(Biomes.FOREST, 0.1F, 0.2F, FOREST);
        put(Biomes.FLOWER_FOREST, 0.1F, 0.4F, FOREST);
        put(Biomes.BIRCH_FOREST, 0.1F, 0.2F, FOREST);
        put(Biomes.OLD_GROWTH_BIRCH_FOREST, 0.2F, 0.4F, 0); // Birch Forest M: BiomeGenMutated, treesPerChunk 0 -> makeBestGuess gives only HILLS
        put(Biomes.DARK_FOREST, 0.1F, 0.2F, FOREST);
        put(Biomes.TAIGA, 0.2F, 0.2F, FOREST);
        put(Biomes.SNOWY_TAIGA, 0.2F, 0.2F, FOREST | SNOWY);
        put(Biomes.OLD_GROWTH_PINE_TAIGA, 0.2F, 0.2F, FOREST);
        put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 0.2F, 0.2F, FOREST);
        put(Biomes.SWAMP, -0.2F, 0.1F, SWAMP);
        put(Biomes.SNOWY_PLAINS, 0.125F, 0.05F, SNOWY);
        put(Biomes.ICE_SPIKES, 0.425F, 0.45F, SNOWY);
        put(Biomes.MUSHROOM_FIELDS, 0.2F, 0.3F, 0);
        put(Biomes.BEACH, 0.0F, 0.025F, BEACH);
        put(Biomes.STONY_SHORE, 0.1F, 0.8F, BEACH);
        put(Biomes.SNOWY_BEACH, 0.0F, 0.025F, BEACH | SNOWY);
        put(Biomes.JUNGLE, 0.1F, 0.2F, 0);
        put(Biomes.SPARSE_JUNGLE, 0.1F, 0.2F, FOREST);
        put(Biomes.SAVANNA, 0.125F, 0.05F, PLAINS | SPARSE);
        put(Biomes.SAVANNA_PLATEAU, 1.5F, 0.025F, PLAINS | SPARSE);
        put(Biomes.WINDSWEPT_SAVANNA, 0.3625F, 1.225F, SPARSE); // Savanna M: treesPerChunk 2 -> SPARSE (guess)
        put(Biomes.BADLANDS, 0.1F, 0.2F, SANDY);
        put(Biomes.ERODED_BADLANDS, 0.1F, 0.2F, SANDY | PLAINS); // Mesa (Bryce): unregistered, guess: variation 0.2 -> PLAINS, sand top -> SANDY
        put(Biomes.WOODED_BADLANDS, 1.5F, 0.025F, SANDY | SPARSE);
    }

    private LegacyBiomeTraits() {
    }

    public static Traits of(Holder<Biome> biome) {
        Traits known = biome.unwrapKey().map(TABLE::get).orElse(null);
        if (known != null) {
            return known;
        }
        // No 1.7.10 ancestor: derive from tags.
        int types = 0;
        if (biome.is(BiomeTags.IS_OCEAN)) types |= OCEAN;
        if (biome.is(BiomeTags.IS_RIVER)) types |= RIVER;
        if (biome.is(BiomeTags.IS_BEACH)) types |= BEACH;
        if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(Tags.Biomes.IS_DESERT)) types |= SANDY;
        if (biome.is(Tags.Biomes.IS_PLAINS) || biome.is(BiomeTags.IS_SAVANNA)) types |= PLAINS;
        if (biome.is(Tags.Biomes.IS_SNOWY)) types |= SNOWY;
        if (biome.is(Tags.Biomes.IS_SWAMP)) types |= SWAMP;
        if (biome.is(BiomeTags.IS_FOREST)) types |= FOREST;
        boolean steep = biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL);
        float root = biome.is(BiomeTags.IS_DEEP_OCEAN) ? -1.8F : (types & OCEAN) != 0 ? -1.0F : (types & RIVER) != 0 ? -0.5F : steep ? 1.0F : 0.1F;
        return new Traits(root, steep ? 0.5F : 0.2F, types);
    }

    /** BiomeDictionary Type.WATER: ocean or river. */
    public static boolean isWater(Holder<Biome> biome) {
        Traits t = of(biome);
        return t.is(OCEAN) || t.is(RIVER);
    }

    /** NTMWorldGenerator.isFlatBiome. */
    public static boolean isFlat(Holder<Biome> biome) {
        Traits t = of(biome);
        return t.heightVariation() <= 0.2F && !isWater(biome) && t.is(SPARSE);
    }

    public static boolean maxVariation(Holder<Biome> biome, float max) {
        return of(biome).heightVariation() <= max && !isWater(biome);
    }

    /** 1.7.10 {@code biome instanceof BiomeGenForest}: forest, flower forest, birch and roofed forest (Birch Forest M is a BiomeGenMutated, not included). */
    public static boolean isForestClass(Holder<Biome> biome) {
        return biome.is(Biomes.FOREST) || biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.BIRCH_FOREST)
                || biome.is(Biomes.DARK_FOREST);
    }
}
