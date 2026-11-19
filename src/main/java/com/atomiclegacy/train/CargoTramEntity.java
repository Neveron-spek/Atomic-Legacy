package com.atomiclegacy.train;

import com.atomiclegacy.blocks.rail.IRailNTM.TrackGauge;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] TrainCargoTram ("Electric Flat Bed Tram"): 28 cargo slots plus the battery slot 28.
 * <pre>
 *     _________
 *    | |       \          <--
 *    | |       |___
 *    | |       |  |                             |
 * _O\|_|_______|__|_____________________________|/O_
 * |____|                                      |____|
 *    \__________________________________________/
 *        '( + )'                      '( + )'
 * </pre>
 */
public class CargoTramEntity extends RailCarElectricEntity {

    public CargoTramEntity(EntityType<? extends CargoTramEntity> type, Level world) {
        super(type, world);
    }

    public CargoTramEntity(Level world) {
        this(HbmEntityTypes.CARGO_TRAM.get(), world);
    }

    @Override public double getPoweredAcceleration() { return 0.01; }
    @Override public double getPassivBrake() { return 0.95; }
    @Override public boolean shouldUseEngineBrake(Player player) { return Math.abs(this.engineSpeed) < 0.1; }
    @Override public double getMaxPoweredSpeed() { return 0.5; }
    @Override public double getMaxRailSpeed() { return 1; }
    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public LegacyVec3 getRiderSeatPosition() { return LegacyVec3.of(0.375, 2.375, 0.5); }
    @Override public boolean shouldRiderSit() { return false; }
    @Override public int getContainerSize() { return 29; }
    @Override public String getInventoryName() { return this.hasCustomInventoryName() ? this.getEntityName() : "container.atomiclegacy.train_tram"; }
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }
    @Override public int getMaxPower() { return this.getPowerConsumption() * 100; }
    @Override public int getPowerConsumption() { return 10; }
    @Override public boolean hasChargeSlot() { return true; }
    @Override public int getChargeSlot() { return 28; }

    @Override
    public DummyConfig[] getDummies() {
        return new DummyConfig[] {
                new DummyConfig(2F, 1F, LegacyVec3.of(0, 0, 1.5)),
                new DummyConfig(2F, 1F, LegacyVec3.of(0, 0, 0)),
                new DummyConfig(2F, 1F, LegacyVec3.of(0, 0, -1.5))
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            this.discard();
        }
        return true;
    }

    @Override
    public LegacyVec3[] getPassengerSeats() {
        return new LegacyVec3[] {
                LegacyVec3.of(0.5, 1.75, -1.5),
                LegacyVec3.of(-0.5, 1.75, -1.5)
        };
    }
}
