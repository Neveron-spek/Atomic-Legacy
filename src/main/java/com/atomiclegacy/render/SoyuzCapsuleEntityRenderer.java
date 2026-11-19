package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.SoyuzCapsuleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class SoyuzCapsuleEntityRenderer extends EntityRenderer<SoyuzCapsuleEntity> {
    // [parity Y1] RenderSoyuzCapsule: byte copy of models/soyuz_lander.obj, "Capsule" with soyuz_lander_tex and
    // "Chute" with soyuz_chute_tex, GL_CULL_FACE enabled.
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/entity/legacy_soyuz_lander.obj"));
    private static final ResourceLocation LANDER_TEX = AtomicLegacy.id("textures/models/soyuz_capsule/soyuz_lander.png");
    private static final ResourceLocation CHUTE_TEX = AtomicLegacy.id("textures/models/soyuz_capsule/soyuz_chute.png");
    private static final double SWAY_RADIUS = rotationSafeRadius(
            BOUNDS_MODEL.maximumFaceRadius(0.0D, 7.0D, 0.0D));

    public SoyuzCapsuleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SoyuzCapsuleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        double time = entity.level().getGameTime() + partialTick;
        float swayZ = Mth.sin((float) (time * 0.05D)) * 5.0F;
        float swayX = Mth.sin((float) (time * 0.05D + Math.PI * 0.5D)) * 5.0F;
        poseStack.translate(0.0D, 7.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(swayZ));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(swayX));
        poseStack.translate(0.0D, -7.0D, 0.0D);
        BOUNDS_MODEL.renderGroup("Capsule", poseStack, bufferSource.getBuffer(RenderType.entityCutout(LANDER_TEX)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        BOUNDS_MODEL.renderGroup("Chute", poseStack, bufferSource.getBuffer(RenderType.entityCutout(CHUTE_TEX)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(SoyuzCapsuleEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // render() rotates the authored mesh about the exact (0, 7, 0)
        // pivot.  Rotations preserve every vertex's distance from that pivot.
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY() + 7.0D, entity.getZ(), SWAY_RADIUS);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(SoyuzCapsuleEntity entity) {
        return LANDER_TEX;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
