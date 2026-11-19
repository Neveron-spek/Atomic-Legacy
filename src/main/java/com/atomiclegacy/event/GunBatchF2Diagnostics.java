package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyCoinEntity;
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
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF2;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryAccelerator;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryCatapult;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunNI4NI;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineInfinite;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch F2 (XFactoryAccelerator: gun_tau, gun_coilgun, gun_n_i_4_n_i;
 * XFactoryCatapult: gun_fatman and the mini nukes) against the pinned 1.7.10 values.
 */
final class GunBatchF2Diagnostics {

	private GunBatchF2Diagnostics() {
	}

	private static final AABB AREA = new AABB(-64, 250, -64, 64, 330, 64);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo, tab ///
		Object[][] guns = {
				{ HbmItems.GUN_TAU.get(), WeaponQuality.A_SIDE, StandardAmmoType.TAU_URANIUM, 15 },
				{ HbmItems.GUN_FATMAN.get(), WeaponQuality.A_SIDE, StandardAmmoType.NUKE_STANDARD, 1 },
				{ HbmItems.GUN_COILGUN.get(), WeaponQuality.SPECIAL, StandardAmmoType.COIL_TUNGSTEN, 5 },
				{ HbmItems.GUN_N_I_4_N_I.get(), WeaponQuality.SPECIAL, null, 0 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == 1 && !ItemGunBaseNT.secrets.contains(item), name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, g[2] == null ? def.isEmpty() : StandardAmmoItem.standardType(def) == g[2] && def.getCount() == (int) g[3], name + " default ammo " + def);
		}
		check(problems, cases, ((ItemGunBaseNT) HbmItems.GUN_FATMAN.get()).isDefaultExpensive && !((ItemGunBaseNT) HbmItems.GUN_TAU.get()).isDefaultExpensive, "fat man default ammo is expensive");
		check(problems, cases, HbmItems.GUN_N_I_4_N_I.get() instanceof ItemGunNI4NI && HbmItems.GUN_N_I_4_N_I.get() instanceof com.atomiclegacy.items.ICustomizable, "n_i_4_n_i is ItemGunNI4NI / ICustomizable");

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int[] order = {
				indexOf(tab, HbmItems.GUN_STG77.get()), indexOf(tab, HbmItems.GUN_TAU.get()), indexOf(tab, HbmItems.GUN_FATMAN.get()), indexOf(tab, HbmItems.GUN_LASRIFLE.get()),
				indexOf(tab, HbmItems.GUN_FLIEGERFAUST.get()), indexOf(tab, HbmItems.GUN_COILGUN.get()), indexOf(tab, HbmItems.GUN_BOLTER.get()),
				indexOf(tab, HbmItems.GUN_DOUBLE_BARREL.get()), indexOf(tab, HbmItems.GUN_N_I_4_N_I.get()) };
		boolean ordered = order[0] >= 0;
		for(int i = 1; i < order.length; i++) ordered &= order[i] > order[i - 1];
		check(problems, cases, ordered, "weapon tab order (ModItems) " + java.util.Arrays.toString(order));

		/// Config values, sounds, damage types ///
		check(problems, cases, HbmConfig.tauCannonDamage() == 25F && HbmConfig.tauCannonChargeDamage() == 5F && HbmConfig.coilgunDamage() == 35F
				&& HbmConfig.ni4niDamage() == 35F && HbmConfig.fatmanDamage() == 100F, "config damage defaults");
		check(problems, cases, HbmSoundEvents.GUN_COIL_FIRE.isBound() && HbmSoundEvents.GUN_COIL_RELOAD.isBound() && HbmSoundEvents.GUN_FATMAN_FIRE.isBound() && HbmSoundEvents.GUN_FATMAN_RELOAD.isBound()
				&& HbmSoundEvents.GUN_TAU_FIRE.isBound() && HbmSoundEvents.GUN_TAU_STOPFIRE.isBound() && HbmSoundEvents.GUN_TAU_LOOP.isBound(), "sounds registered");
		var damageTypes = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		check(problems, cases, damageTypes.getHolder(HbmDamageTypes.SEDNA_SUBATOMIC).isPresent() && damageTypes.getHolder(HbmDamageTypes.TAU_BLAST).isPresent(), "sedna_subatomic / tau_blast damage types");

		/// Bullet configs ///
		BeamBulletConfig tu = XFactoryAccelerator.tau_uranium, tc = XFactoryAccelerator.tau_uranium_charge, arc = XFactoryAccelerator.ni4ni_arc;
		check(problems, cases, tu.ammo == StandardAmmoType.TAU_URANIUM && tu.dmgClass == DamageClass.SUBATOMIC && tu.expires == 5 && !tu.renderRotations && tu.doesPenetrate() && !tu.spectral
				&& !tu.ammo.damageFalloffByPenetration() && tu.damageMult() == 1F && tu.onImpactBeam == BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT && tu.beamRenderer == BeamBulletConfig.RENDER_TAU, "tau_uranium values");
		check(problems, cases, tc.ammo == StandardAmmoType.TAU_URANIUM && tc.dmgClass == DamageClass.SUBATOMIC && tc.expires == 5 && tc.doesPenetrate() && tc.spectral && tc.beamRenderer == BeamBulletConfig.RENDER_TAU_CHARGE
				&& XFactoryAccelerator.tauChargeMag.acceptedBullets.size() == 1 && XFactoryAccelerator.tauChargeMag.acceptedBullets.get(0) == tc, "tau_uranium_charge values / tauChargeMag");
		check(problems, cases, arc.dmgClass == DamageClass.PHYSICAL && arc.expires == 5 && !arc.doesPenetrate() && arc.thresholdNegation() == 10F && arc.armorPiercing() == 0.2F && !arc.spectral
				&& arc.damageMult() == 1F && arc.beamRenderer == BeamBulletConfig.RENDER_NI4NI_BOLT, "ni4ni_arc values");
		for(StandardAmmoType coil : new StandardAmmoType[] {StandardAmmoType.COIL_TUNGSTEN, StandardAmmoType.COIL_FERROURANIUM}) {
			check(problems, cases, coil.projectileSpeed() == 7.5D && coil.projectileLifetime() == 50 && coil.penetrates() && !coil.damageFalloffByPenetration() && coil.spectral()
					&& coil.onUpdate() != null && coil.projectileGravity() == 0D, coil.id() + " values");
		}
		Object[][] nukes = {
				{ StandardAmmoType.NUKE_STANDARD, 1, 3D, 0F, 1F }, { StandardAmmoType.NUKE_DEMO, 1, 3D, 0F, 1F }, { StandardAmmoType.NUKE_HIGH, 1, 3D, 0F, 1F },
				{ StandardAmmoType.NUKE_TOTS, 8, 3D, 0.1F, 0.35F }, { StandardAmmoType.NUKE_HIVE, 12, 1D, 0.15F, 0.25F }, { StandardAmmoType.NUKE_BALEFIRE, 1, 3D, 0F, 2.5F } };
		for(Object[] n : nukes) {
			StandardAmmoType t = (StandardAmmoType) n[0];
			check(problems, cases, t.projectileCount() == (int) n[1] && t.projectileCountMax() == (int) n[1] && t.projectileSpeed() == (double) n[2] && t.projectileSpread() == (float) n[3]
					&& t.damageMultiplier() == (float) n[4] && t.projectileLifetime() == 300 && t.projectileGravity() == 0.025D && t.onImpact() != null, t.id() + " values");
		}
		check(problems, cases, XFactoryCatapult.cluster_submunition != null, "cluster_submunition config");

		/// Recipes ///
		checkRecipe(problems, cases, server, level, "gun_fatman", HbmItems.GUN_FATMAN.get(), 3, 3, List.of(item("plate_saturnite"), item("plate_saturnite"), item("plate_saturnite"),
				item("part_barrel_heavy_saturnite"), item("shell_saturnite"), item("part_receiver_heavy_saturnite"),
				item("part_grip_pc"), ItemStack.EMPTY, item("part_mechanism_saturnite")));
		checkRecipe(problems, cases, server, level, "gun_tau", HbmItems.GUN_TAU.get(), 3, 3, List.of(ItemStack.EMPTY, item("part_receiver_light_saturnite"), item("circuit_bismoid"),
				item("pipe_copper"), item("coil_copper_torus"), item("coil_copper_torus"),
				item("part_grip_pvc"), item("part_mechanism_saturnite"), item("part_stock_pc")));
		var recipes = server.getRecipeManager();
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_coilgun")).isEmpty() && recipes.byKey(AtomicLegacy.id("gun_n_i_4_n_i")).isEmpty(), "no recipes for gun_coilgun / gun_n_i_4_n_i (none in 1.7.10)");

		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		/// Tau cannon ///
		ItemStack tau = equip(player, HbmItems.GUN_TAU.get(), stack(StandardAmmoType.TAU_URANIUM, 40));
		GunConfig cfg = ((ItemGunBaseNT) tau.getItem()).getConfig(tau, 0);
		Receiver rec = cfg.getReceivers(tau)[0];
		check(problems, cases, rec.getMagazine(tau) instanceof MagazineBelt && cfg.getDurability(tau) == 6400 && cfg.getDrawDuration(tau) == 10 && cfg.getInspectDuration(tau) == 10
				&& cfg.getCrosshair(tau) == Crosshair.CIRCLE && rec.getBaseDamage(tau) == 25F && rec.getHipfireSpread(tau) == 0F && rec.getDelayAfterFire(tau) == 4 && rec.getRefireOnHold(tau)
				&& rec.getInnateSpread(tau) == 0F && rec.getFireSound(tau) == null && rec.getProjectileOffset(tau).equals(new Vec3(1, -0.0625 * 2.5, -0.25D))
				&& cfg.getOrchestra(tau) == OrchestrasF2.ORCHESTRA_TAU && cfg.getPressSecondary(tau) == XFactoryAccelerator.LAMBDA_TAU_SECONDARY_PRESS
				&& cfg.getReleaseSecondary(tau) == XFactoryAccelerator.LAMBDA_TAU_SECONDARY_RELEASE && cfg.getReleasePrimary(tau) == XFactoryAccelerator.LAMBDA_TAU_PRIMARY_RELEASE
				&& cfg.getPressTertiary(tau) == null, "tau cannon values");
		press(player, tau, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletBeamEntity> beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == tu && Math.abs(beams.get(0).damage - 25F) < 0.001F && countAmmo(player, StandardAmmoType.TAU_URANIUM) == 39
				&& ItemGunBaseNT.getTimer(tau, 0) == 4 && ItemGunBaseNT.getWear(tau, 0) == 1F, "tau fires one beam from the belt " + beams.size());
		tick(player, tau, 5);
		check(problems, cases, beams(level).size() == 2 && countAmmo(player, StandardAmmoType.TAU_URANIUM) == 38, "tau auto fire on hold " + beams(level).size());
		release(player, tau, GunKeybind.GUN_PRIMARY);
		discard(level);
		// charge: SPINUP eats one round every 10 ticks, release after 25 ticks -> units 3, damage 1 * 3 * 5, wear +3
		tick(player, tau, 6);
		int before = countAmmo(player, StandardAmmoType.TAU_URANIUM);
		float wearBefore = ItemGunBaseNT.getWear(tau, 0);
		press(player, tau, GunKeybind.GUN_SECONDARY);
		check(problems, cases, ItemGunBaseNT.getLastAnim(tau, 0) == GunAnimation.SPINUP, "tau secondary starts SPINUP");
		tick(player, tau, 25);
		int used = before - countAmmo(player, StandardAmmoType.TAU_URANIUM);
		int timer = ItemGunBaseNT.getAnimTimer(tau, 0);
		release(player, tau, GunKeybind.GUN_SECONDARY);
		beams = beams(level);
		check(problems, cases, used == 3 && timer == 25 && beams.size() == 1 && beams.get(0).config == tc && Math.abs(beams.get(0).damage - 15F) < 0.001F
				&& Math.abs(ItemGunBaseNT.getWear(tau, 0) - wearBefore - 3F) < 0.001F && ItemGunBaseNT.getLastAnim(tau, 0) == GunAnimation.ALT_CYCLE,
				"tau charge units/damage used=" + used + " timer=" + timer + " beams=" + beams.size() + (beams.isEmpty() ? "" : " dmg=" + beams.get(0).damage));
		discard(level);
		press(player, tau, GunKeybind.GUN_SECONDARY);
		tick(player, tau, 5);
		release(player, tau, GunKeybind.GUN_SECONDARY);
		check(problems, cases, beams(level).isEmpty() && ItemGunBaseNT.getLastAnim(tau, 0) == GunAnimation.CYCLE_DRY, "tau charge released early is a dry cycle");
		discard(level);

		/// Coilgun ///
		ItemStack coil = equip(player, HbmItems.GUN_COILGUN.get(), stack(StandardAmmoType.COIL_TUNGSTEN, 5));
		cfg = ((ItemGunBaseNT) coil.getItem()).getConfig(coil, 0);
		rec = cfg.getReceivers(coil)[0];
		IMagazine<?> mag = rec.getMagazine(coil);
		check(problems, cases, mag instanceof MagazineSingleReload && mag.getCapacity(coil) == 1 && cfg.getDurability(coil) == 400 && cfg.getDrawDuration(coil) == 5 && cfg.getInspectDuration(coil) == 39
				&& cfg.getCrosshair(coil) == Crosshair.L_CIRCUMFLEX && rec.getBaseDamage(coil) == 35F && rec.getDelayAfterFire(coil) == 5 && rec.getReloadBeginDuration(coil) == 20
				&& rec.getJamDuration(coil) == 33 && rec.getFireSound(coil) == HbmSoundEvents.GUN_COIL_FIRE && rec.getProjectileOffset(coil).equals(new Vec3(0.75, -0.0625, -0.1875D))
				&& cfg.getOrchestra(coil) == OrchestrasF2.ORCHESTRA_COILGUN && rec.getRecoil(coil) == XFactoryAccelerator.LAMBDA_RECOIL_COILGUN, "coilgun values");
		press(player, coil, GunKeybind.RELOAD);
		release(player, coil, GunKeybind.RELOAD);
		tick(player, coil, 24);
		check(problems, cases, mag.getAmount(coil, null) == 1 && countAmmo(player, StandardAmmoType.COIL_TUNGSTEN) == 4, "coilgun loads one slug");
		press(player, coil, GunKeybind.GUN_PRIMARY);
		release(player, coil, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.COIL_TUNGSTEN && Math.abs(bullets.get(0).baseDamage() - 35F) < 0.001F
				&& Math.abs(bullets.get(0).getDeltaMovement().length() - 7.5D) < 1.5D /* 7.5 * |heading + gaussian hipfire spread 0.025| */ && mag.getAmount(coil, null) == 0, "coilgun fires one slug at 7.5 " + bullets.size() + (bullets.isEmpty() ? "" : " ammo=" + bullets.get(0).ammoType() + " dmg=" + bullets.get(0).baseDamage() + " v=" + bullets.get(0).getDeltaMovement().length()) + " mag=" + mag.getAmount(coil, null));
		discard(level);
		// breakInPath: dirt (0.5) breaks under 1.25, stone (1.5) only under 2.5, obsidian never
		int by = 310;
		BlockPos dirt = new BlockPos(0, by, 5), stone = new BlockPos(0, by, 7), obsidian = new BlockPos(0, by, 9);
		level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
		level.setBlock(obsidian, Blocks.OBSIDIAN.defaultBlockState(), 3);
		LegacyBulletEntity slug = slug(level, StandardAmmoType.COIL_TUNGSTEN, by);
		XFactoryAccelerator.breakInPath(slug, 1.25F);
		check(problems, cases, level.getBlockState(dirt).isAir() && level.getBlockState(stone).is(Blocks.STONE) && level.getBlockState(obsidian).is(Blocks.OBSIDIAN), "tungsten slug breaks dirt only");
		slug.discard();
		slug = slug(level, StandardAmmoType.COIL_FERROURANIUM, by);
		XFactoryAccelerator.breakInPath(slug, 2.5F);
		check(problems, cases, level.getBlockState(stone).isAir() && level.getBlockState(obsidian).is(Blocks.OBSIDIAN), "ferrouranium slug breaks stone, not obsidian");
		slug.discard();
		level.setBlock(obsidian, Blocks.AIR.defaultBlockState(), 3);
		discard(level);

		/// N I 4 N I ///
		ItemStack ni = equip(player, HbmItems.GUN_N_I_4_N_I.get());
		cfg = ((ItemGunBaseNT) ni.getItem()).getConfig(ni, 0);
		rec = cfg.getReceivers(ni)[0];
		mag = rec.getMagazine(ni);
		check(problems, cases, mag instanceof MagazineInfinite inf && inf.type == arc && mag.getAmount(ni, player.getInventory()) == 9999 && !mag.canReload(ni, player.getInventory())
				&& "∞".equals(mag.reportAmmoStateForHUD(ni, player)) && cfg.getDurability(ni) == 0 && cfg.getDrawDuration(ni) == 5 && cfg.getInspectDuration(ni) == 39
				&& cfg.getCrosshair(ni) == Crosshair.CIRCLE && rec.getBaseDamage(ni) == 35F && rec.getDelayAfterFire(ni) == 10 && rec.getFireSound(ni) == HbmSoundEvents.GUN_COIL_FIRE
				&& rec.getOnFire(ni) == XFactoryAccelerator.LAMBDA_NOWEAR_FIRE && cfg.getPressSecondary(ni) == XFactoryAccelerator.LAMBDA_NI4NI_SECONDARY_PRESS
				&& cfg.getOrchestra(ni) == OrchestrasF2.ORCHESTRA_COILGUN, "n_i_4_n_i values");
		int coinsAfterDraw = ItemGunNI4NI.getCoinCount(ni);
		ItemGunNI4NI.setCoinCount(ni, 0);
		ItemGunNI4NI.setCoinCharge(ni, 0);
		tick(player, ni, 79);
		int c79 = ItemGunNI4NI.getCoinCount(ni);
		tick(player, ni, 1);
		int c80 = ItemGunNI4NI.getCoinCount(ni);
		tick(player, ni, 80 * 5);
		check(problems, cases, coinsAfterDraw == 0 && c79 == 0 && c80 == 1 && ItemGunNI4NI.getCoinCount(ni) == 4, "coins regenerate every 80 ticks up to 4: " + c79 + "/" + c80 + "/" + ItemGunNI4NI.getCoinCount(ni));
		press(player, ni, GunKeybind.GUN_PRIMARY);
		release(player, ni, GunKeybind.GUN_PRIMARY);
		beams = beams(level);
		check(problems, cases, beams.size() == 1 && beams.get(0).config == arc && Math.abs(beams.get(0).damage - 35F) < 0.001F && ItemGunBaseNT.getWear(ni, 0) == 0F
				&& mag.getAmount(ni, null) == 9999, "n_i_4_n_i fires an arc without wear or ammo " + beams.size());
		discard(level);
		press(player, ni, GunKeybind.GUN_SECONDARY);
		release(player, ni, GunKeybind.GUN_SECONDARY);
		List<LegacyCoinEntity> coins = level.getEntitiesOfClass(LegacyCoinEntity.class, AREA);
		check(problems, cases, coins.size() == 1 && ItemGunNI4NI.getCoinCount(ni) == 3 && coins.get(0).getThrower() == player
				&& Math.abs(coins.get(0).getDeltaMovement().y - (player.getLookAngle().y * 0.8D + 0.5D)) < 0.001D
				&& Math.abs(coins.get(0).getY() - (player.getEyeY() - 1.125D)) < 0.001D && coins.get(0).getBbWidth() == 1F, "coin thrown " + coins.size());
		if(!coins.isEmpty()) {
			LegacyCoinEntity c = coins.get(0);
			double vy = c.getDeltaMovement().y;
			c.tick();
			check(problems, cases, Math.abs(c.getDeltaMovement().y - (vy - 0.02D)) < 0.0001D && Math.abs(c.getDeltaMovement().z - player.getLookAngle().z * 0.8D) < 0.0001D && c.isPickable(),
					"coin gravity 0.02 without drag");
			c.discard();
		}
		// a coin in the beam's path: the arc ricochets off it with 1.25x damage towards the nearest living entity
		Pig pig = pig(level, new Vec3(6.5D, player.getEyeY() - 0.45D, 6.5D));
		LegacyCoinEntity coin = new LegacyCoinEntity(level);
		coin.setPos(0.5D, player.getEyeY() - 0.0625D - 0.5D, 6.5D);
		level.addFreshEntity(coin);
		float hp = pig != null ? pig.getHealth() : 0F;
		LegacyBulletBeamEntity shot = new LegacyBulletBeamEntity(player, arc, 35F, 0F, 0D, 0D, 0D);
		level.addFreshEntity(shot);
		List<LegacyBulletBeamEntity> rico = level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA, b -> b != shot && !b.isRemoved());
		check(problems, cases, coin.isRemoved() && shot.beamLength < 7D && rico.size() == 1 && Math.abs(rico.get(0).damage - 43.75F) < 0.001F
				&& pig != null && pig.getHealth() < hp, "beam ricochets off the coin rico=" + rico.size() + " len=" + shot.beamLength + " pig=" + (pig != null ? hp - pig.getHealth() : -1));
		if(pig != null) pig.discard();
		discard(level);

		/// Fat Man ///
		ItemStack fm = equip(player, HbmItems.GUN_FATMAN.get(), stack(StandardAmmoType.NUKE_STANDARD, 2), stack(StandardAmmoType.NUKE_TOTS, 1));
		cfg = ((ItemGunBaseNT) fm.getItem()).getConfig(fm, 0);
		rec = cfg.getReceivers(fm)[0];
		mag = rec.getMagazine(fm);
		check(problems, cases, mag instanceof MagazineSingleReload && mag.getCapacity(fm) == 1 && cfg.getDurability(fm) == 300 && cfg.getDrawDuration(fm) == 20 && cfg.getInspectDuration(fm) == 30
				&& cfg.getReloadChangesType(fm) && cfg.getCrosshair(fm) == Crosshair.L_CIRCUMFLEX && !cfg.getHideCrosshair(fm) && rec.getBaseDamage(fm) == 100F && rec.getHipfireSpread(fm) == 0F
				&& rec.getDelayAfterFire(fm) == 10 && rec.getReloadBeginDuration(fm) == 57 && rec.getJamDuration(fm) == 40 && rec.getFireSound(fm) == HbmSoundEvents.GUN_FATMAN_FIRE
				&& rec.getProjectileOffset(fm).equals(new Vec3(1, -0.0625 * 1.5, -0.1875D)) && rec.getProjectileOffsetScoped(fm).equals(new Vec3(1, -0.0625 * 1.5, -0.125D))
				&& cfg.getOrchestra(fm) == OrchestrasF2.ORCHESTRA_FATMAN, "fat man values");
		press(player, fm, GunKeybind.RELOAD);
		release(player, fm, GunKeybind.RELOAD);
		tick(player, fm, 59);
		press(player, fm, GunKeybind.GUN_PRIMARY);
		release(player, fm, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.NUKE_STANDARD && Math.abs(bullets.get(0).baseDamage() - 100F) < 0.001F
				&& Math.abs(bullets.get(0).getDeltaMovement().length() - 3D) < 0.01D && countAmmo(player, StandardAmmoType.NUKE_STANDARD) == 1, "fat man fires one mini nuke " + bullets.size());
		discard(level);
		// tiny tots: the reload switches the type (reloadChangeType) once the magazine is empty
		player.getInventory().setItem(1, ItemStack.EMPTY);
		tick(player, fm, 12);
		press(player, fm, GunKeybind.RELOAD);
		release(player, fm, GunKeybind.RELOAD);
		tick(player, fm, 59);
		press(player, fm, GunKeybind.GUN_PRIMARY);
		release(player, fm, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 8 && bullets.stream().allMatch(b -> b.ammoType() == StandardAmmoType.NUKE_TOTS && Math.abs(b.baseDamage() - 35F) < 0.001F),
				"tiny tots fire 8 projectiles " + bullets.size());
		discard(level);

		// standard: ExplosionVNT(10) without block processing, radiation +50 in the center chunk
		player.setPos(0.5D, 300.0D, -60.5D);
		ChunkRadiationData rad = ChunkRadiationData.get(level);
		BlockPos center = new BlockPos(30, 300, 30);
		double radBefore = rad.getRadiation(center);
		level.setBlock(center.below(), Blocks.STONE.defaultBlockState(), 3);
		Pig victim = pig(level, new Vec3(30.5D, 300.0D, 33.5D));
		float victimHp = victim != null ? victim.getHealth() : 0F;
		LegacyBulletEntity nuke = new LegacyBulletEntity(level, 30.5D, 302D, 30.5D, new Vec3(0, -1, 0), StandardAmmoType.NUKE_STANDARD, 100F, null, 0F);
		level.addFreshEntity(nuke);
		boolean consumed = StandardAmmoType.NUKE_STANDARD.onImpact().onImpact(nuke, new BlockHitResult(new Vec3(30.5D, 300D, 30.5D), Direction.UP, center.below(), false));
		double radAfter = rad.getRadiation(center);
		check(problems, cases, consumed && level.getBlockState(center.below()).is(Blocks.STONE) && radAfter - radBefore >= 49.9D
				&& victim != null && victimHp - victim.getHealth() > 10F, "nuke_standard: no block damage, radiation +" + (radAfter - radBefore) + ", damage " + (victim != null ? victimHp - victim.getHealth() : -1));
		if(victim != null) victim.discard();
		nuke.discard();
		level.setBlock(center.below(), Blocks.AIR.defaultBlockState(), 3);
		// demo: BlockAllocatorStandard(64) + fire mutator destroys the stone around the hit
		BlockPos demo = new BlockPos(-30, 300, 30);
		for(int dx = -1; dx <= 1; dx++) for(int dz = -1; dz <= 1; dz++) level.setBlock(demo.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
		LegacyBulletEntity demoNuke = new LegacyBulletEntity(level, -29.5D, 302D, 30.5D, new Vec3(0, -1, 0), StandardAmmoType.NUKE_DEMO, 100F, null, 0F);
		level.addFreshEntity(demoNuke);
		StandardAmmoType.NUKE_DEMO.onImpact().onImpact(demoNuke, new BlockHitResult(new Vec3(-29.5D, 300D, 30.5D), Direction.UP, demo.below(), false));
		int left = 0;
		for(int dx = -1; dx <= 1; dx++) for(int dz = -1; dz <= 1; dz++) if(level.getBlockState(demo.offset(dx, -1, dz)).is(Blocks.STONE)) left++;
		check(problems, cases, left == 0, "nuke_demo destroys blocks, stone left " + left);
		demoNuke.discard();
		for(BlockPos p : BlockPos.betweenClosed(demo.offset(-3, -3, -3), demo.offset(3, 3, 3))) if(!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);

		player.getInventory().clearContent();
		discard(level);
		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_F2_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static LegacyBulletEntity slug(ServerLevel level, StandardAmmoType type, int y) {
		LegacyBulletEntity slug = new LegacyBulletEntity(level, 0.5D, y + 0.5D, 10.5D, new Vec3(0, 0, 1), type, 35F, null, 0F);
		slug.xo = 0.5D;
		slug.yo = y + 0.5D;
		slug.zo = 3.5D;
		return slug;
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
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired() && !b.isRemoved());
	}

	private static List<LegacyBulletBeamEntity> beams(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA, b -> !b.isRemoved());
	}

	private static void discard(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
		for(LegacyBulletBeamEntity beam : level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA)) beam.discard();
		for(LegacyCoinEntity coin : level.getEntitiesOfClass(LegacyCoinEntity.class, AREA)) coin.discard();
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun batch F2: " + message);
	}
}
