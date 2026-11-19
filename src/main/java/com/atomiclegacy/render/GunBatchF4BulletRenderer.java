package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * [weapons batch F4] GunFactoryClient folly_nuke.setRenderer(LegoClient.RENDER_BIG_NUKE): the MissileMIRV part of
 * projectiles.obj with rocket_mirv.png (scale 0.5, rotated 90 degrees around Z) inside RenderBulletMK4's renderRotations
 * transform; no tracer.
 */
final class GunBatchF4BulletRenderer {

	private GunBatchF4BulletRenderer() {
	}

	/** ResourceManager.rocket_mirv_tex (textures/models/projectiles/rocket_mirv.png) */
	private static final ResourceLocation ROCKET_MIRV_TEX = AtomicLegacy.id("textures/models/projectiles/rocket_mirv.png");
	/** ResourceManager.projectiles (models/projectiles/projectiles.obj; same geometry at the port path) */
	private static final LegacyEntityObjMesh PROJECTILES = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/projectiles/projectiles.obj"));

	static boolean handles(StandardAmmoType ammo) {
		return ammo == StandardAmmoType.FOLLY_NUKE;
	}

	private static double mirvRadius = -1;

	static double radius(LegacyBulletEntity entity) {
		if(mirvRadius < 0) {
			PoseStack pose = new PoseStack();
			modelTransform(pose);
			EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
			PROJECTILES.includeGroupFacePositions("MissileMIRV", pose, bounds);
			mirvRadius = Math.nextUp(bounds.maximumRadius() * 1.0001D);
		}
		return mirvRadius;
	}

	private static void modelTransform(PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
		pose.mulPose(Axis.ZP.rotationDegrees(90F));
	}

	static void render(LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		float yaw = Mth.rotLerp(interp, bullet.yRotO, bullet.getYRot());
		// EntityBulletBaseMK4 rotationPitch is positive upwards; the port stores the negated value
		float pitch = -Mth.lerp(interp, bullet.xRotO, bullet.getXRot());

		pose.pushPose();
		// RenderBulletMK4 renderRotations
		pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
		modelTransform(pose);
		PROJECTILES.renderGroup("MissileMIRV", pose, buffers.getBuffer(RenderType.entityCutout(ROCKET_MIRV_TEX)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		pose.popPose();
	}
}
