package com.atomiclegacy.client.gun;

import com.atomiclegacy.client.particle.GunPlasmaPulseParticle;
import com.atomiclegacy.network.GunPlasmaPulsePayload;

import net.minecraft.client.Minecraft;

/** [weapons batch D1] client side of the batch D1 effect packets. */
public final class GunBatchD1ClientEffects {

	private GunBatchD1ClientEffects() {
	}

	/** ClientProxy#effectNT "plasmablast": new ParticlePlasmaBlast(..., r, g, b, pitch, yaw).setScale(scale). */
	public static void plasmaPulse(GunPlasmaPulsePayload payload) {
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null) return;
		mc.particleEngine.add(new GunPlasmaPulseParticle(mc.level, payload.x(), payload.y(), payload.z(),
				payload.r(), payload.g(), payload.b(), payload.pitch(), payload.yaw(), payload.scale()));
	}
}
