package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderUziAkimbo. */
public class ItemRenderUziAkimbo extends ItemRenderWeaponBase {

	private static final LegacyEntityObjMesh uzi = ItemRenderUzi.uzi;

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

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		uzi.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	private ResourceLocation tex(ItemStack stack, int index) {
		return isSaturnite(stack, index) ? ItemRenderUzi.uzi_saturnite_tex : ItemRenderUzi.uzi_tex;
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			int index = i == -1 ? 0 : 1;
			VertexConsumer vc = consumer(tex(stack, index));

			pose.pushPose();
			standardAimingTransform(pose, stack, -2.25F * offset * i, -1.5F * offset, 2.5F * offset, 0, -4.375 / 8D, 1);

			double scale = 0.25D;
			pose.scale((float) scale, (float) scale, (float) scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] stockFront = HbmAnimations.getRelevantTransformation("STOCKFRONT", index);
			double[] stockBack = HbmAnimations.getRelevantTransformation("STOCKBACK", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] lift = HbmAnimations.getRelevantTransformation("LIFT", index);
			double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
			double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
			double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);
			double[] yeet = HbmAnimations.getRelevantTransformation("YEET", index);
			double[] speen = HbmAnimations.getRelevantTransformation("SPEEN", index);

			pose.translate(yeet[0], yeet[1], yeet[2]);
			glRotate(pose, speen[0], 0, 0, i);

			pose.translate(0, -2, -4);
			glRotate(pose, equip[0], 1, 0, 0);
			pose.translate(0, 2, 4);

			pose.translate(0, 0, -6);
			glRotate(pose, lift[0], 1, 0, 0);
			pose.translate(0, 0, 6);

			pose.translate(0, 0, recoil[2]);

			part(vc, index == 0 ? "GunMirror" : "Gun");

			boolean silenced = hasSilencer(stack, index);
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
				this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.75D);
				pose.popPose();

				pose.pushPose();
				pose.translate(0, 0.75, 8.5);
				glRotate(pose, 90, 0, 1, 0);
				glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
				this.renderMuzzleFlash(gun.lastShot[index], 75, 7.5);
				pose.popPose();
			}

			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 1, 1);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		pose.translate(0, 1, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 8, 0);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 1, -4);
	}

	@Override
	public void renderEquipped(ItemStack stack, LivingEntity ent) {

		VertexConsumer vc = consumer(tex(stack, 1));
		part(vc, "Gun");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		boolean silenced = hasSilencer(stack, 1);
		if(silenced) part(vc, "Silencer");

		if(!silenced && ent != null) {
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
			pose.translate(0, 0.75, 8.5);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {

		VertexConsumer vc = consumer(tex(stack, 0));
		part(vc, "GunMirror");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		boolean silenced = hasSilencer(stack, 0);
		if(silenced) part(vc, "Silencer");

		if(!silenced) {
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

	@Override
	public void renderModTable(ItemStack stack, int index) {
		VertexConsumer vc = consumer(tex(stack, index));
		part(vc, index == 0 ? "GunMirror" : "Gun");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(hasSilencer(stack, index)) part(vc, "Silencer");
	}

	@Override
	public void renderEntity(ItemStack stack) {

		boolean silencer0 = hasSilencer(stack, 1);
		boolean silencer1 = hasSilencer(stack, 0);
		boolean anySilenced = silencer0 || silencer1;

		if(anySilenced) {
			pose.scale(0.75F, 0.75F, 0.75F);
		}

		pose.pushPose();
		pose.translate(-1, 1, 0);
		VertexConsumer vc = consumer(tex(stack, 1));
		part(vc, "Gun");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(silencer0) part(vc, "Silencer");
		pose.popPose();

		pose.pushPose();
		pose.translate(1, 1, 0);
		vc = consumer(tex(stack, 0));
		part(vc, "GunMirror");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(silencer1) part(vc, "Silencer");
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		boolean silencer0 = hasSilencer(stack, 1);
		boolean silencer1 = hasSilencer(stack, 0);
		boolean anySilenced = silencer0 || silencer1;

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 1, 0);
		if(anySilenced) {
			double scale = 0.625D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(0, 0, -4);
		}
		VertexConsumer vc = consumer(tex(stack, 1));
		part(vc, "Gun");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(silencer0) part(vc, "Silencer");
		pose.popPose();

		pose.translate(0, 0, 5);

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -90, 1, 0, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, -45, 0, 1, 0);
		pose.translate(0, 1, 0);
		if(anySilenced) {
			double scale = 0.625D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(0, 0, -4);
		}
		vc = consumer(tex(stack, 0));
		part(vc, "GunMirror");
		part(vc, "StockBack");
		part(vc, "StockFront");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(silencer1) part(vc, "Silencer");
		pose.popPose();
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public boolean isSaturnite(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_UZI_SATURN);
	}
}
