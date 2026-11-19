package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.MiningLaserBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.MachineUpgradeItem;
import com.atomiclegacy.inventory.container.MiningLaserMenu;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.util.ExternalCapabilitySafety;
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
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MiningLaserBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_END = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 30;
    public static final int SLOT_COUNT = 30;
    public static final int DATA_COUNT = 10;
    public static final long MAX_POWER = 100_000_000L;
    public static final int BASE_CONSUMPTION = 10_000;
    public static final int OIL_CAPACITY = 64_000;
    private static final int PUSH_PER_PORT = 16_000;
    private static final int[] INSERT_SLOTS = insertSlots();

    private static final int[] AUTOMATION_SLOTS = outputSlots();
    private static final Set<String> NULLIFIER_BAD_IDS = Set.of(
            "minecraft:dirt",
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:sand",
            "minecraft:sandstone",
            "minecraft:gravel",
            "minecraft:flint",
            "minecraft:snowball",
            "minecraft:wheat_seeds",
            "atomiclegacy:basalt",
            "atomiclegacy:stone_gneiss"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank oilTank = new HbmFluidTank(oil(), OIL_CAPACITY);
    private long power;
    private long lastInput;
    private boolean enabled; // [parity Q4: TileEntityMachineMiningLaser.isOn starts false]
    private boolean beam;
    private BlockPos target = BlockPos.ZERO;
    private BlockPos lastTarget = BlockPos.ZERO;
    private volatile RenderSnapshot renderSnapshot;
    private double breakProgress;
    private int currentConsumption = BASE_CONSUMPTION;
    private int range = 1;
    private int completedBlocks;

    public final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) MiningLaserBlockEntity.this.power;
                case 1 -> (int) MiningLaserBlockEntity.this.lastInput;
                case 2 -> MiningLaserBlockEntity.this.enabled ? 1 : 0;
                case 3 -> MiningLaserBlockEntity.this.beam ? 1 : 0;
                case 4 -> (int) Math.round(MiningLaserBlockEntity.this.breakProgress * 1000.0D);
                case 5 -> MiningLaserBlockEntity.this.range;
                case 6 -> MiningLaserBlockEntity.this.currentConsumption;
                case 7 -> MiningLaserBlockEntity.this.oilTank.amount();
                case 8 -> MiningLaserBlockEntity.this.completedBlocks;
                case 9 -> MiningLaserBlockEntity.this.target.getY();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MiningLaserBlockEntity.this.power = value;
                case 1 -> MiningLaserBlockEntity.this.lastInput = value;
                case 2 -> MiningLaserBlockEntity.this.enabled = value != 0;
                case 3 -> MiningLaserBlockEntity.this.beam = value != 0;
                case 4 -> MiningLaserBlockEntity.this.breakProgress = value / 1000.0D;
                case 5 -> MiningLaserBlockEntity.this.range = Math.max(1, value);
                case 6 -> MiningLaserBlockEntity.this.currentConsumption = Math.max(1, value);
                case 7 -> MiningLaserBlockEntity.this.oilTank.setAmount(value);
                case 8 -> MiningLaserBlockEntity.this.completedBlocks = value;
                case 9 -> MiningLaserBlockEntity.this.target = new BlockPos(MiningLaserBlockEntity.this.target.getX(), value, MiningLaserBlockEntity.this.target.getZ());
                default -> {
                }
            }
            MiningLaserBlockEntity.this.publishRenderSnapshot();
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MiningLaserBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.MINING_LASER.get(), pos, blockState);
        this.target = pos.below(2);
        publishRenderSnapshot();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MiningLaserBlockEntity laser) {
        if (level.isClientSide) return; // [parity Q4: all logic in the original is server-side]
        PowerNetworkManager.tickFromEndpoint(level, laser);
        laser.tickServer(level, state);
    }

    public HbmFluidTank oilTank() {
        return this.oilTank;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
        setChanged();
    }

    public int targetY() {
        return this.target.getY();
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(powerCablePos().immutable());
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPos.equals(powerCablePos()) && machineSide == Direction.UP;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : Math.min(Math.max(BASE_CONSUMPTION, this.currentConsumption), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.atomiclegacy.power.mining_laser",
                this.lastInput,
                this.currentConsumption,
                this.power,
                MAX_POWER,
                this.range,
                this.completedBlocks
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        return slot >= UPGRADE_START && slot < UPGRADE_END && MachineUpgradeItem.isMachineUpgrade(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS; // [parity Q4: getAccessibleSlotsFromSide = 9..29 on every side]
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false; // [parity Q4: isItemValidForSlot is the TileEntityMachineBase default (false)]
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
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("OilTank", this.oilTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Enabled", this.enabled);
        tag.putBoolean("Beam", this.beam);
        tag.putInt("TargetX", this.target.getX());
        tag.putInt("TargetY", this.target.getY());
        tag.putInt("TargetZ", this.target.getZ());
        tag.putDouble("BreakProgress", this.breakProgress);
        tag.putInt("CompletedBlocks", this.completedBlocks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.oilTank.load(tag.getCompound("OilTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.enabled = tag.getBoolean("Enabled");
        this.beam = tag.getBoolean("Beam");
        this.target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        this.breakProgress = tag.getDouble("BreakProgress");
        this.completedBlocks = tag.getInt("CompletedBlocks");
        publishRenderSnapshot();
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

    /** [parity Q4: TileEntityMachineMiningLaser.updateEntity] OVERDRIVE runs 1 + level full cycles per tick. */
    private void tickServer(Level level, BlockState state) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, this); // [parity U1: fluid net]
        if (!this.lastTarget.equals(this.target)) this.breakProgress = 0.0D;
        this.lastTarget = this.target;

        boolean shouldRun = this.enabled && !isMultiblockRedstonePowered(level);
        if (shouldRun && level instanceof ServerLevel serverLevel) {
            updateUpgrades();
            int cycles = 1 + upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
            int speed = speedLevel();
            int fortune = upgradeLevel(MachineUpgradeItem.UpgradeType.FORTUNE);
            for (int i = 0; i < cycles; i++) {
                if (this.power < this.currentConsumption) {
                    this.beam = false;
                    break;
                }
                this.power -= this.currentConsumption;
                if (this.target.getY() <= level.getMinBuildHeight()) {
                    this.target = new BlockPos(this.target.getX(), this.worldPosition.getY() - 2, this.target.getZ());
                }
                scan(level, this.range);
                BlockState targetState = level.getBlockState(this.target);
                if (targetState.liquid()) {
                    level.setBlock(this.target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    buildDam(serverLevel);
                    continue;
                }
                if (this.beam && canBreak(level, this.target)) {
                    this.breakProgress += getBreakSpeed(level, speed);
                    if (this.breakProgress < 1.0D) {
                        serverLevel.destroyBlockProgress(-1, this.target, (int) Math.floor(this.breakProgress * 10.0D));
                    } else {
                        breakTarget(serverLevel, targetState, fortune);
                        buildDam(serverLevel);
                    }
                }
            }
        } else {
            this.target = new BlockPos(this.target.getX(), this.worldPosition.getY() - 2, this.target.getZ());
            this.beam = false;
        }
        for (BlockPos port : fluidCablePorts()) {
            tryFillContainer(level, port);
        }
        setLit(level, state, this.beam);
        setChanged();
        syncRenderSnapshot(level);
    }

    /** getBreakSpeed: hardness * 15 / speed (float); 1 when that is 0. */
    private double getBreakSpeed(Level level, int speed) {
        float hardness = level.getBlockState(this.target).getDestroySpeed(level, this.target) * 15.0F / speed;
        if (hardness == 0.0F) return 1.0D;
        return 1.0D / hardness;
    }

    /** scan: first breakable, non-liquid block of the layer in x-then-z order; otherwise go one layer down. */
    private void scan(Level level, int range) {
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockPos next = new BlockPos(this.worldPosition.getX() + x, this.target.getY(), this.worldPosition.getZ() + z);
                if (level.getBlockState(next).liquid()) continue;
                if (canBreak(level, next)) {
                    this.target = next;
                    this.beam = true;
                    return;
                }
            }
        }
        this.beam = false;
        this.target = this.target.below();
    }

    /** breakBlock: crystallizer (peroxide, then sulfuric acid) > centrifuge > shredder > smelter, else normal drops with fortune. */
    private void breakTarget(ServerLevel level, BlockState targetState, int fortune) {
        BlockState dropState = targetState.is(Blocks.REDSTONE_ORE) ? Blocks.REDSTONE_ORE.defaultBlockState() : targetState;
        ItemStack stack = new ItemStack(dropState.getBlock().asItem());
        boolean normal = true;
        if (!stack.isEmpty()) {
            if (hasUpgrade(MachineUpgradeItem.UpgradeType.CRYSTALLIZER)) {
                ItemStack result = crystallizerResult(level, stack, "peroxide");
                if (result.isEmpty()) result = crystallizerResult(level, stack, "sulfuric_acid");
                if (!result.isEmpty()) {
                    spawnAtTarget(level, result);
                    normal = false;
                }
            } else if (hasUpgrade(MachineUpgradeItem.UpgradeType.CENTRIFUGE)) {
                var recipe = level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CENTRIFUGE.get()).stream()
                        .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                        .filter(r -> r.ingredient().test(stack)).findFirst();
                if (recipe.isPresent()) {
                    for (ItemStack out : recipe.get().results()) {
                        if (!out.isEmpty()) {
                            spawnAtTarget(level, out.copy());
                            normal = false;
                        }
                    }
                }
            } else if (hasUpgrade(MachineUpgradeItem.UpgradeType.SHREDDER)) {
                ItemStack result = level.getRecipeManager()
                        .getRecipeFor(HbmRecipeTypes.SHREDDER.get(), new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level)
                        .map(holder -> holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level.registryAccess()))
                        .orElse(ItemStack.EMPTY);
                if (!result.isEmpty() && !result.is(HbmItems.SCRAP.get())) {
                    spawnAtTarget(level, result.copy());
                    normal = false;
                }
            } else if (hasUpgrade(MachineUpgradeItem.UpgradeType.SMELTER)) {
                ItemStack result = level.getRecipeManager()
                        .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level)
                        .map(holder -> holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level.registryAccess()))
                        .orElse(ItemStack.EMPTY);
                if (!result.isEmpty()) {
                    spawnAtTarget(level, result.copy());
                    normal = false;
                }
            }
        }
        if (normal) {
            Block.dropResources(targetState, level, this.target, level.getBlockEntity(this.target), null, fortuneTool(level, fortune));
        }
        level.destroyBlock(this.target, false);
        collectDrops(level);
        if (hasUpgrade(MachineUpgradeItem.UpgradeType.SCREAM)) {
            level.playSound(null, this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D,
                    com.atomiclegacy.registry.HbmSoundEvents.BLOCK_SCREM.get(), net.minecraft.sounds.SoundSource.BLOCKS, 2000.0F, 1.0F);
        }
        this.breakProgress = 0.0D;
        this.completedBlocks++;
    }

    private ItemStack crystallizerResult(ServerLevel level, ItemStack stack, String acid) {
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get()).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(r -> r.inputCount() == 1 && r.acid().type().name().equals(acid) && r.ingredient().test(stack))
                .map(r -> r.result().copy())
                .findFirst().orElse(ItemStack.EMPTY);
    }

    private void spawnAtTarget(ServerLevel level, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D, stack));
    }

    private static ItemStack fortuneTool(ServerLevel level, int fortune) {
        if (fortune <= 0) return ItemStack.EMPTY;
        ItemStack tool = new ItemStack(net.minecraft.world.item.Items.STICK);
        tool.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), fortune);
        return tool;
    }

    /** tryFillContainer: chests, crates, safe and hopper only; stops after the first slot that moved anything. */
    private void tryFillContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) && !state.is(Blocks.HOPPER)
                && !state.is(HbmBlocks.CRATE_IRON.get()) && !state.is(HbmBlocks.CRATE_DESH.get())
                && !state.is(HbmBlocks.CRATE_STEEL.get()) && !state.is(HbmBlocks.SAFE.get())) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.Container inventory)) return;
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) continue;
            int prev = stack.getCount();
            ItemStack rest = addToInventory(inventory, stack.copy());
            this.items.set(slot, rest);
            if (rest.isEmpty() || rest.getCount() < prev) {
                inventory.setChanged();
                return;
            }
        }
    }

    /** InventoryUtil.tryAddItemToInventory over the whole inventory: merge first, then empty slots. */
    private static ItemStack addToInventory(net.minecraft.world.Container inv, ItemStack stack) {
        int limit = Math.min(inv.getMaxStackSize(), stack.getMaxStackSize());
        for (int i = 0; i < inv.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack cur = inv.getItem(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() < limit && inv.canPlaceItem(i, stack)) {
                int move = Math.min(stack.getCount(), limit - cur.getCount());
                cur.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < inv.getContainerSize() && !stack.isEmpty(); i++) {
            if (inv.getItem(i).isEmpty() && inv.canPlaceItem(i, stack)) {
                int move = Math.min(stack.getCount(), limit);
                inv.setItem(i, stack.copyWithCount(move));
                stack.shrink(move);
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    private void buildDam(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            placeBags(level, this.target.relative(direction));
        }
    }

    private void placeBags(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.liquid()) { // [parity Q4: getMaterial().isLiquid()]
            level.setBlock(pos, HbmBlocks.BARRICADE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void collectDrops(ServerLevel level) {
        double cx = this.target.getX() + 0.5D, cy = this.target.getY() + 0.5D, cz = this.target.getZ() + 0.5D;
        AABB box = new AABB(cx - 3.0D, cy - 1.0D, cz - 3.0D, cx + 3.0D, cy + 1.0D, cz + 3.0D); // [parity Q4: suckDrops]
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.isRemoved()) {
                continue;
            }
            if (hasUpgrade(MachineUpgradeItem.UpgradeType.NULLIFIER) && isNullifierBad(entity.getItem())) {
                entity.discard();
                continue;
            }
            if (entity.getItem().is(HbmBlocks.ORE_OIL.get().asItem())) {
                this.oilTank.setType(oil());
                this.oilTank.setAmount(Math.min(this.oilTank.capacity(), this.oilTank.amount() + 500));
                entity.discard();
                continue;
            }
            ItemStack remaining = insertOutput(entity.getItem().copy());
            if (remaining.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(remaining);
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(cx - 1.0D, cy - 1.0D, cz - 1.0D, cx + 1.0D, cy + 1.0D, cz + 1.0D))) {
            entity.igniteForSeconds(5.0F);
        }
    }

    private ItemStack insertOutput(ItemStack stack) {
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            ItemStack current = this.items.get(slot);
            if (ItemStack.isSameItemSameComponents(current, stack)) {
                int moved = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
                if (moved > 0) {
                    current.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.items.set(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }
        return stack;
    }

    private boolean canBreak(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        return !state.isAir()
                && !(state.getBlock() instanceof com.atomiclegacy.blocks.HbmGasBlock)
                && hardness >= 0.0F
                && !state.liquid()
                && !state.is(Blocks.BEDROCK); // [parity Q4: canBreak]
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!isFluidPort(queriedPos)) {
            return null;
        }
        Direction portDirection = directionFromCenter(queriedPos);
        if (side != null && side != portDirection) {
            return null;
        }
        return this.oilTank;
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        return isFluidPort(queriedPos);
    }

    private boolean isFluidPort(BlockPos queriedPos) {
        for (BlockPos port : fluidProxyPorts()) {
            if (port.equals(queriedPos)) {
                return true;
            }
        }
        return false;
    }


    private List<BlockPos> fluidProxyPorts() {
        return List.of(
                this.worldPosition.east(),
                this.worldPosition.west(),
                this.worldPosition.south(),
                this.worldPosition.north()
        );
    }

    private List<BlockPos> fluidCablePorts() {
        return List.of(
                this.worldPosition.east(2),
                this.worldPosition.west(2),
                this.worldPosition.south(2),
                this.worldPosition.north(2)
        );
    }

    private Direction directionFromCenter(BlockPos port) {
        int dx = port.getX() - this.worldPosition.getX();
        int dz = port.getZ() - this.worldPosition.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void updateUpgrades() {
        int speed = speedLevel();
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        this.range = 1 + upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT) * 2;
        // speedLevel() is 1 + SPEED; the original adds consumption * SPEED / 16
        this.currentConsumption = BASE_CONSUMPTION - BASE_CONSUMPTION * powerLevel / 16 + BASE_CONSUMPTION * (speed - 1) / 16;
    }

    private boolean isMultiblockRedstonePowered(Level level) {
        for (BlockPos port : fluidProxyPorts()) {
            BlockPos signalPos = port.relative(directionFromCenter(port).getOpposite());
            if (level.hasNeighborSignal(signalPos)) {
                return true;
            }
        }
        return false;
    }

    private int speedLevel() {
        return 1 + upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(1, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        // [parity Q4: UpgradeManagerNT caps from getValidUpgrades]
        return switch (type) {
            case SPEED, POWER, EFFECT -> Math.min(level, 12);
            case FORTUNE -> Math.min(level, 3);
            case OVERDRIVE -> Math.min(level, 9);
            default -> level;
        };
    }

    private boolean hasUpgrade(MachineUpgradeItem.UpgradeType type) {
        return upgradeLevel(type) > 0;
    }

    private static boolean isNullifierBad(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return NULLIFIER_BAD_IDS.contains(id);
    }

    private void setLit(Level level, BlockState state, boolean lit) {
        if (state.getBlock() instanceof MiningLaserBlock && state.getValue(MiningLaserBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(MiningLaserBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static HbmFluidDefinition oil() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    private BlockPos powerCablePos() {
        return this.worldPosition.above(2);
    }

    public static List<BlockPos> multiblockPositions(BlockPos corePos) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z).immutable());
                }
            }
        }
        return List.copyOf(positions);
    }

    public Vec3 renderTarget(float partialTick) {
        return Vec3.atCenterOf(this.target);
    }

    public Vec3 renderTargetRaw(float partialTick) {
        BlockPos renderTarget = this.renderSnapshot.target();
        return new Vec3(renderTarget.getX(), renderTarget.getY(), renderTarget.getZ());
    }

    public boolean beamActive() {
        return this.renderSnapshot.beamActive();
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    private void publishRenderSnapshot() {
        this.renderSnapshot = new RenderSnapshot(this.beam, this.target.immutable());
    }

    private void syncRenderSnapshot(Level level) {
        RenderSnapshot previous = this.renderSnapshot;
        RenderSnapshot next = new RenderSnapshot(this.beam, this.target.immutable());
        if (next.equals(previous)) {
            return;
        }
        this.renderSnapshot = next;
        if (!level.isClientSide && (previous.beamActive() || next.beamActive())) {
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public record RenderSnapshot(boolean beamActive, BlockPos target) {
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.machine_mining_laser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MiningLaserMenu(containerId, playerInventory, this, this.menuData);
    }

    private static int[] outputSlots() {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            slots.add(slot);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] insertSlots() {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = BATTERY_SLOT; slot < OUTPUT_START; slot++) {
            slots.add(slot);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    // [parity U1: fluid net] TileEntityMachineMiningLaser (side ports TilePortShapes.flare) as IFluidStandard*MK2 with TilePort nodes
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidSendingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{oilTank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return java.util.stream.Stream.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST).map(d -> new FluidPort(this.worldPosition.relative(d), d)).toList();
    }
}
