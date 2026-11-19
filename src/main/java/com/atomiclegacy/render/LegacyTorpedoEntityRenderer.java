package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyTorpedoEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** EntityTorpedo branch of RenderBoxcar: tilts by min(85, age * 3) degrees around X; ignoreFrustumCheck. */
public final class LegacyTorpedoEntityRenderer extends EntityRenderer<LegacyTorpedoEntity> {
    /** ResourceManager.torpedo / torpedo_tex */
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/torpedo.obj"));
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/models/weapons/torpedo.png");

    public LegacyTorpedoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyTorpedoEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float f = entity.tickCount + partialTick;
        poseStack.mulPose(Axis.XP.rotationDegrees(Math.min(85, f * 3)));
        MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyTorpedoEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // [parity R2: culling bounds] 1.7.10 sets ignoreFrustumCheck; the finite OBJ radius (rotation-invariant,
        // so it also covers the torpedo tilt) keeps every visible vertex inside the frustum test, and the entity's
        // 25,000 squared-distance limit is applied by EntityRenderBounds' distance rule.
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), MODEL.maxRadius()), entity.getBoundingBox(),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTorpedoEntity entity) {
        return TEXTURE;
    }
}
