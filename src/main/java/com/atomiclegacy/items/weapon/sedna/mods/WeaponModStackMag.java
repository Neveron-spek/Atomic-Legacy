package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModStackMag (slot MAG): capacity * 3 / 2. */
public class WeaponModStackMag extends WeaponModBase {

	// the single instances are reused and re-adjusted like in the original: magazines are not cached anywhere,
	// a magazine is only relevant for accessing the stack ammo info and for the reload operation of the state machine
	protected static MagazineSingleReload DUMMY_SINGLE = new MagazineSingleReload(0, 0);
	protected static MagazineFullReload DUMMY_FULL = new MagazineFullReload(0, 0);

	public WeaponModStackMag(int id) {
		super(id, "MAG");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(key == Receiver.O_MAGAZINE) {
			if(base instanceof MagazineSingleReload) {
				MagazineSingleReload original = (MagazineSingleReload) base;
				DUMMY_SINGLE.acceptedBullets = original.acceptedBullets;
				DUMMY_SINGLE.capacity = original.capacity * 3 / 2;
				DUMMY_SINGLE.index = original.index;
				return (T) DUMMY_SINGLE;
			}
			if(base instanceof MagazineFullReload) {
				MagazineFullReload original = (MagazineFullReload) base;
				DUMMY_FULL.acceptedBullets = original.acceptedBullets;
				DUMMY_FULL.capacity = original.capacity * 3 / 2;
				DUMMY_FULL.index = original.index;
				return (T) DUMMY_FULL;
			}
		}

		return base;
	}

	@Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
	@Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}
