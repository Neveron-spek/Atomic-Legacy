package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyDummyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dedicated target-dummy renderer using the old unarmored biped model. */
public final class LegacyDummyEntityRenderer
        extends MobRenderer<LegacyDummyEntity, LegacyDummyModel> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/entity/dummy.png");

    public LegacyDummyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyDummyModel(context.bakeLayer(LegacyDummyModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDummyEntity entity) {
        return TEXTURE;
    }
}
