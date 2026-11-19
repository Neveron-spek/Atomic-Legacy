package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity;
import com.atomiclegacy.tileentity.MachineInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * [parity d2] MachineDiFurnace, MachineDiFurnaceRTG and MachineRtgFurnace of 1.7.10 (all @Deprecated, still registered).
 * As with the brick furnace the off/on pair shares one class; the working state is the LIT property. COVERED mirrors
 * MachineDiFurnace#getIcon(IBlockAccess...): the blast furnace shows its tall textures below an extension.
 */
public final class LegacyDeprecatedFurnaceBlock extends Block implements EntityBlock {
    public enum Kind {
        DIFURNACE(4), DIFURNACE_RTG(9), RTG_FURNACE(5);

        private final int slots;

        Kind(int slots) {
            this.slots = slots;
        }

        public int slots() {
            return this.slots;
        }
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty COVERED = BooleanProperty.create("covered");

    /** StateDefinition is built inside the Block constructor, before the kind field can be assigned. */
    private static final ThreadLocal<Kind> CONSTRUCTING = new ThreadLocal<>();
    private final Kind kind;

    public LegacyDeprecatedFurnaceBlock(Kind kind, Properties properties) {
        super(prepare(kind, properties));
        CONSTRUCTING.remove();
        this.kind = kind;
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(LIT, false);
        registerDefaultState(kind == Kind.DIFURNACE ? state.setValue(COVERED, false) : state);
    }

    private static Properties prepare(Kind kind, Properties properties) {
        CONSTRUCTING.set(kind);
        return properties;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        if (CONSTRUCTING.get() == Kind.DIFURNACE) {
            builder.add(COVERED);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (this.kind == Kind.DIFURNACE) {
            state = state.setValue(COVERED, isExtension(context.getLevel().getBlockState(context.getClickedPos().above())));
        }
        return state;
    }

    private static boolean isExtension(BlockState state) {
        return state.is(HbmBlocks.MACHINE_DIFURNACE_EXTENSION.get());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (this.kind == Kind.DIFURNACE && direction == Direction.UP) {
            return state.setValue(COVERED, isExtension(neighborState));
        }
        return state;
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyDeprecatedFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.LEGACY_DEPRECATED_FURNACE.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof LegacyDeprecatedFurnaceBlockEntity furnace) {
                LegacyDeprecatedFurnaceBlockEntity.tick(tickLevel, tickPos, tickState, furnace);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return open(level, pos, player);
    }

    public static InteractionResult open(Level level, BlockPos pos, Player player) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof LegacyDeprecatedFurnaceBlockEntity furnace && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu((MenuProvider) furnace, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(furnace.kind().ordinal());
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(switch (this.kind) {
            case DIFURNACE -> HbmBlocks.MACHINE_DIFURNACE_OFF.get();
            case DIFURNACE_RTG -> HbmBlocks.MACHINE_DIFURNACE_RTG_OFF.get();
            case RTG_FURNACE -> HbmBlocks.MACHINE_RTG_FURNACE_OFF.get();
        });
    }

    /** randomDisplayTick of the three 1.7.10 classes; the random draws keep their original order. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        float x0 = x + 0.5F;
        float y0 = y + 0.25F + rand.nextFloat() * 6.0F / 16.0F;
        float z0 = z + 0.5F;
        float sideOff = 0.52F;
        float sideRand = this.kind == Kind.RTG_FURNACE ? rand.nextFloat() * 0.6F - 0.3F : rand.nextFloat() * 0.5F - 0.25F;
        float xOff;
        float zOff;
        if (this.kind == Kind.DIFURNACE) {
            xOff = rand.nextFloat() * 0.375F + 0.3125F;
            zOff = rand.nextFloat() * 0.375F + 0.3125F;
            if (state.getValue(COVERED)) {
                y += 1;
            }
        } else if (this.kind == Kind.DIFURNACE_RTG) {
            xOff = rand.nextFloat() * 0.75F + 0.125F;
            zOff = rand.nextFloat() * 0.75F + 0.125F;
        } else {
            rand.nextFloat();
            rand.nextFloat();
            xOff = 0.0F;
            zOff = 0.0F;
        }
        double fx = switch (facing) {
            case WEST -> x0 - sideOff;
            case EAST -> x0 + sideOff;
            default -> x0 + sideRand;
        };
        double fz = switch (facing) {
            case NORTH -> z0 - sideOff;
            case SOUTH -> z0 + sideOff;
            default -> z0 + sideRand;
        };
        if (this.kind == Kind.RTG_FURNACE) {
            level.addParticle(ParticleTypes.SMOKE, fx, y0, fz, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.FLAME, fx, y0, fz, 0.0D, 0.0D, 0.0D);
        } else {
            level.addParticle(ParticleTypes.FLAME, fx, y0, fz, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x + xOff, y + 1, z + zOff, 0.0D, 0.0D, 0.0D);
        }
    }
}
