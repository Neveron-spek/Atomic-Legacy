package com.atomiclegacy.items;

import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RbmkLidItem extends Item {
    private final RbmkComponentBlockEntity.LidType lidType;

    public RbmkLidItem(Properties properties, RbmkComponentBlockEntity.LidType lidType) {
        super(properties);
        this.lidType = lidType;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        RbmkComponentBlockEntity rbmk = resolveRbmk(level, context.getClickedPos());
        if (rbmk == null || !rbmk.canUseLid() || rbmk.hasLid()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            rbmk.installLid(lidType);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            // ItemRBMKLid: stepSound.func_150496_b() of glass / concrete_smooth (stone),
            // volume (getVolume() + 1) / 2, pitch getPitch() * 0.8, at the clicked block.
            net.minecraft.world.level.block.SoundType type = lidType == RbmkComponentBlockEntity.LidType.GLASS
                    ? net.minecraft.world.level.block.SoundType.GLASS : net.minecraft.world.level.block.SoundType.STONE;
            BlockPos clicked = context.getClickedPos();
            level.playSound(null, clicked.getX() + 0.5D, clicked.getY() + 0.5D, clicked.getZ() + 0.5D,
                    type.getPlaceSound(), SoundSource.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static RbmkComponentBlockEntity resolveRbmk(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk) {
            return rbmk;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof RbmkComponentBlockEntity rbmk) {
            return rbmk;
        }
        return null;
    }
}
