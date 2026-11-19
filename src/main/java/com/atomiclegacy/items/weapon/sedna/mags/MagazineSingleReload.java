package com.atomiclegacy.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 MagazineSingleReload: uses individual bullets which are loaded one by one. */
public class MagazineSingleReload extends MagazineSingleTypeBase {

	public MagazineSingleReload(int index, int capacity) {
		super(index, capacity);
	}

	/** Loads one round per reload action. If the mag is empty, the mag's type will change to the first valid ammo type */
	@Override
	public void reloadAction(ItemStack stack, Container inventory) {
		standardReload(stack, inventory, 1);
	}
}
