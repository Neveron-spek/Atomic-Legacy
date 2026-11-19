package com.atomiclegacy.entity;

import java.util.List;
import java.util.UUID;

import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F2] 1.7.10 com.hbm.entity.projectile.EntityCoin (EntityThrowableInterp): a 1x1 thrown coin of the
 * N I 4 N I with gravity 0.02, no air drag (water drag 0.8), that dies on block contact and can be collided with so
 * {@link LegacyBulletBeamEntity} ricochets off it. The 1.7.10 position was the box center (yOffset 0.5); here the
 * position is the box bottom and the renderer draws 0.5 higher. The client only interpolates towards the server
 * positions like the other throwables.
 */
public class LegacyCoinEntity extends Entity {

	private LivingEntity thrower;
	private UUID throwerUuid;
	private int ticksInAir;

	private int turnProgress;
	private double syncPosX;
	private double syncPosY;
	private double syncPosZ;
	private double syncYaw;
	private double syncPitch;

	public LegacyCoinEntity(EntityType<? extends LegacyCoinEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public LegacyCoinEntity(Level level) {
		this(HbmEntityTypes.LEGACY_COIN.get(), level);
	}

	public void setThrower(LivingEntity thrower) {
		this.thrower = thrower;
		this.throwerUuid = thrower == null ? null : thrower.getUUID();
	}

	/** EntityThrowableNT#getThrower: resolves the saved UUID once the owner is loaded again. */
	public LivingEntity getThrower() {
		if(this.thrower == null && this.throwerUuid != null && level() instanceof ServerLevel server && server.getEntity(this.throwerUuid) instanceof LivingEntity living) {
			this.thrower = living;
		}
		return this.thrower;
	}

	/** The 1.7.10 posY of the coin (box center). */
	public double centerY() {
		return getY() + 0.5D;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }

	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
		this.syncPosX = x;
		this.syncPosY = y;
		this.syncPosZ = z;
		this.syncYaw = yRot;
		this.syncPitch = xRot;
		this.turnProgress = steps;
	}

	@Override
	public void tick() {
		super.tick();
		this.xo = getX();
		this.yo = getY();
		this.zo = getZ();
		this.yRotO = getYRot();
		this.xRotO = getXRot();

		if(level().isClientSide) {
			// EntityThrowableInterp: no simulation, only interpolation
			if(this.turnProgress > 0) {
				double interpX = getX() + (this.syncPosX - getX()) / (double) this.turnProgress;
				double interpY = getY() + (this.syncPosY - getY()) / (double) this.turnProgress;
				double interpZ = getZ() + (this.syncPosZ - getZ()) / (double) this.turnProgress;
				double d = Mth.wrapDegrees(this.syncYaw - (double) getYRot());
				setYRot((float) ((double) getYRot() + d / (double) this.turnProgress));
				setXRot((float) ((double) getXRot() + (this.syncPitch - (double) getXRot()) / (double) this.turnProgress));
				--this.turnProgress;
				setPos(interpX, interpY, interpZ);
			}
			return;
		}

		++this.ticksInAir;
		Vec3 motion = getDeltaMovement();
		Vec3 pos = position();
		Vec3 nextPos = pos.add(motion);

		// World.func_147447_a(pos, next, false, true, false): no liquids, blocks without collision box ignored
		HitResult mop = null;
		BlockHitResult blockHit = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if(blockHit.getType() != HitResult.Type.MISS) {
			mop = blockHit;
			nextPos = blockHit.getLocation();
		}

		// EntityThrowableNT entity trace (non penetrating): the nearest entity hit replaces the block hit
		Entity hitEntity = null;
		double nearest = 0.0D;
		Vec3 hitVec = null;
		LivingEntity owner = getThrower();
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(motion).inflate(1.0D, 1.0D, 1.0D));
		for(Entity entity : list) {
			if(entity.isPickable() && (entity != owner || this.ticksInAir >= 5) && entity.isAlive()) {
				AABB aabb = entity.getBoundingBox().inflate(0.3D, 0.3D, 0.3D);
				Vec3 intercept = aabb.clip(pos, nextPos).orElse(null);
				if(intercept != null) {
					double dist = pos.distanceTo(intercept);
					if(dist < nearest || nearest == 0.0D) {
						hitEntity = entity;
						nearest = dist;
						hitVec = intercept;
					}
				}
			}
		}
		if(hitEntity != null) mop = new net.minecraft.world.phys.EntityHitResult(hitEntity, hitVec);

		if(mop != null) {
			// EntityCoin#onImpact: only block hits kill the coin
			if(mop.getType() == HitResult.Type.BLOCK) {
				discard();
				return;
			}
		}

		// rotation follows the motion (EntityThrowableNT; 1.7.10 yaw = atan2(motionX, motionZ))
		float hyp = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
		float pitch = (float) (Math.atan2(motion.y, (double) hyp) * 180.0D / Math.PI);
		while(pitch - this.xRotO < -180.0F) this.xRotO -= 360.0F;
		while(pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
		while(yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
		while(yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;
		setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
		setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);

		// getAirDrag() = 1F, getWaterDrag() = 0.8F, getGravityVelocity() = 0.02D
		float drag = isInWater() ? 0.8F : 1F;
		setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
		setDeltaMovement(motion.x * drag, motion.y * drag - 0.02D, motion.z * drag);
	}

	@Override public boolean isPickable() { return true; }
	@Override public boolean isAttackable() { return true; }
	@Override public boolean isPushable() { return false; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.ticksInAir = tag.getInt("ticksInAir");
		this.throwerUuid = tag.hasUUID("thrower") ? tag.getUUID("thrower") : null;
		this.thrower = null;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("ticksInAir", this.ticksInAir);
		if(this.throwerUuid != null) tag.putUUID("thrower", this.throwerUuid);
	}
}
