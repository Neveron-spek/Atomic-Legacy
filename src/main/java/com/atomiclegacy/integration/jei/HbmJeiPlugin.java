package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.gui.ArcWelderScreen;
import com.atomiclegacy.inventory.gui.ArcFurnaceScreen;
import com.atomiclegacy.inventory.gui.AmmoPressScreen;
import com.atomiclegacy.inventory.gui.CompressorScreen;
import com.atomiclegacy.inventory.gui.CyclotronScreen;
import com.atomiclegacy.inventory.gui.ExposureChamberScreen;
import com.atomiclegacy.inventory.gui.FusionMachineScreen;
import com.atomiclegacy.inventory.gui.HydrotreaterScreen;
import com.atomiclegacy.inventory.gui.IronFurnaceScreen;
import com.atomiclegacy.inventory.gui.MixerScreen;
import com.atomiclegacy.inventory.gui.ParticleAcceleratorScreen;
import com.atomiclegacy.inventory.gui.PrecisionAssemblerScreen;
import com.atomiclegacy.inventory.gui.PyroOvenScreen;
import com.atomiclegacy.inventory.gui.RockMillScreen;
import com.atomiclegacy.inventory.gui.SteelFurnaceScreen;
import com.atomiclegacy.inventory.gui.SupercomputerScreen;
import com.atomiclegacy.inventory.recipes.ArcWelderRecipe;
import com.atomiclegacy.inventory.recipes.ArcFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.AmmoPressRecipe;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
import com.atomiclegacy.inventory.recipes.BlastFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.CentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
import com.atomiclegacy.inventory.recipes.CokerRecipe;
import com.atomiclegacy.inventory.recipes.CombinationOvenRecipe;
import com.atomiclegacy.inventory.recipes.CompressorRecipe;
import com.atomiclegacy.inventory.recipes.CrackingRecipe;
import com.atomiclegacy.inventory.recipes.CrystallizerRecipe;
import com.atomiclegacy.inventory.recipes.CrucibleRecipe;
import com.atomiclegacy.inventory.recipes.CyclotronRecipe;
import com.atomiclegacy.inventory.recipes.ElectrolyzerFluidRecipe;
import com.atomiclegacy.inventory.recipes.ElectrolyzerMetalRecipe;
import com.atomiclegacy.inventory.recipes.ExposureChamberRecipe;
import com.atomiclegacy.inventory.recipes.FractionTowerRecipe;
import com.atomiclegacy.inventory.recipes.FuelPoolRecipe;
import com.atomiclegacy.inventory.recipes.FusionBreederFluidRecipe;
import com.atomiclegacy.inventory.recipes.FusionRecipe;
import com.atomiclegacy.inventory.recipes.GasCentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.LiquefactionRecipe;
import com.atomiclegacy.inventory.recipes.MixerRecipe;
import com.atomiclegacy.inventory.recipes.ParticleAcceleratorRecipe;
import com.atomiclegacy.inventory.recipes.PlasmaForgeRecipe;
import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;
import com.atomiclegacy.inventory.recipes.PressRecipe;
import com.atomiclegacy.inventory.recipes.PyroOvenRecipe;
import com.atomiclegacy.inventory.recipes.PurexRecipe;
import com.atomiclegacy.inventory.recipes.RefineryRecipe;
import com.atomiclegacy.inventory.recipes.ReformingRecipe;
import com.atomiclegacy.inventory.recipes.HydrotreatingRecipe;
import com.atomiclegacy.inventory.recipes.RbmkOutgasserRecipe;
import com.atomiclegacy.inventory.recipes.RotaryFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;
import com.atomiclegacy.inventory.recipes.SupercomputerRecipe;
import com.atomiclegacy.inventory.recipes.SpaceAssemblerRecipe;
import com.atomiclegacy.inventory.recipes.ShredderRecipe;
import com.atomiclegacy.inventory.recipes.SilexRecipe;
import com.atomiclegacy.inventory.recipes.SolidificationRecipe;
import com.atomiclegacy.inventory.recipes.SolderingStationRecipe;
import com.atomiclegacy.inventory.recipes.VacuumDistillRecipe;
import com.atomiclegacy.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.atomiclegacy.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.atomiclegacy.inventory.recipes.anvil.HbmAnvilRecipes;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.HbmFluidDuctItem;
import com.atomiclegacy.items.LegacyVariantBlockItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.NuclearWasteItem;
import com.atomiclegacy.items.RbmkPelletItem;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.items.WasteFuelItem;
import com.atomiclegacy.items.WatzPelletItem;
import com.atomiclegacy.integration.tacz.TaczAmmoAssemblyRecipes;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.registry.LegacyContentVisibility;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public class HbmJeiPlugin implements IModPlugin {
    private static final String NO_SUBTYPE = "";
    private static final ISubtypeInterpreter<ItemStack> COMPONENT_SUBTYPE_INTERPRETER = new ISubtypeInterpreter<>() {
        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            return componentSubtype(stack);
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            return componentSubtype(stack);
        }
    };

    public static final RecipeType<RecipeHolder<ShredderRecipe>> SHREDDER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("shredder"));
    public static final RecipeType<SawmillJeiRecipe> SAWMILL =
            RecipeType.create(AtomicLegacy.MOD_ID, "sawmill", SawmillJeiRecipe.class);
    public static final RecipeType<RecipeHolder<AssemblyMachineRecipe>> ASSEMBLY_MACHINE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("assembly_machine"));
    public static final RecipeType<RecipeHolder<AssemblyMachineRecipe>> TACZ_AMMO_ASSEMBLER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("tacz_ammo_assembler"));
    public static final RecipeType<RecipeHolder<PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("precision_assembler"));
    public static final RecipeType<RecipeHolder<ChemicalPlantRecipe>> CHEMICAL_PLANT =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("chemical_plant"));
    public static final RecipeType<RecipeHolder<SolderingStationRecipe>> SOLDERING_STATION =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("soldering_station"));
    public static final RecipeType<RecipeHolder<ArcWelderRecipe>> ARC_WELDER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("arc_welder"));
    public static final RecipeType<RecipeHolder<CompressorRecipe>> COMPRESSOR =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("compressor"));
    public static final RecipeType<RecipeHolder<MixerRecipe>> MIXER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("mixer"));
    public static final RecipeType<RecipeHolder<CrystallizerRecipe>> CRYSTALLIZER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("crystallizer"));
    public static final RecipeType<RecipeHolder<CyclotronRecipe>> CYCLOTRON =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("cyclotron"));
    public static final RecipeType<RecipeHolder<ExposureChamberRecipe>> EXPOSURE_CHAMBER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("exposure_chamber"));
    public static final RecipeType<RecipeHolder<ParticleAcceleratorRecipe>> PARTICLE_ACCELERATOR =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("particle_accelerator"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("silex"));
    public static final RecipeType<RecipeHolder<ElectrolyzerFluidRecipe>> ELECTROLYZER_FLUID =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("electrolyzer_fluid"));
    public static final RecipeType<RecipeHolder<ElectrolyzerMetalRecipe>> ELECTROLYZER_METAL =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("electrolyzer_metal"));
    public static final RecipeType<RecipeHolder<PressRecipe>> PRESS =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("press"));
    public static final RecipeType<RecipeHolder<PyroOvenRecipe>> PYRO_OVEN =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("pyrolysis"));
    public static final RecipeType<RecipeHolder<AmmoPressRecipe>> AMMO_PRESS =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("ammo_press"));
    public static final RecipeType<RecipeHolder<CentrifugeRecipe>> CENTRIFUGE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("centrifuge"));
    public static final RecipeType<RecipeHolder<GasCentrifugeRecipe>> GAS_CENTRIFUGE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("gas_centrifuge"));
    public static final RecipeType<PwrJeiRecipe> PWR =
            RecipeType.create(AtomicLegacy.MOD_ID, "pwr", PwrJeiRecipe.class);
    public static final RecipeType<RecipeHolder<FuelPoolRecipe>> FUEL_POOL =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("fuel_pool"));
    public static final RecipeType<StorageDrumJeiRecipe> STORAGE_DRUM =
            RecipeType.create(AtomicLegacy.MOD_ID, "storage_drum", StorageDrumJeiRecipe.class);
    public static final RecipeType<BoilingJeiRecipe> BOILING =
            RecipeType.create(AtomicLegacy.MOD_ID, "boiling", BoilingJeiRecipe.class);
    public static final RecipeType<LegacyFluidContainerJeiRecipe> LEGACY_FLUID_CONTAINER =
            RecipeType.create(AtomicLegacy.MOD_ID, "legacy_fluid_container", LegacyFluidContainerJeiRecipe.class);
    public static final RecipeType<OreSlopperJeiRecipe> ORE_SLOPPER =
            RecipeType.create(AtomicLegacy.MOD_ID, "ore_slopper", OreSlopperJeiRecipe.class);
    public static final RecipeType<RecipeHolder<RockMillRecipe>> ROCK_MILL =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("rock_mill"));
    public static final RecipeType<RecipeHolder<SupercomputerRecipe>> SUPERCOMPUTER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("supercomputer"));
    public static final RecipeType<RecipeHolder<SpaceAssemblerRecipe>> SPACE_ASSEMBLER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("space_assembler"));
    public static final RecipeType<RecipeHolder<PurexRecipe>> PUREX =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("purex"));
    public static final RecipeType<RecipeHolder<FusionRecipe>> FUSION =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("fusion"));
    public static final RecipeType<RecipeHolder<PlasmaForgeRecipe>> PLASMA_FORGE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("plasma_forge"));
    public static final RecipeType<RecipeHolder<FusionBreederFluidRecipe>> FUSION_BREEDER_FLUID =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("fusion_breeder_fluid"));
    public static final RecipeType<RecipeHolder<RbmkOutgasserRecipe>> RBMK_OUTGASSER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("rbmk_outgasser"));
    public static final RecipeType<RecipeHolder<CrucibleRecipe>> CRUCIBLE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("crucible"));
    public static final RecipeType<RecipeHolder<RefineryRecipe>> REFINERY =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("refinery"));
    public static final RecipeType<RecipeHolder<VacuumDistillRecipe>> VACUUM_DISTILL =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("vacuum_distill"));
    public static final RecipeType<RecipeHolder<CokerRecipe>> COKER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("coking"));
    public static final RecipeType<RecipeHolder<CombinationOvenRecipe>> COMBINATION_OVEN =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("combination_oven"));
    public static final RecipeType<RecipeHolder<RotaryFurnaceRecipe>> ROTARY_FURNACE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("rotary_furnace"));
    public static final RecipeType<RecipeHolder<ArcFurnaceRecipe>> ARC_FURNACE_SOLID =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("arc_furnace_solid"));
    public static final RecipeType<RecipeHolder<ArcFurnaceRecipe>> ARC_FURNACE_LIQUID =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("arc_furnace_liquid"));
    public static final RecipeType<RecipeHolder<SolidificationRecipe>> SOLIDIFICATION =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("solidification"));
    public static final RecipeType<RecipeHolder<LiquefactionRecipe>> LIQUEFACTION =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("liquefaction"));
    public static final RecipeType<RecipeHolder<FractionTowerRecipe>> FRACTION_TOWER =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("fraction_tower"));
    public static final RecipeType<RecipeHolder<CrackingRecipe>> CRACKING =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("cracking"));
    public static final RecipeType<RadiolysisJeiRecipe> RADIOLYSIS =
            RecipeType.create(AtomicLegacy.MOD_ID, "radiolysis", RadiolysisJeiRecipe.class);
    public static final RecipeType<RecipeHolder<ReformingRecipe>> REFORMING =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("reforming"));
    public static final RecipeType<RecipeHolder<HydrotreatingRecipe>> HYDROTREATING =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("hydrotreating"));
    public static final RecipeType<RecipeHolder<BlastFurnaceRecipe>> BLAST_FURNACE =
            RecipeType.createRecipeHolderType(AtomicLegacy.id("blast_furnace"));
    public static final RecipeType<FoundryCastingJeiRecipe> CRUCIBLE_CASTING =
            RecipeType.create(AtomicLegacy.MOD_ID, "crucible_foundry", FoundryCastingJeiRecipe.class);
    public static final RecipeType<AnvilConstructionRecipe> ANVIL_CONSTRUCTION =
            RecipeType.create(AtomicLegacy.MOD_ID, "anvil_construction", AnvilConstructionRecipe.class);
    public static final RecipeType<AnvilSmithingRecipe> ANVIL_SMITHING =
            RecipeType.create(AtomicLegacy.MOD_ID, "anvil_smithing", AnvilSmithingRecipe.class);
    public static final RecipeType<WatzJeiRecipe> WATZ =
            RecipeType.create(AtomicLegacy.MOD_ID, "watz", WatzJeiRecipe.class);
    // [parity T4: JEI] port-only categories removed (no NEIRegistry handler): breeder_reactor, blast_furnace_fuel, armor_table,
    // silex_ir/visible/uv/gamma/digamma, fusion_breeder_item (empty split of FusionBreederHandler)
    // [parity S3: JEI] watz_construction / icf_construction / icf_tooling replaced by NEI's Construction and Tooling (ParityS3Jei)

    @Override
    public ResourceLocation getPluginUid() {
        return AtomicLegacy.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = runtime.getIngredientManager()
                .getAllIngredients(VanillaTypes.ITEM_STACK)
                .stream()
                .filter(LegacyContentVisibility::isJeiHidden)
                .toList();
        // [parity N1: port-only fluids hidden] fluids with IDs 157+ are not 1.7.10 FluidTypes
        List<net.neoforged.neoforge.fluids.FluidStack> hiddenFluids = runtime.getIngredientManager()
                .getAllIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK)
                .stream()
                .filter(stack -> com.atomiclegacy.registry.HbmFluids.fromNeoFluid(stack.getFluid()).map(com.atomiclegacy.fluid.HbmFluidDefinition::isPortOnly).orElse(false))
                .toList();
        if (!hiddenFluids.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, hiddenFluids);
        }
        if (hidden.isEmpty()) {
            return; // JEI rejects an empty removal list ("ingredients must not be empty")
        }
        runtime.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                hidden
        );
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (AtomicLegacy.MOD_ID.equals(id.getNamespace()) && needsComponentSubtype(item)) {
                registration.registerSubtypeInterpreter(item, COMPONENT_SUBTYPE_INTERPRETER);
            }
        }
    }

    private static boolean needsComponentSubtype(Item item) {
        return item instanceof ScrapsItem
                || item instanceof LegacyVariantItem
                || item instanceof LegacyVariantBlockItem
                || item instanceof NuclearWasteItem
                || item instanceof RbmkPelletItem
                || item instanceof WasteFuelItem
                || item instanceof WatzPelletItem
                || item instanceof HbmFluidDuctItem
                || item instanceof FluidIdentifierItem
                || item instanceof com.atomiclegacy.items.CustomMachineItem // [parity X1: custom machines] machineType
                || item instanceof HbmFluidContainerItem container && container.isFilledContainer();
    }

    private static String componentSubtype(ItemStack stack) {
        if (stack.getItem() instanceof ScrapsItem) {
            FoundryMaterialStack contents = ScrapsItem.contents(stack);
            if (contents == null) {
                return NO_SUBTYPE;
            }
            return materialSubtype(contents.material())
                    + (ScrapsItem.isLiquid(stack) ? ":liquid" : ":solid");
        }
        if (stack.getItem() instanceof com.atomiclegacy.items.CustomMachineItem) { // [parity X1: custom machines] the damage value i + 100
            com.atomiclegacy.config.CustomMachineConfigJSON.MachineConfiguration conf = com.atomiclegacy.config.CustomMachineConfigJSON.configFor(stack);
            return conf == null ? NO_SUBTYPE : "custom_machine:" + conf.unlocalizedName;
        }
        if (stack.getItem() instanceof LegacyVariantItem legacyVariant) {
            return "variant:" + legacyVariant.variant(stack).id();
        }
        if (stack.getItem() instanceof LegacyVariantBlockItem legacyVariantBlock) {
            return "block_variant:" + legacyVariantBlock.variant(stack);
        }
        if (stack.getItem() instanceof RbmkPelletItem pellet) {
            return "rbmk_pellet_state:" + pellet.state(stack);
        }
        if (stack.getItem() instanceof NuclearWasteItem waste) {
            return "nuclear_waste:"
                    + waste.family()
                    + ":"
                    + waste.depleted()
                    + ":"
                    + waste.tiny()
                    + ":"
                    + NuclearWasteItem.classOf(stack).metadata();
        }
        if (stack.getItem() instanceof WasteFuelItem) {
            return WasteFuelItem.isHot(stack) ? "spent_fuel:hot" : "spent_fuel:cooled";
        }
        if (stack.getItem() instanceof WatzPelletItem) {
            return "watz_type:" + WatzPelletItem.type(stack).id
                    + (WatzPelletItem.isDepletedPellet(stack) ? ":depleted" : ":active");
        }
        if (stack.getItem() instanceof HbmFluidDuctItem) {
            return "fluid:" + HbmFluidDuctItem.fluid(stack).name();
        }
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return "fluid_identifier:"
                    + FluidIdentifierItem.primary(stack).name()
                    + ":"
                    + FluidIdentifierItem.secondary(stack).name();
        }
        if (stack.getItem() instanceof HbmFluidContainerItem container && container.isFilledContainer()) {
            return "fluid:" + HbmFluidContainerItem.fluid(stack).name();
        }
        return NO_SUBTYPE;
    }

    private static String materialSubtype(FoundryMaterial material) {
        if (material == null) {
            return NO_SUBTYPE;
        }
        return "material:" + material.id() + ":" + material.name();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ShredderRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SawmillRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AssemblyMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AssemblyMachineRecipeCategory(
                        registration.getJeiHelpers().getGuiHelper(),
                        TACZ_AMMO_ASSEMBLER,
                        Component.translatable("container.atomiclegacy.tacz_ammo_assembler"),
                        new ItemStack(HbmBlocks.MACHINE_TACZ_AMMO_ASSEMBLER.get())
                ),
                new PrecisionAssemblerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ChemicalPlantRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SolderingStationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ArcWelderRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CompressorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MixerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AmmoPressRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrystallizerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CyclotronRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ExposureChamberRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ParticleAcceleratorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper()), // [parity T4: JEI] SILEXRecipeHandler layout
                new ElectrolyzerFluidRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ElectrolyzerMetalRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PyroOvenRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CentrifugeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new GasCentrifugeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PwrRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FuelPoolRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new StorageDrumRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BoilingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new LegacyFluidContainerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new OreSlopperRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RockMillRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SupercomputerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SpaceAssemblerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PurexRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FusionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PlasmaForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FusionBreederFluidRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RbmkOutgasserRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrucibleRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RefineryRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new VacuumDistillRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CokerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CombinationOvenRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RotaryFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ArcFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper(), false),
                new ArcFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper(), true),
                new SolidificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new LiquefactionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FractionTowerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrackingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RadiolysisRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ReformingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new HydrotreatingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BlastFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FoundryCastingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AnvilConstructionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AnvilSmithingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new WatzRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
        CustomMachineRecipeCategory.registerCategories(registration); // [parity X1: custom machines] NEIConfig: one handler per niceList entry
        ParityS3Jei.registerCategories(registration); // [parity S3: JEI]
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ANVIL_CONSTRUCTION, HbmAnvilRecipes.construction());
        registration.addRecipes(ANVIL_SMITHING, HbmAnvilRecipes.smithing());
        registration.addRecipes(BOILING, BoilingJeiRecipe.createAll());
        registration.addRecipes(LEGACY_FLUID_CONTAINER, LegacyFluidContainerJeiRecipe.createAll().stream() // [parity S3: JEI] NEI lists only (fluid, empty) -> full
                .filter(recipe -> recipe.mode() == LegacyFluidContainerJeiRecipe.Mode.FILL).toList());
        registration.addRecipes(SAWMILL, SawmillJeiRecipe.createAll());
        registration.addRecipes(ORE_SLOPPER, java.util.List.of(OreSlopperJeiRecipe.create()));
        registration.addRecipes(WATZ, WatzJeiRecipe.createAll());
        ParityS3Jei.registerRecipes(registration); // [parity S3: JEI]
        registration.addRecipes(PWR, PwrJeiRecipe.createAll());

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<RecipeHolder<RockMillRecipe>> rockMillRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ROCK_MILL.get());
        registration.addRecipes(ROCK_MILL, rockMillRecipes);
        List<RecipeHolder<SupercomputerRecipe>> supercomputerRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SUPERCOMPUTER.get());
        registration.addRecipes(SUPERCOMPUTER, supercomputerRecipes);
        List<RecipeHolder<SpaceAssemblerRecipe>> spaceAssemblerRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SPACE_ASSEMBLER.get());
        registration.addRecipes(SPACE_ASSEMBLER, spaceAssemblerRecipes);

        List<RecipeHolder<ShredderRecipe>> recipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SHREDDER.get());
        registration.addRecipes(SHREDDER, recipes);
        List<RecipeHolder<AssemblyMachineRecipe>> assemblyRecipes = AssemblyMachineRecipe.activeVariants(
                minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
        );
        registration.addRecipes(ASSEMBLY_MACHINE, assemblyRecipes.stream().filter(NeiGenericCategory.notSecret(h -> h.value().blueprintPools())).toList()); // [parity T4: NEI_HIDE_SECRETS]
        registration.addRecipes(TACZ_AMMO_ASSEMBLER, TaczAmmoAssemblyRecipes.available(minecraft.level));
        List<RecipeHolder<PrecisionAssemblerRecipe>> precisionAssemblerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRECISION_ASSEMBLER.get());
        registration.addRecipes(PRECISION_ASSEMBLER, precisionAssemblerRecipes.stream().filter(NeiGenericCategory.notSecret(h -> h.value().blueprintPools())).toList()); // [parity T4: NEI_HIDE_SECRETS]
        List<RecipeHolder<ChemicalPlantRecipe>> chemicalPlantRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get());
        registration.addRecipes(CHEMICAL_PLANT, chemicalPlantRecipes.stream().filter(NeiGenericCategory.notSecret(h -> h.value().blueprintPools())).toList()); // [parity T4: NEI_HIDE_SECRETS]
        List<RecipeHolder<SolderingStationRecipe>> solderingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLDERING_STATION.get());
        registration.addRecipes(SOLDERING_STATION, solderingRecipes);
        List<RecipeHolder<ArcWelderRecipe>> arcWelderRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get());
        registration.addRecipes(ARC_WELDER, arcWelderRecipes);
        List<RecipeHolder<CompressorRecipe>> compressorRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COMPRESSOR.get());
        registration.addRecipes(COMPRESSOR, CompressorRecipeCategory.visible(compressorRecipes)); // [parity T4: CompressorRecipes.getRecipes skip]
        CustomMachineRecipeCategory.registerRecipes(registration); // [parity X1: custom machines] one category per configuration
        List<RecipeHolder<MixerRecipe>> mixerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.MIXER.get());
        registration.addRecipes(MIXER, mixerRecipes);
        List<RecipeHolder<CrystallizerRecipe>> crystallizerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get());
        registration.addRecipes(CRYSTALLIZER, CrystallizerRecipeCategory.visible(crystallizerRecipes)); // [parity T4: scrap_plastic skip]
        List<RecipeHolder<CyclotronRecipe>> cyclotronRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get());
        registration.addRecipes(CYCLOTRON, CyclotronRecipeCategory.expand(cyclotronRecipes)); // [parity T4: one NEI entry per target item]
        List<RecipeHolder<ExposureChamberRecipe>> exposureChamberRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.EXPOSURE_CHAMBER.get());
        registration.addRecipes(EXPOSURE_CHAMBER, exposureChamberRecipes);
        List<RecipeHolder<ParticleAcceleratorRecipe>> particleAcceleratorRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PARTICLE_ACCELERATOR.get());
        registration.addRecipes(PARTICLE_ACCELERATOR, particleAcceleratorRecipes);
        List<RecipeHolder<SilexRecipe>> silexRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SILEX.get());
        registration.addRecipes(SILEX, silexRecipes);
        List<RecipeHolder<ElectrolyzerFluidRecipe>> electrolyzerFluidRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_FLUID.get());
        registration.addRecipes(ELECTROLYZER_FLUID, electrolyzerFluidRecipes);
        List<RecipeHolder<ElectrolyzerMetalRecipe>> electrolyzerMetalRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_METAL.get());
        registration.addRecipes(ELECTROLYZER_METAL, electrolyzerMetalRecipes);
        List<RecipeHolder<PressRecipe>> pressRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get());
        registration.addRecipes(PRESS, pressRecipes);
        List<RecipeHolder<PyroOvenRecipe>> pyroOvenRecipes = PyroOvenJeiRecipes.appendSolidFuelRecipes(
                minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PYRO_OVEN.get())
        );
        registration.addRecipes(PYRO_OVEN, pyroOvenRecipes);
        List<RecipeHolder<AmmoPressRecipe>> ammoPressRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.AMMO_PRESS.get());
        registration.addRecipes(AMMO_PRESS, ammoPressRecipes);
        List<RecipeHolder<CentrifugeRecipe>> centrifugeRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CENTRIFUGE.get());
        registration.addRecipes(CENTRIFUGE, centrifugeRecipes);
        List<RecipeHolder<GasCentrifugeRecipe>> gasCentrifugeRecipes = GasCentrifugeJeiRecipes.original(); // [parity N1: port-only fluids hidden] NEI shows only the 4 cascade entries
        registration.addRecipes(GAS_CENTRIFUGE, gasCentrifugeRecipes);
        List<RecipeHolder<FuelPoolRecipe>> fuelPoolRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUEL_POOL.get());
        registration.addRecipes(FUEL_POOL, fuelPoolRecipes);
        registration.addRecipes(STORAGE_DRUM, StorageDrumJeiRecipe.createAll());
        List<RecipeHolder<PurexRecipe>> purexRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get());
        registration.addRecipes(PUREX, purexRecipes);
        List<RecipeHolder<FusionRecipe>> fusionRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION.get());
        registration.addRecipes(FUSION, fusionRecipes);
        List<RecipeHolder<PlasmaForgeRecipe>> plasmaForgeRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get());
        registration.addRecipes(PLASMA_FORGE, plasmaForgeRecipes.stream().filter(NeiGenericCategory.notSecret(h -> h.value().blueprintPools())).toList()); // [parity T4: NEI_HIDE_SECRETS]
        List<RecipeHolder<FusionBreederFluidRecipe>> fusionBreederFluidRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION_BREEDER_FLUID.get());
        registration.addRecipes(FUSION_BREEDER_FLUID, fusionBreederFluidRecipes);
        List<RecipeHolder<RbmkOutgasserRecipe>> outgasserRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.RBMK_OUTGASSER.get()).stream()
                .filter(holder -> !holder.value().fusionOnly())
                .toList();
        registration.addRecipes(RBMK_OUTGASSER, outgasserRecipes);
        List<RecipeHolder<CrucibleRecipe>> crucibleRecipes = com.atomiclegacy.inventory.recipes.CrucibleRecipeOrder.sorted(minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRUCIBLE.get())); // [parity Q1: registration order]
        registration.addRecipes(CRUCIBLE, crucibleRecipes);
        List<RecipeHolder<RefineryRecipe>> refineryRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.REFINERY.get());
        registration.addRecipes(REFINERY, refineryRecipes);
        List<RecipeHolder<VacuumDistillRecipe>> vacuumDistillRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.VACUUM_DISTILL.get());
        registration.addRecipes(VACUUM_DISTILL, vacuumDistillRecipes);
        List<RecipeHolder<CokerRecipe>> cokerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COKER.get());
        registration.addRecipes(COKER, cokerRecipes);
        List<RecipeHolder<CombinationOvenRecipe>> combinationOvenRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COMBINATION_OVEN.get());
        registration.addRecipes(COMBINATION_OVEN, combinationOvenRecipes);
        List<RecipeHolder<RotaryFurnaceRecipe>> rotaryFurnaceRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ROTARY_FURNACE.get());
        registration.addRecipes(ROTARY_FURNACE, rotaryFurnaceRecipes);
        List<RecipeHolder<ArcFurnaceRecipe>> arcFurnaceRecipes = ArcFurnaceJeiRecipes.appendDynamic(
                minecraft.level.getRecipeManager(),
                minecraft.level.registryAccess(),
                minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_FURNACE.get())
        );
        registration.addRecipes(ARC_FURNACE_SOLID, arcFurnaceRecipes.stream()
                .filter(holder -> holder.value().hasSolidOutput()).toList());
        registration.addRecipes(ARC_FURNACE_LIQUID, arcFurnaceRecipes.stream()
                .filter(holder -> holder.value().hasLiquidOutput()).toList());
        List<RecipeHolder<SolidificationRecipe>> solidificationRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLIDIFICATION.get());
        registration.addRecipes(SOLIDIFICATION, solidificationRecipes);
        List<RecipeHolder<LiquefactionRecipe>> liquefactionRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.LIQUEFACTION.get());
        registration.addRecipes(LIQUEFACTION, liquefactionRecipes);
        List<RecipeHolder<FractionTowerRecipe>> fractionTowerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FRACTION_TOWER.get());
        registration.addRecipes(FRACTION_TOWER, fractionTowerRecipes);
        List<RecipeHolder<CrackingRecipe>> crackingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRACKING.get());
        registration.addRecipes(CRACKING, crackingRecipes);
        registration.addRecipes(RADIOLYSIS, RadiolysisJeiRecipe.createAll(minecraft.level.getRecipeManager()));
        List<RecipeHolder<ReformingRecipe>> reformingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.REFORMING.get());
        registration.addRecipes(REFORMING, reformingRecipes);
        List<RecipeHolder<HydrotreatingRecipe>> hydrotreatingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.HYDROTREATING.get());
        registration.addRecipes(HYDROTREATING, hydrotreatingRecipes);
        List<RecipeHolder<BlastFurnaceRecipe>> blastFurnaceRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get()).stream()
                .filter(holder -> !holder.value().hidden())
                .toList();
        registration.addRecipes(BLAST_FURNACE, blastFurnaceRecipes);
        registration.addRecipes(CRUCIBLE_CASTING, FoundryCastingJeiRecipe.createAll());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(IronFurnaceScreen.class, 52, 36, 70, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(ArcWelderScreen.class, 72, 28, 33, 14, ARC_WELDER);
        registration.addRecipeClickArea(ArcFurnaceScreen.class, 7, 17, 18, 18, ARC_FURNACE_SOLID, ARC_FURNACE_LIQUID);
        registration.addRecipeClickArea(CompressorScreen.class, 42, 26, 55, 17, COMPRESSOR);
        registration.addRecipeClickArea(MixerScreen.class, 62, 36, 53, 44, MIXER);
        registration.addRecipeClickArea(AmmoPressScreen.class, 116, 18, 54, 72, AMMO_PRESS);
        registration.addRecipeClickArea(PyroOvenScreen.class, 57, 47, 27, 12, PYRO_OVEN);
        registration.addRecipeClickArea(CyclotronScreen.class, 48, 27, 34, 34, CYCLOTRON);
        registration.addRecipeClickArea(ExposureChamberScreen.class, 36, 39, 42, 10, EXPOSURE_CHAMBER);
        registration.addRecipeClickArea(ParticleAcceleratorScreen.class, 62, 18, 36, 45, PARTICLE_ACCELERATOR);
        registration.addRecipeClickArea(PrecisionAssemblerScreen.class, 62, 126, 70, 16, PRECISION_ASSEMBLER);
        registration.addRecipeClickArea(FusionMachineScreen.class, 43, 80, 18, 18, FUSION);
        registration.addRecipeClickArea(FusionMachineScreen.class, 7, 80, 18, 18, PLASMA_FORGE);
        registration.addRecipeClickArea(FusionMachineScreen.class, 67, 46, 42, 14, FUSION_BREEDER_FLUID);
        registration.addRecipeClickArea(HydrotreaterScreen.class, 35, 18, 124, 52, HYDROTREATING);
        registration.addRecipeClickArea(SteelFurnaceScreen.class, 54, 18, 68, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(SteelFurnaceScreen.class, 54, 36, 68, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(SteelFurnaceScreen.class, 54, 54, 68, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(RockMillScreen.class, 62, 90, 70, 16, ROCK_MILL);
        registration.addRecipeClickArea(SupercomputerScreen.class, 62, 81, 70, 16, SUPERCOMPUTER);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SHREDDER.get(), SHREDDER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SAWMILL.get(), SAWMILL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get(), ASSEMBLY_MACHINE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ASSEMBLY_FACTORY.get(), ASSEMBLY_MACHINE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_TACZ_AMMO_ASSEMBLER.get(), TACZ_AMMO_ASSEMBLER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PRECASS.get(), PRECISION_ASSEMBLER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CHEMICAL_PLANT.get(), CHEMICAL_PLANT);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CHEMICAL_FACTORY.get(), CHEMICAL_PLANT);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SOLDERING_STATION.get(), SOLDERING_STATION);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ARC_WELDER.get(), ARC_WELDER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_COMPRESSOR.get(), COMPRESSOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_COMPRESSOR_COMPACT.get(), COMPRESSOR);
        CustomMachineRecipeCategory.registerCatalysts(registration); // [parity X1: custom machines]
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_RADIOLYSIS.get(), RADIOLYSIS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_MIXER.get(), MIXER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_AMMO_PRESS.get(), AMMO_PRESS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CRYSTALLIZER.get(), CRYSTALLIZER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CYCLOTRON.get(), CYCLOTRON);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get(), EXPOSURE_CHAMBER);
        registration.addRecipeCatalyst(HbmBlocks.PA_DETECTOR.get(), PARTICLE_ACCELERATOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SILEX.get(), SILEX);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ELECTROLYSER.get(), ELECTROLYZER_FLUID);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ELECTROLYSER.get(), ELECTROLYZER_METAL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PRESS.get(), PRESS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_EPRESS.get(), PRESS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PYROOVEN.get(), PYRO_OVEN);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CENTRIFUGE.get(), CENTRIFUGE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_GASCENT.get(), GAS_CENTRIFUGE);
        registration.addRecipeCatalyst(HbmBlocks.PWR_CONTROLLER.get(), PWR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_WASTE_DRUM.get(), FUEL_POOL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_STORAGE_DRUM.get(), STORAGE_DRUM);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_BOILER.get(), BOILING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_INDUSTRIAL_BOILER.get(), BOILING);
        registration.addRecipeCatalyst(HbmItems.TANK_STEEL.get(), LEGACY_FLUID_CONTAINER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ORE_SLOPPER.get(), ORE_SLOPPER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ROCKMILL.get(), ROCK_MILL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SUPERCOMPUTER.get(), SUPERCOMPUTER);
        registration.addRecipeCatalyst(HbmItems.SATELLITE_SCIENCE_ASSEMBLER.get(), SPACE_ASSEMBLER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PUREX.get(), PUREX);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_TORUS.get(), FUSION);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_PLASMA_FORGE.get(), PLASMA_FORGE);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_BREEDER.get(), FUSION_BREEDER_FLUID);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_BREEDER.get(), RBMK_OUTGASSER);
        registration.addRecipeCatalyst(HbmBlocks.RBMK_OUTGASSER.get(), RBMK_OUTGASSER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CRUCIBLE.get(), CRUCIBLE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_REFINERY.get(), REFINERY);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_VACUUM_DISTILL.get(), VACUUM_DISTILL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_COKER.get(), COKER);
        registration.addRecipeCatalyst(HbmBlocks.FURNACE_COMBINATION.get(), COMBINATION_OVEN);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ROTARY_FURNACE.get(), ROTARY_FURNACE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ARC_FURNACE.get(), ARC_FURNACE_SOLID);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ARC_FURNACE.get(), ARC_FURNACE_LIQUID);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SOLIDIFIER.get(), SOLIDIFICATION);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_LIQUEFACTOR.get(), LIQUEFACTION);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_FRACTION_TOWER.get(), FRACTION_TOWER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CATALYTIC_CRACKER.get(), CRACKING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CATALYTIC_REFORMER.get(), REFORMING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_HYDROTREATER.get(), HYDROTREATING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_BLAST_FURNACE.get(), BLAST_FURNACE);
        registration.addRecipeCatalyst(HbmBlocks.FURNACE_IRON.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(HbmBlocks.FURNACE_STEEL.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(HbmBlocks.FOUNDRY_MOLD.get(), CRUCIBLE_CASTING);
        registration.addRecipeCatalyst(HbmBlocks.FOUNDRY_BASIN.get(), CRUCIBLE_CASTING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_STRAND_CASTER.get(), CRUCIBLE_CASTING);
        registration.addRecipeCatalyst(HbmBlocks.WATZ.get(), WATZ);
        ParityS3Jei.registerCatalysts(registration); // [parity S3: JEI]
        for (var anvil : HbmBlocks.ANVIL_BLOCKS) {
            registration.addRecipeCatalyst(anvil.get(), ANVIL_CONSTRUCTION);
            registration.addRecipeCatalyst(anvil.get(), ANVIL_SMITHING);
        }
    }

}
