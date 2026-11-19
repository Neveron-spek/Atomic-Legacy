package com.atomiclegacy.inventory.container;

import com.atomiclegacy.blocks.ParticleAcceleratorBlock;
import com.atomiclegacy.tileentity.ParticleAcceleratorBlockEntity;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ParticleAcceleratorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ParticleAcceleratorBlockEntity.SLOT_COUNT;
    // [parity e2] vanilla ContainerData is sent as signed shorts: every 32-bit value (power, momentum, threshold ...)
    // is split into two 16-bit data slots; value index DATA_COUNT carries the dipole threshold (TileEntityPADipole.threshold)
    private static final int VALUE_COUNT = ParticleAcceleratorBlockEntity.DATA_COUNT + 1;
    private static final int SYNC_COUNT = VALUE_COUNT * 2;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final ParticleAcceleratorBlock.Kind kind;

    public ParticleAcceleratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(SYNC_COUNT));
    }

    public ParticleAcceleratorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.PARTICLE_ACCELERATOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        if (data.getCount() != SYNC_COUNT) {
            checkContainerDataCount(data, ParticleAcceleratorBlockEntity.DATA_COUNT);
            data = new SplitData(data, container);
        }
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof ParticleAcceleratorBlockEntity accelerator ? accelerator.getBlockPos() : BlockPos.ZERO;
        this.kind = container instanceof ParticleAcceleratorBlockEntity accelerator ? accelerator.kind() : ParticleAcceleratorBlock.Kind.SOURCE;

        addMachineSlots();
        addPlayerInventory(playerInventory, 8, 122);
        addDataSlots(this.data);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public ParticleAcceleratorBlock.Kind kind() {
        return this.kind;
    }

    // [parity e2] slots exactly as ContainerPASource / ContainerPARFC / ContainerPAQuadrupole / ContainerPADipole /
    // ContainerPADetector: plain Slot for battery/inputs/coil, SlotTakeOnly (source) and SlotCraftingOutput (detector)
    private void addMachineSlots() {
        switch (this.kind) {
            case SOURCE -> {
                addSlot(new Slot(container, 0, 8, 72));
                addSlot(new Slot(container, 1, 62, 16));
                addSlot(new Slot(container, 2, 80, 16));
                addSlot(new TakeOnlySlot(container, 3, 62, 43));
                addSlot(new TakeOnlySlot(container, 4, 80, 43));
            }
            case RFC -> addSlot(new Slot(container, 0, 53, 72));
            case QUADRUPOLE -> {
                addSlot(new Slot(container, 0, 26, 72));
                addSlot(new Slot(container, 1, 71, 36));
            }
            case DIPOLE -> {
                addSlot(new Slot(container, 0, 8, 72));
                addSlot(new Slot(container, 1, 89, 26));
            }
            case DETECTOR -> {
                addSlot(new Slot(container, 0, 8, 72));
                addSlot(new Slot(container, 1, 62, 18));
                addSlot(new Slot(container, 2, 80, 18));
                addSlot(new OutputSlot(container, 3, 62, 45));
                addSlot(new OutputSlot(container, 4, 80, 45));
            }
            case BEAMLINE -> {
            }
        }
    }

    // [parity e2] transferStackInSlot of the five reference containers (including the "index <= 5" bound of
    // ContainerPASource/ContainerPADetector, which also covers the first player slot)
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        int size = this.slots.size();
        boolean battery = BatteryPackItem.isBattery(moved);

        switch (this.kind) {
            case SOURCE, DETECTOR -> {
                if (index <= 5) {
                    if (!moveItemStackTo(stack, 6, size, true)) return ItemStack.EMPTY;
                } else if (battery) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 1, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }
            case QUADRUPOLE, DIPOLE -> {
                if (index <= 1) {
                    if (!moveItemStackTo(stack, 2, size, true)) return ItemStack.EMPTY;
                } else if (battery) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }
            case RFC -> {
                if (index <= 0) {
                    if (!moveItemStackTo(stack, 1, size, true)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            case BEAMLINE -> {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private int value(int index) {
        return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16);
    }

    public int power() { return value(0); }
    public int maxPower() { return Math.max(1, value(1)); }
    public int coldAmount() { return value(2); }
    public int hotAmount() { return value(3); }
    public int coolantCapacity() { return Math.max(1, value(4)); }
    public float temperature() { return value(5) / 10.0F; }
    public float targetTemperature() { return value(6) / 10.0F; }
    public int stateOrdinal() { return value(7); }
    public int lastSpeed() { return value(8); }
    public int debugSpeed() { return value(9); }
    public int dirLower() { return value(10); }
    public int dirUpper() { return value(11); }
    public int dirRedstone() { return value(12); }
    public int threshold() { return value(ParticleAcceleratorBlockEntity.DATA_COUNT); }

    /** TileEntityPASource / PADipole / PAQuadrupole / PADetector: usage = 100_000 */
    public long usage() { return 100_000L; }

    public ParticleAcceleratorBlockEntity.PAState state() {
        ParticleAcceleratorBlockEntity.PAState[] values = ParticleAcceleratorBlockEntity.PAState.values();
        int ordinal = stateOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ParticleAcceleratorBlockEntity.PAState.IDLE;
    }

    /** slots[1] of the dipole/quadrupole (the coil) */
    public ItemStack coilStack() {
        return this.container.getItem(ParticleAcceleratorBlockEntity.INPUT_A_SLOT);
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof ParticleAcceleratorBlockEntity accelerator ? accelerator : new SimpleContainer(MACHINE_SLOT_COUNT);
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

    private static final class OutputSlot extends LegacyAchievementOutputSlot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class SplitData implements ContainerData {
        private final ContainerData source;
        private final Container container;

        private SplitData(ContainerData source, Container container) {
            this.source = source;
            this.container = container;
        }

        private int full(int valueIndex) {
            if (valueIndex < ParticleAcceleratorBlockEntity.DATA_COUNT) {
                return this.source.get(valueIndex);
            }
            return this.container instanceof ParticleAcceleratorBlockEntity accelerator ? accelerator.threshold() : 0;
        }

        @Override
        public int get(int index) {
            int full = full(index / 2);
            return (index & 1) == 0 ? full & 0xFFFF : (full >>> 16) & 0xFFFF;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return SYNC_COUNT;
        }
    }
}
