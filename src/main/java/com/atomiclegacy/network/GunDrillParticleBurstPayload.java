package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [weapons batch F3] 1.7.10 ParticleBurstPacket (XFactoryDrill.breakExtraBlock): block destroy particles without the
 * break sound for the AoE blocks of the drill, sent only to the drilling player.
 */
public record GunDrillParticleBurstPayload(BlockPos pos, int blockState) implements CustomPacketPayload {
	public static final Type<GunDrillParticleBurstPayload> TYPE = new Type<>(AtomicLegacy.id("gun_drill_particle_burst"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunDrillParticleBurstPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public GunDrillParticleBurstPayload decode(RegistryFriendlyByteBuf buffer) {
			return new GunDrillParticleBurstPayload(buffer.readBlockPos(), buffer.readVarInt());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, GunDrillParticleBurstPayload payload) {
			buffer.writeBlockPos(payload.pos());
			buffer.writeVarInt(payload.blockState());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(GunDrillParticleBurstPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> com.atomiclegacy.client.gun.GunBatchF3ClientEffects.handleParticleBurst(payload));
	}
}
