package com.atomiclegacy.blocks;

import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmHazardSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [parity K2: block radiation] 1.7.10 BlockHazard / BlockHazardFalling world emission.
 *
 * <p>onBlockAdded: rad = HazardSystem.getHazardLevelFromStack(new ItemStack(this), RADIATION) * 0.1F (a field of
 * the Block singleton, shared by every block of that type for the rest of the session) and, if positive, an update
 * in 20 ticks. updateTick: if rad > 0, incrementRad(rad) and another update in 20 ticks. These blocks do not tick
 * randomly, so after a restart the persisted scheduled tick finds rad = 0 until a block of that type is placed again
 * and the chain stops (original behaviour, kept). BlockOre (ores, BlockOutgas) keeps its constructor rad, which is
 * only non-zero for ore_schrabidium (0.1F, random ticks).</p>
 */
public final class LegacyBlockHazardRadiation {
    /** ModBlocks entries created as BlockHazard, BlockHotHazard, BlockNuclearWaste, BlockSellafield, BlockHazardFalling. */
    private static final Set<String> BLOCK_HAZARD_IDS = Set.of(
            "block_actinium", "block_corium", "block_mox_fuel", "block_neptunium", "block_plutonium",
            "block_plutonium_fuel", "block_pu239", "block_pu240", "block_pu_mix", "block_ra226", "block_schrabidate",
            "block_schrabidium", "block_schrabidium_fuel", "block_schraranium", "block_solinium", "block_thorium",
            "block_thorium_fuel", "block_trinitite", "block_u233", "block_u235", "block_u238", "block_uranium",
            "block_uranium_fuel", "block_white_phosphorus", "block_polonium", "block_pu238", "block_waste",
            "block_waste_painted", "block_waste_vitrified", "sellafield", "block_fallout", "block_red_phosphorus",
            "block_yellowcake");
    /** BlockOre(Material.rock, 0.1F, 0.5F). */
    public static final String ORE_SCHRABIDIUM = "ore_schrabidium";
    public static final float ORE_SCHRABIDIUM_RAD = 0.1F;
    /** BlockHazard.tickRate when rad > 0. */
    public static final int TICK_RATE = 20;

    /** BlockHazard.rad per Block singleton. */
    private static final Map<Block, Float> RAD = new ConcurrentHashMap<>();

    private LegacyBlockHazardRadiation() {
    }

    public static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    public static boolean isBlockHazard(Block block) {
        return BLOCK_HAZARD_IDS.contains(id(block));
    }

    /** Current value of the rad field; {@code initial} is the constructor value (0, BlockSellafield 0.5). */
    public static float rad(Block block, float initial) {
        return RAD.getOrDefault(block, initial);
    }

    /** BlockHazard.onBlockAdded. */
    public static void onBlockAdded(Block block, Level level, BlockPos pos) {
        float rad = (float) HbmHazardSystem.rawRadiation(new ItemStack(block)) * 0.1F;
        RAD.put(block, rad);
        if (rad > 0 && !level.isClientSide) {
            level.scheduleTick(pos, block, TICK_RATE);
        }
    }

    /** BlockHazard.updateTick. */
    public static void updateTick(Block block, ServerLevel level, BlockPos pos) {
        float rad = rad(block, 0F);
        if (rad > 0) {
            ChunkRadiationData.get(level).incrementRadiation(pos, rad);
            level.scheduleTick(pos, block, TICK_RATE);
        }
    }

    /** Diagnostics hook: forget the session value of the rad field. */
    public static void resetForTest(Block block) {
        RAD.remove(block);
    }
}
