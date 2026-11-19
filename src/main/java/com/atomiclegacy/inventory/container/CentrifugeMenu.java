package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.CentrifugeBlockEntity;
import com.atomiclegacy.tileentity.ShredderBlockEntity;
import com.atomiclegacy.items.MachineUpgradeItem;
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

public class CentrifugeMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CentrifugeBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public CentrifugeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CentrifugeBlockEntity.DATA_COUNT));
    }

    public CentrifugeMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CENTRIFUGE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CentrifugeBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        // [parity K3: centrifuge] ContainerCentrifuge (1.7.10) slot layout
        this.addSlot(new Slot(container, CentrifugeBlockEntity.INPUT_SLOT, 44, 57));
        this.addSlot(new Slot(container, CentrifugeBlockEntity.BATTERY_SLOT, 8, 57));
        for (int slot = CentrifugeBlockEntity.OUTPUT_START; slot < CentrifugeBlockEntity.OUTPUT_END; slot++) {
            this.addSlot(new OutputSlot(container, slot, 70 + (slot - CentrifugeBlockEntity.OUTPUT_START) * 20, 57));
        }
        this.addSlot(new UpgradeSlot(container, CentrifugeBlockEntity.UPGRADE_START, 156, 31));
        this.addSlot(new UpgradeSlot(container, CentrifugeBlockEntity.UPGRADE_START + 1, 156, 49));
        addPlayerInventory(playerInventory, 11, 107);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();

        // [parity K3: centrifuge] ContainerCentrifuge.transferStackInSlot
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            if (slot instanceof OutputSlot) {
                slot.onTake(player, moved); // SlotCraftingOutput.onSlotChange -> achievement hook
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, CentrifugeBlockEntity.BATTERY_SLOT, CentrifugeBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof MachineUpgradeItem) {
            if (!moveItemStackTo(stack, CentrifugeBlockEntity.UPGRADE_START, CentrifugeBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, CentrifugeBlockEntity.INPUT_SLOT, CentrifugeBlockEntity.INPUT_SLOT + 1, false)) {
            return ItemStack.EMPTY;
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

    public int energy() {
        // [parity K3: centrifuge] power is split into two 16-bit data slots
        return ((this.data.get(8) & 0xFFFF) << 16) | (this.data.get(0) & 0xFFFF);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int processTime() {
        return Math.max(1, this.data.get(3));
    }

    public int consumption() {
        return Math.max(1, this.data.get(4));
    }

    public int speed() {
        return Math.max(1, this.data.get(5));
    }

    public int completedCycles() {
        return this.data.get(6);
    }

    public boolean isWorking() {
        return this.data.get(7) != 0;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / CentrifugeBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CentrifugeBlockEntity centrifuge) {
            return centrifuge;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    /** SlotUpgrade: any ItemMachineUpgrade. */
    private static final class UpgradeSlot extends Slot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof MachineUpgradeItem;
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
}
