package com.atomiclegacy.tileentity;

import com.atomiclegacy.drone.DroneItemMatcher;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.PneumaticTubeMenu;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.util.MachineMuffler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * [parity R2] TileEntityPneumoTube: a tube becomes a compressor when it has an insertion side (pulls from the
 * inventory there, 50 mB compressed air per send, every 5 ticks) and an endpoint when it has an ejection side
 * (registers the inventory there as receiver every 10 ticks). Fifteen SlotPattern filters with
 * ModulePatternMatcher modes, whitelist/blacklist, redstone inversion, provider/receiver orders and a 4000 mB
 * air tank whose pressure (1..5 PU) sets the range.
 */
public final class PneumaticTubeBlockEntity extends BlockEntity implements MenuProvider, com.atomiclegacy.util.SettingsCopiable {
    public static final int FILTER_SLOTS = 15;
    public static final int AIR_CAPACITY = 4_000;
    public static final ModelProperty<Integer> MODEL_DATA = new ModelProperty<>();

    private final ItemStack[] filters = new ItemStack[FILTER_SLOTS];
    private final String[] modes = new String[FILTER_SLOTS];
    @Nullable private Direction insertionDir;
    @Nullable private Direction ejectionDir;
    private boolean whitelist;
    private boolean redstone;
    private byte sendOrder;
    private byte receiveOrder;
    private int soundDelay;
    private int sendCounter;
    private final HbmFluidTank compair = new HbmFluidTank(air(), AIR_CAPACITY);
    private long fillTick = Long.MIN_VALUE;
    private int filledThisTick;
    // [parity V2: pneumatic_tube_paintable] TileEntityPneumoTubePaintable.block and the block metadata (defuser)
    @Nullable private ResourceLocation paintBlock;
    private boolean overlayHidden;

    public PneumaticTubeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PNEUMATIC_TUBE.get(), pos, state);
        for (int i = 0; i < FILTER_SLOTS; i++) filters[i] = ItemStack.EMPTY;
        compair.setPressure(1);
    }

    public static HbmFluidDefinition air() {
        return HbmFluids.byName("air").orElse(HbmFluids.none());
    }

    // ---- accessors ----
    @Nullable public Direction insertionDir() { return insertionDir; }
    @Nullable public Direction ejectionDir() { return ejectionDir; }
    public boolean isCompressor() { return insertionDir != null; }
    public boolean isEndpoint() { return ejectionDir != null; }
    public boolean whitelist() { return whitelist; }
    public boolean redstone() { return redstone; }
    public int sendOrder() { return sendOrder; }
    public int receiveOrder() { return receiveOrder; }
    public HbmFluidTank compair() { return compair; }
    public ItemStack filter(int index) { return filters[index]; }
    @Nullable public String mode(int index) { return modes[index]; }

    public static int getRangeFromPressure(int pressure) {
        if (pressure == 0) return 0;
        if (pressure == 1) return 10;
        if (pressure == 2) return 25;
        if (pressure == 3) return 100;
        if (pressure == 4) return 250;
        if (pressure == 5) return 1_000;
        return 0;
    }

    public static int getIdentifier(int x, int y, int z) {
        return (y + z * 27644437) * 27644437 + x;
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!filters[i].isEmpty() && new DroneItemMatcher(filters[i], modes[i] == null ? DroneItemMatcher.EXACT : modes[i]).matches(stack)) {
                return true;
            }
        }
        return false;
    }

    /** whitelist: pass only matching; blacklist: pass only non-matching. */
    public boolean passes(ItemStack stack) {
        boolean match = matchesFilter(stack);
        return !((match && !whitelist) || (!match && whitelist));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PneumaticTubeBlockEntity tube) {
        if (!(level instanceof ServerLevel server)) return;
        if (tube.soundDelay > 0) tube.soundDelay--;

        if (tube.isCompressor() && (!level.hasNeighborSignal(pos) ^ tube.redstone)) {
            int randTime = Math.abs((int) (level.getGameTime() + getIdentifier(pos.getX(), pos.getY(), pos.getZ())));
            if (randTime % 5 == 0 && tube.compair.amount() >= 50) {
                Container source = inventoryAt(level, pos.relative(tube.insertionDir));
                if (source != null) {
                    if (PneumaticNetworkManager.send(server, pos, source, pos.relative(tube.insertionDir), tube,
                            tube.insertionDir.getOpposite(), tube.sendOrder, tube.receiveOrder,
                            getRangeFromPressure(tube.compair.pressure()), tube.sendCounter)) {
                        tube.compair.setAmount(tube.compair.amount() - 50);
                        if (tube.soundDelay <= 0 && !MachineMuffler.isMuffled(tube)) {
                            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                                    HbmSoundEvents.TUBE_FWOOMP.get(), SoundSource.PLAYERS, 0.25F,
                                    0.9F + level.random.nextFloat() * 0.2F);
                            tube.soundDelay = 20;
                        }
                        tube.setChanged();
                    }
                    tube.sendCounter++;
                }
            }
        }

        if (tube.isEndpoint() && level.getGameTime() % 10 == 0) {
            BlockPos target = pos.relative(tube.ejectionDir);
            Container dest = inventoryAt(level, target);
            if (dest != null) PneumaticNetworkManager.addReceiver(server, pos, dest, target, tube.ejectionDir, tube);
        }
    }

    /** Compat.getTileStandard + IInventory: the block entity itself, or the core behind a multiblock dummy. */
    @Nullable
    public static Container inventoryAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof PneumaticTubeBlockEntity) return null;
        return entity instanceof Container container ? container : null;
    }

    // ---- configuration (screwdriver / GUI) ----

    /** PneumoTube.onScrew: cycles the insertion (or, sneaking, ejection) side to the next inventory or to none. */
    public void cycleDirection(boolean ejection) {
        if (level == null) return;
        int rot = ordinal(ejection ? ejectionDir : insertionDir);
        int oth = ordinal(ejection ? insertionDir : ejectionDir);
        for (int i = 0; i < 7; i++) {
            rot = (rot + 1) % 7;
            if (rot == 6) break;
            if (rot == oth) continue;
            BlockEntity tile = level.getBlockEntity(worldPosition.relative(Direction.from3DDataValue(rot)));
            if (tile instanceof PneumaticTubeBlockEntity) continue;
            if (tile instanceof Container) break;
        }
        Direction result = rot == 6 ? null : Direction.from3DDataValue(rot);
        if (ejection) ejectionDir = result; else insertionDir = result;
        setChanged();
        level.invalidateCapabilities(worldPosition);
        syncAll();
    }

    private static int ordinal(@Nullable Direction direction) {
        return direction == null ? 6 : direction.get3DDataValue();
    }

    /** NBTControlPacket keys of GUIPneumoTube, mapped to menu button ids. */
    public void handleButton(int id) {
        switch (id) {
            case 0 -> whitelist = !whitelist;
            case 1 -> redstone = !redstone;
            case 2 -> {
                int pressure = compair.pressure() + 1;
                if (pressure > 5) pressure = 1;
                compair.setPressure(pressure);
            }
            case 3 -> {
                sendOrder++;
                if (sendOrder > 2) sendOrder = 0;
            }
            case 4 -> {
                receiveOrder++;
                if (receiveOrder > 1) receiveOrder = 0;
            }
            default -> {
                return;
            }
        }
        setChanged();
        syncAll();
    }

    /** ContainerPneumoTube.slotClick: ghost slot set (initPatternSmart) or right click nextMode. */
    public void patternClick(int index, ItemStack held, boolean rightClick) {
        if (index < 0 || index >= FILTER_SLOTS) return;
        if (rightClick && !filters[index].isEmpty()) {
            nextMode(index);
        } else {
            filters[index] = held.isEmpty() ? ItemStack.EMPTY : held.copy();
            modes[index] = filters[index].isEmpty() ? null : smartMode(filters[index]);
        }
        setChanged();
        syncAll();
    }

    /** ModulePatternMatcher.initPatternSmart: first ingot/block/dust/nugget/plate key, else the standard mode. */
    private static String smartMode(ItemStack stack) {
        List<ResourceLocation> tags = stack.getTags().map(TagKey::location).toList();
        for (String prefix : new String[] {"ingots/", "storage_blocks/", "dusts/", "nuggets/", "plates/"}) {
            for (ResourceLocation tag : tags) {
                if ("c".equals(tag.getNamespace()) && tag.getPath().startsWith(prefix)) return tag.toString();
            }
        }
        return DroneItemMatcher.defaultMode(stack);
    }

    private void nextMode(int index) {
        ItemStack pattern = filters[index];
        String mode = modes[index];
        List<ResourceLocation> tags = pattern.getTags().map(TagKey::location).toList();
        if (mode == null) {
            modes[index] = DroneItemMatcher.EXACT;
        } else if (DroneItemMatcher.EXACT.equals(mode)) {
            modes[index] = DroneItemMatcher.WILDCARD;
        } else if (DroneItemMatcher.WILDCARD.equals(mode)) {
            modes[index] = tags.isEmpty() ? DroneItemMatcher.EXACT : tags.getFirst().toString();
        } else {
            int at = tags.indexOf(ResourceLocation.tryParse(mode));
            modes[index] = at < 0 || at + 1 >= tags.size() ? DroneItemMatcher.EXACT : tags.get(at + 1).toString();
        }
    }

    // ---- [parity V2: pneumatic_tube_paintable] paint, overlay and the copy tool ----

    @Nullable
    public Block paintBlock() {
        return paintBlock == null ? null : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(paintBlock).orElse(null);
    }

    public void setPaintBlock(@Nullable Block block) {
        paintBlock = block == null ? null : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        setChanged();
        syncAll();
    }

    public boolean overlayHidden() {
        return overlayHidden;
    }

    public void toggleOverlayHidden() {
        overlayHidden = !overlayHidden;
        setChanged();
        syncAll();
    }

    private boolean paintable() {
        return getBlockState().getBlock() instanceof com.atomiclegacy.blocks.PneumaticTubeBlock tube && tube.isPaintable();
    }

    /** Paintable: TileEntityPneumoTubePaintable.getSettings (paint only); tube: IControlReceiverFilter (filters 0..15). */
    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        if (paintable()) {
            if (paintBlock != null) tag.putString("paintblock", paintBlock.toString());
            return tag;
        }
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!filters[i].isEmpty()) {
                CompoundTag slot = (CompoundTag) filters[i].save(level.registryAccess(), new CompoundTag());
                slot.putByte("slot", (byte) i);
                items.add(slot);
            }
        }
        tag.put("items", items);
        return tag;
    }

    @Override
    public void pasteSettings(CompoundTag settings, int index, Level level, Player player, BlockPos pos) {
        if (paintable()) {
            if (settings.contains("paintblock")) paintBlock = ResourceLocation.tryParse(settings.getString("paintblock"));
            setChanged();
            syncAll();
            return;
        }
        net.minecraft.nbt.ListTag items = settings.getList("items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        int listSize = items.size();
        if (listSize <= 0) return;
        for (int i = 0, count = 0; i < FILTER_SLOTS; i++, count++) {
            if (i >= listSize) continue;
            CompoundTag slotTag = items.getCompound(count);
            int slot = slotTag.getByte("slot");
            ItemStack loaded = ItemStack.parse(level.registryAccess(), slotTag).orElse(ItemStack.EMPTY);
            if (!loaded.isEmpty() && index < listSize && slot >= 0 && slot < FILTER_SLOTS) {
                filters[slot] = loaded;
                nextMode(slot);
            }
        }
        setChanged();
        syncAll();
    }

    // ---- compressed air (IFluidStandardReceiverMK2) ----

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (!isCompressor() || side == null || side == insertionDir || side == ejectionDir) return null;
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tank) { return compair.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return compair.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) {
                return HbmFluids.fromNeoFluid(stack.getFluid()).filter(def -> def == air()).isPresent();
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !isFluidValid(0, resource) || level == null) return 0;
                long now = level.getGameTime();
                int used = fillTick == now ? filledThisTick : 0;
                // getReceiverSpeed: clamp((max - fill) / 25, 1, 100) per tick
                int speed = Mth.clamp((compair.capacity() - compair.amount()) / 25, 1, 100);
                int offered = Math.min(resource.getAmount(), speed - used);
                if (offered <= 0) return 0;
                int filled = compair.fill(air(), offered, compair.pressure(), action.simulate());
                if (filled > 0 && action.execute()) {
                    fillTick = now;
                    filledThisTick = used + filled;
                    setChanged();
                }
                return filled;
            }

            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
        };
    }

    // ---- model data / sync ----

    /** Bits 0-2 insertion (0 = none, 1..6 = direction + 1), 3-5 ejection, 6-11 air connector mask. */
    public int modelBits() {
        int bits = (insertionDir == null ? 0 : insertionDir.get3DDataValue() + 1)
                | (ejectionDir == null ? 0 : ejectionDir.get3DDataValue() + 1) << 3;
        if (level != null && isCompressor()) {
            for (Direction dir : Direction.values()) {
                if (dir == insertionDir || dir == ejectionDir) continue;
                if (com.atomiclegacy.blocks.PneumaticTubeBlock.canConnectToAir(level, worldPosition, dir)) {
                    bits |= 1 << (6 + dir.get3DDataValue());
                }
            }
        }
        return bits;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MODEL_DATA, modelBits()).build();
    }

    public void syncAll() {
        if (level == null) return;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(net, packet, registries);
        requestModelDataUpdate();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("insertionDir", (byte) ordinal(insertionDir));
        tag.putByte("ejectionDir", (byte) ordinal(ejectionDir));
        tag.put("tank", compair.save());
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!filters[i].isEmpty()) tag.put("filter" + i, filters[i].save(registries));
            if (modes[i] != null) tag.putString("mode" + i, modes[i]);
        }
        tag.putByte("sendOrder", sendOrder);
        tag.putByte("receiveOrder", receiveOrder);
        tag.putInt("sendCounter", sendCounter);
        tag.putBoolean("whitelist", whitelist);
        tag.putBoolean("redstone", redstone);
        if (paintBlock != null) tag.putString("paint_block", paintBlock.toString()); // [parity V2: pneumatic_tube_paintable]
        tag.putBoolean("overlay_hidden", overlayHidden);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int ins = tag.getByte("insertionDir");
        int ej = tag.getByte("ejectionDir");
        insertionDir = ins >= 0 && ins < 6 && tag.contains("insertionDir") ? Direction.from3DDataValue(ins) : null;
        ejectionDir = ej >= 0 && ej < 6 && tag.contains("ejectionDir") ? Direction.from3DDataValue(ej) : null;
        if (tag.contains("tank")) compair.load(tag.getCompound("tank"));
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filters[i] = tag.contains("filter" + i) ? ItemStack.parse(registries, tag.getCompound("filter" + i)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            modes[i] = tag.contains("mode" + i) ? tag.getString("mode" + i) : null;
        }
        sendOrder = tag.getByte("sendOrder");
        receiveOrder = tag.getByte("receiveOrder");
        sendCounter = tag.getInt("sendCounter");
        whitelist = tag.getBoolean("whitelist");
        redstone = tag.getBoolean("redstone");
        paintBlock = tag.contains("paint_block") ? ResourceLocation.tryParse(tag.getString("paint_block")) : null; // [parity V2: pneumatic_tube_paintable]
        overlayHidden = tag.getBoolean("overlay_hidden");
        if (level != null && level.isClientSide) requestModelDataUpdate();
    }

    // ---- menu ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.atomiclegacy.pneumatic_tube");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PneumaticTubeMenu(id, inventory, this, !isCompressor());
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }
}
