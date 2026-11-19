package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.FluidPumpBlock;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * [parity T1: fluid_pump] Deprecated FluidPump.TileEntityFluidPump (936c60c, hidden from the creative tabs):
 * a 100 mB buffer transceiver whose single port node sits on the pump and connects to both sides of the
 * rot(DOWN) axis. No GUI and no fluid identifier interaction exist in the original; type, buffer, pressure and
 * priority only come from NBT ("t", "buffer", "p").
 */
public class FluidPumpBlockEntity extends BlockEntity implements HbmFluidNetUser {
    public static final int MAX_BUFFER_SIZE = 10_000;

    private final HbmFluidTank tank = new HbmFluidTank(100);
    private int bufferSize = 100;
    private PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.NORMAL;
    private boolean redstone;

    public FluidPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FLUID_PUMP.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPumpBlockEntity pump) {
        // if the capacity were changed directly, any excess buffered fluid would be destroyed
        if (pump.bufferMismatch()) {
            pump.tank.setCapacity(Math.max(pump.tank.amount(), pump.bufferSize));
        }
        boolean next = level.hasNeighborSignal(pos);
        if (next != pump.redstone) {
            pump.redstone = next;
            pump.sync();
        }
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, pump); // [parity U1: fluid_pump]
    }

    public HbmFluidTank tank() {
        return tank;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public PowerEndpoint.ConnectionPriority priority() {
        return priority;
    }

    public boolean powered() {
        return redstone;
    }

    public boolean bufferMismatch() {
        return this.bufferSize != this.tank.capacity();
    }

    /** ForgeDirection.getOrientation(meta).getRotation(DOWN): the port connects to this side and its opposite */
    public Direction portAxis() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(FluidPumpBlock.FACING) ? state.getValue(FluidPumpBlock.FACING) : Direction.NORTH;
        return facing.getCounterClockWise();
    }

    @Override public HbmFluidTank[] fluidSendingTanks() { return new HbmFluidTank[]{tank}; }
    @Override public HbmFluidTank[] fluidReceivingTanks() { return new HbmFluidTank[]{tank}; }
    @Override public PowerEndpoint.ConnectionPriority fluidPriority() { return priority; }
    @Override public long fluidReceiverSpeed(HbmFluidDefinition type, int pressure) { return redstone || bufferMismatch() ? 0 : DEFAULT_SPEED; }
    @Override public long fluidProviderSpeed(HbmFluidDefinition type, int pressure) { return redstone ? 0 : DEFAULT_SPEED; }

    /** Port node shape for direct neighbours and pipe connection; pipe nets use the MK2 interface. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side != null && side.getAxis() != portAxis().getAxis()) {
            return null;
        }
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int index) { return tank.getFluidInTank(index); }
            @Override public int getTankCapacity(int index) { return tank.getTankCapacity(index); }
            @Override public boolean isFluidValid(int index, FluidStack stack) { return tank.isFluidValid(index, stack); }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (fluidReceiverSpeed(tank.type(), 0) <= 0 || tank.type().isNone()) return 0;
                if (HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none()) != tank.type()) return 0;
                int filled = tank.fill(tank.type(), resource.getAmount(), 0, action.simulate());
                if (filled > 0 && action.execute()) setChanged();
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none()) != tank.type()) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (redstone || tank.pressure() != 0) return FluidStack.EMPTY;
                FluidStack drained = tank.drain(maxDrain, action);
                if (!drained.isEmpty() && action.execute()) setChanged();
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // legacy port keys (Tank/BufferSize/Priority) are still read
        tank.load(tag.contains("t") ? tag.getCompound("t") : tag.getCompound("Tank"));
        bufferSize = tag.contains("buffer") ? tag.getInt("buffer") : tag.contains("BufferSize") ? tag.getInt("BufferSize") : 100;
        bufferSize = Math.max(0, Math.min(MAX_BUFFER_SIZE, bufferSize));
        priority = FlowControlPumpBlockEntity.byOrdinal(tag.contains("p") ? tag.getByte("p") : tag.getByte("Priority"));
        redstone = tag.getBoolean("redstone");
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

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** [parity U1: fluid_pump] getPorts(): PortDef.make(x, y, z, dir, dir.getOpposite()) - one node, both sides one net */
    @Override
    public java.util.List<FluidPort> fluidPorts(HbmFluidDefinition type) {
        Direction axis = portAxis();
        return java.util.List.of(new FluidPort(this.worldPosition, axis), new FluidPort(this.worldPosition, axis.getOpposite()));
    }
}
