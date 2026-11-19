package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.AmmoFamily;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * [weapons batch D3] GunFactoryClient renderers of the XFactory40mm rounds: LegoClient.RENDER_FLARE,
 * RENDER_FLARE_SUPPLY, RENDER_FLARE_WEAPON (g26 flares, setRenderRotations(false)) and RENDER_GRENADE (g40).
 */
final class GunBatchD3BulletRenderer {

	private GunBatchD3BulletRenderer() {
	}

	/** LegoClient.flare (textures/particle/flare.png) */
	private static final ResourceLocation FLARE = AtomicLegacy.id("textures/particle/flare.png");
	/** ResourceManager.grenade_tex (textures/models/projectiles/grenade.png) */
	private static final ResourceLocation GRENADE_TEX = AtomicLegacy.id("textures/models/projectiles/grenade.png");
	/** ResourceManager.projectiles (models/projectiles/projectiles.obj; same geometry at the port path) */
	static final LegacyEntityObjMesh PROJECTILES = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/projectiles/projectiles.obj"));

	/** Whether this round has a D3 renderer; the caller then skips the standard tracer. */
	static boolean handles(StandardAmmoType ammo) {
		return ammo.family() == AmmoFamily.FLARE || ammo.family() == AmmoFamily.G40;
	}

	/** Culling radius: the flare sprite grows to 5 * 1.2 (half extent, diagonal sqrt(2)), the grenade is below 1 block. */
	static double radius(StandardAmmoType ammo) {
		return ammo.family() == AmmoFamily.FLARE ? 6.0D * Math.sqrt(2.0D) + 0.1D : 1.0D;
	}

	static void render(LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		StandardAmmoType ammo = bullet.ammoType();
		switch(ammo) {
			case G26_FLARE -> renderFlare(bullet, interp, pose, buffers, 1F, 0.5F, 0.5F);
			case G26_FLARE_SUPPLY -> renderFlare(bullet, interp, pose, buffers, 0.5F, 0.5F, 1F);
			case G26_FLARE_WEAPON -> renderFlare(bullet, interp, pose, buffers, 0.5F, 1F, 0.5F);
			default -> renderGrenade(bullet, interp, pose, buffers, light);
		}
	}

	/** RenderBulletMK4 (renderRotations) + LegoClient.RENDER_GRENADE. */
	static void renderGrenade( // [weapons batch F2] also nuke_tots (RENDER_GRENADE)
			LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		float yaw = Mth.rotLerp(interp, bullet.yRotO, bullet.getYRot());
		float pitch = -Mth.lerp(interp, bullet.xRotO, bullet.getXRot()); // 1.7.10 pitch is positive upwards
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
		pose.scale(0.25F, 0.25F, 0.25F);
		pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
		PROJECTILES.renderGroup("Grenade", pose, buffers.getBuffer(RenderType.entityCutout(GRENADE_TEX)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		pose.popPose();
	}

	/**
	 * LegoClient.renderFlare: hidden for the first 2 ticks, fullbright, no fog, scale min(5, (age - 2) * 0.5) * [0.8;1.2),
	 * then renderFlareSprite with outer alpha 0.5 and inner alpha 0.75.
	 */
	private static void renderFlare(LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, float r, float g, float b) {
		if(bullet.tickCount < 2) return;
		double scale = Math.min(5, (bullet.tickCount + interp - 2) * 0.5) * (0.8 + bullet.level().random.nextDouble() * 0.4);
		renderFlareSprite(pose, buffers, r, g, b, scale, 0.5F, 0.75F);
	}

	/**
	 * LegoClient.renderFlareSprite: camera facing quad (ActiveRenderInfo rotation), additive blending
	 * (GL_SRC_ALPHA, GL_ONE), no depth write, no lighting; a coloured outer quad and a white inner one of half size.
	 */
	static void renderFlareSprite(PoseStack pose, MultiBufferSource buffers, float r, float g, float b, double scale, float outerAlpha, float innerAlpha) {
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();
		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.additive(FLARE));
		Matrix4f m = pose.last().pose();
		quad(tess, m, left, up, (float) scale, r, g, b, outerAlpha);
		quad(tess, m, left, up, (float) (scale * 0.5D), 1F, 1F, 1F, innerAlpha);
	}

	private static void quad(VertexConsumer tess, Matrix4f m, Vector3f l, Vector3f u, float s, float r, float g, float b, float a) {
		vertex(tess, m, -l.x() * s - u.x() * s, -l.y() * s - u.y() * s, -l.z() * s - u.z() * s, 1, 1, r, g, b, a);
		vertex(tess, m, -l.x() * s + u.x() * s, -l.y() * s + u.y() * s, -l.z() * s + u.z() * s, 1, 0, r, g, b, a);
		vertex(tess, m, l.x() * s + u.x() * s, l.y() * s + u.y() * s, l.z() * s + u.z() * s, 0, 0, r, g, b, a);
		vertex(tess, m, l.x() * s - u.x() * s, l.y() * s - u.y() * s, l.z() * s - u.z() * s, 0, 1, r, g, b, a);
	}

	private static void vertex(VertexConsumer tess, Matrix4f m, float x, float y, float z, float uu, float vv, float r, float g, float b, float a) {
		tess.addVertex(m, x, y, z).setUv(uu, vv).setColor(r, g, b, a);
	}
}
