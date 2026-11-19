package com.atomiclegacy.entity.cart;

import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.items.ModMinecartItem.EnumMinecart;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** [parity S1: carts] EntityMinecartSemtex (decorative, the semtex only changes the model). */
public class MinecartSemtexEntity extends NtmMinecartEntity {

    public MinecartSemtexEntity(EntityType<? extends MinecartSemtexEntity> type, Level world) {
        super(type, world);
    }

    public MinecartSemtexEntity(Level world, double x, double y, double z, EnumCartBase type) {
        super(HbmEntityTypes.CART_SEMTEX.get(), world, x, y, z, type);
    }

    @Override
    public ItemStack getCartItem() {
        return cart(this.getBase(), EnumMinecart.SEMTEX);
    }
}
