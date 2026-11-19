package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderTau. */
public class ItemRenderTau extends ItemRenderWeaponBase {

	/** ResourceManager.tau (models/weapons/tau.obj) */
	static final LegacyEntityObjMesh tau = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/tau.obj"));
	public static final ResourceLocation tau_tex = AtomicLegacy.id("textures/models/weapons/tau.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.75F * offset, -1.75F * offset, 3.5F * offset,
				-1.75F * offset, -1.75F * offset, 3.5F * offset);
	}

	private void part(String name) {
		// GL_CULL_FACE is disabled around the tau model in 1.7.10
		tau.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tau_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

		pose.translate(0, -1, -4);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 4);

		pose.translate(0, 0, recoil[2]);

		pose.translate(0, 0, -2);
		glRotate(pose, recoil[2] * 5, 1, 0, 0);
		pose.translate(0, 0, 2);

		part("Body");

		pose.pushPose();
		pose.translate(0, -0.25, 0);
		glRotate(pose, rotate[2], 0, 0, 1);
		pose.translate(0, 0.25, 0);
		part("Rotor");
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 1, 2);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.25, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		tau.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tau_tex)), light, overlay, 0xFFFFFFFF);
	}
}
