package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory22lr;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory357;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory44;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory45;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory50;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory9mm;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager.WeaponModDefinition;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmWeaponModsG1;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 XWeaponModManager.init(): the weapon_mod_test (ids 0-9), weapon_mod_generic (ids 100-117) and
 * weapon_mod_caliber (ids 300-371) definitions with the original ids, gun lists and values.
 * [parity d2] gun_minigun_dual is ported and listed for R556 (352) and BMG50 (371).
 */
public final class XWeaponModsG1 {

	private XWeaponModsG1() {
	}

	private static WeaponModDefinition def(Item mod) {
		return new WeaponModDefinition(new ItemStack(mod));
	}

	/** XWeaponModManager.init(), "TEST" block and the weapon_mod_generic block (registered before weapon_mod_special). */
	public static void registerTestAndGeneric() {
		/// TEST ///
		IWeaponMod TEST_FIRERATE = new WeaponModTestFirerate(0, "FIRERATE");
		IWeaponMod TEST_DAMAGE = new WeaponModTestDamage(1, "DAMAGE");
		IWeaponMod TEST_MULTI = new WeaponModTestMulti(2, "MULTI");

		def(HbmWeaponModsG1.test("firerate").get()).addDefault(TEST_FIRERATE);
		def(HbmWeaponModsG1.test("damage").get()).addDefault(TEST_DAMAGE);
		def(HbmWeaponModsG1.test("multi").get()).addDefault(TEST_MULTI);

		def(HbmWeaponModsG1.test("override_2_5").get()).addDefault(new WeaponModOverride(3, 2.5F, "OVERRIDE"));
		def(HbmWeaponModsG1.test("override_5").get()).addDefault(new WeaponModOverride(4, 5F, "OVERRIDE"));
		def(HbmWeaponModsG1.test("override_7_5").get()).addDefault(new WeaponModOverride(5, 7.5F, "OVERRIDE"));
		def(HbmWeaponModsG1.test("override_10").get()).addDefault(new WeaponModOverride(6, 10F, "OVERRIDE"));
		// the original literal is 12_5F, i.e. 125, not 12.5
		def(HbmWeaponModsG1.test("override_12_5").get()).addDefault(new WeaponModOverride(7, 12_5F, "OVERRIDE"));
		def(HbmWeaponModsG1.test("override_15").get()).addDefault(new WeaponModOverride(8, 15F, "OVERRIDE"));
		def(HbmWeaponModsG1.test("override_20").get()).addDefault(new WeaponModOverride(9, 20F, "OVERRIDE"));

		def(HbmWeaponModsG1.generic("iron_damage").get()).addMod(HbmItems.GUN_PEPPERBOX.get(), new WeaponModGenericDamage(100));
		def(HbmWeaponModsG1.generic("iron_dura").get()).addMod(HbmItems.GUN_PEPPERBOX.get(), new WeaponModGenericDurability(101));

		Item[] steelGuns = new Item[] {
				HbmItems.GUN_LIGHT_REVOLVER.get(), HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get(),
				HbmItems.GUN_HENRY.get(), HbmItems.GUN_HENRY_LINCOLN.get(),
				HbmItems.GUN_GREASEGUN.get(),
				HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_MARESLEG_AKIMBO.get(),
				HbmItems.GUN_FLAREGUN.get() };
		Item[] duraGuns = new Item[] {
				HbmItems.GUN_AM180.get(),
				HbmItems.GUN_LIBERATOR.get(),
				HbmItems.GUN_CONGOLAKE.get(),
				HbmItems.GUN_FLAMER.get(),
				HbmItems.GUN_FLAMER_TOPAZ.get() };
		Item[] deshGuns = new Item[] {
				HbmItems.GUN_HEAVY_REVOLVER.get(),
				HbmItems.GUN_CARBINE.get(),
				HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(),
				HbmItems.GUN_SPAS12.get(),
				HbmItems.GUN_PANZERSCHRECK.get() };
		Item[] wsteelGuns = new Item[] {
				HbmItems.GUN_STAR_F.get(), HbmItems.GUN_STAR_F_AKIMBO.get(),
				HbmItems.GUN_G3.get(), HbmItems.GUN_G3_ZEBRA.get(),
				HbmItems.GUN_MK108.get(),
				HbmItems.GUN_CHEMTHROWER.get() };
		Item[] ferroGuns = new Item[] {
				HbmItems.GUN_AMAT.get(),
				HbmItems.GUN_M2.get(),
				HbmItems.GUN_AUTOSHOTGUN.get(), HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(),
				HbmItems.GUN_QUADRO.get() };
		Item[] tcalloyGuns = new Item[] {
				HbmItems.GUN_LAG.get(),
				HbmItems.GUN_MINIGUN.get(),
				HbmItems.GUN_MISSILE_LAUNCHER.get(),
				HbmItems.GUN_TESLA_CANNON.get() };
		Item[] bigmtGuns = new Item[] {
				HbmItems.GUN_LASER_PISTOL.get(), HbmItems.GUN_LASER_PISTOL_PEW_PEW.get(),
				HbmItems.GUN_STG77.get(),
				HbmItems.GUN_FATMAN.get(),
				HbmItems.GUN_TAU.get() };
		Item[] bronzeGuns = new Item[] {
				HbmItems.GUN_LASRIFLE.get() };

		def(HbmWeaponModsG1.generic("steel_damage").get()).addMod(steelGuns, new WeaponModGenericDamage(102));
		def(HbmWeaponModsG1.generic("steel_dura").get()).addMod(steelGuns, new WeaponModGenericDurability(103));
		def(HbmWeaponModsG1.generic("dura_damage").get()).addMod(duraGuns, new WeaponModGenericDamage(104));
		def(HbmWeaponModsG1.generic("dura_dura").get()).addMod(duraGuns, new WeaponModGenericDurability(105));
		def(HbmWeaponModsG1.generic("desh_damage").get()).addMod(deshGuns, new WeaponModGenericDamage(106));
		def(HbmWeaponModsG1.generic("desh_dura").get()).addMod(deshGuns, new WeaponModGenericDurability(107));
		def(HbmWeaponModsG1.generic("wsteel_damage").get()).addMod(wsteelGuns, new WeaponModGenericDamage(108));
		def(HbmWeaponModsG1.generic("wsteel_dura").get()).addMod(wsteelGuns, new WeaponModGenericDurability(109));
		def(HbmWeaponModsG1.generic("ferro_damage").get()).addMod(ferroGuns, new WeaponModGenericDamage(110));
		def(HbmWeaponModsG1.generic("ferro_dura").get()).addMod(ferroGuns, new WeaponModGenericDurability(111));
		def(HbmWeaponModsG1.generic("tcalloy_damage").get()).addMod(tcalloyGuns, new WeaponModGenericDamage(112));
		def(HbmWeaponModsG1.generic("tcalloy_dura").get()).addMod(tcalloyGuns, new WeaponModGenericDurability(113));
		def(HbmWeaponModsG1.generic("bigmt_damage").get()).addMod(bigmtGuns, new WeaponModGenericDamage(114));
		def(HbmWeaponModsG1.generic("bigmt_dura").get()).addMod(bigmtGuns, new WeaponModGenericDurability(115));
		def(HbmWeaponModsG1.generic("bronze_damage").get()).addMod(bronzeGuns, new WeaponModGenericDamage(116));
		def(HbmWeaponModsG1.generic("bronze_dura").get()).addMod(bronzeGuns, new WeaponModGenericDurability(117));
	}

	/** XWeaponModManager.init(), weapon_mod_caliber block (registered after weapon_mod_special). */
	public static void registerCaliber() {
		BulletConfig[] p9 = new BulletConfig[] {XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap};
		BulletConfig[] p45 = new BulletConfig[] {XFactory45.p45_sp, XFactory45.p45_fmj, XFactory45.p45_jhp, XFactory45.p45_ap, XFactory45.p45_du};
		BulletConfig[] p22 = new BulletConfig[] {XFactory22lr.p22_sp, XFactory22lr.p22_fmj, XFactory22lr.p22_jhp, XFactory22lr.p22_ap};
		BulletConfig[] m357 = new BulletConfig[] {XFactory357.m357_sp, XFactory357.m357_fmj, XFactory357.m357_jhp, XFactory357.m357_ap, XFactory357.m357_express};
		BulletConfig[] m44 = new BulletConfig[] {XFactory44.m44_sp, XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express};
		BulletConfig[] r556 = new BulletConfig[] {XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp, XFactory556mm.r556_ap};
		BulletConfig[] r762 = new BulletConfig[] {XFactory762mm.r762_sp, XFactory762mm.r762_fmj, XFactory762mm.r762_jhp, XFactory762mm.r762_ap, XFactory762mm.r762_du, XFactory762mm.r762_he};
		BulletConfig[] bmg50 = new BulletConfig[] {XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_he};
		def(HbmWeaponModsG1.caliber("p9").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(300, 28, 10F, p9))
			.addMod(HbmItems.GUN_STAR_F.get(), new WeaponModCaliber(301, 12, 15F, p9))
			.addMod(HbmItems.GUN_STAR_F_AKIMBO.get(), new WeaponModCaliber(302, 12, 15F, p9));
		def(HbmWeaponModsG1.caliber("p45").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(310, 28, 10F, p45))
			.addMod(HbmItems.GUN_GREASEGUN.get(), new WeaponModCaliber(311, 24, 3F, p45))
			.addMod(HbmItems.GUN_UZI.get(), new WeaponModCaliber(312, 24, 3F, p45))
			.addMod(HbmItems.GUN_UZI_AKIMBO.get(), new WeaponModCaliber(313, 24, 3F, p45))
			.addMod(HbmItems.GUN_LAG.get(), new WeaponModCaliber(314, 15, 25F, p45));
		def(HbmWeaponModsG1.caliber("p22").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(320, 28, 10F, p22))
			.addMod(HbmItems.GUN_UZI.get(), new WeaponModCaliber(321, 40, 3F, p22))
			.addMod(HbmItems.GUN_UZI_AKIMBO.get(), new WeaponModCaliber(322, 40, 3F, p22));
		def(HbmWeaponModsG1.caliber("m357").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(330, 20, 10F, m357))
			.addMod(HbmItems.GUN_LAG.get(), new WeaponModCaliber(331, 15, 25F, m357));
		def(HbmWeaponModsG1.caliber("m44").get())
			.addMod(HbmItems.GUN_LAG.get(), new WeaponModCaliber(340, 13, 25F, m44));
		// 352 and 371: the original list is {gun_minigun, gun_minigun_dual}
		def(HbmWeaponModsG1.caliber("r556").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(350, 10, 10F, r556))
			.addMod(HbmItems.GUN_CARBINE.get(), new WeaponModCaliber(351, 20, 15F, r556))
			.addMod(new Item[] {HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_MINIGUN_DUAL.get() /* [parity d2] */}, new WeaponModCaliber(352, 0, 6F, r556));
		def(HbmWeaponModsG1.caliber("r762").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(360, 8, 10F, r762))
			.addMod(HbmItems.GUN_G3.get(), new WeaponModCaliber(361, 24, 5F, r762));
		def(HbmWeaponModsG1.caliber("bmg50").get())
			.addMod(HbmItems.GUN_HENRY.get(), new WeaponModCaliber(370, 5, 10F, bmg50))
			.addMod(new Item[] {HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_MINIGUN_DUAL.get() /* [parity d2] */}, new WeaponModCaliber(371, 0, 6F, bmg50));
	}
}
