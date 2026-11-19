package com.atomiclegacy.worldgen.structure;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** [parity E1: diagnostics] Public window onto the package-private spawn condition table. */
public final class HbmLegacyStructureSelectionProbe {
    private HbmLegacyStructureSelectionProbe() {
    }

    public static Boolean canSpawn(String spawnName, Holder<Biome> biome) {
        return HbmLegacyStructureSelection.canSpawn(spawnName, biome);
    }
}
