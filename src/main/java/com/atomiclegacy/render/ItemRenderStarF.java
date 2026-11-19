package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderStarF. */
public class ItemRenderStarF extends ItemRenderWeaponBase {

	/** ResourceManager.star_f (models/weapons/star_f.obj) */
	static final LegacyEntityObjMesh star_f = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/star_f.obj"));
	public static final ResourceLocation star_f_tex = AtomicLegacy.id("textures/models/weapons/star_f.png");
	public static final ResourceLocation star_f_elite_tex = AtomicLegacy.id("textures/models/weapons/star_f_elite.png");

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
				-1.75F * offset, -1.75F * offset, 2.5F * offset,
				0, -7.625 / 8D, 1);
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		star_f.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(star_f_tex);
		double scale = 0.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");

		pose.translate(0, -2, -8);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 8);

		pose.translate(0, 1, -3);
		glRotate(pose, turn[2], 0, 0, 1);
		glRotate(pose, tilt[0], 1, 0, 0);
		pose.translate(0, -1, 3);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(0, 1.75, -4.25);
		glRotate(pose, 60 * (hammer[0] - 1), 1, 0, 0);
		pose.translate(0, -1.75, 4.25);
		part(vc, "Hammer");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, slide[2] * 2.3125);
		part(vc, "Slide");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part(vc, "Mag");
		pose.translate(bullet[0], bullet[1], bullet[2]);
		part(vc, "Bullet");
		pose.popPose();

		if(hasSilencer(stack)) {
			pose.pushPose();
			pose.translate(0, 2.375, -0.25);
			ItemRenderUzi.uzi.renderGroup("Silencer", pose, consumer(ItemRenderUzi.uzi_tex), light, overlay, 0xFFFFFFFF);
			pose.popPose();

		} else {
			double smokeScale = 0.5;

			pose.pushPose();
			pose.translate(0, 3, 6.125);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.75D);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 3, 6.125);
			pose.scale(0.75F, 0.75F, 0.75F);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
			this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, -0.25, 1.75);
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-1, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.25, -5);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		renderStandardGun(stack);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		boolean silenced = hasSilencer(stack);

		if(silenced && type == LegacyRenderType.INVENTORY) {
			double scale = 0.625D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(0, 0, -6);
		}

		renderStandardGun(stack);

		if(type == LegacyRenderType.EQUIPPED && !silenced && ent != null) {
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
			pose.translate(0, 3, 6.25);
			pose.scale(0.75F, 0.75F, 0.75F);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}

	public boolean hasSilencer(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}

	public void renderStandardGun(ItemStack stack) {
		VertexConsumer vc = consumer(star_f_tex);
		part(vc, "Gun");
		part(vc, "Slide");
		part(vc, "Mag");
		part(vc, "Hammer");
		boolean silenced = hasSilencer(stack);
		if(silenced) {
			pose.translate(0, 2.375, -0.25);
			ItemRenderUzi.uzi.renderGroup("Silencer", pose, consumer(ItemRenderUzi.uzi_tex), light, overlay, 0xFFFFFFFF);
		}
	}
}
