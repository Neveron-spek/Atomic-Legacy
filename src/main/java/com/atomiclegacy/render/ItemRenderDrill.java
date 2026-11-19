package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F3] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderDrill. */
public class ItemRenderDrill extends ItemRenderWeaponBase {

	/** ResourceManager.drill (models/weapons/drill.obj) */
	static final LegacyEntityObjMesh drill = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/drill.obj"));
	public static final ResourceLocation drill_tex = AtomicLegacy.id("textures/models/weapons/drill.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.25F * offset, -1.75F * offset, 1.75F * offset,
				-1F * offset, -1.75F * offset, 1.25F * offset);
	}

	private void part(String name) {
		drill.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(drill_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		double gauge = (double) mag.getAmount(stack, null) / (double) mag.getCapacity(stack);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] deploy = HbmAnimations.getRelevantTransformation("DEPLOY");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] spin = HbmAnimations.getRelevantTransformation("SPIN");

		glRotate(pose, 15 * (1 - deploy[0] * 0.5), 0, 1, 0);
		glRotate(pose, -10 * (1 - deploy[0] * 0.5), 1, 0, 0);

		pose.translate(0, 2, -6);
		glRotate(pose, equip[0] * -45, 0, 1, 0);
		glRotate(pose, equip[0] * -20, 1, 0, 0);
		pose.translate(0, -2, 6);

		glRotate(pose, lift[0], 1, 0, 0);

		pose.translate(0, 0, deploy[0]);

		part("Base");

		pose.pushPose();
		pose.translate(1, 2.0625, -1.75);
		glRotate(pose, 45, 1, 0, 0);
		glRotate(pose, -135 + gauge * 270, 0, 0, 1);
		glRotate(pose, -45, 1, 0, 0);
		pose.translate(-1, -2.0625, 1.75);
		part("Gauge");
		pose.popPose();

		double rot = spin[0];
		double rot2 = rot * 5;

		pose.pushPose();
		pose.translate(0, Math.sin(rot2 * Math.PI / 180) * 0.125 - 0.125, 0);
		part("Piston1");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 2D / 3D) * 0.125 - 0.125, 0);
		part("Piston2");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 4D / 3D) * 0.125 - 0.125, 0);
		part("Piston3");
		pose.popPose();

		pose.pushPose();
		glRotate(pose, rot, 0, 0, -1);
		part("DrillBack");
		pose.popPose();

		pose.pushPose();
		glRotate(pose, rot, 0, 0, 1);
		part("DrillFront");
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(1, -2, 6);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity entity) {
		drill.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(drill_tex)), light, overlay, 0xFFFFFFFF);
	}
}
