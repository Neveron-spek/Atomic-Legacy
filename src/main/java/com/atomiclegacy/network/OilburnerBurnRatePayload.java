package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.HeaterBlockEntity;
import com.atomiclegacy.inventory.container.HeaterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** [parity L3: oilburner] NBTControlPacket "burnRate" from GUIOilburner. */
public record OilburnerBurnRatePayload(BlockPos pos, int burnRate) implements CustomPacketPayload {
    public static final Type<OilburnerBurnRatePayload> TYPE = new Type<>(AtomicLegacy.id("oilburner_burn_rate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OilburnerBurnRatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OilburnerBurnRatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new OilburnerBurnRatePayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, OilburnerBurnRatePayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.burnRate());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OilburnerBurnRatePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof HeaterMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof HeaterBlockEntity heater && heater.kind() == HeaterBlockEntity.Kind.OILBURNER) {
            heater.setOilburnerBurnRate(payload.burnRate());
            player.containerMenu.broadcastChanges();
        }
    }
}
