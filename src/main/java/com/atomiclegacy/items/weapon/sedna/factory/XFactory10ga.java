package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
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
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 1.7.10 XFactory10ga: the five 10 gauge rounds, gun_double_barrel and
 * gun_double_barrel_sacred_dragon. [parity c1] gun_autoshotgun_heretic (DEBUG).
 * Damage multiplier, threshold negation, armor piercing and penetration of the rounds are the
 * port's StandardAmmoType constants; the remaining BulletConfig fields are set below.
 */
public class XFactory10ga {

	/**
	 * LAMBDA_TINY_EXPLODE: ignored for the shooter during the first 3 ticks, otherwise
	 * Lego.tinyExplode(bullet, mop, 1.5F) and the bullet dies.
	 */
	public static final BulletImpact LAMBDA_TINY_EXPLODE = (bullet, hit) -> {
		if(hit instanceof EntityHitResult entityHit && bullet.tickCount < 3 && entityHit.getEntity() == bullet.ownerEntity()) return false;
		Direction side = hit instanceof BlockHitResult blockHit ? blockHit.getDirection() : null;
		XFactory50.tinyExplode(bullet, hit.getLocation(), side, bullet.baseDamage(), 1.5F, 1F);
		return true;
	};

	static {
		float buckshotSpread = 0.035F;
		StandardAmmoType.G10.setProjectiles(10).setSpread(buckshotSpread).setRicochetAngle(15);
		StandardAmmoType.G10_SHRAPNEL.setProjectiles(10).setSpread(buckshotSpread).setRicochetAngle(90).setRicochetCount(15);
		StandardAmmoType.G10_DU.setProjectiles(10).setSpread(buckshotSpread).setRicochetAngle(15);
		StandardAmmoType.G10_SLUG.setRicochetAngle(15);
		StandardAmmoType.G10_EXPLOSIVE.setProjectiles(10).setSpread(buckshotSpread).setOnImpact(LAMBDA_TINY_EXPLODE);
	}

	public static final BulletConfig g10 = new BulletConfig(StandardAmmoType.G10).setCasingEffect(OrchestrasC4.CASING_G10);
	public static final BulletConfig g10_shrapnel = new BulletConfig(StandardAmmoType.G10_SHRAPNEL).setCasingEffect(OrchestrasC4.CASING_G10_SHRAPNEL);
	public static final BulletConfig g10_du = new BulletConfig(StandardAmmoType.G10_DU).setCasingEffect(OrchestrasC4.CASING_G10_DU);
	public static final BulletConfig g10_slug = new BulletConfig(StandardAmmoType.G10_SLUG).setCasingEffect(OrchestrasC4.CASING_G10_SLUG);
	public static final BulletConfig g10_explosive = new BulletConfig(StandardAmmoType.G10_EXPLOSIVE).setWear(3F).setCasingEffect(OrchestrasC4.CASING_G10_EXP);

	public static ItemGunBaseNT createDoubleBarrel(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SPECIAL, new GunConfig()
				.dura(1000).draw(10).inspect(39).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::doubleBarrelDamage).rounds(2).delay(10).reload(41).reloadOnEmpty(true).sound(HbmSoundEvents.GUN_SHOTGUN_FIRE, 1.0F, 0.9F)
						.mag(new MagazineFullReload(0, 2).addConfigs(g10, g10_shrapnel, g10_du, g10_slug, g10_explosive))
						.offset(0.75, -0.0625, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_DOUBLE_BARREL))
				.setupStandardConfiguration().ps(LAMBDA_DOUBLE_SECONDARY)
				.anim(LAMBDA_DOUBLE_BARREL_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_DOUBLE_BARREL)
				).setDefaultAmmo(StandardAmmoType.G10, 6);
	}

	public static ItemGunBaseNT createDoubleBarrelSacredDragon(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE, new GunConfig()
				.dura(6000).draw(10).inspect(39).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::sacredDragonDamage).spreadAmmo(1.35F).rounds(2).delay(10).reload(41).reloadOnEmpty(true).sound(HbmSoundEvents.GUN_SHOTGUN_FIRE, 1.0F, 0.9F)
						.mag(new MagazineFullReload(0, 2).addConfigs(g10, g10_shrapnel, g10_du, g10_slug, g10_explosive))
						.offset(0.75, -0.0625, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_DOUBLE_BARREL))
				.setupStandardConfiguration().ps(LAMBDA_DOUBLE_SECONDARY)
				.anim(LAMBDA_DOUBLE_BARREL_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_DOUBLE_BARREL)
				).setDefaultAmmo(StandardAmmoType.G10_DU, 6);
	}

	/** [parity c1] gun_autoshotgun_heretic (WeaponQuality.DEBUG: no creative tab, no recipe). */
	public static ItemGunBaseNT createAutoshotgunHeretic(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.DEBUG, new GunConfig()
				.draw(20).inspect(65).reloadSequential(true).inspectCancel(false).crosshair(Crosshair.L_CIRCLE).hideCrosshair(false).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::hereticDamage).delay(3).auto(true).dryfireAfterAuto(true).reload(110).jam(19).sound(HbmSoundEvents.GUN_SHREDDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 250).addConfigs(g10, g10_shrapnel, g10_du, g10_slug, g10_explosive))
						.offset(0.75, -0.125, -0.25)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE).recoil(XFactory12ga.LAMBDA_RECOIL_SEXY))
				.setupStandardConfiguration()
				.anim(XFactory12ga.LAMBDA_SEXY_ANIMS).orchestra(OrchestrasD1.ORCHESTRA_SHREDDER_SEXY)
				).setDefaultAmmo(StandardAmmoType.G10, 50);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_DOUBLE_BARREL = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	/** Fires a single barrel (one round instead of the receiver's two rounds per cycle). */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DOUBLE_SECONDARY = (stack, ctx) -> {

		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		Receiver rec = ctx.config.getReceivers(stack)[0];
		int index = ctx.configIndex;
		GunState state = ItemGunBaseNT.getState(stack, index);

		if(state == GunState.IDLE) {

			if(rec.getCanFire(stack).apply(stack, ctx)) {
				rec.getOnFire(stack).accept(stack, ctx);

				if(rec.getFireSound(stack) != null)
					GunEffects.playSound(entity, rec.getFireSound(stack).get(), rec.getFireVolume(stack), rec.getFirePitch(stack));

				ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
				ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
			} else {

				if(rec.getDoesDryFire(stack)) {
					ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
					ItemGunBaseNT.setState(stack, index, rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
					ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
				}
			}
		}

		if(state == GunState.RELOADING) {
			ItemGunBaseNT.setReloadCancel(stack, true);
		}
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DOUBLE_BARREL_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
				.addBus("BUCKLE", new BusAnimationSequence().addPos(0, -60, 0, 50).addPos(0, 0, 0, 250));
		case RELOAD: return new BusAnimation()
				.addBus("TURN", new BusAnimationSequence()
						.addPos(0, 30, 0, 350, IType.SIN_FULL)
						.addPos(0, 30, 0, 1150)
						.addPos(0, 0, 0, 350, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence()
						.addPos(0, 0, 0, 250)
						.addPos(0, 0, -90, 100, IType.SIN_FULL)
						.addPos(0, 0, -90, 1300)
						.addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BARREL", new BusAnimationSequence()
						.addPos(0, 0, 0, 300)
						.addPos(60, 0, 0, 150, IType.SIN_UP)
						.addPos(60, 0, 0, 1150)
						.addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("LIFT", new BusAnimationSequence()
						.addPos(0, 0, 0, 350)
						.addPos(-5, 0, 0, 150, IType.SIN_FULL)
						.addPos(0, 0, 0, 100, IType.SIN_FULL)
						.addPos(0, 0, 0, 700)
						.addPos(-5, 0, 0, 100, IType.SIN_FULL)
						.addPos(0, 0, 0, 100, IType.SIN_UP) //1500
						.addPos(45, 0, 0, 150)
						.addPos(45, 0, 0, 150)
						.addPos(-5, 0, 0, 150, IType.SIN_DOWN)
						.addPos(0, 0, 0, 100, IType.SIN_FULL)) //2050
				.addBus("SHELLS", new BusAnimationSequence()
						.addPos(0, 0, 0, 450)
						.addPos(0, 0, -2.5, 100)
						.addPos(0, -5, -5, 350, IType.SIN_DOWN)
						.addPos(0, -3, -2, 0)
						.addPos(0, 0, -2, 250)
						.addPos(0, 0, 0, 150, IType.SIN_UP)) //1300
				.addBus("SHELL_FLIP", new BusAnimationSequence().addPos(0, 0, 0, 450).addPos(-360, 0, 0, 450).addPos(0, 0, 0, 0));
		case INSPECT: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence()
						.addPos(0, 0, 0, 250)
						.addPos(0, 0, -90, 100, IType.SIN_FULL)
						.addPos(0, 0, -90, 800)
						.addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BARREL", new BusAnimationSequence()
						.addPos(0, 0, 0, 300)
						.addPos(60, 0, 0, 150, IType.SIN_UP)
						.addPos(60, 0, 0, 650)
						.addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("LIFT", new BusAnimationSequence()
						.addPos(0, 0, 0, 350)
						.addPos(-5, 0, 0, 150, IType.SIN_FULL)
						.addPos(0, 0, 0, 100, IType.SIN_FULL)
						.addPos(0, 0, 0, 200)
						.addPos(-5, 0, 0, 100, IType.SIN_FULL)
						.addPos(0, 0, 0, 100, IType.SIN_UP) //1500
						.addPos(45, 0, 0, 150)
						.addPos(45, 0, 0, 150)
						.addPos(-5, 0, 0, 150, IType.SIN_DOWN)
						.addPos(0, 0, 0, 100, IType.SIN_FULL));
		}

		return null;
	};
}
