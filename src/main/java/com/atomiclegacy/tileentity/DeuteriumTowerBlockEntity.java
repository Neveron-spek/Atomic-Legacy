package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DeuteriumTowerBlockEntity extends DeuteriumExtractorBlockEntity {
    public DeuteriumTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DEUTERIUM_TOWER.get(), pos, blockState);
    }
}
