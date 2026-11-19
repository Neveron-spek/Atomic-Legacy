package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** [parity U1: network_separator] RenderNetworkSeparator: network_separator.obj rotated by the block direction. */
public final class NetworkSeparatorBlockEntityRenderer implements LongRangeBlockEntityRenderer<NetworkSeparatorBlockEntity> {
    static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/network_separator");

    public NetworkSeparatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(NetworkSeparatorBlockEntity separator, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // metadata - offset: 2 (NORTH) 90, 4 (WEST) 180, 3 (SOUTH) 270, 5 (EAST) 0 around +Y
        switch (separator.facing()) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        MachineModelRenderer.render(MachineModelRenderer.model(MODEL), poseStack, bufferSource, separator.getBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(NetworkSeparatorBlockEntity separator) {
        // TileEntityNetworkSeparator#getRenderBoundingBox
        return new AABB(separator.getBlockPos().getX() - 1, separator.getBlockPos().getY(), separator.getBlockPos().getZ() - 1,
                separator.getBlockPos().getX() + 2, separator.getBlockPos().getY() + 2, separator.getBlockPos().getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256; // getMaxRenderDistanceSquared 65536
    }
}
