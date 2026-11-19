package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.tileentity.SupplyCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity W1: airdrop] 1.7.10 EntityParachuteCrate: falls with motionY -= 0.02 while above -0.2,
 * posY capped at 600; on entering a non-air block it places crate_supply one block higher and
 * hands it its items.
 */
public final class LegacyParachuteCrateEntity extends Entity {
    public final List<ItemStack> items = new ArrayList<>();

    public LegacyParachuteCrateEntity(EntityType<? extends LegacyParachuteCrateEntity> type, Level level) {
        super(type, level);
    }

    public LegacyParachuteCrateEntity(Level level) {
        this(HbmEntityTypes.LEGACY_PARACHUTE_CRATE.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        // EntityParachuteCrate#onUpdate does not call Entity#onUpdate.
        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (motion.y > -0.2D) setDeltaMovement(motion.x, motion.y - 0.02D, motion.z);
        if (getY() > 600.0D) setPosRaw(getX(), 600.0D, getZ());

        BlockPos block = new BlockPos(Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ()));
        if (!level().getBlockState(block).isAir()) {
            this.discard();

            if (!level().isClientSide) {
                BlockPos cratePos = new BlockPos(Mth.floor(getX()), Mth.floor(getY() + 1), Mth.floor(getZ()));
                level().setBlock(cratePos, HbmBlocks.CRATE_SUPPLY.get().defaultBlockState(), 3);
                if (level().getBlockEntity(cratePos) instanceof SupplyCrateBlockEntity crate) {
                    // crate.items.addAll(items): an already present crate keeps its contents.
                    List<ItemStack> contents = new ArrayList<>(crate.takeContents());
                    contents.addAll(this.items);
                    crate.setContents(contents);
                }
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        items.clear();
        ListTag list = nbt.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            items.add(ItemStack.parseOptional(registryAccess(), list.getCompound(i)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            list.add(stack.saveOptional(registryAccess()));
        }
        nbt.put("items", list);
    }
}
