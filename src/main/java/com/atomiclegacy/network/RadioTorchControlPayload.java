package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.RttyConfigMenu;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [parity R2] NBTControlPacket for the RTTY sender, receiver, logic receiver and item counter GUIs.
 * The compound carries the exact 1.7.10 keys consumed by RadioTorchBlockEntity.receiveControl.
 */
public record RadioTorchControlPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
    public static final Type<RadioTorchControlPayload> TYPE = new Type<>(AtomicLegacy.id("radio_torch_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadioTorchControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RadioTorchControlPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, RadioTorchControlPayload::data,
            RadioTorchControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadioTorchControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof RttyConfigMenu menu)
                || !menu.blockPos().equals(payload.pos())
                || !(player.level().getBlockEntity(payload.pos()) instanceof RadioTorchBlockEntity radio)
                || radio.kind() != menu.kind()) {
            return;
        }
        radio.receiveControl(player, payload.data());
    }
}
