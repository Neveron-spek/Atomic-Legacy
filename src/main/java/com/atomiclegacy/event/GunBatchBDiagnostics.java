package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.inventory.recipes.AmmoPressRecipe;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
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
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasB;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory22lr;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory50;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for weapons batch B (gun_am180, gun_star_f, gun_star_f_akimbo,
 * gun_amat, gun_m2 and the .22 LR / .50 BMG HE+SM rounds) against the pinned 1.7.10
 * XFactory22lr / XFactory50 / AmmoPressRecipes / AssemblyMachineRecipes values.
 */
final class GunBatchBDiagnostics {

	private GunBatchBDiagnostics() {
	}

	private static final AABB AREA = new AABB(-8, 290, -8, 8, 310, 8);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Registration, quality, default ammo, tab order ///
		Object[][] guns = {
				{ HbmItems.GUN_AM180.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.P22_SP, 35 },
				{ HbmItems.GUN_STAR_F.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.P22_SP, 15 },
				{ HbmItems.GUN_STAR_F_AKIMBO.get(), WeaponQuality.B_SIDE, 2, StandardAmmoType.P22_SP, 30 },
				{ HbmItems.GUN_AMAT.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.BMG50_SP, 7 },
				{ HbmItems.GUN_M2.get(), WeaponQuality.A_SIDE, 1, StandardAmmoType.BMG50_FMJ, 25 },
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
		Item[] order = { HbmItems.GUN_GREASEGUN.get(), HbmItems.GUN_AM180.get(), HbmItems.GUN_UZI.get(),
				HbmItems.GUN_STAR_F.get(), HbmItems.GUN_G3.get(), HbmItems.GUN_AMAT.get(),
				HbmItems.GUN_M2.get(), HbmItems.GUN_LAG.get(), HbmItems.GUN_STG77.get(), HbmItems.AMMO_STANDARD.get() };
		int last = -1;
		boolean ordered = true;
		for(Item item : order) { int i = indexOf(tab, item); if(i <= last) ordered = false; last = i; }
		check(problems, cases, ordered, "weapon tab gun order (ModItems)");
		check(problems, cases, indexOf(tab, HbmItems.GUN_UZI_AKIMBO.get()) < 0 && indexOf(tab, HbmItems.GUN_STAR_F_AKIMBO.get()) < 0, "B_SIDE akimbo guns must not be in the weapon tab");
		List<String> ammoOrder = new ArrayList<>();
		for(ItemStack stack : tab) if(stack.is(HbmItems.AMMO_STANDARD.get())) ammoOrder.add(StandardAmmoItem.standardType(stack).id());
		check(problems, cases, ammoOrder.indexOf("p22_sp") >= 0 && ammoOrder.indexOf("p22_sp") < ammoOrder.indexOf("p22_fmj") && ammoOrder.indexOf("p22_fmj") < ammoOrder.indexOf("p22_jhp")
				&& ammoOrder.indexOf("p22_jhp") < ammoOrder.indexOf("p22_ap") && ammoOrder.indexOf("p22_ap") < ammoOrder.indexOf("p9_sp")
				&& ammoOrder.indexOf("bmg50_du") < ammoOrder.indexOf("bmg50_sm") && ammoOrder.indexOf("bmg50_sm") < ammoOrder.indexOf("bmg50_he"), "ammo tab order " + ammoOrder);

		/// Ammo values (BulletConfig) ///
		check(problems, cases, round(StandardAmmoType.P22_SP, 1F, false, true, 0F, 0F, 1.25F, 0F), "p22_sp values");
		check(problems, cases, round(StandardAmmoType.P22_FMJ, 0.8F, false, true, 1F, 0.1F, 1.25F, 0F), "p22_fmj values");
		check(problems, cases, round(StandardAmmoType.P22_JHP, 1.5F, false, true, 0F, -0.25F, 1.5F, 0F), "p22_jhp values");
		check(problems, cases, round(StandardAmmoType.P22_AP, 1.25F, true, false, 2.5F, 0.15F, 1.25F, 0F), "p22_ap values");
		check(problems, cases, round(StandardAmmoType.BMG50_HE, 1.75F, false, true, 0F, 0F, 1.25F, 0.1F) && StandardAmmoType.BMG50_HE.explodesOnImpact(), "bmg50_he values");
		check(problems, cases, round(StandardAmmoType.BMG50_SM, 2.5F, true, false, 30F, 0.35F, 1.25F, 0.1F), "bmg50_sm values");
		check(problems, cases, XFactory50.bmg50_he.wear == 3F && XFactory50.bmg50_sm.wear == 10F && XFactory50.bmg50_sp.wear == 1F, "bmg50 wear");
		check(problems, cases, XFactory22lr.p22_ap.casingEffect == OrchestrasB.CASING_P22_STEEL && XFactory22lr.p22_sp.casingEffect == OrchestrasB.CASING_P22
				&& XFactory50.bmg50_he.casingEffect == OrchestrasB.CASING_BMG50_STEEL && XFactory50.bmg50_fmj.casingEffect == OrchestrasB.CASING_BMG50, "casings");
		check(problems, cases, StandardAmmoType.byId("bmg50_he") == StandardAmmoType.BMG50_HE && StandardAmmoItem.bmg50Type(stack(StandardAmmoType.BMG50_HE, 1)) == StandardAmmoItem.Bmg50Type.NONE, "bmg50_he not a Chekhov round");

		/// Config values ///
		check(problems, cases, HbmConfig.am180Damage() == 2F && HbmConfig.starfDamage() == 12.5F && HbmConfig.starfAkimboLeftDamage() == 12.5F
				&& HbmConfig.starfAkimboRightDamage() == 12.5F && HbmConfig.antiMaterielDamage() == 30F && HbmConfig.m2Damage() == 7.5F, "config damage defaults");

		/// Recipes ///
		var recipes = server.getRecipeManager();
		var starRecipe = recipes.byKey(AtomicLegacy.id("gun_star_f")).map(h -> h.value()).orElse(null);
		List<ItemStack> grid = new ArrayList<>(List.of(item("part_barrel_light_weaponsteel"), item("part_receiver_light_weaponsteel"), item("part_mechanism_weaponsteel"),
				ItemStack.EMPTY, ItemStack.EMPTY, item("part_grip_polymer")));
		var starInput = CraftingInput.of(3, 2, grid);
		check(problems, cases, starRecipe instanceof ShapedRecipe s && s.matches(starInput, level) && s.assemble(starInput, server.registryAccess()).is(HbmItems.GUN_STAR_F.get()), "gun_star_f recipe BRM/  G");
		var akimboRecipe = recipes.byKey(AtomicLegacy.id("gun_star_f_akimbo")).map(h -> h.value()).orElse(null);
		var akimboInput = CraftingInput.of(3, 1, List.of(new ItemStack(HbmItems.GUN_STAR_F.get()), item("part_mechanism_saturnite"), new ItemStack(HbmItems.GUN_STAR_F.get())));
		check(problems, cases, akimboRecipe instanceof ShapedRecipe s && s.matches(akimboInput, level) && s.assemble(akimboInput, server.registryAccess()).is(HbmItems.GUN_STAR_F_AKIMBO.get()), "gun_star_f_akimbo recipe UMU");

		// AmmoPressRecipes (slot grid 3x3, null = empty)
		String lead = "#c:ingots/lead", steel = "#c:ingots/steel", plastic = "#c:ingots/plastic", copper = "#c:ingots/copper", wsteel = "#c:ingots/weaponsteel";
		String smokeless = "#c:dusts/smokeless", cSmall = "atomiclegacy:casing_small", sSmall = "atomiclegacy:casing_small_steel", sBig = "atomiclegacy:casing_large_steel";
		pressRecipe(server, problems, cases, "p22_sp", StandardAmmoType.P22_SP, 24, new Object[] { null, lead, null, null, smokeless, null, null, cSmall, null }, new int[] { 0, 1, 0, 0, 1, 0, 0, 1, 0 });
		pressRecipe(server, problems, cases, "p22_fmj", StandardAmmoType.P22_FMJ, 24, new Object[] { null, steel, null, null, smokeless, null, null, cSmall, null }, new int[] { 0, 1, 0, 0, 1, 0, 0, 1, 0 });
		pressRecipe(server, problems, cases, "p22_jhp", StandardAmmoType.P22_JHP, 24, new Object[] { plastic, copper, null, null, smokeless, null, null, cSmall, null }, new int[] { 1, 1, 0, 0, 1, 0, 0, 1, 0 });
		pressRecipe(server, problems, cases, "p22_ap", StandardAmmoType.P22_AP, 24, new Object[] { null, wsteel, null, null, smokeless, null, null, sSmall, null }, new int[] { 0, 1, 0, 0, 2, 0, 0, 1, 0 });
		pressRecipe(server, problems, cases, "bmg50_he", StandardAmmoType.BMG50_HE, 12, new Object[] { "#c:ingots/high_explosive", "#c:ingots/ferrouranium", null, null, smokeless, null, null, sBig, null }, new int[] { 1, 1, 0, 0, 6, 0, 0, 1, 0 });
		// AssemblyMachineRecipes ass.50bmgsm: 100 ticks, 100 HE/t, LARGE_STEEL casing, 6 smokeless, 3 starmetal -> 6, pool discover.silverstorm
		var sm = recipes.byKey(AtomicLegacy.id("assembly_machine/ammo_standard_bmg50_sm")).map(h -> h.value()).orElse(null);
		check(problems, cases, sm instanceof AssemblyMachineRecipe a && a.duration() == 100 && a.power() == 100 && a.ingredients().size() == 3
				&& a.blueprintPools().equals(List.of("discover.silverstorm")) && StandardAmmoItem.standardType(a.result()) == StandardAmmoType.BMG50_SM && a.result().getCount() == 6, "ass.50bmgsm recipe");

		/// Gun cycles ///
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		discardBullets(level);

		// AM180: dura 4425, draw 15, inspect 38, dmg 2, delay 1, dry 10, auto, spread 0.01, reload 66, jam 30, mag 177
		ItemStack am180 = equip(player, HbmItems.GUN_AM180.get(), stack(StandardAmmoType.P22_SP, 64), stack(StandardAmmoType.P22_SP, 64), stack(StandardAmmoType.P22_SP, 64));
		ItemGunBaseNT am180Gun = (ItemGunBaseNT) am180.getItem();
		GunConfig cfg = am180Gun.getConfig(am180, 0);
		Receiver rec = cfg.getReceivers(am180)[0];
		check(problems, cases, cfg.getDurability(am180) == 177 * 25 && cfg.getDrawDuration(am180) == 15 && cfg.getInspectDuration(am180) == 38
				&& rec.getBaseDamage(am180) == 2F && rec.getDelayAfterFire(am180) == 1 && rec.getDelayAfterDryFire(am180) == 10 && rec.getRefireOnHold(am180)
				&& rec.getInnateSpread(am180) == 0.01F && rec.getReloadBeginDuration(am180) == 66 && rec.getJamDuration(am180) == 30
				&& rec.getMagazine(am180).getCapacity(am180) == 177, "am180 values");
		reloadCycle(problems, cases, player, am180, 0, 66, 177, "am180");
		check(problems, cases, countAmmo(player, StandardAmmoType.P22_SP) == 192 - 177, "am180 reload consumption " + countAmmo(player, StandardAmmoType.P22_SP));
		fire(player, am180, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.P22_SP && ItemGunBaseNT.getTimer(am180, 0) == 1, "am180 shot " + bullets.size());
		tick(player, am180, 10);
		release(player, am180, GunKeybind.GUN_PRIMARY);
		check(problems, cases, rec.getMagazine(am180).getAmount(am180, null) == 177 - 11 && bullets(level).size() == 11, "am180 auto fire (1 per tick) mag=" + rec.getMagazine(am180).getAmount(am180, null) + " bullets=" + bullets(level).size());
		discardBullets(level);

		// STAR-F: dura 375, draw 15, dmg 12.5, delay 5, dry 17, semi-auto, reload 40, jam 32, mag 15
		ItemStack starF = equip(player, HbmItems.GUN_STAR_F.get(), stack(StandardAmmoType.P22_AP, 20));
		ItemGunBaseNT starGun = (ItemGunBaseNT) starF.getItem();
		cfg = starGun.getConfig(starF, 0);
		rec = cfg.getReceivers(starF)[0];
		check(problems, cases, cfg.getDurability(starF) == 375 && cfg.getDrawDuration(starF) == 15 && rec.getBaseDamage(starF) == 12.5F && rec.getDelayAfterFire(starF) == 5
				&& rec.getDelayAfterDryFire(starF) == 17 && !rec.getRefireOnHold(starF) && rec.getReloadBeginDuration(starF) == 40 && rec.getJamDuration(starF) == 32
				&& rec.getMagazine(starF).getCapacity(starF) == 15, "star_f values");
		reloadCycle(problems, cases, player, starF, 0, 40, 15, "star_f");
		fire(player, starF, GunKeybind.GUN_PRIMARY);
		tick(player, starF, 12);
		release(player, starF, GunKeybind.GUN_PRIMARY);
		bullets = bullets(level);
		check(problems, cases, rec.getMagazine(starF).getAmount(starF, null) == 14 && bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.P22_AP
				&& rec.getMagazine(starF).getCasing(starF, null) == OrchestrasB.CASING_P22_STEEL, "star_f semi-auto shot");
		discardBullets(level);

		// STAR-F akimbo: two configs, left = mag 0 on LMB, right = mag 1 on RMB, R reloads both
		ItemStack akimbo = equip(player, HbmItems.GUN_STAR_F_AKIMBO.get(), stack(StandardAmmoType.P22_SP, 30));
		ItemGunBaseNT akimboGun = (ItemGunBaseNT) akimbo.getItem();
		IMagazine<?> left = akimboGun.getConfig(akimbo, 0).getReceivers(akimbo)[0].getMagazine(akimbo);
		IMagazine<?> right = akimboGun.getConfig(akimbo, 1).getReceivers(akimbo)[0].getMagazine(akimbo);
		check(problems, cases, akimboGun.getConfig(akimbo, 0).getReceivers(akimbo)[0].getProjectileOffset(akimbo).z == 0.25D
				&& akimboGun.getConfig(akimbo, 1).getReceivers(akimbo)[0].getProjectileOffset(akimbo).z == -0.25D
				&& akimboGun.getConfig(akimbo, 0).getPressTertiary(akimbo) == null, "akimbo offsets / no aim");
		press(player, akimbo, GunKeybind.RELOAD);
		release(player, akimbo, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(akimbo, 0) == GunState.RELOADING && ItemGunBaseNT.getState(akimbo, 1) == GunState.RELOADING, "akimbo both reload");
		tick(player, akimbo, 41);
		check(problems, cases, left.getAmount(akimbo, null) == 15 && right.getAmount(akimbo, null) == 15 && countAmmo(player, StandardAmmoType.P22_SP) == 0, "akimbo reload result " + left.getAmount(akimbo, null) + "/" + right.getAmount(akimbo, null));
		fire(player, akimbo, GunKeybind.GUN_PRIMARY);
		release(player, akimbo, GunKeybind.GUN_PRIMARY);
		check(problems, cases, left.getAmount(akimbo, null) == 14 && right.getAmount(akimbo, null) == 15 && ItemGunBaseNT.getLastAnim(akimbo, 0) == GunAnimation.CYCLE, "akimbo left fire");
		fire(player, akimbo, GunKeybind.GUN_SECONDARY);
		release(player, akimbo, GunKeybind.GUN_SECONDARY);
		check(problems, cases, left.getAmount(akimbo, null) == 14 && right.getAmount(akimbo, null) == 14 && ItemGunBaseNT.getLastAnim(akimbo, 1) == GunAnimation.CYCLE
				&& bullets(level).size() == 2, "akimbo right fire");
		discardBullets(level);

		// AMAT: dura 350, draw 20, inspect 50, dmg 30, delay 25, dry 25, hipfire 0.05, reload 51, jam 43, mag 7, scope
		ItemStack amat = equip(player, HbmItems.GUN_AMAT.get(), stack(StandardAmmoType.BMG50_HE, 3), stack(StandardAmmoType.BMG50_SP, 10));
		ItemGunBaseNT amatGun = (ItemGunBaseNT) amat.getItem();
		cfg = amatGun.getConfig(amat, 0);
		rec = cfg.getReceivers(amat)[0];
		check(problems, cases, cfg.getDurability(amat) == 350 && cfg.getDrawDuration(amat) == 20 && cfg.getInspectDuration(amat) == 50 && rec.getBaseDamage(amat) == 30F
				&& rec.getDelayAfterFire(amat) == 25 && rec.getDelayAfterDryFire(amat) == 25 && rec.getHipfireSpread(amat) == 0.05F && rec.getReloadBeginDuration(amat) == 51
				&& rec.getJamDuration(amat) == 43 && rec.getMagazine(amat).getCapacity(amat) == 7
				&& AtomicLegacy.id("textures/misc/scope_amat.png").equals(cfg.getScopeTexture(amat)), "amat values");
		reloadCycle(problems, cases, player, amat, 0, 51, 3, "amat (HE loaded first)");
		fire(player, amat, GunKeybind.GUN_PRIMARY);
		release(player, amat, GunKeybind.GUN_PRIMARY);
		check(problems, cases, ItemGunBaseNT.getTimer(amat, 0) == 25 && ItemGunBaseNT.getWear(amat, 0) == 3F && bullets(level).size() == 1
				&& bullets(level).get(0).ammoType() == StandardAmmoType.BMG50_HE, "amat HE shot wear=" + ItemGunBaseNT.getWear(amat, 0));
		discardBullets(level);

		// M2: belt fed from the inventory, dura 3000, draw 10, dmg 7.5, delay 2, dry 10, auto, spread 0.005
		ItemStack m2 = equip(player, HbmItems.GUN_M2.get(), stack(StandardAmmoType.BMG50_JHP, 3), stack(StandardAmmoType.BMG50_SP, 5));
		ItemGunBaseNT m2Gun = (ItemGunBaseNT) m2.getItem();
		cfg = m2Gun.getConfig(m2, 0);
		rec = cfg.getReceivers(m2)[0];
		IMagazine<?> belt = rec.getMagazine(m2);
		check(problems, cases, belt instanceof MagazineBelt && cfg.getDurability(m2) == 3000 && cfg.getDrawDuration(m2) == 10 && rec.getBaseDamage(m2) == 7.5F
				&& rec.getDelayAfterFire(m2) == 2 && rec.getDelayAfterDryFire(m2) == 10 && rec.getRefireOnHold(m2) && rec.getInnateSpread(m2) == 0.005F, "m2 values");
		check(problems, cases, belt.getAmount(m2, player.getInventory()) == 3 && belt.getType(m2, player.getInventory()) == XFactory50.bmg50_jhp
				&& belt.reportAmmoStateForHUD(m2, player).equals("x3") && !belt.canReload(m2, player.getInventory()), "m2 belt reads first inventory round");
		press(player, m2, GunKeybind.RELOAD);
		release(player, m2, GunKeybind.RELOAD);
		check(problems, cases, ItemGunBaseNT.getState(m2, 0) == GunState.IDLE && ItemGunBaseNT.getLastAnim(m2, 0) == GunAnimation.INSPECT, "m2 R inspects (no reload)");
		fire(player, m2, GunKeybind.GUN_PRIMARY);
		tick(player, m2, 9);
		release(player, m2, GunKeybind.GUN_PRIMARY);
		int jhpLeft = countAmmo(player, StandardAmmoType.BMG50_JHP);
		int spLeft = countAmmo(player, StandardAmmoType.BMG50_SP);
		long jhpBullets = bullets(level).stream().filter(b -> b.ammoType() == StandardAmmoType.BMG50_JHP).count();
		check(problems, cases, jhpLeft == 0 && spLeft == 3 && jhpBullets == 3 && bullets(level).size() == 5, "m2 belt fire jhp=" + jhpLeft + " sp=" + spLeft + " bullets=" + bullets(level).size());
		discardBullets(level);
		player.getInventory().clearContent();
		player.getInventory().setItem(0, m2);
		tick(player, m2, 10);
		fire(player, m2, GunKeybind.GUN_PRIMARY);
		release(player, m2, GunKeybind.GUN_PRIMARY);
		check(problems, cases, ItemGunBaseNT.getLastAnim(m2, 0) == GunAnimation.CYCLE_DRY && ItemGunBaseNT.getTimer(m2, 0) == 10, "m2 dry fire with empty belt");

		/// Bullet behaviour ///
		Pig pig = EntityType.PIG.create(level);
		if(pig != null) {
			pig.setNoAi(true);
			pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
			pig.setHealth(1000F);
			pig.setPos(0.5D, player.getEyeY() - 0.45D, 3.5D);
			level.addFreshEntity(pig);

			// .22: knockback 0 (no vanilla knockback either), damage 12.5 * 1.25 AP, shooter attributed
			pig.setDeltaMovement(Vec3.ZERO);
			float before = pig.getHealth();
			shoot(player, XFactory22lr.p22_ap, 12.5F);
			Vec3 motion = pig.getDeltaMovement();
			check(problems, cases, Math.abs(before - pig.getHealth() - 12.5F * 1.25F) < 0.01F && pig.getLastHurtByMob() == player
					&& Math.abs(motion.x) < 1.0E-6 && Math.abs(motion.z) < 1.0E-6, ".22 AP hit dmg=" + (before - pig.getHealth()) + " motion=" + motion);

			// .50 SP: knockbackMult 0.1 -> 0.04 horizontal away from the shooter
			pig.setDeltaMovement(Vec3.ZERO);
			before = pig.getHealth();
			shoot(player, XFactory50.bmg50_sp, 30F);
			motion = pig.getDeltaMovement();
			check(problems, cases, Math.abs(before - pig.getHealth() - 30F) < 0.01F && motion.z > 0.039D && motion.z < 0.041D, ".50 SP hit dmg=" + (before - pig.getHealth()) + " motion=" + motion);

			// .50 HE: LAMBDA_STANDARD_EXPLODE -> tinyExplode(2), the bullet itself deals no direct hit
			pig.setDeltaMovement(Vec3.ZERO);
			before = pig.getHealth();
			LegacyBulletEntity he = shoot(player, XFactory50.bmg50_he, 30F);
			float dealt = before - pig.getHealth();
			check(problems, cases, he.isRemoved() || he.isRetired(), "HE bullet dies on impact");
			check(problems, cases, dealt > 0F && dealt <= 30F * 1.75F + 0.01F && dealt != 30F * 1.75F * 1.25F, "HE explosion damage " + dealt);
			check(problems, cases, level.getBlockState(pig.blockPosition()).isAir(), "HE has no block damage");
			pig.discard();
		}
		discardBullets(level);
		player.getInventory().clearContent();

		AtomicLegacy.LOGGER.info("HBM_GUN_BATCH_B_AUDIT cases={} problems={}", cases[0], problems.size());
		return cases[0];
	}

	private static boolean round(StandardAmmoType t, float dmg, boolean pen, boolean falloff, float dt, float ap, float head, float knockback) {
		return t.damageMultiplier() == dmg && t.penetrates() == pen && t.damageFalloffByPenetration() == falloff && t.armorThresholdNegation() == dt
				&& t.armorPiercingPercent() == ap && t.headshotMultiplier() == head && t.knockbackMultiplier() == knockback && t.projectileSpeed() == 10D
				&& t.projectileLifetime() == 30 && t.projectileGravity() == 0D && t.selfDamageDelay() == 2;
	}

	private static void pressRecipe(MinecraftServer server, List<String> problems, int[] cases, String id, StandardAmmoType type, int count, Object[] slots, int[] counts) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id("ammo_press/" + id)).map(h -> h.value()).orElse(null);
		boolean ok = recipe instanceof AmmoPressRecipe;
		if(ok) {
			AmmoPressRecipe press = (AmmoPressRecipe) recipe;
			ok = press.input().size() == 9 && StandardAmmoItem.standardType(press.result()) == type && press.result().getCount() == count;
			for(int i = 0; ok && i < 9; i++) {
				var slot = press.input().get(i);
				if(slots[i] == null) { ok = slot.ingredient().isEmpty(); continue; }
				ok = slot.ingredient().isPresent() && slot.count() == counts[i] && sameItems(slot.ingredient().get(), (String) slots[i]);
			}
		}
		check(problems, cases, ok, "ammo press recipe " + id);
	}

	private static boolean sameItems(Ingredient ingredient, String spec) {
		Set<Item> expected = new HashSet<>();
		if(spec.startsWith("#")) {
			BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(spec.substring(1)))).ifPresent(tag -> tag.forEach(h -> expected.add(h.value())));
		} else {
			expected.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec)));
		}
		Set<Item> actual = new HashSet<>();
		for(ItemStack s : ingredient.getItems()) actual.add(s.getItem());
		return !expected.isEmpty() && expected.equals(actual);
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
		tick(player, stack, 1);
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
		if(!ok) problems.add("gun batch B: " + message);
	}
}
