package com.atomiclegacy.effect;

import com.atomiclegacy.util.ContaminationUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * [parity J1] HbmPotion.radiation ("potion.hbm_radiation", 0x84C128, bad effect, icon 1,0 of potions.png):
 * performEffect every tick (isReady always true) calls
 * ContaminationUtil.contaminate(entity, RADIATION, CREATIVE, (level + 1) * 0.05F) on the server.
 */
public class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x84C128);
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        if (!living.level().isClientSide) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE, (amplifier + 1.0F) * 0.05F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
