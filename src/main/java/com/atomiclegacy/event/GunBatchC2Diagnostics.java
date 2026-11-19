package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBoxcarEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyTorpedoEntity;
import com.atomiclegacy.items.LegacyVariantItem;
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
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory44;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch C2 (XFactory44: gun_henry, gun_henry_lincoln, gun_heavy_revolver,
 * gun_heavy_revolver_lilmac, gun_heavy_revolver_protege, gun_hangman and the .44 rounds) against the pinned
 * 1.7.10 XFactory44 / Orchestras / WeaponRecipes / PedestalRecipes / XWeaponModManager values.
 */
final class GunBatchC2Diagnostics {

	private GunBatchC2Diagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 380, 8);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_HENRY.get(), WeaponQuality.A_SIDE, StandardAmmoType.M44_SP, 14 },
				{ HbmItems.GUN_HENRY_LINCOLN.get(), WeaponQuality.B_SIDE, StandardAmmoType.M44_JHP, 14 },
				{ HbmItems.GUN_HEAVY_REVOLVER.get(), WeaponQuality.A_SIDE, StandardAmmoType.M44_SP, 12 },
				{ HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get(), WeaponQuality.LEGENDARY, StandardAmmoType.M44_JHP, 12 },
				{ HbmItems.GUN_HEAVY_REVOLVER_PROTEGE.get(), WeaponQuality.LEGENDARY, StandardAmmoType.M44_JHP, 12 },
				{ HbmItems.GUN_HANGMAN.get(), WeaponQuality.LEGENDARY, StandardAmmoType.M44_FMJ, 16 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == 1, name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, StandardAmmoItem.standardType(def) == g[2] && def.getCount() == (int) g[3], name + " default ammo " + def);
			check(problems, cases, ItemGunBaseNT.secrets.contains(item) == (gun.quality == WeaponQuality.LEGENDARY), name + " secrets list");
		}

		/// Creative tab: only A_SIDE guns get MainRegistry.weaponTab ///
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int henryIndex = indexOf(tab, HbmItems.GUN_HENRY.get());
		int revolverIndex = indexOf(tab, HbmItems.GUN_HEAVY_REVOLVER.get());
		check(problems, cases, henryIndex >= 0 && revolverIndex > henryIndex && indexOf(tab, HbmItems.AMMO_STANDARD.get()) > revolverIndex, "weapon tab henry/heavy_revolver " + henryIndex + "/" + revolverIndex);
		check(problems, cases, indexOf(tab, HbmItems.GUN_HENRY_LINCOLN.get()) < 0 && indexOf(tab, HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get()) < 0
				&& indexOf(tab, HbmItems.GUN_HEAVY_REVOLVER_PROTEGE.get()) < 0 && indexOf(tab, HbmItems.GUN_HANGMAN.get()) < 0, "B_SIDE / LEGENDARY guns hidden");

		/// Ammo values (BulletConfig) ///
		check(problems, cases, round(StandardAmmoType.M44_BP, 0.75F, false, true, 0F, 0F, 1.25F) && StandardAmmoType.M44_BP.blackPowder(), "m44_bp values");
		check(problems, cases, round(StandardAmmoType.M44_SP, 1F, false, true, 0F, 0F, 1.25F) && !StandardAmmoType.M44_SP.blackPowder(), "m44_sp values");
		check(problems, cases, round(StandardAmmoType.M44_FMJ, 0.8F, false, true, 3F, 0.1F, 1.25F), "m44_fmj values");
		check(problems, cases, round(StandardAmmoType.M44_JHP, 1.5F, false, true, 0F, -0.25F, 1.5F), "m44_jhp values");
		check(problems, cases, round(StandardAmmoType.M44_AP, 1.25F, true, false, 7.5F, 0.15F, 1.25F), "m44_ap values");
		check(problems, cases, round(StandardAmmoType.M44_EXPRESS, 1.5F, true, true, 3F, 0.1F, 1.25F) && XFactory44.m44_express.wear == 1.5F, "m44_express values");
		check(problems, cases, round(StandardAmmoType.M44_EQUESTRIAN_PIP, 0F, false, true, 0F, 0F, 1.25F) && StandardAmmoType.M44_EQUESTRIAN_PIP.onImpact() == XFactory44.LAMBDA_BOXCAR
				&& round(StandardAmmoType.M44_EQUESTRIAN_MN7, 0F, false, true, 0F, 0F, 1.25F) && StandardAmmoType.M44_EQUESTRIAN_MN7.onImpact() == XFactory44.LAMBDA_TORPEDO, "m44_equestrian values");
		check(problems, cases, XFactory44.m44_sp.casingEffect == XFactory44.CASING_M44 && XFactory44.m44_ap.casingEffect == XFactory44.CASING_M44_STEEL
				&& XFactory44.m44_equestrian_pip.casingEffect == XFactory44.CASING_M44_EQUESTRIAN && XFactory44.m44_sp.wear == 1F, "casings / wear");
		ItemStack secret = LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "m44_equestrian");
		check(problems, cases, XFactory44.m44_equestrian_pip.matches(secret) && XFactory44.m44_equestrian_mn7.matches(secret) && !XFactory44.m44_jhp.matches(secret)
				&& !XFactory44.m44_equestrian_pip.matches(LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "g12_equestrian"))
				&& !XFactory44.m44_equestrian_pip.matches(stack(StandardAmmoType.M44_JHP, 1)), "ammo_secret m44_equestrian matching");

		/// Config values ///
		check(problems, cases, HbmConfig.henryDamage() == 10F && HbmConfig.lincolnDamage() == 20F && HbmConfig.heavyRevolverDamage() == 15F && HbmConfig.lilmacDamage() == 30F
				&& HbmConfig.protegeDamage() == 30F && HbmConfig.hangmanDamage() == 25F && HbmConfig.hangmanSmackDamage() == 10F, "config damage defaults");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		ItemStack E = ItemStack.EMPTY;
		shaped(server, problems, cases, "gun_henry", HbmItems.GUN_HENRY.get(), 3, 2, List.of(item("part_barrel_light_steel"), item("part_receiver_light_gunmetal"), item("plate_gunmetal"),
				item("part_barrel_light_steel"), item("part_mechanism_gunmetal"), item("part_stock_wood")));
		shaped(server, problems, cases, "gun_henry_lincoln", HbmItems.GUN_HENRY_LINCOLN.get(), 3, 3, List.of(E, item("part_mechanism_weaponsteel"), E,
				item("plate_cast_gold"), new ItemStack(HbmItems.GUN_HENRY.get()), item("plate_cast_gold"), E, item("part_mechanism_weaponsteel"), E));
		shaped(server, problems, cases, "gun_heavy_revolver", HbmItems.GUN_HEAVY_REVOLVER.get(), 3, 2, List.of(item("part_barrel_light_desh"), item("part_receiver_light_desh"), item("part_mechanism_gunmetal"),
				E, E, item("part_grip_wood")));
		check(problems, cases, recipes.byKey(AtomicLegacy.id("ammo_secret_m44_equestrian")).isPresent(), "m44_equestrian shapeless recipe present");
		pedestal(problems, cases, "gun_heavy_revolver_lilmac");
		pedestal(problems, cases, "gun_heavy_revolver_protege");

		/// Mods: SCOPE on gun_heavy_revolver only ///
		ItemStack scope = new ItemStack(HbmItems.WEAPON_MOD_SCOPE.get());
		ItemStack revolverScoped = new ItemStack(HbmItems.GUN_HEAVY_REVOLVER.get());
		check(problems, cases, XWeaponModManager.isApplicable(revolverScoped, scope, 0, true) && !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_HENRY.get()), scope, 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get()), scope, 0, true)
				&& ((ItemGunBaseNT) HbmItems.GUN_HEAVY_REVOLVER.get()).recognizedMods.contains(HbmItems.WEAPON_MOD_SCOPE.get()), "scope applicability");
		ItemGunBaseNT revolverGun = (ItemGunBaseNT) HbmItems.GUN_HEAVY_REVOLVER.get();
		check(problems, cases, revolverGun.getConfig(revolverScoped, 0).getScopeTexture(revolverScoped) == null
				&& revolverGun.getName(revolverScoped).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t0 && t0.getKey().equals("item.atomiclegacy.gun_heavy_revolver"), "unscoped revolver");
		XWeaponModManager.install(revolverScoped, 0, scope);
		check(problems, cases, XWeaponModManager.hasUpgrade(revolverScoped, 0, XWeaponModManager.ID_SCOPE) && XFactory44.scope_lilmac.equals(revolverGun.getConfig(revolverScoped, 0).getScopeTexture(revolverScoped))
				&& revolverGun.getConfig(revolverScoped, 0).getHideCrosshair(revolverScoped)
				&& revolverGun.getName(revolverScoped).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t1 && t1.getKey().equals("item.atomiclegacy.gun_heavy_revolver_scoped"), "scoped revolver: scope_44 + _scoped name");
		ItemStack lilmacStack = new ItemStack(HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get());
		check(problems, cases, XFactory44.scope_lilmac.equals(((ItemGunBaseNT) lilmacStack.getItem()).getConfig(lilmacStack, 0).getScopeTexture(lilmacStack))
				&& ((ItemGunBaseNT) HbmItems.GUN_HEAVY_REVOLVER_PROTEGE.get()).getConfig(null, 0).getScopeTexture(null) == null, "lilmac scope texture");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discardBullets(level);

		// Henry: dura 300, draw 15, inspect 23, sequential, CIRCLE, dmg 10, delay 20, reload 25/11/14/8, jam 45, single reload x14
		ItemStack henry = equip(player, HbmItems.GUN_HENRY.get(), stack(StandardAmmoType.M44_SP, 20));
		ItemGunBaseNT henryGun = (ItemGunBaseNT) henry.getItem();
		GunConfig cfg = henryGun.getConfig(henry, 0);
		Receiver rec = cfg.getReceivers(henry)[0];
		IMagazine<?> mag = rec.getMagazine(henry);
		check(problems, cases, cfg.getDurability(henry) == 300 && cfg.getDrawDuration(henry) == 15 && cfg.getInspectDuration(henry) == 23 && cfg.getReloadAnimSequential(henry)
				&& cfg.getCrosshair(henry) == Crosshair.CIRCLE && rec.getBaseDamage(henry) == 10F && rec.getDelayAfterFire(henry) == 20 && rec.getReloadBeginDuration(henry) == 25
				&& rec.getReloadCycleDuration(henry) == 11 && rec.getReloadEndDuration(henry) == 14 && rec.getReloadCockOnEmptyPost(henry) == 8 && rec.getReloadCockOnEmptyPre(henry) == 0
				&& rec.getJamDuration(henry) == 45 && mag instanceof MagazineSingleReload && mag.getCapacity(henry) == 14 && rec.getHipfireSpread(henry) == 0.025F
				&& rec.getProjectileOffset(henry).equals(new Vec3(0.75, -0.0625, -0.1875D)), "henry values");
		press(player, henry, GunKeybind.RELOAD);
		release(player, henry, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(henry, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(henry, 0) == 25, "henry reload start");
		tick(player, henry, 25);
		check(problems, cases, mag.getAmount(henry, null) == 1 && ItemGunBaseNT.getLastAnim(henry, 0) == GunAnimation.RELOAD_CYCLE && ItemGunBaseNT.getTimer(henry, 0) == 11, "henry first round " + mag.getAmount(henry, null));
		tick(player, henry, 11 * 13);
		check(problems, cases, mag.getAmount(henry, null) == 14 && countAmmo(player, StandardAmmoType.M44_SP) == 6 && ItemGunBaseNT.getState(henry, 0) == GunState.DRAWING
				&& ItemGunBaseNT.getLastAnim(henry, 0) == GunAnimation.RELOAD_END && ItemGunBaseNT.getTimer(henry, 0) == 14 + 8, "henry sequential reload x14 timer=" + ItemGunBaseNT.getTimer(henry, 0) + " mag=" + mag.getAmount(henry, null));
		tick(player, henry, 22);
		check(problems, cases, ItemGunBaseNT.getState(henry, 0) == GunState.IDLE, "henry idle after reload end");
		fire(player, henry, GunKeybind.GUN_PRIMARY);
		release(player, henry, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.M44_SP && ItemGunBaseNT.getTimer(henry, 0) == 20 && mag.getAmount(henry, null) == 13
				&& Math.abs(bullets.get(0).baseDamage() - 10F) < 1.0E-4F, "henry shot");
		discardBullets(level);
		// partial reload: one round, LMB during the reload sets the cancel flag (not empty -> no cock time)
		tick(player, henry, 21);
		press(player, henry, GunKeybind.RELOAD);
		release(player, henry, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getTimer(henry, 0) == 25, "henry partial reload start");
		press(player, henry, GunKeybind.GUN_PRIMARY);
		release(player, henry, GunKeybind.GUN_PRIMARY);
		tick(player, henry, 25);
		check(problems, cases, mag.getAmount(henry, null) == 14 && ItemGunBaseNT.getLastAnim(henry, 0) == GunAnimation.RELOAD_END && ItemGunBaseNT.getTimer(henry, 0) == 14, "henry partial reload (13 -> 14, no cock time)");

		// Lincoln: dmg 20, no hipfire spread, pitch 1.25
		ItemStack lincoln = new ItemStack(HbmItems.GUN_HENRY_LINCOLN.get());
		Receiver lrec = ((ItemGunBaseNT) lincoln.getItem()).getConfig(lincoln, 0).getReceivers(lincoln)[0];
		check(problems, cases, lrec.getBaseDamage(lincoln) == 20F && lrec.getHipfireSpread(lincoln) == 0F && lrec.getFirePitch(lincoln) == 1.25F && lrec.getMagazine(lincoln) instanceof MagazineSingleReload, "lincoln values");

		// Heavy revolver: dura 600, draw 10, inspect 23, L_CLASSIC, dmg 15, delay 14, reload 46, jam 23, mag 6
		ItemStack revolver = equip(player, HbmItems.GUN_HEAVY_REVOLVER.get(), stack(StandardAmmoType.M44_AP, 10));
		cfg = revolverGun.getConfig(revolver, 0);
		rec = cfg.getReceivers(revolver)[0];
		check(problems, cases, cfg.getDurability(revolver) == 600 && cfg.getDrawDuration(revolver) == 10 && cfg.getInspectDuration(revolver) == 23 && cfg.getCrosshair(revolver) == Crosshair.L_CLASSIC
				&& rec.getBaseDamage(revolver) == 15F && rec.getDelayAfterFire(revolver) == 14 && rec.getJamDuration(revolver) == 23 && rec.getMagazine(revolver) instanceof MagazineFullReload
				&& rec.getMagazine(revolver).getCapacity(revolver) == 6 && rec.getProjectileOffset(revolver).equals(new Vec3(0.75, -0.0625, -0.3125D)), "heavy_revolver values");
		reloadCycle(problems, cases, player, revolver, 46, 6, "heavy_revolver");
		fire(player, revolver, GunKeybind.GUN_PRIMARY);
		release(player, revolver, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.M44_AP && ItemGunBaseNT.getTimer(revolver, 0) == 14
				&& rec.getMagazine(revolver).getCasing(revolver, null) == XFactory44.CASING_M44_STEEL, "heavy_revolver AP shot");
		discardBullets(level);

		// Lil'Mac: loads ammo_secret m44_equestrian first (Pip), dura 31000, dmg 30
		ItemStack lilmac = equip(player, HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get(), stack(StandardAmmoType.M44_JHP, 6), secret.copyWithCount(6));
		cfg = ((ItemGunBaseNT) lilmac.getItem()).getConfig(lilmac, 0);
		rec = cfg.getReceivers(lilmac)[0];
		check(problems, cases, cfg.getDurability(lilmac) == 31_000 && rec.getBaseDamage(lilmac) == 30F, "lilmac values");
		reloadCycle(problems, cases, player, lilmac, 46, 6, "lilmac");
		check(problems, cases, rec.getMagazine(lilmac).getType(lilmac, null) == XFactory44.m44_jhp, "lilmac loads first inventory match (jhp slot first)");
		ItemStack lilmac2 = equip(player, HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get(), secret.copyWithCount(6));
		reloadCycle(problems, cases, player, lilmac2, 46, 6, "lilmac secret");
		check(problems, cases, rec.getMagazine(lilmac2).getType(lilmac2, null) == XFactory44.m44_equestrian_pip && player.getInventory().countItem(HbmItems.AMMO_SECRET.get()) == 0
				&& rec.getMagazine(lilmac2).getIconForHUD(lilmac2, player).is(HbmItems.AMMO_SECRET.get()), "lilmac equestrian reload");
		fire(player, lilmac2, GunKeybind.GUN_PRIMARY);
		release(player, lilmac2, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.M44_EQUESTRIAN_PIP && bullets.get(0).baseDamage() == 0F, "lilmac pip shot");
		discardBullets(level);

		// Equestrian impacts: Pip drops a boxcar 50 blocks above the hit, MN7 a torpedo
		BlockPos wall = new BlockPos(0, 301, 4);
		level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		shoot(player, XFactory44.m44_equestrian_pip, 30F);
		List<LegacyBoxcarEntity> boxcars = level.getEntitiesOfClass(LegacyBoxcarEntity.class, new AABB(-4, 340, 0, 4, 360, 8));
		check(problems, cases, boxcars.size() == 1 && Math.abs(boxcars.get(0).getY() - 351.62D) < 0.5D, "pip boxcar spawn " + boxcars.size());
		boxcars.forEach(Entity::discard);
		shoot(player, XFactory44.m44_equestrian_mn7, 30F);
		List<LegacyTorpedoEntity> torpedoes = level.getEntitiesOfClass(LegacyTorpedoEntity.class, new AABB(-4, 340, 0, 4, 360, 8));
		check(problems, cases, torpedoes.size() == 1, "mn7 torpedo spawn " + torpedoes.size());
		torpedoes.forEach(Entity::discard);
		level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
		discardBullets(level);

		// Protege: Mn7 first in the mag, pitch 0.8
		ItemStack protege = new ItemStack(HbmItems.GUN_HEAVY_REVOLVER_PROTEGE.get());
		Receiver prec = ((ItemGunBaseNT) protege.getItem()).getConfig(protege, 0).getReceivers(protege)[0];
		check(problems, cases, prec.getFirePitch(protege) == 0.8F && prec.getBaseDamage(protege) == 30F
				&& ((MagazineFullReload) prec.getMagazine(protege)).acceptedBullets.get(0) == XFactory44.m44_equestrian_mn7
				&& ((MagazineFullReload) prec.getMagazine(protege)).acceptedBullets.size() == 7, "protege values");

		// Torpedo: falls (0.04/t) and detonates ExplosionVNT(20).makeStandard on the first non-air block
		BlockPos target = new BlockPos(48, 300, 48);
		level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
		LegacyTorpedoEntity torpedo = new LegacyTorpedoEntity(level, new Vec3(48.5D, 306.0D, 48.5D));
		level.addFreshEntity(torpedo);
		int ticks = 0;
		while(!torpedo.isRemoved() && ticks < 60) { torpedo.tick(); ticks++; }
		check(problems, cases, torpedo.isRemoved() && ticks > 10 && ticks < 25 && level.getBlockState(target).isAir(), "torpedo fall + detonation ticks=" + ticks + " block=" + level.getBlockState(target));
		// EntityProcessorStandard in open air: range 2 * 20, damage (int) ((k * k + k) / 2 * 8 * range + 1), knockback away from the center
		Pig victim = EntityType.PIG.create(level);
		if(victim != null) {
			victim.setNoAi(true);
			victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
			victim.setHealth(1000F);
			victim.setPos(52.5D, 305.0D, 48.5D);
			level.addFreshEntity(victim);
			Vec3 c = new Vec3(48.5D, 305.5D, 48.5D);
			double scaled = Math.sqrt(victim.distanceToSqr(c)) / 40D;
			double k = 1D - scaled;
			float expected = (float) ((int) ((k * k + k) / 2.0D * 8.0D * 40D + 1.0D));
			LegacyTorpedoEntity.explodeStandard(level, c.x, c.y, c.z, 20F);
			check(problems, cases, Math.abs(1000F - victim.getHealth() - expected) < 0.01F && victim.getDeltaMovement().x > 0.5D, "VNT standard damage " + (1000F - victim.getHealth()) + " expected " + expected + " motion=" + victim.getDeltaMovement());
			victim.discard();
		}

		// Hangman: dura 600, inspect 31 (no cancel), dmg 25, delay 10, mag 8, RMB smack
		ItemStack hangman = equip(player, HbmItems.GUN_HANGMAN.get(), stack(StandardAmmoType.M44_FMJ, 8));
		cfg = ((ItemGunBaseNT) hangman.getItem()).getConfig(hangman, 0);
		rec = cfg.getReceivers(hangman)[0];
		check(problems, cases, cfg.getDurability(hangman) == 600 && cfg.getInspectDuration(hangman) == 31 && !cfg.getInspectCancel(hangman) && rec.getBaseDamage(hangman) == 25F
				&& rec.getDelayAfterFire(hangman) == 10 && rec.getMagazine(hangman).getCapacity(hangman) == 8 && cfg.getPressSecondary(hangman) == XFactory44.SMACK_A_FUCKER, "hangman values");
		reloadCycle(problems, cases, player, hangman, 46, 8, "hangman");
		Pig pig = EntityType.PIG.create(level);
		if(pig != null) {
			pig.setNoAi(true);
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
			pig.setHealth(1000F);
			pig.setPos(0.5D, 301.0D, 2.0D);
			level.addFreshEntity(pig);
			press(player, hangman, GunKeybind.GUN_SECONDARY);
			release(player, hangman, GunKeybind.GUN_SECONDARY);
			check(problems, cases, ItemGunBaseNT.getState(hangman, 0) == GunState.DRAWING && ItemGunBaseNT.getTimer(hangman, 0) == 31 && ItemGunBaseNT.getLastAnim(hangman, 0) == GunAnimation.INSPECT, "hangman smack start");
			tick(player, hangman, 16);
			check(problems, cases, pig.getHealth() == 1000F, "hangman smack not before timer 16");
			tick(player, hangman, 1);
			check(problems, cases, Math.abs(1000F - pig.getHealth() - 10F) < 0.01F && pig.getLastHurtByMob() == player, "hangman smack damage " + (1000F - pig.getHealth()));
			pig.discard();
		}

		discardBullets(level);
		player.getInventory().clearContent();

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_C2_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static boolean round(StandardAmmoType t, float dmg, boolean pen, boolean falloff, float dt, float ap, float head) {
		return t.damageMultiplier() == dmg && t.penetrates() == pen && t.damageFalloffByPenetration() == falloff && t.armorThresholdNegation() == dt
				&& t.armorPiercingPercent() == ap && t.headshotMultiplier() == head && t.knockbackMultiplier() == 0.1F && t.projectileSpeed() == 10D
				&& t.projectileLifetime() == 30 && t.projectileGravity() == 0D && t.selfDamageDelay() == 2 && t.projectileCount() == 1 && t.projectileSpread() == 0F
				&& t.ricochetAngle() == 5F && t.maxRicochetCount() == 2 && t.family() == StandardAmmoItem.AmmoFamily.LEGACY_BULLET;
	}

	private static void shaped(MinecraftServer server, List<String> problems, int[] cases, String id, Item result, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		var input = CraftingInput.of(w, h, grid);
		check(problems, cases, recipe instanceof ShapedRecipe s && s.getWidth() == w && s.getHeight() == h && s.matches(input, server.overworld())
				&& s.assemble(input, server.registryAccess()).is(result) && s.assemble(input, server.registryAccess()).getCount() == 1, id + " recipe");
	}

	/** LegacyPedestalRecipes holds the entry and every input resolves. */
	private static void pedestal(List<String> problems, int[] cases, String output) {
		boolean found = false;
		try {
			var field = com.atomiclegacy.tileentity.LegacyPedestalRecipes.class.getDeclaredField("RECIPES");
			field.setAccessible(true);
			for(Object recipe : (List<?>) field.get(null)) {
				var result = recipe.getClass().getDeclaredMethod("result");
				var available = recipe.getClass().getDeclaredMethod("available");
				result.setAccessible(true);
				available.setAccessible(true);
				ItemStack stack = (ItemStack) result.invoke(recipe);
				if(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(AtomicLegacy.id(output))) found = (boolean) available.invoke(recipe);
			}
		} catch(ReflectiveOperationException e) {
			found = false;
		}
		check(problems, cases, found, "pedestal recipe " + output);
	}

	private static LegacyBulletEntity shoot(FakePlayer player, BulletConfig config, float damage) {
		LegacyBulletEntity bullet = Lego.spawnBullet(player, config, damage, 0F, 0D, 0D, 1D);
		for(int i = 0; i < 3 && !bullet.isRemoved() && !bullet.isRetired(); i++) bullet.tick();
		return bullet;
	}

	private static ItemStack equip(FakePlayer player, Item gun, ItemStack... ammo) {
		player.getInventory().clearContent();
		player.getInventory().setItem(0, new ItemStack(gun));
		for(int i = 0; i < ammo.length; i++) player.getInventory().setItem(i + 1, ammo[i]);
		ItemStack stack = player.getInventory().getItem(0);
		tick(player, stack, 1);
		return stack;
	}

	private static void reloadCycle(List<String> problems, int[] cases, FakePlayer player, ItemStack gun, int duration, int expected, String name) {
		IMagazine<?> mag = ((ItemGunBaseNT) gun.getItem()).getConfig(gun, 0).getReceivers(gun)[0].getMagazine(gun);
		press(player, gun, GunKeybind.RELOAD);
		release(player, gun, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(gun, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(gun, 0) == duration && ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.RELOAD, name + " reload start");
		tick(player, gun, duration - 1);
		check(problems, cases, mag.getAmount(gun, null) == 0, name + " reload finished early");
		tick(player, gun, 2);
		check(problems, cases, mag.getAmount(gun, null) == expected && ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, name + " reload result " + mag.getAmount(gun, null));
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
		if(!ok) problems.add("gun batch C2: " + message);
	}
}
