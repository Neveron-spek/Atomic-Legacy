package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunBatchD1ClientEffects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [weapons batch D1] AuxParticlePacketNT {type: plasmablast, r, g, b, pitch, yaw, scale} of
 * XFactory12ga.spawnPulse, sent to all around within 100 blocks.
 */
public record GunPlasmaPulsePayload(double x, double y, double z, float r, float g, float b, float pitch, float yaw, float scale) implements CustomPacketPayload {

	public static final Type<GunPlasmaPulsePayload> TYPE = new Type<>(AtomicLegacy.id("gun_plasma_pulse"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunPlasmaPulsePayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public GunPlasmaPulsePayload decode(RegistryFriendlyByteBuf buffer) {
			return new GunPlasmaPulsePayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, GunPlasmaPulsePayload payload) {
			buffer.writeDouble(payload.x());
			buffer.writeDouble(payload.y());
			buffer.writeDouble(payload.z());
			buffer.writeFloat(payload.r());
			buffer.writeFloat(payload.g());
			buffer.writeFloat(payload.b());
			buffer.writeFloat(payload.pitch());
			buffer.writeFloat(payload.yaw());
			buffer.writeFloat(payload.scale());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(GunPlasmaPulsePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> GunBatchD1ClientEffects.plasmaPulse(payload));
	}
}
