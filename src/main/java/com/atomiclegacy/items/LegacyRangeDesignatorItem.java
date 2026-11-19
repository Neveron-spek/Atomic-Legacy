package com.atomiclegacy.items;

import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** 1.7.10 ItemDesingatorRange coordinate selection behaviour. */
public class LegacyRangeDesignatorItem extends Item {
    private static final String X = "xCoord";
    private static final String Z = "zCoord";

    public LegacyRangeDesignatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(300.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos target = blockHit.getBlockPos();
        // [parity P1] the old designators ignore a LaunchPad (launch_pad) block and print a plain chat line client side.
        if (isLaunchPad(level, target)) return InteractionResultHolder.pass(stack);
        setTarget(stack, target);
        if (level.isClientSide) {
            player.displayClientMessage(Component.literal("Position set to X:" + target.getX() + ", Z:" + target.getZ()), false);
        } else {
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    static boolean isLaunchPad(Level level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath().equals("launch_pad");
    }

    static void setTarget(ItemStack stack, BlockPos target) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(X, target.getX());
        tag.putInt(Z, target.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        legacyTooltip(stack, tooltip);
    }

    /** [parity P1] ItemDesingator/Range/Manual.addInformation (hard-coded English in 1.7.10). */
    static void legacyTooltip(ItemStack stack, List<Component> tooltip) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (stack.has(DataComponents.CUSTOM_DATA) && (tag.contains(X) || tag.contains(Z))) {
            tooltip.add(LegacyMissileItem.legacy("Target Coordinates:"));
            tooltip.add(LegacyMissileItem.legacy("X: " + tag.getInt(X)));
            tooltip.add(LegacyMissileItem.legacy("Z: " + tag.getInt(Z)));
        } else {
            tooltip.add(LegacyMissileItem.legacy("Please select a target."));
        }
    }

    public static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(X) || tag.contains(Z) ? new BlockPos(tag.getInt(X), 0, tag.getInt(Z)) : null;
    }
}
