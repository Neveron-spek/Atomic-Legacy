package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * [parity I3] 1.7.10 ItemMS (mysteryshovel): a plain item (stack 1, no tool stats). Right-clicking ntm_dirt breaks it
 * without drops and spawns ingot_u238m2 metas 1, 2 and 3 with the vanilla dispenser-style scatter.
 */
public final class LegacyMysteryShovelItem extends Item {
    public LegacyMysteryShovelItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LegacyConsumableSupport.literal(tooltip, "Lost but not forgotten");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) return InteractionResult.PASS;
        if (!level.getBlockState(pos).is(BuiltInRegistries.BLOCK.get(AtomicLegacy.id("ntm_dirt")))) return InteractionResult.PASS;
        level.destroyBlock(pos, false);
        Item ingot = BuiltInRegistries.ITEM.get(AtomicLegacy.id("ingot_u238m2"));
        for (int meta = 1; meta <= 3; meta++) {
            ItemStack stack = LegacyUnstableItem.stackFor(ingot, meta);
            float f = level.random.nextFloat() * 0.8F + 0.1F;
            float f1 = level.random.nextFloat() * 0.8F + 0.1F;
            float f2 = level.random.nextFloat() * 0.8F + 0.1F;
            ItemEntity entity = new ItemEntity(level, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, stack);
            float f3 = 0.05F;
            entity.setDeltaMovement((float) level.random.nextGaussian() * f3, (float) level.random.nextGaussian() * f3 + 0.2F,
                    (float) level.random.nextGaussian() * f3);
            level.addFreshEntity(entity);
        }
        return InteractionResult.SUCCESS;
    }
}
