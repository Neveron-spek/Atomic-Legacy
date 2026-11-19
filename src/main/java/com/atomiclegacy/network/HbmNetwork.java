package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HbmNetwork {
    private HbmNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AnnihilatorControlPayload.TYPE, AnnihilatorControlPayload.STREAM_CODEC, AnnihilatorControlPayload::handle);
        registrar.playToServer(SetFluidIdentifierPayload.TYPE, SetFluidIdentifierPayload.STREAM_CODEC, SetFluidIdentifierPayload::handle);
        registrar.playToServer(SetFluidBookmarksPayload.TYPE, SetFluidBookmarksPayload.STREAM_CODEC, SetFluidBookmarksPayload::handle); // [parity e2]
        registrar.playToServer(SettingsToolKeysPayload.TYPE, SettingsToolKeysPayload.STREAM_CODEC, SettingsToolKeysPayload::handle);
        registrar.playToServer(LegacyDuckPayload.TYPE, LegacyDuckPayload.STREAM_CODEC, LegacyDuckPayload::handle);
        registrar.playToServer(ToggleMagnetPayload.TYPE, ToggleMagnetPayload.STREAM_CODEC, ToggleMagnetPayload::handle);
        registrar.playToServer(TrainInventoryPayload.TYPE, TrainInventoryPayload.STREAM_CODEC, TrainInventoryPayload::handle); // [parity S1: trainInv]
        registrar.playToServer(JetpackControlPayload.TYPE, JetpackControlPayload.STREAM_CODEC, JetpackControlPayload::handle);
        registrar.playToServer(ArmorDashPayload.TYPE, ArmorDashPayload.STREAM_CODEC, ArmorDashPayload::handle);
        registrar.playToServer(ToggleHeaterPayload.TYPE, ToggleHeaterPayload.STREAM_CODEC, ToggleHeaterPayload::handle);
        registrar.playToServer(OilburnerBurnRatePayload.TYPE, OilburnerBurnRatePayload.STREAM_CODEC, OilburnerBurnRatePayload::handle); // [parity L3: oilburner]
        registrar.playToServer(HeatExchangerControlPayload.TYPE, HeatExchangerControlPayload.STREAM_CODEC, HeatExchangerControlPayload::handle);
        registrar.playToServer(GasTurbineControlPayload.TYPE, GasTurbineControlPayload.STREAM_CODEC, GasTurbineControlPayload::handle);
        registrar.playToServer(WoodBurnerControlPayload.TYPE, WoodBurnerControlPayload.STREAM_CODEC, WoodBurnerControlPayload::handle);
        registrar.playToServer(MicrowaveControlPayload.TYPE, MicrowaveControlPayload.STREAM_CODEC, MicrowaveControlPayload::handle);
        registrar.playToServer(LambdaLaunchpadControlPayload.TYPE, LambdaLaunchpadControlPayload.STREAM_CODEC, LambdaLaunchpadControlPayload::handle);
        registrar.playToServer(SoyuzLaunchpadControlPayload.TYPE, SoyuzLaunchpadControlPayload.STREAM_CODEC, SoyuzLaunchpadControlPayload::handle);
        registrar.playToServer(CompressorControlPayload.TYPE, CompressorControlPayload.STREAM_CODEC, CompressorControlPayload::handle);
        registrar.playToServer(FunnelControlPayload.TYPE, FunnelControlPayload.STREAM_CODEC, FunnelControlPayload::handle);
        registrar.playToServer(MissileAssemblyControlPayload.TYPE, MissileAssemblyControlPayload.STREAM_CODEC, MissileAssemblyControlPayload::handle);
        registrar.playToServer(RadarControlPayload.TYPE, RadarControlPayload.STREAM_CODEC, RadarControlPayload::handle);
        registrar.playToServer(RadarCommandPayload.TYPE, RadarCommandPayload.STREAM_CODEC, RadarCommandPayload::handle);
        registrar.playToServer(FusionMachineControlPayload.TYPE, FusionMachineControlPayload.STREAM_CODEC, FusionMachineControlPayload::handle);
        registrar.playToServer(MixerControlPayload.TYPE, MixerControlPayload.STREAM_CODEC, MixerControlPayload::handle);
        registrar.playToServer(ResearchReactorControlPayload.TYPE, ResearchReactorControlPayload.STREAM_CODEC, ResearchReactorControlPayload::handle);
        registrar.playToServer(ReactorControlPayload.TYPE, ReactorControlPayload.STREAM_CODEC, ReactorControlPayload::handle);
        registrar.playToServer(RbmkConsoleControlPayload.TYPE, RbmkConsoleControlPayload.STREAM_CODEC, RbmkConsoleControlPayload::handle);
        registrar.playToServer(RbmkControlRodPayload.TYPE, RbmkControlRodPayload.STREAM_CODEC, RbmkControlRodPayload::handle);
        registrar.playToServer(RbmkAutoControlPayload.TYPE, RbmkAutoControlPayload.STREAM_CODEC, RbmkAutoControlPayload::handle);
        registrar.playToServer(RbmkCraneControlPayload.TYPE, RbmkCraneControlPayload.STREAM_CODEC, RbmkCraneControlPayload::handle);
        registrar.playToServer(ZirnoxControlPayload.TYPE, ZirnoxControlPayload.STREAM_CODEC, ZirnoxControlPayload::handle);
        registrar.playToServer(PwrControlPayload.TYPE, PwrControlPayload.STREAM_CODEC, PwrControlPayload::handle);
        registrar.playToServer(ParticleAcceleratorControlPayload.TYPE, ParticleAcceleratorControlPayload.STREAM_CODEC, ParticleAcceleratorControlPayload::handle);
        registrar.playToServer(BatteryReddControlPayload.TYPE, BatteryReddControlPayload.STREAM_CODEC, BatteryReddControlPayload::handle);
        registrar.playToServer(BatterySocketControlPayload.TYPE, BatterySocketControlPayload.STREAM_CODEC, BatterySocketControlPayload::handle);
        registrar.playToServer(DfcControlPayload.TYPE, DfcControlPayload.STREAM_CODEC, DfcControlPayload::handle);
        registrar.playToServer(TurretJeremyControlPayload.TYPE, TurretJeremyControlPayload.STREAM_CODEC, TurretJeremyControlPayload::handle);
        registrar.playToServer(TurretChekhovControlPayload.TYPE, TurretChekhovControlPayload.STREAM_CODEC, TurretChekhovControlPayload::handle);
        registrar.playToServer(LegacyTurretControlPayload.TYPE, LegacyTurretControlPayload.STREAM_CODEC, LegacyTurretControlPayload::handle);
        registrar.playToServer(WandConfigPayload.TYPE, WandConfigPayload.STREAM_CODEC, WandConfigPayload::handle);
        registrar.playToServer(SetRttyPagerChannelPayload.TYPE, SetRttyPagerChannelPayload.STREAM_CODEC, SetRttyPagerChannelPayload::handle);
        registrar.playToServer(SetRttyDesignatorChannelPayload.TYPE, SetRttyDesignatorChannelPayload.STREAM_CODEC, SetRttyDesignatorChannelPayload::handle);
        registrar.playToServer(ManualDesignatorPayload.TYPE, ManualDesignatorPayload.STREAM_CODEC, ManualDesignatorPayload::handle); // [parity P1: designator_manual]
        registrar.playToServer(RadioRecControlPayload.TYPE, RadioRecControlPayload.STREAM_CODEC, RadioRecControlPayload::handle);
        registrar.playToServer(RttyConfigPayload.TYPE, RttyConfigPayload.STREAM_CODEC, RttyConfigPayload::handle);
        registrar.playToServer(RadioTorchControlPayload.TYPE, RadioTorchControlPayload.STREAM_CODEC, RadioTorchControlPayload::handle); // [parity R2: RTTY torch GUIs]
        registrar.playToServer(RbmkPanelControlPayload.TYPE, RbmkPanelControlPayload.STREAM_CODEC, RbmkPanelControlPayload::handle); // [parity T2: RoR panel GUIs]
        registrar.playToServer(FlowControlPumpControlPayload.TYPE, FlowControlPumpControlPayload.STREAM_CODEC, FlowControlPumpControlPayload::handle); // [parity T1: flow_control_pump]
        registrar.playToServer(NetworkSeparatorControlPayload.TYPE, NetworkSeparatorControlPayload.STREAM_CODEC, NetworkSeparatorControlPayload::handle); // [parity U1: network_separator]
        registrar.playToServer(RadioTelexControlPayload.TYPE, RadioTelexControlPayload.STREAM_CODEC, RadioTelexControlPayload::handle);
        registrar.playToServer(AutocalControlPayload.TYPE, AutocalControlPayload.STREAM_CODEC, AutocalControlPayload::handle);
        registrar.playToServer(BobmazonOrderPayload.TYPE, BobmazonOrderPayload.STREAM_CODEC, BobmazonOrderPayload::handle);
        registrar.playToServer(PortableCrateOpenPayload.TYPE, PortableCrateOpenPayload.STREAM_CODEC, PortableCrateOpenPayload::handle);
        registrar.playToServer(PortableCrateSlotClickPayload.TYPE, PortableCrateSlotClickPayload.STREAM_CODEC, PortableCrateSlotClickPayload::handle);
        registrar.playToServer(GunKeyPayload.TYPE, GunKeyPayload.STREAM_CODEC, GunKeyPayload::handle);
        registrar.playToServer(SatellitePanelClickPayload.TYPE, SatellitePanelClickPayload.STREAM_CODEC, SatellitePanelClickPayload::handle);
        registrar.playToServer(SatelliteCoordPayload.TYPE, SatelliteCoordPayload.STREAM_CODEC, SatelliteCoordPayload::handle); // [parity P2: sat_coord GUI]
        registrar.playToServer(SoyuzLauncherControlPayload.TYPE, SoyuzLauncherControlPayload.STREAM_CODEC, SoyuzLauncherControlPayload::handle); // [parity P2: soyuz_launcher]
        registrar.playToClient(PlayerInformPayload.TYPE, PlayerInformPayload.STREAM_CODEC, PlayerInformPayload::handle);
        registrar.playToClient(MarkerPayload.TYPE, MarkerPayload.STREAM_CODEC, MarkerPayload::handle); // [parity b2: assembly markers]
        registrar.playToClient(SerializableRecipePayload.TYPE, SerializableRecipePayload.STREAM_CODEC, SerializableRecipePayload::handle); // [parity b2: custom machines]
        registrar.playToClient(PneumaticAccessPayloads.Sync.TYPE, PneumaticAccessPayloads.Sync.STREAM_CODEC, PneumaticAccessPayloads.Sync::handle); // [parity V2: storage]
        registrar.playToServer(PneumaticAccessPayloads.Click.TYPE, PneumaticAccessPayloads.Click.STREAM_CODEC, PneumaticAccessPayloads.Click::handle); // [parity V2: storage]
        registrar.playToClient(PollutionSyncPayload.TYPE, PollutionSyncPayload.STREAM_CODEC, PollutionSyncPayload::handle);
        registrar.playToClient(TomImpactSyncPayload.TYPE, TomImpactSyncPayload.STREAM_CODEC, TomImpactSyncPayload::handle);
        registrar.playToClient(BatteryReddSyncPayload.TYPE, BatteryReddSyncPayload.STREAM_CODEC, BatteryReddSyncPayload::handle);
        registrar.playToClient(LegacyExplosionEffectPayload.TYPE, LegacyExplosionEffectPayload.STREAM_CODEC, LegacyExplosionEffectPayload::handle);
        registrar.playToClient(LegacySmallExplosionEffectPayload.TYPE, LegacySmallExplosionEffectPayload.STREAM_CODEC, LegacySmallExplosionEffectPayload::handle);
        registrar.playToClient(BlackPowderEffectPayload.TYPE, BlackPowderEffectPayload.STREAM_CODEC, BlackPowderEffectPayload::handle);
        registrar.playToClient(TurretCasingEffectPayload.TYPE, TurretCasingEffectPayload.STREAM_CODEC, TurretCasingEffectPayload::handle);
        registrar.playToClient(TurretMuzzleFlashPayload.TYPE, TurretMuzzleFlashPayload.STREAM_CODEC, TurretMuzzleFlashPayload::handle);
        registrar.playToClient(MaxwellGibEffectPayload.TYPE, MaxwellGibEffectPayload.STREAM_CODEC, MaxwellGibEffectPayload::handle);
        registrar.playToClient(LandmineEffectPayload.TYPE, LandmineEffectPayload.STREAM_CODEC, LandmineEffectPayload::handle);
        registrar.playToClient(SirenSoundPayload.TYPE, SirenSoundPayload.STREAM_CODEC, SirenSoundPayload::handle);
        registrar.playToClient(PressAnimationPayload.TYPE, PressAnimationPayload.STREAM_CODEC, PressAnimationPayload::handle);
        registrar.playToClient(SatellitePanelOpenPayload.TYPE, SatellitePanelOpenPayload.STREAM_CODEC, SatellitePanelOpenPayload::handle);
        registrar.playToClient(GunAnimationPayload.TYPE, GunAnimationPayload.STREAM_CODEC, GunAnimationPayload::handle);
        registrar.playToClient(ItemAnimationPayload.TYPE, ItemAnimationPayload.STREAM_CODEC, ItemAnimationPayload::handle);
        registrar.playToClient(GunMuzzleFlashPayload.TYPE, GunMuzzleFlashPayload.STREAM_CODEC, GunMuzzleFlashPayload::handle);
        registrar.playToClient(GunTinyExplosionPayload.TYPE, GunTinyExplosionPayload.STREAM_CODEC, GunTinyExplosionPayload::handle);
        // [weapons batch D1] "plasmablast" aux particle of the shredder pulse (XFactory12ga.spawnPulse)
        registrar.playToClient(GunPlasmaPulsePayload.TYPE, GunPlasmaPulsePayload.STREAM_CODEC, GunPlasmaPulsePayload::handle);
        // [weapons batch F3] ParticleBurstPacket of the drill's AoE blocks (XFactoryDrill.breakExtraBlock)
        registrar.playToClient(GunDrillParticleBurstPayload.TYPE, GunDrillParticleBurstPayload.STREAM_CODEC, GunDrillParticleBurstPayload::handle);
        // [weapons batch F2] "muke" / "tinytot" aux particles of the Fat Man mini nukes (XFactoryCatapult)
        registrar.playToClient(GunMukeEffectPayload.TYPE, GunMukeEffectPayload.STREAM_CODEC, GunMukeEffectPayload::handle);
        // [C2] ExplosionEffectStandard packet of the Protège torpedo (ExplosionVNT.makeStandard)
        registrar.playToClient(VntStandardExplosionPayload.TYPE, VntStandardExplosionPayload.STREAM_CODEC, VntStandardExplosionPayload::handle);
        registrar.playToClient(PwrPrinterOpenPayload.TYPE, PwrPrinterOpenPayload.STREAM_CODEC, PwrPrinterOpenPayload::handle);
    }
}
