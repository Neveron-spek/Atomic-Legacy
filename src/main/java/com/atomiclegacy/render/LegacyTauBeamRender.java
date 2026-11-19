package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * [weapons batch F2] 1.7.10 LegoClient.RENDER_TAU, RENDER_TAU_CHARGE (BeamPronter RANDOM/SOLID core plus a full
 * bright renderBulletStandard tracer along the beam) and RENDER_NI4NI_BOLT (tracer only).
 */
public final class LegacyTauBeamRender {

	private LegacyTauBeamRender() {
	}

	public static void renderTau(LegacyBulletBeamEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, boolean charge) {
		double age = Mth.clamp(1D - ((double) bullet.tickCount - 2 + interp) / (double) bullet.config.expires, 0, 1);

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180 - bullet.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-bullet.getXRot() - 90));

		pose.pushPose();
		pose.scale((float) (age / 2 + 0.5), 1F, (float) (age / 2 + 0.5));
		double scale = 0.075D;
		int colorInner = charge
				? ((int)(0x60 * age) << 16) | ((int)(0x50 * age) << 8) | (int) (0x30 * age)
				: ((int)(0x30 * age) << 16) | ((int)(0x25 * age) << 8) | (int) (0x10 * age);
		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR);
		LegacyBulletBeamRenderer.prontBeamRandomSolid(pose, tess, bullet.beamLength, colorInner, colorInner, (bullet.tickCount + bullet.getId()) / 2, (int)(bullet.beamLength / 2 + 1), (float) scale * 4F, 2, 0.0625F);
		pose.popPose();

		pose.scale((float) (age * 2), 1F, (float) (age * 2));
		pose.translate(0, bullet.beamLength, 0);
		pose.mulPose(Axis.ZP.rotationDegrees(-90));
		LegacyProjectileEntityRenderer.renderTracerGeometry(pose.last(), buffers.getBuffer(HbmUnlitRenderTypes.TRACER), bullet.beamLength,
				0.03125D, 0.03125D * 0.25D, charge ? 0xFFF0A0 : 0xFFBF00, 0xFFFFFF, LightTexture.FULL_BRIGHT);

		pose.popPose();
	}

	public static void renderNI4NIBolt(LegacyBulletBeamEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers) {
		double age = Mth.clamp(1D - ((double) bullet.tickCount - 2 + interp) / (double) bullet.config.expires, 0, 1);

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180 - bullet.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-bullet.getXRot() - 90));

		double scale = 5D;
		pose.scale((float) (age * scale), 1F, (float) (age * scale));
		pose.translate(0, bullet.beamLength, 0);
		pose.mulPose(Axis.ZP.rotationDegrees(-90));
		LegacyProjectileEntityRenderer.renderTracerGeometry(pose.last(), buffers.getBuffer(HbmUnlitRenderTypes.TRACER), bullet.beamLength,
				0.03125D, 0.03125D * 0.25D, 0xAAD2E5, 0xffffff, LightTexture.FULL_BRIGHT);

		pose.popPose();
	}
}
