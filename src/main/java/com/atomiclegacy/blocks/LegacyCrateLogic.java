package com.atomiclegacy.blocks;

import com.atomiclegacy.inventory.container.StorageCrateMenu;
import com.atomiclegacy.items.KeyPinItem;
import com.atomiclegacy.items.LockItem;
import com.atomiclegacy.tileentity.LegacyCrate;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [parity R4: crates] BlockStorageCrate, ItemBlockStorageCrate and TileEntityCrateBase item handling
 * (ServerConfig defaults CRATE_KEEP_CONTENTS = true, CRATE_OPEN_HELD = true).
 */
public final class LegacyCrateLogic {
    public static final String DATA_KEY = StorageCrateBlockEntity.ITEM_DATA_KEY;
    public static final int NBT_LIMIT = 6000;
    public static final String NBT_WARNING = "Warning: Container NBT exceeds 6kB, contents will be ejected!";
    /** Loop bound of BlockStorageCrate#addInformation ("whatever the biggest container is"). */
    private static final int TOOLTIP_SLOTS = 104;

    private LegacyCrateLogic() {
    }

    /** BlockStorageCrate#onBlockActivated: padlocks and the key kit act on the block instead of opening it. */
    public static ItemInteractionResult useItemOn(ItemStack stack) {
        if (stack.getItem() instanceof LockItem || stack.is(HbmItems.KEY_KIT.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult use(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof LegacyCrate crate && crate.canAccess(player)
                && crate instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> {
                buffer.writeByte(0);
                buffer.writeBlockPos(pos);
            });
            crate.releaseSpiders(player);
        }
        return InteractionResult.CONSUME;
    }

    public static int comparator(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container container
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(container) : 0;
    }

    public static CompoundTag data(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(DATA_KEY) ? root.getCompound(DATA_KEY) : new CompoundTag();
    }

    public static void setData(ItemStack stack, @Nullable CompoundTag data) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data == null || data.isEmpty()) {
            root.remove(DATA_KEY);
        } else {
            root.put(DATA_KEY, data);
        }
        if (root.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    public static int compressedSize(CompoundTag tag) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag, out);
        } catch (IOException ignored) {
            return 0;
        }
        return out.size();
    }

    /** removedByPlayer with CRATE_KEEP_CONTENTS: decides the drop before the block is removed. */
    public static void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof LegacyCrate crate)) {
            return;
        }
        crate.setPendingDrop(ItemStack.EMPTY);
        if (!com.atomiclegacy.config.HbmServerParityConfig.crateKeepContents()) {
            // [parity S2: configs] removedByPlayer with CRATE_KEEP_CONTENTS = false: plain block item (not in
            // creative), contents spill through breakBlock unless the crate is locked.
            if (!player.isCreative()) {
                crate.setPendingDrop(new ItemStack(state.getBlock()));
            }
            boolean locked = (crate instanceof StorageCrateBlockEntity storage && storage.isLocked())
                    || (crate instanceof com.atomiclegacy.tileentity.SafeBlockEntity safe && safe.isLocked());
            crate.setKeepContentsOnRemove(locked);
            return;
        }
        crate.setKeepContentsOnRemove(true);
        if (player.isCreative() || !state.canHarvestBlock(level, pos, player)) {
            return;
        }
        ItemStack drop = dropStack(state.getBlock(), crate, level);
        CompoundTag data = data(drop);
        if (!data.isEmpty() && compressedSize(data) > NBT_LIMIT) {
            player.sendSystemMessage(Component.literal(NBT_WARNING).withStyle(ChatFormatting.RED));
            crate.setPendingDrop(new ItemStack(state.getBlock()));
            crate.setKeepContentsOnRemove(false);
            return;
        }
        crate.setPendingDrop(drop);
    }

    /** Spawns the crate item exactly like removedByPlayer (EntityItem at the block centre). */
    public static void playerDestroy(Level level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (level.isClientSide || !(blockEntity instanceof LegacyCrate crate) || crate.pendingDrop().isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, crate.pendingDrop()));
        crate.setPendingDrop(ItemStack.EMPTY);
    }

    /** BlockStorageCrate#breakBlock while dropInv is true. */
    public static void onRemove(Level level, BlockPos pos, BlockState state, BlockState newState) {
        if (level.isClientSide || state.is(newState.getBlock())) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof LegacyCrate crate && !crate.keepContentsOnRemove()) {
            spill(level, pos.getX(), pos.getY(), pos.getZ(), crate, null);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    public static void spill(Level level, double x, double y, double z, Container inventory, @Nullable Player player) {
        RandomSource rand = level.random;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i).copy();
            if (stack.isEmpty()) {
                continue;
            }
            float f = rand.nextFloat() * 0.8F + 0.1F;
            float f1 = rand.nextFloat() * 0.8F + 0.1F;
            float f2 = rand.nextFloat() * 0.8F + 0.1F;
            while (stack.getCount() > 0) {
                int j1 = rand.nextInt(21) + 10;
                if (j1 > stack.getCount()) {
                    j1 = stack.getCount();
                }
                ItemEntity entity = new ItemEntity(level, x + f, y + f1, z + f2, stack.split(j1));
                float f3 = 0.05F;
                double mx = (float) rand.nextGaussian() * f3;
                double my = (float) rand.nextGaussian() * f3 + 0.2F;
                double mz = (float) rand.nextGaussian() * f3;
                if (player != null) {
                    mx += player.getDeltaMovement().x;
                    my += player.getDeltaMovement().y;
                    mz += player.getDeltaMovement().z;
                }
                entity.setDeltaMovement(mx, my, mz);
                level.addFreshEntity(entity);
            }
        }
    }

    public static ItemStack dropStack(Block block, LegacyCrate crate, Level level) {
        ItemStack drop = new ItemStack(block);
        HolderLookup.Provider registries = level.registryAccess();
        CompoundTag nbt = new CompoundTag();
        for (int i = 0; i < crate.getContainerSize(); i++) {
            ItemStack stack = crate.getItem(i);
            if (!stack.isEmpty()) {
                nbt.put("slot" + i, stack.save(registries));
            }
        }
        if (crate.isLocked()) {
            nbt.putInt("lock", crate.pins());
            nbt.putDouble("lockMod", crate.lockMod());
        }
        if (crate.hasSpiders()) {
            nbt.putBoolean("spiders", true);
        }
        if (!nbt.isEmpty()) {
            nbt.putLong("stacklock", level.random.nextLong());
            setData(drop, nbt);
        }
        if (crate.hasCustomName()) {
            drop.set(DataComponents.CUSTOM_NAME, Component.literal(crate.customName()));
        }
        return drop;
    }

    /** BlockStorageCrate#onBlockPlacedBy. */
    public static void loadFromItem(LegacyCrate crate, ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag data = data(stack);
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (data.isEmpty() && name == null) {
            return;
        }
        for (int i = 0; i < crate.getContainerSize(); i++) {
            crate.setItem(i, ItemStack.parseOptional(registries, data.getCompound("slot" + i)));
        }
        if (data.contains("lock")) {
            crate.setPins(data.getInt("lock"));
            crate.setLockMod(data.getDouble("lockMod"));
            crate.lock();
        }
        crate.setHasSpiders(data.getBoolean("spiders"));
        if (name != null) {
            crate.setCustomName(name.getString());
        }
    }

    /** BlockStorageCrate#addInformation. */
    public static void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Component> list) {
        CompoundTag data = data(stack);
        HolderLookup.Provider registries = context.registries();
        if (data.isEmpty() || registries == null) {
            return;
        }
        if (data.getBoolean("spiders")) {
            if (data.contains("lock")) {
                list.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
            }
            list.add(Component.literal("Skittering emanates from within...").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (data.contains("lock")) {
            list.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
            for (int i = 0; i < TOOLTIP_SLOTS; i++) {
                if (!ItemStack.parseOptional(registries, data.getCompound("slot" + i)).isEmpty()) {
                    list.add(Component.literal("It feels heavy...").withStyle(ChatFormatting.YELLOW));
                    return;
                }
            }
            list.add(Component.literal("It feels empty...").withStyle(ChatFormatting.YELLOW));
            return;
        }
        List<Component> contents = new ArrayList<>();
        int amount = 0;
        for (int i = 0; i < TOOLTIP_SLOTS; i++) {
            ItemStack content = ItemStack.parseOptional(registries, data.getCompound("slot" + i));
            if (!content.isEmpty()) {
                amount++;
                if (contents.size() < 10) {
                    contents.add(Component.literal(" - " + content.getHoverName().getString()
                            + (content.getCount() > 1 ? " x" + content.getCount() : "")).withStyle(ChatFormatting.AQUA));
                }
            }
        }
        if (!contents.isEmpty()) {
            list.add(Component.literal("Contains:").withStyle(ChatFormatting.AQUA));
            list.addAll(contents);
            amount -= contents.size();
            if (amount > 0) {
                list.add(Component.literal("...and " + amount + " more.").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /** ItemBlockStorageCrate#onItemRightClick. */
    public static InteractionResultHolder<ItemStack> useHeld(Level level, Player player, InteractionHand hand,
                                                             StorageCrateBlockEntity.Kind kind) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || stack.getCount() != 1) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = data(stack);
            if (data.contains("lock")) {
                int lock = data.getInt("lock");
                for (ItemStack item : player.getInventory().items) {
                    if (item.isEmpty() || !(item.getItem() instanceof KeyPinItem) || item.getItem() instanceof LockItem
                            || !item.has(DataComponents.CUSTOM_DATA)) {
                        continue;
                    }
                    if (KeyPinItem.pins(item) == lock) {
                        spawnHeldSpiders(player, stack);
                        openHeld(serverPlayer, stack, kind);
                        break;
                    }
                }
                return InteractionResultHolder.consume(stack);
            }
            spawnHeldSpiders(player, stack);
            openHeld(serverPlayer, stack, kind);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void openHeld(ServerPlayer player, ItemStack stack, StorageCrateBlockEntity.Kind kind) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        Component title = customName != null ? Component.literal(customName.getString()) : Component.translatable(kind.containerKey());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> StorageCrateMenu.held(id, inventory, kind), title),
                buffer -> {
                    buffer.writeByte(1);
                    buffer.writeByte(kind.ordinal());
                });
    }

    /** TileEntityCrateBase#spawnSpiders(EntityPlayer, World, ItemStack). */
    private static void spawnHeldSpiders(Player player, ItemStack crate) {
        CompoundTag data = data(crate);
        if (!data.getBoolean("spiders")) {
            return;
        }
        Level level = player.level();
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            CaveSpider spider = new CaveSpider(EntityType.CAVE_SPIDER, level);
            spider.moveTo(player.getX() + random.nextGaussian() * 2, player.getY() + 1, player.getZ() + random.nextGaussian() * 2,
                    random.nextFloat(), 0.0F);
            spider.setTarget(player);
            level.addFreshEntity(spider);
        }
        data.remove("spiders");
        setData(crate, data);
    }
}
