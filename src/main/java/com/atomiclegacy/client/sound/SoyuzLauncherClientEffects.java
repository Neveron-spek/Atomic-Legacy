package com.atomiclegacy.client.sound;

import com.atomiclegacy.entity.SoyuzEntity;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * [parity P2: soyuz_launcher] Client half of TileEntitySoyuzLauncher#updateEntity: the
 * "block.soyuzReady" loop while counting down and the shockwave smoke ring (50 particles,
 * strength gaussian * 3 + 6, at y - 3) while a Soyuz is inside the 2 x 10 x 2 launch box.
 */
public final class SoyuzLauncherClientEffects {
    private static final Map<BlockPos, Loop> SOUNDS = new HashMap<>();

    private SoyuzLauncherClientEffects() {
    }

    public static void tick(SoyuzLauncherBlockEntity launcher) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != launcher.getLevel()) return;
        BlockPos pos = launcher.getBlockPos();
        boolean running = launcher.renderStarting() && launcher.renderCountdown() > 0;
        Loop loop = SOUNDS.get(pos);
        if (!running) {
            if (loop != null) {
                loop.finish();
                SOUNDS.remove(pos);
            }
        } else if (loop == null || loop.isStopped() || !client.getSoundManager().isActive(loop)) {
            loop = new Loop(launcher);
            SOUNDS.put(pos.immutable(), loop);
            client.getSoundManager().play(loop);
        }

        boolean rocket = !client.level.getEntitiesOfClass(SoyuzEntity.class,
                new AABB(pos.getX() - 0.5D, pos.getY(), pos.getZ() - 0.5D, pos.getX() + 1.5D, pos.getY() + 10, pos.getZ() + 1.5D)).isEmpty();
        if (rocket) {
            double strength = client.level.random.nextGaussian() * 3.0D + 6.0D;
            Vec3 vector = new Vec3(strength, 0.0D, 0.0D).yRot(client.level.random.nextInt(360));
            for (int i = 0; i < 50; i++) {
                double factor = client.level.random.nextDouble();
                client.level.addParticle(HbmParticleTypes.LAUNCHER_EX_SMOKE.get(),
                        pos.getX() + 0.5D, pos.getY() - 3.0D, pos.getZ() + 0.5D,
                        vector.x * factor, 0.0D, vector.z * factor);
                vector = vector.yRot(360.0F / 50.0F);
            }
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final SoyuzLauncherBlockEntity launcher;

        private Loop(SoyuzLauncherBlockEntity launcher) {
            super(HbmSoundEvents.SOYUZ_READY.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.launcher = launcher;
            this.looping = true;
            // [parity b2: soyuzReady] AudioDynamic: no engine attenuation; createAudioLoop(2.0F, range 100) followed by
            // audio.updateVolume(100) gives volume = (dist / 100) * -100 + 100 every tick (full until the last block).
            this.attenuation = Attenuation.NONE;
            this.volume = dynamicVolume();
            this.pitch = 1.0F;
            BlockPos pos = launcher.getBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }

        private float dynamicVolume() {
            net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
            if (player == null) return 0.0F;
            float dist = (float) Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
            return Math.max(0.0F, Math.min(1.0F, (dist / 100.0F) * -100.0F + 100.0F));
        }

        @Override
        public void tick() {
            this.volume = dynamicVolume();
            Minecraft client = Minecraft.getInstance();
            if (client.level != this.launcher.getLevel() || this.launcher.isRemoved()
                    || !this.launcher.renderStarting() || this.launcher.renderCountdown() <= 0) {
                finish();
            }
        }

        void finish() {
            stop();
        }
    }
}
