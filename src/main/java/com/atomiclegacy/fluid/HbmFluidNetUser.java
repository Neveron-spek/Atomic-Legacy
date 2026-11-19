package com.atomiclegacy.fluid;

import com.atomiclegacy.power.PowerEndpoint;

/**
 * [parity T1: fluid net] Port of {@code api.hbm.fluidmk2.IFluidStandardSenderMK2} /
 * {@code IFluidStandardReceiverMK2} (HbmMods 936c60c). A block entity (or the core of a
 * multiblock) implementing this is a provider/receiver of {@link HbmFluidNetworks} with
 * exact FluidNetMK2 semantics: per-pressure availability and demand, provider/receiver
 * speeds and connection priority. Endpoints without this interface fall back to the
 * NeoForge fluid capability at pressure 0 and NORMAL priority.
 */
public interface HbmFluidNetUser {
    int HIGHEST_VALID_PRESSURE = 5;
    int[] DEFAULT_PRESSURE_RANGE = new int[]{0, 0};
    long DEFAULT_SPEED = 1_000_000_000L;
    HbmFluidTank[] NO_TANKS = new HbmFluidTank[0];

    /**
     * [parity U1: fluid ports] TilePort.PortDef.make(x, y, z, dir): a port node position inside the machine
     * and its connection toward {@code pos.relative(dir)}. All ports of a machine are combined into one node
     * per fluid type (ModulePortManFluidAdaptive / setupFluidPorts use PortDef.combine).
     */
    record FluidPort(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction dir, PortRole role) {
        public FluidPort {
            pos = pos.immutable();
        }

        public FluidPort(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction dir) {
            this(pos, dir, PortRole.BOTH);
        }

        public net.minecraft.core.BlockPos connection() {
            return pos.relative(dir);
        }
    }

    /**
     * [parity U1: fluid ports] Port group of a TilePort node: BOTH for the combined in/out ports of
     * setupFluidPorts / ModulePortManFluidAdaptive, RECEIVE / PROVIDE for separate in and out port nodes
     * (FlowControlPump getInPort / getOutPort) which never join each other's nets.
     */
    enum PortRole { BOTH, RECEIVE, PROVIDE }

    /**
     * [parity U1: fluid ports] The port nodes of this machine for the given fluid type, or {@code null} for the
     * legacy discovery through the NeoForge fluid capability next to ducts. Machines returning ports must call
     * {@link HbmFluidNetworks#subscribe} every server tick (TilePort#update / checkSubscribe / checkProvide).
     */
    @org.jetbrains.annotations.Nullable
    default java.util.List<FluidPort> fluidPorts(HbmFluidDefinition type) {
        return null;
    }

    /** [parity U1: fluid net] false for block entity classes shared with machines that are not MK2 users */
    default boolean isFluidNetUser() {
        return true;
    }

    default HbmFluidTank[] fluidSendingTanks() {
        return NO_TANKS;
    }

    default HbmFluidTank[] fluidReceivingTanks() {
        return NO_TANKS;
    }

    /** TilePort registration: an out port exists for every sending tank of the net's type. */
    default boolean isFluidProvider(HbmFluidDefinition type) {
        for (HbmFluidTank tank : fluidSendingTanks()) {
            if (tank.type() == type) return true;
        }
        return false;
    }

    /** TilePort registration: an in port exists for every receiving tank of the net's type. */
    default boolean isFluidReceiver(HbmFluidDefinition type) {
        for (HbmFluidTank tank : fluidReceivingTanks()) {
            if (tank.type() == type) return true;
        }
        return false;
    }

    default long fluidProviderSpeed(HbmFluidDefinition type, int pressure) {
        return DEFAULT_SPEED;
    }

    default long fluidReceiverSpeed(HbmFluidDefinition type, int pressure) {
        return DEFAULT_SPEED;
    }

    default PowerEndpoint.ConnectionPriority fluidPriority() {
        return PowerEndpoint.ConnectionPriority.NORMAL;
    }

    /** IFluidStandardSenderMK2#getFluidAvailable */
    default long fluidAvailable(HbmFluidDefinition type, int pressure) {
        long amount = 0;
        for (HbmFluidTank tank : fluidSendingTanks()) {
            if (tank.type() == type && tank.pressure() == pressure) amount += tank.amount();
        }
        return amount;
    }

    /** IFluidStandardSenderMK2#useUpFluid */
    default void useUpFluid(HbmFluidDefinition type, int pressure, long amount) {
        int tanks = 0;
        for (HbmFluidTank tank : fluidSendingTanks()) {
            if (tank.type() == type && tank.pressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (HbmFluidTank tank : fluidSendingTanks()) {
                if (tank.type() == type && tank.pressure() == pressure) {
                    int toRem = Math.min(firstRound, tank.amount());
                    tank.setAmount(tank.amount() - toRem);
                    amount -= toRem;
                }
            }
        }
        if (amount > 0) for (HbmFluidTank tank : fluidSendingTanks()) {
            if (tank.type() == type && tank.pressure() == pressure) {
                int toRem = (int) Math.min(amount, tank.amount());
                tank.setAmount(tank.amount() - toRem);
                amount -= toRem;
            }
        }
    }

    /** IFluidStandardReceiverMK2#getDemand */
    default long fluidDemand(HbmFluidDefinition type, int pressure) {
        long amount = 0;
        for (HbmFluidTank tank : fluidReceivingTanks()) {
            if (tank.type() == type && tank.pressure() == pressure) amount += tank.capacity() - tank.amount();
        }
        return amount;
    }

    /** IFluidStandardReceiverMK2#transferFluid, returns the remainder */
    default long transferFluid(HbmFluidDefinition type, int pressure, long amount) {
        int tanks = 0;
        for (HbmFluidTank tank : fluidReceivingTanks()) {
            if (tank.type() == type && tank.pressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (HbmFluidTank tank : fluidReceivingTanks()) {
                if (tank.type() == type && tank.pressure() == pressure) {
                    int toAdd = Math.min(firstRound, tank.capacity() - tank.amount());
                    tank.setAmount(tank.amount() + toAdd);
                    amount -= toAdd;
                }
            }
        }
        if (amount > 0) for (HbmFluidTank tank : fluidReceivingTanks()) {
            if (tank.type() == type && tank.pressure() == pressure) {
                int toAdd = (int) Math.min(amount, tank.capacity() - tank.amount());
                tank.setAmount(tank.amount() + toAdd);
                amount -= toAdd;
            }
        }
        return amount;
    }

    default int[] providingPressureRange(HbmFluidDefinition type) {
        return pressureRange(fluidSendingTanks(), type);
    }

    default int[] receivingPressureRange(HbmFluidDefinition type) {
        return pressureRange(fluidReceivingTanks(), type);
    }

    /** Called after the net changed one of the tanks (TileEntity#markDirty equivalent). */
    default void onFluidNetTransfer() {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = fluidNetOwner(); // [parity V1: fluid net]
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
    }

    /**
     * [parity V1: fluid net] The block entity behind this user: itself, or the machine of a delegate
     * (TilePort#setupOwner(delegate), e.g. the coolant ports of the assembly and chemical factories).
     */
    default net.minecraft.world.level.block.entity.BlockEntity fluidNetOwner() {
        return this instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity ? blockEntity : null;
    }

    static int[] pressureRange(HbmFluidTank[] tanks, HbmFluidDefinition type) {
        int lowest = HIGHEST_VALID_PRESSURE;
        int highest = 0;
        for (HbmFluidTank tank : tanks) {
            if (tank.type() == type) {
                if (tank.pressure() < lowest) lowest = tank.pressure();
                if (tank.pressure() > highest) highest = tank.pressure();
            }
        }
        return lowest <= highest ? new int[]{lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }
}
