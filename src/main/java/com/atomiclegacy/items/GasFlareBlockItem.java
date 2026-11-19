package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class GasFlareBlockItem extends LegacyOffsetBlockItem {
    public GasFlareBlockItem(Block block, Item.Properties properties) {
        super(block, properties, 1, true);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // MachineGasFlare.addInformation (literal lines)
        LegacyTooltips.line(tooltip, ChatFormatting.GOLD, "Can burn fluids and vent gasses");
        LegacyTooltips.pair(tooltip, ChatFormatting.GOLD, "Burns up to ", ChatFormatting.RED, "10mB/t");
        LegacyTooltips.pair(tooltip, ChatFormatting.GOLD, "Vents up to ", ChatFormatting.RED, "50mB/t");
        LegacyTooltips.line(tooltip, "");
        LegacyTooltips.line(tooltip, ChatFormatting.YELLOW, "Fuel efficiency:");
        LegacyTooltips.pair(tooltip, ChatFormatting.YELLOW, "-Flammable Gasses: ", ChatFormatting.RED, "20%");
        LegacyTooltips.pair(tooltip, ChatFormatting.YELLOW, "-Flammable Liquids: ", ChatFormatting.RED, "10%");
    }
}
