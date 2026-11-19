package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBoxcarEntity;
import com.atomiclegacy.entity.LegacyTorpedoEntity;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 XFactory44: gun_henry, gun_henry_lincoln, gun_heavy_revolver, gun_heavy_revolver_lilmac,
 * gun_heavy_revolver_protege and gun_hangman with the .44 Magnum rounds. Values are verbatim; the
 * projectile data of each round lives in the port's StandardAmmoType constants, the gun-side data
 * (wear, casing) in the BulletConfig wrappers below. The two ammo_secret M44_EQUESTRIAN configs use
 * the StandardAmmoType constants M44_EQUESTRIAN_PIP / _MN7 and match the ammo_secret variant.
 */
public class XFactory44 {

	public static final ResourceLocation scope_lilmac = AtomicLegacy.id("textures/misc/scope_44.png");

	/** Casing effect ids (ordinals of TurretVisualClientEffects.CasingConfig): casing44 brass, COLOR_CASE_44 (m44ap), COLOR_CASE_EQUESTRIAN. */
	public static final int CASING_M44 = 13;
	public static final int CASING_M44_STEEL = 14;
	public static final int CASING_M44_EQUESTRIAN = 15;

	/** BulletConfig.setItem(EnumAmmoSecret.M44_EQUESTRIAN): matches ammo_secret:m44_equestrian. */
	public static class SecretBulletConfig extends BulletConfig {
		public final String secretVariant;

		public SecretBulletConfig(StandardAmmoType ammo, String secretVariant) {
			super(ammo);
			this.secretVariant = secretVariant;
		}

		@Override
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && stack.is(HbmItems.AMMO_SECRET.get())
					&& ((LegacyVariantItem) HbmItems.AMMO_SECRET.get()).variant(stack).id().equals(this.secretVariant);
		}

		@Override
		public ItemStack toStack() {
			return LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), this.secretVariant);
		}
	}

	/** LAMBDA_BOXCAR: a boxcar 50 blocks above the hit, the train horn another 50 blocks above it. */
	public static BulletImpact LAMBDA_BOXCAR = (bullet, mop) -> {
		Vec3 hit = mop.getLocation();
		LegacyBoxcarEntity pippo = new LegacyBoxcarEntity(bullet.level(), new Vec3(hit.x, hit.y + 50, hit.z));
		bullet.level().addFreshEntity(pippo);
		bullet.level().playSound(null, pippo.getX(), pippo.getY() + 50, pippo.getZ(), HbmSoundEvents.ALARM_TRAIN_HORN.get(), SoundSource.RECORDS, 100F, 1F);
		return true;
	};

	/** LAMBDA_TORPEDO: a torpedo 50 blocks above the hit. */
	public static BulletImpact LAMBDA_TORPEDO = (bullet, mop) -> {
		Vec3 hit = mop.getLocation();
		LegacyTorpedoEntity murky = new LegacyTorpedoEntity(bullet.level(), new Vec3(hit.x, hit.y + 50, hit.z));
		bullet.level().addFreshEntity(murky);
		return true;
	};

	static {
		// BulletConfig fields carried by the StandardAmmoType constants
		StandardAmmoType.M44_BP.setBlackPowder(true);
		StandardAmmoType.M44_EQUESTRIAN_PIP.setOnImpact(LAMBDA_BOXCAR);
		StandardAmmoType.M44_EQUESTRIAN_MN7.setOnImpact(LAMBDA_TORPEDO);
	}

	public static final BulletConfig m44_bp = new BulletConfig(StandardAmmoType.M44_BP).setCasingEffect(CASING_M44);
	public static final BulletConfig m44_sp = new BulletConfig(StandardAmmoType.M44_SP).setCasingEffect(CASING_M44);
	public static final BulletConfig m44_fmj = new BulletConfig(StandardAmmoType.M44_FMJ).setCasingEffect(CASING_M44);
	public static final BulletConfig m44_jhp = new BulletConfig(StandardAmmoType.M44_JHP).setCasingEffect(CASING_M44);
	public static final BulletConfig m44_ap = new BulletConfig(StandardAmmoType.M44_AP).setCasingEffect(CASING_M44_STEEL);
	public static final BulletConfig m44_express = new BulletConfig(StandardAmmoType.M44_EXPRESS).setWear(1.5F).setCasingEffect(CASING_M44);
	public static final BulletConfig m44_equestrian_pip = new SecretBulletConfig(StandardAmmoType.M44_EQUESTRIAN_PIP, "m44_equestrian").setCasingEffect(CASING_M44_EQUESTRIAN);
	public static final BulletConfig m44_equestrian_mn7 = new SecretBulletConfig(StandardAmmoType.M44_EQUESTRIAN_MN7, "m44_equestrian").setCasingEffect(CASING_M44_EQUESTRIAN);

	public static ItemGunBaseNT createHenry(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::henryDamage).delay(20).reload(25, 11, 14, 8).jam(45).sound(HbmSoundEvents.GUN_RIFLE_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 14).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_HENRY))
				.setupStandardConfiguration()
				.anim(LAMBDA_HENRY_ANIMS).orchestra(OrchestrasC2.ORCHESTRA_HENRY)
				);
		gun.setDefaultAmmo(StandardAmmoType.M44_SP, 14);
		return gun;
	}

	public static ItemGunBaseNT createHenryLincoln(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.B_SIDE, new GunConfig()
				.dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::lincolnDamage).spreadHipfire(0F).delay(20).reload(25, 11, 14, 8).jam(45).sound(HbmSoundEvents.GUN_RIFLE_FIRE, 1.0F, 1.25F)
						.mag(new MagazineSingleReload(0, 14).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_HENRY))
				.setupStandardConfiguration()
				.anim(LAMBDA_HENRY_ANIMS).orchestra(OrchestrasC2.ORCHESTRA_HENRY)
				);
		gun.setDefaultAmmo(StandardAmmoType.M44_JHP, 14);
		return gun;
	}

	public static ItemGunBaseNT createHeavyRevolver(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(600).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::heavyRevolverDamage).delay(14).reload(46).jam(23).sound(HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 6).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
						.offset(0.75, -0.0625, -0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_NOPIP))
				.setupStandardConfiguration()
				.anim(LAMBDA_NOPIP_ANIMS).orchestra(OrchestrasC2.ORCHESTRA_NOPIP)
				);
		gun.setDefaultAmmo(StandardAmmoType.M44_SP, 12).setNameMutator(LAMBDA_NAME_NOPIP);
		return gun;
	}

	public static ItemGunBaseNT createLilmac(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC).scopeTexture(scope_lilmac).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::lilmacDamage).delay(14).reload(46).jam(23).sound(HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 6).addConfigs(m44_equestrian_pip, m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
						.offset(0.75, -0.0625, -0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_NOPIP))
				.setupStandardConfiguration()
				.anim(LAMBDA_LILMAC_ANIMS).orchestra(OrchestrasC2.ORCHESTRA_NOPIP)
				);
		gun.setDefaultAmmo(StandardAmmoType.M44_JHP, 12);
		return gun;
	}

	public static ItemGunBaseNT createProtege(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::protegeDamage).delay(14).reload(46).jam(23).sound(HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE, 1.0F, 0.8F)
						.mag(new MagazineFullReload(0, 6).addConfigs(m44_equestrian_mn7, m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
						.offset(0.75, -0.0625, -0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_NOPIP))
				.setupStandardConfiguration()
				.anim(LAMBDA_LILMAC_ANIMS).orchestra(OrchestrasC2.ORCHESTRA_NOPIP)
				);
		gun.setDefaultAmmo(StandardAmmoType.M44_JHP, 12);
		return gun;
	}

	public static ItemGunBaseNT createHangman(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(600).draw(10).inspect(31).inspectCancel(false).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::hangmanDamage).delay(10).reload(46).jam(23).sound(HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 8).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_HANGMAN))
				.setupStandardConfiguration().ps(SMACK_A_FUCKER)
				.anim(LAMBDA_HANGMAN_ANIMS).orchestra(OrchestrasC2.ORCHESTRA_HANGMAN)
				);
		gun.setDefaultAmmo(StandardAmmoType.M44_FMJ, 16);
		return gun;
	}

	public static Function<ItemStack, String> LAMBDA_NAME_NOPIP = (stack) -> {
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE)) return stack.getItem().getDescriptionId() + "_scoped";
		return null;
	};

	public static BiConsumer<ItemStack, LambdaContext> SMACK_A_FUCKER = (stack, ctx) -> {
		if(ItemGunBaseNT.getState(stack, ctx.configIndex) == GunState.IDLE || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimation.CYCLE) {
			ItemGunBaseNT.setIsAiming(stack, false);
			ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.DRAWING);
			ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
			ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.INSPECT, ctx.configIndex);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_HENRY = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_NOPIP = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_HANGMAN = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_HENRY_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-90, 0, 0, 0).addPos(0, 0, -3, 350, IType.SIN_DOWN))
				.addBus("SIGHT", new BusAnimationSequence().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, -3, 250, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
				.addBus("SIGHT", new BusAnimationSequence().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
		case RELOAD: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-60, 0, 0, 400, IType.SIN_FULL))
				.addBus("TWIST", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -90, 200, IType.SIN_FULL))
				.addBus("BULLET", new BusAnimationSequence().addPos(0, 0, 0, 700).addPos(3, 0, -6, 0).addPos(0, 0, 1, 300, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case RELOAD_CYCLE: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-60, 0, 0, 0))
				.addBus("TWIST", new BusAnimationSequence().addPos(0, 0, -90, 0))
				.addBus("BULLET", new BusAnimationSequence().addPos(3, 0, -6, 0).addPos(0, 0, 1, 300, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case RELOAD_END:
			boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmountBeforeReload(stack) <= 0;
			return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(-60, 0, 0, 300).addPos(0, 0, 0, 400, IType.SIN_FULL))
				.addBus("TWIST", new BusAnimationSequence().addPos(0, 0, -90, 0).addPos(0, 0, 0, 200, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 700).addPos(empty ? -90 : 0, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 700).addPos(0, 0, empty ? 45 : 0, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(-60, 0, 0, 300).addPos(0, 0, 0, 400, IType.SIN_FULL))
				.addBus("TWIST", new BusAnimationSequence().addPos(0, 0, -90, 0).addPos(0, 0, 0, 200, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 700).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200).addPos(0, 0, 0, 500).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 700).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 500).addPos(0, 0, 45, 200, IType.SIN_FULL).addPos(0, 0, 45, 600).addPos(0, 0, 0, 200, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("YEET", new BusAnimationSequence().addPos(0, 2, 0, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("ROLL", new BusAnimationSequence().addPos(0, 0, 360, 400));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_NOPIP_ANIMS = (stack, type) -> {
		switch(type) {
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
				.addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
				.addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300 + 100).addPos(0, 0, 0, 200))
				.addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
		case EQUIP: return new BusAnimation().addBus("ROTATE", new BusAnimationSequence().addPos(90, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case RELOAD: return new BusAnimation()
					.addBus("RELAOD_TILT", new BusAnimationSequence().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
					.addBus("RELOAD_CYLINDER", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
					.addBus("RELOAD_LIFT", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250).addPos(-45, 0, 0, 350).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050).addPos(0, 0, 0, 100))
					.addBus("RELOAD_JOLT", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50).addPos(0, 0, 0, 100))
					.addBus("RELOAD_BULLETS", new BusAnimationSequence().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300).addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
					.addBus("RELOAD_BULLETS_CON", new BusAnimationSequence().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950).addPos(0, 0, 0, 1 ) );
		case INSPECT:
		case JAMMED: return new BusAnimation()
					.addBus("RELAOD_TILT", new BusAnimationSequence().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
					.addBus("RELOAD_CYLINDER", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LILMAC_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation().addBus("SPIN", new BusAnimationSequence().addPos(-360, 0, 0, 350));
		}

		return LAMBDA_NOPIP_ANIMS.apply(stack, type);
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_HANGMAN_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250));
		case RELOAD: return new BusAnimation()
				.addBus("LID", new BusAnimationSequence().addPos(0, 0, -90, 250).addPos(0, 0, -90, 1500).addPos(0, 0, 0, 250))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP).addPos(0, -10, 0, 500).addPos(0, 0, 0, 350, IType.SIN_FULL))
				.addBus("BULLETS", new BusAnimationSequence().addPos(1, 1, 1, 0).addPos(0, 0, 0, 500))
				.addBus("EQUIP", new BusAnimationSequence().addPos(-15, 0, 0, 500, IType.SIN_FULL).addPos(-15, 0, 0, 850).addPos(-25, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL))
				.addBus("ROLL", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 25, 250, IType.SIN_FULL).addPos(0, 0, 25, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("TURN", new BusAnimationSequence().addPos(0, 170, 0, 500, IType.SIN_UP).addPos(0, 170, 0, 550).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("ROLL", new BusAnimationSequence().addPos(0, 0, 110, 500, IType.SIN_FULL).addPos(0, 0, 110, 550).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SMACK", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 1, 150, IType.SIN_DOWN).addPos(0, 0, -3, 150, IType.SIN_UP).addPos(0, 0, 0, 350, IType.SIN_FULL));
		case JAMMED: return new BusAnimation()
				.addBus("LID", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -90, 250).addPos(0, 0, -90, 300).addPos(0, 0, 0, 250))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250).addPos(0, -3, 0, 150, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 1000).addPos(-10, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL))
				.addBus("ROLL", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 25, 250, IType.SIN_FULL).addPos(0, 0, 25, 300).addPos(0, 0, 0, 250, IType.SIN_FULL));
		}

		return null;
	};
}
