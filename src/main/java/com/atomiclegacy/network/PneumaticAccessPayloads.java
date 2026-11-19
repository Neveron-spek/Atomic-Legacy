package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.PneumaticStorageMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity V2: storage] ContainerNBTCommsPacket traffic of ContainerPneumoStorageAccess: the server sends the access
 * cache (type + amount, CacheSlotDummy) and the client sends LEFT_CLICK / RIGHT_CLICK / SHIFT_CLICK on a grid stack.
 * Sorting, search and scrolling stay client-side as in GUIPneumoStorageAccess / rebuildClientIndex.
 */
public final class PneumaticAccessPayloads {
    private PneumaticAccessPayloads() {
    }

    public record Entry(ItemStack stack, long amount) {
    }

    public record Sync(int containerId, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(AtomicLegacy.id("pneumatic_access_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Sync decode(RegistryFriendlyByteBuf buffer) {
                int id = buffer.readVarInt();
                int size = buffer.readVarInt();
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
                    entries.add(new Entry(stack, buffer.readVarLong()));
                }
                return new Sync(id, entries);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Sync payload) {
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    ItemStack.STREAM_CODEC.encode(buffer, entry.stack());
                    buffer.writeVarLong(entry.amount());
                }
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync payload, IPayloadContext context) {
            if (context.player().containerMenu instanceof PneumaticStorageMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptAccessEntries(payload.entries());
            }
        }
    }

    /** type: 0 LEFT_CLICK, 1 RIGHT_CLICK, 2 SHIFT_CLICK; stack: the clicked grid stack (empty for an empty slot). */
    public record Click(int containerId, int clickType, ItemStack stack) implements CustomPacketPayload {
        public static final Type<Click> TYPE = new Type<>(AtomicLegacy.id("pneumatic_access_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Click> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Click decode(RegistryFriendlyByteBuf buffer) {
                return new Click(buffer.readVarInt(), buffer.readByte(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Click payload) {
                buffer.writeVarInt(payload.containerId());
                buffer.writeByte(payload.clickType());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Click payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PneumaticStorageMenu menu
                    && menu.containerId == payload.containerId() && menu.stillValid(player)) {
                menu.handleAccessClick(player, payload.clickType(), payload.stack());
            }
        }
    }
}
