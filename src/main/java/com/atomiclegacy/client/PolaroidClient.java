package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * [parity J1] ModItems: polaroid and glitch use the texture "polaroid_" / "glitch_" + MainRegistry.polaroidID. The
 * item model picks the variant through the property atomiclegacy:polaroid_id = polaroidID / 32 (clamped properties,
 * exact binary fractions).
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PolaroidClient {
    public static final float PROPERTY_SCALE = 1.0F / 32.0F;

    private PolaroidClient() {
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (String name : new String[] {"polaroid", "glitch"}) {
                Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(name));
                if (item != Items.AIR) {
                    ItemProperties.register(item, AtomicLegacy.id("polaroid_id"),
                            (stack, level, entity, seed) -> AtomicLegacy.polaroidID * PROPERTY_SCALE);
                }
            }
        });
    }
}
