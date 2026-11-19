package com.atomiclegacy.event;

import com.mojang.brigadier.tree.CommandNode;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.pollution.HbmPollutionWorlds;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.worldgen.ZirnoxWasteManager;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * [e3] Checks of the second bug pass that can run on a dedicated server without a player: command permission
 * levels against com.hbm.commands, config-driven divisors, cleanup of per-world static state and network lookups
 * that must not load chunks. Not covered here: packet handlers and the player clone/dimension hooks (need a player).
 */
final class ParityE3BugDiagnostics {
    private ParityE3BugDiagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            commands(server, problems, cases);
            divisors(problems, cases);
            staticState(server.overworld(), problems, cases);
            unloadedLookups(server.overworld(), problems, cases);
        } catch (Exception exception) {
            problems.add("E3: diagnostics threw " + exception);
        }
        return cases[0];
    }

    /** CommandBase default level 4; CommandCustomize 0; CommandReloadConfig players only; /rhbm is port-only (2). */
    private static void commands(MinecraftServer server, List<String> problems, int[] cases) {
        CommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();
        permission(server, root, "ntmrad", 4, problems, cases);
        permission(server, root, "ntmreapnetworks", 4, problems, cases);
        permission(server, root, "ntmreload", 4, problems, cases);
        permission(server, root, "ntmloadchunk", 4, problems, cases);
        permission(server, root, "ntmlocate", 4, problems, cases);
        permission(server, root, "ntmsatellites", 4, problems, cases);
        permission(server, root, "ntmcustomize", 0, problems, cases);
        permission(server, root, "rhbm", 2, problems, cases);

        cases[0]++;
        CommandNode<CommandSourceStack> config = root.getChild("ntmserver");
        if (config == null) {
            problems.add("E3: command ntmserver is not registered");
        } else if (config.canUse(server.createCommandSourceStack().withPermission(4))) {
            problems.add("E3: ntmserver must be usable by players only (CommandReloadConfig#canCommandSenderUseCommand)");
        }
    }

    private static void permission(MinecraftServer server, CommandNode<CommandSourceStack> root, String name, int level,
                                   List<String> problems, int[] cases) {
        cases[0]++;
        CommandNode<CommandSourceStack> node = root.getChild(name);
        if (node == null) {
            problems.add("E3: command " + name + " is not registered");
            return;
        }
        if (!node.canUse(server.createCommandSourceStack().withPermission(level))) {
            problems.add("E3: command " + name + " is not usable at permission level " + level);
        }
        if (level > 0 && node.canUse(server.createCommandSourceStack().withPermission(level - 1))) {
            problems.add("E3: command " + name + " is usable below permission level " + level);
        }
    }

    private static void divisors(List<String> problems, int[] cases) {
        cases[0]++;
        if (HbmConfig.glyphidSwarmCooldown() <= 0) {
            problems.add("E3: glyphidSwarmCooldown() must be positive (modulo operand of the spawner tick)");
        }
        cases[0]++;
        if (Math.min(HbmConfig.glyphidScoutSwarmChance(), Integer.MAX_VALUE - 1) + 1 <= 0) {
            problems.add("E3: glyphid scout swarm bound must be positive");
        }
    }

    private static void staticState(ServerLevel level, List<String> problems, int[] cases) {
        ResourceKey<Level> probe = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("atomiclegacy", "e3_probe"));

        cases[0]++;
        ZirnoxWasteManager.clear(probe);
        if (ZirnoxWasteManager.pending(probe) != 0) {
            problems.add("E3: ZirnoxWasteManager kept tasks of a cleared dimension");
        }

        // Scheduled work is only queued here; it is dropped again before any level tick can run it.
        if (ZirnoxWasteManager.pending(level.dimension()) == 0) { // never drop real pending work
            cases[0]++;
            ZirnoxWasteManager.schedule(level, new BlockPos(0, level.getMaxBuildHeight() - 1, 0), 1);
            boolean queued = ZirnoxWasteManager.pending(level.dimension()) == 1;
            ZirnoxWasteManager.clear(level.dimension());
            if (!queued || ZirnoxWasteManager.pending(level.dimension()) != 0) {
                problems.add("E3: ZirnoxWasteManager.clear(dimension) did not drop the queue");
            }
        }

        cases[0]++;
        int solvers = HbmPollutionWorlds.solverCount();
        HbmPollutionWorlds.unload(probe);
        if (HbmPollutionWorlds.solverCount() != solvers) {
            problems.add("E3: HbmPollutionWorlds.unload removed the solver of another dimension");
        }

        cases[0]++;
        MeteorPedestalBroadcasts.clear(probe);
        if (MeteorPedestalBroadcasts.influence(level, BlockPos.ZERO) == null) {
            problems.add("E3: MeteorPedestalBroadcasts.influence returned null after a clear");
        }
    }

    /** Power and fluid network lookups at a position whose chunk is not loaded answer "nothing" and leave it unloaded. */
    private static void unloadedLookups(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos far = new BlockPos(1_000_008, 64, 1_000_008);
        int chunkX = far.getX() >> 4;
        int chunkZ = far.getZ() >> 4;
        if (level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            return; // someone really is out there; nothing to check
        }

        cases[0]++;
        if (PowerNetworkManager.endpointAt(level, far) != null) {
            problems.add("E3: PowerNetworkManager.endpointAt found an endpoint in an unloaded chunk");
        }
        cases[0]++;
        // the cable itself would sit in the unloaded chunk as well: the probe may only answer false
        if (PowerNetworkManager.canCableConnectTo(level, far, Direction.EAST)) {
            problems.add("E3: PowerNetworkManager.canCableConnectTo connected into an unloaded chunk");
        }
        cases[0]++;
        if (HbmFluidNetworks.netUser(level, far) != null) {
            problems.add("E3: HbmFluidNetworks.netUser found a user in an unloaded chunk");
        }
        cases[0]++;
        if (level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            problems.add("E3: a power/fluid network lookup loaded chunk " + chunkX + "," + chunkZ);
        }
    }
}
