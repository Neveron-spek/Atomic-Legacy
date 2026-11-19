package com.atomiclegacy.pneumatic;

import com.atomiclegacy.blocks.PneumaticStorageBlock;
import com.atomiclegacy.blocks.PneumaticTubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Event-invalidated pneumatic topology cache.  A connected component is only
 * traversed after placement/removal/chunk-load invalidates it; ordinary item
 * operations reuse the component and inspect only its storage endpoints.
 */
public final class PneumaticNetworkManager {
    private static final Map<ServerLevel, LevelState> STATES = new WeakHashMap<>();
    private static long nextRevision = 1L;

    private PneumaticNetworkManager() {
    }

    public record Entry(ItemStack stack, long amount) {
        public Entry {
            stack = stack.copyWithCount(1);
            amount = Math.max(0L, amount);
        }
    }

    public record Snapshot(long revision, List<Entry> entries) {
    }

    public static Snapshot snapshot(ServerLevel level, BlockPos accessor) {
        Component component = component(level, accessor);
        List<Entry> entries = new ArrayList<>();
        for (PneumaticStorageNode storage : storages(level, component, accessor)) {
            for (int slot = 0; slot < storage.pneumaticSlotCount(); slot++) {
                ItemStack prototype = storage.pneumaticPrototype(slot);
                long amount = storage.pneumaticAmount(slot);
                if (prototype.isEmpty() || amount <= 0L) {
                    continue;
                }
                Entry existing = null;
                for (Entry entry : entries) {
                    if (ItemStack.isSameItemSameComponents(entry.stack(), prototype)) {
                        existing = entry;
                        break;
                    }
                }
                if (existing == null) {
                    entries.add(new Entry(prototype, amount));
                } else {
                    int index = entries.indexOf(existing);
                    entries.set(index, new Entry(existing.stack(), saturatedAdd(existing.amount(), amount)));
                }
            }
        }
        entries.sort(Comparator
                .comparingLong(Entry::amount).reversed()
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString()));
        return new Snapshot(component.revision, List.copyOf(entries));
    }

    /** Returns the quantity that could not be inserted. */
    public static long insert(ServerLevel level, BlockPos accessor, ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0L) {
            return Math.max(0L, amount);
        }
        Component component = component(level, accessor);
        long remaining = amount;
        List<PneumaticStorageNode> storages = storages(level, component, accessor);

        // Match occupied slots first so empty clutter slots do not fragment the
        // same item across the network unnecessarily.
        for (int pass = 0; pass < 2 && remaining > 0L; pass++) {
            for (PneumaticStorageNode storage : storages) {
                for (int slot = 0; slot < storage.pneumaticSlotCount() && remaining > 0L; slot++) {
                    ItemStack prototype = storage.pneumaticPrototype(slot);
                    boolean occupiedMatch = !prototype.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack);
                    if ((pass == 0) != occupiedMatch) {
                        continue;
                    }
                    remaining = storage.pneumaticInsert(slot, stack, remaining, simulate);
                }
            }
        }
        if (!simulate && remaining != amount) {
            component.revision = nextRevision++;
        }
        return remaining;
    }

    /** Returns the quantity actually extracted. */
    public static long extract(ServerLevel level, BlockPos accessor, ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0L) {
            return 0L;
        }
        Component component = component(level, accessor);
        long remaining = amount;
        for (PneumaticStorageNode storage : storages(level, component, accessor)) {
            for (int slot = 0; slot < storage.pneumaticSlotCount() && remaining > 0L; slot++) {
                remaining -= storage.pneumaticExtract(slot, stack, remaining, simulate);
            }
            if (remaining <= 0L) {
                break;
            }
        }
        long extracted = amount - remaining;
        if (!simulate && extracted > 0L) {
            component.revision = nextRevision++;
        }
        return extracted;
    }

    public static long available(ServerLevel level, BlockPos accessor, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        long amount = 0L;
        Component component = component(level, accessor);
        for (PneumaticStorageNode storage : storages(level, component, accessor)) {
            for (int slot = 0; slot < storage.pneumaticSlotCount(); slot++) {
                ItemStack prototype = storage.pneumaticPrototype(slot);
                if (!prototype.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack)) {
                    amount = saturatedAdd(amount, storage.pneumaticAmount(slot));
                }
            }
        }
        return amount;
    }

    public static void markContentsChanged(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelState state = state(serverLevel);
        Component component = state.byPosition.get(pos.asLong());
        if (component != null) {
            component.revision = nextRevision++;
        }
    }

    public static void invalidate(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelState state = state(serverLevel);
        Set<Component> affected = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Component own = state.byPosition.get(pos.asLong());
        if (own != null) {
            affected.add(own);
        }
        for (Direction direction : Direction.values()) {
            Component adjacent = state.byPosition.get(pos.relative(direction).asLong());
            if (adjacent != null) {
                affected.add(adjacent);
            }
        }
        for (Component component : affected) {
            for (long member : component.members) {
                if (state.byPosition.get(member) == component) {
                    state.byPosition.remove(member);
                }
            }
        }
    }

    private static Component component(ServerLevel level, BlockPos origin) {
        LevelState state = state(level);
        Component cached = state.byPosition.get(origin.asLong());
        if (cached != null) {
            return cached;
        }

        Set<Long> members = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            long packed = current.asLong();
            if (members.contains(packed) || !level.hasChunkAt(current) || !isPneumatic(level.getBlockState(current))) {
                continue;
            }
            members.add(packed);
            BlockState currentState = level.getBlockState(current);
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (!members.contains(adjacent.asLong()) && level.hasChunkAt(adjacent)
                        && isPneumatic(level.getBlockState(adjacent))
                        && connectsOn(currentState, direction)
                        && connectsOn(level.getBlockState(adjacent), direction.getOpposite())) { // [parity V2: pipe_anchor_pneumatic]
                    queue.addLast(adjacent.immutable());
                }
            }
            // [parity V2: pipe_anchor_pneumatic] TileEntityPipelineBase.initNode: linked anchors share the node net
            if (level.getBlockEntity(current) instanceof com.atomiclegacy.tileentity.PipeAnchorBlockEntity anchor
                    && anchor.isPneumaticAnchor()) {
                for (BlockPos link : anchor.anchorLinks()) {
                    if (!members.contains(link.asLong()) && level.hasChunkAt(link)
                            && level.getBlockEntity(link) instanceof com.atomiclegacy.tileentity.PipeAnchorBlockEntity other
                            && other.isPneumaticAnchor()) {
                        queue.addLast(link.immutable());
                    }
                }
            }
        }
        Component created = new Component(members, nextRevision++);
        for (long member : members) {
            state.byPosition.put(member, created);
        }
        return created;
    }

    private static List<PneumaticStorageNode> storages(ServerLevel level, Component component, BlockPos accessor) {
        List<PneumaticStorageNode> result = new ArrayList<>();
        for (long packed : component.members) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PneumaticStorageNode storage && storage.pneumaticAvailableFrom(accessor)) {
                result.add(storage);
            }
        }
        result.sort(Comparator.comparingDouble(storage -> storage.pneumaticPosition().distSqr(accessor)));
        return result;
    }

    // ------------------------------------------------------------------------------------------------
    // [parity R2] PneumaticNetwork item transport (send / addReceiver) for compressor and endpoint tubes.
    // ------------------------------------------------------------------------------------------------

    public static final byte SEND_FIRST = 0;
    public static final byte SEND_LAST = 1;
    public static final byte SEND_RANDOM = 2;
    public static final byte RECEIVE_ROBIN = 0;
    public static final byte RECEIVE_RANDOM = 1;
    private static final long TIMEOUT = 1_000L;
    public static final int ITEMS_PER_TRANSFER = 64;
    private static final java.util.Random RAND = new java.util.Random();

    private record Receiver(BlockPos inventoryPos, Direction pipeDir, long timestamp, BlockPos endpoint) {
    }

    public static void addReceiver(ServerLevel level, BlockPos tube, net.minecraft.world.Container inventory,
                                   BlockPos inventoryPos, Direction pipeDir,
                                   com.atomiclegacy.tileentity.PneumaticTubeBlockEntity endpoint) {
        Component component = component(level, tube);
        component.receivers.put(inventoryPos.asLong(), new Receiver(inventoryPos.immutable(), pipeDir, System.currentTimeMillis(), endpoint.getBlockPos()));
    }

    /** Number of receivers currently registered in the tube's network (diagnostics). */
    public static int receiverCount(ServerLevel level, BlockPos tube) {
        return component(level, tube).receivers.size();
    }

    private static boolean badLink(ServerLevel level, Receiver receiver) {
        return !level.isLoaded(receiver.inventoryPos())
                || com.atomiclegacy.tileentity.PneumaticTubeBlockEntity.inventoryAt(level, receiver.inventoryPos()) == null;
    }

    public static boolean send(ServerLevel level, BlockPos tubePos, net.minecraft.world.Container source, BlockPos sourcePos,
                               com.atomiclegacy.tileentity.PneumaticTubeBlockEntity tube, Direction accessDir,
                               int sendOrder, int receiveOrder, int maxRange, int nextReceiver) {
        Component component = component(level, tubePos);
        long timestamp = System.currentTimeMillis();
        component.receivers.values().removeIf(x -> timestamp - x.timestamp() > TIMEOUT || badLink(level, x));
        if (component.receivers.isEmpty()) return false;

        int[] sourceSlotAccess = slotAccess(source, accessDir);
        if (sendOrder == SEND_LAST) reverse(sourceSlotAccess);
        if (sendOrder == SEND_RANDOM) shuffle(sourceSlotAccess);
        net.minecraft.world.WorldlyContainer sidedSource = source instanceof net.minecraft.world.WorldlyContainer sided ? sided : null;

        boolean hasItem = false;
        for (int i : sourceSlotAccess) {
            ItemStack stack = source.getItem(i);
            if (!stack.isEmpty()) {
                if (sidedSource != null && !sidedSource.canTakeItemThroughFace(i, stack, accessDir)) continue;
                if (!tube.passes(stack)) continue;
                hasItem = true;
                break;
            }
        }
        if (!hasItem) return false;

        List<Receiver> receiverList = new ArrayList<>(component.receivers.values());
        BlockPos origin = tube.getBlockPos();
        if (receiveOrder == RECEIVE_ROBIN) receiverList.sort((o1, o2) -> {
            int d1 = (int) o1.inventoryPos().distSqr(origin);
            int d2 = (int) o2.inventoryPos().distSqr(origin);
            if (d1 == d2) {
                return com.atomiclegacy.tileentity.PneumaticTubeBlockEntity.getIdentifier(o1.inventoryPos().getX(), o1.inventoryPos().getY(), o1.inventoryPos().getZ())
                        - com.atomiclegacy.tileentity.PneumaticTubeBlockEntity.getIdentifier(o2.inventoryPos().getX(), o2.inventoryPos().getY(), o2.inventoryPos().getZ());
            }
            return d1 - d2;
        });
        if (receiveOrder == RECEIVE_RANDOM) java.util.Collections.shuffle(receiverList);

        int attempts = 0;
        int maxAttempts = Math.min(receiverList.size(), 5);
        while (attempts < maxAttempts) {
            int index = receiveOrder == RECEIVE_ROBIN ? Math.floorMod(nextReceiver + attempts, receiverList.size()) : attempts;
            Receiver candidate = receiverList.get(index);
            if (candidate.inventoryPos().equals(sourcePos)) {
                attempts++;
                continue;
            }
            net.minecraft.world.Container dest = badLink(level, candidate) ? null
                    : com.atomiclegacy.tileentity.PneumaticTubeBlockEntity.inventoryAt(level, candidate.inventoryPos());
            if (dest == null || dest == source) {
                component.receivers.remove(candidate.inventoryPos().asLong());
                attempts++;
                continue;
            }
            if (sourcePos.distSqr(candidate.inventoryPos()) > (double) maxRange * maxRange) {
                attempts++;
                continue;
            }
            com.atomiclegacy.tileentity.PneumaticTubeBlockEntity endpointTile =
                    level.getBlockEntity(candidate.endpoint()) instanceof com.atomiclegacy.tileentity.PneumaticTubeBlockEntity t ? t : null;
            net.minecraft.world.WorldlyContainer sidedDest = dest instanceof net.minecraft.world.WorldlyContainer sided ? sided : null;
            Direction destSide = candidate.pipeDir().getOpposite();
            int[] destSlotAccess = slotAccess(dest, destSide);
            int itemsLeftToSend = ITEMS_PER_TRANSFER;
            int itemHardCap = level.getBlockState(candidate.inventoryPos()).is(com.atomiclegacy.registry.HbmBlocks.MACHINE_AUTOCRAFTER.get())
                    ? 1 : ITEMS_PER_TRANSFER;
            boolean didSomething = false;

            for (int sourceIndex : sourceSlotAccess) {
                ItemStack sourceStack = source.getItem(sourceIndex);
                if (sourceStack.isEmpty()) continue;
                if (sidedSource != null && !sidedSource.canTakeItemThroughFace(sourceIndex, sourceStack, accessDir)) continue;
                if (!tube.passes(sourceStack)) continue;
                if (endpointTile != null && endpointTile != tube && !endpointTile.passes(sourceStack)) continue;
                int proportionalValue = net.minecraft.util.Mth.clamp(64 / sourceStack.getMaxStackSize(), 1, 64);

                for (int destIndex : destSlotAccess) {
                    ItemStack destStack = dest.getItem(destIndex);
                    if (destStack.isEmpty()) continue;
                    if (!ItemStack.isSameItemSameComponents(sourceStack, destStack)) continue;
                    int toMove = min(sourceStack.getCount(), destStack.getMaxStackSize() - destStack.getCount(),
                            dest.getMaxStackSize() - destStack.getCount(), itemsLeftToSend / proportionalValue, itemHardCap);
                    if (toMove <= 0) continue;
                    ItemStack checkStack = destStack.copyWithCount(destStack.getCount() + toMove);
                    if (!dest.canPlaceItem(destIndex, checkStack)) continue;
                    if (sidedDest != null && !sidedDest.canPlaceItemThroughFace(destIndex, checkStack, destSide)) continue;
                    sourceStack.shrink(toMove);
                    if (sourceStack.isEmpty()) source.setItem(sourceIndex, ItemStack.EMPTY);
                    destStack.grow(toMove);
                    itemsLeftToSend -= toMove * proportionalValue;
                    didSomething = true;
                    if (itemsLeftToSend <= 0) break;
                }

                if (itemsLeftToSend > 0 && !sourceStack.isEmpty()) {
                    for (int destIndex : destSlotAccess) {
                        if (!dest.getItem(destIndex).isEmpty()) continue;
                        int toMove = min(sourceStack.getCount(), dest.getMaxStackSize(), itemsLeftToSend / proportionalValue, itemHardCap);
                        if (toMove <= 0) continue;
                        ItemStack checkStack = sourceStack.copyWithCount(toMove);
                        if (!dest.canPlaceItem(destIndex, checkStack)) continue;
                        if (sidedDest != null && !sidedDest.canPlaceItemThroughFace(destIndex, checkStack, destSide)) continue;
                        ItemStack newStack = sourceStack.copyWithCount(toMove);
                        sourceStack.shrink(toMove);
                        if (sourceStack.isEmpty()) source.setItem(sourceIndex, ItemStack.EMPTY);
                        dest.setItem(destIndex, newStack);
                        itemsLeftToSend -= toMove * proportionalValue;
                        didSomething = true;
                        if (itemsLeftToSend <= 0) break;
                    }
                }
                if (itemsLeftToSend <= 0) break;
            }

            if (didSomething) {
                source.setChanged();
                dest.setChanged();
                return true;
            }
            attempts++;
        }
        return false;
    }

    private static int[] slotAccess(net.minecraft.world.Container inventory, Direction dir) {
        if (inventory instanceof net.minecraft.world.WorldlyContainer sided) {
            int[] access = sided.getSlotsForFace(dir);
            return java.util.Arrays.copyOf(access, access.length);
        }
        int[] access = new int[inventory.getContainerSize()];
        for (int i = 0; i < access.length; i++) access[i] = i;
        return access;
    }

    private static int min(int... values) {
        int result = values[0];
        for (int value : values) result = Math.min(result, value);
        return result;
    }

    private static void reverse(int[] array) {
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            int t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
    }

    private static void shuffle(int[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = RAND.nextInt(i + 1);
            int t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
    }

    private static boolean isPneumatic(BlockState state) {
        return state.getBlock() instanceof PneumaticTubeBlock || state.getBlock() instanceof PneumaticStorageBlock
                || state.getBlock() instanceof com.atomiclegacy.blocks.PipeAnchorBlock anchor && anchor.pneumatic(); // [parity V2: pipe_anchor_pneumatic]
    }

    /**
     * [parity V2: pipe_anchor_pneumatic] UniNodespace.checkConnection: tubes and storages have six standard
     * connections, a pneumatic anchor only the one at getOppositeDir() (its IPneumaticConnector side).
     */
    public static boolean connectsOn(BlockState state, Direction side) {
        if (state.getBlock() instanceof com.atomiclegacy.blocks.PipeAnchorBlock anchor) {
            return anchor.pneumatic() && state.getValue(com.atomiclegacy.blocks.PipeAnchorBlock.FACING).getOpposite() == side;
        }
        return state.getBlock() instanceof PneumaticTubeBlock || state.getBlock() instanceof PneumaticStorageBlock;
    }

    /** [parity S2: commands] CommandReapNetworks: drops the cached pneumatic nets; they are rebuilt on demand. */
    public static int reapAll(net.minecraft.server.MinecraftServer server) {
        synchronized (STATES) {
            int count = 0;
            for (Map.Entry<ServerLevel, LevelState> entry : STATES.entrySet()) if (entry.getKey().getServer() == server) count += new java.util.HashSet<>(entry.getValue().byPosition.values()).size();
            STATES.keySet().removeIf(level -> level.getServer() == server);
            return count;
        }
    }

    private static LevelState state(ServerLevel level) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(level, ignored -> new LevelState());
        }
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static final class LevelState {
        private final Map<Long, Component> byPosition = new HashMap<>();
    }

    private static final class Component {
        private final Set<Long> members;
        private long revision;
        /** [parity R2] PneumaticNetwork.receivers: inventory position -> (pipe direction, timestamp, endpoint). */
        private final Map<Long, Receiver> receivers = new java.util.LinkedHashMap<>();

        private Component(Set<Long> members, long revision) {
            this.members = Set.copyOf(members);
            this.revision = revision;
        }
    }
}
