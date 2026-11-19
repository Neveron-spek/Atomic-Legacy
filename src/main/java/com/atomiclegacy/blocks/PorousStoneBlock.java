package com.atomiclegacy.blocks;

import net.minecraft.world.level.block.Block;

/**
 * [parity A1] 1.7.10 BlockPorous extends BlockStone: hardness 1.5, no ticking and no radiation handling (the former
 * "-10 RAD per 90-109 ticks" absorption was invented by the port base). Drops follow BlockStone: cobblestone,
 * silk touch gives smooth stone (createStackedBlock) - see loot_table/blocks/stone_porous.json.
 */
public class PorousStoneBlock extends Block {
    public PorousStoneBlock(Properties properties) {
        super(properties);
    }
}
