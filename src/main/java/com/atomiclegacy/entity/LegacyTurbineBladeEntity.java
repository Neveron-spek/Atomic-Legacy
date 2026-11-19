package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * [parity A1] 1.7.10 {@code EntityBulletBaseNT} ("entity_bullet_mk3") with {@code BulletConfigSyncingUtil.TURBINE}
 * = {@code GunEnergyFactory.getTurbineConfig()}, the only configuration still spawned by the selected source
 * (EntityMissileCustom, warhead TURBINE). Config: dmgMin 100, dmgMax 150, velocity 1, gravity 0, maxAge 200,
 * STYLE_BLADE, destroysBlocks, no ricochet (HBRC 0), no penetration, no incendiary/explosive/effects,
 * damageType "revolverBullet" (projectile), air and water drag 1, not saved to NBT.
 * Movement and collision follow EntityThrowableNT.onUpdate (block ray, then the nearest entity whose box inflated
 * by 0.3 intersects the segment, searched in the box swept by the motion and expanded by 1).
 */
public final class LegacyTurbineBladeEntity extends Entity {
    public static final int MAX_AGE = 200;
    public static final float DMG_MIN = 100.0F;
    public static final float DMG_MAX = 150.0F;
    /** EntityBulletBaseNT.onBlockImpact: destroysBlocks breaks blocks with getBlockHardness <= 120 (no drops). */
    public static final float MAX_BREAK_HARDNESS = 120.0F;

    private static Field lastHurtField;

    public LegacyTurbineBladeEntity(EntityType<? extends LegacyTurbineBladeEntity> type, Level level) {
        super(type, level);
    }

    /** blade.setPositionAndRotation(x, y, z, 0, 0); blade.motionX/Z = vec (motionY stays 0). */
    public LegacyTurbineBladeEntity(Level level, double x, double y, double z, double motionX, double motionZ) {
        this(HbmEntityTypes.LEGACY_TURBINE_BLADE.get(), level);
        moveTo(x, y, z, 0.0F, 0.0F);
        setDeltaMovement(motionX, 0.0D, motionZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        // ticksExisted > maxAge is tested before Entity.onUpdate increments it; 1.21 increments tickCount before tick()
        if (!level().isClientSide && tickCount - 1 > MAX_AGE) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide) {
            return; // EntityThrowableInterp: the client only interpolates synced positions
        }

        Vec3 motion = getDeltaMovement(); // motionMult() = velocity 1
        Vec3 pos = position();
        Vec3 nextPos = pos.add(motion);
        // func_147447_a(pos, nextPos, false, true, false): no liquids, blocks without a collision box ignored
        BlockHitResult blockHit = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        HitResult mop = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
        if (mop != null) {
            nextPos = mop.getLocation();
        }

        Entity hitEntity = null;
        Vec3 hitVec = null;
        double nearest = 0.0D;
        AABB search = getBoundingBox().expandTowards(motion).inflate(1.0D);
        for (Entity entity : level().getEntities(this, search)) {
            if (!entity.isPickable() || !entity.isAlive()) {
                continue; // thrower is null, so the selfDamageDelay test never excludes anyone
            }
            Optional<Vec3> clip = entity.getBoundingBox().inflate(0.3D).clip(pos, nextPos);
            if (clip.isPresent()) {
                double dist = pos.distanceTo(clip.get());
                if (dist < nearest || nearest == 0.0D) {
                    hitEntity = entity;
                    nearest = dist;
                    hitVec = clip.get();
                }
            }
        }

        if (hitEntity != null) {
            onEntityImpact(hitEntity, hitVec);
        } else if (mop != null) {
            onBlockImpact(blockHit);
        }

        updateRotation(motion);
        // posX += motionX * motionMult(); drag 1; gravity 0
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    /** onImpact(BLOCK): HBRC 0 and doesRicochet false -> setPosition(hitVec), onBlockImpact, setDead. */
    private void onBlockImpact(BlockHitResult hit) {
        setPos(hit.getLocation());
        BlockPos blockPos = hit.getBlockPos();
        discard(); // bY > -1 && !inGround
        if (breaks(level(), blockPos)) {
            level().destroyBlock(blockPos, false, this); // func_147480_a(x, y, z, false)
        }
    }

    /**
     * onImpact(entity): not penetrating -> setPosition(hitVec), onEntityImpact (onBlockImpact(-1, -1, -1) only kills
     * the bullet), then damage = rand(dmgMin..dmgMax); if attackEntityFrom fails it is retried with
     * damage + lastDamage (reflection, as in the original).
     */
    private void onEntityImpact(Entity victim, Vec3 hitVec) {
        setPos(hitVec);
        discard();
        float damage = random.nextFloat() * (DMG_MAX - DMG_MIN) + DMG_MIN;
        DamageSource source = level().damageSources().source(HbmDamageTypes.REVOLVER_BULLET);
        if (!victim.hurt(source, damage) && victim instanceof LivingEntity living) {
            try {
                if (lastHurtField == null) {
                    lastHurtField = ObfuscationReflectionHelper.findField(LivingEntity.class, "lastHurt");
                }
                victim.hurt(source, damage + lastHurtField.getFloat(living));
            } catch (Exception ignored) {
            }
        }
    }

    /** EntityThrowableNT rotation: target atan2 angles approached by 20 % per tick (not while onGround). */
    private void updateRotation(Vec3 motion) {
        float hyp = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, hyp) * 180.0D / Math.PI);
        float prevPitch = xRotO;
        float prevYaw = yRotO;
        while (pitch - prevPitch < -180.0F) prevPitch -= 360.0F;
        while (pitch - prevPitch >= 180.0F) prevPitch += 360.0F;
        while (yaw - prevYaw < -180.0F) prevYaw -= 360.0F;
        while (yaw - prevYaw >= 180.0F) prevYaw += 360.0F;
        setXRot(prevPitch + (pitch - prevPitch) * 0.2F);
        setYRot(prevYaw + (yaw - prevYaw) * 0.2F);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityThrowableNT.isInRangeToRenderDist: (0.5 average edge * 4 * 64)^2
        return distance < 16_384.0D;
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // writeToNBTOptional -> false
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard(); // readEntityFromNBT -> setDead
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /**
     * Hardness gate of onBlockImpact. 1.7.10 getBlockHardness is -1 for unbreakable blocks, which also passes
     * "<= 120"; getDestroySpeed is -1 for the same blocks, so that behaviour of the original is kept.
     */
    public static boolean breaks(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getDestroySpeed(level, pos) <= MAX_BREAK_HARDNESS;
    }
}
