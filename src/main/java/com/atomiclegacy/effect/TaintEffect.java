package com.atomiclegacy.effect;

import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** The 1.7.10 taint potion: periodic damage and optional residue trails are kept as one effect. */
public class TaintEffect extends MobEffect {
    public TaintEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        // [parity H1] HbmPotion: EntityCreeperTainted and EntityTaintCrab take no taint damage.
        if (!living.level().isClientSide
                && !(living instanceof com.atomiclegacy.entity.LegacyTaintedCreeperEntity)
                && !(living instanceof com.atomiclegacy.entity.LegacyTaintCrabEntity)
                && living.getRandom().nextInt(40) == 0) {
            living.hurt(living.damageSources().source(HbmDamageTypes.TAINT), amplifier + 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }
}
