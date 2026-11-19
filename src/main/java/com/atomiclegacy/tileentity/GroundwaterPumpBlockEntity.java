package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.GroundwaterPumpBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GroundwaterPumpBlockEntity extends BlockEntity implements PowerEndpoint, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    public static final int STEAM_SPEED = 1_000;
    public static final int ELECTRIC_SPEED = 10_000;
    public static final int STEAM_INPUT_PER_TICK = 100;
    public static final int SPENT_STEAM_PER_TICK = 1;
    public static final long ELECTRIC_POWER_PER_TICK = 1_000L;
    public static final long ELECTRIC_MAX_POWER = 10_000L;
    /** Oil-drill machines advance one drilling step on a 20-tick work cycle. */
    public static final int INTAKE_PIPE_INTERVAL = 20;
    /** [parity L2: pumps] TileEntityMachinePumpBase.groundHeight / groundDepth. */
    public static final int GROUND_HEIGHT = 70;
    public static final int GROUND_DEPTH = 4;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int STEAM_PULL_PER_PORT = 1_000;

    private final HbmFluidTank waterTank = new HbmFluidTank(water(), ELECTRIC_SPEED * 100);
    private final HbmFluidTank steamTank = new HbmFluidTank(steam(), 1_000);
    private final HbmFluidTank spentSteamTank = new HbmFluidTank(spentSteam(), 10);

    private long power;
    private long lastInput;
    private boolean active;
    private boolean onGround; // [parity L2: pumps]
    private float rotor;
    private float lastRotor;

    public GroundwaterPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.GROUNDWATER_PUMP.get(), pos, blockState);
        configureTankCapacities();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GroundwaterPumpBlockEntity pump) {
        if (level.isClientSide) {
            pump.tickClient(level);
            return;
        }
        pump.tickServer(level, state);
    }

    public GroundwaterPumpBlock.Kind kind() {
        BlockState state = getBlockState();
        if (state.is(HbmBlocks.PUMP_ELECTRIC.get())) {
            return GroundwaterPumpBlock.Kind.ELECTRIC;
        }
        return GroundwaterPumpBlock.Kind.STEAM;
    }

    public HbmFluidTank waterTank() {
        return this.waterTank;
    }

    public HbmFluidTank steamTank() {
        return this.steamTank;
    }

    public HbmFluidTank spentSteamTank() {
        return this.spentSteamTank;
    }

    public long power() {
        return this.power;
    }

    public long lastInput() {
        return this.lastInput;
    }

    public boolean active() {
        return this.active;
    }

    /** [parity L2: pumps] TileEntityMachinePumpBase.onGround, synced for the look overlay. */
    public boolean hasValidWaterSource() {
        return this.onGround;
    }

    public float rotor(float partialTick) {
        return this.lastRotor + (this.rotor - this.lastRotor) * partialTick;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new PumpFluidHandler(queriedPos.immutable(), side);
    }

    public List<Port> steamPumpPorts(LevelAccessor level) {
        return List.of(
                new Port(this.worldPosition.relative(Direction.EAST), Direction.EAST),
                new Port(this.worldPosition.relative(Direction.WEST), Direction.WEST),
                new Port(this.worldPosition.relative(Direction.SOUTH), Direction.SOUTH),
                new Port(this.worldPosition.relative(Direction.NORTH), Direction.NORTH)
        );
    }

    public List<Port> electricPumpPorts(LevelAccessor level) {
        return List.of(
                new Port(this.worldPosition.relative(Direction.EAST), Direction.EAST),
                new Port(this.worldPosition.relative(Direction.WEST), Direction.WEST),
                new Port(this.worldPosition.relative(Direction.SOUTH), Direction.SOUTH),
                new Port(this.worldPosition.relative(Direction.NORTH), Direction.NORTH)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        if (kind() != GroundwaterPumpBlock.Kind.ELECTRIC) {
            return List.of();
        }
        return List.of(
                this.worldPosition.offset(2, 0, 0).immutable(),
                this.worldPosition.offset(-2, 0, 0).immutable(),
                this.worldPosition.offset(0, 0, 2).immutable(),
                this.worldPosition.offset(0, 0, -2).immutable()
        );
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (kind() != GroundwaterPumpBlock.Kind.ELECTRIC || this.power >= ELECTRIC_MAX_POWER) {
            return 0L;
        }
        return ELECTRIC_MAX_POWER - this.power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(ELECTRIC_MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", this.power, ELECTRIC_MAX_POWER));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("WaterTank", this.waterTank.save());
        tag.put("SteamTank", this.steamTank.save());
        tag.put("SpentSteamTank", this.spentSteamTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Active", this.active);
        tag.putBoolean("OnGround", this.onGround); // [parity L2: pumps]
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        configureTankCapacities();
        this.waterTank.load(tag.getCompound("WaterTank"));
        this.steamTank.load(tag.getCompound("SteamTank"));
        this.spentSteamTank.load(tag.getCompound("SpentSteamTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.active = tag.getBoolean("Active");
        this.onGround = tag.getBoolean("OnGround"); // [parity L2: pumps]
        ensureTankTypes();
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

    private void tickServer(Level level, BlockState state) {
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, this); // [parity U1: fluid net]
        configureTankCapacities();
        ensureTankTypes();

        if (kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            PowerNetworkManager.tickFromEndpoint(level, this);
        }

        // [parity L2: pumps] TileEntityMachinePumpBase.updateEntity: groundCheckDelay is never set, so the ground is
        // checked every tick; the pump works below y = groundHeight (70) on mostly valid ground. No intake column.
        boolean oldActive = this.active;
        boolean oldGround = this.onGround;
        this.onGround = checkGround(level);
        this.active = false;
        if (hasOperatingResources() && this.worldPosition.getY() <= GROUND_HEIGHT && this.onGround) {
            this.active = true;
            operate();
        }
        if (oldGround != this.onGround) {
            oldActive = !this.active;
        }

        setChanged();
        if (oldActive != this.active || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level) {
        this.lastRotor = this.rotor;
        if (!this.active) {
            return;
        }

        this.rotor += 10.0F;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
            level.playLocalSound(
                    this.worldPosition.getX(),
                    this.worldPosition.getY(),
                    this.worldPosition.getZ(),
                    HbmSoundEvents.STEAM_ENGINE_OPERATE.get(),
                    SoundSource.BLOCKS,
                    0.5F,
                    0.75F,
                    false
            );
            level.playLocalSound(
                    this.worldPosition.getX(),
                    this.worldPosition.getY(),
                    this.worldPosition.getZ(),
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.5F,
                    false
            );
        }
    }

    private boolean hasOperatingResources() {
        if (this.waterTank.amount() >= this.waterTank.capacity()) {
            return false;
        }
        if (kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            return this.power >= ELECTRIC_POWER_PER_TICK;
        }
        return this.steamTank.amount() >= STEAM_INPUT_PER_TICK
                && this.spentSteamTank.capacity() - this.spentSteamTank.amount() >= SPENT_STEAM_PER_TICK;
    }

    /** [parity L2: pumps] TileEntityMachinePumpBase.checkGround with groundDepth = 4. */
    private boolean checkGround(Level level) {
        if (!level.dimensionType().hasSkyLight()) return false;
        int valid = 0;
        int invalid = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -GROUND_DEPTH; y--) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = this.worldPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (y == -1 && !state.isRedstoneConductor(level, pos)) return false; // first layer has to be full solid
                    if (isValidGround(state)) valid++;
                    else invalid++;
                }
            }
        }
        return valid >= invalid;
    }

    /** TileEntityMachinePumpBase.validBlocks; 1.7.10 dirt and sand covered their metadata variants. */
    private static boolean isValidGround(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.PODZOL)
                || state.is(net.minecraft.world.level.block.Blocks.SAND)
                || state.is(net.minecraft.world.level.block.Blocks.RED_SAND)
                || state.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
                || state.is(HbmBlocks.WASTE_EARTH.get())
                || state.is(HbmBlocks.DIRT_DEAD.get())
                || state.is(HbmBlocks.DIRT_OILY.get())
                || state.is(HbmBlocks.SAND_DIRTY.get())
                || state.is(HbmBlocks.SAND_DIRTY_RED.get());
    }

    private void operate() {
        if (kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            this.power -= ELECTRIC_POWER_PER_TICK;
            this.waterTank.fill(water(), ELECTRIC_SPEED, false);
            return;
        }
        this.steamTank.drain(steam(), STEAM_INPUT_PER_TICK, false);
        this.spentSteamTank.fill(spentSteam(), SPENT_STEAM_PER_TICK, false);
        this.waterTank.fill(water(), STEAM_SPEED, false);
    }






    private void configureTankCapacities() {
        this.waterTank.setCapacity(kind() == GroundwaterPumpBlock.Kind.ELECTRIC ? ELECTRIC_SPEED * 100 : STEAM_SPEED * 100);
        this.steamTank.setCapacity(1_000);
        this.spentSteamTank.setCapacity(10);
    }

    private void ensureTankTypes() {
        if (this.waterTank.amount() == 0 && this.waterTank.type() != water()) {
            this.waterTank.setType(water());
        }
        if (this.steamTank.amount() == 0 && this.steamTank.type() != steam()) {
            this.steamTank.setType(steam());
        }
        if (this.spentSteamTank.amount() == 0 && this.spentSteamTank.type() != spentSteam()) {
            this.spentSteamTank.setType(spentSteam());
        }
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        return switch (kind()) {
            case STEAM -> allowsSteamPumpPort(queriedPos, side);
            case ELECTRIC -> allowsElectricPumpPort(queriedPos, side);
        };
    }

    private boolean allowsSteamPumpPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : steamPumpPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private boolean allowsElectricPumpPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : electricPumpPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (kind() == GroundwaterPumpBlock.Kind.STEAM) {
                for (Port port : steamPumpPorts(this.level)) {
                    this.level.invalidateCapabilities(port.pos());
                }
            } else {
                for (Port port : electricPumpPorts(this.level)) {
                    this.level.invalidateCapabilities(port.pos());
                }
            }
        }
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
    }

    private final class PumpFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private PumpFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return kind() == GroundwaterPumpBlock.Kind.STEAM ? 3 : 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            HbmFluidTank selected = tankForIndex(tank);
            return selected == null ? FluidStack.EMPTY : selected.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            HbmFluidTank selected = tankForIndex(tank);
            return selected == null ? 0 : selected.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (kind() != GroundwaterPumpBlock.Kind.STEAM || tank != 0 || stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == steam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || kind() != GroundwaterPumpBlock.Kind.STEAM || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != steam()) {
                return 0;
            }
            int filled = steamTank.fill(steam(), resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid == water()) {
                return drainTank(waterTank, fluid, resource.getAmount(), action);
            }
            if (kind() == GroundwaterPumpBlock.Kind.STEAM && fluid == spentSteam()) {
                return drainTank(spentSteamTank, fluid, resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            if (waterTank.amount() > 0) {
                return drainTank(waterTank, water(), maxDrain, action);
            }
            if (kind() == GroundwaterPumpBlock.Kind.STEAM && spentSteamTank.amount() > 0) {
                return drainTank(spentSteamTank, spentSteam(), maxDrain, action);
            }
            return FluidStack.EMPTY;
        }

        @Nullable
        private HbmFluidTank tankForIndex(int tank) {
            if (kind() == GroundwaterPumpBlock.Kind.STEAM) {
                return switch (tank) {
                    case 0 -> steamTank;
                    case 1 -> spentSteamTank;
                    case 2 -> waterTank;
                    default -> null;
                };
            }
            return tank == 0 ? waterTank : null;
        }

        private FluidStack drainTank(HbmFluidTank tank, HbmFluidDefinition fluid, int amount, FluidAction action) {
            FluidStack drained = tank.drain(HbmFluids.toNeoStack(fluid, amount), action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }

    // [parity U1: fluid net] TileEntityMachinePumpElectric / TileEntityMachinePumpSteam (TilePortShapes.flare) as IFluidStandard*MK2 with TilePort nodes
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return java.util.stream.Stream.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST).map(d -> new FluidPort(this.worldPosition.relative(d), d)).toList();
    }
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidSendingTanks() { return kind() == GroundwaterPumpBlock.Kind.ELECTRIC ? new com.atomiclegacy.fluid.HbmFluidTank[]{waterTank} : new com.atomiclegacy.fluid.HbmFluidTank[]{waterTank, spentSteamTank}; }
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return kind() == GroundwaterPumpBlock.Kind.ELECTRIC ? NO_TANKS : new com.atomiclegacy.fluid.HbmFluidTank[]{steamTank}; }
}
