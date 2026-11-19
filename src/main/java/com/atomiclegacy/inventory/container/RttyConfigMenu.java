package com.atomiclegacy.inventory.container;

import com.atomiclegacy.blocks.RadioTorchBlock;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
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
import org.jetbrains.annotations.Nullable;

/**
 * Configuration menu for the RTTY torch screens. Slotless for the sender, receiver, logic receiver,
 * reader and controller (1.7.10 GuiScreens); the item counter carries ContainerCounterTorch's three
 * SlotPattern ghost slots at (138, 18 + 44i) and the player inventory at (12, 156) / hotbar 214.
 * [parity R2: sender/receiver/logic/counter GUIs]
 */
public final class RttyConfigMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private final RadioTorchBlock.Kind kind;
    @Nullable
    private final RadioTorchBlockEntity serverRadio;

    public RttyConfigMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readEnum(RadioTorchBlock.Kind.class), null);
    }

    private RttyConfigMenu(int containerId, Inventory inventory, BlockPos blockPos, RadioTorchBlock.Kind kind,
                           @Nullable RadioTorchBlockEntity radio) {
        super(HbmMenus.RTTY_CONFIG.get(), containerId);
        this.blockPos = blockPos.immutable();
        this.kind = kind;
        this.serverRadio = radio;
        if (kind == RadioTorchBlock.Kind.COUNTER) {
            Container patterns = radio == null ? new SimpleContainer(3) : new CounterPatterns(radio);
            for (int i = 0; i < 3; i++) addSlot(new PatternSlot(patterns, i, 138, 18 + 44 * i));
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, 12 + column * 18, 156 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 12 + column * 18, 214));
        }
    }

    public RttyConfigMenu(int containerId, Inventory inventory, RadioTorchBlockEntity radio) {
        this(containerId, inventory, radio.getBlockPos(), radio.kind(), radio);
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public RadioTorchBlock.Kind kind() {
        return kind;
    }

    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (kind != RadioTorchBlock.Kind.COUNTER || index < 0 || index > 2) {
            super.clicked(index, button, clickType, player);
            return;
        }
        if (serverRadio != null) {
            serverRadio.counterPatternClick(index, getCarried(), button == 1 && clickType == ClickType.PICKUP);
            broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(blockPos) instanceof RadioTorchBlockEntity radio
                && radio.kind() == kind
                && kind != RadioTorchBlock.Kind.RELAY
                && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 256.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class PatternSlot extends Slot {
        private PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** Server-side view of the counter's pattern array. */
    private static final class CounterPatterns extends SimpleContainer {
        private final RadioTorchBlockEntity radio;

        private CounterPatterns(RadioTorchBlockEntity radio) {
            super(3);
            this.radio = radio;
        }

        @Override
        public ItemStack getItem(int slot) {
            ItemStack stack = radio.counterFilter(slot);
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }
}
