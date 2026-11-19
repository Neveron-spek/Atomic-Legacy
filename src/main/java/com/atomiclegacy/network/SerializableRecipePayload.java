package com.atomiclegacy.network;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * [parity b2: custom machines] com.hbm.packet.toclient.SerializableRecipePacket: one packet per recipe file
 * (file name + raw bytes), then a packet with reinit set that makes the client re-run SerializableRecipe.initialize().
 */
public record SerializableRecipePayload(boolean reinit, String filename, byte[] fileBytes) implements CustomPacketPayload {
    public static final Type<SerializableRecipePayload> TYPE = new Type<>(AtomicLegacy.id("serializable_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SerializableRecipePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SerializableRecipePayload decode(RegistryFriendlyByteBuf buf) {
            boolean reinit = buf.readBoolean();
            if (reinit) {
                return reinitialize();
            }
            String filename = buf.readUtf();
            byte[] bytes = new byte[buf.readInt()];
            buf.readBytes(bytes);
            return new SerializableRecipePayload(false, filename, bytes);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SerializableRecipePayload payload) {
            buf.writeBoolean(payload.reinit);
            if (payload.reinit) {
                return;
            }
            buf.writeUtf(payload.filename);
            buf.writeInt(payload.fileBytes.length);
            buf.writeBytes(payload.fileBytes);
        }
    };

    public static SerializableRecipePayload reinitialize() {
        return new SerializableRecipePayload(true, "", new byte[0]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SerializableRecipePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                // Only reinitialize after receiving all recipes
                if (payload.reinit) {
                    CustomMachineRecipes.initialize();
                    return;
                }
                CustomMachineRecipes.receiveRecipes(payload.filename, payload.fileBytes);
            } catch (Exception ignored) {
            }
        });
    }
}
