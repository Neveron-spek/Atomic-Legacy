package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.CoolingTowerBlock;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.fluid.HbmThermalConversions;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmParticleTypes;
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

import java.util.List;

public class CoolingTowerBlockEntity extends BlockEntity implements FluidCopiable, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    private final HbmFluidTank inputTank = new HbmFluidTank(spentSteam(), CoolingTowerBlock.Kind.SMALL.inputCapacity());
    private final HbmFluidTank outputTank = new HbmFluidTank(water(), CoolingTowerBlock.Kind.SMALL.outputCapacity());
    private int waterTimer;
    private int throughput;

    public CoolingTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.COOLING_TOWER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
        tower.setupTanks();
        tower.tryConvert();
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, tower); // [parity U1: fluid net]
        tower.setChanged();
        if (level.getGameTime() % 20L == 0L) {
            tower.sync();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
        if (!HbmClientConfig.ENABLE_COOLING_TOWER_PARTICLES.get()) {
            return;
        }
        CoolingTowerBlock.Kind kind = tower.kind();
        long interval = kind == CoolingTowerBlock.Kind.LARGE ? 4L : 2L;
        if (tower.waterTimer <= 0 || level.getGameTime() % interval != 0L) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + (kind == CoolingTowerBlock.Kind.LARGE ? 1.0D : 18.0D);
        double z = pos.getZ() + 0.5D;
        if (kind == CoolingTowerBlock.Kind.LARGE) {
            x += level.random.nextDouble() * 3.0D - 1.5D;
            z += level.random.nextDouble() * 3.0D - 1.5D;
        }
        level.addParticle(
                HbmParticleTypes.COOLING_TOWER.get(),
                x,
                y,
                z,
                kind == CoolingTowerBlock.Kind.LARGE ? 1.0D : 0.0D,
                0.0D,
                0.0D
        );
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
        if (fluid == spentSteam() && this.inputTank.amount() == 0) {
            this.inputTank.setType(fluid);
            sync();
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            return side == null ? new CoolingTowerFluidHandler() : null;
        }
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new CoolingTowerFluidHandler();
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
        setupTanks();
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

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : ports()) {
            if (queriedPos.equals(port.dummyPos()) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private List<Port> ports() {
        return switch (kind()) {
            case SMALL -> smallTowerPorts(this.worldPosition);
            case LARGE -> largeTowerPorts(this.worldPosition);
        };
    }

    private void setupTanks() {
        HbmFluidDefinition spent = spentSteam();
        HbmFluidDefinition water = water();
        CoolingTowerBlock.Kind kind = kind();
        this.inputTank.setCapacity(kind.inputCapacity());
        this.outputTank.setCapacity(kind.outputCapacity());
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
            for (Port port : ports()) {
                this.level.invalidateCapabilities(port.dummyPos());
            }
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public CoolingTowerBlock.Kind kind() {
        if (this.getBlockState().getBlock() instanceof CoolingTowerBlock towerBlock) {
            return towerBlock.kind();
        }
        return CoolingTowerBlock.Kind.SMALL;
    }

    public static List<Port> smallTowerPorts(BlockPos corePos) {
        return List.of(
                new Port(corePos.offset(0, 0, -2), corePos.offset(0, 0, -3), Direction.NORTH),
                new Port(corePos.offset(0, 0, 2), corePos.offset(0, 0, 3), Direction.SOUTH),
                new Port(corePos.offset(-2, 0, 0), corePos.offset(-3, 0, 0), Direction.WEST),
                new Port(corePos.offset(2, 0, 0), corePos.offset(3, 0, 0), Direction.EAST)
        );
    }

    public static List<Port> largeTowerPorts(BlockPos corePos) {
        return List.of(
                new Port(corePos.offset(0, 0, -4), corePos.offset(0, 0, -5), Direction.NORTH),
                new Port(corePos.offset(3, 0, -4), corePos.offset(3, 0, -5), Direction.NORTH),
                new Port(corePos.offset(-3, 0, -4), corePos.offset(-3, 0, -5), Direction.NORTH),
                new Port(corePos.offset(0, 0, 4), corePos.offset(0, 0, 5), Direction.SOUTH),
                new Port(corePos.offset(-3, 0, 4), corePos.offset(-3, 0, 5), Direction.SOUTH),
                new Port(corePos.offset(3, 0, 4), corePos.offset(3, 0, 5), Direction.SOUTH),
                new Port(corePos.offset(-4, 0, 0), corePos.offset(-5, 0, 0), Direction.WEST),
                new Port(corePos.offset(-4, 0, -3), corePos.offset(-5, 0, -3), Direction.WEST),
                new Port(corePos.offset(-4, 0, 3), corePos.offset(-5, 0, 3), Direction.WEST),
                new Port(corePos.offset(4, 0, 0), corePos.offset(5, 0, 0), Direction.EAST),
                new Port(corePos.offset(4, 0, 3), corePos.offset(5, 0, 3), Direction.EAST),
                new Port(corePos.offset(4, 0, -3), corePos.offset(5, 0, -3), Direction.EAST)
        );
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private final class CoolingTowerFluidHandler implements IFluidHandler {
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
            if (tank == 0) {
                return inputTank.capacity();
            }
            if (tank == 1) {
                return outputTank.capacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == spentSteam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != spentSteam()) {
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

    public record Port(BlockPos dummyPos, BlockPos connectorPos, Direction face) {
    }

    // [parity U1: fluid net] TileEntityTowerSmall/Large (TileEntityCondenser) as IFluidStandard*MK2 with TilePort nodes
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidSendingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{outputTank}; }
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{inputTank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return ports().stream().map(p -> new FluidPort(p.dummyPos(), p.face())).toList();
    }
}
