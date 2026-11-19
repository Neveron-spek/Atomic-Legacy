package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.LegacyDeprecatedFurnaceBlock;
import com.atomiclegacy.blocks.LegacyDeprecatedFurnaceBlock.Kind;
import com.atomiclegacy.inventory.container.LegacyDeprecatedFurnaceMenu;
import com.atomiclegacy.inventory.recipes.BlastFurnaceRecipe;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * [parity d2] The three deprecated 1.7.10 furnaces that are still registered in the pinned original:
 * TileEntityDiFurnace (machine_difurnace_off/on), TileEntityDiFurnaceRTG (machine_difurnace_rtg_off/on) and
 * TileEntityRtgFurnace (machine_rtg_furnace_off/on). The update order of every kind follows the original line by
 * line, including the repeated RTGUtil.updateRTGs calls of the nuclear blast furnace.
 */
public final class LegacyDeprecatedFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int DATA_COUNT = 6;
    /** TileEntityDiFurnace.maxFuel / processingSpeed. */
    public static final int DIFURNACE_MAX_FUEL = 12_800;
    public static final int DIFURNACE_PROCESSING_SPEED = 400;
    /** TileEntityDiFurnaceRTG.timeRequired. */
    public static final int DIFURNACE_RTG_TIME_REQUIRED = 1_200;
    /** TileEntityRtgFurnace.processingSpeed. */
    public static final int RTG_FURNACE_PROCESSING_SPEED = 1_000;

    private static final int[] DIFURNACE_SLOTS = {0, 1, 2, 3};
    private static final int[] DIFURNACE_RTG_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] DIFURNACE_RTG_PELLETS = {3, 4, 5, 6, 7, 8};
    private static final int[] RTG_TOP = {0};
    private static final int[] RTG_BOTTOM = {4};
    private static final int[] RTG_SIDE = {1, 2, 3};

    private final Kind kind;
    private final NonNullList<ItemStack> items;
    private int progress;
    private int fuel;
    private int processSpeed;
    private byte sideFuel = 1;
    private byte sideUpper = 1;
    private byte sideLower = 1;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            LegacyDeprecatedFurnaceBlockEntity furnace = LegacyDeprecatedFurnaceBlockEntity.this;
            return switch (index) {
                case 0 -> furnace.progress;
                case 1 -> furnace.kind == Kind.DIFURNACE ? furnace.fuel
                        : furnace.kind == Kind.DIFURNACE_RTG ? furnace.processSpeed
                        : (furnace.rtgFurnaceHasPower() ? 1 : 0);
                case 2 -> furnace.sideFuel;
                case 3 -> furnace.sideUpper;
                case 4 -> furnace.sideLower;
                case 5 -> furnace.kind == Kind.DIFURNACE && furnace.diFurnaceCanProcess() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LegacyDeprecatedFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_DEPRECATED_FURNACE.get(), pos, state);
        this.kind = state.getBlock() instanceof LegacyDeprecatedFurnaceBlock block ? block.kind() : Kind.DIFURNACE;
        this.items = NonNullList.withSize(this.kind.slots(), ItemStack.EMPTY);
    }

    public Kind kind() {
        return this.kind;
    }

    public int progress() {
        return this.progress;
    }

    public int fuel() {
        return this.fuel;
    }

    public int processSpeed() {
        return this.processSpeed;
    }

    public void setFuel(int fuel) {
        this.fuel = fuel;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public byte side(int slot) {
        return this.kind == Kind.DIFURNACE
                ? (slot == 0 ? this.sideUpper : slot == 1 ? this.sideLower : this.sideFuel)
                : (slot == 0 ? this.sideUpper : this.sideLower);
    }

    /** ContainerDiFurnace / ContainerMachineDiFurnaceRTG#slotClick: right click on an empty input slot. */
    public void cycleSide(int slot) {
        if (slot == 0) {
            this.sideUpper = (byte) ((this.sideUpper + 1) % 6);
        } else if (slot == 1) {
            this.sideLower = (byte) ((this.sideLower + 1) % 6);
        } else if (slot == 2 && this.kind == Kind.DIFURNACE) {
            this.sideFuel = (byte) ((this.sideFuel + 1) % 6);
        }
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LegacyDeprecatedFurnaceBlockEntity furnace) {
        if (level.isClientSide) {
            return;
        }
        switch (furnace.kind) {
            case DIFURNACE -> furnace.tickDiFurnace(level);
            case DIFURNACE_RTG -> furnace.tickDiFurnaceRtg(level);
            case RTG_FURNACE -> furnace.tickRtgFurnace(level);
        }
    }

    // ---- TileEntityDiFurnace ----

    /** TileEntityDiFurnace#getItemPower. */
    public static int diFurnaceItemPower(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 200; // Items.coal of 1.7.10 covers both metas
        if (stack.is(Items.COAL_BLOCK)) return 2_000;
        if (stack.is(Items.LAVA_BUCKET)) return 12_800;
        if (stack.is(Items.BLAZE_ROD)) return 1_000;
        if (stack.is(Items.BLAZE_POWDER)) return 300;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(com.atomiclegacy.AtomicLegacy.MOD_ID)) {
            return 0;
        }
        if (path.equals("block_coke") || path.startsWith("block_coke_")) return 4_000;
        if (path.equals("lignite") || path.equals("powder_lignite")) return 150;
        if (path.equals("powder_coal")) return 200;
        if (path.startsWith("briquette_")) return 200;
        if (path.startsWith("coke_")) return 400;
        if (path.equals("solid_fuel")) return 400;
        return 0;
    }

    /** BlastFurnaceRecipes#getOutput: both orders, stack sizes ignored. */
    private ItemStack blastOutput(ItemStack in1, ItemStack in2) {
        if (this.level == null || in1.isEmpty() || in2.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (RecipeHolder<BlastFurnaceRecipe> holder : this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
            BlastFurnaceRecipe recipe = holder.value();
            if ((recipe.inputA().test(in1) && recipe.inputB().test(in2)) || (recipe.inputB().test(in1) && recipe.inputA().test(in2))) {
                return recipe.result().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean diFurnaceCanProcess() {
        if (this.items.get(0).isEmpty() || this.items.get(1).isEmpty()) return false;
        if (this.fuel <= 0) return false;
        ItemStack output = blastOutput(this.items.get(0), this.items.get(1));
        if (output.isEmpty()) return false;
        ItemStack current = this.items.get(3);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItem(current, output)) return false;
        return current.getCount() + output.getCount() <= current.getMaxStackSize();
    }

    private void diFurnaceProcessItem() {
        ItemStack output = blastOutput(this.items.get(0), this.items.get(1));
        ItemStack current = this.items.get(3);
        if (current.isEmpty()) {
            this.items.set(3, output.copy());
        } else if (ItemStack.isSameItem(current, output)) {
            current.grow(output.getCount());
        }
        for (int i = 0; i < 2; i++) {
            removeItem(i, 1);
        }
    }

    private void tickDiFurnace(Level level) {
        boolean extension = level.getBlockState(this.worldPosition.above()).is(HbmBlocks.MACHINE_DIFURNACE_EXTENSION.get());
        boolean markDirty = false;

        ItemStack fuelStack = this.items.get(2);
        int itemPower = diFurnaceItemPower(fuelStack);
        if (itemPower > 0 && this.fuel <= DIFURNACE_MAX_FUEL - itemPower) {
            this.fuel += itemPower;
            markDirty = true;
            ItemStack remainder = fuelStack.getCraftingRemainingItem();
            fuelStack.shrink(1);
            if (fuelStack.isEmpty()) {
                this.items.set(2, remainder);
            }
        }

        if (diFurnaceCanProcess()) {
            this.fuel -= 1;
            this.progress += extension ? 3 : 1;
            if (this.progress >= DIFURNACE_PROCESSING_SPEED) {
                this.progress -= DIFURNACE_PROCESSING_SPEED;
                diFurnaceProcessItem();
                markDirty = true;
            }
            if (this.fuel < 0) {
                this.fuel = 0;
            }
        } else {
            this.progress = 0;
        }

        boolean trigger = true;
        if (diFurnaceCanProcess() && this.progress == 0) {
            trigger = false;
        }
        if (trigger) {
            markDirty = true;
            setLit(level, this.progress > 0);
        }
        if (markDirty) {
            setChanged();
        }
    }

    // ---- TileEntityDiFurnaceRTG ----

    /** RTGUtil#updateRTGs: sums the heat and advances the decay of every pellet. */
    private int updateRtgs(int[] slots) {
        int newHeat = 0;
        for (int slot : slots) {
            ItemStack stack = this.items.get(slot);
            if (stack.getItem() instanceof RtgPelletItem pellet) {
                newHeat += pellet.heat(stack);
                this.items.set(slot, pellet.burnTick(stack));
            }
        }
        return newHeat;
    }

    /** TileEntityDiFurnaceRTG#hasPower: every call runs RTGUtil.updateRTGs, exactly like the original. */
    private boolean diFurnaceRtgHasPower() {
        this.processSpeed = (short) updateRtgs(DIFURNACE_RTG_PELLETS);
        return this.processSpeed >= 15;
    }

    private boolean diFurnaceRtgCanProcess() {
        if ((this.items.get(0).isEmpty() || this.items.get(1).isEmpty()) && !diFurnaceRtgHasPower()) return false;
        ItemStack result = blastOutput(this.items.get(0), this.items.get(1));
        ItemStack current = this.items.get(2);
        if (result.isEmpty()) return false;
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItem(current, result)) return false;
        if (current.getCount() + result.getCount() > 64) return false;
        if (current.getCount() < 64 && current.getCount() < current.getMaxStackSize()) return true;
        return current.getCount() < result.getMaxStackSize();
    }

    private void diFurnaceRtgProcessItem() {
        if (!diFurnaceRtgCanProcess()) {
            return;
        }
        ItemStack output = blastOutput(this.items.get(0), this.items.get(1));
        ItemStack current = this.items.get(2);
        if (current.isEmpty()) {
            this.items.set(2, output.copy());
        } else if (ItemStack.isSameItem(current, output)) {
            current.grow(output.getCount());
        }
        for (int i = 0; i < 2; i++) {
            this.items.get(i).shrink(1);
            if (this.items.get(i).isEmpty()) {
                this.items.set(i, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    private void tickDiFurnaceRtg(Level level) {
        if (diFurnaceRtgCanProcess() && diFurnaceRtgHasPower()) {
            this.progress += this.processSpeed;
            if (this.progress >= DIFURNACE_RTG_TIME_REQUIRED) {
                diFurnaceRtgProcessItem();
                this.progress = 0;
            }
        } else {
            this.progress = 0;
        }
        setLit(level, this.progress > 0 || (diFurnaceRtgCanProcess() && diFurnaceRtgHasPower()));
        setChanged(); // pellet decay lives in the stacks
    }

    // ---- TileEntityRtgFurnace ----

    /** RTGUtil#hasHeat over the three pellet slots. */
    public boolean rtgFurnaceHasPower() {
        if (this.kind != Kind.RTG_FURNACE) {
            return false;
        }
        for (int slot : RTG_SIDE) {
            if (this.items.get(slot).getItem() instanceof RtgPelletItem) {
                return true;
            }
        }
        return false;
    }

    private ItemStack smeltingResult(ItemStack input) {
        if (this.level == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return this.level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), this.level)
                .map(holder -> holder.value().getResultItem(this.level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
    }

    private boolean rtgFurnaceCanProcess() {
        if (this.items.get(0).isEmpty()) return false;
        ItemStack result = smeltingResult(this.items.get(0));
        if (result.isEmpty()) return false;
        ItemStack current = this.items.get(4);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItem(current, result)) return false;
        if (current.getCount() < 64 && current.getCount() < current.getMaxStackSize()) return true;
        return current.getCount() < result.getMaxStackSize();
    }

    private void rtgFurnaceProcessItem() {
        if (!rtgFurnaceCanProcess()) {
            return;
        }
        ItemStack result = smeltingResult(this.items.get(0));
        ItemStack current = this.items.get(4);
        if (current.isEmpty()) {
            this.items.set(4, result.copy());
        } else if (ItemStack.isSameItem(current, result)) {
            current.grow(result.getCount());
        }
        this.items.get(0).shrink(1);
        if (this.items.get(0).isEmpty()) {
            this.items.set(0, ItemStack.EMPTY);
        }
    }

    private void tickRtgFurnace(Level level) {
        boolean dirty = false;
        if (rtgFurnaceHasPower() && rtgFurnaceCanProcess()) {
            this.progress += updateRtgs(RTG_SIDE);
            if (this.progress >= RTG_FURNACE_PROCESSING_SPEED) {
                this.progress = 0;
                rtgFurnaceProcessItem();
                dirty = true;
            }
        } else {
            this.progress = 0;
            updateRtgs(RTG_SIDE);
        }
        boolean trigger = true;
        if (rtgFurnaceHasPower() && rtgFurnaceCanProcess() && this.progress == 0) {
            trigger = false;
        }
        if (trigger) {
            dirty = true;
            setLit(level, this.progress > 0);
        }
        if (dirty) {
            setChanged();
        }
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(LegacyDeprecatedFurnaceBlock.LIT) && state.getValue(LegacyDeprecatedFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(LegacyDeprecatedFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    // ---- inventory ----

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (this.kind) {
            case DIFURNACE -> slot != 3;
            case DIFURNACE_RTG -> {
                if (slot == 2) yield false;
                if (stack.getItem() instanceof RtgPelletItem) yield slot > 2;
                yield true;
            }
            case RTG_FURNACE -> true;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (this.kind) {
            case DIFURNACE -> DIFURNACE_SLOTS;
            case DIFURNACE_RTG -> DIFURNACE_RTG_SLOTS;
            case RTG_FURNACE -> side == Direction.DOWN ? RTG_BOTTOM : side == Direction.UP ? RTG_TOP : RTG_SIDE;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        int side = direction == null ? -1 : direction.get3DDataValue();
        switch (this.kind) {
            case DIFURNACE -> {
                if (slot == 0 && this.sideUpper != side) return false;
                if (slot == 1 && this.sideLower != side) return false;
                if (slot == 2 && this.sideFuel != side) return false;
                if (slot == 3) return false;
            }
            case DIFURNACE_RTG -> {
                if (slot == 0 && this.sideUpper != side) return false;
                if (slot == 1 && this.sideLower != side) return false;
            }
            case RTG_FURNACE -> {
            }
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return switch (this.kind) {
            case DIFURNACE -> slot == 3;
            case DIFURNACE_RTG -> slot > 2 ? !(stack.getItem() instanceof RtgPelletItem) : slot == 2;
            case RTG_FURNACE -> direction != Direction.DOWN || slot != 1 || stack.is(Items.BUCKET);
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        this.items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(switch (this.kind) {
            case DIFURNACE -> "container.atomiclegacy.difurnace";
            case DIFURNACE_RTG -> "container.atomiclegacy.difurnace_rtg";
            case RTG_FURNACE -> "container.atomiclegacy.rtg_furnace";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LegacyDeprecatedFurnaceMenu(containerId, playerInventory, this, this.menuData, this.kind, this.worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        switch (this.kind) {
            case DIFURNACE -> {
                tag.putInt("powerTime", this.fuel);
                tag.putShort("cookTime", (short) this.progress);
                tag.putByteArray("modes", new byte[] {this.sideFuel, this.sideUpper, this.sideLower});
            }
            case DIFURNACE_RTG -> {
                tag.putShort("progress", (short) this.progress);
                tag.putShort("speed", (short) this.processSpeed);
                tag.putByteArray("modes", new byte[] {this.sideUpper, this.sideLower});
            }
            case RTG_FURNACE -> {
                // TileEntityRtgFurnace writes "cookTime" but reads "CookTime": the progress never survives a reload
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        byte[] modes = tag.getByteArray("modes");
        switch (this.kind) {
            case DIFURNACE -> {
                this.fuel = tag.getInt("powerTime");
                this.progress = tag.getShort("cookTime");
                if (modes.length >= 3) {
                    this.sideFuel = modes[0];
                    this.sideUpper = modes[1];
                    this.sideLower = modes[2];
                }
            }
            case DIFURNACE_RTG -> {
                this.progress = tag.getShort("progress");
                this.processSpeed = tag.getShort("speed");
                if (modes.length >= 2) {
                    this.sideUpper = modes[0];
                    this.sideLower = modes[1];
                }
            }
            case RTG_FURNACE -> {
            }
        }
    }
}
