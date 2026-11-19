package com.atomiclegacy.train;

import net.minecraft.util.Mth;

/**
 * [parity S1: trains] Mutable copy of the 1.7.10 {@code net.minecraft.util.Vec3} semantics the train code depends on:
 * {@link #subtract} returns {@code other - this}, {@link #lengthVector} and {@link #normalize} go through
 * {@code MathHelper.sqrt_double} (float precision) and the rotations use the shared sine table.
 */
public final class LegacyVec3 {
    public double xCoord;
    public double yCoord;
    public double zCoord;

    public LegacyVec3(double x, double y, double z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public static LegacyVec3 of(double x, double y, double z) {
        return new LegacyVec3(x, y, z);
    }

    public LegacyVec3 copy() {
        return new LegacyVec3(xCoord, yCoord, zCoord);
    }

    /** 1.7.10: {@code createVectorHelper(vec.x - this.x, vec.y - this.y, vec.z - this.z)}. */
    public LegacyVec3 subtract(LegacyVec3 vec) {
        return new LegacyVec3(vec.xCoord - this.xCoord, vec.yCoord - this.yCoord, vec.zCoord - this.zCoord);
    }

    public LegacyVec3 addVector(double x, double y, double z) {
        return new LegacyVec3(this.xCoord + x, this.yCoord + y, this.zCoord + z);
    }

    public LegacyVec3 normalize() {
        double length = sqrtDouble(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
        return length < 1.0E-4D ? new LegacyVec3(0.0D, 0.0D, 0.0D)
                : new LegacyVec3(this.xCoord / length, this.yCoord / length, this.zCoord / length);
    }

    public double lengthVector() {
        return sqrtDouble(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
    }

    public void rotateAroundX(float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double x = this.xCoord;
        double y = this.yCoord * cos + this.zCoord * sin;
        double z = this.zCoord * cos - this.yCoord * sin;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public void rotateAroundY(float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double x = this.xCoord * cos + this.zCoord * sin;
        double y = this.yCoord;
        double z = this.zCoord * cos - this.xCoord * sin;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public void rotateAroundZ(float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double x = this.xCoord * cos + this.yCoord * sin;
        double y = this.yCoord * cos - this.xCoord * sin;
        double z = this.zCoord;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    /** MathHelper.sqrt_double returns a float. */
    public static double sqrtDouble(double value) {
        return (float) Math.sqrt(value);
    }

    /** MathHelper.wrapAngleTo180_double. */
    public static double wrapAngleTo180(double value) {
        value %= 360.0D;
        if (value >= 180.0D) value -= 360.0D;
        if (value < -180.0D) value += 360.0D;
        return value;
    }

    /** BobMathUtil.angularDifference. */
    public static double angularDifference(double alpha, double beta) {
        double delta = (beta - alpha + 180) % 360 - 180;
        return delta < -180 ? delta + 360 : delta;
    }

    @Override
    public String toString() {
        return "(" + xCoord + ", " + yCoord + ", " + zCoord + ")";
    }
}
