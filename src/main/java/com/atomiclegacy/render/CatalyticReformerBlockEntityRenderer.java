package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.blocks.CatalyticReformerBlock;
import com.atomiclegacy.tileentity.CatalyticReformerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CatalyticReformerBlockEntityRenderer implements LongRangeBlockEntityRenderer<CatalyticReformerBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_catalytic_reformer");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB[] LOCAL_BOUNDS = buildCatalyticReformerBounds();

    public CatalyticReformerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    // [parity J1] RenderCatalyticReformer: polaroidID == 11 adds a spinning, sitting alicorn (HorsePronter with the
    // texture models/horse/dyx, horn and wings) holding a cigarette.
    private static final String[] DYX_PARTS = {"body", "head", "left_front_leg", "right_front_leg", "left_back_leg",
            "right_back_leg", "tail", "wings"};
    private static final java.util.Map<String, ModelResourceLocation> DYX = dyxModels();

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
        if (com.atomiclegacy.AtomicLegacy.polaroidID == 11) {
            DYX.values().forEach(event::register);
        }
    }

    private static java.util.Map<String, ModelResourceLocation> dyxModels() {
        java.util.Map<String, ModelResourceLocation> models = new java.util.HashMap<>();
        for (String part : DYX_PARTS) {
            models.put(part, MachineModelRenderer.standalone("block/trinkets/horse_dyx_" + part));
        }
        return java.util.Map.copyOf(models);
    }

    @Override
    public void render(CatalyticReformerBlockEntity reformer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = reformer.getBlockState();
        Direction facing = catalyticReformerFacing(state);

        poseStack.pushPose();
        applyCatalyticReformerTransform(poseStack, facing);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        if (com.atomiclegacy.AtomicLegacy.polaroidID == 11) {
            renderDyx(poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderDyx(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(-1.125F, 1.375F, 1.0F);
        poseStack.scale(0.125F, 0.125F, 0.125F);
        poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees((float) (System.currentTimeMillis() / 5D % 360D)));
        poseStack.translate(0.0F, 0.1F, -0.5F);

        // HorsePronter.reset(); poses of RenderCatalyticReformer (r = 60); enableHorn(); enableWings(); pront()
        float r = 60.0F;
        poseStack.pushPose();
        horseTransform(poseStack, 0.0F, 0.0F, 0.0F, 0.0F, -r, 0.0F); // id_body
        dyxPart("body", poseStack, bufferSource, state, light, overlay);
        dyxPart("head", poseStack, bufferSource, state, light, overlay, 0.0F, 1.125F, 0.375F, 0.0F, r, 0.0F);
        dyxPart("left_front_leg", poseStack, bufferSource, state, light, overlay, 0.125F, 0.75F, 0.3125F, 0.0F, r - 10.0F, 5.0F);
        dyxPart("right_front_leg", poseStack, bufferSource, state, light, overlay, -0.125F, 0.75F, 0.3125F, 0.0F, r - 10.0F, -5.0F);
        dyxPart("left_back_leg", poseStack, bufferSource, state, light, overlay, 0.125F, 0.75F, -0.25F, 0.0F, -90.0F + r, 35.0F);
        dyxPart("right_back_leg", poseStack, bufferSource, state, light, overlay, -0.125F, 0.75F, -0.25F, 0.0F, -90.0F + r, -35.0F);
        dyxPart("tail", poseStack, bufferSource, state, light, overlay, 0.0F, 1.125F, -0.4375F, 0.0F, 45.0F, 90.0F);
        dyxPart("wings", poseStack, bufferSource, state, light, overlay);
        poseStack.popPose();

        net.minecraft.world.item.Item cigarette = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                com.atomiclegacy.AtomicLegacy.id("cigarette"));
        if (cigarette != net.minecraft.world.item.Items.AIR) {
            poseStack.pushPose();
            poseStack.translate(0.02F, 1.13F, -0.42F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(90.0F));
            poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(60.0F));
            // ItemRenderer.renderItemIn2D spans [0, 1] x [0, 1] x [-1/16, 0] with the icon mirrored on +Z; the FIXED
            // generated-item transform (centred, rotated 180 degrees around Y) matches it after this offset.
            poseStack.translate(0.5F, 0.5F, -0.03125F);
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                    new net.minecraft.world.item.ItemStack(cigarette), net.minecraft.world.item.ItemDisplayContext.FIXED,
                    light, overlay, poseStack, bufferSource, null, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void dyxPart(String part, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                int light, int overlay) {
        MachineModelRenderer.render(MachineModelRenderer.model(DYX.get(part)), poseStack, bufferSource, state, light, overlay);
    }

    private static void dyxPart(String part, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                int light, int overlay, float ox, float oy, float oz, float yaw, float pitch, float roll) {
        poseStack.pushPose();
        horseTransform(poseStack, ox, oy, oz, yaw, pitch, roll);
        dyxPart(part, poseStack, bufferSource, state, light, overlay);
        poseStack.popPose();
    }

    /** HorsePronter.doTransforms: rotate yaw (Y), pitch (X), roll (Z) around the part offset. */
    private static void horseTransform(PoseStack poseStack, float ox, float oy, float oz, float yaw, float pitch, float roll) {
        poseStack.translate(ox, oy, oz);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(roll));
        poseStack.translate(-ox, -oy, -oz);
    }

    @Override
    public AABB getRenderBoundingBox(CatalyticReformerBlockEntity blockEntity) {
        Direction facing = catalyticReformerFacing(blockEntity.getBlockState());
        // Face-used catalytic_reformer.obj vertices under the render pose.
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildCatalyticReformerBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCatalyticReformerTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -2.5F, 1.5F, 7.0F, 2.5F);
        }
        return bounds;
    }

    private static void applyCatalyticReformerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Catalytic reformer facing must be horizontal: " + facing);
        }
    }

    private static Direction catalyticReformerFacing(BlockState state) {
        if (!state.hasProperty(CatalyticReformerBlock.FACING)) {
            throw new IllegalStateException("Catalytic reformer state is missing its owned facing property: " + state);
        }
        return state.getValue(CatalyticReformerBlock.FACING);
    }
}
