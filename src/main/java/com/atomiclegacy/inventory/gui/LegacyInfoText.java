package com.atomiclegacy.inventory.gui;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** [parity P2: GUI] I18nUtil.resolveKeyArray: one translated string split on "$", formatting codes kept. */
public final class LegacyInfoText {
    private LegacyInfoText() {
    }

    public static List<Component> lines(String key) {
        List<Component> lines = new ArrayList<>();
        for (String line : Component.translatable(key).getString().split("\\$")) {
            lines.add(Component.literal(line));
        }
        return lines;
    }
}
