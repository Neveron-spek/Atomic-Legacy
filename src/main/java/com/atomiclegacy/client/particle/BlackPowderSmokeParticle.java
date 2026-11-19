package com.atomiclegacy.client.particle;

import java.awt.Color;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/**
 * 1.7.10 com.hbm.particle.ParticleBlackPowderSmoke (an EntityFXRotating). The in-plane rotation of
 * renderParticleRotated is the 1.21 quad roll; the entity id parity that picks the spin direction is
 * replaced by an alternating counter (consecutive 1.7.10 entity ids alternate the same way).
 */
public class BlackPowderSmokeParticle extends TextureSheetParticle {

	private static int idCounter;

	public float hue;
	private final float particleScale;
	private final int entityId;
	private float rotationPitch;
	private float prevRotationPitch;
	private float interp;

	public BlackPowderSmokeParticle(ClientLevel world, double x, double y, double z, float scale) {
		super(world, x, y, z);
		this.setSprite(BlackPowderEffects.sprite(BlackPowderEffects.PARTICLE_BASE));
		this.entityId = idCounter++ & Integer.MAX_VALUE;
		this.lifetime = 30 + random.nextInt(15);
		this.particleScale = scale * 0.9F + random.nextFloat() * 0.2F;

		this.gravity = 0F;

		this.hue = 20F + random.nextFloat() * 20F;
		Color color = Color.getHSBColor(hue / 255F, 1F, 1F);
		this.rCol = color.getRed() / 255F;
		this.gCol = color.getGreen() / 255F;
		this.bCol = color.getBlue() / 255F;

		this.hasPhysics = false;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		this.age++;

		if(this.age >= this.lifetime) {
			this.remove();
		}

		this.yd -= gravity;
		this.prevRotationPitch = this.rotationPitch;

		float ageScaled = (float) this.age / (float) this.lifetime;
		this.rotationPitch += (1 - ageScaled) * 2 * ((this.entityId % 2) - 0.5);

		this.xd *= 0.65D;
		this.yd *= 0.65D;
		this.zd *= 0.65D;

		this.move(this.xd, this.yd, this.zd);
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		double ageScaled = (double) (this.age + partialTicks) / (double) this.lifetime;

		Color color = Color.getHSBColor(hue / 255F, Math.max(1F - (float) ageScaled * 4F, 0), Mth.clamp(1.25F - (float) ageScaled * 2F, 0.7F, 1F));
		this.rCol = color.getRed() / 255F;
		this.gCol = color.getGreen() / 255F;
		this.bCol = color.getBlue() / 255F;

		float particleAlpha = (float) Math.pow(1 - Math.min(ageScaled, 1), 0.25);
		this.alpha = particleAlpha * 0.25F;

		this.oRoll = this.prevRotationPitch * Mth.DEG_TO_RAD;
		this.roll = this.rotationPitch * Mth.DEG_TO_RAD;
		this.interp = partialTicks;
		super.render(buffer, camera, partialTicks);
	}

	@Override
	public float getQuadSize(float partialTicks) {
		double ageScaled = (double) (this.age + interp) / (double) this.lifetime;
		return (float) ((0.25 + ageScaled + (this.age + interp) * 0.025) * this.particleScale);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return BlackPowderEffects.LEGACY_TRANSLUCENT;
	}
}
