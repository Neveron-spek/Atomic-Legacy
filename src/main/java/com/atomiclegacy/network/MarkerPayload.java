package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [parity b2: assembly markers] AuxParticlePacketNT type "marker" (ClientProxy.effectNT): a coloured block outline with
 * an optional label that is drawn through walls until it expires or the player is farther away than dist.
 */
public record MarkerPayload(BlockPos pos, int color, int expires, double dist, String label) implements CustomPacketPayload {
    public static final Type<MarkerPayload> TYPE = new Type<>(AtomicLegacy.id("marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MarkerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            MarkerPayload::pos,
            ByteBufCodecs.INT,
            MarkerPayload::color,
            ByteBufCodecs.INT,
            MarkerPayload::expires,
            ByteBufCodecs.DOUBLE,
            MarkerPayload::dist,
            ByteBufCodecs.STRING_UTF8,
            MarkerPayload::label,
            MarkerPayload::new
    );

    /** MachinePWRController.sendError / MachineICFController.sendError: red, 5 000 ms, 128 blocks. */
    public static void sendError(ServerPlayer player, BlockPos pos, String message) {
        PacketDistributor.sendToPlayer(player, new MarkerPayload(pos.immutable(), 0xff0000, 5_000, 128D, message == null ? "" : message));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarkerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.atomiclegacy.client.OverheadMarkerRenderer.queue(payload);
            }
        });
    }
}
