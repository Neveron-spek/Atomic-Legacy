package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.DeadPlantBlock;
import com.atomiclegacy.blocks.HempPlantBlock;
import com.atomiclegacy.blocks.MustardWillowPlantBlock;
import com.atomiclegacy.entity.LegacyCloudTomEntity;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.foundry.FoundryShape;
import com.atomiclegacy.items.FoundryMoldItem;
import com.atomiclegacy.radiation.HbmHazardRegistry;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.worldgen.NuclearFalloutTerrainEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * [parity d2: diagnostics] Miscellaneous pass: ItemAutogen(NUGGET) nuggets, the plant_flower / plant_tall chain,
 * EntityCloudTom, EntityFallingBlockNT users, ItemSoup / ItemFood values and the client/packet commands.
 */
final class ParityD2MiscDiagnostics {
	private ParityD2MiscDiagnostics() {
	}

	/** Test column; everything placed here is removed again before returning. */
	private static final BlockPos ORIGIN = new BlockPos(10, 300, -10);
	private static final AABB AREA = new AABB(ORIGIN).inflate(12.0D);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		try {
			level.getChunk(ORIGIN);
			nuggets(server, level, problems, cases);
			plants(level, problems, cases);
			cloudTom(level, problems, cases);
			falling(level, problems, cases);
			food(problems, cases);
			commands(server, problems, cases);
		} catch(Exception exception) {
			problems.add("parity d2 misc: diagnostics threw " + exception);
			AtomicLegacy.LOGGER.error("parity d2 misc diagnostics", exception);
		} finally {
			cleanup(level);
		}
		return cases[0];
	}

	// ---------------------------------------------------------------- 1. nugget_raw

	/** Mats#setAutogen(..., NUGGET, ...): material, ingot the 1:9 pair is built from (first "ingot" + name ore). */
	private static final String[][] RAW_NUGGETS = {
			{"titanium", "atomiclegacy:ingot_titanium"},
			{"copper", "minecraft:copper_ingot"},
			{"tungsten", "atomiclegacy:ingot_tungsten"},
			{"aluminium", "atomiclegacy:ingot_aluminium"},
			{"boron", "atomiclegacy:ingot_boron"},
			{"sodium", "atomiclegacy:ingot_raw_sodium"},
			{"strontium", "atomiclegacy:ingot_raw_strontium"},
			{"lithium", "atomiclegacy:lithium"},
	};

	private static void nuggets(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		FoundryMoldItem.Mold nuggetMold = null;
		for(FoundryMoldItem.Mold mold : FoundryMoldItem.molds()) {
			if(mold.name().equals("nugget")) nuggetMold = mold;
		}
		check(problems, cases, nuggetMold != null, "nugget mold is registered");

		for(String[] row : RAW_NUGGETS) {
			String material = row[0];
			String id = "nugget_raw_" + material;
			Item nugget = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).orElse(null);
			Item ingot = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(row[1])).orElse(null);
			check(problems, cases, nugget != null, id + " is registered");
			check(problems, cases, ingot != null, row[1] + " is registered");
			if(nugget == null || ingot == null) continue;

			ItemStack stack = new ItemStack(nugget);
			check(problems, cases, stack.getMaxStackSize() == 64, id + " stacks to 64");
			TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "nuggets/" + material));
			check(problems, cases, stack.is(tag), id + " is in c:nuggets/" + material);

			// MaterialShapes.NUGGET = 8 quanta
			Optional<FoundryMaterialStack> molten = FoundryMaterial.materialFromItem(stack);
			check(problems, cases, molten.isPresent() && molten.get().material().name().equals(material) && molten.get().amount() == 8
					&& FoundryShape.NUGGET.q(1) == 8, id + " melts into 8 quanta of " + material + " (" + molten + ")");

			// MoldShape#getOutput
			FoundryMaterial foundry = FoundryMaterial.byName(material).orElse(null);
			Optional<ItemStack> cast = nuggetMold == null || foundry == null ? Optional.empty() : nuggetMold.outputFor(foundry);
			check(problems, cases, cast.isPresent() && cast.get().is(nugget) && cast.get().getCount() == 1, "nugget mold casts " + id + " (" + cast + ")");

			// OreDictManager: add1To9Pair(ingot, nugget_raw x9)
			recipe(server, level, problems, cases, id + "_from_ingot", 1, 1, List.of(new ItemStack(ingot)), nugget, 9);
			recipe(server, level, problems, cases, id + "_to_ingot", 3, 3, Collections.nCopies(9, new ItemStack(nugget)), ingot, 1);

			// LI/NA .hydro(1F).hazNugget(), SR .hot(1F).hydro(1F).hazNugget(); nugget multiplier 0.1
			float hydro = material.equals("lithium") || material.equals("sodium") || material.equals("strontium") ? 0.1F : 0.0F;
			float hot = material.equals("strontium") ? 0.1F : 0.0F;
			check(problems, cases, near(HbmHazardRegistry.level(stack, HbmHazardRegistry.Type.HYDROACTIVE), hydro), id + " hydroactive " + hydro);
			check(problems, cases, near(HbmHazardRegistry.level(stack, HbmHazardRegistry.Type.HOT), hot), id + " hot " + hot);
			check(problems, cases, near(HbmHazardRegistry.level(stack, HbmHazardRegistry.Type.RADIATION), 0.0F), id + " is not radioactive");
		}
	}

	// ---------------------------------------------------------------- 2. plant_flower / plant_tall

	private static void plants(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos soil = ORIGIN;
		BlockPos plant = soil.above();
		BlockPos water = soil.east();

		// EnumFlowerType FOXGLOVE / TOBACCO / NIGHTSHADE: always growable, func_149853_b drops the flower again
		for(Block flower : List.of(HbmBlocks.PLANT_FOXGLOVE.get(), HbmBlocks.PLANT_TOBACCO.get(), HbmBlocks.PLANT_NIGHTSHADE.get())) {
			String name = BuiltInRegistries.BLOCK.getKey(flower).getPath();
			reset(level);
			level.setBlock(soil, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			level.setBlock(plant, flower.defaultBlockState(), 3);
			BlockState state = level.getBlockState(plant);
			boolean growable = false;
			if(flower instanceof BonemealableBlock bonemeal) {
				growable = bonemeal.isValidBonemealTarget(level, plant, state) && bonemeal.isBonemealSuccess(level, level.random, plant, state);
				bonemeal.performBonemeal(level, level.random, plant, state);
			}
			check(problems, cases, growable, name + " always accepts bonemeal");
			check(problems, cases, countItems(level, flower.asItem()) == 1 && level.getBlockState(plant).is(flower), name + " bonemeal drops one flower and keeps the plant");
			check(problems, cases, drops(level, plant, flower.asItem()) == 1, name + " drops itself");
		}

		// canPlaceBlockOn: grass, dirt, farmland, dirt_dead, dirt_oily
		Block willow = HbmBlocks.PLANT_MUSTARD_WILLOW.get();
		for(Block ground : List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.FARMLAND, HbmBlocks.DIRT_DEAD.get(), HbmBlocks.DIRT_OILY.get())) {
			reset(level);
			level.setBlock(soil, ground.defaultBlockState(), 2);
			check(problems, cases, willow.defaultBlockState().canSurvive(level, plant), "plants stay on " + BuiltInRegistries.BLOCK.getKey(ground).getPath());
		}
		reset(level);
		level.setBlock(soil, Blocks.STONE.defaultBlockState(), 2);
		check(problems, cases, !willow.defaultBlockState().canSurvive(level, plant), "plants do not stay on stone");

		// CD0 -> CD1 -> CD2 -> CD3 -> CD4
		reset(level);
		level.setBlock(soil, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		level.setBlock(plant, willow.defaultBlockState(), 3);
		BonemealableBlock grow = (BonemealableBlock) willow;
		check(problems, cases, !grow.isValidBonemealTarget(level, plant, level.getBlockState(plant)), "CD0 does not grow without water");
		level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
		check(problems, cases, grow.isValidBonemealTarget(level, plant, level.getBlockState(plant)), "CD0 grows next to water");
		grow.performBonemeal(level, level.random, plant, level.getBlockState(plant));
		check(problems, cases, willowAge(level, plant) == MustardWillowPlantBlock.CD1 && level.getBlockState(plant.above()).isAir(), "CD0 grows into CD1");
		check(problems, cases, drops(level, plant, HbmItems.PLANT_MUSTARD_WILLOW.get()) == 1, "CD1 drops the CD0 item");
		level.setBlock(plant.above(), Blocks.STONE.defaultBlockState(), 3);
		check(problems, cases, !grow.isValidBonemealTarget(level, plant, level.getBlockState(plant)), "CD1 needs air above");
		level.setBlock(plant.above(), Blocks.AIR.defaultBlockState(), 3);
		grow.performBonemeal(level, level.random, plant, level.getBlockState(plant));
		check(problems, cases, willowAge(level, plant) == MustardWillowPlantBlock.CD2 && willowAge(level, plant.above()) == MustardWillowPlantBlock.CD2
				&& level.getBlockState(plant.above()).getValue(MustardWillowPlantBlock.HALF) == DoubleBlockHalf.UPPER, "CD1 grows into the two block CD2");
		// growing through the upper half (meta > 7)
		check(problems, cases, grow.isValidBonemealTarget(level, plant.above(), level.getBlockState(plant.above())), "CD2 grows through its upper half");
		grow.performBonemeal(level, level.random, plant.above(), level.getBlockState(plant.above()));
		check(problems, cases, willowAge(level, plant) == MustardWillowPlantBlock.CD3 && willowAge(level, plant.above()) == MustardWillowPlantBlock.CD3, "CD2 grows into CD3");
		check(problems, cases, !grow.isValidBonemealTarget(level, plant, level.getBlockState(plant)), "CD3 does not grow on grass");
		level.setBlock(soil, HbmBlocks.DIRT_OILY.get().defaultBlockState(), 3);
		check(problems, cases, willowAge(level, plant) == MustardWillowPlantBlock.CD3
				&& grow.isValidBonemealTarget(level, plant, level.getBlockState(plant))
				&& grow.isBonemealSuccess(level, level.random, plant, level.getBlockState(plant)), "CD3 always grows on oily dirt next to water");
		grow.performBonemeal(level, level.random, plant, level.getBlockState(plant));
		check(problems, cases, willowAge(level, plant) == MustardWillowPlantBlock.CD4 && willowAge(level, plant.above()) == MustardWillowPlantBlock.CD4
				&& level.getBlockState(soil).is(Blocks.DIRT), "CD3 grows into CD4 and turns the soil into dirt");
		check(problems, cases, !grow.isValidBonemealTarget(level, plant, level.getBlockState(plant)), "CD4 does not grow");

		// getDrops: every half one CD0 flower, the CD4 upper half 3 + rand(4) mustard willow leaves on top
		Item leaves = HbmItems.variantStack(HbmItems.PLANT_ITEMS, "mustardwillow").getItem();
		int leafCount = drops(level, plant.above(), leaves);
		check(problems, cases, drops(level, plant.above(), HbmItems.PLANT_MUSTARD_WILLOW.get()) == 1 && leafCount >= 3 && leafCount <= 6,
				"CD4 upper half drops one flower and 3-6 leaves (" + leafCount + ")");
		check(problems, cases, drops(level, plant, HbmItems.PLANT_MUSTARD_WILLOW.get()) == 1 && drops(level, plant, leaves) == 0, "CD4 lower half drops one flower only");

		// checkAndDropBlock: the upper half of a plant whose lower half is gone disappears without drops
		discardItems(level);
		level.setBlock(plant, Blocks.AIR.defaultBlockState(), 3);
		check(problems, cases, level.getBlockState(plant.above()).isAir() && countItems(level, HbmItems.PLANT_MUSTARD_WILLOW.get()) == 0
				&& countItems(level, leaves) == 0, "the upper half vanishes without drops when the lower half is gone");

		// detectCut: a lower half without its upper half reverts to the small flower
		level.setBlock(soil, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		BlockState tall = willow.defaultBlockState().setValue(MustardWillowPlantBlock.AGE, MustardWillowPlantBlock.CD3);
		level.setBlock(plant.above(), tall.setValue(MustardWillowPlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
		level.setBlock(plant, tall, 3);
		level.setBlock(plant.above(), Blocks.AIR.defaultBlockState(), 3);
		check(problems, cases, willowAge(level, plant) == MustardWillowPlantBlock.CD0, "a cut CD3 plant reverts to CD0");

		// WEED: flower -> tall; on dead / oily dirt -> plant_dead GENERIC, the tall plant -> BIGFLOWER
		Block hemp = HbmBlocks.PLANT_HEMP.get();
		BonemealableBlock hempGrow = (BonemealableBlock) hemp;
		reset(level);
		level.setBlock(soil, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		level.setBlock(plant, hemp.defaultBlockState(), 3);
		check(problems, cases, hempGrow.isValidBonemealTarget(level, plant, level.getBlockState(plant)), "hemp grows without water");
		hempGrow.performBonemeal(level, level.random, plant, level.getBlockState(plant));
		BlockState lower = level.getBlockState(plant);
		BlockState upper = level.getBlockState(plant.above());
		check(problems, cases, lower.is(hemp) && lower.getValue(HempPlantBlock.AGE) == HempPlantBlock.MATURE && upper.is(hemp)
				&& upper.getValue(HempPlantBlock.HALF) == DoubleBlockHalf.UPPER, "hemp grows into the two block plant");
		check(problems, cases, !hempGrow.isValidBonemealTarget(level, plant, lower), "tall hemp does not grow");
		check(problems, cases, drops(level, plant, HbmItems.PLANT_HEMP.get()) == 1 && drops(level, plant.above(), HbmItems.PLANT_HEMP.get()) == 1, "tall hemp drops one flower per half");
		level.setBlock(soil, HbmBlocks.DIRT_DEAD.get().defaultBlockState(), 3);
		level.getBlockState(plant).randomTick(level, plant, level.random);
		BlockState dead = level.getBlockState(plant);
		check(problems, cases, dead.is(HbmBlocks.PLANT_DEAD.get()) && dead.getValue(DeadPlantBlock.KIND) == DeadPlantBlock.Kind.BIG_FLOWER
				&& level.getBlockState(plant.above()).isAir(), "tall hemp on dead dirt dies into plant_dead BIGFLOWER");
		reset(level);
		level.setBlock(soil, HbmBlocks.DIRT_OILY.get().defaultBlockState(), 3);
		level.setBlock(plant, hemp.defaultBlockState(), 3);
		hempGrow.performBonemeal(level, level.random, plant, level.getBlockState(plant));
		dead = level.getBlockState(plant);
		check(problems, cases, dead.is(HbmBlocks.PLANT_DEAD.get()) && dead.getValue(DeadPlantBlock.KIND) == DeadPlantBlock.Kind.GENERIC,
				"hemp growing on oily dirt dies into plant_dead GENERIC");

		// creative variants: plant_flower CD1, plant_tall WEED / CD2 / CD3 / CD4
		String[][] stages = {
				{"plant_flower_cd1", "block.atomiclegacy.plant_flower.cd1"},
				{"plant_tall_weed", "block.atomiclegacy.plant_tall.weed"},
				{"plant_tall_cd2", "block.atomiclegacy.plant_tall.cd2"},
				{"plant_tall_cd3", "block.atomiclegacy.plant_tall.cd3"},
				{"plant_tall_cd4", "block.atomiclegacy.plant_tall.cd4"},
		};
		for(String[] stage : stages) {
			Item item = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(stage[0])).orElse(null);
			check(problems, cases, item != null && item.getDescriptionId().equals(stage[1]), stage[0] + " is registered as " + stage[1]);
		}
		check(problems, cases, willow.asItem() == HbmItems.PLANT_MUSTARD_WILLOW.get() && hemp.asItem() == HbmItems.PLANT_HEMP.get(),
				"the stage items do not replace the base flower items");
		reset(level);
	}

	// ---------------------------------------------------------------- 3. EntityCloudTom

	private static void cloudTom(ServerLevel level, List<String> problems, int[] cases) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(AtomicLegacy.id("entity_moonstone_blast")).orElse(null);
		check(problems, cases, type != null && type == HbmEntityTypes.LEGACY_CLOUD_TOM.get(), "entity_moonstone_blast is registered");
		// setSize(20, 40), isImmuneToFire = true
		check(problems, cases, type != null && type.getWidth() == 20.0F && type.getHeight() == 40.0F && type.fireImmune(), "EntityCloudTom is 20 x 40 and fire immune");
		// EntityTom: new EntityCloudTom(worldObj, 500)
		check(problems, cases, LegacyCloudTomEntity.TOM_MAX_AGE == 500, "the Tom impact cloud lasts 500 ticks");
		check(problems, cases, ParityD2MiscDiagnostics.class.getResource("/assets/atomiclegacy/textures/models/explosion/tomblast.png") != null,
				"textures/models/explosion/tomblast.png is present");

		LegacyCloudTomEntity cloud = LegacyCloudTomEntity.spawn(level, ORIGIN.getX() + 0.5D, ORIGIN.getY() + 6.0D, ORIGIN.getZ() + 0.5D, 3);
		check(problems, cases, cloud.getMaxAge() == 3 && cloud.age == 0, "the cloud starts at age 0");
		cloud.tick();
		cloud.tick();
		check(problems, cases, cloud.isAlive() && cloud.age == 2, "the cloud ages one per tick");
		CompoundTag saved = new CompoundTag();
		cloud.saveWithoutId(saved);
		check(problems, cases, saved.getShort("age") == 2, "the cloud saves its age as a short");
		cloud.tick();
		check(problems, cases, !cloud.isAlive(), "the cloud dies when age >= maxAge");
		cloud.discard();
	}

	// ---------------------------------------------------------------- 4. EntityFallingBlockNT

	/** 1.7.10 falling blocks: BlockFalling, BlockHazardFalling, BlockAshes, BlockNTMSand, BlockCrate and NTMAnvil (BlockFallingNT). */
	private static final String[] FALLING = {
			"ore_oil_sand", "block_scrap", "block_electrical_scrap", "block_yellowcake", "block_red_phosphorus", "block_fallout",
			"gravel_obsidian", "gravel_diamond", "concrete_super_broken", "moon_turf", "ash_digamma", "dirt_dead", "dirt_oily",
			"sand_dirty", "sand_dirty_red", "stone_cracked", "sand_mix", "crate", "crate_weapon", "crate_lead", "crate_metal", "crate_red",
			"anvil_iron", "anvil_lead", "anvil_steel", "anvil_desh", "anvil_ferrouranium", "anvil_saturnite", "anvil_bismuth_bronze",
			"anvil_arsenic_bronze", "anvil_schrabidate", "anvil_dnt", "anvil_osmiridium", "anvil_murky",
	};

	private static void falling(ServerLevel level, List<String> problems, int[] cases) {
		for(String id : FALLING) {
			Block block = BuiltInRegistries.BLOCK.getOptional(AtomicLegacy.id(id)).orElse(null);
			check(problems, cases, block instanceof FallingBlock, id + " is a falling block");
		}

		// NTMAnvil never overrides modifyFallingBlock: canHurtEntities stays false, canDrop true
		reset(level);
		BlockPos pos = ORIGIN.above(2);
		Block anvil = BuiltInRegistries.BLOCK.getOptional(AtomicLegacy.id("anvil_iron")).orElse(Blocks.AIR);
		level.setBlock(pos, anvil.defaultBlockState(), 2);
		level.getBlockState(pos).tick(level, pos, level.random);
		List<FallingBlockEntity> fallen = level.getEntitiesOfClass(FallingBlockEntity.class, AREA);
		check(problems, cases, fallen.size() == 1 && level.getBlockState(pos).isAir() && fallen.get(0).getBlockState().is(anvil), "an unsupported NTM anvil falls");
		if(fallen.size() == 1) {
			CompoundTag tag = new CompoundTag();
			fallen.get(0).saveWithoutId(tag);
			check(problems, cases, !tag.getBoolean("HurtEntities") && tag.getBoolean("DropItem"), "a falling NTM anvil does not hurt entities and may drop");
		}
		discardFalling(level);

		// EntityFalloutRain#stomp: hardness <= 6 (stone brick blast resistance 30 / 5), dist < 65, air below, depth + 1 blocks, canDrop = false
		check(problems, cases, NuclearFalloutTerrainEffects.COLLAPSE_MAX_HARDNESS == 6.0F, "fallout collapse hardness limit is 6");
		reset(level);
		level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
		level.setBlock(pos.above(), Blocks.DIRT.defaultBlockState(), 2);
		level.setBlock(pos.above(2), Blocks.OBSIDIAN.defaultBlockState(), 2);
		NuclearFalloutTerrainEffects.diagnosticCollapse(level, pos, 2, 65.0D);
		check(problems, cases, level.getEntitiesOfClass(FallingBlockEntity.class, AREA).isEmpty(), "nothing collapses at 65 % of the fallout radius");
		NuclearFalloutTerrainEffects.diagnosticCollapse(level, pos, 2, 64.0D);
		fallen = level.getEntitiesOfClass(FallingBlockEntity.class, AREA);
		boolean noDrops = true;
		for(FallingBlockEntity entity : fallen) noDrops &= !entity.dropItem;
		check(problems, cases, fallen.size() == 2 && noDrops && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos.above(2)).is(Blocks.OBSIDIAN), "stone and dirt collapse without drops, obsidian (hardness 50) stays (" + fallen.size() + ")");
		discardFalling(level);
		level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
		level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
		NuclearFalloutTerrainEffects.diagnosticCollapse(level, pos, 0, 10.0D);
		check(problems, cases, level.getEntitiesOfClass(FallingBlockEntity.class, AREA).isEmpty(), "a supported block does not collapse");
		discardFalling(level);
		reset(level);
	}

	// ---------------------------------------------------------------- 5. ItemSoup / ItemFood

	private static void food(List<String> problems, int[] cases) {
		// ItemSoup(6): ItemFood(6, 0.6F, false), stack size 1, onEaten returns a bowl
		for(String id : List.of("glowing_stew", "balefire_scrambled", "balefire_and_ham")) {
			ItemStack stack = stack(id);
			FoodProperties food = stack.get(DataComponents.FOOD);
			check(problems, cases, food != null && food.nutrition() == 6 && near(food.saturation(), 6 * 0.6F * 2.0F) && !food.canAlwaysEat()
					&& near(food.eatSeconds(), 1.6F) && food.effects().isEmpty(), id + " is ItemSoup(6): 6 hunger, 0.6 saturation modifier, 32 ticks");
			check(problems, cases, food != null && food.usingConvertsTo().isPresent() && food.usingConvertsTo().get().is(Items.BOWL)
					&& food.usingConvertsTo().get().getCount() == 1, id + " returns a bowl");
			check(problems, cases, stack.getMaxStackSize() == 1, id + " stacks to 1");
		}
		// ingot_smore = new ItemFood(10, 20F, false)
		ItemStack smore = stack("ingot_smore");
		FoodProperties food = smore.get(DataComponents.FOOD);
		check(problems, cases, food != null && food.nutrition() == 10 && near(food.saturation(), 10 * 20.0F * 2.0F) && !food.canAlwaysEat()
				&& near(food.eatSeconds(), 1.6F) && food.effects().isEmpty() && food.usingConvertsTo().isEmpty(), "ingot_smore is ItemFood(10, 20F, false)");
		check(problems, cases, smore.getMaxStackSize() == 64, "ingot_smore stacks to 64");
	}

	// ---------------------------------------------------------------- 6. commands

	private static void commands(MinecraftServer server, List<String> problems, int[] cases) {
		var root = server.getCommands().getDispatcher().getRoot();
		// CommandReloadClient is registered with ClientCommandHandler only; CommandPacketInfo debugs PacketThreading, which the port does not have
		check(problems, cases, root.getChild("ntmclient") == null, "/ntmclient is a client command, not a server command");
		check(problems, cases, root.getChild("ntmpackets") == null, "/ntmpackets is not registered (no PacketThreading in the port)");
	}

	// ---------------------------------------------------------------- helpers

	private static void recipe(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases, String id, int width, int height,
			List<ItemStack> grid, Item result, int count) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(holder -> holder.value()).orElse(null);
		CraftingInput input = CraftingInput.of(width, height, new ArrayList<>(grid));
		boolean ok = false;
		if(recipe instanceof CraftingRecipe crafting && crafting.matches(input, level)) {
			ItemStack out = crafting.assemble(input, server.registryAccess());
			ok = out.is(result) && out.getCount() == count;
		}
		check(problems, cases, ok, "recipe " + id + " gives " + count + " x " + BuiltInRegistries.ITEM.getKey(result));
	}

	private static int willowAge(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(HbmBlocks.PLANT_MUSTARD_WILLOW.get()) ? state.getValue(MustardWillowPlantBlock.AGE) : -1;
	}

	private static int drops(ServerLevel level, BlockPos pos, Item item) {
		int count = 0;
		for(ItemStack stack : Block.getDrops(level.getBlockState(pos), level, pos, null)) {
			if(stack.is(item)) count += stack.getCount();
		}
		return count;
	}

	private static int countItems(ServerLevel level, Item item) {
		int count = 0;
		for(ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, AREA)) {
			if(entity.isAlive() && entity.getItem().is(item)) count += entity.getItem().getCount();
		}
		return count;
	}

	private static void discardItems(ServerLevel level) {
		for(ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, AREA)) entity.discard();
	}

	private static void discardFalling(ServerLevel level) {
		for(FallingBlockEntity entity : level.getEntitiesOfClass(FallingBlockEntity.class, AREA)) entity.discard();
	}

	/** Clears the test column (soil, the neighbouring water and five blocks above) and the item entities around it. */
	private static void reset(ServerLevel level) {
		for(BlockPos pos : BlockPos.betweenClosed(ORIGIN.offset(-1, -1, -1), ORIGIN.offset(1, 5, 1))) {
			if(!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
		}
		discardItems(level);
	}

	private static void cleanup(ServerLevel level) {
		try {
			reset(level);
			discardFalling(level);
			for(Entity entity : level.getEntitiesOfClass(LegacyCloudTomEntity.class, AREA.inflate(32.0D))) entity.discard();
		} catch(Exception exception) {
			AtomicLegacy.LOGGER.error("parity d2 misc cleanup", exception);
		}
	}

	private static ItemStack stack(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(Items.BARRIER));
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 1.0E-4F;
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("parity d2 misc: " + message);
	}
}
