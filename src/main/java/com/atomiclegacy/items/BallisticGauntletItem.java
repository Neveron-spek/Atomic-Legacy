package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** [parity I2] ItemModTwoKick (ballistic_gauntlet): chestplate servo-slot mod; punches fire 12 gauge shells
 * (handled by ArmorParityEvents#onPlayerPunch like ModEventHandler#onPlayerPunch). */
public final class BallisticGauntletItem extends ArmorModItem {
    public BallisticGauntletItem(Properties properties) {
        super(properties, ArmorModHandler.SERVOS, false, true, false, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\"I've had worse\"").withStyle(ChatFormatting.ITALIC));
        tooltip.add(line(ChatFormatting.YELLOW, "Punches fire 12 gauge shells"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.YELLOW, mod, " (Shotgun punches)"));
    }
}
