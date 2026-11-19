package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.GunConfig;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModGenericDurability (weapon_mod_generic *_DURA, slot GENERIC_DURABILITY, durability x2). */
public class WeaponModGenericDurability extends WeaponModBase {

	public WeaponModGenericDurability(int id) {
		super(id, "GENERIC_DURABILITY");
		this.setPriority(PRIORITY_MULTIPLICATIVE);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(parent instanceof GunConfig && key == GunConfig.F_DURABILITY && base instanceof Float) {
			return cast((Float) base * 2F, base);
		}

		return base;
	}
}
