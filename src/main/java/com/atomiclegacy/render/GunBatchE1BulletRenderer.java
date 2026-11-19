package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryRocket;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * [weapons batch E1] GunFactoryClient renderers of the XFactoryRocket rounds: LegoClient.RENDER_RPZB (rocket_rpzb),
 * RENDER_QD (rocket_qd), RENDER_ML (rocket_ml) and RENDER_FLIEGERFAUST (rocket_ff), drawn inside RenderBulletMK4's
 * renderRotations transform.
 */
final class GunBatchE1BulletRenderer {

	private GunBatchE1BulletRenderer() {
	}

	/** ResourceManager.rocket_tex (textures/models/projectiles/rocket.png) */
	private static final ResourceLocation ROCKET_TEX = AtomicLegacy.id("textures/models/projectiles/rocket.png");
	/** ResourceManager.grenade_tex (textures/models/projectiles/grenade.png) */
	private static final ResourceLocation GRENADE_TEX = AtomicLegacy.id("textures/models/projectiles/grenade.png");
	/** ResourceManager.projectiles (models/projectiles/projectiles.obj; same geometry at the port path) */
	private static final LegacyEntityObjMesh PROJECTILES = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/projectiles/projectiles.obj"));

	private enum Kind { RPZB, QD, ML, FF }

	private static Kind kind(StandardAmmoType ammo) {
		for(StandardAmmoType t : XFactoryRocket.RPZB) if(t == ammo) return Kind.RPZB;
		for(StandardAmmoType t : XFactoryRocket.QD) if(t == ammo) return Kind.QD;
		for(StandardAmmoType t : XFactoryRocket.ML) if(t == ammo) return Kind.ML;
		// [parity c1] GunFactoryClient: setRendererBulk(LegoClient.RENDER_RPZB, rocket_ncrpa_steer / rocket_ncrpa)
		for(StandardAmmoType t : XFactoryRocket.NCRPA) if(t == ammo) return Kind.RPZB;
		for(StandardAmmoType t : XFactoryRocket.NCRPA_STEER) if(t == ammo) return Kind.RPZB;
		return ammo == StandardAmmoType.ROCKET_FF ? Kind.FF : null;
	}

	/** Whether this round has an E1 renderer; the caller then skips the standard tracer. */
	static boolean handles(StandardAmmoType ammo) {
		return kind(ammo) != null;
	}

	private static double rpzbRadius = -1, qdRadius = -1, mlRadius = -1, ffRadius = -1;

	/** Culling radius: the rocket model in its render transform plus the tracer (length = 2 * displacement, 0.375 offset). */
	static double radius(LegacyBulletEntity entity) {
		Kind kind = kind(entity.ammoType());
		double model;
		switch(kind) {
			case RPZB -> { if(rpzbRadius < 0) rpzbRadius = bounds(ItemRenderPanzerschreck.panzerschreck, "Rocket", Kind.RPZB); model = rpzbRadius; }
			case QD -> { if(qdRadius < 0) qdRadius = bounds(PROJECTILES, "Rocket", Kind.QD); model = qdRadius; }
			case ML -> { if(mlRadius < 0) mlRadius = bounds(ItemRenderMissileLauncher.missile_launcher, "Missile", Kind.ML); model = mlRadius; }
			default -> { if(ffRadius < 0) ffRadius = Math.max(bounds(PROJECTILES, "Grenade", Kind.FF), Math.nextUp(0.375D * Math.sqrt(2D))); return ffRadius; }
		}
		double dx = entity.getX() - entity.xo, dy = entity.getY() - entity.yo, dz = entity.getZ() - entity.zo;
		double tracer = 0.375D + 2D * Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.05D;
		return Math.max(model, tracer);
	}

	private static double bounds(LegacyEntityObjMesh mesh, String group, Kind kind) {
		PoseStack pose = new PoseStack();
		modelTransform(pose, kind);
		EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
		mesh.includeGroupFacePositions(group, pose, bounds);
		return Math.nextUp(bounds.maximumRadius() * 1.0001D);
	}

	private static void modelTransform(PoseStack pose, Kind kind) {
		switch(kind) {
			case RPZB -> {
				pose.scale(0.125F, 0.125F, 0.125F);
				pose.mulPose(Axis.YP.rotationDegrees(-90F)); // glRotated(90, 0, -1, 0)
				pose.translate(0, 0, 3.5F);
			}
			case QD -> pose.mulPose(Axis.ZP.rotationDegrees(90F));
			case ML -> {
				pose.scale(0.25F, 0.25F, 0.25F);
				pose.mulPose(Axis.YP.rotationDegrees(-90F));
				pose.translate(0, -1, -4.5F);
			}
			case FF -> {
				pose.scale(0.25F * 4, 0.125F, 0.125F);
				pose.mulPose(Axis.ZP.rotationDegrees(90F));
				pose.translate(0, 0.25, 0);
			}
		}
	}

	static void render(LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		Kind kind = kind(bullet.ammoType());
		float yaw = Mth.rotLerp(interp, bullet.yRotO, bullet.getYRot());
		// EntityBulletBaseMK4 rotationPitch is atan2(dY, hyp) (positive upwards); the port stores the negated value
		float pitch = -Mth.lerp(interp, bullet.xRotO, bullet.getXRot());

		pose.pushPose();
		// RenderBulletMK4 renderRotations
		pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));

		pose.pushPose();
		modelTransform(pose, kind);
		switch(kind) {
			case RPZB -> ItemRenderPanzerschreck.panzerschreck.renderGroup("Rocket", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderPanzerschreck.panzerschreck_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			case QD -> PROJECTILES.renderGroup("Rocket", pose, buffers.getBuffer(RenderType.entityCutout(ROCKET_TEX)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			case ML -> ItemRenderMissileLauncher.missile_launcher.renderGroup("Missile", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderMissileLauncher.missile_launcher_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			case FF -> PROJECTILES.renderGroup("Grenade", pose, buffers.getBuffer(RenderType.entityCutout(GRENADE_TEX)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		}
		pose.popPose();

		if(kind != Kind.FF) {
			// glTranslatef(0.375F, 0, 0); renderBulletStandard(0x808080, 0xFFF2A7, length * 2, true)
			pose.translate(0.375D, 0.0D, 0.0D);
			double dx = bullet.getX() - bullet.xo, dy = bullet.getY() - bullet.yo, dz = bullet.getZ() - bullet.zo;
			double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if(length > 0) LegacyProjectileEntityRenderer.renderTracerGeometry(pose.last(), buffers.getBuffer(HbmUnlitRenderTypes.TRACER), length * 2,
					0.03125D, 0.03125D * 0.25D, 0x808080, 0xFFF2A7, LightTexture.FULL_BRIGHT);
		}
		pose.popPose();

		if(kind == Kind.FF) {
			// the flare sprite undoes the renderRotations (fullbright, camera facing): renderFlareSprite(1, 0.875, 0.25, 0.375, 0.125, 0.25)
			GunBatchD3BulletRenderer.renderFlareSprite(pose, buffers, 1F, 0.875F, 0.25F, 0.375D, 0.125F, 0.25F);
		}
	}
}
