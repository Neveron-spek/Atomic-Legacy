package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayLine;
import com.atomiclegacy.blocks.WandStructureBlock;
import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.blocks.GasTurbineBlock;
import com.atomiclegacy.tileentity.AirCompressorBlockEntity;
import com.atomiclegacy.tileentity.CatalyticCrackerBlockEntity;
import com.atomiclegacy.tileentity.CoolingTowerBlockEntity;
import com.atomiclegacy.tileentity.ConveyorPressBlockEntity;
import com.atomiclegacy.tileentity.DeuteriumExtractorBlockEntity;
import com.atomiclegacy.tileentity.DrainBlockEntity;
import com.atomiclegacy.tileentity.FractionTowerBlockEntity;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import com.atomiclegacy.tileentity.HeatBoilerBlockEntity;
import com.atomiclegacy.tileentity.HeaterBlockEntity;
import com.atomiclegacy.tileentity.GroundwaterPumpBlockEntity;
import com.atomiclegacy.tileentity.GeothermalHeatExchangerBlockEntity;
import com.atomiclegacy.tileentity.GasTurbineBlockEntity;
import com.atomiclegacy.tileentity.IndustrialTurbineBlockEntity;
import com.atomiclegacy.tileentity.LeviathanTurbineBlockEntity;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.tileentity.PoweredSteamCondenserBlockEntity;
import com.atomiclegacy.tileentity.PowerGaugeBlockEntity;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import com.atomiclegacy.tileentity.RotaryFurnaceBlockEntity;
import com.atomiclegacy.tileentity.SatelliteGroundStationBlockEntity;
import com.atomiclegacy.tileentity.SolarBoilerBlockEntity;
import com.atomiclegacy.tileentity.SteamCondenserBlockEntity;
import com.atomiclegacy.tileentity.SteamEngineBlockEntity;
import com.atomiclegacy.tileentity.StirlingGeneratorBlockEntity;
import com.atomiclegacy.tileentity.StrandCasterBlockEntity;
import com.atomiclegacy.tileentity.WandJigsawBlockEntity;
import com.atomiclegacy.tileentity.WandLogicBlockEntity;
import com.atomiclegacy.tileentity.WandLootBlockEntity;
import com.atomiclegacy.tileentity.WandStructureBlockEntity;
import com.atomiclegacy.tileentity.WandTandemBlockEntity;
import com.atomiclegacy.items.FoundryMoldItem;
import com.atomiclegacy.items.BlowtorchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.atomiclegacy.api.client.HbmLookOverlayApi.energyRateLine;
import static com.atomiclegacy.api.client.HbmLookOverlayApi.energyPercentLine;
import static com.atomiclegacy.api.client.HbmLookOverlayApi.energyStorageLine;
import static com.atomiclegacy.api.client.HbmLookOverlayApi.fluidLine;
import static com.atomiclegacy.api.client.HbmLookOverlayApi.fluidNameLine;
import static com.atomiclegacy.api.client.HbmLookOverlayApi.fluidText;
import static com.atomiclegacy.api.client.HbmLookOverlayApi.heatLine;

@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class HbmLookOverlay {
    private static final int BUILTIN_PROVIDER_PRIORITY = 1_000;
    private static final int X_OFFSET = 28;
    private static final int Y_OFFSET = 14;
    private static final int TITLE_TO_LINES = 14;
    private static final int LINE_SPACING = 13;

    static {
        HbmLookOverlayApi.registerProvider(
                AtomicLegacy.id("builtin_look_overlay"),
                BUILTIN_PROVIDER_PRIORITY,
                HbmLookOverlay::builtInOverlay
        );
    }

    private HbmLookOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        // [parity V3: ClientConfig.DODD_RBMK_DIAGNOSTIC] ModEventHandlerClient runs every ILookOverlay hook only when it is on
        if (!com.atomiclegacy.config.HbmClientConfig.doddRbmkDiagnostic()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.level == null || minecraft.hitResult == null) {
            return;
        }
        if (minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        OverlayData data = HbmLookOverlayApi.findOverlay(
                minecraft.level,
                (BlockHitResult) minecraft.hitResult
        );
        if (data != null) {
            renderGeneric(event.getGuiGraphics(), minecraft.font, data);
        }
    }

    @Nullable
    private static OverlayData builtInOverlay(Level level, BlockHitResult hit) {
        BlockPos corePos = corePos(level, hit.getBlockPos());
        if (corePos == null) {
            return null;
        }

        BlockState coreState = level.getBlockState(corePos);
        BlockEntity coreEntity = level.getBlockEntity(corePos);
        if (coreState.isAir() || coreEntity == null) {
            return null;
        }

        if (coreEntity instanceof RotaryFurnaceBlockEntity furnace) {
            return rotaryFurnaceOverlay(coreState, furnace, corePos, hit.getBlockPos());
        }

        if (coreEntity instanceof GasTurbineBlockEntity turbine) {
            return gasTurbineOverlay(coreState, turbine, corePos, hit.getBlockPos());
        }

        return overlayData(coreState, coreEntity);
    }

    @Nullable
    private static BlockPos corePos(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            return corePos.equals(BlockPos.ZERO) && !pos.equals(BlockPos.ZERO) ? null : corePos;
        }
        if (entity instanceof HeaterBlockEntity
                || entity instanceof StirlingGeneratorBlockEntity
                || entity instanceof HeatBoilerBlockEntity
                || entity instanceof SolarBoilerBlockEntity
                || entity instanceof SteamEngineBlockEntity
                || entity instanceof GasTurbineBlockEntity
                || entity instanceof IndustrialTurbineBlockEntity
                || entity instanceof LeviathanTurbineBlockEntity
                || entity instanceof SteamCondenserBlockEntity
                || entity instanceof PoweredSteamCondenserBlockEntity
                || entity instanceof CoolingTowerBlockEntity
                || entity instanceof FractionTowerBlockEntity
                || entity instanceof FluidTankBlockEntity
                || entity instanceof CatalyticCrackerBlockEntity
                || entity instanceof DrainBlockEntity
                || entity instanceof DeuteriumExtractorBlockEntity
                || entity instanceof AirCompressorBlockEntity
                || entity instanceof GroundwaterPumpBlockEntity
                || entity instanceof PowerGaugeBlockEntity
                || entity instanceof GeothermalHeatExchangerBlockEntity
                || entity instanceof StrandCasterBlockEntity
                || entity instanceof ConveyorPressBlockEntity
                || entity instanceof RotaryFurnaceBlockEntity
                || entity instanceof SatelliteGroundStationBlockEntity
                || entity instanceof LegacyMachineBlockEntity
                || entity instanceof RbmkComponentBlockEntity
                || entity instanceof WandStructureBlockEntity
                || entity instanceof WandJigsawBlockEntity
                || entity instanceof WandTandemBlockEntity
                || entity instanceof WandLootBlockEntity
                || entity instanceof WandLogicBlockEntity) {
            return pos;
        }
        return null;
    }

    @Nullable
    private static OverlayData overlayData(BlockState state, BlockEntity entity) {
        if (entity instanceof HeaterBlockEntity heater) {
            return heaterOverlay(state, heater);
        }
        if (entity instanceof StirlingGeneratorBlockEntity stirling) {
            return stirlingOverlay(state, stirling);
        }
        if (entity instanceof HeatBoilerBlockEntity boiler) {
            return boilerOverlay(state, boiler);
        }
        if (entity instanceof com.atomiclegacy.tileentity.LegacyBatteryBlockEntity battery && battery.getMaxPower() > 0L) {
            // [parity d2: MachineBattery.printHook]
            List<OverlayLine> lines = new ArrayList<>();
            lines.add(OverlayLine.white(com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(battery.getPower()) + " / "
                    + com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(battery.getMaxPower()) + "HE"));
            double percent = (double) battery.getPower() / (double) battery.getMaxPower();
            int charge = (int) Math.floor(percent * 10_000D);
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
            lines.add(new OverlayLine((charge / 100D) + "%", color));
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }
        if (entity instanceof SolarBoilerBlockEntity boiler) {
            return solarBoilerOverlay(state, boiler);
        }
        if (entity instanceof SteamEngineBlockEntity engine) {
            return steamEngineOverlay(state, engine);
        }
        if (entity instanceof IndustrialTurbineBlockEntity turbine) {
            return industrialTurbineOverlay(state, turbine);
        }
        if (entity instanceof LeviathanTurbineBlockEntity turbine) {
            return leviathanTurbineOverlay(state, turbine);
        }
        if (entity instanceof SteamCondenserBlockEntity condenser) {
            return condenserOverlay(state, condenser);
        }
        if (entity instanceof PoweredSteamCondenserBlockEntity condenser) {
            return poweredCondenserOverlay(state, condenser);
        }
        if (entity instanceof CoolingTowerBlockEntity tower) {
            return coolingTowerOverlay(state, tower);
        }
        if (entity instanceof FractionTowerBlockEntity tower) {
            return fractionTowerOverlay(state, tower);
        }
        if (entity instanceof FluidTankBlockEntity tank) {
            return fluidTankOverlay(state, tank);
        }
        if (entity instanceof CatalyticCrackerBlockEntity cracker) {
            return catalyticCrackerOverlay(state, cracker);
        }
        // [parity Q3: chimney_brick/chimney_industrial have no ILookOverlay in 1.7.10]
        if (entity instanceof DrainBlockEntity drain) {
            return drainOverlay(state, drain);
        }
        if (entity instanceof DeuteriumExtractorBlockEntity extractor) {
            return deuteriumOverlay(state, extractor);
        }
        if (entity instanceof AirCompressorBlockEntity compressor) {
            return airCompressorOverlay(state, compressor);
        }
        // [parity L1: capacitor] capacitor_copper has no TileEntity/look overlay in 1.7.10.
        if (entity instanceof GroundwaterPumpBlockEntity pump) {
            return groundwaterPumpOverlay(state, pump);
        }
        if (entity instanceof PowerGaugeBlockEntity gauge) {
            return powerGaugeOverlay(state, gauge);
        }
        if (entity instanceof GeothermalHeatExchangerBlockEntity exchanger) {
            return geothermalHeatExchangerOverlay(state, exchanger);
        }
        if (entity instanceof StrandCasterBlockEntity caster) {
            return strandCasterOverlay(state, caster);
        }
        // [parity Q1: FoundryCastingBase#printHook and FoundryOutlet#printHook]
        if (entity instanceof com.atomiclegacy.tileentity.FoundryCastingBlockEntity casting) {
            List<OverlayLine> lines = new ArrayList<>();
            FoundryMoldItem.Mold mold = casting.getInstalledMold();
            if (casting.getItem(com.atomiclegacy.tileentity.FoundryCastingBlockEntity.MOLD_SLOT).isEmpty()) {
                lines.add(new OverlayLine(Component.translatable("foundry.noCast").getString(), 0xFF5555));
            } else if (mold != null) {
                lines.add(new OverlayLine(mold.title().getString(), 0x5555FF));
            }
            if (casting.material() != null && casting.amount() > 0) {
                lines.add(new OverlayLine(Component.translatable(casting.material().translationKey()).getString() + ": "
                        + casting.amount() + " / " + casting.getCapacity(), 0xFFFF55));
            }
            return new OverlayData(state.getBlock().getName(), 0xFF4000, lines);
        }
        if (entity instanceof com.atomiclegacy.tileentity.FoundryFlowBlockEntity outlet
                && state.getBlock() instanceof com.atomiclegacy.blocks.FoundryOutletBlock) {
            List<OverlayLine> lines = new ArrayList<>();
            if (outlet.filter() != null) {
                lines.add(new OverlayLine(Component.translatable("foundry.filter",
                        Component.translatable(outlet.filter().translationKey())).getString(), 0xFFFF55));
            }
            if (outlet.isFilterInverted()) {
                lines.add(new OverlayLine(Component.translatable("foundry.invertFilter").getString(), 0xFFFF55));
            }
            if (outlet.isRedstoneInverted()) {
                lines.add(new OverlayLine(Component.translatable("foundry.inverted").getString(), 0xAA0000));
            }
            return new OverlayData(state.getBlock().getName(), 0xFF4000, lines);
        }
        if (entity instanceof ConveyorPressBlockEntity press) {
            return conveyorPressOverlay(state, press);
        }
        if (entity instanceof SatelliteGroundStationBlockEntity station) {
            return satelliteGroundStationOverlay(state, station);
        }
        if (entity instanceof LegacyMachineBlockEntity machine) {
            return legacyMachineOverlay(state, machine);
        }
        if (entity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isColumn()) {
            return rbmkDoddOverlay(state, rbmk);
        }
        if (entity instanceof WandStructureBlockEntity structure) {
            return wandStructureOverlay(state, structure);
        }
        if (entity instanceof WandTandemBlockEntity tandem) {
            return wandTandemOverlay(state, tandem);
        }
        if (entity instanceof WandJigsawBlockEntity jigsaw) {
            return wandJigsawOverlay(state, jigsaw);
        }
        if (entity instanceof WandLootBlockEntity loot) {
            return wandLootOverlay(state, loot);
        }
        if (entity instanceof WandLogicBlockEntity logic) {
            return wandLogicOverlay(state, logic);
        }
        return null;
    }

    private static OverlayData satelliteGroundStationOverlay(BlockState state,
                                                              SatelliteGroundStationBlockEntity station) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable(
                "overlay.atomiclegacy.satellite_ground_station.frequency", station.frequency()).getString()));
        int connectionColor = station.connected() ? 0x00FF00 : 0xFF0000;
        lines.add(new OverlayLine(Component.translatable(
                "overlay.atomiclegacy.satellite_ground_station.connected",
                Component.translatable(station.connected()
                        ? "overlay.atomiclegacy.satellite_ground_station.yes"
                        : "overlay.atomiclegacy.satellite_ground_station.no")).getString(), connectionColor));
        for (Component component : station.satelliteInfo()) {
            lines.add(OverlayLine.white(component.getString()));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    @Nullable
    private static OverlayData heaterOverlay(BlockState state, HeaterBlockEntity heater) {
        List<OverlayLine> lines = new ArrayList<>();
        switch (heater.kind()) {
            case FIREBOX, OVEN -> {
                lines.add(OverlayLine.white(String.format("%,d TU", heater.getHeatStored())));
                lines.add(OverlayLine.white("<- " + String.format("%,d TU/t", heater.burnHeat())));
            }
            case OILBURNER -> {
                lines.add(flueOrSmokeLine());
                lines.add(OverlayLine.white("<- " + String.format("%,d TU/t", heater.setting() * 160)));
            }
            case ELECTRIC -> {
                lines.add(OverlayLine.white(String.format("%,d TU", heater.getHeatStored())));
                lines.add(flueOrSmokeLine());
                lines.add(OverlayLine.white("<- " + heater.heatGeneration() + " TU/t"));
            }
            case HEATEX -> lines.add(OverlayLine.white(String.format("%,d TU", heater.getHeatStored())));
            default -> {
                return null;
            }
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData boilerOverlay(BlockState state, HeatBoilerBlockEntity boiler) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(heatLine("", boiler.heat()));
        lines.add(fluidLine("-> ", boiler.inputTank()));
        lines.add(fluidLine("<- ", boiler.outputTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData solarBoilerOverlay(BlockState state, SolarBoilerBlockEntity boiler) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineSolarBoiler.printHook]
        lines.add(fluidLine("-> ", boiler.inputTank()));
        lines.add(fluidLine("<- ", boiler.outputTank()));
        if (boiler.heat() < 1) {
            lines.add(new OverlayLine("Too cold!", System.currentTimeMillis() % 1000L < 500L ? 0xFF0000 : 0xFFFF00));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData steamEngineOverlay(BlockState state, SteamEngineBlockEntity engine) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineSteamEngine.printHook - %,d fill / %,d max]
        lines.add(OverlayLine.white(q3FluidText("-> ", engine.inputTank(), engine.inputTank().type(), " / ")));
        lines.add(OverlayLine.white(q3FluidText("<- ", engine.outputTank(), engine.outputTank().type(), " / ")));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData industrialTurbineOverlay(BlockState state, IndustrialTurbineBlockEntity turbine) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineIndustrialTurbine.printHook]
        lines.add(OverlayLine.white(q3FluidText("-> ", turbine.inputTank(), turbine.inputTank().type(), "/")));
        lines.add(OverlayLine.white(q3FluidText("<- ", turbine.outputTank(), q3CoolsTo(turbine.inputTank().type()), "/")));
        int spinColor = ((int) (0xFF - 0xFF * turbine.spin())) << 16 | ((int) (0xFF * turbine.spin()) << 8);
        String[] blocks = {"▖ ", "▘ ", " ▘", " ▖"};
        int time = Minecraft.getInstance().level == null ? 0 : (int) ((Minecraft.getInstance().level.getGameTime() / 4) % 4);
        lines.add(new OverlayLine("§c<- §f" + com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(turbine.powerBuffer()) + "HE (§r"
                + blocks[turbine.powerBuffer() <= 0 ? 0 : time] + (int) Math.round(turbine.spin() * 100) + "%§f)", spinColor));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData leviathanTurbineOverlay(BlockState state, LeviathanTurbineBlockEntity turbine) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineChungus.printHook]
        lines.add(OverlayLine.white(q3FluidText("-> ", turbine.inputTank(), turbine.inputTank().type(), "/")));
        lines.add(OverlayLine.white(q3FluidText("<- ", turbine.outputTank(), q3CoolsTo(turbine.inputTank().type()), "/")));
        lines.add(OverlayLine.white("<- " + com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(turbine.powerBuffer()) + "HE"));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData condenserOverlay(BlockState state, SteamCondenserBlockEntity condenser) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineCondenser.printHook]
        lines.add(fluidLine("-> ", condenser.inputTank()));
        lines.add(fluidLine("<- ", condenser.outputTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData poweredCondenserOverlay(BlockState state, PoweredSteamCondenserBlockEntity condenser) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineCondenserPowered.printHook]
        lines.add(OverlayLine.white(com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(condenser.power()) + "HE / "
                + com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(PoweredSteamCondenserBlockEntity.MAX_POWER) + "HE"));
        lines.add(OverlayLine.white(q3FluidText("-> ", condenser.inputTank(), condenser.inputTank().type(), "/")));
        lines.add(OverlayLine.white(q3FluidText("<- ", condenser.outputTank(), condenser.outputTank().type(), "/")));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData coolingTowerOverlay(BlockState state, CoolingTowerBlockEntity tower) {
        List<OverlayLine> lines = new ArrayList<>();
        // [parity Q3: MachineTowerSmall/Large.printHook]
        lines.add(fluidLine("-> ", tower.inputTank()));
        lines.add(fluidLine("<- ", tower.outputTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData fractionTowerOverlay(BlockState state, FractionTowerBlockEntity tower) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", tower.tank(0)));
        lines.add(fluidLine("<- ", tower.tank(1)));
        lines.add(fluidLine("<- ", tower.tank(2)));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    @Nullable
    private static OverlayData fluidTankOverlay(BlockState state, FluidTankBlockEntity tank) {
        if (!tank.isDamaged()
                || Minecraft.getInstance().player == null
                || !(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof BlowtorchItem)) {
            return null;
        }
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(new OverlayLine(
                Component.translatable("overlay.atomiclegacy.fluid_tank.repair_with").getString(),
                0xFFAA00));
        lines.add(OverlayLine.white(Component.translatable(
                "overlay.atomiclegacy.fluid_tank.steel_plates", 6).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData catalyticCrackerOverlay(BlockState state, CatalyticCrackerBlockEntity cracker) {
        List<OverlayLine> lines = new ArrayList<>();
        for (int index = 0; index < CatalyticCrackerBlockEntity.TANK_COUNT; index++) {
            lines.add(fluidLine(index < CatalyticCrackerBlockEntity.OUTPUT_LEFT_TANK ? "-> " : "<- ", cracker.tank(index)));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData groundwaterPumpOverlay(BlockState state, GroundwaterPumpBlockEntity pump) {
        List<OverlayLine> lines = new ArrayList<>();
        if (pump.kind() == com.atomiclegacy.blocks.GroundwaterPumpBlock.Kind.STEAM) {
            lines.add(fluidLine("-> ", pump.steamTank()));
            lines.add(fluidLine("<- ", pump.spentSteamTank()));
            lines.add(fluidLine("<- ", pump.waterTank()));
        } else {
            lines.add(energyStorageLine(
                    "-> ",
                    pump.power(),
                    GroundwaterPumpBlockEntity.ELECTRIC_MAX_POWER,
                    HbmLookOverlayApi.COLOR_GREEN
            ));
            lines.add(fluidLine("<- ", pump.waterTank()));
        }

        int warningColor = System.currentTimeMillis() % 1000L < 500L ? 0xFF0000 : 0xFFFF00;
        // [parity L2: pumps] MachinePump.printHook
        if (pump.getBlockPos().getY() > GroundwaterPumpBlockEntity.GROUND_HEIGHT) {
            lines.add(new OverlayLine("! ! ! ALTITUDE ! ! !", warningColor));
        }
        if (!pump.hasValidWaterSource()) {
            lines.add(new OverlayLine("! ! ! NO VALID GROUND ! ! !", warningColor));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData airCompressorOverlay(BlockState state, AirCompressorBlockEntity compressor) {
        List<OverlayLine> lines = new ArrayList<>();
        int powerColor = compressor.power() < AirCompressorBlockEntity.POWER_PER_TICK ? 0xFF5555 : 0x55FF55;
        // [parity Q3: MachineIntake.printHook - BobMathUtil.getShortNumber]
        lines.add(new OverlayLine("Power: " + com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(compressor.power()) + "HE", powerColor));
        lines.add(fluidLine("<- ", compressor.airTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData powerGaugeOverlay(BlockState state, PowerGaugeBlockEntity gauge) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d HE/t", gauge.deltaTick())));
        lines.add(OverlayLine.white(String.format("%,d HE/s", gauge.deltaLastSecond())));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData geothermalHeatExchangerOverlay(BlockState state, GeothermalHeatExchangerBlockEntity exchanger) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d TU", exchanger.bufferedHeat())));
        lines.add(new OverlayLine(fluidText("-> ", exchanger.inputTank()), 0x00FF00));
        lines.add(new OverlayLine(fluidText("<- ", exchanger.outputTank()), 0xFF5555));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData strandCasterOverlay(BlockState state, StrandCasterBlockEntity caster) {
        List<OverlayLine> lines = new ArrayList<>();
        FoundryMoldItem.Mold mold = caster.getInstalledMold();
        if (mold == null) {
            lines.add(new OverlayLine(Component.translatable("foundry.noCast").getString(), 0xFF5555));
        } else {
            lines.add(new OverlayLine(mold.title().getString(), 0x5555FF));
        }
        return new OverlayData(state.getBlock().getName(), 0xFF4000, lines);
    }

    /** MachineRotaryFurnace#printHook from 1.7.10: only the four actual ports show data. */
    @Nullable
    private static OverlayData rotaryFurnaceOverlay(BlockState state, RotaryFurnaceBlockEntity furnace,
                                                    BlockPos corePos, BlockPos hitPos) {
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        Direction turn = rotaryFurnacePortTurn(facing);
        BlockPos steamNear = corePos.relative(facing.getOpposite()).relative(turn.getOpposite());
        BlockPos steamFar = corePos.relative(facing.getOpposite()).relative(turn.getOpposite(), 2);
        BlockPos additiveFront = corePos.relative(facing).relative(turn, 2);
        BlockPos additiveBack = corePos.relative(facing.getOpposite()).relative(turn, 2);
        BlockPos fuel = corePos.relative(facing).relative(turn);
        List<OverlayLine> lines = new ArrayList<>();

        if (hitPos.equals(steamNear) || hitPos.equals(steamFar)) {
            lines.add(new OverlayLine("-> " + Component.translatable(furnace.steamTank().type().translationKey()).getString(), 0x00FF00));
            lines.add(new OverlayLine("<- " + Component.translatable(furnace.spentSteamTank().type().translationKey()).getString(), 0xFF0000));
        } else if (hitPos.equals(additiveFront) || hitPos.equals(additiveBack)) {
            lines.add(new OverlayLine("-> " + Component.translatable(furnace.additiveTank().type().translationKey()).getString(), 0x00FF00));
        } else if (hitPos.equals(fuel)) {
            lines.add(new OverlayLine("-> Fuel", 0xFFFF00));
        } else {
            return null;
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    /** Exact ForgeDirection#getRotation(DOWN) table used only by MachineRotaryFurnace#printHook. */
    private static Direction rotaryFurnacePortTurn(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
            default -> facing;
        };
    }

    /** MachineTurbineGas#printHook from 1.7.10: only service dummies expose an overlay. */
    @Nullable
    private static OverlayData gasTurbineOverlay(BlockState state, GasTurbineBlockEntity turbine,
                                                  BlockPos corePos, BlockPos hitPos) {
        Direction facing = state.hasProperty(GasTurbineBlock.FACING)
                ? state.getValue(GasTurbineBlock.FACING)
                : Direction.SOUTH;
        List<OverlayLine> lines = new ArrayList<>();

        for (GasTurbineBlockEntity.Port port : GasTurbineBlockEntity.portsFor(corePos, facing)) {
            if (!port.pos().equals(hitPos)) {
                continue;
            }
            switch (port.kind()) {
                case FUEL_LUBE -> {
                    lines.add(fluidNameLine("-> ", turbine.fuelTank()));
                    lines.add(fluidNameLine("-> ", turbine.lubricantTank()));
                }
                case WATER -> lines.add(fluidNameLine("-> ", turbine.waterTank()));
                case STEAM -> lines.add(fluidNameLine("<- ", turbine.steamTank()));
            }
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }

        if (GasTurbineBlockEntity.powerConnectors(corePos, facing).contains(hitPos)) {
            lines.add(OverlayLine.white("<- Power"));
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }
        return null;
    }

    /** 1.7.10 MachineConveyorPress#printHook. */
    private static OverlayData conveyorPressOverlay(BlockState state, ConveyorPressBlockEntity press) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable(
                "overlay.atomiclegacy.conveyor_press.power",
                String.format("%,d", press.power()),
                String.format("%,d", ConveyorPressBlockEntity.MAX_POWER)
        ).getString()));

        if (press.stamp().isEmpty()) {
            lines.add(new OverlayLine(Component.translatable(
                    "overlay.atomiclegacy.conveyor_press.stamp",
                    Component.translatable("overlay.atomiclegacy.conveyor_press.none")
            ).getString(), 0xFF5555));
        } else {
            lines.add(OverlayLine.white(Component.translatable(
                    "overlay.atomiclegacy.conveyor_press.stamp",
                    press.stamp().getHoverName()
            ).getString()));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData rbmkDoddOverlay(BlockState state, RbmkComponentBlockEntity rbmk) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(new OverlayLine(state.getBlock().getName().getString(), 0xFFFF00));
        for (Map.Entry<String, String> entry : rbmk.diagnosticData().entrySet()) {
            String label = Component.translatable("tile.rbmk.dodd." + entry.getKey()).getString();
            lines.add(OverlayLine.white(label + ": " + entry.getValue()));
        }
        return new OverlayData(Component.literal("Dump of Ordered Data Diagnostic (DODD)"), 0x00FF00, lines);
    }

    private static OverlayData wandStructureOverlay(BlockState state, WandStructureBlockEntity structure) {
        if (state.hasProperty(WandStructureBlock.LOAD) && state.getValue(WandStructureBlock.LOAD)) {
            return null;
        }
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.name", structure.name).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.size", structure.sizeX, structure.sizeY, structure.sizeZ).getString()));
        lines.add(new OverlayLine(Component.translatable("overlay.atomiclegacy.wand.blacklist").getString(), 0xA0A0A0));
        for (var key : structure.blacklist()) {
            lines.add(new OverlayLine("- " + key.id() + " : " + key.meta(), 0xFF5555));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandJigsawOverlay(BlockState state, WandJigsawBlockEntity jigsaw) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.target_pool", jigsaw.pool).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.name", jigsaw.name).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.target_name", jigsaw.target).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.turns_into", jigsaw.replaceBlock).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.meta", jigsaw.replaceMeta).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.priority", jigsaw.selectionPriority, jigsaw.placementPriority).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.joint_type", jointName(jigsaw.rollable)).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandTandemOverlay(BlockState state, WandTandemBlockEntity tandem) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.target_pool", tandem.pool).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.target_name", tandem.target).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.turns_into", tandem.replaceBlock).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.meta", tandem.replaceMeta).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.joint_type", jointName(tandem.rollable)).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandLootOverlay(BlockState state, WandLootBlockEntity loot) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.loot_replace", loot.replaceBlock).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.meta", loot.replaceMeta).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.loot_pool", loot.poolName).getString()));
        if (!loot.replaceBlock.endsWith(":deco_loot")) {
            lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.min_items", loot.minItems).getString()));
            lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.max_items", loot.maxItems).getString()));
        }
        if (loot.lockCode != 0) {
            lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.container_locked").getString()));
            lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.lockpick_chance", loot.lockMod).getString()));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandLogicOverlay(BlockState state, WandLogicBlockEntity logic) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.action", logic.actionID).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.condition", logic.conditionID).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.interaction", blankFallback(logic.interactionID)).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.atomiclegacy.wand.disguise", blankFallback(logic.disguise)).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    @Nullable
    private static OverlayData legacyMachineOverlay(BlockState state, LegacyMachineBlockEntity machine) {
        if (machine.machineId().equals("machine_teleporter")) {
            List<OverlayLine> lines = new ArrayList<>();
            if (!machine.hasTeleporterTarget()) {
                lines.add(new OverlayLine(Component.translatable("overlay.atomiclegacy.teleporter.no_destination").getString(), 0xFF5555));
            } else {
                int powerColor = machine.energyStored() >= 1_000_000L ? 0x00FF00 : 0xFF5555;
                lines.add(new OverlayLine(Component.translatable(
                        "overlay.atomiclegacy.teleporter.power",
                        String.format(Locale.US, "%,d", machine.energyStored()),
                        String.format(Locale.US, "%,d", machine.energyCapacity())
                ).getString(), powerColor));
                lines.add(OverlayLine.white(Component.translatable(
                        "overlay.atomiclegacy.teleporter.destination",
                        machine.teleporterTargetX(),
                        machine.teleporterTargetY(),
                        machine.teleporterTargetZ(),
                        machine.teleporterLegacyDimensionId()
                ).getString()));
            }
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }
        if (!machine.machineId().equals("machine_thresher") && !machine.machineId().equals("machine_autosaw")) {
            return null;
        }
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("", machine.inputTank()));
        if (machine.machineId().equals("machine_thresher") && machine.thresherSuspended()) {
            lines.add(new OverlayLine(Component.translatable("block.atomiclegacy.machine_thresher.suspended").getString(), 0xFF5555));
        }
        if (machine.machineId().equals("machine_autosaw") && machine.autosawSuspended()) {
            lines.add(new OverlayLine(Component.translatable("block.atomiclegacy.machine_autosaw.suspended").getString(), 0xFF5555));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static String jointName(boolean rollable) {
        return Component.translatable(rollable ? "gui.atomiclegacy.wand.rollable" : "gui.atomiclegacy.wand.aligned").getString();
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? Component.translatable("overlay.atomiclegacy.wand.none").getString() : value;
    }

    private static OverlayData drainOverlay(BlockState state, DrainBlockEntity drain) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", drain.tank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData deuteriumOverlay(BlockState state, DeuteriumExtractorBlockEntity extractor) {
        List<OverlayLine> lines = new ArrayList<>();
        int powerColor = extractor.power() < extractor.powerPerOperation() ? 0xFF0000 : 0x00FF00;
        lines.add(new OverlayLine("Power: " + String.format("%,d", extractor.power()) + "HE", powerColor));
        lines.add(new OverlayLine(fluidText("-> ", extractor.inputTank()), 0x00FF00));
        lines.add(new OverlayLine(fluidText("<- ", extractor.outputTank()), 0xFF5555));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    // [parity Q3: overlay helpers] "<prefix><name>: %,d<sep>%,dmB" as in the 1.7.10 printHooks
    private static String q3FluidText(String prefix, com.atomiclegacy.fluid.HbmFluidTank tank, com.atomiclegacy.fluid.HbmFluidDefinition shownType, String separator) {
        return prefix + Component.translatable(shownType.translationKey()).getString() + ": "
                + String.format(java.util.Locale.US, "%,d", tank.amount()) + separator + String.format(java.util.Locale.US, "%,d", tank.capacity()) + "mB";
    }

    private static com.atomiclegacy.fluid.HbmFluidDefinition q3CoolsTo(com.atomiclegacy.fluid.HbmFluidDefinition input) {
        com.atomiclegacy.fluid.HbmFluidTraitData.Coolable coolable = com.atomiclegacy.fluid.HbmFluidTraitData.coolable(input);
        return coolable == null ? com.atomiclegacy.registry.HbmFluids.none() : com.atomiclegacy.registry.HbmFluids.byName(coolable.coolsTo()).orElse(com.atomiclegacy.registry.HbmFluids.none());
    }

    private static OverlayLine flueOrSmokeLine() {
        return OverlayLine.white("-> "
                + Component.translatable("hbmfluid.flue").getString()
                + " / "
                + Component.translatable("hbmfluid.smoke").getString());
    }

    private static OverlayData stirlingOverlay(BlockState state, StirlingGeneratorBlockEntity stirling) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(stirling.heat() + "TU/t"));
        lines.add(OverlayLine.white(stirling.currentOutput() + "HE/t"));

        if (!stirling.isCreative()) {
            int maxHeat = stirling.maxHeat();
            double percent = (double) stirling.heat() / (double) Math.max(1, maxHeat);
            int color = ((int) (0xFF - 0xFF * Math.min(percent, 1.0D))) << 16
                    | ((int) (0xFF * Math.min(percent, 1.0D)) << 8);
            if (percent > 1.0D) {
                color = 0xFF0000;
            }

            lines.add(new OverlayLine(((stirling.heat() * 1000 / maxHeat) / 10.0D) + "%", color));
            if (stirling.heat() > maxHeat) {
                lines.add(new OverlayLine(Component.translatable(
                        "message.atomiclegacy.stirling.overspeed"
                ).getString(), System.currentTimeMillis() % 1000L < 500L ? 0xFF0000 : 0xFFFF00)); // [parity Q3: BobMathUtil.getBlink]
            }
            if (!stirling.hasCog()) {
                lines.add(new OverlayLine(Component.translatable(
                        "message.atomiclegacy.stirling.gear_missing"
                ).getString(), 0xFF0000));
            }
        }

        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static void renderGeneric(GuiGraphics graphics, Font font, OverlayData data) {
        int x = graphics.guiWidth() / 2 + X_OFFSET;
        int y = graphics.guiHeight() / 2 + Y_OFFSET;

        graphics.drawString(font, data.title(), x + 1, y + 1, data.shadowColor(), false);
        graphics.drawString(font, data.title(), x, y, data.titleColor(), false);

        int lineY = y + TITLE_TO_LINES;
        for (OverlayLine line : data.lines()) {
            graphics.drawString(font, line.text(), x, lineY, line.color(), true);
            lineY += LINE_SPACING;
        }
    }

}

