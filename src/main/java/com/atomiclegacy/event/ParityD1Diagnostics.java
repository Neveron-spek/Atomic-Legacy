package com.atomiclegacy.event;

import java.util.List;
import java.util.Set;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBossProjectileEntity;
import com.atomiclegacy.entity.LegacyCreeperEntity;
import com.atomiclegacy.entity.LegacyFbiEntity;
import com.atomiclegacy.entity.LegacyPigeonEntity;
import com.atomiclegacy.entity.LegacyQuackosEntity;
import com.atomiclegacy.entity.LegacyRadBeastEntity;
import com.atomiclegacy.entity.LegacyUndeadSoldierEntity;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * [parity D1: diagnostics] AI and parameters of the remaining 1.7.10 mobs (creepers, crabs, FBI agent and drone,
 * RAD beast, ghost, maggot, pigeon, dummy, undead soldier, Quackos) and the a2 leftovers: NPC bullet velocity
 * multipliers, penetration and the chopper / cyber crab EntityBullet flight.
 */
final class ParityD1Diagnostics {
    private ParityD1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            attributes(level, problems, cases);
            creepers(level, problems, cases);
            goalsAndGear(level, problems, cases);
            projectileTable(problems, cases);
            projectileFlight(level, problems, cases);
        } catch (Exception exception) {
            problems.add("parity d1: diagnostics threw " + exception);
            AtomicLegacy.LOGGER.error("parity d1 diagnostics", exception);
        }
        return cases[0];
    }

    private static void attributes(ServerLevel level, List<String> problems, int[] cases) {
        mob(level, problems, cases, HbmEntityTypes.NUCLEAR_CREEPER.get(), 50.0D, 0.3D, -1.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.TAINTED_CREEPER.get(), 15.0D, 0.35D, -1.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.PHOSGENE_CREEPER.get(), 20.0D, 0.25D, -1.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.VOLATILE_CREEPER.get(), 20.0D, 0.25D, -1.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.GOLD_CREEPER.get(), 20.0D, 0.25D, -1.0D, 5);
        // the crab is removed before the death animation, so it never awards experience
        mob(level, problems, cases, HbmEntityTypes.CYBER_CRAB.get(), 4.0D, 0.75D, -1.0D, 0);
        mob(level, problems, cases, HbmEntityTypes.TESLA_CRAB.get(), 10.0D, 0.5D, -1.0D, 0);
        mob(level, problems, cases, HbmEntityTypes.TAINT_CRAB.get(), 25.0D, 0.5D, -1.0D, 0);
        mob(level, problems, cases, HbmEntityTypes.FBI.get(), 20.0D, 0.3D, -1.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.FBI_DRONE.get(), 35.0D, -1.0D, -1.0D, 0);
        mob(level, problems, cases, HbmEntityTypes.RAD_BEAST.get(), 120.0D, 0.10000000149011612D, 16.0D, 30);
        mob(level, problems, cases, HbmEntityTypes.GHOST.get(), 8.0D, 0.2D, -1.0D, 0);
        mob(level, problems, cases, HbmEntityTypes.PARASITE_MAGGOT.get(), 8.0D, 1.0D, 2.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.BLOCK_SPIDER.get(), 10.0D, 1.0D, -1.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.PIGEON.get(), 20.0D, 0.699999988079071D, -1.0D, 0);
        mob(level, problems, cases, HbmEntityTypes.UNDEAD_SOLDIER.get(), 20.0D, 0.25D, 5.0D, 5);
        mob(level, problems, cases, HbmEntityTypes.QUACKOS.get(), 4.0D, 0.25D, -1.0D, -1);
        mob(level, problems, cases, HbmEntityTypes.DUCK.get(), 4.0D, 0.25D, -1.0D, -1);
        mob(level, problems, cases, HbmEntityTypes.DUMMY.get(), 20.0D, -1.0D, -1.0D, 0);

        // old (non-task) AI mobs: getAIMoveSpeed() is the constant 0.1
        for (EntityType<? extends Mob> type : List.of(HbmEntityTypes.RAD_BEAST.get(), HbmEntityTypes.GHOST.get(),
                HbmEntityTypes.PARASITE_MAGGOT.get())) {
            Mob mob = type.create(level);
            check(problems, cases, mob != null && mob.getSpeed() == 0.1F, "old-AI move speed of " + EntityType.getKey(type));
        }
        check(problems, cases, HbmEntityTypes.FBI.get().fireImmune() && HbmEntityTypes.RAD_BEAST.get().fireImmune(),
                "FBI / RAD beast fire immunity");
        size(problems, cases, HbmEntityTypes.CYBER_CRAB.get(), 0.75F, 0.35F);
        size(problems, cases, HbmEntityTypes.TESLA_CRAB.get(), 0.75F, 1.25F);
        size(problems, cases, HbmEntityTypes.TAINT_CRAB.get(), 1.25F, 1.25F);
        size(problems, cases, HbmEntityTypes.QUACKOS.get(), 7.5F, 17.5F);
        size(problems, cases, HbmEntityTypes.PIGEON.get(), 0.5F, 1.0F);
        size(problems, cases, HbmEntityTypes.PLASTIC_BAG.get(), 0.45F, 0.45F);
        size(problems, cases, HbmEntityTypes.PARASITE_MAGGOT.get(), 0.3F, 0.7F);
        size(problems, cases, HbmEntityTypes.BLOCK_SPIDER.get(), 0.95F, 1.25F);

        LegacyRadBeastEntity leader = HbmEntityTypes.RAD_BEAST.get().create(level);
        if (leader != null) {
            leader.makeLeader();
            check(problems, cases, leader.getMaxHealth() == 360.0F && leader.getHealth() == 360.0F
                            && leader.getItemBySlot(EquipmentSlot.MAINHAND).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AtomicLegacy.id("coin_radiation"))),
                    "RAD beast leader 360 hp + coin_radiation");
        }
        Mob soldier = HbmEntityTypes.UNDEAD_SOLDIER.get().create(level);
        check(problems, cases, soldier != null && soldier.getAttributeValue(Attributes.FOLLOW_RANGE) == 40.0D,
                "undead soldier follow range 40");
        Mob fbi = HbmEntityTypes.FBI.get().create(level);
        check(problems, cases, fbi != null && fbi.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.5D,
                "FBI knockback resistance 0.5");
        Mob dummy = HbmEntityTypes.DUMMY.get().create(level);
        if (dummy != null) {
            dummy.setHealth(19.96F);
            check(problems, cases, "19.9 / 20.0".equals(dummy.getDisplayName().getString()),
                    "dummy name truncation: " + dummy.getDisplayName().getString());
        }
        try {
            LegacyQuackosEntity.class.getDeclaredMethod("checkDespawn");
            check(problems, cases, false, "Quackos must not override checkDespawn (no natural despawn in 1.7.10)");
        } catch (NoSuchMethodException expected) {
            check(problems, cases, true, "");
        }
        for (String method : List.of("causeFallDamage", "isIgnoringBlockTriggers")) {
            boolean declared = false;
            for (java.lang.reflect.Method m : LegacyPigeonEntity.class.getDeclaredMethods()) {
                declared |= m.getName().equals(method);
            }
            check(problems, cases, !declared, "pigeon must not override " + method);
        }
    }

    private static void creepers(ServerLevel level, List<String> problems, int[] cases) {
        fuse(level, problems, cases, HbmEntityTypes.NUCLEAR_CREEPER.get(), 75);
        fuse(level, problems, cases, HbmEntityTypes.PHOSGENE_CREEPER.get(), 20);
        fuse(level, problems, cases, HbmEntityTypes.TAINTED_CREEPER.get(), 30);
        fuse(level, problems, cases, HbmEntityTypes.VOLATILE_CREEPER.get(), 30);
        fuse(level, problems, cases, HbmEntityTypes.GOLD_CREEPER.get(), 30);

        LegacyCreeperEntity nuclear = HbmEntityTypes.NUCLEAR_CREEPER.get().create(level);
        LegacyCreeperEntity tainted = HbmEntityTypes.TAINTED_CREEPER.get().create(level);
        LegacyCreeperEntity phosgene = HbmEntityTypes.PHOSGENE_CREEPER.get().create(level);
        if (nuclear == null || tainted == null || phosgene == null) {
            check(problems, cases, false, "creeper instances");
            return;
        }
        nuclear.setHealth(10.0F);
        boolean nuclearHurt = nuclear.hurt(level.damageSources().source(HbmDamageTypes.RADIATION), 2.0F);
        check(problems, cases, !nuclearHurt && nuclear.getHealth() == 12.0F,
                "nuclear creeper heals from radiation: " + nuclear.getHealth());
        tainted.setHealth(10.0F);
        tainted.hurt(level.damageSources().source(HbmDamageTypes.RADIATION), 2.0F);
        check(problems, cases, tainted.getHealth() < 10.0F, "tainted creeper takes radiation damage: " + tainted.getHealth());
        // phosgene: -4 unless the source bypasses armour; a negative remainder is rejected
        check(problems, cases, !phosgene.hurt(level.damageSources().cactus(), 3.0F) && phosgene.getHealth() == 20.0F,
                "phosgene creeper ignores 3 blockable damage");
        phosgene.invulnerableTime = 0;
        check(problems, cases, phosgene.hurt(level.damageSources().cactus(), 6.0F) && phosgene.getHealth() == 18.0F,
                "phosgene creeper takes 6 - 4 damage: " + phosgene.getHealth());
        phosgene.invulnerableTime = 0;
        check(problems, cases, phosgene.hurt(level.damageSources().magic(), 3.0F) && phosgene.getHealth() == 15.0F,
                "phosgene creeper takes unblockable damage in full: " + phosgene.getHealth());
        // the vanilla 30-tick fuse must stay disarmed while the vanilla swell direction is preserved for the AI
        nuclear.setSwellDir(1);
        check(problems, cases, nuclear.getSwellDir() == 1, "creeper keeps the AI swell direction");
    }

    private static void goalsAndGear(ServerLevel level, List<String> problems, int[] cases) {
        LegacyFbiEntity fbi = HbmEntityTypes.FBI.get().create(level);
        if (fbi != null) {
            int ranged = -1;
            int melee = -1;
            for (WrappedGoal goal : fbi.goalSelector.getAvailableGoals()) {
                if (goal.getGoal() instanceof RangedAttackGoal) ranged = goal.getPriority();
                if (goal.getGoal() instanceof MeleeAttackGoal) melee = goal.getPriority();
            }
            check(problems, cases, ranged == 2 && melee == 3, "FBI arrow-attack task 2 above melee task 3: " + ranged + "/" + melee);
            fbi.setPos(0.5D, 300.0D, 0.5D);
            fbi.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(0, 300, 0)), MobSpawnType.MOB_SUMMONED, null);
            Item held = fbi.getItemBySlot(EquipmentSlot.MAINHAND).getItem();
            check(problems, cases, held == HbmItems.GUN_HEAVY_REVOLVER.get() || held == HbmItems.GUN_SPAS12.get(),
                    "FBI holds gun_heavy_revolver or gun_spas12: " + held);
        }
        LegacyUndeadSoldierEntity soldier = HbmEntityTypes.UNDEAD_SOLDIER.get().create(level);
        if (soldier != null) {
            soldier.setPos(0.5D, 300.0D, 0.5D);
            soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(0, 300, 0)), MobSpawnType.MOB_SUMMONED, null);
            Set<Item> guns = Set.of(HbmItems.GUN_HEAVY_REVOLVER.get(), HbmItems.GUN_LIGHT_REVOLVER.get(),
                    HbmItems.GUN_CARBINE.get(), HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_GREASEGUN.get());
            check(problems, cases, guns.contains(soldier.getItemBySlot(EquipmentSlot.MAINHAND).getItem())
                            && soldier.getItemBySlot(EquipmentSlot.HEAD).is(HbmItems.TAURUN_HELMET.get()),
                    "undead soldier gear: " + soldier.getItemBySlot(EquipmentSlot.MAINHAND));
        }
        LegacyPigeonEntity pigeon = HbmEntityTypes.PIGEON.get().create(level);
        if (pigeon != null) {
            boolean bread = false;
            for (WrappedGoal goal : pigeon.goalSelector.getAvailableGoals()) {
                bread |= goal.getPriority() == 2 && goal.getGoal().getClass().getSimpleName().equals("EatBreadGoal");
            }
            check(problems, cases, bread, "pigeon EatBread task at priority 2");
        }
        Mob crab = HbmEntityTypes.CYBER_CRAB.get().create(level);
        check(problems, cases, crab != null
                        && crab.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER) == -1.0F,
                "cyber crab avoids water");
        for (String texture : List.of("textures/models/emplacer.png", "textures/models/tau.png")) {
            ResourceLocation id = AtomicLegacy.id(texture);
            check(problems, cases, ParityD1Diagnostics.class.getResource(
                    "/assets/" + id.getNamespace() + "/" + id.getPath()) != null, "missing " + id);
        }
        check(problems, cases, ParityD1Diagnostics.class.getResource(
                "/assets/atomiclegacy/models/weapons/meteor.obj") != null, "missing models/weapons/meteor.obj");
    }

    private static void projectileTable(List<String> problems, int[] cases) {
        velocity(problems, cases, LegacyBossProjectileEntity.Type.WORM_BOLT, 5.0D, true, false);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.WORM_LASER, 5.0D, true, false);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.UFO_LASER, 5.0D, true, false);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.UFO_ROCKET, 2.0D, false, true);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.CHOPPER_BULLET, 1.0D, false, false);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_ORB, 0.25D, false, false);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_TRACER, 5.0D, true, true);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_ROCKET, 1.0D, false, false);
        velocity(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_METEOR, 1.0D, false, false);
        LegacyBossProjectileEntity.Type tau = LegacyBossProjectileEntity.Type.CRAB_TAU;
        check(problems, cases, tau.projectileSpeed() == 1.6D && tau.minDamage() == 3.0F && tau.maxDamage() == 3.0F
                && tau.ordinal() == 9, "CRAB_TAU 1.6 / 3 damage, appended after the saved ordinals");
        check(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_TRACER.trailParticle() != null
                && LegacyBossProjectileEntity.Type.MASKMAN_METEOR.trailParticle() != null
                && LegacyBossProjectileEntity.Type.MASKMAN_ORB.trailParticle() == null, "vPFX trails");
    }

    private static void projectileFlight(ServerLevel level, List<String> problems, int[] cases) {
        Pig owner = pig(level, new Vec3(0.5D, 300.0D, -3.5D));
        Pig victim = pig(level, new Vec3(0.5D, 300.0D, 6.5D));
        if (owner == null || victim == null) {
            check(problems, cases, false, "projectile test pigs");
            return;
        }
        try {
            Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);
            // penetrating tracer: 1.0 heading * velocity 5 crosses the pig in one tick and keeps flying
            LegacyBossProjectileEntity tracer = new LegacyBossProjectileEntity(level, owner, new Vec3(0.5D, 300.45D, 2.5D),
                    forward, LegacyBossProjectileEntity.Type.MASKMAN_TRACER, null);
            float before = victim.getHealth();
            tracer.tick();
            check(problems, cases, victim.getHealth() < before && !tracer.isRemoved() && Math.abs(tracer.getZ() - 7.5D) < 1.0E-6D,
                    "tracer pierces entities at 5 blocks/tick: z=" + tracer.getZ() + " removed=" + tracer.isRemoved());
            tracer.discard();

            // chopper bullet: 3 blocks/tick, damages on the second tick and is not consumed by the hit
            victim.setHealth(1000.0F);
            victim.invulnerableTime = 0;
            LegacyBossProjectileEntity bullet = new LegacyBossProjectileEntity(level, owner, new Vec3(0.5D, 300.45D, 2.5D),
                    forward, LegacyBossProjectileEntity.Type.CHOPPER_BULLET, null);
            bullet.tick();
            bullet.tick();
            check(problems, cases, victim.getHealth() < 1000.0F && !bullet.isRemoved() && bullet.getZ() > 8.0D,
                    "chopper bullet flies through entities: z=" + bullet.getZ() + " removed=" + bullet.isRemoved());
            bullet.discard();

            // cyber crab tau round: same EntityBullet flight at 1.6 blocks/tick, armour-bypassing tau damage of 3
            victim.setHealth(1000.0F);
            victim.invulnerableTime = 0;
            LegacyBossProjectileEntity tau = new LegacyBossProjectileEntity(level, owner, new Vec3(0.5D, 300.45D, 5.0D),
                    forward, LegacyBossProjectileEntity.Type.CRAB_TAU, null);
            tau.tick();
            check(problems, cases, victim.getHealth() == 997.0F && !tau.isRemoved(),
                    "crab tau round: 3 damage, not consumed: " + victim.getHealth());
            tau.discard();

            // worm bolt: 0.5 heading * velocity 5 = 2.5 blocks per tick
            LegacyBossProjectileEntity bolt = new LegacyBossProjectileEntity(level, owner, new Vec3(8.5D, 300.45D, 2.5D),
                    forward, LegacyBossProjectileEntity.Type.WORM_BOLT, null);
            bolt.tick();
            check(problems, cases, Math.abs(bolt.getZ() - 5.0D) < 1.0E-6D, "worm bolt 2.5 blocks/tick: z=" + bolt.getZ());
            bolt.discard();

            // orb: 2.0 heading * velocity 0.25 = 0.5 blocks per tick
            LegacyBossProjectileEntity orb = new LegacyBossProjectileEntity(level, owner, new Vec3(8.5D, 300.45D, 2.5D),
                    forward, LegacyBossProjectileEntity.Type.MASKMAN_ORB, null);
            orb.tick();
            check(problems, cases, Math.abs(orb.getZ() - 3.0D) < 1.0E-6D && !orb.isRemoved(), "orb 0.5 blocks/tick: z=" + orb.getZ());
            orb.discard();
        } finally {
            owner.discard();
            victim.discard();
        }
    }

    private static Pig pig(ServerLevel level, Vec3 pos) {
        Pig pig = EntityType.PIG.create(level);
        if (pig == null) return null;
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D);
        pig.setHealth(1000.0F);
        pig.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(pig);
        return pig;
    }

    private static void velocity(List<String> problems, int[] cases, LegacyBossProjectileEntity.Type type,
                                 double multiplier, boolean penetrates, boolean ricochets) {
        check(problems, cases, type.velocityMultiplier() == multiplier && type.penetrates() == penetrates
                        && type.ricochets() == ricochets,
                "projectile " + type + " velocity/penetration/ricochet (" + type.velocityMultiplier() + ", "
                        + type.penetrates() + ", " + type.ricochets() + ")");
    }

    private static void fuse(ServerLevel level, List<String> problems, int[] cases,
                             EntityType<? extends LegacyCreeperEntity> type, int ticks) {
        LegacyCreeperEntity creeper = type.create(level);
        check(problems, cases, creeper != null && creeper.legacyFuseTicks() == ticks,
                "fuse of " + EntityType.getKey(type) + " = " + (creeper == null ? "null" : creeper.legacyFuseTicks()));
    }

    private static void size(List<String> problems, int[] cases, EntityType<?> type, float width, float height) {
        check(problems, cases, type.getWidth() == width && type.getHeight() == height,
                "size of " + EntityType.getKey(type) + " = " + type.getWidth() + "x" + type.getHeight());
    }

    /** Negative expectations are skipped (attribute absent or irrelevant in the original). */
    private static void mob(ServerLevel level, List<String> problems, int[] cases, EntityType<? extends LivingEntity> type,
                            double health, double speed, double attack, int experience) {
        LivingEntity entity = type.create(level);
        if (entity == null) {
            check(problems, cases, false, "cannot create " + EntityType.getKey(type));
            return;
        }
        boolean ok = entity.getAttributeBaseValue(Attributes.MAX_HEALTH) == health;
        if (speed >= 0.0D) ok &= entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) == speed;
        if (attack >= 0.0D) ok &= entity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == attack;
        if (experience >= 0) ok &= entity.getExperienceReward(level, null) == experience;
        check(problems, cases, ok, "attributes of " + EntityType.getKey(type) + ": hp "
                + entity.getAttributeBaseValue(Attributes.MAX_HEALTH) + " speed "
                + entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) + " xp " + entity.getExperienceReward(level, null));
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) problems.add("parity d1: " + message);
    }
}
