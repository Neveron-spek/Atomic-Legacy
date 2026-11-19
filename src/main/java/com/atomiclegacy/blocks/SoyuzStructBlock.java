package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.tileentity.SoyuzStructBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** [parity P2: soyuz_launcher] BlockSoyuzStruct: plain non-opaque block with the TileEntitySoyuzStruct checker. */
public final class SoyuzStructBlock extends Block implements EntityBlock {
    public SoyuzStructBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoyuzStructBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.SOYUZ_STRUCT.get()) return null;
        return (tickerLevel, pos, tickerState, entity) ->
                SoyuzStructBlockEntity.tick(tickerLevel, pos, tickerState, (SoyuzStructBlockEntity) entity);
    }
}
