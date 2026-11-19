package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.inventory.container.RadarMenu;
import com.atomiclegacy.inventory.container.RadarSlotsMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative controls copied from GUIMachineRadarNT. */
public record RadarControlPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final Type<RadarControlPayload> TYPE = new Type<>(AtomicLegacy.id("radar_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public RadarControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RadarControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, RadarControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RadarControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || (!(player.containerMenu instanceof RadarMenu menu) && !(player.containerMenu instanceof RadarSlotsMenu))) return;
        // [e3] only the radar whose GUI is open (also keeps the lookup inside a loaded chunk)
        BlockPos open = player.containerMenu instanceof RadarSlotsMenu slots ? slots.blockPos() : ((RadarMenu) player.containerMenu).blockPos();
        if (!open.equals(payload.pos())) return;
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (!(entity instanceof LegacyMachineBlockEntity machine)
                || (!machine.machineId().equals("machine_radar") && !machine.machineId().equals("machine_radar_large"))
                || !player.containerMenu.stillValid(player)) return;
        if (payload.action() == 7 && player.containerMenu instanceof RadarMenu) {
            machine.openRadarSlots(player);
        } else if (payload.action() == 7) {
            // [parity g2] GUIMachineRadarNTSlots (5,5) button: back to the radar screen (GUI id 0)
            player.openMenu(machine, buffer -> buffer.writeBlockPos(payload.pos()));
        } else if (payload.action() >= 0 && payload.action() <= 6) {
            machine.applyRadarControl(payload.action());
            player.containerMenu.broadcastChanges();
        }
    }
}
