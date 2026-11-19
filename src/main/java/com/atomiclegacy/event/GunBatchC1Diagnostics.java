package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory357;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryBlackPowder;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.network.BlackPowderEffectPayload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.tileentity.LegacyDisplayStandBlockEntity;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.CreativeModeTab;
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
 * Dedicated-server checks for weapons batch C1 (gun_pepperbox, gun_light_revolver,
 * gun_light_revolver_atlas, gun_light_revolver_dani, the .357 and ball-and-powder rounds)
 * against the pinned 1.7.10 XFactory357 / XFactoryBlackPowder / WeaponRecipes / PedestalRecipes.
 */
final class GunBatchC1Diagnostics {

	private GunBatchC1Diagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 8);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo, tabs ///
		Object[][] guns = {
				{ HbmItems.GUN_PEPPERBOX.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.STONE, 12 },
				{ HbmItems.GUN_LIGHT_REVOLVER.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.M357_SP, 12 },
				{ HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get(), WeaponQuality.B_SIDE, 1, StandardAmmoType.M357_JHP, 12 },
				{ HbmItems.GUN_LIGHT_REVOLVER_DANI.get(), WeaponQuality.LEGENDARY, 2, StandardAmmoType.M357_EXPRESS, 24 },
		};
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof ItemGunBaseNT, name + " is not an ItemGunBaseNT");
			if(!(item instanceof ItemGunBaseNT gun)) continue;
			check(problems, cases, new ItemStack(item).getMaxStackSize() == 1 && gun.quality == g[1] && gun.getConfigCount() == (int) g[2], name + " stack/quality/configs");
			ItemStack def = gun.getDefaultAmmo();
			check(problems, cases, StandardAmmoItem.standardType(def) == g[3] && def.getCount() == (int) g[4], name + " default ammo " + def);
		}

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		check(problems, cases, indexOf(tab, HbmItems.GUN_PEPPERBOX.get()) >= 0 && indexOf(tab, HbmItems.GUN_PEPPERBOX.get()) < indexOf(tab, HbmItems.GUN_LIGHT_REVOLVER.get())
				&& indexOf(tab, HbmItems.GUN_LIGHT_REVOLVER.get()) < indexOf(tab, HbmItems.AMMO_STANDARD.get()), "weapon tab: pepperbox, light_revolver before ammo");
		boolean hiddenEverywhere = true;
		for(CreativeModeTab t : BuiltInRegistries.CREATIVE_MODE_TAB) {
			if(t.getType() != CreativeModeTab.Type.CATEGORY) continue;
			for(ItemStack s : t.getDisplayItems()) if(s.is(HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get()) || s.is(HbmItems.GUN_LIGHT_REVOLVER_DANI.get())) hiddenEverywhere = false;
		}
		check(problems, cases, hiddenEverywhere, "atlas (B_SIDE) and dani (LEGENDARY) have no creative tab");

		/// Ammo values (BulletConfig) ///
		check(problems, cases, round(StandardAmmoType.M357_BP, 0.75F, false, true, 0F, 0F, 1.25F) && StandardAmmoType.M357_BP.blackPowder(), "m357_bp values");
		check(problems, cases, round(StandardAmmoType.M357_SP, 1F, false, true, 0F, 0F, 1.25F) && !StandardAmmoType.M357_SP.blackPowder(), "m357_sp values");
		check(problems, cases, round(StandardAmmoType.M357_FMJ, 0.8F, false, true, 2F, 0.1F, 1.25F), "m357_fmj values");
		check(problems, cases, round(StandardAmmoType.M357_JHP, 1.5F, false, true, 0F, -0.25F, 1.5F), "m357_jhp values");
		check(problems, cases, round(StandardAmmoType.M357_AP, 1.25F, true, false, 5F, 0.15F, 1.25F), "m357_ap values");
		check(problems, cases, round(StandardAmmoType.M357_EXPRESS, 1.5F, true, true, 2F, 0.1F, 1.25F) && XFactory357.m357_express.wear == 1.5F && XFactory357.m357_sp.wear == 1F, "m357_express values");
		for(StandardAmmoType t : new StandardAmmoType[] { StandardAmmoType.M357_BP, StandardAmmoType.M357_SP, StandardAmmoType.M357_FMJ, StandardAmmoType.M357_JHP, StandardAmmoType.M357_AP, StandardAmmoType.M357_EXPRESS })
			check(problems, cases, t.projectileSpread() == 0F && t.ricochetAngle() == 5F && t.maxRicochetCount() == 2 && t.projectileCount() == 1 && t.projectileCountMax() == 1, t.id() + " defaults");
		check(problems, cases, round(StandardAmmoType.STONE, 1F, false, true, 0F, 0F, 1F) && StandardAmmoType.STONE.projectileSpread() == 0.025F && StandardAmmoType.STONE.ricochetAngle() == 15F
				&& StandardAmmoType.STONE.maxRicochetCount() == 2 && StandardAmmoType.STONE.projectileCount() == 1 && StandardAmmoType.STONE.blackPowder(), "stone values");
		check(problems, cases, round(StandardAmmoType.STONE_AP, 1.5F, true, true, 0F, 0F, 1F) && StandardAmmoType.STONE_AP.projectileSpread() == 0.01F && StandardAmmoType.STONE_AP.ricochetAngle() == 5F
				&& StandardAmmoType.STONE_AP.blackPowder(), "stone_ap (flint) values");
		check(problems, cases, round(StandardAmmoType.STONE_IRON, 1.5F, true, false, 0F, 0F, 1F) && StandardAmmoType.STONE_IRON.projectileSpread() == 0F && StandardAmmoType.STONE_IRON.ricochetAngle() == 90F
				&& StandardAmmoType.STONE_IRON.maxRicochetCount() == 5 && StandardAmmoType.STONE_IRON.blackPowder(), "stone_iron values");
		check(problems, cases, round(StandardAmmoType.STONE_SHOT, 1F / 6F, false, true, 0F, 0F, 1F) && StandardAmmoType.STONE_SHOT.projectileSpread() == 0.1F && StandardAmmoType.STONE_SHOT.ricochetAngle() == 45F
				&& StandardAmmoType.STONE_SHOT.projectileCount() == 6 && StandardAmmoType.STONE_SHOT.projectileCountMax() == 6 && StandardAmmoType.STONE_SHOT.blackPowder(), "stone_shot values");
		check(problems, cases, XFactoryBlackPowder.shot.projectilesMin() == 6 && XFactoryBlackPowder.stone.spread() == 0.025F && XFactoryBlackPowder.stone.casingEffect == -1 && XFactory357.m357_ap.casingEffect == -1, "bullet config wrappers");

		/// Black powder effect packet ///
		BlackPowderEffectPayload sent = new BlackPowderEffectPayload(1, 2, 3, 4, 5, 6, 10, 0.25F, 0.5F, 10, 0.25F);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
		BlackPowderEffectPayload.STREAM_CODEC.encode(buf, sent);
		check(problems, cases, sent.equals(BlackPowderEffectPayload.STREAM_CODEC.decode(buf)) && buf.readableBytes() == 0, "black powder payload round trip");

		/// Config values and sounds ///
		check(problems, cases, HbmConfig.lightRevolverDamage() == 7.5F && HbmConfig.atlasDamage() == 12.5F && HbmConfig.daniCelestialDamage() == 15F
				&& HbmConfig.daniLunarDamage() == 15F && HbmConfig.pepperboxDamage() == 5F, "config damage defaults");
		check(problems, cases, HbmSoundEvents.GUN_POWDER_FIRE.get().getLocation().equals(AtomicLegacy.id("weapon.fire.blackpowder"))
				&& HbmSoundEvents.GUN_REVOLVER_SPIN.get().getLocation().equals(AtomicLegacy.id("weapon.reload.revolverspin")), "sound ids");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		var pepRecipe = recipes.byKey(AtomicLegacy.id("gun_pepperbox")).map(h -> h.value()).orElse(null);
		var pepInput = CraftingInput.of(3, 2, List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.OAK_PLANKS),
				ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.COPPER_INGOT)));
		check(problems, cases, pepRecipe instanceof ShapedRecipe s && s.matches(pepInput, level) && s.assemble(pepInput, server.registryAccess()).is(HbmItems.GUN_PEPPERBOX.get()), "gun_pepperbox recipe IIW/  C");
		var revRecipe = recipes.byKey(AtomicLegacy.id("gun_light_revolver")).map(h -> h.value()).orElse(null);
		var revInput = CraftingInput.of(3, 2, List.of(item("part_barrel_light_steel"), item("part_receiver_light_steel"), item("part_mechanism_gunmetal"),
				ItemStack.EMPTY, ItemStack.EMPTY, item("part_grip_wood")));
		check(problems, cases, revRecipe instanceof ShapedRecipe s && s.matches(revInput, level) && s.assemble(revInput, server.registryAccess()).is(HbmItems.GUN_LIGHT_REVOLVER.get()), "gun_light_revolver recipe BRM/  G");
		var atlasRecipe = recipes.byKey(AtomicLegacy.id("gun_light_revolver_atlas")).map(h -> h.value()).orElse(null);
		ItemStack m = item("part_mechanism_weaponsteel");
		var atlasInput = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, m, ItemStack.EMPTY, m, new ItemStack(HbmItems.GUN_LIGHT_REVOLVER.get()), m, ItemStack.EMPTY, m, ItemStack.EMPTY));
		check(problems, cases, atlasRecipe instanceof ShapedRecipe s && s.matches(atlasInput, level) && s.assemble(atlasInput, server.registryAccess()).is(HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get()), "gun_light_revolver_atlas recipe  M /MAM/ M ");
		check(problems, cases, !m.isEmpty() && !item("part_grip_wood").isEmpty(), "recipe parts exist");

		// PedestalRecipes: lead plates N/S, gold plates W/E, gun_light_revolver centre -> gun_light_revolver_dani
		checkPedestal(level, problems, cases);

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discardBullets(level);

		// Light revolver: dura 300, draw 4, inspect 23, dmg 7.5, delay 11, reload 55, jam 45, mag 6, semi-auto
		ItemStack rev = equip(player, HbmItems.GUN_LIGHT_REVOLVER.get(), stack(StandardAmmoType.M357_BP, 4), stack(StandardAmmoType.M357_SP, 10));
		ItemGunBaseNT revGun = (ItemGunBaseNT) rev.getItem();
		GunConfig cfg = revGun.getConfig(rev, 0);
		Receiver rec = cfg.getReceivers(rev)[0];
		check(problems, cases, cfg.getDurability(rev) == 300 && cfg.getDrawDuration(rev) == 4 && cfg.getInspectDuration(rev) == 23 && rec.getBaseDamage(rev) == 7.5F
				&& rec.getDelayAfterFire(rev) == 11 && !rec.getRefireOnHold(rev) && rec.getReloadBeginDuration(rev) == 55 && rec.getJamDuration(rev) == 45
				&& rec.getMagazine(rev).getCapacity(rev) == 6 && rec.getFirePitch(rev) == 1F && rec.getProjectileOffset(rev).z == -0.3125D, "light_revolver values");
		reloadCycle(problems, cases, player, rev, 0, 55, 4, "light_revolver (BP loaded first)");
		fire(player, rev, GunKeybind.GUN_PRIMARY);
		release(player, rev, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.M357_BP && ItemGunBaseNT.getTimer(rev, 0) == 11
				&& rec.getMagazine(rev).getAmount(rev, null) == 3 && ItemGunBaseNT.getWear(rev, 0) == 1F && ItemGunBaseNT.getLastAnim(rev, 0) == GunAnimation.CYCLE, "light_revolver BP shot (black powder path)");
		tick(player, rev, 12);
		fire(player, rev, GunKeybind.GUN_PRIMARY);
		release(player, rev, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).size() == 2 && rec.getMagazine(rev).getAmount(rev, null) == 2, "light_revolver refire after delay");
		discardBullets(level);

		// Atlas: same receiver, dmg 12.5
		ItemStack atlas = equip(player, HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get(), stack(StandardAmmoType.M357_JHP, 12));
		Receiver atlasRec = ((ItemGunBaseNT) atlas.getItem()).getConfig(atlas, 0).getReceivers(atlas)[0];
		check(problems, cases, atlasRec.getBaseDamage(atlas) == 12.5F && atlasRec.getDelayAfterFire(atlas) == 11 && atlasRec.getMagazine(atlas).getCapacity(atlas) == 6
				&& ((ItemGunBaseNT) atlas.getItem()).getConfig(atlas, 0).getDurability(atlas) == 300, "atlas values");

		// Day and Night: two configs, celestial (0) on LMB, lunar (1) on RMB, R reloads both, dura 30000, draw 20, no hipfire spread
		ItemStack dani = equip(player, HbmItems.GUN_LIGHT_REVOLVER_DANI.get(), stack(StandardAmmoType.M357_EXPRESS, 12));
		ItemGunBaseNT daniGun = (ItemGunBaseNT) dani.getItem();
		Receiver celestial = daniGun.getConfig(dani, 0).getReceivers(dani)[0];
		Receiver lunar = daniGun.getConfig(dani, 1).getReceivers(dani)[0];
		IMagazine<?> left = celestial.getMagazine(dani);
		IMagazine<?> right = lunar.getMagazine(dani);
		check(problems, cases, daniGun.getConfig(dani, 0).getDurability(dani) == 30_000 && daniGun.getConfig(dani, 1).getDrawDuration(dani) == 20
				&& celestial.getBaseDamage(dani) == 15F && lunar.getBaseDamage(dani) == 15F && celestial.getHipfireSpread(dani) == 0F && lunar.getHipfireSpread(dani) == 0F
				&& celestial.getFirePitch(dani) == 1.1F && lunar.getFirePitch(dani) == 0.9F && celestial.getProjectileOffset(dani).z == 0.3125D
				&& lunar.getProjectileOffset(dani).z == -0.3125D && daniGun.getConfig(dani, 0).getPressTertiary(dani) == null, "dani values");
		press(player, dani, GunKeybind.RELOAD);
		release(player, dani, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(dani, 0) == GunState.RELOADING && ItemGunBaseNT.getState(dani, 1) == GunState.RELOADING, "dani both reload");
		tick(player, dani, 56);
		check(problems, cases, left.getAmount(dani, null) == 6 && right.getAmount(dani, null) == 6 && countAmmo(player, StandardAmmoType.M357_EXPRESS) == 0, "dani reload result " + left.getAmount(dani, null) + "/" + right.getAmount(dani, null));
		fire(player, dani, GunKeybind.GUN_PRIMARY);
		release(player, dani, GunKeybind.GUN_PRIMARY);
		check(problems, cases, left.getAmount(dani, null) == 5 && right.getAmount(dani, null) == 6 && ItemGunBaseNT.getWear(dani, 0) == 1.5F, "dani celestial fire (express wear 1.5)");
		fire(player, dani, GunKeybind.GUN_SECONDARY);
		release(player, dani, GunKeybind.GUN_SECONDARY);
		check(problems, cases, right.getAmount(dani, null) == 5 && ItemGunBaseNT.getLastAnim(dani, 1) == GunAnimation.CYCLE && bullets(level).size() == 2, "dani lunar fire");
		discardBullets(level);

		// Pepperbox: dmg 5, delay 27, reload 67, jam 58, mag 6, black powder fire sound
		ItemStack pep = equip(player, HbmItems.GUN_PEPPERBOX.get(), stack(StandardAmmoType.STONE_SHOT, 6));
		ItemGunBaseNT pepGun = (ItemGunBaseNT) pep.getItem();
		cfg = pepGun.getConfig(pep, 0);
		rec = cfg.getReceivers(pep)[0];
		check(problems, cases, cfg.getDurability(pep) == 300 && cfg.getDrawDuration(pep) == 4 && cfg.getInspectDuration(pep) == 23 && rec.getBaseDamage(pep) == 5F
				&& rec.getDelayAfterFire(pep) == 27 && rec.getReloadBeginDuration(pep) == 67 && rec.getJamDuration(pep) == 58 && rec.getMagazine(pep).getCapacity(pep) == 6
				&& rec.getFireSound(pep) != null && rec.getFireSound(pep).get() == HbmSoundEvents.GUN_POWDER_FIRE.get() && rec.getProjectileOffset(pep).z == -0.1875D, "pepperbox values");
		reloadCycle(problems, cases, player, pep, 0, 67, 6, "pepperbox");
		fire(player, pep, GunKeybind.GUN_PRIMARY);
		release(player, pep, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 6 && bullets.stream().allMatch(b -> b.ammoType() == StandardAmmoType.STONE_SHOT) && rec.getMagazine(pep).getAmount(pep, null) == 5
				&& ItemGunBaseNT.getTimer(pep, 0) == 27, "pepperbox shot: 6 pellets " + bullets.size());
		discardBullets(level);

		/// Bullet behaviour ///
		Pig pig = EntityType.PIG.create(level);
		if(pig != null) {
			pig.setNoAi(true);
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
			pig.setHealth(1000F);
			pig.setPos(0.5D, player.getEyeY() - 0.45D, 3.5D);
			level.addFreshEntity(pig);

			float before = pig.getHealth();
			shoot(player, XFactory357.m357_sp, 7.5F);
			check(problems, cases, Math.abs(before - pig.getHealth() - 7.5F) < 0.01F && pig.getLastHurtByMob() == player, ".357 SP hit dmg=" + (before - pig.getHealth()));
			before = pig.getHealth();
			pig.invulnerableTime = 0;
			shoot(player, XFactory357.m357_bp, 7.5F);
			check(problems, cases, Math.abs(before - pig.getHealth() - 7.5F * 0.75F) < 0.01F, ".357 BP hit dmg=" + (before - pig.getHealth()));
			before = pig.getHealth();
			pig.invulnerableTime = 0;
			shoot(player, XFactoryBlackPowder.flint, 5F);
			check(problems, cases, Math.abs(before - pig.getHealth() - 5F * 1.5F) < 0.01F, "flint hit dmg=" + (before - pig.getHealth()));
			pig.discard();
		}
		discardBullets(level);
		player.getInventory().clearContent();

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_C1_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static void checkPedestal(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos center = new BlockPos(20, 250, 20);
		int[][] offsets = { {-2, -2}, {0, -3}, {2, -2}, {-3, 0}, {0, 0}, {3, 0}, {-2, 2}, {0, 3}, {2, 2} };
		ItemStack lead = firstTagged("plates/lead");
		ItemStack gold = firstTagged("plates/gold");
		ItemStack[] input = { ItemStack.EMPTY, lead, ItemStack.EMPTY, gold, new ItemStack(HbmItems.GUN_LIGHT_REVOLVER.get()), gold, ItemStack.EMPTY, lead, ItemStack.EMPTY };
		for(int i = 0; i < offsets.length; i++) {
			BlockPos p = center.offset(offsets[i][0], 0, offsets[i][1]);
			level.setBlock(p, HbmBlocks.PEDESTAL.get().defaultBlockState(), 3);
			if(level.getBlockEntity(p) instanceof LegacyDisplayStandBlockEntity stand) stand.setDisplayedItem(input[i].copy());
		}
		// a redstone signal into the centre pedestal crafts (LegacyDisplayStandBlock#neighborChanged)
		level.setBlock(center.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		boolean ok = level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity stand && stand.displayedItem().is(HbmItems.GUN_LIGHT_REVOLVER_DANI.get());
		for(int i = 0; i < offsets.length; i++) {
			BlockPos p = center.offset(offsets[i][0], 0, offsets[i][1]);
			if(level.getBlockEntity(p) instanceof LegacyDisplayStandBlockEntity stand) {
				if(i != 4 && !stand.displayedItem().isEmpty()) ok = false;
				stand.setDisplayedItem(ItemStack.EMPTY);
			}
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
		level.setBlock(center.below(), Blocks.AIR.defaultBlockState(), 3);
		check(problems, cases, !lead.isEmpty() && !gold.isEmpty() && ok, "pedestal recipe gun_light_revolver_dani");
	}

	private static ItemStack firstTagged(String path) {
		var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", path));
		return BuiltInRegistries.ITEM.getTag(tag).flatMap(t -> t.stream().findFirst()).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
	}

	private static boolean round(StandardAmmoType t, float dmg, boolean pen, boolean falloff, float dt, float ap, float head) {
		return t.damageMultiplier() == dmg && t.penetrates() == pen && t.damageFalloffByPenetration() == falloff && t.armorThresholdNegation() == dt
				&& t.armorPiercingPercent() == ap && t.headshotMultiplier() == head && t.knockbackMultiplier() == 0.1F && t.projectileSpeed() == 10D
				&& t.projectileLifetime() == 30 && t.projectileGravity() == 0D && t.selfDamageDelay() == 2 && !t.spectral() && t.onImpact() == null;
	}

	private static LegacyBulletEntity shoot(FakePlayer player, BulletConfig config, float damage) {
		LegacyBulletEntity bullet = Lego.spawnBullet(player, config, damage, 0F, -0.25D, -0.0625D * 2.5D, 1D);
		for(int i = 0; i < 3 && !bullet.isRemoved() && !bullet.isRetired(); i++) bullet.tick();
		return bullet;
	}

	private static ItemStack equip(FakePlayer player, Item gun, ItemStack... ammo) {
		player.getInventory().clearContent();
		player.getInventory().setItem(0, new ItemStack(gun));
		for(int i = 0; i < ammo.length; i++) player.getInventory().setItem(i + 1, ammo[i]);
		ItemStack stack = player.getInventory().getItem(0);
		tick(player, stack, 21);
		return stack;
	}

	private static void reloadCycle(List<String> problems, int[] cases, FakePlayer player, ItemStack gun, int index, int duration, int expected, String name) {
		IMagazine<?> mag = ((ItemGunBaseNT) gun.getItem()).getConfig(gun, index).getReceivers(gun)[0].getMagazine(gun);
		press(player, gun, GunKeybind.RELOAD);
		release(player, gun, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(gun, index) == GunState.RELOADING && ItemGunBaseNT.getTimer(gun, index) == duration && ItemGunBaseNT.getLastAnim(gun, index) == GunAnimation.RELOAD, name + " reload start");
		tick(player, gun, duration - 1);
		check(problems, cases, mag.getAmount(gun, null) == 0, name + " reload finished early");
		tick(player, gun, 2);
		check(problems, cases, mag.getAmount(gun, null) == expected && ItemGunBaseNT.getState(gun, index) == GunState.IDLE, name + " reload result " + mag.getAmount(gun, null));
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
		if(!ok) problems.add("gun batch C1: " + message);
	}
}
