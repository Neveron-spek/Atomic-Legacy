package com.atomiclegacy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation settings retained from HBM 1.7.10. */
public final class HbmClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_COOLING_TOWER_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_LARGE_MACHINE_OCCLUSION_CULLING;
    /** HBM 1.7.10 ClientConfig GUN_MODEL_FOV / GUN_VISUAL_RECOIL / GUN_ANIMATION_SPEED. */
    public static final ModConfigSpec.BooleanValue GUN_MODEL_FOV;
    public static final ModConfigSpec.BooleanValue GUN_VISUAL_RECOIL;
    public static final ModConfigSpec.DoubleValue GUN_ANIMATION_SPEED;
    /** HBM 1.7.10 ClientConfig GUN_ANIMS_LEGACY. */
    public static final ModConfigSpec.BooleanValue GUN_ANIMS_LEGACY;
    // [parity V3: ClientConfig] keys of hbmClient.json with a ported consumer
    public static final ModConfigSpec.IntValue GEIGER_OFFSET_HORIZONTAL;
    public static final ModConfigSpec.IntValue GEIGER_OFFSET_VERTICAL;
    public static final ModConfigSpec.IntValue INFO_OFFSET_HORIZONTAL;
    public static final ModConfigSpec.IntValue INFO_OFFSET_VERTICAL;
    public static final ModConfigSpec.IntValue INFO_POSITION;
    public static final ModConfigSpec.BooleanValue DODD_RBMK_DIAGNOSTIC;
    public static final ModConfigSpec.BooleanValue RENDER_CABLE_HANG;
    public static final ModConfigSpec.BooleanValue RENDER_REEDS;
    public static final ModConfigSpec.IntValue RENDER_HELIOSTAT_BEAM_LIMIT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("particles");
        ENABLE_COOLING_TOWER_PARTICLES = builder
                .comment("是否显示冷却塔工作时的水蒸气粒子效果。默认值与 HBM 1.7.10 一致：true。")
                .define("enableCoolingTowerParticles", true);
        builder.pop();
        builder.push("rendering");
        ENABLE_LARGE_MACHINE_OCCLUSION_CULLING = builder
                .comment("使用原版区段遮挡图剔除被地形完全遮挡的大型设备；只在设备完整渲染包围盒覆盖的所有区段均不可见时生效。")
                .define("enableLargeMachineOcclusionCulling", true);
        builder.pop();
        builder.push("weapons");
        GUN_MODEL_FOV = builder
                .comment("HBM 1.7.10 GUN_MODEL_FOV: first-person gun models use the FOV setting instead of 70. Default: false.")
                .define("gunModelFov", false);
        GUN_VISUAL_RECOIL = builder
                .comment("HBM 1.7.10 GUN_VISUAL_RECOIL: guns kick the camera when fired. Default: true.")
                .define("gunVisualRecoil", true);
        GUN_ANIMATION_SPEED = builder
                .comment("HBM 1.7.10 GUN_ANIMATION_SPEED: multiplier for gun animation playback speed. Default: 1.0.")
                .defineInRange("gunAnimationSpeed", 1.0D, 0.0D, Double.MAX_VALUE);
        GUN_ANIMS_LEGACY = builder
                .comment("HBM 1.7.10 GUN_ANIMS_LEGACY: use the legacy hand-made animations for guns that also have loaded ones. Default: false.")
                .define("gunAnimsLegacy", false);
        builder.pop();
        // [parity V3: ClientConfig] original names and defaults (ClientConfig ConfigWrapper values)
        builder.push("hbmClient");
        GEIGER_OFFSET_HORIZONTAL = builder.comment("ClientConfig GEIGER_OFFSET_HORIZONTAL: horizontal offset of the Geiger counter HUD. HBM 1.7.10 default: 0.")
                .defineInRange("GEIGER_OFFSET_HORIZONTAL", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        GEIGER_OFFSET_VERTICAL = builder.comment("ClientConfig GEIGER_OFFSET_VERTICAL: vertical offset of the Geiger counter HUD. HBM 1.7.10 default: 0.")
                .defineInRange("GEIGER_OFFSET_VERTICAL", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        INFO_OFFSET_HORIZONTAL = builder.comment("ClientConfig INFO_OFFSET_HORIZONTAL: horizontal offset of the info message box. HBM 1.7.10 default: 0.")
                .defineInRange("INFO_OFFSET_HORIZONTAL", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        INFO_OFFSET_VERTICAL = builder.comment("ClientConfig INFO_OFFSET_VERTICAL: vertical offset of the info message box. HBM 1.7.10 default: 0.")
                .defineInRange("INFO_OFFSET_VERTICAL", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        INFO_POSITION = builder.comment("ClientConfig INFO_POSITION: info message box position (0 top left, 1 top right, 2 right of the crosshair, other values left of the crosshair). HBM 1.7.10 default: 0.")
                .defineInRange("INFO_POSITION", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DODD_RBMK_DIAGNOSTIC = builder.comment("ClientConfig DODD_RBMK_DIAGNOSTIC: enables the look-at block/item info overlays (ILookOverlay hooks). HBM 1.7.10 default: true.")
                .define("DODD_RBMK_DIAGNOSTIC", true);
        RENDER_CABLE_HANG = builder.comment("ClientConfig RENDER_CABLE_HANG: pylon wires sag; when off they are straight. HBM 1.7.10 default: true.")
                .define("RENDER_CABLE_HANG", true);
        RENDER_REEDS = builder.comment("ClientConfig RENDER_REEDS: reeds extend down to the water floor; when off they are one block tall. HBM 1.7.10 default: true (false with Angelica).")
                .define("RENDER_REEDS", true);
        RENDER_HELIOSTAT_BEAM_LIMIT = builder.comment("ClientConfig RENDER_HELIOSTAT_BEAM_LIMIT: maximum heliostat beams rendered per solar boiler. HBM 1.7.10 default: 250.")
                .defineInRange("RENDER_HELIOSTAT_BEAM_LIMIT", 250, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private HbmClientConfig() {
    }

    public static double gunAnimationSpeed() {
        return SPEC.isLoaded() ? GUN_ANIMATION_SPEED.get() : 1.0D;
    }

    public static boolean gunModelFov() {
        return SPEC.isLoaded() && GUN_MODEL_FOV.get();
    }

    /** ClientConfig.GUN_ANIMS_LEGACY; false where the client spec is not loaded (dedicated server default). */
    public static boolean gunAnimsLegacy() {
        return SPEC.isLoaded() && GUN_ANIMS_LEGACY.get();
    }

    // [parity V3: ClientConfig] defaults where the client spec is not loaded (dedicated server)
    public static int geigerOffsetHorizontal() { return SPEC.isLoaded() ? GEIGER_OFFSET_HORIZONTAL.get() : 0; }
    public static int geigerOffsetVertical() { return SPEC.isLoaded() ? GEIGER_OFFSET_VERTICAL.get() : 0; }
    public static int infoOffsetHorizontal() { return SPEC.isLoaded() ? INFO_OFFSET_HORIZONTAL.get() : 0; }
    public static int infoOffsetVertical() { return SPEC.isLoaded() ? INFO_OFFSET_VERTICAL.get() : 0; }
    public static int infoPosition() { return SPEC.isLoaded() ? INFO_POSITION.get() : 0; }
    public static boolean doddRbmkDiagnostic() { return !SPEC.isLoaded() || DODD_RBMK_DIAGNOSTIC.get(); }
    public static boolean renderCableHang() { return !SPEC.isLoaded() || RENDER_CABLE_HANG.get(); }
    public static boolean renderReeds() { return !SPEC.isLoaded() || RENDER_REEDS.get(); }
    public static int renderHeliostatBeamLimit() { return SPEC.isLoaded() ? RENDER_HELIOSTAT_BEAM_LIMIT.get() : 250; }

    public static boolean gunVisualRecoil() {
        return !SPEC.isLoaded() || GUN_VISUAL_RECOIL.get();
    }
}
