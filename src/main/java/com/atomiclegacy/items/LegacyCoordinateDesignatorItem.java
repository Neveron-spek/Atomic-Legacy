package com.atomiclegacy.items;

import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** The 1.7.10 ItemDesingator block-click coordinate writer for launch pads. */
public final class LegacyCoordinateDesignatorItem extends Item {
    private static final String X = "xCoord";
    private static final String Z = "zCoord";

    public LegacyCoordinateDesignatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** [parity P1] ItemDesingator.onItemUse: not on a LaunchPad, "Position set!" chat line client side. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        BlockPos target = context.getClickedPos();
        if (LegacyRangeDesignatorItem.isLaunchPad(context.getLevel(), target)) return InteractionResult.PASS;
        LegacyRangeDesignatorItem.setTarget(stack, target);
        if (context.getLevel().isClientSide) {
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.literal("Position set!"), false);
        } else if (context.getPlayer() != null) {
            context.getLevel().playSound(null, context.getPlayer().blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LegacyRangeDesignatorItem.legacyTooltip(stack, tooltip);
    }
}
