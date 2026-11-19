package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyItemParity;
import com.atomiclegacy.items.LegacyLoreItem;
import com.atomiclegacy.items.LegacyTooltips;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/**
 * [e3] Item properties and static tooltips against the 1.7.10 ModItems table
 * (legacy/atomiclegacy/parity-e3-items.tsv, see items/LegacyItemParity): stack size, durability, container item,
 * rarity, constant glint, ItemCustomLore ".desc" keys, the static tooltips of items/LegacyTooltips and the lang keys
 * used by the tooltips ported in this pass. Runs on a dedicated server: only LegacyLoreItem#appendHoverText is called
 * (other item classes may query the keyboard). Not covered: NBT/Shift dependent tooltips and their colours (manual).
 */
final class ParityE3TooltipDiagnostics {
    private static final int LISTED = 12;

    /** Lang keys read by tooltips rewritten in this pass (texts copied from the 1.7.10 en_US.lang). */
    private static final String[] KEYS = {
            "desc.item.pileRod", "desc.item.rtgHeat", "desc.item.rtgDecay",
            "trait.atomiclegacy.hlParticle", "trait.atomiclegacy.hlPlayer", "trait.atomiclegacy.drop",
            "trait.atomiclegacy.digamma", "item.atomiclegacy.conveyor_wand.desc",
            "item.atomiclegacy.conveyor_wand.vertical.desc", "item.atomiclegacy.fluid_identifier_multi.info",
            "item.atomiclegacy.fluid_identifier_multi.info2", "item.atomiclegacy.rag.desc",
            "item.atomiclegacy.pile_rod_uranium.desc", "item.atomiclegacy.pellet_rtg.desc",
            "armor.electricJetpack", "armor.glider", "armor.moreAmmo", "armor.rocketBoots", "armor.fastFall",
            "armor.sprintBoost"
    };

    private ParityE3TooltipDiagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            properties(problems, cases);
            staticTooltips(problems, cases);
            for (String key : KEYS) {
                cases[0]++;
                if (missing(key)) {
                    problems.add("E3 tooltips: lang key " + key + " is missing");
                }
            }
        } catch (Exception exception) {
            problems.add("E3 tooltips: diagnostics threw " + exception);
        }
        return cases[0];
    }

    private static void properties(List<String> problems, int[] cases) {
        List<String> stack = new ArrayList<>();
        List<String> damage = new ArrayList<>();
        List<String> container = new ArrayList<>();
        List<String> rarity = new ArrayList<>();
        List<String> glint = new ArrayList<>();
        List<String> desc = new ArrayList<>();
        int present = 0;
        for (LegacyItemParity.Row row : LegacyItemParity.table().values()) {
            ResourceLocation id = AtomicLegacy.id(row.id());
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            present++;
            Item item = BuiltInRegistries.ITEM.get(id);
            ItemStack itemStack = new ItemStack(item);

            // 64 / 0 are table defaults (no setMaxStackSize / setMaxDamage in ModItems; the class constructor may set them)
            if (row.stack() > 0 && row.stack() != 64) {
                cases[0]++;
                if (itemStack.getMaxStackSize() != row.stack()) {
                    stack.add(row.id() + " " + itemStack.getMaxStackSize() + "!=" + row.stack());
                }
            }
            // pipettes: ItemPipette#setMaxDamage(1) only made the item non-stackable/damageable; the port keeps stack 1 without durability (parity-e3.md)
            if (row.damage() > 0 && !row.id().startsWith("pipette")) {
                cases[0]++;
                if (itemStack.getMaxDamage() != row.damage()) {
                    damage.add(row.id() + " " + itemStack.getMaxDamage() + "!=" + row.damage());
                }
            }
            if (!row.container().isEmpty() && !row.container().startsWith("?") && !row.multi()) {
                ResourceLocation expectedId = row.container().indexOf(':') >= 0
                        ? ResourceLocation.parse(row.container()) : AtomicLegacy.id(row.container());
                if (BuiltInRegistries.ITEM.containsKey(expectedId)) {
                    cases[0]++;
                    ItemStack remainder = item.getCraftingRemainingItem(itemStack);
                    if (!remainder.is(BuiltInRegistries.ITEM.get(expectedId))) {
                        container.add(row.id() + " " + BuiltInRegistries.ITEM.getKey(remainder.getItem()).getPath()
                                + "!=" + expectedId.getPath());
                    }
                }
            }
            Rarity expectedRarity = row.expectedRarity();
            if (expectedRarity != null) {
                cases[0]++;
                if (itemStack.getRarity() != expectedRarity) {
                    rarity.add(row.id() + " " + itemStack.getRarity() + "!=" + expectedRarity);
                }
            }
            if (!row.effect().isEmpty()) {
                cases[0]++;
                if (itemStack.hasFoil() != "1".equals(row.effect())) {
                    glint.add(row.id() + " glint " + itemStack.hasFoil());
                }
            }
            if (row.desc()) {
                cases[0]++;
                String key = "item." + AtomicLegacy.MOD_ID + "." + row.id() + ".desc";
                if (missing(key)) {
                    desc.add(row.id() + " (no lang key)");
                } else if (item instanceof LegacyLoreItem) {
                    List<Component> lines = new ArrayList<>();
                    item.appendHoverText(itemStack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
                    int expected = Component.translatable(key).getString().split("\\$").length;
                    if (lines.size() != expected) {
                        desc.add(row.id() + " " + lines.size() + " lines!=" + expected);
                    }
                }
            }
        }
        cases[0]++;
        if (present < 1000) {
            problems.add("E3 tooltips: only " + present + " of the 1.7.10 ModItems ids are registered (table or ids broken)");
        }
        report(problems, "stack size", stack);
        report(problems, "durability", damage);
        report(problems, "container item", container);
        report(problems, "rarity", rarity);
        report(problems, "hasEffect", glint);
        report(problems, "ItemCustomLore .desc", desc);
    }

    private static void staticTooltips(List<String> problems, int[] cases) {
        for (String id : LegacyTooltips.staticIds()) {
            cases[0]++;
            if (!BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(id))) {
                continue; // e.g. item_expensive exists only in expensive mode registrations
            } else if (LegacyTooltips.staticLines(id).isEmpty()) {
                problems.add("E3 tooltips: static tooltip of " + id + " is empty");
            }
        }
    }

    private static boolean missing(String key) {
        return Component.translatable(key).getString().equals(key);
    }

    private static void report(List<String> problems, String what, List<String> entries) {
        if (entries.isEmpty()) {
            return;
        }
        String listed = String.join(", ", entries.subList(0, Math.min(LISTED, entries.size())));
        problems.add("E3 tooltips: " + entries.size() + " items differ from 1.7.10 in " + what + " (port!=original): "
                + listed + (entries.size() > LISTED ? ", ..." : ""));
    }
}
