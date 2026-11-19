package com.atomiclegacy.inventory.gui;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/** [parity g2] I18nClient#autoBreak of 1.7.10: greedy split on single spaces, never breaks inside a word. */
final class LegacyTextWrap {
    private LegacyTextWrap() {
    }

    /** I18nClient#autoBreakWithParagraphs: '$' starts a new paragraph. */
    static List<String> autoBreakWithParagraphs(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\$")) lines.addAll(autoBreak(font, paragraph, width));
        return lines;
    }

    static List<String> autoBreak(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        lines.add(words[0]);
        int indent = font.width(words[0]);
        for (int w = 1; w < words.length; w++) {
            indent += font.width(" " + words[w]);
            if (indent <= width) {
                lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " " + words[w]);
            } else {
                lines.add(words[w]);
                indent = font.width(words[w]);
            }
        }
        return lines;
    }
}
