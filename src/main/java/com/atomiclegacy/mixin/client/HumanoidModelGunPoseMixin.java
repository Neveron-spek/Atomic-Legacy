package com.atomiclegacy.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atomiclegacy.client.gun.GunAkimboRenderHandler;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/** ModEventHandlerRenderer akimbo arm yaw, applied after the vanilla arm poses (see GunAkimboRenderHandler). */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelGunPoseMixin {

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void atomiclegacy$gunArmPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		GunAkimboRenderHandler.applyArmPose((HumanoidModel<?>) (Object) this, entity);
	}
}
