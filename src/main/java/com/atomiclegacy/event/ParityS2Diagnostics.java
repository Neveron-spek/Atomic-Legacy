package com.atomiclegacy.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.command.ParityS2Commands;
import com.atomiclegacy.command.ParityS2RunningConfig;
import com.atomiclegacy.config.HbmServerParityConfig;
import com.atomiclegacy.radiation.ChunkRadiationData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** [parity S2: diagnostics] Commands, achievements and config keys/defaults against the 1.7.10 tables. */
final class ParityS2Diagnostics {
    private ParityS2Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            commands(server, problems, cases);
        } catch (Throwable t) {
            problems.add("parity S2 commands threw " + t);
        }
        try {
            advancements(server, problems, cases);
        } catch (Throwable t) {
            problems.add("parity S2 advancements threw " + t);
        }
        try {
            configs(problems, cases);
        } catch (Throwable t) {
            problems.add("parity S2 configs threw " + t);
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("parity S2: " + what);
    }

    /** Runs a command from the console; returns the result, -1 for a syntax/permission error, or records a crash. */
    private static int execute(MinecraftServer server, String command, List<String> problems) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        try {
            return server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException ex) {
            return -1;
        } catch (Throwable t) {
            problems.add("parity S2: /" + command + " threw " + t);
            return -2;
        }
    }

    private static void commands(MinecraftServer server, List<String> problems, int[] cases) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        List<String> all = new ArrayList<>(List.of(ParityS2Commands.COMMANDS));
        all.add("ntmlocate");
        all.add("ntmsatellites");
        for (String name : all) {
            check(dispatcher.getRoot().getChild(name) != null, problems, cases, "command /" + name + " registered");
        }
        // Console runs: original texts/flows, no exceptions besides "player required".
        check(execute(server, "ntmcustomize", problems) == 0, problems, cases, "/ntmcustomize from console refuses (players only)");
        check(execute(server, "ntmrad", problems) == 0, problems, cases, "/ntmrad prints usage");
        check(execute(server, "ntmrad set 100001", problems) == -1, problems, cases, "/ntmrad set is bounded to 100000");
        BlockPos spawn = BlockPos.containing(server.createCommandSourceStack().getPosition());
        ChunkRadiationData rad = ChunkRadiationData.get(server.overworld());
        double before = rad.getRadiation(spawn);
        boolean loaded = server.overworld().getChunkSource().getChunkNow(spawn.getX() >> 4, spawn.getZ() >> 4) != null;
        check(execute(server, "ntmrad set 5", problems) == 1, problems, cases, "/ntmrad set 5 runs");
        if (loaded) check(Math.abs(rad.getRadiation(spawn) - 5.0D) < 1e-4, problems, cases, "/ntmrad set stores 5 at the sender chunk");
        rad.setRadiation(spawn, before);
        check(execute(server, "ntmloadchunk", problems) == 0, problems, cases, "/ntmloadchunk without 2 args returns silently");
        check(execute(server, "ntmloadchunk 29999984 29999984", problems) == 1, problems, cases, "/ntmloadchunk on an absent chunk reports without crashing");
        check(execute(server, "ntmlocate list", problems) == -1, problems, cases, "/ntmlocate from console needs a player");
        check(execute(server, "ntmsatellites list", problems) == 0, problems, cases, "/ntmsatellites from console refuses");
        check(execute(server, "ntmserver list", problems) == -1, problems, cases, "/ntmserver is players only");
        check(execute(server, "ntmreapnetworks", problems) == 1, problems, cases, "/ntmreapnetworks runs");

        // CommandReloadConfig flow on the server map.
        ParityS2RunningConfig cfg = ParityS2RunningConfig.server();
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        List<String> out = new ArrayList<>();
        try {
            cfg.process(source, "list", c -> out.add(c.getString()));
            check(out.size() == cfg.configMap().size() + 1 && out.get(0).equals("§cSERVER VARIABLES:"), problems, cases,
                    "/ntmserver list prints title + every key (" + out.size() + ")");
            out.clear();
            cfg.process(source, "get MINE_AP_DAMAGE", c -> out.add(c.getString()));
            check(out.equals(List.of("§6MINE_AP_DAMAGE: §e10.0")), problems, cases, "/ntmserver get formats Float like ConfigWrapper " + out);
            out.clear();
            cfg.process(source, "get GUN_DOUBLE_BARREL_DAMAGE", c -> out.add(c.getString()));
            check(out.equals(List.of("§6GUN_DOUBLE_BARREL_DAMAGE: §e10.0")), problems, cases,
                    "GUN_DOUBLE_BARREL_DAMAGE key is bound to the drill (XFactoryDrill overwrites the 10ga entry) " + out);
            out.clear();
            cfg.process(source, "help get", c -> out.add(c.getString()));
            check(out.equals(List.of("§eShows value for the specified variable name.")), problems, cases, "/ntmserver help get " + out);
        } catch (CommandSyntaxException ex) {
            problems.add("parity S2: /ntmserver flow threw " + ex.getMessage());
        }
        check(syntaxError(cfg, source, "get NOPE").equals("Key does not exist."), problems, cases, "/ntmserver get unknown key");
        check(syntaxError(cfg, source, "set MINE_AP_DAMAGE abc").equals("Error parsing type for Float: For input string: \"abc\""),
                problems, cases, "/ntmserver set parse error text " + syntaxError(cfg, source, "set MINE_AP_DAMAGE abc"));
        check(syntaxError(cfg, source, "").equals("/ntmserver help"), problems, cases, "/ntmserver without args throws usage");
        check(syntaxError(cfg, source, "set ENABLE_MKU").equals("/ntmserver help"), problems, cases, "/ntmserver set without value throws usage");
        try {
            ParityS2RunningConfig.Wrapper wrapper = cfg.configMap().get("CONVEYOR_CRAM_MAX");
            wrapper.update("26");
            check(Integer.valueOf(26).equals(wrapper.get()), problems, cases, "ConfigWrapper#update writes the NeoForge value");
            wrapper.update("25");
        } catch (Throwable t) {
            problems.add("parity S2: running config update threw " + t);
        }
    }

    private static String syntaxError(ParityS2RunningConfig cfg, CommandSourceStack source, String args) {
        try {
            cfg.process(source, args, c -> {
            });
            return "<no error>";
        } catch (CommandSyntaxException ex) {
            return ex.getRawMessage().getString();
        }
    }

    private static void advancements(MinecraftServer server, List<String> problems, int[] cases) {
        int own = 0;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (holder.id().getNamespace().equals(AtomicLegacy.MOD_ID)) own++;
        }
        check(own == ParityS2AdvancementTable.ENTRIES.length + 1, problems, cases,
                "advancement count = 61 achievements + technical root (" + own + ")");
        check(server.getAdvancements().get(AtomicLegacy.id("foeq")) == null, problems, cases, "no FOEQ advancement (not registered in 1.7.10)");
        AdvancementHolder root = server.getAdvancements().get(AtomicLegacy.id("root"));
        check(root != null && root.value().parent().isEmpty(), problems, cases, "technical root present");
        for (ParityS2AdvancementTable.Entry entry : ParityS2AdvancementTable.ENTRIES) {
            AdvancementHolder holder = server.getAdvancements().get(AtomicLegacy.id(entry.id()));
            check(holder != null, problems, cases, "advancement " + entry.id() + " loaded");
            if (holder == null) continue;
            Optional<ResourceLocation> parent = holder.value().parent();
            check(parent.isPresent() && parent.get().equals(AtomicLegacy.id(entry.parent())), problems, cases,
                    entry.id() + " parent " + entry.parent() + " (" + parent.orElse(null) + ")");
            Optional<DisplayInfo> display = holder.value().display();
            check(display.isPresent(), problems, cases, entry.id() + " has display");
            if (display.isEmpty()) continue;
            ItemStack icon = display.get().getIcon();
            check(BuiltInRegistries.ITEM.getKey(icon.getItem()).toString().equals(entry.icon()), problems, cases,
                    entry.id() + " icon " + entry.icon() + " (" + BuiltInRegistries.ITEM.getKey(icon.getItem()) + ")");
            if (!entry.fluid().isEmpty()) {
                String fluid = icon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("fluid");
                check(entry.fluid().equals(fluid), problems, cases, entry.id() + " icon fluid " + entry.fluid() + " (" + fluid + ")");
            }
            AdvancementType expected = entry.frame().equals("challenge") ? AdvancementType.CHALLENGE : AdvancementType.TASK;
            check(display.get().getType() == expected, problems, cases, entry.id() + " frame " + entry.frame());
            Component title = display.get().getTitle();
            check(title.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                            && t.getKey().equals("advancements.atomiclegacy." + entry.id() + ".title"), problems, cases,
                    entry.id() + " title key");
        }
    }

    private static void configs(List<String> problems, int[] cases) {
        for (ParityS2ConfigTable.Entry entry : ParityS2ConfigTable.FORGE) {
            Object def;
            try {
                def = defaultOf(entry.owner(), entry.field());
            } catch (Throwable t) {
                problems.add("parity S2: config " + entry.source() + "/" + entry.key() + " unbound " + entry.owner() + "." + entry.field());
                cases[0]++;
                continue;
            }
            check(same(entry.kind(), entry.value(), def, entry.inverted()), problems, cases,
                    "config default " + entry.source() + "/" + entry.key() + " = " + entry.value() + (entry.inverted() ? " (inverted)" : "")
                            + " but " + entry.owner() + "." + entry.field() + " = " + def);
        }
        for (ParityS2ConfigTable.Running[] list : new ParityS2ConfigTable.Running[][]{ParityS2ConfigTable.SERVER, ParityS2ConfigTable.CLIENT}) {
            for (ParityS2ConfigTable.Running entry : list) {
                Object def;
                try {
                    def = defaultOf(entry.owner(), entry.field());
                } catch (Throwable t) {
                    problems.add("parity S2: running config " + entry.key() + " unbound " + entry.owner() + "." + entry.field());
                    cases[0]++;
                    continue;
                }
                char kind = entry.type().equals("Boolean") ? 'B' : entry.type().equals("String") ? 'S' : 'N';
                check(same(kind, entry.value(), def, false), problems, cases,
                        "running config default " + entry.key() + " = " + entry.value() + " but " + entry.owner() + "." + entry.field() + " = " + def);
            }
        }
        check(ParityS2RunningConfig.server().configMap().size() == ParityS2ConfigTable.SERVER.length, problems, cases, "server running map size");
        check(HbmServerParityConfig.crateOpenHeld() && HbmServerParityConfig.crateKeepContents() && HbmServerParityConfig.enableMku()
                && HbmServerParityConfig.itemHazardDropTickrate() == 2 && HbmServerParityConfig.maxBufferLength() == 256, problems, cases,
                "ServerConfig defaults active (CRATE_OPEN_HELD, CRATE_KEEP_CONTENTS, ENABLE_MKU, ITEM_HAZARD_DROP_TICKRATE, MAX_BUFFER_LENGTH)");
        com.atomiclegacy.autocal.AutocalScript.Context context = new com.atomiclegacy.autocal.AutocalScript.Context(null);
        boolean fits = context.writeBuffer("x".repeat(300));
        check(!fits && context.buffer.length() == 256, problems, cases, "AUTOCAL buffer truncated to MAX_BUFFER_LENGTH 256");
    }

    private static Object defaultOf(String owner, String field) throws ReflectiveOperationException {
        Class<?> type = Class.forName("com.atomiclegacy.config." + owner);
        return ((ModConfigSpec.ConfigValue<?>) type.getField(field).get(null)).getDefault();
    }

    private static boolean same(char kind, String value, Object def, boolean inverted) {
        return switch (kind) {
            case 'B' -> def instanceof Boolean b && (Boolean.parseBoolean(value) ^ inverted) == b;
            case 'S' -> value.equals(String.valueOf(def));
            default -> def instanceof Number n && Math.abs(n.doubleValue() - Double.parseDouble(value))
                    <= 1e-6 * Math.max(1.0D, Math.abs(Double.parseDouble(value)));
        };
    }
}
