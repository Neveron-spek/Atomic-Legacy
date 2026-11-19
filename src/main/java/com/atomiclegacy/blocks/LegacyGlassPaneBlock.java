package com.atomiclegacy.blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Port of HBM's connected BlockNTMGlassPane. */
public final class LegacyGlassPaneBlock extends IronBarsBlock {
    private final boolean dropsSelf;

    public LegacyGlassPaneBlock(Properties properties, boolean dropsSelf) {
        super(properties);
        this.dropsSelf = dropsSelf;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return dropsSelf ? super.getDrops(state, params) : List.of();
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        if (state == null) return null;
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock() instanceof LegacyGlassBlock) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), true);
            }
        }
        return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor,
                                     net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
                                     net.minecraft.core.BlockPos neighborPos) {
        var updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return direction.getAxis().isHorizontal() && neighbor.getBlock() instanceof LegacyGlassBlock
                ? updated.setValue(PROPERTY_BY_DIRECTION.get(direction), true) : updated;
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
