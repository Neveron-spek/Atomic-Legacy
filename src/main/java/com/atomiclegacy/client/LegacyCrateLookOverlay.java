package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.tileentity.LegacyCrate;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** [parity R4: crates] BlockStorageCrate#printHook: a renamed crate shows its name in yellow. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyCrateLookOverlay {
    private LegacyCrateLookOverlay() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> HbmLookOverlayApi.registerProvider(AtomicLegacy.id("legacy_crate_name"), 2_000, (level, hit) -> {
            if (!(level.getBlockEntity(hit.getBlockPos()) instanceof LegacyCrate crate) || !crate.hasCustomName()) {
                return null;
            }
            return new HbmLookOverlayApi.OverlayData(Component.literal(crate.customName()), 0xFFFF00, List.of());
        }));
    }
}
