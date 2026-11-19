package com.atomiclegacy.train;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.atomiclegacy.blocks.rail.IRailNTM;
import com.atomiclegacy.blocks.rail.IRailNTM.MoveContext;
import com.atomiclegacy.blocks.rail.IRailNTM.RailCheckType;
import com.atomiclegacy.blocks.rail.IRailNTM.RailContext;
import com.atomiclegacy.blocks.rail.IRailNTM.TrackGauge;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * [parity S1: trains] com.hbm.entity.train.EntityRailCarBase with its LogicalTrainUnit, coupling and bounding dummies.
 * Platform adaptation: 1.7.10 smuggled the yaw/pitch through the velocity packet (motionX = yaw / 360); here the
 * same values travel as synched data and are read when the client interpolates a position update.
 */
public abstract class RailCarEntity extends Entity {
    private static final EntityDataAccessor<Float> TRAIN_YAW = SynchedEntityData.defineId(RailCarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAIN_PITCH = SynchedEntityData.defineId(RailCarEntity.class, EntityDataSerializers.FLOAT);

    public LogicalTrainUnit ltu;
    public int ltuIndex = 0;
    public boolean isOnRail = true;

    private int turnProgress;
    /* Clientside position that should be approached with smooth interpolation */
    private double trainX;
    private double trainY;
    private double trainZ;
    private double trainYaw;
    private double trainPitch;

    /* "Actual" position with offset directly between the front and back pos, won't match the standard position on curves */
    public double lastRenderX;
    public double lastRenderY;
    public double lastRenderZ;
    public double renderX;
    public double renderY;
    public double renderZ;

    public double cachedSpeed;

    public RailCarEntity coupledFront;
    public RailCarEntity coupledBack;

    public boolean initDummies = false;
    public BoundingBoxDummyEntity[] dummies = new BoundingBoxDummyEntity[0];

    protected RailCarEntity(EntityType<? extends RailCarEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIN_YAW, 0.0F);
        builder.define(TRAIN_PITCH, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) { }

    /** EntityRailCarBase.interactFirst: coupling with the coupling tool, otherwise false. */
    public boolean interactFirst(Player player) {
        if (player.getMainHandItem().is(HbmItems.COUPLING_TOOL.get())) {
            List<RailCarEntity> intersecting = level().getEntitiesOfClass(RailCarEntity.class, this.getBoundingBox().inflate(2D, 0D, 2D));
            for (RailCarEntity neighbor : intersecting) {
                if (neighbor == this) continue;
                if (neighbor.getGauge() != this.getGauge()) continue;

                TrainCoupling closestOwnCoupling = null;
                TrainCoupling closestNeighborCoupling = null;
                double closestDist = Double.POSITIVE_INFINITY;

                for (TrainCoupling ownCoupling : TrainCoupling.values()) {
                    for (TrainCoupling neighborCoupling : TrainCoupling.values()) {
                        LegacyVec3 ownPos = this.getCouplingPos(ownCoupling);
                        LegacyVec3 neighborPos = neighbor.getCouplingPos(neighborCoupling);
                        if (ownPos != null && neighborPos != null) {
                            LegacyVec3 delta = LegacyVec3.of(ownPos.xCoord - neighborPos.xCoord, ownPos.yCoord - neighborPos.yCoord, ownPos.zCoord - neighborPos.zCoord);
                            double length = delta.lengthVector();
                            if (length < 1 && length < closestDist) {
                                closestDist = length;
                                closestOwnCoupling = ownCoupling;
                                closestNeighborCoupling = neighborCoupling;
                            }
                        }
                    }
                }

                if (closestOwnCoupling != null && closestNeighborCoupling != null) {
                    if (this.getCoupledTo(closestOwnCoupling) != null) continue;
                    if (neighbor.getCoupledTo(closestNeighborCoupling) != null) continue;
                    this.couple(closestOwnCoupling, neighbor);
                    neighbor.couple(closestNeighborCoupling, this);
                    if (this.ltu != null) this.ltu.dissolveTrain();
                    if (neighbor.ltu != null) neighbor.ltu.dissolveTrain();
                    player.swing(InteractionHand.MAIN_HAND);
                    player.sendSystemMessage(Component.literal("Coupled " + this.hashCode() + " (" + closestOwnCoupling.name() + ") to " + neighbor.hashCode() + " (" + closestNeighborCoupling.name() + ")"));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return interactFirst(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void tick() {
        this.firstTick = false;
        if (this.level().isClientSide) {
            if (this.turnProgress > 0) {
                double x = this.getX() + (this.trainX - this.getX()) / (double) this.turnProgress;
                double y = this.getY() + (this.trainY - this.getY()) / (double) this.turnProgress;
                double z = this.getZ() + (this.trainZ - this.getZ()) / (double) this.turnProgress;
                double yaw = LegacyVec3.wrapAngleTo180(this.trainYaw - (double) this.getYRot());
                this.setYRot((float) ((double) this.getYRot() + yaw / (double) this.turnProgress));
                this.setXRot((float) ((double) this.getXRot() + (this.trainPitch - (double) this.getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                this.setPos(x, y, z);
            }

            BlockPos anchor = this.getCurrentAnchorPos();
            LegacyVec3 frontPos = getRelPosAlongRail(anchor, this.getLengthSpan(), new MoveContext(RailCheckType.FRONT, this.getCollisionSpan() - this.getLengthSpan()));
            LegacyVec3 backPos = getRelPosAlongRail(anchor, -this.getLengthSpan(), new MoveContext(RailCheckType.BACK, this.getCollisionSpan() - this.getLengthSpan()));

            this.lastRenderX = this.renderX;
            this.lastRenderY = this.renderY;
            this.lastRenderZ = this.renderZ;

            if (frontPos != null && backPos != null) {
                this.renderX = (frontPos.xCoord + backPos.xCoord) / 2D;
                this.renderY = (frontPos.yCoord + backPos.yCoord) / 2D;
                this.renderZ = (frontPos.zCoord + backPos.zCoord) / 2D;
            } else {
                this.renderX = getX();
                this.renderY = getY();
                this.renderZ = getZ();
            }
        } else {
            if (!this.isOnRail) {
                if (this.coupledFront != null) this.coupledFront.couple(this.coupledFront.getCouplingFrom(this), null);
                if (this.coupledBack != null) this.coupledBack.couple(this.coupledBack.getCouplingFrom(this), null);
                this.coupledFront = null;
                this.coupledBack = null;
            }

            if (this.coupledFront != null && this.coupledFront.isRemoved()) {
                this.coupledFront = null;
                if (this.ltu != null) this.ltu.dissolveTrain();
            }
            if (this.coupledBack != null && this.coupledBack.isRemoved()) {
                this.coupledBack = null;
                if (this.ltu != null) this.ltu.dissolveTrain();
            }

            if (this.ltu == null && (this.coupledFront == null || this.coupledBack == null) && this.isOnRail) {
                LogicalTrainUnit.generateTrain(this);
            }

            if (!this.isOnRail) {
                LegacyVec3 motion = LegacyVec3.of(0, 0, this.cachedSpeed);
                motion.rotateAroundY((float) (-this.getYRot() * Math.PI / 180D));
                this.move(MoverType.SELF, new Vec3(motion.xCoord, motion.yCoord - 0.04, motion.zCoord));
                this.renderX = getX();
                this.renderY = getY();
                this.renderZ = getZ();
                this.cachedSpeed *= 0.95D;
            }

            DummyConfig[] definitions = this.getDummies();

            if (!this.initDummies) {
                this.dummies = new BoundingBoxDummyEntity[definitions.length];
                for (int i = 0; i < definitions.length; i++) {
                    DummyConfig def = definitions[i];
                    BoundingBoxDummyEntity dummy = new BoundingBoxDummyEntity(level(), this, def.width, def.height);
                    LegacyVec3 rot = LegacyVec3.of(def.offset.xCoord, def.offset.yCoord, def.offset.zCoord);
                    rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
                    double x = getX() + rot.xCoord;
                    double y = getY() + rot.yCoord;
                    double z = getZ() + rot.zCoord;
                    dummy.setPos(x, y, z);
                    dummy.setDummySize(def.width, def.height);
                    level().addFreshEntity(dummy);
                    this.dummies[i] = dummy;
                }
                this.initDummies = true;
            }

            if (renderY != 0) {
                for (int i = 0; i < definitions.length; i++) {
                    DummyConfig def = definitions[i];
                    BoundingBoxDummyEntity dummy = dummies[i];
                    LegacyVec3 rot = LegacyVec3.of(def.offset.xCoord, def.offset.yCoord, def.offset.zCoord);
                    rot.rotateAroundX((float) (this.getXRot() * Math.PI / 180D));
                    rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
                    double x = renderX + rot.xCoord;
                    double y = renderY + rot.yCoord;
                    double z = renderZ + rot.zCoord;
                    dummy.setPos(x, y, z);
                }
            }
        }
    }

    public LegacyVec3 getRelPosAlongRail(BlockPos anchor, double distanceToCover, MoveContext context) {
        return getRelPosAlongRail(anchor, distanceToCover, this.getGauge(), this.level(), LegacyVec3.of(getX(), getY(), getZ()), this.getYRot(), context);
    }

    @Nullable
    public static LegacyVec3 getRelPosAlongRail(BlockPos anchor, double distanceToCover, TrackGauge gauge, Level worldObj, LegacyVec3 next, float yaw, MoveContext context) {
        if (distanceToCover < 0) {
            distanceToCover *= -1;
            yaw += 180;
        }

        int it = 0;

        do {
            it++;

            if (it > 30) {
                return null;
            }

            int x = anchor.getX();
            int y = anchor.getY();
            int z = anchor.getZ();
            Block block = worldObj.getBlockState(anchor).getBlock();

            LegacyVec3 rot = LegacyVec3.of(0, 0, 1);
            rot.rotateAroundY((float) (-yaw * Math.PI / 180D));

            if (block instanceof IRailNTM rail) {
                if (it == 1) {
                    next = rail.getTravelLocation(worldObj, x, y, z, next.xCoord, next.yCoord, next.zCoord, rot.xCoord, rot.yCoord, rot.zCoord, 0, new RailContext(), context);
                }

                boolean flip = distanceToCover < 0;

                if (rail.getGauge(worldObj, x, y, z) == gauge) {
                    RailContext info = new RailContext();
                    LegacyVec3 prev = next;
                    next = rail.getTravelLocation(worldObj, x, y, z, prev.xCoord, prev.yCoord, prev.zCoord, rot.xCoord, rot.yCoord, rot.zCoord, distanceToCover, info, context);
                    distanceToCover = info.overshoot;
                    anchor = info.pos;
                    yaw = generateYaw(next, prev) * (flip ? -1 : 1);
                } else {
                    return null;
                }
            } else {
                return null;
            }

        } while (distanceToCover != 0); //if there's still length to cover, keep going

        return next;
    }

    public static float generateYaw(LegacyVec3 front, LegacyVec3 back) {
        double deltaX = front.xCoord - back.xCoord;
        double deltaZ = front.zCoord - back.zCoord;
        double radians = -Math.atan2(deltaX, deltaZ);
        return (float) LegacyVec3.wrapAngleTo180(radians * 180D / Math.PI);
    }

    /** ModEventHandler.worldTick (Phase.END, server): moves every logical train unit once per tick. */
    public static void updateMotion(ServerLevel world) {
        Set<LogicalTrainUnit> ltus = new HashSet<>();

        /* gather all LTUs */
        for (Entity o : world.getAllEntities()) {
            if (o instanceof RailCarEntity train) {
                if (train.ltu != null) ltus.add(train.ltu);
            }
        }

        for (LogicalTrainUnit ltu : ltus) {

            double speed = ltu.getTotalSpeed() + ltu.pushForce;
            if (Math.abs(speed) < 0.001) speed = 0;

            for (RailCarEntity car : ltu.trains) car.cachedSpeed = speed;

            if (ltu.trains.length == 1) {
                RailCarEntity train = ltu.trains[0];

                BlockPos anchor = IRailNTM.floorPos(train.getX(), train.getY(), train.getZ());
                LegacyVec3 newPos = train.getRelPosAlongRail(anchor, speed, new MoveContext(RailCheckType.CORE, 0));
                if (newPos == null) {
                    train.derail();
                    ltu.dissolveTrain();
                    continue;
                }
                train.setPos(newPos.xCoord, newPos.yCoord, newPos.zCoord);
                anchor = train.getCurrentAnchorPos();
                LegacyVec3 frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan()));
                LegacyVec3 backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan()));

                if (frontPos == null || backPos == null) {
                    train.derail();
                    ltu.dissolveTrain();
                    continue;
                } else {
                    ltu.setRenderPos(train, frontPos, backPos);
                }

                ltu.pushForce = 0;
                ltu.collideTrain(speed);
                continue;
            }

            if (speed == 0) {
                ltu.combineWagons();
            } else {
                ltu.moveTrainByApproach(speed);
            }

            ltu.pushForce = 0;
            ltu.collideTrain(speed);
        }
    }

    /** Returns the amount of blocks that the train should move per tick */
    public abstract double getCurrentSpeed();
    public abstract double getMaxRailSpeed();
    /** Returns the gauge of this train */
    public abstract TrackGauge getGauge();
    /** Returns the length between the core and one of the bogies */
    public abstract double getLengthSpan();
    /** Returns the length between the core and the collision points */
    public abstract double getCollisionSpan();

    /** Returns the "true" position of the train, i.e. the block it wants to snap to */
    public BlockPos getCurrentAnchorPos() {
        return IRailNTM.floorPos(getX(), getY() + 0.25, getZ());
    }

    public void derail() {
        isOnRail = false;
    }

    /** setPositionAndRotation2: the train approaches the packet position over turnProg + 2 ticks. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.trainX = x;
        this.trainY = y;
        this.trainZ = z;
        this.turnProgress = steps + 2;
        this.trainYaw = this.entityData.get(TRAIN_YAW);
        this.trainPitch = this.entityData.get(TRAIN_PITCH);
    }

    @Override
    public double lerpTargetX() {
        return this.turnProgress > 0 ? this.trainX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.turnProgress > 0 ? this.trainY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.turnProgress > 0 ? this.trainZ : this.getZ();
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
    }

    public DummyConfig[] getDummies() {
        return new DummyConfig[0];
    }

    public static class DummyConfig {
        public LegacyVec3 offset;
        public float width;
        public float height;

        public DummyConfig(float width, float height, LegacyVec3 offset) {
            this.width = width;
            this.height = height;
            this.offset = offset;
        }
    }

    public enum TrainCoupling {
        FRONT,
        BACK
    }

    public double getCouplingDist(@Nullable TrainCoupling coupling) {
        return 0D;
    }

    @Nullable
    public LegacyVec3 getCouplingPos(TrainCoupling coupling) {
        double dist = this.getCouplingDist(coupling);
        if (dist <= 0) return null;
        if (coupling == TrainCoupling.BACK) dist *= -1;
        LegacyVec3 rot = LegacyVec3.of(0, 0, dist);
        rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180D));
        rot.xCoord += this.renderX;
        rot.yCoord += this.renderY;
        rot.zCoord += this.renderZ;
        return rot;
    }

    @Nullable
    public RailCarEntity getCoupledTo(@Nullable TrainCoupling coupling) {
        return coupling == TrainCoupling.FRONT ? this.coupledFront : coupling == TrainCoupling.BACK ? this.coupledBack : null;
    }

    @Nullable
    public TrainCoupling getCouplingFrom(@Nullable RailCarEntity coupledTo) {
        return coupledTo == this.coupledFront ? TrainCoupling.FRONT : coupledTo == this.coupledBack ? TrainCoupling.BACK : null;
    }

    public void couple(@Nullable TrainCoupling coupling, @Nullable RailCarEntity to) {
        if (coupling == TrainCoupling.FRONT) this.coupledFront = to;
        if (coupling == TrainCoupling.BACK) this.coupledBack = to;
    }

    /** Server side: rotation and the synched copy read by the clients. */
    protected void setTrainRotation(float yaw, float pitch) {
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.entityData.set(TRAIN_YAW, yaw);
        this.entityData.set(TRAIN_PITCH, pitch);
    }

    public static class LogicalTrainUnit {

        protected double pushForce;
        protected RailCarEntity[] trains;

        public RailCarEntity[] trains() {
            return trains;
        }

        /** Assumes that the train is an endpoint, i.e. that only one coupling is in use */
        public static LogicalTrainUnit generateTrain(RailCarEntity train) {
            List<RailCarEntity> links = new ArrayList<>();
            Set<RailCarEntity> brake = new HashSet<>();
            LogicalTrainUnit ltu = new LogicalTrainUnit();

            if (train.coupledFront == null && train.coupledBack == null) {
                ltu.trains = new RailCarEntity[] {train};
                train.ltu = ltu;
                train.ltuIndex = 0;
                return ltu;
            }

            RailCarEntity current = train;
            RailCarEntity next;

            do {
                next = null;

                if (current.coupledFront != null && !brake.contains(current.coupledFront)) next = current.coupledFront;
                if (current.coupledBack != null && !brake.contains(current.coupledBack)) next = current.coupledBack;

                links.add(current);
                brake.add(current);

                current = next;

            } while (next != null);

            ltu.trains = new RailCarEntity[links.size()];

            for (int i = 0; i < ltu.trains.length; i++) {
                ltu.trains[i] = links.get(i);
                ltu.trains[i].ltu = ltu;
                ltu.trains[i].ltuIndex = i;
            }

            return ltu;
        }

        /** Removes the LTU from all wagons */
        public void dissolveTrain() {
            for (RailCarEntity train : trains) {
                train.ltu = null;
                train.ltuIndex = 0;
            }
        }

        /** Find the center fo the train, then moves all wagons towards that center until the coupling points roughly touch */
        public void combineWagons() {

            if (trains.length <= 1) return;

            boolean odd = trains.length % 2 == 1;
            int centerIndex = odd ? trains.length / 2 : trains.length / 2 - 1;
            RailCarEntity center = trains[centerIndex];
            RailCarEntity prev = center;

            for (int i = centerIndex - 1; i >= 0; i--) {
                RailCarEntity next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }

            prev = center;
            for (int i = centerIndex + 1; i < trains.length; i++) {
                RailCarEntity next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }
        }

        /** Moves one wagon to ne next until the coupling points roughly touch */
        public void moveWagonTo(RailCarEntity moveTo, RailCarEntity moving) {
            TrainCoupling prevCouple = moveTo.getCouplingFrom(moving);
            TrainCoupling nextCouple = moving.getCouplingFrom(moveTo);
            LegacyVec3 prevLoc = moveTo.getCouplingPos(prevCouple);
            LegacyVec3 nextLoc = moving.getCouplingPos(nextCouple);
            LegacyVec3 delta = LegacyVec3.of(prevLoc.xCoord - nextLoc.xCoord, 0, prevLoc.zCoord - nextLoc.zCoord);
            double len = delta.lengthVector();
            len = (len / (0.5D / (len * len) + 1D)); //smart suspension

            BlockPos anchor = IRailNTM.floorPos(moving.getX(), moving.getY(), moving.getZ());
            LegacyVec3 trainPos = LegacyVec3.of(moving.getX(), moving.getY(), moving.getZ());
            float yaw = RailCarEntity.generateYaw(prevLoc, nextLoc);
            LegacyVec3 newPos = RailCarEntity.getRelPosAlongRail(anchor, len, moving.getGauge(), moving.level(), trainPos, yaw, new MoveContext(RailCheckType.CORE, 0));
            if (newPos == null) {
                // 1.7.10 dereferenced null here (NullPointerException in the world tick); the wagon derails instead
                moving.derail();
                this.dissolveTrain();
                return;
            }
            moving.setPos(newPos.xCoord, newPos.yCoord, newPos.zCoord);

            anchor = moving.getCurrentAnchorPos(); //reset origin to new position
            LegacyVec3 frontPos = moving.getRelPosAlongRail(anchor, moving.getLengthSpan(), new MoveContext(RailCheckType.FRONT, moving.getCollisionSpan() - moving.getLengthSpan()));
            LegacyVec3 backPos = moving.getRelPosAlongRail(anchor, -moving.getLengthSpan(), new MoveContext(RailCheckType.BACK, moving.getCollisionSpan() - moving.getLengthSpan()));

            if (frontPos == null || backPos == null) {
                moving.derail();
                this.dissolveTrain();
            } else {
                setRenderPos(moving, frontPos, backPos);
            }
        }

        /** Returns the total speed of the LTU, negative if it is backwards compared to the arbitrary "front" wagon */
        public double getTotalSpeed() {
            RailCarEntity prev = trains[0];
            double totalSpeed = 0;
            double maxSpeed = Double.POSITIVE_INFINITY;
            //if the first car is in reverse, flip all subsequent cars as well
            boolean reverseTheReverse = prev.getCouplingFrom(null) == TrainCoupling.BACK;

            if (trains.length == 1) {
                return prev.getCurrentSpeed();
            }

            for (RailCarEntity train : this.trains) {
                //if the car's linked indices are the wrong way, it is in reverse and speed applies negatively
                boolean reverse = false;

                RailCarEntity conFront = train.getCoupledTo(TrainCoupling.FRONT);
                RailCarEntity conBack = train.getCoupledTo(TrainCoupling.BACK);

                if (conFront != null && conFront.ltuIndex > train.ltuIndex) reverse = true;
                if (conBack != null && conBack.ltuIndex < train.ltuIndex) reverse = true;

                reverse ^= reverseTheReverse;

                double speed = train.getCurrentSpeed();
                if (reverse) speed *= -1;
                totalSpeed += speed;
                maxSpeed = Math.min(maxSpeed, train.getMaxRailSpeed());
            }

            if (Math.abs(totalSpeed) > maxSpeed) {
                totalSpeed = maxSpeed * Math.signum(totalSpeed);
            }

            return totalSpeed;
        }

        /** Determines the "front" wagon based on the movement and moves it, then moves all other wagons towards that */
        public void moveTrainByApproach(double speed) {
            RailCarEntity previous = null;
            RailCarEntity first = this.trains[0];
            boolean forward = speed > 0;
            boolean order = forward ^ first.getCouplingFrom(null) == TrainCoupling.BACK;

            for (int i = order ? 0 : this.trains.length - 1; order ? i < this.trains.length : i >= 0; i += order ? 1 : -1) {
                RailCarEntity current = this.trains[i];

                if (previous == null) {

                    if (first == current) speed *= -1;

                    boolean inReverse = first.getCouplingFrom(null) == current.getCouplingFrom(null);
                    int sigNum = inReverse ? 1 : -1;
                    BlockPos anchor = current.getCurrentAnchorPos();

                    LegacyVec3 frontPos = current.getRelPosAlongRail(anchor, (speed + current.getLengthSpan()) * -sigNum, new MoveContext(RailCheckType.FRONT, current.getCollisionSpan() - current.getLengthSpan()));

                    if (frontPos == null) {
                        current.derail();
                        this.dissolveTrain();
                        return;
                    } else {
                        anchor = current.getCurrentAnchorPos(); //reset origin to new position
                        LegacyVec3 corePos = current.getRelPosAlongRail(anchor, speed * -sigNum, new MoveContext(RailCheckType.CORE, 0));
                        if (corePos == null) {
                            // 1.7.10 dereferenced null here; derail like every other failed lookup
                            current.derail();
                            this.dissolveTrain();
                            return;
                        }
                        current.setPos(corePos.xCoord, corePos.yCoord, corePos.zCoord);
                        LegacyVec3 backPos = current.getRelPosAlongRail(anchor, (speed - current.getLengthSpan()) * -sigNum, new MoveContext(RailCheckType.BACK, current.getCollisionSpan() - current.getLengthSpan()));

                        if (backPos == null) {
                            current.derail();
                            this.dissolveTrain();
                            return;
                        } else {
                            setRenderPos(current, inReverse ? backPos : frontPos, inReverse ? frontPos : backPos);
                        }
                    }

                } else {
                    this.moveWagonTo(previous, current);
                }

                previous = current;
            }
        }

        /** Uses the front and back bogey positions to set the render pos and angles of a wagon */
        public void setRenderPos(RailCarEntity current, LegacyVec3 frontPos, LegacyVec3 backPos) {
            current.renderX = (frontPos.xCoord + backPos.xCoord) / 2D;
            current.renderY = (frontPos.yCoord + backPos.yCoord) / 2D;
            current.renderZ = (frontPos.zCoord + backPos.zCoord) / 2D;
            current.yRotO = current.getYRot();
            float yaw = generateYaw(frontPos, backPos);
            LegacyVec3 delta = LegacyVec3.of(frontPos.xCoord - backPos.xCoord, frontPos.yCoord - backPos.yCoord, frontPos.zCoord - backPos.zCoord);
            float pitch = (float) (Math.asin(delta.yCoord / delta.lengthVector()) * 180D / Math.PI);
            current.setTrainRotation(yaw, pitch);
        }

        public void collideTrain(double speed) {
            RailCarEntity collidingTrain = speed > 0 ? trains[0] : trains[trains.length - 1];
            List<RailCarEntity> intersect = collidingTrain.level().getEntitiesOfClass(RailCarEntity.class, collidingTrain.getBoundingBox().inflate(1, 1, 1));

            RailCarEntity collidesWith = null;

            for (RailCarEntity train : intersect) {
                if (train.ltu != null && train.ltu != this) {
                    collidesWith = train;
                    break;
                }
            }

            if (collidesWith == null) return;

            LegacyVec3 delta = LegacyVec3.of(collidingTrain.getX() - collidesWith.getX(), 0, collidingTrain.getZ() - collidesWith.getZ());
            double totalSpan = collidingTrain.getCollisionSpan() + collidesWith.getCollisionSpan();
            double diff = delta.lengthVector();
            if (diff > totalSpan) return;
            double push = (totalSpan - diff);

            RailCarEntity[][] whatever = new RailCarEntity[][] {{collidingTrain, collidesWith}, {collidesWith, collidingTrain}};

            for (RailCarEntity[] array : whatever) {
                LogicalTrainUnit ltu = array[0].ltu;

                if (ltu.trains.length == 1) {
                    LegacyVec3 rot = LegacyVec3.of(0, 0, array[0].getCollisionSpan());
                    rot.rotateAroundX((float) (array[0].getXRot() * Math.PI / 180D));
                    rot.rotateAroundY((float) (-array[0].getYRot() * Math.PI / 180));
                    LegacyVec3 forward = LegacyVec3.of(array[1].getX() - (array[0].getX() + rot.xCoord), 0, array[1].getZ() - (array[0].getZ() + rot.zCoord));
                    LegacyVec3 backward = LegacyVec3.of(array[1].getX() - (array[0].getX() - rot.xCoord), 0, array[1].getZ() - (array[0].getZ() - rot.zCoord));

                    if (forward.lengthVector() > backward.lengthVector()) {
                        ltu.pushForce += push;
                    } else {
                        ltu.pushForce -= push;
                    }
                } else {
                    if (array[0].ltuIndex < ltu.trains.length / 2) {
                        ltu.pushForce -= push;
                    } else {
                        ltu.pushForce += push;
                    }
                }
            }
        }
    }
}
