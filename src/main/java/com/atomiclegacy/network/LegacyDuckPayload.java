package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyDuckEntity;
import com.atomiclegacy.player.HbmLegacyMobSpawnState;
import com.atomiclegacy.registry.HbmDataAttachments;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server side of HBM 1.7.10's AuxButtonPacket value 999. */
public record LegacyDuckPayload() implements CustomPacketPayload {
    public static final Type<LegacyDuckPayload> TYPE = new Type<>(AtomicLegacy.id("legacy_duck"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LegacyDuckPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LegacyDuckPayload decode(RegistryFriendlyByteBuf buffer) {
            return new LegacyDuckPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LegacyDuckPayload payload) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LegacyDuckPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        HbmLegacyMobSpawnState state = player.getData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE);
        if (!HbmConfig.ENABLE_DUCKS.get() || state.hasDucked()) {
            return;
        }

        LegacyDuckEntity duck = new LegacyDuckEntity(HbmEntityTypes.DUCK.get(), player.level());
        duck.setPos(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        duck.setDeltaMovement(player.getLookAngle());
        player.level().addFreshEntity(duck);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.DUCC.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.setData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE,
                new HbmLegacyMobSpawnState(state.fbiMarkUntil(), state.radMarked(), true));
    }
}
