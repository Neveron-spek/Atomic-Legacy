package com.atomiclegacy.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F3] GunFactoryClient renderers of the XFactoryTool rounds: LegoClient.RENDER_CT_HOOK (with renderWire),
 * RENDER_CT_MORTAR and RENDER_CT_MORTAR_CHARGE. The fext_* rounds have no renderer (RenderBulletMK4 draws nothing).
 */
final class GunBatchF3BulletRenderer {
	private GunBatchF3BulletRenderer() {
	}

	/** ResourceManager.wire_greyscale_tex */
	private static final ResourceLocation WIRE_TEX = AtomicLegacy.id("textures/models/network/wire_greyscale.png");

	/** Textured, lightmap-lit, unshaded quads (GL_LIGHTING and GL_CULL_FACE off in renderWire). */
	private static final RenderType WIRE = RenderType.create("atomiclegacy_ct_wire",
			DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, false,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorTexLightmapShader))
					.setTextureState(new RenderStateShard.TextureStateShard(WIRE_TEX, false, false))
					.setLightmapState(RenderStateShard.LIGHTMAP)
					.setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
					.createCompositeState(false));

	static boolean handles(StandardAmmoType ammo) {
		return ammo == StandardAmmoType.CT_HOOK || ammo == StandardAmmoType.CT_MORTAR || ammo == StandardAmmoType.CT_MORTAR_CHARGE
				|| ammo == StandardAmmoType.FEXT_WATER || ammo == StandardAmmoType.FEXT_FOAM || ammo == StandardAmmoType.FEXT_SAND;
	}

	/** fext_* rounds set no renderer in GunFactoryClient. */
	static boolean visible(StandardAmmoType ammo) {
		return ammo == StandardAmmoType.CT_HOOK || ammo == StandardAmmoType.CT_MORTAR || ammo == StandardAmmoType.CT_MORTAR_CHARGE;
	}

	private static double modelRadius = -1;

	/** Culling radius of the charge thrower rounds (Hook / Mortar + Oomph in their render transform); the wire is never culled (noCulling). */
	static double radius(LegacyBulletEntity entity) {
		if(modelRadius < 0) {
			PoseStack pose = new PoseStack();
			modelTransform(pose);
			EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
			ItemRenderChargeThrower.charge_thrower.includeGroupFacePositions("Hook", pose, bounds);
			ItemRenderChargeThrower.charge_thrower.includeGroupFacePositions("Mortar", pose, bounds);
			ItemRenderChargeThrower.charge_thrower.includeGroupFacePositions("Oomph", pose, bounds);
			modelRadius = Math.nextUp(bounds.maximumRadius() * 1.0001D);
		}
		return modelRadius;
	}

	/** glScalef(0.125); glRotated(90, 0, -1, 0); glRotated(180, 0, 0, 1); glTranslatef(0, 0, -6) */
	private static void modelTransform(PoseStack pose) {
		pose.scale(0.125F, 0.125F, 0.125F);
		pose.mulPose(Axis.YP.rotationDegrees(-90F));
		pose.mulPose(Axis.ZP.rotationDegrees(180F));
		pose.translate(0, 0, -6F);
	}

	static void render(LegacyBulletEntity bullet, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
		StandardAmmoType ammo = bullet.ammoType();
		if(!visible(ammo)) return;
		float yaw = Mth.rotLerp(interp, bullet.yRotO, bullet.getYRot());
		// EntityBulletBaseMK4 rotationPitch is atan2(dY, hyp) (positive upwards); the port stores the negated value
		float pitch = -Mth.lerp(interp, bullet.xRotO, bullet.getXRot());

		pose.pushPose();
		// ct_hook has setRenderRotations(false) and rotates inside its own renderer; the mortars use RenderBulletMK4's renderRotations
		pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
		modelTransform(pose);
		VertexConsumer consumer;
		switch(ammo) {
			case CT_HOOK -> ItemRenderChargeThrower.charge_thrower.renderGroup("Hook", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderChargeThrower.charge_thrower_hook_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			case CT_MORTAR -> ItemRenderChargeThrower.charge_thrower.renderGroup("Mortar", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderChargeThrower.charge_thrower_mortar_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			default -> {
				consumer = buffers.getBuffer(RenderType.entityCutout(ItemRenderChargeThrower.charge_thrower_mortar_tex));
				ItemRenderChargeThrower.charge_thrower.renderGroup("Mortar", pose, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
				ItemRenderChargeThrower.charge_thrower.renderGroup("Oomph", pose, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
			}
		}
		pose.popPose();

		if(ammo == StandardAmmoType.CT_HOOK && bullet.ownerEntity() instanceof Player player) {
			ItemStack held = player.getMainHandItem();
			if(held.is(HbmItems.GUN_CHARGE_THROWER.get()) && ItemGunChargeThrower.getLastHook(held) == bullet.getId()) {
				renderWire(bullet, player, interp, pose, buffers);
			}
		}
	}

	/** LegoClient.renderWire: ten sagging segments from the hook to the thrower's hand (offset 0.125, 0.25, -0.75 from the eyes). */
	static void renderWire(LegacyBulletEntity bullet, Entity thrower, float interp, PoseStack pose, MultiBufferSource buffers) {
		double bx = bullet.xo + (bullet.getX() - bullet.xo) * interp;
		double by = bullet.yo + (bullet.getY() - bullet.yo) * interp;
		double bz = bullet.zo + (bullet.getZ() - bullet.zo) * interp;

		double x = thrower.xo + (thrower.getX() - thrower.xo) * interp;
		double y = thrower.yo + (thrower.getY() - thrower.yo) * interp;
		double z = thrower.zo + (thrower.getZ() - thrower.zo) * interp;
		double eyaw = thrower.yRotO + (thrower.getYRot() - thrower.yRotO) * interp;
		double epitch = thrower.xRotO + (thrower.getXRot() - thrower.xRotO) * interp;

		Vec3 offset = new Vec3(0.125D, 0.25, -0.75)
				.xRot((float) -epitch / 180F * (float) Math.PI)
				.yRot((float) -eyaw / 180F * (float) Math.PI);

		Vec3 target = new Vec3(x - offset.x, y + thrower.getEyeHeight() - offset.y, z - offset.z);

		double deltaX = target.x - bx;
		double deltaY = target.y - by;
		double deltaZ = target.z - bz;
		Vec3 delta = new Vec3(deltaX, deltaY, deltaZ);

		VertexConsumer tess = buffers.getBuffer(WIRE);
		Matrix4f m = pose.last().pose();

		int count = 10;
		double hang = Math.min(delta.length() / 15D, 0.5D);

		double girth = 0.03125D;
		double hyp = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		double yaw = Math.atan2(delta.x, delta.z);
		double pitch = Math.atan2(delta.y, hyp);
		double rotator = Math.PI * 0.5D;
		double newPitch = pitch + rotator;
		double newYaw = yaw + rotator;
		double iZ = Math.cos(yaw) * Math.cos(newPitch) * girth;
		double iX = Math.sin(yaw) * Math.cos(newPitch) * girth;
		double iY = Math.sin(newPitch) * girth;
		double jZ = Math.cos(newYaw) * girth;
		double jX = Math.sin(newYaw) * girth;

		for(float j = 0; j < count; j++) {

			float k = j + 1;

			double sagJ = Math.sin(j / count * Math.PI) * hang;
			double sagK = Math.sin(k / count * Math.PI) * hang;
			double sagMean = (sagJ + sagK) / 2D;

			double ja = j + 0.5D;
			double ix = bx + deltaX / (double) (count) * ja;
			double iy = by + deltaY / (double) (count) * ja - sagMean;
			double iz = bz + deltaZ / (double) (count) * ja;

			int brightness = LevelRenderer.getLightColor(bullet.level(), new BlockPos(Mth.floor(ix), Mth.floor(iy), Mth.floor(iz)));

			drawLineSegment(tess, m, brightness,
					(deltaX * j / count),
					(deltaY * j / count) - sagJ,
					(deltaZ * j / count),
					(deltaX * k / count),
					(deltaY * k / count) - sagK,
					(deltaZ * k / count),
					iX, iY, iZ, jX, jZ);
		}
	}

	/** LegoClient.drawLineSegment with Tessellator#setColorOpaque_I(0x606060). */
	private static void drawLineSegment(VertexConsumer tess, Matrix4f m, int light, double x, double y, double z, double a, double b, double c, double iX, double iY, double iZ, double jX, double jZ) {

		double deltaX = a - x;
		double deltaY = b - y;
		double deltaZ = c - z;
		double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		int wrap = (int) Math.ceil(length * 8);

		if(deltaX + deltaZ < 0) {
			wrap *= -1;
			jZ *= -1;
			jX *= -1;
		}

		vertex(tess, m, light, x + iX, y + iY, z + iZ, 0, 0);
		vertex(tess, m, light, x - iX, y - iY, z - iZ, 0, 1);
		vertex(tess, m, light, a - iX, b - iY, c - iZ, wrap, 1);
		vertex(tess, m, light, a + iX, b + iY, c + iZ, wrap, 0);
		vertex(tess, m, light, x + jX, y, z + jZ, 0, 0);
		vertex(tess, m, light, x - jX, y, z - jZ, 0, 1);
		vertex(tess, m, light, a - jX, b, c - jZ, wrap, 1);
		vertex(tess, m, light, a + jX, b, c + jZ, wrap, 0);
	}

	private static void vertex(VertexConsumer tess, Matrix4f m, int light, double x, double y, double z, float u, float v) {
		tess.addVertex(m, (float) x, (float) y, (float) z).setColor(0x60, 0x60, 0x60, 0xFF).setUv(u, v).setLight(light);
	}
}
