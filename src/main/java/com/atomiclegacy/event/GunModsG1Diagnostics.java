package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.inventory.container.WeaponTableMenu;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.GunEffects;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory22lr;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory357;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory44;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory45;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory50;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory9mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.atomiclegacy.items.weapon.sedna.mods.IWeaponMod;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModBase;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModCaliber;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModGenericDamage;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModGenericDurability;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModOverride;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModTestDamage;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModTestFirerate;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModTestMulti;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmWeaponModsG1;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for the weapon mods of group G1 (weapon_mod_test, weapon_mod_generic, weapon_mod_caliber)
 * against the pinned 1.7.10 XWeaponModManager table, the mod classes, GunFactory and WeaponRecipes.
 * The expected tables below are transcribed from the original and are independent of XWeaponModsG1.
 */
final class GunModsG1Diagnostics {

	private GunModsG1Diagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 40);

	/** XWeaponModManager.init(): weapon_mod_generic material -> gun list (damage id 100 + 2n, durability id 101 + 2n). */
	private static final String[][] GENERIC_GUNS = {
			{ "iron", "gun_pepperbox" },
			{ "steel", "gun_light_revolver", "gun_light_revolver_atlas", "gun_henry", "gun_henry_lincoln", "gun_greasegun", "gun_maresleg", "gun_maresleg_akimbo", "gun_flaregun" },
			{ "dura", "gun_am180", "gun_liberator", "gun_congolake", "gun_flamer", "gun_flamer_topaz" },
			{ "desh", "gun_heavy_revolver", "gun_carbine", "gun_uzi", "gun_uzi_akimbo", "gun_spas12", "gun_panzerschreck" },
			{ "wsteel", "gun_star_f", "gun_star_f_akimbo", "gun_g3", "gun_g3_zebra", "gun_mk108", "gun_chemthrower" },
			{ "ferro", "gun_amat", "gun_m2", "gun_autoshotgun", "gun_autoshotgun_shredder", "gun_quadro" },
			{ "tcalloy", "gun_lag", "gun_minigun", "gun_missile_launcher", "gun_tesla_cannon" },
			{ "bigmt", "gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_stg77", "gun_fatman", "gun_tau" },
			{ "bronze", "gun_lasrifle" },
	};

	/** XWeaponModManager.init(): {caliber, gun, id, capacity, base damage}. */
	private static final Object[][] CALIBER_MODS = {
			{ "p9", "gun_henry", 300, 28, 10F }, { "p9", "gun_star_f", 301, 12, 15F }, { "p9", "gun_star_f_akimbo", 302, 12, 15F },
			{ "p45", "gun_henry", 310, 28, 10F }, { "p45", "gun_greasegun", 311, 24, 3F }, { "p45", "gun_uzi", 312, 24, 3F },
			{ "p45", "gun_uzi_akimbo", 313, 24, 3F }, { "p45", "gun_lag", 314, 15, 25F },
			{ "p22", "gun_henry", 320, 28, 10F }, { "p22", "gun_uzi", 321, 40, 3F }, { "p22", "gun_uzi_akimbo", 322, 40, 3F },
			{ "m357", "gun_henry", 330, 20, 10F }, { "m357", "gun_lag", 331, 15, 25F },
			{ "m44", "gun_lag", 340, 13, 25F },
			{ "r556", "gun_henry", 350, 10, 10F }, { "r556", "gun_carbine", 351, 20, 15F }, { "r556", "gun_minigun", 352, 0, 6F }, { "r556", "gun_minigun_dual", 352, 0, 6F },
			{ "r762", "gun_henry", 360, 8, 10F }, { "r762", "gun_g3", 361, 24, 5F },
			{ "bmg50", "gun_henry", 370, 5, 10F }, { "bmg50", "gun_minigun", 371, 0, 6F }, { "bmg50", "gun_minigun_dual", 371, 0, 6F },
	};

	/** GunFactory.EnumModTest order with the WeaponModOverride damage (12_5F is 125 in the original source). */
	private static final float[] OVERRIDES = { 2.5F, 5F, 7.5F, 10F, 125F, 15F, 20F };

	private static List<BulletConfig> rounds(String caliber) {
		return switch(caliber) {
			case "p9" -> List.of(XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap);
			case "p45" -> List.of(XFactory45.p45_sp, XFactory45.p45_fmj, XFactory45.p45_jhp, XFactory45.p45_ap, XFactory45.p45_du);
			case "p22" -> List.of(XFactory22lr.p22_sp, XFactory22lr.p22_fmj, XFactory22lr.p22_jhp, XFactory22lr.p22_ap);
			case "m357" -> List.of(XFactory357.m357_sp, XFactory357.m357_fmj, XFactory357.m357_jhp, XFactory357.m357_ap, XFactory357.m357_express);
			case "m44" -> List.of(XFactory44.m44_sp, XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express);
			case "r556" -> List.of(XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp, XFactory556mm.r556_ap);
			case "r762" -> List.of(XFactory762mm.r762_sp, XFactory762mm.r762_fmj, XFactory762mm.r762_jhp, XFactory762mm.r762_ap, XFactory762mm.r762_du, XFactory762mm.r762_he);
			default -> List.of(XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_he);
		};
	}

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		int before = problems.size();

		/// Registration: 10 + 18 + 8 flattened ItemEnumMulti variants, stack size 1 ///
		check(problems, cases, HbmWeaponModsG1.TEST_ITEMS.size() == 10 && HbmWeaponModsG1.GENERIC_ITEMS.size() == 18 && HbmWeaponModsG1.CALIBER_ITEMS.size() == 8, "variant counts");
		List<Item> all = new ArrayList<>();
		String[][] families = { HbmWeaponModsG1.TEST, HbmWeaponModsG1.GENERIC, HbmWeaponModsG1.CALIBER };
		String[] prefixes = { "weapon_mod_test_", "weapon_mod_generic_", "weapon_mod_caliber_" };
		for(int f = 0; f < 3; f++) for(String name : families[f]) {
			Item item = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(prefixes[f] + name)).orElse(null);
			check(problems, cases, item != null && new ItemStack(item).getMaxStackSize() == 1, prefixes[f] + name + " registration/stack size");
			if(item != null) all.add(item);
		}
		if(all.size() != 36) return cases[0];

		/// Creative tab: weapon_mod_test hidden; generic then caliber right after ammo_standard (ModItems order) ///
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int lastAmmo = -1;
		for(int i = 0; i < tab.size(); i++) if(tab.get(i).is(HbmItems.AMMO_STANDARD.get())) lastAmmo = i;
		boolean tabOrder = lastAmmo >= 0;
		// after the merge of all mod batches: generic (18) directly after ammo_standard, then weapon_mod_special, then caliber (8)
		for(int i = 0; i < 18 && tabOrder; i++) tabOrder = lastAmmo + 1 + i < tab.size() && tab.get(lastAmmo + 1 + i).is(all.get(10 + i));
		int firstCaliber = -1;
		for(int i = 0; i < tab.size(); i++) if(tab.get(i).is(all.get(28))) { firstCaliber = i; break; }
		tabOrder = tabOrder && firstCaliber >= lastAmmo + 19;
		for(int i = 0; i < 8 && tabOrder; i++) tabOrder = firstCaliber + i < tab.size() && tab.get(firstCaliber + i).is(all.get(28 + i));
		for(int i = lastAmmo + 19; i < firstCaliber && tabOrder; i++) tabOrder = BuiltInRegistries.ITEM.getKey(tab.get(i).getItem()).getPath().startsWith("weapon_mod_special_");
		check(problems, cases, tabOrder, "weapon tab order of weapon_mod_generic/weapon_mod_caliber after ammo_standard (ammo at " + lastAmmo + ")");
		boolean testHidden = true;
		for(int i = 0; i < 10; i++) testHidden &= indexOf(tab, all.get(i)) < 0;
		for(var other : BuiltInRegistries.CREATIVE_MODE_TAB) for(ItemStack s : other.getDisplayItems()) for(int i = 0; i < 10; i++) testHidden &= !s.is(all.get(i));
		check(problems, cases, testHidden, "weapon_mod_test must stay out of the creative tabs");

		/// Definition table: ids, slots, priorities, classes ///
		XWeaponModManager.init();
		ItemStack anyGun = new ItemStack(HbmItems.GUN_G3.get());
		Class<?>[] testClasses = { WeaponModTestFirerate.class, WeaponModTestDamage.class, WeaponModTestMulti.class };
		String[] testSlots = { "FIRERATE", "DAMAGE", "MULTI" };
		for(int i = 0; i < 10; i++) {
			IWeaponMod mod = XWeaponModManager.modFromStack(anyGun, new ItemStack(all.get(i)), 0);
			boolean ok = mod != null && XWeaponModManager.idToMod.get(i) == mod;
			if(ok && i < 3) ok = mod.getClass() == testClasses[i] && Arrays.equals(mod.getSlots(), new String[] { testSlots[i] }) && mod.getModPriority() == WeaponModBase.PRIORITY_MULT_FINAL;
			if(ok && i >= 3) ok = mod.getClass() == WeaponModOverride.class && Arrays.equals(mod.getSlots(), new String[] { "OVERRIDE" }) && mod.getModPriority() == WeaponModBase.PRIORITY_SET;
			check(problems, cases, ok, "weapon_mod_test_" + HbmWeaponModsG1.TEST[i] + " definition id " + i);
		}
		check(problems, cases, WeaponModBase.PRIORITY_SET == 1_000_000 && WeaponModBase.PRIORITY_MULTIPLICATIVE == 1_000 && WeaponModBase.PRIORITY_MULT_FINAL == -1, "priority constants");

		// expected (mod item -> guns); also the complete applicability matrix over every registered gun
		List<Set<Item>> expectedGuns = new ArrayList<>();
		for(int i = 0; i < 36; i++) expectedGuns.add(new LinkedHashSet<>());
		Set<String> missingGuns = new LinkedHashSet<>();
		for(int m = 0; m < GENERIC_GUNS.length; m++) {
			check(problems, cases, HbmWeaponModsG1.GENERIC[m * 2].equals(GENERIC_GUNS[m][0] + "_damage") && HbmWeaponModsG1.GENERIC[m * 2 + 1].equals(GENERIC_GUNS[m][0] + "_dura"), "generic enum order " + GENERIC_GUNS[m][0]);
			for(int g = 1; g < GENERIC_GUNS[m].length; g++) {
				Item gun = gun(GENERIC_GUNS[m][g]);
				if(gun == null) { missingGuns.add(GENERIC_GUNS[m][g]); continue; }
				expectedGuns.get(10 + m * 2).add(gun);
				expectedGuns.get(11 + m * 2).add(gun);
				ItemStack stack = new ItemStack(gun);
				IWeaponMod dmg = XWeaponModManager.modFromStack(stack, new ItemStack(all.get(10 + m * 2)), 0);
				IWeaponMod dura = XWeaponModManager.modFromStack(stack, new ItemStack(all.get(11 + m * 2)), 0);
				check(problems, cases, dmg instanceof WeaponModGenericDamage && XWeaponModManager.idToMod.get(100 + m * 2) == dmg
						&& Arrays.equals(dmg.getSlots(), new String[] { "GENERIC_DAMAGE" }) && dmg.getModPriority() == 1_000
						&& dura instanceof WeaponModGenericDurability && XWeaponModManager.idToMod.get(101 + m * 2) == dura
						&& Arrays.equals(dura.getSlots(), new String[] { "GENERIC_DURABILITY" }) && dura.getModPriority() == 1_000
						&& ((ItemGunBaseNT) gun).recognizedMods.contains(all.get(10 + m * 2)) && ((ItemGunBaseNT) gun).recognizedMods.contains(all.get(11 + m * 2)),
						"generic " + GENERIC_GUNS[m][0] + " definition on " + GENERIC_GUNS[m][g]);
				genericValues(problems, cases, GENERIC_GUNS[m][0], GENERIC_GUNS[m][g], stack, all.get(10 + m * 2), all.get(11 + m * 2), 100 + m * 2);
			}
		}
		for(Object[] row : CALIBER_MODS) {
			Item gun = gun((String) row[1]);
			if(gun == null) { missingGuns.add((String) row[1]); continue; }
			int modIndex = 28 + Arrays.asList(HbmWeaponModsG1.CALIBER).indexOf(row[0]);
			expectedGuns.get(modIndex).add(gun);
			caliberValues(problems, cases, row, gun, all.get(modIndex));
		}
		check(problems, cases, missingGuns.isEmpty(), "guns of the original table absent in the port: " + missingGuns + " (expected none; [parity d2] gun_minigun_dual is ported)");

		int gunCount = 0;
		for(Item item : BuiltInRegistries.ITEM) {
			if(!(item instanceof ItemGunBaseNT)) continue;
			gunCount++;
			ItemStack stack = new ItemStack(item);
			StringBuilder wrong = new StringBuilder();
			for(int i = 0; i < 36; i++) {
				boolean expected = i < 10 || expectedGuns.get(i).contains(item);
				if(XWeaponModManager.isApplicable(stack, new ItemStack(all.get(i)), 0, false) != expected) wrong.append(' ').append(BuiltInRegistries.ITEM.getKey(all.get(i)).getPath());
			}
			check(problems, cases, wrong.length() == 0, "applicability matrix of " + BuiltInRegistries.ITEM.getKey(item).getPath() + ":" + wrong);
		}
		check(problems, cases, gunCount > 0, "no guns registered");

		/// weapon_mod_test values ///
		ItemGunBaseNT grease = (ItemGunBaseNT) HbmItems.GUN_GREASEGUN.get();
		ItemStack plain = new ItemStack(grease);
		Receiver plainRec = grease.getConfig(plain, 0).getReceivers(plain)[0];
		float baseDmg = plainRec.getBaseDamage(plain);
		int baseDelay = plainRec.getDelayAfterFire(plain);
		int baseRounds = plainRec.getRoundsPerCycle(plain);
		float baseDura = grease.getConfig(plain, 0).getDurability(plain);
		check(problems, cases, baseDmg == 3F && baseDelay == 4 && baseRounds == 1 && baseDura == 3000F, "greasegun base values " + baseDmg + "/" + baseDelay + "/" + baseRounds + "/" + baseDura);

		ItemStack g = installed(grease, all.get(0));
		Receiver r = grease.getConfig(g, 0).getReceivers(g)[0];
		check(problems, cases, r.getDelayAfterFire(g) == 2 && r.getBaseDamage(g) == baseDmg && r.getRoundsPerCycle(g) == 1, "FIRERATE: delay 4 -> 2");
		ItemStack m2Gun = installed(HbmItems.GUN_M2.get(), all.get(0));
		Receiver m2 = ((ItemGunBaseNT) m2Gun.getItem()).getConfig(m2Gun, 0).getReceivers(m2Gun)[0];
		ItemStack m2Plain = new ItemStack(HbmItems.GUN_M2.get());
		int m2Delay = ((ItemGunBaseNT) m2Plain.getItem()).getConfig(m2Plain, 0).getReceivers(m2Plain)[0].getDelayAfterFire(m2Plain);
		check(problems, cases, m2.getDelayAfterFire(m2Gun) == Math.max(m2Delay / 2, 1), "FIRERATE on gun_m2: delay " + m2Delay + " -> " + m2.getDelayAfterFire(m2Gun));
		g = installed(grease, all.get(1));
		r = grease.getConfig(g, 0).getReceivers(g)[0];
		check(problems, cases, r.getBaseDamage(g) == 3F * 1.5F && r.getDelayAfterFire(g) == 4, "DAMAGE: 3 -> 4.5");
		g = installed(grease, all.get(2));
		r = grease.getConfig(g, 0).getReceivers(g)[0];
		check(problems, cases, r.getRoundsPerCycle(g) == 3 && r.getBaseDamage(g) == 3F, "MULTI: rounds per cycle 1 -> 3");
		for(int i = 0; i < OVERRIDES.length; i++) {
			g = installed(grease, all.get(3 + i));
			r = grease.getConfig(g, 0).getReceivers(g)[0];
			check(problems, cases, r.getBaseDamage(g) == OVERRIDES[i] && XWeaponModManager.hasUpgrade(g, 0, 3 + i) && grease.getConfig(g, 0).getDurability(g) == 3000F,
					"weapon_mod_test_" + HbmWeaponModsG1.TEST[3 + i] + ": damage " + r.getBaseDamage(g) + " expected " + OVERRIDES[i]);
			check(problems, cases, !XWeaponModManager.isApplicable(g, new ItemStack(all.get(3 + (i + 1) % OVERRIDES.length)), 0, true)
					&& XWeaponModManager.isApplicable(g, new ItemStack(all.get(1)), 0, true), "OVERRIDE slot mutex " + i);
		}
		g = installed(grease, all.get(0));
		check(problems, cases, !XWeaponModManager.isApplicable(g, new ItemStack(all.get(0)), 0, true) && XWeaponModManager.isApplicable(g, new ItemStack(all.get(1)), 0, true)
				&& XWeaponModManager.isApplicable(g, new ItemStack(all.get(2)), 0, true), "FIRERATE/DAMAGE/MULTI use separate slots");

		// Priority order: SET (override 10) -> MULTIPLICATIVE (steel damage) -> MULT_FINAL (test damage, test firerate), regardless of slot order
		g = new ItemStack(grease);
		XWeaponModManager.install(g, 0, new ItemStack(all.get(1)), new ItemStack(all.get(13)), new ItemStack(all.get(12)), new ItemStack(all.get(0)), new ItemStack(all.get(6)));
		int[] ids = ItemGunBaseNT.readTag(g) == null ? new int[0] : ItemGunBaseNT.readTag(g).getIntArray(XWeaponModManager.KEY_MOD_LIST + 0);
		r = grease.getConfig(g, 0).getReceivers(g)[0];
		check(problems, cases, ids.length == 5 && ids[0] == 6 && Set.of(ids[1], ids[2]).equals(Set.of(102, 103)) && Set.of(ids[3], ids[4]).equals(Set.of(0, 1)), "install order by priority " + Arrays.toString(ids));
		check(problems, cases, r.getBaseDamage(g) == 10F * 1.15F * 1.5F && r.getDelayAfterFire(g) == 2 && grease.getConfig(g, 0).getDurability(g) == 6000F,
				"stacked values: damage " + r.getBaseDamage(g) + " delay " + r.getDelayAfterFire(g) + " dura " + grease.getConfig(g, 0).getDurability(g));
		ItemStack[] back = XWeaponModManager.getUpgradeItems(g, 0);
		check(problems, cases, back.length == 5 && back[0].is(all.get(6)), "getUpgradeItems returns the mod items");
		XWeaponModManager.uninstall(g, 0);
		r = grease.getConfig(g, 0).getReceivers(g)[0];
		check(problems, cases, r.getBaseDamage(g) == 3F && r.getDelayAfterFire(g) == 4 && grease.getConfig(g, 0).getDurability(g) == 3000F && XWeaponModManager.getUpgradeItems(g, 0).length == 0, "uninstall restores the base values");

		/// Slot mutex across families (gun_henry: steel generic + calibers) ///
		ItemStack henry = installed(HbmItems.GUN_HENRY.get(), all.get(12));
		check(problems, cases, !XWeaponModManager.isApplicable(henry, new ItemStack(all.get(12)), 0, true) && XWeaponModManager.isApplicable(henry, new ItemStack(all.get(13)), 0, true)
				&& XWeaponModManager.isApplicable(henry, new ItemStack(all.get(28)), 0, true) && !XWeaponModManager.isApplicable(henry, new ItemStack(all.get(14)), 0, true), "GENERIC_DAMAGE mutex on gun_henry");
		henry = installed(HbmItems.GUN_HENRY.get(), all.get(28));
		boolean caliberMutex = XWeaponModManager.isApplicable(henry, new ItemStack(all.get(12)), 0, true) && XWeaponModManager.isApplicable(henry, new ItemStack(all.get(3)), 0, true);
		for(int i = 28; i < 36; i++) if(expectedGuns.get(i).contains(HbmItems.GUN_HENRY.get())) caliberMutex &= !XWeaponModManager.isApplicable(henry, new ItemStack(all.get(i)), 0, true);
		check(problems, cases, caliberMutex, "CALIBER mutex on gun_henry");

		/// XFactory45 rounds ///
		BulletConfig[] p45 = { XFactory45.p45_sp, XFactory45.p45_fmj, XFactory45.p45_jhp, XFactory45.p45_ap, XFactory45.p45_du };
		StandardAmmoType[] p45Types = { StandardAmmoType.P45_SP, StandardAmmoType.P45_FMJ, StandardAmmoType.P45_JHP, StandardAmmoType.P45_AP, StandardAmmoType.P45_DU };
		float[] p45Damage = { 1F, 0.8F, 1.5F, 1.25F, 2.5F };
		for(int i = 0; i < 5; i++) {
			check(problems, cases, p45[i].ammo == p45Types[i] && p45[i].damageMult() == p45Damage[i] && p45[i].wear == 1F && p45[i].ammoReloadCount == 1
					&& p45[i].casingEffect == (i < 3 ? GunEffects.CASING_P9 : GunEffects.CASING_P9_STEEL) && BulletConfig.configs.contains(p45[i])
					&& p45[i].matches(stack(p45Types[i], 1)) && !p45[i].matches(stack(StandardAmmoType.P9_SP, 1)), "XFactory45 " + p45Types[i].id());
		}

		/// Caliber change in use: gun_greasegun + P45 (id 311: 24 rounds, damage 3) ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.getInventory().clearContent();
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);
		player.getInventory().setItem(0, new ItemStack(grease));
		ItemStack held = player.getInventory().getItem(0);
		tick(player, held, 1);
		tick(player, held, grease.getConfig(held, 0).getDrawDuration(held) + 1);
		MagazineSingleTypeBase baseMag = (MagazineSingleTypeBase) grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		baseMag.setType(held, XFactory9mm.p9_sp);
		baseMag.setAmount(held, 30);
		// a mod that leaves the magazine alone keeps the loaded rounds
		XWeaponModManager.install(held, 0, new ItemStack(all.get(12)));
		IMagazine<?> mag = grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		check(problems, cases, mag.getAmount(held, null) == 30 && mag.getType(held, null) == XFactory9mm.p9_sp, "generic mod keeps the loaded rounds");
		Item p45Kit = all.get(29);
		XWeaponModManager.install(held, 0, new ItemStack(all.get(12)), new ItemStack(p45Kit));
		mag = grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		check(problems, cases, mag.getAmount(held, null) == 0 && mag.getCapacity(held) == 24 && XWeaponModManager.hasUpgrade(held, 0, 311) && XWeaponModManager.hasUpgrade(held, 0, 102),
				"caliber install clears the magazine: amount " + mag.getAmount(held, null) + " capacity " + mag.getCapacity(held));
		// 9mm is no longer accepted
		player.getInventory().setItem(1, stack(StandardAmmoType.P9_SP, 64));
		press(player, held, GunKeybind.RELOAD);
		release(player, held, GunKeybind.RELOAD);
		tick(player, held, 70);
		mag = grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		check(problems, cases, mag.getAmount(held, null) == 0 && player.getInventory().getItem(1).getCount() == 64, "9mm rounds rejected after the P45 conversion");
		player.getInventory().setItem(2, stack(StandardAmmoType.P45_FMJ, 40));
		press(player, held, GunKeybind.RELOAD);
		release(player, held, GunKeybind.RELOAD);
		tick(player, held, 70);
		mag = grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		check(problems, cases, mag.getAmount(held, null) == 24 && mag.getType(held, null) == XFactory45.p45_fmj && player.getInventory().getItem(2).getCount() == 16
				&& player.getInventory().getItem(1).getCount() == 64, "P45 reload: amount " + mag.getAmount(held, null) + " left " + player.getInventory().getItem(2).getCount());
		discard(level);
		press(player, held, GunKeybind.GUN_PRIMARY);
		release(player, held, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
		mag = grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		// calcDamage: base 3 (caliber SET) * 1.15 (steel receiver) * 0.8 (p45_fmj)
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.P45_FMJ && Math.abs(bullets.get(0).baseDamage() - 3F * 1.15F * 0.8F) < 0.0001F
				&& mag.getAmount(held, null) == 23, "P45 shot: bullets " + bullets.size() + (bullets.isEmpty() ? "" : " type " + bullets.get(0).ammoType().id() + " damage " + bullets.get(0).baseDamage()) + " mag " + mag.getAmount(held, null));
		tick(player, held, 10);
		discard(level);
		// removing the kit and installing again clears the magazine (changedMagState)
		XWeaponModManager.install(held, 0, new ItemStack(all.get(12)));
		mag = grease.getConfig(held, 0).getReceivers(held)[0].getMagazine(held);
		check(problems, cases, !XWeaponModManager.hasUpgrade(held, 0, 311) && mag.getCapacity(held) == 30 && mag.getAmount(held, null) == 0
				&& grease.getConfig(held, 0).getReceivers(held)[0].getBaseDamage(held) == 3F * 1.15F, "caliber removal restores the 9mm magazine and clears it: amount " + mag.getAmount(held, null));
		player.getInventory().clearContent();

		/// Weapon modification table: install and removal of every mod ///
		for(int i = 0; i < 36; i++) {
			Item gun = i < 10 ? HbmItems.GUN_G3.get() : expectedGuns.get(i).iterator().next();
			int id = i < 10 ? i : i < 28 ? 100 + (i - 10) : -1;
			if(i >= 28) for(Object[] row : CALIBER_MODS) if(row[0].equals(HbmWeaponModsG1.CALIBER[i - 28]) && gun(((String) row[1])) == gun) id = (int) row[2];
			String name = BuiltInRegistries.ITEM.getKey(all.get(i)).getPath();
			ItemStack mod = new ItemStack(all.get(i));
			WeaponTableMenu menu = new WeaponTableMenu(0, player.getInventory());
			check(problems, cases, !menu.getSlot(0).mayPlace(mod), "table: " + name + " accepted without a gun");
			menu.getSlot(7).set(new ItemStack(gun));
			check(problems, cases, menu.getSlot(0).mayPlace(mod), "table: " + name + " rejected for " + BuiltInRegistries.ITEM.getKey(gun).getPath());
			menu.getSlot(0).set(mod.copy());
			ItemStack inTable = menu.getSlot(7).getItem();
			check(problems, cases, XWeaponModManager.hasUpgrade(inTable, 0, id) && !menu.getSlot(1).mayPlace(mod), "table: " + name + " install id " + id + " / mutex");
			menu.getSlot(7).set(ItemStack.EMPTY);
			menu.getSlot(7).onTake(player, inTable);
			check(problems, cases, XWeaponModManager.hasUpgrade(inTable, 0, id) && menu.mods.getItem(0).isEmpty(), "table: " + name + " consumed when the gun is taken");
			menu.getSlot(7).set(inTable);
			check(problems, cases, menu.mods.getItem(0).is(all.get(i)) && menu.mods.getItem(1).isEmpty(), "table: " + name + " shown for the modified gun");
			ItemStack taken = menu.getSlot(0).remove(1);
			menu.getSlot(0).onTake(player, taken);
			check(problems, cases, taken.is(all.get(i)) && taken.getCount() == 1 && !XWeaponModManager.hasUpgrade(menu.gun.getItem(0), 0, id)
					&& XWeaponModManager.getUpgradeItems(menu.gun.getItem(0), 0).length == 0, "table: " + name + " removal");
			menu.gun.clearContent();
			menu.mods.clearContent();
		}
		// two generic mods and a caliber kit side by side, wrong material rejected
		WeaponTableMenu menu = new WeaponTableMenu(0, player.getInventory());
		menu.getSlot(7).set(new ItemStack(HbmItems.GUN_HENRY.get()));
		menu.getSlot(0).set(new ItemStack(all.get(12)));
		check(problems, cases, menu.getSlot(1).mayPlace(new ItemStack(all.get(13))) && menu.getSlot(1).mayPlace(new ItemStack(all.get(33))) && !menu.getSlot(1).mayPlace(new ItemStack(all.get(10)))
				&& !menu.getSlot(1).mayPlace(new ItemStack(all.get(32))), "table: slot validation on gun_henry");
		menu.getSlot(1).set(new ItemStack(all.get(13)));
		menu.getSlot(2).set(new ItemStack(all.get(33)));
		ItemStack tableHenry = menu.gun.getItem(0);
		ItemGunBaseNT henryItem = (ItemGunBaseNT) tableHenry.getItem();
		ItemStack henryPlain = new ItemStack(henryItem);
		check(problems, cases, XWeaponModManager.hasUpgrade(tableHenry, 0, 102) && XWeaponModManager.hasUpgrade(tableHenry, 0, 103) && XWeaponModManager.hasUpgrade(tableHenry, 0, 350)
				&& henryItem.getConfig(tableHenry, 0).getReceivers(tableHenry)[0].getBaseDamage(tableHenry) == 10F * 1.15F
				&& henryItem.getConfig(tableHenry, 0).getDurability(tableHenry) == henryItem.getConfig(henryPlain, 0).getDurability(henryPlain) * 2F
				&& henryItem.getConfig(tableHenry, 0).getReceivers(tableHenry)[0].getMagazine(tableHenry).getCapacity(tableHenry) == 10, "table: three mods on gun_henry");
		menu.removed(player);
		check(problems, cases, menu.gun.isEmpty() && menu.mods.isEmpty() && XWeaponModManager.getUpgradeItems(tableHenry, 0).length == 0, "table: closing uninstalls and drops");
		for(var drop : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AREA)) drop.discard();
		player.getInventory().clearContent();
		discard(level);

		/// Recipes (WeaponRecipes lines 120-137, addShapelessAuto); test and caliber mods have none ///
		recipe(problems, cases, server, level, "iron_damage", List.of("ingot_gunmetal"), List.of("minecraft:iron_ingot"), 3);
		recipe(problems, cases, server, level, "iron_dura", List.of("ingot_gunmetal"), List.of("minecraft:iron_ingot"), 1);
		Object[][] materials = {
				{ "steel", "gunmetal", List.of("plate_cast_steel") },
				{ "dura", "gunmetal", List.of("plate_cast_dura_steel") },
				{ "desh", "gunmetal", List.of("plate_cast_desh") },
				{ "wsteel", "weaponsteel", List.of("plate_cast_weaponsteel") },
				{ "ferro", "weaponsteel", List.of("plate_cast_ferrouranium") },
				{ "tcalloy", "weaponsteel", List.of("plate_cast_tcalloy", "plate_cast_cdalloy") },
				{ "bigmt", "saturnite", List.of("plate_cast_saturnite") },
				{ "bronze", "saturnite", List.of("plate_cast_bismuth_bronze", "plate_cast_arsenic_bronze") },
		};
		for(Object[] row : materials) {
			@SuppressWarnings("unchecked")
			List<String> casts = (List<String>) row[2];
			recipe(problems, cases, server, level, row[0] + "_damage", List.of("part_mechanism_" + row[1]), casts, 3);
			recipe(problems, cases, server, level, row[0] + "_dura", List.of("plate_" + row[1]), casts, 1);
		}
		boolean noRecipes = true;
		for(int i = 0; i < 36; i++) if(i < 10 || i >= 28) {
			for(var holder : server.getRecipeManager().getRecipes()) noRecipes &= !holder.value().getResultItem(server.registryAccess()).is(all.get(i));
		}
		check(problems, cases, noRecipes, "weapon_mod_test/weapon_mod_caliber have no recipes in 1.7.10");

		AtomicLegacy.LOGGER.info("HBM_GUN_MODS_G1_AUDIT cases={} problems={}", cases[0], problems.size() - before);
		return cases[0];
	}

	/** WeaponModGenericDamage (x1.15 on every receiver) and WeaponModGenericDurability (x2) on one gun. */
	private static void genericValues(List<String> problems, int[] cases, String material, String gunName, ItemStack plain, Item damageMod, Item duraMod, int damageId) {
		ItemGunBaseNT gun = (ItemGunBaseNT) plain.getItem();
		GunConfig cfg = gun.getConfig(plain, 0);
		Receiver[] recs = cfg.getReceivers(plain);
		float[] dmg = new float[recs.length];
		for(int i = 0; i < recs.length; i++) dmg[i] = recs[i].getBaseDamage(plain);
		float dura = cfg.getDurability(plain);

		ItemStack a = installed(gun, damageMod);
		Receiver[] ra = gun.getConfig(a, 0).getReceivers(a);
		boolean ok = ra.length == recs.length && gun.getConfig(a, 0).getDurability(a) == dura && XWeaponModManager.hasUpgrade(a, 0, damageId) && !XWeaponModManager.hasUpgrade(a, 0, damageId + 1);
		for(int i = 0; ok && i < ra.length; i++) ok = ra[i].getBaseDamage(a) == dmg[i] * 1.15F;
		check(problems, cases, ok, "generic " + material + "_damage on " + gunName + ": damage " + (ra.length > 0 ? ra[0].getBaseDamage(a) : -1F) + " base " + (dmg.length > 0 ? dmg[0] : -1F));

		ItemStack b = installed(gun, duraMod);
		Receiver[] rb = gun.getConfig(b, 0).getReceivers(b);
		ok = gun.getConfig(b, 0).getDurability(b) == dura * 2F && XWeaponModManager.hasUpgrade(b, 0, damageId + 1) && !XWeaponModManager.hasUpgrade(b, 0, damageId);
		for(int i = 0; ok && i < rb.length; i++) ok = rb[i].getBaseDamage(b) == dmg[i];
		check(problems, cases, ok, "generic " + material + "_dura on " + gunName + ": durability " + gun.getConfig(b, 0).getDurability(b) + " base " + dura);

		check(problems, cases, !XWeaponModManager.isApplicable(a, new ItemStack(damageMod), 0, true) && XWeaponModManager.isApplicable(a, new ItemStack(duraMod), 0, true)
				&& !XWeaponModManager.isApplicable(b, new ItemStack(duraMod), 0, true) && XWeaponModManager.isApplicable(b, new ItemStack(damageMod), 0, true), "generic " + material + " slot mutex on " + gunName);

		XWeaponModManager.install(a, 0, new ItemStack(duraMod), new ItemStack(damageMod));
		Receiver[] rc = gun.getConfig(a, 0).getReceivers(a);
		ok = gun.getConfig(a, 0).getDurability(a) == dura * 2F;
		for(int i = 0; ok && i < rc.length; i++) ok = rc[i].getBaseDamage(a) == dmg[i] * 1.15F;
		check(problems, cases, ok, "generic " + material + " damage + dura on " + gunName);
	}

	/** WeaponModCaliber: accepted rounds, capacity, base damage, magazine class kept. */
	private static void caliberValues(List<String> problems, int[] cases, Object[] row, Item gunItem, Item modItem) {
		String label = "caliber " + row[0] + " on " + row[1];
		ItemGunBaseNT gun = (ItemGunBaseNT) gunItem;
		ItemStack plain = new ItemStack(gun);
		IMagazine<?> baseMag = gun.getConfig(plain, 0).getReceivers(plain)[0].getMagazine(plain);
		float dura = gun.getConfig(plain, 0).getDurability(plain);
		int delay = gun.getConfig(plain, 0).getReceivers(plain)[0].getDelayAfterFire(plain);
		IWeaponMod mod = XWeaponModManager.modFromStack(plain, new ItemStack(modItem), 0);
		check(problems, cases, mod instanceof WeaponModCaliber && XWeaponModManager.idToMod.get((int) row[2]) == mod && Arrays.equals(mod.getSlots(), new String[] { "CALIBER" })
				&& mod.getModPriority() == 1_000_000 && gun.recognizedMods.contains(modItem), label + " definition id " + row[2]);
		ItemStack s = installed(gun, modItem);
		Receiver rec = gun.getConfig(s, 0).getReceivers(s)[0];
		IMagazine<?> mag = rec.getMagazine(s);
		List<BulletConfig> accepted = mag instanceof MagazineSingleTypeBase single ? new ArrayList<>(single.acceptedBullets) : mag instanceof MagazineBelt belt ? new ArrayList<>(belt.acceptedBullets) : List.of();
		boolean sameKind = (baseMag instanceof MagazineSingleReload && mag instanceof MagazineSingleReload) || (baseMag instanceof MagazineFullReload && mag instanceof MagazineFullReload)
				|| (baseMag instanceof MagazineBelt && mag instanceof MagazineBelt);
		boolean capacity = mag instanceof MagazineBelt ? (int) row[3] == 0 : mag.getCapacity(s) == (int) row[3];
		boolean index = !(mag instanceof MagazineSingleTypeBase single) || single.index == ((MagazineSingleTypeBase) baseMag).index;
		check(problems, cases, XWeaponModManager.hasUpgrade(s, 0, (int) row[2]) && sameKind && capacity && index && accepted.equals(rounds((String) row[0])) && rec.getBaseDamage(s) == (float) row[4]
				&& gun.getConfig(s, 0).getDurability(s) == dura && rec.getDelayAfterFire(s) == delay,
				label + ": kind " + mag.getClass().getSimpleName() + "/" + baseMag.getClass().getSimpleName() + " capacity " + mag.getCapacity(s) + " damage " + rec.getBaseDamage(s) + " rounds " + accepted.size());
		XWeaponModManager.uninstall(s, 0);
		IMagazine<?> after = gun.getConfig(s, 0).getReceivers(s)[0].getMagazine(s);
		check(problems, cases, after == baseMag && after.getCapacity(s) == baseMag.getCapacity(plain)
				&& gun.getConfig(s, 0).getReceivers(s)[0].getBaseDamage(s) == gun.getConfig(plain, 0).getReceivers(plain)[0].getBaseDamage(plain), label + " uninstall");
	}

	private static void recipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String variant, List<String> first, List<String> repeated, int repeat) {
		String id = "weapon_mod_generic_" + variant;
		Item result = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).orElse(null);
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(holder -> holder.value()).orElse(null);
		if(!(recipe instanceof ShapelessRecipe shapeless) || result == null) {
			check(problems, cases, false, id + " shapeless recipe missing");
			return;
		}
		boolean ok = shapeless.getIngredients().size() == repeat + 2;
		for(String a : first) for(String b : repeated) {
			List<ItemStack> grid = new ArrayList<>();
			grid.add(item(a));
			for(int i = 0; i < repeat; i++) grid.add(item(b));
			grid.add(item("ducttape"));
			boolean present = grid.stream().noneMatch(s -> s.isEmpty());
			CraftingInput input = CraftingInput.of(grid.size(), 1, grid);
			ok &= present && shapeless.matches(input, level) && shapeless.assemble(input, server.registryAccess()).is(result) && shapeless.assemble(input, server.registryAccess()).getCount() == 1;
			// one ingredient short or a foreign plate must not craft
			List<ItemStack> shorter = new ArrayList<>(grid.subList(0, grid.size() - 1));
			ok &= !shapeless.matches(CraftingInput.of(shorter.size(), 1, shorter), level);
			List<ItemStack> foreign = new ArrayList<>(grid);
			foreign.set(1, item("plate_cast_lead"));
			ok &= !shapeless.matches(CraftingInput.of(foreign.size(), 1, foreign), level);
		}
		check(problems, cases, ok, id + " recipe (" + first + " + " + repeat + "x " + repeated + " + ducttape)");
	}

	private static ItemStack installed(Item gun, Item mod) {
		ItemStack stack = new ItemStack(gun);
		XWeaponModManager.install(stack, 0, new ItemStack(mod));
		return stack;
	}

	private static Item gun(String id) {
		Item item = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).orElse(null);
		return item instanceof ItemGunBaseNT ? item : null;
	}

	private static ItemStack item(String id) {
		ResourceLocation key = id.contains(":") ? ResourceLocation.parse(id) : AtomicLegacy.id(id);
		return BuiltInRegistries.ITEM.getOptional(key).map(Item::getDefaultInstance).orElse(ItemStack.EMPTY);
	}

	private static ItemStack stack(StandardAmmoType type, int count) {
		return StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), type).copyWithCount(count);
	}

	private static void press(FakePlayer player, ItemStack gun, GunKeybind key) { ((ItemGunBaseNT) gun.getItem()).handleKeybind(player, gun, key, true); }
	private static void release(FakePlayer player, ItemStack gun, GunKeybind key) { ((ItemGunBaseNT) gun.getItem()).handleKeybind(player, gun, key, false); }

	private static void tick(FakePlayer player, ItemStack stack, int ticks) {
		for(int i = 0; i < ticks; i++) stack.getItem().inventoryTick(stack, player.level(), player, 0, true);
	}

	private static void discard(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("weapon mods G1: " + message);
	}
}
