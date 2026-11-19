package com.atomiclegacy.inventory.container;

import com.atomiclegacy.blocks.PneumaticStorageBlock;
import com.atomiclegacy.tileentity.PneumaticStorageBlockEntity;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Shared menu retaining the exact slot layouts of the five legacy endpoints. */
public final class PneumaticStorageMenu extends AbstractContainerMenu {
    public static final int ACCESS_SLOTS = 48;

    private final PneumaticStorageBlockEntity storage;
    private final PneumaticStorageBlock.Kind kind;
    private final Container machineContainer;
    // [parity V2: storage] ContainerPneumoStorageAccess: the server sends the whole cache, the client builds the grid
    private final SimpleContainer accessDisplay = new SimpleContainer(ACCESS_SLOTS);
    private final long[] accessAmounts = new long[ACCESS_SLOTS];
    private final Player player;
    private List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> clientEntries = List.of();
    private int clientEntriesVersion;
    private long accessRevision = Long.MIN_VALUE;
    private List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> sentEntries;
    private final int machineSlotCount;

    public PneumaticStorageMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()), readKind(buffer));
    }

    public PneumaticStorageMenu(int id, Inventory inventory, PneumaticStorageBlockEntity storage) {
        this(id, inventory, storage, storage.kind());
    }

    private PneumaticStorageMenu(int id, Inventory inventory, PneumaticStorageBlockEntity storage,
                                 PneumaticStorageBlock.Kind fallbackKind) {
        super(HbmMenus.PNEUMATIC_STORAGE.get(), id);
        this.player = inventory.player;
        this.storage = storage;
        this.kind = storage == null ? fallbackKind : storage.kind();
        this.machineContainer = storage == null ? new SimpleContainer(slotCount(this.kind)) : storage;

        switch (this.kind) {
            case ACCESS -> {
                machineSlotCount = ACCESS_SLOTS;
                for (int row = 0; row < 6; row++) {
                    for (int column = 0; column < 8; column++) {
                        addSlot(new ReadOnlySlot(accessDisplay, column + row * 8, 42 + column * 18, 17 + row * 18));
                    }
                }
                addPlayerInventory(inventory, 42, 169, 227);
            }
            case CLUTTER -> {
                machineSlotCount = PneumaticStorageBlockEntity.CLUTTER_SLOTS;
                for (int row = 0; row < 6; row++) {
                    for (int column = 0; column < 9; column++) {
                        addSlot(new Slot(machineContainer, column + row * 9, 8 + column * 18, 17 + row * 18));
                    }
                }
                addPlayerInventory(inventory, 8, 153, 211);
            }
            case MONO -> {
                machineSlotCount = PneumaticStorageBlockEntity.MONO_SLOTS;
                for (int row = 0; row < 3; row++) {
                    addSlot(new ReadOnlySlot(machineContainer, row, 8, 17 + row * 18));
                }
                addPlayerInventory(inventory, 8, 99, 157);
            }
            case IMPORTER -> {
                machineSlotCount = PneumaticStorageBlockEntity.IMPORTER_SLOTS;
                for (int row = 0; row < 3; row++) {
                    for (int column = 0; column < 3; column++) {
                        addSlot(new Slot(machineContainer, column + row * 3, 62 + column * 18, 17 + row * 18));
                    }
                }
                addPlayerInventory(inventory, 8, 103, 161);
            }
            case EXPORTER -> {
                machineSlotCount = PneumaticStorageBlockEntity.EXPORTER_TOTAL_SLOTS;
                // [parity e2] ContainerPneumoStorageExporter: nine SlotPattern first, then addTakeOnlySlots(exporter, 9, 80, 17, 3, 3)
                for (int index = 0; index < 9; index++) {
                    addSlot(new ReadOnlySlot(machineContainer, index, 17 + (index % 3) * 18, 17 + (index / 3) * 18));
                }
                for (int index = 0; index < 9; index++) {
                    addSlot(new TakeOnlySlot(machineContainer, index + PneumaticStorageBlockEntity.EXPORTER_OUTPUT_START,
                            80 + (index % 3) * 18, 17 + (index / 3) * 18));
                }
                addPlayerInventory(inventory, 8, 103, 161);
            }
            default -> throw new IllegalStateException("Unhandled pneumatic storage kind " + this.kind);
        }
    }

    public PneumaticStorageBlock.Kind kind() {
        return kind;
    }

    public PneumaticStorageBlockEntity storage() {
        return storage;
    }

    public long accessAmount(int slot) {
        return slot >= 0 && slot < accessAmounts.length ? accessAmounts[slot] : 0L;
    }

    /** Client: the CacheSlotDummy index received from the server. */
    public void acceptAccessEntries(List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> entries) {
        this.clientEntries = List.copyOf(entries);
        this.clientEntriesVersion++;
    }

    public List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> clientAccessEntries() {
        return clientEntries;
    }

    public int clientAccessEntriesVersion() {
        return clientEntriesVersion;
    }

    /** Client: rebuildClientIndex writes the visible window of the sorted and filtered index into the grid. */
    public void setAccessGrid(List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> visible) {
        for (int slot = 0; slot < ACCESS_SLOTS; slot++) {
            if (slot < visible.size()) {
                accessDisplay.setItem(slot, visible.get(slot).stack().copyWithCount(1));
                accessAmounts[slot] = visible.get(slot).amount();
            } else {
                accessDisplay.setItem(slot, ItemStack.EMPTY);
                accessAmounts[slot] = 0L;
            }
        }
    }

    @Override
    public void broadcastChanges() {
        refreshAccess();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (storage != null && !player.level().isClientSide) {
            if (kind == PneumaticStorageBlock.Kind.ACCESS && slotId >= 0 && slotId < ACCESS_SLOTS) {
                return; // [parity V2: storage] grid clicks arrive as PneumaticAccessPayloads.Click
            }
            if (kind == PneumaticStorageBlock.Kind.MONO && slotId >= 0
                    && slotId < PneumaticStorageBlockEntity.MONO_SLOTS) {
                storage.setMonoFilter(slotId, getCarried());
                return;
            }
            if (kind == PneumaticStorageBlock.Kind.EXPORTER && slotId >= 0
                    && slotId < PneumaticStorageBlockEntity.EXPORTER_FILTER_SLOTS) {
                storage.setExporterFilter(slotId, getCarried());
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (storage == null) {
            return false;
        }
        return switch (id) {
            case 0 -> {
                storage.cyclePressure();
                yield kind == PneumaticStorageBlock.Kind.CLUTTER || kind == PneumaticStorageBlock.Kind.MONO;
            }
            case 1 -> {
                storage.toggleContinuousRequest();
                yield kind == PneumaticStorageBlock.Kind.EXPORTER;
            }
            case 2 -> {
                storage.cycleRequestMode();
                yield kind == PneumaticStorageBlock.Kind.EXPORTER;
            }
            case 3 -> {
                storage.toggleRttyConfiguredMode();
                yield kind == PneumaticStorageBlock.Kind.EXPORTER;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        if (kind == PneumaticStorageBlock.Kind.ACCESS) {
            return quickMoveAccess(player, index);
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlotCount) {
            if (!slot.mayPickup(player) || !moveItemStackTo(stack, machineSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int targetStart = switch (kind) {
                case CLUTTER, IMPORTER -> 0;
                default -> machineSlotCount;
            };
            if (targetStart == machineSlotCount || !moveItemStackTo(stack, targetStart, machineSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage == null || storage.stillValid(player);
    }

    /** ContainerPneumoStorageAccess.slotClick: shift-clicking a player stack adds it to the system. */
    private ItemStack quickMoveAccess(Player player, int index) {
        if (storage == null || !(player.level() instanceof ServerLevel level) || index < ACCESS_SLOTS) {
            return ItemStack.EMPTY;
        }
        Slot playerSlot = slots.get(index);
        if (!playerSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = playerSlot.getItem().copy();
        long remainder = PneumaticNetworkManager.insert(level, storage.getBlockPos(), original, original.getCount(), false);
        int moved = original.getCount() - (int) remainder;
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        playerSlot.getItem().shrink(moved);
        if (playerSlot.getItem().isEmpty()) {
            playerSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            playerSlot.setChanged();
        }
        return original.copyWithCount(moved);
    }

    /**
     * [parity V2: storage] ContainerPneumoStorageAccess.acceptData (server): 0 LEFT_CLICK, 1 RIGHT_CLICK, 2 SHIFT_CLICK
     * on the grid stack {@code clicked} (empty for an empty grid slot).
     */
    public void handleAccessClick(Player player, int type, ItemStack clicked) {
        if (kind != PneumaticStorageBlock.Kind.ACCESS || storage == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = storage.getBlockPos();
        ItemStack held = getCarried();
        boolean sameType = !held.isEmpty() && !clicked.isEmpty() && ItemStack.isSameItemSameComponents(held, clicked);
        if (type == 0 || type == 1) {
            // if we drop an item onto a different stack, or no stack at all, deposit
            if (!held.isEmpty() && !sameType) {
                int toDeposit = type == 0 ? held.getCount() : 1;
                long remainder = PneumaticNetworkManager.insert(level, pos, held, toDeposit, false);
                held.shrink(toDeposit - (int) remainder);
                setCarried(held.isEmpty() ? ItemStack.EMPTY : held);
                broadcastChanges();
                return;
            }
            // if our hand is empty or we have the same type, withdraw
            if (!clicked.isEmpty()) {
                ItemStack stack = clicked.copyWithCount(1);
                int alreadyHeld = held.isEmpty() ? 0 : held.getCount();
                int capacity = stack.getMaxStackSize() - alreadyHeld;
                if (type == 1 && capacity > 1) capacity = 1;
                long toGrab = Math.min(capacity, PneumaticNetworkManager.available(level, pos, stack));
                int grabbed = toGrab <= 0 ? 0 : (int) PneumaticNetworkManager.extract(level, pos, stack, toGrab, false);
                int size = alreadyHeld + grabbed;
                setCarried(size > 0 ? stack.copyWithCount(size) : ItemStack.EMPTY);
                broadcastChanges();
            }
            return;
        }
        if (type == 2 && !clicked.isEmpty()) {
            ItemStack stack = clicked.copyWithCount(1);
            int grabbed = (int) PneumaticNetworkManager.extract(level, pos, stack, inventorySpace(player, stack), false);
            if (grabbed > 0) {
                ItemStack moved = stack.copyWithCount(grabbed);
                if (!player.getInventory().add(moved) && !moved.isEmpty()) {
                    PneumaticNetworkManager.insert(level, pos, moved, moved.getCount(), false);
                }
            }
            broadcastChanges();
        }
    }

    private static boolean sameEntries(List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> first,
                                       List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).amount() != second.get(i).amount()
                    || !ItemStack.isSameItemSameComponents(first.get(i).stack(), second.get(i).stack())) return false;
        }
        return true;
    }

    /** ContainerPneumoStorageAccess.getInventorySpace: a free main-inventory slot means one full stack. */
    private static int inventorySpace(Player player, ItemStack stack) {
        int space = 0;
        for (ItemStack existing : player.getInventory().items) {
            if (existing.isEmpty()) return stack.getMaxStackSize();
            if (ItemStack.isSameItemSameComponents(existing, stack)) space += Math.max(existing.getMaxStackSize() - existing.getCount(), 0);
        }
        return Math.min(space, stack.getMaxStackSize());
    }

    /** ContainerPneumoStorageAccess.checkAndSyncCache: pushes the cache to the client whenever it changed. */
    private void refreshAccess() {
        if (kind != PneumaticStorageBlock.Kind.ACCESS || storage == null
                || !(storage.getLevel() instanceof ServerLevel level) || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        PneumaticNetworkManager.Snapshot snapshot = PneumaticNetworkManager.snapshot(level, storage.getBlockPos());
        if (snapshot.revision() == accessRevision) {
            return;
        }
        accessRevision = snapshot.revision();
        List<com.atomiclegacy.network.PneumaticAccessPayloads.Entry> entries = new ArrayList<>(snapshot.entries().size());
        for (PneumaticNetworkManager.Entry entry : snapshot.entries()) {
            entries.add(new com.atomiclegacy.network.PneumaticAccessPayloads.Entry(entry.stack(), entry.amount()));
        }
        if (sentEntries != null && sameEntries(sentEntries, entries)) {
            return; // air top-ups bump the revision without changing the cache
        }
        sentEntries = entries;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                new com.atomiclegacy.network.PneumaticAccessPayloads.Sync(containerId, entries));
    }

    private void addPlayerInventory(Inventory inventory, int x, int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, hotbarY));
        }
    }

    private static int slotCount(PneumaticStorageBlock.Kind kind) {
        return switch (kind) {
            case ACCESS -> 0;
            case CLUTTER -> PneumaticStorageBlockEntity.CLUTTER_SLOTS;
            case MONO -> PneumaticStorageBlockEntity.MONO_SLOTS;
            case IMPORTER -> PneumaticStorageBlockEntity.IMPORTER_SLOTS;
            case EXPORTER -> PneumaticStorageBlockEntity.EXPORTER_TOTAL_SLOTS;
        };
    }

    private static PneumaticStorageBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof PneumaticStorageBlockEntity storage ? storage : null;
    }

    private static PneumaticStorageBlock.Kind readKind(RegistryFriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        PneumaticStorageBlock.Kind[] values = PneumaticStorageBlock.Kind.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PneumaticStorageBlock.Kind.ACCESS;
    }

    private static class ReadOnlySlot extends Slot {
        private ReadOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
