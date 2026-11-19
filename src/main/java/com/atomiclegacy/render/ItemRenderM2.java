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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderM2. */
public class ItemRenderM2 extends ItemRenderWeaponBase {

	/** ResourceManager.m2 (models/weapons/m2_browning.obj) */
	static final LegacyEntityObjMesh m2 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/m2_browning.obj"));
	public static final ResourceLocation m2_tex = AtomicLegacy.id("textures/models/weapons/m2_browning.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.5F * offset, -2.5F * offset, 1.75F * offset,
				0, -12.5 / 8D, 1.75);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(m2_tex));
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");

		pose.translate(0, 1, -2.25);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, -1, 2.25);

		pose.translate(0, 0, recoil[2]);

		pose.pushPose();
		glRotate(pose, 180, 0, 1, 0);
		m2.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);
		pose.popPose();

		double smokeScale = 0.5;

		pose.pushPose();
		pose.translate(0, 1.625, 5);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.375D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.625, 5);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.5F, 0.5F, 0.5F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0.5, -2, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.625D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0.5, -1.25, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -15D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1.5, -0.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		pose.pushPose();
		glRotate(pose, 180, 0, 1, 0);
		m2.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);
		pose.popPose();

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
			pose.translate(0, 1.625, 5);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.5F, 0.5F, 0.5F);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}
