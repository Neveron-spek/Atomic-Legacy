package com.atomiclegacy.blocks;

import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.config.CustomMachineConfigJSON.MachineConfiguration;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.CustomMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity X1: custom machines] BlockCustomMachine (HbmMods 936c60c): light 1F, the configuration comes from the item,
 * right click checks the structure and opens the GUI or builds the structure with the structure wand (wand_s),
 * drops one controller of its configuration and never the six template slots.
 */
public final class CustomMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CustomMachineBlock(Properties properties) {
        super(properties.lightLevel(state -> 15));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** onBlockPlacedBy: the front (meta 2..5) faces the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile) {
            MachineConfiguration config = CustomMachineConfigJSON.configFor(stack);
            if (config != null) {
                tile.machineType = config.unlocalizedName;
                tile.init();
                tile.setChanged();
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.CUSTOM_MACHINE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                CustomMachineBlockEntity.serverTick(tickerLevel, pos, tickerState, (CustomMachineBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile) {
                if (tile.checkStructure()) {
                    if (tile.config != null && player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(tile, buffer -> {
                            buffer.writeBlockPos(pos);
                            buffer.writeUtf(tile.machineType);
                        });
                    }
                } else if (player.getMainHandItem().is(HbmItems.STRUCTURE_WAND.get())) {
                    tile.buildStructure();
                }
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** getDrops: ItemStack(item, 1, niceList.indexOf(tile.config) + 100) - an unknown configuration is the invalid controller. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> ret = new ArrayList<>();
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof CustomMachineBlockEntity tile) {
            ret.add(tile.config != null ? CustomMachineConfigJSON.stackFor(tile.config) : new ItemStack(this));
        }
        return ret;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile
                && tile.machineType != null && !tile.machineType.isEmpty()) {
            return tile.config != null ? CustomMachineConfigJSON.stackFor(tile.config) : new ItemStack(this);
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    /** breakBlock: every slot except the filters 10 - 15 is dropped. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile) {
                for (int i1 = 0; i1 < tile.getContainerSize(); ++i1) {
                    if (i1 >= 10 && i1 <= 15) continue; // do NOT drop the filters
                    ItemStack itemstack = tile.getItem(i1);
                    if (!itemstack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemstack.copy());
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
