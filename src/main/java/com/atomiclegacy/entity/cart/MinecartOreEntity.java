package com.atomiclegacy.entity.cart;

import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.items.ModMinecartItem.EnumMinecart;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** [parity S1: carts] EntityMinecartOre (the HBM "Minecart", type EMPTY). */
public class MinecartOreEntity extends NtmMinecartEntity {

    public MinecartOreEntity(EntityType<? extends MinecartOreEntity> type, Level world) {
        super(type, world);
    }

    public MinecartOreEntity(Level world, double x, double y, double z, EnumCartBase type) {
        super(HbmEntityTypes.CART_ORE.get(), world, x, y, z, type);
    }

    @Override
    public ItemStack getCartItem() {
        return cart(this.getBase(), EnumMinecart.EMPTY);
    }
}
