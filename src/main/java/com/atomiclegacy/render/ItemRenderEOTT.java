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

/**
 * [weapons batch F4] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderEOTT: the aberrator model with eott.png held akimbo
 * (config 0 left with mirrored roll/magazine motion, config 1 right), both in first person and on the left arm.
 */
public class ItemRenderEOTT extends ItemRenderWeaponBase {

	private static final LegacyEntityObjMesh aberrator = ItemRenderAberrator.aberrator;
	public static final ResourceLocation eott_tex = AtomicLegacy.id("textures/models/weapons/eott.png");

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(eott_tex));
	}

	private void part(VertexConsumer vc, String name) {
		aberrator.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	private void gunParts(VertexConsumer vc) {
		part(vc, "Gun");
		part(vc, "Hammer");
		part(vc, "Magazine");
		part(vc, "Slide");
		part(vc, "Sight");
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			VertexConsumer vc = consumer();
			int index = i == -1 ? 0 : 1;

			pose.pushPose();
			standardAimingTransform(pose, stack,
					-1.0F * offset * i, -1.25F * offset, 1.25F * offset,
					0, -5.25 / 8D, 0.125);

			double scale = 0.25D;
			pose.scale((float) scale, (float) scale, (float) scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] rise = HbmAnimations.getRelevantTransformation("RISE", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);
			double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
			double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER", index);
			double[] roll = HbmAnimations.getRelevantTransformation("ROLL", index);
			double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
			double[] magroll = HbmAnimations.getRelevantTransformation("MAGROLL", index);
			double[] sight = HbmAnimations.getRelevantTransformation("SIGHT", index);

			pose.translate(0, rise[1], 0);

			pose.translate(0, 1, -2.25);
			glRotate(pose, equip[0], 1, 0, 0);
			pose.translate(0, -1, 2.25);

			pose.translate(0, -1, -4);
			glRotate(pose, recoil[0], 1, 0, 0);
			pose.translate(0, 1, 4);

			pose.translate(0, 1, 0);
			glRotate(pose, roll[2] * i, 0, 0, 1);
			pose.translate(0, -1, 0);

			part(vc, "Gun");

			pose.pushPose();
			pose.translate(0, 2.4375, -1.9375);
			glRotate(pose, sight[0], 1, 0, 0);
			pose.translate(0, -2.4375, 1.9375);
			part(vc, "Sight");
			pose.popPose();

			pose.pushPose();
			pose.translate(mag[0] * i, mag[1], mag[2]);

			pose.translate(0, 1, 0);
			glRotate(pose, magroll[2] * i, 0, 0, 1);
			pose.translate(0, -1, 0);

			part(vc, "Magazine");
			pose.translate(bullet[0], bullet[1], bullet[2]);
			part(vc, "Bullet");
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 0, slide[2]);
			part(vc, "Slide");
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 1.25, -3.625);
			glRotate(pose, -45 + hammer[0], 1, 0, 0);
			pose.translate(0, -1.25, 3.625);
			part(vc, "Hammer");
			pose.popPose();

			double smokeScale = 0.5;

			pose.pushPose();
			pose.translate(0, 2, 4);
			glRotate(pose, recoil[0], -1, 0, 0);
			glRotate(pose, roll[2] * i, 0, 0, -1);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.5D);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 2, 4);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(gun.lastShot[index], 75, 7.5);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 2, -1.5);
			pose.scale(0.5F, 0.5F, 0.5F);
			ItemRenderAberrator.renderFireball(pose, buffers, gun.lastShot[index]);
			pose.popPose();

			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, -1, 4);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		pose.translate(0, -1, 4);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 8, 0);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1, 0.5);
	}

	@Override
	public void renderInv(ItemStack stack) {

		pose.translate(0, 1, 0);

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-1, 0, 0);
		gunParts(consumer());
		pose.popPose();

		pose.translate(0, 0, 5);
		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -90, 1, 0, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, -45, 0, 1, 0);
		pose.translate(1, 0, 0);
		gunParts(consumer());
		pose.popPose();
	}

	@Override
	public void renderEquipped(ItemStack stack, LivingEntity ent) {
		gunParts(consumer());
		flashAndFireball(stack, ent, 1);
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {
		gunParts(consumer());
		flashAndFireball(stack, ent, 0);
	}

	private void flashAndFireball(ItemStack stack, LivingEntity ent, int index) {
		if(ent == null) return;
		long shot;
		double shotRand = 0;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[index];
			shotRand = gun.shotRand;
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		pose.pushPose();
		pose.translate(0, 2, 4);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * shotRand, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(shot, 75, 7.5);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 2, -1.5);
		pose.scale(0.5F, 0.5F, 0.5F);
		ItemRenderAberrator.renderFireball(pose, buffers, shot);
		pose.popPose();
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		renderOther(stack, LegacyRenderType.INVENTORY, null);
	}

	@Override
	public void renderEntity(ItemStack stack) {
		pose.pushPose();
		pose.translate(-1, 1, 0);
		gunParts(consumer());
		pose.popPose();

		pose.pushPose();
		pose.translate(1, 1, 0);
		gunParts(consumer());
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		gunParts(consumer());
	}
}
