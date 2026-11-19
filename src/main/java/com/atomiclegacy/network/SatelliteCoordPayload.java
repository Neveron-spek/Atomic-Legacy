package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacySatelliteToolItem;
import com.atomiclegacy.items.SatelliteChipItem;
import com.atomiclegacy.satellite.SatelliteSavedData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [parity P2: sat_coord GUI] NBTItemControlPacket sent by GUIScreenSatCoord
 * and handled by ItemSatInterface#receiveControl: the held sat_coord forwards
 * x/z (and y, or -1 when the field was empty) to SatelliteBase#onCoordAction.
 */
public record SatelliteCoordPayload(int x, boolean hasY, int y, int z) implements CustomPacketPayload {
    public static final Type<SatelliteCoordPayload> TYPE = new Type<>(AtomicLegacy.id("satellite_coord"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SatelliteCoordPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SatelliteCoordPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SatelliteCoordPayload(buffer.readInt(), buffer.readBoolean(), buffer.readInt(), buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SatelliteCoordPayload payload) {
            buffer.writeInt(payload.x());
            buffer.writeBoolean(payload.hasY());
            buffer.writeInt(payload.y());
            buffer.writeInt(payload.z());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SatelliteCoordPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof LegacySatelliteToolItem tool) || !tool.isCoordinateInterface()) {
            return;
        }
        SatelliteSavedData data = SatelliteSavedData.get(level);
        data.satellite(SatelliteChipItem.frequency(held)).ifPresent(record ->
                data.performCoordinateAction(level, player, record,
                        payload.x(), payload.hasY() ? payload.y() : -1, payload.z()));
    }
}
