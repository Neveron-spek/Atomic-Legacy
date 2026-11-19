package com.atomiclegacy.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [parity e2: diagnostics] Slot tables of the 1.7.10 containers (tools/e2_gui_extract.py + tools/e2_gui_cases.py ->
 * /legacy/atomiclegacy/parity-e2-gui-slots.json) against the menus the port really builds: every registered menu type
 * is created from a set of probe buffers and every block entity that is a MenuProvider opens its own menu. Each case
 * compares slot count, order, x/y, player-inventory membership and the take-only rule of output slots.
 */
public final class ParityE2Diagnostics {
    private static final BlockPos PROBE = new BlockPos(1048, 250, 1048);

    private ParityE2Diagnostics() {
    }

    private record SlotRow(String type, boolean player, int index, int x, int y, boolean place, boolean pickup) {
    }

    public static int run(MinecraftServer server, List<String> problems) {
        int cases = 0;
        try {
            ServerLevel level = server.overworld();
            FakePlayer player = FakePlayerFactory.getMinecraft(level);
            Inventory inventory = player.getInventory();
            Map<String, List<SlotRow>> layouts = new LinkedHashMap<>();
            Map<String, String> owners = new LinkedHashMap<>();

            for (Map.Entry<net.minecraft.resources.ResourceKey<MenuType<?>>, MenuType<?>> entry : BuiltInRegistries.MENU.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) {
                    continue;
                }
                Set<String> seen = new HashSet<>();
                for (String label : probeLabels()) {
                    AbstractContainerMenu menu;
                    try {
                        menu = entry.getValue().create(1, inventory, probe(level, label));
                    } catch (Throwable ignored) {
                        continue;
                    }
                    List<SlotRow> rows = rows(menu, player);
                    if (seen.add(signature(rows))) {
                        String key = "type:" + id.getPath() + "@" + label;
                        layouts.put(key, rows);
                        owners.put(key, id.getPath() + "\t" + menu.getClass().getSimpleName());
                    }
                }
            }
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (!AtomicLegacy.MOD_ID.equals(id.getNamespace()) || !(block instanceof EntityBlock entityBlock)) {
                    continue;
                }
                try {
                    BlockEntity blockEntity = entityBlock.newBlockEntity(PROBE, block.defaultBlockState());
                    if (!(blockEntity instanceof MenuProvider provider)) {
                        continue;
                    }
                    blockEntity.setLevel(level);
                    AbstractContainerMenu menu = provider.createMenu(1, inventory, player);
                    if (menu == null) {
                        continue;
                    }
                    String key = "block:" + id.getPath();
                    layouts.put(key, rows(menu, player));
                    ResourceLocation typeId = BuiltInRegistries.MENU.getKey(menu.getType());
                    owners.put(key, (typeId == null ? "?" : typeId.getPath()) + "\t" + menu.getClass().getSimpleName());
                } catch (Throwable ignored) {
                    // block entities that need a placed structure are covered by the menu type probes
                }
            }

            List<String> dump = new ArrayList<>();
            for (Map.Entry<String, List<SlotRow>> entry : layouts.entrySet()) {
                StringBuilder line = new StringBuilder(entry.getKey()).append('\t').append(owners.get(entry.getKey())).append('\t');
                for (SlotRow row : entry.getValue()) {
                    line.append(row.type()).append(',').append(row.player() ? 1 : 0).append(',').append(row.index()).append(',')
                            .append(row.x()).append(',').append(row.y()).append(',').append(row.place() ? 1 : 0).append(',')
                            .append(row.pickup() ? 1 : 0).append(';');
                }
                dump.add(line.toString());
            }
            Files.write(Path.of("parity-e2-menus.tsv"), dump, StandardCharsets.UTF_8);

            JsonObject root;
            try (var input = ParityE2Diagnostics.class.getResourceAsStream("/legacy/atomiclegacy/parity-e2-gui-slots.json")) {
                if (input == null) {
                    problems.add("parity e2: slot table resource is missing");
                    return 0;
                }
                root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            for (JsonElement element : root.getAsJsonArray("cases")) {
                JsonObject testCase = element.getAsJsonObject();
                String key = testCase.get("key").getAsString();
                String container = testCase.get("container").getAsString();
                cases++;
                List<SlotRow> rows = layouts.get(key);
                if (rows == null) {
                    problems.add("parity e2: " + container + " has no port menu layout " + key);
                    continue;
                }
                JsonArray slots = testCase.getAsJsonArray("slots");
                if (slots.size() != rows.size()) {
                    problems.add("parity e2: " + container + " slot count " + slots.size() + " != " + rows.size() + " in " + key);
                    continue;
                }
                for (int i = 0; i < rows.size(); i++) {
                    JsonArray expected = slots.get(i).getAsJsonArray();
                    SlotRow row = rows.get(i);
                    String type = expected.get(0).getAsString();
                    boolean expectedPlayer = expected.get(1).getAsInt() == 1;
                    int x = expected.get(3).getAsInt();
                    int y = expected.get(4).getAsInt();
                    if (row.x() != x || row.y() != y || row.player() != expectedPlayer) {
                        problems.add("parity e2: " + container + " slot " + i + " expected " + (expectedPlayer ? "player " : "") + x + "," + y
                                + " but " + key + " has " + (row.player() ? "player " : "") + row.x() + "," + row.y());
                        break;
                    }
                    if (expectedPlayer && row.index() != expected.get(2).getAsInt()) {
                        problems.add("parity e2: " + container + " slot " + i + " player index " + expected.get(2).getAsInt() + " != " + row.index());
                        break;
                    }
                    boolean takeOnly = type.equals("SlotTakeOnly") || type.equals("SlotCraftingOutput") || type.equals("SlotSmelting")
                            || type.equals("SlotDeprecated");
                    if (takeOnly && row.place()) {
                        problems.add("parity e2: " + container + " slot " + i + " (" + type + ") accepts items in " + key);
                        break;
                    }
                }
            }
        } catch (Throwable throwable) {
            problems.add("parity e2 diagnostics crashed: " + throwable);
        }
        return cases;
    }

    private static List<String> probeLabels() {
        List<String> labels = new ArrayList<>();
        labels.add("pos");
        for (int k = 1; k <= 12; k++) {
            labels.add("pos+" + k);
        }
        for (int k = 0; k <= 12; k++) {
            labels.add("b" + k);
        }
        for (int k = 0; k <= 12; k++) {
            labels.add("h" + k);
        }
        return labels;
    }

    private static RegistryFriendlyByteBuf probe(ServerLevel level, String label) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        if (label.startsWith("pos")) {
            buffer.writeBlockPos(PROBE);
            buffer.writeByte(label.length() > 3 ? Integer.parseInt(label.substring(4)) : 0);
        } else if (label.startsWith("b")) {
            buffer.writeByte(Integer.parseInt(label.substring(1)));
        } else {
            buffer.writeByte(0);
            buffer.writeByte(Integer.parseInt(label.substring(1)));
        }
        buffer.writeZero(64);
        return buffer;
    }

    private static List<SlotRow> rows(AbstractContainerMenu menu, FakePlayer player) {
        List<SlotRow> rows = new ArrayList<>();
        ItemStack probe = new ItemStack(Items.STONE);
        for (Slot slot : menu.slots) {
            boolean place;
            boolean pickup;
            try {
                place = slot.mayPlace(probe);
            } catch (Throwable ignored) {
                place = false;
            }
            try {
                pickup = slot.mayPickup(player);
            } catch (Throwable ignored) {
                pickup = true;
            }
            rows.add(new SlotRow(slot.getClass().getSimpleName(), slot.container instanceof Inventory, slot.getContainerSlot(), slot.x, slot.y,
                    place, pickup));
        }
        return rows;
    }

    private static String signature(List<SlotRow> rows) {
        StringBuilder builder = new StringBuilder();
        for (SlotRow row : rows) {
            builder.append(row.player() ? 'p' : 'm').append(row.x()).append(',').append(row.y()).append(';');
        }
        return builder.toString();
    }
}
