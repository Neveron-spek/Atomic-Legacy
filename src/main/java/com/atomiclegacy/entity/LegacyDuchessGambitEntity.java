package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch D1] 1.7.10 com.hbm.entity.projectile.EntityDuchessGambit ("entity_duchessgambit", tracking
 * range 1000), spawned by XFactory12ga.LAMBDA_BOAT (g12_equestrian_bj of gun_autoshotgun_sexy): a boat falling
 * from 50 blocks above the hit that kills everything in a 10 x 4 x 18 box, explodes five times along its length,
 * leaves a boat block and a ring of shock smoke.
 */
public final class LegacyDuchessGambitEntity extends Entity {

	public LegacyDuchessGambitEntity(EntityType<? extends LegacyDuchessGambitEntity> type, Level level) {
		super(type, level);
	}

	public LegacyDuchessGambitEntity(Level level, Vec3 position) {
		this(HbmEntityTypes.LEGACY_DUCHESS_GAMBIT.get(), level);
		setPos(position);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	/** EntityDuchessGambit#onUpdate replaces Entity#onUpdate entirely; the tick counter is advanced by the level. */
	@Override
	public void tick() {

		if(!level().isClientSide && this.tickCount == 1 && level() instanceof ServerLevel serverLevel) {
			for(int i = 0; i < 50; i++) {
				// AuxParticlePacketNT type "bf" (ParticleMukeCloudBF, no motion) within 150 blocks
				double px = getX() + (random.nextDouble() - 0.5) * 5;
				double py = getY() + (random.nextDouble() - 0.5) * 7;
				double pz = getZ() + (random.nextDouble() - 0.5) * 20;
				sendAround(serverLevel, HbmParticleTypes.MUKE_CLOUD_BALEFIRE.get(), px, py, pz, 0D, 0D, 0D, 150D);
			}
		}

		xo = getX();
		yo = getY();
		zo = getZ();
		Vec3 motion = getDeltaMovement();
		setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

		double motionY = motion.y - 0.03;
		if(motionY < -1.5) motionY = -1.5;
		setDeltaMovement(motion.x, motionY, motion.z);

		if(!level().isClientSide && level() instanceof ServerLevel serverLevel
				&& !level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
			impact(serverLevel);
		}
	}

	private void impact(ServerLevel level) {
		double posX = getX();
		double posY = getY();
		double posZ = getZ();
		level.playSound(null, posX, posY, posZ, HbmSoundEvents.ALARM_GAMBIT.get(), SoundSource.RECORDS, 10000.0F, 1F);
		discard();

		for(Entity e : level.getEntities((Entity) null, new AABB(posX - 5, posY - 2, posZ - 9, posX + 5, posY + 2, posZ + 9))) {
			if(e == this) continue;
			e.hurt(level.damageSources().source(HbmDamageTypes.BOAT), 1000);
		}

		explodeLarge(level, posX, posY, posZ - 6, 2);
		explodeLarge(level, posX, posY, posZ - 3, 2);
		explodeLarge(level, posX, posY, posZ, 2);
		explodeLarge(level, posX, posY, posZ + 3, 2);
		explodeLarge(level, posX, posY, posZ + 6, 2);

		level.setBlock(new BlockPos((int) (posX - 0.5), (int) (posY + 0.5), (int) (posZ - 0.5)), HbmBlocks.BOAT.get().defaultBlockState(), 3);

		spawnShock(level, posX, posY + 1, posZ, 24, 3);
		spawnShock(level, posX, posY + 1, posZ, 24, 2.5);
		spawnShock(level, posX, posY + 1, posZ, 24, 2);
		spawnShock(level, posX, posY + 1, posZ, 24, 1.5);
		spawnShock(level, posX, posY + 1, posZ, 24, 1);
	}

	/**
	 * ExplosionLarge.explode(world, x, y, z, strength, cloud = true, rubble = false, shrapnel = false):
	 * World#createExplosion(null, x, y, z, strength, true) plus the "smoke"/"cloud" aux particles
	 * (cloudFunction(strength) ParticleExSmoke, client-randomised motion, within 250 blocks).
	 */
	public static void explodeLarge(ServerLevel level, double x, double y, double z, float strength) {
		level.explode(null, x, y, z, strength, Level.ExplosionInteraction.BLOCK);
		int count = Math.max(1, cloudFunction((int) strength));
		RandomSource rand = level.random;
		for(int i = 0; i < count; i++) {
			double mY = rand.nextGaussian() * (1 + (count / 100));
			double mX = rand.nextGaussian() * (1 + (count / 150));
			double mZ = rand.nextGaussian() * (1 + (count / 150));
			if(rand.nextBoolean()) mY = Math.abs(mY);
			sendAround(level, HbmParticleTypes.LAUNCHER_EX_SMOKE.get(), x, y, z, mX, mY, mZ, 250D);
		}
	}

	/** ExplosionLarge.cloudFunction (the exponent uses integer division). */
	public static int cloudFunction(int i) {
		return (int) (850 * (1 - Math.pow(Math.E, -i / 15)) + 15);
	}

	/**
	 * ExplosionLarge.spawnShock: "smoke"/"shock" aux particles at y + 0.5 within 250 blocks, a ring of count
	 * ParticleExSmoke with horizontal motion strength (Vec3#rotateAroundY uses radians, the start angle is
	 * rand.nextInt(360) radians as in the original).
	 */
	public static void spawnShock(ServerLevel level, double x, double y, double z, int count, double strength) {
		y += 0.5;
		double vx = strength;
		double vz = 0;
		double[] rot = rotateAroundY(vx, vz, level.random.nextInt(360));
		vx = rot[0];
		vz = rot[1];
		for(int i = 0; i < count; i++) {
			sendAround(level, HbmParticleTypes.LAUNCHER_EX_SMOKE.get(), x, y, z, vx, 0D, vz, 250D);
			rot = rotateAroundY(vx, vz, (float) Math.PI * 2F / (float) count);
			vx = rot[0];
			vz = rot[1];
		}
	}

	/** Vec3#rotateAroundY(float) of 1.7.10. */
	private static double[] rotateAroundY(double x, double z, float angle) {
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		return new double[] {x * (double) cos + z * (double) sin, z * (double) cos - x * (double) sin};
	}

	/** One particle with an exact motion (count 0) to every player within range. */
	private static void sendAround(ServerLevel level, ParticleOptions type, double x, double y, double z, double mx, double my, double mz, double range) {
		for(ServerPlayer player : level.players()) {
			if(player.distanceToSqr(x, y, z) <= range * range) {
				level.sendParticles(player, type, true, x, y, z, 0, mx, my, mz, 1D);
			}
		}
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 25000;
	}
}
