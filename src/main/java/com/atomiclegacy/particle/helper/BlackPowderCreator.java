package com.atomiclegacy.particle.helper;

import com.atomiclegacy.network.BlackPowderEffectPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 com.hbm.particle.helper.BlackPowderCreator (server half). The packet goes to every player
 * within 200 blocks (IParticleCreator.sendPacket range); the client half is
 * {@link com.atomiclegacy.client.particle.BlackPowderEffects#makeParticle}.
 */
public class BlackPowderCreator {

	public static void composeEffect(Level world, double x, double y, double z, double headingX, double headingY, double headingZ, int cloudCount, float cloudScale, float cloudSpeedMult, int sparkCount, float sparkSpeedMult) {
		if(!(world instanceof ServerLevel level)) return;
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, 200,
				new BlackPowderEffectPayload(x, y, z, headingX, headingY, headingZ, cloudCount, cloudScale, cloudSpeedMult, sparkCount, sparkSpeedMult));
	}
}
