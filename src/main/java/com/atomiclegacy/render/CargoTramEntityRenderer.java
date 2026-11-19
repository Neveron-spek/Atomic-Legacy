package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.train.CargoTramTrailerEntity;
import com.atomiclegacy.train.RailCarCargoEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * [parity S1: trains] RenderTrainCargoTram and RenderTrainCargoTramTrailer: the OBJ drawn at the render position
 * between the bogies (yaw - 720, pitch), back-face culling off; the trailer adds up to nine crates by occupied slots.
 */
public final class CargoTramEntityRenderer extends EntityRenderer<RailCarCargoEntity> {
    private static final LegacyEntityObjMesh TRAM = LegacyEntityObjMesh.load(AtomicLegacy.id("models/vehicles/tram.obj"));
    private static final LegacyEntityObjMesh TRAILER = LegacyEntityObjMesh.load(AtomicLegacy.id("models/vehicles/tram_trailer.obj"));
    private static final ResourceLocation TRAIN_TRAM = AtomicLegacy.id("textures/models/trains/tram.png");
    private static final ResourceLocation TRAM_TRAILER = AtomicLegacy.id("textures/models/trains/tram_trailer.png");
    /** Mesh radius plus the offset between the core and the bogie centre (bounded by the 1.5 length span). */
    private static final double RADIUS = Math.max(TRAM.maxRadius(), TRAILER.maxRadius()) + 2.0D;

    /** Crate positions of RenderTrainCargoTramTrailer per 5 occupied slots (inside the 2x scale). */
    private static final double[][][] CRATES = {
            {{0.0, 0.375, 0.0}},
            {{0.1, 0.375, 0.25}, {-0.1, 0.375, -0.25}},
            {{0.1, 0.375, 0.0}, {-0.1, 0.375, 0.375}, {-0.1, 0.375, -0.375}},
            {{0.2, 0.375, 0.3}, {0.2, 0.375, -0.2}, {-0.2, 0.375, 0.2}, {-0.2, 0.375, -0.3}},
            {{0.2, 0.375, 0.6}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.5}, {-0.2, 0.375, 0.2}, {-0.2, 0.375, -0.3}},
            {{0.2, 0.375, 0.6}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.5}, {-0.2, 0.375, 0.5}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.6}},
            {{0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4}, {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5}, {0.0, 0.6875, -0.25}},
            {{0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4}, {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5}, {0.0, 0.6875, -0.25}, {0.0, 0.6875, 0.15}},
            {{0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4}, {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5}, {0.0, 0.6875, -0.25}, {0.0, 0.6875, 0.15}, {-0.1, 0.375, 0.8}},
    };

    public CargoTramEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RailCarCargoEntity train, float swing, float interp, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();

        double iX = Mth.lerp(interp, train.xOld, train.getX());
        double iY = Mth.lerp(interp, train.yOld, train.getY());
        double iZ = Mth.lerp(interp, train.zOld, train.getZ());
        double rX = train.lastRenderX + (train.renderX - train.lastRenderX) * interp;
        double rY = train.lastRenderY + (train.renderY - train.lastRenderY) * interp;
        double rZ = train.lastRenderZ + (train.renderZ - train.lastRenderZ) * interp;
        poseStack.translate(rX - iX, rY - iY, rZ - iZ);

        float yaw = train.getYRot();
        float prevYaw = train.yRotO;

        if (yaw - prevYaw > 180) yaw -= 360;
        if (prevYaw - yaw > 180) prevYaw -= 360;

        float yawInterp = prevYaw + (yaw - prevYaw) * interp - 720;

        poseStack.mulPose(Axis.YP.rotationDegrees(-yawInterp));

        float pitch = train.getXRot();
        float prevPitch = train.xRotO;
        float pitchInterp = prevPitch + (pitch - prevPitch) * interp;
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitchInterp));

        boolean trailer = train instanceof CargoTramTrailerEntity;
        ResourceLocation texture = trailer ? TRAM_TRAILER : TRAIN_TRAM;
        (trailer ? TRAILER : TRAM).renderAll(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        if (trailer) {
            int slots = train.occupiedSlots();
            if (slots > 0) {
                // slots <= 5, <= 10, ... <= 40, else
                int layout = Math.min((slots - 1) / 5, CRATES.length - 1);
                poseStack.scale(2F, 2F, 2F);
                // renderEntityWithPosYaw(dummy, x, y, z, 0, 0): age 0, hoverStart 0 and partialTicks 0 -> bob 0.1, spin 0
                float bob = Mth.sin(0.0F) * 0.1F + 0.1F;
                float spin = 0.0F;
                for (double[] crate : CRATES[layout]) {
                    poseStack.pushPose();
                    poseStack.translate(crate[0], crate[1] + bob, crate[2]);
                    poseStack.mulPose(Axis.YP.rotationDegrees(spin));
                    // RenderItem.renderInFrame: scale 1.25, raise 0.05, turn -90; 3D block items at 0.25, centred
                    poseStack.scale(1.25F, 1.25F, 1.25F);
                    poseStack.translate(0.0F, 0.05F, 0.0F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    poseStack.scale(0.25F, 0.25F, 0.25F);
                    poseStack.translate(-0.5F, -0.5F, -0.5F);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(HbmBlocks.CRATE.get().defaultBlockState(), poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
        super.render(train, swing, interp, poseStack, buffers, light);
    }

    @Override
    public boolean shouldRender(RailCarCargoEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // [parity S1: culling bounds] OBJ radius plus the bogie render offset; the crates sit inside the trailer bed
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), RADIUS), entity.getBoundingBox(),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(RailCarCargoEntity entity) {
        return entity instanceof CargoTramTrailerEntity ? TRAM_TRAILER : TRAIN_TRAM;
    }
}
