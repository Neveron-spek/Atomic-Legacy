package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;

import net.minecraft.world.item.ItemStack;

/**
 * [weapon mods G3] 1.7.10 WeaponModLasCapacitor (gun_lasrifle extended capacitor, slot UNDERBARREL).
 * The original borrows WeaponModStackMag.DUMMY_FULL as the scratch magazine; this class keeps its own instance.
 */
public class WeaponModLasCapacitor extends WeaponModBase {

	protected static final MagazineFullReload DUMMY_FULL = new MagazineFullReload(0, 0);

	public WeaponModLasCapacitor(int id) {
		super(id, "UNDERBARREL");
		this.setPriority(PRIORITY_MULTIPLICATIVE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.F_BASEDAMAGE) return cast((Float) base * 1.05F, base);
		if(key == Receiver.O_MAGAZINE) {
			MagazineFullReload original = (MagazineFullReload) base;
			DUMMY_FULL.acceptedBullets = original.acceptedBullets;
			DUMMY_FULL.capacity = original.capacity * 3 / 2;
			DUMMY_FULL.index = original.index;
			return (T) DUMMY_FULL;
		}
		return base;
	}
}
