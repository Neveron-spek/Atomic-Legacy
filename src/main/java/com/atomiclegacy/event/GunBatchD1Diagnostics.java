package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.DecoCrtBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyDuchessGambitEntity;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC4;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasD1;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.tileentity.LegacyDisplayStandBlockEntity;
import com.atomiclegacy.tileentity.LegacyPedestalRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch D1 (gun_spas12, gun_autoshotgun, gun_autoshotgun_shredder,
 * gun_autoshotgun_sexy, g12_equestrian_bj, the shredder beams and submunitions, EntityDuchessGambit) against
 * the pinned 1.7.10 XFactory12ga / Orchestras / WeaponRecipes / PedestalRecipes values.
 */
final class GunBatchD1Diagnostics {

	private GunBatchD1Diagnostics() {
	}

	/** Inside the always loaded spawn chunks, apart from the batch C4 area. */
	private static final double OX = 0.5D, OY = 260.0D, OZ = 0.5D;
	private static final AABB AREA = new AABB(OX - 40, OY - 40, OZ - 40, OX + 40, OY + 40, OZ + 40);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo, tab ///
		Object[][] guns = {
				{ HbmItems.GUN_SPAS12.get(), WeaponQuality.A_SIDE, StandardAmmoType.G12, 16 },
				{ HbmItems.GUN_AUTOSHOTGUN.get(), WeaponQuality.A_SIDE, StandardAmmoType.G12, 20 },
				{ HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(), WeaponQuality.B_SIDE, StandardAmmoType.G12, 20 },
				{ HbmItems.GUN_AUTOSHOTGUN_SEXY.get(), WeaponQuality.LEGENDARY, StandardAmmoType.G12_MAGNUM, 50 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == 1, name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, def.is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(def) == g[2] && def.getCount() == (int) g[3], name + " default ammo " + def);
		}
		check(problems, cases, ItemGunBaseNT.secrets.contains(HbmItems.GUN_AUTOSHOTGUN_SEXY.get()) && !ItemGunBaseNT.secrets.contains(HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get()), "sexy is a secret (LEGENDARY)");

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int iUzi = indexOf(tab, HbmItems.GUN_UZI.get()), iSpas = indexOf(tab, HbmItems.GUN_SPAS12.get()), iStar = indexOf(tab, HbmItems.GUN_STAR_F.get());
		int iM2 = indexOf(tab, HbmItems.GUN_M2.get()), iAuto = indexOf(tab, HbmItems.GUN_AUTOSHOTGUN.get()), iLag = indexOf(tab, HbmItems.GUN_LAG.get());
		check(problems, cases, iUzi >= 0 && iUzi + 1 == iSpas && iSpas < iStar && iM2 >= 0 && iM2 + 1 == iAuto && iAuto < iLag /* [weapons batch E1] panzerschreck / quadro follow in ModItems order */,
				"weapon tab order uzi/spas12/star_f, m2/autoshotgun/lag " + iUzi + "/" + iSpas + "/" + iStar + " " + iM2 + "/" + iAuto + "/" + iLag);
		check(problems, cases, indexOf(tab, HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get()) < 0 && indexOf(tab, HbmItems.GUN_AUTOSHOTGUN_SEXY.get()) < 0, "B_SIDE/LEGENDARY guns have no creative tab");

		/// Rounds: submunitions (clones of the nine-round configs), beams, equestrian BJ ///
		StandardAmmoType[][] subs = {
				{ StandardAmmoType.G12, StandardAmmoType.G12_SUB }, { StandardAmmoType.G12_SLUG, StandardAmmoType.G12_SUB_SLUG },
				{ StandardAmmoType.G12_FLECHETTE, StandardAmmoType.G12_SUB_FLECHETTE }, { StandardAmmoType.G12_MAGNUM, StandardAmmoType.G12_SUB_MAGNUM },
				{ StandardAmmoType.G12_EXPLOSIVE, StandardAmmoType.G12_SUB_EXPLOSIVE }, { StandardAmmoType.G12_PHOSPHORUS, StandardAmmoType.G12_SUB_PHOSPHORUS },
		};
		for(StandardAmmoType[] p : subs) {
			StandardAmmoType o = p[0], s = p[1];
			boolean ok = s.damageMultiplier() == o.damageMultiplier() && s.projectileCount() == o.projectileCount() && s.projectileCountMax() == o.projectileCountMax()
					&& s.projectileSpread() == o.projectileSpread() && s.armorThresholdNegation() == o.armorThresholdNegation() && s.armorPiercingPercent() == o.armorPiercingPercent()
					&& s.headshotMultiplier() == o.headshotMultiplier() && s.penetrates() == o.penetrates() && s.damageFalloffByPenetration() == o.damageFalloffByPenetration()
					&& s.blackPowder() == o.blackPowder() && s.onImpact() == o.onImpact() && s.knockbackMultiplier() == o.knockbackMultiplier()
					&& s.ricochetAngle() == 90F && s.maxRicochetCount() == 3 && s.projectileSpeed() == 0.5D && s.projectileLifetime() == 50 && s.projectileGravity() == 0D
					&& s.onRicochet() == XFactory12ga.LAMBDA_SHREDDER_RICOCHET && s.onEntityHit() == XFactory12ga.LAMBDA_PLASMA_ENTITY_HIT;
			check(problems, cases, ok, s.id() + " submunition = clone of " + o.id());
		}
		Object[][] beams = {
				{ XFactory12ga.g12_shredder, 1F, XFactory12ga.g12 }, { XFactory12ga.g12_shredder_slug, 1F, XFactory12ga.g12_slug },
				{ XFactory12ga.g12_shredder_flechette, 1F, XFactory12ga.g12_flechette }, { XFactory12ga.g12_shredder_magnum, 2F, XFactory12ga.g12_magnum },
				{ XFactory12ga.g12_shredder_explosive, 2.5F, XFactory12ga.g12_explosive }, { XFactory12ga.g12_shredder_phosphorus, 1F, XFactory12ga.g12_phosphorus },
		};
		for(Object[] b : beams) {
			BeamBulletConfig beam = (BeamBulletConfig) b[0];
			BulletConfig orig = (BulletConfig) b[2];
			boolean ok = beam.damageMult() == (float) b[1] && Math.abs(beam.damageMult() - orig.damageMult() * orig.projectilesMax()) < 1.0E-6F
					&& beam.expires == 5 && !beam.renderRotations && beam.dmgClass == DamageClass.LASER && beam.laserColor == 0x151580 && beam.impactsEntities && !beam.spectral
					&& !beam.doesPenetrate() && beam.onImpactBeam != null && beam.casingEffect == orig.casingEffect && beam.wear == 1F
					&& beam.matches(orig.toStack()) && beam.toStack().is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(beam.toStack()) == orig.ammo;
			check(problems, cases, ok, beam.ammo.id() + " shredder beam values");
		}
		ItemStack secret = LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "g12_equestrian");
		StandardAmmoType bj = StandardAmmoType.G12_EQUESTRIAN_BJ;
		check(problems, cases, XFactory12ga.g12_equestrian_bj.matches(secret) && !XFactory12ga.g12_equestrian_bj.matches(stack(StandardAmmoType.G12, 1))
				&& bj.damageMultiplier() == 0F && bj.onImpact() == XFactory12ga.LAMBDA_BOAT && bj.projectileCount() == 1 && bj.ricochetAngle() == 5F
				&& XFactory12ga.g12_equestrian_bj.casingEffect == OrchestrasC4.CASING_G12_EQUESTRIAN, "g12_equestrian_bj values");
		check(problems, cases, server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(HbmDamageTypes.SEDNA_PLASMA).isPresent()
				&& server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(HbmDamageTypes.BOAT).isPresent(), "sedna_plasma / boat damage types");
		check(problems, cases, BeamBulletConfig.plasmaResistance("fau")[0] == 100F && BeamBulletConfig.plasmaResistance("trenchmaster")[1] == 0.25F
				&& BeamBulletConfig.plasmaResistance("rpa")[0] == 30F, "PLASMA resistances (energy category, no exact LASER entries)");

		/// Config values ///
		check(problems, cases, HbmConfig.spasDamage() == 32F && HbmConfig.autoshotgunDamage() == 48F && HbmConfig.shredderDamage() == 50F
				&& HbmConfig.sexyDamage() == 64F, "config damage defaults");

		/// Recipes (WeaponRecipes, PedestalRecipes) ///
		ItemStack bl = item("part_barrel_light_desh");
		recipe(server, level, problems, cases, "gun_spas12", 3, 2, List.of(bl, item("part_receiver_light_desh"), item("part_mechanism_gunmetal"),
				bl, item("part_grip_bakelite"), item("part_stock_desh")), HbmItems.GUN_SPAS12.get(), true);
		recipe(server, level, problems, cases, "gun_spas12", 3, 2, List.of(bl, item("part_receiver_light_desh"), item("part_mechanism_gunmetal"),
				bl, item("part_grip_polymer"), item("part_stock_desh")), HbmItems.GUN_SPAS12.get(), false);
		ItemStack grip = item("part_grip_polymer");
		recipe(server, level, problems, cases, "gun_autoshotgun", 3, 2, List.of(item("part_barrel_heavy_ferrouranium"), item("part_receiver_heavy_ferrouranium"),
				item("part_mechanism_weaponsteel"), grip, ItemStack.EMPTY, grip), HbmItems.GUN_AUTOSHOTGUN.get(), true);
		ItemStack m = item("part_mechanism_saturnite");
		recipe(server, level, problems, cases, "gun_autoshotgun_shredder", 3, 3, List.of(ItemStack.EMPTY, m, ItemStack.EMPTY, m, new ItemStack(HbmItems.GUN_AUTOSHOTGUN.get()), m,
				ItemStack.EMPTY, m, ItemStack.EMPTY), HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(), true);
		check(problems, cases, server.getRecipeManager().byKey(AtomicLegacy.id("gun_autoshotgun_sexy")).isEmpty(), "no crafting recipe for sexy (pedestal only)");
		pedestal(level, problems, cases);

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(OX, OY, OZ);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discard(level);

		// SPAS-12: dura 600, draw 20, inspect 39, reload 5/10/10/10/0, jam 36, delay 20, mag 8, hipfire 0, change type, RMB double shot, no aiming
		ItemStack spas = equip(player, HbmItems.GUN_SPAS12.get(), stack(StandardAmmoType.G12, 3));
		GunConfig cfg = ((ItemGunBaseNT) spas.getItem()).getConfig(spas, 0);
		Receiver rec = cfg.getReceivers(spas)[0];
		IMagazine<?> mag = rec.getMagazine(spas);
		check(problems, cases, mag instanceof MagazineSingleReload && cfg.getDurability(spas) == 600 && cfg.getDrawDuration(spas) == 20 && cfg.getInspectDuration(spas) == 39
				&& cfg.getReloadAnimSequential(spas) && cfg.getReloadChangesType(spas) && rec.getBaseDamage(spas) == 32F && rec.getDelayAfterFire(spas) == 20
				&& rec.getReloadCockOnEmptyPre(spas) == 5 && rec.getReloadBeginDuration(spas) == 10 && rec.getReloadCycleDuration(spas) == 10 && rec.getReloadEndDuration(spas) == 10
				&& rec.getReloadCockOnEmptyPost(spas) == 0 && rec.getJamDuration(spas) == 36 && mag.getCapacity(spas) == 8 && rec.getHipfireSpread(spas) == 0F
				&& cfg.getPressSecondary(spas) == XFactory12ga.LAMBDA_SPAS_SECONDARY && cfg.getPressTertiary(spas) == null && cfg.getOrchestra(spas) == OrchestrasD1.ORCHESTRA_SPAS
				&& rec.getProjectileOffset(spas).equals(new Vec3(0.75, -0.0625, -0.1875)), "spas12 values");
		press(player, spas, GunKeybind.RELOAD);
		release(player, spas, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(spas, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(spas, 0) == 15, "spas12 empty reload start 10 + 5 t=" + ItemGunBaseNT.getTimer(spas, 0));
		tick(player, spas, 15 + 10 + 10);
		check(problems, cases, mag.getAmount(spas, null) == 3 && ItemGunBaseNT.getLastAnim(spas, 0) == GunAnimation.RELOAD_END, "spas12 3 shells after 35 ticks " + mag.getAmount(spas, null));
		tick(player, spas, 10);
		fire(player, spas, GunKeybind.GUN_PRIMARY);
		release(player, spas, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 8 && bullets.stream().allMatch(e -> e.ammoType() == StandardAmmoType.G12 && Math.abs(e.baseDamage() - 4F) < 1.0E-4F)
				&& mag.getAmount(spas, null) == 2 && ItemGunBaseNT.getTimer(spas, 0) == 20, "spas12 LMB 8 pellets of 32/8 n=" + bullets.size());
		discard(level);
		tick(player, spas, 21);
		fire(player, spas, GunKeybind.GUN_SECONDARY);
		release(player, spas, GunKeybind.GUN_SECONDARY);
		check(problems, cases, bullets(level).size() == 16 && mag.getAmount(spas, null) == 0 && ItemGunBaseNT.getState(spas, 0) == GunState.COOLDOWN
				&& ItemGunBaseNT.getTimer(spas, 0) == 20 && ItemGunBaseNT.getWear(spas, 0) == 3F, "spas12 RMB fires both remaining shells n=" + bullets(level).size());
		discard(level);

		// Auto shotgun: dura 2000, draw 10, inspect 33, full reload 44, jam 19, delay 10, auto, mag 20
		ItemStack auto = equip(player, HbmItems.GUN_AUTOSHOTGUN.get(), stack(StandardAmmoType.G12_MAGNUM, 25));
		cfg = ((ItemGunBaseNT) auto.getItem()).getConfig(auto, 0);
		rec = cfg.getReceivers(auto)[0];
		mag = rec.getMagazine(auto);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(auto) == 2000 && cfg.getDrawDuration(auto) == 10 && cfg.getInspectDuration(auto) == 33
				&& rec.getBaseDamage(auto) == 48F && rec.getDelayAfterFire(auto) == 10 && rec.getRefireOnHold(auto) && rec.getRefireAfterDry(auto) && rec.getDoesDryFireAfterAuto(auto)
				&& rec.getReloadBeginDuration(auto) == 44 && rec.getJamDuration(auto) == 19 && mag.getCapacity(auto) == 20 && cfg.getOrchestra(auto) == OrchestrasD1.ORCHESTRA_SHREDDER
				&& rec.getProjectileOffset(auto).equals(new Vec3(0.75, -0.125, -0.25)), "autoshotgun values");
		press(player, auto, GunKeybind.RELOAD);
		release(player, auto, GunKeybind.RELOAD);
		tick(player, auto, 44);
		check(problems, cases, mag.getAmount(auto, null) == 20 && countAmmo(player, StandardAmmoType.G12_MAGNUM) == 5, "autoshotgun loads 20 after 44 ticks");
		tick(player, auto, 1);
		fire(player, auto, GunKeybind.GUN_PRIMARY);
		tick(player, auto, 10);
		release(player, auto, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, mag.getAmount(auto, null) == 18 && bullets.size() == 8 && bullets.stream().allMatch(e -> Math.abs(e.baseDamage() - 24F) < 1.0E-4F),
				"autoshotgun auto refire after 10 ticks, magnum 48*0.5 n=" + bullets.size() + " mag=" + mag.getAmount(auto, null));
		discard(level);

		// Shredder: belt feed of the shredder beams, 50 damage
		ItemStack shred = equip(player, HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get(), stack(StandardAmmoType.G12, 3));
		cfg = ((ItemGunBaseNT) shred.getItem()).getConfig(shred, 0);
		rec = cfg.getReceivers(shred)[0];
		mag = rec.getMagazine(shred);
		check(problems, cases, mag instanceof MagazineBelt belt && belt.acceptedBullets.size() == 6 && belt.acceptedBullets.get(0) == XFactory12ga.g12_shredder
				&& rec.getBaseDamage(shred) == 50F && cfg.getDurability(shred) == 2000 && mag.getAmount(shred, player.getInventory()) == 3
				&& mag.getType(shred, player.getInventory()) == XFactory12ga.g12_shredder, "shredder values");
		fire(player, shred, GunKeybind.GUN_PRIMARY);
		release(player, shred, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletBeamEntity> beamList = level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA);
		check(problems, cases, beamList.size() == 1 && beamList.get(0).config == XFactory12ga.g12_shredder && beamList.get(0).damage == 50F
				&& countAmmo(player, StandardAmmoType.G12) == 2, "shredder fires one beam of 50, uses one shell n=" + beamList.size());
		discard(level);

		// Beam block impact: pulse + 8 submunitions off the face (damage 50 * 1/8, vel 0.5)
		BlockPos wall = BlockPos.containing(OX, OY + 1.62D, OZ + 6D);
		level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		XFactory12ga.g12_shredder.onImpactBeam.accept(beamList.get(0), new BlockHitResult(new Vec3(wall.getX() + 0.5, wall.getY() + 0.5, wall.getZ()), Direction.NORTH, wall, false));
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 8 && bullets.stream().allMatch(e -> e.ammoType() == StandardAmmoType.G12_SUB && Math.abs(e.baseDamage() - 6.25F) < 1.0E-4F
				&& e.getDeltaMovement().z < 0), "beam block hit scatters 8 g12_sub n=" + bullets.size());
		// submunition ricochet: always bounces (angle 90), counts up to 3
		LegacyBulletEntity sub = bullets.isEmpty() ? null : bullets.get(0);
		discard(level, sub);
		if(sub != null) {
			sub.setPos(wall.getX() + 0.5, wall.getY() + 0.5, wall.getZ() - 0.25);
			sub.setDeltaMovement(0, 0, 0.5);
			sub.tick();
			check(problems, cases, !sub.isRemoved() && !sub.isRetired() && sub.getDeltaMovement().z < 0 && sub.ricochets() == 1, "g12_sub ricochets head-on " + sub.getDeltaMovement());
			sub.setRicochets(3);
			sub.setPos(wall.getX() + 0.5, wall.getY() + 0.5, wall.getZ() - 0.25);
			sub.setDeltaMovement(0, 0, 0.5);
			sub.tick();
			check(problems, cases, sub.isRemoved() || sub.isRetired(), "g12_sub dies after the 4th ricochet");
		}
		level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
		discard(level);

		// deco_crt is broken by a ricochet (variant 1, rotation kept)
		level.setBlockAndUpdate(wall, HbmBlocks.DECO_CRT.get().defaultBlockState().setValue(DecoCrtBlock.FACING, Direction.EAST));
		LegacyBulletEntity crtSub = new LegacyBulletEntity(level, wall.getX() + 0.5, wall.getY() + 0.5, wall.getZ() - 0.25, new Vec3(0, 0, 1), StandardAmmoType.G12_SUB, 1F, player, 0F);
		level.addFreshEntity(crtSub);
		crtSub.tick();
		check(problems, cases, level.getBlockState(wall).is(HbmBlocks.DECO_CRT.get()) && level.getBlockState(wall).getValue(DecoCrtBlock.VARIANT) == 1
				&& level.getBlockState(wall).getValue(DecoCrtBlock.FACING) == Direction.EAST, "ricochet breaks deco_crt");
		level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
		discard(level);

		Pig pig = EntityType.PIG.create(level);
		if(pig != null) {
			pig.setNoAi(true);
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
			pig.setHealth(1000F);
			pig.setPos(OX, OY + 1.0D, OZ + 3D);
			level.addFreshEntity(pig);

			// beam entity hit: no direct damage, 8 submunitions start inside the pig and hit it with PLASMA damage on the way out
			float before = pig.getHealth();
			XFactory12ga.g12_shredder.onImpactBeam.accept(beamList.get(0), new EntityHitResult(pig, pig.position().add(0, 0.45, 0)));
			bullets = bullets(level);
			check(problems, cases, pig.getHealth() == before && bullets.size() == 8, "beam entity hit: no direct damage, 8 submunitions");
			for(int t = 0; t < 4; t++) for(LegacyBulletEntity e : bullets) if(!e.isRemoved() && !e.isRetired()) e.tick();
			float dealt = before - pig.getHealth();
			check(problems, cases, dealt > 6.25F && dealt <= 8 * 6.25F * 1.25F + 0.01F && pig.getLastDamageSource() != null && pig.getLastDamageSource().is(HbmDamageTypes.SEDNA_PLASMA),
					"submunitions hit the pig from inside with sedna_plasma dmg=" + dealt);
			discard(level);

			// beam block blast: 0.75 around the hit, LASER, no knockback
			BlockPos floor = BlockPos.containing(pig.getX(), pig.getY() - 1, pig.getZ());
			before = pig.getHealth();
			XFactory12ga.g12_shredder.onImpactBeam.accept(beamList.get(0), new BlockHitResult(new Vec3(pig.getX(), pig.getY() - 0.1, pig.getZ()), Direction.UP, floor, false));
			check(problems, cases, Math.abs(before - pig.getHealth() - 50F) < 0.01F && pig.getLastDamageSource().is(HbmDamageTypes.SEDNA_LASER), "beam block blast 50 laser dmg=" + (before - pig.getHealth()));
			discard(level);
			pig.discard();
		}

		// Sexy: dura 5000, draw 20, inspect 65 (no cancel), crosshair shown, delay 4, auto without auto-after-dry, reload 110, mag 100
		ItemStack sexy = equip(player, HbmItems.GUN_AUTOSHOTGUN_SEXY.get(), secret.copyWithCount(2), stack(StandardAmmoType.G12, 30));
		cfg = ((ItemGunBaseNT) sexy.getItem()).getConfig(sexy, 0);
		rec = cfg.getReceivers(sexy)[0];
		mag = rec.getMagazine(sexy);
		check(problems, cases, mag instanceof MagazineFullReload && cfg.getDurability(sexy) == 5000 && cfg.getDrawDuration(sexy) == 20 && cfg.getInspectDuration(sexy) == 65
				&& !cfg.getInspectCancel(sexy) && !cfg.getHideCrosshair(sexy) && rec.getBaseDamage(sexy) == 64F && rec.getDelayAfterFire(sexy) == 4 && rec.getRefireOnHold(sexy)
				&& !rec.getRefireAfterDry(sexy) && rec.getDoesDryFireAfterAuto(sexy) && rec.getReloadBeginDuration(sexy) == 110 && rec.getJamDuration(sexy) == 19
				&& mag.getCapacity(sexy) == 100 && cfg.getOrchestra(sexy) == OrchestrasD1.ORCHESTRA_SHREDDER_SEXY, "sexy values");
		press(player, sexy, GunKeybind.RELOAD);
		release(player, sexy, GunKeybind.RELOAD);
		tick(player, sexy, 55);
		check(problems, cases, mag.getAmount(sexy, null) == 0, "sexy not loaded before anim tick 55");
		tick(player, sexy, 1);
		check(problems, cases, mag.getAmount(sexy, null) == 2 && mag.getType(sexy, null) == XFactory12ga.g12_equestrian_bj && ItemGunBaseNT.getState(sexy, 0) == GunState.RELOADING,
				"sexy loads at anim tick 55 (orchestra reloadAction) " + mag.getAmount(sexy, null));
		tick(player, sexy, 54 + 111 + 1);
		fire(player, sexy, GunKeybind.GUN_PRIMARY);
		release(player, sexy, GunKeybind.GUN_PRIMARY);
		tick(player, sexy, 1);
		check(problems, cases, mag.getAmount(sexy, null) == 1 && ItemGunBaseNT.getTimer(sexy, 0) == 19, "sexy equestrian BJ shot holds the gun for 20 ticks t=" + ItemGunBaseNT.getTimer(sexy, 0) + " a=" + mag.getAmount(sexy, null));
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.G12_EQUESTRIAN_BJ && bullets.get(0).baseDamage() == 0F, "sexy fires one equestrian BJ round");
		discard(level);
		player.getInventory().clearContent();
		player.getInventory().setItem(0, sexy);
		tick(player, sexy, 30);
		// inspect: mag cannot reload (no ammo) -> INSPECT, gun locked for 65 ticks, buffs at anim tick 60
		press(player, sexy, GunKeybind.RELOAD);
		release(player, sexy, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getLastAnim(sexy, 0) == GunAnimation.INSPECT && ItemGunBaseNT.getState(sexy, 0) == GunState.DRAWING && ItemGunBaseNT.getTimer(sexy, 0) == 65, "sexy inspect locks the gun");
		tick(player, sexy, 61);
		check(problems, cases, player.hasEffect(MobEffects.DAMAGE_BOOST) && player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 2
				&& player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 2 && player.hasEffect(MobEffects.CONFUSION), "sexy inspect drink gives strength/resistance III and nausea");
		player.removeAllEffects();
		player.getInventory().clearContent();

		// LAMBDA_BOAT: a Duchess Gambit 50 blocks above the hit
		LegacyBulletEntity boat = new LegacyBulletEntity(level, OX, OY, OZ, new Vec3(0, 0, 1), StandardAmmoType.G12_EQUESTRIAN_BJ, 0F, player, 0F);
		boolean consumed = XFactory12ga.LAMBDA_BOAT.onImpact(boat, new BlockHitResult(new Vec3(OX + 20, OY - 30, OZ), Direction.UP, BlockPos.containing(OX + 20, OY - 31, OZ), false));
		List<LegacyDuchessGambitEntity> gambits = level.getEntitiesOfClass(LegacyDuchessGambitEntity.class, AREA);
		check(problems, cases, consumed && gambits.size() == 1 && Math.abs(gambits.get(0).getY() - (OY + 20)) < 1.0E-6D, "LAMBDA_BOAT spawns a gambit 50 above the hit n=" + gambits.size());
		for(LegacyDuchessGambitEntity g : gambits) g.discard();

		// EntityDuchessGambit impact: 1000 boat damage in the 10x4x18 box, boat block, explosions
		BlockPos ground = BlockPos.containing(OX + 20, OY - 30, OZ);
		level.setBlockAndUpdate(ground, Blocks.BEDROCK.defaultBlockState());
		Pig victim = EntityType.PIG.create(level);
		LegacyDuchessGambitEntity gambit = new LegacyDuchessGambitEntity(level, new Vec3(ground.getX() + 0.5, ground.getY() + 3, ground.getZ() + 0.5));
		level.addFreshEntity(gambit);
		if(victim != null) {
			victim.setNoAi(true);
			victim.setPos(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 6.5);
			level.addFreshEntity(victim);
		}
		for(int t = 0; t < 40 && !gambit.isRemoved(); t++) gambit.tick();
		check(problems, cases, gambit.isRemoved() && (victim == null || (!victim.isAlive() && victim.getLastDamageSource() != null && victim.getLastDamageSource().is(HbmDamageTypes.BOAT))),
				"gambit impact kills with boat damage");
		BlockPos boatPos = new BlockPos((int) (gambit.getX() - 0.5), (int) (gambit.getY() + 0.5), (int) (gambit.getZ() - 0.5));
		check(problems, cases, level.getBlockState(boatPos).is(HbmBlocks.BOAT.get()), "gambit leaves a boat block at " + boatPos);
		level.setBlockAndUpdate(boatPos, Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(ground, Blocks.AIR.defaultBlockState());
		if(victim != null) victim.discard();
		for(Entity e : level.getEntitiesOfClass(Entity.class, new AABB(ground).inflate(16), e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
		discard(level);

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_D1_AUDIT cases={} problems={}", cases[0], problems.size());
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
		ItemStack spike = item("bolt_spike").copyWithCount(16);
		ItemStack[] items = { spike, item("wild_p"), spike, item("card_qos"), new ItemStack(HbmItems.GUN_AUTOSHOTGUN.get()), item("card_aos"),
				spike, item("ingot_starmetal").copyWithCount(16), spike };
		for(int i = 0; i < 9; i++) {
			BlockPos pos = center.offset(offsets[i][0], 0, offsets[i][1]);
			level.setBlockAndUpdate(pos, HbmBlocks.PEDESTAL.get().defaultBlockState());
			if(level.getBlockEntity(pos) instanceof LegacyDisplayStandBlockEntity stand) stand.setDisplayedItem(items[i].copy());
		}
		level.setBlockAndUpdate(center.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		boolean crafted = level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity c && c.displayedItem().is(HbmItems.GUN_AUTOSHOTGUN_SEXY.get());
		if(!crafted) LegacyPedestalRecipes.find(level, center).ifPresent(r -> LegacyPedestalRecipes.consume(level, center, r));
		boolean ok = level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity stand && stand.displayedItem().is(HbmItems.GUN_AUTOSHOTGUN_SEXY.get())
				&& level.getBlockEntity(center.offset(0, 0, -3)) instanceof LegacyDisplayStandBlockEntity side && side.displayedItem().isEmpty();
		check(problems, cases, ok, "pedestal recipe gun_autoshotgun_sexy");
		for(int[] o : offsets) {
			BlockPos pos = center.offset(o[0], 0, o[1]);
			if(level.getBlockEntity(pos) instanceof LegacyDisplayStandBlockEntity stand) stand.setDisplayedItem(ItemStack.EMPTY);
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		level.setBlockAndUpdate(center.below(), Blocks.AIR.defaultBlockState());
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
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired() && !b.isRemoved());
	}

	private static void discard(ServerLevel level) { discard(level, null); }

	private static void discard(ServerLevel level, Entity keep) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) if(bullet != keep) bullet.remove(Entity.RemovalReason.KILLED);
		for(LegacyBulletBeamEntity beam : level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA)) beam.remove(Entity.RemovalReason.KILLED);
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun batch D1: " + message);
	}
}
