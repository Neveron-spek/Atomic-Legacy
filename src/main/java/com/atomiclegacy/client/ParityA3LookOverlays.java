package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.tileentity.IcfControllerBlockEntity;
import com.atomiclegacy.util.LegacyI18nFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** [parity A3: icf_controller] MachineICFController.printHook: "power/maxPowerHE" (short numbers) under the yellow name. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ParityA3LookOverlays {
    private ParityA3LookOverlays() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("parity_a3_icf_controller"), 1_500, (level, hit) ->
                level.getBlockEntity(hit.getBlockPos()) instanceof IcfControllerBlockEntity icf
                        ? new OverlayData(level.getBlockState(hit.getBlockPos()).getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW,
                        List.of(HbmLookOverlayApi.whiteLine(LegacyI18nFormat.shortNumber(icf.power()) + "/"
                                + LegacyI18nFormat.shortNumber(icf.maxPower()) + "HE")))
                        : null);
    }
}
