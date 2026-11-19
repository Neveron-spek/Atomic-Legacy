package com.atomiclegacy.items;

import com.atomiclegacy.tileentity.PipeAnchorBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import com.atomiclegacy.network.PlayerInformPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.7.10 ItemWrench(tMatSteel): an ItemSword (4 + 2 damage, 750 uses) with the pipe-anchor linking on right click.
 * [parity I3] sword stats, sneak bypass, chat messages, hit knockback, distance info line; the anchor connection rules
 * themselves (TileEntityPipelineBase.canConnect) belong to the logistics port and are kept from the base.
 */
public final class LegacyWrenchItem extends SwordItem {
    private static final String ANCHOR = "pipe_anchor";
    private static final HbmToolProfile PROFILE = HbmToolProfile.create(HbmToolTier.STEEL, 6.0F, -2.4F);

    public LegacyWrenchItem(Properties properties) {
        super(HbmToolItems.tier(PROFILE, HbmToolItems.Energy.NONE), HbmToolItems.props(PROFILE, HbmToolItems.Energy.NONE));
    }

    /** ItemWrench.hitEntity: look * 0.5 knockback, random.anvil_land 3 / 0.75, returns false without damageItem. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            Vec3 vec = attacker.getLookAngle();
            target.setDeltaMovement(target.getDeltaMovement().add(vec.x * 0.5, vec.y * 0.5, vec.z * 0.5));
            target.hurtMarked = true;
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 0.75F);
        }
        return false;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /** ItemWrench.onUpdate: "<name>: <distance>m" info line (ServerProxy.ID_WRENCH = 13) while a start is stored. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(ANCHOR)) return;
        BlockPos pos = BlockPos.of(tag.getLong(ANCHOR));
        double length = new Vec3(entity.getX() - pos.getX(), entity.getY() - pos.getY(), entity.getZ() - pos.getZ()).length();
        PacketDistributor.sendToPlayer(player, new PlayerInformPayload(13, stack.getHoverName().getString() + ": " + ((int) length) + "m", 0xFFFFFF, 3000));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (!(level.getBlockEntity(clicked) instanceof PipeAnchorBlockEntity)) {
            return InteractionResult.PASS;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(ANCHOR)) {
            tag.putLong(ANCHOR, clicked.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (!level.isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.atomiclegacy.wrench.start"), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos firstPos = BlockPos.of(tag.getLong(ANCHOR));
        tag.remove(ANCHOR);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (!(level.getBlockEntity(firstPos) instanceof PipeAnchorBlockEntity first)
                || !(level.getBlockEntity(clicked) instanceof PipeAnchorBlockEntity second)) {
            return pipeError(level, context, "message.atomiclegacy.wrench.error");
        }

        // [parity T1: pipe_anchor_exhaust] TileEntityPipelineBase#canConnect: different NetworkType -> code 1
        if (first.anchorKind() != second.anchorKind()) { // [parity V2: pipe_anchor_pneumatic] FLUID / EXHAUST / PNEUMATIC
            return pipeError(level, context, "message.atomiclegacy.wrench.incompatible");
        }
        if (first == second) {
            return pipeError(level, context, "message.atomiclegacy.wrench.same");
        }
        if (first.anchorKind() != com.atomiclegacy.blocks.PipeAnchorBlock.Kind.FLUID) { // exhaust and pneumatic anchors have no fluid type (no code 4) [parity V2: pipe_anchor_pneumatic]
            if (firstPos.distSqr(clicked) > 100.0D) {
                return pipeError(level, context, "message.atomiclegacy.wrench.distance");
            }
            if (!level.isClientSide) {
                first.addLink(clicked);
                second.addLink(firstPos);
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(Component.translatable("message.atomiclegacy.wrench.end"), false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        HbmFluidDefinition firstType = first.type();
        HbmFluidDefinition secondType = second.type();
        if (firstType.isNone() && !secondType.isNone()) {
            first.setType(secondType);
            firstType = secondType;
        } else if (secondType.isNone() && !firstType.isNone()) {
            second.setType(firstType);
            secondType = firstType;
        }
        if (firstType != secondType) {
            return pipeError(level, context, "message.atomiclegacy.wrench.type_error");
        }

        if (firstPos.distSqr(clicked) > 100.0D) {
            return pipeError(level, context, "message.atomiclegacy.wrench.distance");
        }

        if (!level.isClientSide) {
            first.addLink(clicked);
            second.addLink(firstPos);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.atomiclegacy.wrench.end"), false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult pipeError(Level level, UseOnContext context, String key) {
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable(key), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(ANCHOR)) {
            BlockPos pos = BlockPos.of(tag.getLong(ANCHOR));
            tooltip.add(Component.translatable("tooltip.atomiclegacy.wrench.anchor_x", pos.getX()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.atomiclegacy.wrench.anchor_y", pos.getY()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.atomiclegacy.wrench.anchor_z", pos.getZ()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.atomiclegacy.wrench.use").withStyle(ChatFormatting.GRAY));
        }
    }

}
