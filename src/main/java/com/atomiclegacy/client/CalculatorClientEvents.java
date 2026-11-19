package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** [parity S3: calculator] HbmKeybinds#keyEvent: the calculator key (only seen without an open screen) opens GUIScreenCalculator. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class CalculatorClientEvents {
    private CalculatorClientEvents() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (HbmKeyMappings.CALCULATOR.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new CalculatorScreen());
            }
        }
    }
}
