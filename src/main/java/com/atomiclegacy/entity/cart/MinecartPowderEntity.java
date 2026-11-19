package com.atomiclegacy.entity.cart;

import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.items.ModMinecartItem.EnumMinecart;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** [parity S1: carts] EntityMinecartPowder (decorative, the powder only changes the model). */
public class MinecartPowderEntity extends NtmMinecartEntity {

    public MinecartPowderEntity(EntityType<? extends MinecartPowderEntity> type, Level world) {
        super(type, world);
    }

    public MinecartPowderEntity(Level world, double x, double y, double z, EnumCartBase type) {
        super(HbmEntityTypes.CART_POWDER.get(), world, x, y, z, type);
    }

    @Override
    public ItemStack getCartItem() {
        return cart(this.getBase(), EnumMinecart.POWDER);
    }
}
