package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.DieselGeneratorBlock;
import com.atomiclegacy.client.sound.DieselGeneratorClientSounds;
import com.atomiclegacy.fluid.CombustibleFuelGrade;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.inventory.container.DieselGeneratorMenu;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DieselGeneratorBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    public static final long BASE_MAX_POWER = 50_000L;
    public static final int FUEL_CAPACITY = 16_000; // [parity L1: diesel] fuelCap
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;
    // [parity L1: diesel] 1.7.10 has 4 slots; tank.setType(3, slots) keeps the identifier in slot 3.
    public static final int SLOT_IDENTIFIER_INPUT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 8;
    private static final int SMOKE_BUFFER_CAPACITY = 100;
    private static final int[] TOP_SLOTS = {SLOT_INPUT};
    private static final int[] BOTTOM_SLOTS = {SLOT_OUTPUT, SLOT_BATTERY};
    private static final int[] SIDE_SLOTS = {SLOT_BATTERY};
    private static final Map<CombustibleFuelGrade, Double> FUEL_EFFICIENCY = Map.of(
            CombustibleFuelGrade.MEDIUM, 0.5D,
            CombustibleFuelGrade.HIGH, 0.75D,
            CombustibleFuelGrade.AERO, 0.1D
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank fuelTank = new HbmFluidTank(defaultFuel(), FUEL_CAPACITY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(HbmPollution.smokeFluid(com.atomiclegacy.pollution.HbmPollutionType.SOOT), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(HbmPollution.smokeFluid(com.atomiclegacy.pollution.HbmPollutionType.HEAVYMETAL), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(HbmPollution.smokeFluid(com.atomiclegacy.pollution.HbmPollutionType.POISON), SMOKE_BUFFER_CAPACITY);
    private long power;
    private long powerCap = BASE_MAX_POWER;
    private boolean running;
    private boolean isOn; // [parity L1: diesel] GUI toggle, NBT "isOn"
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) DieselGeneratorBlockEntity.this.power;
                case 1 -> (int) DieselGeneratorBlockEntity.this.powerCap;
                case 2 -> DieselGeneratorBlockEntity.this.running ? 1 : 0;
                case 3 -> DieselGeneratorBlockEntity.this.fuelTank.type().oldId();
                case 4 -> DieselGeneratorBlockEntity.this.fuelTank.amount();
                case 5 -> DieselGeneratorBlockEntity.this.fuelTank.capacity();
                case 6 -> (int) DieselGeneratorBlockEntity.this.hePerTick();
                case 7 -> DieselGeneratorBlockEntity.this.isOn ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DieselGeneratorBlockEntity.this.power = value;
                case 1 -> DieselGeneratorBlockEntity.this.powerCap = value;
                case 2 -> DieselGeneratorBlockEntity.this.running = value != 0;
                case 7 -> DieselGeneratorBlockEntity.this.isOn = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DieselGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DIESEL_GENERATOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DieselGeneratorBlockEntity diesel) {
        if (level.isClientSide) {
            DieselGeneratorClientSounds.tick(diesel);
            return;
        }
        diesel.tickServer(level);
    }

    public static boolean isBattery(ItemStack stack) {
        return BatteryPackItem.isBattery(stack);
    }

    public static boolean acceptsFuel(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return false;
        }
        CombustibleFuelGrade grade = fluid.combustibleFuelGrade();
        return grade != null
                && grade != CombustibleFuelGrade.LOW
                && fluid.combustibleHeatEnergy() > 0; // [parity L1: diesel] any FT_Combustible grade except LOW
    }

    public static long heFromFuel(HbmFluidDefinition fluid) {
        if (!acceptsFuel(fluid)) {
            return 0L;
        }
        double efficiency = fuelEfficiency(fluid.combustibleFuelGrade());
        // 1.7.10 performs the integer division first:
        // (combustionEnergy / 1000L) * efficiency, then truncates to long.
        return (long) ((fluid.combustibleHeatEnergy() / 1000L) * efficiency);
    }

    public static double fuelEfficiency(CombustibleFuelGrade grade) {
        return FUEL_EFFICIENCY.getOrDefault(grade, 0.0D);
    }

    public HbmFluidTank fuelTank() {
        return this.fuelTank;
    }

    public boolean running() {
        return this.running;
    }

    public long power() {
        return this.power;
    }

    public long powerCap() {
        return this.powerCap;
    }

    public long hePerTick() {
        return heFromFuel(this.fuelTank.type());
    }

    public boolean hasAcceptableFuel() {
        return acceptsFuel(this.fuelTank.type());
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new DieselFluidHandler();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                this.worldPosition.north(),
                this.worldPosition.south(),
                this.worldPosition.east(),
                this.worldPosition.west(),
                this.worldPosition.above(),
                this.worldPosition.below()
        );
    }

    @Override
    public long getAvailableOutput() {
        return this.power;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.max(0L, this.power - usedOutput);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.atomiclegacy.power.diesel_generator",
                hePerTick(),
                this.power,
                this.powerCap
        );
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> canDrainIntoFuel(stack);
            case SLOT_BATTERY -> isBattery(stack);
            case SLOT_IDENTIFIER_INPUT -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case DOWN -> BOTTOM_SLOTS;
            case UP -> TOP_SLOTS;
            default -> SIDE_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_OUTPUT) {
            // [parity L1: diesel] only canister_empty or tank_steel may be extracted.
            return stack.is(com.atomiclegacy.registry.HbmItems.CANISTER_EMPTY.get()) || stack.is(com.atomiclegacy.registry.HbmItems.TANK_STEEL.get());
        }
        return slot == SLOT_BATTERY && isBattery(stack) && BatteryPackItem.charge(stack) >= BatteryPackItem.capacity(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.machine_diesel");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DieselGeneratorMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putLong("PowerCap", this.powerCap);
        tag.putBoolean("Running", this.running);
        tag.putBoolean("isOn", this.isOn);
        tag.put("FuelTank", this.fuelTank.save());
        tag.put("Smoke", this.smokeTank.save());
        tag.put("SmokeLeaded", this.smokeLeadedTank.save());
        tag.put("SmokePoison", this.smokePoisonTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.powerCap = tag.contains("PowerCap") ? tag.getLong("PowerCap") : BASE_MAX_POWER;
        this.running = tag.getBoolean("Running");
        this.isOn = tag.getBoolean("isOn");
        this.fuelTank.load(tag.getCompound("FuelTank"));
        this.smokeTank.load(tag.getCompound("Smoke"));
        this.smokeLeadedTank.load(tag.getCompound("SmokeLeaded"));
        this.smokePoisonTank.load(tag.getCompound("SmokePoison"));
        if (this.fuelTank.type().isNone() && this.fuelTank.amount() == 0) {
            this.fuelTank.setType(defaultFuel());
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

    private void tickServer(Level level) {
        boolean changed = applyFluidIdentifierSlot();
        changed |= transferFluidInput();
        this.powerCap = BASE_MAX_POWER;
        this.power = BatteryPackItem.chargeFromMachine(this.items.get(SLOT_BATTERY), this.power);
        this.running = false;
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, this); // [parity U1: fluid net]

        if (this.isOn && !level.hasNeighborSignal(this.worldPosition) && acceptsFuel(this.fuelTank.type()) && this.fuelTank.amount() > 0) {
            this.running = true;
            HbmFluidDefinition burnedFuel = this.fuelTank.type();
            this.fuelTank.drain(this.fuelTank.type(), 1, false);
            // The legacy FluidTank retains its configured type when it reaches
            // zero, so the generator never switches fuel types implicitly.
            if (this.fuelTank.amount() == 0) {
                this.fuelTank.setType(burnedFuel);
            }
            if (level.getGameTime() % 5L == 0L) {
                HbmPollution.bufferedLegacyPolluteFluid(level, this.worldPosition, burnedFuel, HbmPollution.ReleaseType.BURN, 5.0D, this::smokeTank);
            }
            this.power = Math.min(this.powerCap, this.power + hePerTick());
            changed = true;
        }

        PowerNetworkManager.tickFromEndpoint(level, this);
        setLit(this.running);
        if (changed || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private boolean transferFluidInput() {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.fuelTank,
                DieselGeneratorBlockEntity::acceptsFuel,
                output -> canPlaceOutput(SLOT_OUTPUT, output),
                output -> placeOutput(SLOT_OUTPUT, output)
        );
        if (moved && input.isEmpty()) {
            this.items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        return moved;
    }

    /** [parity L1: diesel] FluidTank#setType(3, slots): new type empties the tank, identifier stays. */
    private boolean applyFluidIdentifierSlot() {
        ItemStack identifier = this.items.get(SLOT_IDENTIFIER_INPUT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
        if (this.fuelTank.type() == selected) {
            return false;
        }
        this.fuelTank.setType(selected);
        this.fuelTank.setAmount(0);
        return true;
    }

    /** [parity L1: diesel] receiveControl "turnOn" toggles isOn. */
    public void toggleOn() {
        this.isOn = !this.isOn;
        setChanged();
    }

    private boolean canDrainIntoFuel(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE))
                .flatMap(fluidStack -> HbmFluids.fromNeoFluid(fluidStack.getFluid()))
                .filter(fluid -> fluid == this.fuelTank.type()) // [parity L1: diesel] getFluidContent(stack, tank type) > 0
                .isPresent();
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof DieselGeneratorBlock && state.getValue(DieselGeneratorBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(DieselGeneratorBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }


    private HbmFluidTank smokeTank(com.atomiclegacy.pollution.HbmPollutionType type) {
        return switch (type) {
            case HEAVYMETAL -> this.smokeLeadedTank;
            case POISON -> this.smokePoisonTank;
            case SOOT, FALLOUT -> this.smokeTank;
        };
    }

    private static HbmFluidDefinition defaultFuel() {
        return HbmFluids.byName("diesel").orElse(HbmFluids.none());
    }

    private final class DieselFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fuelTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? fuelTank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            // [parity L1: diesel] IFluidStandardTransceiverMK2 receives only the tank type.
            return HbmFluids.fromNeoFluid(stack.getFluid()).filter(fluid -> fluid == fuelTank.type()).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != fuelTank.type()) { // [parity L1: diesel]
                return 0;
            }
            int filled = fuelTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    // [parity U1: fluid net] TileEntityMachineDiesel (TilePortShapes.around) as IFluidStandard*MK2 with TilePort nodes
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidSendingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{smokeTank, smokeLeadedTank, smokePoisonTank}; }
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{fuelTank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return java.util.Arrays.stream(Direction.values()).map(d -> new FluidPort(this.worldPosition, d)).toList();
    }
}
