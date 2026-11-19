package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
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
 * 1.7.10 XFactory762mm: gun_carbine, gun_minigun, gun_minigun_lacunae and gun_mas36 with the
 * r762 rounds and the lacunae capacitor beams. [parity d2] gun_minigun_dual (WeaponQuality.DEBUG)
 * is ported with its two ServerConfig values.
 */
public class XFactory762mm {

	/** Original XWeaponModManager ids referenced by the renderers and orchestras; the bayonet and speedup mods are not ported. */
	public static final int ID_CARBINE_BAYONET = 219;
	public static final int ID_MAS_BAYONET = 213;
	public static final int ID_MINIGUN_SPEED = 208;
	public static final int ID_MINIGUN_SLOWDOWN = 207;

	/** Casing effect ids (TurretVisualClientEffects.CasingConfig R762 / R762_STEEL): casing762 BOTTLENECK, brass / COLOR_CASE_44. */
	public static final int CASING_R762 = 29;
	public static final int CASING_R762_STEEL = 30;

	public static final BulletConfig r762_sp = new BulletConfig(StandardAmmoType.R762_SP).setCasingEffect(CASING_R762);
	public static final BulletConfig r762_fmj = new BulletConfig(StandardAmmoType.R762_FMJ).setCasingEffect(CASING_R762);
	public static final BulletConfig r762_jhp = new BulletConfig(StandardAmmoType.R762_JHP).setCasingEffect(CASING_R762);
	public static final BulletConfig r762_ap = new BulletConfig(StandardAmmoType.R762_AP).setCasingEffect(CASING_R762_STEEL);
	public static final BulletConfig r762_du = new BulletConfig(StandardAmmoType.R762_DU).setCasingEffect(CASING_R762_STEEL);
	public static final BulletConfig r762_he = new BulletConfig(StandardAmmoType.R762_HE).setWear(3F).setCasingEffect(CASING_R762_STEEL);

	// setCasing(ingot_polymer x2, 160) feeds the casing bag through LegacyCasingTable [parity b1]
	public static final BeamBulletConfig energy_lacunae = (BeamBulletConfig) new BeamBulletConfig(StandardAmmoType.CAPACITOR).setupDamageClass(DamageClass.LASER).setLife(5).setRenderRotations(false).setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setLaserColor(0x60, 0x15, 0x80).setReloadCount(40);
	public static final BeamBulletConfig energy_lacunae_overcharge = (BeamBulletConfig) new BeamBulletConfig(StandardAmmoType.CAPACITOR_OVERCHARGE).setupDamageClass(DamageClass.LASER).setLife(5).setRenderRotations(false).setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setLaserColor(0x60, 0x15, 0x80).setReloadCount(40);
	public static final BeamBulletConfig energy_lacunae_ir = (BeamBulletConfig) new BeamBulletConfig(StandardAmmoType.CAPACITOR_IR).setupDamageClass(DamageClass.FIRE).setLife(5).setRenderRotations(false).setOnBeamImpact(XFactory762mm::irHit).setLaserColor(0x60, 0x15, 0x80).setReloadCount(40);

	static {
		StandardAmmoType.R762_HE.setOnImpact(XFactory762mm::tinyExplodeImpact);
	}

	public static ItemGunBaseNT createCarbine(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(3_000).draw(10).inspect(31).reloadSequential(true).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::carbineDamage).delay(5).dry(15).spread(0.0F).reload(30, 0, 15, 0).jam(60).sound(HbmSoundEvents.GUN_POWDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 14).addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_CARBINE))
				.setupStandardConfiguration()
				.anim(LAMBDA_CARBINE_ANIMS).orchestra(OrchestrasC3.ORCHESTRA_CARBINE)
				).setDefaultAmmo(StandardAmmoType.R762_SP, 14);
	}

	public static ItemGunBaseNT createMinigun(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::minigunDamage).delay(1).auto(true).dry(15).spread(0.01F).sound(HbmSoundEvents.GUN_MINIGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineBelt().addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MINIGUN))
				.setupStandardConfiguration()
				.anim(LAMBDA_MINIGUN_ANIMS).orchestra(OrchestrasC3.ORCHESTRA_MINIGUN)
				).setDefaultAmmo(StandardAmmoType.R762_FMJ, 30);
	}

	public static ItemGunBaseNT createMinigunLacunae(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::lacunaeDamage).delay(1).auto(true).dry(15).reload(15).spread(0.01F).sound(HbmSoundEvents.GUN_LASER_GATLING_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 200).addConfigs(energy_lacunae, energy_lacunae_overcharge, energy_lacunae_ir))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_LACUNAE))
				.setupStandardConfiguration()
				.anim(LAMBDA_MINIGUN_ANIMS).orchestra(OrchestrasC3.ORCHESTRA_MINIGUN)
				).setDefaultAmmo(StandardAmmoType.CAPACITOR, 15);
	}

	/** [parity d2] gun_minigun_dual (WeaponQuality.DEBUG: no creative tab, no recipe): config 0 is the left gun (primary key), config 1 the right gun (secondary key). */
	public static ItemGunBaseNT createMinigunDual(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.DEBUG,
				new GunConfig()
				.dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::minigunDualLeftDamage).delay(1).auto(true).dry(15).spread(0.01F).sound(HbmSoundEvents.GUN_MINIGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineBelt().addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
						.offset(1, -0.0625 * 2.5, 0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MINIGUN))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_MINIGUN_ANIMS).orchestra(OrchestrasC3.ORCHESTRA_MINIGUN_DUAL),
				new GunConfig()
				.dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::minigunDualRightDamage).delay(1).auto(true).dry(15).spread(0.01F).sound(HbmSoundEvents.GUN_MINIGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineBelt().addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MINIGUN))
				.ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(LAMBDA_SECOND_MINIGUN)
				.anim(LAMBDA_MINIGUN_ANIMS).orchestra(OrchestrasC3.ORCHESTRA_MINIGUN_DUAL)
				).setDefaultAmmo(StandardAmmoType.R762_SP, 50);
	}

	/** [parity d2] decider of the right gun: refires on the secondary key. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_MINIGUN = (stack, ctx) -> {
		int index = ctx.configIndex;
		ItemGunBaseNT.GunState lastState = ItemGunBaseNT.getState(stack, index);
		GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
		GunStateDecider.deciderStandardClearJam(stack, lastState, index);
		GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
		GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index, () -> { return ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0; });
	};

	public static ItemGunBaseNT createMas36(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(5_000).draw(20).inspect(31).reloadSequential(true).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::southStarDamage).delay(25).dry(25).spread(0.0F).reload(43).jam(43).sound(HbmSoundEvents.GUN_HEAVY_RIFLE_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 7).addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
						.offset(1, -0.0625 * 1.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_CARBINE))
				.setupStandardConfiguration()
				.anim(LAMBDA_MAS36_ANIMS).orchestra(OrchestrasC3.ORCHESTRA_MAS36)
				).setDefaultAmmo(StandardAmmoType.R762_AP, 14);
	}

	/**
	 * LAMBDA_TINY_EXPLODE (r762_he onImpact): ignored for the shooter during the first 3 ticks,
	 * otherwise Lego.tinyExplode(bullet, mop, 1.5F) and the bullet dies.
	 */
	public static boolean tinyExplodeImpact(LegacyBulletEntity bullet, HitResult mop) {
		if(mop instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.ownerEntity()) return false;
		Direction side = mop instanceof BlockHitResult bhr ? bhr.getDirection() : null;
		XFactory50.tinyExplode(bullet, mop.getLocation(), side, bullet.baseDamage(), 1.5F, 1F);
		return true;
	}

	/** XFactoryEnergy.LAMBDA_IR_HIT */
	public static void irHit(LegacyBulletBeamEntity beam, HitResult mop) {
		BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT.accept(beam, mop);

		if(mop instanceof EntityHitResult ehr) {
			if(ehr.getEntity() instanceof LivingEntity living) {
				// HbmLivingProps.fire: if(props.fire < 100) props.fire = 100
				com.atomiclegacy.radiation.HbmLivingHazards.get(living).extendFire(100);
			}
		}

		if(mop instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
			Level world = beam.level();
			BlockPos pos = bhr.getBlockPos();
			BlockState b = world.getBlockState(pos);
			Direction dir = bhr.getDirection();
			if(b.isFlammable(world, pos, dir.getOpposite())) {
				BlockPos adj = pos.relative(dir);
				if(world.getBlockState(adj).isAir()) {
					world.setBlockAndUpdate(adj, Blocks.FIRE.defaultBlockState());
					return;
				}
			}

			// EntityFireLingering().setArea(2, 1).setDuration(100).setType(TYPE_DIESEL)
			LegacyProjectileUtil.spawnLingeringFire(beam, mop.getLocation(), LegacyLingeringFireEntity.FireType.DIESEL, 2.0F, 1.0F, 100, false);
		}
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
		Lego.handleStandardSmoke(ctx.entity, stack, 1500, 0.075D, 1.1D, 0);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_CARBINE = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MINIGUN = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LACUNAE = (stack, ctx) -> { };

	/** Rounds in the magazine of receiver 0 (MainRegistry.proxy.me().inventory is not read by full-reload magazines). */
	private static int ammo(ItemStack stack) {
		return ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CARBINE_ANIMS = (stack, type) -> {
		int ammo = ammo(stack);
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_UP))
				.addBus(ammo <= 1 ? "NULL" : "REL", new BusAnimationSequence().addPos(0, 0, 0.25, 50).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP));
		case RELOAD: return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence().addPos(0, -4, 0, 250, IType.SIN_UP).addPos(0, -4, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1000))
				.addBus("BULLET", new BusAnimationSequence().addPos(ammo == 0 ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
		case RELOAD_END: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-25, 0, 0, 0).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP))
				.addBus("REL", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, 0.25, 150).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-25, 0, 0, 0).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, -0.25, 100, IType.SIN_UP).addPos(0, 0, -0.25, 1250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP))
				.addBus("REL", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, 0.25, 150).addPos(0, 0.125, 1, 100, IType.SIN_UP).addPos(0, 0.125, 1, 1250).addPos(0, 0.125, 0.25, 100, IType.SIN_DOWN).addPos(0, 0.125, 1, 100, IType.SIN_UP));
		case INSPECT: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -0.75, 150, IType.SIN_DOWN).addPos(0, 0, -0.75, 1000).addPos(0, 0, 0, 100, IType.SIN_UP))
				.addBus(ammo == 0 ? "NULL" : "REL", new BusAnimationSequence().addPos(0, 0.125, 1.25, 0).addPos(0, 0.125, 1.25, 500).addPos(0, 0.125, 0.5, 150, IType.SIN_DOWN).addPos(0, 0.125, 0.5, 1000).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MINIGUN_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 0).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 100).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 60, 50).addPos(0, 0, 720, 1000, IType.SIN_DOWN));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 60, 50).addPos(0, 0, 720, 1000, IType.SIN_DOWN));
		case RELOAD: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-15, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 60, 50).addPos(0, 0, 720, 1000, IType.SIN_DOWN));
		case INSPECT: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(3, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -720, 1000, IType.SIN_DOWN));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MAS36_ANIMS = (stack, type) -> {
		int mag = ammo(stack);
		double turn = -90;
		double pullAmount = ItemGunBaseNT.getIsAiming(stack) ? -1F : -1.5D;
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("STOCK", new BusAnimationSequence().setPos(-158, 0, 0).hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().setPos(45, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500).addPos(1, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
				.addBus("LIFT", new BusAnimationSequence().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("BULLET", mag <= 1 ? new BusAnimationSequence().setPos(-100, 0, 0) : new BusAnimationSequence().hold(850).addPos(0, 0.1875, 1.5, 200, IType.LINEAR));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
				.addBus("LIFT", new BusAnimationSequence().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("BULLET", new BusAnimationSequence().setPos(-100, 0, 0));
		case RELOAD: return new BusAnimation()
				.addBus("BOLT_TURN", new BusAnimationSequence().addPos(0, 0, turn, 150).holdUntil(2000).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(100).addPos(0, 0, -1.5D, 250, IType.SIN_UP).holdUntil(1800).addPos(0, 0, 0, 200, IType.LINEAR))
				.addBus("BULLET", new BusAnimationSequence().setPos(-100, 0, 0).holdUntil(1200).setPos(0, 0, 0).hold(600).addPos(0, 0.1875, 1.5, 200, IType.LINEAR))
				.addBus("LIFT", new BusAnimationSequence().hold(200).addPos(30, 0, 0, 500, IType.SIN_FULL).holdUntil(1200).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SHOW_CLIP", new BusAnimationSequence().setPos(1, 1, 1))
				.addBus("CLIP", new BusAnimationSequence().setPos(2, -3, 0).hold(250).addPos(0.5, 1, 0, 500, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL).hold(400).addPos(-0.5, 0.5, 0, 150).addPos(-3, -3, 0, 250, IType.SIN_UP))
				.addBus("BULLETS", new BusAnimationSequence().setPos(2, -3, 0).hold(250).addPos(0.5, 1, 0, 500, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL).hold(150).addPos(0, -1.5, 0, 250, IType.SIN_DOWN));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().hold(250).addPos(-15, 0, 0, 500, IType.SIN_FULL).holdUntil(1650).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(250).addPos(0, 0, turn, 150).holdUntil(1250).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR));
		case INSPECT: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().hold(350).addPos(-3, 0, 0, 150, IType.SIN_DOWN).holdUntil(1050).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("BOLT_TURN", new BusAnimationSequence().addPos(0, 0, turn, 150).holdUntil(1050).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(100).addPos(0, 0, -1D, 250, IType.SIN_UP).hold(500).addPos(0, 0, 0, 200, IType.LINEAR))
				.addBus("BULLET", mag == 0 ? new BusAnimationSequence().setPos(-100, 0, 0) : new BusAnimationSequence().setPos(0, 0.1875, 1.5).hold(100).addPos(0, 0.125, 0.5, 250, IType.SIN_UP).hold(500).addPos(0, 0.1875, 1.5, 200, IType.LINEAR));
		}

		return null;
	};
}
