package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.PwrBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.fluid.HbmThermalConversions;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.items.PwrFuelItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.inventory.container.PwrMenu;
import com.atomiclegacy.rtty.RttyInfo;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.rtty.RttyValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PwrControllerBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider,
        RttyValueProvider, RttyInteractive, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_IDENTIFIER = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 16;
    private static final long CORE_HEAT_CAPACITY_BASE = 10_000_000L;
    private static final long HULL_HEAT_CAPACITY_BASE = 10_000_000L;
    private static final int MAX_ASSEMBLY_SIZE = 4096;

    private final net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank coolant = new HbmFluidTank(HbmFluids.byName("coolant").orElse(HbmFluids.none()), 128_000);
    private final HbmFluidTank hotCoolant = new HbmFluidTank(HbmFluids.byName("coolant_hot").orElse(HbmFluids.none()), 128_000);
    private final List<BlockPos> assembledParts = new ArrayList<>();
    private final List<BlockPos> ports = new ArrayList<>();
    private final List<BlockPos> rods = new ArrayList<>();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> clampInt(coreHeat);
                case 1 -> clampInt(hullHeat);
                case 2 -> clampInt(coreHeatCapacity);
                case 3 -> (int) Math.round(flux);
                case 4 -> (int) Math.round(progress);
                case 5 -> (int) Math.round(processTime);
                case 6 -> typeLoaded;
                case 7 -> amountLoaded;
                case 8 -> rodCount;
                case 9 -> (int) Math.round(rodLevel * 100.0D);
                case 10 -> (int) Math.round(rodTarget * 100.0D);
                case 11 -> coolant.type().oldId();
                case 12 -> coolant.amount();
                case 13 -> hotCoolant.type().oldId();
                case 14 -> hotCoolant.amount();
                case 15 -> assembled ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 9) rodLevel = value / 100.0D;
            if (index == 10) rodTarget = value / 100.0D;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private long coreHeat;
    private long coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;
    private long hullHeat;
    private double flux;
    private double rodLevel = 100.0D;
    private double rodTarget = 100.0D;
    private int typeLoaded = -1;
    private int amountLoaded;
    private double progress;
    private double processTime;
    private int rodCount;
    private int connections;
    private int connectionsControlled;
    private int heatexCount;
    private int heatsinkCount;
    private int channelCount;
    private int sourceCount;
    private int unloadDelay;
    private boolean assembled;

    public PwrControllerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PWR_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PwrControllerBlockEntity reactor) {
        if (level.isClientSide) {
            return;
        }
        reactor.tickServer();
    }

    private void tickServer() {
        // [parity M2: PWR] TileEntityPWRController#updateEntity order: identifier, tanks, unload delay, ports, fission.
        setupTanks();
        if (unloadDelay > 0) {
            unloadDelay--;
        }
        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        // since fluid sources are often not within 1 chunk, the original checks 2 chunks distance
        if (!level.hasChunk(chunkX, chunkZ)
                || !level.hasChunk(chunkX + 2, chunkZ + 2)
                || !level.hasChunk(chunkX + 2, chunkZ - 2)
                || !level.hasChunk(chunkX - 2, chunkZ + 2)
                || !level.hasChunk(chunkX - 2, chunkZ - 2)) {
            unloadDelay = 60;
        }
        if (!assembled) {
            sync();
            return;
        }
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, this); // [parity U1: fluid net]
        if (unloadDelay > 0) {
            hullHeat = 0;
            coreHeat = 0;
            sync();
            return;
        }
        loadFuel();
        moveControlRods();
        runFission(level);
        if (amountLoaded <= 0) {
            typeLoaded = -1;
        }
        if (amountLoaded > rodCount) {
            amountLoaded = rodCount;
        }
        updateCoolant();
        coreHeat *= 0.999D;
        hullHeat *= 0.999D;
        flux = newFluxThisTick;
        if (coolant.amount() > 0) {
            flux *= HbmThermalConversions.pwrModeratorMultiplier(coolant.type());
        }
        if (coreHeat > coreHeatCapacity) {
            meltDown();
            return;
        }
        sync();
    }

    private int newFluxThisTick;

    public AssemblyResult assemble() {
        if (level == null || level.isClientSide) {
            return new AssemblyResult(AssemblyStatus.UNAVAILABLE, worldPosition);
        }
        disassemble();
        Direction facing = getBlockState().hasProperty(PwrBlock.FACING) ? getBlockState().getValue(PwrBlock.FACING) : Direction.NORTH;
        // FACING is the visible controller face.  The reactor vessel is behind
        // it, exactly as in MachinePWRController from 1.7.10.
        BlockPos start = worldPosition.relative(facing.getOpposite());
        // [parity b2: assembly markers] MachinePWRController#assemble: the flood fill keeps going after an error and
        // every offending position gets its own sendError marker; the rod/source checks are reported at the controller.
        Set<BlockPos> found = new HashSet<>();
        List<AssemblyError> errors = new ArrayList<>();
        floodFill(start, found, errors);

        Map<BlockPos, Block> partMap = new HashMap<>();
        Map<BlockPos, Block> rodMap = new HashMap<>();
        int sourceBlocks = 0;
        for (BlockPos pos : found) {
            BlockState original = level.getBlockState(pos);
            Block block = original.getBlock();
            if (!(block instanceof PwrBlock pwr)) {
                continue;
            }
            partMap.put(pos, block);
            if (pwr.kind() == PwrBlock.Kind.FUEL) {
                rodMap.put(pos, block);
            }
            if (pwr.kind() == PwrBlock.Kind.NEUTRON_SOURCE) {
                sourceBlocks++;
            }
        }
        // [parity M2: PWR] MachinePWRController seeds the controller into the assembly map passed to setup()
        partMap.put(worldPosition, getBlockState().getBlock());
        AssemblyStatus status = errors.isEmpty() ? AssemblyStatus.SUCCESS
                : errors.get(0).label().equals(ERROR_MAX_SIZE) ? AssemblyStatus.MAX_SIZE_EXCEEDED : AssemblyStatus.NON_REACTOR_BLOCK;
        BlockPos problemPos = errors.isEmpty() ? worldPosition : errors.get(0).pos();
        if (rodMap.isEmpty()) {
            errors.add(new AssemblyError(worldPosition, ERROR_FUEL_RODS));
        }
        if (sourceBlocks == 0) {
            errors.add(new AssemblyError(worldPosition, ERROR_NEUTRON_SOURCES));
        }
        if (!errors.isEmpty()) {
            if (status == AssemblyStatus.SUCCESS) {
                status = rodMap.isEmpty() && sourceBlocks == 0 ? AssemblyStatus.FUEL_RODS_AND_NEUTRON_SOURCES_REQUIRED
                        : rodMap.isEmpty() ? AssemblyStatus.FUEL_RODS_REQUIRED : AssemblyStatus.NEUTRON_SOURCES_REQUIRED;
            }
            return new AssemblyResult(status, problemPos, List.copyOf(errors));
        }

        for (BlockPos pos : found) {
            BlockState original = level.getBlockState(pos);
            if (!(original.getBlock() instanceof PwrBlock pwr)) {
                continue;
            }
            BlockState replacement = HbmBlocks.PWR_BLOCK.get().defaultBlockState()
                    .setValue(PwrBlock.PORT, pwr.kind() == PwrBlock.Kind.PORT);
            level.setBlock(pos, replacement, 3);
            if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
                part.bind(original, worldPosition, pwr.kind());
            }
        }
        setup(partMap, rodMap);
        assembled = true;
        sync();
        return new AssemblyResult(AssemblyStatus.SUCCESS, worldPosition);
    }

    public void disassemble() {
        if (level == null || level.isClientSide || !assembled) {
            return;
        }
        List<BlockPos> restore = new ArrayList<>(assembledParts);
        for (BlockPos pos : new HashSet<>(restore)) {
            if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
                part.restoreOriginal();
            }
        }
        assembled = false;
        assembledParts.clear();
        ports.clear();
        rods.clear();
        setChanged();
    }

    public void invalidateAssemblyOnly() {
        assembled = false;
        setChanged();
    }

    public static final String ERROR_MAX_SIZE = "Max size exceeded";
    public static final String ERROR_NON_REACTOR = "Non-reactor block";
    public static final String ERROR_FUEL_RODS = "Fuel rods required";
    public static final String ERROR_NEUTRON_SOURCES = "Neutron sources required";

    /** MachinePWRController#floodFill, same visiting order (x+1, x-1, y+1, y-1, z+1, z-1). */
    private void floodFill(BlockPos pos, Set<BlockPos> found, List<AssemblyError> errors) {
        // The old assembler seeded the controller itself into the visited
        // map.  It is therefore the only non-core block that may touch the
        // reactor interior without being treated as a vessel leak.
        if (pos.equals(worldPosition) || found.contains(pos)) {
            return;
        }
        if (found.size() + 1 >= MAX_ASSEMBLY_SIZE) {
            errors.add(new AssemblyError(pos.immutable(), ERROR_MAX_SIZE));
            return;
        }
        Block block = level.getBlockState(pos).getBlock();
        if (isValidPwrCasing(block)) {
            found.add(pos.immutable());
            return;
        }
        if (isValidPwrCore(block)) {
            found.add(pos.immutable());
            floodFill(pos.east(), found, errors);
            floodFill(pos.west(), found, errors);
            floodFill(pos.above(), found, errors);
            floodFill(pos.below(), found, errors);
            floodFill(pos.south(), found, errors);
            floodFill(pos.north(), found, errors);
            return;
        }
        errors.add(new AssemblyError(pos.immutable(), ERROR_NON_REACTOR));
    }

    public enum AssemblyStatus {
        SUCCESS,
        UNAVAILABLE,
        NON_REACTOR_BLOCK,
        MAX_SIZE_EXCEEDED,
        FUEL_RODS_REQUIRED,
        NEUTRON_SOURCES_REQUIRED,
        FUEL_RODS_AND_NEUTRON_SOURCES_REQUIRED
    }

    /** One MachinePWRController#sendError call: marker position and label. */
    public record AssemblyError(BlockPos pos, String label) {
    }

    public record AssemblyResult(AssemblyStatus status, BlockPos problemPos, List<AssemblyError> errors) {
        public AssemblyResult(AssemblyStatus status, BlockPos problemPos) {
            this(status, problemPos, List.of());
        }
    }

    private static boolean isValidPwrCore(Block block) {
        return isFuelRod(block)
                || block == HbmBlocks.PWR_CONTROL.get()
                || block == HbmBlocks.PWR_CHANNEL.get()
                || block == HbmBlocks.PWR_HEATEX.get()
                || block == HbmBlocks.PWR_HEATSINK.get()
                || block == HbmBlocks.PWR_NEUTRON_SOURCE.get();
    }

    private static boolean isValidPwrCasing(Block block) {
        return block == HbmBlocks.PWR_CASING.get()
                || block == HbmBlocks.PWR_REFLECTOR.get()
                || block == HbmBlocks.PWR_PORT.get();
    }

    private void setup(Map<BlockPos, Block> partMap, Map<BlockPos, Block> rodMap) {
        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();
        assembledParts.clear();

        int connectionsDouble = 0;
        int controlledDouble = 0;
        for (Map.Entry<BlockPos, Block> entry : partMap.entrySet()) {
            Block block = entry.getValue();
            assembledParts.add(entry.getKey());
            if (isFuelRod(block)) rodCount++;
            if (block == HbmBlocks.PWR_HEATEX.get()) heatexCount++;
            if (block == HbmBlocks.PWR_CHANNEL.get()) channelCount++;
            if (block == HbmBlocks.PWR_HEATSINK.get()) heatsinkCount++;
            if (block == HbmBlocks.PWR_NEUTRON_SOURCE.get()) sourceCount++;
            if (block == HbmBlocks.PWR_PORT.get()) ports.add(entry.getKey());
        }

        for (BlockPos fuelPos : rodMap.keySet()) {
            rods.add(fuelPos);
            for (Direction direction : Direction.values()) {
                boolean controlled = false;
                for (int i = 1; i < 16; i++) {
                    BlockPos check = fuelPos.relative(direction, i);
                    Block at = partMap.get(check);
                    if (at == null || at == HbmBlocks.PWR_CASING.get()) break;
                    if (at == HbmBlocks.PWR_CONTROL.get()) controlled = true;
                    if (isFuelRod(at)) {
                        if (controlled) controlledDouble++; else connectionsDouble++;
                        break;
                    }
                    if (at == HbmBlocks.PWR_REFLECTOR.get()) {
                        if (controlled) controlledDouble += 2; else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }
        connections = connectionsDouble / 2;
        connectionsControlled = controlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);
        coreHeatCapacity = CORE_HEAT_CAPACITY_BASE + heatsinkCount * (CORE_HEAT_CAPACITY_BASE / 20L);
    }

    private static boolean isFuelRod(Block block) {
        return block == HbmBlocks.PWR_FUELROD.get()
                || block == HbmBlocks.PWR_FUEL.get();
    }

    private void loadFuel() {
        ItemStack input = items.get(SLOT_INPUT);
        if ((typeLoaded == -1 || amountLoaded <= 0) && input.getItem() instanceof PwrFuelItem) {
            typeLoaded = PwrFuelItem.fuelIndex(input);
            amountLoaded++;
            input.shrink(1);
            if (input.isEmpty()) items.set(SLOT_INPUT, ItemStack.EMPTY);
        } else if (input.getItem() instanceof PwrFuelItem && PwrFuelItem.fuelIndex(input) == typeLoaded && amountLoaded < rodCount) {
            amountLoaded++;
            input.shrink(1);
            if (input.isEmpty()) items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
    }

    private void moveControlRods() {
        double diff = rodLevel - rodTarget;
        if (diff < 1.0D && diff > -1.0D) rodLevel = rodTarget;
        if (rodTarget > rodLevel) rodLevel++;
        if (rodTarget < rodLevel) rodLevel--;
    }

    private void runFission(Level level) {
        int newFlux = sourceCount * 20;
        double moderatorMultiplier = coolant.amount() > 0
                ? HbmThermalConversions.pwrModeratorMultiplier(coolant.type()) : 1.0D;
        if (typeLoaded >= 0 && amountLoaded > 0 && typeLoaded < PwrFuelItem.FUELS.size() && rodCount > 0) {
            if (level instanceof ServerLevel serverLevel && level.getGameTime() % 100L == 0L) {
                com.atomiclegacy.satellite.SatelliteEventTracker.reportRay(
                        serverLevel, this.worldPosition,
                        com.atomiclegacy.satellite.SatelliteEventTracker.RayEvent.INFO_NUCLEAR, 200);
            }
            PwrFuelItem.Fuel fuel = PwrFuelItem.FUELS.get(typeLoaded);
            double usedRods = getTotalProcessMultiplier();
            double fluxPerRod = flux / rodCount;
            double outputPerRod = fuel.function().output(fluxPerRod);
            double totalOutput = outputPerRod * amountLoaded * usedRods;
            double totalHeatOutput = totalOutput * fuel.heatEmission();
            if (coolant.amount() > 0) {
                totalHeatOutput *= moderatorMultiplier;
            }
            coreHeat += totalHeatOutput;
            newFlux += totalOutput;
            processTime = fuel.yield();
            progress += totalOutput;
            if (progress >= processTime) {
                progress -= processTime;
                ItemStack output = items.get(SLOT_OUTPUT);
                if (output.isEmpty()) {
                    items.set(SLOT_OUTPUT, PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL_HOT.get(), typeLoaded));
                } else if (output.getItem() == HbmItems.PWR_FUEL_HOT.get() && PwrFuelItem.fuelIndex(output) == typeLoaded && output.getCount() < output.getMaxStackSize()) {
                    output.grow(1);
                }
                amountLoaded--;
            }
        }
        newFluxThisTick = newFlux;
    }

    private void updateCoolant() {
        // [parity M2: PWR] compound long -= double, exactly as CORE COOLING in the original
        double approach = getXOverE((double) heatexCount * 5.0D / (double) getRodCountForCoolant(), 2.0D) / 2.0D;
        long average = (coreHeat + hullHeat) / 2L;
        coreHeat -= (coreHeat - average) * approach;
        hullHeat -= (hullHeat - average) * approach;
        HbmThermalConversions.firstPwrStep(coolant.type()).filter(step -> step.boilerEfficiency() > 0.0D).ifPresent(step -> {
            double coolingEff = (double) channelCount / (double) getRodCountForCoolant() * 0.1D;
            if (coolingEff > 1.0D) coolingEff = 1.0D;
            int heatToUse = (int) Math.min(Math.min(hullHeat,
                    (long) (hullHeat * coolingEff * step.boilerEfficiency())), 2_000_000_000L);
            int coolCycles = coolant.amount() / step.amountReq();
            int hotCycles = (hotCoolant.capacity() - hotCoolant.amount()) / step.amountProduced();
            int heatCycles = heatToUse / step.heatReq();
            int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));
            if (cycles > 0) {
                hullHeat -= (long) step.heatReq() * cycles;
                coolant.setAmount(coolant.amount() - step.amountReq() * cycles);
                hotCoolant.fill(step.output(), step.amountProduced() * cycles, false);
            }
        });
    }

    private void setupTanks() {
        // [parity M2: PWR] tanks[0].setType(2, slots) while no fuel is loaded, then setupTanks(): an unusable
        // coolant voids both tanks (FluidTank#setTankType clears the fill on a type change).
        ItemStack identifier = items.get(SLOT_IDENTIFIER);
        if (amountLoaded <= 0 && identifier.getItem() instanceof FluidIdentifierItem) {
            coolant.setType(FluidIdentifierItem.primary(identifier));
        }
        HbmThermalConversions.firstPwrStep(coolant.type()).filter(step -> step.boilerEfficiency() > 0.0D).ifPresentOrElse(
                step -> hotCoolant.setType(step.output()),
                () -> {
                    coolant.setType(HbmFluids.none());
                    hotCoolant.setType(HbmFluids.none());
                }
        );
    }


    private void meltDown() {
        // [parity M2: PWR] TileEntityPWRController#meltDown: destroy the controller without drops, turn every fuel
        // column into corium, explode with fire at the average rod position (15F, flaming, smoking).
        if (level == null) return;
        assembled = false;
        List<BlockPos> rodPositions = new ArrayList<>(rods);
        level.destroyBlock(worldPosition, false);
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (BlockPos rod : rodPositions) {
            if (level.getBlockEntity(rod) instanceof PwrBlockEntity part) {
                part.restoreOriginal();
            }
            level.setBlock(rod, HbmBlocks.CORIUM_BLOCK.get().defaultBlockState(), 3);
            x += rod.getX() + 0.5D;
            y += rod.getY() + 0.5D;
            z += rod.getZ() + 0.5D;
        }
        x /= rodPositions.size();
        y /= rodPositions.size();
        z /= rodPositions.size();
        level.explode(null, null, null, x, y, z, 15.0F, true, Level.ExplosionInteraction.BLOCK);
    }

    private int getRodCountForCoolant() {
        return rodCount + (int) Math.ceil(heatsinkCount / 4.0D);
    }

    private double getTotalProcessMultiplier() {
        double totalConnections = connections + connectionsControlled * (1.0D - (rodLevel / 100.0D));
        return connectinFunc(totalConnections);
    }

    private double connectinFunc(double connections) {
        return connections / 10.0D * (1.0D - getXOverE(connections, 300.0D)) + connections / 150.0D * getXOverE(connections, 300.0D);
    }

    private double getXOverE(double x, double d) {
        return 1.0D - Math.pow(Math.E, -x / d);
    }

    public boolean assembled() { return assembled; }
    public void setRodTarget(double target) { this.rodTarget = Math.max(0.0D, Math.min(100.0D, target)); sync(); }
    public HbmFluidTank coolantTank() { return coolant; }
    public HbmFluidTank hotCoolantTank() { return hotCoolant; }
    public boolean isPort(BlockPos pos) { return ports.contains(pos); }

    @Override
    public String[] rttyFunctionInfo() {
        return new String[] {
                RttyInfo.PREFIX_VALUE + "rods",
                RttyInfo.PREFIX_VALUE + "coreheat",
                RttyInfo.PREFIX_VALUE + "hullheat",
                RttyInfo.PREFIX_VALUE + "coldbuf",
                RttyInfo.PREFIX_VALUE + "hotbuf",
                RttyInfo.PREFIX_VALUE + "flux",
                RttyInfo.PREFIX_VALUE + "depletion",
                RttyInfo.PREFIX_FUNCTION + "setrods" + RttyInteractive.NAME_SEPARATOR + "percent",
                RttyInfo.PREFIX_FUNCTION + "jettison"
        };
    }

    @Override
    public String provideRttyValue(String name) {
        if (name.equals(RttyInfo.PREFIX_VALUE + "rods")) return Integer.toString((int) (100.0D - this.rodLevel));
        if (name.equals(RttyInfo.PREFIX_VALUE + "coreheat")) return Long.toString(this.coreHeat);
        if (name.equals(RttyInfo.PREFIX_VALUE + "hullheat")) return Long.toString(this.hullHeat);
        if (name.equals(RttyInfo.PREFIX_VALUE + "coldbuf")) return Integer.toString(this.coolant.amount());
        if (name.equals(RttyInfo.PREFIX_VALUE + "hotbuf")) return Integer.toString(this.hotCoolant.amount());
        if (name.equals(RttyInfo.PREFIX_VALUE + "flux")) return Integer.toString((int) this.flux);
        if (name.equals(RttyInfo.PREFIX_VALUE + "depletion")) {
            return Integer.toString((int) (this.progress * 100 / this.processTime)); // [parity U2: ROR] no guard, like 1.7.10
        }
        return null;
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if (name.equals(RttyInfo.PREFIX_FUNCTION + "setrods") && params.length > 0) {
            int percent = RttyInteractive.parseInt(params[0], 0, 100);
            this.rodTarget = 100.0D - percent;
            sync();
        }
        if (name.equals(RttyInfo.PREFIX_FUNCTION + "jettison")) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0.0D;
            sync();
        }
        return null;
    }

    /**
     * PWR stores its assembled footprint as absolute positions. Create moves
     * the block entities as a snapshot, so all three lists must follow the
     * same authoritative StructureTransform as their blocks.
     */
    public void transformAssemblyLinks(java.util.function.UnaryOperator<BlockPos> transform) {
        if (!assembled) {
            return;
        }
        assembledParts.replaceAll(pos -> transform.apply(pos).immutable());
        ports.replaceAll(pos -> transform.apply(pos).immutable());
        rods.replaceAll(pos -> transform.apply(pos).immutable());
        setChanged();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int clampInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    @Override
    public int getContainerSize() { return SLOT_COUNT; }
    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }
    @Override
    public ItemStack removeItem(int slot, int amount) { ItemStack stack = ContainerHelper.removeItem(items, slot, amount); if (!stack.isEmpty()) sync(); return stack; }
    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override
    public void setItem(int slot, ItemStack stack) { items.set(slot, stack); sync(); }
    @Override
    public boolean stillValid(Player player) { return MachineDummyMenuAccess.stillValid(this, player); }
    @Override
    public void clearContent() { items.clear(); sync(); }
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && stack.getItem() instanceof PwrFuelItem
                || slot == SLOT_IDENTIFIER && stack.getItem() instanceof FluidIdentifierItem;
    }
    @Override
    public int[] getSlotsForFace(Direction side) { return new int[]{SLOT_INPUT, SLOT_OUTPUT}; }
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) { return canPlaceItem(slot, stack); }
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return slot == SLOT_OUTPUT; }
    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.atomiclegacy.pwr"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PwrMenu(containerId, inventory, this, menuData, worldPosition);
    }

    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!queriedPos.equals(worldPosition) && !isPort(queriedPos)) {
            return null;
        }
        return new PwrFluidHandler();
    }

    private final class PwrFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? coolant.getFluidInTank(0) : hotCoolant.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? coolant.capacity() : hotCoolant.capacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && coolant.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return coolant.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return hotCoolant.drain(resource, action); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return hotCoolant.drain(maxDrain, action); }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("coolant", coolant.save());
        tag.put("hotCoolant", hotCoolant.save());
        tag.putLong("coreHeat", coreHeat);
        tag.putLong("coreHeatCapacity", coreHeatCapacity);
        tag.putLong("hullHeat", hullHeat);
        tag.putDouble("flux", flux);
        tag.putDouble("rodLevel", rodLevel);
        tag.putDouble("rodTarget", rodTarget);
        tag.putInt("typeLoaded", typeLoaded);
        tag.putInt("amountLoaded", amountLoaded);
        tag.putDouble("progress", progress);
        tag.putDouble("processTime", processTime);
        tag.putBoolean("assembled", assembled);
        tag.putInt("rodCount", rodCount);
        tag.putInt("connections", connections);
        tag.putInt("connectionsControlled", connectionsControlled);
        tag.putInt("heatexCount", heatexCount);
        tag.putInt("heatsinkCount", heatsinkCount);
        tag.putInt("channelCount", channelCount);
        tag.putInt("sourceCount", sourceCount);
        tag.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) tag.putLong("p" + i, ports.get(i).asLong());
        tag.putInt("rodPosCount", rods.size());
        for (int i = 0; i < rods.size(); i++) tag.putLong("r" + i, rods.get(i).asLong());
        tag.putInt("assembledPartCount", assembledParts.size());
        for (int i = 0; i < assembledParts.size(); i++) tag.putLong("a" + i, assembledParts.get(i).asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        coolant.load(tag.getCompound("coolant"));
        hotCoolant.load(tag.getCompound("hotCoolant"));
        coreHeat = tag.getLong("coreHeat");
        coreHeatCapacity = Math.max(CORE_HEAT_CAPACITY_BASE, tag.getLong("coreHeatCapacity"));
        hullHeat = tag.getLong("hullHeat");
        flux = tag.getDouble("flux");
        rodLevel = tag.getDouble("rodLevel");
        rodTarget = tag.getDouble("rodTarget");
        typeLoaded = tag.getInt("typeLoaded");
        amountLoaded = tag.getInt("amountLoaded");
        progress = tag.getDouble("progress");
        processTime = tag.getDouble("processTime");
        assembled = tag.getBoolean("assembled");
        rodCount = tag.getInt("rodCount");
        connections = tag.getInt("connections");
        connectionsControlled = tag.getInt("connectionsControlled");
        heatexCount = tag.getInt("heatexCount");
        heatsinkCount = tag.getInt("heatsinkCount");
        channelCount = tag.getInt("channelCount");
        sourceCount = tag.getInt("sourceCount");
        ports.clear();
        for (int i = 0; i < tag.getInt("portCount"); i++) ports.add(BlockPos.of(tag.getLong("p" + i)));
        rods.clear();
        for (int i = 0; i < tag.getInt("rodPosCount"); i++) rods.add(BlockPos.of(tag.getLong("r" + i)));
        assembledParts.clear();
        for (int i = 0; i < tag.getInt("assembledPartCount"); i++) assembledParts.add(BlockPos.of(tag.getLong("a" + i)));
    }

    // [parity U1: fluid net] TileEntityPWRController (pwr_port: PortDef.make(port, VALID_DIRECTIONS)) as IFluidStandard*MK2 with TilePort nodes
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidSendingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{hotCoolant}; }
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{coolant}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return !assembled ? java.util.List.<FluidPort>of() : ports.stream().flatMap(p -> java.util.Arrays.stream(Direction.values()).map(d -> new FluidPort(p, d))).toList();
    }
}
