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

/** 1.7.10 MagazineSingleTypeBase, including the ammo bag / casing bag branches. [parity b1: ammo bag] */
public abstract class MagazineSingleTypeBase implements IMagazine<BulletConfig> {

	public static final String KEY_MAG_COUNT = "magcount";
	public static final String KEY_MAG_TYPE = "magtype";
	public static final String KEY_MAG_PREV = "magprev";
	public static final String KEY_MAG_AFTER = "magafter";

	public List<BulletConfig> acceptedBullets = new ArrayList<>();

	/** A number so the gun tell multiple mags apart */
	public int index;
	/** How much ammo this mag can hold */
	public int capacity;

	public MagazineSingleTypeBase(int index, int capacity) {
		this.index = index;
		this.capacity = capacity;
	}

	public MagazineSingleTypeBase addConfigs(BulletConfig... cfgs) { for(BulletConfig cfg : cfgs) acceptedBullets.add(cfg); return this; }

	@Override
	public BulletConfig getType(ItemStack stack, Container inventory) {
		int type = getMagType(stack, index);
		if(type >= 0 && type < BulletConfig.configs.size()) {
			BulletConfig cfg = BulletConfig.configs.get(type);
			if(acceptedBullets.contains(cfg)) return cfg;
			return acceptedBullets.get(0);
		}
		return null;
	}

	@Override
	public void setType(ItemStack stack, BulletConfig type) {
		int i = BulletConfig.configs.indexOf(type);
		if(i >= 0) setMagType(stack, index, i);
	}

	@Override
	public ItemStack getIconForHUD(ItemStack stack, Player player) {
		BulletConfig config = this.getType(stack, player != null ? player.getInventory() : null);
		if(config != null) return config.toStack();
		return ItemStack.EMPTY;
	}

	@Override
	public String reportAmmoStateForHUD(ItemStack stack, Player player) {
		return getAmount(stack, player != null ? player.getInventory() : null) + " / " + getCapacity(stack);
	}

	@Override
	public int getCasing(ItemStack stack, Container inventory) {
		BulletConfig config = this.getType(stack, inventory);
		return config != null ? config.casingEffect : -1;
	}

	@Override
	public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
		if(!IMagazine.shouldUseUpTrenchie(inventory) && getCapacity(stack) != 1) return;
		this.setAmount(stack, this.getAmount(stack, inventory) - amount);
		IMagazine.handleAmmoBag(inventory, this.getType(stack, inventory), amount);
	}

	/** Returns true if the player has the same ammo if partially loaded, or any valid ammo if not */
	@Override
	public boolean canReload(ItemStack stack, Container inventory) {
		if(this.getAmount(stack, inventory) >= this.getCapacity(stack)) return false;
		if(inventory == null) return true;
		BulletConfig nextConfig = getFirstConfig(stack, inventory);
		return nextConfig != null;
	}

	public void standardReload(ItemStack stack, Container inventory, int loadLimit) {

		if(inventory == null) {
			BulletConfig config = this.getType(stack, inventory);
			if(config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); } //fixing broken NBT
			this.setAmount(stack, this.capacity);
			return;
		}

		for(int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);

			if(loadLimit <= 0) return;

			if(!slot.isEmpty()) {

				//mag is empty, assume next best type
				if(this.getAmount(stack, null) == 0) {

					for(BulletConfig config : this.acceptedBullets) {
						if(config.matches(slot)) {
							this.setType(stack, config);
							int wantsToLoad = (int) Math.ceil((double) this.getCapacity(stack) / (double) config.ammoReloadCount);
							int toLoad = Math.min(Math.min(wantsToLoad, slot.getCount()), loadLimit);
							this.setAmount(stack, Math.min(toLoad * config.ammoReloadCount, this.capacity));
							inventory.removeItem(i, toLoad);
							loadLimit -= toLoad;
							break;
						}
					}
				//mag has a type set, only load that
				} else {
					BulletConfig config = this.getType(stack, null);
					if(config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); } //fixing broken NBT

					if(config.matches(slot)) {
						int alreadyLoaded = this.getAmount(stack, null);
						int wantsToLoad = (int) Math.ceil((double) (this.getCapacity(stack) - alreadyLoaded) / (double) config.ammoReloadCount);
						int toLoad = Math.min(Math.min(wantsToLoad, slot.getCount()), loadLimit);
						this.setAmount(stack, Math.min((toLoad * config.ammoReloadCount) + alreadyLoaded, this.capacity));
						inventory.removeItem(i, toLoad);
						loadLimit -= toLoad;
					}
				}

				boolean infBag = LegacyAmmoBagItem.isInfiniteBag(slot);
				if(LegacyAmmoBagItem.isAmmoBag(slot)) {
					BagInventory bag = MagazineBelt.bag(slot, inventory);
					if(bag == null) continue;

					for(int j = 0; j < bag.getContainerSize(); j++) {
						ItemStack bagslot = bag.getItem(j);

						if(!bagslot.isEmpty()) {

							//mag is empty, assume next best type
							if(this.getAmount(stack, null) == 0) {

								for(BulletConfig config : this.acceptedBullets) {
									if(config.matches(bagslot)) {
										this.setType(stack, config);
										int wantsToLoad = (int) Math.ceil((double) this.getCapacity(stack) / (double) config.ammoReloadCount);
										int toLoad = Math.min(Math.min(wantsToLoad, infBag ? 9_999 : bagslot.getCount()), loadLimit);
										this.setAmount(stack, Math.min(toLoad * config.ammoReloadCount, this.capacity));
										if(!infBag) bag.removeItem(j, toLoad);
										loadLimit -= toLoad;
										break;
									}
								}
							//mag has a type set, only load that
							} else {
								BulletConfig config = this.getType(stack, null);
								if(config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); } //fixing broken NBT

								if(config.matches(bagslot)) {
									int alreadyLoaded = this.getAmount(stack, bag);
									int wantsToLoad = (int) Math.ceil((double) (this.getCapacity(stack) - alreadyLoaded) / (double) config.ammoReloadCount);
									int toLoad = Math.min(Math.min(wantsToLoad, infBag ? 9_999 : bagslot.getCount()), loadLimit);
									this.setAmount(stack, Math.min((toLoad * config.ammoReloadCount) + alreadyLoaded, this.capacity));
									if(!infBag) bag.removeItem(j, toLoad);
									loadLimit -= toLoad;
								}
							}
						}
					}
				}
			}
		}
	}

	/** Returns the config of the first potential loadable round, either what's already chambered or the first valid one if empty */
	public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {
		if(inventory == null) return null;

		for(int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);

			if(!slot.isEmpty()) {
				if(this.getAmount(stack, null) == 0) {
					for(BulletConfig config : this.acceptedBullets) {
						if(config.matches(slot)) return config;
					}
				} else {
					BulletConfig config = this.getType(stack, null);
					if(config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); }
					if(config.matches(slot)) return config;
				}

				if(LegacyAmmoBagItem.isAmmoBag(slot)) {
					BagInventory bag = MagazineBelt.bag(slot, inventory);
					if(bag == null) continue;

					for(int j = 0; j < bag.getContainerSize(); j++) {
						ItemStack bagslot = bag.getItem(j);

						if(!bagslot.isEmpty()) {
							if(this.getAmount(stack, null) == 0) {
								for(BulletConfig config : this.acceptedBullets) {
									if(config.matches(bagslot)) return config;
								}
							} else {
								BulletConfig config = this.getType(stack, null);
								if(config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); }
								if(config.matches(bagslot)) return config;
							}
						}
					}
				}
			}
		}

		return null;
	}

	@Override public void initNewType(ItemStack stack, Container inventory) {
		if(inventory == null) return;
		BulletConfig nextConfig = getFirstConfig(stack, inventory);
		if(nextConfig != null) {
			int i = BulletConfig.configs.indexOf(nextConfig);
			setMagType(stack, index, i);
		}
	}

	@Override public int getCapacity(ItemStack stack) { return capacity; }
	@Override public int getAmount(ItemStack stack, Container inventory) { return getMagCount(stack, index); }
	@Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, Math.max(amount, 0)); }

	@Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
	@Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
	@Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
	@Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

	public static int getMagType(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE + index); }
	public static void setMagType(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE + index, value); }

	public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
	public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
