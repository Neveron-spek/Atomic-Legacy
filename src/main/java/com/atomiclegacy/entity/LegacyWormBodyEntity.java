package com.atomiclegacy.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** [parity A2: BOT prime] EntityBOTPrimeBody: follows the segment ahead, forwards damage to it, fires worm bolts. */
public final class LegacyWormBodyEntity extends LegacyWormBaseEntity {
    public static final double RANGE_FOR_PARTS = 70.0D;
    public static final double SEGMENT_DISTANCE = 3.5D;
    public static final double MAX_BODY_SPEED = 1.4D;

    private UUID previousUuid;
    private int partNumber;
    private LivingEntity followed;
    private boolean didCheck;

    public LegacyWormBodyEntity(EntityType<? extends LegacyWormBodyEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15_000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new NearestPlayerTargetGoal(this, 128.0D));
    }

    public void initialize(UUID headUuid, UUID previousUuid, int partNumber) {
        this.headUuid = headUuid;
        this.previousUuid = previousUuid;
        this.partNumber = partNumber;
    }

    public int partNumber() {
        return partNumber;
    }

    @Override
    public boolean isWormHead() {
        return false;
    }

    @Override
    protected float attackStrength(Entity target) {
        return target instanceof LivingEntity living ? living.getHealth() * 0.75F : 100.0F;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        faceTargeted();
    }

    /** EntityBOTPrimeBody#updateAITasks. */
    @Override
    protected void customServerAiStep() {
        wormActionState();
        updateMovement();
        if (didCheck) {
            if (targetedEntity == null || !targetedEntity.isAlive()) {
                setHealth(getHealth() - 1999.0F);
            }
            if ((followed == null || !followed.isAlive()) && random.nextInt(60) == 0) {
                level().explode(this, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.NONE);
            }
        }
        LivingEntity attackTarget = getTarget();
        if (followed != null && followed.isAlive() && attackTarget != null) {
            if (canSeeThroughNonSolids(attackTarget)) {
                attackCounter += 1;
                if (attackCounter == 10) {
                    laserAttack(attackTarget, false);
                    attackCounter = -20;
                }
            } else if (attackCounter > 0) {
                attackCounter -= 1;
            }
        } else if (attackCounter > 0) {
            attackCounter -= 1;
        }
        faceTargeted();
    }

    private void faceTargeted() {
        if (targetedEntity == null) return;
        double dx = targetedEntity.getX() - getX();
        double dy = targetedEntity.getY() - getY();
        double dz = targetedEntity.getZ() - getZ();
        float horizontal = Mth.sqrt((float) (dx * dx + dz * dz));
        setYRot((float) (Math.atan2(dx, dz) * 180.0D / Math.PI));
        setXRot((float) (Math.atan2(dy, horizontal) * 180.0D / Math.PI));
        yRotO = getYRot();
        xRotO = getXRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /** EntityWormBaseNT#updateMovement. */
    private void updateMovement() {
        double targetingRange = 128.0D;
        if (targetedEntity != null && targetedEntity.distanceToSqr(this) < targetingRange * targetingRange) {
            waypointX = targetedEntity.getX();
            waypointY = targetedEntity.getY();
            waypointZ = targetedEntity.getZ();
        }
        if ((tickCount % 60 == 0 || tickCount == 1) && (targetedEntity == null || followed == null)) {
            findEntityToFollow();
        }
        double deltaX = waypointX - getX();
        double deltaY = waypointY - getY();
        double deltaZ = waypointZ - getZ();
        double deltaDist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double speed = Math.max(0.0D, Math.min(deltaDist - SEGMENT_DISTANCE, MAX_BODY_SPEED));
        if (deltaDist < SEGMENT_DISTANCE * 0.895D) {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
        } else {
            setDeltaMovement(new Vec3(deltaX / deltaDist * speed, deltaY / deltaDist * speed, deltaZ / deltaDist * speed));
        }
    }

    /** EntityWormBaseNT#findEntityToFollow: same-worm segments whose box touches this box expanded by rangeForParts. */
    private void findEntityToFollow() {
        AABB area = getBoundingBox().inflate(RANGE_FOR_PARTS);
        Entity head = byUuid(headUuid);
        if (head instanceof LegacyWormHeadEntity wormHead && !head.isRemoved() && area.intersects(head.getBoundingBox())) {
            if (partNumber == 0) {
                targetedEntity = wormHead;
            }
            followed = wormHead;
        }
        if (partNumber > 0) {
            Entity previous = byUuid(previousUuid);
            if (previous instanceof LegacyWormBodyEntity && !previous.isRemoved() && area.intersects(previous.getBoundingBox())) {
                targetedEntity = previous;
            }
        }
        // Tech adaptation of the UUID link: on the very first tick after a chunk load the neighbour may not be
        // loaded yet, so the "checked" flag (which starts the 1999/tick decay) waits for a hit or the next search.
        if (targetedEntity != null || tickCount > 1) {
            didCheck = true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (previousUuid != null) tag.putUUID("previous", previousUuid);
        tag.putInt("part", partNumber);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        previousUuid = tag.hasUUID("previous") ? tag.getUUID("previous") : null;
        partNumber = tag.getInt("part");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityBOTPrimeBase renderDistanceWeight = 15; this registration is 2 blocks wide.
        return distance < 3_686_400.0D;
    }
}
