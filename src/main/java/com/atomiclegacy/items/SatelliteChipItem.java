package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** 1.7.10 ItemSatChip: each satellite module carries its mutable frequency in NBT. */
public class SatelliteChipItem extends Item {
    private static final String FREQUENCY_KEY = "freq";
    private final String descriptionKey;

    public SatelliteChipItem(Properties properties, String descriptionKey) {
        super(properties);
        this.descriptionKey = descriptionKey;
    }

    public static int frequency(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(FREQUENCY_KEY);
    }

    public static void setFrequency(ItemStack stack, int frequency) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(FREQUENCY_KEY, frequency);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity P2: satellites] ItemSatChip#addInformation: aqua frequency line, then the
        // description (lines split on the 1.7.10 "$" separator, default grey).
        tooltip.add(Component.translatable("item.atomiclegacy.satellite.frequency", frequency(stack)).withStyle(ChatFormatting.AQUA));
        if (!this.descriptionKey.isEmpty()) {
            for (String line : Component.translatable(this.descriptionKey).getString().split("\\$")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
