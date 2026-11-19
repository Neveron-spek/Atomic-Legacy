package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayLine;
import com.atomiclegacy.blocks.IndustrialGeneratorBlock;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** [parity h1] MachineIGenerator#printHook: printGeneric("Industrial Generator Memorial", 0xff8000, 0x804000, text). */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ParityH1LookOverlays {
    private ParityH1LookOverlays() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("parity_h1_industrial_generator"), 1_500, (level, hit) -> {
            if (!(level.getBlockState(hit.getBlockPos()).getBlock() instanceof IndustrialGeneratorBlock)) {
                return null;
            }
            // hardcoded English in the original (no lang key)
            return new OverlayData(Component.literal("Industrial Generator Memorial"), 0xFF8000, 0x804000,
                    List.of(OverlayLine.white("In memory of all that we have lost")));
        });
    }
}
