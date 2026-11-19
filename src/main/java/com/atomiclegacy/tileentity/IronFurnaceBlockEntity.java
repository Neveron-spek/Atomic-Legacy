package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.LegacyFurnaceBlock;
import com.atomiclegacy.items.MachineUpgradeItem;
import com.atomiclegacy.inventory.container.IronFurnaceMenu;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.pollution.HbmPollutionConstants;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.atomiclegacy.util.FuelItemConsumption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

public class IronFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_A_SLOT = 1;
    public static final int FUEL_B_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int UPGRADE_SLOT = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 6;
    public static final int BASE_TIME = 160; // [parity K3] TileEntityFurnaceIron.baseTime
    // [parity K3] TileEntityFurnaceIron burnModule
    public static final com.atomiclegacy.util.LegacyBurnModule BURN_MODULE = new com.atomiclegacy.util.LegacyBurnModule()
            .time(com.atomiclegacy.util.LegacyBurnModule.LIGNITE, 1.25).time(com.atomiclegacy.util.LegacyBurnModule.COAL, 1.25)
            .time(com.atomiclegacy.util.LegacyBurnModule.COKE, 1.5).time(com.atomiclegacy.util.LegacyBurnModule.SOLID, 2)
            .time(com.atomiclegacy.util.LegacyBurnModule.ROCKET, 2).time(com.atomiclegacy.util.LegacyBurnModule.BALEFIRE, 2);
    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, FUEL_A_SLOT, FUEL_B_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int maxBurnTime;
    private int burnTime;
    private int progress;
    private int processingTime = BASE_TIME;
    private boolean wasOn;
    private boolean canSmelt;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> IronFurnaceBlockEntity.this.maxBurnTime;
                case 1 -> IronFurnaceBlockEntity.this.burnTime;
                case 2 -> IronFurnaceBlockEntity.this.progress;
                case 3 -> IronFurnaceBlockEntity.this.processingTime;
                case 4 -> IronFurnaceBlockEntity.this.wasOn ? 1 : 0;
                case 5 -> IronFurnaceBlockEntity.this.canSmelt ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> IronFurnaceBlockEntity.this.maxBurnTime = value;
                case 1 -> IronFurnaceBlockEntity.this.burnTime = value;
                case 2 -> IronFurnaceBlockEntity.this.progress = value;
                case 3 -> IronFurnaceBlockEntity.this.processingTime = Math.max(1, value);
                case 4 -> IronFurnaceBlockEntity.this.wasOn = value != 0;
                case 5 -> IronFurnaceBlockEntity.this.canSmelt = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public IronFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.IRON_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IronFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            furnace.tickServer(level);
        }
    }

    public static int fuelDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return BURN_MODULE.getBurnTime(stack); // [parity K3] ModuleBurnTime#getBurnTime
    }

    public static boolean isSpeedUpgrade(ItemStack stack) {
        return MachineUpgradeItem.isMachineUpgrade(stack)
                && MachineUpgradeItem.upgradeType(stack) == MachineUpgradeItem.UpgradeType.SPEED;
    }

    public static boolean hasSmeltingRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
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
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }

        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> this.level == null || hasSmeltingRecipe(this.level, stack);
            case FUEL_A_SLOT, FUEL_B_SLOT -> fuelDuration(stack) > 0;
            case UPGRADE_SLOT -> isSpeedUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != OUTPUT_SLOT && slot != UPGRADE_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
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
        return Component.translatable("container.atomiclegacy.furnace_iron");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IronFurnaceMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData menuData() {
        return this.menuData;
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
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("Progress", this.progress);
        tag.putInt("ProcessingTime", this.processingTime);
        tag.putBoolean("WasOn", this.wasOn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.burnTime = tag.getInt("BurnTime");
        this.progress = tag.getInt("Progress");
        this.processingTime = Math.max(1, tag.getInt("ProcessingTime"));
        this.wasOn = tag.getBoolean("WasOn");
    }

    private void tickServer(Level level) {
        // [parity K3] TileEntityFurnaceIron#updateEntity
        this.processingTime = currentProcessingTime();
        this.wasOn = false;

        if (this.burnTime <= 0) {
            tryConsumeFuel();
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipe = currentRecipe(level);
        this.canSmelt = this.burnTime > 0 && recipe.isPresent();
        if (this.canSmelt) {
            this.wasOn = true;
            this.progress++;
            this.burnTime--;

            if (this.progress % 15 == 0) {
                level.playSound(null, this.worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 0.5F);
            }

            if (this.progress >= this.processingTime) {
                finishSmelting(level, recipe.get());
                this.progress = 0;
            }

            if (level.getGameTime() % 20L == 0L) {
                HbmPollution.increment(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND);
            }
        } else {
            this.progress = 0;
        }

        setLit(level, this.wasOn);
        setChanged();
    }

    private int currentProcessingTime() {
        ItemStack upgrade = this.items.get(UPGRADE_SLOT);
        int speed = isSpeedUpgrade(upgrade) ? Math.min(3, Math.max(0, MachineUpgradeItem.upgradeTier(upgrade))) : 0;
        return BASE_TIME - ((BASE_TIME / 2) * speed / 3);
    }

    private Optional<RecipeHolder<SmeltingRecipe>> currentRecipe(Level level) {
        ItemStack input = this.items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> holder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = holder.get().value().assemble(recipeInput, level.registryAccess());
        return canPlaceOutput(result) ? holder : Optional.empty();
    }

    private boolean tryConsumeFuel() {
        for (int slot = FUEL_A_SLOT; slot <= FUEL_B_SLOT; slot++) {
            ItemStack fuel = this.items.get(slot);
            int duration = fuelDuration(fuel);
            if (duration > 0) {
                this.maxBurnTime = this.burnTime = duration;
                ItemStack remainder = FuelItemConsumption.consumeOne(fuel);
                if (fuel.isEmpty()) {
                    this.items.set(slot, remainder);
                }
                return true;
            }
        }
        return false;
    }

    private void finishSmelting(Level level, RecipeHolder<SmeltingRecipe> recipe) {
        SingleRecipeInput input = new SingleRecipeInput(this.items.get(INPUT_SLOT));
        ItemStack result = recipe.value().assemble(input, level.registryAccess());
        addOutput(result);
        this.items.get(INPUT_SLOT).shrink(1);
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canPlaceOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return stack.getCount() <= stack.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize(output));
    }

    private void addOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(output, stack)) {
            output.grow(stack.getCount());
        }
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(LegacyFurnaceBlock.LIT) && state.getValue(LegacyFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(LegacyFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.items.size();
    }

    private static String registryPath(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }
}
