package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * [parity H2: grenade_universal] 1.7.10 RenderGrenadeUniversal: scale 0.0625, interpolated yaw around Y, the spin
 * around X, -80 degrees around Z once the grenade has bounced, then ItemRenderGrenade.renderGrenade(stack, null)
 * (body only, without spoon, ring or cap).
 */
public final class UniversalGrenadeEntityRenderer extends EntityRenderer<UniversalGrenadeEntity> {
    public UniversalGrenadeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(UniversalGrenadeEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (!entity.grenadeStack().isEmpty()) {
            poseStack.pushPose();
            float scale = 0.0625F;
            poseStack.scale(scale, scale, scale);
            float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            double spin = entity.prevSpin + (entity.spin - entity.prevSpin) * partialTick;
            poseStack.mulPose(Axis.XP.rotationDegrees((float) spin));
            if (entity.getBounces() > 0) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-80.0F));
            }
            UniversalGrenadeItemRenderer.renderThrown(entity.grenadeStack(), poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(UniversalGrenadeEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // [parity S4: culling] model-derived radius of the thrown shell (see UniversalGrenadeItemRenderer.thrownRadius).
        if (entity.grenadeStack().isEmpty()) {
            return EntityRenderBounds.shouldRender(entity, entity.getBoundingBox(), frustum, cameraX, cameraY, cameraZ);
        }
        double radius = UniversalGrenadeItemRenderer.thrownRadius(entity.grenadeStack());
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius),
                entity.getBoundingBox(), frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(UniversalGrenadeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    static synchronized void invalidateBounds() {
        UniversalGrenadeItemRenderer.invalidateGroundBounds();
    }
}
