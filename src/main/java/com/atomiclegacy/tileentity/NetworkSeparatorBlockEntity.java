package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.NetworkSeparatorBlock;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.power.PowerSplitEndpoints;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.rtty.RttyValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * [parity U1: network_separator] BlockNetworkSeparator.TileEntityNetworkSeparator (936c60c): a power buffer whose
 * in port (core + rot(UP), receiver only) and out port (core + rot(DOWN), provider only) are separate nodes, so the
 * two power nets never merge. The buffer size follows the configured limit, lowered to the demand of the outgoing
 * net (separators there count with their own limit) but never below the stored power.
 */
public class NetworkSeparatorBlockEntity extends BlockEntity implements PowerSplitEndpoints, RttyValueProvider, RttyInteractive {
    public static final long MAX_LIMIT = 10_000_000_000L;

    /** Inter-tick tracker for transferred power */
    protected long deltaTracker = 0;
    /** Final power per tick over the past second */
    public final long[] deltaTick = new long[20];
    /** Average of the entire previous log */
    public long deltaSecond = 0;

    private long power;
    public PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.NORMAL;
    public boolean redstone = false;
    public long configuredLimit = 1_000;
    public long actualLimit = configuredLimit;

    private final InPort inPort = new InPort();
    private final OutPort outPort = new OutPort();

    public NetworkSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.NETWORK_SEPARATOR.get(), pos, state);
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(NetworkSeparatorBlock.FACING) ? state.getValue(NetworkSeparatorBlock.FACING) : Direction.NORTH;
    }

    /** getDir(false): ForgeDirection.getRotation(DOWN) of the BlockDummyable direction - the out port side */
    public Direction outDirection() {
        return facing().getCounterClockWise();
    }

    public Direction inDirection() {
        return outDirection().getOpposite();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NetworkSeparatorBlockEntity separator) {
        separator.tickServer(level);
    }

    private void tickServer(Level level) {
        PowerNetworkManager.tickFromEndpoint(level, this.inPort);
        PowerNetworkManager.tickFromEndpoint(level, this.outPort);

        long oldLimit = this.actualLimit;
        this.actualLimit = this.configuredLimit;
        surveyOutgoingNetwork(level);
        this.actualLimit = Math.max(this.actualLimit, this.power);

        long total = this.deltaTick[0];
        for (int i = 1; i < this.deltaTick.length; i++) {
            total += this.deltaTick[i];
            this.deltaTick[i - 1] = this.deltaTick[i];
        }
        long oldTick = this.deltaTick[19];
        long oldSecond = this.deltaSecond;
        this.deltaTick[19] = this.deltaTracker;
        this.deltaTracker = 0;
        this.deltaSecond = total;

        Direction dir = outDirection();
        boolean oldRedstone = this.redstone;
        this.redstone = level.hasNeighborSignal(this.worldPosition)
                || level.hasNeighborSignal(this.worldPosition.relative(dir))
                || level.hasNeighborSignal(this.worldPosition.relative(dir.getOpposite()));

        // networkPackNT(15): priority, redstone, configuredLimit, deltaTick[19], deltaSecond
        if (oldRedstone != this.redstone || oldTick != this.deltaTick[19] || oldSecond != this.deltaSecond
                || oldLimit != this.actualLimit || level.getGameTime() % 20 == 0) {
            sync();
        } else {
            setChanged();
        }
    }

    /** surveyOutgoingNetwork(PowerNetMK2) over the receivers of the out port's net */
    protected void surveyOutgoingNetwork(Level level) {
        List<PowerEndpoint> endpoints = PowerNetworkManager.componentEndpoints(level, this.outPort.getPowerPos());
        if (endpoints.isEmpty()) {
            return;
        }
        long demand = 0;
        for (PowerEndpoint endpoint : endpoints) {
            if (endpoint == this.outPort) continue;
            if (endpoint instanceof InPort other) {
                demand += other.owner().actualLimit;
            } else {
                demand += Math.max(0L, endpoint.getRequestedInput());
            }
        }
        if (demand < this.actualLimit) this.actualLimit = demand;
    }

    public void usePower(long amount) {
        this.power -= amount;
        this.deltaTracker += amount;
    }

    public long getReceiverSpeed() { return redstone || bufferMismatch() ? 0 : this.getMaxPower(); }
    public long getProviderSpeed() { return redstone ? 0 : this.getMaxPower(); }
    public long getMaxPower() { return actualLimit; }
    public long getPower() { return power; }

    public boolean bufferMismatch() {
        return this.configuredLimit < this.getMaxPower();
    }

    @Override
    @Nullable
    public PowerEndpoint splitPowerEndpointAt(BlockPos pos) {
        if (pos.equals(this.inPort.getPowerPos())) return this.inPort;
        if (pos.equals(this.outPort.getPowerPos())) return this.outPort;
        return null;
    }

    public PowerEndpoint inPort() {
        return inPort;
    }

    public PowerEndpoint outPort() {
        return outPort;
    }

    /** hasPermission: distance squared at most 128 */
    public void receiveControl(Player player, CompoundTag data) {
        if (player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) > 128) {
            return;
        }
        if (data.contains("limit")) this.configuredLimit = data.getLong("limit");
        if (data.contains("priority")) this.priority = byOrdinal(data.getByte("priority"));
        if (configuredLimit < 0) configuredLimit = 0;
        if (configuredLimit > MAX_LIMIT) configuredLimit = MAX_LIMIT;
        sync();
    }

    /** EnumUtil.grabEnumSafely */
    public static PowerEndpoint.ConnectionPriority byOrdinal(int ordinal) {
        PowerEndpoint.ConnectionPriority[] values = PowerEndpoint.ConnectionPriority.VALUES;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
    }

    // IRORValueProvider / IRORInteractive

    @Override
    public String[] rttyFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "deltatick",
                PREFIX_VALUE + "deltasecond",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
                PREFIX_FUNCTION + "setlimit" + NAME_SEPARATOR + "limit",
        };
    }

    @Override
    public String provideRttyValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick[19];
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaSecond;
        return null;
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            int value = RttyInteractive.parseInt(params[0], 0, 2) + 1;
            this.priority = byOrdinal(value);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setlimit").equals(name) && params.length > 0) {
            this.configuredLimit = RttyInteractive.parseLong(params[0], 0, MAX_LIMIT);
            setChanged();
            return null;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("configuredLimit", configuredLimit);
        tag.putLong("actualLimit", actualLimit);
        tag.putByte("p", (byte) this.priority.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = tag.getLong("power");
        this.configuredLimit = tag.getLong("configuredLimit");
        this.actualLimit = tag.getLong("actualLimit");
        this.priority = byOrdinal(tag.getByte("p"));
        if (tag.contains("redstone")) this.redstone = tag.getBoolean("redstone");
        if (tag.contains("deltaTick")) this.deltaTick[19] = tag.getLong("deltaTick");
        if (tag.contains("deltaSecond")) this.deltaSecond = tag.getLong("deltaSecond");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putBoolean("redstone", redstone);
        tag.putLong("deltaTick", deltaTick[19]);
        tag.putLong("deltaSecond", deltaSecond);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** ModulePortManPower#onlyInput: IEnergyReceiverMK2 side of the separator */
    public final class InPort implements PowerEndpoint {
        NetworkSeparatorBlockEntity owner() {
            return NetworkSeparatorBlockEntity.this;
        }

        @Override public BlockPos getPowerPos() { return worldPosition.relative(inDirection()); }
        @Override public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) { return List.of(getPowerPos().relative(inDirection())); }
        @Override public long getAvailableOutput() { return 0L; }
        @Override public long getRequestedInput() { return Math.max(0L, Math.min(getMaxPower() - power, getReceiverSpeed())); }
        @Override public ConnectionPriority getPowerPriority() { return priority; }
        @Override public void applyPower(long usedOutput, long receivedInput) {
            if (receivedInput > 0) {
                power += receivedInput;
                setChanged();
            }
        }
        @Override public Component getPowerStatus() { return Component.literal(power + " HE"); }
    }

    /** ModulePortManPower#onlyOutput: IEnergyProviderMK2 side of the separator */
    public final class OutPort implements PowerEndpoint {
        @Override public BlockPos getPowerPos() { return worldPosition.relative(outDirection()); }
        @Override public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) { return List.of(getPowerPos().relative(outDirection())); }
        @Override public long getAvailableOutput() { return Math.max(0L, Math.min(power, getProviderSpeed())); }
        @Override public long getRequestedInput() { return 0L; }
        @Override public void applyPower(long usedOutput, long receivedInput) {
            if (usedOutput > 0) {
                usePower(usedOutput);
                setChanged();
            }
        }
        @Override public Component getPowerStatus() { return Component.literal(power + " HE"); }
    }
}
