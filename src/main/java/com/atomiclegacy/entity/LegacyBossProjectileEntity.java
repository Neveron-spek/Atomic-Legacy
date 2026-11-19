package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.explosion.LegacyMukeExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NPC bullet configuration port for worm bolts, UFO lasers and UFO rockets.
 * The values are the corresponding 1.7.10 GunNPCFactory values, rather than
 * reusing a player-ammo profile.
 */
public final class LegacyBossProjectileEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(LegacyBossProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(LegacyBossProjectileEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUuid;
    private UUID targetUuid;

    public LegacyBossProjectileEntity(EntityType<? extends LegacyBossProjectileEntity> type, Level level) {
        super(type, level);
    }

    public LegacyBossProjectileEntity(Level level, Entity owner, Vec3 origin, Vec3 direction, Type type, Entity target) {
        this(level, owner, origin, direction, type, target, 0.0F);
    }

    public LegacyBossProjectileEntity(Level level, Entity owner, Vec3 origin, Vec3 direction,
                                      Type type, Entity target, float spread) {
        this(HbmEntityTypes.LEGACY_BOSS_PROJECTILE.get(), level);
        setPos(origin.x, origin.y, origin.z);
        this.ownerUuid = owner.getUUID();
        this.targetUuid = target == null ? null : target.getUUID();
        entityData.set(TYPE, type.ordinal());
        entityData.set(DAMAGE, type.damage(level.random));
        // EntityBulletBaseNT#setThrowableHeading normalizes the requested
        // heading, adds Gaussian noise (0.0075 * inaccuracy), and then
        // applies the velocity without renormalizing the noisy vector.
        Vec3 normalized = direction.normalize();
        Vec3 motion = new Vec3(
                normalized.x + random.nextGaussian() * 0.0075D * spread,
                normalized.y + random.nextGaussian() * 0.0075D * spread,
                normalized.z + random.nextGaussian() * 0.0075D * spread
        ).scale(type.speed());
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, Type.WORM_BOLT.ordinal());
        builder.define(DAMAGE, 15.0F);
    }

    public Type projectileType() {
        int id = entityData.get(TYPE);
        return id >= 0 && id < Type.values().length ? Type.values()[id] : Type.WORM_BOLT;
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            maskmanUpdate();
        }
        if (projectileType() == Type.UFO_ROCKET && !level().isClientSide) {
            Entity target = target();
            if (target == null) {
                target = chooseHomingTarget();
                targetUuid = target == null ? null : target.getUUID();
            }
            if (target != null) {
                // GunNPCFactory.getRocketUFOConfig detonates once the homing
                // rocket is within sqrt(5) blocks, even without a collision
                // ray crossing the target's box.
                if (distanceToSqr(target) < 5.0D) {
                    impact(position(), null, null); // [parity A2] bntImpact at the rocket itself
                    return;
                }
                // [parity A2] getRocketUFOConfig keeps the current speed (no 2.0 floor).
                motion = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                        .subtract(position()).normalize().scale(motion.length());
                setDeltaMovement(motion);
            }
        }
        updateRotation(motion);
        Type type = projectileType();
        if (type == Type.CHOPPER_BULLET || type == Type.CRAB_TAU) {
            tickLegacyBullet(type, motion);
            return;
        }
        // [parity D1] EntityThrowableNT: the per-tick displacement is motion * motionMult(), and
        // EntityBulletBaseNT#motionMult() is BulletConfiguration.velocity; gravity acts on the raw motion.
        Vec3 start = position();
        Vec3 end = start.add(motion.scale(type.velocityMultiplier()));
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockMiss = blockHit.getType() == HitResult.Type.MISS;
        if (level().isClientSide) {
            // EntityThrowableInterp never resolves impacts on the client; it waits for the server.
            if (type == Type.MASKMAN_METEOR) {
                // GunNPCFactory.getMaskmanMeteor bntUpdate: 5 "vanillaExt"/"flame" particles per client tick.
                for (int i = 0; i < 5; i++) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                            getX() + random.nextDouble() * 0.5D - 0.25D,
                            getY() + random.nextDouble() * 0.5D - 0.25D,
                            getZ() + random.nextDouble() * 0.5D - 0.25D, 0.0D, 0.0D, 0.0D);
                }
            }
            if (blockMiss) {
                setPos(end);
                setDeltaMovement(new Vec3(motion.x, motion.y - type.gravity(), motion.z));
            }
            clientTrail(type);
            return;
        }
        Vec3 entityEnd = blockMiss ? end : blockHit.getLocation();
        List<EntityHitResult> entityHits = findEntityHits(start, entityEnd, true);
        if (type.penetrates()) {
            // EntityThrowableNT: a penetrating bullet calls onImpact for every intersected entity and flies on;
            // EntityBulletBaseNT then only runs onEntityHurt + damage (no bntImpact, no setDead).
            for (EntityHitResult entityHit : entityHits) {
                hurtEntity(type, entityHit.getEntity());
            }
        } else if (!entityHits.isEmpty()) {
            impact(entityHits.getFirst().getLocation(), entityHits.getFirst().getEntity(), null);
            return;
        }
        if (blockHit instanceof BlockHitResult block && !blockMiss) {
            // EntityBulletBaseNT#onImpact: doesRicochet && rand(100) < HBRC (2) always reflects off the hit
            // face, plays the plink sound, moves to the hit point and scales the motion by bounceMod (0.8).
            if (type.ricochets() && random.nextInt(100) < 2) {
                motion = switch (block.getDirection().getAxis()) {
                    case Y -> new Vec3(motion.x, -motion.y, motion.z);
                    case Z -> new Vec3(motion.x, motion.y, -motion.z);
                    case X -> new Vec3(-motion.x, motion.y, motion.z);
                };
                if (type == Type.UFO_ROCKET) {
                    level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_GRENADE_BOUNCE.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                } else {
                    level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_RICOCHET.get(), SoundSource.HOSTILE, 0.25F, 1.0F);
                }
                motion = motion.scale(0.8D);
                end = block.getLocation().add(motion.scale(type.velocityMultiplier()));
            } else {
                impact(block.getLocation(), null, block.getBlockPos());
                return;
            }
        }
        setPos(end);
        setDeltaMovement(new Vec3(motion.x, motion.y - type.gravity(), motion.z));
        if (tickCount > type.maxAge() || getY() < level().getMinBuildHeight() - 16) {
            discard();
        }
    }

    /**
     * [parity D1] 1.7.10 EntityBullet (chopper flavour, not critical): the nearest entity on the path is
     * damaged every tick and the bullet flies on (it is never removed by an entity hit; the block hit of that
     * tick is skipped), a block hit removes it and shatters vanilla glass, drag 0.99 (0.8 in water), 250 ticks.
     */
    private void tickLegacyBullet(Type type, Vec3 motion) {
        // getIsCritical() (the cyber crab's tau round): block hits are ignored altogether and 8 "reddust"
        // particles are spawned along the motion every tick.
        boolean critical = type == Type.CRAB_TAU;
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = critical
                ? BlockHitResult.miss(end, net.minecraft.core.Direction.UP, BlockPos.containing(end))
                : level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockMiss = blockHit.getType() == HitResult.Type.MISS;
        if (level().isClientSide) {
            if (critical) {
                for (int i = 0; i < 8; i++) {
                    level().addParticle(net.minecraft.core.particles.DustParticleOptions.REDSTONE,
                            getX() + motion.x * i / 8.0D, getY() + motion.y * i / 8.0D, getZ() + motion.z * i / 8.0D,
                            0.0D, 0.0D, 0.0D);
                }
            }
            if (blockMiss) {
                setPos(end);
                setDeltaMovement(motion.scale(isInWater() ? 0.8D : 0.99D));
            }
            return;
        }
        List<EntityHitResult> entityHits = findEntityHits(start, blockMiss ? end : blockHit.getLocation(), false);
        if (!entityHits.isEmpty()) {
            Entity hit = entityHits.getFirst().getEntity();
            // capabilities.disableDamage players void the whole impact of this tick
            if (!(hit instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().invulnerable)) {
                hurtEntity(type, hit);
            }
        } else if (blockHit instanceof BlockHitResult block && !blockMiss) {
            net.minecraft.world.level.block.state.BlockState state = level().getBlockState(block.getBlockPos());
            if (isLegacyGlass(state)) {
                level().setBlockAndUpdate(block.getBlockPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level().playSound(null, block.getBlockPos(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setPos(block.getLocation());
            discard();
            return;
        }
        setPos(end);
        setDeltaMovement(motion.scale(isInWater() ? 0.8D : 0.99D));
        if (tickCount > type.maxAge() || getY() < level().getMinBuildHeight() - 16) {
            discard();
        }
    }

    /** Blocks.glass, stained_glass, glass_pane and stained_glass_pane of 1.7.10. */
    private static boolean isLegacyGlass(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == net.minecraft.world.level.block.Blocks.GLASS
                || block == net.minecraft.world.level.block.Blocks.GLASS_PANE
                || block instanceof net.minecraft.world.level.block.StainedGlassBlock
                || block instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;
    }

    /** EntityBulletBaseNT client branch: config.vPFX particles every 0.5 blocks along the last displacement. */
    private void clientTrail(Type type) {
        net.minecraft.core.particles.ParticleOptions particle = type.trailParticle();
        if (particle == null) {
            return;
        }
        Vec3 delta = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
        double length = Math.max(delta.length(), 0.1D);
        Vec3 direction = delta.normalize();
        for (double d = 0.0D; d < length; d += 0.5D) {
            level().addParticle(particle, getX() - direction.x * d, getY() - direction.y * d, getZ() - direction.z * d,
                    0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * EntityBulletBaseNT / EntityBullet entity damage: dmgMin + rand * (dmgMax - dmgMin) with the config's damage
     * type; a hit rejected by the hurt cooldown is retried on top of lastDamage.
     */
    private void hurtEntity(Type type, Entity directHit) {
        net.minecraft.world.damagesource.DamageSource source = damageSources().source(type.damageType(), this, owner());
        float damage = entityData.get(DAMAGE);
        if (type.incendiary > 0) directHit.igniteForSeconds(type.incendiary);
        if (!directHit.hurt(source, damage) && directHit.invulnerableTime > 0) {
            int cooldown = directHit.invulnerableTime;
            directHit.invulnerableTime = 0;
            if (!directHit.hurt(source, damage)) directHit.invulnerableTime = cooldown;
        }
    }

    private void impact(Vec3 hit, Entity directHit, BlockPos blockPos) {
        if (level().isClientSide) {
            return;
        }
        Type type = projectileType();
        if (directHit != null) {
            hurtEntity(type, directHit);
        }
        // BulletConfiguration.doesBreakGlass (standardBulletConfig): func_147480_a(x, y, z, false) on vanilla glass.
        if (blockPos != null && type.penetrates() && isLegacyGlass(level().getBlockState(blockPos))) {
            level().destroyBlock(blockPos, false);
        }
        setPos(hit.x, hit.y, hit.z);
        if (type == Type.MASKMAN_TRACER) {
            LegacyBossProjectileEntity meteor = new LegacyBossProjectileEntity(HbmEntityTypes.LEGACY_BOSS_PROJECTILE.get(), level());
            meteor.setPos(hit.x, hit.y + 30 + level().random.nextInt(10), hit.z);
            meteor.ownerUuid = ownerUuid;
            meteor.entityData.set(TYPE, Type.MASKMAN_METEOR.ordinal());
            meteor.entityData.set(DAMAGE, Type.MASKMAN_METEOR.damage(level().random));
            meteor.setDeltaMovement(0.0D, -1.0D, 0.0D);
            level().addFreshEntity(meteor);
        }
        if (type.incendiary > 0) {
            int bx = (int) hit.x, by = (int) hit.y, bz = (int) hit.z;
            int[][] offsets = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int[] o : offsets) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(bx + o[0], by + o[1], bz + o[2]);
                if (level().random.nextInt(3) == 0 && level().getBlockState(pos).isAir()) {
                    level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                }
            }
        }
        if (type.explosive > 0.0F) {
            if (type.blockDamage) LegacyProjectileUtil.standardExplosion(this, hit, type.explosive, 1.0F, true, false, true);
            else LegacyProjectileUtil.deathRayBoltImpactExplosion(this, hit, type.explosive);
        }
        if (type == Type.UFO_ROCKET) {
            level().playSound(null, hit.x, hit.y, hit.z, HbmSoundEvents.ENTITY_UFO_BLAST.get(),
                    SoundSource.HOSTILE, 5.0F, 0.9F + level().random.nextFloat() * 0.2F);
            level().playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.HOSTILE, 5.0F, 0.5F);
            LegacyMukeExplosion.detonateUfoRocket((ServerLevel) level(), this, hit);
            if (level() instanceof ServerLevel serverLevel) {
                for (int index = 0; index < 3; index++) {
                    serverLevel.sendParticles(com.atomiclegacy.registry.HbmParticleTypes.LEGACY_PLASMA_BLAST.get(),
                            hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
        discard();
    }

    /** [parity A2] GunNPCFactory.getMaskmanOrb bntUpdate: every 10 ticks a MASKMAN_BOLT at each player within 50 blocks. */
    private void maskmanUpdate() {
        if (projectileType() != Type.MASKMAN_ORB || tickCount % 10 != 5) return;
        for (net.minecraft.world.entity.player.Player player : level().getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class, getBoundingBox().inflate(50.0D))) {
            Vec3 heading = new Vec3(player.getX() - getX(), player.getY() + player.getEyeHeight() - getY(),
                    player.getZ() - getZ()).normalize();
            Vec3 motion = new Vec3(
                    heading.x + random.nextGaussian() * 0.0075D * 0.05D,
                    heading.y + random.nextGaussian() * 0.0075D * 0.05D,
                    heading.z + random.nextGaussian() * 0.0075D * 0.05D).scale(0.5D);
            level().addFreshEntity(new LegacyDeathRayBoltEntity(level(), getX(), getY(), getZ(), motion));
        }
    }

    /** [parity A2] EntityAIMaskmanLasergun adjusts the spawned bullet's motion before adding it to the world. */
    public void overrideMotion(Vec3 motion) {
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    /**
     * EntityThrowableNT / EntityBullet candidate rule: canBeCollidedWith and (not the thrower, or ticksInAir >=
     * selfDamageDelay 5); EntityThrowableNT additionally requires isEntityAlive. Sorted nearest first.
     */
    private List<EntityHitResult> findEntityHits(Vec3 start, Vec3 end, boolean requireAlive) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity owner = owner();
        return level().getEntities(this, area, entity -> entity.isPickable()
                        && (!requireAlive || entity.isAlive())
                        && (entity != owner || tickCount >= 5))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.3D).clip(start, end)
                        .map(point -> new EntityHitResult(entity, point)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())))
                .toList();
    }

    private Entity chooseHomingTarget() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D) {
            return null;
        }
        Entity owner = owner();
        Entity best = null;
        double bestAngle = 90.0D;
        AABB area = new AABB(position(), position()).inflate(100.0D);
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != owner)) {
            Vec3 delta = candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D)
                    .subtract(position());
            if (delta.lengthSqr() < 1.0E-8D) {
                continue;
            }
            HitResult sight = level().clip(new ClipContext(position(), position().add(delta),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (sight.getType() != HitResult.Type.MISS) {
                continue;
            }
            double cosine = motion.normalize().dot(delta.normalize());
            double angle = Math.toDegrees(Math.acos(Mth.clamp(cosine, -1.0D, 1.0D)));
            if (angle < bestAngle) {
                bestAngle = angle;
                best = candidate;
            }
        }
        return best;
    }

    private Entity owner() {
        return ownerUuid == null || !(level() instanceof ServerLevel serverLevel) ? null : serverLevel.getEntity(ownerUuid);
    }

    private Entity target() {
        return targetUuid == null || !(level() instanceof ServerLevel serverLevel) ? null : serverLevel.getEntity(targetUuid);
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityBulletBaseNT used renderDistanceWeight = 10, i.e. the
        // vanilla 64-block base multiplied by ten.
        return distance < 102400.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("type", entityData.get(TYPE));
        tag.putFloat("damage", entityData.get(DAMAGE));
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
        if (targetUuid != null) tag.putUUID("target", targetUuid);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("type"));
        entityData.set(DAMAGE, tag.getFloat("damage"));
        ownerUuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        targetUuid = tag.hasUUID("target") ? tag.getUUID("target") : null;
    }

    public enum Type {
        WORM_BOLT(0.5D, 60, 15.0F, 25.0F, 0.0D),
        WORM_LASER(1.0D, 100, 35.0F, 60.0F, 0.0D),
        UFO_LASER(2.0D, 100, 35.0F, 60.0F, 0.0D),
        UFO_ROCKET(2.0D, 300, 10.0F, 15.0F, 0.005D),
        CHOPPER_BULLET(3.0D, 250, 3.0F, 7.0F, 0.0D),
        // [parity A2: maskman] GunNPCFactory getMaskmanOrb/Tracer/Rocket/Meteor; speed is the value
        // EntityAIMaskmanLasergun passes to the bullet constructor. New values are appended (ordinal is saved).
        MASKMAN_ORB(2.0D, 60, 100.0F, 100.0F, 0.0D, 1.5F, true, 0),
        MASKMAN_TRACER(1.0D, 100, 15.0F, 20.0F, 0.0D, 0.0F, true, 0),
        MASKMAN_ROCKET(1.0D, 300, 15.0F, 20.0F, 0.1D, 5.0F, false, 0),
        MASKMAN_METEOR(1.0D, 300, 20.0F, 30.0F, 0.1D, 2.5F, false, 3),
        // [parity D1: cyber crab] EntityBullet(world, crab, target, 1.6F, 2): critical tau round, damage 3.
        CRAB_TAU(1.6D, 250, 3.0F, 3.0F, 0.0D);

        private final double speed;
        private final int maxAge;
        private final float minDamage;
        private final float maxDamage;
        private final double gravity;
        private final float explosive;
        private final boolean blockDamage;
        private final int incendiary;
        Type(double speed, int maxAge, float minDamage, float maxDamage, double gravity) {
            this(speed, maxAge, minDamage, maxDamage, gravity, 0.0F, true, 0);
        }
        Type(double speed, int maxAge, float minDamage, float maxDamage, double gravity,
             float explosive, boolean blockDamage, int incendiary) {
            this.explosive = explosive;
            this.blockDamage = blockDamage;
            this.incendiary = incendiary;
            this.speed = speed;
            this.maxAge = maxAge;
            this.minDamage = minDamage;
            this.maxDamage = maxDamage;
            this.gravity = gravity;
        }

        private double speed() { return speed; }
        /** [parity D1] BulletConfiguration.velocity = EntityBulletBaseNT#motionMult() (EntityBullet: 1). */
        public double velocityMultiplier() {
            return switch (this) {
                case WORM_BOLT, WORM_LASER, UFO_LASER, MASKMAN_TRACER -> 5.0D; // standardBulletConfig
                case UFO_ROCKET -> 2.0D; // standardRocketConfig
                case MASKMAN_ORB -> 0.25D;
                default -> 1.0D;
            };
        }
        /** [parity D1] BulletConfiguration.doesPenetrate (standardBulletConfig = true); also doesBreakGlass. */
        public boolean penetrates() {
            return this == WORM_BOLT || this == WORM_LASER || this == UFO_LASER || this == MASKMAN_TRACER;
        }
        /** [parity D1] doesRicochet with HBRC 2 (worm bolts switch it off; grenade configs have HBRC 0). */
        public boolean ricochets() {
            return this == UFO_ROCKET || this == MASKMAN_TRACER;
        }
        /** [parity D1] BulletConfiguration.vPFX: "reddust" / "smoke" (grenade default) / none. */
        public net.minecraft.core.particles.ParticleOptions trailParticle() {
            return switch (this) {
                case UFO_ROCKET, MASKMAN_TRACER -> net.minecraft.core.particles.DustParticleOptions.REDSTONE;
                case MASKMAN_ROCKET, MASKMAN_METEOR -> net.minecraft.core.particles.ParticleTypes.SMOKE;
                default -> null;
            };
        }
        private int maxAge() { return maxAge; }
        private double gravity() { return gravity; }
        private float damage(net.minecraft.util.RandomSource random) {
            if (this == CHOPPER_BULLET) {
                return minDamage + random.nextInt((int) (maxDamage - minDamage + 1.0F));
            }
            // [parity A2] EntityBulletBaseNT: rand.nextFloat() * (dmgMax - dmgMin) + dmgMin
            return minDamage + random.nextFloat() * (maxDamage - minDamage);
        }
        private net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> damageType() {
            return switch (this) {
                case CHOPPER_BULLET -> com.atomiclegacy.registry.HbmDamageTypes.EMPLACER;
                case CRAB_TAU -> com.atomiclegacy.registry.HbmDamageTypes.TAU;
                case UFO_ROCKET, MASKMAN_ORB, MASKMAN_ROCKET, MASKMAN_METEOR -> com.atomiclegacy.registry.HbmDamageTypes.REVOLVER_BULLET;
                default -> com.atomiclegacy.registry.HbmDamageTypes.LASER;
            };
        }
        public float minDamage() { return minDamage; }
        public float maxDamage() { return maxDamage; }
        public double projectileSpeed() { return speed; }
    }
}
