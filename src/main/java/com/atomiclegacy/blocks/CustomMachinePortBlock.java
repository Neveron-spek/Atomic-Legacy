package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.CustomMachinePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** [parity X1: custom machines] BlockCMPort: a cm_* casing whose tile is the controller's proxy port. */
public class CustomMachinePortBlock extends LegacyVariantBlock implements EntityBlock {
    public CustomMachinePortBlock(Properties properties, int maxVariant) {
        super(properties, maxVariant);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomMachinePortBlockEntity(pos, state);
    }
}
