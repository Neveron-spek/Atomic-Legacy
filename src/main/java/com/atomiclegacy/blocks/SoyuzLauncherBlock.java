package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.tileentity.MachineInventory;
import com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * [parity P2: soyuz_launcher] Deprecated 1.7.10 SoyuzLauncher (BlockDummyable, height 4,
 * offset 0, getDirModified always EAST).  The footprint is the seven fillSpace volumes of
 * the reference resolved for EAST; the click position (y - 4) is left empty as in
 * BlockDummyable#onBlockPlacedBy.  No item drops; breakBlock refunds the building
 * materials (see SoyuzLauncherBlockEntity#dropContents).
 */
public final class SoyuzLauncherBlock extends LargeMachineBlock implements EntityBlock {
    public static final int HEIGHT = 4;
    /** SoyuzLauncher#getAllDimensions: {up, down, north, south, west, east}. */
    private static final int[][] DIMENSIONS = {
            {0, 1, 6, 6, 6, 6},
            {-2, 4, -3, 6, -3, 6},
            {-2, 4, 6, -3, -3, 6},
            {-2, 4, 6, -3, 6, -3},
            {-2, 4, -3, 6, 6, -3},
            {0, 4, 1, 1, -6, 8},
            {0, 4, 2, 2, 9, -5},
    };
    public static final Footprint FOOTPRINT = footprint();

    public SoyuzLauncherBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), (facing, offset) -> SoyuzLaunchpadBlock.mapAuthoredOffset(Direction.EAST, offset));
    }

    private static Footprint footprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        offsets.add(BlockPos.ZERO);
        for (int[] d : DIMENSIONS) {
            // MultiblockHandlerXR EAST bounds as resolved by SoyuzLaunchpadBlock#addAuthoredEastVolume
            for (int x = -d[2]; x <= d[3]; x++) {
                for (int y = -d[1]; y <= d[0]; y++) {
                    for (int z = -d[5]; z <= d[4]; z++) {
                        if (x != 0 || y != 0 || z != 0) offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return new Footprint(List.copyOf(new ArrayList<>(offsets)));
    }

    /** TileEntitySoyuzStruct: places the core (meta = EAST core) and fills the space. */
    public static void build(Level level, BlockPos core) {
        BlockState state = HbmBlocks.SOYUZ_LAUNCHER.get().defaultBlockState().setValue(FACING, Direction.EAST);
        level.setBlock(core, state, 3);
        placeDummies(level, core, Direction.EAST, FOOTPRINT,
                (facing, offset) -> SoyuzLaunchpadBlock.mapAuthoredOffset(Direction.EAST, offset));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, Direction.EAST);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoyuzLauncherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.SOYUZ_LAUNCHER.get()) return null;
        return (tickerLevel, pos, tickerState, entity) ->
                SoyuzLauncherBlockEntity.tick(tickerLevel, pos, tickerState, (SoyuzLauncherBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            com.atomiclegacy.event.LegacyMobSpawnEvents.markFbi(serverPlayer); // BossSpawnHandler.markFBI
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
