package com.atomiclegacy.blocks.rail;

import com.atomiclegacy.train.LegacyVec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] RailStandardBuffer. The Z branch compares the buffer clamp against {@code vec.xCoord}
 * exactly like the original, so a north/south buffer always reports a collision.
 */
public class RailStandardBufferBlock extends RailDummyableBlock {

    public RailStandardBufferBlock(Properties properties) {
        super(properties);
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
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext(), new MoveContext(RailCheckType.OTHER, 0));
    }

    @Override
    public LegacyVec3 getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info, context);
    }

    /* Very simple function determining the snapping position and adding the motion value to it, if desired. */
    public LegacyVec3 snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        int[] pos = this.findCore(world, x, y, z);
        if (pos == null) return LegacyVec3.of(trainX, trainY, trainZ);
        int cX = pos[0];
        int cZ = pos[2];
        int meta = meta(world, cX, pos[1], cZ) - offset;
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
            vec.xCoord = Mth.clamp(targetX, cX - 2, cX + 3);
            vec.yCoord = y + 0.1875;
            vec.zCoord = cZ + 0.5 + dz(rot) * 0.5;

            double nX = (dir == Direction.EAST ? -1 - context.collisionBogieDistance : 2);
            double pX = (dir == Direction.WEST ? 0 - context.collisionBogieDistance : 3);
            double buffer = Mth.clamp(targetX, cX - nX, cX + pX);

            if (buffer != vec.xCoord) {
                context.collision = true;
                context.overshoot = Math.abs(buffer - vec.xCoord);
                vec.xCoord = buffer;
                return vec;
            }

            info.dist(Math.abs(targetX - vec.xCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX + (motionX * speed > 0 ? 3 : -3), y, cZ));
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
            vec.zCoord = Mth.clamp(targetZ, cZ - 2, cZ + 3);

            double nZ = (dir == Direction.SOUTH ? -1 - context.collisionBogieDistance : 2);
            double pZ = (dir == Direction.NORTH ? 0 - context.collisionBogieDistance : 3);
            double buffer = Mth.clamp(targetZ, cZ - nZ, cZ + pZ);

            if (buffer != vec.xCoord) {
                context.collision = true;
                context.overshoot = Math.abs(buffer - vec.zCoord);
                vec.zCoord = buffer;
                return vec;
            }

            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, y, cZ + (motionZ * speed > 0 ? 3 : -3)));
        }
        return vec;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
