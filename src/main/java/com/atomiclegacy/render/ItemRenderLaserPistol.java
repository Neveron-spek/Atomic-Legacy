package com.atomiclegacy.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderLaserPistol (laser pistol, Pew Pew, Morning Glory). */
public class ItemRenderLaserPistol extends ItemRenderWeaponBase {

	/** ResourceManager.laser_pistol (models/weapons/laser_pistol.obj) */
	static final LegacyEntityObjMesh laser_pistol = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/laser_pistol.obj"));
	public static final ResourceLocation laser_pistol_tex = AtomicLegacy.id("textures/models/weapons/laser_pistol.png");
	public static final ResourceLocation laser_pistol_pew_pew_tex = AtomicLegacy.id("textures/models/weapons/laser_pistol_pew_pew.png");
	public static final ResourceLocation laser_pistol_morning_glory_tex = AtomicLegacy.id("textures/models/weapons/laser_pistol_morning_glory.png");
	/** ItemRenderWeaponBase.laser_flash */
	public static final ResourceLocation laser_flash = AtomicLegacy.id("textures/models/weapons/laser_flash.png");

	public ResourceLocation texture;

	public ItemRenderLaserPistol(ResourceLocation texture) {
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
				-1.75F * offset, -2F * offset, 2.75F * offset,
				0, -10 / 8D, 1.25);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(texture));
	}

	private void part(VertexConsumer vc, String name) {
		laser_pistol.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] latch = HbmAnimations.getRelevantTransformation("LATCH");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] jolt = HbmAnimations.getRelevantTransformation("JOLT");
		double[] battery = HbmAnimations.getRelevantTransformation("BATTERY");
		double[] swirl = HbmAnimations.getRelevantTransformation("SWIRL");

		pose.translate(0, -1, -6);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 6);

		pose.translate(0, 2, -2);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, -2, 2);

		pose.translate(0, -1, -1);
		glRotate(pose, swirl[0], 1, 0, 0);
		pose.translate(0, 1, 1);

		pose.translate(0, 0, recoil[2]);
		pose.translate(jolt[0], jolt[1], jolt[2]);

		part(vc, "Gun");
		if(hasCapacitors(stack)) part(vc, "Capacitors");
		if(hasTape(stack)) part(vc, "Tape");

		pose.pushPose();
		pose.translate(1.125, 0, -1.9125);
		glRotate(pose, latch[1], 0, 1, 0);
		pose.translate(-1.125, 0, 1.9125);
		part(vc, "Latch");
		pose.translate(battery[0], battery[1], battery[2]);
		part(vc, "Battery");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 2, 4.75);
		glRotate(pose, 90, 0, 1, 0);
		renderLaserFlash(pose, buffers, gun.lastShot[0], 150, 1.5D, hasEmerald(stack) ? 0x008000 : 0xff0000);
		pose.translate(0, 0, -0.25);
		renderLaserFlash(pose, buffers, gun.lastShot[0], 150, 0.75D, hasEmerald(stack) ? 0x80ff00 : 0xff8000);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -0.5, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Latch");
		if(hasCapacitors(stack)) part(vc, "Capacitors");
		if(hasTape(stack)) part(vc, "Tape");

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
			pose.translate(0, 2, 4.75);
			glRotate(pose, 90, 0, 1, 0);
			renderLaserFlash(pose, buffers, shot, 150, 1.5D, hasEmerald(stack) ? 0x008000 : 0xff0000);
			pose.translate(0, 0, -0.25);
			renderLaserFlash(pose, buffers, shot, 150, 0.75D, hasEmerald(stack) ? 0x80ff00 : 0xff8000);
			pose.popPose();
		}
	}

	public boolean hasCapacitors(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_LASER_PISTOL_PEW_PEW.get();
	}

	public boolean hasTape(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_LASER_PISTOL_PEW_PEW.get();
	}

	public boolean hasEmerald(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_LASER_PISTOL_MORNING_GLORY.get();
	}

	/** ItemRenderWeaponBase.renderLaserFlash: laser_flash, GL_SRC_ALPHA/GL_ONE, no depth write, no culling, brightness 240. */
	public static void renderLaserFlash(PoseStack pose, MultiBufferSource buffers, long lastShot, int flash, double scale, int color) {

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
