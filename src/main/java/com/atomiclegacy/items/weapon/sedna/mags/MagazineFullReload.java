package com.atomiclegacy.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 MagazineFullReload. */
public class MagazineFullReload extends MagazineSingleTypeBase {

	public MagazineFullReload(int index, int capacity) {
		super(index, capacity);
	}

	/** Reloads all rounds at once. If the mag is empty, the mag's type will change to the first valid ammo type */
	@Override
	public void reloadAction(ItemStack stack, Container inventory) {
		standardReload(stack, inventory, this.capacity);
	}
}
