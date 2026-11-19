package com.atomiclegacy.train;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** [parity S1: trains] EntityRailCarCargo: an IInventory rail car whose occupied slot count is synched (watcher 10). */
public abstract class RailCarCargoEntity extends RailCarEntity implements Container {
    private static final EntityDataAccessor<Integer> OCCUPIED = SynchedEntityData.defineId(RailCarCargoEntity.class, EntityDataSerializers.INT);

    @Nullable
    protected String entityName;
    protected NonNullList<ItemStack> slots;

    protected RailCarCargoEntity(EntityType<? extends RailCarCargoEntity> type, Level world) {
        super(type, world);
        this.slots = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OCCUPIED, 0);
    }

    public int occupiedSlots() {
        return this.entityData.get(OCCUPIED);
    }

    public int countOccupiedSlots() {
        int count = 0;
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.slots.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.getCount() <= amount) {
            this.slots.set(slot, ItemStack.EMPTY);
            return stack;
        }
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) this.slots.set(slot, ItemStack.EMPTY);
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.slots.get(slot);
        this.slots.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.slots.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : slots) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < slots.size(); i++) slots.set(i, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) this.entityData.set(OCCUPIED, this.countOccupiedSlots());
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() { }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ListTag list = new ListTag();
        for (int i = 0; i < this.slots.size(); ++i) {
            if (!this.slots.get(i).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("Slot", (byte) i);
                list.add(this.slots.get(i).save(this.registryAccess(), slot));
            }
        }
        nbt.put("Items", list);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        this.slots = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag slot = list.getCompound(i);
            int j = slot.getByte("Slot") & 255;
            if (j < this.slots.size()) {
                this.slots.set(j, ItemStack.parseOptional(this.registryAccess(), slot));
            }
        }
        this.entityData.set(OCCUPIED, this.countOccupiedSlots());
    }

    public boolean hasCustomInventoryName() {
        return this.entityName != null;
    }

    @Nullable
    public String getEntityName() {
        return this.entityName;
    }

    public void setEntityName(@Nullable String name) {
        this.entityName = name;
    }

    /** Unlocalized inventory name, or the custom name. */
    public abstract String getInventoryName();

    public Component inventoryTitle() {
        return this.hasCustomInventoryName() ? Component.literal(this.getInventoryName()) : Component.translatable(this.getInventoryName());
    }
}
