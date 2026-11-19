package com.atomiclegacy.blocks;

import com.mojang.serialization.MapCodec;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** A standalone, non-growing HBM plant. */
public class HbmPlantBlock extends BushBlock implements BonemealableBlock {
    private static final MapCodec<HbmPlantBlock> CODEC = simpleCodec(HbmPlantBlock::new);
    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public HbmPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isPlantableSoil(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this.asItem()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this.asItem());
    }

    // [parity d2: plants] BlockNTMFlower is IGrowable for every meta: foxglove, tobacco and nightshade
    // always accept bonemeal (func_149851_a/func_149852_a == true) and func_149853_b drops one more flower.
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        popResource(level, pos, new ItemStack(this.asItem()));
    }

    static boolean hasAdjacentWater(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(below.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    static boolean isDeadOrOily(BlockState state) {
        return state.is(HbmBlocks.DIRT_DEAD.get()) || state.is(HbmBlocks.DIRT_OILY.get());
    }

    static boolean isPlantableSoil(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.FARMLAND)
                || state.is(HbmBlocks.DIRT_DEAD.get())
                || state.is(HbmBlocks.DIRT_OILY.get());
    }
}
