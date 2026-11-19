package com.atomiclegacy.tileentity;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.atomiclegacy.config.HbmConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * [parity b2: RBMKDials] com.hbm.tileentity.machine.rbmk.RBMKDials — the RBMK dials are game rules
 * (/gamerule dialXxx) with the original names, defaults and clamps. As in the original the clamped
 * values are cached and refreshed on world load and whenever a rule is changed; game rules are one
 * set per server in 1.21, so the per-world list of the original collapses into one cache.
 */
public final class RbmkDials {
    private static final List<GameRules.Key<?>> KEYS = new ArrayList<>();
    private static final List<Consumer<GameRules>> RESETTERS = new ArrayList<>();

    public static final GameRules.Key<GameRules.BooleanValue> KEY_SAVE_DIALS = bool("dialSaveDials", true);
    public static final GameRules.Key<DoubleValue> KEY_PASSIVE_COOLING = dbl("dialPassiveCooling", 2.5D);
    public static final GameRules.Key<DoubleValue> KEY_PASSIVE_COOLING_INNER = dbl("dialPassiveCoolingInner", 0.1D);
    public static final GameRules.Key<DoubleValue> KEY_COLUMN_HEAT_FLOW = dbl("dialColumnHeatFlow", 0.2D);
    public static final GameRules.Key<DoubleValue> KEY_FUEL_DIFFUSION_MOD = dbl("dialDiffusionMod", 1.0D);
    public static final GameRules.Key<DoubleValue> KEY_HEAT_PROVISION = dbl("dialHeatProvision", 0.2D);
    public static final GameRules.Key<GameRules.IntegerValue> KEY_COLUMN_HEIGHT = integer("dialColumnHeight", 4);
    public static final GameRules.Key<GameRules.BooleanValue> KEY_PERMANENT_SCRAP = bool("dialEnablePermaScrap", true);
    public static final GameRules.Key<DoubleValue> KEY_BOILER_HEAT_CONSUMPTION = dbl("dialBoilerHeatConsumption", 0.1D);
    public static final GameRules.Key<DoubleValue> KEY_CONTROL_SPEED_MOD = dbl("dialControlSpeed", 1.0D);
    public static final GameRules.Key<DoubleValue> KEY_REACTIVITY_MOD = dbl("dialReactivityMod", 1.0D);
    public static final GameRules.Key<DoubleValue> KEY_OUTGASSER_MOD = dbl("dialOutgasserSpeedMod", 1.0D);
    public static final GameRules.Key<DoubleValue> KEY_SURGE_MOD = dbl("dialControlSurgeMod", 1.0D);
    public static final GameRules.Key<GameRules.IntegerValue> KEY_FLUX_RANGE = integer("dialFluxRange", 5);
    public static final GameRules.Key<GameRules.IntegerValue> KEY_REASIM_RANGE = integer("dialReasimRange", 10);
    public static final GameRules.Key<GameRules.BooleanValue> KEY_REASIM_BOILERS = bool("dialReasimBoilers", false);
    public static final GameRules.Key<DoubleValue> KEY_REASIM_BOILER_SPEED = dbl("dialReasimBoilerSpeed", 0.05D);
    public static final GameRules.Key<GameRules.BooleanValue> KEY_DISABLE_MELTDOWNS = bool("dialDisableMeltdowns", false);
    public static final GameRules.Key<GameRules.BooleanValue> KEY_ENABLE_MELTDOWN_OVERPRESSURE = bool("dialEnableMeltdownOverpressure", false);
    public static final GameRules.Key<DoubleValue> KEY_MODERATOR_EFFICIENCY = dbl("dialModeratorEfficiency", 1.0D);
    public static final GameRules.Key<DoubleValue> KEY_ABSORBER_EFFICIENCY = dbl("dialAbsorberEfficiency", 1.0D);
    public static final GameRules.Key<DoubleValue> KEY_REFLECTOR_EFFICIENCY = dbl("dialReflectorEfficiency", 1.0D);
    public static final GameRules.Key<GameRules.BooleanValue> KEY_DISABLE_DEPLETION = bool("dialDisableDepletion", false);
    public static final GameRules.Key<GameRules.BooleanValue> KEY_DISABLE_XENON = bool("dialDisableXenon", false);
    public static final GameRules.Key<DoubleValue> KEY_ABSORBER_HEAT_CONVERSION = dbl("dialAbsorberHeatConversion", 0.05D);

    // cache (RBMKDials.gameRules); defaults until a server refreshes it — the original hands out defValue on the client
    private static volatile double passiveCooling;
    private static volatile double passiveCoolingInner;
    private static volatile double columnHeatFlow;
    private static volatile double fuelDiffusionMod;
    private static volatile double fuelHeatProvision;
    private static volatile int columnHeight;
    private static volatile boolean permaScrap;
    private static volatile double boilerHeatConsumption;
    private static volatile double controlSpeed;
    private static volatile double reactivityMod;
    private static volatile double outgasserMod;
    private static volatile double surgeMod;
    private static volatile int fluxRange;
    private static volatile int reasimRange;
    private static volatile boolean reasimBoilers;
    private static volatile double reasimBoilerSpeed;
    private static volatile boolean meltdownsDisabled;
    private static volatile boolean overpressure;
    private static volatile double moderatorEfficiency;
    private static volatile double absorberEfficiency;
    private static volatile double reflectorEfficiency;
    private static volatile boolean disableDepletion;
    private static volatile boolean disableXenon;
    private static volatile double absorberHeatConversion;

    static {
        clear();
    }

    private RbmkDials() {
    }

    /** Forces class initialization so the rules exist before any GameRules instance is built. */
    public static void bootstrap() {
    }

    public static List<GameRules.Key<?>> keys() {
        return List.copyOf(KEYS);
    }

    private static GameRules.Key<GameRules.BooleanValue> bool(String name, boolean def) {
        GameRules.Key<GameRules.BooleanValue> key = GameRules.register(name, GameRules.Category.MISC,
                GameRules.BooleanValue.create(def, (server, value) -> refresh(server.getGameRules())));
        KEYS.add(key);
        RESETTERS.add(rules -> rules.getRule(key).set(def, null));
        return key;
    }

    private static GameRules.Key<GameRules.IntegerValue> integer(String name, int def) {
        GameRules.Key<GameRules.IntegerValue> key = GameRules.register(name, GameRules.Category.MISC,
                GameRules.IntegerValue.create(def, (server, value) -> refresh(server.getGameRules())));
        KEYS.add(key);
        RESETTERS.add(rules -> rules.getRule(key).set(def, null));
        return key;
    }

    private static GameRules.Key<DoubleValue> dbl(String name, double def) {
        GameRules.Key<DoubleValue> key = GameRules.register(name, GameRules.Category.MISC, DoubleValue.create(def));
        KEYS.add(key);
        RESETTERS.add(rules -> rules.getRule(key).set(def));
        return key;
    }

    /** RBMKDials.createDials(world): unless dialSaveDials is set, every dial returns to its default on load. */
    public static void createDials(GameRules rules) {
        if (!rules.getBoolean(KEY_SAVE_DIALS)) {
            RESETTERS.forEach(resetter -> resetter.accept(rules));
        }
        refresh(rules);
    }

    /** RBMKDials.refresh(world). */
    public static void refresh(GameRules rules) {
        passiveCooling = Math.max(rules.getRule(KEY_PASSIVE_COOLING).get(), 0.0D);
        passiveCoolingInner = Math.max(rules.getRule(KEY_PASSIVE_COOLING_INNER).get(), 0.0D);
        columnHeatFlow = Mth.clamp(rules.getRule(KEY_COLUMN_HEAT_FLOW).get(), 0.0D, 1.0D);
        fuelDiffusionMod = Math.max(rules.getRule(KEY_FUEL_DIFFUSION_MOD).get(), 0.0D);
        fuelHeatProvision = Mth.clamp(rules.getRule(KEY_HEAT_PROVISION).get(), 0.0D, 1.0D);
        columnHeight = Mth.clamp(rules.getInt(KEY_COLUMN_HEIGHT), 2, 16) - 1;
        permaScrap = rules.getBoolean(KEY_PERMANENT_SCRAP);
        boilerHeatConsumption = Math.max(rules.getRule(KEY_BOILER_HEAT_CONSUMPTION).get(), 0.0D);
        controlSpeed = Math.max(rules.getRule(KEY_CONTROL_SPEED_MOD).get(), 0.0D);
        reactivityMod = Math.max(rules.getRule(KEY_REACTIVITY_MOD).get(), 0.0D);
        outgasserMod = Math.max(rules.getRule(KEY_OUTGASSER_MOD).get(), 0.0D);
        surgeMod = Math.max(rules.getRule(KEY_SURGE_MOD).get(), 0.0D);
        fluxRange = Mth.clamp(rules.getInt(KEY_FLUX_RANGE), 1, 100);
        reasimRange = Mth.clamp(rules.getInt(KEY_REASIM_RANGE), 1, 100);
        reasimBoilers = rules.getBoolean(KEY_REASIM_BOILERS);
        reasimBoilerSpeed = Mth.clamp(rules.getRule(KEY_REASIM_BOILER_SPEED).get(), 0.0D, 1.0D);
        meltdownsDisabled = rules.getBoolean(KEY_DISABLE_MELTDOWNS);
        overpressure = rules.getBoolean(KEY_ENABLE_MELTDOWN_OVERPRESSURE);
        moderatorEfficiency = Mth.clamp(rules.getRule(KEY_MODERATOR_EFFICIENCY).get(), 0.0D, 1.0D);
        absorberEfficiency = Mth.clamp(rules.getRule(KEY_ABSORBER_EFFICIENCY).get(), 0.0D, 1.0D);
        absorberHeatConversion = Mth.clamp(rules.getRule(KEY_ABSORBER_HEAT_CONVERSION).get(), 0.0D, 1.0D);
        reflectorEfficiency = Mth.clamp(rules.getRule(KEY_REFLECTOR_EFFICIENCY).get(), 0.0D, 1.0D);
        disableDepletion = rules.getBoolean(KEY_DISABLE_DEPLETION);
        disableXenon = rules.getBoolean(KEY_DISABLE_XENON);
    }

    /** No server: defValue of every key, which is what the original returns without a server world. */
    public static void clear() {
        passiveCooling = 2.5D;
        passiveCoolingInner = 0.1D;
        columnHeatFlow = 0.2D;
        fuelDiffusionMod = 1.0D;
        fuelHeatProvision = 0.2D;
        columnHeight = 4 - 1;
        permaScrap = true;
        boilerHeatConsumption = 0.1D;
        controlSpeed = 1.0D;
        reactivityMod = 1.0D;
        outgasserMod = 1.0D;
        surgeMod = 1.0D;
        fluxRange = 5;
        reasimRange = 10;
        reasimBoilers = false;
        reasimBoilerSpeed = 0.05D;
        meltdownsDisabled = false;
        overpressure = false;
        moderatorEfficiency = 1.0D;
        absorberEfficiency = 1.0D;
        reflectorEfficiency = 1.0D;
        disableDepletion = false;
        disableXenon = false;
        absorberHeatConversion = 0.05D;
    }

    public static double getPassiveCooling() { return passiveCooling; }
    public static double getPassiveCoolingInner() { return passiveCoolingInner; }
    public static double getColumnHeatFlow() { return columnHeatFlow; }
    public static double getFuelDiffusionMod() { return fuelDiffusionMod; }
    public static double getFuelHeatProvision() { return fuelHeatProvision; }
    /** [1;15]: blocks above the base, like the original (dialColumnHeight - 1). */
    public static int getColumnHeight() { return columnHeight; }
    public static boolean getPermaScrap() { return permaScrap; }
    public static double getBoilerHeatConsumption() { return boilerHeatConsumption; }
    public static double getControlSpeed() { return controlSpeed; }
    public static double getReactivityMod() { return reactivityMod; }
    public static double getOutgasserMod() { return outgasserMod; }
    public static double getSurgeMod() { return surgeMod; }
    public static int getFluxRange() { return fluxRange; }
    public static int getReaSimRange() { return reasimRange; }

    /** dialReasimBoilers || GeneralConfig.enable528ReasimBoilers. */
    public static boolean getReasimBoilers() {
        return reasimBoilers || HbmConfig.ENABLE_528_MODE.get() && HbmConfig.ENABLE_528_FORCE_REASIM_BOILERS.get();
    }

    public static double getReaSimBoilerSpeed() { return reasimBoilerSpeed; }
    public static boolean getMeltdownsDisabled() { return meltdownsDisabled; }
    public static boolean getOverpressure() { return overpressure; }
    public static double getModeratorEfficiency() { return moderatorEfficiency; }
    public static double getAbsorberEfficiency() { return absorberEfficiency; }
    public static double getReflectorEfficiency() { return reflectorEfficiency; }
    public static boolean getDepletion() { return !disableDepletion; }
    public static boolean getXenon() { return !disableXenon; }
    public static double getAbsorberHeatConversion() { return absorberHeatConversion; }

    /** A decimal game rule: 1.7.10 rules are strings parsed with Double.parseDouble (GameRuleHelper). */
    public static final class DoubleValue extends GameRules.Value<DoubleValue> {
        private final double def;
        private double value;

        static GameRules.Type<DoubleValue> create(double def) {
            return new GameRules.Type<>(
                    DoubleArgumentType::doubleArg,
                    type -> new DoubleValue(type, def, def),
                    (server, value) -> refresh(server.getGameRules()),
                    (visitor, key, type) -> visitor.visit(key, type));
        }

        DoubleValue(GameRules.Type<DoubleValue> type, double def, double value) {
            super(type);
            this.def = def;
            this.value = value;
        }

        public double get() {
            return this.value;
        }

        public void set(double value) {
            this.value = value;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> context, String paramName) {
            this.value = DoubleArgumentType.getDouble(context, paramName);
        }

        /** GameRuleHelper.parseDouble: an unreadable value falls back to the default. */
        @Override
        protected void deserialize(String value) {
            try {
                this.value = Double.parseDouble(value);
            } catch (Exception ex) {
                this.value = this.def;
            }
        }

        @Override
        public String serialize() {
            return Double.toString(this.value);
        }

        @Override
        public int getCommandResult() {
            return (int) this.value;
        }

        @Override
        protected DoubleValue getSelf() {
            return this;
        }

        @Override
        protected DoubleValue copy() {
            return new DoubleValue(this.type, this.def, this.value);
        }

        @Override
        public void setFrom(DoubleValue value, @Nullable MinecraftServer server) {
            this.value = value.value;
            this.onChanged(server);
        }
    }
}
