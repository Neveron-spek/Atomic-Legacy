package com.atomiclegacy.network;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.GunBatchC2ClientEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * ExplosionVanillaNewTechnologyCompressedAffectedBlockPositionDataForClientEffectsAndParticleHandlingPacket
 * (ExplosionEffectStandard, sent to all around within 250 blocks): float position, size and the affected
 * block positions as byte offsets from the truncated explosion position, exactly like the original encoding.
 */
public record VntStandardExplosionPayload(double x, double y, double z, float size, List<BlockPos> affectedBlocks) implements CustomPacketPayload {

	public static final Type<VntStandardExplosionPayload> TYPE = new Type<>(AtomicLegacy.id("vnt_standard_explosion"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VntStandardExplosionPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public VntStandardExplosionPayload decode(RegistryFriendlyByteBuf buf) {
			double posX = buf.readFloat();
			double posY = buf.readFloat();
			double posZ = buf.readFloat();
			float size = buf.readFloat();
			int i = buf.readInt();
			List<BlockPos> blocks = new ArrayList<>(i);
			int j = (int) posX;
			int k = (int) posY;
			int l = (int) posZ;
			for(int i1 = 0; i1 < i; ++i1) {
				int j1 = buf.readByte() + j;
				int k1 = buf.readByte() + k;
				int l1 = buf.readByte() + l;
				blocks.add(new BlockPos(j1, k1, l1));
			}
			return new VntStandardExplosionPayload(posX, posY, posZ, size, blocks);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, VntStandardExplosionPayload payload) {
			buf.writeFloat((float) payload.x());
			buf.writeFloat((float) payload.y());
			buf.writeFloat((float) payload.z());
			buf.writeFloat(payload.size());
			buf.writeInt(payload.affectedBlocks().size());
			int i = (int) payload.x();
			int j = (int) payload.y();
			int k = (int) payload.z();
			for(BlockPos pos : payload.affectedBlocks()) {
				buf.writeByte(pos.getX() - i);
				buf.writeByte(pos.getY() - j);
				buf.writeByte(pos.getZ() - k);
			}
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(VntStandardExplosionPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> GunBatchC2ClientEffects.standardExplosion(payload));
	}
}
