package com.atomiclegacy.event;

import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBossProjectileEntity;
import com.atomiclegacy.entity.LegacyChopperEntity;
import com.atomiclegacy.entity.LegacyMaskManEntity;
import com.atomiclegacy.entity.LegacyUfoEntity;
import com.atomiclegacy.entity.LegacyWormBodyEntity;
import com.atomiclegacy.entity.LegacyWormHeadEntity;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * [parity A2] Dedicated-server checks of the boss mobs against the pinned 1.7.10 sources (EntityUFO, EntityHunterChopper,
 * EntityBOTPrime*, EntityWormBaseNT, EntityBurrowingNT, EntityMaskMan + ai/EntityAIMaskman*, GunNPCFactory, ModDamageSource)
 * and, through ParityA2GlyphidChecks, of the glyphids and their hives. Entities are created without being added to the
 * world. Not covered: target scans with players, projectile flight and impacts, pathfinding, sounds, rendering.
 */
final class ParityA2Diagnostics {

    private ParityA2Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            damageTypes(level, problems, cases);
            projectiles(problems, cases);
            ufo(level, problems, cases);
            chopper(level, problems, cases);
            worm(level, problems, cases);
            maskman(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity A2 diagnostics crashed", e);
            problems.add("A2 diagnostics crashed: " + e);
        }
        try {
            ParityA2GlyphidChecks.run(server, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity A2 glyphid diagnostics crashed", e);
            problems.add("A2 glyphid diagnostics crashed: " + e);
        }
        return cases[0];
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String what) {
        cases[0]++;
        if (!ok) problems.add("A2: " + what);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    private static Vec3 testPos(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        return new Vec3(spawn.getX() + 0.5D, 300.0D, spawn.getZ() + 0.5D);
    }

    private static Holder<DamageType> type(ServerLevel level, ResourceKey<DamageType> key) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(key).orElse(null);
    }

    private static void damageTypes(ServerLevel level, List<String> problems, int[] cases) {
        Holder<DamageType> cmb = type(level, HbmDamageTypes.CMB);
        Holder<DamageType> bullet = type(level, HbmDamageTypes.REVOLVER_BULLET);
        Holder<DamageType> emplacer = type(level, HbmDamageTypes.EMPLACER);
        check(problems, cases, cmb != null && "cmb".equals(cmb.value().msgId())
                && cmb.is(DamageTypeTags.BYPASSES_ARMOR) && cmb.is(DamageTypeTags.IS_PROJECTILE), "damage type cmb (projectile, bypasses armor)");
        check(problems, cases, bullet != null && "revolverBullet".equals(bullet.value().msgId())
                && bullet.is(DamageTypeTags.IS_PROJECTILE) && !bullet.is(DamageTypeTags.BYPASSES_ARMOR), "damage type revolverBullet");
        check(problems, cases, emplacer != null && "emplacer".equals(emplacer.value().msgId())
                && emplacer.is(DamageTypeTags.IS_PROJECTILE), "damage type emplacer");
        check(problems, cases, HbmSoundEvents.ENTITY_BOMB_DET.isBound() && HbmSoundEvents.WEAPON_TESLA_SHOOT.isBound(),
                "sounds entity.bombdet / weapon.teslashoot registered");
    }

    private static void projectile(List<String> problems, int[] cases, LegacyBossProjectileEntity.Type type,
                                   double speed, float min, float max) {
        check(problems, cases, near(type.projectileSpeed(), speed) && near(type.minDamage(), min) && near(type.maxDamage(), max),
                "projectile " + type + " speed/damage (" + type.projectileSpeed() + ", " + type.minDamage() + ".." + type.maxDamage() + ")");
    }

    private static void projectiles(List<String> problems, int[] cases) {
        projectile(problems, cases, LegacyBossProjectileEntity.Type.WORM_BOLT, 0.5, 15, 25);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.WORM_LASER, 1.0, 35, 60);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.UFO_LASER, 2.0, 35, 60);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.UFO_ROCKET, 2.0, 10, 15);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.CHOPPER_BULLET, 3.0, 3, 7);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_ORB, 2.0, 100, 100);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_TRACER, 1.0, 15, 20);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_ROCKET, 1.0, 15, 20);
        projectile(problems, cases, LegacyBossProjectileEntity.Type.MASKMAN_METEOR, 1.0, 20, 30);
        check(problems, cases, LegacyBossProjectileEntity.Type.values().length == 10
                && LegacyBossProjectileEntity.Type.CHOPPER_BULLET.ordinal() == 4, "projectile type ordinals kept (saved in NBT)");
    }

    private static void ufo(ServerLevel level, List<String> problems, int[] cases) {
        LegacyUfoEntity ufo = HbmEntityTypes.LEGACY_UFO.get().create(level);
        if (ufo == null) {
            check(problems, cases, false, "ufo not created");
            return;
        }
        Vec3 pos = testPos(level);
        ufo.setPos(pos);
        ufo.setHealth(20000.0F);
        check(problems, cases, near(ufo.getMaxHealth(), 20000) && near(ufo.getHealth(), 20000), "ufo health 20000 not capped");
        check(problems, cases, near(ufo.getBbWidth(), 15) && near(ufo.getBbHeight(), 4) && ufo.fireImmune(), "ufo size 15x4, fire immune");
        // EntityFlying movement: one move per tick with the 0.91 drag, no gravity
        ufo.setDeltaMovement(1.0D, 0.0D, 0.0D);
        ufo.travel(Vec3.ZERO);
        check(problems, cases, near(ufo.getX(), pos.x + 1.0D) && near(ufo.getY(), pos.y) && near(ufo.getDeltaMovement().x, 0.91D),
                "ufo travel: single move, 0.91 drag, no gravity (" + ufo.position() + ", " + ufo.getDeltaMovement() + ")");
        // hurtCooldown 5: the second hit is rejected regardless of vanilla i-frames
        boolean first = ufo.hurt(level.damageSources().generic(), 100.0F);
        ufo.invulnerableTime = 0;
        boolean second = ufo.hurt(level.damageSources().generic(), 100.0F);
        check(problems, cases, first && !second && near(ufo.getHealth(), 19900), "ufo hurt cooldown (" + first + ", " + second + ", " + ufo.getHealth() + ")");
        ufo.discard();
    }

    private static void chopper(ServerLevel level, List<String> problems, int[] cases) {
        LegacyChopperEntity chopper = HbmEntityTypes.LEGACY_CHOPPER.get().create(level);
        if (chopper == null) {
            check(problems, cases, false, "chopper not created");
            return;
        }
        Vec3 pos = testPos(level);
        chopper.setPos(pos);
        chopper.setDeltaMovement(1.0D, 0.0D, 0.0D);
        chopper.travel(Vec3.ZERO);
        check(problems, cases, near(chopper.getX(), pos.x + 1.0D) && near(chopper.getY(), pos.y) && near(chopper.getDeltaMovement().x, 0.91D),
                "chopper travel: single move, 0.91 drag, no gravity (" + chopper.position() + ", " + chopper.getDeltaMovement() + ")");
        chopper.setDeltaMovement(Vec3.ZERO);
        // non-explosive damage is reduced to 10 %
        chopper.hurt(level.damageSources().generic(), 100.0F);
        check(problems, cases, near(chopper.getHealth(), 740), "chopper generic damage x0.1 (" + chopper.getHealth() + ")");
        chopper.invulnerableTime = 0;
        // explosions are taken in full
        chopper.hurt(level.damageSources().explosion(null, null), 100.0F);
        check(problems, cases, near(chopper.getHealth(), 640), "chopper explosion damage x1 (" + chopper.getHealth() + ")");
        chopper.invulnerableTime = 0;
        // entity-caused damage is ignored (EntityDamageSource guard)
        LivingEntity attacker = HbmEntityTypes.LEGACY_CHOPPER.get().create(level);
        if (attacker != null) {
            boolean hit = chopper.hurt(level.damageSources().mobAttack(attacker), 100.0F);
            check(problems, cases, !hit && near(chopper.getHealth(), 640), "chopper ignores entity damage");
            attacker.discard();
        }
        // lethal damage starts the crash instead of killing
        boolean lethal = chopper.hurt(level.damageSources().explosion(null, null), 10000.0F);
        check(problems, cases, !lethal && chopper.isCrashing() && near(chopper.getHealth(), 0.1) && chopper.isAlive(),
                "chopper lethal damage -> crash at 0.1 health");
        chopper.discard();
    }

    private static void worm(ServerLevel level, List<String> problems, int[] cases) {
        LegacyWormHeadEntity head = HbmEntityTypes.LEGACY_WORM_HEAD.get().create(level);
        LegacyWormBodyEntity body = HbmEntityTypes.LEGACY_WORM_BODY.get().create(level);
        if (head == null || body == null) {
            check(problems, cases, false, "bot prime not created");
            return;
        }
        Vec3 pos = testPos(level);
        head.setPos(pos);
        body.setPos(pos.add(0.0D, 5.0D, 0.0D));
        check(problems, cases, LegacyWormHeadEntity.SEGMENT_COUNT == 74 && LegacyWormHeadEntity.ATTACK_RANGE == 150.0D
                && LegacyWormHeadEntity.MAX_SPEED == 1.0D && LegacyWormHeadEntity.FALL_SPEED == 0.006D
                && LegacyWormHeadEntity.SURFACE_Y == 60, "bot prime head constants");
        check(problems, cases, LegacyWormBodyEntity.RANGE_FOR_PARTS == 70.0D && LegacyWormBodyEntity.SEGMENT_DISTANCE == 3.5D
                && LegacyWormBodyEntity.MAX_BODY_SPEED == 1.4D, "bot prime body constants");
        check(problems, cases, near(head.getEyeHeight(), 1.5) && near(body.getEyeHeight(), 1.0)
                && near(head.getBbWidth(), 3) && near(body.getBbWidth(), 2), "bot prime sizes / eye height = height / 2");
        check(problems, cases, near(head.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE), 1.0)
                && near(head.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), 0.15)
                && near(head.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 16.0), "bot prime head attributes");
        head.setHealth(15000.0F);
        check(problems, cases, near(head.getHealth(), 15000), "bot prime health 15000 not capped");
        // EntityBurrowingNT drag: 0.995 in air, x0.9 for body parts
        head.setDeltaMovement(1.0D, 0.0D, 0.0D);
        head.travel(Vec3.ZERO);
        check(problems, cases, near(head.getX(), pos.x + 1.0D) && near(head.getDeltaMovement().x, 0.995D),
                "bot prime head travel drag 0.995 (" + head.getDeltaMovement() + ")");
        body.setDeltaMovement(1.0D, 0.0D, 0.0D);
        body.travel(Vec3.ZERO);
        check(problems, cases, near(body.getDeltaMovement().x, 0.995F * 0.9F), "bot prime body travel drag 0.8955 (" + body.getDeltaMovement() + ")");
        // drowning and suffocation are ignored, segments do not hurt their own worm
        check(problems, cases, !head.hurt(level.damageSources().drown(), 10.0F) && !head.hurt(level.damageSources().inWall(), 10.0F)
                && near(head.getHealth(), 15000), "bot prime ignores drown / inWall");
        body.initialize(head.getUUID(), head.getUUID(), 0);
        check(problems, cases, body.belongsTo(head.getUUID()) && body.partNumber() == 0, "bot prime body link");
        boolean hit = head.hurt(level.damageSources().generic(), 100.0F);
        check(problems, cases, hit && near(head.getHealth(), 14900), "bot prime head takes plain damage (" + head.getHealth() + ")");
        check(problems, cases, !body.canBeAffected(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 100)),
                "bot prime body immune to potion effects");
        // addVelocity is a no-op
        head.setDeltaMovement(Vec3.ZERO);
        head.push(1.0D, 1.0D, 1.0D);
        check(problems, cases, head.getDeltaMovement().lengthSqr() == 0.0D, "bot prime ignores pushes");
        head.discard();
        body.discard();
    }

    private static void maskman(ServerLevel level, List<String> problems, int[] cases) {
        check(problems, cases, LegacyMaskManEntity.MINIGUN_DELAY == 3
                && LegacyMaskManEntity.LASER_ATTACKS[0][0] == 60 && LegacyMaskManEntity.LASER_ATTACKS[0][1] == 5
                && LegacyMaskManEntity.LASER_ATTACKS[1][0] == 10 && LegacyMaskManEntity.LASER_ATTACKS[1][1] == 10
                && LegacyMaskManEntity.LASER_ATTACKS[2][0] == 40 && LegacyMaskManEntity.LASER_ATTACKS[2][1] == 3,
                "maskman attack delays / amounts");
        check(problems, cases, LegacyMaskManEntity.nextLaserAttack(0, 0) == 0 && LegacyMaskManEntity.nextLaserAttack(0, 1) == 1
                && LegacyMaskManEntity.nextLaserAttack(2, 1) == 0, "maskman attack rotation (same or next)");
        LegacyMaskManEntity man = HbmEntityTypes.MASK_MAN.get().create(level);
        if (man == null) {
            check(problems, cases, false, "maskman not created");
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            man.discard();
            return;
        }
        man.setPos(testPos(level));
        check(problems, cases, near(man.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 100)
                && near(man.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE), 1) && man.fireImmune(), "maskman attributes");
        DamageSource generic = level.damageSources().generic();
        man.hurt(generic, 40.0F);
        check(problems, cases, near(man.getHealth(), 960), "maskman plain damage (" + man.getHealth() + ")");
        man.invulnerableTime = 0;
        man.hurt(generic, 100.0F); // 50 + 50 * 0.5
        check(problems, cases, near(man.getHealth(), 885), "maskman damage above 50 halved (" + man.getHealth() + ")");
        man.invulnerableTime = 0;
        man.hurt(level.damageSources().explosion(null, null), 40.0F); // x0.5
        check(problems, cases, near(man.getHealth(), 865), "maskman explosion damage x0.5 (" + man.getHealth() + ")");
        man.invulnerableTime = 0;
        man.hurt(level.damageSources().magic(), 40.0F);
        check(problems, cases, near(man.getHealth(), 865), "maskman magic damage 0 (" + man.getHealth() + ")");
        man.discard();
    }
}
