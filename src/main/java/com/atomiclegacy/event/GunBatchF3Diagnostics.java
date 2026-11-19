package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LayeringBlock;
import com.atomiclegacy.blocks.SandMixBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.LegacyVariantBlockItem;
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
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF3;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryDrill;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryTool;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunDrill;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Dedicated-server checks for weapons batch F3 (XFactoryTool gun_fireext / gun_charge_thrower, XFactoryDrill gun_drill,
 * ammo_fireext) against the pinned 1.7.10 values.
 */
final class GunBatchF3Diagnostics {

	private GunBatchF3Diagnostics() {
	}

	private static final int X0 = 200;
	private static final AABB AREA = new AABB(X0 - 40, 260, -40, X0 + 40, 320, 60);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		for(int cx = (X0 - 40) >> 4; cx <= (X0 + 40) >> 4; cx++) for(int cz = -40 >> 4; cz <= 60 >> 4; cz++) level.getChunk(cx, cz);

		/// Registration, quality, default ammo, tab ///
		Item fe = HbmItems.GUN_FIREEXT.get(), ct = HbmItems.GUN_CHARGE_THROWER.get(), dr = HbmItems.GUN_DRILL.get();
		check(problems, cases, fe instanceof ItemGunBaseNT g && g.quality == WeaponQuality.UTILITY && g.getConfigCount() == 1 && g.getDefaultAmmo().isEmpty()
				&& new ItemStack(fe).getMaxStackSize() == 1, "gun_fireext item/quality/default ammo");
		check(problems, cases, ct instanceof ItemGunChargeThrower g && g.quality == WeaponQuality.UTILITY && StandardAmmoItem.standardType(g.getDefaultAmmo()) == StandardAmmoType.CT_MORTAR
				&& g.getDefaultAmmo().getCount() == 3, "gun_charge_thrower item/quality/default ammo CT_MORTAR x3");
		check(problems, cases, dr instanceof ItemGunDrill g && g.quality == WeaponQuality.UTILITY && g.getDefaultAmmo().isEmpty(), "gun_drill item/quality");
		Item ammo = HbmItems.AMMO_FIREEXT.get();
		check(problems, cases, ammo instanceof LegacyVariantItem v && v.variantList().size() == 3 && v.variantList().get(0).id().equals("water")
				&& v.variantList().get(1).id().equals("foam") && v.variantList().get(2).id().equals("sand")
				&& new ItemStack(ammo).getMaxStackSize() == 64, "ammo_fireext variants water/foam/sand");
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int db = indexOf(tab, HbmItems.GUN_DOUBLE_BARREL.get()), ife = indexOf(tab, fe), ict = indexOf(tab, ct), idr = indexOf(tab, dr);
		int iam = indexOf(tab, ammo), ishell = indexOf(tab, HbmItems.AMMO_SHELL.get());
		int ammoEntries = 0;
		for(ItemStack s : tab) if(s.is(ammo)) ammoEntries++;
		check(problems, cases, db >= 0 && db < ife && ife + 1 == ict && ict + 1 == idr, "weapon tab order double_barrel < fireext < charge_thrower < drill " + db + "/" + ife + "/" + ict + "/" + idr);
		check(problems, cases, iam > idr && iam < ishell && ammoEntries == 3, "ammo_fireext (3 variants) in the weapon tab before ammo_shell " + iam + "/" + ishell + "/" + ammoEntries);

		/// Config, sounds ///
		check(problems, cases, HbmConfig.extinguisherDamage() == 0F && HbmConfig.chargeThrowerDamage() == 10F && HbmConfig.drillDamage() == 10F
				&& HbmConfig.doubleBarrelDamage() == 30F, "config damage defaults (drill has its own key, double barrel unchanged)");
		check(problems, cases, HbmSoundEvents.GUN_EXTINGUISHER_FIRE.isBound() && HbmSoundEvents.GUN_CHARGE_FIRE.isBound() && HbmSoundEvents.GUN_VALVE.isBound()
				&& HbmSoundEvents.GUN_ROCKET_INSERT.isBound(), "F3 sounds registered");

		/// Bullet configs ///
		check(problems, cases, bullet(XFactoryTool.fext_water, StandardAmmoType.FEXT_WATER, 300, 100, 0.75D, 0.04D, 0.025F) && bullet(XFactoryTool.fext_foam, StandardAmmoType.FEXT_FOAM, 300, 100, 0.75D, 0.04D, 0.05F)
				&& bullet(XFactoryTool.fext_sand, StandardAmmoType.FEXT_SAND, 300, 100, 0.75D, 0.04D, 0.05F), "fext_* values");
		check(problems, cases, StandardAmmoType.FEXT_WATER.onUpdate() == XFactoryTool.LAMBDA_WATER_UPDATE && StandardAmmoType.FEXT_WATER.onRicochet() == XFactoryTool.LAMBDA_WATER_HIT
				&& StandardAmmoType.FEXT_FOAM.onRicochet() == XFactoryTool.LAMBDA_FOAM_HIT && StandardAmmoType.FEXT_SAND.onRicochet() == XFactoryTool.LAMBDA_SAND_HIT
				&& StandardAmmoType.FEXT_WATER.damageMultiplier() == 1F, "fext_* lambdas");
		check(problems, cases, bullet(XFactoryTool.ct_hook, StandardAmmoType.CT_HOOK, 1, 6_000, 3D, 0.035D, 0F) && StandardAmmoType.CT_HOOK.penetrates()
				&& !StandardAmmoType.CT_HOOK.damageFalloffByPenetration() && StandardAmmoType.CT_HOOK.onImpact() == XFactoryTool.LAMBDA_HOOK
				&& StandardAmmoType.CT_HOOK.onUpdate() == XFactoryTool.LAMBDA_SET_HOOK, "ct_hook values");
		check(problems, cases, bullet(XFactoryTool.ct_mortar, StandardAmmoType.CT_MORTAR, 1, 200, 3D, 0.035D, 0F) && StandardAmmoType.CT_MORTAR.damageMultiplier() == 2.5F
				&& bullet(XFactoryTool.ct_mortar_charge, StandardAmmoType.CT_MORTAR_CHARGE, 1, 200, 3D, 0.035D, 0F) && StandardAmmoType.CT_MORTAR_CHARGE.damageMultiplier() == 5F
				&& StandardAmmoType.CT_MORTAR.onImpact() == XFactoryTool.LAMBDA_MORTAR && StandardAmmoType.CT_MORTAR_CHARGE.onImpact() == XFactoryTool.LAMBDA_MORTAR_CHARGE, "ct_mortar values");
		check(problems, cases, XFactoryTool.fext_foam.matches(LegacyVariantItem.stackFor(ammo, "foam")) && !XFactoryTool.fext_foam.matches(LegacyVariantItem.stackFor(ammo, "water"))
				&& XFactoryTool.fext_sand.toStack().is(ammo), "fext configs match the ammo_fireext variants");

		/// Recipes ///
		ItemStack water = HbmFluidContainerItem.makeFull(HbmItems.FLUID_TANK_FULL::get, HbmFluids.byName("water").orElseThrow());
		ItemStack tankWater = LegacyVariantItem.stackFor(ammo, "water");
		ItemStack boron = LegacyVariantBlockItem.stackFor((LegacyVariantBlockItem) HbmBlocks.SAND_MIX.get().asItem(), 0);
		ItemStack niter = item("niter"), plate = item("plate_steel"), bolt = item("bolt_steel");
		ItemStack mech = item("part_mechanism_gunmetal"), barrel = item("part_barrel_heavy_steel"), grip = item("part_grip_steel");
		checkRecipe(problems, cases, server, level, "gun_charge_thrower", new ItemStack(ct), null, 3, 3, List.of(mech, mech, mech, barrel, barrel, new ItemStack(Items.LEATHER), grip, grip, ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_charge_thrower_from_rubber", new ItemStack(ct), null, 3, 3, List.of(mech, mech, mech, barrel, barrel, item("ingot_rubber"), grip, grip, ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "gun_drill", new ItemStack(dr), null, 3, 3, List.of(ItemStack.EMPTY, item("ingot_gunmetal"), item("ingot_rubber"),
				item("ingot_titanium"), item("block_steel"), item("piston_selenium"), ItemStack.EMPTY, item("ingot_gunmetal"), item("ingot_rubber")));
		checkRecipe(problems, cases, server, level, "gun_fireext", new ItemStack(fe), null, 2, 2, List.of(item("pipe_steel"), bolt, ItemStack.EMPTY, item("tank_steel")));
		checkRecipe(problems, cases, server, level, "ammo_fireext", new ItemStack(ammo), "water", 3, 3, List.of(ItemStack.EMPTY, plate, ItemStack.EMPTY, bolt, water, bolt, ItemStack.EMPTY, plate, ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "ammo_fireext_foam", new ItemStack(ammo), "foam", 3, 3, List.of(ItemStack.EMPTY, niter, ItemStack.EMPTY, niter, tankWater, niter, ItemStack.EMPTY, niter, ItemStack.EMPTY));
		checkRecipe(problems, cases, server, level, "ammo_fireext_sand", new ItemStack(ammo), "sand", 3, 3, List.of(boron, boron, boron, boron, tankWater, boron, boron, boron, boron));

		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(X0 + 0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		clearArea(level);

		/// Fire extinguisher: dura 5000, draw 10, inspect 55, L_CIRCLE, delay 1, dry 0, auto, reload 20, mag 300 ///
		ItemStack ext = equip(player, fe, LegacyVariantItem.stackFor(ammo, "foam"));
		GunConfig cfg = ((ItemGunBaseNT) fe).getConfig(ext, 0);
		Receiver rec = cfg.getReceivers(ext)[0];
		IMagazine<?> mag = rec.getMagazine(ext);
		check(problems, cases, mag instanceof MagazineFullReload && mag.getCapacity(ext) == 300 && cfg.getDurability(ext) == 5_000 && cfg.getDrawDuration(ext) == 10 && cfg.getInspectDuration(ext) == 55
				&& cfg.getReloadChangesType(ext) && !cfg.getHideCrosshair(ext) && cfg.getCrosshair(ext) == Crosshair.L_CIRCLE && rec.getBaseDamage(ext) == 0F && rec.getDelayAfterFire(ext) == 1
				&& rec.getDelayAfterDryFire(ext) == 0 && rec.getRefireOnHold(ext) && rec.getInnateSpread(ext) == 0F && rec.getHipfireSpread(ext) == 0F && rec.getReloadBeginDuration(ext) == 20
				&& rec.getJamDuration(ext) == 0 && rec.getFireSound(ext) == HbmSoundEvents.GUN_EXTINGUISHER_FIRE && rec.getProjectileOffset(ext).equals(new Vec3(1, -0.0625 * 2.5, -0.25D))
				&& cfg.getOrchestra(ext) == OrchestrasF3.ORCHESTRA_FIREEXT && cfg.getAnims(ext) == null, "fire extinguisher values");
		press(player, ext, GunKeybind.RELOAD);
		release(player, ext, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(ext, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(ext, 0) == 20, "fire extinguisher reload start");
		tick(player, ext, 21);
		check(problems, cases, mag.getAmount(ext, null) == 300 && mag.getType(ext, null) == XFactoryTool.fext_foam && player.getInventory().getItem(1).isEmpty(),
				"fire extinguisher loads one foam tank as 300 shots");
		tick(player, ext, 2);
		press(player, ext, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> shots = bullets(level);
		check(problems, cases, shots.size() == 1 && shots.get(0).ammoType() == StandardAmmoType.FEXT_FOAM && mag.getAmount(ext, null) == 299 && ItemGunBaseNT.getTimer(ext, 0) == 1
				&& Math.abs(shots.get(0).getDeltaMovement().length() - 0.75D) < 0.2D, "fire extinguisher fires a foam round " + shots.size());
		tick(player, ext, 1);
		check(problems, cases, mag.getAmount(ext, null) == 298, "fire extinguisher auto refire (delay 1)");
		release(player, ext, GunKeybind.GUN_PRIMARY);
		clearArea(level);

		// Water: fire, foam layers and foam blocks in 3x3x3 removed
		BlockPos base = new BlockPos(X0 + 10, 290, 10);
		level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(base.above(), Blocks.FIRE.defaultBlockState());
		level.setBlockAndUpdate(base.east(), HbmBlocks.BLOCK_FOAM.get().defaultBlockState());
		LegacyBulletEntity w = round(level, player, StandardAmmoType.FEXT_WATER, base.above(2));
		boolean dead = XFactoryTool.LAMBDA_WATER_HIT.onImpact(w, new BlockHitResult(Vec3.atCenterOf(base), Direction.UP, base, false));
		check(problems, cases, dead && level.getBlockState(base.above()).isAir() && level.getBlockState(base.east()).isAir() && level.getBlockState(base).is(Blocks.STONE),
				"water round removes fire / foam around the hit and stops");
		level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
		// Water over volcanic lava source -> obsidian
		BlockPos lava = new BlockPos(X0 + 12, 290, 10);
		level.setBlock(lava, HbmBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState(), 2);
		LegacyBulletEntity w2 = round(level, player, StandardAmmoType.FEXT_WATER, lava);
		w2.setPos(lava.getX() + 0.5, lava.getY() + 0.5, lava.getZ() + 0.5);
		XFactoryTool.LAMBDA_WATER_UPDATE.onUpdate(w2);
		check(problems, cases, level.getBlockState(lava).is(Blocks.OBSIDIAN) && (w2.isRetired() || w2.isRemoved()), "water round turns a volcanic lava source into obsidian");
		level.setBlockAndUpdate(lava, Blocks.AIR.defaultBlockState());
		// Entity hit extinguishes
		Pig pig = pig(level, new Vec3(X0 + 14.5, 290, 10.5));
		if(pig != null) {
			pig.setRemainingFireTicks(100);
			boolean stop = XFactoryTool.LAMBDA_EXTINGUISH_STOP.onImpact(w, new EntityHitResult(pig));
			boolean pass = XFactoryTool.LAMBDA_EXTINGUISH_PASS.onImpact(w, new EntityHitResult(pig));
			check(problems, cases, !pig.isOnFire() && stop && !pass, "extinguisher rounds put out burning entities");
			pig.discard();
		}
		// Foam: lay a layer on stone, grow it, solidify to block_foam
		level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
		LegacyBulletEntity foam = round(level, player, StandardAmmoType.FEXT_FOAM, base.above(2));
		BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(base).add(0, 0.5, 0), Direction.UP, base, false);
		boolean laid = false;
		for(int i = 0; i < 64 && !laid; i++) { XFactoryTool.LAMBDA_FOAM_HIT.onImpact(foam, top); laid = level.getBlockState(base.above()).is(HbmBlocks.FOAM_LAYER.get()); }
		boolean solid = false;
		int maxLayers = laid ? level.getBlockState(base.above()).getValue(LayeringBlock.LAYERS) : 0;
		for(int i = 0; i < 256 && laid && !solid; i++) {
			XFactoryTool.LAMBDA_FOAM_HIT.onImpact(foam, top);
			if(level.getBlockState(base.above()).is(HbmBlocks.FOAM_LAYER.get())) maxLayers = Math.max(maxLayers, level.getBlockState(base.above()).getValue(LayeringBlock.LAYERS));
			solid = level.getBlockState(base.above()).is(HbmBlocks.BLOCK_FOAM.get());
		}
		check(problems, cases, laid && solid && maxLayers == 7 && level.getBlockState(base).is(Blocks.STONE), "foam round lays foam layers up to 7, then block_foam (" + maxLayers + ")");
		level.setBlockAndUpdate(base.above(), Blocks.FIRE.defaultBlockState());
		XFactoryTool.LAMBDA_FOAM_HIT.onImpact(foam, top);
		check(problems, cases, !level.getBlockState(base.above()).is(Blocks.FIRE), "foam round removes fire");
		level.setBlockAndUpdate(base.above(), Blocks.AIR.defaultBlockState());
		// Sand: boron sand layers, then sand_mix boron
		LegacyBulletEntity sand = round(level, player, StandardAmmoType.FEXT_SAND, base.above(2));
		boolean sandLaid = false, sandSolid = false;
		for(int i = 0; i < 64 && !sandLaid; i++) { XFactoryTool.LAMBDA_SAND_HIT.onImpact(sand, top); sandLaid = level.getBlockState(base.above()).is(HbmBlocks.SAND_BORON_LAYER.get()); }
		for(int i = 0; i < 256 && sandLaid && !sandSolid; i++) {
			XFactoryTool.LAMBDA_SAND_HIT.onImpact(sand, top);
			sandSolid = level.getBlockState(base.above()).is(HbmBlocks.SAND_MIX.get());
		}
		check(problems, cases, sandLaid && sandSolid && level.getBlockState(base.above()).getValue(SandMixBlock.VARIANT) == 0, "sand round lays boron sand layers, then sand_mix boron");
		level.setBlockAndUpdate(base.above(), Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
		clearArea(level);

		/// Charge thrower: dura 3000, L_CIRCUMFLEX, dmg 10, delay 4, dry 10, reload 60, mag 1 ///
		ItemStack thrower = equip(player, ct, stack(StandardAmmoType.CT_HOOK, 2));
		cfg = ((ItemGunBaseNT) ct).getConfig(thrower, 0);
		rec = cfg.getReceivers(thrower)[0];
		mag = rec.getMagazine(thrower);
		check(problems, cases, mag instanceof MagazineFullReload && mag.getCapacity(thrower) == 1 && cfg.getDurability(thrower) == 3_000 && cfg.getInspectDuration(thrower) == 55 && cfg.getReloadChangesType(thrower)
				&& !cfg.getHideCrosshair(thrower) && cfg.getCrosshair(thrower) == Crosshair.L_CIRCUMFLEX && rec.getBaseDamage(thrower) == 10F && rec.getDelayAfterFire(thrower) == 4
				&& rec.getDelayAfterDryFire(thrower) == 10 && rec.getRefireOnHold(thrower) && rec.getReloadBeginDuration(thrower) == 60 && rec.getFireSound(thrower) == HbmSoundEvents.GUN_CHARGE_FIRE
				&& rec.getRecoil(thrower) == XFactoryTool.LAMBDA_RECOIL_CT && cfg.getOrchestra(thrower) == OrchestrasF3.ORCHESTRA_CHARGE_THROWER
				&& cfg.getAnims(thrower).apply(thrower, GunAnimation.RELOAD) != null && cfg.getAnims(thrower).apply(thrower, GunAnimation.INSPECT) != null
				&& cfg.getAnims(thrower).apply(thrower, GunAnimation.JAMMED) == null, "charge thrower values");
		// stone wall 8 blocks ahead
		BlockPos wall = BlockPos.containing(X0 + 0.5, player.getEyeY(), 8.5);
		for(int dx = -2; dx <= 2; dx++) for(int dy = -2; dy <= 2; dy++) level.setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState());
		press(player, thrower, GunKeybind.RELOAD);
		release(player, thrower, GunKeybind.RELOAD);
		tick(player, thrower, 61);
		check(problems, cases, mag.getAmount(thrower, null) == 1 && mag.getType(thrower, null) == XFactoryTool.ct_hook && countAmmo(player, StandardAmmoType.CT_HOOK) == 1, "charge thrower loads one hook");
		tick(player, thrower, 2);
		press(player, thrower, GunKeybind.GUN_PRIMARY);
		release(player, thrower, GunKeybind.GUN_PRIMARY);
		shots = bullets(level);
		LegacyBulletEntity hook = shots.size() == 1 ? shots.get(0) : null;
		check(problems, cases, hook != null && hook.ammoType() == StandardAmmoType.CT_HOOK && mag.getAmount(thrower, null) == 0, "charge thrower fires the hook " + shots.size());
		if(hook != null) {
			hook.tickCount++;
			hook.tick();
			check(problems, cases, ItemGunChargeThrower.getLastHook(thrower) == hook.getId() && hook.ignoreFrustumCheck, "hook registers itself as lasthook during its first ticks");
			for(int i = 0; i < 10 && !hook.inGround; i++) { hook.tickCount++; hook.tick(); }
			hook.tickCount++;
			hook.tick();
			check(problems, cases, hook.inGround && hook.stuckPos().getZ() == 8 && hook.velocity < 0.01 && Math.abs(hook.getZ() - 7.95D) < 0.01
					&& !hook.isRemoved(), "hook sticks in the wall 0.05 before the face (" + hook.getZ() + ")");
			// pull with LMB held
			player.setDeltaMovement(Vec3.ZERO);
			ItemGunBaseNT.setPrimary(thrower, 0, true);
			thrower.inventoryTick(level, player, 0, true);
			Vec3 pull = player.getDeltaMovement();
			ItemGunBaseNT.setPrimary(thrower, 0, false);
			check(problems, cases, pull.z > 0.09 && pull.y > 0.02 && player.fallDistance == 0, "LMB pulls the player towards the hook (+0.04 up) " + pull);
			// RMB brakes
			player.setDeltaMovement(new Vec3(0, 0, 1));
			ItemGunBaseNT.setSecondary(thrower, 0, true);
			thrower.inventoryTick(level, player, 0, true);
			ItemGunBaseNT.setSecondary(thrower, 0, false);
			check(problems, cases, Math.abs(player.getDeltaMovement().z - 0.5) < 1E-6, "RMB halves the speed on the rope");
			// rope: moving away is cut back to the rope length
			player.setDeltaMovement(new Vec3(0, 0, -1));
			thrower.inventoryTick(level, player, 0, true);
			check(problems, cases, player.getDeltaMovement().z > -0.5, "rope limits moving away from the hook " + player.getDeltaMovement());
			// reload state clears lasthook
			ItemGunBaseNT.setState(thrower, 0, GunState.RELOADING);
			ItemGunBaseNT.setTimer(thrower, 0, 100);
			thrower.inventoryTick(level, player, 0, true);
			check(problems, cases, ItemGunChargeThrower.getLastHook(thrower) == -1, "RELOADING clears lasthook");
			ItemGunBaseNT.setState(thrower, 0, GunState.IDLE);
			ItemGunBaseNT.setTimer(thrower, 0, 0);
			// not held clears lasthook
			ItemGunChargeThrower.setLastHook(thrower, hook.getId());
			thrower.inventoryTick(level, player, 0, false);
			check(problems, cases, ItemGunChargeThrower.getLastHook(thrower) == -1, "not held clears lasthook");
			// breaking the block releases the hook
			level.setBlockAndUpdate(hook.stuckPos(), Blocks.AIR.defaultBlockState());
			hook.tickCount++;
			hook.tick();
			check(problems, cases, !hook.inGround, "hook falls out when its block is gone");
		}
		player.setDeltaMovement(Vec3.ZERO);
		for(int dx = -2; dx <= 2; dx++) for(int dy = -2; dy <= 2; dy++) level.setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.AIR.defaultBlockState());
		clearArea(level);

		/// Mortar (VNT 5, bulkie 60/8) ///
		BlockPos m0 = new BlockPos(X0 - 20, 280, 30);
		buildTarget(level, m0);
		LegacyBulletEntity mortar = round(level, player, StandardAmmoType.CT_MORTAR, m0.above(3));
		mortar.setDamage(10F * 2.5F);
		Pig target = pig(level, Vec3.atCenterOf(m0.above(3)).add(1.5, 0, 0));
		float hp = target != null ? target.getHealth() : 0F;
		boolean consumed = XFactoryTool.LAMBDA_MORTAR.onImpact(mortar, new BlockHitResult(Vec3.atCenterOf(m0.above(2)).add(0, 0.5, 0), Direction.UP, m0.above(2), false));
		int stone = count(level, m0, Blocks.STONE), obsidian = count(level, m0, Blocks.OBSIDIAN);
		check(problems, cases, consumed && stone < 50 && obsidian == 81 && target != null && target.getHealth() < hp, "ct_mortar explosion breaks stone, stops at obsidian, hurts (" + stone + "/" + obsidian + ")");
		if(target != null) target.discard();
		clearDrops(level);
		/// Mortar charge (VNT 15, no drops, slag debris) ///
		buildTarget(level, m0);
		LegacyBulletEntity charge = round(level, player, StandardAmmoType.CT_MORTAR_CHARGE, m0.above(3));
		charge.setDamage(10F * 5F);
		consumed = XFactoryTool.LAMBDA_MORTAR_CHARGE.onImpact(charge, new BlockHitResult(Vec3.atCenterOf(m0.above(2)).add(0, 0.5, 0), Direction.UP, m0.above(2), false));
		stone = count(level, m0, Blocks.STONE);
		int slag = count(level, m0, HbmBlocks.BLOCK_SLAG.get());
		int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(m0).inflate(20)).size();
		check(problems, cases, consumed && stone < 10 && slag > 0 && drops == 0, "ct_mortar_charge explosion without drops leaves slag debris (" + stone + "/" + slag + "/" + drops + ")");
		check(problems, cases, !XFactoryTool.LAMBDA_MORTAR.onImpact(round(level, player, StandardAmmoType.CT_MORTAR, m0.above(30)), new EntityHitResult(player)), "mortar ignores the thrower for 3 ticks");
		wipe(level, m0);
		clearArea(level);

		/// Drill: fuel 4000, delay 20, dry 30, reach 5, AoE 1, 10 mB per use ///
		ItemStack drill = equip(player, dr);
		cfg = ((ItemGunBaseNT) dr).getConfig(drill, 0);
		rec = cfg.getReceivers(drill)[0];
		mag = rec.getMagazine(drill);
		check(problems, cases, mag instanceof MagazineLiquidEngine eng && eng.capacity == 4_000 && eng.acceptedTypes.length == 4 && eng.acceptedTypes[0].name().equals("gasoline")
				&& eng.acceptedTypes[1].name().equals("gasoline_leaded") && eng.acceptedTypes[2].name().equals("coalgas") && eng.acceptedTypes[3].name().equals("coalgas_leaded")
				&& cfg.getDurability(drill) == 3_000 && cfg.getInspectDuration(drill) == 55 && !cfg.getHideCrosshair(drill) && cfg.getCrosshair(drill) == Crosshair.L_CIRCUMFLEX
				&& rec.getBaseDamage(drill) == 10F && rec.getDelayAfterFire(drill) == 20 && rec.getDelayAfterDryFire(drill) == 30 && rec.getRefireOnHold(drill)
				&& cfg.getOrchestra(drill) == OrchestrasF3.ORCHESTRA_DRILL && cfg.getAnims(drill).apply(drill, GunAnimation.CYCLE) != null
				&& cfg.getAnims(drill).apply(drill, GunAnimation.CYCLE_DRY) != null && XFactoryDrill.getModdableReach(drill, 5D) == 5D, "drill values");
		IFluidHandlerItem handler = drill.getCapability(Capabilities.FluidHandler.ITEM);
		int filled = handler == null ? -1 : handler.fill(HbmFluids.toNeoStack(HbmFluids.byName("gasoline").orElseThrow(), 1_000), IFluidHandler.FluidAction.EXECUTE);
		int refused = handler == null ? -1 : handler.fill(HbmFluids.toNeoStack(HbmFluids.byName("water").orElseThrow(), 1_000), IFluidHandler.FluidAction.EXECUTE);
		check(problems, cases, filled == 50 && refused == 0 && mag.getAmount(drill, null) == 50, "drill takes 50 mB of gasoline per fill, no water (" + filled + "/" + refused + ")");
		for(int i = 0; i < 200 && handler != null; i++) handler.fill(HbmFluids.toNeoStack(HbmFluids.byName("coalgas_leaded").orElseThrow(), 1_000), IFluidHandler.FluidAction.EXECUTE);
		check(problems, cases, mag.getAmount(drill, null) == 4_000 && mag.reportAmmoStateForHUD(drill, player).equals("4000/4000mB")
				&& mag.getIconForHUD(drill, player).is(HbmItems.FLUID_ICON.get()), "drill fills to 4000 mB, HUD 4000/4000mB with a fluid icon");
		check(problems, cases, ((ItemGunDrill) dr).isCorrectToolForDrops(drill, Blocks.IRON_ORE.defaultBlockState()) && !((ItemGunDrill) dr).isCorrectToolForDrops(drill, Blocks.OBSIDIAN.defaultBlockState()),
				"drill harvest level IRON");
		BlockPos face = BlockPos.containing(X0 + 0.5, player.getEyeY(), 3.5);
		for(int dx = -1; dx <= 1; dx++) for(int dy = -1; dy <= 1; dy++) for(int dz = 0; dz <= 2; dz++) level.setBlockAndUpdate(face.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
		press(player, drill, GunKeybind.GUN_PRIMARY);
		release(player, drill, GunKeybind.GUN_PRIMARY);
		int left = 0;
		for(int dx = -1; dx <= 1; dx++) for(int dy = -1; dy <= 1; dy++) for(int dz = 0; dz <= 2; dz++) if(level.getBlockState(face.offset(dx, dy, dz)).is(Blocks.STONE)) left++;
		check(problems, cases, left == 27 - 18 && level.getBlockState(face).isAir() && level.getBlockState(face.offset(1, 1, 1)).isAir() && level.getBlockState(face.offset(0, 0, 2)).is(Blocks.STONE)
				&& mag.getAmount(drill, null) == 3_990 && ItemGunBaseNT.getWear(drill, 0) == 0F && ItemGunBaseNT.getTimer(drill, 0) == 20, "drill breaks the 3x3x3 AoE, uses 10 mB, no wear (" + left + ")");
		for(int dx = -1; dx <= 1; dx++) for(int dy = -1; dy <= 1; dy++) for(int dz = 0; dz <= 2; dz++) level.setBlockAndUpdate(face.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
		tick(player, drill, 21);
		level.setBlockAndUpdate(face, Blocks.BEDROCK.defaultBlockState());
		level.setBlockAndUpdate(face.above(), HbmBlocks.STONE_KEYHOLE.get().defaultBlockState());
		press(player, drill, GunKeybind.GUN_PRIMARY);
		release(player, drill, GunKeybind.GUN_PRIMARY);
		check(problems, cases, level.getBlockState(face).is(Blocks.BEDROCK) && level.getBlockState(face.above()).is(HbmBlocks.STONE_KEYHOLE.get()) && mag.getAmount(drill, null) == 3_980
				&& !XFactoryDrill.didPlink, "drill plinks on bedrock / keyhole stone and still burns fuel");
		level.setBlockAndUpdate(face, Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(face.above(), Blocks.AIR.defaultBlockState());
		tick(player, drill, 21);
		Pig victim = pig(level, new Vec3(X0 + 0.5, player.getEyeY() - 0.45D, 2.5));
		if(victim != null) {
			float before = victim.getHealth();
			press(player, drill, GunKeybind.GUN_PRIMARY);
			release(player, drill, GunKeybind.GUN_PRIMARY);
			check(problems, cases, Math.abs(before - victim.getHealth() - 10F) < 0.01F, "drill hits entities for 10 (" + (before - victim.getHealth()) + ")");
			victim.discard();
		}
		mag.setAmount(drill, 5);
		tick(player, drill, 31);
		press(player, drill, GunKeybind.GUN_PRIMARY);
		release(player, drill, GunKeybind.GUN_PRIMARY);
		check(problems, cases, mag.getAmount(drill, null) == 0, "drill burns the rest below 10 mB");
		tick(player, drill, 21);
		press(player, drill, GunKeybind.GUN_PRIMARY);
		release(player, drill, GunKeybind.GUN_PRIMARY);
		check(problems, cases, ItemGunBaseNT.getLastAnim(drill, 0) == GunAnimation.CYCLE_DRY && ItemGunBaseNT.getTimer(drill, 0) == 30, "empty drill dry fires (30)");
		player.getInventory().clearContent();
		clearArea(level);
		return cases[0];
	}

	private static boolean bullet(BulletConfig c, StandardAmmoType type, int reload, int life, double vel, double grav, float spread) {
		return c.ammo == type && c.ammoReloadCount == reload && type.projectileLifetime() == life && type.projectileSpeed() == vel && type.projectileGravity() == grav
				&& type.projectileSpread() == spread && c.wear == 1F && type.projectileCount() == 1;
	}

	private static LegacyBulletEntity round(ServerLevel level, FakePlayer player, StandardAmmoType type, BlockPos pos) {
		LegacyBulletEntity b = new LegacyBulletEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new Vec3(0, -1, 0), type, 1F, player, 0F);
		level.addFreshEntity(b);
		return b;
	}

	/** 9x9 obsidian floor, 5x5x2 stone on top. */
	private static void buildTarget(ServerLevel level, BlockPos m0) {
		wipe(level, m0);
		for(int dx = -4; dx <= 4; dx++) for(int dz = -4; dz <= 4; dz++) level.setBlock(m0.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState(), 2);
		for(int dx = -2; dx <= 2; dx++) for(int dz = -2; dz <= 2; dz++) for(int dy = 1; dy <= 2; dy++) level.setBlock(m0.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 2);
	}

	private static void wipe(ServerLevel level, BlockPos m0) {
		for(BlockPos p : BlockPos.betweenClosed(m0.offset(-18, -18, -18), m0.offset(18, 18, 18))) if(!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		clearDrops(level);
	}

	private static int count(ServerLevel level, BlockPos m0, net.minecraft.world.level.block.Block block) {
		int n = 0;
		for(BlockPos p : BlockPos.betweenClosed(m0.offset(-18, -18, -18), m0.offset(18, 18, 18))) if(level.getBlockState(p).is(block)) n++;
		return n;
	}

	private static void clearDrops(ServerLevel level) {
		for(ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, AREA)) e.discard();
	}

	private static void checkRecipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, ItemStack result, String variant, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		boolean ok = recipe instanceof ShapedRecipe s && s.matches(input, level);
		if(ok) {
			ItemStack out = ((ShapedRecipe) recipe).assemble(input, server.registryAccess());
			ok = out.is(result.getItem()) && out.getCount() == 1 && (variant == null || ((LegacyVariantItem) out.getItem()).variant(out).id().equals(variant));
		}
		check(problems, cases, ok, id + " recipe");
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
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(Items.BARRIER));
	}

	private static List<LegacyBulletEntity> bullets(ServerLevel level) {
		return level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired());
	}

	private static void clearArea(ServerLevel level) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, AREA)) bullet.remove(Entity.RemovalReason.KILLED);
		clearDrops(level);
	}

	private static int indexOf(List<ItemStack> entries, Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("gun batch F3: " + message);
	}
}
