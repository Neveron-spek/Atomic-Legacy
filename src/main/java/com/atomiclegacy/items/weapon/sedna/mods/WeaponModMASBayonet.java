package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC3;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory44;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModMASBayonet (slot BAYONET; gun_mas36): the secondary key stabs instead of inspecting. */
public class WeaponModMASBayonet extends WeaponModBase {

	public WeaponModMASBayonet(int id) {
		super(id, "BAYONET");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.FUN_ANIMNATIONS) return (T) LAMBDA_MAS36_ANIMS;
		if(key == GunConfig.I_INSPECTDURATION) return cast(30, base);
		if(key == GunConfig.CON_ONPRESSSECONDARY) return (T) XFactory44.SMACK_A_FUCKER;
		if(key == GunConfig.CON_ORCHESTRA) return (T) ORCHESTRA_MAS36;
		if(key == GunConfig.I_INSPECTCANCEL) return cast(false, base);
		return base;
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MAS36 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.INSPECT) {
			if(timer == 15 && ctx.getPlayer() != null) WeaponModCarbineBayonet.stab(ctx, HbmConfig.southStarBayonetDamage());
			return;
		}

		OrchestrasC3.ORCHESTRA_MAS36.accept(stack, ctx);
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MAS36_ANIMS = (stack, type) -> {
		switch(type) {
		case INSPECT: return new BusAnimation()
				.addBus("STAB", new BusAnimationSequence().addPos(0, 1, -2, 250, IType.SIN_DOWN).hold(250).addPos(0, 1, 5, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 500, IType.SIN_FULL));
		}
		return XFactory762mm.LAMBDA_MAS36_ANIMS.apply(stack, type);
	};
}
