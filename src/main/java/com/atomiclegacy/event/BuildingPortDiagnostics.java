package com.atomiclegacy.event;

import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyNukeDefinition;
import com.atomiclegacy.tileentity.LegacyNukeBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.atomiclegacy.blocks.LegacyVariantSlabBlock;
import com.atomiclegacy.blocks.SpotlightBeamBlock;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.LegacyVariantBlockItem;
import com.atomiclegacy.inventory.recipes.ingredient.LegacyFluidContainerIngredient;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.LegacyContentVisibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Opt-in comparison against the pinned original source, on a real dedicated server. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class BuildingPortDiagnostics {
    private static final BlockPos BLAST_TEST = new BlockPos(1032, 220, 1032);
    private static int blastTestTicks = -1;

    private BuildingPortDiagnostics() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("atomiclegacy.buildingAudit")) {
            return;
        }
        var server = event.getServer();
        var level = server.overworld();
        var pos = new BlockPos(0, 300, 0);
        List<String> problems = new ArrayList<>();
        int registered = 0;
        try (var input = BuildingPortDiagnostics.class.getResourceAsStream("/legacy/atomiclegacy/building-scope.json")) {
            if (input == null) throw new IllegalStateException("Missing original-source fixture");
            var rows = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            for (var element : rows) {
                var row = element.getAsJsonObject();
                var id = ResourceLocation.parse("atomiclegacy:" + row.get("id").getAsString());
                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    problems.add("Missing block " + id);
                    continue;
                }
                registered++;
                var block = BuiltInRegistries.BLOCK.get(id);
                var state = block.defaultBlockState();
                if (block instanceof net.minecraft.world.level.block.LiquidBlockContainer container
                        && container.canPlaceLiquid(null, level, pos, state, net.minecraft.world.level.material.Fluids.WATER)) {
                    problems.add(id + " unexpectedly accepts waterlogging");
                }
                if (row.has("hardness")) {
                    float expected = row.get("hardness").getAsFloat();
                    float actual = state.getDestroySpeed(level, pos);
                    if (Math.abs(expected - actual) > 0.001F) problems.add(id + " hardness " + actual + " expected " + expected);
                }
                if (row.has("resistance")) {
                    float expected = row.get("resistance").getAsFloat();
                    float actual = block.getExplosionResistance(state, level, pos, null);
                    if (Math.abs(expected - actual) > 0.001F) problems.add(id + " resistance " + actual + " expected " + expected);
                }
            }
        }

        int checkedRecipes = 0;
        try (var input = BuildingPortDiagnostics.class.getResourceAsStream("/legacy/atomiclegacy/building-recipes.json")) {
            if (input == null) throw new IllegalStateException("Missing original recipe fixture");
            var rows = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            for (var element : rows) {
                var row = element.getAsJsonObject();
                var id = ResourceLocation.parse("atomiclegacy:" + row.get("recipe").getAsString());
                var recipe = server.getRecipeManager().byKey(id);
                if (recipe.isEmpty()) {
                    problems.add("Missing source recipe " + id);
                    continue;
                }
                var expected = ItemStack.CODEC.parse(net.minecraft.resources.RegistryOps.create(
                        com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess()), row.get("result")).getOrThrow();
                var actual = recipe.get().value().getResultItem(server.registryAccess());
                for (var inputIngredient : recipe.get().value().getIngredients()) {
                    if (!inputIngredient.isEmpty() && inputIngredient.getItems().length == 0) {
                        problems.add("Source recipe has an empty ingredient/tag " + id);
                    }
                }
                if (expected.getCount() != actual.getCount() || !ItemStack.isSameItemSameComponents(expected, actual)) {
                    problems.add("Source recipe output mismatch " + id);
                }
                checkedRecipes++;
            }
        }

        for (var holder : List.of(HbmBlocks.BRICK_SLAB, HbmBlocks.CONCRETE_SLAB,
                HbmBlocks.CONCRETE_BRICK_SLAB, HbmBlocks.STONES_SLAB)) {
            var block = (LegacyVariantSlabBlock) holder.get();
            if (!(block.asItem() instanceof LegacyVariantBlockItem item)) {
                problems.add(holder.getId() + " missing variant-aware item");
                continue;
            }
            for (int variant = 0; variant <= block.maxVariant(); variant++) {
                var state = block.defaultBlockState().setValue(LegacyVariantSlabBlock.VARIANT, variant);
                var doubled = state.setValue(LegacyVariantSlabBlock.TYPE, SlabType.DOUBLE);
                var drops = Block.getDrops(doubled, level, pos, null);
                if (drops.size() != 1 || drops.getFirst().getCount() != 2 || item.variantIndex(drops.getFirst()) != variant) {
                    problems.add(holder.getId() + " double drop mismatch at variant " + variant);
                }
            }
        }

        var fence = HbmBlocks.FENCE_METAL.get();
        for (int mask = 0; mask < 16; mask++) {
            for (boolean post : List.of(false, true)) {
                var state = fence.defaultBlockState()
                        .setValue(com.atomiclegacy.blocks.MetalFenceBlock.NORTH, (mask & 1) != 0)
                        .setValue(com.atomiclegacy.blocks.MetalFenceBlock.SOUTH, (mask & 2) != 0)
                        .setValue(com.atomiclegacy.blocks.MetalFenceBlock.WEST, (mask & 4) != 0)
                        .setValue(com.atomiclegacy.blocks.MetalFenceBlock.EAST, (mask & 8) != 0)
                        .setValue(com.atomiclegacy.blocks.MetalFenceBlock.PILLAR, post);
                // Original collision is the union of two bars through the center.
                var expected = net.minecraft.world.phys.shapes.Shapes.or(
                        net.minecraft.world.phys.shapes.Shapes.box(0.375, 0, (mask & 1) != 0 ? 0 : 0.375,
                                0.625, 1, (mask & 2) != 0 ? 1 : 0.625),
                        net.minecraft.world.phys.shapes.Shapes.box((mask & 4) != 0 ? 0 : 0.375, 0, 0.375,
                                (mask & 8) != 0 ? 1 : 0.625, 1, 0.625));
                if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(expected,
                        state.getCollisionShape(level, pos), net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME)) {
                    problems.add("Metal fence source collision mismatch mask=" + mask + " post=" + post);
                }
            }
        }

        checkCrossingBeams(level, pos.east(12), problems);
        int fittingCases = checkBuildingFittings(level, pos, problems);

        // BlockBarrier: selection follows metadata even when other sides connect.
        for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var selectedSide = facing.getOpposite();
            var expectedSelection = switch (selectedSide) {
                case NORTH -> Block.box(0, 0, 0, 16, 16, 2);
                case EAST -> Block.box(14, 0, 0, 16, 16, 16);
                case SOUTH -> Block.box(0, 0, 14, 16, 16, 16);
                case WEST -> Block.box(0, 0, 0, 2, 16, 16);
                default -> throw new IllegalStateException();
            };
            for (int mask = 0; mask < 16; mask++) {
                var barrier = HbmBlocks.WOOD_BARRIER.get().defaultBlockState()
                        .setValue(com.atomiclegacy.blocks.WoodBarrierBlock.FACING, facing)
                        .setValue(com.atomiclegacy.blocks.WoodBarrierBlock.NORTH, (mask & 1) != 0)
                        .setValue(com.atomiclegacy.blocks.WoodBarrierBlock.EAST, (mask & 2) != 0)
                        .setValue(com.atomiclegacy.blocks.WoodBarrierBlock.SOUTH, (mask & 4) != 0)
                        .setValue(com.atomiclegacy.blocks.WoodBarrierBlock.WEST, (mask & 8) != 0);
                checkShape("wood barrier selection " + facing + " connections=" + mask,
                        expectedSelection, barrier.getShape(level, pos), problems);
            }
        }

        // BlockSpeedy and BlockSpeedyStairs use a literal blue tooltip in the pinned source.
        for (var holder : List.of(HbmBlocks.ASPHALT, HbmBlocks.ASPHALT_LIGHT, HbmBlocks.ASPHALT_STAIRS)) {
            var tooltip = new ArrayList<net.minecraft.network.chat.Component>();
            holder.get().appendHoverText(new ItemStack(holder.get()),
                    net.minecraft.world.item.Item.TooltipContext.of(level), tooltip,
                    net.minecraft.world.item.TooltipFlag.NORMAL);
            if (tooltip.size() != 1 || !tooltip.getFirst().getString().equals("Increases speed by 50%")
                    || !net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.BLUE)
                            .equals(tooltip.getFirst().getStyle().getColor())) {
                problems.add("Source asphalt tooltip mismatch " + holder.getId());
            }
        }

        var tritium = HbmFluids.byName("tritium").orElseThrow();
        var ingredient = new LegacyFluidContainerIngredient("tritium", 1000);
        int containers = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof HbmFluidContainerItem container && container.isFilledContainer() && container.kind().allows(tritium)) {
                boolean expected = container.kind().capacity() == 1000;
                if (ingredient.test(container.filledStack(tritium)) != expected) problems.add("Tritium capacity mismatch " + BuiltInRegistries.ITEM.getKey(item));
                containers++;
            }
        }
        if (containers == 0 || ingredient.test(new ItemStack(Items.WATER_BUCKET))) problems.add("Tritium ingredient invalid");

        var steelTank = com.atomiclegacy.registry.HbmItems.TANK_STEEL.get();
        if (steelTank.getDefaultMaxStackSize() != 64) problems.add("Steel tank source stack limit mismatch");
        var tankRecipe = server.getRecipeManager().byKey(ResourceLocation.parse("atomiclegacy:tank_steel"));
        if (tankRecipe.isEmpty() || tankRecipe.get().value().getResultItem(server.registryAccess()).getCount() != 2) {
            problems.add("Steel tank source crafting output mismatch");
        }
        var diesel = HbmFluids.byName("diesel").orElseThrow();
        var stackedTanks = ((com.atomiclegacy.items.TankSteelItem) steelTank).createFluidHandler(new ItemStack(steelTank, 2));
        if (stackedTanks.fill(HbmFluids.toNeoStack(diesel, 10_000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) != 0
                || stackedTanks.getContainer().getCount() != 2) {
            problems.add("Steel tank fluid handler consumed stacked containers");
        }

        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, server.registryAccess());
        net.neoforged.neoforge.common.CreativeModeTabRegistry.sortTabs();
        var tabIds = net.neoforged.neoforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .map(BuiltInRegistries.CREATIVE_MODE_TAB::getKey)
                .filter(id -> id.getNamespace().equals("atomiclegacy")).map(ResourceLocation::getPath).toList();
        var expectedTabs = List.of("materials", "control", "templates", "building", "machines", "nuclear_weapons",
                "rockets_missiles", "turrets", "consumables");
        if (!tabIds.equals(expectedTabs)) problems.add("Creative tab order " + tabIds);
        var seenStacks = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
        for (var holder : HbmCreativeTabs.TABS.getEntries()) {
            var tab = holder.get();
            if (tab.getIconItem().isEmpty() || tab.getDisplayItems().isEmpty()) problems.add("Empty tab or icon " + holder.getId());
            for (var stack : tab.getDisplayItems()) {
                if (!seenStacks.add(stack) || LegacyContentVisibility.isCreativeHidden(stack))
                    problems.add("Duplicate or hidden creative entry " + stack + " in " + holder.getId());
            }
        }
        var entries = HbmCreativeTabs.BUILDING.get().getDisplayItems();
        for (ItemStack stack : entries) {
            if (LegacyContentVisibility.isCreativeHidden(stack)) problems.add("Hidden item visible " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        var kitKey = AtomicLegacy.id("weapon_mod_special_greasegun");
        var kitRecipe = server.getRecipeManager().byKey(kitKey).orElseThrow().value();
        for (String plastic : List.of("polymer", "bakelite")) {
            List<ItemStack> grid = new ArrayList<>();
            for (String part : List.of("part_barrel_light_weaponsteel", "part_receiver_light_weaponsteel",
                    "part_mechanism_weaponsteel", "plate_dura_steel", "", "part_grip_" + plastic)) {
                grid.add(part.isEmpty() ? ItemStack.EMPTY : BuiltInRegistries.ITEM
                        .getOptional(AtomicLegacy.id(part)).orElseThrow().getDefaultInstance());
            }
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, grid);
            if (!(kitRecipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped)
                    || !shaped.matches(input, level) || !shaped.assemble(input, server.registryAccess()).is(HbmItems.GREASEGUN_KIT.get())) {
                problems.add("M3 kit crafting failed with " + plastic);
            }
        }
        int circuitCases = checkCircuitBatch(server, problems);
        int gunCases = GunPortDiagnostics.run(server, problems);
        gunCases += GunPartsDiagnostics.run(server, problems);
        gunCases += GunBatchADiagnostics.run(server, problems);
        gunCases += ParityH3Diagnostics.run(server, problems); // [parity H3: diagnostics]
        // [weapons batch A: diagnostics]
        gunCases += GunBatchBDiagnostics.run(server, problems);
        // [weapons batch B: diagnostics]
        gunCases += GunBatchC1Diagnostics.run(server, problems);
        // [weapons batch C1: diagnostics]
        gunCases += GunBatchC2Diagnostics.run(server, problems);
        // [weapons batch C2: diagnostics]
        // [weapons batch C3: diagnostics]
        gunCases += GunBatchC3Diagnostics.run(server, problems);
        // [weapons batch C4: diagnostics]
        gunCases += GunBatchC4Diagnostics.run(server, problems);
        // [weapons batch D2: diagnostics]
        gunCases += GunBatchD2Diagnostics.run(server, problems);
        // [weapons batch D3: diagnostics]
        gunCases += GunBatchD3Diagnostics.run(server, problems);
        // [weapons batch E2: diagnostics]
        gunCases += GunBatchE2Diagnostics.run(server, problems);
        // [weapons batch D1: diagnostics]
        gunCases += GunBatchD1Diagnostics.run(server, problems);
        // [weapons batch E1: diagnostics]
        gunCases += GunBatchE1Diagnostics.run(server, problems);
        // [weapons batch F4: diagnostics]
        gunCases += GunBatchF4Diagnostics.run(server, problems);
        // [weapons batch F1: diagnostics]
        gunCases += GunBatchF1Diagnostics.run(server, problems);
        // [weapons batch F3: diagnostics]
        gunCases += GunBatchF3Diagnostics.run(server, problems);
        // [weapons batch F2: diagnostics]
        gunCases += GunBatchF2Diagnostics.run(server, problems);
        gunCases += GunModsG1Diagnostics.run(server, problems); // [weapon mods G1: diagnostics]
        gunCases += GunModsG3Diagnostics.run(server, problems); // [weapon mods G3: diagnostics]
        gunCases += GunModsG2Diagnostics.run(server, problems); // [weapon mods G2: diagnostics]
        gunCases += ParityH2Diagnostics.run(server, problems); // [parity H2: diagnostics]
        int parityH1Cases = ParityH1Diagnostics.run(server, problems)
                + ParityH1MachinesDiagnostics.run(server, problems); // [parity H1: diagnostics] [parity h1 2026-11-19: machines]
        int parityI1Cases = ParityI1Diagnostics.run(server, problems); // [parity I1: diagnostics]
        int parityJ1Cases = ParityJ1Diagnostics.run(server, problems); // [parity J1: diagnostics]
        int parityJ2Cases = ParityJ2Diagnostics.run(server, problems); // [parity j2: diagnostics]
        int parityJ1bCases = ParityJ1bDiagnostics.run(server, problems); // [parity J1b: old AI, polaroidID, 1-to-9]
        int parityK1Cases = ParityK1Diagnostics.run(server, problems); // [parity K1: diagnostics]
        int parityK1bCases = ParityK1bDiagnostics.run(server, problems); // [parity K1b: recipe selector, machine_reactor, AI]
        int parityI2Cases = ParityI2Diagnostics.run(server, problems); // [parity I2: diagnostics]
        int parityI3Cases = ParityI3Diagnostics.run(server, problems); // [parity I3: diagnostics]
        int parityK2Cases = ParityK2Diagnostics.run(server, problems); // [parity K2: diagnostics]
        int parityK2VanillaCases = ParityK2VanillaDiagnostics.run(server, problems); // [parity K2: 1.7.10 vanilla sources]
        int parityK3Cases = ParityK3Diagnostics.run(server, problems); // [parity K3: diagnostics]
        int parityL1Cases = ParityL1Diagnostics.run(server, problems); // [parity L1: diagnostics]
        int parityL2Cases = ParityL2Diagnostics.run(server, problems); // [parity L2: diagnostics]
        int parityL3Cases = ParityL3Diagnostics.run(server, problems); // [parity L3: diagnostics]
        int parityM1Cases = ParityM1Diagnostics.run(server, problems); // [parity M1: diagnostics]
        int parityM2Cases = ParityM2Diagnostics.run(server, problems); // [parity M2: diagnostics]
        int parityM3Cases = ParityM3Diagnostics.run(server, problems); // [parity M3: diagnostics]
        int parityM1ItemCases = ParityM1ItemDiagnostics.run(server, problems); // [parity m1: diagnostics]
        int parityP1Cases = ParityP1Diagnostics.run(server, problems); // [parity P1: diagnostics]
        int parityP3Cases = ParityP3Diagnostics.run(server, problems); // [parity P3: diagnostics]
        int parityQ2Cases = ParityQ2Diagnostics.run(server, problems); // [parity Q2: diagnostics]
        int parityP2Cases = ParityP2Diagnostics.run(server, problems); // [parity P2: diagnostics]
        int parityQ4Cases = ParityQ4Diagnostics.run(server, problems); // [parity Q4: diagnostics]
        int parityQ3Cases = ParityQ3Diagnostics.run(server, problems); // [parity Q3: diagnostics]
        int parityQ1Cases = ParityQ1Diagnostics.run(server, problems); // [parity Q1: diagnostics]
        int parityR1Cases = ParityR1Diagnostics.run(server, problems); // [parity R1: diagnostics]
        int parityR4Cases = ParityR4Diagnostics.run(server, problems); // [parity R4: diagnostics]
        int parityR2Cases = ParityR2Diagnostics.run(server, problems); // [parity R2: diagnostics]
        int parityR3Cases = ParityR3Diagnostics.run(server, problems); // [parity R3: diagnostics]
        int parityS2Cases = ParityS2Diagnostics.run(server, problems); // [parity S2: diagnostics]
        int parityV3Cases = ParityV3Diagnostics.run(server, problems); // [parity V3: diagnostics]
        int parityS3Cases = ParityS3Diagnostics.run(server, problems); // [parity S3: diagnostics]
        int parityS4Cases = ParityS4Diagnostics.run(server, problems); // [parity S4: diagnostics]
        int parityT2Cases = ParityT2Diagnostics.run(server, problems); // [parity T2: diagnostics]
        int parityT1Cases = ParityT1Diagnostics.run(server, problems); // [parity T1: diagnostics]
        int parityU1Cases = ParityU1Diagnostics.run(server, problems); // [parity U1: diagnostics]
        int parityS1Cases = ParityS1Diagnostics.run(server, problems); // [parity S1: diagnostics]
        int parityT3Cases = ParityT3Diagnostics.run(server, problems); // [parity T3: diagnostics]
        int parityU3Cases = ParityU3Diagnostics.run(server, problems); // [parity U3: diagnostics]
        int parityV4Cases = ParityV4Diagnostics.run(server, problems); // [parity V4: diagnostics]
        int parityT4Cases = ParityT4Diagnostics.run(server, problems); // [parity T4: diagnostics]
        int parityU2Cases = ParityU2Diagnostics.run(server, problems); // [parity U2: diagnostics]
        int parityV2Cases = ParityV2Diagnostics.run(server, problems); // [parity V2: diagnostics]
        int parityV1Cases = ParityV1Diagnostics.run(server, problems); // [parity V1: diagnostics]
        int parityW1Cases = ParityW1Diagnostics.run(server, problems); // [parity W1: diagnostics]
        int parityX1Cases = ParityX1Diagnostics.run(server, problems); // [parity X1: diagnostics]
        int parityZ1Cases = ParityZ1Diagnostics.run(server, problems); // [parity z1: diagnostics]
        int parityA1Cases = ParityA1Diagnostics.run(server, problems); // [parity A1: diagnostics]
        int parityA3Cases = ParityA3Diagnostics.run(server, problems); // [parity A3: diagnostics]
        int parityB1Cases = ParityB1Diagnostics.run(server, problems); // [parity b1: diagnostics]
        int parityB2Cases = ParityB2Diagnostics.run(server, problems); // [parity b2: diagnostics]
        int parityA2Cases = ParityA2Diagnostics.run(server, problems); // [parity A2: diagnostics]
        int parityC1Cases = ParityC1Diagnostics.run(server, problems); // [parity c1: diagnostics]
        int parityD3Cases = ParityD3Diagnostics.run(server, problems); // [parity d3: diagnostics]
        int parityD1Cases = ParityD1Diagnostics.run(server, problems); // [parity D1: diagnostics]
        int parityD2Cases = ParityD2Diagnostics.run(server, problems) + ParityD2GunDiagnostics.run(server, problems)
                + ParityD2MiscDiagnostics.run(server, problems); // [parity d2: diagnostics]
        int parityE1Cases = ParityE1Diagnostics.run(server, problems); // [parity E1: diagnostics]
        int parityE2Cases = ParityE2Diagnostics.run(server, problems); // [parity e2: diagnostics]
        int parityE3Cases = ParityE3Diagnostics.run(server, problems); // [parity e3: diagnostics]
        List<String> report = new ArrayList<>();
        report.add("registered_source_block_families=" + registered);
        report.add("creative_tab_order=" + tabIds);
        report.add("creative_building_entries=" + entries.size());
        report.add("tested_tritium_container_kinds=" + containers);
        report.add("checked_source_crafting_recipes=" + checkedRecipes);
        report.add("checked_building_fitting_cases=" + fittingCases);
        report.add("checked_circuit_batch_cases=" + circuitCases);
        report.add("checked_gun_cases=" + gunCases);
        report.add("checked_parity_h1_cases=" + parityH1Cases); // [parity H1: diagnostics]
        report.add("checked_parity_i1_cases=" + parityI1Cases); // [parity I1: diagnostics]
        report.add("checked_parity_j1_cases=" + parityJ1Cases); // [parity J1: diagnostics]
        report.add("checked_parity_j2_cases=" + parityJ2Cases); // [parity j2: diagnostics]
        report.add("checked_parity_j1b_cases=" + parityJ1bCases); // [parity J1b]
        report.add("checked_parity_k1_cases=" + parityK1Cases); // [parity K1: diagnostics]
        report.add("checked_parity_k1b_cases=" + parityK1bCases); // [parity K1b]
        report.add("checked_parity_i2_cases=" + parityI2Cases); // [parity I2: diagnostics]
        report.add("checked_parity_i3_cases=" + parityI3Cases); // [parity I3: diagnostics]
        report.add("checked_parity_k2_cases=" + parityK2Cases); // [parity K2: diagnostics]
        report.add("checked_parity_k2_vanilla_cases=" + parityK2VanillaCases); // [parity K2: 1.7.10 vanilla sources]
        report.add("checked_parity_k3_cases=" + parityK3Cases); // [parity K3: diagnostics]
        report.add("checked_parity_l1_cases=" + parityL1Cases); // [parity L1: diagnostics]
        report.add("checked_parity_l2_cases=" + parityL2Cases); // [parity L2: diagnostics]
        report.add("checked_parity_l3_cases=" + parityL3Cases); // [parity L3: diagnostics]
        report.add("checked_parity_m1_cases=" + parityM1Cases); // [parity M1: diagnostics]
        report.add("checked_parity_m2_cases=" + parityM2Cases); // [parity M2: diagnostics]
        report.add("checked_parity_m3_cases=" + parityM3Cases); // [parity M3: diagnostics]
        report.add("checked_parity_m1_item_cases=" + parityM1ItemCases); // [parity m1: diagnostics]
        report.add("checked_parity_p1_cases=" + parityP1Cases); // [parity P1: diagnostics]
        report.add("checked_parity_p3_cases=" + parityP3Cases); // [parity P3: diagnostics]
        report.add("checked_parity_q2_cases=" + parityQ2Cases); // [parity Q2: diagnostics]
        report.add("checked_parity_p2_cases=" + parityP2Cases); // [parity P2: diagnostics]
        report.add("checked_parity_q4_cases=" + parityQ4Cases); // [parity Q4: diagnostics]
        report.add("checked_parity_r2_cases=" + parityR2Cases); // [parity R2: diagnostics]
        report.add("checked_parity_q3_cases=" + parityQ3Cases); // [parity Q3: diagnostics]
        report.add("checked_parity_q1_cases=" + parityQ1Cases); // [parity Q1: diagnostics]
        report.add("checked_parity_r1_cases=" + parityR1Cases); // [parity R1: diagnostics]
        report.add("checked_parity_r4_cases=" + parityR4Cases); // [parity R4: diagnostics]
        report.add("checked_parity_r3_cases=" + parityR3Cases); // [parity R3: diagnostics]
        report.add("checked_parity_s2_cases=" + parityS2Cases); // [parity S2: diagnostics]
        report.add("checked_parity_v3_cases=" + parityV3Cases); // [parity V3: diagnostics]
        report.add("checked_parity_s3_cases=" + parityS3Cases); // [parity S3: diagnostics]
        report.add("checked_parity_s4_cases=" + parityS4Cases); // [parity S4: diagnostics]
        report.add("checked_parity_t3_cases=" + parityT3Cases); // [parity T3: diagnostics]
        report.add("checked_parity_u3_cases=" + parityU3Cases); // [parity U3: diagnostics]
        report.add("checked_parity_v4_cases=" + parityV4Cases); // [parity V4: diagnostics]
        report.add("checked_parity_t2_cases=" + parityT2Cases); // [parity T2: diagnostics]
        report.add("checked_parity_t1_cases=" + parityT1Cases); // [parity T1: diagnostics]
        report.add("checked_parity_u1_cases=" + parityU1Cases); // [parity U1: diagnostics]
        report.add("checked_parity_s1_cases=" + parityS1Cases); // [parity S1: diagnostics]
        report.add("checked_parity_t4_cases=" + parityT4Cases); // [parity T4: diagnostics]
        report.add("checked_parity_u2_cases=" + parityU2Cases); // [parity U2: diagnostics]
        report.add("checked_parity_v2_cases=" + parityV2Cases); // [parity V2: diagnostics]
        report.add("checked_parity_v1_cases=" + parityV1Cases); // [parity V1: diagnostics]
        report.add("checked_parity_w1_cases=" + parityW1Cases); // [parity W1: diagnostics]
        report.add("checked_parity_x1_cases=" + parityX1Cases); // [parity X1: diagnostics]
        report.add("checked_parity_z1_cases=" + parityZ1Cases); // [parity z1: diagnostics]
        report.add("checked_parity_a1_cases=" + parityA1Cases); // [parity A1: diagnostics]
        report.add("checked_parity_a3_cases=" + parityA3Cases); // [parity A3: diagnostics]
        report.add("checked_parity_b1_cases=" + parityB1Cases); // [parity b1: diagnostics]
        report.add("checked_parity_b2_cases=" + parityB2Cases); // [parity b2: diagnostics]
        report.add("checked_parity_a2_cases=" + parityA2Cases); // [parity A2: diagnostics]
        report.add("checked_parity_c1_cases=" + parityC1Cases); // [parity c1: diagnostics]
        report.add("checked_parity_d3_cases=" + parityD3Cases); // [parity d3: diagnostics]
        report.add("checked_parity_d1_cases=" + parityD1Cases); // [parity D1: diagnostics]
        report.add("checked_parity_d2_cases=" + parityD2Cases); // [parity d2: diagnostics]
        report.add("checked_parity_e1_cases=" + parityE1Cases); // [parity E1: diagnostics]
        report.add("checked_parity_e2_cases=" + parityE2Cases); // [parity e2: diagnostics]
        report.add("checked_parity_e3_cases=" + parityE3Cases); // [parity e3: diagnostics]
        report.add("errors=" + problems.size());
        report.addAll(problems);
        Files.write(Path.of("building-audit.txt"), report, StandardCharsets.UTF_8);
        if (!problems.isEmpty()) throw new IllegalStateException("Building source parity failures: " + problems);
        AtomicLegacy.LOGGER.info("HBM_BUILDING_AUDIT_COMPLETE families={} entries={}", registered, entries.size());
        level.setBlockAndUpdate(BLAST_TEST.east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(BLAST_TEST.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(BLAST_TEST.below(2), Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(BLAST_TEST.below(3), Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(BLAST_TEST, HbmBlocks.NUKE_MAN.get().defaultBlockState());
        var bomb = (LegacyNukeBlockEntity) level.getBlockEntity(BLAST_TEST);
        for (int slot = 0; slot < LegacyNukeDefinition.MAN.slotCount(); slot++) {
            var item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(LegacyNukeDefinition.MAN.expected(slot)));
            bomb.setItem(slot, new ItemStack(item));
        }
        level.setBlockAndUpdate(BLAST_TEST.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        if (!level.isEmptyBlock(BLAST_TEST)) throw new IllegalStateException("Fat Man failed to detonate");
        blastTestTicks = 0;
    }

    @SubscribeEvent
    public static void checkBlast(ServerTickEvent.Post event) {
        if (blastTestTicks < 0) return;
        var server = event.getServer();
        var level = server.overworld();
        if (level.isEmptyBlock(BLAST_TEST.below()) && level.isEmptyBlock(BLAST_TEST.below(2))
                && level.getBlockState(BLAST_TEST.below(3)).is(Blocks.BEDROCK)) {
            AtomicLegacy.LOGGER.info("HBM_FAT_MAN_CRATER_PASS ticks={}", blastTestTicks);
            blastTestTicks = -1;
            server.halt(false);
        } else if (++blastTestTicks > 200) {
            throw new IllegalStateException("Fat Man failed to remove stone and dirt while preserving bedrock");
        }
    }

    private static int checkCircuitBatch(net.minecraft.server.MinecraftServer server, List<String> problems) throws Exception {
        try (var input = BuildingPortDiagnostics.class.getResourceAsStream("/legacy/atomiclegacy/circuit-batch.json")) {
            if (input == null) throw new IllegalStateException("Missing original circuit fixture");
            var fixture = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            List<ResourceLocation> expectedOrder = new ArrayList<>();
            for (var entry : fixture.getAsJsonArray("items")) {
                var id = ResourceLocation.parse("atomiclegacy:" + entry.getAsString());
                expectedOrder.add(id);
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    problems.add("Missing circuit " + id);
                    continue;
                }
                var stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                if (stack.getMaxStackSize() != 64 || stack.isDamageableItem()
                        || stack.getRarity() != net.minecraft.world.item.Rarity.COMMON) {
                    problems.add("Circuit source properties mismatch " + id);
                }
            }
            var actualOrder = HbmCreativeTabs.MATERIALS.get().getDisplayItems().stream()
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .filter(expectedOrder::contains).toList();
            if (!expectedOrder.equals(actualOrder)) problems.add("Circuit source creative order mismatch " + actualOrder);
            for (var entry : fixture.getAsJsonArray("crafting")) {
                var row = entry.getAsJsonObject();
                var id = ResourceLocation.parse("atomiclegacy:" + row.get("recipe").getAsString());
                var recipe = server.getRecipeManager().byKey(id);
                if (recipe.isEmpty()) {
                    problems.add("Missing circuit crafting recipe " + id);
                    continue;
                }
                var expected = row.getAsJsonObject("result");
                var result = recipe.get().value().getResultItem(server.registryAccess());
                if (!BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(expected.get("id").getAsString())
                        || result.getCount() != expected.get("count").getAsInt()) problems.add("Circuit crafting result mismatch " + id);
                for (var ingredient : recipe.get().value().getIngredients()) {
                    if (!ingredient.isEmpty() && ingredient.getItems().length == 0) problems.add("Empty circuit ingredient " + id);
                }
            }
            return expectedOrder.size() + fixture.getAsJsonArray("crafting").size() + 1;
        }
    }

    /** Bounds and support cases transcribed from the pinned BlockGrate/Chain/Scaffold/Sandbags sources. */
    private static void checkCrossingBeams(net.minecraft.server.level.ServerLevel level, BlockPos crossing, List<String> problems) {
        var blue = crossing.west(3);
        var green = crossing.north(3);
        var obstacle = crossing.east();
        try {
            level.setBlockAndUpdate(blue, HbmBlocks.LAMP_TRITIUM_BLUE_ON.get().defaultBlockState());
            level.setBlockAndUpdate(green, HbmBlocks.LAMP_TRITIUM_GREEN_ON.get().defaultBlockState());
            checkBeamColor(level, crossing, SpotlightBeamBlock.BLUE, problems);
            checkBeamColor(level, crossing.south(), SpotlightBeamBlock.GREEN, problems);
            if (!level.getBlockState(crossing).is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES)) {
                problems.add("Spotlight beam obstructs tree growth");
            }
            level.setBlockAndUpdate(obstacle, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            if (!level.isEmptyBlock(obstacle.east())) problems.add("Obstructed blue beam continued past stone");
            level.removeBlock(obstacle, false);
            checkBeamColor(level, obstacle.east(), SpotlightBeamBlock.BLUE, problems);
            level.removeBlock(blue, false);
            checkBeamColor(level, crossing, SpotlightBeamBlock.BLUE, problems);
            level.removeBlock(green, false);
            if (!level.isEmptyBlock(crossing)) problems.add("Beam survived removal of both sources");
        } finally {
            level.removeBlock(blue, false);
            level.removeBlock(green, false);
            level.removeBlock(obstacle, false);
        }
    }

    private static void checkBeamColor(net.minecraft.server.level.ServerLevel level, BlockPos pos, int expected, List<String> problems) {
        var state = level.getBlockState(pos);
        if (!state.is(HbmBlocks.SPOTLIGHT_BEAM.get()) || state.getValue(SpotlightBeamBlock.COLOR) != expected) {
            problems.add("Expected beam color " + expected + " at " + pos + ", got " + state);
        }
    }

    private static int checkBuildingFittings(net.minecraft.server.level.ServerLevel level, BlockPos pos, List<String> problems) {
        int cases = 0;
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var oldHere = level.getBlockState(pos);
        var oldAbove = level.getBlockState(pos.above());
        var oldBelow = level.getBlockState(pos.below());
        try {
            // Skip neighbor shape updates while arranging isolated source fixtures.
            level.setBlock(pos, air, 18);
            for (var holder : List.of(HbmBlocks.STEEL_GRATE, HbmBlocks.STEEL_GRATE_WIDE)) {
                boolean wide = holder == HbmBlocks.STEEL_GRATE_WIDE;
                var item = new net.minecraft.world.entity.item.ItemEntity(level, 0, 0, 0, new ItemStack(Items.STONE));
                var xp = new net.minecraft.world.entity.ExperienceOrb(level, 0, 0, 0, 1);
                for (int height = 0; height < 10; height++) {
                    var state = holder.get().defaultBlockState().setValue(com.atomiclegacy.blocks.SteelGrateBlock.HEIGHT, height);
                    double y = height == 9 ? -0.125 : height * 0.125;
                    var expected = net.minecraft.world.phys.shapes.Shapes.box(0, y, 0, 1, y + 0.125 - (wide ? 0.001F : 0), 1);
                    checkShape("grate " + holder.getId() + " height=" + height, expected, state.getShape(level, pos), problems);
                    checkShape("grate collision " + holder.getId() + " height=" + height, expected, state.getCollisionShape(level, pos), problems);
                    for (var face : net.minecraft.core.Direction.values()) {
                        boolean expectedSolid = height == 0 && face == net.minecraft.core.Direction.DOWN
                                || height == 7 && face == net.minecraft.core.Direction.UP;
                        if (state.isFaceSturdy(level, pos, face) != expectedSolid) problems.add("Grate support mismatch " + holder.getId() + " height=" + height + " face=" + face);
                    }
                    for (var entity : List.<net.minecraft.world.entity.Entity>of(item, xp)) {
                        var actual = state.getCollisionShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.of(entity));
                        checkShape("grate item/XP " + holder.getId() + " height=" + height,
                                wide ? net.minecraft.world.phys.shapes.Shapes.empty() : expected, actual, problems);
                        if (wide) {
                            double startY = pos.getY() + height * 0.125 + 0.25;
                            entity.setPos(pos.getX() + 0.5, startY, pos.getZ() + 0.5);
                            entity.setDeltaMovement(1, 1, 1);
                            state.entityInside(level, pos, entity);
                            if (Math.abs(entity.getY() - (startY - 0.125)) > 1E-6
                                    || !entity.getDeltaMovement().equals(new net.minecraft.world.phys.Vec3(0, -0.25, 0))) {
                                problems.add("Wide grate item/XP fall mismatch height=" + height);
                            }
                        }
                    }
                    cases++;
                }
                for (int height : new int[]{8, 9}) {
                    var adjacent = height == 9 ? pos.below() : pos.above();
                    var state = holder.get().defaultBlockState().setValue(com.atomiclegacy.blocks.SteelGrateBlock.HEIGHT, height);
                    level.setBlock(adjacent, air, 18);
                    level.setBlock(pos, state, 18);
                    level.neighborChanged(state, pos, stone.getBlock(), adjacent, false);
                    if (!level.getBlockState(pos).equals(state)) problems.add("Offset grate broke beside air " + holder.getId() + " height=" + height);
                    level.setBlock(adjacent, stone, 18);
                    level.neighborChanged(state, pos, stone.getBlock(), adjacent, false);
                    if (!level.getBlockState(pos).isAir()) problems.add("Offset grate survived obstructing block " + holder.getId() + " height=" + height);
                    level.setBlock(adjacent, air, 18);
                    cases++;
                }
            }
            level.setBlock(pos, air, 18);
            var chain = HbmBlocks.DUNGEON_CHAIN.get().defaultBlockState();
            var orientations = new ArrayList<net.minecraft.world.level.block.state.BlockState>();
            orientations.add(chain);
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                orientations.add(chain.setValue(com.atomiclegacy.blocks.DungeonChainBlock.WALL, true)
                        .setValue(com.atomiclegacy.blocks.DungeonChainBlock.FACING, direction));
            }
            var supports = new ArrayList<>(orientations);
            supports.add(air);
            supports.add(stone);
            for (int index = 0; index < orientations.size(); index++) {
                var state = orientations.get(index);
                if (!state.isLadder(level, pos, null)) problems.add("Chain is not climbable orientation=" + index);
                for (int above = 0; above < supports.size(); above++) {
                    level.setBlock(pos.above(), supports.get(above), 18);
                    boolean expectedSurvival = above == index || (index == 0 && above == 6);
                    if (state.canSurvive(level, pos) != expectedSurvival) problems.add("Chain support orientation=" + index + " above=" + above);
                    for (int below = 0; below < supports.size(); below++) {
                        level.setBlock(pos.below(), supports.get(below), 18);
                        double minY = below == index || below == 6 ? 0 : 0.25;
                        if (Math.abs(state.getShape(level, pos).bounds().minY - minY) > 1E-6) {
                            problems.add("Chain end bounds orientation=" + index + " below=" + below);
                        }
                        if (!state.getCollisionShape(level, pos).isEmpty()) problems.add("Chain has collision");
                        cases++;
                    }
                }
            }
            var trapdoor = HbmBlocks.TRAPDOOR_STEEL.get().defaultBlockState();
            for (var below : List.of(chain, HbmBlocks.LADDER_STEEL.get().defaultBlockState(), stone, air)) {
                level.setBlock(pos.below(), below, 18);
                for (boolean open : List.of(false, true)) {
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        var state = trapdoor.setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, open)
                                .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, direction);
                        boolean climbable = open && (below.is(chain.getBlock()) || below.is(HbmBlocks.LADDER_STEEL.get()));
                        if (state.isLadder(level, pos, null) != climbable) problems.add("Trapdoor climbability over " + below + " open=" + open);
                        if (climbable) {
                            var bounds = state.getCollisionShape(level, pos).bounds();
                            double thickness = direction.getAxis() == net.minecraft.core.Direction.Axis.X ? bounds.getXsize() : bounds.getZsize();
                            if (Math.abs(thickness - 0.125) > 1E-6) problems.add("Trapdoor ladder collision thickness " + direction);
                        }
                        cases++;
                    }
                }
            }
            for (var holder : List.of(HbmBlocks.STEEL_SCAFFOLD, HbmBlocks.STEEL_SCAFFOLD_RED,
                    HbmBlocks.STEEL_SCAFFOLD_WHITE, HbmBlocks.STEEL_SCAFFOLD_YELLOW)) {
                for (var orientation : com.atomiclegacy.blocks.SteelScaffoldBlock.Orientation.values()) {
                    var state = holder.get().defaultBlockState().setValue(com.atomiclegacy.blocks.SteelScaffoldBlock.ORIENT, orientation);
                    var expected = switch (orientation) {
                        case HORIZONTAL_NS -> Block.box(0, 0, 2, 16, 16, 14);
                        case HORIZONTAL_EW -> Block.box(2, 0, 0, 14, 16, 16);
                        case VERTICAL_NS, VERTICAL_EW -> Block.box(0, 2, 0, 16, 14, 16);
                    };
                    checkShape("scaffold " + holder.getId() + " " + orientation, expected, state.getCollisionShape(level, pos), problems);
                    cases++;
                }
            }
            for (int mask = 0; mask < 16; mask++) {
                var state = HbmBlocks.SANDBAGS.get().defaultBlockState()
                        .setValue(com.atomiclegacy.blocks.SandbagsBlock.NORTH, (mask & 1) != 0)
                        .setValue(com.atomiclegacy.blocks.SandbagsBlock.SOUTH, (mask & 2) != 0)
                        .setValue(com.atomiclegacy.blocks.SandbagsBlock.WEST, (mask & 4) != 0)
                        .setValue(com.atomiclegacy.blocks.SandbagsBlock.EAST, (mask & 8) != 0);
                var expected = Block.box((mask & 4) != 0 ? 0 : 4, 0, (mask & 1) != 0 ? 0 : 4,
                        (mask & 8) != 0 ? 16 : 12, 16, (mask & 2) != 0 ? 16 : 12);
                checkShape("sandbags mask=" + mask, expected, state.getCollisionShape(level, pos), problems);
                cases++;
            }
            var silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
            var fortuneTool = new ItemStack(Items.DIAMOND_PICKAXE);
            var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            silkTool.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
            fortuneTool.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
            for (var glass : List.of(HbmBlocks.REINFORCED_GLASS, HbmBlocks.REINFORCED_LAMINATE,
                    HbmBlocks.GLASS_QUARTZ, HbmBlocks.GLASS_ASH, HbmBlocks.GLASS_BORON, HbmBlocks.GLASS_LEAD,
                    HbmBlocks.GLASS_POLARIZED, HbmBlocks.GLASS_POLONIUM, HbmBlocks.GLASS_TRINITITE, HbmBlocks.GLASS_URANIUM)) {
                for (var tool : List.of(ItemStack.EMPTY, silkTool, fortuneTool)) {
                    boolean expectDrop = tool == silkTool || glass == HbmBlocks.REINFORCED_LAMINATE || glass == HbmBlocks.GLASS_QUARTZ;
                    var drops = Block.getDrops(glass.get().defaultBlockState(), level, pos, null, null, tool);
                    if (expectDrop ? drops.size() != 1 || !drops.getFirst().is(glass.get().asItem()) || drops.getFirst().getCount() != 1 : !drops.isEmpty()) {
                        problems.add("Glass source drop mismatch " + glass.getId() + " tool=" + tool);
                    }
                    cases++;
                }
            }
            for (var pane : List.of(HbmBlocks.REINFORCED_GLASS_PANE, HbmBlocks.REINFORCED_LAMINATE_PANE)) {
                for (var glass : List.of(HbmBlocks.REINFORCED_GLASS, HbmBlocks.REINFORCED_LAMINATE,
                        HbmBlocks.GLASS_QUARTZ, HbmBlocks.GLASS_ASH, HbmBlocks.GLASS_BORON, HbmBlocks.GLASS_LEAD, HbmBlocks.GLASS_POLARIZED,
                        HbmBlocks.GLASS_POLONIUM, HbmBlocks.GLASS_TRINITITE, HbmBlocks.GLASS_URANIUM)) {
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        var property = switch (direction) {
                            case NORTH -> net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
                            case SOUTH -> net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
                            case WEST -> net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
                            default -> net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
                        };
                        var state = pane.get().defaultBlockState().updateShape(direction, glass.get().defaultBlockState(), level, pos, pos.relative(direction));
                        if (!state.getValue(property)) problems.add("Pane does not connect to " + glass.getId());
                        state = state.updateShape(direction, air, level, pos, pos.relative(direction));
                        if (state.getValue(property)) problems.add("Pane retains removed connection " + direction);
                        cases++;
                    }
                }
            }
        } finally {
            level.setBlock(pos, oldHere, 18);
            level.setBlock(pos.above(), oldAbove, 18);
            level.setBlock(pos.below(), oldBelow, 18);
        }
        return cases;
    }

    private static void checkShape(String label, net.minecraft.world.phys.shapes.VoxelShape expected,
                                   net.minecraft.world.phys.shapes.VoxelShape actual, List<String> problems) {
        if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(expected, actual, net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME)) {
            problems.add("Source shape mismatch " + label);
        }
    }

}
