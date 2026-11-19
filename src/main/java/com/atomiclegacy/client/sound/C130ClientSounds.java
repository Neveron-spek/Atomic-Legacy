package com.atomiclegacy.client.sound;

import com.atomiclegacy.entity.LegacyC130Entity;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

/**
 * [parity W1: airdrop] EntityC130 client audio: getLoopedSound("hbm:entity.bomberLoop", volume 2,
 * range 250, pitch 1, keepAlive 20) while DataWatcher 17 (health) > 0, stopped otherwise.
 * AudioDynamic: attenuation NONE, volume = maxVolume - dist / range * maxVolume (sound engine clamps to [0, 1]);
 * sounds.json category "player".
 */
public final class C130ClientSounds {
    private static final float MAX_VOLUME = 2.0F;
    private static final double RANGE = 250.0D;
    private static final int KEEP_ALIVE = 20;
    private static final Map<Integer, LoopSound> SOUNDS = new HashMap<>();

    private C130ClientSounds() {
    }

    public static void tick(LegacyC130Entity plane) {
        Minecraft minecraft = Minecraft.getInstance();
        if (plane.syncedHealth() > 0.0F) {
            LoopSound audio = SOUNDS.get(plane.getId());
            if (audio == null || !minecraft.getSoundManager().isActive(audio)) {
                audio = new LoopSound(plane);
                SOUNDS.put(plane.getId(), audio);
                minecraft.getSoundManager().play(audio);
            }
            audio.keepAlive();
            audio.updatePosition(plane.getX(), plane.getY(), plane.getZ());
        } else {
            LoopSound audio = SOUNDS.remove(plane.getId());
            if (audio != null) {
                audio.stopSound();
            }
        }
    }

    private static final class LoopSound extends AbstractTickableSoundInstance {
        private final int entityId;
        private int timeSinceKA;

        private LoopSound(LegacyC130Entity plane) {
            super(HbmSoundEvents.ENTITY_BOMBER_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.entityId = plane.getId();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.attenuation = SoundInstance.Attenuation.NONE;
            updatePosition(plane.getX(), plane.getY(), plane.getZ());
            this.volume = volumeAt();
        }

        private void updatePosition(double x, double y, double z) {
            // AudioWrapperClient#updatePosition takes floats; the 1.7.10 client player posY is the eye height.
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
        }

        private float volumeAt() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return MAX_VOLUME;
            }
            float f = (float) Math.sqrt(Math.pow(x - minecraft.player.getX(), 2)
                    + Math.pow(y - minecraft.player.getEyeY(), 2) + Math.pow(z - minecraft.player.getZ(), 2));
            return (float) ((f / RANGE) * -MAX_VOLUME + MAX_VOLUME);
        }

        @Override
        public void tick() {
            this.volume = volumeAt();
            if (this.timeSinceKA > KEEP_ALIVE) {
                stopSound();
            }
            this.timeSinceKA++;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void keepAlive() {
            this.timeSinceKA = 0;
        }

        private void stopSound() {
            stop();
            SOUNDS.remove(entityId, this);
        }
    }
}
