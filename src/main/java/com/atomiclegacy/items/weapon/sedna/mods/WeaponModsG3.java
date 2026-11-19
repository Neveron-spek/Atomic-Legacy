package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.List;

import com.atomiclegacy.registry.HbmItems;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * [weapon mods G3] The weapon_mod_special variants LAS_SHOTGUN .. SIFTER of 1.7.10 EnumModSpecial (one flattened item per
 * variant), in enum order. ModItems registers weapon_mod_special after ammo_standard / ammo_secret, which is where the
 * weapon tab lists them.
 */
public final class WeaponModsG3 {

	private WeaponModsG3() {
	}

	public static List<DeferredItem<Item>> items() {
		return List.of(
				HbmItems.WEAPON_MOD_LAS_SHOTGUN, HbmItems.WEAPON_MOD_LAS_CAPACITOR, HbmItems.WEAPON_MOD_LAS_AUTO,
				HbmItems.WEAPON_MOD_NICKEL, HbmItems.WEAPON_MOD_DOUBLOONS,
				HbmItems.WEAPON_MOD_DRILL_HSS, HbmItems.WEAPON_MOD_DRILL_WEAPONSTEEL, HbmItems.WEAPON_MOD_DRILL_TCALLOY, HbmItems.WEAPON_MOD_DRILL_SATURNITE,
				HbmItems.WEAPON_MOD_ENGINE_DIESEL, HbmItems.WEAPON_MOD_ENGINE_AVIATION, HbmItems.WEAPON_MOD_ENGINE_ELECTRIC, HbmItems.WEAPON_MOD_ENGINE_TURBO,
				HbmItems.WEAPON_MOD_MAGNET, HbmItems.WEAPON_MOD_SIFTER);
	}

	public static void addCreativeTab(CreativeModeTab.Output output) {
		for(DeferredItem<Item> item : items()) output.accept(item);
	}
}
