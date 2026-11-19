package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.gui.SoyuzLauncherScreen;
import com.atomiclegacy.items.SoyuzItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * [parity P2] Client registrations of the satellite batch kept out of the shared client registries:
 * ItemSoyuz icons soyuz_0..2 by skin, the soyuz_launcher GUI/renderer and the struct_soyuz_core preview.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SatelliteClientSetup {
    private SatelliteClientSetup() {
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(HbmItems.MISSILE_SOYUZ.get(),
                AtomicLegacy.id("soyuz_skin"), (stack, level, entity, seed) -> SoyuzItem.skin(stack)));
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(HbmMenus.SOYUZ_LAUNCHER.get(), SoyuzLauncherScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HbmBlockEntities.SOYUZ_LAUNCHER.get(), SoyuzLauncherBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOYUZ_STRUCT.get(), SoyuzStructBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        SoyuzLauncherBlockEntityRenderer.registerAdditionalModels(event);
    }

    @SubscribeEvent
    public static void afterBake(ModelEvent.BakingCompleted event) {
        Minecraft.getInstance().execute(SoyuzStructBlockEntityRenderer::invalidateAfterBake);
    }
}
