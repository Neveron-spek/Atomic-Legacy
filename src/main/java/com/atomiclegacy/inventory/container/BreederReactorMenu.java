package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.BreederReactorBlockEntity;
import com.atomiclegacy.inventory.recipes.BreederReactorRecipe;
import com.atomiclegacy.registry.HbmMenus;
import com.atomiclegacy.registry.HbmRecipeTypes;
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

public class BreederReactorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = BreederReactorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public BreederReactorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(BreederReactorBlockEntity.DATA_COUNT));
    }

    public BreederReactorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.BREEDER_REACTOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, BreederReactorBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, BreederReactorBlockEntity.INPUT_SLOT, 35, 35));
        this.addSlot(new OutputSlot(container, BreederReactorBlockEntity.OUTPUT_SLOT, 125, 35));
        addPlayerInventory(playerInventory, 8, 84);
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

        // [parity e2] ContainerMachineReactorBreeding.transferStackInSlot: index <= 2 -> 2..size (reverse),
        // otherwise a breeding rod -> slot 0, anything else stays put. [parity h1] ItemBreedingRod = rod / rod_dual /
        // rod_quad (every variant, also outputs; the etched meteorite sword is not shift-clicked).
        if (index <= 2) {
            if (!moveItemStackTo(stack, 2, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (isBreedingRod(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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

    public int flux() {
        return this.data.get(0);
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.data.get(1) * pixels / BreederReactorBlockEntity.PROGRESS_SCALE);
    }

    public int requiredFlux() {
        return this.data.get(2);
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

    private static boolean isBreedingRod(ItemStack stack) {
        return stack.is(com.atomiclegacy.registry.HbmItems.ROD.get()) || stack.is(com.atomiclegacy.registry.HbmItems.ROD_DUAL.get())
                || stack.is(com.atomiclegacy.registry.HbmItems.ROD_QUAD.get());
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BreederReactorBlockEntity breeder) {
            return breeder;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
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
