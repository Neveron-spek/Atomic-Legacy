package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.blocks.DeadPlantBlock;
import com.atomiclegacy.items.BoltgunItem;
import com.atomiclegacy.items.DeadPlantBlockItem;
import com.atomiclegacy.items.FluidBarrelBlockItem;
import com.atomiclegacy.items.IAnimatedItem;
import com.atomiclegacy.items.LegacyVariantBlockItem;
import com.atomiclegacy.items.ObjMachineBlockItem;
import com.atomiclegacy.render.anim.AnimationEnums;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * [parity m1] Dedicated-server checks of the m1 stage (named M1Item because ParityM1Diagnostics is the RBMK stage):
 * BlockDeadPlant metadata variants (EnumDeadPlantType order, canPlaceBlockOn, no drops, pick block, full selection
 * box, Material.plants piston behaviour) and the items moved to their 1.7.10 item renderers (boltgun RECOIL
 * animation, OBJ renderer opt-outs). Client rendering itself is covered by runItemRenderAudit sheets.
 */
final class ParityM1ItemDiagnostics {

    private ParityM1ItemDiagnostics() {
    }

    private static final BlockPos PAD = new BlockPos(-13_500, 200, -13_500);
    private static final String[] VARIANTS = {"generic", "grass", "flower", "big_flower", "fern"};

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        items(problems, cases);
        ServerLevel level = server.overworld();
        List<BlockPos> used = new ArrayList<>();
        try {
            deadPlantWorld(level, problems, cases, used);
        } catch (RuntimeException exception) {
            problems.add("parity m1 plant_dead world checks threw " + exception);
        } finally {
            for (BlockPos pos : used) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            level.setChunkForced(PAD.getX() >> 4, PAD.getZ() >> 4, false);
        }
        return cases[0];
    }

    private static void items(List<String> problems, int[] cases) {
        Block block = HbmBlocks.PLANT_DEAD.get();
        check(block.asItem() instanceof DeadPlantBlockItem, problems, cases, "parity m1 plant_dead item is not variant-aware");
        if (block.asItem() instanceof DeadPlantBlockItem item) {
            List<ItemStack> creative = new ArrayList<>();
            item.addCreativeVariants((stack, visibility) -> creative.add(stack));
            check(creative.size() == 5, problems, cases, "parity m1 plant_dead creative variants " + creative.size() + " != 5");
            for (int i = 0; i < VARIANTS.length; i++) {
                ItemStack stack = LegacyVariantBlockItem.stackFor(item, i);
                check(item.variantIndex(stack) == i && item.variant(stack).equals(VARIANTS[i]), problems, cases,
                        "parity m1 plant_dead variant " + i + " = " + item.variant(stack));
                check(DeadPlantBlock.Kind.values()[i].getSerializedName().equals(VARIANTS[i]), problems, cases,
                        "parity m1 EnumDeadPlantType ordinal " + i + " = " + DeadPlantBlock.Kind.values()[i]);
                check(item.getName(stack).getContents().equals(net.minecraft.network.chat.Component
                                .translatable("block.atomiclegacy.plant_dead").getContents()), problems, cases,
                        "parity m1 plant_dead variant " + i + " is not named tile.plant_dead (multiName = false)");
            }
        }
        BlockState def = block.defaultBlockState();
        check(def.getValue(DeadPlantBlock.KIND) == DeadPlantBlock.Kind.GENERIC, problems, cases,
                "parity m1 plant_dead meta 0 must be GENERIC");
        check(def.getPistonPushReaction() == PushReaction.DESTROY, problems, cases,
                "parity m1 plant_dead Material.plants must be destroyed by pistons");
        check(def.getDestroySpeed(null, BlockPos.ZERO) == 0.0F, problems, cases, "parity m1 plant_dead hardness 0");

        check(HbmItems.BOLTGUN.get() instanceof BoltgunItem bolt && bolt instanceof IAnimatedItem<?>
                        && bolt.getAnimation(AnimationEnums.ToolAnimation.SWING, ItemStack.EMPTY).getBus("RECOIL") != null,
                problems, cases, "parity m1 boltgun must play the ItemBoltgun RECOIL animation");
        for (String id : ObjMachineBlockItem.LEGACY_RENDERED_IDS) {
            check(BuiltInRegistries.ITEM.containsKey(com.atomiclegacy.AtomicLegacy.id(id)), problems, cases,
                    "parity m1 legacy-rendered item missing: " + id);
        }
        check(HbmBlocks.BARREL_CORRODED.get().asItem() instanceof FluidBarrelBlockItem, problems, cases,
                "parity m1 barrel_corroded must use the RenderBarrel item path");
    }

    private static void deadPlantWorld(ServerLevel level, List<String> problems, int[] cases, List<BlockPos> used) {
        level.setChunkForced(PAD.getX() >> 4, PAD.getZ() >> 4, true);
        level.getChunk(PAD.getX() >> 4, PAD.getZ() >> 4);
        Block[] grounds = {Blocks.GRASS_BLOCK, Blocks.DIRT, HbmBlocks.WASTE_EARTH.get(), HbmBlocks.DIRT_OILY.get(),
                HbmBlocks.DIRT_DEAD.get(), Blocks.STONE, Blocks.SAND, Blocks.FARMLAND};
        boolean[] allowed = {true, true, true, true, true, false, false, false};
        DeadPlantBlock.Kind[] kinds = DeadPlantBlock.Kind.values();
        for (int g = 0; g < grounds.length; g++) {
            BlockPos ground = PAD.offset(g * 2, 0, 0);
            BlockPos plant = ground.above();
            used.add(plant);
            used.add(ground);
            level.setBlock(plant, Blocks.AIR.defaultBlockState(), 2 | 16);
            level.setBlock(ground, grounds[g].defaultBlockState(), 2 | 16);
            BlockState state = HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, kinds[g % kinds.length]);
            check(state.canSurvive(level, plant) == allowed[g], problems, cases,
                    "parity m1 plant_dead canBlockStay on " + BuiltInRegistries.BLOCK.getKey(grounds[g]) + " != " + allowed[g]);
            if (!allowed[g]) {
                continue;
            }
            level.setBlock(plant, state, 2 | 16);
            check(Block.getDrops(state, level, plant, null).isEmpty(), problems, cases,
                    "parity m1 plant_dead getItemDropped is null: no drops");
            check(state.getShape(level, plant).equals(Shapes.block()), problems, cases,
                    "parity m1 plant_dead selection box is the full block");
            check(state.getCollisionShape(level, plant).isEmpty(), problems, cases,
                    "parity m1 plant_dead has no collision box");
            ItemStack pick = state.getBlock().getCloneItemStack(level, plant, state);
            check(pick.getItem() instanceof DeadPlantBlockItem item && item.variantIndex(pick) == state.getValue(DeadPlantBlock.KIND).ordinal(),
                    problems, cases, "parity m1 plant_dead pick block keeps metadata " + state.getValue(DeadPlantBlock.KIND));
            // onNeighborBlockChange -> checkAndDropBlock: removing the ground removes the plant
            level.setBlock(ground, Blocks.AIR.defaultBlockState(), 3);
            check(level.getBlockState(plant).isAir(), problems, cases,
                    "parity m1 plant_dead must break when its ground is removed");
        }
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add(message);
    }
}
