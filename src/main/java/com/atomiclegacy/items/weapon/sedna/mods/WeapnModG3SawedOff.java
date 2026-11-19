package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiFunction;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeapnModG3SawedOff (sic; slot SHIELD; gun_g3, gun_g3_zebra): removes the stock. */
public class WeapnModG3SawedOff extends WeaponModBase {

	public WeapnModG3SawedOff(int id) {
		super(id, "SHIELD");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.I_DRAWDURATION) return cast(5, base);
		if(key == GunConfig.FUN_ANIMNATIONS) return (T) LAMBDA_G3_ANIMS;
		return base;
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_G3_ANIMS = (stack, type) -> {
		switch(type) {
			case EQUIP: return new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_FULL));
		}
		return XFactory556mm.LAMBDA_G3_ANIMS.apply(stack, type);
	};
}
