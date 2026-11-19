package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.items.CustomMachineStructureItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.render.CustomMachineBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.ArrayList;
import java.util.List;

/** [parity X1: custom machines] ClientProxy: RenderCustomMachine and the ItemCMStructure look overlay (held item first). */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomMachineClientEvents {
    private CustomMachineClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HbmBlockEntities.CUSTOM_MACHINE.get(), CustomMachineBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("structure_custommachine"), Integer.MAX_VALUE, (level, hit) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return null;
            ItemStack stack = minecraft.player.getMainHandItem();
            if (!(stack.getItem() instanceof CustomMachineStructureItem)) return null;
            List<HbmLookOverlayApi.OverlayLine> lines = new ArrayList<>();
            for (String text : CustomMachineStructureItem.overlayLines(stack)) lines.add(HbmLookOverlayApi.OverlayLine.white(text));
            return new HbmLookOverlayApi.OverlayData(stack.getHoverName(), 0xffff00, lines);
        });
    }
}
