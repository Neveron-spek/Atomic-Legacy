package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.WeaponTableMenu;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory44;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.atomiclegacy.items.weapon.sedna.mods.IWeaponMod;
import com.atomiclegacy.items.weapon.sedna.mods.WeapnModG3SawedOff;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModBase;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModCarbineBayonet;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModLiberatorSpeedloader;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModMASBayonet;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModPanzerschreckSawedOff;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModPolymerFurniture;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for the weapon_mod_special variants SILENCER, SAW, SLOWDOWN, SPEEDUP, CHOKE, SPEEDLOADER,
 * FURNITURE_GREEN, FURNITURE_BLACK, BAYONET, STACK_MAG, SKIN_SATURNITE and CANISTERS against the pinned 1.7.10
 * XWeaponModManager table and WeaponMod* classes: registration, ids, slots, priorities, values before and after
 * installing, mutual exclusion, the modification table and the crafting recipes.
 */
final class GunModsG2Diagnostics {

	private GunModsG2Diagnostics() {
	}

	private static final AABB AREA = new AABB(-24, 280, -24, 24, 320, 40);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		Item silencer = HbmItems.WEAPON_MOD_SILENCER.get(), saw = HbmItems.WEAPON_MOD_SAW.get(), slowdown = HbmItems.WEAPON_MOD_SLOWDOWN.get();
		Item speedup = HbmItems.WEAPON_MOD_SPEEDUP.get(), choke = HbmItems.WEAPON_MOD_CHOKE.get(), speedloader = HbmItems.WEAPON_MOD_SPEEDLOADER.get();
		Item green = HbmItems.WEAPON_MOD_FURNITURE_GREEN.get(), black = HbmItems.WEAPON_MOD_FURNITURE_BLACK.get(), bayonet = HbmItems.WEAPON_MOD_BAYONET.get();
		Item stackMag = HbmItems.WEAPON_MOD_STACK_MAG.get(), saturnite = HbmItems.WEAPON_MOD_SKIN_SATURNITE.get(), canisters = HbmItems.WEAPON_MOD_CANISTERS.get();

		/// Items: registry names, stack size, weapon tab position (EnumModSpecial order after ammo_standard) ///
		Item[] mods = {silencer, saw, slowdown, speedup, choke, speedloader, green, black, bayonet, stackMag, saturnite, canisters};
		String[] names = {"silencer", "saw", "slowdown", "speedup", "choke", "speedloader", "furniture_green", "furniture_black", "bayonet", "stack_mag", "skin_saturnite", "canisters"};
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int last = indexOf(tab, HbmItems.AMMO_STANDARD.get());
		check(problems, cases, last >= 0 && last > indexOf(tab, HbmItems.GUN_DRILL.get()), "ammo_standard follows the guns in the weapon tab");
		for(int i = 0; i < mods.length; i++) {
			String id = BuiltInRegistries.ITEM.getKey(mods[i]).toString();
			int index = indexOf(tab, mods[i]);
			check(problems, cases, id.equals("atomiclegacy:weapon_mod_special_" + names[i]) && new ItemStack(mods[i]).getMaxStackSize() == 1, id + " id/stack size");
			check(problems, cases, index > last, id + " weapon tab position " + index + " after " + last);
			last = Math.max(last, index);
		}

		/// Definitions: id, slots, priority, gun list (XWeaponModManager) ///
		XWeaponModManager.init();
		definition(problems, cases, speedloader, 200, "SPEEDLOADER", 0, HbmItems.GUN_LIBERATOR.get());
		definition(problems, cases, silencer, 201, "SILENCER", 0, HbmItems.GUN_AM180.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(), HbmItems.GUN_STAR_F.get(),
				HbmItems.GUN_STAR_F_AKIMBO.get(), HbmItems.GUN_G3.get(), HbmItems.GUN_AMAT.get());
		definition(problems, cases, saw, 203, "BARREL", 0, HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_DOUBLE_BARREL.get());
		definition(problems, cases, saw, 204, "SHIELD", 0, HbmItems.GUN_PANZERSCHRECK.get());
		definition(problems, cases, saw, 205, "SHIELD", 0, HbmItems.GUN_G3.get(), HbmItems.GUN_G3_ZEBRA.get());
		definition(problems, cases, slowdown, 207, "SPEED", 0, HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_MINIGUN_DUAL.get() /* [parity d2] */);
		definition(problems, cases, speedup, 208, "SPEED", 0, HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_MINIGUN_DUAL.get() /* [parity d2] */);
		definition(problems, cases, speedup, 209, "SPEED", 0, HbmItems.GUN_AUTOSHOTGUN.get(), HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(), HbmItems.GUN_MK108.get());
		definition(problems, cases, choke, 210, "BARREL", 0, HbmItems.GUN_PEPPERBOX.get(), HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_DOUBLE_BARREL.get(), HbmItems.GUN_LIBERATOR.get(),
				HbmItems.GUN_SPAS12.get(), HbmItems.GUN_AUTOSHOTGUN_SEXY.get(), HbmItems.GUN_AUTOSHOTGUN_HERETIC.get() /* [parity c1] */);
		definition(problems, cases, green, 211, "FURNITURE", 0, HbmItems.GUN_G3.get());
		definition(problems, cases, black, 212, "FURNITURE", 0, HbmItems.GUN_G3.get());
		definition(problems, cases, bayonet, 213, "BAYONET", 0, HbmItems.GUN_MAS36.get());
		definition(problems, cases, bayonet, 219, "BAYONET", 0, HbmItems.GUN_CARBINE.get());
		definition(problems, cases, stackMag, 214, "MAG", 0, HbmItems.GUN_GREASEGUN.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(), HbmItems.GUN_ABERRATOR.get(),
				HbmItems.GUN_ABERRATOR_EOTT.get());
		definition(problems, cases, saturnite, 215, "FURNITURE", WeaponModBase.PRIORITY_ADDITIVE, HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get());
		definition(problems, cases, canisters, 232, "CANISTERS", WeaponModBase.PRIORITY_MULT_FINAL, HbmItems.GUN_DRILL.get());
		// not applicable outside the lists
		check(problems, cases, !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_GREASEGUN.get()), new ItemStack(silencer), 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_AMAT_PENANCE.get()), new ItemStack(silencer), 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_G3_ZEBRA.get()), new ItemStack(green), 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_AUTOSHOTGUN.get()), new ItemStack(choke), 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_MINIGUN_LACUNAE.get()), new ItemStack(speedup), 0, true), "mods outside the gun lists are rejected");

		/// SILENCER ///
		for(Item gun : new Item[] {HbmItems.GUN_AM180.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(), HbmItems.GUN_STAR_F.get(), HbmItems.GUN_STAR_F_AKIMBO.get(),
				HbmItems.GUN_G3.get(), HbmItems.GUN_AMAT.get()}) {
			ItemStack s = new ItemStack(gun);
			var silenced = gun == HbmItems.GUN_AMAT.get() ? HbmSoundEvents.GUN_AMAT_SILENCER.get() : HbmSoundEvents.GUN_RIFLE_SILENCER.get();
			var before = rec(s, 0).getFireSound(s).get();
			float volume = rec(s, 0).getFireVolume(s), damage = rec(s, 0).getBaseDamage(s);
			XWeaponModManager.install(s, 0, new ItemStack(silencer));
			check(problems, cases, before != silenced && rec(s, 0).getFireSound(s).get() == silenced && rec(s, 0).getFireVolume(s) == volume && rec(s, 0).getBaseDamage(s) == damage
					&& XWeaponModManager.hasUpgrade(s, 0, XWeaponModManager.ID_SILENCER), name(gun) + " silencer sound");
			if(((ItemGunBaseNT) gun).getConfigCount() > 1) {
				check(problems, cases, rec(s, 1).getFireSound(s).get() == before && !XWeaponModManager.hasUpgrade(s, 1, XWeaponModManager.ID_SILENCER), name(gun) + " silencer is per config");
				XWeaponModManager.install(s, 1, new ItemStack(silencer));
				check(problems, cases, rec(s, 1).getFireSound(s).get() == silenced, name(gun) + " silencer on config 1");
			}
			XWeaponModManager.uninstall(s, 0);
			check(problems, cases, rec(s, 0).getFireSound(s).get() == before && !XWeaponModManager.hasUpgrade(s, 0, XWeaponModManager.ID_SILENCER), name(gun) + " silencer removal");
		}
		check(problems, cases, modded(HbmItems.GUN_AM180.get(), silencer).getHoverName().getContents() instanceof TranslatableContents t1 && t1.getKey().equals("item.atomiclegacy.gun_am180_silenced")
				&& modded(HbmItems.GUN_UZI.get(), silencer).getHoverName().getContents() instanceof TranslatableContents t2 && t2.getKey().equals("item.atomiclegacy.gun_uzi_richter")
				&& modded(HbmItems.GUN_STAR_F.get(), silencer).getHoverName().getContents() instanceof TranslatableContents t3 && t3.getKey().equals("item.atomiclegacy.gun_star_f_silenced"), "silenced names");

		/// SAW: maresleg / double barrel (WeaponModSawedOff) ///
		for(Item gun : new Item[] {HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_DOUBLE_BARREL.get()}) {
			ItemStack s = new ItemStack(gun);
			float innate = rec(s, 0).getInnateSpread(s), ammo = rec(s, 0).getAmmoSpread(s), damage = rec(s, 0).getBaseDamage(s);
			int draw = cfg(s, 0).getDrawDuration(s);
			var anims = cfg(s, 0).getAnims(s);
			XWeaponModManager.install(s, 0, new ItemStack(saw));
			boolean mares = gun == HbmItems.GUN_MARESLEG.get();
			check(problems, cases, rec(s, 0).getInnateSpread(s) == Math.max(0.025F, innate) && rec(s, 0).getAmmoSpread(s) == ammo * 1.5F && rec(s, 0).getBaseDamage(s) == damage * 1.35F
					&& cfg(s, 0).getDrawDuration(s) == (mares ? 5 : draw) && cfg(s, 0).getAnims(s) == (mares ? XFactory12ga.LAMBDA_MARESLEG_SHORT_ANIMS : anims)
					&& XWeaponModManager.hasUpgrade(s, 0, XFactory12ga.ID_SAWED_OFF), name(gun) + " sawed-off values " + rec(s, 0).getInnateSpread(s) + "/" + rec(s, 0).getAmmoSpread(s) + "/" + rec(s, 0).getBaseDamage(s));
			check(problems, cases, !XWeaponModManager.isApplicable(s, new ItemStack(choke), 0, true) && XWeaponModManager.isApplicable(s, new ItemStack(choke), 0, false), name(gun) + " BARREL mutex saw/choke");
			XWeaponModManager.uninstall(s, 0);
			check(problems, cases, rec(s, 0).getAmmoSpread(s) == ammo && rec(s, 0).getBaseDamage(s) == damage && cfg(s, 0).getDrawDuration(s) == draw && cfg(s, 0).getAnims(s) == anims, name(gun) + " sawed-off removal");
		}
		check(problems, cases, modded(HbmItems.GUN_MARESLEG.get(), saw).getHoverName().getContents() instanceof TranslatableContents t4 && t4.getKey().equals("item.atomiclegacy.gun_maresleg_short"), "maresleg short name");

		/// SAW: panzerschreck (no shield) and G3 (no stock) ///
		ItemStack pz = new ItemStack(HbmItems.GUN_PANZERSCHRECK.get());
		var pzFire = rec(pz, 0).getOnFire(pz);
		var pzAnims = cfg(pz, 0).getAnims(pz);
		int pzDraw = cfg(pz, 0).getDrawDuration(pz);
		XWeaponModManager.install(pz, 0, new ItemStack(saw));
		check(problems, cases, pzDraw == 7 && cfg(pz, 0).getDrawDuration(pz) == 5 && pzFire != WeaponModPanzerschreckSawedOff.LAMBDA_FIRE && rec(pz, 0).getOnFire(pz) == WeaponModPanzerschreckSawedOff.LAMBDA_FIRE
				&& pzAnims != WeaponModPanzerschreckSawedOff.LAMBDA_PANZERSCHRECK_ANIMS && cfg(pz, 0).getAnims(pz) == WeaponModPanzerschreckSawedOff.LAMBDA_PANZERSCHRECK_ANIMS
				&& XWeaponModManager.hasUpgrade(pz, 0, XWeaponModManager.ID_NO_SHIELD) && rec(pz, 0).getBaseDamage(pz) == 25F, "panzerschreck no-shield values");
		check(problems, cases, cfg(pz, 0).getAnims(pz).apply(pz, GunAnimation.EQUIP) != null && cfg(pz, 0).getAnims(pz).apply(pz, GunAnimation.RELOAD) != null, "panzerschreck no-shield animations");

		for(Item gun : new Item[] {HbmItems.GUN_G3.get(), HbmItems.GUN_G3_ZEBRA.get()}) {
			ItemStack s = new ItemStack(gun);
			int draw = cfg(s, 0).getDrawDuration(s);
			var anims = cfg(s, 0).getAnims(s);
			XWeaponModManager.install(s, 0, new ItemStack(saw));
			check(problems, cases, draw != 5 && cfg(s, 0).getDrawDuration(s) == 5 && anims != WeapnModG3SawedOff.LAMBDA_G3_ANIMS && cfg(s, 0).getAnims(s) == WeapnModG3SawedOff.LAMBDA_G3_ANIMS
					&& XWeaponModManager.hasUpgrade(s, 0, XWeaponModManager.ID_NO_STOCK) && cfg(s, 0).getAnims(s).apply(s, GunAnimation.EQUIP) != null
					&& cfg(s, 0).getAnims(s).apply(s, GunAnimation.CYCLE) != null, name(gun) + " no-stock values");
		}

		/// SLOWDOWN / SPEEDUP ///
		ItemStack mini = new ItemStack(HbmItems.GUN_MINIGUN.get());
		int miniDelay = rec(mini, 0).getDelayAfterFire(mini), miniRounds = rec(mini, 0).getRoundsPerCycle(mini);
		float miniSpread = rec(mini, 0).getInnateSpread(mini);
		XWeaponModManager.install(mini, 0, new ItemStack(slowdown));
		check(problems, cases, miniSpread != 0F && rec(mini, 0).getDelayAfterFire(mini) == miniDelay * 2 && rec(mini, 0).getInnateSpread(mini) == 0F && rec(mini, 0).getRoundsPerCycle(mini) == miniRounds
				&& XWeaponModManager.hasUpgrade(mini, 0, XFactory762mm.ID_MINIGUN_SLOWDOWN), "minigun slowdown values");
		check(problems, cases, !XWeaponModManager.isApplicable(mini, new ItemStack(speedup), 0, true), "minigun SPEED mutex slowdown/speedup");
		XWeaponModManager.install(mini, 0, new ItemStack(speedup));
		check(problems, cases, rec(mini, 0).getRoundsPerCycle(mini) == miniRounds * 3 && rec(mini, 0).getInnateSpread(mini) == miniSpread * 1.5F && rec(mini, 0).getDelayAfterFire(mini) == miniDelay
				&& XWeaponModManager.hasUpgrade(mini, 0, XFactory762mm.ID_MINIGUN_SPEED) && !XWeaponModManager.hasUpgrade(mini, 0, XFactory762mm.ID_MINIGUN_SLOWDOWN), "minigun speedup values");
		for(Item gun : new Item[] {HbmItems.GUN_AUTOSHOTGUN.get(), HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(), HbmItems.GUN_MK108.get()}) {
			ItemStack s = new ItemStack(gun);
			int delay = rec(s, 0).getDelayAfterFire(s), dry = rec(s, 0).getDelayAfterDryFire(s), rounds = rec(s, 0).getRoundsPerCycle(s);
			XWeaponModManager.install(s, 0, new ItemStack(speedup));
			check(problems, cases, rec(s, 0).getDelayAfterFire(s) == delay / 2 && rec(s, 0).getDelayAfterDryFire(s) == dry / 2 && rec(s, 0).getRoundsPerCycle(s) == rounds
					&& XWeaponModManager.hasUpgrade(s, 0, 209), name(gun) + " speedup delays " + delay + "->" + rec(s, 0).getDelayAfterFire(s));
		}

		/// CHOKE ///
		for(Item gun : new Item[] {HbmItems.GUN_PEPPERBOX.get(), HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_DOUBLE_BARREL.get(), HbmItems.GUN_LIBERATOR.get(), HbmItems.GUN_SPAS12.get(),
				HbmItems.GUN_AUTOSHOTGUN_SEXY.get()}) {
			ItemStack s = new ItemStack(gun);
			float ammo = rec(s, 0).getAmmoSpread(s), innate = rec(s, 0).getInnateSpread(s), damage = rec(s, 0).getBaseDamage(s);
			XWeaponModManager.install(s, 0, new ItemStack(choke));
			check(problems, cases, rec(s, 0).getAmmoSpread(s) == ammo * 0.5F && rec(s, 0).getInnateSpread(s) == innate && rec(s, 0).getBaseDamage(s) == damage
					&& XWeaponModManager.hasUpgrade(s, 0, 210), name(gun) + " choke spread " + ammo + "->" + rec(s, 0).getAmmoSpread(s));
		}

		/// SPEEDLOADER ///
		ItemStack lib = new ItemStack(HbmItems.GUN_LIBERATOR.get());
		IMagazine<?> libMag = rec(lib, 0).getMagazine(lib);
		var libAnims = cfg(lib, 0).getAnims(lib);
		XWeaponModManager.install(lib, 0, new ItemStack(speedloader), new ItemStack(choke));
		IMagazine<?> loader = rec(lib, 0).getMagazine(lib);
		check(problems, cases, !(libMag instanceof MagazineFullReload) && libMag.getCapacity(lib) == 4 && loader == WeaponModLiberatorSpeedloader.MAG && loader instanceof MagazineFullReload
				&& loader.getCapacity(lib) == 4 && ((MagazineSingleTypeBase) loader).acceptedBullets.equals(((MagazineSingleTypeBase) libMag).acceptedBullets)
				&& libAnims != WeaponModLiberatorSpeedloader.LAMBDA_LIBERATOR_ANIMS && cfg(lib, 0).getAnims(lib) == WeaponModLiberatorSpeedloader.LAMBDA_LIBERATOR_ANIMS
				&& XWeaponModManager.hasUpgrade(lib, 0, 200) && XWeaponModManager.hasUpgrade(lib, 0, 210), "liberator speedloader magazine/animations (with choke)");
		check(problems, cases, cfg(lib, 0).getAnims(lib).apply(lib, GunAnimation.RELOAD) != null && cfg(lib, 0).getAnims(lib).apply(lib, GunAnimation.RELOAD_END) != null
				&& cfg(lib, 0).getAnims(lib).apply(lib, GunAnimation.JAMMED) != null && cfg(lib, 0).getAnims(lib).apply(lib, GunAnimation.CYCLE) != null, "liberator speedloader animation set");

		/// FURNITURE (G3) ///
		ItemStack g3 = new ItemStack(HbmItems.GUN_G3.get());
		var g3Recoil = rec(g3, 0).getRecoil(g3);
		XWeaponModManager.install(g3, 0, new ItemStack(green));
		check(problems, cases, g3Recoil != WeaponModPolymerFurniture.LAMBDA_RECOIL_G3 && rec(g3, 0).getRecoil(g3) == WeaponModPolymerFurniture.LAMBDA_RECOIL_G3
				&& XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_FURNITURE_GREEN) && !XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_FURNITURE_BLACK)
				&& g3.getHoverName().getContents() instanceof TranslatableContents t5 && t5.getKey().equals("item.atomiclegacy.gun_g3_a3"), "g3 green furniture (recoil, A3 name)");
		check(problems, cases, !XWeaponModManager.isApplicable(g3, new ItemStack(black), 0, true) && XWeaponModManager.isApplicable(g3, new ItemStack(silencer), 0, true)
				&& XWeaponModManager.isApplicable(g3, new ItemStack(saw), 0, true), "g3 FURNITURE mutex green/black, other slots free");
		XWeaponModManager.install(g3, 0, new ItemStack(silencer), new ItemStack(saw), new ItemStack(black), new ItemStack(HbmItems.WEAPON_MOD_SCOPE.get()));
		check(problems, cases, rec(g3, 0).getRecoil(g3) == WeaponModPolymerFurniture.LAMBDA_RECOIL_G3 && XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_FURNITURE_BLACK)
				&& XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_SILENCER) && XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_NO_STOCK) && XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_SCOPE)
				&& XWeaponModManager.getUpgradeItems(g3, 0).length == 4
				&& g3.getHoverName().getContents() instanceof TranslatableContents t6 && t6.getKey().equals("item.atomiclegacy.gun_g3_infiltrator"), "g3 infiltrator (silencer + saw + black furniture + scope)");

		/// STACK_MAG, SKIN_SATURNITE ///
		for(Item gun : new Item[] {HbmItems.GUN_GREASEGUN.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(), HbmItems.GUN_ABERRATOR.get(), HbmItems.GUN_ABERRATOR_EOTT.get()}) {
			ItemStack s = new ItemStack(gun);
			for(int c = 0; c < ((ItemGunBaseNT) gun).getConfigCount(); c++) {
				int capacity = rec(s, c).getMagazine(s).getCapacity(s);
				Object type = rec(s, c).getMagazine(s).getType(s, null);
				Class<?> magClass = rec(s, c).getMagazine(s).getClass();
				rec(s, c).getMagazine(s).setAmount(s, capacity);
				XWeaponModManager.install(s, c, new ItemStack(stackMag));
				IMagazine<?> big = rec(s, c).getMagazine(s);
				check(problems, cases, big.getCapacity(s) == capacity * 3 / 2 && big.getClass() == magClass && big.getType(s, null) == type && big.getAmount(s, null) == (c == 0 ? capacity : capacity)
						&& XWeaponModManager.hasUpgrade(s, c, 214), name(gun) + " config " + c + " stacked magazine " + capacity + "->" + big.getCapacity(s));
				big.setAmount(s, capacity * 3 / 2);
				XWeaponModManager.uninstall(s, c);
				check(problems, cases, rec(s, c).getMagazine(s).getCapacity(s) == capacity && !XWeaponModManager.hasUpgrade(s, c, 214), name(gun) + " config " + c + " stacked magazine removal");
			}
		}
		// install() restores the receiver 0 magazine: the amount is clamped to the new capacity when the mod is dropped from the list
		ItemStack grease = new ItemStack(HbmItems.GUN_GREASEGUN.get());
		int greaseCap = rec(grease, 0).getMagazine(grease).getCapacity(grease);
		XWeaponModManager.install(grease, 0, new ItemStack(stackMag));
		rec(grease, 0).getMagazine(grease).setAmount(grease, greaseCap * 3 / 2);
		XWeaponModManager.install(grease, 0, new ItemStack(HbmItems.GREASEGUN_KIT.get()));
		check(problems, cases, rec(grease, 0).getMagazine(grease).getCapacity(grease) == greaseCap && rec(grease, 0).getMagazine(grease).getAmount(grease, null) == greaseCap
				&& !XWeaponModManager.hasUpgrade(grease, 0, 214), "greasegun magazine clamped after the stacked magazine is replaced");

		for(Item gun : new Item[] {HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get()}) {
			ItemStack s = new ItemStack(gun);
			float dura = cfg(s, 0).getDurability(s), damage = rec(s, 0).getBaseDamage(s);
			XWeaponModManager.install(s, 0, new ItemStack(silencer), new ItemStack(stackMag), new ItemStack(saturnite));
			ItemStack[] installed = XWeaponModManager.getUpgradeItems(s, 0);
			check(problems, cases, cfg(s, 0).getDurability(s) == dura * 5F && rec(s, 0).getBaseDamage(s) == damage + 3F && XWeaponModManager.hasUpgrade(s, 0, XWeaponModManager.ID_UZI_SATURN)
					&& installed.length == 3 && installed[0].is(saturnite), name(gun) + " saturnite skin values (additive priority first) " + dura + "/" + damage);
		}

		/// CANISTERS ///
		ItemStack drill = new ItemStack(HbmItems.GUN_DRILL.get());
		IMagazine<?> tank = rec(drill, 0).getMagazine(drill);
		int tankCap = tank.getCapacity(drill);
		tank.setAmount(drill, tankCap);
		XWeaponModManager.install(drill, 0, new ItemStack(canisters));
		IMagazine<?> bigTank = rec(drill, 0).getMagazine(drill);
		check(problems, cases, tank instanceof MagazineLiquidEngine && tankCap == 4_000 && bigTank instanceof MagazineLiquidEngine && bigTank.getCapacity(drill) == 12_000
				&& bigTank.getAmount(drill, null) == 4_000 && bigTank.getType(drill, null) == tank.getType(drill, null) && XWeaponModManager.hasUpgrade(drill, 0, 232), "drill canisters capacity");
		bigTank.setAmount(drill, 12_000);
		XWeaponModManager.install(drill, 0);
		check(problems, cases, rec(drill, 0).getMagazine(drill).getCapacity(drill) == 4_000 && rec(drill, 0).getMagazine(drill).getAmount(drill, null) == 12_000
				&& !XWeaponModManager.hasUpgrade(drill, 0, 232), "drill canisters removal (install() with an empty list returns before restoring the magazine, as in 1.7.10)");

		/// BAYONET: values and the stab ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		Object[][] bayonets = {
				{HbmItems.GUN_CARBINE.get(), XFactory762mm.ID_CARBINE_BAYONET, WeaponModCarbineBayonet.LAMBDA_CARBINE_ANIMS, WeaponModCarbineBayonet.ORCHESTRA_CARBINE},
				{HbmItems.GUN_MAS36.get(), XFactory762mm.ID_MAS_BAYONET, WeaponModMASBayonet.LAMBDA_MAS36_ANIMS, WeaponModMASBayonet.ORCHESTRA_MAS36},
		};
		for(Object[] b : bayonets) {
			Item gun = (Item) b[0];
			ItemStack plain = new ItemStack(gun);
			var secondary = cfg(plain, 0).getPressSecondary(plain);
			ItemStack s = modded(gun, bayonet);
			check(problems, cases, secondary != XFactory44.SMACK_A_FUCKER && cfg(s, 0).getPressSecondary(s) == XFactory44.SMACK_A_FUCKER && cfg(s, 0).getInspectDuration(s) == 30
					&& !cfg(s, 0).getInspectCancel(s) && cfg(s, 0).getAnims(s) == b[2] && cfg(s, 0).getOrchestra(s) == b[3] && XWeaponModManager.hasUpgrade(s, 0, (int) b[1])
					&& cfg(s, 0).getAnims(s).apply(s, GunAnimation.INSPECT) != null && cfg(s, 0).getAnims(s).apply(s, GunAnimation.CYCLE) != null
					&& rec(s, 0).getBaseDamage(s) == rec(plain, 0).getBaseDamage(plain), name(gun) + " bayonet values");

			player.getInventory().clearContent();
			player.getInventory().setItem(0, s);
			s = player.getInventory().getItem(0);
			tick(player, s, 1);
			tick(player, s, cfg(s, 0).getDrawDuration(s) + 1);
			Pig pig = EntityType.PIG.create(level);
			if(pig != null) {
				pig.setNoAi(true);
				pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
				pig.setHealth(1000F);
				pig.setPos(0.5D, 301.0D, 2.0D);
				level.addFreshEntity(pig);
				((ItemGunBaseNT) gun).handleKeybind(player, s, GunKeybind.GUN_SECONDARY, true);
				((ItemGunBaseNT) gun).handleKeybind(player, s, GunKeybind.GUN_SECONDARY, false);
				check(problems, cases, ItemGunBaseNT.getState(s, 0) == GunState.DRAWING && ItemGunBaseNT.getTimer(s, 0) == 30 && ItemGunBaseNT.getLastAnim(s, 0) == GunAnimation.INSPECT, name(gun) + " stab start");
				tick(player, s, 15);
				check(problems, cases, pig.getHealth() == 1000F, name(gun) + " stab not before timer 15");
				tick(player, s, 1);
				check(problems, cases, Math.abs(1000F - pig.getHealth() - 15F) < 0.01F && pig.getLastHurtByMob() == player, name(gun) + " stab damage " + (1000F - pig.getHealth()));
				pig.discard();
			}
		}

		/// Panzerschreck without the shield burns the shooter (HbmLivingProps.fire += 100) ///
		player.getInventory().clearContent();
		player.getInventory().setItem(0, pz);
		pz = player.getInventory().getItem(0);
		tick(player, pz, 1);
		tick(player, pz, cfg(pz, 0).getDrawDuration(pz) + 1);
		rec(pz, 0).getMagazine(pz).setAmount(pz, 1);
		HbmLivingHazards.get(player).clearFire();
		((ItemGunBaseNT) pz.getItem()).handleKeybind(player, pz, GunKeybind.GUN_PRIMARY, true);
		((ItemGunBaseNT) pz.getItem()).handleKeybind(player, pz, GunKeybind.GUN_PRIMARY, false);
		check(problems, cases, HbmLivingHazards.get(player).getFire() == 100 && rec(pz, 0).getMagazine(pz).getAmount(pz, null) == 0, "panzerschreck no-shield fire " + HbmLivingHazards.get(player).getFire());
		HbmLivingHazards.get(player).clearFire();
		player.getInventory().clearContent();
		for(var entity : level.getEntities((net.minecraft.world.entity.Entity) null, AREA, e -> !(e instanceof net.minecraft.world.entity.player.Player))) entity.discard();

		/// Weapon modification table ///
		WeaponTableMenu menu = new WeaponTableMenu(0, player.getInventory());
		menu.getSlot(7).set(new ItemStack(HbmItems.GUN_G3.get()));
		check(problems, cases, menu.getSlot(0).mayPlace(new ItemStack(green)) && menu.getSlot(0).mayPlace(new ItemStack(silencer)) && !menu.getSlot(0).mayPlace(new ItemStack(choke))
				&& !menu.getSlot(0).mayPlace(new ItemStack(bayonet)), "table accepts only the G3 mods");
		menu.getSlot(0).set(new ItemStack(green));
		check(problems, cases, !menu.getSlot(1).mayPlace(new ItemStack(black)) && menu.getSlot(1).mayPlace(new ItemStack(silencer)), "table FURNITURE mutex");
		menu.getSlot(1).set(new ItemStack(silencer));
		menu.getSlot(2).set(new ItemStack(saw));
		ItemStack taken = menu.getSlot(7).getItem();
		menu.getSlot(7).set(ItemStack.EMPTY);
		menu.getSlot(7).onTake(player, taken);
		check(problems, cases, XWeaponModManager.hasUpgrade(taken, 0, XWeaponModManager.ID_FURNITURE_GREEN) && XWeaponModManager.hasUpgrade(taken, 0, XWeaponModManager.ID_SILENCER)
				&& XWeaponModManager.hasUpgrade(taken, 0, XWeaponModManager.ID_NO_STOCK) && menu.mods.isEmpty(), "table install on take");
		menu.getSlot(7).set(taken);
		List<Item> shown = new ArrayList<>();
		for(int i = 0; i < 7; i++) if(!menu.mods.getItem(i).isEmpty()) shown.add(menu.mods.getItem(i).getItem());
		check(problems, cases, shown.size() == 3 && shown.containsAll(Arrays.asList(green, silencer, saw)), "table shows installed mods " + shown);
		player.getInventory().clearContent();
		menu.removed(player);
		check(problems, cases, menu.gun.isEmpty() && menu.mods.isEmpty() && XWeaponModManager.getUpgradeItems(taken, 0).length == 0
				&& rec(taken, 0).getFireSound(taken).get() != HbmSoundEvents.GUN_RIFLE_SILENCER.get(), "table close uninstalls");
		for(var drop : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AREA)) drop.discard();
		player.getInventory().clearContent();

		/// Crafting recipes (WeaponRecipes lines 138-164) ///
		ItemStack e = ItemStack.EMPTY;
		ItemStack plastic = item("ingot_polymer"), bolt = item("bolt_steel"), wsPlate = item("plate_weaponsteel"), steelPlate = item("plate_steel");
		recipe(problems, cases, server, level, "silencer", silencer, 1, 3, plastic, item("part_barrel_light_steel"), item("ingot_bakelite"));
		recipe(problems, cases, server, level, "saw", saw, 3, 2, bolt, bolt, new ItemStack(Items.STICK), bolt, item("plate_dura_steel"), new ItemStack(Items.STICK));
		recipe(problems, cases, server, level, "speedloader", speedloader, 3, 3, e, bolt, e, bolt, wsPlate, bolt, e, bolt, e);
		recipe(problems, cases, server, level, "slowdown", slowdown, 3, 3, e, item("ingot_weaponsteel"), e, e, item("part_mechanism_weaponsteel"), e, item("ingot_weaponsteel"), e, item("ingot_weaponsteel"));
		recipe(problems, cases, server, level, "speedup", speedup, 3, 3, wsPlate, item("ingot_gunmetal"), wsPlate, item("wire_dense_gold"), item("wire_dense_gold"), item("wire_dense_gold"),
				wsPlate, item("ingot_gunmetal"), wsPlate);
		recipe(problems, cases, server, level, "choke", choke, 1, 3, wsPlate, item("part_barrel_light_dura_steel"), wsPlate);
		recipe(problems, cases, server, level, "furniture_green", green, 3, 2, plastic, new ItemStack(Items.GREEN_DYE), item("part_stock_polymer"), e, e, item("part_grip_bakelite"));
		recipe(problems, cases, server, level, "furniture_black", black, 3, 2, plastic, new ItemStack(Items.BLACK_DYE), item("part_stock_bakelite"), e, e, item("part_grip_polymer"));
		recipe(problems, cases, server, level, "skin_saturnite", saturnite, 3, 2, item("part_barrel_light_saturnite"), item("part_receiver_light_saturnite"), item("part_mechanism_saturnite"),
				e, item("plate_saturnite"), e);
		recipe(problems, cases, server, level, "stack_mag", stackMag, 3, 3, wsPlate, e, wsPlate, wsPlate, e, wsPlate, wsPlate, item("part_mechanism_saturnite"), wsPlate);
		recipe(problems, cases, server, level, "bayonet", bayonet, 3, 2, e, e, steelPlate, bolt, bolt, bolt);
		recipe(problems, cases, server, level, "canisters", canisters, 3, 3, e, item("pipe_rubber"), e, item("canister_empty"), item("canister_empty"), item("canister_empty"),
				steelPlate, steelPlate, steelPlate);
		// wrong dye must not produce the other furniture
		var blackRecipe = server.getRecipeManager().byKey(AtomicLegacy.id("weapon_mod_special_furniture_black")).map(r -> r.value()).orElse(null);
		check(problems, cases, blackRecipe instanceof ShapedRecipe sr && !sr.matches(CraftingInput.of(3, 2, List.of(plastic, new ItemStack(Items.GREEN_DYE), item("part_stock_polymer"), e, e,
				item("part_grip_polymer"))), level), "black furniture rejects green dye");

		AtomicLegacy.LOGGER.info("HBM_GUN_MODS_G2_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	/** One IWeaponMod of a definition: id, slots, priority, mod item mapping and the complete gun list of the port. */
	private static void definition(List<String> problems, int[] cases, Item modItem, int id, String slot, int priority, Item... guns) {
		IWeaponMod mod = XWeaponModManager.idToMod.get(id);
		var def = XWeaponModManager.stackToMod.get(modItem);
		String name = name(modItem) + " id " + id;
		check(problems, cases, mod != null && def != null && Arrays.equals(mod.getSlots(), new String[] {slot}) && mod.getModPriority() == priority
				&& XWeaponModManager.modToStack.get(mod) != null && XWeaponModManager.modToStack.get(mod).is(modItem), name + " definition (slot " + slot + ", priority " + priority + ")");
		if(mod == null || def == null) return;
		List<Item> actual = new ArrayList<>();
		for(var entry : def.modByGun.entrySet()) if(entry.getValue() == mod) actual.add(entry.getKey());
		check(problems, cases, actual.size() == guns.length && actual.containsAll(Arrays.asList(guns)), name + " gun list " + actual.size() + "/" + guns.length);
		for(Item gun : guns) {
			ItemStack stack = new ItemStack(gun);
			ItemStack modStack = new ItemStack(modItem);
			boolean before = XWeaponModManager.isApplicable(stack, modStack, 0, true) && !XWeaponModManager.hasUpgrade(stack, 0, id)
					&& gun instanceof ItemGunBaseNT nt && nt.recognizedMods.contains(modItem);
			XWeaponModManager.install(stack, 0, modStack);
			ItemStack[] installed = XWeaponModManager.getUpgradeItems(stack, 0);
			boolean after = XWeaponModManager.hasUpgrade(stack, 0, id) && installed.length == 1 && installed[0].is(modItem)
					&& !XWeaponModManager.isApplicable(stack, modStack, 0, true) && XWeaponModManager.isApplicable(stack, modStack, 0, false);
			XWeaponModManager.uninstall(stack, 0);
			check(problems, cases, before && after && !XWeaponModManager.hasUpgrade(stack, 0, id) && XWeaponModManager.getUpgradeItems(stack, 0).length == 0,
					name + " install/uninstall on " + name(gun) + " " + before + "/" + after);
		}
	}

	private static void recipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String mod, Item result, int w, int h, ItemStack... grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id("weapon_mod_special_" + mod)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, Arrays.asList(grid));
		ItemStack out = recipe instanceof ShapedRecipe s && s.matches(input, level) ? s.assemble(input, server.registryAccess()) : ItemStack.EMPTY;
		check(problems, cases, out.is(result) && out.getCount() == 1, "weapon_mod_special_" + mod + " recipe");
	}

	private static ItemStack modded(Item gun, Item... mods) {
		ItemStack stack = new ItemStack(gun);
		ItemStack[] stacks = new ItemStack[mods.length];
		for(int i = 0; i < mods.length; i++) stacks[i] = new ItemStack(mods[i]);
		XWeaponModManager.install(stack, 0, stacks);
		return stack;
	}

	private static GunConfig cfg(ItemStack stack, int index) { return ((ItemGunBaseNT) stack.getItem()).getConfig(stack, index); }
	private static Receiver rec(ItemStack stack, int index) { return cfg(stack, index).getReceivers(stack)[0]; }
	private static String name(Item item) { return BuiltInRegistries.ITEM.getKey(item).getPath(); }

	private static void tick(FakePlayer player, ItemStack stack, int ticks) {
		for(int i = 0; i < ticks; i++) stack.getItem().inventoryTick(stack, player.level(), player, 0, true);
	}

	private static ItemStack item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(Items.BARRIER));
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("weapon mods G2: " + message);
	}
}
