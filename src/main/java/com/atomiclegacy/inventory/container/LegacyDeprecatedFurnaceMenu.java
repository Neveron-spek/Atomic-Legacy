package com.atomiclegacy.inventory.container;

import com.atomiclegacy.blocks.LegacyDeprecatedFurnaceBlock.Kind;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.registry.HbmMenus;
import com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** [parity d2] ContainerDiFurnace, ContainerMachineDiFurnaceRTG and ContainerRtgFurnace of 1.7.10. */
public final class LegacyDeprecatedFurnaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final Kind kind;
    private final BlockPos pos;
    private final int machineSlots;

    public LegacyDeprecatedFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos(), Kind.values()[Math.floorMod(buffer.readVarInt(), Kind.values().length)]);
    }

    private LegacyDeprecatedFurnaceMenu(int containerId, Inventory playerInventory, BlockPos pos, Kind kind) {
        this(containerId, playerInventory, getContainer(playerInventory, pos, kind),
                new SimpleContainerData(LegacyDeprecatedFurnaceBlockEntity.DATA_COUNT), kind, pos);
    }

    public LegacyDeprecatedFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, Kind kind, BlockPos pos) {
        super(HbmMenus.LEGACY_DEPRECATED_FURNACE.get(), containerId);
        checkContainerSize(container, kind.slots());
        checkContainerDataCount(data, LegacyDeprecatedFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.kind = kind;
        this.pos = pos;
        this.machineSlots = kind.slots();
        switch (kind) {
            case DIFURNACE -> {
                addSlot(new Slot(container, 0, 80, 18));
                addSlot(new Slot(container, 1, 80, 54));
                addSlot(new Slot(container, 2, 8, 36));
                addSlot(new OutputSlot(container, 3, 134, 36));
            }
            case DIFURNACE_RTG -> {
                addSlot(new Slot(container, 0, 80, 18));
                addSlot(new Slot(container, 1, 80, 54));
                addSlot(new OutputSlot(container, 2, 134, 36));
                addSlot(new Slot(container, 3, 22, 18));
                addSlot(new Slot(container, 4, 40, 18));
                addSlot(new Slot(container, 5, 22, 36));
                addSlot(new Slot(container, 6, 40, 36));
                addSlot(new Slot(container, 7, 22, 54));
                addSlot(new Slot(container, 8, 40, 54));
            }
            case RTG_FURNACE -> {
                addSlot(new Slot(container, 0, 56, 17));
                addSlot(new Slot(container, 1, 38, 53));
                addSlot(new Slot(container, 2, 56, 53));
                addSlot(new Slot(container, 3, 74, 53));
                addSlot(new OutputSlot(container, 4, 116, 35));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public Kind kind() {
        return this.kind;
    }

    public int progress() {
        return this.data.get(0);
    }

    /** Blast furnace: fuel; nuclear blast furnace: heat; RTG furnace: 1 when a pellet is loaded. */
    public int power() {
        return this.data.get(1);
    }

    public int side(int slot) {
        return this.kind == Kind.DIFURNACE
                ? this.data.get(slot == 0 ? 3 : slot == 1 ? 4 : 2)
                : this.data.get(slot == 0 ? 3 : 4);
    }

    public boolean canProcess() {
        return this.data.get(5) != 0;
    }

    /** Number of leading input slots whose insertion side can be cycled. */
    public int sidedSlots() {
        return this.kind == Kind.DIFURNACE ? 3 : this.kind == Kind.DIFURNACE_RTG ? 2 : 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < sidedSlots() && button == 1 && clickType == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotId);
            if (!slot.hasItem() && getCarried().isEmpty()) {
                if (!player.level().isClientSide && this.container instanceof LegacyDeprecatedFurnaceBlockEntity furnace) {
                    furnace.cycleSide(slotId);
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        int end = this.slots.size();
        if (index < this.machineSlots) {
            if (!moveItemStackTo(stack, this.machineSlots, end, true)) return ItemStack.EMPTY;
        } else {
            boolean ok = switch (this.kind) {
                case DIFURNACE -> moveItemStackTo(stack, 0, 3, false);
                case DIFURNACE_RTG -> stack.getItem() instanceof RtgPelletItem
                        ? moveItemStackTo(stack, 3, 9, false)
                        : moveItemStackTo(stack, 0, 3, false);
                case RTG_FURNACE -> moveItemStackTo(stack, 0, 4, false);
            };
            if (!ok) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos pos() {
        return this.pos;
    }

    private static Container getContainer(Inventory inventory, BlockPos pos, Kind kind) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyDeprecatedFurnaceBlockEntity furnace && furnace.kind() == kind ? furnace : new SimpleContainer(kind.slots());
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
