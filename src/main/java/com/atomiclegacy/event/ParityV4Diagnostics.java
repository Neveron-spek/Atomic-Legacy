package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.IcfCoreBlock;
import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.blocks.LegacyStepSound;
import com.atomiclegacy.blocks.LegacyVariantBlock;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.inventory.recipes.PurexNaquadriaRecipes;
import com.atomiclegacy.inventory.recipes.PurexRecipe;
import com.atomiclegacy.items.WatzPelletItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.registry.HbmSoundTypes;
import com.atomiclegacy.tileentity.IcfCoreBlockEntity;
import com.atomiclegacy.tileentity.IcfStructBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [parity V4: diagnostics] ICF structure (MachineICF dimensions with the two y+3 wings, TileEntityICF ports,
 * TileEntityICFStruct assembly, BlockDummyable offset 1), PUREX naquadria recipes (OreDictionary nuggetNaquadria),
 * MK5 ray tip boundary, 1.7.10 step sounds of every block, shears / axe-only dig speeds. Site x ~ 38000.
 */
final class ParityV4Diagnostics {
    private ParityV4Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            icf(level, problems, cases);
        } catch (RuntimeException e) {
            problems.add("V4 ICF threw " + e);
        }
        purex(server, problems, cases);
        nukeTip(level, problems, cases);
        sounds(level, problems, cases);
        digSpeed(level, problems, cases);
        return cases[0];
    }

    // ---------------------------------------------------------------- ICF

    private static void icf(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos origin = new BlockPos(38000, 120, 38000);
        level.getChunkAt(origin);
        clear(level, origin, 12);
        // MachineICF.getAllDimensions: body x -8..8, y 0..5, z -1..1 plus wings x -8..8, y 2..4, z +-2 (SOUTH basis).
        Set<BlockPos> expected = new HashSet<>();
        for (int x = -8; x <= 8; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = -1; z <= 1; z++) expected.add(new BlockPos(x, y, z));
            }
            for (int y = 2; y <= 4; y++) {
                expected.add(new BlockPos(x, y, 2));
                expected.add(new BlockPos(x, y, -2));
            }
        }
        check(new HashSet<>(IcfCoreBlock.FOOTPRINT.offsets()).equals(expected), problems, cases,
                "ICF footprint has " + IcfCoreBlock.FOOTPRINT.offsets().size() + " cells, the original " + expected.size());

        // Assembly: TileEntityICFStruct requirement scan, then setBlock(icf) + fillSpace.
        Direction facing = Direction.SOUTH;
        level.setBlock(origin, HbmBlocks.STRUCT_ICF_CORE.get().defaultBlockState()
                .setValue(com.atomiclegacy.blocks.IcfStructBlock.FACING, facing), 3);
        for (int i = -8; i <= 8; i++) {
            component(level, origin, 1, 0, i, 0);
            if (i != 0) component(level, origin, 0, 0, i, 0);
            component(level, origin, -1, 0, i, 0);
            component(level, origin, 0, 3, i, 2);
            int v = Math.abs(i) <= 2 ? 2 : 4;
            for (int j = -1; j <= 1; j++) component(level, origin, j, 1, i, v);
            for (int j = -2; j <= 2; j++) component(level, origin, j, 2, i, v);
            for (int j = -2; j <= 2; j++) if (j != 0) component(level, origin, j, 3, i, v);
            for (int j = -2; j <= 2; j++) component(level, origin, j, 4, i, v);
            for (int j = -1; j <= 1; j++) component(level, origin, j, 5, i, v);
        }
        if (level.getBlockEntity(origin) instanceof IcfStructBlockEntity struct) {
            for (int t = 0; t < 20; t++) IcfStructBlockEntity.tick(level, origin, level.getBlockState(origin), struct);
        }
        boolean assembled = level.getBlockState(origin).is(HbmBlocks.ICF.get());
        check(assembled, problems, cases, "ICF struct with all icf_component blocks did not assemble");
        if (assembled) {
            int dummies = 0;
            int components = 0;
            for (BlockPos offset : expected) {
                BlockPos p = origin.offset(offset);
                if (p.equals(origin)) continue;
                if (level.getBlockEntity(p) instanceof MachineDummyBlockEntity dummy && dummy.getCorePos().equals(origin)) dummies++;
                if (level.getBlockState(p).is(HbmBlocks.ICF_COMPONENT.get())) components++;
            }
            check(dummies == expected.size() - 1 && components == 0, problems, cases,
                    "ICF assembly: " + dummies + " linked cells, " + components + " components left in the footprint");
            icfPorts(level, origin, facing, problems, cases);
            // sound of a cell = sound of the machine block (1.7.10 cells are the block itself)
            BlockPos wing = origin.offset(0, 3, 2);
            check(level.getBlockState(wing).getSoundType(level, wing, null) == level.getBlockState(origin).getSoundType(),
                    problems, cases, "ICF cell step sound differs from the core");
            // Disassembly: breaking the core removes every cell.
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
            int left = 0;
            for (BlockPos offset : expected) {
                if (level.getBlockState(origin.offset(offset)).is(HbmBlocks.MACHINE_DUMMY.get())) left++;
            }
            check(left == 0, problems, cases, "ICF removal left " + left + " cells");
        }
        clear(level, origin, 12);

        // Direct placement: BlockDummyable with getOffset() = 1 puts the core one block away from the player.
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setYRot(0.0F); // facing south -> dir NORTH -> core = clicked.south()
        ItemStack stack = new ItemStack(HbmBlocks.ICF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos clicked = origin;
        level.setBlock(clicked.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(clicked.below()), Direction.UP, clicked.below(), false));
        ((BlockItem) stack.getItem()).place(context);
        BlockPos core = clicked.south();
        check(level.getBlockState(core).is(HbmBlocks.ICF.get())
                        && level.getBlockState(core).getValue(LargeMachineBlock.FACING) == Direction.NORTH
                        && level.getBlockEntity(clicked) instanceof MachineDummyBlockEntity cell && cell.getCorePos().equals(core),
                problems, cases, "ICF placement: core expected at clicked.south() facing NORTH, found "
                        + level.getBlockState(core) + " / " + level.getBlockState(clicked));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        clear(level, origin, 12);
    }

    private static void icfPorts(ServerLevel level, BlockPos core, Direction facing, List<String> problems, int[] cases) {
        // TileEntityICF.getPorts (SOUTH: dir = +z, rot = east): side proxies at dir*+-2 +- rot*6, y+3.
        List<IcfCoreBlockEntity.Port> ports = IcfCoreBlockEntity.portsFor(core, facing);
        Set<BlockPos> sides = Set.of(core.offset(6, 3, 2), core.offset(-6, 3, 2), core.offset(6, 3, -2), core.offset(-6, 3, -2));
        Set<BlockPos> found = new HashSet<>();
        for (IcfCoreBlockEntity.Port port : ports) {
            if (port.proxyPos().getY() == core.getY() + 3) {
                found.add(port.proxyPos());
                Direction want = port.proxyPos().getZ() > core.getZ() ? Direction.SOUTH : Direction.NORTH;
                check(port.exposedFace() == want, problems, cases, "ICF side port " + port.proxyPos() + " faces " + port.exposedFace());
                check(level.getCapability(Capabilities.FluidHandler.BLOCK, port.proxyPos(), port.exposedFace()) != null,
                        problems, cases, "ICF side port " + port.proxyPos() + " has no fluid handler");
                check(level.getCapability(Capabilities.ItemHandler.BLOCK, port.proxyPos(), port.exposedFace()) != null,
                        problems, cases, "ICF side port " + port.proxyPos() + " is no inventory proxy");
            }
        }
        check(found.equals(sides), problems, cases, "ICF side ports " + found + ", the original " + sides);
        check(level.getCapability(Capabilities.ItemHandler.BLOCK, core.above(5), Direction.UP) != null, problems, cases,
                "ICF top port is no inventory proxy");
        check(level.getCapability(Capabilities.ItemHandler.BLOCK, core, Direction.DOWN) != null, problems, cases,
                "ICF core block is no ISidedInventory");
        check(level.getCapability(Capabilities.ItemHandler.BLOCK, core.offset(3, 1, 1), Direction.SOUTH) == null, problems, cases,
                "ICF plain cell exposes an inventory (only makeExtra ports are TileEntityProxyCombo)");
    }

    private static void component(ServerLevel level, BlockPos origin, int width, int y, int length, int variant) {
        // TileEntityICFStruct.cbarp for dir SOUTH: x = origin - length (port authored mapping), z = width
        level.setBlock(origin.offset(-length, y, width), HbmBlocks.ICF_COMPONENT.get().defaultBlockState()
                .setValue(LegacyVariantBlock.VARIANT, variant), 2 | 16);
    }

    // ---------------------------------------------------------------- PUREX

    private static void purex(MinecraftServer server, List<String> problems, int[] cases) {
        boolean tagEmpty = PurexNaquadriaRecipes.firstNaquadriaNugget().isEmpty();
        boolean present = server.getRecipeManager().byKey(PurexNaquadriaRecipes.WATZ_NAQADAH).isPresent()
                || server.getRecipeManager().byKey(PurexNaquadriaRecipes.WATZ_NAQADRIA).isPresent();
        check(present != tagEmpty, problems, cases, "PUREX watznaqadah/watznaqadria present=" + present
                + " while c:nuggets/naquadria empty=" + tagEmpty + " (original: only with OreDictionary nuggetNaquadria)");
        List<RecipeHolder<PurexRecipe>> built = PurexNaquadriaRecipes.recipesFor(server.registryAccess(), BuiltInRegistries.ITEM.get(AtomicLegacy.id("nugget_co60")));
        ItemStack nqd = WatzPelletItem.stack(WatzPelletItem.Type.NQD, HbmItems.WATZ_PELLET_DEPLETED.get());
        ItemStack nqr = WatzPelletItem.stack(WatzPelletItem.Type.NQR, HbmItems.WATZ_PELLET_DEPLETED.get());
        String[][] outputs = {{"nugget_co60", "nugget_euphemium", "nuclear_waste"}, {"nugget_co60", "nugget_euphemium", "nuclear_waste"}};
        int[] counts = {12, 6, 2};
        for (int r = 0; r < 2; r++) {
            PurexRecipe recipe = built.get(r).value();
            ItemStack in = r == 0 ? nqd : nqr;
            ItemStack other = r == 0 ? nqr : nqd;
            check(recipe.duration() == 60 && recipe.power() == 10_000 && "autoswitch.watz".equals(recipe.group()), problems, cases,
                    built.get(r).id() + " setup " + recipe.duration() + "/" + recipe.power() + "/" + recipe.group());
            check(recipe.inputItems().size() == 1 && recipe.inputItems().get(0).count() == 1
                            && recipe.inputItems().get(0).ingredient().test(in) && !recipe.inputItems().get(0).ingredient().test(other),
                    problems, cases, built.get(r).id() + " input pellet");
            check(recipe.inputFluids().size() == 2
                            && "kerosene".equals(recipe.inputFluids().get(0).type().name()) && recipe.inputFluids().get(0).amount() == 500
                            && "nitric_acid".equals(recipe.inputFluids().get(1).type().name()) && recipe.inputFluids().get(1).amount() == 250,
                    problems, cases, built.get(r).id() + " input fluids");
            boolean ok = recipe.outputItems().size() == 3;
            for (int i = 0; ok && i < 3; i++) {
                ItemStack out = recipe.outputItems().get(i).stack();
                ok = BuiltInRegistries.ITEM.getKey(out.getItem()).getPath().equals(outputs[r][i]) && out.getCount() == counts[i];
            }
            check(ok, problems, cases, built.get(r).id() + " outputs " + recipe.outputItems());
            check(recipe.outputFluids().size() == 1 && "watz".equals(recipe.outputFluids().get(0).type().name())
                    && recipe.outputFluids().get(0).amount() == 1000, problems, cases, built.get(r).id() + " output fluid");
        }
        int count = server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get()).size();
        check(count == (tagEmpty ? 57 : 59), problems, cases, "PUREX recipe count " + count);
    }

    // ---------------------------------------------------------------- MK5 tip

    private static void nukeTip(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = new BlockPos(38040, 150, 38040);
        level.getChunkAt(o);
        clear(level, o, 6);
        for (int i = 0; i <= 3; i++) level.setBlock(o.east(i), Blocks.STONE.defaultBlockState(), 2 | 16);
        // (int) center: 38040.5 -> 38040; along +x the float tip length equals its step: i < 3 excludes the tip.
        List<BlockPos> removed = NukeExplosionManager.parityV4Ray(level, Vec3.atCenterOf(o), 1, 0, 0, 100, 5);
        check(removed.contains(o) && removed.contains(o.east()) && removed.contains(o.east(2)) && !removed.contains(o.east(3)),
                problems, cases, "V4 MK5 ray along +x removed " + removed + "; the original keeps the tip " + o.east(3));
        clear(level, o, 6);
        // A ray whose float tip lies slightly beyond its step keeps destroying the tip (i < |tip - pos| true).
        Vec3 c = new Vec3(38040, 150, 38040);
        double dx = 0.6D;
        double dy = 0.0D;
        double dz = 0.8D;
        int step = 3;
        double fx = (float) (c.x + dx * step) - c.x;
        double fz = (float) (c.z + dz * step) - c.z;
        boolean beyond = step < Math.sqrt(fx * fx + fz * fz);
        BlockPos tip = BlockPos.containing(c.x + dx * step, c.y, c.z + dz * step);
        for (int i = 0; i <= step; i++) {
            level.setBlock(BlockPos.containing(c.x + dx * i, c.y, c.z + dz * i), Blocks.STONE.defaultBlockState(), 2 | 16);
        }
        removed = NukeExplosionManager.parityV4Ray(level, c, dx, dy, dz, 100, step + 2);
        check(removed.contains(tip) == beyond, problems, cases, "V4 MK5 diagonal tip " + tip + " removed=" + removed.contains(tip)
                + ", float length beyond step=" + beyond);
        clear(level, o, 6);
    }

    // ---------------------------------------------------------------- step sounds

    private static void sounds(ServerLevel level, List<String> problems, int[] cases) {
        List<String> dump = new ArrayList<>();
        dump.add("id\toriginal_sound\tport_sound\tchanged");
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!AtomicLegacy.MOD_ID.equals(key.getNamespace())) continue;
            String id = key.getPath();
            String original = LegacyStepSound.originalSound(id);
            SoundType actual = block.defaultBlockState().getSoundType();
            String changed;
            synchronized (LegacyStepSound.CHANGED) {
                changed = LegacyStepSound.CHANGED.getOrDefault(id, "-");
            }
            dump.add(id + "\t" + (original == null ? "-" : original) + "\t" + LegacyStepSound.name(actual) + "\t" + changed);
            if (original == null) continue;
            checked++;
            cases[0]++;
            if (actual != LegacyStepSound.soundType(original)) {
                problems.add("V4 " + id + " step sound " + LegacyStepSound.name(actual) + ", original " + original);
            }
        }
        check(HbmBlocks.BLOCK_INSULATOR.get().defaultBlockState().getSoundType() == SoundType.WOOL, problems, cases,
                "block_insulator: original Block.soundTypeCloth");
        check(HbmBlocks.GLYPHID_BASE.get().defaultBlockState().getSoundType() == HbmSoundTypes.FLESH, problems, cases,
                "glyphid_base: original ModSoundTypes.flesh");
        check(checked > 900, problems, cases, "V4 sounds: only " + checked + " blocks with an original counterpart");
        try {
            Files.write(Path.of("parity-v4-sounds.tsv"), dump, StandardCharsets.UTF_8);
        } catch (Exception e) {
            problems.add("V4 sounds: cannot write dump " + e);
        }
    }

    // ---------------------------------------------------------------- dig speed

    private static void digSpeed(ServerLevel level, List<String> problems, int[] cases) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        BlockPos probe = new BlockPos(38080, 300, 38000);
        level.getChunkAt(probe);
        BlockState oak = Blocks.OAK_LEAVES.defaultBlockState();
        for (Block block : new Block[] {HbmBlocks.WASTE_LEAVES.get(), HbmBlocks.LEAVES_LAYER.get()}) {
            BlockState state = block.defaultBlockState();
            float hardness = state.getDestroySpeed(level, probe);
            float shears = progress(player, Items.SHEARS, state, level, probe);
            float oakShears = progress(player, Items.SHEARS, oak, level, probe);
            // both start from shears speed 15: progress ratio = inverse hardness ratio (oak leaves 0.2)
            check(near(shears * hardness, oakShears * 0.2F), problems, cases,
                    BuiltInRegistries.BLOCK.getKey(block).getPath() + " shears progress " + shears + ", expected speed 15 like leaves");
            check(near(progress(player, Items.IRON_HOE, state, level, probe), progress(player, Items.AIR, state, level, probe)),
                    problems, cases, BuiltInRegistries.BLOCK.getKey(block).getPath() + " hoe faster than hand (1.7.10 hoe is no tool)");
            check(!state.is(BlockTags.LEAVES), problems, cases, BuiltInRegistries.BLOCK.getKey(block).getPath() + " is #minecraft:leaves");
        }
        BlockState rail = HbmBlocks.RAIL_WOOD.get().defaultBlockState();
        float hand = progress(player, Items.AIR, rail, level, probe);
        check(near(progress(player, Items.IRON_PICKAXE, rail, level, probe), hand), problems, cases,
                "rail_wood pickaxe progress differs from hand (RailGeneric(\"axe\", 0))");
        check(near(progress(player, Items.IRON_AXE, rail, level, probe), hand * 6.0F), problems, cases,
                "rail_wood iron axe progress " + progress(player, Items.IRON_AXE, rail, level, probe) + ", expected 6x hand " + hand);
        check(rail.is(BlockTags.RAILS), problems, cases, "rail_wood left #minecraft:rails");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static float progress(FakePlayer player, net.minecraft.world.item.Item item, BlockState state, ServerLevel level, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
        return state.getDestroyProgress(player, level, pos);
    }

    // ---------------------------------------------------------------- helpers

    private static void clear(ServerLevel level, BlockPos center, int r) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 8, r))) {
            if (!level.getBlockState(p).isAir()) {
                level.removeBlockEntity(p);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) <= 1.0E-4F * Math.max(1.0F, Math.abs(b));
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) {
            problems.add("V4 " + message);
        }
    }

}
