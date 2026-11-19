package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModChoke (slot BARREL). */
public class WeaponModChoke extends WeaponModBase {

	public WeaponModChoke(int id) {
		super(id, "BARREL");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.F_SPREADAMMO) { return cast((Float) base * 0.5F, base); }

		return base;
	}
}
