package com.atomiclegacy.blocks;

import com.atomiclegacy.entity.TimedExplosiveEntity;

public final class FissureBombBlock extends TimedExplosiveBlock {
    public FissureBombBlock(Properties properties) {
        super(properties, TimedExplosiveEntity.Kind.FISSURE);
    }
}
