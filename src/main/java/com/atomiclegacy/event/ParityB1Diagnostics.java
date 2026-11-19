package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.RadonOutgassingOreBlock;
import com.atomiclegacy.blocks.SmolderingOreBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
import com.atomiclegacy.items.InfiniteFluidContainerItem;
import com.atomiclegacy.items.LegacyAmmoBagItem;
import com.atomiclegacy.items.LegacyFusionShieldItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.LegacyCasingTable;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.level.block.EntityBlock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * [parity b1: diagnostics] Missing-content pass. Dumps every runtime registry of the mod (kind, id, class) to
 * parity-b1-registry.tsv so that work/parity_b1_registry.py can compare it with the 1.7.10 registries, and checks the
 * objects restored by this pass: radon outgassing uranium ores, the smoldering ore, the infinite water tanks, the
 * TiCl4 recipes, the ammo / casing bags with the casing table, and the fusion shields.
 */
final class ParityB1Diagnostics {
    private ParityB1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int cases = 0;
        try {
            dump(server);
        } catch (Exception exception) {
            problems.add("parity b1: registry dump failed: " + exception);
        }
        cases++;
        int[] counter = {cases};
        ServerLevel level = server.overworld();

        // BlockOutgas uranium ores: class, random ticking, radon into an adjacent air block, radon on harvest.
        for (String id : new String[] {"ore_uranium", "ore_uranium_scorched", "ore_gneiss_uranium", "ore_gneiss_uranium_scorched",
                "ore_nether_uranium", "ore_nether_uranium_scorched", "ore_deepslate_uranium", "ore_deepslate_uranium_scorched"}) {
            Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
            check(block instanceof RadonOutgassingOreBlock, problems, counter, id + " is not a RadonOutgassingOreBlock");
            check(block.defaultBlockState().isRandomlyTicking(), problems, counter, id + " does not tick randomly");
        }
        BlockPos ore = new BlockPos(8_900, 120, 8_900);
        for (BlockPos pos : BlockPos.betweenClosed(ore.offset(-1, -1, -1), ore.offset(1, 1, 1))) {
            level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockState oreState = HbmBlocks.ORE_URANIUM.get().defaultBlockState();
        level.setBlock(ore, oreState, Block.UPDATE_CLIENTS);
        oreState.randomTick(level, ore, level.random);
        int radon = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(ore.relative(direction)).is(HbmBlocks.GAS_RADON.get())) radon++;
        }
        check(radon == 1, problems, counter, "uranium ore random tick placed " + radon + " radon blocks, expected 1");
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        oreState.getBlock().playerDestroy(level, player, ore, oreState, null, new ItemStack(Items.IRON_PICKAXE));
        check(level.getBlockState(ore).is(HbmBlocks.GAS_RADON.get()), problems, counter, "harvested uranium ore left no radon");
        for (BlockPos pos : BlockPos.betweenClosed(ore.offset(-1, -1, -1), ore.offset(1, 1, 1))) {
            level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        // BlockSmolder
        check(HbmBlocks.ORE_NETHER_SMOLDERING.get() instanceof SmolderingOreBlock, problems, counter, "ore_nether_smoldering is not a SmolderingOreBlock");
        check(HbmBlocks.ORE_NETHER_SMOLDERING.get().defaultBlockState().getLightEmission() == 15, problems, counter, "ore_nether_smoldering light is not 15");

        // ItemInfiniteFluid water tanks: always registered, recipes unless 528 mode.
        check(item("inf_water") instanceof InfiniteFluidContainerItem, problems, counter, "inf_water is not registered as an infinite fluid item");
        check(item("inf_water_mk2") instanceof InfiniteFluidContainerItem, problems, counter, "inf_water_mk2 is not registered as an infinite fluid item");
        boolean waterRecipes = !HbmConfig.ENABLE_528_MODE.get();
        for (String id : new String[] {"inf_water", "inf_water_mk2"}) {
            check(server.getRecipeManager().byKey(AtomicLegacy.id(id)).isPresent() == waterRecipes, problems, counter,
                    "recipe " + id + " presence does not match the 528 mode switch");
        }

        // Ilmenite -> TiCl4 -> titanium chain and the bag recipes.
        for (String id : new String[] {"chemical_plant/ticl4", "pyrolysis/titanium_tetrachloride", "rotary_furnace/titanium_titanium_tetrachloride_flux",
                "ammo_bag_leather", "ammo_bag_rubber", "casing_bag_leather", "casing_bag_rubber"}) {
            check(server.getRecipeManager().byKey(AtomicLegacy.id(id)).isPresent(), problems, counter, "recipe " + id + " is not loaded");
        }
        server.getRecipeManager().byKey(AtomicLegacy.id("chemical_plant/ticl4")).ifPresent(holder -> {
            if (holder.value() instanceof ChemicalPlantRecipe recipe) {
                check(recipe.duration() == 60 && recipe.power() == 500 && recipe.inputItems().size() == 2 && recipe.inputItems().get(0).count() == 4
                                && recipe.inputFluids().size() == 1 && recipe.inputFluids().get(0).amount() == 500
                                && recipe.outputFluids().size() == 1 && recipe.outputFluids().get(0).amount() == 4_000,
                        problems, counter, "chem.ticl4 differs from setup(60, 500), 4 ilmenite + coke + 500 chlorine -> 4000 TiCl4");
            } else {
                problems.add("parity b1: chemical_plant/ticl4 is not a chemical plant recipe");
            }
        });

        // ItemAmmoBag / ItemCasingBag
        check(item("ammo_bag") instanceof LegacyAmmoBagItem bag && bag.kind() == LegacyAmmoBagItem.Kind.AMMO_BAG, problems, counter, "ammo_bag kind");
        check(item("ammo_bag_infinite") instanceof LegacyAmmoBagItem bag && bag.kind() == LegacyAmmoBagItem.Kind.AMMO_BAG_INFINITE, problems, counter, "ammo_bag_infinite kind");
        check(item("casing_bag") instanceof LegacyAmmoBagItem bag && bag.kind() == LegacyAmmoBagItem.Kind.CASING_BAG, problems, counter, "casing_bag kind");
        check(LegacyAmmoBagItem.Kind.AMMO_BAG.slots() == 8 && LegacyAmmoBagItem.Kind.CASING_BAG.slots() == 15, problems, counter, "bag slot counts");
        for (String id : new String[] {"ammo_bag", "ammo_bag_infinite", "casing_bag"}) {
            check(item(id).getDefaultMaxStackSize() == 1, problems, counter, id + " stack size is not 1");
        }
        ItemStack ammoBag = new ItemStack(item("ammo_bag"));
        check(ammoBag.isBarVisible() && ammoBag.getBarWidth() == 0, problems, counter, "a fresh ammo bag must show an empty bar");
        check(!new ItemStack(item("ammo_bag_infinite")).isBarVisible(), problems, counter, "the infinite ammo bag must not show a bar");
        ItemStack round = BulletConfig.configs.isEmpty() ? ItemStack.EMPTY : BulletConfig.configs.get(0).toStack();
        check(!round.isEmpty() && LegacyAmmoBagItem.isValidAmmo(round), problems, counter, "the first bullet config has no valid ammo stack");
        if (!round.isEmpty()) {
            LegacyAmmoBagItem.BagInventory inventory = new LegacyAmmoBagItem.BagInventory(ammoBag, level.registryAccess(), LegacyAmmoBagItem.Kind.AMMO_BAG);
            check(inventory.canPlaceItem(0, round) && !inventory.canPlaceItem(0, new ItemStack(Items.STONE)), problems, counter, "ammo bag slot filter");
            inventory.setItem(3, round.copyWithCount(round.getMaxStackSize()));
            LegacyAmmoBagItem.BagInventory reread = new LegacyAmmoBagItem.BagInventory(ammoBag, level.registryAccess(), LegacyAmmoBagItem.Kind.AMMO_BAG);
            check(ItemStack.matches(reread.getItem(3), round.copyWithCount(round.getMaxStackSize())), problems, counter, "ammo bag contents were not stored on the stack");
            double expected = 1.0D - (double) round.getMaxStackSize() / (7 * 64 + round.getMaxStackSize());
            check(Math.abs(((LegacyAmmoBagItem) ammoBag.getItem()).emptyFraction(ammoBag) - expected) < 1.0E-9, problems, counter, "ammo bag durability fraction");
            reread.removeItem(3, round.getMaxStackSize());
            check(new LegacyAmmoBagItem.BagInventory(ammoBag, level.registryAccess(), LegacyAmmoBagItem.Kind.AMMO_BAG).isEmpty(), problems, counter, "ammo bag was not emptied");
            check(ammoBag.isBarVisible(), problems, counter, "an emptied ammo bag keeps its bar");

            // MagazineSingleTypeBase / MagazineBelt read the bag in a player inventory.
            player.getInventory().clearContent();
            ItemStack carried = new ItemStack(item("ammo_bag"));
            new LegacyAmmoBagItem.BagInventory(carried, level.registryAccess(), LegacyAmmoBagItem.Kind.AMMO_BAG).setItem(0, round.copyWithCount(10));
            player.getInventory().setItem(5, carried);
            MagazineBelt belt = new MagazineBelt().addConfigs(BulletConfig.configs.get(0));
            ItemStack gun = new ItemStack(Items.STICK);
            check(belt.getAmount(gun, player.getInventory()) == 10, problems, counter, "belt magazine does not count the rounds of an ammo bag");
            belt.useUpAmmo(gun, player.getInventory(), 4);
            check(belt.getAmount(gun, player.getInventory()) == 6, problems, counter, "belt magazine did not take 4 rounds out of the ammo bag");
            ItemStack infinite = new ItemStack(item("ammo_bag_infinite"));
            new LegacyAmmoBagItem.BagInventory(infinite, level.registryAccess(), LegacyAmmoBagItem.Kind.AMMO_BAG_INFINITE).setItem(0, round.copyWithCount(1));
            player.getInventory().setItem(5, infinite);
            check(belt.getAmount(gun, player.getInventory()) == 9_999, problems, counter, "infinite ammo bag does not report 9999 rounds");
            belt.useUpAmmo(gun, player.getInventory(), 1);
            check(belt.getAmount(gun, player.getInventory()) == 9_999, problems, counter, "infinite ammo bag was consumed");
            player.getInventory().clearContent();
        }
        ItemStack casingBag = new ItemStack(item("casing_bag"));
        check(!new LegacyAmmoBagItem.BagInventory(casingBag, level.registryAccess(), LegacyAmmoBagItem.Kind.CASING_BAG).canPlaceItem(0, round), problems, counter,
                "the casing bag must not accept items by hand");
        ItemStack casing = new ItemStack(item("casing_small"));
        // 1 / casingAmount * 0.5 per shot: with casingAmount 1, two shots make one casing.
        check(LegacyAmmoBagItem.pushCasing(casingBag, casing, 0.5F, level.registryAccess()), problems, counter, "first half casing rejected");
        check(new LegacyAmmoBagItem.BagInventory(casingBag, level.registryAccess(), LegacyAmmoBagItem.Kind.CASING_BAG).isEmpty(), problems, counter, "half a casing produced an item");
        check(LegacyAmmoBagItem.pushCasing(casingBag, casing, 0.5F, level.registryAccess()), problems, counter, "second half casing rejected");
        ItemStack stored = new LegacyAmmoBagItem.BagInventory(casingBag, level.registryAccess(), LegacyAmmoBagItem.Kind.CASING_BAG).getItem(0);
        check(stored.is(casing.getItem()) && stored.getCount() == 1, problems, counter, "two half casings did not produce one casing in slot 0");

        // BulletConfig.casingItem / casingAmount table.
        check(LegacyCasingTable.BY_AMMO.size() == 72, problems, counter, "casing table size " + LegacyCasingTable.BY_AMMO.size() + ", expected 72");
        for (var entry : LegacyCasingTable.BY_AMMO.entrySet()) {
            check(StandardAmmoItem.StandardAmmoType.byId(entry.getKey()) != StandardAmmoItem.StandardAmmoType.NONE, problems, counter,
                    "casing table round " + entry.getKey() + " is not a standard ammo type");
            check(BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(entry.getValue().item())) && entry.getValue().amount() > 0, problems, counter,
                    "casing table item " + entry.getValue().item() + " is not registered");
        }
        int withCasing = 0;
        for (BulletConfig config : BulletConfig.configs) {
            if (config.casing != null && !config.casingStack().isEmpty()) withCasing++;
        }
        check(withCasing > 0, problems, counter, "no bullet config carries a casing item");
        AtomicLegacy.LOGGER.info("parity b1: {} of {} bullet configs carry a casing item", withCasing, BulletConfig.configs.size());
        LegacyCasingTable.Casing g10 = LegacyCasingTable.BY_AMMO.get("g10");
        check(g10 != null && g10.item().equals("casing_buckshot_advanced") && g10.count() == 1 && g10.amount() == 4, problems, counter, "g10 casing is not BUCKSHOT_ADVANCED / 4");

        // ItemFusionShield(maxDamage, maxTemp)
        long hour = 60L * 60 * 60;
        shield(problems, counter, "fusion_shield_tungsten", hour * 5, 3500);
        shield(problems, counter, "fusion_shield_desh", hour * 10, 4500);
        shield(problems, counter, "fusion_shield_chlorophyte", hour * 15, 9000);
        shield(problems, counter, "fusion_shield_vaporwave", hour * 10, 1916169);
        ItemStack shield = new ItemStack(item("fusion_shield_desh"));
        check(!shield.isBarVisible(), problems, counter, "an undamaged fusion shield shows a bar");
        LegacyFusionShieldItem.setShieldDamage(shield, hour * 5);
        check(LegacyFusionShieldItem.getShieldDamage(shield) == hour * 5 && shield.isBarVisible() && shield.getBarWidth() == 7, problems, counter, "fusion shield damage bar");
        return counter[0];
    }

    private static void shield(List<String> problems, int[] counter, String id, long maxDamage, int maxTemp) {
        check(item(id) instanceof LegacyFusionShieldItem shield && shield.maxDamage == maxDamage && shield.maxTemp == maxTemp
                && shield.getDefaultMaxStackSize() == 1, problems, counter, id + " differs from ItemFusionShield(" + maxDamage + ", " + maxTemp + ")");
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static void check(boolean condition, List<String> problems, int[] counter, String message) {
        counter[0]++;
        if (!condition) problems.add("parity b1: " + message);
    }

    private static void dump(MinecraftServer server) throws Exception {
        List<String> lines = new ArrayList<>();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!own(id)) continue;
            var block = entry.getValue();
            lines.add("block\t" + id.getPath() + "\t" + block.getClass().getName() + "\t" + (block instanceof EntityBlock ? "be" : ""));
        }
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!own(id)) continue;
            var item = entry.getValue();
            lines.add("item\t" + id.getPath() + "\t" + item.getClass().getName() + "\t" + (item instanceof BlockItem ? "blockitem" : ""));
        }
        dumpRegistry(lines, "entity", BuiltInRegistries.ENTITY_TYPE);
        dumpRegistry(lines, "blockentity", BuiltInRegistries.BLOCK_ENTITY_TYPE);
        dumpRegistry(lines, "menu", BuiltInRegistries.MENU);
        dumpRegistry(lines, "fluid", BuiltInRegistries.FLUID);
        dumpRegistry(lines, "effect", BuiltInRegistries.MOB_EFFECT);
        dumpRegistry(lines, "particle", BuiltInRegistries.PARTICLE_TYPE);
        dumpRegistry(lines, "sound", BuiltInRegistries.SOUND_EVENT);
        dumpRegistry(lines, "recipetype", BuiltInRegistries.RECIPE_TYPE);
        dumpRegistry(lines, "tab", BuiltInRegistries.CREATIVE_MODE_TAB);
        server.registryAccess().registry(Registries.ENCHANTMENT).ifPresent(registry -> {
            for (ResourceLocation id : registry.keySet()) if (own(id)) lines.add("enchantment\t" + id.getPath() + "\t\t");
        });
        for (var node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            lines.add("command\t" + node.getName() + "\t\t");
        }
        for (var holder : server.getAdvancements().getAllAdvancements()) {
            if (own(holder.id())) lines.add("advancement\t" + holder.id().getPath() + "\t\t");
        }
        Files.write(Path.of("parity-b1-registry.tsv"), lines, StandardCharsets.UTF_8);
    }

    private static <T> void dumpRegistry(List<String> lines, String kind, Registry<T> registry) {
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (own(id)) lines.add(kind + "\t" + id.getPath() + "\t" + entry.getValue().getClass().getName() + "\t");
        }
    }

    private static boolean own(ResourceLocation id) {
        return AtomicLegacy.MOD_ID.equals(id.getNamespace());
    }
}
