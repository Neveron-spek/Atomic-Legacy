package com.atomiclegacy.inventory.container;

import com.atomiclegacy.tileentity.LegacyCrate;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import com.atomiclegacy.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** [parity R4: crates] ContainerCrateBase + ContainerCrateIron/Steel/Desh/Tungsten/Safe (placed or held). */
public class StorageCrateMenu extends AbstractContainerMenu {
    private final Container container;
    private final StorageCrateBlockEntity.Kind kind;
    private final int crateSlotCount;
    private final boolean held;
    private final int lockedHotbarSlot;

    public StorageCrateMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, fromBuffer(playerInventory, buffer));
    }

    private StorageCrateMenu(int containerId, Inventory playerInventory, Container container) {
        this(containerId, playerInventory, container, container instanceof HeldCrateContainer);
    }

    public StorageCrateMenu(int containerId, Inventory playerInventory, LegacyCrate crate) {
        this(containerId, playerInventory, crate, false);
    }

    // [parity S1: crate cart] EntityMinecartCrate opens ContainerCrateSteel on the entity inventory
    public static StorageCrateMenu cart(int containerId, Inventory playerInventory, com.atomiclegacy.entity.cart.MinecartCrateEntity cart) {
        return new StorageCrateMenu(containerId, playerInventory, cart, false);
    }

    // [parity S1: crate cart]
    public static void writeCart(RegistryFriendlyByteBuf buffer, com.atomiclegacy.entity.cart.MinecartCrateEntity cart) {
        buffer.writeByte(2);
        buffer.writeVarInt(cart.getId());
    }

    public static StorageCrateMenu held(int containerId, Inventory playerInventory, StorageCrateBlockEntity.Kind kind) {
        return new StorageCrateMenu(containerId, playerInventory, new HeldCrateContainer(playerInventory.player, kind), true);
    }

    private StorageCrateMenu(int containerId, Inventory playerInventory, Container container, boolean held) {
        super(HbmMenus.STORAGE_CRATE.get(), containerId);
        this.container = container;
        this.held = held;
        if (container instanceof LegacyCrate crate) {
            this.kind = crate.kind();
        } else if (container instanceof HeldCrateContainer heldCrate) {
            this.kind = heldCrate.kind();
        } else if (container instanceof com.atomiclegacy.entity.cart.MinecartCrateEntity) { // [parity S1: crate cart] ContainerCrateSteel
            this.kind = StorageCrateBlockEntity.Kind.STEEL;
        } else {
            this.kind = StorageCrateBlockEntity.Kind.IRON;
        }
        this.crateSlotCount = this.kind.slots();
        this.lockedHotbarSlot = held ? playerInventory.selected : -1;
        checkContainerSize(container, this.crateSlotCount);
        for (int row = 0; row < this.kind.rows(); row++) {
            for (int column = 0; column < this.kind.columns(); column++) {
                int slot = column + row * this.kind.columns();
                this.addSlot(new Slot(container, slot, this.kind.crateX() + column * 18, this.kind.crateY() + row * 18));
            }
        }
        addPlayerInventory(playerInventory);
        container.startOpen(playerInventory.player);
    }

    /** ContainerBase#transferStackInSlot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < this.crateSlotCount) {
            if (!moveItemStackTo(stack, this.crateSlotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, this.crateSlotCount, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return moved;
    }

    /** ContainerCrateBase#slotClick: the open held crate cannot be moved. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // [parity S1: crate cart] a non-tile crate inventory also locks a held storage crate item in the selected slot
        if (this.container instanceof com.atomiclegacy.entity.cart.MinecartCrateEntity && isStorageCrateItem(player.getInventory().getSelected())) {
            if (clickType == ClickType.SWAP && button == player.getInventory().selected) {
                return;
            }
            if (slotId == player.getInventory().selected + 27 + this.crateSlotCount) {
                return;
            }
        }
        if (this.held) {
            if (clickType == ClickType.SWAP && button == this.lockedHotbarSlot) {
                return;
            }
            if (slotId == this.lockedHotbarSlot + 27 + this.crateSlotCount) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    // [parity S1: crate cart] ItemBlockStorageCrate: crate_iron, crate_steel, crate_desh, crate_tungsten, safe, mass_storage
    private static boolean isStorageCrateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return java.util.Set.of("crate_iron", "crate_steel", "crate_desh", "crate_tungsten", "safe", "mass_storage").contains(id);
    }

    public StorageCrateBlockEntity.Kind kind() {
        return this.kind;
    }

    /** GUICrateTungsten: hot texture only for a placed crate while heated (metadata 1). */
    public boolean hot() {
        return this.container instanceof StorageCrateBlockEntity crate && crate.heatTimer() > 0;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, this.kind.playerInventoryX() + column * 18, this.kind.playerInventoryY() + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(column == this.lockedHotbarSlot
                    ? new LockedSlot(inventory, column, this.kind.playerInventoryX() + column * 18, this.kind.hotbarY())
                    : new Slot(inventory, column, this.kind.playerInventoryX() + column * 18, this.kind.hotbarY()));
        }
    }

    private static Container fromBuffer(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        byte mode = buffer.readByte(); // [parity S1: crate cart] 2 = crate cart entity id
        if (mode == 2) {
            net.minecraft.world.entity.Entity entity = playerInventory.player.level().getEntity(buffer.readVarInt());
            return entity instanceof com.atomiclegacy.entity.cart.MinecartCrateEntity cart ? cart : new SimpleContainer(StorageCrateBlockEntity.Kind.STEEL.slots());
        }
        if (mode == 1) {
            int ordinal = Byte.toUnsignedInt(buffer.readByte());
            StorageCrateBlockEntity.Kind[] kinds = StorageCrateBlockEntity.Kind.values();
            return new HeldCrateContainer(playerInventory.player, ordinal < kinds.length ? kinds[ordinal] : StorageCrateBlockEntity.Kind.IRON);
        }
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof LegacyCrate crate) {
            return crate;
        }
        return new SimpleContainer(StorageCrateBlockEntity.Kind.IRON.slots());
    }

    /** ContainerCrateBase.SlotPlayerCrateLocked. */
    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
