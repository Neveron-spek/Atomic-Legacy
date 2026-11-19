package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBuildingEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.MineRubbleEntity;
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
import com.atomiclegacy.items.weapon.sedna.factory.Orchestras;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasB;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory50;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
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
 * Dedicated-server checks for weapons batch D2 (gun_g3_zebra from XFactory556mm with the r556_inc_* rounds,
 * gun_amat_subtlety / gun_amat_penance from XFactory50 with the ammo_secret bmg50_equestrian / bmg50_black
 * rounds and EntityBuilding) against the pinned 1.7.10 XFactory556mm / XFactory50 / WeaponRecipes /
 * PedestalRecipes values.
 */
final class GunBatchD2Diagnostics {

	private GunBatchD2Diagnostics() {
	}

	/** Attributes.MAX_HEALTH is capped at 1024 in 1.21. */
	private static final float BASE_HP = 1024F;

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 380, 16);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo ///
		Object[][] guns = {
				{ HbmItems.GUN_G3_ZEBRA.get(), WeaponQuality.B_SIDE, StandardAmmoType.R556_JHP, 30 },
				{ HbmItems.GUN_AMAT_SUBTLETY.get(), WeaponQuality.LEGENDARY, StandardAmmoType.BMG50_JHP, 7 },
				{ HbmItems.GUN_AMAT_PENANCE.get(), WeaponQuality.LEGENDARY, StandardAmmoType.BMG50_JHP, 7 },
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

		/// Creative tab: B_SIDE / LEGENDARY guns get no tab ///
		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		check(problems, cases, indexOf(tab, HbmItems.GUN_G3.get()) >= 0 && indexOf(tab, HbmItems.GUN_AMAT.get()) >= 0, "gun_g3 / gun_amat still in the weapon tab");
		check(problems, cases, indexOf(tab, HbmItems.GUN_G3_ZEBRA.get()) < 0 && indexOf(tab, HbmItems.GUN_AMAT_SUBTLETY.get()) < 0 && indexOf(tab, HbmItems.GUN_AMAT_PENANCE.get()) < 0, "D2 guns hidden");

		/// Rounds ///
		StandardAmmoType[][] inc = {
				{ StandardAmmoType.R556_INC_SP, StandardAmmoType.R556_SP }, { StandardAmmoType.R556_INC_FMJ, StandardAmmoType.R556_FMJ },
				{ StandardAmmoType.R556_INC_JHP, StandardAmmoType.R556_JHP }, { StandardAmmoType.R556_INC_AP, StandardAmmoType.R556_AP } };
		for(StandardAmmoType[] p : inc) {
			StandardAmmoType t = p[0], src = p[1];
			check(problems, cases, t.damageMultiplier() == src.damageMultiplier() && t.penetrates() == src.penetrates() && t.damageFalloffByPenetration() == src.damageFalloffByPenetration()
					&& t.armorThresholdNegation() == src.armorThresholdNegation() && t.armorPiercingPercent() == src.armorPiercingPercent() && t.headshotMultiplier() == src.headshotMultiplier()
					&& t.knockbackMultiplier() == src.knockbackMultiplier() && t.projectileSpeed() == src.projectileSpeed() && t.projectileLifetime() == src.projectileLifetime()
					&& t.projectileGravity() == src.projectileGravity() && t.ricochetAngle() == src.ricochetAngle() && t.maxRicochetCount() == src.maxRicochetCount()
					&& t.projectileCount() == 1 && t.projectileSpread() == 0F && !t.spectral() && t.onImpact() == XFactory556mm.INCENDIARY && src.onImpact() == null, t.id() + " clone of " + src.id());
		}
		check(problems, cases, round(StandardAmmoType.BMG50_BLACK, 1.5F, true, false, 30F, 0.35F, 3F) && StandardAmmoType.BMG50_BLACK.spectral() && StandardAmmoType.BMG50_BLACK.onImpact() == null
				&& XFactory50.bmg50_black.wear == 5F, "bmg50_black values");
		check(problems, cases, round(StandardAmmoType.BMG50_EQUESTRIAN, 0F, false, true, 0F, 0F, 1.25F) && !StandardAmmoType.BMG50_EQUESTRIAN.spectral()
				&& StandardAmmoType.BMG50_EQUESTRIAN.onImpact() == XFactory50.LAMBDA_BUILDING && XFactory50.bmg50_equestrian.wear == 1F, "bmg50_equestrian values");
		check(problems, cases, XFactory556mm.r556_inc_sp.casingEffect == XFactory556mm.CASING_R556 && XFactory556mm.r556_inc_ap.casingEffect == XFactory556mm.CASING_R556_STEEL
				&& XFactory50.bmg50_black.casingEffect == XFactory50.CASING_BMG50_EQUESTRIAN && XFactory50.bmg50_equestrian.casingEffect == XFactory50.CASING_BMG50_EQUESTRIAN
				&& XFactory50.CASING_BMG50_EQUESTRIAN == 31, "casings");
		check(problems, cases, XFactory556mm.r556_inc_sp.matches(stack(StandardAmmoType.R556_SP, 1)) && !XFactory556mm.r556_inc_sp.matches(stack(StandardAmmoType.R556_FMJ, 1))
				&& XFactory556mm.r556_inc_ap.matches(stack(StandardAmmoType.R556_AP, 1)) && XFactory556mm.r556_inc_jhp.toStack().is(HbmItems.AMMO_STANDARD.get())
				&& StandardAmmoItem.standardType(XFactory556mm.r556_inc_jhp.toStack()) == StandardAmmoType.R556_JHP
				&& BulletConfig.configs.indexOf(XFactory556mm.r556_inc_sp) != BulletConfig.configs.indexOf(XFactory556mm.r556_sp), "r556_inc matching / own index");
		ItemStack black = LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "bmg50_black");
		ItemStack equestrian = LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "bmg50_equestrian");
		check(problems, cases, XFactory50.bmg50_black.matches(black) && !XFactory50.bmg50_black.matches(equestrian) && XFactory50.bmg50_equestrian.matches(equestrian)
				&& !XFactory50.bmg50_equestrian.matches(stack(StandardAmmoType.BMG50_SP, 1)) && !XFactory50.bmg50_sp.matches(black), "ammo_secret bmg50 matching");

		/// Config values, sounds, damage type ///
		check(problems, cases, HbmConfig.zebraDamage() == 7.5F && HbmConfig.subtletyDamage() == 50F && HbmConfig.penanceDamage() == 45F, "config damage defaults");
		check(problems, cases, HbmSoundEvents.GUN_RIFLE_SILENCER.getId().equals(AtomicLegacy.id("weapon.fire.silenced"))
				&& HbmSoundEvents.GUN_AMAT_SILENCER.getId().equals(AtomicLegacy.id("weapon.silencershoot")), "silencer sound ids");
		var building = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(LegacyBuildingEntity.BUILDING);
		check(problems, cases, building.isPresent() && building.get().is(DamageTypeTags.BYPASSES_ARMOR) && building.get().is(DamageTypeTags.BYPASSES_EFFECTS)
				&& building.get().is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && building.get().value().msgId().equals("building"), "damage type building");

		/// Recipes ///
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id("gun_g3_zebra")).map(r -> r.value()).orElse(null);
		ItemStack E = ItemStack.EMPTY;
		ItemStack mech = item("part_mechanism_saturnite");
		var input = CraftingInput.of(3, 3, List.of(E, mech, E, mech, new ItemStack(HbmItems.GUN_G3.get()), mech, E, mech, E));
		check(problems, cases, recipe instanceof ShapedRecipe s && s.getWidth() == 3 && s.getHeight() == 3 && s.matches(input, level)
				&& s.assemble(input, server.registryAccess()).is(HbmItems.GUN_G3_ZEBRA.get()) && s.assemble(input, server.registryAccess()).getCount() == 1, "gun_g3_zebra recipe");
		var wrong = CraftingInput.of(3, 3, List.of(E, mech, E, mech, new ItemStack(HbmItems.GUN_STG77.get()), mech, E, mech, E));
		check(problems, cases, recipe instanceof ShapedRecipe s && !s.matches(wrong, level), "gun_g3_zebra recipe needs gun_g3");
		check(problems, cases, pedestal("gun_amat_subtlety") && pedestal("gun_amat_penance"), "pedestal recipes (subtlety and penance present)"); // [weapon mods G2: penance recipe registered]

		/// Gun values ///
		ItemStack zebra = new ItemStack(HbmItems.GUN_G3_ZEBRA.get());
		ItemGunBaseNT zebraGun = (ItemGunBaseNT) zebra.getItem();
		GunConfig cfg = zebraGun.getConfig(zebra, 0);
		Receiver rec = cfg.getReceivers(zebra)[0];
		IMagazine<?> mag = rec.getMagazine(zebra);
		check(problems, cases, cfg.getDurability(zebra) == 6_000 && cfg.getDrawDuration(zebra) == 10 && cfg.getInspectDuration(zebra) == 33 && cfg.getCrosshair(zebra) == Crosshair.CIRCLE
				&& XFactory556mm.scope.equals(cfg.getScopeTexture(zebra)) && !cfg.hasThermalSights(zebra) && cfg.getOrchestra(zebra) == Orchestras.ORCHESTRA_G3
				&& cfg.getSmokeHandler(zebra) == XFactory556mm.LAMBDA_SMOKE && cfg.getAnims(zebra) == XFactory556mm.LAMBDA_G3_ANIMS && cfg.getPressSecondary(zebra) == Lego.LAMBDA_STANDARD_CLICK_SECONDARY
				&& rec.getBaseDamage(zebra) == 7.5F && rec.getDelayAfterFire(zebra) == 2 && rec.getRefireOnHold(zebra) && rec.getDelayAfterDryFire(zebra) == 15 && rec.getHipfireSpread(zebra) == 0.01F
				&& rec.getReloadBeginDuration(zebra) == 50 && rec.getJamDuration(zebra) == 47 && rec.getFireSound(zebra).get() == HbmSoundEvents.GUN_RIFLE_SILENCER.get()
				&& rec.getFireVolume(zebra) == 1F && rec.getFirePitch(zebra) == 1F && mag instanceof MagazineFullReload && mag.getCapacity(zebra) == 30
				&& ((MagazineFullReload) mag).acceptedBullets.equals(List.of(XFactory556mm.r556_inc_sp, XFactory556mm.r556_inc_fmj, XFactory556mm.r556_inc_jhp, XFactory556mm.r556_inc_ap))
				&& rec.getProjectileOffset(zebra).equals(new Vec3(1, -0.0625 * 2.5, -0.25D)), "gun_g3_zebra values");
		check(problems, cases, zebraGun.getName(zebra).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("item.atomiclegacy.gun_g3_zebra"), "gun_g3_zebra name");

		ItemStack subtlety = new ItemStack(HbmItems.GUN_AMAT_SUBTLETY.get());
		cfg = ((ItemGunBaseNT) subtlety.getItem()).getConfig(subtlety, 0);
		rec = cfg.getReceivers(subtlety)[0];
		check(problems, cases, cfg.getDurability(subtlety) == 1_000 && cfg.getDrawDuration(subtlety) == 20 && cfg.getInspectDuration(subtlety) == 50 && XFactory50.scope.equals(cfg.getScopeTexture(subtlety))
				&& !cfg.hasThermalSights(subtlety) && cfg.getOrchestra(subtlety) == OrchestrasB.ORCHESTRA_AMAT && rec.getBaseDamage(subtlety) == 50F && rec.getDelayAfterFire(subtlety) == 25
				&& rec.getDelayAfterDryFire(subtlety) == 25 && rec.getHipfireSpread(subtlety) == 0.05F && rec.getReloadBeginDuration(subtlety) == 51 && rec.getJamDuration(subtlety) == 43
				&& rec.getFireSound(subtlety).get() == HbmSoundEvents.GUN_AMAT_FIRE.get() && rec.getMagazine(subtlety).getCapacity(subtlety) == 7
				&& ((MagazineFullReload) rec.getMagazine(subtlety)).acceptedBullets.equals(List.of(XFactory50.bmg50_equestrian, XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp,
						XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_sm, XFactory50.bmg50_he))
				&& rec.getProjectileOffset(subtlety).equals(new Vec3(1, -0.0625 * 1.5, -0.25D)), "gun_amat_subtlety values");

		ItemStack penance = new ItemStack(HbmItems.GUN_AMAT_PENANCE.get());
		cfg = ((ItemGunBaseNT) penance.getItem()).getConfig(penance, 0);
		rec = cfg.getReceivers(penance)[0];
		check(problems, cases, cfg.getDurability(penance) == 5_000 && cfg.getDrawDuration(penance) == 20 && cfg.getInspectDuration(penance) == 50 && XFactory50.scope_thermal.equals(cfg.getScopeTexture(penance))
				&& cfg.hasThermalSights(penance) && cfg.getOrchestra(penance) == OrchestrasB.ORCHESTRA_AMAT && rec.getBaseDamage(penance) == 45F && rec.getDelayAfterFire(penance) == 25
				&& rec.getHipfireSpread(penance) == 0F && rec.getReloadBeginDuration(penance) == 51 && rec.getJamDuration(penance) == 43
				&& rec.getFireSound(penance).get() == HbmSoundEvents.GUN_AMAT_SILENCER.get() && rec.getMagazine(penance).getCapacity(penance) == 7
				&& ((MagazineFullReload) rec.getMagazine(penance)).acceptedBullets.equals(List.of(XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap,
						XFactory50.bmg50_du, XFactory50.bmg50_sm, XFactory50.bmg50_he, XFactory50.bmg50_black)), "gun_amat_penance values");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discardBullets(level);

		// Zebra: reload 50 loads the incendiary clone of the carried .556, automatic fire, incendiary hit
		ItemStack zebraHeld = equip(player, HbmItems.GUN_G3_ZEBRA.get(), stack(StandardAmmoType.R556_SP, 40));
		reloadCycle(problems, cases, player, zebraHeld, 50, 30, "zebra");
		IMagazine<?> zmag = ((ItemGunBaseNT) zebraHeld.getItem()).getConfig(zebraHeld, 0).getReceivers(zebraHeld)[0].getMagazine(zebraHeld);
		check(problems, cases, zmag.getType(zebraHeld, null) == XFactory556mm.r556_inc_sp && countAmmo(player, StandardAmmoType.R556_SP) == 10
				&& zmag.getIconForHUD(zebraHeld, player).is(HbmItems.AMMO_STANDARD.get()), "zebra loads r556_inc_sp from r556_sp");
		Pig pig = pig(level, 0.5D, 301.0D, 3.0D);
		press(player, zebraHeld, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.R556_INC_SP && Math.abs(bullets.get(0).baseDamage() - 7.5F) < 1.0E-4F
				&& ItemGunBaseNT.getTimer(zebraHeld, 0) == 2 && zmag.getAmount(zebraHeld, null) == 29, "zebra shot");
		tick(player, zebraHeld, 2);
		check(problems, cases, bullets(level).size() == 2 && zmag.getAmount(zebraHeld, null) == 28, "zebra automatic refire");
		release(player, zebraHeld, GunKeybind.GUN_PRIMARY);
		discardBullets(level);
		if(pig != null) {
			shoot(player, XFactory556mm.r556_inc_sp, 7.5F);
			check(problems, cases, HbmLivingHazards.get(pig).getPhosphorus() >= 300 && pig.getHealth() < BASE_HP, "incendiary hit phosphorus=" + HbmLivingHazards.get(pig).getPhosphorus() + " hp=" + pig.getHealth());
			pig.discard();
		}
		Pig pig2 = pig(level, 0.5D, 301.0D, 3.0D);
		if(pig2 != null) {
			shoot(player, XFactory556mm.r556_sp, 5F);
			check(problems, cases, HbmLivingHazards.get(pig2).getPhosphorus() == 0 && pig2.getHealth() < BASE_HP, "plain r556_sp has no phosphorus");
			pig2.discard();
		}
		discardBullets(level);

		// Penance: loads ammo_secret bmg50_black; the spectral round passes through a wall
		ItemStack penanceHeld = equip(player, HbmItems.GUN_AMAT_PENANCE.get(), black.copyWithCount(7));
		reloadCycle(problems, cases, player, penanceHeld, 51, 7, "penance");
		IMagazine<?> pmag = ((ItemGunBaseNT) penanceHeld.getItem()).getConfig(penanceHeld, 0).getReceivers(penanceHeld)[0].getMagazine(penanceHeld);
		check(problems, cases, pmag.getType(penanceHeld, null) == XFactory50.bmg50_black && player.getInventory().countItem(HbmItems.AMMO_SECRET.get()) == 0
				&& pmag.getIconForHUD(penanceHeld, player).is(HbmItems.AMMO_SECRET.get()), "penance bmg50_black reload");
		press(player, penanceHeld, GunKeybind.GUN_PRIMARY);
		release(player, penanceHeld, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.BMG50_BLACK && Math.abs(bullets.get(0).baseDamage() - 45F * 1.5F) < 1.0E-4F
				&& ItemGunBaseNT.getTimer(penanceHeld, 0) == 25 && Math.abs(ItemGunBaseNT.getWear(penanceHeld, 0) - 5F) < 1.0E-4F
				&& pmag.getCasing(penanceHeld, null) == XFactory50.CASING_BMG50_EQUESTRIAN, "penance black shot (wear 5) bullets=" + bullets.size() + " timer=" + ItemGunBaseNT.getTimer(penanceHeld, 0) + " wear=" + ItemGunBaseNT.getWear(penanceHeld, 0) + " casing=" + pmag.getCasing(penanceHeld, null) + " dmg=" + (bullets.isEmpty() ? -1 : bullets.get(0).baseDamage()));
		discardBullets(level);
		BlockPos wall = new BlockPos(0, 301, 4);
		level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		Pig behind = pig(level, 0.5D, 301.0D, 8.0D);
		if(behind != null) {
			shoot(player, XFactory50.bmg50_black, 45F);
			check(problems, cases, behind.getHealth() < BASE_HP, "bmg50_black spectral through wall hp=" + behind.getHealth());
			behind.discard();
		}
		Pig behind2 = pig(level, 0.5D, 301.0D, 8.0D);
		if(behind2 != null) {
			shoot(player, XFactory50.bmg50_sp, 45F);
			check(problems, cases, behind2.getHealth() == BASE_HP, "bmg50_sp stopped by the wall hp=" + behind2.getHealth());
			behind2.discard();
		}
		discardBullets(level);

		// Subtlety: bmg50_equestrian first; the round summons EntityBuilding 50 blocks above the hit
		ItemStack subtletyHeld = equip(player, HbmItems.GUN_AMAT_SUBTLETY.get(), stack(StandardAmmoType.BMG50_JHP, 7), equestrian.copyWithCount(7));
		reloadCycle(problems, cases, player, subtletyHeld, 51, 7, "subtlety");
		IMagazine<?> smag = ((ItemGunBaseNT) subtletyHeld.getItem()).getConfig(subtletyHeld, 0).getReceivers(subtletyHeld)[0].getMagazine(subtletyHeld);
		check(problems, cases, smag.getType(subtletyHeld, null) == XFactory50.bmg50_jhp, "subtlety loads the first inventory match");
		ItemStack subtletyHeld2 = equip(player, HbmItems.GUN_AMAT_SUBTLETY.get(), equestrian.copyWithCount(7));
		reloadCycle(problems, cases, player, subtletyHeld2, 51, 7, "subtlety equestrian");
		check(problems, cases, smag.getType(subtletyHeld2, null) == XFactory50.bmg50_equestrian, "subtlety equestrian reload");
		discardBullets(level);
		LegacyBulletEntity eq = shoot(player, XFactory50.bmg50_equestrian, 50F);
		List<LegacyBuildingEntity> buildings = level.getEntitiesOfClass(LegacyBuildingEntity.class, new AABB(-4, 340, 0, 4, 360, 8));
		check(problems, cases, buildings.size() == 1 && Math.abs(buildings.get(0).getY() - 351.62D) < 0.5D && (eq.isRemoved() || eq.isRetired()), "equestrian building spawn " + buildings.size());
		buildings.forEach(Entity::discard);
		level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
		discardBullets(level);

		// EntityBuilding: falls 0.03/t, on the first non-air block 1000 building damage within 8 blocks and 250 brick rubble
		BlockPos target = new BlockPos(64, 300, 64);
		level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
		Pig victim = pig(level, 67.5D, 301.0D, 64.5D);
		LegacyBuildingEntity house = new LegacyBuildingEntity(level, new Vec3(64.5D, 306.0D, 64.5D));
		level.addFreshEntity(house);
		int ticks = 0;
		while(!house.isRemoved() && ticks < 60) { house.tick(); ticks++; }
		AABB around = new AABB(56, 290, 56, 72, 320, 72);
		List<MineRubbleEntity> rubble = level.getEntitiesOfClass(MineRubbleEntity.class, around);
		check(problems, cases, house.isRemoved() && ticks > 15 && ticks < 25 && !level.getBlockState(target).isAir()
				&& rubble.size() == 250 && rubble.stream().allMatch(r -> r.blockState().is(Blocks.BRICKS) && r.getDeltaMovement().y >= 0D), "building fall/impact ticks=" + ticks + " rubble=" + rubble.size());
		if(victim != null) {
			check(problems, cases, Math.abs(BASE_HP - victim.getHealth() - 1000F) < 0.01F && victim.getLastDamageSource() != null
					&& victim.getLastDamageSource().is(LegacyBuildingEntity.BUILDING), "building damage hp=" + victim.getHealth());
			victim.discard();
		}
		rubble.forEach(Entity::discard);
		level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());

		discardBullets(level);
		player.getInventory().clearContent();

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_D2_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static Pig pig(ServerLevel level, double x, double y, double z) {
		Pig pig = EntityType.PIG.create(level);
		if(pig == null) return null;
		pig.setNoAi(true);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HP);
		pig.setHealth(BASE_HP);
		pig.setPos(x, y, z);
		level.addFreshEntity(pig);
		return pig;
	}

	private static boolean round(StandardAmmoType t, float dmg, boolean pen, boolean falloff, float dt, float ap, float head) {
		return t.damageMultiplier() == dmg && t.penetrates() == pen && t.damageFalloffByPenetration() == falloff && t.armorThresholdNegation() == dt
				&& t.armorPiercingPercent() == ap && t.headshotMultiplier() == head && t.knockbackMultiplier() == 0.1F && t.projectileSpeed() == 10D
				&& t.projectileLifetime() == 30 && t.projectileGravity() == 0D && t.selfDamageDelay() == 2 && t.projectileCount() == 1 && t.projectileSpread() == 0F
				&& t.ricochetAngle() == 5F && t.maxRicochetCount() == 2 && t.family() == StandardAmmoItem.AmmoFamily.LEGACY_BULLET;
	}

	/** LegacyPedestalRecipes holds an available entry for the output. */
	private static boolean pedestal(String output) {
		try {
			var field = com.atomiclegacy.tileentity.LegacyPedestalRecipes.class.getDeclaredField("RECIPES");
			field.setAccessible(true);
			for(Object recipe : (List<?>) field.get(null)) {
				var result = recipe.getClass().getDeclaredMethod("result");
				var available = recipe.getClass().getDeclaredMethod("available");
				result.setAccessible(true);
				available.setAccessible(true);
				ItemStack stack = (ItemStack) result.invoke(recipe);
				if(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(AtomicLegacy.id(output))) return (boolean) available.invoke(recipe);
			}
		} catch(ReflectiveOperationException e) {
			return false;
		}
		return false;
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
		tick(player, gun, 25);
		press(player, gun, GunKeybind.RELOAD);
		release(player, gun, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(gun, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(gun, 0) == duration && ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.RELOAD, name + " reload start");
		tick(player, gun, duration - 1);
		check(problems, cases, mag.getAmount(gun, null) == 0, name + " reload finished early");
		tick(player, gun, 2);
		check(problems, cases, mag.getAmount(gun, null) == expected && ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, name + " reload result " + mag.getAmount(gun, null));
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
		if(!ok) problems.add("gun batch D2: " + message);
	}
}
