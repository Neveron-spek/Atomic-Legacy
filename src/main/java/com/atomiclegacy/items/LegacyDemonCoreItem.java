package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct port of ItemDemonCore: dropped open cores close and eject a screwdriver. */
public final class LegacyDemonCoreItem extends Item {
    public LegacyDemonCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide || !entity.onGround()) {
            return false;
        }
        Item closedCore = BuiltInRegistries.ITEM.get(AtomicLegacy.id("demon_core_closed"));
        if (closedCore == null || closedCore == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        entity.setItem(new ItemStack(closedCore, stack.getCount()));
        level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(HbmItems.SCREWDRIVER.get())));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("trait.atomiclegacy.drop").withStyle(ChatFormatting.RED));
    }
}
