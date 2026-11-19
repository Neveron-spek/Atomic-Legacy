package com.atomiclegacy.mixin.parity;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [parity P3] 1.7.10 SharedMonsterAttributes.maxHealth ranged up to Double.MAX_VALUE; 1.21 caps generic.max_health at
 * 1024. EntityUFO (20000) and EntityBOTPrimeBase (15000) need the uncapped value. Only values above 1024 change.
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMaxHealthMixin {
    @Shadow @Final private double minValue;

    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void atomiclegacy$uncapMaxHealth(double value, CallbackInfoReturnable<Double> cir) {
        if ((Object) this == Attributes.MAX_HEALTH.value()) {
            cir.setReturnValue(Double.isNaN(value) ? this.minValue : Math.max(this.minValue, value));
        }
    }
}
