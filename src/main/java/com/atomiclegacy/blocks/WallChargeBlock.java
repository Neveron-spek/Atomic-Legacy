package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.WallChargeBlockEntity;
import com.atomiclegacy.tileentity.WallChargeExplosions;
import com.atomiclegacy.items.ScrewdriverItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exact six-sided wall charge behavior from 1.7.10 BlockChargeBase. */
public final class WallChargeBlock extends BaseEntityBlock implements EntityBlock {
    private static final MapCodec<WallChargeBlock> CODEC = MapCodec.unit(() ->
            new WallChargeBlock(BlockBehaviour.Properties.of(), Kind.DYNAMITE));
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape UP = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape DOWN = Block.box(0, 10, 0, 16, 16, 16);
    private static final VoxelShape NORTH = Block.box(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 6);
    private static final VoxelShape WEST = Block.box(10, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 6, 16, 16);

    private final Kind kind;

    public WallChargeBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case DYNAMITE -> dynamiteStateForPlacement(context);
            case MINER -> minerStateForPlacement(context);
            case C4 -> c4StateForPlacement(context);
            case SEMTEX -> semtexStateForPlacement(context);
        };
    }

    private BlockState dynamiteStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private BlockState minerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private BlockState c4StateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private BlockState semtexStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case DYNAMITE -> rotateDynamite(state, rotation);
            case MINER -> rotateMiner(state, rotation);
            case C4 -> rotateC4(state, rotation);
            case SEMTEX -> rotateSemtex(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case DYNAMITE -> mirrorDynamite(state, mirror);
            case MINER -> mirrorMiner(state, mirror);
            case C4 -> mirrorC4(state, mirror);
            case SEMTEX -> mirrorSemtex(state, mirror);
        };
    }

    private static BlockState rotateDynamite(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateMiner(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateC4(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateSemtex(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState mirrorDynamite(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorMiner(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorC4(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorSemtex(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return switch (kind) {
            case DYNAMITE -> dynamiteCanSurvive(level, pos, facing);
            case MINER -> minerCanSurvive(level, pos, facing);
            case C4 -> c4CanSurvive(level, pos, facing);
            case SEMTEX -> semtexCanSurvive(level, pos, facing);
        };
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            // BlockChargeBase.onNeighborBlockChange: setBlockToAir, breakBlock then explodes.
            level.removeBlock(pos, false);
        }
    }

    /** BlockChargeBase.breakBlock: any removal that is not flagged safe detonates the charge. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && !WallChargeExplosions.safe && level instanceof ServerLevel server) {
            WallChargeExplosions.detonate(server, pos, kind);
        }
    }

    /** BlockChargeBase.addInformation and the subclass lines (hard-coded English in 1.7.10). */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.literal("Right-click to change timer.").withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.add(net.minecraft.network.chat.Component.literal("Sneak-click to arm.").withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.add(net.minecraft.network.chat.Component.literal("Can only be disarmed and removed with defuser.").withStyle(net.minecraft.ChatFormatting.RED));
        if (kind == Kind.C4) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Does not drop blocks.").withStyle(net.minecraft.ChatFormatting.BLUE));
        }
        if (kind == Kind.MINER || kind == Kind.SEMTEX) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Will drop all blocks.").withStyle(net.minecraft.ChatFormatting.BLUE));
            tooltip.add(net.minecraft.network.chat.Component.literal("Does not do damage.").withStyle(net.minecraft.ChatFormatting.BLUE));
        }
        if (kind == Kind.SEMTEX) {
            tooltip.add(net.minecraft.network.chat.Component.literal("").withStyle(net.minecraft.ChatFormatting.BLUE));
            tooltip.add(net.minecraft.network.chat.Component.literal("Fortune III").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.Explosion explosion) {
        if (!level.isClientSide && level instanceof ServerLevel server) {
            // Block.onBlockExploded: setBlockToAir (breakBlock explodes at once), then
            // BlockChargeBase.onBlockDestroyedByExplosion spawns a fuse-0 primed entity that explodes again.
            level.removeBlock(pos, false);
            com.atomiclegacy.explosion.LegacyDeferredDetonations.defer(server,
                    () -> WallChargeExplosions.detonate(server, pos, kind));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (kind) {
            case DYNAMITE -> dynamiteShape(facing);
            case MINER -> minerShape(facing);
            case C4 -> c4Shape(facing);
            case SEMTEX -> semtexShape(facing);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallChargeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.WALL_CHARGE.get()
                ? (tickerLevel, pos, tickerState, blockEntity) ->
                WallChargeBlockEntity.tick(tickerLevel, pos, tickerState, (WallChargeBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide) {
                WallChargeBlockEntity charge = charge(level, pos);
                if (charge != null && charge.started()) {
                    charge.disarm();
                } else {
                    // BlockBase.dismantle with BlockChargeBase.safe set.
                    WallChargeExplosions.safe = true;
                    try {
                        level.removeBlock(pos, false);
                    } finally {
                        WallChargeExplosions.safe = false;
                    }
                    net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level,
                            pos.getX() + level.random.nextFloat() * 0.6F + 0.2F,
                            pos.getY() + level.random.nextFloat() * 0.2F + 1.0D,
                            pos.getZ() + level.random.nextFloat() * 0.6F + 0.2F,
                            new ItemStack(this));
                    item.setDeltaMovement(level.random.nextGaussian() * 0.05D,
                            level.random.nextGaussian() * 0.05D + 0.2D,
                            level.random.nextGaussian() * 0.05D);
                    level.addFreshEntity(item);
                }
                // ItemTooling.onItemUse: a successful onScrew costs one durability.
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                        ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                        : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hitResult) {
        WallChargeBlockEntity charge = charge(level, pos);
        if (charge == null || charge.started()) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                if (charge.timer() > 0) {
                    charge.arm(charge.timer());
                    level.playSound(null, pos, HbmSoundEvents.CHARGE_START.get(),
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                charge.cycleTimer();
                level.playSound(null, pos, HbmSoundEvents.TECH_BOOP.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return List.of();
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private WallChargeBlockEntity charge(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WallChargeBlockEntity charge ? charge : null;
    }

    private static boolean dynamiteCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static boolean minerCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static boolean c4CanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static boolean semtexCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static VoxelShape dynamiteShape(Direction facing) {
        return switch (facing) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private static VoxelShape minerShape(Direction facing) {
        return switch (facing) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private static VoxelShape c4Shape(Direction facing) {
        return switch (facing) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private static VoxelShape semtexShape(Direction facing) {
        return switch (facing) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public enum Kind {
        DYNAMITE("charge_dynamite", 4.0F, true, false),
        MINER("charge_miner", 4.0F, true, true),
        C4("charge_c4", 15.0F, false, false),
        SEMTEX("charge_semtex", 10.0F, true, false);

        private final String id;
        private final float radius;
        private final boolean drops;
        private final boolean noHurt;

        Kind(String id, float radius, boolean drops, boolean noHurt) {
            this.id = id;
            this.radius = radius;
            this.drops = drops;
            this.noHurt = noHurt;
        }

        public String id() { return id; }
        public float radius() { return radius; }
        public boolean drops() { return drops; }
        public boolean noHurt() { return noHurt; }
    }
}
