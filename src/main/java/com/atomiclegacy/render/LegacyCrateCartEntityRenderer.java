package com.atomiclegacy.render;

import com.atomiclegacy.entity.cart.MinecartCrateEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;

/**
 * [parity S1: carts] EntityMinecartCrate uses the vanilla RenderMinecart: the vanilla minecart model carrying the
 * steel crate as its display block (default display offset 6, scale 0.75).
 */
public final class LegacyCrateCartEntityRenderer extends MinecartRenderer<MinecartCrateEntity> {
    /** Vanilla minecart model (20 x 10 x 16 px) plus the 0.75-scaled display block stay within this radius. */
    private static final double RADIUS = 2.0D;

    public LegacyCrateCartEntityRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }

    @Override
    public boolean shouldRender(MinecartCrateEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // [parity S1: culling bounds] the minecart model and display block stay within RADIUS of the origin
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), RADIUS), entity.getBoundingBox(),
                frustum, cameraX, cameraY, cameraZ);
    }
}
