package com.atomiclegacy.render;

import net.minecraft.world.phys.AABB;
import java.awt.Color;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyChemicalEntity;
import com.atomiclegacy.entity.LegacyChemicalEntity.ChemicalStyle;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F1] 1.7.10 RenderChemical: AMAT / LIGHTNING draw an additive beam, GAS a growing translucent cloud quad,
 * GASFLAME a growing fire quad (particle_base); LIQUID and BURNING have no renderer (particles only).
 */
public class LegacyChemicalEntityRenderer extends EntityRenderer<LegacyChemicalEntity> {

	private static final ResourceLocation gas = AtomicLegacy.id("textures/particle/particle_base.png");

	public LegacyChemicalEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/** ignoreFrustumCheck = true */
	@Override
	public boolean shouldRender(LegacyChemicalEntity entity, Frustum frustum, double camX, double camY, double camZ) {
		// [parity S4: culling] ignoreFrustumCheck replaced by the finite envelope of the three styles at the latest
		// frame of this tick (tickCount + 1): GAS billboard exp*10, GASFLAME billboard exp*2 (corner sqrt 2),
		// AMAT/LIGHTNING beam |motion| * age * 0.75 plus its 0.0625 half-width. Distance keeps the entity rule.
		double exp = (entity.tickCount + 1.0D) / Math.max(1, entity.getMaxAge());
		ChemicalStyle style = entity.getStyle();
		double radius;
		if (style == ChemicalStyle.AMAT || style == ChemicalStyle.LIGHTNING) {
			radius = entity.getDeltaMovement().length() * (entity.tickCount + 1.0D) * 0.75D + 0.0625D * Math.sqrt(2.0D);
		} else if (style == ChemicalStyle.GAS) {
			radius = exp * 10.0D * Math.sqrt(2.0D);
		} else if (style == ChemicalStyle.GASFLAME) {
			radius = exp * 2.0D * Math.sqrt(2.0D);
		} else {
			radius = 0.0D;
		}
		AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), Math.nextUp(radius) + 0.01D);
		return EntityRenderBounds.shouldRender(entity, bounds, entity.getBoundingBox(), frustum, camX, camY, camZ);
	}

	@Override
	public void render(LegacyChemicalEntity chem, float entityYaw, float interp, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		poseStack.pushPose();

		ChemicalStyle style = chem.getStyle();

		if(style == ChemicalStyle.AMAT || style == ChemicalStyle.LIGHTNING)
			renderAmatBeam(chem, interp, poseStack, buffers);

		if(style == ChemicalStyle.GAS) {
			renderGasCloud(chem, interp, poseStack, buffers);
		}

		if(style == ChemicalStyle.GASFLAME) {
			renderGasFire(chem, interp, poseStack, buffers);
		}

		poseStack.popPose();
		super.render(chem, entityYaw, interp, poseStack, buffers, packedLight);
	}

	/** GL11.glRotatef(180 - playerViewY, 0, 1, 0); GL11.glRotatef(-playerViewX, 1, 0, 0) */
	private void faceCamera(PoseStack poseStack) {
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - this.entityRenderDispatcher.camera.getYRot()));
		poseStack.mulPose(Axis.XP.rotationDegrees(-this.entityRenderDispatcher.camera.getXRot()));
	}

	private void renderGasFire(LegacyChemicalEntity chem, float interp, PoseStack poseStack, MultiBufferSource buffers) {

		float exp = (float) (chem.tickCount + interp) / (float) chem.getMaxAge();
		double size = 0.0 + exp * 2;
		Color color = Color.getHSBColor(Math.max((60 - exp * 100) / 360F, 0.0F), 1 - exp * 0.25F, 1 - exp * 0.5F);
		int alpha = (int) Math.max(255 * (1 - exp), 0);

		faceCamera(poseStack);
		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.blend(gas));
		PoseStack.Pose pose = poseStack.last();
		vertex(tess, pose, -size, -size, 0.0D, 1, 1, color.getRed(), color.getGreen(), color.getBlue(), alpha);
		vertex(tess, pose, size, -size, 0.0D, 0, 1, color.getRed(), color.getGreen(), color.getBlue(), alpha);
		vertex(tess, pose, size, size, 0.0D, 0, 0, color.getRed(), color.getGreen(), color.getBlue(), alpha);
		vertex(tess, pose, -size, size, 0.0D, 1, 0, color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private void renderGasCloud(LegacyChemicalEntity chem, float interp, PoseStack poseStack, MultiBufferSource buffers) {

		double exp = (double) (chem.tickCount + interp) / (double) chem.getMaxAge();
		double size = 0.0 + exp * 10;
		int color = chem.getFluid().color();
		int alpha = (int) Math.max(127 * (1 - exp), 0);

		faceCamera(poseStack);

		Random rand = new Random(chem.getId());
		int i = rand.nextInt(2);
		int j = rand.nextInt(2);

		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.blend(gas));
		PoseStack.Pose pose = poseStack.last();
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
		vertex(tess, pose, -size, -size, 0.0D, 1 - i, 1 - j, r, g, b, alpha);
		vertex(tess, pose, size, -size, 0.0D, i, 1 - j, r, g, b, alpha);
		vertex(tess, pose, size, size, 0.0D, i, j, r, g, b, alpha);
		vertex(tess, pose, -size, size, 0.0D, 1 - i, j, r, g, b, alpha);
	}

	private void renderAmatBeam(LegacyChemicalEntity chem, float interp, PoseStack poseStack, MultiBufferSource buffers) {

		float yaw = chem.yRotO + (chem.getYRot() - chem.yRotO) * interp;
		float pitch = chem.xRotO + (chem.getXRot() - chem.xRotO) * interp;
		poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(-pitch - 90));

		Vec3 motion = chem.getDeltaMovement();
		double length = Mth.sqrt((float) (motion.x * motion.x + motion.y * motion.y + motion.z * motion.z)) * (chem.tickCount + interp) * 0.75;
		double size = 0.0625;
		int o = (int) (0.2F * 255);

		// GL_SRC_ALPHA / GL_ONE, smooth shading, no texture, no cull, no depth write
		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR);
		PoseStack.Pose pose = poseStack.last();

		beam(tess, pose, -size, 0, -size, size, 0, -size, size, length, -size, -size, length, -size, o);
		beam(tess, pose, -size, 0, size, size, 0, size, size, length, size, -size, length, size, o);
		beam(tess, pose, -size, 0, -size, -size, 0, size, -size, length, size, -size, length, -size, o);
		beam(tess, pose, size, 0, -size, size, 0, size, size, length, size, size, length, -size, o);
	}

	private static void beam(VertexConsumer tess, PoseStack.Pose pose, double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, int o) {
		tess.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(255, 255, 255, o);
		tess.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(255, 255, 255, o);
		tess.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(255, 255, 255, 0);
		tess.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(255, 255, 255, 0);
	}

	private static void vertex(VertexConsumer tess, PoseStack.Pose pose, double x, double y, double z, float u, float v, int r, int g, int b, int a) {
		tess.addVertex(pose, (float) x, (float) y, (float) z).setUv(u, v).setColor(r, g, b, a);
	}

	@Override
	public ResourceLocation getTextureLocation(LegacyChemicalEntity entity) {
		return gas;
	}
}
