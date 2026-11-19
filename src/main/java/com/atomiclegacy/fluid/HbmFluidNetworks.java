package com.atomiclegacy.fluid;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.ChimneyBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import com.atomiclegacy.tileentity.FluidPipeBlockEntity;
import com.atomiclegacy.integration.sable.HbmSablePowerCompat;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.ExternalCapabilitySafety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumSet;

@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class HbmFluidNetworks {
    private static final int MAX_PIPE_SEARCH = 1024;
    private static final int MAX_BALANCE_PER_ENDPOINT = 1_000_000_000;
    private static final long PRUNE_INTERVAL_TICKS = 20L;
    private static final TagKey<Block> MOBILE_FLUID_COMPATIBLE = TagKey.create(
            Registries.BLOCK,
            AtomicLegacy.id("mobile_fluid_compatible")
    );
    private static final Map<NetworkKey, LevelNetwork> NETWORKS = new ConcurrentHashMap<>();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Comparator<Endpoint> ENDPOINT_ORDER = Comparator
            .comparingInt((Endpoint endpoint) -> endpoint.pos().getX())
            .thenComparingInt(endpoint -> endpoint.pos().getY())
            .thenComparingInt(endpoint -> endpoint.pos().getZ())
            .thenComparingInt(endpoint -> endpoint.side().ordinal());

    private HbmFluidNetworks() {
    }

    /** [parity T1: fluid net] ModEventHandler#onServerTick (Phase.START): UniNodespace.updateNodespace() */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Map.Entry<NetworkKey, LevelNetwork> entry : NETWORKS.entrySet()) {
                if (entry.getKey().server() == level.getServer()
                        && entry.getKey().dimension().equals(level.dimension())) {
                    entry.getValue().updateNetworks(level);
                }
            }
        }
    }

    /** [parity T1: diagnostics] runs one UniNodespace update for the level immediately (ignores the once-per-tick guard). */
    public static void updateNow(ServerLevel level) {
        for (Map.Entry<NetworkKey, LevelNetwork> entry : NETWORKS.entrySet()) {
            if (entry.getKey().server() == level.getServer() && entry.getKey().dimension().equals(level.dimension())) {
                entry.getValue().lastUpdateTick = Long.MIN_VALUE;
                entry.getValue().updateNetworks(level);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            NETWORKS.keySet().removeIf(key -> key.server() == level.getServer()
                    && key.dimension().equals(level.dimension()));
        }
    }

    /**
     * [parity S2: commands] CommandReapNetworks: drops every fluid net of the server; loaded ducts register
     * again (ducts are the only nodes of this network), like expired GenNodes. Returns the number of dropped nets.
     */
    public static int reapAll(net.minecraft.server.MinecraftServer server) {
        Map<ResourceKey<Level>, List<BlockPos>> nodes = new HashMap<>();
        int count = 0;
        for (Map.Entry<NetworkKey, LevelNetwork> entry : List.copyOf(NETWORKS.entrySet())) {
            if (entry.getKey().server() != server) continue;
            nodes.computeIfAbsent(entry.getKey().dimension(), ignored -> new ArrayList<>())
                    .addAll(List.copyOf(entry.getValue().pipes.keySet()));
            if (NETWORKS.remove(entry.getKey()) != null) count++;
        }
        nodes.forEach((dimension, positions) -> {
            net.minecraft.server.level.ServerLevel level = server.getLevel(dimension);
            if (level == null) return;
            for (BlockPos pos : positions) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos)
                        instanceof com.atomiclegacy.tileentity.FluidPipeBlockEntity pipe) {
                    pipe.onLoad();
                }
            }
        });
        return count;
    }

    /**
     * [parity U1: fluid ports] TilePort#update + checkSubscribe/checkProvide: a machine with port nodes
     * ({@link HbmFluidNetUser#fluidPorts}) keeps them alive by calling this every server tick. Its ports join
     * the duct nets they touch and the ports of directly adjacent machines (one shared net per fluid type).
     */
    public static void subscribe(Level level, HbmFluidNetUser user) {
        if (level == null || level.isClientSide || !(user.fluidNetOwner() instanceof BlockEntity blockEntity) || !user.isFluidNetUser()) { // [parity V1: fluid net] delegates
            return;
        }
        network(level, blockEntity.getBlockPos()).addPortUser(user);
    }

    public static void registerPipe(Level level, BlockPos pos, HbmFluidDefinition type, boolean open) {
        if (!level.isClientSide) {
            if (!mobileFluidNodeAllowed(level, pos)) {
                removePipe(level, pos);
                return;
            }
            Set<String> names = type == null || type.isNone() ? Set.of() : Set.of(type.name());
            network(level, pos).upsert(pos, names, open, networkLinks(level, pos),
                    allowedDirections(level, pos, type == null ? List.of() : List.of(type)));
        }
    }

    public static void registerPipe(Level level, BlockPos pos, List<HbmFluidDefinition> types, boolean open) {
        if (!level.isClientSide) {
            if (!mobileFluidNodeAllowed(level, pos)) {
                removePipe(level, pos);
                return;
            }
            Set<String> typeNames = new HashSet<>();
            if (types != null) {
                for (HbmFluidDefinition type : types) {
                    if (type != null && !type.isNone()) {
                        typeNames.add(type.name());
                    }
                }
            }
            network(level, pos).upsert(pos, typeNames, open, networkLinks(level, pos),
                    allowedDirections(level, pos, types == null ? List.of() : types));
        }
    }

    private static List<BlockPos> networkLinks(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof FluidPipeBlockEntity pipe)) {
            return List.of();
        }
        ArrayList<BlockPos> links = new ArrayList<>();
        for (BlockPos link : pipe.networkLinks()) {
            if (link != null && sameFluidSpace(level, pos, link) && mobileFluidNodeAllowed(level, link)) {
                links.add(link.immutable());
            }
        }
        return List.copyOf(links);
    }

    private static Set<Direction> allowedDirections(Level level, BlockPos pos, List<HbmFluidDefinition> types) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof FluidPipeBlockEntity pipe)) {
            return Set.of();
        }
        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            for (HbmFluidDefinition type : types) {
                if (type != null && pipe.canConnectFrom(direction, type)) {
                    directions.add(direction);
                    break;
                }
            }
        }
        return Set.copyOf(directions);
    }

    public static void unregisterPipe(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            removePipe(level, pos);
        }
    }

    public static boolean canPipeConnect(LevelAccessor level, BlockPos pipePos, Direction direction, HbmFluidDefinition type) {
        return canPipeConnectTo(level, pipePos, pipePos.relative(direction), direction, type);
    }

    private static boolean canPipeConnectTo(LevelAccessor level, BlockPos pipePos, BlockPos target,
                                            Direction direction, HbmFluidDefinition type) {
        if (!mobileFluidNodeAllowed(level, pipePos)) {
            return false;
        }
        BlockEntity current = level.getBlockEntity(pipePos);
        if (!(current instanceof FluidPipeBlockEntity pipe) || !pipe.canConnectFrom(direction, type)) {
            return false;
        }
        if (!sameFluidSpace(level, pipePos, target)) {
            return false;
        }
        if (!level.hasChunkAt(target)) { // [e3] never load the neighbouring chunk for a connection probe
            return false;
        }
        BlockEntity neighbor = level.getBlockEntity(target);
        if (neighbor instanceof FluidPipeBlockEntity neighborPipe) {
            return mobileFluidNodeAllowed(level, target)
                    && neighborPipe.canConnectFrom(direction.getOpposite(), type);
        }
        if (type == null || type.isNone()) {
            return false;
        }
        Direction targetSide = direction.getOpposite();
        if (isLegacyChimneyExhaustPort(level, target, targetSide, type)) {
            return true;
        }
        if (level instanceof Level realLevel) {
            if (!realLevel.isLoaded(target) || !mobileFluidEndpointAllowed(realLevel, target)) {
                return false;
            }
            return fluidHandler(realLevel, target, targetSide, "pipe connection probe") != null;
        }
        return false;
    }

    private static boolean isLegacyChimneyExhaustPort(LevelAccessor level, BlockPos target, Direction side, HbmFluidDefinition type) {
        if (!(level.getBlockEntity(target) instanceof MachineDummyBlockEntity dummy)) {
            return false;
        }
        BlockEntity core = dummy.core();
        return core instanceof ChimneyBlockEntity chimney
                && chimney.canConnectLegacyExhaustPort(target, side, type);
    }

    /**
     * The direct modern counterpart to FluidDuctBase#getDebugInfo().  It only
     * reports a live connected component, never a guessed machine endpoint.
     */
    public static List<String> debugInfo(Level level, BlockPos pos) {
        if (!mobileFluidNodeAllowed(level, pos) || !(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) {
            return List.of();
        }
        HbmFluidDefinition type = pipe.type();
        if (type == null || type.isNone()) {
            return List.of();
        }
        // [parity T1: fluid net] FluidDuctBase#getDuctDebugInfo: links (nodes), subscribers, providers, fluidTracker
        Set<BlockPos> pipes = cachedPipes(level, pos, type);
        if (pipes.isEmpty()) {
            return List.of();
        }
        int[] stats = network(level, pos).netStats(cursorKey(type, pipes)); // [parity U1: fluid ports] whole net
        if (stats == null) {
            NetEndpoints endpoints = netEndpoints(level, type, pipes);
            stats = new int[]{pipes.size() + endpoints.touched().size(), endpoints.receivers().size(), endpoints.providers().size()};
        }
        return List.of(
                "Links: " + stats[0],
                "Subscribers: " + stats[1],
                "Providers: " + stats[2],
                "Transfer: " + network(level, pos).lastTransfer(cursorKey(type, pipes))
        );
    }

    /** Returns the amount moved by this connected component during the last completed server tick. */
    public static int lastTransfer(Level level, BlockPos pos, HbmFluidDefinition type) {
        if (level.isClientSide || type == null || type.isNone() || !mobileFluidNodeAllowed(level, pos)) {
            return 0;
        }
        Set<BlockPos> pipes = cachedPipes(level, pos, type);
        if (pipes.isEmpty()) {
            return 0;
        }
        return network(level, pos).lastTransfer(cursorKey(type, pipes));
    }

    public static FluidStack drainFrom(Level level, BlockPos target, Direction side, HbmFluidDefinition type, int amount, @Nullable BlockPos excluded, boolean execute) {
        if (type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        if (excluded != null && !sameFluidSpace(level, target, excluded)) {
            return FluidStack.EMPTY;
        }
        if (!level.isLoaded(target)) { // [e3] Compat.getTileStandard: chunkExists check
            return FluidStack.EMPTY;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof FluidPipeBlockEntity pipe && pipe.canConnect(type)) {
            if (!mobileFluidNodeAllowed(level, target) || pipePushHandledByNet(level, excluded)) { // [parity T1: fluid net]
                return FluidStack.EMPTY;
            }
            return drainFromPipeNetwork(level, target, type, amount, excluded, execute);
        }
        if (!mobileFluidEndpointAllowed(level, target)) {
            return FluidStack.EMPTY;
        }
        IFluidHandler handler = fluidHandler(level, target, side, "direct drain discovery");
        if (handler == null) {
            return FluidStack.EMPTY;
        }
        return drainCompatible(level, target, side, handler, type, amount,
                execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
    }

    private static FluidStack drainCompatible(Level level, BlockPos pos, @Nullable Direction side,
                                              IFluidHandler handler, HbmFluidDefinition type, int amount,
                                              IFluidHandler.FluidAction action) {
        if (handler == null || type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack exact = drainEndpoint(level, pos, side, handler, HbmFluids.toNeoStack(type, amount), action,
                "typed drain");
        if (!exact.isEmpty() && HbmFluids.fromNeoFluid(exact.getFluid()).filter(definition -> definition == type).isPresent()) {
            return HbmFluids.toNeoStack(type, Math.min(amount, exact.getAmount()));
        }

        int tanks = endpointTankCount(level, pos, side, handler);
        for (int tank = 0; tank < tanks; tank++) {
            FluidStack stored = endpointFluidInTank(level, pos, side, handler, tank);
            if (stored.isEmpty()) {
                continue;
            }
            if (HbmFluids.fromNeoFluid(stored.getFluid()).filter(definition -> definition == type).isEmpty()) {
                continue;
            }
            FluidStack request = stored.copy();
            request.setAmount(Math.min(amount, stored.getAmount()));
            FluidStack drained = drainEndpoint(level, pos, side, handler, request, action, "compatible drain");
            if (!drained.isEmpty()
                    && HbmFluids.fromNeoFluid(drained.getFluid()).filter(definition -> definition == type).isPresent()) {
                return HbmFluids.toNeoStack(type, Math.min(amount, drained.getAmount()));
            }
        }

        return FluidStack.EMPTY;
    }

    public static int fillInto(Level level, BlockPos target, Direction side, FluidStack stack, @Nullable BlockPos excluded, boolean execute) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (excluded != null && !sameFluidSpace(level, target, excluded)) {
            return 0;
        }
        HbmFluidDefinition type = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
        if (type.isNone()) {
            return 0;
        }
        if (!level.isLoaded(target)) { // [e3] Compat.getTileStandard: chunkExists check
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof FluidPipeBlockEntity pipe && pipe.canConnect(type)) {
            if (!mobileFluidNodeAllowed(level, target) || pipePushHandledByNet(level, excluded)) { // [parity T1: fluid net]
                return 0;
            }
            return fillPipeNetwork(level, target, type, stack, excluded, execute);
        }
        if (!mobileFluidEndpointAllowed(level, target)) {
            return 0;
        }
        IFluidHandler handler = fluidHandler(level, target, side, "direct fill discovery");
        return handler == null ? 0 : fillEndpoint(level, target, side, handler, stack,
                execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE,
                "direct fill");
    }

    public static FluidStack drainFromPipeNetwork(Level level, BlockPos start, HbmFluidDefinition type, int amount, @Nullable BlockPos excluded, boolean execute) {
        if (type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        if (!mobileFluidNodeAllowed(level, start)
                || excluded != null && !sameFluidSpace(level, start, excluded)) {
            return FluidStack.EMPTY;
        }

        EndpointSelection selection = endpoints(level, start, type, excluded);
        if (selection.endpoints().isEmpty()) {
            return FluidStack.EMPTY;
        }

        LevelNetwork network = network(level, start);
        int startIndex = network.cursor(CursorKind.DRAIN, selection.cursorKey(), selection.endpoints().size());
        int remaining = amount;
        int drainedTotal = 0;
        int lastSuccessIndex = -1;

        for (int i = 0; i < selection.endpoints().size() && remaining > 0; i++) {
            int endpointIndex = (startIndex + i) % selection.endpoints().size();
            Endpoint endpoint = selection.endpoints().get(endpointIndex);
            FluidStack drained = drainCompatible(level, endpoint.pos(), endpoint.side(), endpoint.handler(), type, remaining,
                    execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) {
                continue;
            }
            int accepted = Math.min(remaining, drained.getAmount());
            drainedTotal += accepted;
            remaining -= accepted;
            lastSuccessIndex = endpointIndex;
        }

        if (execute && lastSuccessIndex >= 0) {
            network.advance(CursorKind.DRAIN, selection.cursorKey(), lastSuccessIndex + 1, selection.endpoints().size());
        }
        if (execute && drainedTotal > 0) {
            network.recordTransfer(selection.cursorKey(), drainedTotal);
        }
        return drainedTotal <= 0 ? FluidStack.EMPTY : HbmFluids.toNeoStack(type, drainedTotal);
    }

    public static int fillPipeNetwork(Level level, BlockPos start, HbmFluidDefinition type, FluidStack offered, @Nullable BlockPos excluded, boolean execute) {
        if (!mobileFluidNodeAllowed(level, start)
                || excluded != null && !sameFluidSpace(level, start, excluded)) {
            return 0;
        }
        int remaining = offered.getAmount();
        int filled = 0;
        EndpointSelection selection = endpoints(level, start, type, excluded);
        if (selection.endpoints().isEmpty()) {
            return 0;
        }

        LevelNetwork network = network(level, start);
        int startIndex = network.cursor(CursorKind.FILL, selection.cursorKey(), selection.endpoints().size());
        int lastSuccessIndex = -1;

        for (int i = 0; i < selection.endpoints().size() && remaining > 0; i++) {
            int endpointIndex = (startIndex + i) % selection.endpoints().size();
            Endpoint endpoint = selection.endpoints().get(endpointIndex);
            FluidStack slice = offered.copy();
            slice.setAmount(remaining);
            int accepted = fillEndpoint(level, endpoint.pos(), endpoint.side(), endpoint.handler(), slice,
                    execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE,
                    "pipe-network fill");
            if (accepted <= 0) {
                continue;
            }
            int moved = Math.min(remaining, accepted);
            remaining -= moved;
            filled += moved;
            lastSuccessIndex = endpointIndex;
        }

        if (execute && lastSuccessIndex >= 0) {
            network.advance(CursorKind.FILL, selection.cursorKey(), lastSuccessIndex + 1, selection.endpoints().size());
        }
        if (execute && filled > 0) {
            network.recordTransfer(selection.cursorKey(), filled);
        }
        return filled;
    }

    private static EndpointSelection endpoints(Level level, BlockPos start, HbmFluidDefinition type, @Nullable BlockPos excluded) {
        Set<BlockPos> pipes = cachedPipes(level, start, type);
        return endpoints(level, type, pipes, excluded);
    }

    private static EndpointSelection endpoints(Level level, HbmFluidDefinition type, Set<BlockPos> pipes, @Nullable BlockPos excluded) {
        ArrayList<Endpoint> endpoints = new ArrayList<>();
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        for (BlockPos pipePos : pipes) {
            for (Direction direction : DIRECTIONS) {
                target.setWithOffset(pipePos, direction);
                if (target.equals(excluded) || pipes.contains(target)
                        || !canPipeConnectTo(level, pipePos, target, direction, type)) {
                    continue;
                }
                Direction endpointSide = direction.getOpposite();
                IFluidHandler handler = fluidHandler(level, target, endpointSide, "pipe endpoint discovery");
                if (handler != null) {
                    endpoints.add(new Endpoint(target.immutable(), endpointSide, handler));
                }
            }
        }
        endpoints.sort(ENDPOINT_ORDER);
        return new EndpointSelection(cursorKey(type, pipes), List.copyOf(endpoints));
    }

    private static EndpointCursorKey cursorKey(HbmFluidDefinition type, Set<BlockPos> pipes) {
        BlockPos anchor = null;
        for (BlockPos candidate : pipes) {
            if (anchor == null || comparePositions(candidate, anchor) < 0) {
                anchor = candidate;
            }
        }
        anchor = anchor == null ? BlockPos.ZERO : anchor.immutable();
        return new EndpointCursorKey(type == null ? "none" : type.name(), anchor);
    }

    private static int comparePositions(BlockPos left, BlockPos right) {
        int result = Integer.compare(left.getX(), right.getX());
        if (result == 0) {
            result = Integer.compare(left.getY(), right.getY());
        }
        return result == 0 ? Integer.compare(left.getZ(), right.getZ()) : result;
    }

    /// [parity T1: fluid net] FluidNetMK2 subscribers ///

    /** Resolves the MK2 user behind a block (the block entity itself or the core of a multiblock dummy). */
    @Nullable
    public static HbmFluidNetUser netUser(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) { // [e3] Compat.getTileStandard: chunkExists check
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HbmFluidNetUser user) {
            return user.isFluidNetUser() ? user : null;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy && dummy.core() instanceof HbmFluidNetUser user) {
            return user.isFluidNetUser() ? user : null;
        }
        // [parity U1: fluid ports] TileEntityBlockPWR (pwr_port) delegates to its controller
        if (blockEntity instanceof com.atomiclegacy.tileentity.PwrBlockEntity part && part.isPort()
                && part.controller() instanceof HbmFluidNetUser user) {
            return user;
        }
        return null;
    }

    private static boolean isBadLink(@Nullable Object user) {
        if (user instanceof HbmFluidNetUser netUser && !(user instanceof BlockEntity)) user = netUser.fluidNetOwner(); // [parity V1: fluid net]
        return user instanceof BlockEntity blockEntity
                && (blockEntity.isRemoved() || blockEntity.getLevel() == null
                || !blockEntity.getLevel().isLoaded(blockEntity.getBlockPos()));
    }

    /**
     * The subscribers of one net: MK2 users are deduplicated by identity like the HashSets
     * NodeNet.providerEntries/receiverEntries, capability endpoints by position and side.
     */
    private static NetEndpoints netEndpoints(Level level, HbmFluidDefinition type, Set<BlockPos> pipes) {
        return netEndpoints(level, type, pipes, List.of(), List.of());
    }

    private static NetEndpoints netEndpoints(Level level, HbmFluidDefinition type, Set<BlockPos> pipes,
                                             List<NodeKey> portUsers, List<Endpoint> neighbours) {
        Map<Object, NetEndpoint> byKey = new java.util.LinkedHashMap<>();
        if (!pipes.isEmpty()) for (Endpoint endpoint : endpoints(level, type, pipes, null).endpoints()) {
            if (!fluidEndpointMatchesNetwork(level, endpoint.pos(), networkKey(level, pipes.iterator().next()))) {
                continue;
            }
            HbmFluidNetUser user = netUser(level, endpoint.pos());
            if (user != null) {
                if (user.fluidPorts(type) != null) {
                    continue; // [parity U1: fluid ports] joins through its own port nodes
                }
                if (!isBadLink(user)) {
                    byKey.computeIfAbsent(user, ignored -> new UserEndpoint(user, type));
                }
            } else {
                EndpointKey key = new EndpointKey(endpoint.pos(), endpoint.side());
                byKey.computeIfAbsent(key, ignored -> new HandlerEndpoint(level, endpoint, type));
            }
        }
        for (NodeKey node : portUsers) { // [parity U1: fluid ports]
            if (!isBadLink(node.user())) {
                NetEndpoint existing = byKey.get(node.user());
                if (existing instanceof UserEndpoint endpoint) {
                    endpoint.allow(node.role());
                } else if (existing == null) {
                    UserEndpoint endpoint = new UserEndpoint(node.user(), type);
                    endpoint.receive = false;
                    endpoint.provide = false;
                    endpoint.allow(node.role());
                    byKey.put(node.user(), endpoint);
                }
            }
        }
        for (Endpoint endpoint : neighbours) { // [parity U1: fluid ports] non-HBM block at a port connection
            byKey.computeIfAbsent(new EndpointKey(endpoint.pos(), endpoint.side()),
                    ignored -> new HandlerEndpoint(level, endpoint, type));
        }
        List<NetEndpoint> providers = new ArrayList<>();
        List<NetEndpoint> receivers = new ArrayList<>();
        for (NetEndpoint endpoint : byKey.values()) {
            if (endpoint.isProvider()) providers.add(endpoint);
            if (endpoint.isReceiver()) receivers.add(endpoint);
        }
        return new NetEndpoints(providers, receivers, List.copyOf(byKey.values()));
    }

    private record NetEndpoints(List<NetEndpoint> providers, List<NetEndpoint> receivers, List<NetEndpoint> touched) {
    }

    private interface NetEndpoint extends HbmFluidNetMK2Transfer.Endpoint {
        void finish();
    }

    /** IFluidStandardTransceiverMK2 endpoint. */
    private static final class UserEndpoint implements NetEndpoint {
        private final HbmFluidNetUser user;
        private final HbmFluidDefinition type;
        private boolean changed;
        /** [parity U1: fluid ports] TilePort roles: checkSubscribe (in ports) / checkProvide (out ports) */
        private boolean receive = true;
        private boolean provide = true;

        private UserEndpoint(HbmFluidNetUser user, HbmFluidDefinition type) {
            this.user = user;
            this.type = type;
        }

        private void allow(HbmFluidNetUser.PortRole role) {
            this.receive |= role != HbmFluidNetUser.PortRole.PROVIDE;
            this.provide |= role != HbmFluidNetUser.PortRole.RECEIVE;
        }

        @Override public boolean isProvider() { return provide && user.isFluidProvider(type); }
        @Override public boolean isReceiver() { return receive && user.isFluidReceiver(type); }
        @Override public int[] providingPressureRange() { return user.providingPressureRange(type); }
        @Override public int[] receivingPressureRange() { return user.receivingPressureRange(type); }
        @Override public long fluidAvailable(int pressure) { return user.fluidAvailable(type, pressure); }
        @Override public long providerSpeed(int pressure) { return user.fluidProviderSpeed(type, pressure); }
        @Override public long demand(int pressure) { return user.fluidDemand(type, pressure); }
        @Override public long receiverSpeed(int pressure) { return user.fluidReceiverSpeed(type, pressure); }
        @Override public PowerEndpoint.ConnectionPriority priority() { return user.fluidPriority(); }

        @Override
        public void useUpFluid(int pressure, long amount) {
            user.useUpFluid(type, pressure, amount);
            changed |= amount > 0;
        }

        @Override
        public long transferFluid(int pressure, long amount) {
            long remainder = user.transferFluid(type, pressure, amount);
            changed |= remainder != amount;
            return remainder;
        }

        @Override
        public void finish() {
            if (changed) {
                user.onFluidNetTransfer();
            }
        }
    }

    /**
     * Foreign endpoint (other mods, machines without MK2 tanks): the NeoForge fluid capability as a
     * pressure-0 transceiver with NORMAL priority and unlimited speed.
     */
    private static final class HandlerEndpoint implements NetEndpoint {
        private final Level level;
        private final Endpoint endpoint;
        private final HbmFluidDefinition type;
        private final long available;
        private final long demand;

        private HandlerEndpoint(Level level, Endpoint endpoint, HbmFluidDefinition type) {
            this.level = level;
            this.endpoint = endpoint;
            this.type = type;
            FluidStack drained = drainCompatible(level, endpoint.pos(), endpoint.side(), endpoint.handler(), type,
                    MAX_BALANCE_PER_ENDPOINT, IFluidHandler.FluidAction.SIMULATE);
            this.available = drained.isEmpty() ? 0 : drained.getAmount();
            this.demand = fillEndpoint(level, endpoint.pos(), endpoint.side(), endpoint.handler(),
                    HbmFluids.toNeoStack(type, MAX_BALANCE_PER_ENDPOINT), IFluidHandler.FluidAction.SIMULATE,
                    "net demand probe");
        }

        @Override public boolean isProvider() { return available > 0; }
        @Override public boolean isReceiver() { return demand > 0; }
        @Override public int[] providingPressureRange() { return HbmFluidNetUser.DEFAULT_PRESSURE_RANGE; }
        @Override public int[] receivingPressureRange() { return HbmFluidNetUser.DEFAULT_PRESSURE_RANGE; }
        @Override public long providerSpeed(int pressure) { return HbmFluidNetUser.DEFAULT_SPEED; }
        @Override public long receiverSpeed(int pressure) { return HbmFluidNetUser.DEFAULT_SPEED; }
        @Override public PowerEndpoint.ConnectionPriority priority() { return PowerEndpoint.ConnectionPriority.NORMAL; }

        @Override
        public long fluidAvailable(int pressure) {
            if (pressure != 0) return 0;
            FluidStack drained = drainCompatible(level, endpoint.pos(), endpoint.side(), endpoint.handler(), type,
                    MAX_BALANCE_PER_ENDPOINT, IFluidHandler.FluidAction.SIMULATE);
            return drained.isEmpty() ? 0 : drained.getAmount();
        }

        @Override
        public void useUpFluid(int pressure, long amount) {
            if (pressure != 0 || amount <= 0) return;
            drainCompatible(level, endpoint.pos(), endpoint.side(), endpoint.handler(), type,
                    (int) Math.min(Integer.MAX_VALUE, amount), IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public long demand(int pressure) {
            return pressure == 0 ? demand : 0;
        }

        @Override
        public long transferFluid(int pressure, long amount) {
            if (pressure != 0 || amount <= 0) return amount;
            int offered = (int) Math.min(Integer.MAX_VALUE, amount);
            int accepted = fillEndpoint(level, endpoint.pos(), endpoint.side(), endpoint.handler(),
                    HbmFluids.toNeoStack(type, offered), IFluidHandler.FluidAction.EXECUTE, "net receiver fill");
            return amount - accepted;
        }

        @Override
        public void finish() {
        }
    }

    /** True when the block at {@code source} is an MK2 user: its ports are served by the net itself. */
    private static boolean pipePushHandledByNet(Level level, @Nullable BlockPos source) {
        return source != null && netUser(level, source) != null;
    }

    @Nullable
    private static IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side, String operation) {
        try {
            return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, pos, side, "fluid handler", operation, failure);
            return null;
        }
    }

    private static int fillEndpoint(Level level, BlockPos pos, @Nullable Direction side, IFluidHandler handler,
                                    FluidStack stack, IFluidHandler.FluidAction action, String operation) {
        try {
            int accepted = handler.fill(stack, action);
            if (accepted < 0 || accepted > stack.getAmount()) {
                ExternalCapabilitySafety.report(level, pos, side, "fluid handler", operation,
                        new IllegalStateException("fill returned " + accepted + " for an offer of " + stack.getAmount()));
                return Math.max(0, Math.min(stack.getAmount(), accepted));
            }
            return accepted;
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, pos, side, "fluid handler", operation, failure);
            return 0;
        }
    }

    private static FluidStack drainEndpoint(Level level, BlockPos pos, @Nullable Direction side, IFluidHandler handler,
                                            FluidStack request, IFluidHandler.FluidAction action, String operation) {
        try {
            FluidStack drained = handler.drain(request, action);
            if (drained == null) {
                ExternalCapabilitySafety.report(level, pos, side, "fluid handler", operation,
                        new IllegalStateException("drain returned null"));
                return FluidStack.EMPTY;
            }
            if (drained.getAmount() > request.getAmount()) {
                ExternalCapabilitySafety.report(level, pos, side, "fluid handler", operation,
                        new IllegalStateException("drain returned " + drained.getAmount()
                                + " for a request of " + request.getAmount()));
                FluidStack clamped = drained.copy();
                clamped.setAmount(request.getAmount());
                return clamped;
            }
            return drained;
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, pos, side, "fluid handler", operation, failure);
            return FluidStack.EMPTY;
        }
    }

    private static int endpointTankCount(Level level, BlockPos pos, @Nullable Direction side, IFluidHandler handler) {
        try {
            int tanks = handler.getTanks();
            if (tanks < 0) {
                ExternalCapabilitySafety.report(level, pos, side, "fluid handler", "tank count probe",
                        new IllegalStateException("getTanks returned " + tanks));
                return 0;
            }
            return tanks;
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, pos, side, "fluid handler", "tank count probe", failure);
            return 0;
        }
    }

    private static FluidStack endpointFluidInTank(Level level, BlockPos pos, @Nullable Direction side,
                                                  IFluidHandler handler, int tank) {
        try {
            FluidStack stored = handler.getFluidInTank(tank);
            if (stored == null) {
                ExternalCapabilitySafety.report(level, pos, side, "fluid handler", "tank content probe",
                        new IllegalStateException("getFluidInTank returned null for tank " + tank));
                return FluidStack.EMPTY;
            }
            return stored;
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, pos, side, "fluid handler", "tank content probe", failure);
            return FluidStack.EMPTY;
        }
    }

    private static Set<BlockPos> cachedPipes(Level level, BlockPos start, HbmFluidDefinition type) {
        Set<BlockPos> cached = network(level, start).componentFor(start, type);
        return cached == null ? collectPipes(level, start, type) : cached;
    }

    private static Set<BlockPos> collectPipes(Level level, BlockPos start, HbmFluidDefinition type) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty() && visited.size() < MAX_PIPE_SEARCH) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof FluidPipeBlockEntity pipe) || !pipe.canConnect(type)) {
                visited.remove(pos);
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || !level.isLoaded(next)) { // [e3] no chunk loading from a pipe walk
                    continue;
                }
                BlockEntity neighbor = level.getBlockEntity(next);
                if (neighbor instanceof FluidPipeBlockEntity nextPipe
                        && canPipeConnect(level, pos, direction, type)) {
                    queue.addLast(next);
                }
            }
            for (BlockPos link : pipe.networkLinks()) {
                if (!level.isLoaded(link)) { // [e3]
                    continue;
                }
                BlockEntity remote = level.getBlockEntity(link);
                if (sameFluidSpace(level, pos, link)
                        && mobileFluidNodeAllowed(level, link)
                        && remote instanceof FluidPipeBlockEntity remotePipe
                        && remotePipe.networkLinks().contains(pos)
                        && remotePipe.canConnect(type)) {
                    queue.addLast(link.immutable());
                }
            }
        }
        return visited;
    }

    private static LevelNetwork network(Level level, BlockPos pos) {
        return NETWORKS.computeIfAbsent(networkKey(level, pos), LevelNetwork::new);
    }

    private static NetworkKey networkKey(Level level, BlockPos pos) {
        return new NetworkKey(level.getServer(), level.dimension(), HbmSablePowerCompat.powerSpaceId(level, pos));
    }

    private static void removePipe(Level level, BlockPos pos) {
        BlockPos removed = pos.immutable();
        for (Map.Entry<NetworkKey, LevelNetwork> entry : NETWORKS.entrySet()) {
            if (entry.getKey().server() == level.getServer()
                    && entry.getKey().dimension().equals(level.dimension())) {
                entry.getValue().remove(removed);
            }
        }
    }

    private static boolean sameFluidSpace(LevelAccessor level, BlockPos first, BlockPos second) {
        return HbmSablePowerCompat.samePowerSpace(level, first, second);
    }

    private static boolean mobileFluidNodeAllowed(LevelAccessor level, BlockPos pos) {
        return !HbmSablePowerCompat.isSubLevelBlock(level, pos)
                || level.getBlockState(pos).is(MOBILE_FLUID_COMPATIBLE);
    }

    private static boolean mobileFluidEndpointAllowed(LevelAccessor level, BlockPos pos) {
        if (!HbmSablePowerCompat.isSubLevelBlock(level, pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(MOBILE_FLUID_COMPATIBLE)) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            if (sameFluidSpace(level, pos, corePos)
                    && level.getBlockState(corePos).is(MOBILE_FLUID_COMPATIBLE)) {
                return true;
            }
        }
        return !AtomicLegacy.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    private static boolean fluidEndpointMatchesNetwork(Level level, BlockPos pos, NetworkKey networkKey) {
        return networkKey.equals(networkKey(level, pos)) && mobileFluidEndpointAllowed(level, pos);
    }

    private record Endpoint(BlockPos pos, Direction side, IFluidHandler handler) {
    }

    private record EndpointSelection(EndpointCursorKey cursorKey, List<Endpoint> endpoints) {
    }

    private record EndpointCursorKey(String typeName, BlockPos anchor) {
    }

    private enum CursorKind {
        FILL,
        DRAIN
    }

    private record EndpointKey(BlockPos pos, Direction side) {
    }

    private record NetworkKey(net.minecraft.server.MinecraftServer server, ResourceKey<Level> dimension,
                              String fluidSpace) {
    }

    private static final class LevelNetwork {
        private final NetworkKey key;
        private final Map<BlockPos, PipeNode> pipes = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> fillCursors = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> drainCursors = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> lastTransfers = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> currentTransfers = new HashMap<>();
        private Map<PipeComponentKey, PipeComponent> componentsByPipe = Map.of();
        private final Set<HbmFluidNetUser> portUsers = new java.util.LinkedHashSet<>(); // [parity U1: fluid ports]
        private Map<EndpointCursorKey, int[]> netStats = Map.of(); // [parity U1: fluid ports]
        private long graphVersion;
        private long lastPruneTick = Long.MIN_VALUE;
        private long lastUpdateTick = Long.MIN_VALUE;
        private boolean dirty = true;

        private LevelNetwork(NetworkKey key) {
            this.key = key;
        }

        synchronized void upsert(BlockPos pos, Set<String> typeNames, boolean open, List<BlockPos> links,
                                  Set<Direction> directions) {
            Set<BlockPos> linkSet = new HashSet<>();
            if (links != null) {
                for (BlockPos link : links) {
                    if (link != null && !link.equals(pos)) {
                        linkSet.add(link.immutable());
                    }
                }
            }
            PipeNode next = new PipeNode(pos.immutable(), Set.copyOf(typeNames), open,
                    Set.copyOf(linkSet), Set.copyOf(directions));
            PipeNode old = this.pipes.put(next.pos(), next);
            if (!next.equals(old)) {
                markDirty();
            }
        }

        synchronized void remove(BlockPos pos) {
            BlockPos removed = pos.immutable();
            boolean changed = this.pipes.remove(removed) != null;
            for (Map.Entry<BlockPos, PipeNode> entry : new ArrayList<>(this.pipes.entrySet())) {
                PipeNode node = entry.getValue();
                if (node.links().contains(removed)) {
                    Set<BlockPos> links = new HashSet<>(node.links());
                    links.remove(removed);
                    this.pipes.put(entry.getKey(), new PipeNode(node.pos(), node.typeNames(), node.open(),
                            Set.copyOf(links), node.directions()));
                    changed = true;
                }
            }
            if (changed) {
                markDirty();
            }
        }

        synchronized Set<BlockPos> componentFor(BlockPos start, HbmFluidDefinition type) {
            tick();
            PipeComponent component = this.componentsByPipe.get(new PipeComponentKey(start.immutable(), type == null ? "none" : type.name()));
            if (component == null || type == null || type.isNone()) {
                return null;
            }
            return component.pipes();
        }

        synchronized int cursor(CursorKind kind, EndpointCursorKey key, int size) {
            if (size <= 0) {
                return 0;
            }
            return cursorMap(kind).getOrDefault(key, 0) % size;
        }

        synchronized void advance(CursorKind kind, EndpointCursorKey key, int nextIndex, int size) {
            if (size <= 0) {
                return;
            }
            cursorMap(kind).put(key, Math.floorMod(nextIndex, size));
        }

        synchronized void addPortUser(HbmFluidNetUser user) {
            this.portUsers.add(user);
        }

        synchronized int[] netStats(EndpointCursorKey key) {
            return this.netStats.get(key);
        }

        synchronized int lastTransfer(EndpointCursorKey key) {
            return this.lastTransfers.getOrDefault(key, 0);
        }

        synchronized void recordTransfer(EndpointCursorKey key, int amount) {
            if (amount > 0) {
                this.currentTransfers.merge(key, amount, Integer::sum);
            }
        }

        /**
         * [parity T1: fluid net] UniNodespace.updateNodespace() for this level: connections are
         * rebuilt for changed nodes, then every net runs FluidNetMK2#update once. The tracker of this
         * update (plus fluid pushed by not yet converted machines during the previous world tick) is
         * what counter valves read during the following world tick (FluidNetMK2.fluidTracker).
         */
        void updateNetworks(Level level) {
            long gameTime = level.getGameTime();
            Set<PipeComponent> components;
            Map<PipeComponentKey, PipeComponent> componentIndex;
            Map<BlockPos, PipeNode> pipeNodes;
            List<HbmFluidNetUser> users;
            synchronized (this) {
                if (this.lastUpdateTick == gameTime) {
                    return;
                }
                this.lastUpdateTick = gameTime;
                if (this.lastPruneTick == Long.MIN_VALUE || gameTime - this.lastPruneTick >= PRUNE_INTERVAL_TICKS) {
                    pruneInvalidPipes(level);
                    this.lastPruneTick = gameTime;
                }
                tick();
                components = this.componentsByPipe.isEmpty() ? Set.of() : Set.copyOf(this.componentsByPipe.values());
                componentIndex = this.componentsByPipe;
                pipeNodes = Map.copyOf(this.pipes);
                this.portUsers.removeIf(user -> isBadLink(user) || !(user.fluidNetOwner() instanceof BlockEntity blockEntity) // [parity V1: fluid net]
                        || blockEntity.getLevel() != level || !this.key.equals(networkKey(level, blockEntity.getBlockPos())));
                users = List.copyOf(this.portUsers);
            }
            Map<EndpointCursorKey, Integer> trackers = new HashMap<>();
            Map<EndpointCursorKey, int[]> stats = new HashMap<>();
            for (PortNet net : portNets(level, components, componentIndex, pipeNodes, users)) { // [parity U1: fluid ports]
                HbmFluidDefinition type = net.type();
                NetEndpoints endpoints = netEndpoints(level, type, net.pipes(), net.users(), net.neighbours());
                long tracker = HbmFluidNetMK2Transfer.update(endpoints.providers(), endpoints.receivers());
                for (NetEndpoint endpoint : endpoints.touched()) {
                    endpoint.finish();
                }
                int[] stat = new int[]{net.pipes().size() + endpoints.touched().size(),
                        endpoints.receivers().size(), endpoints.providers().size()};
                for (PipeComponent component : net.components()) {
                    EndpointCursorKey key = cursorKey(type, component.pipes());
                    stats.put(key, stat);
                    if (tracker > 0) {
                        trackers.merge(key, (int) Math.min(Integer.MAX_VALUE, tracker), Integer::sum);
                    }
                }
            }
            synchronized (this) {
                this.netStats = Map.copyOf(stats);
                this.lastTransfers.clear();
                this.lastTransfers.putAll(this.currentTransfers);
                this.currentTransfers.clear();
                trackers.forEach((key, amount) -> this.lastTransfers.merge(key, amount, Integer::sum));
            }
        }

        /**
         * [parity U1: fluid ports] UniNodespace#checkNodeConnection for TilePort nodes: a port connects to a duct
         * at its connection position, or to another machine's port whose connection points back at it
         * (UniNodespace.checkConnection). Duct components, machines and non-HBM capability blocks at a port
         * connection that end up linked form one FluidNetMK2 per fluid type.
         */
        private List<PortNet> portNets(Level level, Set<PipeComponent> components,
                                       Map<PipeComponentKey, PipeComponent> componentIndex,
                                       Map<BlockPos, PipeNode> pipeNodes, List<HbmFluidNetUser> users) {
            Map<String, List<PipeComponent>> componentsByType = new java.util.LinkedHashMap<>();
            for (PipeComponent component : components) {
                componentsByType.computeIfAbsent(component.typeName(), ignored -> new ArrayList<>()).add(component);
            }
            Map<String, List<HbmFluidNetUser>> usersByType = new java.util.LinkedHashMap<>();
            for (HbmFluidNetUser user : users) {
                Set<HbmFluidDefinition> types = new java.util.LinkedHashSet<>();
                for (HbmFluidTank tank : user.fluidReceivingTanks()) types.add(tank.type());
                for (HbmFluidTank tank : user.fluidSendingTanks()) types.add(tank.type());
                for (HbmFluidDefinition type : types) {
                    if (type == null || type.isNone()) continue;
                    List<HbmFluidNetUser.FluidPort> ports = user.fluidPorts(type);
                    if (ports != null && !ports.isEmpty()) {
                        usersByType.computeIfAbsent(type.name(), ignored -> new ArrayList<>()).add(user);
                    }
                }
            }
            Set<String> typeNames = new java.util.LinkedHashSet<>(componentsByType.keySet());
            typeNames.addAll(usersByType.keySet());
            List<PortNet> nets = new ArrayList<>();
            for (String typeName : typeNames) {
                HbmFluidDefinition type = HbmFluids.byName(typeName).orElse(HbmFluids.none());
                if (type.isNone()) continue;
                List<PipeComponent> typeComponents = componentsByType.getOrDefault(typeName, List.of());
                Map<Object, Object> parent = new HashMap<>();
                for (PipeComponent component : typeComponents) parent.put(component, component);
                // one GenNode per port group (role) of a machine; in and out ports of one tank type share it
                Map<NodeKey, List<HbmFluidNetUser.FluidPort>> portsOf = new java.util.LinkedHashMap<>();
                for (HbmFluidNetUser user : usersByType.getOrDefault(typeName, List.of())) {
                    for (HbmFluidNetUser.FluidPort port : user.fluidPorts(type)) {
                        portsOf.computeIfAbsent(new NodeKey(user, port.role()), ignored -> new ArrayList<>()).add(port);
                    }
                }
                Map<PortKey, NodeKey> owners = new HashMap<>();
                Map<BlockPos, NodeKey> positionOwners = new HashMap<>();
                for (Map.Entry<NodeKey, List<HbmFluidNetUser.FluidPort>> entry : portsOf.entrySet()) {
                    NodeKey node = entry.getKey();
                    parent.put(node, node);
                    for (HbmFluidNetUser.FluidPort port : entry.getValue()) owners.putIfAbsent(new PortKey(port.pos(), port.dir()), node);
                }
                for (Map.Entry<NodeKey, List<HbmFluidNetUser.FluidPort>> entry : portsOf.entrySet()) {
                    NodeKey node = entry.getKey();
                    for (HbmFluidNetUser.FluidPort port : entry.getValue()) {
                        // TilePort#enableIfMissing reuses a node already at the port position (duct or other port)
                        PipeComponent shared = componentIndex.get(new PipeComponentKey(port.pos(), typeName));
                        if (shared != null) union(parent, node, shared);
                        NodeKey previous = positionOwners.putIfAbsent(port.pos(), node);
                        if (previous != null && previous.user() != node.user()) union(parent, node, previous);
                    }
                }
                Map<NodeKey, List<Endpoint>> neighbours = new HashMap<>();
                Map<NodeKey, List<NodeKey>> legacyUsers = new HashMap<>();
                for (Map.Entry<NodeKey, List<HbmFluidNetUser.FluidPort>> entry : portsOf.entrySet()) {
                    NodeKey node = entry.getKey();
                    for (HbmFluidNetUser.FluidPort port : entry.getValue()) {
                        BlockPos target = port.connection();
                        Direction back = port.dir().getOpposite();
                        if (!sameFluidSpace(level, port.pos(), target)) continue;
                        PipeComponent component = componentIndex.get(new PipeComponentKey(target, typeName));
                        if (component != null) {
                            PipeNode pipe = pipeNodes.get(target);
                            if (pipe != null && pipe.directions().contains(back)) union(parent, node, component);
                            continue;
                        }
                        NodeKey other = owners.get(new PortKey(target, back));
                        if (other != null) {
                            if (other.user() != node.user()) union(parent, node, other);
                            continue;
                        }
                        if (!level.isLoaded(target) || level.getBlockEntity(target) instanceof FluidPipeBlockEntity
                                || !fluidEndpointMatchesNetwork(level, target, this.key)) {
                            continue;
                        }
                        HbmFluidNetUser legacy = netUser(level, target);
                        if (legacy != null && legacy.fluidPorts(type) != null) {
                            continue; // its port there does not point back
                        }
                        IFluidHandler handler = fluidHandler(level, target, back, "port neighbour discovery");
                        if (handler != null && legacy != null) {
                            // MK2 machine without port nodes: its capability side stands for the port facing back
                            legacyUsers.computeIfAbsent(node, ignored -> new ArrayList<>())
                                    .add(new NodeKey(legacy, HbmFluidNetUser.PortRole.BOTH));
                        } else if (handler != null) {
                            neighbours.computeIfAbsent(node, ignored -> new ArrayList<>())
                                    .add(new Endpoint(target.immutable(), back, handler));
                        }
                    }
                }
                Map<Object, PortNetBuilder> groups = new java.util.LinkedHashMap<>();
                for (PipeComponent component : typeComponents) {
                    PortNetBuilder builder = groups.computeIfAbsent(find(parent, component), ignored -> new PortNetBuilder());
                    builder.components.add(component);
                    builder.pipes.addAll(component.pipes());
                }
                for (NodeKey node : portsOf.keySet()) {
                    PortNetBuilder builder = groups.computeIfAbsent(find(parent, node), ignored -> new PortNetBuilder());
                    builder.users.add(node);
                    builder.neighbours.addAll(neighbours.getOrDefault(node, List.of()));
                    for (NodeKey legacy : legacyUsers.getOrDefault(node, List.of())) {
                        if (!builder.users.contains(legacy)) builder.users.add(legacy);
                    }
                }
                for (PortNetBuilder builder : groups.values()) {
                    nets.add(new PortNet(type, Set.copyOf(builder.pipes), List.copyOf(builder.components),
                            List.copyOf(builder.users), List.copyOf(builder.neighbours)));
                }
            }
            return nets;
        }

        private void pruneInvalidPipes(Level level) {
            List<BlockPos> removed = new ArrayList<>();
            for (BlockPos pos : this.pipes.keySet()) {
                if (!this.key.equals(networkKey(level, pos))
                        || !mobileFluidNodeAllowed(level, pos)
                        || !(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity)) {
                    removed.add(pos);
                }
            }
            if (!removed.isEmpty()) {
                for (BlockPos pos : removed) {
                    this.pipes.remove(pos);
                }
                markDirty();
            }
        }

        private void markDirty() {
            this.dirty = true;
            this.graphVersion++;
            this.componentsByPipe = Map.of();
        }

        private Map<EndpointCursorKey, Integer> cursorMap(CursorKind kind) {
            return kind == CursorKind.FILL ? this.fillCursors : this.drainCursors;
        }

        /** [parity T1: fluid net] topology is rebuilt synchronously (UniNodespace#checkNodeConnection). */
        private void tick() {
            if (this.dirty) {
                this.dirty = false;
                try {
                    this.componentsByPipe = solve(new SolveSnapshot(this.graphVersion, Map.copyOf(this.pipes))).componentsByPipe();
                } catch (RuntimeException failure) {
                    this.componentsByPipe = Map.of();
                    AtomicLegacy.LOGGER.error("Fluid-network topology solve failed for {}; routes discarded", this.key, failure);
                }
            }
        }
    }

    private static SolveResult solve(SolveSnapshot snapshot) {
        Map<PipeComponentKey, PipeComponent> componentsByPipe = new HashMap<>();
        Set<PipeComponentKey> visited = new HashSet<>();

        for (PipeNode node : snapshot.pipes().values()) {
            if (!node.active()) {
                continue;
            }

            for (String typeName : node.typeNames()) {
                PipeComponentKey startKey = new PipeComponentKey(node.pos(), typeName);
                if (visited.contains(startKey)) {
                    continue;
                }

                List<BlockPos> component = new ArrayList<>();
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(node.pos());

                while (!queue.isEmpty()) { // [parity T1: fluid net] UNINOS nets have no size cap
                    BlockPos current = queue.removeFirst();
                    PipeComponentKey currentKey = new PipeComponentKey(current, typeName);
                    if (!visited.add(currentKey)) {
                        continue;
                    }
                    PipeNode currentNode = snapshot.pipes().get(current);
                    if (currentNode == null || !currentNode.active() || !currentNode.typeNames().contains(typeName)) {
                        continue;
                    }
                    component.add(current);

                    for (Direction direction : Direction.values()) {
                        BlockPos next = current.relative(direction).immutable();
                        PipeComponentKey nextKey = new PipeComponentKey(next, typeName);
                        if (!visited.contains(nextKey)) {
                            PipeNode nextNode = snapshot.pipes().get(next);
                            if (nextNode != null && nextNode.active() && nextNode.typeNames().contains(typeName)
                                    && currentNode.directions().contains(direction)
                                    && nextNode.directions().contains(direction.getOpposite())) {
                                queue.addLast(next);
                            }
                        }
                    }
                    for (BlockPos link : currentNode.links()) {
                        PipeNode nextNode = snapshot.pipes().get(link);
                        PipeComponentKey nextKey = new PipeComponentKey(link, typeName);
                        if (!visited.contains(nextKey)
                                && nextNode != null
                                && nextNode.active()
                                && nextNode.typeNames().contains(typeName)
                                && nextNode.links().contains(current)) {
                            queue.addLast(link);
                        }
                    }
                }

                if (!component.isEmpty()) {
                    Set<BlockPos> immutablePipes = Set.copyOf(component);
                    PipeComponent pipeComponent = new PipeComponent(typeName, immutablePipes);
                    for (BlockPos pos : immutablePipes) {
                        componentsByPipe.put(new PipeComponentKey(pos, typeName), pipeComponent);
                    }
                }
            }
        }

        return new SolveResult(snapshot.graphVersion(), Map.copyOf(componentsByPipe));
    }

    private record PipeNode(BlockPos pos, Set<String> typeNames, boolean open, Set<BlockPos> links,
                            Set<Direction> directions) {
        private boolean active() {
            return open && !typeNames.isEmpty();
        }
    }

    private record PipeComponentKey(BlockPos pos, String typeName) {
    }

    /** [parity U1: fluid ports] one FluidNetMK2: duct components, port machines, capability blocks at ports */
    private record PortNet(HbmFluidDefinition type, Set<BlockPos> pipes, List<PipeComponent> components,
                           List<NodeKey> users, List<Endpoint> neighbours) {
    }

    /** [parity U1: fluid ports] TilePort GenNode of one machine: a port group with its subscription role */
    private record NodeKey(HbmFluidNetUser user, HbmFluidNetUser.PortRole role) {
    }

    private record PortKey(BlockPos pos, Direction dir) {
    }

    private static final class PortNetBuilder {
        private final Set<BlockPos> pipes = new HashSet<>();
        private final List<PipeComponent> components = new ArrayList<>();
        private final List<NodeKey> users = new ArrayList<>();
        private final List<Endpoint> neighbours = new ArrayList<>();
    }

    private static Object find(Map<Object, Object> parent, Object node) {
        Object root = node;
        while (parent.get(root) != root) root = parent.get(root);
        while (parent.get(node) != root) {
            Object next = parent.get(node);
            parent.put(node, root);
            node = next;
        }
        return root;
    }

    private static void union(Map<Object, Object> parent, Object first, Object second) {
        Object a = find(parent, first);
        Object b = find(parent, second);
        if (a != b) parent.put(b, a);
    }

    private record PipeComponent(String typeName, Set<BlockPos> pipes) {
    }

    private record SolveSnapshot(long graphVersion, Map<BlockPos, PipeNode> pipes) {
    }

    private record SolveResult(long graphVersion, Map<PipeComponentKey, PipeComponent> componentsByPipe) {
    }
}
