package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.AmmoFamily;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch C3 (XFactory762mm: gun_carbine, gun_minigun, gun_minigun_lacunae,
 * gun_mas36, the r762 rounds and the lacunae capacitor beams) against the pinned 1.7.10 values.
 */
final class GunBatchC3Diagnostics {

	private GunBatchC3Diagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 12);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_CARBINE.get(), WeaponQuality.A_SIDE, StandardAmmoType.R762_SP, 14 },
				{ HbmItems.GUN_MINIGUN.get(), WeaponQuality.A_SIDE, StandardAmmoType.R762_FMJ, 30 },
				{ HbmItems.GUN_MINIGUN_LACUNAE.get(), WeaponQuality.LEGENDARY, StandardAmmoType.CAPACITOR, 15 },
				{ HbmItems.GUN_MAS36.get(), WeaponQuality.LEGENDARY, StandardAmmoType.R762_AP, 14 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == 1, name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, StandardAmmoItem.standardType(def) == g[2] && def.getCount() == (int) g[3], name + " default ammo " + def);
		}
		check(problems, cases, ItemGunBaseNT.secrets.contains(HbmItems.GUN_MINIGUN_LACUNAE.get()) && ItemGunBaseNT.secrets.contains(HbmItems.GUN_MAS36.get())
				&& !ItemGunBaseNT.secrets.contains(HbmItems.GUN_CARBINE.get()), "LEGENDARY secrets");

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int stg = indexOf(tab, HbmItems.GUN_STG77.get()), carbine = indexOf(tab, HbmItems.GUN_CARBINE.get()), minigun = indexOf(tab, HbmItems.GUN_MINIGUN.get()), ammo = indexOf(tab, HbmItems.AMMO_STANDARD.get());
		check(problems, cases, carbine >= 0 && carbine < indexOf(tab, HbmItems.GUN_AM180.get()) && carbine < minigun && minigun < stg && stg < ammo, "weapon tab carbine/minigun order (ModItems) " + stg + "/" + carbine + "/" + minigun + "/" + ammo);
		check(problems, cases, indexOf(tab, HbmItems.GUN_MINIGUN_LACUNAE.get()) < 0 && indexOf(tab, HbmItems.GUN_MAS36.get()) < 0, "LEGENDARY guns not in the creative tab");

		/// Ammo values (BulletConfig) ///
		check(problems, cases, round(StandardAmmoType.R762_SP, 1F, false, true, 0F, 0F, 1.25F), "r762_sp values");
		check(problems, cases, round(StandardAmmoType.R762_FMJ, 0.8F, false, true, 5F, 0.1F, 1.25F), "r762_fmj values");
		check(problems, cases, round(StandardAmmoType.R762_JHP, 1.5F, false, true, 0F, -0.25F, 1.5F), "r762_jhp values");
		check(problems, cases, round(StandardAmmoType.R762_AP, 1.25F, true, false, 12.5F, 0.15F, 1.25F), "r762_ap values");
		check(problems, cases, round(StandardAmmoType.R762_DU, 1.5F, true, false, 15F, 0.25F, 1.25F), "r762_du values");
		check(problems, cases, round(StandardAmmoType.R762_HE, 1.75F, false, true, 0F, 0F, 1.25F) && StandardAmmoType.R762_HE.onImpact() != null, "r762_he values");
		check(problems, cases, StandardAmmoType.R762_SP.family() == AmmoFamily.LEGACY_BULLET && StandardAmmoType.R762_FMJ.family() == AmmoFamily.LEGACY_BULLET
				&& StandardAmmoType.R762_SP.onImpact() == null && StandardAmmoType.R762_FMJ.onImpact() == null, "r762_sp/fmj unchanged for mob guns");
		check(problems, cases, XFactory762mm.r762_he.wear == 3F && XFactory762mm.r762_sp.wear == 1F && XFactory762mm.r762_du.wear == 1F, "r762 wear");
		check(problems, cases, XFactory762mm.r762_sp.casingEffect == XFactory762mm.CASING_R762 && XFactory762mm.r762_jhp.casingEffect == XFactory762mm.CASING_R762
				&& XFactory762mm.r762_ap.casingEffect == XFactory762mm.CASING_R762_STEEL && XFactory762mm.r762_he.casingEffect == XFactory762mm.CASING_R762_STEEL, "r762 casings");
		BeamBulletConfig las = XFactory762mm.energy_lacunae, over = XFactory762mm.energy_lacunae_overcharge, ir = XFactory762mm.energy_lacunae_ir;
		check(problems, cases, las.expires == 5 && over.expires == 5 && ir.expires == 5 && las.ammoReloadCount == 40 && over.ammoReloadCount == 40 && ir.ammoReloadCount == 40
				&& !las.renderRotations && las.dmgClass == BeamBulletConfig.DamageClass.LASER && over.dmgClass == BeamBulletConfig.DamageClass.LASER
				&& ir.dmgClass == BeamBulletConfig.DamageClass.FIRE && !las.doesPenetrate() && over.doesPenetrate() && !ir.doesPenetrate()
				&& las.laserColor == 0x601580 && las.spread() == 0F && las.damageMult() == 1F, "lacunae beam configs");

		/// Config values ///
		check(problems, cases, HbmConfig.carbineDamage() == 15F && HbmConfig.carbineBayonetDamage() == 15F && HbmConfig.minigunDamage() == 6F
				&& HbmConfig.lacunaeDamage() == 12F && HbmConfig.southStarDamage() == 30F && HbmConfig.southStarBayonetDamage() == 15F, "config damage defaults");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		var carbineRecipe = recipes.byKey(AtomicLegacy.id("gun_carbine")).map(h -> h.value()).orElse(null);
		var carbineInput = CraftingInput.of(3, 2, List.of(item("part_barrel_light_desh"), item("part_receiver_light_desh"), item("part_mechanism_gunmetal"),
				item("part_grip_wood"), ItemStack.EMPTY, item("part_stock_wood")));
		check(problems, cases, carbineRecipe instanceof ShapedRecipe s && s.matches(carbineInput, level) && s.assemble(carbineInput, server.registryAccess()).is(HbmItems.GUN_CARBINE.get()), "gun_carbine recipe BRM/G S");
		var minigunRecipe = recipes.byKey(AtomicLegacy.id("gun_minigun")).map(h -> h.value()).orElse(null);
		var minigunInput = CraftingInput.of(3, 3, List.of(item("part_barrel_light_tcalloy"), item("part_mechanism_weaponsteel"), item("part_grip_polymer"),
				item("part_barrel_light_cdalloy"), item("part_receiver_heavy_tcalloy"), item("motor_desh"),
				item("part_barrel_light_tcalloy"), item("part_grip_bakelite"), item("part_mechanism_weaponsteel")));
		check(problems, cases, minigunRecipe instanceof ShapedRecipe s && s.matches(minigunInput, level) && s.assemble(minigunInput, server.registryAccess()).is(HbmItems.GUN_MINIGUN.get()), "gun_minigun recipe BMG/BRE/BGM");
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_minigun_lacunae")).isEmpty() && recipes.byKey(AtomicLegacy.id("gun_mas36")).isEmpty(), "no crafting recipe for lacunae / mas36");
		check(problems, cases, pedestalLacunae(), "pedestal recipe gun_minigun_lacunae (FULL_MOON)");

		/// SCOPE mod ///
		ItemStack scope = new ItemStack(HbmItems.WEAPON_MOD_SCOPE.get());
		ItemStack scopedCarbine = new ItemStack(HbmItems.GUN_CARBINE.get());
		check(problems, cases, XWeaponModManager.isApplicable(scopedCarbine, scope, 0, true) && XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_MAS36.get()), scope, 0, true)
				&& XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_G3.get()), scope, 0, true)
				&& !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_MINIGUN.get()), scope, 0, true), "SCOPE applicable to carbine/mas36/g3 only");
		XWeaponModManager.install(scopedCarbine, 0, scope);
		GunConfig scopedCfg = ((ItemGunBaseNT) scopedCarbine.getItem()).getConfig(scopedCarbine, 0);
		check(problems, cases, XWeaponModManager.hasUpgrade(scopedCarbine, 0, XWeaponModManager.ID_SCOPE) && XFactory556mm.scope.equals(scopedCfg.getScopeTexture(scopedCarbine))
				&& scopedCfg.getHideCrosshair(scopedCarbine), "carbine with SCOPE uses scope_bolt");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		// Carbine: dura 3000, draw 10, inspect 31, dmg 15, delay 5, dry 15, reload 30 + end 15 (sequential), jam 60, mag 14
		ItemStack carbineGun = equip(player, HbmItems.GUN_CARBINE.get(), stack(StandardAmmoType.R762_JHP, 20));
		GunConfig cfg = ((ItemGunBaseNT) carbineGun.getItem()).getConfig(carbineGun, 0);
		Receiver rec = cfg.getReceivers(carbineGun)[0];
		IMagazine<?> mag = rec.getMagazine(carbineGun);
		check(problems, cases, cfg.getDurability(carbineGun) == 3000 && cfg.getDrawDuration(carbineGun) == 10 && cfg.getInspectDuration(carbineGun) == 31 && cfg.getReloadAnimSequential(carbineGun)
				&& rec.getBaseDamage(carbineGun) == 15F && rec.getDelayAfterFire(carbineGun) == 5 && rec.getDelayAfterDryFire(carbineGun) == 15 && !rec.getRefireOnHold(carbineGun)
				&& rec.getInnateSpread(carbineGun) == 0F && rec.getReloadBeginDuration(carbineGun) == 30 && rec.getReloadEndDuration(carbineGun) == 15
				&& rec.getJamDuration(carbineGun) == 60 && mag.getCapacity(carbineGun) == 14, "carbine values");
		press(player, carbineGun, GunKeybind.RELOAD);
		release(player, carbineGun, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(carbineGun, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(carbineGun, 0) == 30, "carbine reload start");
		tick(player, carbineGun, 31);
		check(problems, cases, mag.getAmount(carbineGun, null) == 14 && countAmmo(player, StandardAmmoType.R762_JHP) == 6 && ItemGunBaseNT.getState(carbineGun, 0) == GunState.DRAWING
				&& ItemGunBaseNT.getLastAnim(carbineGun, 0) == GunAnimation.RELOAD_END, "carbine reload -> RELOAD_END " + mag.getAmount(carbineGun, null) + " " + ItemGunBaseNT.getState(carbineGun, 0));
		tick(player, carbineGun, 16);
		check(problems, cases, ItemGunBaseNT.getState(carbineGun, 0) == GunState.IDLE, "carbine reload end 15 ticks");
		fire(player, carbineGun, GunKeybind.GUN_PRIMARY);
		release(player, carbineGun, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.R762_JHP && ItemGunBaseNT.getTimer(carbineGun, 0) == 5
				&& Math.abs(bullets.get(0).baseDamage() - 15F * 1.5F) < 0.001F && mag.getAmount(carbineGun, null) == 13 && mag.getCasing(carbineGun, null) == XFactory762mm.CASING_R762, "carbine shot");
		discard(level);

		// Minigun: belt fed, dura 50000, draw 20, inspect 20, dmg 6, delay 1, auto, dry 15, spread 0.01
		ItemStack mini = equip(player, HbmItems.GUN_MINIGUN.get(), stack(StandardAmmoType.R762_DU, 4), stack(StandardAmmoType.R762_SP, 10));
		cfg = ((ItemGunBaseNT) mini.getItem()).getConfig(mini, 0);
		rec = cfg.getReceivers(mini)[0];
		IMagazine<?> belt = rec.getMagazine(mini);
		check(problems, cases, belt instanceof MagazineBelt && cfg.getDurability(mini) == 50000 && cfg.getDrawDuration(mini) == 20 && cfg.getInspectDuration(mini) == 20
				&& rec.getBaseDamage(mini) == 6F && rec.getDelayAfterFire(mini) == 1 && rec.getDelayAfterDryFire(mini) == 15 && rec.getRefireOnHold(mini) && rec.getInnateSpread(mini) == 0.01F, "minigun values");
		check(problems, cases, belt.getAmount(mini, player.getInventory()) == 4 && belt.getType(mini, player.getInventory()) == XFactory762mm.r762_du, "minigun belt reads first inventory round");
		fire(player, mini, GunKeybind.GUN_PRIMARY);
		tick(player, mini, 7);
		release(player, mini, GunKeybind.GUN_PRIMARY);
		check(problems, cases, countAmmo(player, StandardAmmoType.R762_DU) == 0 && countAmmo(player, StandardAmmoType.R762_SP) == 6 && bullets(level).size() == 8
				&& bullets(level).stream().filter(b -> b.ammoType() == StandardAmmoType.R762_DU).count() == 4, "minigun auto fire 1/tick du=" + countAmmo(player, StandardAmmoType.R762_DU) + " sp=" + countAmmo(player, StandardAmmoType.R762_SP) + " bullets=" + bullets(level).size());
		discard(level);
		player.getInventory().clearContent();
		player.getInventory().setItem(0, mini);
		tick(player, mini, 20);
		fire(player, mini, GunKeybind.GUN_PRIMARY);
		release(player, mini, GunKeybind.GUN_PRIMARY);
		check(problems, cases, ItemGunBaseNT.getLastAnim(mini, 0) == GunAnimation.CYCLE_DRY && ItemGunBaseNT.getTimer(mini, 0) == 15, "minigun dry fire");

		// Lacunae: mag 200 capacitor rounds, 40 per item, reload 15, dmg 12, beams
		ItemStack lac = equip(player, HbmItems.GUN_MINIGUN_LACUNAE.get(), stack(StandardAmmoType.CAPACITOR, 15));
		cfg = ((ItemGunBaseNT) lac.getItem()).getConfig(lac, 0);
		rec = cfg.getReceivers(lac)[0];
		mag = rec.getMagazine(lac);
		check(problems, cases, cfg.getDurability(lac) == 50000 && rec.getBaseDamage(lac) == 12F && rec.getDelayAfterFire(lac) == 1 && rec.getRefireOnHold(lac)
				&& rec.getReloadBeginDuration(lac) == 15 && mag.getCapacity(lac) == 200 && cfg.getSmokeHandler(lac) == null, "lacunae values");
		press(player, lac, GunKeybind.RELOAD);
		release(player, lac, GunKeybind.RELOAD);
		tick(player, lac, 16);
		check(problems, cases, mag.getAmount(lac, null) == 200 && countAmmo(player, StandardAmmoType.CAPACITOR) == 10, "lacunae reload 5 capacitors -> 200, left " + countAmmo(player, StandardAmmoType.CAPACITOR));
		tick(player, lac, 20);
		fire(player, lac, GunKeybind.GUN_PRIMARY);
		release(player, lac, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletBeamEntity> beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactory762mm.energy_lacunae && mag.getAmount(lac, null) == 199 && bullets(level).isEmpty()
				&& Math.abs(beams.get(0).beamLength - 250D) < 0.01D, "lacunae fires one beam (250 m in the air) " + beams.size());
		if(!beams.isEmpty()) {
			LegacyBulletBeamEntity beam = beams.get(0);
			for(int i = 0; i < 6; i++) { beam.tickCount++; beam.tick(); }
			check(problems, cases, beam.isRemoved(), "beam dies after 5 ticks");
		}
		discard(level);

		/// Beam and bullet behaviour ///
		Pig pig = pig(level, player, 3.5D);
		Pig pig2 = pig(level, player, 6.5D);
		if(pig != null && pig2 != null) {
			float h1 = pig.getHealth(), h2 = pig2.getHealth();
			LegacyBulletBeamEntity beam = beam(player, XFactory762mm.energy_lacunae, 12F);
			check(problems, cases, Math.abs(h1 - pig.getHealth() - 12F) < 0.01F && h2 == pig2.getHealth() && pig.getLastHurtByMob() == player
					&& Math.abs(beam.beamLength - (pig.getZ() - 0.45D - 0.3D - beam.getZ())) < 0.05D, "laser beam hits nearest only dmg=" + (h1 - pig.getHealth()) + " len=" + beam.beamLength);

			h1 = pig.getHealth(); h2 = pig2.getHealth();
			beam = beam(player, XFactory762mm.energy_lacunae_overcharge, 12F);
			check(problems, cases, Math.abs(h1 - pig.getHealth() - 12F) < 0.01F && Math.abs(h2 - pig2.getHealth() - 12F) < 0.01F && Math.abs(beam.beamLength - 250D) < 0.01D, "overcharge beam penetrates");

			h1 = pig.getHealth();
			beam(player, XFactory762mm.energy_lacunae_ir, 12F);
			check(problems, cases, Math.abs(h1 - pig.getHealth() - 12F) < 0.01F && HbmLivingHazards.get(pig).getFire() >= 100, "IR beam damage + fire " + HbmLivingHazards.get(pig).getFire());
			pig2.discard();

			// r762_he: LAMBDA_TINY_EXPLODE -> tinyExplode(1.5), no direct hit
			pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			h1 = pig.getHealth();
			LegacyBulletEntity he = shoot(player, XFactory762mm.r762_he, 15F);
			float dealt = h1 - pig.getHealth();
			check(problems, cases, he.isRemoved() || he.isRetired(), "r762 HE bullet dies on impact");
			check(problems, cases, dealt > 0F && dealt <= 15F * 1.75F + 0.01F && dealt != 15F * 1.75F * 1.25F, "r762 HE explosion damage " + dealt);

			// r762_fmj direct hit: 15 * 0.8
			h1 = pig.getHealth();
			shoot(player, XFactory762mm.r762_fmj, 15F);
			check(problems, cases, Math.abs(h1 - pig.getHealth() - 12F) < 0.01F, "r762 FMJ hit " + (h1 - pig.getHealth()));
			pig.discard();
		}
		discard(level);

		// MAS-36: dura 5000, draw 20, inspect 31, dmg 30, delay 25, dry 25, reload(43) = begin/cycle 43, end 0 (sequential), jam 43, mag 7
		ItemStack mas = equip(player, HbmItems.GUN_MAS36.get(), stack(StandardAmmoType.R762_AP, 14));
		cfg = ((ItemGunBaseNT) mas.getItem()).getConfig(mas, 0);
		rec = cfg.getReceivers(mas)[0];
		mag = rec.getMagazine(mas);
		check(problems, cases, cfg.getDurability(mas) == 5000 && cfg.getDrawDuration(mas) == 20 && cfg.getInspectDuration(mas) == 31 && cfg.getReloadAnimSequential(mas)
				&& rec.getBaseDamage(mas) == 30F && rec.getDelayAfterFire(mas) == 25 && rec.getDelayAfterDryFire(mas) == 25 && rec.getReloadBeginDuration(mas) == 43
				&& rec.getReloadEndDuration(mas) == 0 && rec.getJamDuration(mas) == 43 && mag.getCapacity(mas) == 7
				&& rec.getProjectileOffset(mas).y == -0.0625 * 1.5, "mas36 values " + cfg.getDurability(mas) + "/" + cfg.getDrawDuration(mas) + "/" + cfg.getInspectDuration(mas) + "/" + rec.getBaseDamage(mas) + "/" + rec.getDelayAfterFire(mas) + "/" + rec.getDelayAfterDryFire(mas) + "/" + rec.getReloadBeginDuration(mas) + "/" + rec.getReloadEndDuration(mas) + "/" + rec.getJamDuration(mas) + "/" + mag.getCapacity(mas) + "/" + rec.getProjectileOffset(mas));
		press(player, mas, GunKeybind.RELOAD);
		release(player, mas, GunKeybind.RELOAD);
		tick(player, mas, 44);
		check(problems, cases, mag.getAmount(mas, null) == 7 && countAmmo(player, StandardAmmoType.R762_AP) == 7, "mas36 reload 7 AP");
		tick(player, mas, 44);
		fire(player, mas, GunKeybind.GUN_PRIMARY);
		release(player, mas, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).size() == 1 && bullets(level).get(0).ammoType() == StandardAmmoType.R762_AP && ItemGunBaseNT.getTimer(mas, 0) == 25
				&& Math.abs(bullets(level).get(0).baseDamage() - 30F * 1.25F) < 0.001F, "mas36 shot");
		discard(level);
		player.getInventory().clearContent();

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_C3_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static boolean round(StandardAmmoType t, float dmg, boolean pen, boolean falloff, float dt, float ap, float head) {
		return t.damageMultiplier() == dmg && t.penetrates() == pen && t.damageFalloffByPenetration() == falloff && t.armorThresholdNegation() == dt
				&& t.armorPiercingPercent() == ap && t.headshotMultiplier() == head && t.knockbackMultiplier() == 0.1F && t.projectileSpeed() == 10D
				&& t.projectileLifetime() == 30 && t.projectileGravity() == 0D && t.selfDamageDelay() == 2 && t.projectileSpread() == 0F
				&& t.projectileCount() == 1 && t.ricochetAngle() == 5F && t.maxRicochetCount() == 2;
	}

	@SuppressWarnings("unchecked")
	private static boolean pedestalLacunae() {
		try {
			var field = com.atomiclegacy.tileentity.LegacyPedestalRecipes.class.getDeclaredField("RECIPES");
			field.setAccessible(true);
			for(Object o : (List<Object>) field.get(null)) {
				Object[] input = (Object[]) o.getClass().getMethod("input").invoke(o);
				String s = o.getClass().getMethod("output").invoke(o) + java.util.Arrays.toString(input) + o.getClass().getMethod("condition").invoke(o);
				if(input.length != 9 || !input[4].toString().contains("itemId=gun_minigun,") || !input[0].toString().contains("count=0")) continue;
				if(s.contains("gun_minigun_lacunae") && s.contains("FULL_MOON") && s.contains("item_secret_selenium_steel, variant=null, tagPath=null, count=4")
						&& s.contains("itemId=gun_minigun, variant=null, tagPath=null, count=1") && s.contains("itemId=powder_magic, variant=null, tagPath=null, count=4")) return true;
			}
		} catch(ReflectiveOperationException e) {
			return false;
		}
		return false;
	}

	private static Pig pig(ServerLevel level, FakePlayer player, double z) {
		Pig pig = EntityType.PIG.create(level);
		if(pig == null) return null;
		pig.setNoAi(true);
		pig.setNoGravity(true);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
		pig.setHealth(1000F);
		pig.setPos(0.5D, player.getEyeY() - 0.45D, z);
		level.addFreshEntity(pig);
		return pig;
	}

	private static LegacyBulletBeamEntity beam(FakePlayer player, BeamBulletConfig config, float damage) {
		LegacyBulletBeamEntity beam = new LegacyBulletBeamEntity(player, config, damage, 0F, -0.25D, -0.0625D * 2.5D, 1D);
		player.level().addFreshEntity(beam);
		return beam;
	}

	private static LegacyBulletEntity shoot(FakePlayer player, com.atomiclegacy.items.weapon.sedna.BulletConfig config, float damage) {
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
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(ItemStack.EMPTY);
	}

	private static List<LegacyBulletEntity> bullets(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
	}

	private static List<LegacyBulletBeamEntity> beams(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA, b -> !b.isRemoved());
	}

	private static void discard(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
		for(LegacyBulletBeamEntity beam : level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA)) beam.discard();
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun batch C3: " + message);
	}
}
