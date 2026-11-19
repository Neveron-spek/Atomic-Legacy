package com.atomiclegacy.tileentity;

import com.atomiclegacy.entity.SoyuzEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.SoyuzLauncherMenu;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.SoyuzItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * [parity P2: soyuz_launcher] Port of the deprecated (but creative-listed) 1.7.10
 * TileEntitySoyuzLauncher: 27 slots, 128 000 mB kerosene + oxygen tanks filled only
 * from containers, 1 000 000 HE buffer charged only from the battery slot (the
 * TileEntityProxyCombo ring never subscribes to a cable network), 600-tick countdown
 * with an alarm every 100 ticks, satellite (mode 0) / cargo (mode 1) launch.
 * Quirks kept: canLaunch() checks kerosene only, launch costs 75 % of max power and the
 * same amount of both fluids, starting/countdown are not saved.
 */
public final class SoyuzLauncherBlockEntity extends BlockEntity implements Container, MenuProvider, MachineInventory {
    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_SATELLITE = 2;
    public static final int SLOT_ORBITAL_MODULE = 3;
    public static final int SLOT_KEROSENE_IN = 4;
    public static final int SLOT_KEROSENE_OUT = 5;
    public static final int SLOT_OXYGEN_IN = 6;
    public static final int SLOT_OXYGEN_OUT = 7;
    public static final int SLOT_BATTERY = 8;
    public static final int SLOT_CARGO_START = 9;
    public static final int SLOT_COUNT = 27;
    public static final long MAX_POWER = 1_000_000L;
    public static final int MAX_COUNT = 600;
    public static final int TANK_CAPACITY = 128_000;
    public static final int DATA_COUNT = 11;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank keroseneTank = new HbmFluidTank(fluid("kerosene"), TANK_CAPACITY);
    private final HbmFluidTank oxygenTank = new HbmFluidTank(fluid("oxygen"), TANK_CAPACITY);
    private long power;
    /** 0: satellite, 1: cargo */
    private byte mode;
    private boolean starting;
    private int countdown = MAX_COUNT;
    /** Client copy of getType(): -1 without a rocket, otherwise the rocket's damage (skin). */
    private int rocketType = -1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) power;
                case 1 -> mode;
                case 2 -> starting ? 1 : 0;
                case 3 -> countdown;
                case 4 -> keroseneTank.amount();
                case 5 -> oxygenTank.amount();
                case 6 -> hasRocket() ? 1 : 0;
                case 7 -> designator();
                case 8 -> orbital();
                case 9 -> satellite();
                case 10 -> getFuelRequired();
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

    public SoyuzLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SOYUZ_LAUNCHER.get(), pos, state);
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SoyuzLauncherBlockEntity launcher) {
        if (level.isClientSide) {
            com.atomiclegacy.client.sound.SoyuzLauncherClientEffects.tick(launcher);
            return;
        }
        long before = launcher.syncSignature();
        launcher.drainContainer(SLOT_KEROSENE_IN, SLOT_KEROSENE_OUT, launcher.keroseneTank);
        launcher.drainContainer(SLOT_OXYGEN_IN, SLOT_OXYGEN_OUT, launcher.oxygenTank);
        launcher.power = BatteryPackItem.dischargeIntoMachine(launcher.items.get(SLOT_BATTERY), launcher.power, MAX_POWER);

        if (!launcher.starting || !launcher.canLaunch()) {
            launcher.countdown = MAX_COUNT;
            launcher.starting = false;
        } else if (launcher.countdown > 0) {
            launcher.countdown--;
            if (launcher.countdown % 100 == 0 && launcher.countdown > 0) {
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HbmSoundEvents.HATCH_ALARM.get(),
                        SoundSource.BLOCKS, 100.0F, 1.1F);
            }
        } else {
            launcher.liftOff();
        }
        launcher.rocketType = launcher.rocketTypeNow();
        launcher.setChanged();
        // networkPackNT(250) every tick; a block update while counting down or on change.
        if (before != launcher.syncSignature() || level.getGameTime() % 20L == 0L) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private long syncSignature() {
        long h = this.power;
        h = h * 31 + this.keroseneTank.amount();
        h = h * 31 + this.oxygenTank.amount();
        h = h * 31 + this.mode;
        h = h * 31 + (this.starting ? 1 : 0);
        h = h * 31 + this.countdown;
        return h * 31 + this.rocketType;
    }

    private void drainContainer(int in, int out, HbmFluidTank tank) {
        ItemStack input = this.items.get(in);
        if (input.isEmpty()) return;
        HbmFluidDefinition type = tank.type();
        boolean moved = HbmFluidContainerTransfer.drainIntoTank(input, tank, candidate -> candidate == type,
                output -> output.isEmpty() || this.items.get(out).isEmpty()
                        || ItemStack.isSameItemSameComponents(this.items.get(out), output)
                        && this.items.get(out).getCount() + output.getCount() <= this.items.get(out).getMaxStackSize(),
                output -> {
                    if (output.isEmpty()) return;
                    if (this.items.get(out).isEmpty()) this.items.set(out, output.copy());
                    else this.items.get(out).grow(output.getCount());
                });
        if (moved && input.isEmpty()) this.items.set(in, ItemStack.EMPTY);
    }

    public void setMode(int value) {
        this.mode = (byte) value;
        setChanged();
    }

    public void startCountdown() {
        if (canLaunch()) this.starting = true;
    }

    private void liftOff() {
        this.starting = false;
        int req = getFuelRequired();
        int pow = getPowerRequired();
        int targetX = 0;
        int targetZ = 0;
        if (this.mode == 1) {
            CompoundTag tag = customData(this.items.get(SLOT_DESIGNATOR));
            targetX = tag.getInt("xCoord");
            targetZ = tag.getInt("zCoord");
        }
        SoyuzEntity soyuz = new SoyuzEntity(this.level, this.mode, targetX, targetZ, rocketTypeNow());
        soyuz.moveTo(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1, this.worldPosition.getZ() + 0.5D, 0.0F, 0.0F);
        if (this.mode == 0) {
            soyuz.setPayload(0, this.items.get(SLOT_SATELLITE).copy());
            if (orbital() == 2) this.items.set(SLOT_ORBITAL_MODULE, ItemStack.EMPTY);
            this.items.set(SLOT_SATELLITE, ItemStack.EMPTY);
        }
        if (this.mode == 1) {
            for (int i = SLOT_CARGO_START; i < SLOT_COUNT; i++) {
                soyuz.setPayload(i - SLOT_CARGO_START, this.items.get(i).copy());
                this.items.set(i, ItemStack.EMPTY);
            }
        }
        this.level.addFreshEntity(soyuz);
        this.level.playSound(null, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                HbmSoundEvents.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100.0F, 1.1F);
        // FluidTank#setFill is unclamped in 1.7.10; HbmFluidTank clamps a negative oxygen fill at 0.
        this.keroseneTank.setAmount(this.keroseneTank.amount() - req);
        this.oxygenTank.setAmount(this.oxygenTank.amount() - req);
        this.power -= pow;
        this.items.set(SLOT_ROCKET, ItemStack.EMPTY);
    }

    public boolean canLaunch() {
        return hasRocket() && hasFuel() && hasRocket() && hasPower() && designator() != 1 && orbital() != 1 && satellite() != 1;
    }

    public boolean hasFuel() { return this.keroseneTank.amount() >= getFuelRequired(); }
    public boolean hasOxy() { return this.oxygenTank.amount() >= getFuelRequired(); }

    public int getFuelRequired() {
        if (this.mode == 1) return Math.min(5000 + getDist(), 128_000);
        return 128_000;
    }

    public int getDist() {
        if (designator() == 2) {
            CompoundTag tag = customData(this.items.get(SLOT_DESIGNATOR));
            double dx = this.worldPosition.getX() - tag.getInt("xCoord");
            double dz = this.worldPosition.getZ() - tag.getInt("zCoord");
            return (int) Math.sqrt(dx * dx + dz * dz);
        }
        return 0;
    }

    public boolean hasPower() { return this.power >= getPowerRequired(); }
    public static int getPowerRequired() { return (int) (MAX_POWER * 0.75); }

    private int rocketTypeNow() { // TileEntitySoyuzLauncher#getType
        if (!hasRocket()) return -1;
        return (byte) SoyuzItem.rawSkin(this.items.get(SLOT_ROCKET));
    }

    public boolean hasRocket() {
        return this.items.get(SLOT_ROCKET).is(HbmItems.MISSILE_SOYUZ.get());
    }

    /** 0: not required, 1: required but missing/not ready, 2: present */
    public int designator() {
        if (this.mode == 0) return 0;
        ItemStack stack = this.items.get(SLOT_DESIGNATOR);
        boolean designator = !stack.isEmpty() && (stack.getItem() instanceof com.atomiclegacy.items.LegacyCoordinateDesignatorItem
                || stack.getItem() instanceof com.atomiclegacy.items.LegacyRangeDesignatorItem);
        CompoundTag tag = customData(stack);
        return designator && tag.contains("xCoord") && tag.contains("zCoord") ? 2 : 1;
    }

    public int satellite() {
        if (this.mode == 1) return 0;
        return this.items.get(SLOT_SATELLITE).isEmpty() ? 1 : 2;
    }

    public int orbital() {
        if (this.mode == 1) return 0;
        ItemStack sat = this.items.get(SLOT_SATELLITE);
        // slots[2].getItemDamage() == EnumSatType.MINER_LUNAR.ordinal(): the lunar miner variant
        if (!sat.isEmpty() && (sat.is(HbmItems.SAT_GERALD.get()) || sat.is(HbmItems.SATELLITE_MINER_LUNAR.get()))) {
            return this.items.get(SLOT_ORBITAL_MODULE).is(HbmItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
        }
        return 0;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // ---- client view ----
    public boolean renderStarting() { return this.starting; }
    public int renderCountdown() { return this.countdown; }
    public int renderRocketType() { return this.rocketType; }
    public long power() { return this.power; }
    /** Diagnostics only: the reference fills the buffer from the battery slot. */
    public void applyTestPower(long value) { this.power = value; }
    public ContainerData menuData() { return this.data; }

    /** Material refund of SoyuzLauncher#breakBlock (the launcher itself drops nothing). */
    @Override
    public void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, this.items);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (int l = 0; l < 6; l++) drop(level, x, y, z, () -> HbmBlocks.STRUCT_LAUNCHER.get().asItem(), 64);
        for (int l = 0; l < 4; l++) drop(level, x, y, z, () -> HbmBlocks.CONCRETE_SMOOTH.get().asItem(), 64);
        for (int l = 0; l < 6; l++) drop(level, x, y, z, () -> HbmBlocks.STRUCT_SCAFFOLD.get().asItem(), 64);
        drop(level, x, y, z, () -> HbmBlocks.STRUCT_LAUNCHER.get().asItem(), 30);
        drop(level, x, y, z, () -> HbmBlocks.STRUCT_SCAFFOLD.get().asItem(), 63);
        drop(level, x, y, z, () -> HbmBlocks.CONCRETE_SMOOTH.get().asItem(), 38);
        drop(level, x, y, z, () -> HbmBlocks.STRUCT_SOYUZ_CORE.get().asItem(), 1);
    }

    private static void drop(Level level, double x, double y, double z, Supplier<net.minecraft.world.item.Item> item, int count) {
        level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(item.get(), count)));
    }

    // ---- persistence: TileEntitySoyuzLauncher#writeToNBT ----
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.put("fuel", this.keroseneTank.save());
        tag.put("oxidizer", this.oxygenTank.save());
        tag.putLong("power", this.power);
        tag.putByte("mode", this.mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        if (tag.contains("fuel")) this.keroseneTank.load(tag.getCompound("fuel"));
        if (tag.contains("oxidizer")) this.oxygenTank.load(tag.getCompound("oxidizer"));
        this.power = tag.getLong("power");
        this.mode = tag.getByte("mode");
        if (tag.contains("sync_starting")) {
            this.starting = tag.getBoolean("sync_starting");
            this.countdown = tag.getInt("sync_countdown");
            this.rocketType = tag.getInt("sync_type");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("power", this.power);
        tag.putByte("mode", this.mode);
        tag.putBoolean("sync_starting", this.starting);
        tag.putInt("sync_countdown", this.countdown);
        tag.putInt("sync_type", this.rocketType);
        tag.put("fuel", this.keroseneTank.save());
        tag.put("oxidizer", this.oxygenTank.save());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.power = tag.getLong("power");
        this.mode = tag.getByte("mode");
        this.starting = tag.getBoolean("sync_starting");
        this.countdown = tag.getInt("sync_countdown");
        this.rocketType = tag.getInt("sync_type");
        this.keroseneTank.load(tag.getCompound("fuel"));
        this.oxygenTank.load(tag.getCompound("oxidizer"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    // ---- Container ----
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return this.items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return this.items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.soyuz_launcher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SoyuzLauncherMenu(id, inventory, this, this.data);
    }

    public HbmFluidTank keroseneTank() { return this.keroseneTank; }
    public HbmFluidTank oxygenTank() { return this.oxygenTank; }
}
