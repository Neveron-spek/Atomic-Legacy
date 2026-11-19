package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyGhostEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.7.10 RenderGhost: the vanilla biped with the legacy translucent texture pass. */
public final class LegacyGhostEntityRenderer
        extends MobRenderer<LegacyGhostEntity, HumanoidModel<LegacyGhostEntity>> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/entity/ghost.png");

    public LegacyGhostEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    protected RenderType getRenderType(LegacyGhostEntity entity, boolean bodyVisible, boolean translucent,
                                       boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyGhostEntity entity) {
        return TEXTURE;
    }
}
