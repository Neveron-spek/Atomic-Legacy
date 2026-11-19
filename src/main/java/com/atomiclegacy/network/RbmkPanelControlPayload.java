package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.RbmkPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** [parity T2] NBTControlPacket of the Redstone-over-Radio panel GUIs (keys of the 1.7.10 receiveControl). */
public record RbmkPanelControlPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
    public static final Type<RbmkPanelControlPayload> TYPE = new Type<>(AtomicLegacy.id("rbmk_panel_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RbmkPanelControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RbmkPanelControlPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, RbmkPanelControlPayload::data,
            RbmkPanelControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RbmkPanelControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !player.level().isLoaded(payload.pos())
                || !(player.level().getBlockEntity(payload.pos()) instanceof RbmkPanelBlockEntity panel)
                || !panel.hasPermission(player)) {
            return;
        }
        panel.receiveControl(player, payload.data());
    }
}
