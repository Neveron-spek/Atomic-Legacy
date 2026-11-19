package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.SoyuzLauncherBlock;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity P2: soyuz_launcher] TileEntitySoyuzStruct: once per 20 ticks checks the
 * struct_launcher pad, the concrete legs and both scaffold towers; when complete it
 * removes the scaffolding and the core and builds soyuz_launcher (core at y + 4).
 * Offsets are absolute world axes, exactly as in the reference.
 */
public final class SoyuzStructBlockEntity extends BlockEntity {
    private int age;

    public SoyuzStructBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SOYUZ_STRUCT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SoyuzStructBlockEntity core) {
        if (level.isClientSide) return;
        core.age++;
        if (core.age < 20) return;
        core.age = 0;
        if (isComplete(level, pos)) build(level, pos);
    }

    private static boolean is(Level level, BlockPos pos, int x, int y, int z, Block block) {
        return level.getBlockState(pos.offset(x, y, z)).is(block);
    }

    private static boolean concrete(Level level, BlockPos pos, int x, int y, int z) {
        BlockState state = level.getBlockState(pos.offset(x, y, z));
        return state.is(HbmBlocks.CONCRETE.get()) || state.is(HbmBlocks.CONCRETE_SMOOTH.get());
    }

    public static boolean isComplete(Level level, BlockPos pos) {
        Block launcher = HbmBlocks.STRUCT_LAUNCHER.get();
        Block scaffold = HbmBlocks.STRUCT_SCAFFOLD.get();
        // CHECK PAD
        for (int i = -6; i <= 6; i++) for (int j = 3; j <= 4; j++) for (int k = -6; k <= 6; k++)
            if (!is(level, pos, i, j, k, launcher)) return false;
        for (int i = -1; i <= 1; i++) for (int j = 3; j <= 4; j++) for (int k = -8; k <= -7; k++)
            if (!is(level, pos, i, j, k, launcher)) return false;
        for (int i = -2; i <= 2; i++) for (int j = 3; j <= 4; j++) for (int k = 7; k <= 9; k++)
            if (!is(level, pos, i, j, k, launcher)) return false;
        for (int i = -2; i <= 2; i++) for (int k = 5; k <= 9; k++)
            if (!is(level, pos, i, 51, k, launcher)) return false;
        for (int i = -1; i <= 1; i++) for (int k = -8; k <= -6; k++)
            if (!is(level, pos, i, 38, k, launcher)) return false;
        // CHECK LEGS
        for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++)
            if (!concrete(level, pos, i, j, k)) return false;
        for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++)
            if (!concrete(level, pos, i, j, k)) return false;
        for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++)
            if (!concrete(level, pos, i, j, k)) return false;
        for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++)
            if (!concrete(level, pos, i, j, k)) return false;
        for (int i = -1; i <= 1; i++) for (int j = 0; j <= 2; j++) for (int k = -8; k <= -6; k++)
            if (!concrete(level, pos, i, j, k)) return false;
        for (int i = -2; i <= 2; i++) for (int j = 0; j <= 2; j++) for (int k = 5; k <= 9; k++)
            if (!concrete(level, pos, i, j, k)) return false;
        // CHECK SCAFFOLDING
        for (int i = -1; i <= 1; i++) for (int j = 5; j <= 50; j++) for (int k = 6; k <= 8; k++)
            if (!is(level, pos, i, j, k, scaffold)) return false;
        for (int j = 5; j <= 37; j++)
            if (!is(level, pos, 0, j, -7, scaffold)) return false;
        return true;
    }

    private static void air(Level level, BlockPos pos, int x, int y, int z) {
        level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void build(Level level, BlockPos pos) {
        // DELETE SCAFFOLDING
        for (int i = -2; i <= 2; i++) for (int k = 5; k <= 9; k++) air(level, pos, i, 51, k);
        for (int i = -1; i <= 1; i++) for (int k = -8; k <= -6; k++) air(level, pos, i, 38, k);
        for (int i = -2; i <= 2; i++) for (int j = 0; j <= 2; j++) for (int k = 5; k <= 9; k++) air(level, pos, i, j, k);
        for (int i = -1; i <= 1; i++) for (int j = 5; j <= 50; j++) for (int k = 6; k <= 8; k++) air(level, pos, i, j, k);
        for (int j = 5; j <= 37; j++) air(level, pos, 0, j, -7);
        // GENERATE LAUNCHER
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        SoyuzLauncherBlock.build(level, pos.above(SoyuzLauncherBlock.HEIGHT));
    }
}
