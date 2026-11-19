package com.atomiclegacy.items;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Metadata-compatible block item for Soda and Obamna vending-machine variants. */
public final class VendingMachineBlockItem extends LegacyVariantBlockItem {
    public VendingMachineBlockItem(Block block, Properties properties) {
        super(block, properties, com.atomiclegacy.blocks.VendingMachineBlock.VARIANT,
                "block.atomiclegacy.vending_machine", "soda", "snacks");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
