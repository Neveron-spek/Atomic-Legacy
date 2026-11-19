package com.atomiclegacy.client.gun;

import com.atomiclegacy.network.GunTinyExplosionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/** Client side of weapons batch B effects. */
public final class GunBatchBClientEffects {

	private GunBatchBClientEffects() {
	}

	/**
	 * ClientProxy#effectNT "vanillaExt"/"largeexplode": an EntityLargeExplodeFX of the given size
	 * (HugeExplosionParticle, same size formula) tinted (1, 0.9, 0.5) * r, plus count grey
	 * EntityExplodeFX (POOF / ExplodeParticle) scaled by i + 1.
	 */
	public static void largeExplode(GunTinyExplosionPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		if(world == null) return;
		RandomSource rand = world.random;
		double x = payload.x();
		double y = payload.y();
		double z = payload.z();

		Particle fx = mc.particleEngine.createParticle(ParticleTypes.EXPLOSION, x, y, z, payload.size(), 0.0D, 0.0D);
		float r = 1.0F - rand.nextFloat() * 0.2F;
		if(fx != null) fx.setColor(1F * r, 0.9F * r, 0.5F * r);

		for(int i = 0; i < payload.count(); i++) {
			Particle sec = mc.particleEngine.createParticle(ParticleTypes.POOF, x, y, z, 0.0D, 0.0D, 0.0D);
			float r2 = 1.0F - rand.nextFloat() * 0.5F;
			if(sec != null) {
				sec.setColor(0.5F * r2, 0.5F * r2, 0.5F * r2);
				sec.scale(i + 1);
			}
		}
	}
}
