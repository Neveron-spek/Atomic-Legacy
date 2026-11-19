package com.atomiclegacy.event;

import com.mojang.authlib.GameProfile;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyBedrockOreStageItem;
import com.atomiclegacy.items.LegacyHeldInventoryItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.OreBasaltBlockItem;
import com.atomiclegacy.items.PlateFuelItem;
import com.atomiclegacy.items.RbmkFuelRodItem;
import com.atomiclegacy.items.RbmkPelletItem;
import com.atomiclegacy.items.RtgDepletedPelletItem;
import com.atomiclegacy.items.SellafieldBlockItem;
import com.atomiclegacy.items.StorageCrateBlockItem;
import com.atomiclegacy.items.ToolboxItem;
import com.atomiclegacy.items.WasteFuelItem;
import com.atomiclegacy.items.WatzPelletItem;
import com.atomiclegacy.items.ZirnoxRodItem;
import com.atomiclegacy.radiation.HbmHazardRegistry;
import com.atomiclegacy.radiation.HbmHazardSystem;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.radiation.HbmReactiveHazards;
import com.atomiclegacy.radiation.RadiationEvents;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [parity K1] Item hazards against 1.7.10 HazardRegistry / OreDictManager / hazard.type / hazard.modifier /
 * hazard.transformer (work/original, commit 936c60c). EXPECTED is produced from the original sources by
 * work/tmp-k1/gen_hazards.py (independent float32 evaluation of the constant expressions).
 */
public final class ParityK1Diagnostics {

    private ParityK1Diagnostics() {
    }

    private static final double X = -8192.5D, Y = 300.0D, Z = 8192.5D;

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        for (int cx = ((int) X - 32) >> 4; cx <= ((int) X + 32) >> 4; cx++) {
            for (int cz = ((int) Z - 32) >> 4; cz <= ((int) Z + 32) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        try {
            table(problems, cases);
            modifiers(server, problems, cases);
            transformers(server, problems, cases);
            tooltips(problems, cases);
            holder(server, level, problems, cases);
            dropped(level, problems, cases);
        } catch (Throwable t) {
            StackTraceElement[] trace = t.getStackTrace();
            problems.add("parity K1: exception " + t + (trace.length > 0 ? " at " + trace[0] : "") + (trace.length > 1 ? " < " + trace[1] : ""));
        }
        cleanup(level);
        return cases[0];
    }

    /// HazardRegistry table, entry by entry ///
    private static void table(List<String> problems, int[] cases) {
        java.util.Map<String, List<String[]>> expected = new java.util.LinkedHashMap<>();
        for (String line : EXPECTED.split("\n")) {
            if (line.isBlank()) continue;
            String[] f = java.util.Arrays.copyOf(line.strip().split("\t", -1), 4); // text blocks strip trailing tabs
            if (f[3] == null) f[3] = "";
            expected.computeIfAbsent(f[0], k -> new ArrayList<>()).add(f);
        }
        for (var e : expected.entrySet()) {
            String key = e.getKey();
            ItemStack stack = stackFor(key);
            if (stack == null) {
                check(problems, cases, false, key + ": item/variant missing in the port");
                continue;
            }
            List<HbmHazardRegistry.Entry> actual = HbmHazardRegistry.entries(stack);
            List<String[]> exp = e.getValue();
            boolean ok = actual.size() == exp.size();
            StringBuilder got = new StringBuilder();
            for (int i = 0; i < actual.size(); i++) {
                HbmHazardRegistry.Entry a = actual.get(i);
                got.append(a.type()).append('=').append(a.baseLevel()).append(a.mods().isEmpty() ? "" : "+mod").append(' ');
                if (i < exp.size()) {
                    String[] x = exp.get(i);
                    boolean modExpected = !x[3].isEmpty() && !x[3].equals("ORE");
                    ok &= a.type().name().equals(x[1])
                            && Float.floatToIntBits(a.baseLevel()) == Float.floatToIntBits(Float.parseFloat(x[2]))
                            && a.mods().isEmpty() != modExpected;
                }
            }
            StringBuilder want = new StringBuilder();
            for (String[] x : exp) want.append(x[1]).append('=').append(x[2]).append(x[3].isEmpty() || x[3].equals("ORE") ? "" : "+mod").append(' ');
            check(problems, cases, ok, key + ": expected " + want + "got " + got);
        }
        // blacklist and removed port inventions
        for (String id : new String[] {"ore_uranium", "ore_uranium_scorched", "ore_nether_uranium", "ore_gneiss_uranium", "ore_thorium",
                "raw_uranium", "raw_thorium", "ore_deepslate_uranium"}) {
            Item item = it(id);
            if (item != Items.AIR) check(problems, cases, HbmHazardRegistry.entries(new ItemStack(item)).isEmpty(), id + ": blacklisted ore has no hazards");
        }
        ItemStack enrichedU = LegacyBedrockOreStageItem.stackFor(it("ore_enriched"), LegacyBedrockOreStageItem.Type.URANIUM);
        check(problems, cases, HbmHazardRegistry.entries(enrichedU).isEmpty(), "ore_enriched uranium: blacklisted");
        for (String id : new String[] {"plate_uranium", "wire_schrabidium", "bolt_schrabidium", "plate_welded_schrabidium", "ingot_lead",
                "powder_lignite_tiny", "rod_zirnox_empty", "watz_pellet_depleted"}) {
            Item item = it(id);
            if (item != Items.AIR) check(problems, cases, HbmHazardSystem.hazardsPerItem(new ItemStack(item)).isEmpty()
                    && HbmHazardSystem.toxicLevel(new ItemStack(item)) == 0, id + ": no hazard registered in 1.7.10");
        }
        // aliases of port-only objects
        check(problems, cases, same("raw_schrabidium", "ore_schrabidium") && same("ore_deepslate_schrabidium", "ore_schrabidium")
                && same("raw_plutonium", "ore_nether_plutonium"), "raw/deepslate ores take the ore entries");
        ItemStack depleted = LegacyVariantItem.stackFor(it("rod_zirnox_depleted"), "u235_fuel");
        check(problems, cases, HbmHazardRegistry.entries(depleted).equals(HbmHazardRegistry.entriesForKey("rod_zirnox_u235_fuel_depleted")),
                "rod_zirnox_depleted#u235_fuel = rod_zirnox_u235_fuel_depleted");
        // external items through c tags are not testable without another mod; the key map must exist
        check(problems, cases, HbmHazardRegistry.oreKeyOf("ingot_uranium").equals("ingotUranium")
                && HbmHazardRegistry.oreKeyOf("powder_coal").equals("dustCoal"), "ore dictionary keys recorded");
    }

    private static boolean same(String a, String b) {
        return !HbmHazardRegistry.entriesForKey(b).isEmpty()
                && HbmHazardRegistry.entries(new ItemStack(it(a))).equals(HbmHazardRegistry.entriesForKey(b));
    }

    /** Builds the port stack of a table key and checks that it resolves back to the same variant. */
    private static ItemStack stackFor(String key) {
        int hash = key.indexOf('#');
        String id = hash < 0 ? key : key.substring(0, hash);
        String variant = hash < 0 ? null : key.substring(hash + 1);
        ResourceLocation location = id.contains(":") ? ResourceLocation.parse(id) : AtomicLegacy.id(id);
        if (!BuiltInRegistries.ITEM.containsKey(location)) return null;
        Item item = BuiltInRegistries.ITEM.get(location);
        ItemStack stack;
        if (variant == null) {
            stack = new ItemStack(item);
        } else if (item instanceof SellafieldBlockItem sellafield) {
            stack = SellafieldBlockItem.stackFor(sellafield, Integer.parseInt(variant));
        } else if (item instanceof WasteFuelItem) {
            stack = variant.equals("hot") ? WasteFuelItem.hot(new ItemStack(item)) : new ItemStack(item);
        } else if (item instanceof WatzPelletItem) {
            WatzPelletItem.Type type = WatzPelletItem.Type.byId(variant);
            if (type == null || !type.id.equals(variant)) return null;
            stack = WatzPelletItem.stack(type, item);
        } else if (item instanceof RtgDepletedPelletItem pellet) {
            stack = RtgDepletedPelletItem.stackFor(pellet, variant);
        } else if (item instanceof OreBasaltBlockItem basalt) {
            stack = OreBasaltBlockItem.stackFor(basalt, List.of("sulfur", "fluorite", "asbestos", "gem", "molysite").indexOf(variant));
        } else if (item instanceof LegacyBedrockOreStageItem) {
            stack = LegacyBedrockOreStageItem.stackFor(item, LegacyBedrockOreStageItem.Type.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
        } else if (item instanceof LegacyVariantItem variantItem) {
            boolean known = false;
            for (var v : variantItem.variantList()) known |= v.id().equals(variant);
            if (!known) return null;
            stack = LegacyVariantItem.stackFor(item, variant);
        } else {
            return null;
        }
        return variant == null || variant.equals(HbmHazardRegistry.variant(stack)) ? stack : null;
    }

    /// HazardModifier* at different NBT states ///
    private static void modifiers(MinecraftServer server, List<String> problems, int[] cases) {
        // HazardModifierRBMKRadiation, rbmk_fuel_ueu: u * rod_rbmk = 1.4, depleted wst * rod_rbmk * 20 = 1200
        ItemStack ueu = new ItemStack(it("rbmk_fuel_ueu"));
        near(problems, cases, rad(ueu), 1.4F, "rbmk_fuel_ueu fresh");
        RbmkFuelRodItem.setState(ueu, 1.0D, 0.0D, 20.0D, 20.0D);
        near(problems, cases, rad(ueu), 1200F, "rbmk_fuel_ueu depleted");
        RbmkFuelRodItem.setState(ueu, 0.5D, 0.0D, 20.0D, 20.0D); // 1 - enrichment^2 = 0.75
        near(problems, cases, rad(ueu), 1.4F + (1200F - 1.4F) * 0.75F, "rbmk_fuel_ueu half (quadratic)");
        RbmkFuelRodItem.setState(ueu, 0.0D, 0.5D, 20.0D, 20.0D); // + xe135 * poison
        near(problems, cases, rad(ueu), 1.4F + 1250F * 0.5F, "rbmk_fuel_ueu xenon 50%");
        ItemStack po = new ItemStack(it("rbmk_fuel_po210be")); // linear: pobe * rod_rbmk = 900, target 90
        RbmkFuelRodItem.setState(po, 0.5D, 0.0D, 20.0D, 20.0D);
        near(problems, cases, rad(po), 900F + (90F - 900F) * 0.5F, "rbmk_fuel_po210be half (linear)");
        // HazardModifierRBMKHot: min(ceil((hull - 100) / 10), 60)
        RbmkFuelRodItem.setState(ueu, 0.0D, 0.0D, 600.0D, 600.0D);
        check(problems, cases, HbmHazardSystem.hazardsPerItem(ueu).hot() == 50.0D, "rbmk hot: hull 600 -> 50 (" + HbmHazardSystem.hazardsPerItem(ueu).hot() + ")");
        RbmkFuelRodItem.setState(ueu, 0.0D, 0.0D, 2000.0D, 2000.0D);
        check(problems, cases, HbmHazardSystem.hazardsPerItem(ueu).hot() == 60.0D, "rbmk hot: capped at 60");
        RbmkFuelRodItem.setState(ueu, 0.0D, 0.0D, 50.0D, 50.0D);
        check(problems, cases, HbmHazardSystem.hazardsPerItem(ueu).hot() <= 0.0D, "rbmk hot: hull 50 -> not hot");
        // drx: digamma 1/3 constant
        check(problems, cases, Math.abs(HbmHazardSystem.hazardsPerItem(new ItemStack(it("rbmk_fuel_drx"))).digamma() - 1F / 3F) < 1e-6, "rbmk_fuel_drx digamma 1/3");
        // ItemRBMKPellet: level + (target - level) * (meta % 5) / 4, xenon meta >= 5 adds xe135 * nugget
        RbmkPelletItem pellet = (RbmkPelletItem) it("rbmk_pellet_ueu");
        near(problems, cases, rad(pellet.stackFor(0)), 0.175F, "rbmk_pellet_ueu 0");
        near(problems, cases, rad(pellet.stackFor(4)), 150F, "rbmk_pellet_ueu 4");
        near(problems, cases, rad(pellet.stackFor(2)), 0.175F + (150F - 0.175F) * 0.5F, "rbmk_pellet_ueu 2");
        near(problems, cases, rad(pellet.stackFor(5)), 0.175F + 125F, "rbmk_pellet_ueu 5 (xenon)");
        // HazardModifierFuelRadiation: pow(durability, 0.4)
        ItemStack zirnox = LegacyVariantItem.stackFor(it("rod_zirnox"), "natural_uranium_fuel");
        near(problems, cases, rad(zirnox), 0.35F, "rod_zirnox natural fresh (u * rod_dual)");
        ZirnoxRodItem.setLife(zirnox, 125_000);
        near(problems, cases, rad(zirnox), (float) (0.35F + (172.5F - 0.35F) * Math.pow(0.5D, 0.4D)), "rod_zirnox natural half life");
        ItemStack plate = new ItemStack(it("plate_fuel_u233"));
        PlateFuelItem.setLife(plate, 1_100_000);
        near(problems, cases, rad(plate), (float) (5F + (195F - 5F) * Math.pow(0.5D, 0.4D)), "plate_fuel_u233 half life");
        // HazardModifierRTGRadiation: target 0, depletion = 1 - life / max
        ItemStack rtg = new ItemStack(it("pellet_rtg"));
        near(problems, cases, rad(rtg), 15F, "pellet_rtg fresh");
        CompoundTag tag = new CompoundTag();
        tag.putLong("rtg_depletion", 631_440_000L / 2);
        rtg.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        near(problems, cases, rad(rtg), 7.5F, "pellet_rtg half");
        // hot waste / pwr
        check(problems, cases, HbmHazardSystem.hazardsPerItem(WasteFuelItem.hot(new ItemStack(it("waste_uranium")))).hot() == 5.0D, "waste_uranium hot: HOT 5");
    }

    private static float rad(ItemStack stack) {
        return (float) HbmHazardSystem.hazardsPerItem(stack).radiation();
    }

    private static void near(List<String> problems, int[] cases, float got, float want, String what) {
        check(problems, cases, Math.abs(got - want) <= Math.max(1e-4F, Math.abs(want) * 1e-5F), what + ": " + want + " RAD/s (got " + got + ")");
    }

    /// HazardTransformerRadiationNBT / Container ///
    private static void transformers(MinecraftServer server, List<String> problems, int[] cases) {
        var registries = server.registryAccess();
        ItemStack stick = new ItemStack(Items.STICK);
        CompoundTag tag = new CompoundTag();
        tag.putFloat(HbmHazardRegistry.RAD_KEY, 12.5F);
        stick.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        near(problems, cases, rad(stick), 12.5F, "hfrHazRadiation NBT");
        List<ItemStack> u235 = new ArrayList<>(List.of(new ItemStack(it("ingot_u235"), 10)));
        ItemStack box = new ItemStack(it("containment_box"));
        List<ItemStack> singles = new ArrayList<>(); // the held inventories keep one item per slot
        for (int i = 0; i < 10; i++) singles.add(new ItemStack(it("ingot_u235")));
        LegacyHeldInventoryItem.saveContents(box, singles, registries, LegacyHeldInventoryItem.Kind.CONTAINMENT_BOX);
        near(problems, cases, rad(box), (float) HbmHazardRegistry.squirt(10D), "containment_box: squirt(10)");
        ItemStack bag = new ItemStack(it("plastic_bag"));
        LegacyHeldInventoryItem.saveContents(bag, singles, registries, LegacyHeldInventoryItem.Kind.PLASTIC_BAG);
        near(problems, cases, rad(bag), 2F, "plastic_bag: x2");
        ItemStack toolbox = new ItemStack(it("toolbox"));
        ToolboxItem.saveContents(toolbox, u235, registries);
        near(problems, cases, rad(toolbox), 10F, "toolbox: sum");
        Item crateItem = null;
        for (Item item : BuiltInRegistries.ITEM) if (item instanceof StorageCrateBlockItem) { crateItem = item; break; }
        if (crateItem != null) {
            ItemStack crate = new ItemStack(crateItem);
            CompoundTag slots = new CompoundTag();
            slots.put("slot3", new ItemStack(it("ingot_u235"), 10).save(registries));
            CompoundTag root = new CompoundTag();
            root.put(StorageCrateBlockEntity.ITEM_DATA_KEY, slots);
            crate.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            near(problems, cases, rad(crate), 10F, "storage crate: sum");
        }
    }

    /// HazardType*.addHazardInformation ///
    private static void tooltips(List<String> problems, int[] cases) {
        tip(problems, cases, new ItemStack(it("ingot_uranium")), "[{trait.atomiclegacy.radioactive}]|0.35RAD/s");
        tip(problems, cases, new ItemStack(it("ingot_uranium"), 64), "[{trait.atomiclegacy.radioactive}]|0.35RAD/s|Stack: 22.4RAD/s");
        tip(problems, cases, new ItemStack(it("egg_balefire")), "[{trait.atomiclegacy.radioactive}]|300000.0RAD/s");
        tip(problems, cases, new ItemStack(it("rbmk_fuel_ueu")), "[{trait.atomiclegacy.radioactive}]|1.4RAD/s");
        tip(problems, cases, new ItemStack(it("nuke_fstbmb")), "[{trait.atomiclegacy.digamma}]|10.0mDRX/s");
        tip(problems, cases, new ItemStack(it("powder_caesium")), "[{trait.atomiclegacy.hydro}]|[{trait.atomiclegacy.hot}]");
        tip(problems, cases, new ItemStack(it("fleija_propellant")),
                "[{trait.atomiclegacy.radioactive}]|15.0RAD/s|[{trait.atomiclegacy.explosive}]|[{trait.atomiclegacy.blinding}]");
        tip(problems, cases, new ItemStack(it("powder_coal")), "[{trait.atomiclegacy.coal}]");
        tip(problems, cases, new ItemStack(it("brick_asbestos")), "[{trait.atomiclegacy.asbestos}]");
        tip(problems, cases, new ItemStack(it("ore_uranium")), "");
        tip(problems, cases, new ItemStack(Items.GUNPOWDER), "[{trait.atomiclegacy.explosive}]");
        List<Component> out = new ArrayList<>();
        HbmHazardSystem.appendTooltip(new ItemStack(it("nuke_fstbmb"), 2), out);
        check(problems, cases, out.size() == 3 && color(out.get(0)) == ChatFormatting.RED && color(out.get(1)) == ChatFormatting.DARK_RED
                && "Stack: 20.0mDRX/s".equals(text(out.get(2))), "digamma tooltip colors and stack line: " + flat(out));
        out.clear();
        HbmHazardSystem.appendTooltip(new ItemStack(it("ingot_u235")), out);
        check(problems, cases, out.size() == 2 && color(out.get(0)) == ChatFormatting.GREEN && color(out.get(1)) == ChatFormatting.YELLOW,
                "radiation tooltip colors");
        out.clear();
        HbmHazardSystem.appendTooltip(new ItemStack(it("lithium")), out);
        check(problems, cases, out.size() == 1 && color(out.get(0)) == ChatFormatting.RED, "hydro tooltip color");
    }

    private static void tip(List<String> problems, int[] cases, ItemStack stack, String expected) {
        List<Component> out = new ArrayList<>();
        HbmHazardSystem.appendTooltip(stack, out);
        StringBuilder b = new StringBuilder();
        for (Component c : out) b.append(b.length() == 0 ? "" : "|").append(text(c));
        check(problems, cases, expected.equals(b.toString()), BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + " x" + stack.getCount()
                + " tooltip: expected " + expected + " got " + b);
    }

    private static ChatFormatting color(Component c) {
        TextColor color = c.getStyle().getColor();
        for (ChatFormatting f : ChatFormatting.values()) if (f.isColor() && TextColor.fromLegacyFormat(f).equals(color)) return f;
        return null;
    }

    /// holder: dose per tick, hydroactive and explosive stacks ///
    private static void holder(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) throws Exception {
        ServerPlayer p = player(server, level);
        reset(p);
        p.getInventory().setItem(0, new ItemStack(it("ingot_u235"), 64));
        Method tick = RadiationEvents.class.getDeclaredMethod("tickLiving", LivingEntity.class);
        tick.setAccessible(true);
        float start = -1F;
        for (int i = 0; i < 400 && start < 0; i++) {
            p.tickCount++;
            tick.invoke(null, p);
            float r = HbmLivingRadiation.get(p).getRadiation();
            if (r > 0F) start = r;
            else Thread.sleep(2L);
        }
        if (start < 0) {
            check(problems, cases, false, "holder: 64 ingot_u235 never irradiated the player");
        } else {
            for (int i = 0; i < 20; i++) {
                p.tickCount++;
                tick.invoke(null, p);
            }
            float delta = HbmLivingRadiation.get(p).getRadiation() - start;
            check(problems, cases, Math.abs(delta - 64F) < 0.01F, "holder: 64 x ingot_u235 (1 RAD/s) for 20 ticks = 64 RAD (got " + delta + ")");
        }
        // HazardTypeHydroactive.onUpdate: wet holder -> stack consumed, explosion
        reset(p);
        p.getInventory().setItem(1, new ItemStack(it("powder_caesium"), 5));
        p.getInventory().setItem(2, new ItemStack(Items.GUNPOWDER, 3));
        HbmReactiveHazards.tickHolder(p);
        check(problems, cases, p.getInventory().getItem(1).getCount() == 5 && p.getInventory().getItem(2).getCount() == 3, "dry, not burning: nothing reacts");
        net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(Entity.class, p, true, "wasTouchingWater");
        HbmReactiveHazards.tickHolder(p);
        check(problems, cases, p.getInventory().getItem(1).isEmpty() && p.getInventory().getItem(2).getCount() == 3, "wet holder: hydroactive stack consumed, gunpowder kept");
        net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(Entity.class, p, false, "wasTouchingWater");
        reset(p);
        p.getInventory().setItem(2, new ItemStack(Items.GUNPOWDER, 3));
        p.getInventory().setItem(3, new ItemStack(it("lithium"), 2));
        p.setRemainingFireTicks(100);
        HbmReactiveHazards.tickHolder(p);
        check(problems, cases, p.getInventory().getItem(2).isEmpty() && p.getInventory().getItem(3).getCount() == 2, "burning holder: explosive stack consumed, lithium kept");
        p.setRemainingFireTicks(0);
        p.discard();
    }

    /// HazardSystem.updateDroppedItem ///
    private static void dropped(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos water = BlockPos.containing(X + 4, Y, Z);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        ItemEntity caesium = drop(level, water, new ItemStack(it("powder_caesium")));
        ItemEntity gunpowderWet = drop(level, water, new ItemStack(Items.GUNPOWDER));
        ItemEntity uranium = drop(level, water, new ItemStack(it("ingot_u235")));
        HbmReactiveHazards.updateDroppedItem(gunpowderWet);
        HbmReactiveHazards.updateDroppedItem(uranium);
        check(problems, cases, !gunpowderWet.isRemoved() && !uranium.isRemoved(), "dropped gunpowder (not burning) and ingot_u235 in water stay");
        HbmReactiveHazards.updateDroppedItem(caesium);
        check(problems, cases, caesium.isRemoved(), "dropped powder_caesium in water explodes");
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        BlockPos air = BlockPos.containing(X - 4, Y, Z);
        ItemEntity tnt = drop(level, air, new ItemStack(Items.TNT));
        ItemEntity lithium = drop(level, air, new ItemStack(it("lithium")));
        HbmReactiveHazards.updateDroppedItem(tnt);
        check(problems, cases, !tnt.isRemoved(), "dropped tnt not burning stays");
        tnt.setRemainingFireTicks(100);
        lithium.setRemainingFireTicks(100);
        HbmReactiveHazards.updateDroppedItem(lithium);
        check(problems, cases, !lithium.isRemoved(), "burning dropped lithium (hydroactive only) stays");
        HbmReactiveHazards.updateDroppedItem(tnt);
        check(problems, cases, tnt.isRemoved(), "burning dropped tnt explodes");
        check(problems, cases, HbmReactiveHazards.ITEM_HAZARD_DROP_TICKRATE == 2, "ITEM_HAZARD_DROP_TICKRATE 2");
    }

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, stack);
        item.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(item);
        return item;
    }

    /// HELPERS ///
    private static Item it(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) problems.add("parity K1: " + message);
    }

    private static String text(Component c) {
        StringBuilder b = new StringBuilder();
        if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) b.append('{').append(tc.getKey()).append('}');
        else if (c.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents pt) b.append(pt.text());
        for (Component sibling : c.getSiblings()) b.append(text(sibling));
        return b.toString();
    }

    private static String flat(List<Component> out) {
        StringBuilder b = new StringBuilder();
        for (Component c : out) b.append('[').append(text(c)).append(']');
        return b.toString();
    }

    private static ServerPlayer player(MinecraftServer server, ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "parity_k1");
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { }
        };
        net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
        player.setPos(X, Y, Z);
        return player;
    }

    private static void reset(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.getInventory().clearContent();
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        p.tickCount = 400;
        p.setRemainingFireTicks(0);
        HbmLivingRadiation d = HbmLivingRadiation.get(p);
        d.setRadiation(0F);
        d.setDigamma(0F);
        d.setEnvironmentRadiation(0F);
        d.setRadiationBuffer(0F);
        d.getContamination().clear();
        HbmLivingRadiation.set(p, d);
    }

    private static void cleanup(ServerLevel level) {
        AABB area = new AABB(X - 32, Y - 32, Z - 32, X + 32, Y + 32, Z + 32);
        for (Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) entity.discard();
    }

    /** key \t type \t base level (float32) \t modifier — generated from HazardRegistry/OreDictManager. */
    private static final String EXPECTED = """
            ancient_scrap	RADIATION	150.0	
            ball_dynamite	EXPLOSIVE	2.0	
            ballistite	EXPLOSIVE	1.0	
            billet_actinium	RADIATION	15.0	ORE
            billet_am241	RADIATION	4.25	ORE
            billet_am242	RADIATION	4.75	ORE
            billet_am_mix	RADIATION	4.5	ORE
            billet_americium_fuel	RADIATION	2.375	
            billet_au198	RADIATION	250.0	ORE
            billet_au198	HOT	2.5	ORE
            billet_balefire_gold	RADIATION	250.0	
            billet_co60	RADIATION	15.0	ORE
            billet_co60	HOT	0.5	ORE
            billet_flashlead	RADIATION	6250.0	
            billet_flashlead	HOT	7.0	
            billet_gh336	RADIATION	2.5	ORE
            billet_hes	RADIATION	2.924999952316284	
            billet_les	RADIATION	2.924999952316284	
            billet_mox_fuel	RADIATION	1.25	
            billet_neptunium	RADIATION	1.25	ORE
            billet_neptunium_fuel	RADIATION	0.75	
            billet_nuclear_waste	RADIATION	7.5	
            billet_pb209	RADIATION	5000.0	ORE
            billet_pb209	BLINDING	25.0	ORE
            billet_pb209	HOT	3.5	ORE
            billet_plutonium	RADIATION	3.75	ORE
            billet_plutonium_fuel	RADIATION	2.125	
            billet_po210be	RADIATION	112.5	
            billet_polonium	RADIATION	37.5	ORE
            billet_polonium	HOT	1.5	ORE
            billet_pu238	RADIATION	5.0	ORE
            billet_pu238	HOT	1.5	ORE
            billet_pu238be	RADIATION	15.0	
            billet_pu239	RADIATION	2.5	ORE
            billet_pu240	RADIATION	3.75	ORE
            billet_pu241	RADIATION	12.5	ORE
            billet_pu_mix	RADIATION	3.125	ORE
            billet_ra226	RADIATION	3.75	ORE
            billet_ra226be	RADIATION	11.25	
            billet_schrabidium	RADIATION	7.5	ORE
            billet_schrabidium	BLINDING	25.0	ORE
            billet_schrabidium_fuel	RADIATION	2.924999952316284	
            billet_schrabidium_fuel	BLINDING	2.5	
            billet_solinium	RADIATION	8.75	ORE
            billet_solinium	BLINDING	25.0	ORE
            billet_sr90	RADIATION	7.5	ORE
            billet_sr90	HOT	0.5	ORE
            billet_sr90	HYDROACTIVE	0.5	ORE
            billet_technetium	RADIATION	1.375	ORE
            billet_th232	RADIATION	0.05000000074505806	ORE
            billet_thorium_fuel	RADIATION	0.875	
            billet_u233	RADIATION	2.5	ORE
            billet_u235	RADIATION	0.5	ORE
            billet_u238	RADIATION	0.125	ORE
            billet_uranium	RADIATION	0.17499999701976776	ORE
            billet_uranium_fuel	RADIATION	0.25	
            billet_uzh	RADIATION	0.0625	
            block_actinium	RADIATION	300.0	ORE
            block_asbestos	ASBESTOS	10.0	ORE
            block_corium	RADIATION	150.0	
            block_corium_cobble	RADIATION	150.0	
            block_fallout	RADIATION	10.5	
            block_lithium	HYDROACTIVE	10.0	ORE
            block_mox_fuel	RADIATION	25.0	
            block_neptunium	RADIATION	25.0	ORE
            block_plutonium	RADIATION	75.0	ORE
            block_plutonium_fuel	RADIATION	42.5	
            block_polonium	RADIATION	750.0	ORE
            block_polonium	HOT	30.0	ORE
            block_pu238	RADIATION	100.0	ORE
            block_pu238	HOT	30.0	ORE
            block_pu239	RADIATION	50.0	ORE
            block_pu240	RADIATION	75.0	ORE
            block_pu_mix	RADIATION	62.5	ORE
            block_ra226	RADIATION	75.0	ORE
            block_schrabidate	RADIATION	15.0	ORE
            block_schrabidate	BLINDING	500.0	ORE
            block_schrabidium	RADIATION	150.0	ORE
            block_schrabidium	BLINDING	500.0	ORE
            block_schrabidium_fuel	RADIATION	58.5	
            block_schrabidium_fuel	BLINDING	50.0	
            block_schraranium	RADIATION	15.0	ORE
            block_schraranium	BLINDING	500.0	ORE
            block_solinium	RADIATION	175.0	ORE
            block_solinium	BLINDING	500.0	ORE
            block_thorium	RADIATION	1.0	ORE
            block_thorium_fuel	RADIATION	17.5	
            block_trinitite	RADIATION	1.0	
            block_u233	RADIATION	50.0	ORE
            block_u235	RADIATION	10.0	ORE
            block_u238	RADIATION	2.5	ORE
            block_uranium	RADIATION	3.5	ORE
            block_uranium_fuel	RADIATION	5.0	
            block_waste	RADIATION	150.0	
            block_waste_painted	RADIATION	150.0	
            block_waste_vitrified	RADIATION	75.0	
            block_white_phosphorus	HOT	50.0	ORE
            block_yellowcake	RADIATION	10.5	
            boy_bullet	RADIATION	1.0	
            boy_propellant	EXPLOSIVE	2.0	
            boy_target	RADIATION	2.0	
            brick_asbestos	ASBESTOS	1.0	
            cell_balefire	RADIATION	50.0	
            cell_sas3	RADIATION	5.0	
            cell_sas3	BLINDING	60.0	
            cell_tritium	RADIATION	0.0010000000474974513	
            cordite	EXPLOSIVE	2.0	
            crystal_lithium	HYDROACTIVE	10.0	
            crystal_phosphorus	HOT	20.0	
            crystal_plutonium	RADIATION	75.0	
            crystal_schrabidium	RADIATION	150.0	
            crystal_schraranium	RADIATION	15.0	
            crystal_thorium	RADIATION	1.0	
            crystal_trixite	RADIATION	250.0	
            crystal_uranium	RADIATION	3.5	
            debris_concrete	RADIATION	30.0	
            debris_element	RADIATION	100.0	
            debris_exchanger	RADIATION	25.0	
            debris_fuel	RADIATION	500.0	
            debris_fuel	HOT	5.0	
            debris_graphite	RADIATION	70.0	
            debris_graphite	HOT	5.0	
            debris_metal	RADIATION	5.0	
            debris_shrapnel	RADIATION	2.5	
            demon_core_closed	RADIATION	100000.0	
            demon_core_open	RADIATION	5.0	
            egg_balefire	RADIATION	300000.0	
            egg_balefire_shard	RADIATION	30000.0	
            fallout	RADIATION	60.0	
            falloutitem	RADIATION	30.0	
            fleija_core	RADIATION	10.0	
            fleija_propellant	RADIATION	15.0	
            fleija_propellant	EXPLOSIVE	8.0	
            fleija_propellant	BLINDING	50.0	
            gadget_core	RADIATION	5.0	
            gem_rad	RADIATION	25.0	
            holotape_damaged	DIGAMMA	1000.0	
            holotape_image#holo_restored	DIGAMMA	1.0	
            ingot_actinium	RADIATION	30.0	ORE
            ingot_am241	RADIATION	8.5	ORE
            ingot_am242	RADIATION	9.5	ORE
            ingot_am_mix	RADIATION	9.0	ORE
            ingot_americium_fuel	RADIATION	4.75	
            ingot_asbestos	ASBESTOS	1.0	ORE
            ingot_au198	RADIATION	500.0	ORE
            ingot_au198	HOT	5.0	ORE
            ingot_co60	RADIATION	30.0	ORE
            ingot_co60	HOT	1.0	ORE
            ingot_gh336	RADIATION	5.0	ORE
            ingot_hes	RADIATION	5.849999904632568	
            ingot_les	RADIATION	5.849999904632568	
            ingot_mox_fuel	RADIATION	2.5	
            ingot_mud	RADIATION	1.0	ORE
            ingot_neptunium	RADIATION	2.5	ORE
            ingot_neptunium_fuel	RADIATION	1.5	
            ingot_pb209	RADIATION	10000.0	ORE
            ingot_pb209	BLINDING	50.0	ORE
            ingot_pb209	HOT	7.0	ORE
            ingot_phosphorus	HOT	5.0	ORE
            ingot_plutonium	RADIATION	7.5	ORE
            ingot_plutonium_fuel	RADIATION	4.25	
            ingot_polonium	RADIATION	75.0	ORE
            ingot_polonium	HOT	3.0	ORE
            ingot_pu238	RADIATION	10.0	ORE
            ingot_pu238	HOT	3.0	ORE
            ingot_pu239	RADIATION	5.0	ORE
            ingot_pu240	RADIATION	7.5	ORE
            ingot_pu241	RADIATION	25.0	ORE
            ingot_pu_mix	RADIATION	6.25	ORE
            ingot_ra226	RADIATION	7.5	ORE
            ingot_raw_sodium	HYDROACTIVE	1.0	ORE
            ingot_raw_strontium	HOT	1.0	ORE
            ingot_raw_strontium	HYDROACTIVE	1.0	ORE
            ingot_schrabidate	RADIATION	1.5	ORE
            ingot_schrabidate	BLINDING	50.0	ORE
            ingot_schrabidium	RADIATION	15.0	ORE
            ingot_schrabidium	BLINDING	50.0	ORE
            ingot_schrabidium_fuel	RADIATION	5.849999904632568	
            ingot_schrabidium_fuel	BLINDING	5.0	
            ingot_schraranium	RADIATION	1.5	ORE
            ingot_schraranium	BLINDING	50.0	ORE
            ingot_solinium	RADIATION	17.5	ORE
            ingot_solinium	BLINDING	50.0	ORE
            ingot_sr90	RADIATION	15.0	ORE
            ingot_sr90	HOT	1.0	ORE
            ingot_sr90	HYDROACTIVE	1.0	ORE
            ingot_technetium	RADIATION	2.75	ORE
            ingot_th232	RADIATION	0.10000000149011612	ORE
            ingot_thorium_fuel	RADIATION	1.75	
            ingot_u233	RADIATION	5.0	ORE
            ingot_u235	RADIATION	1.0	ORE
            ingot_u238	RADIATION	0.25	ORE
            ingot_uranium	RADIATION	0.3499999940395355	ORE
            ingot_uranium_fuel	RADIATION	0.5	
            insert_polonium	RADIATION	100.0	
            lamp_demon	RADIATION	100000.0	
            lithium	HYDROACTIVE	1.0	ORE
            man_core	RADIATION	5.0	
            mike_core	RADIATION	0.25	
            minecraft:gunpowder	EXPLOSIVE	1.0	
            minecraft:pumpkin_pie	EXPLOSIVE	1.0	
            minecraft:tnt	EXPLOSIVE	4.0	
            nuclear_waste	RADIATION	15.0	
            nuclear_waste_long	RADIATION	5.0	
            nuclear_waste_long_depleted	RADIATION	0.5	
            nuclear_waste_long_depleted_tiny	RADIATION	0.05000000074505806	
            nuclear_waste_long_tiny	RADIATION	0.5	
            nuclear_waste_short	RADIATION	30.0	
            nuclear_waste_short	HOT	5.0	
            nuclear_waste_short_depleted	RADIATION	3.0	
            nuclear_waste_short_depleted_tiny	RADIATION	0.30000001192092896	
            nuclear_waste_short_tiny	RADIATION	3.0	
            nuclear_waste_short_tiny	HOT	5.0	
            nuclear_waste_tiny	RADIATION	1.5	
            nuclear_waste_vitrified	RADIATION	7.5	
            nuclear_waste_vitrified_tiny	RADIATION	0.75	
            nugget_actinium	RADIATION	3.0	ORE
            nugget_am241	RADIATION	0.8500000238418579	ORE
            nugget_am242	RADIATION	0.949999988079071	ORE
            nugget_am_mix	RADIATION	0.9000000357627869	ORE
            nugget_americium_fuel	RADIATION	0.4749999940395355	
            nugget_au198	RADIATION	50.0	ORE
            nugget_au198	HOT	0.5	ORE
            nugget_co60	RADIATION	3.0	ORE
            nugget_co60	HOT	0.10000000149011612	ORE
            nugget_gh336	RADIATION	0.5	ORE
            nugget_hes	RADIATION	0.5849999785423279	
            nugget_les	RADIATION	0.5849999785423279	
            nugget_mox_fuel	RADIATION	0.25	
            nugget_neptunium	RADIATION	0.25	ORE
            nugget_neptunium_fuel	RADIATION	0.15000000596046448	
            nugget_pb209	RADIATION	1000.0	ORE
            nugget_pb209	BLINDING	5.0	ORE
            nugget_pb209	HOT	0.699999988079071	ORE
            nugget_plutonium	RADIATION	0.75	ORE
            nugget_plutonium_fuel	RADIATION	0.42500001192092896	
            nugget_polonium	RADIATION	7.5	ORE
            nugget_polonium	HOT	0.30000001192092896	ORE
            nugget_pu238	RADIATION	1.0	ORE
            nugget_pu238	HOT	0.30000001192092896	ORE
            nugget_pu239	RADIATION	0.5	ORE
            nugget_pu240	RADIATION	0.75	ORE
            nugget_pu241	RADIATION	2.5	ORE
            nugget_pu_mix	RADIATION	0.625	ORE
            nugget_ra226	RADIATION	0.75	ORE
            nugget_schrabidium	RADIATION	1.5	ORE
            nugget_schrabidium	BLINDING	5.0	ORE
            nugget_schrabidium_fuel	RADIATION	0.5849999785423279	
            nugget_schrabidium_fuel	BLINDING	0.5	
            nugget_solinium	RADIATION	1.75	ORE
            nugget_solinium	BLINDING	5.0	ORE
            nugget_sr90	RADIATION	1.5	ORE
            nugget_sr90	HOT	0.10000000149011612	ORE
            nugget_sr90	HYDROACTIVE	0.10000000149011612	ORE
            nugget_technetium	RADIATION	0.2750000059604645	ORE
            nugget_th232	RADIATION	0.010000000707805157	ORE
            nugget_thorium_fuel	RADIATION	0.17499999701976776	
            nugget_u233	RADIATION	0.5	ORE
            nugget_u235	RADIATION	0.10000000149011612	ORE
            nugget_u238	RADIATION	0.02500000037252903	ORE
            nugget_uranium	RADIATION	0.03500000014901161	ORE
            nugget_uranium_fuel	RADIATION	0.05000000074505806	
            nuke_fstbmb	DIGAMMA	0.009999999776482582	
            ore_asbestos	ASBESTOS	1.0	ORE
            ore_basalt#asbestos	ASBESTOS	1.0	ORE
            ore_enriched#asbestos	ASBESTOS	1.0	ORE
            ore_gneiss_asbestos	ASBESTOS	1.0	ORE
            ore_gneiss_lithium	HYDROACTIVE	1.0	ORE
            ore_gneiss_schrabidium	RADIATION	15.0	ORE
            ore_gneiss_schrabidium	BLINDING	50.0	ORE
            ore_nether_plutonium	RADIATION	7.5	ORE
            ore_nether_schrabidium	RADIATION	15.0	ORE
            ore_nether_schrabidium	BLINDING	50.0	ORE
            ore_schrabidium	RADIATION	15.0	ORE
            ore_schrabidium	BLINDING	50.0	ORE
            ore_sellafield_radgem	RADIATION	25.0	
            ore_sellafield_schrabidium	RADIATION	15.0	ORE
            ore_sellafield_schrabidium	BLINDING	50.0	ORE
            pellet_rtg	RADIATION	15.0	RTG_RADIATION(0.0)
            pellet_rtg	HOT	3.0	
            pellet_rtg_actinium	RADIATION	45.0	RTG_RADIATION(0.0)
            pellet_rtg_americium	RADIATION	12.75	RTG_RADIATION(0.0)
            pellet_rtg_cobalt	RADIATION	45.0	RTG_RADIATION(0.0)
            pellet_rtg_depleted#neptunium	RADIATION	3.75	
            pellet_rtg_gold	RADIATION	750.0	RTG_RADIATION(0.0)
            pellet_rtg_gold	HOT	5.0	
            pellet_rtg_lead	RADIATION	15000.0	RTG_RADIATION(0.0)
            pellet_rtg_lead	HOT	7.0	
            pellet_rtg_lead	BLINDING	50.0	
            pellet_rtg_polonium	RADIATION	112.5	RTG_RADIATION(0.0)
            pellet_rtg_polonium	HOT	3.0	
            pellet_rtg_radium	RADIATION	11.25	RTG_RADIATION(0.0)
            pellet_rtg_strontium	RADIATION	22.5	RTG_RADIATION(0.0)
            pellet_rtg_weak	RADIATION	5.25	RTG_RADIATION(0.0)
            pile_rod_nu	RADIATION	0.5249999761581421	
            pile_rod_plutonium	RADIATION	6.425000190734863	
            pile_rod_po210be	RADIATION	337.5	
            pile_rod_pu239	RADIATION	5.800000190734863	
            pile_rod_pu239_graphite	RADIATION	7.5	
            pile_rod_ra226be	RADIATION	33.75	
            pile_rod_rgp	RADIATION	9.375	
            pile_rod_source	RADIATION	33.75	
            pile_rod_thorium	RADIATION	0.15000000596046448	
            pile_rod_thorium_fuel	RADIATION	2.625	
            pile_rod_uranium	RADIATION	0.5249999761581421	
            pile_rod_waste	RADIATION	22.5	
            plate_cast_schrabidium	RADIATION	45.0	ORE
            plate_cast_schrabidium	BLINDING	150.0	ORE
            plate_fuel_mox	RADIATION	2.5	FUEL_RADIATION(240.0)
            plate_fuel_pu238be	RADIATION	15.0	FUEL_RADIATION(3.0)
            plate_fuel_pu239	RADIATION	5.0	FUEL_RADIATION(202.5)
            plate_fuel_ra226be	RADIATION	11.25	FUEL_RADIATION(67.5)
            plate_fuel_sa326	RADIATION	15.0	FUEL_RADIATION(150.0)
            plate_fuel_sa326	BLINDING	20.0	
            plate_fuel_u233	RADIATION	5.0	FUEL_RADIATION(195.0)
            plate_fuel_u235	RADIATION	1.0	FUEL_RADIATION(150.0)
            plate_schrabidium	RADIATION	15.0	ORE
            plate_schrabidium	BLINDING	50.0	ORE
            powder_actinium	RADIATION	90.0	ORE
            powder_actinium_tiny	RADIATION	9.0	ORE
            powder_asbestos	ASBESTOS	3.0	ORE
            powder_at209	RADIATION	22500.0	ORE
            powder_at209	HOT	60.0	ORE
            powder_au198	RADIATION	1500.0	ORE
            powder_au198	HOT	15.0	ORE
            powder_balefire	RADIATION	500.0	
            powder_caesium	HYDROACTIVE	1.0	
            powder_caesium	HOT	3.0	
            powder_co60	RADIATION	90.0	ORE
            powder_co60	HOT	3.0	ORE
            powder_coal	COAL	3.0	ORE
            powder_coal_tiny	COAL	0.30000001192092896	ORE
            powder_coltan_ore	ASBESTOS	3.0	
            powder_cs137	RADIATION	60.0	ORE
            powder_cs137	HOT	9.0	ORE
            powder_cs137	HYDROACTIVE	9.0	ORE
            powder_cs137_tiny	RADIATION	6.0	ORE
            powder_cs137_tiny	HOT	0.9000000357627869	ORE
            powder_cs137_tiny	HYDROACTIVE	0.9000000357627869	ORE
            powder_i131	RADIATION	450.0	ORE
            powder_i131	HOT	3.0	ORE
            powder_i131_tiny	RADIATION	45.0	ORE
            powder_i131_tiny	HOT	0.30000001192092896	ORE
            powder_lignite	COAL	3.0	ORE
            powder_lithium	HYDROACTIVE	3.0	ORE
            powder_lithium_tiny	HYDROACTIVE	0.30000001192092896	ORE
            powder_neptunium	RADIATION	7.5	ORE
            powder_plutonium	RADIATION	22.5	ORE
            powder_polonium	RADIATION	225.0	ORE
            powder_polonium	HOT	9.0	ORE
            powder_ra226	RADIATION	22.5	ORE
            powder_schrabidate	RADIATION	4.5	ORE
            powder_schrabidate	BLINDING	150.0	ORE
            powder_schrabidium	RADIATION	45.0	ORE
            powder_schrabidium	BLINDING	150.0	ORE
            powder_sodium	HYDROACTIVE	3.0	ORE
            powder_sr90	RADIATION	45.0	ORE
            powder_sr90	HOT	3.0	ORE
            powder_sr90	HYDROACTIVE	3.0	ORE
            powder_sr90_tiny	RADIATION	4.5	ORE
            powder_sr90_tiny	HOT	0.30000001192092896	ORE
            powder_sr90_tiny	HYDROACTIVE	0.30000001192092896	ORE
            powder_strontium	HOT	3.0	ORE
            powder_strontium	HYDROACTIVE	3.0	ORE
            powder_thorium	RADIATION	0.30000001192092896	ORE
            powder_uranium	RADIATION	1.0499999523162842	ORE
            powder_xe135	RADIATION	3750.0	ORE
            powder_xe135	HOT	30.0	ORE
            powder_xe135_tiny	RADIATION	375.0	ORE
            powder_xe135_tiny	HOT	3.0	ORE
            powder_yellowcake	RADIATION	1.0499999523162842	
            pwr_fuel#bfb_am_mix	RADIATION	4.5	
            pwr_fuel#bfb_pu241	RADIATION	12.5	
            pwr_fuel#hea242	RADIATION	9.5	
            pwr_fuel#hen237	RADIATION	2.5	
            pwr_fuel#hep239	RADIATION	5.0	
            pwr_fuel#hep241	RADIATION	25.0	
            pwr_fuel#hes326	RADIATION	15.0	
            pwr_fuel#hes327	RADIATION	17.5	
            pwr_fuel#heu233	RADIATION	5.0	
            pwr_fuel#heu235	RADIATION	1.0	
            pwr_fuel#mea	RADIATION	9.0	
            pwr_fuel#men	RADIATION	1.5	
            pwr_fuel#mep	RADIATION	6.25	
            pwr_fuel#meu	RADIATION	0.5	
            pwr_fuel#mox	RADIATION	2.5	
            pwr_fuel_depleted#bfb_am_mix	RADIATION	45.0	
            pwr_fuel_depleted#bfb_pu241	RADIATION	125.0	
            pwr_fuel_depleted#hea242	RADIATION	95.0	
            pwr_fuel_depleted#hen237	RADIATION	25.0	
            pwr_fuel_depleted#hep239	RADIATION	50.0	
            pwr_fuel_depleted#hep241	RADIATION	250.0	
            pwr_fuel_depleted#hes326	RADIATION	150.0	
            pwr_fuel_depleted#hes327	RADIATION	175.0	
            pwr_fuel_depleted#heu233	RADIATION	50.0	
            pwr_fuel_depleted#heu235	RADIATION	10.0	
            pwr_fuel_depleted#mea	RADIATION	90.0	
            pwr_fuel_depleted#men	RADIATION	15.0	
            pwr_fuel_depleted#mep	RADIATION	62.5	
            pwr_fuel_depleted#meu	RADIATION	5.0	
            pwr_fuel_depleted#mox	RADIATION	25.0	
            pwr_fuel_hot#bfb_am_mix	RADIATION	45.0	
            pwr_fuel_hot#bfb_am_mix	HOT	5.0	
            pwr_fuel_hot#bfb_pu241	RADIATION	125.0	
            pwr_fuel_hot#bfb_pu241	HOT	5.0	
            pwr_fuel_hot#hea242	RADIATION	95.0	
            pwr_fuel_hot#hea242	HOT	5.0	
            pwr_fuel_hot#hen237	RADIATION	25.0	
            pwr_fuel_hot#hen237	HOT	5.0	
            pwr_fuel_hot#hep239	RADIATION	50.0	
            pwr_fuel_hot#hep239	HOT	5.0	
            pwr_fuel_hot#hep241	RADIATION	250.0	
            pwr_fuel_hot#hep241	HOT	5.0	
            pwr_fuel_hot#hes326	RADIATION	150.0	
            pwr_fuel_hot#hes326	HOT	5.0	
            pwr_fuel_hot#hes327	RADIATION	175.0	
            pwr_fuel_hot#hes327	HOT	5.0	
            pwr_fuel_hot#heu233	RADIATION	50.0	
            pwr_fuel_hot#heu233	HOT	5.0	
            pwr_fuel_hot#heu235	RADIATION	10.0	
            pwr_fuel_hot#heu235	HOT	5.0	
            pwr_fuel_hot#mea	RADIATION	90.0	
            pwr_fuel_hot#mea	HOT	5.0	
            pwr_fuel_hot#men	RADIATION	15.0	
            pwr_fuel_hot#men	HOT	5.0	
            pwr_fuel_hot#mep	RADIATION	62.5	
            pwr_fuel_hot#mep	HOT	5.0	
            pwr_fuel_hot#meu	RADIATION	5.0	
            pwr_fuel_hot#meu	HOT	5.0	
            pwr_fuel_hot#mox	RADIATION	25.0	
            pwr_fuel_hot#mox	HOT	5.0	
            rbmk_fuel_balefire	RADIATION	1200000.0	RBMK_RADIATION(120000000.0,true)
            rbmk_fuel_balefire	HOT	0.0	RBMK_HOT
            rbmk_fuel_balefire_gold	RADIATION	2000.0	RBMK_RADIATION(600000.0,true)
            rbmk_fuel_balefire_gold	HOT	0.0	RBMK_HOT
            rbmk_fuel_drx	RADIATION	1200000.0	RBMK_RADIATION(120000000.0,true)
            rbmk_fuel_drx	HOT	0.0	RBMK_HOT
            rbmk_fuel_drx	DIGAMMA	0.3333333432674408	
            rbmk_fuel_flashlead	RADIATION	50000.0	RBMK_RADIATION(200.0,true)
            rbmk_fuel_flashlead	HOT	0.0	RBMK_HOT
            rbmk_fuel_hea241	RADIATION	34.0	RBMK_RADIATION(2010.0,false)
            rbmk_fuel_hea241	HOT	0.0	RBMK_HOT
            rbmk_fuel_hea242	RADIATION	38.0	RBMK_RADIATION(2040.0,false)
            rbmk_fuel_hea242	HOT	0.0	RBMK_HOT
            rbmk_fuel_heaus	RADIATION	0.0	RBMK_RADIATION(1950.0,false)
            rbmk_fuel_heaus	HOT	0.0	RBMK_HOT
            rbmk_fuel_hen	RADIATION	10.0	RBMK_RADIATION(1800.0,false)
            rbmk_fuel_hen	HOT	0.0	RBMK_HOT
            rbmk_fuel_hep	RADIATION	20.0	RBMK_RADIATION(1950.0,false)
            rbmk_fuel_hep	HOT	0.0	RBMK_HOT
            rbmk_fuel_hep241	RADIATION	100.0	RBMK_RADIATION(2100.0,false)
            rbmk_fuel_hep241	HOT	0.0	RBMK_HOT
            rbmk_fuel_hes	RADIATION	23.399999618530273	RBMK_RADIATION(3000.0,false)
            rbmk_fuel_hes	HOT	0.0	RBMK_HOT
            rbmk_fuel_heu233	RADIATION	20.0	RBMK_RADIATION(1860.0,false)
            rbmk_fuel_heu233	HOT	0.0	RBMK_HOT
            rbmk_fuel_heu235	RADIATION	4.0	RBMK_RADIATION(1800.0,false)
            rbmk_fuel_heu235	HOT	0.0	RBMK_HOT
            rbmk_fuel_lea	RADIATION	19.0	RBMK_RADIATION(1560.0,false)
            rbmk_fuel_lea	HOT	0.0	RBMK_HOT
            rbmk_fuel_leaus	RADIATION	0.0	RBMK_RADIATION(2250.0,false)
            rbmk_fuel_leaus	HOT	0.0	RBMK_HOT
            rbmk_fuel_lep	RADIATION	17.0	RBMK_RADIATION(1500.0,false)
            rbmk_fuel_lep	HOT	0.0	RBMK_HOT
            rbmk_fuel_les	RADIATION	23.399999618530273	RBMK_RADIATION(1470.0,false)
            rbmk_fuel_les	HOT	0.0	RBMK_HOT
            rbmk_fuel_mea	RADIATION	36.0	RBMK_RADIATION(1830.0,false)
            rbmk_fuel_mea	HOT	0.0	RBMK_HOT
            rbmk_fuel_men	RADIATION	6.0	RBMK_RADIATION(1350.0,false)
            rbmk_fuel_men	HOT	0.0	RBMK_HOT
            rbmk_fuel_mep	RADIATION	25.0	RBMK_RADIATION(1800.0,false)
            rbmk_fuel_mep	HOT	0.0	RBMK_HOT
            rbmk_fuel_mes	RADIATION	23.399999618530273	RBMK_RADIATION(1800.0,false)
            rbmk_fuel_mes	HOT	0.0	RBMK_HOT
            rbmk_fuel_meu	RADIATION	2.0	RBMK_RADIATION(1290.0,false)
            rbmk_fuel_meu	HOT	0.0	RBMK_HOT
            rbmk_fuel_mox	RADIATION	10.0	RBMK_RADIATION(1530.0,false)
            rbmk_fuel_mox	HOT	0.0	RBMK_HOT
            rbmk_fuel_po210be	RADIATION	900.0	RBMK_RADIATION(90.0,true)
            rbmk_fuel_po210be	HOT	0.0	RBMK_HOT
            rbmk_fuel_pu238be	RADIATION	120.0	RBMK_RADIATION(150.0,false)
            rbmk_fuel_pu238be	HOT	0.0	RBMK_HOT
            rbmk_fuel_ra226be	RADIATION	90.0	RBMK_RADIATION(36.0,true)
            rbmk_fuel_ra226be	HOT	0.0	RBMK_HOT
            rbmk_fuel_thmeu	RADIATION	7.0	RBMK_RADIATION(1050.0,false)
            rbmk_fuel_thmeu	HOT	0.0	RBMK_HOT
            rbmk_fuel_ueu	RADIATION	1.399999976158142	RBMK_RADIATION(1200.0,false)
            rbmk_fuel_ueu	HOT	0.0	RBMK_HOT
            rbmk_fuel_uzh	RADIATION	0.5	RBMK_RADIATION(1200.0,false)
            rbmk_fuel_uzh	HOT	0.0	RBMK_HOT
            rbmk_fuel_zfb_am_mix	RADIATION	10.0	RBMK_RADIATION(600.0,false)
            rbmk_fuel_zfb_am_mix	HOT	0.0	RBMK_HOT
            rbmk_fuel_zfb_bismuth	RADIATION	10.0	RBMK_RADIATION(300.0,false)
            rbmk_fuel_zfb_bismuth	HOT	0.0	RBMK_HOT
            rbmk_fuel_zfb_pu241	RADIATION	2.0	RBMK_RADIATION(450.0,false)
            rbmk_fuel_zfb_pu241	HOT	0.0	RBMK_HOT
            rbmk_pellet_balefire	RADIATION	150000.0	RBMK_RADIATION(15000000.0,true)
            rbmk_pellet_balefire_gold	RADIATION	250.0	RBMK_RADIATION(75000.0,true)
            rbmk_pellet_drx	RADIATION	150000.0	RBMK_RADIATION(15000000.0,true)
            rbmk_pellet_drx	DIGAMMA	0.0416666679084301	
            rbmk_pellet_flashlead	RADIATION	6250.0	RBMK_RADIATION(50.0,true)
            rbmk_pellet_hea241	RADIATION	4.25	RBMK_RADIATION(251.25,false)
            rbmk_pellet_hea242	RADIATION	4.75	RBMK_RADIATION(255.0,false)
            rbmk_pellet_heaus	RADIATION	0.0	RBMK_RADIATION(243.75,false)
            rbmk_pellet_hen	RADIATION	1.25	RBMK_RADIATION(225.0,false)
            rbmk_pellet_hep239	RADIATION	2.5	RBMK_RADIATION(243.75,false)
            rbmk_pellet_hep241	RADIATION	12.5	RBMK_RADIATION(262.5,false)
            rbmk_pellet_hes	RADIATION	2.924999952316284	RBMK_RADIATION(375.0,false)
            rbmk_pellet_heu233	RADIATION	2.5	RBMK_RADIATION(232.5,false)
            rbmk_pellet_heu235	RADIATION	0.5	RBMK_RADIATION(225.0,false)
            rbmk_pellet_lea	RADIATION	2.375	RBMK_RADIATION(195.0,false)
            rbmk_pellet_leaus	RADIATION	0.0	RBMK_RADIATION(281.25,false)
            rbmk_pellet_lep	RADIATION	2.125	RBMK_RADIATION(187.5,false)
            rbmk_pellet_les	RADIATION	2.924999952316284	RBMK_RADIATION(183.75,false)
            rbmk_pellet_mea	RADIATION	4.5	RBMK_RADIATION(228.75,false)
            rbmk_pellet_men	RADIATION	0.75	RBMK_RADIATION(168.75,false)
            rbmk_pellet_mep	RADIATION	3.125	RBMK_RADIATION(225.0,false)
            rbmk_pellet_mes	RADIATION	2.924999952316284	RBMK_RADIATION(225.0,false)
            rbmk_pellet_meu	RADIATION	0.25	RBMK_RADIATION(161.25,false)
            rbmk_pellet_mox	RADIATION	1.25	RBMK_RADIATION(191.25,false)
            rbmk_pellet_po210be	RADIATION	112.5	RBMK_RADIATION(11.25,true)
            rbmk_pellet_pu238be	RADIATION	15.0	RBMK_RADIATION(22.5,false)
            rbmk_pellet_ra226be	RADIATION	11.25	RBMK_RADIATION(4.5,true)
            rbmk_pellet_thmeu	RADIATION	0.875	RBMK_RADIATION(131.25,false)
            rbmk_pellet_ueu	RADIATION	0.17499999701976776	RBMK_RADIATION(150.0,false)
            rbmk_pellet_uzh	RADIATION	0.0625	RBMK_RADIATION(150.0,false)
            rbmk_pellet_zfb_am_mix	RADIATION	1.25	RBMK_RADIATION(75.0,false)
            rbmk_pellet_zfb_bismuth	RADIATION	1.25	RBMK_RADIATION(37.5,false)
            rbmk_pellet_zfb_pu241	RADIATION	0.25	RBMK_RADIATION(56.25,false)
            rod#ac227	RADIATION	30.0	
            rod#co60	RADIATION	30.0	
            rod#np237	RADIATION	2.5	
            rod#pu238	RADIATION	10.0	
            rod#pu239	RADIATION	5.0	
            rod#ra226	RADIATION	7.5	
            rod#rgp	RADIATION	6.25	
            rod#th232	RADIATION	0.10000000149011612	
            rod#thf	RADIATION	1.75	
            rod#tritium	RADIATION	0.0010000000474974513	
            rod#u235	RADIATION	1.0	
            rod#u238	RADIATION	0.25	
            rod#uranium	RADIATION	0.3499999940395355	
            rod#waste	RADIATION	15.0	
            rod_dual#ac227	RADIATION	30.0	
            rod_dual#co60	RADIATION	30.0	
            rod_dual#np237	RADIATION	2.5	
            rod_dual#pu238	RADIATION	10.0	
            rod_dual#pu239	RADIATION	5.0	
            rod_dual#ra226	RADIATION	7.5	
            rod_dual#rgp	RADIATION	6.25	
            rod_dual#th232	RADIATION	0.10000000149011612	
            rod_dual#thf	RADIATION	1.75	
            rod_dual#tritium	RADIATION	0.0010000000474974513	
            rod_dual#u235	RADIATION	1.0	
            rod_dual#u238	RADIATION	0.25	
            rod_dual#uranium	RADIATION	0.3499999940395355	
            rod_dual#waste	RADIATION	15.0	
            rod_quad#ac227	RADIATION	60.0	
            rod_quad#co60	RADIATION	60.0	
            rod_quad#np237	RADIATION	5.0	
            rod_quad#pu238	RADIATION	20.0	
            rod_quad#pu239	RADIATION	10.0	
            rod_quad#ra226	RADIATION	15.0	
            rod_quad#rgp	RADIATION	12.5	
            rod_quad#th232	RADIATION	0.20000000298023224	
            rod_quad#thf	RADIATION	3.5	
            rod_quad#tritium	RADIATION	0.0020000000949949026	
            rod_quad#u235	RADIATION	2.0	
            rod_quad#u238	RADIATION	0.5	
            rod_quad#uranium	RADIATION	0.699999988079071	
            rod_quad#waste	RADIATION	30.0	
            rod_zirnox#les_fuel	RADIATION	5.849999904632568	FUEL_RADIATION(225.0)
            rod_zirnox#lithium_fuel	RADIATION	0.0	FUEL_RADIATION(0.0010000000474974513)
            rod_zirnox#mox_fuel	RADIATION	2.5	FUEL_RADIATION(150.0)
            rod_zirnox#natural_uranium_fuel	RADIATION	0.3499999940395355	FUEL_RADIATION(172.5)
            rod_zirnox#plutonium_fuel	RADIATION	4.25	FUEL_RADIATION(187.5)
            rod_zirnox#th232_fuel	RADIATION	0.10000000149011612	FUEL_RADIATION(1.75)
            rod_zirnox#thorium_fuel	RADIATION	1.75	FUEL_RADIATION(112.5)
            rod_zirnox#u233_fuel	RADIATION	5.0	FUEL_RADIATION(150.0)
            rod_zirnox#u235_fuel	RADIATION	1.0	FUEL_RADIATION(165.0)
            rod_zirnox#uranium_fuel	RADIATION	0.5	FUEL_RADIATION(150.0)
            rod_zirnox#zfb_mox_fuel	RADIATION	2.5	FUEL_RADIATION(75.0)
            rod_zirnox_les_fuel_depleted	RADIATION	225.0	
            rod_zirnox_les_fuel_depleted	BLINDING	20.0	
            rod_zirnox_mox_fuel_depleted	RADIATION	150.0	
            rod_zirnox_natural_uranium_fuel_depleted	RADIATION	172.5	
            rod_zirnox_plutonium_fuel_depleted	RADIATION	187.5	
            rod_zirnox_thorium_fuel_depleted	RADIATION	112.5	
            rod_zirnox_tritium	RADIATION	0.0010000000474974513	
            rod_zirnox_u233_fuel_depleted	RADIATION	150.0	
            rod_zirnox_u235_fuel_depleted	RADIATION	165.0	
            rod_zirnox_uranium_fuel_depleted	RADIATION	150.0	
            rod_zirnox_zfb_mox_depleted	RADIATION	75.0	
            scrap_nuclear	RADIATION	1.0	
            sellafield#0	RADIATION	0.5	
            sellafield#1	RADIATION	1.0	
            sellafield#2	RADIATION	2.5	
            sellafield#3	RADIATION	4.0	
            sellafield#4	RADIATION	5.0	
            sellafield#5	RADIATION	10.0	
            solid_fuel_bf	RADIATION	1000.0	
            solid_fuel_presto_bf	RADIATION	2000.0	
            solid_fuel_presto_triplet_bf	RADIATION	6000.0	
            solinium_core	RADIATION	14.0	
            solinium_core	BLINDING	45.0	
            solinium_propellant	EXPLOSIVE	10.0	
            stick_c4	EXPLOSIVE	2.5	
            stick_dynamite	EXPLOSIVE	1.0	
            stick_semtex	EXPLOSIVE	2.5	
            stick_tnt	EXPLOSIVE	1.5	
            stone_resource_asbestos	ASBESTOS	1.0	ORE
            tile_lab_broken	ASBESTOS	1.0	
            trinitite	RADIATION	0.10000000149011612	
            tsar_core	RADIATION	7.5	
            waste_mox#cold	RADIATION	5.625	
            waste_mox#hot	RADIATION	75.0	
            waste_mox#hot	HOT	5.0	
            waste_natural_uranium#cold	RADIATION	6.468750476837158	
            waste_natural_uranium#hot	RADIATION	86.25	
            waste_natural_uranium#hot	HOT	5.0	
            waste_plate_mox#cold	RADIATION	18.0	
            waste_plate_mox#hot	RADIATION	240.0	
            waste_plate_mox#hot	HOT	5.0	
            waste_plate_pu238be#cold	RADIATION	3.0	
            waste_plate_pu238be#hot	RADIATION	3.0	
            waste_plate_pu238be#hot	HOT	5.0	
            waste_plate_pu239#cold	RADIATION	15.187500953674316	
            waste_plate_pu239#hot	RADIATION	202.5	
            waste_plate_pu239#hot	HOT	5.0	
            waste_plate_ra226be#cold	RADIATION	67.5	
            waste_plate_ra226be#hot	RADIATION	67.5	
            waste_plate_ra226be#hot	HOT	5.0	
            waste_plate_sa326#cold	RADIATION	11.25	
            waste_plate_sa326#hot	RADIATION	150.0	
            waste_plate_sa326#hot	HOT	5.0	
            waste_plate_u233#cold	RADIATION	14.625000953674316	
            waste_plate_u233#hot	RADIATION	195.0	
            waste_plate_u233#hot	HOT	5.0	
            waste_plate_u235#cold	RADIATION	11.25	
            waste_plate_u235#hot	RADIATION	150.0	
            waste_plate_u235#hot	HOT	5.0	
            waste_plutonium#cold	RADIATION	7.031250476837158	
            waste_plutonium#hot	RADIATION	93.75	
            waste_plutonium#hot	HOT	5.0	
            waste_schrabidium#cold	RADIATION	8.4375	
            waste_schrabidium#hot	RADIATION	112.5	
            waste_schrabidium#hot	HOT	5.0	
            waste_thorium#cold	RADIATION	4.21875	
            waste_thorium#hot	RADIATION	56.25	
            waste_thorium#hot	HOT	5.0	
            waste_u233#cold	RADIATION	5.625	
            waste_u233#hot	RADIATION	75.0	
            waste_u233#hot	HOT	5.0	
            waste_u235#cold	RADIATION	6.187500476837158	
            waste_u235#hot	RADIATION	82.5	
            waste_u235#hot	HOT	5.0	
            waste_uranium#cold	RADIATION	5.625	
            waste_uranium#hot	RADIATION	75.0	
            waste_uranium#hot	HOT	5.0	
            waste_zfb_mox#cold	RADIATION	2.8125	
            waste_zfb_mox#hot	RADIATION	37.5	
            waste_zfb_mox#hot	HOT	5.0	
            watz_pellet#du	RADIATION	1.0	
            watz_pellet#hen	RADIATION	10.0	
            watz_pellet#hes	RADIATION	23.399999618530273	
            watz_pellet#les	RADIATION	23.399999618530273	
            watz_pellet#mep	RADIATION	25.0	
            watz_pellet#mes	RADIATION	23.399999618530273	
            watz_pellet#meu	RADIATION	2.0	
            watz_pellet#nqd	RADIATION	4.0	
            watz_pellet#nqr	RADIATION	20.0	
            watz_pellet#schrabidium	RADIATION	60.0	
            yellow_barrel	RADIATION	150.0	
            """;
}
