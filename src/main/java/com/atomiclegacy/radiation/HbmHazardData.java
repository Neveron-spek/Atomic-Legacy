package com.atomiclegacy.radiation;

/**
 * Per-item hazard levels of a stack, summed per HazardTypeBase after the modifiers ([parity K1: HazardRegistry]).
 * {@code contaminating} is kept for callers of the former port-only dropped-item pass; it is always 0.
 */
public record HbmHazardData(
        double radiation,
        double contaminating,
        double digamma,
        double hot,
        double blinding,
        double asbestos,
        double coal,
        double hydroactive,
        double explosive
) {
    public static final HbmHazardData EMPTY = new HbmHazardData(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    public HbmHazardData(double radiation, double contaminating, double digamma, double hot, double blinding) {
        this(radiation, contaminating, digamma, hot, blinding, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public boolean isEmpty() {
        return radiation <= 0.0D && contaminating <= 0.0D && digamma <= 0.0D && hot <= 0.0D && blinding <= 0.0D
                && asbestos <= 0.0D && coal <= 0.0D && hydroactive <= 0.0D && explosive <= 0.0D;
    }

    public HbmHazardData multiply(double multiplier) {
        if (multiplier == 1.0D || isEmpty()) {
            return this;
        }
        return new HbmHazardData(
                radiation * multiplier,
                contaminating * multiplier,
                digamma * multiplier,
                hot * multiplier,
                blinding * multiplier,
                asbestos * multiplier,
                coal * multiplier,
                hydroactive * multiplier,
                explosive * multiplier
        );
    }

    public HbmHazardData plus(HbmHazardData other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        return new HbmHazardData(
                this.radiation + other.radiation,
                this.contaminating + other.contaminating,
                this.digamma + other.digamma,
                this.hot + other.hot,
                this.blinding + other.blinding,
                this.asbestos + other.asbestos,
                this.coal + other.coal,
                this.hydroactive + other.hydroactive,
                this.explosive + other.explosive
        );
    }
}
