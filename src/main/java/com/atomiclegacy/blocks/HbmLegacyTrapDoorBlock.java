package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HbmLegacyTrapDoorBlock extends TrapDoorBlock {
    private static final VoxelShape LADDER_NORTH = Shapes.box(0.0D, 0.0D, 0.875D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape LADDER_SOUTH = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.125D);
    private static final VoxelShape LADDER_WEST = Shapes.box(0.875D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape LADDER_EAST = Shapes.box(0.0D, 0.0D, 0.0D, 0.125D, 1.0D, 1.0D);

    public HbmLegacyTrapDoorBlock(Properties properties) {
        // Collision depends on the climbable block below, not just this block's state.
        super(BlockSetType.IRON, properties.dynamicShape());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        state = state.cycle(OPEN);
        level.setBlock(pos, state, 2);
        level.levelEvent(player, 1003, pos, 0);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        state = state.cycle(OPEN);
        level.setBlock(pos, state, 2);
        level.levelEvent(player, 1003, pos, 0);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(OPEN) && level.getBlockState(pos.below()).isLadder(level, pos.below(), entity);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level instanceof LevelReader reader && isLadder(state, reader, pos, null)) {
            return switch (state.getValue(FACING)) {
                case NORTH -> LADDER_NORTH;
                case SOUTH -> LADDER_SOUTH;
                case WEST -> LADDER_WEST;
                default -> LADDER_EAST;
            };
        }
        return super.getCollisionShape(state, level, pos, context);
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
