package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.inventory.recipes.CyclotronRecipe;
import com.atomiclegacy.inventory.recipes.ExposureChamberRecipe;
import com.atomiclegacy.inventory.recipes.GasCentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;
import com.atomiclegacy.inventory.recipes.PurexRecipe;
import com.atomiclegacy.inventory.recipes.SilexRecipe;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.ChemicalFactoryBlockEntity;
import com.atomiclegacy.tileentity.CyclotronBlockEntity;
import com.atomiclegacy.tileentity.ExposureChamberBlockEntity;
import com.atomiclegacy.tileentity.FelBlockEntity;
import com.atomiclegacy.tileentity.GasCentrifugeBlockEntity;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.SilexBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * [parity Q2] Nuclear-chemical and advanced processing machines against the pinned 1.7.10 sources
 * (TileEntityMachineCyclotron, -ExposureChamber, TileEntitySILEX, TileEntityFEL, -PUREX, -GasCent, -Radiolysis,
 * -PrecAss, -ChemicalFactory, -Annihilator, -Autocrafter, -KeyForge, -Hephaestus, -LPW2 and their *Recipes).
 * Recipe tables are generated from the original by work/tmp-q2/gen_tables.py. Machines are placed on pads near
 * x = 21000 and ticked directly. Not covered: client animation, sounds, GUI drawing, the power network itself.
 */
final class ParityQ2Diagnostics {

    private ParityQ2Diagnostics() {
    }

    private static final int BASE_X = 21_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 21_000;
    private static final int SPACING = 32;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            properties(problems, cases);
            cyclotronRecipes(server, problems, cases);
            exposureRecipes(server, problems, cases);
            silexRecipes(server, problems, cases);
            purexRecipes(server, problems, cases);
            precassRecipes(server, problems, cases);
            gasCentrifugeRecipes(server, problems, cases);
            cyclotronBehaviour(level, problems, cases);
            felBehaviour(level, problems, cases);
            gasCentrifugeBehaviour(level, problems, cases);
            silexBehaviour(level, problems, cases);
            exposureBehaviour(level, problems, cases);
            radiolysisBehaviour(level, problems, cases);
            chemicalFactoryBehaviour(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity Q2 diagnostics crashed", e);
            problems.add("Q2 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
            FORCED.clear();
        }
        return cases[0];
    }

    /// ModBlocks.java: setHardness / setResistance ///
    private static void properties(List<String> problems, int[] cases) {
        Object[][] table = {
                {HbmBlocks.MACHINE_GASCENT.get(), 5.0F, 10.0F}, {HbmBlocks.MACHINE_FEL.get(), 5.0F, 10.0F},
                {HbmBlocks.MACHINE_SILEX.get(), 5.0F, 10.0F}, {HbmBlocks.MACHINE_CYCLOTRON.get(), 5.0F, 10.0F},
                {HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get(), 5.0F, 10.0F}, {HbmBlocks.MACHINE_RADIOLYSIS.get(), 10.0F, 10.0F},
                {HbmBlocks.MACHINE_HEPHAESTUS.get(), 10.0F, 10.0F}, {HbmBlocks.MACHINE_KEYFORGE.get(), 5.0F, 10.0F},
                {HbmBlocks.MACHINE_ANNIHILATOR.get(), 5.0F, 10.0F}, {HbmBlocks.MACHINE_PRECASS.get(), 5.0F, 30.0F},
                {HbmBlocks.MACHINE_CHEMICAL_FACTORY.get(), 5.0F, 30.0F}, {HbmBlocks.MACHINE_PUREX.get(), 5.0F, 30.0F},
                {HbmBlocks.MACHINE_LPW2.get(), 5.0F, 100.0F}, {HbmBlocks.MACHINE_AUTOCRAFTER.get(), 10.0F, 20.0F}};
        for (Object[] row : table) {
            Block block = (Block) row[0];
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(block.defaultDestroyTime() == (float) row[1], problems, cases, id + " hardness " + block.defaultDestroyTime() + " expected " + row[1]);
            check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance() + " expected " + row[2]); // [parity T3: expected = original r*3/5]
        }
    }

    /// CyclotronRecipes ///
    private static void cyclotronRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        ServerLevel level = server.overworld();
        int loaded = server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get()).size();
        check(loaded == CYCLOTRON.length, problems, cases, "cyclotron has " + loaded + " recipes, the original " + CYCLOTRON.length);
        for (String row : CYCLOTRON) {
            String[] f = row.split("\\|");
            ItemStack particle = stack(f[0], 1);
            ItemStack input = f[1].startsWith("c:") ? firstOfTag(f[1]) : stack(f[1], 1);
            Optional<RecipeHolder<CyclotronRecipe>> recipe = server.getRecipeManager().getRecipeFor(HbmRecipeTypes.CYCLOTRON.get(),
                    new CyclotronRecipe.Input(particle, input), level);
            check(recipe.isPresent(), problems, cases, "cyclotron " + row + " missing");
            if (recipe.isEmpty()) continue;
            String actual = f[0] + "|" + f[1] + "|" + path(recipe.get().value().output()) + "|" + recipe.get().value().antimatter();
            check(actual.equals(row) && recipe.get().value().output().getCount() == 1, problems, cases, "cyclotron " + actual + " expected " + row);
        }
        // ComparableStack inputs accept only that item, dustNetherQuartz is not certus or fluix
        check(server.getRecipeManager().getRecipeFor(HbmRecipeTypes.CYCLOTRON.get(),
                new CyclotronRecipe.Input(stack("part_lithium", 1), stack("powder_quartz", 1)), level)
                .map(r -> path(r.value().output()).equals("powder_fire")).orElse(false), problems, cases, "cyclotron nether quartz dust");
    }

    /// ExposureChamberRecipes (default config) ///
    private static void exposureRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        String[][] rows = {{"particle_higgs", "ingot_uranium", "ingot_schraranium"}, {"particle_higgs", "ingot_u238", "ingot_schrabidium"},
                {"particle_dark", "ingot_plutonium", "ingot_euphemium"}, {"particle_sparkticle", "ingot_schrabidate", "ingot_dineutronium"}};
        int loaded = server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.EXPOSURE_CHAMBER.get()).size();
        check(loaded == rows.length, problems, cases, "exposure chamber has " + loaded + " recipes, the original " + rows.length);
        for (String[] row : rows) {
            Optional<RecipeHolder<ExposureChamberRecipe>> recipe = server.getRecipeManager().getRecipeFor(HbmRecipeTypes.EXPOSURE_CHAMBER.get(),
                    new ExposureChamberRecipe.Input(stack(row[0], 1), stack(row[1], 1)), server.overworld());
            check(recipe.isPresent() && path(recipe.get().value().output()).equals(row[2]) && recipe.get().value().output().getCount() == 1,
                    problems, cases, "exposure chamber " + String.join("+", row) + " -> " + recipe.map(r -> path(r.value().output())).orElse("none"));
        }
        check(server.getRecipeManager().getRecipeFor(HbmRecipeTypes.EXPOSURE_CHAMBER.get(),
                new ExposureChamberRecipe.Input(stack("particle_sparkticle", 1), stack("ingot_schrabidium", 1)), server.overworld()).isEmpty(),
                problems, cases, "exposure chamber accepts schrabidium instead of schrabidate (SBD)");
    }

    /// SILEXRecipes.register ///
    private static void silexRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        Map<String, SilexRecipe> byPath = new HashMap<>();
        for (RecipeHolder<SilexRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SILEX.get())) {
            byPath.put(holder.id().getPath(), holder.value());
        }
        check(byPath.size() == SILEX.length, problems, cases, "SILEX has " + byPath.size() + " recipes, the original " + SILEX.length);
        for (String row : SILEX) {
            String[] f = row.split("\\|");
            SilexRecipe recipe = byPath.get(f[0]);
            check(recipe != null, problems, cases, "SILEX recipe " + f[0] + " missing");
            if (recipe == null) continue;
            List<String> outs = new ArrayList<>();
            for (SilexRecipe.WeightedOutput out : recipe.outputs()) {
                outs.add(path(out.stack()) + ":" + meta(out.stack()) + "x" + out.stack().getCount() + "@" + out.weight());
            }
            String actual = f[0] + "|" + recipe.fluidProduced() + "|" + recipe.fluidConsumed() + "|" + recipe.wavelength().getSerializedName()
                    + "|" + String.join(",", outs);
            check(actual.equals(row), problems, cases, "SILEX " + actual + " expected " + row);
        }
    }

    /// PUREXRecipes.registerDefaults ///
    private static void purexRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        Map<String, PurexRecipe> byPath = new HashMap<>();
        for (RecipeHolder<PurexRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get())) {
            byPath.put(holder.id().getPath(), holder.value());
        }
        check(byPath.size() == PUREX.length, problems, cases, "PUREX has " + byPath.size() + " recipes, the original " + PUREX.length);
        for (String row : PUREX) {
            String[] f = row.split("\\|", -1);
            PurexRecipe recipe = byPath.get(f[0]);
            check(recipe != null, problems, cases, "PUREX recipe " + f[0] + " missing");
            if (recipe == null) continue;
            List<String> counts = new ArrayList<>();
            recipe.inputItems().forEach(in -> counts.add(String.valueOf(in.count())));
            List<String> fin = new ArrayList<>();
            recipe.inputFluids().forEach(fl -> fin.add(fl.type().name() + ":" + fl.amount()));
            List<String> fout = new ArrayList<>();
            recipe.outputFluids().forEach(fl -> fout.add(fl.type().name() + ":" + fl.amount()));
            List<String> outs = new ArrayList<>();
            for (PurexRecipe.PurexItemOutput out : recipe.outputItems()) {
                outs.add(path(out.stack()) + "x" + out.stack().getCount() + (out.chance() < 1.0F ? "@" + out.chance() : ""));
            }
            String actual = f[0] + "|" + recipe.group() + "|" + recipe.duration() + "|" + recipe.power() + "|" + String.join(",", counts) + "|"
                    + String.join(",", fin) + "|" + String.join(",", fout) + "|" + String.join(",", outs);
            check(actual.equals(row), problems, cases, "PUREX " + actual + " expected " + row);
        }
    }

    /// PrecAssRecipes.registerDefaults with enable528 = false, enableExpensiveMode = false ///
    private static void precassRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        String[] rows = {
                "precision_assembler/orbital_assembly_crystal_circuit|600|20000|4,4,16,32||50,50|",
                "precision_assembler/orbital_assembly_crystal_circuit_recycle|600|20000|1||100,100,100,100|",
                "precision_assembler/blueprints|6000|20000|16,16,4||10,90|",
                "precision_assembler/beigeprints|6000|50000|24,24,8||5,95|",
                "precision_assembler/rpa_helmet|3600|25000|12,4,4,8||100|discover..rpa",
                "precision_assembler/rpa_plate|3600|25000|24,8,8,8,16,1||100|discover..rpa",
                "precision_assembler/rpa_legs|3600|25000|24,8,8,16||100|discover..rpa",
                "precision_assembler/rpa_boots|3600|25000|12,4,8||100|discover..rpa"};
        Map<String, PrecisionAssemblerRecipe> byPath = new HashMap<>();
        for (RecipeHolder<PrecisionAssemblerRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRECISION_ASSEMBLER.get())) {
            byPath.put(holder.id().getPath(), holder.value());
        }
        check(byPath.size() == rows.length, problems, cases, "precision assembler has " + byPath.size() + " default recipes, the original " + rows.length);
        for (String row : rows) {
            String[] f = row.split("\\|", -1);
            PrecisionAssemblerRecipe recipe = byPath.get(f[0]);
            check(recipe != null, problems, cases, "precision assembler recipe " + f[0] + " missing");
            if (recipe == null) continue;
            List<String> counts = new ArrayList<>();
            recipe.ingredients().forEach(in -> counts.add(String.valueOf(in.count())));
            List<String> fin = new ArrayList<>();
            recipe.inputFluids().forEach(fl -> fin.add(fl.type().name() + ":" + fl.amount()));
            List<String> weights = new ArrayList<>();
            recipe.outputs().forEach(out -> weights.add(String.valueOf(out.weight())));
            String actual = f[0] + "|" + recipe.duration() + "|" + recipe.power() + "|" + String.join(",", counts) + "|" + String.join(",", fin)
                    + "|" + String.join(",", weights) + "|" + String.join(",", recipe.blueprintPools());
            check(actual.equals(row), problems, cases, "precision assembler " + actual + " expected " + row);
        }
    }

    /// GasCentrifugeRecipes.PseudoFluidType (internal UF6 / PuF6 / Watz stages) ///
    private static void gasCentrifugeRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        String[] rows = {"heuf6|300||0|true|nugget_u238x2,nugget_u235x1,fluoritex1", "meuf6|200|heuf6|100|false|nugget_u238x1",
                "leuf6|300|meuf6|200|false|nugget_u238x1,fluoritex1", "uf6|400|leuf6|300|false|nugget_u238x1",
                "puf6|300||0|false|nugget_pu238x1,nugget_pu_mixx2,fluoritex1", "watz_heavy|500||0|false|powder_ironx1,dustx1,nuclear_waste_tinyx1",
                "watz|1000|watz_heavy|500|false|powder_leadx1,dustx1"};
        Map<String, GasCentrifugeRecipe> byFluid = new HashMap<>();
        for (RecipeHolder<GasCentrifugeRecipe> holder : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.GAS_CENTRIFUGE.get())) {
            byFluid.put(holder.value().input().fluid().name(), holder.value());
        }
        check(byFluid.size() == rows.length, problems, cases, "gas centrifuge has " + byFluid.size() + " stages, the original " + rows.length);
        for (String row : rows) {
            String fluid = row.substring(0, row.indexOf('|'));
            GasCentrifugeRecipe recipe = byFluid.get(fluid);
            check(recipe != null, problems, cases, "gas centrifuge stage " + fluid + " missing");
            if (recipe == null) continue;
            List<String> outs = new ArrayList<>();
            recipe.results().forEach(out -> outs.add(path(out) + "x" + out.getCount()));
            String actual = fluid + "|" + recipe.input().amount() + "|" + (recipe.output().isEmpty() ? "" : recipe.output().fluid().name()) + "|"
                    + (recipe.output().isEmpty() ? 0 : recipe.output().amount()) + "|" + recipe.highSpeed() + "|" + String.join(",", outs);
            check(actual.equals(row), problems, cases, "gas centrifuge " + actual + " expected " + row);
        }
    }

    /// TileEntityMachineCyclotron: upgrades, coolant, antimatter clamp, automation ports ///
    private static void cyclotronBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        CyclotronBlockEntity c = (CyclotronBlockEntity) place(level, HbmBlocks.MACHINE_CYCLOTRON.get());
        check(c.getSpeed() == 1 && c.getConsumption() == 1_000_000 && c.getCoolantConsumption() == 500, problems, cases,
                "cyclotron base speed/consumption/coolant " + c.getSpeed() + "/" + c.getConsumption() + "/" + c.getCoolantConsumption());
        c.setItem(10, stack("upgrade_speed_1", 1));
        c.setItem(11, stack("upgrade_power_1", 1));
        check(c.getSpeed() == 2 && c.getConsumption() == 900_000 && c.getCoolantConsumption() == 1_000, problems, cases,
                "cyclotron speed I + power I " + c.getSpeed() + "/" + c.getConsumption() + "/" + c.getCoolantConsumption());
        c.setItem(11, stack("upgrade_effect_2", 1));
        check(c.getConsumption() == 1_000_000 && c.getCoolantConsumption() == 332, problems, cases,
                "cyclotron effect II coolant " + c.getCoolantConsumption() + " expected 500 / 3 * 2 = 332");
        c.setItem(11, stack("upgrade_power_1", 1));
        c.setItem(0, stack("part_lithium", 2));
        c.setItem(3, stack("powder_lithium", 2));
        HbmFluidDefinition water = fluid("water");
        int ticks = 0;
        long used = -1;
        while (ticks < 400 && c.getItem(6).isEmpty()) {
            c.waterTank().fill(water, 32_000, false);
            c.spentSteamTank().setAmount(0);
            c.applyPower(0L, CyclotronBlockEntity.MAX_POWER);
            if (ticks == 343) c.antimatterTank().setAmount(7_990);
            long before = c.power();
            CyclotronBlockEntity.tick(level, c.getBlockPos(), c.getBlockState(), c);
            if (ticks == 0) used = before - c.power();
            ticks++;
        }
        check(used == 900_000, problems, cases, "cyclotron consumed " + used + " HE/t, expected 1000000 - 100000 * power level");
        check(ticks == 345, problems, cases, "cyclotron cycle " + ticks + " ticks, expected 690 / 2 = 345");
        check(path(c.getItem(6)).equals("powder_beryllium") && c.getItem(6).getCount() == 1 && c.getItem(0).getCount() == 1,
                problems, cases, "cyclotron output " + c.getItem(6));
        check(c.antimatterTank().amount() == 8_000, problems, cases, "cyclotron antimatter " + c.antimatterTank().amount() + " expected clamp to 8000");
        BlockPos core = c.getBlockPos();
        Direction rot = Direction.NORTH.getClockWise();
        check(Arrays.equals(c.getSlotsForAccessor(core.relative(Direction.NORTH, 2).relative(rot)), new int[] {0, 3, 6, 7, 8})
                && Arrays.equals(c.getSlotsForAccessor(core.relative(Direction.NORTH, 2)), new int[] {1, 4, 6, 7, 8})
                && Arrays.equals(c.getSlotsForAccessor(core.relative(Direction.NORTH, 2).relative(rot.getOpposite())), new int[] {2, 5, 6, 7, 8})
                && Arrays.equals(c.getSlotsForAccessor(core), new int[] {6, 7, 8}), problems, cases, "cyclotron port slot layout");
        check(c.isAutomationPort(core.offset(2, 0, 1)) && !c.isAutomationPort(core.offset(2, 0, 2)) && !c.isAutomationPort(core.offset(2, 1, 0)),
                problems, cases, "cyclotron automation ports are the twelve makeExtra blocks");
        check(c.canPlaceItemThroughFace(0, stack("part_lithium", 1), Direction.UP) && !c.canPlaceItemThroughFace(0, stack("powder_lithium", 1), Direction.UP)
                && c.canPlaceItemThroughFace(3, stack("powder_lithium", 1), Direction.UP)
                && !c.canPlaceItemThroughFace(9, stack("battery_potato", 1), Direction.UP), problems, cases, "cyclotron isItemValidForSlot");
    }

    /// TileEntityFEL: req = 1250 * 3^ordinal, power drained to 0 when short ///
    private static void felBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        FelBlockEntity fel = (FelBlockEntity) place(level, HbmBlocks.MACHINE_FEL.get());
        fel.setItem(FelBlockEntity.CRYSTAL_SLOT, stack("laser_crystal_co2", 1));
        fel.toggleEnabled();
        fel.applyPower(0L, 20_000_000L);
        long before = fel.power();
        FelBlockEntity.tick(level, fel.getBlockPos(), fel.getBlockState(), fel);
        check(before - fel.power() == 3_750L, problems, cases, "FEL IR used " + (before - fel.power()) + " HE, expected 1250 * 3");
        fel.setItem(FelBlockEntity.CRYSTAL_SLOT, stack("laser_crystal_bismuth", 1));
        before = fel.power();
        FelBlockEntity.tick(level, fel.getBlockPos(), fel.getBlockState(), fel);
        check(before - fel.power() == 11_250L, problems, cases, "FEL visible used " + (before - fel.power()) + " HE, expected 1250 * 9");
        FelBlockEntity weak = (FelBlockEntity) place(level, HbmBlocks.MACHINE_FEL.get());
        weak.setItem(FelBlockEntity.CRYSTAL_SLOT, stack("laser_crystal_co2", 1));
        weak.toggleEnabled();
        weak.applyPower(0L, 3_000L);
        FelBlockEntity.tick(level, weak.getBlockPos(), weak.getBlockState(), weak);
        check(weak.power() == 0L, problems, cases, "FEL with 3000 HE < 3750 kept " + weak.power() + " HE, expected 0");
    }

    /// TileEntityMachineGasCent: running dry resets the cycle ///
    private static void gasCentrifugeBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        GasCentrifugeBlockEntity g = (GasCentrifugeBlockEntity) place(level, HbmBlocks.MACHINE_GASCENT.get());
        g.applyFluidIdentifier(FluidIdentifierItem.forFluid(fluid("uf6")));
        g.inputTank().fill(fluid("uf6"), 4_000, false);
        g.applyPower(0L, 100L);
        GasCentrifugeBlockEntity.tick(level, g.getBlockPos(), g.getBlockState(), g);
        check(g.getMenuData().get(0) == 0 && g.getMenuData().get(2) == 0, problems, cases,
                "gas centrifuge with 100 HE: power " + g.getMenuData().get(0) + " progress " + g.getMenuData().get(2) + ", expected 0 / 0");
        g.applyPower(0L, 100_000L);
        GasCentrifugeBlockEntity.tick(level, g.getBlockPos(), g.getBlockState(), g);
        check(g.getMenuData().get(0) == 100_000 - 200 && g.getMenuData().get(2) == 1 && g.getMenuData().get(3) == 150, problems, cases,
                "gas centrifuge tick: power " + g.getMenuData().get(0) + " progress " + g.getMenuData().get(2) + " time " + g.getMenuData().get(3));
    }

    /// SILEXRecipes.getOutput: tiny waste uses the full waste recipe with fluidProduced / 9 ///
    private static void silexBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        SilexBlockEntity s = (SilexBlockEntity) place(level, HbmBlocks.MACHINE_SILEX.get());
        ItemStack tiny = waste("nuclear_waste_short_tiny", 3);
        ItemStack tinyAmericium = waste("nuclear_waste_short_tiny", 6);
        check(s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, waste("nuclear_waste_short", 3)), problems, cases, "SILEX rejects Pu-239 short waste");
        check(s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, tiny), problems, cases, "SILEX rejects tiny Pu-239 short waste");
        // outside Q2: the 3x3 tiny <-> full waste crafting recipes use the same component encoding
        level.getServer().getRecipeManager().byKey(AtomicLegacy.id("nuclear_waste/short_plutonium239_from_tiny")).ifPresent(holder -> {
            if (holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
                    && !shaped.getIngredients().get(0).test(tiny)) {
                AtomicLegacy.LOGGER.warn("[parity Q2] nuclear_waste/short_plutonium239_from_tiny does not accept a real tiny waste stack");
            }
        });
        check(!s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, tinyAmericium), problems, cases, "SILEX accepts tiny Am-242 waste without a full-size recipe");
        check(s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, stack("powder_uranium", 1)) && s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, new ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI))
                && s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, stack("powder_lapis", 1)) && s.canPlaceItem(SilexBlockEntity.INPUT_SLOT, new ItemStack(net.minecraft.world.item.Items.GRAVEL)),
                problems, cases, "SILEX translations (uranium dust, lapis, gravel)");
    }

    /// TileEntityMachineExposureChamber#isItemValidForSlot ///
    private static void exposureBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        ExposureChamberBlockEntity e = (ExposureChamberBlockEntity) place(level, HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get());
        check(e.canPlaceItemThroughFace(ExposureChamberBlockEntity.PARTICLE_SLOT, stack("particle_higgs", 1), Direction.UP)
                && !e.canPlaceItemThroughFace(ExposureChamberBlockEntity.PARTICLE_SLOT, stack("ingot_uranium", 1), Direction.UP), problems, cases,
                "exposure chamber empty: only recipe particles");
        e.setItem(ExposureChamberBlockEntity.INGREDIENT_SLOT, stack("ingot_plutonium", 1));
        check(!e.canPlaceItemThroughFace(ExposureChamberBlockEntity.PARTICLE_SLOT, stack("particle_higgs", 1), Direction.UP)
                && e.canPlaceItemThroughFace(ExposureChamberBlockEntity.PARTICLE_SLOT, stack("particle_dark", 1), Direction.UP), problems, cases,
                "exposure chamber with plutonium: only the matching particle");
    }

    /// TileEntityMachineRadiolysis: FluidTank.setType(10, 11, slots) ///
    private static void radiolysisBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        LegacyMachineBlockEntity r = (LegacyMachineBlockEntity) place(level, HbmBlocks.MACHINE_RADIOLYSIS.get());
        r.setItem(10, FluidIdentifierItem.forFluid(fluid("water")));
        LegacyMachineBlockEntity.tick(level, r.getBlockPos(), r.getBlockState(), r);
        check(r.getItem(10).isEmpty() && r.getItem(11).getItem() instanceof FluidIdentifierItem && r.tank(0).type() == fluid("water")
                && r.tank(1).type() == fluid("peroxide") && r.tank(2).type() == fluid("hydrogen"), problems, cases,
                "radiolysis identifier: slot10 " + r.getItem(10) + " slot11 " + r.getItem(11) + " tanks " + r.tank(0).type().name() + "/"
                        + r.tank(1).type().name() + "/" + r.tank(2).type().name());
    }

    /// TileEntityMachineChemicalFactory + ModuleMachineBase: speed * 2, pow * 2, fractional progress, 100 mB coolant ///
    private static void chemicalFactoryBehaviour(ServerLevel level, List<String> problems, int[] cases) {
        ChemicalFactoryBlockEntity f = (ChemicalFactoryBlockEntity) place(level, HbmBlocks.MACHINE_CHEMICAL_FACTORY.get());
        f.setSelectedRecipe(0, AtomicLegacy.id("chemical_plant/co2"));
        ChemicalFactoryBlockEntity.tick(level, f.getBlockPos(), f.getBlockState(), f);
        double progress = 0.0D;
        int expected = 0;
        while (progress < 1.0D) {
            progress += Math.min(2.0D / 60.0D, 1.0D);
            expected++;
        }
        int ticks = 0;
        long used = -1;
        while (ticks < 100 && f.outputTank(0).amount() == 0) {
            f.inputTank(0).fill(fluid("gas"), 24_000, false);
            f.waterTank().fill(fluid("water"), 4_000, false);
            f.spentSteamTank().setAmount(0);
            f.applyPower(0L, 1_000_000L);
            long before = stored(f);
            ChemicalFactoryBlockEntity.tick(level, f.getBlockPos(), f.getBlockState(), f);
            if (ticks == 0) used = before - stored(f);
            ticks++;
        }
        check(ticks == expected, problems, cases, "chemical factory co2 cycle " + ticks + " ticks, expected " + expected);
        check(used == 200L, problems, cases, "chemical factory used " + used + " HE/t, expected (long) (100 * 2.0)");
        check(f.outputTank(0).amount() == 1_000 && f.outputTank(0).type() == fluid("carbondioxide"), problems, cases,
                "chemical factory output " + f.outputTank(0).type().name() + ":" + f.outputTank(0).amount());
    }

    private static long stored(ChemicalFactoryBlockEntity f) {
        return f.energyStored();
    }

    /// helpers ///
    private static BlockEntity place(ServerLevel level, Block block) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 6) * SPACING, BASE_Y, BASE_Z + (index / 6) * SPACING);
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced(cx + dx, cz + dz, true);
                level.getChunk(cx + dx, cz + dz);
                FORCED.add(new int[] {cx + dx, cz + dz});
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); // drop the block entity of a previous run
        level.setBlock(pos, block.defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) throw new IllegalStateException(BuiltInRegistries.BLOCK.getKey(block) + " has no block entity");
        return be;
    }

    private static ItemStack stack(String path, int count) {
        Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(path));
        if (item == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("missing item " + path);
        return new ItemStack(item, count);
    }

    private static ItemStack waste(String path, int wasteClass) {
        ItemStack stack = stack(path, 1);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("waste_class", wasteClass);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static ItemStack firstOfTag(String tag) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
            if (BuiltInRegistries.ITEM.getKey(holder.value()).getNamespace().equals(AtomicLegacy.MOD_ID)) return new ItemStack(holder);
        }
        throw new IllegalStateException("empty tag " + tag);
    }

    private static String path(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static int meta(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("waste_class")) return data.copyTag().getInt("waste_class");
        var model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return model == null ? 0 : model.value();
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow(() -> new IllegalStateException("missing fluid " + name));
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) {
            problems.add("Q2: " + message);
        }
    }

    /** path|fluidProduced|fluidConsumed|wavelength|item:meta x count @ weight (SILEXRecipes.register) */
    static final String[] SILEX = {
        "silex/crystal_schraranium|900|100|uv|nugget_schrabidium:0x1@5,nugget_uranium:0x1@2,nugget_neptunium:0x1@2",
        "silex/crystal_trixite|1200|100|uv|powder_plutonium:0x1@2,powder_cobalt:0x1@3,powder_niobium:0x1@3,powder_nitan_mix:0x1@1,powder_spark_mix:0x1@1",
        "silex/death|1000|1000|gamma|powder_impure_osmiridium:0x1@1",
        "silex/fallout|900|100|visible|dust_tiny:0x1@90,nugget_co60:0x1@2,powder_sr90_tiny:0x1@3,powder_i131_tiny:0x1@1,powder_cs137_tiny:0x1@3,nugget_au198:0x1@1",
        "silex/fullerene|1000|1000|visible|powder_ash_fullerene:0x1@1",
        "silex/gravel|1000|250|visible|flint:0x1@80,powder_boron:0x1@5,powder_lithium:0x1@10,fluorite:0x1@5",
        "silex/ingot_am_mix|900|100|visible|nugget_am241:0x1@3,nugget_am242:0x1@6",
        "silex/ingot_australium|900|100|visible|nugget_australium_lesser:0x1@5,nugget_australium_greater:0x1@1",
        "silex/ingot_plutonium|900|100|visible|nugget_pu238:0x1@3,nugget_pu239:0x1@4,nugget_pu240:0x1@2",
        "silex/ingot_pu_mix|900|100|visible|nugget_pu239:0x1@6,nugget_pu240:0x1@3",
        "silex/ingot_schraranium|900|100|visible|nugget_schrabidium:0x1@4,nugget_uranium:0x1@3,nugget_neptunium:0x1@2",
        "silex/ingot_uranium|900|100|visible|nugget_u235:0x1@1,nugget_u238:0x1@11",
        "silex/lapis|100|100|ir|sulfur:0x1@4,powder_aluminium:0x1@3,powder_cobalt:0x1@3",
        "silex/nuclear_waste_long_depleted_neptunium|900|100|ir|nugget_u238:0x1@16,nugget_lead:0x1@55,dust_tiny:0x1@20,nuclear_waste_tiny:0x1@9",
        "silex/nuclear_waste_long_depleted_schrabidium|900|100|ir|nugget_solinium:0x1@20,nugget_euphemium:0x1@18,nugget_gh336:0x1@15,nugget_tantalium:0x1@8,powder_neodymium_tiny:0x1@8,nuclear_waste_tiny:0x1@31",
        "silex/nuclear_waste_long_depleted_thorium|900|100|ir|nugget_lead:0x1@35,nugget_bismuth:0x1@40,dust_tiny:0x1@15,nuclear_waste_tiny:0x1@10",
        "silex/nuclear_waste_long_depleted_uranium233|900|100|ir|nugget_lead:0x1@60,nugget_bismuth:0x1@25,dust_tiny:0x1@15",
        "silex/nuclear_waste_long_depleted_uranium235|900|100|ir|nugget_lead:0x1@65,nugget_bismuth:0x1@20,dust_tiny:0x1@15",
        "silex/nuclear_waste_long_neptunium|900|100|ir|nugget_u238:0x1@15,nugget_pu239:0x1@40,nugget_pu240:0x1@15,nugget_technetium:0x1@15,nuclear_waste_tiny:0x1@15",
        "silex/nuclear_waste_long_schrabidium|900|100|ir|nugget_solinium:0x1@25,nugget_euphemium:0x1@18,nugget_gh336:0x1@16,nugget_tantalium:0x1@8,powder_neodymium_tiny:0x1@8,nuclear_waste_tiny:0x1@25",
        "silex/nuclear_waste_long_thorium|900|100|ir|nugget_u233:0x1@40,nugget_u235:0x1@35,nuclear_waste_tiny:0x1@25",
        "silex/nuclear_waste_long_uranium233|900|100|ir|nugget_u235:0x1@15,nugget_neptunium:0x1@25,nugget_pu239:0x1@45,nugget_technetium:0x1@15",
        "silex/nuclear_waste_long_uranium235|900|100|ir|nugget_neptunium:0x1@20,nugget_pu239:0x1@45,nugget_pu240:0x1@20,nugget_technetium:0x1@15",
        "silex/nuclear_waste_short_depleted_neptunium|900|100|ir|nugget_zirconium:0x1@7,dust_tiny:0x1@29,nugget_u238:0x1@2,nugget_lead:0x1@45,nuclear_waste_tiny:0x1@17",
        "silex/nuclear_waste_short_depleted_plutonium239|900|100|ir|nugget_zirconium:0x1@2,dust_tiny:0x1@16,nugget_lead:0x1@40,nugget_u238:0x1@3,nuclear_waste_tiny:0x1@39",
        "silex/nuclear_waste_short_depleted_plutonium240|900|100|ir|nugget_zirconium:0x1@2,dust_tiny:0x1@22,nugget_bismuth:0x1@20,nugget_lead:0x1@17,nugget_u238:0x1@3,nuclear_waste_tiny:0x1@36",
        "silex/nuclear_waste_short_depleted_plutonium241|900|100|visible|nugget_bismuth:0x1@60,dust_tiny:0x1@20,nugget_lead:0x1@15,nuclear_waste_tiny:0x1@5",
        "silex/nuclear_waste_short_depleted_schrabidium|900|100|ir|nugget_bismuth:0x1@7,nugget_mercury:0x1@12,powder_cerium_tiny:0x1@14,powder_lanthanium_tiny:0x1@15,dust_tiny:0x1@20,nuclear_waste_tiny:0x1@32",
        "silex/nuclear_waste_short_depleted_uranium233|900|100|ir|nugget_zirconium:0x1@12,dust_tiny:0x1@34,nugget_lead:0x1@13,nugget_u238:0x1@2,nugget_bismuth:0x1@10,nuclear_waste_tiny:0x1@29",
        "silex/nuclear_waste_short_depleted_uranium235|900|100|ir|nugget_zirconium:0x1@10,dust_tiny:0x1@32,nugget_lead:0x1@22,nugget_u238:0x1@5,nugget_bismuth:0x1@15,nuclear_waste_tiny:0x1@16",
        "silex/nuclear_waste_short_neptunium|900|100|ir|nugget_pu238:0x1@40,powder_sr90_tiny:0x1@7,powder_i131_tiny:0x1@5,powder_cs137_tiny:0x1@8,nuclear_waste_tiny:0x1@40",
        "silex/nuclear_waste_short_plutonium239|900|100|ir|nugget_pu240:0x1@10,nugget_pu241:0x1@25,powder_sr90_tiny:0x1@2,powder_i131_tiny:0x1@5,powder_cs137_tiny:0x1@6,nuclear_waste_tiny:0x1@52",
        "silex/nuclear_waste_short_plutonium240|900|100|ir|nugget_pu241:0x1@15,nugget_neptunium:0x1@5,powder_sr90_tiny:0x1@2,powder_i131_tiny:0x1@5,powder_cs137_tiny:0x1@7,nuclear_waste_tiny:0x1@66",
        "silex/nuclear_waste_short_plutonium241|900|100|visible|nugget_am241:0x1@25,nugget_am242:0x1@35,nugget_technetium:0x1@5,powder_i131_tiny:0x1@3,powder_cs137_tiny:0x1@7,nuclear_waste_tiny:0x1@25",
        "silex/nuclear_waste_short_schrabidium|900|100|ir|nugget_pb209:0x1@7,nugget_au198:0x1@7,powder_cs137_tiny:0x1@5,powder_i131_tiny:0x1@5,nuclear_waste_tiny:0x1@76",
        "silex/nuclear_waste_short_uranium233|900|100|ir|nugget_pu238:0x1@4,powder_sr90_tiny:0x1@12,powder_i131_tiny:0x1@10,powder_cs137_tiny:0x1@14,nuclear_waste_tiny:0x1@60",
        "silex/nuclear_waste_short_uranium235|900|100|ir|nugget_pu238:0x1@12,powder_sr90_tiny:0x1@10,powder_i131_tiny:0x1@10,powder_cs137_tiny:0x1@12,nuclear_waste_tiny:0x1@56",
        "silex/ore_tikite|900|100|uv|powder_plutonium:0x1@2,powder_cobalt:0x1@3,powder_niobium:0x1@3,powder_nitan_mix:0x1@2",
        "silex/puf6|900|100|visible|nugget_pu238:0x1@3,nugget_pu239:0x1@4,nugget_pu240:0x1@2",
        "silex/rbmk_pellet_balefire_0|400|100|uv|powder_balefire:0x1@90,nuclear_waste_tiny:0x1@10",
        "silex/rbmk_pellet_balefire_1|400|100|uv|powder_balefire:0x1@70,nuclear_waste_tiny:0x1@30",
        "silex/rbmk_pellet_balefire_2|400|100|uv|powder_balefire:0x1@50,nuclear_waste_tiny:0x1@50",
        "silex/rbmk_pellet_balefire_3|400|100|uv|powder_balefire:0x1@30,nuclear_waste_tiny:0x1@70",
        "silex/rbmk_pellet_balefire_4|400|100|uv|powder_balefire:0x1@10,nuclear_waste_tiny:0x1@90",
        "silex/rbmk_pellet_balefire_gold_0|600|100|visible|nugget_au198:0x1@90,powder_balefire:0x1@10",
        "silex/rbmk_pellet_balefire_gold_1|600|100|visible|nugget_au198:0x1@70,powder_balefire:0x1@30",
        "silex/rbmk_pellet_balefire_gold_2|600|100|visible|nugget_au198:0x1@50,powder_balefire:0x1@50",
        "silex/rbmk_pellet_balefire_gold_3|600|100|visible|nugget_au198:0x1@30,powder_balefire:0x1@70",
        "silex/rbmk_pellet_balefire_gold_4|600|100|visible|nugget_au198:0x1@10,powder_balefire:0x1@90",
        "silex/rbmk_pellet_drx_0|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_1|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_2|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_3|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_4|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_5|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_6|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_7|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_8|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_drx_9|600|100|gamma|undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1,undefined:0x1@1",
        "silex/rbmk_pellet_flashlead_0|600|100|visible|nugget_au198:0x1@44,nugget_pb209:0x1@44,nugget_bismuth:0x1@1,nugget_mercury:0x1@1,nugget_gh336:0x1@10",
        "silex/rbmk_pellet_flashlead_1|600|100|visible|nugget_au198:0x1@34,nugget_pb209:0x1@34,nugget_bismuth:0x1@7,nugget_mercury:0x1@7,nugget_gh336:0x1@18",
        "silex/rbmk_pellet_flashlead_2|600|100|visible|nugget_au198:0x1@24,nugget_pb209:0x1@24,nugget_bismuth:0x1@13,nugget_mercury:0x1@13,nugget_gh336:0x1@26",
        "silex/rbmk_pellet_flashlead_3|600|100|visible|nugget_au198:0x1@14,nugget_pb209:0x1@14,nugget_bismuth:0x1@19,nugget_mercury:0x1@19,nugget_gh336:0x1@34",
        "silex/rbmk_pellet_flashlead_4|600|100|visible|nugget_au198:0x1@4,nugget_pb209:0x1@4,nugget_bismuth:0x1@25,nugget_mercury:0x1@25,nugget_gh336:0x1@42",
        "silex/rbmk_pellet_heaus_0|600|100|visible|nugget_australium_greater:0x1@90,nugget_au198:0x1@5,gold_nugget:0x1@3,nugget_pb209:0x1@2",
        "silex/rbmk_pellet_heaus_1|600|100|visible|nugget_australium_greater:0x1@70,nugget_au198:0x1@15,gold_nugget:0x1@9,nugget_pb209:0x1@6",
        "silex/rbmk_pellet_heaus_2|600|100|visible|nugget_australium_greater:0x1@50,nugget_au198:0x1@25,gold_nugget:0x1@15,nugget_pb209:0x1@10",
        "silex/rbmk_pellet_heaus_3|600|100|visible|nugget_australium_greater:0x1@30,nugget_au198:0x1@35,gold_nugget:0x1@21,nugget_pb209:0x1@14",
        "silex/rbmk_pellet_heaus_4|600|100|visible|nugget_australium_greater:0x1@10,nugget_au198:0x1@45,gold_nugget:0x1@27,nugget_pb209:0x1@18",
        "silex/rbmk_pellet_heaus_5|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_greater:0x1@89,nugget_au198:0x1@5,gold_nugget:0x1@3,nugget_pb209:0x1@2",
        "silex/rbmk_pellet_heaus_6|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_greater:0x1@69,nugget_au198:0x1@15,gold_nugget:0x1@9,nugget_pb209:0x1@6",
        "silex/rbmk_pellet_heaus_7|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_greater:0x1@49,nugget_au198:0x1@25,gold_nugget:0x1@15,nugget_pb209:0x1@10",
        "silex/rbmk_pellet_heaus_8|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_greater:0x1@29,nugget_au198:0x1@35,gold_nugget:0x1@21,nugget_pb209:0x1@14",
        "silex/rbmk_pellet_heaus_9|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_greater:0x1@9,nugget_au198:0x1@45,gold_nugget:0x1@27,nugget_pb209:0x1@18",
        "silex/rbmk_pellet_hen_0|600|100|ir|nugget_neptunium:0x1@90,nuclear_waste_long_tiny:2x1@4,nuclear_waste_short_tiny:2x1@6",
        "silex/rbmk_pellet_hen_1|600|100|ir|nugget_neptunium:0x1@70,nuclear_waste_long_tiny:2x1@12,nuclear_waste_short_tiny:2x1@18",
        "silex/rbmk_pellet_hen_2|600|100|ir|nugget_neptunium:0x1@50,nuclear_waste_long_tiny:2x1@20,nuclear_waste_short_tiny:2x1@30",
        "silex/rbmk_pellet_hen_3|600|100|ir|nugget_neptunium:0x1@30,nuclear_waste_long_tiny:2x1@28,nuclear_waste_short_tiny:2x1@42",
        "silex/rbmk_pellet_hen_4|600|100|ir|nugget_neptunium:0x1@10,nuclear_waste_long_tiny:2x1@36,nuclear_waste_short_tiny:2x1@54",
        "silex/rbmk_pellet_hen_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium:0x1@89,nuclear_waste_long_tiny:2x1@4,nuclear_waste_short_tiny:2x1@6",
        "silex/rbmk_pellet_hen_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium:0x1@69,nuclear_waste_long_tiny:2x1@12,nuclear_waste_short_tiny:2x1@18",
        "silex/rbmk_pellet_hen_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium:0x1@49,nuclear_waste_long_tiny:2x1@20,nuclear_waste_short_tiny:2x1@30",
        "silex/rbmk_pellet_hen_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium:0x1@29,nuclear_waste_long_tiny:2x1@28,nuclear_waste_short_tiny:2x1@42",
        "silex/rbmk_pellet_hen_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium:0x1@9,nuclear_waste_long_tiny:2x1@36,nuclear_waste_short_tiny:2x1@54",
        "silex/rbmk_pellet_hep239_0|600|100|ir|nugget_pu239:0x1@85,nuclear_waste_short_tiny:3x1@15",
        "silex/rbmk_pellet_hep239_1|600|100|ir|nugget_pu239:0x1@65,nuclear_waste_short_tiny:3x1@35",
        "silex/rbmk_pellet_hep239_2|600|100|ir|nugget_pu239:0x1@45,nuclear_waste_short_tiny:3x1@55",
        "silex/rbmk_pellet_hep239_3|600|100|ir|nugget_pu239:0x1@25,nuclear_waste_short_tiny:3x1@75",
        "silex/rbmk_pellet_hep239_4|600|100|ir|nugget_pu239:0x1@5,nuclear_waste_short_tiny:3x1@95",
        "silex/rbmk_pellet_hep239_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu239:0x1@84,nuclear_waste_short_tiny:3x1@15",
        "silex/rbmk_pellet_hep239_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu239:0x1@64,nuclear_waste_short_tiny:3x1@35",
        "silex/rbmk_pellet_hep239_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu239:0x1@44,nuclear_waste_short_tiny:3x1@55",
        "silex/rbmk_pellet_hep239_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu239:0x1@24,nuclear_waste_short_tiny:3x1@75",
        "silex/rbmk_pellet_hep239_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu239:0x1@4,nuclear_waste_short_tiny:3x1@95",
        "silex/rbmk_pellet_hep241_0|600|100|visible|nugget_pu241:0x1@85,nuclear_waste_short_tiny:5x1@15",
        "silex/rbmk_pellet_hep241_1|600|100|visible|nugget_pu241:0x1@65,nuclear_waste_short_tiny:5x1@35",
        "silex/rbmk_pellet_hep241_2|600|100|visible|nugget_pu241:0x1@45,nuclear_waste_short_tiny:5x1@55",
        "silex/rbmk_pellet_hep241_3|600|100|visible|nugget_pu241:0x1@25,nuclear_waste_short_tiny:5x1@75",
        "silex/rbmk_pellet_hep241_4|600|100|visible|nugget_pu241:0x1@5,nuclear_waste_short_tiny:5x1@95",
        "silex/rbmk_pellet_hep241_5|600|100|visible|powder_xe135_tiny:0x1@1,nugget_pu241:0x1@84,nuclear_waste_short_tiny:5x1@15",
        "silex/rbmk_pellet_hep241_6|600|100|visible|powder_xe135_tiny:0x1@1,nugget_pu241:0x1@64,nuclear_waste_short_tiny:5x1@35",
        "silex/rbmk_pellet_hep241_7|600|100|visible|powder_xe135_tiny:0x1@1,nugget_pu241:0x1@44,nuclear_waste_short_tiny:5x1@55",
        "silex/rbmk_pellet_hep241_8|600|100|visible|powder_xe135_tiny:0x1@1,nugget_pu241:0x1@24,nuclear_waste_short_tiny:5x1@75",
        "silex/rbmk_pellet_hep241_9|600|100|visible|powder_xe135_tiny:0x1@1,nugget_pu241:0x1@4,nuclear_waste_short_tiny:5x1@95",
        "silex/rbmk_pellet_hes_0|600|100|visible|nugget_hes:0x1@90,nuclear_waste_long_tiny:2x1@1,nuclear_waste_short_tiny:2x1@1,nuclear_waste_long_tiny:4x1@2,nuclear_waste_short_tiny:7x1@4,powder_coal_tiny:0x1@2",
        "silex/rbmk_pellet_hes_1|600|100|visible|nugget_hes:0x1@70,nuclear_waste_long_tiny:2x1@3,nuclear_waste_short_tiny:2x1@4,nuclear_waste_long_tiny:4x1@7,nuclear_waste_short_tiny:7x1@10,powder_coal_tiny:0x1@6",
        "silex/rbmk_pellet_hes_2|600|100|visible|nugget_hes:0x1@50,nuclear_waste_long_tiny:2x1@5,nuclear_waste_short_tiny:2x1@7,nuclear_waste_long_tiny:4x1@12,nuclear_waste_short_tiny:7x1@16,powder_coal_tiny:0x1@10",
        "silex/rbmk_pellet_hes_3|600|100|visible|nugget_hes:0x1@30,nuclear_waste_long_tiny:2x1@7,nuclear_waste_short_tiny:2x1@10,nuclear_waste_long_tiny:4x1@17,nuclear_waste_short_tiny:7x1@22,powder_coal_tiny:0x1@14",
        "silex/rbmk_pellet_hes_4|600|100|visible|nugget_hes:0x1@10,nuclear_waste_long_tiny:2x1@9,nuclear_waste_short_tiny:2x1@13,nuclear_waste_long_tiny:4x1@22,nuclear_waste_short_tiny:7x1@28,powder_coal_tiny:0x1@18",
        "silex/rbmk_pellet_hes_5|600|100|visible|nugget_hes:0x1@90,nuclear_waste_long_tiny:2x1@1,nuclear_waste_short_tiny:2x1@1,nuclear_waste_long_tiny:4x1@2,nuclear_waste_short_tiny:7x1@4,powder_coal_tiny:0x1@2",
        "silex/rbmk_pellet_hes_6|600|100|visible|nugget_hes:0x1@70,nuclear_waste_long_tiny:2x1@3,nuclear_waste_short_tiny:2x1@4,nuclear_waste_long_tiny:4x1@7,nuclear_waste_short_tiny:7x1@10,powder_coal_tiny:0x1@6",
        "silex/rbmk_pellet_hes_7|600|100|visible|nugget_hes:0x1@50,nuclear_waste_long_tiny:2x1@5,nuclear_waste_short_tiny:2x1@7,nuclear_waste_long_tiny:4x1@12,nuclear_waste_short_tiny:7x1@16,powder_coal_tiny:0x1@10",
        "silex/rbmk_pellet_hes_8|600|100|visible|nugget_hes:0x1@30,nuclear_waste_long_tiny:2x1@7,nuclear_waste_short_tiny:2x1@10,nuclear_waste_long_tiny:4x1@17,nuclear_waste_short_tiny:7x1@22,powder_coal_tiny:0x1@14",
        "silex/rbmk_pellet_hes_9|600|100|visible|nugget_hes:0x1@10,nuclear_waste_long_tiny:2x1@9,nuclear_waste_short_tiny:2x1@13,nuclear_waste_long_tiny:4x1@22,nuclear_waste_short_tiny:7x1@28,powder_coal_tiny:0x1@18",
        "silex/rbmk_pellet_heu233_0|600|100|ir|nugget_u233:0x1@90,nuclear_waste_long_tiny:1x1@4,nuclear_waste_short_tiny:1x1@6",
        "silex/rbmk_pellet_heu233_1|600|100|ir|nugget_u233:0x1@70,nuclear_waste_long_tiny:1x1@12,nuclear_waste_short_tiny:1x1@18",
        "silex/rbmk_pellet_heu233_2|600|100|ir|nugget_u233:0x1@50,nuclear_waste_long_tiny:1x1@20,nuclear_waste_short_tiny:1x1@30",
        "silex/rbmk_pellet_heu233_3|600|100|ir|nugget_u233:0x1@30,nuclear_waste_long_tiny:1x1@28,nuclear_waste_short_tiny:1x1@42",
        "silex/rbmk_pellet_heu233_4|600|100|ir|nugget_u233:0x1@10,nuclear_waste_long_tiny:1x1@36,nuclear_waste_short_tiny:1x1@54",
        "silex/rbmk_pellet_heu233_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u233:0x1@89,nuclear_waste_long_tiny:1x1@4,nuclear_waste_short_tiny:1x1@6",
        "silex/rbmk_pellet_heu233_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u233:0x1@69,nuclear_waste_long_tiny:1x1@12,nuclear_waste_short_tiny:1x1@18",
        "silex/rbmk_pellet_heu233_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u233:0x1@49,nuclear_waste_long_tiny:1x1@20,nuclear_waste_short_tiny:1x1@30",
        "silex/rbmk_pellet_heu233_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u233:0x1@29,nuclear_waste_long_tiny:1x1@28,nuclear_waste_short_tiny:1x1@42",
        "silex/rbmk_pellet_heu233_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u233:0x1@9,nuclear_waste_long_tiny:1x1@36,nuclear_waste_short_tiny:1x1@54",
        "silex/rbmk_pellet_heu235_0|600|100|ir|nugget_u235:0x1@90,nuclear_waste_long_tiny:0x1@4,nuclear_waste_short_tiny:0x1@6",
        "silex/rbmk_pellet_heu235_1|600|100|ir|nugget_u235:0x1@70,nuclear_waste_long_tiny:0x1@12,nuclear_waste_short_tiny:0x1@18",
        "silex/rbmk_pellet_heu235_2|600|100|ir|nugget_u235:0x1@50,nuclear_waste_long_tiny:0x1@20,nuclear_waste_short_tiny:0x1@30",
        "silex/rbmk_pellet_heu235_3|600|100|ir|nugget_u235:0x1@30,nuclear_waste_long_tiny:0x1@28,nuclear_waste_short_tiny:0x1@42",
        "silex/rbmk_pellet_heu235_4|600|100|ir|nugget_u235:0x1@10,nuclear_waste_long_tiny:0x1@36,nuclear_waste_short_tiny:0x1@54",
        "silex/rbmk_pellet_heu235_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u235:0x1@89,nuclear_waste_long_tiny:0x1@4,nuclear_waste_short_tiny:0x1@6",
        "silex/rbmk_pellet_heu235_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u235:0x1@69,nuclear_waste_long_tiny:0x1@12,nuclear_waste_short_tiny:0x1@18",
        "silex/rbmk_pellet_heu235_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u235:0x1@49,nuclear_waste_long_tiny:0x1@20,nuclear_waste_short_tiny:0x1@30",
        "silex/rbmk_pellet_heu235_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u235:0x1@29,nuclear_waste_long_tiny:0x1@28,nuclear_waste_short_tiny:0x1@42",
        "silex/rbmk_pellet_heu235_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_u235:0x1@9,nuclear_waste_long_tiny:0x1@36,nuclear_waste_short_tiny:0x1@54",
        "silex/rbmk_pellet_leaus_0|600|100|visible|nugget_australium_lesser:0x1@90,nugget_lead:0x1@6,nugget_pb209:0x1@4",
        "silex/rbmk_pellet_leaus_1|600|100|visible|nugget_australium_lesser:0x1@70,nugget_lead:0x1@18,nugget_pb209:0x1@12",
        "silex/rbmk_pellet_leaus_2|600|100|visible|nugget_australium_lesser:0x1@50,nugget_lead:0x1@30,nugget_pb209:0x1@20",
        "silex/rbmk_pellet_leaus_3|600|100|visible|nugget_australium_lesser:0x1@30,nugget_lead:0x1@42,nugget_pb209:0x1@28",
        "silex/rbmk_pellet_leaus_4|600|100|visible|nugget_australium_lesser:0x1@10,nugget_lead:0x1@54,nugget_pb209:0x1@36",
        "silex/rbmk_pellet_leaus_5|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_lesser:0x1@89,nugget_lead:0x1@6,nugget_pb209:0x1@4",
        "silex/rbmk_pellet_leaus_6|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_lesser:0x1@69,nugget_lead:0x1@18,nugget_pb209:0x1@12",
        "silex/rbmk_pellet_leaus_7|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_lesser:0x1@49,nugget_lead:0x1@30,nugget_pb209:0x1@20",
        "silex/rbmk_pellet_leaus_8|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_lesser:0x1@29,nugget_lead:0x1@42,nugget_pb209:0x1@28",
        "silex/rbmk_pellet_leaus_9|600|100|visible|powder_xe135_tiny:0x1@1,nugget_australium_lesser:0x1@9,nugget_lead:0x1@54,nugget_pb209:0x1@36",
        "silex/rbmk_pellet_lep_0|600|100|ir|nugget_plutonium_fuel:0x1@84,nugget_pu239:0x1@6,nuclear_waste_short_tiny:3x1@7,nuclear_waste_short_tiny:4x1@3",
        "silex/rbmk_pellet_lep_1|600|100|ir|nugget_plutonium_fuel:0x1@70,nugget_pu_mix:0x1@8,nuclear_waste_short_tiny:3x1@15,nuclear_waste_short_tiny:4x1@7",
        "silex/rbmk_pellet_lep_2|600|100|ir|nugget_plutonium_fuel:0x1@56,nugget_pu_mix:0x1@10,nuclear_waste_short_tiny:3x1@23,nuclear_waste_short_tiny:4x1@11",
        "silex/rbmk_pellet_lep_3|600|100|ir|nugget_plutonium_fuel:0x1@42,nugget_pu_mix:0x1@12,nuclear_waste_short_tiny:3x1@31,nuclear_waste_short_tiny:4x1@15",
        "silex/rbmk_pellet_lep_4|600|100|ir|nugget_plutonium_fuel:0x1@28,nugget_pu_mix:0x1@14,nuclear_waste_short_tiny:3x1@39,nuclear_waste_short_tiny:4x1@19",
        "silex/rbmk_pellet_lep_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_plutonium_fuel:0x1@83,nugget_pu239:0x1@6,nuclear_waste_short_tiny:3x1@7,nuclear_waste_short_tiny:4x1@3",
        "silex/rbmk_pellet_lep_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_plutonium_fuel:0x1@69,nugget_pu_mix:0x1@8,nuclear_waste_short_tiny:3x1@15,nuclear_waste_short_tiny:4x1@7",
        "silex/rbmk_pellet_lep_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_plutonium_fuel:0x1@55,nugget_pu_mix:0x1@10,nuclear_waste_short_tiny:3x1@23,nuclear_waste_short_tiny:4x1@11",
        "silex/rbmk_pellet_lep_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_plutonium_fuel:0x1@41,nugget_pu_mix:0x1@12,nuclear_waste_short_tiny:3x1@31,nuclear_waste_short_tiny:4x1@15",
        "silex/rbmk_pellet_lep_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_plutonium_fuel:0x1@27,nugget_pu_mix:0x1@14,nuclear_waste_short_tiny:3x1@39,nuclear_waste_short_tiny:4x1@19",
        "silex/rbmk_pellet_les_0|600|100|visible|nugget_les:0x1@90,nuclear_waste_long_tiny:2x1@2,nuclear_waste_short_tiny:2x1@2,nuclear_waste_long_tiny:4x1@1,nuclear_waste_short_tiny:7x1@1,powder_coal_tiny:0x1@4",
        "silex/rbmk_pellet_les_1|600|100|visible|nugget_les:0x1@70,nuclear_waste_long_tiny:2x1@5,nuclear_waste_short_tiny:2x1@7,nuclear_waste_long_tiny:4x1@3,nuclear_waste_short_tiny:7x1@3,powder_coal_tiny:0x1@12",
        "silex/rbmk_pellet_les_2|600|100|visible|nugget_les:0x1@50,nuclear_waste_long_tiny:2x1@8,nuclear_waste_short_tiny:2x1@12,nuclear_waste_long_tiny:4x1@5,nuclear_waste_short_tiny:7x1@5,powder_coal_tiny:0x1@20",
        "silex/rbmk_pellet_les_3|600|100|visible|nugget_les:0x1@30,nuclear_waste_long_tiny:2x1@11,nuclear_waste_short_tiny:2x1@17,nuclear_waste_long_tiny:4x1@7,nuclear_waste_short_tiny:7x1@7,powder_coal_tiny:0x1@28",
        "silex/rbmk_pellet_les_4|600|100|visible|nugget_les:0x1@10,nuclear_waste_long_tiny:2x1@14,nuclear_waste_short_tiny:2x1@22,nuclear_waste_long_tiny:4x1@9,nuclear_waste_short_tiny:7x1@9,powder_coal_tiny:0x1@36",
        "silex/rbmk_pellet_les_5|600|100|visible|nugget_les:0x1@90,nuclear_waste_long_tiny:2x1@2,nuclear_waste_short_tiny:2x1@2,nuclear_waste_long_tiny:4x1@1,nuclear_waste_short_tiny:7x1@1,powder_coal_tiny:0x1@4",
        "silex/rbmk_pellet_les_6|600|100|visible|nugget_les:0x1@70,nuclear_waste_long_tiny:2x1@5,nuclear_waste_short_tiny:2x1@7,nuclear_waste_long_tiny:4x1@3,nuclear_waste_short_tiny:7x1@3,powder_coal_tiny:0x1@12",
        "silex/rbmk_pellet_les_7|600|100|visible|nugget_les:0x1@50,nuclear_waste_long_tiny:2x1@8,nuclear_waste_short_tiny:2x1@12,nuclear_waste_long_tiny:4x1@5,nuclear_waste_short_tiny:7x1@5,powder_coal_tiny:0x1@20",
        "silex/rbmk_pellet_les_8|600|100|visible|nugget_les:0x1@30,nuclear_waste_long_tiny:2x1@11,nuclear_waste_short_tiny:2x1@17,nuclear_waste_long_tiny:4x1@7,nuclear_waste_short_tiny:7x1@7,powder_coal_tiny:0x1@28",
        "silex/rbmk_pellet_les_9|600|100|visible|nugget_les:0x1@10,nuclear_waste_long_tiny:2x1@14,nuclear_waste_short_tiny:2x1@22,nuclear_waste_long_tiny:4x1@9,nuclear_waste_short_tiny:7x1@9,powder_coal_tiny:0x1@36",
        "silex/rbmk_pellet_men_0|600|100|ir|nugget_neptunium_fuel:0x1@84,nugget_pu239:0x1@6,nuclear_waste_long_tiny:2x1@4,nuclear_waste_short_tiny:2x1@6",
        "silex/rbmk_pellet_men_1|600|100|ir|nugget_neptunium_fuel:0x1@70,nugget_pu_mix:0x1@8,nuclear_waste_long_tiny:2x1@9,nuclear_waste_short_tiny:2x1@13",
        "silex/rbmk_pellet_men_2|600|100|ir|nugget_neptunium_fuel:0x1@56,nugget_pu_mix:0x1@10,nuclear_waste_long_tiny:2x1@14,nuclear_waste_short_tiny:2x1@20",
        "silex/rbmk_pellet_men_3|600|100|ir|nugget_neptunium_fuel:0x1@42,nugget_pu_mix:0x1@12,nuclear_waste_long_tiny:2x1@19,nuclear_waste_short_tiny:2x1@27",
        "silex/rbmk_pellet_men_4|600|100|ir|nugget_neptunium_fuel:0x1@28,nugget_pu_mix:0x1@14,nuclear_waste_long_tiny:2x1@24,nuclear_waste_short_tiny:2x1@34",
        "silex/rbmk_pellet_men_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium_fuel:0x1@83,nugget_pu239:0x1@6,nuclear_waste_long_tiny:2x1@4,nuclear_waste_short_tiny:2x1@6",
        "silex/rbmk_pellet_men_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium_fuel:0x1@69,nugget_pu_mix:0x1@8,nuclear_waste_long_tiny:2x1@9,nuclear_waste_short_tiny:2x1@13",
        "silex/rbmk_pellet_men_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium_fuel:0x1@55,nugget_pu_mix:0x1@10,nuclear_waste_long_tiny:2x1@14,nuclear_waste_short_tiny:2x1@20",
        "silex/rbmk_pellet_men_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium_fuel:0x1@41,nugget_pu_mix:0x1@12,nuclear_waste_long_tiny:2x1@19,nuclear_waste_short_tiny:2x1@27",
        "silex/rbmk_pellet_men_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_neptunium_fuel:0x1@27,nugget_pu_mix:0x1@14,nuclear_waste_long_tiny:2x1@24,nuclear_waste_short_tiny:2x1@34",
        "silex/rbmk_pellet_mep_0|600|100|ir|nugget_pu_mix:0x1@85,nuclear_waste_short_tiny:3x1@10,nuclear_waste_short_tiny:4x1@5",
        "silex/rbmk_pellet_mep_1|600|100|ir|nugget_pu_mix:0x1@65,nuclear_waste_short_tiny:3x1@20,nuclear_waste_short_tiny:4x1@10",
        "silex/rbmk_pellet_mep_2|600|100|ir|nugget_pu_mix:0x1@45,nuclear_waste_short_tiny:3x1@30,nuclear_waste_short_tiny:4x1@15",
        "silex/rbmk_pellet_mep_3|600|100|ir|nugget_pu_mix:0x1@25,nuclear_waste_short_tiny:3x1@40,nuclear_waste_short_tiny:4x1@20",
        "silex/rbmk_pellet_mep_4|600|100|ir|nugget_pu_mix:0x1@5,nuclear_waste_short_tiny:3x1@50,nuclear_waste_short_tiny:4x1@25",
        "silex/rbmk_pellet_mep_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu_mix:0x1@84,nuclear_waste_short_tiny:3x1@10,nuclear_waste_short_tiny:4x1@5",
        "silex/rbmk_pellet_mep_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu_mix:0x1@64,nuclear_waste_short_tiny:3x1@20,nuclear_waste_short_tiny:4x1@10",
        "silex/rbmk_pellet_mep_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu_mix:0x1@44,nuclear_waste_short_tiny:3x1@30,nuclear_waste_short_tiny:4x1@15",
        "silex/rbmk_pellet_mep_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu_mix:0x1@24,nuclear_waste_short_tiny:3x1@40,nuclear_waste_short_tiny:4x1@20",
        "silex/rbmk_pellet_mep_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu_mix:0x1@4,nuclear_waste_short_tiny:3x1@50,nuclear_waste_short_tiny:4x1@25",
        "silex/rbmk_pellet_mes_0|600|100|visible|nugget_schrabidium_fuel:0x1@90,nuclear_waste_long_tiny:2x1@1,nuclear_waste_short_tiny:2x1@2,nuclear_waste_long_tiny:4x1@1,nuclear_waste_short_tiny:7x1@2,powder_coal_tiny:0x1@4",
        "silex/rbmk_pellet_mes_1|600|100|visible|nugget_schrabidium_fuel:0x1@70,nuclear_waste_long_tiny:2x1@4,nuclear_waste_short_tiny:2x1@6,nuclear_waste_long_tiny:4x1@4,nuclear_waste_short_tiny:7x1@6,powder_coal_tiny:0x1@10",
        "silex/rbmk_pellet_mes_2|600|100|visible|nugget_schrabidium_fuel:0x1@50,nuclear_waste_long_tiny:2x1@7,nuclear_waste_short_tiny:2x1@10,nuclear_waste_long_tiny:4x1@7,nuclear_waste_short_tiny:7x1@10,powder_coal_tiny:0x1@16",
        "silex/rbmk_pellet_mes_3|600|100|visible|nugget_schrabidium_fuel:0x1@30,nuclear_waste_long_tiny:2x1@10,nuclear_waste_short_tiny:2x1@14,nuclear_waste_long_tiny:4x1@10,nuclear_waste_short_tiny:7x1@14,powder_coal_tiny:0x1@22",
        "silex/rbmk_pellet_mes_4|600|100|visible|nugget_schrabidium_fuel:0x1@10,nuclear_waste_long_tiny:2x1@13,nuclear_waste_short_tiny:2x1@18,nuclear_waste_long_tiny:4x1@13,nuclear_waste_short_tiny:7x1@18,powder_coal_tiny:0x1@28",
        "silex/rbmk_pellet_mes_5|600|100|visible|nugget_schrabidium_fuel:0x1@90,nuclear_waste_long_tiny:2x1@1,nuclear_waste_short_tiny:2x1@2,nuclear_waste_long_tiny:4x1@1,nuclear_waste_short_tiny:7x1@2,powder_coal_tiny:0x1@4",
        "silex/rbmk_pellet_mes_6|600|100|visible|nugget_schrabidium_fuel:0x1@70,nuclear_waste_long_tiny:2x1@4,nuclear_waste_short_tiny:2x1@6,nuclear_waste_long_tiny:4x1@4,nuclear_waste_short_tiny:7x1@6,powder_coal_tiny:0x1@10",
        "silex/rbmk_pellet_mes_7|600|100|visible|nugget_schrabidium_fuel:0x1@50,nuclear_waste_long_tiny:2x1@7,nuclear_waste_short_tiny:2x1@10,nuclear_waste_long_tiny:4x1@7,nuclear_waste_short_tiny:7x1@10,powder_coal_tiny:0x1@16",
        "silex/rbmk_pellet_mes_8|600|100|visible|nugget_schrabidium_fuel:0x1@30,nuclear_waste_long_tiny:2x1@10,nuclear_waste_short_tiny:2x1@14,nuclear_waste_long_tiny:4x1@10,nuclear_waste_short_tiny:7x1@14,powder_coal_tiny:0x1@22",
        "silex/rbmk_pellet_mes_9|600|100|visible|nugget_schrabidium_fuel:0x1@10,nuclear_waste_long_tiny:2x1@13,nuclear_waste_short_tiny:2x1@18,nuclear_waste_long_tiny:4x1@13,nuclear_waste_short_tiny:7x1@18,powder_coal_tiny:0x1@28",
        "silex/rbmk_pellet_meu_0|600|100|ir|nugget_uranium_fuel:0x1@84,nugget_pu239:0x1@6,nuclear_waste_long_tiny:0x1@4,nuclear_waste_short_tiny:0x1@6",
        "silex/rbmk_pellet_meu_1|600|100|ir|nugget_uranium_fuel:0x1@68,nugget_pu_mix:0x1@10,nuclear_waste_long_tiny:0x1@9,nuclear_waste_short_tiny:0x1@13",
        "silex/rbmk_pellet_meu_2|600|100|ir|nugget_uranium_fuel:0x1@52,nugget_pu_mix:0x1@14,nuclear_waste_long_tiny:0x1@14,nuclear_waste_short_tiny:0x1@20",
        "silex/rbmk_pellet_meu_3|600|100|ir|nugget_uranium_fuel:0x1@36,nugget_pu_mix:0x1@18,nuclear_waste_long_tiny:0x1@19,nuclear_waste_short_tiny:0x1@27",
        "silex/rbmk_pellet_meu_4|600|100|ir|nugget_uranium_fuel:0x1@20,nugget_pu_mix:0x1@22,nuclear_waste_long_tiny:0x1@24,nuclear_waste_short_tiny:0x1@34",
        "silex/rbmk_pellet_meu_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium_fuel:0x1@83,nugget_pu239:0x1@6,nuclear_waste_long_tiny:0x1@4,nuclear_waste_short_tiny:0x1@6",
        "silex/rbmk_pellet_meu_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium_fuel:0x1@67,nugget_pu_mix:0x1@10,nuclear_waste_long_tiny:0x1@9,nuclear_waste_short_tiny:0x1@13",
        "silex/rbmk_pellet_meu_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium_fuel:0x1@51,nugget_pu_mix:0x1@14,nuclear_waste_long_tiny:0x1@14,nuclear_waste_short_tiny:0x1@20",
        "silex/rbmk_pellet_meu_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium_fuel:0x1@35,nugget_pu_mix:0x1@18,nuclear_waste_long_tiny:0x1@19,nuclear_waste_short_tiny:0x1@27",
        "silex/rbmk_pellet_meu_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium_fuel:0x1@19,nugget_pu_mix:0x1@22,nuclear_waste_long_tiny:0x1@24,nuclear_waste_short_tiny:0x1@34",
        "silex/rbmk_pellet_mox_0|600|100|ir|nugget_mox_fuel:0x1@84,nugget_pu_mix:0x1@6,nuclear_waste_long_tiny:0x1@2,nuclear_waste_short_tiny:0x1@3,nuclear_waste_short_tiny:3x1@5",
        "silex/rbmk_pellet_mox_1|600|100|ir|nugget_mox_fuel:0x1@64,nugget_pu_mix:0x1@10,nuclear_waste_long_tiny:0x1@5,nuclear_waste_short_tiny:0x1@8,nuclear_waste_short_tiny:3x1@13",
        "silex/rbmk_pellet_mox_2|600|100|ir|nugget_mox_fuel:0x1@44,nugget_pu_mix:0x1@14,nuclear_waste_long_tiny:0x1@8,nuclear_waste_short_tiny:0x1@13,nuclear_waste_short_tiny:3x1@21",
        "silex/rbmk_pellet_mox_3|600|100|ir|nugget_mox_fuel:0x1@24,nugget_pu_mix:0x1@18,nuclear_waste_long_tiny:0x1@11,nuclear_waste_short_tiny:0x1@18,nuclear_waste_short_tiny:3x1@29",
        "silex/rbmk_pellet_mox_4|600|100|ir|nugget_mox_fuel:0x1@4,nugget_pu_mix:0x1@22,nuclear_waste_long_tiny:0x1@14,nuclear_waste_short_tiny:0x1@23,nuclear_waste_short_tiny:3x1@37",
        "silex/rbmk_pellet_mox_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_mox_fuel:0x1@83,nugget_pu_mix:0x1@6,nuclear_waste_long_tiny:0x1@2,nuclear_waste_short_tiny:0x1@3,nuclear_waste_short_tiny:3x1@5",
        "silex/rbmk_pellet_mox_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_mox_fuel:0x1@63,nugget_pu_mix:0x1@10,nuclear_waste_long_tiny:0x1@5,nuclear_waste_short_tiny:0x1@8,nuclear_waste_short_tiny:3x1@13",
        "silex/rbmk_pellet_mox_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_mox_fuel:0x1@43,nugget_pu_mix:0x1@14,nuclear_waste_long_tiny:0x1@8,nuclear_waste_short_tiny:0x1@13,nuclear_waste_short_tiny:3x1@21",
        "silex/rbmk_pellet_mox_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_mox_fuel:0x1@23,nugget_pu_mix:0x1@18,nuclear_waste_long_tiny:0x1@11,nuclear_waste_short_tiny:0x1@18,nuclear_waste_short_tiny:3x1@29",
        "silex/rbmk_pellet_mox_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_mox_fuel:0x1@3,nugget_pu_mix:0x1@22,nuclear_waste_long_tiny:0x1@14,nuclear_waste_short_tiny:0x1@23,nuclear_waste_short_tiny:3x1@37",
        "silex/rbmk_pellet_po210be_0|600|100|ir|nugget_polonium:0x1@45,nugget_beryllium:0x1@45,nugget_lead:0x1@5,powder_coal_tiny:0x1@5",
        "silex/rbmk_pellet_po210be_1|600|100|ir|nugget_polonium:0x1@35,nugget_beryllium:0x1@35,nugget_lead:0x1@15,powder_coal_tiny:0x1@15",
        "silex/rbmk_pellet_po210be_2|600|100|ir|nugget_polonium:0x1@25,nugget_beryllium:0x1@25,nugget_lead:0x1@25,powder_coal_tiny:0x1@25",
        "silex/rbmk_pellet_po210be_3|600|100|ir|nugget_polonium:0x1@15,nugget_beryllium:0x1@15,nugget_lead:0x1@35,powder_coal_tiny:0x1@35",
        "silex/rbmk_pellet_po210be_4|600|100|ir|nugget_polonium:0x1@5,nugget_beryllium:0x1@5,nugget_lead:0x1@45,powder_coal_tiny:0x1@45",
        "silex/rbmk_pellet_pu238be_0|600|100|ir|nugget_pu238:0x1@45,nugget_beryllium:0x1@45,nugget_lead:0x1@3,nuclear_waste_tiny:0x1@2,powder_coal_tiny:0x1@5",
        "silex/rbmk_pellet_pu238be_1|600|100|ir|nugget_pu238:0x1@35,nugget_beryllium:0x1@35,nugget_lead:0x1@8,nuclear_waste_tiny:0x1@7,powder_coal_tiny:0x1@15",
        "silex/rbmk_pellet_pu238be_2|600|100|ir|nugget_pu238:0x1@25,nugget_beryllium:0x1@25,nugget_lead:0x1@13,nuclear_waste_tiny:0x1@12,powder_coal_tiny:0x1@25",
        "silex/rbmk_pellet_pu238be_3|600|100|ir|nugget_pu238:0x1@15,nugget_beryllium:0x1@15,nugget_lead:0x1@18,nuclear_waste_tiny:0x1@17,powder_coal_tiny:0x1@35",
        "silex/rbmk_pellet_pu238be_4|600|100|ir|nugget_pu238:0x1@5,nugget_beryllium:0x1@5,nugget_lead:0x1@23,nuclear_waste_tiny:0x1@22,powder_coal_tiny:0x1@45",
        "silex/rbmk_pellet_pu238be_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu238:0x1@44,nugget_beryllium:0x1@45,nugget_lead:0x1@3,nuclear_waste_tiny:0x1@2,powder_coal_tiny:0x1@5",
        "silex/rbmk_pellet_pu238be_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu238:0x1@34,nugget_beryllium:0x1@35,nugget_lead:0x1@8,nuclear_waste_tiny:0x1@7,powder_coal_tiny:0x1@15",
        "silex/rbmk_pellet_pu238be_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu238:0x1@24,nugget_beryllium:0x1@25,nugget_lead:0x1@13,nuclear_waste_tiny:0x1@12,powder_coal_tiny:0x1@25",
        "silex/rbmk_pellet_pu238be_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu238:0x1@14,nugget_beryllium:0x1@15,nugget_lead:0x1@18,nuclear_waste_tiny:0x1@17,powder_coal_tiny:0x1@35",
        "silex/rbmk_pellet_pu238be_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_pu238:0x1@4,nugget_beryllium:0x1@5,nugget_lead:0x1@23,nuclear_waste_tiny:0x1@22,powder_coal_tiny:0x1@45",
        "silex/rbmk_pellet_ra226be_0|600|100|ir|nugget_ra226:0x1@45,nugget_beryllium:0x1@45,nugget_lead:0x1@3,nugget_polonium:0x1@2,powder_coal_tiny:0x1@5",
        "silex/rbmk_pellet_ra226be_1|600|100|ir|nugget_ra226:0x1@35,nugget_beryllium:0x1@35,nugget_lead:0x1@8,nugget_polonium:0x1@7,powder_coal_tiny:0x1@15",
        "silex/rbmk_pellet_ra226be_2|600|100|ir|nugget_ra226:0x1@25,nugget_beryllium:0x1@25,nugget_lead:0x1@13,nugget_polonium:0x1@12,powder_coal_tiny:0x1@25",
        "silex/rbmk_pellet_ra226be_3|600|100|ir|nugget_ra226:0x1@15,nugget_beryllium:0x1@15,nugget_lead:0x1@18,nugget_polonium:0x1@17,powder_coal_tiny:0x1@35",
        "silex/rbmk_pellet_ra226be_4|600|100|ir|nugget_ra226:0x1@5,nugget_beryllium:0x1@5,nugget_lead:0x1@23,nugget_polonium:0x1@22,powder_coal_tiny:0x1@45",
        "silex/rbmk_pellet_thmeu_0|600|100|ir|nugget_thorium_fuel:0x1@84,nugget_u233:0x1@6,nuclear_waste_long_tiny:3x1@10",
        "silex/rbmk_pellet_thmeu_1|600|100|ir|nugget_thorium_fuel:0x1@64,nugget_u233:0x1@10,nuclear_waste_long_tiny:3x1@26",
        "silex/rbmk_pellet_thmeu_2|600|100|ir|nugget_thorium_fuel:0x1@44,nugget_u233:0x1@14,nuclear_waste_long_tiny:3x1@42",
        "silex/rbmk_pellet_thmeu_3|600|100|ir|nugget_thorium_fuel:0x1@24,nugget_u233:0x1@18,nuclear_waste_long_tiny:3x1@58",
        "silex/rbmk_pellet_thmeu_4|600|100|ir|nugget_thorium_fuel:0x1@4,nugget_u233:0x1@22,nuclear_waste_long_tiny:3x1@74",
        "silex/rbmk_pellet_thmeu_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_thorium_fuel:0x1@83,nugget_u233:0x1@6,nuclear_waste_long_tiny:3x1@10",
        "silex/rbmk_pellet_thmeu_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_thorium_fuel:0x1@63,nugget_u233:0x1@10,nuclear_waste_long_tiny:3x1@26",
        "silex/rbmk_pellet_thmeu_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_thorium_fuel:0x1@43,nugget_u233:0x1@14,nuclear_waste_long_tiny:3x1@42",
        "silex/rbmk_pellet_thmeu_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_thorium_fuel:0x1@23,nugget_u233:0x1@18,nuclear_waste_long_tiny:3x1@58",
        "silex/rbmk_pellet_thmeu_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_thorium_fuel:0x1@3,nugget_u233:0x1@22,nuclear_waste_long_tiny:3x1@74",
        "silex/rbmk_pellet_ueu_0|600|100|ir|nugget_uranium:0x1@86,nugget_pu239:0x1@10,nuclear_waste_long_tiny:0x1@2,nuclear_waste_short_tiny:0x1@2",
        "silex/rbmk_pellet_ueu_1|600|100|ir|nugget_uranium:0x1@75,nugget_pu239:0x1@13,nuclear_waste_long_tiny:0x1@5,nuclear_waste_short_tiny:0x1@7",
        "silex/rbmk_pellet_ueu_2|600|100|ir|nugget_uranium:0x1@64,nugget_pu_mix:0x1@16,nuclear_waste_long_tiny:0x1@8,nuclear_waste_short_tiny:0x1@12",
        "silex/rbmk_pellet_ueu_3|600|100|ir|nugget_uranium:0x1@53,nugget_pu_mix:0x1@19,nuclear_waste_long_tiny:0x1@11,nuclear_waste_short_tiny:0x1@17",
        "silex/rbmk_pellet_ueu_4|600|100|ir|nugget_uranium:0x1@42,nugget_pu_mix:0x1@22,nuclear_waste_long_tiny:0x1@14,nuclear_waste_short_tiny:0x1@22",
        "silex/rbmk_pellet_ueu_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium:0x1@86,nugget_pu239:0x1@10,nuclear_waste_long_tiny:0x1@2,nuclear_waste_short_tiny:0x1@1",
        "silex/rbmk_pellet_ueu_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium:0x1@75,nugget_pu239:0x1@13,nuclear_waste_long_tiny:0x1@5,nuclear_waste_short_tiny:0x1@6",
        "silex/rbmk_pellet_ueu_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium:0x1@64,nugget_pu_mix:0x1@16,nuclear_waste_long_tiny:0x1@8,nuclear_waste_short_tiny:0x1@11",
        "silex/rbmk_pellet_ueu_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium:0x1@53,nugget_pu_mix:0x1@19,nuclear_waste_long_tiny:0x1@11,nuclear_waste_short_tiny:0x1@16",
        "silex/rbmk_pellet_ueu_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_uranium:0x1@42,nugget_pu_mix:0x1@22,nuclear_waste_long_tiny:0x1@14,nuclear_waste_short_tiny:0x1@21",
        "silex/rbmk_pellet_uzh_0|600|100|ir|nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@20,nugget_pu_mix:0x1@3,nuclear_waste_long_tiny:0x1@1,nuclear_waste_short_tiny:0x1@1",
        "silex/rbmk_pellet_uzh_1|600|100|ir|nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@16,nugget_pu_mix:0x1@6,nuclear_waste_long_tiny:0x1@2,nuclear_waste_short_tiny:0x1@2",
        "silex/rbmk_pellet_uzh_2|600|100|ir|nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@12,nugget_pu_mix:0x1@9,nuclear_waste_long_tiny:0x1@3,nuclear_waste_short_tiny:0x1@3",
        "silex/rbmk_pellet_uzh_3|600|100|ir|nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@8,nugget_pu_mix:0x1@12,nuclear_waste_long_tiny:0x1@4,nuclear_waste_short_tiny:0x1@4",
        "silex/rbmk_pellet_uzh_4|600|100|ir|nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@4,nugget_pu_mix:0x1@15,nuclear_waste_long_tiny:0x1@5,nuclear_waste_short_tiny:0x1@5",
        "silex/rbmk_pellet_uzh_5|600|100|ir|powder_xe135_tiny:0x1@1,nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@19,nugget_pu_mix:0x1@3,nuclear_waste_long_tiny:0x1@1,nuclear_waste_short_tiny:0x1@1",
        "silex/rbmk_pellet_uzh_6|600|100|ir|powder_xe135_tiny:0x1@1,nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@15,nugget_pu_mix:0x1@6,nuclear_waste_long_tiny:0x1@2,nuclear_waste_short_tiny:0x1@2",
        "silex/rbmk_pellet_uzh_7|600|100|ir|powder_xe135_tiny:0x1@1,nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@11,nugget_pu_mix:0x1@9,nuclear_waste_long_tiny:0x1@3,nuclear_waste_short_tiny:0x1@3",
        "silex/rbmk_pellet_uzh_8|600|100|ir|powder_xe135_tiny:0x1@1,nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@7,nugget_pu_mix:0x1@12,nuclear_waste_long_tiny:0x1@4,nuclear_waste_short_tiny:0x1@4",
        "silex/rbmk_pellet_uzh_9|600|100|ir|powder_xe135_tiny:0x1@1,nugget_zirconium:0x1@75,nugget_uranium_fuel:0x1@3,nugget_pu_mix:0x1@15,nuclear_waste_long_tiny:0x1@5,nuclear_waste_short_tiny:0x1@5",
        "silex/rbmk_pellet_zfb_am_mix_0|600|100|visible|nugget_pu241:0x1@100,nugget_am_mix:0x1@50,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_am_mix_1|600|100|visible|nugget_pu241:0x1@80,nugget_am_mix:0x1@70,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_am_mix_2|600|100|visible|nugget_pu241:0x1@60,nugget_am_mix:0x1@90,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_am_mix_3|600|100|visible|nugget_pu241:0x1@40,nugget_am_mix:0x1@110,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_am_mix_4|600|100|visible|nugget_pu241:0x1@20,nugget_am_mix:0x1@130,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_am_mix_5|600|100|visible|powder_xe135_tiny:0x1@3,nugget_pu241:0x1@100,nugget_am_mix:0x1@50,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_am_mix_6|600|100|visible|powder_xe135_tiny:0x1@3,nugget_pu241:0x1@80,nugget_am_mix:0x1@70,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_am_mix_7|600|100|visible|powder_xe135_tiny:0x1@3,nugget_pu241:0x1@60,nugget_am_mix:0x1@90,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_am_mix_8|600|100|visible|powder_xe135_tiny:0x1@3,nugget_pu241:0x1@40,nugget_am_mix:0x1@110,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_am_mix_9|600|100|visible|powder_xe135_tiny:0x1@3,nugget_pu241:0x1@20,nugget_am_mix:0x1@130,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_bismuth_0|600|100|visible|nugget_uranium:0x1@50,nugget_pu241:0x1@50,nugget_bismuth:0x1@50,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_bismuth_1|600|100|visible|nugget_uranium:0x1@40,nugget_pu241:0x1@40,nugget_bismuth:0x1@70,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_bismuth_2|600|100|visible|nugget_uranium:0x1@30,nugget_pu241:0x1@30,nugget_bismuth:0x1@90,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_bismuth_3|600|100|visible|nugget_uranium:0x1@20,nugget_pu241:0x1@20,nugget_bismuth:0x1@110,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_bismuth_4|600|100|visible|nugget_uranium:0x1@10,nugget_pu241:0x1@10,nugget_bismuth:0x1@130,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_bismuth_5|600|100|visible|powder_xe135_tiny:0x1@3,nugget_uranium:0x1@50,nugget_pu241:0x1@50,nugget_bismuth:0x1@50,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_bismuth_6|600|100|visible|powder_xe135_tiny:0x1@3,nugget_uranium:0x1@40,nugget_pu241:0x1@40,nugget_bismuth:0x1@70,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_bismuth_7|600|100|visible|powder_xe135_tiny:0x1@3,nugget_uranium:0x1@30,nugget_pu241:0x1@30,nugget_bismuth:0x1@90,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_bismuth_8|600|100|visible|powder_xe135_tiny:0x1@3,nugget_uranium:0x1@20,nugget_pu241:0x1@20,nugget_bismuth:0x1@110,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_bismuth_9|600|100|visible|powder_xe135_tiny:0x1@3,nugget_uranium:0x1@10,nugget_pu241:0x1@10,nugget_bismuth:0x1@130,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_pu241_0|600|100|visible|nugget_u235:0x1@50,nugget_pu240:0x1@50,nugget_pu241:0x1@50,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_pu241_1|600|100|visible|nugget_u235:0x1@40,nugget_pu240:0x1@40,nugget_pu241:0x1@70,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_pu241_2|600|100|visible|nugget_u235:0x1@30,nugget_pu240:0x1@30,nugget_pu241:0x1@90,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_pu241_3|600|100|visible|nugget_u235:0x1@20,nugget_pu240:0x1@20,nugget_pu241:0x1@110,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_pu241_4|600|100|visible|nugget_u235:0x1@10,nugget_pu240:0x1@10,nugget_pu241:0x1@130,nugget_zirconium:0x1@150",
        "silex/rbmk_pellet_zfb_pu241_5|600|100|visible|powder_xe135_tiny:0x1@3,nugget_u235:0x1@50,nugget_pu240:0x1@50,nugget_pu241:0x1@50,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_pu241_6|600|100|visible|powder_xe135_tiny:0x1@3,nugget_u235:0x1@40,nugget_pu240:0x1@40,nugget_pu241:0x1@70,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_pu241_7|600|100|visible|powder_xe135_tiny:0x1@3,nugget_u235:0x1@30,nugget_pu240:0x1@30,nugget_pu241:0x1@90,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_pu241_8|600|100|visible|powder_xe135_tiny:0x1@3,nugget_u235:0x1@20,nugget_pu240:0x1@20,nugget_pu241:0x1@110,nugget_zirconium:0x1@147",
        "silex/rbmk_pellet_zfb_pu241_9|600|100|visible|powder_xe135_tiny:0x1@3,nugget_u235:0x1@10,nugget_pu240:0x1@10,nugget_pu241:0x1@130,nugget_zirconium:0x1@147",
        "silex/redmud|300|50|visible|powder_aluminium:0x1@10,powder_neodymium_tiny:0x3@5,powder_boron_tiny:0x3@5,nugget_zirconium:0x1@5,powder_iron:0x1@20,powder_titanium:0x1@15,powder_sodium:0x1@10",
        "silex/uf6|900|100|visible|nugget_u235:0x1@1,nugget_u238:0x1@11",
        "silex/vitriol|1000|300|ir|powder_bromine:0x1@5,powder_iodine:0x1@5,powder_iron:0x1@5,sulfur:0x1@15"
    };
    /** path|autoswitch group|duration|power|item input counts|fluid in|fluid out|outputs (PUREXRecipes, no Naquadria) */
    static final String[] PUREX = {
        "purex/uzh||600|1000|1,3|nitric_acid:1000,hydrogen:4000||billet_uzhx4",
        "purex/flash_gold||600|1000|1,1|amat:1000||billet_balefire_goldx2",
        "purex/flash_lead||600|1000|1,1|amat:1000||billet_flashleadx1",
        "purex/pile_pu239|autoswitch.pile|40|100|1|sulfuric_acid:100||billet_pu239x2,billet_uraniumx1",
        "purex/pile_rgp|autoswitch.pile|40|100|1|sulfuric_acid:100||billet_pu_mixx2,billet_nuclear_wastex1",
        "purex/pile_thorium|autoswitch.pile|40|100|1|sulfuric_acid:100||billet_thorium_fuelx2,billet_nuclear_wastex1",
        "purex/pile_waste|autoswitch.pile|40|100|1|sulfuric_acid:100||billet_nuclear_wastex2,billet_poloniumx1",
        "purex/zirnox_natural_uranium|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_u238x1,nugget_pu_mixx2,nugget_pu239x1,nuclear_waste_tinyx2",
        "purex/zirnox_meu|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_pu_mixx1,nugget_plutoniumx2,nugget_technetiumx1,nuclear_waste_tinyx2",
        "purex/zirnox_thorium|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_u238x1,nugget_th232x1,nugget_u233x2,nuclear_waste_tinyx2",
        "purex/zirnox_mox|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_pu_mixx1,nugget_technetiumx1,nugget_u238x1,nuclear_waste_tinyx3",
        "purex/zirnox_mep|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_pu_mixx2,nugget_technetiumx1,nuclear_waste_tinyx3",
        "purex/zirnox_heu233|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_u235x1,nugget_neptuniumx1,nugget_technetiumx1,nuclear_waste_tinyx3",
        "purex/zirnox_heu235|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_pu238x1,nugget_neptuniumx1,nugget_technetiumx1,nuclear_waste_tinyx3",
        "purex/zirnox_les|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_berylliumx2,nugget_pu239x1,nuclear_waste_tinyx1,nuclear_waste_tinyx2",
        "purex/zirnox_zfb_mox|autoswitch.zirnox|100|1000|1|kerosene:500,nitric_acid:250||nugget_zirconiumx3,nugget_technetiumx1,nugget_pu_mixx1,nuclear_waste_tinyx1",
        "purex/plate_mox|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||powder_sr90_tinyx1,nugget_pu_mixx3,powder_cs137_tinyx1,nuclear_waste_tinyx4",
        "purex/plate_pu238be|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||nugget_berylliumx1,nugget_pu238x1,powder_coal_tinyx2,nugget_leadx2",
        "purex/plate_pu239|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||nugget_pu240x2,nugget_technetiumx1,powder_cs137_tinyx1,nuclear_waste_tinyx5",
        "purex/plate_ra226be|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||nugget_berylliumx2,nugget_poloniumx2,powder_coal_tinyx1,nugget_leadx1",
        "purex/plate_sa326|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||nugget_soliniumx1,powder_neodymium_tinyx1,nugget_tantaliumx1,nuclear_waste_tinyx6",
        "purex/plate_u233|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||nugget_u235x1,powder_i131_tinyx1,powder_sr90_tinyx1,nuclear_waste_tinyx6",
        "purex/plate_u235|autoswitch.plate|100|1500|1|kerosene:500,nitric_acid:250||nugget_neptuniumx1,nugget_pu238x1,nugget_technetiumx1,nuclear_waste_tinyx6",
        "purex/pwr_meu|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_u238x3,nugget_plutoniumx4,nugget_technetiumx2,nuclear_waste_tinyx3",
        "purex/pwr_heu233|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_u235x3,nugget_pu238x3,nugget_technetiumx1,nuclear_waste_tinyx5",
        "purex/pwr_heu235|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_neptuniumx3,nugget_pu238x3,nugget_technetiumx1,nuclear_waste_tinyx5",
        "purex/pwr_men|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_u238x3,nugget_pu239x4,nugget_technetiumx2,nuclear_waste_tinyx3",
        "purex/pwr_hen237|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_pu238x2,nugget_pu239x4,nugget_technetiumx1,nuclear_waste_tinyx5",
        "purex/pwr_mox|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_u238x3,nugget_pu240x4,nugget_technetiumx2,nuclear_waste_tinyx3",
        "purex/pwr_mep|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_leadx2,nugget_pu_mixx4,nugget_technetiumx2,nuclear_waste_tinyx3",
        "purex/pwr_hep239|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_pu_mixx2,nugget_pu240x4,nugget_technetiumx1,nuclear_waste_tinyx5",
        "purex/pwr_hep241|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_leadx3,nugget_zirconiumx2,nugget_technetiumx1,nuclear_waste_tinyx6",
        "purex/pwr_mea|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_leadx3,nugget_zirconiumx2,nugget_technetiumx1,nuclear_waste_tinyx6",
        "purex/pwr_hea242|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_leadx3,nugget_zirconiumx2,nugget_technetiumx1,nuclear_waste_tinyx6",
        "purex/pwr_hes326|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_soliniumx3,nugget_leadx2,nugget_euphemiumx1,nuclear_waste_tinyx6",
        "purex/pwr_hes327|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_australiumx4,nugget_leadx1,nugget_euphemiumx1,nuclear_waste_tinyx6",
        "purex/pwr_bfb_am_mix|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_am_mixx9,nugget_pu_mixx2,nugget_bismuthx6,nuclear_waste_tinyx1",
        "purex/pwr_bfb_pu241|autoswitch.pwr|100|2500|1|kerosene:500,nitric_acid:250||nugget_pu241x9,nugget_pu_mixx2,nugget_bismuthx6,nuclear_waste_tinyx1",
        "purex/thorium_salt||20|10000|2|thorium_salt_depleted:16000|thorium_salt:16000|nugget_u233x1@0.5,nuclear_waste_tinyx1@0.25",
        "purex/watz_schrabidium|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_soliniumx15,nugget_euphemiumx3,nuclear_wastex2",
        "purex/watz_hes|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_soliniumx17,nugget_euphemiumx1,nuclear_wastex2",
        "purex/watz_mes|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_soliniumx12,nugget_tantaliumx6,nuclear_wastex2",
        "purex/watz_les|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_soliniumx9,nugget_tantaliumx9,nuclear_wastex2",
        "purex/watz_hen|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_pu239x12,nugget_technetiumx6,nuclear_wastex2",
        "purex/watz_meu|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_pu239x12,nugget_bismuthx6,nuclear_wastex2",
        "purex/watz_mep|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_pu241x12,nugget_bismuthx6,nuclear_wastex2",
        "purex/watz_lead|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_leadx6,nugget_bismuthx12,nuclear_wastex2",
        "purex/watz_boron|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|powder_coal_tinyx12,nugget_co60x6,nuclear_wastex2",
        "purex/watz_du|autoswitch.watz|60|10000|1|kerosene:500,nitric_acid:250|watz:1000|nugget_poloniumx12,nugget_pu238x6,nuclear_wastex2",
        "purex/icf||300|10000|1||helium4:1250|icf_pellet_emptyx1,pellet_chargedx1,powder_ironx1",
        "purex/vitrify_liquid||100|1000|1|wastefluid:1000||nuclear_waste_vitrifiedx1",
        "purex/vitrify_gas||100|1000|1|wastegas:1000||nuclear_waste_vitrifiedx1",
        "purex/vitrify_solid||300|1000|1,4|||nuclear_waste_vitrifiedx4",
        "purex/schraranium||200|1000|1|kerosene:2000,nitric_acid:1000||nugget_schrabidiumx3,nugget_uraniumx3,nugget_neptuniumx2",
        "purex/schrab_zirnox|autoswitch.schrab|200|50000|1|solvent:4000,schrabidic:250||powder_schrabidiumx1,nugget_technetiumx3,nuclear_waste_tinyx4",
        "purex/schrab_pwr_mep|autoswitch.schrab|200|50000|1|solvent:4000,schrabidic:250||powder_schrabidiumx1,nugget_technetiumx3,nuclear_waste_tinyx4",
        "purex/schrab_pwr_men|autoswitch.schrab|200|50000|1|solvent:4000,schrabidic:250||powder_schrabidiumx1,nugget_technetiumx3,nuclear_waste_tinyx4"
    };
    /** particle|input (tag c:..., otherwise item)|output|antimatter (CyclotronRecipes.registerDefaults) */
    static final String[] CYCLOTRON = {
        "part_lithium|c:dusts/lithium|powder_beryllium|50",
        "part_lithium|c:dusts/beryllium|powder_boron|50",
        "part_lithium|c:dusts/boron|powder_coal|50",
        "part_lithium|c:dusts/quartz|powder_fire|50",
        "part_lithium|c:dusts/phosphorus|sulfur|50",
        "part_lithium|c:dusts/iron|powder_cobalt|50",
        "part_lithium|powder_strontium|powder_zirconium|50",
        "part_lithium|c:dusts/gold|nugget_mercury|50",
        "part_lithium|c:dusts/polonium|powder_astatine|50",
        "part_lithium|c:dusts/lanthanium|powder_cerium|50",
        "part_lithium|c:dusts/actinium|powder_thorium|50",
        "part_lithium|c:dusts/uranium|powder_neptunium|50",
        "part_lithium|c:dusts/neptunium|powder_plutonium|50",
        "part_beryllium|c:dusts/lithium|powder_boron|25",
        "part_beryllium|c:dusts/quartz|sulfur|25",
        "part_beryllium|c:dusts/titanium|powder_iron|25",
        "part_beryllium|c:dusts/cobalt|powder_copper|25",
        "part_beryllium|powder_strontium|powder_niobium|25",
        "part_beryllium|powder_cerium|powder_neodymium|25",
        "part_beryllium|c:dusts/thorium|powder_uranium|25",
        "part_carbon|c:dusts/boron|powder_aluminium|10",
        "part_carbon|c:dusts/sulfur|powder_titanium|10",
        "part_carbon|c:dusts/titanium|powder_cobalt|10",
        "part_carbon|powder_caesium|powder_lanthanium|10",
        "part_carbon|powder_neodymium|powder_gold|10",
        "part_carbon|nugget_mercury|powder_polonium|10",
        "part_carbon|c:dusts/lead|powder_ra226|10",
        "part_carbon|powder_astatine|powder_actinium|10",
        "part_copper|c:dusts/beryllium|powder_quartz|15",
        "part_copper|c:dusts/coal|powder_bromine|15",
        "part_copper|c:dusts/titanium|powder_strontium|15",
        "part_copper|c:dusts/iron|powder_niobium|15",
        "part_copper|powder_bromine|powder_iodine|15",
        "part_copper|powder_strontium|powder_neodymium|15",
        "part_copper|powder_niobium|powder_caesium|15",
        "part_copper|powder_iodine|powder_polonium|15",
        "part_copper|powder_caesium|powder_actinium|15",
        "part_copper|c:dusts/gold|powder_uranium|15",
        "part_plutonium|c:dusts/phosphorus|powder_tennessine|100",
        "part_plutonium|c:dusts/plutonium|powder_tennessine|100",
        "part_plutonium|powder_tennessine|powder_australium|100",
        "part_plutonium|pellet_charged|nugget_schrabidium|1000"
    };
}
