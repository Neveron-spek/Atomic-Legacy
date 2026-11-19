package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 1.7.10 KeybindPacket for the four gun keybinds: forwards key state changes to the held gun. */
public record GunKeyPayload(int key, boolean pressed) implements CustomPacketPayload {
    public static final Type<GunKeyPayload> TYPE = new Type<>(AtomicLegacy.id("gun_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunKeyPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GunKeyPayload decode(RegistryFriendlyByteBuf buffer) {
            return new GunKeyPayload(buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GunKeyPayload payload) {
            buffer.writeVarInt(payload.key());
            buffer.writeBoolean(payload.pressed());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GunKeybind[] keys = GunKeybind.values();
            if (payload.key() < 0 || payload.key() >= keys.length) {
                return;
            }
            GunKeybind key = keys[payload.key()];
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof ItemGunBaseNT gun && gun.canHandleKeybind(player, held, key)) {
                gun.handleKeybind(player, held, key, payload.pressed());
            }
        });
    }
}
