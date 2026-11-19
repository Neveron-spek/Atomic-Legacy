package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.WeaponTableMenu;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryDrill;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryEnergy;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunDrill;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunNI4NI;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineElectricEngine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.atomiclegacy.items.weapon.sedna.mods.IWeaponMod;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModBase;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModEngine;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModsG3;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * [weapon mods G3] Dedicated-server checks of the weapon_mod_special variants LAS_SHOTGUN .. SIFTER against the pinned
 * 1.7.10 XWeaponModManager table (ids, slots, priorities, values), the drill engines, the weapon table and the recipes.
 */
final class GunModsG3Diagnostics {

	private GunModsG3Diagnostics() {
	}

	private static final int X0 = 1200;
	private static final AABB AREA = new AABB(X0 - 16, 280, -16, X0 + 16, 320, 24);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		for(int cx = (X0 - 16) >> 4; cx <= (X0 + 16) >> 4; cx++) for(int cz = -16 >> 4; cz <= 24 >> 4; cz++) level.getChunk(cx, cz);

		Item las = HbmItems.GUN_LASRIFLE.get(), ni = HbmItems.GUN_N_I_4_N_I.get(), dr = HbmItems.GUN_DRILL.get();
		Item mShotgun = HbmItems.WEAPON_MOD_LAS_SHOTGUN.get(), mCapacitor = HbmItems.WEAPON_MOD_LAS_CAPACITOR.get(), mAuto = HbmItems.WEAPON_MOD_LAS_AUTO.get();
		Item mNickel = HbmItems.WEAPON_MOD_NICKEL.get(), mDoubloons = HbmItems.WEAPON_MOD_DOUBLOONS.get();
		Item mHss = HbmItems.WEAPON_MOD_DRILL_HSS.get(), mWsteel = HbmItems.WEAPON_MOD_DRILL_WEAPONSTEEL.get(), mTc = HbmItems.WEAPON_MOD_DRILL_TCALLOY.get(), mSat = HbmItems.WEAPON_MOD_DRILL_SATURNITE.get();
		Item mDiesel = HbmItems.WEAPON_MOD_ENGINE_DIESEL.get(), mAviation = HbmItems.WEAPON_MOD_ENGINE_AVIATION.get(), mElectric = HbmItems.WEAPON_MOD_ENGINE_ELECTRIC.get(), mTurbo = HbmItems.WEAPON_MOD_ENGINE_TURBO.get();
		Item mMagnet = HbmItems.WEAPON_MOD_MAGNET.get(), mSifter = HbmItems.WEAPON_MOD_SIFTER.get();

		/// Registration, stack size, tab ///
		String[] ids = {"las_shotgun", "las_capacitor", "las_auto", "nickel", "doubloons", "drill_hss", "drill_weaponsteel", "drill_tcalloy", "drill_saturnite",
				"engine_diesel", "engine_aviation", "engine_electric", "engine_turbo", "magnet", "sifter"};
		List<DeferredItem<Item>> items = WeaponModsG3.items();
		boolean registered = items.size() == ids.length;
		for(int i = 0; registered && i < ids.length; i++) {
			Item item = items.get(i).get();
			registered = BuiltInRegistries.ITEM.getKey(item).equals(AtomicLegacy.id("weapon_mod_special_" + ids[i])) && new ItemStack(item).getMaxStackSize() == 1;
		}
		check(problems, cases, registered, "15 weapon_mod_special items in EnumModSpecial order, stack size 1");

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int lastAmmo = -1;
		for(int i = 0; i < tab.size(); i++) if(tab.get(i).is(HbmItems.AMMO_STANDARD.get())) lastAmmo = i;
		boolean order = lastAmmo >= 0;
		// after the merge of all mod batches the 15 mods sit between SKIN_SATURNITE and CANISTERS (EnumModSpecial order)
		int first = -1;
		for(int i = 0; i < tab.size(); i++) if(tab.get(i).is(items.get(0).get())) { first = i; break; }
		order = order && first > lastAmmo && first > 0 && tab.get(first - 1).is(HbmItems.WEAPON_MOD_SKIN_SATURNITE.get());
		for(int i = 0; order && i < ids.length; i++) order = first + i < tab.size() && tab.get(first + i).is(items.get(i).get());
		order = order && first + ids.length < tab.size() && tab.get(first + ids.length).is(HbmItems.WEAPON_MOD_CANISTERS.get());
		check(problems, cases, order, "weapon tab: mods follow ammo_standard in enum order (last ammo index " + lastAmmo + ")");

		/// Ids, slots, priorities (XWeaponModManager lines 153-168) ///
		XWeaponModManager.init();
		check(problems, cases, mod(216, "BARREL", WeaponModBase.PRIORITY_MULTIPLICATIVE) && mod(217, "UNDERBARREL", WeaponModBase.PRIORITY_MULTIPLICATIVE) && mod(218, "RECEIVER", WeaponModBase.PRIORITY_SET)
				&& XFactoryEnergy.ID_LAS_SHOTGUN == XWeaponModManager.ID_LAS_SHOTGUN && XFactoryEnergy.ID_LAS_CAPACITOR == XWeaponModManager.ID_LAS_CAPACITOR
				&& XFactoryEnergy.ID_LAS_AUTO == XWeaponModManager.ID_LAS_AUTO, "laser mod ids/slots/priorities");
		check(problems, cases, mod(220, "COIN1", WeaponModBase.PRIORITY_SET) && mod(221, "COIN2", WeaponModBase.PRIORITY_SET), "coin mod ids/slots/priorities");
		check(problems, cases, mod(222, "DRILL", WeaponModBase.PRIORITY_SET) && mod(223, "DRILL", WeaponModBase.PRIORITY_SET) && mod(224, "DRILL", WeaponModBase.PRIORITY_SET)
				&& mod(225, "DRILL", WeaponModBase.PRIORITY_SET), "drill bit ids/slots/priorities");
		check(problems, cases, mod(226, "ENGINE", WeaponModBase.PRIORITY_SET) && mod(227, "ENGINE", WeaponModBase.PRIORITY_SET) && mod(228, "ENGINE", WeaponModBase.PRIORITY_SET)
				&& mod(229, "ENGINE", WeaponModBase.PRIORITY_SET), "engine ids/slots/priorities");
		check(problems, cases, mod(230, "MAGNET", WeaponModBase.PRIORITY_ADDITIVE) && mod(231, "SIFTER", WeaponModBase.PRIORITY_ADDITIVE), "magnet/sifter ids/slots/priorities");
		Item[][] recognized = {{las, mShotgun, mCapacitor, mAuto}, {ni, mNickel, mDoubloons}, {dr, mHss, mWsteel, mTc, mSat, mDiesel, mAviation, mElectric, mTurbo, mMagnet, mSifter}};
		boolean rec = true;
		for(Item[] row : recognized) for(int i = 1; i < row.length; i++) {
			rec &= ((ItemGunBaseNT) row[0]).recognizedMods.contains(row[i]);
			for(Item[] other : recognized) rec &= XWeaponModManager.isApplicable(new ItemStack(other[0]), new ItemStack(row[i]), 0, true) == (other == row);
		}
		check(problems, cases, rec, "mods are recognized by and applicable to their own gun only");

		/// Laser rifle ///
		ItemGunBaseNT lasItem = (ItemGunBaseNT) las;
		ItemStack rifle = new ItemStack(las);
		GunConfig cfg = lasItem.getConfig(rifle, 0);
		Receiver r = cfg.getReceivers(rifle)[0];
		check(problems, cases, r.getBaseDamage(rifle) == 50F && r.getDelayAfterFire(rifle) == 8 && !r.getRefireOnHold(rifle) && r.getSplitProjectiles(rifle) == 1F && r.getInnateSpread(rifle) == 0F
				&& r.getHipfireSpread(rifle) == 1F && r.getMagazine(rifle).getCapacity(rifle) == 24 && cfg.getCrosshair(rifle) == Crosshair.CIRCLE
				&& XFactoryEnergy.scope_luna.equals(cfg.getScopeTexture(rifle)), "lasrifle base values");
		XWeaponModManager.install(rifle, 0, new ItemStack(mShotgun));
		check(problems, cases, XWeaponModManager.hasUpgrade(rifle, 0, 216) && near(r.getBaseDamage(rifle), 17.5F) && r.getSplitProjectiles(rifle) == 3F && r.getInnateSpread(rifle) == 3F
				&& r.getHipfireSpread(rifle) == 0F && cfg.getCrosshair(rifle) == Crosshair.L_CIRCLE && r.getDelayAfterFire(rifle) == 8 && r.getMagazine(rifle).getCapacity(rifle) == 24,
				"LAS_SHOTGUN: damage x0.35, split x3, spread +3, hipfire 0, L_CIRCLE");
		check(problems, cases, !XWeaponModManager.isApplicable(rifle, new ItemStack(mShotgun), 0, true) && XWeaponModManager.isApplicable(rifle, new ItemStack(mAuto), 0, true)
				&& XWeaponModManager.isApplicable(rifle, new ItemStack(mCapacitor), 0, true), "LAS_SHOTGUN BARREL mutex, other slots free");
		XWeaponModManager.install(rifle, 0, new ItemStack(mCapacitor));
		IMagazine<?> capMag = r.getMagazine(rifle);
		check(problems, cases, XWeaponModManager.hasUpgrade(rifle, 0, 217) && !XWeaponModManager.hasUpgrade(rifle, 0, 216) && near(r.getBaseDamage(rifle), 52.5F) && capMag instanceof MagazineFullReload full
				&& full.getCapacity(rifle) == 36 && full.acceptedBullets.size() == 3 && full.acceptedBullets.get(0) == XFactoryEnergy.energy_las && r.getSplitProjectiles(rifle) == 1F,
				"LAS_CAPACITOR: damage x1.05, capacity 24 -> 36, same ammo");
		XWeaponModManager.install(rifle, 0, new ItemStack(mAuto));
		check(problems, cases, XWeaponModManager.hasUpgrade(rifle, 0, 218) && near(r.getBaseDamage(rifle), 33F) && r.getRefireOnHold(rifle) && r.getDelayAfterFire(rifle) == 5
				&& cfg.getScopeTexture(rifle) == null && r.getMagazine(rifle).getCapacity(rifle) == 24, "LAS_AUTO: damage x0.66, refire on hold, delay 5, no scope");
		XWeaponModManager.install(rifle, 0, new ItemStack(mShotgun), new ItemStack(mCapacitor), new ItemStack(mAuto));
		check(problems, cases, XWeaponModManager.getUpgradeItems(rifle, 0).length == 3 && near(r.getBaseDamage(rifle), 50F * 0.66F * 0.35F * 1.05F) && r.getMagazine(rifle).getCapacity(rifle) == 36
				&& r.getSplitProjectiles(rifle) == 3F && r.getDelayAfterFire(rifle) == 5, "all three laser mods stack (" + r.getBaseDamage(rifle) + ")");
		XWeaponModManager.uninstall(rifle, 0);
		check(problems, cases, r.getBaseDamage(rifle) == 50F && r.getMagazine(rifle).getCapacity(rifle) == 24 && XFactoryEnergy.scope_luna.equals(cfg.getScopeTexture(rifle)), "lasrifle values restored");

		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(X0 + 0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().clearContent();

		/// NI4NI coins: 4, +2 per mod ///
		ItemStack coinGun = new ItemStack(ni);
		for(int i = 0; i < 80 * 10; i++) ni.inventoryTick(coinGun, level, player, 5, false);
		int plain = ItemGunNI4NI.getCoinCount(coinGun);
		XWeaponModManager.install(coinGun, 0, new ItemStack(mNickel));
		check(problems, cases, !XWeaponModManager.isApplicable(coinGun, new ItemStack(mNickel), 0, true) && XWeaponModManager.isApplicable(coinGun, new ItemStack(mDoubloons), 0, true), "NICKEL COIN1 mutex, DOUBLOONS free");
		for(int i = 0; i < 80 * 10; i++) ni.inventoryTick(coinGun, level, player, 5, false);
		int nickel = ItemGunNI4NI.getCoinCount(coinGun);
		XWeaponModManager.install(coinGun, 0, new ItemStack(mDoubloons));
		ItemStack doubloonGun = new ItemStack(ni);
		XWeaponModManager.install(doubloonGun, 0, new ItemStack(mDoubloons));
		for(int i = 0; i < 80 * 10; i++) ni.inventoryTick(doubloonGun, level, player, 5, false);
		int doubloons = ItemGunNI4NI.getCoinCount(doubloonGun);
		XWeaponModManager.install(coinGun, 0, new ItemStack(mNickel), new ItemStack(mDoubloons));
		for(int i = 0; i < 80 * 10; i++) ni.inventoryTick(coinGun, level, player, 5, false);
		int both = ItemGunNI4NI.getCoinCount(coinGun);
		Receiver nr = ((ItemGunBaseNT) ni).getConfig(coinGun, 0).getReceivers(coinGun)[0];
		check(problems, cases, plain == 4 && nickel == 6 && doubloons == 6 && both == 8 && nr.getBaseDamage(coinGun) == 35F,
				"NI4NI max coins 4 / 6 / 6 / 8, damage untouched (" + plain + "/" + nickel + "/" + doubloons + "/" + both + ")");

		/// Drill bits ///
		ItemGunDrill drillItem = (ItemGunDrill) dr;
		ItemStack drill = new ItemStack(dr);
		Receiver d = drillItem.getConfig(drill, 0).getReceivers(drill)[0];
		check(problems, cases, drillValues(drill, d, 10F, 5D, 2F, 0.15F, 1, 2) && d.getDelayAfterFire(drill) == 20, "drill base values");
		XWeaponModManager.install(drill, 0, new ItemStack(mHss));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 222) && drillValues(drill, d, 12.5F, 5D, 3F, 0.15F, 1, 3) && drillItem.isCorrectToolForDrops(drill, Blocks.OBSIDIAN.defaultBlockState()),
				"DRILL_HSS: damage x1.25, dt 3, pierce 0.15, harvest 3");
		check(problems, cases, !XWeaponModManager.isApplicable(drill, new ItemStack(mWsteel), 0, true) && !XWeaponModManager.isApplicable(drill, new ItemStack(mHss), 0, true)
				&& XWeaponModManager.isApplicable(drill, new ItemStack(mDiesel), 0, true) && XWeaponModManager.isApplicable(drill, new ItemStack(mMagnet), 0, true), "DRILL slot mutex, ENGINE / MAGNET free");
		XWeaponModManager.install(drill, 0, new ItemStack(mWsteel));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 223) && drillValues(drill, d, 15F, 5D, 5F, 0.2F, 2, 3), "DRILL_WEAPONSTEEL: damage x1.5, dt 5, pierce 0.2, aoe 2, harvest 3");
		XWeaponModManager.install(drill, 0, new ItemStack(mTc));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 224) && drillValues(drill, d, 20F, 10D, 7.5F, 0.2F, 3, 4), "DRILL_TCALLOY: damage x2, dt 7.5, pierce 0.2, reach x2, aoe 3, harvest 4");
		XWeaponModManager.install(drill, 0, new ItemStack(mSat));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 225) && drillValues(drill, d, 30F, 10D, 10F, 0.25F, 3, 5) && d.getDelayAfterFire(drill) == 20
				&& d.getMagazine(drill) instanceof MagazineLiquidEngine, "DRILL_SATURNITE: damage x3, dt 10, pierce 0.25, reach x2, aoe 3, harvest 5; engine untouched");
		XWeaponModManager.uninstall(drill, 0);

		/// Engines: magazine, delay, fluids, mag state ///
		IMagazine<?> baseMag = d.getMagazine(drill);
		baseMag.setAmount(drill, 3_000);
		XWeaponModManager.install(drill, 0, new ItemStack(mHss));
		check(problems, cases, d.getMagazine(drill) == baseMag && baseMag.getAmount(drill, null) == 3_000, "drill bit keeps the fuel");
		XWeaponModManager.install(drill, 0, new ItemStack(mDiesel));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 226) && d.getMagazine(drill) == WeaponModEngine.ENGINE_DIESEL && d.getDelayAfterFire(drill) == 15
				&& d.getMagazine(drill).getAmount(drill, null) == 0 && engine(d.getMagazine(drill), 4_000, "diesel", "diesel_crack", "lightoil"), "ENGINE_DIESEL: delay 15, 4000 mB diesel/diesel_crack/lightoil, tank emptied");
		check(problems, cases, fill(drill, "diesel") == 50 && fill(drill, "lightoil") == 50 && fill(drill, "gasoline") == 0 && fill(drill, "kerosene") == 0
				&& d.getMagazine(drill).reportAmmoStateForHUD(drill, player).equals("100/4000mB"), "diesel engine takes diesel/lightoil only");
		check(problems, cases, !XWeaponModManager.isApplicable(drill, new ItemStack(mTurbo), 0, true) && !XWeaponModManager.isApplicable(drill, new ItemStack(mElectric), 0, true), "ENGINE slot mutex");
		XWeaponModManager.install(drill, 0, new ItemStack(mDiesel), new ItemStack(mSat));
		check(problems, cases, d.getMagazine(drill).getAmount(drill, null) == 100 && drillValues(drill, d, 30F, 10D, 10F, 0.25F, 3, 5), "same engine reinstalled keeps the fuel, bit applies");
		XWeaponModManager.install(drill, 0, new ItemStack(mAviation));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 227) && d.getDelayAfterFire(drill) == 10 && engine(d.getMagazine(drill), 4_000, "kerosene", "lpg") && d.getMagazine(drill).getAmount(drill, null) == 0
				&& fill(drill, "lpg") == 50 && fill(drill, "diesel") == 0, "ENGINE_AVIATION: delay 10, 4000 mB kerosene/lpg");
		XWeaponModManager.install(drill, 0, new ItemStack(mTurbo));
		check(problems, cases, XWeaponModManager.hasUpgrade(drill, 0, 229) && d.getDelayAfterFire(drill) == 5 && engine(d.getMagazine(drill), 4_000, "kerosene_reform", "reformate")
				&& d.getMagazine(drill).getAmount(drill, null) == 0 && fill(drill, "reformate") == 50 && fill(drill, "kerosene") == 0, "ENGINE_TURBO: delay 5, 4000 mB kerosene_reform/reformate");
		check(problems, cases, BatteryPackItem.capacity(drill) == 0L && BatteryPackItem.chargeFromMachine(drill, 1_000_000L) == 1_000_000L && d.getMagazine(drill).getAmount(drill, null) == 50,
				"liquid engines take no HE");

		/// Electric engine: HE buffer, battery contract, 1000 per operation; fired with the weapon steel bit (5x5x5) ///
		ItemStack held = equip(player, dr);
		Receiver h = drillItem.getConfig(held, 0).getReceivers(held)[0];
		h.getMagazine(held).setAmount(held, 2_000);
		XWeaponModManager.install(held, 0, new ItemStack(mElectric), new ItemStack(mWsteel));
		IMagazine<?> electric = h.getMagazine(held);
		check(problems, cases, XWeaponModManager.hasUpgrade(held, 0, 228) && electric == WeaponModEngine.ENGINE_ELECTRIC && electric instanceof MagazineElectricEngine && electric.getCapacity(held) == 1_000_000
				&& electric.getAmount(held, null) == 0 && h.getDelayAfterFire(held) == 15 && fill(held, "gasoline") == 0 && fill(held, "diesel") == 0, "ENGINE_ELECTRIC: delay 15, 1M HE buffer, no fluids, starts empty");
		long left = BatteryPackItem.chargeFromMachine(held, 1_000_000L);
		check(problems, cases, BatteryPackItem.isBattery(held) && BatteryPackItem.capacity(held) == 1_000_000L && BatteryPackItem.chargeRate(held) == 50_000L && BatteryPackItem.dischargeRate(held) == 0L
				&& left == 950_000L && BatteryPackItem.charge(held) == 50_000L && electric.reportAmmoStateForHUD(held, player).equals("50.0k/1.0MHE")
				&& electric.getIconForHUD(held, player).is(HbmItems.BATTERY_CREATIVE.get()), "electric drill charges 50k HE per tick, HUD 50.0k/1.0MHE (" + electric.reportAmmoStateForHUD(held, player) + ")");
		check(problems, cases, BatteryPackItem.dischargeIntoMachine(held, 0L, 1_000_000L) == 0L && BatteryPackItem.charge(held) == 50_000L, "electric drill does not discharge");

		BlockPos face = BlockPos.containing(X0 + 0.5, player.getEyeY(), 3.5);
		for(int dx = -3; dx <= 3; dx++) for(int dy = -3; dy <= 3; dy++) for(int dz = 0; dz <= 5; dz++) level.setBlockAndUpdate(face.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
		press(player, held, GunKeybind.GUN_PRIMARY);
		release(player, held, GunKeybind.GUN_PRIMARY);
		int inside = 0, outside = 0;
		for(int dx = -3; dx <= 3; dx++) for(int dy = -3; dy <= 3; dy++) for(int dz = 0; dz <= 5; dz++) {
			boolean air = level.getBlockState(face.offset(dx, dy, dz)).isAir();
			if(Math.abs(dx) <= 2 && Math.abs(dy) <= 2 && dz <= 2) { if(air) inside++; } else if(!air) outside++;
		}
		check(problems, cases, inside == 75 && outside == 7 * 7 * 6 - 75 && electric.getAmount(held, null) == 49_000 && ItemGunBaseNT.getTimer(held, 0) == 15,
				"electric drill with the weapon steel bit breaks the 5x5x5 AoE (75 of the 7x7x6 stone) for 1000 HE, delay 15 (" + inside + "/" + outside + "/" + electric.getAmount(held, null) + ")");
		for(int dx = -3; dx <= 3; dx++) for(int dy = -3; dy <= 3; dy++) for(int dz = 0; dz <= 5; dz++) level.setBlockAndUpdate(face.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
		for(int i = 0; i < 30; i++) BatteryPackItem.chargeFromMachine(held, 1_000_000L);
		check(problems, cases, BatteryPackItem.charge(held) == 1_000_000L && BatteryPackItem.chargeFromMachine(held, 5L) == 5L, "electric drill charge caps at 1M HE");
		XWeaponModManager.install(held, 0, new ItemStack(mWsteel)); // the table reinstalls the remaining mods when one is pulled
		check(problems, cases, h.getMagazine(held) instanceof MagazineLiquidEngine && h.getMagazine(held).getAmount(held, null) == 0 && h.getDelayAfterFire(held) == 20
				&& BatteryPackItem.capacity(held) == 0L, "removing the electric engine restores the gasoline engine, empty");
		player.getInventory().clearContent();

		/// Magnet / sifter: fortune +2 / +1 ///
		Holder<Enchantment> fortune = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
		ItemStack lucky = new ItemStack(dr);
		XWeaponModManager.install(lucky, 0, new ItemStack(mMagnet));
		int magnet = lucky.getTagEnchantments().getLevel(fortune);
		check(problems, cases, !XWeaponModManager.isApplicable(lucky, new ItemStack(mMagnet), 0, true) && XWeaponModManager.isApplicable(lucky, new ItemStack(mSifter), 0, true), "MAGNET mutex, SIFTER free");
		XWeaponModManager.install(lucky, 0, new ItemStack(mSifter));
		int sifter = lucky.getTagEnchantments().getLevel(fortune);
		XWeaponModManager.install(lucky, 0, new ItemStack(mMagnet), new ItemStack(mSifter), new ItemStack(mTc));
		int bothFortune = lucky.getTagEnchantments().getLevel(fortune);
		XWeaponModManager.install(lucky, 0, new ItemStack(mMagnet), new ItemStack(mSifter), new ItemStack(mTc));
		int reinstall = lucky.getTagEnchantments().getLevel(fortune);
		XWeaponModManager.uninstall(lucky, 0);
		check(problems, cases, magnet == 2 && sifter == 1 && bothFortune == 3 && reinstall == 3 && lucky.getTagEnchantments().isEmpty(),
				"MAGNET fortune 2, SIFTER 1, both 3, none after removal (" + magnet + "/" + sifter + "/" + bothFortune + "/" + reinstall + ")");

		/// Weapon modification table ///
		ItemStack tableGun = new ItemStack(dr);
		WeaponTableMenu menu = new WeaponTableMenu(0, player.getInventory());
		menu.getSlot(7).set(tableGun);
		check(problems, cases, menu.getSlot(0).mayPlace(new ItemStack(mSat)) && menu.getSlot(0).mayPlace(new ItemStack(mElectric)) && !menu.getSlot(0).mayPlace(new ItemStack(mAuto))
				&& !menu.getSlot(0).mayPlace(new ItemStack(mNickel)), "table accepts drill mods for the drill only");
		menu.getSlot(0).set(new ItemStack(mSat));
		check(problems, cases, !menu.getSlot(1).mayPlace(new ItemStack(mHss)) && menu.getSlot(1).mayPlace(new ItemStack(mElectric)), "table DRILL mutex");
		menu.getSlot(1).set(new ItemStack(mElectric));
		menu.getSlot(2).set(new ItemStack(mMagnet));
		check(problems, cases, !menu.getSlot(3).mayPlace(new ItemStack(mDiesel)) && !menu.getSlot(3).mayPlace(new ItemStack(mMagnet)) && menu.getSlot(3).mayPlace(new ItemStack(mSifter)), "table ENGINE / MAGNET mutex");
		ItemStack taken = menu.getSlot(7).getItem();
		menu.getSlot(7).set(ItemStack.EMPTY);
		menu.getSlot(7).onTake(player, taken);
		Receiver t = drillItem.getConfig(taken, 0).getReceivers(taken)[0];
		check(problems, cases, XWeaponModManager.hasUpgrade(taken, 0, 225) && XWeaponModManager.hasUpgrade(taken, 0, 228) && XWeaponModManager.hasUpgrade(taken, 0, 230) && menu.mods.isEmpty()
				&& t.getMagazine(taken) instanceof MagazineElectricEngine && XFactoryDrill.getModdableAoE(taken, 1) == 3 && taken.getTagEnchantments().getLevel(fortune) == 2, "table installs on take");
		menu.getSlot(7).set(taken);
		int shown = 0;
		for(int i = 0; i < 7; i++) if(!menu.mods.getItem(i).isEmpty()) shown++;
		player.getInventory().clearContent();
		menu.removed(player);
		check(problems, cases, shown == 3 && menu.gun.isEmpty() && menu.mods.isEmpty() && XWeaponModManager.getUpgradeItems(taken, 0).length == 0 && taken.getTagEnchantments().isEmpty()
				&& t.getMagazine(taken) instanceof MagazineLiquidEngine, "table shows 3 installed mods, closing uninstalls them (" + shown + ")");
		for(ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, AREA)) drop.discard();
		player.getInventory().clearContent();

		/// Recipes (WeaponRecipes lines 151-163); NICKEL / DOUBLOONS have none (red room loot) ///
		ItemStack E = ItemStack.EMPTY;
		ItemStack pc = item("ingot_pc"), red = item("crystal_redstone"), bism = item("circuit_chip_bismoid"), dura = item("ingot_dura_steel"), poly = item("ingot_polymer"), rubber = item("ingot_rubber");
		ItemStack mg = item("part_mechanism_gunmetal"), mw = item("part_mechanism_weaponsteel"), piston = item("piston_selenium"), wire = item("wire_dense_gold");
		checkRecipe(problems, cases, server, level, mShotgun, 3, 3, List.of(pc, pc, pc, red, item("circuit_advanced"), red, pc, pc, pc));
		checkRecipe(problems, cases, server, level, mShotgun, 3, 3, List.of(item("ingot_pvc"), pc, pc, red, item("circuit_advanced"), red, pc, pc, pc));
		ItemStack tant = item("circuit_capacitor_tantalium");
		checkRecipe(problems, cases, server, level, mCapacitor, 3, 2, List.of(tant, tant, tant, pc, bism, pc));
		checkRecipe(problems, cases, server, level, mAuto, 3, 3, List.of(E, bism, E, red, item("part_receiver_heavy_bismuth_bronze"), red, E, bism, E));
		checkRecipe(problems, cases, server, level, mAuto, 3, 3, List.of(E, bism, E, red, item("part_receiver_heavy_arsenic_bronze"), red, E, bism, E));
		checkRecipe(problems, cases, server, level, mHss, 3, 3, bit(dura, poly, mg));
		checkRecipe(problems, cases, server, level, mHss, 3, 3, bit(dura, item("ingot_bakelite"), mg));
		checkRecipe(problems, cases, server, level, mWsteel, 3, 3, bit(item("ingot_weaponsteel"), rubber, mg));
		checkRecipe(problems, cases, server, level, mTc, 3, 3, bit(item("ingot_tcalloy"), rubber, mw));
		checkRecipe(problems, cases, server, level, mTc, 3, 3, bit(item("ingot_cdalloy"), rubber, mw));
		checkRecipe(problems, cases, server, level, mSat, 3, 3, bit(item("ingot_saturnite"), pc, mw));
		checkRecipe(problems, cases, server, level, mDiesel, 3, 3, engine(item("plate_dura_steel"), piston, item("pipe_steel")));
		checkRecipe(problems, cases, server, level, mAviation, 3, 3, engine(item("plate_cast_dura_steel"), piston, mg));
		checkRecipe(problems, cases, server, level, mElectric, 3, 3, engine(poly, wire, item("battery_pack_capacitor_gold")));
		ItemStack charged = item("battery_pack_capacitor_gold");
		BatteryPackItem.setStoredCharge(charged, BatteryPackItem.capacity(charged));
		checkRecipe(problems, cases, server, level, mElectric, 3, 3, engine(poly, wire, charged));
		checkRecipe(problems, cases, server, level, mTurbo, 3, 3, engine(item("plate_cast_bismuth_bronze"), piston, mw));
		checkRecipe(problems, cases, server, level, mTurbo, 3, 3, engine(item("plate_cast_arsenic_bronze"), piston, mw));
		checkRecipe(problems, cases, server, level, mMagnet, 3, 3, List.of(rubber, wire, rubber, wire, item("block_niobium"), wire, rubber, wire, rubber));
		ItemStack grate = item("steel_grate");
		checkRecipe(problems, cases, server, level, mSifter, 3, 2, List.of(dura, grate, dura, dura, grate, dura));
		check(problems, cases, server.getRecipeManager().byKey(AtomicLegacy.id("weapon_mod_special_nickel")).isEmpty()
				&& server.getRecipeManager().byKey(AtomicLegacy.id("weapon_mod_special_doubloons")).isEmpty(), "NICKEL / DOUBLOONS have no crafting recipe");

		return cases[0];
	}

	private static boolean mod(int id, String slot, int priority) {
		IWeaponMod mod = XWeaponModManager.idToMod.get(id);
		return mod != null && mod.getSlots().length == 1 && mod.getSlots()[0].equals(slot) && mod.getModPriority() == priority && XWeaponModManager.modToStack.get(mod) != null;
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 0.001F;
	}

	private static boolean drillValues(ItemStack drill, Receiver rec, float damage, double reach, float dt, float pierce, int aoe, int harvest) {
		return near(rec.getBaseDamage(drill), damage) && XFactoryDrill.getModdableReach(drill, 5D) == reach && XFactoryDrill.getModdableDTNegation(drill, 2F) == dt
				&& XFactoryDrill.getModdablePiercing(drill, 0.15F) == pierce && XFactoryDrill.getModdableAoE(drill, 1) == aoe && XFactoryDrill.getModdableHarvestLevel(drill, 2) == harvest;
	}

	private static boolean engine(IMagazine<?> mag, int capacity, String... fluids) {
		if(!(mag instanceof MagazineLiquidEngine engine) || engine.capacity != capacity || engine.acceptedTypes.length != fluids.length) return false;
		for(int i = 0; i < fluids.length; i++) if(engine.acceptedTypes[i] != HbmFluids.byName(fluids[i]).orElse(null)) return false;
		return true;
	}

	private static int fill(ItemStack drill, String fluid) {
		IFluidHandlerItem handler = drill.getCapability(Capabilities.FluidHandler.ITEM);
		return handler == null ? -1 : handler.fill(HbmFluids.toNeoStack(HbmFluids.byName(fluid).orElseThrow(), 1_000), IFluidHandler.FluidAction.EXECUTE);
	}

	/** " IP", "IIM", " IP" */
	private static List<ItemStack> bit(ItemStack i, ItemStack p, ItemStack m) {
		return List.of(ItemStack.EMPTY, i, p, i, i, m, ItemStack.EMPTY, i, p);
	}

	/** "DSD", "PPP", "DSD" */
	private static List<ItemStack> engine(ItemStack d, ItemStack p, ItemStack s) {
		return List.of(d, s, d, p, p, p, d, s, d);
	}

	private static void checkRecipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, Item result, int w, int h, List<ItemStack> grid) {
		String id = BuiltInRegistries.ITEM.getKey(result).getPath();
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		boolean ok = recipe instanceof ShapedRecipe s && s.matches(input, level);
		if(ok) {
			ItemStack out = ((ShapedRecipe) recipe).assemble(input, server.registryAccess());
			ok = out.is(result) && out.getCount() == 1;
		}
		check(problems, cases, ok, id + " recipe");
	}

	private static ItemStack equip(FakePlayer player, Item gun) {
		player.getInventory().clearContent();
		player.getInventory().setItem(0, new ItemStack(gun));
		ItemStack stack = player.getInventory().getItem(0);
		tick(player, stack, 1);
		tick(player, stack, ((ItemGunBaseNT) gun).getConfig(stack, 0).getDrawDuration(stack) + 1);
		return stack;
	}

	private static void press(FakePlayer player, ItemStack gun, GunKeybind key) { ((ItemGunBaseNT) gun.getItem()).handleKeybind(player, gun, key, true); }
	private static void release(FakePlayer player, ItemStack gun, GunKeybind key) { ((ItemGunBaseNT) gun.getItem()).handleKeybind(player, gun, key, false); }

	private static void tick(FakePlayer player, ItemStack stack, int ticks) {
		for(int i = 0; i < ticks; i++) stack.getItem().inventoryTick(stack, player.level(), player, 0, true);
	}

	private static ItemStack item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(Items.BARRIER));
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun mods G3: " + message);
	}
}
