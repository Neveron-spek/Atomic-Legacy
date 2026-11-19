package com.atomiclegacy.tileentity;

import com.atomiclegacy.inventory.container.BreederReactorMenu;
import com.atomiclegacy.inventory.recipes.BreederReactorRecipe;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

public class BreederReactorBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int DATA_COUNT = 3;
    public static final int PROGRESS_SCALE = 10_000;

    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int flux;
    private float progress;
    private int requiredFlux;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BreederReactorBlockEntity.this.flux;
                case 1 -> Math.round(BreederReactorBlockEntity.this.progress * PROGRESS_SCALE);
                case 2 -> BreederReactorBlockEntity.this.requiredFlux;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BreederReactorBlockEntity.this.flux = value;
                case 1 -> BreederReactorBlockEntity.this.progress = value / (float) PROGRESS_SCALE;
                case 2 -> BreederReactorBlockEntity.this.requiredFlux = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BreederReactorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BREEDER_REACTOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BreederReactorBlockEntity breeder) {
        breeder.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public float progress() {
        return this.progress;
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
        setChangedAndSync(false); // [parity h1] 1.7.10 keeps progress; canProcess() resets it on the next tick
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
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT; // [parity h1] TileEntityMachineReactorBreeding#isItemValidForSlot: i == 0
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT; // TileEntityMachineBase#canInsertItem -> isItemValidForSlot
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
        this.progress = 0.0F;
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.breeder_reactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BreederReactorMenu(containerId, playerInventory, this, this.menuData);
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
        tag.putInt("Flux", this.flux);
        tag.putFloat("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.flux = tag.getInt("Flux");
        this.progress = tag.getFloat("Progress");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickServer(Level level) {
        this.flux = researchReactorFlux(level, this.worldPosition);
        Optional<RecipeHolder<BreederReactorRecipe>> holder = recipeFor(this.items.get(INPUT_SLOT));
        this.requiredFlux = holder.map(recipe -> recipe.value().flux()).orElse(0);

        if (holder.isEmpty() || !canProcess(holder.get().value())) {
            if (this.progress != 0.0F) {
                this.progress = 0.0F;
                setChangedAndSync(true);
            } else {
                setChanged();
            }
            return;
        }

        BreederReactorRecipe recipe = holder.get().value();
        // [parity M2: breeder] 0.0025F * (flux / recipe.flux) with integer division, as in the original
        this.progress += 0.0025F * (this.flux / recipe.flux());
        if (this.progress >= 1.0F) {
            this.progress = 0.0F;
            finishRecipe(recipe);
        }
        setChangedAndSync(true);
    }

    /**
     * [parity M2: breeder] TileEntityMachineReactorBreeding#getInteractions: only horizontally adjacent
     * reactor_research blocks (core or dummy part) contribute their core's totalFlux.
     */
    public static int researchReactorFlux(Level level, BlockPos pos) {
        int sum = 0;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            BlockPos core = neighbor;
            if (level.getBlockEntity(neighbor) instanceof MachineDummyBlockEntity dummy) {
                core = dummy.getCorePos();
            }
            if (level.getBlockState(core).is(com.atomiclegacy.registry.HbmBlocks.MACHINE_REACTOR_SMALL.get())
                    && level.getBlockEntity(core) instanceof ResearchReactorBlockEntity reactor) {
                sum += reactor.totalFlux();
            }
        }
        return sum;
    }

    private Optional<RecipeHolder<BreederReactorRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(HbmRecipeTypes.BREEDER_REACTOR.get(), new BreederReactorRecipe.Input(stack), this.level);
    }

    private boolean canProcess(BreederReactorRecipe recipe) {
        if (this.flux < recipe.flux()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        ItemStack result = recipe.result();
        // [parity h1] canProcess: same item and stackSize < maxStackSize
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() < output.getMaxStackSize());
    }

    private void finishRecipe(BreederReactorRecipe recipe) {
        ItemStack input = this.items.get(INPUT_SLOT);
        input.shrink(1);
        if (input.isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        ItemStack output = this.items.get(OUTPUT_SLOT);
        ItemStack result = recipe.result().copy();
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result);
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            output.grow(result.getCount());
        }
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (sync && this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
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
