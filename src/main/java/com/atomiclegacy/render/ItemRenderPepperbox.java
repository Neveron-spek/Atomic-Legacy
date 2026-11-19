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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderPepperbox. */
public class ItemRenderPepperbox extends ItemRenderWeaponBase {

	/** ResourceManager.pepperbox (models/weapons/pepperbox.obj) */
	static final LegacyEntityObjMesh pepperbox = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/pepperbox.obj"));
	public static final ResourceLocation pepperbox_tex = AtomicLegacy.id("textures/models/weapons/pepperbox.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1.5);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.25F * offset, -0.75F * offset, 1F * offset,
				0, -2.5 / 8D, 0.5);
	}

	private void part(VertexConsumer vc, String name) {
		pepperbox.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		double scale = 0.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] cylinder = HbmAnimations.getRelevantTransformation("ROTATE");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] trigger = HbmAnimations.getRelevantTransformation("TRIGGER");
		double[] translate = HbmAnimations.getRelevantTransformation("TRANSLATE");
		double[] loader = HbmAnimations.getRelevantTransformation("LOADER");
		double[] shot = HbmAnimations.getRelevantTransformation("SHOT");

		pose.translate(translate[0], translate[1], translate[2]);

		pose.translate(0, 0, -5);
		glRotate(pose, recoil[0], -1, 0, 0);
		pose.translate(0, 0, 5);

		pose.pushPose();
		pose.translate(0, 0.5, 7);
		glRotate(pose, 90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(pepperbox_tex));

		if(loader[0] != 0 || loader[1] != 0 || loader[2] != 0) {
			pose.pushPose();
			pose.translate(loader[0], loader[1], loader[2]);
			part(vc, "Speedloader");
			if(shot[0] != 0) part(vc, "Shot");
			pose.popPose();
		}

		part(vc, "Grip");

		pose.pushPose();
		glRotate(pose, cylinder[0], 0, 0, 1);
		part(vc, "Cylinder");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.375, -1.875);
		glRotate(pose, hammer[0], 1, 0, 0);
		pose.translate(0, -0.375, 1.875);
		part(vc, "Hammer");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, -trigger[0] * 0.5);
		part(vc, "Trigger");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.5, 7);
		pose.scale(0.5F, 0.5F, 0.5F);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0]);
		glRotate(pose, 45, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0]);
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
		pose.translate(0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(pepperbox_tex));
		part(vc, "Grip");
		part(vc, "Cylinder");
		part(vc, "Hammer");
		part(vc, "Trigger");

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
			pose.translate(0, 0.5, 7);
			pose.scale(0.5F, 0.5F, 0.5F);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot);
			glRotate(pose, 45, 1, 0, 0);
			this.renderMuzzleFlash(shot);
			pose.popPose();
		}
	}
}
