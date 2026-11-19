package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.blocks.ArcWelderBlock;
import com.atomiclegacy.tileentity.ArcWelderBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ArcWelderBlockEntityRenderer implements LongRangeBlockEntityRenderer<ArcWelderBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_arc_welder_world");
    private static final AABB[] LOCAL_BOUNDS = buildArcWelderBounds();

    public ArcWelderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(ArcWelderBlockEntity arcWelder, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = arcWelder.getBlockState();
        Direction facing = arcWelderFacing(state);

        poseStack.pushPose();
        applyArcWelderTransform(poseStack, facing);
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        // [parity d3: RenderArcWelder - display item]
        if (!arcWelder.getDisplay().isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0625D * 2.5D, 1.125D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            poseStack.scale(1.5F, 1.5F, 1.5F);
            LegacyFramedItemRender.render(arcWelder.getDisplay(), arcWelder.getLevel(), poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArcWelderBlockEntity blockEntity) {
        Direction facing = arcWelderFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildArcWelderBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyArcWelderTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.5F, 1.0F, 2.279531F, 1.5F);
        }
        return bounds;
    }

    private static void applyArcWelderTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Arc welder facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, 0.0F);
    }

    private static Direction arcWelderFacing(BlockState state) {
        if (!state.hasProperty(ArcWelderBlock.FACING)) {
            throw new IllegalStateException("Arc welder state is missing its owned facing property: " + state);
        }
        return state.getValue(ArcWelderBlock.FACING);
    }
}
