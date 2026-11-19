package com.atomiclegacy.client;

import com.atomiclegacy.inventory.gui.WandConfigScreens;
import net.minecraft.core.BlockPos;

public final class WandClientHooks {
    private WandClientHooks() {
    }

    public static void openStructure(BlockPos pos, boolean load) {
        WandConfigScreens.openStructure(pos, load);
    }

    public static void openJigsaw(BlockPos pos) {
        WandConfigScreens.openJigsaw(pos);
    }

    public static void openTandem(BlockPos pos) {
        WandConfigScreens.openTandem(pos);
    }
}
