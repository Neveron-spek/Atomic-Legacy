package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.AmmoFamily;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * [weapons batch F2] GunFactoryClient renderers of the Fat Man rounds: LegoClient.RENDER_NUKE (nuke_standard / demo /
 * high), RENDER_GRENADE (nuke_tots), RENDER_HIVE (nuke_hive) and RENDER_NUKE_BALEFIRE (nuke_balefire).
 */
final class GunBatchF2BulletRenderer {

	private GunBatchF2BulletRenderer() {
	}

	private static double nukeRadius = -1;
	private static double hiveRadius = -1;
	private static double totsRadius = -1;

	static boolean handles(StandardAmmoType ammo) {
		return ammo.family() == AmmoFamily.NUKE;
	}

	static double radius(StandardAmmoType ammo) {
		switch(ammo) {
			case NUKE_TOTS -> { if(totsRadius < 0) totsRadius = bounds(GunBatchD3BulletRenderer.PROJECTILES, "Grenade", 0.25F, 0, 0, 0, 90, 0, 0, 1); return totsRadius; }
			case NUKE_HIVE -> { if(hiveRadius < 0) hiveRadius = bounds(ItemRenderPanzerschreck.panzerschreck, "Rocket", 0.125F, 0, 0, 3.5F, -90, 0, 1, 0); return hiveRadius; }
			default -> { if(nukeRadius < 0) nukeRadius = bounds(ItemRenderFatMan.fatman, "MiniNuke", 0.125F, 0, -1, 1, -90, 0, 1, 0); return nukeRadius; }
		}
	}

	private static double bounds(LegacyEntityObjMesh mesh, String group, float scale, float tx, float ty, float tz, float angle, float ax, float ay, float az) {
		PoseStack pose = new PoseStack();
		pose.scale(scale, scale, scale);
		ItemRenderWeaponBase.glRotate(pose, angle, ax, ay, az);
		pose.translate(tx, ty, tz);
		EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
		mesh.includeGroupFacePositions(group, pose, bounds);
		return Math.nextUp(bounds.maximumRadius() + 0.1D);
	}

	static void render(LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		StandardAmmoType ammo = bullet.ammoType();
		if(ammo == StandardAmmoType.NUKE_TOTS) {
			GunBatchD3BulletRenderer.renderGrenade(bullet, interp, pose, buffers, light);
			return;
		}
		float yaw = Mth.rotLerp(interp, bullet.yRotO, bullet.getYRot());
		float pitch = -Mth.lerp(interp, bullet.xRotO, bullet.getXRot()); // 1.7.10 pitch is positive upwards
		pose.pushPose();
		// RenderBulletMK4 renderRotations
		pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
		switch(ammo) {
			case NUKE_HIVE -> {
				pose.scale(0.125F, 0.125F, 0.125F);
				pose.mulPose(Axis.YN.rotationDegrees(90));
				pose.translate(0, 0, 3.5F);
				ItemRenderPanzerschreck.panzerschreck.renderGroup("Rocket", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderPanzerschreck.panzerschreck_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			}
			case NUKE_BALEFIRE -> {
				pose.scale(0.125F, 0.125F, 0.125F);
				pose.mulPose(Axis.YP.rotationDegrees(-90));
				pose.translate(0, -1, 1F);
				ItemRenderFatMan.renderBalefire(pose, buffers, light, interp);
			}
			default -> {
				pose.scale(0.125F, 0.125F, 0.125F);
				pose.mulPose(Axis.YP.rotationDegrees(-90));
				pose.translate(0, -1, 1F);
				ItemRenderFatMan.fatman.renderGroup("MiniNuke", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderFatMan.fatman_mininuke_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			}
		}
		pose.popPose();
	}
}
