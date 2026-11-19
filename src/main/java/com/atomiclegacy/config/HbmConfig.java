package com.atomiclegacy.config;

import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

public final class HbmConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_FLUID_CONTAINER_COMPAT;

    public static final ModConfigSpec.BooleanValue ENABLE_528_MODE;
    // [parity H1: potion sickness] PotionConfig 8.S0_potionSickness: "NORMAL", "TERRARIA", anything else = off.
    public static final ModConfigSpec.ConfigValue<String> POTION_SICKNESS;
    public static final ModConfigSpec.BooleanValue ENABLE_528_FORCE_REASIM_BOILERS;
    public static final ModConfigSpec.BooleanValue ENABLE_528_COLTAN_DEPOSIT;
    public static final ModConfigSpec.BooleanValue ENABLE_528_COLTAN_SPAWN;
    public static final ModConfigSpec.BooleanValue ENABLE_528_NETHER_BURN;
    public static final ModConfigSpec.BooleanValue ENABLE_528_PRESSURIZED_RECIPES;
    public static final ModConfigSpec.BooleanValue ENABLE_528_EXPLOSIVE_ENERGISTICS;
    public static final ModConfigSpec.BooleanValue ENABLE_528_MACHINE_GRAVITY;
    public static final ModConfigSpec.BooleanValue ENABLE_EXPENSIVE_MODE;
    public static final ModConfigSpec.BooleanValue ENABLE_BOMBER_SHORT_MODE;
    /** 1.7.10 GeneralConfig.enableCataclysm; drops a burning FOEQ for every real mob death. */
    public static final ModConfigSpec.BooleanValue ENABLE_CATACLYSM;
    /** 1.7.10 GeneralConfig.enableLBSM; mutually exclusive with 528 mode. */
    public static final ModConfigSpec.BooleanValue ENABLE_LESS_BULLSHIT_MODE;
    public static final ModConfigSpec.BooleanValue LBSM_FULL_SCHRAB;
    public static final ModConfigSpec.BooleanValue LBSM_SHORTER_DECAY;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_ARMOR_RECIPES;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_TOOL_RECIPES;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_ALLOY;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_CHEMISTRY;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_CENTRIFUGE;
    public static final ModConfigSpec.BooleanValue LBSM_UNLOCK_ANVIL;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_CRAFTING;
    public static final ModConfigSpec.BooleanValue LBSM_SIMPLE_MEDICINE_RECIPES;
    public static final ModConfigSpec.BooleanValue LBSM_SAFE_CRATES;
    public static final ModConfigSpec.BooleanValue LBSM_SAFE_ME_DRIVES;
    public static final ModConfigSpec.IntValue LBSM_SCHRAB_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_VIRUS;
    /** Allows glowing mushrooms to convert waste earth into waste mycelium. */
    public static final ModConfigSpec.BooleanValue ENABLE_MYCELIUM_SPREAD;
    public static final ModConfigSpec.BooleanValue DROPPED_XEN_CRYSTAL_EFFECT;
    public static final ModConfigSpec.BooleanValue DROPPED_ANTIMATTER_CELL_EFFECT;
    // [parity c1] WeaponConfig.dropStar (WeaponizedCell)
    public static final ModConfigSpec.BooleanValue DROPPED_STAR_CELL_EFFECT;
    /** 1.7.10 BombConfig.aSchrabRadius; radius of a dropped anti-schrabidium cell. */
    public static final ModConfigSpec.IntValue DROPPED_ANTI_SCHRABIDIUM_RADIUS;
    public static final ModConfigSpec.BooleanValue DROPPED_SINGULARITY_EFFECT;
    public static final ModConfigSpec.BooleanValue SCALE_RTG_POWER;
    public static final ModConfigSpec.BooleanValue ENABLE_RTG_DECAY;
    public static final ModConfigSpec.BooleanValue ENABLE_MACHINE_GRAVITY;
    public static final ModConfigSpec.IntValue AUTOCAL_MAX_CLOCK;
    public static final ModConfigSpec.IntValue CONVEYOR_CRAM_MAX;
    public static final ModConfigSpec.BooleanValue CONVEYOR_CRAM_EXPLODE;
    public static final ModConfigSpec.BooleanValue ULTRA_LARP_MODE;
    public static final ModConfigSpec.IntValue INDUSTRIAL_TURBINE_INPUT_CAPACITY;
    public static final ModConfigSpec.IntValue INDUSTRIAL_TURBINE_OUTPUT_CAPACITY;
    public static final ModConfigSpec.DoubleValue INDUSTRIAL_TURBINE_EFFICIENCY;
    public static final ModConfigSpec.LongValue STEAM_TURBINE_MAX_POWER;
    public static final ModConfigSpec.IntValue STEAM_TURBINE_INPUT_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_TURBINE_OUTPUT_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_TURBINE_MAX_STEAM_PER_TICK;
    public static final ModConfigSpec.DoubleValue STEAM_TURBINE_EFFICIENCY;
    public static final ModConfigSpec.IntValue STEAM_ENGINE_STEAM_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_ENGINE_SPENT_STEAM_CAPACITY;
    public static final ModConfigSpec.DoubleValue STEAM_ENGINE_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue STIRLING_DIFFUSION;
    public static final ModConfigSpec.DoubleValue STIRLING_EFFICIENCY;
    public static final ModConfigSpec.IntValue STIRLING_MAX_HEAT_NORMAL;
    public static final ModConfigSpec.IntValue STIRLING_MAX_HEAT_STEEL;
    public static final ModConfigSpec.IntValue STIRLING_OVERSPEED_LIMIT;
    public static final ModConfigSpec.LongValue FORCEFIELD_MAX_POWER;
    public static final ModConfigSpec.IntValue FORCEFIELD_BASE_CONSUMPTION;
    public static final ModConfigSpec.IntValue FORCEFIELD_RADIUS_CONSUMPTION;
    public static final ModConfigSpec.IntValue FORCEFIELD_SHIELD_CONSUMPTION;
    public static final ModConfigSpec.IntValue FORCEFIELD_BASE_RADIUS;
    public static final ModConfigSpec.IntValue FORCEFIELD_RADIUS_UPGRADE;
    public static final ModConfigSpec.IntValue FORCEFIELD_SHIELD_UPGRADE;
    public static final ModConfigSpec.DoubleValue FORCEFIELD_COOLDOWN_MODIFIER;
    public static final ModConfigSpec.DoubleValue FORCEFIELD_HEALTH_REGEN_MODIFIER;
    public static final ModConfigSpec.LongValue RADAR_POWER_CAP;
    public static final ModConfigSpec.LongValue RADAR_CONSUMPTION;
    public static final ModConfigSpec.IntValue RADAR_RANGE;
    public static final ModConfigSpec.IntValue RADAR_LARGE_RANGE;
    public static final ModConfigSpec.IntValue RADAR_BUFFER;
    public static final ModConfigSpec.IntValue RADAR_ALTITUDE;
    public static final ModConfigSpec.IntValue RADAR_CHUNK_LOAD_CAP;
    public static final ModConfigSpec.BooleanValue RADAR_GENERATE_CHUNKS;
    public static final ModConfigSpec.IntValue CIWS_ACCURACY;
    /** 1.7.10 BombConfig.fatmanRadius; used by the atomic bomber payload. */
    public static final ModConfigSpec.IntValue FATMAN_RADIUS;
    public static final ModConfigSpec.DoubleValue MINE_AP_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_HE_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_SHRAP_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_NUKE_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_NAVAL_DAMAGE;
    public static final ModConfigSpec.LongValue HE_TO_FE_HE_USED;
    public static final ModConfigSpec.LongValue HE_TO_FE_FE_CREATED;
    public static final ModConfigSpec.DoubleValue HE_TO_FE_INPUT_DECAY;
    public static final ModConfigSpec.LongValue FE_TO_HE_FE_USED;
    public static final ModConfigSpec.LongValue FE_TO_HE_HE_CREATED;
    public static final ModConfigSpec.DoubleValue FE_TO_HE_INPUT_DECAY;
    /** 1.12's GeneralConfig.conversionRateHeToRF (one HE becomes this many FE/RF). */
    public static final ModConfigSpec.DoubleValue HE_TO_FE_CONVERSION_RATE;
    /** 1.12's GeneralConfig.autoCableConversion; absent in 1.7.10, so disabled by default. [parity N1: FE bridge off] Also gates the generic machine FE capability. */
    public static final ModConfigSpec.BooleanValue AUTO_CABLE_CONVERSION;
    public static final ModConfigSpec.BooleanValue ENABLE_CREATE_ADDITION_MOTOR_DEFAULTS;
    public static final ModConfigSpec.IntValue CREATE_ADDITION_FE_AT_MAX_RPM;
    public static final ModConfigSpec.IntValue CREATE_ADDITION_MAX_STRESS;
    public static final ModConfigSpec.BooleanValue ENABLE_IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_DEFAULTS;
    public static final ModConfigSpec.IntValue IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_OUTPUT;
    public static final ModConfigSpec.IntValue IMMERSIVE_ENGINEERING_HV_WIRE_TRANSFER_RATE;
    public static final ModConfigSpec.IntValue IMMERSIVE_ENGINEERING_HV_CONNECTOR_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_POLLUTION;
    /** 1.7.10 RadiationConfig.disableAsbestos inverse; default false. */
    public static final ModConfigSpec.BooleanValue ENABLE_ASBESTOS;
    /** 1.7.10 RadiationConfig.disableCoal inverse; default false. */
    public static final ModConfigSpec.BooleanValue ENABLE_COAL_DUST;
    public static final ModConfigSpec.BooleanValue ENABLE_LEAD_FROM_BLOCKS;
    public static final ModConfigSpec.BooleanValue ENABLE_LEAD_POISONING;
    public static final ModConfigSpec.BooleanValue ENABLE_SOOT_FOG;
    public static final ModConfigSpec.BooleanValue ENABLE_POISON_EFFECT;
    public static final ModConfigSpec.BooleanValue TAINT_TRAILS;
    public static final ModConfigSpec.DoubleValue POLLUTION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue POLLUTION_MOB_BUFF_THRESHOLD;
    public static final ModConfigSpec.DoubleValue POLLUTION_SOOT_FOG_THRESHOLD;
    public static final ModConfigSpec.DoubleValue POLLUTION_SOOT_FOG_DIVISOR;
    public static final ModConfigSpec.DoubleValue GLYPHID_RAMPANT_SMOKESTACK_OVERRIDE;
    public static final ModConfigSpec.BooleanValue GLYPHID_ENABLE_HIVES;
    public static final ModConfigSpec.IntValue GLYPHID_HIVE_SPAWN;
    public static final ModConfigSpec.ConfigValue<String> LEGACY_DUNGEON_SPAWN_FLAG;
    public static final ModConfigSpec.DoubleValue GLYPHID_SCOUT_SOOT_THRESHOLD;
    public static final ModConfigSpec.IntValue GLYPHID_SCOUT_SWARM_CHANCE;
    public static final ModConfigSpec.IntValue GLYPHID_LARGE_HIVE_CHANCE;
    public static final ModConfigSpec.DoubleValue GLYPHID_TARGETING_THRESHOLD;
    public static final ModConfigSpec.BooleanValue GLYPHID_NATURAL_SCOUT_SPAWN;
    public static final ModConfigSpec.DoubleValue GLYPHID_NATURAL_SCOUT_THRESHOLD;
    public static final ModConfigSpec.IntValue GLYPHID_NATURAL_SCOUT_CHANCE;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_MODE;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_EXTENDED_TARGETING;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_DIG;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_GUIDANCE;
    public static final ModConfigSpec.BooleanValue GLYPHID_SCOUT_INITIAL_SPAWN;
    public static final ModConfigSpec.BooleanValue GLYPHID_WAYPOINT_DEBUG;
    public static final ModConfigSpec.IntValue GLYPHID_SWARM_COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue GLYPHID_BASE_SWARM_SIZE;
    public static final ModConfigSpec.DoubleValue GLYPHID_SWARM_SCALING_MULTIPLIER;
    public static final ModConfigSpec.IntValue GLYPHID_SOOT_STEP;
    public static final ModConfigSpec.DoubleValue GLYPHID_SPAWN_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_GRUNT_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_BRAWLER_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_BOMBARDIER_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_BLASTER_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_DIGGER_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_BEHEMOTH_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_BRENDA_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GLYPHID_NUCLEAR_CHANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_STAT_REREGISTERING;
    public static final ModConfigSpec.BooleanValue ENABLE_DUCKS;
    public static final ModConfigSpec.BooleanValue ENABLE_MOB_GEAR;
    public static final ModConfigSpec.BooleanValue ENABLE_MOB_WEAPONS;
    public static final ModConfigSpec.DoubleValue MOB_WEAPON_SOOT_REDUCTION;
    public static final ModConfigSpec.BooleanValue ENABLE_MASK_MAN;
    public static final ModConfigSpec.IntValue MASK_MAN_DELAY;
    public static final ModConfigSpec.IntValue MASK_MAN_MIN_RAD;
    public static final ModConfigSpec.BooleanValue MASK_MAN_UNDERGROUND;
    public static final ModConfigSpec.BooleanValue ENABLE_FBI_RAIDS;
    public static final ModConfigSpec.IntValue FBI_RAID_DELAY;
    public static final ModConfigSpec.IntValue FBI_RAID_CHANCE;
    public static final ModConfigSpec.IntValue FBI_RAID_AMOUNT;
    public static final ModConfigSpec.IntValue FBI_RAID_DRONES;
    public static final ModConfigSpec.IntValue FBI_RAID_ATTACK_DISTANCE;
    public static final ModConfigSpec.IntValue FBI_RAID_ATTACK_DELAY;
    public static final ModConfigSpec.IntValue FBI_RAID_ATTACK_REACH;
    public static final ModConfigSpec.BooleanValue ENABLE_MELTDOWN_ELEMENTALS;
    public static final ModConfigSpec.IntValue ELEMENTAL_DELAY;
    public static final ModConfigSpec.IntValue ELEMENTAL_CHANCE;
    public static final ModConfigSpec.IntValue ELEMENTAL_AMOUNT;
    public static final ModConfigSpec.IntValue ELEMENTAL_DISTANCE;
    public static final ModConfigSpec.IntValue RBMK_COLUMN_HEIGHT;
    public static final ModConfigSpec.DoubleValue RBMK_PASSIVE_COOLING;
    public static final ModConfigSpec.DoubleValue RBMK_PASSIVE_COOLING_INNER;
    public static final ModConfigSpec.DoubleValue RBMK_COLUMN_HEAT_FLOW;
    public static final ModConfigSpec.DoubleValue RBMK_FUEL_DIFFUSION_MOD;
    public static final ModConfigSpec.DoubleValue RBMK_HEAT_PROVISION;
    public static final ModConfigSpec.DoubleValue RBMK_BOILER_HEAT_CONSUMPTION;
    public static final ModConfigSpec.DoubleValue RBMK_CONTROL_SPEED;
    public static final ModConfigSpec.DoubleValue RBMK_REACTIVITY_MOD;
    public static final ModConfigSpec.DoubleValue RBMK_SURGE_MOD;
    public static final ModConfigSpec.DoubleValue RBMK_OUTGASSER_SPEED_MOD;
    public static final ModConfigSpec.IntValue RBMK_FLUX_RANGE;
    public static final ModConfigSpec.DoubleValue RBMK_MODERATOR_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue RBMK_ABSORBER_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue RBMK_ABSORBER_HEAT_CONVERSION;
    public static final ModConfigSpec.DoubleValue RBMK_REFLECTOR_EFFICIENCY;
    public static final ModConfigSpec.BooleanValue RBMK_REASIM_BOILERS;
    public static final ModConfigSpec.DoubleValue RBMK_REASIM_BOILER_SPEED;
    public static final ModConfigSpec.BooleanValue RBMK_DISABLE_MELTDOWNS;
    public static final ModConfigSpec.BooleanValue RBMK_OVERPRESSURE;
    public static final ModConfigSpec.BooleanValue RBMK_ENABLE_DEPLETION;
    public static final ModConfigSpec.BooleanValue RBMK_ENABLE_XENON;
    public static final ModConfigSpec.IntValue RBMK_FALLOUT_RANGE;
    public static final ModConfigSpec.IntValue RBMK_FALLOUT_DELAY;
    public static final ModConfigSpec.BooleanValue RBMK_PERMANENT_SCRAP;

    public static final ModConfigSpec.BooleanValue ENABLE_NETHER_ORES;
    public static final ModConfigSpec.BooleanValue ENABLE_NETHER_PLUTONIUM_ORE;
    public static final ModConfigSpec.BooleanValue ENABLE_RADIATION_HOTSPOTS;
    public static final ModConfigSpec.IntValue RADIATION_HOTSPOT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue GAS_BUBBLE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue EXPLOSIVE_GAS_BUBBLE_SPAWN_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_SULFUR_CAVES;
    public static final ModConfigSpec.BooleanValue ENABLE_ASBESTOS_CAVES;
    public static final ModConfigSpec.BooleanValue ENABLE_HEMATITE_DEPOSITS;
    public static final ModConfigSpec.BooleanValue ENABLE_MALACHITE_DEPOSITS;
    public static final ModConfigSpec.BooleanValue ENABLE_BAUXITE_DEPOSITS;
    public static final ModConfigSpec.BooleanValue GENERATE_OIL_DEPOSITS;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_DRY_BIOME_DIVISOR;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_MIN_RADIUS;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_MAX_RADIUS;

    public static final ModConfigSpec.BooleanValue GENERATE_OIL_SAND_DEPOSITS;
    public static final ModConfigSpec.IntValue OIL_SAND_DEPOSIT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue OIL_SAND_DEPOSIT_MIN_RADIUS;
    public static final ModConfigSpec.IntValue OIL_SAND_DEPOSIT_MAX_RADIUS;

    public static final ModConfigSpec.BooleanValue GENERATE_BEDROCK_OIL_DEPOSITS;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_DEPOSIT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_DXZ_LIMIT;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_MAX_Y_OFFSET;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_L1_MAX;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_SURFACE_RADIUS;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_SURFACE_ATTEMPTS;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_VEIN_COUNT;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_VEIN_SIZE;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_MIN_Y;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_Y_VARIANCE;
    public static final ModConfigSpec.BooleanValue GENERATE_BEDROCK_ORES;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_NETHER_GLOWSTONE_WEIGHT;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_NETHER_PHOSPHORUS_WEIGHT;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_NETHER_QUARTZ_WEIGHT;
    public static final ModConfigSpec.IntValue METEORITE_SPAWN;
    public static final ModConfigSpec.IntValue CHLORINE_GEYSER_SPAWN_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_METEOR_STRIKES;
    public static final ModConfigSpec.BooleanValue ENABLE_METEOR_SHOWERS;
    public static final ModConfigSpec.BooleanValue ENABLE_METEOR_TAILS;
    public static final ModConfigSpec.BooleanValue ENABLE_SPECIAL_METEORS;
    /** GeneralConfig.enableGuns ("1.20_enableGuns"). */
    public static final ModConfigSpec.BooleanValue ENABLE_GUNS;
    /** ServerConfig GUN_GREASEGUN_DAMAGE (XFactory9mm). */
    public static final ModConfigSpec.DoubleValue GUN_GREASEGUN_DAMAGE;
    /** ServerConfig GUN_LONG_PISTOL_DAMAGE (XFactory9mm). */
    public static final ModConfigSpec.DoubleValue GUN_LONG_PISTOL_DAMAGE;
    /** ServerConfig GUN_UZI_DAMAGE (XFactory9mm). */
    public static final ModConfigSpec.DoubleValue GUN_UZI_DAMAGE;
    /** ServerConfig GUN_UZI_AKIMBO_LEFT_DAMAGE (XFactory9mm). */
    public static final ModConfigSpec.DoubleValue GUN_UZI_AKIMBO_LEFT_DAMAGE;
    /** ServerConfig GUN_UZI_AKIMBO_RIGHT_DAMAGE (XFactory9mm). */
    public static final ModConfigSpec.DoubleValue GUN_UZI_AKIMBO_RIGHT_DAMAGE;
    /** ServerConfig GUN_ASSAULT_RIFLE_DAMAGE (XFactory556mm; registered but unused by gun_g3, as in the original). */
    public static final ModConfigSpec.DoubleValue GUN_ASSAULT_RIFLE_DAMAGE;
    /** ServerConfig GUN_ZEBRA_DAMAGE (XFactory556mm, weapons batch D2). */
    public static final ModConfigSpec.DoubleValue GUN_ZEBRA_DAMAGE;
    /** ServerConfig GUN_STG77_DAMAGE (XFactory556mm). */
    public static final ModConfigSpec.DoubleValue GUN_STG77_DAMAGE;
    // [weapons batch A: config fields]
    /** ServerConfig GUN_AM180_DAMAGE (XFactory22lr). */
    public static final ModConfigSpec.DoubleValue GUN_AM180_DAMAGE;
    /** ServerConfig GUN_STARF_DAMAGE (XFactory22lr). */
    public static final ModConfigSpec.DoubleValue GUN_STARF_DAMAGE;
    /** ServerConfig GUN_STARF_AKIMBO_LEFT_DAMAGE (XFactory22lr). */
    public static final ModConfigSpec.DoubleValue GUN_STARF_AKIMBO_LEFT_DAMAGE;
    /** ServerConfig GUN_STARF_AKIMBO_RIGHT_DAMAGE (XFactory22lr). */
    public static final ModConfigSpec.DoubleValue GUN_STARF_AKIMBO_RIGHT_DAMAGE;
    /** ServerConfig GUN_ANTI_MATERIEL_DAMAGE (XFactory50). */
    public static final ModConfigSpec.DoubleValue GUN_ANTI_MATERIEL_DAMAGE;
    /** ServerConfig GUN_SUBTLETY_DAMAGE (XFactory50, weapons batch D2). */
    public static final ModConfigSpec.DoubleValue GUN_SUBTLETY_DAMAGE;
    /** ServerConfig GUN_PENANCE_DAMAGE (XFactory50, weapons batch D2). */
    public static final ModConfigSpec.DoubleValue GUN_PENANCE_DAMAGE;
    /** ServerConfig GUN_M2_DAMAGE (XFactory50). */
    public static final ModConfigSpec.DoubleValue GUN_M2_DAMAGE;
    // [weapons batch B: config fields]
    // [weapons batch C1: config fields]
    /** ServerConfig GUN_LIGHT_REVOLVER_DAMAGE (XFactory357). */
    public static final ModConfigSpec.DoubleValue GUN_LIGHT_REVOLVER_DAMAGE;
    /** ServerConfig GUN_ATLAS_DAMAGE (XFactory357). */
    public static final ModConfigSpec.DoubleValue GUN_ATLAS_DAMAGE;
    /** ServerConfig GUN_DANI_CELESTIAL_DAMAGE (XFactory357). */
    public static final ModConfigSpec.DoubleValue GUN_DANI_CELESTIAL_DAMAGE;
    /** ServerConfig GUN_DANI_LUNAR_DAMAGE (XFactory357). */
    public static final ModConfigSpec.DoubleValue GUN_DANI_LUNAR_DAMAGE;
    /** ServerConfig GUN_PEPPERBOX_DAMAGE (XFactoryBlackPowder). */
    public static final ModConfigSpec.DoubleValue GUN_PEPPERBOX_DAMAGE;
    /** ServerConfig GUN_HENRY_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_HENRY_DAMAGE;
    /** ServerConfig GUN_LINCOLN_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_LINCOLN_DAMAGE;
    /** ServerConfig GUN_HEAVY_REVOLVER_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_HEAVY_REVOLVER_DAMAGE;
    /** ServerConfig GUN_LILMAC_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_LILMAC_DAMAGE;
    /** ServerConfig GUN_PROTEGE_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_PROTEGE_DAMAGE;
    /** ServerConfig GUN_HANGMAN_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_HANGMAN_DAMAGE;
    /** ServerConfig GUN_HANGMAN_SMACK_DAMAGE (XFactory44). */
    public static final ModConfigSpec.DoubleValue GUN_HANGMAN_SMACK_DAMAGE;
    // [weapons batch C2: config fields]
    // [weapons batch C3: config fields]
    /** ServerConfig GUN_CARBINE_DAMAGE (XFactory762mm). */
    public static final ModConfigSpec.DoubleValue GUN_CARBINE_DAMAGE;
    /** ServerConfig GUN_CARBINE_BAYONET_DAMAGE (XFactory762mm) (WeaponModCarbineBayonet; the bayonet mod is not ported, registered but unused). */
    public static final ModConfigSpec.DoubleValue GUN_CARBINE_BAYONET_DAMAGE;
    /** ServerConfig GUN_MINIGUN_DAMAGE (XFactory762mm). */
    public static final ModConfigSpec.DoubleValue GUN_MINIGUN_DAMAGE;
    /** ServerConfig GUN_LACUNAE_DAMAGE (XFactory762mm). */
    public static final ModConfigSpec.DoubleValue GUN_LACUNAE_DAMAGE;
    // [weapons batch E2: config fields] ServerConfig GUN_TESLA_CANNON_DAMAGE ... GUN_LASER_RIFLE_DAMAGE (XFactoryEnergy).
    public static final ModConfigSpec.DoubleValue GUN_TESLA_CANNON_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_LASER_PISTOL_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_PEW_PEW_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_MORNING_GLORY_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_LASER_RIFLE_DAMAGE;
    // [weapons batch F1: config fields] ServerConfig GUN_FLAMER_DAMAGE, GUN_TOPAZ_DAMAGE, GUN_DAYBREAKER_DAMAGE (XFactoryFlamer).
    public static final ModConfigSpec.DoubleValue GUN_FLAMER_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_TOPAZ_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_DAYBREAKER_DAMAGE;
    // [weapons batch F3: config fields] ServerConfig GUN_EXTINGUISHER_DAMAGE, GUN_CHARGE_THROWER_DAMAGE (XFactoryTool) and
    // GUN_DRILL_DAMAGE (XFactoryDrill; 1.7.10 initConfig filed it under the key "GUN_DOUBLE_BARREL_DAMAGE", the port keeps its own key).
    public static final ModConfigSpec.DoubleValue GUN_EXTINGUISHER_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_CHARGE_THROWER_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_DRILL_DAMAGE;
    /** [parity c1] ServerConfig GUN_HERETIC_DAMAGE (XFactory10ga). */
    public static final ModConfigSpec.DoubleValue GUN_HERETIC_DAMAGE;
    /** [parity d2] ServerConfig GUN_MINIGUN_DUAL_LEFT_DAMAGE / GUN_MINIGUN_DUAL_RIGHT_DAMAGE (XFactory762mm, gun_minigun_dual). */
    public static final ModConfigSpec.DoubleValue GUN_MINIGUN_DUAL_LEFT_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_MINIGUN_DUAL_RIGHT_DAMAGE;
    // [parity c1] XFactoryPA ServerConfig values (power armor melee / ranged controllers)
    public static final ModConfigSpec.DoubleValue MELEE_RPA_SWINGS_DAMAGE;
    public static final ModConfigSpec.DoubleValue MELEE_RPA_SLAP_DAMAGE;
    public static final ModConfigSpec.DoubleValue MELEE_RPA_LARGE_MULT;
    public static final ModConfigSpec.DoubleValue MELEE_NCRPA_SWINGS_DAMAGE;
    public static final ModConfigSpec.DoubleValue MELEE_NCRPA_SWEEP_DAMAGE;
    public static final ModConfigSpec.DoubleValue MELEE_NCRPA_LARGE_MULT;
    public static final ModConfigSpec.DoubleValue RANGED_NCRPA_DAMAGE;
    // [weapons batch F2: config fields] ServerConfig GUN_TAU_CANNON_DAMAGE, GUN_TAU_CANNON_CHARGE_DAMAGE, GUN_COILGUN_DAMAGE, GUN_NI4NI_DAMAGE (XFactoryAccelerator), GUN_FATMAN_DAMAGE (XFactoryCatapult).
    public static final ModConfigSpec.DoubleValue GUN_TAU_CANNON_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_TAU_CANNON_CHARGE_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_COILGUN_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_NI4NI_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_FATMAN_DAMAGE;
    /** ServerConfig GUN_SOUTH_STAR_DAMAGE (XFactory762mm). */
    public static final ModConfigSpec.DoubleValue GUN_SOUTH_STAR_DAMAGE;
    /** ServerConfig GUN_SOUTH_STAR_BAYONET_DAMAGE (XFactory762mm) (WeaponModMASBayonet; the bayonet mod is not ported, registered but unused). */
    public static final ModConfigSpec.DoubleValue GUN_SOUTH_STAR_BAYONET_DAMAGE;
    // [weapons batch C4: config fields]
    /** ServerConfig GUN_DOUBLE_BARREL_DAMAGE (XFactory10ga). */
    public static final ModConfigSpec.DoubleValue GUN_DOUBLE_BARREL_DAMAGE;
    /** ServerConfig GUN_SACRED_DRAGON_DAMAGE (XFactory10ga). */
    public static final ModConfigSpec.DoubleValue GUN_SACRED_DRAGON_DAMAGE;
    /** ServerConfig GUN_MARES_LEG_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_MARES_LEG_DAMAGE;
    /** ServerConfig GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE;
    /** ServerConfig GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE;
    /** ServerConfig GUN_BROKEN_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_BROKEN_DAMAGE;
    /** ServerConfig GUN_LIBERATOR_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_LIBERATOR_DAMAGE;
    // [weapons batch D1: config fields]
    /** ServerConfig GUN_SPAS_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_SPAS_DAMAGE;
    /** ServerConfig GUN_AUTOSHOTGUN_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_AUTOSHOTGUN_DAMAGE;
    /** ServerConfig GUN_SHREDDER_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_SHREDDER_DAMAGE;
    /** ServerConfig GUN_SEXY_DAMAGE (XFactory12ga). */
    public static final ModConfigSpec.DoubleValue GUN_SEXY_DAMAGE;
    // [weapons batch D3: config fields]
    /** [weapons batch E1] ServerConfig GUN_PANZERSCHRECK/QUADRO/FLIEGERFAUST/MISSILE_LAUNCHER_DAMAGE (XFactoryRocket), GUN_BOLTER_DAMAGE (XFactory75Bolt). */
    public static final ModConfigSpec.DoubleValue GUN_PANZERSCHRECK_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_QUADRO_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_FLIEGERFAUST_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_MISSILE_LAUNCHER_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_BOLTER_DAMAGE;
    // [weapons batch F4: config fields]
    /** ServerConfig GUN_STINGER_DAMAGE (XFactoryRocket), GUN_ABERRATOR/EOTT_LEFT/EOTT_RIGHT_DAMAGE (XFactory35800); XFactoryFolly GUN_FOLLY_DAMAGE has no ServerConfig entry in 1.7.10. */
    public static final ModConfigSpec.DoubleValue GUN_STINGER_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_ABERRATOR_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_EOTT_LEFT_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_EOTT_RIGHT_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_FOLLY_DAMAGE;
    /** ServerConfig GUN_FLARE_DAMAGE, GUN_CONGO_LAKE_DAMAGE, GUN_MK108_DAMAGE (XFactory40mm). */
    public static final ModConfigSpec.DoubleValue GUN_FLARE_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_CONGO_LAKE_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUN_MK108_DAMAGE;
    // [parity P3: XFactoryTurret ServerConfig TURRET_*_DAMAGE]
    public static final ModConfigSpec.DoubleValue TURRET_CHEKHOV_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_FRIENDLY_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_FRITZ_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_HOWARD_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_METHUSALEM_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_JEREMY_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_MAXWELL_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_RICHARD_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_BROWN_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_EDWIN_DAMAGE;
    public static final ModConfigSpec.DoubleValue TURRET_TAUON_DAMAGE;
    public static final ModConfigSpec.IntValue METEOR_STRIKE_CHANCE;
    public static final ModConfigSpec.IntValue METEOR_SHOWER_CHANCE;
    public static final ModConfigSpec.IntValue METEOR_SHOWER_DURATION;

    public static final ModConfigSpec.BooleanValue GENERATE_HBM_STRUCTURES;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_MIN_CHUNKS;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_MAX_CHUNKS;
    public static final ModConfigSpec.BooleanValue HBM_STRUCTURE_ENABLE_RUINS;
    public static final ModConfigSpec.BooleanValue HBM_STRUCTURE_ENABLE_OCEAN;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_SPIRE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_VERTIBIRD_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_OIL_RIG_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_LIGHTHOUSE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_BEACHED_PATROL_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DISH_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_WATER_PUMP_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FOREST_CHEM_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_PLANE1_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_PLANE2_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DESERT_SHACK1_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DESERT_SHACK2_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DESERT_SHACK3_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DEAD_DISH_SMALL_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_LABORATORY_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FOREST_POST_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FACTORY_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_CRANE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RADIO_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FEATURES_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_BUNKER_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_A_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_B_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_C_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_D_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_E_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_F_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_G_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_H_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_I_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_J_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_TOWER_BASE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FALLOUT_SHELTER_HOUSE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_ABANDONED_ARCTIC_STATION_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_MILITARY_BASE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_SUBMARINE_WEIGHT; // [parity R3: submarine]
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_PLAINS_NULL_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_OCEAN_NULL_WEIGHT;
    public static final ModConfigSpec.IntValue ANCIENT_TOMB_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue ANTENNA_STRUCTURE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue ATOM_STRUCTURE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue LIBRARY_DUNGEON_SPAWN_RATE;
    public static final ModConfigSpec.IntValue DUD_STRUCTURE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue SPACESHIP_STRUCTURE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue WASTE_TANK_STRUCTURE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue BROADCASTER_SPAWN_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_WORLDGEN_LANDMINES;
    public static final ModConfigSpec.IntValue LANDMINE_SPAWN_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_528_BOSNIA_MINES;
    public static final ModConfigSpec.BooleanValue ENABLE_WORLDGEN_VAULTS;
    public static final ModConfigSpec.IntValue VAULT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue SOYUZ_CAPSULE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue ARCTIC_VAULT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue JUNGLE_DUNGEON_SPAWN_RATE;
    public static final ModConfigSpec.IntValue PINK_TREE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue STONE_KEYHOLE_SPAWN_RATE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLE_FLUID_CONTAINER_COMPAT = builder
                .comment("Original GeneralConfig.enableFluidContainerCompat: allow other mods' exact-capacity fluid containers in crafting.")
                .define("enableFluidContainerCompat", true);

        builder.push("528");
        ENABLE_528_MODE = builder
                .comment("是否启用 HBM 528 模式。默认与 HBM 1.12.2 一致：false。普通模式配方全部开放；启用后部分机器和配方会使用 528 模式，并启用完整蓝图权限。")
                .define("enable528Mode", false);
        ENABLE_528_FORCE_REASIM_BOILERS = builder
                .comment("528 模式下强制启用 RBMK ReaSim 锅炉。仅在 enable528Mode=true 时生效；旧版默认：true。")
                .define("forceReasimBoilers", true);
        ENABLE_528_COLTAN_DEPOSIT = builder
                .comment("是否生成由世界种子决定的钶钽大型矿区。对应 HBM 1.7.10 GeneralConfig.enable528ColtanDeposit，默认：true。")
                .define("enable528ColtanDeposit", true);
        ENABLE_528_COLTAN_SPAWN = builder
                .comment("是否让钶钽矿作为普通随机矿脉生成。对应 HBM 1.7.10 GeneralConfig.enable528ColtanSpawn，默认：false。")
                .define("enable528ColtanSpawning", false);
        ENABLE_528_NETHER_BURN = builder
                .comment("528 模式下使无火焰免疫的玩家在下界持续燃烧。仅在 enable528Mode=true 时生效；旧版默认：true。")
                .define("enableNetherBurn", true);
        ENABLE_528_PRESSURIZED_RECIPES = builder
                .comment("528 模式下启用化工配方的加压流体要求。仅在 enable528Mode=true 时生效；旧版默认：true。")
                .define("enablePressurizedRecipes", true);
        ENABLE_528_EXPLOSIVE_ENERGISTICS = builder
                .comment("528 模式下右键 AE2 方块会将其破坏并引爆。仅在 enable528Mode=true 时生效；旧版默认：true。")
                .define("enableExplosiveEnergistics", true);
        ENABLE_528_MACHINE_GRAVITY = builder
                .comment("528 模式下要求旧版指定的大型机器具有合格地基。仅在 enable528Mode=true 时生效；旧版默认：true。")
                .define("enableMachineGravity", true);
        ENABLE_EXPENSIVE_MODE = builder
                .comment("是否启用 HBM 1.7.10 昂贵模式。独立于 528 模式；切换 inputItemsEx 机器配方，并影响精密装配机故障产物的材料回收率。")
                .define("enableExpensiveMode", false);
        builder.pop();

        builder.push("general");
        // [parity H1: potion sickness]
        POTION_SICKNESS = builder
                .comment("HBM 1.7.10 PotionConfig 8.S0_potionSickness: valid configs include \"NORMAL\" and \"TERRARIA\", otherwise potion sickness is turned off. Default: OFF.")
                .define("potionSickness", "OFF");
        ENABLE_CATACLYSM = builder
                .comment("每个生物真正死亡时从 Y=500 坠落一枚燃烧中的 FOEQ 卫星。严格对应 HBM 1.7.10 enableCataclysm，默认：false。")
                .define("enableCataclysm", false);
        ENABLE_GUNS = builder
                .comment("HBM 1.7.10 GeneralConfig 1.20_enableGuns: prevents new system guns to be fired. Default: true.")
                .define("enableGuns", true);
        builder.pop();

        builder.push("weapons");
        GUN_GREASEGUN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_GREASEGUN_DAMAGE. Default: 3.0.")
                .defineInRange("gunGreasegunDamage", 3.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LONG_PISTOL_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LONG_PISTOL_DAMAGE. Default: 25.0.")
                .defineInRange("gunLongPistolDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_UZI_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_UZI_DAMAGE. Default: 3.0.")
                .defineInRange("gunUziDamage", 3.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_UZI_AKIMBO_LEFT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_UZI_AKIMBO_LEFT_DAMAGE. Default: 3.0.")
                .defineInRange("gunUziAkimboLeftDamage", 3.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_UZI_AKIMBO_RIGHT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_UZI_AKIMBO_RIGHT_DAMAGE. Default: 3.0.")
                .defineInRange("gunUziAkimboRightDamage", 3.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_ASSAULT_RIFLE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_ASSAULT_RIFLE_DAMAGE. Default: 5.0.")
                .defineInRange("gunAssaultRifleDamage", 5.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_ZEBRA_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_ZEBRA_DAMAGE. Default: 7.5.")
                .defineInRange("gunZebraDamage", 7.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_STG77_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_STG77_DAMAGE. Default: 10.0.")
                .defineInRange("gunStg77Damage", 10.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch A: config define]
        GUN_AM180_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_AM180_DAMAGE. Default: 2.0.")
                .defineInRange("gunAm180Damage", 2.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_STARF_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_STARF_DAMAGE. Default: 12.5.")
                .defineInRange("gunStarfDamage", 12.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_STARF_AKIMBO_LEFT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_STARF_AKIMBO_LEFT_DAMAGE. Default: 12.5.")
                .defineInRange("gunStarfAkimboLeftDamage", 12.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_STARF_AKIMBO_RIGHT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_STARF_AKIMBO_RIGHT_DAMAGE. Default: 12.5.")
                .defineInRange("gunStarfAkimboRightDamage", 12.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_ANTI_MATERIEL_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_ANTI_MATERIEL_DAMAGE. Default: 30.0.")
                .defineInRange("gunAntiMaterielDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_SUBTLETY_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SUBTLETY_DAMAGE. Default: 50.0.")
                .defineInRange("gunSubtletyDamage", 50.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_PENANCE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_PENANCE_DAMAGE. Default: 45.0.")
                .defineInRange("gunPenanceDamage", 45.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_M2_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_M2_DAMAGE. Default: 7.5.")
                .defineInRange("gunM2Damage", 7.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch B: config define]
        // [weapons batch C1: config define]
        GUN_LIGHT_REVOLVER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LIGHT_REVOLVER_DAMAGE. Default: 7.5.")
                .defineInRange("gunLightRevolverDamage", 7.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_ATLAS_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_ATLAS_DAMAGE. Default: 12.5.")
                .defineInRange("gunAtlasDamage", 12.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_DANI_CELESTIAL_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_DANI_CELESTIAL_DAMAGE. Default: 15.0.")
                .defineInRange("gunDaniCelestialDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_DANI_LUNAR_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_DANI_LUNAR_DAMAGE. Default: 15.0.")
                .defineInRange("gunDaniLunarDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_PEPPERBOX_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_PEPPERBOX_DAMAGE. Default: 5.0.")
                .defineInRange("gunPepperboxDamage", 5.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_HENRY_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_HENRY_DAMAGE. Default: 10.0.")
                .defineInRange("gunHenryDamage", 10.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LINCOLN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LINCOLN_DAMAGE. Default: 20.0.")
                .defineInRange("gunLincolnDamage", 20.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_HEAVY_REVOLVER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_HEAVY_REVOLVER_DAMAGE. Default: 15.0.")
                .defineInRange("gunHeavyRevolverDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LILMAC_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LILMAC_DAMAGE. Default: 30.0.")
                .defineInRange("gunLilmacDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_PROTEGE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_PROTEGE_DAMAGE. Default: 30.0.")
                .defineInRange("gunProtegeDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_HANGMAN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_HANGMAN_DAMAGE. Default: 25.0.")
                .defineInRange("gunHangmanDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_HANGMAN_SMACK_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_HANGMAN_SMACK_DAMAGE. Default: 10.0.")
                .defineInRange("gunHangmanSmackDamage", 10.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch C2: config define]
        // [weapons batch C3: config define]
        GUN_CARBINE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_CARBINE_DAMAGE. Default: 15.0.")
                .defineInRange("gunCarbineDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_CARBINE_BAYONET_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_CARBINE_BAYONET_DAMAGE. Default: 15.0.")
                .defineInRange("gunCarbineBayonetDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MINIGUN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MINIGUN_DAMAGE. Default: 6.0.")
                .defineInRange("gunMinigunDamage", 6.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LACUNAE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LACUNAE_DAMAGE. Default: 12.0.")
                .defineInRange("gunLacunaeDamage", 12.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch E2: config define]
        GUN_TESLA_CANNON_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_TESLA_CANNON_DAMAGE. Default: 35.0.")
                .defineInRange("gunTeslaCannonDamage", 35.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LASER_PISTOL_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LASER_PISTOL_DAMAGE. Default: 25.0.")
                .defineInRange("gunLaserPistolDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_PEW_PEW_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_PEW_PEW_DAMAGE. Default: 30.0.")
                .defineInRange("gunPewPewDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MORNING_GLORY_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MORNING_GLORY_DAMAGE. Default: 30.0.")
                .defineInRange("gunMorningGloryDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LASER_RIFLE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LASER_RIFLE_DAMAGE. Default: 50.0.")
                .defineInRange("gunLaserRifleDamage", 50.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch F1: config defines]
        GUN_FLAMER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_FLAMER_DAMAGE. Default: 1.0.")
                .defineInRange("gunFlamerDamage", 1.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_TOPAZ_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_TOPAZ_DAMAGE. Default: 1.5.")
                .defineInRange("gunTopazDamage", 1.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_DAYBREAKER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_DAYBREAKER_DAMAGE. Default: 25.0.")
                .defineInRange("gunDaybreakerDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch F3: config define]
        GUN_EXTINGUISHER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_EXTINGUISHER_DAMAGE. Default: 0.0.")
                .defineInRange("gunExtinguisherDamage", 0.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_CHARGE_THROWER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_CHARGE_THROWER_DAMAGE. Default: 10.0.")
                .defineInRange("gunChargeThrowerDamage", 10.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_DRILL_DAMAGE = builder
                .comment("HBM 1.7.10 XFactoryDrill GUN_DRILL_DAMAGE (filed under GUN_DOUBLE_BARREL_DAMAGE in 1.7.10). Default: 10.0.")
                .defineInRange("gunDrillDamage", 10.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_HERETIC_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_HERETIC_DAMAGE. Default: 100.0.")
                .defineInRange("gunHereticDamage", 100.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MINIGUN_DUAL_LEFT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MINIGUN_DUAL_LEFT_DAMAGE. Default: 6.0.")
                .defineInRange("gunMinigunDualLeftDamage", 6.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MINIGUN_DUAL_RIGHT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MINIGUN_DUAL_RIGHT_DAMAGE. Default: 6.0.")
                .defineInRange("gunMinigunDualRightDamage", 6.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        MELEE_RPA_SWINGS_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig MELEE_RPA_SWINGS_DAMAGE (XFactoryPA). Default: 15.0.")
                .defineInRange("meleeRpaSwingsDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        MELEE_RPA_SLAP_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig MELEE_RPA_SLAP_DAMAGE (XFactoryPA). Default: 35.0.")
                .defineInRange("meleeRpaSlapDamage", 35.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        MELEE_RPA_LARGE_MULT = builder
                .comment("HBM 1.7.10 ServerConfig MELEE_RPA_LARGE_MULT (XFactoryPA). Default: 2.5.")
                .defineInRange("meleeRpaLargeMult", 2.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        MELEE_NCRPA_SWINGS_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig MELEE_NCRPA_SWINGS_DAMAGE (XFactoryPA). Default: 15.0.")
                .defineInRange("meleeNcrpaSwingsDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        MELEE_NCRPA_SWEEP_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig MELEE_NCRPA_SWEEP_DAMAGE (XFactoryPA). Default: 35.0.")
                .defineInRange("meleeNcrpaSweepDamage", 35.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        MELEE_NCRPA_LARGE_MULT = builder
                .comment("HBM 1.7.10 ServerConfig MELEE_NCRPA_LARGE_MULT (XFactoryPA). Default: 2.5.")
                .defineInRange("meleeNcrpaLargeMult", 2.5D, -Double.MAX_VALUE, Double.MAX_VALUE);
        RANGED_NCRPA_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig RANGED_NCRPA_DAMAGE (XFactoryPA). Default: 25.0.")
                .defineInRange("rangedNcrpaDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch F2: config defaults]
        GUN_TAU_CANNON_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_TAU_CANNON_DAMAGE. Default: 25.0.")
                .defineInRange("gunTauCannonDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_TAU_CANNON_CHARGE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_TAU_CANNON_CHARGE_DAMAGE. Default: 5.0.")
                .defineInRange("gunTauCannonChargeDamage", 5.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_COILGUN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_COILGUN_DAMAGE. Default: 35.0.")
                .defineInRange("gunCoilgunDamage", 35.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_NI4NI_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_NI4NI_DAMAGE. Default: 35.0.")
                .defineInRange("gunNi4niDamage", 35.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_FATMAN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_FATMAN_DAMAGE. Default: 100.0.")
                .defineInRange("gunFatmanDamage", 100.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_SOUTH_STAR_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SOUTH_STAR_DAMAGE. Default: 30.0.")
                .defineInRange("gunSouthStarDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_SOUTH_STAR_BAYONET_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SOUTH_STAR_BAYONET_DAMAGE. Default: 15.0.")
                .defineInRange("gunSouthStarBayonetDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch C4: config define]
        GUN_DOUBLE_BARREL_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_DOUBLE_BARREL_DAMAGE. Default: 30.0.")
                .defineInRange("gunDoubleBarrelDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_SACRED_DRAGON_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SACRED_DRAGON_DAMAGE. Default: 45.0.")
                .defineInRange("gunSacredDragonDamage", 45.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MARES_LEG_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MARES_LEG_DAMAGE. Default: 16.0.")
                .defineInRange("gunMaresLegDamage", 16.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE. Default: 16.0.")
                .defineInRange("gunMaresLegAkimboLeftDamage", 16.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE. Default: 16.0.")
                .defineInRange("gunMaresLegAkimboRightDamage", 16.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_BROKEN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_BROKEN_DAMAGE. Default: 48.0.")
                .defineInRange("gunBrokenDamage", 48.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_LIBERATOR_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_LIBERATOR_DAMAGE. Default: 16.0.")
                .defineInRange("gunLiberatorDamage", 16.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch D1: config define]
        GUN_SPAS_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SPAS_DAMAGE. Default: 32.0.")
                .defineInRange("gunSpasDamage", 32.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_AUTOSHOTGUN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_AUTOSHOTGUN_DAMAGE. Default: 48.0.")
                .defineInRange("gunAutoshotgunDamage", 48.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_SHREDDER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SHREDDER_DAMAGE. Default: 50.0.")
                .defineInRange("gunShredderDamage", 50.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_SEXY_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_SEXY_DAMAGE. Default: 64.0.")
                .defineInRange("gunSexyDamage", 64.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch D3: config define]
        GUN_PANZERSCHRECK_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_PANZERSCHRECK_DAMAGE. Default: 25.0.")
                .defineInRange("gunPanzerschreckDamage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_QUADRO_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_QUADRO_DAMAGE. Default: 40.0.")
                .defineInRange("gunQuadroDamage", 40.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_FLIEGERFAUST_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_FLIEGERFAUST_DAMAGE. Default: 15.0.")
                .defineInRange("gunFliegerfaustDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MISSILE_LAUNCHER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MISSILE_LAUNCHER_DAMAGE. Default: 50.0.")
                .defineInRange("gunMissileLauncherDamage", 50.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_BOLTER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_BOLTER_DAMAGE. Default: 15.0.")
                .defineInRange("gunBolterDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [weapons batch F4: config define]
        GUN_STINGER_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_STINGER_DAMAGE. Default: 35.0.")
                .defineInRange("gunStingerDamage", 35.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_ABERRATOR_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_ABERRATOR_DAMAGE. Default: 100.0.")
                .defineInRange("gunAberratorDamage", 100.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_EOTT_LEFT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_EOTT_LEFT_DAMAGE. Default: 100.0.")
                .defineInRange("gunEottLeftDamage", 100.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_EOTT_RIGHT_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_EOTT_RIGHT_DAMAGE. Default: 100.0.")
                .defineInRange("gunEottRightDamage", 100.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_FOLLY_DAMAGE = builder
                .comment("HBM 1.7.10 XFactoryFolly GUN_FOLLY_DAMAGE (no ServerConfig entry in the original). Default: 1000.0.")
                .defineInRange("gunFollyDamage", 1000.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_FLARE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_FLARE_DAMAGE. Default: 15.0.")
                .defineInRange("gunFlareDamage", 15.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_CONGO_LAKE_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_CONGO_LAKE_DAMAGE. Default: 20.0.")
                .defineInRange("gunCongoLakeDamage", 20.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        GUN_MK108_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig GUN_MK108_DAMAGE. Default: 25.0.")
                .defineInRange("gunMk108Damage", 25.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        // [parity P3: XFactoryTurret ServerConfig TURRET_*_DAMAGE]
        TURRET_CHEKHOV_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_CHEKHOV_DAMAGE. Default: 10.0.")
                .defineInRange("turretChekhovDamage", 10.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_FRIENDLY_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_FRIENDLY_DAMAGE. Default: 5.0.")
                .defineInRange("turretFriendlyDamage", 5.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_FRITZ_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_FRITZ_DAMAGE. Default: 1.0.")
                .defineInRange("turretFritzDamage", 1.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_HOWARD_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_HOWARD_DAMAGE. Default: 2.0.")
                .defineInRange("turretHowardDamage", 2.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_METHUSALEM_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_METHUSALEM_DAMAGE. Default: 2.0.")
                .defineInRange("turretMethusalemDamage", 2.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_JEREMY_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_JEREMY_DAMAGE. Default: 50.0.")
                .defineInRange("turretJeremyDamage", 50.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_MAXWELL_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_MAXWELL_DAMAGE. Default: 0.25.")
                .defineInRange("turretMaxwellDamage", 0.25D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_RICHARD_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_RICHARD_DAMAGE. Default: 30.0.")
                .defineInRange("turretRichardDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_BROWN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_BROWN_DAMAGE. Default: 5.0.")
                .defineInRange("turretBrownDamage", 5.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_EDWIN_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_EDWIN_DAMAGE. Default: 5.0.")
                .defineInRange("turretEdwinDamage", 5.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        TURRET_TAUON_DAMAGE = builder
                .comment("HBM 1.7.10 ServerConfig TURRET_TAUON_DAMAGE. Default: 30.0.")
                .defineInRange("turretTauonDamage", 30.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        builder.pop();

        builder.push("less_bullshit_mode");
        ENABLE_LESS_BULLSHIT_MODE = builder
                .comment("HBM 1.7.10 Less Bullshit Mode 总开关。528 模式启用时强制失效。默认：false。")
                .define("enableLessBullshitMode", false);
        LBSM_FULL_SCHRAB = builder
                .comment("将嬗变器的锶铀合金产物替换为完整施拉比锭。旧版默认：true。")
                .define("fullSchrab", true);
        LBSM_SHORTER_DECAY = builder
                .comment("大幅缩短核废料桶衰变时间。旧版默认：true。")
                .define("shorterDecay", true);
        LBSM_SIMPLE_ARMOR_RECIPES = builder
                .comment("启用简化的星辉金属与施拉比护甲配方。旧版默认：true。")
                .define("simpleArmorRecipes", true);
        LBSM_SIMPLE_TOOL_RECIPES = builder
                .comment("启用简化的钴装饰、星辉金属与施拉比工具配方。旧版默认：true。")
                .define("simpleToolRecipes", true);
        LBSM_SIMPLE_ALLOY = builder
                .comment("启用旧版 LBSM 简化合金选项。旧版最新版保留该开关但没有消费点；默认：true。")
                .define("simpleAlloy", true);
        LBSM_SIMPLE_CHEMISTRY = builder
                .comment("启用简化的化工厂与高炉化学配方。旧版默认：true。")
                .define("simpleChemistry", true);
        LBSM_SIMPLE_CENTRIFUGE = builder
                .comment("提高离心机若干矿石配方的产出。旧版默认：true。")
                .define("simpleCentrifuge", true);
        LBSM_UNLOCK_ANVIL = builder
                .comment("将全部砧配方开放到等级 1。旧版默认：true。")
                .define("unlockAnvil", true);
        LBSM_SIMPLE_CRAFTING = builder
                .comment("启用旧版 LBSM 的简化合成与焊接配方。旧版默认：true。")
                .define("simpleCrafting", true);
        LBSM_SIMPLE_MEDICINE_RECIPES = builder
                .comment("启用更便宜的 Siox 与 Xanax 配方。旧版默认：true。")
                .define("simpleMedicineRecipes", true);
        LBSM_SAFE_CRATES = builder
                .comment("阻止容器内容向外传递辐射。当前新版辐射扫描不递归容器，因此保持旧版开关以维持配置对齐。旧版默认：true。")
                .define("safeCrates", true);
        LBSM_SAFE_ME_DRIVES = builder
                .comment("阻止 AE 存储内容向外传递辐射。当前新版辐射扫描不递归 AE 存储，因此保持旧版开关以维持配置对齐。旧版默认：true。")
                .define("safeMEDrives", true);
        LBSM_SCHRAB_RATE = builder
                .comment("核爆将铀矿转化为施拉比矿石的平均分母。旧版 LBSM 默认：20。")
                .defineInRange("schrabRate", 20, 1, Integer.MAX_VALUE);
        builder.pop();


        builder.push("airstrikes");
        ENABLE_BOMBER_SHORT_MODE = builder
                .comment("Spawn bombers closer to the target. HBM 1.7.10 default: false.")
                .define("enableBomberShortMode", false);
        builder.pop();

        builder.push("dangerousItems");
        DROPPED_XEN_CRYSTAL_EFFECT = builder
                .comment("Whether dropped Xen crystals move terrain. HBM 1.7.10 default: true.")
                .define("droppedXenCrystalEffect", true);
        DROPPED_ANTIMATTER_CELL_EFFECT = builder
                .comment("Whether dropped antimatter cells and pellets detonate. HBM 1.7.10 WeaponConfig.dropCell default: true. The dropped item is consumed either way.")
                .define("droppedAntimatterCellEffect", true);
        DROPPED_STAR_CELL_EFFECT = builder
                .comment("Whether rigged star blaster cells should explode when dropped. HBM 1.7.10 WeaponConfig.dropStar default: true. The dropped item is consumed either way.")
                .define("droppedStarCellEffect", true);
        DROPPED_ANTI_SCHRABIDIUM_RADIUS = builder
                .comment("Radius of the Fleija blast created by a dropped anti-schrabidium cell. HBM 1.7.10 BombConfig.aSchrabRadius default: 20.")
                .defineInRange("droppedAntiSchrabidiumRadius", 20, 1, 1000);
        DROPPED_SINGULARITY_EFFECT = builder
                .comment("Whether dropped singularities, black holes and digamma particles spawn their singularity effect. HBM 1.7.10 WeaponConfig.dropSing default: true. The dropped item is consumed either way.")
                .define("droppedSingularityEffect", true);
        ENABLE_VIRUS = builder
                .comment("Allow crystal virus blocks to spread. HBM 1.7.10 default: false.")
                .define("enableVirus", false);
        ENABLE_MYCELIUM_SPREAD = builder
                .comment("Allow glowing mushrooms to spread waste mycelium beneath them. HBM 1.7.10 default: false.")
                .define("enableMyceliumSpread", false);
        builder.pop();

        builder.push("machines");
        ENABLE_MACHINE_GRAVITY = builder
                .comment("Require the configurable large machines to stand on a proper foundation. Independent from 528 mode, matching HBM 1.7.10 GeneralConfig.enableMachineGravity. Default: false.")
                .define("enableMachineGravity", false);
        SCALE_RTG_POWER = builder
                .comment("RTG 燃料输出是否随剩余寿命降低。HBM 1.7.10 默认值：false。")
                .define("scaleRtgPower", false);
        ENABLE_RTG_DECAY = builder
                .comment("RTG 燃料是否衰变为枯竭靶丸。HBM 1.7.10 默认值：true；528 模式强制启用。")
                .define("enableRtgDecay", true);
        AUTOCAL_MAX_CLOCK = builder
                .comment("AUTOCAL MS-ES1 maximum instructions per tick. HBM 1.7.10 default: 20.")
                .defineInRange("autocalMaxClock", 20, 1, 100);
        CONVEYOR_CRAM_MAX = builder
                .comment("传送带同一点允许堆积的移动物体数量。HBM 1.7.10 最新版默认：25。")
                .defineInRange("conveyorCramMax", 25, 1, Integer.MAX_VALUE);
        CONVEYOR_CRAM_EXPLODE = builder
                .comment("传送带物体拥堵超过上限且最老物体存在超过 400 tick 时，是否破坏所在传送带。HBM 1.7.10 最新版默认：true。")
                .define("conveyorCramExplode", true);
        ULTRA_LARP_MODE = builder
                .comment("启用后，大型工业门只能由红石控制；水密门仍可手动开关。HBM 1.7.10 最新版默认：false。")
                .define("ultraLarpMode", false);
        builder.pop();

        builder.push("industrialTurbine");
        INDUSTRIAL_TURBINE_INPUT_CAPACITY = builder
                .comment("工业汽轮机输入蒸汽容量。1.7.10 默认值：750000 mB。")
                .defineInRange("inputTankSize", 750_000, 1, Integer.MAX_VALUE);
        INDUSTRIAL_TURBINE_OUTPUT_CAPACITY = builder
                .comment("工业汽轮机输出乏蒸汽容量。1.7.10 默认值：3000000 mB。")
                .defineInRange("outputTankSize", 3_000_000, 1, Integer.MAX_VALUE);
        INDUSTRIAL_TURBINE_EFFICIENCY = builder
                .comment("工业汽轮机效率倍率。1.7.10 默认值：1.0。")
                .defineInRange("efficiency", 1.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("steamTurbine");
        STEAM_TURBINE_MAX_POWER = builder
                .comment("小型汽轮机的最大电力缓存。HBM 1.7.10 默认值：1000000 HE。")
                .defineInRange("maxPower", 1_000_000L, 1L, Long.MAX_VALUE);
        STEAM_TURBINE_INPUT_CAPACITY = builder
                .comment("小型汽轮机输入蒸汽容量。HBM 1.7.10 默认值：64000 mB。")
                .defineInRange("inputTankSize", 64_000, 1, Integer.MAX_VALUE);
        STEAM_TURBINE_OUTPUT_CAPACITY = builder
                .comment("小型汽轮机输出乏蒸汽容量。HBM 1.7.10 默认值：128000 mB。")
                .defineInRange("outputTankSize", 128_000, 1, Integer.MAX_VALUE);
        STEAM_TURBINE_MAX_STEAM_PER_TICK = builder
                .comment("小型汽轮机每 tick 最多处理的蒸汽量。HBM 1.7.10 默认值：6000 mB。")
                .defineInRange("maxSteamPerTick", 6_000, 1, Integer.MAX_VALUE);
        STEAM_TURBINE_EFFICIENCY = builder
                .comment("小型汽轮机效率倍率。HBM 1.7.10 默认值：0.85。")
                .defineInRange("efficiency", 0.85D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("steamEngine");
        STEAM_ENGINE_STEAM_CAPACITY = builder
                .comment("蒸汽机输入蒸汽容量。HBM 1.7.10 默认值：2000 mB。")
                .defineInRange("steamCap", 2_000, 1, Integer.MAX_VALUE);
        STEAM_ENGINE_SPENT_STEAM_CAPACITY = builder
                .comment("蒸汽机输出乏蒸汽容量。HBM 1.7.10 默认值：20 mB。")
                .defineInRange("spentSteamCap", 20, 1, Integer.MAX_VALUE);
        STEAM_ENGINE_EFFICIENCY = builder
                .comment("蒸汽机发电效率倍率。HBM 1.7.10 默认值：0.85。")
                .defineInRange("efficiency", 0.85D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("stirling");
        STIRLING_DIFFUSION = builder
                .comment("斯特林发电机每 tick 从热源提取的热量比例。HBM 1.7.10 默认值：0.1。")
                .defineInRange("diffusion", 0.1D, 0.0D, Double.MAX_VALUE);
        STIRLING_EFFICIENCY = builder
                .comment("斯特林发电机热能转电能效率。HBM 1.7.10 默认值：0.5。")
                .defineInRange("efficiency", 0.5D, 0.0D, Double.MAX_VALUE);
        STIRLING_MAX_HEAT_NORMAL = builder
                .comment("普通斯特林发电机的超速热量阈值。HBM 1.7.10 默认值：300 TU/t。")
                .defineInRange("maxHeatNormal", 300, 1, Integer.MAX_VALUE);
        STIRLING_MAX_HEAT_STEEL = builder
                .comment("重型斯特林发电机的超速热量阈值。HBM 1.7.10 默认值：1500 TU/t。")
                .defineInRange("maxHeatSteel", 1_500, 1, Integer.MAX_VALUE);
        STIRLING_OVERSPEED_LIMIT = builder
                .comment("斯特林发电机连续超速多少 tick 后抛出齿轮。HBM 1.7.10 默认值：300。")
                .defineInRange("overspeedLimit", 300, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("forcefield");
        FORCEFIELD_MAX_POWER = builder
                .comment("力场发生器的最大储能。1.7.10 默认值：1000000。")
                .defineInRange("maxPower", 1_000_000L, 0L, Long.MAX_VALUE);
        FORCEFIELD_BASE_CONSUMPTION = builder
                .comment("力场发生器的基础耗电量。1.7.10 默认值：1000 HE/t。")
                .defineInRange("baseConsumption", 1000, 0, Integer.MAX_VALUE);
        FORCEFIELD_RADIUS_CONSUMPTION = builder
                .comment("每个半径升级增加的耗电量。1.7.10 默认值：500 HE/t。")
                .defineInRange("radiusConsumption", 500, 0, Integer.MAX_VALUE);
        FORCEFIELD_SHIELD_CONSUMPTION = builder
                .comment("每个护盾升级增加的耗电量。1.7.10 默认值：250 HE/t。")
                .defineInRange("shieldConsumption", 250, 0, Integer.MAX_VALUE);
        FORCEFIELD_BASE_RADIUS = builder
                .comment("力场发生器的基础半径。1.7.10 默认值：16。")
                .defineInRange("baseRadius", 16, 0, Integer.MAX_VALUE);
        FORCEFIELD_RADIUS_UPGRADE = builder
                .comment("每个半径升级增加的半径。1.7.10 默认值：16。")
                .defineInRange("radiusUpgrade", 16, 0, Integer.MAX_VALUE);
        FORCEFIELD_SHIELD_UPGRADE = builder
                .comment("每个护盾升级增加的生命值。1.7.10 默认值：50。")
                .defineInRange("shieldUpgrade", 50, 0, Integer.MAX_VALUE);
        FORCEFIELD_COOLDOWN_MODIFIER = builder
                .comment("力场耗尽后的冷却时间倍率。1.7.10 默认值：1.0。")
                .defineInRange("cooldownModifier", 1.0D, 0.0D, Double.MAX_VALUE);
        FORCEFIELD_HEALTH_REGEN_MODIFIER = builder
                .comment("力场生命值恢复倍率。1.7.10 默认值：1.0。")
                .defineInRange("healthRegenModifier", 1.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("radar");
        RADAR_POWER_CAP = builder
                .comment("雷达最大储能。1.7.10 默认值：100000 HE。")
                .defineInRange("powerCap", 100_000L, 0L, Long.MAX_VALUE);
        RADAR_CONSUMPTION = builder
                .comment("雷达每 tick 扫描耗能。1.7.10 默认值：500 HE/t。")
                .defineInRange("consumption", 500L, 0L, Long.MAX_VALUE);
        RADAR_RANGE = builder
                .comment("雷达扫描半径。1.7.10 默认值：1000 格。")
                .defineInRange("radarRange", 1_000, 1, 30_000);
        RADAR_LARGE_RANGE = builder
                .comment("大型雷达扫描半径。1.7.10 默认值：3000 格。")
                .defineInRange("radarLargeRange", 3_000, 1, 30_000);
        RADAR_BUFFER = builder
                .comment("雷达忽略低于本体该高度范围内目标的缓冲距离。1.7.10 默认值：30 格。")
                .defineInRange("radarBuffer", 30, 0, 1_024);
        RADAR_ALTITUDE = builder
                .comment("雷达允许工作的最低高度。1.7.10 默认值：Y=55。")
                .defineInRange("radarAltitude", 55, -64, 2_048);
        RADAR_CHUNK_LOAD_CAP = builder
                .comment("雷达地形图每 tick 最多请求的未加载区块数。1.7.10 默认值：10。")
                .defineInRange("chunkLoadCap", 10, 0, 100);
        RADAR_GENERATE_CHUNKS = builder
                .comment("雷达地形图是否允许生成此前不存在的区块。1.7.10 默认值：false。")
                .define("generateChunks", false);
        builder.pop();

        builder.push("weapons");
        CIWS_ACCURACY = builder
                .comment("近防炮命中率修正值。默认值与 HBM 1.7.10 的 7.03_ciwsAccuracy 一致：50。")
                .defineInRange("ciwsAccuracy", 50, Integer.MIN_VALUE, Integer.MAX_VALUE);
        FATMAN_RADIUS = builder
                .comment("Fatman/轰炸机原子弹子弹的 MK5 基础半径。对应 HBM 1.7.10 BombConfig.fatmanRadius，默认：35。")
                .defineInRange("fatmanRadius", 35, 0, Integer.MAX_VALUE);
        MINE_AP_DAMAGE = builder
                .comment("Anti-personnel mine base entity damage. HBM 1.7.10 default: 10.")
                .defineInRange("mineApDamage", 10.0D, 0.0D, Double.MAX_VALUE);
        MINE_HE_DAMAGE = builder
                .comment("High explosive mine base entity damage. HBM 1.7.10 default: 35.")
                .defineInRange("mineHeDamage", 35.0D, 0.0D, Double.MAX_VALUE);
        MINE_SHRAP_DAMAGE = builder
                .comment("Shrapnel mine base entity damage. HBM 1.7.10 default: 7.5.")
                .defineInRange("mineShrapnelDamage", 7.5D, 0.0D, Double.MAX_VALUE);
        MINE_NUKE_DAMAGE = builder
                .comment("Nuclear mine base entity damage. HBM 1.7.10 default: 100.")
                .defineInRange("mineNuclearDamage", 100.0D, 0.0D, Double.MAX_VALUE);
        MINE_NAVAL_DAMAGE = builder
                .comment("Naval mine base entity damage. HBM 1.7.10 default: 60.")
                .defineInRange("mineNavalDamage", 60.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("energyConverters");
        builder.push("heToFe");
        HE_TO_FE_HE_USED = builder
                .comment("每次换算使用的 HE。对应 HBM 1.7.10 HEToRFConverter 的 L:HE_Used，默认：5。")
                .defineInRange("heUsed", 5L, 1L, Long.MAX_VALUE);
        HE_TO_FE_FE_CREATED = builder
                .comment("每次换算产生的 FE（旧版为 RF）。对应 HBM 1.7.10 的 L:RF_Created，默认：1。")
                .defineInRange("feCreated", 1L, 1L, Long.MAX_VALUE);
        HE_TO_FE_INPUT_DECAY = builder
                .comment("换算后剩余 HE 每 tick 的损耗比例。对应 HBM 1.7.10 的 D:inputDecay2，默认：0。")
                .defineInRange("inputDecay", 0.0D, 0.0D, 1.0D);
        builder.pop();
        builder.push("feToHe");
        FE_TO_HE_FE_USED = builder
                .comment("每次换算使用的 FE（旧版为 RF）。对应 HBM 1.7.10 RFToHEConverter 的 L:RF_Used2，默认：2。")
                .defineInRange("feUsed", 2L, 1L, Long.MAX_VALUE);
        FE_TO_HE_HE_CREATED = builder
                .comment("每次换算产生的 HE。对应 HBM 1.7.10 的 L:HE_Created2，默认：5。")
                .defineInRange("heCreated", 5L, 1L, Long.MAX_VALUE);
        FE_TO_HE_INPUT_DECAY = builder
                .comment("换算后剩余 FE 每 tick 的损耗比例。对应 HBM 1.7.10 的 D:inputDecay2，默认：0。")
                .defineInRange("inputDecay", 0.0D, 0.0D, 1.0D);
        builder.pop();
        HE_TO_FE_CONVERSION_RATE = builder
                .comment("原生 HBM 电力与 Forge Energy/RF 的直接换算倍率。1 HE 等于该数值 FE；高版本整合默认：4.0。")
                .defineInRange("conversionRateHeToRF", 4.0D, Double.MIN_VALUE, Double.MAX_VALUE);
        AUTO_CABLE_CONVERSION = builder
                .comment("HBM 电缆自动与相邻 Forge Energy 设备互转，无需转换器方块；同时控制 HBM 机器直接暴露 FE 能力。1.7.10 原版没有此功能，默认：false（仅通过 machine_converter_he_rf / machine_converter_rf_he 交换 FE）。")
                .define("autoCableConversion", false); // [parity N1: FE bridge off by default, as 1.7.10]
        builder.pop();

        builder.push("compatibility");
        ENABLE_CREATE_ADDITION_MOTOR_DEFAULTS = builder
                .comment(
                        "检测到 Create Crafts & Additions 时，是否由 HBM 应用推荐的电动马达平衡值。",
                        "启用后无需手动编辑 createaddition-common.toml；默认把满速马达消耗提高到 4096 FE/t。"
                )
                .define("enableCreateAdditionMotorDefaults", true);
        CREATE_ADDITION_FE_AT_MAX_RPM = builder
                .comment(
                        "CCA 电动马达/发电机在 256 RPM 时的 FE/t 换算值。HBM 高版本整合推荐默认：4096。",
                        "启用兼容后，CCA 马达最大输入会至少等于此值；内部容量下限为 16384 FE，耗电较高时则至少等于此值的两倍加 1。"
                )
                .defineInRange("createAdditionFeAtMaxRpm", 4_096, 1, (Integer.MAX_VALUE - 1) / 2);
        CREATE_ADDITION_MAX_STRESS = builder
                .comment("CCA 电动马达/发电机最大应力。-1 表示不覆盖 CCA 自身配置；HBM 当前推荐保持不改。")
                .defineInRange("createAdditionMaxStress", -1, -1, Integer.MAX_VALUE);
        ENABLE_IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_DEFAULTS = builder
                .comment(
                        "检测到 Immersive Engineering 时，是否由 HBM 应用推荐的柴油发电机与高压线缆平衡值。",
                        "启用后无需手动编辑 immersiveengineering-server.toml；默认让 IE 大型柴油发电机能够稳定驱动 8 台满速 CCA 电动马达。"
                )
                .define("enableImmersiveEngineeringDieselGeneratorDefaults", true);
        IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_OUTPUT = builder
                .comment("IE 柴油发电机每 tick 输出的 FE/IF。HBM 高版本整合推荐默认：32768。")
                .defineInRange("immersiveEngineeringDieselGeneratorOutput", 32_768, 1, Integer.MAX_VALUE);
        IMMERSIVE_ENGINEERING_HV_WIRE_TRANSFER_RATE = builder
                .comment("IE 高压线缆每 tick 最大传输 FE/IF。-1 表示不覆盖 IE 自身配置；默认提升到 131072，可承受单台 IE 柴油发电机 3 个 32768 FE/t 高压接线器合流后的瞬时负载。")
                .defineInRange("immersiveEngineeringHvWireTransferRate", 131_072, -1, Integer.MAX_VALUE);
        IMMERSIVE_ENGINEERING_HV_CONNECTOR_RATE = builder
                .comment("IE 高压接线器每 tick 最大输入/输出 FE/IF。-1 表示不覆盖 IE 自身配置；默认提升到 32768。")
                .defineInRange("immersiveEngineeringHvConnectorRate", 32_768, -1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("pollution");
        ENABLE_POLLUTION = builder
                .comment("是否启用 HBM 污染系统。默认与旧版一致：true。关闭后机器、流体和烟囱不会增加世界污染。")
                .define("enablePollution", true);
        ENABLE_ASBESTOS = builder
                .comment("是否启用旧版石棉粉尘/石棉危害。对应 1.7.10 HAZ_00_disableAsbestos=false。")
                .define("enableAsbestos", true);
        ENABLE_COAL_DUST = builder
                .comment("是否启用旧版煤尘/黑肺危害。对应 1.7.10 HAZ_01_disableCoaldust=false。")
                .define("enableCoalDust", true);
        ENABLE_LEAD_FROM_BLOCKS = builder
                .comment("是否启用旧版重金属污染区域破坏方块导致铅中毒的机制。默认与旧版一致：true。")
                .define("enableLeadFromBlocks", true);
        ENABLE_LEAD_POISONING = builder
                .comment("是否启用旧版重金属污染区域停留导致铅中毒的机制。默认与旧版一致：true。")
                .define("enableLeadPoisoning", true);
        ENABLE_SOOT_FOG = builder
                .comment("是否启用旧版烟尘雾效。默认与旧版一致：true。")
                .define("enableSootFog", true);
        ENABLE_POISON_EFFECT = builder
                .comment("是否启用毒性污染区域对实体施加中毒/凋零效果。默认与旧版一致：true。")
                .define("enablePoisonEffect", true);
        TAINT_TRAILS = builder
                .comment("Enable the low-age taint trails used by the 1.7.10 taint effect. Default: false.")
                .define("taintTrails", false);
        POLLUTION_MULTIPLIER = builder
                .comment("污染生成倍率。默认与旧版配置读取值一致：1.0。旧版开启 rampant 模式时会额外改为 3.0。")
                .defineInRange("pollutionMultiplier", 1.0D, 0.0D, 1_000.0D);
        POLLUTION_MOB_BUFF_THRESHOLD = builder
                .comment("烟尘污染超过该值时，新生成的敌对生物获得旧版烟尘强化。默认与旧版一致：15。")
                .defineInRange("pollutionMobBuffThreshold", 15.0D, 0.0D, 10_000.0D);
        POLLUTION_SOOT_FOG_THRESHOLD = builder
                .comment("烟尘污染超过该值后客户端显示烟雾雾效。默认与旧版一致：35。")
                .defineInRange("pollutionSootFogThreshold", 35.0D, 0.0D, 10_000.0D);
        POLLUTION_SOOT_FOG_DIVISOR = builder
                .comment("烟尘雾效强度除数，数值越大雾越弱。默认与 HBM 1.7.10 一致：120。")
                .defineInRange("pollutionSootFogDivisor", 120.0D, 1.0D, 10_000.0D);
        builder.pop();

        builder.push("glyphids");
        GLYPHID_ENABLE_HIVES = builder
                .comment("Allow small Glyphid hives to generate in the Overworld. HBM 1.7.10 default: true.")
                .define("enableHives", true);
        GLYPHID_HIVE_SPAWN = builder
                .comment("Average number of Overworld chunks per naturally generated Glyphid hive. HBM 1.7.10 default: 256.")
                .defineInRange("hiveSpawn", 256, 1, Integer.MAX_VALUE);
        LEGACY_DUNGEON_SPAWN_FLAG = builder
                .comment("HBM 1.7.10 1.03_enableDungeonSpawn: true forces structures on, false disables them, and flag respects the world Generate Structures setting.")
                .define("legacyDungeonSpawn", "flag");
        GLYPHID_SCOUT_SOOT_THRESHOLD = builder
                .comment("Minimum soot for Scout behavior to use polluted-world expansion. HBM 1.7.10 default: 1.")
                .defineInRange("scoutSootThreshold", 1.0D, 0.0D, 10_000.0D);
        GLYPHID_SCOUT_SWARM_CHANCE = builder
                .comment("Scout swarm chance denominator. HBM 1.7.10 default: 3.")
                .defineInRange("scoutSwarmChance", 3, 1, Integer.MAX_VALUE);
        GLYPHID_LARGE_HIVE_CHANCE = builder
                .comment("Large hive chance denominator. HBM 1.7.10 default: 5.")
                .defineInRange("largeHiveChance", 5, 1, Integer.MAX_VALUE);
        GLYPHID_TARGETING_THRESHOLD = builder
                .comment("Soot threshold for extended Glyphid targeting. HBM 1.7.10 default: 1.")
                .defineInRange("targetingThreshold", 1.0D, 0.0D, 10_000.0D);
        GLYPHID_NATURAL_SCOUT_SPAWN = builder
                .comment("Allow Scouts to spawn alongside natural mobs in polluted areas. HBM 1.7.10 default: false.")
                .define("rampantNaturalScoutSpawn", false);
        GLYPHID_NATURAL_SCOUT_THRESHOLD = builder
                .comment("Soot threshold for natural Scout spawning. HBM 1.7.10 default: 13.")
                .defineInRange("rampantScoutSpawnThreshold", 13.0D, 0.0D, 10_000.0D);
        GLYPHID_NATURAL_SCOUT_CHANCE = builder
                .comment("Natural Scout spawn chance denominator. HBM 1.7.10 default: 1400.")
                .defineInRange("rampantScoutSpawnChance", 1400, 1, Integer.MAX_VALUE);
        GLYPHID_RAMPANT_MODE = builder
                .comment("Enable the complete Rampant Glyphid behavior set. HBM 1.7.10 default: false.")
                .define("rampantMode", false);
        GLYPHID_RAMPANT_SMOKESTACK_OVERRIDE = builder
                .comment("Rampant-mode smokestack pollution multiplier. HBM 1.7.10 default: 0.4.")
                .defineInRange("rampantSmokeStackOverride", 0.4D, 0.0D, 10_000.0D);
        GLYPHID_RAMPANT_EXTENDED_TARGETING = builder
                .comment("Always give Glyphids extended targeting. HBM 1.7.10 default: false.")
                .define("rampantExtendedTargeting", false);
        GLYPHID_RAMPANT_DIG = builder
                .comment("Allow Glyphids to dig to waypoints. HBM 1.7.10 default: false.")
                .define("rampantDig", false);
        GLYPHID_RAMPANT_GUIDANCE = builder
                .comment("Make Scouts expand toward the player's respawn point. HBM 1.7.10 default: false.")
                .define("rampantGlyphidGuidance", false);
        GLYPHID_SCOUT_INITIAL_SPAWN = builder
                .comment("Allow a Scout to spawn in a hive's first swarm. HBM 1.7.10 default: false.")
                .define("scoutInitialSpawn", false);
        GLYPHID_WAYPOINT_DEBUG = builder
                .comment("Show Glyphid task waypoints for debugging. HBM 1.7.10 default: false.")
                .define("waypointDebug", false);
        GLYPHID_SWARM_COOLDOWN_SECONDS = builder
                .comment("Seconds between Glyphid-spawner swarm checks. HBM 1.7.10 default: 120.")
                .defineInRange("swarmCooldown", 120, 1, Integer.MAX_VALUE);
        GLYPHID_BASE_SWARM_SIZE = builder
                .comment("Glyphid-spawner base swarm size. HBM 1.7.10 default: 5.")
                .defineInRange("baseSwarmSize", 5, 0, Integer.MAX_VALUE);
        GLYPHID_SWARM_SCALING_MULTIPLIER = builder
                .comment("Glyphid-spawner soot scaling multiplier. HBM 1.7.10 default: 1.2.")
                .defineInRange("swarmScalingMult", 1.2D, 0.0D, Double.MAX_VALUE);
        GLYPHID_SOOT_STEP = builder
                .comment("Soot interval used by Glyphid-spawner swarm scaling. HBM 1.7.10 default: 50.")
                .defineInRange("sootStep", 50, 1, Integer.MAX_VALUE);
        GLYPHID_SPAWN_MAX = builder
                .comment("Maximum loaded Glyphids before a Glyphid spawner stops. HBM 1.7.10 default: 50.")
                .defineInRange("spawnMax", 50.0D, 0.0D, Double.MAX_VALUE);
        GLYPHID_GRUNT_CHANCE = glyphidChance(builder, "glyphidChance", List.of(50, -45, 0));
        GLYPHID_BRAWLER_CHANCE = glyphidChance(builder, "brawlerChance", List.of(10, 30, 1));
        GLYPHID_BOMBARDIER_CHANCE = glyphidChance(builder, "bombardierChance", List.of(20, -15, 1));
        GLYPHID_BLASTER_CHANCE = glyphidChance(builder, "blasterChance", List.of(-5, 40, 5));
        GLYPHID_DIGGER_CHANCE = glyphidChance(builder, "diggerChance", List.of(-15, 25, 5));
        GLYPHID_BEHEMOTH_CHANCE = glyphidChance(builder, "behemothChance", List.of(-30, 45, 10));
        GLYPHID_BRENDA_CHANCE = glyphidChance(builder, "brendaChance", List.of(-50, 60, 20));
        GLYPHID_NUCLEAR_CHANCE = glyphidChance(builder, "johnsonChance", List.of(-50, 60, 50));
        builder.pop();

        builder.push("legacyMobSpawning");
        ENABLE_DUCKS = builder
                .comment("Whether pressing O may spawn the one-time duck. HBM 1.7.10 default: true.")
                .define("enableDucks", true);
        ENABLE_MOB_GEAR = builder
                .comment("Whether naturally spawned legacy zombies and skeletons receive their 1.7.10 equipment pools. Default: true.")
                .define("enableMobGear", true);
        // [parity E1: MobConfig 12.D02_enableMobWeapons / 12.D03_mobWeaponSootReduction]
        ENABLE_MOB_WEAPONS = builder
                .comment("Whether skeletons should have bows replaced with guns when spawning at higher soot levels. HBM 1.7.10 default: true.")
                .define("enableMobWeapons", true);
        MOB_WEAPON_SOOT_REDUCTION = builder
                .comment("Reduces the amount of soot needed for skeleton guns to appear. HBM 1.7.10 default: 0.")
                .defineInRange("mobWeaponSootReduction", 0.0D, -Double.MAX_VALUE, Double.MAX_VALUE);
        ENABLE_STAT_REREGISTERING = builder
                .comment("Keep the old mask-man crafted/placed-crystallizer statistic gate. HBM 1.7.10 default: true.")
                .define("enableStatReRegistering", true);
        ENABLE_MASK_MAN = builder
                .comment("Whether Mask Man should spawn. HBM 1.7.10 default: true.")
                .define("enableMaskman", true);
        // [parity P3: MobConfig 12.M01_maskmanTimer, world seconds counted every 20 ticks; no chance roll in the selected commit]
        MASK_MAN_DELAY = builder
                .comment("How many world seconds need to pass for mask man to spawn, if the requirements are met. HBM 1.7.10 default: 1200.")
                .defineInRange("maskmanTimer", 20 * 60, 1, Integer.MAX_VALUE);
        MASK_MAN_MIN_RAD = builder
                .comment("Minimum radiation for Mask Man. HBM 1.7.10 default: 50.")
                .defineInRange("maskmanMinRad", 50, 0, Integer.MAX_VALUE);
        MASK_MAN_UNDERGROUND = builder
                .comment("Whether the Mask Man target must be more than three blocks below the surface. HBM 1.7.10 default: true.")
                .define("maskmanUnderground", true);
        ENABLE_FBI_RAIDS = builder
                .comment("Whether FBI raids should spawn. HBM 1.7.10 default: false.")
                .define("enableFBIRaids", false);
        FBI_RAID_DELAY = builder
                .comment("World ticks between FBI raid checks. HBM 1.7.10 default: 108000.")
                .defineInRange("raidDelay", 108_000, 1, Integer.MAX_VALUE);
        FBI_RAID_CHANCE = builder
                .comment("FBI raid chance denominator. HBM 1.7.10 default: 3.")
                .defineInRange("raidChance", 3, 1, Integer.MAX_VALUE);
        FBI_RAID_AMOUNT = builder
                .comment("FBI agents per raid. HBM 1.7.10 default: 15.")
                .defineInRange("raidAmount", 15, 0, Integer.MAX_VALUE);
        FBI_RAID_DRONES = builder
                .comment("FBI drones per raid. HBM 1.7.10 default: 5.")
                .defineInRange("raidDrones", 5, 0, Integer.MAX_VALUE);
        FBI_RAID_ATTACK_DISTANCE = builder
                .comment("FBI raid spawn radius. HBM 1.7.10 default: 32; old elemental spawning also uses this value.")
                .defineInRange("raidAttackDistance", 32, 0, Integer.MAX_VALUE);
        FBI_RAID_ATTACK_DELAY = builder
                .comment("FBI machine-break attempt interval. HBM 1.7.10 MobConfig.raidAttackDelay default: 40.")
                .defineInRange("raidAttackDelay", 40, 1, Integer.MAX_VALUE);
        FBI_RAID_ATTACK_REACH = builder
                .comment("FBI machine-break ray length. HBM 1.7.10 MobConfig.raidAttackReach default: 2.")
                .defineInRange("raidAttackReach", 2, 1, Integer.MAX_VALUE);
        ENABLE_MELTDOWN_ELEMENTALS = builder
                .comment("Whether reactor meltdowns can mark players for radiation beasts. HBM 1.7.10 default: true.")
                .define("enableMeltdownElementals", true);
        ELEMENTAL_DELAY = builder
                .comment("World ticks between radiation-beast checks. HBM 1.7.10 default: 108000.")
                .defineInRange("elementalDelay", 108_000, 1, Integer.MAX_VALUE);
        ELEMENTAL_CHANCE = builder
                .comment("Radiation-beast chance denominator. HBM 1.7.10 default: 2.")
                .defineInRange("elementalChance", 2, 1, Integer.MAX_VALUE);
        ELEMENTAL_AMOUNT = builder
                .comment("Radiation beasts per marked-player event. HBM 1.7.10 default: 10.")
                .defineInRange("elementalAmount", 10, 0, Integer.MAX_VALUE);
        ELEMENTAL_DISTANCE = builder
                .comment("Retained legacy elemental-distance setting. BossSpawnHandler uses raidAttackDistance in 1.7.10.")
                .defineInRange("elementalAttackDistance", 32, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("rbmk");
        RBMK_COLUMN_HEIGHT = builder
                .comment("RBMK 柱体总高度。默认与 HBM 1.7.10 的 dialColumnHeight 一致：4。允许范围与旧版一致：2 到 16。")
                .defineInRange("rbmkColumnHeight", 4, 2, 16);
        RBMK_PASSIVE_COOLING = builder
                .comment("RBMK 边缘被动冷却。默认与 HBM 1.7.10 的 dialPassiveCooling 一致：2.5。")
                .defineInRange("rbmkPassiveCooling", 2.5D, 0.0D, 1_000.0D);
        RBMK_PASSIVE_COOLING_INNER = builder
                .comment("RBMK 内部被动冷却。默认与 HBM 1.7.10 的 dialPassiveCoolingInner 一致：0.1。")
                .defineInRange("rbmkPassiveCoolingInner", 0.1D, 0.0D, 1_000.0D);
        RBMK_COLUMN_HEAT_FLOW = builder
                .comment("RBMK 相邻柱体热量均衡速度。默认与 HBM 1.7.10 的 dialColumnHeatFlow 一致：0.2。")
                .defineInRange("rbmkColumnHeatFlow", 0.2D, 0.0D, 1.0D);
        RBMK_FUEL_DIFFUSION_MOD = builder
                .comment("RBMK 燃料芯到包壳的热扩散倍率。默认与 HBM 1.7.10 的 dialDiffusionMod 一致：1.0。")
                .defineInRange("rbmkFuelDiffusionMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_HEAT_PROVISION = builder
                .comment("RBMK 燃料棒向柱体供热倍率。默认与 HBM 1.7.10 的 dialHeatProvision 一致：0.2。")
                .defineInRange("rbmkHeatProvision", 0.2D, 0.0D, 1.0D);
        RBMK_BOILER_HEAT_CONSUMPTION = builder
                .comment("RBMK 锅炉每 mB 水消耗的热量。默认与 HBM 1.7.10 的 dialBoilerHeatConsumption 一致：0.1。")
                .defineInRange("rbmkBoilerHeatConsumption", 0.1D, 0.0D, 1_000.0D);
        RBMK_CONTROL_SPEED = builder
                .comment("RBMK 控制棒移动速度倍率。默认与 HBM 1.7.10 的 dialControlSpeed 一致：1.0。")
                .defineInRange("rbmkControlSpeed", 1.0D, 0.0D, 1_000.0D);
        RBMK_REACTIVITY_MOD = builder
                .comment("RBMK 燃料反应性倍率。默认与 HBM 1.7.10 的 dialReactivityMod 一致：1.0。")
                .defineInRange("rbmkReactivityMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_SURGE_MOD = builder
                .comment("RBMK 手动控制棒快速抽出时的瞬态反应性倍率。默认与 HBM 1.7.10 的 dialSurgeMod 一致：1.0。")
                .defineInRange("rbmkSurgeMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_OUTGASSER_SPEED_MOD = builder
                .comment("RBMK 脱气器速度倍率。默认与 HBM 1.7.10 的 dialOutgasserSpeedMod 一致：1.0。")
                .defineInRange("rbmkOutgasserSpeedMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_FLUX_RANGE = builder
                .comment("RBMK 中子通量传播范围。默认与 HBM 1.7.10 的 dialFluxRange 一致：5。")
                .defineInRange("rbmkFluxRange", 5, 1, 100);
        RBMK_MODERATOR_EFFICIENCY = builder
                .comment("RBMK 慢化效率。默认与 HBM 1.7.10 的 dialModeratorEfficiency 一致：1.0。")
                .defineInRange("rbmkModeratorEfficiency", 1.0D, 0.0D, 1.0D);
        RBMK_ABSORBER_EFFICIENCY = builder
                .comment("RBMK 吸收柱吸收效率。默认与 HBM 1.7.10 的 dialAbsorberEfficiency 一致：1.0。")
                .defineInRange("rbmkAbsorberEfficiency", 1.0D, 0.0D, 1.0D);
        RBMK_ABSORBER_HEAT_CONVERSION = builder
                .comment("RBMK 吸收柱每单位通量转换的热量。默认与 HBM 1.7.10 的 dialAbsorberHeatConversion 一致：0.05。")
                .defineInRange("rbmkAbsorberHeatConversion", 0.05D, 0.0D, 1_000.0D);
        RBMK_REFLECTOR_EFFICIENCY = builder
                .comment("RBMK 反射器效率。默认与 HBM 1.7.10 的 dialReflectorEfficiency 一致：1.0。")
                .defineInRange("rbmkReflectorEfficiency", 1.0D, 0.0D, 1.0D);
        RBMK_REASIM_BOILERS = builder
                .comment("是否启用 RBMK ReaSim 锅炉机制。默认与 HBM 1.7.10 的 dialReasimBoilers 一致：false。启用后所有 RBMK 柱体都具有内部水/蒸汽缓存，并通过进水口与蒸汽出口交互。")
                .define("rbmkReasimBoilers", false);
        RBMK_REASIM_BOILER_SPEED = builder
                .comment("RBMK ReaSim 锅炉速度。默认与 HBM 1.7.10 的 dialReasimBoilerSpeed 一致：0.05，表示每 tick 处理理论最大量的 5%。")
                .defineInRange("rbmkReasimBoilerSpeed", 0.05D, 0.0D, 1.0D);
        RBMK_DISABLE_MELTDOWNS = builder
                .comment("是否禁用 RBMK 融毁机制。默认与 HBM 1.7.10 一致：false。设为 true 后 RBMK 超温不会触发整堆融毁。")
                .define("rbmkDisableMeltdowns", false);
        RBMK_OVERPRESSURE = builder
                .comment("RBMK 融毁时是否触发相连流体管网过压破坏。默认与 HBM 1.7.10 的 dialEnableMeltdownOverpressure 一致：false。")
                .define("rbmkOverpressure", false);
        RBMK_ENABLE_DEPLETION = builder
                .comment("是否启用 RBMK 燃料消耗。默认与 HBM 1.7.10 的 dialDisableDepletion=false 一致：true。")
                .define("rbmkEnableDepletion", true);
        RBMK_ENABLE_XENON = builder
                .comment("是否启用 RBMK 氙毒机制。默认与 HBM 1.7.10 的 dialDisableXenon=false 一致：true。")
                .define("rbmkEnableXenon", true);
        RBMK_FALLOUT_RANGE = builder
                .comment("RBMK 融毁触发核爆焦土/辐射尘地形效果的半径。默认与 HBM 1.7.10 核爆 falloutRange 基准一致：100。")
                .defineInRange("rbmkFalloutRange", 100, 0, 1024);
        RBMK_FALLOUT_DELAY = builder
                .comment("RBMK 融毁焦土效果每批区块处理之间的 tick 间隔。默认与 HBM 1.7.10 falloutDelay 一致：4。")
                .defineInRange("rbmkFalloutDelay", 4, 0, 200);
        RBMK_PERMANENT_SCRAP = builder
                .comment("RBMK 融毁残骸是否永久保留。默认与 HBM 1.7.10 的 dialEnablePermaScrap 一致：true。")
                .define("rbmkPermanentScrap", true);
        builder.pop();

        builder.push("worldgen");
        ENABLE_NETHER_ORES = builder
                .comment("是否生成 HBM 下界矿石。关闭后不会生成常规下界矿、深层钕矿或下界基岩矿；熔燃矿与下界间歇泉保持生成，与 HBM 1.7.10 的 netherOres 开关一致。默认：true。")
                .define("enableNetherOres", true);
        ENABLE_NETHER_PLUTONIUM_ORE = builder
                .comment("是否生成下界钚矿。对应 HBM 1.7.10 GeneralConfig.enablePlutoniumOre，默认：false。")
                .define("enableNetherPlutoniumOre", false);
        ENABLE_RADIATION_HOTSPOTS = builder
                .comment("是否生成 Sellafield 辐射热点地形。对应 HBM 1.7.10 GeneralConfig.enableRad，默认：true。")
                .define("enableRadiationHotspots", true);
        RADIATION_HOTSPOT_SPAWN_RATE = builder
                .comment("Sellafield 辐射热点生成间隔，数值越大越稀有。HBM 1.7.10 WorldConfig.radfreq 默认：5000；设置为 0 可禁用。")
                .defineInRange("radiationHotspotSpawnRate", 5000, 0, 1_000_000);
        GAS_BUBBLE_SPAWN_RATE = builder
                .comment("易燃气泡矿脉生成间隔。HBM 1.7.10 WorldConfig.gasbubbleSpawn 默认：12；设置为 0 可禁用。")
                .defineInRange("gasBubbleSpawnRate", 12, 0, 1_000_000);
        EXPLOSIVE_GAS_BUBBLE_SPAWN_RATE = builder
                .comment("爆炸气泡矿脉生成间隔。HBM 1.7.10 WorldConfig.explosivebubbleSpawn 默认：0，默认禁用。")
                .defineInRange("explosiveGasBubbleSpawnRate", 0, 0, 1_000_000);
        ENABLE_SULFUR_CAVES = builder
                .comment("是否生成硫磺洞穴。对应 HBM 1.7.10 WorldConfig.enableSulfurCave，默认：true。")
                .define("enableSulfurCaves", true);
        ENABLE_ASBESTOS_CAVES = builder
                .comment("是否生成石棉洞穴。对应 HBM 1.7.10 WorldConfig.enableAsbestosCave，默认：true。")
                .define("enableAsbestosCaves", true);

        builder.push("resourceLayers");
        ENABLE_HEMATITE_DEPOSITS = builder
                .comment("是否生成赤铁矿 3D 矿层。对应 HBM 1.7.10 WorldConfig.enableHematite，默认：true。")
                .define("enableHematite", true);
        ENABLE_MALACHITE_DEPOSITS = builder
                .comment("是否生成孔雀石 3D 矿层。对应 HBM 1.7.10 WorldConfig.enableMalachite，默认：true。")
                .define("enableMalachite", true);
        ENABLE_BAUXITE_DEPOSITS = builder
                .comment("是否生成铝土矿 3D 矿层。对应 HBM 1.7.10 WorldConfig.enableBauxite，默认：true。")
                .define("enableBauxite", true);
        builder.pop();

        builder.push("oil");

        GENERATE_OIL_DEPOSITS = builder
                .comment("是否生成油田。默认与 HBM 1.7.10 一致：true。")
                .define("generateOilDeposits", true);
        OIL_DEPOSIT_SPAWN_RATE = builder
                .comment("油田生成间隔，数值越大越稀有。HBM 1.7.10 WorldConfig.oilSpawn 默认：100。")
                .defineInRange("oilDepositSpawnRate", 100, 1, 1_000_000);
        OIL_DEPOSIT_DRY_BIOME_DIVISOR = builder
                .comment("炎热干燥群系中的油田生成间隔除数。HBM 1.7.10 MapGenBubble 固定：3。")
                .defineInRange("oilDepositHotDryBiomeDivisor", 3, 1, 1_000);
        OIL_DEPOSIT_MIN_RADIUS = builder
                .comment("油田最小半径。HBM 1.7.10 默认：8。")
                .defineInRange("oilDepositMinRadius", 8, 1, 128);
        OIL_DEPOSIT_MAX_RADIUS = builder
                .comment("油田最大半径。HBM 1.7.10 默认：16。")
                .defineInRange("oilDepositMaxRadius", 16, 1, 128);

        GENERATE_OIL_SAND_DEPOSITS = builder
                .comment("是否生成油砂矿床。HBM 1.7.10 硬编码启用。")
                .define("generateOilSandDeposits", true);
        OIL_SAND_DEPOSIT_SPAWN_RATE = builder
                .comment("油砂矿床生成间隔，数值越大越稀有。HBM 1.7.10 硬编码默认：200。")
                .defineInRange("oilSandDepositSpawnRate", 200, 1, 1_000_000);
        OIL_SAND_DEPOSIT_MIN_RADIUS = builder
                .comment("油砂矿床最小半径。HBM 1.7.10 默认：16。")
                .defineInRange("oilSandDepositMinRadius", 16, 1, 128);
        OIL_SAND_DEPOSIT_MAX_RADIUS = builder
                .comment("油砂矿床最大半径。HBM 1.7.10 默认：48。")
                .defineInRange("oilSandDepositMaxRadius", 48, 1, 128);

        GENERATE_BEDROCK_OIL_DEPOSITS = builder
                .comment("是否生成基岩油田。HBM 1.7.10 WorldConfig.bedrockOilSpawn 默认开启。")
                .define("generateBedrockOilDeposits", true);
        BEDROCK_OIL_DEPOSIT_SPAWN_RATE = builder
                .comment("基岩油田生成间隔，数值越大越稀有。HBM 1.7.10 默认：200。")
                .defineInRange("bedrockOilDepositSpawnRate", 200, 1, 1_000_000);
        BEDROCK_OIL_DXZ_LIMIT = builder
                .comment("基岩油田水平偏移限制。HBM 1.7.10 MapGenBedrockOil range 固定：4。")
                .defineInRange("bedrockOilHorizontalLimit", 4, 1, 64);
        BEDROCK_OIL_MAX_Y_OFFSET = builder
                .comment("基岩油田最大垂直偏移。HBM 1.7.10 生成 y=0..4。")
                .defineInRange("bedrockOilMaxYOffset", 4, 0, 64);
        BEDROCK_OIL_L1_MAX = builder
                .comment("基岩油田 L1 半径。HBM 1.7.10 固定：6。")
                .defineInRange("bedrockOilL1Radius", 6, 1, 128);
        BEDROCK_OIL_SURFACE_RADIUS = builder
                .comment("基岩油田地表油泥半径。HBM 1.7.10 spotWidth 默认：5。")
                .defineInRange("bedrockOilSurfaceRadius", 5, 1, 128);
        BEDROCK_OIL_SURFACE_ATTEMPTS = builder
                .comment("基岩油田地表油泥放置尝试次数。HBM 1.7.10 spotCount 默认：50。")
                .defineInRange("bedrockOilSurfaceAttempts", 50, 0, 10_000);
        BEDROCK_OIL_POROUS_VEIN_COUNT = builder
                .comment("兼容旧配置保留：1.7.10 基岩油没有多孔岩伴生，本字段不会被世界生成使用。")
                .defineInRange("bedrockOilPorousStoneVeinCount", 16, 0, 10_000);
        BEDROCK_OIL_POROUS_VEIN_SIZE = builder
                .comment("兼容旧配置保留：1.7.10 基岩油没有多孔岩伴生，本字段不会被世界生成使用。")
                .defineInRange("bedrockOilPorousStoneVeinSize", 8, 1, 128);
        BEDROCK_OIL_POROUS_MIN_Y = builder
                .comment("兼容旧配置保留：1.7.10 基岩油没有多孔岩伴生，本字段不会被世界生成使用。")
                .defineInRange("bedrockOilPorousStoneMinY", 10, -256, 512);
        BEDROCK_OIL_POROUS_Y_VARIANCE = builder
                .comment("兼容旧配置保留：1.7.10 基岩油没有多孔岩伴生，本字段不会被世界生成使用。")
                .defineInRange("bedrockOilPorousStoneYVariance", 50, 0, 512);
        GENERATE_BEDROCK_ORES = builder
                .comment("是否生成通用基岩矿。HBM 1.7.10 默认：true。")
                .define("generateBedrockOres", true);
        BEDROCK_ORE_SPAWN_RATE = builder
                .comment("通用基岩矿生成间隔，数值越大越稀有。HBM 1.7.10 默认：10。")
                .defineInRange("bedrockOreSpawnRate", 10, 1, 1_000_000);
        BEDROCK_ORE_NETHER_GLOWSTONE_WEIGHT = builder
                .comment("下界荧石通用基岩矿的生成权重。HBM 1.7.10 默认：100。")
                .defineInRange("bedrockOreNetherGlowstoneWeight", 100, 0, 1_000_000);
        BEDROCK_ORE_NETHER_PHOSPHORUS_WEIGHT = builder
                .comment("下界磷通用基岩矿的生成权重。HBM 1.7.10 默认：50。")
                .defineInRange("bedrockOreNetherPhosphorusWeight", 50, 0, 1_000_000);
        BEDROCK_ORE_NETHER_QUARTZ_WEIGHT = builder
                .comment("下界石英通用基岩矿的生成权重。HBM 1.7.10 默认：100。")
                .defineInRange("bedrockOreNetherQuartzWeight", 100, 0, 1_000_000);
        METEORITE_SPAWN = builder
                .comment("Fallen meteorite worldgen interval in chunks. Matches HBM 1.7.10 default: 200.")
                .defineInRange("meteoriteSpawnRate", 200, 1, 1_000_000);

        builder.pop();

        builder.push("geysers");
        CHLORINE_GEYSER_SPAWN_RATE = builder
                .comment("氯气间歇泉生成间隔，数值表示平均每多少个平原区块尝试生成一次；设置为 0 可禁用。默认与 HBM 1.7.10 一致：3000。")
                .defineInRange("chlorineGeyserSpawnRate", 3000, 0, 1_000_000);
        builder.pop();

        builder.push("meteors");
        ENABLE_METEOR_STRIKES = builder
                .comment("Enable falling meteor strikes. Matches HBM 1.7.10 default: true.")
                .define("enableMeteorStrikes", true);
        ENABLE_METEOR_SHOWERS = builder
                .comment("Enable meteor showers. Matches HBM 1.7.10 default: true.")
                .define("enableMeteorShowers", true);
        ENABLE_METEOR_TAILS = builder
                .comment("Enable falling meteor tail particles and impact trail effects. Matches HBM 1.7.10 default: true.")
                .define("enableMeteorTails", true);
        ENABLE_SPECIAL_METEORS = builder
                .comment("Enable rare special meteorite variants. Matches HBM 1.7.10 default: true.")
                .define("enableSpecialMeteors", true);
        METEOR_STRIKE_CHANCE = builder
                .comment("Average falling meteor spawn interval in ticks. Matches HBM 1.7.10 default: 360000.")
                .defineInRange("meteorStrikeChance", 20 * 60 * 60 * 5, 1, Integer.MAX_VALUE / 100);
        METEOR_SHOWER_CHANCE = builder
                .comment("Average falling meteor spawn interval during a meteor shower, in ticks. Matches HBM 1.7.10 default: 18000.")
                .defineInRange("meteorShowerChance", 20 * 60 * 15, 1, Integer.MAX_VALUE);
        METEOR_SHOWER_DURATION = builder
                .comment("Maximum meteor shower duration in ticks. Matches HBM 1.7.10 default: 36000.")
                .defineInRange("meteorShowerDuration", 20 * 60 * 30, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("structures");
        GENERATE_HBM_STRUCTURES = builder
                .comment("是否生成 HBM 旧版结构。")
                .define("generateHbmStructures", true);
        HBM_STRUCTURE_MIN_CHUNKS = builder
                .comment("HBM 结构最小区块间距。HBM 1.12.2 默认：4。")
                .defineInRange("hbmStructureMinChunks", 4, 0, 1024);
        HBM_STRUCTURE_MAX_CHUNKS = builder
                .comment("HBM 结构最大区块间距。HBM 1.7.10 默认：16（StructureConfig 5.02_structureMaxChunks）。")
                .defineInRange("hbmStructureMaxChunks", 16, 1, 1024); // [parity R3: structures] was the 1.12.2 value 12
        HBM_STRUCTURE_ENABLE_RUINS = builder
                .comment("是否生成 NTM 废墟 A-J。HBM 1.12.2 默认：true。")
                .define("hbmStructureEnableRuins", true);
        HBM_STRUCTURE_ENABLE_OCEAN = builder
                .comment("是否生成海洋结构。HBM 1.12.2 默认：true。")
                .define("hbmStructureEnableOcean", true);

        HBM_STRUCTURE_SPIRE_WEIGHT = weight(builder, "spireSpawnWeight", "尖塔结构生成权重。", 2);
        HBM_STRUCTURE_VERTIBIRD_WEIGHT = weight(builder, "vertibirdSpawnWeight", "飞鸟直升机结构生成权重。", 6);
        HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT = weight(builder, "crashedVertibirdSpawnWeight", "坠毁飞鸟直升机结构生成权重。", 10);
        HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT = weight(builder, "aircraftCarrierSpawnWeight", "航空母舰结构生成权重。", 3);
        HBM_STRUCTURE_OIL_RIG_WEIGHT = weight(builder, "oilRigSpawnWeight", "钻油平台结构生成权重。", 5);
        HBM_STRUCTURE_LIGHTHOUSE_WEIGHT = weight(builder, "lighthouseSpawnWeight", "灯塔结构生成权重。", 1);
        HBM_STRUCTURE_BEACHED_PATROL_WEIGHT = weight(builder, "beachedPatrolSpawnWeight", "搁浅巡逻艇结构生成权重。", 15);
        HBM_STRUCTURE_DISH_WEIGHT = weight(builder, "dishSpawnWeight", "碟形天线结构生成权重。", 10);
        HBM_STRUCTURE_WATER_PUMP_WEIGHT = weight(builder, "waterPumpSpawnWeight", "水泵站结构生成权重。HBM 1.7.10 最新版默认：15。", 15);
        HBM_STRUCTURE_FOREST_CHEM_WEIGHT = weight(builder, "forestChemSpawnWeight", "森林化工结构生成权重。", 30);
        HBM_STRUCTURE_PLANE1_WEIGHT = weight(builder, "plane1SpawnWeight", "坠毁飞机 1 结构生成权重。", 25);
        HBM_STRUCTURE_PLANE2_WEIGHT = weight(builder, "plane2SpawnWeight", "坠毁飞机 2 结构生成权重。", 25);
        HBM_STRUCTURE_DESERT_SHACK1_WEIGHT = weight(builder, "desertShack1SpawnWeight", "沙漠棚屋 1 结构生成权重。", 18);
        HBM_STRUCTURE_DESERT_SHACK2_WEIGHT = weight(builder, "desertShack2SpawnWeight", "沙漠棚屋 2 结构生成权重。", 20);
        HBM_STRUCTURE_DESERT_SHACK3_WEIGHT = weight(builder, "desertShack3SpawnWeight", "沙漠棚屋 3 结构生成权重。", 22);
        HBM_STRUCTURE_DEAD_DISH_SMALL_WEIGHT = weight(builder, "deadDishSmallSpawnWeight", "小型废弃雷达天线结构生成权重。HBM 1.7.10 最新版默认：15。", 15);
        HBM_STRUCTURE_LABORATORY_WEIGHT = weight(builder, "laboratorySpawnWeight", "实验室结构生成权重。", 20);
        HBM_STRUCTURE_FOREST_POST_WEIGHT = weight(builder, "forestPostSpawnWeight", "森林哨站结构生成权重。", 30);
        HBM_STRUCTURE_FACTORY_WEIGHT = weight(builder, "factorySpawnWeight", "工厂结构生成权重。", 40);
        HBM_STRUCTURE_CRANE_WEIGHT = weight(builder, "craneSpawnWeight", "起重机结构生成权重。", 20);
        HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT = weight(builder, "broadcastingTowerSpawnWeight", "广播塔结构生成权重。", 25);
        HBM_STRUCTURE_RADIO_WEIGHT = weight(builder, "radioSpawnWeight", "无线电结构生成权重。", 25);
        HBM_STRUCTURE_FEATURES_WEIGHT = weight(builder, "featuresSpawnWeight", "地表特征结构生成权重。", 50);
        HBM_STRUCTURE_BUNKER_WEIGHT = weight(builder, "bunkerSpawnWeight", "地堡结构生成权重。", 6);
        HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT = weight(builder, "meteorDungeonSpawnWeight", "陨石地牢结构生成权重。", 1);
        HBM_STRUCTURE_RUIN_A_WEIGHT = weight(builder, "ruinASpawnWeight", "废墟 A 生成权重。", 10);
        HBM_STRUCTURE_RUIN_B_WEIGHT = weight(builder, "ruinBSpawnWeight", "废墟 B 生成权重。", 12);
        HBM_STRUCTURE_RUIN_C_WEIGHT = weight(builder, "ruinCSpawnWeight", "废墟 C 生成权重。", 12);
        HBM_STRUCTURE_RUIN_D_WEIGHT = weight(builder, "ruinDSpawnWeight", "废墟 D 生成权重。", 12);
        HBM_STRUCTURE_RUIN_E_WEIGHT = weight(builder, "ruinESpawnWeight", "废墟 E 生成权重。", 12);
        HBM_STRUCTURE_RUIN_F_WEIGHT = weight(builder, "ruinFSpawnWeight", "废墟 F 生成权重。", 12);
        HBM_STRUCTURE_RUIN_G_WEIGHT = weight(builder, "ruinGSpawnWeight", "废墟 G 生成权重。", 12);
        HBM_STRUCTURE_RUIN_H_WEIGHT = weight(builder, "ruinHSpawnWeight", "废墟 H 生成权重。", 12);
        HBM_STRUCTURE_RUIN_I_WEIGHT = weight(builder, "ruinISpawnWeight", "废墟 I 生成权重。", 12);
        HBM_STRUCTURE_RUIN_J_WEIGHT = weight(builder, "ruinJSpawnWeight", "废墟 J 生成权重。", 12);
        HBM_STRUCTURE_TOWER_BASE_WEIGHT = weight(builder, "towerBaseSpawnWeight", "塔基结构生成权重。HBM 1.7.10 最新版默认：30。", 30);
        HBM_STRUCTURE_FALLOUT_SHELTER_HOUSE_WEIGHT = weight(builder, "falloutShelterHouseSpawnWeight", "辐射避难所住宅结构生成权重。HBM 1.7.10 最新版默认：15。", 15);
        HBM_STRUCTURE_ABANDONED_ARCTIC_STATION_WEIGHT = weight(builder, "abandonedArcticStationSpawnWeight", "废弃北极站结构生成权重。HBM 1.7.10 最新版默认：5。", 5);
        HBM_STRUCTURE_MILITARY_BASE_WEIGHT = weight(builder, "militaryBaseSpawnWeight", "军事基地结构生成权重。HBM 1.7.10 最新版默认：1。", 1);
        HBM_STRUCTURE_SUBMARINE_WEIGHT = weight(builder, "submarineSpawnWeight", "Spawn weight for submarine. HBM 1.7.10 default: 2.", 2); // [parity R3: submarine]
        HBM_STRUCTURE_PLAINS_NULL_WEIGHT = weight(builder, "plainsNullWeight", "平原结构空结果权重。", 20);
        HBM_STRUCTURE_OCEAN_NULL_WEIGHT = weight(builder, "oceanNullWeight", "海洋结构空结果权重。", 35);
        ANCIENT_TOMB_SPAWN_CHANCE = builder
                .comment("古代墓穴生成概率，旧版 HBM 1.7.10 默认每 4000 个区块尝试一次；设置为 0 可禁用。")
                .defineInRange("ancientTombSpawnChance", 4000, 0, Integer.MAX_VALUE);
        ANTENNA_STRUCTURE_SPAWN_RATE = builder
                .comment("旧版地表天线结构生成间隔。HBM 1.7.10 WorldConfig.antennaStructure 默认：250；设置为 0 可禁用。")
                .defineInRange("antennaStructureSpawnRate", 250, 0, 1_000_000);
        ATOM_STRUCTURE_SPAWN_RATE = builder
                .comment("旧版沙漠核设施生成间隔。HBM 1.7.10 WorldConfig.atomStructure 默认：500；设置为 0 可禁用。")
                .defineInRange("atomStructureSpawnRate", 500, 0, 1_000_000);
        LIBRARY_DUNGEON_SPAWN_RATE = builder
                .comment("旧版图书馆地牢生成间隔。HBM 1.7.10 WorldConfig.dungeonStructure 默认：64；设置为 0 可禁用。")
                .defineInRange("libraryDungeonSpawnRate", 64, 0, 1_000_000);
        DUD_STRUCTURE_SPAWN_RATE = builder
                .comment("未爆弹地表生成间隔。HBM 1.7.10 WorldConfig.dudStructure 默认：500；设置为 0 可禁用。")
                .defineInRange("dudStructureSpawnRate", 500, 0, 1_000_000);
        SPACESHIP_STRUCTURE_SPAWN_RATE = builder
                .comment("旧版坠毁飞船生成间隔。HBM 1.7.10 WorldConfig.spaceshipStructure 默认：1000；设置为 0 可禁用。")
                .defineInRange("spaceshipStructureSpawnRate", 1000, 0, 1_000_000);
        WASTE_TANK_STRUCTURE_SPAWN_RATE = builder
                .comment("旧版核废料罐结构生成间隔。HBM 1.7.10 WorldConfig.barrelStructure 默认：5000；设置为 0 可禁用。")
                .defineInRange("wasteTankStructureSpawnRate", 5000, 0, 1_000_000);
        BROADCASTER_SPAWN_RATE = builder
                .comment("腐化广播器地表生成间隔。HBM 1.7.10 WorldConfig.broadcaster 默认：5000；设置为 0 可禁用。")
                .defineInRange("broadcasterSpawnRate", 5000, 0, 1_000_000);
        ENABLE_WORLDGEN_LANDMINES = builder
                .comment("是否生成旧版地表反步兵地雷。对应 HBM 1.7.10 GeneralConfig.enableMines，默认：true。")
                .define("enableWorldgenLandmines", true);
        LANDMINE_SPAWN_RATE = builder
                .comment("旧版地表反步兵地雷生成间隔。HBM 1.7.10 WorldConfig.minefreq 默认：64；设置为 0 可禁用。")
                .defineInRange("landmineSpawnRate", 64, 0, 1_000_000);
        ENABLE_528_BOSNIA_MINES = builder
                .comment("是否允许 528 波黑模拟器高爆地雷散点生成。仅在 enable528Mode=true 时生效；旧版子开关默认：true。")
                .define("enable528BosniaMines", true);
        ENABLE_WORLDGEN_VAULTS = builder
                .comment("是否生成旧版地表保险箱。对应 HBM 1.7.10 GeneralConfig.enableVaults，默认：true。")
                .define("enableWorldgenVaults", true);
        VAULT_SPAWN_RATE = builder
                .comment("旧版地表保险箱生成间隔。HBM 1.7.10 WorldConfig.vaultfreq 默认：2500；设置为 0 可禁用。")
                .defineInRange("vaultSpawnRate", 2500, 0, 1_000_000);
        SOYUZ_CAPSULE_SPAWN_RATE = builder
                .comment("沙滩联盟号返回舱生成间隔。HBM 1.7.10 WorldConfig.capsuleStructure 默认：100；设置为 0 可禁用。")
                .defineInRange("soyuzCapsuleSpawnRate", 100, 0, 1_000_000);
        ARCTIC_VAULT_SPAWN_RATE = builder
                .comment("寒冷地下代码库生成间隔。HBM 1.7.10 WorldConfig.arcticStructure 默认：500；设置为 0 可禁用。")
                .defineInRange("arcticVaultSpawnRate", 500, 0, 1_000_000);
        JUNGLE_DUNGEON_SPAWN_RATE = builder
                .comment("旧版丛林地牢生成间隔。HBM 1.7.10 WorldConfig.jungleStructure 默认：2000；设置为 0 可禁用。")
                .defineInRange("jungleDungeonSpawnRate", 2000, 0, 1_000_000);
        PINK_TREE_SPAWN_RATE = builder
                .comment("橡木替换为粉色树干的旧版彩蛋生成间隔。HBM 1.7.10 硬编码默认：1000；设置为 0 可禁用。")
                .defineInRange("pinkTreeSpawnRate", 1000, 0, 1_000_000);
        STONE_KEYHOLE_SPAWN_RATE = builder
                .comment("石钥孔散点生成间隔。HBM 1.7.10 硬编码默认：4；设置为 0 可禁用。")
                .defineInRange("stoneKeyholeSpawnRate", 4, 0, 1_000_000);

        builder.pop();
        builder.pop();

        HbmServerParityConfig.define(builder); // [parity S2: configs] ServerConfig keys that were hard-coded
        HbmParityV3Config.define(builder); // [parity V3: configs] BombConfig/RadiationConfig/ToolConfig/MobConfig keys
        SPEC = builder.build();
    }

    private HbmConfig() {
    }

    private static ModConfigSpec.IntValue weight(ModConfigSpec.Builder builder, String name, String comment, int defaultValue) {
        return builder
                .comment(comment + " HBM 1.12.2 默认：" + defaultValue + "。设置为 0 可禁用该结构。")
                .defineInRange(name, defaultValue, 0, 1_000_000);
    }

    public static int radius(ModConfigSpec.IntValue minValue, ModConfigSpec.IntValue maxValue, RandomSource random) {
        int min = Math.min(minValue.get(), maxValue.get());
        int max = Math.max(minValue.get(), maxValue.get());
        return min + random.nextInt(max - min + 1);
    }

    /** CommonConfig.parseStructureFlag used by HBM 1.7.10's HbmWorldGen. */
    public static boolean legacyDungeonGenerationEnabled(boolean worldGenerateStructures) {
        return switch (LEGACY_DUNGEON_SPAWN_FLAG.get().toLowerCase(Locale.US)) {
            case "true", "on", "yes" -> true;
            case "false", "off", "no" -> false;
            default -> worldGenerateStructures;
        };
    }

    /** Mirrors VersatileConfig's 1.7.10 RTG rule: 528 mode forces decay. */
    public static boolean rtgDecay() {
        return ENABLE_528_MODE.get() || ENABLE_RTG_DECAY.get();
    }

    /** Mirrors GeneralConfig: 528 mode forcibly disables LBSM. */
    public static boolean lessBullshitMode() {
        return !ENABLE_528_MODE.get() && ENABLE_LESS_BULLSHIT_MODE.get();
    }

    public static boolean lbsmFullSchrab() {
        return lessBullshitMode() && LBSM_FULL_SCHRAB.get();
    }

    public static boolean lbsmShorterDecay() {
        return lessBullshitMode() && LBSM_SHORTER_DECAY.get();
    }

    public static boolean lbsmSimpleArmorRecipes() {
        return lessBullshitMode() && LBSM_SIMPLE_ARMOR_RECIPES.get();
    }

    public static boolean lbsmSimpleToolRecipes() {
        return lessBullshitMode() && LBSM_SIMPLE_TOOL_RECIPES.get();
    }

    public static boolean lbsmSimpleAlloy() {
        return lessBullshitMode() && LBSM_SIMPLE_ALLOY.get();
    }

    public static boolean lbsmSimpleChemistry() {
        return lessBullshitMode() && LBSM_SIMPLE_CHEMISTRY.get();
    }

    public static boolean lbsmSimpleCentrifuge() {
        return lessBullshitMode() && LBSM_SIMPLE_CENTRIFUGE.get();
    }

    public static boolean lbsmUnlockAnvil() {
        return lessBullshitMode() && LBSM_UNLOCK_ANVIL.get();
    }

    public static boolean lbsmSimpleCrafting() {
        return lessBullshitMode() && LBSM_SIMPLE_CRAFTING.get();
    }

    public static boolean lbsmSimpleMedicineRecipes() {
        return lessBullshitMode() && LBSM_SIMPLE_MEDICINE_RECIPES.get();
    }

    public static int schrabOreChance() {
        return lessBullshitMode() ? LBSM_SCHRAB_RATE.get() : 250;
    }

    public static int longDecayChance() {
        int minute = 60 * 20;
        int hour = 60 * minute;
        return ENABLE_528_MODE.get() ? 15 * hour : lbsmShorterDecay() ? 15 * minute : 3 * hour;
    }

    public static int shortDecayChance() {
        int minute = 60 * 20;
        int hour = 60 * minute;
        return ENABLE_528_MODE.get() ? 3 * hour : lbsmShorterDecay() ? 3 * minute : 15 * minute;
    }

    /** Mirrors VersatileConfig's 1.7.10 RTG rule: 528 mode forces output scaling. */
    public static boolean scaleRtgPower() {
        return ENABLE_528_MODE.get() || SCALE_RTG_POWER.get();
    }

    public static boolean reasimBoilers() {
        return RBMK_REASIM_BOILERS.get()
                || ENABLE_528_MODE.get() && ENABLE_528_FORCE_REASIM_BOILERS.get();
    }

    public static boolean bosniaMines() {
        return ENABLE_528_MODE.get() && ENABLE_528_BOSNIA_MINES.get();
    }

    public static boolean netherBurn() {
        return ENABLE_528_MODE.get() && ENABLE_528_NETHER_BURN.get();
    }

    public static boolean pressurizedRecipes() {
        return ENABLE_528_MODE.get() && ENABLE_528_PRESSURIZED_RECIPES.get();
    }

    public static boolean explosiveEnergistics() {
        return ENABLE_528_MODE.get() && ENABLE_528_EXPLOSIVE_ENERGISTICS.get();
    }

    public static boolean machineGravity() {
        return ENABLE_MACHINE_GRAVITY.get()
                || ENABLE_528_MODE.get() && ENABLE_528_MACHINE_GRAVITY.get();
    }

    public static boolean glyphidNaturalScoutSpawn() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_NATURAL_SCOUT_SPAWN.get();
    }

    public static boolean glyphidExtendedTargeting() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_RAMPANT_EXTENDED_TARGETING.get();
    }

    public static boolean glyphidDig() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_RAMPANT_DIG.get();
    }

    public static boolean glyphidGuidance() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_RAMPANT_GUIDANCE.get();
    }

    public static int glyphidScoutSwarmChance() {
        return GLYPHID_RAMPANT_MODE.get() ? 1 : GLYPHID_SCOUT_SWARM_CHANCE.get();
    }

    public static int glyphidSwarmCooldown() {
        // [e3] multiplyExact threw ArithmeticException inside the spawner tick for values above Integer.MAX_VALUE / 20
        return (int) Math.min((long) GLYPHID_SWARM_COOLDOWN_SECONDS.get() * 20L, Integer.MAX_VALUE);
    }

    public static int[] glyphidChance(ModConfigSpec.ConfigValue<List<? extends Integer>> value, String key) {
        List<? extends Integer> entries = value.get();
        if (entries.size() != 3) {
            throw new IllegalStateException("Glyphid chance entry " + key + " must contain exactly three integers.");
        }
        return new int[]{entries.get(0), entries.get(1), entries.get(2)};
    }

    private static ModConfigSpec.ConfigValue<List<? extends Integer>> glyphidChance(
            ModConfigSpec.Builder builder, String key, List<Integer> defaults
    ) {
        return builder.comment("Glyphid spawning tuple: base chance, soot modifier, minimum soot. HBM 1.7.10 default: " + defaults + '.')
                .defineList(key, defaults, value -> value instanceof Integer);
    }

    public static double glyphidScoutSootThreshold() {
        return GLYPHID_RAMPANT_MODE.get() ? 0.1D : GLYPHID_SCOUT_SOOT_THRESHOLD.get();
    }

    /** Rampant mode raises pollution output exactly as MobConfig did in 1.7.10. */
    public static double pollutionMultiplier() {
        double configured = POLLUTION_MULTIPLIER.get();
        return GLYPHID_RAMPANT_MODE.get() && configured == 1.0D ? 3.0D : configured;
    }

    public static double smokestackPollutionModifier(boolean industrial) {
        if (GLYPHID_RAMPANT_MODE.get()) {
            double override = GLYPHID_RAMPANT_SMOKESTACK_OVERRIDE.get();
            return industrial ? override / 2.0D : override;
        }
        return industrial ? 0.1D : 0.25D;
    }

    public static boolean gunsEnabled() {
        return !SPEC.isLoaded() || ENABLE_GUNS.get();
    }

    public static float longPistolDamage() {
        return SPEC.isLoaded() ? GUN_LONG_PISTOL_DAMAGE.get().floatValue() : 25.0F;
    }

    public static float uziDamage() {
        return SPEC.isLoaded() ? GUN_UZI_DAMAGE.get().floatValue() : 3.0F;
    }

    public static float uziAkimboLeftDamage() {
        return SPEC.isLoaded() ? GUN_UZI_AKIMBO_LEFT_DAMAGE.get().floatValue() : 3.0F;
    }

    public static float uziAkimboRightDamage() {
        return SPEC.isLoaded() ? GUN_UZI_AKIMBO_RIGHT_DAMAGE.get().floatValue() : 3.0F;
    }

    public static float assaultRifleDamage() {
        return SPEC.isLoaded() ? GUN_ASSAULT_RIFLE_DAMAGE.get().floatValue() : 5.0F;
    }

    public static float zebraDamage() {
        return SPEC.isLoaded() ? GUN_ZEBRA_DAMAGE.get().floatValue() : 7.5F;
    }

    public static float stg77Damage() {
        return SPEC.isLoaded() ? GUN_STG77_DAMAGE.get().floatValue() : 10.0F;
    }

    // [weapons batch A: config getters]
    public static float am180Damage() {
        return SPEC.isLoaded() ? GUN_AM180_DAMAGE.get().floatValue() : 2.0F;
    }

    public static float starfDamage() {
        return SPEC.isLoaded() ? GUN_STARF_DAMAGE.get().floatValue() : 12.5F;
    }

    public static float starfAkimboLeftDamage() {
        return SPEC.isLoaded() ? GUN_STARF_AKIMBO_LEFT_DAMAGE.get().floatValue() : 12.5F;
    }

    public static float starfAkimboRightDamage() {
        return SPEC.isLoaded() ? GUN_STARF_AKIMBO_RIGHT_DAMAGE.get().floatValue() : 12.5F;
    }

    public static float antiMaterielDamage() {
        return SPEC.isLoaded() ? GUN_ANTI_MATERIEL_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float subtletyDamage() {
        return SPEC.isLoaded() ? GUN_SUBTLETY_DAMAGE.get().floatValue() : 50.0F;
    }

    public static float penanceDamage() {
        return SPEC.isLoaded() ? GUN_PENANCE_DAMAGE.get().floatValue() : 45.0F;
    }

    public static float m2Damage() {
        return SPEC.isLoaded() ? GUN_M2_DAMAGE.get().floatValue() : 7.5F;
    }

    // [weapons batch B: config getters]
    // [weapons batch C1: config getters]
    public static float lightRevolverDamage() {
        return SPEC.isLoaded() ? GUN_LIGHT_REVOLVER_DAMAGE.get().floatValue() : 7.5F;
    }

    public static float atlasDamage() {
        return SPEC.isLoaded() ? GUN_ATLAS_DAMAGE.get().floatValue() : 12.5F;
    }

    public static float daniCelestialDamage() {
        return SPEC.isLoaded() ? GUN_DANI_CELESTIAL_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float daniLunarDamage() {
        return SPEC.isLoaded() ? GUN_DANI_LUNAR_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float pepperboxDamage() {
        return SPEC.isLoaded() ? GUN_PEPPERBOX_DAMAGE.get().floatValue() : 5.0F;
    }

    public static float henryDamage() {
        return SPEC.isLoaded() ? GUN_HENRY_DAMAGE.get().floatValue() : 10.0F;
    }

    public static float lincolnDamage() {
        return SPEC.isLoaded() ? GUN_LINCOLN_DAMAGE.get().floatValue() : 20.0F;
    }

    public static float heavyRevolverDamage() {
        return SPEC.isLoaded() ? GUN_HEAVY_REVOLVER_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float lilmacDamage() {
        return SPEC.isLoaded() ? GUN_LILMAC_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float protegeDamage() {
        return SPEC.isLoaded() ? GUN_PROTEGE_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float hangmanDamage() {
        return SPEC.isLoaded() ? GUN_HANGMAN_DAMAGE.get().floatValue() : 25.0F;
    }

    public static float hangmanSmackDamage() {
        return SPEC.isLoaded() ? GUN_HANGMAN_SMACK_DAMAGE.get().floatValue() : 10.0F;
    }

    // [weapons batch C2: config getters]
    // [weapons batch C3: config getters]
    public static float carbineDamage() {
        return SPEC.isLoaded() ? GUN_CARBINE_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float carbineBayonetDamage() {
        return SPEC.isLoaded() ? GUN_CARBINE_BAYONET_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float minigunDamage() {
        return SPEC.isLoaded() ? GUN_MINIGUN_DAMAGE.get().floatValue() : 6.0F;
    }

    public static float lacunaeDamage() {
        return SPEC.isLoaded() ? GUN_LACUNAE_DAMAGE.get().floatValue() : 12.0F;
    }

    // [weapons batch E2: config getters]
    public static float teslaCannonDamage() {
        return SPEC.isLoaded() ? GUN_TESLA_CANNON_DAMAGE.get().floatValue() : 35.0F;
    }

    public static float laserPistolDamage() {
        return SPEC.isLoaded() ? GUN_LASER_PISTOL_DAMAGE.get().floatValue() : 25.0F;
    }

    public static float pewPewDamage() {
        return SPEC.isLoaded() ? GUN_PEW_PEW_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float morningGloryDamage() {
        return SPEC.isLoaded() ? GUN_MORNING_GLORY_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float laserRifleDamage() {
        return SPEC.isLoaded() ? GUN_LASER_RIFLE_DAMAGE.get().floatValue() : 50.0F;
    }

    // [weapons batch F1: config accessors]
    public static float flamerDamage() {
        return SPEC.isLoaded() ? GUN_FLAMER_DAMAGE.get().floatValue() : 1.0F;
    }

    public static float topazDamage() {
        return SPEC.isLoaded() ? GUN_TOPAZ_DAMAGE.get().floatValue() : 1.5F;
    }

    public static float daybreakerDamage() {
        return SPEC.isLoaded() ? GUN_DAYBREAKER_DAMAGE.get().floatValue() : 25.0F;
    }

    // [weapons batch F3: config getters]
    public static float extinguisherDamage() {
        return SPEC.isLoaded() ? GUN_EXTINGUISHER_DAMAGE.get().floatValue() : 0.0F;
    }

    public static float chargeThrowerDamage() {
        return SPEC.isLoaded() ? GUN_CHARGE_THROWER_DAMAGE.get().floatValue() : 10.0F;
    }

    public static float drillDamage() {
        return SPEC.isLoaded() ? GUN_DRILL_DAMAGE.get().floatValue() : 10.0F;
    }

    // [parity c1] XFactoryPA config accessors
    public static float hereticDamage() {
        return SPEC.isLoaded() ? GUN_HERETIC_DAMAGE.get().floatValue() : 100.0F;
    }
    // [parity d2] gun_minigun_dual
    public static float minigunDualLeftDamage() {
        return SPEC.isLoaded() ? GUN_MINIGUN_DUAL_LEFT_DAMAGE.get().floatValue() : 6.0F;
    }
    public static float minigunDualRightDamage() {
        return SPEC.isLoaded() ? GUN_MINIGUN_DUAL_RIGHT_DAMAGE.get().floatValue() : 6.0F;
    }
    public static float meleeRpaSwingsDamage() {
        return SPEC.isLoaded() ? MELEE_RPA_SWINGS_DAMAGE.get().floatValue() : 15.0F;
    }
    public static float meleeRpaSlapDamage() {
        return SPEC.isLoaded() ? MELEE_RPA_SLAP_DAMAGE.get().floatValue() : 35.0F;
    }
    public static float meleeRpaLargeMult() {
        return SPEC.isLoaded() ? MELEE_RPA_LARGE_MULT.get().floatValue() : 2.5F;
    }
    public static float meleeNcrpaSwingsDamage() {
        return SPEC.isLoaded() ? MELEE_NCRPA_SWINGS_DAMAGE.get().floatValue() : 15.0F;
    }
    public static float meleeNcrpaSweepDamage() {
        return SPEC.isLoaded() ? MELEE_NCRPA_SWEEP_DAMAGE.get().floatValue() : 35.0F;
    }
    public static float meleeNcrpaLargeMult() {
        return SPEC.isLoaded() ? MELEE_NCRPA_LARGE_MULT.get().floatValue() : 2.5F;
    }
    public static float rangedNcrpaDamage() {
        return SPEC.isLoaded() ? RANGED_NCRPA_DAMAGE.get().floatValue() : 25.0F;
    }

    // [weapons batch F2: config accessors]
    public static float tauCannonDamage() {
        return SPEC.isLoaded() ? GUN_TAU_CANNON_DAMAGE.get().floatValue() : 25.0F;
    }

    public static float tauCannonChargeDamage() {
        return SPEC.isLoaded() ? GUN_TAU_CANNON_CHARGE_DAMAGE.get().floatValue() : 5.0F;
    }

    public static float coilgunDamage() {
        return SPEC.isLoaded() ? GUN_COILGUN_DAMAGE.get().floatValue() : 35.0F;
    }

    public static float ni4niDamage() {
        return SPEC.isLoaded() ? GUN_NI4NI_DAMAGE.get().floatValue() : 35.0F;
    }

    public static float fatmanDamage() {
        return SPEC.isLoaded() ? GUN_FATMAN_DAMAGE.get().floatValue() : 100.0F;
    }

    public static float southStarDamage() {
        return SPEC.isLoaded() ? GUN_SOUTH_STAR_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float southStarBayonetDamage() {
        return SPEC.isLoaded() ? GUN_SOUTH_STAR_BAYONET_DAMAGE.get().floatValue() : 15.0F;
    }

    // [weapons batch C4: config getters]
    public static float doubleBarrelDamage() {
        return SPEC.isLoaded() ? GUN_DOUBLE_BARREL_DAMAGE.get().floatValue() : 30.0F;
    }

    public static float sacredDragonDamage() {
        return SPEC.isLoaded() ? GUN_SACRED_DRAGON_DAMAGE.get().floatValue() : 45.0F;
    }

    public static float maresLegDamage() {
        return SPEC.isLoaded() ? GUN_MARES_LEG_DAMAGE.get().floatValue() : 16.0F;
    }

    public static float maresLegAkimboLeftDamage() {
        return SPEC.isLoaded() ? GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE.get().floatValue() : 16.0F;
    }

    public static float maresLegAkimboRightDamage() {
        return SPEC.isLoaded() ? GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE.get().floatValue() : 16.0F;
    }

    public static float brokenDamage() {
        return SPEC.isLoaded() ? GUN_BROKEN_DAMAGE.get().floatValue() : 48.0F;
    }

    public static float liberatorDamage() {
        return SPEC.isLoaded() ? GUN_LIBERATOR_DAMAGE.get().floatValue() : 16.0F;
    }

    // [weapons batch D1: config getters]
    public static float spasDamage() {
        return SPEC.isLoaded() ? GUN_SPAS_DAMAGE.get().floatValue() : 32.0F;
    }

    public static float autoshotgunDamage() {
        return SPEC.isLoaded() ? GUN_AUTOSHOTGUN_DAMAGE.get().floatValue() : 48.0F;
    }

    public static float shredderDamage() {
        return SPEC.isLoaded() ? GUN_SHREDDER_DAMAGE.get().floatValue() : 50.0F;
    }

    public static float sexyDamage() {
        return SPEC.isLoaded() ? GUN_SEXY_DAMAGE.get().floatValue() : 64.0F;
    }

    // [weapons batch D3: config getters]
    public static float panzerschreckDamage() {
        return SPEC.isLoaded() ? GUN_PANZERSCHRECK_DAMAGE.get().floatValue() : 25.0F;
    }

    public static float quadroDamage() {
        return SPEC.isLoaded() ? GUN_QUADRO_DAMAGE.get().floatValue() : 40.0F;
    }

    public static float fliegerfaustDamage() {
        return SPEC.isLoaded() ? GUN_FLIEGERFAUST_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float missileLauncherDamage() {
        return SPEC.isLoaded() ? GUN_MISSILE_LAUNCHER_DAMAGE.get().floatValue() : 50.0F;
    }

    public static float bolterDamage() {
        return SPEC.isLoaded() ? GUN_BOLTER_DAMAGE.get().floatValue() : 15.0F;
    }
    // [weapons batch F4: config getters]
    public static float stingerDamage() {
        return SPEC.isLoaded() ? GUN_STINGER_DAMAGE.get().floatValue() : 35.0F;
    }

    public static float aberratorDamage() {
        return SPEC.isLoaded() ? GUN_ABERRATOR_DAMAGE.get().floatValue() : 100.0F;
    }

    public static float eottLeftDamage() {
        return SPEC.isLoaded() ? GUN_EOTT_LEFT_DAMAGE.get().floatValue() : 100.0F;
    }

    public static float eottRightDamage() {
        return SPEC.isLoaded() ? GUN_EOTT_RIGHT_DAMAGE.get().floatValue() : 100.0F;
    }

    public static float follyDamage() {
        return SPEC.isLoaded() ? GUN_FOLLY_DAMAGE.get().floatValue() : 1000.0F;
    }

    public static float flareDamage() {
        return SPEC.isLoaded() ? GUN_FLARE_DAMAGE.get().floatValue() : 15.0F;
    }

    public static float congoLakeDamage() {
        return SPEC.isLoaded() ? GUN_CONGO_LAKE_DAMAGE.get().floatValue() : 20.0F;
    }

    public static float mk108Damage() {
        return SPEC.isLoaded() ? GUN_MK108_DAMAGE.get().floatValue() : 25.0F;
    }


    public static float greasegunDamage() {
        return SPEC.isLoaded() ? GUN_GREASEGUN_DAMAGE.get().floatValue() : 3.0F;
    }
}
