package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity K2: block radiation] 1.7.10 BlockHazardFalling (block_yellowcake): emission as
 * {@link LegacyBlockHazardRadiation}, then the falling update. No contact dose (invented by the port base).
 */
public final class RadiatingFallingBlock extends HbmFallingBlock {
    public RadiatingFallingBlock(Properties properties, double radiation) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.getBlock() != state.getBlock() && !level.isClientSide) {
            LegacyBlockHazardRadiation.onBlockAdded(this, level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        LegacyBlockHazardRadiation.updateTick(this, level, pos);
        super.tick(state, level, pos, random);
    }
}
