package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/**
 * missile_soyuz_lander: a plain 1.7.10 ItemCustomLore (stack size 1, common
 * rarity) whose "item.missile_soyuz_lander.desc" lore is split on "$".
 * [parity P2: soyuz] rarity and tooltip as in the reference.
 */
public final class OrbitalModuleItem extends Item {
    public OrbitalModuleItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String line : Component.translatable("item.atomiclegacy.missile_soyuz_lander.desc").getString().split("\\$")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
