package com.atomiclegacy.blocks.rail;

import com.atomiclegacy.train.LegacyVec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** [parity S1: trains] RailStandardRamp: 5x2 footprint plus a second layer, rising one block over five. */
public class RailStandardRampBlock extends RailDummyableBlock {
    private static final int[] UPPER = {1, -1, 2, 2, 1, 0};

    public RailStandardRampBlock(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-2.5, 0.0, -1.5, -1.5, 0.1, 0.5));
        this.bounding.add(new AABB(-1.5, 0.0, -1.5, -0.5, 0.3, 0.5));
        this.bounding.add(new AABB(-0.5, 0.0, -1.5, 0.5, 0.5, 0.5));
        this.bounding.add(new AABB(0.5, 0.0, -1.5, 1.5, 0.7, 0.5));
        this.bounding.add(new AABB(1.5, 0.0, -1.5, 2.5, 0.9, 0.5));
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, 2, 2, 1, 0};
    }

    @Override
    public int getOffset() {
        return 2;
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
        int cY = pos[1];
        int cZ = pos[2];
        int meta = meta(world, cX, cY, cZ) - offset;
        Direction dir = orientation(meta);
        Direction rot = dir == null || dir.getAxis() == Direction.Axis.Y ? null : rotationUp(dir);

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
            double dist = (cX + 0.5 - targetX + 2.5) / 5;
            vec.xCoord = Mth.clamp(targetX, cX - 2, cX + 3);
            vec.yCoord = Mth.clamp(dir == Direction.EAST ? cY + dist : cY + 1 - dist, cY, cY + 1) + 0.1875;
            vec.zCoord = cZ + 0.5 + dz(rot) * 0.5;
            info.dist(Math.abs(targetX - vec.xCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX + (motionX * speed > 0 ? 3 : -3), cY + (motionX * speed > 0 ^ dir == Direction.EAST ? 1 : 0), cZ));
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            double dist = (cZ + 0.5 - targetZ + 2.5) / 5;
            vec.xCoord = cX + 0.5 + dx(rot) * 0.5;
            vec.yCoord = Mth.clamp(dir == Direction.SOUTH ? cY + dist : cY + 1 - dist, cY, cY + 1) + 0.1875;
            vec.zCoord = Mth.clamp(targetZ, cZ - 2, cZ + 3);
            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, cY + (motionZ * speed > 0 ^ dir == Direction.SOUTH ? 1 : 0), cZ + (motionZ * speed > 0 ? 3 : -3)));
        }
        return vec;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, Direction dir, int o) {
        return checkSpace(world, x + dir.getStepX() * o, y + dir.getStepY() * o, z + dir.getStepZ() * o, getDimensions(), x, y, z, dir)
                && checkSpace(world, x + dir.getStepX() * o, y + dir.getStepY() * o, z + dir.getStepZ() * o, UPPER, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, Direction dir, int o) {
        fillSpace(world, x + dir.getStepX() * o, y + dir.getStepY() * o, z + dir.getStepZ() * o, getDimensions(), this, dir);
        fillSpace(world, x + dir.getStepX() * o, y + dir.getStepY() * o, z + dir.getStepZ() * o, UPPER, this, dir);
    }
}
