package com.atomiclegacy.items;

import com.atomiclegacy.inventory.container.LegacyAmmoBagMenu;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * [parity b1: ammo bag] 1.7.10 ItemAmmoBag (ammo_bag, ammo_bag_infinite) and ItemCasingBag (casing_bag): stack 1,
 * right click opens the bag held in the hand; the contents live on the stack. The ammo bag holds 8 stacks of
 * ammo_standard / ammo_secret and is read by the sedna magazines; the casing bag holds 15 stacks, accepts nothing
 * by hand and is filled by {@link #pushCasing}.
 */
public final class LegacyAmmoBagItem extends Item {
    public enum Kind {
        AMMO_BAG(8, "container.atomiclegacy.ammo_bag"),
        AMMO_BAG_INFINITE(8, "container.atomiclegacy.ammo_bag"),
        CASING_BAG(15, "container.atomiclegacy.casing_bag");

        private final int slots;
        private final String titleKey;

        Kind(int slots, String titleKey) {
            this.slots = slots;
            this.titleKey = titleKey;
        }

        public int slots() { return slots; }
        public String titleKey() { return titleKey; }
        public boolean ammo() { return this != CASING_BAG; }
    }

    private static final String CONTENTS_KEY = "legacy_bag";
    private static final String CASINGS_KEY = "legacy_bag_casings";
    private final Kind kind;

    public LegacyAmmoBagItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    /** ModItems.ammo_bag or ModItems.ammo_bag_infinite. */
    public static boolean isAmmoBag(ItemStack stack) {
        return stack.getItem() instanceof LegacyAmmoBagItem bag && bag.kind.ammo();
    }

    public static boolean isInfiniteBag(ItemStack stack) {
        return stack.getItem() instanceof LegacyAmmoBagItem bag && bag.kind == Kind.AMMO_BAG_INFINITE;
    }

    public static boolean isCasingBag(ItemStack stack) {
        return stack.getItem() instanceof LegacyAmmoBagItem bag && bag.kind == Kind.CASING_BAG;
    }

    /** InventoryAmmoBag#isItemValidForSlot: ammo_standard or ammo_secret (the casing bag accepts nothing). */
    public static boolean isValidAmmo(ItemStack stack) {
        return stack.is(HbmItems.AMMO_STANDARD.get()) || stack.is(HbmItems.AMMO_SECRET.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            Component title = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName() : Component.translatable(kind.titleKey());
            serverPlayer.openMenu(
                    new SimpleMenuProvider((containerId, inventory, ignored) -> new LegacyAmmoBagMenu(containerId, inventory, hand, kind), title),
                    buffer -> {
                        buffer.writeBoolean(hand == InteractionHand.OFF_HAND);
                        buffer.writeByte(kind.ordinal());
                    });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ItemAmmoBag#showDurabilityBar / getDurabilityForDisplay: never for the infinite bag or the casing bag; an ammo
    // bag without data shows an empty bar, otherwise the bar is hidden only when every slot is full.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (kind != Kind.AMMO_BAG) return false;
        return !hasContents(stack) || emptyFraction(stack) != 0.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float) emptyFraction(stack) * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) (1.0D - emptyFraction(stack))) / 3.0F, 1.0F, 1.0F);
    }

    private static boolean hasContents(ItemStack stack) {
        return data(stack).contains(CONTENTS_KEY);
    }

    /** ItemAmmoBag#getDurabilityForDisplay: 1 - bullets / capacity, an empty slot counting as 64. */
    public double emptyFraction(ItemStack stack) {
        CompoundTag root = data(stack);
        if (!root.contains(CONTENTS_KEY)) return 1.0D;
        CompoundTag stored = root.getCompound(CONTENTS_KEY);
        int capacity = 0;
        int bullets = 0;
        for (int slot = 0; slot < kind.slots(); slot++) {
            CompoundTag tag = stored.getCompound("slot_" + slot);
            int count = tag.getInt("count");
            ResourceLocation id = tag.contains("id") ? ResourceLocation.tryParse(tag.getString("id")) : null;
            if (id == null || count <= 0) {
                capacity += 64;
            } else {
                capacity += BuiltInRegistries.ITEM.get(id).getDefaultMaxStackSize();
                bullets += count;
            }
        }
        return 1.0D - (double) bullets / (double) capacity;
    }

    /**
     * ItemCasingBag#pushCasing: adds a fractional casing to the per-casing counter of the bag and moves whole casings
     * into the bag slots. Returns true if the casing was accounted for.
     */
    public static boolean pushCasing(ItemStack bag, ItemStack casing, float amount, HolderLookup.Provider registries) {
        CompoundTag root = data(bag);
        CompoundTag counters = root.getCompound(CASINGS_KEY);
        String name = BuiltInRegistries.ITEM.getKey(casing.getItem()).toString();
        boolean ret = false;

        // only add if the previous number did not exceed 1 (i.e. the bag ran full, and may have been emptied)
        if (counters.getFloat(name) < 1) {
            ret = true;
            counters.putFloat(name, counters.getFloat(name) + amount);
        }
        root.put(CASINGS_KEY, counters);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        if (counters.getFloat(name) >= 1) {
            BagInventory inv = new BagInventory(bag, registries, Kind.CASING_BAG);
            ItemStack toAdd = casing.copy();
            while (counters.getFloat(name) >= 1) {
                boolean didSomething = false;

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (toAdd.getCount() <= 0) break;
                    ItemStack slot = inv.getItem(i);
                    if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, toAdd)) {
                        int am = Math.min(toAdd.getCount(), slot.getMaxStackSize() - slot.getCount());
                        toAdd.shrink(am);
                        slot.grow(am);
                        if (am > 0) didSomething = true;
                    }
                }

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (toAdd.getCount() <= 0) break;
                    if (inv.getItem(i).isEmpty()) {
                        inv.items[i] = toAdd;
                        didSomething = true;
                        break;
                    }
                }

                if (didSomething) {
                    counters.putFloat(name, counters.getFloat(name) - 1F);
                    ret = true;
                } else {
                    break;
                }
            }
            inv.setChanged();
            root = data(bag);
            root.put(CASINGS_KEY, counters);
            bag.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return ret;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** InventoryAmmoBag / InventoryCasingBag: a view of the bag stack that writes itself back on every change. */
    public static final class BagInventory implements Container {
        public final ItemStack box;
        private final HolderLookup.Provider registries;
        private final Kind kind;
        private final ItemStack[] items;

        public BagInventory(ItemStack box, HolderLookup.Provider registries, Kind kind) {
            this.box = box;
            this.registries = registries;
            this.kind = kind;
            this.items = new ItemStack[kind.slots()];
            CompoundTag stored = data(box).getCompound(CONTENTS_KEY);
            for (int slot = 0; slot < items.length; slot++) {
                items[slot] = stored.contains("slot_" + slot)
                        ? ItemStack.parseOptional(registries, stored.getCompound("slot_" + slot))
                        : ItemStack.EMPTY;
            }
        }

        @Override public int getContainerSize() { return items.length; }
        @Override public ItemStack getItem(int slot) { return items[slot]; }
        @Override public int getMaxStackSize() { return 64; }
        @Override public boolean stillValid(Player player) { return true; }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) if (!stack.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = items[slot];
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack removed = stack.split(amount);
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = items[slot];
            items[slot] = ItemStack.EMPTY;
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
            items[slot] = stack;
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return kind.ammo() && isValidAmmo(stack);
        }

        @Override
        public void setChanged() {
            CompoundTag root = data(box);
            CompoundTag stored = new CompoundTag();
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot].isEmpty()) {
                    items[slot] = ItemStack.EMPTY;
                } else {
                    stored.put("slot_" + slot, items[slot].save(registries));
                }
            }
            // An empty compound is kept: 1.7.10 always leaves the stack list on a bag that was opened once.
            root.put(CONTENTS_KEY, stored);
            box.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }

        @Override
        public void clearContent() {
            java.util.Arrays.fill(items, ItemStack.EMPTY);
            setChanged();
        }
    }
}
