package com.atomiclegacy.items.weapon.sedna.mags;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.items.LegacyAmmoBagItem;
import com.atomiclegacy.items.LegacyAmmoBagItem.BagInventory;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 com.hbm.items.weapon.sedna.mags.MagazineBelt: the gun has no magazine and
 * feeds directly from the inventory, including ammo bags; fired rounds feed the casing
 * bag (IMagazine.handleAmmoBag). [parity b1: ammo bag]
 */
public class MagazineBelt implements IMagazine<BulletConfig> {

	public List<BulletConfig> acceptedBullets = new ArrayList<>();

	public MagazineBelt addConfigs(BulletConfig... cfgs) { for(BulletConfig cfg : cfgs) acceptedBullets.add(cfg); return this; }

	@Override
	public BulletConfig getType(ItemStack stack, Container inventory) {
		BulletConfig config = getFirstConfig(stack, inventory);
		int id = BulletConfig.configs.indexOf(config);
		if(getMagType(stack) != id) {
			setMagType(stack, id);
		}
		return config;
	}

	@Override
	public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
		if(inventory == null) return;
		if(!IMagazine.shouldUseUpTrenchie(inventory)) return;

		BulletConfig first = this.getFirstConfig(stack, inventory);

		for(int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);

			if(!slot.isEmpty()) {
				if(first.matches(slot)) {
					int toRemove = Math.min(slot.getCount(), amount);
					amount -= toRemove;
					inventory.removeItem(i, toRemove);
					IMagazine.handleAmmoBag(inventory, first, toRemove);
					if(amount <= 0) return;
				}

				boolean infBag = LegacyAmmoBagItem.isInfiniteBag(slot);
				if(LegacyAmmoBagItem.isAmmoBag(slot)) {
					BagInventory bag = bag(slot, inventory);
					if(bag == null) continue;
					for(int j = 0; j < bag.getContainerSize(); j++) {
						ItemStack bagslot = bag.getItem(j);

						if(!bagslot.isEmpty()) {
							if(first.matches(bagslot)) {
								int toRemove = Math.min(bagslot.getCount(), amount);
								amount -= toRemove;
								if(!infBag) bag.removeItem(j, toRemove);
								IMagazine.handleAmmoBag(inventory, first, toRemove);
								if(amount <= 0) return;
							}
						}
					}
				}
			}
		}
	}

	@Override public void setType(ItemStack stack, BulletConfig type) { }
	@Override public int getCapacity(ItemStack stack) { return 0; }
	@Override public void setAmount(ItemStack stack, int amount) { }
	@Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
	@Override public void initNewType(ItemStack stack, Container inventory) { }
	@Override public void reloadAction(ItemStack stack, Container inventory) { }
	@Override public void setAmountBeforeReload(ItemStack stack, int amount) { }
	@Override public int getAmountBeforeReload(ItemStack stack) { return 0; }
	@Override public void setAmountAfterReload(ItemStack stack, int amount) { }
	@Override public int getAmountAfterReload(ItemStack stack) { return 0; }

	@Override
	public int getAmount(ItemStack stack, Container inventory) {
		if(inventory == null) return 1; // for EntityAIFireGun
		BulletConfig first = this.getFirstConfig(stack, inventory);
		int count = 0;
		for(int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);

			if(!slot.isEmpty()) {
				if(first.matches(slot)) count += slot.getCount();

				boolean infBag = LegacyAmmoBagItem.isInfiniteBag(slot);
				if(LegacyAmmoBagItem.isAmmoBag(slot)) {
					BagInventory bag = bag(slot, inventory);
					if(bag == null) continue;
					for(int j = 0; j < bag.getContainerSize(); j++) {
						ItemStack bagslot = bag.getItem(j);

						if(!bagslot.isEmpty()) {
							if(first.matches(bagslot)) {
								if(infBag) return 9_999;
								count += bagslot.getCount();
							}
						}
					}
				}
			}
		}
		return count;
	}

	@Override
	public ItemStack getIconForHUD(ItemStack stack, Player player) {
		BulletConfig first = this.getFirstConfig(stack, player != null ? player.getInventory() : null);
		return first.toStack();
	}

	@Override
	public String reportAmmoStateForHUD(ItemStack stack, Player player) {
		return "x" + getAmount(stack, player != null ? player.getInventory() : null);
	}

	@Override
	public int getCasing(ItemStack stack, Container inventory) {
		return getFirstConfig(stack, inventory).casingEffect;
	}

	public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {

		if(inventory == null) return acceptedBullets.get(0);

		for(int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);

			if(!slot.isEmpty()) {
				for(BulletConfig config : this.acceptedBullets) {
					if(config.matches(slot)) return config;
				}

				if(LegacyAmmoBagItem.isAmmoBag(slot)) {
					BagInventory bag = bag(slot, inventory);
					if(bag == null) continue;
					for(int j = 0; j < bag.getContainerSize(); j++) {
						ItemStack bagslot = bag.getItem(j);

						if(!bagslot.isEmpty()) {
							for(BulletConfig config : this.acceptedBullets) {
								if(config.matches(bagslot)) return config;
							}
						}
					}
				}
			}
		}

		int type = getMagType(stack);
		BulletConfig cached = type >= 0 && type < BulletConfig.configs.size() ? BulletConfig.configs.get(type) : null;
		return acceptedBullets.contains(cached) ? cached : acceptedBullets.get(0);
	}

	/** new InventoryAmmoBag(slot); the registries come from the owning player, null for inventories without one. */
	static BagInventory bag(ItemStack slot, Container inventory) {
		if(!(inventory instanceof net.minecraft.world.entity.player.Inventory inv) || !(slot.getItem() instanceof LegacyAmmoBagItem item)) return null;
		return new BagInventory(slot, inv.player.registryAccess(), item.kind());
	}

	public static final String KEY_MAG_TYPE = "magtype";
	public static int getMagType(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE); }
	public static void setMagType(ItemStack stack, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE, value); }
}
