package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModTestFirerate (weapon_mod_test FIRERATE, halves the delay after fire, minimum 1). */
public class WeaponModTestFirerate extends WeaponModBase {

	public WeaponModTestFirerate(int id, String... slots) {
		super(id, slots);
		this.setPriority(PRIORITY_MULT_FINAL);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(parent instanceof Receiver && key == Receiver.I_DELAYAFTERFIRE && base instanceof Integer) {
			return cast(Math.max((Integer) base / 2, 1), base);
		}

		return base;
	}
}
