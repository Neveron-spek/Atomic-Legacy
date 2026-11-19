package com.atomiclegacy.tileentity;

import com.atomiclegacy.entity.MinerRocketEntity;
import com.atomiclegacy.items.SatelliteChipItem;
import com.atomiclegacy.inventory.container.SatelliteDockMenu;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.satellite.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exact inventory, timing and cargo-table port of TileEntityMachineSatDock. */
public final class SatelliteDockBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int OUTPUT_SLOT_COUNT = 15;
    public static final int SLOT_SATELLITE = 15;
    public static final int SLOT_COUNT = 16;
    private static final int[] ACCESSIBLE_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
    };
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public SatelliteDockBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SAT_DOCK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SatelliteDockBlockEntity dock) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // [parity P2: satellites] TileEntityMachineSatDock: every 20 ticks offset by the
        // faux-1.12 BlockPos identity, only a plain sat_chip, any satellite with
        // requestable cargo (miners and the space laboratory's 0G assembler).
        ItemStack chip = dock.items.get(SLOT_SATELLITE);
        long time = level.getGameTime() + ((pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX());
        if (time % 20L != 0L || !chip.is(com.atomiclegacy.registry.HbmItems.SAT_CHIP.get())) {
            return;
        }
        int frequency = SatelliteChipItem.frequency(chip);
        SatelliteSavedData.SatelliteRecord satellite = SatelliteSavedData.get(serverLevel).satellite(frequency).orElse(null);
        if (satellite != null && !satellite.requestableItems().isEmpty()) {
            List<ItemStack> cargo = satellite.requestableItems();
            MinerRocketEntity rocket = new MinerRocketEntity(level, frequency, pos.getY(), cargo);
            rocket.setPos(pos.getX() + 0.5D, 300.0D, pos.getZ() + 0.5D);
            if (level.addFreshEntity(rocket)) {
                satellite.takeRequestableItems(serverLevel);
            }
        }
    }

    /** Called by the landed delivery pod. Returns all cargo that did not fit. */
    public List<ItemStack> acceptCargo(List<ItemStack> cargo) {
        List<ItemStack> remainder = new java.util.ArrayList<>();
        for (ItemStack supplied : cargo) {
            ItemStack left = addToOutput(supplied.copy());
            if (!left.isEmpty()) remainder.add(left);
        }
        setChanged();
        return remainder;
    }

    private ItemStack addToOutput(ItemStack stack) {
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT && !stack.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int accepted = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (accepted > 0) {
                    existing.grow(accepted);
                    stack.shrink(accepted);
                }
            }
        }
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT && !stack.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                items.set(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }
        return stack;
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SLOT_COUNT ? items.get(slot) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_SATELLITE && stack.getItem() instanceof SatelliteChipItem;
    }
    @Override public int[] getSlotsForFace(Direction side) { return ACCESSIBLE_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 0 && slot < OUTPUT_SLOT_COUNT; }
    @Override public boolean stillValid(Player player) { return MachineDummyMenuAccess.stillValid(this, player); }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.atomiclegacy.sat_dock");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SatelliteDockMenu(containerId, inventory, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

}
