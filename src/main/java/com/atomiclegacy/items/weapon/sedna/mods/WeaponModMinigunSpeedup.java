package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModMinigunSpeedup (slot SPEED; gun_minigun). */
public class WeaponModMinigunSpeedup extends WeaponModBase {

	public WeaponModMinigunSpeedup(int id) {
		super(id, "SPEED");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.I_ROUNDSPERCYCLE) return cast((Integer) base * 3, base);
		if(key == Receiver.F_SPREADINNATE) return cast((Float) base * 1.5F, base);
		return base;
	}
}
