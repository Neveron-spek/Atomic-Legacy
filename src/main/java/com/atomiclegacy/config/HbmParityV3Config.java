package com.atomiclegacy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * [parity V3: configs] HBM 1.7.10 Forge config keys (BombConfig, RadiationConfig, ToolConfig, MobConfig, RBMKDials)
 * that the port had hard-coded. Keys keep the original category, name, default and comment; dotted names are single
 * TOML keys (List path). Built inside {@link HbmConfig}'s spec. Keys marked "no effect" are read by no code in the
 * 1.7.10 sources either; they exist so the config file matches the original.
 */
public final class HbmParityV3Config {
    // 03_nukes (BombConfig); 3.09_fatmanRadius and 3.11_aSchrabRadius are HbmConfig.FATMAN_RADIUS / DROPPED_ANTI_SCHRABIDIUM_RADIUS.
    public static ModConfigSpec.IntValue GADGET_RADIUS;
    public static ModConfigSpec.IntValue BOY_RADIUS;
    public static ModConfigSpec.IntValue MAN_RADIUS;
    public static ModConfigSpec.IntValue MIKE_RADIUS;
    public static ModConfigSpec.IntValue TSAR_RADIUS;
    public static ModConfigSpec.IntValue PROTOTYPE_RADIUS;
    public static ModConfigSpec.IntValue FLEIJA_RADIUS;
    public static ModConfigSpec.IntValue MISSILE_RADIUS;
    // [parity b2] 17_biomes (WorldConfig); the numeric biome ids 17.B00..B02 have no 1.21 equivalent
    public static ModConfigSpec.BooleanValue ENABLE_CRATER_BIOMES;
    public static ModConfigSpec.DoubleValue CRATER_BIOME_RAD;
    public static ModConfigSpec.DoubleValue CRATER_BIOME_INNER_RAD;
    public static ModConfigSpec.DoubleValue CRATER_BIOME_OUTER_RAD;
    public static ModConfigSpec.DoubleValue CRATER_BIOME_WATER_MULT;
    /** [parity b2] GeneralConfig 0.05_enableServerRecipeSync. */
    public static ModConfigSpec.BooleanValue ENABLE_SERVER_RECIPE_SYNC;
    /** No effect (BombConfig.mirvRadius is never read). */
    public static ModConfigSpec.IntValue MIRV_RADIUS;
    /** No effect (BombConfig.nukaRadius is never read). */
    public static ModConfigSpec.IntValue NUKA_RADIUS;
    public static ModConfigSpec.IntValue SOLINIUM_RADIUS;
    public static ModConfigSpec.IntValue N2_RADIUS;
    // 06_explosions
    public static ModConfigSpec.IntValue BLAST_SPEED;
    public static ModConfigSpec.IntValue MK5_BLAST_TIME;
    public static ModConfigSpec.IntValue FALLOUT_RANGE;
    public static ModConfigSpec.IntValue FALLOUT_DELAY;
    /** No effect: only ExplosionNukeRayParallelized reads it and EntityNukeExplosionMK5 never creates one. */
    public static ModConfigSpec.BooleanValue ENABLE_CHUNK_LOADING;
    /** No effect: the algorithm switch in EntityNukeExplosionMK5 is commented out (always ExplosionNukeRayBatched). */
    public static ModConfigSpec.IntValue EXPLOSION_ALGORITHM;
    // 11_tools
    public static ModConfigSpec.IntValue RECURSION_DEPTH;
    public static ModConfigSpec.BooleanValue RECURSION_STONE;
    public static ModConfigSpec.BooleanValue RECURSION_NETHERRACK;
    public static ModConfigSpec.BooleanValue ABILITY_HAMMER;
    public static ModConfigSpec.BooleanValue ABILITY_VEIN;
    public static ModConfigSpec.BooleanValue ABILITY_LUCK;
    public static ModConfigSpec.BooleanValue ABILITY_SILK;
    public static ModConfigSpec.BooleanValue ABILITY_FURNACE;
    public static ModConfigSpec.BooleanValue ABILITY_SHREDDER;
    public static ModConfigSpec.BooleanValue ABILITY_CENTRIFUGE;
    public static ModConfigSpec.BooleanValue ABILITY_CRYSTALLIZER;
    public static ModConfigSpec.BooleanValue ABILITY_MERCURY;
    public static ModConfigSpec.BooleanValue ABILITY_EXPLOSION;
    // 12_mobs (no effect: MobConfig.largeHiveThreshold, enableInfestation, baseInfestChance are never read)
    public static ModConfigSpec.IntValue LARGE_HIVE_THRESHOLD;
    public static ModConfigSpec.BooleanValue ENABLE_INFESTATION;
    public static ModConfigSpec.DoubleValue BASE_INFEST_CHANCE;
    // 13_radiation
    public static ModConfigSpec.IntValue FOG_RAD;
    public static ModConfigSpec.IntValue FOG_CH;
    public static ModConfigSpec.DoubleValue HELL_RAD;
    public static ModConfigSpec.BooleanValue WORLD_RAD_EFFECTS;
    /** No effect (handleWorldDestruction hard-codes 10 operations). */
    public static ModConfigSpec.IntValue WORLD_RAD;
    /** No effect (handleWorldDestruction hard-codes threshold 10). */
    public static ModConfigSpec.IntValue WORLD_RAD_THRESHOLD;
    public static ModConfigSpec.BooleanValue CLEANUP_DEAD_DIRT;
    public static ModConfigSpec.BooleanValue ENABLE_CONTAMINATION;
    public static ModConfigSpec.BooleanValue ENABLE_CHUNK_RADS;
    // 14_hazard (HAZ_00/01 are HbmConfig.ENABLE_ASBESTOS / ENABLE_COAL_DUST)
    public static ModConfigSpec.BooleanValue DISABLE_HOT;
    public static ModConfigSpec.BooleanValue DISABLE_EXPLOSIVE;
    public static ModConfigSpec.BooleanValue DISABLE_HYDRO;
    public static ModConfigSpec.BooleanValue DISABLE_BLINDING;
    /** No effect (RadiationConfig.disableFibrosis is never read). */
    public static ModConfigSpec.BooleanValue DISABLE_FIBROSIS;
    // 16_pollution
    /** No effect (RadiationConfig.smokeStackSootMult is never read). */
    public static ModConfigSpec.DoubleValue SMOKE_STACK_SOOT_MULT;
    // rbmk (RBMKDials game rule)
    /** No effect (RBMKDials.getReaSimRange has no caller). */
    public static ModConfigSpec.IntValue DIAL_REASIM_RANGE;

    private HbmParityV3Config() {
    }

    /**
     * NightConfig's String overloads (getComment/setComment, used by ModConfigSpec.correct) split keys on '.', so a
     * 1.7.10 key such as "3.00_gadgetRadius" lost its comment and produced a stray "03_nukes.3" table that was
     * "corrected" on every launch. The dot of the legacy key is written as '_' in the TOML key.
     */
    private static String path(String key) {
        return key.replace('.', '_');
    }

    private static ModConfigSpec.IntValue i(ModConfigSpec.Builder b, String key, String comment, int def) {
        return b.comment(comment).defineInRange(List.of(path(key)), def, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static ModConfigSpec.DoubleValue d(ModConfigSpec.Builder b, String key, String comment, double def) {
        return b.comment(comment).defineInRange(List.of(path(key)), def, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private static ModConfigSpec.BooleanValue z(ModConfigSpec.Builder b, String key, String comment, boolean def) {
        return b.comment(comment).define(List.of(path(key)), def);
    }

    static void define(ModConfigSpec.Builder b) {
        b.push("01_general");
        ENABLE_SERVER_RECIPE_SYNC = z(b, "0.05_enableServerRecipeSync", "Syncs any recipes customised via JSON to clients connecting to the server.", false);
        b.pop();
        b.push("03_nukes");
        GADGET_RADIUS = i(b, "3.00_gadgetRadius", "Radius of the Gadget", 150);
        BOY_RADIUS = i(b, "3.01_boyRadius", "Radius of Little Boy", 120);
        MAN_RADIUS = i(b, "3.02_manRadius", "Radius of Fat Man", 175);
        MIKE_RADIUS = i(b, "3.03_mikeRadius", "Radius of Ivy Mike", 250);
        TSAR_RADIUS = i(b, "3.04_tsarRadius", "Radius of the Tsar Bomba", 500);
        PROTOTYPE_RADIUS = i(b, "3.05_prototypeRadius", "Radius of the Prototype", 150);
        FLEIJA_RADIUS = i(b, "3.06_fleijaRadius", "Radius of F.L.E.I.J.A.", 50);
        MISSILE_RADIUS = i(b, "3.07_missileRadius", "Radius of the nuclear missile", 100);
        MIRV_RADIUS = i(b, "3.08_mirvRadius", "Radius of a MIRV", 100);
        NUKA_RADIUS = i(b, "3.10_nukaRadius", "Radius of the nuka grenade", 25);
        SOLINIUM_RADIUS = i(b, "3.12_soliniumRadius", "Radius of the blue rinse", 150);
        N2_RADIUS = i(b, "3.13_n2Radius", "Radius of the N2 mine", 200);
        b.pop();
        b.push("06_explosions");
        BLAST_SPEED = i(b, "6.01_blastSpeed", "Base speed of MK3 system (old and schrabidium) detonations (Blocks / tick)", 1024);
        MK5_BLAST_TIME = i(b, "6.02_mk5BlastTime", "Minimum amount of milliseconds per tick allocated for mk5 chunk processing", 50);
        FALLOUT_RANGE = i(b, "6.03_falloutRange", "Radius of fallout area (base radius * value in percent)", 100);
        FALLOUT_DELAY = i(b, "6.04_falloutDelay", "How many ticks to wait for the next fallout chunk computation", 4);
        ENABLE_CHUNK_LOADING = z(b, "6.05_enableChunkLoading", "Allows all types of procedural explosions to keep the central chunk loaded and to generate new chunks.", true);
        EXPLOSION_ALGORITHM = i(b, "6.06_explosionAlgorithm", "Configures the algorithm of mk5 explosion. \n0 = Legacy, 1 = Threaded DDA, 2 = Threaded DDA with damage accumulation.", 2);
        b.pop();
        b.push("11_tools");
        RECURSION_DEPTH = i(b, "11.00_recursionDepth", "Limits veinminer's recursive function. Usually not an issue, unless you're using bukkit which is especially sensitive for some reason.", 1000);
        RECURSION_STONE = z(b, "11.01_recursionStone", "Determines whether veinminer can break stone", false);
        RECURSION_NETHERRACK = z(b, "11.02_recursionNetherrack", "Determines whether veinminer can break netherrack", false);
        ABILITY_HAMMER = z(b, "11.03_hammerAbility", "Allows AoE ability", true);
        ABILITY_VEIN = z(b, "11.04_abilityVein", "Allows veinminer ability", true);
        ABILITY_LUCK = z(b, "11.05_abilityLuck", "Allow luck (fortune) ability", true);
        ABILITY_SILK = z(b, "11.06_abilitySilk", "Allow silk touch ability", true);
        ABILITY_FURNACE = z(b, "11.07_abilityFurnace", "Allow auto-smelter ability", true);
        ABILITY_SHREDDER = z(b, "11.08_abilityShredder", "Allow auto-shredder ability", true);
        ABILITY_CENTRIFUGE = z(b, "11.09_abilityCentrifuge", "Allow auto-centrifuge ability", true);
        ABILITY_CRYSTALLIZER = z(b, "11.10_abilityCrystallizer", "Allow auto-crystallizer ability", true);
        ABILITY_MERCURY = z(b, "11.11_abilityMercury", "Allow mercury touch ability (digging redstone gives mercury)", true);
        ABILITY_EXPLOSION = z(b, "11.12_abilityExplosion", "Allow explosion ability", true);
        b.pop();
        b.push("12_mobs");
        LARGE_HIVE_THRESHOLD = i(b, "12.G12_largeHiveThreshold", "The soot threshold for a large hive to spawn", 20);
        ENABLE_INFESTATION = z(b, "12.I01_enableInfestation", "Whether structures infested with glyphids should spawn", true);
        BASE_INFEST_CHANCE = d(b, "12.I02_baseInfestChance", "The chance for infested structures to spawn", 5D);
        b.pop();
        b.push("13_radiation");
        FOG_RAD = i(b, "FOG_00_threshold", "Radiation in RADs required for fog to spawn", 100);
        FOG_CH = i(b, "FOG_01_threshold", "1:n chance of fog spawning every second", 20);
        HELL_RAD = d(b, "AMBIENT_00_nether", "RAD/s in the nether", 0.1D);
        WORLD_RAD_EFFECTS = z(b, "RADWORLD_00_toggle", "Whether high radiation levels should perform changes in the world", true);
        WORLD_RAD = i(b, "RADWORLD_01_amount", "How many block operations radiation can perform per tick", 10);
        WORLD_RAD_THRESHOLD = i(b, "RADWORLD_02_minimum", "The least amount of RADs required for block modification to happen", 20);
        CLEANUP_DEAD_DIRT = z(b, "RADWORLD_03_regrow", "Whether dead grass and mycelium should decay into dirt", false);
        ENABLE_CONTAMINATION = z(b, "RADIATION_00_enableContamination", "Toggles player contamination (and negative effects from radiation poisoning)", true);
        ENABLE_CHUNK_RADS = z(b, "RADIATION_01_enableChunkRads", "Toggles the world radiation system (chunk radiation only, some blocks use an AoE!)", true);
        b.pop();
        b.push("17_biomes");
        ENABLE_CRATER_BIOMES = z(b, "17.B_toggle", "Enables the biome change caused by nuclear explosions", true);
        CRATER_BIOME_RAD = d(b, "17.R00_craterBiomeRad", "RAD/s for the crater biome", 5D);
        CRATER_BIOME_INNER_RAD = d(b, "17.R01_craterBiomeInnerRad", "RAD/s for the inner crater biome", 25D);
        CRATER_BIOME_OUTER_RAD = d(b, "17.R02_craterBiomeOuterRad", "RAD/s for the outer crater biome", 0.5D);
        CRATER_BIOME_WATER_MULT = d(b, "17.R03_craterBiomeWaterMult", "Multiplier for RAD/s in crater biomes when in water", 5D);
        b.pop();
        b.push("14_hazard");
        DISABLE_HOT = z(b, "HAZ_02_disableHot", "When turned off, all hot hazards are disabled", false);
        DISABLE_EXPLOSIVE = z(b, "HAZ_03_disableExplosive", "When turned off, all explosive hazards are disabled", false);
        DISABLE_HYDRO = z(b, "HAZ_04_disableHydroactive", "When turned off, all hydroactive hazards are disabled", false);
        DISABLE_BLINDING = z(b, "HAZ_05_disableBlinding", "When turned off, all blinding hazards are disabled", false);
        DISABLE_FIBROSIS = z(b, "HAZ_06_disableFibrosis", "When turned off, all fibrosis hazards are disabled", false);
        b.pop();
        b.push("16_pollution");
        SMOKE_STACK_SOOT_MULT = d(b, "POL_08_smokeStackSootMult", "How much does smokestack multiply soot by, with decimal values reducing the soot", 0.8D);
        b.pop();
        b.push("rbmkDials");
        DIAL_REASIM_RANGE = i(b, "dialReasimRange", "Superseded by /gamerule dialReasimRange (RBMKDials are game rules, as in 1.7.10); this key is not read. RBMKDials KEY_REASIM_RANGE: how far the flux of a ReaSim fuel rod reaches [1;100] (no caller in 1.7.10). Default: 10.", 10);
        b.pop();
    }

    private static boolean loaded() {
        return HbmConfig.SPEC.isLoaded();
    }

    private static int get(ModConfigSpec.IntValue v, int def) {
        return loaded() ? v.get() : def;
    }

    private static boolean get(ModConfigSpec.BooleanValue v, boolean def) {
        return loaded() ? v.get() : def;
    }

    public static boolean enableCraterBiomes() { return get(ENABLE_CRATER_BIOMES, true); }
    public static float craterBiomeRad() { return loaded() ? CRATER_BIOME_RAD.get().floatValue() : 5F; }
    public static float craterBiomeInnerRad() { return loaded() ? CRATER_BIOME_INNER_RAD.get().floatValue() : 25F; }
    public static float craterBiomeOuterRad() { return loaded() ? CRATER_BIOME_OUTER_RAD.get().floatValue() : 0.5F; }
    public static float craterBiomeWaterMult() { return loaded() ? CRATER_BIOME_WATER_MULT.get().floatValue() : 5F; }
    public static boolean enableServerRecipeSync() { return get(ENABLE_SERVER_RECIPE_SYNC, false); }

    // BombConfig
    public static int gadgetRadius() { return get(GADGET_RADIUS, 150); }
    public static int boyRadius() { return get(BOY_RADIUS, 120); }
    public static int manRadius() { return get(MAN_RADIUS, 175); }
    public static int mikeRadius() { return get(MIKE_RADIUS, 250); }
    public static int tsarRadius() { return get(TSAR_RADIUS, 500); }
    public static int prototypeRadius() { return get(PROTOTYPE_RADIUS, 150); }
    public static int fleijaRadius() { return get(FLEIJA_RADIUS, 50); }
    public static int missileRadius() { return get(MISSILE_RADIUS, 100); }
    public static int soliniumRadius() { return get(SOLINIUM_RADIUS, 150); }
    public static int n2Radius() { return get(N2_RADIUS, 200); }
    public static int blastSpeed() { return get(BLAST_SPEED, 1024); }
    /** BombConfig.mk5: milliseconds per tick for MK5 terrain work and EntityFalloutRain. */
    public static int mk5() { return get(MK5_BLAST_TIME, 50); }
    public static int falloutRange() { return get(FALLOUT_RANGE, 100); }
    public static int falloutDelay() { return get(FALLOUT_DELAY, 4); }

    // ToolConfig
    public static int recursionDepth() { return get(RECURSION_DEPTH, 1000); }
    public static boolean recursiveStone() { return get(RECURSION_STONE, false); }
    public static boolean recursiveNetherrack() { return get(RECURSION_NETHERRACK, false); }
    public static boolean abilityHammer() { return get(ABILITY_HAMMER, true); }
    public static boolean abilityVein() { return get(ABILITY_VEIN, true); }
    public static boolean abilityLuck() { return get(ABILITY_LUCK, true); }
    public static boolean abilitySilk() { return get(ABILITY_SILK, true); }
    public static boolean abilityFurnace() { return get(ABILITY_FURNACE, true); }
    public static boolean abilityShredder() { return get(ABILITY_SHREDDER, true); }
    public static boolean abilityCentrifuge() { return get(ABILITY_CENTRIFUGE, true); }
    public static boolean abilityCrystallizer() { return get(ABILITY_CRYSTALLIZER, true); }
    public static boolean abilityMercury() { return get(ABILITY_MERCURY, true); }
    public static boolean abilityExplosion() { return get(ABILITY_EXPLOSION, true); }

    // RadiationConfig
    public static int fogRad() { return get(FOG_RAD, 100); }
    /** CommonConfig.setDef(fogCh, 20): values <= 0 fall back to 20. */
    public static int fogCh() {
        int v = get(FOG_CH, 20);
        return v <= 0 ? 20 : v;
    }
    public static double hellRad() { return loaded() ? HELL_RAD.get() : 0.1D; }
    public static boolean worldRadEffects() { return get(WORLD_RAD_EFFECTS, true); }
    public static boolean cleanupDeadDirt() { return get(CLEANUP_DEAD_DIRT, false); }
    public static boolean enableContamination() { return get(ENABLE_CONTAMINATION, true); }
    public static boolean enableChunkRads() { return get(ENABLE_CHUNK_RADS, true); }
    public static boolean disableHot() { return get(DISABLE_HOT, false); }
    public static boolean disableExplosive() { return get(DISABLE_EXPLOSIVE, false); }
    public static boolean disableHydro() { return get(DISABLE_HYDRO, false); }
    public static boolean disableBlinding() { return get(DISABLE_BLINDING, false); }
}
