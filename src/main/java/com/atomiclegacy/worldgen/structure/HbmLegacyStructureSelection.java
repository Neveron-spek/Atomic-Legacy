package com.atomiclegacy.worldgen.structure;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.worldgen.LegacyBiomeTraits;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

final class HbmLegacyStructureSelection {
    private static final List<Candidate> CANDIDATES = createCandidates();

    private HbmLegacyStructureSelection() {
    }

    static SelectedStructure select(Holder<Biome> biome, RandomSource random) {
        int totalWeight = 0;
        for (Candidate candidate : CANDIDATES) {
            if (candidate.canSpawn.test(biome)) {
                totalWeight += Math.max(0, candidate.weight.getAsInt());
            }
        }
        if (totalWeight <= 0) {
            return null;
        }

        int selected = random.nextInt(totalWeight);
        for (Candidate candidate : CANDIDATES) {
            if (!candidate.canSpawn.test(biome)) {
                continue;
            }
            selected -= Math.max(0, candidate.weight.getAsInt());
            if (selected < 0) {
                return candidate.toSelected();
            }
        }
        return null;
    }

    static List<String> listStructures() {
        return CANDIDATES.stream()
                .map(Candidate::spawnName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    /** [parity E1: diagnostics] canSpawn of the named spawn condition; null when unknown. */
    static Boolean canSpawn(String name, Holder<Biome> biome) {
        for (Candidate candidate : CANDIDATES) {
            if (name.equals(candidate.spawnName)) {
                return candidate.canSpawn.test(biome);
            }
        }
        return null;
    }

    static boolean isKnownStructureName(String name) {
        return name != null && listStructures().contains(name);
    }

    private static List<Candidate> createCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        add(candidates, "spire", "spire", -1, 1, 128, false, HbmConfig.HBM_STRUCTURE_SPIRE_WEIGHT, HbmLegacyStructureSelection::isSpireBiome);
        add(candidates, "vertibird", "vertibird", -3, 1, 128, false, HbmConfig.HBM_STRUCTURE_VERTIBIRD_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "crashed_vertibird", "crashed-vertibird", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "aircraft_carrier", "aircraft_carrier", -6, 1, 42, false, oceanWeight(HbmConfig.HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT), HbmLegacyStructureSelection::isOcean);
        add(candidates, "oil_rig", "oil_rig", -20, 11, 12, false, oceanWeight(HbmConfig.HBM_STRUCTURE_OIL_RIG_WEIGHT), HbmLegacyStructureSelection::isShallowOcean);
        add(candidates, "lighthouse", "lighthouse", -40, 28, 29, false, oceanWeight(HbmConfig.HBM_STRUCTURE_LIGHTHOUSE_WEIGHT), HbmLegacyStructureSelection::isOceanOrBeach);
        add(candidates, "beached_patrol", "beached_patrol", -5, 58, 67, false, HbmConfig.HBM_STRUCTURE_BEACHED_PATROL_WEIGHT, HbmLegacyStructureSelection::isBeach);
        add(candidates, "dish", "dish", -10, 53, 65, false, HbmConfig.HBM_STRUCTURE_DISH_WEIGHT, HbmLegacyStructureSelection::isPlainsLike);
        add(candidates, "water_pump", "water_pump", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_WATER_PUMP_WEIGHT, HbmLegacyStructureSelection::isWaterPumpBiome);
        add(candidates, "forestchem", "forest_chem", -9, 1, 128, false, HbmConfig.HBM_STRUCTURE_FOREST_CHEM_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "plane1", "crashed_plane_1", -5, 1, 128, false, HbmConfig.HBM_STRUCTURE_PLANE1_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "plane2", "crashed_plane_2", -8, 1, 128, false, HbmConfig.HBM_STRUCTURE_PLANE2_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "desert_shack_1", "desert_shack_1", -7, 1, 128, false, HbmConfig.HBM_STRUCTURE_DESERT_SHACK1_WEIGHT, HbmLegacyStructureSelection::isSandyTagged);
        add(candidates, "desert_shack_2", "desert_shack_2", -7, 1, 128, false, HbmConfig.HBM_STRUCTURE_DESERT_SHACK2_WEIGHT, HbmLegacyStructureSelection::isSandyTagged);
        add(candidates, "desert_shack_3", "desert_shack_3", -5, 1, 128, false, HbmConfig.HBM_STRUCTURE_DESERT_SHACK3_WEIGHT, HbmLegacyStructureSelection::isSandyTagged);
        add(candidates, "dead_dish_small", "dead_dish_small", -5, 1, 128, false, HbmConfig.HBM_STRUCTURE_DEAD_DISH_SMALL_WEIGHT, HbmLegacyStructureSelection::isSandyTagged);
        add(candidates, "labolatory", "laboratory", -10, 53, 65, false, HbmConfig.HBM_STRUCTURE_LABORATORY_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        add(candidates, "forest_post", "forest_post", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_FOREST_POST_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        // [parity R3: structures] factory/crane use heightVariation <= 0.2 && !WATER (not isFlatBiome, which also needs SPARSE).
        add(candidates, "factory", "factory", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_FACTORY_WEIGHT, HbmLegacyStructureSelection::isVeryFlatNonInvalid);
        add(candidates, "crane", "crane", -13, 1, 128, false, HbmConfig.HBM_STRUCTURE_CRANE_WEIGHT, HbmLegacyStructureSelection::isVeryFlatNonInvalid);
        add(candidates, "broadcaster_tower", "broadcasting_tower", -9, 1, 128, false, HbmConfig.HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        add(candidates, "radio", "radio_house", -6, 1, 128, false, HbmConfig.HBM_STRUCTURE_RADIO_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        addProcedural(candidates, "features", HbmLegacyProceduralPiece.Kind.FEATURES, HbmConfig.HBM_STRUCTURE_FEATURES_WEIGHT, HbmLegacyStructureSelection::isProceduralBiome);
        addProcedural(candidates, "bunker", HbmLegacyProceduralPiece.Kind.BUNKER, HbmConfig.HBM_STRUCTURE_BUNKER_WEIGHT, HbmLegacyStructureSelection::isProceduralBiome);
        addJigsaw(candidates, "meteor_dungeon", "meteor/meteor-core", 0, 32, 32, false, HbmConfig.HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT, HbmLegacyStructureSelection::isMeteorDungeonBiome, "start", 128, 128);
        add(candidates, "ruin1", "ntmruins_a", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_A_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin2", "ntmruins_b", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_B_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin3", "ntmruins_c", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_C_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin4", "ntmruins_d", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_D_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin5", "ntmruins_e", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_E_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin6", "ntmruins_f", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_F_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin7", "ntmruins_g", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_G_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin8", "ntmruins_h", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_H_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin9", "ntmruins_i", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_I_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin10", "ntmruins_j", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_J_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "tower_base", "tower_base", -6, 1, 128, false, HbmConfig.HBM_STRUCTURE_TOWER_BASE_WEIGHT, HbmLegacyStructureSelection::isTowerBaseBiome);
        add(candidates, "falloutshelter_house", "falloutshelter_house", -8, 1, 128, false, HbmConfig.HBM_STRUCTURE_FALLOUT_SHELTER_HOUSE_WEIGHT, HbmLegacyStructureSelection::isPlainsTagged);
        add(candidates, "abandoned_arcticstation", "abandoned_arcticstation", 0, 1, 128, false, HbmConfig.HBM_STRUCTURE_ABANDONED_ARCTIC_STATION_WEIGHT, HbmLegacyStructureSelection::isSnowyTagged);
        add(candidates, "military_base", "military_base", -5, 1, 128, false, HbmConfig.HBM_STRUCTURE_MILITARY_BASE_WEIGHT, HbmLegacyStructureSelection::isSnowyTagged);
        // [parity R3: submarine] NTMWorldGenerator: biome == ocean || biome == deepOcean, offset -10.
        add(candidates, "submarine", "submarine", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_SUBMARINE_WEIGHT, HbmLegacyStructureSelection::isPlainOcean);
        addNull(candidates, HbmConfig.HBM_STRUCTURE_PLAINS_NULL_WEIGHT, HbmLegacyStructureSelection::isPlains);
        addNull(candidates, HbmConfig.HBM_STRUCTURE_OCEAN_NULL_WEIGHT, HbmLegacyStructureSelection::isOcean);

        return candidates;
    }

    private static void add(List<Candidate> candidates, String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, IntSupplier weight, Predicate<Holder<Biome>> canSpawn) {
        candidates.add(new Candidate(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight, canSpawn));
    }

    private static void add(List<Candidate> candidates, String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, ModConfigSpec.IntValue weight, Predicate<Holder<Biome>> canSpawn) {
        add(candidates, spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight::get, canSpawn);
    }

    private static void addJigsaw(
            List<Candidate> candidates,
            String spawnName,
            String templateName,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean conformToTerrain,
            ModConfigSpec.IntValue weight,
            Predicate<Holder<Biome>> canSpawn,
            String startPool,
            int sizeLimit,
            int rangeLimit
    ) {
        candidates.add(new Candidate(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight::get, canSpawn, true, startPool, sizeLimit, rangeLimit));
    }

    private static void addProcedural(List<Candidate> candidates, String spawnName, HbmLegacyProceduralPiece.Kind proceduralKind, ModConfigSpec.IntValue weight, Predicate<Holder<Biome>> canSpawn) {
        candidates.add(new Candidate(spawnName, "", 0, 1, 128, false, weight::get, canSpawn, false, "", 0, 0, proceduralKind));
    }

    private static void addNull(List<Candidate> candidates, IntSupplier weight, Predicate<Holder<Biome>> canSpawn) {
        candidates.add(new Candidate(null, null, 0, 1, 128, false, weight, canSpawn));
    }

    private static void addNull(List<Candidate> candidates, ModConfigSpec.IntValue weight, Predicate<Holder<Biome>> canSpawn) {
        addNull(candidates, weight::get, canSpawn);
    }

    private static IntSupplier oceanWeight(IntSupplier delegate) {
        return () -> HbmConfig.HBM_STRUCTURE_ENABLE_OCEAN.get() ? delegate.getAsInt() : 0;
    }

    private static IntSupplier oceanWeight(ModConfigSpec.IntValue delegate) {
        return oceanWeight(delegate::get);
    }

    private static IntSupplier ruinWeight(IntSupplier delegate) {
        return () -> HbmConfig.HBM_STRUCTURE_ENABLE_RUINS.get() ? delegate.getAsInt() : 0;
    }

    private static IntSupplier ruinWeight(ModConfigSpec.IntValue delegate) {
        return ruinWeight(delegate::get);
    }

    // [parity E1] NTMWorldGenerator canSpawn predicates on the 1.7.10 traits table (LegacyBiomeTraits).
    private static boolean notWater(Holder<Biome> biome) {
        return !biome.is(Biomes.THE_VOID) && !LegacyBiomeTraits.isWater(biome);
    }

    private static boolean has(Holder<Biome> biome, int type) {
        return LegacyBiomeTraits.of(biome).is(type);
    }

    /** spire: heightVariation <= 0.05F && !isWaterBiome */
    private static boolean isSpireBiome(Holder<Biome> biome) {
        return notWater(biome) && LegacyBiomeTraits.maxVariation(biome, 0.05F);
    }

    /** factory, crane: heightVariation <= 0.2F && !isWaterBiome */
    private static boolean isVeryFlatNonInvalid(Holder<Biome> biome) {
        return notWater(biome) && LegacyBiomeTraits.maxVariation(biome, 0.2F);
    }

    /** forestchem, forest_post, plane1/2: heightVariation <= 0.3F && !isWaterBiome */
    private static boolean isGentleNonInvalid(Holder<Biome> biome) {
        return notWater(biome) && LegacyBiomeTraits.maxVariation(biome, 0.3F);
    }

    private static boolean isFlatBiome(Holder<Biome> biome) {
        return notWater(biome) && LegacyBiomeTraits.isFlat(biome);
    }

    /** vertibird, crashed_vertibird: !isWaterBiome && SANDY */
    private static boolean isSandy(Holder<Biome> biome) {
        return notWater(biome) && has(biome, LegacyBiomeTraits.SANDY);
    }

    /** desert shacks, dead_dish_small: SANDY */
    private static boolean isSandyTagged(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.SANDY);
    }

    private static boolean isWaterPumpBiome(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.PLAINS) || has(biome, LegacyBiomeTraits.SWAMP);
    }

    /** tower_base: heightVariation <= 0.3F && !isWaterBiome && !SANDY */
    private static boolean isTowerBaseBiome(Holder<Biome> biome) {
        return isGentleNonInvalid(biome) && !has(biome, LegacyBiomeTraits.SANDY);
    }

    private static boolean isPlainsTagged(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.PLAINS);
    }

    private static boolean isSnowyTagged(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.SNOWY);
    }

    private static boolean isOcean(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.OCEAN);
    }

    /** oil_rig: OCEAN && rootHeight >= -1.5F */
    private static boolean isShallowOcean(Holder<Biome> biome) {
        return isOcean(biome) && LegacyBiomeTraits.of(biome).rootHeight() >= -1.5F;
    }

    private static boolean isPlainOcean(Holder<Biome> biome) {
        return biome.is(Biomes.OCEAN) || biome.is(Biomes.DEEP_OCEAN);
    }

    private static boolean isBeach(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.BEACH);
    }

    private static boolean isOceanOrBeach(Holder<Biome> biome) {
        return isOcean(biome) || isBeach(biome);
    }

    private static boolean isPlains(Holder<Biome> biome) {
        return biome.is(Biomes.PLAINS);
    }

    private static boolean isPlainsLike(Holder<Biome> biome) {
        return has(biome, LegacyBiomeTraits.PLAINS);
    }

    /** ruins: !isWaterBiome && canSpawnLightningBolt (enableSnow ? false : enableRain) */
    private static boolean canSpawnRainRuin(Holder<Biome> biome) {
        return notWater(biome) && biome.value().hasPrecipitation() && biome.value().getBaseTemperature() >= 0.15F;
    }

    /** meteor_dungeon: rootHeight >= 0 */
    private static boolean isMeteorDungeonBiome(Holder<Biome> biome) {
        return !biome.is(Biomes.THE_VOID) && LegacyBiomeTraits.of(biome).rootHeight() >= 0.0F;
    }

    private static boolean isProceduralBiome(Holder<Biome> biome) {
        return notWater(biome);
    }

    private record Candidate(
            String spawnName,
            String templateName,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean conformToTerrain,
            IntSupplier weight,
            Predicate<Holder<Biome>> canSpawn,
            boolean jigsaw,
            String startPool,
            int sizeLimit,
            int rangeLimit,
            HbmLegacyProceduralPiece.Kind proceduralKind
    ) {
        private Candidate(String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, IntSupplier weight, Predicate<Holder<Biome>> canSpawn) {
            this(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight, canSpawn, false, "", 0, 0, null);
        }

        private Candidate(String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, IntSupplier weight, Predicate<Holder<Biome>> canSpawn, boolean jigsaw, String startPool, int sizeLimit, int rangeLimit) {
            this(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight, canSpawn, jigsaw, startPool, sizeLimit, rangeLimit, null);
        }

        SelectedStructure toSelected() {
            if (templateName == null) {
                return null;
            }
            return new SelectedStructure(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, jigsaw, startPool, sizeLimit, rangeLimit, proceduralKind);
        }
    }

    record SelectedStructure(
            String spawnName,
            String templateName,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean conformToTerrain,
            boolean jigsaw,
            String startPool,
            int sizeLimit,
            int rangeLimit,
            HbmLegacyProceduralPiece.Kind proceduralKind
    ) {
        boolean procedural() {
            return proceduralKind != null;
        }
    }
}
