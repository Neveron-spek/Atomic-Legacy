package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.MassStorageBlockEntity;
import com.atomiclegacy.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MassStorageMenu extends AbstractContainerMenu {
    private static final int STORAGE_END = MassStorageBlockEntity.SLOT_COUNT;
    private static final int PLAYER_END = STORAGE_END + 27;
    private final Container container;

    public MassStorageMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()));
    }

    public MassStorageMenu(int id, Inventory inventory, MassStorageBlockEntity storage) {
        this(id, inventory, (Container) storage);
    }

    private MassStorageMenu(int id, Inventory inventory, Container container) {
        super(HbmMenus.MASS_STORAGE.get(), id);
        this.container = container;
        checkContainerSize(container, STORAGE_END);
        addSlot(new Slot(container, 0, 61, 17));
        addSlot(new FilterSlot(container, 1, 61, 53));
        addSlot(new OutputSlot(container, 2, 61, 89));
        addPlayerInventory(inventory, 8, 139);
        if (!inventory.player.level().isClientSide) container.startOpen(inventory.player); // [parity R2] openInventory sound
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) container.stopOpen(player); // [parity R2] closeInventory sound
    }

    /** [parity R2] ContainerMassStorage.slotClick: slot 1 is a SlotPattern ghost slot, locked while the stockpile is non-empty. */
    @Override
    public void clicked(int index, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (index != 1) {
            super.clicked(index, button, clickType, player);
            return;
        }
        if (container instanceof MassStorageBlockEntity storage && storage.stockpile() > 0) return;
        ItemStack held = getCarried();
        container.setItem(1, held.isEmpty() ? ItemStack.EMPTY : held.copy());
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(container instanceof MassStorageBlockEntity storage) || !storage.stillValid(player)) return false;
        if (id == 0 || id == 1) {
            storage.provide(id == 1); // [parity R2] "provide" fills the output slot, not the player inventory
            return true;
        }
        if (id == 2) {
            storage.toggleOutput();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // [parity R2] refill the output slot instantly before regular shift-click behaviour
        if (index == 2 && index < slots.size() && !slots.get(2).hasItem() && container instanceof MassStorageBlockEntity refill) {
            ItemStack extracted = refill.quickExtract();
            if (!extracted.isEmpty()) slots.get(2).set(extracted);
        }
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();
        if (index == 1) return ItemStack.EMPTY;
        if (index == 0 || index == 2) {
            if (!moveItemStackTo(original, STORAGE_END, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(container instanceof MassStorageBlockEntity storage && storage.quickInsert(original))
                && !moveItemStackTo(original, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    public int stockpile() { return container instanceof MassStorageBlockEntity storage ? storage.stockpile() : 0; }
    public int capacity() { return container instanceof MassStorageBlockEntity storage ? storage.capacity() : 1; }
    public boolean output() { return container instanceof MassStorageBlockEntity storage && storage.output(); }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, left + column * 18, top + 58));
    }

    private static Container resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof MassStorageBlockEntity storage ? storage : new SimpleContainer(STORAGE_END);
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return container instanceof MassStorageBlockEntity storage && storage.canPlaceItem(1, stack); }
        @Override public boolean mayPickup(Player player) { return container instanceof MassStorageBlockEntity storage && storage.stockpile() == 0; }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
