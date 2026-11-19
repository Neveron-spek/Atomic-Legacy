package com.atomiclegacy.mixin.client;

import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** [parity z1] ArmorNo9 flashlight: the light engine's section storage. */
@Mixin(LightEngine.class)
public interface LightEngineAccessor {
    @Accessor("storage")
    LayerLightSectionStorage<?> hbm$getStorage();
}
