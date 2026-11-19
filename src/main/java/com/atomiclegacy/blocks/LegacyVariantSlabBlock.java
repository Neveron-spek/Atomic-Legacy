package com.atomiclegacy.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public class LegacyVariantSlabBlock extends SlabBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
    private final int maxVariant;
    private final LegacyVariantStrengths.Strength[] strengths;

    public LegacyVariantSlabBlock(Properties properties) {
        this(properties, 7);
    }

    public LegacyVariantSlabBlock(Properties properties, int maxVariant) {
        this(properties, maxVariant, null);
    }

    public LegacyVariantSlabBlock(Properties properties, LegacyVariantStrengths.Strength[] strengths) {
        this(properties, strengths.length - 1, strengths);
    }

    public LegacyVariantSlabBlock(Properties properties, int maxVariant, @Nullable LegacyVariantStrengths.Strength[] strengths) {
        super(properties);
        this.maxVariant = Math.max(0, Math.min(7, maxVariant));
        this.strengths = strengths;
        this.registerDefaultState(this.defaultBlockState().setValue(VARIANT, 0));
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return this.strengths == null
                ? super.getExplosionResistance(state, level, pos, explosion)
                : LegacyVariantStrengths.strengthFor(state, this.strengths).resistance();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (this.strengths == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        float hardness = LegacyVariantStrengths.strengthFor(state, this.strengths).hardness();
        if (hardness == -1.0F) {
            return 0.0F;
        }
        int divisor = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / (float) divisor;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
                                     net.minecraft.core.BlockPos neighborPos) {
        return clampVariant(super.updateShape(state, direction, neighborState, level, pos, neighborPos));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    private BlockState clampVariant(BlockState state) {
        int variant = state.getValue(VARIANT);
        return variant > maxVariant ? state.setValue(VARIANT, maxVariant) : state;
    }

    public int maxVariant() {
        return this.maxVariant;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(WATERLOGGED, false);
    }

    @Override
    public boolean canPlaceLiquid(Player player, BlockGetter level, BlockPos pos, BlockState state,
                                  net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state,
                               net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!(context.getItemInHand().getItem() instanceof com.atomiclegacy.items.LegacyVariantBlockItem item)
                || item.variantIndex(context.getItemInHand()) != state.getValue(VARIANT)) {
            return false;
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                       net.minecraft.world.entity.Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (this.strengths == LegacyVariantStrengths.CONCRETE_SLAB && state.getValue(VARIANT) == 5
                && level.isClientSide && entity instanceof Player player
                && (player.zza != 0.0F || player.xxa != 0.0F)) {
            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 1.5D, motion.y, motion.z * 1.5D);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.asItem() instanceof com.atomiclegacy.items.LegacyVariantBlockItem item) {
            ItemStack stack = com.atomiclegacy.items.LegacyVariantBlockItem.stackFor(item, state.getValue(VARIANT));
            stack.setCount(state.getValue(TYPE) == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE ? 2 : 1);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (this.asItem() instanceof com.atomiclegacy.items.LegacyVariantBlockItem item) {
            return com.atomiclegacy.items.LegacyVariantBlockItem.stackFor(item, state.getValue(VARIANT));
        }
        return super.getCloneItemStack(level, pos, state);
    }
}
