package com.atomiclegacy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * [parity S2: configs] 1.7.10 {@code ServerConfig} (hbmServer.json) entries that the port had hard-coded.
 * Keys keep their original names; defaults are the original ConfigWrapper defaults. Built inside
 * {@link HbmConfig}'s spec (section {@code hbmServer}) so they live in the common config file.
 */
public final class HbmServerParityConfig {
    public static ModConfigSpec.BooleanValue CRATE_OPEN_HELD;
    public static ModConfigSpec.BooleanValue CRATE_KEEP_CONTENTS;
    public static ModConfigSpec.IntValue ITEM_HAZARD_DROP_TICKRATE;
    public static ModConfigSpec.BooleanValue ENABLE_MKU;
    public static ModConfigSpec.IntValue MAX_BUFFER_LENGTH;
    public static ModConfigSpec.IntValue MAX_STACK_SIZE;
    /** WeaponConfig 10.05_dropDead. */
    public static ModConfigSpec.BooleanValue DROPPED_DEAD_MANS_EXPLOSIVE;

    private HbmServerParityConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("hbmServer");
        CRATE_OPEN_HELD = builder
                .comment("ServerConfig CRATE_OPEN_HELD: crates can be opened from the hand; placing them then requires sneaking. HBM 1.7.10 default: true.")
                .define("CRATE_OPEN_HELD", true);
        CRATE_KEEP_CONTENTS = builder
                .comment("ServerConfig CRATE_KEEP_CONTENTS: broken crates keep their contents in the dropped item. HBM 1.7.10 default: true.")
                .define("CRATE_KEEP_CONTENTS", true);
        ITEM_HAZARD_DROP_TICKRATE = builder
                .comment("ServerConfig ITEM_HAZARD_DROP_TICKRATE: ticks between hazard updates of dropped items (values below 1 count as 1). HBM 1.7.10 default: 2.")
                .defineInRange("ITEM_HAZARD_DROP_TICKRATE", 2, Integer.MIN_VALUE, Integer.MAX_VALUE);
        ENABLE_MKU = builder
                .comment("ServerConfig ENABLE_MKU: enables the MKU contagion. HBM 1.7.10 default: true.")
                .define("ENABLE_MKU", true);
        MAX_BUFFER_LENGTH = builder
                .comment("ServerConfig MAX_BUFFER_LENGTH: maximum length of the AUTOCAL buffer. HBM 1.7.10 default: 256.")
                .defineInRange("MAX_BUFFER_LENGTH", 256, Integer.MIN_VALUE, Integer.MAX_VALUE);
        MAX_STACK_SIZE = builder
                .comment("ServerConfig MAX_STACK_SIZE: size of the AUTOCAL parse stack (the MS-ES1 language never pushes to it). HBM 1.7.10 default: 256.")
                .defineInRange("MAX_STACK_SIZE", 256, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();
        builder.push("dangerousItemsS2");
        DROPPED_DEAD_MANS_EXPLOSIVE = builder
                .comment("WeaponConfig 10.05_dropDead: whether a dropped dead man's explosive (detonator_de) explodes. The item is removed either way. HBM 1.7.10 default: true.")
                .define("droppedDeadMansExplosive", true);
        builder.pop();
    }

    public static boolean dropDead() {
        return !HbmConfig.SPEC.isLoaded() || DROPPED_DEAD_MANS_EXPLOSIVE.get();
    }

    public static boolean crateOpenHeld() {
        return !HbmConfig.SPEC.isLoaded() || CRATE_OPEN_HELD.get();
    }

    public static boolean crateKeepContents() {
        return !HbmConfig.SPEC.isLoaded() || CRATE_KEEP_CONTENTS.get();
    }

    public static int itemHazardDropTickrate() {
        return Math.max(1, HbmConfig.SPEC.isLoaded() ? ITEM_HAZARD_DROP_TICKRATE.get() : 2);
    }

    public static boolean enableMku() {
        return !HbmConfig.SPEC.isLoaded() || ENABLE_MKU.get();
    }

    public static int maxBufferLength() {
        return HbmConfig.SPEC.isLoaded() ? MAX_BUFFER_LENGTH.get() : 256;
    }
}
