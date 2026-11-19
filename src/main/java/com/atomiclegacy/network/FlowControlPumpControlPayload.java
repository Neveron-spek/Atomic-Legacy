package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** [parity T1: flow_control_pump] NBTControlPacket of GUIPump (keys capacity, pressure, priority). */
public record FlowControlPumpControlPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
    public static final Type<FlowControlPumpControlPayload> TYPE = new Type<>(AtomicLegacy.id("flow_control_pump_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlowControlPumpControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FlowControlPumpControlPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, FlowControlPumpControlPayload::data,
            FlowControlPumpControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlowControlPumpControlPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().isLoaded(payload.pos())
                && player.level().getBlockEntity(payload.pos()) instanceof FlowControlPumpBlockEntity pump) {
            pump.receiveControl(player, payload.data()); // hasPermission: distance squared <= 128
        }
    }
}
