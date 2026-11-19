package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBuildingEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** EntityBuilding branch of RenderBoxcar: building.obj with GL_CULL_FACE disabled, lit; ignoreFrustumCheck. */
public final class LegacyBuildingEntityRenderer extends EntityRenderer<LegacyBuildingEntity> {
    /** ResourceManager.building / building_tex */
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/building.obj"));
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/models/weapons/building.png");

    public LegacyBuildingEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyBuildingEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyBuildingEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // [parity R2: culling bounds] 1.7.10 sets ignoreFrustumCheck; the finite OBJ radius (rotation-invariant,
        // so it also covers the torpedo tilt) keeps every visible vertex inside the frustum test, and the entity's
        // 25,000 squared-distance limit is applied by EntityRenderBounds' distance rule.
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), MODEL.maxRadius()), entity.getBoundingBox(),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBuildingEntity entity) {
        return TEXTURE;
    }
}
