package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.SoyuzLauncherMenu;
import com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** [parity P2: soyuz_launcher] AuxButtonPacket for TileEntitySoyuzLauncher: id 0 sets mode = value, id 1 starts the countdown. */
public record SoyuzLauncherControlPayload(BlockPos pos, int value, int id) implements CustomPacketPayload {
    public static final Type<SoyuzLauncherControlPayload> TYPE = new Type<>(AtomicLegacy.id("soyuz_launcher_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoyuzLauncherControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SoyuzLauncherControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoyuzLauncherControlPayload(buffer.readBlockPos(), buffer.readInt(), buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SoyuzLauncherControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeInt(payload.value());
            buffer.writeInt(payload.id());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoyuzLauncherControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof SoyuzLauncherMenu menu)
                || !menu.blockPos().equals(payload.pos()) || !menu.stillValid(player)
                || !(player.level().getBlockEntity(payload.pos()) instanceof SoyuzLauncherBlockEntity launcher)) {
            return;
        }
        if (payload.id() == 0) launcher.setMode(payload.value());
        if (payload.id() == 1) launcher.startCountdown();
        player.containerMenu.broadcastChanges();
    }
}
