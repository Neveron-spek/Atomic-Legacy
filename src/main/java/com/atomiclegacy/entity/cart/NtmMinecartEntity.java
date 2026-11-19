package com.atomiclegacy.entity.cart;

import com.atomiclegacy.items.ModMinecartItem;
import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * [parity S1: carts] com.hbm.entity.cart.EntityMinecartNTM: a minecart with an EnumCartBase (watcher 23, NBT "base"),
 * solid for other entities, dropping only its own cart item. Not rideable (only EntityMinecartEmpty was).
 */
public abstract class NtmMinecartEntity extends AbstractMinecart {
    private static final EntityDataAccessor<Integer> CART_BASE = SynchedEntityData.defineId(NtmMinecartEntity.class, EntityDataSerializers.INT);

    protected NtmMinecartEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    protected NtmMinecartEntity(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base) {
        super(type, world, x, y, z);
        this.setBase(base);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CART_BASE, 0); //EnumCartBase
    }

    public void setBase(EnumCartBase type) {
        this.entityData.set(CART_BASE, type.ordinal());
    }

    public EnumCartBase getBase() {
        return EnumCartBase.values()[this.entityData.get(CART_BASE)];
    }

    @Override
    public Type getMinecartType() {
        // 1.7.10 returned -1; CHEST keeps it out of the rideable and furnace logic, like the original
        return Type.CHEST;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** killMinecart: drop getCartItem with the cart's custom name. */
    @Override
    public void destroy(DamageSource source) {
        this.kill();
        if (this.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
            dropCartItem(source);
        }
    }

    protected void dropCartItem(DamageSource source) {
        ItemStack itemstack = getCartItem();
        if (this.hasCustomName()) {
            itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        }
        this.spawnAtLocation(itemstack, 0.0F);
    }

    public abstract ItemStack getCartItem();

    @Override
    protected Item getDropItem() {
        return HbmItems.CART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return getCartItem();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("base", this.entityData.get(CART_BASE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(CART_BASE, Math.floorMod(nbt.getInt("base"), EnumCartBase.values().length));
    }

    protected static ItemStack cart(EnumCartBase base, ModMinecartItem.EnumMinecart type) {
        return ModMinecartItem.createCartItem(base, type);
    }
}
