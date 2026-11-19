package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.SoyuzCapsuleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * [parity Y1] RenderCapsule (1.7.10): ResourceManager.soyuz_lander part "Capsule" with soyuz_lander_tex,
 * GL_CULL_FACE and GL_LIGHTING enabled. The OBJ is a byte copy of the original models/soyuz_lander.obj.
 * The rust texture of the original is selected by block metadata 3, which only the (unported) beach
 * capsule world generator places; the port's block has no such state.
 */
public class SoyuzCapsuleBlockEntityRenderer implements BlockEntityRenderer<SoyuzCapsuleBlockEntity> {
    private static final LegacyEntityObjMesh SOYUZ_LANDER = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/entity/legacy_soyuz_lander.obj"));
    private static final ResourceLocation SOYUZ_LANDER_TEX =
            AtomicLegacy.id("textures/models/soyuz_capsule/soyuz_lander.png");
    /** [parity E1] RenderCapsule: getBlockMetadata() == 3 binds soyuz_lander_rust_tex. */
    private static final ResourceLocation SOYUZ_LANDER_RUST_TEX =
            AtomicLegacy.id("textures/models/soyuz_capsule/soyuz_lander_rust.png");
    /** Rotations about the render origin preserve each vertex's distance from it. */
    private static final double RADIUS = Math.nextUp(SOYUZ_LANDER.maximumGroupFaceRadius("Capsule") * (1.0D + 0x1.0p-18));

    public SoyuzCapsuleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SoyuzCapsuleBlockEntity capsule, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.translate(0.0F, -0.25F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));
        SOYUZ_LANDER.renderGroup("Capsule", poseStack, bufferSource.getBuffer(RenderType.entityCutout(
                        capsule.getBlockState().hasProperty(com.atomiclegacy.blocks.SoyuzCapsuleBlock.RUSTY)
                                && capsule.getBlockState().getValue(com.atomiclegacy.blocks.SoyuzCapsuleBlock.RUSTY)
                                ? SOYUZ_LANDER_RUST_TEX : SOYUZ_LANDER_TEX)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzCapsuleBlockEntity capsule) {
        BlockPos pos = capsule.getBlockPos();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() - 0.25D;
        double z = pos.getZ() + 0.5D;
        return new AABB(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS);
    }
}
