package com.atomiclegacy.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** [parity X1: custom machines] BlockCMGlass (cm_tank): not opaque, faces between two tanks are not rendered. */
public class CustomMachineGlassBlock extends LegacyVariantBlock {
    public CustomMachineGlassBlock(Properties properties, int maxVariant) {
        super(properties, maxVariant);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }
}
