package com.atomiclegacy.inventory.container;

import com.atomiclegacy.registry.HbmMenus;
import com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** [parity P2: soyuz_launcher] ContainerSoyuzLauncher: plain slots, shift-click from the player only into slot 0. */
public final class SoyuzLauncherMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public SoyuzLauncherMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, clientContainer(inventory, buffer.readBlockPos()),
                new SimpleContainerData(SoyuzLauncherBlockEntity.DATA_COUNT));
    }

    public SoyuzLauncherMenu(int id, Inventory inventory, SoyuzLauncherBlockEntity launcher, ContainerData data) {
        super(HbmMenus.SOYUZ_LAUNCHER.get(), id);
        this.container = launcher;
        this.data = data;
        this.blockPos = launcher.getBlockPos();
        checkContainerSize(launcher, SoyuzLauncherBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, SoyuzLauncherBlockEntity.DATA_COUNT);
        addSlot(new Slot(launcher, 0, 98, 80));
        addSlot(new Slot(launcher, 1, 80, 80));
        addSlot(new Slot(launcher, 2, 98, 26));
        addSlot(new Slot(launcher, 3, 80, 26));
        addSlot(new Slot(launcher, 4, 152, 98));
        addSlot(new Slot(launcher, 5, 152, 116));
        addSlot(new Slot(launcher, 6, 170, 98));
        addSlot(new Slot(launcher, 7, 170, 116));
        addSlot(new Slot(launcher, 8, 134, 98));
        for (int i = 0; i < 3; i++) for (int j = 0; j < 6; j++)
            addSlot(new Slot(launcher, j + i * 6 + 9, 44 - i * 18, 26 + j * 18));
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            addSlot(new Slot(inventory, j + i * 9 + 9, 17 + j * 18, 162 + i * 18));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 17 + i * 18, 220));
        addDataSlots(data);
    }

    private static SoyuzLauncherBlockEntity clientContainer(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof SoyuzLauncherBlockEntity launcher) return launcher;
        throw new IllegalStateException("No soyuz_launcher at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SoyuzLauncherBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() { return this.blockPos; }
    /** GUISoyuzLauncher reads the tile directly; slots arrive through the menu, the rest through the block update tag. */
    public SoyuzLauncherBlockEntity launcher() { return (SoyuzLauncherBlockEntity) this.container; }
    public long power() { return Integer.toUnsignedLong(this.data.get(0)); }
    public int mode() { return this.data.get(1); }
    public boolean starting() { return this.data.get(2) != 0; }
    public int countdown() { return this.data.get(3); }
    public int kerosene() { return this.data.get(4); }
    public int oxygen() { return this.data.get(5); }
    public boolean hasRocket() { return this.data.get(6) != 0; }
    public int designator() { return this.data.get(7); }
    public int orbital() { return this.data.get(8); }
    public int satellite() { return this.data.get(9); }
    public int fuelRequired() { return this.data.get(10); }
}
