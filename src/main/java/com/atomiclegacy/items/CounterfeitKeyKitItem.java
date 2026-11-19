package com.atomiclegacy.items;

import com.atomiclegacy.tileentity.LockableBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class CounterfeitKeyKitItem extends Item {
    public CounterfeitKeyKitItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // [parity I3] ItemCounterfeitKeys uses world.getTileEntity directly (no dummy-to-core lookup, unlike ItemLock)
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof LockableBlockEntity lockable)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!lockable.isLocked() || !lockable.cheesable()) {
            if (!context.getLevel().isClientSide && player != null && !lockable.cheesable()) {
                player.displayClientMessage(Component.translatable("chat.atomiclegacy.key_kit.no_counterfeit").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                player.displayClientMessage(Component.translatable("chat.atomiclegacy.key_kit.no_counterfeit_hint").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide && player != null) {
            ItemStack first = new ItemStack(HbmItems.KEY_FAKE.get());
            ItemStack second = first.copy();
            KeyPinItem.setPins(first, lockable.pins());
            KeyPinItem.setPins(second, lockable.pins());
            player.setItemInHand(context.getHand(), first);
            if (!player.getInventory().add(second)) {
                player.drop(second, false);
            }
            player.swing(context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.atomiclegacy.key_kit").withStyle(ChatFormatting.GRAY));
    }
}
