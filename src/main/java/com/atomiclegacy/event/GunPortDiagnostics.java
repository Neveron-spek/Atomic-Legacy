package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.inventory.container.WeaponTableMenu;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory9mm;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mods.WeaponModGreasegun;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks of the ported sedna gun system against the values of
 * the pinned 1.7.10 source (XFactory9mm, Lego, GunStateDecider, WeaponModGreasegun,
 * ContainerWeaponTable).
 */
final class GunPortDiagnostics {

	private GunPortDiagnostics() {
	}

	static int run(MinecraftServer server, List<String> problems) {
		int cases = 0;
		ServerLevel level = server.overworld();

		// Registration and tabs
		ItemStack gun = new ItemStack(HbmItems.GUN_GREASEGUN.get());
		cases++; if(!(gun.getItem() instanceof ItemGunBaseNT)) { problems.add("gun_greasegun is not an ItemGunBaseNT"); return cases; }
		ItemGunBaseNT item = (ItemGunBaseNT) gun.getItem();
		cases++; check(problems, gun.getMaxStackSize() == 1, "gun_greasegun stack size " + gun.getMaxStackSize());
		cases++; check(problems, item.quality == ItemGunBaseNT.WeaponQuality.A_SIDE, "gun_greasegun quality");
		cases++; check(problems, HbmBlocks.MACHINE_WEAPON_TABLE.get().asItem() != net.minecraft.world.item.Items.AIR, "machine_weapon_table has no item");
		var weaponTab = HbmCreativeTabs.TURRETS.get();
		var consumableTab = HbmCreativeTabs.CONSUMABLES.get();
		if(weaponTab != null) {
			List<ItemStack> entries = new ArrayList<>(weaponTab.getDisplayItems());
			int gunIndex = indexOf(entries, HbmItems.GUN_GREASEGUN.get());
			int ammoIndex = indexOf(entries, HbmItems.AMMO_STANDARD.get());
			cases++; check(problems, gunIndex >= 0 && (ammoIndex < 0 || gunIndex < ammoIndex), "gun_greasegun weapon tab position " + gunIndex + " ammo " + ammoIndex);
		}
		if(consumableTab != null) {
			cases++; check(problems, indexOf(new ArrayList<>(consumableTab.getDisplayItems()), HbmBlocks.MACHINE_WEAPON_TABLE.get().asItem()) >= 0, "machine_weapon_table missing from consumable tab");
		}

		// Recipes (WeaponRecipes)
		var gunRecipe = server.getRecipeManager().byKey(AtomicLegacy.id("gun_greasegun")).map(holder -> holder.value()).orElse(null);
		List<ItemStack> grid = new ArrayList<>();
		for(String part : List.of("part_barrel_light_steel", "part_receiver_light_steel", "bolt_steel", "bolt_steel", "part_mechanism_gunmetal", "part_grip_steel")) {
			grid.add(BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(part)).map(i -> i.getDefaultInstance()).orElse(ItemStack.EMPTY));
		}
		cases++; check(problems, grid.stream().noneMatch(ItemStack::isEmpty), "gun_greasegun recipe part missing " + grid);
		var input = CraftingInput.of(3, 2, grid);
		cases++; check(problems, gunRecipe instanceof ShapedRecipe shaped && shaped.matches(input, level)
				&& shaped.assemble(input, server.registryAccess()).is(HbmItems.GUN_GREASEGUN.get())
				&& shaped.assemble(input, server.registryAccess()).getCount() == 1, "gun_greasegun recipe BRS/SMG failed");
		var tableRecipe = server.getRecipeManager().byKey(AtomicLegacy.id("machine_weapon_table")).map(holder -> holder.value()).orElse(null);
		cases++; check(problems, tableRecipe != null && tableRecipe.getResultItem(server.registryAccess()).is(HbmBlocks.MACHINE_WEAPON_TABLE.get().asItem()), "machine_weapon_table recipe not loaded");

		// Config values (XFactory9mm)
		GunConfig cfg = item.getConfig(gun, 0);
		Receiver rec = cfg.getReceivers(gun)[0];
		cases++; check(problems, cfg.getDurability(gun) == 3000F && cfg.getDrawDuration(gun) == 20 && cfg.getInspectDuration(gun) == 31, "greasegun dura/draw/inspect");
		cases++; check(problems, rec.getBaseDamage(gun) == 3F && rec.getDelayAfterFire(gun) == 4 && rec.getDelayAfterDryFire(gun) == 40
				&& rec.getRefireOnHold(gun) && rec.getInnateSpread(gun) == 0.015F && rec.getReloadBeginDuration(gun) == 60
				&& rec.getReloadEndDuration(gun) == 0 && rec.getJamDuration(gun) == 55, "greasegun receiver values");
		cases++; check(problems, rec.getMagazine(gun).getCapacity(gun) == 30, "greasegun magazine capacity");

		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.getInventory().clearContent();
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		player.getInventory().setItem(0, gun);
		player.getInventory().setItem(1, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.P9_SP).copyWithCount(64));
		player.getInventory().setItem(2, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.P9_AP).copyWithCount(10));
		gun = player.getInventory().getItem(0);
		IMagazine<?> mag = rec.getMagazine(gun);

		// Equip and draw
		tick(item, gun, player, 1);
		cases++; check(problems, ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.EQUIP && ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, "equip -> " + ItemGunBaseNT.getLastAnim(gun, 0) + "/" + ItemGunBaseNT.getState(gun, 0));

		// Reload: 60 ticks, loads 30 SP rounds, AP untouched
		item.handleKeybind(player, gun, GunKeybind.RELOAD, true);
		item.handleKeybind(player, gun, GunKeybind.RELOAD, false);
		cases++; check(problems, ItemGunBaseNT.getState(gun, 0) == GunState.RELOADING && ItemGunBaseNT.getTimer(gun, 0) == 60 && ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.RELOAD, "reload start");
		tick(item, gun, player, 59);
		cases++; check(problems, mag.getAmount(gun, null) == 0, "reload finished early");
		tick(item, gun, player, 1);
		cases++; check(problems, mag.getAmount(gun, null) == 30 && player.getInventory().getItem(1).getCount() == 34
				&& player.getInventory().getItem(2).getCount() == 10 && mag.getType(gun, null) == XFactory9mm.p9_sp
				&& ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.RELOAD_END, "reload result mag=" + mag.getAmount(gun, null) + " sp=" + player.getInventory().getItem(1).getCount());
		tick(item, gun, player, 1);
		cases++; check(problems, ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, "idle after reload");

		// Auto fire: one shot on press, then one every 4 ticks while held
		AABB area = new AABB(-8, 290, -8, 8, 310, 8);
		discardBullets(level, area);
		item.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
		cases++; check(problems, mag.getAmount(gun, null) == 29 && ItemGunBaseNT.getState(gun, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(gun, 0) == 4
				&& ItemGunBaseNT.getWear(gun, 0) == 1F && ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.CYCLE, "first shot");
		List<LegacyBulletEntity> bullets = level.getEntitiesOfClass(LegacyBulletEntity.class, area);
		cases++; check(problems, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.P9_SP && bullets.get(0).ownerEntity() == player, "bullet spawn " + bullets.size());
		tick(item, gun, player, 20);
		cases++; check(problems, mag.getAmount(gun, null) == 24 && level.getEntitiesOfClass(LegacyBulletEntity.class, area).size() == 6, "auto fire mag=" + mag.getAmount(gun, null));
		item.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
		tick(item, gun, player, 4);
		cases++; check(problems, ItemGunBaseNT.getState(gun, 0) == GunState.IDLE && mag.getAmount(gun, null) == 24, "release stops fire");
		discardBullets(level, area);

		// Bullet damage attributed to the shooter: 3 * 1.0 on an unarmored pig
		Pig pig = EntityType.PIG.create(level);
		if(pig != null) {
			pig.setNoAi(true);
			pig.setPos(0.5D, player.getEyeY() - 0.45D, 3.5D);
			level.addFreshEntity(pig);
			float before = pig.getHealth();
			LegacyBulletEntity bullet = Lego.spawnBullet(player, XFactory9mm.p9_sp, Lego.calcDamage(new ItemGunBaseNT.LambdaContext(cfg, player, player.getInventory(), 0), gun, rec, true, 0), 0F, -0.25D, -0.0625D * 2.5D, 1D);
			for(int i = 0; i < 3 && !bullet.isRemoved(); i++) bullet.tick();
			cases++; check(problems, Math.abs(before - pig.getHealth() - 3F) < 0.001F && pig.getLastHurtByMob() == player, "bullet damage " + (before - pig.getHealth()) + " attacker " + pig.getLastHurtByMob());
			pig.discard();
		}

		// Wear: damage halves at 100%, jam is certain after a reload
		ItemGunBaseNT.setWear(gun, 0, 3000F);
		var ctx = new ItemGunBaseNT.LambdaContext(cfg, player, player.getInventory(), 0);
		cases++; check(problems, Lego.calcDamage(ctx, gun, rec, true, 0) == 1.5F && Lego.calcSpread(ctx, gun, rec, XFactory9mm.p9_sp, true, 0, false) == 0.015F + 0.025F + 0.125F, "wear damage/spread");
		item.handleKeybind(player, gun, GunKeybind.RELOAD, true);
		item.handleKeybind(player, gun, GunKeybind.RELOAD, false);
		tick(item, gun, player, 60);
		cases++; check(problems, ItemGunBaseNT.getState(gun, 0) == GunState.JAMMED && ItemGunBaseNT.getTimer(gun, 0) == 55 && ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.JAMMED
				&& mag.getAmount(gun, null) == 30 && player.getInventory().getItem(1).getCount() == 28, "jam after reload " + ItemGunBaseNT.getState(gun, 0));
		tick(item, gun, player, 55);
		cases++; check(problems, ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, "jam clears after 55 ticks");
		ItemGunBaseNT.setWear(gun, 0, 0F);

		// Dry fire and inspect without ammo
		mag.setAmount(gun, 0);
		player.getInventory().setItem(1, ItemStack.EMPTY);
		player.getInventory().setItem(2, ItemStack.EMPTY);
		item.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, true);
		item.handleKeybind(player, gun, GunKeybind.GUN_PRIMARY, false);
		cases++; check(problems, ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.CYCLE_DRY && ItemGunBaseNT.getState(gun, 0) == GunState.DRAWING && ItemGunBaseNT.getTimer(gun, 0) == 40, "dry fire");
		tick(item, gun, player, 40);
		item.handleKeybind(player, gun, GunKeybind.RELOAD, true);
		item.handleKeybind(player, gun, GunKeybind.RELOAD, false);
		cases++; check(problems, ItemGunBaseNT.getLastAnim(gun, 0) == GunAnimation.INSPECT && ItemGunBaseNT.getState(gun, 0) == GunState.IDLE, "inspect without ammo");

		// Mixed ammo: empty mag loads the first accepted type found in the inventory (AP here)
		player.getInventory().setItem(3, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.P9_AP).copyWithCount(40));
		item.handleKeybind(player, gun, GunKeybind.RELOAD, true);
		item.handleKeybind(player, gun, GunKeybind.RELOAD, false);
		tick(item, gun, player, 60);
		cases++; check(problems, mag.getType(gun, null) == XFactory9mm.p9_ap && mag.getAmount(gun, null) == 30 && player.getInventory().getItem(3).getCount() == 10, "AP reload");
		cases++; check(problems, mag.getCasing(gun, null) == com.atomiclegacy.items.weapon.sedna.factory.GunEffects.CASING_P9_STEEL, "AP casing");

		// Unequip resets to DRAWING with the draw timer
		tick(item, gun, player, 2);
		item.inventoryTick(gun, level, player, 0, false);
		cases++; check(problems, ItemGunBaseNT.getState(gun, 0) == GunState.DRAWING && ItemGunBaseNT.getTimer(gun, 0) == 20 && !ItemGunBaseNT.getIsAiming(gun), "unequip reset");

		// M3 refurbishment (WeaponModGreasegun)
		ItemStack kit = new ItemStack(HbmItems.GREASEGUN_KIT.get());
		ItemStack clean = new ItemStack(HbmItems.GUN_GREASEGUN.get());
		cases++; check(problems, XWeaponModManager.isApplicable(clean, kit, 0, true), "M3 kit not applicable");
		XWeaponModManager.install(clean, 0, kit);
		GunConfig cleanCfg = item.getConfig(clean, 0);
		Receiver cleanRec = cleanCfg.getReceivers(clean)[0];
		cases++; check(problems, XWeaponModManager.hasUpgrade(clean, 0, XWeaponModManager.ID_GREASEGUN_CLEAN)
				&& cleanCfg.getDurability(clean) == 9000F && cleanRec.getBaseDamage(clean) == 5F && cleanRec.getInnateSpread(clean) == 0F
				&& cleanRec.getDelayAfterFire(clean) == 2 && cleanRec.getDelayAfterDryFire(clean) == 40
				&& cleanCfg.getOrchestra(clean) == WeaponModGreasegun.ORCHESTRA_GREASEGUN, "M3 values");
		cases++; check(problems, clean.getHoverName().getContents() instanceof TranslatableContents t && t.getKey().equals("item.atomiclegacy.gun_greasegun_m3"), "M3 name");
		cases++; check(problems, !XWeaponModManager.isApplicable(clean, kit, 0, true), "M3 FURNITURE mutex");
		cases++; check(problems, item.recognizedMods.contains(HbmItems.GREASEGUN_KIT.get()), "recognized mods");

		// Weapon modification table (ContainerWeaponTable)
		ItemStack tableGun = new ItemStack(HbmItems.GUN_GREASEGUN.get());
		WeaponTableMenu menu = new WeaponTableMenu(0, player.getInventory());
		menu.getSlot(7).set(tableGun);
		cases++; check(problems, menu.getSlot(0).mayPlace(kit) && !menu.getSlot(0).mayPlace(new ItemStack(HbmItems.AMMO_STANDARD.get())), "table mod slot validation");
		menu.getSlot(0).set(kit.copy());
		cases++; check(problems, !menu.getSlot(1).mayPlace(kit), "table mutex");
		ItemStack taken = menu.getSlot(7).getItem();
		menu.getSlot(7).set(ItemStack.EMPTY);
		menu.getSlot(7).onTake(player, taken);
		cases++; check(problems, XWeaponModManager.hasUpgrade(taken, 0, XWeaponModManager.ID_GREASEGUN_CLEAN) && menu.mods.getItem(0).isEmpty(), "table install on take");
		menu.getSlot(7).set(taken);
		cases++; check(problems, menu.mods.getItem(0).is(HbmItems.GREASEGUN_KIT.get()), "table shows installed mods");
		player.getInventory().clearContent();
		menu.removed(player);
		cases++; check(problems, menu.gun.isEmpty() && menu.mods.isEmpty() && !XWeaponModManager.hasUpgrade(taken, 0, XWeaponModManager.ID_GREASEGUN_CLEAN), "table close uninstalls and drops");
		for(var drop : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)) drop.discard();
		discardBullets(level, area);
		player.getInventory().clearContent();

		AtomicLegacy.LOGGER.info("HBM_GUN_AUDIT cases={} problems={}", cases, problems.size());
		return cases;
	}

	private static void tick(ItemGunBaseNT item, ItemStack stack, FakePlayer player, int ticks) {
		for(int i = 0; i < ticks; i++) item.inventoryTick(stack, player.level(), player, 0, true);
	}

	private static void discardBullets(ServerLevel level, AABB area) {
		for(LegacyBulletEntity bullet : level.getEntitiesOfClass(LegacyBulletEntity.class, area)) bullet.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
	}

	private static int indexOf(List<ItemStack> entries, net.minecraft.world.item.Item item) {
		for(int i = 0; i < entries.size(); i++) if(entries.get(i).is(item)) return i;
		return -1;
	}

	private static void check(List<String> problems, boolean ok, String message) {
		if(!ok) problems.add("gun: " + message);
	}
}
