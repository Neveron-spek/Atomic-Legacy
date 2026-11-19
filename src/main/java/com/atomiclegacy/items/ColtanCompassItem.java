package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

/** Stores the same world-seed-derived coltan deposit target as 1.7.10. */
public class ColtanCompassItem extends Item {
    private static final String X = "colX";
    private static final String Z = "colZ";

    public ColtanCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(X) && tag.contains(Z)) {
            return;
        }
        Random random = new Random(level.getServer().getWorldData().worldGenOptions().seed() + 5L);
        tag.putInt(X, (int) (random.nextGaussian() * 1500.0D));
        tag.putInt(Z, (int) (random.nextGaussian() * 1500.0D));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // ItemColtanCompass.addInformation (literal lines; the target coordinates were never shown)
        LegacyTooltips.line(tooltip, "Points towards the coltan deposit.");
        LegacyTooltips.line(tooltip, "The deposit is a large area where coltan ore spawns like standard ore,");
        LegacyTooltips.line(tooltip, "it's not one large blob of ore on that exact location.");
    }

    /** Seed-derived destination consumed by the client compass property. */
    public static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(X) && tag.contains(Z)
                ? new BlockPos(tag.getInt(X), 0, tag.getInt(Z))
                : null;
    }
}
