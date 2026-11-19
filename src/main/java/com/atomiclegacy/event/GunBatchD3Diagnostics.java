package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.AmmoFamily;
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
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasD3;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory40mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch D3 (XFactory40mm: gun_flaregun, gun_congolake, gun_mk108, the g26
 * flares and the g40 grenades) against the pinned 1.7.10 values.
 */
final class GunBatchD3Diagnostics {

	private GunBatchD3Diagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 12);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Ballistics: EntityThrowableNT drops the unscaled motion by gravity, so the displacement loses gravity * velocity per tick ///
		for(StandardAmmoType t : new StandardAmmoType[] { StandardAmmoType.G26_FLARE, StandardAmmoType.G40_HE }) {
			double y0 = 300D;
			com.atomiclegacy.entity.LegacyBulletEntity b = new com.atomiclegacy.entity.LegacyBulletEntity(level, 0.5D, y0, 0.5D, new net.minecraft.world.phys.Vec3(0, 0, 1), t, 0F, null, 0F);
			level.addFreshEntity(b);
			for(int i = 0; i < 3; i++) b.tick();
			double expected = 3D * t.projectileGravity() * t.projectileSpeed();
			check(problems, cases, Math.abs((y0 - b.getY()) - expected) < 1.0E-9 && Math.abs(b.getZ() - (0.5D + 3D * t.projectileSpeed())) < 1.0E-9,
					t + " trajectory after 3 ticks: drop " + (y0 - b.getY()) + " expected " + expected + ", z " + b.getZ());
			b.discard();
		}

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_FLAREGUN.get(), StandardAmmoType.G26_FLARE, 3 },
				{ HbmItems.GUN_CONGOLAKE.get(), StandardAmmoType.G40_HE, 8 },
				{ HbmItems.GUN_MK108.get(), StandardAmmoType.G40_HE, 50 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == WeaponQuality.A_SIDE && gun.getConfigCount() == 1
					&& !ItemGunBaseNT.secrets.contains(item), name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, StandardAmmoItem.standardType(def) == g[1] && def.getCount() == (int) g[2], name + " default ammo " + def);
		}

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int mares = indexOf(tab, HbmItems.GUN_MARESLEG.get()), flare = indexOf(tab, HbmItems.GUN_FLAREGUN.get()), heavy = indexOf(tab, HbmItems.GUN_HEAVY_REVOLVER.get());
		int lib = indexOf(tab, HbmItems.GUN_LIBERATOR.get()), congo = indexOf(tab, HbmItems.GUN_CONGOLAKE.get()), uzi = indexOf(tab, HbmItems.GUN_UZI.get());
		int g3 = indexOf(tab, HbmItems.GUN_G3.get()), mk108 = indexOf(tab, HbmItems.GUN_MK108.get()), amat = indexOf(tab, HbmItems.GUN_AMAT.get());
		check(problems, cases, mares >= 0 && mares < flare && flare < heavy && lib < congo && congo < uzi && g3 < mk108 && mk108 < amat,
				"weapon tab order (ModItems) " + mares + "/" + flare + "/" + heavy + " " + lib + "/" + congo + "/" + uzi + " " + g3 + "/" + mk108 + "/" + amat);

		/// Ammo values (BulletConfig) ///
		for(StandardAmmoType t : new StandardAmmoType[] {StandardAmmoType.G26_FLARE, StandardAmmoType.G26_FLARE_SUPPLY, StandardAmmoType.G26_FLARE_WEAPON}) {
			check(problems, cases, t.family() == AmmoFamily.FLARE && t.projectileSpeed() == 2D && t.projectileGravity() == 0.015D && t.projectileLifetime() == 100
					&& t.damageMultiplier() == 1F && !t.penetrates() && t.ricochetAngle() == 5F && t.maxRicochetCount() == 2 && t.selfDamageDelay() == 2
					&& t.onImpact() == null && (t == StandardAmmoType.G26_FLARE ? t.onUpdate() == null : t.onUpdate() != null) /* [parity W1: airdrop] LAMBDA_SPAWN_C130_* */ && t.projectileCount() == 1 && t.projectileSpread() == 0F, t.id() + " values");
		}
		float[] g40mult = {1F, 0.5F, 0.75F, 0.75F, 0.75F};
		StandardAmmoType[] g40 = {StandardAmmoType.G40_HE, StandardAmmoType.G40_HEAT, StandardAmmoType.G40_DEMO, StandardAmmoType.G40_INC, StandardAmmoType.G40_PHOSPHORUS};
		for(int i = 0; i < g40.length; i++) {
			StandardAmmoType t = g40[i];
			check(problems, cases, t.family() == AmmoFamily.G40 && t.projectileSpeed() == 2D && t.projectileGravity() == 0.035D && t.projectileLifetime() == 200
					&& t.damageMultiplier() == g40mult[i] && !t.penetrates() && t.armorThresholdNegation() == 0F && t.armorPiercingPercent() == 0F
					&& t.onImpact() != null && t.selfDamageDelay() == 2 && t.projectileCount() == 1 && t.projectileSpread() == 0F, t.id() + " values");
		}
		check(problems, cases, StandardAmmoType.G40_HE.onImpact() == XFactory40mm.LAMBDA_STANDARD_EXPLODE && StandardAmmoType.G40_HEAT.onImpact() == XFactory40mm.LAMBDA_STANDARD_EXPLODE_HEAT
				&& StandardAmmoType.G40_DEMO.onImpact() == XFactory40mm.LAMBDA_STANDARD_EXPLODE_DEMO && StandardAmmoType.G40_INC.onImpact() == XFactory40mm.LAMBDA_STANDARD_EXPLODE_INC
				&& StandardAmmoType.G40_PHOSPHORUS.onImpact() == XFactory40mm.LAMBDA_STANDARD_EXPLODE_PHOSPHORUS, "g40 impact lambdas");
		BulletConfig[] cfgs = {XFactory40mm.g26_flare, XFactory40mm.g26_flare_supply, XFactory40mm.g26_flare_weapon, XFactory40mm.g40_he, XFactory40mm.g40_heat, XFactory40mm.g40_demo, XFactory40mm.g40_inc, XFactory40mm.g40_phosphorus};
		boolean casings = true;
		for(int i = 0; i < cfgs.length; i++) casings &= cfgs[i].casingEffect == OrchestrasD3.CASING_G26_FLARE + i && cfgs[i].wear == 1F && cfgs[i].ammoReloadCount == 1;
		check(problems, cases, casings && OrchestrasD3.CASING_G40_PHOS == OrchestrasD3.CASING_D3_FIRST + 7, "casing ordinals (CasingConfig G26_FLARE..G40_PHOS), wear 1");

		/// Config values ///
		check(problems, cases, HbmConfig.flareDamage() == 15F && HbmConfig.congoLakeDamage() == 20F && HbmConfig.mk108Damage() == 25F, "config damage defaults");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		checkRecipe(problems, cases, server, level, "gun_flaregun", HbmItems.GUN_FLAREGUN.get(), 3, 2, List.of(item("part_barrel_heavy_steel"), item("part_receiver_light_steel"), item("part_mechanism_gunmetal"),
				ItemStack.EMPTY, ItemStack.EMPTY, item("part_grip_steel")));
		checkRecipe(problems, cases, server, level, "gun_congolake", HbmItems.GUN_CONGOLAKE.get(), 3, 3, List.of(item("part_barrel_heavy_dura_steel"), item("part_mechanism_gunmetal"), ItemStack.EMPTY,
				item("part_barrel_heavy_dura_steel"), item("part_receiver_light_dura_steel"), item("part_stock_wood"),
				item("part_grip_wood"), ItemStack.EMPTY, ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_mk108", HbmItems.GUN_MK108.get(), 3, 3, List.of(ItemStack.EMPTY, item("part_grip_polymer"), item("part_grip_bakelite"),
				item("part_barrel_heavy_weaponsteel"), item("part_receiver_heavy_weaponsteel"), item("part_mechanism_weaponsteel"),
				ItemStack.EMPTY, item("shell_weaponsteel"), ItemStack.EMPTY));
		check(problems, cases, recipes.byKey(AtomicLegacy.id("gun_mk108")).isPresent(), "gun_mk108 recipe present");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		// Flare gun: dura 100, draw 7, inspect 39, dmg 15, delay 20, reload 28, jam 33, mag 1
		ItemStack fg = equip(player, HbmItems.GUN_FLAREGUN.get(), stack(StandardAmmoType.G26_FLARE, 3));
		GunConfig cfg = ((ItemGunBaseNT) fg.getItem()).getConfig(fg, 0);
		Receiver rec = cfg.getReceivers(fg)[0];
		IMagazine<?> mag = rec.getMagazine(fg);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(fg) == 100 && cfg.getDrawDuration(fg) == 7 && cfg.getInspectDuration(fg) == 39
				&& cfg.getCrosshair(fg) == Crosshair.L_CIRCUMFLEX && cfg.getSmokeHandler(fg) == XFactory40mm.LAMBDA_SMOKE && !cfg.getReloadAnimSequential(fg)
				&& rec.getBaseDamage(fg) == 15F && rec.getDelayAfterFire(fg) == 20 && rec.getReloadBeginDuration(fg) == 28 && rec.getJamDuration(fg) == 33
				&& mag.getCapacity(fg) == 1 && rec.getProjectileOffset(fg).equals(new Vec3(0.75, -0.0625, -0.1875)), "flaregun values");
		press(player, fg, GunKeybind.RELOAD);
		release(player, fg, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(fg, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(fg, 0) == 28, "flaregun reload start");
		tick(player, fg, 29);
		check(problems, cases, mag.getAmount(fg, null) == 1 && countAmmo(player, StandardAmmoType.G26_FLARE) == 2, "flaregun reload 1 flare");
		tick(player, fg, 5);
		fire(player, fg, GunKeybind.GUN_PRIMARY);
		release(player, fg, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.G26_FLARE && ItemGunBaseNT.getTimer(fg, 0) == 20
				&& Math.abs(bullets.get(0).baseDamage() - 15F) < 0.001F && Math.abs(bullets.get(0).getDeltaMovement().length() - 2D) < 0.3D
				&& mag.getAmount(fg, null) == 0 && mag.getCasing(fg, null) == OrchestrasD3.CASING_G26_FLARE, "flaregun shot n=" + bullets.size()
				+ (bullets.isEmpty() ? "" : " type=" + bullets.get(0).ammoType() + " dmg=" + bullets.get(0).baseDamage() + " v=" + bullets.get(0).getDeltaMovement().length())
				+ " timer=" + ItemGunBaseNT.getTimer(fg, 0) + " amount=" + mag.getAmount(fg, null) + " casing=" + mag.getCasing(fg, null));
		discard(level);

		// Congo Lake: dura 400, draw 7, inspect 39, sequential, type change, dmg 20, delay 24, reload 16/16/16/0, jam 0, mag 4
		ItemStack cl = equip(player, HbmItems.GUN_CONGOLAKE.get(), stack(StandardAmmoType.G40_HE, 8));
		cfg = ((ItemGunBaseNT) cl.getItem()).getConfig(cl, 0);
		rec = cfg.getReceivers(cl)[0];
		mag = rec.getMagazine(cl);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(cl) == 400 && cfg.getDrawDuration(cl) == 7 && cfg.getInspectDuration(cl) == 39
				&& cfg.getReloadAnimSequential(cl) && cfg.getReloadChangesType(cl) && rec.getBaseDamage(cl) == 20F && rec.getDelayAfterFire(cl) == 24
				&& rec.getReloadBeginDuration(cl) == 16 && rec.getReloadCycleDuration(cl) == 16 && rec.getReloadEndDuration(cl) == 16 && rec.getJamDuration(cl) == 0
				&& mag.getCapacity(cl) == 4, "congolake values");
		press(player, cl, GunKeybind.RELOAD);
		release(player, cl, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(cl, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(cl, 0) == 16, "congolake reload start");
		tick(player, cl, 120);
		check(problems, cases, mag.getAmount(cl, null) == 4 && countAmmo(player, StandardAmmoType.G40_HE) == 4 && ItemGunBaseNT.getState(cl, 0) == GunState.IDLE,
				"congolake sequential reload 4 grenades " + mag.getAmount(cl, null) + " " + ItemGunBaseNT.getState(cl, 0));
		fire(player, cl, GunKeybind.GUN_PRIMARY);
		release(player, cl, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.G40_HE && ItemGunBaseNT.getTimer(cl, 0) == 24
				&& Math.abs(bullets.get(0).baseDamage() - 20F) < 0.001F && mag.getAmount(cl, null) == 3 && mag.getCasing(cl, null) == OrchestrasD3.CASING_G40, "congolake shot");
		discard(level);

		// MK108: dura 5000, draw 20, inspect 65, dmg 25, delay 10, auto, dry fire after auto, reload 135, jam 25, mag 30
		ItemStack mk = equip(player, HbmItems.GUN_MK108.get(), stack(StandardAmmoType.G40_HEAT, 50));
		cfg = ((ItemGunBaseNT) mk.getItem()).getConfig(mk, 0);
		rec = cfg.getReceivers(mk)[0];
		mag = rec.getMagazine(mk);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(mk) == 5000 && cfg.getDrawDuration(mk) == 20 && cfg.getInspectDuration(mk) == 65
				&& !cfg.getHideCrosshair(mk) && cfg.getSmokeHandler(mk) == null && rec.getBaseDamage(mk) == 25F && rec.getDelayAfterFire(mk) == 10 && rec.getRefireOnHold(mk)
				&& rec.getDoesDryFireAfterAuto(mk) && rec.getReloadBeginDuration(mk) == 135 && rec.getJamDuration(mk) == 25 && mag.getCapacity(mk) == 30
				&& rec.getProjectileOffset(mk).equals(new Vec3(0.75, -0.125, -0.125)), "mk108 values");
		press(player, mk, GunKeybind.RELOAD);
		release(player, mk, GunKeybind.RELOAD);
		tick(player, mk, 62);
		check(problems, cases, mag.getAmount(mk, null) == 30 && countAmmo(player, StandardAmmoType.G40_HEAT) == 20 && ItemGunBaseNT.getState(mk, 0) == GunState.RELOADING,
				"mk108 orchestra reloadAction at tick 60 " + mag.getAmount(mk, null));
		tick(player, mk, 80);
		check(problems, cases, mag.getAmount(mk, null) == 30 && countAmmo(player, StandardAmmoType.G40_HEAT) == 20 && ItemGunBaseNT.getState(mk, 0) == GunState.IDLE, "mk108 reload end");
		fire(player, mk, GunKeybind.GUN_PRIMARY);
		tick(player, mk, 10);
		release(player, mk, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).size() == 2 && bullets(level).get(0).ammoType() == StandardAmmoType.G40_HEAT && mag.getAmount(mk, null) == 28
				&& Math.abs(bullets(level).get(0).baseDamage() - 25F * 0.5F) < 0.001F, "mk108 auto fire every 10 ticks " + bullets(level).size());
		discard(level);
		player.getInventory().clearContent();

		/// Projectile behaviour ///
		Pig pig = pig(level, player, 3.5D);
		if(pig != null) {
			// flare: HbmLivingProps.fire += 200 (additive), then the standard hit (15 * 1)
			HbmLivingHazards hz = HbmLivingHazards.get(pig);
			hz.addFire(50);
			HbmLivingHazards.set(pig, hz);
			float h = pig.getHealth();
			LegacyBulletEntity b = shoot(player, XFactory40mm.g26_flare, 15F);
			check(problems, cases, HbmLivingHazards.get(pig).getFire() == 250 && Math.abs(h - pig.getHealth() - 15F) < 0.01F && (b.isRemoved() || b.isRetired()),
					"flare ignite +200 and hit, fire=" + HbmLivingHazards.get(pig).getFire() + " dmg=" + (h - pig.getHealth()));
			HbmLivingHazards.get(pig).clearFire();
			HbmLivingHazards.set(pig, HbmLivingHazards.get(pig));

			// HE: standardExplode(5F), no direct hit; damage at most 20 (CrossSmooth)
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			b = shoot(player, XFactory40mm.g40_he, 20F);
			float dealt = h - pig.getHealth();
			check(problems, cases, (b.isRemoved() || b.isRetired()) && dealt > 0F && dealt <= 20F + 0.01F, "g40 HE explosion " + dealt);

			// HEAT: standardExplode(3.5F) with damage 10 (at most) plus a direct 30 (damage x3) hit
			pig.setDeltaMovement(Vec3.ZERO);
			h = pig.getHealth();
			b = shoot(player, XFactory40mm.g40_heat, 20F); // LegacyBulletEntity applies damageMult 0.5 -> damage 10
			dealt = h - pig.getHealth();
			check(problems, cases, (b.isRemoved() || b.isRetired()) && dealt > 30F - 0.01F && dealt <= 40F + 0.01F, "g40 HEAT explosion + direct x3 " + dealt);

			// phosphorus: standardExplode(3F) + lingering phosphorus fire 5x2 for 400 ticks
			pig.setDeltaMovement(Vec3.ZERO);
			b = shoot(player, XFactory40mm.g40_phosphorus, 15F);
			List<LegacyLingeringFireEntity> fires = level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
			check(problems, cases, (b.isRemoved() || b.isRetired()) && fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 5F) < 0.01F && Math.abs(fires.get(0).getBbHeight() - 2F) < 0.01F,
					"g40 phosphorus lingering fire " + fires.size());
			for(LegacyLingeringFireEntity f : fires) f.discard();
			pig.discard();
		}

		// DEMO: the VNT explosion destroys blocks (dirt in the flight path)
		BlockPos dirt = BlockPos.containing(0.5D, player.getEyeY() - 0.1D, 4.5D);
		level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
		LegacyBulletEntity demo = shoot(player, XFactory40mm.g40_demo, 15F);
		check(problems, cases, (demo.isRemoved() || demo.isRetired()) && level.getBlockState(dirt).isAir(), "g40 demo destroys blocks " + level.getBlockState(dirt));
		clearBlocks(level, dirt);

		// INC: fire in air blocks touching a flammable face (planks in the flight path)
		BlockPos planks = BlockPos.containing(0.5D, player.getEyeY() - 0.1D, 4.5D);
		level.setBlock(planks, Blocks.OAK_PLANKS.defaultBlockState(), 3);
		LegacyBulletEntity inc = shoot(player, XFactory40mm.g40_inc, 15F);
		boolean fireBlock = false;
		for(BlockPos p : BlockPos.betweenClosed(planks.offset(-2, -2, -2), planks.offset(2, 2, 2))) fireBlock |= level.getBlockState(p).is(Blocks.FIRE);
		List<LegacyLingeringFireEntity> fires = level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
		check(problems, cases, (inc.isRemoved() || inc.isRetired()) && fireBlock && fires.size() == 1, "g40 incendiary fire blocks + lingering fire " + fires.size());
		for(LegacyLingeringFireEntity f : fires) f.discard();
		clearBlocks(level, planks);
		discard(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_D3_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static void checkRecipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, Item result, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		check(problems, cases, recipe instanceof ShapedRecipe s && s.matches(input, level) && s.assemble(input, server.registryAccess()).is(result), id + " recipe");
	}

	private static void clearBlocks(ServerLevel level, BlockPos center) {
		for(BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
			if(!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
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

	private static LegacyBulletEntity shoot(FakePlayer player, BulletConfig config, float damage) {
		LegacyBulletEntity bullet = Lego.spawnBullet(player, config, damage, 0F, -0.25D, -0.0625D * 2.5D, 1D);
		for(int i = 0; i < 6 && !bullet.isRemoved() && !bullet.isRetired(); i++) { bullet.tickCount++; bullet.tick(); }
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
		if(!ok) problems.add("gun batch D3: " + message);
	}
}
