package com.atomiclegacy.mixin.client;

import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** [parity z1] ArmorNo9 flashlight: read / write a stored block-light level like 1.7.10 Chunk#setLightValue. */
@Mixin(LayerLightSectionStorage.class)
public interface LayerLightSectionStorageInvoker {
    @Invoker("storingLightForSection")
    boolean hbm$storingLightForSection(long sectionPos);

    @Invoker("getStoredLevel")
    int hbm$getStoredLevel(long levelPos);

    @Invoker("setStoredLevel")
    void hbm$setStoredLevel(long levelPos, int lightLevel);
}
