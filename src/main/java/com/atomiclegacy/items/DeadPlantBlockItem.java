package com.atomiclegacy.items;

import com.atomiclegacy.blocks.DeadPlantBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * [parity m1] BlockDeadPlant (BlockEnumMulti, multiName = false, multiTexture = true): five metadata variants in
 * EnumDeadPlantType order, all named tile.plant_dead.name.
 */
public final class DeadPlantBlockItem extends LegacyVariantBlockItem {
    public DeadPlantBlockItem(Block block, Properties properties) {
        super(block, properties, DeadPlantBlock.KIND, "block.atomiclegacy.plant_dead",
                "generic", "grass", "flower", "big_flower", "fern");
    }

    /** multiName = false: getUnlocalizedName(stack) is the block name for every variant. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }
}
