package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * [weapons batch E2] 1.7.10 LegoClient.RENDER_LIGHTNING (tesla cannon beams) and RENDER_LIGHTNING_SUB (energy_tesla_ir_sub):
 * three BeamPronter RANDOM/SOLID beams, full bright, GL_SRC_ALPHA/GL_ONE. The two only differ in the width scale offset.
 */
public final class LegacyLightningBeamRender {

	private LegacyLightningBeamRender() {
	}

	public static void render(LegacyBulletBeamEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, double widthOffset) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180 - bullet.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-bullet.getXRot() - 90));
		double age = Mth.clamp(1D - ((double) bullet.tickCount - 2 + interp) / (double) bullet.config.expires, 0, 1);
		pose.scale((float) (age / 2 + widthOffset), 1F, (float) (age / 2 + widthOffset));
		double scale = 0.075D;
		int colorInner = ((int)(0x20 * age) << 16) | ((int)(0x20 * age) << 8) | (int) (0x40 * age);
		int colorOuter = ((int)(0x40 * age) << 16) | ((int)(0x40 * age) << 8) | (int) (0x80 * age);
		int segments = (int)(bullet.beamLength / 2 + 1);
		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR);
		LegacyBulletBeamRenderer.prontBeamRandomSolid(pose, tess, bullet.beamLength, colorInner, colorInner, bullet.tickCount / 3, segments, (float) scale * 1F, 4, 0.25F);
		LegacyBulletBeamRenderer.prontBeamRandomSolid(pose, tess, bullet.beamLength, colorOuter, colorOuter, bullet.tickCount, segments, (float) scale * 7F, 2, 0.0625F);
		LegacyBulletBeamRenderer.prontBeamRandomSolid(pose, tess, bullet.beamLength, colorOuter, colorOuter, bullet.tickCount / 2, segments, (float) scale * 7F, 2, 0.0625F);
		pose.popPose();
	}
}
