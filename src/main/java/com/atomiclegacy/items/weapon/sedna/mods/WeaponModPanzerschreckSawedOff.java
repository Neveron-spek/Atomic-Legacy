package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryRocket;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModPanzerschreckSawedOff (slot SHIELD): no blast shield, the shooter is set on fire. */
public class WeaponModPanzerschreckSawedOff extends WeaponModBase {

	public WeaponModPanzerschreckSawedOff(int id) {
		super(id, "SHIELD");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.I_DRAWDURATION) return cast(5, base);
		if(key == Receiver.CON_ONFIRE) { return (T) LAMBDA_FIRE; }
		if(key == GunConfig.FUN_ANIMNATIONS) { return (T) LAMBDA_PANZERSCHRECK_ANIMS; }
		return base;
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PANZERSCHRECK_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_DOWN));
		}
		return XFactoryRocket.LAMBDA_PANZERSCHRECK_ANIMS.apply(stack, type);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) -> {
		Lego.LAMBDA_STANDARD_FIRE.accept(stack, ctx);
		if(ctx.entity != null) {
			// HbmLivingProps.getData(ctx.entity).fire += 100
			HbmLivingHazards.get(ctx.entity).addFire(100);
			// EntityDamageUtil.attackEntityFromNT(entity, BulletConfig.getDamage(entity, entity, FIRE), 4F, true, false, 0F, 0F, 0F)
			BeamBulletConfig.attackEntityFromNT(ctx.entity, BeamBulletConfig.getDamage(ctx.entity, ctx.entity, DamageClass.FIRE), DamageClass.FIRE, 4F, 0D, 0F, 0F);
		}
	};
}
