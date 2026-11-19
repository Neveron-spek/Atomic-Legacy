package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import java.util.List;

/**
 * ItemSoyuz: metadata 0/1/2 is the skin (Original / Luna Space Center /
 * Post-War) with rarity uncommon/rare/epic, icons soyuz_0..2 and one
 * "Skin: <name>" tooltip line.  [parity P2: soyuz skins] the 1.7.10 damage
 * value is stored as custom data "skin".
 */
public final class SoyuzItem extends Item {
    public SoyuzItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** Raw 1.7.10 damage value; renderers ignore values outside 0..2 like SoyuzPronter. */
    public static int rawSkin(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("skin");
    }

    public static int skin(ItemStack stack) {
        return Math.clamp(rawSkin(stack), 0, 2);
    }

    public static void setSkin(ItemStack stack, int skin) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("skin", skin);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.RARITY, skin == 0 ? Rarity.UNCOMMON : skin == 1 ? Rarity.RARE
                : skin == 2 ? Rarity.EPIC : Rarity.COMMON);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int skin = 0; skin < 3; skin++) {
            ItemStack stack = new ItemStack(this);
            if (skin > 0) setSkin(stack, skin);
            output.accept(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int skin = rawSkin(stack);
        if (skin < 0 || skin > 2) return;
        ChatFormatting color = skin == 0 ? ChatFormatting.GOLD : skin == 1 ? ChatFormatting.BLUE : ChatFormatting.GREEN;
        tooltip.add(Component.translatable("tooltip.atomiclegacy.soyuz.skin").append(" ")
                .append(Component.translatable("tooltip.atomiclegacy.soyuz.skin." + skin).withStyle(color))
                .withStyle(ChatFormatting.GRAY));
    }
}
