package com.atomiclegacy.inventory.container;

import com.atomiclegacy.entity.cart.MinecartDestroyerEntity;
import com.atomiclegacy.registry.HbmMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * [parity S1: carts] ContainerCartDestroyer: two 3x3 SlotPattern filter grids (exact match left, item-only right).
 * Every click on a filter slot copies one of the held items into it (or clears it); shift-click does nothing.
 */
public class CartDestroyerMenu extends AbstractContainerMenu {
    private final Container cart;

    public CartDestroyerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, clientCart(inventory, buffer));
    }

    private static Container clientCart(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Entity entity = inventory.player.level().getEntity(buffer.readVarInt());
        return entity instanceof MinecartDestroyerEntity destroyer ? destroyer : new SimpleContainer(18);
    }

    public CartDestroyerMenu(int id, Inventory invPlayer, Container tedf) {
        super(HbmMenus.CART_DESTROYER.get(), id);
        this.cart = tedf;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new PatternSlot(tedf, j + i * 3, 10 + j * 18, 17 + i * 18));
                this.addSlot(new PatternSlot(tedf, j + i * 3 + 9, 114 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return cart.stillValid(player);
    }

    @Override
    public void clicked(int index, int button, ClickType mode, Player player) {
        if (index < 0 || index >= cart.getContainerSize()) {
            super.clicked(index, button, mode, player);
            return;
        }
        Slot slot = this.getSlot(index);
        slot.set(this.getCarried());
    }

    /** SlotPattern: ghost slot holding a one-item copy, never taken out. */
    private static final class PatternSlot extends Slot {
        private PatternSlot(Container container, int index, int x, int y) {
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
            if (!stack.isEmpty()) {
                stack = stack.copyWithCount(1);
            }
            super.set(stack);
        }
    }
}
