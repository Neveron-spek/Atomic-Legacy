package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunBatchF2ClientEffects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [weapons batch F2] AuxParticlePacketNT {type: "muke" | "tinytot", balefire} of the Fat Man mini nukes
 * (XFactoryCatapult.spawnMush / LAMBDA_NUKE_TINYTOT / LAMBDA_NUKE_BALEFIRE), sent to everyone within 250 blocks.
 */
public record GunMukeEffectPayload(double x, double y, double z, int mode, boolean balefire) implements CustomPacketPayload {

	public static final int MODE_MUKE = 0;
	public static final int MODE_TINYTOT = 1;

	public static final Type<GunMukeEffectPayload> TYPE = new Type<>(AtomicLegacy.id("gun_muke_effect"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunMukeEffectPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public GunMukeEffectPayload decode(RegistryFriendlyByteBuf buffer) {
			return new GunMukeEffectPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readByte(), buffer.readBoolean());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, GunMukeEffectPayload payload) {
			buffer.writeDouble(payload.x());
			buffer.writeDouble(payload.y());
			buffer.writeDouble(payload.z());
			buffer.writeByte(payload.mode());
			buffer.writeBoolean(payload.balefire());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(GunMukeEffectPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> GunBatchF2ClientEffects.mukeEffect(payload));
	}
}
