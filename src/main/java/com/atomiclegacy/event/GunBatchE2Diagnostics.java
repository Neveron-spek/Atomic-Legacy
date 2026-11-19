package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasE2;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryEnergy;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch E2 (XFactoryEnergy: gun_tesla_cannon, gun_laser_pistol, gun_laser_pistol_pew_pew,
 * gun_laser_pistol_morning_glory, gun_lasrifle and the capacitor beams) against the pinned 1.7.10 values.
 */
final class GunBatchE2Diagnostics {

	private GunBatchE2Diagnostics() {
	}

	private static final AABB AREA = new AABB(-24, 280, -24, 24, 320, 30);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_TESLA_CANNON.get(), WeaponQuality.A_SIDE, StandardAmmoType.CAPACITOR, 15 },
				{ HbmItems.GUN_LASER_PISTOL.get(), WeaponQuality.A_SIDE, StandardAmmoType.CAPACITOR, 15 },
				{ HbmItems.GUN_LASER_PISTOL_PEW_PEW.get(), WeaponQuality.B_SIDE, StandardAmmoType.CAPACITOR_OVERCHARGE, 10 },
				{ HbmItems.GUN_LASER_PISTOL_MORNING_GLORY.get(), WeaponQuality.LEGENDARY, StandardAmmoType.CAPACITOR_OVERCHARGE, 20 },
				{ HbmItems.GUN_LASRIFLE.get(), WeaponQuality.A_SIDE, StandardAmmoType.CAPACITOR, 24 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == 1
					&& ItemGunBaseNT.secrets.contains(item) == (g[1] == WeaponQuality.LEGENDARY), name + " stack/quality/configs/secret");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, StandardAmmoItem.standardType(def) == g[2] && def.getCount() == (int) g[3], name + " default ammo " + def);
		}

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int minigun = indexOf(tab, HbmItems.GUN_MINIGUN.get()), tesla = indexOf(tab, HbmItems.GUN_TESLA_CANNON.get()), laser = indexOf(tab, HbmItems.GUN_LASER_PISTOL.get());
		int stg = indexOf(tab, HbmItems.GUN_STG77.get()), lasrifle = indexOf(tab, HbmItems.GUN_LASRIFLE.get()), dbarrel = indexOf(tab, HbmItems.GUN_DOUBLE_BARREL.get());
		check(problems, cases, minigun >= 0 && minigun < tesla && tesla < laser && laser < stg && stg < lasrifle && lasrifle < dbarrel,
				"weapon tab order (ModItems) " + minigun + "/" + tesla + "/" + laser + "/" + stg + "/" + lasrifle + "/" + dbarrel);
		check(problems, cases, indexOf(tab, HbmItems.GUN_LASER_PISTOL_PEW_PEW.get()) < 0 && indexOf(tab, HbmItems.GUN_LASER_PISTOL_MORNING_GLORY.get()) < 0,
				"B_SIDE / LEGENDARY laser pistols not in the tab");

		/// Beam configs ///
		check(problems, cases, beamCfg(XFactoryEnergy.energy_tesla, StandardAmmoType.CAPACITOR, DamageClass.ELECTRIC, 5, true, 1F, 0F, 0F, BeamBulletConfig.RENDER_LIGHTNING)
				&& XFactoryEnergy.energy_tesla.onImpactBeam != null, "energy_tesla values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_tesla_overcharge, StandardAmmoType.CAPACITOR_OVERCHARGE, DamageClass.ELECTRIC, 5, true, 1.5F, 0F, 0F, BeamBulletConfig.RENDER_LIGHTNING), "energy_tesla_overcharge values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_tesla_ir, StandardAmmoType.CAPACITOR_IR, DamageClass.ELECTRIC, 5, false, 0.8F, 0F, 0F, BeamBulletConfig.RENDER_LIGHTNING), "energy_tesla_ir values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_tesla_ir_sub, StandardAmmoType.CAPACITOR_IR, DamageClass.ELECTRIC, 3, true, 0.5F, 0F, 0F, BeamBulletConfig.RENDER_LIGHTNING_SUB)
				&& XFactoryEnergy.energy_tesla_ir_sub.wear == 3F && XFactoryEnergy.energy_tesla_ir_sub.onImpactBeam == BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT, "energy_tesla_ir_sub values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_las, StandardAmmoType.CAPACITOR, DamageClass.LASER, 5, false, 1F, 0F, 0F, 0) && XFactoryEnergy.energy_las.laserColor == 0x801515
				&& XFactoryEnergy.energy_las.onImpactBeam == BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT, "energy_las values (RENDER_LASER_RED)");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_las_overcharge, StandardAmmoType.CAPACITOR_OVERCHARGE, DamageClass.LASER, 5, true, 1F, 0F, 0F, 0) && XFactoryEnergy.energy_las_overcharge.laserColor == 0x801515, "energy_las_overcharge values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_las_ir, StandardAmmoType.CAPACITOR_IR, DamageClass.FIRE, 5, false, 1F, 0F, 0F, 0) && XFactoryEnergy.energy_las_ir.laserColor == 0x801515, "energy_las_ir values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_emerald, StandardAmmoType.CAPACITOR, DamageClass.LASER, 5, false, 1F, 0.5F, 50F, 0) && XFactoryEnergy.energy_emerald.laserColor == 0x158015, "energy_emerald values (RENDER_LASER_EMERALD)");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_emerald_overcharge, StandardAmmoType.CAPACITOR_OVERCHARGE, DamageClass.LASER, 5, true, 1F, 0.5F, 75F, 0), "energy_emerald_overcharge values");
		check(problems, cases, beamCfg(XFactoryEnergy.energy_emerald_ir, StandardAmmoType.CAPACITOR_IR, DamageClass.FIRE, 5, false, 1F, 0.5F, 50F, 0), "energy_emerald_ir values");
		BeamBulletConfig[] all = {XFactoryEnergy.energy_tesla, XFactoryEnergy.energy_tesla_overcharge, XFactoryEnergy.energy_tesla_ir, XFactoryEnergy.energy_tesla_ir_sub, XFactoryEnergy.energy_las,
				XFactoryEnergy.energy_las_overcharge, XFactoryEnergy.energy_las_ir, XFactoryEnergy.energy_emerald, XFactoryEnergy.energy_emerald_overcharge, XFactoryEnergy.energy_emerald_ir};
		boolean order = true;
		for(int i = 1; i < all.length; i++) order &= BulletConfig.configs.indexOf(all[i]) == BulletConfig.configs.indexOf(all[i - 1]) + 1;
		check(problems, cases, order, "beam configs registered consecutively in XFactoryEnergy order");

		/// Config values, damage type, sounds ///
		check(problems, cases, HbmConfig.teslaCannonDamage() == 35F && HbmConfig.laserPistolDamage() == 25F && HbmConfig.pewPewDamage() == 30F
				&& HbmConfig.morningGloryDamage() == 30F && HbmConfig.laserRifleDamage() == 50F, "config damage defaults");
		check(problems, cases, server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(HbmDamageTypes.SEDNA_ELECTRIC).isPresent(), "sedna_electric damage type");
		check(problems, cases, HbmSoundEvents.GUN_TESLA_FIRE.isBound() && HbmSoundEvents.GUN_LASER_PISTOL_FIRE.isBound() && HbmSoundEvents.GUN_LASER_RIFLE_FIRE.isBound(), "fire sounds registered");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		ItemStack[] parts = {item("part_barrel_heavy_tcalloy"), item("part_receiver_heavy_cdalloy"), item("part_mechanism_weaponsteel"), item("part_grip_bakelite")};
		checkRecipe(problems, cases, server, level, "gun_tesla_cannon", HbmItems.GUN_TESLA_CANNON.get(), 3, 3, List.of(item("coil_copper"), item("coil_copper"), item("coil_copper"),
				parts[0], parts[1], item("part_barrel_heavy_cdalloy"),
				parts[2], parts[3], item("circuit_advanced")));
		checkRecipe(problems, cases, server, level, "gun_laser_pistol", HbmItems.GUN_LASER_PISTOL.get(), 3, 2, List.of(item("crystal_redstone"), item("part_receiver_light_saturnite"), item("part_mechanism_saturnite"),
				item("part_grip_pc"), item("part_grip_pvc"), ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_laser_pistol_pew_pew", HbmItems.GUN_LASER_PISTOL_PEW_PEW.get(), 3, 3, List.of(ItemStack.EMPTY, item("part_mechanism_saturnite"), ItemStack.EMPTY,
				item("part_mechanism_saturnite"), new ItemStack(HbmItems.GUN_LASER_PISTOL.get()), item("part_mechanism_saturnite"),
				ItemStack.EMPTY, item("part_mechanism_saturnite"), ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_lasrifle", HbmItems.GUN_LASRIFLE.get(), 3, 3, List.of(item("crystal_redstone"), item("weapon_mod_special_scope"), item("circuit_bismoid"),
				item("part_barrel_light_bismuth_bronze"), item("part_receiver_light_arsenic_bronze"), item("part_stock_pc"),
				item("part_mechanism_saturnite"), item("part_grip_pvc"), ItemStack.EMPTY));
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_laser_pistol_morning_glory")).isEmpty(), "no crafting recipe for gun_laser_pistol_morning_glory");
		check(problems, cases, pedestalMorningGlory(), "pedestal recipe gun_laser_pistol_morning_glory");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		// Tesla cannon: dura 1000, draw 10, inspect 33, dmg 35, delay 20, hipfire 1.5, reload 44, jam 19, belt
		ItemStack tc = equip(player, HbmItems.GUN_TESLA_CANNON.get(), stack(StandardAmmoType.CAPACITOR, 15));
		GunConfig cfg = ((ItemGunBaseNT) tc.getItem()).getConfig(tc, 0);
		Receiver rec = cfg.getReceivers(tc)[0];
		IMagazine<?> mag = rec.getMagazine(tc);
		check(problems, cases, mag instanceof MagazineBelt && cfg.getDurability(tc) == 1000 && cfg.getDrawDuration(tc) == 10 && cfg.getInspectDuration(tc) == 33
				&& cfg.getCrosshair(tc) == Crosshair.CIRCLE && cfg.getSmokeHandler(tc) == null && cfg.getOrchestra(tc) == OrchestrasE2.ORCHESTRA_TESLA && cfg.getScopeTexture(tc) == null
				&& rec.getBaseDamage(tc) == 35F && rec.getDelayAfterFire(tc) == 20 && rec.getHipfireSpread(tc) == 1.5F && rec.getReloadBeginDuration(tc) == 44 && rec.getJamDuration(tc) == 19
				&& rec.getFireSound(tc) == HbmSoundEvents.GUN_TESLA_FIRE && rec.getProjectileOffset(tc).equals(new Vec3(0.75, 0, -0.375))
				&& rec.getProjectileOffsetScoped(tc).equals(new Vec3(0.75, 0, -0.25)) && rec.getRecoil(tc) == XFactoryEnergy.LAMBDA_RECOIL_ENERGY, "tesla cannon values");
		fire(player, tc, GunKeybind.GUN_PRIMARY);
		release(player, tc, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletBeamEntity> beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactoryEnergy.energy_tesla && Math.abs(beams.get(0).damage - 35F) < 0.001F
				&& countAmmo(player, StandardAmmoType.CAPACITOR) == 14 && ItemGunBaseNT.getTimer(tc, 0) == 20 && bullets(level).isEmpty(), "tesla cannon fires one lightning beam from the belt " + beams.size());
		discard(level);

		// Laser pistol: dura 500, inspect 26, dmg 25, delay 5, spread 1, hipfire 1, reload 45, jam 37, mag 30
		ItemStack lp = equip(player, HbmItems.GUN_LASER_PISTOL.get(), stack(StandardAmmoType.CAPACITOR, 40));
		cfg = ((ItemGunBaseNT) lp.getItem()).getConfig(lp, 0);
		rec = cfg.getReceivers(lp)[0];
		mag = rec.getMagazine(lp);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(lp) == 500 && cfg.getDrawDuration(lp) == 10 && cfg.getInspectDuration(lp) == 26 && cfg.getCrosshair(lp) == Crosshair.CIRCLE
				&& rec.getBaseDamage(lp) == 25F && rec.getDelayAfterFire(lp) == 5 && rec.getInnateSpread(lp) == 1F && rec.getHipfireSpread(lp) == 1F && rec.getReloadBeginDuration(lp) == 45
				&& rec.getJamDuration(lp) == 37 && mag.getCapacity(lp) == 30 && rec.getFirePitch(lp) == 1F && rec.getProjectileOffset(lp).equals(new Vec3(0.75, -0.0625 * 1.5, -0.1875))
				&& cfg.getOrchestra(lp) == OrchestrasE2.ORCHESTRA_LASER_PISTOL, "laser pistol values");
		press(player, lp, GunKeybind.RELOAD);
		release(player, lp, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(lp, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(lp, 0) == 45, "laser pistol reload start");
		tick(player, lp, 46);
		check(problems, cases, mag.getAmount(lp, null) == 30 && countAmmo(player, StandardAmmoType.CAPACITOR) == 10, "laser pistol reload 30 capacitors, left " + countAmmo(player, StandardAmmoType.CAPACITOR));
		tick(player, lp, 2);
		fire(player, lp, GunKeybind.GUN_PRIMARY);
		release(player, lp, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactoryEnergy.energy_las && mag.getAmount(lp, null) == 29 && ItemGunBaseNT.getTimer(lp, 0) == 5
				&& Math.abs(beams.get(0).beamLength - 250D) < 0.01D, "laser pistol fires one red laser " + beams.size());
		if(!beams.isEmpty()) {
			LegacyBulletBeamEntity beam = beams.get(0);
			for(int i = 0; i < 6; i++) { beam.tickCount++; beam.tick(); }
			check(problems, cases, beam.isRemoved(), "laser beam dies after 5 ticks");
		}
		check(problems, cases, cfg.getAnims(lp).apply(lp, GunAnimation.RELOAD) != null && cfg.getAnims(lp).apply(lp, GunAnimation.JAMMED) != null
				&& cfg.getAnims(lp).apply(lp, GunAnimation.INSPECT) != null && cfg.getAnims(lp).apply(lp, GunAnimation.CYCLE_DRY) == null, "laser pistol animations");
		discard(level);

		// Pew Pew: 5 rounds per cycle, delay 10, spread 0.25, mag 10, pitch 0.8
		ItemStack pp = equip(player, HbmItems.GUN_LASER_PISTOL_PEW_PEW.get(), stack(StandardAmmoType.CAPACITOR_OVERCHARGE, 20));
		cfg = ((ItemGunBaseNT) pp.getItem()).getConfig(pp, 0);
		rec = cfg.getReceivers(pp)[0];
		mag = rec.getMagazine(pp);
		check(problems, cases, cfg.getDurability(pp) == 500 && rec.getBaseDamage(pp) == 30F && rec.getRoundsPerCycle(pp) == 5 && rec.getDelayAfterFire(pp) == 10 && rec.getInnateSpread(pp) == 0.25F
				&& rec.getHipfireSpread(pp) == 1F && mag.getCapacity(pp) == 10 && rec.getFirePitch(pp) == 0.8F && rec.getReloadBeginDuration(pp) == 45 && rec.getJamDuration(pp) == 37, "pew pew values");
		press(player, pp, GunKeybind.RELOAD);
		release(player, pp, GunKeybind.RELOAD);
		tick(player, pp, 48);
		fire(player, pp, GunKeybind.GUN_PRIMARY);
		release(player, pp, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 5 && beams.get(0).config == XFactoryEnergy.energy_las_overcharge && mag.getAmount(pp, null) == 5 && countAmmo(player, StandardAmmoType.CAPACITOR_OVERCHARGE) == 10
				&& ItemGunBaseNT.getTimer(pp, 0) == 10, "pew pew fires 5 beams per click " + beams.size() + " " + mag.getAmount(pp, null));
		discard(level);

		// Morning Glory: dura 1500, delay 7, spread 0, hipfire 0.5, mag 20, emerald beams, pitch 1.1
		ItemStack mg = equip(player, HbmItems.GUN_LASER_PISTOL_MORNING_GLORY.get(), stack(StandardAmmoType.CAPACITOR, 25));
		cfg = ((ItemGunBaseNT) mg.getItem()).getConfig(mg, 0);
		rec = cfg.getReceivers(mg)[0];
		mag = rec.getMagazine(mg);
		check(problems, cases, cfg.getDurability(mg) == 1500 && rec.getBaseDamage(mg) == 30F && rec.getDelayAfterFire(mg) == 7 && rec.getInnateSpread(mg) == 0F && rec.getHipfireSpread(mg) == 0.5F
				&& mag.getCapacity(mg) == 20 && rec.getFirePitch(mg) == 1.1F, "morning glory values");
		press(player, mg, GunKeybind.RELOAD);
		release(player, mg, GunKeybind.RELOAD);
		tick(player, mg, 48);
		fire(player, mg, GunKeybind.GUN_PRIMARY);
		release(player, mg, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactoryEnergy.energy_emerald && mag.getAmount(mg, null) == 19, "morning glory fires an emerald laser");
		discard(level);

		// Laser rifle: dura 2000, scope_amat, dmg 50, delay 8, hipfire 1, reload 44, jam 36, mag 24
		ItemStack lr = equip(player, HbmItems.GUN_LASRIFLE.get(), stack(StandardAmmoType.CAPACITOR_IR, 24));
		cfg = ((ItemGunBaseNT) lr.getItem()).getConfig(lr, 0);
		rec = cfg.getReceivers(lr)[0];
		mag = rec.getMagazine(lr);
		check(problems, cases, cfg.getDurability(lr) == 2000 && cfg.getInspectDuration(lr) == 26 && AtomicLegacy.id("textures/misc/scope_amat.png").equals(cfg.getScopeTexture(lr))
				&& rec.getBaseDamage(lr) == 50F && rec.getDelayAfterFire(lr) == 8 && rec.getHipfireSpread(lr) == 1F && rec.getReloadBeginDuration(lr) == 44 && rec.getJamDuration(lr) == 36
				&& mag.getCapacity(lr) == 24 && rec.getFireSound(lr) == HbmSoundEvents.GUN_LASER_RIFLE_FIRE && cfg.getOrchestra(lr) == OrchestrasE2.ORCHESTRA_LASRIFLE, "laser rifle values");
		press(player, lr, GunKeybind.RELOAD);
		release(player, lr, GunKeybind.RELOAD);
		tick(player, lr, 46);
		fire(player, lr, GunKeybind.GUN_PRIMARY);
		release(player, lr, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == XFactoryEnergy.energy_las_ir && mag.getAmount(lr, null) == 23 && Math.abs(beams.get(0).damage - 50F) < 0.001F,
				"laser rifle fires an IR laser " + beams.size());
		discard(level);
		player.getInventory().clearContent();

		/// Beam behaviour ///
		Pig pig = pig(level, player, 3.5D);
		Pig pig2 = pig(level, player, 6.5D);
		if(pig != null && pig2 != null) {
			// laser vs armor 10: 25 * (25 - 10) / 25 = 15; emerald (armor piercing 0.5): 25 * (25 - 5) / 25 = 20
			pig.getAttribute(Attributes.ARMOR).setBaseValue(10D);
			float h1 = pig.getHealth(), h2 = pig2.getHealth();
			beam(player, XFactoryEnergy.energy_las, 25F);
			float d1 = h1 - pig.getHealth();
			h1 = pig.getHealth();
			beam(player, XFactoryEnergy.energy_emerald, 25F);
			float d2 = h1 - pig.getHealth();
			check(problems, cases, Math.abs(d1 - 15F) < 0.01F && Math.abs(d2 - 20F) < 0.01F && h2 == pig2.getHealth(), "laser / emerald armor piercing dmg=" + d1 + "/" + d2);
			pig.getAttribute(Attributes.ARMOR).setBaseValue(0D);

			// ELECTRIC: x5 against an ArmorFSBPowered chestplate, armor applied like LASER
			h1 = pig.getHealth();
			com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.attackEntityFromNT(pig, BeamBulletConfig.getDamage(player, player, DamageClass.ELECTRIC), DamageClass.ELECTRIC, 2F, 0D, 0F, 0F);
			float e1 = h1 - pig.getHealth();
			pig.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HbmItems.T51_PLATE.get()));
			h1 = pig.getHealth();
			com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.attackEntityFromNT(pig, BeamBulletConfig.getDamage(player, player, DamageClass.ELECTRIC), DamageClass.ELECTRIC, 2F, 0D, 0F, 0F);
			float e2 = h1 - pig.getHealth();
			pig.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			check(problems, cases, Math.abs(e1 - 2F) < 0.01F && Math.abs(e2 - 10F) < 0.01F && pig.getLastDamageSource() != null && pig.getLastDamageSource().is(HbmDamageTypes.SEDNA_ELECTRIC),
					"electric damage x5 vs powered chestplate " + e1 + "/" + e2);

			// tesla: penetrating lightning, ExplosionVNT(2) with 35 fixed damage at each hit, slowness / mining fatigue X
			h1 = pig.getHealth(); h2 = pig2.getHealth();
			LegacyBulletBeamEntity beam = beam(player, XFactoryEnergy.energy_tesla, 35F);
			d1 = h1 - pig.getHealth();
			d2 = h2 - pig2.getHealth();
			var slow = pig.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
			var fatigue = pig2.getEffect(MobEffects.DIG_SLOWDOWN);
			check(problems, cases, d1 > 0F && d1 <= 70F + 0.01F && d2 > 0F && slow != null && slow.getAmplifier() == 9 && slow.getDuration() == 60 && fatigue != null && fatigue.getAmplifier() == 9
					&& Math.abs(beam.beamLength - 250D) < 0.01D && pig.getLastDamageSource() != null && pig.getLastDamageSource().is(HbmDamageTypes.SEDNA_ELECTRIC),
					"tesla lightning penetrates and explodes dmg=" + d1 + "/" + d2 + " len=" + beam.beamLength);
			pig.removeAllEffects();
			pig2.removeAllEffects();
			discard(level);

			// tesla IR: no penetration, then one sub beam (0.8 * 35 * 0.5 = 14) to every other living entity within 20 blocks
			h2 = pig2.getHealth();
			beam = beam(player, XFactoryEnergy.energy_tesla_ir, 35F);
			List<LegacyBulletBeamEntity> subs = level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA, b -> b.config == XFactoryEnergy.energy_tesla_ir_sub);
			d2 = h2 - pig2.getHealth();
			check(problems, cases, subs.size() == 1 && Math.abs(subs.get(0).damage - 14F) < 0.001F && d2 >= 14F - 0.01F && Math.abs(beam.damage - 28F) < 0.001F
					&& beam.beamLength < 5D, "tesla IR splits to the second pig subs=" + subs.size() + " dmg=" + d2 + " len=" + beam.beamLength);
			if(!subs.isEmpty()) {
				LegacyBulletBeamEntity sub = subs.get(0);
				for(int i = 0; i < 4; i++) { sub.tickCount++; sub.tick(); }
				check(problems, cases, sub.isRemoved(), "sub beam dies after 3 ticks");
			}
			pig.removeAllEffects();
			discard(level);

			// laser IR: damage + HbmLivingProps.fire >= 100
			h1 = pig.getHealth();
			beam(player, XFactoryEnergy.energy_las_ir, 50F);
			check(problems, cases, Math.abs(h1 - pig.getHealth() - 50F) < 0.01F && HbmLivingHazards.get(pig).getFire() >= 100, "IR laser damage + fire " + HbmLivingHazards.get(pig).getFire());
			pig.discard();
			pig2.discard();
		}
		discard(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_E2_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static boolean beamCfg(BeamBulletConfig c, StandardAmmoType ammo, DamageClass clazz, int life, boolean pen, float dmg, float ap, float tn, int renderer) {
		return c.ammo == ammo && c.dmgClass == clazz && c.expires == life && c.doesPenetrate() == pen && c.damageMult() == dmg && c.armorPiercing() == ap
				&& c.thresholdNegation() == tn && c.beamRenderer == renderer && !c.renderRotations && c.impactsEntities && !c.spectral && c.ammoReloadCount == 1
				&& c.casingEffect == -1 && c.spread() == 0F && c.ammo.knockbackMultiplier() == 0.1F && c.projectilesMin() == 1 && c.projectilesMax() == 1
				&& (c.beamRenderer != 0 || c.laserColor >= 0) && (c == XFactoryEnergy.energy_tesla_ir_sub || c.wear == 1F);
	}

	@SuppressWarnings("unchecked")
	private static boolean pedestalMorningGlory() {
		try {
			var field = com.atomiclegacy.tileentity.LegacyPedestalRecipes.class.getDeclaredField("RECIPES");
			field.setAccessible(true);
			for(Object o : (List<Object>) field.get(null)) {
				Object[] input = (Object[]) o.getClass().getMethod("input").invoke(o);
				String s = o.getClass().getMethod("output").invoke(o) + java.util.Arrays.toString(input) + o.getClass().getMethod("condition").invoke(o) + " set=" + o.getClass().getMethod("set").invoke(o);
				if(input.length != 9 || !input[4].toString().contains("itemId=gun_laser_pistol,")) continue;
				if(s.contains("gun_laser_pistol_morning_glory") && s.endsWith("NONE set=0") && input[0].toString().contains("count=0") && input[2].toString().contains("count=0")
						&& input[6].toString().contains("count=0") && input[8].toString().contains("count=0")
						&& input[1].toString().contains("itemId=morning_glory, variant=null, tagPath=null, count=1")
						&& input[3].toString().contains("itemId=item_secret_selenium_steel, variant=null, tagPath=null, count=2")
						&& input[5].toString().contains("itemId=item_secret_selenium_steel, variant=null, tagPath=null, count=2")
						&& input[7].toString().contains("itemId=null, variant=null, tagPath=gems/emerald, count=16")) return true;
			}
		} catch(ReflectiveOperationException e) {
			return false;
		}
		return false;
	}

	private static void checkRecipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, Item result, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		check(problems, cases, recipe instanceof ShapedRecipe s && s.matches(input, level) && s.assemble(input, server.registryAccess()).is(result), id + " recipe");
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
		if(!ok) problems.add("gun batch E2: " + message);
	}
}
