package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Capacity multiplier installed in the dedicated powered-armor battery slot. */
public final class ArmorBatteryModItem extends ArmorModItem {
    private final double capacityMultiplier;

    public ArmorBatteryModItem(Item.Properties properties, double capacityMultiplier) {
        super(properties, ArmorModHandler.BATTERY, true, true, true, true);
        this.capacityMultiplier = capacityMultiplier;
    }

    public double capacityMultiplier() {
        return this.capacityMultiplier;
    }

    // [parity I2: tooltip] ItemModBattery has no own tooltip lines.
}
