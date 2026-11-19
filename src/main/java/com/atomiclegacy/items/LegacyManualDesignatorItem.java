package com.atomiclegacy.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** [parity P1: designator_manual] 1.7.10 ItemDesingatorManual: right-click opens GUIScreenDesignator. */
public final class LegacyManualDesignatorItem extends LegacyRangeDesignatorItem {
    public LegacyManualDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && hand == InteractionHand.MAIN_HAND && FMLEnvironment.dist.isClient())
            com.atomiclegacy.inventory.gui.ManualDesignatorScreen.open(player);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LegacyRangeDesignatorItem.legacyTooltip(stack, tooltip);
    }

    /** ItemDesignatorPacket.Handler.onMessage. Missing coordinates read as 0 like NBTTagCompound.getInteger. */
    public static void apply(ItemStack stack, int operator, int value, int reference, double playerX, double playerZ) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int x = tag.getInt("xCoord");
        int z = tag.getInt("zCoord");
        if (operator == 2) {
            if (reference == 0) x = (int) Math.round(playerX);
            else z = (int) Math.round(playerZ);
        } else {
            int result = operator == 0 ? value : operator == 1 ? -value : 0;
            if (reference == 0) x += result;
            else z += result;
        }
        tag.putInt("xCoord", x);
        tag.putInt("zCoord", z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
