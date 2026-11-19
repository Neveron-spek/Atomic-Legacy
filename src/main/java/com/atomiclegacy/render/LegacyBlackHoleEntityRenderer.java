package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBlackHoleEntity;
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
public final class LegacyBlackHoleEntityRenderer extends EntityRenderer<LegacyBlackHoleEntity> {
    private static final ResourceLocation CORE = AtomicLegacy.id("textures/models/blackhole.png");
    private static final ResourceLocation SWIRL = AtomicLegacy.id("textures/entity/bhole.png");
    private static final ResourceLocation DISC = AtomicLegacy.id("textures/entity/bholedisc.png");
    /* RenderBlackHole#renderDisc: per layer, pass j=0 with glBlendFunc(770, 771), pass j=1 with (GL_SRC_ALPHA, GL_ONE). */
    private static final RenderType DISC_TYPE = HbmUnlitRenderTypes.blend(DISC);
    private static final RenderType DISC_GLOW = HbmUnlitRenderTypes.additive(DISC);
    private static final RenderType CORE_TYPE = HbmUnlitRenderTypes.opaque(CORE);
    private static final RenderType SWIRL_TYPE = HbmUnlitRenderTypes.blend(SWIRL);

    public LegacyBlackHoleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LegacyBlackHoleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float size = entity.size();
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        LegacyEntityObjMesh.ORIGINAL_SPHERE.renderAllTriangles(poseStack, bufferSource.getBuffer(CORE_TYPE),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        poseStack.pushPose();
        if (entity.isRaging()) {
            renderSwirl(entity, partialTick, poseStack, bufferSource, size);
        } else {
            renderDisc(entity, partialTick, poseStack, bufferSource, size);
        }
        poseStack.popPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        renderJets(poseStack.last().pose(), bufferSource.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR), size);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderJets(Matrix4f matrix, VertexConsumer vertices, float size) {
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 12; i++) {
                double angle = side * i * Math.PI / 6;
                double next = side * (i + 1) * Math.PI / 6;
                float radius = size * 0.5F;
                float height = side * size * 10F;
                vertices.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 89);
                vertices.addVertex(matrix, radius * (float) Math.cos(angle), height, radius * (float) Math.sin(angle)).setColor(255, 255, 255, 0);
                vertices.addVertex(matrix, radius * (float) Math.cos(next), height, radius * (float) Math.sin(next)).setColor(255, 255, 255, 0);
                vertices.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 89);
            }
        }
    }

    @Override
    public boolean shouldRender(LegacyBlackHoleEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(
                entity, visualBounds(entity), frustum, cameraX, cameraY, cameraZ);
    }

    private static AABB visualBounds(LegacyBlackHoleEntity entity) {
        // The jet rim is ten model units above or below the center.
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        double extent = Math.nextUp(Math.abs(entity.size()) * Math.hypot(10, 0.5) * (1.0D + gamma64));
        return new AABB(
                entity.getX() - extent, entity.getY() - extent, entity.getZ() - extent,
                entity.getX() + extent, entity.getY() + extent, entity.getZ() + extent);
    }

    private static void renderDisc(LegacyBlackHoleEntity entity, float partialTick, PoseStack poseStack,
                                   MultiBufferSource buffers, float size) {
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        for (int layer = 0; layer < 15; layer++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-(entity.tickCount + partialTick)
                    * (float) Math.pow(layer + 1, 1.25)));
            float radius = size * (3.0F - layer * 0.175F);
            int red = layer <= 5 ? 255 : Math.round(255 * (1 - (layer - 6) / 9F));
            int green = layer < 5 ? Math.round(255 * (0.125F + layer / 10F)) : red;
            int blue = layer <= 5 ? 0 : Math.min(255, Math.round(255 * (layer - 6) / 5F));
            /* Switching the shared render type flushes the previous batch, keeping the
             * original per-layer blend/additive draw order. */
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer consumer = buffers.getBuffer(pass == 0 ? DISC_TYPE : DISC_GLOW);
                for (int i = 0; i < 16; i++) {
                    discVertex(poseStack.last(), consumer, i, radius, false, pass, red, green, blue);
                    discVertex(poseStack.last(), consumer, i, radius, true, pass, red, green, blue);
                    discVertex(poseStack.last(), consumer, i + 1, radius, true, pass, red, green, blue);
                    discVertex(poseStack.last(), consumer, i + 1, radius, false, pass, red, green, blue);
                }
            }
            poseStack.popPose();
        }
    }

    private static void discVertex(PoseStack.Pose pose, VertexConsumer consumer, int segment,
                                   float radius, boolean outer, int pass, int red, int green, int blue) {
        double angle = -segment * Math.PI / 8;
        float x = (float) Math.cos(angle) * (outer ? 2 : 1);
        float z = (float) Math.sin(angle) * (outer ? 2 : 1);
        if (outer) {
            vertex(consumer, pose, pose.pose(), x * radius, 0, z * radius,
                    0.5F + x * 0.25F, 0.5F + z * 0.25F, red, green, blue, 0);
        } else if (pass == 0) {
            vertex(consumer, pose, pose.pose(), x * radius, 0, z * radius,
                    0.5F + x * 0.25F, 0.5F + z * 0.25F, red, green, blue, 1);
        } else {
            vertex(consumer, pose, pose.pose(), x * radius, 0, z * radius,
                    0.5F + x * 0.25F, 0.5F + z * 0.25F, 255, 255, 255, 0.75F);
        }
    }

    private static void renderSwirl(LegacyBlackHoleEntity entity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffers, float size) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        poseStack.mulPose(Axis.YP.rotationDegrees(-(entity.tickCount + partialTick) * 5.0F));
        LegacyVortexEntityRenderer.renderSwirlRings(poseStack.last(), buffers.getBuffer(SWIRL_TYPE), size, 0xE8, 0x39, 0x0D, 1);
        LegacyVortexEntityRenderer.renderSwirlRings(poseStack.last(), buffers.getBuffer(LegacyVortexEntityRenderer.SWIRL_GLOW), size, 255, 255, 255, 0.25F);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f matrix,
                               double x, double y, double z, float u, float v, int red, int green, int blue, float alpha) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setUv(u, v)
                .setColor(red, green, blue, Math.max(0, Math.min(255, Math.round(alpha * 255.0F))));
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBlackHoleEntity entity) {
        return CORE;
    }
}
