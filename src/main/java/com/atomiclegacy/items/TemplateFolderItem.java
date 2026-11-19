package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TemplateFolderItem extends Item {
    public TemplateFolderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.atomiclegacy.template_folder.desc.machine").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.atomiclegacy.template_folder.desc.fluid").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.atomiclegacy.template_folder.desc.press").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.atomiclegacy.template_folder.desc.siren").withStyle(ChatFormatting.GRAY));
    }
}
