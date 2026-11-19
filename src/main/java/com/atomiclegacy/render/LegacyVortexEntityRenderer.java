package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyVortexEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** Renderer equivalent to the old black-hole sphere plus its blue vortex disc. */
public final class LegacyVortexEntityRenderer extends EntityRenderer<LegacyVortexEntity> {
    private static final ResourceLocation CORE = AtomicLegacy.id("textures/models/blackhole.png");
    private static final ResourceLocation SWIRL = AtomicLegacy.id("textures/entity/bhole.png");
    private static final RenderType CORE_TYPE = HbmUnlitRenderTypes.opaque(CORE);
    /* RenderBlackHole#renderSwirl: pass j=0 with glBlendFunc(770, 771), pass j=1 with (GL_SRC_ALPHA, GL_ONE). */
    private static final RenderType SWIRL_TYPE = HbmUnlitRenderTypes.blend(SWIRL);
    static final RenderType SWIRL_GLOW = HbmUnlitRenderTypes.additive(SWIRL);

    public LegacyVortexEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LegacyVortexEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float size = entity.size();
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        LegacyEntityObjMesh.ORIGINAL_SPHERE.renderAllTriangles(poseStack, bufferSource.getBuffer(CORE_TYPE),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        renderSwirl(entity, partialTick, poseStack, bufferSource, size);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyVortexEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(
                entity, visualBounds(entity), frustum, cameraX, cameraY, cameraZ);
    }

    private static AABB visualBounds(LegacyVortexEntity entity) {
        float innerRadius = Math.abs(entity.size()) * 3.0F;
        float outerRadius = innerRadius * 2.0F;
        /* The outer disc is the largest primitive. Every corner is exactly
         * outerRadius*sqrt(2) from the entity origin; rotations preserve that
         * norm. gamma(64) bounds the complete float quaternion/PoseStack path. */
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        double extent = Math.nextUp((double) outerRadius * Math.sqrt(2.0D) * (1.0D + gamma64));
        return new AABB(
                entity.getX() - extent, entity.getY() - extent, entity.getZ() - extent,
                entity.getX() + extent, entity.getY() + extent, entity.getZ() + extent);
    }

    private static void renderSwirl(LegacyVortexEntity entity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffers, float size) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        poseStack.mulPose(Axis.YP.rotationDegrees(-(entity.tickCount + partialTick) * 5.0F));
        renderSwirlRings(poseStack.last(), buffers.getBuffer(SWIRL_TYPE), size, 0x38, 0x98, 0xB3, 1);
        renderSwirlRings(poseStack.last(), buffers.getBuffer(SWIRL_GLOW), size, 255, 255, 255, 0.75F);
        poseStack.popPose();
    }

    static void renderSwirlRings(PoseStack.Pose pose, VertexConsumer consumer, float size,
                                 int red, int green, int blue, float opacity) {
        for (int segment = 0; segment < 16; segment++) {
            for (int ring = 0; ring < 2; ring++) {
                swirlVertex(pose, consumer, segment, ring, size, red, green, blue, opacity);
                swirlVertex(pose, consumer, segment, ring + 1, size, red, green, blue, opacity);
                swirlVertex(pose, consumer, segment + 1, ring + 1, size, red, green, blue, opacity);
                swirlVertex(pose, consumer, segment + 1, ring, size, red, green, blue, opacity);
            }
        }
    }

    private static void swirlVertex(PoseStack.Pose pose, VertexConsumer consumer, int segment, int edge,
                                    float size, int red, int green, int blue, float opacity) {
        double angle = -segment * Math.PI / 8;
        double radius = edge == 0 ? 0.9 : edge * 3.0;
        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        vertex(consumer, pose, pose.pose(), size * x, 0, size * z, (float) (0.5 + x / 12),
                (float) (0.5 + z / 12), edge == 0 ? 0 : red, edge == 0 ? 0 : green,
                edge == 0 ? 0 : blue, edge == 2 ? 0 : edge == 0 ? 1 : opacity);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f matrix,
                               double x, double y, double z, float u, float v, int red, int green, int blue, float alpha) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setUv(u, v)
                .setColor(red, green, blue, Math.max(0, Math.min(255, Math.round(alpha * 255.0F))));
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyVortexEntity entity) {
        return CORE;
    }
}
