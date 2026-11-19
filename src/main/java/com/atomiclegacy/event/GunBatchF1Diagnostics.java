package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyChemicalEntity;
import com.atomiclegacy.entity.LegacyChemicalEntity.ChemicalStyle;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
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
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF1;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm.CloneBulletConfig;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryFlamer;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChemthrower;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFluid;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Dedicated-server checks for weapons batch F1 (XFactoryFlamer: gun_flamer, gun_flamer_topaz, gun_flamer_daybreaker,
 * gun_chemthrower, the 14 flame rounds and EntityChemical) against the pinned 1.7.10 values.
 */
final class GunBatchF1Diagnostics {

	private GunBatchF1Diagnostics() {
	}

	private static final AABB AREA = new AABB(-24, 280, -24, 24, 320, 30);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_FLAMER.get(), WeaponQuality.A_SIDE, StandardAmmoType.FLAME_DIESEL, 1 },
				{ HbmItems.GUN_FLAMER_TOPAZ.get(), WeaponQuality.B_SIDE, StandardAmmoType.FLAME_DIESEL, 1 },
				{ HbmItems.GUN_FLAMER_DAYBREAKER.get(), WeaponQuality.LEGENDARY, StandardAmmoType.FLAME_DIESEL, 1 },
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
		Item chem = HbmItems.GUN_CHEMTHROWER.get();
		check(problems, cases, chem instanceof ItemGunChemthrower ct && ct.quality == WeaponQuality.A_SIDE && ct.getConfigCount() == 1 && ct.getDefaultAmmo().isEmpty()
				&& new ItemStack(chem).getMaxStackSize() == 1 && !ItemGunBaseNT.secrets.contains(chem), "gun_chemthrower registration");

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int congo = indexOf(tab, HbmItems.GUN_CONGOLAKE.get()), flamer = indexOf(tab, HbmItems.GUN_FLAMER.get()), uzi = indexOf(tab, HbmItems.GUN_UZI.get());
		int mk108 = indexOf(tab, HbmItems.GUN_MK108.get()), chemT = indexOf(tab, chem), amat = indexOf(tab, HbmItems.GUN_AMAT.get());
		check(problems, cases, congo >= 0 && congo < flamer && flamer < uzi && mk108 >= 0 && mk108 < chemT && chemT < amat,
				"weapon tab order (ModItems) " + congo + "/" + flamer + "/" + uzi + " " + mk108 + "/" + chemT + "/" + amat);
		check(problems, cases, indexOf(tab, HbmItems.GUN_FLAMER_TOPAZ.get()) < 0 && indexOf(tab, HbmItems.GUN_FLAMER_DAYBREAKER.get()) < 0, "B_SIDE / LEGENDARY flamers not in the tab");

		/// Config values, sounds, damage type ///
		check(problems, cases, HbmConfig.flamerDamage() == 1F && HbmConfig.topazDamage() == 1.5F && HbmConfig.daybreakerDamage() == 25F, "config damage defaults");
		check(problems, cases, HbmSoundEvents.GUN_FLAMER_LOOP.isBound() && HbmSoundEvents.GUN_VALVE.isBound() && HbmSoundEvents.GUN_POWDER_FIRE.isBound()
				&& HbmSoundEvents.GUN_LATCH_OPEN.isBound() && HbmSoundEvents.GUN_CANISTER_INSERT.isBound() && HbmSoundEvents.GUN_REVOLVER_CLOSE.isBound(), "flamer sounds registered");
		check(problems, cases, server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(HbmDamageTypes.FLAMETHROWER).isPresent(), "flamethrower damage type");

		/// Flame rounds (BulletConfig) ///
		// type, life, vel, grav, spread, projectiles, onImpact, onRicochet
		Object[][] rounds = {
				{ StandardAmmoType.FLAME_DIESEL, 100, 1D, 0.02D, 0F, 1, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_DIESEL },
				{ StandardAmmoType.FLAME_GAS, 10, 1D, 0D, 0.05F, 1, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_GAS },
				{ StandardAmmoType.FLAME_NAPALM, 200, 1D, 0.02D, 0F, 1, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_NAPALM },
				{ StandardAmmoType.FLAME_BALEFIRE, 200, 1D, 0.02D, 0F, 1, XFactoryFlamer.LAMBDA_IGNITE_BALEFIRE, XFactoryFlamer.LAMBDA_LINGER_BALEFIRE },
				{ StandardAmmoType.FLAME_NOGRAV, 100, 1D, 0D, 0F, 1, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_DIESEL },
				{ StandardAmmoType.FLAME_NOGRAV_BF, 100, 1D, 0D, 0F, 1, XFactoryFlamer.LAMBDA_IGNITE_BALEFIRE, XFactoryFlamer.LAMBDA_LINGER_BALEFIRE },
				{ StandardAmmoType.FLAME_TOPAZ_DIESEL, 60, 1D, 0D, 0.05F, 2, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_DIESEL },
				{ StandardAmmoType.FLAME_TOPAZ_GAS, 10, 1D, 0D, 0.05F, 2, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_GAS },
				{ StandardAmmoType.FLAME_TOPAZ_NAPALM, 60, 1D, 0D, 0.05F, 2, XFactoryFlamer.LAMBDA_IGNITE_FIRE, XFactoryFlamer.LAMBDA_LINGER_NAPALM },
				{ StandardAmmoType.FLAME_TOPAZ_BALEFIRE, 60, 1D, 0D, 0.05F, 2, XFactoryFlamer.LAMBDA_IGNITE_BALEFIRE, XFactoryFlamer.LAMBDA_LINGER_BALEFIRE },
				{ StandardAmmoType.FLAME_DAYBREAKER_DIESEL, 200, 2D, 0.035D, 0F, 1, null, XFactoryFlamer.LAMBDA_LINGER_DIESEL },
				{ StandardAmmoType.FLAME_DAYBREAKER_GAS, 200, 2D, 0.035D, 0.05F, 1, null, XFactoryFlamer.LAMBDA_LINGER_GAS },
				{ StandardAmmoType.FLAME_DAYBREAKER_NAPALM, 200, 2D, 0.035D, 0F, 1, null, XFactoryFlamer.LAMBDA_LINGER_NAPALM },
				{ StandardAmmoType.FLAME_DAYBREAKER_BALEFIRE, 200, 2D, 0.035D, 0F, 1, null, XFactoryFlamer.LAMBDA_LINGER_BALEFIRE },
		};
		for(Object[] r : rounds) {
			StandardAmmoType t = (StandardAmmoType) r[0];
			boolean impact = r[6] == null ? t.onImpact() != null && t.onImpact() != XFactoryFlamer.LAMBDA_IGNITE_FIRE && t.onImpact() != XFactoryFlamer.LAMBDA_IGNITE_BALEFIRE : t.onImpact() == r[6];
			check(problems, cases, t.family() == AmmoFamily.FLAME && t.projectileLifetime() == (int) r[1] && t.projectileSpeed() == (double) r[2] && t.projectileGravity() == (double) r[3]
					&& t.projectileSpread() == (float) r[4] && t.projectileCount() == (int) r[5] && t.projectileCountMax() == (int) r[5] && t.selfDamageDelay() == 20
					&& t.knockbackMultiplier() == 0F && impact && t.onRicochet() == (BulletImpact) r[7] && t.onUpdate() == null && t.onEntityHit() == null
					&& !t.spectral() && !t.blackPowder() && t.headshotMultiplier() == 1.25F && t.damageMultiplier() == 1F && !t.penetrates(), t.id() + " round values");
		}
		BulletConfig[] all = { XFactoryFlamer.flame_diesel, XFactoryFlamer.flame_gas, XFactoryFlamer.flame_napalm, XFactoryFlamer.flame_balefire, XFactoryFlamer.flame_nograv, XFactoryFlamer.flame_nograv_bf,
				XFactoryFlamer.flame_topaz_diesel, XFactoryFlamer.flame_topaz_gas, XFactoryFlamer.flame_topaz_napalm, XFactoryFlamer.flame_topaz_balefire,
				XFactoryFlamer.flame_daybreaker_diesel, XFactoryFlamer.flame_daybreaker_gas, XFactoryFlamer.flame_daybreaker_napalm, XFactoryFlamer.flame_daybreaker_balefire };
		StandardAmmoType[] items = { StandardAmmoType.FLAME_DIESEL, StandardAmmoType.FLAME_GAS, StandardAmmoType.FLAME_NAPALM, StandardAmmoType.FLAME_BALEFIRE, StandardAmmoType.FLAME_DIESEL, StandardAmmoType.FLAME_BALEFIRE,
				StandardAmmoType.FLAME_DIESEL, StandardAmmoType.FLAME_GAS, StandardAmmoType.FLAME_NAPALM, StandardAmmoType.FLAME_BALEFIRE,
				StandardAmmoType.FLAME_DIESEL, StandardAmmoType.FLAME_GAS, StandardAmmoType.FLAME_NAPALM, StandardAmmoType.FLAME_BALEFIRE };
		boolean cfgOk = true, order = true;
		for(int i = 0; i < all.length; i++) {
			BulletConfig c = all[i];
			cfgOk &= c.ammo == rounds[i][0] && c.ammoReloadCount == 500 && c.wear == 1F && c.casingEffect == -1 && c.matches(stack(items[i], 1)) && StandardAmmoItem.standardType(c.toStack()) == items[i]
					&& (i < 4 ? !(c instanceof CloneBulletConfig) : c instanceof CloneBulletConfig cl && cl.item == items[i]);
			if(i > 0) order &= BulletConfig.configs.indexOf(c) == BulletConfig.configs.indexOf(all[i - 1]) + 1;
		}
		check(problems, cases, cfgOk, "flame BulletConfig wrappers (reload 500, item mapping)");
		check(problems, cases, order, "flame configs registered consecutively in XFactoryFlamer order");
		check(problems, cases, XFactoryFlamer.isBalefireFlame(StandardAmmoType.FLAME_BALEFIRE) && XFactoryFlamer.isBalefireFlame(StandardAmmoType.FLAME_NOGRAV_BF)
				&& XFactoryFlamer.isBalefireFlame(StandardAmmoType.FLAME_TOPAZ_BALEFIRE) && XFactoryFlamer.isBalefireFlame(StandardAmmoType.FLAME_DAYBREAKER_BALEFIRE)
				&& !XFactoryFlamer.isBalefireFlame(StandardAmmoType.FLAME_DIESEL) && !XFactoryFlamer.isBalefireFlame(StandardAmmoType.FLAME_NAPALM), "balefire flame rounds");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		checkRecipe(problems, cases, server, level, "gun_flamer", HbmItems.GUN_FLAMER.get(), 3, 3, List.of(ItemStack.EMPTY, item("part_mechanism_gunmetal"), item("part_grip_dura_steel"),
				item("part_barrel_heavy_dura_steel"), item("part_barrel_heavy_dura_steel"), item("part_receiver_heavy_dura_steel"),
				ItemStack.EMPTY, item("part_grip_dura_steel"), item("part_mechanism_gunmetal")));
		checkRecipe(problems, cases, server, level, "gun_flamer_topaz", HbmItems.GUN_FLAMER_TOPAZ.get(), 3, 3, List.of(ItemStack.EMPTY, item("part_mechanism_weaponsteel"), ItemStack.EMPTY,
				item("part_mechanism_weaponsteel"), new ItemStack(HbmItems.GUN_FLAMER.get()), item("part_mechanism_weaponsteel"),
				ItemStack.EMPTY, item("part_mechanism_weaponsteel"), ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_chemthrower", chem, 3, 2, List.of(item("part_mechanism_weaponsteel"), item("pipe_rubber"), item("wrench"),
				item("part_barrel_heavy_weaponsteel"), item("shell_weaponsteel"), item("shell_weaponsteel")));
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_flamer_daybreaker")).isEmpty(), "no crafting recipe for gun_flamer_daybreaker");
		check(problems, cases, pedestalDaybreaker(), "pedestal recipe gun_flamer_daybreaker (SUN)");

		/// Gun values ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		// Flamethrower: dura 20000, draw 10, inspect 17, L_CIRCLE, dmg 1, hipfire 0, delay 1, auto, reload 90, jam 17, mag 300, no fire sound
		ItemStack fl = equip(player, HbmItems.GUN_FLAMER.get(), stack(StandardAmmoType.FLAME_DIESEL, 2));
		GunConfig cfg = ((ItemGunBaseNT) fl.getItem()).getConfig(fl, 0);
		Receiver rec = cfg.getReceivers(fl)[0];
		IMagazine<?> mag = rec.getMagazine(fl);
		check(problems, cases, mag instanceof MagazineFullReload && mag.getCapacity(fl) == 300 && cfg.getDurability(fl) == 20_000 && cfg.getDrawDuration(fl) == 10 && cfg.getInspectDuration(fl) == 17
				&& cfg.getCrosshair(fl) == Crosshair.L_CIRCLE && cfg.getHideCrosshair(fl) && cfg.getSmokeHandler(fl) == null && cfg.getOrchestra(fl) == OrchestrasF1.ORCHESTRA_FLAMER && cfg.getScopeTexture(fl) == null
				&& rec.getBaseDamage(fl) == 1F && rec.getHipfireSpread(fl) == 0F && rec.getInnateSpread(fl) == 0F && rec.getDelayAfterFire(fl) == 1 && rec.getDelayAfterDryFire(fl) == 1 && rec.getRefireOnHold(fl)
				&& rec.getReloadBeginDuration(fl) == 90 && rec.getReloadCycleDuration(fl) == 90 && rec.getReloadEndDuration(fl) == 0 && rec.getJamDuration(fl) == 17 && rec.getFireSound(fl) == null
				&& rec.getProjectileOffset(fl).equals(new Vec3(0.75, -0.0625, -0.25D)) && rec.getProjectileOffsetScoped(fl).equals(new Vec3(0.75, -0.0625, 0))
				&& cfg.getPressReload(fl) == Lego.LAMBDA_STANDARD_RELOAD && cfg.getPressTertiary(fl) == Lego.LAMBDA_TOGGLE_AIM && rec.getCanFire(fl) == Lego.LAMBDA_STANDARD_CAN_FIRE && rec.getOnFire(fl) == Lego.LAMBDA_STANDARD_FIRE, "flamethrower values");
		check(problems, cases, cfg.getAnims(fl) == XFactoryFlamer.LAMBDA_FLAMER_ANIMS && cfg.getAnims(fl).apply(fl, GunAnimation.EQUIP) != null && cfg.getAnims(fl).apply(fl, GunAnimation.INSPECT) != null
				&& cfg.getAnims(fl).apply(fl, GunAnimation.JAMMED) != null && cfg.getAnims(fl).apply(fl, GunAnimation.CYCLE) == null, "flamethrower animations (Reload is the client JSON)");
		var mfr = (MagazineFullReload) mag;
		check(problems, cases, mfr.acceptedBullets.size() == 4 && mfr.acceptedBullets.get(0) == XFactoryFlamer.flame_diesel && mfr.acceptedBullets.get(1) == XFactoryFlamer.flame_gas
				&& mfr.acceptedBullets.get(2) == XFactoryFlamer.flame_napalm && mfr.acceptedBullets.get(3) == XFactoryFlamer.flame_balefire, "flamethrower magazine rounds");

		// reload: 90 ticks, one diesel canister (reload count 500) fills the 300 mag
		press(player, fl, GunKeybind.RELOAD);
		release(player, fl, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(fl, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(fl, 0) == 90, "flamethrower reload start " + ItemGunBaseNT.getTimer(fl, 0));
		tick(player, fl, 91);
		check(problems, cases, mag.getAmount(fl, null) == 300 && countAmmo(player, StandardAmmoType.FLAME_DIESEL) == 1 && mag.getType(fl, null) == XFactoryFlamer.flame_diesel,
				"flamethrower reload 300 from one canister, left " + countAmmo(player, StandardAmmoType.FLAME_DIESEL) + " loaded " + mag.getAmount(fl, null));
		tick(player, fl, 2);
		// fire: one FLAME_DIESEL bullet per shot, delay 1, wear 1, auto refire while held
		fire(player, fl, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.FLAME_DIESEL && !bullets.get(0).balefire() && Math.abs(bullets.get(0).baseDamage() - 1F) < 0.001F
				&& mag.getAmount(fl, null) == 299 && ItemGunBaseNT.getTimer(fl, 0) == 1 && ItemGunBaseNT.getWear(fl, 0) == 1F && ItemGunBaseNT.getState(fl, 0) == GunState.COOLDOWN, "flamethrower fires one flame " + bullets.size());
		tick(player, fl, 20);
		release(player, fl, GunKeybind.GUN_PRIMARY);
		int shots = 300 - mag.getAmount(fl, null);
		check(problems, cases, shots >= 10 && bullets(level).size() == shots && ItemGunBaseNT.getWear(fl, 0) == shots, "flamethrower auto fire (delay 1) shots=" + shots);
		discard(level);

		// Mister Topaz: dmg 1.5, mag 500 of the topaz clones
		ItemStack tp = equip(player, HbmItems.GUN_FLAMER_TOPAZ.get(), stack(StandardAmmoType.FLAME_GAS, 2));
		cfg = ((ItemGunBaseNT) tp.getItem()).getConfig(tp, 0);
		rec = cfg.getReceivers(tp)[0];
		mag = rec.getMagazine(tp);
		mfr = (MagazineFullReload) mag;
		check(problems, cases, mag.getCapacity(tp) == 500 && cfg.getDurability(tp) == 20_000 && rec.getBaseDamage(tp) == 1.5F && rec.getDelayAfterFire(tp) == 1 && rec.getReloadBeginDuration(tp) == 90 && rec.getJamDuration(tp) == 17
				&& rec.getFireSound(tp) == null && cfg.getOrchestra(tp) == OrchestrasF1.ORCHESTRA_FLAMER && cfg.getAnims(tp) == XFactoryFlamer.LAMBDA_FLAMER_ANIMS
				&& mfr.acceptedBullets.get(0) == XFactoryFlamer.flame_topaz_diesel && mfr.acceptedBullets.get(1) == XFactoryFlamer.flame_topaz_gas
				&& mfr.acceptedBullets.get(2) == XFactoryFlamer.flame_topaz_napalm && mfr.acceptedBullets.get(3) == XFactoryFlamer.flame_topaz_balefire, "topaz values");
		press(player, tp, GunKeybind.RELOAD);
		release(player, tp, GunKeybind.RELOAD);
		tick(player, tp, 93);
		fire(player, tp, GunKeybind.GUN_PRIMARY);
		release(player, tp, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, mag.getAmount(tp, null) == 499 && countAmmo(player, StandardAmmoType.FLAME_GAS) == 1 && bullets.size() == 2 && bullets.get(0).ammoType() == StandardAmmoType.FLAME_TOPAZ_GAS
				&& Math.abs(bullets.get(0).baseDamage() - 1.5F) < 0.001F, "topaz loads 500 gas and fires two flames per shot " + bullets.size() + " " + mag.getAmount(tp, null));
		discard(level);

		// Daybreaker: dmg 25, delay 10, mag 50, black powder fire sound
		ItemStack db = equip(player, HbmItems.GUN_FLAMER_DAYBREAKER.get(), stack(StandardAmmoType.FLAME_BALEFIRE, 2));
		cfg = ((ItemGunBaseNT) db.getItem()).getConfig(db, 0);
		rec = cfg.getReceivers(db)[0];
		mag = rec.getMagazine(db);
		mfr = (MagazineFullReload) mag;
		check(problems, cases, mag.getCapacity(db) == 50 && cfg.getDurability(db) == 20_000 && rec.getBaseDamage(db) == 25F && rec.getDelayAfterFire(db) == 10 && rec.getRefireOnHold(db) && rec.getReloadBeginDuration(db) == 90
				&& rec.getJamDuration(db) == 17 && rec.getFireSound(db) == HbmSoundEvents.GUN_POWDER_FIRE && rec.getFireVolume(db) == 1F && rec.getFirePitch(db) == 1F
				&& cfg.getOrchestra(db) == OrchestrasF1.ORCHESTRA_FLAMER_DAYBREAKER && cfg.getAnims(db) == XFactoryFlamer.LAMBDA_FLAMER_ANIMS
				&& mfr.acceptedBullets.get(0) == XFactoryFlamer.flame_daybreaker_diesel && mfr.acceptedBullets.get(3) == XFactoryFlamer.flame_daybreaker_balefire, "daybreaker values");
		press(player, db, GunKeybind.RELOAD);
		release(player, db, GunKeybind.RELOAD);
		tick(player, db, 93);
		fire(player, db, GunKeybind.GUN_PRIMARY);
		release(player, db, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, mag.getAmount(db, null) == 49 && bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.FLAME_DAYBREAKER_BALEFIRE && bullets.get(0).balefire()
				&& ItemGunBaseNT.getTimer(db, 0) == 10 && Math.abs(bullets.get(0).baseDamage() - 25F) < 0.001F, "daybreaker loads 50 balefire and fires a balefire shell " + bullets.size());
		discard(level);
		player.getInventory().clearContent();

		/// Flame behaviour ///
		Pig pig = pig(level, new Vec3(0.5D, player.getEyeY() - 0.45D, 3.5D));
		if(pig != null) {
			float h = pig.getHealth();
			LegacyBulletEntity b = shoot(player, XFactoryFlamer.flame_diesel, 1F);
			HbmLivingHazards haz = HbmLivingHazards.get(pig);
			check(problems, cases, (b.isRemoved() || b.isRetired()) && haz.getFire() >= 100 && haz.getBalefire() == 0 && Math.abs(h - pig.getHealth() - 1F) < 0.01F
					&& pig.getLastDamageSource() != null && pig.getLastDamageSource().is(HbmDamageTypes.SEDNA_FIRE), "diesel flame ignites (fire 100) and deals 1 fire damage " + haz.getFire() + " dmg=" + (h - pig.getHealth()));
			h = pig.getHealth();
			b = shoot(player, XFactoryFlamer.flame_balefire, 1F);
			haz = HbmLivingHazards.get(pig);
			check(problems, cases, b.balefire() && haz.getBalefire() >= 200 && Math.abs(h - pig.getHealth() - 1F) < 0.01F, "balefire flame ignites with balefire 200 " + haz.getBalefire());
			haz.clearFire();
			pig.discard();
		}
		discard(level);

		// block hits: flammable face -> fire block and the flame flies on; stone -> lingering fire (diesel 2x1, napalm 2.5x1, balefire 3x1), gas only dies
		BlockPos planks = BlockPos.containing(0.5D, player.getEyeY() - 0.1D, 6.5D);
		level.setBlock(planks, Blocks.OAK_PLANKS.defaultBlockState(), 3);
		// igniteIfPossible returns true on the first hit: no lingering fire and the flame is still alive in that tick
		LegacyBulletEntity d = Lego.spawnBullet(player, XFactoryFlamer.flame_diesel, 1F, 0F, -0.25D, -0.0625D, 0.75D);
		boolean fireBlock = false, aliveAtIgnite = false, noLinger = false;
		for(int i = 0; i < 8 && !d.isRemoved() && !d.isRetired(); i++) {
			d.tickCount++; d.tick();
			if(level.getBlockState(planks.north()).is(Blocks.FIRE)) { fireBlock = true; aliveAtIgnite = !d.isRemoved() && !d.isRetired(); noLinger = fires(level).isEmpty(); break; }
		}
		check(problems, cases, fireBlock && aliveAtIgnite && noLinger, "diesel flame ignites the planks face and keeps flying " + fireBlock + "/" + aliveAtIgnite + "/" + noLinger);
		discardFires(level);
		clearBlocks(level, planks);
		discard(level);
		level.setBlock(planks, Blocks.STONE.defaultBlockState(), 3);
		d = shootTicks(player, XFactoryFlamer.flame_diesel, 1F, 8);
		List<LegacyLingeringFireEntity> fires = fires(level);
		check(problems, cases, (d.isRemoved() || d.isRetired()) && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 2F) < 0.01F && Math.abs(fires.get(0).getBbHeight() - 1F) < 0.01F, "diesel flame on stone: lingering fire 2x1 " + fires.size());
		discardFires(level);
		d = shootTicks(player, XFactoryFlamer.flame_gas, 1F, 8);
		check(problems, cases, (d.isRemoved() || d.isRetired()) && fires(level).isEmpty(), "gas flame on stone: no lingering fire");
		d = shootTicks(player, XFactoryFlamer.flame_napalm, 1F, 8);
		fires = fires(level);
		check(problems, cases, (d.isRemoved() || d.isRetired()) && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 2.5F) < 0.01F, "napalm flame on stone: lingering fire 2.5x1 " + fires.size());
		discardFires(level);
		d = shootTicks(player, XFactoryFlamer.flame_balefire, 1F, 8);
		fires = fires(level);
		check(problems, cases, (d.isRemoved() || d.isRetired()) && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 3F) < 0.01F, "balefire flame on stone: lingering fire 3x1 " + fires.size());
		if(fires.size() == 1) {
			// the lingering fire type is checked through its effect: balefire extends HbmLivingProps.balefire
			Pig inFire = pig(level, fires.get(0).position());
			if(inFire != null) {
				for(int i = 0; i < 3; i++) { fires.get(0).tickCount++; fires.get(0).tick(); }
				check(problems, cases, HbmLivingHazards.get(inFire).getBalefire() > 0, "balefire lingering fire type");
				inFire.discard();
			}
		}
		discardFires(level);
		discard(level);

		// daybreaker: standardExplode(5) + lingering fire 6x2 (diesel, 200 ticks), the shell dies on impact
		Pig near = pig(level, new Vec3(2.5D, player.getEyeY() - 0.45D, 5.5D));
		if(near != null) {
			float h = near.getHealth();
			d = shootTicks(player, XFactoryFlamer.flame_daybreaker_diesel, 25F, 8);
			fires = fires(level);
			check(problems, cases, (d.isRemoved() || d.isRetired()) && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 6F) < 0.01F && Math.abs(fires.get(0).getBbHeight() - 2F) < 0.01F && near.getHealth() < h,
					"daybreaker shell explodes (dmg " + (h - near.getHealth()) + ") and leaves a 6x2 fire " + fires.size());
			discardFires(level);
			discard(level);
			// gas shell: explosion only
			h = near.getHealth();
			d = shootTicks(player, XFactoryFlamer.flame_daybreaker_gas, 25F, 8);
			check(problems, cases, (d.isRemoved() || d.isRetired()) && fires(level).isEmpty() && near.getHealth() < h, "daybreaker gas shell explodes without fire");
			near.discard();
		}
		discardFires(level);
		discard(level);
		clearBlocks(level, planks);

		/// Chemthrower ///
		ItemStack ct = equip(player, chem);
		cfg = ((ItemGunBaseNT) ct.getItem()).getConfig(ct, 0);
		rec = cfg.getReceivers(ct)[0];
		mag = rec.getMagazine(ct);
		check(problems, cases, mag instanceof MagazineFluid && mag.getCapacity(ct) == 3_000 && cfg.getDurability(ct) == 90_000 && cfg.getDrawDuration(ct) == 10 && cfg.getInspectDuration(ct) == 17
				&& cfg.getCrosshair(ct) == Crosshair.L_CIRCLE && cfg.getSmokeHandler(ct) == Lego.LAMBDA_STANDARD_SMOKE && cfg.getOrchestra(ct) == OrchestrasF1.ORCHESTRA_CHEMTHROWER
				&& rec.getDelayAfterFire(ct) == 1 && rec.getHipfireSpread(ct) == 0F && rec.getRefireOnHold(ct) && rec.getBaseDamage(ct) == 0F && rec.getFireSound(ct) == null
				&& rec.getProjectileOffset(ct).equals(new Vec3(0.75, -0.0625, -0.25D)) && rec.getCanFire(ct) == ItemGunChemthrower.LAMBDA_CAN_FIRE && rec.getOnFire(ct) == ItemGunChemthrower.LAMBDA_FIRE
				&& cfg.getPressPrimary(ct) == Lego.LAMBDA_STANDARD_CLICK_PRIMARY && cfg.getPressReload(ct) == null && cfg.getPressTertiary(ct) == null && cfg.getPressSecondary(ct) == null
				&& cfg.getAnims(ct) == XFactoryFlamer.LAMBDA_CHEMTHROWER_ANIMS && cfg.getAnims(ct).apply(ct, GunAnimation.EQUIP) != null && cfg.getAnims(ct).apply(ct, GunAnimation.RELOAD) == null, "chemthrower values");
		check(problems, cases, mag.getAmount(ct, null) == 0 && mag.getType(ct, null) == HbmFluids.none() && !mag.canReload(ct, player.getInventory()) && mag.getCasing(ct, null) == -1
				&& "0mB".equals(mag.reportAmmoStateForHUD(ct, player)) && mag.getIconForHUD(ct, player).is(HbmItems.FLUID_ICON.get()), "empty chemthrower magazine");

		// IFillableItem: 50 mB per operation, one fluid at a time, 3000 mB
		HbmFluidDefinition diesel = HbmFluids.byName("diesel").orElse(HbmFluids.none());
		HbmFluidDefinition water = HbmFluids.byName("water").orElse(HbmFluids.none());
		Optional<IFluidHandlerItem> handler = FluidUtil.getFluidHandler(ct);
		check(problems, cases, handler.isPresent(), "chemthrower fluid handler capability");
		if(handler.isPresent()) {
			IFluidHandlerItem h = handler.get();
			int first = h.fill(HbmFluids.toNeoStack(diesel, 3_000), IFluidHandler.FluidAction.EXECUTE);
			check(problems, cases, first == 50 && ItemGunChemthrower.getMagCount(ct) == 50 && ((ItemGunChemthrower) chem).getFluidType(ct) == diesel && mag.getType(ct, null) == diesel
					&& h.getTankCapacity(0) == 3_000 && h.getFluidInTank(0).getAmount() == 50, "chemthrower tryFill 50 mB diesel " + first);
			check(problems, cases, h.fill(HbmFluids.toNeoStack(water, 1_000), IFluidHandler.FluidAction.EXECUTE) == 0 && mag.getType(ct, null) == diesel, "chemthrower rejects a second fluid while filled");
			int total = 50;
			for(int i = 0; i < 100 && total < 3_000; i++) total += h.fill(HbmFluids.toNeoStack(diesel, 10_000), IFluidHandler.FluidAction.EXECUTE);
			check(problems, cases, total == 3_000 && ItemGunChemthrower.getMagCount(ct) == 3_000 && h.fill(HbmFluids.toNeoStack(diesel, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "chemthrower fills to 3000 mB " + total);
			int drained = h.drain(200, IFluidHandler.FluidAction.EXECUTE).getAmount();
			check(problems, cases, drained == 50 && ItemGunChemthrower.getMagCount(ct) == 2_950 && h.drain(HbmFluids.toNeoStack(water, 50), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "chemthrower tryEmpty 50 mB " + drained);
			check(problems, cases, "2950mB".equals(mag.reportAmmoStateForHUD(ct, player)) && mag.getIconForHUD(ct, player).is(HbmItems.FLUID_ICON.get()), "chemthrower HUD " + mag.reportAmmoStateForHUD(ct, player));
		}
		// fire: EntityChemical of the loaded fluid, 3 mB and 1 wear per shot, no reload key
		tick(player, ct, 2);
		fire(player, ct, GunKeybind.GUN_PRIMARY);
		release(player, ct, GunKeybind.GUN_PRIMARY);
		List<LegacyChemicalEntity> chems = chems(level);
		check(problems, cases, chems.size() == 1 && chems.get(0).getFluid() == diesel && chems.get(0).getStyle() == ChemicalStyle.BURNING && chems.get(0).getThrower() == player
				&& mag.getAmount(ct, null) == 2_947 && ItemGunBaseNT.getWear(ct, 0) == 1F && ItemGunBaseNT.getTimer(ct, 0) == 1 && ItemGunBaseNT.getState(ct, 0) == GunState.COOLDOWN, "chemthrower sprays diesel " + chems.size());
		discardChems(level);
		press(player, ct, GunKeybind.RELOAD);
		release(player, ct, GunKeybind.RELOAD);
		tick(player, ct, 2);
		check(problems, cases, ItemGunBaseNT.getState(ct, 0) == GunState.IDLE && mag.getAmount(ct, null) == 2_947, "chemthrower has no reload key");
		ItemGunChemthrower.setMagCount(ct, 2);
		fire(player, ct, GunKeybind.GUN_PRIMARY);
		release(player, ct, GunKeybind.GUN_PRIMARY);
		check(problems, cases, chems(level).isEmpty() && mag.getAmount(ct, null) == 2 && ItemGunBaseNT.getWear(ct, 0) == 1F, "chemthrower needs 3 mB per shot");
		discardChems(level);
		player.getInventory().clearContent();

		/// EntityChemical behaviour ///
		Pig target = pig(level, new Vec3(0.5D, player.getEyeY() - 0.45D, 3.0D));
		if(target != null) {
			// diesel (LIQUID + COMBUSTIBLE -> BURNING): flamethrower damage 0.2 + min(combustion energy / 100000, 15) (registerCalculatedFuel value), 5 s of fire, fire blocks around the hit block
			float h = target.getHealth();
			// the spray is not killed by entity hits (only block hits), so the first hit is measured
			LegacyChemicalEntity c = new LegacyChemicalEntity(level, player, -0.25D, -0.0625D, 0.75D);
			c.setFluid(diesel);
			level.addFreshEntity(c);
			for(int i = 0; i < 40 && !c.isRemoved() && target.getHealth() == h; i++) { c.tickCount++; c.tick(); }
			float firstHit = h - target.getHealth();
			c.discard();
			check(problems, cases, c.getStyle() == ChemicalStyle.BURNING && c.getMaxAge() == 600 && c.getGravityVelocity() == 0.03D
					&& Math.abs(firstHit - (0.2F + Math.min(diesel.combustibleHeatEnergy() / 100_000F, 15F))) < 0.01F && diesel.combustibleHeatEnergy() > 500_000L && target.getRemainingFireTicks() > 0 && target.getLastDamageSource() != null && target.getLastDamageSource().is(HbmDamageTypes.FLAMETHROWER)
					&& target.getLastDamageSource().getEntity() == player, "diesel spray burns (dmg " + firstHit + ")");
			// water (LIQUID, < 50 C, not flammable): extinguishes
			c = spray(player, water);
			check(problems, cases, c.getStyle() == ChemicalStyle.LIQUID && target.getRemainingFireTicks() <= 0, "water spray extinguishes " + target.getRemainingFireTicks());
			// oil (LIQUID + FLAMMABLE, not combustible): soaked in oil for 300 ticks
			HbmFluidDefinition oil = HbmFluids.byName("oil").orElse(HbmFluids.none());
			c = spray(player, oil);
			check(problems, cases, c.getStyle() == ChemicalStyle.LIQUID && target.getPersistentData().getInt(LegacyChemicalEntity.OIL_TICKS_TAG) == 300, "oil spray douses the target");
			target.getPersistentData().remove(LegacyChemicalEntity.OIL_TICKS_TAG);
			// styles of the other fluid classes
			check(problems, cases, LegacyChemicalEntity.getStyleFromType(HbmFluids.byName("amat").orElse(HbmFluids.none())) == ChemicalStyle.AMAT
					&& LegacyChemicalEntity.getStyleFromType(HbmFluids.byName("iongel").orElse(HbmFluids.none())) == ChemicalStyle.LIGHTNING
					&& LegacyChemicalEntity.getStyleFromType(HbmFluids.byName("chlorine").orElse(HbmFluids.none())) == ChemicalStyle.GAS
					&& LegacyChemicalEntity.getStyleFromType(HbmFluids.byName("gas").orElse(HbmFluids.none())) == ChemicalStyle.GASFLAME
					&& LegacyChemicalEntity.getStyleFromType(HbmFluids.byName("balefire").orElse(HbmFluids.none())) == ChemicalStyle.BURNING, "chemical styles by fluid traits");
			target.discard();
		}
		// burning spray on a block: fire in the air neighbours
		level.setBlock(planks, Blocks.STONE.defaultBlockState(), 3);
		LegacyChemicalEntity c = spray(player, diesel);
		boolean around = false;
		for(net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) around |= level.getBlockState(planks.relative(dir)).is(Blocks.FIRE);
		check(problems, cases, c.isRemoved() && around, "burning spray ignites the hit block's surroundings");
		for(net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) if(level.getBlockState(planks.relative(dir)).is(Blocks.FIRE)) level.setBlock(planks.relative(dir), Blocks.AIR.defaultBlockState(), 3);
		clearBlocks(level, planks);
		discardChems(level);
		discard(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_F1_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	@SuppressWarnings("unchecked")
	private static boolean pedestalDaybreaker() {
		try {
			var field = com.atomiclegacy.tileentity.LegacyPedestalRecipes.class.getDeclaredField("RECIPES");
			field.setAccessible(true);
			for(Object o : (List<Object>) field.get(null)) {
				Object[] input = (Object[]) o.getClass().getMethod("input").invoke(o);
				String s = o.getClass().getMethod("output").invoke(o) + java.util.Arrays.toString(input) + o.getClass().getMethod("condition").invoke(o) + " set=" + o.getClass().getMethod("set").invoke(o);
				if(input.length != 9 || !input[4].toString().contains("itemId=gun_flamer,")) continue;
				if(s.contains("gun_flamer_daybreaker") && s.endsWith("SUN set=0")
						&& input[0].toString().contains("tagPath=plates/cast_gold, count=1") && input[2].toString().contains("tagPath=plates/cast_gold, count=1")
						&& input[6].toString().contains("tagPath=plates/cast_gold, count=1") && input[8].toString().contains("tagPath=plates/cast_gold, count=1")
						&& input[1].toString().contains("itemId=canned_conserve, variant=slime, tagPath=null, count=1")
						&& input[3].toString().contains("tagPath=ingots/phosphorus, count=1") && input[5].toString().contains("tagPath=ingots/phosphorus, count=1")
						&& input[7].toString().contains("itemId=stick_dynamite, variant=null, tagPath=null, count=1")) return true;
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

	/** Lego.spawnBullet with the gun offset, ticked until it is gone (at most 40 ticks). */
	private static LegacyBulletEntity shoot(FakePlayer player, BulletConfig config, float damage) {
		return shootTicks(player, config, damage, 40);
	}

	private static LegacyBulletEntity shootTicks(FakePlayer player, BulletConfig config, float damage, int ticks) {
		LegacyBulletEntity bullet = Lego.spawnBullet(player, config, damage, 0F, -0.25D, -0.0625D, 0.75D);
		for(int i = 0; i < ticks && !bullet.isRemoved() && !bullet.isRetired(); i++) { bullet.tickCount++; bullet.tick(); }
		return bullet;
	}

	/** ItemGunChemthrower.LAMBDA_FIRE's EntityChemical, ticked until it is gone (at most 40 ticks). */
	private static LegacyChemicalEntity spray(FakePlayer player, HbmFluidDefinition fluid) {
		LegacyChemicalEntity chem = new LegacyChemicalEntity(player.level(), player, -0.25D, -0.0625D, 0.75D);
		chem.setFluid(fluid);
		player.level().addFreshEntity(chem);
		for(int i = 0; i < 40 && !chem.isRemoved(); i++) { chem.tickCount++; chem.tick(); }
		return chem;
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

	private static List<LegacyLingeringFireEntity> fires(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
	}

	private static List<LegacyChemicalEntity> chems(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyChemicalEntity.class, AREA, c -> !c.isRemoved());
	}

	private static void discardFires(ServerLevel level) {
		for(LegacyLingeringFireEntity e : fires(level)) e.discard();
	}

	private static void discardChems(ServerLevel level) {
		for(LegacyChemicalEntity e : level.getEntitiesOfClass(LegacyChemicalEntity.class, AREA)) e.discard();
	}

	private static void discard(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
	}

	private static void clearBlocks(ServerLevel level, BlockPos center) {
		for(BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
			if(!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("[gun F1] " + message);
	}
}
