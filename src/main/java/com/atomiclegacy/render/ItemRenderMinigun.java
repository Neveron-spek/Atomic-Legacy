package com.atomiclegacy.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMinigun (gun_minigun and gun_minigun_lacunae). */
public class ItemRenderMinigun extends ItemRenderWeaponBase {

	/** ResourceManager.minigun (models/weapons/minigun.obj) */
	static final LegacyEntityObjMesh minigun = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/minigun.obj"));
	public static final ResourceLocation minigun_tex = AtomicLegacy.id("textures/models/weapons/minigun.png");
	public static final ResourceLocation minigun_lacunae_tex = AtomicLegacy.id("textures/models/weapons/minigun_lacunae.png");
	/** ItemRenderWeaponBase.laser_flash */
	public static final ResourceLocation laser_flash = AtomicLegacy.id("textures/models/weapons/laser_flash.png");

	protected ResourceLocation texture;

	public ItemRenderMinigun(ResourceLocation texture) {
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
				-1.75F * offset, -1.75F * offset, 3.5F * offset,
				0, -6.25 / 8D, 1);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(texture));
	}

	private void part(VertexConsumer vc, String name) {
		minigun.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

		pose.translate(0, 3, -6);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, -3, 6);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");
		part(vc, "Grip");

		pose.pushPose();
		glRotate(pose, rotate[2], 0, 0, 1);
		part(vc, "Barrels");
		pose.popPose();

		double smokeScale = 0.5;

		pose.pushPose();
		pose.translate(-2, 1.25, -3.5);
		glRotate(pose, 45, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 12);
		glRotate(pose, 90, 0, 1, 0);

		if(stack.getItem() == HbmItems.GUN_MINIGUN_LACUNAE.get()) {
			renderLaserFlash(gun.lastShot[0], 50, 1D, 0xff00ff);
			pose.translate(0, 0, -0.25);
			renderLaserFlash(gun.lastShot[0], 50, 0.5D, 0xff0080);
		} else {
			pose.translate(0, 0.5, 0);
			glRotate(pose, gun.shotRand * 90, 1, 0, 0);
			pose.scale(1.5F, 1.5F, 1.5F);
			this.renderMuzzleFlash(gun.lastShot[0], 50, 7.5);
		}
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(1, -3.5, 8);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 0.875D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.25, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Grip");
		part(vc, "Barrels");

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
			pose.translate(0, 0, 12.25);
			glRotate(pose, 90, 0, 1, 0);

			if(stack.getItem() == HbmItems.GUN_MINIGUN_LACUNAE.get()) {
				renderLaserFlash(shot, 50, 1D, 0xff00ff);
				pose.translate(0, 0, -0.25);
				renderLaserFlash(shot, 50, 0.5D, 0xff0080);
			} else {
				pose.translate(0, 0.5, 0);
				glRotate(pose, shotRand * 90, 1, 0, 0);
				pose.scale(1.5F, 1.5F, 1.5F);
				this.renderMuzzleFlash(shot, 50, 7.55);
			}
			pose.popPose();
		}
	}

	/** ItemRenderWeaponBase.renderLaserFlash: GL_SRC_ALPHA/GL_ONE, no depth write, no culling, brightness 240. */
	public void renderLaserFlash(long lastShot, int flash, double scale, int color) {

		if(System.currentTimeMillis() - lastShot < flash) {
			double fire = (System.currentTimeMillis() - lastShot) / (double) flash;
			double size = 4 * fire * scale;

			VertexConsumer tess = buffers.getBuffer(FLASH.apply(laser_flash));
			Matrix4f m = pose.last().pose();
			int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;

			tess.addVertex(m, 0F, (float) -size, (float) -size).setUv(1, 1).setColor(r, g, b, 255);
			tess.addVertex(m, 0F, (float) size, (float) -size).setUv(0, 1).setColor(r, g, b, 255);
			tess.addVertex(m, 0F, (float) size, (float) size).setUv(0, 0).setColor(r, g, b, 255);
			tess.addVertex(m, 0F, (float) -size, (float) size).setUv(1, 0).setColor(r, g, b, 255);
		}
	}
}
