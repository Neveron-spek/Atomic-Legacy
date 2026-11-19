package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * [parity d2] MachineDiFurnaceExtension (@Deprecated, machineTab): opens the blast furnace below and proxies its
 * inventory (TileEntityProxyCombo().inventory(); the item handler is registered in HbmCapabilities). The furnace below
 * works three times as fast while this block sits on top of it.
 */
public final class DiFurnaceExtensionBlock extends Block {
    public DiFurnaceExtensionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos.below()) instanceof LegacyDeprecatedFurnaceBlockEntity furnace
                && furnace.kind() == LegacyDeprecatedFurnaceBlock.Kind.DIFURNACE) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return LegacyDeprecatedFurnaceBlock.open(level, pos.below(), player);
        }
        return InteractionResult.PASS;
    }
}
