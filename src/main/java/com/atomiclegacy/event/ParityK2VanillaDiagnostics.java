package com.atomiclegacy.event;

import com.atomiclegacy.worldgen.LegacyBiomeTraits;
import com.atomiclegacy.worldgen.structure.HbmLegacyStructureSelectionProbe;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;

/**
 * [parity K2] LegacyBiomeTraits re-checked against decompiled 1.7.10 sources (MCP 9.08 BiomeGenBase and
 * subclasses, Forge 1.7.10 BiomeDictionary). Mutated biomes (id + 128) are not in registerVanillaBiomes,
 * so their types are BiomeDictionary.makeBestGuess results.
 */
public final class ParityK2VanillaDiagnostics {
    private static final int OCEAN = LegacyBiomeTraits.OCEAN, RIVER = LegacyBiomeTraits.RIVER, BEACH = LegacyBiomeTraits.BEACH,
            SANDY = LegacyBiomeTraits.SANDY, PLAINS = LegacyBiomeTraits.PLAINS, SPARSE = LegacyBiomeTraits.SPARSE,
            SNOWY = LegacyBiomeTraits.SNOWY, SWAMP = LegacyBiomeTraits.SWAMP, FOREST = LegacyBiomeTraits.FOREST;

    private ParityK2VanillaDiagnostics() {
    }

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            // root, variation, types (restricted to the bits LegacyBiomeTraits tracks)
            expect(server, problems, cases, Biomes.OCEAN, -1.0F, 0.1F, OCEAN);
            expect(server, problems, cases, Biomes.DEEP_OCEAN, -1.8F, 0.1F, OCEAN);
            expect(server, problems, cases, Biomes.FROZEN_OCEAN, -1.0F, 0.1F, OCEAN | SNOWY);
            expect(server, problems, cases, Biomes.RIVER, -0.5F, 0.0F, RIVER);
            expect(server, problems, cases, Biomes.FROZEN_RIVER, -0.5F, 0.0F, RIVER | SNOWY);
            expect(server, problems, cases, Biomes.PLAINS, 0.125F, 0.05F, PLAINS);
            expect(server, problems, cases, Biomes.SUNFLOWER_PLAINS, 0.125F, 0.05F, PLAINS);
            expect(server, problems, cases, Biomes.DESERT, 0.125F, 0.05F, SANDY);
            expect(server, problems, cases, Biomes.WINDSWEPT_HILLS, 1.0F, 0.5F, 0);
            expect(server, problems, cases, Biomes.WINDSWEPT_GRAVELLY_HILLS, 1.0F, 0.5F, 0);
            expect(server, problems, cases, Biomes.WINDSWEPT_FOREST, 1.0F, 0.5F, FOREST | SPARSE);
            expect(server, problems, cases, Biomes.FOREST, 0.1F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.FLOWER_FOREST, 0.1F, 0.4F, FOREST);
            expect(server, problems, cases, Biomes.BIRCH_FOREST, 0.1F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.OLD_GROWTH_BIRCH_FOREST, 0.2F, 0.4F, 0);
            expect(server, problems, cases, Biomes.DARK_FOREST, 0.1F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.TAIGA, 0.2F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.SNOWY_TAIGA, 0.2F, 0.2F, FOREST | SNOWY);
            expect(server, problems, cases, Biomes.OLD_GROWTH_PINE_TAIGA, 0.2F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.OLD_GROWTH_SPRUCE_TAIGA, 0.2F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.SWAMP, -0.2F, 0.1F, SWAMP);
            expect(server, problems, cases, Biomes.SNOWY_PLAINS, 0.125F, 0.05F, SNOWY);
            expect(server, problems, cases, Biomes.ICE_SPIKES, 0.425F, 0.45F, SNOWY);
            expect(server, problems, cases, Biomes.MUSHROOM_FIELDS, 0.2F, 0.3F, 0);
            expect(server, problems, cases, Biomes.BEACH, 0.0F, 0.025F, BEACH);
            expect(server, problems, cases, Biomes.STONY_SHORE, 0.1F, 0.8F, BEACH);
            expect(server, problems, cases, Biomes.SNOWY_BEACH, 0.0F, 0.025F, BEACH | SNOWY);
            expect(server, problems, cases, Biomes.JUNGLE, 0.1F, 0.2F, 0);
            expect(server, problems, cases, Biomes.SPARSE_JUNGLE, 0.1F, 0.2F, FOREST);
            expect(server, problems, cases, Biomes.SAVANNA, 0.125F, 0.05F, PLAINS | SPARSE);
            expect(server, problems, cases, Biomes.SAVANNA_PLATEAU, 1.5F, 0.025F, PLAINS | SPARSE);
            expect(server, problems, cases, Biomes.WINDSWEPT_SAVANNA, 0.3625F, 1.225F, SPARSE);
            expect(server, problems, cases, Biomes.BADLANDS, 0.1F, 0.2F, SANDY);
            expect(server, problems, cases, Biomes.ERODED_BADLANDS, 0.1F, 0.2F, SANDY | PLAINS);
            expect(server, problems, cases, Biomes.WOODED_BADLANDS, 1.5F, 0.025F, SANDY | SPARSE);

            // biome instanceof BiomeGenForest: Birch Forest M is an anonymous BiomeGenMutated
            cases[0]++;
            if (LegacyBiomeTraits.isForestClass(holder(server, Biomes.OLD_GROWTH_BIRCH_FOREST))
                    || !LegacyBiomeTraits.isForestClass(holder(server, Biomes.FLOWER_FOREST))) {
                problems.add("parity K2: BiomeGenForest class mapping of old_growth_birch_forest / flower_forest is wrong");
            }

            // eroded_badlands now carries PLAINS (dish / water_pump), still SANDY
            spawn(server, problems, cases, "dish", true, Biomes.ERODED_BADLANDS);
            spawn(server, problems, cases, "water_pump", true, Biomes.ERODED_BADLANDS);
            spawn(server, problems, cases, "desert_shack_1", true, Biomes.ERODED_BADLANDS);
            spawn(server, problems, cases, "dish", false, Biomes.WINDSWEPT_SAVANNA, Biomes.OLD_GROWTH_BIRCH_FOREST);
        } catch (Throwable t) {
            problems.add("parity K2 vanilla: exception " + t);
        }
        return cases[0];
    }

    private static Holder<Biome> holder(MinecraftServer server, ResourceKey<Biome> key) {
        return server.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
    }

    private static void expect(MinecraftServer server, List<String> problems, int[] cases, ResourceKey<Biome> key,
                               float root, float variation, int types) {
        cases[0]++;
        LegacyBiomeTraits.Traits t = LegacyBiomeTraits.of(holder(server, key));
        if (Float.compare(t.rootHeight(), root) != 0 || Float.compare(t.heightVariation(), variation) != 0 || t.types() != types) {
            problems.add("parity K2: biome traits " + key.location() + " = " + t + ", 1.7.10 ancestor gives root=" + root
                    + " variation=" + variation + " types=" + types);
        }
    }

    @SafeVarargs
    private static void spawn(MinecraftServer server, List<String> problems, int[] cases, String structure, boolean expected,
                              ResourceKey<Biome>... biomes) {
        for (ResourceKey<Biome> key : biomes) {
            cases[0]++;
            Boolean actual = HbmLegacyStructureSelectionProbe.canSpawn(structure, holder(server, key));
            if (actual == null || actual != expected) {
                problems.add("parity K2: structure " + structure + " in " + key.location() + " canSpawn=" + actual
                        + " expected " + expected);
            }
        }
    }
}
