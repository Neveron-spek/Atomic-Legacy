package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.BallisticGauntletItem;
import com.atomiclegacy.items.LegacyArmorModParity;
import com.atomiclegacy.items.LegacyJetpackItem;
import com.atomiclegacy.items.LegacyWingsItem;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.factory.Lego;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * [parity I2] Common-side hooks of the armor-mod batch:
 * onArmorTick of jetpacks/wings worn directly in the chest slot (both sides, like 1.7.10),
 * toggle sync, and ModEventHandler#onPlayerPunch for the ballistic gauntlet.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class ArmorParityEvents {
    private ArmorParityEvents() {
    }

    /** Item#onArmorTick for the chest slot. Installed jetpacks are ticked by the armor-mod loop on the server
     * and by the client handler for the local player. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide && !player.isLocalPlayer()) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof LegacyJetpackItem jetpack) {
            jetpack.onArmorTick(player.level(), player, chest);
        } else if (chest.getItem() instanceof LegacyWingsItem wings) {
            wings.onArmorTick(player.level(), player, chest);
        } else if (player.level().isClientSide && !chest.isEmpty()) {
            ItemStack installed = ArmorModHandler.pryMods(chest, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
            if (installed.getItem() instanceof LegacyJetpackItem jetpack) {
                jetpack.onArmorTick(player.level(), player, installed);
            } else if (installed.getItem() instanceof LegacyWingsItem wings) {
                wings.onArmorTick(player.level(), player, installed);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LegacyArmorModParity.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LegacyArmorModParity.sync(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains("hbm_parity_i2")) {
            event.getEntity().getPersistentData().put("hbm_parity_i2", old.getCompound("hbm_parity_i2").copy());
        }
    }

    /** ModEventHandler#onPlayerPunch: "only for the ballistic gauntlet! contains dangerous conditional returns!" */
    @SubscribeEvent
    public static void onPlayerPunch(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (player.level().isClientSide || chestplate.isEmpty()) return;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getAttributeModifiers().modifiers().stream()
                .anyMatch(entry -> entry.attribute().is(Attributes.ATTACK_DAMAGE))) return;
        ItemStack servo = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.SERVOS];
        if (!(servo.getItem() instanceof BallisticGauntletItem)) return;

        BulletConfig fired = null;
        BulletConfig[] configs = gauntletConfigs();
        for (BulletConfig config : configs) {
            if (consumeAmmo(player, config)) {
                fired = config;
                break;
            }
        }
        if (fired == null) return;
        int bullets = fired.projectilesMin();
        if (fired.projectilesMax() > fired.projectilesMin()) {
            bullets += player.getRandom().nextInt(fired.projectilesMax() - fired.projectilesMin());
        }
        for (int i = 0; i < bullets; i++) {
            var mk4 = Lego.spawnBullet(player, fired, 15F, 0F, -0.1875, -0.0625, 0.5);
            if (i == 0 && fired.ammo.blackPowder()) {
                com.atomiclegacy.particle.helper.BlackPowderCreator.composeEffect(player.level(), mk4.getX(), mk4.getY(), mk4.getZ(),
                        mk4.getDeltaMovement().x, mk4.getDeltaMovement().y, mk4.getDeltaMovement().z, 10, 0.25F, 0.5F, 10, 0.25F);
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.GUN_SPAS_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** DT of ArmorHat (2) and ArmorNo9 (0.5); 0 when neither is worn. */
    private static float helmetDT(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(HbmItems.HAT.get())) return 2F;
        if (head.is(HbmItems.NO9.get())) return 0.5F;
        return 0F;
    }

    /** IAttackHandler#handleAttack of ArmorHat / ArmorNo9: blockable hits up to the DT are cancelled
     * (the hat also plays random.break 5.0 / 1 + rand * 0.5). */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onHelmetAttack(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        float dt = helmetDT(player);
        if (dt <= 0F || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return;
        if (event.getAmount() <= dt) {
            if (dt == 2F) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                        SoundSource.PLAYERS, 5F, 1.0F + player.getRandom().nextFloat() * 0.5F);
            }
            event.setCanceled(true);
        }
    }

    /** IDamageHandler#handleDamage (after the armor mods): amount -= DT, not below 0. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOW)
    public static void onHelmetDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        float dt = helmetDT(player);
        if (dt <= 0F || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return;
        event.setAmount(Math.max(0F, event.getAmount() - dt));
    }

    /** ArmorHat#onEntityItemUpdate: a dropped hat vanishes. */
    @SubscribeEvent
    public static void onHatDropped(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                && item.getItem().is(HbmItems.HAT.get())) {
            event.setCanceled(true);
        }
    }

    public static BulletConfig[] gauntletConfigs() {
        return new BulletConfig[] {XFactory12ga.g12_bp, XFactory12ga.g12_bp_magnum, XFactory12ga.g12_bp_slug, XFactory12ga.g12,
                XFactory12ga.g12_slug, XFactory12ga.g12_flechette, XFactory12ga.g12_magnum, XFactory12ga.g12_explosive, XFactory12ga.g12_phosphorus};
    }

    /** InventoryUtil.doesPlayerHaveAStack(player, config.ammo, true, true): main inventory, removes one. */
    public static boolean consumeAmmo(Player player, BulletConfig config) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (config.matches(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) items.set(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }
}
