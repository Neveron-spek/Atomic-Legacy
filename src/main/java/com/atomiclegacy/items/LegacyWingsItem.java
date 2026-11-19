package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** [parity I2] WingsMurk (wings_limp / wings_murk): JetpackBase armor mod, wearable on its own. */
public final class LegacyWingsItem extends ArmorModItem {
    private final boolean powered;

    public LegacyWingsItem(Properties properties, boolean powered) {
        super(properties.stacksTo(1), ArmorModHandler.PLATE_ONLY, false, true, false, false);
        this.powered = powered;
    }

    public boolean powered() {
        return this.powered;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        ItemStack installed = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
        if (installed.isEmpty()) return;
        onArmorTick(player.level(), player, installed);
        ArmorModHandler.applyMod(armor, installed, player.registryAccess());
    }

    public void onArmorTick(Level level, Player player, ItemStack stack) {
        if (player.onGround()) return;
        com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower.resetFlightTime(player);
        if (player.fallDistance > 0) player.fallDistance = 0;
        Vec3 m = player.getDeltaMovement();
        double mx = m.x, my = m.y, mz = m.z;
        boolean sneaking = player.isShiftKeyDown();

        if (!this.powered) {
            if (my < -0.4D) my = -0.4D;
            if (sneaking) {
                if (my < -0.08) {
                    double mo = my * -0.2;
                    my += mo;
                    Vec3 vec = player.getLookAngle().scale(mo);
                    mx += vec.x;
                    my += vec.y;
                    mz += vec.z;
                }
            }
        } else {
            boolean backpack = LegacyArmorModParity.enableBackpack(player);
            if (LegacyArmorModParity.isJetpackActive(player)) {
                if (sneaking) {
                    if (my < -1) my += 0.4D;
                    else if (my < -0.1) my += 0.2D;
                    else if (my < 0) my = 0;
                    else if (my > 1) my -= 0.4D;
                    else if (my > 0.1) my -= 0.2D;
                    else if (my > 0) my = 0;
                } else {
                    if (my < 0.6D) my += 0.2D;
                    else my = 0.8D;
                }
            } else if (backpack && !sneaking) {
                if (my < -1) my += 0.4D;
                else if (my < -0.1) my += 0.2D;
                else if (my < 0) my = 0;
            }
            if (backpack) {
                Vec3 orig = player.getLookAngle();
                Vec3 look = new Vec3(orig.x, 0, orig.z).normalize();
                double mod = player.isSprinting() ? 1D : 0.25D;
                if (player.zza != 0) {
                    mx += look.x * 0.35 * player.zza * mod;
                    mz += look.z * 0.35 * player.zza * mod;
                }
                if (player.xxa != 0) {
                    look = look.yRot((float) Math.PI * 0.5F);
                    mx += look.x * 0.15 * player.xxa * mod;
                    mz += look.z * 0.15 * player.xxa * mod;
                }
            }
        }
        player.setDeltaMovement(mx, my, mz);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(line(ChatFormatting.GOLD, "Can be worn on its own!"));
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.RED, mod, ""));
    }
}
