package com.atomiclegacy.client.sound;

import java.util.HashMap;
import java.util.Map;

import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.CentrifugeBlockEntity;
import com.atomiclegacy.util.MachineMuffler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * [parity K3: centrifuge] Client half of TileEntityMachineCentrifuge.updateEntity (1.7.10): audioDuration += 2 while
 * progressing, -= 3 otherwise, clamped to 0..60; above 10 and within 25 blocks the CENTRIFUGE_LOOP
 * ("hbm:block.centrifugeOperate", AudioDynamic volume 1, range 10, keep-alive 20) plays with pitch
 * (audioDuration - 10) / 100 + 0.5. AudioDynamic volume: (dist / range) * -maxVolume + maxVolume.
 */
public final class CentrifugeClientSounds {
	private static final float RANGE = 10F;
	private static final int KEEP_ALIVE = 20;
	private static final Map<BlockPos, State> STATES = new HashMap<>();

	private CentrifugeClientSounds() {
	}

	public static void tick(CentrifugeBlockEntity centrifuge, boolean progressing) {
		Minecraft mc = Minecraft.getInstance();
		BlockPos pos = centrifuge.getBlockPos().immutable();
		State state = STATES.computeIfAbsent(pos, p -> new State());
		state.audioDuration = Mth.clamp(state.audioDuration + (progressing ? 2 : -3), 0, 60);
		boolean near = mc.player != null && mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 25 * 25;
		if(state.audioDuration > 10 && near) {
			if(state.sound == null || state.sound.isStopped() || !mc.getSoundManager().isActive(state.sound)) {
				state.sound = new Loop(centrifuge, pos);
				mc.getSoundManager().play(state.sound);
			}
			state.sound.pitchTarget = (state.audioDuration - 10) / 100F + 0.5F;
			state.sound.keepAlive = 0;
		} else if(state.sound != null) {
			state.sound.end();
			state.sound = null;
			if(state.audioDuration == 0) STATES.remove(pos);
		}
	}

	private static final class State {
		int audioDuration;
		Loop sound;
	}

	private static final class Loop extends AbstractTickableSoundInstance {
		private final CentrifugeBlockEntity owner;
		float pitchTarget = 0.5F;
		int keepAlive;

		Loop(CentrifugeBlockEntity owner, BlockPos pos) {
			super(HbmSoundEvents.CENTRIFUGE_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
			this.owner = owner;
			// AudioWrapperClient is positioned at xCoord, yCoord, zCoord (block corner)
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			this.looping = true;
			this.delay = 0;
			this.volume = 0F;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			if(mc.player == null || this.owner.isRemoved() || this.keepAlive++ > KEEP_ALIVE) {
				end();
				return;
			}
			double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
			float maxVolume = MachineMuffler.attenuate(this.owner, 1F);
			this.volume = Math.max(0F, (float) (dist / RANGE) * -maxVolume + maxVolume);
			this.pitch = this.pitchTarget;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		void end() {
			this.stop();
		}
	}
}
