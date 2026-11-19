package com.atomiclegacy.blocks;

import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MeltdownGasBlock extends HbmGasBlock {
    public MeltdownGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        return random.nextInt(2) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(direction);
        if (random.nextInt(7) == 0 && level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
        }
        if (level.canSeeSky(pos)) {
            com.atomiclegacy.radiation.ChunkRadiationData.get(level).incrementRadiation(pos, 5); // [parity K2] incrementRad(5), no cap
        }
        if (random.nextInt(350) == 0) {
            level.removeBlock(pos, false);
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
        // [parity J1] BlockGasMeltdown: contaminate(RADIATION, CREATIVE, 0.5F), HbmPotion.radiation 60 * 20, amplifier 2.
        com.atomiclegacy.util.ContaminationUtil.contaminate(living, com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, 0.5F);
        com.atomiclegacy.util.ContaminationUtil.addRadiationEffect(living, 60 * 20, 2);
        if (!HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE, 1)) {
            HbmLivingHazards hazards = HbmLivingHazards.get(living);
            hazards.addAsbestos(living, 5);
            HbmLivingHazards.set(living, hazards);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // [parity R1] BlockGasMeltdown spawns townaura on every display tick.
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }


    @Override
    protected float[] cloudColor() { // [parity R1: ashglasses gas cloud]
        return new float[] {0.1F, 0.4F, 0.1F};
    }
}
