package com.atomiclegacy.event;

import java.util.List;
import java.util.Set;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyMobEquipment;
import com.atomiclegacy.entity.ai.LegacyFireGunGoal;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Parity E1: 1.7.10 mob armament - EntityAIFireGun, MobUtil gun pools,
 * ModEventHandler.decorateMob/getSkelegun/addAITasks, MobConfig 12.D01-D03.
 */
public final class ParityE1MobChecks {
    private ParityE1MobChecks() {
    }

    public static int run(ServerLevel level, List<String> errors) {
        int[] cases = {0};
        try {
            configs(errors, cases);
            gunPools(errors, cases);
            armourPools(errors, cases);
            skelegun(errors, cases);
            goalDefaults(level, errors, cases);
            forcedWeapon(level, errors, cases);
            joinHandler(level, errors, cases);
            logicTiers(level, errors, cases);
            goalSmoke(level, errors, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity E1 mob checks crashed", e);
            errors.add("E1: mob checks crashed: " + e);
        }
        return cases[0];
    }

    private static void check(List<String> errors, int[] cases, boolean ok, String what) {
        cases[0]++;
        if (!ok) errors.add("E1: " + what);
    }

    private static void configs(List<String> errors, int[] cases) {
        check(errors, cases, Boolean.TRUE.equals(HbmConfig.ENABLE_MOB_GEAR.getDefault()),
                "12.D01_enableMobGear default must be true");
        check(errors, cases, Boolean.TRUE.equals(HbmConfig.ENABLE_MOB_WEAPONS.getDefault()),
                "12.D02_enableMobWeapons default must be true");
        check(errors, cases, HbmConfig.MOB_WEAPON_SOOT_REDUCTION.getDefault() == 0.0D,
                "12.D03_mobWeaponSootReduction default must be 0");
    }

    private static void pool(List<String> errors, int[] cases, String name, String... expected) {
        List<String> actual = LegacyMobEquipment.describeGunPool(name);
        check(errors, cases, actual.equals(List.of(expected)), name + " is " + actual + ", expected " + List.of(expected));
        for (String entry : expected) {
            String id = entry.substring(0, entry.indexOf('='));
            check(errors, cases, item(id) instanceof ItemGunBaseNT, name + " entry " + id + " is not a registered sedna gun");
        }
    }

    private static void gunPools(List<String> errors, int[] cases) {
        pool(errors, cases, "slotPoolGuns[0.3]", "gun_light_revolver=16", "gun_greasegun=8", "gun_maresleg=2");
        pool(errors, cases, "slotPoolGuns[1]", "gun_light_revolver=6", "gun_greasegun=8", "gun_maresleg=4", "gun_henry=6");
        pool(errors, cases, "slotPoolGuns[3]", "gun_uzi=10", "gun_maresleg=8", "gun_henry=12", "gun_heavy_revolver=4",
                "gun_flaregun=2");
        pool(errors, cases, "slotPoolGuns[5]", "gun_am180=6", "gun_uzi=10", "gun_spas12=8", "gun_henry_lincoln=2",
                "gun_heavy_revolver=12", "gun_flaregun=4", "gun_flamer=2");
        pool(errors, cases, "slotPoolGunsTier1", "gun_light_revolver=16", "gun_greasegun=8", "gun_maresleg=2",
                "gun_flaregun=1");
        pool(errors, cases, "slotPoolGunsTier2", "gun_uzi=12", "gun_maresleg=8", "gun_henry=12", "gun_heavy_revolver=8",
                "gun_flaregun=4", "gun_star_f=8");
        pool(errors, cases, "slotPoolGunsTier3", "gun_g3=25", "gun_spas12=20", "gun_carbine=15", "gun_star_f=20",
                "gun_am180=6", "gun_amat=5");
        check(errors, cases, LegacyMobEquipment.SKELEGUN_EMPTY_WEIGHT == 50, "bow replacement pool: null weight must be 50");
        check(errors, cases, LegacyMobEquipment.PEPPERBOX_WEIGHT == 5 && LegacyMobEquipment.PEPPERBOX_EMPTY_WEIGHT == 20,
                "pepperbox pool must be pepperbox 5 / null 20");
        check(errors, cases, item("gun_pepperbox") instanceof ItemGunBaseNT, "gun_pepperbox is not a registered sedna gun");
    }

    private static void slots(List<String> errors, int[] cases, String name, EquipmentSlot slot, String... expected) {
        List<String> actual = LegacyMobEquipment.describeSlotPool(name, slot);
        List<String> wanted = expected.length == 0 ? null : List.of(expected);
        check(errors, cases, java.util.Objects.equals(actual, wanted), name + "/" + slot + " is " + actual + ", expected " + wanted);
    }

    private static void armourPools(List<String> errors, int[] cases) {
        slots(errors, cases, "slotPoolRangedS", EquipmentSlot.HEAD, "null=8000", "gas_mask_m65=16", "gas_mask_olde=12",
                "mask_of_infamy=8", "gas_mask_mono=8", "robes_helmet=32", "no9=16", "rag_piss=1", "goggles=1",
                "titanium_helmet=4", "steel_helmet=8");
        slots(errors, cases, "slotPoolRangedS", EquipmentSlot.CHEST, "null=7000", "starmetal_plate=1", "cobalt_plate=2",
                "steel_plate=8", "titanium_plate=4");
        slots(errors, cases, "slotPoolRangedS", EquipmentSlot.LEGS, "null=7000", "zirconium_legs=1", "cobalt_legs=2",
                "steel_legs=16", "titanium_legs=8", "robes_legs=32");
        slots(errors, cases, "slotPoolRangedS", EquipmentSlot.FEET, "null=10000", "robes_boots=32", "steel_boots=16",
                "cobalt_boots=2", "titanium_boots=6");
        slots(errors, cases, "slotPoolRangedS", EquipmentSlot.MAINHAND);
        slots(errors, cases, "slotPoolCommonS", EquipmentSlot.HEAD, "null=8000", "gas_mask_m65=16", "gas_mask_olde=12",
                "mask_of_infamy=8", "gas_mask_mono=8", "robes_helmet=32", "no9=16", "cobalt_helmet=2", "rag_piss=1",
                "hat=1", "titanium_helmet=4", "steel_helmet=8");
        slots(errors, cases, "slotPoolCommonS", EquipmentSlot.CHEST, "null=7000", "starmetal_plate=1", "cobalt_plate=2",
                "robes_plate=32", "jackt=32", "jackt2=32", "steel_plate=2");
        slots(errors, cases, "slotPoolCommonS", EquipmentSlot.LEGS, "null=7000", "zirconium_legs=1", "cobalt_legs=2",
                "steel_legs=16", "titanium_legs=8", "robes_legs=32");
        slots(errors, cases, "slotPoolCommonS", EquipmentSlot.FEET, "null=7000", "robes_boots=32", "steel_boots=16",
                "cobalt_boots=2");
        // ModItems.pipe_lead is registered as weapon_pipe_lead in the port.
        slots(errors, cases, "slotPoolCommonS", EquipmentSlot.MAINHAND, "null=10000", "weapon_pipe_lead=30", "crowbar=25",
                "geiger_counter=20", "reer_graar=16", "steel_pickaxe=12", "stopsign=10", "sopsign=8", "chernobylsign=6",
                "steel_sword=15", "titanium_sword=8", "lead_gavel=4", "wrench_flipped=2", "wrench=20");
        slots(errors, cases, "slotPoolRanged", EquipmentSlot.CHEST, "null=10", "starmetal_plate=1", "cobalt_plate=2",
                "steel_plate=8", "titanium_plate=4");
        slots(errors, cases, "slotPoolRanged", EquipmentSlot.MAINHAND);
        slots(errors, cases, "slotPoolAdvRanged", EquipmentSlot.MAINHAND);
        slots(errors, cases, "slotPoolAdvRanged", EquipmentSlot.HEAD, "null=0", "security_helmet=10", "t51_helmet=4",
                "asbestos_helmet=12", "liquidator_helmet=4", "no9=12", "hazmat_helmet=6");
    }

    private static void skelegun(List<String> errors, int[] cases) {
        String[][] branches = {
                {"-1", "pepperbox"}, {"0.1", "pepperbox"}, {"0.5", "slotPoolGuns[0.3]"}, {"1", "slotPoolGuns[1]"},
                {"2.9", "slotPoolGuns[1]"}, {"3", "slotPoolGuns[3]"}, {"4.9", "slotPoolGuns[3]"}, {"5", "slotPoolGuns[5]"},
                {"100", "slotPoolGuns[5]"},
        };
        for (String[] branch : branches) {
            String actual = LegacyMobEquipment.describeSkelegunBranch(Float.parseFloat(branch[0]));
            check(errors, cases, actual.equals(branch[1]), "getSkelegun branch at soot " + branch[0] + " is " + actual
                    + ", expected " + branch[1]);
        }

        if (!HbmConfig.ENABLE_MOB_WEAPONS.get() || HbmConfig.MOB_WEAPON_SOOT_REDUCTION.get() != 0.0D) {
            // The roll checks below are written for the default configuration.
            return;
        }
        RandomSource random = RandomSource.create(0xE1L);
        boolean noneAtZero = true;
        for (int i = 0; i < 200; i++) {
            // log(0) * 0.25 is -Infinity: every roll fails.
            if (LegacyMobEquipment.getSkelegun(0.0F, random) != null) noneAtZero = false;
        }
        check(errors, cases, noneAtZero, "getSkelegun must never yield a gun at 0 soot");

        Set<String> high = Set.of("gun_am180", "gun_uzi", "gun_spas12", "gun_henry_lincoln", "gun_heavy_revolver",
                "gun_flaregun", "gun_flamer");
        boolean allHigh = true;
        for (int i = 0; i < 200; i++) {
            // log(1000) * 0.25 > 1: every roll passes.
            String gun = LegacyMobEquipment.getSkelegun(1000.0F, random);
            if (gun == null || !high.contains(gun)) allHigh = false;
        }
        check(errors, cases, allHigh, "getSkelegun at 1000 soot must always draw from slotPoolGuns[5]");

        // log(e^2) * 0.25 = 0.5: about half of the rolls pass, all from slotPoolGuns[5] (soot > 5).
        int passed = 0;
        for (int i = 0; i < 2000; i++) {
            if (LegacyMobEquipment.getSkelegun((float) Math.exp(2.0D), random) != null) passed++;
        }
        check(errors, cases, passed > 850 && passed < 1150, "getSkelegun at e^2 soot passed " + passed + " of 2000, expected ~1000");
    }

    private static void goalDefaults(ServerLevel level, List<String> errors, int[] cases) {
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (skeleton == null) {
            check(errors, cases, false, "could not create a skeleton");
            return;
        }
        LegacyFireGunGoal goal = new LegacyFireGunGoal(skeleton);
        check(errors, cases, goal.attackMoveSpeed == 1.0D, "EntityAIFireGun.attackMoveSpeed default 1.0");
        check(errors, cases, goal.maxRange == 20.0D, "EntityAIFireGun.maxRange default 20");
        check(errors, cases, goal.burstTime == 10, "EntityAIFireGun.burstTime default 10");
        check(errors, cases, goal.minWait == 10, "EntityAIFireGun.minWait default 10");
        check(errors, cases, goal.maxWait == 40, "EntityAIFireGun.maxWait default 40");
        check(errors, cases, goal.inaccuracy == 30.0F, "EntityAIFireGun.inaccuracy default 30");
        check(errors, cases, goal.randomBurst, "EntityAIFireGun.randomBurst default true");
        check(errors, cases, goal.getFlags().isEmpty(), "EntityAIFireGun had mutex bits 0: the goal must claim no flags");
        check(errors, cases, goal.requiresUpdateEveryTick(), "EntityAIFireGun.updateTask ran every tick");
        check(errors, cases, !goal.canUse(), "fire goal must not start without target and gun");
    }

    private static void forcedWeapon(ServerLevel level, List<String> errors, int[] cases) {
        Skeleton none = EntityType.SKELETON.create(level);
        Skeleton armed = EntityType.SKELETON.create(level);
        if (none == null || armed == null) {
            check(errors, cases, false, "could not create a skeleton");
            return;
        }
        for (int i = 0; i < 200; i++) {
            LegacyMobEquipment.assignSootRangedSkeleton(none, (String) null);
        }
        check(errors, cases, none.getMainHandItem().isEmpty(), "skeleton without bow replacement received "
                + none.getMainHandItem());
        check(errors, cases, !LegacyMobEquipment.hasFireTask(none), "skeleton without gun must not receive the fire task");

        // 1 : 50 per decoration, as in createSlotPool(50, {{bowReplacement, 1}}).
        int tries = 0;
        while (armed.getMainHandItem().isEmpty() && tries < 20000) {
            LegacyMobEquipment.assignSootRangedSkeleton(armed, "gun_uzi");
            tries++;
        }
        check(errors, cases, armed.getMainHandItem().is(item("gun_uzi")), "forced bow replacement did not arrive within "
                + tries + " decorations");
        check(errors, cases, LegacyMobEquipment.hasFireTask(armed), "armed skeleton must receive the fire task");
        check(errors, cases, firePriority(armed) == 3, "fire task priority is " + firePriority(armed) + ", expected 3");
        check(errors, cases, mainHandDropChance(armed) == 0.0F, "addFireTask must zero the held-item drop chance");

        LegacyMobEquipment.addFireTask(armed);
        LegacyMobEquipment.addFireTask(armed, new LegacyFireGunGoal(armed));
        check(errors, cases, fireTaskCount(armed) == 1, "fire tasks must not stack: " + fireTaskCount(armed));
    }

    private static void joinHandler(ServerLevel level, List<String> errors, int[] cases) {
        Zombie armed = EntityType.ZOMBIE.create(level);
        Zombie unarmed = EntityType.ZOMBIE.create(level);
        if (armed == null || unarmed == null) {
            check(errors, cases, false, "could not create a zombie");
            return;
        }
        armed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item("gun_light_revolver")));
        unarmed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item("steel_sword")));
        check(errors, cases, !LegacyMobEquipment.hasFireTask(armed), "equipping alone must not add the fire task to a zombie");
        LegacyMobSpawnEvents.addLegacyAiTasks(new EntityJoinLevelEvent(armed, level));
        LegacyMobSpawnEvents.addLegacyAiTasks(new EntityJoinLevelEvent(unarmed, level));
        check(errors, cases, LegacyMobEquipment.hasFireTask(armed) && firePriority(armed) == 3,
                "addAITasks: any mob joining with a gun receives the fire task at priority 3");
        check(errors, cases, mainHandDropChance(armed) == 0.0F, "addAITasks must zero the held-item drop chance");
        check(errors, cases, !LegacyMobEquipment.hasFireTask(unarmed), "addAITasks: no fire task without a gun");
        LegacyMobSpawnEvents.addLegacyAiTasks(new EntityJoinLevelEvent(armed, level));
        check(errors, cases, fireTaskCount(armed) == 1, "addAITasks must not stack fire tasks on a re-join");
    }

    private static void logicTiers(ServerLevel level, List<String> errors, int[] cases) {
        Set<String> tier1 = Set.of("gun_light_revolver", "gun_greasegun", "gun_maresleg", "gun_flaregun");
        Set<String> tier2 = Set.of("gun_uzi", "gun_maresleg", "gun_henry", "gun_heavy_revolver", "gun_flaregun", "gun_star_f");
        Set<String> tier3 = Set.of("gun_g3", "gun_spas12", "gun_carbine", "gun_star_f", "gun_am180", "gun_amat");
        List<Set<String>> guns = List.of(tier1, tier2, tier3);
        for (int tier = 1; tier <= 3; tier++) {
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton == null) {
                check(errors, cases, false, "could not create a skeleton");
                return;
            }
            LegacyMobEquipment.assignLogicSkeletonTier(skeleton, tier);
            String held = BuiltInRegistries.ITEM.getKey(skeleton.getMainHandItem().getItem()).getPath();
            check(errors, cases, guns.get(tier - 1).contains(held), "SKELETON_GUN_TIER_" + tier + " holds " + held);
            LegacyFireGunGoal goal = LegacyMobEquipment.getFireTask(skeleton);
            check(errors, cases, goal != null && fireTaskCount(skeleton) == 1 && firePriority(skeleton) == 3,
                    "SKELETON_GUN_TIER_" + tier + " must carry exactly one fire task at priority 3");
            if (goal == null) continue;
            boolean ok = tier == 1
                    ? goal.minWait == 10 && goal.maxWait == 40 && goal.maxRange == 20.0D && goal.burstTime == 10
                            && goal.inaccuracy == 30.0F && goal.randomBurst
                    : goal.minWait == 4 && goal.maxWait == 5 && goal.maxRange == (tier == 2 ? 50.0D : 100.0D)
                            && goal.burstTime == 6 && goal.inaccuracy == (tier == 2 ? 5.0F : 1.0F) && !goal.randomBurst;
            check(errors, cases, ok, "SKELETON_GUN_TIER_" + tier + " fire task tuning differs from LogicBlockActions");
        }
    }

    /** One goal tick against a visible target: the gun code must accept a mob without inventory. */
    private static void goalSmoke(ServerLevel level, List<String> errors, int[] cases) {
        Skeleton skeleton = EntityType.SKELETON.create(level);
        Zombie target = EntityType.ZOMBIE.create(level);
        if (skeleton == null || target == null) {
            check(errors, cases, false, "could not create the smoke-test mobs");
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        if (!level.isLoaded(spawn)) {
            return;
        }
        double y = level.getMaxBuildHeight() - 8;
        skeleton.moveTo(spawn.getX() + 0.5D, y, spawn.getZ() + 0.5D, 0.0F, 0.0F);
        target.moveTo(spawn.getX() + 0.5D, y, spawn.getZ() + 3.5D, 0.0F, 0.0F);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item("gun_light_revolver")));
        LegacyFireGunGoal goal = new LegacyFireGunGoal(skeleton);
        check(errors, cases, !goal.canUse(), "fire goal must not start without a target");
        skeleton.setTarget(target);
        check(errors, cases, goal.canUse() && goal.canContinueToUse(), "fire goal must run with a target and a gun in hand");
        try {
            goal.tick();
            check(errors, cases, true, "");
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity E1 fire goal tick crashed", e);
            check(errors, cases, false, "fire goal tick crashed for a mob without inventory: " + e);
        }
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item("steel_sword")));
        check(errors, cases, !goal.canUse(), "fire goal must stop once the held item is not a gun");
        skeleton.discard();
        target.discard();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static int fireTaskCount(Mob mob) {
        int count = 0;
        for (WrappedGoal task : mob.goalSelector.getAvailableGoals()) {
            if (task.getGoal() instanceof LegacyFireGunGoal) count++;
        }
        return count;
    }

    private static int firePriority(Mob mob) {
        for (WrappedGoal task : mob.goalSelector.getAvailableGoals()) {
            if (task.getGoal() instanceof LegacyFireGunGoal) return task.getPriority();
        }
        return -1;
    }

    private static float mainHandDropChance(Mob mob) {
        CompoundTag tag = mob.saveWithoutId(new CompoundTag());
        ListTag chances = tag.getList("HandDropChances", Tag.TAG_FLOAT);
        return chances.isEmpty() ? Float.NaN : chances.getFloat(0);
    }
}
