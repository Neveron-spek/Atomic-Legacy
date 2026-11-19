package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.registry.HbmBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** [parity V2: pipe_anchor_pneumatic] BasePipeAnchor.printHook: the block name (0xFFFF00), no hook text. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ParityV2LookOverlays {
    private ParityV2LookOverlays() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("parity_v2_pneumatic_anchor"), 1_500, (level, hit) ->
                level.getBlockState(hit.getBlockPos()).is(HbmBlocks.PIPE_ANCHOR_PNEUMATIC.get())
                        ? new OverlayData(level.getBlockState(hit.getBlockPos()).getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, List.of())
                        : null);
    }
}
