package com.atomiclegacy.entity;

import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.blocks.LegacyBarrelBlock;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public class LegacyBulletEntity extends Entity {
    private static final EntityDataAccessor<Integer> AMMO_TYPE =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BASE_DAMAGE =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BALEFIRE =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NO_PROJECTILE_GRAVITY =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID targetUuid;
    private UUID ownerUuid;
    /** EntityThrowableNT#thrower: direct reference while loaded, the UUID survives saving. */
    private Entity ownerCache;
    private int ricochets;
    /**
     * [weapons batch E1] EntityBulletBaseMK4#accel and the unscaled EntityThrowableNT motion vector
     * (motionX/Y/Z; the per-tick displacement is motion * (velocity + accel)). Only the XFactoryRocket
     * onUpdate lambdas use them; every other round keeps the displacement in the delta movement.
     */
    public double accel;
    public Vec3 legacyMotion;
    /**
     * [weapons batch F3] EntityBulletBaseMK4#velocity (blocks moved during the last tick, both sides) and the
     * EntityThrowableNT getStuck state (ct_hook): a stuck bullet keeps its position, skips collisions and
     * gravity, despawns after groundDespawn() = 1200 ticks in the ground and is released with 0-20 % of its
     * motion when the block it is stuck in changes.
     */
    public double velocity;
    public boolean inGround;
    /** [parity S4: culling] Entity#ignoreFrustumCheck of 1.7.10 (charge thrower hook); used by the renderer's finite envelope. */
    public boolean ignoreFrustumCheck;
    private BlockPos stuckPos = BlockPos.ZERO;
    private net.minecraft.world.level.block.Block stuckBlock = Blocks.AIR;
    private int ticksInGround;

    /** EntityThrowableNT#getStuck(x, y, z, side). [weapons batch F3] */
    public void getStuck(BlockPos pos, Direction side) {
        this.stuckPos = pos.immutable();
        this.stuckBlock = level().getBlockState(pos).getBlock();
        this.inGround = true;
        setDeltaMovement(Vec3.ZERO);
    }

    public BlockPos stuckPos() {
        return this.stuckPos;
    }

    public LegacyBulletEntity(EntityType<? extends LegacyBulletEntity> type, Level level) {
        super(type, level);
    }

    public LegacyBulletEntity(Level level, double x, double y, double z, Vec3 direction, StandardAmmoItem.StandardAmmoType ammo, float baseDamage) {
        this(level, x, y, z, direction, ammo, baseDamage, null, 0.0F);
    }

    /**
     * EntityBulletBaseMK4's constructor: apply its Gaussian heading spread,
     * then the BulletConfig velocity.  Mob firearm callers also retain the
     * owner so BulletConfig.selfDamageDelay is respected.
     */
    public LegacyBulletEntity(Level level, double x, double y, double z, Vec3 direction,
                              StandardAmmoItem.StandardAmmoType ammo, float baseDamage,
                              Entity owner, float spread) {
        this(HbmEntityTypes.LEGACY_BULLET.get(), level);
        setPos(x, y, z);
        setAmmoType(ammo);
        if (com.atomiclegacy.items.weapon.sedna.factory.XFactoryFlamer.isBalefireFlame(ammo)) setBalefire(true); // [weapons batch F1: balefire flame rounds]
        setBaseDamage(baseDamage * ammo.damageMultiplier());
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.ownerCache = owner;
        // EntityBulletBaseMK4#headingForceMult() returns 1D (EntityThrowableNT's 0.0075D is overridden).
        Vec3 heading = direction.normalize().add(
                random.nextGaussian() * 1.0D * spread,
                random.nextGaussian() * 1.0D * spread,
                random.nextGaussian() * 1.0D * spread
        );
        double speed = ammo.projectileSpeed();
        this.legacyMotion = heading; // [weapons batch E1]
        Vec3 motion = heading.scale(speed);
        setDeltaMovement(motion);
        updateRotationFromMotion(heading);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AMMO_TYPE, StandardAmmoItem.StandardAmmoType.NONE.ordinal());
        builder.define(BASE_DAMAGE, 0.0F);
        builder.define(BALEFIRE, false);
        builder.define(NO_PROJECTILE_GRAVITY, false);
    }

    public StandardAmmoItem.StandardAmmoType ammoType() {
        StandardAmmoItem.StandardAmmoType[] values = StandardAmmoItem.StandardAmmoType.values();
        int ordinal = this.entityData.get(AMMO_TYPE);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : StandardAmmoItem.StandardAmmoType.NONE;
    }

    /**
     * EntityThrowableNT#getThrower: the shooter is the indirect attacker of
     * BulletConfig.getDamage (EntityDamageSourceIndirect(bullet, thrower)).
     */
    public Entity ownerEntity() {
        if (this.ownerCache != null && !this.ownerCache.isRemoved()) {
            return this.ownerCache;
        }
        if (this.ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(this.ownerUuid);
    }

    /** EntityBulletBaseMK4#damage (already multiplied with BulletConfig.damageMult). */
    public float baseDamage() {
        return this.entityData.get(BASE_DAMAGE);
    }

    public void setDamage(float damage) {
        setBaseDamage(damage);
    }

    public boolean balefire() {
        return this.entityData.get(BALEFIRE);
    }

    public void setBalefire(boolean balefire) {
        this.entityData.set(BALEFIRE, balefire);
    }

    public void setNoProjectileGravity(boolean noProjectileGravity) {
        this.entityData.set(NO_PROJECTILE_GRAVITY, noProjectileGravity);
    }

    public void setLockonTarget(Entity target) {
        this.targetUuid = target == null ? null : target.getUUID();
    }

    private void setAmmoType(StandardAmmoItem.StandardAmmoType ammo) {
        this.entityData.set(AMMO_TYPE, ammo.ordinal());
    }

    private void setBaseDamage(float damage) {
        this.entityData.set(BASE_DAMAGE, damage);
    }

    /* EntityThrowableInterp sync state (setPositionAndRotation2). */
    private int turnProgress;
    private double syncPosX;
    private double syncPosY;
    private double syncPosZ;
    private double syncYaw;
    private double syncPitch;

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yRot;
        this.syncPitch = xRot;
        this.turnProgress = steps;
    }

    /*
     * 1.7.10 sent a tracked entity's position at the end of the tick it moved and the
     * destroy packet only with the next player update, so the client kept interpolating
     * a bullet for about two ticks after the server killed it. 1.21 sends the removal in
     * the same tick as the last position, so the client dropped short-range bullets
     * before drawing any tracer. A server-side bullet therefore stays inert for this many
     * ticks after discard(), without moving, colliding or dealing damage.
     */
    private static final int CLIENT_REMOVAL_DELAY = 2;
    private int retireTicks = -1;

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.DISCARDED && !level().isClientSide
                && this.retireTicks < 0 && !isRemoved()) {
            this.retireTicks = CLIENT_REMOVAL_DELAY;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.remove(reason);
    }

    /** EntityBulletBaseMK4#ricochets, used by custom onRicochet lambdas. [weapons batch D1] */
    public int ricochets() {
        return this.ricochets;
    }

    public void setRicochets(int ricochets) {
        this.ricochets = ricochets;
    }

    public boolean isRetired() {
        return this.retireTicks >= 0;
    }

    @Override
    public void tick() {
        if (this.retireTicks >= 0) {
            this.xo = getX();
            this.yo = getY();
            this.zo = getZ();
            if (--this.retireTicks < 0) {
                super.remove(Entity.RemovalReason.DISCARDED);
            }
            return;
        }
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();

        StandardAmmoItem.StandardAmmoType ammo = ammoType();
        // EntityBulletBaseMK4 extends EntityThrowableInterp: the client never simulates
        // flight or collisions, it only interpolates towards the server positions.
        // Flame projectiles keep their separate client visual path below.
        if (level().isClientSide && ammo.family() != StandardAmmoItem.AmmoFamily.FLAME) {
            if (this.turnProgress > 0) {
                double interpX = getX() + (this.syncPosX - getX()) / (double) this.turnProgress;
                double interpY = getY() + (this.syncPosY - getY()) / (double) this.turnProgress;
                double interpZ = getZ() + (this.syncPosZ - getZ()) / (double) this.turnProgress;
                double d = Mth.wrapDegrees(this.syncYaw - (double) getYRot());
                setYRot((float) ((double) getYRot() + d / (double) this.turnProgress));
                setXRot((float) ((double) getXRot() + (this.syncPitch - (double) getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                setPos(interpX, interpY, interpZ);
            }
            updateVelocity(); // [weapons batch F3]
            if (ammo.onClientUpdate() != null) ammo.onClientUpdate().onUpdate(this); // [weapons batch F3]
            return;
        }
        // [weapons batch F3] EntityThrowableNT#onUpdate inGround branch, then EntityBulletBaseMK4's expiry and onUpdate
        if (this.inGround) {
            if (level().getBlockState(this.stuckPos).getBlock() == this.stuckBlock) {
                this.velocity = 0.0D;
                if (++this.ticksInGround == 1200) { // groundDespawn()
                    discard();
                    return;
                }
                if (tickCount > ammo.projectileLifetime()) {
                    discard();
                    return;
                }
                if (ammo.onUpdate() != null && !isRetired()) {
                    ammo.onUpdate().onUpdate(this);
                }
                return;
            }
            this.inGround = false;
            setDeltaMovement(getDeltaMovement().multiply(this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F));
            this.ticksInGround = 0;
        }
        Vec3 motion = getDeltaMovement();
        updateRotationFromMotion(motion);
        // EntityThrowableInterp in 1.7.10 never performed collision prediction
        // on the client.  In particular, a flame projectile remained alive
        // client-side until the authoritative server removal arrived, so its
        // onUpdate callback could emit one flame particle every tick.  Keep
        // that behavior for flame projectiles; local collision prediction was
        // deleting their visual trail before the server entity was finished.
        boolean clientVisualFlame = level().isClientSide
                && ammo.family() == StandardAmmoItem.AmmoFamily.FLAME;
        if (!clientVisualFlame) {
            Vec3 start = position();
            Vec3 end = start.add(motion);
            HitResult blockHit = ammo.spectral()
                    ? net.minecraft.world.phys.BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end))
                    : level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
            List<EntityHitResult> entityHits = findEntityHits(start, entityEnd);

            if (!entityHits.isEmpty()) {
                if (ammo.penetrates()) {
                    for (EntityHitResult entityHit : entityHits) {
                        if (impactEntity(ammo, entityHit)) {
                            return;
                        }
                    }
                    if (blockHit instanceof BlockHitResult blockResult && blockHit.getType() != HitResult.Type.MISS) {
                        if (impactBlock(ammo, blockResult)) {
                            return;
                        }
                        motion = getDeltaMovement();
                    }
                } else if (impactEntity(ammo, entityHits.getFirst())) {
                    return;
                }
            } else if (blockHit instanceof BlockHitResult blockResult && blockHit.getType() != HitResult.Type.MISS) {
                if (impactBlock(ammo, blockResult)) {
                        return;
                }
                motion = getDeltaMovement();
            }
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateVelocity(); // [weapons batch F3]
        if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
            setDeltaMovement(nextRocketMotion(motion));
        } else if (!this.entityData.get(NO_PROJECTILE_GRAVITY) && ammo.projectileGravity() != 0.0D) {
            // EntityThrowableNT: motionY -= gravity on the unscaled motion, displacement = motion * motionMult()
            // (BulletConfig.velocity), so per tick the displacement loses gravity * velocity.
            setDeltaMovement(motion.add(0.0D, -ammo.projectileGravity() * ammo.projectileSpeed(), 0.0D));
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME && level().isClientSide) {
            level().addParticle(
                    balefire() ? HbmParticleTypes.FLAMETHROWER_BALEFIRE.get() : HbmParticleTypes.FLAMETHROWER_FIRE.get(),
                    getX(), getY() - 0.125D, getZ(), 0.0D, 0.0D, 0.0D
            );
        }
        if (!level().isClientSide && ammo.onUpdate() != null && !isRetired()) {
            ammo.onUpdate().onUpdate(this);
        }
        int maxLife = ammo.projectileLifetime();
        if (!level().isClientSide && (tickCount > maxLife || getY() < level().getMinBuildHeight() - 16)) {
            discard();
        }
    }

    private Vec3 nextRocketMotion(Vec3 previousMotion) {
        Vec3 direction = previousMotion.lengthSqr() > 1.0E-12D
                ? previousMotion.normalize()
                : directionFromRotation();
        Entity target = lockonTarget();
        if (target != null && target.isAlive()) {
            Vec3 delta = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(position());
            double turn = Math.min(0.005D * this.tickCount, 1.0D);
            direction = direction.lerp(delta, turn).normalize();
        }
        double speed = previousMotion.length();
        if (speed < 7.0D) {
            speed += 0.4D;
        }
        return direction.scale(speed);
    }

    private Vec3 directionFromRotation() {
        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(getXRot());
        double horizontal = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * horizontal, -Math.sin(pitch), Math.cos(yaw) * horizontal).normalize();
    }

    /** EntityBulletBaseMK4#lockonTarget (server side). */
    public Entity lockonTarget() {
        if (this.targetUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(this.targetUuid);
    }

    private boolean impactEntity(StandardAmmoItem.StandardAmmoType ammo, EntityHitResult entityHit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        Entity hit = entityHit.getEntity();
        if (ammo.onImpact() != null) {
            if (ammo.onImpact().onImpact(this, entityHit)) {
                discard();
                return true;
            }
            if (isRemoved() || isRetired()) {
                return true;
            }
        }
        // [weapons batch D1] BulletConfig.onEntityHit replacement (shredder submunitions)
        if (ammo.onEntityHit() != null) {
            if (ammo.onEntityHit().onImpact(this, entityHit)) {
                if (!isRemoved() && !isRetired()) discard();
                return true;
            }
            return isRemoved() || isRetired();
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
            if (this.tickCount < 3) {
                return false;
            }
            LegacyProjectileUtil.rocketImpact(ammo, this, entityHit.getLocation(), hit);
            discard();
            return true;
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME) {
            if (hit instanceof LivingEntity living) {
                HbmLivingHazards hazards = HbmLivingHazards.get(living);
                if (balefire()) {
                    hazards.extendBalefire(200);
                } else {
                    hazards.extendFire(100);
                }
            }
            // [weapons batch F1: flame entity hit] LAMBDA_STANDARD_ENTITY_HIT: headshotMult, damage source with the shooter
            float flameDamage = this.entityData.get(BASE_DAMAGE);
            if (hit instanceof LivingEntity headTarget && headTarget.isAlive() && ammo.headshotMultiplier() > 1.0F) {
                double head = headTarget.getBbHeight() - headTarget.getEyeHeight();
                if (entityHit.getLocation().y > headTarget.getY() + headTarget.getBbHeight() - head * 2.0D) flameDamage *= ammo.headshotMultiplier();
            }
            LegacyProjectileUtil.hurtSednaFire(hit, this, ownerEntity(), flameDamage);
            discard();
            return true;
        }
        // BulletConfig.onImpact (XFactory50.LAMBDA_STANDARD_EXPLODE) runs before the standard entity hit.
        if (ammo.explodesOnImpact() && com.atomiclegacy.items.weapon.sedna.factory.XFactory50.standardExplode(
                this, hit, entityHit.getLocation(), null, this.entityData.get(BASE_DAMAGE))) {
            discard();
            return true;
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.FLARE && hit instanceof LivingEntity living) {
            // XFactory40mm.LAMBDA_STANDARD_IGNITE affects entities only: props.fire += 200.
            HbmLivingHazards hazards = HbmLivingHazards.get(living);
            hazards.addFire(200);
            HbmLivingHazards.set(living, hazards);
        }
        LegacyProjectileUtil.SednaImpact impact = LegacyProjectileUtil.bulletImpact(
                ammo,
                this,
                hit,
                entityHit.getLocation(),
                this.entityData.get(BASE_DAMAGE)
        );
        setBaseDamage(impact.remainingDamage());
        if (impact.continueFlight()) {
            return false;
        }
        discard();
        return true;
    }

    private boolean impactBlock(StandardAmmoItem.StandardAmmoType ammo, BlockHitResult hit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        if (ammo.onImpact() != null) {
            if (ammo.onImpact().onImpact(this, hit)) {
                discard();
                return true;
            }
            if (isRemoved() || isRetired() || this.inGround) { // [weapons batch F3] EntityBulletBaseMK4#onImpact: if(isDead || inGround) return
                return true;
            }
        }
        // [weapons batch D1] BulletConfig.onRicochet replacement (shredder submunitions)
        if (ammo.onRicochet() != null) {
            if (ammo.onRicochet().onImpact(this, hit)) {
                if (!isRemoved() && !isRetired()) discard();
                return true;
            }
            return isRemoved() || isRetired();
        }
        if (ammo.explodesOnImpact()) {
            // XFactory50.LAMBDA_STANDARD_EXPLODE: block hits always explode, before the ricochet logic
            com.atomiclegacy.items.weapon.sedna.factory.XFactory50.standardExplode(
                    this, null, hit.getLocation(), hit.getDirection(), this.entityData.get(BASE_DAMAGE));
            discard();
            return true;
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
            LegacyProjectileUtil.rocketImpact(ammo, this, hit.getLocation(), null);
        } else if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME) {
            if (balefire()) {
                LegacyProjectileUtil.spawnLingeringFire(
                        this,
                        hit.getLocation(),
                        LegacyLingeringFireEntity.FireType.BALEFIRE,
                        3.0F,
                        1.0F,
                        300,
                        true
                );
                discard();
                return true;
            }
            BlockPos blockPos = hit.getBlockPos();
            Direction side = hit.getDirection();
            BlockPos firePos = blockPos.relative(side);
            BlockState state = level().getBlockState(blockPos);
            if (state.isFlammable(level(), blockPos, side.getOpposite()) && level().isEmptyBlock(firePos)) {
                level().setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                setPos(hit.getLocation());
                return false;
            }
            LegacyProjectileUtil.spawnLingeringFire(
                    this,
                    hit.getLocation(),
                    LegacyLingeringFireEntity.FireType.DIESEL,
                    2.0F,
                    1.0F,
                    100,
                    true
            );
        } else if (isSednaBullet(ammo)) {
            return ricochetOrStop(ammo, hit);
        }
        BlockState hitState = level().getBlockState(hit.getBlockPos());
        if (hitState.getBlock() instanceof LegacyBarrelBlock barrel && barrel.detonatesWhenShot()) {
            barrel.detonateOnShot((net.minecraft.server.level.ServerLevel) level(), hit.getBlockPos(), this);
            discard();
            return true;
        }
        discard();
        return true;
    }

    private boolean ricochetOrStop(StandardAmmoItem.StandardAmmoType ammo, BlockHitResult hit) {
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (state.getSoundType(level(), hit.getBlockPos(), this) == SoundType.GLASS) {
            level().destroyBlock(hit.getBlockPos(), false, this);
            setPos(hit.getLocation());
            return false;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        double grazingAngle = Math.toDegrees(Math.asin(Mth.clamp(Math.abs(motion.normalize().dot(normal)), 0.0D, 1.0D)));
        if (grazingAngle > ammo.ricochetAngle() || ++this.ricochets > ammo.maxRicochetCount()) {
            discard();
            return true;
        }

        Vec3 reflected = switch (hit.getDirection().getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        };
        setDeltaMovement(reflected);
        setPos(hit.getLocation());
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_RICOCHET.get(), SoundSource.PLAYERS, 0.25F, 1.0F);
        return false;
    }

    private static boolean isSednaBullet(StandardAmmoItem.StandardAmmoType ammo) {
        return ammo.family() == StandardAmmoItem.AmmoFamily.BMG50
                || ammo.family() == StandardAmmoItem.AmmoFamily.R556
                || ammo.family() == StandardAmmoItem.AmmoFamily.P9
                || ammo.family() == StandardAmmoItem.AmmoFamily.P22
                || ammo.family() == StandardAmmoItem.AmmoFamily.LEGACY_BULLET
                || ammo.family() == StandardAmmoItem.AmmoFamily.FLARE
                || ammo.family() == StandardAmmoItem.AmmoFamily.B75; // [weapons batch E1] b75 rounds ricochet
    }

    private List<EntityHitResult> findEntityHits(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        List<EntityHitResult> hits = new java.util.ArrayList<>();
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive()
                && entity.isPickable() && !isProtectedOwner(entity))) {
            AABB box = entity.getBoundingBox().inflate(0.3D);
            Optional<Vec3> optionalHit = box.clip(start, end);
            // [weapons batch D1] 1.7.10 AxisAlignedBB#calculateIntercept also yields the exit point of a
            // segment that starts inside the box (shredder submunitions spawn inside the entity the beam hit).
            if (optionalHit.isEmpty() && box.contains(start)) {
                optionalHit = box.clip(end, start);
            }
            if (optionalHit.isEmpty()) {
                continue;
            }
            hits.add(new EntityHitResult(entity, optionalHit.get()));
        }
        if (!ammoType().penetrates()) {
            hits.sort(java.util.Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())));
        }
        return hits;
    }

    private boolean isProtectedOwner(Entity entity) {
        return this.tickCount < ammoType().selfDamageDelay() && this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID());
    }

    /** [weapons batch F3] EntityBulletBaseMK4#onUpdate: velocity = distance to the previous tick position. */
    private void updateVelocity() {
        double dX = getX() - this.xo;
        double dY = getY() - this.yo;
        double dZ = getZ() - this.zo;
        this.velocity = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    private void updateRotationFromMotion(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    /** EntityBullet#canTriggerWalking returned false in 1.7.10. */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ammo_type", this.entityData.get(AMMO_TYPE));
        tag.putFloat("base_damage", this.entityData.get(BASE_DAMAGE));
        tag.putBoolean("balefire", this.entityData.get(BALEFIRE));
        tag.putBoolean("no_projectile_gravity", this.entityData.get(NO_PROJECTILE_GRAVITY));
        tag.putInt("ricochets", this.ricochets);
        // [weapons batch F3] EntityThrowableNT xTile/yTile/zTile/inTile/inGround
        tag.putBoolean("inGround", this.inGround);
        tag.putInt("xTile", this.stuckPos.getX());
        tag.putInt("yTile", this.stuckPos.getY());
        tag.putInt("zTile", this.stuckPos.getZ());
        tag.putString("inTile", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this.stuckBlock).toString());
        if (this.ownerUuid != null) {
            tag.putUUID("owner", this.ownerUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(AMMO_TYPE, tag.getInt("ammo_type"));
        this.entityData.set(BASE_DAMAGE, tag.getFloat("base_damage"));
        this.entityData.set(BALEFIRE, tag.getBoolean("balefire"));
        this.entityData.set(NO_PROJECTILE_GRAVITY, tag.getBoolean("no_projectile_gravity"));
        this.ricochets = tag.getInt("ricochets");
        this.inGround = tag.getBoolean("inGround"); // [weapons batch F3]
        this.stuckPos = new BlockPos(tag.getInt("xTile"), tag.getInt("yTile"), tag.getInt("zTile"));
        this.stuckBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.tryParse(tag.getString("inTile")));
        this.ownerUuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        this.targetUuid = null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityBullet sets renderDistanceWeight = 10.  Its 0.5 x 0.5 x 0.5
        // box therefore renders through the legacy 320-block squared range.
        return distance < 102400.0D;
    }
}
