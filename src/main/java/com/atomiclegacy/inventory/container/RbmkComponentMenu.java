package com.atomiclegacy.inventory.container;

import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import com.atomiclegacy.items.RbmkFuelRodItem;
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

public class RbmkComponentMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;
    private final RbmkComponentBlock.Kind kind;
    private final BlockPos pos;
    private int machineSlots;

    public RbmkComponentMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readContext(playerInventory, buffer));
    }

    private RbmkComponentMenu(int containerId, Inventory playerInventory, Context context) {
        this(containerId, playerInventory, context.container(), context.data(), context.pos());
    }

    public RbmkComponentMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos pos) {
        super(HbmMenus.RBMK_COMPONENT.get(), containerId);
        checkContainerSize(container, RbmkComponentBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, RbmkComponentBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;
        this.pos = pos;
        this.kind = container instanceof RbmkComponentBlockEntity rbmk ? rbmk.kind() : RbmkComponentBlock.Kind.BLANK;

        addMachineSlots();
        if (kind != RbmkComponentBlock.Kind.CONSOLE) {
            addPlayerInventory(playerInventory, 8, kind == RbmkComponentBlock.Kind.AUTOLOADER ? 100 : 104);
        }
        addDataSlots(data);
    }

    private void addMachineSlots() {
        if (kind.acceptsFuel()) {
            addSlot(new FuelSlot(container, RbmkComponentBlockEntity.SLOT_FUEL, 80, 45));
            machineSlots = 1;
            return;
        }
        if (kind == RbmkComponentBlock.Kind.HEATER) {
            addSlot(new Slot(container, 0, 41, 45));
            machineSlots = 1;
            return;
        }
        if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
            addSlot(new Slot(container, 0, 48, 45));
            addSlot(new OutputSlot(container, 1, 112, 69));
            machineSlots = 2;
            return;
        }
        if (kind == RbmkComponentBlock.Kind.STORAGE) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 4; column++) {
                    addSlot(new FuelSlot(container, row + column * 3, 32 + 32 * column, 29 + 16 * row));
                }
            }
            machineSlots = 12;
            return;
        }
        if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    addSlot(new FuelSlot(container, RbmkComponentBlockEntity.AUTOLOADER_INPUT_START + column + row * 3, 17 + column * 18, 18 + row * 18));
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    addSlot(new OutputSlot(container, RbmkComponentBlockEntity.AUTOLOADER_OUTPUT_START + column + row * 3, 107 + column * 18, 18 + row * 18));
                }
            }
            machineSlots = 18;
        }
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // ContainerRBMKControl and ContainerRBMKControlAuto in 1.7.10 had
        // no machine slots and deliberately returned null from
        // transferStackInSlot; shift-clicking therefore did not move items
        // between the player's inventory and hotbar.
        if (kind.isControl()) {
            return ItemStack.EMPTY;
        }
        // [parity e2] ContainerRBMKGeneric (boiler and other slotless columns)
        // returned null for every shift-click.
        if (machineSlots <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        int playerStart = machineSlots;
        int hotbarEnd = playerStart + 36;

        // [parity e2] ContainerRBMKOutgasser only treated slot 0 as a machine
        // slot (par2 == 0); a shift-click on the output slot 1 fell into the
        // "merge into 0..1" branch like any player slot.
        boolean fromMachine = kind == RbmkComponentBlock.Kind.OUTGASSER ? index == 0 : index < machineSlots;
        if (fromMachine) {
            // [parity e2] ContainerRBMKRod.transferStackInSlot: hot fuel stays put outside creative.
            if (kind.acceptsFuel() && hotFuelLocked(player)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, playerStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // [parity e2] The 1.7.10 containers only merged into the machine
            // range (rod/heater 0..size, outgasser 0..1, storage 0..12,
            // autoloader 0..9) and returned null otherwise; there was no
            // inventory <-> hotbar fallback.
            int machineEnd = kind == RbmkComponentBlock.Kind.OUTGASSER ? 1
                    : kind == RbmkComponentBlock.Kind.AUTOLOADER ? 9 : machineSlots;
            if (!canMoveToMachine(stack) || !moveItemStackTo(stack, 0, machineEnd, false)) {
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

    /** [parity e2] TileEntityRBMKRod.coldEnoughForManual() negated, ignored in creative like ContainerRBMKRod. */
    private boolean hotFuelLocked(Player player) {
        if (player.getAbilities().instabuild) {
            return false;
        }
        ItemStack fuel = container.getItem(RbmkComponentBlockEntity.SLOT_FUEL);
        return fuel.getItem() instanceof RbmkFuelRodItem && RbmkFuelRodItem.hullHeat(fuel) > 200;
    }

    /** [parity e2] ContainerRBMKRod.slotClick: slot 0 ignores every click while the fuel skin is above 200 degrees. */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (slotId == 0 && kind.acceptsFuel() && hotFuelLocked(player)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private boolean canMoveToMachine(ItemStack stack) {
        if (machineSlots <= 0) {
            return false;
        }
        if (kind.acceptsFuel() || kind == RbmkComponentBlock.Kind.STORAGE || kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            return stack.getItem() instanceof RbmkFuelRodItem;
        }
        return kind == RbmkComponentBlock.Kind.HEATER || kind == RbmkComponentBlock.Kind.OUTGASSER;
    }

    @Override
    public boolean stillValid(Player player) {
        // The legacy control containers deliberately overrode
        // canInteractWith to return true, so their GUI stayed open regardless
        // of player distance.  The legacy console exposed a GuiScreen (its
        // provideContainer returned null), which likewise had no container
        // distance check.  Keep those two menu-backed modern screens from
        // applying the generic 8-block AbstractContainerMenu rule.
        if (kind.isControl() || kind == RbmkComponentBlock.Kind.CONSOLE) {
            return true;
        }
        return container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(container instanceof RbmkComponentBlockEntity rbmk)) {
            return false;
        }
        if (id == 3 && rbmk.kind() == RbmkComponentBlock.Kind.BOILER) {
            data.set(6, (data.get(6) + 1) & 3);
            return true;
        }
        if (id == 4 && rbmk.kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            data.set(13, Math.max(5, data.get(13) - 5));
            return true;
        }
        if (id == 5 && rbmk.kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            data.set(13, Math.min(95, data.get(13) + 5));
            return true;
        }
        return false;
    }

    public RbmkComponentBlock.Kind kind() {
        return kind;
    }

    public BlockPos pos() {
        return pos;
    }

    public int heat() {
        return data.get(0);
    }

    public int flux() {
        return data.get(1);
    }

    public int controlLevel() {
        return data.get(2);
    }

    public int targetControlLevel() {
        return data.get(3);
    }

    public int water() {
        return data.get(4);
    }

    public int steam() {
        return data.get(5);
    }

    public int steamCompression() {
        return data.get(6);
    }

    public int heaterInput() {
        return data.get(8);
    }

    public int heaterOutput() {
        return data.get(9);
    }

    public int outgasserGas() {
        return data.get(10);
    }

    public int outgasserProgress() {
        return data.get(11);
    }

    public int autoloaderPiston() {
        return data.get(12);
    }

    public int autoloaderCycle() {
        return data.get(13);
    }

    public int autoLevelUpper() {
        return data.get(15);
    }

    public int autoLevelLower() {
        return data.get(16);
    }

    public int autoHeatUpper() {
        return data.get(17);
    }

    public int autoHeatLower() {
        return data.get(18);
    }

    public int autoFunction() {
        return data.get(19);
    }

    public int colorGroup() {
        return data.get(20);
    }

    private static Context readContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        return new Context(getContainer(playerInventory, pos), new SimpleContainerData(RbmkComponentBlockEntity.DATA_COUNT), pos);
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk) {
            return rbmk;
        }
        return new SimpleContainer(RbmkComponentBlockEntity.SLOT_COUNT);
    }

    private record Context(Container container, ContainerData data, BlockPos pos) {
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof RbmkFuelRodItem && this.container.canPlaceItem(this.index, stack);
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
