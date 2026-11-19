package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.HashBiMap;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 XWeaponModManager. Only the mod definitions inside the agreed scope
 * are registered: GREASEGUN (M3 refurbishment, id 206). The 1.7.10 metadata
 * variant weapon_mod_special:GREASEGUN is the flattened item
 * weapon_mod_special_greasegun, so definitions are keyed by item.
 */
public class XWeaponModManager {

	public static final String KEY_MOD_LIST = "KEY_MOD_LIST_";

	/** Mapping of mods to IDs, keep the register order consistent! */
	public static HashBiMap<Integer, IWeaponMod> idToMod = HashBiMap.create();
	/** Mapping of mod items to mod definitions */
	public static HashMap<Item, WeaponModDefinition> stackToMod = new HashMap<>();
	/** Map for turning individual mods back into their item form, used when uninstaling mods */
	public static HashMap<IWeaponMod, ItemStack> modToStack = new HashMap<>();

	/** Original mod IDs referenced by ported renderers/name mutators; the mod items themselves are not ported. */
	public static final int ID_SILENCER = 201;
	public static final int ID_SCOPE = 202;
	/** [weapons batch E1] WeaponModPanzerschreckSawedOff (EnumModSpecial.SAW on gun_panzerschreck); the mod item is not ported. */
	public static final int ID_NO_SHIELD = 204;
	public static final int ID_NO_STOCK = 205;
	public static final int ID_GREASEGUN_CLEAN = 206;
	public static final int ID_FURNITURE_GREEN = 211;
	public static final int ID_FURNITURE_BLACK = 212;
	public static final int ID_UZI_SATURN = 215;
	/** [weapons batch F2] WeaponModNickel ids of gun_n_i_4_n_i (EnumModSpecial.NICKEL / DOUBLOONS, +2 coins each); the mod items are not ported. */
	public static final int ID_NI4NI_NICKEL = 220;
	public static final int ID_NI4NI_DOUBLOONS = 221;
	// [weapon mods G3: ids] 1.7.10 XWeaponModManager ids of the laser rifle, drill bit and drill engine mods
	public static final int ID_LAS_SHOTGUN = 216;
	public static final int ID_LAS_CAPACITOR = 217;
	public static final int ID_LAS_AUTO = 218;
	public static final int ID_DRILL_HSS = 222;
	public static final int ID_DRILL_WSTEEL = 223;
	public static final int ID_DRILL_TCALLOY = 224;
	public static final int ID_DRILL_SATURN = 225;
	public static final int ID_ENGINE_DIESEL = 226;
	public static final int ID_ENGINE_AVIATION = 227;
	public static final int ID_ENGINE_ELECTRIC = 228;
	public static final int ID_ENGINE_TURBO = 229;
	// [weapon mods G3: ids end]

	private static boolean initialized = false;

	/** Assigns the IWeaponMod instances to items. Called lazily once items are registered. */
	public static void init() {
		if(initialized) return;
		initialized = true;
		// [weapon mods G1: test + generic] weapon_mod_test (ids 0-9) and weapon_mod_generic (ids 100-117), registered before weapon_mod_special as in 1.7.10
		XWeaponModsG1.registerTestAndGeneric();
		// 1.7.10 registration order (ids are stored on guns).
		// SCOPE: original list is heavy_revolver, carbine, g3, mas36, charge_thrower; add the others when ported.
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SCOPE.get())).addMod(new Item[] {HbmItems.GUN_G3.get()}, new WeaponModScope(ID_SCOPE));
		// [weapons batch C1: mods]
		// XWeaponModManager line 135: SCOPE also applies to gun_heavy_revolver (same WeaponModScope instance, id 202)
		stackToMod.get(HbmItems.WEAPON_MOD_SCOPE.get()).addMod(HbmItems.GUN_HEAVY_REVOLVER.get(), idToMod.get(ID_SCOPE));
		// [weapons batch C2: mods]
		// [weapons batch C3: mods]
		// SCOPE (original list: heavy_revolver, carbine, g3, mas36, charge_thrower): same definition and WeaponModScope instance
		stackToMod.get(HbmItems.WEAPON_MOD_SCOPE.get()).addMod(new Item[] {HbmItems.GUN_CARBINE.get(), HbmItems.GUN_MAS36.get()}, idToMod.get(ID_SCOPE));
		// [weapons batch C4: mods]
		new WeaponModDefinition(new ItemStack(HbmItems.GREASEGUN_KIT.get())).addMod(HbmItems.GUN_GREASEGUN.get(), new WeaponModGreasegun(ID_GREASEGUN_CLEAN));
		// [weapon mods G2: registrations] XWeaponModManager lines 133-145/163 (ids 200, 201, 203-205, 207-215, 219, 232).
		// [parity c1] gun_autoshotgun_heretic (CHOKE) and [parity d2] gun_minigun_dual (SLOWDOWN, SPEEDUP) are ported.
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SPEEDLOADER.get())).addMod(HbmItems.GUN_LIBERATOR.get(), new WeaponModLiberatorSpeedloader(200));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SILENCER.get())).addMod(new Item[] {HbmItems.GUN_AM180.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(), HbmItems.GUN_STAR_F.get(), HbmItems.GUN_STAR_F_AKIMBO.get(), HbmItems.GUN_G3.get(), HbmItems.GUN_AMAT.get()}, new WeaponModSilencer(ID_SILENCER));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SAW.get()))
			.addMod(new Item[] {HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_DOUBLE_BARREL.get()}, new WeaponModSawedOff(com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga.ID_SAWED_OFF))
			.addMod(HbmItems.GUN_PANZERSCHRECK.get(), new WeaponModPanzerschreckSawedOff(ID_NO_SHIELD))
			.addMod(new Item[] {HbmItems.GUN_G3.get(), HbmItems.GUN_G3_ZEBRA.get()}, new WeapnModG3SawedOff(ID_NO_STOCK));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SLOWDOWN.get())).addMod(new Item[] {HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_MINIGUN_DUAL.get() /* [parity d2] */}, new WeaponModSlowdown(com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm.ID_MINIGUN_SLOWDOWN));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SPEEDUP.get()))
			.addMod(new Item[] {HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_MINIGUN_DUAL.get() /* [parity d2] */}, new WeaponModMinigunSpeedup(com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm.ID_MINIGUN_SPEED))
			.addMod(new Item[] {HbmItems.GUN_AUTOSHOTGUN.get(), HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(), HbmItems.GUN_MK108.get()}, new WeaponModShredderSpeedup(209));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_CHOKE.get())).addMod(new Item[] {HbmItems.GUN_PEPPERBOX.get(), HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_DOUBLE_BARREL.get(), HbmItems.GUN_LIBERATOR.get(), HbmItems.GUN_SPAS12.get(), HbmItems.GUN_AUTOSHOTGUN_SEXY.get(), HbmItems.GUN_AUTOSHOTGUN_HERETIC.get() /* [parity c1] */}, new WeaponModChoke(210));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_FURNITURE_GREEN.get())).addMod(HbmItems.GUN_G3.get(), new WeaponModPolymerFurniture(ID_FURNITURE_GREEN));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_FURNITURE_BLACK.get())).addMod(HbmItems.GUN_G3.get(), new WeaponModPolymerFurniture(ID_FURNITURE_BLACK));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_BAYONET.get()))
			.addMod(HbmItems.GUN_MAS36.get(), new WeaponModMASBayonet(com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm.ID_MAS_BAYONET))
			.addMod(HbmItems.GUN_CARBINE.get(), new WeaponModCarbineBayonet(com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm.ID_CARBINE_BAYONET));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_STACK_MAG.get())).addMod(new Item[] {HbmItems.GUN_GREASEGUN.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get(), HbmItems.GUN_ABERRATOR.get(), HbmItems.GUN_ABERRATOR_EOTT.get()}, new WeaponModStackMag(214));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SKIN_SATURNITE.get())).addMod(new Item[] {HbmItems.GUN_UZI.get(), HbmItems.GUN_UZI_AKIMBO.get()}, new WeaponModUziSaturnite(ID_UZI_SATURN));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_CANISTERS.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModCanisters(232));
		// [weapon mods G2: registrations end]
		// [weapon mods G3: mods] 1.7.10 XWeaponModManager lines 153-168 (LAS_SHOTGUN .. SIFTER), original order and ids
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_LAS_SHOTGUN.get())).addMod(new Item[] {HbmItems.GUN_LASRIFLE.get()}, new WeaponModLasShotgun(ID_LAS_SHOTGUN));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_LAS_CAPACITOR.get())).addMod(new Item[] {HbmItems.GUN_LASRIFLE.get()}, new WeaponModLasCapacitor(ID_LAS_CAPACITOR));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_LAS_AUTO.get())).addMod(new Item[] {HbmItems.GUN_LASRIFLE.get()}, new WeaponModLasAuto(ID_LAS_AUTO));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_NICKEL.get())).addMod(new Item[] {HbmItems.GUN_N_I_4_N_I.get()}, new WeaponModNickel(ID_NI4NI_NICKEL, "COIN1"));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_DOUBLOONS.get())).addMod(new Item[] {HbmItems.GUN_N_I_4_N_I.get()}, new WeaponModNickel(ID_NI4NI_DOUBLOONS, "COIN2"));

		// ToolMaterial.EMERALD.ordinal() = 3
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_DRILL_HSS.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModDrill(ID_DRILL_HSS).damage(1.25F).dt(3F).pierce(0.15F).harvest(3));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_DRILL_WEAPONSTEEL.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModDrill(ID_DRILL_WSTEEL).damage(1.5F).dt(5F).pierce(0.2F).aoe(2).harvest(3));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_DRILL_TCALLOY.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModDrill(ID_DRILL_TCALLOY).damage(2F).dt(7.5F).pierce(0.2F).reach(2).aoe(3).harvest(3 + 1));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_DRILL_SATURNITE.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModDrill(ID_DRILL_SATURN).damage(3F).dt(10F).pierce(0.25F).reach(2).aoe(3).harvest(3 + 2));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_ENGINE_DIESEL.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModEngine(ID_ENGINE_DIESEL).mag(WeaponModEngine.ENGINE_DIESEL).delay(15));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_ENGINE_AVIATION.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModEngine(ID_ENGINE_AVIATION).mag(WeaponModEngine.ENGINE_AVIATION).delay(10));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_ENGINE_ELECTRIC.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModEngine(ID_ENGINE_ELECTRIC).mag(WeaponModEngine.ENGINE_ELECTRIC).delay(15));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_ENGINE_TURBO.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModEngine(ID_ENGINE_TURBO).mag(WeaponModEngine.ENGINE_TURBO).delay(5));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_MAGNET.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModDrillFortune(230, "MAGNET", 2));
		new WeaponModDefinition(new ItemStack(HbmItems.WEAPON_MOD_SIFTER.get())).addMod(new Item[] {HbmItems.GUN_DRILL.get()}, new WeaponModDrillFortune(231, "SIFTER", 1));
		// [weapon mods G3: mods end]
		// [weapon mods G1: caliber] weapon_mod_caliber (ids 300-371), registered after every weapon_mod_special as in 1.7.10 (keep last)
		XWeaponModsG1.registerCaliber();
	}

	public static ItemStack[] getUpgradeItems(ItemStack stack, int cfg) {
		init();
		CompoundTag tag = ItemGunBaseNT.readTag(stack);
		if(tag == null) return new ItemStack[0];
		int[] modIds = tag.getIntArray(KEY_MOD_LIST + cfg);
		if(modIds.length == 0) return new ItemStack[0];
		List<ItemStack> mods = new ArrayList<>();
		for(int modId : modIds) {
			IWeaponMod mod = idToMod.get(modId);
			if(mod != null) {
				ItemStack item = modToStack.get(mod);
				if(item != null) mods.add(item.copy());
			}
		}
		return mods.toArray(new ItemStack[0]);
	}

	public static boolean hasUpgrade(ItemStack stack, int cfg, int id) {
		CompoundTag tag = ItemGunBaseNT.readTag(stack);
		if(tag == null) return false;
		int[] modIds = tag.getIntArray(KEY_MOD_LIST + cfg);
		for(int modId : modIds) {
			if(modId == id) return true;
		}
		return false;
	}

	private static Object prevMagType;
	private static int prevMagCount;
	private static boolean changedMagState = false;

	public static void changedMagState() {
		changedMagState = true;
	}

	/** Saves the state on receiver 0 so that if the mag changes through upgrading, the state may potentially be restored, if compatible */
	private static void saveMagState(ItemStack stack, int cfg) {
		IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
		if(mag == null) return;
		prevMagType = mag.getType(stack, null);
		prevMagCount = mag.getAmount(stack, null);
	}

	private static void restoreMagState(ItemStack stack, int cfg) {
		if(!changedMagState) return;
		changedMagState = false;

		IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
		if(mag == null) return;
		if(mag.getType(stack, null) == prevMagType) {
			mag.setAmount(stack, Mth.clamp(prevMagCount, 0, mag.getCapacity(stack)));
		} else {
			mag.setAmount(stack, 0);
		}
	}

	/**
	 * Saves the mag state on receiver 0, uninstalls all existing mods to ensure there's no double install calls,
	 * then installs the mods. If a mag state change has been reported, the mag on receiver 0 is validated,
	 * i.e. if the type is still the same, the amount is restored, otherwise the mag is cleared.
	 */
	public static void install(ItemStack stack, int cfg, ItemStack... mods) {
		init();
		saveMagState(stack, cfg);
		// we need to always clear things, so existing mods aren't installed twice, i.e. enchantment levels applied twice
		uninstall(stack, cfg);

		List<IWeaponMod> toInstall = new ArrayList<>();

		for(ItemStack mod : mods) {
			if(mod == null || mod.isEmpty()) continue;
			WeaponModDefinition def = stackToMod.get(mod.getItem());
			if(def != null) {
				IWeaponMod forGun = def.modByGun.get(stack.getItem());
				if(forGun != null) {
					toInstall.add(forGun);
				} else {
					forGun = def.modByGun.get(null);
					if(forGun != null) toInstall.add(forGun);
				}
			}
		}
		if(toInstall.isEmpty()) return;
		toInstall.sort(modSorter);
		int[] modIds = new int[toInstall.size()];
		for(int i = 0; i < modIds.length; i++) {
			IWeaponMod mod = toInstall.get(i);
			modIds[i] = idToMod.inverse().get(mod);
			onInstallStack(stack, modToStack.get(mod), cfg);
		}
		ItemGunBaseNT.updateTag(stack, tag -> tag.putIntArray(KEY_MOD_LIST + cfg, modIds));
		restoreMagState(stack, cfg);
	}

	/** Wipes all mods from the gun */
	public static void uninstall(ItemStack stack, int cfg) {
		if(stack != null && ItemGunBaseNT.readTag(stack) != null) {
			for(ItemStack mod : getUpgradeItems(stack, cfg)) {
				XWeaponModManager.onUninstallStack(stack, mod, cfg);
			}
			ItemGunBaseNT.updateTag(stack, tag -> tag.remove(KEY_MOD_LIST + cfg));
		}
	}

	public static void onInstallStack(ItemStack gun, ItemStack mod, int cfg) {
		IWeaponMod newMod = modFromStack(gun, mod, cfg);
		if(newMod == null) return;
		newMod.onInstall(gun, mod, cfg);
	}

	public static void onUninstallStack(ItemStack gun, ItemStack mod, int cfg) {
		IWeaponMod newMod = modFromStack(gun, mod, cfg);
		if(newMod == null) return;
		newMod.onUninstall(gun, mod, cfg);
	}

	public static IWeaponMod modFromStack(ItemStack gun, ItemStack mod, int cfg) {
		init();
		if(gun == null || mod == null || gun.isEmpty() || mod.isEmpty()) return null;
		WeaponModDefinition def = stackToMod.get(mod.getItem());
		if(def == null) return null;
		IWeaponMod newMod = def.modByGun.get(gun.getItem());
		if(newMod == null) newMod = def.modByGun.get(null);
		return newMod;
	}

	public static boolean isApplicable(ItemStack gun, ItemStack mod, int cfg, boolean checkMutex) {
		IWeaponMod newMod = modFromStack(gun, mod, cfg);
		if(newMod == null) return false; //if there's just no mod applicable

		CompoundTag tag = ItemGunBaseNT.readTag(gun);
		if(checkMutex && tag != null) for(int i : tag.getIntArray(KEY_MOD_LIST + cfg)) {
			IWeaponMod iMod = idToMod.get(i);
			if(iMod != null) for(String mutex0 : newMod.getSlots()) for(String mutex1 : iMod.getSlots()) {
				if(mutex0.equals(mutex1)) return false; //if any of the mod's slots are already taken
			}
		}

		return true; //yippie!
	}

	public static Comparator<IWeaponMod> modSorter = (o1, o2) -> o2.getModPriority() - o1.getModPriority();

	/** Scrapes all upgrades, iterates over them and evaluates the given value. The parent (i.e. holder of the base value)
	 * is passed for context (so upgrades can differentiate primary and secondary receivers for example). Passing a null
	 * stack causes the base value to be returned. */
	public static <T> T eval(T base, ItemStack stack, String key, Object parent, int cfg) {
		if(stack == null) return base;
		CompoundTag tag = ItemGunBaseNT.readTag(stack);
		if(tag == null) return base;
		int[] modIds = tag.getIntArray(KEY_MOD_LIST + cfg);
		if(modIds.length == 0) return base;
		init();

		for(int i : modIds) {
			IWeaponMod mod = idToMod.get(i);
			if(mod != null) base = mod.eval(base, stack, key, parent);
		}

		return base;
	}

	public static class WeaponModDefinition {

		/** Holds the weapon mod handlers for each given gun. Key null refers to mods that apply to ALL guns that are otherwise not included. */
		public HashMap<Item, IWeaponMod> modByGun = new HashMap<>();
		public ItemStack stack;

		public WeaponModDefinition(ItemStack stack) {
			this.stack = stack;
			stackToMod.put(stack.getItem(), this);
		}

		public WeaponModDefinition addMod(Item gun, IWeaponMod mod) {
			modByGun.put(gun, mod);
			modToStack.put(mod, stack);
			if(gun instanceof ItemGunBaseNT nt) {
				if(!nt.recognizedMods.contains(stack.getItem())) nt.recognizedMods.add(stack.getItem());
			}
			return this;
		}

		public WeaponModDefinition addMod(Item[] gun, IWeaponMod mod) { for(Item item : gun) addMod(item, mod); return this; }

		public WeaponModDefinition addDefault(IWeaponMod mod) {
			return addMod((Item) null, mod);
		}
	}
}
