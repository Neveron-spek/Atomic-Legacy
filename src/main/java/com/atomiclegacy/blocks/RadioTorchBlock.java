package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.rtty.RttyValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Six-sided redstone-over-radio endpoint variants from 1.7.10. The block is
 * attached to a supporting face while its active face points away from that
 * support, matching the old metadata placement convention.
 */
public final class RadioTorchBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public enum Kind {
        SENDER,
        RECEIVER,
        COUNTER,
        LOGIC,
        READER,
        CONTROLLER,
        RELAY // [parity R2] RadioTorchRelay: placeable, overlay only (TileEntityRadioTorchRelay.updateEntity is a TODO in 1.7.10)
    }

    private final Kind kind;

    public RadioTorchBlock(Properties properties, Kind kind) {
        super(properties.noOcclusion().noCollission());
        this.kind = kind;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(POWERED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case SENDER -> senderStateForPlacement(context);
            case RECEIVER -> receiverStateForPlacement(context);
            case COUNTER -> counterStateForPlacement(context);
            case LOGIC -> logicStateForPlacement(context);
            case READER -> readerStateForPlacement(context);
            case CONTROLLER -> controllerStateForPlacement(context);
            case RELAY -> senderStateForPlacement(context);
        };
    }

    private BlockState senderStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    private BlockState receiverStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    private BlockState counterStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    private BlockState logicStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    private BlockState readerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    private BlockState controllerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return switch (kind) {
            case SENDER -> senderCanSurvive(level, pos, facing);
            case RECEIVER -> receiverCanSurvive(level, pos, facing);
            case COUNTER -> counterCanSurvive(level, pos, facing);
            case LOGIC -> logicCanSurvive(level, pos, facing);
            case READER -> readerCanSurvive(level, pos, facing);
            case CONTROLLER -> controllerCanSurvive(level, pos, facing);
            case RELAY -> relayCanSurvive(level, pos, facing);
        };
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (kind) {
            case SENDER -> senderShape(facing);
            case RECEIVER -> receiverShape(facing);
            case COUNTER -> counterShape(facing);
            case LOGIC -> logicShape(facing);
            case READER -> readerShape(facing);
            case CONTROLLER -> controllerShape(facing);
            case RELAY -> senderShape(facing);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind == Kind.RECEIVER || kind == Kind.LOGIC;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!isSignalSource(state)) {
            return 0;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof RadioTorchBlockEntity radio ? radio.signal() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (kind == Kind.RELAY) {
            return player.isShiftKeyDown() ? InteractionResult.PASS : InteractionResult.sidedSuccess(level.isClientSide); // [parity R2] provideGUI returns null
        }
        // [parity R2] RadioTorchBase.onBlockActivated: every torch opens its GUI unless sneaking
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof RadioTorchBlockEntity radio) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(radio, buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeEnum(kind);
                });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchBlockEntity(pos, state, kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return type == HbmBlockEntities.RADIO_TORCH.get()
                ? (tickLevel, tickPos, tickState, entity) -> RadioTorchBlockEntity.tick(
                tickLevel, tickPos, tickState, (RadioTorchBlockEntity) entity)
                : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case SENDER -> rotateSender(state, rotation);
            case RECEIVER -> rotateReceiver(state, rotation);
            case COUNTER -> rotateCounter(state, rotation);
            case LOGIC -> rotateLogic(state, rotation);
            case READER -> rotateReader(state, rotation);
            case CONTROLLER -> rotateController(state, rotation);
            case RELAY -> rotateSender(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case SENDER -> mirrorSender(state, mirror);
            case RECEIVER -> mirrorReceiver(state, mirror);
            case COUNTER -> mirrorCounter(state, mirror);
            case LOGIC -> mirrorLogic(state, mirror);
            case READER -> mirrorReader(state, mirror);
            case CONTROLLER -> mirrorController(state, mirror);
            case RELAY -> mirrorSender(state, mirror);
        };
    }

    private static BlockState rotateSender(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateReceiver(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCounter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateLogic(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateReader(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateController(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorSender(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorReceiver(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorCounter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorLogic(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorReader(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorController(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    private static boolean senderCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return baseCanStay(level, support, facing); // [parity R2] RadioTorchBase.canBlockStay
    }

    private static boolean receiverCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return baseCanStay(level, support, facing); // [parity R2] RadioTorchBase.canBlockStay
    }

    private static boolean counterCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        // [parity R2] RadioTorchCounter.canBlockStay: solid side, normal block or an inventory
        BlockState supportState = level.getBlockState(support);
        if (supportState.isFaceSturdy(level, support, facing) || normalBlock(level, support, supportState)) return true;
        BlockEntity inventory = level.getBlockEntity(support);
        return inventory instanceof net.minecraft.world.Container
                || (level instanceof Level real && real.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, support, facing) != null);
    }

    private static boolean logicCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return baseCanStay(level, support, facing); // [parity R2] RadioTorchBase.canBlockStay
    }

    private static boolean readerCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        // [parity T2: ROR] TileEntityProxyCombo always implemented IRORValueProvider.
        return resolveAttachedTarget(level, support) instanceof RttyValueProvider provider
                && (provider.rttyProvidesValues() || level.getBlockEntity(support) instanceof MachineDummyBlockEntity);
    }

    private static boolean controllerCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        // [parity T2: ROR] TileEntityProxyCombo always implemented IRORInteractive.
        return resolveAttachedTarget(level, support) instanceof RttyInteractive interactive
                && (interactive.rttyInteractive() || level.getBlockEntity(support) instanceof MachineDummyBlockEntity);
    }

    private static boolean relayCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = pos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(support);
        return supportState.isFaceSturdy(level, support, facing) || normalBlock(level, support, supportState); // [parity R2] RadioTorchRelay.canBlockStay
    }

    /** RadioTorchBase.canBlockStay: isSideSolid || hasComparatorInputOverride || canProvidePower || (renderAsNormalBlock && !isAir). */
    private static boolean baseCanStay(LevelReader level, BlockPos support, Direction facing) {
        BlockState supportState = level.getBlockState(support);
        return supportState.isFaceSturdy(level, support, facing)
                || supportState.hasAnalogOutputSignal()
                || supportState.isSignalSource()
                || normalBlock(level, support, supportState);
    }

    private static boolean normalBlock(LevelReader level, BlockPos support, BlockState supportState) {
        return !supportState.isAir() && supportState.isSolidRender(level, support);
    }

    @Nullable
    private static BlockEntity resolveAttachedTarget(LevelReader level, BlockPos support) {
        BlockEntity target = level.getBlockEntity(support);
        if (target instanceof MachineDummyBlockEntity dummy) {
            BlockEntity core = dummy.core();
            // [parity T2: ROR] RBMK columns only had a proxy tile on their top block.
            // [parity U2: ROR] other multiblocks: only their 1.7.10 TileEntityProxyCombo dummies
            if (core != null && !com.atomiclegacy.rtty.RttyProxyLayouts.isProxy(core, support)) return null;
            return core;
        }
        return target;
    }

    private static VoxelShape senderShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 1.0D, 0.625D);
            case UP -> Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 0.625D, 0.625D);
            case NORTH -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 0.625D, 1.0D);
            case SOUTH -> Shapes.box(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.625D);
            case WEST -> Shapes.box(0.375D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
            case EAST -> Shapes.box(0.0D, 0.375D, 0.375D, 0.625D, 0.625D, 0.625D);
        };
    }

    private static VoxelShape receiverShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 1.0D, 0.625D);
            case UP -> Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 0.625D, 0.625D);
            case NORTH -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 0.625D, 1.0D);
            case SOUTH -> Shapes.box(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.625D);
            case WEST -> Shapes.box(0.375D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
            case EAST -> Shapes.box(0.0D, 0.375D, 0.375D, 0.625D, 0.625D, 0.625D);
        };
    }

    private static VoxelShape counterShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 1.0D, 0.625D);
            case UP -> Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 0.625D, 0.625D);
            case NORTH -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 0.625D, 1.0D);
            case SOUTH -> Shapes.box(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.625D);
            case WEST -> Shapes.box(0.375D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
            case EAST -> Shapes.box(0.0D, 0.375D, 0.375D, 0.625D, 0.625D, 0.625D);
        };
    }

    private static VoxelShape logicShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 1.0D, 0.625D);
            case UP -> Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 0.625D, 0.625D);
            case NORTH -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 0.625D, 1.0D);
            case SOUTH -> Shapes.box(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.625D);
            case WEST -> Shapes.box(0.375D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
            case EAST -> Shapes.box(0.0D, 0.375D, 0.375D, 0.625D, 0.625D, 0.625D);
        };
    }

    private static VoxelShape readerShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 1.0D, 0.625D);
            case UP -> Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 0.625D, 0.625D);
            case NORTH -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 0.625D, 1.0D);
            case SOUTH -> Shapes.box(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.625D);
            case WEST -> Shapes.box(0.375D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
            case EAST -> Shapes.box(0.0D, 0.375D, 0.375D, 0.625D, 0.625D, 0.625D);
        };
    }

    private static VoxelShape controllerShape(Direction facing) {
        return switch (facing) {
            case DOWN -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 1.0D, 0.625D);
            case UP -> Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 0.625D, 0.625D);
            case NORTH -> Shapes.box(0.375D, 0.375D, 0.375D, 0.625D, 0.625D, 1.0D);
            case SOUTH -> Shapes.box(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.625D);
            case WEST -> Shapes.box(0.375D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
            case EAST -> Shapes.box(0.0D, 0.375D, 0.375D, 0.625D, 0.625D, 0.625D);
        };
    }
}
