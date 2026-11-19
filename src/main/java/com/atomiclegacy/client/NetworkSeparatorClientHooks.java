package com.atomiclegacy.client;

import com.atomiclegacy.inventory.gui.NetworkSeparatorScreen;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** [parity U1: network_separator] client side of FMLNetworkHandler.openGui for GUIDiode (no container). */
public final class NetworkSeparatorClientHooks {
    private NetworkSeparatorClientHooks() {
    }

    public static void open(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof NetworkSeparatorBlockEntity separator) {
            minecraft.setScreen(new NetworkSeparatorScreen(separator));
        }
    }
}
