package com.atomiclegacy.entity;

import java.util.List;
import java.util.Optional;

import com.atomiclegacy.items.GunB92Item;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * [parity c1] 1.7.10 com.hbm.entity.projectile.EntityB92Beam ("entity_beam_bomb"): the B92 beam, an arrow derivative
 * without gravity that lives 100 ticks, detonates a range 10 Fleija (statFacFleija + rainbow cloud) on the block it
 * hit (one tick later), on every tick it touches an entity (it is not removed by that) and in water. legacyYaw /
 * legacyPitch are the 1.7.10 rotationYaw / rotationPitch of the arrow convention (atan2(x, z), atan2(y, horizontal)).
 */
public class B92BeamEntity extends Entity {

	private int xTile = -1;
	private int yTile = -1;
	private int zTile = -1;
	/** 1.7.10 used (-1, -1, -1), always air there; y = -1 is a real block now, so the "no block yet" state is explicit. */
	private boolean hasTile;
	public Entity shootingEntity;
	private int ticksInAir;
	private double damage = 2.0D;

	public B92BeamEntity(EntityType<? extends B92BeamEntity> type, Level level) {
		super(type, level);
	}

	/** EntityB92Beam(World, EntityLivingBase, float) */
	public B92BeamEntity(Level level, LivingEntity shooter, float velocity) {
		this(HbmEntityTypes.B92_BEAM.get(), level);
		this.shootingEntity = shooter;

		float yaw = shooter.getYRot();
		float pitch = shooter.getXRot();
		double x = shooter.getX();
		double y = shooter.getEyeY();
		double z = shooter.getZ();
		x -= Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
		y -= 0.10000000149011612D;
		z -= Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
		this.setPos(x, y, z);
		double mx = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
		double mz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
		double my = -Mth.sin(pitch / 180.0F * (float) Math.PI);
		this.setThrowableHeading(mx, my, mz, velocity * 1.5F, 1.0F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
		float f2 = Mth.sqrt((float) (x * x + y * y + z * z));
		x /= f2;
		y /= f2;
		z /= f2;
		x += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
		y += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
		z += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
		x *= velocity;
		y *= velocity;
		z *= velocity;
		this.setDeltaMovement(x, y, z);
		float f3 = Mth.sqrt((float) (x * x + z * z));
		this.setYRot((float) (Math.atan2(x, z) * 180.0D / Math.PI));
		this.setXRot((float) (Math.atan2(y, f3) * 180.0D / Math.PI));
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	@Override
	public void tick() {
		super.tick();

		Vec3 motion = this.getDeltaMovement();

		// the client only moves the beam; hits, lifetime and removal are decided by the server
		if(this.level().isClientSide) {
			this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
			return;
		}

		if(this.tickCount > 100)
			this.discard();

		if(this.xRotO == 0.0F && this.yRotO == 0.0F) {
			this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
			this.yRotO = this.getYRot();
		}

		if(this.hasTile && !this.level().getBlockState(new BlockPos(this.xTile, this.yTile, this.zTile)).isAir()) {
			this.discard();
			explode();
		}

		++this.ticksInAir;
		Vec3 vec31 = this.position();
		Vec3 vec3 = vec31.add(motion);
		// World#func_147447_a(start, end, false, true, false): blocks without a collision box are ignored
		HitResult mop = this.level().clip(new ClipContext(vec31, vec3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if(mop.getType() == HitResult.Type.MISS) mop = null;

		if(mop != null) {
			vec3 = mop.getLocation();
		}

		Entity entity = null;
		List<Entity> list = this.level().getEntities(this, this.getBoundingBox().expandTowards(motion).inflate(1.0D, 1.0D, 1.0D));
		double d0 = 0.0D;

		for(Entity entity1 : list) {

			if(entity1.isPickable() && (entity1 != this.shootingEntity || this.ticksInAir >= 5)) {
				float f1 = 0.3F;
				AABB axisalignedbb1 = entity1.getBoundingBox().inflate(f1, f1, f1);
				Optional<Vec3> intercept = axisalignedbb1.clip(vec31, vec3);

				if(intercept.isPresent()) {
					double d1 = vec31.distanceTo(intercept.get());

					if(d1 < d0 || d0 == 0.0D) {
						entity = entity1;
						d0 = d1;
					}
				}
			}
		}

		boolean hitEntity = entity != null;

		if(hitEntity && entity instanceof Player entityplayer) {

			if(entityplayer.getAbilities().invulnerable || this.shootingEntity instanceof Player shooter && !shooter.canHarmPlayer(entityplayer)) {
				hitEntity = false;
				mop = null;
			}
		}

		if(hitEntity) {
			explode();
		} else if(mop instanceof BlockHitResult blockHit) {
			this.xTile = blockHit.getBlockPos().getX();
			this.yTile = blockHit.getBlockPos().getY();
			this.zTile = blockHit.getBlockPos().getZ();
			this.hasTile = true;
		}

		double x = this.getX() + motion.x;
		double y = this.getY() + motion.y;
		double z = this.getZ() + motion.z;
		this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

		if(this.isInWater()) {
			this.discard();
			explode();
		}

		this.setPos(x, y, z);
		this.checkInsideBlocks();
	}

	private void explode() {
		if(this.level() instanceof ServerLevel serverLevel) {
			GunB92Item.fleija(serverLevel, this.getX(), this.getY(), this.getZ(), 10);
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("xTile", this.xTile);
		tag.putInt("yTile", this.yTile);
		tag.putInt("zTile", this.zTile);
		tag.putBoolean("hasTile", this.hasTile);
		tag.putDouble("damage", this.damage);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.xTile = tag.getInt("xTile");
		this.yTile = tag.getInt("yTile");
		this.zTile = tag.getInt("zTile");
		this.hasTile = tag.getBoolean("hasTile");

		if(tag.contains("damage", 99)) {
			this.damage = tag.getDouble("damage");
		}
	}

	@Override
	protected MovementEmission getMovementEmission() {
		return MovementEmission.NONE;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	/** renderDistanceWeight 10: average edge 0.5 * 64 * 10 */
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double d1 = 0.5D * 64.0D * 10.0D;
		return distance < d1 * d1;
	}
}
