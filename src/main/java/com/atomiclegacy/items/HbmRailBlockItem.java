package com.atomiclegacy.items;

import com.atomiclegacy.blocks.HbmRailBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HbmRailBlockItem extends BlockItem {
    public HbmRailBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!(this.getBlock() instanceof HbmRailBlock rail)) {
            return;
        }

        float speed = rail.maxSpeed() / HbmRailBlock.VANILLA_SPEED; // [parity R2] RailGeneric: (int) (speed * 100), shown when speed != 1
        int speedPercent = (int) (speed * 100);
        if (speed != 1F) {
            tooltip.add(Component.translatable("tooltip.atomiclegacy.rail.speed", speedPercent)
                    .withStyle(speed > 1 ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        if (!rail.flexible()) {
            tooltip.add(Component.translatable("tooltip.atomiclegacy.rail.no_turns").withStyle(ChatFormatting.RED));
        }
        if (!rail.slopable()) {
            tooltip.add(Component.translatable("tooltip.atomiclegacy.rail.no_slopes").withStyle(ChatFormatting.RED));
        }
    }
}
