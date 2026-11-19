package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModGenericDamage (weapon_mod_generic *_DAMAGE, slot GENERIC_DAMAGE, base damage x1.15). */
public class WeaponModGenericDamage extends WeaponModBase {

	public WeaponModGenericDamage(int id) {
		super(id, "GENERIC_DAMAGE");
		this.setPriority(PRIORITY_MULTIPLICATIVE);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(parent instanceof Receiver && key == Receiver.F_BASEDAMAGE && base instanceof Float) {
			return cast((Float) base * 1.15F, base);
		}

		return base;
	}
}
