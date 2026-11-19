package com.atomiclegacy.render;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.7.10 RenderBeam with the LegoClient.renderStandardLaser beam renderer (RENDER_LASER_* colors)
 * drawn through BeamPronter.prontBeam(RANDOM, SOLID): untextured, unlit, GL_SRC_ALPHA/GL_ONE,
 * no depth write, no culling, no fog.
 */
public class LegacyBulletBeamRenderer extends EntityRenderer<LegacyBulletBeamEntity> {

	public LegacyBulletBeamRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	/** Entity#ignoreFrustumCheck */
	@Override
	public boolean shouldRender(LegacyBulletBeamEntity entity, Frustum frustum, double camX, double camY, double camZ) {
		// [parity S4: culling] ignoreFrustumCheck replaced by a finite sphere: every beam renderer draws along one
		// axis of beamLength from the origin (tracers are translated by beamLength and drawn back towards it);
		// the widest lateral part is the folly flare billboard, corner sqrt(2) * (7.5 + 1.5) < 13.
		// Distance keeps the entity rule (renderDistanceWeight 10 on its own collision box).
		AABB beam = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), Math.abs(entity.beamLength) + 13.0D);
		return EntityRenderBounds.shouldRender(entity, beam, entity.getBoundingBox(), frustum, camX, camY, camZ);
	}

	@Override
	public void render(LegacyBulletBeamEntity bullet, float entityYaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		BeamBulletConfig config = bullet.config;
		if(config == null) return;

		pose.pushPose();

		if(config.renderRotations) {
			pose.mulPose(Axis.YP.rotationDegrees(bullet.yRotO + (bullet.getYRot() - bullet.yRotO) * interp - 90.0F));
			pose.mulPose(Axis.ZP.rotationDegrees(bullet.xRotO + (bullet.getXRot() - bullet.xRotO) * interp + 180));
		}

		// [weapons batch E2] LegoClient.RENDER_LIGHTNING / RENDER_LIGHTNING_SUB
		if(config.beamRenderer == BeamBulletConfig.RENDER_LIGHTNING) LegacyLightningBeamRender.render(bullet, interp, pose, buffers, 0.5D);
		else if(config.beamRenderer == BeamBulletConfig.RENDER_LIGHTNING_SUB) LegacyLightningBeamRender.render(bullet, interp, pose, buffers, 0.15D);
		// [weapons batch F4] LegoClient.RENDER_FOLLY / RENDER_CRACKLE / RENDER_BLACK_LIGHTNING
		else if(config.beamRenderer == BeamBulletConfig.RENDER_FOLLY) GunBatchF4BeamRenderer.renderFolly(bullet, interp, pose, buffers);
		else if(config.beamRenderer == BeamBulletConfig.RENDER_CRACKLE) GunBatchF4BeamRenderer.renderCrackle(bullet, interp, pose, buffers, 0xE3D692, 0xffffff);
		else if(config.beamRenderer == BeamBulletConfig.RENDER_BLACK_LIGHTNING) GunBatchF4BeamRenderer.renderCrackle(bullet, interp, pose, buffers, 0x4C3093, 0x000000);
		// [weapons batch F2] LegoClient.RENDER_TAU / RENDER_TAU_CHARGE / RENDER_NI4NI_BOLT
		else if(config.beamRenderer == BeamBulletConfig.RENDER_TAU) LegacyTauBeamRender.renderTau(bullet, interp, pose, buffers, false);
		else if(config.beamRenderer == BeamBulletConfig.RENDER_TAU_CHARGE) LegacyTauBeamRender.renderTau(bullet, interp, pose, buffers, true);
		else if(config.beamRenderer == BeamBulletConfig.RENDER_NI4NI_BOLT) LegacyTauBeamRender.renderNI4NIBolt(bullet, interp, pose, buffers);
		else if(config.laserColor >= 0) {
			renderStandardLaser(bullet, config, interp, pose, buffers, (config.laserColor >> 16) & 0xFF, (config.laserColor >> 8) & 0xFF, config.laserColor & 0xFF);
		}

		pose.popPose();
	}

	/** LegoClient.renderStandardLaser */
	public static void renderStandardLaser(LegacyBulletBeamEntity bullet, BeamBulletConfig config, float interp, PoseStack pose, MultiBufferSource buffers, int r, int g, int b) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180 - bullet.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-bullet.getXRot() - 90));
		double age = Mth.clamp(1D - ((double) bullet.tickCount - 2 + interp) / (double) config.expires, 0, 1);
		pose.scale((float) (age / 2 + 0.5), 1F, (float) (age / 2 + 0.5));
		int colorInner = ((int)(r * age) << 16) | ((int)(g * age) << 8) | (int) (b * age);
		prontBeamRandomSolid(pose, buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR), bullet.beamLength, colorInner, colorInner, bullet.tickCount / 3, (int)(bullet.beamLength / 2 + 1), 0F, 4, 0.025F);
		pose.popPose();
	}

	/**
	 * BeamPronter.prontBeam(Vec3(0, length, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, ...): the skeleton points
	 * along +Y, so the skeleton rotation reduces to the fixed 180 degree yaw.
	 */
	public static void prontBeamRandomSolid(PoseStack pose, VertexConsumer tess, double length, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180));
		Matrix4f m = pose.last().pose();

		java.util.Random rand = new java.util.Random(start);
		double segLength = length / segments;
		double lastX = 0;
		double lastY = 0;
		double lastZ = 0;

		for(int i = 0; i <= segments; i++) {

			// Vec3(size, 0, 0).rotateAroundY(2PI * rand) twice
			double angle = (float) Math.PI * 2 * rand.nextFloat();
			double sx = size * Math.cos(angle);
			double sz = -size * Math.sin(angle);
			float angle2 = (float) Math.PI * 2 * rand.nextFloat();
			double c = Math.cos(angle2), s = Math.sin(angle2);
			double spinnerX = sx * c + sz * s;
			double spinnerZ = sz * c - sx * s;

			double pX = spinnerX;
			double pY = segLength * i;
			double pZ = spinnerZ;

			if(i > 0) {
				float radius = thickness / layers;

				for(int j = 1; j <= layers; j++) {
					float inter = (float) (j - 1) / (float) (layers - 1);

					int r1 = ((outerColor & 0xFF0000) >> 16);
					int g1 = ((outerColor & 0x00FF00) >> 8);
					int b1 = ((outerColor & 0x0000FF) >> 0);
					int r2 = ((innerColor & 0xFF0000) >> 16);
					int g2 = ((innerColor & 0x00FF00) >> 8);
					int b2 = ((innerColor & 0x0000FF) >> 0);
					int cr = (int)(r1 + (r2 - r1) * inter);
					int cg = (int)(g1 + (g2 - g1) * inter);
					int cb = (int)(b1 + (b2 - b1) * inter);
					double w = radius * j;

					quad(tess, m, cr, cg, cb, lastX + w, lastY, lastZ + w, lastX + w, lastY, lastZ - w, pX + w, pY, pZ - w, pX + w, pY, pZ + w);
					quad(tess, m, cr, cg, cb, lastX - w, lastY, lastZ + w, lastX - w, lastY, lastZ - w, pX - w, pY, pZ - w, pX - w, pY, pZ + w);
					quad(tess, m, cr, cg, cb, lastX + w, lastY, lastZ + w, lastX - w, lastY, lastZ + w, pX - w, pY, pZ + w, pX + w, pY, pZ + w);
					quad(tess, m, cr, cg, cb, lastX + w, lastY, lastZ - w, lastX - w, lastY, lastZ - w, pX - w, pY, pZ - w, pX + w, pY, pZ - w);
				}
			}

			lastX = pX;
			lastY = pY;
			lastZ = pZ;
		}

		pose.popPose();
	}

	private static void quad(VertexConsumer tess, Matrix4f m, int r, int g, int b,
			double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3) {
		tess.addVertex(m, (float) x0, (float) y0, (float) z0).setColor(r, g, b, 255);
		tess.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 255);
		tess.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 255);
		tess.addVertex(m, (float) x3, (float) y3, (float) z3).setColor(r, g, b, 255);
	}

	/** RenderBeam#getEntityTexture: ResourceManager.universal */
	@Override
	public ResourceLocation getTextureLocation(LegacyBulletBeamEntity entity) {
		return AtomicLegacy.id("textures/models/thegadget3_.png");
	}
}
