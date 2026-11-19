package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderLiberator. */
public class ItemRenderLiberator extends ItemRenderWeaponBase {

	/** ResourceManager.liberator (models/weapons/liberator.obj) */
	static final LegacyEntityObjMesh liberator = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/liberator.obj"));
	public static final ResourceLocation liberator_tex = AtomicLegacy.id("textures/models/weapons/liberator.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

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
				-1.5F * offset, -1.25F * offset, 1.25F * offset,
				0, -4.625 / 8D, 0.25);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(liberator_tex));
	}

	private void part(VertexConsumer vc, String name) {
		liberator.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] latch = HbmAnimations.getRelevantTransformation("LATCH");
		double[] brk = HbmAnimations.getRelevantTransformation("BREAK");
		double[] shell1 = HbmAnimations.getRelevantTransformation("SHELL1");
		double[] shell2 = HbmAnimations.getRelevantTransformation("SHELL2");
		double[] shell3 = HbmAnimations.getRelevantTransformation("SHELL3");
		double[] shell4 = HbmAnimations.getRelevantTransformation("SHELL4");

		pose.translate(0, -1, -3);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 3);

		pose.translate(0, -3, -3);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 3, 3);

		pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
		glRotate(pose, recoil[2] * 10, 1, 0, 0);

		part(vc, "Gun");

		pose.pushPose();

		pose.translate(0, -0.5, 0.75);
		glRotate(pose, brk[0], 1, 0, 0);
		pose.translate(0, 0.5, -0.75);
		part(vc, "Barrel");

		pose.pushPose();
		pose.translate(shell1[0], shell1[1], shell1[2]);
		part(vc, "Shell1");
		pose.popPose();
		pose.pushPose();
		pose.translate(shell2[0], shell2[1], shell2[2]);
		part(vc, "Shell2");
		pose.popPose();
		pose.pushPose();
		pose.translate(shell3[0], shell3[1], shell3[2]);
		part(vc, "Shell3");
		pose.popPose();
		pose.pushPose();
		pose.translate(shell4[0], shell4[1], shell4[2]);
		part(vc, "Shell4");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.15625, 0.75);
		glRotate(pose, latch[0], 1, 0, 0);
		pose.translate(0, -1.15625, -0.75);
		part(vc, "Latch");
		pose.popPose();
		pose.popPose();

		double smokeScale = 0.375;

		GunConfig cfg = gun.getConfig(stack, 0);

		pose.pushPose();
		pose.translate(0, 0.25, 7.25);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		pose.translate(0, 0, 0.25 / smokeScale);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		pose.translate(0, 0, -0.5 / smokeScale);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		pose.translate(0, 0.5 / smokeScale, 0);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		pose.translate(0, 0, 0.5 / smokeScale);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.5, 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(1.5F, 1.5F, 1.5F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		liberator.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);

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
			pose.translate(0, 0.5, 8);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(1.5F, 1.5F, 1.5F);
			this.renderMuzzleFlash(shot, 75, 5);
			pose.popPose();
		}
	}
}
