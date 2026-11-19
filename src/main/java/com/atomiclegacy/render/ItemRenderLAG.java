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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderLAG. */
public class ItemRenderLAG extends ItemRenderWeaponBase {

	/** ResourceManager.mike_hawk (models/weapons/mike_hawk.obj) */
	public static final LegacyEntityObjMesh mike_hawk = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/mike_hawk.obj"));
	/** ResourceManager.mike_hawk_tex */
	public static final ResourceLocation mike_hawk_tex = AtomicLegacy.id("textures/models/weapons/lag.png");

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
				-1.5F * offset, -1F * offset, 1.5F * offset,
				0, -3.375 / 8D, 0.5);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(mike_hawk_tex));
	}

	private void part(VertexConsumer vc, String name) {
		mike_hawk.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] addTrans = HbmAnimations.getRelevantTransformation("ADD_TRANS");
		double[] addRot = HbmAnimations.getRelevantTransformation("ADD_ROT");

		pose.translate(4, -4, 0);
		glRotate(pose, -equip[0], 0, 0, 1);
		pose.translate(-4, 4, 0);

		pose.translate(addTrans[0], addTrans[1], addTrans[2]);
		glRotate(pose, addRot[2], 0, 0, 1);
		glRotate(pose, addRot[1], 0, 1, 0);

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Grip");
		part(vc, "Grip");

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Slide");
		part(vc, "Slide");
		pose.popPose();

		pose.pushPose();
		pose.translate(3.125, 0.125, 0);
		glRotate(pose, -25, 0, 0, 1);
		pose.translate(-3.125, -0.125, 0);
		HbmAnimations.applyRelevantTransformation(pose, "Hammer");
		part(vc, "Hammer");
		pose.popPose();

		if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) {
			pose.pushPose();
			HbmAnimations.applyRelevantTransformation(pose, "Bullet");
			part(vc, "Bullet");
			pose.popPose();
		}

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Magazine");
		part(vc, "Magazine");
		pose.popPose();

		double smokeScale = 0.5;

		pose.pushPose();
		pose.translate(-10.25, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		pose.pushPose();
		pose.translate(-10.25, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();

		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 1, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(2.5, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0.5, -2);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		glRotate(pose, 90, 0, 1, 0);

		VertexConsumer vc = consumer();
		part(vc, "Grip");
		part(vc, "Slide");
		part(vc, "Hammer");

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
			pose.translate(-10.25, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}
