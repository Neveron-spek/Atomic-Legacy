package com.atomiclegacy.items;

import com.atomiclegacy.player.HbmPlayerArmorState;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Direct port of ItemModLodestone's armor-slot item attraction. */
public final class ArmorMagnetItem extends ArmorModItem {
    private final int range;

    public ArmorMagnetItem(Properties properties, int range) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.range = range;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (!(player.level() instanceof ServerLevel serverLevel)
                || !HbmPlayerArmorState.isMagnetActive(player)) {
            return;
        }

        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(this.range))) {
            Vec3 direction = player.position().subtract(item.position());
            if (direction.lengthSqr() < 1.0E-6D) {
                continue;
            }

            direction = direction.normalize();
            Vec3 motion = item.getDeltaMovement().add(direction.scale(0.75D));
            if (direction.y > 0.0D && motion.y < 0.04D) {
                motion = motion.add(0.0D, 0.2D, 0.0D);
            }
            item.setDeltaMovement(motion);
            item.hasImpulse = true;
            serverLevel.getChunkSource().broadcast(item, new ClientboundSetEntityMotionPacket(item));
        }
    }

    // [parity I2: tooltip] ItemModLodestone
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.DARK_GRAY, "Attracts nearby items"));
        tooltip.add(line(ChatFormatting.DARK_GRAY, "Item attraction range: " + this.range));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.DARK_GRAY, mod, " (Magnetic range: " + this.range + ")"));
    }
}
