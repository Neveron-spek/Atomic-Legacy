package com.atomiclegacy.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.ToDoubleFunction;

/**
 * [parity J1] The 1.7.10 "old AI" of EntityCreature / EntityLiving (mobs whose isAIEnabled() is false: glyphids,
 * RAD beast, parasite maggot, ghost). One instance per mob; the mob calls the pieces from customServerAiStep in the
 * order of EntityCreature.updateEntityActionState:
 * <ol>
 * <li>{@link #tickFleeing()}; the mob's own target handling and attackEntity;</li>
 * <li>{@link #repath(Entity, boolean, boolean, Runnable)}: path to entityToAttack (no path or 1/20) or wander;</li>
 * <li>{@link #followPath(Entity, boolean, double)}: path following with at most 30 degrees of yaw per tick,
 * moveForward = movementSpeed attribute, strafing while hasAttacked, faceEntity(target, 30, 30) and jumping; with
 * a 1/100 chance per tick (and without a path) the EntityLiving idle look runs instead and the path is dropped.</li>
 * </ol>
 * The mob uses {@link LegacyPathNavigation} with manual following, {@link #noMoveControl} and
 * {@link #headFollowsYaw} (rotationYawHead = rotationYaw after the old AI).
 */
public final class LegacyCreatureAi {
    private final PathfinderMob mob;
    private final LegacyPathNavigation navigation;
    /** EntityCreature.fleeingTick */
    public int fleeingTick;
    /** EntityLiving.currentTarget / numTicksToChaseTarget / randomYawVelocity (idle look). */
    @Nullable
    private Entity lookTarget;
    private int ticksToChase;
    private float randomYawVelocity;

    public LegacyCreatureAi(PathfinderMob mob, LegacyPathNavigation navigation) {
        this.mob = mob;
        this.navigation = navigation;
    }

    public static MoveControl noMoveControl(PathfinderMob mob) {
        return new MoveControl(mob) {
            @Override
            public void tick() {
                // the old AI sets moveForward / moveStrafing itself
            }
        };
    }

    public static LookControl headFollowsYaw(PathfinderMob mob) {
        return new LookControl(mob) {
            @Override
            public void tick() {
                mob.setYHeadRot(mob.getYRot());
            }
        };
    }

    public LegacyPathNavigation navigation() {
        return this.navigation;
    }

    public boolean hasPath() {
        return !this.navigation.isDone();
    }

    /** EntityCreature: fleeingTick counts down at the start of updateEntityActionState. */
    public void tickFleeing() {
        if (this.fleeingTick > 0) {
            this.fleeingTick--;
        }
    }

    /**
     * EntityCreature.updateEntityActionState, second block: with an entityToAttack a path to it (16 blocks) is
     * computed when there is none or with a 1/20 chance; otherwise, without a path 1/180, or 1/120, or while fleeing,
     * the wander path is chosen (only while entityAge &lt; 100).
     */
    public void repath(@Nullable Entity entityToAttack, boolean hasAttacked, boolean avoidWater, Runnable updateWanderPath) {
        if (!hasAttacked && entityToAttack != null && (!hasPath() || this.mob.getRandom().nextInt(20) == 0)) {
            this.navigation.setLegacyPath(this.navigation.pathToEntity(entityToAttack, 16.0F, avoidWater));
        } else if (!hasAttacked && (!hasPath() && this.mob.getRandom().nextInt(180) == 0
                || this.mob.getRandom().nextInt(120) == 0 || this.fleeingTick > 0)
                && this.mob.getNoActionTime() < 100) {
            updateWanderPath.run();
        }
    }

    /** EntityCreature.updateWanderPath with getBlockPathWeight. */
    public void updateWanderPath(ToDoubleFunction<BlockPos> blockPathWeight) {
        boolean found = false;
        int bestX = -1;
        int bestY = -1;
        int bestZ = -1;
        float best = -99999.0F;
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = Mth.floor(this.mob.getX() + this.mob.getRandom().nextInt(13) - 6.0D);
            int y = Mth.floor(this.mob.getY() + this.mob.getRandom().nextInt(7) - 3.0D);
            int z = Mth.floor(this.mob.getZ() + this.mob.getRandom().nextInt(13) - 6.0D);
            float weight = (float) blockPathWeight.applyAsDouble(new BlockPos(x, y, z));
            if (weight > best) {
                best = weight;
                bestX = x;
                bestY = y;
                bestZ = z;
                found = true;
            }
        }
        if (found) {
            this.navigation.setLegacyPath(this.navigation.pathToXYZ(bestX, bestY, bestZ, 10.0F, false));
        }
    }

    /**
     * EntityCreature.updateEntityActionState, path-following block. {@code moveForward} is the legacy movementSpeed
     * attribute value; returns true when a path was followed this tick.
     */
    public boolean followPath(@Nullable Entity entityToAttack, boolean hasAttacked, double moveForward) {
        int feet = Mth.floor(this.mob.getBoundingBox().minY + 0.5D);
        boolean inWater = this.mob.isInWater();
        boolean inLava = this.mob.isInLava();
        this.mob.setXRot(0.0F);
        Path path = this.navigation.getPath();
        if (path != null && this.mob.getRandom().nextInt(100) != 0) {
            Vec3 next = path.isDone() ? null : path.getNextEntityPos(this.mob);
            double reach = this.mob.getBbWidth() * 2.0F;
            while (next != null && sqr(next.x - this.mob.getX()) + sqr(next.z - this.mob.getZ()) < reach * reach) {
                path.advance();
                if (path.isDone()) {
                    next = null;
                    this.navigation.setLegacyPath(null);
                } else {
                    next = path.getNextEntityPos(this.mob);
                }
            }
            boolean jumping = false;
            if (next != null) {
                double dx = next.x - this.mob.getX();
                double dz = next.z - this.mob.getZ();
                double dy = next.y - feet;
                float wanted = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                float turn = Mth.wrapDegrees(wanted - this.mob.getYRot());
                float forward = (float) moveForward;
                float strafe = 0.0F;
                turn = Mth.clamp(turn, -30.0F, 30.0F);
                this.mob.setYRot(this.mob.getYRot() + turn);
                if (hasAttacked && entityToAttack != null) {
                    double tx = entityToAttack.getX() - this.mob.getX();
                    double tz = entityToAttack.getZ() - this.mob.getZ();
                    float previous = this.mob.getYRot();
                    this.mob.setYRot((float) (Math.atan2(tz, tx) * 180.0D / Math.PI) - 90.0F);
                    float angle = (previous - this.mob.getYRot() + 90.0F) * (float) Math.PI / 180.0F;
                    strafe = -Mth.sin(angle) * forward;
                    forward = Mth.cos(angle) * forward;
                }
                this.mob.setZza(forward);
                this.mob.setXxa(strafe);
                if (dy > 0.0D) {
                    jumping = true;
                }
            }
            if (entityToAttack != null) {
                faceEntity(entityToAttack, 30.0F, 30.0F);
            }
            if (this.mob.horizontalCollision && !hasPath()) {
                jumping = true;
            }
            if (this.mob.getRandom().nextFloat() < 0.8F && (inWater || inLava)) {
                jumping = true;
            }
            if (jumping) {
                this.mob.getJumpControl().jump();
            }
            return true;
        }
        idle(inWater, inLava);
        this.navigation.setLegacyPath(null);
        return false;
    }

    /** EntityLiving.updateEntityActionState: stop, look at a player within 8 blocks or turn randomly. */
    private void idle(boolean inWater, boolean inLava) {
        this.mob.setZza(0.0F);
        this.mob.setXxa(0.0F);
        if (this.mob.getRandom().nextFloat() < 0.02F) {
            Player player = this.mob.level().getNearestPlayer(this.mob, 8.0D);
            if (player != null) {
                this.lookTarget = player;
                this.ticksToChase = 10 + this.mob.getRandom().nextInt(20);
            } else {
                this.randomYawVelocity = (this.mob.getRandom().nextFloat() - 0.5F) * 20.0F;
            }
        }
        if (this.lookTarget != null) {
            faceEntity(this.lookTarget, 10.0F, 40.0F);
            if (this.ticksToChase-- <= 0 || this.lookTarget.isRemoved() || this.lookTarget.distanceToSqr(this.mob) > 64.0D) {
                this.lookTarget = null;
            }
        } else {
            if (this.mob.getRandom().nextFloat() < 0.05F) {
                this.randomYawVelocity = (this.mob.getRandom().nextFloat() - 0.5F) * 20.0F;
            }
            this.mob.setYRot(this.mob.getYRot() + this.randomYawVelocity);
            this.mob.setXRot(0.0F);
        }
        if ((inWater || inLava) && this.mob.getRandom().nextFloat() < 0.8F) {
            this.mob.getJumpControl().jump();
        }
    }

    /** EntityLiving.faceEntity */
    public void faceEntity(Entity target, float maxYaw, float maxPitch) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        double dy = target instanceof LivingEntity living
                ? living.getEyeY() - this.mob.getEyeY()
                : (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0D - this.mob.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
        this.mob.setXRot(updateRotation(this.mob.getXRot(), pitch, maxPitch));
        this.mob.setYRot(updateRotation(this.mob.getYRot(), yaw, maxYaw));
    }

    private static float updateRotation(float current, float wanted, float max) {
        return current + Mth.clamp(Mth.wrapDegrees(wanted - current), -max, max);
    }

    private static double sqr(double value) {
        return value * value;
    }
}
