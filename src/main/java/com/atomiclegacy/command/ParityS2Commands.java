package com.atomiclegacy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.items.ICustomizable;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.radiation.ChunkRadiationData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * [parity S2: commands] Server commands of 1.7.10 MainRegistry#serverStart that the port was missing:
 * ntmcustomize, ntmrad, ntmreapnetworks, ntmreload, ntmserver and ntmloadchunk (CommandCustomize, CommandRadiation,
 * CommandReapNetworks, CommandReloadRecipes, CommandReloadServer, CommandDebugChunkLoad). Permission levels follow
 * CommandBase (default 4) and the overridden canCommandSenderUseCommand methods; chat texts are the original literals.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class ParityS2Commands {
    public static final String[] COMMANDS = {"ntmcustomize", "ntmrad", "ntmreapnetworks", "ntmreload", "ntmserver", "ntmloadchunk"};

    private ParityS2Commands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // CommandCustomize: permission 0, canCommandSenderUseCommand -> true.
        dispatcher.register(Commands.literal("ntmcustomize")
                .executes(context -> customize(context, ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(context -> customize(context, StringArgumentType.getString(context, "args")))));
        // CommandRadiation: CommandBase default level 4.
        dispatcher.register(Commands.literal("ntmrad")
                .requires(source -> source.hasPermission(4))
                .executes(ParityS2Commands::radiationUsage)
                .then(Commands.literal("clear").executes(ParityS2Commands::radiationClear))
                .then(Commands.literal("set")
                        .executes(ParityS2Commands::radiationUsage)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 100_000.0D))
                                .executes(ParityS2Commands::radiationSet))));
        dispatcher.register(Commands.literal("ntmreapnetworks")
                .requires(source -> source.hasPermission(4))
                .executes(ParityS2Commands::reapNetworks));
        dispatcher.register(Commands.literal("ntmreload")
                .requires(source -> source.hasPermission(4))
                .executes(ParityS2Commands::reloadRecipes));
        // CommandReloadConfig#canCommandSenderUseCommand: any player, no operator level.
        dispatcher.register(Commands.literal("ntmserver")
                .requires(source -> source.getEntity() instanceof Player)
                .executes(context -> runningConfig(context, ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((context, builder) -> ParityS2RunningConfig.server().suggest(builder))
                        .executes(context -> runningConfig(context, StringArgumentType.getString(context, "args")))));
        dispatcher.register(Commands.literal("ntmloadchunk")
                .requires(source -> source.hasPermission(4))
                .executes(context -> 0) // args.length != 2: returns silently
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .executes(context -> 0)
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ParityS2Commands::loadChunk))));
    }

    private static void chat(CommandSourceStack source, String text) {
        source.sendSystemMessage(Component.literal(text));
    }

    /** CommandCustomize#processCommand. */
    private static int customize(CommandContext<CommandSourceStack> context, String raw) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendSystemMessage(Component.literal("Customization is only available to players!").withStyle(ChatFormatting.RED));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof ICustomizable item)) {
            source.sendSystemMessage(Component.literal("You have to hold a customizable item to use this command!").withStyle(ChatFormatting.RED));
            return 0;
        }
        item.customize(player, held, raw.isEmpty() ? new String[0] : raw.split(" "));
        return 1;
    }

    private static int radiationUsage(CommandContext<CommandSourceStack> context) {
        chat(context.getSource(), ChatFormatting.RED + "/ntmrad <set/clear>");
        return 0;
    }

    private static int radiationClear(CommandContext<CommandSourceStack> context) {
        ChunkRadiationData.get(context.getSource().getLevel()).clearSystem();
        chat(context.getSource(), "Cleared radiation data!");
        return 1;
    }

    private static int radiationSet(CommandContext<CommandSourceStack> context) {
        float amount = (float) DoubleArgumentType.getDouble(context, "amount");
        BlockPos pos = BlockPos.containing(context.getSource().getPosition());
        ChunkRadiationData.get(context.getSource().getLevel()).setRadiation(pos, amount);
        chat(context.getSource(), "Radiation set.");
        return 1;
    }

    /** Shared error block of CommandReapNetworks / CommandReloadRecipes. */
    private static void reportError(CommandSourceStack source, Throwable ex, String line) {
        source.sendSystemMessage(Component.literal("----------------------------------").withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.RED));
        source.sendSystemMessage(Component.literal(String.valueOf(ex.getLocalizedMessage())).withStyle(ChatFormatting.RED));
        StackTraceElement[] trace = ex.getStackTrace();
        source.sendSystemMessage(Component.literal(trace.length > 0 ? trace[0].toString() : "").withStyle(ChatFormatting.RED));
        source.sendSystemMessage(Component.literal("----------------------------------").withStyle(ChatFormatting.GRAY));
    }

    /** CommandReapNetworks: the port's power, fluid and pneumatic nets stand in for UniNodespace. */
    private static int reapNetworks(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            MinecraftServer server = source.getServer();
            PowerNetworkManager.reapAll(server);
            HbmFluidNetworks.reapAll(server);
            PneumaticNetworkManager.reapAll(server);
            chat(source, ChatFormatting.YELLOW + "Nodespace cleared :)");
            return 1;
        } catch (RuntimeException ex) {
            reportError(source, ex, "An error has occoured during network reap, consult the log for details.");
            throw ex;
        }
    }

    /**
     * CommandReloadRecipes: the port's recipes, loot pools and tags are data packs, so the equivalent of
     * re-running SerializableRecipe/ItemPoolConfigJSON is a data pack reload of the selected packs.
     */
    private static int reloadRecipes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((ignored, ex) -> server.execute(() -> {
            Throwable failure = ex;
            if (failure == null) {
                try {
                    com.atomiclegacy.inventory.recipes.CustomMachineRecipes.initialize(); // [parity X1: custom machines] SerializableRecipe.initialize()
                } catch (RuntimeException recipeFailure) {
                    failure = recipeFailure;
                }
            }
            if (failure == null) {
                chat(source, ChatFormatting.YELLOW + "Reload complete :)");
            } else {
                Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
                AtomicLegacy.LOGGER.error("ntmreload failed", cause);
                reportError(source, cause, "An error has occoured during loading, consult the log for details.");
            }
        }));
        return 1;
    }

    private static int runningConfig(CommandContext<CommandSourceStack> context, String raw) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        return ParityS2RunningConfig.server().process(source, raw, source::sendSystemMessage);
    }

    /** CommandDebugChunkLoad: lists the block entities stored in an unloaded chunk. */
    private static int loadChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int x = IntegerArgumentType.getInteger(context, "x");
        int z = IntegerArgumentType.getInteger(context, "z");
        int cX = x >> 4;
        int cZ = z >> 4;
        try {
            if (level.hasChunk(cX, cZ)) {
                chat(source, ChatFormatting.RED + "Chunk currently loaded.");
                return 0;
            }
            Optional<CompoundTag> data = level.getChunkSource().chunkMap.read(new ChunkPos(cX, cZ)).join();
            // AnvilChunkLoader returns null for a missing chunk; the reference command then reports the NPE message.
            CompoundTag nbt = data.orElseThrow(NullPointerException::new);
            ListTag tagList = nbt.getList("block_entities", Tag.TAG_COMPOUND);
            if (tagList.isEmpty()) {
                chat(source, ChatFormatting.RED + "Tag list empty");
            }
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight() - 1;
            for (int i1 = 0; i1 < tagList.size(); ++i1) {
                CompoundTag tileCompound = tagList.getCompound(i1);
                int i = tileCompound.getInt("x") - cX * 16;
                int j = tileCompound.getInt("y");
                int k = tileCompound.getInt("z") - cZ * 16;
                String name = tileCompound.getString("id");
                ChatFormatting color = ChatFormatting.GREEN;
                if (i < 0 || i > 15 || j < minY || j > maxY || k < 0 || k > 15) {
                    color = ChatFormatting.RED;
                }
                chat(source, color + name + " " + i + " " + j + " " + k);
            }
        } catch (Exception e) {
            Throwable cause = e instanceof java.util.concurrent.CompletionException && e.getCause() != null ? e.getCause() : e;
            chat(source, ChatFormatting.RED + "" + cause.getLocalizedMessage());
        }
        return 1;
    }
}
