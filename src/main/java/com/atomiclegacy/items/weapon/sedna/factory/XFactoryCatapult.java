package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.network.GunMukeEffectPayload;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;
import com.atomiclegacy.satellite.SatelliteEventTracker;
import com.atomiclegacy.satellite.SatelliteEventTracker.BurstIntensity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * [weapons batch F2] 1.7.10 XFactoryCatapult: gun_fatman (M42 nuclear catapult) with the six mini nukes
 * nuke_standard / nuke_demo / nuke_high / nuke_tots / nuke_hive / nuke_balefire and the cluster_submunition config.
 *
 * <p>The ExplosionVNT setups of the original are reproduced by {@link #explodeVNT}: BlockAllocatorStandard(64),
 * BlockProcessorStandard with BlockMutatorFire / BlockMutatorBalefire, EntityProcessorCrossSmooth(nodeDist, damage)
 * .withRangeMod(1.5F) (allowSelfDamage, no piercing, DamageClass.EXPLOSIVE), PlayerProcessorStandard and
 * ExplosionEffectWeapon(10, 2.5F, 1F). EntityNukeExplosionMK5.statFac(35) is NukeExplosionManager.scheduleMk5Nuclear.
 * The balefire mushroom comes from MainRegistry.polaroidID == 11 (AtomicLegacy.polaroidID) or the 1/100 roll. The bullet renderers are LegoClient.RENDER_NUKE, RENDER_GRENADE (tots),
 * RENDER_HIVE, RENDER_NUKE_BALEFIRE and RENDER_BOMB (submunition).
 */
public class XFactoryCatapult {

	/** MainRegistry.proxy.me().getRNG() for LAMBDA_FATMAN_ANIMS (set by the client setup). */
	private static final RandomSource ANIM_RANDOM = RandomSource.create();
	public static Supplier<RandomSource> LOCAL_RANDOM = () -> ANIM_RANDOM;

	public static final BulletConfig nuke_standard = new BulletConfig(StandardAmmoType.NUKE_STANDARD);
	public static final BulletConfig nuke_demo = new BulletConfig(StandardAmmoType.NUKE_DEMO);
	public static final BulletConfig nuke_high = new BulletConfig(StandardAmmoType.NUKE_HIGH);
	public static final BulletConfig nuke_tots = new BulletConfig(StandardAmmoType.NUKE_TOTS);
	public static final BulletConfig nuke_hive = new BulletConfig(StandardAmmoType.NUKE_HIVE);
	public static final BulletConfig nuke_balefire = new BulletConfig(StandardAmmoType.NUKE_BALEFIRE);

	/**
	 * new BulletConfig().setLife(1_200).setGrav(0.025F).setOnImpact(LAMBDA_SUBMUNITION): no ammo item. The port's
	 * ClusterSubmunitionEntity (BombMulti) carries the same lifetime and gravity with its own fixed-damage impact;
	 * this config keeps the original registration slot and lambda for completeness.
	 */
	public static final BulletConfig cluster_submunition = new BulletConfig(StandardAmmoType.NONE);


	/** EntityBulletBaseMK4: entity hit on the thrower within the first 3 ticks is ignored by the lambda. */
	private static boolean hitsOwnerEarly(LegacyBulletEntity bullet, HitResult hit) {
		return hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.ownerEntity();
	}

	public static final BulletImpact LAMBDA_NUKE_STANDARD = (bullet, mop) -> {
		if(hitsOwnerEarly(bullet, mop)) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();

		explodeVNT(level, hit.x, hit.y, hit.z, 10, 0, null, 2, bullet.baseDamage(), 1.5F, false);

		incrementRad(level, hit.x, hit.y, hit.z, 1F);
		spawnMush(bullet, mop);
		return true;
	};

	public static final BulletImpact LAMBDA_NUKE_DEMO = (bullet, mop) -> {
		if(hitsOwnerEarly(bullet, mop)) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();

		explodeVNT(level, hit.x, hit.y, hit.z, 15, 64, Blocks.FIRE.defaultBlockState(), 2, bullet.baseDamage(), 1.5F, false);

		incrementRad(level, hit.x, hit.y, hit.z, 1.5F);
		spawnMush(bullet, mop);
		return true;
	};

	public static final BulletImpact LAMBDA_NUKE_HIGH = (bullet, mop) -> {
		if(hitsOwnerEarly(bullet, mop)) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();
		NukeExplosionManager.scheduleMk5Nuclear(level, hit.x, hit.y, hit.z, 35);
		spawnMush(bullet, mop);
		return true;
	};

	public static final BulletImpact LAMBDA_NUKE_BALEFIRE = (bullet, mop) -> {
		if(hitsOwnerEarly(bullet, mop)) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();

		explodeVNT(level, hit.x, hit.y, hit.z, 10, 64, HbmBlocks.BALEFIRE.get().defaultBlockState(), 2, bullet.baseDamage(), 1.5F, false);

		incrementRad(level, hit.x, hit.y, hit.z, 1.5F);

		SatelliteEventTracker.reportRadiation(level, SatelliteEventTracker.DETECTOR_LOW_DURATION, BurstIntensity.LOW, bullet.getX(), bullet.getZ());
		level.playSound(null, hit.x, hit.y + 0.5, hit.z, HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
		sendMukeEffect(level, hit.x, hit.y + 0.5, hit.z, GunMukeEffectPayload.MODE_MUKE, true);
		return true;
	};

	/** incrementRad(world, x, y, z, mult): 50 / (|i| + |j| + 1) * mult in the 5x5 chunk cross around the blast (|i| + |j| < 4). */
	public static void incrementRad(ServerLevel level, double posX, double posY, double posZ, float mult) {
		ChunkRadiationData radiation = ChunkRadiationData.get(level);
		for(int i = -2; i <= 2; i++) { for(int j = -2; j <= 2; j++) {
				if(Math.abs(i) + Math.abs(j) < 4) {
					radiation.incrementRadiation(new BlockPos(Mth.floor(posX + i * 16), Mth.floor(posY), Mth.floor(posZ + j * 16)), 50F / (Math.abs(i) + Math.abs(j) + 1) * mult);
				}
			}
		}
	}

	public static void spawnMush(LegacyBulletEntity bullet, HitResult mop) {
		if(!(bullet.level() instanceof ServerLevel level)) return;
		Vec3 hit = mop.getLocation();
		SatelliteEventTracker.reportRadiation(level, SatelliteEventTracker.DETECTOR_LOW_DURATION, BurstIntensity.LOW, bullet.getX(), bullet.getZ());
		level.playSound(null, hit.x, hit.y + 0.5, hit.z, HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
		sendMukeEffect(level, hit.x, hit.y + 0.5, hit.z, GunMukeEffectPayload.MODE_MUKE, com.atomiclegacy.AtomicLegacy.polaroidID == 11 || level.random.nextInt(100) == 0);
	}

	/** AuxParticlePacketNT {type: muke / tinytot, balefire} to everyone within 250 blocks. */
	public static void sendMukeEffect(ServerLevel level, double x, double y, double z, int mode, boolean balefire) {
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, 250, new GunMukeEffectPayload(x, y, z, mode, balefire));
	}

	public static final BulletImpact LAMBDA_NUKE_TINYTOT = (bullet, mop) -> {
		if(hitsOwnerEarly(bullet, mop)) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();

		explodeVNT(level, hit.x, hit.y, hit.z, 5, 0, null, 2, bullet.baseDamage(), 1.5F, false);

		incrementRad(level, hit.x, hit.y, hit.z, 0.25F);
		SatelliteEventTracker.reportRadiation(level, SatelliteEventTracker.DETECTOR_LOW_DURATION, BurstIntensity.LOW, bullet.getX(), bullet.getZ());
		level.playSound(null, hit.x, hit.y + 0.5, hit.z, HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
		sendMukeEffect(level, hit.x, hit.y + 0.5, hit.z, GunMukeEffectPayload.MODE_TINYTOT, com.atomiclegacy.AtomicLegacy.polaroidID == 11 || level.random.nextInt(100) == 0);
		return true;
	};

	public static final BulletImpact LAMBDA_NUKE_HIVE = (bullet, mop) -> {
		if(hitsOwnerEarly(bullet, mop)) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();
		explodeVNT(level, hit.x, hit.y, hit.z, 5, 0, null, 1, bullet.baseDamage(), 1.5F, true);
		return true;
	};

	/** ExplosionVNT(7.5F, thrower) with BlockAllocatorStandard(16), BlockProcessorStandard, EntityProcessorCrossSmooth(1, damage), PlayerProcessorStandard, ExplosionEffectWeapon(10, 2.5F, 1F). */
	public static final BulletImpact LAMBDA_SUBMUNITION = (bullet, mop) -> {
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();
		explodeVNT(level, bullet.ownerEntity(), hit.x, hit.y, hit.z, 7.5F, 16, null, 1, bullet.baseDamage(), 1F, true);
		return true;
	};

	static {
		// nuke_*: setLife(300).setVel(3F).setGrav(0.025F); tots: 8 projectiles, spread 0.1, damage 0.35; hive: 12 projectiles, vel 1, spread 0.15, damage 0.25
		StandardAmmoType.NUKE_STANDARD.setLife(300).setVel(3D).setGrav(0.025D).setOnImpact(LAMBDA_NUKE_STANDARD);
		StandardAmmoType.NUKE_DEMO.setLife(300).setVel(3D).setGrav(0.025D).setOnImpact(LAMBDA_NUKE_DEMO);
		StandardAmmoType.NUKE_HIGH.setLife(300).setVel(3D).setGrav(0.025D).setOnImpact(LAMBDA_NUKE_HIGH);
		StandardAmmoType.NUKE_TOTS.setProjectiles(8).setLife(300).setVel(3D).setGrav(0.025D).setSpread(0.1F).setOnImpact(LAMBDA_NUKE_TINYTOT);
		StandardAmmoType.NUKE_HIVE.setProjectiles(12).setLife(300).setVel(1D).setGrav(0.025D).setSpread(0.15F).setOnImpact(LAMBDA_NUKE_HIVE);
		StandardAmmoType.NUKE_BALEFIRE.setLife(300).setVel(3D).setGrav(0.025D).setOnImpact(LAMBDA_NUKE_BALEFIRE);
	}

	public static ItemGunBaseNT createFatMan(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(300).draw(20).inspect(30).reloadChangeType(true).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
				.rec(new Receiver(0)
						.dmg(HbmConfig::fatmanDamage).spreadHipfire(0F).delay(10).reload(57).jam(40).sound(HbmSoundEvents.GUN_FATMAN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(nuke_standard, nuke_demo, nuke_high, nuke_tots, nuke_hive, nuke_balefire))
						.offset(1, -0.0625 * 1.5, -0.1875D).offsetScoped(1, -0.0625 * 1.5, -0.125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_FATMAN))
				.setupStandardConfiguration()
				.anim(LAMBDA_FATMAN_ANIMS).orchestra(OrchestrasF2.ORCHESTRA_FATMAN)
				).setDefaultAmmoExpensive(StandardAmmoType.NUKE_STANDARD, 1);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_FATMAN = (stack, ctx) -> { };

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FATMAN_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
		case CYCLE:
			RandomSource rand = LOCAL_RANDOM.get();
			return new BusAnimation()
				.addBus("GAUGE", new BusAnimationSequence().addPos(0, 0, 135 + rand.nextInt(136), 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_DOWN))
				.addBus("PISTON", new BusAnimationSequence().addPos(0, 0, 3, 100, IType.SIN_UP))
				.addBus("NUKE", new BusAnimationSequence().addPos(0, 0, 3, 100, IType.SIN_UP).addPos(0, 0, 0, 0));
		case RELOAD: return new BusAnimation()
				.addBus("LID", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, -45, 250, IType.SIN_UP).addPos(0, 0, -45, 1200).addPos(0, 0, 0, 250, IType.SIN_UP))
				.addBus("HANDLE", new BusAnimationSequence().addPos(0, 0, -2, 500, IType.SIN_FULL).addPos(0, 0, -2, 1700).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("NUKE", new BusAnimationSequence().addPos(5, -4, 3, 0).addPos(5, -4, 3, 750).addPos(2, 0.5, 3, 500, IType.SIN_UP).addPos(1, 0.5, 3, 100).addPos(0, 0, 3, 100).addPos(0, 0, 3, 750).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("PISTON", new BusAnimationSequence().addPos(0, 0, 3, 0).addPos(0, 0, 3, 2200).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().addPos(5, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 450).addPos(3, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 500).addPos(-10, 0, 0, 375, IType.SIN_DOWN).addPos(0, 0, 0, 375, IType.SIN_UP));
		case JAMMED: return new BusAnimation()
				.addBus("HANDLE", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-15, 0, 0, 250, IType.SIN_FULL).addPos(-15, 0, 0, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("HANDLE", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().addPos(-15, 0, 0, 250, IType.SIN_FULL).addPos(-15, 0, 0, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
		}
		return null;
	};

	/* ===================== ExplosionVNT ===================== */

	public static void explodeVNT(ServerLevel level, double x, double y, double z, float size, int blockResolution, BlockState mutator, double nodeDist, float fixedDamage, float rangeMod, boolean weaponSfx) {
		explodeVNT(level, null, x, y, z, size, blockResolution, mutator, nodeDist, fixedDamage, rangeMod, weaponSfx);
	}

	/**
	 * ExplosionVNT.explode(): block allocation (BlockAllocatorStandard(resolution), none for resolution 0), entity
	 * processing (EntityProcessorCrossSmooth(nodeDist, fixedDamage).withRangeMod(rangeMod)), block processing
	 * (BlockProcessorStandard, drop chance 1 / size, optional fire / balefire mutator), player knockback
	 * (PlayerProcessorStandard) and the optional ExplosionEffectWeapon(10, 2.5F, 1F).
	 */
	public static void explodeVNT(ServerLevel level, Entity exploder, double x, double y, double z, float size, int blockResolution, BlockState mutator, double nodeDist, float fixedDamage, float rangeMod, boolean weaponSfx) {
		Explosion compat = new Explosion(level, exploder, x, y, z, size, false, Explosion.BlockInteraction.DESTROY);

		/// BlockAllocatorStandard(resolution) ///
		Set<BlockPos> affectedBlocks = null;
		if(blockResolution > 0) {
			affectedBlocks = new HashSet<>();
			int resolution = blockResolution;
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
		}

		/// EntityProcessorCrossSmooth(nodeDist, fixedDamage).withRangeMod(rangeMod): setAllowSelfDamage ///
		double knockbackMult = 1D;
		float range = size * 2.0F * rangeMod;
		AABB area = new AABB(x - range - 1.0D, y - range - 1.0D, z - range - 1.0D, x + range + 1.0D, y + range + 1.0D, z + range + 1.0D);
		List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
		EventHooks.onExplosionDetonate(level, compat, list, range);

		Vec3[] nodes;
		if(nodeDist > 0) {
			nodes = new Vec3[7];
			Direction[] dirs = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
			for(int i = 0; i < 6; i++) nodes[i] = new Vec3(x + dirs[i].getStepX() * nodeDist, y + dirs[i].getStepY() * nodeDist, z + dirs[i].getStepZ() * nodeDist);
			nodes[6] = new Vec3(x, y, z); // ForgeDirection.UNKNOWN
		} else {
			nodes = new Vec3[] { new Vec3(x, y, z) };
		}

		Map<Entity, Float> damageMap = new LinkedHashMap<>();
		HashMap<ServerPlayer, Vec3> affectedPlayers = new HashMap<>();

		for(Entity entity : list) {
			AABB box = entity.getBoundingBox();
			double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
			double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
			double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
			double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
			double distanceScaled = dist / range;

			if(distanceScaled <= 1.0D) {
				double deltaX = entity.getX() - x;
				double deltaY = entity.getEyeY() - y;
				double deltaZ = entity.getZ() - z;
				double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

				if(distance != 0.0D) {
					deltaX /= distance;
					deltaY /= distance;
					deltaZ /= distance;

					double density = 0;
					for(Vec3 vec : nodes) {
						double d = Explosion.getSeenPercent(vec, entity);
						if(d > density) density = d;
					}

					double knockback = (1.0D - distanceScaled) * density;
					float dmg = density < 0.125 ? 0F : (float) (fixedDamage * (1 - distanceScaled)); // EntityProcessorCrossSmooth.calculateDamage
					if(!damageMap.containsKey(entity) || damageMap.get(entity) < dmg) damageMap.put(entity, dmg);
					double enchKnockback = entity instanceof LivingEntity living ? knockback * (1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) : knockback;

					if(!(entity instanceof LegacyBulletEntity) && !(entity instanceof UniversalGrenadeEntity) && !(entity instanceof Player)) {
						entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * enchKnockback * knockbackMult, deltaY * enchKnockback * knockbackMult, deltaZ * enchKnockback * knockbackMult));
						entity.hurtMarked = true;
					}

					if(entity instanceof ServerPlayer player) {
						affectedPlayers.put(player, new Vec3(deltaX * knockback * knockbackMult, deltaY * knockback * knockbackMult, deltaZ * knockback * knockbackMult));
					}
				}
			}
		}

		for(Map.Entry<Entity, Float> entry : damageMap.entrySet()) {
			Entity entity = entry.getKey();
			float amount = entry.getValue();
			if(!entity.isAlive()) continue;
			if(exploder == entity) amount *= 0.5F;
			// BulletConfig.getDamage(null, exploder, EXPLOSIVE)
			DamageSource dmg = level.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, null, exploder instanceof LivingEntity ? exploder : null);
			if(entity instanceof LivingEntity living) {
				// EntityDamageUtil.attackEntityFromNT(living, dmg, amount, true, false, 0F, 0F, 0F)
				XFactory40mm.attackExplosiveNT(living, dmg, amount, 0D, 0F, 0F);
			} else {
				entity.hurt(dmg, amount);
			}
		}

		/// BlockProcessorStandard(.withBlockEffect(mutator)) ///
		if(affectedBlocks != null) {
			float dropChance = 1.0F / size;
			Set<BlockPos> exploded = new HashSet<>();
			for(BlockPos pos : affectedBlocks) {
				BlockState state = level.getBlockState(pos);
				if(state.isAir()) continue;
				exploded.add(pos);
				boolean canDrop = state.canDropFromExplosion(level, pos, compat);
				state.onExplosionHit(level, pos, compat, (stack, dropPos) -> {
					if(canDrop && level.random.nextFloat() <= dropChance) Block.popResource(level, dropPos, stack);
				});
			}
			if(mutator != null) {
				// BlockMutatorFire / BlockMutatorBalefire.mutatePost: air above an opaque cube, 1 in 3
				for(BlockPos pos : exploded) {
					BlockPos below = pos.below();
					if(level.getBlockState(pos).isAir() && level.getBlockState(below).isSolidRender(level, below) && level.random.nextInt(3) == 0) {
						level.setBlock(pos, mutator, 3);
					}
				}
			}
		}

		/// PlayerProcessorStandard: ExplosionKnockbackPacket adds the motion on the client ///
		for(Map.Entry<ServerPlayer, Vec3> entry : affectedPlayers.entrySet()) {
			ServerPlayer player = entry.getKey();
			player.setDeltaMovement(player.getDeltaMovement().add(entry.getValue()));
			player.hurtMarked = true;
		}

		/// ExplosionEffectWeapon(10, 2.5F, 1F) ///
		if(weaponSfx) LegacyProjectileUtil.sendSmallExplosionEffect(level, new Vec3(x, y, z), 10, 2.5F, 1F);
	}
}
