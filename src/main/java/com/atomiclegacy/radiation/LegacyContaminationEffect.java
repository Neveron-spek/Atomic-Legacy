package com.atomiclegacy.radiation;

import net.minecraft.nbt.CompoundTag;

/**
 * [parity J1] HbmLivingProps.ContaminationEffect: a linearly decaying radiation source attached to an entity.
 * EntityEffectHandler.handleContamination applies getRad() every tick (RAD_BYPASS when ignoreArmor, otherwise
 * CREATIVE), decrements time and drops the entry at time <= 0.
 */
public final class LegacyContaminationEffect {
    public final float maxRad;
    public final int maxTime;
    public int time;
    public final boolean ignoreArmor;

    public LegacyContaminationEffect(float rad, int time, boolean ignoreArmor) {
        this.maxRad = rad;
        this.maxTime = time;
        this.time = time;
        this.ignoreArmor = ignoreArmor;
    }

    public float getRad() {
        return maxRad * ((float) time / (float) maxTime);
    }

    CompoundTag save() {
        CompoundTag me = new CompoundTag();
        me.putFloat("maxRad", maxRad);
        me.putInt("maxTime", maxTime);
        me.putInt("time", time);
        me.putBoolean("ignoreArmor", ignoreArmor);
        return me;
    }

    /**
     * 1.7.10 read maxTime/time/ignoreArmor from the parent compound instead of the entry (a load bug that turns
     * getRad() into NaN). The port reads them from the entry itself; see work/parity-j1.md.
     */
    static LegacyContaminationEffect load(CompoundTag me) {
        LegacyContaminationEffect effect = new LegacyContaminationEffect(
                me.getFloat("maxRad"), me.getInt("maxTime"), me.getBoolean("ignoreArmor"));
        effect.time = me.getInt("time");
        return effect;
    }
}
