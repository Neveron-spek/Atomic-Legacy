package com.atomiclegacy.blocks;

import com.mojang.serialization.MapCodec;
import com.atomiclegacy.tileentity.FoundryFlowBlockEntity;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class FoundryOutletBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final MapCodec<FoundryOutletBlock> codec;
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0.3125D, 0.0D, 0.0D, 0.6875D, 0.5D, 0.375D);
    private static final VoxelShape EAST_SHAPE = Shapes.box(0.625D, 0.0D, 0.3125D, 1.0D, 0.5D, 0.6875D);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0.3125D, 0.0D, 0.625D, 0.6875D, 0.5D, 1.0D);
    private static final VoxelShape WEST_SHAPE = Shapes.box(0.0D, 0.0D, 0.3125D, 0.375D, 0.5D, 0.6875D);

    private final Kind kind;

    public FoundryOutletBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(nextProperties -> new FoundryOutletBlock(nextProperties, kind));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case OUTLET -> outletStateForPlacement(context);
            case SLAGTAP -> slagTapStateForPlacement(context);
        };
    }

    private BlockState outletStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState slagTapStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryFlowBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTicker(blockEntityType, HbmBlockEntities.FOUNDRY_FLOW.get(), FoundryFlowBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // [parity Q1: FoundryOutlet#onBlockActivated - silent, does nothing while sneaking]
        if (!level.isClientSide && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof FoundryFlowBlockEntity outlet) {
            outlet.toggleRedstoneInversion();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // [parity Q1: scraps set the filter, any other held item toggles the redstone inversion]
        if (!level.isClientSide && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof FoundryFlowBlockEntity outlet) {
            if (stack.getItem() instanceof ScrapsItem) {
                outlet.setFilterFromScraps(stack);
            } else {
                outlet.toggleRedstoneInversion();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * [parity Q1: FoundryOutlet#onScrew, reached when sneaking - screwdriver clears the filter,
     * hand drill inverts it; always returns false, so the tool takes no damage]
     */
    public static void onScrew(Level level, BlockPos pos, boolean handDrill) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof FoundryFlowBlockEntity outlet)
                || !(level.getBlockState(pos).getBlock() instanceof FoundryOutletBlock)) {
            return;
        }
        if (handDrill) {
            outlet.toggleFilterInversion();
        } else {
            outlet.clearFilter();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FoundryFlowBlockEntity outlet) {
            outlet.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case OUTLET -> rotateOutlet(state, rotation);
            case SLAGTAP -> rotateSlagTap(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case OUTLET -> mirrorOutlet(state, mirror);
            case SLAGTAP -> mirrorSlagTap(state, mirror);
        };
    }

    private static BlockState rotateOutlet(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSlagTap(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorOutlet(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSlagTap(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing.getOpposite()) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker
    ) {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }

    public enum Kind {
        OUTLET,
        SLAGTAP
    }
}
