package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyTurbineBladeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * [parity A1] RenderBullet with STYLE_BLADE: yaw - 90 around Y, pitch + 180 around Z, scale 1.5 (no random roll for
 * blades), then renderBlade(): an EntityItem of blade_titanium drawn with RenderItem.renderInFrame, spun by
 * System.currentTimeMillis() % 360 degrees around X about y = 0.5, rotated 90 around Y and stretched (1, 2, 1).
 * RenderItem.doRender for a flat item in frame mode: bob 0.1 (age 0, hoverStart 0), scale 0.5128205, y - 0.05,
 * renderDroppedItem turns 180 around Y and draws the 2D icon offset by (-0.5, -0.25).
 */
public final class LegacyTurbineBladeEntityRenderer extends EntityRenderer<LegacyTurbineBladeEntity> {
    private static final double VISUAL_RADIUS = 2.5D;
    private ItemStack blade;

    public LegacyTurbineBladeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyTurbineBladeEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        if (blade == null) {
            blade = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("blade_titanium")));
        }
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        pose.scale(1.5F, 1.5F, 1.5F);

        // renderBlade
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.translate(0.0D, 0.5D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(System.currentTimeMillis() % 360));
        pose.translate(0.0D, -0.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(1.0F, 2.0F, 1.0F);

        // RenderItem.doRender / renderDroppedItem, renderInFrame
        pose.translate(0.0D, 0.1D, 0.0D);
        pose.scale(0.5128205F, 0.5128205F, 0.5128205F);
        pose.translate(0.0D, -0.05D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        // 2D icon spans x -0.5..0.5, y -0.25..0.75; ItemRenderer already centres the model with (-0.5, -0.5, -0.5)
        pose.translate(0.0D, 0.25D, 0.0D);
        Minecraft.getInstance().getItemRenderer().renderStatic(blade, ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyTurbineBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // spinning icon: 1.5 * 0.5128 * (1, 2, 1) around the 0.5 pivot stays inside 2.5 blocks of the entity
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), VISUAL_RADIUS),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTurbineBladeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; // item drawn from the atlas
    }
}
