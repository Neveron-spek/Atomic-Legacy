package com.atomiclegacy.entity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.ai.LegacyFireGunGoal;
import com.atomiclegacy.items.GasMaskItem;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.7.10 MobUtil's slot pools, including the firearm pools. MobUtil inserted
 * integer slots 4 through 0 into a HashMap; the old HashMap bucket iteration
 * therefore consumed its random rolls in the concrete 0, 1, 2, 3, 4 order.
 */
public final class LegacyMobEquipment {
    private static final String PROTOCOL_COMPAT_MOD_ID = "reinhardt_protocol_compat";
    private static final Random RANDOM = new Random();
    private static final EquipmentSlot[] LEGACY_SLOT_ITERATION_ORDER = {
            EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private static final Map<EquipmentSlot, Pool> COMMON_SOOT = commonSootPools();
    private static final Map<EquipmentSlot, Pool> COMMON = commonPools();
    private static final Map<EquipmentSlot, Pool> ADVANCED = advancedPools();
    private static final Map<EquipmentSlot, Pool> RANGED_SOOT = rangedSootPools();
    private static final Map<EquipmentSlot, Pool> RANGED = rangedPools();
    private static final Map<EquipmentSlot, Pool> ADVANCED_RANGED = advancedRangedPools();

    // [parity E1: MobUtil.slotPoolGuns, keyed by minimum soot; createSlotPool(items) had no empty entry]
    private static final Pool GUNS_SOOT_0_3 = pool(0, entries(
            "gun_light_revolver", 16, "gun_greasegun", 8, "gun_maresleg", 2
    ));
    private static final Pool GUNS_SOOT_1 = pool(0, entries(
            "gun_light_revolver", 6, "gun_greasegun", 8, "gun_maresleg", 4, "gun_henry", 6
    ));
    private static final Pool GUNS_SOOT_3 = pool(0, entries(
            "gun_uzi", 10, "gun_maresleg", 8, "gun_henry", 12, "gun_heavy_revolver", 4, "gun_flaregun", 2
    ));
    private static final Pool GUNS_SOOT_5 = pool(0, entries(
            "gun_am180", 6, "gun_uzi", 10, "gun_spas12", 8, "gun_henry_lincoln", 2, "gun_heavy_revolver", 12,
            "gun_flaregun", 4, "gun_flamer", 2
    ));
    // [parity E1: MobUtil.slotPoolGunsTier1..3, slot 0 with an empty weight of 0, for the Logic Block actions]
    private static final Pool GUNS_TIER_1 = pool(0, entries(
            "gun_light_revolver", 16, "gun_greasegun", 8, "gun_maresleg", 2, "gun_flaregun", 1
    ));
    private static final Pool GUNS_TIER_2 = pool(0, entries(
            "gun_uzi", 12, "gun_maresleg", 8, "gun_henry", 12, "gun_heavy_revolver", 8, "gun_flaregun", 4,
            "gun_star_f", 8
    ));
    private static final Pool GUNS_TIER_3 = pool(0, entries(
            "gun_g3", 25, "gun_spas12", 20, "gun_carbine", 15, "gun_star_f", 20, "gun_am180", 6, "gun_amat", 5
    ));
    /** ModEventHandler.decorateMob: createSlotPool(50, {{bowReplacement, 1}}). */
    public static final int SKELEGUN_EMPTY_WEIGHT = 50;
    /** ModEventHandler.getSkelegun below 0.3 soot: pepperbox 5, then null 20. */
    public static final int PEPPERBOX_WEIGHT = 5;
    public static final int PEPPERBOX_EMPTY_WEIGHT = 20;

    private LegacyMobEquipment() {
    }

    /** MobUtil.slotPoolCommonS, used by the old global zombie spawn listener. */
    public static void assignCommonSoot(Mob mob) {
        assign(mob, COMMON_SOOT, RANDOM);
    }

    /** MobUtil.slotPoolCommon, used by the tier-one Logic Block zombie wave. */
    public static void assignCommon(Mob mob) {
        // LogicBlockActions constructed a fresh java.util.Random for every
        // MobUtil.assignItemsToEntity call.
        assign(mob, COMMON, new Random());
    }

    /** MobUtil.slotPoolAdv, used by FODDER_WAVE and tier-two zombie waves. */
    public static void assignAdvanced(Mob mob) {
        // LogicBlockActions constructed a fresh java.util.Random for every
        // MobUtil.assignItemsToEntity call.
        assign(mob, ADVANCED, new Random());
    }

    /** MobUtil.equipFullSet(...hazmat...). */
    public static void equipHazmat(Mob mob) {
        equipWithoutNativeDrop(mob, EquipmentSlot.HEAD, new ItemStack(item("hazmat_helmet")));
        equipWithoutNativeDrop(mob, EquipmentSlot.CHEST, new ItemStack(item("hazmat_plate")));
        equipWithoutNativeDrop(mob, EquipmentSlot.LEGS, new ItemStack(item("hazmat_legs")));
        equipWithoutNativeDrop(mob, EquipmentSlot.FEET, new ItemStack(item("hazmat_boots")));
    }

    /**
     * ModEventHandler.decorateMob's skeleton branch: slotPoolRangedS plus slot 0
     * = createSlotPool(50, bowReplacement != null ? {{bowReplacement, 1}} : {}),
     * rolled with ModEventHandler's own Random in slot order 0..4.
     */
    public static void assignSootRangedSkeleton(AbstractSkeleton skeleton, float soot, RandomSource worldRandom) {
        assignSootRangedSkeleton(skeleton, getSkelegun(soot, worldRandom));
    }

    /** As above with the getSkelegun result already decided ({@code null} = no bow replacement). */
    public static void assignSootRangedSkeleton(AbstractSkeleton skeleton, String bowReplacement) {
        Map<EquipmentSlot, Pool> pools = new HashMap<>(RANGED_SOOT);
        pools.put(EquipmentSlot.MAINHAND, pool(SKELEGUN_EMPTY_WEIGHT,
                bowReplacement != null ? entries(bowReplacement, 1) : List.of()));
        assign(skeleton, pools, RANDOM);
    }

    /**
     * ModEventHandler.getSkelegun. The roll order (nextDouble, then the weighted
     * nextInt), the float soot arithmetic and the branch bounds are the
     * original's, including its NaN/zero logarithm behaviour.
     */
    public static String getSkelegun(float soot, RandomSource rand) {
        if (!HbmConfig.ENABLE_MOB_WEAPONS.get()) return null;

        soot -= HbmConfig.MOB_WEAPON_SOOT_REDUCTION.get().doubleValue();
        if (rand.nextDouble() > Math.log(soot) * 0.25) return null;

        Pool pool = skelegunPool(soot);
        if (pool == null) {
            int roll = rand.nextInt(PEPPERBOX_WEIGHT + PEPPERBOX_EMPTY_WEIGHT);
            return roll < PEPPERBOX_WEIGHT ? "gun_pepperbox" : null;
        }
        return pool.choose((java.util.function.IntUnaryOperator) bound -> rand.nextInt(bound));
    }

    /** The soot branch of getSkelegun; {@code null} is the inline pepperbox pool. */
    private static Pool skelegunPool(float soot) {
        if (soot < 0.3) {
            return null;
        } else if (soot > 0.3 && soot < 1) {
            return GUNS_SOOT_0_3;
        } else if (soot < 3) {
            return GUNS_SOOT_1;
        } else if (soot < 5) {
            return GUNS_SOOT_3;
        } else {
            return GUNS_SOOT_5;
        }
    }

    /**
     * The three discrete LogicBlockActions skeleton branches: SKELETONS_GUN_TIER_1
     * (tier-one guns, the never-filled slotPoolMasks, slotPoolRanged),
     * SKELETONS_GUN_TIER_2 and _3 (tuned fire task first, then guns, then armour).
     */
    public static void assignLogicSkeletonTier(Skeleton skeleton, int tier) {
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Unknown 1.7.10 skeleton equipment tier " + tier);
        }
        if (tier != 1) {
            LegacyFireGunGoal gunTask = new LegacyFireGunGoal(skeleton);
            gunTask.minWait = 4;
            gunTask.maxWait = 5;
            gunTask.maxRange = tier == 2 ? 50 : 100;
            gunTask.burstTime = 6;
            gunTask.inaccuracy = tier == 2 ? 5F : 1F;
            gunTask.randomBurst = false;
            addFireTask(skeleton, gunTask);
        }
        // LogicBlockActions created a fresh Random for each MobUtil call.
        Map<EquipmentSlot, Pool> guns = new HashMap<>();
        guns.put(EquipmentSlot.MAINHAND, tier == 1 ? GUNS_TIER_1 : tier == 2 ? GUNS_TIER_2 : GUNS_TIER_3);
        assign(skeleton, guns, new Random());
        assign(skeleton, tier == 3 ? ADVANCED_RANGED : RANGED, new Random());
    }

    // these fucking tasks keep stacking on top of themselves
    /** MobUtil.addFireTask(EntityLiving). */
    public static void addFireTask(Mob entity) {
        entity.setDropChance(EquipmentSlot.MAINHAND, 0.0F); // Prevent dropping guns

        if (hasFireTask(entity)) return;

        entity.goalSelector.addGoal(3, new LegacyFireGunGoal(entity));
    }

    /** MobUtil.addFireTask(EntityLiving, EntityAIFireGun). */
    public static void addFireTask(Mob entity, LegacyFireGunGoal gunTask) {
        entity.setDropChance(EquipmentSlot.MAINHAND, 0.0F); // Prevent dropping guns

        if (hasFireTask(entity)) return;

        entity.goalSelector.addGoal(3, gunTask);
    }

    public static boolean hasFireTask(Mob entity) {
        return getFireTask(entity) != null;
    }

    /** The mob's fire task, or {@code null}. */
    public static LegacyFireGunGoal getFireTask(Mob entity) {
        for (WrappedGoal task : entity.goalSelector.getAvailableGoals()) {
            if (task.getGoal() instanceof LegacyFireGunGoal goal) return goal;
        }
        return null;
    }

    /** Diagnostic view of a firearm pool: "id=weight" in original order, with "null=weight" first when present. */
    public static List<String> describeGunPool(String name) {
        Pool pool = switch (name) {
            case "slotPoolGuns[0.3]" -> GUNS_SOOT_0_3;
            case "slotPoolGuns[1]" -> GUNS_SOOT_1;
            case "slotPoolGuns[3]" -> GUNS_SOOT_3;
            case "slotPoolGuns[5]" -> GUNS_SOOT_5;
            case "slotPoolGunsTier1" -> GUNS_TIER_1;
            case "slotPoolGunsTier2" -> GUNS_TIER_2;
            case "slotPoolGunsTier3" -> GUNS_TIER_3;
            default -> throw new IllegalArgumentException("Unknown 1.7.10 gun pool " + name);
        };
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (pool.emptyWeight != 0) out.add("null=" + pool.emptyWeight);
        for (WeightedId entry : pool.entries) out.add(entry.id + "=" + entry.weight);
        return out;
    }

    /** Diagnostic: which named pool getSkelegun draws from at this (already reduced) soot level. */
    public static String describeSkelegunBranch(float soot) {
        Pool pool = skelegunPool(soot);
        if (pool == null) return "pepperbox";
        if (pool == GUNS_SOOT_0_3) return "slotPoolGuns[0.3]";
        if (pool == GUNS_SOOT_1) return "slotPoolGuns[1]";
        if (pool == GUNS_SOOT_3) return "slotPoolGuns[3]";
        return "slotPoolGuns[5]";
    }

    /** Diagnostic view of an armour/melee pool map, e.g. "slotPoolRangedS" slot HEAD. */
    public static List<String> describeSlotPool(String name, EquipmentSlot slot) {
        Map<EquipmentSlot, Pool> pools = switch (name) {
            case "slotPoolCommonS" -> COMMON_SOOT;
            case "slotPoolRangedS" -> RANGED_SOOT;
            case "slotPoolCommon" -> COMMON;
            case "slotPoolRanged" -> RANGED;
            case "slotPoolAdv" -> ADVANCED;
            case "slotPoolAdvRanged" -> ADVANCED_RANGED;
            default -> throw new IllegalArgumentException("Unknown 1.7.10 slot pool " + name);
        };
        Pool pool = pools.get(slot);
        if (pool == null) return null;
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        out.add("null=" + pool.emptyWeight);
        for (WeightedId entry : pool.entries) out.add(entry.id + "=" + entry.weight);
        return out;
    }

    private static void assign(Mob mob, Map<EquipmentSlot, Pool> pools, Random random) {
        for (EquipmentSlot slot : LEGACY_SLOT_ITERATION_ORDER) {
            Pool pool = pools.get(slot);
            if (pool == null) {
                continue;
            }
            String id = pool.choose(random);
            apply(mob, slot, id);

            //Give skeleton AI if it has a gun
            // [parity E1: MobUtil.assignItemsToEntity - slot 0, EntitySkeleton, a non-null choice]
            if (id != null && slot == EquipmentSlot.MAINHAND && mob instanceof AbstractSkeleton) {
                addFireTask(mob);
            }
        }
    }

    private static void apply(Mob mob, EquipmentSlot slot, String id) {
        if (id == null) {
            return;
        }
        ItemStack stack = new ItemStack(item(id));
        if (id.equals("gas_mask_m65") || id.equals("gas_mask_olde") || id.equals("gas_mask_mono")) {
            if (!GasMaskItem.installFilter(stack, new ItemStack(HbmItems.GAS_MASK_FILTER.get()), mob)) {
                throw new IllegalStateException("Could not install the legacy gas-mask filter on " + id);
            }
        }
        equipWithoutNativeDrop(mob, slot, stack);
    }

    /**
     * HBM owns the spawn/equipment pool, while the compatibility layer owns
     * the equipment reward roll when it is installed.  Keeping the stack
     * equipped but setting its native chance to zero lets the compat layer
     * read installed filters and armor-module data during LivingDropsEvent.
     * Without the compatibility mod this helper leaves HBM's native behavior
     * untouched.
     */
    private static void equipWithoutNativeDrop(Mob mob, EquipmentSlot slot, ItemStack stack) {
        mob.setItemSlot(slot, stack);
        if (isProtocolCompatLoaded()) {
            mob.setDropChance(slot, 0.0F);
        }
    }

    private static boolean isProtocolCompatLoaded() {
        return ModList.get().isLoaded(PROTOCOL_COMPAT_MOD_ID);
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, id));
        if (item == net.minecraft.world.item.Items.AIR) {
            throw new IllegalStateException("Missing required 1.7.10 mob-pool item " + AtomicLegacy.MOD_ID + ":" + id);
        }
        return item;
    }

    private static Map<EquipmentSlot, Pool> commonSootPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(8000, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "cobalt_helmet", 2, "rag_piss", 1, "hat", 1,
                "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(7000, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "robes_plate", 32, "jackt", 32, "jackt2", 32,
                "steel_plate", 2
        )));
        pools.put(EquipmentSlot.LEGS, pool(7000, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32
        )));
        pools.put(EquipmentSlot.FEET, pool(7000, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2
        )));
        pools.put(EquipmentSlot.MAINHAND, pool(10000, entries(
                "weapon_pipe_lead", 30, "crowbar", 25, "geiger_counter", 20, "reer_graar", 16,
                "steel_pickaxe", 12, "stopsign", 10, "sopsign", 8, "chernobylsign", 6, "steel_sword", 15,
                "titanium_sword", 8, "lead_gavel", 4, "wrench_flipped", 2, "wrench", 20
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> commonPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(0, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "cobalt_helmet", 2, "rag_piss", 1, "hat", 1,
                "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(10, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "robes_plate", 32, "jackt", 32, "jackt2", 32,
                "steel_plate", 2
        )));
        pools.put(EquipmentSlot.LEGS, pool(20, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32
        )));
        pools.put(EquipmentSlot.FEET, pool(10, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2
        )));
        pools.put(EquipmentSlot.MAINHAND, pool(1000, entries(
                "weapon_pipe_lead", 30, "crowbar", 25, "geiger_counter", 20, "reer_graar", 16,
                "steel_pickaxe", 12, "stopsign", 10, "sopsign", 8, "chernobylsign", 6, "steel_sword", 15,
                "titanium_sword", 8, "lead_gavel", 4, "wrench_flipped", 2, "wrench", 20
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> advancedPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(0, entries(
                "security_helmet", 10, "t51_helmet", 4, "asbestos_helmet", 12, "liquidator_helmet", 4,
                "no9", 12, "hazmat_helmet", 6
        )));
        pools.put(EquipmentSlot.CHEST, pool(0, entries(
                "liquidator_plate", 4, "security_plate", 8, "asbestos_plate", 12, "t51_plate", 4,
                "hazmat_plate", 6, "steel_plate", 8
        )));
        pools.put(EquipmentSlot.LEGS, pool(0, entries(
                "liquidator_legs", 4, "security_legs", 8, "asbestos_legs", 12, "t51_legs", 4,
                "hazmat_legs", 6, "steel_legs", 8
        )));
        pools.put(EquipmentSlot.FEET, pool(0, entries(
                "liquidator_boots", 4, "security_boots", 8, "asbestos_boots", 12, "t51_boots", 4,
                "hazmat_boots", 6, "robes_boots", 8
        )));
        pools.put(EquipmentSlot.MAINHAND, pool(500, entries(
                "weapon_pipe_lead", 20, "crowbar", 10, "geiger_counter", 10, "reer_graar", 20,
                "wrench_flipped", 20, "stopsign", 16, "sopsign", 4, "chernobylsign", 16,
                "titanium_sword", 18, "lead_gavel", 8, "wrench", 20
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> rangedSootPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(8000, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "rag_piss", 1, "goggles", 1,
                "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(7000, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "steel_plate", 8, "titanium_plate", 4
        )));
        pools.put(EquipmentSlot.LEGS, pool(7000, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32
        )));
        pools.put(EquipmentSlot.FEET, pool(10000, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "titanium_boots", 6
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> rangedPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(0, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "rag_piss", 1, "goggles", 1,
                "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(10, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "steel_plate", 8, "titanium_plate", 4
        )));
        pools.put(EquipmentSlot.LEGS, pool(10, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32
        )));
        pools.put(EquipmentSlot.FEET, pool(10, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "titanium_boots", 6
        )));
        return pools;
    }

    /** slotPoolAdvRanged was a copy of slotPoolAdv with only slot 0 removed. */
    private static Map<EquipmentSlot, Pool> advancedRangedPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>(ADVANCED);
        pools.remove(EquipmentSlot.MAINHAND);
        return pools;
    }

    private static Pool pool(int emptyWeight, List<WeightedId> entries) {
        return new Pool(emptyWeight, entries);
    }

    private static List<WeightedId> entries(Object... values) {
        java.util.ArrayList<WeightedId> entries = new java.util.ArrayList<>(values.length / 2);
        for (int index = 0; index < values.length; index += 2) {
            entries.add(new WeightedId((String) values[index], (Integer) values[index + 1]));
        }
        return List.copyOf(entries);
    }

    private record WeightedId(String id, int weight) {
    }

    private record Pool(int emptyWeight, List<WeightedId> entries) {
        private String choose(Random random) {
            return choose((java.util.function.IntUnaryOperator) bound -> random.nextInt(bound));
        }

        private String choose(java.util.function.IntUnaryOperator nextInt) {
            int total = emptyWeight;
            for (WeightedId entry : entries) {
                total += entry.weight;
            }
            int roll = nextInt.applyAsInt(total);
            if (roll < emptyWeight) {
                return null;
            }
            roll -= emptyWeight;
            for (WeightedId entry : entries) {
                if (roll < entry.weight) {
                    return entry.id;
                }
                roll -= entry.weight;
            }
            throw new IllegalStateException("Legacy weighted mob-pool roll escaped its configured range");
        }
    }
}
