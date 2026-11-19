package com.atomiclegacy.blocks;

import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.tileentity.EnergyConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** [parity L1: converters] 1.7.10 BlockConverterHeRf / BlockConverterRfHe (full cube, all-side ports). */
public final class EnergyConverterBlock extends Block implements EntityBlock {
    private final boolean heToRf;

    public EnergyConverterBlock(Properties properties, boolean heToRf) {
        super(properties);
        this.heToRf = heToRf;
    }

    public boolean heToRf() {
        return heToRf;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConverterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (tickerLevel, pos, tickerState, entity) -> {
            if (entity instanceof EnergyConverterBlockEntity converter) {
                converter.serverTick(tickerLevel);
            }
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
