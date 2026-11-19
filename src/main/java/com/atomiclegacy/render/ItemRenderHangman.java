package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderHangman. */
public class ItemRenderHangman extends ItemRenderWeaponBase {

	/** ResourceManager.hangman (models/weapons/hangman.obj) */
	public static final LegacyEntityObjMesh hangman = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/hangman.obj"));
	public static final ResourceLocation hangman_tex = AtomicLegacy.id("textures/models/weapons/hangman.png");

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
				-1.5F * offset, -0.875F * offset, 1.75F * offset,
				0, -1.5 / 8D, 1.25);
	}

	protected void part(String name) {
		hangman.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(hangman_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		float offset = 0.8F;

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] smack = HbmAnimations.getRelevantTransformation("SMACK");
		double[] lid = HbmAnimations.getRelevantTransformation("LID");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] bullets = HbmAnimations.getRelevantTransformation("BULLETS");

		pose.translate(1.5F * offset, 0, -1);
		glRotate(pose, turn[1], 0, 1, 0);
		pose.translate(-1.5F * offset, 0, 1);

		glRotate(pose, roll[2], 0, 0, 1);
		pose.translate(smack[0], smack[1], smack[2]);

		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);

		pose.translate(0, -4, -10);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 4, 10);

		pose.translate(0, 0, recoil[2]);

		part("Rifle");
		part("Internals");

		pose.pushPose();
		pose.translate(-2.1875, -1.75, 0);
		glRotate(pose, lid[2], 0, 0, 1);
		pose.translate(2.1875, 1.75, 0);
		part("Lid");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part("Magazine");
		if(bullets[0] == 0) part("Bullets");
		pose.popPose();

		double smokeScale = 1.5;

		pose.pushPose();
		pose.translate(0, 0, 29);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 29);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(2, 2, 2);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 4.25, 11);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 2.5, 0);
	}

	@Override
	public void setupEntity(ItemStack stack) {
		double scale = 0.0625D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		hangman.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(hangman_tex)), light, overlay, 0xFFFFFFFF);

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
			pose.translate(0, 0, 29);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(2, 2, 2);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}
