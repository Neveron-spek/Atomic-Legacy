package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyArmorModParity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * [parity I2] Network messages of the armor-mod / jetpack parity batch:
 * toggle keys (HbmKeybinds toggleBack/toggleHUD), the synced toggle state, and the
 * AuxParticlePacketNT effects "jetpack" and "properJolt".
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ArmorParityPayloads {
    private ArmorParityPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("i2");
        registrar.playToServer(ToggleKey.TYPE, ToggleKey.STREAM_CODEC, ToggleKey::handle);
        registrar.playToClient(TogglesSync.TYPE, TogglesSync.STREAM_CODEC, TogglesSync::handle);
        registrar.playToClient(Effect.TYPE, Effect.STREAM_CODEC, Effect::handle);
    }

    /** 0 = toggleBack (C), 1 = toggleHUD (V). */
    public record ToggleKey(int which) implements CustomPacketPayload {
        public static final Type<ToggleKey> TYPE = new Type<>(AtomicLegacy.id("parity_i2_toggle_key"));
        public static final StreamCodec<ByteBuf, ToggleKey> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(ToggleKey::new, ToggleKey::which);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ToggleKey payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                LegacyArmorModParity.toggle(player, payload.which());
            }
        }
    }

    public record TogglesSync(boolean backpack, boolean hud) implements CustomPacketPayload {
        public static final Type<TogglesSync> TYPE = new Type<>(AtomicLegacy.id("parity_i2_toggles"));
        public static final StreamCodec<ByteBuf, TogglesSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, TogglesSync::backpack, ByteBufCodecs.BOOL, TogglesSync::hud, TogglesSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(TogglesSync payload, IPayloadContext context) {
            LegacyArmorModParity.clientBackpack = payload.backpack();
            LegacyArmorModParity.clientHud = payload.hud();
        }
    }

    /** type 0: "jetpack" (a = mode); type 1: "properJolt" (a = time, b = maxTime). */
    public record Effect(int kind, int entity, int a, int b) implements CustomPacketPayload {
        public static final int JETPACK = 0;
        public static final int PROPER_JOLT = 1;
        public static final Type<Effect> TYPE = new Type<>(AtomicLegacy.id("parity_i2_effect"));
        public static final StreamCodec<ByteBuf, Effect> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Effect::kind, ByteBufCodecs.VAR_INT, Effect::entity,
                ByteBufCodecs.VAR_INT, Effect::a, ByteBufCodecs.VAR_INT, Effect::b, Effect::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Effect payload, IPayloadContext context) {
            context.enqueueWork(() -> com.atomiclegacy.client.ArmorParityClient.handleEffect(payload));
        }
    }
}
