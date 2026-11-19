package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.inventory.recipes.AmmoPressRecipe;
import com.atomiclegacy.inventory.recipes.ArcFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.SolderingStationRecipe;
import com.atomiclegacy.items.ArcElectrodeItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.AmmoPressBlockEntity;
import com.atomiclegacy.tileentity.ArcFurnaceBlockEntity;
import com.atomiclegacy.tileentity.SolderingStationBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Dedicated-server checks for parity batch K3, group C: machine_soldering_station
 * (TileEntityMachineSolderingStation / SolderingRecipes), machine_arc_furnace (TileEntityMachineArcFurnaceLarge /
 * ArcFurnaceRecipes) and machine_ammo_press (TileEntityMachineAmmoPress / AmmoPressRecipes) of the pinned 1.7.10
 * reference. Each machine is placed on its own pad far from the origin and ticked directly; expected timings are
 * computed with the original arithmetic (int division, float accumulation). Recipe tables were generated from the
 * original sources by work/tmp-k3/C/gen_soldering_table.py and ammo_press_compare.py.
 * Not covered: client particles/sounds/GUI, the dynamic arc furnace material autogen and bedrock ore recipes.
 */
final class ParityK3CDiagnostics {

	private ParityK3CDiagnostics() {
	}

	private static final int BASE_X = 8_000;
	private static final int BASE_Y = 200;
	private static final int BASE_Z = 8_000;
	private static final int SPACING = 48;
	private static int site;
	private static final List<int[]> FORCED = new ArrayList<>();

	/** SolderingRecipes.registerDefaults with GeneralConfig defaults (no LBSM, no 528); see gen_soldering_table.py */
	private static final String[] SOLDERING = {
		"circuit_analog|100|100|-|circuit_vacuum_tube*3/circuit_capacitor*2|circuit_pcb*4|#c:wires/lead*4",
		"circuit_basic|200|250|-|circuit_chip*4|circuit_pcb*4|#c:wires/lead*4",
		"circuit_advanced|300|1000|sulfuric_acid:1000|circuit_chip*16/circuit_capacitor*4|circuit_pcb*8/#c:ingots/rubber*2|#c:wires/lead*8",
		"circuit_capacitor_board|200|300|peroxide:250|circuit_capacitor_tantalium*3|circuit_pcb*1|#c:wires/lead*3",
		"circuit_bismoid|400|10000|solvent:1000|circuit_chip_bismoid*4/circuit_chip*16/circuit_capacitor*24|circuit_pcb*12/#c:ingots/hard_plastic*2|#c:wires/lead*12",
		"circuit_quantum|400|100000|helium4:1000|circuit_chip_quantum*4/circuit_chip_bismoid*16/circuit_atomic_clock*4|circuit_pcb*16/#c:ingots/hard_plastic*4|#c:wires/lead*16",
		"circuit_controller|400|15000|perfluoromethyl:1000|circuit_chip*32/circuit_capacitor*32/circuit_capacitor_tantalium*16|circuit_controller_chassis*1/#c:ingots/rubber*4|#c:wires/lead*16",
		"circuit_controller_advanced|600|25000|perfluoromethyl:4000|circuit_chip_bismoid*16/circuit_capacitor_tantalium*48/circuit_atomic_clock*1|circuit_controller_chassis*1/#c:ingots/hard_plastic*4|#c:wires/lead*24",
		"circuit_controller_quantum|600|250000|perfluoromethyl_cold:6000|circuit_chip_quantum*16/circuit_chip_bismoid*48/circuit_atomic_clock*8|circuit_controller_advanced*2/upgrade_speed_3*1|#c:wires/lead*32",
		"sat_chip|100|100|-|circuit_chip*8/circuit_capacitor*2|circuit_pcb*2|#c:wires/lead*4",
		"upgrade_speed_1|200|1000|-|circuit_vacuum_tube*4/circuit_capacitor*1|upgrade_template*1/#c:dusts/red_copper*4|",
		"upgrade_effect_1|200|1000|-|circuit_vacuum_tube*4/circuit_capacitor*1|upgrade_template*1/#c:dusts/emerald*4|",
		"upgrade_power_1|200|1000|-|circuit_vacuum_tube*4/circuit_capacitor*1|upgrade_template*1/#c:dusts/gold*4|",
		"upgrade_fortune_1|200|1000|-|circuit_vacuum_tube*4/circuit_capacitor*1|upgrade_template*1/#c:dusts/niobium*4|",
		"upgrade_afterburn_1|200|1000|-|circuit_vacuum_tube*4/circuit_capacitor*1|upgrade_template*1/#c:dusts/tungsten*4|",
		"upgrade_radius|200|1000|-|circuit_chip*4/circuit_capacitor*4|upgrade_template*1/#c:dusts/glowstone*4|",
		"upgrade_health|200|1000|-|circuit_chip*4/circuit_capacitor*4|upgrade_template*1/#c:dusts/lithium*4|",
		"upgrade_speed_2|300|10000|-|circuit_chip*8/circuit_capacitor*4|upgrade_speed_1*1/#c:ingots/plastic*4|",
		"upgrade_speed_3|400|25000|solvent:500|circuit_chip*16/circuit_capacitor*16|upgrade_speed_2*1/#c:ingots/rubber*4|",
		"upgrade_effect_2|300|10000|-|circuit_chip*8/circuit_capacitor*4|upgrade_effect_1*1/#c:ingots/plastic*4|",
		"upgrade_effect_3|400|25000|solvent:500|circuit_chip*16/circuit_capacitor*16|upgrade_effect_2*1/#c:ingots/rubber*4|",
		"upgrade_power_2|300|10000|-|circuit_chip*8/circuit_capacitor*4|upgrade_power_1*1/#c:ingots/plastic*4|",
		"upgrade_power_3|400|25000|solvent:500|circuit_chip*16/circuit_capacitor*16|upgrade_power_2*1/#c:ingots/rubber*4|",
		"upgrade_fortune_2|300|10000|-|circuit_chip*8/circuit_capacitor*4|upgrade_fortune_1*1/#c:ingots/plastic*4|",
		"upgrade_fortune_3|400|25000|solvent:500|circuit_chip*16/circuit_capacitor*16|upgrade_fortune_2*1/#c:ingots/rubber*4|",
		"upgrade_afterburn_2|300|10000|-|circuit_chip*8/circuit_capacitor*4|upgrade_afterburn_1*1/#c:ingots/plastic*4|",
		"upgrade_afterburn_3|400|25000|solvent:500|circuit_chip*16/circuit_capacitor*16|upgrade_afterburn_2*1/#c:ingots/rubber*4|",
	};

	/** AmmoPressRecipes.recipes list order (index = GUI position and selectedRecipe value) */
	private static final String[] AMMO_ORDER = {
		"m357_bp", "m357_sp", "m357_fmj", "m357_jhp", "m357_ap", "m357_express",
		"m44_bp", "m44_sp", "m44_fmj", "m44_jhp", "m44_ap", "m44_express",
		"p22_sp", "p22_fmj", "p22_jhp", "p22_ap", "p9_sp", "p9_fmj",
		"p9_jhp", "p9_ap", "p45_sp", "p45_fmj", "p45_jhp", "p45_ap",
		"p45_du", "r556_sp", "r556_fmj", "r556_jhp", "r556_ap", "r762_sp",
		"r762_fmj", "r762_jhp", "r762_ap", "r762_du", "r762_he", "bmg50_sp",
		"bmg50_fmj", "bmg50_jhp", "bmg50_ap", "bmg50_du", "bmg50_he", "g12_bp",
		"g12_bp_magnum", "g12_bp_slug", "g12", "g12_slug", "g12_flechette", "g12_magnum",
		"g12_explosive", "g12_phosphorus", "g10", "g10_shrapnel", "g10_du", "g10_slug",
		"g10_explosive", "g26_flare", "g40_he", "g40_heat", "g40_demo", "g40_inc",
		"g40_phosphorus", "rocket_he_smokeless", "rocket_he_rocket_fuel", "rocket_heat_smokeless", "rocket_heat_rocket_fuel", "rocket_demo_smokeless",
		"rocket_demo_rocket_fuel", "rocket_inc_smokeless", "rocket_inc_rocket_fuel", "rocket_phosphorus_smokeless", "rocket_phosphorus_rocket_fuel", "rocket_micro",
		"flame_diesel", "flame_napalm", "flame_gas", "flame_balefire", "capacitor", "capacitor_overcharge",
		"capacitor_ir", "tau_uranium", "coil_tungsten", "coil_ferrouranium", "nuke_standard", "nuke_demo",
		"nuke_high", "nuke_tots", "nuke_hive", "nuke_balefire", "ct_hook", "ct_mortar",
		"ammo_dgk",
	};

	/**
	 * Explicit ArcFurnaceRecipes entries: input item | solid id*count | liquid material:quanta (MaterialShapes NUGGET 8,
	 * INGOT 72). ILMENITE.ingot (chunk_ore ilmenite) is not ported, so its recipe cannot exist.
	 */
	private static final String[] ARC = {
		"minecraft:sand|nugget_silicon*1|silicon:8",
		"minecraft:flint|nugget_silicon*4|silicon:36",
		"minecraft:quartz|nugget_silicon*3|silicon:24",
		"atomiclegacy:powder_quartz|nugget_silicon*3|silicon:24",
		"minecraft:quartz_block|nugget_silicon*12|silicon:96",
		"atomiclegacy:ingot_fiberglass|nugget_silicon*4|silicon:36",
		"atomiclegacy:block_fiberglass|nugget_silicon*40|silicon:324",
		"atomiclegacy:ingot_asbestos|nugget_silicon*4|silicon:36",
		"atomiclegacy:powder_asbestos|nugget_silicon*4|silicon:36",
		"atomiclegacy:block_asbestos|nugget_silicon*40|silicon:324",
		"atomiclegacy:sand_mix#quartz|glass_quartz*1|-", // [parity e3: ArcFurnaceRecipes uses sand_mix QUARTZ]
		"atomiclegacy:powder_borax|powder_boron_tiny*3|boron:24",
	};

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		site = 0;
		try {
			solderingTable(level, problems, cases);
			soldering(level, problems, cases);
			arcTable(level, problems, cases);
			arcFurnace(level, problems, cases);
			ammoPress(level, problems, cases);
		} catch(RuntimeException e) {
			AtomicLegacy.LOGGER.error("Parity K3C diagnostics crashed", e);
			problems.add("K3C diagnostics crashed: " + e);
		} finally {
			for(int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
			FORCED.clear();
		}
		return cases[0];
	}

	/* ---------------- soldering station ---------------- */

	private static void solderingTable(ServerLevel level, List<String> problems, int[] cases) {
		boolean defaults = !HbmConfig.ENABLE_528_MODE.get() && !HbmConfig.lbsmSimpleCrafting();
		if(!defaults) return;
		List<RecipeHolder<SolderingStationRecipe>> all = level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLDERING_STATION.get());
		check(all.size() == SOLDERING.length, problems, cases, "soldering: " + all.size() + " active recipes, original " + SOLDERING.length);
		List<RecipeHolder<SolderingStationRecipe>> unmatched = new ArrayList<>(all);
		for(String row : SOLDERING) {
			String[] f = row.split("\\|", -1);
			RecipeHolder<SolderingStationRecipe> hit = null;
			String why = "no result";
			for(RecipeHolder<SolderingStationRecipe> holder : unmatched) {
				SolderingStationRecipe r = holder.value();
				if(!BuiltInRegistries.ITEM.getKey(r.result().getItem()).getPath().equals(f[0]) || r.result().getCount() != 1) continue;
				why = "time/power " + r.duration() + "/" + r.consumption();
				if(r.duration() != Integer.parseInt(f[1]) || r.consumption() != Long.parseLong(f[2])) continue;
				String fluid = r.fluid().map(x -> x.fluid().name() + ":" + x.amount()).orElse("-");
				why = "fluid " + fluid;
				if(!fluid.equals(f[3])) continue;
				why = "groups " + group(r.toppings(), f[4]) + group(r.pcb(), f[5]) + group(r.solder(), f[6]);
				if(!group(r.toppings(), f[4]) || !group(r.pcb(), f[5]) || !group(r.solder(), f[6])) continue;
				hit = holder;
				break;
			}
			check(hit != null, problems, cases, "soldering: no loaded recipe equals original " + row + " (" + why + ")");
			if(hit != null) unmatched.remove(hit);
		}
		check(unmatched.isEmpty(), problems, cases, "soldering: recipes without an original counterpart " + unmatched.stream().map(RecipeHolder::id).toList());
		// SolderingRecipes.getRecipe returns the first item match: no two recipes may share an item signature
		for(int i = 0; i < all.size(); i++) for(int j = i + 1; j < all.size(); j++) {
			SolderingStationRecipe a = all.get(i).value(), b = all.get(j).value();
			boolean same = a.toppings().size() == b.toppings().size() && a.pcb().size() == b.pcb().size() && a.solder().size() == b.solder().size()
					&& a.matchesItems(sample(b));
			check(!same, problems, cases, "soldering: " + all.get(i).id() + " and " + all.get(j).id() + " share their items");
		}
	}

	private static SolderingStationRecipe.Input sample(SolderingStationRecipe r) {
		return new SolderingStationRecipe.Input(stacks(r.toppings(), 3), stacks(r.pcb(), 2), stacks(r.solder(), 1), HbmFluids.none(), 0);
	}

	private static List<ItemStack> stacks(List<SolderingStationRecipe.CountedIngredient> list, int size) {
		List<ItemStack> out = new ArrayList<>();
		for(SolderingStationRecipe.CountedIngredient c : list) {
			ItemStack[] items = c.ingredient().getItems();
			out.add(items.length == 0 ? ItemStack.EMPTY : items[0].copyWithCount(c.count()));
		}
		while(out.size() < size) out.add(ItemStack.EMPTY);
		return out;
	}

	private static boolean group(List<SolderingStationRecipe.CountedIngredient> actual, String expected) {
		List<String> parts = expected.isEmpty() ? List.of() : Arrays.asList(expected.split("(?<=\\*\\d{1,3})/")); // tags contain "/": split only after "*count"
		if(parts.size() != actual.size()) return false;
		List<SolderingStationRecipe.CountedIngredient> left = new ArrayList<>(actual);
		for(String part : parts) {
			String key = part.substring(0, part.lastIndexOf('*'));
			int count = Integer.parseInt(part.substring(part.lastIndexOf('*') + 1));
			ItemStack probe = probe(key);
			if(probe.isEmpty()) return false;
			SolderingStationRecipe.CountedIngredient found = null;
			for(SolderingStationRecipe.CountedIngredient c : left) if(c.count() == count && c.ingredient().test(probe) && sameTag(c.ingredient(), key)) { found = c; break; }
			if(found == null) return false;
			left.remove(found);
		}
		return true;
	}

	/** An OreDictStack must be the tag (every member accepted), a ComparableStack the single item. */
	private static boolean sameTag(Ingredient ingredient, String key) {
		if(!key.startsWith("#")) return ingredient.getItems().length >= 1;
		var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(key.substring(1)));
		for(var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) if(!ingredient.test(new ItemStack(holder))) return false;
		return true;
	}

	private static ItemStack probe(String key) {
		if(key.startsWith("#")) {
			var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(key.substring(1)));
			for(var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) return new ItemStack(holder);
			return ItemStack.EMPTY;
		}
		return new ItemStack(item(key));
	}

	/** [parity e3] BlockNTMSand variant stack: custom_data.variant as written by the sand_mix recipes. */
	private static ItemStack sandMix(String variant) {
		ItemStack stack = new ItemStack(item("sand_mix"));
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putString("variant", variant);
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
		return stack;
	}

	private static Item item(String key) {
		ResourceLocation id = key.contains(":") ? ResourceLocation.parse(key) : AtomicLegacy.id(key);
		return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
	}

	private static void soldering(ServerLevel level, List<String> problems, int[] cases) {
		Block block = HbmBlocks.MACHINE_SOLDERING_STATION.get();
		check(block.defaultDestroyTime() == 5.0F, problems, cases, "soldering: hardness " + block.defaultDestroyTime() + " expected 5");
		// ModBlocks: setResistance(30.0F); the port maps setResistance(r) to strength(h, r) (see machine_press 10 -> 10)
		check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, "soldering: resistance " + block.getExplosionResistance() + " expected 30 (registry/HbmBlocks solderingStation)"); // [parity T3: expected = original r*3/5]

		// circuit_analog: 100 ticks, 100 HE/t, maxPower = consumption * 20 = 2000
		SolderingStationBlockEntity st = placeSoldering(level);
		fillAnalog(st);
		int ticks = runSoldering(level, st, 1000, s -> !s.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).isEmpty());
		check(ticks == 100, problems, cases, "soldering analog: " + ticks + " ticks, original duration 100");
		check(st.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).is(item("circuit_analog")) && st.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).getCount() == 1, problems, cases, "soldering analog: output " + st.getItem(SolderingStationBlockEntity.OUTPUT_SLOT));
		check(st.getItem(0).getCount() + st.getItem(1).getCount() == 0 && st.getItem(3).isEmpty() && st.getItem(5).isEmpty(), problems, cases, "soldering analog: inputs not fully consumed");
		check(st.consumption() == 100 && st.maxPower() == 2000, problems, cases, "soldering analog: consumption " + st.consumption() + " maxPower " + st.maxPower() + " expected 100 / 2000");
		check(st.getRequestedInput() == st.maxPower() - st.power(), problems, cases, "soldering: receiver must request the whole free buffer");

		// speed 3 + overdrive 1: duration 100 - 100*3/6 = 50, consumption (100 + 300) * 2 = 800, progress +2 per tick -> 25 ticks
		st = placeSoldering(level);
		fillAnalog(st);
		st.setItem(SolderingStationBlockEntity.UPGRADE_START, new ItemStack(item("upgrade_speed_3")));
		st.setItem(SolderingStationBlockEntity.UPGRADE_START + 1, new ItemStack(item("upgrade_overdrive_1")));
		ticks = runSoldering(level, st, 1000, s -> !s.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).isEmpty());
		check(st.processTime() == 50 && st.consumption() == 800, problems, cases, "soldering upgrades: processTime " + st.processTime() + " consumption " + st.consumption() + " expected 50 / 800");
		check(ticks == 25, problems, cases, "soldering upgrades: " + ticks + " ticks, expected 25");
		// power 3: duration 100 + 100 = 200, consumption 100 - 50 = 50
		st = placeSoldering(level);
		fillAnalog(st);
		st.setItem(SolderingStationBlockEntity.UPGRADE_START, new ItemStack(item("upgrade_power_3")));
		runSoldering(level, st, 1, s -> false);
		check(st.processTime() == 200 && st.consumption() == 50, problems, cases, "soldering power upgrade: processTime " + st.processTime() + " consumption " + st.consumption() + " expected 200 / 50");

		// capacitor board needs 250 mB peroxide: without fluid the recipe is selected (consumption 300) but never progresses
		HbmFluidDefinition peroxide = HbmFluids.byName("peroxide").orElse(HbmFluids.none());
		st = placeSoldering(level);
		st.setItem(0, new ItemStack(item("circuit_capacitor_tantalium"), 3));
		st.setItem(3, new ItemStack(item("circuit_pcb"), 1));
		st.setItem(5, new ItemStack(item("wire_lead"), 3));
		runSoldering(level, st, 50, s -> false);
		check(st.consumption() == 300 && st.progress() == 0 && st.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).isEmpty(), problems, cases,
				"soldering fluid: without peroxide consumption " + st.consumption() + " progress " + st.progress());
		st.tank().setType(peroxide);
		st.tank().fill(peroxide, 1000, false);
		ticks = runSoldering(level, st, 1000, s -> !s.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).isEmpty());
		check(ticks == 200 && st.tank().amount() == 750, problems, cases, "soldering fluid: " + ticks + " ticks, tank " + st.tank().amount() + " expected 200 / 750");

		// collision prevention: no-fluid recipe blocked while the tank holds fluid
		st = placeSoldering(level);
		fillAnalog(st);
		st.tank().setType(peroxide);
		st.tank().fill(peroxide, 100, false);
		st.toggleCollisionPrevention();
		runSoldering(level, st, 150, s -> false);
		check(st.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).isEmpty() && st.progress() == 0, problems, cases, "soldering collision prevention ON still processed");
		st.toggleCollisionPrevention();
		ticks = runSoldering(level, st, 1000, s -> !s.getItem(SolderingStationBlockEntity.OUTPUT_SLOT).isEmpty());
		check(ticks == 100, problems, cases, "soldering collision prevention OFF: " + ticks + " ticks");

		// power loss resets the progress (canProcess false -> progress = 0)
		st = placeSoldering(level);
		fillAnalog(st);
		runSoldering(level, st, 10, s -> false);
		st.setPowerForDiagnostics(0);
		SolderingStationBlockEntity.tick(level, st.getBlockPos(), st.getBlockState(), st);
		check(st.progress() == 0, problems, cases, "soldering: progress " + st.progress() + " kept without power, original resets to 0");

		// slots: automation 0-6, extraction only 6, a second identical topping is rejected
		check(Arrays.equals(st.getSlotsForFace(Direction.UP), new int[] {0, 1, 2, 3, 4, 5, 6}), problems, cases, "soldering: accessible slots");
		check(st.canTakeItemThroughFace(6, ItemStack.EMPTY, Direction.UP) && !st.canTakeItemThroughFace(0, ItemStack.EMPTY, Direction.UP), problems, cases, "soldering: extraction slots");
		check(!st.canPlaceItem(1, new ItemStack(item("circuit_vacuum_tube"))), problems, cases, "soldering: duplicate topping accepted");
		check(!st.canPlaceItem(7, new ItemStack(item("circuit_pcb"))), problems, cases, "soldering: battery slot accepts a pcb through automation");
		// fluid reception only for the identifier-set type, never drained
		var handler = st.fluidHandler(Direction.UP);
		check(handler.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).isEmpty(), problems, cases, "soldering: tank can be drained, original is receiver only");
	}

	private static void fillAnalog(SolderingStationBlockEntity st) {
		st.setItem(0, new ItemStack(item("circuit_vacuum_tube"), 3));
		st.setItem(1, new ItemStack(item("circuit_capacitor"), 2));
		st.setItem(3, new ItemStack(item("circuit_pcb"), 4));
		st.setItem(5, new ItemStack(item("wire_lead"), 4));
	}

	private interface Done<T> { boolean test(T t); }

	private static int runSoldering(ServerLevel level, SolderingStationBlockEntity st, int max, Done<SolderingStationBlockEntity> done) {
		for(int t = 1; t <= max; t++) {
			st.applyPower(0L, 1_000_000L);
			SolderingStationBlockEntity.tick(level, st.getBlockPos(), st.getBlockState(), st);
			if(done.test(st)) return t;
		}
		return -1;
	}

	private static SolderingStationBlockEntity placeSoldering(ServerLevel level) {
		BlockPos pos = pad(level);
		BlockState state = HbmBlocks.MACHINE_SOLDERING_STATION.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		level.setBlock(pos, state, 3);
		state.getBlock().setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		return (SolderingStationBlockEntity) level.getBlockEntity(pos);
	}

	/* ---------------- arc furnace ---------------- */

	private static void arcTable(ServerLevel level, List<String> problems, int[] cases) {
		for(String row : ARC) {
			String[] f = row.split("\\|");
			ItemStack in = f[0].contains("#") ? sandMix(f[0].substring(f[0].indexOf('#') + 1)) : new ItemStack(item(f[0])); // [parity e3]
			if(in.isEmpty()) { check(false, problems, cases, "arc: input item " + f[0] + " missing"); continue; }
			Optional<RecipeHolder<ArcFurnaceRecipe>> found = level.getRecipeManager().getRecipeFor(HbmRecipeTypes.ARC_FURNACE.get(), new ArcFurnaceRecipe.Input(List.of(in)), level);
			check(found.isPresent(), problems, cases, "arc: no recipe for " + f[0]);
			if(found.isEmpty()) continue;
			ArcFurnaceRecipe r = found.get().value();
			String solid = BuiltInRegistries.ITEM.getKey(r.solidOutput().getItem()).getPath() + "*" + r.solidOutput().getCount();
			check(solid.equals(f[1]), problems, cases, "arc " + f[0] + ": solid " + solid + " expected " + f[1]);
			StringBuilder liquid = new StringBuilder();
			for(FoundryMaterialStack m : r.liquidStacks()) liquid.append(liquid.length() == 0 ? "" : ",").append(m.material().name()).append(':').append(m.amount());
			check((liquid.length() == 0 ? "-" : liquid.toString()).equals(f[2]), problems, cases, "arc " + f[0] + ": liquid " + liquid + " expected " + f[2]);
		}
	}

	private static void arcFurnace(ServerLevel level, List<String> problems, int[] cases) {
		Block block = HbmBlocks.MACHINE_ARC_FURNACE.get();
		check(block.defaultDestroyTime() == 5.0F && ParityT3Diagnostics.resistanceMatches(block), problems, cases, "arc: hardness/resistance " + block.defaultDestroyTime() + "/" + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]

		// solid mode, 1 sand, no upgrade: lid closes by 1/60 per tick, then 400 ticks at 1000 HE/t, delay 120 afterwards
		ArcFurnaceBlockEntity arc = placeArc(level);
		electrodes(arc);
		arc.setItem(ArcFurnaceBlockEntity.QUEUE_START, new ItemStack(Items.SAND, 3));
		int expected = arcTicks(0);
		long[] used = {0};
		int ticks = runArc(level, arc, 2000, a -> a.delay() > 0, used);
		check(ticks == expected, problems, cases, "arc solid: " + ticks + " ticks, original arithmetic gives " + expected);
		// loadIngredients spreads the queue over empty grid slots, at most getMaxInputSize() = 1 each
		for(int i = 0; i < 3; i++) {
			ItemStack grid = arc.getItem(ArcFurnaceBlockEntity.INPUT_START + i);
			check(grid.is(item("nugget_silicon")) && grid.getCount() == 1, problems, cases, "arc solid: grid slot " + i + " holds " + grid);
		}
		check(arc.getItem(ArcFurnaceBlockEntity.QUEUE_START).isEmpty(), problems, cases, "arc solid: queue not emptied into the grid");
		check(used[0] == progressSteps(400) * 1000L, problems, cases, "arc solid: used " + used[0] + " HE, expected " + progressSteps(400) + " x 1000"); // [parity K3: float progress += 1F/duration needs 401 steps]
		check(arc.delay() == 120, problems, cases, "arc solid: delay " + arc.delay() + " expected 120");
		check(ArcElectrodeItem.durability(arc.getItem(0)) == 1, problems, cases, "arc solid: electrode durability " + ArcElectrodeItem.durability(arc.getItem(0)));
		check(arc.getRequestedInput() == ArcFurnaceBlockEntity.MAX_POWER - arc.menuData().get(0), problems, cases, "arc: receiver must request the whole free buffer");

		// liquid mode: 4 sand melt to 4 x 8 quanta silicon in one cycle
		arc = placeArc(level);
		electrodes(arc);
		arc.toggleLiquidMode();
		arc.setItem(ArcFurnaceBlockEntity.UPGRADE_SLOT, new ItemStack(item("upgrade_speed_2")));
		arc.setItem(ArcFurnaceBlockEntity.QUEUE_START, new ItemStack(Items.SAND, 8));
		check(arc.getMaxInputSize() == 8, problems, cases, "arc: max input size " + arc.getMaxInputSize() + " with speed 2, expected 8");
		expected = arcTicks(2);
		used[0] = 0;
		ticks = runArc(level, arc, 2000, a -> a.delay() > 0, used);
		int silicon = arc.liquids().stream().filter(m -> m.material().name().equals("silicon")).mapToInt(FoundryMaterialStack::amount).sum();
		check(ticks == expected, problems, cases, "arc liquid speed 2: " + ticks + " ticks, original arithmetic gives " + expected);
		check(silicon == 64, problems, cases, "arc liquid: " + silicon + " quanta silicon, expected 8 sand x 8");
		check(used[0] == progressSteps(400 / 5) * 25_000L, problems, cases, "arc liquid speed 2: used " + used[0] + " HE, expected " + progressSteps(400 / 5) + " x 25000");
		// with molten material inside the furnace never starts again
		arc.setItem(ArcFurnaceBlockEntity.QUEUE_START + 1, new ItemStack(Items.SAND, 1));
		runArc(level, arc, 300, a -> false, used);
		check(!arc.working(), problems, cases, "arc: works although the melt was not poured out");

		// slot rules (isItemValidForSlot / canInsertItem / canExtractItem)
		check(arc.canPlaceItem(0, arc.getItem(0)) && !arc.canPlaceItem(0, new ItemStack(Items.SAND)), problems, cases, "arc: electrode slot rules");
		check(!arc.canPlaceItem(ArcFurnaceBlockEntity.BATTERY_SLOT, new ItemStack(Items.SAND)) && !arc.canPlaceItem(ArcFurnaceBlockEntity.UPGRADE_SLOT, new ItemStack(item("upgrade_speed_1"))), problems, cases, "arc: slots 3/4 must not be valid for automation");
		check(arc.canPlaceItemThroughFace(ArcFurnaceBlockEntity.QUEUE_START, new ItemStack(Items.SAND), Direction.UP)
				&& !arc.canPlaceItemThroughFace(ArcFurnaceBlockEntity.INPUT_START, new ItemStack(Items.SAND), Direction.UP), problems, cases, "arc: automation inserts only into electrodes and the queue");
	}

	/** Original arithmetic: lid -= 1F / (60F / (u * 0.5 + 1)) until 0, then progress += 1F / (400 / (2u + 1)) until >= 1F. */
	/** Ticks the original float accumulator (progress += 1F / duration until >= 1F) takes; each consumes power. */
	private static long progressSteps(int duration) {
		float progress = 0F;
		long steps = 0;
		while(progress < 1F) {
			progress += 1F / duration;
			steps++;
		}
		return steps;
	}

	private static int arcTicks(int upgrade) {
		float lid = 1F;
		int ticks = 0;
		while(lid > 0F) {
			lid -= 1F / (60F / (upgrade * 0.5 + 1));
			if(lid < 0F) lid = 0F;
			ticks++;
		}
		int duration = 400 / (upgrade * 2 + 1);
		float progress = 0F;
		while(progress < 1F) {
			progress += 1F / duration;
			ticks++;
		}
		return ticks;
	}

	private static void electrodes(ArcFurnaceBlockEntity arc) {
		for(int i = 0; i < 3; i++) arc.setItem(i, HbmItems.variantStack(HbmItems.ARC_ELECTRODE_ITEMS, "graphite"));
	}

	private static int runArc(ServerLevel level, ArcFurnaceBlockEntity arc, int max, Done<ArcFurnaceBlockEntity> done, long[] used) {
		for(int t = 1; t <= max; t++) {
			arc.applyPower(0L, ArcFurnaceBlockEntity.MAX_POWER);
			long before = arc.menuData().get(0);
			ArcFurnaceBlockEntity.tick(level, arc.getBlockPos(), arc.getBlockState(), arc);
			used[0] += before - arc.menuData().get(0);
			if(done.test(arc)) return t;
		}
		return -1;
	}

	private static ArcFurnaceBlockEntity placeArc(ServerLevel level) {
		BlockPos pos = pad(level);
		BlockState state = HbmBlocks.MACHINE_ARC_FURNACE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		level.setBlock(pos, state, 3);
		state.getBlock().setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		return (ArcFurnaceBlockEntity) level.getBlockEntity(pos);
	}

	/* ---------------- ammo press ---------------- */

	private static void ammoPress(ServerLevel level, List<String> problems, int[] cases) {
		Block block = HbmBlocks.MACHINE_AMMO_PRESS.get();
		check(block.defaultDestroyTime() == 5.0F && ParityT3Diagnostics.resistanceMatches(block), problems, cases, "ammo press: hardness/resistance " + block.defaultDestroyTime() + "/" + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
		BlockPos pos = pad(level);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		level.setBlock(pos, state, 3);
		state.getBlock().setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
		AmmoPressBlockEntity press = (AmmoPressBlockEntity) level.getBlockEntity(pos);

		List<RecipeHolder<AmmoPressRecipe>> recipes = press.recipes(level);
		check(recipes.size() == AMMO_ORDER.length, problems, cases, "ammo press: " + recipes.size() + " recipes, original " + AMMO_ORDER.length);
		for(int i = 0; i < Math.min(recipes.size(), AMMO_ORDER.length); i++) {
			check(recipes.get(i).id().getPath().equals("ammo_press/" + AMMO_ORDER[i]), problems, cases, "ammo press: GUI position " + i + " is " + recipes.get(i).id() + ", original " + AMMO_ORDER[i]);
		}
		Map<String, Integer> counts = new HashMap<>();
		for(RecipeHolder<AmmoPressRecipe> r : recipes) counts.merge(r.id().getPath(), 1, Integer::sum);

		// no selection: nothing is valid; select recipe 0 (.357 BP: 2 lead, gunpowder, small casing -> 16)
		check(!press.canPlaceItem(1, new ItemStack(Items.GUNPOWDER)), problems, cases, "ammo press: input accepted without a selected recipe");
		press.selectRecipeByIndex(0, false);
		AmmoPressRecipe recipe = recipes.get(0).value();
		for(int i = 0; i < 9; i++) {
			AmmoPressRecipe.SlotIngredient cell = recipe.input().get(i);
			if(!cell.isEmpty()) press.setItem(i, cell.ingredient().orElseThrow().getItems()[0].copyWithCount(cell.count() * 3));
		}
		check(press.canPlaceItem(4, new ItemStack(Items.GUNPOWDER)) && !press.canPlaceItem(0, new ItemStack(Items.GUNPOWDER)), problems, cases, "ammo press: cell rules of the selected recipe");
		AmmoPressBlockEntity.tick(level, pos, press.getBlockState(), press);
		ItemStack out = press.getItem(AmmoPressBlockEntity.OUTPUT_SLOT);
		check(out.getCount() == 48 && ItemStack.isSameItemSameComponents(out, recipe.result()), problems, cases, "ammo press: one tick must process every batch (performRecipe recursion), output " + out);
		check(press.getItem(1).isEmpty() && press.getItem(4).isEmpty() && press.getItem(7).isEmpty(), problems, cases, "ammo press: inputs left after processing");
		check(Arrays.equals(press.getSlotsForFace(Direction.UP), new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) && press.canTakeItemThroughFace(9, out, Direction.UP)
				&& !press.canTakeItemThroughFace(0, out, Direction.UP), problems, cases, "ammo press: automation slots");
		// an extra item in an empty cell blocks the recipe
		for(int i = 0; i < 9; i++) {
			AmmoPressRecipe.SlotIngredient cell = recipe.input().get(i);
			if(!cell.isEmpty()) press.setItem(i, cell.ingredient().orElseThrow().getItems()[0].copyWithCount(cell.count()));
		}
		press.setItem(0, new ItemStack(Items.STONE));
		AmmoPressBlockEntity.tick(level, pos, press.getBlockState(), press);
		check(press.getItem(AmmoPressBlockEntity.OUTPUT_SLOT).getCount() == 48, problems, cases, "ammo press: processed with a foreign item in an empty cell");
		// selecting the same recipe again clears the selection
		press.setSelectedRecipe(recipes.get(0).id());
		check(press.selectedRecipeIndex() == -1, problems, cases, "ammo press: reselecting does not clear the selection");
	}

	/* ---------------- helpers ---------------- */

	private static void check(boolean ok, List<String> problems, int[] cases, String message) {
		cases[0]++;
		if(!ok) problems.add("K3C " + message);
	}

	private static BlockPos pad(ServerLevel level) {
		int index = site++;
		BlockPos pos = new BlockPos(BASE_X + (index % 6) * SPACING, BASE_Y, BASE_Z + (index / 6) * SPACING);
		for(int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 8) >> 4; cx++) {
			for(int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				level.getChunk(cx, cz);
				FORCED.add(new int[] { cx, cz });
			}
		}
		for(int dx = -6; dx <= 6; dx++) for(int dy = -2; dy <= 8; dy++) for(int dz = -6; dz <= 6; dz++) {
			level.setBlock(pos.offset(dx, dy, dz), dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		return pos;
	}
}
