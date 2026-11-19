package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC4;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory10ga;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.tileentity.LegacyDisplayStandBlockEntity;
import com.atomiclegacy.tileentity.LegacyPedestalRecipes;

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
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch C4 (gun_double_barrel, gun_double_barrel_sacred_dragon,
 * gun_maresleg, gun_maresleg_akimbo, gun_maresleg_broken, gun_liberator and the 10/12 gauge rounds)
 * against the pinned 1.7.10 XFactory10ga / XFactory12ga / WeaponRecipes / PedestalRecipes values.
 */
final class GunBatchC4Diagnostics {

	private GunBatchC4Diagnostics() {
	}

	/** Inside the always loaded spawn chunks (entities added elsewhere would not be entity-ticking/visible). */
	private static final double OX = 0.5D, OY = 300.0D, OZ = 0.5D;
	private static final AABB AREA = new AABB(OX - 40, OY - 40, OZ - 40, OX + 40, OY + 40, OZ + 40);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo, tab ///
		Object[][] guns = {
				{ HbmItems.GUN_MARESLEG.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.G12, 12 },
				{ HbmItems.GUN_MARESLEG_AKIMBO.get(), WeaponQuality.B_SIDE, 2, StandardAmmoType.G12, 24 },
				{ HbmItems.GUN_MARESLEG_BROKEN.get(), WeaponQuality.LEGENDARY, 1, StandardAmmoType.G12_MAGNUM, 24 },
				{ HbmItems.GUN_LIBERATOR.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.G12, 12 },
				{ HbmItems.GUN_DOUBLE_BARREL.get(), WeaponQuality.SPECIAL, 1, StandardAmmoType.G10, 6 },
				{ HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get(), WeaponQuality.B_SIDE, 1, StandardAmmoType.G10_DU, 6 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == (int) g[2], name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, def.is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(def) == g[3] && def.getCount() == (int) g[4], name + " default ammo " + def);
		}
		check(problems, cases, ItemGunBaseNT.secrets.contains(HbmItems.GUN_MARESLEG_BROKEN.get()), "broken is a secret (LEGENDARY)");

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int iMares = indexOf(tab, HbmItems.GUN_MARESLEG.get()), iLib = indexOf(tab, HbmItems.GUN_LIBERATOR.get()), iDouble = indexOf(tab, HbmItems.GUN_DOUBLE_BARREL.get());
		check(problems, cases, iMares >= 0 && iMares < iLib && iLib < iDouble && iDouble < indexOf(tab, HbmItems.AMMO_STANDARD.get()), "weapon tab order maresleg/liberator/double_barrel " + iMares + "/" + iLib + "/" + iDouble);
		check(problems, cases, indexOf(tab, HbmItems.GUN_MARESLEG_AKIMBO.get()) < 0 && indexOf(tab, HbmItems.GUN_MARESLEG_BROKEN.get()) < 0
				&& indexOf(tab, HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get()) < 0, "B_SIDE/LEGENDARY guns have no creative tab");

		/// Rounds (BulletConfig) ///
		Object[][] rounds = {
				// type, damageMult, projectiles, spread, ricochet angle, ricochet count, black powder, onImpact, DT, AP, penetrates, falloff, headshot
				{ StandardAmmoType.G10, 0.1F, 10, 0.035F, 15F, 2, false, false, 5F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G10_SHRAPNEL, 0.1F, 10, 0.035F, 90F, 15, false, false, 5F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G10_DU, 0.25F, 10, 0.035F, 15F, 2, false, false, 10F, 0.2F, true, false, 1.25F },
				{ StandardAmmoType.G10_SLUG, 1F, 1, 0F, 15F, 2, false, false, 10F, 0.1F, true, true, 1.25F },
				{ StandardAmmoType.G10_EXPLOSIVE, 0.25F, 10, 0.035F, 5F, 2, false, true, 0F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_BP, 0.75F / 8F, 8, 0.035F, 15F, 2, true, false, 0F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_BP_MAGNUM, 0.75F / 4F, 4, 0.035F, 25F, 2, true, false, 0F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_BP_SLUG, 0.75F, 1, 0.01F, 5F, 2, true, false, 0F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12, 1F / 8F, 8, 0.035F, 15F, 2, false, false, 2F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_SLUG, 1F, 1, 0F, 25F, 2, false, false, 4F, 0.15F, false, true, 1.5F },
				{ StandardAmmoType.G12_FLECHETTE, 1F / 8F, 8, 0.025F, 5F, 2, false, false, 5F, 0.2F, false, true, 1.25F },
				{ StandardAmmoType.G12_MAGNUM, 2F / 4F, 4, 0.015F, 15F, 2, false, false, 4F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_EXPLOSIVE, 2.5F, 1, 0F, 15F, 2, false, true, 0F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_PHOSPHORUS, 1F / 8F, 8, 0.015F, 15F, 2, false, true, 0F, 0F, false, true, 1.25F },
				{ StandardAmmoType.G12_EQUESTRIAN, 0F, 1, 0F, 5F, 2, false, false, 0F, 0F, false, true, 1.25F },
		};
		for(Object[] r : rounds) {
			StandardAmmoType t = (StandardAmmoType) r[0];
			boolean ok = t.damageMultiplier() == (float) r[1] && t.projectileCount() == (int) r[2] && t.projectileCountMax() == (int) r[2]
					&& t.projectileSpread() == (float) r[3] && t.ricochetAngle() == (float) r[4] && t.maxRicochetCount() == (int) r[5]
					&& t.blackPowder() == (boolean) r[6] && (t.onImpact() != null) == (boolean) r[7] && t.armorThresholdNegation() == (float) r[8]
					&& t.armorPiercingPercent() == (float) r[9] && t.penetrates() == (boolean) r[10] && t.damageFalloffByPenetration() == (boolean) r[11]
					&& t.headshotMultiplier() == (float) r[12] && t.projectileSpeed() == 10D && t.projectileLifetime() == 30 && t.projectileGravity() == 0D
					&& t.selfDamageDelay() == 2 && t.knockbackMultiplier() == 0.1F && !t.spectral() && t.onUpdate() == null;
			check(problems, cases, ok, t.id() + " BulletConfig values");
		}
		check(problems, cases, XFactory10ga.g10_explosive.wear == 3F && XFactory10ga.g10.wear == 1F && XFactory12ga.g12_explosive.wear == 1F, "10/12ga wear");
		check(problems, cases, XFactory10ga.g10.casingEffect == OrchestrasC4.CASING_G10 && XFactory10ga.g10_explosive.casingEffect == OrchestrasC4.CASING_G10_EXP
				&& XFactory12ga.g12_bp_slug.casingEffect == OrchestrasC4.CASING_G12_BP && XFactory12ga.g12_phosphorus.casingEffect == OrchestrasC4.CASING_G12_PHOSPHORUS
				&& XFactory12ga.g12_equestrian_tkr.casingEffect == OrchestrasC4.CASING_G12_EQUESTRIAN, "shotgun casings");
		ItemStack secret = LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "g12_equestrian");
		check(problems, cases, XFactory12ga.g12_equestrian_tkr.matches(secret) && !XFactory12ga.g12_equestrian_tkr.matches(stack(StandardAmmoType.G12, 1))
				&& !XFactory12ga.g12.matches(secret) && XFactory12ga.g12_equestrian_tkr.toStack().is(HbmItems.AMMO_SECRET.get())
				&& StandardAmmoItem.standardType(stack(StandardAmmoType.G12, 1)) == StandardAmmoType.G12, "g12_equestrian_tkr uses ammo_secret g12_equestrian");

		/// Config values ///
		check(problems, cases, HbmConfig.doubleBarrelDamage() == 30F && HbmConfig.sacredDragonDamage() == 45F && HbmConfig.maresLegDamage() == 16F
				&& HbmConfig.maresLegAkimboLeftDamage() == 16F && HbmConfig.maresLegAkimboRightDamage() == 16F && HbmConfig.brokenDamage() == 48F
				&& HbmConfig.liberatorDamage() == 16F, "config damage defaults");

		/// Recipes (WeaponRecipes, PedestalRecipes) ///
		recipe(server, level, problems, cases, "gun_maresleg", 3, 2, List.of(item("part_barrel_light_steel"), item("part_receiver_light_steel"), item("part_mechanism_gunmetal"),
				item("part_barrel_light_steel"), item("bolt_steel"), item("part_stock_wood")), HbmItems.GUN_MARESLEG.get(), false);
		recipe(server, level, problems, cases, "gun_maresleg", 3, 2, List.of(item("part_barrel_light_steel"), item("part_receiver_light_steel"), item("part_mechanism_gunmetal"),
				item("part_barrel_light_steel"), item("bolt_steel"), item("part_stock_wood")), HbmItems.GUN_MARESLEG.get(), true);
		recipe(server, level, problems, cases, "gun_maresleg_akimbo", 3, 1, List.of(new ItemStack(HbmItems.GUN_MARESLEG.get()), item("part_mechanism_weaponsteel"),
				new ItemStack(HbmItems.GUN_MARESLEG.get())), HbmItems.GUN_MARESLEG_AKIMBO.get(), true);
		ItemStack b = item("part_barrel_light_dura_steel"), w = item("part_grip_wood");
		recipe(server, level, problems, cases, "gun_liberator", 3, 3, List.of(b, b, ItemStack.EMPTY, b, b, item("part_mechanism_gunmetal"), w, ItemStack.EMPTY, w),
				HbmItems.GUN_LIBERATOR.get(), true);
		recipe(server, level, problems, cases, "gun_double_barrel_sacred_dragon", 2, 1, List.of(item("item_secret_selenium_steel"), new ItemStack(HbmItems.GUN_DOUBLE_BARREL.get())),
				HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get(), true);
		check(problems, cases, server.getRecipeManager().byKey(AtomicLegacy.id("gun_double_barrel")).isEmpty()
				&& server.getRecipeManager().byKey(AtomicLegacy.id("gun_maresleg_broken")).isEmpty(), "no crafting recipe for double_barrel / broken (original has none)");
		pedestal(level, problems, cases);

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(OX, OY, OZ);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discardBullets(level);

		// Mare's Leg: dura 600, draw 10, inspect 39, sequential reload 22/10/13, jam 24, delay 20, mag 6 (single reload)
		ItemStack mares = equip(player, HbmItems.GUN_MARESLEG.get(), stack(StandardAmmoType.G12, 7));
		GunConfig cfg = ((ItemGunBaseNT) mares.getItem()).getConfig(mares, 0);
		Receiver rec = cfg.getReceivers(mares)[0];
		IMagazine<?> mag = rec.getMagazine(mares);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(mares) == 600 && cfg.getDrawDuration(mares) == 10 && cfg.getInspectDuration(mares) == 39
				&& cfg.getReloadAnimSequential(mares) && rec.getBaseDamage(mares) == 16F && rec.getDelayAfterFire(mares) == 20 && rec.getReloadBeginDuration(mares) == 22
				&& rec.getReloadCycleDuration(mares) == 10 && rec.getReloadEndDuration(mares) == 13 && rec.getJamDuration(mares) == 24 && mag.getCapacity(mares) == 6
				&& rec.getHipfireSpread(mares) == 0.025F && rec.getAmmoSpread(mares) == 1F && cfg.getPressTertiary(mares) != null, "maresleg values");
		press(player, mares, GunKeybind.RELOAD);
		release(player, mares, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(mares, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(mares, 0) == 22, "maresleg reload start");
		tick(player, mares, 21);
		check(problems, cases, mag.getAmount(mares, null) == 0, "maresleg first shell early");
		tick(player, mares, 1);
		check(problems, cases, mag.getAmount(mares, null) == 1 && ItemGunBaseNT.getLastAnim(mares, 0) == GunAnimation.RELOAD_CYCLE && ItemGunBaseNT.getTimer(mares, 0) == 10, "maresleg first shell at 22");
		tick(player, mares, 50);
		check(problems, cases, mag.getAmount(mares, null) == 6 && countAmmo(player, StandardAmmoType.G12) == 1 && ItemGunBaseNT.getLastAnim(mares, 0) == GunAnimation.RELOAD_END
				&& ItemGunBaseNT.getState(mares, 0) == GunState.DRAWING && ItemGunBaseNT.getTimer(mares, 0) == 13, "maresleg 6 shells after 72 ticks " + mag.getAmount(mares, null));
		tick(player, mares, 13);
		check(problems, cases, ItemGunBaseNT.getState(mares, 0) == GunState.IDLE, "maresleg idle after reload end");
		fire(player, mares, GunKeybind.GUN_PRIMARY);
		release(player, mares, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 8 && bullets.stream().allMatch(e -> e.ammoType() == StandardAmmoType.G12 && Math.abs(e.baseDamage() - 2F) < 1.0E-4F)
				&& mag.getAmount(mares, null) == 5 && ItemGunBaseNT.getTimer(mares, 0) == 20 && ItemGunBaseNT.getWear(mares, 0) == 1F
				&& mag.getCasing(mares, null) == OrchestrasC4.CASING_G12, "maresleg 8 pellets of 16/8 dmg, n=" + bullets.size() + " mag=" + mag.getAmount(mares, null) + " t=" + ItemGunBaseNT.getTimer(mares, 0) + " wear=" + ItemGunBaseNT.getWear(mares, 0));
		discardBullets(level);

		// Pellet spread: EntityBulletBaseMK4 heading noise = gaussian * gunSpread per axis (headingForceMult 1)
		LambdaContext ctx = new LambdaContext(cfg, player, player.getInventory(), 0);
		float spread = Lego.calcSpread(ctx, mares, rec, XFactory12ga.g12, true, 0, false);
		check(problems, cases, Math.abs(spread - (0.035F + 0.025F)) < 1.0E-6F, "maresleg hipfire spread " + spread);
		double dev = 0;
		int n = 400;
		for(int i = 0; i < n; i++) {
			LegacyBulletEntity e = Lego.spawnBullet(player, XFactory12ga.g12, 1F, 0.035F, 0, 0, 0);
			Vec3 m = e.getDeltaMovement().normalize();
			dev += Math.sqrt(m.x * m.x + m.y * m.y);
			e.remove(Entity.RemovalReason.KILLED);
		}
		dev /= n;
		check(problems, cases, dev > 0.035D && dev < 0.053D, "buckshot spread 0.035 mean deviation " + dev);
		discardBullets(level);

		// Mare's Leg akimbo: spreadHipfire 0, spreadAmmo 1.35, both configs reload with R, LMB = left (mag 0), RMB = right (mag 1)
		ItemStack akimbo = equip(player, HbmItems.GUN_MARESLEG_AKIMBO.get(), stack(StandardAmmoType.G12, 12));
		ItemGunBaseNT akimboGun = (ItemGunBaseNT) akimbo.getItem();
		Receiver left = akimboGun.getConfig(akimbo, 0).getReceivers(akimbo)[0];
		Receiver right = akimboGun.getConfig(akimbo, 1).getReceivers(akimbo)[0];
		check(problems, cases, left.getHipfireSpread(akimbo) == 0F && left.getAmmoSpread(akimbo) == 1.35F && right.getAmmoSpread(akimbo) == 1.35F
				&& left.getProjectileOffset(akimbo).z == 0.1875D && right.getProjectileOffset(akimbo).z == -0.1875D && akimboGun.getConfig(akimbo, 0).getDrawDuration(akimbo) == 5
				&& akimboGun.getConfig(akimbo, 0).getPressTertiary(akimbo) == null, "maresleg akimbo values");
		press(player, akimbo, GunKeybind.RELOAD);
		release(player, akimbo, GunKeybind.RELOAD);
		tick(player, akimbo, 72);
		check(problems, cases, left.getMagazine(akimbo).getAmount(akimbo, null) == 6 && right.getMagazine(akimbo).getAmount(akimbo, null) == 6 && countAmmo(player, StandardAmmoType.G12) == 0,
				"akimbo both tubes loaded " + left.getMagazine(akimbo).getAmount(akimbo, null) + "/" + right.getMagazine(akimbo).getAmount(akimbo, null));
		tick(player, akimbo, 13);
		fire(player, akimbo, GunKeybind.GUN_PRIMARY);
		release(player, akimbo, GunKeybind.GUN_PRIMARY);
		check(problems, cases, left.getMagazine(akimbo).getAmount(akimbo, null) == 5 && right.getMagazine(akimbo).getAmount(akimbo, null) == 6 && bullets(level).size() == 8, "akimbo left fire");
		fire(player, akimbo, GunKeybind.GUN_SECONDARY);
		release(player, akimbo, GunKeybind.GUN_SECONDARY);
		check(problems, cases, right.getMagazine(akimbo).getAmount(akimbo, null) == 5 && bullets(level).size() == 16, "akimbo right fire");
		discardBullets(level);

		// Broken: dura 0, no wear, spreadAmmo 1.15, dmg 48, loads the ammo_secret equestrian round first
		ItemStack broken = equip(player, HbmItems.GUN_MARESLEG_BROKEN.get(), secret.copyWithCount(2), stack(StandardAmmoType.G12_MAGNUM, 4));
		Receiver brokenRec = ((ItemGunBaseNT) broken.getItem()).getConfig(broken, 0).getReceivers(broken)[0];
		check(problems, cases, ((ItemGunBaseNT) broken.getItem()).getConfig(broken, 0).getDurability(broken) == 0F && brokenRec.getAmmoSpread(broken) == 1.15F
				&& brokenRec.getBaseDamage(broken) == 48F, "broken values");
		press(player, broken, GunKeybind.RELOAD);
		release(player, broken, GunKeybind.RELOAD);
		tick(player, broken, 22 + 10 + 13);
		check(problems, cases, brokenRec.getMagazine(broken).getAmount(broken, null) == 2 && brokenRec.getMagazine(broken).getType(broken, null) == XFactory12ga.g12_equestrian_tkr
				&& countAmmo(player, StandardAmmoType.G12_MAGNUM) == 4, "broken loads equestrian only");
		fire(player, broken, GunKeybind.GUN_PRIMARY);
		release(player, broken, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.G12_EQUESTRIAN && bullets.get(0).baseDamage() == 0F
				&& ItemGunBaseNT.getWear(broken, 0) == 0F, "broken NOWEAR equestrian shot");
		discardBullets(level);

		// Liberator: dura 200, draw 20, inspect 21, reload 25/15/7, jam 45, rounds 4, mag 4
		ItemStack lib = equip(player, HbmItems.GUN_LIBERATOR.get(), stack(StandardAmmoType.G12_BP, 4));
		cfg = ((ItemGunBaseNT) lib.getItem()).getConfig(lib, 0);
		rec = cfg.getReceivers(lib)[0];
		check(problems, cases, cfg.getDurability(lib) == 200 && cfg.getDrawDuration(lib) == 20 && cfg.getInspectDuration(lib) == 21 && rec.getRoundsPerCycle(lib) == 4
				&& rec.getReloadBeginDuration(lib) == 25 && rec.getReloadCycleDuration(lib) == 15 && rec.getReloadEndDuration(lib) == 7 && rec.getJamDuration(lib) == 45
				&& rec.getMagazine(lib).getCapacity(lib) == 4 && rec.getBaseDamage(lib) == 16F, "liberator values");
		press(player, lib, GunKeybind.RELOAD);
		release(player, lib, GunKeybind.RELOAD);
		tick(player, lib, 25 + 45);
		check(problems, cases, rec.getMagazine(lib).getAmount(lib, null) == 4 && ItemGunBaseNT.getLastAnim(lib, 0) == GunAnimation.RELOAD_END, "liberator 4 shells after 70 ticks");
		tick(player, lib, 7);
		fire(player, lib, GunKeybind.GUN_PRIMARY);
		release(player, lib, GunKeybind.GUN_PRIMARY);
		check(problems, cases, rec.getMagazine(lib).getAmount(lib, null) == 0 && bullets(level).size() == 32 && ItemGunBaseNT.getWear(lib, 0) == 4F
				&& bullets(level).stream().allMatch(e -> e.ammoType() == StandardAmmoType.G12_BP), "liberator fires all 4 barrels (32 BP pellets) n=" + bullets(level).size());
		discardBullets(level);

		// Double barrel: dura 1000, rounds 2, delay 10, reload(41) = begin/cycle 41, end 0 (full), reloadOnEmpty, secondary fires one barrel
		ItemStack dbl = equip(player, HbmItems.GUN_DOUBLE_BARREL.get(), stack(StandardAmmoType.G10, 5));
		cfg = ((ItemGunBaseNT) dbl.getItem()).getConfig(dbl, 0);
		rec = cfg.getReceivers(dbl)[0];
		mag = rec.getMagazine(dbl);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(dbl) == 1000 && cfg.getInspectDuration(dbl) == 39 && rec.getRoundsPerCycle(dbl) == 2
				&& rec.getDelayAfterFire(dbl) == 10 && rec.getReloadBeginDuration(dbl) == 41 && rec.getReloadCycleDuration(dbl) == 41 && rec.getReloadEndDuration(dbl) == 0 && rec.getReloadOnEmpty(dbl)
				&& mag.getCapacity(dbl) == 2 && rec.getBaseDamage(dbl) == 30F && rec.getFirePitch(dbl) == 0.9F && cfg.getPressSecondary(dbl) == XFactory10ga.LAMBDA_DOUBLE_SECONDARY,
				"double barrel values dura=" + cfg.getDurability(dbl) + " rounds=" + rec.getRoundsPerCycle(dbl) + " reload=" + rec.getReloadBeginDuration(dbl) + "/" + rec.getReloadEndDuration(dbl)
				+ " empty=" + rec.getReloadOnEmpty(dbl) + " cap=" + mag.getCapacity(dbl) + " dmg=" + rec.getBaseDamage(dbl) + " pitch=" + rec.getFirePitch(dbl));
		press(player, dbl, GunKeybind.RELOAD);
		release(player, dbl, GunKeybind.RELOAD);
		tick(player, dbl, 41);
		check(problems, cases, mag.getAmount(dbl, null) == 2 && ItemGunBaseNT.getTimer(dbl, 0) == 0 && ItemGunBaseNT.getState(dbl, 0) == GunState.DRAWING, "double barrel full reload " + mag.getAmount(dbl, null) + " t=" + ItemGunBaseNT.getTimer(dbl, 0) + " s=" + ItemGunBaseNT.getState(dbl, 0));
		tick(player, dbl, 1);
		fire(player, dbl, GunKeybind.GUN_PRIMARY);
		release(player, dbl, GunKeybind.GUN_PRIMARY);
		check(problems, cases, mag.getAmount(dbl, null) == 0 && bullets(level).size() == 20 && ItemGunBaseNT.getWear(dbl, 0) == 2F, "double barrel LMB fires both barrels n=" + bullets(level).size());
		discardBullets(level);
		tick(player, dbl, 10);
		check(problems, cases, ItemGunBaseNT.getState(dbl, 0) == GunState.RELOADING && ItemGunBaseNT.getLastAnim(dbl, 0) == GunAnimation.RELOAD, "double barrel reload on empty");
		tick(player, dbl, 42);
		fire(player, dbl, GunKeybind.GUN_SECONDARY);
		release(player, dbl, GunKeybind.GUN_SECONDARY);
		check(problems, cases, mag.getAmount(dbl, null) == 1 && bullets(level).size() == 10 && ItemGunBaseNT.getState(dbl, 0) == GunState.COOLDOWN, "double barrel RMB fires one barrel");
		discardBullets(level);

		ItemStack dragon = equip(player, HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get(), stack(StandardAmmoType.G10_EXPLOSIVE, 2));
		Receiver dragonRec = ((ItemGunBaseNT) dragon.getItem()).getConfig(dragon, 0).getReceivers(dragon)[0];
		check(problems, cases, dragonRec.getAmmoSpread(dragon) == 1.35F && dragonRec.getBaseDamage(dragon) == 45F
				&& ((ItemGunBaseNT) dragon.getItem()).getConfig(dragon, 0).getDurability(dragon) == 6000F, "sacred dragon values");
		press(player, dragon, GunKeybind.RELOAD);
		release(player, dragon, GunKeybind.RELOAD);
		tick(player, dragon, 82);
		fire(player, dragon, GunKeybind.GUN_SECONDARY);
		release(player, dragon, GunKeybind.GUN_SECONDARY);
		check(problems, cases, ItemGunBaseNT.getWear(dragon, 0) == 3F && bullets(level).size() == 10, "explosive 10ga wear 3");
		discardBullets(level);
		player.getInventory().clearContent();

		/// Bullet behaviour ///
		Pig pig = EntityType.PIG.create(level);
		if(pig != null) {
			pig.setNoAi(true);
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
			pig.setHealth(1000F);
			pig.setPos(OX, player.getEyeY() - 0.45D, OZ + 3D);
			level.addFreshEntity(pig);

			// phosphorus: HbmLivingProps.phosphorus = 300, normal hit follows
			float before = pig.getHealth();
			LegacyBulletEntity e = shoot(player, XFactory12ga.g12_phosphorus, 16F);
			check(problems, cases, Math.abs(before - pig.getHealth() - 2F) < 0.01F && HbmLivingHazards.get(pig).getPhosphorus() == 300 && (e.isRemoved() || e.isRetired()),
					"phosphorus pellet dmg=" + (before - pig.getHealth()) + " phosphorus=" + HbmLivingHazards.get(pig).getPhosphorus());
			HbmLivingHazards.clear(pig);

			// DU buckshot penetrates without damage falloff
			before = pig.getHealth();
			e = shoot(player, XFactory10ga.g10_du, 30F);
			check(problems, cases, Math.abs(before - pig.getHealth() - 7.5F) < 0.01F && !e.isRemoved() && !e.isRetired() && Math.abs(e.baseDamage() - 7.5F) < 1.0E-4F,
					"g10_du penetrates dmg=" + (before - pig.getHealth()));
			discardBullets(level);

			// equestrian TKR: damage 0
			before = pig.getHealth();
			e = shoot(player, XFactory12ga.g12_equestrian_tkr, 48F);
			check(problems, cases, pig.getHealth() == before && (e.isRemoved() || e.isRetired()), "equestrian TKR deals no damage");

			// 12ga explosive: Lego.standardExplode(2): explosion damage, bullet dies
			before = pig.getHealth();
			e = shoot(player, XFactory12ga.g12_explosive, 16F);
			float dealt = before - pig.getHealth();
			check(problems, cases, (e.isRemoved() || e.isRetired()) && dealt > 0F && dealt <= 40F + 0.01F, "12ga explosive dmg=" + dealt);
			check(problems, cases, level.getBlockState(pig.blockPosition()).isAir(), "12ga explosive has no block damage");

			// 10ga explosive pellet: Lego.tinyExplode(1.5)
			before = pig.getHealth();
			e = shoot(player, XFactory10ga.g10_explosive, 30F);
			dealt = before - pig.getHealth();
			check(problems, cases, (e.isRemoved() || e.isRetired()) && dealt > 0F && dealt <= 7.5F + 0.01F, "10ga explosive pellet dmg=" + dealt);
			pig.discard();
		}
		discardBullets(level);

		// ricochet: a head-on wall hit ricochets only within the ricochet angle (shrapnel 90, buckshot 15)
		BlockPos wall = BlockPos.containing(OX, player.getEyeY(), OZ + 4D);
		for(int dx = -1; dx <= 1; dx++) for(int dy = -1; dy <= 1; dy++) level.setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState());
		LegacyBulletEntity shrap = Lego.spawnBullet(player, XFactory10ga.g10_shrapnel, 1F, 0F, 0, 0, 0);
		shrap.tick();
		check(problems, cases, !shrap.isRemoved() && !shrap.isRetired() && shrap.getDeltaMovement().z < 0, "shrapnel ricochets head-on, motion " + shrap.getDeltaMovement());
		LegacyBulletEntity buck = Lego.spawnBullet(player, XFactory10ga.g10, 1F, 0F, 0, 0, 0);
		buck.tick();
		check(problems, cases, buck.isRemoved() || buck.isRetired(), "buckshot stops on a head-on hit");
		for(int dx = -1; dx <= 1; dx++) for(int dy = -1; dy <= 1; dy++) level.setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.AIR.defaultBlockState());
		discardBullets(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_C4_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static void recipe(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases, String id, int width, int height, List<ItemStack> grid, Item result, boolean expect) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(h -> h.value()).orElse(null);
		List<ItemStack> input = new ArrayList<>(grid);
		if(!expect) input.set(3, ItemStack.EMPTY);
		var craft = CraftingInput.of(width, height, input);
		boolean ok = recipe instanceof CraftingRecipe c && c.matches(craft, level) == expect && (!expect || c.assemble(craft, server.registryAccess()).is(result));
		check(problems, cases, ok, id + " recipe" + (expect ? "" : " rejects a wrong grid"));
	}

	private static void pedestal(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos center = BlockPos.containing(OX, OY - 20, OZ);
		int[][] offsets = { {-2, -2}, {0, -3}, {2, -2}, {-3, 0}, {0, 0}, {3, 0}, {-2, 2}, {0, 3}, {2, 2} };
		ItemStack wire = item("barbed_wire"), plate = item("plate_weaponsteel");
		ItemStack[] items = { wire, plate, wire, plate, new ItemStack(HbmItems.GUN_MARESLEG.get()), plate, wire, plate, wire };
		for(int i = 0; i < 9; i++) {
			BlockPos pos = center.offset(offsets[i][0], 0, offsets[i][1]);
			level.setBlockAndUpdate(pos, HbmBlocks.PEDESTAL.get().defaultBlockState());
			if(level.getBlockEntity(pos) instanceof LegacyDisplayStandBlockEntity stand) stand.setDisplayedItem(items[i].copyWithCount(1));
		}
		// placing the redstone block triggers BlockPedestal#onNeighborBlockChange, which finds and consumes the recipe
		level.setBlockAndUpdate(center.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		boolean crafted = level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity c && c.displayedItem().is(HbmItems.GUN_MARESLEG_BROKEN.get());
		if(!crafted) LegacyPedestalRecipes.find(level, center).ifPresent(r -> LegacyPedestalRecipes.consume(level, center, r));
		boolean ok = level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity stand && stand.displayedItem().is(HbmItems.GUN_MARESLEG_BROKEN.get())
				&& level.getBlockEntity(center.offset(0, 0, -3)) instanceof LegacyDisplayStandBlockEntity side && side.displayedItem().isEmpty();
		check(problems, cases, ok, "pedestal recipe gun_maresleg_broken");
		for(int[] o : offsets) {
			BlockPos pos = center.offset(o[0], 0, o[1]);
			if(level.getBlockEntity(pos) instanceof LegacyDisplayStandBlockEntity stand) stand.setDisplayedItem(ItemStack.EMPTY);
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		level.setBlockAndUpdate(center.below(), Blocks.AIR.defaultBlockState());
	}

	private static LegacyBulletEntity shoot(FakePlayer player, BulletConfig config, float damage) {
		LegacyBulletEntity bullet = Lego.spawnBullet(player, config, damage, 0F, -0.25D, -0.0625D * 2.5D, 1D);
		for(int i = 0; i < 3 && !bullet.isRemoved() && !bullet.isRetired(); i++) bullet.tick();
		return bullet;
	}

	private static ItemStack equip(FakePlayer player, Item gun, ItemStack... ammo) {
		player.getInventory().clearContent();
		player.getInventory().setItem(0, new ItemStack(gun));
		for(int i = 0; i < ammo.length; i++) player.getInventory().setItem(i + 1, ammo[i]);
		ItemStack stack = player.getInventory().getItem(0);
		tick(player, stack, 1);
		tick(player, stack, 25);
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
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(ItemStack.EMPTY);
	}

	private static List<LegacyBulletEntity> bullets(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
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
		if(!ok) problems.add("gun batch C4: " + message);
	}
}
