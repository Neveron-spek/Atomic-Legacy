package com.atomiclegacy.client;

import com.atomiclegacy.inventory.gui.RbmkPanelScreen;
import com.atomiclegacy.tileentity.RbmkPanelBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** [parity T2] FMLNetworkHandler.openGui on the client for the RoR panels (they had no container). Client only. */
public final class RbmkPanelClientHooks {
    private RbmkPanelClientHooks() {
    }

    public static void open(RbmkPanelBlockEntity panel) {
        Screen screen = switch (panel.kind()) {
            case GAUGE -> new RbmkPanelScreen.Gauge(panel);
            case KEY_PAD -> new RbmkPanelScreen.KeyPad(panel);
            case LEVER -> new RbmkPanelScreen.Lever(panel);
            case INDICATOR -> new RbmkPanelScreen.Indicator(panel);
            case NUMITRON -> new RbmkPanelScreen.Numitron(panel);
            case GRAPH -> new RbmkPanelScreen.Graph(panel);
            case TERMINAL -> new RbmkPanelScreen.Terminal(panel);
            default -> null;
        };
        if (screen != null) Minecraft.getInstance().setScreen(screen);
    }
}
