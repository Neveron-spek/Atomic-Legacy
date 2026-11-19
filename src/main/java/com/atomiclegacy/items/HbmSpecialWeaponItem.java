package com.atomiclegacy.items;

import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.entity.MineRubbleEntity;
import com.atomiclegacy.explosion.LegacyVnt;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * [parity I3] 1.7.10 WeaponSpecial: ItemSword with 4 + material damage, per-item hitEntity / onItemUse /
 * movement modifiers / lore. hitEntity returns false and does not damage the stack (except the caber's 505).
 */
public final class HbmSpecialWeaponItem extends SwordItem {
    public enum Kind {
        BOTTLE_OPENER,
        CHERNOBYL_SIGN,
        DIAMOND_GAVEL,
        LEAD_GAVEL,
        MEME_SPOON,
        SCHRABIDIUM_HAMMER,
        SHIMMER_SLEDGE,
        SHIMMER_AXE,
        SOP_SIGN,
        STOP_SIGN,
        ULLAPOOL_CABER,
        WOOD_GAVEL,
        WRENCH_FLIPPED
    }

    private final Kind kind;
    private final HbmToolProfile profile;

    public HbmSpecialWeaponItem(HbmToolProfile profile, Kind kind) {
        super(HbmToolItems.tier(profile, HbmToolItems.Energy.NONE), HbmToolItems.props(profile, HbmToolItems.Energy.NONE));
        this.kind = kind;
        this.profile = profile;
    }

    public Kind kind() {
        return this.kind;
    }

    public HbmToolProfile profile() {
        return this.profile;
    }

    /** WeaponSpecial.onUpdate: achFiend / achFiend2 (jackt / jackt2 chestplate and the held shimmer weapon). */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack held = player.getMainHandItem();
        if (chest.is(HbmItems.JACKT.get()) && held.getItem() instanceof HbmSpecialWeaponItem w && w.kind == Kind.SHIMMER_SLEDGE) {
            HbmAdvancements.award(player, "fiend");
        } else if (chest.is(HbmItems.JACKT2.get()) && held.getItem() instanceof HbmSpecialWeaponItem w && w.kind == Kind.SHIMMER_AXE) {
            HbmAdvancements.award(player, "fiend2");
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        if (level.isClientSide) return false;
        switch (this.kind) {
            case SCHRABIDIUM_HAMMER -> {
                target.setHealth(0.0F);
                sound(target, HbmSoundEvents.WEAPON_BONK.get(), 3.0F, 1.0F);
            }
            case BOTTLE_OPENER -> {
                int i = level.random.nextInt(7);
                if (i == 0) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 60 * 20, 0));
                if (i == 1) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 60 * 20, 2));
                if (i == 2) target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5 * 60 * 20, 2));
                if (i == 3) target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60 * 20, 0));
                sound(target, SoundEvents.ANVIL_LAND, 3.0F, 1.0F);
            }
            case ULLAPOOL_CABER -> {
                LegacyVnt.vanilla((ServerLevel) level, null, target.getX(), target.getY(), target.getZ(), 7.5F, false, true);
                stack.hurtAndBreak(505, attacker, EquipmentSlot.MAINHAND);
            }
            case SHIMMER_SLEDGE -> {
                Vec3 vec = attacker.getLookAngle();
                target.setDeltaMovement(target.getDeltaMovement().add(vec.x * 5, vec.y * 5, vec.z * 5));
                target.hurtMarked = true;
                sound(target, HbmSoundEvents.WEAPON_BANG.get(), 3.0F, 1.0F);
            }
            case SHIMMER_AXE -> {
                target.setHealth(target.getHealth() / 2);
                sound(target, HbmSoundEvents.WEAPON_SLICE.get(), 3.0F, 1.0F);
            }
            case WOOD_GAVEL -> sound(target, HbmSoundEvents.WEAPON_WHACK.get(), 3.0F, 1.0F);
            case LEAD_GAVEL -> {
                sound(target, HbmSoundEvents.WEAPON_WHACK.get(), 3.0F, 1.0F);
                target.addEffect(new MobEffectInstance(HbmMobEffects.LEAD_POISONING, 15 * 20, 4));
            }
            case DIAMOND_GAVEL -> {
                float ded = target.getMaxHealth() / 3;
                target.setHealth(target.getHealth() - ded);
                sound(target, HbmSoundEvents.WEAPON_WHACK.get(), 3.0F, 1.0F);
            }
            case MEME_SPOON -> {
                if (!(attacker instanceof Player player)) return false;
                if (player.fallDistance >= 2) {
                    sound(target, HbmSoundEvents.WEAPON_BANG.get(), 3.0F, 0.75F);
                    target.hurt(target.damageSources().playerAttack(player), 50F);
                }
                if (player.fallDistance >= 20 && !player.getAbilities().instabuild) {
                    Vec3 center = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2D, target.getZ());
                    LegacyVnt.at((ServerLevel) level, center, 15).exploder(player).entitiesCrossSmooth(1, 150, 25, 0.5F).explode();
                    LegacyProjectileUtil.composeExplosionEffect((ServerLevel) level, center,
                            10, 2.0F, 0.5F, 25.0F, 5, 8, 20, 0.75F, 1.0F, -2.0F, 150.0F);
                }
            }
            case STOP_SIGN, SOP_SIGN -> sound(target, HbmSoundEvents.WEAPON_STOP.get(), 1.0F, 1.0F);
            case CHERNOBYL_SIGN, WRENCH_FLIPPED -> {
            }
        }
        return false;
    }

    /** hitEntity returned false without damageItem: no durability loss on hits. */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    private static void sound(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (this.kind == Kind.SHIMMER_SLEDGE) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000 && player != null && !level.isClientSide) {
                Vec3 vec = player.getLookAngle();
                MineRubbleEntity rubble = new MineRubbleEntity(level, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F,
                        new Vec3(vec.x * 5, vec.y * 5, vec.z * 5), state);
                sound(rubble, HbmSoundEvents.WEAPON_BANG.get(), 3.0F, 1.0F);
                level.addFreshEntity(rubble);
                level.destroyBlock(pos, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (this.kind == Kind.SHIMMER_AXE) {
            if (!level.isClientSide) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundEvents.WEAPON_KAPENG.get(),
                        SoundSource.PLAYERS, 3.0F, 1.0F);
                for (BlockPos p : new BlockPos[]{pos, pos.above(), pos.below()}) {
                    BlockState state = level.getBlockState(p);
                    if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000) level.destroyBlock(p, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean polaroid11 = com.atomiclegacy.AtomicLegacy.polaroidID == 11; // [parity J1] MainRegistry.polaroidID == 11 variants
        switch (this.kind) {
            case SCHRABIDIUM_HAMMER -> {
                gray(tooltip, "Even though it says \"+1000000000");
                gray(tooltip, "damage\", it's actually \"onehit anything\"");
            }
            case ULLAPOOL_CABER -> {
                gray(tooltip, "High-yield Scottish face removal.");
                gray(tooltip, "A sober person would throw it...");
            }
            case BOTTLE_OPENER -> {
                gray(tooltip, "My very own bottle opener.");
                gray(tooltip, "Use with caution!");
            }
            case SHIMMER_SLEDGE -> {
                if (polaroid11) {
                    gray(tooltip, "shimmer no");
                    gray(tooltip, "drop that hammer");
                    gray(tooltip, "you're going to hurt somebody");
                    gray(tooltip, "shimmer no");
                    gray(tooltip, "shimmer pls");
                } else {
                    gray(tooltip, "Breaks everything, even portals.");
                }
            }
            case SHIMMER_AXE -> gray(tooltip, polaroid11 ? "shim's toolbox does an e-x-p-a-n-d" : "Timber!");
            case WRENCH_FLIPPED -> gray(tooltip, "Wrench 2: The Wrenchening");
            case MEME_SPOON -> {
                tooltip.add(Component.literal("Level 10 Shovel").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Deals crits while the wielder is rocket jumping").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("20% slower firing speed").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("No random critical hits").withStyle(ChatFormatting.RED));
            }
            case WOOD_GAVEL -> gray(tooltip, "Thunk!");
            case LEAD_GAVEL -> gray(tooltip, "You are hereby sentenced to lead poisoning.");
            case DIAMOND_GAVEL -> {
                gray(tooltip, "The joke! It makes sense now!!");
                gray(tooltip, "");
                tooltip.add(Component.literal("Deals as much damage as it needs to.").withStyle(ChatFormatting.BLUE));
            }
            default -> {
            }
        }
    }

    private static void gray(List<Component> tooltip, String line) {
        tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
}
