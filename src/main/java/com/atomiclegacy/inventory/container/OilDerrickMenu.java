package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.OilDerrickBlockEntity;
import com.atomiclegacy.registry.HbmFluids;
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

public class OilDerrickMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = OilDerrickBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;

    public OilDerrickMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(OilDerrickBlockEntity.DATA_COUNT));
    }

    public OilDerrickMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.OIL_DERRICK.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, OilDerrickBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        // [parity e2] ContainerMachineOilWell: 7 visible slots (tile slot 7 exists but has no GUI slot), gui_well.png 184x190
        this.addSlot(new ValidatedSlot(container, OilDerrickBlockEntity.BATTERY_SLOT, 8, 58));
        this.addSlot(new ValidatedSlot(container, OilDerrickBlockEntity.OIL_INPUT_SLOT, 94, 22));
        this.addSlot(new TakeOnlySlot(container, OilDerrickBlockEntity.OIL_OUTPUT_SLOT, 94, 58));
        this.addSlot(new ValidatedSlot(container, OilDerrickBlockEntity.GAS_INPUT_SLOT, 130, 22));
        this.addSlot(new TakeOnlySlot(container, OilDerrickBlockEntity.GAS_OUTPUT_SLOT, 130, 58));
        this.addSlot(new ValidatedSlot(container, OilDerrickBlockEntity.UPGRADE_START, 156, 36));
        this.addSlot(new ValidatedSlot(container, OilDerrickBlockEntity.UPGRADE_START + 1, 156, 54));
        addPlayerInventory(playerInventory, 12, 108);
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

        // [parity e2] ContainerMachineOilWell#transferStackInSlot, bounds kept literally (par2 <= 7; upgrades 5..8 reversed)
        if (index <= 7) {
            if (!moveItemStackTo(stack, 8, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof com.atomiclegacy.items.MachineUpgradeItem) {
            if (!moveItemStackTo(stack, 5, 8, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
            if (!moveItemStackTo(stack, 3, 4, false)) {
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

    public com.atomiclegacy.fluid.HbmFluidDefinition oilFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int oilAmount() {
        return this.data.get(1);
    }

    public int oilCapacity() {
        return Math.max(1, this.data.get(2));
    }

    public com.atomiclegacy.fluid.HbmFluidDefinition gasFluid() {
        return HbmFluids.byOldId(this.data.get(3)).orElse(HbmFluids.none());
    }

    public int gasAmount() {
        return this.data.get(4);
    }

    public int gasCapacity() {
        return Math.max(1, this.data.get(5));
    }

    public int power() {
        return this.data.get(6);
    }

    public int indicator() {
        return this.data.get(7);
    }

    public int maxPower() {
        return Math.max(1, this.data.get(8));
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / maxPower()));
    }

    public int oilScaled(int pixels) {
        return Math.min(pixels, this.oilAmount() * pixels / Math.max(1, oilCapacity()));
    }

    public int gasScaled(int pixels) {
        return Math.min(pixels, this.gasAmount() * pixels / Math.max(1, gasCapacity()));
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
        if (blockEntity instanceof OilDerrickBlockEntity derrick) {
            return derrick;
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
