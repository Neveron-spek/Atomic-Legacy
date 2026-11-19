package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.SupplyCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public final class SupplyCrateBlock extends Block implements EntityBlock {
    public SupplyCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyCrateBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate) {
            crate.loadFromItem(stack, level.registryAccess());
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player player)) {
            return List.of(new ItemStack(this)); // [parity R4: loot crates] default getItemDropped (explosions etc.), contents lost
        }
        if (player.getAbilities().instabuild) {
            return List.of();
        }
        ItemStack drop = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SupplyCrateBlockEntity crate) {
            crate.saveToItem(drop, params.getLevel().registryAccess());
        }
        return List.of(drop);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!CrateBlockSupport.isCrowbar(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            List<ItemStack> contents = level.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate
                    ? crate.takeContents()
                    : List.of();
            CrateBlockSupport.openDropAsItem(level, pos, contents, true); // [parity R4: loot crates] dropBlockAsItem + func_147480_a
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
