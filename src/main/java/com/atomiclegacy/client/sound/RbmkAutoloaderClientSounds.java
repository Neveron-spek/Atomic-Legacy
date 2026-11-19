package com.atomiclegacy.client.sound;

import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * TileEntityRBMKAutoloader client audio: a looped "hbm:door.wgh_start"
 * (volume 0.75, range 25, keep-alive 5) while the piston is between 0.01 and
 * 0.99, followed by a single "hbm:door.wgh_stop" (volume 2) once it stops.
 */
public final class RbmkAutoloaderClientSounds {
    private static final double RANGE = 25.0D;
    private static final int KEEP_ALIVE_TICKS = 5;
    private static final Map<BlockPos, LiftSound> SOUNDS = new HashMap<>();

    private RbmkAutoloaderClientSounds() {
    }

    public static void tick(RbmkComponentBlockEntity autoloader, double renderPiston) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlockPos pos = autoloader.getBlockPos().immutable();
        LiftSound sound = SOUNDS.get(pos);
        if (renderPiston > 0.01D && renderPiston < 0.99D) {
            if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
                sound = new LiftSound(pos);
                SOUNDS.put(pos, sound);
                minecraft.getSoundManager().play(sound);
            }
            sound.keepAlive = KEEP_ALIVE_TICKS;
        } else if (sound != null) {
            sound.requestStop();
            minecraft.level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    HbmSoundEvents.WGH_STOP.get(), SoundSource.BLOCKS, 2.0F, 1.0F, false);
        }
    }

    private static final class LiftSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private int keepAlive = KEEP_ALIVE_TICKS;

        private LiftSound(BlockPos pos) {
            super(HbmSoundEvents.WGH_START.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.volume = 0.0F;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null || keepAlive-- <= 0) {
                requestStop();
                return;
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (!(blockEntity instanceof RbmkComponentBlockEntity)) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
            this.volume = distance >= RANGE ? 0.0F : (float) (0.75D * (1.0D - distance / RANGE));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void requestStop() {
            stop();
            SOUNDS.remove(pos, this);
        }
    }
}
