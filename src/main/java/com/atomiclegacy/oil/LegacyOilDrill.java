package com.atomiclegacy.oil;

import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * [parity L3: oil drills] Shared parts of TileEntityOilDrillBase: the recursive deposit search
 * (suckRec with a trace set, shuffled directions and a 64 layer limit) and the onDrill gas release
 * of the derrick and the pumpjack.
 */
public final class LegacyOilDrill {
    private static final TagKey<Item> ORE_URANIUM = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/uranium"));
    private static final TagKey<Item> ORE_ASBESTOS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/asbestos"));
    private static final Direction[] FORGE_ORDER = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private LegacyOilDrill() {
    }

    /**
     * TileEntityOilDrillBase#suckRec. {@code found} are the blocks that end the search successfully
     * (ore_oil and ore_bedrock_oil for every drill), {@code onFound} is doSuck, which only yields for
     * blocks the specific drill can pump.
     */
    public static boolean suckRec(Level level, BlockPos pos, int layer, Set<BlockPos> trace,
                                  Predicate<BlockState> found, Predicate<BlockState> empty, Consumer<BlockPos> onFound) {
        if (!trace.add(pos)) {
            return false;
        }
        if (layer > 64) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (found.test(state)) {
            onFound.accept(pos);
            return true;
        }
        if (empty.test(state)) {
            for (Direction dir : shuffledDirs(level)) {
                if (suckRec(level, pos.relative(dir), layer + 1, trace, found, empty, onFound)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Set<BlockPos> newTrace() {
        return new HashSet<>();
    }

    /** BobMathUtil#getShuffledDirs: the six ForgeDirection.VALID_DIRECTIONS in random order. */
    private static Direction[] shuffledDirs(Level level) {
        List<Integer> indices = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collections.shuffle(indices, new java.util.Random(level.random.nextLong()));
        Direction[] dirs = new Direction[6];
        for (int i = 0; i < 6; i++) {
            dirs[i] = FORGE_ORDER[indices.get(i)];
        }
        return dirs;
    }

    /** OreDictionary names "oreUranium" / "oreAsbestos" of the drilled block, as c: item tags. */
    private static Block releasedGas(BlockState drilled) {
        ItemStack stack = new ItemStack(drilled.getBlock().asItem());
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(ORE_URANIUM)) {
            return HbmBlocks.GAS_RADON_DENSE.get();
        }
        if (stack.is(ORE_ASBESTOS)) {
            return HbmBlocks.GAS_ASBESTOS.get();
        }
        return null;
    }

    /**
     * TileEntityMachineOilWell#onDrill, including its index mix-up: it tests (x + j, y + 10, z + j)
     * and places the gas at (x + k, y + 10, z + k).
     */
    public static void derrickOnDrill(Level level, BlockPos machine, BlockState drilled) {
        Block gas = releasedGas(drilled);
        if (gas == null) {
            return;
        }
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                if (level.getBlockState(machine.offset(j, 10, j)).canBeReplaced()) {
                    level.setBlock(machine.offset(k, 10, k), gas.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** TileEntityMachinePumpjack#onDrill: the four horizontal neighbours of the core block. */
    public static void pumpjackOnDrill(Level level, BlockPos machine, BlockState drilled) {
        Block gas = releasedGas(drilled);
        if (gas == null) {
            return;
        }
        for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos pos = machine.relative(dir);
            if (level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, gas.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
