package com.atomiclegacy.api.entity;

public interface LegacyTurretMachineTarget {
    TargetKind turretTargetKind();

    enum TargetKind {
        MISSILE,
        RAILCAR,
        BOMBER
    }
}
