package com.atomiclegacy.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.B92BeamEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * [parity c1] 1.7.10 com.hbm.render.entity.projectile.RenderBeam5 (EntityB92Beam): nine nested untextured square
 * tubes of half width 0 .. 0.175 and length 2 along the flight direction, blue with a white core, drawn with
 * glBlendFunc(GL_SRC_ALPHA, GL_ONE) and no culling. The Tessellator colours are used as they are (no shading).
 */
public final class B92BeamRenderer extends EntityRenderer<B92BeamEntity> {

	/** RenderBeam5#getEntityTexture (never bound: GL_TEXTURE_2D is disabled) */
	private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/models/plasma_beam.png");
	private static final float RADIUS = 0.175F;
	private static final int DISTANCE = 2;
	/** Farthest vertex from the entity origin: the tube corner at (0.175, 0.175, 2). */
	private static final double MODEL_RADIUS = Math.nextUp(Math.sqrt(RADIUS * RADIUS * 2.0D + DISTANCE * DISTANCE));

	public B92BeamRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(B92BeamEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
		AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), MODEL_RADIUS + 0.01D);
		if(!entity.shouldRender(cameraX, cameraY, cameraZ)) return false;
		return EntityRenderBounds.shouldRender(entity, bounds, entity.getBoundingBox(), frustum, cameraX, cameraY, cameraZ);
	}

	@Override
	public void render(B92BeamEntity rocket, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
		float radius = RADIUS;
		int distance = DISTANCE;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(rocket.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-rocket.getXRot()));

		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR);
		Matrix4f m = pose.last().pose();

		for(float o = 0; o <= radius; o += radius / 8) {
			float color = 1f - (o * 8.333f);
			if(color < 0)
				color = 0;
			// red = false, green = false, blue = true
			vertex(tess, m, 0 + o, 0 - o, 0, color);
			vertex(tess, m, 0 + o, 0 + o, 0, color);
			vertex(tess, m, 0 + o, 0 + o, 0 + distance, color);
			vertex(tess, m, 0 + o, 0 - o, 0 + distance, color);

			vertex(tess, m, 0 - o, 0 - o, 0, color);
			vertex(tess, m, 0 + o, 0 - o, 0, color);
			vertex(tess, m, 0 + o, 0 - o, 0 + distance, color);
			vertex(tess, m, 0 - o, 0 - o, 0 + distance, color);

			vertex(tess, m, 0 - o, 0 + o, 0, color);
			vertex(tess, m, 0 - o, 0 - o, 0, color);
			vertex(tess, m, 0 - o, 0 - o, 0 + distance, color);
			vertex(tess, m, 0 - o, 0 + o, 0 + distance, color);

			vertex(tess, m, 0 + o, 0 + o, 0, color);
			vertex(tess, m, 0 - o, 0 + o, 0, color);
			vertex(tess, m, 0 - o, 0 + o, 0 + distance, color);
			vertex(tess, m, 0 + o, 0 + o, 0 + distance, color);
		}
		pose.popPose();
	}

	private static void vertex(VertexConsumer tess, Matrix4f m, float x, float y, float z, float color) {
		tess.addVertex(m, x, y, z).setColor(color, color, 1F, 1F);
	}

	@Override
	public ResourceLocation getTextureLocation(B92BeamEntity entity) {
		return TEXTURE;
	}
}
