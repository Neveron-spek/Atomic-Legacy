package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 Spikes: visual-only collision and falling-contact damage. */
public final class SpikeBlock extends Block {
    private static final VoxelShape EMPTY = Shapes.empty();

    public SpikeBlock(Properties properties) {
        super(properties.noOcclusion().noCollission());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return EMPTY;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide && entity instanceof LivingEntity living && entity.getDeltaMovement().y < -0.1D) {
            // [parity R4: spikes] ModDamageSource.spikes + hbm:entity.slicer when the hit lands
            if (living.hurt(level.damageSources().source(com.atomiclegacy.registry.HbmDamageTypes.SPIKES), 100.0F)) {
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        com.atomiclegacy.registry.HbmSoundEvents.ENTITY_SLICER.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
}
