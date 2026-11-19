package com.atomiclegacy.train;

import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.InfiniteBatteryItem;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] EntityRailCarElectric: the charge slot fills the synched power (watcher 3) from any battery;
 * canAccelerate is always true and consumeFuel does nothing (both commented out in the original).
 */
public abstract class RailCarElectricEntity extends RailCarRidableEntity {
    private static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(RailCarElectricEntity.class, EntityDataSerializers.INT);

    protected RailCarElectricEntity(EntityType<? extends RailCarElectricEntity> type, Level world) {
        super(type, world);
    }

    public abstract int getMaxPower();
    public abstract int getPowerConsumption();
    public boolean hasChargeSlot() { return false; }
    public int getChargeSlot() { return 0; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0);
    }

    @Override
    public boolean canAccelerate() {
        return true;
    }

    @Override
    public void consumeFuel() {
    }

    public void setPower(int power) {
        this.entityData.set(POWER, power);
    }

    public int getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (this.hasChargeSlot()) {
                ItemStack stack = this.getItem(this.getChargeSlot());

                if (BatteryPackItem.isBattery(stack)) {
                    int powerNeeded = this.getMaxPower() - this.getPower();
                    long powerProvided = Math.min(BatteryPackItem.dischargeRate(stack), BatteryPackItem.charge(stack));
                    int powerTransfered = (int) Math.min(powerNeeded, powerProvided);

                    if (powerTransfered > 0) {
                        if (!(stack.getItem() instanceof InfiniteBatteryItem)) {
                            // battery.dischargeBattery(stack, powerTransfered); the creative battery ignores it
                            BatteryPackItem.dischargeIntoMachine(stack, this.getMaxPower() - powerTransfered, this.getMaxPower());
                        }
                        this.setPower(this.getPower() + powerTransfered);
                    }
                }
            }
        }
    }
}
