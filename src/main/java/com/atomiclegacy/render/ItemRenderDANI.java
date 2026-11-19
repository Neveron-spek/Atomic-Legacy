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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderDANI (gun_light_revolver_dani, akimbo). */
public class ItemRenderDANI extends ItemRenderWeaponBase {

	private static final LegacyEntityObjMesh bio_revolver = ItemRenderAtlas.bio_revolver;
	public static final ResourceLocation dani_celestial_tex = AtomicLegacy.id("textures/models/weapons/dani_celestial.png");
	public static final ResourceLocation dani_lunar_tex = AtomicLegacy.id("textures/models/weapons/dani_lunar.png");

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		bio_revolver.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	private void all(ResourceLocation tex) {
		bio_revolver.renderAll(pose, consumer(tex), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {

			int index = i == -1 ? 0 : 1;
			pose.pushPose();

			standardAimingTransform(pose, stack,
					-1.5F * offset * i, -0.75F * offset, 1F * offset,
					0, -3.125 / 8D, 0.25);

			double scale = 0.125D;
			pose.scale((float) scale, (float) scale, (float) scale);

			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE", index);
			double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT", index);
			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);

			pose.translate(recoil[0], recoil[1], recoil[2]);
			glRotate(pose, recoil[2] * 10, 1, 0, 0);

			pose.translate(0, -2, -2);
			glRotate(pose, equip[0], -1, 0, 0);
			pose.translate(0, 2, 2);

			pose.pushPose();
			pose.translate(0, 1.5, 9.25);
			glRotate(pose, -recoil[2] * 10, 1, 0, 0);
			glRotate(pose, 90, 0, 1, 0);
			this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.5D);
			pose.popPose();

			VertexConsumer vc = consumer(index == 0 ? dani_celestial_tex : dani_lunar_tex);
			pose.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

			glRotate(pose, reloadRot[0], 1, 0, 0);
			glRotate(pose, reloadRot[2] * i, 0, 0, 1);
			glRotate(pose, reloadRot[1] * i, 0, 1, 0);
			part(vc, "Grip");

			pose.pushPose(); /// FRONT PUSH ///
			glRotate(pose, HbmAnimations.getRelevantTransformation("FRONT", index)[2], 1, 0, 0);
			part(vc, "Barrel");
			pose.pushPose(); /// LATCH PUSH ///
			pose.translate(0, 2.3125, -0.875);
			glRotate(pose, HbmAnimations.getRelevantTransformation("LATCH", index)[2], 1, 0, 0);
			pose.translate(0, -2.3125, 0.875);
			part(vc, "Latch");
			pose.popPose(); /// LATCH POP ///

			pose.pushPose(); /// DRUM PUSH ///
			pose.translate(0, 1, 0);
			glRotate(pose, HbmAnimations.getRelevantTransformation("DRUM", index)[2] * 60, 0, 0, 1);
			pose.translate(0, -1, 0);
			pose.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH", index)[2]);
			part(vc, "Drum");
			pose.popPose(); /// DRUM POP ///

			pose.popPose(); /// FRONT POP ///

			pose.pushPose(); /// HAMMER ///
			pose.translate(0, 0, -4.5);
			glRotate(pose, -45 + 45 * HbmAnimations.getRelevantTransformation("HAMMER", index)[2], 1, 0, 0);
			pose.translate(0, 0, 4.5);
			part(vc, "Hammer");
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 1.5, 9.25);
			glRotate(pose, 90, 0, 1, 0);
			this.renderMuzzleFlash(gun.lastShot[index], 75, 7.5);
			pose.popPose();

			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 6, 0);
		double scale = 1.125D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 1.5, 0);
	}

	@Override
	public void renderInv(ItemStack stack) {

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(2, 0, 0);
		all(dani_celestial_tex);
		pose.popPose();

		pose.translate(0, 0, 5);
		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -90, 1, 0, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, -45, 0, 1, 0);
		pose.translate(-2, 0, 0);
		all(dani_lunar_tex);
		pose.popPose();
	}

	@Override
	public void renderEquipped(ItemStack stack, LivingEntity ent) {

		all(dani_lunar_tex);

		//Stopgap: at the moment, the flashMap is gun agnostic and does not care about index.
		if(ent == null) return;
		long shot;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[1];
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

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {

		all(dani_celestial_tex);

		if(ent == null) return;
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

	@Override
	public void renderModTable(ItemStack stack, int index) {
		all(index == 1 ? dani_celestial_tex : dani_lunar_tex);
	}

	@Override
	public void renderEntity(ItemStack stack) {
		pose.pushPose();
		pose.translate(-2, 1, 0);
		all(dani_lunar_tex);
		pose.popPose();

		pose.pushPose();
		pose.translate(2, 1, 0);
		all(dani_celestial_tex);
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		all(dani_celestial_tex);
	}
}
