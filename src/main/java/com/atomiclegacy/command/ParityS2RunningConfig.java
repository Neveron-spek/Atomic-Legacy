package com.atomiclegacy.command;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.event.ParityS2ConfigTable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * [parity S2: commands] 1.7.10 {@code RunningConfig}/{@code CommandReloadConfig} on top of the port's NeoForge
 * config values: {@code ConfigWrapper} entries keep their original key, value type and text formatting, but read and
 * write the bound {@link ModConfigSpec} value; "refresh" saves the TOML file and "reload" re-reads it.
 */
public final class ParityS2RunningConfig {
    /** Same container type as ServerConfig.configMap / ClientConfig.configMap (iteration order included). */
    private final HashMap<String, Wrapper> configMap = new HashMap<>();
    private final String command;
    private final String side;
    private final String title;
    private final String fileName;
    private final ModConfigSpec spec;

    private static ParityS2RunningConfig server;
    private static ParityS2RunningConfig client;

    private ParityS2RunningConfig(String command, String side, String title, String fileName, ModConfigSpec spec,
                                  ParityS2ConfigTable.Running[] entries) {
        this.command = command;
        this.side = side;
        this.title = title;
        this.fileName = fileName;
        this.spec = spec;
        for (ParityS2ConfigTable.Running entry : entries) {
            configMap.put(entry.key(), new Wrapper(entry.type(), bound(entry.owner(), entry.field())));
        }
    }

    public static synchronized ParityS2RunningConfig server() {
        if (server == null) {
            server = new ParityS2RunningConfig("ntmserver", "server", "SERVER VARIABLES:", AtomicLegacy.MOD_ID + "-common.toml",
                    com.atomiclegacy.config.HbmConfig.SPEC, ParityS2ConfigTable.SERVER);
        }
        return server;
    }

    public static synchronized ParityS2RunningConfig client() {
        if (client == null) {
            client = new ParityS2RunningConfig("ntmclient", "client", "CLIENT VARIABLES:", AtomicLegacy.MOD_ID + "-client.toml",
                    com.atomiclegacy.config.HbmClientConfig.SPEC, ParityS2ConfigTable.CLIENT);
        }
        return client;
    }

    public Map<String, Wrapper> configMap() {
        return configMap;
    }

    static ModConfigSpec.ConfigValue<?> bound(String owner, String field) {
        try {
            Class<?> type = Class.forName("com.atomiclegacy.config." + owner);
            return (ModConfigSpec.ConfigValue<?>) type.getField(field).get(null);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            throw new IllegalStateException("Unbound running config entry " + owner + "." + field, ex);
        }
    }

    /** RunningConfig.ConfigWrapper with the value stored in a NeoForge config value. */
    public static final class Wrapper {
        private final String type;
        private final ModConfigSpec.ConfigValue<?> value;

        Wrapper(String type, ModConfigSpec.ConfigValue<?> value) {
            this.type = type;
            this.value = value;
        }

        public String type() {
            return type;
        }

        public ModConfigSpec.ConfigValue<?> value() {
            return value;
        }

        /** The ConfigWrapper value as the original Java type, so toString() matches the chat output. */
        public Object get() {
            Object raw = value.get();
            return switch (type) {
                case "Float" -> ((Number) raw).floatValue();
                case "Double" -> ((Number) raw).doubleValue();
                case "Integer" -> ((Number) raw).intValue();
                case "Boolean" -> (Boolean) raw;
                default -> String.valueOf(raw);
            };
        }

        /** ConfigWrapper#update(String). */
        @SuppressWarnings("unchecked")
        public void update(String param) {
            switch (type) {
                case "Float" -> setNumber(Float.parseFloat(param));
                case "Double" -> setNumber(Double.parseDouble(param));
                case "Integer" -> setNumber(Integer.parseInt(param));
                case "Boolean" -> ((ModConfigSpec.ConfigValue<Boolean>) value).set(Boolean.parseBoolean(param));
                default -> ((ModConfigSpec.ConfigValue<String>) value).set(param);
            }
        }

        @SuppressWarnings("unchecked")
        private void setNumber(Number number) {
            if (value instanceof ModConfigSpec.DoubleValue d) d.set(number.doubleValue());
            else if (value instanceof ModConfigSpec.IntValue i) i.set(number.intValue());
            else if (value instanceof ModConfigSpec.LongValue l) l.set(number.longValue());
            else ((ModConfigSpec.ConfigValue<Object>) value).set(number);
        }

        void readRaw(Object raw) {
            if (raw == null) return;
            update(String.valueOf(raw));
        }
    }

    /** ServerConfig.refresh / ClientConfig.refresh: writes the running values to the config file. */
    public void refresh() {
        spec.save();
    }

    /** ServerConfig.reload / ClientConfig.reload: overwrites the running values from the config file. */
    public void reload() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(fileName);
        if (!Files.exists(path)) return;
        try (CommentedFileConfig file = CommentedFileConfig.of(path)) {
            file.load();
            for (Wrapper wrapper : configMap.values()) {
                List<String> key = wrapper.value.getPath();
                try {
                    wrapper.readRaw(file.get(key));
                } catch (Exception ex) {
                    AtomicLegacy.LOGGER.error("Could not read {} from {}", String.join(".", key), path, ex);
                }
            }
        }
    }

    private static CommandSyntaxException error(String text) {
        return new SimpleCommandExceptionType(Component.literal(text)).create();
    }

    private String usage() {
        return "/" + command + " help";
    }

    /** CommandReloadConfig#processCommand with the original whitespace-split argument array. */
    public int process(CommandSourceStack source, String raw, Consumer<Component> chat) throws CommandSyntaxException {
        String[] args = raw.isEmpty() ? new String[0] : raw.split(" ");
        if (args.length < 1) throw error(usage());
        String operator = args[0];
        if ("help".equals(operator)) {
            help(args, chat);
            return 1;
        }
        if ("list".equals(operator)) {
            chat.accept(Component.literal(ChatFormatting.RED + title));
            for (Map.Entry<String, Wrapper> line : configMap.entrySet()) {
                chat.accept(Component.literal("  " + ChatFormatting.GOLD + line.getKey() + ": " + ChatFormatting.YELLOW + line.getValue().get()));
            }
            return 1;
        }
        if ("reload".equals(operator)) {
            reload();
            chat.accept(Component.literal(ChatFormatting.YELLOW + "Variables loaded from config file."));
            return 1;
        }
        if (args.length < 2) throw error(usage());
        String key = args[1];
        if ("get".equals(operator)) {
            Wrapper wrapper = configMap.get(key);
            if (wrapper == null) throw error("Key does not exist.");
            chat.accept(Component.literal(ChatFormatting.GOLD + key + ": " + ChatFormatting.YELLOW + wrapper.get()));
            return 1;
        }
        if (args.length < 3) throw error(usage());
        String value = args[2];
        if ("set".equals(operator)) {
            Wrapper wrapper = configMap.get(key);
            if (wrapper == null) throw error("Key does not exist.");
            try {
                wrapper.update(value);
                refresh();
                chat.accept(Component.literal(ChatFormatting.YELLOW + "Value updated."));
            } catch (Exception ex) {
                throw error("Error parsing type for " + wrapper.get().getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
            }
            return 1;
        }
        throw error(usage());
    }

    /** CommandReloadServer#help / CommandReloadClient#help. */
    private void help(String[] args, Consumer<Component> chat) {
        String y = ChatFormatting.YELLOW.toString();
        if (args.length >= 2) {
            String command = args[1];
            if ("help".equals(command)) chat.accept(Component.literal(y + "Shows usage for /" + this.command + " subcommands."));
            if ("list".equals(command)) chat.accept(Component.literal(y + "Shows all " + side + " variable names and values."));
            if ("reload".equals(command)) chat.accept(Component.literal(y + "Reads " + side + " variables from the config file."));
            if ("get".equals(command)) chat.accept(Component.literal(y + "Shows value for the specified variable name."));
            if ("set".equals(command)) chat.accept(Component.literal(y + "Sets a variable's value and saves it to the config file."));
        } else {
            String g = ChatFormatting.GOLD.toString();
            String r = ChatFormatting.RED.toString();
            chat.accept(Component.literal(y + "/" + this.command + " " + g + "help " + r + "<command>"));
            chat.accept(Component.literal(y + "/" + this.command + " " + g + "list"));
            chat.accept(Component.literal(y + "/" + this.command + " " + g + "reload"));
            chat.accept(Component.literal(y + "/" + this.command + " " + g + "get " + r + "<name>"));
            chat.accept(Component.literal(y + "/" + this.command + " " + g + "set " + r + "<name> <value>"));
        }
    }

    /** CommandReloadConfig#addTabCompletionOptions (no "help" suggestion, as in the original). */
    public CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String[] parts = remaining.split(" ", -1);
        if (parts.length == 1) {
            return SharedSuggestionProvider.suggest(List.of("list", "reload", "get", "set"), builder);
        }
        if (parts.length == 2 && ("get".equals(parts[0]) || "set".equals(parts[0]))) {
            com.mojang.brigadier.suggestion.SuggestionsBuilder sub = builder.createOffset(builder.getStart() + parts[0].length() + 1);
            return SharedSuggestionProvider.suggest(configMap.keySet(), sub);
        }
        return builder.buildFuture();
    }
}
