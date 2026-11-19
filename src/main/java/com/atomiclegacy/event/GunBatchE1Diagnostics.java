package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.AmmoFamily;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasE1;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory75Bolt;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryRocket;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch E1 (XFactoryRocket: gun_panzerschreck, gun_quadro, gun_fliegerfaust,
 * gun_missile_launcher and the rocket configs; XFactory75Bolt: gun_bolter and the b75 rounds) against the pinned
 * 1.7.10 values.
 */
final class GunBatchE1Diagnostics {

	private GunBatchE1Diagnostics() {
	}

	private static final AABB AREA = new AABB(-24, 280, -24, 24, 320, 40);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_PANZERSCHRECK.get(), WeaponQuality.A_SIDE, StandardAmmoType.ROCKET_HE, 3 },
				{ HbmItems.GUN_QUADRO.get(), WeaponQuality.A_SIDE, StandardAmmoType.ROCKET_HE, 4 },
				{ HbmItems.GUN_FLIEGERFAUST.get(), WeaponQuality.SPECIAL, StandardAmmoType.ROCKET_MICRO, 9 },
				{ HbmItems.GUN_MISSILE_LAUNCHER.get(), WeaponQuality.A_SIDE, StandardAmmoType.ROCKET_HEAT, 5 },
				{ HbmItems.GUN_BOLTER.get(), WeaponQuality.SPECIAL, StandardAmmoType.B75, 15 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == 1
					&& !ItemGunBaseNT.secrets.contains(item), name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, StandardAmmoItem.standardType(def) == g[2] && def.getCount() == (int) g[3], name + " default ammo " + def);
		}

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int spas = indexOf(tab, HbmItems.GUN_SPAS12.get()), pz = indexOf(tab, HbmItems.GUN_PANZERSCHRECK.get()), starf = indexOf(tab, HbmItems.GUN_STAR_F.get());
		int auto = indexOf(tab, HbmItems.GUN_AUTOSHOTGUN.get()), qd = indexOf(tab, HbmItems.GUN_QUADRO.get()), lag = indexOf(tab, HbmItems.GUN_LAG.get());
		int mini = indexOf(tab, HbmItems.GUN_MINIGUN.get()), ml = indexOf(tab, HbmItems.GUN_MISSILE_LAUNCHER.get()), stg = indexOf(tab, HbmItems.GUN_STG77.get());
		int ff = indexOf(tab, HbmItems.GUN_FLIEGERFAUST.get()), bolt = indexOf(tab, HbmItems.GUN_BOLTER.get()), dbl = indexOf(tab, HbmItems.GUN_DOUBLE_BARREL.get());
		check(problems, cases, spas >= 0 && spas < pz && pz < starf && auto < qd && qd < lag && mini < ml && ml < stg && stg < ff && ff < bolt && bolt < dbl,
				"weapon tab order (ModItems) " + spas + "/" + pz + "/" + starf + " " + auto + "/" + qd + "/" + lag + " " + mini + "/" + ml + "/" + stg + "/" + ff + "/" + bolt + "/" + dbl);

		/// Ammo values (BulletConfig) ///
		float[] mult = {1F, 0.5F, 0.75F, 0.75F, 0.75F};
		BulletImpact[] impacts = {XFactoryRocket.LAMBDA_STANDARD_EXPLODE, XFactoryRocket.LAMBDA_STANDARD_EXPLODE_HEAT, XFactoryRocket.LAMBDA_STANDARD_EXPLODE_DEMO,
				XFactoryRocket.LAMBDA_STANDARD_EXPLODE_INC, XFactoryRocket.LAMBDA_STANDARD_EXPLODE_PHOSPHORUS};
		StandardAmmoType[] items = {StandardAmmoType.ROCKET_HE, StandardAmmoType.ROCKET_HEAT, StandardAmmoType.ROCKET_DEMO, StandardAmmoType.ROCKET_INC, StandardAmmoType.ROCKET_PHOSPHORUS};
		StandardAmmoType[][] sets = {XFactoryRocket.RPZB, XFactoryRocket.QD, XFactoryRocket.ML};
		BulletConfig[][] cfgSets = {XFactoryRocket.rocket_rpzb, XFactoryRocket.rocket_qd, XFactoryRocket.rocket_ml};
		for(int s = 0; s < sets.length; s++) {
			for(int i = 0; i < 5; i++) {
				StandardAmmoType t = sets[s][i];
				check(problems, cases, t.family() == AmmoFamily.LEGACY_BULLET && t.projectileSpeed() == 0D && t.projectileGravity() == 0D && t.projectileLifetime() == (s == 1 ? 400 : 300)
						&& t.selfDamageDelay() == 10 && t.damageMultiplier() == mult[i] && !t.penetrates() && t.armorThresholdNegation() == 0F && t.armorPiercingPercent() == 0F
						&& t.headshotMultiplier() == 1.25F && t.onImpact() == impacts[i] && t.onUpdate() == (s == 1 ? XFactoryRocket.UPDATE_STEERING : XFactoryRocket.UPDATE_STANDARD)
						&& t.onEntityHit() == XFactoryRocket.LAMBDA_NONE && t.onRicochet() == XFactoryRocket.LAMBDA_NONE && t.projectileCount() == 1 && t.projectileSpread() == 0F, t.id() + " values");
				BulletConfig c = cfgSets[s][i];
				check(problems, cases, c.ammo == t && c.matches(stack(items[i], 1)) && !c.matches(stack(items[(i + 1) % 5], 1)) && StandardAmmoItem.standardType(c.toStack()) == items[i]
						&& c.casingEffect == -1 && c.wear == 1F && c.ammoReloadCount == 1, t.id() + " config item/casing");
			}
		}
		StandardAmmoType rf = StandardAmmoType.ROCKET_FF;
		check(problems, cases, rf.projectileSpeed() == 7D && rf.projectileGravity() == 0D && rf.projectileLifetime() == 300 && rf.selfDamageDelay() == 10 && rf.damageMultiplier() == 1F
				&& rf.onUpdate() == null && rf.onImpact() == XFactoryRocket.LAMBDA_MICRO_EXPLODE && rf.onEntityHit() == XFactoryRocket.LAMBDA_NONE
				&& XFactoryRocket.rocket_ff.matches(stack(StandardAmmoType.ROCKET_MICRO, 1)) && XFactoryRocket.rocket_ff.ammo == rf, "rocket_ff values");
		check(problems, cases, StandardAmmoType.ROCKET_HE.onImpact() == null && StandardAmmoType.ROCKET_HE.onUpdate() == null && StandardAmmoType.ROCKET_HE.family() == AmmoFamily.ROCKET_ML,
				"turret_richard rockets unchanged");
		StandardAmmoType[] b75 = {StandardAmmoType.B75, StandardAmmoType.B75_INC, StandardAmmoType.B75_EXP};
		float[] b75mult = {1F, 0.8F, 1.5F};
		float[] b75ap = {0F, 0.1F, -0.25F};
		BulletImpact[] b75imp = {XFactory75Bolt.LAMBDA_TINY_EXPLODE, XFactory12ga.LAMBDA_PHOSPHORUS, XFactory75Bolt.LAMBDA_STANDARD_EXPLODE};
		BulletConfig[] b75cfg = {XFactory75Bolt.b75, XFactory75Bolt.b75_inc, XFactory75Bolt.b75_exp};
		for(int i = 0; i < 3; i++) {
			StandardAmmoType t = b75[i];
			check(problems, cases, t.family() == AmmoFamily.B75 && t.projectileSpeed() == 10D && t.projectileGravity() == 0D && t.projectileLifetime() == 30 && t.selfDamageDelay() == 2
					&& t.damageMultiplier() == b75mult[i] && t.armorThresholdNegation() == 0F && t.armorPiercingPercent() == b75ap[i] && !t.penetrates() && t.ricochetAngle() == 5F
					&& t.maxRicochetCount() == 2 && t.knockbackMultiplier() == 0.1F && t.onImpact() == b75imp[i] && t.onUpdate() == null
					&& b75cfg[i].ammo == t && b75cfg[i].casingEffect == OrchestrasE1.CASING_B75, t.id() + " values");
		}

		/// Config values, sound ///
		check(problems, cases, HbmConfig.panzerschreckDamage() == 25F && HbmConfig.quadroDamage() == 40F && HbmConfig.fliegerfaustDamage() == 15F
				&& HbmConfig.missileLauncherDamage() == 50F && HbmConfig.bolterDamage() == 15F, "config damage defaults");
		check(problems, cases, HbmSoundEvents.GUN_ROCKET_FIRE.getId().getPath().equals("weapon.rpgshoot") && HbmSoundEvents.WEAPON_ROCKET_FLAME.getId().getPath().equals("weapon.rocketflame"), "sound ids");

		/// Recipes (WeaponRecipes); gun_fliegerfaust and gun_bolter have none ///
		var recipes = server.getRecipeManager();
		checkRecipe(problems, cases, server, level, "gun_panzerschreck", HbmItems.GUN_PANZERSCHRECK.get(), 3, 2, List.of(item("part_barrel_heavy_desh"), item("part_barrel_heavy_desh"), item("part_barrel_heavy_desh"),
				item("plate_cast_steel"), item("part_grip_desh"), item("part_mechanism_gunmetal")));
		checkRecipe(problems, cases, server, level, "gun_quadro", HbmItems.GUN_QUADRO.get(), 3, 3, List.of(item("part_barrel_heavy_ferrouranium"), item("circuit_advanced"), item("part_barrel_heavy_ferrouranium"),
				item("part_barrel_heavy_ferrouranium"), item("part_mechanism_weaponsteel"), item("part_barrel_heavy_ferrouranium"),
				item("part_grip_polymer"), item("part_grip_bakelite"), ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_missile_launcher", HbmItems.GUN_MISSILE_LAUNCHER.get(), 3, 3, List.of(ItemStack.EMPTY, item("circuit_advanced"), item("part_mechanism_weaponsteel"),
				item("part_barrel_heavy_tcalloy"), item("part_barrel_heavy_cdalloy"), item("part_barrel_heavy_tcalloy"),
				item("part_grip_bakelite"), ItemStack.EMPTY, ItemStack.EMPTY));
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_fliegerfaust")).isEmpty() && recipes.byKey(AtomicLegacy.id("gun_bolter")).isEmpty(), "no crafting recipe for fliegerfaust/bolter");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		// Panzerschreck: dura 300, draw 7, inspect 40, dmg 25, delay 5, reload 50, jam 40, mag 1
		ItemStack pzs = equip(player, HbmItems.GUN_PANZERSCHRECK.get(), stack(StandardAmmoType.ROCKET_HE, 3));
		GunConfig cfg = ((ItemGunBaseNT) pzs.getItem()).getConfig(pzs, 0);
		Receiver rec = cfg.getReceivers(pzs)[0];
		IMagazine<?> mag = rec.getMagazine(pzs);
		Vec3 offset = new Vec3(1, -0.0625 * 1.5, -0.1875D);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(pzs) == 300 && cfg.getDrawDuration(pzs) == 7 && cfg.getInspectDuration(pzs) == 40
				&& cfg.getCrosshair(pzs) == Crosshair.L_CIRCUMFLEX && cfg.getHideCrosshair(pzs) && cfg.getSmokeHandler(pzs) == null && rec.getBaseDamage(pzs) == 25F && rec.getDelayAfterFire(pzs) == 5
				&& rec.getReloadBeginDuration(pzs) == 50 && rec.getJamDuration(pzs) == 40 && mag.getCapacity(pzs) == 1 && !rec.getRefireOnHold(pzs)
				&& rec.getProjectileOffset(pzs).equals(offset) && rec.getHipfireSpread(pzs) == 0.025F && rec.getFireSound(pzs).get() == HbmSoundEvents.GUN_ROCKET_FIRE.get(), "panzerschreck values");
		press(player, pzs, GunKeybind.RELOAD);
		release(player, pzs, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(pzs, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(pzs, 0) == 50, "panzerschreck reload start");
		tick(player, pzs, 51);
		check(problems, cases, mag.getAmount(pzs, null) == 1 && countAmmo(player, StandardAmmoType.ROCKET_HE) == 2, "panzerschreck reload 1 rocket");
		tick(player, pzs, 3);
		fire(player, pzs, GunKeybind.GUN_PRIMARY);
		release(player, pzs, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_RPZB_HE && ItemGunBaseNT.getTimer(pzs, 0) == 5
				&& Math.abs(bullets.get(0).baseDamage() - 25F) < 0.001F && bullets.get(0).getDeltaMovement().lengthSqr() == 0D && mag.getAmount(pzs, null) == 0,
				"panzerschreck shot n=" + bullets.size() + (bullets.isEmpty() ? "" : " type=" + bullets.get(0).ammoType() + " dmg=" + bullets.get(0).baseDamage()));
		discard(level);

		// Quadro: dura 400, dmg 40, hipfire spread 0, delay 10, reload 55, jam 40, mag 4 (full reload), crosshair shown
		ItemStack qds = equip(player, HbmItems.GUN_QUADRO.get(), stack(StandardAmmoType.ROCKET_HEAT, 6));
		cfg = ((ItemGunBaseNT) qds.getItem()).getConfig(qds, 0);
		rec = cfg.getReceivers(qds)[0];
		mag = rec.getMagazine(qds);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(qds) == 400 && cfg.getDrawDuration(qds) == 7 && cfg.getInspectDuration(qds) == 40
				&& !cfg.getHideCrosshair(qds) && rec.getBaseDamage(qds) == 40F && rec.getHipfireSpread(qds) == 0F && rec.getDelayAfterFire(qds) == 10
				&& rec.getReloadBeginDuration(qds) == 55 && rec.getJamDuration(qds) == 40 && mag.getCapacity(qds) == 4 && rec.getProjectileOffset(qds).equals(offset), "quadro values");
		press(player, qds, GunKeybind.RELOAD);
		release(player, qds, GunKeybind.RELOAD);
		tick(player, qds, 60);
		check(problems, cases, mag.getAmount(qds, null) == 4 && countAmmo(player, StandardAmmoType.ROCKET_HEAT) == 2, "quadro reload 4 rockets");
		fire(player, qds, GunKeybind.GUN_PRIMARY);
		release(player, qds, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_QD_HEAT && Math.abs(bullets.get(0).baseDamage() - 20F) < 0.001F
				&& ItemGunBaseNT.getTimer(qds, 0) == 10 && mag.getAmount(qds, null) == 3, "quadro shot (40 * 0.5)");
		discard(level);

		// Fliegerfaust: dura 225, dmg 15, spread 0.015 / hipfire 0.015, auto, delay 4, reload 85, jam 60, mag 9; secondary fires the whole drum
		ItemStack ffs = equip(player, HbmItems.GUN_FLIEGERFAUST.get(), stack(StandardAmmoType.ROCKET_MICRO, 9));
		cfg = ((ItemGunBaseNT) ffs.getItem()).getConfig(ffs, 0);
		rec = cfg.getReceivers(ffs)[0];
		mag = rec.getMagazine(ffs);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(ffs) == 225 && rec.getBaseDamage(ffs) == 15F && rec.getInnateSpread(ffs) == 0.015F
				&& rec.getHipfireSpread(ffs) == 0.015F && rec.getRefireOnHold(ffs) && rec.getDelayAfterFire(ffs) == 4 && rec.getReloadBeginDuration(ffs) == 85 && rec.getJamDuration(ffs) == 60
				&& mag.getCapacity(ffs) == 9 && rec.getProjectileOffset(ffs).equals(offset) && rec.getProjectileOffsetScoped(ffs).equals(new Vec3(1, -0.0625 * 1.5, -0.125D))
				&& rec.getFireSound(ffs).get() == HbmSoundEvents.WEAPON_ROCKET_FLAME.get(), "fliegerfaust values");
		press(player, ffs, GunKeybind.RELOAD);
		release(player, ffs, GunKeybind.RELOAD);
		tick(player, ffs, 90);
		check(problems, cases, mag.getAmount(ffs, null) == 9 && countAmmo(player, StandardAmmoType.ROCKET_MICRO) == 0, "fliegerfaust reload 9");
		press(player, ffs, GunKeybind.GUN_SECONDARY);
		release(player, ffs, GunKeybind.GUN_SECONDARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 9 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_FF && mag.getAmount(ffs, null) == 0
				&& ItemGunBaseNT.getState(ffs, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(ffs, 0) == 4
				&& Math.abs(bullets.get(0).getDeltaMovement().length() - 7D) < 1D, "fliegerfaust salvo n=" + bullets.size() + " state=" + ItemGunBaseNT.getState(ffs, 0));
		discard(level);

		// Missile launcher: dura 500, draw 20, dmg 50, delay 5, reload 48, jam 33, mag 1; aimed primary locks on
		ItemStack mls = equip(player, HbmItems.GUN_MISSILE_LAUNCHER.get(), stack(StandardAmmoType.ROCKET_HE, 5));
		cfg = ((ItemGunBaseNT) mls.getItem()).getConfig(mls, 0);
		rec = cfg.getReceivers(mls)[0];
		mag = rec.getMagazine(mls);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(mls) == 500 && cfg.getDrawDuration(mls) == 20 && cfg.getInspectDuration(mls) == 40
				&& !cfg.getHideCrosshair(mls) && rec.getBaseDamage(mls) == 50F && rec.getHipfireSpread(mls) == 0F && rec.getDelayAfterFire(mls) == 5
				&& rec.getReloadBeginDuration(mls) == 48 && rec.getJamDuration(mls) == 33 && mag.getCapacity(mls) == 1, "missile launcher values");
		press(player, mls, GunKeybind.RELOAD);
		release(player, mls, GunKeybind.RELOAD);
		tick(player, mls, 50);
		Pig target = pig(level, new Vec3(2.5D, player.getEyeY() - 0.45D, 30D));
		ItemGunBaseNT.setIsAiming(mls, true);
		int lock = XFactoryRocket.getLockonTarget(player, 150D, 20D);
		fire(player, mls, GunKeybind.GUN_PRIMARY);
		release(player, mls, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, target != null && lock == target.getId() && bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_ML_HE
				&& bullets.get(0).lockonTarget() == target && !ItemGunBaseNT.getIsLockedOn(mls) && Math.abs(bullets.get(0).baseDamage() - 50F) < 0.001F,
				"missile launcher lock-on shot lock=" + lock + " n=" + bullets.size());
		if(!bullets.isEmpty() && target != null) {
			// homing: the missile turns towards the target (x = 2.5) while it flies along z
			LegacyBulletEntity m = bullets.get(0);
			double x0 = m.getX();
			for(int i = 0; i < 6; i++) { m.tickCount++; m.tick(); }
			check(problems, cases, m.legacyMotion.x > 0D && m.getX() > x0 && Math.abs(m.legacyMotion.length() - 1D) < 1.0E-3D, "missile homing motion.x=" + m.legacyMotion.x);
		}
		if(target != null) target.discard();
		ItemGunBaseNT.setIsAiming(mls, false);
		discard(level);

		// Bolter: dura 3000, draw 20, inspect 31, dmg 15, delay 2, auto, spread 0.005, reload 40, jam 55, mag 30
		ItemStack bs = equip(player, HbmItems.GUN_BOLTER.get(), stack(StandardAmmoType.B75, 40));
		cfg = ((ItemGunBaseNT) bs.getItem()).getConfig(bs, 0);
		rec = cfg.getReceivers(bs)[0];
		mag = rec.getMagazine(bs);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(bs) == 3000 && cfg.getDrawDuration(bs) == 20 && cfg.getInspectDuration(bs) == 31
				&& cfg.getCrosshair(bs) == Crosshair.L_CIRCLE && cfg.getSmokeHandler(bs) == XFactory75Bolt.LAMBDA_SMOKE && rec.getBaseDamage(bs) == 15F && rec.getDelayAfterFire(bs) == 2
				&& rec.getRefireOnHold(bs) && rec.getInnateSpread(bs) == 0.005F && rec.getReloadBeginDuration(bs) == 40 && rec.getJamDuration(bs) == 55 && mag.getCapacity(bs) == 30
				&& rec.getProjectileOffset(bs).equals(new Vec3(1, -0.0625 * 2.5, -0.25D)) && rec.getFireSound(bs).get() == HbmSoundEvents.GUN_POWDER_FIRE.get(), "bolter values");
		press(player, bs, GunKeybind.RELOAD);
		release(player, bs, GunKeybind.RELOAD);
		tick(player, bs, 45);
		check(problems, cases, mag.getAmount(bs, null) == 30 && countAmmo(player, StandardAmmoType.B75) == 10, "bolter reload 30");
		fire(player, bs, GunKeybind.GUN_PRIMARY);
		tick(player, bs, 4);
		release(player, bs, GunKeybind.GUN_PRIMARY);
		check(problems, cases, mag.getAmount(bs, null) == 27 && mag.getCasing(bs, null) == OrchestrasE1.CASING_B75, "bolter auto fire every 2 ticks " + mag.getAmount(bs, null));
		discard(level);
		player.getInventory().clearContent();

		/// Projectile behaviour ///
		// acceleration: motion * (0 + accel), accel += 0.4 while below 7 (steering rockets: below 4)
		LegacyBulletEntity r = Lego.spawnBullet(player, XFactoryRocket.rocket_rpzb[0], 25F, 0F, 0D, 0D, 1D);
		Vec3 start = r.position();
		for(int i = 0; i < 3; i++) { r.tickCount++; r.tick(); }
		check(problems, cases, Math.abs(r.position().distanceTo(start) - 1.2D) < 1.0E-6D && Math.abs(r.accel - 1.2D) < 1.0E-9D, "rocket acceleration 0/0.4/0.8 " + r.position().distanceTo(start));
		for(int i = 0; i < 16; i++) { r.tickCount++; r.tick(); }
		check(problems, cases, r.accel > 7D && r.accel < 7.3D && Math.abs(r.getDeltaMovement().length() - r.accel) < 1.0E-6D, "rocket accel cap " + r.accel);
		r.discard();
		LegacyBulletEntity q = Lego.spawnBullet(player, XFactoryRocket.rocket_qd[0], 25F, 0F, 0D, 0D, 1D);
		for(int i = 0; i < 20; i++) { q.tickCount++; q.tick(); }
		check(problems, cases, q.accel > 4D && q.accel < 4.5D, "steering rocket accel cap " + q.accel);
		q.discard();
		LegacyBulletEntity f = Lego.spawnBullet(player, XFactoryRocket.rocket_ff, 15F, 0F, 0D, 0D, 1D);
		check(problems, cases, Math.abs(f.getDeltaMovement().length() - 7D) < 1.0E-6D, "rocket_ff velocity 7");
		f.discard();
		discard(level);

		Pig pig = pig(level, new Vec3(0.5D, player.getEyeY() - 0.45D, 6.5D));
		if(pig != null) {
			// HE: standardExplode(5F) at most 25
			float h = pig.getHealth();
			LegacyBulletEntity b = shoot(player, XFactoryRocket.rocket_rpzb[0], 25F);
			float dealt = h - pig.getHealth();
			check(problems, cases, (b.isRemoved() || b.isRetired()) && dealt > 0F && dealt <= 25F + 0.01F, "rocket HE explosion " + dealt);

			// HEAT: standardExplode(3.5F) with 12.5 plus a direct 37.5 (damage x3) hit
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			b = shoot(player, XFactoryRocket.rocket_rpzb[1], 25F);
			dealt = h - pig.getHealth();
			check(problems, cases, (b.isRemoved() || b.isRetired()) && dealt > 37.5F - 0.01F && dealt <= 50F + 0.01F, "rocket HEAT explosion + direct x3 " + dealt);

			// phosphorus: standardExplode(3F) + lingering phosphorus fire 6x2
			pig.setDeltaMovement(Vec3.ZERO);
			b = shoot(player, XFactoryRocket.rocket_rpzb[4], 25F);
			List<LegacyLingeringFireEntity> fires = level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
			check(problems, cases, (b.isRemoved() || b.isRetired()) && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 6F) < 0.01F && Math.abs(fires.get(0).getBbHeight() - 2F) < 0.01F,
					"rocket phosphorus lingering fire " + fires.size());
			for(LegacyLingeringFireEntity e : fires) e.discard();

			// entity hits during the first 3 ticks are ignored (the rocket flies on)
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			LegacyBulletEntity e = Lego.spawnBullet(player, XFactoryRocket.rocket_rpzb[0], 25F, 0F, 0D, 0D, 1D);
			pig.setPos(e.getX(), e.getY() - 0.45D, e.getZ() + 0.2D + 0.75D);
			for(int i = 0; i < 2; i++) { e.tickCount++; e.tick(); }
			boolean ignored = !e.isRemoved() && !e.isRetired() && pig.getHealth() == h;
			for(int i = 0; i < 4 && !e.isRemoved() && !e.isRetired(); i++) { e.tickCount++; e.tick(); }
			check(problems, cases, ignored && (e.isRemoved() || e.isRetired()) && pig.getHealth() < h, "rocket ignores entity hits before tick 3");
			pig.setPos(0.5D, player.getEyeY() - 0.45D, 6.5D);

			// b75_inc: phosphorus >= 300, then the standard hit (15 * 0.8)
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			b = shoot(player, XFactory75Bolt.b75_inc, 15F);
			check(problems, cases, (b.isRemoved() || b.isRetired()) && HbmLivingHazards.get(pig).getPhosphorus() >= 300 && Math.abs(h - pig.getHealth() - 12F) < 0.01F,
					"b75_inc phosphorus " + HbmLivingHazards.get(pig).getPhosphorus() + " dmg=" + (h - pig.getHealth()));

			// b75: tinyExplode(2F); b75_exp: standardExplode(5F) with 22.5
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			b = shoot(player, XFactory75Bolt.b75, 15F);
			dealt = h - pig.getHealth();
			check(problems, cases, (b.isRemoved() || b.isRetired()) && dealt > 0F && dealt <= 15F + 0.01F, "b75 tiny explosion " + dealt);
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			b = shoot(player, XFactory75Bolt.b75_exp, 15F);
			dealt = h - pig.getHealth();
			check(problems, cases, (b.isRemoved() || b.isRetired()) && dealt > 0F && dealt <= 22.5F + 0.01F, "b75_exp explosion " + dealt);
			pig.discard();
		}

		// DEMO: the VNT explosion destroys blocks (dirt in the flight path)
		BlockPos dirt = BlockPos.containing(0.5D, player.getEyeY() - 0.1D, 6.5D);
		level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
		LegacyBulletEntity demo = shoot(player, XFactoryRocket.rocket_rpzb[2], 25F);
		check(problems, cases, (demo.isRemoved() || demo.isRetired()) && level.getBlockState(dirt).isAir(), "rocket demo destroys blocks " + level.getBlockState(dirt));
		clearBlocks(level, dirt);

		// INC: fire in air blocks of the 5x5x5 cube touching a flammable face + lingering diesel fire
		BlockPos planks = BlockPos.containing(0.5D, player.getEyeY() - 0.1D, 6.5D);
		level.setBlock(planks, Blocks.OAK_PLANKS.defaultBlockState(), 3);
		LegacyBulletEntity inc = shoot(player, XFactoryRocket.rocket_rpzb[3], 25F);
		boolean fireBlock = false;
		for(BlockPos p : BlockPos.betweenClosed(planks.offset(-3, -3, -3), planks.offset(3, 3, 3))) fireBlock |= level.getBlockState(p).is(Blocks.FIRE);
		List<LegacyLingeringFireEntity> fires = level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
		check(problems, cases, (inc.isRemoved() || inc.isRetired()) && fireBlock && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 6F) < 0.01F, "rocket incendiary fire " + fires.size());
		for(LegacyLingeringFireEntity e : fires) e.discard();
		clearBlocks(level, planks);
		discard(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_E1_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static void checkRecipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, Item result, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		check(problems, cases, recipe instanceof ShapedRecipe s && s.getWidth() == w && s.getHeight() == h && s.matches(input, level) && s.assemble(input, server.registryAccess()).is(result)
				&& s.assemble(input, server.registryAccess()).getCount() == 1, id + " recipe");
	}

	private static void clearBlocks(ServerLevel level, BlockPos center) {
		for(BlockPos p : BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))) {
			if(!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	private static Pig pig(ServerLevel level, Vec3 pos) {
		Pig pig = EntityType.PIG.create(level);
		if(pig == null) return null;
		pig.setNoAi(true);
		pig.setNoGravity(true);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
		pig.setHealth(1000F);
		pig.setPos(pos.x, pos.y, pos.z);
		level.addFreshEntity(pig);
		return pig;
	}

	/** Spawns the round like Lego.doStandardFire (no spread) and ticks it until it is gone (at most 40 ticks). */
	private static LegacyBulletEntity shoot(FakePlayer player, BulletConfig config, float damage) {
		LegacyBulletEntity bullet = Lego.spawnBullet(player, config, damage, 0F, 0D, -0.0625D * 1.5D, 1D);
		for(int i = 0; i < 40 && !bullet.isRemoved() && !bullet.isRetired(); i++) { bullet.tickCount++; bullet.tick(); }
		return bullet;
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

	private static void fire(FakePlayer player, ItemStack gun, GunKeybind key) { press(player, gun, key); }
	private static void press(FakePlayer player, ItemStack gun, GunKeybind key) { ((ItemGunBaseNT) gun.getItem()).handleKeybind(player, gun, key, true); }
	private static void release(FakePlayer player, ItemStack gun, GunKeybind key) { ((ItemGunBaseNT) gun.getItem()).handleKeybind(player, gun, key, false); }

	private static void tick(FakePlayer player, ItemStack stack, int ticks) {
		for(int i = 0; i < ticks; i++) stack.getItem().inventoryTick(stack, player.level(), player, 0, true);
	}

	private static int countAmmo(FakePlayer player, StandardAmmoType type) {
		int count = 0;
		for(int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if(s.is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(s) == type) count += s.getCount();
		}
		return count;
	}

	private static ItemStack stack(StandardAmmoType type, int count) {
		return StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), type).copyWithCount(count);
	}

	private static ItemStack item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
	}

	private static List<LegacyBulletEntity> bullets(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
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
		if(!ok) problems.add("gun batch E1: " + message);
	}
}
