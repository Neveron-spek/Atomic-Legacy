package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * [parity V3: geiger HUD] ModEventHandlerClient "HANDLE GEIGER COUNTER HUD" + RenderScreenOverlay.renderRadCounter:
 * on the HOTBAR overlay, unless the chestplate is an ArmorFSB set with customGeiger, a player carrying geiger_counter
 * in the main inventory sees a 1000 RAD bar (overlay_misc 94x18) at (16 + GEIGER_OFFSET_HORIZONTAL,
 * height - 20 - GEIGER_OFFSET_VERTICAL), the RAD/s change of the last second and the 2.5/10/25 RAD/s warning icons.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class ParityV3GeigerHud {
    private static final ResourceLocation MISC = AtomicLegacy.id("textures/misc/overlay_misc.png");
    private static long lastSurvey;
    private static float prevResult;
    private static float lastResult;

    private ParityV3GeigerHud() {
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        if (ArmorFSBItem.hasFSBArmor(player) && ArmorFSBItem.features(player).customGeiger()) {
            return;
        }
        boolean hasGeiger = false;
        for (ItemStack stack : player.getInventory().items) { // InventoryPlayer.hasItem: main inventory only
            if (stack.is(HbmItems.GEIGER_COUNTER.get())) {
                hasGeiger = true;
                break;
            }
        }
        if (hasGeiger) {
            renderRadCounter(event.getGuiGraphics(), HbmLivingRadiation.get(player).getRadiation());
        }
    }

    static void renderRadCounter(GuiGraphics graphics, float in) {
        float radiation = lastResult - prevResult;
        if (System.currentTimeMillis() >= lastSurvey + 1000) {
            lastSurvey = System.currentTimeMillis();
            prevResult = lastResult;
            lastResult = in;
        }
        int length = 74;
        int maxRad = 1000;
        int bar = getScaled(in, maxRad, 74);
        int posX = 16 + HbmClientConfig.geigerOffsetHorizontal();
        int posY = graphics.guiHeight() - 20 - HbmClientConfig.geigerOffsetVertical();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(MISC, posX, posY, 0, 0, 94, 18);
        graphics.blit(MISC, posX + 1, posY + 1, 1, 19, bar, 16);
        if (radiation >= 25) graphics.blit(MISC, posX + length + 2, posY - 18, 36, 36, 18, 18);
        else if (radiation >= 10) graphics.blit(MISC, posX + length + 2, posY - 18, 18, 36, 18, 18);
        else if (radiation >= 2.5) graphics.blit(MISC, posX + length + 2, posY - 18, 0, 36, 18, 18);

        var font = Minecraft.getInstance().font;
        if (radiation > 1000) graphics.drawString(font, ">1000 RAD/s", posX, posY - 8, 0xFF0000, false);
        else if (radiation >= 1) graphics.drawString(font, ((int) Math.round(radiation)) + " RAD/s", posX, posY - 8, 0xFF0000, false);
        else if (radiation > 0) graphics.drawString(font, "<1 RAD/s", posX, posY - 8, 0xFF0000, false);
        RenderSystem.disableBlend();
    }

    private static int getScaled(double cur, double max, double scale) {
        return (int) Math.min(cur / max * scale, scale);
    }
}
