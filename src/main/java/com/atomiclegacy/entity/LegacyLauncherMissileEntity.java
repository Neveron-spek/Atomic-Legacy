package com.atomiclegacy.entity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.entity.LegacyRadarDetectable;
import com.atomiclegacy.items.CustomMissileData;
import com.atomiclegacy.items.CustomMissileItem;
import com.atomiclegacy.items.LegacyMissileItem;
import com.atomiclegacy.items.MissilePartItem;
import com.atomiclegacy.registry.HbmChunkTickets;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** EntityMissileBaseNT/Custom/AntiBallistic: stored course is multiplied by velocity when moving. */
public final class LegacyLauncherMissileEntity extends Entity implements LegacyRadarDetectable {
    private static final EntityDataAccessor<ItemStack> MISSILE = SynchedEntityData.defineId(LegacyLauncherMissileEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(LegacyLauncherMissileEntity.class, EntityDataSerializers.DIRECTION);
    private int startX, startZ, targetX, targetZ, activationTimer;
    private double velocity, accelXZ, decelY;
    private float health = 50, fuel, consumption;
    private UUID tracking;
    private final Set<Long> forcedChunks = new HashSet<>();
    private double syncX, syncY, syncZ;
    private float syncYaw, syncPitch;
    private int interpolationTicks;

    public LegacyLauncherMissileEntity(EntityType<? extends LegacyLauncherMissileEntity> type, Level level) {
        super(type, level);
    }
    public static LegacyLauncherMissileEntity create(Level level, ItemStack stack, double x, double y, double z,
                                                     int targetX, int targetZ, Direction facing) {
        if (!(stack.getItem() instanceof LegacyMissileItem) && !(stack.getItem() instanceof CustomMissileItem))
            throw new IllegalArgumentException("Not a legacy launchable missile: " + stack);
        LegacyLauncherMissileEntity missile = new LegacyLauncherMissileEntity(HbmEntityTypes.LEGACY_LAUNCHER_MISSILE.get(), level);
        missile.entityData.set(MISSILE, stack.copyWithCount(1));
        missile.entityData.set(FACING, facing);
        missile.setPos((float)x, (float)y, (float)z);
        missile.startX = (int)(float)x; missile.startZ = (int)(float)z;
        missile.targetX = targetX; missile.targetZ = targetZ;
        missile.setDeltaMovement(0, missile.isAntiBallistic() ? 1.5 : 2, 0);
        double distance = new Vec3(targetX - missile.startX, 0, targetZ - missile.startZ).length();
        missile.accelXZ = 1 / distance; missile.decelY = 2 / distance;
        missile.setYRot((float)Math.toDegrees(Math.atan2(targetX - missile.getX(), targetZ - missile.getZ())));
        if (stack.getItem() instanceof CustomMissileItem) {
            CustomMissileData custom = CustomMissileData.read(stack);
            if (custom == null) throw new IllegalArgumentException("Invalid assembled missile: " + stack);
            missile.fuel = custom.fuselageDefinition().primary();
            missile.consumption = MissilePartItem.definition(custom.thruster()).primary();
        }
        missile.yRotO = missile.getYRot();
        return missile;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MISSILE, ItemStack.EMPTY); builder.define(FACING, Direction.EAST);
    }
    public ItemStack missileItem() { return entityData.get(MISSILE); }
    public Direction launchFacing() { return entityData.get(FACING); }
    public String missileId() { return BuiltInRegistries.ITEM.getKey(missileItem().getItem()).getPath(); }
    public boolean isAntiBallistic() { return missileId().equals("missile_anti_ballistic"); }
    public void setTrackingTarget(Entity entity) { if (isAntiBallistic()) tracking = entity.getUUID(); }
    public double velocity() { return velocity; }
    public boolean hasPropulsion() { return !(missileItem().getItem() instanceof CustomMissileItem) || fuel > 0; }

    @Override public void tick() {
        super.tick();
        xo = getX(); yo = getY(); zo = getZ(); yRotO = getYRot(); xRotO = getXRot();
        if (level().isClientSide) { interpolateClient(); return; }
        if (missileItem().isEmpty()) throw new IllegalStateException("Missile entity has no missile item");
        if (missileItem().getItem() instanceof CustomMissileItem && hasPropulsion()) fuel -= consumption;
        updateForcedChunks();
        Vec3 motion = getDeltaMovement();
        Vec3 end = position().add(motion.scale(velocity));
        BlockHitResult hit = level().clip(new ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK && (!isAntiBallistic() || activationTimer >= 40)) {
            // EntityMissileAntiBallistic.onImpact used the fixed 20F blast when
            // it reached a block after arming.  Keep this separate from the
            // proximity detonation (15F) below; they are two different legacy
            // paths and must not be collapsed into one generic radius.
            if (isAntiBallistic()) LegacyMissileImpacts.antiBallistic(this, 20F);
            else LegacyMissileImpacts.impact(this, hit);
            discard(); return;
        }
        setPos(end);
        if (isAntiBallistic()) tickAntiBallistic();
        else {
            if (velocity < 4) velocity += Mth.clamp(tickCount / 60D * .05, 0, .05);
            if (hasPropulsion()) {
                motion = motion.add(0, -decelY * velocity, 0);
                Vec3 course = new Vec3(targetX - startX, 0, targetZ - startZ).normalize().scale(accelXZ * velocity);
                if (motion.y > 0) motion = motion.add(course);
                if (motion.y < 0) motion = motion.subtract(course);
            } else motion = new Vec3(motion.x * .99, motion.y > -1.5 ? motion.y - .05 : motion.y, motion.z * .99);
            setDeltaMovement(motion);
            if (motion.y < -1.5 && Set.of("missile_cluster", "missile_cluster_strong", "missile_rain").contains(missileId())) {
                LegacyMissileImpacts.impact(this, null); discard(); return;
            }
            setYRot((float)Math.toDegrees(Math.atan2(targetX - getX(), targetZ - getZ())));
        }
        motion = getDeltaMovement();
        if (isAntiBallistic()) setYRot((float)Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float)Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())) - 90);
        while (getXRot() - xRotO < -180) xRotO -= 360;
        while (getXRot() - xRotO >= 180) xRotO += 360;
        while (getYRot() - yRotO < -180) yRotO -= 360;
        while (getYRot() - yRotO >= 180) yRotO += 360;
        hasImpulse = true;
    }
    private void tickAntiBallistic() {
        if (velocity < 6) velocity += .1;
        if (activationTimer < 40) {
            activationTimer++;
            setDeltaMovement(getDeltaMovement().x, 1.5, getDeltaMovement().z);
            return;
        }
        ServerLevel server = (ServerLevel)level();
        Entity target = tracking == null ? null : server.getEntity(tracking);
        boolean hadTarget = target != null;
        if (target == null || !target.isAlive()) {
            target = null;
            // Old targetMissile deliberately kept the last eligible entity inside 1000, not the closest.
            for (Entity candidate : server.getAllEntities()) {
                if (candidate instanceof LegacyLauncherMissileEntity m && !m.isAntiBallistic()
                        && !m.missileId().equals("missile_stealth") && m != this && m.isAlive() && distanceTo(m) < 1000)
                    target = m;
            }
            tracking = target == null ? null : target.getUUID();
            // [parity P1] EntityMissileAntiBallistic: shock ring when a target is first acquired.
            if (!hadTarget && target != null) LegacyBuildingEntity.spawnShock(server, getX(), getY(), getZ(), 24, 3F);
        }
        if (target != null && target.isAlive()) {
            Vec3 delta = target.position().subtract(position());
            double eta = delta.length() / (1.5 * velocity);
            Vec3 predicted = target.position().add(target.position().subtract(new Vec3(target.xo, target.yo, target.zo)).scale(eta));
            if (delta.length() < 10) { LegacyMissileImpacts.antiBallistic(this, 15F); discard(); return; }
            setDeltaMovement(predicted.subtract(position()).normalize().scale(1.5));
        } else if (tickCount > 600 || getY() > 2000) discard();
    }
    private void interpolateClient() {
        if (interpolationTicks > 0) {
            setPos(getX() + (syncX-getX())/interpolationTicks, getY() + (syncY-getY())/interpolationTicks, getZ() + (syncZ-getZ())/interpolationTicks);
            setYRot(getYRot() + Mth.wrapDegrees(syncYaw-getYRot())/interpolationTicks);
            setXRot(getXRot() + (syncPitch-getXRot())/interpolationTicks--);
        }
        spawnContrailClient();
    }
    private double clientVelocity;
    private static final Set<String> SMALL_CONTRAIL = Set.of("missile_test", "missile_micro", "missile_schrabidium",
            "missile_bhole", "missile_taint", "missile_emp", "missile_generic", "missile_decoy", "missile_incendiary",
            "missile_cluster", "missile_buster");
    private static final Set<String> TIER3_CLASS = Set.of("missile_burst", "missile_inferno", "missile_rain", "missile_drill");
    private static final Set<String> TIER4_CLASS = Set.of("missile_nuclear", "missile_nuclear_cluster", "missile_volcano",
            "missile_doomsday", "missile_doomsday_rusted");
    /** [parity P1: missile contrail] EntityMissileBaseNT/Tier3/Tier4/Custom/AntiBallistic client contrails. */
    private void spawnContrailClient() {
        if (isAntiBallistic()) {
            Vec3 v = getDeltaMovement().normalize();
            level().addParticle(HbmParticleTypes.KEROSENE_ROCKET_FLAME.get(), getX() - v.x, getY() - v.y, getZ() - v.z, 0, 0, 0);
            return;
        }
        if (clientVelocity < 4) clientVelocity += Mth.clamp(tickCount / 60D * 0.05D, 0, 0.05);
        var custom = CustomMissileData.read(missileItem());
        if (custom != null) {
            var particle = switch (custom.fuselageDefinition().fuel()) {
                case KEROSENE -> HbmParticleTypes.KEROSENE_ROCKET_FLAME.get();
                case HYDROGEN -> HbmParticleTypes.HYDROGEN_ROCKET_FLAME.get();
                case BALEFIRE -> HbmParticleTypes.BALEFIRE_ROCKET_FLAME.get();
                case SOLID -> HbmParticleTypes.SOLID_ROCKET_FLAME.get();
                case XENON -> null;
            };
            if (particle == null) return;
            Vec3 v = getDeltaMovement().normalize();
            for (int i = 0; i < clientVelocity; i++)
                level().addParticle(particle, getX() - v.x * i, getY() - v.y * i, getZ() - v.z * i, 0, 0, 0);
            return;
        }
        String id = missileId();
        float yaw = getYRot(), pitch = getXRot();
        if (TIER3_CLASS.contains(id)) {
            Vec3 t = new Vec3(0, 0, 0.5).yRot((yaw + 90) * Mth.DEG_TO_RAD).xRot(pitch * Mth.DEG_TO_RAD).yRot(-(yaw + 90) * Mth.DEG_TO_RAD);
            contrail(t.x, t.y, t.z); contrail(-t.z, t.y, t.x); contrail(-t.x, -t.z, -t.z); contrail(t.z, -t.z, -t.x);
        } else if (TIER4_CLASS.contains(id)) {
            Vec3 t = new Vec3(0, 0, 1);
            switch (launchFacing().get3DDataValue()) {
                case 2 -> t = t.yRot((float) -Math.PI / 2F);
                case 4 -> t = t.yRot((float) -Math.PI);
                case 3 -> t = t.yRot((float) -Math.PI / 2F * 3F);
                default -> { }
            }
            t = t.yRot((yaw + 90) * Mth.DEG_TO_RAD).xRot(pitch * Mth.DEG_TO_RAD).yRot(-(yaw + 90) * Mth.DEG_TO_RAD);
            contrail(t.x, t.y, t.z); contrail(0, 0, 0); contrail(-t.x, -t.z, -t.z);
        } else contrail(0, 0, 0);
    }
    /** EntityMissileBaseNT.spawnContrailWithOffset ("missileContrail", maxAge 60 + rand(20) in the particle). */
    private void contrail(double ox, double oy, double oz) {
        Vec3 vec = new Vec3(xo - getX(), yo - getY(), zo - getZ());
        double len = vec.length();
        vec = vec.normalize();
        Vec3 thrust = new Vec3(0, 1, 0).zRot(getXRot() * Mth.DEG_TO_RAD).yRot((getYRot() + 90) * Mth.DEG_TO_RAD);
        var type = SMALL_CONTRAIL.contains(missileId()) ? HbmParticleTypes.MISSILE_CONTRAIL_SMALL.get() : HbmParticleTypes.MISSILE_CONTRAIL.get();
        for (int i = 0; i < Math.max(Math.min(len, 10), 1); i++) {
            double j = i - len;
            level().addParticle(type, getX() - vec.x * j + ox, getY() - vec.y * j + oy, getZ() - vec.z * j + oz,
                    -thrust.x, -thrust.y, -thrust.z);
        }
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        syncX=x; syncY=y; syncZ=z; syncYaw=yaw; syncPitch=pitch; interpolationTicks=steps;
    }
    // [parity P1] EntityMissileAntiBallistic is a plain EntityThrowable: not collidable and never damaged.
    @Override public boolean isPickable() { return !isAntiBallistic(); }
    @Override public boolean canBeCollidedWith() { return isAlive() && !isAntiBallistic(); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isAntiBallistic()) return false;
        if (!level().isClientSide && health > 0 && (health -= amount) <= 0) {
            LegacyMissileImpacts.killMissile(this); discard();
        }
        return true;
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }
    private void updateForcedChunks() {
        if (!(level() instanceof ServerLevel server)) return;
        int x = Mth.floor(getX()) >> 4, z = Mth.floor(getZ()) >> 4, radius = isAntiBallistic() ? 1 : 0;
        Set<Long> desired = new HashSet<>();
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++)
            desired.add(net.minecraft.world.level.ChunkPos.asLong(x+dx,z+dz));
        for (long chunk : Set.copyOf(forcedChunks)) if (!desired.contains(chunk)) {
            ticket(server, chunk, false); forcedChunks.remove(chunk);
        }
        for (long chunk : desired) if (forcedChunks.add(chunk)) ticket(server, chunk, true);
    }
    private void ticket(ServerLevel level, long chunk, boolean force) {
        HbmChunkTickets.LAUNCHER_MISSILES.forceChunk(level, this, net.minecraft.world.level.ChunkPos.getX(chunk), net.minecraft.world.level.ChunkPos.getZ(chunk), force, true);
    }
    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server) for (long chunk : forcedChunks) ticket(server, chunk, false);
        forcedChunks.clear(); super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Missile", missileItem().saveOptional(registryAccess()));
        tag.putInt("Facing", launchFacing().get3DDataValue());
        tag.putInt("sX",startX); tag.putInt("sZ",startZ); tag.putInt("tX",targetX); tag.putInt("tZ",targetZ);
        tag.putDouble("veloc",velocity); tag.putDouble("accel",accelXZ); tag.putDouble("decel",decelY);
        tag.putFloat("fuel",fuel); tag.putFloat("consumption",consumption);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MISSILE, ItemStack.parseOptional(registryAccess(),tag.getCompound("Missile")));
        entityData.set(FACING, Direction.from3DDataValue(tag.getInt("Facing")));
        startX=tag.getInt("sX"); startZ=tag.getInt("sZ"); targetX=tag.getInt("tX"); targetZ=tag.getInt("tZ");
        velocity=tag.getDouble("veloc"); accelXZ=tag.getDouble("accel"); decelY=tag.getDouble("decel");
        fuel=tag.getFloat("fuel"); consumption=tag.getFloat("consumption");
    }
    @Override public String translationKey() {
        if (isAntiBallistic()) return "radar.target.abm";
        if (missileId().equals("missile_doomsday") || missileId().equals("missile_doomsday_rusted")) return "radar.target.doomsday";
        if (missileId().equals("missile_shuttle")) return "radar.target.shuttle";
        if (missileItem().getItem() instanceof CustomMissileItem) return switch (blipLevel()) {
            case TIER10 -> "radar.target.custom10"; case TIER10_15 -> "radar.target.custom1015";
            case TIER15 -> "radar.target.custom15"; case TIER15_20 -> "radar.target.custom1520";
            case TIER20 -> "radar.target.custom20"; default -> "radar.target.custom";
        };
        return "radar.target.tier" + blipLevel();
    }
    @Override public int blipLevel() {
        if (isAntiBallistic()) return TIER_AB;
        if (missileId().equals("missile_decoy")) return TIER4;
        var custom = CustomMissileData.read(missileItem());
        if (custom != null) {
            // [parity P1] EntityMissileCustom.getBlipLevel: exact top/bottom pairs only.
            var f=custom.fuselageDefinition(); var t=f.top(); var b=f.bottom();
            if (t==MissilePartItem.Size.SIZE_10 && b==MissilePartItem.Size.SIZE_10) return TIER10;
            if (t==MissilePartItem.Size.SIZE_10 && b==MissilePartItem.Size.SIZE_15) return TIER10_15;
            if (t==MissilePartItem.Size.SIZE_15 && b==MissilePartItem.Size.SIZE_15) return TIER15;
            if (t==MissilePartItem.Size.SIZE_15 && b==MissilePartItem.Size.SIZE_20) return TIER15_20;
            if (t==MissilePartItem.Size.SIZE_20 && b==MissilePartItem.Size.SIZE_20) return TIER20;
            return TIER1;
        }
        return missileItem().getItem() instanceof LegacyMissileItem m ? m.tier().ordinal() : SPECIAL;
    }
    @Override public boolean canBeSeenBy(Object radar) { return !missileId().equals("missile_stealth"); }
    @Override public boolean paramsApplicable(RadarScanParams params) { return params.scanMissiles(); }
    @Override public boolean suppliesRedstone(RadarScanParams params) { return !isAntiBallistic() && (!params.smartMode() || getDeltaMovement().y < 0); }
}
