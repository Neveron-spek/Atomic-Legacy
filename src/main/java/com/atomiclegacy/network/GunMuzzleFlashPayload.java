package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunClientHandler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 1.7.10 MuzzleFlashPacket: third-person muzzle flash of another entity's gun. */
public record GunMuzzleFlashPayload(int entityId) implements CustomPacketPayload {
    public static final Type<GunMuzzleFlashPayload> TYPE = new Type<>(AtomicLegacy.id("gun_muzzle_flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunMuzzleFlashPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GunMuzzleFlashPayload decode(RegistryFriendlyByteBuf buffer) {
            return new GunMuzzleFlashPayload(buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GunMuzzleFlashPayload payload) {
            buffer.writeInt(payload.entityId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunMuzzleFlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunClientHandler.handleMuzzleFlash(payload));
    }
}
