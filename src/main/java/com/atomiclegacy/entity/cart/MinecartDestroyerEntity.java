package com.atomiclegacy.entity.cart;

import java.util.List;

import com.atomiclegacy.inventory.container.CartDestroyerMenu;
import com.atomiclegacy.items.ModMinecartItem.EnumCartBase;
import com.atomiclegacy.items.ModMinecartItem.EnumMinecart;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * [parity S1: carts] EntityMinecartDestroyer: every 5 ticks deletes dropped items within (x +-2.5, y -1.5..+2, z +-2.5)
 * that match a filter; slots 0-8 match item and damage, slots 9-17 match the item only. The 1.7.10 damage value maps to
 * the damage component plus the custom model data the port uses for flattened metadata variants (NBT stays ignored).
 */
public class MinecartDestroyerEntity extends NtmMinecartContainerEntity {

    public MinecartDestroyerEntity(EntityType<? extends MinecartDestroyerEntity> type, Level world) {
        super(type, world);
    }

    public MinecartDestroyerEntity(Level world, double x, double y, double z, EnumCartBase type) {
        super(HbmEntityTypes.CART_DESTROYER.get(), world, x, y, z, type);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CartDestroyerMenu(id, inventory, this), this.inventoryTitle()),
                    buffer -> buffer.writeVarInt(this.getId()));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return HbmBlocks.MACHINE_SHREDDER.get().defaultBlockState();
    }

    @Override
    public int getContainerSize() {
        return 18;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && this.tickCount % 5 == 0) {
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, new AABB(
                    getX() - 2.5,
                    getY() - 1.5,
                    getZ() - 2.5,
                    getX() + 2.5,
                    getY() + 2,
                    getZ() + 2.5));

            boolean sound = false;

            outer:
            for (ItemEntity item : items) {
                ItemStack stack = item.getItem();

                //Match meta
                for (int i = 0; i < 9; i++) {
                    ItemStack match = this.slots.get(i);
                    if (!match.isEmpty() && match.getItem() == stack.getItem() && sameLegacyDamage(match, stack)) {
                        item.discard();
                        sound = true;
                        continue outer;
                    }
                }

                //Match wildcard
                for (int i = 9; i < 18; i++) {
                    ItemStack match = this.slots.get(i);
                    if (!match.isEmpty() && match.getItem() == stack.getItem()) {
                        item.discard();
                        sound = true;
                        continue outer;
                    }
                }
            }

            if (sound)
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.NEUTRAL, 0.5F, 0.5F + level().random.nextFloat() * 0.2F);
        }

        if (level().isClientSide && this.tickCount % 5 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.75, getZ(), 0.0, 0.01, 0.0);
        }
    }

    private static boolean sameLegacyDamage(ItemStack a, ItemStack b) {
        return a.getDamageValue() == b.getDamageValue()
                && java.util.Objects.equals(a.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA), b.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA));
    }

    @Override
    public ItemStack getCartItem() {
        return cart(this.getBase(), EnumMinecart.DESTROYER);
    }
}
