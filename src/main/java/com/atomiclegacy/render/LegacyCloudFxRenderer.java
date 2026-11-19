package com.atomiclegacy.render;

import net.minecraft.client.renderer.culling.Frustum;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyCloudFxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * [parity R1: FX entities] 1.7.10 MultiCloudRenderer: one of eight item icons chosen by
 * particleAge / maxAge, drawn as five camera-facing quads scaled 0.5 * 7.5 with offsets from
 * new Random(100) and greys from new Random(entity.hashCode()) (the entity id in 1.7.10).
 */
public final class LegacyCloudFxRenderer extends EntityRenderer<LegacyCloudFxEntity> {
    private final ResourceLocation[] frames = new ResourceLocation[8];

    public LegacyCloudFxRenderer(EntityRendererProvider.Context context, String prefix) {
        super(context);
        for (int i = 0; i < 8; i++) {
            frames[i] = AtomicLegacy.id("textures/item/" + prefix + (i + 1) + ".png");
        }
    }

    /**
     * [parity S4: culling] EntityModFX sets ignoreFrustumCheck; the finite replacement is the exact radius of the five
     * billboards: the offsets come from the fixed new Random(100) sequence used by render(), each quad corner is at
     * most sqrt(0.5^2 + 0.75^2) from its centre, and everything is scaled by 0.5 * 7.5.
     */
    private static final double VISUAL_RADIUS;

    static {
        Random rand = new Random(100);
        double max = 0.0D;
        double corner = Math.sqrt(0.5D * 0.5D + 0.75D * 0.75D);
        for (int i = 0; i < 5; i++) {
            double dX = (rand.nextGaussian() - 1.0D) * 0.15D;
            double dY = (rand.nextGaussian() - 1.0D) * 0.15D;
            double dZ = (rand.nextGaussian() - 1.0D) * 0.15D;
            float size = (float) (rand.nextDouble() * 0.5D + 0.25D);
            max = Math.max(max, Math.sqrt(dX * dX + dY * dY + dZ * dZ) + size * corner);
        }
        VISUAL_RADIUS = Math.nextUp(max * 0.5D * 7.5D) + 0.01D;
    }

    @Override
    public boolean shouldRender(LegacyCloudFxEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), VISUAL_RADIUS),
                entity.getBoundingBox(), frustum, cameraX, cameraY, cameraZ);
    }

    public static EntityRendererProvider<LegacyCloudFxEntity> provider(String prefix) {
        return context -> new LegacyCloudFxRenderer(context, prefix);
    }

    private int frame(LegacyCloudFxEntity fx) {
        int age = fx.particleAge;
        int eighth = fx.maxAge / 8;
        for (int i = 7; i >= 1; i--) {
            if (age >= eighth * i && (i == 7 ? age <= fx.maxAge : age < eighth * (i + 1))) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void render(LegacyCloudFxEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        poseStack.pushPose();
        poseStack.scale(0.5F * 7.5F, 0.5F * 7.5F, 0.5F * 7.5F);
        Random randy = new Random(entity.getId());
        Random rand = new Random(100);
        for (int i = 0; i < 5; i++) {
            float grey = (float) (1.0D - randy.nextInt(10) * 0.05D);
            double dX = (rand.nextGaussian() - 1.0D) * 0.15D;
            double dY = (rand.nextGaussian() - 1.0D) * 0.15D;
            double dZ = (rand.nextGaussian() - 1.0D) * 0.15D;
            float size = (float) (rand.nextDouble() * 0.5D + 0.25D);
            poseStack.translate(dX, dY, dZ);
            poseStack.scale(size, size, size);
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            Matrix4f pose = poseStack.last().pose();
            PoseStack.Pose last = poseStack.last();
            vertex(consumer, pose, last, -0.5F, -0.25F, 0.0F, 1.0F, grey, packedLight);
            vertex(consumer, pose, last, 0.5F, -0.25F, 1.0F, 1.0F, grey, packedLight);
            vertex(consumer, pose, last, 0.5F, 0.75F, 1.0F, 0.0F, grey, packedLight);
            vertex(consumer, pose, last, -0.5F, 0.75F, 0.0F, 0.0F, grey, packedLight);
            poseStack.popPose();
            poseStack.scale(1.0F / size, 1.0F / size, 1.0F / size);
            poseStack.translate(-dX, -dY, -dZ);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, PoseStack.Pose last, float x, float y,
                               float u, float v, float grey, int light) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(grey, grey, grey, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(last, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyCloudFxEntity entity) {
        return frames[frame(entity)];
    }
}
