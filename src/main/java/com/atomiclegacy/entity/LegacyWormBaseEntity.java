package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * [parity A2: BOT prime] EntityBurrowingNT + EntityWormBaseNT + EntityBOTPrimeBase.
 * Segments are linked by UUID instead of the session-local entity id of 1.7.10 (ids do not survive a reload).
 */
public abstract class LegacyWormBaseEntity extends Monster {
    protected static final float DRAG_IN_AIR = 0.995F;
    protected static final float DRAG_IN_GROUND = 0.98F;
    protected static final double KNOCKBACK_DIVIDER = 1.0D;

    protected UUID headUuid;
    /** EntityWormBaseNT#targetedEntity: the head's prey, or the segment a body part follows. */
    protected Entity targetedEntity;
    protected double waypointX;
    protected double waypointY;
    protected double waypointZ;
    public int attackCounter;

    protected LegacyWormBaseEntity(EntityType<? extends LegacyWormBaseEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public abstract boolean isWormHead();

    protected abstract float attackStrength(Entity target);

    public UUID headUuid() {
        return headUuid;
    }

    public boolean belongsTo(UUID otherHead) {
        return headUuid != null && headUuid.equals(otherHead);
    }

    private boolean sameWorm(Entity entity) {
        return entity instanceof LegacyWormBaseEntity worm && headUuid != null && headUuid.equals(worm.headUuid);
    }

    @Override
    public void tick() {
        noPhysics = true;
        setNoGravity(true);
        super.tick();
    }

    /** EntityWormBaseNT#attackEntityFrom. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)
                || sameWorm(source.getEntity())) {
            return false;
        }
        markHurt();
        if (isWormHead()) {
            return super.hurt(source, amount);
        }
        Entity ahead = targetedEntity;
        if (ahead != null) {
            return ahead.hurt(source, amount);
        }
        return super.hurt(source, amount);
    }

    /** EntityWormBaseNT#updateEntityActionState. */
    protected void wormActionState() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
        }
        if (targetedEntity != null && targetedEntity.isRemoved()) {
            targetedEntity = null;
        }
        Vec3 motion = getDeltaMovement();
        if (getY() < -10.0D) {
            setDeltaMovement(motion.x, 1.0D, motion.z);
        } else if (getY() < 3.0D) {
            setDeltaMovement(motion.x, 0.3D, motion.z);
        }
        if (tickCount % 5 == 0) {
            attackEntitiesInList(level().getEntities(this, getBoundingBox().inflate(0.5D)));
        }
    }

    private void attackEntitiesInList(List<Entity> targets) {
        for (Entity target : targets) {
            if (target instanceof LivingEntity && !sameWorm(target)) {
                attackAsWorm(target);
            }
        }
    }

    /** EntityWormBaseNT#attackEntityAsMob. */
    private void attackAsWorm(Entity target) {
        if (!target.hurt(damageSources().mobAttack(this), attackStrength(target))) {
            return;
        }
        setNoActionTime(0);
        AABB box = getBoundingBox();
        double deltaX = target.getX() - (box.minX + box.maxX) / 2.0D;
        double deltaZ = target.getZ() - (box.minZ + box.maxZ) / 2.0D;
        double deltaY = target.getY() - (box.minY + box.maxY) / 2.0D;
        double knockback = KNOCKBACK_DIVIDER * (deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY + 0.1D);
        target.push(deltaX / knockback, deltaY / knockback, deltaZ / knockback);
        if (target instanceof ServerPlayer) {
            target.hurtMarked = true;
        }
    }

    /** EntityWormBaseNT#addVelocity is empty. */
    @Override
    public void push(double x, double y, double z) {
    }

    /** EntityBurrowingNT#moveEntityWithHeading. */
    @Override
    public void travel(Vec3 input) {
        float drag = DRAG_IN_GROUND;
        if (!isInsideOpaqueBlock() && !isInWater() && !isInLava()) {
            drag = DRAG_IN_AIR;
        }
        if (!isWormHead()) {
            drag *= 0.9F;
        }
        moveRelative(0.02F, input);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(drag));
        calculateEntityAnimation(false);
    }

    /** 1.7.10 Entity#isEntityInsideOpaqueBlock (eight sample points around the eyes), without the noClip shortcut of 1.21. */
    protected boolean isInsideOpaqueBlock() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 8; i++) {
            float f = ((float) ((i >> 0) % 2) - 0.5F) * getBbWidth() * 0.8F;
            float f1 = ((float) ((i >> 1) % 2) - 0.5F) * 0.1F;
            float f2 = ((float) ((i >> 2) % 2) - 0.5F) * getBbWidth() * 0.8F;
            pos.set(net.minecraft.util.Mth.floor(getX() + f), net.minecraft.util.Mth.floor(getY() + getEyeHeight() + f1),
                    net.minecraft.util.Mth.floor(getZ() + f2));
            BlockState state = level().getBlockState(pos);
            if (state.isRedstoneConductor(level(), pos)) {
                return true;
            }
        }
        return false;
    }

    /** EntityBOTPrimeBase#canEntityBeSeenThroughNonSolids. */
    protected boolean canSeeThroughNonSolids(Entity entity) {
        HitResult result = level().clip(new ClipContext(
                new Vec3(getX(), getY() + getEyeHeight(), getZ()),
                new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS;
    }

    /** EntityBOTPrimeBase#laserAttack through EntityBulletBaseNT(world, config, shooter, target, motion, deviation). */
    protected void laserAttack(Entity target, boolean head) {
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        double sourceY = getY() + getEyeHeight() - 0.10000000149011612D;
        double dx = living.getX() - getX();
        double dy = living.getBoundingBox().minY + living.getBbHeight() / 3.0F - sourceY;
        double dz = living.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        Vec3 origin;
        Vec3 direction;
        if (horizontal >= 1.0E-7D) {
            origin = new Vec3(getX() + dx / horizontal, sourceY, getZ() + dz / horizontal);
            direction = new Vec3(dx, dy, dz);
        } else {
            origin = null;
            direction = null;
        }
        int count = head ? 5 : 1;
        if (origin != null) {
            for (int i = 0; i < count; i++) {
                level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin, direction,
                        head ? LegacyBossProjectileEntity.Type.WORM_LASER : LegacyBossProjectileEntity.Type.WORM_BOLT,
                        living, head ? i * 0.05F : 0.125F));
            }
        }
        playSound(HbmSoundEvents.WEAPON_BALLS_LASER.get(), 5.0F, head ? 0.75F : 1.0F);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundEvents.ENTITY_BOMB_DET.get();
    }

    /** EntityWormBaseNT#setDead plays the death sound whenever the segment is destroyed. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !level().isClientSide && !isRemoved()) {
            playSound(getDeathSound(), getSoundVolume(), getVoicePitch());
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (headUuid != null) tag.putUUID("head", headUuid);
        tag.putInt("attack", attackCounter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        headUuid = tag.hasUUID("head") ? tag.getUUID("head") : null;
        attackCounter = tag.getInt("attack");
    }

    protected Entity byUuid(UUID uuid) {
        return uuid != null && level() instanceof ServerLevel server ? server.getEntity(uuid) : null;
    }

    /**
     * EntityAINearestAttackableTargetNT(owner, EntityPlayer.class, 0, false, false, selector, range): no sight check;
     * as in the original the found player only becomes the attack target when the task resets.
     */
    protected static final class NearestPlayerTargetGoal extends Goal {
        private final LegacyWormBaseEntity owner;
        private final double range;
        private LivingEntity found;

        NearestPlayerTargetGoal(LegacyWormBaseEntity owner, double range) {
            this.owner = owner;
            this.range = range;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Player player : owner.level().getEntitiesOfClass(Player.class, owner.getBoundingBox().inflate(range))) {
                if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
                double distance = owner.distanceToSqr(player);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = player;
                }
            }
            if (best == null) {
                return false;
            }
            found = best;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = owner.getTarget();
            return target != null && target.isAlive() && owner.distanceToSqr(target) <= range * range;
        }

        @Override
        public void stop() {
            owner.setTarget(found);
        }
    }
}
