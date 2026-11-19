package com.atomiclegacy.tileentity;

import com.atomiclegacy.items.SirenTrackItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SirenBlockEntity extends BlockEntity implements WorldlyContainer, MachineInventory, MenuProvider {
    public static final int SLOT = 0;
    private static final int[] SLOTS = {SLOT};
    private ItemStack track = ItemStack.EMPTY;
    private boolean lock;

    public SirenBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SIREN.get(), pos, state);
    }

    /**
     * [parity Q4: TileEntityMachineSiren.updateEntity] the original sends TESirenPacket to everyone within 1500 blocks
     * every tick: NULL track (id 0, off); LOOP tracks (id, powered); PASS/SOUND tracks restart (off, on) on the rising
     * edge only and keep playing after the signal drops.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, SirenBlockEntity siren) {
        if (level.isClientSide) {
            return;
        }
        SirenTrackItem.Track selected = SirenTrackItem.track(siren.track);
        if (selected == null) {
            siren.broadcast(level, 0, false);
            return;
        }
        boolean active = level.hasNeighborSignal(pos);
        if (selected.playback() == SirenTrackItem.Playback.LOOP) {
            siren.broadcast(level, selected.id(), active);
        } else {
            if (!siren.lock && active) {
                siren.lock = true;
                siren.broadcast(level, selected.id(), false);
                siren.broadcast(level, selected.id(), true);
            }
            if (siren.lock && !active) {
                siren.lock = false;
            }
        }
    }

    public SirenTrackItem.Track selectedTrack() {
        return SirenTrackItem.track(this.track);
    }

    @Override
    public int getContainerSize() { return 1; }

    @Override
    public boolean isEmpty() { return this.track.isEmpty(); }

    @Override
    public ItemStack getItem(int slot) { return slot == SLOT ? this.track : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != SLOT || amount <= 0 || this.track.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = this.track.split(amount);
        if (this.track.isEmpty()) this.track = ItemStack.EMPTY;
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != SLOT) return ItemStack.EMPTY;
        ItemStack result = this.track;
        this.track = ItemStack.EMPTY;
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != SLOT) return;
        this.track = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT && SirenTrackItem.track(stack) != null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) { return SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; } // [parity Q4: isItemValidForSlot false]

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

    @Override
    public boolean stillValid(Player player) { return MachineDummyMenuAccess.stillValid(this, player); }

    @Override
    public void clearContent() { this.track = ItemStack.EMPTY; setChanged(); }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (!this.track.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, this.track.copy()));
            this.track = ItemStack.EMPTY;
        }
    }

    private void broadcast(Level level, int trackId, boolean active) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double range = 1500.0D;
        double rangeSqr = range * range;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this.worldPosition.getX(), this.worldPosition.getY(),
                    this.worldPosition.getZ()) < rangeSqr) { // TargetPoint(x, y, z, 1500)
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        player, new com.atomiclegacy.network.SirenSoundPayload(this.worldPosition, trackId, active));
            }
        }
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.atomiclegacy.machine_siren"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.atomiclegacy.inventory.container.SirenMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Track", this.track.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.track = ItemStack.parseOptional(registries, tag.getCompound("Track"));
    }
}
