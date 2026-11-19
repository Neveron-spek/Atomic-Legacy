package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.LegacyBatteryBlockEntity;
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

/** [parity d2] ContainerMachineBattery of 1.7.10. */
public class LegacyBatteryMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = LegacyBatteryBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final BlockPos blockPos;

    public LegacyBatteryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public LegacyBatteryMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.LEGACY_BATTERY.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        this.container = container;
        this.blockPos = blockPosFromContainer(container);
        // ContainerMachineBattery: plain slots
        this.addSlot(new Slot(container, 0, 26, 17));
        this.addSlot(new Slot(container, 1, 26, 53));
        addPlayerInventory(playerInventory, 8, 84);
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
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    @org.jetbrains.annotations.Nullable
    public LegacyBatteryBlockEntity battery() {
        return this.container instanceof LegacyBatteryBlockEntity socket ? socket : null;
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
        if (blockEntity instanceof LegacyBatteryBlockEntity socket) {
            return socket;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static BlockPos blockPosFromContainer(Container container) {
        return container instanceof LegacyBatteryBlockEntity socket ? socket.getBlockPos() : BlockPos.ZERO;
    }
}
