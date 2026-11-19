package com.atomiclegacy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.atomiclegacy.worldgen.structure.HbmLegacyNbtStructure;
import com.atomiclegacy.worldgen.structure.HbmLegacyScatterStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;

public final class NtmLocateCommand {
    /** [parity S2: commands] CommandLocate.MAX_DISTANCE = 256, searched with {@code radius < MAX_DISTANCE}. */
    private static final int MAX_DISTANCE = 256;

    private NtmLocateCommand() {
    }

    // [parity S2: commands] CommandLocate: permission level 4 (CommandBase default); non-players get PlayerNotFoundException.
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmlocate")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("list")
                        .executes(NtmLocateCommand::list))
                .then(Commands.literal("structure")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(NtmLocateCommand::suggestStructures)
                                .executes(NtmLocateCommand::locate))));
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        context.getSource().getPlayerOrException();
        List<String> names = allStructureNames();
        context.getSource().sendSystemMessage(Component.literal(String.join(", ", names)));
        return names.size();
    }

    private static int locate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        if (!allStructureNames().contains(name)) {
            context.getSource().sendSystemMessage(Component.translatable("commands.locate.no_match").withStyle(ChatFormatting.RED));
            return 0;
        }

        // MathHelper.floor_double(posX) / 16 (integer division, as in the reference command).
        ChunkPos start = new ChunkPos(Mth.floor(player.getX()) / 16, Mth.floor(player.getZ()) / 16);
        ChunkPos found = nearest(context.getSource(), name, start);
        if (found == null) {
            context.getSource().sendSystemMessage(Component.translatable("commands.locate.none_found").withStyle(ChatFormatting.RED));
            return 0;
        }

        context.getSource().sendSystemMessage(Component.translatable("commands.locate.success.coordinates",
                name, found.x * 16, found.z * 16).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    /** CommandLocate#getNearestLocationTo: square rings around the start chunk. */
    private static ChunkPos nearest(CommandSourceStack source, String name, ChunkPos start) {
        if (name.equals(selectedNameAt(source, name, start))) {
            return start;
        }
        for (int radius = 1; radius < MAX_DISTANCE; radius++) {
            for (int x = start.x - radius; x <= start.x + radius; x++) {
                ChunkPos north = new ChunkPos(x, start.z - radius);
                if (name.equals(selectedNameAt(source, name, north))) {
                    return north;
                }
                ChunkPos south = new ChunkPos(x, start.z + radius);
                if (name.equals(selectedNameAt(source, name, south))) {
                    return south;
                }
            }
            for (int z = start.z - radius; z <= start.z + radius; z++) {
                ChunkPos west = new ChunkPos(start.x - radius, z);
                if (name.equals(selectedNameAt(source, name, west))) {
                    return west;
                }
                ChunkPos east = new ChunkPos(start.x + radius, z);
                if (name.equals(selectedNameAt(source, name, east))) {
                    return east;
                }
            }
        }
        return null;
    }

    private static CompletableFuture<Suggestions> suggestStructures(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(allStructureNames(), builder);
    }

    private static List<String> allStructureNames() {
        List<String> names = new ArrayList<>(HbmLegacyNbtStructure.listStructureNames());
        names.addAll(HbmLegacyScatterStructure.listStructureNames());
        return names;
    }

    private static String selectedNameAt(CommandSourceStack source, String name, ChunkPos chunk) {
        String nbt = HbmLegacyNbtStructure.selectedNameAt(source.getLevel(), chunk);
        if (name.equals(nbt)) {
            return nbt;
        }
        return HbmLegacyScatterStructure.selectedNameAt(source.getLevel(), chunk, name);
    }
}
