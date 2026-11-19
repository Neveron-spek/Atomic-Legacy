package com.atomiclegacy.tileentity;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.fluid.HbmThermalConversions;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class SteamCondenserBlockEntity extends BlockEntity implements FluidCopiable, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    public static final int TANK_CAPACITY = 100;

    private final HbmFluidTank inputTank = new HbmFluidTank(HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(HbmFluids.byName("water").orElse(HbmFluids.none()), TANK_CAPACITY);
    private int waterTimer;
    private int throughput;

    public SteamCondenserBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.STEAM_CONDENSER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamCondenserBlockEntity condenser) {
        // TileEntityCondenser only mutates tanks and its animation timer on the
        // logical server; clients receive the packed state from the server.
        if (level.isClientSide) {
            return;
        }
        condenser.setupTanks();
        condenser.tryConvert();
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, condenser); // [parity U1: fluid net]
        condenser.setChanged();
        if (level.getGameTime() % 20L == 0L) {
            condenser.sync();
        }
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public int throughput() {
        return this.throughput;
    }

    public int waterTimer() {
        return this.waterTimer;
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.inputTank.type().oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (HbmThermalConversions.condenserStep(fluid).isEmpty()) {
            return;
        }
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(fluid);
            sync();
        }
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new CondenserFluidHandler();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putInt("WaterTimer", this.waterTimer);
        tag.putInt("Throughput", this.throughput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.waterTimer = tag.getInt("WaterTimer");
        this.throughput = tag.getInt("Throughput");
        setupTanks();
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

    private void setupTanks() {
        HbmFluidDefinition spent = HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
        HbmFluidDefinition water = HbmFluids.byName("water").orElse(HbmFluids.none());
        if (this.inputTank.amount() == 0 && this.inputTank.type() != spent) {
            this.inputTank.setType(spent);
        }
        if (this.outputTank.amount() == 0 && this.outputTank.type() != water) {
            this.outputTank.setType(water);
        }
    }

    private void tryConvert() {
        if (this.waterTimer > 0) {
            this.waterTimer--;
        }
        HbmThermalConversions.condenserStep(this.inputTank.type()).ifPresentOrElse(step -> {
            int convert = Math.min(this.inputTank.amount(), this.outputTank.capacity() - this.outputTank.amount());
            this.throughput = convert;
            if (convert <= 0) {
                return;
            }
            this.inputTank.drain(step.input(), convert, false);
            this.outputTank.fill(step.output(), convert, false);
            this.waterTimer = 20;
        }, () -> this.throughput = 0);
    }


    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private final class CondenserFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return outputTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 || tank == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return HbmThermalConversions.condenserStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (HbmThermalConversions.condenserStep(fluid).isEmpty()) {
                return 0;
            }
            int filled = inputTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != outputTank.type()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = outputTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }

    // [parity U1: fluid net] TileEntityCondenser (TilePortShapes.around) as IFluidStandard*MK2 with TilePort nodes
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidSendingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{outputTank}; }
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{inputTank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return java.util.Arrays.stream(Direction.values()).map(d -> new FluidPort(this.worldPosition, d)).toList();
    }
}
