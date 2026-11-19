package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** [parity T1: flow_control_pump] RenderFlowControlPump: Pump, optional RailTop/RailBottom, Rotor spun around -Z at y 0.5. */
public final class FlowControlPumpBlockEntityRenderer implements LongRangeBlockEntityRenderer<FlowControlPumpBlockEntity> {
    static final ModelResourceLocation PUMP = MachineModelRenderer.standalone("block/flow_control_pump_pump");
    static final ModelResourceLocation RAIL_TOP = MachineModelRenderer.standalone("block/flow_control_pump_rail_top");
    static final ModelResourceLocation RAIL_BOTTOM = MachineModelRenderer.standalone("block/flow_control_pump_rail_bottom");
    static final ModelResourceLocation ROTOR = MachineModelRenderer.standalone("block/flow_control_pump_rotor");

    public FlowControlPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PUMP);
        event.register(RAIL_TOP);
        event.register(RAIL_BOTTOM);
        event.register(ROTOR);
    }

    @Override
    public void render(FlowControlPumpBlockEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        BlockState state = pump.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // metadata - offset: 2 (NORTH) 90, 4 (WEST) 180, 3 (SOUTH) 270, 5 (EAST) 0 around +Y
        switch (pump.facing()) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        MachineModelRenderer.render(MachineModelRenderer.model(PUMP), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (pump.topRail) MachineModelRenderer.render(MachineModelRenderer.model(RAIL_TOP), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (pump.bottomRail) MachineModelRenderer.render(MachineModelRenderer.model(RAIL_BOTTOM), poseStack, bufferSource, state, packedLight, packedOverlay);
        float rotor = pump.prevRotor + (pump.rotor - pump.prevRotor) * partialTick;
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees(rotor));
        poseStack.translate(0.0F, -0.5F, 0.0F);
        MachineModelRenderer.render(MachineModelRenderer.model(ROTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FlowControlPumpBlockEntity pump) {
        // TileEntityFlowControlPump#getRenderBoundingBox
        return new AABB(pump.getBlockPos().getX() - 1, pump.getBlockPos().getY(), pump.getBlockPos().getZ() - 1,
                pump.getBlockPos().getX() + 2, pump.getBlockPos().getY() + 1, pump.getBlockPos().getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256; // getMaxRenderDistanceSquared 65536
    }
}
