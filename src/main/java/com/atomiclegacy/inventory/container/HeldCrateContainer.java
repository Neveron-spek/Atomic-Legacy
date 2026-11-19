package com.atomiclegacy.inventory.container;

import com.atomiclegacy.blocks.LegacyCrateLogic;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/** [parity R4: crates] ItemBlockStorageCrate.InventoryCrate: the crate inventory of the held item. */
public final class HeldCrateContainer extends SimpleContainer {
    private static final Random RAND = new Random();
    private final Player player;
    private final ItemStack target;
    private final StorageCrateBlockEntity.Kind kind;
    private boolean loading;

    public HeldCrateContainer(Player player, StorageCrateBlockEntity.Kind kind) {
        super(kind.slots());
        this.player = player;
        this.kind = kind;
        this.target = player.getMainHandItem();
        CompoundTag data = LegacyCrateLogic.data(this.target);
        if (data.isEmpty()) {
            data.putLong("stacklock", RAND.nextLong());
            LegacyCrateLogic.setData(this.target, data);
        }
        this.loading = true;
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.parseOptional(player.registryAccess(), data.getCompound("slot" + i)));
        }
        this.loading = false;
    }

    public StorageCrateBlockEntity.Kind kind() {
        return this.kind;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.loading || this.player.level().isClientSide) {
            return;
        }
        CompoundTag nbt = LegacyCrateLogic.data(this.target);
        nbt.remove("stacklock");
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                nbt.remove("slot" + i);
                continue;
            }
            nbt.put("slot" + i, stack.save(this.player.registryAccess()));
        }
        nbt.putLong("stacklock", RAND.nextLong());
        LegacyCrateLogic.setData(this.target, nbt);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == this.target;
    }

    @Override
    public void startOpen(Player player) {
        if (!player.level().isClientSide) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.CRATE_OPEN.get(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.CRATE_CLOSE.get(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
        // ItemInventory#checkNBT: oversized contents are ejected and the whole tag is reset.
        CompoundTag nbt = LegacyCrateLogic.data(this.target);
        if (!nbt.isEmpty() && LegacyCrateLogic.compressedSize(nbt) > LegacyCrateLogic.NBT_LIMIT) {
            player.sendSystemMessage(Component.literal(LegacyCrateLogic.NBT_WARNING).withStyle(ChatFormatting.RED));
            LegacyCrateLogic.spill(player.level(), player.getX(), player.getY(), player.getZ(), this, player);
            this.loading = true;
            clearContent();
            this.loading = false;
            nbt = new CompoundTag();
        }
        nbt.remove("stacklock");
        if (!nbt.isEmpty()) {
            nbt.putLong("stacklock", RAND.nextLong());
        }
        LegacyCrateLogic.setData(this.target, nbt);
        player.inventoryMenu.broadcastChanges();
    }
}
