package com.atomiclegacy.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity V1: fluid net] TilePort.PortDef.make and TilePortShapes (HbmMods 936c60c) as
 * {@link HbmFluidNetUser.FluidPort} lists: one port per (position, connection direction), all ports of a
 * machine combined into one node per fluid type (setupFluidPorts uses PortDef.combine).
 * {@code dir} is the 1.7.10 core orientation (meta - offset) = the core "facing" state,
 * {@code rot = dir.getRotation(UP)} = {@link Direction#getClockWise()}.
 */
public final class HbmPortDefs {
    public static final Direction POS_X = Direction.EAST;
    public static final Direction NEG_X = Direction.WEST;
    public static final Direction POS_Y = Direction.UP;
    public static final Direction NEG_Y = Direction.DOWN;
    public static final Direction POS_Z = Direction.SOUTH;
    public static final Direction NEG_Z = Direction.NORTH;

    private final List<HbmFluidNetUser.FluidPort> ports = new ArrayList<>();

    private HbmPortDefs() {
    }

    public static HbmPortDefs create() {
        return new HbmPortDefs();
    }

    /** PortDef.make(x, y, z, dirs...) */
    public HbmPortDefs make(int x, int y, int z, Direction... dirs) {
        BlockPos pos = new BlockPos(x, y, z);
        for (Direction dir : dirs) ports.add(new HbmFluidNetUser.FluidPort(pos, dir));
        return this;
    }

    public HbmPortDefs make(BlockPos pos, Direction... dirs) {
        return make(pos.getX(), pos.getY(), pos.getZ(), dirs);
    }

    public List<HbmFluidNetUser.FluidPort> build() {
        return List.copyOf(ports);
    }

    /** The original core orientation (ForgeDirection.getOrientation(meta - 10)). */
    public static Direction dir(BlockEntity core) {
        return com.atomiclegacy.rtty.RttyProxyLayouts.facing(core.getBlockState());
    }

    /** dir.getRotation(ForgeDirection.UP) */
    public static Direction rot(Direction dir) {
        return dir.getClockWise();
    }

    /** dir.getRotation(ForgeDirection.DOWN) */
    public static Direction rotDown(Direction dir) {
        return dir.getCounterClockWise();
    }

    public static List<HbmFluidNetUser.FluidPort> around(BlockPos p) {
        return create().make(p, Direction.values()).build();
    }

    public static List<HbmFluidNetUser.FluidPort> mixer(BlockPos p) {
        return create().make(p, POS_X, NEG_X, POS_Z, NEG_Z, NEG_Y).make(p.getX(), p.getY() + 2, p.getZ(), POS_Y).build();
    }

    public static List<HbmFluidNetUser.FluidPort> horizontal(BlockPos p) {
        return create().make(p, POS_X, NEG_X, POS_Z, NEG_Z, NEG_Y).build();
    }

    public static List<HbmFluidNetUser.FluidPort> assembler(BlockPos p) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        return create()
                .make(x - 1, y, z - 1, NEG_X, NEG_Z)
                .make(x, y, z - 1, NEG_Z)
                .make(x + 1, y, z - 1, POS_X, NEG_Z)
                .make(x + 1, y, z, POS_X)
                .make(x + 1, y, z + 1, POS_X, POS_Z)
                .make(x, y, z + 1, POS_Z)
                .make(x - 1, y, z + 1, NEG_X, POS_Z)
                .make(x - 1, y, z, NEG_X)
                .build();
    }

    public static List<HbmFluidNetUser.FluidPort> refinery(BlockPos p) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        return create()
                .make(x - 1, y, z - 1, NEG_X, NEG_Z)
                .make(x + 1, y, z - 1, POS_X, NEG_Z)
                .make(x + 1, y, z + 1, POS_X, POS_Z)
                .make(x - 1, y, z + 1, NEG_X, POS_Z)
                .build();
    }

    public static List<HbmFluidNetUser.FluidPort> flare(BlockPos p) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        return create()
                .make(x, y, z - 1, NEG_Z)
                .make(x + 1, y, z, POS_X)
                .make(x, y, z + 1, POS_Z)
                .make(x - 1, y, z, NEG_X)
                .build();
    }

    public static List<HbmFluidNetUser.FluidPort> solderer(BlockPos p, Direction dir) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        Direction rot = rot(dir);
        return create()
                .make(x, y, z, dir, rot.getOpposite())
                .make(x + rot.getStepX(), y, z + rot.getStepZ(), dir, rot)
                .make(x - dir.getStepX(), y, z - dir.getStepZ(), dir.getOpposite(), rot.getOpposite())
                .make(x - dir.getStepX() + rot.getStepX(), y, z - dir.getStepZ() + rot.getStepZ(), dir.getOpposite(), rot)
                .build();
    }

    public static List<HbmFluidNetUser.FluidPort> liquefactor(BlockPos p) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        return create()
                .make(x, y + 3, z, POS_Y)
                .make(x, y, z, NEG_Y)
                .make(x + 1, y + 1, z, POS_X)
                .make(x - 1, y + 1, z, NEG_X)
                .make(x, y + 1, z + 1, POS_Z)
                .make(x, y + 1, z - 1, NEG_Z)
                .build();
    }
}
