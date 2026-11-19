package com.atomiclegacy.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;

/** 1.7.10 com.hbm.particle.ParticleBlackPowderSpark (particles.png index 0, full bright). */
public class BlackPowderSparkParticle extends TextureSheetParticle {

	public BlackPowderSparkParticle(ClientLevel world, double x, double y, double z, double mX, double mY, double mZ) {
		super(world, x, y, z, mX, mY, mZ);

		this.xd = mX;
		this.yd = mY;
		this.zd = mZ;

		float f = this.random.nextFloat() * 0.1F + 0.2F;
		this.rCol = f + 0.7F;
		this.gCol = f + 0.5F;
		this.bCol = f;
		this.setSprite(BlackPowderEffects.sprite(BlackPowderEffects.GENERIC_0));
		this.setSize(0.02F, 0.02F);
		this.quadSize *= this.random.nextFloat() * 0.6F + 0.5F;
		this.lifetime = 15 + this.random.nextInt(5);

		this.gravity = 0.01F;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		this.yd -= gravity;
		this.move(this.xd, this.yd, this.zd);
		this.xd *= 0.95D;
		this.yd *= 0.95D;
		this.zd *= 0.95D;

		if(this.lifetime-- <= 0) {
			this.remove();
		}
	}

	@Override
	public int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return BlackPowderEffects.LEGACY_TRANSLUCENT;
	}
}
