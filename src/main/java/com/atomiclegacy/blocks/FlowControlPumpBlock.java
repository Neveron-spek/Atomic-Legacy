package com.atomiclegacy.blocks;

import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * [parity T1: flow_control_pump] FlowControlPump (BlockDummyable, dimensions {0, 0, 0, 0, 1, 1}): the core
 * and one dummy on each side along the rot(UP)/rot(DOWN) axis; both end blocks are fluid ports.
 */
public class FlowControlPumpBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 0, 0, 0);

    public FlowControlPumpBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), FlowControlPumpBlock::mapOffset);
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
        return new FlowControlPumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.FLOW_CONTROL_PUMP.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (tickerLevel, pos, tickerState, blockEntity) ->
                    FlowControlPumpBlockEntity.clientTick(tickerLevel, pos, tickerState, (FlowControlPumpBlockEntity) blockEntity);
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                FlowControlPumpBlockEntity.serverTick(tickerLevel, pos, tickerState, (FlowControlPumpBlockEntity) blockEntity);
    }

    /** FlowControlPump#onBlockActivated, also reached from the dummies */
    public static ItemInteractionResult useOnCore(ItemStack stack, Level level, BlockPos corePos, Player player) {
        if (!(level.getBlockEntity(corePos) instanceof FlowControlPumpBlockEntity pump)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!level.isClientSide) {
                var type = FluidIdentifierItem.primary(stack);
                pump.setType(type);
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(type.translationKey()))
                        .append(Component.literal("!")));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult openOnCore(Level level, BlockPos corePos, Player player) {
        if (!(level.getBlockEntity(corePos) instanceof FlowControlPumpBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            com.atomiclegacy.client.FlowControlPumpClientHooks.open(corePos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        return useOnCore(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openOnCore(level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            level.invalidateCapabilities(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
