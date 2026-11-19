package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.BlastFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.CombinationOvenRecipe;
import com.atomiclegacy.inventory.recipes.PressRecipe;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.ElectricFurnaceBlockEntity;
import com.atomiclegacy.tileentity.FurnaceCombinationBlockEntity;
import com.atomiclegacy.tileentity.IronFurnaceBlockEntity;
import com.atomiclegacy.tileentity.MachineBlastFurnaceBlockEntity;
import com.atomiclegacy.tileentity.PressBlockEntity;
import com.atomiclegacy.tileentity.SteelFurnaceBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity K3] Dedicated-server checks for group A of batch K3 (machine_press, machine_epress, furnace_iron,
 * furnace_steel, furnace_combination, machine_blast_furnace, machine_electric_furnace) against the pinned 1.7.10
 * sources. Every machine is placed on its own stone pad far from the origin and ticked directly; the expected tick
 * counts, fuel and energy use are computed by re-running the 1.7.10 updateEntity arithmetic of the referenced
 * TileEntity inside the test. Recipe tables were compiled from PressRecipes, BlastFurnaceRecipesNT and
 * CombinationRecipes (registerDefaults). Not covered: client GUI drawing, sounds, particles, fluid networks.
 */
final class ParityK3ADiagnostics {

	private ParityK3ADiagnostics() {
	}

	private static final int BASE_X = 7_000;
	private static final int BASE_Y = 200;
	private static final int BASE_Z = 7_000;
	private static final int SPACING = 32;
	private static int site;
	private static final List<int[]> FORCED = new ArrayList<>();

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		site = 0;
		try {
			properties(problems, cases);
			fuels(level, problems, cases);
			firePress(level, problems, cases);
			electricPress(level, problems, cases, 0);
			electricPress(level, problems, cases, 3);
			ironFurnace(level, problems, cases, 0);
			ironFurnace(level, problems, cases, 3);
			steelFurnace(level, problems, cases);
			combinationOven(level, problems, cases);
			blastFurnace(level, problems, cases);
			electricFurnace(level, problems, cases, null);
			electricFurnace(level, problems, cases, "upgrade_speed_3");
			electricFurnace(level, problems, cases, "upgrade_power_3");
			pressRecipes(server, problems, cases);
			blastRecipes(server, problems, cases);
			combinationRecipes(server, problems, cases);
		} catch(RuntimeException e) {
			AtomicLegacy.LOGGER.error("Parity K3A diagnostics crashed", e);
			problems.add("K3A diagnostics crashed: " + e);
		} finally {
			for(int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
			FORCED.clear();
		}
		return cases[0];
	}

	/// ModBlocks.java:1761-1763, 1786, 1827, 2233-2234 ///
	private static void properties(List<String> problems, int[] cases) {
		Object[][] rows = {
				{HbmBlocks.MACHINE_PRESS.get(), 5.0F, 10.0F}, {HbmBlocks.MACHINE_EPRESS.get(), 5.0F, 10.0F},
				{HbmBlocks.FURNACE_IRON.get(), 5.0F, 10.0F}, {HbmBlocks.FURNACE_STEEL.get(), 5.0F, 10.0F},
				{HbmBlocks.FURNACE_COMBINATION.get(), 5.0F, 10.0F}, {HbmBlocks.MACHINE_BLAST_FURNACE.get(), 5.0F, 10.0F},
				{HbmBlocks.MACHINE_ELECTRIC_FURNACE_OFF.get(), 5.0F, 10.0F},
		};
		for(Object[] row : rows) {
			Block block = (Block) row[0];
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			check(block.defaultDestroyTime() == (float) row[1], problems, cases, id + " hardness " + block.defaultDestroyTime());
			check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
		}
	}

	/// FuelHandler, ModuleBurnTime, TileEntityFurnaceIron / TileEntityMachineBlastFurnace burn modules ///
	private static void fuels(ServerLevel level, List<String> problems, int[] cases) {
		// TileEntityFurnace.getItemBurnTime for the burner press
		checkEq(PressBlockEntity.burnTimeOf(new ItemStack(Items.COAL)), 1600, problems, cases, "press coal burn");
		checkEq(PressBlockEntity.burnTimeOf(new ItemStack(Items.OAK_LOG)), 300, problems, cases, "press log burn (no ModuleBurnTime bonus)");
		checkEq(PressBlockEntity.burnTimeOf(stack("solid_fuel_presto_triplet", 1)), 40_000, problems, cases, "press presto triplet burn");
		checkEq(PressBlockEntity.burnTimeOf(stack("block_coke", 1)), 32_000, problems, cases, "press block_coke burn"); // [parity K3: port has one block_coke item]
		// furnace_iron: lignite/coal 1.25, coke 1.5, solid/rocket/balefire 2
		checkEq(IronFurnaceBlockEntity.fuelDuration(new ItemStack(Items.COAL)), 2000, problems, cases, "iron furnace coal");
		checkEq(IronFurnaceBlockEntity.fuelDuration(stack("lignite", 1)), 1500, problems, cases, "iron furnace lignite");
		checkEq(IronFurnaceBlockEntity.fuelDuration(stack("coke_coal", 1)), 4800, problems, cases, "iron furnace coke");
		checkEq(IronFurnaceBlockEntity.fuelDuration(stack("solid_fuel", 1)), 6400, problems, cases, "iron furnace solid fuel");
		checkEq(IronFurnaceBlockEntity.fuelDuration(stack("solid_fuel_bf", 1)), 64_000, problems, cases, "iron furnace balefire fuel");
		checkEq(IronFurnaceBlockEntity.fuelDuration(stack("rocket_fuel", 1)), 12_800, problems, cases, "iron furnace rocket fuel");
		checkEq(IronFurnaceBlockEntity.fuelDuration(new ItemStack(Items.OAK_PLANKS)), 300, problems, cases, "iron furnace planks");
		checkEq(IronFurnaceBlockEntity.fuelDuration(new ItemStack(Items.BLAZE_ROD)), 2400, problems, cases, "iron furnace blaze rod");
		// blast furnace: getBurnHeat(getBurnTime(stack, 0D)), wood heat 0, container items 0
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.COAL)), 1600, problems, cases, "blast coal");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, stack("coke_coal", 1)), 3200, problems, cases, "blast coke");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, stack("solid_fuel_presto", 1)), 8000, problems, cases, "blast presto");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.OAK_LOG)), 300, problems, cases, "blast log");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.OAK_PLANKS)), 0, problems, cases, "blast planks (wood heat 0)");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, stack("briquette_wood", 1)), 0, problems, cases, "blast wood briquette");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.BLAZE_ROD)), 0, problems, cases, "blast blaze rod (def 0)");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.CHARCOAL)), 0, problems, cases, "blast charcoal (no Coal ore name)");
		checkEq(MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.LAVA_BUCKET)), 0, problems, cases, "blast lava bucket (container item)");
	}

	/// TileEntityMachinePress#updateEntity ///
	private static void firePress(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level);
		PressBlockEntity press = place(level, pos, HbmBlocks.MACHINE_PRESS.get().defaultBlockState(), PressBlockEntity.class);
		if(press == null) {
			check(false, problems, cases, "burner press block entity missing");
			return;
		}
		press.setItem(PressBlockEntity.FIRE_FUEL_SLOT, new ItemStack(Items.COAL, 1));
		press.setItem(PressBlockEntity.STAMP_SLOT, stack("stamp_iron_plate", 1));
		press.setItem(PressBlockEntity.INPUT_SLOT, new ItemStack(Items.IRON_INGOT, 3));
		int expected = legacyFirePressTicks(1600, 3);
		int ticks = 0;
		while(press.getItem(PressBlockEntity.OUTPUT_SLOT).getCount() < 3 && ticks < 5000) {
			PressBlockEntity.tick(level, pos, level.getBlockState(pos), press);
			ticks++;
		}
		checkEq(ticks, expected, problems, cases, "burner press ticks for 3 plates");
		check(press.getItem(PressBlockEntity.OUTPUT_SLOT).is(item("plate_iron")), problems, cases, "burner press output " + press.getItem(PressBlockEntity.OUTPUT_SLOT));
		checkEq(press.burnTime(), 1600 - 3 * 200, problems, cases, "burner press burn time after 3 operations");
		checkEq(press.getItem(PressBlockEntity.STAMP_SLOT).getDamageValue(), 3, problems, cases, "stamp wear");
		// automation: TileEntityMachinePress#getAccessibleSlotsFromSide = {0, 1, 2, 3} on the core only
		WorldlyContainer sided = press;
		check(java.util.Arrays.equals(sided.getSlotsForFace(Direction.UP), new int[] {0, 1, 2, 3}), problems, cases, "burner press sided slots");
		check(press.canPlaceItem(PressBlockEntity.INPUT_SLOT, new ItemStack(Items.COAL)), problems, cases, "burner press input takes fuel items");
		check(!press.canPlaceItem(PressBlockEntity.INPUT_SLOT, stack("stamp_iron_flat", 1)), problems, cases, "stamps only in slot 1");
		check(press.getSlotsForAccessor(pos.above(), Direction.NORTH).length == 0, problems, cases, "burner press dummies are no inventories");
		cleanup(level, pos);
	}

	/** Re-run of the 1.7.10 burner press loop: one fuel item, {@code ops} operations, returns the ticks needed. */
	private static int legacyFirePressTicks(int fuelValue, int ops) {
		int speed = 0, burn = 0, press = 0, delay = 0, done = 0, fuelItems = 1;
		boolean retracting = false;
		for(int t = 1; t < 10_000; t++) {
			boolean canProcess = burn >= 200 && done < ops;
			if((canProcess || retracting) && burn >= 200) speed = Math.min(400, speed + 1); else speed = Math.max(0, speed - 1);
			if(delay <= 0) {
				int stampSpeed = speed * 25 / 400;
				if(retracting) {
					press -= stampSpeed;
					if(press <= 0) { retracting = false; delay = 5; }
				} else if(canProcess) {
					press += stampSpeed;
					if(press >= 200) {
						done++;
						retracting = true;
						delay = 5;
						if(burn >= 200) burn -= 200;
						if(done == ops) return t;
					}
				} else if(press > 0) retracting = true;
			} else delay--;
			if(fuelItems > 0 && burn < 200) { burn += fuelValue; fuelItems--; }
		}
		return -1;
	}

	/// TileEntityMachineEPress#updateEntity (speed = 1 + level, stamp 45 / 20 * (1 + speed / 4), delay 5 - speed + 1) ///
	private static void electricPress(ServerLevel level, List<String> problems, int[] cases, int speedLevel) {
		BlockPos pos = pad(level);
		PressBlockEntity press = place(level, pos, HbmBlocks.MACHINE_EPRESS.get().defaultBlockState(), PressBlockEntity.class);
		if(press == null) {
			check(false, problems, cases, "electric press block entity missing");
			return;
		}
		press.applyPower(0L, PressBlockEntity.ELECTRIC_ENERGY_CAPACITY);
		press.setItem(PressBlockEntity.STAMP_SLOT, stack("stamp_iron_plate", 1));
		press.setItem(PressBlockEntity.INPUT_SLOT, new ItemStack(Items.IRON_INGOT, 2));
		if(speedLevel > 0) press.setItem(PressBlockEntity.ELECTRIC_UPGRADE_SLOT, stack("upgrade_speed_" + speedLevel, 1));
		int[] expected = legacyEPressTicks(speedLevel, 2);
		int ticks = 0;
		while(press.getItem(PressBlockEntity.OUTPUT_SLOT).getCount() < 2 && ticks < 2000) {
			PressBlockEntity.tick(level, pos, level.getBlockState(pos), press);
			ticks++;
		}
		checkEq(ticks, expected[0], problems, cases, "electric press ticks (speed " + speedLevel + ")");
		checkEq(press.energy(), (int) PressBlockEntity.ELECTRIC_ENERGY_CAPACITY - expected[1], problems, cases, "electric press energy use (speed " + speedLevel + ")");
		check(java.util.Arrays.equals(press.getSlotsForFace(Direction.UP), new int[] {1, 2, 3}), problems, cases, "electric press sided slots");
		cleanup(level, pos);
	}

	private static int[] legacyEPressTicks(int level, int ops) {
		int press = 0, delay = 0, done = 0, used = 0;
		boolean retracting = false;
		for(int t = 1; t < 10_000; t++) {
			boolean canProcess = done < ops;
			if(canProcess || retracting || delay > 0) {
				used += 100;
				if(delay <= 0) {
					int speed = 1 + level;
					int stampSpeed = retracting ? 20 : 45;
					stampSpeed *= (1D + (double) speed / 4D);
					if(retracting) {
						press -= stampSpeed;
						if(press <= 0) { retracting = false; delay = 5 - speed + 1; }
					} else if(canProcess) {
						press += stampSpeed;
						if(press >= 200) {
							done++;
							retracting = true;
							delay = 5 - speed + 1;
							if(done == ops) return new int[] {t, used};
						}
					} else if(press > 0) retracting = true;
				} else delay--;
			}
		}
		return new int[] {-1, -1};
	}

	/// TileEntityFurnaceIron: baseTime 160, processingTime = 160 - 80 * speed / 3 ///
	private static void ironFurnace(ServerLevel level, List<String> problems, int[] cases, int speedLevel) {
		BlockPos pos = pad(level);
		IronFurnaceBlockEntity furnace = place(level, pos, HbmBlocks.FURNACE_IRON.get().defaultBlockState(), IronFurnaceBlockEntity.class);
		if(furnace == null) {
			check(false, problems, cases, "iron furnace block entity missing");
			return;
		}
		furnace.setItem(IronFurnaceBlockEntity.FUEL_A_SLOT, new ItemStack(Items.COAL, 2));
		furnace.setItem(IronFurnaceBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON, 2));
		if(speedLevel > 0) furnace.setItem(IronFurnaceBlockEntity.UPGRADE_SLOT, stack("upgrade_speed_" + speedLevel, 1));
		int time = 160 - (80 * speedLevel / 3);
		for(int t = 0; t < time; t++) IronFurnaceBlockEntity.tick(level, pos, level.getBlockState(pos), furnace);
		checkEq(furnace.getItem(IronFurnaceBlockEntity.OUTPUT_SLOT).getCount(), 1, problems, cases, "iron furnace output after " + time + " ticks");
		checkEq(furnace.menuData().get(1), 2000 - time, problems, cases, "iron furnace burn time left (speed " + speedLevel + ")");
		checkEq(furnace.getItem(IronFurnaceBlockEntity.FUEL_A_SLOT).getCount(), 1, problems, cases, "iron furnace fuel items used");
		cleanup(level, pos);
	}

	/// TileEntityFurnaceSteel: processTime 40,000 TU, burn = (heat - maxHeat / 3) / 10, passive loss heat / 1000 ///
	private static void steelFurnace(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level);
		SteelFurnaceBlockEntity furnace = place(level, pos, HbmBlocks.FURNACE_STEEL.get().defaultBlockState(), SteelFurnaceBlockEntity.class);
		if(furnace == null) {
			check(false, problems, cases, "steel furnace block entity missing");
			return;
		}
		furnace.setItem(0, new ItemStack(Items.RAW_IRON, 1));
		furnace.menuData().set(6, 100_000);
		int heat = 100_000, progress = 0, expected = -1;
		for(int t = 1; t < 2000 && expected < 0; t++) {
			if(heat < 100_000) heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
			int burn = (heat - 100_000 / 3) / 10;
			if(heat >= 100_000 / 3) { progress += burn; heat -= burn; }
			if(progress >= 40_000) expected = t;
		}
		int ticks = 0;
		while(furnace.getItem(3).isEmpty() && ticks < 2000) {
			SteelFurnaceBlockEntity.tick(level, pos, level.getBlockState(pos), furnace);
			ticks++;
		}
		checkEq(ticks, expected, problems, cases, "steel furnace ticks for one smelt at full heat");
		checkEq(furnace.getItem(3).getCount(), 1, problems, cases, "steel furnace output (raw iron has no ore bonus)");
		cleanup(level, pos);
	}

	/// TileEntityFurnaceCombination: processTime 20,000, burn = heat / 100, CombinationRecipes coal -> coke + 100 mB creosote ///
	private static void combinationOven(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level);
		FurnaceCombinationBlockEntity oven = place(level, pos, HbmBlocks.FURNACE_COMBINATION.get().defaultBlockState(), FurnaceCombinationBlockEntity.class);
		if(oven == null) {
			check(false, problems, cases, "combination oven block entity missing");
			return;
		}
		oven.setItem(FurnaceCombinationBlockEntity.INPUT_SLOT, new ItemStack(Items.COAL, 1));
		oven.menuData().set(5, 100_000);
		int heat = 100_000, progress = 0, expected = -1;
		for(int t = 1; t < 5000 && expected < 0; t++) {
			if(heat < 100_000) heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
			int burn = heat / 100;
			if(burn > 0) { progress += burn; heat -= burn; }
			if(progress >= 20_000) expected = t;
		}
		int ticks = 0;
		while(oven.getItem(FurnaceCombinationBlockEntity.ITEM_OUTPUT_SLOT).isEmpty() && ticks < 5000) {
			FurnaceCombinationBlockEntity.tick(level, pos, level.getBlockState(pos), oven);
			ticks++;
		}
		checkEq(ticks, expected, problems, cases, "combination oven ticks for coal");
		check(oven.getItem(FurnaceCombinationBlockEntity.ITEM_OUTPUT_SLOT).is(item("coke_coal")), problems, cases, "combination oven coke output");
		checkEq(oven.tank().amount(), 100, problems, cases, "combination oven creosote amount");
		check("coalcreosote".equals(oven.tank().type().name()), problems, cases, "combination oven tank type " + oven.tank().type().name());
		cleanup(level, pos);
	}

	/// TileEntityMachineBlastFurnace + BlastFurnaceRecipesNT blast.steelFromIngot (800 ticks, 2 iron + sand) ///
	private static void blastFurnace(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level);
		MachineBlastFurnaceBlockEntity furnace = place(level, pos, HbmBlocks.MACHINE_BLAST_FURNACE.get().defaultBlockState(), MachineBlastFurnaceBlockEntity.class);
		if(furnace == null) {
			check(false, problems, cases, "blast furnace block entity missing");
			return;
		}
		furnace.setItem(MachineBlastFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 1));
		furnace.setItem(MachineBlastFurnaceBlockEntity.UPPER_INPUT_SLOT, new ItemStack(Items.IRON_INGOT, 2));
		furnace.setItem(MachineBlastFurnaceBlockEntity.LOWER_INPUT_SLOT, new ItemStack(Items.SAND, 1));
		float progress = 0F;
		int expected = -1;
		for(int t = 1; t < 5000 && expected < 0; t++) {
			progress += 1F / 800;
			if(progress >= 1F) expected = t;
		}
		int ticks = 0;
		while(furnace.getItem(MachineBlastFurnaceBlockEntity.OUTPUT_SLOT).isEmpty() && ticks < 5000) {
			MachineBlastFurnaceBlockEntity.tick(level, pos, level.getBlockState(pos), furnace);
			ticks++;
		}
		checkEq(ticks, expected, problems, cases, "blast furnace ticks without airblast");
		ItemStack out = furnace.getItem(MachineBlastFurnaceBlockEntity.OUTPUT_SLOT);
		check(out.is(item("ingot_steel")) && out.getCount() == 2, problems, cases, "blast furnace steel output " + out);
		ItemStack slag = furnace.getItem(MachineBlastFurnaceBlockEntity.BYPRODUCT_SLOT);
		check(slag.is(item("ingot_raw_slag")) && slag.getCount() == 1, problems, cases, "blast furnace slag " + slag);
		checkEq(furnace.menuData().get(0), 1600 - 800, problems, cases, "blast furnace fuel after one operation");
		checkEq(furnace.flueTank().amount(), 1000, problems, cases, "blast furnace flue gas (8 mB * 800 capped at 1000)");
		// airblast decay: (int) (fill * 0.95)
		furnace.airblastTank().setAmount(21);
		MachineBlastFurnaceBlockEntity.tick(level, pos, level.getBlockState(pos), furnace);
		checkEq(furnace.airblastTank().amount(), 19, problems, cases, "airblast decay");
		furnace.setItem(MachineBlastFurnaceBlockEntity.LOWER_INPUT_SLOT, new ItemStack(Items.SAND, 1));
		check(furnace.canPlaceItemThroughFace(MachineBlastFurnaceBlockEntity.UPPER_INPUT_SLOT, new ItemStack(Items.IRON_INGOT), Direction.UP),
				problems, cases, "blast furnace accepts a recipe input in the free slot");
		check(!furnace.canPlaceItemThroughFace(MachineBlastFurnaceBlockEntity.UPPER_INPUT_SLOT, new ItemStack(Items.SAND), Direction.UP),
				problems, cases, "blast furnace rejects the same item as the other input slot");
		cleanup(level, pos);
	}

	/// TileEntityMachineElectricFurnace: 100 ticks / 50 HE, speed -25 ticks +50 HE, power +10 ticks -15 HE ///
	private static void electricFurnace(ServerLevel level, List<String> problems, int[] cases, String upgrade) {
		BlockPos pos = pad(level);
		ElectricFurnaceBlockEntity furnace = place(level, pos, HbmBlocks.MACHINE_ELECTRIC_FURNACE_OFF.get().defaultBlockState(), ElectricFurnaceBlockEntity.class);
		if(furnace == null) {
			check(false, problems, cases, "electric furnace block entity missing");
			return;
		}
		int speed = upgrade != null && upgrade.startsWith("upgrade_speed") ? 3 : 0;
		int power = upgrade != null && upgrade.startsWith("upgrade_power") ? 3 : 0;
		int maxProgress = 100 - speed * 25 + power * 10;
		int consumption = 50 + speed * 50 - power * 15;
		furnace.applyPower(0L, ElectricFurnaceBlockEntity.ENERGY_CAPACITY);
		furnace.setItem(ElectricFurnaceBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON, 1));
		if(upgrade != null) furnace.setItem(ElectricFurnaceBlockEntity.UPGRADE_SLOT, stack(upgrade, 1));
		long start = furnace.getMenuData().get(0);
		int ticks = 0;
		while(furnace.getItem(ElectricFurnaceBlockEntity.OUTPUT_SLOT).isEmpty() && ticks < 1000) {
			ElectricFurnaceBlockEntity.tick(level, pos, level.getBlockState(pos), furnace);
			ticks++;
		}
		String tag = upgrade == null ? "none" : upgrade;
		checkEq(ticks, maxProgress, problems, cases, "electric furnace ticks (" + tag + ")");
		checkEq((int) (start - furnace.getMenuData().get(0)), maxProgress * consumption, problems, cases, "electric furnace energy (" + tag + ")");
		check(!furnace.canTakeItemThroughFace(ElectricFurnaceBlockEntity.BATTERY_SLOT, ItemStack.EMPTY, Direction.DOWN), problems, cases, "electric furnace battery not extractable");
		cleanup(level, pos);
	}

	/// PressRecipes#registerDefaults: 11 FLAT, 13 PLATE (ingot + plate ore names), 4 casings, 11 WIRE (WIRE autogen), 1 CIRCUIT, 8 PRINTING ///
	private static void pressRecipes(MinecraftServer server, List<String> problems, int[] cases) {
		List<String> expected = new ArrayList<>(List.of(
				"flat|minecraft:quartz|1", "flat|minecraft:lapis_lazuli|1", "flat|minecraft:diamond|1", "flat|minecraft:emerald|1",
				"flat|atomiclegacy:biomass_compressed|1", "flat|atomiclegacy:ingot_graphite|1", "flat|atomiclegacy:meteorite_sword_hardened|1",
				"flat|atomiclegacy:ball_resin|1", "flat|atomiclegacy:briquette_coal|1", "flat|atomiclegacy:briquette_lignite|1",
				"flat|atomiclegacy:briquette_wood|1",
				"c9|atomiclegacy:casing_small|4", "c50|atomiclegacy:casing_large|2", "c9|atomiclegacy:casing_small_steel|4",
				"c50|atomiclegacy:casing_large_steel|2", "circuit|atomiclegacy:circuit_silicon|1"));
		for(String plate : new String[] {"iron", "gold", "schrabidium", "titanium", "copper", "aluminium", "steel", "lead", "dura_steel",
				"combine_steel", "gunmetal", "weaponsteel", "saturnite"}) expected.add("plate|atomiclegacy:plate_" + plate + "|1");
		for(String wire : new String[] {"carbon", "gold", "schrabidium", "copper", "tungsten", "aluminium", "lead", "zirconium", "steel",
				"red_copper", "magnetized_tungsten"}) expected.add("wire|atomiclegacy:wire_" + wire + "|8");
		for(int i = 1; i <= 8; i++) expected.add("printing" + i + "|atomiclegacy:page_of_page" + i + "|1");
		List<String> actual = new ArrayList<>();
		for(RecipeHolder<PressRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get())) {
			PressRecipe recipe = holder.value();
			actual.add(recipe.stamp().getSerializedName() + "|" + BuiltInRegistries.ITEM.getKey(recipe.result().getItem()) + "|" + recipe.result().getCount());
			check(recipe.inputCount() == 1, problems, cases, "press recipe " + holder.id() + " consumes " + recipe.inputCount());
		}
		compareLists(expected, actual, problems, cases, "press recipes");
		Map<String, Item> inputs = Map.of("plate|atomiclegacy:plate_iron|1", Items.IRON_INGOT, "flat|atomiclegacy:ball_resin|1", Items.JUNGLE_LOG,
				"flat|minecraft:quartz|1", item("powder_quartz"), "circuit|atomiclegacy:circuit_silicon|1", item("billet_silicon"));
		for(RecipeHolder<PressRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get())) {
			PressRecipe recipe = holder.value();
			Item input = inputs.get(recipe.stamp().getSerializedName() + "|" + BuiltInRegistries.ITEM.getKey(recipe.result().getItem()) + "|" + recipe.result().getCount());
			if(input != null) check(recipe.ingredient().test(new ItemStack(input)), problems, cases, "press recipe input of " + holder.id());
		}
	}

	/// BlastFurnaceRecipesNT#registerDefaults (blast.mingradeIngot / mingradeCursed need ingotRedstone, which no item carries) ///
	private static void blastRecipes(MinecraftServer server, List<String> problems, int[] cases) {
		List<String> expected = List.of(
				"blast.steelFromIngot|2|1|800|atomiclegacy:ingot_steel|2|atomiclegacy:ingot_raw_slag|1",
				"blast.steelFromDust|2|1|800|atomiclegacy:ingot_steel|2|atomiclegacy:ingot_raw_slag|1",
				"blast.steelFromOre|1|1|800|atomiclegacy:ingot_steel|2|atomiclegacy:ingot_raw_slag|2",
				"blast.steelWithFlux|1|1|1200|atomiclegacy:ingot_steel|3|atomiclegacy:ingot_raw_slag|2",
				"blast.mingrade|1|1|400|atomiclegacy:ingot_red_copper|2|minecraft:air|0",
				"blast.mingradeDust|1|1|400|atomiclegacy:ingot_red_copper|2|minecraft:air|0",
				"blast.mingradeOre|1|6|1200|atomiclegacy:ingot_red_copper|6|atomiclegacy:ingot_raw_slag|1",
				"blast.meteorSword|1|1|1200|atomiclegacy:meteorite_sword_alloyed|1|minecraft:air|0",
				"blast.meteor|1|1|600|atomiclegacy:ingot_meteorite|1|minecraft:air|0",
				"blast.starmetal|1|1|600|atomiclegacy:ingot_starmetal|1|minecraft:air|0",
				"blast.paa|1|1|600|atomiclegacy:plate_paa|1|minecraft:air|0",
				"blast.firebrick|1|7|800|atomiclegacy:ingot_firebrick|8|minecraft:air|0",
				"blast.firebrickLimestone|1|6|800|atomiclegacy:ingot_firebrick|8|minecraft:air|0");
		List<String> actual = new ArrayList<>();
		for(RecipeHolder<BlastFurnaceRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
			BlastFurnaceRecipe r = holder.value();
			actual.add(r.group() + "|" + r.inputACount() + "|" + r.inputBCount() + "|" + r.duration() + "|" + BuiltInRegistries.ITEM.getKey(r.result().getItem())
					+ "|" + r.result().getCount() + "|" + BuiltInRegistries.ITEM.getKey(r.byproduct().getItem()) + "|" + r.byproduct().getCount());
		}
		compareLists(new ArrayList<>(expected), actual, problems, cases, "blast furnace recipes");
	}

	/// CombinationRecipes#registerDefaults: 23 fixed entries + 5 grades x 6 bedrock ore types ///
	private static void combinationRecipes(MinecraftServer server, List<String> problems, int[] cases) {
		Map<String, String> expected = new HashMap<>();
		expected.put("minecraft:coal", "atomiclegacy:coke_coal|1|coalcreosote|100");
		expected.put("atomiclegacy:powder_coal", "atomiclegacy:coke_coal|1|coalcreosote|100");
		expected.put("atomiclegacy:briquette_coal", "atomiclegacy:coke_coal|1|coalcreosote|150");
		expected.put("atomiclegacy:lignite", "atomiclegacy:coke_lignite|1|coalcreosote|50");
		expected.put("atomiclegacy:powder_lignite", "atomiclegacy:coke_lignite|1|coalcreosote|50");
		expected.put("atomiclegacy:briquette_lignite", "atomiclegacy:coke_lignite|1|coalcreosote|100");
		expected.put("atomiclegacy:powder_chlorocalcite", "atomiclegacy:powder_calcium|1|chlorine|250");
		expected.put("atomiclegacy:powder_molysite", "minecraft:iron_ingot|1|chlorine|250");
		expected.put("minecraft:glowstone_dust", "atomiclegacy:sulfur|1|chlorine|100");
		expected.put("atomiclegacy:gem_sodalite", "atomiclegacy:powder_sodium|1|chlorine|100");
		expected.put("atomiclegacy:chunk_ore_cryolite", "atomiclegacy:powder_aluminium|1|lye|150");
		expected.put("atomiclegacy:powder_sodium", "minecraft:air|0|sodium|100");
		expected.put("atomiclegacy:powder_limestone", "atomiclegacy:powder_calcium|1|carbondioxide|50");
		expected.put("minecraft:oak_log", "minecraft:charcoal|1|woodoil|250");
		expected.put("minecraft:oak_sapling", "atomiclegacy:powder_ash_wood|1|woodoil|50");
		expected.put("atomiclegacy:briquette_wood", "minecraft:charcoal|1|woodoil|500");
		expected.put("atomiclegacy:oil_tar", "atomiclegacy:coke_petroleum|1|none|0");
		expected.put("atomiclegacy:oil_tar_crack", "atomiclegacy:coke_petroleum|1|none|0");
		expected.put("atomiclegacy:oil_tar_coal", "atomiclegacy:coke_coal|1|none|0");
		expected.put("atomiclegacy:oil_tar_wood", "atomiclegacy:coke_coal|1|none|0");
		expected.put("minecraft:sugar_cane", "minecraft:sugar|2|ethanol|50");
		expected.put("minecraft:clay", "minecraft:bricks|1|none|0");
		List<RecipeHolder<CombinationOvenRecipe>> recipes = server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COMBINATION_OVEN.get());
		checkEq(recipes.size(), 23 + 5 * 6, problems, cases, "combination oven recipe count");
		int vitriol = 0;
		for(RecipeHolder<CombinationOvenRecipe> holder : recipes) {
			CombinationOvenRecipe r = holder.value();
			String fluid = r.hasFluid() ? r.fluid().fluid().name() + "|" + r.fluid().amount() : "none|0";
			String out = BuiltInRegistries.ITEM.getKey(r.output().getItem()) + "|" + r.output().getCount() + "|" + fluid;
			if(fluid.equals("vitriol|50") && r.output().is(item("bedrock_ore_new"))) vitriol++;
			for(Map.Entry<String, String> entry : expected.entrySet()) {
				Item input = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(entry.getKey()));
				if(input != Items.AIR && r.input().test(new ItemStack(input))) {
					check(entry.getValue().equals(out), problems, cases, "combination " + entry.getKey() + " -> " + out + " expected " + entry.getValue());
				}
			}
		}
		checkEq(vitriol, 30, problems, cases, "combination bedrock roasting recipes (vitriol 50)");
	}

	/// Helpers ///
	private static void check(boolean condition, List<String> problems, int[] cases, String message) {
		cases[0]++;
		if(!condition) problems.add("K3A: " + message);
	}

	private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
		check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
	}

	private static void compareLists(List<String> expected, List<String> actual, List<String> problems, int[] cases, String what) {
		Collections.sort(expected);
		Collections.sort(actual);
		List<String> missing = new ArrayList<>(expected);
		for(String a : actual) missing.remove(a);
		List<String> extra = new ArrayList<>(actual);
		for(String e : expected) extra.remove(e);
		check(missing.isEmpty() && extra.isEmpty(), problems, cases, what + " differ: missing " + missing + ", extra " + extra);
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.get(AtomicLegacy.id(path));
	}

	private static ItemStack stack(String path, int count) {
		return new ItemStack(item(path), count);
	}

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> T place(ServerLevel level, BlockPos pos, BlockState state, Class<T> type) {
		level.setBlock(pos, state, 2 | 16);
		BlockEntity be = level.getBlockEntity(pos);
		return type.isInstance(be) ? (T) be : null;
	}

	private static BlockPos pad(ServerLevel level) {
		int index = site++;
		BlockPos pos = new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
		for(int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 8) >> 4; cx++) {
			for(int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				level.getChunk(cx, cz);
				FORCED.add(new int[] {cx, cz});
			}
		}
		for(int dx = -3; dx <= 3; dx++) for(int dy = -2; dy <= 8; dy++) for(int dz = -3; dz <= 3; dz++) {
			level.setBlock(pos.offset(dx, dy, dz), dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		return pos;
	}

	private static void cleanup(ServerLevel level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if(be instanceof net.minecraft.world.Container container) container.clearContent();
		for(BlockPos cursor : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 8, 3))) {
			if(!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
	}
}
