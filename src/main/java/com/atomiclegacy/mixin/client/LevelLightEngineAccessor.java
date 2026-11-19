package com.atomiclegacy.mixin.client;

import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** [parity z1] ArmorNo9 flashlight: the client block-light engine (1.7.10 World#setLightValue(EnumSkyBlock.Block)). */
@Mixin(LevelLightEngine.class)
public interface LevelLightEngineAccessor {
    @Accessor("blockEngine")
    LightEngine<?, ?> hbm$getBlockEngine();
}
