package com.atomiclegacy.client;

import com.atomiclegacy.inventory.gui.FlowControlPumpScreen;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** [parity T1: flow_control_pump] client side of FMLNetworkHandler.openGui for GUIPump (no container). */
public final class FlowControlPumpClientHooks {
    private FlowControlPumpClientHooks() {
    }

    public static void open(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof FlowControlPumpBlockEntity pump) {
            minecraft.setScreen(new FlowControlPumpScreen(pump));
        }
    }
}
