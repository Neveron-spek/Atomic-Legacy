package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyC130Entity;
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

/** [parity W1: airdrop] RenderC130: models/weapons/c130.obj parts Plane and Prop1-4, texture c130_0. */
public final class LegacyC130EntityRenderer extends EntityRenderer<LegacyC130Entity> {
    private static final ModelResourceLocation PLANE = MachineModelRenderer.standalone("entity/c130_plane");
    private static final ModelResourceLocation[] PROPS = {
            MachineModelRenderer.standalone("entity/c130_prop1"),
            MachineModelRenderer.standalone("entity/c130_prop2"),
            MachineModelRenderer.standalone("entity/c130_prop3"),
            MachineModelRenderer.standalone("entity/c130_prop4")
    };
    /** Prop pivots (10, 4.2, z) of RenderC130, in Prop1..Prop4 order. */
    private static final double[] PROP_Z = {-20.5D, -11.16D, 11.16D, 20.5D};
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/obj/entity/c130.obj"));
    /**
     * Every transform is a rotation about the origin except the prop spins about (10, 4.2, z):
     * a spun vertex stays within |pivot| + |v - pivot| of the origin.
     */
    private static final double RADIUS = rotationSafeRadius(visualRadius());

    public LegacyC130EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PLANE);
        for (ModelResourceLocation prop : PROPS) event.register(prop);
    }

    @Override
    public void render(LegacyC130Entity entity, float entityYaw, float interp, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.yRotO + (entity.getYRot() - entity.yRotO) * interp - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.xRotO + (entity.getXRot() - entity.xRotO) * interp));

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PLANE), poseStack, bufferSource,
                RENDER_STATE, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); // [fix: overlay 0 = red hurt tint]

        double spin = System.currentTimeMillis() * 15D % 360D;
        for (int i = 0; i < PROPS.length; i++) {
            poseStack.pushPose();
            poseStack.translate(10.0D, 4.2D, PROP_Z[i]);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) spin));
            poseStack.translate(-10.0D, -4.2D, -PROP_Z[i]);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PROPS[i]), poseStack, bufferSource,
                    RENDER_STATE, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); // [fix: overlay 0 = red hurt tint]
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, interp, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyC130Entity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), RADIUS);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyC130Entity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static double visualRadius() {
        double radius = BOUNDS_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D);
        for (double z : PROP_Z) {
            double pivot = Math.sqrt(10.0D * 10.0D + 4.2D * 4.2D + z * z);
            radius = Math.max(radius, pivot + BOUNDS_MODEL.maximumFaceRadius(10.0D, 4.2D, z));
        }
        return radius;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
