package com.atomiclegacy.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.B92BeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.SoliniumCloudEntity;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.handler.LegacyBobmazonOffers;
import com.atomiclegacy.items.GunB92CellItem;
import com.atomiclegacy.items.GunB92Item;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.WeaponizedCellItem;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryPA;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryRocket;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * [parity c1: diagnostics] Missing weapons pass: gun_b92 with its cell, the rigged cell and the beam entity, the power
 * armor controllers gun_pa_melee / gun_pa_ranged with the RPA / NCRPA components, and the two Bobmazon kits. Every
 * Fleija this pass could set off is suppressed by a field disturber entry that expires with the current tick.
 */
final class ParityC1Diagnostics {
	private ParityC1Diagnostics() {
	}

	private static final AABB AREA = new AABB(-24, 280, -24, 24, 320, 40);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		try {
			registration(server, level, problems, cases);
			recipes(server, level, problems, cases);
			b92(level, problems, cases);
			powerArmor(level, problems, cases);
			bobmazon(server, problems, cases);
		} catch(Exception exception) {
			problems.add("parity c1: diagnostics threw " + exception);
			AtomicLegacy.LOGGER.error("parity c1 diagnostics", exception);
		}
		return cases[0];
	}

	private static void registration(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		Item b92 = HbmItems.GUN_B92.get();
		ItemStack gun = new ItemStack(b92);
		double attack = gun.getAttributeModifiers().modifiers().stream()
				.filter(e -> e.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(e -> e.modifier().amount()).sum();
		check(problems, cases, b92 instanceof GunB92Item && gun.getMaxStackSize() == 1 && gun.getRarity() == Rarity.UNCOMMON && b92.getEnchantmentValue() == 1
				&& attack == 3.5D && gun.getUseDuration(null) == 72000 && !gun.isDamageableItem(), "gun_b92 item values attack=" + attack);
		check(problems, cases, HbmItems.GUN_B92_AMMO.get() instanceof GunB92CellItem && new ItemStack(HbmItems.GUN_B92_AMMO.get()).getMaxStackSize() == 1, "gun_b92_ammo");
		check(problems, cases, HbmItems.WEAPONIZED_STARBLASTER_CELL.get() instanceof WeaponizedCellItem
				&& new ItemStack(HbmItems.WEAPONIZED_STARBLASTER_CELL.get()).getMaxStackSize() == 1, "weaponized_starblaster_cell");
		check(problems, cases, BuiltInRegistries.ENTITY_TYPE.getKey(HbmEntityTypes.B92_BEAM.get()).equals(AtomicLegacy.id("entity_beam_bomb"))
				&& HbmEntityTypes.B92_BEAM.get().getWidth() == 0.5F && HbmEntityTypes.B92_BEAM.get().getHeight() == 0.5F, "entity_beam_bomb type");
		check(problems, cases, BuiltInRegistries.SOUND_EVENT.containsKey(AtomicLegacy.id("weapon.b92reload"))
				&& HbmSoundEvents.WEAPON_B92_RELOAD.get() != null, "weapon.b92reload sound");
		check(problems, cases, HbmConfig.DROPPED_STAR_CELL_EFFECT.getDefault(), "dropStar default true");
		check(problems, cases, HbmConfig.MELEE_RPA_SWINGS_DAMAGE.getDefault() == 15D && HbmConfig.MELEE_RPA_SLAP_DAMAGE.getDefault() == 35D
				&& HbmConfig.MELEE_RPA_LARGE_MULT.getDefault() == 2.5D && HbmConfig.MELEE_NCRPA_SWINGS_DAMAGE.getDefault() == 15D
				&& HbmConfig.MELEE_NCRPA_SWEEP_DAMAGE.getDefault() == 35D && HbmConfig.MELEE_NCRPA_LARGE_MULT.getDefault() == 2.5D
				&& HbmConfig.RANGED_NCRPA_DAMAGE.getDefault() == 25D, "XFactoryPA config defaults");

		Object[][] guns = { { HbmItems.GUN_PA_MELEE.get(), 10, Crosshair.NONE, true }, { HbmItems.GUN_PA_RANGED.get(), 0, Crosshair.CROSS, false } };
		for(Object[] g : guns) {
			Item item = (Item) g[0];
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			check(problems, cases, item instanceof XFactoryPA.ItemGunPA, name + " is not an ItemGunPA");
			if(!(item instanceof ItemGunBaseNT pa)) continue;
			ItemStack stack = new ItemStack(item);
			GunConfig cfg = pa.getConfig(stack, 0);
			List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
			item.appendHoverText(stack, Item.TooltipContext.EMPTY, tooltip, net.minecraft.world.item.TooltipFlag.NORMAL);
			check(problems, cases, stack.getMaxStackSize() == 1 && pa.quality == WeaponQuality.UTILITY && pa.getConfigCount() == 1
					&& cfg.getDrawDuration(stack) == (int) g[1] && cfg.getCrosshair(stack) == g[2] && cfg.getReceivers(stack).length == 1
					&& cfg.getReceivers(stack)[0].getMagazine(stack) == null && cfg.getDurability(stack) == 0F && tooltip.isEmpty()
					&& (cfg.getAnims(stack) != null) == (boolean) g[3] && (cfg.getOrchestra(stack) != null) == (boolean) g[3]
					&& !ItemGunBaseNT.secrets.contains(item), name + " config values");
		}

		List<ItemStack> tab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		if(!tab.isEmpty()) {
			int b = indexOf(tab, b92), pepper = indexOf(tab, HbmItems.GUN_PEPPERBOX.get()), drill = indexOf(tab, HbmItems.GUN_DRILL.get());
			int melee = indexOf(tab, HbmItems.GUN_PA_MELEE.get()), ranged = indexOf(tab, HbmItems.GUN_PA_RANGED.get());
			int cell = indexOf(tab, HbmItems.GUN_B92_AMMO.get()), rigged = indexOf(tab, HbmItems.WEAPONIZED_STARBLASTER_CELL.get());
			int ammo = indexOf(tab, HbmItems.AMMO_STANDARD.get());
			check(problems, cases, b >= 0 && b < pepper && drill < melee && melee == ranged - 1 && ranged < ammo && ammo < cell && cell < rigged,
					"weapon tab order " + b + "/" + pepper + " " + drill + "/" + melee + "/" + ranged + " " + ammo + "/" + cell + "/" + rigged);
		}

		// rocket_ncrpa (makeRPZB) / rocket_ncrpa_steer (makeNCR) rounds
		float[] mult = {1F, 0.5F, 0.75F, 0.75F, 0.75F};
		for(int i = 0; i < 5; i++) {
			StandardAmmoType plain = XFactoryRocket.NCRPA[i], steer = XFactoryRocket.NCRPA_STEER[i];
			check(problems, cases, plain.damageMultiplier() == mult[i] && steer.damageMultiplier() == mult[i]
					&& plain.onUpdate() == XFactoryRocket.UPDATE_STANDARD && steer.onUpdate() == XFactoryRocket.UPDATE_NCR
					&& plain.onImpact() == XFactoryRocket.RPZB[i].onImpact() && steer.onImpact() == XFactoryRocket.RPZB[i].onImpact()
					&& plain.family() == StandardAmmoItem.AmmoFamily.LEGACY_BULLET, "rocket_ncrpa round " + plain.id());
		}

		check(problems, cases, server.getAdvancements().get(AtomicLegacy.id("selenium")) != null && "selenium".equals(craftedAdvancement("gun_b92")),
				"gun_b92 crafting must award the selenium advancement");
	}

	@SuppressWarnings("unchecked")
	private static String craftedAdvancement(String item) {
		try {
			Field field = HbmAdvancements.class.getDeclaredField("CRAFTED_STACK_ADVANCEMENTS");
			field.setAccessible(true);
			return ((Map<ResourceLocation, String>) field.get(null)).get(AtomicLegacy.id(item));
		} catch(ReflectiveOperationException exception) {
			return "unreadable: " + exception;
		}
	}

	private static void recipes(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ItemStack e = ItemStack.EMPTY;
		ItemStack dnt = item("plate_dineutronium"), star = item("ingot_starmetal"), steel = item("plate_steel"), wire = item("wire_dense_gold");
		recipe(server, level, problems, cases, "gun_b92", 3, 3, List.of(dnt, dnt, dnt, star, star, item("circuit_bismoid"), e, e, item("gun_lasrifle")), HbmItems.GUN_B92.get(), true);
		recipe(server, level, problems, cases, "gun_b92", 3, 3, List.of(dnt, dnt, dnt, star, star, item("circuit_basic"), e, e, item("gun_lasrifle")), HbmItems.GUN_B92.get(), false);
		recipe(server, level, problems, cases, "gun_b92_ammo", 3, 3, List.of(steel, star, steel, item("powder_spark_mix"), star, item("powder_spark_mix"), steel, star, steel),
				HbmItems.GUN_B92_AMMO.get(), true);
		recipe(server, level, problems, cases, "gun_pa_melee", 3, 2, List.of(e, item("circuit_basic"), e, item("motor"), wire, item("motor")), HbmItems.GUN_PA_MELEE.get(), true);
		recipe(server, level, problems, cases, "gun_pa_ranged", 1, 3, List.of(item("circuit_basic"), wire, item("ingot_polymer")), HbmItems.GUN_PA_RANGED.get(), true);
		recipe(server, level, problems, cases, "gun_pa_ranged", 1, 3, List.of(item("circuit_basic"), wire, item("ingot_bakelite")), HbmItems.GUN_PA_RANGED.get(), true);
		recipe(server, level, problems, cases, "gun_pa_ranged", 1, 3, List.of(item("circuit_basic"), wire, item("ingot_steel")), HbmItems.GUN_PA_RANGED.get(), false);

		ItemStack tank = item("fluid_tank_full");
		CustomData.update(DataComponents.CUSTOM_DATA, tank, tag -> tag.putString("fluid", "peroxide"));
		ItemStack full = GunB92CellItem.getFullCell();
		ItemStack partial = new ItemStack(HbmItems.GUN_B92_AMMO.get());
		GunB92CellItem.setPower(partial, 24);
		check(problems, cases, GunB92CellItem.getPower(full) == 25, "getFullCell holds 25 charges");
		recipe(server, level, problems, cases, "weaponized_starblaster_cell", 3, 1, List.of(tank, full, item("wire_copper")), HbmItems.WEAPONIZED_STARBLASTER_CELL.get(), true);
		recipe(server, level, problems, cases, "weaponized_starblaster_cell", 3, 1, List.of(tank, partial, item("wire_copper")), HbmItems.WEAPONIZED_STARBLASTER_CELL.get(), false);
		recipe(server, level, problems, cases, "weaponized_starblaster_cell", 3, 1, List.of(tank, new ItemStack(HbmItems.GUN_B92_AMMO.get()), item("wire_copper")),
				HbmItems.WEAPONIZED_STARBLASTER_CELL.get(), false);
	}

	private static void b92(ServerLevel level, List<String> problems, int[] cases) {
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().clearContent();
		// statFacFleija returns a dead entity near a field disturber: no explosion or cloud for the rest of this tick
		NukeExplosionManager.registerFieldDisturber(level, BlockPos.containing(player.position()), 0);
		check(problems, cases, NukeExplosionManager.isFleijaSuppressed(level, BlockPos.containing(player.position())), "field disturber entry active");

		ItemStack gun = new ItemStack(HbmItems.GUN_B92.get());
		player.getInventory().setItem(0, gun);
		player.getInventory().selected = 0;

		// empty gun: the right click starts the charge animation; at step 15 it gains one charge, at 30 the animation ends
		gun.use(level, player, InteractionHand.MAIN_HAND);
		check(problems, cases, GunB92Item.getAnim(gun) == 1 && GunB92Item.getPower(gun) == 0 && !player.isUsingItem(), "b92 first click starts charging");
		tick(player, gun, 15);
		check(problems, cases, GunB92Item.getAnim(gun) == 16 && GunB92Item.getPower(gun) == 1, "b92 charge at step 15: anim=" + GunB92Item.getAnim(gun) + " power=" + GunB92Item.getPower(gun));
		GunB92Item.setAnim(gun, 15);
		check(problems, cases, Math.abs(GunB92Item.getRotationFromAnim(gun) - 0.0174533F * 7.5F * 5) < 1.0E-6F && GunB92Item.getTransFromAnim(gun) == 0F, "b92 anim 15 rotation");
		GunB92Item.setAnim(gun, 17);
		check(problems, cases, Math.abs(GunB92Item.getTransFromAnim(gun) - 0.1F) < 1.0E-6F, "b92 anim 17 pump offset");
		GunB92Item.setAnim(gun, 28);
		check(problems, cases, Math.abs(GunB92Item.getRotationFromAnim(gun) - 0.0174533F * 7.5F * 2) < 1.0E-6F, "b92 anim 28 rotation");
		GunB92Item.setAnim(gun, 16);
		tick(player, gun, 15);
		check(problems, cases, GunB92Item.getAnim(gun) == 0 && GunB92Item.getPower(gun) == 1, "b92 animation ends at step 30");

		// overcharge: the eleventh charge resets the gun (and detonates a range 50 Fleija, suppressed here)
		GunB92Item.setPower(gun, 10);
		GunB92Item.setAnim(gun, 1);
		tick(player, gun, 15);
		check(problems, cases, GunB92Item.getPower(gun) == 0, "b92 overcharge resets the energy");
		tick(player, gun, 15);

		// three charges, then a shot: needs 10 ticks of draw, fires one beam per charge
		GunB92Item.setPower(gun, 3);
		gun.use(level, player, InteractionHand.MAIN_HAND);
		check(problems, cases, player.isUsingItem() && GunB92Item.getAnim(gun) == 0, "b92 charged click draws the gun");
		gun.releaseUsing(level, player, 72000 - 5);
		check(problems, cases, beams(level).isEmpty() && GunB92Item.getPower(gun) == 3, "b92 released before 10 ticks does not fire");
		gun.releaseUsing(level, player, 72000 - 20);
		player.stopUsingItem();
		List<B92BeamEntity> beams = beams(level);
		B92BeamEntity first = beams.stream().filter(b -> Math.abs(b.getDeltaMovement().length() - 4.5D) < 0.05D).findFirst().orElse(null);
		check(problems, cases, beams.size() == 3 && first != null && GunB92Item.getPower(gun) == 0 && GunB92Item.getAnim(gun) == 1,
				"b92 shot beams=" + beams.size() + " power=" + GunB92Item.getPower(gun));
		if(first != null) {
			Vec3 motion = first.getDeltaMovement();
			check(problems, cases, Math.abs(first.getY() - (player.getEyeY() - 0.10000000149011612D)) < 1.0E-6D && Math.abs(first.getX() - (0.5D - 0.16D)) < 1.0E-5D
					&& motion.z > 4.4D && Math.abs(first.getYRot()) < 1F && Math.abs(first.getXRot()) < 1F, "b92 beam origin and heading " + first.position() + " " + motion);
			for(B92BeamEntity other : beams) if(other != first) other.discard();

			// a wall ahead: the beam records the block, flies on and goes off one tick later
			for(int x = -2; x <= 2; x++) for(int y = 299; y <= 304; y++) level.setBlockAndUpdate(new BlockPos(x, y, 12), Blocks.STONE.defaultBlockState());
			int ticks = 0;
			while(!first.isRemoved() && ticks < 20) { first.tickCount++; first.tick(); ticks++; }
			check(problems, cases, first.isRemoved() && ticks == 4, "b92 beam goes off the tick after reaching a block, ticks=" + ticks);
			for(int x = -2; x <= 2; x++) for(int y = 299; y <= 304; y++) level.setBlockAndUpdate(new BlockPos(x, y, 12), Blocks.AIR.defaultBlockState());
		}
		for(B92BeamEntity beam : beams(level)) beam.discard();

		// a beam with nothing in its way is removed after 100 ticks
		B92BeamEntity free = new B92BeamEntity(level, player, 3.0F);
		free.setDeltaMovement(0, 0.01D, 0);
		int life = 0;
		while(!free.isRemoved() && life < 200) { free.tickCount++; free.tick(); life++; }
		check(problems, cases, life == 101, "b92 beam lifetime " + life);

		// cell: drains the gun down to one charge, one per tick
		GunB92Item.setAnim(gun, 0);
		GunB92Item.setPower(gun, 5);
		ItemStack cell = new ItemStack(HbmItems.GUN_B92_AMMO.get());
		player.getInventory().setItem(1, cell);
		for(int i = 0; i < 8; i++) cell.getItem().inventoryTick(cell, level, player, 1, false);
		check(problems, cases, GunB92CellItem.getPower(cell) == 4 && GunB92Item.getPower(gun) == 1, "b92 cell drain cell=" + GunB92CellItem.getPower(cell) + " gun=" + GunB92Item.getPower(gun));
		GunB92CellItem.setPower(cell, 24);
		GunB92Item.setPower(gun, 9);
		for(int i = 0; i < 8; i++) cell.getItem().inventoryTick(cell, level, player, 1, false);
		check(problems, cases, GunB92CellItem.getPower(cell) == 25 && GunB92Item.getPower(gun) == 8
				&& ItemStack.isSameItemSameComponents(cell, GunB92CellItem.getFullCell()), "b92 cell stops at 25 and equals the full cell of the recipe");

		// rigged cell on the floor: consumed after 50 seconds
		ItemEntity young = new ItemEntity(level, 0.5D, 300D, 0.5D, new ItemStack(HbmItems.WEAPONIZED_STARBLASTER_CELL.get()));
		young.tickCount = 1000;
		young.getItem().onEntityItemUpdate(young);
		ItemEntity old = new ItemEntity(level, 0.5D, 300D, 0.5D, new ItemStack(HbmItems.WEAPONIZED_STARBLASTER_CELL.get()));
		old.tickCount = 1001;
		old.getItem().onEntityItemUpdate(old);
		check(problems, cases, !young.isRemoved() && old.isRemoved(), "rigged cell timer");
		check(problems, cases, level.getEntitiesOfClass(SoliniumCloudEntity.class, AREA).isEmpty(), "suppressed Fleija left a cloud");
		player.getInventory().clearContent();
	}

	private static void powerArmor(ServerLevel level, List<String> problems, int[] cases) {
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(0.5D, 300.0D, 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		for(EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) player.setItemSlot(slot, ItemStack.EMPTY);

		// no armor: the controllers do nothing
		ItemStack melee = equip(player, HbmItems.GUN_PA_MELEE.get());
		check(problems, cases, XFactoryPA.getMeleeComponentCommon(player) == null && XFactoryPA.getRangedComponentCommon(player) == null, "no component without armor");
		press(player, melee, GunKeybind.GUN_PRIMARY);
		release(player, melee, GunKeybind.GUN_PRIMARY);
		check(problems, cases, ItemGunBaseNT.getState(melee, 0) == GunState.IDLE, "melee controller without armor stays idle");

		// incomplete set: still nothing
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HbmItems.RPA_PLATE.get()));
		check(problems, cases, XFactoryPA.getMeleeComponentCommon(player) == null, "rpa chestplate alone gives no component");

		// RPA: melee only; swings at 3 and 9 (15 damage, x2.5 on 100+ health), slap at 8 (35 damage)
		wear(player, HbmItems.RPA_HELMET.get(), HbmItems.RPA_PLATE.get(), HbmItems.RPA_LEGS.get(), HbmItems.RPA_BOOTS.get());
		check(problems, cases, XFactoryPA.getMeleeComponentCommon(player) == XFactoryPA.RPA_MELEE && XFactoryPA.getRangedComponentCommon(player) == null, "rpa components");
		Pig pig = pig(level, new Vec3(0.5D, player.getEyeY() - 0.45D, 2.5D));
		if(pig != null) {
			press(player, melee, GunKeybind.GUN_PRIMARY);
			release(player, melee, GunKeybind.GUN_PRIMARY);
			check(problems, cases, ItemGunBaseNT.getState(melee, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(melee, 0) == 14
					&& ItemGunBaseNT.getLastAnim(melee, 0) == GunAnimation.CYCLE, "rpa swing starts a 14 tick cooldown");
			tick(player, melee, 3);
			check(problems, cases, pig.getHealth() == 1000F, "rpa swing hits before tick 3");
			tick(player, melee, 1);
			float afterFirst = pig.getHealth();
			tick(player, melee, 6);
			float afterSecond = pig.getHealth();
			check(problems, cases, Math.abs(afterFirst - (1000F - 37.5F)) < 0.01F && Math.abs(afterSecond - (1000F - 75F)) < 0.01F,
					"rpa swings health " + afterFirst + " / " + afterSecond);
			tick(player, melee, 12);
			check(problems, cases, ItemGunBaseNT.getState(melee, 0) == GunState.IDLE, "rpa swing returns to idle " + ItemGunBaseNT.getState(melee, 0));
			press(player, melee, GunKeybind.GUN_SECONDARY);
			release(player, melee, GunKeybind.GUN_SECONDARY);
			check(problems, cases, ItemGunBaseNT.getTimer(melee, 0) == 20 && ItemGunBaseNT.getLastAnim(melee, 0) == GunAnimation.ALT_CYCLE, "rpa slap starts a 20 tick cooldown");
			tick(player, melee, 9);
			check(problems, cases, Math.abs(pig.getHealth() - (1000F - 75F - 87.5F)) < 0.01F, "rpa slap health " + pig.getHealth());
			tick(player, melee, 15);

			// held primary: the swing repeats at animation tick 14
			press(player, melee, GunKeybind.GUN_PRIMARY);
			tick(player, melee, 15);
			check(problems, cases, ItemGunBaseNT.getState(melee, 0) == GunState.COOLDOWN && ItemGunBaseNT.getAnimTimer(melee, 0) == 1, "rpa held swing refires");
			release(player, melee, GunKeybind.GUN_PRIMARY);
			tick(player, melee, 20);

			// NCRPA: swings at 5 and 15, cooldown 25; sweep at 5, cooldown 30
			wear(player, HbmItems.NCRPA_HELMET.get(), HbmItems.NCRPA_PLATE.get(), HbmItems.NCRPA_LEGS.get(), HbmItems.NCRPA_BOOTS.get());
			check(problems, cases, XFactoryPA.getMeleeComponentCommon(player) == XFactoryPA.NCRPA_MELEE && XFactoryPA.getRangedComponentCommon(player) == XFactoryPA.NCRPA_RANGED,
					"ncrpa components");
			pig.setHealth(1000F);
			press(player, melee, GunKeybind.GUN_PRIMARY);
			release(player, melee, GunKeybind.GUN_PRIMARY);
			check(problems, cases, ItemGunBaseNT.getTimer(melee, 0) == 25, "ncrpa swing cooldown 25");
			tick(player, melee, 6);
			float ncrFirst = pig.getHealth();
			tick(player, melee, 10);
			check(problems, cases, Math.abs(ncrFirst - (1000F - 37.5F)) < 0.01F && Math.abs(pig.getHealth() - (1000F - 75F)) < 0.01F, "ncrpa swings health " + ncrFirst + " / " + pig.getHealth());
			tick(player, melee, 15);
			press(player, melee, GunKeybind.GUN_SECONDARY);
			release(player, melee, GunKeybind.GUN_SECONDARY);
			check(problems, cases, ItemGunBaseNT.getTimer(melee, 0) == 30, "ncrpa sweep cooldown 30");
			tick(player, melee, 6);
			check(problems, cases, Math.abs(pig.getHealth() - (1000F - 75F - 87.5F)) < 0.01F, "ncrpa sweep health " + pig.getHealth());
			pig.discard();
		}

		// NCRPA ranged: primary fires the steered rocket, secondary the plain one, both from the inventory, 10 tick cooldown
		ItemStack ranged = equip(player, HbmItems.GUN_PA_RANGED.get(), StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.ROCKET_HE).copyWithCount(2));
		wear(player, HbmItems.NCRPA_HELMET.get(), HbmItems.NCRPA_PLATE.get(), HbmItems.NCRPA_LEGS.get(), HbmItems.NCRPA_BOOTS.get());
		int configsBefore = com.atomiclegacy.items.weapon.sedna.BulletConfig.configs.size();
		press(player, ranged, GunKeybind.GUN_PRIMARY);
		release(player, ranged, GunKeybind.GUN_PRIMARY);
		List<LegacyBulletEntity> bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_NCRPA_STEER_HE && Math.abs(bullets.get(0).baseDamage() - 25F) < 0.001F
				&& countAmmo(player, StandardAmmoType.ROCKET_HE) == 1 && ItemGunBaseNT.getState(ranged, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(ranged, 0) == 10,
				"ncrpa steered rocket n=" + bullets.size());
		discardBullets(level);
		press(player, ranged, GunKeybind.GUN_SECONDARY);
		release(player, ranged, GunKeybind.GUN_SECONDARY);
		check(problems, cases, bullets(level).isEmpty(), "ncrpa rocket during cooldown");
		tick(player, ranged, 11);
		press(player, ranged, GunKeybind.GUN_SECONDARY);
		release(player, ranged, GunKeybind.GUN_SECONDARY);
		bullets = bullets(level);
		check(problems, cases, bullets.size() == 1 && bullets.get(0).ammoType() == StandardAmmoType.ROCKET_NCRPA_HE && countAmmo(player, StandardAmmoType.ROCKET_HE) == 0,
				"ncrpa plain rocket n=" + bullets.size());
		discardBullets(level);
		tick(player, ranged, 11);
		press(player, ranged, GunKeybind.GUN_PRIMARY);
		release(player, ranged, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).isEmpty() && ItemGunBaseNT.getState(ranged, 0) == GunState.COOLDOWN && ItemGunBaseNT.getTimer(ranged, 0) == 10, "ncrpa dry click");
		int created = com.atomiclegacy.items.weapon.sedna.BulletConfig.configs.size() - configsBefore;
		check(problems, cases, created == 0 || created == 10, "ncrpa bullet configs are appended once, added " + created);
		discardBullets(level);

		// RPA has no ranged component
		wear(player, HbmItems.RPA_HELMET.get(), HbmItems.RPA_PLATE.get(), HbmItems.RPA_LEGS.get(), HbmItems.RPA_BOOTS.get());
		player.getInventory().setItem(1, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.ROCKET_HE));
		tick(player, ranged, 11);
		press(player, ranged, GunKeybind.GUN_PRIMARY);
		release(player, ranged, GunKeybind.GUN_PRIMARY);
		check(problems, cases, bullets(level).isEmpty() && ItemGunBaseNT.getState(ranged, 0) == GunState.IDLE, "rpa ranged controller does nothing");

		for(EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) player.setItemSlot(slot, ItemStack.EMPTY);
		player.getInventory().clearContent();
	}

	private static void bobmazon(MinecraftServer server, List<String> problems, int[] cases) {
		List<LegacyBobmazonOffers.Offer> hidden = LegacyBobmazonOffers.offers(true, server.registryAccess());
		int fusion = -1, maid = -1, nut = -1, frenchie = -1;
		for(int i = 0; i < hidden.size(); i++) {
			String name = hidden.get(i).stack().getHoverName().getString();
			if(name.equals("Fusion Man")) fusion = i;
			if(name.equals("Maid's Cleaning Utensils")) maid = i;
			if(name.equals("The Nut Bucket")) nut = i;
			if(name.equals("Frenchie's Reward")) frenchie = i;
		}
		check(problems, cases, fusion >= 0 && maid == fusion + 1 && nut == maid + 1 && frenchie == nut + 1 && frenchie == hidden.size() - 1,
				"Bobmazon kit order " + fusion + "/" + maid + "/" + nut + "/" + frenchie);
		if(maid >= 0 && frenchie >= 0) {
			check(problems, cases, hidden.get(maid).caps() == 64 && hidden.get(frenchie).caps() == 32
					&& hidden.get(maid).requirement() == LegacyBobmazonOffers.Requirement.HIDDEN && hidden.get(frenchie).requirement() == LegacyBobmazonOffers.Requirement.HIDDEN
					&& hidden.get(maid).stack().has(DataComponents.LORE),
					"Bobmazon kit prices " + hidden.get(maid).caps() + "/" + hidden.get(frenchie).caps() + " " + hidden.get(maid).requirement() + "/" + hidden.get(frenchie).requirement()
							+ " lore " + hidden.get(maid).stack().has(DataComponents.LORE) + "/" + hidden.get(frenchie).stack().has(DataComponents.LORE));
		}
	}

	private static void recipe(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases, String id, int width, int height, List<ItemStack> grid, Item result, boolean expect) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(h -> h.value()).orElse(null);
		var craft = CraftingInput.of(width, height, new ArrayList<>(grid));
		boolean ok = recipe instanceof CraftingRecipe c && c.matches(craft, level) == expect && (!expect || c.assemble(craft, server.registryAccess()).is(result));
		check(problems, cases, ok, id + " recipe" + (expect ? "" : " rejects a wrong grid"));
	}

	private static void wear(FakePlayer player, Item helmet, Item plate, Item legs, Item boots) {
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(plate));
		player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
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
		player.getInventory().selected = 0;
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

	private static ItemStack item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
	}

	private static List<B92BeamEntity> beams(ServerLevel level) {
		return level.getEntitiesOfClass(B92BeamEntity.class, AREA, b -> !b.isRemoved());
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
		if(!ok) problems.add("parity c1: " + message);
	}
}
