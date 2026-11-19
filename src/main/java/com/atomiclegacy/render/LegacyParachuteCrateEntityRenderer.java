package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyParachuteCrateEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * [parity W1: airdrop] RenderParachuteCrate: conservecrate.obj (crate_can) swaying about (0, 7, 0),
 * then the "Chute" part of soyuz_lander.obj (soyuz_chute) one block lower.
 * The port's conservecrate.obj is the original shifted by +0.5 X/Z into block space; -0.5 restores it.
 */
public final class LegacyParachuteCrateEntityRenderer extends EntityRenderer<LegacyParachuteCrateEntity> {
    private static final ModelResourceLocation CRATE = MachineModelRenderer.standalone("entity/parachute_crate");
    private static final ModelResourceLocation CHUTE = MachineModelRenderer.standalone("entity/parachute_chute");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final int HEIGHT = 7;
    private static final LegacyEntityObjMesh CRATE_BOUNDS = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/obj/blocks/conservecrate.obj"));
    private static final LegacyEntityObjMesh CHUTE_BOUNDS = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/obj/entity/soyuz_lander.obj"));
    /** Sway rotations preserve each vertex's distance from the (0, 7, 0) pivot. */
    private static final double SWAY_RADIUS = rotationSafeRadius(Math.max(
            CRATE_BOUNDS.maximumFaceRadius(0.5D, HEIGHT, 0.5D),
            CHUTE_BOUNDS.maximumFaceRadius(0.0D, HEIGHT + 1.0D, 0.0D)));

    public LegacyParachuteCrateEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(CRATE);
        event.register(CHUTE);
    }

    @Override
    public void render(LegacyParachuteCrateEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // getTotalWorldTime(), without partial ticks.
        double time = entity.level().getGameTime();
        double sine = Math.sin(time * 0.05D) * 5.0D;
        double sin3 = Math.sin(time * 0.05D + Math.PI * 0.5D) * 5.0D;

        poseStack.translate(0.0D, HEIGHT, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) sine));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) sin3));
        poseStack.translate(0.0D, -HEIGHT, 0.0D);

        poseStack.pushPose();
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRATE), poseStack, bufferSource,
                RENDER_STATE, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); // [fix: overlay 0 = red hurt tint]
        poseStack.popPose();

        poseStack.translate(0.0D, -1.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CHUTE), poseStack, bufferSource,
                RENDER_STATE, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); // [fix: overlay 0 = red hurt tint]
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyParachuteCrateEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY() + HEIGHT, entity.getZ(), SWAY_RADIUS);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyParachuteCrateEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
