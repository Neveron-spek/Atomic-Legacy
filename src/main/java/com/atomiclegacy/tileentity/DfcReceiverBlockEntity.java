package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.DfcComponentBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.DfcReceiverMenu;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class DfcReceiverBlockEntity extends DfcInventoryBlockEntity implements DfcLaserTarget, MenuProvider, PowerEndpoint, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity V1: fluid net]
    public static final int TANK_CAPACITY = 64_000;
    public static final int DATA_COUNT = 7;

    private final HbmFluidTank tank = new HbmFluidTank(cryogel(), TANK_CAPACITY);
    private final IFluidHandler fluidHandler = new DfcFluidHandler(new HbmFluidTank[]{this.tank}, DfcReceiverBlockEntity::isCryogel);
    private long power;
    private long joules;
    private long lastOutput;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DfcReceiverBlockEntity.this.tank.type().oldId();
                case 1 -> DfcReceiverBlockEntity.this.tank.amount();
                case 2 -> DfcReceiverBlockEntity.this.tank.capacity();
                case 3 -> (int) DfcReceiverBlockEntity.this.joules;
                case 4 -> (int) DfcReceiverBlockEntity.this.power;
                case 5 -> (int) DfcReceiverBlockEntity.this.lastOutput;
                case 6 -> DfcReceiverBlockEntity.this.joules > 0L ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcReceiverBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> DfcReceiverBlockEntity.this.tank.setAmount(value);
                case 3 -> DfcReceiverBlockEntity.this.joules = Integer.toUnsignedLong(value);
                case 4 -> DfcReceiverBlockEntity.this.power = Integer.toUnsignedLong(value);
                case 5 -> DfcReceiverBlockEntity.this.lastOutput = Integer.toUnsignedLong(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcReceiverBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_RECEIVER.get(), pos, blockState, 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcReceiverBlockEntity receiver) {
        if (!level.isClientSide) {
            receiver.tickServer(level);
        } else {
            receiver.refreshClientBeamSnapshotRevision();
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

    public long joules() {
        return this.joules;
    }

    public long power() {
        return this.power;
    }

    @Override
    public void addDfcEnergy(long energy, Direction beamDirection) {
        if (beamDirection.getOpposite() == facing()) {
            this.joules += Math.max(0L, energy);
            setChangedAndSync(true);
            return;
        }
        if (this.level != null) {
            this.level.destroyBlock(this.worldPosition, false);
            this.level.explode(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, 2.5F, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return this.power;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastOutput = usedOutput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.lastOutput + " HE/t");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.dfc_receiver");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcReceiverMenu(containerId, playerInventory, this, this.menuData);
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
        tag.putLong("Joules", this.joules);
        tag.putLong("LastOutput", this.lastOutput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.load(tag.getCompound("Cryogel"));
        if (this.tank.type().isNone() && this.tank.amount() == 0) {
            this.tank.setType(cryogel());
        }
        this.power = Math.max(0L, tag.getLong("Power"));
        this.joules = Math.max(0L, tag.getLong("Joules"));
        this.lastOutput = Math.max(0L, tag.getLong("LastOutput"));
    }

    private void tickServer(Level level) {
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(this.level, this); // [parity V1: fluid net] setupAllPorts + updatePort*FIFO
        this.power = this.joules * 5000L;
        PowerNetworkManager.tickFromEndpoint(level, this);

        if (this.joules > 0L && !consumeCryogel(level)) {
            return;
        }
        setChangedAndSync(level.getGameTime() % 10L == 0L || this.joules > 0L);
        this.joules = 0L;
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

    private static boolean isCryogel(HbmFluidDefinition fluid) {
        return fluid != null && fluid.name().equals("cryogel");
    }

    private static HbmFluidDefinition cryogel() {
        return HbmFluids.byName("cryogel").orElse(HbmFluids.none());
    }

    // [parity V1: fluid net] TileEntityCoreReceiver as IFluidStandard*MK2 with TilePort nodes (getPorts)
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{this.tank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        return com.atomiclegacy.fluid.HbmPortDefs.around(this.worldPosition);
    }
}
