package com.atomiclegacy.tileentity;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** [parity R4: crates] Shared state of TileEntityCrateBase (crates and the safe). */
public interface LegacyCrate extends Container, LockableBlockEntity {
    StorageCrateBlockEntity.Kind kind();

    @Nullable
    String customName();

    void setCustomName(@Nullable String name);

    boolean hasSpiders();

    void setHasSpiders(boolean spiders);

    /** TileEntityCrateBase#spawnSpiders for a placed crate. */
    void releaseSpiders(net.minecraft.world.entity.player.Player player);

    /** BlockStorageCrate.dropInv == false: removal must not spill the inventory. */
    boolean keepContentsOnRemove();

    void setKeepContentsOnRemove(boolean keep);

    ItemStack pendingDrop();

    void setPendingDrop(ItemStack stack);

    default boolean hasCustomName() {
        String name = customName();
        return name != null && !name.isEmpty();
    }
}
