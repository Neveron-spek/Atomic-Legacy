package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunBatchBClientEffects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * ExplosionEffectTiny's AuxParticlePacketNT: {type: vanillaExt, mode: largeexplode, size, count},
 * sent to all around within 100 blocks.
 */
public record GunTinyExplosionPayload(double x, double y, double z, float size, int count) implements CustomPacketPayload {

	public static final Type<GunTinyExplosionPayload> TYPE = new Type<>(AtomicLegacy.id("gun_tiny_explosion"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunTinyExplosionPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public GunTinyExplosionPayload decode(RegistryFriendlyByteBuf buffer) {
			return new GunTinyExplosionPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readByte());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, GunTinyExplosionPayload payload) {
			buffer.writeDouble(payload.x());
			buffer.writeDouble(payload.y());
			buffer.writeDouble(payload.z());
			buffer.writeFloat(payload.size());
			buffer.writeByte(payload.count());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(GunTinyExplosionPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> GunBatchBClientEffects.largeExplode(payload));
	}
}
