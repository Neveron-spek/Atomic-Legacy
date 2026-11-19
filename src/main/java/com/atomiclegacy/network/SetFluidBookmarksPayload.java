package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** [parity e2] GUIScreenFluid#onGuiClosed: NBTItemControlPacket with the "bookmarks" list of the multi fluid identifier. */
public record SetFluidBookmarksPayload(List<String> fluidNames) implements CustomPacketPayload {
    public static final int MAX_BOOKMARKS = 18;
    public static final Type<SetFluidBookmarksPayload> TYPE = new Type<>(AtomicLegacy.id("set_fluid_bookmarks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidBookmarksPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_BOOKMARKS)),
            SetFluidBookmarksPayload::fluidNames,
            SetFluidBookmarksPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetFluidBookmarksPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        List<String> names = new ArrayList<>();
        for (String name : payload.fluidNames()) {
            if (names.size() < MAX_BOOKMARKS && HbmFluids.byName(name).isPresent() && !names.contains(name)) {
                names.add(name);
            }
        }
        FluidIdentifierItem.setBookmarks(stack, names);
        player.containerMenu.broadcastChanges();
    }
}
