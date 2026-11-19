package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory9mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch A (gun_lag, gun_uzi, gun_uzi_akimbo,
 * gun_g3, gun_stg77) against the pinned 1.7.10 XFactory9mm / XFactory556mm values.
 */
final class GunBatchADiagnostics {

	private GunBatchADiagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 8);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		// Registration, quality, tab placement
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int greaseIndex = indexOf(tab, HbmItems.GUN_GREASEGUN.get());
		int ammoIndex = indexOf(tab, HbmItems.AMMO_STANDARD.get());
		Object[][] guns = {
				{ HbmItems.GUN_UZI.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.P9_SP, 30 },
				{ HbmItems.GUN_UZI_AKIMBO.get(), WeaponQuality.B_SIDE, 2, StandardAmmoType.P9_SP, 60 },
				{ HbmItems.GUN_G3.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.R556_SP, 30 },
				{ HbmItems.GUN_LAG.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.P9_JHP, 17 },
				{ HbmItems.GUN_STG77.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.R556_FMJ, 30 },
		};
		int last = greaseIndex;
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String id = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, id + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(gun).getMaxStackSize() == 1, id + " stack size");
			check(problems, cases, gun.quality == g[1] && gun.getConfigCount() == (int) g[2], id + " quality/config count");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, def.is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(def) == g[3] && def.getCount() == (int) g[4], id + " default ammo " + def);
			int index = indexOf(tab, item);
			if(g[1] != WeaponQuality.A_SIDE) {
				check(problems, cases, index < 0, id + " (" + g[1] + ") must not be in the weapon tab");
				continue;
			}
			check(problems, cases, index > last && (ammoIndex < 0 || index < ammoIndex), id + " weapon tab position " + index + " (prev " + last + ", ammo " + ammoIndex + ")");
			last = Math.max(last, index);
		}

		// Recipes
		var akimboRecipe = server.getRecipeManager().byKey(AtomicLegacy.id("gun_uzi_akimbo")).map(h -> h.value()).orElse(null);
		ItemStack mech = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id("part_mechanism_weaponsteel")).map(Item::getDefaultInstance).orElse(ItemStack.EMPTY);
		CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(HbmItems.GUN_UZI.get()), mech, new ItemStack(HbmItems.GUN_UZI.get())));
		check(problems, cases, !mech.isEmpty() && akimboRecipe instanceof ShapedRecipe shaped && shaped.matches(input, level)
				&& shaped.assemble(input, server.registryAccess()).is(HbmItems.GUN_UZI_AKIMBO.get())
				&& shaped.assemble(input, server.registryAccess()).getCount() == 1, "gun_uzi_akimbo recipe UMU failed");
		// gun_uzi / gun_g3 / gun_lag / gun_stg77 recipes: GunPartsDiagnostics

		// Config values
		ItemGunBaseNT uzi = (ItemGunBaseNT) HbmItems.GUN_UZI.get();
		ItemGunBaseNT akimbo = (ItemGunBaseNT) HbmItems.GUN_UZI_AKIMBO.get();
		ItemGunBaseNT g3 = (ItemGunBaseNT) HbmItems.GUN_G3.get();
		ItemGunBaseNT lag = (ItemGunBaseNT) HbmItems.GUN_LAG.get();
		ItemGunBaseNT stg = (ItemGunBaseNT) HbmItems.GUN_STG77.get();
		check(problems, cases, HbmConfig.longPistolDamage() == 25F && HbmConfig.uziDamage() == 3F && HbmConfig.uziAkimboLeftDamage() == 3F
				&& HbmConfig.uziAkimboRightDamage() == 3F && HbmConfig.assaultRifleDamage() == 5F && HbmConfig.stg77Damage() == 10F, "config damage defaults");
		checkGun(problems, cases, "uzi", uzi, 0, 3000F, 15, 31, Crosshair.CIRCLE, 3F, 2, 25, true, 0.005F, 55, 50, 30, XFactory9mm.p9_sp);
		checkGun(problems, cases, "uzi_akimbo/0", akimbo, 0, 3000F, 15, 31, Crosshair.CIRCLE, 3F, 2, 25, true, 0.005F, 55, 50, 30, XFactory9mm.p9_sp);
		checkGun(problems, cases, "uzi_akimbo/1", akimbo, 1, 3000F, 15, 31, Crosshair.CIRCLE, 3F, 2, 25, true, 0.005F, 55, 50, 30, XFactory9mm.p9_sp);
		checkGun(problems, cases, "g3", g3, 0, 3000F, 10, 33, Crosshair.CIRCLE, 5F, 2, 15, true, 0F, 50, 47, 30, XFactory556mm.r556_sp);
		checkGun(problems, cases, "lag", lag, 0, 1700F, 7, 31, Crosshair.CIRCLE, 25F, 4, 10, false, 0.005F, 53, 44, 17, XFactory9mm.p9_sp);
		checkGun(problems, cases, "stg77", stg, 0, 3000F, 10, 125, Crosshair.CIRCLE, 10F, 2, 15, true, 0F, 46, 0, 30, XFactory556mm.r556_sp);
		Receiver akL = akimbo.getConfig(null, 0).getReceivers(null)[0];
		Receiver akR = akimbo.getConfig(null, 1).getReceivers(null)[0];
		check(problems, cases, akL.getHipfireSpread(null) == 0F && akR.getHipfireSpread(null) == 0F
				&& akL.getProjectileOffset(null).z == 0.375D && akR.getProjectileOffset(null).z == -0.375D, "akimbo hipfire/offsets");
		check(problems, cases, stg.getConfig(null, 0).getScopeTexture(null) != null && g3.getConfig(null, 0).getScopeTexture(null) == null, "scope textures");
		check(problems, cases, XFactory556mm.r556_ap.casingEffect == XFactory556mm.CASING_R556_STEEL && XFactory556mm.r556_sp.casingEffect == XFactory556mm.CASING_R556, "r556 casings");

		FakePlayer player = FakePlayerFactory.getMinecraft(level);

		// UZI: reload 55 ticks, auto fire every 2 ticks
		{
			ItemStack gun = setup(player, uzi, StandardAmmoType.P9_SP, 64);
			IMagazine<?> mag = uzi.getConfig(gun, 0).getReceivers(gun)[0].getMagazine(gun);
			tick(uzi, gun, player, 1);
			check(problems, cases, ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.EQUIP && ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, "uzi equip");
			press(uzi, player, gun, GunKeybind.RELOAD);
			check(problems, cases, ItemGunBaseNT.getState(gun, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(gun, 0) == 55, "uzi reload start");
			tick(uzi, gun, player, 55);
			check(problems, cases, mag.getAmount(gun, null) == 30 && player.getInventory().getItem(1).getCount() == 34, "uzi reload result " + mag.getAmount(gun, null));
			tick(uzi, gun, player, 1);
			discardBullets(level);
			uzi.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
			tick(uzi, gun, player, 10);
			uzi.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
			check(problems, cases, mag.getAmount(gun, null) == 24 && countBullets(level) == 6, "uzi auto fire mag=" + mag.getAmount(gun, null) + " bullets=" + countBullets(level));
			discardBullets(level);
		}

		// UZI AKIMBO: one reload press reloads both configs; primary fires config 0, secondary config 1
		{
			ItemStack gun = setup(player, akimbo, StandardAmmoType.P9_SP, 64);
			IMagazine<?> magL = akimbo.getConfig(gun, 0).getReceivers(gun)[0].getMagazine(gun);
			IMagazine<?> magR = akimbo.getConfig(gun, 1).getReceivers(gun)[0].getMagazine(gun);
			tick(akimbo, gun, player, 1);
			check(problems, cases, ItemGunBaseNT.getState(gun, 0) == GunState.IDLE && ItemGunBaseNT.getState(gun, 1) == GunState.IDLE, "akimbo equip");
			press(akimbo, player, gun, GunKeybind.RELOAD);
			check(problems, cases, ItemGunBaseNT.getState(gun, 0) == GunState.RELOADING && ItemGunBaseNT.getState(gun, 1) == GunState.RELOADING, "akimbo reload start");
			tick(akimbo, gun, player, 56);
			check(problems, cases, magL.getAmount(gun, null) == 30 && magR.getAmount(gun, null) == 30 && player.getInventory().getItem(1).getCount() == 4, "akimbo reload L=" + magL.getAmount(gun, null) + " R=" + magR.getAmount(gun, null));
			discardBullets(level);
			akimbo.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
			tick(akimbo, gun, player, 4);
			akimbo.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
			check(problems, cases, magL.getAmount(gun, null) == 27 && magR.getAmount(gun, null) == 30 && ItemGunBaseNT.getWear(gun, 0) == 3F && ItemGunBaseNT.getWear(gun, 1) == 0F, "akimbo primary L=" + magL.getAmount(gun, null) + " R=" + magR.getAmount(gun, null));
			tick(akimbo, gun, player, 2);
			akimbo.handleKeybind(player, gun, GunKeybind.GUN_SECONDARY, true);
			tick(akimbo, gun, player, 4);
			akimbo.handleKeybind(player, gun, GunKeybind.GUN_SECONDARY, false);
			check(problems, cases, magL.getAmount(gun, null) == 27 && magR.getAmount(gun, null) == 27 && ItemGunBaseNT.getLastAnim(gun, 1) == GunAnimation.CYCLE, "akimbo secondary L=" + magL.getAmount(gun, null) + " R=" + magR.getAmount(gun, null));
			check(problems, cases, countBullets(level) == 6, "akimbo bullets " + countBullets(level));
			discardBullets(level);
		}

		// G3: secondary toggles mode 1 (semi), auto refire only in mode 0
		{
			ItemStack gun = setup(player, g3, StandardAmmoType.R556_SP, 64);
			IMagazine<?> mag = g3.getConfig(gun, 0).getReceivers(gun)[0].getMagazine(gun);
			tick(g3, gun, player, 1);
			press(g3, player, gun, GunKeybind.RELOAD);
			check(problems, cases, ItemGunBaseNT.getTimer(gun, 0) == 50, "g3 reload start");
			tick(g3, gun, player, 51);
			check(problems, cases, mag.getAmount(gun, null) == 30 && mag.getType(gun, null) == XFactory556mm.r556_sp, "g3 reload result");
			discardBullets(level);
			g3.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
			tick(g3, gun, player, 6);
			g3.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
			tick(g3, gun, player, 2);
			check(problems, cases, mag.getAmount(gun, null) == 26 && countBullets(level) == 4, "g3 auto mag=" + mag.getAmount(gun, null));
			press(g3, player, gun, GunKeybind.GUN_SECONDARY);
			check(problems, cases, ItemGunBaseNT.getMode(gun, 0) == 1, "g3 mode switch");
			g3.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
			tick(g3, gun, player, 6);
			g3.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
			check(problems, cases, mag.getAmount(gun, null) == 25, "g3 semi mag=" + mag.getAmount(gun, null));
			tick(g3, gun, player, 2);
			press(g3, player, gun, GunKeybind.GUN_SECONDARY);
			check(problems, cases, ItemGunBaseNT.getMode(gun, 0) == 0, "g3 mode switch back");
			discardBullets(level);
		}

		// LAG: semi fire; firing during the inspect animation shoots the player instead of spawning a bullet
		{
			ItemStack gun = setup(player, lag, StandardAmmoType.P9_JHP, 64);
			IMagazine<?> mag = lag.getConfig(gun, 0).getReceivers(gun)[0].getMagazine(gun);
			tick(lag, gun, player, 1);
			press(lag, player, gun, GunKeybind.RELOAD);
			check(problems, cases, ItemGunBaseNT.getTimer(gun, 0) == 53, "lag reload start");
			tick(lag, gun, player, 54);
			check(problems, cases, mag.getAmount(gun, null) == 17 && mag.getType(gun, null) == XFactory9mm.p9_jhp, "lag reload result " + mag.getAmount(gun, null));
			discardBullets(level);
			lag.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
			tick(lag, gun, player, 8);
			lag.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
			check(problems, cases, mag.getAmount(gun, null) == 16 && countBullets(level) == 1, "lag semi fire mag=" + mag.getAmount(gun, null));
			discardBullets(level);
			mag.setAmount(gun, 17);
			player.getInventory().setItem(1, ItemStack.EMPTY);
			press(lag, player, gun, GunKeybind.RELOAD);
			check(problems, cases, ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.INSPECT && ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, "lag inspect state=" + ItemGunBaseNT.getState(gun, 0)); // inspectCancel_DNA defaults to true: inspecting keeps the gun IDLE
			tick(lag, gun, player, 32);
			press(lag, player, gun, GunKeybind.GUN_PRIMARY);
			check(problems, cases, mag.getAmount(gun, null) == 16 && countBullets(level) == 0 && ItemGunBaseNT.getState(gun, 0) == GunState.COOLDOWN
					&& ItemGunBaseNT.getTimer(gun, 0) == 4 && ItemGunBaseNT.getWear(gun, 0) == 2F, "lag inspect self-shot mag=" + mag.getAmount(gun, null) + " bullets=" + countBullets(level));
			discardBullets(level);
		}

		// STG77: both buttons fire (secondary refires), tertiary toggles aim, never jams
		{
			ItemStack gun = setup(player, stg, StandardAmmoType.R556_FMJ, 64);
			IMagazine<?> mag = stg.getConfig(gun, 0).getReceivers(gun)[0].getMagazine(gun);
			tick(stg, gun, player, 1);
			press(stg, player, gun, GunKeybind.RELOAD);
			check(problems, cases, ItemGunBaseNT.getTimer(gun, 0) == 46, "stg77 reload start");
			tick(stg, gun, player, 47);
			check(problems, cases, mag.getAmount(gun, null) == 30 && mag.getType(gun, null) == XFactory556mm.r556_fmj, "stg77 reload result");
			discardBullets(level);
			stg.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
			tick(stg, gun, player, 6);
			stg.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
			check(problems, cases, mag.getAmount(gun, null) == 29, "stg77 primary is semi mag=" + mag.getAmount(gun, null));
			tick(stg, gun, player, 2);
			stg.handleKeybind(player, gun, GunKeybind.GUN_SECONDARY, true);
			tick(stg, gun, player, 6);
			stg.handleKeybind(player, gun, GunKeybind.GUN_SECONDARY, false);
			check(problems, cases, mag.getAmount(gun, null) == 25 && countBullets(level) == 5, "stg77 secondary auto mag=" + mag.getAmount(gun, null));
			tick(stg, gun, player, 2);
			press(stg, player, gun, GunKeybind.GUN_TERTIARY);
			check(problems, cases, ItemGunBaseNT.getIsAiming(gun), "stg77 aim toggle");
			ItemGunBaseNT.setWear(gun, 0, 3000F);
			press(stg, player, gun, GunKeybind.RELOAD);
			tick(stg, gun, player, 47);
			check(problems, cases, ItemGunBaseNT.getState(gun, 0) != GunState.JAMMED && mag.getAmount(gun, null) == 30, "stg77 reload at full wear " + ItemGunBaseNT.getState(gun, 0));
			discardBullets(level);
		}

		player.getInventory().clearContent();
		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_A_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static void checkGun(List<String> problems, int[] cases, String name, ItemGunBaseNT gun, int index, float dura, int draw, int inspect, Crosshair crosshair,
			float damage, int delay, int dry, boolean auto, float spread, int reload, int jam, int capacity, BulletConfig first) {
		GunConfig cfg = gun.getConfig(null, index);
		Receiver rec = cfg.getReceivers(null)[0];
		check(problems, cases, cfg.getDurability(null) == dura && cfg.getDrawDuration(null) == draw && cfg.getInspectDuration(null) == inspect && cfg.getCrosshair(null) == crosshair, name + " dura/draw/inspect/crosshair");
		check(problems, cases, rec.getBaseDamage(null) == damage && rec.getDelayAfterFire(null) == delay && rec.getDelayAfterDryFire(null) == dry
				&& rec.getRefireOnHold(null) == auto && rec.getInnateSpread(null) == spread && rec.getReloadBeginDuration(null) == reload
				&& rec.getReloadEndDuration(null) == 0 && rec.getJamDuration(null) == jam, name + " receiver values");
		check(problems, cases, rec.getMagazine(null).getCapacity(null) == capacity && rec.getMagazine(null).getType(new ItemStack(gun), null) == first, name + " magazine");
	}

	private static ItemStack setup(FakePlayer player, ItemGunBaseNT item, StandardAmmoType ammo, int count) {
		player.getInventory().clearContent();
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		player.getInventory().setItem(0, new ItemStack(item));
		player.getInventory().setItem(1, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), ammo).copyWithCount(count));
		return player.getInventory().getItem(0);
	}

	private static void press(ItemGunBaseNT item, FakePlayer player, ItemStack gun, GunKeybind key) {
		item.handleKeybind(player, gun, key, true);
		item.handleKeybind(player, gun, key, false);
	}

	private static void tick(ItemGunBaseNT item, ItemStack stack, FakePlayer player, int ticks) {
		for(int i = 0; i < ticks; i++) item.inventoryTick(stack, player.level(), player, 0, true);
	}

	private static int countBullets(ServerLevel level) {
		int n = 0;
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) if(!bullet.isRetired()) n++;
		return n;
	}

	private static void discardBullets(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun batch A: " + message);
	}
}
