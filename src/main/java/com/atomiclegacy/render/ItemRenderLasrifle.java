package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryEnergy;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderLasrifle. */
public class ItemRenderLasrifle extends ItemRenderWeaponBase {

	/** ResourceManager.lasrifle / lasrifle_mods (models/weapons/lasrifle.obj, lasrifle_mods.obj) */
	static final LegacyEntityObjMesh lasrifle = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/lasrifle.obj"));
	static final LegacyEntityObjMesh lasrifle_mods = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/lasrifle_mods.obj"));
	public static final ResourceLocation lasrifle_tex = AtomicLegacy.id("textures/models/weapons/lasrifle.png");
	public static final ResourceLocation lasrifle_mods_tex = AtomicLegacy.id("textures/models/weapons/lasrifle_mods.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * (hasScope(stack) ? 0.75F : 0.66F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;

		if(hasScope(stack)) {
			standardAimingTransform(pose, stack,
					-1.5F * offset, -1.5F * offset, 2.5F * offset,
					0, -7.375 / 8D, 0.75);
		} else {
			standardAimingTransform(pose, stack,
					-1.5F * offset, -1.5F * offset, 2.5F * offset,
					0, -5.25 / 8D, 1);
		}
	}

	private void part(String name) {
		lasrifle.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(lasrifle_tex)), light, overlay, 0xFFFFFFFF);
	}

	private void modPart(String name) {
		lasrifle_mods.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(lasrifle_mods_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		if(hasScope(stack) && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.3125D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");

		pose.translate(0, -1, -6);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 6);

		pose.translate(0, 0, recoil[2]);

		part("Gun");
		part("Stock");
		if(hasScope(stack)) part("Scope");

		pose.pushPose();
		pose.translate(0, -0.375, 2.375);
		glRotate(pose, lever[0], 1, 0, 0);
		pose.translate(0, 0.375, -2.375);
		part("Lever");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part("Battery");
		pose.popPose();

		if(!hasShotgun(stack)) part("Barrel");
		if(hasShotgun(stack)) modPart("BarrelShotgun");
		if(hasCapacitor(stack)) modPart("UnderBarrel");

		pose.pushPose();
		pose.translate(0, 1.5, 12);
		glRotate(pose, 90, 0, 1, 0);
		ItemRenderLaserPistol.renderLaserFlash(pose, buffers, gun.lastShot[0], 150, 1.5D, 0xff0000);
		pose.translate(0, 0, -0.25);
		ItemRenderLaserPistol.renderLaserFlash(pose, buffers, gun.lastShot[0], 150, 0.75D, 0xff8000);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.03125D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0.75, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1, -1);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		part("Gun");
		part("Stock");
		if(hasScope(stack)) part("Scope");
		part("Lever");
		part("Battery");
		if(!hasShotgun(stack)) part("Barrel");
		if(hasShotgun(stack)) modPart("BarrelShotgun");
		if(hasCapacitor(stack)) modPart("UnderBarrel");

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
			pose.translate(0, 1.5, 12);
			glRotate(pose, 90, 0, 1, 0);
			ItemRenderLaserPistol.renderLaserFlash(pose, buffers, shot, 150, 1.5D, 0xff0000);
			pose.translate(0, 0, -0.25);
			ItemRenderLaserPistol.renderLaserFlash(pose, buffers, shot, 150, 0.75D, 0xff8000);
			pose.popPose();
		}
	}

	public boolean hasScope(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, XFactoryEnergy.ID_LAS_AUTO);
	}

	public boolean hasShotgun(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XFactoryEnergy.ID_LAS_SHOTGUN);
	}

	public boolean hasCapacitor(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XFactoryEnergy.ID_LAS_CAPACITOR);
	}
}
