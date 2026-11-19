package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * [weapons batch F4] LegoClient.RENDER_FOLLY (folly_sm: flare sprite plus a wide BeamPronter RANDOM/SOLID beam),
 * RENDER_CRACKLE (p35800: 0xE3D692 / 0xFFFFFF) and RENDER_BLACK_LIGHTNING (p35800_bl: 0x4C3093 / 0x000000), the latter two
 * a renderBulletStandard tracer of the beam length scaled down with the age. All full bright.
 */
final class GunBatchF4BeamRenderer {

	private GunBatchF4BeamRenderer() {
	}

	private static double age(LegacyBulletBeamEntity bullet, float interp) {
		return Mth.clamp(1D - ((double) bullet.tickCount - 2 + interp) / (double) bullet.config.expires, 0, 1);
	}

	static void renderFolly(LegacyBulletBeamEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers) {
		double age = age(bullet, interp);

		pose.pushPose();
		GunBatchD3BulletRenderer.renderFlareSprite(pose, buffers, 1F, 1F, 1F, (1 - age) * 7.5 + 1.5, 0.5F * (float) age, 0.75F * (float) age);
		pose.popPose();

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180 - bullet.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-bullet.getXRot() - 90));
		float scale = (float) ((1 - age) * 25 + 2.5);
		pose.scale(scale, 1F, scale);
		int colorInner = ((int)(0x20 * age) << 16) | ((int)(0x20 * age) << 8) | (int) (0x20 * age);
		LegacyBulletBeamRenderer.prontBeamRandomSolid(pose, buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR), bullet.beamLength, colorInner, colorInner, bullet.tickCount / 3, (int)(bullet.beamLength / 2 + 1), 0F, 8, 0.0625F);
		pose.popPose();
	}

	static void renderCrackle(LegacyBulletBeamEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int dark, int light) {
		double age = age(bullet, interp);

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180 - bullet.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-bullet.getXRot() - 90));

		double scale = 5D;
		pose.scale((float) (age * scale), 1F, (float) (age * scale));
		pose.translate(0, bullet.beamLength, 0);
		pose.mulPose(Axis.ZP.rotationDegrees(-90));
		// renderBulletStandard(tess, dark, light, beamLength, true): widths 0.03125 / 0.03125 * 0.25
		LegacyProjectileEntityRenderer.renderTracerGeometry(pose.last(), buffers.getBuffer(HbmUnlitRenderTypes.TRACER), bullet.beamLength,
				0.03125D, 0.03125D * 0.25D, dark, light, LightTexture.FULL_BRIGHT);

		pose.popPose();
	}
}
