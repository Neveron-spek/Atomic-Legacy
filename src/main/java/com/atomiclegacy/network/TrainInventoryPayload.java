package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.train.CargoTramEntity;
import com.atomiclegacy.train.TrainMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [parity S1: trains] KeybindPacket(EnumKeybind.TRAIN): HbmPlayerProps opens the GUI of the ridden rail car if it is an
 * IGUIProvider (only TrainCargoTram; passengers ride a seat dummy and get nothing).
 */
public record TrainInventoryPayload() implements CustomPacketPayload {
    public static final Type<TrainInventoryPayload> TYPE = new Type<>(AtomicLegacy.id("train_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrainInventoryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TrainInventoryPayload decode(RegistryFriendlyByteBuf buffer) {
            return new TrainInventoryPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TrainInventoryPayload payload) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrainInventoryPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof CargoTramEntity tram) {
            TrainMenus.open(player, tram);
        }
    }
}
