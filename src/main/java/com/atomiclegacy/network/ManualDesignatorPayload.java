package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyManualDesignatorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** [parity P1: designator_manual] 1.7.10 ItemDesignatorPacket(operator, value, reference) for the held manual designator. */
public record ManualDesignatorPayload(int operator, int value, int reference) implements CustomPacketPayload {
    public static final Type<ManualDesignatorPayload> TYPE = new Type<>(AtomicLegacy.id("manual_designator"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManualDesignatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManualDesignatorPayload::operator,
            ByteBufCodecs.VAR_INT, ManualDesignatorPayload::value,
            ByteBufCodecs.VAR_INT, ManualDesignatorPayload::reference,
            ManualDesignatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManualDesignatorPayload m, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer p)) return;
            ItemStack stack = p.getMainHandItem();
            if (stack.getItem() instanceof LegacyManualDesignatorItem) {
                LegacyManualDesignatorItem.apply(stack, m.operator(), m.value(), m.reference(), p.getX(), p.getZ());
                p.getInventory().setChanged();
            }
        });
    }
}
