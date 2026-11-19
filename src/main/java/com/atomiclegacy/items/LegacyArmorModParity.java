package com.atomiclegacy.items;

import com.atomiclegacy.network.ArmorParityPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * [parity I2] Small pieces of 1.7.10 HbmPlayerProps used by armor mods and jetpacks:
 * enableBackpack / enableHUD toggles (keys C and V), plink() with its 10-tick cooldown.
 * Server state lives in the player's persistent data (saved with the player); the client
 * copy is synced by {@link ArmorParityPayloads.TogglesSync}.
 */
public final class LegacyArmorModParity {
    private static final String ROOT = "hbm_parity_i2";
    private static final String BACKPACK_OFF = "backpackOff";
    private static final String HUD_OFF = "hudOff";
    private static final String PLINK = "plinkTime";
    public static final int PLINK_COOLDOWN = 10;

    /** Client-side mirror of the local player's toggles. */
    public static boolean clientBackpack = true;
    public static boolean clientHud = true;

    private LegacyArmorModParity() {
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    public static boolean enableBackpack(Player player) {
        if (player.level().isClientSide) return clientBackpack;
        return !data(player).getBoolean(BACKPACK_OFF);
    }

    public static boolean enableHUD(Player player) {
        if (player.level().isClientSide) return clientHud;
        return !data(player).getBoolean(HUD_OFF);
    }

    /** HbmPlayerProps#isJetpackActive: enableBackpack && JETPACK (jump) key held. */
    public static boolean isJetpackActive(Player player) {
        return enableBackpack(player) && LegacyJetpackItem.isJumpHeld(player);
    }

    public static void toggle(ServerPlayer player, int which) {
        CompoundTag tag = data(player);
        if (which == 0) {
            boolean enabled = tag.getBoolean(BACKPACK_OFF);
            tag.putBoolean(BACKPACK_OFF, !enabled);
            // [parity z1] PlayerInformPacket(GREEN "Jetpack ON" / RED "Jetpack OFF", ID_JETPACK = 5, 1000)
            PacketDistributor.sendToPlayer(player, new com.atomiclegacy.network.PlayerInformPayload(5,
                    enabled ? "Jetpack ON" : "Jetpack OFF", enabled ? 0x55FF55 : 0xFF5555, 1000));
        } else if (which == 1) {
            boolean enabled = tag.getBoolean(HUD_OFF);
            tag.putBoolean(HUD_OFF, !enabled);
            // [parity z1] PlayerInformPacket(GREEN "HUD ON" / RED "HUD OFF", ID_HUD = 7, 1000)
            PacketDistributor.sendToPlayer(player, new com.atomiclegacy.network.PlayerInformPayload(7,
                    enabled ? "HUD ON" : "HUD OFF", enabled ? 0x55FF55 : 0xFF5555, 1000));
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ArmorParityPayloads.TogglesSync(enableBackpack(player), enableHUD(player)));
    }

    /** Creeper DataWatcher 18 (ignited) of 1.7.10. */
    public static void setCreeperIgnited(net.minecraft.world.entity.monster.Creeper creeper, boolean value) {
        setCreeperFlag(creeper, "DATA_IS_IGNITED", value);
    }

    /** Creeper DataWatcher 17 (powered) of 1.7.10. */
    public static void setCreeperPowered(net.minecraft.world.entity.monster.Creeper creeper, boolean value) {
        setCreeperFlag(creeper, "DATA_IS_POWERED", value);
    }

    @SuppressWarnings("unchecked")
    private static void setCreeperFlag(net.minecraft.world.entity.monster.Creeper creeper, String field, boolean value) {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.monster.Creeper.class.getDeclaredField(field);
            f.setAccessible(true);
            creeper.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Boolean>) f.get(null), value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /** HbmPlayerProps.plink: playSoundAtEntity, then 10 ticks of cooldown. */
    public static void plink(Player player, SoundEvent sound, float volume, float pitch) {
        if (player.level().isClientSide) return;
        CompoundTag tag = data(player);
        long now = player.level().getGameTime();
        if (!tag.contains(PLINK) || now - tag.getLong(PLINK) >= PLINK_COOLDOWN) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
            tag.putLong(PLINK, now);
        }
    }
}
