package com.atomiclegacy.inventory.container;

import com.atomiclegacy.registry.HbmMenus;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
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
 * [parity R2] ContainerPneumoTube: 15 SlotPattern ghost slots at (35 + j * 18, 17 + i * 18), player inventory
 * at (8, 103) / hotbar 161. Shift-click does nothing. Button ids: 0 whitelist, 1 redstone, 2 pressure,
 * 3 send order, 4 receive order.
 */
public final class PneumaticTubeMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final boolean endpointOnly;
    @Nullable private final PneumaticTubeBlockEntity tube;

    public PneumaticTubeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), buffer.readBoolean(), null);
    }

    public PneumaticTubeMenu(int id, Inventory inventory, PneumaticTubeBlockEntity tube, boolean endpointOnly) {
        this(id, inventory, tube.getBlockPos(), endpointOnly, tube);
    }

    private PneumaticTubeMenu(int id, Inventory inventory, BlockPos pos, boolean endpointOnly, @Nullable PneumaticTubeBlockEntity tube) {
        super(HbmMenus.PNEUMATIC_TUBE.get(), id);
        this.pos = pos.immutable();
        this.endpointOnly = endpointOnly;
        this.tube = tube;
        Container patterns = tube == null ? new SimpleContainer(PneumaticTubeBlockEntity.FILTER_SLOTS) : new Patterns(tube);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) addSlot(new PatternSlot(patterns, i * 5 + j, 35 + j * 18, 17 + i * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 161));
    }

    public BlockPos pos() {
        return pos;
    }

    public boolean endpointOnly() {
        return endpointOnly;
    }

    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index < 0 || index >= PneumaticTubeBlockEntity.FILTER_SLOTS) {
            super.clicked(index, button, clickType, player);
            return;
        }
        if (tube != null) {
            tube.patternClick(index, getCarried(), button == 1 && clickType == ClickType.PICKUP);
            broadcastChanges();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (tube == null) return false;
        if (endpointOnly && id != 0) return false;
        tube.handleButton(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof PneumaticTubeBlockEntity entity && entity.stillValid(player);
    }

    private static final class PatternSlot extends Slot {
        private PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    private static final class Patterns extends SimpleContainer {
        private final PneumaticTubeBlockEntity tube;

        private Patterns(PneumaticTubeBlockEntity tube) {
            super(PneumaticTubeBlockEntity.FILTER_SLOTS);
            this.tube = tube;
        }

        @Override
        public ItemStack getItem(int slot) {
            return tube.filter(slot);
        }
    }
}
