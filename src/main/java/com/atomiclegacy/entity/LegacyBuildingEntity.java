package com.atomiclegacy.entity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 EntityBuilding (EntityMappings "entity_falling_building", tracking range 1000), spawned by
 * XFactory50.LAMBDA_BUILDING (gun_amat_subtlety's bmg50_equestrian round). On its first tick it sends 100
 * "bf" particles within 15 blocks, then falls with 0.03 gravity (terminal -1.5). When the block at its
 * truncated position is not air it plays entity.oldExplosion, sends ExplosionLarge.spawnParticles(150) and
 * five spawnShock rings, deals ModDamageSource.building (absolute, bypasses armor) 1000 damage to every
 * entity within 8 blocks and throws 250 brick EntityRubble.
 */
public final class LegacyBuildingEntity extends Entity {

	/** ModDamageSource.building: setDamageIsAbsolute().setDamageBypassesArmor(). */
	public static final ResourceKey<DamageType> BUILDING = ResourceKey.create(Registries.DAMAGE_TYPE, AtomicLegacy.id("building"));

	public LegacyBuildingEntity(EntityType<? extends LegacyBuildingEntity> type, Level level) {
		super(type, level);
	}

	public LegacyBuildingEntity(Level level, Vec3 position) {
		this(HbmEntityTypes.LEGACY_BUILDING.get(), level);
		setPos(position);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	/** EntityBuilding#onUpdate replaces Entity#onUpdate entirely; the tick counter is advanced by the level. */
	@Override
	public void tick() {
		if(!level().isClientSide && this.tickCount == 1 && level() instanceof ServerLevel serverLevel) {
			for(int i = 0; i < 100; i++) {
				double px = getX() + (random.nextDouble() - 0.5) * 15;
				double py = getY() + (random.nextDouble() - 0.5) * 15;
				double pz = getZ() + (random.nextDouble() - 0.5) * 15;
				// AuxParticlePacketNT type "bf" (ParticleMukeCloudBF, no motion) within 150 blocks
				for(ServerPlayer player : serverLevel.players()) {
					if(player.distanceToSqr(getX(), getY(), getZ()) <= 150D * 150D) {
						serverLevel.sendParticles(player, HbmParticleTypes.MUKE_CLOUD_BALEFIRE.get(), true, px, py, pz, 1, 0D, 0D, 0D, 0D);
					}
				}
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

		if(!level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
			if(!level().isClientSide && level() instanceof ServerLevel serverLevel) {
				impact(serverLevel);
			}
		}

		checkBelowWorld();
	}

	private void impact(ServerLevel level) {
		// sounds.json category "ambient"
		level.playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(), SoundSource.AMBIENT, 10000.0F, 0.5F + this.random.nextFloat() * 0.1F);
		discard();
		spawnParticles(level, getX(), getY() + 3, getZ(), 150);
		spawnShock(level, getX(), getY() + 1, getZ(), 24, 6);
		spawnShock(level, getX(), getY() + 1, getZ(), 24, 5);
		spawnShock(level, getX(), getY() + 1, getZ(), 24, 4);
		spawnShock(level, getX(), getY() + 1, getZ(), 24, 3);
		spawnShock(level, getX(), getY() + 1, getZ(), 24, 3);

		for(Entity e : level.getEntities(this, new AABB(getX() - 8, getY() - 8, getZ() - 8, getX() + 8, getY() + 8, getZ() + 8))) {
			e.hurt(damageSources().source(BUILDING), 1000);
		}

		for(int i = 0; i < 250; i++) {
			// Vec3(1, 0, 0).rotateAroundZ(-rand * PI / 2).rotateAroundY(rand * 2 PI)
			Vec3 vec = new Vec3(1, 0, 0).zRot((float) (-random.nextFloat() * Math.PI / 2)).yRot((float) (random.nextFloat() * Math.PI * 2));
			level.addFreshEntity(new MineRubbleEntity(level, getX(), getY() + 3, getZ(), vec, Blocks.BRICKS.defaultBlockState()));
		}
	}

	/** ExplosionLarge.spawnParticles: AuxParticlePacketNT "smoke"/"cloud" (ParticleExSmoke) within 250 blocks. */
	public static void spawnParticles(ServerLevel level, double x, double y, double z, int count) {
		for(ServerPlayer player : level.players()) {
			if(player.distanceToSqr(x, y, z) > 250D * 250D) continue;
			for(int i = 0; i < Math.max(1, count); i++) {
				double my = level.random.nextGaussian() * (1 + (count / 100));
				double mx = level.random.nextGaussian() * (1 + (count / 150));
				double mz = level.random.nextGaussian() * (1 + (count / 150));
				if(level.random.nextBoolean()) my = Math.abs(my);
				level.sendParticles(player, HbmParticleTypes.LANDMINE_SMOKE.get(), true, x, y, z, 0, mx, my, mz, 1.0D);
			}
		}
	}

	/** ExplosionLarge.spawnShock: AuxParticlePacketNT "smoke"/"shock" at y + 0.5, a horizontal ring of ParticleExSmoke. */
	public static void spawnShock(ServerLevel level, double x, double y, double z, int count, double strength) {
		for(ServerPlayer player : level.players()) {
			if(player.distanceToSqr(x, y, z) > 250D * 250D) continue;
			// Vec3.rotateAroundY(rand.nextInt(360)) takes radians in 1.7.10
			Vec3 vec = new Vec3(strength, 0, 0).yRot(level.random.nextInt(360));
			for(int i = 0; i < Math.max(1, count); i++) {
				level.sendParticles(player, HbmParticleTypes.LANDMINE_SMOKE.get(), true, x, y + 0.5, z, 0, vec.x, 0, vec.z, 1.0D);
				vec = vec.yRot((float) Math.PI * 2F / (float) count);
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 25_000.0D;
	}
}
