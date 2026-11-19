package com.atomiclegacy.blocks;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DenseRadonGasBlock extends HbmGasBlock {
    public DenseRadonGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        return random.nextInt(5) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            Block wasteEarth = BuiltInRegistries.BLOCK.get(AtomicLegacy.id("waste_earth"));
            if (wasteEarth != Blocks.AIR) {
                level.setBlock(pos.below(), wasteEarth.defaultBlockState(), 3);
            }
        }
        if (random.nextInt(30) == 0) {
            level.removeBlock(pos, false);
            Block fallout = BuiltInRegistries.BLOCK.get(AtomicLegacy.id("fallout"));
            if (fallout != Blocks.AIR && fallout.defaultBlockState().canSurvive(level, pos)) {
                level.setBlock(pos, fallout.defaultBlockState(), 3);
            }
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE, 1)) {
            return;
        }
        // [parity J1] BlockGasRadonDense: contaminate(RADIATION, CREATIVE, 0.5F), HbmPotion.radiation 15 * 20,
        // amplifier 0, then incrementAsbestos(5) (unconditional, like the original).
        com.atomiclegacy.util.ContaminationUtil.contaminate(living, com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, 0.5F);
        com.atomiclegacy.util.ContaminationUtil.addRadiationEffect(living, 15 * 20, 0);
        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        hazards.addAsbestos(living, 5);
        HbmLivingHazards.set(living, hazards);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    @Override
    protected float[] cloudColor() { // [parity R1: ashglasses gas cloud]
        return new float[] {0.1F, 0.5F, 0.1F};
    }
}
