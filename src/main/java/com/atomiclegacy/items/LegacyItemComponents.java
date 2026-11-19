package com.atomiclegacy.items;

import com.mojang.serialization.Codec;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components used by standalone 1.7.10 item variants. */
public final class LegacyItemComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, AtomicLegacy.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DRONE_TYPE = COMPONENTS.register(
            "drone_type", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).build()
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> DRONE_LINK_ORIGIN = COMPONENTS.register(
            "drone_link_origin", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build()
    );

    // [parity S1: carts/trains] ItemModMinecart damage (EnumMinecart) + "cartBase" NBT, ItemTrain damage (EnumTrainType)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CART_TYPE = COMPONENTS.register(
            "cart_type", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build()
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CART_BASE = COMPONENTS.register(
            "cart_base", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build()
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TRAIN_TYPE = COMPONENTS.register(
            "train_type", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build()
    );
    /** [parity V3: hot items] ItemHotDusted metadata (forging grade 0..9); 1.21 damage needs max_damage, so a component. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DUSTED_GRADE = COMPONENTS.register(
            "dusted_grade", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build()
    );

    private LegacyItemComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
