package com.atomiclegacy.client.gun;

import java.util.Map;
import java.util.WeakHashMap;

import com.atomiclegacy.network.GunMukeEffectPayload;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/** [weapons batch F2] Client side of the tau cannon spinup loop and the Fat Man "muke" / "tinytot" aux particles. */
public final class GunBatchF2ClientEffects {

	private GunBatchF2ClientEffects() {
	}

	private static final Map<LivingEntity, TauLoopSound> LOOPS = new WeakHashMap<>();

	/**
	 * ORCHESTRA_TAU client branch: MainRegistry.proxy.getLoopedSound(GUN_TAU_LOOP, volume 1, range 15, pitch 0.75,
	 * keepAlive 10) attached to the entity while the SPINUP timer is below 300, pitch 0.75 + timer * 0.01; stopped at
	 * 300 and on any other animation (timer -1).
	 */
	public static void tauLoop(LivingEntity entity, int timer) {
		Minecraft mc = Minecraft.getInstance();
		if(timer < 0 || timer >= 300) {
			TauLoopSound running = LOOPS.remove(entity);
			if(running != null) running.requestStop();
			return;
		}
		TauLoopSound running = LOOPS.get(entity);
		if(running == null || running.isStopped() || !mc.getSoundManager().isActive(running)) {
			TauLoopSound audio = new TauLoopSound(entity);
			audio.setPitch(0.75F);
			LOOPS.put(entity, audio);
			mc.getSoundManager().play(audio);
			return;
		}
		running.keepAlive();
		running.setPitch(0.75F + timer * 0.01F);
	}

	private static final class TauLoopSound extends AbstractTickableSoundInstance {
		private final LivingEntity entity;
		private int keepAliveTicks = 10;

		private TauLoopSound(LivingEntity entity) {
			super(HbmSoundEvents.GUN_TAU_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.entity = entity;
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();
			this.looping = true;
			this.delay = 0;
			this.volume = 1.0F;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
		}

		@Override
		public void tick() {
			if(this.entity.isRemoved() || this.keepAliveTicks-- <= 0) {
				requestStop();
				return;
			}
			// AudioWrapper.attachTo(entity)
			this.x = this.entity.getX();
			this.y = this.entity.getY();
			this.z = this.entity.getZ();
		}

		private void setPitch(float pitch) {
			this.pitch = pitch;
		}

		private void keepAlive() {
			this.keepAliveTicks = 10;
		}

		private void requestStop() {
			this.stop();
		}
	}

	/** ClientProxy.effectNT "muke" (wave + flash, camera hurt tilt) and "tinytot" (wave + mushroom clouds). */
	public static void mukeEffect(GunMukeEffectPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		LocalPlayer player = mc.player;
		if(world == null || player == null) return;
		double x = payload.x(), y = payload.y(), z = payload.z();
		RandomSource rand = world.random;

		if(payload.mode() == GunMukeEffectPayload.MODE_MUKE) {
			world.addParticle(HbmParticleTypes.MUKE_WAVE.get(), x, y, z, 0D, 0D, 0D);
			world.addParticle(payload.balefire() ? HbmParticleTypes.MUKE_FLASH_BALEFIRE.get() : HbmParticleTypes.MUKE_FLASH.get(), x, y, z, 0D, 0D, 0D);
			//single swing: 			HT 15,  MHT 15
			player.hurtTime = 15;
			player.hurtDuration = 15;
		}

		if(payload.mode() == GunMukeEffectPayload.MODE_TINYTOT) {
			world.addParticle(HbmParticleTypes.MUKE_WAVE.get(), x, y, z, 0D, 0D, 0D);
			SimpleParticleType cloud = HbmParticleTypes.MUKE_CLOUD.get();

			for(double d = 0.0D; d <= 1.6D; d += 0.1) {
				world.addParticle(cloud, x, y, z, rand.nextGaussian() * 0.05, d + rand.nextGaussian() * 0.02, rand.nextGaussian() * 0.05);
			}
			for(int i = 0; i < 50; i++) {
				world.addParticle(cloud, x, y + 0.5, z, rand.nextGaussian() * 0.5, rand.nextInt(5) == 0 ? 0.02 : 0, rand.nextGaussian() * 0.5);
			}
			for(int i = 0; i < 15; i++) {
				double ix = rand.nextGaussian() * 0.2;
				double iz = rand.nextGaussian() * 0.2;

				if(ix * ix + iz * iz > 0.75) {
					ix *= 0.5;
					iz *= 0.5;
				}

				double iy = 1.6 + (rand.nextDouble() * 2 - 1) * (0.75 - (ix * ix + iz * iz)) * 0.5;

				world.addParticle(cloud, x, y, z, ix, iy + rand.nextGaussian() * 0.02, iz);
			}
			player.hurtTime = 15;
			player.hurtDuration = 15;
		}
	}
}
