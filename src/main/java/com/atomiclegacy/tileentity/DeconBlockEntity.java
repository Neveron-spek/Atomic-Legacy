package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class DeconBlockEntity extends BlockEntity {
    private static final AABB EFFECT_AREA = new AABB(-0.5D, 0.0D, -0.5D, 1.5D, 2.0D, 1.5D);

    public DeconBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECON.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeconBlockEntity decon) {
        if (level.isClientSide) {
            return;
        }

        AABB area = EFFECT_AREA.move(pos.getX(), pos.getY(), pos.getZ());
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            // [parity J1] TileEntityDecon: incrementRadiation(-0.5F), removePotionEffect(radiation), getCont().clear().
            // Asbestos, black lung and burn timers are not touched by the original.
            HbmLivingRadiation radiation = HbmLivingRadiation.get(living);
            radiation.addRadiation(-0.5F);
            radiation.getContamination().clear();
            HbmLivingRadiation.set(living, radiation);
            living.removeEffect(com.atomiclegacy.registry.HbmMobEffects.RADIATION);
        }
    }

    /** [parity Q4: TileEntityDecon client branch] vanillaExt townaura, 0.125 + rand * 0.75, y + 1.1, mY 0.04. */
    public static void clientTick(Level level, BlockPos pos) {
        com.atomiclegacy.client.ParityQ4ClientParticles.townaura(
                pos.getX() + 0.125D + level.random.nextDouble() * 0.75D,
                pos.getY() + 1.1D,
                pos.getZ() + 0.125D + level.random.nextDouble() * 0.75D,
                0.0D, 0.04D, 0.0D);
    }
}
