package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMareslegAkimbo. */
public class ItemRenderMareslegAkimbo extends ItemRenderWeaponBase {

	private static final LegacyEntityObjMesh maresleg = ItemRenderMaresleg.maresleg;

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(ItemRenderMaresleg.maresleg_tex));
	}

	// [fix: crash] smoke/flash switch the immediate buffer; fetch the cutout consumer per part
	private void part(VertexConsumer vc, String name) {
		maresleg.renderGroup(name, pose, consumer(), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {

			VertexConsumer vc = consumer();
			pose.pushPose();

			int index = i == -1 ? 0 : 1;

			standardAimingTransform(pose, stack, -1.5F * offset * i, -1F * offset, 2F * offset, 0, -3.875 / 8D, 1);

			double scale = 0.375D;
			pose.scale((float) scale, (float) scale, (float) scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] lever = HbmAnimations.getRelevantTransformation("LEVER", index);
			double[] turn = HbmAnimations.getRelevantTransformation("TURN", index);
			double[] flip = HbmAnimations.getRelevantTransformation("FLIP", index);
			double[] lift = HbmAnimations.getRelevantTransformation("LIFT", index);
			double[] shell = HbmAnimations.getRelevantTransformation("SHELL", index);
			double[] flag = HbmAnimations.getRelevantTransformation("FLAG", index);

			pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
			glRotate(pose, recoil[2] * 5, 1, 0, 0);
			glRotate(pose, turn[2], 0, 0, 1);

			pose.translate(0, 0, -4);
			glRotate(pose, lift[0], 1, 0, 0);
			pose.translate(0, 0, 4);

			pose.translate(0, 0, -4);
			glRotate(pose, equip[0], -1, 0, 0);
			pose.translate(0, 0, 4);

			pose.translate(0, 0, -2);
			glRotate(pose, flip[0], -1, 0, 0);
			pose.translate(0, 0, 2);

			pose.pushPose();
			pose.translate(0, 1, 3.75);
			glRotate(pose, turn[2], 0, 0, -1);
			glRotate(pose, flip[0], 1, 0, 0);
			glRotate(pose, 90, 0, 1, 0);
			this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.25D);
			pose.popPose();

			part(vc, "Gun");

			pose.pushPose();
			pose.translate(0, 0.125, -2.875);
			glRotate(pose, lever[0], 1, 0, 0);
			pose.translate(0, -0.125, 2.875);
			part(vc, "Lever");
			pose.popPose();

			pose.pushPose();
			pose.translate(shell[0], shell[1] - 0.75, shell[2]);
			part(vc, "Shell");
			pose.popPose();

			if(flag[0] != 0) {
				pose.pushPose();
				pose.translate(0, -0.5, 0);
				part(vc, "Shell");
				pose.popPose();
			}

			pose.pushPose();
			pose.translate(0, 1, 3.75);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
			this.renderMuzzleFlash(gun.lastShot[index], 75, 5);
			pose.popPose();

			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.25, 3);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.25, 3);
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
		pose.translate(0, -0.5, 1);
	}

	@Override
	public void renderInv(ItemStack stack) {

		VertexConsumer vc = consumer();

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-1, 0, 0);
		part(vc, "Gun");
		part(vc, "Lever");
		pose.popPose();

		pose.translate(0, 0, 5);
		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -90, 1, 0, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, -45, 0, 1, 0);
		pose.translate(1, 0, 0);
		part(vc, "Gun");
		part(vc, "Lever");
		pose.popPose();
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		renderOther(stack, LegacyRenderType.INVENTORY, null);
	}

	@Override
	public void renderEntity(ItemStack stack) {

		VertexConsumer vc = consumer();

		pose.pushPose();
		pose.translate(-1, 1, 0);
		part(vc, "Gun");
		part(vc, "Lever");
		pose.popPose();

		pose.pushPose();
		pose.translate(1, 1, 0);
		part(vc, "Gun");
		part(vc, "Lever");
		pose.popPose();
	}

	@Override
	public void renderEquipped(ItemStack stack, LivingEntity ent) {
		renderOther(stack, LegacyRenderType.EQUIPPED, null);
		if(ent == null) return;
		long shot;
		double shotRand = 0;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[1];
			shotRand = gun.shotRand;
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		pose.pushPose();
		pose.translate(0, 1, 3.75);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * shotRand, 1, 0, 0);
		this.renderMuzzleFlash(shot, 75, 5);
		pose.popPose();
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {
		renderOther(stack, LegacyRenderType.EQUIPPED, null);
		if(ent == null) return;

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
		pose.translate(0, 1, 3.75);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * shotRand, 1, 0, 0);
		this.renderMuzzleFlash(shot, 75, 5);
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Lever");
	}
}
