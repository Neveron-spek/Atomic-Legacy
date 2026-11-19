package com.atomiclegacy.items.weapon.sedna.mags;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.util.ArmorModHandler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 IMagazine. The inventory is null for entities without one (e.g. mobs). */
public interface IMagazine<T> {

	public T getType(ItemStack stack, Container inventory);

	public void setType(ItemStack stack, T type);

	public int getCapacity(ItemStack stack);

	public int getAmount(ItemStack stack, Container inventory);

	public void setAmount(ItemStack stack, int amount);

	public void useUpAmmo(ItemStack stack, Container inventory, int amount);

	public boolean canReload(ItemStack stack, Container inventory);

	public void initNewType(ItemStack stack, Container inventory);

	public void reloadAction(ItemStack stack, Container inventory);

	public ItemStack getIconForHUD(ItemStack stack, Player player);

	public String reportAmmoStateForHUD(ItemStack stack, Player player);

	/** Casing effect id, see {@code BulletConfig#casingEffect}. */
	public int getCasing(ItemStack stack, Container inventory);

	public void setAmountBeforeReload(ItemStack stack, int amount);

	public int getAmountBeforeReload(ItemStack stack);

	public void setAmountAfterReload(ItemStack stack, int amount);

	public int getAmountAfterReload(ItemStack stack);

	/** [parity b1: casing bag] 1.7.10 IMagazine#handleAmmoBag: half of the fired casings go to the first casing bag of the main inventory that takes them. */
	public static void handleAmmoBag(Container inventory, com.atomiclegacy.items.weapon.sedna.BulletConfig config, int shotsFired) {
		if(config != null && config.casing != null && config.casing.amount() > 0 && inventory instanceof Inventory inv) {
			ItemStack casing = config.casingStack();
			if(casing.isEmpty()) return;
			for(ItemStack stack : inv.items) {
				if(com.atomiclegacy.items.LegacyAmmoBagItem.isCasingBag(stack) && com.atomiclegacy.items.LegacyAmmoBagItem.pushCasing(
						stack, casing, 1F / config.casing.amount() * 0.5F * shotsFired, inv.player.registryAccess())) return;
			}
		}
	}

	/**
	 * 1.7.10 IMagazine#shouldUseUpTrenchie: Trenchmaster armor or an Ace of
	 * Spades helmet mod keeps the round two out of three times.
	 */
	public static boolean shouldUseUpTrenchie(Container inv) {
		if(inv instanceof Inventory invPlayer) {
			boolean trenchie = isTrenchMaster(invPlayer.player);
			boolean aos = hasAoS(invPlayer.player);
			if(trenchie || aos) return invPlayer.player.getRandom().nextInt(3) < 2;
		}
		return true;
	}

	/** ArmorTrenchmaster#isTrenchMaster: Trenchmaster chestplate as part of an active full FSB set. */
	public static boolean isTrenchMaster(Player player) {
		if(player == null) return false;
		return player.getItemBySlot(EquipmentSlot.CHEST).is(HbmItems.TRENCHMASTER_PLATE.get()) && ArmorFSBItem.hasFSBArmor(player);
	}

	/** ArmorTrenchmaster#hasAoS: Ace of Spades installed in the helmet-only mod slot. */
	public static boolean hasAoS(Player player) {
		if(player == null) return false;
		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		if(helmet.isEmpty()) return false;
		ItemStack[] mods = ArmorModHandler.pryMods(helmet, player.registryAccess());
		ItemStack mod = mods[ArmorModHandler.HELMET_ONLY];
		return mod != null && !mod.isEmpty() && BuiltInRegistries.ITEM.getKey(mod.getItem()).equals(AtomicLegacy.id("card_aos"));
	}
}
