package com.atomiclegacy.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.PlushieType;
import com.atomiclegacy.blocks.SnowglobeType;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.handler.LegacyBobmazonOffers;
import com.atomiclegacy.integration.jei.ParityS3JeiRecipes;
import com.atomiclegacy.integration.jei.UniversalJeiRecipe;
import com.atomiclegacy.inventory.recipes.LegacyCraftBookRecipes;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.items.SnowglobeBlockItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.util.Calculator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * [parity S3: diagnostics] NEI-handler data behind the new JEI categories, Bobmazon offer tables,
 * the calculator backend, the slice printer / book items and restored QMAW texts.
 */
public final class ParityS3Diagnostics {
    private ParityS3Diagnostics() {
    }

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            jei(problems, cases);
            bobmazon(server, problems, cases);
            calculator(problems, cases);
            itemsAndPages(problems, cases);
        } catch (RuntimeException exception) {
            problems.add("S3: diagnostics threw " + exception);
        }
        return cases[0];
    }

    private static void jei(List<String> problems, int[] cases) {
        // AnnihilatorRecipes only registers milestones in 528 mode.
        int annihilator = ParityS3JeiRecipes.annihilator().size();
        check(annihilator == (HbmConfig.ENABLE_528_MODE.get() ? 16 : 0), problems, cases, "annihilator recipes " + annihilator);

        List<UniversalJeiRecipe> ashpit = ParityS3JeiRecipes.ashpit();
        check(ashpit.size() == 9, problems, cases, "ashpit recipes " + ashpit.size());
        check(ashpit.stream().allMatch(r -> r.outputs().stream().noneMatch(UniversalJeiRecipe.Slot::isEmpty)
                && r.inputs().stream().noneMatch(UniversalJeiRecipe.Slot::isEmpty)), problems, cases, "ashpit slots resolved");
        check(ashpit.get(0).inputs().get(1).items().size() == 3 && ashpit.get(1).inputs().get(1).items().size() == 5
                && ashpit.get(2).inputs().get(1).items().size() == 4, problems, cases, "ashpit coal/wood/misc fuel lists");
        check(ashpit.get(3).inputs().get(1).amount() == 2_000 && ashpit.get(4).inputs().get(1).amount() == 8_000
                && ashpit.get(4).inputs().get(0).items().size() == 1, problems, cases, "ashpit fly ash 2000 mB / soot 8000 mB industrial chimney");

        List<UniversalJeiRecipe> deuterium = ParityS3JeiRecipes.deuterium();
        check(deuterium.size() == 1 && deuterium.get(0).inputs().get(0).amount() == 1_000
                && deuterium.get(0).outputs().get(0).amount() == 20 && deuterium.get(0).machines().size() == 2,
                problems, cases, "deuterium 1000 mB water -> 20 mB heavy water");

        List<UniversalJeiRecipe> tooling = ParityS3JeiRecipes.tooling();
        check(tooling.size() == 4, problems, cases, "tooling conversions " + tooling.size());
        check(tooling.stream().allMatch(r -> r.inputs().stream().noneMatch(UniversalJeiRecipe.Slot::isEmpty)
                && !r.outputs().get(0).isEmpty()), problems, cases, "tooling inputs resolved (tags)");
        check(tooling.get(0).inputs().get(0).items().get(0).getCount() == 4 && tooling.get(0).machines().size() == 1
                && tooling.get(1).machines().size() == 2 && tooling.get(3).inputs().size() == 3,
                problems, cases, "tooling bolts x4, torch tools, icf bolt step");

        List<UniversalJeiRecipe> construction = ParityS3JeiRecipes.construction();
        check(construction.size() == 6, problems, cases, "construction recipes " + construction.size());
        check(construction.get(0).inputs().size() == 7 && construction.get(3).inputs().size() == 6 && construction.get(4).inputs().size() == 8,
                problems, cases, "construction watz/soyuz/icf bills");
        check(construction.get(3).inputs().get(1).items().get(0).getCount() == 384
                && construction.get(3).inputs().get(1).lines().size() == 1
                && "6x64".equals(construction.get(3).inputs().get(1).lines().get(0).getString()),
                problems, cases, "construction stack size label 6x64");
        check(construction.get(4).inputs().get(1).lines().get(0).getString().equals("3x64 + 48"), problems, cases, "construction label 3x64 + 48");
        check(construction.get(5).outputs().get(0).items().get(0).is(HbmBlocks.FUSION_TORUS.get().asItem())
                && construction.get(5).inputs().getLast().items().get(0).is(HbmItems.BLOWTORCH.get()), problems, cases, "construction fusion torus");

        List<UniversalJeiRecipe> rbmk = ParityS3JeiRecipes.rbmkDisassembly();
        check(rbmk.size() >= 5 * 16 && rbmk.stream().allMatch(r -> r.outputs().get(0).items().get(0).getCount() == 8),
                problems, cases, "rbmk rod disassembly recipes " + rbmk.size());

        int rtg = ParityS3JeiRecipes.rtg().size();
        long pellets = BuiltInRegistries.ITEM.stream().filter(RtgPelletItem.class::isInstance).count();
        check(rtg == (HbmConfig.rtgDecay() ? pellets : 0) && pellets == 10, problems, cases, "rtg decay recipes " + rtg + "/" + pellets);

        int grenades = ParityS3JeiRecipes.grenade().size();
        check(grenades == 19 * 5 * 5, problems, cases, "grenade crafting combinations " + grenades);

        List<ParityS3JeiRecipes.SatelliteDisplay> satellites = ParityS3JeiRecipes.satellites();
        check(satellites.size() == 4 && satellites.stream().allMatch(s -> !s.outputs().isEmpty()), problems, cases, "satellite cargo pools");
        double sum = satellites.get(0).chances().stream().mapToDouble(c -> Double.parseDouble(c.getString().replace("%", ""))).sum();
        check(sum > 95 && sum <= 100.01, problems, cases, "satellite chance lines sum " + sum);

        List<LegacyCraftBookRecipes.MagicDisplay> book = ParityS3JeiRecipes.book();
        check(book.size() == 7 && book.stream().allMatch(b -> !b.output().isEmpty()
                && b.inputs().stream().allMatch(list -> !list.isEmpty() && !list.get(0).isEmpty())), problems, cases, "black book recipes");

        List<ParityS3JeiRecipes.CrucibleSmeltingDisplay> smelting = ParityS3JeiRecipes.crucibleSmelting(ParityS3JeiRecipes.defaultStacks());
        check(smelting.size() > 50, problems, cases, "crucible smelting recipes " + smelting.size());
        check(smelting.stream().anyMatch(s -> s.inputs().stream().anyMatch(i -> i.is(Items.IRON_INGOT))), problems, cases, "crucible smelting iron ingot");
    }

    private static void bobmazon(MinecraftServer server, List<String> problems, int[] cases) {
        List<LegacyBobmazonOffers.Offer> standard = LegacyBobmazonOffers.offers(false, server.registryAccess());
        int expected = 5 + 2 + 2 + 6 + 8 + SnowglobeType.values().length + PlushieType.values().length; // [parity S4: gun_kit_1 ported]
        check(standard.size() == expected, problems, cases, "standard offers " + standard.size() + " expected " + expected);
        check(standard.get(0).stack().is(Items.TORCH) && standard.get(0).stack().getCount() == 64 && standard.get(0).caps() == 2,
                problems, cases, "first offer 64 torches for 2 caps");
        check(standard.stream().anyMatch(o -> o.stack().getItem() instanceof SnowglobeBlockItem
                && SnowglobeBlockItem.type(o.stack()) == SnowglobeType.NONE), problems, cases, "snowglobe NONE offer");
        List<LegacyBobmazonOffers.Offer> hidden = LegacyBobmazonOffers.offers(true, server.registryAccess());
        check(hidden.size() == 55, problems, cases, "hidden offers " + hidden.size() + " (55)" /* [parity S4: ammo_container, gun_kit_1/2 ported] [parity c1: Maid's and Frenchie's kits ported] */);
        check(hidden.stream().anyMatch(o -> o.stack().is(item("crucible")) && o.stack().getDamageValue() == 3), problems, cases, "crucible offered with damage 3");
        check(hidden.stream().anyMatch(o -> o.stack().has(DataComponents.LORE) && "Fusion Man".equals(o.stack().getHoverName().getString())),
                problems, cases, "Fusion Man kit lore");
    }

    private static void calculator(List<String> problems, int[] cases) {
        check(Calculator.evaluateExpression("2+3*4") == 14.0D, problems, cases, "calculator precedence");
        check(Calculator.evaluateExpression("2^10") == 1024.0D, problems, cases, "calculator power");
        check(Calculator.evaluateExpression("(1+2)*-3") == -9.0D, problems, cases, "calculator negative number");
    }

    private static void itemsAndPages(List<String> problems, int[] cases) {
        for (String id : List.of("pwr_printer", "book_lemegeton", "book_of_", "book_guide_book")) {
            check(item(id) != Items.AIR, problems, cases, "item " + id);
        }
        check(new ItemStack(Blocks.CRAFTING_TABLE).is(Items.CRAFTING_TABLE), problems, cases, "crafting table machine stack");
        JsonObject biofuel = json("/assets/atomiclegacy/manual/fluids/biofuel.json");
        check(biofuel != null && biofuel.getAsJsonObject("title").has("ru_RU"), problems, cases, "QMAW biofuel ru_RU title restored");
        JsonObject industrialOil = json("/assets/atomiclegacy/manual/fluids/industrial_oil.json");
        check(industrialOil != null && industrialOil.getAsJsonObject("content").get("ru_RU").getAsString().contains("Материал в основном применя"),
                problems, cases, "QMAW industrial oil original ru_RU text");
    }

    private static JsonObject json(String path) {
        try (InputStream stream = ParityS3Diagnostics.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) text = text.substring(1);
            return JsonParser.parseReader(new InputStreamReader(new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            return null;
        }
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(path));
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) {
            problems.add("S3: " + what);
        }
    }
}
