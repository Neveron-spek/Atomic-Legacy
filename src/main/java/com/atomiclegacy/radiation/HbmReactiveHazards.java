package com.atomiclegacy.radiation;

import com.atomiclegacy.explosion.LegacyVnt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [parity K1: HazardTypeHydroactive / HazardTypeExplosive] The two hazard types that act on the stack itself:
 * onUpdate (held by a living entity) and updateEntity (dropped item, HazardSystem.updateDroppedItem every
 * ServerConfig.ITEM_HAZARD_DROP_TICKRATE ticks). RadiationConfig.disableHydro / disableExplosive default to false.
 */
public final class HbmReactiveHazards {

    /** ServerConfig.ITEM_HAZARD_DROP_TICKRATE default. */
    public static final int ITEM_HAZARD_DROP_TICKRATE = 2;

    private static volatile Set<Item> reactiveItems;

    private HbmReactiveHazards() {
    }

    /** Items that have a HYDROACTIVE or EXPLOSIVE entry under any of their keys (fast pre-filter). */
    private static Set<Item> reactiveItems() {
        Set<Item> items = reactiveItems;
        if (items == null) {
            items = new HashSet<>();
            for (var entry : HbmHazardRegistry.table().entrySet()) {
                boolean reactive = false;
                for (HbmHazardRegistry.Entry hazard : entry.getValue()) {
                    reactive |= hazard.type() == HbmHazardRegistry.Type.HYDROACTIVE || hazard.type() == HbmHazardRegistry.Type.EXPLOSIVE;
                }
                if (!reactive) {
                    continue;
                }
                String key = entry.getKey();
                int hash = key.indexOf('#');
                String id = hash < 0 ? key : key.substring(0, hash);
                ResourceLocation location = id.contains(":") ? ResourceLocation.parse(id)
                        : ResourceLocation.fromNamespaceAndPath(com.atomiclegacy.AtomicLegacy.MOD_ID, id);
                Item item = BuiltInRegistries.ITEM.get(location);
                if (item != Items.AIR) {
                    items.add(item);
                }
            }
            reactiveItems = items;
        }
        return items;
    }

    private static boolean mayReact(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (reactiveItems().contains(item)) {
            return true;
        }
        // other mods' items through the ore dictionary tags (lithium, sodium, strontium, caesium...)
        String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
        return !namespace.equals(com.atomiclegacy.AtomicLegacy.MOD_ID) && !namespace.equals("minecraft")
                && stack.getTags().findAny().isPresent();
    }

    /** HazardSystem.updatePlayerInventory / updateLivingInventory for the two stack-consuming types. */
    public static void tickHolder(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        if (target instanceof Player player) {
            tickStacks(level, target, player.getInventory().items);
            tickStacks(level, target, player.getInventory().armor);
            tickStacks(level, target, player.getInventory().offhand);
        } else {
            for (ItemStack stack : target.getHandSlots()) {
                tickStack(level, target, stack);
            }
            for (ItemStack stack : target.getArmorSlots()) {
                tickStack(level, target, stack);
            }
        }
    }

    private static void tickStacks(ServerLevel level, LivingEntity target, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            tickStack(level, target, stack);
        }
    }

    private static void tickStack(ServerLevel level, LivingEntity target, ItemStack stack) {
        if (!mayReact(stack)) {
            return;
        }
        for (HbmHazardRegistry.Entry entry : HbmHazardRegistry.entries(stack)) {
            if (entry.type() == HbmHazardRegistry.Type.HYDROACTIVE) {
                // HazardTypeHydroactive.onUpdate: target.isWet() && stack.stackSize > 0
                if (!com.atomiclegacy.config.HbmParityV3Config.disableHydro() // [parity V3: RadiationConfig.disableHydro]
                        && target.isInWaterOrRain() && stack.getCount() > 0) {
                    float strength = entry.eval(stack, target);
                    stack.setCount(0);
                    LegacyVnt.vanilla(level, null, target.getX(), target.getEyeY(), target.getZ(), strength, false, true);
                }
            } else if (entry.type() == HbmHazardRegistry.Type.EXPLOSIVE) {
                // HazardTypeExplosive.onUpdate: target.isBurning() && stack.stackSize > 0
                if (!com.atomiclegacy.config.HbmParityV3Config.disableExplosive() // [parity V3: RadiationConfig.disableExplosive]
                        && target.isOnFire() && stack.getCount() > 0) {
                    float strength = entry.eval(stack, target);
                    stack.setCount(0);
                    LegacyVnt.vanilla(level, null, target.getX(), target.getEyeY(), target.getZ(), strength, false, true);
                }
            }
        }
    }

    /** HazardSystem.updateDroppedItem for HazardTypeHydroactive / HazardTypeExplosive.updateEntity. */
    public static void tickDroppedItem(ItemEntity item) {
        if (item.level() instanceof ServerLevel level && level.getGameTime() % com.atomiclegacy.config.HbmServerParityConfig.itemHazardDropTickrate() == 0 /* [parity S2: configs] */) {
            updateDroppedItem(item);
        }
    }

    /** HazardSystem.updateDroppedItem (without the tick-rate gate). */
    public static void updateDroppedItem(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = item.getItem();
        if (item.isRemoved() || stack.isEmpty() || !mayReact(stack)) {
            return;
        }
        for (HbmHazardRegistry.Entry entry : HbmHazardRegistry.entries(stack)) {
            float strength = entry.eval(stack, null);
            if (entry.type() == HbmHazardRegistry.Type.HYDROACTIVE) {
                if (com.atomiclegacy.config.HbmParityV3Config.disableHydro()) continue; // [parity V3: RadiationConfig.disableHydro]
                BlockPos pos = new BlockPos(Mth.floor(item.getX()), Mth.floor(item.getY()), Mth.floor(item.getZ()));
                if (item.isInWaterOrRain() || level.getBlockState(pos).getFluidState().is(FluidTags.WATER)) {
                    item.discard();
                    LegacyVnt.vanilla(level, null, item.getX(), item.getY() + item.getBbHeight() * 0.5D, item.getZ(), strength, false, true);
                }
            } else if (entry.type() == HbmHazardRegistry.Type.EXPLOSIVE) {
                if (!com.atomiclegacy.config.HbmParityV3Config.disableExplosive() && item.isOnFire()) { // [parity V3]
                    item.discard();
                    LegacyVnt.vanilla(level, null, item.getX(), item.getY() + item.getBbHeight() * 0.5D, item.getZ(), strength, false, true);
                }
            }
        }
    }
}
