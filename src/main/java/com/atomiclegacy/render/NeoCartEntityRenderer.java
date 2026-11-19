package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.cart.MinecartDestroyerEntity;
import com.atomiclegacy.entity.cart.MinecartPowderEntity;
import com.atomiclegacy.entity.cart.MinecartSemtexEntity;
import com.atomiclegacy.entity.cart.NtmMinecartEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * [parity S1: carts] RenderNeoCart: the cart.obj Carriage and Bucket (bucket wobbles when hit) with the base texture,
 * plus the destroyer / powder / semtex contents. The rail-following transform is the vanilla minecart one; 1.7.10 posY
 * was the cart centre, which the 1.21 renderer reaches with its 0.375 offset, so the original -0.4375 follows it.
 */
public final class NeoCartEntityRenderer extends EntityRenderer<NtmMinecartEntity> {
    private static final LegacyEntityObjMesh CART = LegacyEntityObjMesh.load(AtomicLegacy.id("models/vehicles/cart.obj"));
    private static final LegacyEntityObjMesh CART_DESTROYER = LegacyEntityObjMesh.load(AtomicLegacy.id("models/vehicles/cart_destroyer.obj"));
    private static final LegacyEntityObjMesh CART_POWDER = LegacyEntityObjMesh.load(AtomicLegacy.id("models/vehicles/cart_powder.obj"));

    private static final ResourceLocation CART_METAL = AtomicLegacy.id("textures/entity/cart_metal.png");
    private static final ResourceLocation CART_BLANK = AtomicLegacy.id("textures/entity/cart_metal_naked.png");
    private static final ResourceLocation CART_WOOD = AtomicLegacy.id("textures/entity/cart_wood.png");
    private static final ResourceLocation CART_DESTROYER_TEX = AtomicLegacy.id("textures/entity/cart_destroyer.png");
    private static final ResourceLocation CART_POWDER_TEX = AtomicLegacy.id("textures/block/block_gunpowder.png");
    private static final ResourceLocation CART_SEMTEX_SIDE = AtomicLegacy.id("textures/block/semtex_side.png");
    private static final ResourceLocation CART_SEMTEX_TOP = AtomicLegacy.id("textures/block/semtex_bottom.png");

    /** Largest vertex distance of every drawn mesh plus the 0.0625 render offset and the rail correction margin. */
    private static final double RADIUS = Math.max(CART.maxRadius(), Math.max(CART_DESTROYER.maxRadius(), CART_POWDER.maxRadius())) + 1.0D;

    public NeoCartEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(NtmMinecartEntity cart, float rot, float interp, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(cart, rot, interp, poseStack, buffers, light);
        poseStack.pushPose();

        long rand = (long) cart.getId() * 493286711L;
        rand = rand * rand * 4392167121L + rand * 98761L;
        float randX = (((float) (rand >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float randY = (((float) (rand >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float randZ = (((float) (rand >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(randX, randY, randZ);

        double interpX = Mth.lerp(interp, cart.xOld, cart.getX());
        double interpY = Mth.lerp(interp, cart.yOld, cart.getY());
        double interpZ = Mth.lerp(interp, cart.zOld, cart.getZ());
        double mult = 0.3;
        Vec3 vec3 = cart.getPos(interpX, interpY, interpZ);
        float interpPitch = Mth.lerp(interp, cart.xRotO, cart.getXRot());

        if (vec3 != null) {
            Vec3 vec31 = cart.getPosOffs(interpX, interpY, interpZ, mult);
            Vec3 vec32 = cart.getPosOffs(interpX, interpY, interpZ, -mult);
            if (vec31 == null) vec31 = vec3;
            if (vec32 == null) vec32 = vec3;

            poseStack.translate(vec3.x - interpX, (vec31.y + vec32.y) / 2.0D - interpY, vec3.z - interpZ);
            Vec3 vec33 = vec32.add(-vec31.x, -vec31.y, -vec31.z);

            if (vec33.length() != 0.0D) {
                vec33 = vec33.normalize();
                rot = (float) (Math.atan2(vec33.z, vec33.x) * 180.0D / Math.PI);
                interpPitch = (float) (Math.atan(vec33.y) * 73.0D);
            }
        }

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-interpPitch));
        float interpRoll = (float) cart.getHurtTime() - interp;
        float interpDamage = cart.getDamage() - interp;

        if (interpDamage < 0.0F) {
            interpDamage = 0.0F;
        }

        poseStack.translate(0, -0.4375F, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));

        ResourceLocation texture = getTextureLocation(cart);
        int overlay = OverlayTexture.NO_OVERLAY;
        CART.renderGroup("Carriage", poseStack, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);

        if (interpRoll > 0.0F) {
            poseStack.translate(0, 0.75F, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(interpRoll) * interpRoll * interpDamage / 10.0F * (float) cart.getHurtDir()));
            poseStack.translate(0, -0.75F, 0);
        }

        CART.renderGroup("Bucket", poseStack, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);

        // renderSpecialContent
        if (cart instanceof MinecartDestroyerEntity) {
            CART_DESTROYER.renderAll(poseStack, buffers.getBuffer(RenderType.entityCutout(CART_DESTROYER_TEX)), light, overlay, 0xFFFFFFFF);
        } else if (cart instanceof MinecartPowderEntity) {
            CART_POWDER.renderGroup("Powder", poseStack, buffers.getBuffer(RenderType.entityCutout(CART_POWDER_TEX)), light, overlay, 0xFFFFFFFF);
        } else if (cart instanceof MinecartSemtexEntity) {
            CART_POWDER.renderGroup("SemtexTop", poseStack, buffers.getBuffer(RenderType.entityCutout(CART_SEMTEX_TOP)), light, overlay, 0xFFFFFFFF);
            CART_POWDER.renderGroup("SemtexSide", poseStack, buffers.getBuffer(RenderType.entityCutout(CART_SEMTEX_SIDE)), light, overlay, 0xFFFFFFFF);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(NtmMinecartEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // [parity S1: culling bounds] rotation-invariant OBJ radius around the cart origin
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), RADIUS), entity.getBoundingBox(),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(NtmMinecartEntity entity) {
        return switch (entity.getBase()) {
            case PAINTED -> CART_METAL;
            case WOOD -> CART_WOOD;
            default -> CART_BLANK;
        };
    }
}
