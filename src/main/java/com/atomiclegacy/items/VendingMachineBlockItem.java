package com.atomiclegacy.items;

import net.minecraft.world.level.block.Block;

/** Metadata-compatible block item for Soda and Obamna vending-machine variants. */
public final class VendingMachineBlockItem extends LegacyVariantBlockItem {
    public VendingMachineBlockItem(Block block, Properties properties) {
        super(block, properties, com.atomiclegacy.blocks.VendingMachineBlock.VARIANT,
                "block.atomiclegacy.vending_machine", "soda", "snacks");
    }

    // [parity l1] item renderer: RenderVendingMachine#getRenderer via U4LegacyItemRenderSetup
}
