package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.DfcComponentBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.DfcInjectorMenu;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class DfcInjectorBlockEntity extends DfcInventoryBlockEntity implements MenuProvider, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity V1: fluid net]
    public static final int INPUT_0 = 0;
    public static final int OUTPUT_0 = 1;
    public static final int INPUT_1 = 2;
    public static final int OUTPUT_1 = 3;
    public static final int SLOT_COUNT = 4;
    public static final int TANK_CAPACITY = 128_000;
    public static final int RANGE = 15;
    public static final int DATA_COUNT = 8;

    private final HbmFluidTank[] tanks = {
            new HbmFluidTank(fluid("deuterium"), TANK_CAPACITY),
            new HbmFluidTank(fluid("tritium"), TANK_CAPACITY)
    };
    // [parity M3: dfc] IFluidStandardReceiverMK2: each tank only receives its own (identifier-set) type.
    private final IFluidHandler fluidHandler = new DfcFluidHandler(this.tanks, fluid -> fluid == this.tanks[0].type() || fluid == this.tanks[1].type());
    private int beam;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DfcInjectorBlockEntity.this.tanks[0].type().oldId();
                case 1 -> DfcInjectorBlockEntity.this.tanks[0].amount();
                case 2 -> DfcInjectorBlockEntity.this.tanks[0].capacity();
                case 3 -> DfcInjectorBlockEntity.this.tanks[1].type().oldId();
                case 4 -> DfcInjectorBlockEntity.this.tanks[1].amount();
                case 5 -> DfcInjectorBlockEntity.this.tanks[1].capacity();
                case 6 -> DfcInjectorBlockEntity.this.beam;
                case 7 -> 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcInjectorBlockEntity.this.tanks[0].setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> DfcInjectorBlockEntity.this.tanks[0].setAmount(value);
                case 3 -> DfcInjectorBlockEntity.this.tanks[1].setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 4 -> DfcInjectorBlockEntity.this.tanks[1].setAmount(value);
                case 6 -> DfcInjectorBlockEntity.this.setBeam(value, true);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcInjectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_INJECTOR.get(), pos, blockState, SLOT_COUNT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcInjectorBlockEntity injector) {
        if (!level.isClientSide) {
            injector.tickServer(level);
        } else {
            injector.refreshClientBeamSnapshotRevision();
        }
    }

    public HbmFluidTank tank(int index) {
        return this.tanks[index];
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return this.fluidHandler;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int beam() {
        return this.beam;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // [parity M3: dfc] ContainerCoreInjector: slots 0 and 2 are plain Slots, 1 and 3 are output slots.
        return slot == INPUT_0 || slot == INPUT_1;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.dfc_injector");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcInjectorMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel1", this.tanks[0].save());
        tag.put("Fuel2", this.tanks[1].save());
        tag.putInt("Beam", this.beam);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tanks[0].load(tag.getCompound("Fuel1"));
        this.tanks[1].load(tag.getCompound("Fuel2"));
        if (this.tanks[0].type().isNone() && this.tanks[0].amount() == 0) {
            this.tanks[0].setType(fluid("deuterium"));
        }
        if (this.tanks[1].type().isNone() && this.tanks[1].amount() == 0) {
            this.tanks[1].setType(fluid("tritium"));
        }
        setBeam(tag.getInt("Beam"), true);
    }

    private void tickServer(Level level) {
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(this.level, this); // [parity V1: fluid net] setupAllPorts + updatePort*FIFO
        boolean changed = transferContainers();
        int previousBeam = this.beam;
        this.beam = 0;
        Direction direction = facing();
        for (int distance = 1; distance <= RANGE; distance++) {
            BlockPos targetPos = this.worldPosition.relative(direction, distance);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target instanceof DfcCoreBlockEntity core) {
                for (int index = 0; index < this.tanks.length; index++) {
                    changed |= transferTankToCore(this.tanks[index], core.tank(index));
                }
                this.beam = distance;
                break;
            }
            if (!level.getBlockState(targetPos).isAir()) {
                break;
            }
        }
        publishBeamRenderSnapshot(this.beam, false);
        if (changed || level.getGameTime() % 10L == 0L || this.beam > 0 || previousBeam != this.beam) {
            setChangedAndSync(true);
        }
    }

    private void setBeam(int beam, boolean rebuildClientSection) {
        this.beam = Math.max(0, beam);
        publishBeamRenderSnapshot(this.beam, rebuildClientSection);
    }

    // [parity M3: dfc] tanks[0].setType(0, 1, slots); tanks[1].setType(2, 3, slots) (FluidTank.setType with
    // in != out): an identifier in the input slot sets the tank type, empties the tank and moves to the output slot.
    private boolean transferContainers() {
        boolean changed = setTypeFromIdentifier(this.tanks[0], INPUT_0, OUTPUT_0);
        changed |= setTypeFromIdentifier(this.tanks[1], INPUT_1, OUTPUT_1);
        return changed;
    }

    private boolean setTypeFromIdentifier(HbmFluidTank tank, int in, int out) {
        ItemStack id = this.items.get(in);
        if (id.isEmpty() || !(id.getItem() instanceof com.atomiclegacy.items.FluidIdentifierItem) || !this.items.get(out).isEmpty()) {
            return false;
        }
        HbmFluidDefinition newType = com.atomiclegacy.items.FluidIdentifierItem.primary(id);
        if (tank.type() == newType) {
            return false;
        }
        tank.setType(newType);
        tank.setAmount(0);
        this.items.set(out, id.copy());
        this.items.set(in, ItemStack.EMPTY);
        return true;
    }

    // [parity M3: dfc] same type: move min(fill, space); otherwise, if the core tank is empty, adopt the injector type first.
    private boolean transferTankToCore(HbmFluidTank source, HbmFluidTank target) {
        if (target.type() != source.type()) {
            if (target.amount() != 0) {
                return false;
            }
            target.setType(source.type());
        }
        int moved = Math.min(source.amount(), target.capacity() - target.amount());
        source.setAmount(source.amount() - moved);
        target.setAmount(target.amount() + moved);
        return true;
    }

    private boolean canPlaceOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, output.copy());
        } else if (ItemStack.isSameItemSameComponents(current, output)) {
            current.grow(output.getCount());
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DfcComponentBlock.FACING) ? state.getValue(DfcComponentBlock.FACING) : Direction.NORTH;
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    // [parity V1: fluid net] TileEntityCoreInjector as IFluidStandard*MK2 with TilePort nodes (getPorts)
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{this.tanks[0], this.tanks[1]}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        return com.atomiclegacy.fluid.HbmPortDefs.around(this.worldPosition);
    }
}
