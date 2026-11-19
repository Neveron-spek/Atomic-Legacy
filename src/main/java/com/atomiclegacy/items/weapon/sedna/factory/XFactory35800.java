package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryFolly.SecretBeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * [weapons batch F4] 1.7.10 XFactory35800: the ammo_secret beams p35800 / p35800_bl (.35-800 V9, black lightning) with the
 * "35-800" casing, gun_aberrator (SECRET) and gun_aberrator_eott (SECRET, two GunConfigs: left mag index 0 / right mag
 * index 1). ServerConfig GUN_ABERRATOR_DAMAGE, GUN_EOTT_LEFT_DAMAGE, GUN_EOTT_RIGHT_DAMAGE are HbmConfig entries.
 */
public class XFactory35800 {

	public static final BeamBulletConfig p35800;
	public static final BeamBulletConfig p35800_bl;

	public static final BiConsumer<LegacyBulletBeamEntity, HitResult> LAMBDA_BLACK_IMPACT = (bullet, mop) -> {
		if(mop instanceof EntityHitResult ehr) {
			if(ehr.getEntity() instanceof LivingEntity living) {
				HbmLivingHazards.get(living).addBlackFire(200);
			}
		}
		if(mop instanceof BlockHitResult && mop.getType() == HitResult.Type.BLOCK) {
			// EntityFireLingering.setArea(7.5F, 2F).setDuration(200).setType(TYPE_BLACK) at the hit vector
			LegacyProjectileUtil.spawnLingeringFire(bullet, mop.getLocation(), LegacyLingeringFireEntity.FireType.BLACK, 7.5F, 2F, 200, false);
		}

		BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT.accept(bullet, mop);
	};

	static {
		StandardAmmoType.P35_800.setSpread(0F).setLife(3);
		StandardAmmoType.P35_800_BL.setSpread(0F).setLife(3);
		p35800 = (BeamBulletConfig) new SecretBeamBulletConfig(StandardAmmoType.P35_800, "p35_800").setArmorPiercing(0.5F).setThresholdNegation(50F).setLife(3).setRenderRotations(false)
				.setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setBeamRenderer(BeamBulletConfig.RENDER_CRACKLE).setCasingEffect(OrchestrasF4.CASING_P35_800);
		p35800_bl = (BeamBulletConfig) new SecretBeamBulletConfig(StandardAmmoType.P35_800_BL, "p35_800_bl").setArmorPiercing(0.5F).setThresholdNegation(50F).setLife(3).setRenderRotations(false)
				.setOnBeamImpact(LAMBDA_BLACK_IMPACT).setBeamRenderer(BeamBulletConfig.RENDER_BLACK_LIGHTNING).setCasingEffect(OrchestrasF4.CASING_P35_800);
	}

	/** Lego.LAMBDA_NOWEAR_FIRE: doStandardFire without wear. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE = (stack, ctx) -> {
		Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
	};

	public static ItemGunBaseNT createAberrator(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SECRET, new GunConfig()
				.dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::aberratorDamage).delay(13).dry(21).reload(51).sound(HbmSoundEvents.GUN_ABERRATOR_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 5).addConfigs(p35800, p35800_bl))
						.offset(0.75, -0.0625 * 1.5, -0.1875)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_NOWEAR_FIRE).recoil(LAMBDA_RECOIL_ABERRATOR))
				.setupStandardConfiguration()
				.anim(LAMBDA_ABERRATOR).orchestra(OrchestrasF4.ORCHESTRA_ABERRATOR)
				);
	}

	public static ItemGunBaseNT createAberratorEott(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SECRET,
				new GunConfig().dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::eottLeftDamage).spreadHipfire(0F).delay(13).dry(21).reload(51).sound(HbmSoundEvents.GUN_ABERRATOR_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 5).addConfigs(p35800, p35800_bl))
						.offset(0.75, -0.0625 * 1.5, 0.1875)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_NOWEAR_FIRE).recoil(LAMBDA_RECOIL_ABERRATOR))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_ABERRATOR).orchestra(OrchestrasF4.ORCHESTRA_ABERRATOR),
				new GunConfig().dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::eottRightDamage).spreadHipfire(0F).delay(13).dry(21).reload(51).sound(HbmSoundEvents.GUN_ABERRATOR_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(1, 5).addConfigs(p35800, p35800_bl))
						.offset(0.75, -0.0625 * 1.5, -0.1875)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_NOWEAR_FIRE).recoil(LAMBDA_RECOIL_ABERRATOR))
				.ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_ABERRATOR).orchestra(OrchestrasF4.ORCHESTRA_ABERRATOR)
				);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ABERRATOR = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_ABERRATOR = (stack, type) -> {
		boolean aim = ItemGunBaseNT.getIsAiming(stack);
		int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(360, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("RISE", new BusAnimationSequence().addPos(0, -3, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(aim ? -15 : -25, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("SIGHT", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(aim ? 5 : 15, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -1.125, 50, IType.SIN_DOWN).addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus(ammo <= 1 ? "NULL" : "BULLET", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0.375, 1.125, 150, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(45, 0, 0, 50).addPos(-45, 0, -1.125, 50, IType.SIN_DOWN).addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 700).addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 550).addPos(0, 0, -1.125, 150, IType.SIN_FULL).addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(45, 0, 0, 50).addPos(45, 0, 0, 500).addPos(-45, 0, -1.125, 150, IType.SIN_FULL).addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP));
		case RELOAD: return new BusAnimation()
				.addBus("ROLL", new BusAnimationSequence().addPos(0, 0, 20, 150, IType.SIN_FULL).addPos(0, 0, 20, 50).addPos(0, 0, -45, 150, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, -2, 0, 0).addPos(-15, -5, 0, 350).addPos(-15, 0, 0, 0).addPos(-15, 0, 0, 700).addPos(3, 3, 0, 0).addPos(0, -2, 0, 250, IType.SIN_DOWN).addPos(0, -2, 0, 50).addPos(0, 0, 0, 150, IType.SIN_DOWN))
				.addBus("MAGROLL", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, 0, -180, 250).addPos(0, 0, 0, 0))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(5, 0, 0, 150, IType.SIN_FULL).addPos(-190, 0, 0, 500, IType.SIN_FULL).addPos(-190, 0, 0, 450).addPos(-360, 0, 0, 350, IType.SIN_DOWN).addPos(0, 0, 0, 0))
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 2350).addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 2200).addPos(0, 0, -1.125, 150, IType.SIN_FULL).addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 0, 2250).addPos(-45, 0, -1.125, 100, IType.SIN_FULL).addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("BULLET", new BusAnimationSequence().addPos(ammo > 0 ? 0 : -100, 0, 0, 0).addPos(ammo > 0 ? 0 : -100, 0, 0, 2400).addPos(0, 0, 0, 0).addPos(0, 0.375, 1.125, 150, IType.SIN_UP));
		case INSPECT: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 0).addPos(-720, 0, 0, 1000, IType.SIN_FULL).addPos(-720, 0, 0, 250).addPos(0, 0, 0, 1000, IType.SIN_FULL));
		}

		return null;
	};
}
