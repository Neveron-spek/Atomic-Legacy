package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayLine;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [parity T1] ILookOverlay.printHook of FlowControlPump and the ExhaustPipeAnchor hook text
 * (title colour 0xFFFF00).
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ParityT1LookOverlays {
    private static final String GREEN = "§a";
    private static final String RED = "§c";
    private static final String YELLOW = "§e";
    private static final String RESET = "§r";

    private ParityT1LookOverlays() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("parity_t1_fluid_network"), 1_500, (level, hit) -> {
            BlockEntity entity = level.getBlockEntity(hit.getBlockPos());
            if (entity instanceof MachineDummyBlockEntity dummy) {
                entity = level.getBlockEntity(dummy.getCorePos());
            }
            if (entity instanceof FlowControlPumpBlockEntity pump) {
                List<OverlayLine> text = new ArrayList<>();
                text.add(OverlayLine.white(GREEN + "-> " + RESET + Component.translatable(pump.tank.type().translationKey()).getString()
                        + " (" + pump.tank.pressure() + " PU): " + format(pump.bufferSize) + "mB/t" + RED + " ->"));
                text.add(OverlayLine.white("Priority: " + YELLOW + pump.priority.name()));
                if (pump.redstone) text.add(OverlayLine.white(RED + "Disabled by redstone"));
                if (pump.tank.amount() > 0) text.add(OverlayLine.white(format(pump.tank.amount()) + "mB buffered"));
                return new OverlayData(pump.getBlockState().getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
            }
            if (level.getBlockState(hit.getBlockPos()).is(HbmBlocks.PIPE_ANCHOR_EXHAUST.get())) {
                List<OverlayLine> text = new ArrayList<>();
                for (String smoke : new String[]{"smoke", "smoke_leaded", "smoke_poison"}) {
                    com.atomiclegacy.registry.HbmFluids.byName(smoke).ifPresent(fluid ->
                            text.add(OverlayLine.white(Component.translatable(fluid.translationKey()).getString())));
                }
                return new OverlayData(level.getBlockState(hit.getBlockPos()).getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
            }
            return null;
        });
    }

    /** BobMathUtil.format */
    private static String format(int amount) {
        return String.format(Locale.US, "%,d", amount);
    }
}
