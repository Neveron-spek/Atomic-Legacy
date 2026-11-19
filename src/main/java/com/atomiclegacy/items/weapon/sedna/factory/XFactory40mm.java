package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.AnimationLoader;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 XFactory40mm: the three 26mm flares, the five 40mm grenades, gun_flaregun, gun_congolake and
 * gun_mk108. The flare impact (LAMBDA_STANDARD_IGNITE, HbmLivingProps.fire += 200 on living entities, then
 * the standard ricochet/entity hit) is handled by LegacyBulletEntity for the FLARE family.
 * LAMBDA_SPAWN_C130_SUPPLIESS / LAMBDA_SPAWN_C130_WEAPONS: [parity W1: airdrop] LegacyC130Entity.
 * Not ported: the casing items (EnumCasingType.LARGE) and the loot/mob pools.
 */
public class XFactory40mm {

	/** Shooter-protection of the 40mm impact lambdas: entity hits on the thrower during the first 3 ticks are ignored. */
	private static boolean hitsOwnerEarly(LegacyBulletEntity bullet, HitResult hit) {
		return hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.ownerEntity();
	}

	/** LAMBDA_STANDARD_EXPLODE: Lego.standardExplode(bullet, mop, 5F), the bullet dies. */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE = (bullet, hit) -> {
		XFactory12ga.standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), 5F, 1F);
		return true;
	};

	/**
	 * LAMBDA_STANDARD_EXPLODE_HEAT: Lego.standardExplode(3.5F), then a direct hit on the entity with
	 * EntityDamageUtil.attackEntityFromNT(living, EXPLOSIVE, damage * 3, true, true, 0.5, 3F, 0.15F), or a plain
	 * attackEntityFrom for non-living entities.
	 */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_HEAT = (bullet, hit) -> {
		if(hitsOwnerEarly(bullet, hit)) return false;
		XFactory12ga.standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), 3.5F, 1F);
		if(hit instanceof EntityHitResult ehr) {
			Entity entity = ehr.getEntity();
			Entity thrower = bullet.ownerEntity();
			DamageSource source = thrower instanceof LivingEntity shooter
					? bullet.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, bullet, shooter)
					: bullet.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE);
			float amount = bullet.baseDamage() * 3F;
			if(entity instanceof LivingEntity living) {
				attackExplosiveNT(living, source, amount, 0.5D, 3F, 0.15F);
			} else {
				entity.hurt(source, amount);
			}
		}
		return true;
	};

	/**
	 * LAMBDA_STANDARD_EXPLODE_DEMO: ExplosionVNT(5F) with BlockAllocatorStandard, BlockProcessorStandard,
	 * EntityProcessorCrossSmooth(1, damage) (no piercing), PlayerProcessorStandard, ExplosionEffectWeapon(10, 2.5F, 1F).
	 */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_DEMO = (bullet, hit) -> {
		if(hitsOwnerEarly(bullet, hit)) return false;
		demoExplode(bullet, hit.getLocation(), 5F);
		return true;
	};

	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_INC = (bullet, hit) -> spawnFire(bullet, hit, false, 200);
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_PHOSPHORUS = (bullet, hit) -> spawnFire(bullet, hit, true, 400);

	/**
	 * spawnFire: Lego.standardExplode(3F), EntityFireLingering setArea(5, 2) of the given duration and type at the hit
	 * vector, then fire in every air block of the 3x3x3 cube around the hit that touches a flammable face.
	 */
	public static boolean spawnFire(LegacyBulletEntity bullet, HitResult hit, boolean phosphorus, int duration) {
		if(hitsOwnerEarly(bullet, hit)) return false;
		Level world = bullet.level();
		Vec3 vec = hit.getLocation();
		XFactory12ga.standardExplode(bullet, vec, bullet.baseDamage(), 3F, 1F);
		LegacyProjectileUtil.spawnLingeringFire(bullet, vec, phosphorus ? LegacyLingeringFireEntity.FireType.PHOSPHORUS : LegacyLingeringFireEntity.FireType.DIESEL,
				5F, 2F, duration, false);
		for(int dx = -1; dx <= 1; dx++) {
			for(int dy = -1; dy <= 1; dy++) {
				for(int dz = -1; dz <= 1; dz++) {
					BlockPos pos = new BlockPos(Mth.floor(vec.x) + dx, Mth.floor(vec.y) + dy, Mth.floor(vec.z) + dz);
					if(world.getBlockState(pos).isAir()) for(Direction dir : Direction.values()) { // ForgeDirection.VALID_DIRECTIONS order
						BlockPos n = pos.relative(dir);
						if(world.getBlockState(n).isFlammable(world, n, dir.getOpposite())) {
							world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
							break;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * EntityDamageUtil.attackEntityFromNT with DamageResistanceHandler.setup(pierceDT, pierce): cancelled by the
	 * no-PVP rule, by a threshold covering the damage or a full resistance; otherwise DT/DR, the 1.7.10 armor formula
	 * with currentPDR = pierce (sedna_explosive bypasses vanilla armor), no i-frames and the given knockback.
	 */
	public static boolean attackExplosiveNT(LivingEntity living, DamageSource source, float amount, double knockbackMultiplier, float pierceDT, float pierce) {
		Entity attacker = source.getEntity();
		if(living instanceof ServerPlayer target && attacker instanceof Player player && !target.canHarmPlayer(player)) return false;
		amount = LegacyProjectileUtil.sednaExplosiveDamage(living, amount, pierceDT, pierce);
		if(amount <= 0F) return false;
		amount = amount * (25F - living.getArmorValue() * (1F - pierce)) / 25F;
		Vec3 motion = living.getDeltaMovement();
		living.invulnerableTime = 0;
		boolean registered = living.hurt(source, amount);
		living.setDeltaMovement(motion);
		if(registered && attacker != null && knockbackMultiplier > 0) BeamBulletConfig.knockBack(living, attacker, knockbackMultiplier);
		living.hurtMarked = true;
		return registered;
	}

	/** ExplosionVNT order: block allocation, entity processing, block processing, player knockback, SFX. */
	public static void demoExplode(LegacyBulletEntity bullet, Vec3 hitVec, float size) {
		if(!(bullet.level() instanceof ServerLevel level)) return;
		double x = hitVec.x, y = hitVec.y, z = hitVec.z;
		Explosion compat = new Explosion(level, bullet.ownerEntity(), x, y, z, size, false, Explosion.BlockInteraction.DESTROY);

		/// BlockAllocatorStandard(16) ///
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

		/// EntityProcessorCrossSmooth(1, damage) + PlayerProcessorStandard + ExplosionEffectWeapon(10, 2.5F, 1F) ///
		// Same processing as Lego.standardExplode; g40_demo has no armor threshold negation / piercing, matching
		// the missing setupPiercing call. Player knockback and SFX happen before the block processing here, which
		// changes no game state compared to the 1.7.10 order (blocks are allocated and entities processed first).
		XFactory12ga.standardExplode(bullet, hitVec, bullet.baseDamage(), size, 1F);

		/// BlockProcessorStandard ///
		float dropChance = 1.0F / size;
		for(BlockPos pos : affectedBlocks) {
			BlockState state = level.getBlockState(pos);
			if(state.isAir()) continue;
			boolean canDrop = state.canDropFromExplosion(level, pos, compat);
			state.onExplosionHit(level, pos, compat, (stack, dropPos) -> {
				if(canDrop && level.random.nextFloat() <= dropChance) Block.popResource(level, dropPos, stack);
			});
		}
	}

	// [parity W1: airdrop] XFactory40mm#spawnPlane
	public static final com.atomiclegacy.items.StandardAmmoItem.BulletUpdate LAMBDA_SPAWN_C130_SUPPLIESS = (bullet) -> { spawnPlane(bullet, com.atomiclegacy.entity.LegacyC130Entity.PayloadType.SUPPLIES); };
	public static final com.atomiclegacy.items.StandardAmmoItem.BulletUpdate LAMBDA_SPAWN_C130_WEAPONS = (bullet) -> { spawnPlane(bullet, com.atomiclegacy.entity.LegacyC130Entity.PayloadType.WEAPONS); };

	public static void spawnPlane(LegacyBulletEntity bullet, com.atomiclegacy.entity.LegacyC130Entity.PayloadType payload) {
		if(!bullet.level().isClientSide && bullet.tickCount == 40 && bullet.level() instanceof ServerLevel level) {
			Entity thrower = bullet.ownerEntity();
			if(thrower != null) level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), HbmSoundEvents.TECH_BLEEP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
			com.atomiclegacy.entity.LegacyC130Entity c130 = new com.atomiclegacy.entity.LegacyC130Entity(level);
			int x = (int) Math.floor(bullet.getX());
			int z = (int) Math.floor(bullet.getZ());
			// World#getHeightValue: the 1.7.10 height map counts every light-blocking block, leaves and fluids included.
			int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
			c130.fac(level, x, y, z, payload);
			com.atomiclegacy.entity.LegacyC130Entity.loadAndSpawn(level, c130);
			c130.setTrackingRange(com.atomiclegacy.entity.LegacyC130Entity.FLARE_TRACKING_RANGE);
		}
	}

	static {
		// g26_flare*: setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false), onImpact LAMBDA_STANDARD_IGNITE
		for(StandardAmmoType flare : new StandardAmmoType[] {StandardAmmoType.G26_FLARE, StandardAmmoType.G26_FLARE_SUPPLY, StandardAmmoType.G26_FLARE_WEAPON}) {
			flare.setLife(100).setVel(2D).setGrav(0.015D);
		}
		// [parity W1: airdrop] g26_flare_supply / g26_flare_weapon .setOnUpdate(LAMBDA_SPAWN_C130_SUPPLIESS / _WEAPONS)
		StandardAmmoType.G26_FLARE_SUPPLY.setOnUpdate(LAMBDA_SPAWN_C130_SUPPLIESS);
		StandardAmmoType.G26_FLARE_WEAPON.setOnUpdate(LAMBDA_SPAWN_C130_WEAPONS);
		// g40_base: setLife(200).setVel(2F).setGrav(0.035D)
		StandardAmmoType.G40_HE.setLife(200).setVel(2D).setGrav(0.035D).setOnImpact(LAMBDA_STANDARD_EXPLODE);
		StandardAmmoType.G40_HEAT.setLife(200).setVel(2D).setGrav(0.035D).setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT);
		StandardAmmoType.G40_DEMO.setLife(200).setVel(2D).setGrav(0.035D).setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO);
		StandardAmmoType.G40_INC.setLife(200).setVel(2D).setGrav(0.035D).setOnImpact(LAMBDA_STANDARD_EXPLODE_INC);
		StandardAmmoType.G40_PHOSPHORUS.setLife(200).setVel(2D).setGrav(0.035D).setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS);
	}

	public static final BulletConfig g26_flare = new BulletConfig(StandardAmmoType.G26_FLARE).setCasingEffect(OrchestrasD3.CASING_G26_FLARE);
	public static final BulletConfig g26_flare_supply = new BulletConfig(StandardAmmoType.G26_FLARE_SUPPLY).setCasingEffect(OrchestrasD3.CASING_G26_FLARE_SUPPLY);
	public static final BulletConfig g26_flare_weapon = new BulletConfig(StandardAmmoType.G26_FLARE_WEAPON).setCasingEffect(OrchestrasD3.CASING_G26_FLARE_WEAPON);

	public static final BulletConfig g40_he = new BulletConfig(StandardAmmoType.G40_HE).setCasingEffect(OrchestrasD3.CASING_G40);
	public static final BulletConfig g40_heat = new BulletConfig(StandardAmmoType.G40_HEAT).setCasingEffect(OrchestrasD3.CASING_G40_HEAT);
	public static final BulletConfig g40_demo = new BulletConfig(StandardAmmoType.G40_DEMO).setCasingEffect(OrchestrasD3.CASING_G40_DEMO);
	public static final BulletConfig g40_inc = new BulletConfig(StandardAmmoType.G40_INC).setCasingEffect(OrchestrasD3.CASING_G40_INC);
	public static final BulletConfig g40_phosphorus = new BulletConfig(StandardAmmoType.G40_PHOSPHORUS).setCasingEffect(OrchestrasD3.CASING_G40_PHOS);

	public static ItemGunBaseNT createFlaregun(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(100).draw(7).inspect(39).crosshair(Crosshair.L_CIRCUMFLEX).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::flareDamage).delay(20).reload(28).jam(33).sound(HbmSoundEvents.GUN_UNDERBARREL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(g26_flare, g26_flare_supply, g26_flare_weapon))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_GL))
				.setupStandardConfiguration()
				.anim(LAMBDA_FLAREGUN_ANIMS).orchestra(OrchestrasD3.ORCHESTRA_FLAREGUN)
				).setDefaultAmmo(StandardAmmoType.G26_FLARE, 3);
	}

	public static ItemGunBaseNT createCongoLake(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(400).draw(7).inspect(39).reloadSequential(true).reloadChangeType(true).crosshair(Crosshair.L_CIRCUMFLEX).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::congoLakeDamage).delay(24).reload(16, 16, 16, 0).jam(0).sound(HbmSoundEvents.GUN_CONGO_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 4).addConfigs(g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_GL))
				.setupStandardConfiguration()
				.anim(LAMBDA_CONGOLAKE_ANIMS).orchestra(OrchestrasD3.ORCHESTRA_CONGOLAKE)
				).setDefaultAmmo(StandardAmmoType.G40_HE, 8);
	}

	public static ItemGunBaseNT createMK108(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(5_000).draw(20).inspect(65).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
				.rec(new Receiver(0)
						.dmg(HbmConfig::mk108Damage).delay(10).auto(true).dryfireAfterAuto(true).reload(135).jam(25).sound(HbmSoundEvents.GUN_MK108_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 30).addConfigs(g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus))
						.offset(0.75, -0.125, -0.125)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MK108))
				.setupStandardConfiguration()
				.anim(LAMBDA_MK108_ANIMS).orchestra(OrchestrasD3.ORCHESTRA_MK108)
				).setDefaultAmmo(StandardAmmoType.G40_HE, 50);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
		Lego.handleStandardSmoke(ctx.entity, stack, 1500, 0.025D, 1.05D, 0);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_GL = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MK108 = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.0) + 1F, (float) (ctx.getPlayer().getRandom().nextGaussian()));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAREGUN_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
				.addBus("HAMMER", new BusAnimationSequence().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("HAMMER", new BusAnimationSequence().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
		case RELOAD: return new BusAnimation()
				.addBus("OPEN", new BusAnimationSequence().addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 750).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("SHELL", new BusAnimationSequence().addPos(4, -8, -4, 0).addPos(4, -8, -4, 200).addPos(0, 0, -5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 800).addPos(0, 0, 0, 200, IType.SIN_DOWN));
		case JAMMED: return new BusAnimation()
				.addBus("OPEN", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 550).addPos(0, 0, 0, 200, IType.SIN_DOWN));
		case INSPECT: return new BusAnimation()
				.addBus("FLIP", new BusAnimationSequence().addPos(-360 * 3, 0, 0, 1500, IType.SIN_FULL));
		}
		return null;
	};

	/**
	 * ResourceManager.congolake_anim (models/weapons/animations/congolake.json). Loaded on first use
	 * because AnimationLoader reads through the client resource manager; the animation lambda
	 * only runs on the client.
	 */
	private static final class CongoLakeAnim {
		static final HashMap<String, BusAnimation> congolake_anim = AnimationLoader.load(AtomicLegacy.id("models/weapons/animations/congolake.json"));
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CONGOLAKE_ANIMS = (stack, type) -> {
		// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
		int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
		switch(type) {
		case EQUIP: return CongoLakeAnim.congolake_anim.get("Equip");
		case CYCLE: return CongoLakeAnim.congolake_anim.get(ammo <= 1 ? "FireEmpty" : "Fire");
		case RELOAD: return CongoLakeAnim.congolake_anim.get(ammo == 0 ? "ReloadEmpty" : "ReloadStart");
		case RELOAD_CYCLE: return CongoLakeAnim.congolake_anim.get("Reload");
		case RELOAD_END: return CongoLakeAnim.congolake_anim.get("ReloadEnd");
		case JAMMED: return CongoLakeAnim.congolake_anim.get("Jammed");
		case INSPECT: return CongoLakeAnim.congolake_anim.get("Inspect");
		}
		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MK108_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().setPos(45, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
		case CYCLE:
			int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
			return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().hold(50).addPos(0, 0, -0.25, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("BARREL", new BusAnimationSequence().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(1, 0, 0, 150))
				.addBus("SHELLS", new BusAnimationSequence().setPos(amount - 1, 0, 0));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("HAMMER", new BusAnimationSequence().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
		case RELOAD: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(10, 0, 0, 500, IType.SIN_FULL).holdUntil(1250).addPos(-50, 0, 0, 750, IType.SIN_FULL).holdUntil(5500).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("LID", new BusAnimationSequence().addPos(60, 0, 0, 500, IType.SIN_FULL).holdUntil(6000).addPos(0, 0, 0, 500, IType.SIN_UP))
				.addBus("BELT", new BusAnimationSequence().setPos(1, 0, 0).hold(500).addPos(0, 0, 0, 750, IType.SIN_UP).holdUntil(4500).addPos(1, 0, 0, 750, IType.SIN_UP))
				.addBus("DRUM", new BusAnimationSequence().hold(2000).addPos(2.5, 0, 0, 500, IType.SIN_DOWN).addPos(2.5, -2, -8, 500, IType.SIN_UP).setPos(4, -3, -8).addPos(2.5, 0, 0, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
		case JAMMED: return new BusAnimation()
				.addBus("LID", new BusAnimationSequence().hold(250).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP))
				.addBus("LIFT", new BusAnimationSequence().hold(1000).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL));
		case INSPECT:
			int yeetHorizontal = 750;
			int untilImpact = yeetHorizontal * 9 / 15;
			int delay = 250;
			int height = 6;
			int arcUp = untilImpact * 5 / 8;
			int arcDown = untilImpact * 3 / 8;
			return new BusAnimation()
					.addBus("LIFT", new BusAnimationSequence().hold(untilImpact).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL).hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL).hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
					.addBus("GRENH1", new BusAnimationSequence().setPos(9, 0, 0).addPos(-6, 0, 0, yeetHorizontal))
					.addBus("GRENV1", new BusAnimationSequence().setPos(0, -2, 0).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
					.addBus("GRENS1", new BusAnimationSequence().addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact))
					.addBus("GRENH2", new BusAnimationSequence().setPos(9, 0, 0).hold(delay).addPos(-6, 0, 0, yeetHorizontal))
					.addBus("GRENV2", new BusAnimationSequence().setPos(0, -2, 0).hold(delay).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
					.addBus("GRENS2", new BusAnimationSequence().hold(delay).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact))
					.addBus("GRENH3", new BusAnimationSequence().setPos(9, 0, 0).hold(delay * 2).addPos(-6, 0, 0, yeetHorizontal))
					.addBus("GRENV3", new BusAnimationSequence().setPos(0, -2, 0).hold(delay * 2).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
					.addBus("GRENS3", new BusAnimationSequence().hold(delay * 2).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact));
		}
		return null;
	};
}
