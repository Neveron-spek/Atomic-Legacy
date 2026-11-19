package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.GunEffects;
import com.atomiclegacy.items.weapon.sedna.factory.Orchestras;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModGreasegun (M3 refurbishment, slot FURNITURE). */
public class WeaponModGreasegun extends WeaponModBase {

	public WeaponModGreasegun(int id) {
		super(id, "FURNITURE");
		this.setPriority(PRIORITY_ADDITIVE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.F_DURABILITY) return cast((Float) base * 3F, base);
		if(key == Receiver.F_BASEDAMAGE) return cast((Float) base + 2F, base);
		if(key == Receiver.F_SPREADINNATE) return cast(0F, base);
		if(key == Receiver.I_DELAYAFTERFIRE) return cast((Integer) base / 2, base);
		if(key == GunConfig.CON_ORCHESTRA) return (T) ORCHESTRA_GREASEGUN;
		return base;
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_GREASEGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 1) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.55, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -7.5F + (float)entity.getRandom().nextGaussian() * 5F, 12F + (float)entity.getRandom().nextGaussian() * 5F, casing);
			}
			return;
		}
		Orchestras.ORCHESTRA_GREASEGUN.accept(stack, ctx);
	};
}
