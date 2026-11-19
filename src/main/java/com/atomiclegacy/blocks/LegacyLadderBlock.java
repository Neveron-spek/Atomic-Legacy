package com.atomiclegacy.blocks;

/** Original HBM ladders displace water rather than acquiring a new waterlogged state. */
public final class LegacyLadderBlock extends net.minecraft.world.level.block.LadderBlock {
    public LegacyLadderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
    }

    @Override
    public boolean canPlaceLiquid(net.minecraft.world.entity.player.Player player,
                                  net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
                               net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

}
