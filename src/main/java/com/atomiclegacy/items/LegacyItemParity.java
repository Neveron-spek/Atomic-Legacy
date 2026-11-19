package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [parity e3] Item properties of the 1.7.10 ModItems table (setMaxStackSize, setMaxDamage, setContainerItem,
 * getRarity / ItemCustomLore.setRarity, hasEffect). Table: legacy/atomiclegacy/parity-e3-items.tsv, generated from
 * ModItems.java and the item classes by work/parity_e3_tooltips.py.
 *
 * Stack size, rarity and the constant glint are applied to the default components of the item with the same id;
 * durability and container items are only checked (event/ParityE3TooltipDiagnostics).
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyItemParity {
    /** One ModItems registration. Empty string / -1: not a constant in 1.7.10 (tool materials, dynamic getRarity). */
    public record Row(String id, String legacyClass, int stack, int damage, String container, String rarity,
                      String effect, boolean desc, boolean multi) {
        public Rarity expectedRarity() {
            return switch (rarity) {
                case "common" -> Rarity.COMMON;
                case "uncommon" -> Rarity.UNCOMMON;
                case "rare" -> Rarity.RARE;
                case "epic" -> Rarity.EPIC;
                default -> null;
            };
        }
    }

    private static Map<String, Row> table;
    private static int corrected;

    private LegacyItemParity() {
    }

    public static synchronized Map<String, Row> table() {
        if (table == null) {
            Map<String, Row> map = new LinkedHashMap<>();
            try (var input = LegacyItemParity.class.getResourceAsStream("/legacy/atomiclegacy/parity-e3-items.tsv")) {
                if (input == null) {
                    throw new IllegalStateException("missing parity-e3-items.tsv");
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] cell = line.split("\t", -1);
                    if (cell.length < 9) {
                        continue;
                    }
                    map.put(cell[0], new Row(cell[0], cell[1], number(cell[2]), number(cell[3]), cell[4], cell[5],
                            cell[6], "1".equals(cell[7]), "1".equals(cell[8])));
                }
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("cannot read parity-e3-items.tsv", exception);
            }
            table = Collections.unmodifiableMap(map);
        }
        return table;
    }

    /** Number of default components replaced by table values during the last ModifyDefaultComponentsEvent. */
    public static int corrected() {
        return corrected;
    }

    private static int number(String text) {
        return text.isEmpty() ? -1 : Integer.parseInt(text);
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        int changes = 0;
        for (Row row : table().values()) {
            ResourceLocation id = AtomicLegacy.id(row.id());
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            boolean damageable = item.components().has(DataComponents.MAX_DAMAGE);
            int stack = item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
            // a damageable stack cannot be stackable in 1.21.1: such an item keeps its size of 1
            // 64 is the table default when ModItems has no setMaxStackSize; the item class may still limit the stack (getItemStackLimit)
            boolean fixStack = row.stack() > 0 && row.stack() != 64 && row.stack() != stack && !(damageable && row.stack() > 1);
            Rarity rarity = row.expectedRarity();
            boolean fixRarity = rarity != null && item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON) != rarity;
            boolean fixGlint = "1".equals(row.effect())
                    && !Boolean.TRUE.equals(item.components().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
            if (!fixStack && !fixRarity && !fixGlint) {
                continue;
            }
            changes += (fixStack ? 1 : 0) + (fixRarity ? 1 : 0) + (fixGlint ? 1 : 0);
            event.modify(item, builder -> {
                if (fixStack) {
                    builder.set(DataComponents.MAX_STACK_SIZE, row.stack());
                }
                if (fixRarity) {
                    builder.set(DataComponents.RARITY, rarity);
                }
                if (fixGlint) {
                    builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                }
            });
        }
        corrected = changes;
        if (changes > 0) {
            AtomicLegacy.LOGGER.info("[parity e3] {} item default components set from the 1.7.10 ModItems table", changes);
        }
    }
}
