package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The two 1.7.10 enchanted enargite bricks keep their contact effects. */
public final class EnargiteBrickBlock extends Block {
    private final Kind kind;

    public EnargiteBrickBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    // [parity J1] BlockOre.onEntityWalking: brick_jungle_ooze radiation 15 * 20 amplifier 9,
    // brick_jungle_mystic taint 15 * 20 amplifier 2.
    @Override
    public void stepOn(Level level, net.minecraft.core.BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!(entity instanceof LivingEntity living) || !LegacyWalkingTrigger.onEntityWalking(entity)) {
            return;
        }
        if (kind == Kind.RADIOACTIVE) {
            com.atomiclegacy.util.ContaminationUtil.addRadiationEffect(living, 15 * 20, 9);
        } else {
            living.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 15 * 20, 2));
        }
    }

    public enum Kind {
        RADIOACTIVE,
        MYSTIC
    }
}
