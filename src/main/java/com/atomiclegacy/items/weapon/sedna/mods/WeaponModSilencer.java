package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModSilencer (slot SILENCER): replaces the fire sound. */
public class WeaponModSilencer extends WeaponModBase {

	public WeaponModSilencer(int id) {
		super(id, "SILENCER");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(key == Receiver.S_FIRESOUND) {
			if(gun.getItem() == HbmItems.GUN_AMAT.get()) return (T) HbmSoundEvents.GUN_AMAT_SILENCER;
			return (T) HbmSoundEvents.GUN_RIFLE_SILENCER;
		}

		return base;
	}
}
