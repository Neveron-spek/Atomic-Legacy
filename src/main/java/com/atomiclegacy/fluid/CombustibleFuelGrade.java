package com.atomiclegacy.fluid;

import java.util.Locale;

public enum CombustibleFuelGrade {
    LOW("trait.atomiclegacy.combustible.low"),
    MEDIUM("trait.atomiclegacy.combustible.medium"),
    HIGH("trait.atomiclegacy.combustible.high"),
    AERO("trait.atomiclegacy.combustible.aero"),
    GAS("trait.atomiclegacy.combustible.gas");

    private final String translationKey;

    CombustibleFuelGrade(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static CombustibleFuelGrade parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
