package com.atomiclegacy.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity R1] 1.7.10 Material checks used by hazards, mapped onto 1.21 block classes and tags:
 * Material.plants (flowers, saplings, crops, mushrooms, reeds, cocoa, lily pads, stems, nether wart,
 * double plants), Material.vine (tall grass, dead bush, vines) and Material.leaves.
 */
public final class LegacyMaterials {
    private LegacyMaterials() {
    }

    public static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    /** Material.vine: Blocks.tallgrass (grass, fern, dead shrub), Blocks.deadbush, Blocks.vine. */
    public static boolean isVine(BlockState state) {
        Block b = state.getBlock();
        return b instanceof VineBlock || b == Blocks.SHORT_GRASS || b == Blocks.FERN || b == Blocks.DEAD_BUSH;
    }

    /** Blocks.tallgrass of 1.7.10 (short grass, fern and the dead shrub variant). */
    public static boolean isTallGrass(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.SHORT_GRASS || b == Blocks.FERN;
    }

    /** Material.plants. */
    public static boolean isPlants(BlockState state) {
        Block b = state.getBlock();
        return (b instanceof BushBlock && !isVine(state)) || b instanceof SugarCaneBlock || b instanceof CocoaBlock;
    }

    public static boolean isPlantLeavesOrVine(BlockState state) {
        return isPlants(state) || isLeaves(state) || isVine(state);
    }
}
