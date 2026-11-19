package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.DfcComponentBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.DfcEmitterMenu;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class DfcEmitterBlockEntity extends DfcInventoryBlockEntity implements DfcLaserTarget, MenuProvider, PowerEndpoint, com.atomiclegacy.rtty.RttyInteractive, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity V1: fluid net] [parity U2: ROR]
    public static final long MAX_POWER = 1_000_000_000L;
    public static final int TANK_CAPACITY = 64_000;
    public static final int RANGE = 50;
    public static final int DATA_COUNT = 10;

    private final HbmFluidTank tank = new HbmFluidTank(cryogel(), TANK_CAPACITY);
    private final IFluidHandler fluidHandler = new DfcFluidHandler(new HbmFluidTank[]{this.tank}, DfcEmitterBlockEntity::isCryogel);
    private long power;
    private long lastInput;
    private int watts = 1;
    private int beam;
    private long joules;
    private long prev;
    private boolean on;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DfcEmitterBlockEntity.this.tank.type().oldId();
                case 1 -> DfcEmitterBlockEntity.this.tank.amount();
                case 2 -> DfcEmitterBlockEntity.this.tank.capacity();
                case 3 -> (int) DfcEmitterBlockEntity.this.power;
                case 4 -> (int) MAX_POWER;
                case 5 -> DfcEmitterBlockEntity.this.watts;
                case 6 -> (int) DfcEmitterBlockEntity.this.prev;
                case 7 -> DfcEmitterBlockEntity.this.beam;
                case 8 -> DfcEmitterBlockEntity.this.on ? 1 : 0;
                case 9 -> (int) DfcEmitterBlockEntity.this.lastInput;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcEmitterBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> DfcEmitterBlockEntity.this.tank.setAmount(value);
                case 3 -> DfcEmitterBlockEntity.this.power = Integer.toUnsignedLong(value);
                case 5 -> DfcEmitterBlockEntity.this.watts = value;
                case 6 -> DfcEmitterBlockEntity.this.prev = Integer.toUnsignedLong(value);
                case 7 -> DfcEmitterBlockEntity.this.setBeam(value, true);
                case 8 -> DfcEmitterBlockEntity.this.on = value != 0;
                case 9 -> DfcEmitterBlockEntity.this.lastInput = Integer.toUnsignedLong(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcEmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_EMITTER.get(), pos, blockState, 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcEmitterBlockEntity emitter) {
        if (!level.isClientSide) {
            emitter.tickServer(level);
        } else {
            emitter.refreshClientBeamSnapshotRevision();
        }
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return this.fluidHandler;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int watts() {
        return this.watts;
    }

    public int beam() {
        return this.beam;
    }

    public long prev() {
        return this.prev;
    }

    public boolean on() {
        return this.on;
    }

    public void setWatts(int watts) {
        this.watts = clampWatts(watts);
        setChangedAndSync(true);
    }

    public void toggle() {
        this.on = !this.on;
        setChangedAndSync(true);
    }

    @Override
    public void addDfcEnergy(long energy, Direction beamDirection) {
        if (beamDirection.getOpposite() != facing()) {
            this.joules += Math.max(0L, energy);
            setChangedAndSync(true);
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastInput = receivedInput;
        this.power = Math.min(MAX_POWER, this.power + Math.max(0L, receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.power + "/" + MAX_POWER + " HE");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.dfc_emitter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcEmitterMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cryogel", this.tank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Watts", this.watts);
        tag.putInt("Beam", this.beam);
        tag.putLong("Joules", this.joules);
        tag.putLong("Prev", this.prev);
        tag.putBoolean("On", this.on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.load(tag.getCompound("Cryogel"));
        if (this.tank.type().isNone() && this.tank.amount() == 0) {
            this.tank.setType(cryogel());
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.watts = clampWatts(tag.getInt("Watts"));
        setBeam(tag.getInt("Beam"), true);
        this.joules = Math.max(0L, tag.getLong("Joules"));
        this.prev = Math.max(0L, tag.getLong("Prev"));
        this.on = tag.getBoolean("On");
    }

    private void tickServer(Level level) {
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(this.level, this); // [parity V1: fluid net] setupAllPorts + updatePort*FIFO
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.watts = clampWatts(this.watts);
        long demand = MAX_POWER * this.watts / 2000L;
        int previousBeam = this.beam;
        this.beam = 0;

        if ((this.joules > 0L || this.prev > 0L) && !consumeCryogel(level)) {
            finishBeamTick(level, previousBeam);
            return;
        }

        if (this.on) {
            if (this.power >= demand) {
                this.power -= demand;
                this.joules += this.watts * 100L;
            }
            this.prev = this.joules;

            if (this.joules > 0L) {
                emitBeam(level, this.joules * 95L / 100L);
                this.joules = 0L;
            }
        } else {
            this.joules = 0L;
            this.prev = 0L;
        }

        finishBeamTick(level, previousBeam);
    }

    private void finishBeamTick(Level level, int previousBeam) {
        publishBeamRenderSnapshot(this.beam, false);
        setChangedAndSync(level.getGameTime() % 10L == 0L || this.beam > 0 || previousBeam != this.beam);
    }

    private void setBeam(int beam, boolean rebuildClientSection) {
        this.beam = Math.max(0, beam);
        publishBeamRenderSnapshot(this.beam, rebuildClientSection);
    }

    private void emitBeam(Level level, long energy) {
        Direction direction = facing();
        long out = energy;
        for (int distance = 1; distance <= RANGE; distance++) {
            this.beam = distance;
            BlockPos targetPos = this.worldPosition.relative(direction, distance);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target instanceof DfcLaserTarget laserTarget) {
                laserTarget.addDfcEnergy(out, direction);
                break;
            }
            if (target instanceof DfcCoreBlockEntity core) {
                out = core.burn(out);
                continue;
            }

            BlockState hitState = level.getBlockState(targetPos);
            if (!hitState.isAir()) {
                if (!hitState.getFluidState().isEmpty()) {
                    // [parity M3: dfc] "random.fizz" 1.0/1.0 at the block centre.
                    level.playSound(null, targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D,
                            net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
                if (hitState.getBlock().getExplosionResistance() < 6000.0F && level.random.nextInt(20) == 0) {
                    level.destroyBlock(targetPos, false);
                }
                break;
            }
        }

        AABB area = beamArea(direction, this.beam);
        for (Entity entity : level.getEntities(null, area)) {
            entity.hurt(entity.damageSources().source(com.atomiclegacy.registry.HbmDamageTypes.AMS_CORE), 50.0F); // [parity M3: dfc]
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 200));
        }
    }

    private AABB beamArea(Direction direction, int distance) {
        BlockPos end = this.worldPosition.relative(direction, Math.max(0, distance));
        double minX = Math.min(this.worldPosition.getX(), end.getX()) + 0.2D;
        double maxX = Math.max(this.worldPosition.getX(), end.getX()) + 0.8D;
        double minY = Math.min(this.worldPosition.getY(), end.getY()) + 0.2D;
        double maxY = Math.max(this.worldPosition.getY(), end.getY()) + 0.8D;
        double minZ = Math.min(this.worldPosition.getZ(), end.getZ()) + 0.2D;
        double maxZ = Math.max(this.worldPosition.getZ(), end.getZ()) + 0.8D;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean consumeCryogel(Level level) {
        if (this.tank.amount() >= 20) {
            this.tank.drain(this.tank.type(), 20, false);
            return true;
        }
        level.setBlock(this.worldPosition, Blocks.LAVA.defaultBlockState(), 3);
        return false;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DfcComponentBlock.FACING) ? state.getValue(DfcComponentBlock.FACING) : Direction.NORTH;
    }

    private static int clampWatts(int watts) {
        return Math.max(1, Math.min(100, watts));
    }

    private static boolean isCryogel(HbmFluidDefinition fluid) {
        return fluid != null && fluid.name().equals("cryogel");
    }

    private static HbmFluidDefinition cryogel() {
        return HbmFluids.byName("cryogel").orElse(HbmFluids.none());
    }

    // [parity U2: ROR] TileEntityCoreEmitter IRORInteractive (no IRORValueProvider)
    @Override
    public String[] rttyFunctionInfo() {
        return new String[] {
                PREFIX_FUNCTION + "setpower" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "toggle",
                PREFIX_FUNCTION + "switch" + NAME_SEPARATOR + "on/off"};
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpower").equals(name) && params.length > 0) {
            this.watts = com.atomiclegacy.rtty.RttyInteractive.parseInt(params[0], 0, 100); // clamped to 1..100 on the next update
            setChangedAndSync(true);
            return null;
        }
        if ((PREFIX_FUNCTION + "toggle").equals(name)) {
            this.on = !this.on;
            setChangedAndSync(true);
            return null;
        }
        if ((PREFIX_FUNCTION + "switch").equals(name) && params.length > 0) {
            this.on = com.atomiclegacy.rtty.RttyInteractive.parseBool(params[0]);
            setChangedAndSync(true);
            return null;
        }
        return null;
    }

    // [parity V1: fluid net] TileEntityCoreEmitter as IFluidStandard*MK2 with TilePort nodes (getPorts)
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{this.tank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        return com.atomiclegacy.fluid.HbmPortDefs.around(this.worldPosition);
    }
}
