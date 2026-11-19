package com.atomiclegacy.blocks;

import com.atomiclegacy.inventory.gui.K3GuiText;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.tileentity.LegacyBatteryBlockEntity;
import com.atomiclegacy.tileentity.MachineInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** [parity d2] MachineBattery of 1.7.10 (@Deprecated, creative tab null): the five legacy energy storage blocks. */
public final class LegacyBatteryBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final long maxPower;

    public LegacyBatteryBlock(long maxPower, Properties properties) {
        super(properties);
        this.maxPower = maxPower;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public long maxPower() {
        return this.maxPower;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return new LegacyBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.LEGACY_BATTERY.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                LegacyBatteryBlockEntity.tick(tickLevel, tickPos, tickState, (LegacyBatteryBlockEntity) blockEntity);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LegacyBatteryBlockEntity battery) {
            battery.loadFromItem(stack);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** IPersistentNBT.getDrops: the dropped block keeps power, modes and priority. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof LegacyBatteryBlockEntity battery) {
            ItemStack stack = new ItemStack(this);
            battery.saveToItem(stack);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            com.atomiclegacy.power.PowerNetworkManager.markDirty(level); // [parity h1] battery joins cable networks
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        if (!state.is(newState.getBlock())) {
            com.atomiclegacy.power.PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LegacyBatteryBlockEntity battery ? battery.getComparatorPower() : 0;
    }

    /** MachineBattery#addInformation; ItemBlockBase only calls it for stacks that carry the persistent tag. */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || !custom.contains(LegacyBatteryBlockEntity.NBT_PERSISTENT_KEY)) {
            return;
        }
        CompoundTag data = custom.copyTag().getCompound(LegacyBatteryBlockEntity.NBT_PERSISTENT_KEY);
        tooltip.add(Component.literal("Stores up to " + K3GuiText.shortNumber(this.maxPower) + "HE").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Charge speed: " + K3GuiText.shortNumber(this.maxPower / 200) + "HE").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Discharge speed: " + K3GuiText.shortNumber(this.maxPower / 600) + "HE").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(K3GuiText.shortNumber(data.getLong("power")) + "/" + K3GuiText.shortNumber(this.maxPower) + "HE").withStyle(ChatFormatting.YELLOW));
    }
}
