package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyDummyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** The old RenderDummy used an unarmored ModelBiped on a 64x32 canvas. */
public final class LegacyDummyModel extends HumanoidModel<LegacyDummyEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            AtomicLegacy.id("dummy"), "main");

    public LegacyDummyModel(net.minecraft.client.model.geom.ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F), 64, 32);
    }
}
