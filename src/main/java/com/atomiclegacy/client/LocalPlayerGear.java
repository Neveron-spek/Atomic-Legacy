package com.atomiclegacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/** [parity R1: ashglasses gas cloud] MainRegistry.proxy.me() checks for client-only effects. */
public final class LocalPlayerGear {
    private LocalPlayerGear() {
    }

    public static boolean wearsOnHead(Item item) {
        var player = Minecraft.getInstance().player;
        return player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(item);
    }
}
