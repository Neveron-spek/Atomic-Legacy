package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmClientConfig;
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
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.AnimationLoader;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 XFactory22lr: gun_am180, gun_star_f, gun_star_f_akimbo and the four .22 LR
 * rounds. Projectile values come from the port's StandardAmmoType P22 entries.
 */
public class XFactory22lr {

	public static final BulletConfig p22_sp = new BulletConfig(StandardAmmoType.P22_SP).setCasingEffect(OrchestrasB.CASING_P22);
	public static final BulletConfig p22_fmj = new BulletConfig(StandardAmmoType.P22_FMJ).setCasingEffect(OrchestrasB.CASING_P22);
	public static final BulletConfig p22_jhp = new BulletConfig(StandardAmmoType.P22_JHP).setCasingEffect(OrchestrasB.CASING_P22);
	public static final BulletConfig p22_ap = new BulletConfig(StandardAmmoType.P22_AP).setCasingEffect(OrchestrasB.CASING_P22_STEEL);

	public static ItemGunBaseNT createAm180(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(177 * 25).draw(15).inspect(38).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::am180Damage).delay(1).dry(10).auto(true).spread(0.01F).reload(66).jam(30).sound(HbmSoundEvents.GUN_GREASEGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 177).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
						.offset(1, -0.0625 * 1.5, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_AM180))
				.setupStandardConfiguration()
				.anim(LAMBDA_AM180_ANIMS).orchestra(OrchestrasB.ORCHESTRA_AM180)
				).setDefaultAmmo(StandardAmmoType.P22_SP, 35).setNameMutator(LAMBDA_NAME_SILENCED);
	}

	public static ItemGunBaseNT createStarF(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::starfDamage).delay(5).dry(17).spread(0.01F).reload(40).jam(32).sound(HbmSoundEvents.GUN_STARF_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 15).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
						.offset(1, -0.0625 * 1.5, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_STAR_F))
				.setupStandardConfiguration()
				.anim(LAMBDA_STAR_F_ANIMS).orchestra(OrchestrasB.ORCHESTRA_STAR_F)
				).setDefaultAmmo(StandardAmmoType.P22_SP, 15).setNameMutator(LAMBDA_NAME_SILENCED);
	}

	public static ItemGunBaseNT createStarFAkimbo(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE,
				new GunConfig().dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::starfAkimboLeftDamage).delay(5).dry(17).spread(0.01F).reload(40).jam(32).sound(HbmSoundEvents.GUN_STARF_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 15).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
						.offset(1, -0.0625 * 1.5, 0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_STAR_F))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_STAR_F_ANIMS).orchestra(OrchestrasB.ORCHESTRA_STAR_F_AKIMBO),
				new GunConfig().dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::starfAkimboRightDamage).delay(5).dry(17).spread(0.01F).reload(40).jam(32).sound(HbmSoundEvents.GUN_STARF_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(1, 15).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
						.offset(1, -0.0625 * 1.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_STAR_F))
				.ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_STAR_F_ANIMS).orchestra(OrchestrasB.ORCHESTRA_STAR_F_AKIMBO)
				).setDefaultAmmo(StandardAmmoType.P22_SP, 30);
	}

	public static Function<ItemStack, String> LAMBDA_NAME_SILENCED = (stack) -> {
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER)) return stack.getItem().getDescriptionId() + "_silenced";
		return null;
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
		Lego.handleStandardSmoke(ctx.entity, stack, 3000, 0.05D, 1.1D, ctx.configIndex);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AM180 = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_STAR_F = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(2.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	/**
	 * ResourceManager.am180_anim (models/weapons/animations/am180.json). Loaded on first use
	 * because AnimationLoader reads through the client resource manager; the animation lambda
	 * only runs on the client.
	 */
	private static final class Am180Anim {
		static final HashMap<String, BusAnimation> am180_anim = AnimationLoader.load(AtomicLegacy.id("models/weapons/animations/am180.json"));
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_AM180_ANIMS = (stack, type) -> {
		if(HbmClientConfig.gunAnimsLegacy()) {
			switch(type) {
			case EQUIP: return new BusAnimation()
					.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
			case CYCLE: return new BusAnimation()
					.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.25, 15, IType.SIN_DOWN).addPos(0, 0, 0, 35, IType.SIN_FULL));
			case CYCLE_DRY: return new BusAnimation()
					.addBus("BOLT", new BusAnimationSequence().addPos(0, 0, 0, 550).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_UP))
					.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(0, 0, 15, 250, IType.SIN_FULL).addPos(0, 0, 15, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
			case RELOAD:
				return new BusAnimation()
					.addBus("MAGTURN", new BusAnimationSequence().addPos(15, 0, 0, 250, IType.SIN_FULL).addPos(15, 0, 0, 250).addPos(15, 0, 70, 300, IType.SIN_FULL).addPos(15, 0, 0, 0).addPos(15, 0, 0, 750).addPos(0, 0, 0, 250, IType.SIN_FULL))
					.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(2, 0, -4, 250, IType.SIN_FULL).addPos(-10, 2, -4, 300, IType.SIN_UP).addPos(3, -6, -4, 0).addPos(2, 0, -4, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
					.addBus("BOLT", new BusAnimationSequence().addPos(0, 0, 0, 2250).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_UP))
					.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 2000).addPos(0, 0, 15, 250, IType.SIN_FULL).addPos(0, 0, 15, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
			case JAMMED: return new BusAnimation()
					.addBus("BOLT", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_UP))
					.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 45, 250, IType.SIN_FULL).addPos(0, 0, 45, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
			case INSPECT: return new BusAnimation()
					.addBus("MAGTURN", new BusAnimationSequence().addPos(15, 0, 0, 250, IType.SIN_FULL).addPos(15, 0, 0, 1400).addPos(0, 0, 0, 250, IType.SIN_FULL))
					.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(4, -1, -4, 200, IType.SIN_FULL).addPos(4, -1.5, -4, 50).addPos(4, 0, -4, 100).addPos(4, 6, -4, 250, IType.SIN_DOWN).addPos(4, 0, -4, 150, IType.SIN_UP).addPos(4, -1, -4, 100, IType.SIN_DOWN).addPos(4, -1, -4, 250).addPos(0, 0, 0, 250, IType.SIN_FULL))
					.addBus("MAGSPIN", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-400, 0, 0, 500, IType.SIN_FULL).addPos(-400, 0, 0, 250).addPos(-360, 0, 0, 250));
			}
		} else {
			switch(type) {
			case EQUIP: return new BusAnimation()
					.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
			case CYCLE: return Am180Anim.am180_anim.get("Fire");
			case CYCLE_DRY: return Am180Anim.am180_anim.get("FireDry");
			case RELOAD: return Am180Anim.am180_anim.get("Reload");
			case JAMMED: return Am180Anim.am180_anim.get("Jammed");
			case INSPECT: return Am180Anim.am180_anim.get("Inspect");
			}
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_STAR_F_ANIMS = (stack, type) -> {
		// MagazineFullReload#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
		int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.5, 15, IType.SIN_DOWN).addPos(0, 0, 0, 35, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 50, IType.SIN_DOWN))
				.addBus("BULLET", ammo <= 1 ? new BusAnimationSequence().setPos(100, 0, 0) : new BusAnimationSequence().addPos(0, 0, 0, 90).addPos(0, 0.5, 2.25, 50));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("HAMMER", new BusAnimationSequence().addPos(1, 0, 0, 50, IType.SIN_UP).hold(450).addPos(0, 0, 0, 50, IType.SIN_DOWN))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_FULL).hold(100).addPos(0, 0, 0, 75, IType.SIN_UP))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-3, 0, 0, 175, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BULLET", new BusAnimationSequence().setPos(100, 0, 0));
		case RELOAD:
			return new BusAnimation()
					.addBus("TILT", new BusAnimationSequence().addPos(-30, 0, 0, 250, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 250, IType.SIN_FULL))
					.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_FULL).hold(1125).addPos(0, 0, 0, 100, IType.SIN_UP))
					.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, -7, -1.5, 300, IType.SIN_UP).hold(400).addPos(0, 0, 0, 300, IType.SIN_UP))
					.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(3, 0, 0, 750, IType.SIN_FULL).addPos(-3, 0, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
					.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(0, 0, 15, 300, IType.SIN_FULL).hold(900).addPos(0, 0, 0, 150, IType.SIN_FULL))
					.addBus("BULLET", new BusAnimationSequence().setPos(ammo <= 1 ? 100 : 0, 0, 0).hold(750).setPos(0, 0, 0).hold(750).addPos(0, 0.5, 2.25, 50));
		case JAMMED: return new BusAnimation()
				.addBus("TILT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-30, 0, 0, 150, IType.SIN_FULL).hold(800).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 25, 150, IType.SIN_FULL).hold(800).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(100).addPos(0, 0, 0, 100, IType.SIN_UP).hold(100).addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(100).addPos(0, 0, 0, 100, IType.SIN_UP))
				.addBus("BULLET", new BusAnimationSequence().setPos(0, 0.5, 2.25).hold(750).addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(100).addPos(0, 0.5, 2.25, 100, IType.SIN_UP).hold(100).addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(100).addPos(0, 0.5, 2.25, 100, IType.SIN_UP));
		case INSPECT: return new BusAnimation()
				.addBus("TILT", new BusAnimationSequence().addPos(-30, 0, 0, 250, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 25, 250, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(1125).addPos(0, 0, 0, 100, IType.SIN_UP))
				.addBus("BULLET", ammo <= 1 ? new BusAnimationSequence().setPos(100, 0, 0) : new BusAnimationSequence().setPos(0, 0.5, 2.25).hold(350).addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(1125).addPos(0, 0.5, 2.25, 100, IType.SIN_UP));
		}

		return null;
	};
}
