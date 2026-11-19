package com.atomiclegacy.inventory.container;

import com.atomiclegacy.items.LegacyAmmoBagItem;
import com.atomiclegacy.registry.HbmMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * [parity b1: ammo bag] 1.7.10 ContainerAmmoBag (2x4 slots at 53/18, player inventory at y 82/140) and
 * ContainerCasingBag (3x5 slots at 44/18, player inventory at y 100/158). The slot holding the open bag is locked.
 */
public final class LegacyAmmoBagMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final LegacyAmmoBagItem.Kind kind;
    private final LegacyAmmoBagItem.BagInventory bag;

    public LegacyAmmoBagMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory,
                buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                LegacyAmmoBagItem.Kind.values()[Byte.toUnsignedInt(buffer.readByte())]);
    }

    public LegacyAmmoBagMenu(int containerId, Inventory inventory, InteractionHand hand, LegacyAmmoBagItem.Kind kind) {
        super(HbmMenus.AMMO_BAG.get(), containerId);
        this.hand = hand;
        this.kind = kind;
        this.bag = new LegacyAmmoBagItem.BagInventory(inventory.player.getItemInHand(hand), inventory.player.registryAccess(), kind);

        boolean ammo = kind.ammo();
        int rows = ammo ? 2 : 3;
        int columns = ammo ? 4 : 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                addSlot(new BagSlot(bag, j + i * columns, (ammo ? 53 : 44) + j * 18, 18 + i * 18));
            }
        }
        int inventoryY = ammo ? 82 : 100;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, inventoryY + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, inventoryY + 58));
        }
    }

    public LegacyAmmoBagItem.Kind kind() { return kind; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        ItemStack stack = source.getItem();
        ItemStack result = stack.copy();
        int size = bag.getContainerSize();
        if (index <= size - 1) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, size, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        source.onTake(player, stack);
        return result;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // prevents the player from moving around the currently open box
        int heldSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        if (clickType == ClickType.SWAP && (button == heldSlot || (hand == InteractionHand.OFF_HAND && button == Inventory.SLOT_OFFHAND))) return;
        if (heldSlot >= 0 && slotId == heldSlot + 27 + bag.getContainerSize()) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof LegacyAmmoBagItem item && item.kind() == kind;
    }

    /** SlotNonRetarded: validity comes from isItemValidForSlot of the inventory. */
    private static final class BagSlot extends Slot {
        private BagSlot(LegacyAmmoBagItem.BagInventory bag, int index, int x, int y) {
            super(bag, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
