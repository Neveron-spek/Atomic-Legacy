package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity b1: smoldering ore] 1.7.10 {@code BlockSmolder} (ore_nether_smoldering): one "lava" and one "flame"
 * particle per display tick above the block while the block above is air, and entities walking on it are set on
 * fire for 3 seconds. The powder_fire drop stays in the loot table.
 */
public class SmolderingOreBlock extends Block {
    public SmolderingOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.25D + random.nextDouble() * 0.5D, pos.getY() + 1.1D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() * 0.5D, pos.getY() + 1.1D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3.0F);
        super.stepOn(level, pos, state, entity);
    }
}
