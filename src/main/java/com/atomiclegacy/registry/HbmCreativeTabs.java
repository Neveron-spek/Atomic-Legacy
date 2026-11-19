package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.integration.createbigcannons.HbmCreateBigCannonsCompat;
import com.atomiclegacy.items.BedrockOreItem;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.BlueprintItem;
import com.atomiclegacy.items.ConcreteColoredBlockItem;
import com.atomiclegacy.items.CokeBlockItem;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.HbmFluidDuctItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.LegacyVariantBlockItem;
import com.atomiclegacy.items.LegacyBedrockOreStageItem;
import com.atomiclegacy.items.LegacyByproductItem;
import com.atomiclegacy.items.MetalFenceBlockItem;
import com.atomiclegacy.items.MeteorOreBlockItem;
import com.atomiclegacy.items.NuclearWasteItem;
import com.atomiclegacy.items.OreBasaltBlockItem;
import com.atomiclegacy.items.SellafieldBlockItem;
import com.atomiclegacy.items.ToasterBlockItem;
import com.atomiclegacy.items.FusionComponentBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

public final class HbmCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AtomicLegacy.MOD_ID);

    public static final Supplier<CreativeModeTab> MATERIALS = TABS.register("materials", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabParts"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("ingot_uranium")).getDefaultInstance())
            .displayItems((parameters, output) -> display("parts", parameters, output)).build());

    public static final Supplier<CreativeModeTab> CONTROL = TABS.register("control", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabControl"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("pellet_rtg")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("materials"))
            .displayItems((parameters, output) -> display("control", parameters, output)).build());

    public static final Supplier<CreativeModeTab> TEMPLATES = TABS.register("templates", () -> CreativeModeTab.builder()
            .withSearchBar()
            .title(Component.translatable("itemGroup.tabTemplate"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("blueprints")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("control"))
            .displayItems((parameters, output) -> display("template", parameters, output)).build());

    public static final Supplier<CreativeModeTab> BUILDING = TABS.register("building", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabBlocks"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("ore_uranium")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("templates"))
            .displayItems((parameters, output) -> display("block", parameters, output)).build());

    public static final Supplier<CreativeModeTab> MACHINES = TABS.register("machines", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabMachine"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("pwr_controller")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("building"))
            .displayItems((parameters, output) -> display("machine", parameters, output)).build());

    public static final Supplier<CreativeModeTab> NUCLEAR_WEAPONS = TABS.register("nuclear_weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabNuke"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("nuke_man")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("machines"))
            .displayItems((parameters, output) -> display("nuke", parameters, output)).build());

    public static final Supplier<CreativeModeTab> ROCKETS_MISSILES = TABS.register("rockets_missiles", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabMissile"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("missile_nuclear")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("nuclear_weapons"))
            .displayItems((parameters, output) -> display("missile", parameters, output)).build());

    public static final Supplier<CreativeModeTab> TURRETS = TABS.register("turrets", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabWeapon"))
            .icon(() -> BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id("gun_greasegun")).orElse(Items.IRON_PICKAXE).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("rockets_missiles"))
            .displayItems((parameters, output) -> display("weapon", parameters, output)).build());

    public static final Supplier<CreativeModeTab> CONSUMABLES = TABS.register("consumables", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabConsumable"))
            .icon(() -> BuiltInRegistries.ITEM.get(AtomicLegacy.id("bottle_nuka")).getDefaultInstance())
            .withTabsBefore(AtomicLegacy.id("turrets"))
            .displayItems((parameters, output) -> display("consumable", parameters, output)).build());

    private static final java.util.Properties SOURCE_TABS = loadSourceTabs();

    private static java.util.Properties loadSourceTabs() {
        var entries = new java.util.Properties();
        try (var stream = HbmCreativeTabs.class.getResourceAsStream("creative-tabs.properties")) {
            entries.load(java.util.Objects.requireNonNull(stream));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return entries;
    }

    private static void display(String tab, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        var seen = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
        java.util.function.Function<String, CreativeModeTab.Output> select = fallback -> (stack, visibility) -> {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!LegacyContentVisibility.isCreativeHidden(stack) && seen.add(stack)
                    && tab.equals(SOURCE_TABS.getProperty(id.getPath(), fallback))) {
                output.accept(stack, visibility);
            }
        };
        addOres(parameters, select.apply("block"));
        addMaterials(parameters, select.apply("parts"));
        addControl(parameters, select.apply("control"));
        addMaterialBlocks(parameters, select.apply("block"));
        addMachineComponents(parameters, select.apply("parts"));
        addPowerGrid(parameters, select.apply("machine"));
        addGenerators(parameters, select.apply("machine"));
        addPowerStorage(parameters, select.apply("control"));
        addReactors(parameters, select.apply("machine"));
        addFuelRods(parameters, select.apply("control"));
        addRbmkReactor(parameters, select.apply("machine"));
        addNuclearBillets(parameters, select.apply("control"));
        addNuclearWeapons(parameters, select.apply("nuke"));
        addRocketsMissiles(parameters, select.apply("missile"));
        addTurrets(parameters, select.apply("weapon"));
        addCreateArtilleryIntegration(parameters, select.apply("weapon"));
        addFluids(parameters, select.apply("control"));
        addPetroleum(parameters, select.apply("machine"));
        addContainers(parameters, select.apply("machine"));
        addThermal(parameters, select.apply("machine"));
        addFoundry(parameters, select.apply("machine"));
        addMiningProcessing(parameters, select.apply("machine"));
        addMachines(parameters, select.apply("machine"));
        addBuilding(parameters, select.apply("block"));
        addDoors(parameters, select.apply("block"));
        addTools(parameters, select.apply("consumable"));
        addArmor(parameters, select.apply("consumable"));
        addSpawnEggs(parameters, select.apply("consumable"));
        addMiscellaneous(parameters, select.apply("parts"));
        addLegacyBlocks(parameters, select.apply("block"));
    }

    private static void addOres(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var ore : HbmBlocks.OVERWORLD_ORES) {
            if (!isPetroleumBlock(ore)) {
                if (ore == HbmBlocks.ORE_METEOR && ore.get().asItem() instanceof MeteorOreBlockItem meteorOre) {
                    meteorOre.addCreativeVariants(output);
                } else if (ore == HbmBlocks.ORE_BASALT && ore.get().asItem() instanceof OreBasaltBlockItem basaltOre) {
                    basaltOre.addCreativeVariants(output);
                } else {
                    acceptBlock(output, ore);
                }
            }
        }
        output.accept(HbmBlocks.ANCIENT_SCRAP);
        for (var ore : HbmBlocks.DEEPSLATE_ORES) {
            if (!isPetroleumBlock(ore)) {
                acceptBlock(output, ore);
            }
        }
        for (var ore : HbmBlocks.SCHIST_ORES) {
            acceptBlock(output, ore);
        }
        for (var ore : HbmBlocks.NETHER_ORES) {
            output.accept(ore);
        }
        for (var cluster : HbmBlocks.ORE_CLUSTERS) {
            acceptBlock(output, cluster);
        }
        output.accept(HbmBlocks.ORE_VOLCANO);
        for (var rawOre : HbmItems.RAW_ORES) {
            output.accept(rawOre);
        }
        for (var crystal : HbmItems.MINERAL_CRYSTALS) {
            output.accept(crystal);
        }
        for (var oreDrop : HbmItems.ORE_DROPS) {
            if (oreDrop.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else if (oreDrop.get() instanceof LegacyBedrockOreStageItem bedrockOre) {
                bedrockOre.addCreativeVariants(output);
            } else if (oreDrop.get() instanceof LegacyByproductItem byproduct) {
                byproduct.addCreativeVariants(output);
            } else if (oreDrop.get() instanceof BedrockOreItem bedrockOre) {
                bedrockOre.addCreativeVariants(output);
            } else {
                output.accept(oreDrop);
            }
        }
    }

    private static void addMaterials(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var ingot : HbmItems.INGOT_MATERIALS) {
            output.accept(ingot);
        }
        for (var nugget : HbmItems.NUGGET_MATERIALS) {
            output.accept(nugget);
        }
        for (var powder : HbmItems.POWDER_MATERIALS) {
            if (powder.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else {
                output.accept(powder);
            }
        }
        for (var plate : HbmItems.PLATE_MATERIALS) {
            output.accept(plate);
        }
        for (var material : HbmItems.MISC_MATERIALS) {
            if (material == HbmItems.SINGULARITY_SPARK || isPetroleumItem(material) || isNuclearBilletItem(material)) {
                continue;
            }
            if (material == HbmItems.FALLOUT_ITEM) {
                output.accept(material);
            } else if (material.get() instanceof NuclearWasteItem waste) {
                waste.addCreativeVariants(output);
            } else if (material.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else {
                output.accept(material);
            }
        }
    }

    private static void addControl(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(HbmItems.SINGULARITY);
        output.accept(HbmItems.SINGULARITY_COUNTER_RESONANT);
        output.accept(HbmItems.SINGULARITY_SUPER_HEATED);
        output.accept(HbmItems.BLACK_HOLE);
        output.accept(HbmItems.SINGULARITY_SPARK);
    }

    private static void addMaterialBlocks(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.MATERIAL_BLOCKS) {
            if (block.get() == HbmBlocks.BLOCK_COKE.get()
                    && HbmBlocks.BLOCK_COKE.get().asItem() instanceof CokeBlockItem cokeBlock) {
                cokeBlock.addCreativeVariants(output);
            } else {
                acceptBlock(output, block);
            }
        }
    }

    private static void addMachineComponents(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var component : HbmItems.MACHINE_COMPONENTS) {
            if (component.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else if (component.get() instanceof com.atomiclegacy.items.LegacyMetaUpgradeItem upgrade) {
                upgrade.addCreativeVariants(output);
            } else {
                output.accept(component);
            }
        }
    }

    private static void addPowerGrid(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(HbmBlocks.RED_CABLE);
        output.accept(HbmBlocks.RED_CABLE_CLASSIC);
        if (HbmBlocks.RED_CABLE_BOX.get().asItem() instanceof com.atomiclegacy.items.PowerCableBoxBlockItem boxCable) {
            boxCable.addCreativeVariants(output);
        }
        output.accept(HbmBlocks.RED_CABLE_PAINTABLE);
        output.accept(HbmBlocks.RED_CABLE_GAUGE);
        output.accept(HbmBlocks.RED_WIRE_COATED);
        // [parity L1: diode] cable_diode has setCreativeTab(null) in 1.7.10.
        output.accept(HbmBlocks.CABLE_SWITCH);
        output.accept(HbmBlocks.CABLE_DETECTOR);
        output.accept(HbmBlocks.CHARGER);
        output.accept(HbmBlocks.RED_CONNECTOR);
        output.accept(HbmBlocks.CONNECTOR_RED_SUPER);
        output.accept(HbmBlocks.RED_PYLON);
        output.accept(HbmBlocks.RED_PYLON_STEEL);
        output.accept(HbmBlocks.RED_PYLON_MEDIUM_WOOD);
        output.accept(HbmBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER);
        output.accept(HbmBlocks.RED_PYLON_MEDIUM_STEEL);
        output.accept(HbmBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
        output.accept(HbmBlocks.RED_PYLON_LARGE);
        output.accept(HbmBlocks.SUBSTATION);
        output.accept(HbmBlocks.MACHINE_DETECTOR);
        output.accept(HbmBlocks.MACHINE_CONVERTER_HE_RF); // [parity L1: converters] machineTab
        output.accept(HbmBlocks.MACHINE_CONVERTER_RF_HE);
        output.accept(HbmItems.WIRING_RED_COPPER);
    }

    private static void addGenerators(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(HbmBlocks.MACHINE_WOOD_BURNER);
        output.accept(HbmBlocks.MACHINE_STIRLING);
        output.accept(HbmBlocks.MACHINE_STIRLING_STEEL);
        output.accept(HbmBlocks.MACHINE_STIRLING_CREATIVE);
        output.accept(HbmBlocks.MACHINE_DIESEL);
        output.accept(HbmBlocks.MACHINE_COMBUSTION_ENGINE);
        output.accept(HbmBlocks.MACHINE_STEAM_ENGINE);
        output.accept(HbmBlocks.MACHINE_INDUSTRIAL_TURBINE);
        output.accept(HbmBlocks.MACHINE_CHUNGUS);
        output.accept(HbmBlocks.MACHINE_TURBINEGAS);
        output.accept(HbmBlocks.MACHINE_TURBOFAN);
    }

    private static void addPowerStorage(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_redstone"));
        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_lead"));
        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_lithium"));
        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_sodium"));
        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_schrabidium"));
        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_quantum"));
        acceptEmptyAndFull(output, HbmItems.CAPACITOR_COPPER);
        acceptEmptyAndFull(output, HbmItems.CAPACITOR_GOLD);
        acceptEmptyAndFull(output, HbmItems.CAPACITOR_NIOBIUM);
        acceptEmptyAndFull(output, HbmItems.CAPACITOR_TANTALUM);
        acceptEmptyAndFull(output, HbmItems.CAPACITOR_BISMUTH);
        acceptEmptyAndFull(output, HbmItems.CAPACITOR_SPARK);
        ((com.atomiclegacy.items.SelfChargingBatteryItem) HbmItems.BATTERY_SC.get()).addCreativeVariants(output);
        output.accept(HbmItems.BATTERY_CREATIVE);
        output.accept(HbmItems.BATTERY_POTATO);
        output.accept(HbmItems.BATTERY_POTATOS);
        output.accept(HbmItems.CUBE_POWER);
        output.accept(HbmBlocks.MACHINE_BATTERY_REDD);
        output.accept(HbmBlocks.MACHINE_BATTERY_SOCKET);
        output.accept(HbmBlocks.CAPACITOR_COPPER);
    }

    private static void addReactors(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.REACTOR_BLOCKS) {
            if (block == HbmBlocks.FUSION_COMPONENT
                    && block.get().asItem() instanceof FusionComponentBlockItem fusionComponent) {
                fusionComponent.addCreativeVariants(output);
            } else if ((block == HbmBlocks.ICF_COMPONENT || block == HbmBlocks.ICF_LASER_COMPONENT)
                    && block.get().asItem() instanceof LegacyVariantBlockItem variants) {
                variants.addCreativeVariants(output);
            } else {
                acceptBlock(output, block);
            }
        }
        for (var item : HbmItems.REACTOR_ITEMS) {
            output.accept(item);
        }
    }

    private static void addFuelRods(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var item : HbmItems.FUEL_ROD_ITEMS) {
            if (item.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else if (item.get() instanceof com.atomiclegacy.items.WatzPelletItem pelletItem) {
                pelletItem.addCreativeVariants(output);
            } else if (item.get() instanceof com.atomiclegacy.items.IcfPelletItem pelletItem) {
                pelletItem.addCreativeVariants(output);
            } else {
                output.accept(item);
            }
        }
    }

    private static void addRbmkReactor(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.RBMK_BLOCKS) {
            acceptBlock(output, block);
        }
        for (var item : HbmItems.RBMK_REACTOR_ITEMS) {
            output.accept(item);
        }
        for (var item : HbmItems.RBMK_FUEL_ROD_ITEMS) {
            if (item.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else if (item.get() instanceof com.atomiclegacy.items.RbmkPelletItem pelletItem) {
                pelletItem.addCreativeVariants(output);
            } else {
                output.accept(item);
            }
        }
        for (var item : HbmItems.RBMK_PELLET_ITEMS) {
            if (item.get() instanceof com.atomiclegacy.items.RbmkPelletItem pelletItem) {
                pelletItem.addCreativeVariants(output);
            } else {
                output.accept(item);
            }
        }
    }

    private static void addNuclearBillets(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var billet : HbmItems.NUCLEAR_BILLETS) {
            output.accept(billet);
        }
    }

    private static void addNuclearWeapons(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.NUCLEAR_WEAPON_BLOCKS) {
            if (block == HbmBlocks.VOLCANO_CORE || block == HbmBlocks.VOLCANO_RAD_CORE
                    || block == HbmBlocks.CRASHED_BOMB) {
                continue;
            }
            acceptBlock(output, block);
        }
        if (HbmBlocks.CRASHED_BOMB.get().asItem() instanceof com.atomiclegacy.items.CrashedBombBlockItem dud) {
            dud.addCreativeVariants(output);
        }
        if (HbmBlocks.VOLCANO_CORE.get().asItem() instanceof com.atomiclegacy.items.VolcanoCoreBlockItem volcano) {
            volcano.addCreativeVariants(output);
        }
        if (HbmBlocks.VOLCANO_RAD_CORE.get().asItem() instanceof com.atomiclegacy.items.VolcanoCoreBlockItem volcano) {
            volcano.addCreativeVariants(output);
        }
        for (var item : HbmItems.NUCLEAR_WEAPON_ITEMS) {
            output.accept(item);
        }
    }

    private static void addRocketsMissiles(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.ROCKET_MISSILE_BLOCKS) {
            acceptBlock(output, block);
        }
        for (var item : HbmItems.ROCKET_MISSILE_ITEMS) {
            if (!HbmItems.isHiddenMissilePart(item) && !HbmItems.isHiddenLegacyMissile(item)) {
                outputLegacyItem(output, item);
            }
        }
        for (var item : HbmItems.SATELLITE_ITEMS) {
            outputLegacyItem(output, item);
        }
    }

    private static void addTurrets(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.TURRET_BLOCKS) {
            acceptBlock(output, block);
        }
        // 1.7.10 ModItems registers the sedna guns before ammo_standard and the weapon mods. Only A_SIDE,
        // SPECIAL and UTILITY guns get weaponTab (ItemGunBaseNT constructor); B_SIDE/LEGENDARY/SECRET/DEBUG
        // guns have no creative tab. Ported guns in ModItems registration order:
        output.accept(HbmItems.GUN_B92); // [parity c1: tab] ModItems registers gun_b92 first of the guns
        output.accept(HbmItems.GUN_PEPPERBOX);
        output.accept(HbmItems.GUN_LIGHT_REVOLVER);
        output.accept(HbmItems.GUN_HENRY);
        output.accept(HbmItems.GUN_GREASEGUN);
        output.accept(HbmItems.GUN_MARESLEG);
        output.accept(HbmItems.GUN_FLAREGUN); // [weapons batch D3: tab]
        output.accept(HbmItems.GUN_HEAVY_REVOLVER);
        output.accept(HbmItems.GUN_CARBINE);
        output.accept(HbmItems.GUN_AM180);
        output.accept(HbmItems.GUN_LIBERATOR);
        output.accept(HbmItems.GUN_CONGOLAKE); // [weapons batch D3: tab]
        output.accept(HbmItems.GUN_FLAMER); // [weapons batch F1: tab]
        output.accept(HbmItems.GUN_UZI);
        output.accept(HbmItems.GUN_SPAS12); // [weapons batch D1: tab]
        output.accept(HbmItems.GUN_PANZERSCHRECK); // [weapons batch E1: tab]
        output.accept(HbmItems.GUN_STAR_F);
        output.accept(HbmItems.GUN_G3);
        output.accept(HbmItems.GUN_MK108); // [weapons batch D3: tab]
        output.accept(HbmItems.GUN_CHEMTHROWER); // [weapons batch F1: tab]
        output.accept(HbmItems.GUN_AMAT);
        output.accept(HbmItems.GUN_M2);
        output.accept(HbmItems.GUN_AUTOSHOTGUN); // [weapons batch D1: tab]
        output.accept(HbmItems.GUN_QUADRO); // [weapons batch E1: tab]
        output.accept(HbmItems.GUN_LAG);
        output.accept(HbmItems.GUN_MINIGUN);
        output.accept(HbmItems.GUN_MISSILE_LAUNCHER); // [weapons batch E1: tab]
        output.accept(HbmItems.GUN_TESLA_CANNON); // [weapons batch E2: tab]
        output.accept(HbmItems.GUN_LASER_PISTOL); // [weapons batch E2: tab]
        output.accept(HbmItems.GUN_STG77);
        output.accept(HbmItems.GUN_TAU); // [weapons batch F2: tab]
        output.accept(HbmItems.GUN_FATMAN); // [weapons batch F2: tab]
        output.accept(HbmItems.GUN_LASRIFLE); // [weapons batch E2: tab]
        output.accept(HbmItems.GUN_STINGER); // [weapons batch F4: tab]
        output.accept(HbmItems.GUN_FLIEGERFAUST); // [weapons batch E1: tab]
        output.accept(HbmItems.GUN_COILGUN); // [weapons batch F2: tab]
        output.accept(HbmItems.GUN_BOLTER); // [weapons batch E1: tab]
        output.accept(HbmItems.GUN_DOUBLE_BARREL);
        output.accept(HbmItems.GUN_N_I_4_N_I); // [weapons batch F2: tab]
        output.accept(HbmItems.GUN_FIREEXT); // [weapons batch F3: tab]
        output.accept(HbmItems.GUN_CHARGE_THROWER); // [weapons batch F3: tab]
        output.accept(HbmItems.GUN_DRILL); // [weapons batch F3: tab]
        output.accept(HbmItems.GUN_PA_MELEE); // [parity c1: tab]
        output.accept(HbmItems.GUN_PA_RANGED); // [parity c1: tab]
        // [weapons batch A: tab]
        // [weapons batch B: tab]
        // [weapons batch C1: tab]
        // [weapons batch C2: tab]
        // [weapons batch C3: tab]
        // [weapons batch C4: tab]
        for (var item : HbmItems.TURRET_ITEMS) {
            if (item.get() instanceof LegacyVariantItem variantItem) {
                variantItem.addCreativeVariants(output);
            } else if (item.get() instanceof com.atomiclegacy.items.LegacyAmmoContainerItem container) { // [parity S4: ammo_container] meta 0 and 1
                container.addCreativeVariants(output);
            } else {
                output.accept(item);
            }
            // [weapon mods G1/G2/G3: tab] ModItems order after ammo_standard: weapon_mod_generic, weapon_mod_special in
            // EnumModSpecial order (SILENCER, SCOPE, SAW, ... SKIN_SATURNITE, LAS_SHOTGUN .. SIFTER, CANISTERS), weapon_mod_caliber.
            // weapon_mod_special_greasegun keeps its existing earlier position; weapon_mod_test has no tab.
            if (item == HbmItems.AMMO_STANDARD) {
                HbmWeaponModsG1.addCreativeGeneric(output);
                output.accept(HbmItems.WEAPON_MOD_SILENCER);
                output.accept(HbmItems.WEAPON_MOD_SCOPE);
                output.accept(HbmItems.WEAPON_MOD_SAW);
                output.accept(HbmItems.WEAPON_MOD_SLOWDOWN);
                output.accept(HbmItems.WEAPON_MOD_SPEEDUP);
                output.accept(HbmItems.WEAPON_MOD_CHOKE);
                output.accept(HbmItems.WEAPON_MOD_SPEEDLOADER);
                output.accept(HbmItems.WEAPON_MOD_FURNITURE_GREEN);
                output.accept(HbmItems.WEAPON_MOD_FURNITURE_BLACK);
                output.accept(HbmItems.WEAPON_MOD_BAYONET);
                output.accept(HbmItems.WEAPON_MOD_STACK_MAG);
                output.accept(HbmItems.WEAPON_MOD_SKIN_SATURNITE);
                com.atomiclegacy.items.weapon.sedna.mods.WeaponModsG3.addCreativeTab(output);
                output.accept(HbmItems.WEAPON_MOD_CANISTERS);
                HbmWeaponModsG1.addCreativeCaliber(output);
                output.accept(HbmItems.GUN_B92_AMMO); // [parity c1: tab] ModItems: gun_b92_ammo follows the weapon mods
            }
        }
        output.accept(HbmItems.WEAPONIZED_STARBLASTER_CELL); // [parity c1: tab]
    }

    private static void addCreateArtilleryIntegration(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        ItemStack mustardShell = HbmCreateBigCannonsCompat.mustardGasFluidShellStack(parameters.holders());
        if (!mustardShell.isEmpty()) {
            output.accept(mustardShell);
        }
    }

    private static void addFluids(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.FLUID_BLOCKS) {
            acceptBlock(output, block);
        }
        for (var item : HbmItems.FLUID_ITEMS) {
            if (item.get() instanceof com.atomiclegacy.items.HbmFluidContainerItem container) {
                acceptFluidContainerVariants(output, container);
            } else if (item.get() instanceof com.atomiclegacy.items.HbmFluidDuctItem duct) {
                duct.addCreativeVariants(output);
            } else {
                output.accept(item);
            }
        }
        for (var definition : HbmFluids.niceOrder()) {
            if (definition.allowsFluidIdentifier()) {
                output.accept(com.atomiclegacy.items.FluidIdentifierItem.forFluid(definition));
                output.accept(com.atomiclegacy.items.FluidIconItem.forFluid(definition));
            }
        }
        for (var entry : HbmFluids.entries()) {
            entry.bucketItem().ifPresent(output::accept);
        }
    }

    private static void addPetroleum(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.PETROLEUM_BLOCKS) {
            acceptBlock(output, block);
        }
        for (var block : HbmBlocks.OIL_FIELD_BLOCKS) {
            acceptBlock(output, block);
        }

        for (var block : LegacyHbmContent.LEGACY_BLOCKS) {
            if (isPetroleumLegacyBlock(block)) {
                acceptBlock(output, block);
            }
        }

        HbmItems.OIL_TAR_ITEMS.values().forEach(output::accept);
        output.accept(HbmItems.COKE_ITEMS.get("petroleum"));
        output.accept(HbmItems.CANISTER_LUBRICANT);
        output.accept(HbmItems.SOLID_FUEL);
        output.accept(HbmItems.SOLID_FUEL_BF);
        HbmItems.FUEL_ADDITIVE_ITEMS.values().forEach(output::accept);
        for (var item : LegacyHbmContent.LEGACY_ITEMS) {
            if (isPetroleumLegacyItem(item)) {
                output.accept(item);
            }
        }
    }

    private static void addContainers(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.CONTAINER_BLOCKS) {
            acceptBlock(output, block);
        }
    }

    private static void addThermal(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.THERMAL_BLOCKS) {
            acceptBlock(output, block);
        }
    }

    private static void addFoundry(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.FOUNDRY_BLOCKS) {
            acceptBlock(output, block);
        }
        for (var item : HbmItems.FOUNDRY_ITEMS) {
            output.accept(item);
        }
    }

    private static void addMiningProcessing(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.MINING_PROCESSING_BLOCKS) {
            acceptBlock(output, block);
        }
    }

    private static void addMachines(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.MACHINE_BLOCKS) {
            // A number of 1.7.10 machines own specialised BlockItems, so
            // DeferredBlock#asItem points at AIR rather than the actual item.
            // Resolve the registered item by its shared registry id instead.
            Item item = BuiltInRegistries.ITEM.get(block.getId());
            if (item == Items.AIR) {
                continue;
            }
            if (item instanceof LegacyVariantBlockItem variants) {
                variants.addCreativeVariants(output);
            } else {
                output.accept(item);
            }
        }
        // [parity X1: custom machines] ModBlocks "CM stuff": custom_machine (one per configuration), cm_* casings, anchor
        if (HbmBlocks.CUSTOM_MACHINE.get().asItem() instanceof com.atomiclegacy.items.CustomMachineItem controllers) controllers.addCreativeVariants(output);
        for (var block : java.util.List.of(HbmBlocks.CM_BLOCK, HbmBlocks.CM_SHEET, HbmBlocks.CM_ENGINE, HbmBlocks.CM_TANK, HbmBlocks.CM_CIRCUIT,
                HbmBlocks.CM_PORT, HbmBlocks.CM_FLUX, HbmBlocks.CM_HEAT, HbmBlocks.CUSTOM_MACHINE_ANCHOR)) {
            if (block.get().asItem() instanceof LegacyVariantBlockItem variants) variants.addCreativeVariants(output);
            else acceptBlock(output, block);
        }
    }

    private static void addBuilding(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output rawOutput) {
        var seen = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
        CreativeModeTab.Output output = (stack, visibility) -> {
            if (!LegacyContentVisibility.isCreativeHidden(stack) && seen.add(stack)) {
                rawOutput.accept(stack, visibility);
            }
        };
        for (var block : HbmBlocks.BUILDING_BLOCKS) {
            var item = BuiltInRegistries.ITEM.get(block.getId());
            if (item instanceof ConcreteColoredBlockItem variants) {
                variants.addCreativeVariants(output);
            } else if (item instanceof MetalFenceBlockItem variants) {
                variants.addCreativeVariants(output);
            } else if (item instanceof LegacyVariantBlockItem variants) {
                variants.addCreativeVariants(output);
            } else if (item instanceof com.atomiclegacy.items.GlyphBlockItem variants) {
                variants.addCreativeVariants(output);
            } else if (item instanceof com.atomiclegacy.items.TrapBlockItem variants) {
                variants.addCreativeVariants(output);
            } else if (item instanceof com.atomiclegacy.items.CapBlockItem variants) {
                variants.addCreativeVariants(output);
            } else {
                acceptDecorationApplianceBlock(output, block);
            }
        }
    }

    private static void addDoors(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : HbmBlocks.DOOR_BLOCKS) {
            acceptBlock(output, block);
        }
    }

    private static void addTools(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var anvil : HbmBlocks.ANVIL_BLOCKS) {
            acceptBlock(output, anvil);
        }
        for (var tool : HbmItems.TOOL_ITEMS) {
            if (isLegacySpawnItem(tool)) {
                continue;
            }
            if (tool.get() instanceof BlueprintItem blueprints) {
                blueprints.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.ConveyorWandItem conveyorWand) {
                conveyorWand.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.BlueprintFolderItem folders) {
                folders.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.SirenTrackItem tracks) {
                tracks.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.GuideBookItem guideBook) {
                guideBook.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.LegacyHolotapeImageItem holotapes) {
                holotapes.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.LegacyBombCallerItem bombCaller) {
                bombCaller.addCreativeVariants(output);
            } else if (tool.get() instanceof com.atomiclegacy.items.LegacyDroneItem drones) {
                drones.addCreativeVariants(output);
            } else {
                output.accept(tool);
            }
        }
        if (HbmBlocks.WAND_STRUCTURE.asItem() instanceof com.atomiclegacy.items.WandStructureBlockItem item) {
            item.addCreativeVariants(output::accept);
        }
        output.accept(HbmBlocks.WAND_AIR);
        output.accept(HbmBlocks.WAND_JIGSAW);
        output.accept(HbmBlocks.WAND_TANDEM);
        output.accept(HbmBlocks.WAND_LOOT);
        output.accept(HbmBlocks.WAND_LOGIC);
        output.accept(HbmBlocks.DECO_LOOT);
        output.accept(HbmBlocks.LOGIC_BLOCK);
        output.accept(HbmBlocks.MACHINE_KEYFORGE);
        output.accept(HbmItems.DOSIMETER);
        output.accept(HbmItems.GEIGER_COUNTER);
    }

    private static void addArmor(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var armor : HbmItems.ARMOR_ITEMS) {
            output.accept(armor);
        }
    }

    private static void addSpawnEggs(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var egg : HbmItems.GLYPHID_SPAWN_EGGS) {
            output.accept(egg);
        }
        for (var egg : HbmItems.MOB_SPAWN_EGGS) {
            output.accept(egg);
        }
        // The 1.7.10 port also has four standalone entity
        // spawners which are not SpawnEggItem instances.
        for (var tool : HbmItems.TOOL_ITEMS) {
            if (isLegacySpawnItem(tool)) {
                outputLegacyItem(output, tool);
            }
        }
        for (var item : HbmItems.PORTED_PLAIN_ITEMS) {
            if (isLegacySpawnItem(item)) {
                outputLegacyItem(output, item);
            }
        }
    }

    private static void addMiscellaneous(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var crop : HbmItems.CROP_ITEMS) {
            output.accept(crop);
        }
        for (var item : HbmItems.PORTED_PLAIN_ITEMS) {
            if (item != HbmItems.SINGULARITY
                    && item != HbmItems.SINGULARITY_COUNTER_RESONANT
                    && item != HbmItems.SINGULARITY_SUPER_HEATED
                    && item != HbmItems.BLACK_HOLE
                    && !HbmItems.isHiddenPortedPlainItem(item)
                    && !isPetroleumLegacyItem(item)
                    && !isLegacySpawnItem(item)) {
                outputLegacyItem(output, item);
            }
        }
        if (HbmBlocks.VENDING_MACHINE.get().asItem() instanceof com.atomiclegacy.items.VendingMachineBlockItem vending) {
            vending.addCreativeVariants(output);
        }
    }

    private static void addLegacyBlocks(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var block : LegacyHbmContent.LEGACY_BLOCKS) {
            if (!isPetroleumLegacyBlock(block)) {
                acceptBlock(output, block);
            }
        }
    }

    // [parity L1: batteries] ItemBatteryPack.getSubItems lists makeEmptyBattery then makeFullBattery for every variant.
    private static void acceptEmptyAndFull(CreativeModeTab.Output output, Supplier<? extends Item> item) {
        ItemStack empty = new ItemStack(item.get());
        BatteryPackItem.setStoredCharge(empty, 0L);
        output.accept(empty);
        ItemStack full = new ItemStack(item.get());
        BatteryPackItem.setStoredCharge(full, BatteryPackItem.capacity(full));
        output.accept(full);
    }

    private HbmCreativeTabs() {
    }

    /**
     * Resolve a block item by registry id instead of {@link Block#asItem()}.
     *
     * <p>Several ported blocks use a dedicated BlockItem registered separately
     * from the block (for example the radiation-sealed fluid duct).  Their
     * block-to-item map is not a reliable source while creative tab contents
     * are being assembled, even though the item registry entry already exists.
     * Looking up the shared id keeps those legitimate items visible to both
     * the creative inventory and JEI while still skipping structure-only
     * blocks which intentionally have no item.</p>
     */
    private static void acceptBlock(CreativeModeTab.Output output, DeferredBlock<? extends Block> block) {
        Item item = BuiltInRegistries.ITEM.get(block.getId());
        if (item != null && item != Items.AIR) {
            output.accept(item);
        }
    }

    private static void acceptDecorationApplianceBlock(CreativeModeTab.Output output,
                                                       DeferredBlock<? extends Block> block) {
        Item item = BuiltInRegistries.ITEM.get(block.getId());
        if (item == Items.AIR) {
            return;
        }
        if (item instanceof ToasterBlockItem toaster) {
            toaster.addCreativeVariants(output);
        } else if (item instanceof com.atomiclegacy.items.BobbleheadBlockItem bobblehead) {
            bobblehead.addCreativeVariants(output);
        } else if (item instanceof com.atomiclegacy.items.SnowglobeBlockItem snowglobe) {
            snowglobe.addCreativeVariants(output);
        } else if (item instanceof com.atomiclegacy.items.PlushieBlockItem plushie) {
            plushie.addCreativeVariants(output);
        } else if (item instanceof com.atomiclegacy.items.DecoCrtBlockItem crt) {
            crt.addCreativeVariants(output);
        } else if (item instanceof com.atomiclegacy.items.FilingCabinetBlockItem filingCabinet) {
            filingCabinet.addCreativeVariants(output);
        } else {
            output.accept(item);
        }
    }

    private static void outputLegacyItem(CreativeModeTab.Output output, net.neoforged.neoforge.registries.DeferredItem<Item> item) {
        if (item.get() instanceof com.atomiclegacy.items.LegacyConserveItem conserve) {
            conserve.addCreativeVariants(output);
        } else if (item.get() instanceof com.atomiclegacy.items.LegacyCrayonItem crayon) {
            crayon.addCreativeVariants(output);
        } else if (item.get() instanceof com.atomiclegacy.items.LegacySpecialFoodItem food) {
            food.addCreativeVariants(output);
        } else if (item.get() instanceof com.atomiclegacy.items.ModMinecartItem carts) { // [parity S1: carts] ItemModMinecart.getSubItems
            carts.addCreativeVariants(output);
        } else if (item.get() instanceof LegacyVariantItem variant) {
            variant.addCreativeVariants(output);
        } else if (item.get() instanceof com.atomiclegacy.items.UniversalGrenadeItem grenade) {
            grenade.addCreativeVariants(output);
        } else if (item.get() instanceof com.atomiclegacy.items.SoyuzItem soyuz) { // [parity P2: soyuz skins]
            soyuz.addCreativeVariants(output);
        } else {
            output.accept(item);
        }
    }

    private static final Set<String> PETROLEUM_LEGACY_BLOCKS = Set.of(
            "machine_coker",
            "machine_diesel"
    );

    private static final Set<String> PETROLEUM_LEGACY_ITEMS = Set.of(
            "oil_detector",
            "scrap_oil"
    );

    private static final Set<String> LEGACY_SPAWN_ITEM_IDS = Set.of(
            "spawn_duck",
            "chopper",
            "spawn_ufo",
            "spawn_worm"
    );

    private static boolean isPetroleumBlock(DeferredBlock<Block> block) {
        return block == HbmBlocks.ORE_OIL
                || block == HbmBlocks.ORE_OIL_EMPTY
                || block == HbmBlocks.ORE_OIL_SAND
                || block == HbmBlocks.ORE_BEDROCK_OIL
                || block == HbmBlocks.ORE_DEEPSLATE_OIL
                || block == HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY;
    }

    private static boolean isPetroleumLegacyBlock(DeferredBlock<Block> block) {
        return PETROLEUM_LEGACY_BLOCKS.contains(block.getId().getPath());
    }

    private static boolean isPetroleumItem(DeferredItem<Item> item) {
        return HbmItems.OIL_TAR_ITEMS.containsValue(item)
                || item == HbmItems.CANISTER_LUBRICANT
                || item == HbmItems.SOLID_FUEL
                || item == HbmItems.SOLID_FUEL_BF
                || item == HbmItems.ROCKET_FUEL
                || HbmItems.FUEL_ADDITIVE_ITEMS.containsValue(item);
    }

    private static boolean isPetroleumLegacyItem(DeferredItem<Item> item) {
        return PETROLEUM_LEGACY_ITEMS.contains(item.getId().getPath());
    }

    private static boolean isLegacySpawnItem(DeferredItem<Item> item) {
        return LEGACY_SPAWN_ITEM_IDS.contains(item.getId().getPath());
    }

    private static boolean isNuclearBilletItem(DeferredItem<Item> item) {
        return HbmItems.NUCLEAR_BILLETS.contains(item);
    }

    private static void acceptFluidContainerVariants(CreativeModeTab.Output output, HbmFluidContainerItem container) {
        if (!container.isFilledContainer()) {
            output.accept(container);
            return;
        }
        for (var definition : HbmFluids.niceOrder()) {
            if (container.kind().allows(definition)) {
                output.accept(container.filledStack(definition));
            }
        }
    }

    /**
     * Remove legacy internal states after every tab has finished contributing
     * its entries.  Filtering at this event boundary covers direct accepts,
     * generated variants, the search tab, and entries contributed by another
     * compatibility handler without duplicating the rule in every tab builder.
     */
    private static void filterLegacyHiddenContent(BuildCreativeModeTabContentsEvent event) {
        var key = event.getTabKey();
        if (key.equals(net.minecraft.world.item.CreativeModeTabs.COMBAT)) display("combat", event.getParameters(), event);
        if (key.equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) display("tools", event.getParameters(), event);
        if (key.equals(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS)) display("ingredients", event.getParameters(), event);
        var hidden = new ArrayList<ItemStack>();
        for (ItemStack stack : event.getParentEntries()) {
            if (LegacyContentVisibility.isCreativeHidden(stack)) {
                hidden.add(stack);
            }
        }
        for (ItemStack stack : event.getSearchEntries()) {
            if (LegacyContentVisibility.isCreativeHidden(stack)) {
                hidden.add(stack);
            }
        }
        for (ItemStack stack : hidden) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
        eventBus.addListener(HbmCreativeTabs::filterLegacyHiddenContent);
    }
}
