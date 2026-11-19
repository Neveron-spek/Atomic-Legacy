package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyCoinEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** [weapons batch F2] 1.7.10 com.hbm.render.entity.projectile.RenderCoin: models/trinkets/chip.obj with chip_gold.png, scale 0.125. */
public class LegacyCoinEntityRenderer extends EntityRenderer<LegacyCoinEntity> {

	public static final LegacyEntityObjMesh coin = LegacyEntityObjMesh.load(AtomicLegacy.id("models/trinkets/chip.obj"));
	public static final ResourceLocation coin_tex = AtomicLegacy.id("textures/models/trinkets/chip_gold.png");

	public LegacyCoinEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(LegacyCoinEntity entity, float entityYaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		// the 1.7.10 render origin is the box center (yOffset 0.5)
		pose.translate(0, 0.5, 0);
		pose.mulPose(Axis.YN.rotationDegrees(Mth.rotLerp(interp, entity.yRotO, entity.getYRot()) - 90.0F));
		pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + interp) * 45));

		float scale = 0.125F;
		pose.scale(scale, scale, scale);

		coin.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(coin_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

		pose.popPose();
		super.render(entity, entityYaw, interp, pose, buffers, light);
	}

	@Override
	public boolean shouldRender(LegacyCoinEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
		AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY() + 0.5D, entity.getZ(), 1.5D);
		return EntityRenderBounds.shouldRender(entity, bounds, frustum, cameraX, cameraY, cameraZ);
	}

	@Override
	public ResourceLocation getTextureLocation(LegacyCoinEntity entity) {
		return coin_tex;
	}
}
