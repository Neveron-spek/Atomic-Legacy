package com.atomiclegacy.items;

import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraft.util.Mth;
import java.util.function.Consumer;

/** Exact 1.7.10 ItemModTesla target selection and damage calculation. */
public final class LegacyTeslaArmorModItem extends ArmorModItem {
    private static final double RANGE = 5.0D;

    public LegacyTeslaArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
    }

    // 1.7.10 ItemModTesla has no item renderer: the stack is the flat back_tesla icon; ModelBackTesla is only
    // drawn on the wearer.
    // [parity I2] ItemModTesla#modUpdate -> TileEntityTesla.zap(world, x, y + 1.25, z, 5, entity): AABB +-5, distance
    // check against TileEntityTesla.range (10), crabs healed / skipped, creepers charged, Faraday players spared;
    // the chestplate wears by 1 with a 1/5 chance whenever the target list is not empty.
    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.level().isClientSide || !(armor.getItem() instanceof PoweredArmorFSBItem) || !ArmorFSBItem.hasFSBArmor(player)) {
            return;
        }
        List<Vec3> targets = zap(player.level(), player.getX(), player.getY() + 1.25D, player.getZ(), 5.0D, player);
        if (!targets.isEmpty() && player.getRandom().nextInt(5) == 0) {
            armor.hurtAndBreak(1, player, player.getEquipmentSlotForItem(armor));
        }
    }

    public static List<Vec3> zap(Level level, double x, double y, double z, double radius, LivingEntity source) {
        List<Vec3> ret = new java.util.ArrayList<>();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));
        Vec3 origin = new Vec3(x, y, z);
        for (LivingEntity e : targets) {
            if (e instanceof net.minecraft.world.entity.animal.Ocelot || e instanceof Cat || e == source) continue;
            Vec3 point = new Vec3(e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ());
            if (point.distanceTo(origin) > 10) continue;
            if (isObstructed(level, origin, point, source)) continue;
            if (e instanceof com.atomiclegacy.entity.LegacyTaintCrabEntity) {
                ret.add(new Vec3(e.getX(), e.getY() + 1.25, e.getZ()));
                e.heal(15F);
                continue;
            }
            if (e instanceof com.atomiclegacy.entity.LegacyTeslaCrabEntity) {
                ret.add(new Vec3(e.getX(), e.getY() + 1, e.getZ()));
                e.heal(10F);
                continue;
            }
            if (e instanceof com.atomiclegacy.entity.LegacyCyberCrabEntity) {
                ret.add(point);
                continue;
            }
            if (e instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                LegacyArmorModParity.setCreeperPowered(creeper, true);
                ret.add(point);
                continue;
            }
            if (!(e instanceof Player && com.atomiclegacy.pollution.HbmArmorProtection.hasFaradayProtection(e))) {
                if (e.hurt(level.damageSources().source(HbmDamageTypes.ELECTRICITY), Mth.clamp(e.getMaxHealth() * 0.5F, 3, 20) / (float) targets.size())) {
                    level.playSound(null, e.getX(), e.getY(), e.getZ(), HbmSoundEvents.WEAPON_TESLA.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            ret.add(point);
        }
        return ret;
    }

    private static boolean isObstructed(Level level, Vec3 origin, Vec3 target, LivingEntity source) {
        HitResult result = level.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return result.getType() != HitResult.Type.MISS;
    }

    // [parity I2: tooltip] ItemModTesla
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.YELLOW, "Zaps nearby entities (requires full electric set)"));
        tooltip.add(net.minecraft.network.chat.Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<net.minecraft.network.chat.Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(mod.getHoverName().copy().append(" (zaps nearby entities)").withStyle(ChatFormatting.YELLOW));
    }
}
