package com.atomiclegacy.tileentity;

public interface HeatSourceBlockEntity {
    int getHeatStored();

    void useHeat(int amount);
}
