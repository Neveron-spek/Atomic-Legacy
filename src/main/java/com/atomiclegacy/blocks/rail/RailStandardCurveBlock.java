package com.atomiclegacy.blocks.rail;

import com.atomiclegacy.train.LegacyVec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] RailStandardCurveBase (width 4, 5m), RailStandardCurveWide7 (width 6) and
 * RailStandardCurveWide9 (width 8) with their exact footprints.
 */
public class RailStandardCurveBlock extends RailDummyableBlock {
    /** {d, r} offsets of RailStandardCurveBase.checkRequirement; the fill uses the same order. */
    private static final int[][] BASE = {
            {1, 0, 0}, {0, 1, 1}, {1, 1, 1}, {1, 2, 1}, {2, 1, 0}, {2, 2, 0}, {3, 1, 0}, {3, 2, 0},
            {2, 3, 1}, {3, 3, 1}, {4, 3, 0}, {3, 4, 1}, {4, 4, 1},
    };
    /** RailStandardCurveWide7.fillSpace (the third value selects dir (0) or rot (1) metadata). */
    private static final int[][] WIDE7 = {
            {1, 0, 0}, {2, 0, 0}, {0, 1, 1}, {1, 1, 1}, {2, 1, 1}, {3, 1, 0}, {4, 1, 0}, {2, 2, 1},
            {3, 2, 0}, {4, 2, 0}, {5, 2, 0}, {3, 3, 1}, {4, 3, 0}, {5, 3, 0}, {4, 4, 1}, {5, 4, 0},
            {6, 4, 0}, {5, 5, 1}, {5, 6, 1}, {6, 5, 1}, {6, 6, 1},
    };
    /** RailStandardCurveWide9 dim table. */
    private static final int[][] WIDE9 = {
            {1, 0, 0}, {2, 0, 0}, {0, 1, 1}, {1, 1, 0}, {2, 1, 0}, {3, 1, 0}, {4, 1, 0}, {2, 2, 1},
            {3, 2, 1}, {4, 2, 1}, {5, 2, 0}, {4, 3, 1}, {5, 3, 1}, {5, 4, 1}, {6, 3, 0}, {6, 4, 0},
            {7, 4, 0}, {6, 5, 1}, {7, 5, 1}, {6, 6, 1}, {7, 6, 1}, {7, 7, 1}, {7, 8, 1}, {8, 6, 0},
            {8, 7, 0}, {8, 8, 0},
    };

    protected final int width;

    public RailStandardCurveBlock(Properties properties, int width) {
        super(properties);
        this.width = width;
    }

    public int width() {
        return width;
    }

    @Override
    public LegacyVec3 getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext());
    }

    @Override
    public LegacyVec3 getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info);
    }

    /* Very simple function determining the snapping position and adding the motion value to it, if desired. */
    public LegacyVec3 snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info) {
        int[] pos = this.findCore(world, x, y, z);
        if (pos == null) return LegacyVec3.of(trainX, trainY, trainZ);
        int cX = pos[0];
        int cZ = pos[2];
        int meta = meta(world, cX, pos[1], cZ) - offset;
        Direction dir = orientation(meta);
        Direction rot = dir == null || dir.getAxis() == Direction.Axis.Y ? null : rotationUp(dir);

        double turnRadius = width;
        double axisDist = width + 0.5D;

        LegacyVec3 vec = LegacyVec3.of(trainX, trainY, trainZ);

        double axisX = cX + 0.5 + dx(dir) * 0.5 + dx(rot) * axisDist;
        double axisZ = cZ + 0.5 + dz(dir) * 0.5 + dz(rot) * axisDist;

        LegacyVec3 dist = LegacyVec3.of(vec.xCoord - axisX, 0, vec.zCoord - axisZ);
        dist = dist.normalize();
        dist.xCoord *= turnRadius;
        dist.zCoord *= turnRadius;

        double moveAngle = Math.atan2(motionX, motionZ) * 180D / Math.PI + 90;

        if (speed == 0) {
            info.dist(0).pos(new BlockPos(x, y, z)).yaw((float) moveAngle);
            return LegacyVec3.of(axisX + dist.xCoord, y, axisZ + dist.zCoord);
        }

        double angleDeg = Math.atan2(dist.xCoord, dist.zCoord) * 180D / Math.PI + 90;
        if (dir == Direction.WEST) angleDeg -= 90;
        if (dir == Direction.EAST) angleDeg += 90;
        if (dir == Direction.SOUTH) angleDeg += 180;
        angleDeg = LegacyVec3.wrapAngleTo180(angleDeg);
        double length90Deg = turnRadius * Math.PI / 2D;
        double angularChange = speed / length90Deg * 90D;

        Direction moveDir;
        if (Math.abs(motionX) > Math.abs(motionZ)) {
            moveDir = motionX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            moveDir = motionZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        if (moveDir == dir || (rot != null && moveDir == rot.getOpposite())) {
            angularChange *= -1;
        }

        double effAngle = angleDeg + angularChange;
        moveAngle += angularChange;

        if (effAngle > 90) {
            double angleOvershoot = effAngle - 90D;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX - dx(dir) * width + dx(rot) * (width + 1), y, cZ - dz(dir) * width + dz(rot) * (width + 1))).yaw((float) moveAngle);
            return LegacyVec3.of(axisX - dx(dir) * turnRadius, y + 0.1875, axisZ - dz(dir) * turnRadius);
        }

        if (effAngle < 0) {
            double angleOvershoot = -effAngle;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(-lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX + dx(dir), y, cZ + dz(dir))).yaw((float) moveAngle);
            return LegacyVec3.of(axisX - dx(rot) * turnRadius, y + 0.1875, axisZ - dz(rot) * turnRadius);
        }

        double radianChange = angularChange * Math.PI / 180D;
        dist.rotateAroundY((float) radianChange);

        return LegacyVec3.of(axisX + dist.xCoord, y + 0.1875, axisZ + dist.zCoord);
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, width, 0, width, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    private int[][] table() {
        return width == 6 ? WIDE7 : width == 8 ? WIDE9 : BASE;
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, Direction dir, int o) {
        Direction rot = rotationUp(dir);
        dir = dir.getOpposite();
        int dX = dir.getStepX();
        int dZ = dir.getStepZ();
        int rX = rot.getStepX();
        int rZ = rot.getStepZ();
        // RailStandardCurveWide7 inherits the 13-block check of RailStandardCurveBase
        int[][] check = width == 8 ? WIDE9 : BASE;
        for (int[] array : check) {
            if (!replaceable(world, x + dX * array[0] + rX * array[1], y, z + dZ * array[0] + rZ * array[1])) return false;
        }
        return true;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, Direction dir, int o) {
        safeRem = true;
        try {
            Direction rot = rotationUp(dir);
            dir = dir.getOpposite();
            int dX = dir.getStepX();
            int dZ = dir.getStepZ();
            int rX = rot.getStepX();
            int rZ = rot.getStepZ();
            int d = dir.get3DDataValue();
            int r = rot.get3DDataValue();
            for (int[] array : table()) {
                setDummy(world, x + dX * array[0] + rX * array[1], y, z + dZ * array[0] + rZ * array[1], array[2] == 0 ? d : r);
            }
        } finally {
            safeRem = false;
        }
    }
}
