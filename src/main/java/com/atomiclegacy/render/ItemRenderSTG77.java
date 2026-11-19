package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderSTG77. */
public class ItemRenderSTG77 extends ItemRenderWeaponBase {

	/** ResourceManager.stg77 (models/weapons/stg77.obj) */
	public static final LegacyEntityObjMesh stg77 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/stg77.obj"));
	public static final ResourceLocation stg77_tex = AtomicLegacy.id("textures/models/weapons/stg77.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.5F * offset, -1F * offset, 2.5F * offset,
			0, -5.75 / 8D, 2);
	}

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.66F);
	}

	@Override
	protected float getBaseFOV(ItemStack stack) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return 70F - aimingProgress * 65;
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(stg77_tex));
	}

	private void part(VertexConsumer vc, String name) {
		stg77.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		if(ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] safety = HbmAnimations.getRelevantTransformation("SAFETY");

		double[] inspectGun = HbmAnimations.getRelevantTransformation("INSPECT_GUN");
		double[] inspectBarrel = HbmAnimations.getRelevantTransformation("INSPECT_BARREL");
		double[] inspectMove = HbmAnimations.getRelevantTransformation("INSPECT_MOVE");
		double[] inspectLever = HbmAnimations.getRelevantTransformation("INSPECT_LEVER");

		pose.translate(0, -1, -4);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 4);

		pose.translate(0, 0, -4);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, recoil[2]);

		pose.pushPose();

		glRotate(pose, inspectGun[2], 0, 0, 1);
		glRotate(pose, inspectGun[0], 1, 0, 0);

		HbmAnimations.applyRelevantTransformation(pose, "Gun");
		part(vc, "Gun");

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Magazine");
		part(vc, "Magazine");
		pose.popPose();

		pose.pushPose();
		glRotate(pose, inspectLever[2], 0, 0, 1);
		HbmAnimations.applyRelevantTransformation(pose, "Lever");
		part(vc, "Lever");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, bolt[2]);
		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Breech");
		part(vc, "Breech");
		pose.popPose();
		pose.translate(0.125, 0, 0);
		glRotate(pose, handle[2], 0, 0, 1);
		pose.translate(-0.125, 0, 0);
		HbmAnimations.applyRelevantTransformation(pose, "Handle");
		part(vc, "Handle");
		pose.popPose();

		pose.pushPose();
		pose.translate(safety[0], 0, 0);
		HbmAnimations.applyRelevantTransformation(pose, "Safety");
		part(vc, "Safety");
		pose.popPose();

		pose.popPose();

		pose.pushPose();
		pose.translate(inspectMove[0], inspectMove[1], inspectMove[2]);
		glRotate(pose, inspectBarrel[0], 1, 0, 0);
		glRotate(pose, inspectBarrel[2], 0, 0, 1);
		HbmAnimations.applyRelevantTransformation(pose, "Gun");
		HbmAnimations.applyRelevantTransformation(pose, "Barrel");
		part(vc, "Barrel");
		pose.popPose();

		double smokeScale = 0.75;

		pose.pushPose();
		pose.translate(0, 0, 8);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 7.5);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale(0.25F, 0.25F, 0.25F);
		glRotate(pose, -5 + gun.shotRand * 10, 1, 0, 0);
		this.renderGapFlash(gun.lastShot[0]);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 1, 2);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Barrel");
		part(vc, "Lever");
		part(vc, "Magazine");
		part(vc, "Safety");
		part(vc, "Handle");
		part(vc, "Breech");

		if(type == LegacyRenderType.EQUIPPED && ent != null) {
			long shot;
			double shotRand = 0;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
				shotRand = gun.shotRand;
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			pose.pushPose();
			pose.translate(0, 0, 7.5);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale(0.25F, 0.25F, 0.25F);
			glRotate(pose, -5 + shotRand * 10, 1, 0, 0);
			this.renderGapFlash(shot);
			pose.popPose();
		}
	}
}
