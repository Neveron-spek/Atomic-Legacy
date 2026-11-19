package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunClientHandler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 1.7.10 HbmAnimationPacket, IAnimatedItem branch (handleItem). */
public record ItemAnimationPayload(int animType, int itemIndex) implements CustomPacketPayload {
    public static final Type<ItemAnimationPayload> TYPE = new Type<>(AtomicLegacy.id("item_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemAnimationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ItemAnimationPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ItemAnimationPayload(buffer.readShort(), buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ItemAnimationPayload payload) {
            buffer.writeShort(payload.animType());
            buffer.writeInt(payload.itemIndex());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunClientHandler.handleItemAnimation(payload));
    }
}
