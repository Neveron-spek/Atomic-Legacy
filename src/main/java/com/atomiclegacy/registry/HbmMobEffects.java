package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.effect.DeathEffect;
import com.atomiclegacy.effect.BangEffect;
import com.atomiclegacy.effect.LegacyStatusEffect;
import com.atomiclegacy.effect.LeadPoisoningEffect;
import com.atomiclegacy.effect.PhosphorusBurnEffect;
import com.atomiclegacy.effect.RadawayEffect;
import com.atomiclegacy.effect.TaintEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, AtomicLegacy.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> LEAD_POISONING = MOB_EFFECTS.register(
            "lead_poisoning",
            LeadPoisoningEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> DEATH = MOB_EFFECTS.register(
            "death",
            DeathEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> BANG = MOB_EFFECTS.register(
            "bang",
            BangEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> PHOSPHORUS_BURN = MOB_EFFECTS.register(
            "phosphorus_burn",
            PhosphorusBurnEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> TAINT = MOB_EFFECTS.register(
            "taint",
            TaintEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> MUTATION = MOB_EFFECTS.register(
            "mutation",
            () -> new LegacyStatusEffect(MobEffectCategory.BENEFICIAL, 0x800080)
    );
    public static final DeferredHolder<MobEffect, MobEffect> RADX = MOB_EFFECTS.register(
            "radx",
            () -> new LegacyStatusEffect(MobEffectCategory.BENEFICIAL, 0xBB4B00)
    );
    public static final DeferredHolder<MobEffect, MobEffect> RADAWAY = MOB_EFFECTS.register(
            "radaway",
            RadawayEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> STABILITY = MOB_EFFECTS.register(
            "stability",
            () -> new LegacyStatusEffect(MobEffectCategory.BENEFICIAL, 0xD0D0D0)
    );
    public static final DeferredHolder<MobEffect, MobEffect> POTION_SICKNESS = MOB_EFFECTS.register(
            "potion_sickness",
            () -> new LegacyStatusEffect(MobEffectCategory.NEUTRAL, 0xFF8080)
    );
    // [parity J1: HbmPotion.radiation]
    public static final DeferredHolder<MobEffect, MobEffect> RADIATION = MOB_EFFECTS.register(
            "radiation",
            com.atomiclegacy.effect.RadiationEffect::new
    );
    private HbmMobEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
