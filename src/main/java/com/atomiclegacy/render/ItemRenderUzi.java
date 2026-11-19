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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderUzi. */
public class ItemRenderUzi extends ItemRenderWeaponBase {

	/** ResourceManager.uzi (models/weapons/uzi.obj) */
	public static final LegacyEntityObjMesh uzi = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/uzi.obj"));
	public static final ResourceLocation uzi_tex = AtomicLegacy.id("textures/models/weapons/uzi.png");
	public static final ResourceLocation uzi_saturnite_tex = AtomicLegacy.id("textures/models/weapons/uzi_saturnite.png");

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
				-1.75F * offset, -1.5F * offset, 2.5F * offset,
				0, -4.375 / 8D, 1);
	}

	protected VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	protected void part(VertexConsumer vc, String name) {
		uzi.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(isSaturnite(stack) ? uzi_saturnite_tex : uzi_tex);
		double scale = 0.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] stockFront = HbmAnimations.getRelevantTransformation("STOCKFRONT");
		double[] stockBack = HbmAnimations.getRelevantTransformation("STOCKBACK");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] yeet = HbmAnimations.getRelevantTransformation("YEET");
		double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");

		pose.translate(yeet[0], yeet[1], yeet[2]);
		glRotate(pose, speen[0], 0, 0, 1);

		pose.translate(0, -2, -4);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 4);

		pose.translate(0, 0, -6);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 0, 6);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");

		boolean silenced = hasSilencer(stack, 0);
		if(silenced) part(vc, "Silencer");

		pose.pushPose();
		pose.translate(0, 0.3125D, -5.75);
		glRotate(pose, 180 - stockFront[0], 1, 0, 0);
		pose.translate(0, -0.3125D, 5.75);
		part(vc, "StockFront");

		pose.translate(0, -0.3125D, -3);
		glRotate(pose, -200 - stockBack[0], 1, 0, 0);
		pose.translate(0, 0.3125D, 3);
		part(vc, "StockBack");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, slide[2]);
		part(vc, "Slide");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part(vc, "Magazine");
		if(bullet[0] == 1) part(vc, "Bullet");
		pose.popPose();

		if(!silenced) {
			double smokeScale = 0.5;

			pose.pushPose();
			pose.translate(0, 0.75, 8.5);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.75D);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 0.75, 8.5);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
			this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 1, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 1, -4);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		VertexConsumer vc = consumer(isSaturnite(stack) ? uzi_saturnite_tex : uzi_tex);
		part(vc, "Gun");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(hasSilencer(stack, index)) part(vc, "Silencer");
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		boolean silenced = hasSilencer(stack, 0);

		if(silenced && type == LegacyRenderType.INVENTORY) {
			double scale = 0.625D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(0, 0, -4);
		}

		VertexConsumer vc = consumer(isSaturnite(stack) ? uzi_saturnite_tex : uzi_tex);
		part(vc, "Gun");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(silenced) part(vc, "Silencer");

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
			pose.translate(0, 0.75, 8.5);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public boolean isSaturnite(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_UZI_SATURN);
	}
}
