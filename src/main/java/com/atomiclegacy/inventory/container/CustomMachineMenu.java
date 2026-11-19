package com.atomiclegacy.inventory.container;

import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.registry.HbmMenus;
import com.atomiclegacy.tileentity.CustomMachineBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * [parity X1: custom machines] ContainerMachineCustom: battery 150,72, one fluid ID slot per input tank, the configured
 * input / template / output slots, player inventory at y 174 and hotbar at 232, no shift-click; template slots
 * (SlotPattern) take a one-item copy of the held stack, right click cycles the ModulePatternMatcher mode.
 */
public final class CustomMachineMenu extends AbstractContainerMenu {
    public final CustomMachineBlockEntity custom;

    public CustomMachineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, clientTile(playerInventory, buffer));
    }

    private static CustomMachineBlockEntity clientTile(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        CustomMachineBlockEntity tile = (CustomMachineBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos());
        String type = buffer.readUtf();
        if (tile.config == null || !type.equals(tile.machineType)) {
            tile.machineType = type;
            tile.init();
        }
        return tile;
    }

    public CustomMachineMenu(int containerId, Inventory playerInv, CustomMachineBlockEntity tile) {
        super(HbmMenus.CUSTOM_MACHINE.get(), containerId);
        custom = tile;
        CustomMachineConfigJSON.MachineConfiguration config = tile.config;

        //Input
        this.addSlot(new Slot(tile, 0, 150, 72));
        //Fluid IDs
        for (int i = 0; i < tile.inputTanks.length; i++) {
            this.addSlot(new Slot(tile, 1 + i, 8 + 18 * i, 54));
        }
        //Item inputs
        if (config.itemInCount > 0) this.addSlot(new Slot(tile, 4, 8, 72));
        if (config.itemInCount > 1) this.addSlot(new Slot(tile, 5, 26, 72));
        if (config.itemInCount > 2) this.addSlot(new Slot(tile, 6, 44, 72));
        if (config.itemInCount > 3) this.addSlot(new Slot(tile, 7, 8, 90));
        if (config.itemInCount > 4) this.addSlot(new Slot(tile, 8, 26, 90));
        if (config.itemInCount > 5) this.addSlot(new Slot(tile, 9, 44, 90));
        //Templates
        if (config.itemInCount > 0) this.addSlot(new PatternSlot(tile, 10, 8, 108));
        if (config.itemInCount > 1) this.addSlot(new PatternSlot(tile, 11, 26, 108));
        if (config.itemInCount > 2) this.addSlot(new PatternSlot(tile, 12, 44, 108));
        if (config.itemInCount > 3) this.addSlot(new PatternSlot(tile, 13, 8, 126));
        if (config.itemInCount > 4) this.addSlot(new PatternSlot(tile, 14, 26, 126));
        if (config.itemInCount > 5) this.addSlot(new PatternSlot(tile, 15, 44, 126));
        //Output
        if (config.itemOutCount > 0) this.addSlot(new LegacyAchievementOutputSlot(tile, 16, 78, 72));
        if (config.itemOutCount > 1) this.addSlot(new LegacyAchievementOutputSlot(tile, 17, 96, 72));
        if (config.itemOutCount > 2) this.addSlot(new LegacyAchievementOutputSlot(tile, 18, 114, 72));
        if (config.itemOutCount > 3) this.addSlot(new LegacyAchievementOutputSlot(tile, 19, 78, 90));
        if (config.itemOutCount > 4) this.addSlot(new LegacyAchievementOutputSlot(tile, 20, 96, 90));
        if (config.itemOutCount > 5) this.addSlot(new LegacyAchievementOutputSlot(tile, 21, 114, 90));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 174 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 232));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return custom.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int index, int button, ClickType mode, Player player) {
        if (index < 0 || index >= this.slots.size() || !(this.slots.get(index) instanceof PatternSlot)) {
            super.clicked(index, button, mode, player);
            return;
        }

        Slot slot = this.getSlot(index);
        index = slot.getContainerSlot();

        ItemStack held = getCarried();

        if (button == 1 && mode == ClickType.PICKUP && slot.hasItem()) {
            custom.matcher.nextMode(player.level(), slot.getItem(), index - 10);
        } else {
            slot.set(held);
            custom.matcher.initPatternSmart(player.level(), slot.getItem(), index - 10);
        }
        custom.setChanged();
    }

    /** SlotPattern: cannot be taken, holds a one-item copy. */
    public static final class PatternSlot extends Slot {
        public PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void set(ItemStack stack) {
            super.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
    }
}
