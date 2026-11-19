package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.FusionMachineBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.inventory.recipes.LegacyCraftBookRecipes;
import com.atomiclegacy.inventory.recipes.RbmkFuelDisassemblyRecipe;
import com.atomiclegacy.items.LegacyVariantBlockItem;
import com.atomiclegacy.items.RbmkFuelRodItem;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.items.ToolConversion;
import com.atomiclegacy.items.UniversalGrenadeItem;
import com.atomiclegacy.machine.AnnihilatorRecipes;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.HbmStructureLoot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [parity S3: JEI] Recipe lists of the 1.7.10 NEI handlers that had no JEI counterpart.
 * Free of JEI and client classes: ParityS3Diagnostics calls the same methods on the dedicated server.
 */
public final class ParityS3JeiRecipes {
    private ParityS3JeiRecipes() {
    }

    /** AnnihilatorHandler: AnnihilatorRecipes#getRecipes, the milestone as a red lore line. */
    public static List<UniversalJeiRecipe> annihilator() {
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        for (AnnihilatorRecipes.DisplayEntry entry : AnnihilatorRecipes.displayEntries()) {
            Component line = Component.literal(String.format(Locale.US, "%,d", entry.amount())).withStyle(ChatFormatting.RED);
            recipes.add(new UniversalJeiRecipe(
                    List.of(UniversalJeiRecipe.Slot.of(entry.inputs(), List.of(line))),
                    List.of(UniversalJeiRecipe.Slot.of(entry.payout())),
                    List.of(block(HbmBlocks.MACHINE_ANNIHILATOR.get()))));
        }
        return recipes;
    }

    /** AshpitHandler#getRecipes. */
    public static List<UniversalJeiRecipe> ashpit() {
        List<ItemStack> ovens = List.of(block(HbmBlocks.HEATER_FIREBOX.get()), block(HbmBlocks.HEATER_OVEN.get()));
        List<ItemStack> chimneys = List.of(block(HbmBlocks.CHIMNEY_BRICK.get()), block(HbmBlocks.CHIMNEY_INDUSTRIAL.get()));
        List<ItemStack> coals = List.of(new ItemStack(Items.COAL), item("lignite"), item("coke_coal"));
        List<ItemStack> wood = List.of(new ItemStack(Items.OAK_LOG), new ItemStack(Items.ACACIA_LOG), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_SAPLING), new ItemStack(Items.CHARCOAL));
        List<ItemStack> misc = List.of(item("solid_fuel"), item("scrap"), item("dust"), item("rocket_fuel"));
        List<ItemStack> machine = List.of(block(HbmBlocks.MACHINE_ASHPIT.get()));
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(ovens), UniversalJeiRecipe.Slot.of(coals)), List.of(UniversalJeiRecipe.Slot.of(item("powder_ash_coal"))), machine));
        recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(ovens), UniversalJeiRecipe.Slot.of(wood)), List.of(UniversalJeiRecipe.Slot.of(item("powder_ash_wood"))), machine));
        recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(ovens), UniversalJeiRecipe.Slot.of(misc)), List.of(UniversalJeiRecipe.Slot.of(item("powder_ash_misc"))), machine));
        for (String smoke : new String[] {"smoke", "smoke_leaded", "smoke_poison"}) {
            HbmFluidDefinition fluid = fluid(smoke);
            recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(chimneys), UniversalJeiRecipe.Slot.fluid(fluid, 2_000)),
                    List.of(UniversalJeiRecipe.Slot.of(item("powder_ash_fly"))), machine));
            recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.CHIMNEY_INDUSTRIAL.get())), UniversalJeiRecipe.Slot.fluid(fluid, 8_000)),
                    List.of(UniversalJeiRecipe.Slot.of(item("powder_ash_soot"))), machine));
        }
        return recipes;
    }

    /** DeuteriumHandler#generateRecipes. */
    public static List<UniversalJeiRecipe> deuterium() {
        return List.of(new UniversalJeiRecipe(
                List.of(UniversalJeiRecipe.Slot.fluid(fluid("water"), 1_000)),
                List.of(UniversalJeiRecipe.Slot.fluid(fluid("heavywater"), 20)),
                List.of(block(HbmBlocks.MACHINE_DEUTERIUM_EXTRACTOR.get()), block(HbmBlocks.MACHINE_DEUTERIUM_TOWER.get()))));
    }

    /** ToolingHandler: BlockToolConversion#getRecipes, machine = the tool type's display stacks. */
    public static List<UniversalJeiRecipe> tooling() {
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        for (ToolConversion.Display display : ToolConversion.displays()) {
            List<ItemStack> tools = display.tool() == ToolConversion.Tool.TORCH
                    ? List.of(new ItemStack(HbmItems.BLOWTORCH.get()), new ItemStack(HbmItems.ACETYLENE_TORCH.get()))
                    : List.of(new ItemStack(HbmItems.BOLTGUN.get()));
            recipes.add(new UniversalJeiRecipe(display.inputs().stream().map(UniversalJeiRecipe.Slot::of).toList(),
                    List.of(UniversalJeiRecipe.Slot.of(display.output())), tools));
        }
        return recipes;
    }

    public static List<ItemStack> toolingMachines() {
        return List.of(new ItemStack(HbmItems.BOLTGUN.get()), new ItemStack(HbmItems.BLOWTORCH.get()), new ItemStack(HbmItems.ACETYLENE_TORCH.get()));
    }

    public static List<ItemStack> constructionMachines() {
        return List.of(new ItemStack(HbmItems.ACETYLENE_TORCH.get()), new ItemStack(HbmItems.BLOWTORCH.get()), new ItemStack(HbmItems.BOLTGUN.get()));
    }

    /** ConstructionHandler#getRecipes: bill of materials -> multiblock, machine = the structure core. */
    public static List<UniversalJeiRecipe> construction() {
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        recipes.add(new UniversalJeiRecipe(slots(
                count(block(HbmBlocks.WATZ_END.get()), 48), item("bolt_dura_steel", 64), item("bolt_dura_steel", 64), item("bolt_dura_steel", 64),
                count(block(HbmBlocks.WATZ_ELEMENT.get()), 36), count(block(HbmBlocks.WATZ_COOLER.get()), 26), new ItemStack(HbmItems.BOLTGUN.get())),
                List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.WATZ.get()))), List.of(block(HbmBlocks.STRUCT_WATZ_CORE.get()))));
        recipes.add(new UniversalJeiRecipe(slots(count(block(HbmBlocks.STRUCT_LAUNCHER.get()), 8)),
                List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.COMPACT_LAUNCHER.get()))), List.of(block(HbmBlocks.STRUCT_LAUNCHER_CORE.get()))));
        recipes.add(new UniversalJeiRecipe(slots(count(block(HbmBlocks.STRUCT_LAUNCHER.get()), 16), count(block(HbmBlocks.STRUCT_LAUNCHER.get()), 64),
                count(block(HbmBlocks.STRUCT_SCAFFOLD.get()), 11)),
                List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.LAUNCH_TABLE.get()))), List.of(block(HbmBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()))));
        recipes.add(new UniversalJeiRecipe(slots(count(block(HbmBlocks.STRUCT_LAUNCHER.get()), 30), count(block(HbmBlocks.STRUCT_LAUNCHER.get()), 384),
                count(block(HbmBlocks.STRUCT_SCAFFOLD.get()), 63), count(block(HbmBlocks.STRUCT_SCAFFOLD.get()), 384),
                count(block(HbmBlocks.CONCRETE_SMOOTH.get()), 38), count(block(HbmBlocks.CONCRETE_SMOOTH.get()), 320)),
                List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.SOYUZ_LAUNCHER.get()))), List.of(block(HbmBlocks.STRUCT_SOYUZ_CORE.get()))));
        List<UniversalJeiRecipe.Slot> icf = new ArrayList<>(slots(
                variant(HbmBlocks.ICF_COMPONENT.get(), 0, 50), variant(HbmBlocks.ICF_COMPONENT.get(), 3, 240), item("bolt_dura_steel", 960),
                item("plate_cast_steel", 240), variant(HbmBlocks.ICF_COMPONENT.get(), 1, 117)));
        icf.add(labelled(List.of(item("plate_cast_bismuth_bronze", 117), item("plate_cast_arsenic_bronze", 117))));
        icf.addAll(slots(new ItemStack(HbmItems.BLOWTORCH.get()), new ItemStack(HbmItems.BOLTGUN.get())));
        recipes.add(new UniversalJeiRecipe(icf, List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.ICF.get()))), List.of(block(HbmBlocks.STRUCT_ICF_CORE.get()))));
        recipes.add(new UniversalJeiRecipe(torusItems(), List.of(UniversalJeiRecipe.Slot.of(block(HbmBlocks.FUSION_TORUS.get()))),
                List.of(block(HbmBlocks.STRUCT_TORUS_CORE.get()))));
        return recipes;
    }

    private static List<UniversalJeiRecipe.Slot> torusItems() {
        int wallCount = 0;
        int blanketCount = 0;
        int pipeCount = -1; // one block is replaced by the core
        for (int iy = 0; iy < 5; iy++) {
            int l = iy > 2 ? 4 - iy : iy;
            int[][] layer = FusionMachineBlock.TORUS_LAYOUT[l];
            for (int[] row : layer) {
                for (int meta : row) {
                    if (meta == 1) wallCount++;
                    if (meta == 2) blanketCount++;
                    if (meta == 3) pipeCount++;
                }
            }
        }
        List<ItemStack> torus = new ArrayList<>();
        int plateCount = wallCount;
        while (wallCount > 0) { int a = Math.min(wallCount, 256); torus.add(variant(HbmBlocks.FUSION_COMPONENT.get(), 0, a)); wallCount -= a; }
        while (plateCount > 0) { int a = Math.min(plateCount, 256); torus.add(item("plate_cast_steel", a)); plateCount -= a; }
        while (blanketCount > 0) { int a = Math.min(blanketCount, 256); torus.add(variant(HbmBlocks.FUSION_COMPONENT.get(), 2, a)); blanketCount -= a; }
        while (pipeCount > 0) { int a = Math.min(pipeCount, 256); torus.add(variant(HbmBlocks.FUSION_COMPONENT.get(), 3, a)); pipeCount -= a; }
        torus.add(new ItemStack(HbmItems.BLOWTORCH.get()));
        return slots(torus.toArray(ItemStack[]::new));
    }

    /** RBMKRodDisassemblyHandler: every craftable rod at enrichment 0..4, plus the xenon-poisoned band. */
    public static List<UniversalJeiRecipe> rbmkDisassembly() {
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        for (var holder : HbmItems.RBMK_FUEL_ROD_ITEMS) {
            if (!(holder.get() instanceof RbmkFuelRodItem rod) || !RbmkFuelRodItem.FUEL_IDS.contains(rod.fuelId(new ItemStack(rod)))) {
                continue;
            }
            for (int enrichment = 0; enrichment <= 4; enrichment++) {
                for (boolean poison : new boolean[] {false, true}) {
                    ItemStack input = new ItemStack(rod);
                    double yield = Math.min(1.0D - enrichment / 5.0D, 0.99D);
                    RbmkFuelRodItem.setState(input, 1.0D - yield, poison ? 50.0D : 0.0D, 20.0D, 20.0D);
                    ItemStack result = RbmkFuelDisassemblyRecipe.displayResult(input);
                    if (result.isEmpty()) {
                        continue;
                    }
                    ItemStack plain = new ItemStack(rod);
                    RbmkFuelRodItem.setState(plain, 1.0D - yield, 0.0D, 20.0D, 20.0D);
                    // ItemRBMKRod pellets without xenon only get the clean entry.
                    if (poison && ItemStack.isSameItemSameComponents(result, RbmkFuelDisassemblyRecipe.displayResult(plain))) {
                        continue;
                    }
                    recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(input)), List.of(UniversalJeiRecipe.Slot.of(result)),
                            List.of(new ItemStack(Blocks.CRAFTING_TABLE))));
                }
            }
        }
        return recipes;
    }

    /** RTGRecipeHandler: ItemRTGPellet#getRecipeMap; registered only when VersatileConfig.rtgDecay(). */
    public static List<UniversalJeiRecipe> rtg() {
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        if (!HbmConfig.rtgDecay()) {
            return recipes;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof RtgPelletItem pellet && AtomicLegacy.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                recipes.add(new UniversalJeiRecipe(List.of(UniversalJeiRecipe.Slot.of(new ItemStack(pellet))),
                        List.of(UniversalJeiRecipe.Slot.of(pellet.decayResult())), rtgMachines()));
            }
        }
        return recipes;
    }

    /** machine_difurnace_rtg_off is not part of the port; machine_rtg ("machine_rtg_grey") is. */
    public static List<ItemStack> rtgMachines() {
        return List.of(block(HbmBlocks.MACHINE_RTG_GREY.get()));
    }

    /** GrenadeRecipeHandler: every compatible shell/filling/fuze, without and with each extra. */
    public static List<UniversalJeiRecipe> grenade() {
        List<UniversalJeiRecipe> recipes = new ArrayList<>();
        for (UniversalGrenadeItem.Shell shell : UniversalGrenadeItem.Shell.values()) {
            for (UniversalGrenadeItem.Filling filling : UniversalGrenadeItem.Filling.values()) {
                if (!filling.supports(shell)) continue;
                for (UniversalGrenadeItem.Fuze fuze : UniversalGrenadeItem.Fuze.values()) {
                    recipes.add(grenade(shell, filling, fuze, null));
                    for (UniversalGrenadeItem.Extra extra : UniversalGrenadeItem.Extra.values()) {
                        recipes.add(grenade(shell, filling, fuze, extra));
                    }
                }
            }
        }
        return recipes;
    }

    private static UniversalJeiRecipe grenade(UniversalGrenadeItem.Shell shell, UniversalGrenadeItem.Filling filling,
                                              UniversalGrenadeItem.Fuze fuze, UniversalGrenadeItem.Extra extra) {
        List<UniversalJeiRecipe.Slot> ins = new ArrayList<>();
        ins.add(UniversalJeiRecipe.Slot.of(HbmItems.variantStack(HbmItems.GRENADE_SHELL_ITEMS, shell.id())));
        ins.add(UniversalJeiRecipe.Slot.of(HbmItems.variantStack(HbmItems.GRENADE_FILLING_ITEMS, filling.id())));
        ins.add(UniversalJeiRecipe.Slot.of(HbmItems.variantStack(HbmItems.GRENADE_FUZE_ITEMS, fuze.id())));
        if (extra != null) {
            ins.add(UniversalJeiRecipe.Slot.of(HbmItems.variantStack(HbmItems.GRENADE_EXTRA_ITEMS, extra.id())));
        }
        return new UniversalJeiRecipe(ins, List.of(UniversalJeiRecipe.Slot.of(UniversalGrenadeItem.make(shell, filling, fuze, extra))),
                List.of(new ItemStack(Blocks.CRAFTING_TABLE)));
    }

    /** SatelliteHandler: one mining satellite and its ItemPool with "chance%" red lore lines. */
    public record SatelliteDisplay(ItemStack satellite, List<ItemStack> outputs, List<Component> chances) {
    }

    public static List<SatelliteDisplay> satellites() {
        List<SatelliteDisplay> displays = new ArrayList<>();
        // SatelliteHandler#getMiningSatellites order; SatelliteMiner / SatelliteLunarMiner cargo pools.
        Object[][] satellites = {
                {HbmItems.SATELLITE_MINER_ASTRO.get(), "POOL_SAT_MINER"},
                {HbmItems.SATELLITE_MINER_LUNAR.get(), "POOL_SAT_LUNAR"},
                {HbmItems.SAT_MINER.get(), "POOL_SAT_MINER"},
                {HbmItems.SAT_LUNAR_MINER.get(), "POOL_SAT_LUNAR"},
        };
        for (Object[] satellite : satellites) {
            List<Map.Entry<ItemStack, Integer>> pool = HbmStructureLoot.displayEntries((String) satellite[1]);
            int weight = pool.stream().mapToInt(Map.Entry::getValue).sum();
            List<ItemStack> outputs = new ArrayList<>();
            List<Component> chances = new ArrayList<>();
            for (Map.Entry<ItemStack, Integer> entry : pool) {
                float chance = 100F * entry.getValue() / weight;
                outputs.add(entry.getKey());
                chances.add(Component.literal(((int) (chance * 10F) / 10F) + "%").withStyle(ChatFormatting.RED));
            }
            displays.add(new SatelliteDisplay(new ItemStack((Item) satellite[0]), outputs, chances));
        }
        return displays;
    }

    /** BookRecipeHandler ("Black Book"): MagicRecipes. */
    public static List<LegacyCraftBookRecipes.MagicDisplay> book() {
        return LegacyCraftBookRecipes.magicDisplays();
    }

    /** CrucibleSmeltingHandler: input -> material scraps (liquid icon) the crucible would melt it into. */
    public record CrucibleSmeltingDisplay(List<ItemStack> inputs, List<ItemStack> outputs) {
    }

    /**
     * CrucibleRecipes#getSmeltingRecipes was keyed by ore dictionary name; without the ore dictionary every
     * candidate stack is evaluated and stacks melting into identical material lists share one recipe.
     */
    public static List<CrucibleSmeltingDisplay> crucibleSmelting(Collection<ItemStack> candidates) {
        Map<List<FoundryMaterialStack>, List<ItemStack>> grouped = new LinkedHashMap<>();
        for (ItemStack candidate : candidates) {
            if (candidate.isEmpty()) continue;
            ItemStack single = candidate.copyWithCount(1);
            List<FoundryMaterialStack> materials = FoundryMaterial.smeltingMaterialsFromItem(single);
            if (materials.isEmpty() || materials.stream().allMatch(stack -> stack.amount() <= 0)) continue;
            grouped.computeIfAbsent(List.copyOf(materials), key -> new ArrayList<>()).add(single);
        }
        List<CrucibleSmeltingDisplay> displays = new ArrayList<>();
        grouped.forEach((materials, inputs) -> displays.add(new CrucibleSmeltingDisplay(List.copyOf(inputs),
                materials.stream().map(stack -> ScrapsItem.create(stack, true)).toList())));
        return displays;
    }

    public static List<ItemStack> defaultStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) stacks.add(new ItemStack(item));
        }
        return stacks;
    }

    // ---------------------------------------------------------------------------------------------

    private static List<UniversalJeiRecipe.Slot> slots(ItemStack... stacks) {
        List<UniversalJeiRecipe.Slot> slots = new ArrayList<>();
        for (ItemStack stack : stacks) {
            slots.add(labelled(List.of(stack)));
        }
        return slots;
    }

    /** ItemStackUtil#addStackSizeLabel: "§c<stacks>x64[ + <items>]" for stacks above 64. */
    private static UniversalJeiRecipe.Slot labelled(List<ItemStack> stacks) {
        int size = stacks.isEmpty() ? 0 : stacks.getFirst().getCount();
        if (size <= 64) {
            return UniversalJeiRecipe.Slot.of(stacks);
        }
        int full = size / 64;
        int rest = size % 64;
        return UniversalJeiRecipe.Slot.of(stacks, List.of(Component.literal(full + "x64" + (rest > 0 ? " + " + rest : ""))
                .withStyle(ChatFormatting.RED)));
    }

    private static ItemStack block(Block block) {
        return new ItemStack(block);
    }

    private static ItemStack count(ItemStack stack, int count) {
        stack.setCount(count);
        return stack;
    }

    private static ItemStack variant(Block block, int variant, int count) {
        ItemStack stack = block.asItem() instanceof LegacyVariantBlockItem item
                ? LegacyVariantBlockItem.stackFor(item, variant) : new ItemStack(block);
        stack.setCount(count);
        return stack;
    }

    private static ItemStack item(String path) {
        return item(path, 1);
    }

    private static ItemStack item(String path, int count) {
        Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(path));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow(() -> new IllegalStateException("Missing HBM fluid " + name));
    }
}
