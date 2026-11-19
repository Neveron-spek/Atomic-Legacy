package com.atomiclegacy.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.SoliniumCloudEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public final class SoliniumCloudRenderer extends EntityRenderer<SoliniumCloudEntity> {
    private static final LegacyEntityObjMesh SPHERE = LegacyEntityObjMesh.load(AtomicLegacy.id("models/entity/sphere_new.obj"));
    private static final RenderType CORE = sphereLayer("solinium_core", false);
    private static final RenderType GLOW = sphereLayer("solinium_glow", true);
    // [parity M3: dfc] RenderCloudRainbow: models/Sphere.obj, culled, untextured, random colour per layer.
    private static final LegacyEntityObjMesh RAINBOW_SPHERE = LegacyEntityObjMesh.load(AtomicLegacy.id("models/entity/sphere.obj"));
    private static final RenderType RAINBOW_CORE = RenderType.create("rainbow_core", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 4096, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));

    private static final double SPHERE_RADIUS = Math.nextUp(SPHERE.maxRadius()); // [parity S4: culling]
    private static final double RAINBOW_RADIUS = Math.nextUp(RAINBOW_SPHERE.maxRadius());

    /**
     * [parity S4: culling] ignoreFrustumCheck replaced by the finite sphere: solinium core scaled (age + 1) with three
     * 1.025 shells; rainbow layers scaled age (largest layer 1.0). The rainbow cloud additionally keeps its 1.7.10
     * distance < 25000 limit, which the shared policy's collision-box size rule would otherwise widen.
     */
    @Override
    public boolean shouldRender(SoliniumCloudEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        double radius = entity.rainbow()
                ? RAINBOW_RADIUS * entity.age()
                : SPHERE_RADIUS * (entity.age() + 1.0D) * 1.025D * 1.025D * 1.025D;
        AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), Math.nextUp(radius) + 0.01D);
        if (entity.rainbow() && !entity.shouldRender(cameraX, cameraY, cameraZ)) return false;
        return EntityRenderBounds.shouldRender(entity, bounds, entity.getBoundingBox(), frustum, cameraX, cameraY, cameraZ);
    }

    public SoliniumCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static RenderType sphereLayer(String name, boolean glow) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 4096,
                false, false, RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                        .setTransparencyState(glow ? RenderStateShard.LIGHTNING_TRANSPARENCY : RenderStateShard.NO_TRANSPARENCY)
                        .setCullState(glow ? RenderStateShard.CULL : RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }

    @Override
    public void render(SoliniumCloudEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (entity.rainbow()) {
            renderRainbow(entity, pose, buffers);
            return;
        }
        pose.pushPose();
        float scale = entity.age() + partialTick;
        pose.scale(scale, scale, scale);
        SPHERE.renderAllPositionColorTriangles(pose, buffers.getBuffer(CORE), 0xFF27FFDA);
        var shell = buffers.getBuffer(GLOW);
        for (int layer = 0; layer < 3; layer++) {
            pose.scale(1.025F, 1.025F, 1.025F);
            SPHERE.renderAllPositionColorTriangles(pose, shell, 0x2027FFDA);
        }
        pose.popPose();
    }

    private static void renderRainbow(SoliniumCloudEntity entity, PoseStack pose, MultiBufferSource buffers) {
        var random = entity.level().random;
        pose.pushPose();
        float age = entity.age();
        pose.scale(age, age, age);
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        RAINBOW_SPHERE.renderAllPositionColorTriangles(pose, buffers.getBuffer(RAINBOW_CORE), randomColor(random));
        pose.popPose();
        var shell = buffers.getBuffer(GLOW);
        for (float i = 0.6F; i <= 1F; i += 0.1F) {
            pose.pushPose();
            pose.scale(i, i, i);
            RAINBOW_SPHERE.renderAllPositionColorTriangles(pose, shell, randomColor(random));
            pose.popPose();
        }
        pose.popPose();
    }

    private static int randomColor(net.minecraft.util.RandomSource random) {
        return 0xFF000000 | (random.nextInt(0x100) << 16) | (random.nextInt(0x100) << 8) | random.nextInt(0x100);
    }

    @Override
    public ResourceLocation getTextureLocation(SoliniumCloudEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
