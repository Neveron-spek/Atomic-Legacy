package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WatzPumpBlockEntity extends BlockEntity {
    public WatzPumpBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WATZ_PUMP.get(), pos, state);
    }
}
