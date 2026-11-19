package com.atomiclegacy.blocks;

import com.atomiclegacy.items.ScrewdriverItem;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.tileentity.FluidPipeBlockEntity;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Pneumatic tube (PneumoTube / PneumoTubePaintableBlock). The six booleans hold the tube-to-tube / storage
 * connections; the insertion and ejection sides and the compressed-air connectors live in the block entity.
 * [parity R2: pneumatic tube mechanics]
 */
public class PneumaticTubeBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final double LOWER = 0.3125D;
    private static final double UPPER = 0.6875D;

    private final boolean paintable;

    public PneumaticTubeBlock(Properties properties, boolean paintable) {
        super(properties);
        this.paintable = paintable;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    public boolean isPaintable() {
        return paintable;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), canConnect(context.getLevel().getBlockState(pos.relative(direction)), direction));
        }
        return state;
    }

    /** PneumoTube.setBlockBoundsBasedOnState: one box spanning every connected arm. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean[] arms = arms(state, level, pos);
        return Shapes.box(
                arms[Direction.WEST.ordinal()] ? 0 : LOWER, arms[Direction.DOWN.ordinal()] ? 0 : LOWER, arms[Direction.NORTH.ordinal()] ? 0 : LOWER,
                arms[Direction.EAST.ordinal()] ? 1 : UPPER, arms[Direction.UP.ordinal()] ? 1 : UPPER, arms[Direction.SOUTH.ordinal()] ? 1 : UPPER);
    }

    /** PneumoTube.addCollisionBoxesToList: the core plus each tube / air arm (not the inventory connectors). */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.box(LOWER, LOWER, LOWER, UPPER, UPPER, UPPER);
        for (Direction dir : Direction.values()) {
            if (state.getValue(property(dir)) || (level instanceof Level real && canConnectToAir(real, pos, dir))) {
                shape = Shapes.or(shape, arm(dir));
            }
        }
        return shape;
    }

    private static boolean[] arms(BlockState state, BlockGetter level, BlockPos pos) {
        boolean[] arms = new boolean[6];
        PneumaticTubeBlockEntity tube = level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity t ? t : null;
        for (Direction dir : Direction.values()) {
            arms[dir.ordinal()] = state.getValue(property(dir))
                    || (level instanceof Level real && canConnectToAir(real, pos, dir))
                    || (tube != null && (tube.insertionDir() == dir || tube.ejectionDir() == dir));
        }
        return arms;
    }

    private static VoxelShape arm(Direction dir) {
        return switch (dir) {
            case EAST -> Shapes.box(UPPER, LOWER, LOWER, 1, UPPER, UPPER);
            case WEST -> Shapes.box(0, LOWER, LOWER, LOWER, UPPER, UPPER);
            case UP -> Shapes.box(LOWER, UPPER, LOWER, UPPER, 1, UPPER);
            case DOWN -> Shapes.box(LOWER, 0, LOWER, UPPER, LOWER, UPPER);
            case SOUTH -> Shapes.box(LOWER, LOWER, UPPER, UPPER, UPPER, 1);
            case NORTH -> Shapes.box(LOWER, LOWER, 0, UPPER, UPPER, LOWER);
        };
    }

    /** PneumoTube.canConnectToAir: compressor tubes connect to ducts that carry AIR on that side. */
    public static boolean canConnectToAir(Level level, BlockPos pos, Direction dir) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) return false;
        if (!tube.isCompressor()) return false;
        if (tube.ejectionDir() == dir || tube.insertionDir() == dir) return false;
        BlockPos neighborPos = pos.relative(dir);
        if (!level.isLoaded(neighborPos)) return false;
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof PneumaticTubeBlockEntity) return false;
        return neighbor instanceof FluidPipeBlockEntity pipe && pipe.canConnectFrom(dir.getOpposite(), PneumaticTubeBlockEntity.air());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) {
            tube.requestModelDataUpdate();
        }
        return state.setValue(property(direction), canConnect(neighborState, direction));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PneumaticNetworkManager.invalidate(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            PneumaticNetworkManager.invalidate(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** PneumoTube.onBlockActivated: compressors open GUI 0, endpoints GUI 1 (filters only); sneaking passes. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) return InteractionResult.PASS;
        if (!tube.isCompressor() && !tube.isEndpoint()) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(tube, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeBoolean(!tube.isCompressor());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** PneumoTube.onScrew: cycle insertion (sneaking: ejection) side; damages the screwdriver like ItemTooling. */
    public static boolean configureWithScrewdriver(Level level, BlockPos pos, Player player, ItemStack stack, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) return false;
        if (!level.isClientSide) {
            tube.cycleDirection(player.isShiftKeyDown());
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    /** PneumoTube.canConnectTo; [parity V2: pipe_anchor_pneumatic] anchors only on their IPneumaticConnector side. */
    private static boolean canConnect(BlockState state, Direction direction) {
        return com.atomiclegacy.pneumatic.PneumaticNetworkManager.connectsOn(state, direction.getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumaticTubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != HbmBlockEntities.PNEUMATIC_TUBE.get() ? null
                : (tickLevel, tickPos, tickState, entity) -> PneumaticTubeBlockEntity.serverTick(tickLevel, tickPos, tickState, (PneumaticTubeBlockEntity) entity);
    }
}
