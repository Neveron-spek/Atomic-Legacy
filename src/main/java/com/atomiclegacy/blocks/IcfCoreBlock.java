package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.IcfCoreBlockEntity;
import com.atomiclegacy.tileentity.MachineInventory;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** The completed 1.7.10 ICF vessel. SOUTH is the authored model and footprint basis. */
public final class IcfCoreBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    // [parity V4: ICF wings] MachineICF.getAllDimensions: {5,0,1,1,8,8} plus {1,1,-1,2,8,8} and {1,1,2,-1,8,8} at y+3.
    public static final Footprint FOOTPRINT = withWings(Footprint.box(-8, 8, 0, 5, -1, 1));
    /** [parity V4: ICF placement] MachineICF.getOffset() = 1: the core is one block further from the player. */
    public static final int LEGACY_CORE_OFFSET = 1;
    private static final ThreadLocal<Boolean> RELOCATING = ThreadLocal.withInitial(() -> false);

    private static Footprint withWings(Footprint body) {
        java.util.ArrayList<BlockPos> wings = new java.util.ArrayList<>();
        for (int y = 2; y <= 4; y++) {
            for (int x = -8; x <= 8; x++) {
                wings.add(new BlockPos(x, y, 2));
                wings.add(new BlockPos(x, y, -2));
            }
        }
        return body.with(wings.toArray(BlockPos[]::new));
    }

    public IcfCoreBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, IcfCoreBlock::mapIcfCoreOffset);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public static BlockPos mapIcfCoreOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("ICF core requires a horizontal facing: " + facing);
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        // [parity V4: ICF placement] BlockDummyable: core at clicked + dir * -getOffset(), checkSpace on all dimensions.
        BlockPos corePos = context.getClickedPos().relative(facing, -LEGACY_CORE_OFFSET);
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                && this.canPlaceMachineFootprintAt(context.getLevel(), corePos, facing, context)
                ? this.defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (RELOCATING.get()) {
            return;
        }
        BlockPos corePos = pos;
        if (!level.isClientSide) {
            corePos = pos.relative(state.getValue(FACING), -LEGACY_CORE_OFFSET);
            RELOCATING.set(true);
            try {
                level.removeBlock(pos, false);
                level.setBlock(corePos, state, UPDATE_ALL);
            } finally {
                RELOCATING.set(false);
            }
        }
        super.setPlacedBy(level, corePos, state, placer, stack);
        refreshPorts(level, corePos, state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IcfCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.ICF_CORE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) ->
                IcfCoreBlockEntity.tick(tickerLevel, tickerPos, tickerState, (IcfCoreBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (RELOCATING.get()) {
            super.onRemove(state, level, pos, replacement, movedByPiston);
            return;
        }
        boolean removed = !state.is(replacement.getBlock());
        if (removed && !movedByPiston && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
        if (removed && !movedByPiston) {
            refreshPorts(level, pos, state.getValue(FACING));
        }
    }

    /**
     * [parity b2: old worlds] MachineICF.getAllDimensions gained the two wings (parity V4) after ICFs had been placed
     * in existing worlds. Every footprint cell that is still empty (air or replaceable) becomes a dummy of this core;
     * cells holding any other block are left alone. Returns false while part of the footprint is not loaded yet.
     */
    public static boolean completeFootprint(Level level, BlockPos corePos, BlockState state) {
        if (!(state.getBlock() instanceof IcfCoreBlock block)) {
            return true;
        }
        Direction facing = state.getValue(FACING);
        java.util.List<BlockPos> cells = block.machineOccupiedPositions(corePos, facing);
        for (BlockPos cell : cells) {
            if (!level.isLoaded(cell)) {
                return false;
            }
        }
        boolean added = false;
        for (BlockPos cell : cells) {
            if (cell.equals(corePos)) {
                continue;
            }
            BlockState existing = level.getBlockState(cell);
            if (existing.is(com.atomiclegacy.registry.HbmBlocks.MACHINE_DUMMY.get()) || !(existing.isAir() || existing.canBeReplaced())) {
                continue;
            }
            level.setBlock(cell, com.atomiclegacy.registry.HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), UPDATE_ALL);
            if (level.getBlockEntity(cell) instanceof com.atomiclegacy.tileentity.MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
            added = true;
        }
        if (added) {
            refreshPorts(level, corePos, facing);
        }
        return true;
    }

    public static void refreshPorts(Level level, BlockPos corePos, Direction facing) {
        if (level.isClientSide) {
            return;
        }
        for (IcfCoreBlockEntity.Port port : IcfCoreBlockEntity.portsFor(corePos, facing)) {
            BlockPos connector = port.proxyPos().relative(port.exposedFace());
            if (level.getBlockState(connector).getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, connector);
            }
        }
    }
}
