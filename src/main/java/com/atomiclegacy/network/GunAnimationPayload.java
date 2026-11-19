package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunClientHandler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 1.7.10 HbmAnimationPacket (sedna branch): animation type, receiver index, gun config index. */
public record GunAnimationPayload(int animType, int receiverIndex, int itemIndex) implements CustomPacketPayload {
    public static final Type<GunAnimationPayload> TYPE = new Type<>(AtomicLegacy.id("gun_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunAnimationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GunAnimationPayload decode(RegistryFriendlyByteBuf buffer) {
            return new GunAnimationPayload(buffer.readShort(), buffer.readInt(), buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GunAnimationPayload payload) {
            buffer.writeShort(payload.animType());
            buffer.writeInt(payload.receiverIndex());
            buffer.writeInt(payload.itemIndex());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunClientHandler.handleAnimation(payload));
    }
}
