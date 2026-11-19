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

/** [parity d2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderDebug (gun_debug: lilmac model, debug_gun_tex). */
public class ItemRenderDebug extends ItemRenderWeaponBase {

	/** ResourceManager.lilmac (models/weapons/lilmac.obj) */
	private static final LegacyEntityObjMesh lilmac = ItemRenderHeavyRevolver.lilmac;
	/** ResourceManager.debug_gun_tex */
	public static final ResourceLocation debug_gun_tex = AtomicLegacy.id("textures/models/weapons/debug_gun.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.0F * offset, -0.75F * offset, 1F * offset,
				0, -3.875 / 8D, 0);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(debug_gun_tex));
	}

	private void part(VertexConsumer vc, String name) {
		lilmac.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);

		double[] equipSpin = HbmAnimations.getRelevantTransformation("ROTATE");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadLift = HbmAnimations.getRelevantTransformation("RELOAD_LIFT");
		double[] reloadJolt = HbmAnimations.getRelevantTransformation("RELOAD_JOLT");
		double[] reloadTilt = HbmAnimations.getRelevantTransformation("RELAOD_TILT");
		double[] cylinderFlip = HbmAnimations.getRelevantTransformation("RELOAD_CYLINDER");
		double[] reloadBullets = HbmAnimations.getRelevantTransformation("RELOAD_BULLETS");

		glRotate(pose, equipSpin[0], 0, 0, 1);

		standardAimingTransform(pose, stack, 0, 0, recoil[2], -recoil[2], 0, 0);
		glRotate(pose, recoil[2] * 10, 0, 0, 1);

		pose.pushPose();
		pose.translate(-9, 2.5, 0);
		glRotate(pose, recoil[2] * -10, 0, 0, 1);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		glRotate(pose, reloadLift[0], 0, 0, 1);
		pose.translate(reloadJolt[0], 0, 0);
		glRotate(pose, reloadTilt[0], 1, 0, 0);

		// fetched after the smoke nodes: a buffer source without fixed buffers ends the previous batch on a type switch
		VertexConsumer vc = consumer();
		part(vc, "Gun");

		pose.pushPose();
		glRotate(pose, cylinderFlip[0], 1, 0, 0);
		part(vc, "Pivot");
		pose.translate(0, 1.75, 0);
		glRotate(pose, HbmAnimations.getRelevantTransformation("DRUM")[2] * -60, 1, 0, 0);
		pose.translate(0, -1.75, 0);
		part(vc, "Cylinder");
		pose.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
		if(HbmAnimations.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1)
			part(vc, "Bullets");
		part(vc, "Casings");
		pose.popPose();

		pose.pushPose(); /// HAMMER ///
		pose.translate(4, 1.25, 0);
		glRotate(pose, -30 + 30 * HbmAnimations.getRelevantTransformation("HAMMER")[2], 0, 0, 1);
		pose.translate(-4, -1.25, 0);
		part(vc, "Hammer");
		pose.popPose();

		pose.pushPose();
		pose.translate(0.125, 2.5, 0);
		this.renderGapFlash(gun.lastShot[0]);
		pose.popPose();

		// the muzzle flash call of the original is commented out (only its transform remains)
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.scale(0.75F, 0.75F, 0.75F);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		glRotate(pose, 90, 0, 1, 0);

		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Cylinder");
		part(vc, "Bullets");
		part(vc, "Casings");
		part(vc, "Pivot");
		part(vc, "Hammer");

		if(type == LegacyRenderType.EQUIPPED && ent != null) {
			long shot;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			pose.pushPose();
			pose.translate(0.125, 2.5, 0);
			this.renderGapFlash(shot);
			pose.popPose();
		}
	}
}
