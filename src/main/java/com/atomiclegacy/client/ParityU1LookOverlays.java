package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayLine;
import com.atomiclegacy.fluid.HbmFluidTraitInfo;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.ArrayList;
import java.util.List;

/** [parity U1: network_separator] BlockNetworkSeparator#printHook (title 0xFFFF00, background 0x404000). */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ParityU1LookOverlays {
    private static final String GREEN = "§a";
    private static final String RED = "§c";
    private static final String YELLOW = "§e";
    private static final String RESET = "§r";

    private ParityU1LookOverlays() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("parity_u1_network_separator"), 1_500, (level, hit) -> {
            BlockEntity entity = level.getBlockEntity(hit.getBlockPos());
            if (entity instanceof MachineDummyBlockEntity dummy) {
                entity = level.getBlockEntity(dummy.getCorePos());
            }
            if (!(entity instanceof NetworkSeparatorBlockEntity separator)) {
                return null;
            }
            List<OverlayLine> text = new ArrayList<>();
            text.add(OverlayLine.white(GREEN + "-> " + RESET + HbmFluidTraitInfo.shortNumber(separator.deltaTick[19]) + "HE/t" + RED + " ->"));
            text.add(OverlayLine.white(GREEN + "-> " + RESET + HbmFluidTraitInfo.shortNumber(separator.deltaSecond) + "HE/s" + RED + " ->"));
            text.add(OverlayLine.white("Configured: " + HbmFluidTraitInfo.shortNumber(separator.configuredLimit) + "HE/t"));
            text.add(OverlayLine.white("Priority: " + YELLOW + separator.priority.name()));
            if (separator.redstone) text.add(OverlayLine.white(RED + "Disabled by redstone"));
            return new OverlayData(separator.getBlockState().getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
        });
    }
}
