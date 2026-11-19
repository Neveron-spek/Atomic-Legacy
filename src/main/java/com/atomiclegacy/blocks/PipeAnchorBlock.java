package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.PipeAnchorBlockEntity;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class PipeAnchorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /**
     * [parity T1: pipe_anchor_exhaust] ExhaustPipeAnchor: smoke-only TileEntityPipeExhaustAnchor.
     * [parity V2: pipe_anchor_pneumatic] PneumoPipeAnchor: TileEntityPneumaticPipeAnchor (NetworkType.PNEUMATIC).
     */
    public enum Kind { FLUID, EXHAUST, PNEUMATIC }

    private final Kind kind;

    public PipeAnchorBlock(Properties properties) {
        this(properties, Kind.FLUID);
    }

    public PipeAnchorBlock(Properties properties, boolean exhaust) {
        this(properties, exhaust ? Kind.EXHAUST : Kind.FLUID);
    }

    public PipeAnchorBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    public Kind kind() {
        return kind;
    }

    public boolean exhaust() {
        return kind == Kind.EXHAUST;
    }

    public boolean pneumatic() {
        return kind == Kind.PNEUMATIC;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (!level.isClientSide || blockEntityType != HbmBlockEntities.PIPE_ANCHOR.get()) { // shared by pipe_anchor_exhaust
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                ((PipeAnchorBlockEntity) blockEntity).refreshClientRenderSnapshot();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.atomiclegacy.pipe_anchor.connection_type"));
        tooltip.add(Component.translatable("tooltip.atomiclegacy.pipe_anchor.connection_range"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction external = state.getValue(FACING).getOpposite();
        double min = 4.0D / 16.0D;
        double max = 12.0D / 16.0D;
        double minX = external == Direction.WEST ? 0.0D : min;
        double maxX = external == Direction.EAST ? 1.0D : max;
        double minY = external == Direction.DOWN ? 0.0D : min;
        double maxY = external == Direction.UP ? 1.0D : max;
        double minZ = external == Direction.NORTH ? 0.0D : min;
        double maxZ = external == Direction.SOUTH ? 1.0D : max;
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.getBlockEntity(pos) instanceof PipeAnchorBlockEntity anchor) {
            anchor.markNetworkChanged();
        }
        if (pneumatic() && !state.is(oldState.getBlock())) {
            com.atomiclegacy.pneumatic.PneumaticNetworkManager.invalidate(level, pos); // [parity V2: pipe_anchor_pneumatic]
        }
        refreshNeighbors(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            level.invalidateCapabilities(pos);
            if (pneumatic()) {
                com.atomiclegacy.pneumatic.PneumaticNetworkManager.invalidate(level, pos); // [parity V2: pipe_anchor_pneumatic]
            }
            refreshNeighbors(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void refreshNeighbors(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, pos.relative(direction));
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
