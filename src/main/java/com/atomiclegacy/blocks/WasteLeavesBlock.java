package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WasteLeavesBlock extends LeavesBlock {
    public WasteLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) == 0) {
            level.removeBlock(pos, false);
            if (level.isEmptyBlock(pos.below())) {
                level.setBlock(pos.below(), HbmBlocks.LEAVES_LAYER.get().defaultBlockState(), 3);
            }
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    // [parity V4: dig speed] ItemShears.func_150893_a: Material.leaves -> 15 (no minecraft:leaves tag: it also feeds mineable/hoe)
    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        if (player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShearsItem) {
            return LegacyDigSpeed.progress(state, player, level, pos, 15.0F);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
