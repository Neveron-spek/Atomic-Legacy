package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.WallChargeBlock;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.explosion.LegacyVnt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 BlockChargeDynamite / BlockChargeMiner / BlockChargeC4 / BlockChargeSemtex {@code explode}. */
public final class WallChargeExplosions {
    /** BlockChargeBase.safe: suppresses the explode-on-removal of {@code breakBlock}. */
    public static boolean safe = false;

    private WallChargeExplosions() {
    }

    public static void detonate(ServerLevel level, BlockPos pos, WallChargeBlock.Kind kind) {
        safe = true;
        try {
            level.removeBlock(pos, false);
        } finally {
            safe = false;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        switch (kind) {
            // new ExplosionNT(world, null, x + 0.5, y + 0.5, z + 0.5, 4F).explode()
            case DYNAMITE -> {
                LegacyVnt.explodeNT(level, null, center, 4.0F, 16, -1.0F, true);
                LegacyProjectileUtil.sendSmallExplosionEffect(level, center, 15, 3.0F, 1.25F);
            }
            // ... .addAllAttrib(ExAttrib.NOHURT, ExAttrib.ALLDROP)
            case MINER -> {
                LegacyVnt.explodeNT(level, null, center, 4.0F, 16, 1.0F, false);
                LegacyProjectileUtil.sendSmallExplosionEffect(level, center, 15, 3.0F, 1.25F);
            }
            // ExplosionVNT 15F, BlockAllocatorStandard(32), BlockProcessorStandard.setNoDrop(), EntityProcessorStandard
            case C4 -> {
                LegacyVnt.at(level, center, 15.0F).allocStandard(32).noDrop().entitiesStandard().explode();
                composeEffectSmall(level, pos);
            }
            // ExplosionVNT 10F, BlockAllocatorStandard(32), setAllDrop().setFortune(3), no entity processor
            case SEMTEX -> {
                LegacyVnt.at(level, center, 10.0F).allocStandard(32).allDrop().fortune(3).explode();
                composeEffectSmall(level, pos);
            }
        }
    }

    /** ExplosionCreator.composeEffectSmall(world, x + 0.5, y + 1, z + 0.5). */
    private static void composeEffectSmall(ServerLevel level, BlockPos pos) {
        LegacyProjectileUtil.composeExplosionEffect(level,
                new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D),
                10, 2.0F, 0.5F, 25.0F, 5, 8, 20, 0.75F, 1.0F, -2.0F, 150.0F);
    }
}
