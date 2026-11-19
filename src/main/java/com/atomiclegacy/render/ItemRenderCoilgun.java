package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderCoilgun. */
public class ItemRenderCoilgun extends ItemRenderWeaponBase {

	/** ResourceManager.coilgun (models/weapons/coilgun.obj) */
	static final LegacyEntityObjMesh coilgun = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/coilgun.obj"));
	public static final ResourceLocation coilgun_tex = AtomicLegacy.id("textures/models/weapons/coilgun.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.25F * offset, -1.5F * offset, 2.5F * offset,
				0, -7.5 / 8D, 1);
	}

	private void model() {
		coilgun.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(coilgun_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);

		glRotate(pose, -90, 0, 1, 0);
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		pose.translate(-1.5 - recoil[0] * 0.5, 0, 0);
		glRotate(pose, recoil[0] * 45, 0, 0, 1);
		pose.translate(1.5, 0, 0);
		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		pose.translate(-2.5, 0, 0);
		glRotate(pose, reload[0] * -45, 0, 0, 1);
		pose.translate(2.5, 0, 0);
		model();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 3D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.25, 1.25);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 4D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.25, -0.25, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -20D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.25, 0.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		glRotate(pose, -90, 0, 1, 0);
		model();
	}
}
