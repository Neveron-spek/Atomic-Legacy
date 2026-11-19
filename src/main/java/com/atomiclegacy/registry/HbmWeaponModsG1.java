package com.atomiclegacy.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.atomiclegacy.items.weapon.sedna.factory.XFactory45;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 1.7.10 weapon_mod_test, weapon_mod_generic and weapon_mod_caliber (GunFactory: ItemEnumMulti, stack size 1).
 * Every metadata variant is a flattened item {@code <item>_<enum name lower case>}, like weapon_mod_special_scope.
 * The lists keep the enum order of GunFactory.EnumModTest / EnumModGeneric / EnumModCaliber.
 */
public final class HbmWeaponModsG1 {

	private HbmWeaponModsG1() {
	}

	/** GunFactory.EnumModTest. */
	public static final String[] TEST = {
			"firerate", "damage", "multi",
			"override_2_5", "override_5", "override_7_5", "override_10", "override_12_5", "override_15", "override_20" };
	/** GunFactory.EnumModGeneric. */
	public static final String[] GENERIC = {
			"iron_damage", "iron_dura",
			"steel_damage", "steel_dura",
			"dura_damage", "dura_dura",
			"desh_damage", "desh_dura",
			"wsteel_damage", "wsteel_dura",
			"ferro_damage", "ferro_dura",
			"tcalloy_damage", "tcalloy_dura",
			"bigmt_damage", "bigmt_dura",
			"bronze_damage", "bronze_dura" };
	/** GunFactory.EnumModCaliber. */
	public static final String[] CALIBER = { "p9", "p45", "p22", "m357", "m44", "r556", "r762", "bmg50" };

	/** weapon_mod_test: no creative tab in 1.7.10. */
	public static final List<DeferredItem<Item>> TEST_ITEMS = new ArrayList<>();
	/** weapon_mod_generic: MainRegistry.weaponTab. */
	public static final List<DeferredItem<Item>> GENERIC_ITEMS = new ArrayList<>();
	/** weapon_mod_caliber: MainRegistry.weaponTab. */
	public static final List<DeferredItem<Item>> CALIBER_ITEMS = new ArrayList<>();

	/** Called once from the HbmItems static initializer, after every gun registration. */
	public static void register(BiFunction<String, Supplier<Item>, DeferredItem<Item>> registrar) {
		for(String name : TEST) TEST_ITEMS.add(registrar.apply("weapon_mod_test_" + name, HbmWeaponModsG1::create));
		for(String name : GENERIC) GENERIC_ITEMS.add(registrar.apply("weapon_mod_generic_" + name, HbmWeaponModsG1::create));
		for(String name : CALIBER) CALIBER_ITEMS.add(registrar.apply("weapon_mod_caliber_" + name, () -> {
			// 1.7.10 GunFactory.init runs XFactory45.init after the gun factories; the suppliers run in registration order
			XFactory45.init();
			return create();
		}));
	}

	private static Item create() {
		return new Item(new Item.Properties().stacksTo(1));
	}

	public static DeferredItem<Item> test(String name) { return TEST_ITEMS.get(indexOf(TEST, name)); }
	public static DeferredItem<Item> generic(String name) { return GENERIC_ITEMS.get(indexOf(GENERIC, name)); }
	public static DeferredItem<Item> caliber(String name) { return CALIBER_ITEMS.get(indexOf(CALIBER, name)); }

	private static int indexOf(String[] names, String name) {
		for(int i = 0; i < names.length; i++) if(names[i].equals(name)) return i;
		throw new IllegalArgumentException("Unknown weapon mod variant: " + name);
	}

	/** Weapon tab entries in ModItems order: weapon_mod_generic variants; weapon_mod_test stays hidden. */
	public static void addCreativeGeneric(CreativeModeTab.Output output) {
		for(DeferredItem<Item> item : GENERIC_ITEMS) output.accept(item);
	}

	/** Weapon tab entries in ModItems order: weapon_mod_caliber variants (registered after weapon_mod_special). */
	public static void addCreativeCaliber(CreativeModeTab.Output output) {
		for(DeferredItem<Item> item : CALIBER_ITEMS) output.accept(item);
	}
}
