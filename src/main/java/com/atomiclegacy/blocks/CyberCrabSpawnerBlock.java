package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.CyberCrabSpawnerBlockEntity;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 BlockCybercrab, registered as meteor_spawner. */
public final class CyberCrabSpawnerBlock extends Block implements EntityBlock {
    public CyberCrabSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CyberCrabSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (type != HbmBlockEntities.CYBER_CRAB_SPAWNER.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                CyberCrabSpawnerBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                        (CyberCrabSpawnerBlockEntity) blockEntity);
    }
}
