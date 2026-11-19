package com.atomiclegacy.train;

import com.atomiclegacy.blocks.rail.IRailNTM.TrackGauge;
import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** [parity S1: trains] TrainCargoTramTrailer: 45 cargo slots, opened by clicking the trailer. */
public class CargoTramTrailerEntity extends RailCarCargoEntity {

    public CargoTramTrailerEntity(EntityType<? extends CargoTramTrailerEntity> type, Level world) {
        super(type, world);
    }

    public CargoTramTrailerEntity(Level world) {
        this(HbmEntityTypes.CARGO_TRAM_TRAILER.get(), world);
    }

    @Override public double getMaxRailSpeed() { return 1; }
    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public int getContainerSize() { return 45; }
    @Override public String getInventoryName() { return this.hasCustomInventoryName() ? this.getEntityName() : "container.trainTramTrailer"; } // untranslated in every 1.7.10 lang file: the raw key is shown
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }
    @Override public double getCurrentSpeed() { return 0; }

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
    public boolean interactFirst(Player player) {
        if (super.interactFirst(player)) return false;
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            TrainMenus.open(serverPlayer, this);
        }
        return true;
    }
}
