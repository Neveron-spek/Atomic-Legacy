package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.function.BiFunction;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity.FireType;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm.CloneBulletConfig;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChemthrower;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFluid;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.AnimationLoader;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * [weapons batch F1] 1.7.10 XFactoryFlamer: the 14 flame configs (flame_diesel/gas/napalm/balefire, flame_nograv,
 * flame_nograv_bf, flame_topaz_*, flame_daybreaker_*), gun_flamer (A_SIDE), gun_flamer_topaz (B_SIDE),
 * gun_flamer_daybreaker (LEGENDARY) and gun_chemthrower (A_SIDE, ItemGunChemthrower).
 *
 * <p>The projectile side of a 1.7.10 BulletConfig lives in {@link StandardAmmoType}; the four base rounds are the
 * ammo_standard flame variants, the ten clones are extra enum constants matched against the base items
 * ({@link CloneBulletConfig}). setCasing(plate_steel x2, 500) feeds the casing bag through LegacyCasingTable [parity b1].
 * LAMBDA_FIRE / LAMBDA_BALEFIRE (the client-side FlameCreator particle) is the FLAME family particle of
 * {@code LegacyBulletEntity#tick}; the balefire flag of the bullet selects META_BALEFIRE.
 * The turrets keep using the flame rounds through the same enum constants.
 */
public class XFactoryFlamer {

	/// LAMBDA_IGNITE_FIRE: HbmLivingProps.fire = max(fire, 100); the standard entity hit (fire damage) follows ///
	public static final BulletImpact LAMBDA_IGNITE_FIRE = (bullet, hit) -> {
		if(hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
			HbmLivingHazards props = HbmLivingHazards.get(living);
			if(props.getFire() < 100) props.extendFire(100);
			HbmLivingHazards.set(living, props);
		}
		return false;
	};
	/// LAMBDA_IGNITE_BALEFIRE: HbmLivingProps.balefire = max(balefire, 200) ///
	public static final BulletImpact LAMBDA_IGNITE_BALEFIRE = (bullet, hit) -> {
		if(hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
			HbmLivingHazards props = HbmLivingHazards.get(living);
			if(props.getBalefire() < 200) props.extendBalefire(200);
			HbmLivingHazards.set(living, props);
		}
		return false;
	};
	/// onRicochet (block hits): the bullet stops when igniteIfPossible / spawnFire discarded it, otherwise it flies on ///
	public static final BulletImpact LAMBDA_LINGER_DIESEL = (bullet, hit) -> { if(!igniteIfPossible(bullet, hit)) spawnFire(bullet, hit, 2F, 1F, 100, FireType.DIESEL); return false; };
	public static final BulletImpact LAMBDA_LINGER_GAS = (bullet, hit) -> { igniteIfPossible(bullet, hit); return false; };
	public static final BulletImpact LAMBDA_LINGER_NAPALM = (bullet, hit) -> { if(!igniteIfPossible(bullet, hit)) spawnFire(bullet, hit, 2.5F, 1F, 200, FireType.DIESEL); return false; };
	public static final BulletImpact LAMBDA_LINGER_BALEFIRE = (bullet, hit) -> { spawnFire(bullet, hit, 3F, 1F, 300, FireType.BALEFIRE); return false; };

	/**
	 * XFactoryFlamer.igniteIfPossible: on a block hit, if the block is flammable on the hit face and the block in front of
	 * that face is air, fire is placed there and the bullet keeps flying (true). Otherwise the bullet dies (false).
	 */
	public static boolean igniteIfPossible(LegacyBulletEntity bullet, HitResult hit) {
		if(hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
			Level world = bullet.level();
			BlockPos pos = bhr.getBlockPos();
			Direction dir = bhr.getDirection();
			BlockState state = world.getBlockState(pos);
			if(state.isFlammable(world, pos, dir.getOpposite())) {
				BlockPos firePos = pos.relative(dir);
				if(world.getBlockState(firePos).isAir()) {
					world.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
					return true;
				}
			}
			bullet.discard();
		}
		return false;
	}

	/** XFactoryFlamer.spawnFire: one EntityFireLingering at the hit vector unless another one is within (w/2+0.5, h/2+0.5), then the bullet dies. */
	public static void spawnFire(LegacyBulletEntity bullet, HitResult hit, float width, float height, int duration, FireType type) {
		if(hit.getType() == HitResult.Type.BLOCK) {
			LegacyProjectileUtil.spawnLingeringFire(bullet, hit.getLocation(), type, width, height, duration, true);
			bullet.discard();
		}
	}

	/** flame_daybreaker_* onImpact: Lego.standardExplode(range), optional spawnFire, bullet dies (entity and block hits). */
	private static BulletImpact daybreaker(float range, float fireWidth, float fireHeight, int fireDuration, FireType fireType) {
		return (bullet, hit) -> {
			XFactory12ga.standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), range, 1F);
			if(fireType != null) spawnFire(bullet, hit, fireWidth, fireHeight, fireDuration, fireType);
			if(!bullet.isRemoved() && !bullet.isRetired()) bullet.discard();
			return true;
		};
	}

	/** BulletConfig#clone(): copies the projectile fields of the source round onto the clone constant. */
	private static StandardAmmoType clone(StandardAmmoType target, StandardAmmoType source) {
		return target.setProjectiles(source.projectileCount(), source.projectileCountMax()).setSpread(source.projectileSpread())
				.setRicochetAngle(source.ricochetAngle()).setRicochetCount(source.maxRicochetCount())
				.setVel(source.projectileSpeed()).setGrav(source.projectileGravity()).setLife(source.projectileLifetime())
				.setKnockback(source.knockbackMultiplier()).setSelfDamageDelay(source.selfDamageDelay())
				.setBlackPowder(source.blackPowder()).setSpectral(source.spectral())
				.setOnImpact(source.onImpact()).setOnUpdate(source.onUpdate()).setOnRicochet(source.onRicochet()).setOnEntityHit(source.onEntityHit());
	}

	static {
		// setupDamageClass(FIRE) is the FLAME family damage path; LAMBDA_FIRE/LAMBDA_BALEFIRE are the client flame particles of the bullet entity
		StandardAmmoType.FLAME_DIESEL.setLife(100).setVel(1D).setGrav(0.02D).setSelfDamageDelay(20).setKnockback(0F)
				.setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(LAMBDA_LINGER_DIESEL);
		StandardAmmoType.FLAME_GAS.setLife(10).setSpread(0.05F).setVel(1D).setGrav(0D).setSelfDamageDelay(20).setKnockback(0F)
				.setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(LAMBDA_LINGER_GAS);
		StandardAmmoType.FLAME_NAPALM.setLife(200).setVel(1D).setGrav(0.02D).setSelfDamageDelay(20).setKnockback(0F)
				.setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(LAMBDA_LINGER_NAPALM);
		StandardAmmoType.FLAME_BALEFIRE.setLife(200).setVel(1D).setGrav(0.02D).setSelfDamageDelay(20).setKnockback(0F)
				.setOnImpact(LAMBDA_IGNITE_BALEFIRE).setOnRicochet(LAMBDA_LINGER_BALEFIRE);

		clone(StandardAmmoType.FLAME_NOGRAV, StandardAmmoType.FLAME_DIESEL).setGrav(0D);
		clone(StandardAmmoType.FLAME_NOGRAV_BF, StandardAmmoType.FLAME_BALEFIRE).setGrav(0D).setLife(100);

		clone(StandardAmmoType.FLAME_TOPAZ_DIESEL, StandardAmmoType.FLAME_DIESEL).setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0D);
		clone(StandardAmmoType.FLAME_TOPAZ_GAS, StandardAmmoType.FLAME_GAS).setProjectiles(2).setSpread(0.05F);
		clone(StandardAmmoType.FLAME_TOPAZ_NAPALM, StandardAmmoType.FLAME_NAPALM).setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0D);
		clone(StandardAmmoType.FLAME_TOPAZ_BALEFIRE, StandardAmmoType.FLAME_BALEFIRE).setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0D);

		clone(StandardAmmoType.FLAME_DAYBREAKER_DIESEL, StandardAmmoType.FLAME_DIESEL).setLife(200).setVel(2D).setGrav(0.035D)
				.setOnImpact(daybreaker(5F, 6F, 2F, 200, FireType.DIESEL));
		clone(StandardAmmoType.FLAME_DAYBREAKER_GAS, StandardAmmoType.FLAME_GAS).setLife(200).setVel(2D).setGrav(0.035D)
				.setOnImpact(daybreaker(5F, 0F, 0F, 0, null));
		clone(StandardAmmoType.FLAME_DAYBREAKER_NAPALM, StandardAmmoType.FLAME_NAPALM).setLife(200).setVel(2D).setGrav(0.035D)
				.setOnImpact(daybreaker(7.5F, 6F, 2F, 300, FireType.DIESEL));
		clone(StandardAmmoType.FLAME_DAYBREAKER_BALEFIRE, StandardAmmoType.FLAME_BALEFIRE).setLife(200).setVel(2D).setGrav(0.035D)
				.setOnImpact(daybreaker(5F, 7.5F, 2.5F, 400, FireType.BALEFIRE));
	}

	/** Rounds that spawn a balefire bullet (LAMBDA_BALEFIRE particle / balefire ignition), read by the bullet constructor. */
	public static boolean isBalefireFlame(StandardAmmoType ammo) {
		return ammo == StandardAmmoType.FLAME_BALEFIRE || ammo == StandardAmmoType.FLAME_NOGRAV_BF
				|| ammo == StandardAmmoType.FLAME_TOPAZ_BALEFIRE || ammo == StandardAmmoType.FLAME_DAYBREAKER_BALEFIRE;
	}

	/// BulletConfig registration order of XFactoryFlamer.init() (setReloadCount(500) on every flame round) ///
	public static final BulletConfig flame_diesel = new BulletConfig(StandardAmmoType.FLAME_DIESEL).setReloadCount(500);
	public static final BulletConfig flame_gas = new BulletConfig(StandardAmmoType.FLAME_GAS).setReloadCount(500);
	public static final BulletConfig flame_napalm = new BulletConfig(StandardAmmoType.FLAME_NAPALM).setReloadCount(500);
	public static final BulletConfig flame_balefire = new BulletConfig(StandardAmmoType.FLAME_BALEFIRE).setReloadCount(500);

	public static final BulletConfig flame_nograv = new CloneBulletConfig(StandardAmmoType.FLAME_NOGRAV, StandardAmmoType.FLAME_DIESEL).setReloadCount(500);
	public static final BulletConfig flame_nograv_bf = new CloneBulletConfig(StandardAmmoType.FLAME_NOGRAV_BF, StandardAmmoType.FLAME_BALEFIRE).setReloadCount(500);

	public static final BulletConfig flame_topaz_diesel = new CloneBulletConfig(StandardAmmoType.FLAME_TOPAZ_DIESEL, StandardAmmoType.FLAME_DIESEL).setReloadCount(500);
	public static final BulletConfig flame_topaz_gas = new CloneBulletConfig(StandardAmmoType.FLAME_TOPAZ_GAS, StandardAmmoType.FLAME_GAS).setReloadCount(500);
	public static final BulletConfig flame_topaz_napalm = new CloneBulletConfig(StandardAmmoType.FLAME_TOPAZ_NAPALM, StandardAmmoType.FLAME_NAPALM).setReloadCount(500);
	public static final BulletConfig flame_topaz_balefire = new CloneBulletConfig(StandardAmmoType.FLAME_TOPAZ_BALEFIRE, StandardAmmoType.FLAME_BALEFIRE).setReloadCount(500);

	public static final BulletConfig flame_daybreaker_diesel = new CloneBulletConfig(StandardAmmoType.FLAME_DAYBREAKER_DIESEL, StandardAmmoType.FLAME_DIESEL).setReloadCount(500);
	public static final BulletConfig flame_daybreaker_gas = new CloneBulletConfig(StandardAmmoType.FLAME_DAYBREAKER_GAS, StandardAmmoType.FLAME_GAS).setReloadCount(500);
	public static final BulletConfig flame_daybreaker_napalm = new CloneBulletConfig(StandardAmmoType.FLAME_DAYBREAKER_NAPALM, StandardAmmoType.FLAME_NAPALM).setReloadCount(500);
	public static final BulletConfig flame_daybreaker_balefire = new CloneBulletConfig(StandardAmmoType.FLAME_DAYBREAKER_BALEFIRE, StandardAmmoType.FLAME_BALEFIRE).setReloadCount(500);

	public static ItemGunBaseNT createFlamer(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::flamerDamage).spreadHipfire(0F).delay(1).auto(true).reload(90).jam(17)
						.mag(new MagazineFullReload(0, 300).addConfigs(flame_diesel, flame_gas, flame_napalm, flame_balefire))
						.offset(0.75, -0.0625, -0.25D)
						.setupStandardFire())
				.setupStandardConfiguration()
				.anim(LAMBDA_FLAMER_ANIMS).orchestra(OrchestrasF1.ORCHESTRA_FLAMER)
				).setDefaultAmmo(StandardAmmoType.FLAME_DIESEL, 1);
	}

	public static ItemGunBaseNT createFlamerTopaz(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE, new GunConfig()
				.dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::topazDamage).spreadHipfire(0F).delay(1).auto(true).reload(90).jam(17)
						.mag(new MagazineFullReload(0, 500).addConfigs(flame_topaz_diesel, flame_topaz_gas, flame_topaz_napalm, flame_topaz_balefire))
						.offset(0.75, -0.0625, -0.25D)
						.setupStandardFire())
				.setupStandardConfiguration()
				.anim(LAMBDA_FLAMER_ANIMS).orchestra(OrchestrasF1.ORCHESTRA_FLAMER)
				).setDefaultAmmo(StandardAmmoType.FLAME_DIESEL, 1);
	}

	public static ItemGunBaseNT createFlamerDaybreaker(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::daybreakerDamage).spreadHipfire(0F).delay(10).auto(true).reload(90).jam(17).sound(HbmSoundEvents.GUN_POWDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 50).addConfigs(flame_daybreaker_diesel, flame_daybreaker_gas, flame_daybreaker_napalm, flame_daybreaker_balefire))
						.offset(0.75, -0.0625, -0.25D)
						.setupStandardFire())
				.setupStandardConfiguration()
				.anim(LAMBDA_FLAMER_ANIMS).orchestra(OrchestrasF1.ORCHESTRA_FLAMER_DAYBREAKER)
				).setDefaultAmmo(StandardAmmoType.FLAME_DIESEL, 1);
	}

	public static ItemGunBaseNT createChemthrower(Item.Properties properties) {
		return new ItemGunChemthrower(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(90_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.delay(1).spreadHipfire(0F).auto(true)
						.mag(new MagazineFluid(0, 3_000))
						.offset(0.75, -0.0625, -0.25D)
						.canFire(ItemGunChemthrower.LAMBDA_CAN_FIRE).fire(ItemGunChemthrower.LAMBDA_FIRE))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_CHEMTHROWER_ANIMS).orchestra(OrchestrasF1.ORCHESTRA_CHEMTHROWER)
				);
	}

	/** ResourceManager.flamethrower_anim (models/weapons/animations/flamethrower.json); only loaded on the client. */
	private static HashMap<String, BusAnimation> flamethrower_anim;

	public static HashMap<String, BusAnimation> flamethrowerAnim() {
		if(flamethrower_anim == null) flamethrower_anim = AnimationLoader.load(AtomicLegacy.id("models/weapons/animations/flamethrower.json"));
		return flamethrower_anim;
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAMER_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case RELOAD: return flamethrowerAnim().get("Reload");
		case INSPECT:
		case JAMMED: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 45, 250, IType.SIN_FULL).addPos(0, 0, 45, 350).addPos(0, 0, -15, 150, IType.SIN_FULL).addPos(0, 0, 0, 100, IType.SIN_FULL));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CHEMTHROWER_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		}

		return null;
	};
}
