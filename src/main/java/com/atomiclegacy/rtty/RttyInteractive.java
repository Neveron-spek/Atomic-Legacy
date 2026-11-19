package com.atomiclegacy.rtty;

import java.util.Locale;

/** A machine that accepts the original HBM redstone-over-radio command format. */
public interface RttyInteractive extends RttyInfo {
    String NAME_SEPARATOR = "!";
    String PARAM_SEPARATOR = ":";

    String EX_NULL = "Exception: Null Command";
    String EX_NAME = "Exception: Multiple Name Separators";
    String EX_FORMAT = "Exception: Parameter in Invalid Format";

    String runRttyFunction(String name, String[] params);

    /** [parity T2: ROR] false for blocks that share a Java class with an IRORInteractive but were not one in 1.7.10. */
    default boolean rttyInteractive() {
        return true;
    }

    static String command(String input) {
        if (input == null || input.isEmpty()) throw new RttyFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length == 0 || parts.length > 2) throw new RttyFunctionException(EX_NAME);
        if (parts[0].isEmpty()) throw new RttyFunctionException(EX_NULL);
        return parts[0].toLowerCase(Locale.ROOT);
    }

    static String[] params(String input) {
        if (input == null || input.isEmpty()) throw new RttyFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length == 0 || parts.length > 2) throw new RttyFunctionException(EX_NAME);
        return parts.length == 1 ? new String[0] : parts[1].split(PARAM_SEPARATOR);
    }

    static int parseInt(String value) {
        return parseInt(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    static int parseInt(String value, int minimum, int maximum) {
        int parsedResult;
        try {
            parsedResult = Integer.parseInt(value);
        } catch (RuntimeException integerFailure) {
            try {
                double parsed = Double.parseDouble(value);
                parsedResult = (int) Math.round(parsed);
            } catch (RuntimeException decimalFailure) {
                throw new RttyFunctionException(EX_FORMAT);
            }
        }
        int result = parsedResult;
        if (result < minimum || result > maximum) throw new RttyFunctionException(EX_FORMAT);
        return result;
    }

    // [parity T2: ROR] IRORInteractive.parseLong / parseBool; String.split above drops
    // trailing empty parts exactly like the 1.7.10 split(String) calls.
    static long parseLong(String value, long minimum, long maximum) {
        long result;
        try {
            result = Long.parseLong(value);
        } catch (RuntimeException integerFailure) {
            try {
                result = Math.round(Double.parseDouble(value));
            } catch (RuntimeException decimalFailure) {
                throw new RttyFunctionException(EX_FORMAT);
            }
        }
        if (result < minimum || result > maximum) throw new RttyFunctionException(EX_FORMAT);
        return result;
    }

    static boolean parseBool(String value) {
        switch (value.toLowerCase()) {
            case "1", "true", "on": return true;
            case "0", "false", "off": return false;
            default: throw new RttyFunctionException(EX_FORMAT);
        }
    }
}
