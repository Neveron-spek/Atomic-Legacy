package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.util.MachineMuffler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** Attenuates one-shot block sounds emitted at a muffled machine or proxy. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class MachineMufflerEvents {
    private MachineMufflerEvents() {
    }

    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSource() != SoundSource.BLOCKS) {
            return;
        }
        BlockPos pos = BlockPos.containing(event.getPosition());
        if (MachineMuffler.isMuffled(MachineMuffler.resolveCore(event.getLevel(), pos))) {
            event.setNewVolume(event.getNewVolume() * MachineMuffler.VOLUME_MULTIPLIER);
        }
    }
}
