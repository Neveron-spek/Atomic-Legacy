package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * [parity V1: boilers] 1.7.10 {@code MachineBoiler(false)} (machine_boiler_off, "Old Boiler"): a plain iron block
 * without a tile entity or GUI. Orientation from the neighbours on placement (setDefaultDirection), then from the
 * player's yaw (onBlockPlacedBy); drops 3-6 steel scraps (loot table).
 */
public class SmallBoilerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SmallBoilerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    /** MachineBoiler#onBlockAdded / setDefaultDirection: meta 3 unless an opaque block sits on one side only. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || state.is(oldState.getBlock())) return;
        boolean north = opaque(level, pos.north());
        boolean south = opaque(level, pos.south());
        boolean west = opaque(level, pos.west());
        boolean east = opaque(level, pos.east());
        Direction facing = Direction.SOUTH;
        if (south && !north) facing = Direction.NORTH;
        if (west && !east) facing = Direction.EAST;
        if (east && !west) facing = Direction.WEST;
        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), Block.UPDATE_CLIENTS);
    }

    /** MachineBoiler#onBlockPlacedBy: the front faces the player. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer == null) return;
        BlockState current = level.getBlockState(pos);
        if (current.is(this)) level.setBlock(pos, current.setValue(FACING, placer.getDirection().getOpposite()), Block.UPDATE_CLIENTS);
    }

    private static boolean opaque(Level level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender(level, pos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
