package com.atomiclegacy.client.sound;

import com.atomiclegacy.entity.LegacyBomberEntity;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

/**
 * EntityBomber (1.7.10) flyover loop: getLoopedSound(bomberSmallLoop / bomberLoop, volume 2, range 250, pitch 1,
 * keepAlive 20) through AudioDynamic. [parity A1] keep-alive 20 ticks (was 10), sounds.json category "player"
 * -> PLAYERS (was HOSTILE); no distance cut-off - AudioDynamic keeps playing, func(dist) only lowers the volume.
 */
public final class BomberClientSounds {
    private static final double AUDIBLE_DISTANCE = 250.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<Integer, FlyoverSound> SOUNDS = new HashMap<>();

    private BomberClientSounds() {
    }

    public static void tick(LegacyBomberEntity bomber) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || bomber.isRemoved() || !bomber.isFlying()) {
            stop(bomber.getId());
            return;
        }
        FlyoverSound sound = SOUNDS.get(bomber.getId());
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new FlyoverSound(bomber);
            SOUNDS.put(bomber.getId(), sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void stop(int entityId) {
        FlyoverSound sound = SOUNDS.remove(entityId);
        if (sound != null) {
            sound.requestStop();
        }
    }

    private static final class FlyoverSound extends AbstractTickableSoundInstance {
        private final int entityId;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private FlyoverSound(LegacyBomberEntity bomber) {
            super(bomber.style() <= 4 ? HbmSoundEvents.ENTITY_BOMBER_SMALL_LOOP.get() : HbmSoundEvents.ENTITY_BOMBER_LOOP.get(),
                    SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.entityId = bomber.getId();
            this.x = bomber.getX();
            this.y = bomber.getY();
            this.z = bomber.getZ();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.volume = 0.0F;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null
                    || !(minecraft.level.getEntity(entityId) instanceof LegacyBomberEntity bomber) || bomber.isRemoved()
                    || !bomber.isFlying() || keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            x = bomber.getX();
            y = bomber.getY();
            z = bomber.getZ();
            double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
            // AudioDynamic.func: (dist / range) * -maxVolume + maxVolume; SoundManager clamps negative volume to 0
            volume = Math.max(0.0F, (float) (distance / AUDIBLE_DISTANCE) * -2.0F + 2.0F);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void keepAlive() {
            keepAliveTicks = KEEP_ALIVE_TICKS;
        }

        private void requestStop() {
            stop();
            SOUNDS.remove(entityId, this);
        }
    }
}
