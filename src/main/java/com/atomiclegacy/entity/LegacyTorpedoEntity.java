package com.atomiclegacy.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atomiclegacy.network.VntStandardExplosionPayload;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 EntityTorpedo (Protège's M44_EQUESTRIAN round): falls with 0.04 gravity (terminal -2.5) and
 * on touching any non-air block detonates ExplosionVNT(20F).makeStandard() plus
 * ExplosionCreator.composeEffectStandard one block above.
 */
public final class LegacyTorpedoEntity extends Entity {

	public LegacyTorpedoEntity(EntityType<? extends LegacyTorpedoEntity> type, Level level) {
		super(type, level);
	}

	public LegacyTorpedoEntity(Level level, Vec3 position) {
		this(HbmEntityTypes.LEGACY_TORPEDO.get(), level);
		setPos(position);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	/** EntityTorpedo#onUpdate replaces Entity#onUpdate entirely (no onEntityUpdate); the tick counter is advanced by the level. */
	@Override
	public void tick() {
		if(!level().isClientSide && this.tickCount == 1 && level() instanceof ServerLevel serverLevel) {
			for(int i = 0; i < 15; i++) {
				double px = getX() + (random.nextDouble() - 0.5) * 2;
				double py = getY() + (random.nextDouble() - 0.5) * 1;
				double pz = getZ() + (random.nextDouble() - 0.5) * 2;
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

		double motionY = motion.y - 0.04;
		if(motionY < -2.5) motionY = -2.5;
		setDeltaMovement(motion.x, motionY, motion.z);

		if(!level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
			if(!level().isClientSide && level() instanceof ServerLevel serverLevel) {
				discard();
				LegacyProjectileUtil.composeExplosionEffect(serverLevel, new Vec3(getX(), getY() + 1, getZ()),
						15, 5F, 1F, 45F, 10, 16, 50, 1F, 3F, -2F, 200);
				explodeStandard(serverLevel, getX(), getY(), getZ(), 20F);
			}
		}

		// out-of-world cleanup normally done by Entity#baseTick, kept so a torpedo above the void is not leaked
		checkBelowWorld();
	}

	/**
	 * new ExplosionVNT(world, x, y, z, size).makeStandard().explode() without an exploder:
	 * BlockAllocatorStandard(16), EntityProcessorStandard, BlockProcessorStandard (drop chance 1 / size),
	 * PlayerProcessorStandard and ExplosionEffectStandard, in the original processing order.
	 */
	public static void explodeStandard(ServerLevel level, double x, double y, double z, float size) {
		Explosion compat = new Explosion(level, null, x, y, z, size, false, Explosion.BlockInteraction.DESTROY);

		/// BlockAllocatorStandard ///
		Set<BlockPos> affectedBlocks = new HashSet<>();
		int resolution = 16;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for(int i = 0; i < resolution; ++i) {
			for(int j = 0; j < resolution; ++j) {
				for(int k = 0; k < resolution; ++k) {
					if(i == 0 || i == resolution - 1 || j == 0 || j == resolution - 1 || k == 0 || k == resolution - 1) {
						double d0 = (double) ((float) i / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d1 = (double) ((float) j / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d2 = (double) ((float) k / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= d3;
						d1 /= d3;
						d2 /= d3;

						float powerRemaining = size * (0.7F + level.random.nextFloat() * 0.6F);
						double currentX = x;
						double currentY = y;
						double currentZ = z;
						for(float stepSize = 0.3F; powerRemaining > 0.0F; powerRemaining -= stepSize * 0.75F) {
							cursor.set(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
							if(!level.isInWorldBounds(cursor)) break;
							BlockState state = level.getBlockState(cursor);
							if(!state.isAir()) {
								float blockResistance = state.getExplosionResistance(level, cursor, compat);
								powerRemaining -= (blockResistance + 0.3F) * stepSize;
							}
							if(powerRemaining > 0.0F) affectedBlocks.add(cursor.immutable());
							currentX += d0 * (double) stepSize;
							currentY += d1 * (double) stepSize;
							currentZ += d2 * (double) stepSize;
						}
					}
				}
			}
		}

		/// EntityProcessorStandard ///
		Map<ServerPlayer, Vec3> affectedPlayers = new HashMap<>();
		float range = size * 2.0F;
		AABB area = new AABB(x - range - 1.0D, y - range - 1.0D, z - range - 1.0D, x + range + 1.0D, y + range + 1.0D, z + range + 1.0D);
		List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
		EventHooks.onExplosionDetonate(level, compat, list, range);
		Vec3 center = new Vec3(x, y, z);
		for(Entity entity : list) {
			double distanceScaled = Math.sqrt(entity.distanceToSqr(x, y, z)) / range;
			if(distanceScaled <= 1.0D) {
				double deltaX = entity.getX() - x;
				double deltaY = entity.getEyeY() - y;
				double deltaZ = entity.getZ() - z;
				double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
				if(distance != 0.0D) {
					deltaX /= distance;
					deltaY /= distance;
					deltaZ /= distance;

					double density = Explosion.getSeenPercent(center, entity);
					double knockback = (1.0D - distanceScaled) * density;

					entity.hurt(level.damageSources().explosion(null, null), (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * range + 1.0D)));
					// EnchantmentProtection.func_92092_a (Blast Protection): EXPLOSION_KNOCKBACK_RESISTANCE in 1.21
					double enchKnockback = entity instanceof LivingEntity living ? knockback * (1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) : knockback;

					if(entity instanceof ServerPlayer player) {
						// the server-side motion of a player was never synced; PlayerProcessorStandard sends this vector
						affectedPlayers.put(player, new Vec3(deltaX * knockback, deltaY * knockback, deltaZ * knockback));
					} else {
						entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * enchKnockback, deltaY * enchKnockback, deltaZ * enchKnockback));
						entity.hurtMarked = true;
					}
				}
			}
		}

		/// BlockProcessorStandard ///
		// compat.affectedBlockPositions was filled at allocation time, so the effect packet keeps the air entries
		List<BlockPos> compatPositions = new java.util.ArrayList<>(affectedBlocks);
		float dropChance = 1.0F / size;
		for(BlockPos pos : affectedBlocks) {
			BlockState state = level.getBlockState(pos);
			if(state.isAir()) continue;
			boolean canDrop = state.canDropFromExplosion(level, pos, compat);
			// onExplosionHit performs Block#onBlockExploded; drops are filtered per stack like dropBlockAsItemWithChance
			state.onExplosionHit(level, pos, compat, (stack, dropPos) -> {
				if(canDrop && level.random.nextFloat() <= dropChance) Block.popResource(level, dropPos, stack);
			});
		}

		/// PlayerProcessorStandard: ExplosionKnockbackPacket adds the motion on the client ///
		for(Map.Entry<ServerPlayer, Vec3> entry : affectedPlayers.entrySet()) {
			ServerPlayer player = entry.getKey();
			player.setDeltaMovement(player.getDeltaMovement().add(entry.getValue()));
			player.hurtMarked = true;
		}

		/// ExplosionEffectStandard ///
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, 250, new VntStandardExplosionPayload(x, y, z, size, compatPositions));
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

	@Override
	public boolean fireImmune() {
		return true;
	}
}
