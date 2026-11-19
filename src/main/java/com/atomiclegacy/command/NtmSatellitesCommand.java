package com.atomiclegacy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.SatelliteChipItem;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.satellite.SatelliteSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * [parity P2: /ntmsatellites] Port of CommandSatellites: orbit (held ISatChip
 * other than sat_chip, launched at the player's position, stack shrinks by
 * one), descend &lt;frequency&gt; and list.  CommandBase default permission
 * level 4; non-players get the red "should be run as player" message.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class NtmSatellitesCommand {
    private NtmSatellitesCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmsatellites")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("orbit").executes(NtmSatellitesCommand::orbit))
                .then(Commands.literal("descend")
                        .then(Commands.argument("frequency", IntegerArgumentType.integer())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        SatelliteSavedData.get(context.getSource().getLevel()).satellites().keySet()
                                                .stream().map(String::valueOf), builder))
                                .executes(NtmSatellitesCommand::descend)))
                .then(Commands.literal("list").executes(NtmSatellitesCommand::list)));
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) return player;
        context.getSource().sendSystemMessage(Component.translatable("commands.satellite.should_be_run_as_player")
                .withStyle(ChatFormatting.RED));
        return null;
    }

    private static int orbit(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof SatelliteChipItem && !held.is(HbmItems.SAT_CHIP.get())) {
            ServerLevel level = player.serverLevel();
            SatelliteSavedData.get(level).orbit(level, held, player.getX(), player.getZ());
            held.shrink(1);
            context.getSource().sendSystemMessage(Component.translatable("commands.satellite.satellite_orbited")
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        }
        context.getSource().sendSystemMessage(Component.translatable("commands.satellite.not_a_satellite")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int descend(CommandContext<CommandSourceStack> context) {
        if (player(context) == null) return 0;
        int frequency = IntegerArgumentType.getInteger(context, "frequency");
        if (SatelliteSavedData.get(context.getSource().getLevel()).descend(frequency)) {
            context.getSource().sendSystemMessage(Component.translatable("commands.satellite.satellite_descended")
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        }
        context.getSource().sendSystemMessage(Component.translatable("commands.satellite.no_satellite")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        if (player(context) == null) return 0;
        var satellites = SatelliteSavedData.get(context.getSource().getLevel()).satellites();
        if (satellites.isEmpty()) {
            context.getSource().sendSystemMessage(Component.translatable("commands.satellite.no_active_satellites")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        satellites.forEach((frequency, satellite) -> context.getSource().sendSystemMessage(
                Component.literal(frequency + " - " + satellite.kind().className()).withStyle(ChatFormatting.GREEN)));
        return satellites.size();
    }
}
