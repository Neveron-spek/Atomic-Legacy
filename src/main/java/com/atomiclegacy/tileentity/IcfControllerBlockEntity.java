package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.IcfControllerBlock;
import com.atomiclegacy.blocks.IcfLaserComponentBlock;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact graph assembly and one-tick discharge logic from TileEntityICFController. */
public final class IcfControllerBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final int MAX_SIZE = 1024;
    public static final long CAPACITOR_POWER = 2_500_000L;
    public static final long TURBO_POWER = 5_000_000L;

    private long power;
    private int laserLength;
    private int cellCount;
    private int emitterCount;
    private int capacitorCount;
    private int turbochargerCount;
    private List<BlockPos> ports = List.of();
    private boolean assembled;

    public IcfControllerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfControllerBlockEntity controller) {
        if (level.isClientSide) {
            return;
        }
        controller.tickServer(level);
    }

    public boolean assembled() {
        return assembled;
    }

    public void setAssembled(boolean assembled) {
        if (this.assembled != assembled) {
            this.assembled = assembled;
            markDirtyAndSync();
            if (level != null) {
                PowerNetworkManager.markDirty(level);
            }
        }
    }

    /** Rewrites assembled ICF port positions after a Create contraption move. */
    public void transformAssemblyLinks(java.util.function.UnaryOperator<BlockPos> transform) {
        if (!assembled) {
            return;
        }
        this.ports = this.ports.stream()
                .map(pos -> transform.apply(pos).immutable())
                .toList();
        setChanged();
    }

    public long power() {
        return Math.min(power, maxPower());
    }

    public int laserLength() {
        return laserLength;
    }

    public int capacitorCount() {
        return capacitorCount;
    }

    public int turbochargerCount() {
        return turbochargerCount;
    }

    public long maxPower() {
        return (long) (Math.sqrt(capacitorCount) * CAPACITOR_POWER
                + Math.sqrt(Math.min(turbochargerCount, capacitorCount)) * TURBO_POWER);
    }

    public void assemble(Player player) {
        if (this.level == null || this.level.isClientSide || this.assembled) {
            return;
        }
        Direction backward = facing().getOpposite();
        BlockPos first = this.worldPosition.relative(backward);
        Map<BlockPos, Integer> found = new HashMap<>();
        // [parity A3: icf_controller] MachineICFController#assemble: the controller is put into the assembly map before
        // the flood fill (so a cell next to it does not report "Non-laser block" and it counts towards maxSize).
        found.put(this.worldPosition.immutable(), 0);
        Set<BlockPos> casing = new HashSet<>();
        Set<BlockPos> foundPorts = new HashSet<>();
        Set<BlockPos> cells = new HashSet<>();
        Set<BlockPos> emitters = new HashSet<>();
        Set<BlockPos> capacitors = new HashSet<>();
        Set<BlockPos> turbos = new HashSet<>();
        // [parity b2: assembly markers] MachineICFController#assemble: the flood fill keeps going after an error
        // and every offending position gets a red world marker (sendError) instead of a text message.
        List<Map.Entry<BlockPos, String>> errors = new ArrayList<>();
        floodFill(first, found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
        found.remove(this.worldPosition);
        if (!errors.isEmpty()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                for (Map.Entry<BlockPos, String> error : errors) {
                    com.atomiclegacy.network.MarkerPayload.sendError(serverPlayer, error.getKey(), error.getValue());
                }
            }
            return;
        }

        for (Map.Entry<BlockPos, Integer> entry : found.entrySet()) {
            BlockPos pos = entry.getKey();
            int variant = entry.getValue();
            BlockState assembledState = HbmBlocks.ICF_BLOCK.get().defaultBlockState()
                    .setValue(com.atomiclegacy.blocks.LegacyVariantBlock.VARIANT,
                            variant == IcfLaserComponentBlock.Part.PORT.ordinal() ? 1 : 0);
            this.level.setBlock(pos, assembledState, Block.UPDATE_ALL);
            if (this.level.getBlockEntity(pos) instanceof IcfAssembledLaserBlockEntity assembledPart) {
                assembledPart.configure(HbmBlocks.ICF_LASER_COMPONENT.get(), variant, this.worldPosition);
            }
        }

        setup(foundPorts, cells, emitters, capacitors, turbos);
        this.assembled = true;
        markDirtyAndSync();
        PowerNetworkManager.markDirty(this.level);
    }

    /** MachineICFController#floodFill, same visiting order (x+1, x-1, y+1, y-1, z+1, z-1); the controller is pre-seeded. */
    private void floodFill(
            BlockPos pos,
            Map<BlockPos, Integer> found,
            Set<BlockPos> casing,
            Set<BlockPos> foundPorts,
            Set<BlockPos> cells,
            Set<BlockPos> emitters,
            Set<BlockPos> capacitors,
            Set<BlockPos> turbos,
            List<Map.Entry<BlockPos, String>> errors
    ) {
        if (pos.equals(this.worldPosition) || found.containsKey(pos)) {
            return;
        }
        if (found.size() + 1 >= MAX_SIZE) {
            errors.add(Map.entry(pos.immutable(), "Max size exceeded"));
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(HbmBlocks.ICF_LASER_COMPONENT.get())) {
            errors.add(Map.entry(pos.immutable(), "Non-laser block"));
            return;
        }
        int variant = state.getValue(com.atomiclegacy.blocks.LegacyVariantBlock.VARIANT);
        IcfLaserComponentBlock.Part part = IcfLaserComponentBlock.Part.byVariant(variant);
        BlockPos key = pos.immutable();
        found.put(key, variant);
        switch (part) {
            case CASING -> { casing.add(key); return; }
            case PORT -> { foundPorts.add(key); return; }
            case CELL -> cells.add(key);
            case EMITTER -> emitters.add(key);
            case CAPACITOR -> capacitors.add(key);
            case TURBO -> turbos.add(key);
        }
        floodFill(pos.east(), found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
        floodFill(pos.west(), found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
        floodFill(pos.above(), found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
        floodFill(pos.below(), found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
        floodFill(pos.south(), found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
        floodFill(pos.north(), found, casing, foundPorts, cells, emitters, capacitors, turbos, errors);
    }

    private void setup(Set<BlockPos> foundPorts, Set<BlockPos> cells, Set<BlockPos> emitters,
                       Set<BlockPos> capacitors, Set<BlockPos> turbos) {
        this.cellCount = 0;
        this.emitterCount = 0;
        this.capacitorCount = 0;
        this.turbochargerCount = 0;
        Direction backward = facing().getOpposite();
        Set<BlockPos> validCells = new HashSet<>();
        Set<BlockPos> validEmitters = new HashSet<>();
        Set<BlockPos> validCapacitors = new HashSet<>();

        for (int distance = 1; cells.contains(worldPosition.relative(backward, distance)); distance++) {
            BlockPos cell = worldPosition.relative(backward, distance).immutable();
            validCells.add(cell);
            cellCount++;
        }
        for (BlockPos emitter : emitters) {
            if (touches(emitter, validCells)) {
                validEmitters.add(emitter);
                emitterCount++;
            }
        }
        for (BlockPos capacitor : capacitors) {
            if (touches(capacitor, validEmitters)) {
                validCapacitors.add(capacitor);
                capacitorCount++;
            }
        }
        for (BlockPos turbo : turbos) {
            if (touches(turbo, validCapacitors)) {
                turbochargerCount++;
            }
        }
        this.ports = foundPorts.stream()
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();
        this.power = Math.min(this.power, maxPower());
    }

    private static boolean touches(BlockPos pos, Set<BlockPos> targets) {
        for (Direction direction : Direction.values()) {
            if (targets.contains(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private void tickServer(Level level) {
        PowerNetworkManager.tickFromEndpoint(level, this);
        if (!assembled || power <= 0L) {
            boolean beamStopped = laserLength != 0;
            laserLength = 0;
            if (beamStopped || level.getGameTime() % 10L == 0L) {
                markDirtyAndSync();
            }
            return;
        }

        Direction forward = facing();
        for (int distance = 1; distance < 50; distance++) {
            this.laserLength = distance;
            BlockPos hit = this.worldPosition.relative(forward, distance);
            BlockEntity hitEntity = level.getBlockEntity(hit);
            IcfCoreBlockEntity core = hitEntity instanceof IcfCoreBlockEntity direct ? direct
                    : hitEntity instanceof MachineDummyBlockEntity dummy && dummy.core() instanceof IcfCoreBlockEntity reactor ? reactor
                    : null;
            // [parity A3: icf_controller] the beam only feeds a TileEntityICF whose core is at (i + 8) along the beam, y - 3.
            if (core != null && core.getBlockPos().equals(this.worldPosition.relative(forward, distance + 8).below(3))) {
                core.receiveLaser(power(), maxPower());
                break;
            }
            BlockState state = level.getBlockState(hit);
            if (!state.isAir()) {
                if (state.getExplosionResistance(level, hit, null) < 6000.0F) {
                    level.destroyBlock(hit, false);
                }
                break;
            }
        }

        AABB beam = beamBounds(forward, laserLength);
        for (Entity entity : level.getEntities(null, beam)) {
            entity.hurt(level.damageSources().inFire(), 50.0F);
            entity.igniteForSeconds(5.0F);
        }
        if (level instanceof ServerLevel serverLevel && level.random.nextInt(5) == 0) {
            Direction side = forward.getClockWise();
            double offsetXZ = level.random.nextDouble() * 0.25D - 0.125D;
            double offsetY = level.random.nextDouble() * 0.25D - 0.125D;
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F),
                    worldPosition.getX() + 0.5D + forward.getStepX() * 0.55D + side.getStepX() * offsetXZ,
                    worldPosition.getY() + 0.5D + offsetY,
                    worldPosition.getZ() + 0.5D + forward.getStepZ() * 0.55D + side.getStepZ() * offsetXZ,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        power = 0L;
        markDirtyAndSync();
    }

    /** [parity A3: icf_controller] min/max of the block coordinates first, then +0.2 / +0.8 as in the original. */
    private AABB beamBounds(Direction direction, int distance) {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        int ex = x + direction.getStepX() * distance, ey = y + direction.getStepY() * distance, ez = z + direction.getStepZ() * distance;
        return new AABB(Math.min(x, ex) + 0.2D, Math.min(y, ey) + 0.2D, Math.min(z, ez) + 0.2D,
                Math.max(x, ex) + 0.8D, Math.max(y, ey) + 0.8D, Math.max(z, ez) + 0.8D);
    }

    private Direction facing() {
        return getBlockState().getValue(IcfControllerBlock.FACING);
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return ports;
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return assembled && ports.contains(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return assembled ? Math.max(0L, maxPower() - power) : 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (receivedInput > 0L) {
            power = Math.min(maxPower(), power + receivedInput);
            markDirtyAndSync();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(power() + " / " + maxPower() + " HE");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", power);
        tag.putBoolean("Assembled", assembled);
        tag.putInt("CellCount", cellCount);
        tag.putInt("EmitterCount", emitterCount);
        tag.putInt("CapacitorCount", capacitorCount);
        tag.putInt("TurbochargerCount", turbochargerCount);
        tag.putInt("LaserLength", laserLength);
        tag.putInt("PortCount", ports.size());
        for (int index = 0; index < ports.size(); index++) {
            BlockPos port = ports.get(index);
            tag.putIntArray("Port" + index, new int[]{port.getX(), port.getY(), port.getZ()});
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = Math.max(0L, tag.getLong("Power"));
        assembled = tag.getBoolean("Assembled");
        cellCount = Math.max(0, tag.getInt("CellCount"));
        emitterCount = Math.max(0, tag.getInt("EmitterCount"));
        capacitorCount = Math.max(0, tag.getInt("CapacitorCount"));
        turbochargerCount = Math.max(0, tag.getInt("TurbochargerCount"));
        laserLength = Math.max(0, tag.getInt("LaserLength"));
        ArrayList<BlockPos> loadedPorts = new ArrayList<>();
        for (int index = 0; index < Math.min(MAX_SIZE, tag.getInt("PortCount")); index++) {
            int[] values = tag.getIntArray("Port" + index);
            if (values.length == 3) {
                loadedPorts.add(new BlockPos(values[0], values[1], values[2]));
            }
        }
        ports = List.copyOf(loadedPorts);
        power = Math.min(power, maxPower());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
