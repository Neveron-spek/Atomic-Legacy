package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.RadioTorchBlock;
import com.atomiclegacy.inventory.container.RttyConfigMenu;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.rtty.HbmRttySystem;
import com.atomiclegacy.rtty.RttyFunctionException;
import com.atomiclegacy.rtty.RttyInfo;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.rtty.RttyValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Server-side state and ticking logic for the 1.7.10 redstone-over-radio blocks. */
public final class RadioTorchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CHANNEL_LIMIT = 15;
    public static final int VALUE_NAME_LIMIT = 25;
    private final RadioTorchBlock.Kind kind;
    private String channel = "";
    private int lastState;
    private long lastUpdate = -1L;
    private boolean polling;
    private boolean customMap;
    private boolean descending;
    private final String[] mapping = new String[16];
    private final int[] conditions = new int[16];
    private final String[] channels = new String[8];
    private final String[] names = new String[8];
    private final String[] previous = new String[8];
    private final boolean[] forceUpdate = new boolean[8];
    private final ItemStack[] filters = new ItemStack[3];
    private final String[] filterModes = new String[3]; // [parity R2] ModulePatternMatcher(3)
    private final int[] lastCount = new int[3];
    private String previousCommand = "";
    private boolean receiving;
    private boolean firstTick = true;

    public RadioTorchBlockEntity(BlockPos pos, BlockState state, RadioTorchBlock.Kind kind) {
        super(HbmBlockEntities.RADIO_TORCH.get(), pos, state);
        this.kind = kind;
        for (int i = 0; i < mapping.length; i++) mapping[i] = "";
        for (int i = 0; i < channels.length; i++) {
            channels[i] = "";
            names[i] = "";
            previous[i] = "";
            forceUpdate[i] = true;
        }
        for (int i = 0; i < filters.length; i++) filters[i] = ItemStack.EMPTY;
    }

    public RadioTorchBlock.Kind kind() {
        return kind;
    }

    public int signal() {
        return Math.max(0, Math.min(15, lastState));
    }

    public String channel() {
        return channel;
    }

    public void setChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim();
        this.channel = normalized.length() > CHANNEL_LIMIT ? normalized.substring(0, CHANNEL_LIMIT) : normalized;
        setChanged();
    }

    public boolean polling() {
        return polling;
    }

    public void setPolling(boolean polling) {
        if (this.polling == polling) return;
        this.polling = polling;
        setChanged();
        sync();
    }

    /** [parity R2] counter overlay: Freq n / Signal n. */
    public String counterChannel(int index) { return channels[index]; }
    public int counterLastCount(int index) { return lastCount[index]; }

    public String readerChannel(int index) {
        return index >= 0 && index < channels.length ? channels[index] : "";
    }

    public String readerName(int index) {
        return index >= 0 && index < names.length ? names[index] : "";
    }

    public void setReaderEntry(int index, String channel, String name) {
        if (index < 0 || index >= channels.length) return;
        String normalizedChannel = limit(channel, CHANNEL_LIMIT);
        String normalizedName = limit(name, VALUE_NAME_LIMIT);
        if (!normalizedChannel.equals(channels[index]) || !normalizedName.equals(names[index])) {
            channels[index] = normalizedChannel;
            names[index] = normalizedName;
            forceUpdate[index] = true;
            setChanged();
            sync();
        }
    }

    /** [parity g2] GUIScreenRadioTorchReader/Controller show the info tooltip only for an IRORValueProvider / IRORInfo target. */
    public boolean hasRttyInfo(boolean valueProvider) {
        BlockEntity target = resolveTarget();
        return valueProvider ? target instanceof com.atomiclegacy.rtty.RttyValueProvider : target instanceof RttyInfo;
    }

    public String[] supportedFunctions() {
        BlockEntity target = resolveTarget();
        return target instanceof RttyInfo info ? info.rttyFunctionInfo().clone() : new String[0];
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(switch (kind) {
            case READER -> "container.atomiclegacy.rtty_reader";
            case CONTROLLER -> "container.atomiclegacy.rtty_controller";
            default -> "block.atomiclegacy.radio_torch_" + kind.name().toLowerCase(Locale.ROOT);
        });
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // [parity R2] every RTTY torch except the relay opens its 1.7.10 GUI
        return kind == RadioTorchBlock.Kind.RELAY ? null : new RttyConfigMenu(containerId, inventory, this);
    }

    // ---- [parity R2] client data for the sender/receiver/logic/counter screens ----
    public boolean customMap() { return customMap; }
    public boolean descending() { return descending; }
    public String mapping(int index) { return mapping[index]; }
    public int condition(int index) { return conditions[index]; }
    public ItemStack counterFilter(int index) { return filters[index]; }
    @Nullable public String counterMode(int index) { return filterModes[index]; }

    /** [parity R2] TileEntityRadioTorchBase/Logic/Counter.receiveControl (NBTControlPacket keys). */
    public void receiveControl(Player player, CompoundTag data) {
        if (level == null || player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) >= 256.0D) return;
        switch (kind) {
            case SENDER, RECEIVER -> {
                if (data.contains("p")) polling = data.getBoolean("p");
                if (data.contains("m")) customMap = data.getBoolean("m");
                if (data.contains("c")) channel = data.getString("c");
                for (int i = 0; i < 16; i++) if (data.contains("m" + i)) mapping[i] = data.getString("m" + i);
            }
            case LOGIC -> {
                if (data.contains("p")) polling = data.getBoolean("p");
                if (data.contains("c")) channel = data.getString("c");
                if (data.contains("d")) descending = data.getBoolean("d");
                for (int i = 0; i < 16; i++) if (data.contains("m" + i)) mapping[i] = data.getString("m" + i);
                for (int i = 0; i < 16; i++) if (data.contains("c" + i)) conditions[i] = data.getInt("c" + i);
            }
            case COUNTER -> {
                if (data.contains("polling")) {
                    polling = !polling;
                } else {
                    for (int i = 0; i < 3; i++) {
                        String value = data.getString("c" + i);
                        if (!value.equals(channels[i])) {
                            channels[i] = value;
                            forceUpdate[i] = true;
                        }
                    }
                }
            }
            default -> {
                return;
            }
        }
        setChanged();
        sync();
    }

    /** [parity R2] ContainerCounterTorch.slotClick: ghost pattern slots, right click cycles the match mode. */
    public void counterPatternClick(int index, ItemStack held, boolean rightClick) {
        if (index < 0 || index > 2) return;
        if (rightClick && !filters[index].isEmpty()) {
            cycleCounterMode(index);
        } else {
            filters[index] = held.isEmpty() ? ItemStack.EMPTY : held.copy();
            filterModes[index] = filters[index].isEmpty() ? null : com.atomiclegacy.drone.DroneItemMatcher.defaultMode(filters[index]);
            forceUpdate[index] = true;
        }
        setChanged();
        sync();
    }

    private void cycleCounterMode(int index) {
        ItemStack pattern = filters[index];
        String mode = filterModes[index];
        java.util.List<net.minecraft.resources.ResourceLocation> tags = pattern.getTags().map(net.minecraft.tags.TagKey::location).toList();
        if (mode == null) {
            filterModes[index] = com.atomiclegacy.drone.DroneItemMatcher.EXACT;
        } else if (com.atomiclegacy.drone.DroneItemMatcher.EXACT.equals(mode)) {
            filterModes[index] = com.atomiclegacy.drone.DroneItemMatcher.WILDCARD;
        } else if (com.atomiclegacy.drone.DroneItemMatcher.WILDCARD.equals(mode)) {
            filterModes[index] = tags.isEmpty() ? com.atomiclegacy.drone.DroneItemMatcher.EXACT : tags.getFirst().toString();
        } else {
            int at = tags.indexOf(net.minecraft.resources.ResourceLocation.tryParse(mode));
            filterModes[index] = at < 0 || at + 1 >= tags.size() ? com.atomiclegacy.drone.DroneItemMatcher.EXACT : tags.get(at + 1).toString();
        }
        forceUpdate[index] = true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchBlockEntity entity) {
        if (level.isClientSide) return;
        switch (entity.kind) {
            case SENDER -> entity.tickSender(level, pos, state);
            case RECEIVER -> entity.tickReceiver(level, pos, state, false);
            case LOGIC -> entity.tickReceiver(level, pos, state, true);
            case READER -> entity.tickReader(level, pos, state);
            case COUNTER -> entity.tickCounter(level, pos, state);
            case CONTROLLER -> entity.tickController(level, pos, state);
            case RELAY -> { } // [parity R2] TileEntityRadioTorchRelay.updateEntity: //TODO
        }
    }

    /** [parity R2] TileEntityRadioTorchSender.updateEntity. */
    private void tickSender(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(RadioTorchBlock.FACING);
        BlockPos support = senderSupport(pos, facing);
        BlockState inputState = level.getBlockState(support);
        // getIndirectPowerLevelTo(support, meta): the side argument is the torch metadata (its facing)
        int input = level.getSignal(support, facing);
        if (inputState.hasAnalogOutputSignal()) {
            input = inputState.getAnalogOutputSignal(level, support);
        }
        boolean shouldSend = polling;
        if (input != lastState) {
            setChanged();
            lastState = input;
            updatePoweredState(level, pos, state, input > 0);
            sync();
            shouldSend = true;
        }
        if (shouldSend && !channel.isEmpty()) {
            String toSend = customMap ? mapping[Math.max(0, Math.min(15, input))] : Integer.toString(input);
            if (toSend != null && !toSend.isEmpty()) HbmRttySystem.broadcast(level, channel, toSend);
        }
    }

    /** [parity R2] TileEntityRadioTorchReceiver / TileEntityRadioTorchLogic.updateEntity. */
    private void tickReceiver(Level level, BlockPos pos, BlockState state, boolean logic) {
        if (channel.isEmpty() && !polling) return;
        HbmRttySystem.Channel chan = channel.isEmpty() ? null : HbmRttySystem.listen(level, channel);
        if (chan != null && (polling || (chan.timestamp() > lastUpdate - 1 && chan.timestamp() != -1))) {
            String msg = chan.signal();
            lastUpdate = level.getGameTime();
            int nextState = 0;
            if (logic) {
                if (chan.timestamp() < lastUpdate - 1 && polling) msg = "0";
                if (descending) {
                    for (int i = 15; i >= 0; i--) if (!mapping[i].isEmpty() && compare(msg, i)) { nextState = i; break; }
                } else {
                    for (int i = 0; i <= 15; i++) if (!mapping[i].isEmpty() && compare(msg, i)) { nextState = i; break; }
                }
            } else {
                if ("selfdestruct".equals(msg)) {
                    selfDestruct(level, pos);
                    return;
                }
                if (customMap) {
                    for (int i = 15; i >= 0; i--) if (msg.equals(mapping[i])) { nextState = i; break; }
                } else {
                    int sig = 0;
                    try { sig = Integer.parseInt(msg); } catch (Exception ignored) { }
                    nextState = Math.max(0, Math.min(15, sig));
                }
                if (chan.timestamp() < lastUpdate - 1 && polling) nextState = 0;
            }
            setReceivedState(level, pos, state, nextState);
        } else if (polling && lastState != 0) {
            setReceivedState(level, pos, state, 0);
        }
    }

    private void setReceivedState(Level level, BlockPos pos, BlockState state, int nextState) {
        if (lastState == nextState) return;
        lastState = nextState;
        updatePoweredState(level, pos, state, nextState > 0);
        level.updateNeighborsAt(pos, state.getBlock());
        sync();
        setChanged();
    }

    /** Receiver/controller "selfdestruct": func_147480_a(no drop) + ExplosionVNT(5) CrossSmooth(1, 50) piercing(5, 0.5), weapon SFX(10, 2.5, 1). */
    private static void selfDestruct(Level level, BlockPos pos) {
        level.destroyBlock(pos, false);
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        com.atomiclegacy.explosion.LegacyVnt.at(server, center, 5.0F)
                .entitiesCrossSmooth(1.0D, 50.0F, 5.0F, 0.5F)
                .sfx(() -> com.atomiclegacy.entity.LegacyProjectileUtil.sendSmallExplosionEffect(server, center, 10, 2.5F, 1.0F))
                .explode();
    }

    private boolean compare(String message, int index) {
        int op = conditions[index];
        if (op <= 5) {
            long signal;
            long mapped;
            try {
                signal = Long.parseLong(message);
                mapped = Long.parseLong(mapping[index]);
            } catch (Exception ignored) {
                return false;
            }
            return switch (op) {
                case 1 -> signal <= mapped;
                case 2 -> signal >= mapped;
                case 3 -> signal > mapped;
                case 4 -> signal == mapped;
                case 5 -> signal != mapped;
                default -> signal < mapped;
            };
        }
        return switch (op) {
            case 7 -> !message.equals(mapping[index]);
            case 8 -> message.contains(mapping[index]);
            case 9 -> !message.contains(mapping[index]);
            default -> message.equals(mapping[index]);
        };
    }

    /** [parity R2] TileEntityRadioTorchReader.updateEntity: only IRORValueProvider targets are read. */
    private void tickReader(Level level, BlockPos pos, BlockState state) {
        BlockPos support = readerSupport(pos, state.getValue(RadioTorchBlock.FACING));
        BlockEntity target = resolveTarget(level, support);
        if (!(target instanceof RttyValueProvider provider)) return;
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].isEmpty() || names[i].isEmpty()) continue;
            String value = provider.provideRttyValue(RttyInfo.PREFIX_VALUE + names[i].toLowerCase(Locale.US));
            if (value == null) continue;
            if (polling || forceUpdate[i] || !value.equals(previous[i])) {
                HbmRttySystem.broadcast(level, channels[i], value);
                previous[i] = value;
                forceUpdate[i] = false;
            }
        }
        setChanged();
    }

    /** [parity R2] TileEntityRadioTorchCounter.updateEntity with ModulePatternMatcher filters. */
    private void tickCounter(Level level, BlockPos pos, BlockState state) {
        BlockEntity adjacent = level.getBlockEntity(counterSupport(pos, state.getValue(RadioTorchBlock.FACING)));
        if (!(adjacent instanceof Container container)) return;
        boolean changed = false;
        for (int i = 0; i < filters.length; i++) {
            if (channels[i].isEmpty() || filters[i].isEmpty()) continue;
            com.atomiclegacy.drone.DroneItemMatcher matcher = new com.atomiclegacy.drone.DroneItemMatcher(filters[i], filterModes[i]);
            int count = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty() && matcher.matches(stack)) count += stack.getCount();
            }
            if (polling || forceUpdate[i] || lastCount[i] != count) {
                HbmRttySystem.broadcast(level, channels[i], count);
                forceUpdate[i] = false;
            }
            if (lastCount[i] != count) changed = true;
            lastCount[i] = count;
        }
        if (changed) {
            setChanged();
            sync();
        }
    }

    /** [parity R2] TileEntityRadioTorchController.updateEntity. */
    private void tickController(Level level, BlockPos pos, BlockState state) {
        boolean nowReceiving = false;
        if (!channel.isEmpty()) {
            BlockPos supportPos = readerSupport(pos, state.getValue(RadioTorchBlock.FACING));
            BlockEntity target = resolveTarget(level, supportPos);
            // [parity T2: ROR] TileEntityProxyCombo always implemented IRORInteractive; RBMK columns share one class.
            if (target instanceof RttyInteractive interactive
                    && (interactive.rttyInteractive() || level.getBlockEntity(supportPos) instanceof MachineDummyBlockEntity)) {
                HbmRttySystem.Channel received = HbmRttySystem.listen(level, channel);
                if (received != null) {
                    String command = received.signal();
                    if ("selfdestruct".equals(command)) {
                        selfDestruct(level, pos);
                        return;
                    }
                    if (received.timestamp() >= level.getGameTime() - 1L && received.timestamp() != -1L) {
                        nowReceiving = true;
                        boolean changed = !command.equals(previousCommand);
                        if (polling || !receiving || changed) {
                            try {
                                if (!command.isEmpty()) interactive.runRttyFunction(
                                        RttyInfo.PREFIX_FUNCTION + RttyInteractive.command(command),
                                        RttyInteractive.params(command));
                            } catch (RttyFunctionException ignored) {
                                // Malformed radio packets are ignored exactly like 1.7.10.
                            }
                        }
                        if (changed) {
                            previousCommand = command;
                            setChanged();
                        }
                    }
                }
            }
        }
        // The published RTTY queue is empty immediately after loading. Retain
        // the saved burst state for one tick so an edge is not fabricated.
        if (!firstTick || nowReceiving) {
            if (receiving != nowReceiving) {
                receiving = nowReceiving;
                setChanged();
            }
        }
        firstTick = false;
    }
    @Nullable
    private BlockEntity resolveTarget() {
        if (level == null) return null;
        return resolveTarget(level, readerSupport(worldPosition, getBlockState().getValue(RadioTorchBlock.FACING)));
    }

    @Nullable
    private static BlockEntity resolveTarget(Level level, BlockPos position) {
        BlockEntity target = level.getBlockEntity(position);
        if (target instanceof MachineDummyBlockEntity dummy) {
            BlockEntity core = dummy.core();
            // [parity T2: ROR] RBMK columns only had a proxy tile on their top block.
            // [parity U2: ROR] other multiblocks: only their 1.7.10 TileEntityProxyCombo dummies
            if (core != null && !com.atomiclegacy.rtty.RttyProxyLayouts.isProxy(core, position)) return null;
            return core;
        }
        return target;
    }

    private static String limit(String value, int maximumLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > maximumLength ? normalized.substring(0, maximumLength) : normalized;
    }

    private static BlockPos senderSupport(BlockPos pos, Direction facing) {
        return switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
    }

    private static BlockPos readerSupport(BlockPos pos, Direction facing) {
        return switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
    }

    private static BlockPos counterSupport(BlockPos pos, Direction facing) {
        return switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
    }

    private void updatePoweredState(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(RadioTorchBlock.POWERED) && state.getValue(RadioTorchBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(RadioTorchBlock.POWERED, powered), Block.UPDATE_ALL);
        }
    }

    private void sync() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Channel", channel);
        tag.putInt("LastState", lastState);
        tag.putLong("LastUpdate", lastUpdate);
        tag.putBoolean("Polling", polling);
        tag.putBoolean("CustomMap", customMap);
        tag.putBoolean("Descending", descending);
        tag.putString("PreviousCommand", previousCommand);
        tag.putBoolean("Receiving", receiving);
        for (int i = 0; i < mapping.length; i++) {
            if (!mapping[i].isEmpty()) tag.putString("Map" + i, mapping[i]);
            if (conditions[i] != 0) tag.putInt("Condition" + i, conditions[i]);
        }
        for (int i = 0; i < channels.length; i++) {
            tag.putString("Channel" + i, channels[i]);
            tag.putString("Name" + i, names[i]);
            tag.putString("Previous" + i, previous[i]);
        }
        for (int i = 0; i < filters.length; i++) if (!filters[i].isEmpty()) {
            CompoundTag filter = new CompoundTag();
            filters[i].save(registries, filter);
            tag.put("Filter" + i, filter);
            tag.putInt("Count" + i, lastCount[i]);
            if (filterModes[i] != null) tag.putString("FilterMode" + i, filterModes[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channel = tag.getString("Channel");
        lastState = Math.max(0, Math.min(15, tag.getInt("LastState")));
        lastUpdate = tag.getLong("LastUpdate");
        polling = tag.getBoolean("Polling");
        customMap = tag.getBoolean("CustomMap");
        descending = tag.getBoolean("Descending");
        previousCommand = tag.getString("PreviousCommand");
        receiving = tag.getBoolean("Receiving");
        firstTick = true;
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = tag.getString("Map" + i);
            conditions[i] = tag.getInt("Condition" + i);
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i] = tag.getString("Channel" + i);
            names[i] = tag.getString("Name" + i);
            previous[i] = tag.getString("Previous" + i);
            forceUpdate[i] = true;
        }
        for (int i = 0; i < filters.length; i++) {
            if (tag.contains("Filter" + i)) filters[i] = ItemStack.parse(registries, tag.getCompound("Filter" + i)).orElse(ItemStack.EMPTY);
            lastCount[i] = tag.getInt("Count" + i);
            filters[i] = tag.contains("Filter" + i) ? filters[i] : ItemStack.EMPTY;
            filterModes[i] = tag.contains("FilterMode" + i) ? tag.getString("FilterMode" + i) : null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
