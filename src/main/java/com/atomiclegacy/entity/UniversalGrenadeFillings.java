package com.atomiclegacy.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.UniversalGrenadeItem;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory50;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryCatapult;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryEnergy;
import com.atomiclegacy.network.GunMukeEffectPayload;
import com.atomiclegacy.network.GunPlasmaPulsePayload;
import com.atomiclegacy.network.GunTinyExplosionPayload;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.satellite.SatelliteEventTracker;
import com.atomiclegacy.satellite.SatelliteEventTracker.BurstIntensity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * [parity H2: grenade_universal] 1.7.10 {@code ItemGrenadeFilling} explosion lambdas and the {@code onExplode}
 * lambdas of {@code ItemGrenadeExtra}. All values are the literals of
 * {@code com/hbm/items/weapon/grenade/ItemGrenadeFilling.java} / {@code ItemGrenadeExtra.java}.
 */
public final class UniversalGrenadeFillings {

    private UniversalGrenadeFillings() {
    }

    /** LAMBDA_TINY_EXPLODE: no effect during the first tick, then Lego.tinyExplode(bullet, mop, 1.5F). */
    public static final BulletImpact LAMBDA_TINY_EXPLODE = (bullet, mop) -> {
        if (bullet.tickCount < 2) return false;
        Direction side = mop instanceof BlockHitResult block ? block.getDirection() : Direction.DOWN;
        XFactory50.tinyExplode(bullet, mop.getLocation(), side, bullet.baseDamage(), 1.5F, 1F);
        return true;
    };

    /** LAMBDA_EXPLODE: no effect during the first tick, then Lego.standardExplode(bullet, mop, 5F). */
    public static final BulletImpact LAMBDA_EXPLODE = (bullet, mop) -> {
        if (bullet.tickCount < 2) return false;
        XFactory12ga.standardExplode(bullet, mop.getLocation(), bullet.baseDamage(), 5F, 1F);
        return true;
    };

    /**
     * laser = new BulletConfig().setBeam().setupDamageClass(LASER).setLife(3).setRenderRotations(false)
     * .setThresholdNegation(10F).setOnBeamImpact(LAMBDA_STANDARD_BEAM_HIT), rendered with LegoClient.RENDER_LASER_RED.
     */
    public static final BeamBulletConfig laser;

    static {
        // fragmentation = new BulletConfig().setLife(3).setThresholdNegation(5F).setRicochetAngle(90).setRicochetCount(2)
        StandardAmmoType.GRENADE_FRAGMENTATION.setLife(3).setRicochetAngle(90F).setRicochetCount(2);
        // pellets = new BulletConfig().setLife(100).setGrav(0.04).setVel(1.5F).setOnImpact(LAMBDA_TINY_EXPLODE)
        StandardAmmoType.GRENADE_PELLETS.setLife(100).setGrav(0.04D).setVel(1.5D).setOnImpact(LAMBDA_TINY_EXPLODE);
        // pellets_heavy = new BulletConfig().setLife(100).setGrav(0.04).setVel(1.5F).setOnImpact(LAMBDA_EXPLODE)
        StandardAmmoType.GRENADE_PELLETS_HEAVY.setLife(100).setGrav(0.04D).setVel(1.5D).setOnImpact(LAMBDA_EXPLODE);
        laser = new BeamBulletConfig(StandardAmmoType.GRENADE_LASER).setupDamageClass(DamageClass.LASER).setLife(3)
                .setRenderRotations(false).setThresholdNegation(10F)
                .setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setLaserColor(0x80, 0x15, 0x15);
    }

    /** ItemGrenadeFilling's constructor created the configs; called from the item constructor on both sides. */
    public static void init() {
    }

    /** EnumGrenadeFilling.explode */
    static void explode(UniversalGrenadeEntity grenade, ServerLevel level) {
        double x = grenade.getX();
        double y = grenade.getY();
        double z = grenade.getZ();
        LivingEntity thrower = grenade.getThrower();
        switch (grenade.filling()) {
            case POWDER -> standardExplode(level, thrower, x, y, z, 5F, 10F, 5F, 0F);
            case HE -> standardExplode(level, thrower, x, y, z, 7.5F, 25F, 10F, 0.1F);
            case DEMO ->
                // ExplosionVNT(5F, thrower): BlockAllocatorStandard() (resolution 16), BlockProcessorStandard,
                // EntityProcessorCrossSmooth(1, 10F), PlayerProcessorStandard, ExplosionEffectWeapon(10, 2.5F, 1F)
                XFactoryCatapult.explodeVNT(level, thrower, x, y, z, 5F, 16, null, 1D, 10F, 1F, true);
            case INC -> {
                standardExplode(level, thrower, x, y, z, 3F, 10F, 0F, 0F);
                level.addFreshEntity(new LegacyLingeringFireEntity(level, new Vec3(x, y, z),
                        LegacyLingeringFireEntity.FireType.DIESEL, 6F, 2F, 200));
                igniteCube(level, x, y, z, 2);
            }
            case WP -> {
                standardExplode(level, thrower, x, y, z, 3F, 10F, 0F, 0F);
                level.addFreshEntity(new LegacyLingeringFireEntity(level, new Vec3(x, y, z),
                        LegacyLingeringFireEntity.FireType.PHOSPHORUS, 6F, 2F, 600));
                igniteCube(level, x, y, z, 3);
                for (int i = 0; i < 3; i++) {
                    // AuxParticlePacketNT "haze" within 150 blocks of the grenade
                    double hx = x + level.random.nextGaussian() * 4;
                    double hz = z + level.random.nextGaussian() * 4;
                    for (ServerPlayer player : level.players()) {
                        if (player.distanceToSqr(x, y, z) <= 150D * 150D) {
                            level.sendParticles(player, HbmParticleTypes.HAZE.get(), true, hx, y, hz, 1, 0D, 0D, 0D, 0D);
                        }
                    }
                }
            }
            case CLUSTER -> {
                standardExplode(level, thrower, x, y, z, 7.5F, 15F, 10F, 0.1F);
                int frags = 30;
                if (grenade.shell() == UniversalGrenadeItem.Shell.FRAG) frags *= 1.25;
                for (int i = 0; i < frags; i++) {
                    LegacyBulletEntity bullet = bullet(level, StandardAmmoType.GRENADE_PELLETS, 15F,
                            level.random.nextFloat() * 2F * (float) Math.PI,
                            (level.random.nextFloat() * 0.5F + 0.5F) * (float) Math.PI, x, y + 0.05, z);
                    scaleMotion(bullet, 0.5D, 0.75D, 0.5D);
                    level.addFreshEntity(bullet);
                }
            }
            case CLUSTER_HEAVY -> {
                standardExplode(level, thrower, x, y, z, 7.5F, 15F, 10F, 0.1F);
                int frags = 15;
                for (int i = 0; i < frags; i++) {
                    LegacyBulletEntity bullet = bullet(level, StandardAmmoType.GRENADE_PELLETS_HEAVY, 30F,
                            level.random.nextFloat() * 2F * (float) Math.PI,
                            (level.random.nextFloat() * 0.5F + 0.5F) * (float) Math.PI, x, y + 0.05, z);
                    scaleMotion(bullet, 0.5D, 1.25D, 0.5D);
                    level.addFreshEntity(bullet);
                }
            }
            case EMP -> {
                explodeStandardEnergy(level, thrower, x, y, z, 15F, 3F, DamageClass.ELECTRIC, 0.5F, 0.5F, 1F, 3F);
                LegacyEmpUtil.empBlast(level, Mth.floor(x), Mth.floor(y), Mth.floor(z), 5);
            }
            case PLASMA -> explodeStandardEnergy(level, thrower, x, y, z, 50F, 5F, DamageClass.PLASMA, 0.5F, 1F, 0.5F, 4F);
            case LASER -> {
                tinyExplode(level, thrower, x, y, z, 2F, 5F, 0F, 0F);
                double ly = y + 0.125;
                double range = 15;
                List<LivingEntity> potentialTargets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(x, ly, z, x, ly, z).inflate(range, range, range)));
                Collections.shuffle(potentialTargets);
                for (LivingEntity target : potentialTargets) {
                    if (target == thrower) continue;
                    Vec3 delta = new Vec3(target.getX() - x, target.getY() + target.getBbHeight() / 2 - ly, target.getZ() - z);
                    if (delta.length() > range) continue;
                    LegacyBulletBeamEntity sub = new LegacyBulletBeamEntity(level, thrower, laser, 30F);
                    sub.setPos(x, ly, z);
                    sub.setRotationsFromVector(delta);
                    sub.performHitscanExternal(delta.length());
                    level.addFreshEntity(sub);
                }
            }
            case NUCLEAR -> {
                // ExplosionVNT(10) without exploder: EntityProcessorCrossSmooth(2, 100).withRangeMod(1.5F), no blocks, no SFX
                XFactoryCatapult.explodeVNT(level, null, x, y, z, 10F, 0, null, 2D, 100F, 1.5F, false);
                XFactoryCatapult.incrementRad(level, x, y, z, 1F);
                spawnMush(level, x, y, z);
            }
            case NUCLEAR_DEMO -> {
                // BlockAllocatorStandard(64), BlockProcessorStandard + BlockMutatorFire, EntityProcessorCrossSmooth(2, 50).withRangeMod(1.5F)
                XFactoryCatapult.explodeVNT(level, null, x, y, z, 10F, 64, Blocks.FIRE.defaultBlockState(), 2D, 50F, 1.5F, false);
                XFactoryCatapult.incrementRad(level, x, y, z, 1.5F);
                spawnMush(level, x, y, z);
            }
            case SCHRAB -> {
                // EntityNukeExplosionMK3.statFacFleija(world, x, y, z, 20): dead when a field disturber is within 300 blocks
                if (!NukeExplosionManager.isFleijaSuppressed(level, BlockPos.containing(x, y, z))) {
                    level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 100.0F,
                            level.random.nextFloat() * 0.1F + 0.9F);
                    // MK3 explosion entity + EntityCloudFleija(world, 20)
                    NukeExplosionManager.scheduleFleijaNuke(level, x, y, z, 20);
                }
            }
        }
    }

    /** EnumGrenadeExtra.onExplode */
    static void extraExplode(UniversalGrenadeEntity grenade, ServerLevel level) {
        UniversalGrenadeItem.Extra extra = grenade.extra();
        if (extra == UniversalGrenadeItem.Extra.FRAG_SLEEVE) {
            standardFragmentation(grenade, level, 25);
        } else if (extra == UniversalGrenadeItem.Extra.TRIPLEX) {
            ItemStack frag = UniversalGrenadeItem.make(grenade.shell(), grenade.filling(), UniversalGrenadeItem.Fuze.S3, null);
            // new Vec3NT(0.25, 0, 0).rotateAroundYDeg(rand * 360)
            double angle = Math.toRadians(level.random.nextDouble() * 360);
            for (int i = 0; i < 3; i++) {
                Vec3 vec = new Vec3(0.25D * Math.cos(angle), 0D, 0.25D * Math.sin(angle));
                UniversalGrenadeEntity triplet = new UniversalGrenadeEntity(level, frag).setTrail(UniversalGrenadeEntity.TRAIL_TRIPLET);
                triplet.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
                triplet.setThrower(grenade.getThrower());
                triplet.setDeltaMovement(vec.x, 0.75D, vec.z);
                level.addFreshEntity(triplet);
                angle += Math.toRadians(120);
            }
        }
    }

    /** standardFragmentation(grenade, frags): x1.5 for the frag shell, fragmentation bullets with 10 damage. */
    static void standardFragmentation(UniversalGrenadeEntity grenade, ServerLevel level, float frags) {
        if (grenade.shell() == UniversalGrenadeItem.Shell.FRAG) frags *= 1.5;
        for (int i = 0; i < frags; i++) {
            LegacyBulletEntity bullet = bullet(level, StandardAmmoType.GRENADE_FRAGMENTATION, 10F,
                    level.random.nextFloat() * 2F * (float) Math.PI,
                    (level.random.nextFloat() - 0.5F) * 2F * (float) Math.PI,
                    grenade.getX(), grenade.getY() + 0.05, grenade.getZ());
            level.addFreshEntity(bullet);
        }
    }

    /**
     * new EntityBulletBaseMK4(world, config, damage, 0F, yaw, pitch): rotationYaw = yaw, rotationPitch = -pitch (both
     * given in radians), heading from the rotation, no thrower.
     */
    private static LegacyBulletEntity bullet(ServerLevel level, StandardAmmoType type, float damage, float yaw, float pitch,
                                             double x, double y, double z) {
        double mx = -Mth.sin(yaw) * Mth.cos(-pitch);
        double mz = Mth.cos(yaw) * Mth.cos(-pitch);
        double my = -Mth.sin(-pitch);
        return new LegacyBulletEntity(level, x, y, z, new Vec3(mx, my, mz), type, damage, null, 0F);
    }

    /** bullet.motionX/Y/Z *= ...: the unscaled heading, the displacement is heading * BulletConfig.velocity. */
    private static void scaleMotion(LegacyBulletEntity bullet, double mx, double my, double mz) {
        bullet.legacyMotion = bullet.legacyMotion.multiply(mx, my, mz);
        bullet.setDeltaMovement(bullet.getDeltaMovement().multiply(mx, my, mz));
    }

    /** Air blocks with a flammable neighbour inside the cube become fire. */
    private static void igniteCube(ServerLevel level, double px, double py, double pz, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            pos.set(Mth.floor(px) + dx, Mth.floor(py) + dy, Mth.floor(pz) + dz);
            if (!level.getBlockState(pos).isAir()) continue;
            for (Direction dir : Direction.values()) {
                BlockPos neighbour = pos.relative(dir);
                if (level.getBlockState(neighbour).isFlammable(level, neighbour, dir.getOpposite())) {
                    level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    break;
                }
            }
        }
    }

    /** spawnMush: satellite report, mini nuke sound (15, 1) and the "muke" effect at y + 0.5 within 250 blocks. */
    private static void spawnMush(ServerLevel level, double x, double y, double z) {
        SatelliteEventTracker.reportRadiation(level, SatelliteEventTracker.DETECTOR_LOW_DURATION, BurstIntensity.LOW, x, z);
        level.playSound(null, x, y, z, HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
        // [parity J1] balefire: polaroidID == 11 || rand.nextInt(100) == 0
        XFactoryCatapult.sendMukeEffect(level, x, y + 0.5, z, GunMukeEffectPayload.MODE_MUKE, com.atomiclegacy.AtomicLegacy.polaroidID == 11 || level.random.nextInt(100) == 0);
    }

    /**
     * explodeStandardEnergy: ExplosionVNT(range, thrower) with EntityProcessorCrossSmooth(1, damage).setDamageClass,
     * PlayerProcessorStandard; ufoBlast (5, 0.9..1.1) and fireworks bang (5, 0.5); three "plasmablast" particles at
     * y + 0.125 within 100 blocks.
     */
    private static void explodeStandardEnergy(ServerLevel level, @Nullable LivingEntity thrower, double x, double y, double z,
                                              float damage, float range, DamageClass damageClass, float r, float g, float b, float scale) {
        XFactoryEnergy.lightningExplosion(level, thrower, x, y, z, range, damage, damageClass);
        level.playSound(null, x, y, z, HbmSoundEvents.ENTITY_UFO_BLAST.get(), SoundSource.HOSTILE, 5.0F, 0.9F + level.random.nextFloat() * 0.2F);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 5.0F, 0.5F);
        float yaw = level.random.nextFloat() * 180F;
        for (int i = 0; i < 3; i++) {
            PacketDistributor.sendToPlayersNear(level, null, x, y, z, 100,
                    new GunPlasmaPulsePayload(x, y + 0.125, z, r, g, b, -60F + 60F * i, yaw, scale));
        }
    }

    /** standardExplode: EntityProcessorCrossSmooth(1, damage).setupPiercing(dt, dr) + ExplosionEffectWeapon(10, 2.5F, 1F). */
    public static void standardExplode(ServerLevel level, @Nullable LivingEntity thrower, double x, double y, double z,
                                       float range, float damage, float dt, float dr) {
        crossSmooth(level, thrower, x, y, z, range, 1D, damage, dt, dr, 1D);
        LegacyProjectileUtil.sendSmallExplosionEffect(level, new Vec3(x, y, z), 10, 2.5F, 1F);
    }

    /** tinyExplode: EntityProcessorCrossSmooth(0.5, damage).setupPiercing(dt, dr).setKnockback(0.25D) + ExplosionEffectTiny. */
    public static void tinyExplode(ServerLevel level, @Nullable LivingEntity thrower, double x, double y, double z,
                                   float range, float damage, float dt, float dr) {
        crossSmooth(level, thrower, x, y, z, range, 0.5D, damage, dt, dr, 0.25D);
        level.playSound(null, x, y, z, HbmSoundEvents.WEAPON_EXPLOSION_TINY.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, 100, new GunTinyExplosionPayload(x, y, z, 1.5F, 1));
    }

    /**
     * ExplosionVNT(size, exploder) with only EntityProcessorCrossSmooth(nodeDist, fixedDamage).setupPiercing(dt, dr)
     * [.setKnockback(mult)] and PlayerProcessorStandard: no block processing. Same entity processing as
     * XFactoryCatapult.explodeVNT / XFactory12ga.standardExplode (setAllowSelfDamage, the exploder takes half damage,
     * DamageClass.EXPLOSIVE through attackEntityFromNT without knockback from the attack itself).
     */
    public static void crossSmooth(ServerLevel level, @Nullable Entity exploder, double x, double y, double z, float size,
                                   double nodeDist, float fixedDamage, float pierceDT, float pierceDR, double knockbackMult) {
        Explosion compat = new Explosion(level, exploder, x, y, z, size, false, Explosion.BlockInteraction.KEEP);
        float range = size * 2.0F;
        AABB area = new AABB(x - range - 1.0D, y - range - 1.0D, z - range - 1.0D, x + range + 1.0D, y + range + 1.0D, z + range + 1.0D);
        List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
        EventHooks.onExplosionDetonate(level, compat, list, range);

        Vec3[] nodes;
        if (nodeDist > 0) {
            nodes = new Vec3[7];
            Direction[] dirs = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
            for (int i = 0; i < 6; i++) nodes[i] = new Vec3(x + dirs[i].getStepX() * nodeDist, y + dirs[i].getStepY() * nodeDist, z + dirs[i].getStepZ() * nodeDist);
            nodes[6] = new Vec3(x, y, z); // ForgeDirection.UNKNOWN
        } else {
            nodes = new Vec3[] { new Vec3(x, y, z) };
        }

        Map<Entity, Float> damageMap = new LinkedHashMap<>();
        HashMap<ServerPlayer, Vec3> affectedPlayers = new HashMap<>();

        for (Entity entity : list) {
            AABB box = entity.getBoundingBox();
            double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
            double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
            double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
            double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
            double distanceScaled = dist / range;

            if (distanceScaled <= 1.0D) {
                double deltaX = entity.getX() - x;
                double deltaY = entity.getEyeY() - y;
                double deltaZ = entity.getZ() - z;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance != 0.0D) {
                    deltaX /= distance;
                    deltaY /= distance;
                    deltaZ /= distance;

                    double density = 0;
                    for (Vec3 vec : nodes) {
                        double d = Explosion.getSeenPercent(vec, entity);
                        if (d > density) density = d;
                    }

                    double knockback = (1.0D - distanceScaled) * density;
                    float dmg = density < 0.125 ? 0F : (float) (fixedDamage * (1 - distanceScaled)); // EntityProcessorCrossSmooth.calculateDamage
                    if (!damageMap.containsKey(entity) || damageMap.get(entity) < dmg) damageMap.put(entity, dmg);
                    double enchKnockback = entity instanceof LivingEntity living ? knockback * (1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) : knockback;

                    // EntityProcessorCross.shouldDealKnockback: not for bullets and universal grenades
                    if (!(entity instanceof LegacyBulletEntity) && !(entity instanceof UniversalGrenadeEntity) && !(entity instanceof Player)) {
                        entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * enchKnockback * knockbackMult, deltaY * enchKnockback * knockbackMult, deltaZ * enchKnockback * knockbackMult));
                        entity.hurtMarked = true;
                    }

                    if (entity instanceof ServerPlayer player) {
                        affectedPlayers.put(player, new Vec3(deltaX * knockback * knockbackMult, deltaY * knockback * knockbackMult, deltaZ * knockback * knockbackMult));
                    }
                }
            }
        }

        for (Map.Entry<Entity, Float> entry : damageMap.entrySet()) {
            Entity entity = entry.getKey();
            float amount = entry.getValue();
            if (!entity.isAlive()) continue;
            if (exploder == entity) amount *= 0.5F;
            // BulletConfig.getDamage(null, exploder, EXPLOSIVE)
            DamageSource dmg = level.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, null, exploder instanceof LivingEntity ? exploder : null);
            if (entity instanceof LivingEntity living) {
                // EntityDamageUtil.attackEntityFromNT(living, dmg, amount, true, false, 0F, pierceDT, pierceDR)
                amount = LegacyProjectileUtil.sednaExplosiveDamage(living, amount, pierceDT, pierceDR);
                XFactory50.hurtWithoutKnockback(living, dmg, amount);
            } else {
                entity.hurt(dmg, amount);
            }
        }

        /// PlayerProcessorStandard: ExplosionKnockbackPacket adds the motion on the client ///
        for (Map.Entry<ServerPlayer, Vec3> entry : affectedPlayers.entrySet()) {
            ServerPlayer player = entry.getKey();
            player.setDeltaMovement(player.getDeltaMovement().add(entry.getValue()));
            player.hurtMarked = true;
        }
    }
}
