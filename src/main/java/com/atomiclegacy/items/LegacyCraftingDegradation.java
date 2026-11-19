package com.atomiclegacy.items;

import net.minecraft.world.item.ItemStack;

/**
 * [parity I3] ItemCraftingDegradation.getContainerItem: the tool stays in the crafting grid and, when it has a
 * durability, takes one point of damage per craft. ItemTooling (screwdriver, hand drill, defuser, archineer wrench)
 * inherits this in 1.7.10. Same break rule as ChemistrySetItem.
 */
public final class LegacyCraftingDegradation {
    private LegacyCraftingDegradation() {
    }

    public static ItemStack remainder(ItemStack stack) {
        ItemStack remaining = stack.copy();
        remaining.setCount(1);
        if (remaining.isDamageableItem() && remaining.getMaxDamage() > 0) {
            remaining.setDamageValue(remaining.getDamageValue() + 1);
            if (remaining.getDamageValue() >= remaining.getMaxDamage()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}
