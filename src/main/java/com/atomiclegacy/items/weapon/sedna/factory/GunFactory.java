package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

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
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * [parity d2] 1.7.10 GunFactory: the DEBUG revolver gun_debug with its two bullet configs
 * ammo_debug / ammo_debug_shot (item ammo_debug). WeaponQuality.DEBUG: no creative tab, no recipe.
 * The projectile data of the two rounds lives in StandardAmmoType.DEBUG / DEBUG_SHOT (BulletConfig
 * defaults plus the setters below); the remaining guns of the original GunFactory.init() are created
 * by the XFactory classes.
 */
public class GunFactory {

	/** Casing effect id (ordinal of TurretVisualClientEffects.CasingConfig.DEBUG44): CASING44.clone() "DEBUG0" / "DEBUG1". */
	public static final int CASING_DEBUG = 42;

	/** BulletConfig.setItem(ModItems.ammo_debug): matches the plain ammo_debug item. */
	public static class DebugBulletConfig extends BulletConfig {

		public DebugBulletConfig(StandardAmmoType ammo) {
			super(ammo);
		}

		@Override
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && stack.is(HbmItems.AMMO_DEBUG.get());
		}

		@Override
		public ItemStack toStack() {
			return new ItemStack(HbmItems.AMMO_DEBUG.get());
		}
	}

	static {
		// ammo_debug = new BulletConfig().setItem(ModItems.ammo_debug).setSpread(0.01F).setRicochetAngle(45)
		StandardAmmoType.DEBUG.setSpread(0.01F).setRicochetAngle(45);
		// ammo_debug_shot = new BulletConfig().setItem(ModItems.ammo_debug).setSpread(0.05F).setProjectiles(6).setRicochetAngle(45)
		StandardAmmoType.DEBUG_SHOT.setSpread(0.05F).setProjectiles(6).setRicochetAngle(45);
	}

	public static final BulletConfig ammo_debug = new DebugBulletConfig(StandardAmmoType.DEBUG).setCasingEffect(CASING_DEBUG);
	public static final BulletConfig ammo_debug_shot = new DebugBulletConfig(StandardAmmoType.DEBUG_SHOT).setCasingEffect(CASING_DEBUG);

	@SuppressWarnings("deprecation") // Receiver.dmg(float): the original passes plain floats here, no ServerConfig value
	public static ItemGunBaseNT createDebug(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.DEBUG, new GunConfig()
				.dura(600F).draw(15).inspect(23).crosshair(Crosshair.L_CLASSIC).smoke(Lego.LAMBDA_STANDARD_SMOKE).orchestra(Orchestras.DEBUG_ORCHESTRA)
				.rec(new Receiver(0)
						.dmg(10F).delay(14).reload(46).jam(23).sound(HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 12).addConfigs(ammo_debug))
						.offset(0.75, -0.0625, -0.3125D)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE),
						new Receiver(1)
						.dmg(5F).delay(14).reload(46).jam(23).sound(HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(1, 12).addConfigs(ammo_debug_shot))
						.offset(0.75, -0.0625, -0.3125D)
						.canFire(Lego.LAMBDA_SECOND_CAN_FIRE).fire(Lego.LAMBDA_SECOND_FIRE))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).ps((stack, ctx) -> { Lego.clickReceiver(stack, ctx, 1); })
				.pr(Lego.LAMBDA_STANDARD_RELOAD).pt(Lego.LAMBDA_TOGGLE_AIM)
				.decider(LAMBDA_DEBUG_DECIDER)
				.anim(Lego.LAMBDA_DEBUG_ANIMS)
				);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DEBUG_DECIDER = (stack, ctx) -> {
		int index = ctx.configIndex;
		GunState lastState = ItemGunBaseNT.getState(stack, index);
		GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
		GunStateDecider.deciderStandardClearJam(stack, lastState, index);
		GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
		GunStateDecider.deciderStandardReload(stack, ctx, lastState, 1, index);
		GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index, () -> { return ItemGunBaseNT.getPrimary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0; });
		GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 1, index, () -> { return ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0; });
	};
}
