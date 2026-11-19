package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.HbmSpecialWeaponItem;
import com.atomiclegacy.items.HbmToolBehavior;
import com.atomiclegacy.items.HbmToolItems;
import com.atomiclegacy.items.HbmToolProfile;
import com.atomiclegacy.items.HbmToolTier;
import com.atomiclegacy.items.LegacyChainsawItem;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity I3: diagnostics] Dedicated-server checks of the I3 batch (ability tools, ability / special melee weapons)
 * against the pinned 1.7.10 sources: ToolMaterial values, damage attributes, durability, ability lists and levels,
 * preset order and cycling, the area / harvest abilities on stone pads far from the origin, weapon abilities on a
 * zombie, special-weapon hits, power / fuel consumption, recipes and enchantability tags.
 */
final class ParityI3Diagnostics {
	private ParityI3Diagnostics() {
	}

	private static final int BASE_X = -9_000;
	private static final int BASE_Y = 220;
	private static final int BASE_Z = 9_000;
	private static final int SPACING = 48;
	private static int site;
	private static final List<int[]> FORCED = new ArrayList<>();

	/** id, attack damage (- = none), 1.7.10 getMaxDamage (0 = not damageable), enchantability, abilities (name:level). */
	private static final String[][] EXPECTED = {
			{"alloy_axe", "7.0", "2000", "5", "RECURSION:0;BEHEADER:0"},
			{"alloy_hoe", "-", "2000", "5", ""},
			{"alloy_pickaxe", "5.0", "2000", "5", "RECURSION:0"},
			{"alloy_shovel", "4.0", "2000", "5", "RECURSION:0"},
			{"alloy_sword", "8.0", "2000", "5", "STUN:0"},
			{"big_sword", "7.0", "1561", "10", ""},
			{"bismuth_axe", "25.0", "0", "200", "HAMMER:1;HAMMER_FLAT:1;RECURSION:1;SHREDDER:0;LUCK:1;SILK:0;STUN:3;VAMPIRE:1;BEHEADER:0"},
			{"bismuth_pickaxe", "15.0", "0", "200", "HAMMER:1;HAMMER_FLAT:1;RECURSION:1;SHREDDER:0;LUCK:1;SILK:0;STUN:2;VAMPIRE:0;BEHEADER:0"},
			{"bottle_opener", "4.5", "250", "200", ""},
			{"centri_stick", "3.0", "50", "2", "CENTRIFUGE:0"},
			{"chernobylsign", "9.0", "2000", "5", ""},
			{"chlorophyte_axe", "50.0", "0", "200", "HAMMER:1;HAMMER_FLAT:1;RECURSION:1;LUCK:3;STUN:4;VAMPIRE:3;BEHEADER:0"},
			{"chlorophyte_pickaxe", "20.0", "0", "200", "HAMMER:1;HAMMER_FLAT:1;RECURSION:1;LUCK:3;CENTRIFUGE:0;MERCURY:0;STUN:3;VAMPIRE:2;BEHEADER:0"},
			{"cmb_axe", "30.0", "8500", "100", "RECURSION:2;SMELTER:0;SILK:0;LUCK:2;BEHEADER:0"},
			{"cmb_hoe", "-", "8500", "100", ""},
			{"cmb_pickaxe", "10.0", "8500", "100", "RECURSION:2;SMELTER:0;SILK:0;LUCK:2"},
			{"cmb_shovel", "8.0", "8500", "100", "RECURSION:2;SMELTER:0;SILK:0;LUCK:2"},
			{"cmb_sword", "35.0", "8500", "100", "STUN:0;VAMPIRE:0"},
			{"cobalt_axe", "6.0", "750", "60", "RECURSION:1;SILK:0;LUCK:0;BEHEADER:0"},
			{"cobalt_decorated_axe", "8.0", "2500", "75", "RECURSION:1;HAMMER:0;HAMMER_FLAT:0;SILK:0;LUCK:2;BEHEADER:0"},
			{"cobalt_decorated_hoe", "-", "2500", "75", ""},
			{"cobalt_decorated_pickaxe", "6.0", "2500", "75", "RECURSION:1;HAMMER:0;HAMMER_FLAT:0;SILK:0;LUCK:2"},
			{"cobalt_decorated_shovel", "5.0", "2500", "75", "RECURSION:1;HAMMER:0;HAMMER_FLAT:0;SILK:0;LUCK:2"},
			{"cobalt_decorated_sword", "15.0", "2500", "75", "BOBBLE:0"},
			{"cobalt_hoe", "-", "750", "60", ""},
			{"cobalt_pickaxe", "4.0", "750", "60", "RECURSION:1;SILK:0;LUCK:0"},
			{"cobalt_shovel", "3.5", "750", "60", "RECURSION:1;SILK:0;LUCK:0"},
			{"cobalt_sword", "12.0", "750", "60", ""},
			{"crowbar", "6.0", "750", "10", ""},
			{"desh_axe", "7.5", "0", "10", "HAMMER:0;HAMMER_FLAT:0;RECURSION:0;SILK:0;LUCK:1;BEHEADER:0"},
			{"desh_hoe", "-", "0", "10", ""},
			{"desh_pickaxe", "5.0", "0", "10", "HAMMER:0;HAMMER_FLAT:0;RECURSION:0;SILK:0;LUCK:1"},
			{"desh_shovel", "4.0", "0", "10", "HAMMER:0;HAMMER_FLAT:0;RECURSION:0;SILK:0;LUCK:1"},
			{"desh_sword", "12.5", "0", "10", "STUN:0"},
			{"diamond_gavel", "7.0", "1561", "10", ""},
			{"dnt_sword", "12.0", "0", "200", ""},
			{"dwarven_pickaxe", "5.0", "250", "10", "HAMMER:0;HAMMER_FLAT:0"},
			{"elec_axe", "10.0", "0", "2", "HAMMER:0;HAMMER_FLAT:0;RECURSION:2;SILK:0;LUCK:1;CHAINSAW:0;BEHEADER:0"},
			{"elec_pickaxe", "6.0", "0", "2", "HAMMER:0;HAMMER_FLAT:0;RECURSION:2;SILK:0;LUCK:1"},
			{"elec_shovel", "5.0", "0", "2", "HAMMER:0;HAMMER_FLAT:0;RECURSION:2;SILK:0;LUCK:1"},
			{"elec_sword", "12.5", "0", "2", "STUN:2"},
			{"lead_gavel", "6.0", "750", "10", ""},
			{"memespoon", "6.0", "750", "10", ""},
			{"mese_axe", "75.0", "0", "200", "HAMMER:2;HAMMER_FLAT:2;RECURSION:2;SILK:0;LUCK:5;EXPLOSION:3;STUN:4;PHOSPHORUS:1;BEHEADER:0"},
			{"mese_gavel", "250.0", "0", "200", "PHOSPHORUS:0;RADIATION:2;STUN:3;VAMPIRE:4;BEHEADER:0"},
			{"mese_pickaxe", "35.0", "0", "200", "HAMMER:2;HAMMER_FLAT:2;RECURSION:2;CRYSTALLIZER:0;SILK:0;LUCK:5;EXPLOSION:3;STUN:3;PHOSPHORUS:0;BEHEADER:0"},
			{"meteorite_sword", "9.0", "0", "200", ""},
			{"meteorite_sword_alloyed", "17.5", "0", "200", ""},
			{"meteorite_sword_baleful", "75.0", "0", "200", ""},
			{"meteorite_sword_bred", "30.0", "0", "200", ""},
			{"meteorite_sword_etched", "25.0", "0", "200", ""},
			{"meteorite_sword_fused", "50.0", "0", "200", ""},
			{"meteorite_sword_hardened", "15.0", "0", "200", ""},
			{"meteorite_sword_irradiated", "35.0", "0", "200", ""},
			{"meteorite_sword_machined", "20.0", "0", "200", ""},
			{"meteorite_sword_reforged", "12.5", "0", "200", ""},
			{"meteorite_sword_seared", "10.0", "0", "200", ""},
			{"meteorite_sword_treated", "22.5", "0", "200", ""},
			{"redstone_sword", "5.0", "131", "5", ""},
			{"reer_graar", "6.5", "1000", "15", ""},
			{"schrabidium_axe", "25.0", "10000", "200", "RADIATION:0;HAMMER:1;HAMMER_FLAT:1;RECURSION:6;SILK:0;LUCK:4;SMELTER:0;SHREDDER:0;BEHEADER:0"},
			{"schrabidium_hammer", "1000000000.0", "0", "200", ""},
			{"schrabidium_hoe", "-", "10000", "200", ""},
			{"schrabidium_pickaxe", "20.0", "10000", "200", "RADIATION:0;HAMMER:1;HAMMER_FLAT:1;RECURSION:6;SILK:0;LUCK:4;SMELTER:0;SHREDDER:0"},
			{"schrabidium_shovel", "15.0", "10000", "200", "RADIATION:0;HAMMER:1;HAMMER_FLAT:1;RECURSION:6;SILK:0;LUCK:4;SMELTER:0;SHREDDER:0"},
			{"schrabidium_sword", "75.0", "10000", "200", "RADIATION:1;VAMPIRE:0"},
			{"shimmer_axe", "30.0", "0", "200", ""},
			{"shimmer_sledge", "30.0", "0", "200", ""},
			{"smashing_hammer", "12.0", "2500", "10", "SHREDDER:0"},
			{"sopsign", "9.0", "2000", "5", ""},
			{"starmetal_axe", "12.0", "3000", "100", "RECURSION:3;HAMMER:1;HAMMER_FLAT:1;SILK:0;LUCK:4;BEHEADER:0;STUN:1"},
			{"starmetal_hoe", "-", "3000", "100", ""},
			{"starmetal_pickaxe", "8.0", "3000", "100", "RECURSION:3;HAMMER:1;HAMMER_FLAT:1;SILK:0;LUCK:4;STUN:1"},
			{"starmetal_shovel", "7.0", "3000", "100", "RECURSION:3;HAMMER:1;HAMMER_FLAT:1;SILK:0;LUCK:4;STUN:1"},
			{"starmetal_sword", "25.0", "3000", "100", "BEHEADER:0;STUN:1;BOBBLE:0"},
			{"steel_axe", "5.0", "750", "10", "RECURSION:0;BEHEADER:0"},
			{"steel_hoe", "-", "750", "10", ""},
			{"steel_pickaxe", "4.0", "750", "10", "RECURSION:0"},
			{"steel_shovel", "3.0", "750", "10", "RECURSION:0"},
			{"steel_sword", "6.0", "750", "10", "STUN:0"},
			{"stopsign", "9.0", "2000", "5", ""},
			{"titanium_axe", "5.5", "1000", "15", "BEHEADER:0"},
			{"titanium_hoe", "-", "1000", "15", ""},
			{"titanium_pickaxe", "4.5", "1000", "15", ""},
			{"titanium_shovel", "3.5", "1000", "15", ""},
			{"titanium_sword", "6.5", "1000", "15", ""},
			{"ullapool_caber", "6.0", "750", "10", ""},
			{"volcanic_axe", "25.0", "0", "200", "HAMMER:1;HAMMER_FLAT:1;RECURSION:1;SMELTER:0;LUCK:2;SILK:0;FIRE:1;VAMPIRE:1;BEHEADER:0"},
			{"volcanic_pickaxe", "15.0", "0", "200", "HAMMER:1;HAMMER_FLAT:1;RECURSION:1;SMELTER:0;LUCK:2;SILK:0;FIRE:0;VAMPIRE:0;BEHEADER:0"},
			{"weapon_pipe_lead", "7.0", "250", "25", ""},
			{"wood_gavel", "4.0", "59", "15", ""},
			{"wrench_flipped", "16.0", "0", "2", ""},
	};

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		site = 0;
		try {
			materials(problems, cases);
			items(problems, cases);
			presets(level, problems, cases);
			areaAbilities(level, problems, cases);
			harvestAbilities(level, problems, cases);
			durability(level, problems, cases);
			weaponAbilities(level, problems, cases);
			specialWeapons(level, problems, cases);
			recipes(server, problems, cases);
		} catch (RuntimeException e) {
			problems.add("parity I3 diagnostics crashed: " + e);
		} finally {
			for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
			FORCED.clear();
		}
		return cases[0];
	}

	private static void materials(List<String> problems, int[] cases) {
		Object[][] mats = {
				{HbmToolTier.SCHRABIDIUM, 3, 10000, 50F, 100F, 200}, {HbmToolTier.SCHRABIDIUM_HAMMER, 3, 0, 50F, 999999996F, 200},
				{HbmToolTier.CHAINSAW, 3, 1500, 50F, 22F, 0}, {HbmToolTier.STEEL, 3, 750, 8F, 2F, 10}, {HbmToolTier.TITANIUM, 3, 1000, 9F, 2.5F, 15},
				{HbmToolTier.ALLOY, 3, 2000, 15F, 5F, 5}, {HbmToolTier.CMB, 3, 8500, 40F, 55F, 100}, {HbmToolTier.ELEC, 3, 0, 30F, 12F, 2},
				{HbmToolTier.DESH, 2, 0, 7.5F, 2F, 10}, {HbmToolTier.COBALT, 3, 750, 9F, 2.5F, 60}, {HbmToolTier.PIPE_LEAD, 1, 250, 1.5F, 3F, 25},
				{HbmToolTier.BOTTLE_OPENER, 1, 250, 1.5F, 0.5F, 200}, {HbmToolTier.SHIMMER, 1, 0, 25F, 26F, 200},
				{HbmToolTier.COBALT_DECORATED, 3, 2500, 15F, 2.5F, 75}, {HbmToolTier.STARMETAL, 3, 3000, 20F, 2.5F, 100},
				{HbmToolTier.BISMUTH, 4, 0, 50F, 0F, 200}, {HbmToolTier.VOLCANIC, 4, 0, 50F, 0F, 200}, {HbmToolTier.CHLOROPHYTE, 4, 0, 75F, 0F, 200},
				{HbmToolTier.MESE, 4, 0, 100F, 0F, 200}, {HbmToolTier.MESE_GAVEL, 4, 0, 50F, 0F, 200}, {HbmToolTier.DWARVEN, 2, 0, 4F, 0F, 10},
				{HbmToolTier.METEORITE, 4, 0, 50F, 0F, 200}, {HbmToolTier.WOOD_COMPAT, 0, 59, 2F, 0F, 15}, {HbmToolTier.STONE_COMPAT, 1, 131, 4F, 1F, 5},
				{HbmToolTier.DIAMOND_COMPAT, 3, 1561, 8F, 3F, 10}};
		for (Object[] m : mats) {
			HbmToolTier t = (HbmToolTier) m[0];
			check(problems, cases, t.legacyLevel() == (Integer) m[1] && t.getUses() == (Integer) m[2] && t.getSpeed() == (Float) m[3]
					&& t.getAttackDamageBonus() == (Float) m[4] && t.getEnchantmentValue() == (Integer) m[5], "material " + t);
		}
		String[][] repair = {{"STEEL", "ingot_steel"}, {"CMB", "ingot_combine_steel"}, {"DESH", "ingot_desh"}, {"STARMETAL", "ingot_starmetal"},
				{"MESE", "plate_paa"}, {"CHLOROPHYTE", "powder_chlorophyte"}, {"COBALT_DECORATED", "ingot_cobalt"}};
		for (String[] r : repair) {
			check(problems, cases, HbmToolTier.valueOf(r[0]).getRepairIngredient().test(new ItemStack(item(r[1]))), "repair item of " + r[0]);
		}
		check(problems, cases, HbmToolTier.ALLOY.getRepairIngredient().isEmpty() && HbmToolTier.ELEC.getRepairIngredient().isEmpty()
				&& HbmToolTier.COBALT.getRepairIngredient().isEmpty(), "materials without setRepairItem");
	}

	private static void items(List<String> problems, int[] cases) {
		TagKey<Item> mining = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/mining"));
		TagKey<Item> swords = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "swords"));
		for (String[] row : EXPECTED) {
			Item item = item(row[0]);
			ItemStack stack = new ItemStack(item);
			if (!row[1].equals("-")) {
				check(problems, cases, Math.abs(attackDamage(stack) - Float.parseFloat(row[1])) < 1e-3, row[0] + " attack damage " + row[1] + " got " + attackDamage(stack));
			}
			int uses = Integer.parseInt(row[2]);
			boolean damageable = stack.isDamageableItem();
			check(problems, cases, uses == 0 ? !damageable : damageable && stack.getMaxDamage() == uses, row[0] + " durability " + uses + " got " + (damageable ? stack.getMaxDamage() : 0));
			check(problems, cases, item.getEnchantmentValue() == Integer.parseInt(row[3]), row[0] + " enchantability");
			HbmToolProfile profile = profile(item);
			if (profile != null) {
				List<String> got = new ArrayList<>();
				for (var e : profile.areaAbilities()) got.add(e.ability() + ":" + e.level());
				for (var e : profile.harvestAbilities()) got.add(e.ability() + ":" + e.level());
				for (var e : profile.weaponAbilities()) got.add(e.ability() + ":" + e.level());
				List<String> want = row[4].isEmpty() ? List.of() : List.of(row[4].split(";"));
				check(problems, cases, got.size() == want.size() && got.containsAll(want), row[0] + " abilities " + want + " got " + got);
			}
			if (item instanceof HbmToolItems.AbilityTool) check(problems, cases, stack.is(mining), row[0] + " in enchantable/mining");
			if (item instanceof HbmToolItems.AbilitySword || item instanceof HbmSpecialWeaponItem) check(problems, cases, stack.is(swords), row[0] + " in minecraft:swords");
		}
		check(problems, cases, new ItemStack(item("schrabidium_pickaxe")).getRarity() == Rarity.RARE
				&& new ItemStack(item("shimmer_axe")).getRarity() == Rarity.EPIC && new ItemStack(item("ullapool_caber")).getRarity() == Rarity.UNCOMMON, "rarities");
		check(problems, cases, movement(new ItemStack(item("shimmer_sledge"))) == -0.2D && movement(new ItemStack(item("schrabidium_hammer"))) == -0.5D
				&& movement(new ItemStack(item("desh_pickaxe"))) == -0.05D && movement(new ItemStack(item("mese_gavel"))) == 1.5D, "movement modifiers (operation 1)");
		check(problems, cases, profile(item("chainsaw")).shears() && profile(item("elec_axe")).shears() && profile(item("mese_pickaxe")).rockBreaker(), "setShears / setDepthRockBreaker");
		List<Component> tooltip = new ArrayList<>();
		ItemStack pick = new ItemStack(item("schrabidium_pickaxe"));
		pick.getItem().appendHoverText(pick, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
		List<String> lines = tooltip.stream().map(Component::getString).toList();
		check(problems, cases, lines.size() == 13 && lines.get(0).equals("Abilities: ") && lines.get(1).endsWith(" (10)")
				&& lines.get(10).equals("Alt-click to open customization GUI!") && lines.get(11).equals("Weapon modifiers: ") && lines.get(12).endsWith(" (15.0)"), "schrabidium_pickaxe tooltip " + lines);
		tooltip.clear();
		ItemStack saw = new ItemStack(item("chainsaw"));
		saw.getItem().appendHoverText(saw, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
		check(problems, cases, tooltip.get(0).getString().equals("Fuel: 5000/5000mB") && tooltip.size() > 11 && tooltip.get(1).getString().startsWith("- "), "chainsaw fuel tooltip");
		tooltip.clear();
		ItemStack elec = new ItemStack(item("elec_pickaxe"));
		elec.getItem().appendHoverText(elec, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
		check(problems, cases, tooltip.get(0).getString().equals("Charge: 500.0k / 500.0k"), "elec_pickaxe charge tooltip " + tooltip.get(0).getString());
	}

	private static void presets(ServerLevel level, List<String> problems, int[] cases) {
		ItemStack pick = new ItemStack(item("schrabidium_pickaxe"));
		HbmToolProfile profile = profile(pick.getItem());
		HbmToolBehavior.Configuration config = HbmToolBehavior.getConfiguration(pick, profile);
		List<String> order = new ArrayList<>();
		for (HbmToolBehavior.Preset p : config.presets) order.add((p.area == null ? "-" : p.area + "" + p.areaLevel) + "/" + (p.harvest == null ? "-" : p.harvest + "" + p.harvestLevel));
		check(problems, cases, order.equals(List.of("-/-", "RECURSION6/-", "HAMMER1/-", "HAMMER_FLAT1/-", "-/SILK0", "-/LUCK4", "-/SMELTER0", "-/SHREDDER0")),
				"schrabidium_pickaxe preset order " + order);
		BlockPos pos = pad(level, 2, 1);
		FakePlayer player = player(level, pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, pick);
		pick.getItem().use(level, player, InteractionHand.MAIN_HAND);
		pick.getItem().use(level, player, InteractionHand.MAIN_HAND);
		check(problems, cases, HbmToolBehavior.getConfiguration(pick, profile).currentPreset == 2, "right click cycles presets");
		player.setShiftKeyDown(true);
		pick.getItem().use(level, player, InteractionHand.MAIN_HAND);
		player.setShiftKeyDown(false);
		check(problems, cases, HbmToolBehavior.getConfiguration(pick, profile).currentPreset == 0, "sneak right click returns to the first preset");
		ItemStack sword = new ItemStack(item("steel_sword"));
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		check(problems, cases, !sword.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "swords have no presets");
		net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
		data.putInt("ability", 1);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		net.minecraft.nbt.CompoundTag none = new net.minecraft.nbt.CompoundTag();
		none.putString("area", "");
		none.putString("harvest", "");
		net.minecraft.nbt.CompoundTag big = new net.minecraft.nbt.CompoundTag();
		big.putString("area", "tool.ability.recursion");
		big.putInt("areaLevel", 6);
		big.putString("harvest", "tool.ability.luck");
		big.putInt("harvestLevel", 5);
		list.add(none);
		list.add(big);
		data.put("abilityPresets", list);
		ItemStack cobalt = new ItemStack(item("cobalt_pickaxe"));
		HbmToolBehavior.receiveControl(cobalt, data, profile(cobalt.getItem()));
		HbmToolBehavior.Preset active = HbmToolBehavior.getConfiguration(cobalt, profile(cobalt.getItem())).active();
		check(problems, cases, active.area == HbmToolBehavior.AreaAbility.RECURSION && active.areaLevel == 1 && active.harvest == HbmToolBehavior.HarvestAbility.LUCK && active.harvestLevel == 0,
				"receiveControl / restrictTo clamps to the tool levels");
	}

	private static void select(ItemStack stack, HbmToolBehavior.AreaAbility area, HbmToolBehavior.HarvestAbility harvest) {
		HbmToolProfile profile = profile(stack.getItem());
		HbmToolBehavior.Configuration config = HbmToolBehavior.getConfiguration(stack, profile);
		for (int i = 0; i < config.presets.size(); i++) {
			HbmToolBehavior.Preset p = config.presets.get(i);
			if (p.area == area && p.harvest == harvest) {
				config.currentPreset = i;
				HbmToolBehavior.setConfiguration(stack, config);
				return;
			}
		}
		throw new IllegalStateException("preset " + area + "/" + harvest + " missing on " + stack);
	}

	private static void areaAbilities(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level, 3, 5);
		BlockPos center = pos.below(2);
		FakePlayer player = player(level, center.above(3));
		ItemStack tool = new ItemStack(item("cobalt_decorated_pickaxe"));
		select(tool, HbmToolBehavior.AreaAbility.HAMMER, null);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(center);
		check(problems, cases, countAir(level, center, 1) == 27, "HAMMER 1 breaks 27 blocks, got " + countAir(level, center, 1));
		check(problems, cases, countItems(level, center, Items.COBBLESTONE, 0.75) == 27, "HAMMER drops collected at the reference block");
		check(problems, cases, tool.getDamageValue() == 27, "HAMMER damages the tool once per block, got " + tool.getDamageValue());
		pos = pad(level, 3, 5);
		center = pos.below(1);
		player = player(level, center.above());
		player.moveTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 0, 90);
		tool = new ItemStack(item("cobalt_decorated_pickaxe"));
		select(tool, HbmToolBehavior.AreaAbility.HAMMER_FLAT, null);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(center);
		int flat = 0;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (level.getBlockState(center.offset(dx, 0, dz)).isAir()) flat++;
		check(problems, cases, flat == 9 && !level.getBlockState(center.below()).isAir(), "HAMMER_FLAT breaks the 3x3 face plane, got " + flat);
		pos = pad(level, 4, 4);
		center = pos.below(2);
		for (int i = -3; i <= 4; i++) level.setBlock(center.offset(i, 0, 0), Blocks.COAL_ORE.defaultBlockState(), 3);
		player = player(level, center.above(3));
		tool = new ItemStack(item("steel_pickaxe"));
		select(tool, HbmToolBehavior.AreaAbility.RECURSION, null);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(center);
		int coal = 0;
		for (int i = -4; i <= 4; i++) if (level.getBlockState(center.offset(i, 0, 0)).is(Blocks.COAL_ORE)) coal++;
		check(problems, cases, coal == 1 && level.getBlockState(center.offset(0, 1, 0)).is(Blocks.STONE), "RECURSION 3 mines the vein within radius 3, left " + coal);
		BlockPos stone = center.offset(0, 0, 2);
		player.gameMode.destroyBlock(stone);
		check(problems, cases, level.getBlockState(stone.offset(0, 0, 1)).is(Blocks.STONE), "RECURSION skips stone");
		pos = pad(level, 4, 6);
		center = pos.below(3);
		player = player(level, center.above(4));
		tool = new ItemStack(item("mese_pickaxe"));
		select(tool, HbmToolBehavior.AreaAbility.EXPLOSION, null);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(center);
		check(problems, cases, countAir(level, center, 1) >= 20, "EXPLOSION 15 clears the area, air " + countAir(level, center, 1));
	}

	private static void harvestAbilities(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level, 1, 2);
		BlockPos block = pos.below();
		FakePlayer player = player(level, pos);
		ItemStack tool = new ItemStack(item("cobalt_pickaxe"));
		select(tool, null, HbmToolBehavior.HarvestAbility.SILK);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(block);
		check(problems, cases, countItems(level, block, Items.STONE, 1.5) == 1 && tool.getEnchantments().isEmpty(), "SILK drops stone and removes the temporary enchantment");
		pos = pad(level, 1, 2);
		block = pos.below();
		level.setBlock(block, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
		player = player(level, pos);
		tool = new ItemStack(item("cobalt_decorated_pickaxe"));
		select(tool, null, HbmToolBehavior.HarvestAbility.LUCK);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(block);
		int diamonds = countItems(level, block, Items.DIAMOND, 1.5);
		check(problems, cases, diamonds >= 1 && diamonds <= 4 && tool.getEnchantments().isEmpty(), "LUCK 2 (fortune 3) drop " + diamonds);
		harvest(level, problems, cases, "cmb_pickaxe", HbmToolBehavior.HarvestAbility.SMELTER, Blocks.IRON_ORE.defaultBlockState(), new ItemStack(Items.IRON_INGOT));
		BlockState ore = Blocks.IRON_ORE.defaultBlockState();
		ItemStack shred = HbmToolBehavior.shredderResult(level, new ItemStack(Blocks.STONE));
		harvest(level, problems, cases, "smashing_hammer", HbmToolBehavior.HarvestAbility.SHREDDER, Blocks.STONE.defaultBlockState(),
				shred.is(item("scrap")) ? new ItemStack(Items.COBBLESTONE) : shred);
		List<ItemStack> centrifuge = HbmToolBehavior.centrifugeResult(level, new ItemStack(Blocks.IRON_ORE));
		harvest(level, problems, cases, "centri_stick", HbmToolBehavior.HarvestAbility.CENTRIFUGE, ore,
				centrifuge == null || centrifuge.isEmpty() ? new ItemStack(Items.RAW_IRON) : summed(centrifuge));
		ItemStack crystal = HbmToolBehavior.crystallizerResult(level, new ItemStack(Blocks.IRON_ORE));
		harvest(level, problems, cases, "mese_pickaxe", HbmToolBehavior.HarvestAbility.CRYSTALLIZER, ore, crystal == null ? new ItemStack(Items.RAW_IRON) : crystal);
		check(problems, cases, crystal != null && centrifuge != null, "iron ore has crystallizer (peroxide) and centrifuge recipes");
		pos = pad(level, 1, 2);
		block = pos.below();
		level.setBlock(block, Blocks.REDSTONE_ORE.defaultBlockState(), 3);
		player = player(level, pos);
		tool = new ItemStack(item("chlorophyte_pickaxe"));
		select(tool, null, HbmToolBehavior.HarvestAbility.MERCURY);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(block);
		int mercury = countItems(level, block, item("nugget_mercury"), 1.5);
		check(problems, cases, mercury >= 4 && mercury <= 8 && countItems(level, block, Items.REDSTONE, 1.5) == 0, "MERCURY drops 4-8 mercury, got " + mercury);
	}

	private static void harvest(ServerLevel level, List<String> problems, int[] cases, String toolId, HbmToolBehavior.HarvestAbility ability, BlockState state, ItemStack expected) {
		BlockPos pos = pad(level, 1, 2);
		BlockPos block = pos.below();
		level.setBlock(block, state, 3);
		FakePlayer player = player(level, pos);
		ItemStack tool = new ItemStack(item(toolId));
		select(tool, null, ability);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		player.gameMode.destroyBlock(block);
		int got = countItems(level, block, expected.getItem(), 1.5);
		check(problems, cases, level.getBlockState(block).isAir() && got == expected.getCount(), ability + " with " + toolId + " drops " + expected + ", got " + got);
	}

	private static void durability(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level, 2, 2);
		FakePlayer player = player(level, pos);
		ItemStack pick = new ItemStack(item("steel_pickaxe"));
		player.setItemInHand(InteractionHand.MAIN_HAND, pick);
		player.gameMode.destroyBlock(pos.below());
		Zombie zombie = zombie(level, pos);
		pick.getItem().postHurtEnemy(pick, zombie, player);
		check(problems, cases, pick.getDamageValue() == 2, "ability tool: 1 damage per block and per hit, got " + pick.getDamageValue());
		ItemStack sword = new ItemStack(item("steel_sword"));
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		player.gameMode.destroyBlock(pos.below().east());
		sword.getItem().postHurtEnemy(sword, zombie, player);
		check(problems, cases, sword.getDamageValue() == 3, "ability sword: 2 per block, 1 per hit, got " + sword.getDamageValue());
		ItemStack gavel = new ItemStack(item("wood_gavel"));
		gavel.getItem().postHurtEnemy(gavel, zombie, player);
		check(problems, cases, gavel.getDamageValue() == 0, "WeaponSpecial hits do not damage the stack");
		ItemStack elec = new ItemStack(item("elec_pickaxe"));
		HbmToolItems.AbilityItem ability = (HbmToolItems.AbilityItem) elec.getItem();
		check(problems, cases, ability.energy().charge(elec) == 500_000L && ability.canOperate(elec), "elec_pickaxe starts fully charged");
		player.setItemInHand(InteractionHand.MAIN_HAND, elec);
		player.gameMode.destroyBlock(pos.below().west());
		check(problems, cases, ability.energy().charge(elec) == 499_900L && !elec.isEmpty(), "elec_pickaxe uses 100 HE per block, got " + ability.energy().charge(elec));
		ability.energy().setCharge(elec, 99);
		check(problems, cases, !ability.canOperate(elec) && elec.getItem().getDestroySpeed(elec, Blocks.STONE.defaultBlockState()) == 1.0F, "elec_pickaxe below consumption cannot operate");
		ItemStack saw = new ItemStack(item("chainsaw"));
		check(problems, cases, LegacyChainsawItem.fuelAmount(saw) == 5000, "chainsaw starts full (getFill without NBT)");
		level.setBlock(pos.below().north(), Blocks.OAK_LOG.defaultBlockState(), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, saw);
		player.gameMode.destroyBlock(pos.below().north());
		check(problems, cases, LegacyChainsawItem.fuelAmount(saw) == 4999, "chainsaw uses 1 mB per block, got " + LegacyChainsawItem.fuelAmount(saw));
		LegacyChainsawItem.emptyTool(saw);
		var handler = ((LegacyChainsawItem) saw.getItem()).createFluidHandler(saw);
		var diesel = com.atomiclegacy.registry.HbmFluids.byName("diesel").orElseThrow();
		int filled = handler.fill(com.atomiclegacy.registry.HbmFluids.toNeoStack(diesel, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		check(problems, cases, filled == 250 && LegacyChainsawItem.fuelAmount(saw) == 250, "chainsaw fill rate 250 mB, got " + filled);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		zombie.discard();
	}

	private static void weaponAbilities(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level, 2, 1);
		FakePlayer player = player(level, pos);
		Zombie zombie = zombie(level, pos.east());
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.STUN, 0, level, player, zombie);
		MobEffectInstance slow = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
		check(problems, cases, slow != null && slow.getDuration() == 40 && slow.getAmplifier() == 4 && zombie.getEffect(MobEffects.WEAKNESS) != null, "STUN 0: 2 s slowness/weakness V");
		player.setHealth(10.0F);
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.VAMPIRE, 0, level, player, zombie);
		check(problems, cases, zombie.getHealth() == 18.0F && player.getHealth() == 12.0F, "VAMPIRE 0 drains 2 HP");
		float rad = HbmLivingRadiation.get(zombie).getEnvironmentRadiation();
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.RADIATION, 0, level, player, zombie);
		check(problems, cases, Math.abs(HbmLivingRadiation.get(zombie).getEnvironmentRadiation() - rad - 15.0F) < 1e-3, "RADIATION 0 contaminates 15 RAD");
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.FIRE, 1, level, player, zombie);
		check(problems, cases, zombie.getRemainingFireTicks() >= 199, "FIRE 1 sets 10 s of fire, got " + zombie.getRemainingFireTicks());
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.PHOSPHORUS, 1, level, player, zombie);
		MobEffectInstance phos = zombie.getEffect(HbmMobEffects.PHOSPHORUS_BURN);
		check(problems, cases, phos != null && phos.getDuration() == 1800 && phos.getAmplifier() == 4, "PHOSPHORUS 1: 90 s phosphorus V");
		zombie.setHealth(0.0F);
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.BEHEADER, 0, level, player, zombie);
		HbmToolBehavior.onHit(HbmToolBehavior.WeaponAbility.CHAINSAW, 1, level, player, zombie);
		AABB box = zombie.getBoundingBox().inflate(3);
		int heads = 0;
		int nitra = 0;
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
			if (e.getItem().is(Items.ZOMBIE_HEAD)) heads += e.getItem().getCount();
			if (e.getItem().is(item("nitra_small"))) nitra += e.getItem().getCount();
		}
		check(problems, cases, heads == 1, "BEHEADER drops a zombie head");
		check(problems, cases, nitra == 2, "CHAINSAW 1 (1:10) drops ceil(20/10) nitra_small, got " + nitra);
		zombie.discard();
	}

	private static void specialWeapons(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level, 2, 1);
		FakePlayer player = player(level, pos);
		Zombie zombie = zombie(level, pos.east());
		item("diamond_gavel").hurtEnemy(new ItemStack(item("diamond_gavel")), zombie, player);
		check(problems, cases, Math.abs(zombie.getHealth() - (20.0F - 20.0F / 3)) < 1e-3, "diamond_gavel removes 1/3 max health");
		zombie.setHealth(20.0F);
		item("shimmer_axe").hurtEnemy(new ItemStack(item("shimmer_axe")), zombie, player);
		check(problems, cases, zombie.getHealth() == 10.0F, "shimmer_axe halves health");
		item("lead_gavel").hurtEnemy(new ItemStack(item("lead_gavel")), zombie, player);
		MobEffectInstance lead = zombie.getEffect(HbmMobEffects.LEAD_POISONING);
		check(problems, cases, lead != null && lead.getDuration() == 300 && lead.getAmplifier() == 4, "lead_gavel: 15 s lead poisoning V");
		item("schrabidium_hammer").hurtEnemy(new ItemStack(item("schrabidium_hammer")), zombie, player);
		check(problems, cases, zombie.getHealth() == 0.0F, "schrabidium_hammer sets health 0");
		zombie.discard();
		BlockPos column = pos.below();
		level.setBlock(column.above(), Blocks.STONE.defaultBlockState(), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("shimmer_axe")));
		item("shimmer_axe").useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(column), net.minecraft.core.Direction.UP, column, false)));
		check(problems, cases, level.getBlockState(column).isAir() && level.getBlockState(column.above()).isAir() && level.getBlockState(column.below()).isAir()
				&& countItems(level, column, Items.COBBLESTONE, 2) == 0, "shimmer_axe clears 3 blocks without drops");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
		var saw = server.getRecipeManager().byKey(AtomicLegacy.id("chainsaw"));
		check(problems, cases, saw.isPresent() && LegacyChainsawItem.fuelAmount(saw.get().value().getResultItem(server.registryAccess())) == 0, "chainsaw recipe gives an empty tool");
		var match = server.getRecipeManager().byKey(AtomicLegacy.id("matchstick_red_phosphorus"));
		check(problems, cases, match.isPresent() && match.get().value().getResultItem(server.registryAccess()).getCount() == 24, "24 matchsticks from red phosphorus");
		for (String id : new String[]{"steel_pickaxe", "titanium_sword", "cmb_axe", "desh_shovel", "cobalt_hoe", "elec_sword", "elec_pickaxe", "elec_axe",
				"elec_shovel", "smashing_hammer", "meteorite_sword", "dwarven_pickaxe", "bismuth_pickaxe", "volcanic_axe", "chlorophyte_pickaxe", "mese_axe",
				"crowbar", "bottle_opener", "wood_gavel", "lead_gavel", "weapon_pipe_lead", "ullapool_caber"}) {
			Item target = item(id);
			check(problems, cases, server.getRecipeManager().getRecipes().stream().anyMatch(r -> r.value().getResultItem(server.registryAccess()).is(target)), "recipe for " + id);
		}
	}

	/** All centrifuge outputs of the first output's item, summed (each output stack is spawned separately). */
	private static ItemStack summed(List<ItemStack> results) {
		ItemStack first = results.get(0).copy();
		int count = 0;
		for (ItemStack stack : results) if (stack.is(first.getItem())) count += stack.getCount();
		first.setCount(count);
		return first;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
	}

	private static HbmToolProfile profile(Item item) {
		if (item instanceof HbmToolItems.AbilityItem ability) return ability.profile();
		if (item instanceof HbmSpecialWeaponItem special) return special.profile();
		return null;
	}

	private static float attackDamage(ItemStack stack) {
		ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		for (var entry : mods.modifiers()) if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) return (float) entry.modifier().amount();
		return Float.NaN;
	}

	private static double movement(ItemStack stack) {
		ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		for (var entry : mods.modifiers()) {
			if (entry.attribute().is(Attributes.MOVEMENT_SPEED) && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) return entry.modifier().amount();
		}
		return 0.0D;
	}

	private static int countAir(ServerLevel level, BlockPos center, int r) {
		int air = 0;
		for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) if (level.getBlockState(center.offset(dx, dy, dz)).isAir()) air++;
		return air;
	}

	private static int countItems(ServerLevel level, BlockPos pos, Item item, double radius) {
		int count = 0;
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(radius))) if (e.getItem().is(item)) count += e.getItem().getCount();
		return count;
	}

	private static Zombie zombie(ServerLevel level, BlockPos pos) {
		Zombie zombie = EntityType.ZOMBIE.create(level);
		zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		zombie.setNoAi(true);
		level.addFreshEntity(zombie);
		return zombie;
	}

	private static FakePlayer player(ServerLevel level, BlockPos pos) {
		FakePlayer player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
				java.util.UUID.nameUUIDFromBytes("atomiclegacy:parity_i3".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "[ParityI3]"));
		player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 90);
		player.setShiftKeyDown(false);
		player.getInventory().clearContent();
		return player;
	}

	private static BlockPos pad(ServerLevel level, int radius, int depth) {
		int index = site++;
		BlockPos pos = new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
		for (int cx = (pos.getX() - 16) >> 4; cx <= (pos.getX() + 16) >> 4; cx++) {
			for (int cz = (pos.getZ() - 16) >> 4; cz <= (pos.getZ() + 16) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				level.getChunk(cx, cz);
				FORCED.add(new int[]{cx, cz});
			}
		}
		long deadline = System.currentTimeMillis() + 20_000L;
		while (!level.isPositionEntityTicking(pos)) {
			if (System.currentTimeMillis() > deadline) throw new IllegalStateException("test chunk never became entity-ticking");
			if (!level.getChunkSource().pollTask()) Thread.onSpinWait();
		}
		for (int dx = -radius - 3; dx <= radius + 3; dx++) for (int dy = -depth - 2; dy <= 6; dy++) for (int dz = -radius - 3; dz <= radius + 3; dz++) {
			boolean stone = Math.abs(dx) <= radius && Math.abs(dz) <= radius && dy < 0 && dy >= -depth;
			level.setBlock(pos.offset(dx, dy, dz), stone ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(radius + 4))) e.discard();
		return pos;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if (!ok) problems.add("parity I3: " + message);
	}
}
