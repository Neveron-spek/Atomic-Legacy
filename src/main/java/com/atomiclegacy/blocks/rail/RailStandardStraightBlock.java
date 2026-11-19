package com.atomiclegacy.blocks.rail;

import com.atomiclegacy.train.LegacyVec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] RailStandardStraight (5m, {@code shortRail == false}) and RailStandardStraightShort (1m).
 * Both share snapAndMove; only the dimensions, the placement offset and the travel clamp differ.
 */
public class RailStandardStraightBlock extends RailDummyableBlock {
    private final boolean shortRail;

    public RailStandardStraightBlock(Properties properties, boolean shortRail) {
        super(properties);
        this.shortRail = shortRail;
    }

    @Override
    public int[] getDimensions() {
        return shortRail ? new int[] {0, 0, 0, 0, 1, 0} : new int[] {0, 0, 2, 2, 1, 0};
    }

    @Override
    public int getOffset() {
        return shortRail ? 0 : 2;
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
        int low = shortRail ? 0 : -2;
        int high = shortRail ? 1 : 3;
        int exit = shortRail ? 1 : 3;

        LegacyVec3 vec = LegacyVec3.of(trainX, trainY, trainZ);

        if (dir == Direction.EAST || dir == Direction.WEST) {
            double targetX = trainX;
            if (motionX > 0) {
                targetX += speed;
                info.yaw(-90F);
            } else {
                targetX -= speed;
                info.yaw(90F);
            }
            vec.xCoord = Mth.clamp(targetX, cX + low, cX + high);
            vec.yCoord = y + 0.1875;
            vec.zCoord = cZ + 0.5 + dz(rot) * 0.5;
            info.dist(Math.abs(targetX - vec.xCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX + (motionX * speed > 0 ? exit : -exit), y, cZ));
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            vec.xCoord = cX + 0.5 + dx(rot) * 0.5;
            vec.yCoord = y + 0.1875;
            vec.zCoord = Mth.clamp(targetZ, cZ + low, cZ + high);
            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, y, cZ + (motionZ * speed > 0 ? exit : -exit)));
        }
        return vec;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
