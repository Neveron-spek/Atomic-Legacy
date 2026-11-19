package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.AssemblyFactoryBlockEntity;
import com.atomiclegacy.tileentity.AssemblyMachineBlockEntity;
import com.atomiclegacy.tileentity.AirCompressorBlockEntity;
import com.atomiclegacy.tileentity.AmmoPressBlockEntity;
import com.atomiclegacy.tileentity.ArcWelderBlockEntity;
import com.atomiclegacy.tileentity.ArcFurnaceBlockEntity;
import com.atomiclegacy.tileentity.AutocalBlockEntity;
import com.atomiclegacy.tileentity.StorageDrumBlockEntity;
import com.atomiclegacy.tileentity.SupplyCrateBlockEntity;
import com.atomiclegacy.tileentity.SupercomputerBlockEntity;
import com.atomiclegacy.tileentity.TaczAmmoAssemblerBlockEntity;
import com.atomiclegacy.tileentity.VendingMachineBlockEntity;
import com.atomiclegacy.tileentity.AshpitBlockEntity;
import com.atomiclegacy.tileentity.BatteryReddBlockEntity;
import com.atomiclegacy.tileentity.BatterySocketBlockEntity;
import com.atomiclegacy.tileentity.ChargerBlockEntity;
import com.atomiclegacy.tileentity.BrickFurnaceBlockEntity;
import com.atomiclegacy.tileentity.BedrockOreBlockEntity;
import com.atomiclegacy.tileentity.BigAssTankBlockEntity;
import com.atomiclegacy.tileentity.BlastDoorBlockEntity;
import com.atomiclegacy.tileentity.BlastDoorDummyBlockEntity;
import com.atomiclegacy.tileentity.BreederReactorBlockEntity;
import com.atomiclegacy.tileentity.BroadcasterBlockEntity;
import com.atomiclegacy.tileentity.GeigerBlockEntity;
import com.atomiclegacy.tileentity.BobbleheadBlockEntity;
import com.atomiclegacy.tileentity.SnowglobeBlockEntity;
import com.atomiclegacy.tileentity.PlushieBlockEntity;
import com.atomiclegacy.tileentity.LanternBlockEntity;
import com.atomiclegacy.tileentity.LanternBehemothBlockEntity;
import com.atomiclegacy.tileentity.CatalyticCrackerBlockEntity;
import com.atomiclegacy.tileentity.CatalyticReformerBlockEntity;
import com.atomiclegacy.tileentity.HydrotreaterBlockEntity;
import com.atomiclegacy.tileentity.ChimneyBlockEntity;
import com.atomiclegacy.tileentity.CentrifugeBlockEntity;
import com.atomiclegacy.tileentity.ChemicalFactoryBlockEntity;
import com.atomiclegacy.tileentity.ChemicalPlantBlockEntity;
import com.atomiclegacy.tileentity.CableSwitchBlockEntity;
import com.atomiclegacy.tileentity.CableDiodeBlockEntity;
import com.atomiclegacy.tileentity.PaintableCableBlockEntity;
import com.atomiclegacy.tileentity.PowerGaugeBlockEntity;
import com.atomiclegacy.tileentity.CombustionEngineBlockEntity;
import com.atomiclegacy.tileentity.CompressorBlockEntity;
import com.atomiclegacy.tileentity.CustomMachineBlockEntity;
import com.atomiclegacy.tileentity.ConveyorPressBlockEntity;
import com.atomiclegacy.tileentity.CraneMachineBlockEntity;
import com.atomiclegacy.tileentity.CokerBlockEntity;
import com.atomiclegacy.tileentity.CoolingTowerBlockEntity;
import com.atomiclegacy.tileentity.CrystallizerBlockEntity;
import com.atomiclegacy.tileentity.CyclotronBlockEntity;
import com.atomiclegacy.tileentity.DeuteriumExtractorBlockEntity;
import com.atomiclegacy.tileentity.DeuteriumTowerBlockEntity;
import com.atomiclegacy.tileentity.DfcCoreBlockEntity;
import com.atomiclegacy.tileentity.DemonLampBlockEntity;
import com.atomiclegacy.tileentity.DecorationEmitterBlockEntity;
import com.atomiclegacy.tileentity.PartEmitterBlockEntity;
import com.atomiclegacy.tileentity.DungeonSpawnerBlockEntity;
import com.atomiclegacy.tileentity.DfcEmitterBlockEntity;
import com.atomiclegacy.tileentity.DfcInjectorBlockEntity;
import com.atomiclegacy.tileentity.DfcReceiverBlockEntity;
import com.atomiclegacy.tileentity.DfcStabilizerBlockEntity;
import com.atomiclegacy.tileentity.ElectricFurnaceBlockEntity;
import com.atomiclegacy.tileentity.ElectrolyzerBlockEntity;
import com.atomiclegacy.tileentity.ExcavatorBlockEntity;
import com.atomiclegacy.tileentity.ExposureChamberBlockEntity;
import com.atomiclegacy.tileentity.FelBlockEntity;
import com.atomiclegacy.tileentity.DieselGeneratorBlockEntity;
import com.atomiclegacy.tileentity.DeconBlockEntity;
import com.atomiclegacy.tileentity.DrainBlockEntity;
import com.atomiclegacy.tileentity.DroneWaypointBlockEntity;
import com.atomiclegacy.tileentity.DroneRequestWaypointBlockEntity;
import com.atomiclegacy.tileentity.DroneCrateBlockEntity;
import com.atomiclegacy.tileentity.DroneDockBlockEntity;
import com.atomiclegacy.tileentity.DroneProviderBlockEntity;
import com.atomiclegacy.tileentity.DroneRequesterBlockEntity;
import com.atomiclegacy.tileentity.FluidBarrelBlockEntity;
import com.atomiclegacy.tileentity.FluidPipeBlockEntity;
import com.atomiclegacy.tileentity.PipeAnchorBlockEntity;
import com.atomiclegacy.tileentity.PistonInserterBlockEntity;
import com.atomiclegacy.tileentity.PneumaticStorageBlockEntity;
import com.atomiclegacy.tileentity.FluidPumpBlockEntity;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import com.atomiclegacy.tileentity.FractionSpacerBlockEntity;
import com.atomiclegacy.tileentity.FractionTowerBlockEntity;
import com.atomiclegacy.tileentity.FunnelBlockEntity;
import com.atomiclegacy.tileentity.SirenBlockEntity;
import com.atomiclegacy.tileentity.FoundryCastingBlockEntity;
import com.atomiclegacy.tileentity.FoundryFlowBlockEntity;
import com.atomiclegacy.tileentity.FoundrySlagBlockEntity;
import com.atomiclegacy.tileentity.FoundryTankBlockEntity;
import com.atomiclegacy.tileentity.FurnaceCombinationBlockEntity;
import com.atomiclegacy.tileentity.FireworksBlockEntity;
import com.atomiclegacy.tileentity.FrackingTowerBlockEntity;
import com.atomiclegacy.tileentity.FissureBlockEntity;
import com.atomiclegacy.tileentity.FusionMachineBlockEntity;
import com.atomiclegacy.tileentity.FusionTorusStructBlockEntity;
import com.atomiclegacy.tileentity.GasCentrifugeBlockEntity;
import com.atomiclegacy.tileentity.GasFlareBlockEntity;
import com.atomiclegacy.tileentity.GasTurbineBlockEntity;
import com.atomiclegacy.tileentity.GlyphidSpawnerBlockEntity;
import com.atomiclegacy.tileentity.CyberCrabSpawnerBlockEntity;
import com.atomiclegacy.tileentity.GroundwaterPumpBlockEntity;
import com.atomiclegacy.tileentity.GeothermalHeatExchangerBlockEntity;
import com.atomiclegacy.tileentity.HeatBoilerBlockEntity;
import com.atomiclegacy.tileentity.HbmHeavyDoorBlockEntity;
import com.atomiclegacy.tileentity.HbmHeavyDoorPartBlockEntity;
import com.atomiclegacy.tileentity.HeaterBlockEntity;
import com.atomiclegacy.tileentity.HexafluorideTankBlockEntity;
import com.atomiclegacy.tileentity.CrucibleBlockEntity;
import com.atomiclegacy.tileentity.IndustrialTurbineBlockEntity;
import com.atomiclegacy.tileentity.IcfPressBlockEntity;
import com.atomiclegacy.tileentity.IcfCoreBlockEntity;
import com.atomiclegacy.tileentity.IcfStructBlockEntity;
import com.atomiclegacy.tileentity.IcfControllerBlockEntity;
import com.atomiclegacy.tileentity.IcfAssembledLaserBlockEntity;
import com.atomiclegacy.tileentity.IronFurnaceBlockEntity;
import com.atomiclegacy.tileentity.IndustrialBoilerBlockEntity;
import com.atomiclegacy.tileentity.LargeTurbineBlockEntity;
import com.atomiclegacy.tileentity.LargeFluidTankBlockEntity;
import com.atomiclegacy.tileentity.LauncherBlockEntity;
import com.atomiclegacy.tileentity.LauncherStructCoreBlockEntity;
import com.atomiclegacy.tileentity.LegacyTurretBlockEntity;
import com.atomiclegacy.tileentity.LeviathanTurbineBlockEntity;
import com.atomiclegacy.tileentity.LiquefactorBlockEntity;
import com.atomiclegacy.tileentity.LandmineBlockEntity;
import com.atomiclegacy.tileentity.WallChargeBlockEntity;
import com.atomiclegacy.tileentity.LegacyEmitterBlockEntity;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.MachineBlastFurnaceBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.tileentity.MachineKeyForgeBlockEntity;
import com.atomiclegacy.tileentity.SatelliteLinkerBlockEntity;
import com.atomiclegacy.tileentity.SatelliteDockBlockEntity;
import com.atomiclegacy.tileentity.SatelliteGroundStationBlockEntity;
import com.atomiclegacy.tileentity.TapeDriveBlockEntity;
import com.atomiclegacy.tileentity.SatelliteDockDummyBlockEntity;
import com.atomiclegacy.tileentity.MiningLaserBlockEntity;
import com.atomiclegacy.tileentity.MixerBlockEntity;
import com.atomiclegacy.tileentity.MicrowaveBlockEntity;
import com.atomiclegacy.tileentity.NukeBoyBlockEntity;
import com.atomiclegacy.tileentity.LegacyNukeBlockEntity;
import com.atomiclegacy.tileentity.BombMultiBlockEntity;
import com.atomiclegacy.tileentity.CrashedBombBlockEntity;
import com.atomiclegacy.tileentity.OilDerrickBlockEntity;
import com.atomiclegacy.tileentity.OreSlopperBlockEntity;
import com.atomiclegacy.tileentity.RockMillBlockEntity;
import com.atomiclegacy.tileentity.ParticleAcceleratorBlockEntity;
import com.atomiclegacy.tileentity.PileGraphiteBlockEntity;
import com.atomiclegacy.tileentity.PileControlBlockEntity;
import com.atomiclegacy.tileentity.PileLoaderBlockEntity;
import com.atomiclegacy.tileentity.PileMk2CoreBlockEntity;
import com.atomiclegacy.tileentity.PileVentBlockEntity;
import com.atomiclegacy.tileentity.PowerPylonBlockEntity;
import com.atomiclegacy.tileentity.PoleBlockEntity;
import com.atomiclegacy.tileentity.PoweredSteamCondenserBlockEntity;
import com.atomiclegacy.tileentity.PressBlockEntity;
import com.atomiclegacy.tileentity.PowerDetectorBlockEntity;
import com.atomiclegacy.tileentity.PurexBlockEntity;
import com.atomiclegacy.tileentity.PwrBlockEntity;
import com.atomiclegacy.tileentity.PwrControllerBlockEntity;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import com.atomiclegacy.tileentity.RadarScreenBlockEntity;
import com.atomiclegacy.tileentity.RefineryBlockEntity;
import com.atomiclegacy.tileentity.RebarBlockEntity;
import com.atomiclegacy.tileentity.ReactorControlBlockEntity;
import com.atomiclegacy.tileentity.RotaryFurnaceBlockEntity;
import com.atomiclegacy.tileentity.VacuumDistillBlockEntity;
import com.atomiclegacy.tileentity.VolcanoCoreBlockEntity;
import com.atomiclegacy.tileentity.WasteDrumBlockEntity;
import com.atomiclegacy.tileentity.DecoLootBlockEntity;
import com.atomiclegacy.tileentity.DecoDisplayBlockEntity;
import com.atomiclegacy.tileentity.FilingCabinetBlockEntity;
import com.atomiclegacy.tileentity.LogicBlockEntity;
import com.atomiclegacy.tileentity.WandJigsawBlockEntity;
import com.atomiclegacy.tileentity.WandLogicBlockEntity;
import com.atomiclegacy.tileentity.WandLootBlockEntity;
import com.atomiclegacy.tileentity.WandStructureBlockEntity;
import com.atomiclegacy.tileentity.WandTandemBlockEntity;
import com.atomiclegacy.tileentity.ResearchReactorBlockEntity;
import com.atomiclegacy.tileentity.ShredderBlockEntity;
import com.atomiclegacy.tileentity.SolderingStationBlockEntity;
import com.atomiclegacy.tileentity.SilexBlockEntity;
import com.atomiclegacy.tileentity.SolidifierBlockEntity;
import com.atomiclegacy.tileentity.SolarBoilerBlockEntity;
import com.atomiclegacy.tileentity.SolarMirrorBlockEntity;
import com.atomiclegacy.tileentity.SoyuzCapsuleBlockEntity;
import com.atomiclegacy.tileentity.LambdaLaunchpadBlockEntity;
import com.atomiclegacy.tileentity.SoyuzLaunchpadBlockEntity;
import com.atomiclegacy.tileentity.SteamCondenserBlockEntity;
import com.atomiclegacy.tileentity.SteamEngineBlockEntity;
import com.atomiclegacy.tileentity.SteelFurnaceBlockEntity;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import com.atomiclegacy.tileentity.TapeRecorderBlockEntity;
import com.atomiclegacy.tileentity.TeslaCoilBlockEntity;
import com.atomiclegacy.tileentity.SafeBlockEntity;
import com.atomiclegacy.tileentity.MassStorageBlockEntity;
import com.atomiclegacy.tileentity.StrandCasterBlockEntity;
import com.atomiclegacy.tileentity.StirlingGeneratorBlockEntity;
import com.atomiclegacy.tileentity.TurretChekhovBlockEntity;
import com.atomiclegacy.tileentity.TurretJeremyBlockEntity;
import com.atomiclegacy.tileentity.WatzBlockEntity;
import com.atomiclegacy.tileentity.WatzPumpBlockEntity;
import com.atomiclegacy.tileentity.WatzStructBlockEntity;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import com.atomiclegacy.tileentity.RadioRecBlockEntity;
import com.atomiclegacy.tileentity.RadioTelexBlockEntity;
import com.atomiclegacy.tileentity.RadioboxBlockEntity;
import com.atomiclegacy.tileentity.RefuelerBlockEntity;
import com.atomiclegacy.tileentity.WoodBurnerBlockEntity;
import com.atomiclegacy.tileentity.ZirnoxDestroyedBlockEntity;
import com.atomiclegacy.tileentity.ZirnoxReactorBlockEntity;
import com.atomiclegacy.tileentity.SpotlightBeamBlockEntity;
import com.atomiclegacy.tileentity.SpotlightBlockEntity;
import com.atomiclegacy.tileentity.FanBlockEntity;
import com.atomiclegacy.tileentity.FloodlightBeamBlockEntity;
import com.atomiclegacy.tileentity.FloodlightBlockEntity;
import com.atomiclegacy.tileentity.FloodlightDummyBlockEntity;
import com.atomiclegacy.tileentity.CargoElevatorBlockEntity;
import com.atomiclegacy.tileentity.LegacyDisplayStandBlockEntity;
import com.atomiclegacy.tileentity.SealHatchBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AtomicLegacy.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodBurnerBlockEntity>> WOOD_BURNER =
            BLOCK_ENTITIES.register(
                    "machine_wood_burner",
                    () -> BlockEntityType.Builder.of(
                            WoodBurnerBlockEntity::new,
                            HbmBlocks.MACHINE_WOOD_BURNER.get()
                    ).build(null)
            );

    // [parity R2: pneumatic tube mechanics] TileEntityPneumoTube (plain and paintable tube)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.PneumaticTubeBlockEntity>> PNEUMATIC_TUBE =
            BLOCK_ENTITIES.register(
                    "pneumatic_tube",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.PneumaticTubeBlockEntity::new,
                            HbmBlocks.PNEUMATIC_TUBE.get(),
                            HbmBlocks.PNEUMATIC_TUBE_PAINTABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeAnchorBlockEntity>> PIPE_ANCHOR =
            BLOCK_ENTITIES.register(
                    "pipe_anchor",
                    () -> BlockEntityType.Builder.of(
                            PipeAnchorBlockEntity::new,
                            HbmBlocks.PIPE_ANCHOR.get(),
                            HbmBlocks.PIPE_ANCHOR_EXHAUST.get(), // [parity T1: pipe_anchor_exhaust]
                            HbmBlocks.PIPE_ANCHOR_PNEUMATIC.get() // [parity V2: pipe_anchor_pneumatic]
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PistonInserterBlockEntity>> PISTON_INSERTER =
            BLOCK_ENTITIES.register(
                    "piston_inserter",
                    () -> BlockEntityType.Builder.of(
                            PistonInserterBlockEntity::new,
                            HbmBlocks.PISTON_INSERTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PileGraphiteBlockEntity>> PILE_GRAPHITE =
            BLOCK_ENTITIES.register(
                    "pile_graphite",
                    () -> BlockEntityType.Builder.of(
                            PileGraphiteBlockEntity::new,
                            HbmBlocks.BLOCK_GRAPHITE_FUEL.get(),
                            HbmBlocks.BLOCK_GRAPHITE_PLUTONIUM.get(),
                            HbmBlocks.BLOCK_GRAPHITE_SOURCE.get(),
                            HbmBlocks.BLOCK_GRAPHITE_LITHIUM.get(),
                            HbmBlocks.BLOCK_GRAPHITE_DETECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioTorchBlockEntity>> RADIO_TORCH =
            BLOCK_ENTITIES.register(
                    "radio_torch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new RadioTorchBlockEntity(
                                    pos,
                                    state,
                                    state.getBlock() instanceof com.atomiclegacy.blocks.RadioTorchBlock radio
                                            ? radio.kind()
                                            : com.atomiclegacy.blocks.RadioTorchBlock.Kind.RECEIVER),
                            HbmBlocks.RADIO_TORCH_SENDER.get(),
                            HbmBlocks.RADIO_TORCH_RECEIVER.get(),
                            HbmBlocks.RADIO_TORCH_COUNTER.get(),
                            HbmBlocks.RADIO_TORCH_LOGIC.get(),
                            HbmBlocks.RADIO_TORCH_READER.get(),
                            HbmBlocks.RADIO_TORCH_CONTROLLER.get(),
                            HbmBlocks.RADIO_TORCH_RELAY.get() // [parity R2: radio_torch_relay]
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpotlightBeamBlockEntity>> SPOTLIGHT_BEAM =
            BLOCK_ENTITIES.register(
                    "spotlight_beam",
                    () -> BlockEntityType.Builder.of(
                            SpotlightBeamBlockEntity::new,
                            HbmBlocks.SPOTLIGHT_BEAM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpotlightBlockEntity>> SPOTLIGHT =
            BLOCK_ENTITIES.register(
                    "spotlight",
                    () -> BlockEntityType.Builder.of(
                            SpotlightBlockEntity::new,
                            HbmBlocks.SPOTLIGHT_INCANDESCENT.get(),
                            HbmBlocks.SPOTLIGHT_INCANDESCENT_OFF.get(),
                            HbmBlocks.SPOTLIGHT_FLUORO.get(),
                            HbmBlocks.SPOTLIGHT_FLUORO_OFF.get(),
                            HbmBlocks.SPOTLIGHT_HALOGEN.get(),
                            HbmBlocks.SPOTLIGHT_HALOGEN_OFF.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FanBlockEntity>> FAN =
            BLOCK_ENTITIES.register(
                    "fan",
                    () -> BlockEntityType.Builder.of(FanBlockEntity::new, HbmBlocks.FAN.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightBlockEntity>> FLOODLIGHT =
            BLOCK_ENTITIES.register(
                    "floodlight",
                    () -> BlockEntityType.Builder.of(FloodlightBlockEntity::new, HbmBlocks.FLOODLIGHT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightDummyBlockEntity>> FLOODLIGHT_DUMMY =
            BLOCK_ENTITIES.register(
                    "floodlight_dummy",
                    () -> BlockEntityType.Builder.of(FloodlightDummyBlockEntity::new, HbmBlocks.FLOODLIGHT_DUMMY.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightBeamBlockEntity>> FLOODLIGHT_BEAM =
            BLOCK_ENTITIES.register(
                    "floodlight_beam",
                    () -> BlockEntityType.Builder.of(FloodlightBeamBlockEntity::new, HbmBlocks.FLOODLIGHT_BEAM.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoElevatorBlockEntity>> CARGO_ELEVATOR =
            BLOCK_ENTITIES.register(
                    "cargo_elevator",
                    () -> BlockEntityType.Builder.of(CargoElevatorBlockEntity::new, HbmBlocks.CARGO_ELEVATOR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LandmineBlockEntity>> LANDMINE =
            BLOCK_ENTITIES.register(
                    "landmine",
                    () -> BlockEntityType.Builder.of(
                            LandmineBlockEntity::new,
                            HbmBlocks.MINE_AP.get(),
                            HbmBlocks.MINE_HE.get(),
                            HbmBlocks.MINE_SHRAP.get(),
                            HbmBlocks.MINE_FAT.get(),
                            HbmBlocks.MINE_NAVAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallChargeBlockEntity>> WALL_CHARGE =
            BLOCK_ENTITIES.register(
                    "wall_charge",
                    () -> BlockEntityType.Builder.of(
                            WallChargeBlockEntity::new,
                            HbmBlocks.CHARGE_DYNAMITE.get(),
                            HbmBlocks.CHARGE_MINER.get(),
                            HbmBlocks.CHARGE_C4.get(),
                            HbmBlocks.CHARGE_SEMTEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_electric_furnace",
                    () -> BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_ELECTRIC_FURNACE_OFF.get(),
                            HbmBlocks.MACHINE_ELECTRIC_FURNACE_ON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShredderBlockEntity>> SHREDDER =
            BLOCK_ENTITIES.register(
                    "machine_shredder",
                    () -> BlockEntityType.Builder.of(
                            ShredderBlockEntity::new,
                            HbmBlocks.MACHINE_SHREDDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicrowaveBlockEntity>> MICROWAVE =
            BLOCK_ENTITIES.register(
                    "machine_microwave",
                    () -> BlockEntityType.Builder.of(
                            MicrowaveBlockEntity::new,
                            HbmBlocks.MACHINE_MICROWAVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BroadcasterBlockEntity>> BROADCASTER =
            BLOCK_ENTITIES.register(
                    "broadcaster_pc",
                    () -> BlockEntityType.Builder.of(BroadcasterBlockEntity::new, HbmBlocks.BROADCASTER_PC.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DemonLampBlockEntity>> DEMON_LAMP =
            BLOCK_ENTITIES.register("lamp_demon",
                    () -> BlockEntityType.Builder.of(DemonLampBlockEntity::new, HbmBlocks.LAMP_DEMON.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioboxBlockEntity>> RADIOBOX =
            BLOCK_ENTITIES.register("radiobox",
                    () -> BlockEntityType.Builder.of(RadioboxBlockEntity::new, HbmBlocks.RADIOBOX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioRecBlockEntity>> RADIOREC =
            BLOCK_ENTITIES.register("radiorec",
                    () -> BlockEntityType.Builder.of(RadioRecBlockEntity::new, HbmBlocks.RADIOREC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutocalBlockEntity>> AUTOCAL =
            BLOCK_ENTITIES.register("radio_autocal",
                    () -> BlockEntityType.Builder.of(AutocalBlockEntity::new, HbmBlocks.RADIO_AUTOCAL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioTelexBlockEntity>> RADIO_TELEX =
            BLOCK_ENTITIES.register("radio_telex",
                    () -> BlockEntityType.Builder.of(RadioTelexBlockEntity::new, HbmBlocks.RADIO_TELEX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefuelerBlockEntity>> REFUELER =
            BLOCK_ENTITIES.register("refueler",
                    () -> BlockEntityType.Builder.of(RefuelerBlockEntity::new, HbmBlocks.REFUELER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecorationEmitterBlockEntity>> DECO_EMITTER =
            BLOCK_ENTITIES.register("deco_emitter",
                    () -> BlockEntityType.Builder.of(DecorationEmitterBlockEntity::new, HbmBlocks.DECO_EMITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PartEmitterBlockEntity>> PART_EMITTER =
            BLOCK_ENTITIES.register("part_emitter",
                    () -> BlockEntityType.Builder.of(PartEmitterBlockEntity::new, HbmBlocks.PART_EMITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DungeonSpawnerBlockEntity>> DUNGEON_SPAWNER =
            BLOCK_ENTITIES.register("dungeon_spawner",
                    () -> BlockEntityType.Builder.of(DungeonSpawnerBlockEntity::new, HbmBlocks.DUNGEON_SPAWNER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeigerBlockEntity>> GEIGER =
            BLOCK_ENTITIES.register(
                    "geiger",
                    () -> BlockEntityType.Builder.of(GeigerBlockEntity::new, HbmBlocks.GEIGER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BobbleheadBlockEntity>> BOBBLEHEAD =
            BLOCK_ENTITIES.register(
                    "bobblehead",
                    () -> BlockEntityType.Builder.of(BobbleheadBlockEntity::new, HbmBlocks.BOBBLEHEAD.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SnowglobeBlockEntity>> SNOWGLOBE =
            BLOCK_ENTITIES.register(
                    "snowglobe",
                    () -> BlockEntityType.Builder.of(SnowglobeBlockEntity::new, HbmBlocks.SNOWGLOBE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlushieBlockEntity>> PLUSHIE =
            BLOCK_ENTITIES.register(
                    "plushie",
                    () -> BlockEntityType.Builder.of(PlushieBlockEntity::new, HbmBlocks.PLUSHIE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanternBlockEntity>> LANTERN =
            BLOCK_ENTITIES.register(
                    "lantern",
                    () -> BlockEntityType.Builder.of(LanternBlockEntity::new, HbmBlocks.LANTERN.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanternBehemothBlockEntity>> LANTERN_BEHEMOTH =
            BLOCK_ENTITIES.register(
                    "lantern_behemoth",
                    () -> BlockEntityType.Builder.of(LanternBehemothBlockEntity::new,
                            HbmBlocks.LANTERN_BEHEMOTH.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineKeyForgeBlockEntity>> MACHINE_KEYFORGE =
            BLOCK_ENTITIES.register(
                    "machine_keyforge",
                    () -> BlockEntityType.Builder.of(
                            MachineKeyForgeBlockEntity::new,
                            HbmBlocks.MACHINE_KEYFORGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteLinkerBlockEntity>> SATELLITE_LINKER =
            BLOCK_ENTITIES.register(
                    "machine_satlinker",
                    () -> BlockEntityType.Builder.of(
                            SatelliteLinkerBlockEntity::new,
                            HbmBlocks.MACHINE_SATLINKER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteDockBlockEntity>> SAT_DOCK =
            BLOCK_ENTITIES.register(
                    "sat_dock",
                    () -> BlockEntityType.Builder.of(SatelliteDockBlockEntity::new, HbmBlocks.SAT_DOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteDockDummyBlockEntity>> SAT_DOCK_DUMMY =
            BLOCK_ENTITIES.register(
                    "dummy_plate_cargo",
                    () -> BlockEntityType.Builder.of(SatelliteDockDummyBlockEntity::new, HbmBlocks.DUMMY_PLATE_CARGO.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyMachineBlockEntity>> ASSEMBLY_MACHINE =
            BLOCK_ENTITIES.register(
                    "machine_assembly_machine",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> state.is(HbmBlocks.MACHINE_TACZ_AMMO_ASSEMBLER.get())
                                    ? new TaczAmmoAssemblerBlockEntity(pos, state)
                                    : new AssemblyMachineBlockEntity(pos, state),
                            HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get(),
                            HbmBlocks.MACHINE_TACZ_AMMO_ASSEMBLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyFactoryBlockEntity>> ASSEMBLY_FACTORY =
            BLOCK_ENTITIES.register(
                    "machine_assembly_factory",
                    () -> BlockEntityType.Builder.of(
                            AssemblyFactoryBlockEntity::new,
                            HbmBlocks.MACHINE_ASSEMBLY_FACTORY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalPlantBlockEntity>> CHEMICAL_PLANT =
            BLOCK_ENTITIES.register(
                    "machine_chemical_plant",
                    () -> BlockEntityType.Builder.of(
                            ChemicalPlantBlockEntity::new,
                            HbmBlocks.MACHINE_CHEMICAL_PLANT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalFactoryBlockEntity>> CHEMICAL_FACTORY =
            BLOCK_ENTITIES.register(
                    "machine_chemical_factory",
                    () -> BlockEntityType.Builder.of(
                            ChemicalFactoryBlockEntity::new,
                            HbmBlocks.MACHINE_CHEMICAL_FACTORY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolderingStationBlockEntity>> SOLDERING_STATION =
            BLOCK_ENTITIES.register(
                    "machine_soldering_station",
                    () -> BlockEntityType.Builder.of(
                            SolderingStationBlockEntity::new,
                            HbmBlocks.MACHINE_SOLDERING_STATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcWelderBlockEntity>> ARC_WELDER =
            BLOCK_ENTITIES.register(
                    "machine_arc_welder",
                    () -> BlockEntityType.Builder.of(
                            ArcWelderBlockEntity::new,
                            HbmBlocks.MACHINE_ARC_WELDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcFurnaceBlockEntity>> ARC_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_arc_furnace",
                    () -> BlockEntityType.Builder.of(
                            ArcFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_ARC_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageDrumBlockEntity>> STORAGE_DRUM =
            BLOCK_ENTITIES.register(
                    "machine_storage_drum",
                    () -> BlockEntityType.Builder.of(
                            StorageDrumBlockEntity::new,
                            HbmBlocks.MACHINE_STORAGE_DRUM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR =
            BLOCK_ENTITIES.register(
                    "machine_compressor",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> {
                                if (state.getBlock() instanceof com.atomiclegacy.blocks.CompressorBlock compressorBlock) {
                                    return new CompressorBlockEntity(pos, state, compressorBlock.kind());
                                }
                                return new CompressorBlockEntity(pos, state, CompressorBlockEntity.Kind.NORMAL);
                            },
                            HbmBlocks.MACHINE_COMPRESSOR.get(),
                            HbmBlocks.MACHINE_COMPRESSOR_COMPACT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CustomMachineBlockEntity>> CUSTOM_MACHINE =
            BLOCK_ENTITIES.register(
                    "custom_machine",
                    () -> BlockEntityType.Builder.of(
                            CustomMachineBlockEntity::new,
                            HbmBlocks.CUSTOM_MACHINE.get()
                    ).build(null)
            );

    // [parity X1: custom machines] TileEntityProxyCombo of cm_port
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.CustomMachinePortBlockEntity>> CUSTOM_MACHINE_PORT =
            BLOCK_ENTITIES.register(
                    "custom_machine_port",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.CustomMachinePortBlockEntity::new,
                            HbmBlocks.CM_PORT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerBlockEntity>> MIXER =
            BLOCK_ENTITIES.register(
                    "machine_mixer",
                    () -> BlockEntityType.Builder.of(
                            MixerBlockEntity::new,
                            HbmBlocks.MACHINE_MIXER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoPressBlockEntity>> AMMO_PRESS =
            BLOCK_ENTITIES.register(
                    "machine_ammo_press",
                    () -> BlockEntityType.Builder.of(
                            AmmoPressBlockEntity::new,
                            HbmBlocks.MACHINE_AMMO_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressBlockEntity>> PRESS =
            BLOCK_ENTITIES.register(
                    "press",
                    () -> BlockEntityType.Builder.of(
                            PressBlockEntity::new,
                            HbmBlocks.MACHINE_PRESS.get(),
                            HbmBlocks.MACHINE_EPRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE =
            BLOCK_ENTITIES.register(
                    "machine_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            CentrifugeBlockEntity::new,
                            HbmBlocks.MACHINE_CENTRIFUGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasCentrifugeBlockEntity>> GAS_CENTRIFUGE =
            BLOCK_ENTITIES.register(
                    "machine_gascent",
                    () -> BlockEntityType.Builder.of(
                            GasCentrifugeBlockEntity::new,
                            HbmBlocks.MACHINE_GASCENT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER =
            BLOCK_ENTITIES.register(
                    "machine_crystallizer",
                    () -> BlockEntityType.Builder.of(
                            CrystallizerBlockEntity::new,
                            HbmBlocks.MACHINE_CRYSTALLIZER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CyclotronBlockEntity>> CYCLOTRON =
            BLOCK_ENTITIES.register(
                    "machine_cyclotron",
                    () -> BlockEntityType.Builder.of(
                            CyclotronBlockEntity::new,
                            HbmBlocks.MACHINE_CYCLOTRON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExposureChamberBlockEntity>> EXPOSURE_CHAMBER =
            BLOCK_ENTITIES.register(
                    "machine_exposure_chamber",
                    () -> BlockEntityType.Builder.of(
                            ExposureChamberBlockEntity::new,
                            HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeuteriumExtractorBlockEntity>> DEUTERIUM_EXTRACTOR =
            BLOCK_ENTITIES.register(
                    "machine_deuterium_extractor",
                    () -> BlockEntityType.Builder.of(
                            DeuteriumExtractorBlockEntity::new,
                            HbmBlocks.MACHINE_DEUTERIUM_EXTRACTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeuteriumTowerBlockEntity>> DEUTERIUM_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_deuterium_tower",
                    () -> BlockEntityType.Builder.of(
                            DeuteriumTowerBlockEntity::new,
                            HbmBlocks.MACHINE_DEUTERIUM_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParticleAcceleratorBlockEntity>> PARTICLE_ACCELERATOR =
            BLOCK_ENTITIES.register(
                    "particle_accelerator",
                    () -> BlockEntityType.Builder.of(
                            ParticleAcceleratorBlockEntity::new,
                            HbmBlocks.PA_SOURCE.get(),
                            HbmBlocks.PA_BEAMLINE.get(),
                            HbmBlocks.PA_RFC.get(),
                            HbmBlocks.PA_QUADRUPOLE.get(),
                            HbmBlocks.PA_DIPOLE.get(),
                            HbmBlocks.PA_DETECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SilexBlockEntity>> SILEX =
            BLOCK_ENTITIES.register(
                    "machine_silex",
                    () -> BlockEntityType.Builder.of(
                            SilexBlockEntity::new,
                            HbmBlocks.MACHINE_SILEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FelBlockEntity>> FEL =
            BLOCK_ENTITIES.register(
                    "machine_fel",
                    () -> BlockEntityType.Builder.of(
                            FelBlockEntity::new,
                            HbmBlocks.MACHINE_FEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiningLaserBlockEntity>> MINING_LASER =
            BLOCK_ENTITIES.register(
                    "machine_mining_laser",
                    () -> BlockEntityType.Builder.of(
                            MiningLaserBlockEntity::new,
                            HbmBlocks.MACHINE_MINING_LASER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeslaCoilBlockEntity>> TESLA_COIL =
            BLOCK_ENTITIES.register(
                    "tesla",
                    () -> BlockEntityType.Builder.of(
                            TeslaCoilBlockEntity::new,
                            HbmBlocks.TESLA.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreSlopperBlockEntity>> ORE_SLOPPER =
            BLOCK_ENTITIES.register(
                    "machine_ore_slopper",
                    () -> BlockEntityType.Builder.of(
                            OreSlopperBlockEntity::new,
                            HbmBlocks.MACHINE_ORE_SLOPPER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticStorageBlockEntity>> PNEUMATIC_STORAGE =
            BLOCK_ENTITIES.register(
                    "pneumatic_storage",
                    () -> BlockEntityType.Builder.of(
                            PneumaticStorageBlockEntity::new,
                            HbmBlocks.PNEUMATIC_STORAGE_ACCESS.get(),
                            HbmBlocks.PNEUMATIC_STORAGE_CLUTTER.get(),
                            HbmBlocks.PNEUMATIC_STORAGE_MONO.get(),
                            HbmBlocks.PNEUMATIC_STORAGE_IMPORTER.get(),
                            HbmBlocks.PNEUMATIC_STORAGE_EXPORTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PileMk2CoreBlockEntity>> PILE_MK2_CORE =
            BLOCK_ENTITIES.register(
                    "pile_mk2_core",
                    () -> BlockEntityType.Builder.of(
                            PileMk2CoreBlockEntity::new,
                            HbmBlocks.PILE_BLOCK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PileLoaderBlockEntity>> PILE_MK2_LOADER =
            BLOCK_ENTITIES.register(
                    "pile_mk2_loader",
                    () -> BlockEntityType.Builder.of(
                            PileLoaderBlockEntity::new,
                            HbmBlocks.PILE_LOADER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PileVentBlockEntity>> PILE_MK2_VENT =
            BLOCK_ENTITIES.register(
                    "pile_mk2_vent",
                    () -> BlockEntityType.Builder.of(
                            PileVentBlockEntity::new,
                            HbmBlocks.PILE_VENT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PileControlBlockEntity>> PILE_MK2_CONTROL =
            BLOCK_ENTITIES.register(
                    "pile_mk2_control",
                    () -> BlockEntityType.Builder.of(
                            PileControlBlockEntity::new,
                            HbmBlocks.PILE_CONTROL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RockMillBlockEntity>> ROCK_MILL =
            BLOCK_ENTITIES.register(
                    "machine_rockmill",
                    () -> BlockEntityType.Builder.of(
                            RockMillBlockEntity::new,
                            HbmBlocks.MACHINE_ROCKMILL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteGroundStationBlockEntity>> SATELLITE_GROUND_STATION =
            BLOCK_ENTITIES.register(
                    "machine_satlink",
                    () -> BlockEntityType.Builder.of(
                            SatelliteGroundStationBlockEntity::new,
                            HbmBlocks.MACHINE_SATLINK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TapeDriveBlockEntity>> TAPE_DRIVE =
            BLOCK_ENTITIES.register(
                    "machine_tape_drive",
                    () -> BlockEntityType.Builder.of(
                            TapeDriveBlockEntity::new,
                            HbmBlocks.MACHINE_TAPE_DRIVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupercomputerBlockEntity>> SUPERCOMPUTER =
            BLOCK_ENTITIES.register(
                    "machine_supercomputer",
                    () -> BlockEntityType.Builder.of(
                            SupercomputerBlockEntity::new,
                            HbmBlocks.MACHINE_SUPERCOMPUTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExcavatorBlockEntity>> EXCAVATOR =
            BLOCK_ENTITIES.register(
                    "machine_excavator",
                    () -> BlockEntityType.Builder.of(
                            ExcavatorBlockEntity::new,
                            HbmBlocks.MACHINE_EXCAVATOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BedrockOreBlockEntity>> BEDROCK_ORE =
            BLOCK_ENTITIES.register(
                    "ore_bedrock_block",
                    () -> BlockEntityType.Builder.of(
                            BedrockOreBlockEntity::new,
                            HbmBlocks.ORE_BEDROCK_BLOCK.get(),
                            HbmBlocks.ORE_BEDROCK.get(),
                            // BedrockOreBlock creates this block entity too; without it placing the block threw
                            HbmBlocks.ORE_BEDROCK_COLTAN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GlyphidSpawnerBlockEntity>> GLYPHID_SPAWNER =
            BLOCK_ENTITIES.register(
                    "glyphid_spawner",
                    () -> BlockEntityType.Builder.of(
                            GlyphidSpawnerBlockEntity::new,
                            HbmBlocks.GLYPHID_SPAWNER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CyberCrabSpawnerBlockEntity>> CYBER_CRAB_SPAWNER =
            BLOCK_ENTITIES.register(
                    "meteor_spawner",
                    () -> BlockEntityType.Builder.of(
                            CyberCrabSpawnerBlockEntity::new,
                            HbmBlocks.METEOR_SPAWNER.get()
                    ).build(null)
            );

    // [parity h1] TileMappings: TileEntityMachineIGenerator -> "tileentity_igenerator"
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.IndustrialGeneratorBlockEntity>> INDUSTRIAL_GENERATOR =
            BLOCK_ENTITIES.register(
                    "machine_industrial_generator",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.IndustrialGeneratorBlockEntity::new,
                            HbmBlocks.MACHINE_INDUSTRIAL_GENERATOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreederReactorBlockEntity>> BREEDER_REACTOR =
            BLOCK_ENTITIES.register(
                    "machine_reactor",
                    () -> BlockEntityType.Builder.of(
                            BreederReactorBlockEntity::new,
                            HbmBlocks.MACHINE_REACTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurretJeremyBlockEntity>> TURRET_JEREMY =
            BLOCK_ENTITIES.register(
                    "turret_jeremy",
                    () -> BlockEntityType.Builder.of(
                            TurretJeremyBlockEntity::new,
                            HbmBlocks.TURRET_JEREMY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurretChekhovBlockEntity>> TURRET_CHEKHOV =
            BLOCK_ENTITIES.register(
                    "turret_chekhov",
                    () -> BlockEntityType.Builder.of(
                            TurretChekhovBlockEntity::new,
                            HbmBlocks.TURRET_CHEKHOV.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyTurretBlockEntity>> LEGACY_TURRET =
            BLOCK_ENTITIES.register(
                    "legacy_turret",
                    () -> BlockEntityType.Builder.of(
                            LegacyTurretBlockEntity::new,
                            HbmBlocks.TURRET_FRIENDLY.get(),
                            HbmBlocks.TURRET_FRITZ.get(),
                            HbmBlocks.TURRET_HOWARD.get(),
                            HbmBlocks.TURRET_HOWARD_DAMAGED.get(),
                            HbmBlocks.TURRET_MAXWELL.get(),
                            HbmBlocks.TURRET_RICHARD.get(),
                            HbmBlocks.TURRET_TAUON.get(),
                            HbmBlocks.TURRET_ARTY.get(),
                            HbmBlocks.TURRET_HIMARS.get(),
                            HbmBlocks.TURRET_SENTRY.get(),
                            HbmBlocks.TURRET_SENTRY_DAMAGED.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NukeBoyBlockEntity>> NUKE_BOY =
            BLOCK_ENTITIES.register(
                    "nuke_boy",
                    () -> BlockEntityType.Builder.of(
                            NukeBoyBlockEntity::new,
                            HbmBlocks.NUKE_BOY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyNukeBlockEntity>> LEGACY_NUKE =
            BLOCK_ENTITIES.register(
                    "legacy_nuke",
                    () -> BlockEntityType.Builder.of(
                            LegacyNukeBlockEntity::new,
                            HbmBlocks.NUKE_CUSTOM.get(),
                            HbmBlocks.NUKE_FLEIJA.get(),
                            HbmBlocks.NUKE_FSTBMB.get(),
                            HbmBlocks.NUKE_GADGET.get(),
                            HbmBlocks.NUKE_MAN.get(),
                            HbmBlocks.NUKE_MIKE.get(),
                            HbmBlocks.NUKE_N2.get(),
                            HbmBlocks.NUKE_PROTOTYPE.get(),
                            HbmBlocks.NUKE_SOLINIUM.get(),
                            HbmBlocks.NUKE_TSAR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE =
            BLOCK_ENTITIES.register(
                    "vending_machine",
                    () -> BlockEntityType.Builder.of(
                            VendingMachineBlockEntity::new,
                            HbmBlocks.VENDING_MACHINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireworksBlockEntity>> FIREWORKS =
            BLOCK_ENTITIES.register(
                    "fireworks",
                    () -> BlockEntityType.Builder.of(
                            FireworksBlockEntity::new,
                            HbmBlocks.FIREWORKS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BombMultiBlockEntity>> BOMB_MULTI =
            BLOCK_ENTITIES.register(
                    "bomb_multi",
                    () -> BlockEntityType.Builder.of(
                            BombMultiBlockEntity::new,
                            HbmBlocks.BOMB_MULTI.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrashedBombBlockEntity>> CRASHED_BOMB =
            BLOCK_ENTITIES.register(
                    "crashed_bomb",
                    () -> BlockEntityType.Builder.of(
                            CrashedBombBlockEntity::new,
                            HbmBlocks.CRASHED_BOMB.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LauncherBlockEntity>> LAUNCHER =
            BLOCK_ENTITIES.register(
                    "launcher",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> {
                                if (state.is(HbmBlocks.LAUNCH_PAD_LARGE.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.PAD_LARGE);
                                }
                                if (state.is(HbmBlocks.LAUNCH_PAD_RUSTED.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.PAD_RUSTED);
                                }
                                if (state.is(HbmBlocks.COMPACT_LAUNCHER.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.COMPACT);
                                }
                                if (state.is(HbmBlocks.LAUNCH_TABLE.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.TABLE);
                                }
                                return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.PAD_SMALL);
                            },
                            HbmBlocks.LAUNCH_PAD.get(),
                            HbmBlocks.LAUNCH_PAD_LARGE.get(),
                            HbmBlocks.LAUNCH_PAD_RUSTED.get(),
                            HbmBlocks.COMPACT_LAUNCHER.get(),
                            HbmBlocks.LAUNCH_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoyuzCapsuleBlockEntity>> SOYUZ_CAPSULE =
            BLOCK_ENTITIES.register(
                    "soyuz_capsule",
                    () -> BlockEntityType.Builder.of(
                            SoyuzCapsuleBlockEntity::new,
                            HbmBlocks.SOYUZ_CAPSULE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandStructureBlockEntity>> WAND_STRUCTURE =
            BLOCK_ENTITIES.register(
                    "wand_structure",
                    () -> BlockEntityType.Builder.of(
                            WandStructureBlockEntity::new,
                            HbmBlocks.WAND_STRUCTURE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandJigsawBlockEntity>> WAND_JIGSAW =
            BLOCK_ENTITIES.register(
                    "wand_jigsaw",
                    () -> BlockEntityType.Builder.of(
                            WandJigsawBlockEntity::new,
                            HbmBlocks.WAND_JIGSAW.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandTandemBlockEntity>> WAND_TANDEM =
            BLOCK_ENTITIES.register(
                    "wand_tandem",
                    () -> BlockEntityType.Builder.of(
                            WandTandemBlockEntity::new,
                            HbmBlocks.WAND_TANDEM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandLootBlockEntity>> WAND_LOOT =
            BLOCK_ENTITIES.register(
                    "wand_loot",
                    () -> BlockEntityType.Builder.of(
                            WandLootBlockEntity::new,
                            HbmBlocks.WAND_LOOT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandLogicBlockEntity>> WAND_LOGIC =
            BLOCK_ENTITIES.register(
                    "wand_logic",
                    () -> BlockEntityType.Builder.of(
                            WandLogicBlockEntity::new,
                            HbmBlocks.WAND_LOGIC.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecoLootBlockEntity>> DECO_LOOT =
            BLOCK_ENTITIES.register(
                    "deco_loot",
                    () -> BlockEntityType.Builder.of(
                            DecoLootBlockEntity::new,
                            HbmBlocks.DECO_LOOT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecoDisplayBlockEntity>> DECO_DISPLAY =
            BLOCK_ENTITIES.register(
                    "deco_display",
                    () -> BlockEntityType.Builder.of(
                            DecoDisplayBlockEntity::new,
                            HbmBlocks.DECO_COMPUTER.get(),
                            HbmBlocks.DECO_CRT.get(),
                            HbmBlocks.STATUE_ELB_F.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilingCabinetBlockEntity>> FILING_CABINET =
            BLOCK_ENTITIES.register(
                    "filing_cabinet",
                    () -> BlockEntityType.Builder.of(
                            FilingCabinetBlockEntity::new,
                            HbmBlocks.FILING_CABINET.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TapeRecorderBlockEntity>> TAPE_RECORDER =
            BLOCK_ENTITIES.register(
                    "tape_recorder",
                    () -> BlockEntityType.Builder.of(
                            TapeRecorderBlockEntity::new,
                            HbmBlocks.TAPE_RECORDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogicBlockEntity>> LOGIC_BLOCK =
            BLOCK_ENTITIES.register(
                    "logic_block",
                    () -> BlockEntityType.Builder.of(
                            LogicBlockEntity::new,
                            HbmBlocks.LOGIC_BLOCK.get(),
                            HbmBlocks.LOGIC_BLOCK_INVIS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LauncherStructCoreBlockEntity>> LAUNCHER_STRUCT_CORE =
            BLOCK_ENTITIES.register(
                    "launcher_struct_core",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new LauncherStructCoreBlockEntity(
                                    pos,
                                    state,
                                    state.is(HbmBlocks.STRUCT_LAUNCHER_CORE_LARGE.get())
                            ),
                            HbmBlocks.STRUCT_LAUNCHER_CORE.get(),
                            HbmBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteDrumBlockEntity>> WASTE_DRUM =
            BLOCK_ENTITIES.register(
                    "machine_waste_drum",
                    () -> BlockEntityType.Builder.of(
                            WasteDrumBlockEntity::new,
                            HbmBlocks.MACHINE_WASTE_DRUM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurexBlockEntity>> PUREX =
            BLOCK_ENTITIES.register(
                    "machine_purex",
                    () -> BlockEntityType.Builder.of(
                            PurexBlockEntity::new,
                            HbmBlocks.MACHINE_PUREX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfPressBlockEntity>> ICF_PRESS =
            BLOCK_ENTITIES.register(
                    "machine_icf_press",
                    () -> BlockEntityType.Builder.of(
                            IcfPressBlockEntity::new,
                            HbmBlocks.MACHINE_ICF_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfCoreBlockEntity>> ICF_CORE =
            BLOCK_ENTITIES.register(
                    "icf",
                    () -> BlockEntityType.Builder.of(IcfCoreBlockEntity::new, HbmBlocks.ICF.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfStructBlockEntity>> ICF_STRUCT =
            BLOCK_ENTITIES.register(
                    "struct_icf_core",
                    () -> BlockEntityType.Builder.of(IcfStructBlockEntity::new, HbmBlocks.STRUCT_ICF_CORE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfControllerBlockEntity>> ICF_CONTROLLER =
            BLOCK_ENTITIES.register(
                    "icf_controller",
                    () -> BlockEntityType.Builder.of(IcfControllerBlockEntity::new, HbmBlocks.ICF_CONTROLLER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfAssembledLaserBlockEntity>> ICF_ASSEMBLED_LASER =
            BLOCK_ENTITIES.register(
                    "icf_block",
                    () -> BlockEntityType.Builder.of(IcfAssembledLaserBlockEntity::new, HbmBlocks.ICF_BLOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchReactorBlockEntity>> RESEARCH_REACTOR =
            BLOCK_ENTITIES.register(
                    "machine_reactor_small",
                    () -> BlockEntityType.Builder.of(
                            ResearchReactorBlockEntity::new,
                            HbmBlocks.MACHINE_REACTOR_SMALL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorControlBlockEntity>> REACTOR_CONTROL =
            BLOCK_ENTITIES.register(
                    "machine_controller",
                    () -> BlockEntityType.Builder.of(
                            ReactorControlBlockEntity::new,
                            HbmBlocks.MACHINE_CONTROLLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatzBlockEntity>> WATZ =
            BLOCK_ENTITIES.register(
                    "watz",
                    () -> BlockEntityType.Builder.of(
                            WatzBlockEntity::new,
                            HbmBlocks.WATZ.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatzStructBlockEntity>> WATZ_STRUCT =
            BLOCK_ENTITIES.register(
                    "struct_watz_core",
                    () -> BlockEntityType.Builder.of(
                            WatzStructBlockEntity::new,
                            HbmBlocks.STRUCT_WATZ_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatzPumpBlockEntity>> WATZ_PUMP =
            BLOCK_ENTITIES.register(
                    "watz_pump",
                    () -> BlockEntityType.Builder.of(
                            WatzPumpBlockEntity::new,
                            HbmBlocks.WATZ_PUMP.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZirnoxReactorBlockEntity>> ZIRNOX_REACTOR =
            BLOCK_ENTITIES.register(
                    "machine_zirnox",
                    () -> BlockEntityType.Builder.of(
                            ZirnoxReactorBlockEntity::new,
                            HbmBlocks.MACHINE_ZIRNOX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PwrControllerBlockEntity>> PWR_CONTROLLER =
            BLOCK_ENTITIES.register(
                    "pwr_controller",
                    () -> BlockEntityType.Builder.of(
                            PwrControllerBlockEntity::new,
                            HbmBlocks.PWR_CONTROLLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PwrBlockEntity>> PWR_PART =
            BLOCK_ENTITIES.register(
                    "pwr_part",
                    () -> BlockEntityType.Builder.of(
                            PwrBlockEntity::new,
                            HbmBlocks.PWR_BLOCK.get(),
                            HbmBlocks.PWR_CASING.get(),
                            HbmBlocks.PWR_CHANNEL.get(),
                            HbmBlocks.PWR_CONTROL.get(),
                            HbmBlocks.PWR_FUELROD.get(),
                            HbmBlocks.PWR_FUEL.get(),
                            HbmBlocks.PWR_HEATEX.get(),
                            HbmBlocks.PWR_HEATSINK.get(),
                            HbmBlocks.PWR_NEUTRON_SOURCE.get(),
                            HbmBlocks.PWR_PORT.get(),
                            HbmBlocks.PWR_REFLECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZirnoxDestroyedBlockEntity>> ZIRNOX_DESTROYED =
            BLOCK_ENTITIES.register(
                    "zirnox_destroyed",
                    () -> BlockEntityType.Builder.of(
                            ZirnoxDestroyedBlockEntity::new,
                            HbmBlocks.ZIRNOX_DESTROYED.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionMachineBlockEntity>> FUSION_MACHINE =
            BLOCK_ENTITIES.register(
                    "fusion_machine",
                    () -> BlockEntityType.Builder.of(
                            FusionMachineBlockEntity::new,
                            HbmBlocks.FUSION_TORUS.get(),
                            HbmBlocks.FUSION_KLYSTRON.get(),
                            HbmBlocks.FUSION_KLYSTRON_CREATIVE.get(),
                            HbmBlocks.FUSION_BREEDER.get(),
                            HbmBlocks.FUSION_COLLECTOR.get(),
                            HbmBlocks.FUSION_BOILER.get(),
                            HbmBlocks.FUSION_MHDT.get(),
                            HbmBlocks.FUSION_COUPLER.get(),
                            HbmBlocks.FUSION_PLASMA_FORGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcCoreBlockEntity>> DFC_CORE =
            BLOCK_ENTITIES.register(
                    "dfc_core",
                    () -> BlockEntityType.Builder.of(
                            DfcCoreBlockEntity::new,
                            HbmBlocks.DFC_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcEmitterBlockEntity>> DFC_EMITTER =
            BLOCK_ENTITIES.register(
                    "dfc_emitter",
                    () -> BlockEntityType.Builder.of(
                            DfcEmitterBlockEntity::new,
                            HbmBlocks.DFC_EMITTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcReceiverBlockEntity>> DFC_RECEIVER =
            BLOCK_ENTITIES.register(
                    "dfc_receiver",
                    () -> BlockEntityType.Builder.of(
                            DfcReceiverBlockEntity::new,
                            HbmBlocks.DFC_RECEIVER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcInjectorBlockEntity>> DFC_INJECTOR =
            BLOCK_ENTITIES.register(
                    "dfc_injector",
                    () -> BlockEntityType.Builder.of(
                            DfcInjectorBlockEntity::new,
                            HbmBlocks.DFC_INJECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcStabilizerBlockEntity>> DFC_STABILIZER =
            BLOCK_ENTITIES.register(
                    "dfc_stabilizer",
                    () -> BlockEntityType.Builder.of(
                            DfcStabilizerBlockEntity::new,
                            HbmBlocks.DFC_STABILIZER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionTorusStructBlockEntity>> FUSION_TORUS_STRUCT =
            BLOCK_ENTITIES.register(
                    "fusion_torus_struct",
                    () -> BlockEntityType.Builder.of(
                            FusionTorusStructBlockEntity::new,
                            HbmBlocks.STRUCT_TORUS_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RbmkComponentBlockEntity>> RBMK_COMPONENT =
            BLOCK_ENTITIES.register(
                    "rbmk_component",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.RbmkPanelBlockEntity::create, // [parity T2: RoR panels]
                            HbmBlocks.RBMK_ABSORBER.get(),
                            HbmBlocks.RBMK_AUTOLOADER.get(),
                            HbmBlocks.RBMK_BLANK.get(),
                            HbmBlocks.RBMK_BOILER.get(),
                            HbmBlocks.RBMK_CONSOLE.get(),
                            HbmBlocks.RBMK_CONTROL.get(),
                            HbmBlocks.RBMK_CONTROL_AUTO.get(),
                            HbmBlocks.RBMK_CONTROL_MOD.get(),
                            HbmBlocks.RBMK_CONTROL_REASIM.get(),
                            HbmBlocks.RBMK_CONTROL_REASIM_AUTO.get(),
                            HbmBlocks.RBMK_COOLER.get(),
                            HbmBlocks.RBMK_CRANE_CONSOLE.get(),
                            HbmBlocks.RBMK_DISPLAY.get(),
                            HbmBlocks.RBMK_DISPLAY_BLANK.get(),
                            HbmBlocks.RBMK_GAUGE.get(),
                            HbmBlocks.RBMK_GRAPH.get(),
                            HbmBlocks.RBMK_HEATER.get(),
                            HbmBlocks.RBMK_INDICATOR.get(),
                            HbmBlocks.RBMK_KEY_PAD.get(),
                            HbmBlocks.RBMK_LEVER.get(),
                            HbmBlocks.RBMK_LOADER.get(),
                            HbmBlocks.RBMK_MODERATOR.get(),
                            HbmBlocks.RBMK_NUMITRON.get(),
                            HbmBlocks.RBMK_OUTGASSER.get(),
                            HbmBlocks.RBMK_REFLECTOR.get(),
                            HbmBlocks.RBMK_ROD.get(),
                            HbmBlocks.RBMK_ROD_MOD.get(),
                            HbmBlocks.RBMK_ROD_REASIM.get(),
                            HbmBlocks.RBMK_ROD_REASIM_MOD.get(),
                            HbmBlocks.RBMK_STEAM_INLET.get(),
                            HbmBlocks.RBMK_STEAM_OUTLET.get(),
                            HbmBlocks.RBMK_STORAGE.get(),
                            HbmBlocks.RBMK_TERMINAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StirlingGeneratorBlockEntity>> STIRLING_GENERATOR =
            BLOCK_ENTITIES.register(
                    "machine_stirling",
                    () -> BlockEntityType.Builder.of(
                            StirlingGeneratorBlockEntity::new,
                            HbmBlocks.MACHINE_STIRLING.get(),
                            HbmBlocks.MACHINE_STIRLING_STEEL.get(),
                            HbmBlocks.MACHINE_STIRLING_CREATIVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DieselGeneratorBlockEntity>> DIESEL_GENERATOR =
            BLOCK_ENTITIES.register(
                    "machine_diesel",
                    () -> BlockEntityType.Builder.of(
                            DieselGeneratorBlockEntity::new,
                            HbmBlocks.MACHINE_DIESEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionEngineBlockEntity>> COMBUSTION_ENGINE =
            BLOCK_ENTITIES.register(
                    "machine_combustion_engine",
                    () -> BlockEntityType.Builder.of(
                            CombustionEngineBlockEntity::new,
                            HbmBlocks.MACHINE_COMBUSTION_ENGINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasFlareBlockEntity>> GAS_FLARE =
            BLOCK_ENTITIES.register(
                    "machine_flare",
                    () -> BlockEntityType.Builder.of(
                            GasFlareBlockEntity::new,
                            HbmBlocks.MACHINE_FLARE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE =
            BLOCK_ENTITIES.register(
                    "machine_steam_engine",
                    () -> BlockEntityType.Builder.of(
                            SteamEngineBlockEntity::new,
                            HbmBlocks.MACHINE_STEAM_ENGINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeaterBlockEntity>> HEATER =
            BLOCK_ENTITIES.register(
                    "heater",
                    () -> BlockEntityType.Builder.of(
                            HeaterBlockEntity::new,
                            HbmBlocks.HEATER_FIREBOX.get(),
                            HbmBlocks.HEATER_OVEN.get(),
                            HbmBlocks.HEATER_OILBURNER.get(),
                            HbmBlocks.HEATER_ELECTRIC.get(),
                            HbmBlocks.HEATER_HEATEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatBoilerBlockEntity>> HEAT_BOILER =
            BLOCK_ENTITIES.register(
                    "heat_boiler",
                    () -> BlockEntityType.Builder.of(
                            HeatBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_BOILER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialBoilerBlockEntity>> INDUSTRIAL_BOILER =
            BLOCK_ENTITIES.register(
                    "machine_industrial_boiler",
                    () -> BlockEntityType.Builder.of(
                            IndustrialBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_INDUSTRIAL_BOILER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarBoilerBlockEntity>> SOLAR_BOILER =
            BLOCK_ENTITIES.register(
                    "machine_solar_boiler",
                    () -> BlockEntityType.Builder.of(
                            SolarBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_SOLAR_BOILER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirCompressorBlockEntity>> AIR_COMPRESSOR =
            BLOCK_ENTITIES.register(
                    "machine_intake",
                    () -> BlockEntityType.Builder.of(
                            AirCompressorBlockEntity::new,
                            HbmBlocks.MACHINE_INTAKE.get()
                    ).build(null)
            );


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDetectorBlockEntity>> POWER_DETECTOR =
            BLOCK_ENTITIES.register(
                    "machine_detector",
                    () -> BlockEntityType.Builder.of(PowerDetectorBlockEntity::new, HbmBlocks.MACHINE_DETECTOR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableSwitchBlockEntity>> CABLE_SWITCH =
            BLOCK_ENTITIES.register(
                    "cable_switch",
                    () -> BlockEntityType.Builder.of(
                            CableSwitchBlockEntity::new,
                            HbmBlocks.CABLE_SWITCH.get(),
                            HbmBlocks.CABLE_DETECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableDiodeBlockEntity>> CABLE_DIODE =
            BLOCK_ENTITIES.register(
                    "cable_diode",
                    () -> BlockEntityType.Builder.of(
                            CableDiodeBlockEntity::new,
                            HbmBlocks.CABLE_DIODE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintableCableBlockEntity>> PAINTABLE_CABLE =
            BLOCK_ENTITIES.register(
                    "red_cable_paintable",
                    () -> BlockEntityType.Builder.of(
                            PaintableCableBlockEntity::new,
                            HbmBlocks.RED_CABLE_PAINTABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGaugeBlockEntity>> POWER_GAUGE =
            BLOCK_ENTITIES.register(
                    "red_cable_gauge",
                    () -> BlockEntityType.Builder.of(
                            PowerGaugeBlockEntity::new,
                            HbmBlocks.RED_CABLE_GAUGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalHeatExchangerBlockEntity>> GEOTHERMAL_HEAT_EXCHANGER =
            BLOCK_ENTITIES.register(
                    "machine_hephaestus",
                    () -> BlockEntityType.Builder.of(GeothermalHeatExchangerBlockEntity::new, HbmBlocks.MACHINE_HEPHAESTUS.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FissureBlockEntity>> FISSURE =
            BLOCK_ENTITIES.register(
                    "ore_volcano",
                    () -> BlockEntityType.Builder.of(FissureBlockEntity::new, HbmBlocks.ORE_VOLCANO.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VolcanoCoreBlockEntity>> VOLCANO_CORE =
            BLOCK_ENTITIES.register(
                    "volcano_core",
                    () -> BlockEntityType.Builder.of(
                            VolcanoCoreBlockEntity::new,
                            HbmBlocks.VOLCANO_CORE.get(),
                            HbmBlocks.VOLCANO_RAD_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarMirrorBlockEntity>> SOLAR_MIRROR =
            BLOCK_ENTITIES.register(
                    "solar_mirror",
                    () -> BlockEntityType.Builder.of(
                            SolarMirrorBlockEntity::new,
                            HbmBlocks.SOLAR_MIRROR.get()
                    ).build(null)
            );

    // [parity V1: boilers] small_boiler / small_electric_boiler removed: machine_boiler_off has no tile entity in 1.7.10

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialTurbineBlockEntity>> INDUSTRIAL_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_industrial_turbine",
                    () -> BlockEntityType.Builder.of(
                            IndustrialTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_INDUSTRIAL_TURBINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeTurbineBlockEntity>> LARGE_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_large_turbine",
                    () -> BlockEntityType.Builder.of(
                            LargeTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_LARGE_TURBINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeviathanTurbineBlockEntity>> LEVIATHAN_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_chungus",
                    () -> BlockEntityType.Builder.of(
                            LeviathanTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_CHUNGUS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasTurbineBlockEntity>> GAS_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_turbinegas",
                    () -> BlockEntityType.Builder.of(
                            GasTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_TURBINEGAS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamCondenserBlockEntity>> STEAM_CONDENSER =
            BLOCK_ENTITIES.register(
                    "machine_condenser",
                    () -> BlockEntityType.Builder.of(
                            SteamCondenserBlockEntity::new,
                            HbmBlocks.MACHINE_CONDENSER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredSteamCondenserBlockEntity>> POWERED_STEAM_CONDENSER =
            BLOCK_ENTITIES.register(
                    "machine_condenser_powered",
                    () -> BlockEntityType.Builder.of(
                            PoweredSteamCondenserBlockEntity::new,
                            HbmBlocks.MACHINE_CONDENSER_POWERED.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoolingTowerBlockEntity>> COOLING_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_tower_small",
                    () -> BlockEntityType.Builder.of(
                            CoolingTowerBlockEntity::new,
                            HbmBlocks.MACHINE_TOWER_SMALL.get(),
                            HbmBlocks.MACHINE_TOWER_LARGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GroundwaterPumpBlockEntity>> GROUNDWATER_PUMP =
            BLOCK_ENTITIES.register(
                    "groundwater_pump",
                    () -> BlockEntityType.Builder.of(
                            GroundwaterPumpBlockEntity::new,
                            HbmBlocks.PUMP_STEAM.get(),
                            HbmBlocks.PUMP_ELECTRIC.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilDerrickBlockEntity>> OIL_DERRICK =
            BLOCK_ENTITIES.register(
                    "machine_well",
                    () -> BlockEntityType.Builder.of(
                            OilDerrickBlockEntity::new,
                            HbmBlocks.MACHINE_WELL.get(),
                            HbmBlocks.MACHINE_PUMPJACK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrackingTowerBlockEntity>> FRACKING_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_fracking_tower",
                    () -> BlockEntityType.Builder.of(
                            FrackingTowerBlockEntity::new,
                            HbmBlocks.MACHINE_FRACKING_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefineryBlockEntity>> REFINERY =
            BLOCK_ENTITIES.register(
                    "machine_refinery",
                    () -> BlockEntityType.Builder.of(
                            RefineryBlockEntity::new,
                            HbmBlocks.MACHINE_REFINERY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumDistillBlockEntity>> VACUUM_DISTILL =
            BLOCK_ENTITIES.register(
                    "machine_vacuum_distill",
                    () -> BlockEntityType.Builder.of(
                            VacuumDistillBlockEntity::new,
                            HbmBlocks.MACHINE_VACUUM_DISTILL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CokerBlockEntity>> COKER =
            BLOCK_ENTITIES.register(
                    "machine_coker",
                    () -> BlockEntityType.Builder.of(
                            CokerBlockEntity::new,
                            HbmBlocks.MACHINE_COKER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChimneyBlockEntity>> CHIMNEY =
            BLOCK_ENTITIES.register(
                    "chimney",
                    () -> BlockEntityType.Builder.of(
                            ChimneyBlockEntity::new,
                            HbmBlocks.CHIMNEY_BRICK.get(),
                            HbmBlocks.CHIMNEY_INDUSTRIAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FunnelBlockEntity>> FUNNEL =
            BLOCK_ENTITIES.register(
                    "machine_funnel",
                    () -> BlockEntityType.Builder.of(
                            FunnelBlockEntity::new,
                            HbmBlocks.MACHINE_FUNNEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SirenBlockEntity>> SIREN =
            BLOCK_ENTITIES.register(
                    "machine_siren",
                    () -> BlockEntityType.Builder.of(
                            SirenBlockEntity::new,
                            HbmBlocks.MACHINE_SIREN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AshpitBlockEntity>> ASHPIT =
            BLOCK_ENTITIES.register(
                    "machine_ashpit",
                    () -> BlockEntityType.Builder.of(
                            AshpitBlockEntity::new,
                            HbmBlocks.MACHINE_ASHPIT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeconBlockEntity>> DECON =
            BLOCK_ENTITIES.register(
                    "decon",
                    () -> BlockEntityType.Builder.of(
                            DeconBlockEntity::new,
                            HbmBlocks.DECON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceCombinationBlockEntity>> FURNACE_COMBINATION =
            BLOCK_ENTITIES.register(
                    "furnace_combination",
                    () -> BlockEntityType.Builder.of(
                            FurnaceCombinationBlockEntity::new,
                            HbmBlocks.FURNACE_COMBINATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RotaryFurnaceBlockEntity>> ROTARY_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_rotary_furnace",
                    () -> BlockEntityType.Builder.of(
                            RotaryFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_ROTARY_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidifierBlockEntity>> SOLIDIFIER =
            BLOCK_ENTITIES.register(
                    "machine_solidifier",
                    () -> BlockEntityType.Builder.of(
                            SolidifierBlockEntity::new,
                            HbmBlocks.MACHINE_SOLIDIFIER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
            BLOCK_ENTITIES.register(
                    "machine_electrolyser",
                    () -> BlockEntityType.Builder.of(
                            ElectrolyzerBlockEntity::new,
                            HbmBlocks.MACHINE_ELECTROLYSER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquefactorBlockEntity>> LIQUEFACTOR =
            BLOCK_ENTITIES.register(
                    "machine_liquefactor",
                    () -> BlockEntityType.Builder.of(
                            LiquefactorBlockEntity::new,
                            HbmBlocks.MACHINE_LIQUEFACTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FractionTowerBlockEntity>> FRACTION_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_fraction_tower",
                    () -> BlockEntityType.Builder.of(
                            FractionTowerBlockEntity::new,
                            HbmBlocks.MACHINE_FRACTION_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FractionSpacerBlockEntity>> FRACTION_SPACER =
            BLOCK_ENTITIES.register(
                    "fraction_spacer",
                    () -> BlockEntityType.Builder.of(
                            FractionSpacerBlockEntity::new,
                            HbmBlocks.FRACTION_SPACER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatalyticCrackerBlockEntity>> CATALYTIC_CRACKER =
            BLOCK_ENTITIES.register(
                    "machine_catalytic_cracker",
                    () -> BlockEntityType.Builder.of(
                            CatalyticCrackerBlockEntity::new,
                            HbmBlocks.MACHINE_CATALYTIC_CRACKER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatalyticReformerBlockEntity>> CATALYTIC_REFORMER =
            BLOCK_ENTITIES.register(
                    "machine_catalytic_reformer",
                    () -> BlockEntityType.Builder.of(
                            CatalyticReformerBlockEntity::new,
                            HbmBlocks.MACHINE_CATALYTIC_REFORMER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydrotreaterBlockEntity>> HYDROTREATER =
            BLOCK_ENTITIES.register(
                    "machine_hydrotreater",
                    () -> BlockEntityType.Builder.of(
                            HydrotreaterBlockEntity::new,
                            HbmBlocks.MACHINE_HYDROTREATER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlastFurnaceBlockEntity>> MACHINE_BLAST_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_blast_furnace",
                    () -> BlockEntityType.Builder.of(
                            MachineBlastFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_BLAST_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrickFurnaceBlockEntity>> BRICK_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_furnace_brick",
                    () -> BlockEntityType.Builder.of(
                            BrickFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_FURNACE_BRICK_OFF.get(),
                            HbmBlocks.MACHINE_FURNACE_BRICK_ON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronFurnaceBlockEntity>> IRON_FURNACE =
            BLOCK_ENTITIES.register(
                    "furnace_iron",
                    () -> BlockEntityType.Builder.of(
                            IronFurnaceBlockEntity::new,
                            HbmBlocks.FURNACE_IRON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteelFurnaceBlockEntity>> STEEL_FURNACE =
            BLOCK_ENTITIES.register(
                    "furnace_steel",
                    () -> BlockEntityType.Builder.of(
                            SteelFurnaceBlockEntity::new,
                            HbmBlocks.FURNACE_STEEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register(
                    "machine_crucible",
                    () -> BlockEntityType.Builder.of(
                            CrucibleBlockEntity::new,
                            HbmBlocks.MACHINE_CRUCIBLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryCastingBlockEntity>> FOUNDRY_CASTING =
            BLOCK_ENTITIES.register(
                    "foundry_casting",
                    () -> BlockEntityType.Builder.of(
                            FoundryCastingBlockEntity::new,
                            HbmBlocks.FOUNDRY_MOLD.get(),
                            HbmBlocks.FOUNDRY_BASIN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryFlowBlockEntity>> FOUNDRY_FLOW =
            BLOCK_ENTITIES.register(
                    "foundry_flow",
                    () -> BlockEntityType.Builder.of(
                            FoundryFlowBlockEntity::new,
                            HbmBlocks.FOUNDRY_CHANNEL.get(),
                            HbmBlocks.FOUNDRY_OUTLET.get(),
                            HbmBlocks.FOUNDRY_SLAGTAP.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryTankBlockEntity>> FOUNDRY_TANK =
            BLOCK_ENTITIES.register(
                    "foundry_tank",
                    () -> BlockEntityType.Builder.of(
                            FoundryTankBlockEntity::new,
                            HbmBlocks.FOUNDRY_TANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundrySlagBlockEntity>> FOUNDRY_SLAG =
            BLOCK_ENTITIES.register(
                    "foundry_slag",
                    () -> BlockEntityType.Builder.of(
                            FoundrySlagBlockEntity::new,
                            HbmBlocks.SLAG.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StrandCasterBlockEntity>> STRAND_CASTER =
            BLOCK_ENTITIES.register(
                    "machine_strand_caster",
                    () -> BlockEntityType.Builder.of(
                            StrandCasterBlockEntity::new,
                            HbmBlocks.MACHINE_STRAND_CASTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HbmHeavyDoorBlockEntity>> HEAVY_DOOR =
            BLOCK_ENTITIES.register(
                    "heavy_door",
                    () -> BlockEntityType.Builder.of(
                            HbmHeavyDoorBlockEntity::new,
                            HbmBlocks.FIRE_DOOR.get(),
                            HbmBlocks.TRANSITION_SEAL.get(),
                            HbmBlocks.SLIDING_BLAST_DOOR.get(),
                            HbmBlocks.SLIDING_BLAST_DOOR_2.get(),
                            HbmBlocks.SLIDING_GATE_DOOR.get(),
                            HbmBlocks.QE_SLIDING_DOOR.get(),
                            HbmBlocks.QE_CONTAINMENT.get(),
                            HbmBlocks.SLIDING_SEAL_DOOR.get(),
                            HbmBlocks.SECURE_ACCESS_DOOR.get(),
                            HbmBlocks.ROUND_AIRLOCK_DOOR.get(),
                            HbmBlocks.LARGE_VEHICLE_DOOR.get(),
                            HbmBlocks.VAULT_DOOR.get(),
                            HbmBlocks.WATER_DOOR.get(),
                            HbmBlocks.CARGO_DOOR.get(),
                            HbmBlocks.SILO_HATCH.get(),
                            HbmBlocks.SILO_HATCH_LARGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HbmHeavyDoorPartBlockEntity>> HEAVY_DOOR_PART =
            BLOCK_ENTITIES.register(
                    "heavy_door_part",
                    () -> BlockEntityType.Builder.of(
                            HbmHeavyDoorPartBlockEntity::new,
                            HbmBlocks.HEAVY_DOOR_PART.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastDoorBlockEntity>> BLAST_DOOR =
            BLOCK_ENTITIES.register(
                    "blast_door",
                    () -> BlockEntityType.Builder.of(
                            BlastDoorBlockEntity::new,
                            HbmBlocks.BLAST_DOOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastDoorDummyBlockEntity>> BLAST_DOOR_DUMMY =
            BLOCK_ENTITIES.register(
                    "dummy_block_blast",
                    () -> BlockEntityType.Builder.of(
                            BlastDoorDummyBlockEntity::new,
                            HbmBlocks.DUMMY_BLOCK_BLAST.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerPylonBlockEntity>> POWER_PYLON =
            BLOCK_ENTITIES.register(
                    "power_pylon",
                    () -> BlockEntityType.Builder.of(
                            PowerPylonBlockEntity::new,
                            HbmBlocks.RED_CONNECTOR.get(),
                            HbmBlocks.CONNECTOR_RED_SUPER.get(),
                            HbmBlocks.RED_PYLON.get(),
                            HbmBlocks.RED_PYLON_STEEL.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_WOOD.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_STEEL.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get(),
                            HbmBlocks.RED_PYLON_LARGE.get(),
                            HbmBlocks.SUBSTATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoleBlockEntity>> POLE =
            BLOCK_ENTITIES.register(
                    "pole",
                    () -> BlockEntityType.Builder.of(
                            PoleBlockEntity::new,
                            HbmBlocks.POLE_TOP.get(),
                            HbmBlocks.POLE_SATELLITE_RECEIVER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register(
                    "fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            FluidPipeBlockEntity::new,
                            HbmBlocks.FLUID_DUCT_NEO.get(),
                            HbmBlocks.FLUID_DUCT_BOX.get(),
                            HbmBlocks.FLUID_DUCT_EXHAUST.get(),
                            HbmBlocks.FLUID_DUCT_GAUGE.get(),
                            HbmBlocks.FLUID_DUCT_PAINTABLE.get(),
                            HbmBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get(),
                            HbmBlocks.FLUID_VALVE.get(),
                            HbmBlocks.FLUID_SWITCH.get(),
                            HbmBlocks.FLUID_COUNTER_VALVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPumpBlockEntity>> FLUID_PUMP =
            BLOCK_ENTITIES.register(
                    "fluid_pump",
                    () -> BlockEntityType.Builder.of(
                            FluidPumpBlockEntity::new,
                            HbmBlocks.FLUID_PUMP.get()
                    ).build(null)
            );

    // [parity T1: flow_control_pump]
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.FlowControlPumpBlockEntity>> FLOW_CONTROL_PUMP =
            BLOCK_ENTITIES.register(
                    "flow_control_pump",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.FlowControlPumpBlockEntity::new,
                            HbmBlocks.FLOW_CONTROL_PUMP.get()
                    ).build(null)
            );
    // [parity U1: network_separator]
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity>> NETWORK_SEPARATOR =
            BLOCK_ENTITIES.register(
                    "network_separator",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity::new,
                            HbmBlocks.NETWORK_SEPARATOR.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrainBlockEntity>> DRAIN =
            BLOCK_ENTITIES.register(
                    "machine_drain",
                    () -> BlockEntityType.Builder.of(
                            DrainBlockEntity::new,
                            HbmBlocks.MACHINE_DRAIN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register(
                    "fluid_tank",
                    () -> BlockEntityType.Builder.of(
                            FluidTankBlockEntity::new,
                            HbmBlocks.MACHINE_FLUIDTANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexafluorideTankBlockEntity>> HEXAFLUORIDE_TANK =
            BLOCK_ENTITIES.register(
                    "hexafluoride_tank",
                    () -> BlockEntityType.Builder.of(
                            HexafluorideTankBlockEntity::new,
                            HbmBlocks.MACHINE_UF6_TANK.get(),
                            HbmBlocks.MACHINE_PUF6_TANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidBarrelBlockEntity>> FLUID_BARREL =
            BLOCK_ENTITIES.register(
                    "fluid_barrel",
                    () -> BlockEntityType.Builder.of(
                            FluidBarrelBlockEntity::new,
                            HbmBlocks.BARREL_PLASTIC.get(),
                            HbmBlocks.BARREL_CORRODED.get(),
                            HbmBlocks.BARREL_STEEL.get(),
                            HbmBlocks.BARREL_TCALLOY.get(),
                            HbmBlocks.BARREL_ANTIMATTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigAssTankBlockEntity>> BIG_ASS_TANK =
            BLOCK_ENTITIES.register(
                    "machine_bat9000",
                    () -> BlockEntityType.Builder.of(
                            BigAssTankBlockEntity::new,
                            HbmBlocks.MACHINE_BAT9000.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeFluidTankBlockEntity>> LARGE_FLUID_TANK =
            BLOCK_ENTITIES.register(
                    "machine_bigasstank",
                    () -> BlockEntityType.Builder.of(
                            LargeFluidTankBlockEntity::new,
                            HbmBlocks.MACHINE_BIGASSTANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryReddBlockEntity>> BATTERY_REDD =
            BLOCK_ENTITIES.register(
                    "machine_battery_redd",
                    () -> BlockEntityType.Builder.of(
                            BatteryReddBlockEntity::new,
                            HbmBlocks.MACHINE_BATTERY_REDD.get()
                    ).build(null)
            );

    // [parity d2] TileEntityDiFurnace / TileEntityDiFurnaceRTG / TileEntityRtgFurnace and TileEntityMachineBattery
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity>> LEGACY_DEPRECATED_FURNACE =
            BLOCK_ENTITIES.register(
                    "legacy_deprecated_furnace",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_DIFURNACE_OFF.get(), HbmBlocks.MACHINE_DIFURNACE_ON.get(),
                            HbmBlocks.MACHINE_DIFURNACE_RTG_OFF.get(), HbmBlocks.MACHINE_DIFURNACE_RTG_ON.get(),
                            HbmBlocks.MACHINE_RTG_FURNACE_OFF.get(), HbmBlocks.MACHINE_RTG_FURNACE_ON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.LegacyBatteryBlockEntity>> LEGACY_BATTERY =
            BLOCK_ENTITIES.register(
                    "legacy_battery",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.LegacyBatteryBlockEntity::new,
                            HbmBlocks.MACHINE_BATTERY_POTATO.get(), HbmBlocks.MACHINE_BATTERY.get(), HbmBlocks.MACHINE_LITHIUM_BATTERY.get(),
                            HbmBlocks.MACHINE_SCHRABIDIUM_BATTERY.get(), HbmBlocks.MACHINE_DINEUTRONIUM_BATTERY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatterySocketBlockEntity>> BATTERY_SOCKET =
            BLOCK_ENTITIES.register(
                    "machine_battery_socket",
                    () -> BlockEntityType.Builder.of(
                            BatterySocketBlockEntity::new,
                            HbmBlocks.MACHINE_BATTERY_SOCKET.get()
                    ).build(null)
                         );

    // [parity L1: converters] tileentity_converter_herf / tileentity_converter_rfhe
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.EnergyConverterBlockEntity>> ENERGY_CONVERTER =
            BLOCK_ENTITIES.register(
                    "machine_converter",
                    () -> BlockEntityType.Builder.of(
                            com.atomiclegacy.tileentity.EnergyConverterBlockEntity::new,
                            HbmBlocks.MACHINE_CONVERTER_HE_RF.get(),
                            HbmBlocks.MACHINE_CONVERTER_RF_HE.get()
                    ).build(null)
            );

    // [parity L1: capacitor] capacitor_copper/capacitor_bus have no TileEntity in 1.7.10 (BE types removed).

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER =
            BLOCK_ENTITIES.register(
                    "charger",
                    () -> BlockEntityType.Builder.of(
                            ChargerBlockEntity::new,
                            HbmBlocks.CHARGER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageCrateBlockEntity>> STORAGE_CRATE =
            BLOCK_ENTITIES.register(
                    "storage_crate",
                    () -> BlockEntityType.Builder.of(
                            StorageCrateBlockEntity::new,
                            HbmBlocks.CRATE_IRON.get(),
                            HbmBlocks.CRATE_STEEL.get(),
                            HbmBlocks.CRATE_DESH.get(),
                            HbmBlocks.CRATE_TEMPLATE.get(),
                            HbmBlocks.CRATE_TUNGSTEN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyCrateBlockEntity>> SUPPLY_CRATE =
            BLOCK_ENTITIES.register(
                    "crate_supply",
                    () -> BlockEntityType.Builder.of(
                            SupplyCrateBlockEntity::new,
                            HbmBlocks.CRATE_SUPPLY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SafeBlockEntity>> SAFE =
            BLOCK_ENTITIES.register(
                    "safe",
                    () -> BlockEntityType.Builder.of(
                            SafeBlockEntity::new,
                            HbmBlocks.SAFE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE =
            BLOCK_ENTITIES.register(
                    "mass_storage",
                    () -> BlockEntityType.Builder.of(
                            MassStorageBlockEntity::new,
                            HbmBlocks.MASS_STORAGE.get(),
                            HbmBlocks.MASS_STORAGE_IRON.get(),
                            HbmBlocks.MASS_STORAGE_DESH.get(),
                            HbmBlocks.MASS_STORAGE_WOOD.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyEmitterBlockEntity>> LEGACY_EMITTER =
            BLOCK_ENTITIES.register(
                    "legacy_emitter",
                    () -> BlockEntityType.Builder.of(
                            LegacyEmitterBlockEntity::new,
                            HbmBlocks.GEYSIR_CHLORINE.get(),
                            HbmBlocks.GEYSIR_NETHER.get(),
                            HbmBlocks.VENT_CHLORINE.get(),
                            HbmBlocks.VENT_CLOUD.get(),
                            HbmBlocks.VENT_PINK_CLOUD.get(),
                            HbmBlocks.VENT_CHLORINE_SEAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyMachineBlockEntity>> LEGACY_MACHINE =
            BLOCK_ENTITIES.register(
                    "legacy_machine",
                    () -> BlockEntityType.Builder.of(
                            LegacyMachineBlockEntity::new,
                            HbmBlocks.MACHINE_ANNIHILATOR.get(),
                            HbmBlocks.MACHINE_AUTOCRAFTER.get(),
                            HbmBlocks.MACHINE_AUTOSAW.get(),
                            HbmBlocks.MACHINE_THRESHER.get(),
                            HbmBlocks.MACHINE_LPW2.get(),
                            HbmBlocks.MACHINE_FORCEFIELD.get(),
                            HbmBlocks.MACHINE_MISSILE_ASSEMBLY.get(),
                            HbmBlocks.MACHINE_ORBUS.get(),
                            HbmBlocks.MACHINE_PRECASS.get(),
                            HbmBlocks.MACHINE_PYROOVEN.get(),
                            HbmBlocks.MACHINE_RADAR.get(),
                            HbmBlocks.MACHINE_RADAR_LARGE.get(),
                            HbmBlocks.MACHINE_RADGEN.get(),
                            HbmBlocks.MACHINE_RADIOLYSIS.get(),
                            HbmBlocks.MACHINE_RTG_GREY.get(),
                            HbmBlocks.MACHINE_SATLINKER.get(),
                            HbmBlocks.MACHINE_SAWMILL.get(),
                            HbmBlocks.MACHINE_TELEPORTER.get(),
                            HbmBlocks.MACHINE_TURBOFAN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorPressBlockEntity>> CONVEYOR_PRESS =
            BLOCK_ENTITIES.register(
                    "machine_conveyor_press",
                    () -> BlockEntityType.Builder.of(
                            ConveyorPressBlockEntity::new,
                            HbmBlocks.MACHINE_CONVEYOR_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LambdaLaunchpadBlockEntity>> LAMBDA_LAUNCHPAD =
            BLOCK_ENTITIES.register(
                    "launchpad_lambda",
                    () -> BlockEntityType.Builder.of(
                            LambdaLaunchpadBlockEntity::new,
                            HbmBlocks.LAUNCHPAD_LAMBDA.get()
                    ).build(null)
            );

    // [parity P2: soyuz_launcher]
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity>> SOYUZ_LAUNCHER =
            BLOCK_ENTITIES.register("soyuz_launcher", () -> BlockEntityType.Builder.of(
                    com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity::new, HbmBlocks.SOYUZ_LAUNCHER.get()).build(null));
    // [parity P2: soyuz_launcher]
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomiclegacy.tileentity.SoyuzStructBlockEntity>> SOYUZ_STRUCT =
            BLOCK_ENTITIES.register("struct_soyuz_core", () -> BlockEntityType.Builder.of(
                    com.atomiclegacy.tileentity.SoyuzStructBlockEntity::new, HbmBlocks.STRUCT_SOYUZ_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoyuzLaunchpadBlockEntity>> SOYUZ_LAUNCHPAD =
            BLOCK_ENTITIES.register(
                    "launchpad_soyuz",
                    () -> BlockEntityType.Builder.of(
                            SoyuzLaunchpadBlockEntity::new,
                            HbmBlocks.LAUNCHPAD_SOYUZ.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraneMachineBlockEntity>> CRANE_MACHINE =
            BLOCK_ENTITIES.register(
                    "crane_machine",
                    () -> BlockEntityType.Builder.of(
                            CraneMachineBlockEntity::new,
                            HbmBlocks.CRANE_BOXER.get(),
                            HbmBlocks.CRANE_EXTRACTOR.get(),
                            HbmBlocks.CRANE_GRABBER.get(),
                            HbmBlocks.CRANE_INSERTER.get(),
                            HbmBlocks.CRANE_PARTITIONER.get(),
                            HbmBlocks.CRANE_ROUTER.get(),
                            HbmBlocks.CRANE_SPLITTER.get(),
                            HbmBlocks.CRANE_UNBOXER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarScreenBlockEntity>> RADAR_SCREEN =
            BLOCK_ENTITIES.register(
                    "radar_screen",
                    () -> BlockEntityType.Builder.of(
                            RadarScreenBlockEntity::new,
                            HbmBlocks.RADAR_SCREEN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RebarBlockEntity>> REBAR =
            BLOCK_ENTITIES.register(
                    "rebar",
                    () -> BlockEntityType.Builder.of(RebarBlockEntity::new, HbmBlocks.REBAR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineDummyBlockEntity>> MACHINE_DUMMY =
            BLOCK_ENTITIES.register(
                    "machine_dummy",
                    () -> BlockEntityType.Builder.of(
                            MachineDummyBlockEntity::new,
                            HbmBlocks.MACHINE_DUMMY.get(),
                            HbmBlocks.DUMMY_PLATE_COMPACT_LAUNCHER.get(),
                            HbmBlocks.DUMMY_PORT_COMPACT_LAUNCHER.get(),
                            HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get(),
                            HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneWaypointBlockEntity>> DRONE_WAYPOINT =
            BLOCK_ENTITIES.register(
                    "drone_waypoint",
                    () -> BlockEntityType.Builder.of(DroneWaypointBlockEntity::new, HbmBlocks.DRONE_WAYPOINT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneRequestWaypointBlockEntity>> DRONE_WAYPOINT_REQUEST =
            BLOCK_ENTITIES.register(
                    "drone_waypoint_request",
                    () -> BlockEntityType.Builder.of(DroneRequestWaypointBlockEntity::new, HbmBlocks.DRONE_WAYPOINT_REQUEST.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneCrateBlockEntity>> DRONE_CRATE =
            BLOCK_ENTITIES.register(
                    "drone_crate",
                    () -> BlockEntityType.Builder.of(DroneCrateBlockEntity::new, HbmBlocks.DRONE_CRATE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneDockBlockEntity>> DRONE_DOCK =
            BLOCK_ENTITIES.register(
                    "drone_dock",
                    () -> BlockEntityType.Builder.of(DroneDockBlockEntity::new, HbmBlocks.DRONE_DOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneProviderBlockEntity>> DRONE_PROVIDER =
            BLOCK_ENTITIES.register(
                    "drone_crate_provider",
                    () -> BlockEntityType.Builder.of(DroneProviderBlockEntity::new, HbmBlocks.DRONE_CRATE_PROVIDER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneRequesterBlockEntity>> DRONE_REQUESTER =
            BLOCK_ENTITIES.register(
                    "drone_crate_requester",
                    () -> BlockEntityType.Builder.of(DroneRequesterBlockEntity::new, HbmBlocks.DRONE_CRATE_REQUESTER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyDisplayStandBlockEntity>> LEGACY_DISPLAY_STAND =
            BLOCK_ENTITIES.register(
                    "legacy_display_stand",
                    () -> BlockEntityType.Builder.of(
                            LegacyDisplayStandBlockEntity::new,
                            HbmBlocks.PEDESTAL.get(),
                            HbmBlocks.SKELETON_HOLDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealHatchBlockEntity>> SEAL_HATCH =
            BLOCK_ENTITIES.register(
                    "seal_hatch",
                    () -> BlockEntityType.Builder.of(SealHatchBlockEntity::new, HbmBlocks.SEAL_HATCH.get()).build(null)
            );

    private HbmBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
