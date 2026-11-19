package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.HashMap;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.registry.HbmStats;
import com.atomiclegacy.render.anim.AnimationLoader;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 XFactory9mm: gun_greasegun, gun_lag, gun_uzi, gun_uzi_akimbo. The P9 bullet
 * configs keep the original order and casings; projectile values come from the
 * port's StandardAmmoType P9 entries.
 */
public class XFactory9mm {

	public static final BulletConfig p9_sp = new BulletConfig(StandardAmmoType.P9_SP).setCasingEffect(GunEffects.CASING_P9);
	public static final BulletConfig p9_fmj = new BulletConfig(StandardAmmoType.P9_FMJ).setCasingEffect(GunEffects.CASING_P9);
	public static final BulletConfig p9_jhp = new BulletConfig(StandardAmmoType.P9_JHP).setCasingEffect(GunEffects.CASING_P9);
	public static final BulletConfig p9_ap = new BulletConfig(StandardAmmoType.P9_AP).setCasingEffect(GunEffects.CASING_P9_STEEL);

	public static ItemGunBaseNT createGreasegun(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::greasegunDamage).delay(4).dry(40).auto(true).spread(0.015F).reload(60).jam(55).sound(HbmSoundEvents.GUN_GREASEGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_GREASEGUN))
				.setupStandardConfiguration()
				.anim(LAMBDA_GREASEGUN_ANIMS).orchestra(Orchestras.ORCHESTRA_GREASEGUN)
				);
		gun.setNameMutator(LAMBDA_NAME_GREASEGUN);
		return gun;
	}

	public static ItemGunBaseNT createLag(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(1_700).draw(7).inspect(31).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::longPistolDamage).delay(4).dry(10).spread(0.005F).reload(53).jam(44).sound(HbmSoundEvents.GUN_PISTOL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 17).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().fire(LAMBDA_FIRE_LAG).recoil(LAMBDA_RECOIL_LAG))
				.setupStandardConfiguration()
				.anim(LAMBDA_LAG_ANIMS).orchestra(Orchestras.ORCHESTRA_LAG)
				);
		gun.setDefaultAmmo(StandardAmmoType.P9_JHP, 17);
		return gun;
	}

	public static ItemGunBaseNT createUzi(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::uziDamage).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50).sound(HbmSoundEvents.GUN_UZI_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_UZI))
				.setupStandardConfiguration()
				.anim(LAMBDA_UZI_ANIMS).orchestra(Orchestras.ORCHESTRA_UZI)
				);
		gun.setDefaultAmmo(StandardAmmoType.P9_SP, 30).setNameMutator(LAMBDA_NAME_UZI);
		return gun;
	}

	public static ItemGunBaseNT createUziAkimbo(Item.Properties properties) {
		ItemGunBaseNT gun = new ItemGunBaseNT(properties, WeaponQuality.B_SIDE,
				new GunConfig().dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::uziAkimboLeftDamage).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50).sound(HbmSoundEvents.GUN_UZI_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
						.offset(1, -0.0625 * 2.5, 0.375D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_UZI))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_UZI_ANIMS).orchestra(Orchestras.ORCHESTRA_UZI_AKIMBO),
				new GunConfig().dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::uziAkimboRightDamage).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50).sound(HbmSoundEvents.GUN_UZI_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(1, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
						.offset(1, -0.0625 * 2.5, -0.375D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_UZI))
				.ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(LAMBDA_SECOND_UZI)
				.anim(LAMBDA_UZI_ANIMS).orchestra(Orchestras.ORCHESTRA_UZI_AKIMBO)
				);
		gun.setDefaultAmmo(StandardAmmoType.P9_SP, 60);
		return gun;
	}

	public static Function<ItemStack, String> LAMBDA_NAME_GREASEGUN = (stack) -> {
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_GREASEGUN_CLEAN)) return stack.getItem().getDescriptionId() + "_m3";
		return null;
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_GREASEGUN = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(2, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	public static Function<ItemStack, String> LAMBDA_NAME_UZI = (stack) -> {
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER)) return stack.getItem().getDescriptionId() + "_richter";
		return null;
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LAG = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_UZI = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(1, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_UZI = (stack, ctx) -> {
		int index = ctx.configIndex;
		GunState lastState = ItemGunBaseNT.getState(stack, index);
		GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
		GunStateDecider.deciderStandardClearJam(stack, lastState, index);
		GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
		GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index, () -> { return ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0; });
	};

	/** Firing while the inspect animation points the gun at the player's head shoots the player. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE_LAG = (stack, ctx) -> {
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		Player player = ctx.getPlayer();
		if(player != null && type == GunAnimation.INSPECT && timer > 20 && timer < 60) {
			int index = ctx.configIndex;
			Receiver primary = ctx.config.getReceivers(stack)[0];
			IMagazine<?> mag = primary.getMagazine(stack);
			BulletConfig config = (BulletConfig) mag.getType(stack, ctx.inventory);
			player.awardStat(HbmStats.NTM_BULLETS.get());
			mag.useUpAmmo(stack, ctx.inventory, 1);
			ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear, ctx.config.getDurability(stack)));
			GunEffects.playSound(player, primary.getFireSound(stack).get(), primary.getFireVolume(stack), primary.getFirePitch(stack));
			ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
			ItemGunBaseNT.setTimer(stack, index, primary.getDelayAfterFire(stack));
			// EntityDamageUtil.attackEntityFromNT(player, BulletConfig.getDamage(player, player, PHYSICAL), 1_000F, true, false, 1D, 5F, 0F)
			LegacyProjectileUtil.attackSednaPhysical(player, player, player, 1_000F, 5F, 0F);
		} else {
			Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
		Lego.handleStandardSmoke(ctx.entity, stack, 2000, 0.05D, 1.1D, ctx.configIndex);
	};

	/** LAMBDA_GREASEGUN_ANIMS; only invoked on the client by the animation packet handler. */
	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_GREASEGUN_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("STOCK", new BusAnimationSequence().addPos(0, 0, -4, 0).addPos(0, 0, -4, 200).addPos(0, 0, 0, 300, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("FLAP", new BusAnimationSequence().addPos(0, 0, 15, 100, IType.SIN_DOWN).addPos(0, 0, -5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_FULL));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -45, 250, IType.SIN_FULL).addPos(0, 0, -45, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("HANDLE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case RELOAD:
			// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
			boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) <= 0;
			return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence().addPos(0, -8, 0, 250, IType.SIN_UP).addPos(0, -8, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1750).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 1750).addPos(0, 0, -45, 250, IType.SIN_FULL).addPos(0, 0, -45, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("HANDLE", new BusAnimationSequence().addPos(0, 0, 0, 2000).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("BULLET", new BusAnimationSequence().addPos(empty ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -45, 250, IType.SIN_FULL).addPos(0, 0, -45, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("HANDLE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, -45, 150).addPos(0, 0, 45, 150).addPos(0, 0, 45, 50).addPos(0, 0, 0, 250).addPos(0, 0, 0, 500).addPos(0, 0, 45, 150).addPos(0, 0, -45, 150).addPos(0, 0, 0, 150))
				.addBus("FLAP", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(0, 0, 180, 150).addPos(0, 0, 180, 850).addPos(0, 0, 0, 150));
		}

		return null;
	};

	/** ResourceManager.lag_anim (models/weapons/animations/lag.json), loaded on first client use. */
	private static HashMap<String, BusAnimation> lag_anim;

	public static HashMap<String, BusAnimation> lagAnim() {
		if(lag_anim == null) lag_anim = AnimationLoader.load(AtomicLegacy.id("models/weapons/animations/lag.json"));
		return lag_anim;
	}

	/** LAMBDA_LAG_ANIMS; only invoked on the client by the animation packet handler. */
	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LAG_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
		case CYCLE: return lagAnim().get("Firing");
				//.addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 25, 50).addPos(0, 0, 25, 50).addPos(0, 0, 0, 100, IType.SIN_DOWN));
		case CYCLE_DRY: return lagAnim().get("Dryfire");
		case RELOAD: return lagAnim().get("Reload");
		case JAMMED: return lagAnim().get("Jam");
		case INSPECT: return lagAnim().get("Inspect")
				.addBus("ADD_TRANS", new BusAnimationSequence().addPos(-4, 0, -3, 500).addPos(-4, 0, -3, 2000).addPos(0, 0, 0, 500))
				.addBus("ADD_ROT", new BusAnimationSequence().addPos(0, -2, 5, 500).addPos(0, -2, 5, 2000).addPos(0, 0, 0, 500));
		}

		return null;
	};

	/** LAMBDA_UZI_ANIMS; only invoked on the client by the animation packet handler. */
	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_UZI_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("STOCKBACK", new BusAnimationSequence().addPos(-200, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("STOCKFRONT", new BusAnimationSequence().addPos(180, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -0.75, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_FULL));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
		case RELOAD:
			// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
			boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) <= 0;
			return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP).addPos(0, -10, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
				.addBus("LIFT", new BusAnimationSequence().addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 2000).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
				.addBus("BULLET", new BusAnimationSequence().addPos(empty ? 0 : 1, 0, 0, 0).addPos(empty ? 0 : 1, 0, 0, 500).addPos(1, 0, 0, 0));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1250).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 1000).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
		case INSPECT: return new BusAnimation()
				.addBus("YEET", new BusAnimationSequence().addPos(0, -1, 0, 100).addPos(0, 0, 0, 100, IType.SIN_UP).addPos(0, 12, 0, 350, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP).addPos(0, -1, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("SPEEN", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(-360, 0, 0, 600));
		}
		return null;
	};
}
