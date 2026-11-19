package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.particle.BlackPowderEffects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * BlackPowderCreator's AuxParticlePacketNT {type: blackPowder, cloudCount, cloudScale, cloudSpeedMult,
 * sparkCount, sparkSpeedMult, hX, hY, hZ} at x/y/z.
 */
public record BlackPowderEffectPayload(double x, double y, double z, double headingX, double headingY, double headingZ,
		int cloudCount, float cloudScale, float cloudSpeedMult, int sparkCount, float sparkSpeedMult) implements CustomPacketPayload {

	public static final Type<BlackPowderEffectPayload> TYPE = new Type<>(AtomicLegacy.id("black_powder_effect"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlackPowderEffectPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public BlackPowderEffectPayload decode(RegistryFriendlyByteBuf buffer) {
			return new BlackPowderEffectPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readFloat());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, BlackPowderEffectPayload payload) {
			buffer.writeDouble(payload.x());
			buffer.writeDouble(payload.y());
			buffer.writeDouble(payload.z());
			buffer.writeDouble(payload.headingX());
			buffer.writeDouble(payload.headingY());
			buffer.writeDouble(payload.headingZ());
			buffer.writeInt(payload.cloudCount());
			buffer.writeFloat(payload.cloudScale());
			buffer.writeFloat(payload.cloudSpeedMult());
			buffer.writeInt(payload.sparkCount());
			buffer.writeFloat(payload.sparkSpeedMult());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BlackPowderEffectPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> BlackPowderEffects.makeParticle(payload));
	}
}
