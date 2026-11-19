package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyLauncherMissileEntity;
import com.atomiclegacy.items.SatelliteChipItem;
import com.atomiclegacy.items.SoyuzItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.satellite.SatelliteEventTracker;
import com.atomiclegacy.satellite.SatelliteSavedData;
import com.atomiclegacy.satellite.SatelliteSavedData.SatelliteKind;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [parity P2: diagnostics] Dedicated-server checks for the satellite / radar / Soyuz batch against
 * HbmMods 1.7.10 936c60c: XSatelliteRegistry item mapping, SatelliteBase command surface (exact-case
 * names, settarget/gettarget, mapper/radar/detector/ray-scan surveys, canfire), getInfo formatting,
 * part delivery to the space laboratory, TileEntityMachineRadarNT detection and power drain, Soyuz skin
 * metadata and the assembler satellite recipes.  Nothing is fired: the death ray, precision laser and
 * Horizons are only queried.  Pads sit near x = z = 18 000; test frequencies are removed afterwards.
 * Not covered: client GUIs/rendering, resonator teleport (needs a real player), Soyuz flight.
 */
final class ParityP2Diagnostics {
    private ParityP2Diagnostics() {
    }

    private static final int BASE_X = 18_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 18_000;
    private static final int FREQ = 987_650;
    private static final List<int[]> FORCED = new ArrayList<>();
    private static final List<Integer> USED_FREQUENCIES = new ArrayList<>();

    /** ass.* satellite recipes of AssemblyMachineRecipes: result|duration|power. */
    private static final String[] ASSEMBLER = {
            "satellite_science|1200|25000", "satellite_science_sensor|600|25000", "satellite_science_assembler|600|25000",
            "satellite_spy|1200|25000", "satellite_scanner|1200|25000", "satellite_radar|1200|25000",
            "satellite_miner_astro|1200|25000", "satellite_miner_lunar|1200|25000",
            "satellite_precision_laser|1200|25000", "satellite_death_ray|1200|25000",
            "satellite_xenium_resonator|1200|25000", "satellite_relay|1200|25000", "satellite_detector|1200|25000",
            "satellite_ray_scan|1200|25000", "missile_soyuz|6000|25000", "missile_soyuz_lander|2400|25000",
            "sat_dock|100|5000", "launchpad_soyuz|1200|5000", "machine_radar|300|100", "machine_radar_large|400|100"
    };

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            registry(problems, cases);
            BlockPos pad = pad(level, 0);
            commands(level, pad, problems, cases);
            science(level, pad, problems, cases);
            surveys(level, pad, problems, cases);
            radar(level, pad(level, 1), problems, cases);
            soyuz(problems, cases);
            soyuzLauncher(level, pad(level, 2), problems, cases);
            recipes(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity P2 diagnostics crashed", e);
            problems.add("P2 diagnostics crashed: " + e);
        } finally {
            SatelliteSavedData data = SatelliteSavedData.get(level);
            for (int frequency : USED_FREQUENCIES) data.descend(frequency);
            USED_FREQUENCIES.clear();
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    private static void registry(List<String> problems, int[] cases) {
        Object[][] expected = {
                {HbmItems.SATELLITE_SPY, SatelliteKind.MAPPER}, {HbmItems.SATELLITE_SCANNER, SatelliteKind.SCANNER},
                {HbmItems.SATELLITE_RADAR, SatelliteKind.RADAR}, {HbmItems.SATELLITE_DEATH_RAY, SatelliteKind.LASER},
                {HbmItems.SATELLITE_XENIUM_RESONATOR, SatelliteKind.RESONATOR}, {HbmItems.SATELLITE_RELAY, SatelliteKind.RELAY},
                {HbmItems.SATELLITE_MINER_ASTRO, SatelliteKind.MINER}, {HbmItems.SATELLITE_MINER_LUNAR, SatelliteKind.LUNAR_MINER},
                {HbmItems.SAT_GERALD, SatelliteKind.HORIZONS}, {HbmItems.SATELLITE_PRECISION_LASER, SatelliteKind.PRECISION_LASER},
                {HbmItems.SATELLITE_DETECTOR, SatelliteKind.DETECTOR}, {HbmItems.SATELLITE_RAY_SCAN, SatelliteKind.RAY_SCAN},
                {HbmItems.SATELLITE_SCIENCE, SatelliteKind.SCIENCE},
                // legacy chips: registerSatellite skips classes already mapped; FOEQ is never registered
                {HbmItems.SAT_MAPPER, null}, {HbmItems.SAT_SCANNER, null}, {HbmItems.SAT_RADAR, null},
                {HbmItems.SAT_LASER, null}, {HbmItems.SAT_RESONATOR, null}, {HbmItems.SAT_MINER, null},
                {HbmItems.SAT_LUNAR_MINER, null}, {HbmItems.SAT_FOEQ, null}, {HbmItems.SAT_CHIP, null},
                {HbmItems.SAT_RELAY, null}, {HbmItems.SATELLITE_SCIENCE_SENSOR, null}
        };
        for (Object[] row : expected) {
            @SuppressWarnings("unchecked")
            Item item = ((net.neoforged.neoforge.registries.DeferredItem<Item>) row[0]).get();
            SatelliteKind kind = SatelliteKind.fromItem(new ItemStack(item));
            check(kind == row[1], problems, cases, "registry: " + BuiltInRegistries.ITEM.getKey(item) + " -> " + kind + ", expected " + row[1]);
        }
    }

    private static SatelliteSavedData.SatelliteRecord launch(ServerLevel level, Item item, int frequency, BlockPos at) {
        ItemStack stack = new ItemStack(item);
        SatelliteChipItem.setFrequency(stack, frequency);
        SatelliteSavedData data = SatelliteSavedData.get(level);
        data.descend(frequency);
        USED_FREQUENCIES.add(frequency);
        data.orbit(level, stack, at.getX() + 0.7D, at.getZ() + 0.3D);
        return data.satellite(frequency).orElse(null);
    }

    private static void commands(ServerLevel level, BlockPos pad, List<String> problems, int[] cases) {
        SatelliteSavedData.SatelliteRecord ray = launch(level, HbmItems.SATELLITE_DEATH_RAY.get(), FREQ, pad);
        check(ray != null && ray.kind() == SatelliteKind.LASER, problems, cases, "orbit: death ray not created");
        if (ray == null) return;
        // onOrbit: setTarget(floor(x), floor(z))
        check(ray.targetX() == pad.getX() && ray.targetZ() == pad.getZ(), problems, cases,
                "orbit: target " + ray.targetX() + "/" + ray.targetZ() + " expected launch position");
        ray.onCommand(level, "settarget", Integer.toString(pad.getX() + 5), Integer.toString(pad.getZ() - 7));
        ray.onCommand(level, "gettarget");
        check(ray.tx().equals((pad.getX() + 5) + ";" + (pad.getZ() - 7)), problems, cases, "settarget/gettarget: " + ray.tx());
        ray.onCommand(level, "settarget", "1", "2", "3"); // four-token form uses tokens 1 and 3
        ray.onCommand(level, "gettargetz");
        check(ray.tx().equals("3"), problems, cases, "settarget x y z: gettargetz " + ray.tx());
        ray.onCommand(level, "GETTARGETX"); // equals(), not equalsIgnoreCase
        check(ray.tx().equals("3"), problems, cases, "command names are case sensitive: " + ray.tx());
        ray.onCommand(level, "canfire");
        String canFire = Boolean.toString(5 * 60 * 20 < level.getGameTime()).toUpperCase(Locale.US);
        check(ray.tx().equals(canFire), problems, cases, "death ray canfire " + ray.tx() + " expected " + canFire);
        List<SatelliteSavedData.SatelliteInfoLine> info = ray.infoLines(level);
        check(info.size() == 2 && info.get(0).translationKey().equals("item.atomiclegacy.satellite_death_ray"),
                problems, cases, "death ray getInfo " + info);
        if (info.size() == 2 && info.get(1).hasValue()) {
            check(info.get(1).value().endsWith("s"), problems, cases, "death ray cooldown \"" + info.get(1).value() + "\" lacks the s suffix");
        }

        SatelliteSavedData.SatelliteRecord horizons = launch(level, HbmItems.SAT_GERALD.get(), FREQ + 1, pad);
        check(horizons != null && horizons.kind() == SatelliteKind.HORIZONS, problems, cases, "orbit: Horizons not created");
        if (horizons != null) {
            horizons.onCommand(level, "settarget"); // SatelliteHorizons.CMD_CANFIRE is "settarget"
            check(horizons.tx().equals("TRUE") && !horizons.used(), problems, cases, "Horizons canfire " + horizons.tx());
        }

        SatelliteSavedData.SatelliteRecord mapper = launch(level, HbmItems.SATELLITE_SPY.get(), FREQ + 2, pad);
        check(mapper != null, problems, cases, "orbit: spy satellite not created");
        if (mapper != null) {
            mapper.onCommand(level, "targetloaded");
            check(mapper.tx().equals("TRUE"), problems, cases, "mapper targetloaded " + mapper.tx());
            mapper.onCommand(level, "findplayer", "nobody_p2");
            check(mapper.tx().equals("NOT FOUND"), problems, cases, "mapper findplayer " + mapper.tx());
            mapper.onCommand(level, "spotplayers");
            boolean anyPlayer = !level.players().isEmpty();
            check(anyPlayer || mapper.tx().equals("NONE"), problems, cases, "mapper spotplayers " + mapper.tx());
        }

        SatelliteSavedData.SatelliteRecord laser = launch(level, HbmItems.SATELLITE_PRECISION_LASER.get(), FREQ + 3, pad);
        if (laser != null) {
            laser.onCommand(level, "canfire");
            String expected = Boolean.toString(5 * 20 < level.getGameTime()).toUpperCase(Locale.US);
            check(laser.tx().equals(expected), problems, cases, "precision laser canfire " + laser.tx());
        } else {
            check(false, problems, cases, "orbit: precision laser not created");
        }

        // legacy chips and sat_foeq do not occupy a frequency
        SatelliteSavedData.SatelliteRecord legacy = launch(level, HbmItems.SAT_LASER.get(), FREQ + 4, pad);
        check(legacy == null, problems, cases, "legacy sat_laser created a satellite");
    }

    private static void science(ServerLevel level, BlockPos pad, List<String> problems, int[] cases) {
        SatelliteSavedData.SatelliteRecord lab = launch(level, HbmItems.SATELLITE_SCIENCE.get(), FREQ + 10, pad);
        check(lab != null && lab.kind() == SatelliteKind.SCIENCE, problems, cases, "orbit: space laboratory not created");
        if (lab == null) return;
        ItemStack sensor = new ItemStack(HbmItems.SATELLITE_SCIENCE_SENSOR.get());
        SatelliteChipItem.setFrequency(sensor, FREQ + 10);
        SatelliteSavedData.get(level).orbit(level, sensor, pad.getX(), pad.getZ());
        check(lab.sensorCount() == 1, problems, cases, "sensor relay delivered as part: sensorCount " + lab.sensorCount());
        boolean sensorsLine = lab.infoLines(level).stream().anyMatch(line ->
                line.translationKey().equals("satellite.atomiclegacy.sensors") && line.value().equals("1"));
        check(sensorsLine, problems, cases, "science getInfo sensor line missing");
        boolean pending = lab.infoLines(level).stream().anyMatch(line ->
                line.translationKey().equals("satellite.atomiclegacy.pending")
                        && line.value().equals(String.format(Locale.US, "%,d", 100 * 60 * 60 * 20 - lab.sensorProgress())));
        check(pending, problems, cases, "science pending uses BobMathUtil.format grouping");
        int cooldown = (int) (15 * 60 * 20 - level.getGameTime()); // lastScience starts at 0
        if (cooldown > 0) {
            int seconds = cooldown / 20;
            String text = (seconds / 60) + "m" + (seconds % 60) + "s";
            boolean line = lab.infoLines(level).stream().anyMatch(l -> l.value().equals(text));
            check(line, problems, cases, "science cooldown format " + text);
        }
    }

    private static void surveys(ServerLevel level, BlockPos pad, List<String> problems, int[] cases) {
        SatelliteSavedData.SatelliteRecord detector = launch(level, HbmItems.SATELLITE_DETECTOR.get(), FREQ + 20, pad);
        SatelliteSavedData.SatelliteRecord rayScan = launch(level, HbmItems.SATELLITE_RAY_SCAN.get(), FREQ + 21, pad);
        SatelliteSavedData.SatelliteRecord radar = launch(level, HbmItems.SATELLITE_RADAR.get(), FREQ + 22, pad);
        if (detector == null || rayScan == null || radar == null) {
            check(false, problems, cases, "orbit: detector/ray scan/radar satellite not created");
            return;
        }
        detector.onCommand(level, "survey");
        detector.onCommand(level, "count");
        int before = Integer.parseInt(detector.tx());
        SatelliteEventTracker.reportRadiation(level, SatelliteEventTracker.DETECTOR_HIGH_DURATION,
                SatelliteEventTracker.BurstIntensity.HIGH, pad.getX() + 0.5D, pad.getZ() + 0.5D);
        detector.onCommand(level, "survey");
        detector.onCommand(level, "count");
        check(Integer.parseInt(detector.tx()) == before + 1, problems, cases, "detector survey count " + detector.tx() + " expected " + (before + 1));
        detector.onCommand(level, "gettype", Integer.toString(before + 1));
        check(detector.tx().equals("HIGH"), problems, cases, "detector gettype " + detector.tx());
        detector.onCommand(level, "getposition", Integer.toString(before + 1));
        String[] xz = detector.tx().split(";");
        check(xz.length == 2 && Math.abs(Integer.parseInt(xz[0]) - pad.getX()) <= 501
                && Math.abs(Integer.parseInt(xz[1]) - pad.getZ()) <= 501, problems, cases, "detector HIGH inaccuracy > 500: " + detector.tx());

        SatelliteEventTracker.reportRay(level, pad.above(3), SatelliteEventTracker.RayEvent.INFO_RADIO, 300);
        rayScan.onCommand(level, "settarget", Integer.toString(pad.getX() + 200), Integer.toString(pad.getZ()));
        rayScan.onCommand(level, "survey");
        rayScan.onCommand(level, "count");
        check(rayScan.tx().equals("1"), problems, cases, "ray scan survey within 250: count " + rayScan.tx());
        rayScan.onCommand(level, "getinfo", "1");
        check(rayScan.tx().equals("RADIO_WAVES"), problems, cases, "ray scan getinfo " + rayScan.tx());
        rayScan.onCommand(level, "settarget", Integer.toString(pad.getX() + 251), Integer.toString(pad.getZ()));
        rayScan.onCommand(level, "survey");
        rayScan.onCommand(level, "count");
        check(rayScan.tx().equals("0"), problems, cases, "ray scan range 250: count " + rayScan.tx());

        LegacyLauncherMissileEntity missile = LegacyLauncherMissileEntity.create(level,
                new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("missile_generic"))),
                pad.getX() + 10.5D, BASE_Y + 50.0D, pad.getZ() + 0.5D, pad.getX(), pad.getZ(), Direction.NORTH);
        Entity pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        try {
            boolean added = level.addFreshEntity(missile);
            String where = "added=" + added + " byId=" + (level.getEntity(missile.getId()) != null)
                    + " lookup=" + level.getEntities((Entity) null, missile.getBoundingBox().inflate(2.0D), e -> true).size()
                    + " removed=" + missile.isRemoved();
            if (pig != null) {
                pig.setPos(pad.getX() + 2.5D, BASE_Y + 1.0D, pad.getZ() + 2.5D);
                level.addFreshEntity(pig);
            }
            radar.onCommand(level, "survey");
            radar.onCommand(level, "filter", "legacylaunchermissile");
            radar.onCommand(level, "count");
            check(radar.tx().equals("1"), problems, cases, "radar satellite survey/filter count " + radar.tx() + " " + where);
            radar.onCommand(level, "getname", "1");
            check(radar.tx().equals("legacylaunchermissileentity"), problems, cases, "radar satellite getname " + radar.tx());
            radar.onCommand(level, "filter", "pig");
            radar.onCommand(level, "count");
            check(radar.tx().equals("0"), problems, cases, "radar satellite lists a non-detectable pig: " + radar.tx());
        } finally {
            missile.discard();
            if (pig != null) pig.discard();
        }
    }

    private static void radar(ServerLevel level, BlockPos pad, List<String> problems, int[] cases) {
        BlockState state = HbmBlocks.MACHINE_RADAR.get().defaultBlockState();
        level.setBlock(pad, state, 3);
        state.getBlock().setPlacedBy(level, pad, state, null, ItemStack.EMPTY);
        if (!(level.getBlockEntity(pad) instanceof LegacyMachineBlockEntity radar)) {
            check(false, problems, cases, "machine_radar has no block entity");
            return;
        }
        LegacyLauncherMissileEntity missile = LegacyLauncherMissileEntity.create(level,
                new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("missile_generic"))),
                pad.getX() + 10.5D, pad.getY() + 40.0D, pad.getZ() + 0.5D, pad.getX(), pad.getZ(), Direction.NORTH);
        try {
            boolean added = level.addFreshEntity(missile);
            String where = "added=" + added + " byId=" + (level.getEntity(missile.getId()) != null) + " removed=" + missile.isRemoved();
            radar.applyPower(0L, 400L); // below TileEntityMachineRadarNT.consumption (500)
            LegacyMachineBlockEntity.tick(level, pad, level.getBlockState(pad), radar);
            check(radar.energyStored() == 0L && radar.radarTargets().isEmpty(), problems, cases,
                    "radar below consumption: energy " + radar.energyStored() + ", targets " + radar.radarTargets().size());
            radar.applyPower(0L, 10_000L);
            LegacyMachineBlockEntity.tick(level, pad, level.getBlockState(pad), radar);
            check(radar.radarTargets().size() == 1, problems, cases, "radar missile detection: " + radar.radarTargets().size() + " " + where);
            check(radar.energyStored() == 10_000L - radar.radarConsumptionValue(), problems, cases,
                    "radar consumption: energy " + radar.energyStored());
            check(radar.radarRangeValue() == 1_000, problems, cases, "radar range " + radar.radarRangeValue());
            missile.setPos(pad.getX() + 10.5D, pad.getY() + 30.0D, pad.getZ() + 0.5D); // radarBuffer: posY - y > 30
            LegacyMachineBlockEntity.tick(level, pad, level.getBlockState(pad), radar);
            check(radar.radarTargets().isEmpty(), problems, cases, "radar buffer of 30 blocks not applied");
        } finally {
            missile.discard();
            level.setBlock(pad, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    private static void soyuz(List<String> problems, int[] cases) {
        for (int skin = 0; skin < 3; skin++) {
            ItemStack stack = new ItemStack(HbmItems.MISSILE_SOYUZ.get());
            if (skin > 0) SoyuzItem.setSkin(stack, skin);
            Rarity expected = skin == 0 ? Rarity.UNCOMMON : skin == 1 ? Rarity.RARE : Rarity.EPIC;
            check(SoyuzItem.skin(stack) == skin && stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON) == expected,
                    problems, cases, "missile_soyuz skin " + skin + " rarity " + stack.get(DataComponents.RARITY));
        }
        check(new ItemStack(HbmItems.MISSILE_SOYUZ_LANDER.get()).getRarity() == Rarity.COMMON, problems, cases,
                "missile_soyuz_lander rarity (ItemCustomLore default)");
    }

    /** TileEntitySoyuzStruct assembly, then one satellite-mode launch of TileEntitySoyuzLauncher. */
    private static void soyuzLauncher(ServerLevel level, BlockPos core, List<String> problems, int[] cases) {
        net.minecraft.world.level.block.Block launcher = HbmBlocks.STRUCT_LAUNCHER.get();
        net.minecraft.world.level.block.Block concrete = HbmBlocks.CONCRETE_SMOOTH.get();
        net.minecraft.world.level.block.Block scaffold = HbmBlocks.STRUCT_SCAFFOLD.get();
        java.util.function.BiConsumer<int[], net.minecraft.world.level.block.Block> box = (b, block) -> {
            for (int i = b[0]; i <= b[1]; i++) for (int j = b[2]; j <= b[3]; j++) for (int k = b[4]; k <= b[5]; k++)
                level.setBlock(core.offset(i, j, k), block.defaultBlockState(), 2 | 16);
        };
        level.setBlock(core, HbmBlocks.STRUCT_SOYUZ_CORE.get().defaultBlockState(), 3);
        box.accept(new int[] {-6, 6, 3, 4, -6, 6}, launcher);
        box.accept(new int[] {-1, 1, 3, 4, -8, -7}, launcher);
        box.accept(new int[] {-2, 2, 3, 4, 7, 9}, launcher);
        box.accept(new int[] {-2, 2, 51, 51, 5, 9}, launcher);
        box.accept(new int[] {-1, 1, 38, 38, -8, -6}, launcher);
        box.accept(new int[] {3, 6, 0, 2, 3, 6}, concrete);
        box.accept(new int[] {-6, -3, 0, 2, 3, 6}, concrete);
        box.accept(new int[] {-6, -3, 0, 2, -6, -3}, concrete);
        box.accept(new int[] {3, 6, 0, 2, -6, -3}, concrete);
        box.accept(new int[] {-1, 1, 0, 2, -8, -6}, concrete);
        box.accept(new int[] {-2, 2, 0, 2, 5, 9}, concrete);
        box.accept(new int[] {-1, 1, 5, 49, 6, 8}, scaffold); // one layer short: must not assemble
        box.accept(new int[] {0, 0, 5, 37, -7, -7}, scaffold);
        var struct = (com.atomiclegacy.tileentity.SoyuzStructBlockEntity) level.getBlockEntity(core);
        for (int t = 0; t < 20; t++) com.atomiclegacy.tileentity.SoyuzStructBlockEntity.tick(level, core, level.getBlockState(core), struct);
        check(level.getBlockState(core).is(HbmBlocks.STRUCT_SOYUZ_CORE.get()), problems, cases, "struct_soyuz_core assembled with missing scaffold");
        box.accept(new int[] {-1, 1, 50, 50, 6, 8}, scaffold);
        for (int t = 0; t < 20; t++) com.atomiclegacy.tileentity.SoyuzStructBlockEntity.tick(level, core, level.getBlockState(core), struct);
        BlockPos top = core.above(4);
        check(level.getBlockState(core).isAir() && level.getBlockState(top).is(HbmBlocks.SOYUZ_LAUNCHER.get())
                && level.getBlockState(core.offset(0, 20, 7)).isAir() && level.getBlockState(core.offset(0, 51, 7)).isAir(),
                problems, cases, "struct_soyuz_core did not build soyuz_launcher / clear the scaffolding");
        check(level.getBlockState(top.offset(6, 0, 6)).is(HbmBlocks.MACHINE_DUMMY.get())
                && level.getBlockState(top.offset(3, -4, 3)).is(HbmBlocks.MACHINE_DUMMY.get()), problems, cases,
                "soyuz_launcher footprint (table corner / leg) missing");
        if (!(level.getBlockEntity(top) instanceof com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity pad)) {
            check(false, problems, cases, "soyuz_launcher has no block entity");
            return;
        }
        ItemStack rocket = new ItemStack(HbmItems.MISSILE_SOYUZ.get());
        SoyuzItem.setSkin(rocket, 1);
        pad.setItem(0, rocket);
        ItemStack sat = new ItemStack(HbmItems.SATELLITE_SPY.get());
        SatelliteChipItem.setFrequency(sat, FREQ + 30);
        USED_FREQUENCIES.add(FREQ + 30);
        SatelliteSavedData.get(level).descend(FREQ + 30);
        pad.setItem(2, sat);
        pad.keroseneTank().setAmount(128_000);
        pad.oxygenTank().setAmount(128_000);
        check(!pad.canLaunch() && !pad.hasPower(), problems, cases, "soyuz_launcher launches without 750 000 HE");
        pad.applyTestPower(1_000_000L);
        check(pad.canLaunch() && pad.getFuelRequired() == 128_000 && pad.satellite() == 2 && pad.orbital() == 0,
                problems, cases, "soyuz_launcher satellite mode readiness");
        pad.startCountdown();
        int ticks = 0;
        while (!pad.getItem(0).isEmpty() && ticks < 700) {
            com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity.tick(level, top, level.getBlockState(top), pad);
            ticks++;
        }
        check(ticks == 601, problems, cases, "soyuz_launcher countdown " + ticks + " ticks, expected 600 + lift-off");
        check(pad.power() == 250_000L && pad.keroseneTank().amount() == 0 && pad.oxygenTank().amount() == 0
                && pad.getItem(2).isEmpty(), problems, cases, "soyuz_launcher lift-off cost: power " + pad.power());
        List<com.atomiclegacy.entity.SoyuzEntity> rockets = level.getEntitiesOfClass(com.atomiclegacy.entity.SoyuzEntity.class,
                new net.minecraft.world.phys.AABB(top).inflate(3.0D));
        check(rockets.size() == 1 && rockets.get(0).skin() == 1, problems, cases, "soyuz_launcher rocket entity/skin " + rockets.size());
        rockets.forEach(Entity::discard);
        pad.setMode(1);
        check(pad.designator() == 1 && pad.satellite() == 0 && pad.getFuelRequired() == 5000, problems, cases,
                "soyuz_launcher cargo mode requirements");
        level.setBlock(top, Blocks.AIR.defaultBlockState(), 2 | 16);
        for (Entity entity : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(top).inflate(16.0D),
                e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) entity.discard();
    }

    private static void recipes(ServerLevel level, List<String> problems, int[] cases) {
        var manager = level.getRecipeManager();
        for (String row : ASSEMBLER) {
            String[] parts = row.split("\\|");
            var holder = manager.byKey(AtomicLegacy.id("assembly_machine/" + parts[0]));
            check(holder.isPresent(), problems, cases, "assembler recipe missing: " + parts[0]);
            if (holder.isEmpty()) continue;
            if (holder.get().value() instanceof com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe recipe) {
                check(recipe.duration() == Integer.parseInt(parts[1]) && recipe.power() == Integer.parseInt(parts[2]),
                        problems, cases, "assembler " + parts[0] + ": " + recipe.duration() + "/" + recipe.power());
            }
        }
        // not in the reference: sat_chip comes only from the soldering station, sat_interface is removed
        check(manager.byKey(AtomicLegacy.id("sat_chip")).isEmpty(), problems, cases, "crafting recipe sat_chip must not exist");
        check(manager.byKey(AtomicLegacy.id("sat_interface")).isEmpty(), problems, cases, "crafting recipe sat_interface must not exist");
        check(manager.byKey(AtomicLegacy.id("soldering_station/sat_chip")).isPresent(), problems, cases, "soldering sat_chip missing");
        check(manager.byKey(AtomicLegacy.id("plasma_forge/sat_gerald")).isPresent(), problems, cases, "plsm.gerald missing");
        check(manager.byKey(AtomicLegacy.id("space_assembler/crystal_circuit")).isPresent(), problems, cases, "space.crystalcircuit missing");
    }

    private static BlockPos pad(ServerLevel level, int index) {
        BlockPos pos = new BlockPos(BASE_X + index * 64, BASE_Y, BASE_Z);
        for (int cx = (pos.getX() - 16) >> 4; cx <= (pos.getX() + 16) >> 4; cx++) {
            for (int cz = (pos.getZ() - 16) >> 4; cz <= (pos.getZ() + 16) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        // Entities are only visible to queries once the forced chunks are entity-ticking (as in ParityH3Diagnostics).
        long deadline = System.currentTimeMillis() + 20_000L;
        for (int cx = (pos.getX() - 16) >> 4; cx <= (pos.getX() + 16) >> 4; cx++) {
            for (int cz = (pos.getZ() - 16) >> 4; cz <= (pos.getZ() + 16) >> 4; cz++) {
                BlockPos probe = new BlockPos((cx << 4) + 8, BASE_Y, (cz << 4) + 8);
                while (!level.isPositionEntityTicking(probe)) {
                    if (System.currentTimeMillis() > deadline) {
                        throw new IllegalStateException("P2 test chunk " + cx + "," + cz + " never became entity-ticking");
                    }
                    if (!level.getChunkSource().pollTask()) Thread.onSpinWait();
                }
            }
        }
        for (int dx = -6; dx <= 6; dx++) for (int dy = -2; dy <= 8; dy++) for (int dz = -6; dz <= 6; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!ok) problems.add("P2 " + message);
    }
}
