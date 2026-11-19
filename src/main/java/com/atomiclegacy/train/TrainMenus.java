package com.atomiclegacy.train;

import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.registry.HbmMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** [parity S1: trains] TrainCargoTram.ContainerTrainCargoTram and TrainCargoTramTrailer.ContainerTrainCargoTramTrailer. */
public final class TrainMenus {
    private TrainMenus() {
    }

    /** FMLNetworkHandler.openGui(player, instance, 0, world, entityId, 0, 0) */
    public static void open(ServerPlayer player, RailCarCargoEntity train) {
        if (train instanceof CargoTramEntity tram) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CargoTram(id, inventory, tram), tram.inventoryTitle()),
                    buffer -> buffer.writeVarInt(tram.getId()));
        } else if (train instanceof CargoTramTrailerEntity trailer) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CargoTramTrailer(id, inventory, trailer), trailer.inventoryTitle()),
                    buffer -> buffer.writeVarInt(trailer.getId()));
        }
    }

    private static Container clientContainer(Inventory inventory, RegistryFriendlyByteBuf buffer, int size) {
        Entity entity = inventory.player.level().getEntity(buffer.readVarInt());
        return entity instanceof RailCarCargoEntity cargo && cargo.getContainerSize() == size ? cargo : new SimpleContainer(size);
    }

    private abstract static class Base extends AbstractContainerMenu {
        protected final Container train;

        protected Base(MenuType<?> type, int id, Container train) {
            super(type, id);
            this.train = train;
        }

        public Container train() {
            return train;
        }

        @Override
        public boolean stillValid(Player player) {
            return train.stillValid(player);
        }
    }

    public static final class CargoTram extends Base {

        public CargoTram(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
            this(id, inventory, clientContainer(inventory, buffer, 29));
        }

        public CargoTram(int id, Inventory invPlayer, Container train) {
            super(HbmMenus.TRAIN_CARGO_TRAM.get(), id, train);

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 7; j++) {
                    this.addSlot(new Slot(train, i * 7 + j, 8 + j * 18, 18 + i * 18));
                }
            }

            this.addSlot(new Slot(train, 28, 152, 72));

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
                }
            }

            for (int i = 0; i < 9; i++) {
                this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 180));
            }
        }

        public int power() {
            return train instanceof RailCarElectricEntity electric ? electric.getPower() : 0;
        }

        public int maxPower() {
            return train instanceof RailCarElectricEntity electric ? electric.getMaxPower() : 1000;
        }

        public int powerConsumption() {
            return train instanceof RailCarElectricEntity electric ? electric.getPowerConsumption() : 10;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            ItemStack stackCopy = ItemStack.EMPTY;
            Slot slot = this.slots.get(slotIndex);

            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                stackCopy = stack.copy();

                if (slotIndex < train.getContainerSize()) {
                    if (!this.moveItemStackTo(stack, train.getContainerSize(), this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (BatteryPackItem.isBattery(stackCopy)) {
                        if (!this.moveItemStackTo(stack, 28, 29, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!this.moveItemStackTo(stack, 0, 28, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }

                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }

            return stackCopy;
        }
    }

    public static final class CargoTramTrailer extends Base {

        public CargoTramTrailer(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
            this(id, inventory, clientContainer(inventory, buffer, 45));
        }

        public CargoTramTrailer(int id, Inventory invPlayer, Container train) {
            super(HbmMenus.TRAIN_CARGO_TRAM_TRAILER.get(), id, train);

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 9; j++) {
                    this.addSlot(new Slot(train, i * 9 + j, 8 + j * 18, 18 + i * 18));
                }
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
                }
            }

            for (int i = 0; i < 9; i++) {
                this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 198));
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            ItemStack stackCopy = ItemStack.EMPTY;
            Slot slot = this.slots.get(slotIndex);

            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                stackCopy = stack.copy();

                if (slotIndex < train.getContainerSize()) {
                    if (!this.moveItemStackTo(stack, train.getContainerSize(), this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, 0, 45, false)) {
                    return ItemStack.EMPTY;
                }

                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }

            return stackCopy;
        }
    }
}
