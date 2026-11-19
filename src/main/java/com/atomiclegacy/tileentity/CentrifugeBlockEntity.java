package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.CentrifugeBlock;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.MachineUpgradeItem;
import com.atomiclegacy.inventory.container.CentrifugeMenu;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.inventory.recipes.CentrifugeRecipe;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CentrifugeBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_END = 6;
    public static final int UPGRADE_START = 6;
    public static final int UPGRADE_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 9; // [parity K3: centrifuge] index 8 = power >>> 16 (data slots sync as shorts)
    public static final long MAX_POWER = 100_000L;
    public static final int BASE_PROCESSING_SPEED = 200;
    public static final int BASE_CONSUMPTION = 200;
    private static final int TOWER_HEIGHT = 4;

    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, 2, 3, 4, 5};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private long power;
    private long lastInput;
    private int progress;
    private int completedCycles;
    private int currentConsumption = BASE_CONSUMPTION;
    private int currentSpeed = 1;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (CentrifugeBlockEntity.this.power & 0xFFFFL);
                case 1 -> (int) CentrifugeBlockEntity.this.lastInput;
                case 2 -> CentrifugeBlockEntity.this.progress;
                case 3 -> BASE_PROCESSING_SPEED;
                case 4 -> CentrifugeBlockEntity.this.currentConsumption;
                case 5 -> CentrifugeBlockEntity.this.currentSpeed;
                case 6 -> CentrifugeBlockEntity.this.completedCycles;
                case 7 -> CentrifugeBlockEntity.this.isWorking() ? 1 : 0;
                case 8 -> (int) ((CentrifugeBlockEntity.this.power >>> 16) & 0xFFFFL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 1 -> CentrifugeBlockEntity.this.lastInput = value;
                case 2 -> CentrifugeBlockEntity.this.progress = value;
                case 4 -> CentrifugeBlockEntity.this.currentConsumption = value;
                case 5 -> CentrifugeBlockEntity.this.currentSpeed = value;
                case 6 -> CentrifugeBlockEntity.this.completedCycles = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CentrifugeBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CENTRIFUGE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity centrifuge) {
        PowerNetworkManager.tickFromEndpoint(level, centrifuge);
        centrifuge.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public boolean isWorking() {
        return this.progress > 0;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return towerPowerConnectorPositions(this.worldPosition);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return isTowerPowerConnector(this.worldPosition, connectorPos, machineSide);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.power >= MAX_POWER) {
            return 0L;
        }
        return MAX_POWER - this.power; // [parity K3: centrifuge] receivePower fills up to maxPower
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.progress * 100 / BASE_PROCESSING_SPEED;
        return Component.translatable(
                "message.atomiclegacy.power.centrifuge",
                this.lastInput,
                this.currentConsumption,
                this.power,
                MAX_POWER,
                percent,
                this.completedCycles
        );
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> canAcceptInput(stack);
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case 6, 7 -> isSupportedUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canAcceptInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END;
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.progress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(containerId, playerInventory, this, this.menuData);
    }

    public boolean canAcceptInput(ItemStack stack) {
        return !stack.isEmpty() && recipeFor(stack).isPresent();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            drop(level, pos, this.items.get(slot));
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.completedCycles = tag.getInt("CompletedCycles");
    }

    /** [parity K3: centrifuge] TileEntityMachineCentrifuge.updateEntity (server part). */
    private void tickServer(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        updateUpgrades();
        if (this.power > 0 && this.progress > 0) {
            this.power -= this.currentConsumption;
            if (this.power < 0) {
                this.power = 0;
            }
        }
        Optional<RecipeHolder<CentrifugeRecipe>> holder = recipeFor(this.items.get(INPUT_SLOT));
        boolean progressing = this.power > 0 && holder.isPresent() && canProcess(holder.get().value());
        if (progressing) {
            this.progress += this.currentSpeed;
            if (this.progress >= BASE_PROCESSING_SPEED) {
                this.progress = 0;
                finishRecipe(holder.get().value());
                this.completedCycles++;
            }
        } else {
            this.progress = 0;
        }
        setLit(progressing);
        setChanged();
    }

    /** Client tick: CENTRIFUGE_LOOP audio driven by the synced isProgressing flag (blockstate LIT). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity centrifuge) {
        com.atomiclegacy.client.sound.CentrifugeClientSounds.tick(centrifuge, state.getValue(CentrifugeBlock.LIT));
    }

    private Optional<RecipeHolder<CentrifugeRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(HbmRecipeTypes.CENTRIFUGE.get(), new CentrifugeRecipe.Input(stack), this.level);
    }

    /** CentrifugeRecipes output i always goes to slot i + 2 (canProcess / processItem of 1.7.10). */
    private boolean canProcess(CentrifugeRecipe recipe) {
        List<ItemStack> out = recipe.results();
        for (int i = 0; i < Math.min(4, out.size()); i++) {
            ItemStack slot = this.items.get(OUTPUT_START + i);
            ItemStack result = out.get(i);
            if (slot.isEmpty() || result.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(slot, result) && slot.getCount() + result.getCount() <= result.getMaxStackSize()) {
                continue;
            }
            return false;
        }
        return true;
    }

    private void finishRecipe(CentrifugeRecipe recipe) {
        List<ItemStack> out = recipe.results();
        for (int i = 0; i < Math.min(4, out.size()); i++) {
            ItemStack result = out.get(i);
            if (result.isEmpty()) {
                continue;
            }
            ItemStack slot = this.items.get(OUTPUT_START + i);
            if (slot.isEmpty()) {
                this.items.set(OUTPUT_START + i, result.copy());
            } else {
                slot.grow(result.getCount());
            }
        }
        this.items.get(INPUT_SLOT).shrink(1);
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void updateUpgrades() {
        // UpgradeManagerNT: tiers of one type are summed and capped at getValidUpgrades() (3 each)
        int speedLevel = Math.min(3, upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED));
        int powerLevel = Math.min(3, upgradeLevel(MachineUpgradeItem.UpgradeType.POWER));
        int overdriveLevel = Math.min(3, upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE));

        int speed = 1 + speedLevel;
        int consumption = BASE_CONSUMPTION + speedLevel * BASE_CONSUMPTION;
        speed *= 1 + overdriveLevel * 5;
        consumption += overdriveLevel * BASE_CONSUMPTION * 50;
        consumption /= 1 + powerLevel;

        this.currentSpeed = speed;
        this.currentConsumption = consumption;
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof CentrifugeBlock && state.getValue(CentrifugeBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(CentrifugeBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static List<BlockPos> towerPowerConnectorPositions(BlockPos core) {
        ArrayList<BlockPos> connectors = new ArrayList<>(2 + TOWER_HEIGHT * 4);
        connectors.add(core.below().immutable());
        connectors.add(core.above(TOWER_HEIGHT).immutable());
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            BlockPos column = core.above(y);
            connectors.add(column.east().immutable());
            connectors.add(column.west().immutable());
            connectors.add(column.south().immutable());
            connectors.add(column.north().immutable());
        }
        return List.copyOf(connectors);
    }

    private static boolean isTowerPowerConnector(BlockPos core, BlockPos connectorPos, Direction machineSide) {
        if (connectorPos.equals(core.below())) {
            return machineSide == Direction.DOWN;
        }
        if (connectorPos.equals(core.above(TOWER_HEIGHT))) {
            return machineSide == Direction.UP;
        }
        if (!machineSide.getAxis().isHorizontal()) {
            return false;
        }
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            if (connectorPos.equals(core.above(y).relative(machineSide))) {
                return true;
            }
        }
        return false;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack.copy()
        ));
    }
}
