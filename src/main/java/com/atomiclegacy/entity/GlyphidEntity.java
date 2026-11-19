package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.logic.EntityWaypoint;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.network.MaxwellGibEffectPayload;
import com.atomiclegacy.pollution.HbmPollutionData;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.worldgen.GlyphidHiveGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.atomiclegacy.util.LegacyDamageResistance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Base 1.7.10 glyphid entity. Variant-specific behavior is added by the formal subclasses. */
public class GlyphidEntity extends Monster {
    public enum Variant {
        NORMAL, INFECTED, RADIOACTIVE, BOMBARDIER, BRAWLER, DIGGER, BLASTER, BEHEMOTH, BRENDA, NUCLEAR, SCOUT
    }

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_INFECTED = 1;
    public static final int TYPE_RADIOACTIVE = 2;

    private static final EntityDataAccessor<Byte> WALL_CLIMBING =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ARMOR =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> SUBTYPE =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static volatile BlockPos guidanceTarget;

    private Entity lastSpecialTarget;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;
    private int specialTimer;
    private int behemothTimer = 120;
    private int behemothBreathTime;
    private int nuclearDeathTicks;
    private boolean nuclearDeathSignalSent;

    public static final int TASK_IDLE = 0;
    public static final int TASK_RETREAT_FOR_REINFORCEMENTS = 1;
    public static final int TASK_BUILD_HIVE = 2;
    public static final int TASK_INITIATE_RETREAT = 3;
    public static final int TASK_FOLLOW = 4;
    public static final int TASK_TERRAFORM = 5;
    public static final int TASK_DIG = 6;
    private int currentTask = TASK_IDLE;
    private boolean hasHome;
    private int homeX;
    private int homeY;
    private int homeZ;
    private boolean hasWaypoint;
    private EntityWaypoint taskWaypoint;
    private int taskX;
    private int taskY;
    private int taskZ;
    private int previousTask = TASK_IDLE;
    private EntityWaypoint previousWaypoint;
    private boolean shouldDig;
    private boolean scoutHasTarget;
    private int scoutTimer;
    private int scoutRange = 45;
    private int scoutMinimumHiveDistance = 8;
    private boolean scoutLargeHive;
    private float scoutLargeHiveChance;
    /** EntityCreature.entityToAttack while it pointed at a high-priority EntityWaypoint. */
    private EntityWaypoint legacyWaypointTarget;
    private int legacyTaskPathCooldown;
    private boolean legacyTravel;
    /** [parity J1] EntityCreature old AI (path following, wander, idle look). */
    private final com.atomiclegacy.entity.ai.LegacyCreatureAi legacyAi;
    /** EntityLivingBase.attackTime used by EntityMob.attackEntity. */
    private int legacyAttackTime;

    public GlyphidEntity(EntityType<? extends GlyphidEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
        legacyAi = new com.atomiclegacy.entity.ai.LegacyCreatureAi(this,
                (com.atomiclegacy.entity.ai.LegacyPathNavigation) navigation);
        moveControl = com.atomiclegacy.entity.ai.LegacyCreatureAi.noMoveControl(this);
        lookControl = com.atomiclegacy.entity.ai.LegacyCreatureAi.headFollowsYaw(this);
    }

    /** [parity J1] 1.7.10 path finder; the old AI follows the path itself. */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.atomiclegacy.entity.ai.LegacyPathNavigation(this, level, true);
    }

    private com.atomiclegacy.entity.ai.LegacyPathNavigation legacyNavigation() {
        return (com.atomiclegacy.entity.ai.LegacyPathNavigation) navigation;
    }

    public static void setGuidanceTarget(BlockPos pos) {
        guidanceTarget = pos == null ? null : pos.immutable();
    }

    public static BlockPos getGuidanceTarget() {
        return guidanceTarget;
    }

    public boolean isNuclearDeathCountdownActive() {
        return getVariant() == Variant.NUCLEAR && nuclearDeathTicks > 0 && !isRemoved();
    }

    /** ItemDefuser interaction with EntityGlyphidNuclear: remove the armed
     * death-countdown glyphid, play the same small fixed-damage VNT effect,
     * and drop the nuke_demo standard ammo. */
    public boolean legacyDefuseNuclearCountdown(Player player) {
        if (!isNuclearDeathCountdownActive() || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        LegacyProjectileUtil.fixedDamageExplosion(serverLevel, this, position(), 5.0F, 20.0F, false);
        spawnAtLocation(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(),
                StandardAmmoItem.StandardAmmoType.NUKE_DEMO), 1.5F);
        discard();
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        GlyphidStats.StatBundle stats = GlyphidStats.forVariant(Variant.NORMAL);
        // EntityGlyphid.applyEntityAttributes only set maxHealth, movementSpeed and attackDamage.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, stats.health())
                .add(Attributes.MOVEMENT_SPEED, stats.speed() * 0.1D)
                .add(Attributes.ATTACK_DAMAGE, stats.damage());
    }

    @Override
    protected void registerGoals() {
        // [parity J1] movement, swimming, wandering, idle look and the melee hit are the old EntityCreature AI
        // (customServerAiStep / LegacyCreatureAi); only target selection stays in goals.
        // [parity A2: glyphid] EntityMob.attackEntityFrom set entityToAttack to the attacker and EntityCreature
        // kept it until it died; there was no line-of-sight or distance timeout.
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canContinueToUse() {
                LivingEntity current = mob.getTarget();
                return current != null && current.isAlive();
            }
        });
        targetSelector.addGoal(2, new GlyphidPlayerTargetGoal());
    }

    /**
     * EntityGlyphid.findPlayerToAttack: getClosestVulnerablePlayerToEntity needs no line of sight, and the old
     * EntityCreature kept its target until it died instead of dropping it by distance.
     */
    private final class GlyphidPlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        private GlyphidPlayerTargetGoal() {
            super(GlyphidEntity.this, Player.class, 0, false, false, null);
        }

        @Override
        protected double getFollowDistance() {
            return GlyphidEntity.this.legacyTargetRange();
        }

        @Override
        public boolean canUse() {
            if (GlyphidEntity.this.legacyWaypointTarget != null) {
                return false;
            }
            target = GlyphidEntity.this.findLegacyPlayerTarget();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity current = mob.getTarget();
            if (current == null || !current.isAlive()
                    || GlyphidEntity.this.hasEffect(MobEffects.BLINDNESS)) {
                return false;
            }
            return !(current instanceof Player player) || (!player.isCreative() && !player.isSpectator());
        }
    }

    private double legacyTargetRange() {
        if (getVariant() == Variant.SCOUT) {
            return 10.0D;
        }
        return useExtendedTargeting() ? 128.0D : 16.0D;
    }

    /** EntityGlyphid / EntityGlyphidScout.findPlayerToAttack. */
    @Nullable
    private Player findLegacyPlayerTarget() {
        if (hasEffect(MobEffects.BLINDNESS)) {
            return null;
        }
        return level().getNearestPlayer(
                TargetingConditions.forCombat().range(legacyTargetRange()).ignoreLineOfSight(), this);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null) {
            legacyWaypointTarget = null;
        }
        super.setTarget(target);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WALL_CLIMBING, (byte) 0);
        builder.define(ARMOR, (byte) 0b11111);
        builder.define(SUBTYPE, (byte) TYPE_NORMAL);
        builder.define(VARIANT, (byte) Variant.NORMAL.ordinal());
    }

    public Variant getVariant() {
        int id = entityData.get(VARIANT);
        return id < 0 || id >= Variant.values().length ? Variant.NORMAL : Variant.values()[id];
    }

    @Override
    public Component getName() {
        Component customName = getCustomName();
        if (customName != null) {
            return customName;
        }
        String suffix = switch (getVariant()) {
            case BEHEMOTH -> "_behemoth";
            case BLASTER -> "_blaster";
            case BOMBARDIER -> "_bombardier";
            case BRAWLER -> "_brawler";
            case BRENDA -> "_brenda";
            case DIGGER -> "_digger";
            case NUCLEAR -> "_nuclear";
            case SCOUT -> "_scout";
            default -> "";
        };
        return Component.translatable("entity.atomiclegacy.glyphid" + suffix);
    }

    public void setVariant(Variant variant) {
        // The 1.7.10 classes were constructed at full health; a freshly created shared entity must not keep
        // the grunt's 20 health when it becomes a larger variant (hive swarms, scout escorts).
        boolean fresh = tickCount == 0;
        entityData.set(VARIANT, (byte) variant.ordinal());
        refreshAttributes();
        refreshDimensions();
        if (fresh) {
            setHealth(getMaxHealth());
        }
    }

    public int getSubtype() {
        return entityData.get(SUBTYPE);
    }

    public void setSubtype(int subtype) {
        entityData.set(SUBTYPE, (byte) Math.max(TYPE_NORMAL, Math.min(TYPE_RADIOACTIVE, subtype)));
        refreshAttributes();
    }

    public int armorCount() {
        return Integer.bitCount(entityData.get(ARMOR) & 0x1F);
    }

    public byte armorMask() {
        return entityData.get(ARMOR);
    }

    public GlyphidStats.StatBundle stats() {
        return GlyphidStats.forVariant(getVariant());
    }

    /** Scale used by the 1.7.10 OBJ renderer for each concrete glyphid class. */
    public double modelScale() {
        return switch (getVariant()) {
            case BRAWLER, BLASTER -> 1.25D;
            case DIGGER -> 1.30D;
            case BEHEMOTH -> 1.50D;
            case BRENDA, NUCLEAR -> 2.0D;
            case SCOUT -> 0.75D;
            default -> 1.0D;
        };
    }

    public String textureName() {
        return switch (getVariant()) {
            case BOMBARDIER -> "glyphid_bombardier";
            case BRAWLER -> "glyphid_brawler";
            case DIGGER -> "glyphid_digger";
            case BLASTER -> "glyphid_blaster";
            case BEHEMOTH -> "glyphid_behemoth";
            case BRENDA -> "glyphid_brenda";
            case NUCLEAR -> "glyphid_nuclear";
            case SCOUT -> "glyphid_scout";
            default -> "glyphid";
        };
    }

    public boolean hasArmor(int segment) {
        return (entityData.get(ARMOR) & (1 << segment)) != 0;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return switch (getVariant()) {
            case BRAWLER, BLASTER -> EntityDimensions.fixed(2.0F, 1.125F);
            case BEHEMOTH -> EntityDimensions.fixed(2.5F, 1.5F);
            case BRENDA, NUCLEAR -> EntityDimensions.fixed(2.5F, 1.75F);
            case SCOUT -> EntityDimensions.fixed(1.25F, 0.75F);
            default -> EntityDimensions.fixed(1.75F, 1.0F);
        };
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        /*
         * In 1.7.10 EntityGlyphid#getCanSpawnHere rejected block collisions,
         * entity collisions and liquids.  Mob#checkSpawnObstruction in 1.21
         * only covers liquids and obstructing entities, so using it alone lets
         * a hive accept a wide variant while its refreshed bounding box still
         * intersects the hive shell.  Restore the omitted full-box collision
         * query here so every glyphid spawn path receives the legacy safety
         * rule, including hive swarms and natural scout escorts.
         */
        return level.noCollision(this, getBoundingBox())
                && super.checkSpawnObstruction(level);
    }

    private void refreshAttributes() {
        GlyphidStats.StatBundle stats = stats();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.health());
        // EntityGlyphid.applyEntityAttributes read DW_SUBTYPE inside the constructor, before any spawner
        // could set it, and the attributes were never recomputed afterwards (they are restored from NBT).
        // The x2 speed / x5 damage of TYPE_RADIOACTIVE therefore never applied in 1.7.10.
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(stats.speed() * 0.1D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stats.damage());
        // EntityType construction applies attributes after the entity
        // constructor.  A fixed-variant glyphid calls setVariant from that
        // constructor, so its initial health is still zero at this point.
        // Vanilla Mob initialises newly-created entities to max health; keep
        // that same initialisation instead of preserving the pre-attribute
        // zero value.  Existing entities retain their current health when
        // their variant/subtype changes.
        setHealth(getHealth() <= 0.0F ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        refreshAttributes();
        setHealth(getMaxHealth());
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        tickLegacyTaskSystem();
        // setBesideClimbableBlock(isCollidedHorizontally); the climb itself is onClimbable() (isOnLadder).
        entityData.set(WALL_CLIMBING, (byte) (horizontalCollision ? 1 : 0));
        if (tickCount % 100 == 0) {
            swing(InteractionHand.MAIN_HAND);
        }
        runLegacyVariantBehavior();
    }

    /** Mirrors EntityGlyphid.onUpdate / updateEntityActionState: task state, targeting and waypoint paths. */
    private void tickLegacyTaskSystem() {
        if (!hasHome) {
            homeX = (int) getX();
            homeY = (int) getY();
            homeZ = (int) getZ();
            hasHome = true;
        }

        boolean blinded = hasEffect(MobEffects.BLINDNESS);
        if (blinded) {
            onLegacyBlinded();
        }

        if (currentTask == TASK_FOLLOW) {
            if (isAtDestination() && !hasWaypoint) {
                setCurrentTask(TASK_IDLE, null);
            }
        } else if (currentTask == TASK_DIG && tickCount % 20 == 0 && isAtDestination()) {
            swing(InteractionHand.MAIN_HAND);
            performLegacyDigExplosion();
            setCurrentTask(previousTask, previousWaypoint);
        }

    }

    /**
     * [parity J1] EntityGlyphid.updateEntityActionState: EntityCreature.updateEntityActionState (EntityMob attack,
     * path to entityToAttack or wander, old path following) followed by the target rescan and the partial paths of
     * PathFinderUtils while there is no path.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (legacyAttackTime > 0) {
            legacyAttackTime--; // EntityLivingBase.onEntityUpdate
        }
        legacyAi.tickFleeing();
        Entity entityToAttack = legacyEntityToAttack();
        if (entityToAttack != null && !entityToAttack.isAlive()) {
            if (entityToAttack == legacyWaypointTarget) {
                legacyWaypointTarget = null;
            } else {
                setTarget(null);
            }
            entityToAttack = null;
        } else if (entityToAttack != null && hasLineOfSight(entityToAttack)) {
            // [parity K1] EntityCreature: attackEntity(entityToAttack) also for a high-priority EntityWaypoint
            legacyAttackEntity(entityToAttack, distanceTo(entityToAttack));
        }
        legacyAi.repath(entityToAttack, false, false, () -> {
            if (currentTask == TASK_IDLE) { // EntityGlyphid.updateWanderPath
                legacyAi.updateWanderPath(pos -> getWalkTargetValue(pos, level()));
            }
        });
        legacyAi.followPath(entityToAttack, false, getAttributeValue(Attributes.MOVEMENT_SPEED) * 10.0D);

        tickLegacyTargeting();
        if (!hasEffect(MobEffects.BLINDNESS)) {
            tickLegacyTaskNavigation();
        }
    }

    /** EntityCreature.entityToAttack: the attack target or the high-priority waypoint. */
    @Nullable
    private Entity legacyEntityToAttack() {
        return legacyWaypointTarget != null ? legacyWaypointTarget : getTarget();
    }

    /** EntityMob.attackEntity: attackTime 20, distance < 2 and vertically overlapping boxes. */
    private void legacyAttackEntity(Entity target, float distance) {
        AABB own = getBoundingBox();
        AABB other = target.getBoundingBox();
        if (legacyAttackTime <= 0 && distance < 2.0F && other.maxY > own.minY && other.minY < own.maxY) {
            legacyAttackTime = 20;
            doHurtTarget(target);
        }
    }

    private void tickLegacyTargeting() {
        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) {
            setTarget(null);
        }
        if (legacyWaypointTarget != null && !legacyWaypointTarget.isAlive()) {
            legacyWaypointTarget = null;
        }
        // re-scan for new targets every so often; every third glyphid does not do this
        if (getId() % 3 > 0 && (getId() + tickCount) % 100 == 0) {
            Player newTarget = findLegacyPlayerTarget();
            if (newTarget != null) {
                setTarget(newTarget);
            }
        }
    }

    /** The "!hasPath()" branch of EntityGlyphid.updateEntityActionState. */
    private void tickLegacyTaskNavigation() {
        if (legacyTaskPathCooldown > 0) {
            legacyTaskPathCooldown--;
        }
        if (!getNavigation().isDone()) {
            return;
        }
        Entity entityToAttack = legacyEntityToAttack();
        if (entityToAttack != null && useExtendedTargeting()) {
            // PathFinderUtils.getPathEntityToEntityPartial(this, entityToAttack, 16F, true, false, true, true)
            if (legacyTaskPathCooldown <= 0) {
                setLegacyPath(legacyNavigation().partialPathToEntity(entityToAttack, 16.0F, true));
            }
            return;
        }
        if (currentTask == TASK_IDLE || isAtDestination()) {
            return;
        }
        if (taskWaypoint != null) {
            taskX = (int) taskWaypoint.getX();
            taskY = (int) taskWaypoint.getY();
            taskZ = (int) taskWaypoint.getZ();
            if (taskWaypoint.isHighPriority() && taskWaypoint.isAlive()) {
                // setTarget(taskWaypoint): the waypoint replaces the attack target
                setTarget(null);
                legacyWaypointTarget = taskWaypoint;
            }
        }
        if (!hasWaypoint) {
            return;
        }
        if (canDig()) {
            BlockHitResult obstruction = findWaypointObstruction();
            if (modelScale() >= 1.0D && currentTask != TASK_DIG && obstruction != null) {
                digToWaypoint(obstruction);
                return;
            }
        }
        if (legacyTaskPathCooldown <= 0) {
            setLegacyPath(legacyTaskPath());
        }
    }

    /** PathFinderUtils.getPathEntityToCoordPartial(this, taskX, taskY, taskZ, (int) (distance * 1.2), true, false, true, true). */
    private net.minecraft.world.level.pathfinder.Path legacyTaskPath() {
        int maxDist = (int) (Math.sqrt(distanceToSqr(taskX, taskY, taskZ)) * 1.2D);
        return legacyNavigation().partialPathToCoord(taskX, taskY, taskZ, maxDist, true);
    }

    /**
     * EntityCreature.setPathToEntity. 1.7.10 retried a failed search every tick; failed searches are spaced out by 10
     * ticks here (technical adaptation for the cost of the search).
     */
    private void setLegacyPath(@Nullable net.minecraft.world.level.pathfinder.Path path) {
        legacyNavigation().setLegacyPath(path);
        if (path == null) {
            legacyTaskPathCooldown = 10;
        }
    }

    /** Direct port of EntityGlyphid.onBlinded, including the large-glyphid
     * lantern-breaking behavior used by the old AI. */
    private void onLegacyBlinded() {
        setTarget(null);
        legacyWaypointTarget = null;
        getNavigation().stop();
        legacyAi.fleeingTick = 80;
        if (modelScale() < 1.25D || tickCount % 20 != 0) {
            return;
        }
        for (int index = 0; index < 16; index++) {
            float angle = (float) Math.toRadians(360.0D / 16.0D * index);
            Vec3 direction = new Vec3(0.0D, 0.0D, 4.0D).yRot(angle);
            Vec3 start = new Vec3(getX(), getY() + 1.0D, getZ());
            Vec3 end = start.add(direction);
            // World.rayTraceBlocks(start, end) also stopped at blocks without a collision box
            BlockHitResult hit = level().clip(new ClipContext(
                    start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK
                    && level().getBlockState(hit.getBlockPos()).is(HbmBlocks.LANTERN.get())) {
                setYRot(360.0F / 16.0F * index);
                swing(InteractionHand.MAIN_HAND);
                level().destroyBlock(hit.getBlockPos(), false); // World.func_147480_a(x, y, z, false)
            }
        }
    }

    private boolean canDig() {
        return getVariant() == Variant.DIGGER || HbmConfig.glyphidDig();
    }

    /** Matches EntityGlyphid's 1.7.10 obstruction trace. */
    private BlockHitResult findWaypointObstruction() {
        Vec3 start = getEyePosition();
        Vec3 end = new Vec3(taskX, taskY, taskZ);
        BlockHitResult hit = (BlockHitResult) level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level().getBlockState(pos);
        return state.getBlock().getExplosionResistance() <= digResistance() ? hit : null;
    }

    /**
     * Creates the temporary five-block dig waypoint used by the old AI and
     * stores the task which must be restored after the blast.
     */
    private void digToWaypoint(BlockHitResult obstacle) {
        EntityWaypoint digWaypoint = new EntityWaypoint(
                HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
        BlockPos pos = obstacle.getBlockPos();
        digWaypoint.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        digWaypoint.setRadius(5);
        level().addFreshEntity(digWaypoint);

        previousTask = currentTask;
        previousWaypoint = taskWaypoint;
        setCurrentTask(TASK_DIG, digWaypoint);
        legacyNavigation().setLegacyPath(legacyTaskPath());
        communicate(TASK_DIG, digWaypoint);
    }

    public int digBlastSize() {
        return Math.min((int) (3.0D * modelScale()) / 2, 5);
    }

    public int digResistance() {
        return Math.min((int) (50.0D * (modelScale() * 2.0D)), 150);
    }

    /**
     * Port of BlockAllocatorGlyphidDig plus BlockProcessorStandard.setNoDrop.
     * The old implementation did not create explosion effects or entity
     * damage for this task; only the allocated blocks are removed.
     */
    private void performLegacyDigExplosion() {
        double centerX = taskX;
        double centerY = taskY + 2.0D;
        double centerZ = taskZ;
        float size = digBlastSize();
        int resolution = 16;
        Set<BlockPos> affected = new HashSet<>();

        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    if (i != 0 && i != resolution - 1
                            && j != 0 && j != resolution - 1
                            && k != 0 && k != resolution - 1) {
                        continue;
                    }

                    double directionX = (double) ((float) i / (resolution - 1.0F) * 2.0F - 1.0F);
                    double directionY = (double) ((float) j / (resolution - 1.0F) * 2.0F - 1.0F);
                    double directionZ = (double) ((float) k / (resolution - 1.0F) * 2.0F - 1.0F);
                    double length = Math.sqrt(directionX * directionX
                            + directionY * directionY + directionZ * directionZ);
                    directionX /= length;
                    directionY /= length;
                    directionZ /= length;

                    double currentX = centerX;
                    double currentY = centerY;
                    double currentZ = centerZ;
                    double distance = 0.0D;
                    while (distance <= size) {
                        distance = Math.sqrt(
                                (currentX - centerX) * (currentX - centerX)
                                        + (currentY - centerY) * (currentY - centerY)
                                        + (currentZ - centerZ) * (currentZ - centerZ));
                        BlockPos pos = new BlockPos(
                                Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
                        BlockState state = level().getBlockState(pos);

                        if (!state.isAir()) {
                            if (state.getBlock().getExplosionResistance() > digResistance()
                                    || state.is(HbmBlocks.GLYPHID_SPAWNER.get())) {
                                break;
                            }
                        }
                        affected.add(pos);
                        currentX += directionX * 0.3D;
                        currentY += directionY * 0.3D;
                        currentZ += directionZ * 0.3D;
                    }
                }
            }
        }

        for (BlockPos pos : affected) {
            if (!level().getBlockState(pos).isAir()) {
                // [parity A2: glyphid] BlockProcessorStandard.setNoDrop: setBlockToAir, no break effects or sound
                level().removeBlock(pos, false);
            }
        }
    }

    private void runLegacyVariantBehavior() {
        LivingEntity target = getTarget();
        switch (getVariant()) {
            case SCOUT -> runScoutBehavior();
            case BOMBARDIER, BLASTER -> bombardierAttack(target);
            case BRAWLER -> brawlerLeap(target);
            case DIGGER -> diggerSlam(target);
            case BEHEMOTH -> behemothAttack(target);
            case NUCLEAR -> runNuclearBehavior();
            default -> {
            }
        }
    }

    private void runScoutBehavior() {
        // Updates to check whether the player still exists: entityToAttack = findPlayerToAttack() (10 blocks)
        if ((getTarget() != null || legacyWaypointTarget != null) && tickCount % 60 == 0) {
            legacyWaypointTarget = null;
            setTarget(findLegacyPlayerTarget());
        }

        // This condition intentionally follows EntityGlyphidScout's old
        // task gate: a Scout without a waypoint keeps looking for expansion
        // work even after a previous task has completed.
        if ((currentTask != TASK_BUILD_HIVE || currentTask != TASK_TERRAFORM) && taskWaypoint == null) {
            BlockPos sleepTarget = getGuidanceTarget();
            // MobConfig.rampantGlyphidGuidance && PollutionHandler.targetCoords != null
            if (HbmConfig.glyphidGuidance() && sleepTarget != null) {
                if (!scoutHasTarget) {
                    // playerBaseDirFinder: the 1.7.10 Vec3.subtract(v) returned "v - this", so the
                    // waypoint lies 10 blocks from the scout TOWARD the player / last bed position.
                    Vec3 point = scoutGuidancePoint(position(), scoutGuidanceTarget(sleepTarget));
                    EntityWaypoint target = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    target.moveTo(point.x, point.y, point.z, 0.0F, 0.0F);
                    target.setMaxAge(300);
                    target.setRadius(6);
                    target.setWaypointType(TASK_BUILD_HIVE);
                    level().addFreshEntity(target);
                    scoutHasTarget = true;
                    setCurrentTask(TASK_RETREAT_FOR_REINFORCEMENTS, target);
                }
                if (isAtDestinationBase()) {
                    setCurrentTask(TASK_BUILD_HIVE, null);
                    scoutHasTarget = false;
                }
            } else {
                setCurrentTask(TASK_BUILD_HIVE, null);
            }
        }

        if (currentTask != TASK_BUILD_HIVE && currentTask != TASK_TERRAFORM) {
            return;
        }

        if (!scoutHasTarget) {
            if (scoutRange != 60 && hasNuclearGlyphidNearby()) {
                setCurrentTask(TASK_TERRAFORM, null);
            }
            if (expandScoutHive()) {
                addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 180 * 20, 1));
                scoutHasTarget = true;
            }
        }
        if (taskWaypoint == null && scoutHasTarget) {
            scoutHasTarget = false;
        }

        if (currentTask == TASK_TERRAFORM && isAtDestinationBase() && scoutCanBuildHiveHere()) {
            communicate(TASK_TERRAFORM, taskWaypoint);
        }

        if (tickCount % 10 != 0 || !isAtDestination()) {
            return;
        }
        // The timer advances before canBuildHiveHere() and is never reset.
        scoutTimer++;
        if (!scoutCanBuildHiveHere()) {
            return;
        }
        if (scoutTimer == 1) {
            EntityWaypoint returnPoint = new EntityWaypoint(
                    HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
            returnPoint.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
            returnPoint.setWaypointType(TASK_IDLE);

            EntityWaypoint home = new EntityWaypoint(
                    HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
            home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
            home.setAdditionalWaypoint(returnPoint);
            home.moveTo(homeX, homeY, homeZ, 0.0F, 0.0F);
            home.setMaxAge(1200);
            home.setRadius(6);
            level().addFreshEntity(home);

            // "this.taskWaypoint = home": the task coordinates keep pointing at the nest, so the scout
            // stays there (slowed) while the other glyphids are sent home for reinforcements.
            taskWaypoint = home;
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 * 20, 10));
            communicate(TASK_RETREAT_FOR_REINFORCEMENTS, taskWaypoint);
        } else if (scoutTimer >= 5) {
            level().explode(this, getX(), getY(), getZ(), 5.0F, false,
                    Level.ExplosionInteraction.NONE);
            GlyphidHiveGenerator.generateSmall(level(), Mth.floor(getX()), Mth.floor(getY()),
                    Mth.floor(getZ()), random, getSubtype() != TYPE_NORMAL, false);
            discard();
        } else {
            communicate(TASK_FOLLOW, taskWaypoint);
        }
    }

    /** EntityGlyphidScout.getPlayerTargetDirection: nearest player within 300 blocks, else the bed coords. */
    private Vec3 scoutGuidanceTarget(BlockPos sleepTarget) {
        Player player = level().getNearestPlayer(this, 300.0D);
        if (player != null) {
            return player.position();
        }
        return new Vec3(sleepTarget.getX(), sleepTarget.getY(), sleepTarget.getZ());
    }

    /** EntityGlyphidScout.playerBaseDirFinder with the 1.7.10 Vec3.subtract semantics (target - current). */
    public static Vec3 scoutGuidancePoint(Vec3 current, Vec3 target) {
        Vec3 direction = target.subtract(current).normalize();
        return current.add(direction.scale(10.0D));
    }

    /** The first eight of sixteen horizontal rays of EntityGlyphidScout.canBuildHiveHere. */
    public static Vec3 scoutHiveProbeOffset(int index, int length) {
        float angle = (float) Math.toRadians(360.0D / 16.0D * index);
        return new Vec3(0.0D, 0.0D, length).yRot(angle);
    }

    private boolean scoutCanBuildHiveHere() {
        int length = scoutLargeHive ? 16 : 8;
        Vec3 start = new Vec3(getX(), getY() + 1.0D, getZ());
        for (int index = 0; index < 8; index++) {
            Vec3 end = start.add(scoutHiveProbeOffset(index, length));
            // World.rayTraceBlocks(start, end) also stopped at blocks without a collision box
            BlockHitResult hit = level().clip(new ClipContext(
                    start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK
                    && level().getBlockState(hit.getBlockPos()).is(HbmBlocks.GLYPHID_BASE.get())) {
                setCurrentTask(TASK_IDLE, null);
                scoutHasTarget = false;
                return false;
            }
        }
        return true;
    }

    private boolean hasNuclearGlyphidNearby() {
        AABB area = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(8.0D);
        return !level().getEntitiesOfClass(GlyphidEntity.class, area,
                glyphid -> glyphid != this && glyphid.getVariant() == Variant.NUCLEAR).isEmpty();
    }

    private boolean expandScoutHive() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        int nestX = random.nextInt((homeX + scoutRange) - (homeX - scoutRange)) + (homeX - scoutRange);
        int nestZ = random.nextInt((homeZ + scoutRange) - (homeZ - scoutRange)) + (homeZ - scoutRange);
        BlockPos column = new BlockPos(nestX, homeY, nestZ);
        if (!serverLevel.hasChunkAt(column)) {
            return false; // do not load or generate chunks for a candidate
        }
        // World.getHeightValue: the 1.7.10 height map counted every light-blocking block (leaves, liquids).
        int nestY = serverLevel.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, nestX, nestZ);
        BlockPos groundPos = new BlockPos(nestX, nestY - 1, nestZ);
        BlockState ground = level().getBlockState(groundPos);
        // EntityGlyphidScout measured the candidate against its recorded
        // hive home, not against the scout's current position.  Keeping the
        // home-relative distance matters after a scout has travelled toward
        // a previous waypoint.
        double homeDistanceX = nestX - homeX;
        double homeDistanceY = nestY - homeY;
        double homeDistanceZ = nestZ - homeZ;
        boolean farEnough = Math.sqrt(homeDistanceX * homeDistanceX
                + homeDistanceY * homeDistanceY
                + homeDistanceZ * homeDistanceZ) > scoutMinimumHiveDistance;
        if (!farEnough || ground.isAir() || !ground.isCollisionShapeFullBlock(level(), groundPos)
                || ground.is(HbmBlocks.GLYPHID_BASE.get())) {
            return false;
        }

        if (ground.is(HbmBlocks.BASALT.get())) {
            scoutLargeHive = true;
            scoutLargeHiveChance = Math.max(1.0F, HbmConfig.GLYPHID_LARGE_HIVE_CHANCE.get() / 2.0F);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 3));
        }

        EntityWaypoint nest = new EntityWaypoint(HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
        nest.setWaypointType(currentTask);
        nest.setRadius(5);
        if (scoutLargeHive) {
            nest.setHighPriority();
        }
        nest.moveTo(nestX, nestY, nestZ, 0.0F, 0.0F);
        serverLevel.addFreshEntity(nest);
        taskWaypoint = nest;
        // updates the task coordinates
        setCurrentTask(currentTask, taskWaypoint);
        communicate(TASK_BUILD_HIVE, taskWaypoint);
        return true;
    }

    private void bombardierAttack(LivingEntity target) {
        if (target == null) {
            return;
        }
        if (tickCount % 20 == 0) {
            lastSpecialTarget = target;
            lastTargetX = target.getX();
            lastTargetY = target.getY();
            lastTargetZ = target.getZ();
        }
        if (tickCount % 60 != 1) {
            return;
        }
        boolean topAttack = distanceTo(target) > 20.0F;
        double velocityX = target.getX() - lastTargetX;
        double velocityY = target.getY() - lastTargetY;
        double velocityZ = target.getZ() - lastTargetZ;
        if (lastSpecialTarget != target || new Vec3(velocityX, velocityY, velocityZ).length() > 30.0D) {
            velocityX = velocityY = velocityZ = 0.0D;
        }
        int prediction = topAttack ? 60 : 20;
        Vec3 delta = new Vec3(
                target.getX() - getX() + velocityX * prediction,
                target.getY() + target.getBbHeight() * 0.5D - (getY() + 1.0D) + velocityY * prediction,
                target.getZ() - getZ() + velocityZ * prediction
        );
        if (delta.length() < 3.0D) {
            return;
        }
        double targetYaw = -Math.atan2(delta.x, delta.z);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = getVariant() == Variant.BLASTER ? 1.25D : 1.0D;
        double speedSquared = speed * speed;
        double gravity = 0.04D;
        double discriminant = speedSquared * speedSquared
                - gravity * (gravity * horizontal * horizontal + 2.0D * delta.y * speedSquared);
        if (discriminant < 0.0D) {
            return;
        }
        double targetPitch = Math.atan((speedSquared + Math.sqrt(discriminant) * (topAttack ? 1.0D : -1.0D))
                / (gravity * horizontal));
        if (Double.isNaN(targetPitch)) {
            return;
        }
        Vec3 fireVector = new Vec3(speed, 0.0D, 0.0D)
                .zRot((float) -targetPitch)
                .yRot((float) -(targetYaw + Math.PI * 0.5D));
        int bombCount = getVariant() == Variant.BLASTER ? 10 : 5;
        float spread = getVariant() == Variant.BLASTER ? 0.5F : 1.0F;
        float damage = getVariant() == Variant.BLASTER ? 15.0F : 5.0F;
        for (int index = 0; index < bombCount; index++) {
            level().addFreshEntity(new GlyphidAcidBombEntity(level(), this,
                    new Vec3(getX(), getY() + 1.0D, getZ()), fireVector,
                    (float) speed, index * spread, damage));
        }
        swing(InteractionHand.MAIN_HAND);
    }

    private void runNuclearBehavior() {
        if (tickCount % 20 != 0) {
            return;
        }
        if (currentTask == TASK_FOLLOW && isAtDestination()) {
            setCurrentTask(TASK_IDLE, null);
        }
        // [parity A2: glyphid] getAITarget() is the revenge target (last attacker), not the attack target
        if (currentTask == TASK_BUILD_HIVE && getLastHurtByMob() == null) {
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 10 * 20, 3));
        }
        if (currentTask == TASK_TERRAFORM) {
            setHealth(0.0F);
        }
    }

    private void brawlerLeap(LivingEntity target) {
        if (target == null || !isAlive()) {
            return;
        }
        lastTargetX = target.getX();
        lastTargetY = target.getY();
        lastTargetZ = target.getZ();
        if (--specialTimer > 0) {
            return;
        }
        specialTimer = 80 + level().random.nextInt(30);
        if (distanceTo(target) >= 20.0F) {
            return;
        }
        // The 1.7.10 brawler never assigned lastTarget, so its prediction velocity is zero.
        Vec3 delta = new Vec3(
                target.getX() - getX(),
                target.getY() + target.getBbHeight() * 0.5D - (getY() + 1.0D),
                target.getZ() - getZ()
        );
        if (delta.length() < 3.0D) {
            return;
        }
        double yaw = -Math.atan2(delta.x, delta.z);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = 1.5D;
        double gravity = 0.01D;
        double discriminant = speed * speed * speed * speed
                - gravity * (gravity * horizontal * horizontal + 2.0D * delta.y * speed * speed);
        if (discriminant < 0.0D) {
            return;
        }
        double pitch = Math.atan((speed * speed + Math.sqrt(discriminant)) / (gravity * horizontal));
        if (Double.isNaN(pitch)) {
            return;
        }
        Vec3 fire = new Vec3(speed, 0.0D, 0.0D)
                .zRot((float) (-pitch / 3.5D))
                .yRot((float) -(yaw + Math.PI * 0.5D));
        // EntityGlyphidBrawler.setThrowableHeading(x, y, z, v0, rand.nextFloat())
        float inaccuracy = random.nextFloat();
        Vec3 motion = fire.normalize().add(
                random.nextGaussian() * 0.0075D * inaccuracy,
                random.nextGaussian() * 0.0075D * inaccuracy,
                random.nextGaussian() * 0.0075D * inaccuracy
        ).scale((float) speed);
        setDeltaMovement(motion);
        float headingYaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float headingPitch = (float) (Math.atan2(motion.y,
                Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * 180.0D / Math.PI);
        setYRot(headingYaw);
        setXRot(headingPitch);
        yRotO = headingYaw;
        xRotO = headingPitch;
    }

    private void diggerSlam(LivingEntity target) {
        if (target == null || !isAlive()) {
            return;
        }
        lastTargetX = target.getX();
        lastTargetY = target.getY();
        lastTargetZ = target.getZ();
        if (--specialTimer > 0) {
            return;
        }
        specialTimer = 120;
        if (distanceTo(target) >= 30.0F) {
            return;
        }
        int baseX = (int) getX();
        int baseY = (int) getY();
        int baseZ = (int) getZ();
        Vec3 direction = getLookAngle();
        java.util.List<net.minecraft.core.BlockPos> path = blockPath(baseX, baseY, baseZ, 6, direction);
        for (int index = 0; index < 8; index++) {
            direction = direction.yRot(-1.0F / 16.0F);
            path.addAll(blockPath(baseX, baseY - 1, baseZ, 6, direction));
        }

        boolean topAttack = distanceTo(target) > 20.0F;
        double velocityX = target.getX() - lastTargetX;
        double velocityY = target.getY() - lastTargetY;
        double velocityZ = target.getZ() - lastTargetZ;
        if (lastSpecialTarget != target) {
            velocityX = velocityY = velocityZ = 0.0D;
        }
        int prediction = 60;
        Vec3 delta = new Vec3(
                target.getX() - getX() + velocityX * prediction,
                target.getY() + target.getBbHeight() * 0.5D - (getY() + 1.0D) + velocityY * prediction,
                target.getZ() - getZ() + velocityZ * prediction
        );
        if (delta.length() < 3.0D) {
            return;
        }
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = 1.2D;
        double gravity = 0.03D;
        double discriminant = speed * speed * speed * speed
                - gravity * (gravity * horizontal * horizontal + 2.0D * delta.y * speed * speed);
        Vec3 fire = null;
        if (discriminant >= 0.0D) {
            double pitch = Math.atan((speed * speed + Math.sqrt(discriminant) * (topAttack ? 1.0D : -1.0D))
                    / (gravity * horizontal));
            if (!Double.isNaN(pitch)) {
                fire = new Vec3(speed, 0.0D, 0.0D)
                        .zRot((float) -pitch)
                        .yRot((float) -(-Math.atan2(delta.x, delta.z) + Math.PI * 0.5D));
            }
        }
        for (net.minecraft.core.BlockPos pos : path) {
            net.minecraft.world.level.block.state.BlockState state = level().getBlockState(pos);
            if (state.isAir() || !state.isCollisionShapeFullBlock(level(), pos)
                    || state.getBlock() instanceof com.atomiclegacy.blocks.MachineDummyBlock
                    || level().getBlockEntity(pos) != null
                    || state.getBlock().getExplosionResistance() >= HbmBlocks.CONCRETE.get().getExplosionResistance()) {
                continue;
            }
            Vec3 motion = Vec3.ZERO;
            if (fire != null) {
                // EntityRubble.setThrowableHeading(x, y, z, v0, rand.nextFloat())
                float inaccuracy = random.nextFloat();
                motion = fire.normalize().add(
                        random.nextGaussian() * 0.0075D * inaccuracy,
                        random.nextGaussian() * 0.0075D * inaccuracy,
                        random.nextGaussian() * 0.0075D * inaccuracy
                ).scale((float) speed);
            }
            level().addFreshEntity(new MineRubbleEntity(level(), pos.getX() + 0.5D, pos.getY() + 2.0D,
                    pos.getZ() + 0.5D, motion, state));
            level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    private static java.util.List<net.minecraft.core.BlockPos> blockPath(int x, int y, int z, int length, Vec3 direction) {
        java.util.List<net.minecraft.core.BlockPos> result = new java.util.ArrayList<>();
        for (int index = 0; index <= length; index++) {
            result.add(new net.minecraft.core.BlockPos((int) (x + direction.x * index), y,
                    (int) (z + direction.z * index)));
        }
        return result;
    }

    private void behemothAttack(LivingEntity target) {
        // [parity A2: glyphid] getEntityToAttack() also counted a high-priority waypoint; only the acid itself
        // needs a living target.
        if (target == null && legacyWaypointTarget == null) {
            behemothTimer = 120;
            behemothBreathTime = 0;
            return;
        }
        if (behemothBreathTime > 0) {
            if (!swinging) {
                swing(InteractionHand.MAIN_HAND);
            }
            if (target != null && distanceTo(target) < 20.0F) {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 6));
                // EntityChemical(worldObj, this, 0, 0, 0).setFluid(Fluids.SULFURIC_ACID)
                HbmFluidDefinition acid = HbmFluids.byName("sulfuric_acid").orElse(HbmFluids.none());
                level().addFreshEntity(new LegacyChemicalEntity(level(), this, 0.0D, 0.0D, 0.0D).setFluid(acid));
            }
            setYRot(yRotO);
            behemothBreathTime--;
        } else if (--behemothTimer <= 0) {
            behemothBreathTime = 120;
            behemothTimer = 120;
        }
    }

    public boolean isBesideClimbableBlock() {
        return entityData.get(WALL_CLIMBING) != 0;
    }

    public boolean useExtendedTargeting() {
        if (getVariant() == Variant.SCOUT) {
            return false;
        }
        if (HbmConfig.glyphidExtendedTargeting()) {
            return true;
        }
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        return HbmPollutionData.get(serverLevel).get(blockPosition(), HbmPollutionType.SOOT)
                >= HbmConfig.GLYPHID_TARGETING_THRESHOLD.get();
    }

    public int getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(int task) {
        setCurrentTask(task, null);
    }

    public EntityWaypoint getWaypoint() {
        return taskWaypoint;
    }

    /** Assigns the old task and its optional entity waypoint as one operation. */
    public void setCurrentTask(int task, EntityWaypoint waypoint) {
        currentTask = Math.max(TASK_IDLE, Math.min(TASK_DIG, task));
        taskWaypoint = waypoint;
        hasWaypoint = waypoint != null;
        if (waypoint != null) {
            taskX = (int) waypoint.getX();
            taskY = (int) waypoint.getY();
            taskZ = (int) waypoint.getZ();
            if (waypoint.isHighPriority()) {
                setTarget(null);
                legacyWaypointTarget = null;
                getNavigation().stop();
            }
        }
        carryOutTask();
    }

    /** Handles task transitions that are performed once when a task is assigned. */
    public void carryOutTask() {
        if (getVariant() == Variant.SCOUT && !level().isClientSide && taskWaypoint == null) {
            // EntityGlyphidScout.carryOutTask, which then falls through to the base implementation
            switch (currentTask) {
                case TASK_INITIATE_RETREAT -> {
                    removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 4));

                    EntityWaypoint returnPoint = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    returnPoint.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
                    returnPoint.setWaypointType(TASK_IDLE);

                    EntityWaypoint home = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    home.setWaypointType(TASK_BUILD_HIVE);
                    home.setAdditionalWaypoint(returnPoint);
                    home.setHighPriority();
                    home.setRadius(6);
                    home.moveTo(homeX, homeY, homeZ, 0.0F, 0.0F);
                    level().addFreshEntity(home);

                    communicate(TASK_FOLLOW, home);
                }
                case TASK_TERRAFORM -> {
                    scoutRange = 60;
                    scoutMinimumHiveDistance = 20;
                }
                default -> {
                }
            }
        }

        switch (currentTask) {
            case TASK_RETREAT_FOR_REINFORCEMENTS -> {
                if (taskWaypoint != null) {
                    communicate(TASK_FOLLOW, taskWaypoint);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);
                }
            }
            case TASK_INITIATE_RETREAT -> {
                if (!level().isClientSide && taskWaypoint == null) {
                    EntityWaypoint returnPoint = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    returnPoint.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);

                    EntityWaypoint home = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
                    home.setAdditionalWaypoint(returnPoint);
                    home.setHighPriority();
                    home.moveTo(homeX, homeY, homeZ, 0.0F, 0.0F);
                    level().addFreshEntity(home);

                    taskWaypoint = home;
                    communicate(TASK_FOLLOW, home);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);
                }
            }
            case TASK_DIG -> shouldDig = true;
            default -> {
            }
        }
    }

    /**
     * Copies tasks to nearby Glyphids. EntityGlyphid reaches everything except scouts; the
     * EntityGlyphidNuclear override reaches only scouts.
     */
    public void communicate(int task, @Nullable EntityWaypoint waypoint) {
        int radius = waypoint == null ? 4 : waypoint.getRadius();
        AABB area = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ())
                .inflate(radius, radius, radius);
        boolean scoutsOnly = getVariant() == Variant.NUCLEAR;
        for (GlyphidEntity glyphid : level().getEntitiesOfClass(GlyphidEntity.class, area,
                glyphid -> glyphid != this && (glyphid.getVariant() == Variant.SCOUT) == scoutsOnly)) {
            if (glyphid.getCurrentTask() != task) {
                glyphid.setCurrentTask(task, waypoint);
            }
        }
    }

    /** EntityGlyphid.isAtDestination; EntityGlyphidScout additionally requires TASK_BUILD_HIVE. */
    public boolean isAtDestination() {
        if (getVariant() == Variant.SCOUT && currentTask != TASK_BUILD_HIVE) {
            return false;
        }
        return isAtDestinationBase();
    }

    /** EntityGlyphid.isAtDestination (the scout's "super.isAtDestination()"). */
    private boolean isAtDestinationBase() {
        int destinationRadius = taskWaypoint == null ? 25 : taskWaypoint.getRadius() * taskWaypoint.getRadius();
        return distanceToSqr(taskX, taskY, taskZ) <= destinationRadius;
    }

    public boolean shouldBreakArmor(float amount) {
        float multiplier = GlyphidStats.armorBreakMultiplier(getVariant());
        double chanceValue = getVariant() == Variant.SCOUT ? amount : amount * multiplier;
        return random.nextInt(100) <= Math.min(Math.pow(chanceValue, 2.0D), 100.0D);
    }

    public void breakOffArmor() {
        int mask = entityData.get(ARMOR) & 0x1F;
        if (mask == 0) {
            return;
        }
        int bit;
        do {
            bit = 1 << random.nextInt(5);
        } while ((mask & bit) == 0);
        entityData.set(ARMOR, (byte) (mask & ~bit));
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.25F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof GlyphidEntity) {
            return false;
        }
        if (getVariant() == Variant.BRAWLER && source.is(DamageTypeTags.IS_FALL) && amount <= 10.0F) {
            return false;
        }
        return GlyphidStats.handleAttack(this, source, amount);
    }

    @Override
    public boolean fireImmune() {
        return getVariant() == Variant.BRENDA || getVariant() == Variant.NUCLEAR || super.fireImmune();
    }

    /** EntityMob in 1.7.10 did not inherit the modern daylight-burning rule. */
    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    public boolean attackSuperclass(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    /** EntityGlyphid.attackEntityAsMob (+ EntityGlyphidScout): no hit while the swing is in progress. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (swinging) {
            return false;
        }
        swing(InteractionHand.MAIN_HAND);
        if (getSubtype() == TYPE_INFECTED && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && getVariant() == Variant.SCOUT && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 10 * 20, 3));
        }
        return hit;
    }

    /** EntityMob.attackEntity: distance < 2.0F and vertically overlapping bounding boxes. */
    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        AABB own = getBoundingBox();
        AABB other = target.getBoundingBox();
        return distanceTo(target) < 2.0F && other.maxY > own.minY && other.minY < own.maxY;
    }

    /** EntityGlyphid.updateArmSwingProgress with swingDuration() = 15 (behemoth: 100). */
    @Override
    protected void updateSwingTime() {
        int duration = legacySwingDuration();
        if (swinging) {
            swingTime++;
            if (swingTime >= duration) {
                swingTime = 0;
                swinging = false;
            }
        } else {
            swingTime = 0;
        }
        attackAnim = (float) swingTime / (float) duration;
    }

    public int legacySwingDuration() {
        return getVariant() == Variant.BEHEMOTH ? 100 : 15;
    }

    /** EntityGlyphid.isOnLadder: climbs whatever it is pushing against. */
    @Override
    public boolean onClimbable() {
        return isBesideClimbableBlock();
    }

    /** EntityGlyphid.setInWeb was empty. */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    /**
     * EntityGlyphid ran on the 1.7.10 non-task AI: EntityCreature set moveForward to the movementSpeed attribute
     * (LegacyCreatureAi, legacy value = attribute * 10 here; moveFlying clamps the input length to 1) and
     * getAIMoveSpeed() was the constant 0.1F, pinned for the duration of the call.
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (!isEffectiveAi()) {
            super.travel(travelVector);
            return;
        }
        legacyTravel = true;
        try {
            super.travel(travelVector);
        } finally {
            legacyTravel = false;
        }
    }

    @Override
    public float getSpeed() {
        return legacyTravel ? 0.1F : super.getSpeed();
    }

    @Override
    protected void tickDeath() {
        if (getVariant() != Variant.NUCLEAR) {
            super.tickDeath();
            return;
        }

        nuclearDeathTicks++;
        if (!nuclearDeathSignalSent) {
            // effectively causes neighboring scouts to retreat (the nuclear communicate() only reaches scouts)
            communicate(TASK_INITIATE_RETREAT, null);
            nuclearDeathSignalSent = true;
        }

        if (nuclearDeathTicks == 90 && !level().isClientSide) {
            AABB area = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(8.0D);
            if (!level().getEntitiesOfClass(GlyphidEntity.class, area,
                    glyphid -> glyphid != this).isEmpty()) {
                // The original loop applied the effects to the dying glyphid itself;
                // Potion.field_76434_w is Health Boost.
                addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 20, 6));
                addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 15 * 20, 1));
            }
        }

        if (nuclearDeathTicks == 100) {
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                boolean infected = getSubtype() == TYPE_INFECTED;
                if (infected) {
                    int count = 15 + random.nextInt(6);
                    for (int index = 0; index < count; index++) {
                        double offsetX = ((index % 2) - 0.5D) * 0.5D;
                        double offsetZ = ((index / 2) - 0.5D) * 0.5D;
                        ParasiteMaggotEntity maggot = new ParasiteMaggotEntity(
                                HbmEntityTypes.PARASITE_MAGGOT.get(), serverLevel);
                        maggot.moveTo(getX() + offsetX, getY() + 0.5D, getZ() + offsetZ,
                                random.nextFloat() * 360.0F, 0.0F);
                        maggot.setDeltaMovement(offsetX, 0.0D, offsetZ);
                        serverLevel.addFreshEntity(maggot);
                    }
                }
                // EntityGlyphidNuclear always executed its radius-25 VNT
                // explosion.  The infected branch only omitted the block
                // allocator; it still damaged entities/players, played the
                // muke sound and sent the flash/wave effect.
                LegacyProjectileUtil.detonateGlyphidNuclear(serverLevel, this, position(), !infected);
            }
            discard();
            return;
        }

        if (!level().isClientSide && nuclearDeathTicks % 10 == 0) {
            level().playSound(null, blockPosition(), HbmSoundEvents.CHARGE_BEEP.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 5.0F, 1.0F);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && getSubtype() == TYPE_INFECTED
                && getVariant() != Variant.NUCLEAR) {
            spawnInfectedMaggots();
            sendInfectedDeathEffect();
        }
        if (!level().isClientSide && getVariant() == Variant.BRENDA && getHealth() <= 0.0F) {
            level().addFreshEntity(new LegacyMistEntity(level(), getX(), getY(), getZ(),
                    LegacyMistEntity.MistType.PHEROMONE, 14.0F, 6.0F, 80));
            for (int index = 0; index < 12; index++) {
                GlyphidEntity glyphid = new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), level());
                glyphid.setVariant(Variant.NORMAL);
                glyphid.moveTo(getX(), getY() + 0.5D, getZ(),
                        random.nextFloat() * 360.0F, 0.0F);
                level().addFreshEntity(glyphid);
                // moveEntity(gaussian, 0, gaussian): a collision-checked displacement, not a velocity
                glyphid.move(MoverType.SELF, new Vec3(random.nextGaussian(), 0.0D, random.nextGaussian()));
            }
        }
        if (!level().isClientSide && getVariant() == Variant.BEHEMOTH) {
            level().addFreshEntity(new LegacyMistEntity(level(), getX(), getY(), getZ(),
                    LegacyMistEntity.MistType.SULFURIC_ACID, 10.0F, 4.0F, 120));
        }
    }

    private void spawnInfectedMaggots() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        int count = 2 + random.nextInt(3);
        for (int index = 0; index < count; index++) {
            double offsetX = (index % 2 - 0.5D) * 0.5D;
            double offsetZ = (index / 2 - 0.5D) * 0.5D;
            ParasiteMaggotEntity maggot = new ParasiteMaggotEntity(
                    HbmEntityTypes.PARASITE_MAGGOT.get(), serverLevel);
            maggot.moveTo(getX() + offsetX, getY() + 0.5D, getZ() + offsetZ,
                    random.nextFloat() * 360.0F, 0.0F);
            maggot.setDeltaMovement(offsetX, 0.0D, offsetZ);
            maggot.hurtMarked = true;
            serverLevel.addFreshEntity(maggot);
        }
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F,
                0.95F + random.nextFloat() * 0.2F);
    }

    private void sendInfectedDeathEffect() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        MaxwellGibEffectPayload payload = new MaxwellGibEffectPayload(
                getId(), getX(), getY() + getBbHeight() * 0.5D, getZ(), getBbWidth(), getBbHeight(), 0);
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= 22_500.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source,
                                       boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (random.nextInt(2) == 0) {
            int looting = 0;
            // [parity A2: glyphid] EntityLivingBase.onDeath: looting only from a player source entity
            LivingEntity killer = source.getEntity() instanceof Player player ? player : null;
            if (killer != null && !killer.getMainHandItem().isEmpty()) {
                looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                        killer.getMainHandItem());
            }
            int count = legacyMeatCount(looting);
            if (count > 0) {
                spawnAtLocation(new ItemStack(
                        isOnFire() ? HbmItems.GLYPHID_MEAT_GRILLED.get() : HbmItems.GLYPHID_MEAT.get(),
                        count));
            }
        }
        if (getVariant() == Variant.BRENDA && random.nextInt(3) == 0) {
            HbmFluidDefinition pheromone = HbmFluids.byName("pheromone").orElse(HbmFluids.none());
            spawnAtLocation(HbmFluidContainerItem.makeFull(HbmItems.GLYPHID_GLAND::get, pheromone), 1.0F);
        }
        if (getVariant() == Variant.BEHEMOTH) {
            HbmFluidDefinition acid = HbmFluids.byName("sulfuric_acid").orElse(HbmFluids.none());
            spawnAtLocation(HbmFluidContainerItem.makeFull(HbmItems.GLYPHID_GLAND::get, acid), 1.0F);
        }
    }

    /** EntityGlyphid.dropFewItems: ((int) getScale() * 2) + looting. */
    public int legacyMeatCount(int looting) {
        return ((int) modelScale() * 2) + looting;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // entityToAttack == null (a high-priority waypoint was an entityToAttack as well)
        return getTarget() == null && legacyWaypointTarget == null && currentTask == TASK_IDLE && tickCount > 100;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("armor", armorMask());
        tag.putByte("subtype", (byte) getSubtype());
        tag.putByte("variant", (byte) getVariant().ordinal());
        tag.putBoolean("hasHome", hasHome);
        tag.putInt("homeX", homeX);
        tag.putInt("homeY", homeY);
        tag.putInt("homeZ", homeZ);
        tag.putBoolean("hasWaypoint", hasWaypoint);
        tag.putInt("taskX", taskX);
        tag.putInt("taskY", taskY);
        tag.putInt("taskZ", taskZ);
        tag.putInt("task", currentTask);
        tag.putBoolean("shouldDig", shouldDig);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ARMOR, tag.getByte("armor"));
        entityData.set(SUBTYPE, tag.getByte("subtype"));
        entityData.set(VARIANT, tag.getByte("variant"));
        hasHome = tag.getBoolean("hasHome");
        homeX = tag.getInt("homeX");
        homeY = tag.getInt("homeY");
        homeZ = tag.getInt("homeZ");
        hasWaypoint = tag.getBoolean("hasWaypoint");
        taskX = tag.getInt("taskX");
        taskY = tag.getInt("taskY");
        taskZ = tag.getInt("taskZ");
        currentTask = Math.max(TASK_IDLE, Math.min(TASK_DIG, tag.getInt("task")));
        shouldDig = tag.getBoolean("shouldDig");
        refreshAttributes();
        refreshDimensions();
    }

    public float armorDamageThreshold() {
        return stats().thresholdPerArmor() * armorCount() / 5.0F;
    }

    /**
     * EntityGlyphid.getCurrentDTDR (IResistanceProvider), keyed like LegacyDamageResistance.getDTDR:
     * lower-case damage type, DamageResistanceHandler category and the unblockable flag.  Absolute damage
     * never reaches this method (the handler returns before asking for DT/DR).
     */
    public float[] legacyCurrentDTDR(String exactType, String category, boolean unblockable) {
        if (unblockable) {
            return new float[] {0.0F, 0.0F};
        }
        float threshold = armorDamageThreshold();
        float resistance = stats().resistanceMultiplier();
        if ("nuclearblast".equals(exactType)) {
            return new float[] {threshold * 0.25F, 0.0F}; // nukes shred through glyphids
        }
        if ("laser".equals(exactType)) {
            return new float[] {threshold * 0.5F, resistance * 0.5F};
        }
        if ("electric".equals(exactType)) {
            return new float[] {threshold * 0.25F, resistance * 0.25F};
        }
        if ("subatomic".equals(exactType)) {
            return new float[] {0.0F, resistance * 0.1F};
        }
        if (LegacyDamageResistance.CATEGORY_FIRE.equals(category)) {
            return new float[] {0.0F, resistance * 0.2F}; // fire ignores DT and most DR
        }
        if (LegacyDamageResistance.CATEGORY_EXPLOSION.equals(category)) {
            return new float[] {threshold * 0.5F, resistance * 0.35F};
        }
        return new float[] {threshold, resistance};
    }

    /** EntityGlyphid.onDamageDealt, called with the damage left after DT/DR. */
    public void onLegacyDamageDealt(float amount) {
        if (shouldBreakArmor(amount)) {
            breakOffArmor();
        }
    }
}
