package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.network.PlayerInformPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [parity V3: ClientConfig.INFO_*] RenderInfoSystem: entries by id, sorted by lifetime, drawn in a translucent box whose
 * position follows INFO_POSITION plus INFO_OFFSET_HORIZONTAL/VERTICAL; text fades out over the last half of its life.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class PlayerInformOverlay {
    private static final Map<Integer, Line> LINES = new HashMap<>();

    private PlayerInformOverlay() {
    }

    public static void accept(PlayerInformPayload payload) {
        Component message = payload.translated()
                ? Component.translatable(payload.message(), payload.args().stream()
                        .map(arg -> arg.startsWith("@") ? Component.translatable(arg.substring(1)) : arg)
                        .toArray())
                : Component.literal(payload.message());
        LINES.put(payload.line(), new Line(message, payload.color(), System.currentTimeMillis(), (int) payload.lifetimeMillis()));
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        long now = System.currentTimeMillis();
        LINES.entrySet().removeIf(entry -> entry.getValue().start() + entry.getValue().millis() < now);
        if (LINES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;

        List<Line> entries = new ArrayList<>(LINES.values());
        entries.sort((a, b) -> Integer.compare(a.millis(), b.millis()));

        int longest = 0;
        for (Line entry : entries) {
            longest = Math.max(longest, font.width(entry.message()));
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int mode = HbmClientConfig.infoPosition();
        int pX = mode == 0 ? 15 : mode == 1 ? (width - longest - 15) : mode == 2 ? (width / 2 + 7) : (width / 2 - longest - 6);
        int pZ = mode == 0 ? 15 : mode == 1 ? 15 : height / 2 + 7;
        pX += HbmClientConfig.infoOffsetHorizontal();
        pZ += HbmClientConfig.infoOffsetVertical();

        int side = pX + 5 + longest;
        int boxBottom = LINES.size() * 10 + pZ + 2;
        // tess.setColorRGBA_F(0.25F, 0.25F, 0.25F, 0.5F)
        graphics.fill(pX - 5, pZ - 5, side, boxBottom, 0x7F3F3F3F);

        int off = 0;
        for (Line entry : entries) {
            int elapsed = (int) (now - entry.start());
            int alpha = Math.max(Math.min(510 * (entry.millis() - elapsed) / Math.max(1, entry.millis()), 255), 5);
            int color = entry.color() + (alpha << 24 & -0xffffff);
            graphics.drawString(font, entry.message(), pX, pZ + off, color, false);
            off += 10;
        }
    }

    private record Line(Component message, int color, long start, int millis) {
    }
}
