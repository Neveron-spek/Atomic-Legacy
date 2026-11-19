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

/** [parity d2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMinigunDual (gun_minigun_dual). */
public class ItemRenderMinigunDual extends ItemRenderWeaponBase {

	/** ResourceManager.minigun (models/weapons/minigun.obj) */
	private static final LegacyEntityObjMesh minigun = ItemRenderMinigun.minigun;
	/** ResourceManager.minigun_dual_tex */
	public static final ResourceLocation minigun_dual_tex = AtomicLegacy.id("textures/models/weapons/minigun_dual.png");

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
		pose.translate(0, 0, 0.875);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(minigun_dual_tex));
	}

	private void part(VertexConsumer vc, String name) {
		minigun.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			int index = i == -1 ? 0 : 1;
			VertexConsumer vc = consumer();

			pose.pushPose();
			standardAimingTransform(pose, stack, -2.75F * offset * i, -1.75F * offset, 2.5F * offset, 0, 0, 0);

			double scale = 0.375D;
			pose.scale((float) scale, (float) scale, (float) scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE", index);

			pose.translate(0, 3, -6);
			glRotate(pose, equip[0], 1, 0, 0);
			pose.translate(0, -3, 6);

			pose.translate(0, 0, recoil[2]);

			part(vc, index == 0 ? "GunDual" : "Gun");

			pose.pushPose();
			glRotate(pose, rotate[2] * i, 0, 0, 1);
			part(vc, "Barrels");
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 0, 12);
			glRotate(pose, 90, 0, 1, 0);

			glRotate(pose, gun.shotRand * 90, 1, 0, 0);
			pose.scale(1.5F, 1.5F, 1.5F);
			this.renderMuzzleFlash(gun.lastShot[index], 50, 7.5);
			pose.popPose();

			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(-1, -3.5, 8);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPerson(stack); // as in the original: not setupThirdPersonAkimbo
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(2, -3.5, 8);
	}

	@Override
	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 8, 0);
		double scale = 0.875D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderEquipped(ItemStack stack, LivingEntity ent) {

		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Barrels");

		// ItemInHandLayer passes no entity outside the player layers (the original always had data[1])
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
		pose.translate(0, 0, 12.25);
		glRotate(pose, 90, 0, 1, 0);

		pose.translate(0, 0.5, 0);
		glRotate(pose, shotRand * 90, 1, 0, 0);
		pose.scale(1.5F, 1.5F, 1.5F);
		this.renderMuzzleFlash(shot, 50, 7.5);
		pose.popPose();
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {

		VertexConsumer vc = consumer();
		part(vc, "GunDual");
		part(vc, "Barrels");

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
		pose.translate(0, 0, 12.25);
		glRotate(pose, 90, 0, 1, 0);

		pose.translate(0, 0.5, 0);
		glRotate(pose, shotRand * 90, 1, 0, 0);
		pose.scale(1.5F, 1.5F, 1.5F);
		this.renderMuzzleFlash(shot, 50, 7.5);
		pose.popPose();
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		VertexConsumer vc = consumer();
		part(vc, index == 0 ? "GunDual" : "Gun");
		part(vc, "Barrels");
	}

	@Override
	public void renderInv(ItemStack stack) {

		VertexConsumer vc = consumer();

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 45, 0, 1, 0);
		part(vc, "GunDual");
		part(vc, "Barrels");
		pose.popPose();

		pose.translate(0, 0, 8);
		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -90, 1, 0, 0);
		glRotate(pose, -45, 0, 1, 0);
		part(vc, "Gun");
		part(vc, "Barrels");
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Barrels");
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public boolean isSaturnite(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_UZI_SATURN);
	}
}
