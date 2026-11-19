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
 * 1.7.10 XFactoryBlackPowder: gun_pepperbox and the four ball-and-powder rounds. Damage multiplier,
 * penetration, falloff and headshot (1.0) are carried by the StandardAmmoType STONE entries; spread,
 * ricochet, projectile count and the black powder flag are set here.
 */
public class XFactoryBlackPowder {

	static {
		StandardAmmoType.STONE.setBlackPowder(true).setSpread(0.025F).setRicochetAngle(15);
		StandardAmmoType.STONE_AP.setBlackPowder(true).setSpread(0.01F).setRicochetAngle(5);
		StandardAmmoType.STONE_IRON.setBlackPowder(true).setSpread(0F).setRicochetAngle(90).setRicochetCount(5);
		StandardAmmoType.STONE_SHOT.setBlackPowder(true).setSpread(0.1F).setRicochetAngle(45).setProjectiles(6, 6);
	}

	public static final BulletConfig stone = new BulletConfig(StandardAmmoType.STONE);
	public static final BulletConfig flint = new BulletConfig(StandardAmmoType.STONE_AP);
	public static final BulletConfig iron = new BulletConfig(StandardAmmoType.STONE_IRON);
	public static final BulletConfig shot = new BulletConfig(StandardAmmoType.STONE_SHOT);

	public static ItemGunBaseNT createPepperbox(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::pepperboxDamage).delay(27).reload(67).jam(58).sound(HbmSoundEvents.GUN_POWDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 6).addConfigs(stone, flint, iron, shot))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_PEPPERBOX))
				.setupStandardConfiguration()
				.anim(LAMBDA_PEPPERBOX_ANIMS).orchestra(OrchestrasC1.ORCHESTRA_PEPPERBOX)
				).setDefaultAmmo(StandardAmmoType.STONE, 12);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_PEPPERBOX = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PEPPERBOX_ANIMS = (stack, type) -> {
		switch(type) {
		case CYCLE: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 0, 1025).addPos(60, 0, 0, 250))
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(45, 0, 0, 150, IType.SIN_DOWN).addPos(45, 0, 0, 50).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("HAMMER", new BusAnimationSequence().addPos(80, 0, 0, 25).addPos(80, 0, 0, 1000).addPos(0, 0, 0, 250))
				.addBus("TRIGGER", new BusAnimationSequence().addPos(1, 0, 0, 25).addPos(1, 0, 0, 250).addPos(0, 0, 0, 100));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 0, 525).addPos(60, 0, 0, 250))
				.addBus("HAMMER", new BusAnimationSequence().addPos(80, 0, 0, 25).addPos(80, 0, 0, 500).addPos(0, 0, 0, 250))
				.addBus("TRIGGER", new BusAnimationSequence().addPos(1, 0, 0, 25).addPos(1, 0, 0, 250).addPos(0, 0, 0, 100));
		case EQUIP: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 200, IType.SIN_DOWN));
		case RELOAD: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 1600).addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(-5, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 200, IType.SIN_DOWN))
				.addBus("TRANSLATE", new BusAnimationSequence().addPos(0, -12, 5, 500, IType.SIN_FULL).addPos(0, -12, 5, 700).addPos(0, -13, 5, 200).addPos(0, -12, 5, 200).addPos(0, -12, 5, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("LOADER", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 5, -5, 0).addPos(0, 0, -0.1, 500, IType.SIN_FULL).addPos(0, 0, -1, 200).addPos(0, 0, -1, 200).addPos(0, 0, -0.1, 200).addPos(0, 5, -5, 500, IType.SIN_FULL).addPos(0, 0, 0, 0))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 0, 2600).addPos(-360 * 1, 0, 0, 750, IType.SIN_FULL))
				.addBus("SHOT", new BusAnimationSequence().addPos(1, 0, 0, 1400).addPos(0, 0, 0, 0));
		case INSPECT: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(-360 * 1, 0, 0, 750, IType.SIN_FULL))
				.addBus("RECOIL", new BusAnimationSequence().addPos(-5, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 200, IType.SIN_DOWN));
		case JAMMED: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 0, 1300).addPos(60, 0, 0, 500, IType.SIN_FULL).addPos(60, 0, 0, 400).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("TRANSLATE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, -6, 0, 400, IType.SIN_FULL).addPos(0, -6, 0, 2000).addPos(0, 0, 0, 400, IType.SIN_FULL))
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(45, 0, 0, 400, IType.SIN_FULL).addPos(45, 0, 0, 2000).addPos(0, 0, 0, 400, IType.SIN_FULL));
		}

		return null;
	};
}
