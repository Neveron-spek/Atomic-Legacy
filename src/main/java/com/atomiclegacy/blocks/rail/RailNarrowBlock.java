package com.atomiclegacy.blocks.rail;

import com.atomiclegacy.train.LegacyVec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** [parity S1: trains] RailNarrowStraight ({@code curve == false}, 5x1) and RailNarrowCurve (5x5, radius 4.5). */
public class RailNarrowBlock extends RailDummyableBlock {
    private final boolean curve;

    public RailNarrowBlock(Properties properties, boolean curve) {
        super(properties);
        this.curve = curve;
    }

    @Override
    public int[] getDimensions() {
        return curve ? new int[] {0, 0, 4, 0, 4, 0} : new int[] {0, 0, 2, 2, 0, 0};
    }

    @Override
    public int getOffset() {
        return curve ? 0 : 2;
    }

    @Override
    public LegacyVec3 getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext());
    }

    @Override
    public LegacyVec3 getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info);
    }

    public LegacyVec3 snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info) {
        int[] pos = this.findCore(world, x, y, z);
        if (pos == null) return LegacyVec3.of(trainX, trainY, trainZ);
        int cX = pos[0];
        int cZ = pos[2];
        int meta = meta(world, cX, pos[1], cZ) - offset;
        Direction dir = orientation(meta);
        return curve ? curve(dir, x, y, z, cX, cZ, trainX, trainY, trainZ, motionX, motionZ, speed, info)
                : straight(dir, y, cX, cZ, trainX, trainY, trainZ, motionX, motionZ, speed, info);
    }

    private static LegacyVec3 straight(Direction dir, int y, int cX, int cZ, double trainX, double trainY, double trainZ, double motionX, double motionZ, double speed, RailContext info) {
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
            vec.yCoord = y;
            vec.zCoord = cZ + 0.5;
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
            vec.xCoord = cX + 0.5;
            vec.yCoord = y;
            vec.zCoord = Mth.clamp(targetZ, cZ - 2, cZ + 3);
            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, y, cZ + (motionZ * speed > 0 ? 3 : -3)));
        }
        return vec;
    }

    private static LegacyVec3 curve(Direction dir, int x, int y, int z, int cX, int cZ, double trainX, double trainY, double trainZ, double motionX, double motionZ, double speed, RailContext info) {
        Direction rot = dir == null || dir.getAxis() == Direction.Axis.Y ? null : rotationUp(dir);
        double turnRadius = 4.5D;

        LegacyVec3 vec = LegacyVec3.of(trainX, trainY, trainZ);

        double axisX = cX + 0.5 + dx(dir) * 0.5 + dx(rot) * turnRadius;
        double axisZ = cZ + 0.5 + dz(dir) * 0.5 + dz(rot) * turnRadius;

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
            info.dist(lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX - dx(dir) * 4 + dx(rot) * 5, y, cZ - dz(dir) * 4 + dz(rot) * 5)).yaw((float) moveAngle);
            return LegacyVec3.of(axisX - dx(dir) * turnRadius, y, axisZ - dz(dir) * turnRadius);
        }

        if (effAngle < 0) {
            double angleOvershoot = -effAngle;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(-lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX + dx(dir), y, cZ + dz(dir))).yaw((float) moveAngle);
            return LegacyVec3.of(axisX - dx(rot) * turnRadius, y, axisZ - dz(rot) * turnRadius);
        }

        double radianChange = angularChange * Math.PI / 180D;
        dist.rotateAroundY((float) radianChange);

        return LegacyVec3.of(axisX + dist.xCoord, y, axisZ + dist.zCoord);
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.NARROW;
    }
}
