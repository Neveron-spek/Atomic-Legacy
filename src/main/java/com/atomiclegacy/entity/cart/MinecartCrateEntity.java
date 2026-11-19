package com.atomiclegacy.entity.cart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.atomiclegacy.blocks.LegacyCrateLogic;
import com.atomiclegacy.inventory.container.StorageCrateMenu;
import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.items.ModMinecartItem.EnumMinecart;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity S1: carts] EntityMinecartCrate: a vanilla-base cart carrying a steel crate (54 slots, ContainerCrateSteel /
 * GUICrateSteel). Breaking it stores the contents in the crate cart item; above 6000 compressed bytes it explodes
 * (strength 2, fire, smoke) and additionally drops an empty crate cart.
 */
public class MinecartCrateEntity extends NtmMinecartContainerEntity {

    public MinecartCrateEntity(EntityType<? extends MinecartCrateEntity> type, Level world) {
        super(type, world);
    }

    public MinecartCrateEntity(Level world, double x, double y, double z, EnumCartBase type, ItemStack stack) {
        super(HbmEntityTypes.CART_CRATE.get(), world, x, y, z, type);
        CompoundTag data = LegacyCrateLogic.data(stack);
        if (!data.isEmpty()) {
            for (int i = 0; i < getContainerSize(); i++) {
                setItem(i, ItemStack.parseOptional(world.registryAccess(), data.getCompound("slot" + i)));
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> StorageCrateMenu.cart(id, inventory, this), this.inventoryTitle()),
                    buffer -> StorageCrateMenu.writeCart(buffer, this));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return HbmBlocks.CRATE_STEEL.get().defaultBlockState();
    }

    @Override
    public int getContainerSize() {
        return 9 * 6;
    }

    @Override
    protected void dropCartItem(DamageSource source) {
        ItemStack itemstack = cart(EnumCartBase.VANILLA, EnumMinecart.CRATE);
        CompoundTag nbt = new CompoundTag();

        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            nbt.put("slot" + i, stack.save(this.registryAccess()));
        }

        if (!nbt.isEmpty()) {
            LegacyCrateLogic.setData(itemstack, nbt);
        }

        if (this.hasCustomName()) {
            itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        }

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, bytes);
            if (bytes.size() > 6000) {
                level().explode(this, getX(), getY(), getZ(), 2F, true, Level.ExplosionInteraction.TNT);
                this.spawnAtLocation(cart(EnumCartBase.VANILLA, EnumMinecart.CRATE), 0.0F);
            }
        } catch (IOException e) { }

        this.spawnAtLocation(itemstack, 0.0F);
    }

    @Override
    public ItemStack getCartItem() {
        return cart(EnumCartBase.VANILLA, EnumMinecart.CRATE);
    }
}
