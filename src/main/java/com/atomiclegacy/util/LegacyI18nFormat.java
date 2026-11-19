package com.atomiclegacy.util;

import net.minecraft.locale.Language;

import java.util.IllegalFormatException;

/**
 * [parity M2] 1.7.10 I18n.format / Locale#formatMessage: String.format on the translated text, and on an
 * IllegalFormatException the literal "Format error: " + untranslated pattern (I18nUtil.resolveKeyArray then splits
 * the result at NTM's '$' line break).
 */
public final class LegacyI18nFormat {

    private LegacyI18nFormat() {
    }

    public static String format(String key, Object... args) {
        String pattern = Language.getInstance().getOrDefault(key);
        try {
            return String.format(pattern, args);
        } catch (IllegalFormatException e) {
            return "Format error: " + pattern;
        }
    }

    public static String[] formatArray(String key, Object... args) {
        return format(key, args).split("\\$");
    }

    /** BobMathUtil.getShortNumber */
    public static String shortNumber(long l) {
        double res;
        String suffix;
        long abs = Math.abs(l);
        if (abs >= Math.pow(10, 18)) { res = l / Math.pow(10, 18); suffix = "E"; }
        else if (abs >= Math.pow(10, 15)) { res = l / Math.pow(10, 15); suffix = "P"; }
        else if (abs >= Math.pow(10, 12)) { res = l / Math.pow(10, 12); suffix = "T"; }
        else if (abs >= Math.pow(10, 9)) { res = l / Math.pow(10, 9); suffix = "G"; }
        else if (abs >= Math.pow(10, 6)) { res = l / Math.pow(10, 6); suffix = "M"; }
        else if (abs >= Math.pow(10, 3)) { res = l / Math.pow(10, 3); suffix = "k"; }
        else return Long.toString(l);
        if (res <= -100.0) res = Math.round(res * 10.0) / 10.0;
        else res = Math.round(res * 100.0) / 100.0;
        return res + suffix;
    }
}
