package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.IcfControllerBlock;
import com.atomiclegacy.blocks.IcfLaserComponentBlock;
import com.atomiclegacy.blocks.LegacyVariantBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmThermalConversions;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.tileentity.GeothermalHeatExchangerBlockEntity;
import com.atomiclegacy.tileentity.HeatBoilerBlockEntity;
import com.atomiclegacy.tileentity.HeaterBlockEntity;
import com.atomiclegacy.tileentity.IcfControllerBlockEntity;
import com.atomiclegacy.tileentity.IcfCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [parity A3: diagnostics] Heat machines and the ICF laser: the machine_boiler overpressure explosion (ExplosionVNT with
 * range mod 3, no block damage), the uncapped electric heater, the Hephaestus fluid identifier (any type, NONE without a
 * heat-exchanger step, output voided on change), the heat-exchanger / boiler step tables, ICF laser assembly (controller
 * pre-seeded in the flood fill, counts, max power), the beam bounding box in a negative direction and the additive ICF
 * laser budget. Site x ~ 43300, z ~ 4300.
 */
final class ParityA3Diagnostics {
    private static final BlockPos SITE = new BlockPos(43_300, 120, 4_300);

    private ParityA3Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            tables(problems, cases);
            boiler(level, problems, cases);
            electricHeater(level, problems, cases);
            hephaestus(level, problems, cases);
            icf(level, problems, cases);
        } catch (Exception e) {
            problems.add("A3 heat/ICF threw " + e);
            AtomicLegacy.LOGGER.error("A3 diagnostics", e);
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("A3 " + what);
    }

    private static void eq(Object actual, Object expected, List<String> problems, int[] cases, String what) {
        check(String.valueOf(actual).equals(String.valueOf(expected)), problems, cases, what + ": " + actual + " != " + expected);
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow();
    }

    private static void clear(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /** Fluids.java FT_Heatable / FT_Coolable entries used by boilers, heat exchangers and the Hephaestus. */
    private static void tables(List<String> problems, int[] cases) {
        var water = HbmThermalConversions.firstBoilerStep(fluid("water")).orElseThrow();
        eq(water.heatReq() + "/" + water.amountReq() + "/" + water.amountProduced() + "/" + water.output().name(),
                "200/1/100/steam", problems, cases, "water boiler step");
        var steam = HbmThermalConversions.heatExchangerCoolingStep(fluid("steam")).orElseThrow();
        eq(steam.amountReq() + "/" + steam.amountProduced() + "/" + steam.heatEnergy() + "/" + steam.turbineEfficiency(),
                "100/1/200/0.5", problems, cases, "steam heat exchanger step");
        var lead = HbmThermalConversions.heatExchangerCoolingStep(fluid("lead_hot")).orElseThrow();
        eq(lead.heatEnergy(), 680, problems, cases, "hot lead cooling heat");
        check(HbmThermalConversions.firstGeothermalStep(fluid("heavywater")).isEmpty(), problems, cases,
                "heavy water has no HEATEXCHANGER heating step");
        check(HbmThermalConversions.firstBoilerStep(fluid("coolant")).isEmpty(), problems, cases, "coolant is not boilable");
    }

    private static void boiler(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = SITE;
        clear(level, pos.offset(-14, 0, -3), pos.offset(14, 4, 3));
        level.setBlock(pos.offset(-1, 2, 0), Blocks.STONE.defaultBlockState(), 2 | 16);
        level.setBlock(pos, HbmBlocks.MACHINE_BOILER.get().defaultBlockState(), 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof HeatBoilerBlockEntity boiler)) {
            problems.add("A3 machine_boiler has no block entity");
            return;
        }
        eq(boiler.outputTank().capacity(), 1_600_000, problems, cases, "boiler steam tank (16000 * 100 / 1)");
        boiler.outputTank().fill(fluid("steam"), boiler.outputTank().capacity(), false);
        // same chunk as the boiler (x 43296..43311): 10.9 blocks away, beyond the vanilla radius of 10
        Pig pig = EntityType.PIG.create(level);
        pig.moveTo(pos.getX() + 11.4D, pos.getY() + 2.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        float before = pig.getHealth();
        HeatBoilerBlockEntity.tick(level, pos, level.getBlockState(pos), boiler);
        check(boiler.exploded(), problems, cases, "full steam tank makes the boiler explode");
        net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D);
        check(pig.getHealth() < before || !pig.isAlive(), problems, cases,
                "boiler explosion reaches 10.9 blocks (ExplosionVNT 5F withRangeMod(3F)): health " + pig.getHealth()
                        + " seen " + net.minecraft.world.level.Explosion.getSeenPercent(center, pig)
                        + " found " + level.getEntities(null, pig.getBoundingBox().inflate(1.0D)).contains(pig)
                        + " pos " + pig.position());
        check(level.getBlockState(pos.offset(-1, 2, 0)).is(Blocks.STONE), problems, cases, "boiler explosion destroys no blocks");
        pig.discard();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        level.setBlock(pos.offset(-1, 2, 0), Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    private static void electricHeater(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = SITE.offset(0, 0, 40);
        clear(level, pos.offset(-2, -1, -2), pos.offset(2, 1, 2));
        level.setBlock(pos, HbmBlocks.HEATER_ELECTRIC.get().defaultBlockState(), 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof HeaterBlockEntity heater)) {
            problems.add("A3 heater_electric has no block entity");
            return;
        }
        CompoundTag tag = heater.saveWithoutMetadata(level.registryAccess());
        tag.putInt("Setting", 10);
        tag.putInt("HeatEnergy", 100_000);
        heater.loadWithComponents(tag, level.registryAccess());
        heater.applyPower(0L, 1_000_000L);
        eq(heater.consumption(), (long) (Math.pow(10, 1.4D) * 200D), problems, cases, "electric heater consumption at setting 10");
        HeaterBlockEntity.tick(level, pos, level.getBlockState(pos), heater);
        eq(heater.getHeatStored(), 99_900 + 1_000, problems, cases, "electric heater heat is not capped (x0.999 + 10 * 100)");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    private static void hephaestus(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = SITE.offset(0, 0, 80);
        clear(level, pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
        level.setBlock(pos, HbmBlocks.MACHINE_HEPHAESTUS.get().defaultBlockState(), 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof GeothermalHeatExchangerBlockEntity heph)) {
            problems.add("A3 machine_hephaestus has no block entity");
            return;
        }
        heph.applyFluidIdentifier(fluid("water"));
        eq(heph.inputTank().type().name() + ">" + heph.outputTank().type().name(), "water>steam", problems, cases,
                "Hephaestus accepts water (HEATEXCHANGER 0.25)");
        heph.outputTank().fill(fluid("steam"), 500, false);
        heph.applyFluidIdentifier(fluid("oil"));
        eq(heph.outputTank().type().name() + ":" + heph.outputTank().amount(), "hotoil:0", problems, cases,
                "Hephaestus output is retyped and voided (setTankType)");
        heph.applyFluidIdentifier(fluid("steam"));
        eq(heph.inputTank().type().name() + ">" + heph.outputTank().type().name(), "none>none", problems, cases,
                "Hephaestus resets to NONE for a fluid without heating step");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    private static BlockState part(IcfLaserComponentBlock.Part part) {
        return HbmBlocks.ICF_LASER_COMPONENT.get().defaultBlockState().setValue(LegacyVariantBlock.VARIANT, part.ordinal());
    }

    private static void icf(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos c = SITE.offset(0, 0, 120);
        clear(level, c.offset(-2, -2, -7), c.offset(6, 2, 4));
        level.setBlock(c, HbmBlocks.ICF_CONTROLLER.get().defaultBlockState().setValue(IcfControllerBlock.FACING, Direction.NORTH), 2 | 16);
        BlockPos cell1 = c.south(), cell2 = c.south(2);
        BlockPos emitter = cell1.east(), capacitor = cell1.east(2), turbo = cell1.east(3);
        level.setBlock(cell1, part(IcfLaserComponentBlock.Part.CELL), 2 | 16);
        level.setBlock(cell2, part(IcfLaserComponentBlock.Part.CELL), 2 | 16);
        level.setBlock(emitter, part(IcfLaserComponentBlock.Part.EMITTER), 2 | 16);
        level.setBlock(capacitor, part(IcfLaserComponentBlock.Part.CAPACITOR), 2 | 16);
        level.setBlock(turbo, part(IcfLaserComponentBlock.Part.TURBO), 2 | 16);
        Set<BlockPos> core = Set.of(cell1, cell2, emitter, capacitor, turbo);
        Set<BlockPos> casing = new HashSet<>();
        for (BlockPos p : core) {
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!core.contains(n) && !n.equals(c)) casing.add(n.immutable());
            }
        }
        BlockPos port = c.south(3);
        for (BlockPos p : casing) {
            level.setBlock(p, part(p.equals(port) ? IcfLaserComponentBlock.Part.PORT : IcfLaserComponentBlock.Part.CASING), 2 | 16);
        }
        if (!(level.getBlockEntity(c) instanceof IcfControllerBlockEntity controller)) {
            problems.add("A3 icf_controller has no block entity");
            return;
        }
        controller.assemble(FakePlayerFactory.getMinecraft(level));
        check(controller.assembled(), problems, cases, "ICF laser with a cell next to the controller assembles");
        eq(controller.capacitorCount() + "/" + controller.turbochargerCount(), "1/1", problems, cases, "ICF capacitor/turbo count");
        eq(controller.maxPower(), 7_500_000L, problems, cases, "ICF max power sqrt(1)*2.5M + sqrt(1)*5M");
        check(level.getBlockState(cell1).is(HbmBlocks.ICF_BLOCK.get())
                && level.getBlockState(port).is(HbmBlocks.ICF_BLOCK.get())
                && level.getBlockState(port).getValue(LegacyVariantBlock.VARIANT) == 1, problems, cases,
                "ICF parts become icf_block (port meta 1)");

        // breaking an assembled part restores the raw part without an orphaned block entity and unassembles the controller
        BlockPos broken = cell2.below();
        level.setBlock(broken, Blocks.AIR.defaultBlockState(), 3);
        check(level.getBlockState(broken).is(HbmBlocks.ICF_LASER_COMPONENT.get()) && level.getBlockEntity(broken) == null
                && !controller.assembled(), problems, cases, "breaking icf_block restores the casing and unassembles the laser");
        controller.setAssembled(true);

        // Beam towards -Z: TileEntityICFController AABB is min/max of the coordinates, then +0.2 / +0.8.
        BlockPos stop = c.north(5);
        level.setBlock(stop, Blocks.STONE.defaultBlockState(), 2 | 16);
        ItemEntity item = new ItemEntity(level, c.getX() + 0.5D, c.getY() + 0.4D, c.getZ() - 4.5D, new ItemStack(Items.STICK));
        item.setNoGravity(true);
        level.addFreshEntity(item);
        controller.applyPower(0L, 1_000_000L);
        IcfControllerBlockEntity.tick(level, c, level.getBlockState(c), controller);
        eq(controller.laserLength(), 5, problems, cases, "ICF beam stops at the first solid block");
        check(level.getBlockState(stop).isAir(), problems, cases, "ICF beam breaks a block below 6000 resistance");
        check(!item.isAlive(), problems, cases, "ICF beam AABB reaches z - 4.8 in the negative direction");
        item.discard();

        BlockPos icfPos = SITE.offset(0, 0, 160);
        clear(level, icfPos, icfPos);
        level.setBlock(icfPos, HbmBlocks.ICF.get().defaultBlockState(), 2 | 16);
        if (level.getBlockEntity(icfPos) instanceof IcfCoreBlockEntity icf) {
            icf.receiveLaser(3L, 10L);
            icf.receiveLaser(4L, 10L);
            eq(icf.laser() + "/" + icf.maxLaser(), "7/20", problems, cases, "ICF laser and maxLaser are summed over controllers");
        } else {
            problems.add("A3 icf has no block entity");
        }
        level.setBlock(icfPos, Blocks.AIR.defaultBlockState(), 2 | 16);
        // removing an assembled icf_block restores the raw part, so clear twice
        clear(level, c.offset(-2, -2, -7), c.offset(6, 2, 4));
        clear(level, c.offset(-2, -2, -7), c.offset(6, 2, 4));
    }
}
