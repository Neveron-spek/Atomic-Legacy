package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.IndustrialGeneratorBlock;
import com.atomiclegacy.tileentity.IndustrialGeneratorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * [parity h1] RenderIGenerator: translate(x + 0.5, y, z + 0.5), rotation by metadata (NORTH 180, WEST 270, SOUTH 0,
 * EAST 90), translate(0, 0, -1) then (0, 0, 1), scale 1/6, translate(0, 0, -0.5); igen_tex on parts Body and Rotor,
 * GL_CULL_FACE disabled (no-cull cutout). getMaxRenderDistanceSquared 65536 -> long-range renderer.
 */
public final class IndustrialGeneratorBlockEntityRenderer implements LongRangeBlockEntityRenderer<IndustrialGeneratorBlockEntity> {
    private static final LegacyEntityObjMesh IGEN = LegacyEntityObjMesh.load(AtomicLegacy.id("models/entity/legacy_igen.obj"));
    private static final ResourceLocation IGEN_TEX = AtomicLegacy.id("textures/models/machines/igen.png");

    public IndustrialGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(IndustrialGeneratorBlockEntity generator, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        Direction facing = generator.getBlockState().hasProperty(IndustrialGeneratorBlock.FACING)
                ? generator.getBlockState().getValue(IndustrialGeneratorBlock.FACING) : Direction.SOUTH;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> { }
        }
        poseStack.scale(1.0F / 6.0F, 1.0F / 6.0F, 1.0F / 6.0F);
        poseStack.translate(0.0D, 0.0D, -0.5D);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(IGEN_TEX));
        IGEN.renderGroup("Body", poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF);
        IGEN.renderGroup("Rotor", poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(IndustrialGeneratorBlockEntity generator) {
        // igen.obj spans x -1.5..1.5, y 0..3, z -2.5..3.5; after scale 1/6 and z - 0.5 it stays inside the block.
        return new AABB(generator.getBlockPos());
    }
}
