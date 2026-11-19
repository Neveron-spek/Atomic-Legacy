package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.CustomMachineBlock;
import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.config.CustomMachineConfigJSON.ComponentDefinition;
import com.atomiclegacy.config.CustomMachineConfigJSON.MachineConfiguration;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetUser;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.CustomMachineMenu;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.pollution.HbmPollutionWorlds;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * [parity X1: custom machines] TileEntityCustomMachine (HbmMods 936c60c) on TileEntityMachinePolluting(22, 100).
 * Slots: 0 battery, 1-3 fluid IDs, 4-9 inputs, 10-15 templates, 16-21 outputs. Ports: every cm_port of a valid
 * structure plus the controller, all six sides (PortDef.make); set up once (setupAllPorts) and used while the structure
 * is valid: power ports to the HE net (receiver, or provider in generator mode), one FluidNetMK2 node per tank type over
 * all ports, smoke through the same ports. Like the original only the configuration, items, tanks, smoke, templates and
 * the generator-mode recipe are saved; power, progress, heat and flux are not.
 */
public class CustomMachineBlockEntity extends BlockEntity implements PowerEndpoint, WorldlyContainer, MenuProvider, HbmFluidNetUser {
    public static final int SLOT_COUNT = 22;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}; // ForgeDirection 2..5

    public String machineType;
    public MachineConfiguration config;

    public long power;
    public int flux;
    public int heat;
    public int maxHeat;
    public int progress;
    public int maxProgress = 1;
    public HbmFluidTank[] inputTanks = new HbmFluidTank[0];
    public HbmFluidTank[] outputTanks = new HbmFluidTank[0];
    public ModulePatternMatcher matcher = new ModulePatternMatcher(0);
    public int structureCheckDelay;
    public boolean structureOK = false;
    public CustomMachineRecipe cachedRecipe;

    public final HbmFluidTank smoke = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.SOOT), 100);
    public final HbmFluidTank smoke_leaded = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.HEAVYMETAL), 100);
    public final HbmFluidTank smoke_poison = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.POISON), 100);

    public List<BlockPos> ports = new ArrayList<>();
    /** the port positions setupAllPorts used (only the first valid structure sets them up) */
    @Nullable
    private List<BlockPos> setupPorts;
    public final Set<BlockPos> fluxPos = new LinkedHashSet<>();
    public final Set<BlockPos> heatPos = new LinkedHashSet<>();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public CustomMachineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CUSTOM_MACHINE.get(), pos, state);
    }

    public void init() {
        MachineConfiguration config = CustomMachineConfigJSON.customMachines.get(this.machineType);

        if (config != null) {
            this.config = config;

            inputTanks = new HbmFluidTank[config.fluidInCount];
            for (int i = 0; i < inputTanks.length; i++) inputTanks[i] = new HbmFluidTank(config.fluidInCap);
            outputTanks = new HbmFluidTank[config.fluidOutCount];
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i] = new HbmFluidTank(config.fluidOutCap);
            maxHeat = config.maxHeat;
            matcher = new ModulePatternMatcher(config.itemInCount);
            smoke.setCapacity(config.maxPollutionCap);
            smoke_leaded.setCapacity(config.maxPollutionCap);
            smoke_poison.setCapacity(config.maxPollutionCap);

        } else if (this.level != null && !this.level.isClientSide) {
            this.level.destroyBlock(this.worldPosition, false);
        }
    }

    public String getName() {
        return config != null ? config.localizedName : "INVALID";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CustomMachineBlockEntity tile) {
        tile.updateEntity(level);
    }

    private void updateEntity(Level level) {
        if (config == null) {
            level.destroyBlock(this.worldPosition, false);
            return;
        }

        if (this.structureOK) {
            if (this.setupPorts == null) this.setupPorts = List.copyOf(this.ports); // setupAllPorts(ports)
            HbmFluidNetworks.subscribe(level, this); // moduleSmokePorts.update(ports) + updatePortPIFIFO / updatePortPOFIFO
            PowerNetworkManager.tickFromEndpoint(level, this);
        }

        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(0), this.power, this.config.maxPower);

        if (this.inputTanks.length > 0) setType(this.inputTanks[0], 1);
        if (this.inputTanks.length > 1) setType(this.inputTanks[1], 2);
        if (this.inputTanks.length > 2) setType(this.inputTanks[2], 3);

        this.structureCheckDelay--;
        if (this.structureCheckDelay <= 0) this.checkStructure();

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : HORIZONTAL) {
                for (BlockPos pos : this.fluxPos) {
                    ResearchReactorBlockEntity reactor = researchReactor(level, pos.offset(dir.getStepX(), 0, dir.getStepZ()));
                    if (reactor != null) {
                        this.flux = reactor.totalFlux();
                    }
                }
                if (config.maxHeat > 0) {
                    for (BlockPos pos : this.heatPos) {
                        this.tryPullHeat(level, pos.offset(dir.getStepX(), -1, dir.getStepZ()));
                    }
                }
            }
        }

        if (this.structureOK) {

            if (config.generatorMode) {
                if (this.cachedRecipe == null) {
                    CustomMachineRecipe recipe = this.getMatchingRecipe();
                    if (recipe != null && this.hasRequiredQuantities(recipe) && this.hasSpace(recipe)) {
                        this.cachedRecipe = recipe;
                        this.useUpInput(recipe);
                    }
                }

                if (this.cachedRecipe != null) {
                    this.maxProgress = (int) Math.max(cachedRecipe.duration / this.config.recipeSpeedMult, 1);
                    int powerReq = (int) Math.max(cachedRecipe.consumptionPerTick * this.config.recipeConsumptionMult, 1);

                    this.progress++;
                    this.power += powerReq;
                    this.heat -= cachedRecipe.heat;
                    if (power > config.maxPower) power = config.maxPower;
                    if (level.getGameTime() % 20 == 0) {
                        pollution(level, cachedRecipe);
                        radiation(level, cachedRecipe);
                    }
                    if (progress >= this.maxProgress) {
                        this.progress = 0;
                        this.processRecipe(level, cachedRecipe);
                        this.cachedRecipe = null;
                    }
                }

            } else {
                CustomMachineRecipe recipe = this.getMatchingRecipe();

                if (recipe != null) {
                    this.maxProgress = (int) Math.max(recipe.duration / this.config.recipeSpeedMult, 1);
                    int powerReq = (int) Math.max(recipe.consumptionPerTick * this.config.recipeConsumptionMult, 1);

                    if (this.power >= powerReq && this.hasRequiredQuantities(recipe) && this.hasSpace(recipe)) {
                        this.progress++;
                        this.power -= powerReq;
                        this.heat -= recipe.heat;
                        if (level.getGameTime() % 20 == 0) {
                            pollution(level, recipe);
                            radiation(level, recipe);
                        }
                        if (progress >= this.maxProgress) {
                            this.progress = 0;
                            this.useUpInput(recipe);
                            this.processRecipe(level, recipe);
                        }
                    }
                } else {
                    this.progress = 0;
                }
            }
        } else {
            this.progress = 0;
        }
        setChanged();
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); // networkPackNT(50)
    }

    /** FluidTank#setType(in, slots) with an IItemFluidIdentifier in the slot. */
    private void setType(HbmFluidTank tank, int slot) {
        ItemStack stack = this.items.get(slot);
        if (stack.getItem() instanceof FluidIdentifierItem) {
            tank.setType(FluidIdentifierItem.primary(stack));
        }
    }

    /** b == ModBlocks.reactor_research, ReactorResearch#findCore, TileEntityReactorResearch */
    @Nullable
    private static ResearchReactorBlockEntity researchReactor(Level level, BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof ResearchReactorBlockEntity reactor) return reactor;
        if (tile instanceof MachineDummyBlockEntity dummy && dummy.core() instanceof ResearchReactorBlockEntity reactor) return reactor;
        return null;
    }

    /** Only accepts inputs in a fixed order, saves a ton of performance because there's no permutations to check for */
    @Nullable
    public CustomMachineRecipe getMatchingRecipe() {
        List<CustomMachineRecipe> recipes = CustomMachineRecipes.recipes.get(this.config.recipeKey);
        if (recipes == null || recipes.isEmpty()) return null;

        outer:
        for (CustomMachineRecipe recipe : recipes) {
            for (int i = 0; i < recipe.inputFluids.length; i++) {
                if (i >= this.inputTanks.length) continue outer; // the original runs out of the tank array here
                if (this.inputTanks[i].type() != recipe.inputFluids[i].type() || this.inputTanks[i].pressure() != recipe.inputFluids[i].pressure()) continue outer;
            }

            for (int i = 0; i < recipe.inputItems.length; i++) {
                if (i + 4 > 9) continue outer;
                if (recipe.inputItems[i] != null && this.items.get(i + 4).isEmpty()) continue outer;
                if (!recipe.inputItems[i].matchesRecipe(this.items.get(i + 4))) continue outer;
            }

            return recipe;
        }

        return null;
    }

    public void pollution(Level level, CustomMachineRecipe recipe) {
        if (recipe.pollutionAmount > 0) {
            HbmPollution.bufferedLegacyPollute(level, this.worldPosition, HbmPollutionType.valueOf(recipe.pollutionType), recipe.pollutionAmount, this::smokeTank);
        } else if (recipe.pollutionAmount < 0 && level instanceof ServerLevel serverLevel
                && HbmPollutionWorlds.get(serverLevel, this.worldPosition, HbmPollutionType.valueOf(recipe.pollutionType)) >= -recipe.pollutionAmount) {
            HbmPollution.increment(level, this.worldPosition, HbmPollutionType.valueOf(recipe.pollutionType), recipe.pollutionAmount);
        }
    }

    public void radiation(Level level, CustomMachineRecipe recipe) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (recipe.radiationAmount > 0) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(this.worldPosition, recipe.radiationAmount);
        } else if (recipe.radiationAmount < 0) {
            ChunkRadiationData.get(serverLevel).decrementRadiation(this.worldPosition, -recipe.radiationAmount);
        }
    }

    /** TileEntityMachinePolluting#pollute tank selection */
    private HbmFluidTank smokeTank(HbmPollutionType type) {
        return type == HbmPollutionType.SOOT ? smoke : type == HbmPollutionType.HEAVYMETAL ? smoke_leaded : smoke_poison;
    }

    protected void tryPullHeat(Level level, BlockPos pos) {
        BlockEntity con = level.getBlockEntity(pos);

        if (con instanceof HeatSourceBlockEntity source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) {
                return;
            }

            if (diff > 0) {
                source.useHeat(diff);
                this.heat += diff;
                if (this.heat > this.maxHeat)
                    this.heat = this.maxHeat;
            }
        }
    }

    public boolean hasRequiredQuantities(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.inputFluids.length; i++) {
            if (this.inputTanks[i].amount() < recipe.inputFluids[i].amount()) return false;
        }

        for (int i = 0; i < recipe.inputItems.length; i++) {
            if (!this.items.get(i + 4).isEmpty() && this.items.get(i + 4).getCount() < recipe.inputItems[i].stacksize()) return false;
        }
        if (config.fluxMode ? this.flux < recipe.flux : false) return false;
        if (config.maxHeat > 0 && recipe.heat > 0 ? this.heat < recipe.heat : false) return false;
        return true;
    }

    public boolean hasSpace(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.outputFluids.length; i++) {
            if (i >= this.outputTanks.length) return false; // the original runs out of the tank array here
            if (this.outputTanks[i].type() == recipe.outputFluids[i].type() && this.outputTanks[i].amount() + recipe.outputFluids[i].amount() > this.outputTanks[i].capacity()) return false;
        }

        for (int i = 0; i < recipe.outputItems.length; i++) {
            if (i + 16 > 21) return false;
            ItemStack slot = this.items.get(i + 16);
            ItemStack out = recipe.outputItems[i].stack();
            if (!slot.isEmpty() && !ItemStack.isSameItemSameComponents(slot, out)) return false;
            if (!slot.isEmpty() && slot.getCount() + out.getCount() > slot.getMaxStackSize()) return false;
        }

        return true;
    }

    public void useUpInput(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.inputFluids.length; i++) {
            this.inputTanks[i].setAmount(this.inputTanks[i].amount() - recipe.inputFluids[i].amount());
        }

        for (int i = 0; i < recipe.inputItems.length; i++) {
            this.removeItem(i + 4, recipe.inputItems[i].stacksize());
        }
    }

    public void processRecipe(Level level, CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.outputFluids.length; i++) {
            HbmFluidStack out = recipe.outputFluids[i];
            if (this.outputTanks[i].type() != out.type()) this.outputTanks[i].setType(out.type());
            this.outputTanks[i].setAmount(this.outputTanks[i].amount() + out.amount());
        }

        for (int i = 0; i < recipe.outputItems.length; i++) {
            if (level.random.nextFloat() < recipe.outputItems[i].chance()) {
                ItemStack slot = this.items.get(i + 16);
                if (slot.isEmpty()) {
                    this.items.set(i + 16, recipe.outputItems[i].stack().copy());
                } else {
                    slot.grow(recipe.outputItems[i].stack().getCount());
                }
            }
        }
    }

    /** The world position of a component: "precisely the same method used for defining ports", EW swapped. */
    public static BlockPos componentPos(BlockPos core, Direction dir, int cx, int cy, int cz) {
        Direction rot = dir.getClockWise(); // dir.getRotation(ForgeDirection.UP)
        int x = core.getX() - dir.getStepX() * cx + rot.getStepX() * cx;
        int y = core.getY() + cy;
        int z = core.getZ() - dir.getStepZ() * cz + rot.getStepZ() * cz;
        if (dir == Direction.EAST || dir == Direction.WEST) {
            x = core.getX() + dir.getStepZ() * cz - rot.getStepZ() * cz;
            z = core.getZ() + dir.getStepX() * cx - rot.getStepX() * cx;
        }
        return new BlockPos(x, y, z);
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(CustomMachineBlock.FACING) ? state.getValue(CustomMachineBlock.FACING) : Direction.NORTH;
    }

    public boolean checkStructure() {
        this.structureCheckDelay = 300;
        this.structureOK = false;
        if (this.config == null || this.level == null) return false;

        List<BlockPos> portList = new ArrayList<>();
        Direction dir = facing();

        for (ComponentDefinition comp : config.components) {
            BlockPos pos = componentPos(this.worldPosition, dir, comp.x, comp.y, comp.z);

            BlockState state = this.level.getBlockState(pos);
            if (comp.block == null || state.getBlock() != comp.block) return false;

            int meta = CustomMachineConfigJSON.legacyMeta(state);
            if (!comp.allowedMetas.contains(meta)) return false;

            BlockEntity tile = this.level.getBlockEntity(pos);
            if (tile instanceof CustomMachinePortBlockEntity proxy) {
                proxy.bind(this.worldPosition);
                portList.add(pos);
            }
            if (state.getBlock() == HbmBlocks.CM_FLUX.get()) {
                for (Direction facing : Direction.values()) this.fluxPos.add(pos.relative(facing));
            } else if (state.getBlock() == HbmBlocks.CM_HEAT.get()) {
                for (Direction facing : Direction.values()) this.heatPos.add(pos.relative(facing));
            }
        }

        portList.add(this.worldPosition);
        this.ports = portList;

        this.structureOK = true;
        return true;
    }

    public void buildStructure() {
        if (this.config == null || this.level == null) return;
        Direction dir = facing();

        for (ComponentDefinition comp : config.components) {
            if (comp.block == null) continue;
            BlockPos pos = componentPos(this.worldPosition, dir, comp.x, comp.y, comp.z);
            this.level.setBlock(pos, CustomMachineConfigJSON.stateFor(comp.block, comp.firstMeta()), 3);
        }
    }

    /// PORTS ///

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        if (this.setupPorts == null) return List.of();
        Set<BlockPos> connectors = new LinkedHashSet<>();
        for (BlockPos port : this.setupPorts) {
            for (Direction dir : Direction.values()) connectors.add(port.relative(dir));
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    /** getProviderSpeed: maxPower in generator mode */
    @Override
    public long getAvailableOutput() {
        if (this.config == null || !this.config.generatorMode) return 0L;
        return Math.max(0L, Math.min(this.power, this.config.maxPower));
    }

    /** IEnergyReceiverMK2 demand up to getMaxPower at getReceiverSpeed (maxPower), none in generator mode */
    @Override
    public long getRequestedInput() {
        if (this.config == null || this.config.generatorMode) return 0L;
        return Math.max(0L, this.config.maxPower - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power -= usedOutput;
        if (receivedInput > 0) this.power = Math.min(getMaxPower(), this.power + receivedInput);
        setChanged();
    }

    /** transferPower: everything back in generator mode, else the overshoot over getMaxPower */
    @Override
    public long receiveLeftover(long offered) {
        if (this.config != null && this.config.generatorMode) return offered;
        return Math.max(0L, this.power + offered - getMaxPower());
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(getName() + ": " + this.power + " / " + getMaxPower() + " HE");
    }

    public long getMaxPower() {
        return this.config != null ? this.config.maxPower : 1;
    }

    @Override
    public HbmFluidTank[] fluidSendingTanks() {
        HbmFluidTank[] all = new HbmFluidTank[outputTanks.length + 3];
        System.arraycopy(outputTanks, 0, all, 0, outputTanks.length);
        all[outputTanks.length] = smoke;
        all[outputTanks.length + 1] = smoke_leaded;
        all[outputTanks.length + 2] = smoke_poison;
        return all;
    }

    @Override
    public HbmFluidTank[] fluidReceivingTanks() {
        return inputTanks;
    }

    @Override
    public List<FluidPort> fluidPorts(HbmFluidDefinition type) {
        if (this.setupPorts == null) return List.of();
        List<FluidPort> list = new ArrayList<>();
        for (BlockPos port : this.setupPorts) {
            for (Direction dir : Direction.values()) list.add(new FluidPort(port, dir));
        }
        return list;
    }

    public HbmFluidTank[] getAllTanks() {
        HbmFluidTank[] all = new HbmFluidTank[inputTanks.length + outputTanks.length];
        System.arraycopy(inputTanks, 0, all, 0, inputTanks.length);
        System.arraycopy(outputTanks, 0, all, inputTanks.length, outputTanks.length);
        return all;
    }

    /** The controller behind a cm_port (TileEntityProxyBase#getTE). */
    @Nullable
    public static CustomMachineBlockEntity findForPort(LevelAccessor level, BlockPos portPos) {
        return level.getBlockEntity(portPos) instanceof CustomMachinePortBlockEntity proxy
                && proxy.core() instanceof CustomMachineBlockEntity machine ? machine : null;
    }

    /** The original exposes no Forge fluid handler; FluidNetMK2 reaches the tanks through the port nodes. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return null;
    }

    /// INVENTORY ///

    public NonNullList<ItemStack> items() {
        return this.items;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (this.config == null) return new int[] { };
        if (this.config.itemInCount > 5) return new int[] { 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21 };
        if (this.config.itemInCount > 4) return new int[] { 4, 5, 6, 7, 8, 16, 17, 18, 19, 20, 21 };
        if (this.config.itemInCount > 3) return new int[] { 4, 5, 6, 7, 16, 17, 18, 19, 20, 21 };
        if (this.config.itemInCount > 2) return new int[] { 4, 5, 6, 16, 17, 18, 19, 20, 21 };
        if (this.config.itemInCount > 1) return new int[] { 4, 5, 16, 17, 18, 19, 20, 21 };
        if (this.config.itemInCount > 0) return new int[] { 4, 16, 17, 18, 19, 20, 21 };
        return new int[] { 16, 17, 18, 19, 20, 21 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 16 && slot <= 21;
    }

    /** isItemValidForSlot */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 4 || slot > 9) return false;

        int index = slot - 4;
        int filterSlot = slot + 6;

        if (this.items.get(filterSlot).isEmpty()) return true;
        if (index >= matcher.modes.length) return false;

        return matcher.isValidForFilter(this.items.get(filterSlot), index, stack);
    }

    /// NBT ///

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.machineType = nbt.getString("machineType");
        this.config = null;
        this.init();

        this.items.clear();
        ContainerHelper.loadAllItems(nbt, this.items, registries);
        smoke.load(nbt.getCompound("smoke0"));
        smoke_leaded.load(nbt.getCompound("smoke1"));
        smoke_poison.load(nbt.getCompound("smoke2"));
        if (this.config != null) {
            smoke.setCapacity(config.maxPollutionCap);
            smoke_leaded.setCapacity(config.maxPollutionCap);
            smoke_poison.setCapacity(config.maxPollutionCap);

            for (int i = 0; i < inputTanks.length; i++) inputTanks[i].load(nbt.getCompound("i" + i));
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i].load(nbt.getCompound("o" + i));
            for (int i = 0; i < inputTanks.length; i++) inputTanks[i].setCapacity(config.fluidInCap);
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i].setCapacity(config.fluidOutCap);

            this.matcher.readFromNBT(nbt);

            int index = nbt.contains("cachedIndex") ? nbt.getInt("cachedIndex") : -1;
            List<CustomMachineRecipe> list = CustomMachineRecipes.recipes.get(this.config.recipeKey);
            this.cachedRecipe = index != -1 && list != null && index < list.size() ? list.get(index) : null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        if (machineType == null || this.config == null) {
            ContainerHelper.saveAllItems(nbt, this.items, registries);
            return;
        }

        nbt.putString("machineType", machineType);
        ContainerHelper.saveAllItems(nbt, this.items, registries);
        nbt.put("smoke0", smoke.save());
        nbt.put("smoke1", smoke_leaded.save());
        nbt.put("smoke2", smoke_poison.save());

        for (int i = 0; i < inputTanks.length; i++) nbt.put("i" + i, inputTanks[i].save());
        for (int i = 0; i < outputTanks.length; i++) nbt.put("o" + i, outputTanks[i].save());

        this.matcher.writeToNBT(nbt);

        List<CustomMachineRecipe> list = CustomMachineRecipes.recipes.get(this.config.recipeKey);
        nbt.putInt("cachedIndex", this.cachedRecipe != null && list != null ? list.indexOf(this.cachedRecipe) : -1);
    }

    /** serialize(ByteBuf) of networkPackNT */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (this.machineType == null || this.config == null) return tag;
        tag.putString("machineType", this.machineType);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putInt("flux", flux);
        tag.putInt("heat", heat);
        tag.putBoolean("structureOK", structureOK);
        tag.putInt("maxProgress", maxProgress);
        for (int i = 0; i < inputTanks.length; i++) tag.put("i" + i, inputTanks[i].save());
        for (int i = 0; i < outputTanks.length; i++) tag.put("o" + i, outputTanks[i].save());
        for (int i = 0; i < matcher.modes.length; i++) tag.putString("mode" + i, matcher.modes[i] == null ? "" : matcher.modes[i]);
        return tag;
    }

    /** deserialize(ByteBuf) */
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("machineType")) return;
        String type = tag.getString("machineType");
        if (this.config == null || !type.equals(this.machineType)) {
            this.machineType = type;
            this.init();
        }
        if (this.config == null) return;
        this.power = tag.getLong("power");
        this.progress = tag.getInt("progress");
        this.flux = tag.getInt("flux");
        this.heat = tag.getInt("heat");
        this.structureOK = tag.getBoolean("structureOK");
        this.maxProgress = tag.getInt("maxProgress");
        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].load(tag.getCompound("i" + i));
        for (int i = 0; i < outputTanks.length; i++) outputTanks[i].load(tag.getCompound("o" + i));
        for (int i = 0; i < matcher.modes.length; i++) {
            String mode = tag.getString("mode" + i);
            matcher.modes[i] = mode.isEmpty() ? null : mode;
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /// GUI ///

    @Override
    public Component getDisplayName() {
        return Component.literal(getName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (this.config == null) return null;
        return new CustomMachineMenu(containerId, playerInventory, this);
    }
}
