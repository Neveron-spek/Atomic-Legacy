package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyFbiEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** FBI keeps the old ModelFBI (vanilla biped) with both arms aimed. */
public final class LegacyFbiEntityRenderer
        extends MobRenderer<LegacyFbiEntity, LegacyFbiModel> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/entity/fbi.png");

    public LegacyFbiEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyFbiModel(context.bakeLayer(LegacyFbiModel.LAYER)), 0.5F);
        // [parity D1] RenderBiped drew the worn armour and the held item (addRandomArmor gear, revolver / SPAS-12)
        this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE_INNER_ARMOR)),
                new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(LegacyFbiEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
        model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
        model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyFbiEntity entity) {
        return TEXTURE;
    }
}
