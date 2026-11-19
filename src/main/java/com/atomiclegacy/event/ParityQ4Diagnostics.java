package com.atomiclegacy.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.PowerDetectorBlock;
import com.atomiclegacy.blocks.RadioboxBlock;
import com.atomiclegacy.blocks.RefuelerBlock;
import com.atomiclegacy.items.SirenTrackItem;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmRadiationWorlds;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.DrainBlockEntity;
import com.atomiclegacy.tileentity.ExcavatorBlockEntity;
import com.atomiclegacy.tileentity.FunnelBlockEntity;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.MiningLaserBlockEntity;
import com.atomiclegacy.tileentity.PowerDetectorBlockEntity;
import com.atomiclegacy.tileentity.RadioboxBlockEntity;
import com.atomiclegacy.tileentity.RefuelerBlockEntity;
import com.atomiclegacy.tileentity.SirenBlockEntity;
import com.atomiclegacy.tileentity.StorageDrumBlockEntity;
import com.atomiclegacy.tileentity.TapeDriveBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * [parity Q4] Dedicated-server checks for the utility machines against the pinned 1.7.10 sources
 * (ModBlocks, TileEntityMachineDetector, GeigerCounter, TileEntityMachineFunnel, TileEntityStorageDrum,
 * TileEntityMachineTapeDrive, TileEntityMachineDrain, TileEntityRadiobox, TileEntityMachineTeleporter,
 * TileEntityForceField, TileEntityRefueler, TileEntityMachineSiren/ItemCassette, SuperComputerRecipes,
 * TileEntityMachineExcavator, TileEntityMachineMiningLaser). Machines without entities are built on pads near
 * x = 23000; entity cases use the spawn chunks at y = 230. Expected tick counts are recomputed from the 1.7.10
 * arithmetic in the comments. Not covered: GUIs, sounds, particles, rendering, the custom machine config system.
 */
final class ParityQ4Diagnostics {

    private ParityQ4Diagnostics() {
    }

    private static final int BASE_X = 23_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 23_000;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            properties(problems, cases);
            recipes(server, problems, cases);
            sirenTracks(problems, cases);
            powerDetector(level, problems, cases);
            geiger(level, problems, cases);
            funnel(level, problems, cases);
            storageDrum(level, problems, cases);
            tapeDrive(level, problems, cases);
            drain(level, problems, cases);
            refueler(level, problems, cases);
            siren(level, problems, cases);
            forcefield(level, problems, cases);
            excavator(level, problems, cases);
            miningLaser(level, problems, cases, 0, 23);
            miningLaser(level, problems, cases, 1, 12);
            radiobox(level, problems, cases);
            teleporter(level, problems, cases);
        } catch (RuntimeException | ReflectiveOperationException e) {
            AtomicLegacy.LOGGER.error("Parity Q4 diagnostics crashed", e);
            problems.add("Q4 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    /// ModBlocks: setHardness / setResistance (resistance written as the literal setResistance argument) ///
    private static void properties(List<String> problems, int[] cases) {
        Object[][] rows = {
                {HbmBlocks.MACHINE_EXCAVATOR.get(), 5F, 100F}, {HbmBlocks.MACHINE_MINING_LASER.get(), 5F, 100F},
                {HbmBlocks.MACHINE_FUNNEL.get(), 10F, 20F}, {HbmBlocks.MACHINE_DRAIN.get(), 5F, 10F},
                {HbmBlocks.MACHINE_DETECTOR.get(), 5F, 10F}, {HbmBlocks.MACHINE_SIREN.get(), 5F, 10F},
                {HbmBlocks.MACHINE_TAPE_DRIVE.get(), 5F, 10F}, {HbmBlocks.MACHINE_SUPERCOMPUTER.get(), 5F, 30F},
                {HbmBlocks.MACHINE_TELEPORTER.get(), 5F, 10F}, {HbmBlocks.BROADCASTER_PC.get(), 5F, 15F},
                {HbmBlocks.RADIOBOX.get(), 5F, 10F}, {HbmBlocks.RADIOREC.get(), 5F, 10F},
                {HbmBlocks.CARGO_ELEVATOR.get(), 5F, 10F}, {HbmBlocks.REFUELER.get(), 5F, 10F},
                {HbmBlocks.MACHINE_STORAGE_DRUM.get(), 5F, 10F}, {HbmBlocks.MACHINE_WASTE_DRUM.get(), 5F, 10F},
                {HbmBlocks.VENT_CHLORINE_SEAL.get(), 5F, 10F}, {HbmBlocks.MACHINE_FORCEFIELD.get(), 5F, 100F},
                {HbmBlocks.DECON.get(), 5F, 10F}, {HbmBlocks.GEIGER.get(), 15F, 0.25F},
                {HbmBlocks.CUSTOM_MACHINE.get(), 5F, 10F}, {HbmBlocks.SEAL_FRAME.get(), 10F, 100F},
                {HbmBlocks.SEAL_CONTROLLER.get(), 10F, 100F}, {HbmBlocks.LAMP_DEMON.get(), 3F, 3F},
        };
        for (Object[] row : rows) {
            Block block = (Block) row[0];
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(block.defaultDestroyTime() == (float) row[1], problems, cases, id + " hardness " + block.defaultDestroyTime());
            check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
        }
        check(HbmBlocks.SEAL_HATCH.get().defaultDestroyTime() == Float.POSITIVE_INFINITY, problems, cases, "seal_hatch is unbreakable (POSITIVE_INFINITY)"); // [parity U3: ModBlocks seal_hatch.setHardness(Float.POSITIVE_INFINITY)]
    }

    /// CraftingManager:237 (MINGRADE.wireFine), SuperComputerRecipes.registerTriplet time2 = 2 / 5 minutes ///
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        var manager = server.getRecipeManager();
        var detector = manager.byKey(AtomicLegacy.id("machine_detector"));
        check(detector.isPresent() && detector.get().value().getIngredients().stream()
                        .anyMatch(ing -> ing.test(stack("wire_red_copper", 1))) && detector.get().value().getIngredients().stream()
                        .noneMatch(ing -> ing.test(stack("wire_copper", 1))),
                problems, cases, "machine_detector uses red copper (Mingrade) fine wire");
        int[][] durations = {{2400}, {6000}, {36000}, {72000}};
        String[] names = {"processflight_helium", "processorbit_helium", "processflight_water", "processorbit_water"};
        for (int i = 0; i < names.length; i++) {
            ResourceLocation id = AtomicLegacy.id("supercomputer/" + names[i]);
            int expected = durations[i][0];
            var holder = manager.getAllRecipesFor(HbmRecipeTypes.SUPERCOMPUTER.get()).stream()
                    .filter(h -> h.id().equals(id)).findFirst();
            check(holder.isPresent() && holder.get().value().duration() == expected, problems, cases,
                    "supercomputer " + names[i] + " duration " + holder.map(h -> h.value().duration()).orElse(-1) + " expected " + expected);
        }
        // radiobox: ModDamageSource.enervation exists as its own absolute damage type
        check(server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getOptional(HbmDamageTypes.ENERVATION).map(t -> t.msgId().equals("enervation")).orElse(false),
                problems, cases, "enervation damage type registered");
    }

    /// ItemCassette.TrackType colours (decimal in the original) ///
    private static void sirenTracks(List<String> problems, int[] cases) {
        Object[][] rows = {{"HATCH", 3358839}, {"AUTOPILOT", 11908533}, {"AMS", 15055698}, {"BLAST_DOOR", 11665408},
                {"APC_LOOP", 3565216}, {"KLAXON", 8421504}, {"REGULAR", 6684672}, {"BANK", 3572962}, {"BEEP", 13882323},
                {"CONTAINER", 14727839}, {"SWEEP", 15592026}, {"STRIDER", 11250586}, {"AIR_RAID", 0xDF3795},
                {"APC_PASS", 3422163}, {"RAZORTRAIN", 7819501}};
        for (Object[] row : rows) {
            SirenTrackItem.Track track = SirenTrackItem.Track.valueOf((String) row[0]);
            checkEq(track.color(), (int) row[1], problems, cases, "cassette colour " + row[0]);
        }
    }

    /// TileEntityMachineDetector: buffer 5 HE, lit while power > 0, one HE per tick ///
    private static void powerDetector(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_DETECTOR.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof PowerDetectorBlockEntity detector)) {
            check(false, problems, cases, "machine_detector block entity");
            return;
        }
        detector.applyPower(0L, 5L);
        int litTicks = 0;
        for (int t = 0; t < 8; t++) {
            PowerDetectorBlockEntity.tick(level, pos, level.getBlockState(pos), detector);
            if (level.getBlockState(pos).getValue(PowerDetectorBlock.LIT)) litTicks++;
        }
        checkEq(litTicks, 5, problems, cases, "detector lit ticks from 5 HE");
        checkEq(level.getBlockState(pos).getSignal(level, pos, Direction.NORTH), 0, problems, cases, "detector signal after drain");
    }

    /// GeigerCounter.getComparatorInputOverride: min(ceil(check() / 5), 15) on the live chunk radiation ///
    private static void geiger(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.GEIGER.get().defaultBlockState(), 3);
        ChunkRadiationData data = ChunkRadiationData.get(level);
        data.setRadiation(pos, 23.0D);
        double rad = HbmRadiationWorlds.getRadiation(level, pos);
        int expected = Math.min((int) Math.ceil((float) rad / 5.0F), 15);
        checkEq(level.getBlockState(pos).getAnalogOutputSignal(level, pos), expected, problems, cases, "geiger comparator without a tick, rad " + rad);
        data.setRadiation(pos, 0.0D);
    }

    /// TileEntityMachineFunnel: 3x3 before 2x2, mode-independent slot validity ///
    private static void funnel(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_FUNNEL.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof FunnelBlockEntity funnel)) {
            check(false, problems, cases, "machine_funnel block entity");
            return;
        }
        funnel.setItem(0, new ItemStack(Items.IRON_INGOT, 9));
        FunnelBlockEntity.tick(level, pos, level.getBlockState(pos), funnel);
        check(funnel.getItem(9).is(Items.IRON_BLOCK) && funnel.getItem(0).isEmpty(), problems, cases,
                "funnel 9 iron -> block: " + funnel.getItem(9) + " / " + funnel.getItem(0));
        funnel.cycleMode(); // MODE_3x3
        check(funnel.canPlaceItem(1, new ItemStack(Items.CLAY_BALL)), problems, cases,
                "funnel accepts a 2x2-only item in 3x3 mode (isItemValidForSlot ignores the mode)");
        funnel.setItem(1, new ItemStack(Items.CLAY_BALL, 4));
        FunnelBlockEntity.tick(level, pos, level.getBlockState(pos), funnel);
        checkEq(funnel.getItem(1).getCount(), 4, problems, cases, "funnel 3x3 mode leaves 4 clay");
        funnel.clearContent();
    }

    /// TileEntityStorageDrum.isItemValidForSlot / canExtractItem (ModItems.ingot_mercury is "nugget_mercury") ///
    private static void storageDrum(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_STORAGE_DRUM.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof StorageDrumBlockEntity drum)) {
            check(false, problems, cases, "machine_storage_drum block entity");
            return;
        }
        check(drum.canPlaceItem(0, stack("ingot_au198", 1)), problems, cases, "drum accepts ingot_au198");
        check(!drum.canPlaceItem(0, stack("nugget_au198", 1)), problems, cases, "drum rejects nugget_au198");
        check(drum.canTakeItemThroughFace(0, stack("nugget_mercury", 1), Direction.DOWN), problems, cases, "drum extracts nugget_mercury");
        check(!drum.canTakeItemThroughFace(0, stack("bottle_mercury", 1), Direction.DOWN), problems, cases, "drum keeps bottle_mercury");
        check(!drum.canTakeItemThroughFace(0, stack("nugget_mercury_tiny", 1), Direction.DOWN), problems, cases, "drum keeps nugget_mercury_tiny");
    }

    /// TileEntityMachineTapeDrive: all 12 slots exposed, no insertion, empty drives stay ///
    private static void tapeDrive(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_TAPE_DRIVE.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof TapeDriveBlockEntity drive)) {
            check(false, problems, cases, "machine_tape_drive block entity");
            return;
        }
        checkEq(drive.getSlotsForFace(Direction.UP).length, 12, problems, cases, "tape drive automation slots");
        // [b3] TileEntityMachineBase.canInsertItem = isItemValidForSlot; the tape drive overrides it to item == ModItems.drive
        check(drive.canPlaceItemThroughFace(0, stack("drive_flash_empty", 1), Direction.UP), problems, cases, "tape drive accepts drives");
        check(!drive.canPlaceItemThroughFace(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK), Direction.UP), problems, cases, "tape drive rejects non-drives");
        check(!drive.canTakeItemThroughFace(0, stack("drive_flash_empty", 1), Direction.DOWN), problems, cases, "tape drive keeps empty flash");
        check(drive.canTakeItemThroughFace(0, stack("drive_disk_broken", 1), Direction.DOWN), problems, cases, "tape drive extracts broken disk");
    }

    /// TileEntityMachineDrain: NONE tank takes nothing; spills max(fill / 2, 1) per tick ///
    private static void drain(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_DRAIN.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof DrainBlockEntity drain)) {
            check(false, problems, cases, "machine_drain block entity");
            return;
        }
        var water = HbmFluids.byName("water").orElseThrow();
        IFluidHandler handler = drain.fluidHandler(null);
        checkEq(handler.fill(HbmFluids.toNeoStack(water, 1000), IFluidHandler.FluidAction.EXECUTE), 0, problems, cases, "drain NONE tank fill");
        drain.setType(water);
        checkEq(handler.fill(HbmFluids.toNeoStack(water, 1000), IFluidHandler.FluidAction.EXECUTE), 1000, problems, cases, "drain water fill");
        DrainBlockEntity.tick(level, pos, level.getBlockState(pos), drain);
        checkEq(drain.tank().amount(), 500, problems, cases, "drain spill half");
        DrainBlockEntity.tick(level, pos, level.getBlockState(pos), drain);
        checkEq(drain.tank().amount(), 250, problems, cases, "drain spill half again");
    }

    /// TileEntityRefueler.getPorts: the fluid port is at opposite(meta), which is FACING in the port ///
    private static void refueler(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.REFUELER.get().defaultBlockState().setValue(RefuelerBlock.FACING, Direction.SOUTH), 3);
        if (!(level.getBlockEntity(pos) instanceof RefuelerBlockEntity refueler)) {
            check(false, problems, cases, "refueler block entity");
            return;
        }
        check(refueler.fluidHandler(Direction.SOUTH) != null && refueler.fluidHandler(Direction.NORTH) == null, problems, cases,
                "refueler port on the wall side (south for meta 2)");
        checkEq(refueler.tank().capacity(), 100, problems, cases, "refueler tank");
    }

    /// TileEntityMachineSiren: isItemValidForSlot false, NULL track tick is harmless ///
    private static void siren(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_SIREN.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof SirenBlockEntity siren)) {
            check(false, problems, cases, "machine_siren block entity");
            return;
        }
        check(!siren.canPlaceItemThroughFace(0, stack("siren_track", 1), Direction.UP), problems, cases, "siren no automation insertion");
        SirenBlockEntity.tick(level, pos, level.getBlockState(pos), siren);
        check(true, problems, cases, "siren NULL track tick");
    }

    /// TileEntityForceField.damage: 100 / 250 == 0, so even a 0 impact blinks for 5 ticks ///
    private static void forcefield(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_FORCEFIELD.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity field)) {
            check(false, problems, cases, "machine_forcefield block entity");
            return;
        }
        Method damage = LegacyMachineBlockEntity.class.getDeclaredMethod("damageForcefield", net.minecraft.world.entity.Entity.class, int.class);
        damage.setAccessible(true);
        damage.invoke(field, null, 0);
        checkEq(intField(field, "forcefieldBlink"), 5, problems, cases, "forcefield blink on a zero impact");
        checkEq(intField(field, "forcefieldHealth"), 100, problems, cases, "forcefield health after zero impact");
        damage.invoke(field, null, 100);
        // cooldown = (int) (100 + radius * cooldownModif) = 116 for the base radius 16
        checkEq(intField(field, "forcefieldCooldown"), 116, problems, cases, "forcefield cooldown at 0 health");
    }

    /// TileEntityMachineExcavator: 3x3 stone at y - 4, steel drill (speed 1): ceil(9 * 1.5 / 1) = 14 ticks, depth on tick 15 ///
    private static void excavator(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = spawnPad(level, 12); // item entities need entity-ticking (spawn) chunks
        level.setBlock(pos, HbmBlocks.MACHINE_EXCAVATOR.get().defaultBlockState(), 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof ExcavatorBlockEntity excavator)) {
            check(false, problems, cases, "machine_excavator block entity");
            return;
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            level.setBlock(pos.offset(dx, -4, dz), Blocks.STONE.defaultBlockState(), 2 | 16);
        }
        excavator.setItem(ExcavatorBlockEntity.DRILLBIT_SLOT, stack("drillbit_steel", 1));
        excavator.applyPower(0L, 1_000_000L);
        excavator.toggle(0);
        for (int t = 0; t < 13; t++) ExcavatorBlockEntity.tick(level, pos, level.getBlockState(pos), excavator);
        check(level.getBlockState(pos.below(4)).is(Blocks.STONE), problems, cases, "excavator layer intact after 13 ticks");
        ExcavatorBlockEntity.tick(level, pos, level.getBlockState(pos), excavator);
        int cobble = 0;
        for (int slot = ExcavatorBlockEntity.OUTPUT_START; slot < ExcavatorBlockEntity.OUTPUT_START + 9; slot++) {
            if (excavator.getItem(slot).is(Items.COBBLESTONE)) cobble += excavator.getItem(slot).getCount();
        }
        checkEq(cobble, 9, problems, cases, "excavator collected cobblestone after 14 ticks");
        check(level.getBlockState(pos.below(4)).isAir(), problems, cases, "excavator layer broken after 14 ticks");
        checkEq(intField(excavator, "targetDepth"), 0, problems, cases, "excavator depth after 14 ticks");
        ExcavatorBlockEntity.tick(level, pos, level.getBlockState(pos), excavator);
        checkEq(intField(excavator, "targetDepth"), 1, problems, cases, "excavator depth after 15 ticks");
        checkEq(excavator.getItem(ExcavatorBlockEntity.DRILLBIT_SLOT).getCount(), 1, problems, cases, "excavator drill kept");
        excavator.clearContent();
        level.removeBlock(pos, false);
    }

    /// TileEntityMachineMiningLaser: stone 1.5 * 15 / 1 -> 1 / 22.5 per cycle = 23 cycles; OVERDRIVE 1 = 2 cycles per tick ///
    private static void miningLaser(ServerLevel level, List<String> problems, int[] cases, int overdrive, int ticks) {
        BlockPos pos = spawnPad(level, 18 + overdrive * 6); // item entities need entity-ticking (spawn) chunks
        level.setBlock(pos, HbmBlocks.MACHINE_MINING_LASER.get().defaultBlockState(), 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof MiningLaserBlockEntity laser)) {
            check(false, problems, cases, "machine_mining_laser block entity");
            return;
        }
        level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), 2 | 16);
        if (overdrive > 0) laser.setItem(MiningLaserBlockEntity.UPGRADE_START, stack("upgrade_overdrive_" + overdrive, 1));
        laser.applyPower(0L, 100_000_000L);
        check(!laser.enabled(), problems, cases, "mining laser starts off (isOn = false)");
        laser.toggleEnabled();
        for (int t = 0; t < ticks - 1; t++) MiningLaserBlockEntity.tick(level, pos, level.getBlockState(pos), laser);
        check(level.getBlockState(pos.below(2)).is(Blocks.STONE), problems, cases, "mining laser (overdrive " + overdrive + ") stone intact after " + (ticks - 1) + " ticks");
        MiningLaserBlockEntity.tick(level, pos, level.getBlockState(pos), laser);
        check(!level.getBlockState(pos.below(2)).is(Blocks.STONE), problems, cases, "mining laser (overdrive " + overdrive + ") stone broken after " + ticks + " ticks");
        check(laser.getItem(MiningLaserBlockEntity.OUTPUT_START).is(Items.COBBLESTONE), problems, cases,
                "mining laser sucked the drop: " + laser.getItem(MiningLaserBlockEntity.OUTPUT_START));
        laser.clearContent();
        level.removeBlock(pos, false);
    }

    /// TileEntityRadiobox: IMob in x-15..x+15, 25 000 HE per tick unless infinite, enervation 20 ///
    private static void radiobox(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = spawnPad(level, 0);
        level.setBlock(pos, HbmBlocks.RADIOBOX.get().defaultBlockState().setValue(RadioboxBlock.ACTIVE, true), 3);
        if (!(level.getBlockEntity(pos) instanceof RadioboxBlockEntity box)) {
            check(false, problems, cases, "radiobox block entity");
            return;
        }
        Zombie near = EntityType.ZOMBIE.create(level);
        Zombie far = EntityType.ZOMBIE.create(level);
        if (near == null || far == null) {
            check(false, problems, cases, "radiobox zombies");
            return;
        }
        try {
            near.setPos(pos.getX() + 14.5D, pos.getY(), pos.getZ() + 0.5D);
            far.setPos(pos.getX() + 15.5D, pos.getY(), pos.getZ() + 0.5D);
            near.setNoAi(true);
            far.setNoAi(true);
            level.addFreshEntity(near);
            level.addFreshEntity(far);
            box.enableInfinite();
            RadioboxBlockEntity.tick(level, pos, level.getBlockState(pos), box);
            check(near.getHealth() <= 0.0F || near.isDeadOrDying(), problems, cases, "radiobox kills a zombie 14 blocks away: " + near.getHealth());
            check(near.getLastDamageSource() != null && near.getLastDamageSource().is(HbmDamageTypes.ENERVATION), problems, cases,
                    "radiobox damage type enervation");
            check(far.getHealth() == far.getMaxHealth(), problems, cases, "radiobox misses x + 15.5: " + far.getHealth());
        } finally {
            near.discard();
            far.discard();
            level.removeBlock(pos, false);
        }
    }

    /// TileEntityMachineTeleporter: 1 000 000 HE per jump, feet at targetY + 1.5 ///
    private static void teleporter(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = spawnPad(level, 6);
        BlockState state = HbmBlocks.MACHINE_TELEPORTER.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        if (!(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity teleporter)) {
            check(false, problems, cases, "machine_teleporter block entity");
            return;
        }
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        if (stand == null) {
            check(false, problems, cases, "teleporter armor stand");
            return;
        }
        try {
            BlockPos target = pos.offset(4, 2, 0);
            teleporter.setTeleporterTarget(target, level.dimension());
            teleporter.applyPower(0L, 1_500_000L);
            stand.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            level.addFreshEntity(stand);
            LegacyMachineBlockEntity.tick(level, pos, level.getBlockState(pos), teleporter);
            check(Math.abs(stand.getY() - (target.getY() + 1.5D)) < 1.0E-6D && Math.abs(stand.getX() - (target.getX() + 0.5D)) < 1.0E-6D,
                    problems, cases, "teleporter destination " + stand.position() + " expected y " + (target.getY() + 1.5D));
            checkEq(teleporter.energyStored(), 500_000L, problems, cases, "teleporter energy after a jump");
        } finally {
            stand.discard();
            level.removeBlock(pos, false);
        }
    }

    private static int intField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(owner);
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("Q4: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static ItemStack stack(String path, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(path)), count);
    }

    private static BlockPos pad(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 8) * 32, BASE_Y, BASE_Z + (index / 8) * 32);
        for (int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 8) >> 4; cx++) {
            for (int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -4; dx <= 4; dx++) for (int dy = -8; dy <= 6; dy++) for (int dz = -4; dz <= 4; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy < -6 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static BlockPos spawnPad(ServerLevel level, int offsetZ) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = new BlockPos(spawn.getX() + 2, 230, spawn.getZ() + offsetZ);
        for (int dx = -2; dx <= 18; dx++) for (int dy = -6; dy <= 4; dy++) for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }
}
