package com.atomiclegacy.items.weapon.sedna.mods;

import net.minecraft.world.item.ItemStack;

/**
 * [weapon mods G3] 1.7.10 WeaponModNickel (gun_n_i_4_n_i NICKEL / DOUBLOONS, slots COIN1 / COIN2).
 * No eval changes; ItemGunNI4NI reads the two ids through hasUpgrade (+2 max coins each).
 */
public class WeaponModNickel extends WeaponModBase {

	public WeaponModNickel(int id, String name) {
		super(id, name);
		this.setPriority(PRIORITY_SET);
	}

	@Override public <T> T eval(T base, ItemStack gun, String key, Object parent) { return base; }
}
