package com.atomiclegacy.items.weapon.sedna.mods;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * [weapon mods G3] 1.7.10 WeaponModDrillFortune (gun_drill MAGNET +2 / SIFTER +1 fortune, own slots).
 * EnchantmentUtil.get/remove/addEnchantment on Enchantment.fortune is the 1.21 enchantments component with the
 * data-driven minecraft:fortune holder (resolved from the running server or the client connection).
 */
public class WeaponModDrillFortune extends WeaponModBase {

	int addFortune = 0;

	public WeaponModDrillFortune(int id, String slot, int fortune) {
		super(id, slot);
		this.setPriority(PRIORITY_ADDITIVE);
		this.addFortune = fortune;
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		return base;
	}

	private static Holder<Enchantment> fortune() {
		HolderLookup.RegistryLookup<Enchantment> lookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
		return lookup == null ? null : lookup.get(Enchantments.FORTUNE).orElse(null);
	}

	@Override
	public void onInstall(ItemStack gun, ItemStack mod, int index) {
		Holder<Enchantment> fortune = fortune();
		if(fortune == null) return;
		int fortuneLvl = gun.getTagEnchantments().getLevel(fortune);
		int newLvl = fortuneLvl + this.addFortune;
		EnchantmentHelper.updateEnchantments(gun, enchantments -> enchantments.set(fortune, newLvl));
	}

	@Override
	public void onUninstall(ItemStack gun, ItemStack mod, int index) {
		Holder<Enchantment> fortune = fortune();
		if(fortune == null) return;
		int fortuneLvl = gun.getTagEnchantments().getLevel(fortune);
		int newLvl = fortuneLvl - this.addFortune;
		// removeEnchantment, then re-add only above 0 (Mutable#set drops the entry for levels <= 0)
		EnchantmentHelper.updateEnchantments(gun, enchantments -> enchantments.set(fortune, Math.max(newLvl, 0)));
	}
}
