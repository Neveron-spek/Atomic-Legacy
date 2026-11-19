package com.atomiclegacy.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;

public class OilSandBlock extends FallingBlock {
    private static final MapCodec<OilSandBlock> CODEC = simpleCodec(OilSandBlock::new);

    public OilSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
