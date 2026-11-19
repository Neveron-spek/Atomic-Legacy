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
import com.atomiclegacy.items.weapon.sedna.factory.GunFactory;
import com.atomiclegacy.items.weapon.sedna.factory.GunStateDecider;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.Orchestras;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC3;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.registry.HbmWeaponModsG1;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * [parity d2: diagnostics] The three DEBUG items of the original (no creative tab, no recipe): gun_debug with its
 * item ammo_debug (GunFactory) and gun_minigun_dual (XFactory762mm), against the pinned 1.7.10 values.
 */
final class ParityD2GunDiagnostics {

	private ParityD2GunDiagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 12);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		try {
			registration(server, problems, cases);
			debugValues(problems, cases);
			dualValues(problems, cases);
			mods(problems, cases);
			cycles(level, problems, cases);
		} catch(Exception exception) {
			problems.add("parity d2 guns: diagnostics threw " + exception);
			AtomicLegacy.LOGGER.error("parity d2 gun diagnostics", exception);
		}
		return cases[0];
	}

	private static void registration(MinecraftServer server, List<String> problems, int[] cases) {
		Item ammo = HbmItems.AMMO_DEBUG.get();
		Item debug = HbmItems.GUN_DEBUG.get();
		Item dual = HbmItems.GUN_MINIGUN_DUAL.get();
		check(problems, cases, BuiltInRegistries.ITEM.getKey(ammo).equals(AtomicLegacy.id("ammo_debug")) && BuiltInRegistries.ITEM.getKey(debug).equals(AtomicLegacy.id("gun_debug"))
				&& BuiltInRegistries.ITEM.getKey(dual).equals(AtomicLegacy.id("gun_minigun_dual")), "registry ids");
		// ModItems.ammo_debug = new Item(): plain item, stack size 64
		check(problems, cases, ammo.getClass() == Item.class && new ItemStack(ammo).getMaxStackSize() == 64, "ammo_debug is a plain Item (64)");
		check(problems, cases, debug instanceof ItemGunBaseNT g && g.quality == WeaponQuality.DEBUG && g.getConfigCount() == 1 && new ItemStack(debug).getMaxStackSize() == 1
				&& g.getDefaultAmmo().isEmpty(), "gun_debug quality DEBUG / one config / no default ammo");
		check(problems, cases, dual instanceof ItemGunBaseNT g && g.quality == WeaponQuality.DEBUG && g.getConfigCount() == 2 && new ItemStack(dual).getMaxStackSize() == 1
				&& StandardAmmoItem.standardType(g.getDefaultAmmo()) == StandardAmmoType.R762_SP && g.getDefaultAmmo().getCount() == 50, "gun_minigun_dual quality DEBUG / two configs / default ammo R762_SP x50");
		check(problems, cases, !ItemGunBaseNT.secrets.contains(debug) && !ItemGunBaseNT.secrets.contains(dual), "DEBUG guns are not secrets");

		// hidden: in no creative tab
		StringBuilder listed = new StringBuilder();
		for(CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
			List<ItemStack> entries = new ArrayList<>(tab.getDisplayItems());
			entries.addAll(tab.getSearchTabDisplayItems());
			for(ItemStack s : entries) {
				if(s.is(ammo) || s.is(debug) || s.is(dual)) {
					listed.append(' ').append(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).append('@').append(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab));
				}
			}
		}
		check(problems, cases, listed.length() == 0, "DEBUG items listed in creative tabs:" + listed);
		java.util.Properties sourceTabs = new java.util.Properties();
		try(var stream = com.atomiclegacy.registry.HbmCreativeTabs.class.getResourceAsStream("creative-tabs.properties")) {
			if(stream != null) sourceTabs.load(stream);
		} catch(java.io.IOException exception) {
			problems.add("parity d2 guns: creative-tabs.properties unreadable " + exception);
		}
		check(problems, cases, "hidden".equals(sourceTabs.getProperty("ammo_debug")) && "hidden".equals(sourceTabs.getProperty("gun_debug"))
				&& "hidden".equals(sourceTabs.getProperty("gun_minigun_dual")), "creative-tabs.properties marks the three DEBUG items hidden");

		// no recipe produces them
		StringBuilder crafted = new StringBuilder();
		for(var holder : server.getRecipeManager().getRecipes()) {
			ItemStack result;
			try {
				result = holder.value().getResultItem(server.registryAccess());
			} catch(RuntimeException exception) {
				continue;
			}
			if(result != null && (result.is(ammo) || result.is(debug) || result.is(dual))) crafted.append(' ').append(holder.id());
		}
		check(problems, cases, crafted.length() == 0, "recipes producing DEBUG items:" + crafted);
	}

	/** GunFactory lines 40-60. */
	private static void debugValues(List<String> problems, int[] cases) {
		// ammo_debug: setSpread(0.01F).setRicochetAngle(45); ammo_debug_shot: setSpread(0.05F).setProjectiles(6).setRicochetAngle(45); BulletConfig defaults otherwise
		StandardAmmoType a = StandardAmmoType.DEBUG, b = StandardAmmoType.DEBUG_SHOT;
		check(problems, cases, GunFactory.ammo_debug.ammo == a && a.projectileSpread() == 0.01F && a.ricochetAngle() == 45F && a.projectileCount() == 1 && a.projectileCountMax() == 1,
				"ammo_debug spread/ricochet/projectiles " + a.projectileSpread() + "/" + a.ricochetAngle() + "/" + a.projectileCount());
		check(problems, cases, GunFactory.ammo_debug_shot.ammo == b && b.projectileSpread() == 0.05F && b.ricochetAngle() == 45F && b.projectileCount() == 6 && b.projectileCountMax() == 6,
				"ammo_debug_shot spread/ricochet/projectiles " + b.projectileSpread() + "/" + b.ricochetAngle() + "/" + b.projectileCount());
		for(StandardAmmoType t : new StandardAmmoType[] { a, b }) {
			check(problems, cases, t.damageMultiplier() == 1F && t.headshotMultiplier() == 1.25F && !t.penetrates() && t.damageFalloffByPenetration() && t.armorThresholdNegation() == 0F
					&& t.armorPiercingPercent() == 0F && t.maxRicochetCount() == 2 && t.projectileSpeed() == 10D && t.projectileGravity() == 0D && t.projectileLifetime() == 30
					&& t.onImpact() == null && !t.blackPowder(), t.id() + " BulletConfig defaults");
		}
		ItemStack ammoStack = new ItemStack(HbmItems.AMMO_DEBUG.get());
		for(BulletConfig c : new BulletConfig[] { GunFactory.ammo_debug, GunFactory.ammo_debug_shot }) {
			check(problems, cases, c.matches(ammoStack) && !c.matches(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.M44_SP)) && !c.matches(ItemStack.EMPTY)
					&& c.toStack().is(HbmItems.AMMO_DEBUG.get()) && c.wear == 1F && c.ammoReloadCount == 1 && c.casingEffect == GunFactory.CASING_DEBUG && c.casingStack().isEmpty()
					&& BulletConfig.configs.contains(c), c.ammo.id() + " config item/wear/casing");
		}
		check(problems, cases, GunFactory.CASING_DEBUG == 42, "CASING_DEBUG ordinal");

		ItemGunBaseNT gun = (ItemGunBaseNT) HbmItems.GUN_DEBUG.get();
		ItemStack stack = new ItemStack(gun);
		GunConfig cfg = gun.getConfig(stack, 0);
		check(problems, cases, cfg.getDurability(stack) == 600F && cfg.getDrawDuration(stack) == 15 && cfg.getInspectDuration(stack) == 23 && cfg.getCrosshair(stack) == Crosshair.L_CLASSIC
				&& cfg.getSmokeHandler(stack) == Lego.LAMBDA_STANDARD_SMOKE && cfg.getOrchestra(stack) == Orchestras.DEBUG_ORCHESTRA && cfg.getAnims(stack) == Lego.LAMBDA_DEBUG_ANIMS
				&& cfg.getDecider(stack) == GunFactory.LAMBDA_DEBUG_DECIDER && cfg.getPressPrimary(stack) == Lego.LAMBDA_STANDARD_CLICK_PRIMARY && cfg.getPressSecondary(stack) != null
				&& cfg.getPressReload(stack) == Lego.LAMBDA_STANDARD_RELOAD && cfg.getPressTertiary(stack) == Lego.LAMBDA_TOGGLE_AIM, "gun_debug config values");
		Receiver[] recs = cfg.getReceivers(stack);
		check(problems, cases, recs.length == 2, "gun_debug has two receivers");
		if(recs.length != 2) return;
		float[] damage = { 10F, 5F };
		BulletConfig[] rounds = { GunFactory.ammo_debug, GunFactory.ammo_debug_shot };
		for(int i = 0; i < 2; i++) {
			Receiver rec = recs[i];
			IMagazine<?> mag = rec.getMagazine(stack);
			check(problems, cases, rec.getBaseDamage(stack) == damage[i] && rec.getDelayAfterFire(stack) == 14 && rec.getDelayAfterDryFire(stack) == 14 && rec.getReloadBeginDuration(stack) == 46
					&& rec.getReloadCycleDuration(stack) == 46 && rec.getReloadEndDuration(stack) == 0 && rec.getJamDuration(stack) == 23 && !rec.getRefireOnHold(stack)
					&& rec.getRoundsPerCycle(stack) == 1 && rec.getFireSound(stack) == HbmSoundEvents.GUN_HEAVY_REVOLVER_FIRE && rec.getFireVolume(stack) == 1F && rec.getFirePitch(stack) == 1F
					&& rec.getProjectileOffset(stack).equals(new Vec3(0.75, -0.0625, -0.3125D)) && rec.getProjectileOffsetScoped(stack).equals(new Vec3(0.75, -0.0625, 0)),
					"gun_debug receiver " + i + " values (damage " + rec.getBaseDamage(stack) + ", reload " + rec.getReloadBeginDuration(stack) + "/" + rec.getReloadEndDuration(stack) + ")");
			check(problems, cases, mag instanceof MagazineFullReload full && full.index == i && full.capacity == 12 && full.acceptedBullets.equals(List.of(rounds[i])),
					"gun_debug receiver " + i + " magazine (full reload, index " + i + ", 12, one config)");
		}
		check(problems, cases, recs[0].getCanFire(stack) == Lego.LAMBDA_STANDARD_CAN_FIRE && recs[0].getOnFire(stack) == Lego.LAMBDA_STANDARD_FIRE
				&& recs[1].getCanFire(stack) == Lego.LAMBDA_SECOND_CAN_FIRE && recs[1].getOnFire(stack) == Lego.LAMBDA_SECOND_FIRE, "gun_debug fire lambdas");
		check(problems, cases, Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.CYCLE) != null && Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.CYCLE_DRY) != null
				&& Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.EQUIP) != null && Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.RELOAD) != null
				&& Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.INSPECT) != null && Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.JAMMED) != null
				&& Lego.LAMBDA_DEBUG_ANIMS.apply(stack, GunAnimation.RELOAD_END) == null, "gun_debug animation set");
	}

	/** XFactory762mm lines 41-42, 123-144. */
	private static void dualValues(List<String> problems, int[] cases) {
		check(problems, cases, HbmConfig.GUN_MINIGUN_DUAL_LEFT_DAMAGE.getDefault() == 6D && HbmConfig.GUN_MINIGUN_DUAL_RIGHT_DAMAGE.getDefault() == 6D
				&& HbmConfig.minigunDualLeftDamage() == 6F && HbmConfig.minigunDualRightDamage() == 6F, "GUN_MINIGUN_DUAL_LEFT/RIGHT_DAMAGE defaults");
		ItemGunBaseNT gun = (ItemGunBaseNT) HbmItems.GUN_MINIGUN_DUAL.get();
		ItemStack stack = new ItemStack(gun);
		List<BulletConfig> r762 = List.of(XFactory762mm.r762_sp, XFactory762mm.r762_fmj, XFactory762mm.r762_jhp, XFactory762mm.r762_ap, XFactory762mm.r762_du, XFactory762mm.r762_he);
		double[] side = { 0.25D, -0.25D };
		for(int i = 0; i < 2; i++) {
			GunConfig cfg = gun.getConfig(stack, i);
			Receiver[] recs = cfg.getReceivers(stack);
			check(problems, cases, cfg.index == i && cfg.getDurability(stack) == 50_000F && cfg.getDrawDuration(stack) == 20 && cfg.getInspectDuration(stack) == 20
					&& cfg.getCrosshair(stack) == Crosshair.L_CIRCLE && cfg.getSmokeHandler(stack) == XFactory762mm.LAMBDA_SMOKE && cfg.getAnims(stack) == XFactory762mm.LAMBDA_MINIGUN_ANIMS
					&& cfg.getOrchestra(stack) == OrchestrasC3.ORCHESTRA_MINIGUN_DUAL && cfg.getPressReload(stack) == Lego.LAMBDA_STANDARD_RELOAD && cfg.getPressTertiary(stack) == null
					&& recs.length == 1, "gun_minigun_dual config " + i + " values");
			if(recs.length != 1) continue;
			Receiver rec = recs[0];
			IMagazine<?> mag = rec.getMagazine(stack);
			check(problems, cases, rec.getBaseDamage(stack) == 6F && rec.getDelayAfterFire(stack) == 1 && rec.getDelayAfterDryFire(stack) == 15 && rec.getRefireOnHold(stack)
					&& rec.getInnateSpread(stack) == 0.01F && rec.getRoundsPerCycle(stack) == 1 && rec.getFireSound(stack) == HbmSoundEvents.GUN_MINIGUN_FIRE
					&& rec.getFireVolume(stack) == 1F && rec.getFirePitch(stack) == 1F && rec.getProjectileOffset(stack).equals(new Vec3(1, -0.0625 * 2.5, side[i]))
					&& rec.getCanFire(stack) == Lego.LAMBDA_STANDARD_CAN_FIRE && rec.getOnFire(stack) == Lego.LAMBDA_STANDARD_FIRE && rec.getRecoil(stack) == XFactory762mm.LAMBDA_RECOIL_MINIGUN,
					"gun_minigun_dual config " + i + " receiver values (damage " + rec.getBaseDamage(stack) + ", side " + rec.getProjectileOffset(stack).z + ")");
			check(problems, cases, mag instanceof MagazineBelt belt && belt.acceptedBullets.equals(r762), "gun_minigun_dual config " + i + " belt accepts the six r762 rounds");
		}
		GunConfig left = gun.getConfig(stack, 0), right = gun.getConfig(stack, 1);
		check(problems, cases, left.getPressPrimary(stack) == Lego.LAMBDA_STANDARD_CLICK_PRIMARY && left.getPressSecondary(stack) == null && left.getDecider(stack) == GunStateDecider.LAMBDA_STANDARD_DECIDER
				&& right.getPressPrimary(stack) == null && right.getPressSecondary(stack) == Lego.LAMBDA_STANDARD_CLICK_PRIMARY && right.getDecider(stack) == XFactory762mm.LAMBDA_SECOND_MINIGUN,
				"gun_minigun_dual keys: left gun on primary, right gun on secondary");
	}

	/** XWeaponModManager lines 141, 143, 201, 207: SLOWDOWN (207), SPEEDUP (208), caliber R556 (352) and BMG50 (371). */
	private static void mods(List<String> problems, int[] cases) {
		XWeaponModManager.init();
		ItemGunBaseNT gun = (ItemGunBaseNT) HbmItems.GUN_MINIGUN_DUAL.get();
		Item slowdown = HbmItems.WEAPON_MOD_SLOWDOWN.get(), speedup = HbmItems.WEAPON_MOD_SPEEDUP.get();
		Item r556 = HbmWeaponModsG1.caliber("r556").get(), bmg50 = HbmWeaponModsG1.caliber("bmg50").get();
		ItemStack plain = new ItemStack(gun);
		check(problems, cases, XWeaponModManager.isApplicable(plain, new ItemStack(slowdown), 0, true) && XWeaponModManager.isApplicable(plain, new ItemStack(speedup), 0, true)
				&& XWeaponModManager.isApplicable(plain, new ItemStack(r556), 0, true) && XWeaponModManager.isApplicable(plain, new ItemStack(bmg50), 0, true)
				&& gun.recognizedMods.contains(slowdown) && gun.recognizedMods.contains(speedup) && gun.recognizedMods.contains(r556) && gun.recognizedMods.contains(bmg50),
				"gun_minigun_dual accepts SLOWDOWN / SPEEDUP / caliber R556 / BMG50");
		check(problems, cases, !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_DEBUG.get()), new ItemStack(slowdown), 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_DEBUG.get()), new ItemStack(speedup), 0, true)
				&& !XWeaponModManager.isApplicable(plain, new ItemStack(HbmItems.WEAPON_MOD_CHOKE.get()), 0, true), "mods outside the original gun lists are rejected");

		// WeaponModSlowdown: delay * 2, innate spread 0; WeaponModMinigunSpeedup: rounds * 3, innate spread * 1.5
		ItemStack slow = new ItemStack(gun);
		XWeaponModManager.install(slow, 0, new ItemStack(slowdown));
		Receiver rec = gun.getConfig(slow, 0).getReceivers(slow)[0];
		check(problems, cases, XWeaponModManager.hasUpgrade(slow, 0, XFactory762mm.ID_MINIGUN_SLOWDOWN) && rec.getDelayAfterFire(slow) == 2 && rec.getInnateSpread(slow) == 0F
				&& rec.getRoundsPerCycle(slow) == 1 && !XWeaponModManager.isApplicable(slow, new ItemStack(speedup), 0, true),
				"gun_minigun_dual slowdown values (delay " + rec.getDelayAfterFire(slow) + ", spread " + rec.getInnateSpread(slow) + ")");
		ItemStack fast = new ItemStack(gun);
		XWeaponModManager.install(fast, 0, new ItemStack(speedup));
		rec = gun.getConfig(fast, 0).getReceivers(fast)[0];
		check(problems, cases, XWeaponModManager.hasUpgrade(fast, 0, XFactory762mm.ID_MINIGUN_SPEED) && rec.getRoundsPerCycle(fast) == 3 && Math.abs(rec.getInnateSpread(fast) - 0.015F) < 1.0E-6F
				&& rec.getDelayAfterFire(fast) == 1, "gun_minigun_dual speedup values (rounds " + rec.getRoundsPerCycle(fast) + ", spread " + rec.getInnateSpread(fast) + ")");
	}

	private static void cycles(ServerLevel level, List<String> problems, int[] cases) {
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		/// gun_debug: reload fills both magazines from ammo_debug, primary fires receiver 0, secondary receiver 1 ///
		Item ammo = HbmItems.AMMO_DEBUG.get();
		ItemStack debug = equip(player, HbmItems.GUN_DEBUG.get(), new ItemStack(ammo, 30));
		ItemGunBaseNT gun = (ItemGunBaseNT) debug.getItem();
		Receiver[] recs = gun.getConfig(debug, 0).getReceivers(debug);
		IMagazine<?> mag0 = recs[0].getMagazine(debug), mag1 = recs[1].getMagazine(debug);
		check(problems, cases, ItemGunBaseNT.getState(debug, 0) == GunState.IDLE && mag0.getAmount(debug, null) == 0 && mag1.getAmount(debug, null) == 0, "gun_debug drawn and empty");
		press(player, debug, GunKeybind.GUN_PRIMARY);
		release(player, debug, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).isEmpty() && ItemGunBaseNT.getLastAnim(debug, 0) == GunAnimation.CYCLE_DRY && ItemGunBaseNT.getTimer(debug, 0) == 14, "gun_debug dry fire (14 ticks)");
		tick(player, debug, 15);
		press(player, debug, GunKeybind.RELOAD);
		release(player, debug, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(debug, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(debug, 0) == 46 && ItemGunBaseNT.getLastAnim(debug, 0) == GunAnimation.RELOAD,
				"gun_debug reload start (46 ticks) state " + ItemGunBaseNT.getState(debug, 0) + " timer " + ItemGunBaseNT.getTimer(debug, 0));
		tick(player, debug, 48);
		check(problems, cases, mag0.getAmount(debug, null) == 12 && mag1.getAmount(debug, null) == 12 && count(player, ammo) == 6
				&& mag0.getType(debug, null) == GunFactory.ammo_debug && mag1.getType(debug, null) == GunFactory.ammo_debug_shot,
				"gun_debug reload: both magazines 12, 6 rounds left (" + mag0.getAmount(debug, null) + "/" + mag1.getAmount(debug, null) + "/" + count(player, ammo) + ")");
		check(problems, cases, mag0.getIconForHUD(debug, player).is(ammo) && mag1.getIconForHUD(debug, player).is(ammo) && mag0.getCasing(debug, null) == GunFactory.CASING_DEBUG, "gun_debug HUD icon / casing");
		tick(player, debug, 4);
		check(problems, cases, ItemGunBaseNT.getState(debug, 0) == GunState.IDLE, "gun_debug idle after reload: " + ItemGunBaseNT.getState(debug, 0));

		press(player, debug, GunKeybind.GUN_PRIMARY);
		release(player, debug, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.DEBUG && Math.abs(bullets.get(0).baseDamage() - 10F) < 0.001F
				&& mag0.getAmount(debug, null) == 11 && mag1.getAmount(debug, null) == 12 && ItemGunBaseNT.getState(debug, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(debug, 0) == 14
				&& ItemGunBaseNT.getLastAnim(debug, 0) == GunAnimation.CYCLE && ItemGunBaseNT.getWear(debug, 0) == 1F,
				"gun_debug primary shot: bullets " + bullets.size() + " mag " + mag0.getAmount(debug, null) + " timer " + ItemGunBaseNT.getTimer(debug, 0));
		discard(level);
		tick(player, debug, 15);
		check(problems, cases, ItemGunBaseNT.getState(debug, 0) == GunState.IDLE && bullets(level).isEmpty(), "gun_debug is not automatic");

		press(player, debug, GunKeybind.GUN_SECONDARY);
		release(player, debug, GunKeybind.GUN_SECONDARY);
		bullets = bullets(level);
		boolean shot = bullets.size() == 6;
		for(LegacyBulletEntity bullet : bullets) shot &= bullet.ammoType() == StandardAmmoType.DEBUG_SHOT && Math.abs(bullet.baseDamage() - 5F) < 0.001F;
		check(problems, cases, shot && mag0.getAmount(debug, null) == 11 && mag1.getAmount(debug, null) == 11 && ItemGunBaseNT.getTimer(debug, 0) == 14 && ItemGunBaseNT.getWear(debug, 0) == 2F,
				"gun_debug secondary shot: bullets " + bullets.size() + " mag " + mag1.getAmount(debug, null));
		discard(level);
		tick(player, debug, 15);
		press(player, debug, GunKeybind.GUN_TERTIARY);
		release(player, debug, GunKeybind.GUN_TERTIARY);
		check(problems, cases, ItemGunBaseNT.getIsAiming(debug), "gun_debug tertiary toggles aim");

		/// gun_minigun_dual: belt fed from the inventory, left gun on primary, right gun on secondary ///
		ItemStack dual = equip(player, HbmItems.GUN_MINIGUN_DUAL.get(), StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.R762_SP).copyWithCount(10));
		gun = (ItemGunBaseNT) dual.getItem();
		IMagazine<?> beltLeft = gun.getConfig(dual, 0).getReceivers(dual)[0].getMagazine(dual), beltRight = gun.getConfig(dual, 1).getReceivers(dual)[0].getMagazine(dual);
		check(problems, cases, ItemGunBaseNT.getState(dual, 0) == GunState.IDLE && ItemGunBaseNT.getState(dual, 1) == GunState.IDLE
				&& beltLeft.getAmount(dual, player.getInventory()) == 10 && beltRight.getAmount(dual, player.getInventory()) == 10
				&& beltLeft.getType(dual, player.getInventory()) == XFactory762mm.r762_sp, "gun_minigun_dual drawn, both belts read the inventory");
		press(player, dual, GunKeybind.GUN_PRIMARY);
		release(player, dual, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.R762_SP && Math.abs(bullets.get(0).baseDamage() - 6F) < 0.001F && bullets.get(0).getX() > player.getX()
				&& ItemGunBaseNT.getState(dual, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(dual, 0) == 1 && ItemGunBaseNT.getState(dual, 1) == GunState.IDLE
				&& countStandard(player, StandardAmmoType.R762_SP) == 9, "gun_minigun_dual primary fires the left gun only: bullets " + bullets.size() + " left " + countStandard(player, StandardAmmoType.R762_SP));
		discard(level);
		tick(player, dual, 3);
		press(player, dual, GunKeybind.GUN_SECONDARY);
		release(player, dual, GunKeybind.GUN_SECONDARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.R762_SP && Math.abs(bullets.get(0).baseDamage() - 6F) < 0.001F && bullets.get(0).getX() < player.getX()
				&& ItemGunBaseNT.getState(dual, 1) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(dual, 1) == 1 && ItemGunBaseNT.getState(dual, 0) == GunState.IDLE
				&& countStandard(player, StandardAmmoType.R762_SP) == 8, "gun_minigun_dual secondary fires the right gun only: bullets " + bullets.size() + " left " + countStandard(player, StandardAmmoType.R762_SP));
		discard(level);
		tick(player, dual, 3);

		// both keys held: each gun refires every tick until the shared belt is empty
		press(player, dual, GunKeybind.GUN_PRIMARY);
		press(player, dual, GunKeybind.GUN_SECONDARY);
		tick(player, dual, 6);
		release(player, dual, GunKeybind.GUN_PRIMARY);
		release(player, dual, GunKeybind.GUN_SECONDARY);
		check(problems, cases, countStandard(player, StandardAmmoType.R762_SP) == 0 && bullets(level).size() == 8, "gun_minigun_dual auto fire of both guns empties the belt: bullets " + bullets(level).size()
				+ " left " + countStandard(player, StandardAmmoType.R762_SP));
		discard(level);
		player.getInventory().clearContent();
	}

	private static ItemStack equip(FakePlayer player, Item gun, ItemStack... ammo) {
		player.getInventory().clearContent();
		player.getInventory().setItem(0, new ItemStack(gun));
		for(int i = 0; i < ammo.length; i++) player.getInventory().setItem(i + 1, ammo[i]);
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

	private static int count(FakePlayer player, Item item) {
		int count = 0;
		for(int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if(s.is(item)) count += s.getCount();
		}
		return count;
	}

	private static int countStandard(FakePlayer player, StandardAmmoType type) {
		int count = 0;
		for(int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if(s.is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(s) == type) count += s.getCount();
		}
		return count;
	}

	private static List<LegacyBulletEntity> bullets(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
	}

	private static void discard(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("parity d2 guns: " + message);
	}
}
