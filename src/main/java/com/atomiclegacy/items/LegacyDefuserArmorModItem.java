package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct 1.7.10 ItemModDefuser behavior: permanently removes nearby swell AI. */
public final class LegacyDefuserArmorModItem extends ArmorModItem {
    private static final String DEFUSED = "hbm_defused";
    private static final String LEGACY_DEFUSED = "hfr_defused";

    public LegacyDefuserArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.level().isClientSide || player.level().getGameTime() % 20 != 0) { // [parity I2] getTotalWorldTime() % 20
            return;
        }
        for (Creeper creeper : player.level().getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(5.0D))) {
            defuse(creeper, player, true);
        }
    }

    public static boolean defuse(Creeper creeper, LivingEntity entity, boolean dropItem) {
        if (creeper.level().isClientSide
                || creeper.getPersistentData().getBoolean(DEFUSED)
                || creeper.getPersistentData().getBoolean(LEGACY_DEFUSED)) {
            return false;
        }
        // [parity I2] setCreeperState(-1) and DataWatcher 18 (ignited) = 0; the target is kept
        creeper.setSwellDir(-1);
        LegacyArmorModParity.setCreeperIgnited(creeper, false);
        java.util.List<net.minecraft.world.entity.ai.goal.Goal> swellGoals = creeper.goalSelector.getAvailableGoals().stream()
                .filter(goal -> goal.getGoal() instanceof SwellGoal)
                .map(goal -> goal.getGoal())
                .toList();
        if (swellGoals.isEmpty()) {
            return false;
        }
        swellGoals.forEach(creeper.goalSelector::removeGoal);
        creeper.getPersistentData().putBoolean(DEFUSED, true);
        creeper.getPersistentData().putBoolean(LEGACY_DEFUSED, true);
        if (dropItem) {
            // [parity I2] order and values of castrateCreeper: hbm:item.pinBreak, safety_fuse, 1 dmg, Weakness(0, 200)
            creeper.level().playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(),
                    com.atomiclegacy.registry.HbmSoundEvents.PIN_BREAK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Item fuse = BuiltInRegistries.ITEM.get(AtomicLegacy.id("safety_fuse"));
            if (fuse != net.minecraft.world.item.Items.AIR) {
                creeper.spawnAtLocation(new ItemStack(fuse), 0.0F);
            }
            creeper.hurt(entity != null ? entity.damageSources().mobAttack(entity) : creeper.damageSources().magic(), 1.0F);
            creeper.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 0, 200));
        }
        return true;
    }

    // [parity I2: tooltip] ItemModDefuser
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.YELLOW, "Defuses nearby creepers"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.YELLOW, mod, " (Defuses creepers)"));
    }
}
