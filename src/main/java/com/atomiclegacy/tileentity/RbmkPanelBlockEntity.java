package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.rtty.HbmRttySystem;
import com.atomiclegacy.rtty.RttyInfo;
import com.atomiclegacy.rtty.RttyInteractive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity T2] Redstone-over-Radio panels of 1.7.10: TileEntityRBMKGauge, TileEntityRBMKKeyPad,
 * TileEntityRBMKLever, TileEntityRBMKIndicator, TileEntityRBMKNumitron, TileEntityRBMKGraph and
 * TileEntityRBMKTerminal. They share the RBMK component block entity type of the port; the type factory
 * {@link #create} chooses this class for the panel kinds. OpenComputers callbacks are not ported (no OC).
 */
public class RbmkPanelBlockEntity extends RbmkComponentBlockEntity {
    private static final String NET = "t2PanelNet";

    public final GaugeUnit[] gauges = new GaugeUnit[4];
    public final KeyUnit[] keys = new KeyUnit[4];
    public final LeverUnit[] levers = new LeverUnit[2];
    public final IndicatorUnit[] indicators = new IndicatorUnit[6];
    public final DisplayUnit[] displays = new DisplayUnit[2];
    public final GraphUnit[] graphs = new GraphUnit[2];

    /** TileEntityRBMKTerminal */
    public final String[] history = new String[17];
    public String channel = "";
    public String repeatCmd = "";
    public boolean doesRepeat = false;
    public boolean ocMode = false;

    private CompoundTag lastSent;

    public RbmkPanelBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        for (int i = 0; i < 4; i++) gauges[i] = new GaugeUnit(i);
        for (int i = 0; i < 4; i++) keys[i] = new KeyUnit(i);
        for (int i = 0; i < 2; i++) levers[i] = new LeverUnit(i);
        for (int i = 0; i < 6; i++) indicators[i] = new IndicatorUnit(i);
        for (int i = 0; i < 2; i++) displays[i] = new DisplayUnit(i);
        for (int i = 0; i < 2; i++) graphs[i] = new GraphUnit(i);
    }

    public static RbmkComponentBlockEntity create(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof RbmkComponentBlock block && isPanel(block.kind())) {
            return new RbmkPanelBlockEntity(pos, state);
        }
        return new RbmkComponentBlockEntity(pos, state);
    }

    public static boolean isPanel(RbmkComponentBlock.Kind kind) {
        return switch (kind) {
            case GAUGE, KEY_PAD, LEVER, INDICATOR, NUMITRON, GRAPH, TERMINAL -> true;
            default -> false;
        };
    }

    public void tickPanel(Level level) {
        RbmkComponentBlock.Kind kind = kind();
        if (level.isClientSide) {
            if (kind == RbmkComponentBlock.Kind.GAUGE) for (GaugeUnit gauge : gauges) gauge.updateClient();
            if (kind == RbmkComponentBlock.Kind.LEVER) for (LeverUnit lever : levers) lever.updateClient();
            return;
        }
        switch (kind) {
            case GAUGE -> { for (GaugeUnit gauge : gauges) gauge.update(); }
            case KEY_PAD -> { for (KeyUnit key : keys) key.update(); }
            case LEVER -> { for (LeverUnit lever : levers) lever.update(); }
            case INDICATOR -> { for (IndicatorUnit indicator : indicators) indicator.update(); }
            case NUMITRON -> { for (DisplayUnit display : displays) display.update(); }
            case GRAPH -> { if (level.getGameTime() % 10 == 0) for (GraphUnit graph : graphs) graph.update(); }
            case TERMINAL -> {
                if (!ocMode && !channel.isEmpty() && !repeatCmd.isEmpty()) {
                    HbmRttySystem.broadcast(level, channel, repeatCmd + "");
                }
            }
            default -> { }
        }
        // networkPackNT: the port only sends a block entity update when the packed state changed.
        CompoundTag net = new CompoundTag();
        writeNet(net);
        if (!net.equals(lastSent)) {
            lastSent = net;
            setChanged();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** IGUIProvider/IControlReceiver.hasPermission of all panels. */
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) < 15 * 15;
    }

    public void receiveControl(Player player, CompoundTag data) {
        switch (kind()) {
            case GAUGE -> {
                int active = data.getByte("active");
                int polling = data.getByte("polling");
                for (int i = 0; i < 4; i++) {
                    gauges[i].active = (active & (1 << i)) != 0;
                    gauges[i].polling = (polling & (1 << i)) != 0;
                }
                for (int i = 0; i < 4; i++) {
                    GaugeUnit gauge = gauges[i];
                    gauge.color = clamp(data.getInt("color" + i), 0, 0xffffff);
                    gauge.label = data.getString("label" + i);
                    gauge.rtty = data.getString("rtty" + i);
                    gauge.min = data.getInt("min" + i);
                    gauge.max = data.getInt("max" + i);
                }
            }
            case KEY_PAD -> {
                int active = data.getByte("active");
                int polling = data.getByte("polling");
                for (int i = 0; i < 4; i++) {
                    keys[i].active = (active & (1 << i)) != 0;
                    keys[i].polling = (polling & (1 << i)) != 0;
                }
                for (int i = 0; i < 4; i++) {
                    KeyUnit key = keys[i];
                    key.color = clamp(data.getInt("color" + i), 0, 0xffffff);
                    key.label = data.getString("label" + i);
                    key.rtty = data.getString("rtty" + i);
                    key.command = data.getString("cmd" + i);
                }
            }
            case LEVER -> {
                int active = data.getByte("active");
                int polling = data.getByte("polling");
                for (int i = 0; i < 2; i++) {
                    levers[i].active = (active & (1 << i)) != 0;
                    levers[i].polling = (polling & (1 << i)) != 0;
                }
                for (int i = 0; i < 2; i++) {
                    LeverUnit lever = levers[i];
                    lever.label = data.getString("label" + i);
                    lever.rtty = data.getString("rtty" + i);
                    lever.commandOn = data.getString("cmdOn" + i);
                    lever.commandOff = data.getString("cmdOff" + i);
                }
            }
            case INDICATOR -> {
                int active = data.getByte("active");
                int polling = data.getByte("polling");
                for (int i = 0; i < 6; i++) {
                    indicators[i].active = (active & (1 << i)) != 0;
                    indicators[i].polling = (polling & (1 << i)) != 0;
                }
                for (int i = 0; i < 6; i++) {
                    IndicatorUnit indicator = indicators[i];
                    indicator.color = clamp(data.getInt("color" + i), 0, 0xffffff);
                    indicator.label = data.getString("label" + i);
                    indicator.rtty = data.getString("rtty" + i);
                    indicator.min = data.contains("min" + i) ? data.getInt("min" + i) : Integer.MIN_VALUE;
                    indicator.max = data.contains("max" + i) ? data.getInt("max" + i) : Integer.MAX_VALUE;
                }
            }
            case NUMITRON -> {
                int active = data.getByte("active");
                int polling = data.getByte("polling");
                int shorten = data.getByte("shorten_number");
                int leading = data.getByte("leading_zeroes");
                for (int i = 0; i < 2; i++) {
                    displays[i].active = (active & (1 << i)) != 0;
                    displays[i].polling = (polling & (1 << i)) != 0;
                    displays[i].shortenNumber = (shorten & (1 << i)) != 0;
                    displays[i].leadingZeroes = (leading & (1 << i)) != 0;
                }
                for (int i = 0; i < 2; i++) {
                    displays[i].label = data.getString("label" + i);
                    displays[i].rtty = data.getString("rtty" + i);
                }
            }
            case GRAPH -> {
                int active = data.getByte("active");
                int polling = data.getByte("polling");
                for (int i = 0; i < 2; i++) {
                    graphs[i].active = (active & (1 << i)) != 0;
                    graphs[i].polling = (polling & (1 << i)) != 0;
                }
                for (int i = 0; i < 2; i++) {
                    GraphUnit graph = graphs[i];
                    graph.label = data.getString("label" + i);
                    graph.rtty = data.getString("rtty" + i);
                    if (data.contains("min" + i)) {
                        graph.min = data.getLong("min" + i);
                        graph.minBound = true;
                    } else {
                        graph.minBound = false;
                    }
                    if (data.contains("max" + i)) {
                        graph.max = data.getLong("max" + i);
                        graph.maxBound = true;
                    } else {
                        graph.maxBound = false;
                    }
                    if (graph.max < graph.min) {
                        long temp = graph.max;
                        graph.max = graph.min;
                        graph.min = temp;
                    }
                }
            }
            case TERMINAL -> {
                if (data.contains("cmd")) eval(data.getString("cmd"));
            }
            default -> { }
        }
        setChanged();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean channelFresh(HbmRttySystem.Channel chan) {
        return chan != null && level != null && chan.timestamp() >= level.getGameTime() - 1;
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    /* ---------------------------------------------------------------- units */

    public final class GaugeUnit {
        public boolean polling;
        public int color;
        public String label = "";
        public String rtty = "";
        public long min = 0;
        public long max = 100;
        public long value;
        public double renderValue;
        public double lastRenderValue;
        public boolean active;

        GaugeUnit(int initialIndex) {
            if (initialIndex == 0) color = 0x800000;
            if (initialIndex == 1) color = 0x804000;
            if (initialIndex == 2) color = 0x808000;
            if (initialIndex == 3) color = 0x000080;
            label = "Gauge " + (initialIndex + 1);
        }

        void updateClient() {
            lastRenderValue = renderValue;
            double delta = value - renderValue;
            renderValue += delta * 0.1D;
        }

        public void update() {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;
            HbmRttySystem.Channel chan = HbmRttySystem.listen(level, rtty);
            int sigVal = 0;
            if (chan != null && !channelFresh(chan)) chan = null;
            if (chan != null && chan.signal() != null) {
                try { sigVal = Integer.parseInt(chan.signal()); } catch (Exception ignored) { }
                value = sigVal;
            } else if (polling) {
                value = 0;
            }
        }

        void write(CompoundTag tag, int index) {
            tag.putBoolean("active" + index, active);
            tag.putBoolean("polling" + index, polling);
            tag.putInt("color" + index, color);
            tag.putString("label" + index, label);
            tag.putString("rtty" + index, rtty);
            tag.putLong("min" + index, min);
            tag.putLong("max" + index, max);
            tag.putLong("value" + index, value);
        }

        void read(CompoundTag tag, int index) {
            active = tag.getBoolean("active" + index);
            polling = tag.getBoolean("polling" + index);
            color = tag.getInt("color" + index);
            label = tag.getString("label" + index);
            rtty = tag.getString("rtty" + index);
            min = tag.getLong("min" + index);
            max = tag.getLong("max" + index);
            value = tag.getLong("value" + index);
        }
    }

    public final class KeyUnit {
        public boolean polling;
        public boolean isPressed;
        public int color;
        public String label = "";
        public String rtty = "";
        public String command = "";
        public boolean active;
        public int clickTimer;

        KeyUnit(int initialIndex) {
            if (initialIndex == 0) color = 0xff0000;
            if (initialIndex == 1) color = 0xffff00;
            if (initialIndex == 2) color = 0x0080ff;
            if (initialIndex == 3) color = 0x00ff00;
            label = "Button " + (initialIndex + 1);
        }

        public void click() {
            if (!active) return;
            if (!polling) {
                if (canSend()) HbmRttySystem.broadcast(level, rtty, command);
                isPressed = true;
                clickTimer = 7;
            } else {
                isPressed = !isPressed;
                setChanged();
            }
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1F, isPressed ? 1F : 0.75F);
        }

        public void update() {
            if (!active) return;
            if (polling && isPressed && canSend()) HbmRttySystem.broadcast(level, rtty, command);
            if (!polling && isPressed && clickTimer-- <= 0) isPressed = false;
        }

        boolean canSend() {
            return rtty != null && !rtty.isEmpty() && command != null && !command.isEmpty();
        }

        void write(CompoundTag tag, int index) {
            tag.putBoolean("active" + index, active);
            tag.putBoolean("polling" + index, polling);
            tag.putBoolean("isPressed" + index, isPressed);
            tag.putInt("color" + index, color);
            tag.putString("label" + index, label);
            tag.putString("rtty" + index, rtty);
            tag.putString("command" + index, command);
        }

        void read(CompoundTag tag, int index) {
            active = tag.getBoolean("active" + index);
            polling = tag.getBoolean("polling" + index);
            isPressed = tag.getBoolean("isPressed" + index);
            color = tag.getInt("color" + index);
            label = tag.getString("label" + index);
            rtty = tag.getString("rtty" + index);
            command = tag.getString("command" + index);
        }
    }

    public final class LeverUnit {
        public static final float FLIP_SPEED = 1F / 10F;
        public final int index;
        public boolean polling;
        public String label = "";
        public String rtty = "";
        public String commandOn = "";
        public String commandOff = "";
        public boolean active;
        public boolean isTurningOn = false;
        public float flipProgress;
        public float prevFlipProgress;
        public float flipSync;
        public int turnProgress;

        LeverUnit(int initialIndex) {
            index = initialIndex;
            label = "Lever " + (initialIndex + 1);
        }

        public void click() {
            if (!active) return;
            if (flipProgress <= 0 || flipProgress >= 1) playSound(HbmSoundEvents.RBMK_LEVER_START.get(), 1F, 1F);
            isTurningOn = !isTurningOn;
            setChanged();
        }

        public void update() {
            prevFlipProgress = flipProgress;
            if (!active) return;
            boolean arcFlash = false;
            if (polling) {
                if (flipProgress >= 1F && canSend(commandOn)) HbmRttySystem.broadcast(level, rtty, commandOn);
                if (flipProgress <= 0F && canSend(commandOff)) HbmRttySystem.broadcast(level, rtty, commandOff);
            }
            if (isTurningOn && flipProgress < 1F) {
                flipProgress += FLIP_SPEED;
                if (flipProgress >= 1F) {
                    flipProgress = 1F;
                    if (!polling && canSend(commandOn)) HbmRttySystem.broadcast(level, rtty, commandOn);
                    playSound(HbmSoundEvents.RBMK_LEVER_STOP.get(), 0.5F, 1F);
                    arcFlash = true;
                }
            } else if (!isTurningOn && flipProgress > 0F) {
                if (prevFlipProgress >= 1) arcFlash = true;
                flipProgress -= FLIP_SPEED;
                if (flipProgress <= 0F) {
                    flipProgress = 0F;
                    if (!polling && canSend(commandOff)) HbmRttySystem.broadcast(level, rtty, commandOff);
                    playSound(HbmSoundEvents.RBMK_LEVER_STOP.get(), 0.5F, 1F);
                }
            }
            if (arcFlash && level instanceof ServerLevel server) {
                playSound(HbmSoundEvents.RBMK_SPARK.get(), 1F, 1F);
                Direction dir = facing();
                Direction rot = dir.getClockWise(); // ForgeDirection.getRotation(UP)
                for (int i = 0; i < 2; i++) {
                    double x = worldPosition.getX() + 0.5 + dir.getStepX() * 0.4 - rot.getStepX() * (index - 0.5) * (i == 0 ? 0.375 : 0.625);
                    double y = worldPosition.getY() + 0.4375 - 0.03125;
                    double z = worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.4 - rot.getStepZ() * (index - 0.5) * (i == 0 ? 0.375 : 0.625);
                    // AuxParticlePacketNT {type: "tau", count: 5, small: true}, range 25
                    for (int j = 0; j < 5; j++) {
                        sendNear(server, HbmParticleTypes.TAU_SPARK_SMALL.get(), x, y, z,
                                server.random.nextGaussian() * 0.05D, 0.05D, server.random.nextGaussian() * 0.05D);
                    }
                    sendNear(server, HbmParticleTypes.TAU_HADRON_SMALL.get(), x, y, z, 0, 0, 0);
                }
            }
        }

        void updateClient() {
            prevFlipProgress = flipProgress;
            if (turnProgress > 0) {
                flipProgress = flipProgress + ((flipSync - flipProgress) / (float) turnProgress);
                --turnProgress;
            } else {
                flipProgress = flipSync;
            }
        }

        boolean canSend(String command) {
            return rtty != null && !rtty.isEmpty() && command != null && !command.isEmpty();
        }

        void write(CompoundTag tag, int index) {
            tag.putBoolean("active" + index, active);
            tag.putBoolean("polling" + index, polling);
            tag.putBoolean("isTurningOn" + index, isTurningOn);
            tag.putFloat("flipProgress" + index, flipProgress);
            tag.putString("label" + index, label);
            tag.putString("rtty" + index, rtty);
            tag.putString("commandOn" + index, commandOn);
            tag.putString("commandOff" + index, commandOff);
        }

        void read(CompoundTag tag, int index) {
            active = tag.getBoolean("active" + index);
            polling = tag.getBoolean("polling" + index);
            isTurningOn = tag.getBoolean("isTurningOn" + index);
            flipProgress = tag.getFloat("flipProgress" + index);
            label = tag.getString("label" + index);
            rtty = tag.getString("rtty" + index);
            commandOn = tag.getString("commandOn" + index);
            commandOff = tag.getString("commandOff" + index);
        }
    }

    private static void sendNear(ServerLevel server, net.minecraft.core.particles.ParticleOptions particle,
                                 double x, double y, double z, double vx, double vy, double vz) {
        for (net.minecraft.server.level.ServerPlayer player : server.players()) {
            if (player.distanceToSqr(x, y, z) < 25 * 25) {
                server.sendParticles(player, particle, true, x, y, z, 0, vx, vy, vz, 1.0D);
            }
        }
    }

    public final class IndicatorUnit {
        public boolean polling;
        public int color;
        public String label = "";
        public String rtty = "";
        public long min = 0;
        public long max = 100;
        public boolean light;
        public boolean active;

        IndicatorUnit(int initialIndex) {
            color = initialIndex % 2 == 0 ? 0xff0000 : 0xffff00;
            label = "Indicator " + (initialIndex + 1);
        }

        public void update() {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;
            HbmRttySystem.Channel chan = HbmRttySystem.listen(level, rtty);
            int sigVal = 0;
            if (chan != null && !channelFresh(chan)) chan = null;
            if (chan != null && chan.signal() != null) {
                try { sigVal = Integer.parseInt(chan.signal()); } catch (Exception ignored) { }
                decideLight(sigVal);
            } else if (polling) {
                decideLight(0);
            }
        }

        private void decideLight(long value) {
            if (min <= max) {
                light = value >= min && value <= max;
            } else {
                light = value < max || value > min;
            }
        }

        void write(CompoundTag tag, int index) {
            tag.putBoolean("active" + index, active);
            tag.putBoolean("polling" + index, polling);
            tag.putInt("color" + index, color);
            tag.putString("label" + index, label);
            tag.putString("rtty" + index, rtty);
            tag.putLong("min" + index, min);
            tag.putLong("max" + index, max);
            tag.putBoolean("light" + index, light);
        }

        void read(CompoundTag tag, int index) {
            active = tag.getBoolean("active" + index);
            polling = tag.getBoolean("polling" + index);
            color = tag.getInt("color" + index);
            label = tag.getString("label" + index);
            rtty = tag.getString("rtty" + index);
            min = tag.getLong("min" + index);
            max = tag.getLong("max" + index);
            light = tag.getBoolean("light" + index);
        }
    }

    public final class DisplayUnit {
        public boolean polling;
        public String label = "";
        public String rtty = "";
        public long value;
        public boolean active;
        public boolean leadingZeroes;
        public long activeDigits;
        public boolean shortenNumber;

        DisplayUnit(int initialIndex) {
            label = "Display " + (initialIndex + 1);
            activeDigits = 0b01111111;
            shortenNumber = true;
            leadingZeroes = true;
        }

        public void update() {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;
            HbmRttySystem.Channel chan = HbmRttySystem.listen(level, rtty);
            long sigVal = 0;
            if (chan != null && !channelFresh(chan)) chan = null;
            if (chan != null && chan.signal() != null) {
                try { sigVal = Long.parseLong(chan.signal()); } catch (Exception ignored) { }
                value = sigVal;
            } else if (polling) {
                value = 0;
            }
        }

        void write(CompoundTag tag, int index) {
            tag.putBoolean("shorten_number" + index, shortenNumber);
            tag.putLong("active_digits" + index, activeDigits);
            tag.putBoolean("leading_zeroes" + index, leadingZeroes);
            tag.putBoolean("active" + index, active);
            tag.putBoolean("polling" + index, polling);
            tag.putString("label" + index, label);
            tag.putString("rtty" + index, rtty);
            tag.putLong("value" + index, value);
        }

        void read(CompoundTag tag, int index) {
            shortenNumber = tag.getBoolean("shorten_number" + index);
            activeDigits = tag.getLong("active_digits" + index);
            leadingZeroes = tag.getBoolean("leading_zeroes" + index);
            active = tag.getBoolean("active" + index);
            polling = tag.getBoolean("polling" + index);
            label = tag.getString("label" + index);
            rtty = tag.getString("rtty" + index);
            value = tag.getLong("value" + index);
        }
    }

    public final class GraphUnit {
        public boolean polling;
        public String label = "";
        public String rtty = "";
        public final long[] values = new long[30];
        public boolean active;
        public long min;
        public boolean minBound = false;
        public long max;
        public boolean maxBound = false;

        GraphUnit(int initialIndex) {
            label = "Graph " + (initialIndex + 1);
        }

        public void update() {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;
            HbmRttySystem.Channel chan = HbmRttySystem.listen(level, rtty);
            long sigVal = 0;
            if (chan != null && !channelFresh(chan)) chan = null;
            if (chan != null && chan.signal() != null) {
                try { sigVal = Long.parseLong(chan.signal()); } catch (Exception ignored) { }
                pushValue(sigVal);
            } else if (polling) {
                pushValue(0);
            }
        }

        public void pushValue(long value) {
            System.arraycopy(values, 1, values, 0, values.length - 1);
            values[values.length - 1] = value;
        }

        void write(CompoundTag tag, int index) {
            tag.putBoolean("active" + index, active);
            tag.putBoolean("polling" + index, polling);
            tag.putString("label" + index, label);
            tag.putString("rtty" + index, rtty);
            tag.putBoolean("minBound" + index, minBound);
            tag.putLong("min" + index, min);
            tag.putBoolean("maxBound" + index, maxBound);
            tag.putLong("max" + index, max);
            for (int i = 0; i < values.length; i++) tag.putLong("value" + index + "_" + i, values[i]);
        }

        void read(CompoundTag tag, int index) {
            active = tag.getBoolean("active" + index);
            polling = tag.getBoolean("polling" + index);
            label = tag.getString("label" + index);
            rtty = tag.getString("rtty" + index);
            minBound = tag.getBoolean("minBound" + index);
            min = tag.getLong("min" + index);
            maxBound = tag.getBoolean("maxBound" + index);
            max = tag.getLong("max" + index);
            for (int i = 0; i < values.length; i++) values[i] = tag.getLong("value" + index + "_" + i);
        }
    }

    /* ------------------------------------------------------------ terminal */

    public void eval(String cmd) {
        if (cmd == null) return;
        if (ocMode) {
            push(cmd);
            setChanged();
            return;
        }
        push(cmd);
        if (cmd.isEmpty()) return;
        if (cmd.startsWith("chan ")) {
            channel = cmd.substring(5);
            push("Set channel to " + (channel.isEmpty() ? "<none>" : channel));
            return;
        }
        if (cmd.equals("chan")) {
            channel = "";
            push("Set channel to <none>");
            return;
        }
        if (cmd.startsWith("start ")) {
            repeatCmd = cmd.substring(6);
            push("Repeating signal on " + channel);
            return;
        }
        if (cmd.equals("stop")) {
            repeatCmd = "";
            push("Stopping repeat signal");
            return;
        }
        if (cmd.startsWith("send ")) {
            if (channel.isEmpty()) {
                push("Cannot send - no channel set");
                return;
            }
            HbmRttySystem.broadcast(level, channel, cmd.substring(5));
            push("Sent signal on " + channel);
            return;
        }
        if (cmd.equals("horse")) {
            push("Horse.");
            return;
        }
        if (cmd.equals("selfdestruct")) {
            if (level == null) return;
            BlockPos pos = worldPosition;
            level.destroyBlock(pos, false);
            if (level instanceof ServerLevel server) {
                net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                com.atomiclegacy.explosion.LegacyVnt.at(server, center, 5.0F)
                        .entitiesCrossSmooth(1.0D, 50.0F, 5.0F, 0.5F)
                        .sfx(() -> com.atomiclegacy.entity.LegacyProjectileUtil.sendSmallExplosionEffect(server, center, 10, 2.5F, 1.0F))
                        .explode();
            }
            return;
        }
        if (cmd.equals("clear")) {
            for (int i = 0; i < history.length; i++) history[i] = "";
            return;
        }
        push("Unrecognized command!");
    }

    public void push(String msg) {
        for (int i = history.length - 2; i > 0; i--) {
            history[i] = history[i - 1];
        }
        history[0] = msg;
    }

    @Override
    public String[] rttyFunctionInfo() {
        if (kind() != RbmkComponentBlock.Kind.TERMINAL) return super.rttyFunctionInfo();
        String f = RttyInfo.PREFIX_FUNCTION;
        String sep = RttyInteractive.NAME_SEPARATOR;
        return new String[] {f + "clear", f + "write" + sep + "text", f + "set<line#>" + sep + "text", f + "submit" + sep + "command"};
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if (kind() != RbmkComponentBlock.Kind.TERMINAL) return super.runRttyFunction(name, params);
        String f = RttyInfo.PREFIX_FUNCTION;
        if ((f + "clear").equals(name)) {
            for (int i = 0; i < history.length; i++) history[i] = "";
            setChanged();
            return null;
        }
        String allParams = String.join(" ", params);
        if ((f + "write").equals(name)) {
            push(allParams);
            setChanged();
            return null;
        }
        if (name.startsWith(f + "set")) {
            int line = RttyInteractive.parseInt(name.substring(f.length() + 3), 1, 17) - 1;
            history[line] = allParams;
            setChanged();
            return null;
        }
        if ((f + "submit").equals(name)) {
            eval(allParams);
            return null;
        }
        return null;
    }

    @Override
    public boolean rttyInteractive() {
        return kind() == RbmkComponentBlock.Kind.TERMINAL || super.rttyInteractive();
    }

    /* --------------------------------------------------------- persistence */

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(RbmkComponentBlock.FACING) ? state.getValue(RbmkComponentBlock.FACING) : Direction.NORTH;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        switch (kind()) {
            case GAUGE -> { for (int i = 0; i < 4; i++) gauges[i].write(tag, i); }
            case KEY_PAD -> { for (int i = 0; i < 4; i++) keys[i].write(tag, i); }
            case LEVER -> { for (int i = 0; i < 2; i++) levers[i].write(tag, i); }
            case INDICATOR -> { for (int i = 0; i < 6; i++) indicators[i].write(tag, i); }
            case NUMITRON -> { for (int i = 0; i < 2; i++) displays[i].write(tag, i); }
            case GRAPH -> { for (int i = 0; i < 2; i++) graphs[i].write(tag, i); }
            case TERMINAL -> {
                tag.putString("channel", channel.isEmpty() ? " " : channel);
                tag.putString("repeatCmd", repeatCmd.isEmpty() ? " " : repeatCmd);
                tag.putBoolean("ocMode", ocMode);
                for (int i = 0; i < history.length; i++) {
                    tag.putString("history" + i, history[i] == null || history[i].isEmpty() ? " " : history[i]);
                }
            }
            default -> { }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.getBoolean(NET)) {
            readNet(tag);
            return;
        }
        switch (kind()) {
            case GAUGE -> { for (int i = 0; i < 4; i++) gauges[i].read(tag, i); }
            case KEY_PAD -> { for (int i = 0; i < 4; i++) keys[i].read(tag, i); }
            case LEVER -> { for (int i = 0; i < 2; i++) levers[i].read(tag, i); }
            case INDICATOR -> { for (int i = 0; i < 6; i++) indicators[i].read(tag, i); }
            case NUMITRON -> { for (int i = 0; i < 2; i++) displays[i].read(tag, i); }
            case GRAPH -> { for (int i = 0; i < 2; i++) graphs[i].read(tag, i); }
            case TERMINAL -> {
                channel = tag.getString("channel");
                if (channel.equals(" ")) channel = "";
                repeatCmd = tag.getString("repeatCmd");
                if (repeatCmd.equals(" ")) repeatCmd = "";
                ocMode = tag.getBoolean("ocMode");
                for (int i = 0; i < history.length; i++) {
                    history[i] = tag.getString("history" + i);
                    if (history[i].equals(" ")) history[i] = "";
                }
            }
            default -> { }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeNet(tag);
        return tag;
    }

    /** The serialize() halves of the 1.7.10 panels (network state only). */
    private void writeNet(CompoundTag tag) {
        tag.putBoolean(NET, true);
        switch (kind()) {
            case GAUGE -> { for (int i = 0; i < 4; i++) gauges[i].write(tag, i); }
            case KEY_PAD -> { for (int i = 0; i < 4; i++) keys[i].write(tag, i); }
            case LEVER -> {
                for (int i = 0; i < 2; i++) {
                    levers[i].write(tag, i);
                    tag.remove("isTurningOn" + i);
                }
            }
            case INDICATOR -> { for (int i = 0; i < 6; i++) indicators[i].write(tag, i); }
            case NUMITRON -> { for (int i = 0; i < 2; i++) displays[i].write(tag, i); }
            case GRAPH -> {
                for (int i = 0; i < 2; i++) {
                    graphs[i].write(tag, i);
                    if (!graphs[i].active) for (int j = 0; j < graphs[i].values.length; j++) tag.remove("value" + i + "_" + j);
                }
            }
            case TERMINAL -> {
                tag.putBoolean("doesRepeat", !repeatCmd.isEmpty());
                for (int i = 0; i < history.length; i++) tag.putString("history" + i, history[i] == null ? "" : history[i]);
            }
            default -> { }
        }
    }

    private void readNet(CompoundTag tag) {
        switch (kind()) {
            case GAUGE -> {
                for (int i = 0; i < 4; i++) {
                    gauges[i].read(tag, i);
                }
            }
            case KEY_PAD -> { for (int i = 0; i < 4; i++) keys[i].read(tag, i); }
            case LEVER -> {
                for (int i = 0; i < 2; i++) {
                    LeverUnit lever = levers[i];
                    float progress = lever.flipProgress;
                    float prev = lever.prevFlipProgress;
                    lever.read(tag, i);
                    lever.flipSync = tag.getFloat("flipProgress" + i);
                    lever.flipProgress = progress;
                    lever.prevFlipProgress = prev;
                    lever.turnProgress = 3;
                }
            }
            case INDICATOR -> { for (int i = 0; i < 6; i++) indicators[i].read(tag, i); }
            case NUMITRON -> { for (int i = 0; i < 2; i++) displays[i].read(tag, i); }
            case GRAPH -> {
                for (int i = 0; i < 2; i++) {
                    GraphUnit graph = graphs[i];
                    graph.active = tag.getBoolean("active" + i);
                    graph.polling = tag.getBoolean("polling" + i);
                    graph.label = tag.getString("label" + i);
                    graph.rtty = tag.getString("rtty" + i);
                    graph.minBound = tag.getBoolean("minBound" + i);
                    if (graph.minBound) graph.min = tag.getLong("min" + i);
                    graph.maxBound = tag.getBoolean("maxBound" + i);
                    if (graph.maxBound) graph.max = tag.getLong("max" + i);
                    if (graph.active) for (int j = 0; j < graph.values.length; j++) graph.values[j] = tag.getLong("value" + i + "_" + j);
                }
            }
            case TERMINAL -> {
                doesRepeat = tag.getBoolean("doesRepeat");
                for (int i = 0; i < history.length; i++) history[i] = tag.getString("history" + i);
            }
            default -> { }
        }
    }
}
