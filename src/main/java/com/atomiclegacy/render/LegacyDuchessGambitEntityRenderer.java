package com.atomiclegacy.render;

import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyDuchessGambitEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** [weapons batch D1] EntityDuchessGambit branch of RenderBoxcar (translate 0, 0, -1; culling and lighting on); ignoreFrustumCheck. */
public final class LegacyDuchessGambitEntityRenderer extends EntityRenderer<LegacyDuchessGambitEntity> {
    /** ResourceManager.duchessgambit (models/duchessgambit.obj) */
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(AtomicLegacy.id("models/entity/duchessgambit.obj"));
    /** ResourceManager.duchessgambit_tex (textures/models/duchessgambit.png, byte-identical to the boat block texture) */
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/block/duchessgambit.png");
    private static final double MODEL_RADIUS = Math.nextUp(MODEL.maxRadius()); // [parity S4: culling]

    public LegacyDuchessGambitEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyDuchessGambitEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -1.0D);
        MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyDuchessGambitEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // [parity S4: culling] ignoreFrustumCheck replaced by the finite OBJ radius around the translated origin;
        // the distance stays the entity's 1.7.10 rule (distance < 25000), sized by its own 0.25 collision box.
        AABB model = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ() - 1.0D, MODEL_RADIUS);
        return EntityRenderBounds.shouldRender(entity, model, entity.getBoundingBox(), frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDuchessGambitEntity entity) {
        return TEXTURE;
    }
}
