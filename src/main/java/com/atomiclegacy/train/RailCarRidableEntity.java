package com.atomiclegacy.train;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * [parity S1: trains] EntityRailCarRidable: a driver seat (the car itself) plus dynamically spawned passenger seats.
 * Rider positions: 1.7.10 set the rider's posY, which for the client player is the eye position; the passenger is
 * therefore placed 1.62 blocks lower here so the camera ends up where the original put it.
 */
public abstract class RailCarRidableEntity extends RailCarCargoEntity {

    /** 1.7.10 EntityPlayer yOffset of the client player: posY was the eye position. */
    public static final double LEGACY_PLAYER_EYE = 1.62D;

    public double engineSpeed;
    public SeatDummyEntity[] passengerSeats;

    protected RailCarRidableEntity(EntityType<? extends RailCarRidableEntity> type, Level world) {
        super(type, world);
        this.passengerSeats = new SeatDummyEntity[this.getPassengerSeats().length];
    }

    /** Returns the linear speed added per tick when using powered movement */
    public abstract double getPoweredAcceleration();
    /** A mulitplier used on the speed either is there is no player in the train or if the parking brake is active */
    public abstract double getPassivBrake();
    /** The parking brake can be toggled, assuming a player is present, otherwise it is implicitly ON */
    public abstract boolean shouldUseEngineBrake(Player player);
    /** The max speed the engine can provide in both directions */
    public abstract double getMaxPoweredSpeed();
    /** Whether the engine is turned on */
    public abstract boolean canAccelerate();
    /** Called every tick if acceleration is successful */
    public void consumeFuel() { }

    /** An additive to the engine's speed yielding the total speed, caused by uneven surfaces */
    public double getGravitySpeed() {
        return 0D;
    }

    @Override
    public double getCurrentSpeed() { // in its current form, only call once per tick
        if (this.getFirstPassenger() instanceof Player player) {
            if (this.canAccelerate()) {
                if (player.zza > 0) {
                    engineSpeed += this.getPoweredAcceleration();
                    this.consumeFuel();
                } else if (player.zza < 0) {
                    engineSpeed -= this.getPoweredAcceleration();
                    this.consumeFuel();
                } else {
                    if (this.shouldUseEngineBrake(player)) {
                        engineSpeed *= this.getPassivBrake();
                    } else {
                        this.consumeFuel();
                    }
                }
            } else {
                engineSpeed *= this.getPassivBrake();
            }
        } else {
            engineSpeed *= this.getPassivBrake();
        }

        double maxSpeed = this.getMaxPoweredSpeed();
        engineSpeed = Mth.clamp(engineSpeed, -maxSpeed, maxSpeed);

        return engineSpeed + this.getGravitySpeed();
    }

    @Override
    public boolean interactFirst(Player player) {
        if (super.interactFirst(player)) return true;
        if (level().isClientSide) return true;

        int nearestSeat = this.getNearestSeat(player);

        if (nearestSeat == -1) {
            player.startRiding(this);
        } else if (nearestSeat >= 0) {
            SeatDummyEntity dummySeat = new SeatDummyEntity(level(), this, nearestSeat);
            LegacyVec3 passengerSeat = this.getPassengerSeats()[nearestSeat];
            passengerSeat.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
            double x = renderX + passengerSeat.xCoord;
            double y = renderY + passengerSeat.yCoord;
            double z = renderZ + passengerSeat.zCoord;
            dummySeat.setPos(x, y - 1, z);
            passengerSeats[nearestSeat] = dummySeat;
            level().addFreshEntity(dummySeat);
            player.startRiding(dummySeat);
        }

        return true;
    }

    public int getNearestSeat(Player player) {

        double nearestDist = Double.POSITIVE_INFINITY;
        int nearestSeat = -3;

        LegacyVec3[] seats = getPassengerSeats();
        Vec3 lookVec = player.getViewVector(2);
        // 1.7.10: posY + eyeHeight - yOffset, i.e. the eye position of the server player
        LegacyVec3 look = LegacyVec3.of(lookVec.x + player.getX(), lookVec.y + player.getEyeY(), lookVec.z + player.getZ());

        for (int i = 0; i < seats.length; i++) {

            LegacyVec3 seat = seats[i];
            if (seat == null) continue;
            if (passengerSeats[i] != null) continue;

            seat.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
            double x = renderX + seat.xCoord;
            double y = renderY + seat.yCoord;
            double z = renderZ + seat.zCoord;

            LegacyVec3 delta = LegacyVec3.of(look.xCoord - x, look.yCoord - y, look.zCoord - z);
            double dist = delta.lengthVector();

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = i;
            }
        }

        if (this.getFirstPassenger() == null) {
            LegacyVec3 seat = getRiderSeatPosition();
            seat.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
            double x = renderX + seat.xCoord;
            double y = renderY + seat.yCoord;
            double z = renderZ + seat.zCoord;

            LegacyVec3 delta = LegacyVec3.of(look.xCoord - x, look.yCoord - y, look.zCoord - z);
            double dist = delta.lengthVector();

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = -1;
            }
        }

        if (nearestDist > 180) return -2;

        return nearestSeat;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {

            LegacyVec3[] seats = this.getPassengerSeats();

            for (int i = 0; i < passengerSeats.length; i++) {
                SeatDummyEntity seat = passengerSeats[i];

                if (seat != null) {
                    if (seat.getFirstPassenger() == null) {
                        passengerSeats[i] = null;
                        seat.discard();
                    } else {
                        LegacyVec3 rot = seats[i];
                        rot.rotateAroundX((float) (this.getXRot() * Math.PI / 180));
                        rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
                        double x = renderX + rot.xCoord;
                        double y = renderY + rot.yCoord;
                        double z = renderZ + rot.zCoord;
                        seat.setPos(x, y - 1, z);
                    }
                }
            }
        }
    }

    /** updateRiderPosition */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        LegacyVec3 offset = getRiderSeatPosition();
        offset.rotateAroundX((float) (this.getXRot() * Math.PI / 180));
        offset.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
        callback.accept(passenger, this.renderX + offset.xCoord, this.renderY + offset.yCoord - riderEyeOffset(passenger), this.renderZ + offset.zCoord);
    }

    static double riderEyeOffset(Entity passenger) {
        return passenger instanceof Player ? LEGACY_PLAYER_EYE : 0.0D;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    /** Returns a Vec3 showing the relative position from the driver to the core */
    public abstract LegacyVec3 getRiderSeatPosition();

    public abstract LegacyVec3[] getPassengerSeats();
}
