package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.LegacyBatteryBlock;
import com.atomiclegacy.inventory.container.LegacyBatteryMenu;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * [parity d2] TileEntityMachineBattery of 1.7.10 (machine_battery_potato, machine_battery, machine_lithium_battery,
 * machine_schrabidium_battery, machine_dineutronium_battery; all @Deprecated, creative tab null).
 *
 * <p>In the pinned original the block builds its power ports with {@code TilePortShapes.solderer(x, y, z,
 * getBlockMetadata())}. The shape expects a multiblock metadata (meta - 10), the battery has 2..5, so every port
 * direction resolves to ForgeDirection.UNKNOWN: the node sits on the battery itself and only links to itself, and
 * getRelevantMode(true) only ever reads a cache that nothing refreshes (always mode_input).
 *
 * <p>[parity h1: agreed deviation, user 2026-11-19 "fix obvious original bugs"] The battery is a PowerEndpoint on all
 * six faces (MachineBattery.canConnect(dir) = true, so 1.7.10 cables already drew the sleeve). The mode follows the
 * GUI buttons: redstone (isBlockIndirectlyGettingPowered) ? redHigh : redLow; provider speed maxPower/600 for
 * output/buffer, receiver speed maxPower/200 for input/buffer (TileEntityMachineBattery#getProviderSpeed /
 * getReceiverSpeed), priority from the GUI button. The tick order of updateEntity is kept: network, then slots.
 */
public final class LegacyBatteryBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int MODE_INPUT = 0;
    public static final int MODE_BUFFER = 1;
    public static final int MODE_OUTPUT = 2;
    public static final int MODE_NONE = 3;
    public static final int SLOT_COUNT = 2;
    public static final String NBT_PERSISTENT_KEY = "persistent";
    private static final int[] SLOTS_TOP = {0};
    private static final int[] SLOTS_BOTTOM = {0, 1};
    private static final int[] SLOTS_SIDE = {1};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final long[] log = new long[20];
    private long delta;
    private long power;
    private long prevPowerState;
    private short redLow = 0;
    private short redHigh = 2;
    private PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.LOW;
    private byte lastRedstone;
    private long lastInput;
    private long lastOutput;

    public LegacyBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_BATTERY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LegacyBatteryBlockEntity battery) {
        if (level.isClientSide || !(state.getBlock() instanceof LegacyBatteryBlock)) {
            return;
        }
        if (battery.priority == PowerEndpoint.ConnectionPriority.LOWEST || battery.priority == PowerEndpoint.ConnectionPriority.HIGHEST) {
            battery.priority = PowerEndpoint.ConnectionPriority.LOW;
        }
        long prevPower = battery.power;
        battery.lastInput = 0L;
        battery.lastOutput = 0L;
        PowerNetworkManager.tickFromEndpoint(level, battery);
        battery.power = BatteryPackItem.chargeFromMachine(battery.items.get(1), battery.power);

        byte comp = battery.getComparatorPower();
        if (comp != battery.lastRedstone) {
            battery.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        battery.lastRedstone = comp;

        battery.power = BatteryPackItem.dischargeIntoMachine(battery.items.get(0), battery.power, battery.getMaxPower());

        long avg = (battery.power + prevPower) / 2;
        long oldDelta = battery.delta;
        battery.delta = avg - battery.log[0];
        System.arraycopy(battery.log, 1, battery.log, 0, battery.log.length - 1);
        battery.log[19] = avg;
        battery.prevPowerState = battery.power;

        if (battery.power != prevPower || battery.delta != oldDelta) {
            battery.sync();
        }
    }

    public long getPower() {
        return this.power;
    }

    /** getRelevantMode(false): isBlockIndirectlyGettingPowered ? redHigh : redLow. */
    public int relevantMode() {
        return this.level != null && this.level.hasNeighborSignal(this.worldPosition) ? this.redHigh : this.redLow;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        int mode = relevantMode();
        if (mode != MODE_OUTPUT && mode != MODE_BUFFER) {
            return 0L;
        }
        return Math.min(this.power, getMaxPower() / 600L);
    }

    @Override
    public long getRequestedInput() {
        int mode = relevantMode();
        if (mode != MODE_INPUT && mode != MODE_BUFFER) {
            return 0L;
        }
        return Math.max(0L, Math.min(getMaxPower() - this.power, getMaxPower() / 200L));
    }

    @Override
    public PowerEndpoint.ConnectionPriority getPowerPriority() {
        return this.priority;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        long next = this.power;
        if (usedOutput > 0L) next = Math.max(0L, next - usedOutput);
        if (receivedInput > 0L) next = Math.min(getMaxPower(), next + receivedInput);
        this.power = next;
        this.lastOutput = usedOutput;
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.power + " / " + getMaxPower() + " HE (+" + this.lastInput + " / -" + this.lastOutput + ")");
    }

    public void setPower(long power) {
        this.power = power;
    }

    public long getMaxPower() {
        return getBlockState().getBlock() instanceof LegacyBatteryBlock block ? block.maxPower() : 0L;
    }

    public long delta() {
        return this.delta;
    }

    public int redLow() {
        return this.redLow;
    }

    public int redHigh() {
        return this.redHigh;
    }

    public PowerEndpoint.ConnectionPriority priority() {
        return this.priority;
    }

    public long getPowerRemainingScaled(long i) {
        long max = getMaxPower();
        return max <= 0L ? 0L : (this.power * i) / max;
    }

    public byte getComparatorPower() {
        if (this.power == 0) return 0;
        double frac = (double) this.power / (double) getMaxPower() * 15D;
        return (byte) Mth.clamp((int) frac + 1, 0, 15);
    }

    /** AuxButtonPacket ids 0..2 for TileEntityMachineBattery. */
    public void cycleLowMode() {
        this.redLow = (short) ((this.redLow + 1) % 4);
        sync();
    }

    public void cycleHighMode() {
        this.redHigh = (short) ((this.redHigh + 1) % 4);
        sync();
    }

    public void cyclePriority() {
        this.priority = switch (this.priority) {
            case LOW -> PowerEndpoint.ConnectionPriority.NORMAL;
            case NORMAL -> PowerEndpoint.ConnectionPriority.HIGH;
            case HIGH -> PowerEndpoint.ConnectionPriority.LOW;
            default -> this.priority;
        };
        sync();
    }

    /** IPersistentNBT#writeNBT. */
    public void saveToItem(ItemStack stack) {
        CompoundTag data = new CompoundTag();
        data.putLong("power", this.power);
        data.putLong("prevPowerState", this.prevPowerState);
        data.putShort("redLow", this.redLow);
        data.putShort("redHigh", this.redHigh);
        data.putInt("priority", this.priority.ordinal());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(NBT_PERSISTENT_KEY, data));
    }

    /** IPersistentNBT#readNBT (restoreData only runs for stacks that carry a tag). */
    public void loadFromItem(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        CompoundTag data = custom.copyTag().getCompound(NBT_PERSISTENT_KEY);
        this.power = data.getLong("power");
        this.prevPowerState = data.getLong("prevPowerState");
        this.redLow = data.getShort("redLow");
        this.redHigh = data.getShort("redHigh");
        this.priority = priorityOf(data.getInt("priority"));
        sync();
    }

    private static PowerEndpoint.ConnectionPriority priorityOf(int ordinal) {
        PowerEndpoint.ConnectionPriority[] values = PowerEndpoint.ConnectionPriority.VALUES;
        return values[Mth.clamp(ordinal, 0, values.length - 1)];
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putLong("power", this.power);
        tag.putShort("redLow", this.redLow);
        tag.putShort("redHigh", this.redHigh);
        tag.putByte("lastRedstone", this.lastRedstone);
        tag.putByte("priority", (byte) this.priority.ordinal());
        tag.putLong("delta", this.delta);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.power = tag.getLong("power");
        this.redLow = tag.getShort("redLow");
        this.redHigh = tag.contains("redHigh") ? tag.getShort("redHigh") : 2;
        this.lastRedstone = tag.getByte("lastRedstone");
        this.priority = tag.contains("priority") ? priorityOf(tag.getByte("priority")) : PowerEndpoint.ConnectionPriority.LOW;
        this.delta = tag.getLong("delta");
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

    public void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_BOTTOM : side == Direction.UP ? SLOTS_TOP : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return true; // TileEntityMachineBattery#isItemValidForSlot returns true for everything
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (BatteryPackItem.isBattery(stack)) {
            if (slot == 0 && BatteryPackItem.charge(stack) == 0L) return true;
            if (slot == 1 && BatteryPackItem.charge(stack) == BatteryPackItem.capacity(stack)) return true;
        }
        return false;
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return this.items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return this.items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(this.items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override public void clearContent() { this.items.clear(); }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        this.items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LegacyBatteryMenu(containerId, playerInventory, this);
    }
}
