package com.atomiclegacy.blocks;

import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.items.HbmChargeableItem;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Direct port of the one-use 1.7.10 HEV suit battery block. */
public final class HevBatteryBlock extends Block {
    private static final long CHARGE = 150_000L;
    private static final VoxelShape SHAPE = box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);

    public HevBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // [parity R4: hev_battery] HEVBattery#onBlockActivated: full FSB set with an ArmorFSBPowered helmet
        if (!ArmorFSBItem.hasFSBArmorIgnoringCharge(player)
                || !(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof com.atomiclegacy.items.PoweredArmorFSBItem)) {
            return InteractionResult.CONSUME;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof HbmChargeableItem battery) {
                battery.hbmSetCharge(stack, Math.min(battery.hbmCharge(stack) + CHARGE, battery.hbmCapacity(stack)));
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.SUIT_BATTERY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return InteractionResult.CONSUME;
    }
}
