package com.atomiclegacy.items.weapon.sedna.mags;

import com.atomiclegacy.items.weapon.sedna.BulletConfig;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * [weapons batch F2] 1.7.10 com.hbm.items.weapon.sedna.mags.MagazineInfinite: a fixed bullet config that never
 * runs out and never reloads (gun_n_i_4_n_i). getIconForHUD returned ModItems.nothing in 1.7.10; the HUD ammo
 * counter skips an empty stack, which is the same blank slot.
 */
public class MagazineInfinite implements IMagazine<BulletConfig> {

	public BulletConfig type;

	public MagazineInfinite(BulletConfig type) {
		this.type = type;
	}

	@Override
	public BulletConfig getType(ItemStack stack, Container inventory) {
		return this.type;
	}

	@Override public void setType(ItemStack stack, BulletConfig type) { }
	@Override public int getCapacity(ItemStack stack) { return 9999; }
	@Override public int getAmount(ItemStack stack, Container inventory) { return 9999; }
	@Override public void setAmount(ItemStack stack, int amount) { }
	@Override public void useUpAmmo(ItemStack stack, Container inventory, int amount) { }
	@Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
	@Override public void initNewType(ItemStack stack, Container inventory) { }
	@Override public void reloadAction(ItemStack stack, Container inventory) { }
	@Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return ItemStack.EMPTY; }
	@Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return "∞"; }
	@Override public int getCasing(ItemStack stack, Container inventory) { return this.type.casingEffect; }
	@Override public void setAmountBeforeReload(ItemStack stack, int amount) { }
	@Override public int getAmountBeforeReload(ItemStack stack) { return 9999; }
	@Override public void setAmountAfterReload(ItemStack stack, int amount) { }
	@Override public int getAmountAfterReload(ItemStack stack) { return 9999; }
}
