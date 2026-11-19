package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.CapacitorBlock;
import com.atomiclegacy.blocks.LegacyBatteryBlock;
import com.atomiclegacy.blocks.LegacyDeprecatedFurnaceBlock;
import com.atomiclegacy.blocks.LegacyDeprecatedFurnaceBlock.Kind;
import com.atomiclegacy.inventory.recipes.BlastFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe.ItemOutput;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.ChemicalPlantBlockEntity;
import com.atomiclegacy.tileentity.LegacyBatteryBlockEntity;
import com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity;
import com.atomiclegacy.util.LegacyFurnaceFuels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity d2: diagnostics] Bacteria with the weighted chemical plant outputs (GenericRecipes.ChanceOutput /
 * ChanceOutputMulti), swamp dirt and peat, and the deprecated but registered machines of the pinned 1.7.10 original:
 * blast furnace with extension, nuclear blast furnace, RTG furnace, legacy energy storage blocks and legacy capacitors.
 */
final class ParityD2Diagnostics {
	private ParityD2Diagnostics() {
	}

	private static final BlockPos BASE = new BlockPos(9_000, 200, 9_000);
	private static int site;

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		site = 0;
		int cx = BASE.getX() >> 4;
		int cz = BASE.getZ() >> 4;
		try {
			for(int dx = 0; dx <= 2; dx++) {
				level.setChunkForced(cx + dx, cz, true);
				level.getChunk(cx + dx, cz);
			}
			bacteria(server, level, problems, cases);
			outputs(server, problems, cases);
			chemicalPlant(level, problems, cases);
			registration(problems, cases);
			diFurnace(level, problems, cases);
			rtgFurnaces(level, problems, cases);
			battery(level, problems, cases);
		} catch(Exception exception) {
			problems.add("parity d2: diagnostics threw " + exception);
			AtomicLegacy.LOGGER.error("parity d2 diagnostics", exception);
		} finally {
			for(int i = 0; i < site; i++) {
				for(int dy = 0; dy <= 1; dy++) level.setBlock(sitePos(i).above(dy), Blocks.AIR.defaultBlockState(), 2 | 16);
			}
			for(var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(BASE).inflate(48, 8, 8))) entity.discard();
			for(int dx = 0; dx <= 2; dx++) level.setChunkForced(cx + dx, cz, false);
		}
		return cases[0];
	}

	private static BlockPos sitePos(int index) {
		return BASE.offset(index * 3, 0, 4);
	}

	private static BlockEntity place(ServerLevel level, BlockState state) {
		BlockPos pos = sitePos(site++);
		level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
		level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2 | 16);
		level.setBlock(pos, state, 3);
		return level.getBlockEntity(pos);
	}

	/// ModItems.bacteria / briquette PEAT / ModBlocks.dirt, FuelHandler, ChemicalPlantRecipes, RockMillRecipes ///
	private static void bacteria(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		check(problems, cases, List.copyOf(HbmItems.BACTERIA_ITEMS.keySet()).equals(List.of("agar", "anthrax_inert", "anthrax_dead", "anthrax_mutated")),
				"EnumBacteriaType order " + HbmItems.BACTERIA_ITEMS.keySet());
		for(String id : List.of("bacteria_agar", "bacteria_anthrax_inert", "bacteria_anthrax_dead", "bacteria_anthrax_mutated", "briquette_peat")) {
			Item item = item(id);
			check(problems, cases, item != Items.AIR && new ItemStack(item).getMaxStackSize() == 64, id + " registered, stack 64");
		}
		check(problems, cases, LegacyFurnaceFuels.burnTime(new ItemStack(item("briquette_peat"))) == 200 * 6,
				"peat briquette burn time " + LegacyFurnaceFuels.burnTime(new ItemStack(item("briquette_peat"))) + " expected single * 6");
		Block dirt = HbmBlocks.DIRT_SWAMP.get();
		check(problems, cases, dirt.defaultDestroyTime() == 5.0F && dirt.getExplosionResistance() == 5.0F,
				"dirt_swamp hardness " + dirt.defaultDestroyTime() + " resistance " + dirt.getExplosionResistance());

		String[][] chem = {
				{"chem.agar", "100", "250", "salient:1000"}, {"chem.growinertanthrax", "600", "500", "water:4000"},
				{"chem.growmutatedanthrax", "600", "500", "water:4000"}, {"chem.mutateanthrax", "600", "2000", "water:4000"}};
		for(String[] row : chem) {
			ChemicalPlantRecipe recipe = chemRecipe(server, row[0]);
			check(problems, cases, recipe != null, row[0] + " loaded");
			if(recipe == null) continue;
			check(problems, cases, recipe.duration() == Integer.parseInt(row[1]) && recipe.power() == Integer.parseInt(row[2])
					&& recipe.inputFluids().size() == 1
					&& (recipe.inputFluids().getFirst().type().name() + ":" + recipe.inputFluids().getFirst().amount()).equals(row[3])
					&& recipe.outputFluids().isEmpty(), row[0] + " duration/power/fluid");
		}
		ChemicalPlantRecipe agar = chemRecipe(server, "chem.agar");
		if(agar != null) check(problems, cases, agar.inputItems().size() == 2 && agar.inputItems().get(0).ingredient().test(new ItemStack(Items.SUGAR))
				&& agar.inputItems().get(1).ingredient().test(new ItemStack(item("powder_sawdust")))
				&& agar.outputs().size() == 1 && agar.outputs().getFirst().getSingle().is(item("bacteria_agar")), "chem.agar items");
		for(String kind : List.of("inert", "mutated")) {
			ChemicalPlantRecipe grow = chemRecipe(server, "chem.grow" + kind + "anthrax");
			if(grow == null) continue;
			ItemStack out = grow.outputs().getFirst().getSingle();
			check(problems, cases, grow.inputItems().size() == 2 && grow.inputItems().get(0).ingredient().test(new ItemStack(item("bacteria_anthrax_" + kind)))
					&& grow.inputItems().get(1).ingredient().test(new ItemStack(item("bacteria_agar")))
					&& out.is(item("bacteria_anthrax_" + kind)) && out.getCount() == 2, "chem.grow" + kind + "anthrax items");
		}
		// chem.mutateanthrax: ChanceOutputMulti(ChanceOutput(MUTATED, 1)), ChanceOutput(DEAD, 79), ChanceOutput(INERT, 20), ChanceOutput(waste)
		ChemicalPlantRecipe mutate = chemRecipe(server, "chem.mutateanthrax");
		if(mutate != null) {
			List<ItemOutput> out = mutate.outputs();
			// [user decision 2026-11-19] intended pool of the original (its misplaced bracket fixed): one of mutated/dead/inert by 1/79/20, plus waste
			boolean ok = mutate.inputItems().size() == 3 && mutate.inputItems().get(2).ingredient().test(new ItemStack(item("nugget_co60"))) && out.size() == 2;
			if(ok) {
				ok = out.get(0).multi() && out.get(0).pool().size() == 3 && out.get(0).possibleMultiOutput()
						&& out.get(0).pool().get(0).weight() == 1 && out.get(0).pool().get(1).weight() == 79 && out.get(0).pool().get(2).weight() == 20
						&& !out.get(1).multi() && out.get(1).getSingle().is(item("nuclear_waste_tiny"));
			}
			check(problems, cases, ok, "chem.mutateanthrax outputs " + out);
		}
		// rock.swamp: duraShort 100, consumption 25, peat x4 (150) / dirt (49) / inert anthrax (1), no fluids
		RockMillRecipe swamp = null;
		for(RecipeHolder<RockMillRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ROCK_MILL.get())) {
			if(holder.id().getPath().equals("rock_mill/swamp")) swamp = holder.value();
		}
		check(problems, cases, swamp != null, "rock.swamp loaded");
		if(swamp != null) {
			var o = swamp.outputs();
			check(problems, cases, swamp.duration() == 100 && swamp.power() == 25L && swamp.inputFluid().isEmpty() && swamp.outputFluid().isEmpty()
					&& swamp.input().ingredient().test(new ItemStack(HbmBlocks.DIRT_SWAMP.get())) && swamp.input().count() == 1 && o.size() == 3
					&& o.get(0).stack().is(item("briquette_peat")) && o.get(0).stack().getCount() == 4 && o.get(0).weight() == 150
					&& o.get(1).stack().is(Items.DIRT) && o.get(1).stack().getCount() == 1 && o.get(1).weight() == 49
					&& o.get(2).stack().is(item("bacteria_anthrax_inert")) && o.get(2).stack().getCount() == 1 && o.get(2).weight() == 1,
					"rock.swamp values " + o);
		}
	}

	/// GenericRecipes.ChanceOutput / ChanceOutputMulti ///
	private static void outputs(MinecraftServer server, List<String> problems, int[] cases) {
		RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
		String[] json = {
				"{\"id\":\"minecraft:dirt\",\"count\":3}",
				"{\"single\":{\"stack\":{\"id\":\"minecraft:dirt\",\"count\":8},\"chance\":0.25}}",
				"{\"multi\":[{\"stack\":{\"id\":\"minecraft:dirt\"},\"weight\":1},{\"stack\":{\"id\":\"minecraft:sand\"},\"weight\":79},{\"stack\":{\"id\":\"minecraft:gravel\"},\"weight\":20}]}"};
		List<ItemOutput> parsed = new ArrayList<>();
		for(String text : json) {
			Optional<ItemOutput> result = ItemOutput.CODEC.parse(ops, JsonParser.parseString(text)).result();
			check(problems, cases, result.isPresent(), "ItemOutput codec parses " + text);
			if(result.isEmpty()) return;
			Optional<ItemOutput> again = ItemOutput.CODEC.encodeStart(ops, result.get()).result().flatMap(e -> ItemOutput.CODEC.parse(ops, e).result());
			check(problems, cases, again.isPresent() && again.get().multi() == result.get().multi() && again.get().pool().size() == result.get().pool().size()
					&& again.get().pool().getFirst().chance() == result.get().pool().getFirst().chance()
					&& again.get().pool().getFirst().weight() == result.get().pool().getFirst().weight(), "ItemOutput codec round trip " + text);
			parsed.add(result.get());
		}
		ItemOutput plain = parsed.get(0);
		ItemOutput chance = parsed.get(1);
		ItemOutput multi = parsed.get(2);
		RandomSource random = RandomSource.create(936L);
		check(problems, cases, !plain.multi() && !plain.possibleMultiOutput() && plain.getSingle().getCount() == 3 && plain.collapse(random).getCount() == 3
				&& plain.labels().size() == 1 && plain.labels().getFirst().startsWith("3x "), "plain output");
		// ChanceOutput#collapse rolls every item of the stack on its own
		int total = 0;
		int empty = 0;
		for(int i = 0; i < 4_000; i++) {
			ItemStack stack = chance.collapse(random);
			if(stack.isEmpty()) empty++;
			total += stack.getCount();
		}
		check(problems, cases, !chance.possibleMultiOutput() && chance.getSingle().getCount() == 8 && total > 7_400 && total < 8_600 && empty > 250 && empty < 560
				&& chance.labels().getFirst().endsWith(" (25.0%)"), "chance output: total " + total + " of 32000 rolls, empty " + empty + ", label " + chance.labels());
		int[] hits = new int[3];
		for(int i = 0; i < 20_000; i++) {
			ItemStack stack = multi.collapse(random);
			hits[stack.is(Items.DIRT) ? 0 : stack.is(Items.SAND) ? 1 : 2]++;
		}
		check(problems, cases, multi.possibleMultiOutput() && multi.getSingle().isEmpty() && multi.totalWeight() == 100
				&& hits[0] > 120 && hits[0] < 290 && hits[1] > 15_400 && hits[1] < 16_200 && hits[2] > 3_650 && hits[2] < 4_350,
				"weighted output distribution " + hits[0] + "/" + hits[1] + "/" + hits[2]);
		check(problems, cases, multi.labels().size() == 3 && multi.labels().get(0).endsWith(" (1.0%)") && multi.labels().get(1).endsWith(" (79.0%)")
				&& multi.labels().get(2).endsWith(" (20.0%)"), "weighted labels " + multi.labels());
	}

	/// ModuleMachineBase#canFitOutput / produceItem with chem.mutateanthrax ///
	private static void chemicalPlant(ServerLevel level, List<String> problems, int[] cases) {
		BlockEntity be = place(level, HbmBlocks.MACHINE_CHEMICAL_PLANT.get().defaultBlockState());
		if(!(be instanceof ChemicalPlantBlockEntity machine)) {
			check(problems, cases, false, "chemical plant block entity");
			return;
		}
		machine.setSelectedRecipe(AtomicLegacy.id("chemical_plant/mutateanthrax"));
		tickChem(machine, 1);
		check(problems, cases, machine.inputTank(0).type().name().equals("water"), "chemplant: tank set up for chem.mutateanthrax");
		machine.inputTank(0).fill(machine.inputTank(0).type(), 8_000, false);
		int in = ChemicalPlantBlockEntity.SOLID_INPUT_START;
		int out = ChemicalPlantBlockEntity.SOLID_OUTPUT_START;
		machine.setItem(in, new ItemStack(item("bacteria_anthrax_inert"), 2));
		machine.setItem(in + 1, new ItemStack(item("bacteria_agar"), 2));
		machine.setItem(in + 2, new ItemStack(item("nugget_co60"), 2));
		machine.setEnergyStored(10_000_000L);
		tickChem(machine, 590);
		check(problems, cases, machine.getItem(out).isEmpty(), "chemplant: anthrax mutated before 600 ticks");
		tickChem(machine, 12);
		// [user decision 2026-11-19] one anthrax variant from the weighted pool, then the tiny nuclear waste
		ItemStack first = machine.getItem(out);
		check(problems, cases, first.getCount() == 1 && (first.is(item("bacteria_anthrax_mutated")) || first.is(item("bacteria_anthrax_dead"))
				|| first.is(item("bacteria_anthrax_inert"))) && machine.getItem(out + 1).is(item("nuclear_waste_tiny")) && machine.getItem(out + 2).isEmpty(),
				"chemplant: chem.mutateanthrax outputs " + machine.getItem(out) + " " + machine.getItem(out + 1) + " " + machine.getItem(out + 2));
		check(problems, cases, machine.getItem(in).getCount() == 1 && machine.getItem(in + 2).getCount() == 1 && machine.inputTank(0).amount() == 4_000,
				"chemplant: chem.mutateanthrax consumption");
		// an occupied slot of another item blocks the second run
		machine.setItem(out + 1, new ItemStack(Items.DIRT));
		tickChem(machine, 700);
		check(problems, cases, machine.getItem(out).getCount() == 1 && machine.getItem(in).getCount() == 1, "chemplant: foreign item in output slot must block the recipe");
		machine.setItem(in, ItemStack.EMPTY);
		machine.setItem(in + 1, ItemStack.EMPTY);
		machine.setItem(in + 2, ItemStack.EMPTY);
		for(int i = 0; i < 3; i++) machine.setItem(out + i, ItemStack.EMPTY);
	}

	private static void tickChem(ChemicalPlantBlockEntity machine, int ticks) {
		for(int i = 0; i < ticks; i++) {
			ChemicalPlantBlockEntity.tick(machine.getLevel(), machine.getBlockPos(), machine.getLevel().getBlockState(machine.getBlockPos()), machine);
		}
	}

	/// ModBlocks registrations of the deprecated machines ///
	private static void registration(List<String> problems, int[] cases) {
		java.util.Properties tabs = new java.util.Properties();
		try(var stream = com.atomiclegacy.registry.HbmCreativeTabs.class.getResourceAsStream("creative-tabs.properties")) {
			if(stream != null) tabs.load(stream);
		} catch(java.io.IOException exception) {
			problems.add("parity d2: creative-tabs.properties unreadable " + exception);
		}
		// id, hardness is always 5 / resistance 10 (-> 6), tab of the original
		String[][] blocks = {
				{"machine_difurnace_off", "machine"}, {"machine_difurnace_on", "hidden"}, {"machine_difurnace_extension", "machine"},
				{"machine_difurnace_rtg_off", "machine"}, {"machine_difurnace_rtg_on", "hidden"},
				{"machine_rtg_furnace_off", "hidden"}, {"machine_rtg_furnace_on", "hidden"},
				{"machine_battery_potato", "hidden"}, {"machine_battery", "hidden"}, {"machine_lithium_battery", "hidden"},
				{"machine_schrabidium_battery", "hidden"}, {"machine_dineutronium_battery", "hidden"},
				{"capacitor_gold", "hidden"}, {"capacitor_niobium", "hidden"}, {"capacitor_tantalium", "hidden"}, {"capacitor_schrabidate", "hidden"}};
		for(String[] row : blocks) {
			Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(row[0]));
			check(problems, cases, block != Blocks.AIR && block.defaultDestroyTime() == 5.0F && block.getExplosionResistance() == 6.0F
					&& block.asItem() != Items.AIR && row[1].equals(tabs.getProperty(row[0])),
					row[0] + " hardness " + block.defaultDestroyTime() + " resistance " + block.getExplosionResistance() + " tab " + tabs.getProperty(row[0]));
		}
		long[] max = {10_000L, 1_000_000L, 50_000_000L, 25_000_000_000L, 1_000_000_000_000L};
		String[] ids = {"machine_battery_potato", "machine_battery", "machine_lithium_battery", "machine_schrabidium_battery", "machine_dineutronium_battery"};
		for(int i = 0; i < ids.length; i++) {
			Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(ids[i]));
			check(problems, cases, block instanceof LegacyBatteryBlock battery && battery.maxPower() == max[i], ids[i] + " maxPower");
		}
		for(String id : List.of("capacitor_gold", "capacitor_niobium", "capacitor_tantalium", "capacitor_schrabidate")) {
			Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
			check(problems, cases, block instanceof CapacitorBlock && !(block instanceof EntityBlock), id + " is a passive MachineCapacitor");
		}
		check(problems, cases, HbmBlocks.MACHINE_DIFURNACE_ON.get().defaultBlockState().setValue(LegacyDeprecatedFurnaceBlock.LIT, true).getLightEmission() == 15
				&& HbmBlocks.MACHINE_DIFURNACE_OFF.get().defaultBlockState().getLightEmission() == 0, "difurnace light level");
		int[] power = {200, 200, 2_000, 12_800, 1_000, 300, 150, 150, 200, 200, 200, 400, 400, 4_000, 0, 0};
		ItemStack[] fuels = {new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL), new ItemStack(Items.COAL_BLOCK), new ItemStack(Items.LAVA_BUCKET),
				new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.BLAZE_POWDER), new ItemStack(item("lignite")), new ItemStack(item("powder_lignite")),
				new ItemStack(item("powder_coal")), new ItemStack(item("briquette_coal")), new ItemStack(item("briquette_peat")), new ItemStack(item("coke_petroleum")),
				new ItemStack(item("solid_fuel")), new ItemStack(HbmBlocks.BLOCK_COKE.get()), new ItemStack(Items.STICK), new ItemStack(item("solid_fuel_presto"))};
		for(int i = 0; i < fuels.length; i++) {
			int value = LegacyDeprecatedFurnaceBlockEntity.diFurnaceItemPower(fuels[i]);
			check(problems, cases, !fuels[i].isEmpty() && value == power[i], "difurnace fuel " + fuels[i] + " = " + value + " expected " + power[i]);
		}
	}

	/// TileEntityDiFurnace ///
	private static void diFurnace(ServerLevel level, List<String> problems, int[] cases) {
		BlastFurnaceRecipe recipe = null;
		for(RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
			BlastFurnaceRecipe r = holder.value();
			if(r.inputA().getItems().length > 0 && r.inputB().getItems().length > 0 && !r.result().isEmpty()) {
				recipe = r;
				break;
			}
		}
		check(problems, cases, recipe != null, "a blast furnace recipe exists");
		if(recipe == null) return;
		ItemStack a = recipe.inputA().getItems()[0].copyWithCount(3);
		ItemStack b = recipe.inputB().getItems()[0].copyWithCount(3);
		int result = recipe.result().getCount();

		for(boolean extension : new boolean[] {false, true}) {
			BlockEntity be = place(level, HbmBlocks.MACHINE_DIFURNACE_OFF.get().defaultBlockState());
			if(!(be instanceof LegacyDeprecatedFurnaceBlockEntity furnace) || furnace.kind() != Kind.DIFURNACE || furnace.getContainerSize() != 4) {
				check(problems, cases, false, "difurnace block entity");
				return;
			}
			BlockPos pos = furnace.getBlockPos();
			if(extension) {
				level.setBlock(pos.above(), HbmBlocks.MACHINE_DIFURNACE_EXTENSION.get().defaultBlockState(), 3);
				check(problems, cases, level.getBlockState(pos).getValue(LegacyDeprecatedFurnaceBlock.COVERED), "difurnace: covered state below the extension");
				var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos.above(), Direction.UP);
				check(problems, cases, handler != null && handler.getSlots() == 4, "difurnace extension proxies the inventory");
			}
			// order swapped on purpose: BlastFurnaceRecipes#getOutput accepts both orders
			furnace.setItem(0, b.copy());
			furnace.setItem(1, a.copy());
			tick(furnace, 3);
			check(problems, cases, furnace.progress() == 0 && furnace.fuel() == 0 && !level.getBlockState(pos).getValue(LegacyDeprecatedFurnaceBlock.LIT),
					"difurnace: no progress without fuel");
			furnace.setItem(2, new ItemStack(Items.COAL, 3));
			tick(furnace, 1);
			// one coal per tick while fuel <= maxFuel - 200; the first working tick already burned 1
			check(problems, cases, furnace.fuel() == 199 && furnace.getItem(2).getCount() == 2 && furnace.progress() == (extension ? 3 : 1)
					&& level.getBlockState(pos).getValue(LegacyDeprecatedFurnaceBlock.LIT), "difurnace: first tick fuel " + furnace.fuel() + " progress " + furnace.progress());
			int ticks = extension ? 134 : 400;
			tick(furnace, ticks - 2);
			check(problems, cases, furnace.getItem(3).isEmpty(), "difurnace: output before " + ticks + " ticks");
			tick(furnace, 1);
			check(problems, cases, furnace.getItem(3).getCount() == result && ItemStack.isSameItem(furnace.getItem(3), recipe.result())
					&& furnace.getItem(0).getCount() == 2 && furnace.getItem(1).getCount() == 2
					&& furnace.progress() == (extension ? 2 : 0) && furnace.fuel() == 600 - ticks,
					"difurnace: result after " + ticks + " ticks: " + furnace.getItem(3) + " progress " + furnace.progress() + " fuel " + furnace.fuel());
			// lava bucket leaves its container; fuel above maxFuel - power is not accepted
			furnace.setItem(2, new ItemStack(Items.LAVA_BUCKET));
			furnace.setFuel(1);
			tick(furnace, 1);
			check(problems, cases, furnace.getItem(2).is(Items.LAVA_BUCKET), "difurnace: lava bucket must wait for fuel 0");
			furnace.setFuel(0);
			tick(furnace, 1);
			check(problems, cases, furnace.getItem(2).is(Items.BUCKET) && furnace.fuel() == 12_799, "difurnace: lava bucket -> bucket, fuel " + furnace.fuel());
			// sided access: every slot starts on side 1 (up), right click cycles it; only slot 3 can be extracted
			check(problems, cases, furnace.canPlaceItemThroughFace(0, a, Direction.UP) && !furnace.canPlaceItemThroughFace(0, a, Direction.NORTH)
					&& !furnace.canPlaceItemThroughFace(3, a, Direction.UP) && furnace.canTakeItemThroughFace(3, a, Direction.DOWN)
					&& !furnace.canTakeItemThroughFace(0, a, Direction.DOWN), "difurnace: sided access defaults");
			furnace.cycleSide(0);
			check(problems, cases, furnace.side(0) == 2 && furnace.canPlaceItemThroughFace(0, a, Direction.NORTH) && !furnace.canPlaceItemThroughFace(0, a, Direction.UP),
					"difurnace: cycled side " + furnace.side(0));
			furnace.clearContent();
		}
	}

	/// TileEntityDiFurnaceRTG / TileEntityRtgFurnace ///
	private static void rtgFurnaces(ServerLevel level, List<String> problems, int[] cases) {
		RtgPelletItem pellet = (RtgPelletItem) HbmItems.PELLET_RTG_POLONIUM.get();
		int heat = pellet.heat(new ItemStack(pellet));
		{
			BlockEntity be = place(level, HbmBlocks.MACHINE_RTG_FURNACE_OFF.get().defaultBlockState());
			if(!(be instanceof LegacyDeprecatedFurnaceBlockEntity furnace) || furnace.kind() != Kind.RTG_FURNACE || furnace.getContainerSize() != 5) {
				check(problems, cases, false, "rtg furnace block entity");
				return;
			}
			furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 2));
			tick(furnace, 5);
			check(problems, cases, furnace.progress() == 0 && !furnace.rtgFurnaceHasPower(), "rtg furnace: idle without pellets");
			furnace.setItem(1, new ItemStack(pellet));
			furnace.setItem(3, new ItemStack(pellet));
			tick(furnace, 1);
			check(problems, cases, furnace.rtgFurnaceHasPower() && furnace.progress() == 2 * heat
					&& level.getBlockState(furnace.getBlockPos()).getValue(LegacyDeprecatedFurnaceBlock.LIT), "rtg furnace: progress per tick " + furnace.progress() + " heat " + heat);
			int ticks = (1_000 + 2 * heat - 1) / (2 * heat);
			tick(furnace, ticks - 2);
			check(problems, cases, furnace.getItem(4).isEmpty(), "rtg furnace: output before " + ticks + " ticks");
			tick(furnace, 1);
			check(problems, cases, furnace.getItem(4).is(Items.STONE) && furnace.getItem(4).getCount() == 1 && furnace.getItem(0).getCount() == 1
					&& furnace.progress() == 0, "rtg furnace: smelted after " + ticks + " ticks: " + furnace.getItem(4));
			check(problems, cases, furnace.getSlotsForFace(Direction.UP).length == 1 && furnace.getSlotsForFace(Direction.UP)[0] == 0
					&& furnace.getSlotsForFace(Direction.DOWN)[0] == 4 && furnace.getSlotsForFace(Direction.EAST).length == 3
					&& !furnace.canTakeItemThroughFace(1, new ItemStack(pellet), Direction.DOWN) && furnace.canTakeItemThroughFace(4, new ItemStack(Items.STONE), Direction.DOWN),
					"rtg furnace: sided access");
			furnace.clearContent();
		}
		{
			BlastFurnaceRecipe recipe = null;
			for(RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
				BlastFurnaceRecipe r = holder.value();
				if(r.inputA().getItems().length > 0 && r.inputB().getItems().length > 0 && !r.result().isEmpty()) {
					recipe = r;
					break;
				}
			}
			if(recipe == null) return;
			BlockEntity be = place(level, HbmBlocks.MACHINE_DIFURNACE_RTG_OFF.get().defaultBlockState());
			if(!(be instanceof LegacyDeprecatedFurnaceBlockEntity furnace) || furnace.kind() != Kind.DIFURNACE_RTG || furnace.getContainerSize() != 9) {
				check(problems, cases, false, "rtg difurnace block entity");
				return;
			}
			furnace.setItem(0, recipe.inputA().getItems()[0].copyWithCount(2));
			furnace.setItem(1, recipe.inputB().getItems()[0].copyWithCount(2));
			tick(furnace, 3);
			check(problems, cases, furnace.progress() == 0, "rtg difurnace: idle without heat");
			for(int slot = 3; slot < 9; slot++) furnace.setItem(slot, new ItemStack(pellet));
			tick(furnace, 1);
			check(problems, cases, furnace.processSpeed() == 6 * heat && furnace.progress() == 6 * heat
					&& level.getBlockState(furnace.getBlockPos()).getValue(LegacyDeprecatedFurnaceBlock.LIT),
					"rtg difurnace: speed " + furnace.processSpeed() + " progress " + furnace.progress() + " heat " + heat);
			int ticks = (1_200 + 6 * heat - 1) / (6 * heat);
			tick(furnace, ticks - 2);
			check(problems, cases, furnace.getItem(2).isEmpty(), "rtg difurnace: output before " + ticks + " ticks");
			tick(furnace, 1);
			check(problems, cases, ItemStack.isSameItem(furnace.getItem(2), recipe.result()) && furnace.getItem(2).getCount() == recipe.result().getCount()
					&& furnace.getItem(0).getCount() == 1 && furnace.progress() == 0, "rtg difurnace: result after " + ticks + " ticks: " + furnace.getItem(2));
			ItemStack p = new ItemStack(pellet);
			check(problems, cases, !furnace.canPlaceItem(0, p) && furnace.canPlaceItem(3, p) && !furnace.canPlaceItem(2, new ItemStack(Items.DIRT))
					&& furnace.canPlaceItem(4, new ItemStack(Items.DIRT)) && !furnace.canTakeItemThroughFace(3, p, Direction.DOWN)
					&& furnace.canTakeItemThroughFace(3, new ItemStack(Items.DIRT), Direction.DOWN) && furnace.canTakeItemThroughFace(2, p, Direction.DOWN)
					&& !furnace.canTakeItemThroughFace(0, p, Direction.DOWN), "rtg difurnace: slot rules");
			furnace.clearContent();
		}
	}

	/// TileEntityMachineBattery ///
	private static void battery(ServerLevel level, List<String> problems, int[] cases) {
		BlockEntity be = place(level, HbmBlocks.MACHINE_BATTERY.get().defaultBlockState());
		if(!(be instanceof LegacyBatteryBlockEntity battery)) {
			check(problems, cases, false, "legacy battery block entity");
			return;
		}
		BlockPos pos = battery.getBlockPos();
		BlockState state = level.getBlockState(pos);
		check(problems, cases, battery.getMaxPower() == 1_000_000L && battery.redLow() == 0 && battery.redHigh() == 2
				&& battery.priority() == PowerEndpoint.ConnectionPriority.LOW && battery.getContainerSize() == 2, "legacy battery defaults");
		// [parity h1: agreed deviation] TilePortShapes.solderer bug fixed: the battery joins cable networks (no FE capability)
		check(problems, cases, be instanceof PowerEndpoint
				&& level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, Direction.UP) == null,
				"legacy battery must be a power endpoint");
		check(problems, cases, battery.getComparatorPower() == 0, "legacy battery comparator empty");
		battery.setPower(500_000L);
		check(problems, cases, battery.getComparatorPower() == 8 && battery.getPowerRemainingScaled(52) == 26, "legacy battery comparator/scale at half");
		battery.setPower(1_000_000L);
		check(problems, cases, battery.getComparatorPower() == 15, "legacy battery comparator full");
		// slot 1 is charged by the block, slot 0 discharges into it
		ItemStack cell = new ItemStack(HbmItems.CAPACITOR_COPPER.get());
		if(BatteryPackItem.isBattery(cell)) {
			long capacity = BatteryPackItem.capacity(cell);
			long rate = Math.min(BatteryPackItem.chargeRate(cell), capacity);
			battery.setItem(1, cell);
			LegacyBatteryBlockEntity.tick(level, pos, state, battery);
			check(problems, cases, BatteryPackItem.charge(battery.getItem(1)) == rate && battery.getPower() == 1_000_000L - rate,
					"legacy battery charges the item in slot 1: " + BatteryPackItem.charge(battery.getItem(1)) + " power " + battery.getPower());
			ItemStack charged = battery.getItem(1).copy();
			battery.setItem(1, ItemStack.EMPTY);
			battery.setPower(0L);
			battery.setItem(0, charged);
			LegacyBatteryBlockEntity.tick(level, pos, state, battery);
			long expected = Math.min(rate, BatteryPackItem.dischargeRate(charged));
			check(problems, cases, battery.getPower() == expected, "legacy battery drains the item in slot 0: " + battery.getPower() + " expected " + expected);
			check(problems, cases, !battery.canTakeItemThroughFace(1, battery.getItem(0), Direction.DOWN), "legacy battery: partly charged item stays in slot 1");
			battery.setItem(0, ItemStack.EMPTY);
		} else {
			check(problems, cases, false, "copper capacitor is a battery item");
		}
		battery.cycleLowMode();
		battery.cycleHighMode();
		battery.cyclePriority();
		check(problems, cases, battery.redLow() == 1 && battery.redHigh() == 3 && battery.priority() == PowerEndpoint.ConnectionPriority.NORMAL, "legacy battery buttons");
		battery.setPower(123_456L);
		// IPersistentNBT: the drop keeps power, modes and priority
		ItemStack drop = new ItemStack(HbmBlocks.MACHINE_BATTERY.get());
		battery.saveToItem(drop);
		BlockEntity second = place(level, HbmBlocks.MACHINE_BATTERY.get().defaultBlockState());
		if(second instanceof LegacyBatteryBlockEntity restored) {
			restored.loadFromItem(drop);
			check(problems, cases, restored.getPower() == 123_456L && restored.redLow() == 1 && restored.redHigh() == 3
					&& restored.priority() == PowerEndpoint.ConnectionPriority.NORMAL, "legacy battery persistent data");
			restored.setPower(0L);
		}
		battery.setPower(0L);
	}

	private static void tick(LegacyDeprecatedFurnaceBlockEntity furnace, int ticks) {
		for(int i = 0; i < ticks; i++) {
			LegacyDeprecatedFurnaceBlockEntity.tick(furnace.getLevel(), furnace.getBlockPos(), furnace.getLevel().getBlockState(furnace.getBlockPos()), furnace);
		}
	}

	private static ChemicalPlantRecipe chemRecipe(MinecraftServer server, String name) {
		for(RecipeHolder<ChemicalPlantRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())) {
			if(holder.value().group().equals(name)) return holder.value();
		}
		return null;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).orElse(Items.AIR);
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("parity d2: " + message);
	}
}
