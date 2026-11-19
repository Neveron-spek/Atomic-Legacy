package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 XFactory357: gun_light_revolver, gun_light_revolver_atlas, gun_light_revolver_dani and the
 * six .357 Magnum rounds. Damage multiplier, threshold negation, armor piercing, headshot and
 * penetration live in the port's StandardAmmoType M357 entries; the rest of each BulletConfig is set
 * here. The revolvers keep their casings in the drum (no casing effect), setCasing only fed the casing
 * bag which the port does not have.
 */
public class XFactory357 {

	static {
		// m357_bp: setBlackPowder(true); every other value is the BulletConfig default
		StandardAmmoType.M357_BP.setBlackPowder(true);
	}

	public static final BulletConfig m357_bp = new BulletConfig(StandardAmmoType.M357_BP);
	public static final BulletConfig m357_sp = new BulletConfig(StandardAmmoType.M357_SP);
	public static final BulletConfig m357_fmj = new BulletConfig(StandardAmmoType.M357_FMJ);
	public static final BulletConfig m357_jhp = new BulletConfig(StandardAmmoType.M357_JHP);
	public static final BulletConfig m357_ap = new BulletConfig(StandardAmmoType.M357_AP);
	public static final BulletConfig m357_express = new BulletConfig(StandardAmmoType.M357_EXPRESS).setWear(1.5F);

	public static ItemGunBaseNT createLightRevolver(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::lightRevolverDamage).delay(11).reload(55).jam(45).sound(HbmSoundEvents.GUN_PISTOL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
						.offset(0.75, -0.0625, -0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ATLAS))
				.setupStandardConfiguration()
				.anim(LAMBDA_ATLAS_ANIMS).orchestra(OrchestrasC1.ORCHESTRA_ATLAS)
				).setDefaultAmmo(StandardAmmoType.M357_SP, 12);
	}

	public static ItemGunBaseNT createLightRevolverAtlas(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE, new GunConfig()
				.dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::atlasDamage).delay(11).reload(55).jam(45).sound(HbmSoundEvents.GUN_PISTOL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
						.offset(0.75, -0.0625, -0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ATLAS))
				.setupStandardConfiguration()
				.anim(LAMBDA_ATLAS_ANIMS).orchestra(OrchestrasC1.ORCHESTRA_ATLAS)
				).setDefaultAmmo(StandardAmmoType.M357_JHP, 12);
	}

	public static ItemGunBaseNT createLightRevolverDani(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY,
				new GunConfig().dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::daniCelestialDamage).spreadHipfire(0F).delay(11).reload(55).jam(45).sound(HbmSoundEvents.GUN_PISTOL_FIRE, 1.0F, 1.1F)
						.mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
						.offset(0.75, -0.0625, 0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_DANI))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_DANI_ANIMS).orchestra(OrchestrasC1.ORCHESTRA_DANI),
				new GunConfig().dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::daniLunarDamage).spreadHipfire(0F).delay(11).reload(55).jam(45).sound(HbmSoundEvents.GUN_PISTOL_FIRE, 1.0F, 0.9F)
						.mag(new MagazineFullReload(1, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
						.offset(0.75, -0.0625, -0.3125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_DANI))
				.ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_DANI_ANIMS).orchestra(OrchestrasC1.ORCHESTRA_DANI)
				).setDefaultAmmo(StandardAmmoType.M357_EXPRESS, 24);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ATLAS = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_DANI = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.75));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_ATLAS_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
				.addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300).addPos(0, 0, 0, 200))
				.addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, 1, 200));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 200).addPos(0, 0, 0, 200))
				.addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, 1, 200));
		case RELOAD: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(0, 0, 90, 300).addPos(0, 0, 90, 2000).addPos(0, 0, 0, 150))
				.addBus("FRONT", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 2000).addPos(0, 0, 0, 75))
				.addBus("RELOAD_ROT", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(60, 0, 0, 500).addPos(60, 0, 0, 500).addPos(0, -90, -90, 0).addPos(0, -90, -90, 600).addPos(0, 0, 0, 300).addPos(0, 0, 0, 100).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
				.addBus("RELOAD_MOVE", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(0, -15, 0, 1000).addPos(0, 0, 0, 450))
				.addBus("DRUM_PUSH", new BusAnimationSequence().addPos(0, 0, 0, 1600).addPos(0, 0, -5, 0).addPos(0, 0, 0, 300));
		case INSPECT: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
				.addBus("FRONT", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
				.addBus("RELOAD_ROT", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
				.addBus("RELOAD_MOVE", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
		case JAMMED: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
				.addBus("FRONT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
				.addBus("RELOAD_ROT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
				.addBus("RELOAD_MOVE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DANI_ANIMS = (stack, type) -> {
		switch(type) {
			case EQUIP: return new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(360 * 3, 0, 0, 1000, IType.SIN_DOWN));
		}
		return LAMBDA_ATLAS_ANIMS.apply(stack, type);
	};
}
