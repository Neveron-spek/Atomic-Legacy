package com.atomiclegacy.client.particle;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch D1] 1.7.10 com.hbm.particle.ParticlePlasmaBlast as spawned by the "plasmablast" aux particle
 * (XFactory12ga.spawnPulse): a flat shockwave.png quad rotated by yaw (Y) then pitch (X), tinted r/g/b, alpha
 * 1 - age / 20, size (1 - e^(-age / 8)) * scale, full-bright, additive (SRC_ALPHA, ONE), no depth writes and
 * double-sided (GL_CULL_FACE off). Uses the shockwave sprite already stitched for legacy_plasma_blast.
 */
public final class GunPlasmaPulseParticle extends TextureSheetParticle {

	private final float pitch;
	private final float yaw;
	private final float scale;

	public GunPlasmaPulseParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, float pitch, float yaw, float scale) {
		super(level, x, y, z);
		// The particle atlas is owned by the TextureManager/ParticleEngine, not by ModelManager's AtlasSet:
		// Minecraft.getTextureAtlas(LOCATION_PARTICLES) throws an NPE (seen in the game log on every pulse).
		setSprite(((TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES))
				.getSprite(AtomicLegacy.id("shockwave")));
		this.lifetime = 20;
		this.rCol = r;
		this.gCol = g;
		this.bCol = b;
		this.pitch = pitch;
		this.yaw = yaw;
		this.scale = scale;
		this.hasPhysics = false;
		this.xd = this.yd = this.zd = 0;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if(this.age++ >= this.lifetime) remove();
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float interp) {
		Vec3 cam = camera.getPosition();
		float pX = (float) (Mth.lerp(interp, this.xo, this.x) - cam.x);
		float pY = (float) (Mth.lerp(interp, this.yo, this.y) - cam.y);
		float pZ = (float) (Mth.lerp(interp, this.zo, this.z) - cam.z);

		float alpha = 1 - (((float) this.age + interp) / (float) this.lifetime);
		float size = (1 - (float) Math.pow(Math.E, (this.age + interp) * -0.125)) * this.scale;

		Quaternionf rot = new Quaternionf().rotateY((float) Math.toRadians(this.yaw)).rotateX((float) Math.toRadians(this.pitch));
		Vector3f[] corners = {
				new Vector3f(-size, 0, -size), new Vector3f(-size, 0, size), new Vector3f(size, 0, size), new Vector3f(size, 0, -size)
		};
		float[][] uv = { {getU1(), getV1()}, {getU1(), getV0()}, {getU0(), getV0()}, {getU0(), getV1()} };
		for(Vector3f v : corners) rot.transform(v);
		int light = LightTexture.FULL_BRIGHT;
		// front, then back (culling disabled in the original)
		for(int i = 0; i < 4; i++) buffer.addVertex(pX + corners[i].x, pY + corners[i].y, pZ + corners[i].z).setUv(uv[i][0], uv[i][1]).setColor(this.rCol, this.gCol, this.bCol, alpha).setLight(light);
		for(int i = 3; i >= 0; i--) buffer.addVertex(pX + corners[i].x, pY + corners[i].y, pZ + corners[i].z).setUv(uv[i][0], uv[i][1]).setColor(this.rCol, this.gCol, this.bCol, alpha).setLight(light);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return MukeParticleRenderType.ADDITIVE;
	}

	@Override
	public AABB getRenderBoundingBox(float partialTicks) {
		return getBoundingBox().inflate(this.scale + 1D);
	}
}
