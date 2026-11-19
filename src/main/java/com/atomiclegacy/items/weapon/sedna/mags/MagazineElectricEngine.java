package com.atomiclegacy.items.weapon.sedna.mags;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** [weapon mods G3] 1.7.10 MagazineElectricEngine: HE buffer of the gun_drill ENGINE_ELECTRIC mod, charged through the item battery contract. */
public class MagazineElectricEngine implements IMagazine<Object> {

	public static final String KEY_MAG_COUNT = "magcount";
	public static final String KEY_MAG_PREV = "magprev";
	public static final String KEY_MAG_AFTER = "magafter";

	/** A number so the gun tell multiple mags apart */
	public int index;
	/** How much ammo this mag can hold */
	public int capacity;

	public MagazineElectricEngine(int index, int capacity) {
		this.index = index;
		this.capacity = capacity;
	}

	@Override public Object getType(ItemStack stack, Container inventory) { return null; }
	@Override public void setType(ItemStack stack, Object type) { }
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
	/** SpentCasing null */
	@Override public int getCasing(ItemStack stack, Container inventory) { return -1; }

	@Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return new ItemStack(HbmItems.BATTERY_CREATIVE.get()); }
	@Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return getShortNumber(getAmount(stack, player != null ? player.getInventory() : null)) + "/" + getShortNumber(this.capacity) + "HE"; }

	@Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
	@Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
	@Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
	@Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

	public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
	public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }

	/** BobMathUtil.getShortNumber */
	public static String getShortNumber(long l) {

		double res;
		String suffix = "";
		long abs = Math.abs(l);

		if(abs >= Math.pow(10, 18)) {
			res = l / Math.pow(10, 18);
			suffix = "E";
		}
		else if(abs >= Math.pow(10, 15)) {
			res = l / Math.pow(10, 15);
			suffix = "P";
		}
		else if(abs >= Math.pow(10, 12)) {
			res = l / Math.pow(10, 12);
			suffix = "T";
		}
		else if(abs >= Math.pow(10, 9)) {
			res = l / Math.pow(10, 9);
			suffix = "G";
		}
		else if(abs >= Math.pow(10, 6)) {
			res = l / Math.pow(10, 6);
			suffix = "M";
		}
		else if(abs >= Math.pow(10, 3)) {
			res = l / Math.pow(10, 3);
			suffix = "k";
		}
		else {
			return Long.toString(l);
		}

		// Edgecase: a negative triple digit number would result in a 8 character long result so we will loose one decimal place
		if (res <= -100.0) {
			res = Math.round(res * 10.0) / 10.0;
		} else {
			res = Math.round(res * 100.0) / 100.0;
		}

		return res + suffix;
	}
}
