package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.FluidPumpBlock;
import com.atomiclegacy.tileentity.FluidPumpBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * [parity Y1] RenderFluidPump (1.7.10): ResourceManager.fluid_pump (models/network/fluid_diode.obj, byte copy) with
 * fluid_pump_tex, GL_CULL_FACE and GL_LIGHTING enabled, rotated by metadata 2..5 (= the port's FACING).
 */
public class FluidPumpBlockEntityRenderer implements BlockEntityRenderer<FluidPumpBlockEntity> {
    static final LegacyEntityObjMesh FLUID_PUMP = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/entity/legacy_fluid_diode.obj"));
    static final ResourceLocation FLUID_PUMP_TEX = AtomicLegacy.id("textures/models/network/fluid_diode.png");

    public FluidPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidPumpBlockEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        float rotation = switch (pump.getBlockState().getValue(FluidPumpBlock.FACING)) {
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        FLUID_PUMP.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutout(FLUID_PUMP_TEX)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
