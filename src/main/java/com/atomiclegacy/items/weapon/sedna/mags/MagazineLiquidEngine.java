package com.atomiclegacy.items.weapon.sedna.mags;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.FluidIconItem;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F3] 1.7.10 MagazineLiquidEngine: type of fixed ammo engine (gun_drill). FluidType is the port's HbmFluidDefinition. */
public class MagazineLiquidEngine implements IMagazine<HbmFluidDefinition> {

	public static final String KEY_MAG_COUNT = "magcount";
	public static final String KEY_MAG_PREV = "magprev";
	public static final String KEY_MAG_AFTER = "magafter";

	/** A number so the gun tell multiple mags apart */
	public int index;
	/** How much ammo this mag can hold */
	public int capacity;
	/** Whichever fluids we can pour in this bastard */
	public HbmFluidDefinition[] acceptedTypes;

	public MagazineLiquidEngine(int index, int capacity, HbmFluidDefinition... acceptedTypes) {
		this.index = index;
		this.capacity = capacity;
		this.acceptedTypes = acceptedTypes;
	}

	@Override public HbmFluidDefinition getType(ItemStack stack, Container inventory) { return acceptedTypes[0]; }
	@Override public void setType(ItemStack stack, HbmFluidDefinition type) { }
	@Override public int getCapacity(ItemStack stack) { return capacity; }

	@Override
	public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
		this.setAmount(stack, Math.max(this.getAmount(stack, inventory) - amount, 0));
	}

	@Override public int getAmount(ItemStack stack, Container inventory) { return getMagCount(stack, index); }
	@Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }

	@Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
	@Override public void initNewType(ItemStack stack, Container inventory) { }
	@Override public void reloadAction(ItemStack stack, Container inventory) { }
	@Override public int getCasing(ItemStack stack, Container inventory) { return -1; }

	/** new ItemStack(ModItems.fluid_icon, 1, type.getID()) */
	@Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return FluidIconItem.forFluid(this.getType(stack, player != null ? player.getInventory() : null)); }
	@Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return getAmount(stack, player != null ? player.getInventory() : null) + "/" + this.capacity + "mB"; }

	@Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
	@Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
	@Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
	@Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

	public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
	public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
