package com.atomiclegacy.client;

import com.atomiclegacy.tileentity.SolarBoilerBlockEntity;
import net.minecraft.client.Minecraft;

/** Client-only bridge for render-quality state used by the boiler snapshot. */
public final class SolarBoilerClientHooks {
    private SolarBoilerClientHooks() {
    }

    public static void tick(SolarBoilerBlockEntity boiler) {
        boiler.tickClient(Minecraft.useFancyGraphics());
    }
}
