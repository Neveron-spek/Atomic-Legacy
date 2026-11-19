package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.ArmorFSBItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Small, non-invasive replacement for ArmorFSB's custom Geiger/VATS HUD. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class LegacyArmorHudClient {
    private LegacyArmorHudClient() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null
                || !ArmorFSBItem.hasFSBArmor(minecraft.player)) {
            return;
        }
        ArmorFSBItem.FeatureProfile profile = ArmorFSBItem.features(minecraft.player);
        GuiGraphics graphics = event.getGuiGraphics();
        int y = 6;
        // [parity V3: geiger HUD] ArmorFSB.customGeiger draws nothing itself in 1.7.10: it only suppresses the
        // Geiger counter HUD (ParityV3GeigerHud); the invented "hud.atomiclegacy.geiger" line was removed.
        if (profile.vats()) {
            graphics.drawString(minecraft.font,
                    Component.translatable("hud.atomiclegacy.vats"),
                    6, y, 0xFF5050, true);
            if (minecraft.hitResult instanceof EntityHitResult entityHit
                    && entityHit.getEntity() instanceof LivingEntity target
                    && target.isAlive()) {
                y += 11;
                graphics.drawString(minecraft.font,
                        Component.translatable("hud.atomiclegacy.vats_target",
                                target.getDisplayName(), target.getHealth(), target.getMaxHealth()),
                        6, y, 0xFF8080, true);
            }
        }
    }
}
