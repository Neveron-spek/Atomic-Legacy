package com.atomiclegacy.items;

import com.atomiclegacy.tileentity.LockableBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LockItem extends KeyPinItem {
    private final double lockMod;

    public LockItem(Properties properties, double lockMod) {
        super(properties);
        this.lockMod = lockMod;
    }

    public double lockMod() {
        return this.lockMod;
    }

    public static boolean isLock(ItemStack stack) {
        return stack.getItem() instanceof LockItem;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        int pins = KeyPinItem.pins(stack);
        if (pins == 0) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            blockEntity = dummy.core();
        }
        if (!(blockEntity instanceof LockableBlockEntity lockable) || lockable.isLocked()) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            lockable.setPins(pins);
            lockable.setLockMod(this.lockMod);
            lockable.lock();
            // [parity I3] ItemLock: no cheesable change, playSoundAtEntity(player, hbm:block.lockHang), stackSize-- also in creative
            if (context.getPlayer() != null) {
                context.getLevel().playSound(null, context.getPlayer().getX(), context.getPlayer().getY(), context.getPlayer().getZ(), HbmSoundEvents.LOCK_HANG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
