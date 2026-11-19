package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.CyclotronBlockEntity;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.MachineUpgradeItem;
import com.atomiclegacy.inventory.recipes.CyclotronRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CyclotronMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CyclotronBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public CyclotronMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CyclotronBlockEntity.DATA_COUNT));
    }

    public CyclotronMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CYCLOTRON.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CyclotronBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.PARTICLE_A_SLOT, 11, 18));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.PARTICLE_B_SLOT, 11, 36));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.PARTICLE_C_SLOT, 11, 54));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.INPUT_A_SLOT, 101, 18));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.INPUT_B_SLOT, 101, 36));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.INPUT_C_SLOT, 101, 54));
        addSlot(new OutputSlot(container, CyclotronBlockEntity.OUTPUT_A_SLOT, 131, 18));
        addSlot(new OutputSlot(container, CyclotronBlockEntity.OUTPUT_B_SLOT, 131, 36));
        addSlot(new OutputSlot(container, CyclotronBlockEntity.OUTPUT_C_SLOT, 131, 54));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.BATTERY_SLOT, 168, 83));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.UPGRADE_A_SLOT, 60, 81));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.UPGRADE_B_SLOT, 78, 81));

        addPlayerInventory(playerInventory, 15, 133);
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

        // [parity e2] ContainerMachineCyclotron.transferStackInSlot verbatim (index <= 15 quirk included):
        // battery -> 9, upgrade -> 10 then 11, particle parts -> 0..3, everything else -> 3..6, all reverse.
        if (index <= 15) {
            if (!moveItemStackTo(stack, 16, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, 9, 10, true)) return ItemStack.EMPTY;
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, 10, 11, true)) {
                if (!moveItemStackTo(stack, 11, 12, true)) return ItemStack.EMPTY;
            }
        } else if (isParticle(stack)) {
            if (!moveItemStackTo(stack, 0, 3, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 3, 6, true)) return ItemStack.EMPTY;
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

    public int power() { return data.get(0); }
    public int progress() { return data.get(1); }
    public int plugs() { return data.get(2); }
    public int waterAmount() { return data.get(3); }
    public int spentSteamAmount() { return data.get(4); }
    public int antimatterAmount() { return data.get(5); }
    public int waterCapacity() { return Math.max(1, data.get(6)); }
    public int spentSteamCapacity() { return Math.max(1, data.get(7)); }
    public int antimatterCapacity() { return Math.max(1, data.get(8)); }
    public int consumption() { return data.get(9); }
    public boolean active() { return data.get(10) != 0; }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (power() * (long) pixels / CyclotronBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, progress() * pixels / CyclotronBlockEntity.DURATION);
    }

    public int tankScaled(int amount, int capacity, int pixels) {
        return Math.min(pixels, amount * pixels / Math.max(1, capacity));
    }

    private boolean isParticle(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.particle().test(stack));
    }

    private boolean hasTargetRecipe(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.input().test(stack));
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
        return blockEntity instanceof CyclotronBlockEntity cyclotron ? cyclotron : new SimpleContainer(MACHINE_SLOT_COUNT);
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
