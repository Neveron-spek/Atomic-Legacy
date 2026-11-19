package com.atomiclegacy.power;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.items.LegacyBatteryTooltips;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.tileentity.EnergyConverterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

/**
 * [parity L1: converters] RF (Forge Energy) side of machine_converter_he_rf / machine_converter_rf_he.
 * Registered with HIGH priority so it is queried before the generic HE&lt;-&gt;FE bridge of HbmCapabilities.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class EnergyConverterCapabilities {
    private EnergyConverterCapabilities() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, HbmBlockEntities.ENERGY_CONVERTER.get(),
                (converter, side) -> converter.energyStorage());
    }

    /** BlockConverterHeRf/RfHe.printHook: "-> xHE" (green arrow) and "<- yRF" (red arrow). */
    @EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Client {
        private Client() {
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            HbmLookOverlayApi.registerProvider(AtomicLegacy.id("energy_converter"), 2_000, (level, hit) -> {
                BlockEntity entity = level.getBlockEntity(hit.getBlockPos());
                if (!(entity instanceof EnergyConverterBlockEntity converter)) {
                    return null;
                }
                return new HbmLookOverlayApi.OverlayData(entity.getBlockState().getBlock().getName(), 0xFFFF00, List.of(
                        HbmLookOverlayApi.OverlayLine.white("§a-> §r" + LegacyBatteryTooltips.shortNumber(converter.power()) + "HE"),
                        HbmLookOverlayApi.OverlayLine.white("§c<- §r" + LegacyBatteryTooltips.shortNumber(converter.rf()) + "RF")));
            });
        }
    }
}
