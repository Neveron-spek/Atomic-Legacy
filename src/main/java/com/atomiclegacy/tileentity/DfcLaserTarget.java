package com.atomiclegacy.tileentity;

import net.minecraft.core.Direction;

public interface DfcLaserTarget {
    void addDfcEnergy(long energy, Direction beamDirection);
}
