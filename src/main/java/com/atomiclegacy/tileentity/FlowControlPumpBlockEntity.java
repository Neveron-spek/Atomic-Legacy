package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.FlowControlPumpBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetUser;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * [parity T1: flow_control_pump] FlowControlPump.TileEntityFlowControlPump (HbmMods 936c60c): a 100 mB
 * (0..10,000) buffer that receives on the in port (core + rot(UP)) and provides on the out port
 * (core + rot(DOWN)) with a configurable pressure and priority; redstone on any of the three blocks
 * stops it.
 */
public class FlowControlPumpBlockEntity extends BlockEntity implements HbmFluidNetUser {
    public static final float ROTOR_ACCELERATION = 1F;

    public int bufferSize = 100;
    public final HbmFluidTank tank = new HbmFluidTank(HbmFluids.none(), bufferSize);
    public PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.NORMAL;
    public boolean redstone = false;
    public boolean topRail = false;
    public boolean bottomRail = true;
    public boolean isWorking = false;
    private boolean syncedWorking = false;
    public float rotor;
    public float prevRotor;
    public float rotorSpeed;

    public FlowControlPumpBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FLOW_CONTROL_PUMP.get(), pos, state);
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(FlowControlPumpBlock.FACING) ? state.getValue(FlowControlPumpBlock.FACING) : Direction.NORTH;
    }

    /** ForgeDirection#getRotation(DOWN) of the BlockDummyable direction: the out port side */
    public Direction outDirection() {
        return facing().getCounterClockWise();
    }

    /** ForgeDirection#getRotation(UP): the in port side */
    public Direction inDirection() {
        return facing().getClockWise();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FlowControlPumpBlockEntity pump) {
        // if the capacity were changed directly, any excess buffered fluid would be destroyed
        if (pump.bufferMismatch()) {
            int nextBuffer = Math.max(pump.tank.amount(), pump.bufferSize);
            pump.tank.setCapacity(nextBuffer);
        }
        Direction dir = pump.outDirection();
        boolean redstone = level.hasNeighborSignal(pos)
                || level.hasNeighborSignal(pos.relative(dir))
                || level.hasNeighborSignal(pos.relative(dir.getOpposite()));
        boolean changed = redstone != pump.redstone || pump.isWorking != pump.syncedWorking;
        pump.redstone = redstone;
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, pump); // [parity U1: fluid net]
        if (changed) {
            pump.syncedWorking = pump.isWorking;
            pump.sync();
        }
        pump.isWorking = false;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FlowControlPumpBlockEntity pump) {
        if (level.getGameTime() % 20 == 0) {
            pump.topRail = !level.getBlockState(pos.above()).isAir();
            pump.bottomRail = !level.getBlockState(pos.below()).isAir();
        }
        if (pump.isWorking) pump.rotorSpeed += ROTOR_ACCELERATION;
        else pump.rotorSpeed -= ROTOR_ACCELERATION;
        pump.rotorSpeed = Mth.clamp(pump.rotorSpeed, 0F, 30);
        pump.prevRotor = pump.rotor;
        pump.rotor += pump.rotorSpeed;
        if (pump.rotor >= 360F) {
            pump.rotor -= 360F;
            pump.prevRotor -= 360F;
        }
    }

    public boolean bufferMismatch() {
        return this.bufferSize != this.tank.capacity();
    }

    public void setType(HbmFluidDefinition type) {
        tank.setType(type);
        sync();
    }

    /// IFluidStandardTransceiverMK2 ///

    @Override public HbmFluidTank[] fluidSendingTanks() { return new HbmFluidTank[]{tank}; }
    @Override public HbmFluidTank[] fluidReceivingTanks() { return new HbmFluidTank[]{tank}; }
    @Override public PowerEndpoint.ConnectionPriority fluidPriority() { return priority; }
    @Override public long fluidReceiverSpeed(HbmFluidDefinition type, int pressure) { return redstone || bufferMismatch() ? 0 : DEFAULT_SPEED; }
    @Override public long fluidProviderSpeed(HbmFluidDefinition type, int pressure) { return redstone ? 0 : DEFAULT_SPEED; }

    @Override
    public void useUpFluid(HbmFluidDefinition type, int pressure, long amount) {
        if (amount > 0) {
            if (tank.type() == type && tank.pressure() == pressure) {
                int toRem = (int) Math.min(amount, tank.amount());
                tank.setAmount(tank.amount() - toRem);
                this.isWorking = true;
            }
        }
    }

    /** NBTControlPacket keys of GUIPump */
    public void receiveControl(Player player, CompoundTag data) {
        if (player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) > 128) {
            return;
        }
        if (data.contains("capacity")) this.bufferSize = Mth.clamp(data.getInt("capacity"), 0, 10_000);
        if (data.contains("pressure")) this.tank.setPressure(Mth.clamp(data.getByte("pressure"), 0, 5));
        if (data.contains("priority")) this.priority = byOrdinal(data.getByte("priority"));
        sync();
    }

    public static PowerEndpoint.ConnectionPriority byOrdinal(int ordinal) {
        PowerEndpoint.ConnectionPriority[] values = PowerEndpoint.ConnectionPriority.VALUES;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
    }

    /**
     * Port node shape: the in port is the dummy at core + rot(UP) connecting outward, the out port is the
     * dummy at core + rot(DOWN). Other positions/sides have no connection. Direct (pipe-less) neighbours
     * use this handler; pipe nets use the MK2 interface above.
     */
    @Nullable
    public IFluidHandler fluidHandler(BlockPos accessorPos, @Nullable Direction side) {
        boolean in = accessorPos.equals(worldPosition.relative(inDirection())) && (side == null || side == inDirection());
        boolean out = accessorPos.equals(worldPosition.relative(outDirection())) && (side == null || side == outDirection());
        if (!in && !out) return null;
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int index) { return tank.getFluidInTank(index); }
            @Override public int getTankCapacity(int index) { return tank.getTankCapacity(index); }
            @Override public boolean isFluidValid(int index, FluidStack stack) { return in && tank.isFluidValid(index, stack); }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!in || fluidReceiverSpeed(tank.type(), 0) <= 0 || tank.type().isNone()) return 0;
                if (HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none()) != tank.type()) return 0;
                int filled = tank.fill(tank.type(), resource.getAmount(), 0, action.simulate());
                if (filled > 0 && action.execute()) setChanged();
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (!out || redstone || tank.pressure() != 0) return FluidStack.EMPTY;
                if (HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none()) != tank.type()) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (!out || redstone || tank.pressure() != 0) return FluidStack.EMPTY;
                FluidStack drained = tank.drain(maxDrain, action);
                if (!drained.isEmpty() && action.execute()) {
                    isWorking = true;
                    setChanged();
                }
                return drained;
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("t", tank.save());
        tag.putByte("p", (byte) priority.ordinal());
        tag.putInt("buffer", bufferSize);
        tag.putBoolean("redstone", redstone);
        tag.putBoolean("isWorking", syncedWorking);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag.getCompound("t"));
        priority = byOrdinal(tag.getByte("p"));
        bufferSize = tag.contains("buffer") ? tag.getInt("buffer") : 100;
        redstone = tag.getBoolean("redstone");
        if (level != null && level.isClientSide) {
            isWorking = tag.getBoolean("isWorking");
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
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onFluidNetTransfer() {
        setChanged();
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** [parity U1: fluid net] getInPort() / getOutPort(): separate in (subscribe) and out (provide) port nodes */
    @Override
    public java.util.List<FluidPort> fluidPorts(HbmFluidDefinition type) {
        Direction in = inDirection();
        Direction out = outDirection();
        return java.util.List.of(new FluidPort(this.worldPosition.relative(in), in, PortRole.RECEIVE),
                new FluidPort(this.worldPosition.relative(out), out, PortRole.PROVIDE));
    }
}
