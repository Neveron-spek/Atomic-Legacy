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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderAtlas (gun_light_revolver, gun_light_revolver_atlas). */
public class ItemRenderAtlas extends ItemRenderWeaponBase {

	/** ResourceManager.bio_revolver (models/weapons/bio_revolver.obj) */
	static final LegacyEntityObjMesh bio_revolver = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/bio_revolver.obj"));
	public static final ResourceLocation bio_revolver_tex = AtomicLegacy.id("textures/models/weapons/bio_revolver.png");
	public static final ResourceLocation bio_revolver_atlas_tex = AtomicLegacy.id("textures/models/weapons/bio_revolver_atlas.png");

	public ResourceLocation texture;

	public ItemRenderAtlas(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1.0F * offset, -0.75F * offset, 1F * offset,
				0, -3.125 / 8D, 0.25);
	}

	private void part(VertexConsumer vc, String name) {
		bio_revolver.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE");
		double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT");
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

		pose.translate(recoil[0], recoil[1], recoil[2]);
		glRotate(pose, recoil[2] * 10, 1, 0, 0);

		pose.translate(0, 0, -7);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, 0, 7);

		pose.pushPose();
		pose.translate(0, 1.5, 9.25);
		glRotate(pose, -recoil[2] * 10, 1, 0, 0);
		glRotate(pose, 90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		// the cutout buffer is fetched after the smoke (BufferSource ends a shared buffer on switch)
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(texture));
		pose.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

		glRotate(pose, reloadRot[0], 1, 0, 0);
		glRotate(pose, reloadRot[2], 0, 0, 1);
		glRotate(pose, reloadRot[1], 0, 1, 0);
		part(vc, "Grip");

		pose.pushPose(); /// FRONT PUSH ///
		glRotate(pose, HbmAnimations.getRelevantTransformation("FRONT")[2], 1, 0, 0);
		part(vc, "Barrel");
		pose.pushPose(); /// LATCH PUSH ///
		pose.translate(0, 2.3125, -0.875);
		glRotate(pose, HbmAnimations.getRelevantTransformation("LATCH")[2], 1, 0, 0);
		pose.translate(0, -2.3125, 0.875);
		part(vc, "Latch");
		pose.popPose(); /// LATCH POP ///

		pose.pushPose(); /// DRUM PUSH ///
		pose.translate(0, 1, 0);
		glRotate(pose, HbmAnimations.getRelevantTransformation("DRUM")[2] * 60, 0, 0, 1);
		pose.translate(0, -1, 0);
		pose.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH")[2]);
		part(vc, "Drum");
		pose.popPose(); /// DRUM POP ///

		pose.popPose(); /// FRONT POP ///

		pose.pushPose(); /// HAMMER ///
		pose.translate(0, 0, -4.5);
		glRotate(pose, -45 + 45 * HbmAnimations.getRelevantTransformation("HAMMER")[2], 1, 0, 0);
		pose.translate(0, 0, 4.5);
		part(vc, "Hammer");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.5, 9.25);
		glRotate(pose, 90, 0, 1, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.125D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 1.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 1.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		bio_revolver.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);

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
			pose.translate(0, 1.5, 9.25);
			glRotate(pose, 90, 0, 1, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}
