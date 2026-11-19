package com.atomiclegacy.client.gun;

import java.util.List;

import com.atomiclegacy.network.VntStandardExplosionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

/** Client side of weapons batch C2 effects. */
public final class GunBatchC2ClientEffects {

	private GunBatchC2ClientEffects() {
	}

	/**
	 * ExplosionEffectStandard#performClient: "hugeexplosion" (EXPLOSION_EMITTER) for size >= 2, otherwise
	 * "largeexplode" (EXPLOSION), then one "explode" (POOF) and one "smoke" (SMOKE) per affected block.
	 */
	public static void standardExplosion(VntStandardExplosionPayload payload) {
		ClientLevel world = Minecraft.getInstance().level;
		if(world == null) return;
		double x = payload.x();
		double y = payload.y();
		double z = payload.z();
		float size = payload.size();

		if(size >= 2.0F) {
			world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
		} else {
			world.addParticle(ParticleTypes.EXPLOSION, x, y, z, 1.0D, 0.0D, 0.0D);
		}
		List<BlockPos> affectedBlocks = payload.affectedBlocks();
		int count = affectedBlocks.size();
		for(int i = 0; i < count; i++) {
			BlockPos pos = affectedBlocks.get(i);
			double oX = (double) ((float) pos.getX() + world.random.nextFloat());
			double oY = (double) ((float) pos.getY() + world.random.nextFloat());
			double oZ = (double) ((float) pos.getZ() + world.random.nextFloat());
			double dX = oX - x;
			double dY = oY - y;
			double dZ = oZ - z;
			double delta = (double) Mth.sqrt((float) (dX * dX + dY * dY + dZ * dZ)) / 1D;
			dX /= delta;
			dY /= delta;
			dZ /= delta;
			double mod = 0.5D / (delta / (double) size + 0.1D);
			mod *= (double) (world.random.nextFloat() * world.random.nextFloat() + 0.3F);
			dX *= mod;
			dY *= mod;
			dZ *= mod;
			world.addParticle(ParticleTypes.POOF, (oX + x * 1.0D) / 2.0D, (oY + y * 1.0D) / 2.0D, (oZ + z * 1.0D) / 2.0D, dX, dY, dZ);
			world.addParticle(ParticleTypes.SMOKE, oX, oY, oZ, dX, dY, dZ);
		}
	}
}
