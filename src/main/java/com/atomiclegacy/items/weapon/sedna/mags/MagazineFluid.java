package com.atomiclegacy.items.weapon.sedna.mags;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.FluidIconItem;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmFluids;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * [weapons batch F1] 1.7.10 MagazineFluid: a magazine holding a fluid type (stored as the legacy fluid id) and an amount in mB.
 * It cannot be reloaded from the inventory; ItemGunChemthrower fills it through the fluid handler capability.
 */
public class MagazineFluid implements IMagazine<HbmFluidDefinition> {

	public static final String KEY_MAG_COUNT = "magcount";
	public static final String KEY_MAG_TYPE = "magtype";
	public static final String KEY_MAG_PREV = "magprev";
	public static final String KEY_MAG_AFTER = "magafter";

	/** A number so the gun tell multiple mags apart */
	public int index;
	/** How much ammo this mag can hold */
	public int capacity;

	public MagazineFluid(int index, int capacity) {
		this.index = index;
		this.capacity = capacity;
	}

	@Override
	public HbmFluidDefinition getType(ItemStack stack, Container inventory) {
		int id = getMagType(stack, index);
		return HbmFluids.byOldId(id).orElse(HbmFluids.none());
	}

	@Override
	public void setType(ItemStack stack, HbmFluidDefinition type) {
		setMagType(stack, index, type.oldId());
	}

	@Override
	public int getCapacity(ItemStack stack) {
		return capacity;
	}

	@Override
	public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
		this.setAmount(stack, this.getAmount(stack, inventory) - amount);
	}

	@Override public int getAmount(ItemStack stack, Container inventory) { return getMagCount(stack, index); }
	@Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }

	@Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
	@Override public void initNewType(ItemStack stack, Container inventory) { }
	@Override public void reloadAction(ItemStack stack, Container inventory) { }
	@Override public int getCasing(ItemStack stack, Container inventory) { return -1; }

	/** new ItemStack(ModItems.fluid_icon, 1, magtype) */
	@Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return FluidIconItem.forFluid(getType(stack, player != null ? player.getInventory() : null)); }
	@Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return getAmount(stack, player != null ? player.getInventory() : null) + "mB"; }

	@Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
	@Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
	@Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
	@Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

	public static int getMagType(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE + index); }
	public static void setMagType(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE + index, value); }
	public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
	public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
