package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderHenry (gun_henry: henry_tex, gun_henry_lincoln: henry_lincoln_tex). */
public class ItemRenderHenry extends ItemRenderWeaponBase {

	/** ResourceManager.henry (models/weapons/henry.obj) */
	public static final LegacyEntityObjMesh henry = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/henry.obj"));
	public static final ResourceLocation henry_tex = AtomicLegacy.id("textures/models/weapons/henry.png");
	public static final ResourceLocation henry_lincoln_tex = AtomicLegacy.id("textures/models/weapons/henry_lincoln.png");

	public ResourceLocation texture;

	public ItemRenderHenry(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1.25F * offset, -1F * offset, 1.75F * offset,
				0, -5 / 8D, 1);

		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		double r = -2.5 * aimingProgress;
		glRotate(pose, r, 1, 0, 0);
	}

	protected void part(String name) {
		henry.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] sight = HbmAnimations.getRelevantTransformation("SIGHT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] twist = HbmAnimations.getRelevantTransformation("TWIST");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] yeet = HbmAnimations.getRelevantTransformation("YEET");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");

		pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
		glRotate(pose, recoil[2] * 5, 1, 0, 0);
		glRotate(pose, turn[2], 0, 0, 1);

		pose.translate(yeet[0], yeet[1], yeet[2]);

		pose.translate(0, 1, 0);
		glRotate(pose, roll[2], 0, 0, 1);
		pose.translate(0, -1, 0);

		pose.translate(0, -4, 4);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 4, -4);

		pose.translate(0, 2, -4);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, -2, 4);

		pose.pushPose();
		pose.translate(0, 1, 8);
		glRotate(pose, turn[2], 0, 0, -1);
		glRotate(pose, 90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		pose.popPose();

		part("Gun");

		pose.pushPose();
		pose.translate(0, 1.25, -0.1875);
		glRotate(pose, sight[0], 1, 0, 0);
		pose.translate(0, -1.25, 0.1875);
		part("Sight");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.625, -3);
		glRotate(pose, -30 + hammer[0], 1, 0, 0);
		pose.translate(0, -0.625, 3);
		part("Hammer");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.25, -2.3125);
		glRotate(pose, lever[0], 1, 0, 0);
		pose.translate(0, -0.25, 2.3125);
		part("Lever");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1, 0);
		glRotate(pose, twist[2], 0, 0, 1);
		pose.translate(0, -1, 0);
		part("Front");
		pose.popPose();

		pose.pushPose();
		pose.translate(bullet[0], bullet[1], bullet[2] - 1);
		part("Bullet");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1, 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.25, 3);
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
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		henry.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);

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
			pose.translate(0, 1, 8);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 5);
			pose.popPose();
		}
	}
}
