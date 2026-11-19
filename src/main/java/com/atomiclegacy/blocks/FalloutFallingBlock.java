package com.atomiclegacy.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FallingBlock;

public class FalloutFallingBlock extends FallingBlock {
    private static final MapCodec<FalloutFallingBlock> CODEC = simpleCodec(FalloutFallingBlock::new);

    public FalloutFallingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    // [parity K2: block radiation] 1.7.10 block_fallout is a BlockHazardFalling: after placement it emits
    // 0.1 x its item radiation every 20 ticks (LegacyBlockHazardRadiation); the former fixed +1 RAD/s capped at
    // 10 000 was an approximation of the port base.
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
