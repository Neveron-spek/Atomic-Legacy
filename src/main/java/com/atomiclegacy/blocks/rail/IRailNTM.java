package com.atomiclegacy.blocks.rail;

import com.atomiclegacy.train.LegacyVec3;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** [parity S1: trains] com.hbm.blocks.rail.IRailNTM ("in retrospect, not the best name i could have chosen"). */
public interface IRailNTM {

    /** Returns a vector pointing to the closest snapping position given the starting position */
    LegacyVec3 getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ);

    /**
     * Returns a location on the rail based on the train's current X/Y/Z momentum as well as the intended speed along the rail.
     * If the train would leave the rail within that tick, the position is the last valid position on that rail.
     * Also features RailContext which determines overshoot and the final yaw rotation.
     */
    LegacyVec3 getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ,
                                 double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context);

    /** Returns that rail's gauge. Trains will derail if the gauge does not match. */
    TrackGauge getGauge(Level world, int x, int y, int z);

    enum TrackGauge {
        STANDARD, //roughly 1.5m
        NARROW    //roughly 0.75m
    }

    /** A wrapper for all relevant info required when leaving a rail */
    final class RailContext {
        /** The angle at which the train ends up being on this rail */
        public float yaw;
        /** The amount of blocks still left to travel after completing the rail */
        public double overshoot;
        /** The exit position of that rail */
        public BlockPos pos;

        public RailContext yaw(float y) { this.yaw = y; return this; }
        public RailContext dist(double d) { this.overshoot = d; return this; }
        public RailContext pos(BlockPos d) { this.pos = d; return this; }
    }

    /** A wrapper for additional information like stopping on rails and what type of check we're doing */
    final class MoveContext {
        public RailCheckType type;
        public double collisionBogieDistance;
        /** if a buffer stop or similar applies */
        public boolean collision = false;
        /** how much of the travel distance was cut short */
        public double overshoot;

        public MoveContext(RailCheckType type, double collisionBogieDistance) {
            this.type = type;
            this.collisionBogieDistance = collisionBogieDistance;
        }
    }

    enum RailCheckType {
        CORE,
        FRONT,
        BACK,
        OTHER
    }

    /** faux BlockPos(double, double, double): MathHelper.floor_double on every axis. */
    static BlockPos floorPos(double x, double y, double z) {
        return new BlockPos(net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(y), net.minecraft.util.Mth.floor(z));
    }
}
