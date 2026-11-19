package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.ArcFurnaceBlock;
import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.client.sound.ArcFurnaceClientSounds;
import com.atomiclegacy.foundry.CrucibleUtil;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.foundry.FoundryShape;
import com.atomiclegacy.items.ArcElectrodeBurntItem;
import com.atomiclegacy.items.ArcElectrodeItem;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.MachineUpgradeItem;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.inventory.container.ArcFurnaceMenu;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.inventory.recipes.ArcFurnaceRecipe;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ArcFurnaceBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int ELECTRODE_1 = 0;
    public static final int ELECTRODE_3 = 3;
    public static final int BATTERY_SLOT = 3;
    public static final int UPGRADE_SLOT = 4;
    public static final int INPUT_START = 5;
    public static final int INPUT_END = 25;
    public static final int QUEUE_START = 25;
    public static final int QUEUE_END = 30;
    public static final int SLOT_COUNT = 30;
    private static final int LIQUID_DATA_START = 11;
    private static final int LIQUID_DATA_SLOTS = 4;
    public static final int DATA_COUNT = LIQUID_DATA_START + LIQUID_DATA_SLOTS * 2;
    public static final long MAX_POWER = 2_500_000L;
    public static final int MAX_LIQUID = FoundryShape.BLOCK.q(128);

    private static final int[] AUTOMATION_SLOTS = {
            0, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27, 28, 29
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final List<FoundryMaterialStack> liquids = new ArrayList<>();
    private long power;
    // [parity K3: TileEntityMachineArcFurnaceLarge keeps progress as a float advanced by 1F / duration]
    private float progress;
    private int processTime = 400;
    private int upgrade;
    private int delay;
    private float lid = 1.0F;
    private float previousLid = 1.0F;
    private float targetLid = 1.0F;
    private int lidApproachTicks;
    private boolean receivedRenderState;
    private boolean working;
    private boolean hasMaterial;
    private boolean liquidMode;
    private int completedCycles;
    private int lastRenderHash;
    private boolean hasRenderHash;
    private final byte[] syncedElectrodes = new byte[3];

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ArcFurnaceBlockEntity.this.power;
                case 1 -> Float.floatToIntBits(ArcFurnaceBlockEntity.this.progress);
                case 2 -> ArcFurnaceBlockEntity.this.processTime;
                case 3 -> Math.round(ArcFurnaceBlockEntity.this.lid * 1000.0F);
                case 4 -> ArcFurnaceBlockEntity.this.isLiquidMode() ? 1 : 0;
                case 5 -> ArcFurnaceBlockEntity.this.working ? 1 : 0;
                case 6 -> ArcFurnaceBlockEntity.this.hasMaterial ? 1 : 0;
                case 7 -> ArcFurnaceBlockEntity.this.upgrade;
                case 8 -> ArcFurnaceBlockEntity.this.liquidAmount();
                case 9 -> MAX_LIQUID;
                case 10 -> ArcFurnaceBlockEntity.this.completedCycles;
                default -> ArcFurnaceBlockEntity.this.liquidData(index);
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ArcFurnaceBlockEntity.this.power = value;
                case 1 -> ArcFurnaceBlockEntity.this.progress = Float.intBitsToFloat(value);
                case 2 -> ArcFurnaceBlockEntity.this.processTime = Math.max(1, value);
                case 3 -> ArcFurnaceBlockEntity.this.lid = value / 1000.0F;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ARC_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ArcFurnaceBlockEntity furnace) {
        if (level.isClientSide) {
            furnace.tickClient(level);
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, furnace);
        furnace.tickServer(level);
    }

    public ContainerData menuData() {
        return menuData;
    }

    public List<FoundryMaterialStack> liquids() {
        return List.copyOf(liquids);
    }

    public float lid(float partialTick) {
        return previousLid + (lid - previousLid) * partialTick;
    }

    public boolean working() {
        return working;
    }

    public boolean hasMaterial() {
        return hasMaterial;
    }

    public int electrodeState(int index) {
        if (index < 0 || index > 2) {
            return 0;
        }
        if (level != null && level.isClientSide) {
            return syncedElectrodes[index];
        }
        ItemStack stack = items.get(index);
        if (stack.getItem() instanceof ArcElectrodeBurntItem) {
            return 3;
        }
        if (stack.getItem() instanceof ArcElectrodeItem) {
            return working || ArcElectrodeItem.durability(stack) > 0 ? 2 : 1;
        }
        return 0;
    }

    public boolean isLiquidMode() {
        return liquidMode;
    }

    public void toggleLiquidMode() {
        liquidMode = !liquidMode;
        setChanged();
        sync();
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction dir = facing();
        Direction rot = arcFurnaceLeftSide(dir);
        return List.of(
                port(dir, rot, 3, 1), port(dir, rot, 3, -1),
                port(rot, dir, 3, 1), port(rot, dir, 3, -1),
                port(rot, dir, -3, 1), port(rot, dir, -3, -1)
        );
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        Direction dir = facing();
        Direction rot = arcFurnaceLeftSide(dir);
        return queriedPos.equals(port(dir, rot, 2, 1))
                || queriedPos.equals(port(dir, rot, 2, -1))
                || queriedPos.equals(port(rot, dir, 2, 1))
                || queriedPos.equals(port(rot, dir, 2, -1))
                || queriedPos.equals(port(rot, dir, -2, 1))
                || queriedPos.equals(port(rot, dir, -2, -1));
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos)
                && machineSide != null
                && machineSide.getAxis() != Direction.Axis.Y;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        // [parity K3: IEnergyReceiverMK2.getReceiverSpeed() = getMaxPower(); the original accepts up to the free buffer]
        return power >= MAX_POWER ? 0L : MAX_POWER - power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        power = Math.min(MAX_POWER, power + Math.max(0L, receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.atomiclegacy.power.arc_furnace", power, MAX_POWER);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = items.get(slot).split(amount);
        if (items.get(slot).isEmpty()) items.set(slot, ItemStack.EMPTY);
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) return ItemStack.EMPTY;
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) return;
        items.set(slot, stack);
        int limit = getMaxStackSize(stack);
        if (!stack.isEmpty() && stack.getCount() > limit) stack.setCount(limit);
        // [parity K3: upgradePlug only for an ItemMachineUpgrade put into slot 4]
        if (slot == UPGRADE_SLOT && level != null && !level.isClientSide && MachineUpgradeItem.isMachineUpgrade(stack)) {
            level.playSound(null, worldPosition, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // [parity K3: isItemValidForSlot - electrodes in 0-2, recipe inputs with an output for the current mode in 5+]
        if (slot >= 0 && slot < 3) return stack.getItem() instanceof ArcElectrodeItem;
        if (slot > UPGRADE_SLOT && slot < QUEUE_END) return recipeFor(stack).isPresent();
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot >= 0 && slot < 3) return stack.getItem() instanceof ArcElectrodeItem;
        return slot >= QUEUE_START && slot < QUEUE_END && recipeFor(stack).isPresent();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot < 3) return lid >= 1.0F && !(stack.getItem() instanceof ArcElectrodeItem);
        if (slot >= INPUT_START && slot < INPUT_END) return lid > 0.0F && recipeFor(stack).isEmpty();
        if (slot >= QUEUE_START) return recipeFor(stack).isEmpty();
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        items.replaceAll(stack -> ItemStack.EMPTY);
        liquids.clear();
        progress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.machine_arc_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ArcFurnaceMenu(containerId, inventory, this, menuData);
    }

    public boolean emptyMelt(Level level, BlockPos pos, Player player) {
        // [parity K3: MachineArcFurnaceLarge.onBlockActivated - a shovel always empties the melt as solid scraps]
        for (FoundryMaterialStack material : liquids) {
            ItemStack scrap = ScrapsItem.create(material, false);
            if (!player.getInventory().add(scrap)) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, scrap));
            }
        }
        liquids.clear();
        setChanged();
        return true;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack.copy()));
        }
        for (FoundryMaterialStack material : liquids) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, ScrapsItem.create(material, true)));
        }
        items.replaceAll(stack -> ItemStack.EMPTY);
        liquids.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) tag.put("Slot" + slot, items.get(slot).saveOptional(registries));
        tag.putLong("Power", power);
        tag.putFloat("Progress", progress);
        tag.putInt("ProcessTime", processTime);
        tag.putInt("Delay", delay);
        tag.putFloat("Lid", lid);
        tag.putBoolean("LiquidMode", isLiquidMode());
        tag.putInt("CompletedCycles", completedCycles);
        tag.putInt("LiquidCount", liquids.size());
        for (int index = 0; index < liquids.size(); index++) tag.put("Liquid" + index, liquids.get(index).save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.getBoolean("RenderSync")) {
            loadRenderData(tag);
            return;
        }
        for (int slot = 0; slot < SLOT_COUNT; slot++) items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        power = tag.getLong("Power");
        progress = tag.getFloat("Progress");
        processTime = Math.max(1, tag.getInt("ProcessTime"));
        delay = tag.getInt("Delay");
        lid = tag.getFloat("Lid");
        liquidMode = tag.getBoolean("LiquidMode");
        completedCycles = tag.getInt("CompletedCycles");
        liquids.clear();
        for (int index = 0; index < tag.getInt("LiquidCount"); index++) {
            FoundryMaterialStack material = FoundryMaterialStack.load(tag.getCompound("Liquid" + index));
            if (material != null) liquids.add(material);
        }
        targetLid = lid;
        previousLid = lid;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeRenderData(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickClient(Level level) {
        previousLid = lid;
        if (lidApproachTicks > 0) {
            lid += (targetLid - lid) / lidApproachTicks;
            lidApproachTicks--;
        } else {
            lid = targetLid;
        }

        ArcFurnaceClientSounds.tick(this, previousLid, lid);
        if (lid > previousLid && !(previousLid == 0.0F && lid == 1.0F)) {
            for (int count = 0; count < 3; count++) {
                level.addParticle(
                        HbmParticleTypes.ARC_FURNACE_SMOKE.get(),
                        worldPosition.getX() + 0.5D + level.random.nextGaussian() * 0.5D,
                        worldPosition.getY() + 4.0D,
                        worldPosition.getZ() + 0.5D + level.random.nextGaussian() * 0.5D,
                        0.0D, 0.0D, 0.0D
                );
            }
        }
        if (lid < previousLid && lid > 0.5F && hasMaterial && level.random.nextInt(5) == 0) {
            for (int count = 0; count < 2; count++) {
                level.addParticle(
                        HbmParticleTypes.RBMK_FIRE.get(),
                        worldPosition.getX() + 0.5D + level.random.nextGaussian() * 0.5D,
                        worldPosition.getY() + 2.75D,
                        worldPosition.getZ() + 0.5D + level.random.nextGaussian() * 0.5D,
                        50.0D, 0.0D, 0.0D
                );
            }
        }
    }

    private void tickServer(Level level) {
        previousLid = lid;
        upgrade = speedUpgrade();
        power = BatteryPackItem.dischargeIntoMachine(items.get(BATTERY_SLOT), power, MAX_POWER);
        working = false;
        if (lid > 0.0F) {
            moveQueueIntoGrid();
        }
        // [parity K3: TileEntityMachineArcFurnaceLarge.updateEntity]
        if (power > 0L) {
            boolean ingredients = hasIngredients();
            int consumption = (int) (1_000 * Math.pow(5, upgrade));
            int duration = 400 / (upgrade * 2 + 1);
            processTime = duration;
            if (ingredients && hasElectrodes() && delay <= 0 && liquids.isEmpty()) {
                if (lid > 0.0F) {
                    lid -= 1F / (60F / (upgrade * 0.5 + 1)); // float -= double, as in the original
                    if (lid < 0.0F) lid = 0.0F;
                    progress = 0.0F;
                } else if (power >= consumption) {
                    progress += 1F / duration;
                    working = true;
                    power -= consumption;
                    if (progress >= 1F) {
                        process();
                        progress = 0.0F;
                        delay = (int) (120 / (upgrade * 0.5 + 1));
                        completedCycles++;
                        HbmPollution.increment(level, worldPosition, HbmPollutionType.SOOT, 10.0D);
                    }
                }
            } else {
                if (delay > 0) delay--;
                progress = 0.0F;
                if (lid < 1.0F) {
                    lid += 1F / (60F / (upgrade * 0.5 + 1));
                    if (lid > 1.0F) lid = 1.0F;
                }
            }
            hasMaterial = ingredients;
        }
        if (!hasMaterial) hasMaterial = hasIngredients();
        pourMoltenOutput(level);
        liquids.removeIf(stack -> stack.amount() <= 0);
        setLit(level, working);
        setChanged();
        syncRenderState();
    }

    /** [parity K3: loadIngredients - moves items from the input queue to the main grid] */
    private void moveQueueIntoGrid() {
        for (int queue = QUEUE_START; queue < QUEUE_END; queue++) {
            ItemStack queued = items.get(queue);
            if (queued.isEmpty()) continue;
            Optional<ArcFurnaceRecipe> queuedRecipe = recipeFor(queued);
            if (queuedRecipe.isEmpty()) continue;
            int max = getMaxInputSize();
            int recipeMax = isLiquidMode() ? max : queued.getMaxStackSize() / queuedRecipe.get().solidOutput().getCount();
            max = Math.min(max, recipeMax);
            for (int input = INPUT_START; input < INPUT_END; input++) {
                ItemStack current = items.get(input);
                if (current.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(current, queued)) continue;
                int toMove = Math.min(Math.min(current.getMaxStackSize() - current.getCount(), queued.getCount()), max - current.getCount());
                if (toMove > 0) {
                    queued.shrink(toMove);
                    current.grow(toMove);
                }
                if (queued.isEmpty()) break;
            }
            if (!queued.isEmpty()) for (int input = INPUT_START; input < INPUT_END; input++) {
                if (!items.get(input).isEmpty()) continue;
                int toMove = Math.min(max, queued.getCount());
                items.set(input, queued.copyWithCount(toMove));
                queued.shrink(toMove);
                if (queued.isEmpty()) break;
            }
            if (queued.isEmpty()) items.set(queue, ItemStack.EMPTY);
        }
    }

    /** [parity K3: process - solid mode replaces each stack by output x stack size, liquid mode melts one item at a time] */
    private void process() {
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            ItemStack stack = items.get(slot);
            Optional<ArcFurnaceRecipe> recipeResult = recipeFor(stack);
            if (recipeResult.isEmpty()) continue;
            ArcFurnaceRecipe recipe = recipeResult.get();
            if (!isLiquidMode() && recipe.hasSolidOutput()) {
                int amount = stack.getCount();
                ItemStack output = recipe.solidOutput().copy();
                output.setCount(output.getCount() * amount);
                items.set(slot, output);
            }
            if (isLiquidMode() && recipe.hasLiquidOutput()) {
                int toAdd = total(recipe.liquidStacks());
                while (!items.get(slot).isEmpty()) {
                    if (liquidAmount() + toAdd <= MAX_LIQUID) {
                        items.get(slot).shrink(1);
                        for (FoundryMaterialStack output : recipe.liquidStacks()) addLiquid(output);
                    } else {
                        break;
                    }
                }
                if (items.get(slot).isEmpty()) items.set(slot, ItemStack.EMPTY);
            }
        }
        for (int index = 0; index < 3; index++) {
            ItemStack electrode = items.get(index);
            if (ArcElectrodeItem.damage(electrode)) {
                String variant = ArcElectrodeItem.variantId(electrode);
                items.set(index, HbmItems.variantStack(HbmItems.ARC_ELECTRODE_BURNT_ITEMS, variant));
            }
        }
    }

    private Optional<ArcFurnaceRecipe> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        Optional<RecipeHolder<ArcFurnaceRecipe>> registered = level.getRecipeManager().getRecipeFor(HbmRecipeTypes.ARC_FURNACE.get(), new ArcFurnaceRecipe.Input(List.of(stack)), level);
        if (registered.isPresent() && modeMatches(registered.get().value())) return registered.map(RecipeHolder::value);
        if (isLiquidMode() && !ArcFurnaceRecipe.dynamicOutputs(stack).isEmpty()) return dynamicRecipe(stack);
        if (!isLiquidMode()) {
            ItemStack output = ArcFurnaceRecipe.dynamicSolidOutput(stack, level);
            if (!output.isEmpty()) return dynamicSolidRecipe(stack, output);
        }
        return Optional.empty();
    }

    private boolean modeMatches(ArcFurnaceRecipe recipe) {
        return isLiquidMode() ? recipe.hasLiquidOutput() : recipe.hasSolidOutput();
    }

    private Optional<ArcFurnaceRecipe> dynamicRecipe(ItemStack stack) {
        List<FoundryMaterialStack> materials = ArcFurnaceRecipe.dynamicOutputs(stack);
        if (materials.isEmpty()) return Optional.empty();
        return Optional.of(new ArcFurnaceRecipe("dynamic", net.minecraft.world.item.crafting.Ingredient.of(stack.getItem()), 1, ItemStack.EMPTY,
                materials.stream().map(value -> new ArcFurnaceRecipe.MaterialOutput(value.material(), value.amount())).toList(), 400));
    }

    private Optional<ArcFurnaceRecipe> dynamicSolidRecipe(ItemStack stack, ItemStack output) {
        return Optional.of(new ArcFurnaceRecipe("dynamic_smelting", net.minecraft.world.item.crafting.Ingredient.of(stack.copyWithCount(1)), 1,
                output, List.of(), 400));
    }

    private boolean hasIngredients() {
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            if (recipeFor(items.get(slot)).isPresent()) return true;
        }
        return false;
    }

    private boolean hasElectrodes() {
        for (int index = 0; index < 3; index++) if (!(items.get(index).getItem() instanceof ArcElectrodeItem)) return false;
        return true;
    }

    private int speedUpgrade() {
        ItemStack stack = items.get(UPGRADE_SLOT);
        return MachineUpgradeItem.isMachineUpgrade(stack) && MachineUpgradeItem.upgradeType(stack) == MachineUpgradeItem.UpgradeType.SPEED
                ? Math.min(3, Math.max(0, MachineUpgradeItem.upgradeTier(stack))) : 0;
    }

    public int getMaxInputSize() {
        int upgrade = speedUpgrade(); // [parity K3: the original refreshes the upgrade level on both sides every tick]
        return upgrade == 0 ? 1 : upgrade == 1 ? 4 : upgrade == 2 ? 8 : 16;
    }

    /** [parity K3: SlotArcFurnace.isItemValid] */
    public boolean acceptsGridInput(ItemStack stack) {
        if (isLiquidMode()) return true;
        Optional<ArcFurnaceRecipe> recipe = recipeFor(stack);
        if (recipe.isPresent() && recipe.get().hasSolidOutput()) {
            ItemStack output = recipe.get().solidOutput();
            return output.getCount() * stack.getCount() <= output.getMaxStackSize() && stack.getCount() <= getMaxInputSize();
        }
        return false;
    }

    public float progressFraction() {
        return progress;
    }

    public int delay() {
        return delay;
    }

    public float lidState() {
        return lid;
    }

    private float lidStep() {
        return (float) (1F / (60F / (upgrade * 0.5 + 1)));
    }

    private void pourMoltenOutput(Level level) {
        if (liquids.isEmpty() || lid <= 0.0F) return;
        Direction dir = facing();
        FoundryMaterialStack poured = CrucibleUtil.pourFullStack(
                level,
                worldPosition.getX() + 0.5D + dir.getStepX() * 2.875D,
                worldPosition.getY() + 1.25D,
                worldPosition.getZ() + 0.5D + dir.getStepZ() * 2.875D,
                6.0D,
                true,
                liquids,
                FoundryShape.INGOT.q(1),
                material -> true
        );
        if (poured != null) setChanged();
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    public int liquidAmount() {
        return total(liquids);
    }

    private int liquidData(int index) {
        int relative = index - LIQUID_DATA_START;
        if (relative < 0 || relative >= LIQUID_DATA_SLOTS * 2) return 0;
        int stackIndex = relative / 2;
        if (stackIndex >= liquids.size()) return 0;
        FoundryMaterialStack stack = liquids.get(stackIndex);
        return relative % 2 == 0 ? stack.material().id() : stack.amount();
    }

    private void writeRenderData(CompoundTag tag) {
        tag.putBoolean("RenderSync", true);
        tag.putFloat("Lid", lid);
        tag.putBoolean("Working", working);
        tag.putBoolean("HasMaterial", hasMaterial);
        tag.putBoolean("LiquidMode", liquidMode);
        for (int index = 0; index < 3; index++) {
            tag.putByte("Electrode" + index, (byte) electrodeState(index));
        }
        tag.putInt("LiquidCount", liquids.size());
        for (int index = 0; index < liquids.size(); index++) {
            tag.put("Liquid" + index, liquids.get(index).save());
        }
    }

    private void loadRenderData(CompoundTag tag) {
        float nextLid = tag.getFloat("Lid");
        if (!receivedRenderState) {
            lid = nextLid;
            previousLid = nextLid;
            targetLid = nextLid;
            receivedRenderState = true;
        } else {
            targetLid = nextLid;
            lidApproachTicks = nextLid > 0.0F && nextLid < 1.0F ? 2 : 1;
        }
        working = tag.getBoolean("Working");
        hasMaterial = tag.getBoolean("HasMaterial");
        liquidMode = tag.getBoolean("LiquidMode");
        for (int index = 0; index < 3; index++) {
            syncedElectrodes[index] = tag.getByte("Electrode" + index);
        }
        liquids.clear();
        for (int index = 0; index < tag.getInt("LiquidCount"); index++) {
            FoundryMaterialStack material = FoundryMaterialStack.load(tag.getCompound("Liquid" + index));
            if (material != null) liquids.add(material);
        }
    }

    private void syncRenderState() {
        int hash = Float.floatToIntBits(lid);
        hash = 31 * hash + Boolean.hashCode(working);
        hash = 31 * hash + Boolean.hashCode(hasMaterial);
        hash = 31 * hash + Boolean.hashCode(liquidMode);
        for (int index = 0; index < 3; index++) hash = 31 * hash + electrodeState(index);
        for (FoundryMaterialStack stack : liquids) {
            hash = 31 * hash + stack.material().id();
            hash = 31 * hash + stack.amount();
        }
        if (!hasRenderHash || hash != lastRenderHash) {
            lastRenderHash = hash;
            hasRenderHash = true;
            sync();
        }
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int total(List<FoundryMaterialStack> stacks) {
        return stacks.stream().mapToInt(FoundryMaterialStack::amount).sum();
    }

    private void addLiquid(FoundryMaterialStack output) {
        for (int index = 0; index < liquids.size(); index++) {
            FoundryMaterialStack current = liquids.get(index);
            if (current.material() == output.material()) {
                liquids.set(index, new FoundryMaterialStack(current.material(), current.amount() + output.amount()));
                return;
            }
        }
        liquids.add(output);
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof ArcFurnaceBlock && state.getValue(ArcFurnaceBlock.LIT) != lit) level.setBlock(worldPosition, state.setValue(ArcFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }

    private Direction facing() {
        return getBlockState().hasProperty(LargeMachineBlock.FACING) ? getBlockState().getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
    }

    private static Direction arcFurnaceLeftSide(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case EAST -> Direction.SOUTH;
            case UP, DOWN -> throw new IllegalArgumentException("Arc furnace requires a horizontal facing: " + facing);
        };
    }

    private BlockPos port(Direction along, Direction side, int distance, int sideOffset) {
        return worldPosition.offset(along.getStepX() * distance + side.getStepX() * sideOffset, 0, along.getStepZ() * distance + side.getStepZ() * sideOffset).immutable();
    }
}
