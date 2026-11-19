package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyCloudTomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** [parity d2] Literal 1.7.10 RenderCloudTom: 16 segments x 5 shells, radius = age, scrolling tomblast texture. */
public final class LegacyCloudTomEntityRenderer extends EntityRenderer<LegacyCloudTomEntity> {
    public static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/models/explosion/tomblast.png");
    public static final int SEGMENTS = 16;
    public static final int SHELLS = 5;
    public static final int HEIGHT = 20;
    public static final int DEPTH = 20;

    public LegacyCloudTomEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyCloudTomEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        // The original passes its yaw argument (always 0) where a partial tick was meant: the ring grows per tick.
        double scale = entity.age + entityYaw;
        float angle = (float) Math.toRadians(360D / SEGMENTS);
        Minecraft minecraft = Minecraft.getInstance();
        // GL_TEXTURE matrix: glTranslatef(0, movement, 0)
        float movement = minecraft.player == null ? 0.0F : -(minecraft.player.tickCount + entityYaw) * 0.005F * 10;

        VertexConsumer consumer = buffers.getBuffer(HbmUnlitRenderTypes.blend(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);

        for (int i = 0; i < SEGMENTS; i++) {
            for (int j = 0; j < SHELLS; j++) {
                double mod = 1 - j * 0.025;
                double h = HEIGHT + j * 10;
                double off = 1D / j; // infinite for the first shell, as in the original

                // Vec3.createVectorHelper(scale, 0, 0).rotateAroundY(angle * i)
                float f1 = Mth.cos(angle * i);
                float f2 = Mth.sin(angle * i);
                double vx = scale * f1;
                double vz = -scale * f2;
                float x0 = (float) (vx * mod);
                float z0 = (float) (vz * mod);
                float vTop = (float) (1 + off) + movement;
                float vBottom = (float) (0 + off) + movement;
                consumer.addVertex(matrix, x0, (float) h, z0).setUv(0.0F, vTop).setColor(1.0F, 1.0F, 1.0F, 0.0F);
                consumer.addVertex(matrix, x0, -DEPTH, z0).setUv(0.0F, vBottom).setColor(1.0F, 1.0F, 1.0F, 1.0F);

                // vec.rotateAroundY(angle)
                double rx = vx * cos + vz * sin;
                double rz = vz * cos - vx * sin;
                float x1 = (float) (rx * mod);
                float z1 = (float) (rz * mod);
                consumer.addVertex(matrix, x1, -DEPTH, z1).setUv(1.0F, vBottom).setColor(1.0F, 1.0F, 1.0F, 1.0F);
                consumer.addVertex(matrix, x1, (float) h, z1).setUv(1.0F, vTop).setColor(1.0F, 1.0F, 1.0F, 0.0F);
            }
        }
    }

    /** ignoreFrustumCheck = true: the expanding ring is larger than the entity box; finite bounds cover it. */
    @Override
    public boolean shouldRender(LegacyCloudTomEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        net.minecraft.world.phys.AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), 4096.0D);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyCloudTomEntity entity) {
        return TEXTURE;
    }
}
