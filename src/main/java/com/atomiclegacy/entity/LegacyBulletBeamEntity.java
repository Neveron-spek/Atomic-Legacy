package com.atomiclegacy.entity;

import java.util.List;

import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * 1.7.10 com.hbm.entity.projectile.EntityBulletBeamBase ("entity_beam_mk4"): an instant hitscan
 * of 250 blocks performed when the beam is created; the entity only lives for the config's
 * {@code expires} ticks so the client can draw the beam.
 *
 * <p>The EntityCoin ricochet branch is not present because the coin (gun_n_i_4_n_i) is not ported;
 * without coins in the world that branch never runs in the original either. The nether portal
 * branch is not present either: the original block trace ignores blocks without a collision box,
 * so a portal block can never be the hit block.
 */
public class LegacyBulletBeamEntity extends Entity implements IEntityWithComplexSpawn {

	public LivingEntity thrower;
	public BeamBulletConfig config;
	public float damage;
	public double headingX;
	public double headingY;
	public double headingZ;
	public double beamLength;

	public LegacyBulletBeamEntity(EntityType<? extends LegacyBulletBeamEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public LivingEntity getThrower() { return this.thrower; }

	/** EntityBulletBeamBase(entity, config, baseDamage, angularInaccuracy, sideOffset, heightOffset, frontOffset) */
	public LegacyBulletBeamEntity(LivingEntity entity, BeamBulletConfig config, float baseDamage, float angularInaccuracy, double sideOffset, double heightOffset, double frontOffset) {
		this(HbmEntityTypes.LEGACY_BULLET_BEAM.get(), entity.level());

		this.thrower = entity;
		this.config = config;
		this.damage = baseDamage * config.damageMult();

		float yaw = entity.getYRot() + (float) this.random.nextGaussian() * angularInaccuracy;
		float pitch = entity.getXRot() + (float) this.random.nextGaussian() * angularInaccuracy;
		this.setYRot(yaw);
		this.setXRot(pitch);
		this.yRotO = yaw;
		this.xRotO = pitch;

		Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset)
				.xRot(-pitch / 180F * (float) Math.PI)
				.yRot(-yaw / 180F * (float) Math.PI);
		this.setPos(entity.getX() + offset.x, entity.getEyeY() + offset.y, entity.getZ() + offset.z);

		this.headingX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
		this.headingZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
		this.headingY = -Mth.sin(pitch / 180.0F * (float) Math.PI);

		double range = 250D;
		this.headingX *= range;
		this.headingY *= range;
		this.headingZ *= range;

		performHitscan();
	}

	/** [weapons batch E2] EntityBulletBeamBase(entity, config, baseDamage): no position, rotation or hitscan yet (XFactoryEnergy.LAMBDA_LIGHTNING_SPLIT). */
	public LegacyBulletBeamEntity(LivingEntity entity, BeamBulletConfig config, float baseDamage) {
		this(HbmEntityTypes.LEGACY_BULLET_BEAM.get(), entity.level());
		this.thrower = entity;
		this.config = config;
		this.damage = baseDamage * config.damageMult();
	}

	/** [parity H2: grenade_universal] EntityBulletBeamBase(world, config, baseDamage) with "sub.thrower = ..." (ItemGrenadeFilling.EXPLODE_LASER; the thrower may be null). */
	public LegacyBulletBeamEntity(Level level, LivingEntity thrower, BeamBulletConfig config, float baseDamage) {
		this(HbmEntityTypes.LEGACY_BULLET_BEAM.get(), level);
		this.thrower = thrower;
		this.config = config;
		this.damage = baseDamage * config.damageMult();
	}

	/** [weapons batch E2] EntityBulletBeamBase#setRotationsFromVector */
	public void setRotationsFromVector(Vec3 delta) {
		float pitch = (float) (-Math.asin(delta.y / delta.length()) * 180D / Math.PI);
		float yaw = (float) (-Math.atan2(delta.x, delta.z) * 180D / Math.PI);
		this.setXRot(pitch);
		this.setYRot(yaw);

		this.headingX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
		this.headingZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
		this.headingY = -Mth.sin(pitch / 180.0F * (float) Math.PI);
	}

	/** [weapons batch E2] EntityBulletBeamBase#performHitscanExternal */
	public void performHitscanExternal(double range) {
		this.headingX *= range;
		this.headingY *= range;
		this.headingZ *= range;
		performHitscan();
	}

	public BulletConfig getBulletConfig() { return this.config; }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) { }

	@Override
	public void tick() {
		if(config == null) {
			this.discard();
			return;
		}

		if(config.onUpdateBeam != null) config.onUpdateBeam.accept(this); // [weapons batch F4] BulletConfig.onUpdate

		super.tick();

		if(!level().isClientSide && this.tickCount > config.expires) this.discard();
	}

	protected void performHitscan() {

		Vec3 pos = new Vec3(this.getX(), this.getY(), this.getZ());
		Vec3 nextPos = new Vec3(this.getX() + this.headingX, this.getY() + this.headingY, this.getZ() + this.headingZ);
		HitResult mop = null;
		if(!config.spectral) {
			// World.func_147447_a(pos, next, false, true, false): no liquids, blocks without collision box are ignored
			BlockHitResult block = this.level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			if(block.getType() != HitResult.Type.MISS) mop = block;
		}

		if(mop != null) {
			nextPos = mop.getLocation();
		}

		if(!this.level().isClientSide && config.impactsEntities) {

			Entity hitEntity = null;
			List<Entity> list = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.headingX, this.headingY, this.headingZ).inflate(1.0D, 1.0D, 1.0D));
			double nearest = 0.0D;
			Vec3 nonPenImpact = null;
			// [weapons batch F2: coin ricochet] EntityCoin branch of EntityBulletBeamBase.performHitscan
			Vec3 coinHit = null;
			double closestCoin = 0;
			com.atomiclegacy.entity.LegacyCoinEntity hitCoin = null;
			for(Entity entity : list) {
				if(entity.isRemoved()) continue;
				if(entity instanceof com.atomiclegacy.entity.LegacyCoinEntity coin) {
					double hitbox = 0.3F;
					AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
					Vec3 hitMop = aabb.clip(pos, nextPos).orElse(null);
					if(hitMop != null) {
						double dist = pos.distanceTo(hitMop);
						if(closestCoin == 0 || dist < closestCoin) {
							closestCoin = dist;
							hitCoin = coin;
							coinHit = hitMop;
						}
					}
				}
			}

			for(Entity entity : list) {

				if(entity.isPickable() && entity != thrower && entity.isAlive()) {
					double hitbox = 0.3F;
					AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
					Vec3 hitVec = aabb.contains(pos) ? pos : aabb.clip(pos, nextPos).orElse(null);

					if(hitVec != null) {

						double dist = pos.distanceTo(hitVec);

						// if penetration is enabled, run impact for all intersecting entities
						if(config.doesPenetrate()) {
							if(hitCoin == null || dist < closestCoin) this.onImpact(new EntityHitResult(entity, hitVec)); // [weapons batch F2: coin ricochet]
						} else {
							if(dist < nearest || nearest == 0.0D) {
								hitEntity = entity;
								nearest = dist;
								nonPenImpact = hitVec;
							}
						}
					}
				}
			}

			// if not, only run it for the closest MOP
			if(!config.doesPenetrate() && hitEntity != null) {
				mop = new EntityHitResult(hitEntity, nonPenImpact);
			}

			// [weapons batch F2: coin ricochet]
			if(hitCoin != null) {
				ricochetOffCoin(hitCoin, coinHit);
				return;
			}
		}

		if(mop != null) {
			this.onImpact(mop);
			this.beamLength = mop.getLocation().subtract(pos).length();
		} else {
			this.beamLength = nextPos.subtract(pos).length();
		}
	}

	/**
	 * [weapons batch F2: coin ricochet] The beam ends at the coin, the coin dies and a new beam with 1.25x damage
	 * (thrown by the coin's owner, else this beam's thrower) is fired from the coin at the nearest coin, else player,
	 * else hostile mob, else living entity within 50 blocks, or straight down with a random spread; "largeexplode"
	 * (size 1.5, count 1) at the coin.
	 */
	private void ricochetOffCoin(com.atomiclegacy.entity.LegacyCoinEntity hitCoin, Vec3 coinHit) {
		this.beamLength = coinHit.subtract(new Vec3(this.getX(), this.getY(), this.getZ())).length();

		double range = 50;
		List<Entity> targets = this.level().getEntities((Entity) null, new AABB(coinHit, coinHit).inflate(range, range, range), e -> true);
		Entity nearestCoin = null;
		Entity nearestPlayer = null;
		Entity nearestMob = null;
		Entity nearestOther = null;
		double coinDist = 0;
		double playerDist = 0;
		double mobDist = 0;
		double otherDist = 0;

		double coinX = hitCoin.getX(), coinY = hitCoin.centerY(), coinZ = hitCoin.getZ();
		hitCoin.discard();

		for(Entity entity : targets) {
			if(entity == this.getThrower()) continue;
			if(entity.isRemoved()) continue;
			double ey = entity instanceof com.atomiclegacy.entity.LegacyCoinEntity c ? c.centerY() : entity.getY();
			double dist = Math.sqrt((entity.getX() - coinX) * (entity.getX() - coinX) + (ey - coinY) * (ey - coinY) + (entity.getZ() - coinZ) * (entity.getZ() - coinZ));
			if(dist > range) continue;

			if(entity instanceof com.atomiclegacy.entity.LegacyCoinEntity) {
				if(coinDist == 0 || dist < coinDist) { coinDist = dist; nearestCoin = entity; }
			} else if(entity instanceof net.minecraft.world.entity.player.Player) {
				if(playerDist == 0 || dist < playerDist) { playerDist = dist; nearestPlayer = entity; }
			} else if(entity instanceof net.minecraft.world.entity.monster.Monster) {
				if(mobDist == 0 || dist < mobDist) { mobDist = dist; nearestMob = entity; }
			} else if(entity instanceof LivingEntity) {
				if(otherDist == 0 || dist < otherDist) { otherDist = dist; nearestOther = entity; }
			}
		}

		// ternary of shame
		Entity target = nearestCoin != null ? nearestCoin :
			nearestPlayer != null ? nearestPlayer :
			nearestMob != null ? nearestMob :
			nearestOther != null ? nearestOther : null;

		LivingEntity shooter = hitCoin.getThrower() != null ? hitCoin.getThrower() : this.thrower;
		LegacyBulletBeamEntity newBeam = new LegacyBulletBeamEntity(shooter, this.config, this.damage * 1.25F);
		newBeam.setPos(coinHit.x, coinHit.y, coinHit.z);
		if(target != null) {
			double ty = target instanceof com.atomiclegacy.entity.LegacyCoinEntity c ? c.centerY() : target.getY();
			Vec3 delta = new Vec3(target.getX() - newBeam.getX(), (ty + target.getBbHeight() / 2D) - newBeam.getY(), target.getZ() - newBeam.getZ());
			newBeam.setRotationsFromVector(delta);
		} else {
			newBeam.setRotationsFromVector(new Vec3(this.random.nextGaussian() * 0.5, -1, this.random.nextGaussian() * 0.5));
		}
		newBeam.performHitscanExternal(250D);
		this.level().addFreshEntity(newBeam);

		if(this.level() instanceof net.minecraft.server.level.ServerLevel server) {
			net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server, null, coinHit.x, coinHit.y, coinHit.z, 100, new com.atomiclegacy.network.GunTinyExplosionPayload(coinHit.x, coinHit.y, coinHit.z, 1.5F, 1));
		}
	}

	protected void onImpact(HitResult mop) {
		if(!this.level().isClientSide) {
			if(this.config.onImpactBeam != null) this.config.onImpactBeam.accept(this, mop);
		}
	}

	/** Entity#ignoreFrustumCheck plus renderDistanceWeight 10. */
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double size = this.getBoundingBox().getSize() * 64.0D * 10.0D;
		return distance < size * size;
	}

	@Override public boolean isPickable() { return false; }
	@Override public boolean fireImmune() { return true; }
	@Override public boolean displayFireAnimation() { return false; }
	@Override public boolean shouldBeSaved() { return false; }
	@Override protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }
	@Override protected void addAdditionalSaveData(CompoundTag tag) { }

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(BulletConfig.configs.indexOf(this.config));
		buf.writeDouble(this.beamLength);
		buf.writeFloat(this.getYRot());
		buf.writeFloat(this.getXRot());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buf) {
		int id = buf.readVarInt();
		this.config = id >= 0 && id < BulletConfig.configs.size() && BulletConfig.configs.get(id) instanceof BeamBulletConfig beam ? beam : null;
		this.beamLength = buf.readDouble();
		float yaw = buf.readFloat();
		float pitch = buf.readFloat();
		this.setYRot(yaw);
		this.setXRot(pitch);
		this.yRotO = yaw;
		this.xRotO = pitch;
	}
}
