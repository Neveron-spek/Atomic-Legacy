package com.atomiclegacy.mixin.parity;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** [parity V4: step sounds] Lets LegacyStepSound apply the 1.7.10 stepSound table once, at block construction. */
@Mixin(BlockBehaviour.class)
public interface BlockBehaviourSoundAccessor {
    @Mutable
    @Accessor("soundType")
    void atomiclegacy$setSoundType(SoundType soundType);
}
