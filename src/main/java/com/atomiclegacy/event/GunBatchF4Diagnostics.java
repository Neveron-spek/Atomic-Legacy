package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF4;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory35800;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryFolly;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryRocket;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunStinger;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
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
 * Dedicated-server checks for weapons batch F4 (XFactoryFolly: gun_folly, folly_sm, folly_nuke; XFactory35800: gun_aberrator,
 * gun_aberrator_eott, p35800, p35800_bl; XFactoryRocket: gun_stinger) against the pinned 1.7.10 values. The folly_nuke
 * detonation (MK5 radius 100) is not fired on the audit server; only its configuration and the early entity hit guard are checked.
 */
final class GunBatchF4Diagnostics {

	private GunBatchF4Diagnostics() {
	}

	private static final AABB AREA = new AABB(-24, 280, -24, 24, 320, 60);

	static int run(MinecraftServer server, List<String> problems) {
		karmaCases(server, problems);
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, creative tab ///
		Object[][] guns = {
				{ HbmItems.GUN_STINGER.get(), WeaponQuality.SPECIAL, 1 },
				{ HbmItems.GUN_FOLLY.get(), WeaponQuality.SECRET, 1 },
				{ HbmItems.GUN_ABERRATOR.get(), WeaponQuality.SECRET, 1 },
				{ HbmItems.GUN_ABERRATOR_EOTT.get(), WeaponQuality.SECRET, 2 },
		};
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			boolean secret = g[1] == WeaponQuality.SECRET;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == (int) g[2]
					&& ItemGunBaseNT.secrets.contains(item) == secret && (indexOf(tab, item) >= 0) != secret, name + " stack/quality/configs/tab");
		}
		check(problems, cases, HbmItems.GUN_STINGER.get() instanceof ItemGunStinger, "gun_stinger is an ItemGunStinger");
		ItemStack def = ((ItemGunBaseNT) HbmItems.GUN_STINGER.get()).getDefaultAmmo();
		check(problems, cases, StandardAmmoItem.standardType(def) == StandardAmmoType.ROCKET_HEAT && def.getCount() == 3, "gun_stinger default ammo " + def);
		int las = indexOf(tab, HbmItems.GUN_LASRIFLE.get()), sting = indexOf(tab, HbmItems.GUN_STINGER.get()), ff = indexOf(tab, HbmItems.GUN_FLIEGERFAUST.get());
		check(problems, cases, las >= 0 && las < sting && sting < ff, "weapon tab order lasrifle/stinger/fliegerfaust " + las + "/" + sting + "/" + ff);

		/// Config, sounds ///
		check(problems, cases, HbmConfig.stingerDamage() == 35F && HbmConfig.follyDamage() == 1_000F && HbmConfig.aberratorDamage() == 100F
				&& HbmConfig.eottLeftDamage() == 100F && HbmConfig.eottRightDamage() == 100F, "config damage defaults");
		check(problems, cases, HbmSoundEvents.GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS.getId().getPath().equals("weapon.fire.loudestnoiseonearth")
				&& HbmSoundEvents.GUN_ABERRATOR_FIRE.getId().getPath().equals("weapon.fire.aberrator") && HbmSoundEvents.GUN_LOCKON.getId().getPath().equals("weapon.fire.lockon")
				&& HbmSoundEvents.GUN_SCREW.getId().getPath().equals("weapon.reload.screw") && HbmSoundEvents.GUN_ROCKET_INSERT.getId().getPath().equals("weapon.reload.insertrocket")
				&& HbmSoundEvents.GUN_VSTAR_JINGLE.getId().getPath().equals("weapon.fire.vstar"), "sound ids");

		/// Ammo configs ///
		BeamBulletConfig sm = XFactoryFolly.folly_sm;
		StandardAmmoType smT = StandardAmmoType.FOLLY_SM;
		check(problems, cases, sm.ammo == smT && sm.dmgClass == DamageClass.SUBATOMIC && sm.expires == 100 && sm.spectral && sm.doesPenetrate() && !sm.renderRotations
				&& sm.onUpdateBeam == XFactoryFolly.LAMBDA_SM_UPDATE && sm.onImpactBeam == null && sm.beamRenderer == BeamBulletConfig.RENDER_FOLLY && sm.damageMult() == 1F
				&& sm.armorPiercing() == 0F && sm.thresholdNegation() == 0F && sm.wear == 1F && sm.ammoReloadCount == 1 && sm.casingEffect == -1
				&& smT.projectileSpeed() == 2D && smT.projectileGravity() == 0.015D && smT.projectileLifetime() == 100 && smT.spectral() && smT.headshotMultiplier() == 1.25F
				&& sm.matches(secret("folly_sm", 1)) && !sm.matches(secret("folly_nuke", 1)) && !sm.matches(stack(StandardAmmoType.ROCKET_HE, 1))
				&& ((LegacyVariantItem) sm.toStack().getItem()).variant(sm.toStack()).id().equals("folly_sm"), "folly_sm values");
		StandardAmmoType nk = StandardAmmoType.FOLLY_NUKE;
		check(problems, cases, XFactoryFolly.folly_nuke.ammo == nk && nk.projectileLifetime() == 600 && nk.projectileSpeed() == 4D && nk.projectileGravity() == 0.015D
				&& nk.onImpact() == XFactoryFolly.LAMBDA_NUKE_IMPACT && nk.onUpdate() == null && nk.damageMultiplier() == 1F && !nk.penetrates() && !nk.spectral()
				&& XFactoryFolly.folly_nuke.matches(secret("folly_nuke", 1)) && !XFactoryFolly.folly_nuke.matches(secret("folly_sm", 1)) && XFactoryFolly.folly_nuke.casingEffect == -1, "folly_nuke values");
		BeamBulletConfig[] p = { XFactory35800.p35800, XFactory35800.p35800_bl };
		String[] pv = { "p35_800", "p35_800_bl" };
		StandardAmmoType[] pt = { StandardAmmoType.P35_800, StandardAmmoType.P35_800_BL };
		for(int i = 0; i < 2; i++) {
			BeamBulletConfig c = p[i];
			check(problems, cases, c.ammo == pt[i] && c.armorPiercing() == 0.5F && c.thresholdNegation() == 50F && c.expires == 3 && c.spread() == 0F && !c.renderRotations && !c.spectral
					&& !c.doesPenetrate() && c.damageMult() == 1F && c.casingEffect == OrchestrasF4.CASING_P35_800 && c.dmgClass == DamageClass.LASER
					&& c.onImpactBeam == (i == 0 ? BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT : XFactory35800.LAMBDA_BLACK_IMPACT)
					&& c.beamRenderer == (i == 0 ? BeamBulletConfig.RENDER_CRACKLE : BeamBulletConfig.RENDER_BLACK_LIGHTNING)
					&& c.matches(secret(pv[i], 1)) && !c.matches(secret(pv[1 - i], 1)), pv[i] + " values");
		}

		/// Recipes: gun_stinger (WeaponRecipes), pedestals ///
		checkRecipe(problems, cases, server, level, "gun_stinger", HbmItems.GUN_STINGER.get(), 3, 2, List.of(item("part_barrel_heavy_weaponsteel"), item("part_barrel_heavy_weaponsteel"), item("part_barrel_heavy_weaponsteel"),
				item("circuit_advanced"), item("part_grip_weaponsteel"), item("part_mechanism_weaponsteel")));
		var recipes = server.getRecipeManager();
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_folly")).isEmpty() && recipes.byKey(AtomicLegacy.id("gun_aberrator")).isEmpty() && recipes.byKey(AtomicLegacy.id("gun_aberrator_eott")).isEmpty(),
				"no crafting recipe for the secret guns");
		check(problems, cases, pedestal("gun_folly", "FULL_MOON", "itemId=item_secret_folly, variant=null, tagPath=null, count=4", "itemId=item_secret_controller, variant=null, tagPath=null, count=2",
				"tagPath=ingots/bscco, count=16", "tagPath=storage_blocks/starmetal, count=64"), "pedestal recipe gun_folly (FULL_MOON)");
		check(problems, cases, pedestal("gun_aberrator", "NONE", "itemId=item_secret_aberrator, variant=null, tagPath=null, count=1", "tagPath=gun_parts/mechanism/saturnite, count=4"), "pedestal recipe gun_aberrator");
		check(problems, cases, pedestal("gun_aberrator_eott", "GOOD_KARMA", "itemId=item_secret_aberrator, variant=null, tagPath=null, count=1", "tagPath=gun_parts/mechanism/saturnite, count=16"), "pedestal recipe gun_aberrator_eott (GOOD_KARMA)");
		for(String id : new String[] { "item_secret_folly", "item_secret_controller", "item_secret_aberrator", "circuit_advanced", "part_grip_weaponsteel", "part_barrel_heavy_weaponsteel", "part_mechanism_weaponsteel" }) {
			check(problems, cases, BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(id)), "pedestal/recipe ingredient " + id + " exists");
		}
		for(String tag : new String[] { "ingots/bscco", "storage_blocks/starmetal", "gun_parts/mechanism/saturnite" }) {
			check(problems, cases, BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", tag))).map(t -> t.size() > 0).orElse(false), "pedestal tag c:" + tag + " non-empty");
		}

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.tickCount = 400; // ContaminationUtil skips players younger than 200 ticks
		player.getInventory().selected = 0;
		discard(level);

		// Folly: dura 0, draw 40, NONE, delay 26, no dry fire, reload 160, jam 0, mag 1, sound volume 100
		ItemStack fol = equip(player, HbmItems.GUN_FOLLY.get(), secret("folly_sm", 2));
		GunConfig cfg = ((ItemGunBaseNT) fol.getItem()).getConfig(fol, 0);
		Receiver rec = cfg.getReceivers(fol)[0];
		IMagazine<?> mag = rec.getMagazine(fol);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(fol) == 0 && cfg.getDrawDuration(fol) == 40 && cfg.getCrosshair(fol) == Crosshair.NONE
				&& rec.getBaseDamage(fol) == 1_000F && rec.getDelayAfterFire(fol) == 26 && !rec.getDoesDryFire(fol) && rec.getReloadBeginDuration(fol) == 160 && rec.getJamDuration(fol) == 0
				&& mag.getCapacity(fol) == 1 && rec.getProjectileOffset(fol).equals(new Vec3(0.75, -0.0625, -0.1875D)) && rec.getProjectileOffsetScoped(fol).equals(new Vec3(0.75, -0.0625, -0.125D))
				&& rec.getFireSound(fol).get() == HbmSoundEvents.GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS.get() && rec.getFireVolume(fol) == 100F && rec.getCanFire(fol) == XFactoryFolly.LAMBDA_CAN_FIRE
				&& cfg.getPressTertiary(fol) == XFactoryFolly.LAMBDA_TOGGLE_AIM && cfg.getOrchestra(fol) == OrchestrasF4.ORCHESTRA_FOLLY, "folly values");
		press(player, fol, GunKeybind.RELOAD);
		release(player, fol, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(fol, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(fol, 0) == 160, "folly reload start");
		tick(player, fol, 161);
		check(problems, cases, mag.getAmount(fol, null) == 1 && countSecret(player, "folly_sm") == 1, "folly reload 1 round");
		tick(player, fol, 3);
		fire(player, fol, GunKeybind.GUN_PRIMARY);
		release(player, fol, GunKeybind.GUN_PRIMARY);
		check(problems, cases, beams(level).isEmpty() && mag.getAmount(fol, null) == 1, "folly: no shot without aiming");
		press(player, fol, GunKeybind.GUN_TERTIARY);
		release(player, fol, GunKeybind.GUN_TERTIARY);
		check(problems, cases, ItemGunBaseNT.getIsAiming(fol) && ItemGunBaseNT.getLastAnim(fol, 0) == GunAnimation.SPINUP && ItemGunBaseNT.getAnimTimer(fol, 0) == 0, "folly aim toggle plays SPINUP");
		tick(player, fol, 50);
		fire(player, fol, GunKeybind.GUN_PRIMARY);
		release(player, fol, GunKeybind.GUN_PRIMARY);
		check(problems, cases, beams(level).isEmpty() && ItemGunBaseNT.getAnimTimer(fol, 0) < 100, "folly: no shot before SPINUP 100 (timer " + ItemGunBaseNT.getAnimTimer(fol, 0) + ")");
		tick(player, fol, 50);
		// blocks and a pig along the beam (+z from the muzzle at about (0.375, 301.56, 1.25))
		BlockPos stone1 = new BlockPos(0, 301, 10), stone2 = new BlockPos(1, 300, 20);
		level.setBlock(stone1, Blocks.STONE.defaultBlockState(), 3);
		level.setBlock(stone2, Blocks.STONE.defaultBlockState(), 3);
		Pig smPig = pig(level, new Vec3(0.5D, 301.0D, 15.5D));
		float radBefore = HbmLivingRadiation.get(player).getRadiation();
		fire(player, fol, GunKeybind.GUN_PRIMARY);
		release(player, fol, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletBeamEntity> beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == sm && Math.abs(beams.get(0).damage - 1_000F) < 0.001F && beams.get(0).beamLength > 200
				&& mag.getAmount(fol, null) == 0 && ItemGunBaseNT.getTimer(fol, 0) == 26 && ItemGunBaseNT.getAnimTimer(fol, 0) == 0 && ItemGunBaseNT.getLastAnim(fol, 0) == GunAnimation.CYCLE,
				"folly SM shot n=" + beams.size() + (beams.isEmpty() ? "" : " len=" + beams.get(0).beamLength + " dmg=" + beams.get(0).damage));
		if(beams.size() == 1) {
			LegacyBulletBeamEntity beam = beams.get(0);
			check(problems, cases, level.getBlockState(stone1).is(Blocks.STONE) && smPig != null && smPig.getHealth() == 1000F, "folly SM: nothing happens before tick 2");
			for(int i = 0; i < 2; i++) { beam.tickCount++; beam.tick(); }
			float rad = HbmLivingRadiation.get(player).getRadiation();
			check(problems, cases, level.getBlockState(stone1).isAir() && level.getBlockState(stone2).isAir(), "folly SM erases 3x3x3 along the beam");
			check(problems, cases, smPig != null && smPig.getHealth() < 1000F, "folly SM damages entities along the beam (pig " + (smPig == null ? "null" : smPig.getHealth()) + ")");
			check(problems, cases, Math.abs(rad - radBefore - 150F) < 0.01F, "folly SM contaminates the shooter by 150 (got " + (rad - radBefore) + ")");
			for(int i = 0; i < 100; i++) { beam.tickCount++; beam.tick(); }
			check(problems, cases, beam.isRemoved(), "folly SM beam expires after 100 ticks");
		}
		if(smPig != null) smPig.discard();
		clearBlocks(level, stone1);
		clearBlocks(level, stone2);
		discardBeams(level);
		HbmLivingRadiation.get(player).setRadiation(radBefore);
		// folly_nuke: entity hit within the first 2 ticks keeps the round alive (no detonation on the audit server)
		LegacyBulletEntity nukeRound = Lego.spawnBullet(player, XFactoryFolly.folly_nuke, 1_000F, 0F, 0D, -0.0625D, 0.75D);
		Pig guard = pig(level, new Vec3(0.5D, 301.5D, 3.5D));
		boolean early = guard != null && !XFactoryFolly.LAMBDA_NUKE_IMPACT.onImpact(nukeRound, new net.minecraft.world.phys.EntityHitResult(guard)) && !nukeRound.isRemoved();
		check(problems, cases, early && nukeRound.ammoType() == nk && Math.abs(nukeRound.baseDamage() - 1_000F) < 0.001F && nukeRound.getDeltaMovement().length() > 3.9D, "folly_nuke round: early entity hit ignored, velocity 4");
		if(guard != null) guard.discard();
		discard(level);

		// Aberrator: dura 2000, draw 10, inspect 26, CIRCLE, smoke, delay 13, dry 21, reload 51, mag 5 (full reload)
		ItemStack ab = equip(player, HbmItems.GUN_ABERRATOR.get(), secret("p35_800", 7));
		cfg = ((ItemGunBaseNT) ab.getItem()).getConfig(ab, 0);
		rec = cfg.getReceivers(ab)[0];
		mag = rec.getMagazine(ab);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(ab) == 2_000 && cfg.getDrawDuration(ab) == 10 && cfg.getInspectDuration(ab) == 26 && cfg.getCrosshair(ab) == Crosshair.CIRCLE
				&& cfg.getSmokeHandler(ab) == Lego.LAMBDA_STANDARD_SMOKE && rec.getBaseDamage(ab) == 100F && rec.getDelayAfterFire(ab) == 13 && rec.getDelayAfterDryFire(ab) == 21 && rec.getReloadBeginDuration(ab) == 51
				&& mag.getCapacity(ab) == 5 && rec.getProjectileOffset(ab).equals(new Vec3(0.75, -0.0625 * 1.5, -0.1875)) && rec.getFireSound(ab).get() == HbmSoundEvents.GUN_ABERRATOR_FIRE.get()
				&& rec.getOnFire(ab) == XFactory35800.LAMBDA_NOWEAR_FIRE && cfg.getOrchestra(ab) == OrchestrasF4.ORCHESTRA_ABERRATOR, "aberrator values");
		press(player, ab, GunKeybind.RELOAD);
		release(player, ab, GunKeybind.RELOAD);
		tick(player, ab, 52);
		check(problems, cases, mag.getAmount(ab, null) == 5 && countSecret(player, "p35_800") == 2, "aberrator reload 5 rounds");
		float wearBefore = ItemGunBaseNT.getWear(ab, 0);
		Pig abPig = pig(level, new Vec3(0.5D, player.getEyeY() - 0.45D, 12.5D));
		fire(player, ab, GunKeybind.GUN_PRIMARY);
		release(player, ab, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactory35800.p35800 && Math.abs(beams.get(0).damage - 100F) < 0.001F && mag.getAmount(ab, null) == 4
				&& ItemGunBaseNT.getTimer(ab, 0) == 13 && ItemGunBaseNT.getWear(ab, 0) == wearBefore, "aberrator shot (no wear) n=" + beams.size());
		check(problems, cases, abPig != null && Math.abs(abPig.getHealth() - 900F) < 0.001F && HbmLivingHazards.get(abPig).getBlackFire() == 0, "p35800 hit: 100 damage, no black fire (pig " + (abPig == null ? "null" : abPig.getHealth()) + ")");
		if(abPig != null) abPig.discard();
		discardBeams(level);
		// black lightning: black fire 200 on living entities, lingering black fire 7.5 x 2 for 200 ticks on blocks
		ItemStack abl = equip(player, HbmItems.GUN_ABERRATOR.get(), secret("p35_800_bl", 5));
		press(player, abl, GunKeybind.RELOAD);
		release(player, abl, GunKeybind.RELOAD);
		tick(player, abl, 52);
		Pig blPig = pig(level, new Vec3(0.5D, player.getEyeY() - 0.45D, 12.5D));
		fire(player, abl, GunKeybind.GUN_PRIMARY);
		release(player, abl, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactory35800.p35800_bl && blPig != null && Math.abs(blPig.getHealth() - 900F) < 0.001F
				&& HbmLivingHazards.get(blPig).getBlackFire() == 200, "p35800_bl hit: 100 damage + black fire 200 (pig " + (blPig == null ? "null" : blPig.getHealth() + "/" + HbmLivingHazards.get(blPig).getBlackFire()) + ")");
		if(blPig != null) blPig.discard();
		discardBeams(level);
		tick(player, abl, 14);
		BlockPos wall = new BlockPos(0, 301, 10);
		for(int dx = -2; dx <= 2; dx++) for(int dy = -2; dy <= 2; dy++) level.setBlock(wall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState(), 3);
		fire(player, abl, GunKeybind.GUN_PRIMARY);
		release(player, abl, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		List<LegacyLingeringFireEntity> fires = level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
		check(problems, cases, beams.size() == 1 && Math.abs(beams.get(0).beamLength - (10D - 1.25D)) < 0.5D && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 7.5F) < 0.01F
				&& Math.abs(fires.get(0).getBbHeight() - 2F) < 0.01F, "p35800_bl block hit: lingering black fire 7.5x2 n=" + fires.size() + " len=" + (beams.isEmpty() ? -1 : beams.get(0).beamLength));
		for(LegacyLingeringFireEntity e : fires) e.discard();
		clearBlocks(level, wall);
		discardBeams(level);

		// EOTT: two configs, left mag index 0 (offset +0.1875, primary), right mag index 1 (offset -0.1875, secondary)
		ItemStack eott = equip(player, HbmItems.GUN_ABERRATOR_EOTT.get(), secret("p35_800", 12));
		ItemGunBaseNT eGun = (ItemGunBaseNT) eott.getItem();
		GunConfig left = eGun.getConfig(eott, 0), right = eGun.getConfig(eott, 1);
		IMagazine<?> magL = left.getReceivers(eott)[0].getMagazine(eott), magR = right.getReceivers(eott)[0].getMagazine(eott);
		check(problems, cases, magL instanceof MagazineFullReload l && l.index == 0 && magR instanceof MagazineFullReload r && r.index == 1 && magL.getCapacity(eott) == 5 && magR.getCapacity(eott) == 5
				&& left.getReceivers(eott)[0].getProjectileOffset(eott).equals(new Vec3(0.75, -0.0625 * 1.5, 0.1875)) && right.getReceivers(eott)[0].getProjectileOffset(eott).equals(new Vec3(0.75, -0.0625 * 1.5, -0.1875))
				&& left.getReceivers(eott)[0].getHipfireSpread(eott) == 0F && right.getReceivers(eott)[0].getHipfireSpread(eott) == 0F && left.getReceivers(eott)[0].getBaseDamage(eott) == 100F && right.getReceivers(eott)[0].getBaseDamage(eott) == 100F
				&& left.getPressPrimary(eott) == Lego.LAMBDA_STANDARD_CLICK_PRIMARY && left.getPressSecondary(eott) == null && right.getPressSecondary(eott) == Lego.LAMBDA_STANDARD_CLICK_PRIMARY && right.getPressPrimary(eott) == null
				&& left.getDurability(eott) == 2_000 && right.getDurability(eott) == 2_000, "eott values");
		press(player, eott, GunKeybind.RELOAD);
		release(player, eott, GunKeybind.RELOAD);
		tick(player, eott, 52);
		check(problems, cases, magL.getAmount(eott, null) == 5 && magR.getAmount(eott, null) == 5 && countSecret(player, "p35_800") == 2, "eott reload both magazines");
		fire(player, eott, GunKeybind.GUN_PRIMARY);
		release(player, eott, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && magL.getAmount(eott, null) == 4 && magR.getAmount(eott, null) == 5 && beams.get(0).getX() > 0.5D, "eott primary fires the left gun n=" + beams.size());
		discardBeams(level);
		fire(player, eott, GunKeybind.GUN_SECONDARY);
		release(player, eott, GunKeybind.GUN_SECONDARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && magL.getAmount(eott, null) == 4 && magR.getAmount(eott, null) == 4 && beams.get(0).getX() < 0.5D, "eott secondary fires the right gun n=" + beams.size());
		discardBeams(level);
		discard(level);

		// Stinger: dura 300, draw 7, inspect 40, L_BOX_OUTLINE, dmg 35, delay 5, reload 50, jam 40, mag 1, lock-on fire
		ItemStack st = equip(player, HbmItems.GUN_STINGER.get(), stack(StandardAmmoType.ROCKET_HEAT, 3));
		cfg = ((ItemGunBaseNT) st.getItem()).getConfig(st, 0);
		rec = cfg.getReceivers(st)[0];
		mag = rec.getMagazine(st);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(st) == 300 && cfg.getDrawDuration(st) == 7 && cfg.getInspectDuration(st) == 40 && cfg.getCrosshair(st) == Crosshair.L_BOX_OUTLINE
				&& rec.getBaseDamage(st) == 35F && rec.getDelayAfterFire(st) == 5 && rec.getReloadBeginDuration(st) == 50 && rec.getJamDuration(st) == 40 && mag.getCapacity(st) == 1
				&& rec.getProjectileOffset(st).equals(new Vec3(1, -0.0625 * 1.5, -0.1875D)) && rec.getFireSound(st).get() == HbmSoundEvents.GUN_ROCKET_FIRE.get()
				&& rec.getCanFire(st) == Lego.LAMBDA_LOCKON_CAN_FIRE && rec.getOnFire(st) == Lego.LAMBDA_LOCKON_FIRE && cfg.getPressSecondary(st) == XFactoryRocket.LAMBDA_STINGER_SECONDARY_PRESS
				&& cfg.getReleaseSecondary(st) == XFactoryRocket.LAMBDA_STINGER_SECONDARY_RELEASE && cfg.getOrchestra(st) == OrchestrasF4.ORCHESTRA_STINGER, "stinger values");
		press(player, st, GunKeybind.RELOAD);
		release(player, st, GunKeybind.RELOAD);
		tick(player, st, 51);
		check(problems, cases, mag.getAmount(st, null) == 1 && countAmmo(player, StandardAmmoType.ROCKET_HEAT) == 2, "stinger reload 1 rocket");
		tick(player, st, 3);
		Pig target = pig(level, new Vec3(2.5D, player.getEyeY() - 0.45D, 30D));
		fire(player, st, GunKeybind.GUN_PRIMARY);
		release(player, st, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).isEmpty() && mag.getAmount(st, null) == 1, "stinger: no shot without lock-on");
		ItemGunBaseNT.setIsAiming(st, true);
		press(player, st, GunKeybind.GUN_SECONDARY);
		check(problems, cases, ItemGunStinger.getIsLockingOn(st), "stinger secondary press starts locking on");
		tick(player, st, 59);
		check(problems, cases, target != null && ItemGunStinger.getLockonProgress(st) == 59 && !ItemGunBaseNT.getIsLockedOn(st) && ItemGunBaseNT.getLockonTarget(st) == target.getId(),
				"stinger lock-on progress 59 (" + ItemGunStinger.getLockonProgress(st) + ", target " + ItemGunBaseNT.getLockonTarget(st) + ")");
		fire(player, st, GunKeybind.GUN_PRIMARY);
		release(player, st, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).isEmpty(), "stinger: no shot before the lock-on completes");
		tick(player, st, 1);
		check(problems, cases, ItemGunStinger.getLockonProgress(st) == 60 && ItemGunBaseNT.getIsLockedOn(st), "stinger locked on after 60 ticks");
		// the refused shot above was a dry fire (CYCLE_DRY, cooldown delayAfterDryFire 5); wait for IDLE, the lock stays
		tick(player, st, 5);
		check(problems, cases, ItemGunBaseNT.getState(st, 0) == GunState.IDLE && ItemGunBaseNT.getIsLockedOn(st), "stinger idle and still locked on");
		fire(player, st, GunKeybind.GUN_PRIMARY);
		release(player, st, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, target != null && bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_RPZB_HEAT && bullets.get(0).lockonTarget() == target
				&& !ItemGunBaseNT.getIsLockedOn(st) && Math.abs(bullets.get(0).baseDamage() - 17.5F) < 0.001F && mag.getAmount(st, null) == 0 && ItemGunBaseNT.getTimer(st, 0) == 5,
				"stinger lock-on shot n=" + bullets.size());
		if(!bullets.isEmpty() && target != null) {
			LegacyBulletEntity m = bullets.get(0);
			double x0 = m.getX();
			for(int i = 0; i < 6; i++) { m.tickCount++; m.tick(); }
			check(problems, cases, m.legacyMotion.x > 0D && m.getX() > x0 && Math.abs(m.legacyMotion.length() - 1D) < 1.0E-3D, "stinger rocket homes on the target motion.x=" + m.legacyMotion.x);
		}
		release(player, st, GunKeybind.GUN_SECONDARY);
		tick(player, st, 1);
		check(problems, cases, !ItemGunStinger.getIsLockingOn(st) && ItemGunStinger.getLockonProgress(st) == 0 && !ItemGunBaseNT.getIsLockedOn(st), "stinger secondary release resets the lock-on");
		press(player, st, GunKeybind.GUN_SECONDARY);
		st.getItem().inventoryTick(st, level, player, 0, false);
		check(problems, cases, !ItemGunStinger.getIsLockingOn(st), "stinger not held: locking on reset");
		release(player, st, GunKeybind.GUN_SECONDARY);
		if(target != null) target.discard();
		discard(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_F4_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	@SuppressWarnings("unchecked")
	private static boolean pedestal(String output, String condition, String... inputs) {
		try {
			var field = com.atomiclegacy.tileentity.LegacyPedestalRecipes.class.getDeclaredField("RECIPES");
			field.setAccessible(true);
			for(Object o : (List<Object>) field.get(null)) {
				Object[] input = (Object[]) o.getClass().getMethod("input").invoke(o);
				String out = o.getClass().getMethod("output").invoke(o).toString();
				if(input.length != 9 || !out.contains("itemId=" + output + ",")) continue;
				String s = out + java.util.Arrays.toString(input) + o.getClass().getMethod("condition").invoke(o);
				boolean ok = s.contains(condition);
				for(String in : inputs) ok &= s.contains(in);
				if(ok) return true;
			}
		} catch(ReflectiveOperationException e) {
			return false;
		}
		return false;
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

	private static int countSecret(FakePlayer player, String variant) {
		int count = 0;
		for(int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if(s.is(HbmItems.AMMO_SECRET.get()) && ((LegacyVariantItem) s.getItem()).variant(s).id().equals(variant)) count += s.getCount();
		}
		return count;
	}

	private static ItemStack stack(StandardAmmoType type, int count) {
		return StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), type).copyWithCount(count);
	}

	private static ItemStack secret(String variant, int count) {
		return LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), variant).copyWithCount(count);
	}

	private static ItemStack item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
	}

	private static List<LegacyBulletEntity> bullets(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
	}

	private static List<LegacyBulletBeamEntity> beams(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA);
	}

	private static void discard(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
	}

	private static void discardBeams(ServerLevel level) {
		for(LegacyBulletBeamEntity beam : level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA)) beam.discard();
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun batch F4: " + message);
	}

	/** BlockPedestal GOOD_KARMA / BAD_KARMA: a player within 20 blocks with reputation >= 10 / <= -10. */
	private static void karmaCases(MinecraftServer server, List<String> problems) {
		net.minecraft.server.level.ServerLevel level = server.overworld();
		net.minecraft.core.BlockPos center = new net.minecraft.core.BlockPos(0, 250, 0);
		net.neoforged.neoforge.common.util.FakePlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
				new com.mojang.authlib.GameProfile(java.util.UUID.fromString("7a1b7c2e-0d4e-4c1a-9f3e-5a6b7c8d9e01"), "[HBM karma audit]"));
		level.getChunk(center);
		level.addFreshEntity(player);
		int before = com.atomiclegacy.player.HbmPlayerReputation.get(player);
		java.util.function.BiConsumer<Boolean, String> check = (ok, what) -> { if(!ok) problems.add("pedestal karma: " + what); };
		try {
			player.setPos(center.getX() + 10.5, center.getY(), center.getZ() + 0.5);
			com.atomiclegacy.player.HbmPlayerReputation.set(player, 10);
			check.accept(com.atomiclegacy.tileentity.LegacyPedestalRecipes.conditionMatches("GOOD_KARMA", level, center), "reputation 10 within 20 blocks enables GOOD_KARMA");
			check.accept(!com.atomiclegacy.tileentity.LegacyPedestalRecipes.conditionMatches("BAD_KARMA", level, center), "reputation 10 does not enable BAD_KARMA");
			com.atomiclegacy.player.HbmPlayerReputation.set(player, 9);
			check.accept(!com.atomiclegacy.tileentity.LegacyPedestalRecipes.conditionMatches("GOOD_KARMA", level, center), "reputation 9 does not enable GOOD_KARMA");
			com.atomiclegacy.player.HbmPlayerReputation.set(player, -10);
			check.accept(com.atomiclegacy.tileentity.LegacyPedestalRecipes.conditionMatches("BAD_KARMA", level, center), "reputation -10 enables BAD_KARMA");
			com.atomiclegacy.player.HbmPlayerReputation.set(player, 25);
			player.setPos(center.getX() + 22.5, center.getY(), center.getZ() + 0.5);
			check.accept(!com.atomiclegacy.tileentity.LegacyPedestalRecipes.conditionMatches("GOOD_KARMA", level, center), "player outside the 20 block box is ignored");
		} finally {
			com.atomiclegacy.player.HbmPlayerReputation.set(player, before);
			player.discard();
		}
	}
}
