package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

/**
 * [parity U1: network_separator] BlockNetworkSeparator (BlockDummyable, dimensions {1, 0, 0, 0, 1, 1}): the core,
 * one dummy on each side along the rot(UP)/rot(DOWN) axis and the row above; the two side blocks are the in and
 * out power ports (getAllPorts). Right click without sneaking opens GUIDiode.
 */
public class NetworkSeparatorBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 1, 0, 0);

    public NetworkSeparatorBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), NetworkSeparatorBlock::mapOffset);
    }

    /** authored x runs along the in-port direction (facing.getClockWise()) */
    private static BlockPos mapOffset(Direction facing, BlockPos offset) {
        Direction axis = facing.getClockWise();
        return new BlockPos(axis.getStepX() * offset.getX(), offset.getY(), axis.getStepZ() * offset.getX());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkSeparatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.NETWORK_SEPARATOR.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                NetworkSeparatorBlockEntity.serverTick(tickerLevel, pos, tickerState, (NetworkSeparatorBlockEntity) blockEntity);
    }

    /** BlockNetworkSeparator#onBlockActivated, also reached from the dummies */
    public static InteractionResult openOnCore(Level level, BlockPos corePos, Player player) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(corePos) instanceof NetworkSeparatorBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            com.atomiclegacy.client.NetworkSeparatorClientHooks.open(corePos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = openOnCore(level, pos, player);
        return result.consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openOnCore(level, pos, player);
    }
}
