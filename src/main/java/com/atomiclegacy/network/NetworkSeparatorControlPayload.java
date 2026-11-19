package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** [parity U1: network_separator] NBTControlPacket of GUIDiode (keys limit, priority). */
public record NetworkSeparatorControlPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
    public static final Type<NetworkSeparatorControlPayload> TYPE = new Type<>(AtomicLegacy.id("network_separator_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkSeparatorControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NetworkSeparatorControlPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, NetworkSeparatorControlPayload::data,
            NetworkSeparatorControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NetworkSeparatorControlPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().isLoaded(payload.pos())
                && player.level().getBlockEntity(payload.pos()) instanceof NetworkSeparatorBlockEntity separator) {
            separator.receiveControl(player, payload.data());
        }
    }
}
